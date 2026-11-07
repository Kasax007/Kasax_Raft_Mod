package net.kasax.raft.content.vessel;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** The raft motor's fuel bay: one fuel slot (put fuel in or take it out), the flame and the running time left. */
public class RaftMotorMenu extends AbstractContainerMenu {
    public static final int FUEL_X = 56;
    public static final int FUEL_Y = 53;

    private final Container container;
    private final ContainerData data;

    public RaftMotorMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(1), new SimpleContainerData(3));
    }

    public RaftMotorMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(VesselContent.RAFT_MOTOR_MENU, containerId);
        checkContainerSize(container, 1);
        checkContainerDataCount(data, 3);
        this.container = container;
        this.data = data;
        container.startOpen(inventory.player);
        this.addSlot(new Slot(container, 0, FUEL_X, FUEL_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return VesselFuel.isFuel(stack);
            }
        });
        net.kasax.raft.compat.Menus.addStandardInventorySlots(this::addSlot, inventory, 8, 84);
        this.addDataSlots(data);
    }

    public boolean isBurning() {
        return this.data.get(0) > 0;
    }

    /** Height (0..13) of the flame. */
    public int getBurnProgress() {
        int total = Math.max(1, this.data.get(1));
        return Math.min(13, this.data.get(0) * 13 / total);
    }

    /** Running time of everything in the motor, in seconds. */
    public int getTankSeconds() {
        return this.data.get(2);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index == 0) {
                if (!this.moveItemStackTo(stack, 1, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!VesselFuel.isFuel(stack) || !this.moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }
}
