package net.kasax.raft.item.custom;

import net.kasax.raft.content.audio.AudioSounds;
import net.kasax.raft.content.vessel.VesselColliders;
import net.kasax.raft.content.vessel.VesselEntity;
import net.kasax.raft.content.vessel.VesselPose;
import net.kasax.raft.content.vessel.VesselStructure;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Hold right click to pull floating items that are in front of you to your feet. Since 3.4.2 only items you can see: the
 * hook does not pull through walls, floors or glass (a ray from the eyes to the item against the blocks' collision
 * shapes; water does not stop it), also not through the blocks of a sailing raft.
 */
public class PlasticHookItem extends TooltipItem {
    public static final double RANGE = 12.0;
    /** Cosine of the half angle of the pulling cone (about 30 degrees). */
    private static final double CONE = 0.866;
    private static final double PULL = 0.5;

    public PlasticHookItem(Properties properties) {
        super(properties, "tooltip.raft.plastic_hook");
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return net.kasax.raft.compat.Results.holder(this.use26(level, player, hand), player.getItemInHand(hand));
    }

    private InteractionResult use26(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
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
                item -> item.isAlive() && inCone(eye, look, item.position().add(0.0, 0.2, 0.0)) && inSight(level, user, eye, item));
        Vec3 target = user.position().add(0.0, 0.4, 0.0);
        for (ItemEntity item : items) {
            Vec3 toPlayer = target.subtract(item.position());
            double distance = toPlayer.length();
            // fast from afar, slowing down so that the item stops at the feet of the player
            double speed = Math.min(PULL, distance * 0.25);
            Vec3 wanted = distance < 1.0E-4 ? Vec3.ZERO : toPlayer.scale(speed / distance);
            // the small upward push cancels gravity, so the item does not sink while it is pulled
            item.setDeltaMovement(item.getDeltaMovement().scale(0.4).add(wanted.scale(0.6)).add(0.0, 0.04, 0.0));
            item.hurtMarked = true;
        }
        if (!items.isEmpty() && remainingUseTicks % 10 == 0) {
            serverLevel.playSound(null, user.getX(), user.getY(), user.getZ(), AudioSounds.PLASTIC_HOOK_REEL, SoundSource.PLAYERS,
                    0.7F, 0.9F + user.getRandom().nextFloat() * 0.2F);
        }
        if (!items.isEmpty() && remainingUseTicks % 20 == 0) {
            stack.hurtAndBreak(1, user, net.minecraft.world.entity.LivingEntity.getSlotForHand(user.getUsedItemHand()));
            if (stack.isEmpty()) {
                user.stopUsingItem();
            }
        }
    }

    /**
     * Whether nothing solid is between the eyes and the item: the middle or the top of the item must be in clear view
     * (the top too, so an item lying close behind a low edge such as the side of a raft is still pulled).
     */
    private static boolean inSight(Level level, LivingEntity user, Vec3 eye, ItemEntity item) {
        Vec3 middle = item.position().add(0.0, item.getBbHeight() * 0.5, 0.0);
        Vec3 top = item.position().add(0.0, item.getBbHeight(), 0.0);
        return clear(level, user, eye, middle) || clear(level, user, eye, top);
    }

    private static boolean clear(Level level, LivingEntity user, Vec3 eye, Vec3 target) {
        return level.clip(new ClipContext(eye, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, user)).getType()
                == HitResult.Type.MISS && clearOfRafts(level, eye, target);
    }

    /**
     * A sailing raft's blocks are not in the world while it sails (they wait in its shipyard plot): the ray is checked
     * against the blocks of every sailing raft it passes too, in the raft's own frame.
     */
    private static boolean clearOfRafts(Level level, Vec3 eye, Vec3 target) {
        for (VesselEntity vessel : VesselColliders.in(level)) {
            VesselStructure structure = vessel.structure();
            if (structure == null || vessel.isDocked()) {
                continue;
            }
            AABB box = vessel.getBoundingBox().inflate(1.0);
            if (!box.contains(eye) && box.clip(eye, target).isEmpty()) {
                continue;
            }
            VesselPose pose = vessel.currentPose();
            BlockHitResult hit = structure.clipCollision(pose.toLocal(eye), pose.toLocal(target));
            if (hit != null && hit.getType() != HitResult.Type.MISS) {
                return false;
            }
        }
        return true;
    }

    private static boolean inCone(Vec3 eye, Vec3 look, Vec3 point) {
        Vec3 to = point.subtract(eye);
        double distance = to.length();
        return distance <= RANGE && distance > 0.5 && to.scale(1.0 / distance).dot(look) >= CONE;
    }
}
