package net.kasax.raft.content.vessel.block;

import net.kasax.raft.content.vessel.VesselContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

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
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.fuel = Math.max(0, input.getIntOr(FUEL_KEY, 0));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt(FUEL_KEY, this.fuel);
    }
}
