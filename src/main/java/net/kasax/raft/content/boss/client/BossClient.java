package net.kasax.raft.content.boss.client;

import net.kasax.raft.platform.client.EntityRendererRegistry;
import net.kasax.raft.platform.client.ModelLayerRegistry;
import net.kasax.raft.content.boss.BossEntities;
import net.kasax.raft.content.boss.client.model.BrineBoltModel;
import net.kasax.raft.content.boss.client.model.LeviathanModel;
import net.kasax.raft.content.boss.client.model.TidelingModel;
import net.kasax.raft.content.boss.client.render.BrineBoltRenderer;
import net.kasax.raft.content.boss.client.render.LeviathanRenderer;
import net.kasax.raft.content.boss.client.render.TidelingRenderer;

/** Client-only init of the "boss" module: model layers and renderers of the Leviathan, tidelings and brine bolts. */
public final class BossClient {
    private BossClient() {
    }

    public static void init() {
        ModelLayerRegistry.registerModelLayer(LeviathanModel.LAYER, LeviathanModel::createBodyLayer);
        ModelLayerRegistry.registerModelLayer(TidelingModel.LAYER, TidelingModel::createBodyLayer);
        ModelLayerRegistry.registerModelLayer(BrineBoltModel.LAYER, BrineBoltModel::createBodyLayer);
        EntityRendererRegistry.register(BossEntities.LEVIATHAN, LeviathanRenderer::new);
        EntityRendererRegistry.register(BossEntities.TIDELING, TidelingRenderer::new);
        EntityRendererRegistry.register(BossEntities.BRINE_BOLT, BrineBoltRenderer::new);
    }
}
