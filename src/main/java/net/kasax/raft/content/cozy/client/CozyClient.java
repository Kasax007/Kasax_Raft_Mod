package net.kasax.raft.content.cozy.client;

import net.kasax.raft.content.cozy.CozyContent;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

/** Client init of the cozy module: the food on serving plates, the items on shelves (1.21.1: own shelf renderer). */
public final class CozyClient {
    private CozyClient() {
    }

    public static void init() {
        BlockEntityRenderers.register(CozyContent.PLATE_ENTITY, PlateRenderer::new);
        BlockEntityRenderers.register(CozyContent.SHELF_ENTITY, ShelfRenderer::new);
    }
}
