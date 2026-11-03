package net.kasax.raft.content.voyage;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Storage of the Expedition Raft: 3 rows (6 with the cargo crate), the engine's fuel slot (with the engine) and the
 * anchor lever (button 0, with the anchor). Layout: chest grid on the left, engine/anchor panel on the right.
 */
public class ExpeditionRaftMenu extends AbstractContainerMenu {
    public static final int BUTTON_ANCHOR = 0;
    public static final int FUEL_X = 186;
    public static final int FUEL_Y = 36;

    private final Container container;
    private final ContainerData data;
    private final int upgrades;
    private final int rows;
    private final int cargoSlots;
    private final int ownSlots;
    private final @Nullable ExpeditionRaft raft;

    /** Client side, opened by the server with the raft's upgrade mask. */
    public ExpeditionRaftMenu(int containerId, Inventory inventory, Integer upgrades) {
        this(containerId, inventory, new SimpleContainer(ExpeditionRaft.SIZE), new SimpleContainerData(3), upgrades, null);
    }

    public ExpeditionRaftMenu(int containerId, Inventory inventory, Container container, ContainerData data, int upgrades,
                              @Nullable ExpeditionRaft raft) {
        super(VoyageContent.EXPEDITION_RAFT_MENU, containerId);
        checkContainerSize(container, ExpeditionRaft.SIZE);
        checkContainerDataCount(data, 3);
        this.container = container;
        this.data = data;
        this.upgrades = upgrades;
        this.raft = raft;
        this.rows = RaftUpgrade.CRATE.in(upgrades) ? 6 : 3;
        this.cargoSlots = this.rows * 9;
        container.startOpen(inventory.player);
        for (int row = 0; row < this.rows; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(container, col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }
        if (this.hasEngine()) {
            this.addSlot(new Slot(container, ExpeditionRaft.FUEL_SLOT, FUEL_X, FUEL_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return ExpeditionRaft.isFuel(stack);
                }
            });
        }
        this.ownSlots = this.slots.size();
        this.addStandardInventorySlots(inventory, 8, 18 + this.rows * 18 + 13);
        this.addDataSlots(data);
    }

    public int getRows() {
        return this.rows;
    }

    public boolean hasEngine() {
        return RaftUpgrade.ENGINE.in(this.upgrades);
    }

    public boolean hasAnchor() {
        return RaftUpgrade.ANCHOR.in(this.upgrades);
    }

    public boolean isAnchored() {
        return this.data.get(2) != 0;
    }

    public boolean isBurning() {
        return this.data.get(0) > 0;
    }

    /** Height (0..13) of the engine flame. */
    public int getBurnProgress() {
        int total = this.data.get(1);
        if (total <= 0) {
            total = 200;
        }
        return Math.min(13, this.data.get(0) * 13 / total);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_ANCHOR && this.raft != null && this.hasAnchor()) {
            return this.raft.toggleAnchor(player);
        }
        return id == BUTTON_ANCHOR && this.hasAnchor();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < this.ownSlots) {
                if (!this.moveItemStackTo(stack, this.ownSlots, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                boolean moved = false;
                if (this.hasEngine() && ExpeditionRaft.isFuel(stack)) {
                    moved = this.moveItemStackTo(stack, this.cargoSlots, this.cargoSlots + 1, false);
                }
                if (!moved && !this.moveItemStackTo(stack, 0, this.cargoSlots, false)) {
                    return ItemStack.EMPTY;
                }
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
