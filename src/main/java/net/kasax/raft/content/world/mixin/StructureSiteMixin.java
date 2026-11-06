package net.kasax.raft.content.world.mixin;

import java.util.Map;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
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
 * ground under their whole footprint is land (never as a square pillar in the sea), the Coral Temple only where the
 * sea floor is deep enough for all of it to stay under water. Then no structure may grow into one of a higher rank
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

    @Inject(method = "findValidGenerationPoint", at = @At("RETURN"), cancellable = true)
    private void raft$checkSite(Structure.GenerationContext context,
                                CallbackInfoReturnable<Optional<Structure.GenerationStub>> cir) {
        Optional<Structure.GenerationStub> stub = cir.getReturnValue();
        if (stub.isEmpty()) {
            return;
        }
        Identifier id = context.registryAccess().lookupOrThrow(Registries.STRUCTURE).getKey((Structure) (Object) this);
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
        } else if ("coral_temple".equals(id.getPath())) {
            int[] mm = raft$minMax(context, pos, RAFT$TEMPLE_RADIUS);
            if (mm[0] < RAFT$TEMPLE_MIN_FLOOR || mm[1] > sea - RAFT$TEMPLE_CLEARANCE) {
                cir.setReturnValue(Optional.empty());
            }
        }
        // a structure must not grow into a structure of a higher rank (story places, places with a person)
        if (cir.getReturnValue().isPresent()) {
            cir.setReturnValue(net.kasax.raft.content.world.StructurePriority.check((Structure) (Object) this, context, cir.getReturnValue().get()));
        }
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
