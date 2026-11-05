package net.kasax.raft.item.custom;

import net.kasax.raft.content.audio.AudioSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Hold right click to pull floating items that are in front of you to your feet. */
public class PlasticHookItem extends TooltipItem {
    public static final double RANGE = 12.0;
    /** Cosine of the half angle of the pulling cone (about 30 degrees). */
    private static final double CONE = 0.866;
    private static final double PULL = 0.5;

    public PlasticHookItem(Properties properties) {
        super(properties, "tooltip.raft.plastic_hook");
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 eye = user.getEyePosition();
        Vec3 look = user.getLookAngle();
        AABB area = new AABB(eye, eye.add(look.scale(RANGE))).inflate(RANGE * 0.5);
        List<ItemEntity> items = serverLevel.getEntitiesOfClass(ItemEntity.class, area,
                item -> item.isAlive() && inCone(eye, look, item.position().add(0.0, 0.2, 0.0)));
        Vec3 target = user.position().add(0.0, 0.4, 0.0);
        for (ItemEntity item : items) {
            Vec3 toPlayer = target.subtract(item.position());
            double distance = toPlayer.length();
            // fast from afar, slowing down so that the item stops at the feet of the player
            double speed = Math.min(PULL, distance * 0.25);
            Vec3 wanted = distance < 1.0E-4 ? Vec3.ZERO : toPlayer.scale(speed / distance);
            // the small upward push cancels gravity, so the item does not sink while it is pulled
            item.setDeltaMovement(item.getDeltaMovement().scale(0.4).add(wanted.scale(0.6)).add(0.0, 0.04, 0.0));
            item.needsSync = true;
        }
        if (!items.isEmpty() && remainingUseTicks % 10 == 0) {
            serverLevel.playSound(null, user.getX(), user.getY(), user.getZ(), AudioSounds.PLASTIC_HOOK_REEL, SoundSource.PLAYERS,
                    0.7F, 0.9F + user.getRandom().nextFloat() * 0.2F);
        }
        if (!items.isEmpty() && remainingUseTicks % 20 == 0) {
            stack.hurtAndBreak(1, user, user.getUsedItemHand());
            if (stack.isEmpty()) {
                user.stopUsingItem();
            }
        }
    }

    private static boolean inCone(Vec3 eye, Vec3 look, Vec3 point) {
        Vec3 to = point.subtract(eye);
        double distance = to.length();
        return distance <= RANGE && distance > 0.5 && to.scale(1.0 / distance).dot(look) >= CONE;
    }
}
