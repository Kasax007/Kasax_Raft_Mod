package net.kasax.raft.content.cozy.client;

import net.kasax.raft.content.cozy.CozyContent;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

/** Client init of the cozy module: the food on serving plates. */
public final class CozyClient {
    private CozyClient() {
    }

    public static void init() {
        BlockEntityRenderers.register(CozyContent.PLATE_ENTITY, PlateRenderer::new);
    }
}
