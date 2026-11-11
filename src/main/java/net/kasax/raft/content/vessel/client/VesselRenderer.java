package net.kasax.raft.content.vessel.client;

import net.kasax.raft.content.vessel.block.HelmBlock;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.kasax.raft.content.vessel.VesselEntity;
import net.kasax.raft.content.vessel.VesselStructure;
import net.minecraft.client.Minecraft;
import net.kasax.raft.compat.render.SubmitNodeCollector;
import net.kasax.raft.compat.render.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.kasax.raft.compat.render.RenderTypes;
import net.kasax.raft.compat.render.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.kasax.raft.compat.ARGB;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Draws a sailing raft: every block with its normal block model (chests and other special blocks included), lit by
 * the world light at the block, rotated and moved with the raft; plus the outline of the block under the crosshair.
 */
public class VesselRenderer extends EntityRenderer<VesselEntity, VesselRenderState> {
    public VesselRenderer(EntityRendererProvider.Context context) {
        super(context);
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
            model = VesselClientModel.build(structure);
            entity.clientModel = model;
        }
        // an anchored raft is still drawn for its last few ticks, until the world has built the meshes of its blocks
        // (no gap); its light stays as it was, the cells are taken by the placed blocks now
        if (!entity.isDocked()) {
            model.updateLight(entity.level(), entity.renderPose(partialTicks), entity.level().getGameTime());
        }
        state.model = model;
        state.yaw = entity.getViewYRot(partialTicks);
        state.wheel = entity.getWheel();
        state.target = VesselPicker.targetOf(entity);
    }

    @Override
    public void submit(VesselRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        VesselClientModel model = state.model;
        if (model != null) {
            Vec3 pivot = model.structure.pivot();
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(-state.yaw));
            poseStack.translate(-pivot.x, -pivot.y, -pivot.z);
            net.minecraft.client.renderer.block.BlockRenderDispatcher blocks = Minecraft.getInstance().getBlockRenderer();
            for (int i = 0; i < model.states.length; i++) {
                poseStack.pushPose();
                poseStack.translate(model.x[i], model.y[i], model.z[i]);
                BlockState blockState = model.states[i];
                if (blockState.getRenderShape() == net.minecraft.world.level.block.RenderShape.ENTITYBLOCK_ANIMATED
                        && blockState.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
                    // chests and the like are drawn as their item (1.21.1 has no block model for them): turn it the block's way
                    float rot = blockState.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING).toYRot();
                    poseStack.translate(0.5F, 0.0F, 0.5F);
                    poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - rot));
                    poseStack.translate(-0.5F, 0.0F, -0.5F);
                }
                blocks.renderSingleBlock(blockState, poseStack, collector.buffers(), model.light[i], OverlayTexture.NO_OVERLAY);
                if (i == model.helmIndex && model.wheel != null) {
                    // the ship's wheel turns about the axis from stern to bow as the helmsman steers
                    Direction bow = blockState.getValue(HelmBlock.FACING);
                    poseStack.translate(0.5F, 0.75F, 0.5F);
                    poseStack.mulPose(new Quaternionf().rotationAxis(state.wheel * Mth.HALF_PI, bow.getStepX(), 0.0F, bow.getStepZ()));
                    poseStack.translate(-0.5F, -0.75F, -0.5F);
                    blocks.renderSingleBlock(model.wheel, poseStack, collector.buffers(), model.light[i], OverlayTexture.NO_OVERLAY);
                }
                poseStack.popPose();
            }
            BlockPos target = state.target;
            BlockState targetState = target == null ? null : model.structure.get(target);
            if (targetState != null) {
                VoxelShape shape = targetState.getShape(EmptyBlockGetter.INSTANCE, target);
                if (!shape.isEmpty()) {
                    poseStack.pushPose();
                    poseStack.translate(target.getX(), target.getY(), target.getZ());
                    collector.submitShapeOutline(poseStack, shape, RenderTypes.linesTranslucent(), ARGB.black(102), 1.0F, false);
                    poseStack.popPose();
                }
            }
            poseStack.popPose();
        }
        super.submit(state, poseStack, collector, camera);
    }
}
