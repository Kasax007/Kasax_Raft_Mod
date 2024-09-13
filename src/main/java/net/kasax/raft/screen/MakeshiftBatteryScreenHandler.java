package net.kasax.raft.screen;

import net.kasax.raft.block.ModBlocks;
import net.kasax.raft.block.entity.MakeshiftBatteryBlockEntity;
import net.kasax.raft.util.BlockPosPayload;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
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

    // Client Constructor
    public MakeshiftBatteryScreenHandler(int syncId, PlayerInventory playerInventory, BlockPosPayload payload) {
        this(syncId, playerInventory, (MakeshiftBatteryBlockEntity) playerInventory.player.getWorld().getBlockEntity(payload.pos()));
    }

    // Main Constructor - (Directly called from server)
    public MakeshiftBatteryScreenHandler(int syncId, PlayerInventory playerInventory, MakeshiftBatteryBlockEntity blockEntity) {
        super(ModScreenHandlers.MAKESHIFT_BATTERY_SCREEN_HANDLER, syncId);

        this.blockEntity = blockEntity;
        this.context = ScreenHandlerContext.create(this.blockEntity.getWorld(), this.blockEntity.getPos());

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
        System.out.println("getEnergy: " + (this.blockEntity.getEnergyStorage().getAmount()));
        return this.blockEntity.getEnergyStorage().getAmount();
    }

    public long getMaxEnergy() {
        return this.blockEntity.getEnergyStorage().getCapacity();
    }

    public float getEnergyPercent() {
        SimpleEnergyStorage energyStorage = this.blockEntity.getEnergyStorage();
        long energy = energyStorage.getAmount();
        long maxEnergy = energyStorage.getCapacity();
        if (maxEnergy == 0 || energy == 0)
            return 0.0F;


        System.out.println("getEnergyPercent: " + (MathHelper.clamp((float) energy / (float) maxEnergy, 0.0F, 1.0F)));
        return MathHelper.clamp((float) energy / (float) maxEnergy, 0.0F, 1.0F);
    }
}
