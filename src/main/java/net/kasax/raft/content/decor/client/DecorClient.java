package net.kasax.raft.content.decor.client;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.kasax.raft.content.decor.DecorEntities;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

/** Client-only init of the "decor" module: the seat renders nothing, the cannonball as its item. */
public final class DecorClient {
    private DecorClient() {
    }

    public static void init() {
        EntityRendererRegistry.register(DecorEntities.SEAT, NoopRenderer::new);
        EntityRendererRegistry.register(DecorEntities.CANNONBALL, ctx -> new ThrownItemRenderer<>(ctx, 1.25F, false));
    }
}
