package net.kasax.raft.platform;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Fabric's AFTER_PLAYER_CHANGE_LEVEL on NeoForge's {@link PlayerEvent.PlayerChangedDimensionEvent}. */
public final class ServerEntityLevelChangeEvents {
    public static final Event<AfterPlayerChange> AFTER_PLAYER_CHANGE_LEVEL = new Event<>(() -> NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerChangedDimensionEvent.class, e -> {
        if (e.getEntity() instanceof ServerPlayer player) {
            ServerLevel origin = player.level().getServer().getLevel(e.getFrom());
            ServerLevel destination = player.level().getServer().getLevel(e.getTo());
            for (AfterPlayerChange l : ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.listeners()) {
                l.afterChangeLevel(player, origin, destination);
            }
        }
    }));

    private ServerEntityLevelChangeEvents() {
    }

    @FunctionalInterface
    public interface AfterPlayerChange {
        void afterChangeLevel(ServerPlayer player, ServerLevel origin, ServerLevel destination);
    }
}
