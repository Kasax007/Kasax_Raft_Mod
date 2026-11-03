package net.kasax.raft.content.diving.item;

import net.kasax.raft.content.diving.DivingContent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * An oxygen tank worn on the chest. It stores air in ticks ({@code raft:stored_air}); with a diving helmet the wearer
 * breathes from it under water. Refills at the surface, in bubble columns and in the air compressor.
 */
public class OxygenTankItem extends Item {
    private final int capacity;

    public OxygenTankItem(Properties properties, int capacity) {
        super(properties);
        this.capacity = capacity;
    }

    public int capacity() {
        return capacity;
    }

    public static int capacity(ItemStack stack) {
        return stack.getItem() instanceof OxygenTankItem tank ? tank.capacity : 0;
    }

    public static int getAir(ItemStack stack) {
        return Mth.clamp(stack.getOrDefault(DivingContent.STORED_AIR, 0), 0, capacity(stack));
    }

    public static void setAir(ItemStack stack, int air) {
        stack.set(DivingContent.STORED_AIR, Mth.clamp(air, 0, capacity(stack)));
    }

    /** Adds up to {@code amount} air and returns how much went in. */
    public static int fill(ItemStack stack, int amount) {
        int air = getAir(stack);
        int added = Math.min(amount, capacity(stack) - air);
        if (added > 0) {
            setAir(stack, air + added);
        }
        return Math.max(added, 0);
    }

    public static boolean isFull(ItemStack stack) {
        return getAir(stack) >= capacity(stack);
    }

    /** A new tank that is already full (creative tab, loot). */
    public ItemStack filled() {
        ItemStack stack = new ItemStack(this);
        setAir(stack, capacity);
        return stack;
    }

    // ------------------------------------------------------------------------------------------- air bar
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getAir(stack) / Math.max(1, capacity));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float f = (float) getAir(stack) / Math.max(1, capacity);
        // cyan when full, amber at a quarter, red when nearly empty
        return f > 0.25F ? ARGB.colorFromFloat(1.0F, 0.2F + 0.2F * (1 - f), 0.75F + 0.15F * f, 1.0F) & 0xFFFFFF
                : ARGB.colorFromFloat(1.0F, 1.0F, 0.25F + 1.6F * f, 0.2F) & 0xFFFFFF;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder,
                                TooltipFlag flag) {
        int air = getAir(stack);
        builder.accept(Component.translatable("tooltip.raft.oxygen_tank.air", formatTime(air), formatTime(capacity))
                .withStyle(ChatFormatting.AQUA));
        builder.accept(Component.translatable("tooltip.raft.oxygen_tank").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, display, builder, flag);
    }

    /** Ticks as m:ss. */
    public static String formatTime(int ticks) {
        int seconds = Math.max(0, ticks) / 20;
        return seconds / 60 + ":" + String.format("%02d", seconds % 60);
    }
}
