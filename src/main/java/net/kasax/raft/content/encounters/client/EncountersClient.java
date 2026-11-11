package net.kasax.raft.content.encounters.client;

import net.kasax.raft.platform.client.EntityRendererRegistry;
import net.kasax.raft.platform.client.EntityModelLayerRegistry;
import net.kasax.raft.content.encounters.EncounterEntities;

/** Client-only init of the "encounters" module: model layer and renderers. */
public final class EncountersClient {
    private EncountersClient() {
    }

    public static void init() {
        EntityModelLayerRegistry.registerModelLayer(ScavengerModel.LAYER, ScavengerModel::createBodyLayer);
        EntityRendererRegistry.register(EncounterEntities.SCAVENGER, ScavengerRenderer::new);
        EntityRendererRegistry.register(EncounterEntities.REEF_GUARDIAN, ReefGuardianRenderer::new);
        EntityRendererRegistry.register(EncounterEntities.CORSAIR_CAPTAIN, CaptainRenderer::new);
    }
}
