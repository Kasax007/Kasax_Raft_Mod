package net.kasax.raft.content.building.block;

import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.kasax.raft.content.building.BuildingBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.kasax.raft.compat.ValueInput;
import net.kasax.raft.compat.ValueOutput;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.base.SimpleEnergyStorage;

/**
 * Energy and rotor state of a {@link WindTurbineBlock}. The server checks the wind once a second (free 3x3 area of air
 * in front, above y 90), makes {@link #FE_PER_TICK} FE per tick while it runs and pushes the energy into neighbouring
 * storages. The wind state is synced to clients, which turn the rotor smoothly (speeding up and slowing down).
 */
public class WindTurbineBlockEntity extends BlockEntity {
    public static final int FE_PER_TICK = 20;
    public static final int MIN_Y = 90;
    public static final byte WIND_BLOCKED = 0;
    public static final byte WIND_TOO_LOW = 1;
    public static final byte WIND_RUNNING = 2;

    private final SimpleEnergyStorage energy = new SimpleEnergyStorage(64_000, 0, 1_000) {
        @Override
        protected void onFinalCommit() {
            setChanged();
        }
    };

    private byte wind = WIND_BLOCKED;
    private int checkTimer;

    // client-side rotor animation (degrees)
    private float rotorAngle;
    private float prevRotorAngle;
    private float rotorSpeed;

    public WindTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(BuildingBlocks.WIND_TURBINE_BE, pos, state);
        this.checkTimer = (int) (Math.abs(pos.asLong()) % 20);
    }

    public void serverTick(Level level, BlockPos pos, BlockState state) {
        if (--checkTimer <= 0) {
            checkTimer = 20;
            refreshWind(level, pos, state);
        }
        if (wind == WIND_RUNNING && energy.amount < energy.getCapacity()) {
            energy.amount = Math.min(energy.getCapacity(), energy.amount + FE_PER_TICK);
            setChanged();
        }
        if (energy.amount > 0) {
            pushEnergy(level, pos);
        }
    }

    /** Re-checks height and the free rotor area; tells clients when the state changed. */
    public void refreshWind(Level level, BlockPos pos, BlockState state) {
        byte now = computeWind(level, pos, state.getValue(WindTurbineBlock.FACING));
        if (now != wind) {
            wind = now;
            setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private static byte computeWind(Level level, BlockPos pos, Direction facing) {
        BlockPos rotor = pos.relative(facing);
        Direction side = facing.getClockWise();
        for (int up = -1; up <= 1; up++) {
            for (int across = -1; across <= 1; across++) {
                if (!level.getBlockState(rotor.relative(side, across).above(up)).isAir()) {
                    return WIND_BLOCKED;
                }
            }
        }
        return pos.getY() > MIN_Y ? WIND_RUNNING : WIND_TOO_LOW;
    }

    private void pushEnergy(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            EnergyStorage target = EnergyStorage.SIDED.find(level, pos.relative(direction), direction.getOpposite());
            if (target == null || !target.supportsInsertion()) {
                continue;
            }
            try (Transaction transaction = Transaction.openOuter()) {
                long insertable;
                try (Transaction simulation = transaction.openNested()) {
                    insertable = target.insert(energy.amount, simulation);
                }
                long extracted = energy.extract(insertable, transaction);
                if (extracted > 0 && target.insert(extracted, transaction) == extracted) {
                    transaction.commit();
                }
            }
            if (energy.amount <= 0) {
                return;
            }
        }
    }

    public void clientTick() {
        float target = wind == WIND_RUNNING ? 9.0F : wind == WIND_TOO_LOW ? 2.0F : 0.0F;
        rotorSpeed += (target - rotorSpeed) * 0.04F;
        if (Math.abs(rotorSpeed) < 0.01F && target == 0.0F) {
            rotorSpeed = 0.0F;
        }
        prevRotorAngle = rotorAngle;
        rotorAngle += rotorSpeed;
        if (rotorAngle >= 360.0F) {
            rotorAngle -= 360.0F;
            prevRotorAngle -= 360.0F;
        }
    }

    /** Rotor angle in degrees between the last two client ticks. */
    public float getRotorAngle(float partialTicks) {
        return prevRotorAngle + (rotorAngle - prevRotorAngle) * partialTicks;
    }

    public byte getWind() {
        return wind;
    }

    public long getEnergy() {
        return energy.amount;
    }

    public long getMaxEnergy() {
        return energy.getCapacity();
    }

    /** Energy API access: the turbine only gives energy (any side). */
    public EnergyStorage getEnergyStorage(@Nullable Direction side) {
        return energy;
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag121, net.minecraft.core.HolderLookup.Provider reg121) {
        ValueInput input = ValueInput.of(tag121, reg121);
        super.loadAdditional(input.tag(), input.lookup());
        energy.amount = Math.max(0L, Math.min(energy.getCapacity(), input.getLongOr("Energy", 0L)));
        wind = input.getByteOr("Wind", WIND_BLOCKED);
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag121, net.minecraft.core.HolderLookup.Provider reg121) {
        ValueOutput output = ValueOutput.of(tag121, reg121);
        super.saveAdditional(output.tag(), output.lookup());
        output.putLong("Energy", energy.amount);
        output.putByte("Wind", wind);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
