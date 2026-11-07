package net.kasax.raft.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

/** A two block tall block item (scarecrow) with aqua tooltip lines. */
public class TooltipDoubleBlockItem extends DoubleHighBlockItem {
    private final Component[] lines;

    public TooltipDoubleBlockItem(Block block, Properties properties, String... translationKeys) {
        super(block, properties);
        this.lines = new Component[translationKeys.length];
        for (int i = 0; i < translationKeys.length; i++) {
            this.lines[i] = Component.translatable(translationKeys[i]).withStyle(ChatFormatting.AQUA);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, java.util.List<net.minecraft.network.chat.Component> builderList121, TooltipFlag flag) {
        java.util.function.Consumer<net.minecraft.network.chat.Component> builder = builderList121::add;
        for (Component line : lines) {
            builder.accept(line);
        }
        super.appendHoverText(stack, context, builderList121, flag);
    }
}
