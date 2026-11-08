package net.kasax.raft.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

/**
 * A raft foundation: a full block that floats in the sea. A foundation set into the sea keeps the sea's water
 * ({@code waterlogged}), so when it is broken (by a player, a shark or an explosion) the sea closes the gap with still
 * water instead of a patch of flowing water between the other foundations. The water is sealed inside the full
 * block: it neither shows nor spreads, and buckets cannot take it out or put it in.
 */
public class FoundationBlock extends Block implements SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public FoundationBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
    }

    /**
     * Does this foundation float in the sea: open still water (water blocks, not water held by other blocks, so nothing
     * on a sailing raft counts) under it or beside it?
     */
    public static boolean inSea(BlockGetter level, BlockPos pos) {
        if (openWater(level, pos.below())) {
            return true;
        }
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (openWater(level, pos.relative(d))) {
                return true;
            }
        }
        return false;
    }

    private static boolean openWater(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos).is(Blocks.WATER) && level.getFluidState(pos).isSourceOfType(Fluids.WATER);
    }

    /** A foundation state for this spot: waterlogged when the spot holds still water (the sea) when it is set. */
    public static BlockState settle(BlockState state, BlockGetter level, BlockPos pos) {
        return state.hasProperty(WATERLOGGED) ? state.setValue(WATERLOGGED, level.getFluidState(pos).isSourceOfType(Fluids.WATER)) : state;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return settle(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    /**
     * About to be broken: a foundation from before 3.4 (never waterlogged) that floats in the sea takes on the sea's
     * water now, so the gap fills with still water like that of a new one.
     */
    public static void soak(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!level.isClientSide && state.getBlock() instanceof FoundationBlock && !state.getValue(WATERLOGGED) && inSea(level, pos)) {
            level.setBlock(pos, state.setValue(WATERLOGGED, true), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        soak(level, pos);
        return super.playerWillDestroy(level, pos, level.getBlockState(pos).is(this) ? level.getBlockState(pos) : state, player);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public boolean canPlaceLiquid(@Nullable Player user, BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
        return false;
    }

    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluidState) {
        return false;
    }

    @Override
    public ItemStack pickupBlock(@Nullable Player user, LevelAccessor level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }
}
