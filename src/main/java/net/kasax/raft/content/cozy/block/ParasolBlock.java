package net.kasax.raft.content.cozy.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.jspecify.annotations.Nullable;

/**
 * The thatch parasol (Beach Craft 3.4): three blocks tall, so a player can stand under the canopy. The base is
 * {@code half=lower}; above it the pole ({@code half=upper, canopy=false}) and the canopy on top
 * ({@code half=upper, canopy=true}). A parasol placed before 3.4 (two blocks: base and canopy) stays valid. Any part
 * that is broken takes the whole parasol with it; only the base drops the item.
 */
public class ParasolBlock extends CozyBlock {
    public static final BooleanProperty CANOPY = BooleanProperty.create("canopy");

    public ParasolBlock(Properties properties, Config config) {
        super(properties, config);
    }

    @Override
    protected BlockState defaultState(BlockState state) {
        // parasols saved before 3.4 have no canopy property: their upper half is the canopy
        return state.setValue(CANOPY, true);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(CANOPY);
    }

    @Override
    protected String shapeName(BlockState state) {
        if (state.getValue(HALF) == DoubleBlockHalf.LOWER) return "thatch_parasol_lower";
        return state.getValue(CANOPY) ? "thatch_parasol_upper" : "thatch_parasol_middle";
    }

    private static boolean lower(BlockState s) {
        return s.getValue(HALF) == DoubleBlockHalf.LOWER;
    }

    private static boolean pole(BlockState s) {
        return s.getValue(HALF) == DoubleBlockHalf.UPPER && !s.getValue(CANOPY);
    }

    private static boolean canopy(BlockState s) {
        return s.getValue(HALF) == DoubleBlockHalf.UPPER && s.getValue(CANOPY);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (pos.getY() + 2 > level.getMaxY() || !level.getBlockState(pos.above()).canBeReplaced(context)
                || !level.getBlockState(pos.above(2)).canBeReplaced(context)) {
            return null;
        }
        return defaultBlockState().setValue(HALF, DoubleBlockHalf.LOWER).setValue(CANOPY, false);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack itemStack) {
        BlockState upper = state.setValue(HALF, DoubleBlockHalf.UPPER);
        level.setBlock(pos.above(2), upper.setValue(CANOPY, true), Block.UPDATE_ALL);
        level.setBlock(pos.above(), upper.setValue(CANOPY, false), Block.UPDATE_ALL);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (lower(state)) return true;
        BlockState below = level.getBlockState(pos.below());
        if (!below.is(this)) return false;
        // the pole stands on the base; the canopy on the pole (or, for a parasol from before 3.4, on the base)
        return pole(state) ? lower(below) : lower(below) || pole(below);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState,
                                     RandomSource random) {
        if (directionToNeighbour == Direction.UP && !canopy(state)) {
            // the base and the pole need the next part above them
            boolean held = neighbourState.is(this) && neighbourState.getValue(HALF) == DoubleBlockHalf.UPPER
                    && (pole(state) ? canopy(neighbourState) : true);
            if (!held) return Blocks.AIR.defaultBlockState();
        }
        if (directionToNeighbour == Direction.DOWN && !lower(state) && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player.preventsBlockDrops() && !lower(state)) {
            // creative: take the base away first, without drops (it would drop the item when the rest falls apart)
            BlockPos base = pos.below();
            BlockState s = level.getBlockState(base);
            if (s.is(this) && pole(s)) {
                base = base.below();
                s = level.getBlockState(base);
            }
            if (s.is(this) && lower(s)) {
                level.setBlock(base, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                level.levelEvent(player, 2001, base, Block.getId(s));
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected long getSeed(BlockState state, BlockPos pos) {
        int down = lower(state) ? 0 : pole(state) ? 1 : 2;
        return net.minecraft.util.Mth.getSeed(pos.getX(), pos.getY() - down, pos.getZ());
    }
}
