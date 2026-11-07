package net.kasax.raft.item.custom;
//Inspired by Fabric Furnaces Mod by Draylar
//Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
//The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

import net.kasax.raft.block.custom.FurnaceBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.function.Consumer;

public class FurnaceItem extends BlockItem {

    private final FurnaceBlock block;

    public FurnaceItem(FurnaceBlock block, Properties properties) {
        super(block, properties);
        this.block = block;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<net.minecraft.network.chat.Component> builderList121, TooltipFlag flag) {
        java.util.function.Consumer<net.minecraft.network.chat.Component> builder = builderList121::add;
        // add top label section
        builder.accept(Component.literal(""));
        builder.accept(Component.translatable("raft.text.tooltiplabel").withStyle(ChatFormatting.GRAY));

        // add stats
        builder.accept(Component.translatable("raft.text.speedlabel", fmt(block.getSpeedModifier())).withStyle(ChatFormatting.DARK_GREEN));
        builder.accept(Component.translatable("raft.text.fuellabel", fmt(block.getFuelModifier())).withStyle(ChatFormatting.DARK_GREEN));

        if (block.getDuplicationChance() > 0) {
            builder.accept(Component.translatable("raft.text.dupelabel", fmt(block.getDuplicationChance())).withStyle(ChatFormatting.DARK_GREEN).append(Component.literal("%").withStyle(ChatFormatting.DARK_GREEN)));
        }

        super.appendHoverText(stack, context, builderList121, flag);
    }

    /** 1.5 -> "1.5", 32.0 -> "32" */
    private static String fmt(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v);
    }
}
