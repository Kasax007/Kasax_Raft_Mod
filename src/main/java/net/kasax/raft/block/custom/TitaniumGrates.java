package net.kasax.raft.block.custom;

import net.kasax.raft.block.entity.ItemCollectorBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Waterloggable;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldAccess;

public class TitaniumGrates extends Block implements Waterloggable {
    public TitaniumGrates(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState()
                //.with(Properties.HORIZONTAL_FACING, Direction.NORTH)
                .with(WATERLOGGED, false));
    }

    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(Properties.WATERLOGGED);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return (BlockState)this.getDefaultState()
                .with(WATERLOGGED, ctx.getWorld().getFluidState(ctx.getBlockPos()).isOf(Fluids.WATER));
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (state.get(WATERLOGGED)) {
            world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }

        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }
}
