package net.kasax.raft.item.custom;

import net.minecraft.server.level.ServerLevel;
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
public class PaddleItem extends TooltipItem {
    public static final int COOLDOWN_TICKS = 30;
    public static final double BOOST = 0.55;
    public static final double MAX_SPEED = 1.1;

    public PaddleItem(Properties properties) {
        super(properties, "tooltip.raft.paddle");
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player.getVehicle() instanceof AbstractBoat boat) || boat.getControllingPassenger() != player) {
            return InteractionResult.PASS;
        }
        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }
        player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
        Vec3 forward = Vec3.directionFromRotation(0.0F, boat.getYRot());
        Vec3 velocity = boat.getDeltaMovement().add(forward.scale(BOOST));
        double speed = velocity.horizontalDistance();
        if (speed > MAX_SPEED) {
            velocity = new Vec3(velocity.x * MAX_SPEED / speed, velocity.y, velocity.z * MAX_SPEED / speed);
        }
        // Boats are simulated by the rider's client; the server only takes the positions it is sent. So the push is
        // applied on the client alone: a velocity set on the server is zeroed by the server's own boat tick, and that
        // zero is sent to the client, which stopped the boat after two blocks.
        if (level.isClientSide()) {
            boat.setDeltaMovement(velocity);
        }
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, boat.getX(), boat.getY(), boat.getZ(), SoundEvents.BOAT_PADDLE_WATER, SoundSource.PLAYERS, 1.0F, 1.0F);
            serverLevel.sendParticles(ParticleTypes.SPLASH, boat.getX() - forward.x, boat.getY() + 0.3, boat.getZ() - forward.z,
                    12, 0.4, 0.1, 0.4, 0.1);
            stack.hurtAndBreak(1, player, hand);
        }
        return InteractionResult.SUCCESS;
    }
}
