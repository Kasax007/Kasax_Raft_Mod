package net.kasax.raft.content.vessel.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;
import net.kasax.raft.compat.VShapes;

/**
 * A sail for a built raft: a mast with a square of canvas behind it. {@code facing} is the bow. Sails stack: each sail
 * block on board adds wind power, and stacked sails share one mast and one sheet of canvas ({@code top} = no sail
 * above, so the yard is drawn; {@code bottom} = no sail below, so the boom is drawn).
 */
public class SailBlock extends HorizontalDirectionalBlock {
    @Override
    protected com.mojang.serialization.MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(SailBlock::new);
    }

    public static final BooleanProperty TOP = BooleanProperty.create("top");
    public static final BooleanProperty BOTTOM = BooleanProperty.create("bottom");

    /** Shape for facing=north: the mast in the middle, the canvas just aft of it. */
    private static final VoxelShape NORTH_SHAPE = Shapes.or(
            Block.box(7.0, 0.0, 7.0, 9.0, 16.0, 9.0),
            Block.box(1.0, 0.0, 9.0, 15.0, 16.0, 10.5));
    private static final Map<Direction, VoxelShape> SHAPES = VShapes.rotateHorizontal(NORTH_SHAPE);

    public SailBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TOP, true).setValue(BOTTOM, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, TOP, BOTTOM);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        LevelReader level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState below = level.getBlockState(pos.below());
        // a sail set onto another sail takes over its direction, so a mast never twists
        Direction facing = below.getBlock() instanceof SailBlock ? below.getValue(FACING) : context.getHorizontalDirection();
        return defaultBlockState().setValue(FACING, facing)
                .setValue(TOP, !(level.getBlockState(pos.above()).getBlock() instanceof SailBlock))
                .setValue(BOTTOM, !(below.getBlock() instanceof SailBlock));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState, net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (direction == Direction.UP) {
            return state.setValue(TOP, !(neighbourState.getBlock() instanceof SailBlock));
        }
        if (direction == Direction.DOWN) {
            return state.setValue(BOTTOM, !(neighbourState.getBlock() instanceof SailBlock));
        }
        return state;
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
