package net.kasax.raft.content.vessel.block;

import net.kasax.raft.content.vessel.VesselContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.kasax.raft.compat.ValueInput;
import net.kasax.raft.compat.ValueOutput;

/** The fuel tank of a {@link RaftMotorBlock}: ticks of running time left. Kept while the raft sails (in its NBT). */
public class RaftMotorBlockEntity extends BlockEntity {
    /** NBT key of the fuel, also read and written by the vessel while the motor is at sea. */
    public static final String FUEL_KEY = "Fuel";

    private int fuel;

    public RaftMotorBlockEntity(BlockPos pos, BlockState state) {
        super(VesselContent.RAFT_MOTOR_BE, pos, state);
    }

    public int getFuel() {
        return this.fuel;
    }

    public void setFuel(int fuel) {
        this.fuel = Math.max(0, fuel);
        this.setChanged();
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag121, net.minecraft.core.HolderLookup.Provider reg121) {
        ValueInput input = ValueInput.of(tag121, reg121);
        super.loadAdditional(input.tag(), input.lookup());
        this.fuel = Math.max(0, input.getIntOr(FUEL_KEY, 0));
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag121, net.minecraft.core.HolderLookup.Provider reg121) {
        ValueOutput output = ValueOutput.of(tag121, reg121);
        super.saveAdditional(output.tag(), output.lookup());
        output.putInt(FUEL_KEY, this.fuel);
    }
}
