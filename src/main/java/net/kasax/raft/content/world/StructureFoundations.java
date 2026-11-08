package net.kasax.raft.content.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;

/**
 * Footings down to the ground (3.4). A structure is placed at one height, the sea floor and the islands are not flat:
 * where the ground under a piece drops away, its bottom hung in the water. The site check settles the big places onto
 * the real ground first (StructureSiteMixin: lowered onto the floor under their footprint, or not built where they
 * would still hang high); what is left is closed here, once per chunk after the structure's pieces are placed
 * (StructureFootingMixin on Structure.afterPlace):
 * <ul>
 * <li>footings: every footing column of a piece (its template's lowest full block in the two bottom layers) with
 *     water, air or plants under it goes on down to the ground. Logs and pillars go on as they are (the stilts of
 *     Saltmere). Everything else is built of the ground right under the column (sand under a wreck, stone, the
 *     volcanic ash and basalt of an island), or, where that is no natural ground, of the piece's main bottom block;
 *     never lights, ores or magma, never a mix of specks.</li>
 * <li>a rock skirt: beside a footing that stands higher than the ground, the same ground material slopes down
 *     outwards (a little rough), so a foundation reads as rock or a sediment mound, not as a straight wall.</li>
 * </ul>
 * Everything is worked out from the templates, so every chunk builds its part the same way. The rafts, buoys, the
 * iceberg and the tops of stacked towers float or stand on purpose and are left out.
 */
public final class StructureFoundations {
    /** deepest footing (blocks); a column over a deeper hole (a cave) is left as it is. */
    private static final int MAX_DEPTH = 96;
    /** reach of the rock skirt beside a footing (blocks) and its drop per block outwards. */
    private static final int SKIRT = 10;
    private static final double SLOPE = 1.4;
    /** Off only in the test harness (bctest "footings off"): no settling, no footings, to compare before and after. */
    public static volatile boolean enabled = true;
    private static final Set<String> FLOATING = Set.of("abandoned_raft", "buoy_field", "iceberg_outpost");
    private static final Set<String> RAFT_GROUND = Set.of("shell_sand", "black_sand", "volcanic_ash", "pumice",
            "coral_stone", "tidestone");

    private StructureFoundations() {
    }

    private static boolean free(WorldGenLevel level, BlockPos pos, BlockState s) {
        return s.getCollisionShape(level, pos).isEmpty();
    }

    /** Natural ground a footing may be built of (stone, sand, gravel, dirt, clay, volcanic rock, the mod's sands). */
    static boolean natural(BlockState s) {
        if (!StructureFootprint.full(s) || s.getLightEmission() > 0 || s.is(Blocks.MAGMA_BLOCK)) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(s.getBlock());
        if (id.getPath().contains("_ore")) {
            return false;
        }
        if ("raft".equals(id.getNamespace())) {
            return RAFT_GROUND.contains(id.getPath());
        }
        return s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(BlockTags.SAND) || s.is(BlockTags.DIRT)
                || s.is(BlockTags.TERRACOTTA) || s.is(Blocks.GRAVEL) || s.is(Blocks.CLAY) || s.is(Blocks.SANDSTONE)
                || s.is(Blocks.RED_SANDSTONE) || s.is(Blocks.BASALT) || s.is(Blocks.SMOOTH_BASALT)
                || s.is(Blocks.BLACKSTONE) || s.is(Blocks.CALCITE) || s.is(Blocks.COBBLESTONE)
                || s.is(Blocks.MOSSY_COBBLESTONE) || s.is(Blocks.COBBLED_DEEPSLATE) || s.is(Blocks.SNOW_BLOCK)
                || s.is(Blocks.PACKED_ICE) || s.is(Blocks.MUD) || s.is(Blocks.MOSS_BLOCK);
    }

