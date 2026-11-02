package net.kasax.raft.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/** A plain item that shows a few aqua tooltip lines. */
public class TooltipItem extends Item {
    private final String[] translationKeys;

    public TooltipItem(Properties properties, String... translationKeys) {
        super(properties);
        this.translationKeys = translationKeys;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        for (String key : translationKeys) {
            builder.accept(Component.translatable(key).withStyle(ChatFormatting.AQUA));
        }
        super.appendHoverText(stack, context, display, builder, flag);
    }
}
