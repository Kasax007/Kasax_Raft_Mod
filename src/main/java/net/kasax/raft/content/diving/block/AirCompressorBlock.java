package net.kasax.raft.content.diving.block;

import net.kasax.raft.content.diving.DivingContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.ItemInteractionResult;

/**
 * The air compressor: a small red-painted pump on a steel skid. Use it to open the GUI, use it with a tank or the sea
 * scooter in hand to put it in, sneak-use with an empty hand to turn the hand crank.
 */
public class AirCompressorBlock extends BaseEntityBlock {
    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(AirCompressorBlock::new);
    }

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty RUNNING = BooleanProperty.create("running");
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 0, 0, 16, 2, 16),
            Block.box(2, 2, 2, 14, 11, 14),
            Block.box(4, 11, 4, 12, 15, 12));

    public AirCompressorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(RUNNING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, RUNNING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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
        return SHAPE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AirCompressorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level instanceof ServerLevel serverLevel) {
            return createTickerHelper(type, DivingContent.AIR_COMPRESSOR_BE,
                    (lvl, pos, st, be) -> AirCompressorBlockEntity.serverTick(serverLevel, pos, st, be));
        }
        return null;
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (AirCompressorBlockEntity.accepts(stack) && level.getBlockEntity(pos) instanceof AirCompressorBlockEntity be
                && be.getItem(AirCompressorBlockEntity.SLOT).isEmpty()) {
            if (!level.isClientSide()) {
                be.setItem(AirCompressorBlockEntity.SLOT, stack.split(1));
                level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.6F, 1.4F);
            }
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel serverLevel && level.getBlockEntity(pos) instanceof AirCompressorBlockEntity be) {
            if (player.isShiftKeyDown()) {
                be.crank(serverLevel);
                player.causeFoodExhaustion(0.05F);
            } else if (be instanceof MenuProvider provider) {
                player.openMenu(provider);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof AirCompressorBlockEntity be ? be.comparatorSignal() : 0;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(RUNNING)) {
            return;
        }
        Direction facing = state.getValue(FACING);
        double x = pos.getX() + 0.5 + facing.getStepX() * 0.15;
        double z = pos.getZ() + 0.5 + facing.getStepZ() * 0.15;
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.WHITE_SMOKE, x + (random.nextDouble() - 0.5) * 0.2, pos.getY() + 0.95,
                    z + (random.nextDouble() - 0.5) * 0.2, 0.0, 0.03, 0.0);
        }
        if (level.isWaterAt(pos.above())) {
            level.addParticle(ParticleTypes.BUBBLE_COLUMN_UP, x, pos.getY() + 1.0, z, 0.0, 0.05, 0.0);
        }
    }
}
