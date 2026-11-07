package net.kasax.raft.screen;

import net.kasax.raft.block.entity.WaterPurifierBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class WaterPurifierMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;

    public WaterPurifierMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(3), new SimpleContainerData(4));
    }

    public WaterPurifierMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(ModScreenHandlers.WATER_PURIFIER_MENU, containerId);
        checkContainerSize(container, 3);
        checkContainerDataCount(data, 4);
        this.container = container;
        this.data = data;

        this.addSlot(new Slot(container, WaterPurifierBlockEntity.SLOT_INPUT, 56, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return WaterPurifierBlockEntity.isWaterInput(stack);
            }
        });
        this.addSlot(new Slot(container, WaterPurifierBlockEntity.SLOT_FUEL, 56, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return WaterPurifierBlockEntity.fuelValue(stack) > 0;
            }
        });
        this.addSlot(new Slot(container, WaterPurifierBlockEntity.SLOT_OUTPUT, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        net.kasax.raft.compat.Menus.addStandardInventorySlots(this::addSlot, inventory, 8, 84);
        addDataSlots(data);
    }

    public boolean isLit() {
        return data.get(0) > 0;
    }

    /** Height (0..13) of the flame that is still burning. */
    public int getBurnProgress() {
        int total = data.get(1);
        if (total == 0) {
            total = 200;
        }
        return data.get(0) * 13 / total;
    }

    /** Width (0..24) of the progress arrow. */
    public int getCookProgress() {
        int progress = data.get(2);
        int total = data.get(3);
        return total != 0 && progress != 0 ? progress * 24 / total : 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(slotIndex);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (slotIndex == WaterPurifierBlockEntity.SLOT_OUTPUT) {
                if (!moveItemStackTo(stack, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotIndex >= 3) {
                if (WaterPurifierBlockEntity.isWaterInput(stack)) {
                    if (!moveItemStackTo(stack, WaterPurifierBlockEntity.SLOT_INPUT, WaterPurifierBlockEntity.SLOT_INPUT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (WaterPurifierBlockEntity.fuelValue(stack) > 0) {
                    if (!moveItemStackTo(stack, WaterPurifierBlockEntity.SLOT_FUEL, WaterPurifierBlockEntity.SLOT_FUEL + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (slotIndex < 30) {
                    if (!moveItemStackTo(stack, 30, 39, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!moveItemStackTo(stack, 3, 30, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 3, 39, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (stack.getCount() == result.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }
}
