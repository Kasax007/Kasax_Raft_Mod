package net.kasax.raft.content.marine.client;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.kasax.raft.Raft;
import net.kasax.raft.client.render.SharkModel;
import net.kasax.raft.client.render.SharkRenderer;
import net.kasax.raft.content.marine.MarineEntities;
import net.kasax.raft.content.marine.client.model.AnglerfishModel;
import net.kasax.raft.content.marine.client.model.JellyfishModel;
import net.kasax.raft.content.marine.client.model.MantaRayModel;
import net.kasax.raft.content.marine.client.model.MorayEelModel;
import net.kasax.raft.content.marine.client.model.WhaleModel;
import net.kasax.raft.content.marine.client.render.AnglerfishRenderer;
import net.kasax.raft.content.marine.client.render.JellyfishRenderer;
import net.kasax.raft.content.marine.client.render.MantaRayRenderer;
import net.kasax.raft.content.marine.client.render.MorayEelRenderer;
import net.kasax.raft.content.marine.client.render.WhaleRenderer;
import net.minecraft.resources.ResourceLocation;

/** Client-only init of the "marine" module: model layers and renderers of the sea creatures. */
public final class MarineClient {
    private static final ResourceLocation HAMMERHEAD_TEXTURE = Raft.id("textures/entity/hammerhead_shark.png");

    private MarineClient() {
    }

    public static void init() {
        EntityModelLayerRegistry.registerModelLayer(SharkModel.HAMMERHEAD_LAYER, SharkModel::createHammerheadLayer);
        EntityModelLayerRegistry.registerModelLayer(WhaleModel.LAYER, WhaleModel::createBodyLayer);
        EntityModelLayerRegistry.registerModelLayer(MantaRayModel.LAYER, MantaRayModel::createBodyLayer);
        EntityModelLayerRegistry.registerModelLayer(JellyfishModel.LAYER, JellyfishModel::createBodyLayer);
        EntityModelLayerRegistry.registerModelLayer(AnglerfishModel.LAYER, AnglerfishModel::createBodyLayer);
        EntityModelLayerRegistry.registerModelLayer(MorayEelModel.LAYER, MorayEelModel::createBodyLayer);

        EntityRendererRegistry.register(MarineEntities.HAMMERHEAD_SHARK,
                context -> new SharkRenderer(context, SharkModel.HAMMERHEAD_LAYER, HAMMERHEAD_TEXTURE, 1.25F, 1.0F));
        EntityRendererRegistry.register(MarineEntities.WHALE, WhaleRenderer::new);
        EntityRendererRegistry.register(MarineEntities.MANTA_RAY, MantaRayRenderer::new);
        EntityRendererRegistry.register(MarineEntities.JELLYFISH, JellyfishRenderer::new);
        EntityRendererRegistry.register(MarineEntities.ANGLERFISH, AnglerfishRenderer::new);
        EntityRendererRegistry.register(MarineEntities.MORAY_EEL, MorayEelRenderer::new);
    }
}
