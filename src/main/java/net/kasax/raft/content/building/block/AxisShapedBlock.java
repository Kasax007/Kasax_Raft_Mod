package net.kasax.raft.content.building.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
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

/**
 * A non-full block that lies along an axis ({@code axis=x|y|z}) and can be waterlogged: steel beams and rusty pipes.
 * Its shape is given for the upright model ({@code axis=y}); the blockstate turns that model with {@code x=90} for
 * {@code axis=z} and {@code x=90, y=90} for {@code axis=x}, and {@link #axisShapes} turns the boxes the same way.
 */
public class AxisShapedBlock extends RotatedPillarBlock implements SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private final Map<Direction.Axis, VoxelShape> shapes;

    /** @param uprightBoxes boxes {x0, y0, z0, x1, y1, z1} in pixels of the {@code axis=y} model */
    public AxisShapedBlock(Properties properties, double[]... uprightBoxes) {
        super(properties);
        this.shapes = axisShapes(uprightBoxes);
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.Y).setValue(WATERLOGGED, false));
    }

    /** The shape of each axis, turned exactly like the blockstate turns the upright model. */
    public static Map<Direction.Axis, VoxelShape> axisShapes(double[]... uprightBoxes) {
        VoxelShape y = Shapes.empty();
        VoxelShape z = Shapes.empty();
        VoxelShape x = Shapes.empty();
        for (double[] b : uprightBoxes) {
            y = Shapes.or(y, Block.box(b[0], b[1], b[2], b[3], b[4], b[5]));
            // x=90: (x, y, z) -> (x, z, 16 - y)
            double[] zb = {b[0], b[2], 16 - b[4], b[3], b[5], 16 - b[1]};
            z = Shapes.or(z, Block.box(zb[0], zb[1], zb[2], zb[3], zb[4], zb[5]));
            // then y=90: (x, y, z) -> (16 - z, y, x)
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
        FluidState fluid = context.getLevel().getFluidState(context.getClickedPos());
        return defaultBlockState().setValue(AXIS, context.getClickedFace().getAxis())
                .setValue(WATERLOGGED, fluid.is(Fluids.WATER));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction directionToNeighbour, BlockState neighbourState, net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, directionToNeighbour, neighbourState, level, pos, neighbourPos);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, net.minecraft.world.level.BlockGetter level121, BlockPos pos121) {
        return !state.getValue(WATERLOGGED);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}
