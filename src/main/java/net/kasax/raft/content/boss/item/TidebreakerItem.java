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
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

/**
 * Tidebreaker, the legendary spear forged from the Leviathan's scales: 9 attack damage, +50 % against creatures of the
 * sea ({@code #raft:tidebreaker_prey}), and in water or rain a charged use (hold like a trident) hurls its wielder
 * forward in a Riptide-like spinning dash.
 */
public class TidebreakerItem extends Item {
    public static final TagKey<EntityType<?>> PREY = TagKey.create(Registries.ENTITY_TYPE, Raft.id("tidebreaker_prey"));
    private static final int CHARGE_TICKS = 10;
    private static final float DASH_STRENGTH = 3.2F;

    public TidebreakerItem(Properties properties) {
        super(properties);
    }

    @Override
    public float getAttackDamageBonus(Entity victim, float damage, DamageSource damageSource) {
        return victim.is(PREY) ? damage * 0.5F : 0.0F;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.TRIDENT;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.nextDamageWillBreak() || !player.isInWaterOrRain() || player.isPassenger()) {
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingTime) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        int held = this.getUseDuration(stack, entity) - remainingTime;
        if (held < CHARGE_TICKS || !player.isInWaterOrRain() || player.isPassenger()) {
            return false;
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
            stack.hurtWithoutBreaking(1, player);
            player.getCooldowns().addCooldown(stack, 16);
        }
        level.playSound(null, player, BossSounds.TIDEBREAKER_DASH, SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.raft.tidebreaker.1").withStyle(net.minecraft.ChatFormatting.DARK_AQUA));
        builder.accept(Component.translatable("tooltip.raft.tidebreaker.2").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
