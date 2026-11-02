package net.kasax.raft.screen;

import net.kasax.raft.block.ModBlocks;
import net.kasax.raft.block.entity.MakeshiftBatteryBlockEntity;
import net.kasax.raft.util.BlockPosPayload;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

public class MakeshiftBatteryScreenHandler extends AbstractContainerMenu {

    private final MakeshiftBatteryBlockEntity blockEntity;
    private final ContainerLevelAccess context;
    private long energy;
    private long maxEnergy;

    // Client Constructor
    public MakeshiftBatteryScreenHandler(int syncId, Inventory playerInventory, BlockPosPayload payload) {
        this(syncId, playerInventory, (MakeshiftBatteryBlockEntity) playerInventory.player.level().getBlockEntity(payload.pos()));
    }

    // Main Constructor - (Directly called from server)
    public MakeshiftBatteryScreenHandler(int syncId, Inventory playerInventory, MakeshiftBatteryBlockEntity blockEntity) {
        super(ModScreenHandlers.MAKESHIFT_BATTERY_SCREEN_HANDLER, syncId);

        this.blockEntity = blockEntity;
        this.context = ContainerLevelAccess.create(this.blockEntity.getLevel(), this.blockEntity.getBlockPos());
        this.energy = blockEntity.getEnergyStorage().getAmount();
        this.maxEnergy = blockEntity.getEnergyStorage().getCapacity();

        addStandardInventorySlots(playerInventory, 8, 84);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.context, player, ModBlocks.MAKESHIFT_BATTERY);
    }

    public MakeshiftBatteryBlockEntity getBlockEntity() {
        return this.blockEntity;
    }

    public long getEnergy() {
        return this.energy;
    }

    public long getMaxEnergy() {
        return this.maxEnergy;
    }

    public void setEnergy(long energy) {
        this.energy = energy;
    }

    public void setMaxEnergy(long maxEnergy) {
        this.maxEnergy = maxEnergy;
    }

    public float getEnergyPercent() {
        return (maxEnergy == 0) ? 0.0f : (float) energy / maxEnergy;
    }
}
