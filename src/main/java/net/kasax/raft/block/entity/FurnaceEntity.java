package net.kasax.raft.block.entity;
//Inspired by Fabric Furnaces Mod by Draylar
//Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
//The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

import net.kasax.raft.block.custom.FurnaceBlock;
import net.kasax.raft.util.FurnaceEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeType;
import net.minecraft.screen.FurnaceScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class FurnaceEntity extends AbstractFurnaceBlockEntity {
    public FurnaceEntity(BlockPos pos, BlockState state) {
        super(FurnaceEntities.RAFT_FURNACE, pos, state, RecipeType.SMELTING);
    }
    public static void tick(World world, BlockPos pos, BlockState state, FurnaceEntity blockEntity) {
        // Implementation of tick logic
        AbstractFurnaceBlockEntity.tick(world, pos, state, blockEntity);
    }
    @Override
    public int getFuelTime(ItemStack fuel) {
        return (int) (super.getFuelTime(fuel) / getFuelModifier());
    }

    @Override
    public Text getContainerName() {
        return Text.translatable("container.furnace");
    }

    @Override
    public ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
        return new FurnaceScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    public double getSpeedModifier() {
        if(getCachedState().getBlock() instanceof FurnaceBlock) {
            return ((FurnaceBlock) getCachedState().getBlock()).getSpeedModifier();
        }

        return 1;
    }

    public double getFuelModifier() {
        if(getCachedState().getBlock() instanceof FurnaceBlock) {
            return ((FurnaceBlock) getCachedState().getBlock()).getFuelModifier();
        }

        return 1;
    }

    public int getDuplicationChance() {
        if(getCachedState().getBlock() instanceof FurnaceBlock) {
            return ((FurnaceBlock) getCachedState().getBlock()).getDuplicationChance();
        }

        return 0;
    }
}
