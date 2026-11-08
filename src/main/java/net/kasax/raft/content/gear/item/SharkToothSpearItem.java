package net.kasax.raft.content.gear.item;

import net.kasax.raft.Raft;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 1.21.1 port: the Shark Tooth Spear. The newer game builds it on its own spear mechanics, which 1.21.1 does not have;
 * this weapon keeps the feel with the same numbers where they exist:
 * <ul>
 * <li>Jab (attack): 3 damage like the iron spear, a slow stab (about one per second) that reaches 4.5 blocks.</li>
 * <li>Charge (hold use): the spear is raised like a trident. After 0.6 s it is charged; moving into a mob while
 * charged (running, falling or riding at 4.6 blocks a second or more) strikes it for 3 + 0.95 x your speed and
 * knocks it back from 5.1 blocks a second; at 11 blocks a second it also knocks a rider off. Each mob is struck once
 * per charge.</li>
 * <li>Thrust (let go of a charged spear): a lunge forward that hits the first mob within 4.5 blocks for 3 damage plus
 * up to 3 more the longer it was charged (full after 1.5 s), with knockback.</li>
 * </ul>
 * 320 uses, repaired with shark teeth, enchantable like a sword's durability.
 */
public class SharkToothSpearItem extends TieredItem {
    public static final float JAB_DAMAGE = 3.0F;
    public static final double REACH = 4.5;
    public static final int CHARGE_DELAY = 12;
    private static final int CHARGE_MAX = 225;
    private static final int FULL_THRUST = 30;
    private static final double DAMAGE_SPEED = 4.6, KNOCKBACK_SPEED = 5.1, DISMOUNT_SPEED = 11.0;
    private static final int DISMOUNT_TICKS = 50, KNOCKBACK_TICKS = 135;
    private static final float SPEED_DAMAGE = 0.95F;

    public SharkToothSpearItem(Tier tier, Properties properties) {
        super(tier, properties.attributes(attributes(tier)));
    }

