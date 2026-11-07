package net.kasax.raft.content.encounters.client;

import net.kasax.raft.Raft;
import net.kasax.raft.content.encounters.entity.ScavengerEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.Items;

/** Scavenger (and Scavenger Chief: same clothes, bigger, in iron). */
public class ScavengerRenderer extends HumanoidMobRenderer<ScavengerEntity, SkeletonRenderState, ScavengerModel> {
    private static final Identifier TEXTURE = Raft.id("textures/entity/scavenger/scavenger.png");

    public ScavengerRenderer(EntityRendererProvider.Context context) {
        super(context, new ScavengerModel(context.bakeLayer(ScavengerModel.LAYER)), 0.5F);
        this.addLayer(new HumanoidArmorLayer<>(this, ArmorModelSet.bake(ModelLayers.ZOMBIE_ARMOR, context.getModelSet(), ScavengerModel::new),
                context.getEquipmentRenderer()));
    }

    @Override
    public SkeletonRenderState createRenderState() {
        return new SkeletonRenderState();
    }

    @Override
    public void extractRenderState(ScavengerEntity entity, SkeletonRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.isAggressive = entity.isAggressive();
        state.isHoldingBow = entity.getMainHandItem().is(Items.BOW);
    }

    @Override
    protected HumanoidModel.ArmPose getArmPose(ScavengerEntity mob, HumanoidArm arm) {
        return mob.getMainArm() == arm && mob.isAggressive() && mob.getMainHandItem().is(Items.BOW)
                ? HumanoidModel.ArmPose.BOW_AND_ARROW
                : super.getArmPose(mob, arm);
    }

    @Override
    public Identifier getTextureLocation(SkeletonRenderState state) {
        return TEXTURE;
    }
}
