package net.kasax.raft.platform.transfer;

/** The part of Fabric's Storage Beach Craft uses: inserting into a neighbour's inventory. */
public interface Storage<T> {
    default boolean supportsInsertion() {
        return true;
    }

    long insert(T resource, long maxAmount, TransactionContext transaction);
}
