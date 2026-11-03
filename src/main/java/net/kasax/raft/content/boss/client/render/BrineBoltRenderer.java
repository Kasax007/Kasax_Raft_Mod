package net.kasax.raft.content.boss.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.kasax.raft.Raft;
import net.kasax.raft.content.boss.client.model.BrineBoltModel;
import net.kasax.raft.content.boss.entity.BrineBoltEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

/** Draws the brine bolt as a full-bright glowing teal blob. */
public class BrineBoltRenderer extends EntityRenderer<BrineBoltEntity, EntityRenderState> {
    private static final Identifier TEXTURE = Raft.id("textures/entity/leviathan/brine_bolt.png");
    private static final RenderType TYPE = RenderTypes.entityTranslucentEmissive(TEXTURE);
    private final BrineBoltModel model;

    public BrineBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new BrineBoltModel(context.bakeLayer(BrineBoltModel.LAYER));
    }

    @Override
    protected int getBlockLightLevel(BrineBoltEntity entity, BlockPos pos) {
        return 15;
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    @Override
    public void submit(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.25F, 0.0F);
        poseStack.scale(1.4F, 1.4F, 1.4F);
        collector.submitModel(this.model, state, poseStack, TYPE, 15728880, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }
}
