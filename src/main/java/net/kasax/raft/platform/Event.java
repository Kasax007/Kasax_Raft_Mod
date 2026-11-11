package net.kasax.raft.platform;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** A Fabric-style callback list: {@link #register} adds a listener; the NeoForge listener behind it is added once. */
public final class Event<T> {
    private final List<T> listeners = new CopyOnWriteArrayList<>();
    private final Runnable hookup;
    private boolean hooked;

    public Event(Runnable hookup) {
        this.hookup = hookup;
    }

    public synchronized void register(T listener) {
        listeners.add(listener);
        if (!hooked) {
            hooked = true;
            hookup.run();
        }
    }

    public List<T> listeners() {
        return listeners;
    }
}
