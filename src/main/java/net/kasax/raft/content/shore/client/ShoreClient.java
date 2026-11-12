package net.kasax.raft.content.shore.client;

import net.kasax.raft.platform.client.EntityRendererRegistry;
import net.kasax.raft.platform.client.ModelLayerRegistry;
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
        ModelLayerRegistry.registerModelLayer(CrabModel.LAYER, CrabModel::createBodyLayer);
        ModelLayerRegistry.registerModelLayer(CrabModel.EMBER_LAYER, CrabModel::createEmberLayer);
        ModelLayerRegistry.registerModelLayer(SeagullModel.LAYER, SeagullModel::createBodyLayer);
        ModelLayerRegistry.registerModelLayer(PenguinModel.LAYER, PenguinModel::createBodyLayer);
        ModelLayerRegistry.registerModelLayer(SeaOtterModel.LAYER, SeaOtterModel::createBodyLayer);
        ModelLayerRegistry.registerModelLayer(CorsairModel.HAT_LAYER, CorsairModel::createHatLayer);

        EntityRendererRegistry.register(ShoreEntities.CRAB, CrabRenderer::new);
        EntityRendererRegistry.register(ShoreEntities.EMBER_CRAB, EmberCrabRenderer::new);
        EntityRendererRegistry.register(ShoreEntities.SEAGULL, SeagullRenderer::new);
        EntityRendererRegistry.register(ShoreEntities.PENGUIN, PenguinRenderer::new);
        EntityRendererRegistry.register(ShoreEntities.SEA_OTTER, SeaOtterRenderer::new);
        EntityRendererRegistry.register(ShoreEntities.DROWNED_CORSAIR, CorsairRenderer::new);
    }
}
