package net.kasax.raft.content.world.mixin;

import java.util.Map;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Site checks for Beach Craft structures, so they blend into the world: island buildings only start where the
 * ground under their whole footprint is land (never as a square pillar in the sea), the Coral Temple and the Sunken
 * Plane only where the sea floor is deep enough for all of it to stay under water. Then no structure may grow into one of a higher rank
 * (StructurePriority).
 */
@Mixin(Structure.class)
public abstract class StructureSiteMixin {
    /** footprint radius of the island buildings (center and four points at this distance must be land). */
    @Unique
    private static final Map<String, Integer> RAFT$LAND = Map.of(
            "castaway_camp", 6, "beach_shack", 8, "old_lighthouse", 10, "ashen_forge", 12);
    /** Coral Temple: 23 blocks high, sits one block into the floor; the floor must lie between these heights. */
    @Unique
    private static final int RAFT$TEMPLE_RADIUS = 11, RAFT$TEMPLE_MIN_FLOOR = 50, RAFT$TEMPLE_CLEARANCE = 25;
    /** Small island places whose ground the terrain adaptation flattens: on a slope that cut a sharp rectangle with
     * a step into the hill (Teo's sand plaza). They only start where the ground under them is almost flat:
     * radius and the largest height difference of nine points (center, edges, corners). */
    @Unique
    private static final Map<String, int[]> RAFT$FLAT = Map.of("beach_shack", new int[] {10, 3}, "castaway_camp", new int[] {7, 3},
            "old_lighthouse", new int[] {12, 4});
    /** Coastal places (3.2): the ground at the center at most this high above the sea, and open water (floor below the
     * sea) somewhere at this distance: radius, max height above sea. The lighthouse stands on a low coast by the sea,
     * not cut into a hill. */
    @Unique
    private static final Map<String, int[]> RAFT$COAST = Map.of("old_lighthouse", new int[] {28, 9});
    /** Stilt villages over open water (3.2): no ground at or above the sea within these radii (Saltmere grew into
     * island hills, its decks cut flat into the slope). */
    @Unique
    private static final Map<String, int[]> RAFT$OPEN_WATER = Map.of("saltmere_village", new int[] {16, 32, 48});
    /** Sunken Plane: 15 blocks high, sits 3 blocks into the floor; the floor must be this deep, else its top (and the
     * kelp and seagrass on it) stood out of the sea. */
    @Unique
    private static final int RAFT$PLANE_RADIUS = 14, RAFT$PLANE_CLEARANCE = 14;
    /** 3.4: places that settle onto the ground under their whole footprint: lowest drop, highest stand (see raft$settle). */
    @Unique
    private static final Map<String, int[]> RAFT$SETTLE = Map.of("ashen_forge", new int[] {8, 10},
            "abyssal_sanctum", new int[] {6, 10}, "coral_temple", new int[] {4, 10}, "helix_station", new int[] {3, 8});
    /** Helix Station (the lab): footprint radius, lowest floor, floor at least this far below the sea. */
    @Unique
    private static final int RAFT$LAB_RADIUS = 22, RAFT$LAB_MIN_FLOOR = 45, RAFT$LAB_CLEARANCE = 24;

    @Inject(method = "findValidGenerationPoint", at = @At("RETURN"), cancellable = true)
    private void raft$checkSite(Structure.GenerationContext context,
                                CallbackInfoReturnable<Optional<Structure.GenerationStub>> cir) {
        Optional<Structure.GenerationStub> stub = cir.getReturnValue();
        if (stub.isEmpty()) {
            return;
        }
        ResourceLocation id = context.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey((Structure) (Object) this);
        if (id == null || !"raft".equals(id.getNamespace())) {
            return;
        }
        BlockPos pos = stub.get().position();
        ChunkGenerator gen = context.chunkGenerator();
        int sea = gen.getSeaLevel();
        Integer land = RAFT$LAND.get(id.getPath());
        if (land != null) {
            if (raft$minMax(context, pos, land)[0] < sea) {
                cir.setReturnValue(Optional.empty());
            }
            int[] flat = RAFT$FLAT.get(id.getPath());
            if (flat != null && cir.getReturnValue().isPresent() && raft$spread(context, pos, flat[0]) > flat[1]) {
                cir.setReturnValue(Optional.empty());
            }
            int[] coast = RAFT$COAST.get(id.getPath());
            if (coast != null && cir.getReturnValue().isPresent()) {
                int[] near = raft$minMax(context, pos, 0);
                if (near[0] > sea + coast[1] || raft$lowest8(context, pos, coast[0]) >= sea - 1) {
                    cir.setReturnValue(Optional.empty());
                }
            }
        } else if (RAFT$OPEN_WATER.containsKey(id.getPath())) {
            for (int r : RAFT$OPEN_WATER.get(id.getPath())) {
                if (raft$highest8(context, pos, r) >= sea - 1) {
                    cir.setReturnValue(Optional.empty());
                    break;
                }
            }
        } else if ("helix_station".equals(id.getPath())) {
            // 3.2: the lab sits on the sea floor with water above it: floor 45..sea-24 under its whole footprint,
            // so its top (17 blocks high, 3 in the floor) stays at least 10 blocks under the surface
            int[] mm = raft$minMax(context, pos, RAFT$LAB_RADIUS);
            if (mm[0] < RAFT$LAB_MIN_FLOOR || mm[1] > sea - RAFT$LAB_CLEARANCE) {
                cir.setReturnValue(Optional.empty());
            }
        } else if ("sunken_plane".equals(id.getPath())) {
            if (raft$minMax(context, pos, RAFT$PLANE_RADIUS)[1] > sea - RAFT$PLANE_CLEARANCE) {
                cir.setReturnValue(Optional.empty());
            }
        } else if ("coral_temple".equals(id.getPath())) {
            int[] mm = raft$minMax(context, pos, RAFT$TEMPLE_RADIUS);
            if (mm[0] < RAFT$TEMPLE_MIN_FLOOR || mm[1] > sea - RAFT$TEMPLE_CLEARANCE) {
                cir.setReturnValue(Optional.empty());
            }
        }
        // 3.4: the big places settle onto the real ground under their whole footprint, or are not built there
        int[] settle = RAFT$SETTLE.get(id.getPath());
        if (settle != null && net.kasax.raft.content.world.StructureFoundations.enabled && cir.getReturnValue().isPresent()) {
            cir.setReturnValue(raft$settle(context, cir.getReturnValue().get(), settle[0], settle[1]));
        }
        // a structure must not grow into a structure of a higher rank (story places, places with a person)
        if (cir.getReturnValue().isPresent()) {
            cir.setReturnValue(net.kasax.raft.content.world.StructurePriority.check((Structure) (Object) this, context, cir.getReturnValue().get()));
        }
    }

