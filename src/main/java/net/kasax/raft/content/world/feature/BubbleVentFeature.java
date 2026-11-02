package net.kasax.raft.content.world.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BubbleColumnBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.material.Fluids;

/**
 * A cluster of bubble vents in the sea floor with their upward bubble columns already in place (blocks placed by
 * world generation get no neighbour updates, so the column is built here), ringed by basalt.
 */
public record BubbleVentFeature(Block vent, IntProvider count, int spread) implements Feature {
    public static final MapCodec<BubbleVentFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf("vent").forGetter(BubbleVentFeature::vent),
            IntProviders.codec(1, 12).fieldOf("count").forGetter(BubbleVentFeature::count),
            Codec.intRange(0, 6).optionalFieldOf("spread", 2).forGetter(BubbleVentFeature::spread)
    ).apply(i, BubbleVentFeature::new));

    @Override
    public MapCodec<BubbleVentFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        int n = count.sample(random);
        boolean any = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < n; i++) {
            int x = origin.getX() + random.nextIntBetweenInclusive(-spread, spread);
            int z = origin.getZ() + random.nextIntBetweenInclusive(-spread, spread);
            int floor = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
            pos.set(x, floor, z);
            BlockState ground = level.getBlockState(pos);
            if (!ground.isFaceSturdy(level, pos, Direction.UP) || !isWaterSource(level.getBlockState(pos.above()))) {
                continue;
            }
            level.setBlock(pos, vent.defaultBlockState(), 2);
            BlockState column = Blocks.BUBBLE_COLUMN.defaultBlockState().setValue(BubbleColumnBlock.DRAG_DOWN, false);
            pos.move(Direction.UP);
            while (isWaterSource(level.getBlockState(pos)) && pos.getY() < level.getMaxY()) {
                level.setBlock(pos, column, 2);
                pos.move(Direction.UP);
            }
            // a ragged rim of basalt around the vent
            for (Direction d : Direction.Plane.HORIZONTAL) {
                if (random.nextInt(3) == 0) {
                    continue;
                }
                int fx = x + d.getStepX();
                int fz = z + d.getStepZ();
                int fy = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, fx, fz) - 1;
                pos.set(fx, fy, fz);
                BlockState rim = level.getBlockState(pos);
                if (rim.isFaceSturdy(level, pos, Direction.UP) && !rim.is(vent)) {
                    level.setBlock(pos, random.nextBoolean() ? Blocks.BASALT.defaultBlockState()
                            : Blocks.SMOOTH_BASALT.defaultBlockState(), 2);
                }
            }
            any = true;
        }
        return any;
    }

    private static boolean isWaterSource(BlockState state) {
        return state.getBlock() instanceof LiquidBlock && state.getFluidState().is(Fluids.WATER) && state.getFluidState().isSource();
    }
}
