package net.kasax.raft.platform;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Fabric's ServerTickEvents on NeoForge's tick events. */
public final class ServerTickEvents {
    public static final Event<StartTick> START_SERVER_TICK = new Event<>(() -> NeoForge.EVENT_BUS.addListener(ServerTickEvent.Pre.class, e -> {
        for (StartTick l : ServerTickEvents.START_SERVER_TICK.listeners()) l.onStartTick(e.getServer());
    }));
    public static final Event<EndTick> END_SERVER_TICK = new Event<>(() -> NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, e -> {
        for (EndTick l : ServerTickEvents.END_SERVER_TICK.listeners()) l.onEndTick(e.getServer());
    }));
    public static final Event<StartWorldTick> START_WORLD_TICK = new Event<>(() -> NeoForge.EVENT_BUS.addListener(LevelTickEvent.Pre.class, e -> {
        if (e.getLevel() instanceof ServerLevel level) {
            for (StartWorldTick l : ServerTickEvents.START_WORLD_TICK.listeners()) l.onStartTick(level);
        }
    }));
    public static final Event<EndWorldTick> END_WORLD_TICK = new Event<>(() -> NeoForge.EVENT_BUS.addListener(LevelTickEvent.Post.class, e -> {
        if (e.getLevel() instanceof ServerLevel level) {
            for (EndWorldTick l : ServerTickEvents.END_WORLD_TICK.listeners()) l.onEndTick(level);
        }
    }));

    private ServerTickEvents() {
    }

    @FunctionalInterface
    public interface StartTick {
        void onStartTick(MinecraftServer server);
    }

    @FunctionalInterface
    public interface EndTick {
        void onEndTick(MinecraftServer server);
    }

    @FunctionalInterface
    public interface StartWorldTick {
        void onStartTick(ServerLevel world);
    }

    @FunctionalInterface
    public interface EndWorldTick {
        void onEndTick(ServerLevel world);
    }
}
