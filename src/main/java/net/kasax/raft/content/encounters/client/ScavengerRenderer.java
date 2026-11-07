package net.kasax.raft.content.encounters.client;

import net.kasax.raft.Raft;
import net.kasax.raft.content.encounters.entity.ScavengerEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

/** Scavenger (and Scavenger Chief: same clothes, bigger, in iron). */
public class ScavengerRenderer extends HumanoidMobRenderer<ScavengerEntity, ScavengerModel> {
    private static final ResourceLocation TEXTURE = Raft.id("textures/entity/scavenger/scavenger.png");

    public ScavengerRenderer(EntityRendererProvider.Context context) {
        super(context, new ScavengerModel(context.bakeLayer(ScavengerModel.LAYER)), 0.5F);
        this.addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE_OUTER_ARMOR)), context.getModelManager()));
    }

    @Override
    public ResourceLocation getTextureLocation(ScavengerEntity entity) {
        return TEXTURE;
    }
}
