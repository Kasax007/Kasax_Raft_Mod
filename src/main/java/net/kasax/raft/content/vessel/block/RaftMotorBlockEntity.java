package net.kasax.raft.content.vessel.block;

import net.kasax.raft.content.vessel.RaftMotorMenu;
import net.kasax.raft.content.vessel.VesselContent;
import net.kasax.raft.content.vessel.VesselFuel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.kasax.raft.compat.ValueInput;
import net.kasax.raft.compat.ValueOutput;

/**
 * The raft motor: a fuel slot (any furnace fuel, opened with a click like the Expedition Raft's engine bay) and the
 * running time left from the fuel item that burns now. While the raft sails the motor burns one tick per tick; when the
 * time runs out the next item in the slot is burnt. Fuel in the slot can be taken out again. All of it is kept while
 * the raft sails (the block entity lives in the raft's shipyard plot) and when it turns back into blocks.
 */
public class RaftMotorBlockEntity extends BaseContainerBlockEntity {
    /** NBT key of the running time left (ticks), kept from 3.1 (then the only fuel store). */
    public static final String FUEL_KEY = "Fuel";
    public static final int SLOT = 0;

    private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private int fuel;
    private int fuelTotal;

    /** 0: running time left, 1: running time of the item that burns now, 2/3: whole tank (seconds, see {@link #tankSeconds}). */
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> fuel;
                case 1 -> fuelTotal;
                case 2 -> tankSeconds();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> fuel = value;
                case 1 -> fuelTotal = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public RaftMotorBlockEntity(BlockPos pos, BlockState state) {
        super(VesselContent.RAFT_MOTOR_BE, pos, state);
    }

    /** Running time left from the item that burns now (ticks). */
    public int getFuel() {
        return this.fuel;
    }

    public void setFuel(int fuel) {
        this.fuel = Math.max(0, fuel);
        this.fuelTotal = Math.max(this.fuelTotal, this.fuel);
        this.setChanged();
    }

    /** Is there anything to burn: time left or fuel in the slot? */
    public boolean hasFuel() {
        return this.fuel > 0 || VesselFuel.isFuel(this.items.get(SLOT));
    }

    /** Running time of the burning item plus every item in the slot, in seconds (the screen shows it). */
    private int tankSeconds() {
        int ticks = this.fuel;
        ItemStack stack = this.items.get(SLOT);
        if (VesselFuel.isFuel(stack) && this.level instanceof ServerLevel server) {
            ticks += VesselFuel.burnTicks(server, this.worldPosition, stack) * stack.getCount();
        }
        return ticks / 20;
    }

    /**
     * One tick of running: burns the next item from the slot when the running time is up. False when there is nothing
     * left to burn.
     */
    public boolean burnTick(ServerLevel level) {
        if (this.fuel <= 0) {
            ItemStack stack = this.items.get(SLOT);
            int burn = VesselFuel.isFuel(stack) ? VesselFuel.burnTicks(level, this.worldPosition, stack) : 0;
            if (burn <= 0) {
                return false;
            }
            net.minecraft.world.item.Item remainder = stack.getItem().getCraftingRemainingItem();
            stack.shrink(1);
            if (stack.isEmpty() && remainder != null) {
                this.items.set(SLOT, new ItemStack(remainder));
            }
            this.fuel = burn;
            this.fuelTotal = burn;
        }
        this.fuel--;
        this.setChanged();
        return true;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.raft.raft_motor");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new RaftMotorMenu(containerId, inventory, this, this.data);
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return VesselFuel.isFuel(stack);
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag121, net.minecraft.core.HolderLookup.Provider reg121) {
        super.loadAdditional(tag121, reg121);
        ValueInput input = ValueInput.of(tag121, reg121);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag121, this.items, reg121);
        this.fuel = Math.max(0, input.getIntOr(FUEL_KEY, 0));
        this.fuelTotal = Math.max(this.fuel, input.getIntOr("FuelTotal", 0));
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag121, net.minecraft.core.HolderLookup.Provider reg121) {
        super.saveAdditional(tag121, reg121);
        ValueOutput output = ValueOutput.of(tag121, reg121);
        ContainerHelper.saveAllItems(tag121, this.items, reg121);
        output.putInt(FUEL_KEY, this.fuel);
        output.putInt("FuelTotal", this.fuelTotal);
    }
}
