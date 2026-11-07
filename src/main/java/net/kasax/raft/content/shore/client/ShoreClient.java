package net.kasax.raft.content.shore.client;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.kasax.raft.content.shore.ShoreEntities;
import net.kasax.raft.content.shore.client.model.CorsairModel;
import net.kasax.raft.content.shore.client.model.CrabModel;
import net.kasax.raft.content.shore.client.model.PenguinModel;
import net.kasax.raft.content.shore.client.model.SeaOtterModel;
import net.kasax.raft.content.shore.client.model.SeagullModel;
import net.kasax.raft.content.shore.client.render.CorsairRenderer;
import net.kasax.raft.content.shore.client.render.CrabRenderer;
import net.kasax.raft.content.shore.client.render.EmberCrabRenderer;
import net.kasax.raft.content.shore.client.render.PenguinRenderer;
import net.kasax.raft.content.shore.client.render.SeaOtterRenderer;
import net.kasax.raft.content.shore.client.render.SeagullRenderer;

/** Client-only init of the "shore" module: model layers and renderers of the shore creatures. */
public final class ShoreClient {
    private ShoreClient() {
    }

    public static void init() {
        EntityModelLayerRegistry.registerModelLayer(CrabModel.LAYER, CrabModel::createBodyLayer);
        EntityModelLayerRegistry.registerModelLayer(CrabModel.EMBER_LAYER, CrabModel::createEmberLayer);
        EntityModelLayerRegistry.registerModelLayer(SeagullModel.LAYER, SeagullModel::createBodyLayer);
        EntityModelLayerRegistry.registerModelLayer(PenguinModel.LAYER, PenguinModel::createBodyLayer);
        EntityModelLayerRegistry.registerModelLayer(SeaOtterModel.LAYER, SeaOtterModel::createBodyLayer);
        EntityModelLayerRegistry.registerModelLayer(CorsairModel.HAT_LAYER, CorsairModel::createHatLayer);

        EntityRendererRegistry.register(ShoreEntities.CRAB, CrabRenderer::new);
        EntityRendererRegistry.register(ShoreEntities.EMBER_CRAB, EmberCrabRenderer::new);
        EntityRendererRegistry.register(ShoreEntities.SEAGULL, SeagullRenderer::new);
        EntityRendererRegistry.register(ShoreEntities.PENGUIN, PenguinRenderer::new);
        EntityRendererRegistry.register(ShoreEntities.SEA_OTTER, SeaOtterRenderer::new);
        EntityRendererRegistry.register(ShoreEntities.DROWNED_CORSAIR, CorsairRenderer::new);
    }
}
