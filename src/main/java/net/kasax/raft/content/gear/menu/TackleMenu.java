package net.kasax.raft.content.gear.menu;

import net.kasax.raft.content.gear.GearContent;
import net.kasax.raft.content.gear.item.DriftwoodFishingRodItem;
import net.kasax.raft.content.gear.item.Tackle;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The tackle box of a Driftwood Fishing Rod held in the hand: a bait slot and a lure slot over the player's
 * inventory. Every change is written straight back into the rod (data component {@code raft:tackle}); the rod's own
 * inventory slot is locked while the box is open, and the box closes when the rod leaves the hand.
 */
public class TackleMenu extends AbstractContainerMenu {
    public static final int SLOT_X = 12, BAIT_Y = 20, LURE_Y = 40;
    public static final int HEIGHT = 157;

    private final Inventory inventory;
    private final int rodSlot;
    private final SimpleContainer tackle = new SimpleContainer(2) {
        @Override
        public void setChanged() {
            super.setChanged();
            save();
        }
    };
    private boolean loading;

    public TackleMenu(int containerId, Inventory inventory, Integer rodSlot) {
        super(GearContent.TACKLE_MENU, containerId);
        this.inventory = inventory;
        this.rodSlot = rodSlot;
        Tackle loaded = DriftwoodFishingRodItem.tackle(rod());
        loading = true;
        tackle.setItem(0, loaded.get(0));
        tackle.setItem(1, loaded.get(1));
        loading = false;
        addSlot(new TackleSlot(tackle, 0, SLOT_X, BAIT_Y));
        addSlot(new TackleSlot(tackle, 1, SLOT_X, LURE_Y));
        int top = HEIGHT - 82;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new LockedSlot(inventory, col + row * 9 + 9, 8 + col * 18, top + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new LockedSlot(inventory, col, 8 + col * 18, top + 58));
        }
    }

    private ItemStack rod() {
        return inventory.getItem(rodSlot);
    }

    private void save() {
        if (loading) return;
        ItemStack rod = rod();
        if (rod.getItem() instanceof DriftwoodFishingRodItem) {
            rod.set(GearContent.TACKLE, new Tackle(tackle.getItem(0), tackle.getItem(1)));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return rod().getItem() instanceof DriftwoodFishingRodItem;
    }

    @Override
    public void clicked(int slotIndex, int button, ClickType input, Player player) {
        // number keys / off-hand swap must not move the rod out from under the open box
        if (input == ClickType.SWAP && (button == rodSlot || (button == 40 && rodSlot == Inventory.SLOT_OFFHAND))) return;
        // the rod itself takes no clicks while its box is open (loading it by right-click would bypass the box)
        if (slotIndex >= 0 && slotIndex < slots.size() && slots.get(slotIndex) instanceof LockedSlot s
                && s.getContainerSlot() == rodSlot) return;
        super.clicked(slotIndex, button, input, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();
        if (index < 2) {
            if (!moveItemStackTo(stack, 2, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            int target = DriftwoodFishingRodItem.slotFor(stack);
            if (target < 0 || !moveItemStackTo(stack, target, target + 1, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }

    private static class TackleSlot extends Slot {
        TackleSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return DriftwoodFishingRodItem.slotFor(stack) == getContainerSlot();
        }
    }

    /** A player inventory slot that cannot be picked up while it holds the open rod. */
    private class LockedSlot extends Slot {
        LockedSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPickup(Player player) {
            return getContainerSlot() != rodSlot && super.mayPickup(player);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return getContainerSlot() != rodSlot && super.mayPlace(stack);
        }
    }
}
