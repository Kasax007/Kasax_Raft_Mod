package net.kasax.raft.content.food.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A wild island crop (sweet potato, chili, banana, rice): grows on grass, dirt, sand and ash; breaking it drops food and seeds. */
public class WildPlantBlock extends BushBlock {
    @Override
    protected com.mojang.serialization.MapCodec<? extends BushBlock> codec() {
        return null;
    }

    private static final VoxelShape SHAPE = net.kasax.raft.compat.VShapes.column(12.0, 0.0, 13.0);

    public WildPlantBlock(Properties properties) {
        super(properties);
    }


    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(net.minecraft.world.level.block.Blocks.FARMLAND)
                || (state.isFaceSturdy(level, pos, Direction.UP) && !state.is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK)
                && state.getSoundType() != net.minecraft.world.level.block.SoundType.STONE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
