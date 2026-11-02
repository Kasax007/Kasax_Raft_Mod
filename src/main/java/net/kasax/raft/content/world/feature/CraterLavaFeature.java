package net.kasax.raft.content.world.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Fills the flat floors of volcano craters with a lava lake. The terrain makes the floor of the big volcanoes exactly
 * flat with its top block at {@code floor_y - 1}; this feature (one call per chunk, origin = chunk corner) turns every
 * such column whose four neighbours are not lower into lava, with magma underneath, so the lake cannot spill.
 */
public record CraterLavaFeature(int floorY) implements Feature {
    public static final MapCodec<CraterLavaFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.intRange(-64, 320).optionalFieldOf("floor_y", 164).forGetter(CraterLavaFeature::floorY)
    ).apply(i, CraterLavaFeature::new));

    @Override
    public MapCodec<CraterLavaFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        int x0 = origin.getX() & ~15;
        int z0 = origin.getZ() & ~15;
        int top = floorY - 1;
        boolean any = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = x0 + dx;
                int z = z0 + dz;
                if (surface(level, x, z) != top) {
                    continue;
                }
                boolean contained = true;
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    if (surface(level, x + d.getStepX(), z + d.getStepZ()) < top) {
                        contained = false;
                        break;
                    }
                }
                if (!contained) {
                    continue;
                }
                pos.set(x, top, z);
                BlockState here = level.getBlockState(pos);
                if (here.isAir() || !here.getFluidState().isEmpty()) {
                    continue;
                }
                level.setBlock(pos, Blocks.LAVA.defaultBlockState(), 2);
                pos.set(x, top - 1, z);
                level.setBlock(pos, Blocks.MAGMA_BLOCK.defaultBlockState(), 2);
                any = true;
            }
        }
        return any;
    }

    /** y of the top solid block of the column. */
    private static int surface(WorldGenLevel level, int x, int z) {
        return level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
    }
}
