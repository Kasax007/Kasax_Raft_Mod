package net.kasax.raft.platform;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Fabric's ServerPlayConnectionEvents JOIN / DISCONNECT on NeoForge's player log in / out events. */
public final class ServerPlayConnectionEvents {
    public static final Event<Join> JOIN = new Event<>(() -> NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, e -> {
        if (e.getEntity() instanceof ServerPlayer player && player.connection != null) {
            for (Join l : ServerPlayConnectionEvents.JOIN.listeners()) l.onPlayReady(player.connection, null, player.level().getServer());
        }
    }));
    public static final Event<Disconnect> DISCONNECT = new Event<>(() -> NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, e -> {
        if (e.getEntity() instanceof ServerPlayer player && player.connection != null) {
            for (Disconnect l : ServerPlayConnectionEvents.DISCONNECT.listeners()) l.onPlayDisconnect(player.connection, player.level().getServer());
        }
    }));

    private ServerPlayConnectionEvents() {
    }

    /** Fabric passes a packet sender; nothing in Beach Craft uses it, so it is always null here. */
    @FunctionalInterface
    public interface Join {
        void onPlayReady(ServerGamePacketListenerImpl handler, Object sender, MinecraftServer server);
    }

    @FunctionalInterface
    public interface Disconnect {
        void onPlayDisconnect(ServerGamePacketListenerImpl handler, MinecraftServer server);
    }
}