    /** Called after the pieces of a structure were placed into a chunk (StructureFootingMixin). */
    public static void place(Structure structure, WorldGenLevel level, BoundingBox chunkBox, PiecesContainer container) {
        if (!enabled) {
            return;
        }
        ResourceLocation sid = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(structure);
        MinecraftServer server = level.getServer();
        if (sid == null || !"raft".equals(sid.getNamespace()) || FLOATING.contains(sid.getPath()) || server == null) {
            return;
        }
        List<StructurePiece> pieces = container.pieces();
        if (pieces.isEmpty()) {
            return;
        }
        int reach = SKIRT;
        BoundingBox area = chunkBox;
        // the skirt stays inside the structure's box (inflated by 12 with terrain adaptation): only chunks in that box
        // get this call, so it never ends at a chunk that was not asked
        BoundingBox all = container.calculateBoundingBox();
        BoundingBox skirtBox = structure.terrainAdaptation() != TerrainAdjustment.NONE ? all.inflatedBy(12) : all;
        int cx0 = chunkBox.minX(), cz0 = chunkBox.minZ(), cx1 = chunkBox.maxX(), cz1 = chunkBox.maxZ();
        // footings of all pieces near this chunk (lowest per column), and the columns the templates fill
        Map<Long, StructureFootprint.WorldFoot> feet = new HashMap<>();
        Map<Long, BlockState> main = new HashMap<>();
        Set<Long> covered = new HashSet<>();
        for (StructurePiece pc : pieces) {
            BoundingBox b = pc.getBoundingBox();
            if (b.maxX() < cx0 - reach || b.minX() > cx1 + reach || b.maxZ() < cz0 - reach || b.minZ() > cz1 + reach) {
                continue;
            }
            ResourceLocation tid = StructureFootprint.templateOf(pc);
            if (tid == null) {
                continue;
            }
            StructureFootprint.Template t = StructureFootprint.template(server.getStructureManager(), tid);
            if (t == null) {
                continue;
            }
            PoolElementStructurePiece pe = (PoolElementStructurePiece) pc;
            covered.addAll(StructureFootprint.covered(pe, t));
            if (tid.getPath().contains("_top")) {
                continue;                       // stacked on its base piece
            }
            for (StructureFootprint.WorldFoot f : StructureFootprint.feet(pe, t)) {
                if (f.x() < cx0 - reach || f.x() > cx1 + reach || f.z() < cz0 - reach || f.z() > cz1 + reach) {
                    continue;
                }
                long col = BlockPos.asLong(f.x(), 0, f.z());
                StructureFootprint.WorldFoot old = feet.get(col);
                if (old == null || f.y() < old.y()) {
                    feet.put(col, f);
                    main.put(col, t.main());
                }
            }
        }
        if (feet.isEmpty()) {
            return;
        }
        int minY = level.getMinBuildHeight();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        // 1. footings inside this chunk
        for (StructureFootprint.WorldFoot f : feet.values()) {
            if (!area.isInside(f.x(), f.y(), f.z())) {
                continue;
            }
            if (free(level, pos.set(f.x(), f.y(), f.z()), level.getBlockState(pos))) {
                continue;                       // not placed here (broken off, replaced by water)
            }
            int y = f.y() - 1;
            int depth = 0;
            while (y > minY && depth < MAX_DEPTH && free(level, pos.set(f.x(), y, f.z()), level.getBlockState(pos))) {
                y--;
                depth++;
            }
            if (depth == 0 || depth >= MAX_DEPTH || y <= minY) {
                continue;
            }
            BlockState fill;
            if (f.state().hasProperty(BlockStateProperties.AXIS)) {
                fill = f.state();               // stilts and pillars go on as they are
            } else {
                fill = material(level.getBlockState(pos.set(f.x(), y, f.z())), main.get(BlockPos.asLong(f.x(), 0, f.z())), f.state());
            }
            for (int yy = f.y() - 1; yy > y; yy--) {
                level.setBlock(pos.set(f.x(), yy, f.z()), fill, 2);
            }
        }
        // 2. the rock skirt beside raised footings (not beside stilts)
        for (int x = cx0; x <= cx1; x++) {
            for (int z = cz0; z <= cz1; z++) {
                long col = BlockPos.asLong(x, 0, z);
                if (covered.contains(col) || !skirtBox.isInside(x, skirtBox.minY(), z)) {
                    continue;
                }
                double top = Double.NEGATIVE_INFINITY;
                BlockState own = null;
                for (int dx = -reach; dx <= reach; dx++) {
                    for (int dz = -reach; dz <= reach; dz++) {
                        StructureFootprint.WorldFoot f = feet.get(BlockPos.asLong(x + dx, 0, z + dz));
                        if (f == null || f.state().hasProperty(BlockStateProperties.AXIS)) {
                            continue;
                        }
                        double d = Math.sqrt(dx * dx + dz * dz);
                        if (d > reach) {
                            continue;
                        }
                        double h = f.y() - 1 - SLOPE * d;
                        if (h > top) {
                            top = h;
                            own = main.get(BlockPos.asLong(x + dx, 0, z + dz));
                        }
                    }
                }
                if (top == Double.NEGATIVE_INFINITY) {
                    continue;
                }
                // rough edge: up to 1.5 blocks up or down, fixed by the column
                top += (Math.floorMod(Mth.getSeed(x, 7, z), 31L) - 15) / 10.0;
                int ty = (int) Math.floor(top);
                // the ground of this column (water, air and plants above it)
                int y = ty;
                while (y > minY && free(level, pos.set(x, y, z), level.getBlockState(pos))) {
                    y--;
                }
                if (y >= ty || ty - y > MAX_DEPTH || y <= minY) {
                    continue;
                }
                // only where everything from the ground up to the top is water, air or plants
                BlockState fill = material(level.getBlockState(pos.set(x, y, z)), own, null);
                if (fill == null) {
                    continue;
                }
                for (int yy = y + 1; yy <= ty; yy++) {
                    level.setBlock(pos.set(x, yy, z), fill, 2);
                }
            }
        }
    }

    /**
     * What a footing is built of: the natural ground under it (sand, gravel, stone, volcanic ash ...), else the
     * piece's main bottom block, else (only for a footing, not for the skirt) its own bottom block.
     */
    private static BlockState material(BlockState ground, BlockState main, BlockState own) {
        if (natural(ground)) {
            return ground.getBlock() == Blocks.GRASS_BLOCK || ground.getBlock() == Blocks.MYCELIUM
                    || ground.getBlock() == Blocks.PODZOL ? Blocks.DIRT.defaultBlockState() : ground.getBlock().defaultBlockState();
        }
        if (main != null) {
            return main;
        }
        return own;
    }
}
