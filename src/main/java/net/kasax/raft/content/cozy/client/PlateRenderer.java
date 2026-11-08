package net.kasax.raft.content.cozy.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.kasax.raft.content.cozy.block.PlateBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Draws the food on a serving plate: up to four portions lying flat, slightly fanned out. */
public class PlateRenderer implements BlockEntityRenderer<PlateBlockEntity, PlateRenderer.State> {
    private final ItemModelResolver items;

    public PlateRenderer(BlockEntityRendererProvider.Context context) {
        this.items = context.itemModelResolver();
    }

    public static class State extends BlockEntityRenderState {
        final ItemStackRenderState item = new ItemStackRenderState();
        int count;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(PlateBlockEntity plate, State state, float partialTicks, Vec3 camera,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(plate, state, partialTicks, camera, breakProgress);
        state.count = plate.food().getCount();
        items.updateForTopItem(state.item, plate.food(), ItemDisplayContext.FIXED, plate.getLevel(), null,
                (int) plate.getBlockPos().asLong());
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.item.isEmpty()) return;
        for (int i = 0; i < state.count; i++) {
            pose.pushPose();
            pose.translate(0.5F, 1.6F / 16F + i * 0.03F, 0.5F);
            pose.rotateDegrees(Axis.YP, i * 37.0F);
            pose.translate(i == 0 ? 0 : 0.06F, 0, 0);
            pose.rotateDegrees(Axis.XP, 90.0F);
            pose.scale(0.42F, 0.42F, 0.42F);
            state.item.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
    }
}
