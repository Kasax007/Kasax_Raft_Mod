package net.kasax.raft.block.custom;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class DriftwoodPortalFrameBlock extends Block {
    public DriftwoodPortalFrameBlock(Settings settings) {
        super(settings);
    }

    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType options) {
        tooltip.add(Text.translatable("tooltip.raft.driftwood_portal_frame.tooltip").formatted(Formatting.AQUA));
        super.appendTooltip(stack, context, tooltip, options);
    }
}
