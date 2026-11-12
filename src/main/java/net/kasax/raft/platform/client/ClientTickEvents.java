package net.kasax.raft.platform.client;

import net.kasax.raft.platform.Event;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Fabric's ClientTickEvents on NeoForge's client tick events. */
public final class ClientTickEvents {
    public static final Event<StartTick> START_CLIENT_TICK = new Event<>(() -> NeoForge.EVENT_BUS.addListener(ClientTickEvent.Pre.class, e -> {
        Minecraft mc = Minecraft.getInstance();
        for (StartTick l : ClientTickEvents.START_CLIENT_TICK.listeners()) l.onStartTick(mc);
    }));
    public static final Event<EndTick> END_CLIENT_TICK = new Event<>(() -> NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, e -> {
        Minecraft mc = Minecraft.getInstance();
        for (EndTick l : ClientTickEvents.END_CLIENT_TICK.listeners()) l.onEndTick(mc);
    }));

    private ClientTickEvents() {
    }

    @FunctionalInterface
    public interface StartTick {
        void onStartTick(Minecraft client);
    }

    @FunctionalInterface
    public interface EndTick {
        void onEndTick(Minecraft client);
    }
}
