package net.kasax.raft.content.voyage.client;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.kasax.raft.content.voyage.VoyageContent;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.BoatModel;
import net.minecraft.client.model.RaftModel;
import net.minecraft.client.model.ChestBoatModel;
import net.minecraft.client.model.ChestRaftModel;

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
        EntityModelLayerRegistry.registerModelLayer(PALM_BOAT, BoatModel::createBodyModel);
        EntityModelLayerRegistry.registerModelLayer(PALM_CHEST_BOAT, ChestBoatModel::createBodyModel);
        EntityModelLayerRegistry.registerModelLayer(DRIFTWOOD_RAFT, RaftModel::createBodyModel);
        EntityModelLayerRegistry.registerModelLayer(DRIFTWOOD_CHEST_RAFT, ChestRaftModel::createBodyModel);
        EntityModelLayerRegistry.registerModelLayer(ExpeditionRaftModel.LAYER, ExpeditionRaftGeometry::createBodyLayer);
        EntityModelLayerRegistry.registerModelLayer(ExpeditionRaftModel.WATER_MASK, ExpeditionRaftGeometry::createWaterMask);

        EntityRendererRegistry.register(VoyageContent.PALM_BOAT, context -> new ModBoatRenderer(context, PALM_BOAT, BoatModel::new));
        EntityRendererRegistry.register(VoyageContent.PALM_CHEST_BOAT, context -> new ModBoatRenderer(context, PALM_CHEST_BOAT, ChestBoatModel::new));
        EntityRendererRegistry.register(VoyageContent.DRIFTWOOD_RAFT, context -> new ModBoatRenderer(context, DRIFTWOOD_RAFT, RaftModel::new));
        EntityRendererRegistry.register(VoyageContent.DRIFTWOOD_CHEST_RAFT, context -> new ModBoatRenderer(context, DRIFTWOOD_CHEST_RAFT, ChestRaftModel::new));
        EntityRendererRegistry.register(VoyageContent.EXPEDITION_RAFT, ExpeditionRaftRenderer::new);

        MenuScreens.register(VoyageContent.EXPEDITION_RAFT_MENU, ExpeditionRaftScreen::new);
    }
}
