package net.kasax.raft.content.world.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Small plant that grows on sand as well as on dirt, grass and mud (beach grass, tropical fern). With a tall version
 * set, bone meal turns it into that two-block plant (beach grass -> sea oats), like grass into tall grass.
 */
public class BeachPlantBlock extends VegetationBlock implements BonemealableBlock {
    private static final VoxelShape SHAPE = Block.column(12.0, 0.0, 13.0);
    private final @Nullable Supplier<? extends Block> tallVersion;

    public BeachPlantBlock(@Nullable Supplier<? extends Block> tallVersion, Properties properties) {
        super(properties);
        this.tallVersion = tallVersion;
    }

    /** Sand, dirt-like blocks, farmland and clay: what the islands are made of. */
    public static boolean isIslandSoil(BlockState state) {
        return state.is(BlockTags.SAND) || state.is(BlockTags.DIRT) || state.is(BlockTags.SUPPORTS_CROPS)
                || state.is(net.minecraft.world.level.block.Blocks.CLAY);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return isIslandSoil(state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
        return tallVersion != null && tallVersion.get().defaultBlockState().canSurvive(level, pos)
                && level.isEmptyBlock(pos.above()) && level.isInsideBuildHeight(pos.above());
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        if (tallVersion != null) {
            DoublePlantBlock.placeAt(level, tallVersion.get().defaultBlockState(), pos, 2);
        }
    }
}
