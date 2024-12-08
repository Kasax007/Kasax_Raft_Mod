package net.kasax.raft.screen;

import net.kasax.raft.block.ModBlocks;
import net.kasax.raft.block.entity.MakeshiftBatteryBlockEntity;
import net.kasax.raft.util.BlockPosPayload;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.base.SimpleEnergyStorage;

public class MakeshiftBatteryScreenHandler extends ScreenHandler {

    private final MakeshiftBatteryBlockEntity blockEntity;
    private final ScreenHandlerContext context;
    private long energy;
    private long maxEnergy;

    // Client Constructor
    public MakeshiftBatteryScreenHandler(int syncId, PlayerInventory playerInventory, BlockPosPayload payload) {
        this(syncId, playerInventory, (MakeshiftBatteryBlockEntity) playerInventory.player.getWorld().getBlockEntity(payload.pos()));
    }

    // Main Constructor - (Directly called from server)
    public MakeshiftBatteryScreenHandler(int syncId, PlayerInventory playerInventory, MakeshiftBatteryBlockEntity blockEntity) {
        super(ModScreenHandlers.MAKESHIFT_BATTERY_SCREEN_HANDLER, syncId);

        this.blockEntity = blockEntity;
        this.context = ScreenHandlerContext.create(this.blockEntity.getWorld(), this.blockEntity.getPos());
        this.energy = blockEntity.getEnergyStorage().getAmount();
        this.maxEnergy = blockEntity.getEnergyStorage().getCapacity();


        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    private void addPlayerInventory(PlayerInventory playerInv) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInv, 9 + (column + (row * 9)), 8 + (column * 18), 84 + (row * 18)));
            }
        }
    }

    private void addPlayerHotbar(PlayerInventory playerInv) {
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInv, column, 8 + (column * 18), 142));
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return canUse(this.context, player, ModBlocks.MAKESHIFT_BATTERY);
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
