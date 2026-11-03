package net.kasax.raft.content.decor.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/** A thin glowing neon tube along an axis ({@code axis}, {@code waterlogged}); tubes in a row form a long line. */
public class NeonTubeBlock extends RotatedPillarBlock implements SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private final Map<Direction.Axis, VoxelShape> shapes;

    public NeonTubeBlock(Properties properties, String shape) {
        super(properties);
        this.shapes = axisShapes(DecorShapeData.get(shape));
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.Y).setValue(WATERLOGGED, false));
    }

    /** Turns the boxes of the upright model like the blockstate does: z = x 90, x = x 90 then y 90. */
    static Map<Direction.Axis, VoxelShape> axisShapes(double[][] upright) {
        VoxelShape y = Shapes.empty();
        VoxelShape z = Shapes.empty();
        VoxelShape x = Shapes.empty();
        for (double[] b : upright) {
            y = Shapes.or(y, Block.box(b[0], b[1], b[2], b[3], b[4], b[5]));
            double[] zb = {b[0], b[2], 16 - b[4], b[3], b[5], 16 - b[1]};
            z = Shapes.or(z, Block.box(zb[0], zb[1], zb[2], zb[3], zb[4], zb[5]));
            x = Shapes.or(x, Block.box(16 - zb[5], zb[1], zb[0], 16 - zb[2], zb[4], zb[3]));
        }
        Map<Direction.Axis, VoxelShape> map = new EnumMap<>(Direction.Axis.class);
        map.put(Direction.Axis.X, x.optimize());
        map.put(Direction.Axis.Y, y.optimize());
        map.put(Direction.Axis.Z, z.optimize());
        return map;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes.get(state.getValue(AXIS));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(AXIS, context.getClickedFace().getAxis())
                .setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).is(Fluids.WATER));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState,
                                     RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return !state.getValue(WATERLOGGED);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}
