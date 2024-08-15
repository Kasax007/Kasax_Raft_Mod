package net.kasax.raft.item.custom;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.kasax.raft.block.custom.FurnaceBlock;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
//Inspired by Fabric Furnaces Mod by Draylar
//Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
//The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

import java.util.List;

public class FurnaceItem extends BlockItem {

    private final FurnaceBlock block;

    public FurnaceItem(FurnaceBlock block, Settings settings) {
        super(block, settings);
        this.block = block;
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void appendTooltip(ItemStack stack, World world, List<Text> list, TooltipContext context) {
        // add top label section
        list.add(Text.literal(""));
        list.add(Text.translatable("raft.text.tooltiplabel").formatted(Formatting.GRAY));

        // add stats
        list.add(Text.translatable("raft.text.speedlabel", block.getSpeedModifier()).formatted(Formatting.DARK_GREEN));
        list.add(Text.translatable("raft.text.fuellabel", block.getFuelModifier()).formatted(Formatting.DARK_GREEN));

        if (block.getDuplicationChance() > 0) {
            list.add(Text.translatable("raft.text.dupelabel", block.getDuplicationChance()).formatted(Formatting.DARK_GREEN).append(Text.literal("%").formatted(Formatting.DARK_GREEN)));
        }

        super.appendTooltip(stack, world, list, context);
    }
}