    /**
     * The start height comes from one point (the start chunk); a big place on a slope or a hill then hangs over the
     * lower side (the Ashen Forge vault stood 31 blocks above its island's shore). Its footing columns (from the
     * templates, every 4th block) are compared with the ground under them: the structure is lowered by the median
     * height above the ground (at most maxDown), and if a tenth of it would still stand more than maxFloat above the
     * ground, the place is not built here.
     */
    @Unique
    private static Optional<Structure.GenerationStub> raft$settle(Structure.GenerationContext context,
                                                                  Structure.GenerationStub stub, int maxDown, int maxFloat) {
        net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder pieces = stub.getPiecesBuilder();
        Structure.GenerationStub built = new Structure.GenerationStub(stub.position(), com.mojang.datafixers.util.Either.right(pieces));
        if (pieces.isEmpty()) {
            return Optional.of(built);
        }
        java.util.List<Integer> above = new java.util.ArrayList<>();
        for (net.minecraft.world.level.levelgen.structure.StructurePiece pc : pieces.build().pieces()) {
            ResourceLocation tid = net.kasax.raft.content.world.StructureFootprint.templateOf(pc);
            if (tid == null) {
                continue;
            }
            var t = net.kasax.raft.content.world.StructureFootprint.template(context.structureTemplateManager(), tid);
            if (t == null) {
                continue;
            }
            for (var f : net.kasax.raft.content.world.StructureFootprint.feet(
                    (net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece) pc, t)) {
                if (Math.floorMod(f.x(), 4) == 0 && Math.floorMod(f.z(), 4) == 0) {
                    above.add(f.y() - context.chunkGenerator().getFirstOccupiedHeight(f.x(), f.z(),
                            Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState()));
                }
            }
        }
        if (above.isEmpty()) {
            return Optional.of(built);
        }
        java.util.Collections.sort(above);
        int down = Math.max(0, Math.min(maxDown, above.get(above.size() / 2)));
        int high = above.get(Math.min(above.size() - 1, (int) (above.size() * 0.9))) - down;
        if (high > maxFloat) {
            return Optional.empty();
        }
        if (down > 0) {
            pieces.offsetPiecesVertically(-down);
        }
        return Optional.of(built);
    }

    /** height difference of the ground (top solid block, fluids ignored) over the center, edges and corners at r. */
    @Unique
    private static int raft$spread(Structure.GenerationContext context, BlockPos pos, int r) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int dx = -r; dx <= r; dx += r) {
            for (int dz = -r; dz <= r; dz += r) {
                int h = context.chunkGenerator().getFirstOccupiedHeight(pos.getX() + dx, pos.getZ() + dz,
                        Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
                min = Math.min(min, h);
                max = Math.max(max, h);
            }
        }
        return max - min;
    }

    /** top solid block (fluids ignored) at the eight points around pos at distance r: the lowest of them. */
    @Unique
    private static int raft$lowest8(Structure.GenerationContext context, BlockPos pos, int r) {
        int min = Integer.MAX_VALUE;
        for (int dx = -r; dx <= r; dx += r) {
            for (int dz = -r; dz <= r; dz += r) {
                if (dx != 0 || dz != 0) {
                    min = Math.min(min, context.chunkGenerator().getFirstOccupiedHeight(pos.getX() + dx, pos.getZ() + dz,
                            Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState()));
                }
            }
        }
        return min;
    }

    /** the highest top solid block at the center and the eight points around it at distance r. */
    @Unique
    private static int raft$highest8(Structure.GenerationContext context, BlockPos pos, int r) {
        int max = Integer.MIN_VALUE;
        for (int dx = -r; dx <= r; dx += r) {
            for (int dz = -r; dz <= r; dz += r) {
                max = Math.max(max, context.chunkGenerator().getFirstOccupiedHeight(pos.getX() + dx, pos.getZ() + dz,
                        Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState()));
            }
        }
        return max;
    }

    /** lowest and highest top solid block (fluids ignored) at the center and four points at distance r. */
    @Unique
    private static int[] raft$minMax(Structure.GenerationContext context, BlockPos pos, int r) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        int[][] offsets = {{0, 0}, {r, 0}, {-r, 0}, {0, r}, {0, -r}};
        for (int[] o : offsets) {
            int h = context.chunkGenerator().getFirstOccupiedHeight(pos.getX() + o[0], pos.getZ() + o[1],
                    Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
            min = Math.min(min, h);
            max = Math.max(max, h);
        }
        return new int[] {min, max};
    }
}
