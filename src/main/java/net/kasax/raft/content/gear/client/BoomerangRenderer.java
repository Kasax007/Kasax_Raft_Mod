package net.kasax.raft.content.gear.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.kasax.raft.content.gear.entity.BoomerangEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ThrownItemRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;

/** Draws the boomerang lying flat and spinning fast around its middle. */
public class BoomerangRenderer extends EntityRenderer<BoomerangEntity, ThrownItemRenderState> {
    private final ItemModelResolver items;

    public BoomerangRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.items = context.getItemModelResolver();
    }

    @Override
    public ThrownItemRenderState createRenderState() {
        return new ThrownItemRenderState();
    }

    @Override
    public void extractRenderState(BoomerangEntity entity, ThrownItemRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        items.updateForNonLiving(state.item, entity.getItem(), ItemDisplayContext.GROUND, entity);
    }

    @Override
    public void submit(ThrownItemRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0.0F, 0.1F, 0.0F);
        pose.rotateDegrees(Axis.YP, state.ageInTicks * 45.0F);
        pose.rotateDegrees(Axis.XP, 90.0F);
        pose.scale(1.6F, 1.6F, 1.6F);
        state.item.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        pose.popPose();
        super.submit(state, pose, collector, camera);
    }
}
