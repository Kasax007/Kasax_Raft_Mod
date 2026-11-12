package net.kasax.raft.platform.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/** Fabric's EntityRendererRegistry: vanilla's renderer table, read when the renderers are built. */
public final class EntityRendererRegistry {
    private EntityRendererRegistry() {
    }

    public static <E extends Entity> void register(EntityType<? extends E> entityType, EntityRendererProvider<E> entityRendererFactory) {
        EntityRenderers.register(entityType, entityRendererFactory);
    }
}
