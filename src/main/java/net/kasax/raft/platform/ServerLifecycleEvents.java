package net.kasax.raft.platform;

import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/** Fabric's ServerLifecycleEvents on NeoForge's server events. */
public final class ServerLifecycleEvents {
    public static final Event<ServerStarting> SERVER_STARTING = new Event<>(() -> NeoForge.EVENT_BUS.addListener(ServerStartingEvent.class, e -> {
        for (ServerStarting l : ServerLifecycleEvents.SERVER_STARTING.listeners()) l.onServerStarting(e.getServer());
    }));
    public static final Event<ServerStarted> SERVER_STARTED = new Event<>(() -> NeoForge.EVENT_BUS.addListener(ServerStartedEvent.class, e -> {
        for (ServerStarted l : ServerLifecycleEvents.SERVER_STARTED.listeners()) l.onServerStarted(e.getServer());
    }));
    public static final Event<ServerStopping> SERVER_STOPPING = new Event<>(() -> NeoForge.EVENT_BUS.addListener(ServerStoppingEvent.class, e -> {
        for (ServerStopping l : ServerLifecycleEvents.SERVER_STOPPING.listeners()) l.onServerStopping(e.getServer());
    }));
    public static final Event<ServerStopped> SERVER_STOPPED = new Event<>(() -> NeoForge.EVENT_BUS.addListener(ServerStoppedEvent.class, e -> {
        for (ServerStopped l : ServerLifecycleEvents.SERVER_STOPPED.listeners()) l.onServerStopped(e.getServer());
    }));

    private ServerLifecycleEvents() {
    }

    @FunctionalInterface
    public interface ServerStarting {
        void onServerStarting(MinecraftServer server);
    }

    @FunctionalInterface
    public interface ServerStarted {
        void onServerStarted(MinecraftServer server);
    }

    @FunctionalInterface
    public interface ServerStopping {
        void onServerStopping(MinecraftServer server);
    }

    @FunctionalInterface
    public interface ServerStopped {
        void onServerStopped(MinecraftServer server);
    }
}
