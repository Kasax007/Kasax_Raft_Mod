package net.kasax.raft.item.custom;


import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AncientCrystalItem extends Item {
    public AncientCrystalItem(Settings settings) {
        super(settings);
    }
    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType options) {
        tooltip.add(Text.literal("Can be found in Shipwrecks").formatted(Formatting.AQUA));
        super.appendTooltip(stack, context, tooltip, options);
    }
}
