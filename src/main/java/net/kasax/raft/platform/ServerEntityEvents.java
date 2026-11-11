package net.kasax.raft.platform;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;

/**
 * Fabric's ServerEntityEvents: load on {@link EntityJoinLevelEvent} (last, only when no one cancelled it), unload on
 * {@link EntityLeaveLevelEvent}, which NeoForge fires where Fabric fires its unload (the level stops tracking the
 * entity).
 */
public final class ServerEntityEvents {
    public static final Event<Load> ENTITY_LOAD = new Event<>(() -> NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, EntityJoinLevelEvent.class, e -> {
        if (!e.isCanceled() && e.getLevel() instanceof ServerLevel level) {
            for (Load l : ServerEntityEvents.ENTITY_LOAD.listeners()) l.onLoad(e.getEntity(), level);
        }
    }));
    public static final Event<Unload> ENTITY_UNLOAD = new Event<>(() -> NeoForge.EVENT_BUS.addListener(EntityLeaveLevelEvent.class, e -> {
        if (e.getLevel() instanceof ServerLevel level) {
            for (Unload l : ServerEntityEvents.ENTITY_UNLOAD.listeners()) l.onUnload(e.getEntity(), level);
        }
    }));

    private ServerEntityEvents() {
    }

    @FunctionalInterface
    public interface Load {
        void onLoad(Entity entity, ServerLevel world);
    }

    @FunctionalInterface
    public interface Unload {
        void onUnload(Entity entity, ServerLevel world);
    }
}
