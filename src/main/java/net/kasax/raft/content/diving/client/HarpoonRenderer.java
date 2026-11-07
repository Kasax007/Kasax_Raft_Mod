package net.kasax.raft.content.diving.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.kasax.raft.Raft;
import net.kasax.raft.content.diving.entity.Harpoon;
import net.kasax.raft.compat.render.SubmitNodeCollector;
import net.kasax.raft.compat.render.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.kasax.raft.compat.render.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.phys.AABB;

/** Draws the harpoon model along its flight direction; it quivers a moment after it sticks in a block. */
public class HarpoonRenderer extends EntityRenderer<Harpoon, HarpoonRenderState> {
    public static final ResourceLocation TEXTURE = Raft.id("textures/entity/diving/harpoon.png");
    private final HarpoonModel model;

    public HarpoonRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new HarpoonModel(context.bakeLayer(HarpoonModel.LAYER));
    }

    @Override
    public void submit(HarpoonRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yRot - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.xRot + 90.0F));
        if (state.shake > 0.0F) {
            float wobble = -Mth.sin(state.shake * 3.0F) * state.shake;
            poseStack.mulPose(Axis.XP.rotationDegrees(wobble));
        }
        // the point (model -Y) reaches about a third of a block in front of the entity, so a stuck harpoon sits in the block
        poseStack.translate(0.0F, 0.1F, 0.0F);
        collector.submitModel(model, Unit.INSTANCE, poseStack, TEXTURE, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    @Override
    protected AABB getBoundingBoxForCulling(Harpoon entity, float partialTicks) {
        return super.getBoundingBoxForCulling(entity, partialTicks).inflate(1.0);
    }

    @Override
    public HarpoonRenderState createRenderState() {
        return new HarpoonRenderState();
    }

    @Override
    public void extractRenderState(Harpoon entity, HarpoonRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getViewYRot(partialTicks);
        state.xRot = entity.getViewXRot(partialTicks);
        state.shake = entity.shakeTime - partialTicks;
    }
}
