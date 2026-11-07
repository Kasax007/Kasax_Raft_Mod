package net.kasax.raft.content.decor.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/** One to three rum bottles ({@code bottles=1..3}, {@code facing}, {@code waterlogged}); add bottles like sea pickles. */
public class RumBottlesBlock extends DecorBlock {
    public static final IntegerProperty BOTTLES = IntegerProperty.create("bottles", 1, 3);
    private final Map<Direction, VoxelShape>[] shapes;

    @SuppressWarnings("unchecked")
    public RumBottlesBlock(Properties properties, Config config) {
        super(properties, config);
        shapes = new Map[4];
        for (int i = 1; i <= 3; i++) shapes[i] = DecorBlock.shapes("rum_bottles_" + i, true);
    }

    @Override
    protected BlockState defaultState(BlockState state) {
        return state.setValue(BOTTLES, 1);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BOTTLES);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes[state.getValue(BOTTLES)].get(state.getValue(FACING));
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return !context.isSecondaryUseActive() && context.getItemInHand().is(asItem()) && state.getValue(BOTTLES) < 3
                || super.canBeReplaced(state, context);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = context.getLevel().getBlockState(context.getClickedPos());
        if (state.is(this)) return state.setValue(BOTTLES, Math.min(3, state.getValue(BOTTLES) + 1));
        return super.getStateForPlacement(context);
    }
}
