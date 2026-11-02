package net.kasax.raft.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/** A piece of titanium armor. Wearing the full set grants underwater effects. */
public class TitaniumArmorItem extends Item {
    private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public TitaniumArmorItem(Properties properties) {
        super(properties);
    }

    private static final int EFFECT_TICKS = 100;

    private static List<MobEffectInstance> setEffects() {
        return List.of(
                new MobEffectInstance(MobEffects.WATER_BREATHING, EFFECT_TICKS, 1, false, false, true),
                new MobEffectInstance(MobEffects.DOLPHINS_GRACE, EFFECT_TICKS, 1, false, false, true),
                new MobEffectInstance(MobEffects.CONDUIT_POWER, EFFECT_TICKS, 1, false, false, true));
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        if (slot != null && owner instanceof Player player && hasFullSetOn(player)) {
            for (MobEffectInstance effect : setEffects()) {
                // short effects that are topped up while the set is worn: they end about 5 s after a piece comes off
                MobEffectInstance current = player.getEffect(effect.getEffect());
                if (current == null || current.getDuration() < EFFECT_TICKS / 2) {
                    player.addEffect(effect);
                }
            }
        }
        super.inventoryTick(stack, level, owner, slot);
    }

    /** True if every armor slot holds a piece of titanium armor. */
    public static boolean hasFullSetOn(Player player) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (!(player.getItemBySlot(slot).getItem() instanceof TitaniumArmorItem)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.raft.titanium_armor.set").withStyle(ChatFormatting.AQUA));
        super.appendHoverText(stack, context, display, builder, flag);
    }
}
