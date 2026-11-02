package net.kasax.raft.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

/** A block item that shows a few aqua tooltip lines (the old Block#appendTooltip is gone in 26.x). */
public class TooltipBlockItem extends BlockItem {
    private final Component[] lines;

    public TooltipBlockItem(Block block, Properties properties, String... translationKeys) {
        super(block, properties);
        this.lines = new Component[translationKeys.length];
        for (int i = 0; i < translationKeys.length; i++) {
            this.lines[i] = Component.translatable(translationKeys[i]).withStyle(ChatFormatting.AQUA);
        }
    }

    public TooltipBlockItem(Block block, Properties properties, Component... lines) {
        super(block, properties);
        this.lines = lines;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        for (Component line : lines) {
            builder.accept(line);
        }
        super.appendHoverText(stack, context, display, builder, flag);
    }
}
