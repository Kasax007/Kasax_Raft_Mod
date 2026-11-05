package net.kasax.raft.content.vessel.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.kasax.raft.content.vessel.VesselEntity;
import net.kasax.raft.content.vessel.VesselStructure;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Draws a sailing raft: every block with its normal block model (chests and other special blocks included), lit by
 * the world light at the block, rotated and moved with the raft; plus the outline of the block under the crosshair.
 */
@Environment(EnvType.CLIENT)
public class VesselRenderer extends EntityRenderer<VesselEntity, VesselRenderState> {
    private final BlockModelResolver resolver;

    public VesselRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.resolver = context.getBlockModelResolver();
        this.shadowRadius = 0.0F;
    }

    @Override
    public VesselRenderState createRenderState() {
        return new VesselRenderState();
    }

    @Override
    public void extractRenderState(VesselEntity entity, VesselRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.model = null;
        state.target = null;
        VesselStructure structure = entity.structure();
        if (structure == null) {
            return;
        }
        VesselClientModel model = entity.clientModel instanceof VesselClientModel m && m.isCurrent(structure) ? m : null;
        if (model == null) {
            model = VesselClientModel.build(structure, this.resolver);
            entity.clientModel = model;
        }
        // an anchored raft is still drawn for its last few ticks, until the world has built the meshes of its blocks
        // (no gap); its light stays as it was, the cells are taken by the placed blocks now
        if (!entity.isDocked()) {
            model.updateLight(entity.level(), entity.renderPose(partialTicks), entity.level().getGameTime());
        }
        state.model = model;
        state.yaw = entity.getYRot(partialTicks);
        state.target = VesselPicker.targetOf(entity);
    }

    @Override
    public void submit(VesselRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        VesselClientModel model = state.model;
        if (model != null) {
            Vec3 pivot = model.structure.pivot();
            poseStack.pushPose();
            poseStack.rotateDegrees(Axis.YP, -state.yaw);
            poseStack.translate(-pivot.x, -pivot.y, -pivot.z);
            for (int i = 0; i < model.models.length; i++) {
                poseStack.pushPose();
                poseStack.translate(model.x[i], model.y[i], model.z[i]);
                model.models[i].submit(poseStack, collector, model.light[i], OverlayTexture.NO_OVERLAY, state.outlineColor);
                poseStack.popPose();
            }
            BlockPos target = state.target;
            BlockState targetState = target == null ? null : model.structure.get(target);
            if (targetState != null) {
                VoxelShape shape = targetState.getShape(EmptyBlockGetter.INSTANCE, target);
                if (!shape.isEmpty()) {
                    poseStack.pushPose();
                    poseStack.translate(target.getX(), target.getY(), target.getZ());
                    float width = Minecraft.getInstance().gameRenderer.gameRenderState().windowRenderState.appropriateLineWidth;
                    collector.submitShapeOutline(poseStack, shape, RenderTypes.linesTranslucent(), ARGB.black(102), width, false);
                    poseStack.popPose();
                }
            }
            poseStack.popPose();
        }
        super.submit(state, poseStack, collector, camera);
    }
}
