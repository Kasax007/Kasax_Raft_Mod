package net.kasax.raft.content.world.block;

import net.kasax.raft.content.world.WorldBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Giant clam of the reefs. Under water it slowly opens and closes; an open clam with a pearl gives the pearl on
 * right-click and snaps shut. A new pearl grows in about ten minutes (only under water). Stepping on an open clam
 * makes it snap shut too. The hinge is at the back, the opening faces {@code facing}.
 */
public class GiantClamBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock {
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final BooleanProperty PEARL = BooleanProperty.create("pearl");
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    /** Ticks until a harvested clam has a new pearl (about ten minutes, a bit random). */
    public static final int REGROW_TICKS = 12000;

    private static final VoxelShape CLOSED = Block.column(14.0, 0.0, 7.0);
    private static final Map<Direction, VoxelShape> OPEN_SHAPES = Shapes.rotateHorizontal(
            Shapes.or(Block.column(14.0, 0.0, 4.0), Block.box(1.0, 4.0, 11.0, 15.0, 13.0, 15.0)));

    public GiantClamBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, true)
                .setValue(PEARL, true).setValue(WATERLOGGED, false));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluid = context.getLevel().getFluidState(context.getClickedPos());
        boolean water = fluid.is(FluidTags.WATER) && fluid.isSource();
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(WATERLOGGED, water).setValue(OPEN, false).setValue(PEARL, false);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!state.getValue(PEARL) && !oldState.is(this)) {
            level.scheduleTick(pos, this, REGROW_TICKS + level.getRandom().nextInt(2400));
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(PEARL)) {
            return;
        }
        if (state.getValue(WATERLOGGED)) {
            level.setBlock(pos, state.setValue(PEARL, true), 2);
        } else {
            level.scheduleTick(pos, this, REGROW_TICKS / 4);
        }
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean open = state.getValue(OPEN);
        if (!state.getValue(WATERLOGGED)) {
            if (open) {
                setOpen(level, pos, state, false);
            }
        } else if (random.nextInt(4) == 0) {
            setOpen(level, pos, state, !open);
        }
    }

    private static void setOpen(Level level, BlockPos pos, BlockState state, boolean open) {
        level.setBlock(pos, state.setValue(OPEN, open), 3);
        level.playSound(null, pos, open ? SoundEvents.SHULKER_OPEN : SoundEvents.SHULKER_CLOSE, SoundSource.BLOCKS,
                0.6F, 0.7F + level.getRandom().nextFloat() * 0.2F);
        level.gameEvent(null, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!state.getValue(OPEN)) {
            if (!level.isClientSide()) player.sendOverlayMessage(Component.translatable("block.raft.giant_clam.closed"));
            return InteractionResult.PASS;
        }
        if (!state.getValue(PEARL)) {
            if (!level.isClientSide()) player.sendOverlayMessage(Component.translatable("block.raft.giant_clam.empty"));
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel serverLevel) {
            ItemStack pearl = new ItemStack(WorldBlocks.PEARL);
            if (!player.getInventory().add(pearl)) {
                Block.popResourceFromFace(serverLevel, pos, Direction.UP, pearl);
            }
            BlockState harvested = state.setValue(PEARL, false).setValue(OPEN, false);
            serverLevel.setBlock(pos, harvested, 3);
            serverLevel.playSound(null, pos, SoundEvents.SHULKER_CLOSE, SoundSource.BLOCKS, 0.8F, 0.8F);
            serverLevel.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.4F, 1.4F);
            serverLevel.scheduleTick(pos, this, REGROW_TICKS + serverLevel.getRandom().nextInt(2400));
            serverLevel.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide() && state.getValue(OPEN) && entity instanceof LivingEntity && !entity.isSteppingCarefully()) {
            setOpen(level, pos, state, false);
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(OPEN) ? OPEN_SHAPES.get(state.getValue(FACING)) : CLOSED;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
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
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN, PEARL, WATERLOGGED);
    }
}
