package net.kasax.raft.block.custom;

import net.minecraft.block.Block;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.BlockView;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DriftwoodPortalFrameBlock extends Block {
    public DriftwoodPortalFrameBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable BlockView world, List<Text> tooltip, TooltipContext options) {
        tooltip.add(Text.translatable("tooltip.raft.driftwood_portal_frame.tooltip").formatted(Formatting.AQUA));
        super.appendTooltip(stack, world, tooltip, options);
    }
}
