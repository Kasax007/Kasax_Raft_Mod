package net.kasax.raft.item.custom;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Sitting in a boat, a right click with the paddle gives the boat a push forward. */
public class PaddleItem extends Item {
    public static final int COOLDOWN_TICKS = 30;
    public static final double BOOST = 0.55;
    public static final double MAX_SPEED = 1.1;

    public PaddleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player.getVehicle() instanceof AbstractBoat boat)) {
            return InteractionResult.PASS;
        }
        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
            Vec3 forward = Vec3.directionFromRotation(0.0F, boat.getYRot());
            Vec3 velocity = boat.getDeltaMovement().add(forward.scale(BOOST));
            double speed = velocity.horizontalDistance();
            if (speed > MAX_SPEED) {
                velocity = new Vec3(velocity.x * MAX_SPEED / speed, velocity.y, velocity.z * MAX_SPEED / speed);
            }
            boat.setDeltaMovement(velocity);
            // The rider's client steers the boat, so it has to be told about the push.
            serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(boat.getId(), velocity));
            boat.needsSync = true;
            serverLevel.playSound(null, boat.getX(), boat.getY(), boat.getZ(), SoundEvents.BOAT_PADDLE_WATER, SoundSource.PLAYERS, 1.0F, 1.0F);
            serverLevel.sendParticles(ParticleTypes.SPLASH, boat.getX() - forward.x, boat.getY() + 0.3, boat.getZ() - forward.z,
                    12, 0.4, 0.1, 0.4, 0.1);
            stack.hurtAndBreak(1, player, hand);
        }
        return InteractionResult.SUCCESS;
    }
}
