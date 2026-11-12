package net.kasax.raft.platform;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

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
    /**
     * Fabric's ALLOW_LOAD for newly spawned mobs: asked in NeoForge's {@link FinalizeSpawnEvent} with the spawn reason
     * (natural, chunk generation, patrol, reinforcement, jockey, ...); a listener that answers false cancels the spawn.
     */
    public static final Event<AllowLoad> ALLOW_LOAD = new Event<>(() -> NeoForge.EVENT_BUS.addListener(FinalizeSpawnEvent.class, e -> {
        if (e.getLevel().getLevel() instanceof ServerLevel level) {
            for (AllowLoad l : ServerEntityEvents.ALLOW_LOAD.listeners()) {
                if (!l.allowLoad(e.getEntity(), level, e.getSpawnType(), false)) {
                    e.setSpawnCancelled(true);
                    return;
                }
            }
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
    public interface AllowLoad {
        boolean allowLoad(Entity entity, ServerLevel level, net.minecraft.world.entity.EntitySpawnReason reason, boolean fromDisk);
    }

    @FunctionalInterface
    public interface Unload {
        void onUnload(Entity entity, ServerLevel world);
    }
}
