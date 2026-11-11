package net.kasax.raft.platform.transfer;

import java.util.ArrayList;
import java.util.List;

/**
 * Fabric's transactions (the transfer API's undo log), as Beach Craft's energy code uses them: nested transactions
 * per thread, close callbacks in reverse order, outer close callbacks after the outermost one. A transaction that is
 * closed without {@link #commit()} is aborted.
 */
public interface Transaction extends AutoCloseable, TransactionContext {
    static Transaction openOuter() {
        Manager manager = Manager.get();
        if (!manager.stack.isEmpty()) {
            throw new IllegalStateException("An outer transaction is already active on this thread.");
        }
        return manager.open();
    }

    static boolean isOpen() {
        return !Manager.get().stack.isEmpty();
    }

    static TransactionContext getCurrentUnsafe() {
        List<Impl> stack = Manager.get().stack;
        return stack.isEmpty() ? null : stack.get(stack.size() - 1);
    }

    void abort();

    void commit();

    @Override
    void close();

    final class Manager {
        private static final ThreadLocal<Manager> MANAGERS = ThreadLocal.withInitial(Manager::new);
        final List<Impl> stack = new ArrayList<>();

        static Manager get() {
            return MANAGERS.get();
        }

        Impl open() {
            Impl t = new Impl(this, stack.size());
            stack.add(t);
            return t;
        }
    }

    final class Impl implements Transaction {
        private final Manager manager;
        private final int depth;
        private boolean open = true;
        /** While the close callbacks run: outer close callbacks may still be added (snapshot participants do). */
        private boolean closing;
        private final List<CloseCallback> closeCallbacks = new ArrayList<>();
        private final List<OuterCloseCallback> outerCloseCallbacks;

        Impl(Manager manager, int depth) {
            this.manager = manager;
            this.depth = depth;
            this.outerCloseCallbacks = depth == 0 ? new ArrayList<>() : null;
        }

        private void validateCurrent() {
            if (!open || closing) {
                throw new IllegalStateException("Transaction operation cannot be applied to a closed transaction.");
            }
            if (manager.stack.get(manager.stack.size() - 1) != this) {
                throw new IllegalStateException("Transaction operation cannot be applied to a non-current transaction.");
            }
        }

        @Override
        public Transaction openNested() {
            validateCurrent();
            return manager.open();
        }

        @Override
        public int nestingDepth() {
            return depth;
        }

        @Override
        public Transaction getOpenTransaction(int nestingDepth) {
            if (nestingDepth < 0 || nestingDepth >= manager.stack.size()) {
                throw new IndexOutOfBoundsException("No open transaction at depth " + nestingDepth);
            }
            return manager.stack.get(nestingDepth);
        }

        @Override
        public void addCloseCallback(CloseCallback closeCallback) {
            if (!open || closing) {
                throw new IllegalStateException("Transaction is closed.");
            }
            closeCallbacks.add(closeCallback);
        }

        @Override
        public void addOuterCloseCallback(OuterCloseCallback outerCloseCallback) {
            if (!open) {
                throw new IllegalStateException("Transaction is closed.");
            }
            ((Impl) manager.stack.get(0)).outerCloseCallbacks.add(outerCloseCallback);
        }

        private void close(Result result) {
            validateCurrent();
            closing = true;
            RuntimeException error = null;
            for (int i = closeCallbacks.size() - 1; i >= 0; i--) {
                try {
                    closeCallbacks.get(i).onClose(this, result);
                } catch (RuntimeException e) {
                    if (error == null) error = e; else error.addSuppressed(e);
                }
            }
            closeCallbacks.clear();
            open = false;
            closing = false;
            manager.stack.remove(manager.stack.size() - 1);
            if (depth == 0) {
                for (int i = 0; i < outerCloseCallbacks.size(); i++) {
                    try {
                        outerCloseCallbacks.get(i).afterOuterClose(result);
                    } catch (RuntimeException e) {
                        if (error == null) error = e; else error.addSuppressed(e);
                    }
                }
                outerCloseCallbacks.clear();
            }
            if (error != null) {
                throw error;
            }
        }

        @Override
        public void abort() {
            close(Result.ABORTED);
        }

        @Override
        public void commit() {
            close(Result.COMMITTED);
        }

        @Override
        public void close() {
            if (open) {
                abort();
            }
        }
    }
}
