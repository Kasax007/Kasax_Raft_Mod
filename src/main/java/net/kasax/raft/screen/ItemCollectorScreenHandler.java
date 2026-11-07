package net.kasax.raft.screen;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.kasax.raft.item.ModItems;
import net.minecraft.world.item.ItemStack;

public class ItemCollectorScreenHandler extends AbstractContainerMenu {
    private final Container inventory;
    private final ContainerData propertyDelegate;

    public ItemCollectorScreenHandler(int syncId, Inventory inventory) {
        this(syncId, inventory, new SimpleContainer(2), new SimpleContainerData(2));
    }

    public ItemCollectorScreenHandler(int syncId, Inventory playerInventory, Container inventory, ContainerData arrayPropertyDelegate) {
        super(ModScreenHandlers.ITEM_COLLECTOR_SCREEN_HANDLER, syncId);
        checkContainerSize(inventory, 2);
        this.inventory = inventory;
        this.propertyDelegate = arrayPropertyDelegate;

        // top: only a net goes in; bottom: the catch, take-only
        this.addSlot(new Slot(inventory, 0, 80, 11) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.NET);
            }
        });
        this.addSlot(new Slot(inventory, 1, 80, 59) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        net.kasax.raft.compat.Menus.addStandardInventorySlots(this::addSlot, playerInventory, 8, 84);

        addDataSlots(arrayPropertyDelegate);
    }

    public boolean isCrafting() {
        return propertyDelegate.get(0) > 0;
    }

    public int getScaledProgress() {
        int progress = this.propertyDelegate.get(0);
        int maxProgress = this.propertyDelegate.get(1);  // Max Progress
        int progressArrowSize = 26; // This is the height in pixels of your arrow

        return maxProgress != 0 && progress != 0 ? progress * progressArrowSize / maxProgress : 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();
            if (slotIndex < this.inventory.getContainerSize()) {
                if (!this.moveItemStackTo(originalStack, this.inventory.getContainerSize(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(originalStack, 0, this.inventory.getContainerSize(), false)) {
                return ItemStack.EMPTY;
            }

            if (originalStack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return newStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.inventory.stillValid(player);
    }
}
