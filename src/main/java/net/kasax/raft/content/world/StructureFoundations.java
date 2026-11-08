package net.kasax.raft.content.world;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;

/**
 * Footings down to the ground (3.4). A structure piece is placed at one height, the sea floor and the islands are
 * not flat: where the ground under a piece drops away, its bottom hung in the water (Port Solace stood on its plaza
 * high above a slope, the stilts of Saltmere ended in the water, the edges of the Helix Station and the sanctum
 * floated a few blocks). After a piece of a Beach Craft structure is placed, every column whose bottom block (in the
 * two lowest layers of the piece) is a full block without a block entity is continued downwards through water, air
 * and plants until it meets the ground: logs and pillars as they are (the stilts of Saltmere), everything else as a
 * mix of the piece's own bottom blocks (stone, cobblestone and andesite under the city, sand and gravel under the
 * wrecks, concrete under the lab, the forge's volcanic rock). Floating on purpose: the abandoned rafts, the
 * buoys and the iceberg outpost. Tops of stacked tower pieces are skipped (they stand on their base piece).
 */
public final class StructureFoundations {
    /** deepest footing (blocks); a column over a deeper hole (a cave) is left as it is. */
    private static final int MAX_DEPTH = 96;

    private StructureFoundations() {
    }

    /** Templates that get footings: every Beach Craft structure template except the ones that float on purpose. */
    static boolean applies(String path) {
        return !(path.startsWith("abandoned_raft/") || path.startsWith("buoy_field/") || path.startsWith("iceberg_outpost/")
                || path.contains("_top"));
    }

    private static boolean free(WorldGenLevel level, BlockPos pos, BlockState s) {
        return s.getCollisionShape(level, pos).isEmpty();
    }

    /** Called after the piece was placed into the chunk box (PoolPieceFoundationMixin). */
    public static void place(PoolElementStructurePiece piece, WorldGenLevel level, BoundingBox chunkBox) {
        if (!(piece.getElement() instanceof SinglePoolElement single)) {
            return;
        }
        Identifier id;
        try {
            id = single.getTemplateLocation();
        } catch (RuntimeException e) {
            return;
        }
        if (!"raft".equals(id.getNamespace()) || !applies(id.getPath())) {
            return;
        }
        BoundingBox box = piece.getBoundingBox();
        int x0 = Math.max(box.minX(), chunkBox.minX()), x1 = Math.min(box.maxX(), chunkBox.maxX());
        int z0 = Math.max(box.minZ(), chunkBox.minZ()), z1 = Math.min(box.maxZ(), chunkBox.maxZ());
        if (x0 > x1 || z0 > z1) {
            return;
        }
        int minY = level.getMinY();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        // the bottom block of every column (in the two lowest layers of the piece)
        List<int[]> columns = new ArrayList<>();
        List<BlockState> bottoms = new ArrayList<>();
        Map<BlockState, Integer> mix = new LinkedHashMap<>();
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int y = box.minY(); y <= box.minY() + 1; y++) {
                    BlockState s = level.getBlockState(pos.set(x, y, z));
                    if (!free(level, pos, s)) {
                        if (s.isCollisionShapeFullBlock(level, pos) && !s.hasBlockEntity() && s.getFluidState().isEmpty()) {
                            columns.add(new int[]{x, y, z});
                            bottoms.add(s);
                            if (!pillar(s) && s.getLightEmission() == 0) {
                                mix.merge(s, 1, Integer::sum);
                            }
                        }
                        break;
                    }
                }
            }
        }
        int total = mix.values().stream().mapToInt(Integer::intValue).sum();
        for (int i = 0; i < columns.size(); i++) {
            int[] c = columns.get(i);
            int x = c[0], by = c[1], z = c[2];
            int y = by - 1;
            int depth = 0;
            while (y > minY && depth < MAX_DEPTH && free(level, pos.set(x, y, z), level.getBlockState(pos))) {
                y--;
                depth++;
            }
            if (depth == 0 || depth >= MAX_DEPTH || y <= minY) {
                continue;
            }
            BlockState own = bottoms.get(i);
            for (int yy = by - 1; yy > y; yy--) {
                level.setBlock(pos.set(x, yy, z), pillar(own) || total == 0 ? own : pick(mix, total, x, yy, z), 2);
            }
        }
    }

    /** Logs and pillars go on as one column (the stilts of Saltmere); everything else is mixed. */
    private static boolean pillar(BlockState s) {
        return s.hasProperty(BlockStateProperties.AXIS);
    }

    /** A block of the piece's bottom layer, weighted by how often it is there, fixed by the position: a footing of
     * stone, cobblestone and andesite under the city, sand and gravel under a wreck, the forge's volcanic rock mix,
     * without stripes (a column of one block each) and without lights or magma. */
    private static BlockState pick(Map<BlockState, Integer> mix, int total, int x, int y, int z) {
        int r = (int) Math.floorMod(Mth.getSeed(x, y, z), (long) total);
        for (Map.Entry<BlockState, Integer> e : mix.entrySet()) {
            r -= e.getValue();
            if (r < 0) {
                return e.getKey();
            }
        }
        return mix.keySet().iterator().next();
    }
}
