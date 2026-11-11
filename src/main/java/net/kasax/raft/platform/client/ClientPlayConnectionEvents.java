package net.kasax.raft.platform.client;

import net.kasax.raft.platform.Event;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Fabric's ClientPlayConnectionEvents on NeoForge's client network events. */
public final class ClientPlayConnectionEvents {
    public static final Event<Join> JOIN = new Event<>(() -> NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingIn.class, e -> {
        Minecraft mc = Minecraft.getInstance();
        for (Join l : ClientPlayConnectionEvents.JOIN.listeners()) l.onPlayReady(mc.getConnection(), null, mc);
    }));
    public static final Event<Disconnect> DISCONNECT = new Event<>(() -> NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, e -> {
        Minecraft mc = Minecraft.getInstance();
        for (Disconnect l : ClientPlayConnectionEvents.DISCONNECT.listeners()) l.onPlayDisconnect(mc.getConnection(), mc);
    }));

    private ClientPlayConnectionEvents() {
    }

    @FunctionalInterface
    public interface Join {
        void onPlayReady(ClientPacketListener handler, Object sender, Minecraft client);
    }

    @FunctionalInterface
    public interface Disconnect {
        void onPlayDisconnect(ClientPacketListener handler, Minecraft client);
    }
}
