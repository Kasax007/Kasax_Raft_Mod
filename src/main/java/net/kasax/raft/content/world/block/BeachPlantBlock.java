package net.kasax.raft.content.world.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;
import net.kasax.raft.compat.VShapes;

/**
 * Small plant that grows on sand as well as on dirt, grass and mud (beach grass, tropical fern). With a tall version
 * set, bone meal turns it into that two-block plant (beach grass -> sea oats), like grass into tall grass.
 */
public class BeachPlantBlock extends BushBlock implements BonemealableBlock {
    @Override
    protected com.mojang.serialization.MapCodec<? extends BushBlock> codec() {
        return null;
    }

    private static final VoxelShape SHAPE = VShapes.column(12.0, 0.0, 13.0);
    private final @Nullable Supplier<? extends Block> tallVersion;

    public BeachPlantBlock(@Nullable Supplier<? extends Block> tallVersion, Properties properties) {
        super(properties);
        this.tallVersion = tallVersion;
    }

    /** Sand, dirt-like blocks, farmland and clay: what the islands are made of. */
    public static boolean isIslandSoil(BlockState state) {
        return state.is(BlockTags.SAND) || state.is(BlockTags.DIRT) || state.is(net.minecraft.world.level.block.Blocks.FARMLAND)
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
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return tallVersion != null && tallVersion.get().defaultBlockState().canSurvive(level, pos)
                && level.isEmptyBlock(pos.above()) && !level.isOutsideBuildHeight(pos.above());
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        if (tallVersion != null) {
            DoublePlantBlock.placeAt(level, tallVersion.get().defaultBlockState(), pos, 2);
        }
    }
}
