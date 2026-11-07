package net.kasax.raft.content.shore.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.kasax.raft.Raft;
import net.kasax.raft.content.shore.client.model.SeagullModel;
import net.kasax.raft.content.shore.entity.SeagullEntity;
import net.kasax.raft.compat.render.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.kasax.raft.compat.render.MobRenderer;
import net.kasax.raft.compat.render.RenderLayer;
import net.kasax.raft.compat.render.HoldingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Seagull with the food it snatched held in its beak. */
public class SeagullRenderer extends MobRenderer<SeagullEntity, SeagullRenderState, SeagullModel> {
    private static final ResourceLocation TEXTURE = Raft.id("textures/entity/seagull.png");

    public SeagullRenderer(EntityRendererProvider.Context context) {
        super(context, new SeagullModel(context.bakeLayer(SeagullModel.LAYER)), 0.3F);
        this.addLayer(new BeakItemLayer(this));
    }

    @Override
    public SeagullRenderState createRenderState() {
        return new SeagullRenderState();
    }

    @Override
    public void extractRenderState(SeagullEntity entity, SeagullRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        HoldingEntityRenderState.extractHoldingEntityRenderState(entity, state, this.itemModelResolver);
        state.flyAmount = entity.getFlyAmount(partialTicks);
        state.flapPhase = entity.getFlapPhase(partialTicks);
        state.flapPower = entity.getFlapPower(partialTicks);
        state.roll = entity.getRoll(partialTicks);
        Vec3 motion = entity.getDeltaMovement();
        double horizontal = motion.horizontalDistance();
        state.flightPitch = horizontal < 0.01 ? 0.0F : (float) Mth.clamp(-Math.atan2(motion.y, horizontal), -0.6, 0.6);
        state.floating = entity.isFloating();
        state.holdsItem = !entity.getMainHandItem().isEmpty();
        state.peck.copyFrom(entity.peckAnimation);
        state.call.copyFrom(entity.callAnimation);
        state.seed = (entity.getId() * 0.6180339F) % 6.2831855F;
    }

    @Override
    public ResourceLocation getTextureLocation(SeagullRenderState state) {
        return TEXTURE;
    }

    /** The stolen food sticks out of the beak. */
    private static class BeakItemLayer extends RenderLayer<SeagullRenderState, SeagullModel> {
        BeakItemLayer(SeagullRenderer renderer) {
            super(renderer);
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, SeagullRenderState state,
                           float yRot, float xRot) {
            if (state.heldItem.isEmpty()) {
                return;
            }
            poseStack.pushPose();
            this.getParentModel().translateToBeak(poseStack);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            poseStack.scale(0.5F, 0.5F, 0.5F);
            state.heldItem.submit(poseStack, collector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
            poseStack.popPose();
        }
    }
}
