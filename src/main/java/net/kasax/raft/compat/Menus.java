package net.kasax.raft.compat;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.function.Consumer;

/** 1.21.1 port helper: the player's inventory slots below a container (the newer game has this on the menu). */
public final class Menus {
    private Menus() {
    }

    public static void addStandardInventorySlots(Consumer<Slot> addSlot, Inventory inventory, int left, int top) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot.accept(new Slot(inventory, col + row * 9 + 9, left + col * 18, top + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot.accept(new Slot(inventory, col, left + col * 18, top + 58));
        }
    }
}
