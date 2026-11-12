package net.kasax.raft.platform.transfer;

/**
 * Fabric's transaction context, on NeoForge's transactions: every Beach Craft transaction is a NeoForge transaction, so
 * energy and items moved to and from other mods' handlers are part of the same undo log.
 */
public interface TransactionContext {
    net.neoforged.neoforge.transfer.transaction.TransactionContext neo();

    default int nestingDepth() {
        return neo().depth();
    }

    Transaction openNested();
}
