package net.kasax.raft.platform.transfer;

/** Fabric's Transaction (openOuter, openNested, commit, abort by close) on a NeoForge transaction. */
public final class Transaction implements AutoCloseable, TransactionContext {
    private final net.neoforged.neoforge.transfer.transaction.Transaction neo;

    private Transaction(net.neoforged.neoforge.transfer.transaction.Transaction neo) {
        this.neo = neo;
    }

    public static Transaction openOuter() {
        return new Transaction(net.neoforged.neoforge.transfer.transaction.Transaction.openRoot());
    }

    /** A Beach Craft view of a transaction NeoForge or another mod opened. */
    public static TransactionContext of(net.neoforged.neoforge.transfer.transaction.TransactionContext context) {
        return new TransactionContext() {
            @Override
            public net.neoforged.neoforge.transfer.transaction.TransactionContext neo() {
                return context;
            }

            @Override
            public Transaction openNested() {
                return new Transaction(net.neoforged.neoforge.transfer.transaction.Transaction.open(context));
            }
        };
    }

    @Override
    public net.neoforged.neoforge.transfer.transaction.TransactionContext neo() {
        return neo;
    }

    @Override
    public Transaction openNested() {
        return new Transaction(net.neoforged.neoforge.transfer.transaction.Transaction.open(neo));
    }

    public void commit() {
        neo.commit();
    }

    public void abort() {
        neo.close();
    }

    @Override
    public void close() {
        neo.close();
    }
}