    /** Iron spear numbers of the newer game: +2 damage (3 in all), 1.05 attacks a second, 4.5 blocks reach. */
    public static ItemAttributeModifiers attributes(Tier tier) {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, tier.getAttackDamageBonus(),
                        AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, 1.0F / 0.95F - 4.0,
                        AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(Raft.id("spear_reach"), REACH - 3.0,
                        AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build();
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        return true;
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
    }

    @Override
    public boolean canAttackBlock(net.minecraft.world.level.block.state.BlockState state, Level level, net.minecraft.core.BlockPos pos, Player player) {
        return !player.isCreative();
    }

    // ------------------------------------------------------------------------------------------------ charge
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getDamageValue() >= stack.getMaxDamage() - 1) return InteractionResultHolder.fail(stack);
        player.startUsingItem(hand);
        if (!level.isClientSide) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 0.6F, 1.4F);
        }
        return InteractionResultHolder.consume(stack);
    }

    /** Blocks a second the user moves (its vehicle when riding). */
    private static Vec3 velocity(LivingEntity user) {
        Entity mover = user.getVehicle() != null ? user.getRootVehicle() : user;
        return new Vec3(mover.getX() - mover.xo, mover.getY() - mover.yo, mover.getZ() - mover.zo).scale(20.0);
    }

    private static Vec3 velocityOf(Entity e) {
        return new Vec3(e.getX() - e.xo, e.getY() - e.yo, e.getZ() - e.zo).scale(20.0);
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (!(level instanceof ServerLevel server)) return;
        int used = getUseDuration(stack, user) - remaining;
        if (used < CHARGE_DELAY || used > CHARGE_MAX) return;
        if (used == CHARGE_DELAY) {
            server.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.TRIDENT_RIPTIDE_1, SoundSource.PLAYERS, 0.4F, 1.6F);
        }
        Vec3 look = user.getViewVector(1.0F);
        Vec3 own = velocity(user);
        EntityHitResult hit = target(user, REACH);
        if (hit == null || !(hit.getEntity() instanceof LivingEntity target)) return;
        long id = chargeId(user);
        Charge charge = CHARGES.get(user);
        if (charge == null || charge.id != id) CHARGES.put(user, charge = new Charge(id, new java.util.HashSet<>()));
        if (charge.struck.contains(target.getId())) return;
        double relative = own.subtract(velocityOf(target)).dot(look);
        double speed = own.dot(look);
        if (relative < DAMAGE_SPEED) return;
        charge.struck.add(target.getId());
        float damage = JAB_DAMAGE + SPEED_DAMAGE * (float) relative;
        DamageSource source = user instanceof Player p ? user.damageSources().playerAttack(p) : user.damageSources().mobAttack(user);
        if (target.hurt(source, damage)) {
            if (speed >= KNOCKBACK_SPEED && used <= KNOCKBACK_TICKS) target.knockback(0.6, -look.x, -look.z);
            if (speed >= DISMOUNT_SPEED && used <= DISMOUNT_TICKS && target.isPassenger()) target.stopRiding();
            server.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 1.0F, 1.1F);
            stack.hurtAndBreak(1, user, user.getUsedItemHand() == InteractionHand.OFF_HAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int remaining) {
        int used = getUseDuration(stack, user) - remaining;
        if (used < CHARGE_DELAY) return;
        Vec3 look = user.getViewVector(1.0F);
        if (user.getVehicle() == null) {
            // the lunge: a step forward along the ground
            Vec3 push = new Vec3(look.x, 0.0, look.z).normalize().scale(0.55);
            user.push(push.x, user.onGround() ? 0.1 : 0.0, push.z);
            user.hurtMarked = true;
        }
        if (!(level instanceof ServerLevel server)) return;
        server.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 0.8F, 1.3F);
        if (user instanceof Player p) p.getCooldowns().addCooldown(this, 10);
        EntityHitResult hit = target(user, REACH);
        if (hit == null || !(hit.getEntity() instanceof LivingEntity target)) return;
        float charge = Math.min(1.0F, (used - CHARGE_DELAY) / (float) (FULL_THRUST - CHARGE_DELAY));
        float damage = JAB_DAMAGE + 3.0F * charge;
        DamageSource source = user instanceof Player p ? user.damageSources().playerAttack(p) : user.damageSources().mobAttack(user);
        if (target.hurt(source, damage)) {
            target.knockback(0.5 + 0.4 * charge, -look.x, -look.z);
            server.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 1.0F, 1.0F);
            stack.hurtAndBreak(1, user, user.getUsedItemHand() == InteractionHand.OFF_HAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND);
        }
    }

    /** The first living thing the user aims at within the distance, not behind a block. */
    private static EntityHitResult target(LivingEntity user, double reach) {
        Vec3 eye = user.getEyePosition();
        Vec3 look = user.getViewVector(1.0F);
        HitResult block = user.pick(reach, 1.0F, false);
        double max = block.getType() == HitResult.Type.MISS ? reach : block.getLocation().distanceTo(eye);
        Vec3 end = eye.add(look.scale(max));
        AABB area = user.getBoundingBox().expandTowards(look.scale(max)).inflate(1.0);
        return ProjectileUtil.getEntityHitResult(user, eye, end, area,
                e -> e instanceof LivingEntity && e.isAlive() && e.isPickable() && !e.isSpectator() && e != user
                        && !user.isPassengerOfSameVehicle(e), max * max);
    }

    /** The mobs struck during one charge (server side). */
    private record Charge(long id, java.util.Set<Integer> struck) {
    }

    private static final java.util.Map<LivingEntity, Charge> CHARGES = new java.util.WeakHashMap<>();

    /** One id per charge (the tick the use started), so every mob is struck once per charge. */
    private static long chargeId(LivingEntity user) {
        return user.level().getGameTime() - (user.getUseItem().getUseDuration(user) - user.getUseItemRemainingTicks());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.raft.shark_tooth_spear").withStyle(ChatFormatting.AQUA));
        super.appendHoverText(stack, context, lines, flag);
    }
}
