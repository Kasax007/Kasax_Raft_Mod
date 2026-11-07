package net.kasax.raft.content.diving.item;

import net.kasax.raft.content.diving.DivingContent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.function.Consumer;

/** Diving helmet, wetsuit and flippers: armor with a tooltip (what it does + the full set bonus). */
public class DivingGearItem extends net.kasax.raft.compat.GearItem {
    private final String tooltipKey;

    public DivingGearItem(Properties properties, String tooltipKey) {
        super(properties);
        this.tooltipKey = tooltipKey;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<net.minecraft.network.chat.Component> builderList121, TooltipFlag flag) {
        java.util.function.Consumer<net.minecraft.network.chat.Component> builder = builderList121::add;
        builder.accept(Component.translatable(tooltipKey).withStyle(ChatFormatting.GRAY));
        if (stack.has(DivingContent.DIVING_LAMP)) {
            builder.accept(Component.translatable("tooltip.raft.diving_helmet.lamp").withStyle(ChatFormatting.YELLOW));
        } else if (stack.is(DivingContent.DIVING_HELMET)) {
            builder.accept(Component.translatable("tooltip.raft.diving_helmet.no_lamp").withStyle(ChatFormatting.DARK_GRAY));
        }
        builder.accept(Component.translatable("tooltip.raft.diving_gear.set").withStyle(ChatFormatting.AQUA));
        super.appendHoverText(stack, context, builderList121, flag);
    }
}
