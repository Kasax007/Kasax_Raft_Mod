package net.kasax.raft.item.custom;

import net.kasax.raft.util.StoredEnergy;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

/**
 * A block item that shows a few aqua tooltip lines (the old Block#appendTooltip is gone in 26.x), and the energy of a
 * machine that was broken with energy in it ({@link StoredEnergy}).
 */
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
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, java.util.List<net.minecraft.network.chat.Component> builderList121, TooltipFlag flag) {
        java.util.function.Consumer<net.minecraft.network.chat.Component> builder = builderList121::add;
        for (Component line : lines) {
            builder.accept(line);
        }
        StoredEnergy.appendTooltip(stack, getBlock(), builder);
        super.appendHoverText(stack, context, builderList121, flag);
    }
}
