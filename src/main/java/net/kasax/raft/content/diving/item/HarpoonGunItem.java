package net.kasax.raft.content.diving.item;

import net.kasax.raft.content.diving.DivingContent;
import net.kasax.raft.content.diving.DivingSounds;
import net.kasax.raft.content.diving.entity.Harpoon;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

/**
 * Fires a harpoon on use (one harpoon from the inventory, free in creative) and reloads for one second. Under water the
 * harpoon keeps its speed; it is meant for sharks, eels and drowned.
 */
public class HarpoonGunItem extends Item {
    public static final int RELOAD_TICKS = 20;
    public static final float SPEED = 2.6F;

    public HarpoonGunItem(Properties properties) {
        super(properties);
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return net.kasax.raft.compat.Results.holder(this.use26(level, player, hand), player.getItemInHand(hand));
    }

    private InteractionResult use26(Level level, Player player, InteractionHand hand) {
        ItemStack gun = player.getItemInHand(hand);
        ItemStack ammo = findAmmo(player);
        if (ammo.isEmpty() && !player.hasInfiniteMaterials()) {
            player.playSound(net.minecraft.sounds.SoundEvents.DISPENSER_FAIL, 0.6F, 1.4F);
            return InteractionResult.FAIL;
        }
        if (level instanceof ServerLevel serverLevel) {
            ItemStack pickup = ammo.isEmpty() ? new ItemStack(DivingContent.HARPOON_ITEM) : ammo.copyWithCount(1);
            Harpoon harpoon = new Harpoon(serverLevel, player, pickup, gun);
            harpoon.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, SPEED, 0.5F);
            if (player.hasInfiniteMaterials()) {
                harpoon.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
            }
            serverLevel.addFreshEntity(harpoon);
            gun.hurtAndBreak(1, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(hand));
            if (!player.hasInfiniteMaterials()) {
                ammo.shrink(1);
            }
            // a puff at the muzzle: beside and below the line of sight, so it does not cover the crosshair
            Vec3 look = player.getLookAngle();
            Vec3 side = look.cross(new Vec3(0.0, 1.0, 0.0));
            side = side.lengthSqr() < 1.0E-4 ? Vec3.ZERO : side.normalize();
            boolean rightHand = (hand == InteractionHand.MAIN_HAND) == (player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT);
            Vec3 muzzle = player.getEyePosition().add(look.scale(1.2)).add(side.scale(rightHand ? 0.4 : -0.4)).add(0.0, -0.3, 0.0);
            boolean wet = player.isUnderWater();
            serverLevel.sendParticles(wet ? ParticleTypes.BUBBLE : ParticleTypes.POOF, muzzle.x, muzzle.y, muzzle.z,
                    wet ? 5 : 2, 0.05, 0.05, 0.05, wet ? 0.05 : 0.01);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), DivingSounds.HARPOON_GUN_FIRE, SoundSource.PLAYERS,
                1.0F, 0.9F + level.getRandom().nextFloat() * 0.2F);
        player.getCooldowns().addCooldown(gun.getItem(), RELOAD_TICKS);
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResult.SUCCESS;
    }

    /** Plays the click of the next harpoon sliding in shortly before the reload is done. */
    @Override
    public void inventoryTick(ItemStack stack, net.minecraft.world.level.Level level121, net.minecraft.world.entity.Entity owner, int slot121, boolean slot121selected) {
        if (!(level121 instanceof ServerLevel level)) return;
        net.minecraft.world.entity.EquipmentSlot slot = slot121 >= 36 && slot121 < 40 ? net.minecraft.world.entity.EquipmentSlot.values()[2 + slot121 - 36] : slot121selected ? net.minecraft.world.entity.EquipmentSlot.MAINHAND : null;
        if (owner instanceof Player player && (slot == net.minecraft.world.entity.EquipmentSlot.MAINHAND
                || slot == net.minecraft.world.entity.EquipmentSlot.OFFHAND)) {
            float left = player.getCooldowns().getCooldownPercent(stack.getItem(), 0.0F);
            if (left > 0.2F && left <= 0.25F) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), DivingSounds.HARPOON_GUN_RELOAD,
                        SoundSource.PLAYERS, 0.7F, 1.1F);
            }
        }
        super.inventoryTick(stack, level121, owner, slot121, slot121selected);
    }

    /** Off hand first, then the rest of the inventory. */
    private static ItemStack findAmmo(Player player) {
        ItemStack off = player.getOffhandItem();
        if (off.is(DivingContent.HARPOON_ITEM)) {
            return off;
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(DivingContent.HARPOON_ITEM)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<net.minecraft.network.chat.Component> builderList121, TooltipFlag flag) {
        java.util.function.Consumer<net.minecraft.network.chat.Component> builder = builderList121::add;
        builder.accept(Component.translatable("tooltip.raft.harpoon_gun").withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("tooltip.raft.harpoon_gun.1").withStyle(ChatFormatting.DARK_AQUA));
        super.appendHoverText(stack, context, builderList121, flag);
    }
}
