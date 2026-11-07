package net.kasax.raft.content.diving.block;

import net.kasax.raft.content.diving.DivingContent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Menu of the air compressor: one tank/scooter slot, the player inventory and the crank button (id 0). */
public class AirCompressorMenu extends AbstractContainerMenu {
    public static final int BUTTON_CRANK = 0;
    public static final int SLOT_X = 44;
    public static final int SLOT_Y = 35;

    private final Container container;
    private final ContainerData data;

    public AirCompressorMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(1), new SimpleContainerData(7));
    }

    public AirCompressorMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(DivingContent.AIR_COMPRESSOR_MENU, containerId);
        checkContainerSize(container, 1);
        checkContainerDataCount(data, 7);
        this.container = container;
        this.data = data;
        this.addSlot(new Slot(container, AirCompressorBlockEntity.SLOT, SLOT_X, SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return AirCompressorBlockEntity.accepts(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        net.kasax.raft.compat.Menus.addStandardInventorySlots(this::addSlot, inventory, 8, 84);
        addDataSlots(data);
    }

    public int energy() {
        return data.get(0);
    }

    public int energyCapacity() {
        return Math.max(1, data.get(1));
    }

    /** Air ticks (tank) or FE (scooter) in the item in the slot. */
    public int fill() {
        return data.get(2);
    }

    public int fillCapacity() {
        return data.get(3);
    }

    public int kind() {
        return data.get(4);
    }

    public boolean isWorking() {
        return data.get(5) != 0;
    }

    public int crankAnimation() {
        return data.get(6);
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (buttonId == BUTTON_CRANK && container instanceof AirCompressorBlockEntity be && player.level() instanceof ServerLevel level) {
            be.crank(level);
            player.causeFoodExhaustion(0.05F);
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(slotIndex);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (slotIndex == 0) {
                if (!moveItemStackTo(stack, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (AirCompressorBlockEntity.accepts(stack) && !slots.get(0).hasItem()) {
                // tanks and scooters do not stack, so the whole stack is one item
                if (!moveItemStackTo(stack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotIndex < 28) {
                if (!moveItemStackTo(stack, 28, 37, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 1, 28, false)) {
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
