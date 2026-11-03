package net.kasax.raft.content.voyage.client;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.kasax.raft.content.voyage.VoyageContent;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.object.boat.BoatModel;
import net.minecraft.client.model.object.boat.RaftModel;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.client.renderer.entity.RaftRenderer;

/** Client-only init of the "voyage" module: boat/raft renderers and model layers, the Expedition Raft and its screen. */
public final class VoyageClient {
    /** Texture of each layer: textures/entity/&lt;layer path&gt;.png (vanilla boat convention). */
    public static final ModelLayerLocation PALM_BOAT = layer("boat/palm");
    public static final ModelLayerLocation PALM_CHEST_BOAT = layer("chest_boat/palm");
    public static final ModelLayerLocation DRIFTWOOD_RAFT = layer("boat/driftwood");
    public static final ModelLayerLocation DRIFTWOOD_CHEST_RAFT = layer("chest_boat/driftwood");

    private VoyageClient() {
    }

    private static ModelLayerLocation layer(String path) {
        return new ModelLayerLocation(net.kasax.raft.Raft.id(path), "main");
    }

    public static void init() {
        ModelLayerRegistry.registerModelLayer(PALM_BOAT, BoatModel::createBoatModel);
        ModelLayerRegistry.registerModelLayer(PALM_CHEST_BOAT, BoatModel::createChestBoatModel);
        ModelLayerRegistry.registerModelLayer(DRIFTWOOD_RAFT, RaftModel::createRaftModel);
        ModelLayerRegistry.registerModelLayer(DRIFTWOOD_CHEST_RAFT, RaftModel::createChestRaftModel);
        ModelLayerRegistry.registerModelLayer(ExpeditionRaftModel.LAYER, ExpeditionRaftGeometry::createBodyLayer);
        ModelLayerRegistry.registerModelLayer(ExpeditionRaftModel.WATER_MASK, ExpeditionRaftGeometry::createWaterMask);

        EntityRendererRegistry.register(VoyageContent.PALM_BOAT, context -> new BoatRenderer(context, PALM_BOAT));
        EntityRendererRegistry.register(VoyageContent.PALM_CHEST_BOAT, context -> new BoatRenderer(context, PALM_CHEST_BOAT));
        EntityRendererRegistry.register(VoyageContent.DRIFTWOOD_RAFT, context -> new RaftRenderer(context, DRIFTWOOD_RAFT));
        EntityRendererRegistry.register(VoyageContent.DRIFTWOOD_CHEST_RAFT, context -> new RaftRenderer(context, DRIFTWOOD_CHEST_RAFT));
        EntityRendererRegistry.register(VoyageContent.EXPEDITION_RAFT, ExpeditionRaftRenderer::new);

        MenuScreens.register(VoyageContent.EXPEDITION_RAFT_MENU, ExpeditionRaftScreen::new);
    }
}
