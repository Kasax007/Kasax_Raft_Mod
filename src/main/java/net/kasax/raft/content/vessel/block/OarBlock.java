package net.kasax.raft.content.vessel.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;
import net.kasax.raft.compat.VShapes;

/**
 * An oar in a rowlock for a built raft. Set it on the deck at the edge; {@code facing} points outboard, where the
 * blade dips into the water. Every oar on board adds a little rowing power.
 */
public class OarBlock extends HorizontalDirectionalBlock {
    @Override
    protected com.mojang.serialization.MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(OarBlock::new);
    }

    /** Shape for facing=north: the rowlock post at the north edge and the inboard end of the loom. */
    private static final VoxelShape NORTH_SHAPE = Shapes.or(
            Block.box(5.5, 0.0, 0.5, 10.5, 2.0, 5.5),
            Block.box(6.5, 2.0, 1.5, 9.5, 7.0, 4.5),
            Block.box(7.0, 6.0, 4.0, 9.0, 11.0, 14.0));
    private static final Map<Direction, VoxelShape> SHAPES = VShapes.rotateHorizontal(NORTH_SHAPE);

    public OarBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}
