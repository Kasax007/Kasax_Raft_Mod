package net.kasax.raft.platform.energy;

import net.minecraft.core.Direction;
import net.kasax.raft.platform.transfer.SnapshotParticipant;
import net.kasax.raft.platform.transfer.TransactionContext;
import org.jetbrains.annotations.Nullable;

/** Team Reborn Energy's SimpleSidedEnergyContainer: one amount, limits per side. */
public abstract class SimpleSidedEnergyContainer extends SnapshotParticipant<Long> {
    public long amount = 0;
    private final SideStorage[] sideStorages = new SideStorage[7];

    public SimpleSidedEnergyContainer() {
        for (int i = 0; i < 7; ++i) {
            sideStorages[i] = new SideStorage(i == 6 ? null : Direction.from3DDataValue(i));
        }
    }

    public abstract long getCapacity();

    public abstract long getMaxInsert(@Nullable Direction side);

    public abstract long getMaxExtract(@Nullable Direction side);

    public EnergyStorage getSideStorage(@Nullable Direction side) {
        return sideStorages[side == null ? 6 : side.get3DDataValue()];
    }

    @Override
    protected Long createSnapshot() {
        return amount;
    }

    @Override
    protected void readSnapshot(Long snapshot) {
        amount = snapshot;
    }

    private class SideStorage implements EnergyStorage {
        private final Direction side;

        SideStorage(Direction side) {
            this.side = side;
        }

        @Override
        public boolean supportsInsertion() {
            return getMaxInsert(side) > 0;
        }

        @Override
        public long insert(long maxAmount, TransactionContext transaction) {
            long inserted = Math.min(getMaxInsert(side), Math.min(maxAmount, getCapacity() - amount));
            if (inserted > 0) {
                updateSnapshots(transaction);
                amount += inserted;
                return inserted;
            }
            return 0;
        }

        @Override
        public boolean supportsExtraction() {
            return getMaxExtract(side) > 0;
        }

        @Override
        public long extract(long maxAmount, TransactionContext transaction) {
            long extracted = Math.min(getMaxExtract(side), Math.min(maxAmount, amount));
            if (extracted > 0) {
                updateSnapshots(transaction);
                amount -= extracted;
                return extracted;
            }
            return 0;
        }

        @Override
        public long getAmount() {
            return amount;
        }

        @Override
        public long getCapacity() {
            return SimpleSidedEnergyContainer.this.getCapacity();
        }
    }
}
