package net.kasax.raft.platform.energy;

import net.kasax.raft.platform.transfer.SnapshotParticipant;
import net.kasax.raft.platform.transfer.TransactionContext;

/** Team Reborn Energy's SimpleEnergyStorage. */
public class SimpleEnergyStorage extends SnapshotParticipant<Long> implements EnergyStorage {
    public long amount = 0;
    public final long capacity;
    public final long maxInsert;
    public final long maxExtract;

    public SimpleEnergyStorage(long capacity, long maxInsert, long maxExtract) {
        this.capacity = capacity;
        this.maxInsert = maxInsert;
        this.maxExtract = maxExtract;
    }

    @Override
    protected Long createSnapshot() {
        return amount;
    }

    @Override
    protected void readSnapshot(Long snapshot) {
        amount = snapshot;
    }

    @Override
    public boolean supportsInsertion() {
        return maxInsert > 0;
    }

    @Override
    public long insert(long maxAmount, TransactionContext transaction) {
        if (maxAmount < 0) throw new IllegalArgumentException("maxAmount may not be negative");
        long inserted = Math.min(maxInsert, Math.min(maxAmount, capacity - amount));
        if (inserted > 0) {
            updateSnapshots(transaction);
            amount += inserted;
            return inserted;
        }
        return 0;
    }

    @Override
    public boolean supportsExtraction() {
        return maxExtract > 0;
    }

    @Override
    public long extract(long maxAmount, TransactionContext transaction) {
        if (maxAmount < 0) throw new IllegalArgumentException("maxAmount may not be negative");
        long extracted = Math.min(maxExtract, Math.min(maxAmount, amount));
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
        return capacity;
    }
}
