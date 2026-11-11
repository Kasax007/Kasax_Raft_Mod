package net.kasax.raft.platform;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Fabric's AFTER_PLAYER_CHANGE_WORLD on NeoForge's {@link PlayerEvent.PlayerChangedDimensionEvent}. */
public final class ServerEntityWorldChangeEvents {
    public static final Event<AfterPlayerChange> AFTER_PLAYER_CHANGE_WORLD = new Event<>(() -> NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerChangedDimensionEvent.class, e -> {
        if (e.getEntity() instanceof ServerPlayer player) {
            ServerLevel origin = player.server.getLevel(e.getFrom());
            ServerLevel destination = player.server.getLevel(e.getTo());
            for (AfterPlayerChange l : ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.listeners()) {
                l.afterChangeWorld(player, origin, destination);
            }
        }
    }));

    private ServerEntityWorldChangeEvents() {
    }

    @FunctionalInterface
    public interface AfterPlayerChange {
        void afterChangeWorld(ServerPlayer player, ServerLevel origin, ServerLevel destination);
    }
}
