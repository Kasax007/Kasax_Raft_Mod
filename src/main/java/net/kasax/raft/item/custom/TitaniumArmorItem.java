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
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/** A piece of titanium armor. Wearing the full set grants underwater effects. */
public class TitaniumArmorItem extends net.minecraft.world.item.ArmorItem {
    private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public TitaniumArmorItem(Type type, Properties properties) {
        super(net.kasax.raft.item.ModArmorMaterials.TITANIUM, type, properties);
    }

    private static final int EFFECT_TICKS = 100;

    private static List<MobEffectInstance> setEffects() {
        return List.of(
                new MobEffectInstance(MobEffects.WATER_BREATHING, EFFECT_TICKS, 1, false, false, true),
                new MobEffectInstance(MobEffects.DOLPHINS_GRACE, EFFECT_TICKS, 1, false, false, true),
                new MobEffectInstance(MobEffects.CONDUIT_POWER, EFFECT_TICKS, 1, false, false, true));
    }

    @Override
    public void inventoryTick(ItemStack stack, net.minecraft.world.level.Level level121, Entity owner, int slot121, boolean slot121selected) {
        if (!(level121 instanceof ServerLevel level)) return;
        net.minecraft.world.entity.EquipmentSlot slot = slot121 >= 36 && slot121 < 40 ? net.minecraft.world.entity.EquipmentSlot.values()[2 + slot121 - 36] : slot121selected ? net.minecraft.world.entity.EquipmentSlot.MAINHAND : null;
        if (slot != null && owner instanceof Player player && hasFullSetOn(player)) {
            for (MobEffectInstance effect : setEffects()) {
                // short effects that are topped up while the set is worn: they end about 5 s after a piece comes off
                MobEffectInstance current = player.getEffect(effect.getEffect());
                if (current == null || current.getDuration() < EFFECT_TICKS / 2) {
                    player.addEffect(effect);
                }
            }
        }
        super.inventoryTick(stack, level121, owner, slot121, slot121selected);
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
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<net.minecraft.network.chat.Component> builderList121, TooltipFlag flag) {
        java.util.function.Consumer<net.minecraft.network.chat.Component> builder = builderList121::add;
        builder.accept(Component.translatable("tooltip.raft.titanium_armor.set").withStyle(ChatFormatting.AQUA));
        super.appendHoverText(stack, context, builderList121, flag);
    }
}
