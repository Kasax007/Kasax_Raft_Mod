package net.kasax.raft.platform;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Fabric's ServerPlayerEvents.JOIN on NeoForge's {@link PlayerEvent.PlayerLoggedInEvent}. */
public final class ServerPlayerEvents {
    public static final Event<Join> JOIN = new Event<>(() -> NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, e -> {
        if (e.getEntity() instanceof ServerPlayer player) {
            for (Join l : ServerPlayerEvents.JOIN.listeners()) l.onJoin(player);
        }
    }));

    private ServerPlayerEvents() {
    }

    @FunctionalInterface
    public interface Join {
        void onJoin(ServerPlayer player);
    }
}
