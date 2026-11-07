package net.kasax.raft.content.building.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.kasax.raft.Raft;
import net.kasax.raft.content.building.block.WindTurbineBlock;
import net.kasax.raft.content.building.block.WindTurbineBlockEntity;
import net.kasax.raft.compat.render.SubmitNodeCollector;
import net.kasax.raft.compat.render.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.kasax.raft.compat.render.BlockEntityRenderState;
import net.kasax.raft.compat.render.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Draws the spinning rotor of the wind turbine in front of the block. */
public class WindTurbineRenderer implements BlockEntityRenderer<WindTurbineBlockEntity, WindTurbineRenderer.State> {
    public static final ResourceLocation TEXTURE = Raft.id("textures/entity/wind_turbine_rotor.png");

    private final WindTurbineRotorModel model;

    public WindTurbineRenderer(BlockEntityRendererProvider.Context context) {
        this.model = new WindTurbineRotorModel(context.bakeLayer(WindTurbineRotorModel.LAYER));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(WindTurbineBlockEntity entity, State state, float partialTicks, Vec3 cameraPosition,
                                   @Nullable Object breakProgress) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTicks, cameraPosition, breakProgress);
        state.facing = entity.getBlockState().hasProperty(WindTurbineBlock.FACING)
                ? entity.getBlockState().getValue(WindTurbineBlock.FACING) : Direction.NORTH;
        // a fixed offset per block so a row of turbines does not spin in lockstep
        state.angle = entity.getRotorAngle(partialTicks) + (entity.getBlockPos().hashCode() & 0x7F);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.facing.toYRot()));
        // rotor axis: block centre height, front face of the block (north side before turning)
        poseStack.translate(0.0F, 0.0F, -0.5F);
        model.setupAnim(state.angle);
        collector.submitModel(model, state.angle, poseStack, TEXTURE, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    public static class State extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
        public float angle;
    }
}
