package net.kasax.raft.content.decor.block;

import net.kasax.raft.content.audio.AudioSounds;
import net.kasax.raft.content.decor.DecorBlocks;
import net.kasax.raft.content.decor.entity.CannonballEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * A deck cannon ({@code facing}, {@code waterlogged}). Using a cannonball on it fires the ball out of the muzzle
 * (the facing side) with smoke, a flash and a bang. The ball flies about 40 blocks and never breaks blocks.
 */
public class ShipCannonBlock extends DecorBlock {
    private static final int COOLDOWN = 20;

    public ShipCannonBlock(Properties properties, Config config) {
        super(properties, config);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        // the muzzle points where the player looks
        return state == null ? null : state.setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(DecorBlocks.CANNONBALL)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (state.getValue(WATERLOGGED)) return InteractionResult.FAIL;
        if (player.getCooldowns().isOnCooldown(stack)) return InteractionResult.FAIL;
        if (level instanceof ServerLevel serverLevel) {
            fire(serverLevel, pos, state.getValue(FACING), player);
            stack.consume(1, player);
            player.getCooldowns().addCooldown(stack, COOLDOWN);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            player.sendOverlayMessage(Component.translatable("message.raft.ship_cannon.empty"));
        }
        return InteractionResult.SUCCESS;
    }

    /** Fires a cannonball out of the muzzle of the cannon at {@code pos}. */
    public static void fire(ServerLevel level, BlockPos pos, Direction facing, Player shooter) {
        double mx = pos.getX() + 0.5 + facing.getStepX() * 0.75;
        double my = pos.getY() + 0.53;
        double mz = pos.getZ() + 0.5 + facing.getStepZ() * 0.75;
        CannonballEntity ball = new CannonballEntity(level, mx, my - 0.125, mz, shooter);
        ball.shoot(facing.getStepX(), 0.08, facing.getStepZ(), 2.1F, 0.6F);
        level.addFreshEntity(ball);
        level.sendParticles(ParticleTypes.EXPLOSION, mx + facing.getStepX() * 0.4, my, mz + facing.getStepZ() * 0.4, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, mx + facing.getStepX() * 0.5, my, mz + facing.getStepZ() * 0.5, 14,
                0.2 + Math.abs(facing.getStepX()) * 0.3, 0.15, 0.2 + Math.abs(facing.getStepZ()) * 0.3, 0.03);
        level.sendParticles(ParticleTypes.FLAME, mx + facing.getStepX() * 0.3, my, mz + facing.getStepZ() * 0.3, 6, 0.08, 0.08, 0.08, 0.04);
        level.playSound(null, mx, my, mz, AudioSounds.CANNON_FIRE, SoundSource.BLOCKS, 2.0F, 0.92F + level.getRandom().nextFloat() * 0.16F);
        level.gameEvent(shooter, GameEvent.PROJECTILE_SHOOT, pos);
    }
}
