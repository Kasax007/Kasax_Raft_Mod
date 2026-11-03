package net.kasax.raft.content.diving.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.kasax.raft.Raft;
import net.kasax.raft.content.diving.entity.Harpoon;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.phys.AABB;

/** Draws the harpoon model along its flight direction; it quivers a moment after it sticks in a block. */
public class HarpoonRenderer extends EntityRenderer<Harpoon, HarpoonRenderState> {
    public static final Identifier TEXTURE = Raft.id("textures/entity/diving/harpoon.png");
    private final HarpoonModel model;

    public HarpoonRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new HarpoonModel(context.bakeLayer(HarpoonModel.LAYER));
    }

    @Override
    public void submit(HarpoonRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.rotateDegrees(Axis.YP, state.yRot - 90.0F);
        poseStack.rotateDegrees(Axis.ZP, state.xRot + 90.0F);
        if (state.shake > 0.0F) {
            float wobble = -Mth.sin(state.shake * 3.0F) * state.shake;
            poseStack.rotateDegrees(Axis.XP, wobble);
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
        state.yRot = entity.getYRot(partialTicks);
        state.xRot = entity.getXRot(partialTicks);
        state.shake = entity.shakeTime - partialTicks;
    }
}
