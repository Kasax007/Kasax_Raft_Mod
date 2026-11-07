package net.kasax.raft.content.boss.item;

import net.kasax.raft.Raft;
import net.kasax.raft.content.boss.BossSounds;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

/**
 * Tidebreaker, the legendary spear forged from the Leviathan's scales: 9 attack damage, +50 % against creatures of the
 * sea ({@code #raft:tidebreaker_prey}), and in water or rain a charged use (hold like a trident) hurls its wielder
 * forward in a Riptide-like spinning dash.
 */
public class TidebreakerItem extends net.minecraft.world.item.SwordItem {
    public static final TagKey<EntityType<?>> PREY = TagKey.create(Registries.ENTITY_TYPE, Raft.id("tidebreaker_prey"));
    private static final int CHARGE_TICKS = 10;
    private static final float DASH_STRENGTH = 3.2F;

    public TidebreakerItem(Properties properties) {
        super(net.kasax.raft.content.boss.BossItems.TIDEBREAKER_MATERIAL, properties);
    }

    @Override
    public float getAttackDamageBonus(Entity victim, float damage, DamageSource damageSource) {
        return victim.getType().is(PREY) ? damage * 0.5F : 0.0F;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return net.kasax.raft.compat.Results.holder(this.use26(level, player, hand), player.getItemInHand(hand));
    }

    private InteractionResult use26(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getDamageValue() >= stack.getMaxDamage() - 1 || !player.isInWaterOrRain() || player.isPassenger()) {
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingTime) {
        if (!(entity instanceof Player player)) {
            return;
        }
        int held = this.getUseDuration(stack, entity) - remainingTime;
        if (held < CHARGE_TICKS || !player.isInWaterOrRain() || player.isPassenger()) {
            return;
        }
        float yRot = player.getYRot();
        float xRot = player.getXRot();
        float xd = -Mth.sin(yRot * Mth.DEG_TO_RAD) * Mth.cos(xRot * Mth.DEG_TO_RAD);
        float yd = -Mth.sin(xRot * Mth.DEG_TO_RAD);
        float zd = Mth.cos(yRot * Mth.DEG_TO_RAD) * Mth.cos(xRot * Mth.DEG_TO_RAD);
        float len = Mth.sqrt(xd * xd + yd * yd + zd * zd);
        player.push(xd * DASH_STRENGTH / len, yd * DASH_STRENGTH / len, zd * DASH_STRENGTH / len);
        player.startAutoSpinAttack(20, 9.0F, stack);
        if (player.onGround()) {
            player.move(MoverType.SELF, new Vec3(0.0, 1.2, 0.0));
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        if (!level.isClientSide()) {
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
            player.getCooldowns().addCooldown(stack.getItem(), 16);
        }
        level.playSound(null, player, BossSounds.TIDEBREAKER_DASH, SoundSource.PLAYERS, 1.0F, 1.0F);
        
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<net.minecraft.network.chat.Component> builderList121, TooltipFlag flag) {
        java.util.function.Consumer<net.minecraft.network.chat.Component> builder = builderList121::add;
        builder.accept(Component.translatable("tooltip.raft.tidebreaker.1").withStyle(net.minecraft.ChatFormatting.DARK_AQUA));
        builder.accept(Component.translatable("tooltip.raft.tidebreaker.2").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
