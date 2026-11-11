package net.kasax.raft.platform.transfer;

import java.util.ArrayList;
import java.util.List;

/** Fabric's SnapshotParticipant: state saved per nesting depth, restored on abort, {@link #onFinalCommit} at the end. */
public abstract class SnapshotParticipant<T> implements TransactionContext.CloseCallback, TransactionContext.OuterCloseCallback {
    private final List<T> snapshots = new ArrayList<>();

    protected abstract T createSnapshot();

    protected abstract void readSnapshot(T snapshot);

    protected void releaseSnapshot(T snapshot) {
    }

    protected void onFinalCommit() {
    }

    public void updateSnapshots(TransactionContext transaction) {
        while (snapshots.size() <= transaction.nestingDepth()) {
            snapshots.add(null);
        }
        if (snapshots.get(transaction.nestingDepth()) == null) {
            T snapshot = createSnapshot();
            if (snapshot == null) {
                throw new IllegalArgumentException("A snapshot may not be null.");
            }
            snapshots.set(transaction.nestingDepth(), snapshot);
            transaction.addCloseCallback(this);
        }
    }

    @Override
    public void onClose(TransactionContext transaction, TransactionContext.Result result) {
        T snapshot = snapshots.set(transaction.nestingDepth(), null);
        if (result.wasAborted()) {
            readSnapshot(snapshot);
            releaseSnapshot(snapshot);
        } else if (transaction.nestingDepth() > 0) {
            if (snapshots.get(transaction.nestingDepth() - 1) == null) {
                snapshots.set(transaction.nestingDepth() - 1, snapshot);
                transaction.getOpenTransaction(transaction.nestingDepth() - 1).addCloseCallback(this);
            } else {
                releaseSnapshot(snapshot);
            }
        } else {
            releaseSnapshot(snapshot);
            transaction.addOuterCloseCallback(this);
        }
    }

    @Override
    public void afterOuterClose(TransactionContext.Result result) {
        onFinalCommit();
    }
}
