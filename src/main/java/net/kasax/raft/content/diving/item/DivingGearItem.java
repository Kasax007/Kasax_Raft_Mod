package net.kasax.raft.content.diving.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/** Diving helmet, wetsuit and flippers: armor with a tooltip (what it does + the full set bonus). */
public class DivingGearItem extends Item {
    private final String tooltipKey;

    public DivingGearItem(Properties properties, String tooltipKey) {
        super(properties);
        this.tooltipKey = tooltipKey;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder,
                                TooltipFlag flag) {
        builder.accept(Component.translatable(tooltipKey).withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("tooltip.raft.diving_gear.set").withStyle(ChatFormatting.AQUA));
        super.appendHoverText(stack, context, display, builder, flag);
    }
}
