package net.kasax.raft.content.cozy.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.kasax.raft.content.cozy.block.PlateBlockEntity;
import net.kasax.raft.compat.render.SubmitNodeCollector;
import net.kasax.raft.compat.render.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.kasax.raft.compat.render.BlockEntityRenderState;
import net.kasax.raft.compat.render.ItemModelResolver;
import net.kasax.raft.compat.render.ItemStackRenderState;
import net.kasax.raft.compat.render.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Draws the food on a serving plate: up to four portions lying flat, slightly fanned out. */
public class PlateRenderer implements BlockEntityRenderer<PlateBlockEntity, PlateRenderer.State> {
    private final ItemModelResolver items;

    public PlateRenderer(BlockEntityRendererProvider.Context context) {
        this.items = new ItemModelResolver();
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
                                   @Nullable Object breakProgress) {
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
            pose.mulPose(Axis.YP.rotationDegrees(i * 37.0F));
            pose.translate(i == 0 ? 0 : 0.06F, 0, 0);
            pose.mulPose(Axis.XP.rotationDegrees(90.0F));
            pose.scale(0.42F, 0.42F, 0.42F);
            state.item.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
    }
}
