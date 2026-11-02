package net.kasax.raft.block.entity;
//Inspired by Fabric Furnaces Mod by Draylar
//Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
//The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

import net.kasax.raft.block.custom.FurnaceBlock;
import net.kasax.raft.util.FurnaceEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Furnace block entity of the raft furnaces. The speed and fuel multipliers hook into the
 * 26.x cooking-fuel API; the duplication chance is applied right after each cooking tick.
 */
public class FurnaceEntity extends AbstractFurnaceBlockEntity {
    private static final Component DEFAULT_NAME = Component.translatable("container.furnace");

    public FurnaceEntity(BlockPos pos, BlockState state) {
        super(FurnaceEntities.RAFT_FURNACE, pos, state, RecipeType.SMELTING);
    }

    public static void tick(ServerLevel level, BlockPos pos, BlockState state, FurnaceEntity furnace) {
        ItemStack before = furnace.items.get(SLOT_RESULT).copy();
        AbstractFurnaceBlockEntity.serverTick(level, pos, state, furnace);

        int chance = furnace.getDuplicationChance();
        if (chance > 0) {
            ItemStack out = furnace.items.get(SLOT_RESULT);
            if (!out.isEmpty() && (before.isEmpty() || ItemStack.isSameItemSameComponents(before, out))) {
                int produced = out.getCount() - (before.isEmpty() ? 0 : before.getCount());
                if (produced > 0 && level.getRandom().nextInt(100) < chance
                        && out.getCount() < Math.min(furnace.getMaxStackSize(), out.getMaxStackSize())) {
                    out.grow(1);
                    furnace.setChanged();
                }
            }
        }
    }

    @Override
    protected int getBurnDuration(ServerLevel level, ItemStack fuelItem) {
        return (int) (super.getBurnDuration(level, fuelItem) / getFuelModifier());
    }

    @Override
    protected float getSpeedMultiplier(ServerLevel level, ItemStack fuelItem) {
        return super.getSpeedMultiplier(level, fuelItem) * (float) getSpeedModifier();
    }

    @Override
    protected Component getDefaultName() {
        return DEFAULT_NAME;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new FurnaceMenu(containerId, inventory, this, this.dataAccess);
    }

    public double getSpeedModifier() {
        if (getBlockState().getBlock() instanceof FurnaceBlock block) {
            return block.getSpeedModifier();
        }
        return 1;
    }

    public double getFuelModifier() {
        if (getBlockState().getBlock() instanceof FurnaceBlock block) {
            return block.getFuelModifier();
        }
        return 1;
    }

    public int getDuplicationChance() {
        if (getBlockState().getBlock() instanceof FurnaceBlock block) {
            return block.getDuplicationChance();
        }
        return 0;
    }
}
