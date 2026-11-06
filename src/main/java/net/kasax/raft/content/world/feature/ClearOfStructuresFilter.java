package net.kasax.raft.content.world.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/**
 * Placement filter {@code raft:clear_of_structures}: drops a feature position that lies within {@code distance}
 * blocks of a piece of any structure.
 *
 * <p>A structure is placed in its generation step before the features of that step and of the later steps, and the
 * features of neighbouring chunks can run after it too. Features that build or replace blocks would then grow into the
 * structure: basalt columns are placed on every floor of a chunk, so they filled the rooms of the Ashen Forge (and
 * Dr. Voss stood in basalt), the ash dunes turned its walls into volcanic ash. Positions are checked against the pieces
 * of the structures referenced by the chunks around the position (structure references are known from the
 * structure_references status on, so this works from the features step on).
 */
public final class ClearOfStructuresFilter implements PlacementFilter {
    public static final MapCodec<ClearOfStructuresFilter> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.intRange(0, 32).fieldOf("distance").forGetter(f -> f.distance),
            Codec.BOOL.optionalFieldOf("whole_column", false).forGetter(f -> f.wholeColumn)
    ).apply(i, ClearOfStructuresFilter::new));

    private final int distance;
    /** for features placed without a height (their position is at the bottom of the world, they look for the sea
     * floor or the sea surface themselves: bubble vents, rubble, ice floes, icebergs): only x and z count. */
    private final boolean wholeColumn;

    public ClearOfStructuresFilter(int distance, boolean wholeColumn) {
        this.distance = distance;
        this.wholeColumn = wholeColumn;
    }

    @Override
    public boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos origin) {
        return clear(context.getLevel(), origin, this.distance, this.wholeColumn);
    }

    private static boolean inside(BoundingBox box, BlockPos pos, int distance, boolean wholeColumn) {
        BoundingBox b = box.inflatedBy(distance);
        return wholeColumn ? pos.getX() >= b.minX() && pos.getX() <= b.maxX() && pos.getZ() >= b.minZ() && pos.getZ() <= b.maxZ()
                : b.isInside(pos);
    }

    /** true when no structure piece (grown by distance) contains pos (with wholeColumn: its x and z). */
    public static boolean clear(WorldGenLevel level, BlockPos pos, int distance, boolean wholeColumn) {
        // during generation only some chunks around the generating one may be read: references within 1 chunk
        // (carvers status or later), starts within 8 chunks (structure_starts status)
        ChunkPos center = level instanceof WorldGenRegion region ? region.getCenter() : null;
        int pcx = pos.getX() >> 4, pcz = pos.getZ() >> 4;
        int reach = (distance >> 4) + 1;
        for (int cx = pcx - reach; cx <= pcx + reach; cx++) {
            for (int cz = pcz - reach; cz <= pcz + reach; cz++) {
                if (center != null && center.getChessboardDistance(cx, cz) > 1) {
                    continue;
                }
                ChunkAccess chunk = level.getChunk(cx, cz, ChunkStatus.STRUCTURE_REFERENCES, false);
                if (chunk == null) {
                    continue;
                }
                for (Map.Entry<Structure, LongSet> e : chunk.getAllReferences().entrySet()) {
                    for (long start : e.getValue()) {
                        int sx = ChunkPos.getX(start), sz = ChunkPos.getZ(start);
                        if (center != null && center.getChessboardDistance(sx, sz) > 8) {
                            continue;
                        }
                        ChunkAccess startChunk = level.getChunk(sx, sz, ChunkStatus.STRUCTURE_STARTS, false);
                        if (startChunk == null) {
                            continue;
                        }
                        StructureStart st = startChunk.getStartForStructure(e.getKey());
                        if (st == null || !st.isValid() || !inside(st.getBoundingBox(), pos, distance, wholeColumn)) {
                            continue;
                        }
                        for (StructurePiece piece : st.getPieces()) {
                            if (inside(piece.getBoundingBox(), pos, distance, wholeColumn)) {
                                return false;
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    @Override
    public MapCodec<ClearOfStructuresFilter> codec() {
        return CODEC;
    }
}
