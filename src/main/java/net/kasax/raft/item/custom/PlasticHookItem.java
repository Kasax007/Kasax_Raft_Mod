package net.kasax.raft.item.custom;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
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
    private static final double PULL = 0.3;

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
            if (toPlayer.lengthSqr() < 0.25) {
                continue;
            }
            Vec3 velocity = item.getDeltaMovement().scale(0.6).add(toPlayer.normalize().scale(PULL));
            item.setDeltaMovement(velocity);
            item.setNoGravity(true);
            item.needsSync = true;
        }
        if (!items.isEmpty() && remainingUseTicks % 20 == 0) {
            stack.hurtAndBreak(1, user, EquipmentSlot.MAINHAND);
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
