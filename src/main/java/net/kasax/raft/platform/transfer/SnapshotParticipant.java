package net.kasax.raft.platform.transfer;

import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;

/** Fabric's SnapshotParticipant on NeoForge's {@link SnapshotJournal}: restored on abort, {@link #onFinalCommit} at the end. */
public abstract class SnapshotParticipant<T> extends SnapshotJournal<T> {
    @Override
    protected abstract T createSnapshot();

    protected abstract void readSnapshot(T snapshot);

    @Override
    protected final void revertToSnapshot(T snapshot) {
        readSnapshot(snapshot);
    }

    protected void onFinalCommit() {
    }

    @Override
    protected void onRootCommit(T originalState) {
        onFinalCommit();
    }

    public void updateSnapshots(TransactionContext transaction) {
        super.updateSnapshots(transaction.neo());
    }
}
