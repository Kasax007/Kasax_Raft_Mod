package net.kasax.raft.content.world.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import net.kasax.raft.compat.VShapes;

/**
 * A few small things lying on the ground (seashells: {@code shells=1..4}, sea glass pebbles: {@code pebbles=1..3}).
 * Placing the same item again adds one more, like sea pickles. Works in and out of water.
 */
public abstract class ScatterBlock extends Block implements SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private final VoxelShape[] shapes;

    protected ScatterBlock(Properties properties, VoxelShape... shapesByCount) {
        super(properties);
        this.shapes = shapesByCount;
    }

    public abstract IntegerProperty countProperty();

    public int maxCount() {
        return shapes.length;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState existing = context.getLevel().getBlockState(context.getClickedPos());
        if (existing.is(this)) {
            return existing.setValue(countProperty(), Math.min(maxCount(), existing.getValue(countProperty()) + 1));
        }
        FluidState fluid = context.getLevel().getFluidState(context.getClickedPos());
        BlockState state = defaultBlockState().setValue(WATERLOGGED, fluid.is(FluidTags.WATER) && fluid.isSource());
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return !context.isSecondaryUseActive() && context.getItemInHand().is(asItem())
                && state.getValue(countProperty()) < maxCount() || super.canBeReplaced(state, context);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState, net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (!state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, direction, neighbourState, level, pos, neighbourPos);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes[Math.max(0, Math.min(shapes.length, state.getValue(countProperty())) - 1)];
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, net.minecraft.world.level.BlockGetter level121, BlockPos pos121) {
        return true;
    }

    /** Seashells: 1 to 4 shells. */
    public static class Seashells extends ScatterBlock {
        public static final IntegerProperty SHELLS = IntegerProperty.create("shells", 1, 4);

        public Seashells(Properties properties) {
            super(properties, VShapes.column(6.0, 0.0, 3.0), VShapes.column(10.0, 0.0, 3.0), VShapes.column(12.0, 0.0, 3.0),
                    VShapes.column(14.0, 0.0, 3.0));
            registerDefaultState(stateDefinition.any().setValue(SHELLS, 1).setValue(WATERLOGGED, false));
        }

        @Override
        public IntegerProperty countProperty() {
            return SHELLS;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(SHELLS, WATERLOGGED);
        }
    }

    /** Sea glass on the beach: 1 to 3 pebbles. */
    public static class SeaGlass extends ScatterBlock {
        public static final IntegerProperty PEBBLES = IntegerProperty.create("pebbles", 1, 3);

        public SeaGlass(Properties properties) {
            super(properties, VShapes.column(6.0, 0.0, 2.0), VShapes.column(10.0, 0.0, 2.0), VShapes.column(14.0, 0.0, 2.0));
            registerDefaultState(stateDefinition.any().setValue(PEBBLES, 1).setValue(WATERLOGGED, false));
        }

        @Override
        public IntegerProperty countProperty() {
            return PEBBLES;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(PEBBLES, WATERLOGGED);
        }
    }
}
