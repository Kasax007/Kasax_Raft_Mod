package net.kasax.raft.block.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.kasax.raft.block.entity.SimpleGrillBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Renders the food lying on the grill. */
public class SimpleGrillBlockEntityRenderer implements BlockEntityRenderer<SimpleGrillBlockEntity, SimpleGrillBlockEntityRenderer.State> {
    private final ItemModelResolver itemModelResolver;

    public SimpleGrillBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SimpleGrillBlockEntity grill, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(grill, state, partialTicks, cameraPosition, breakProgress);
        state.facing = grill.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        int seed = (int) grill.getBlockPos().asLong();
        state.items = new ArrayList<>();
        for (int slot = 0; slot < grill.getItems().size(); slot++) {
            ItemStackRenderState itemState = new ItemStackRenderState();
            this.itemModelResolver.updateForTopItem(itemState, grill.getItems().get(slot), ItemDisplayContext.FIXED, grill.getLevel(), null, seed + slot);
            state.items.add(itemState);
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        for (int slot = 0; slot < state.items.size(); slot++) {
            ItemStackRenderState itemState = state.items.get(slot);
            if (itemState.isEmpty()) {
                continue;
            }
            poseStack.pushPose();
            poseStack.translate(0.5f, 0.65f, 0.5f);
            poseStack.rotateDegrees(Axis.YP, -state.facing.toYRot());
            poseStack.rotateDegrees(Axis.XP, 90.0f);
            poseStack.translate(slot == 0 ? -0.2f : 0.2f, 0.0f, 0.0f);
            poseStack.scale(0.4f, 0.4f, 0.4f);
            itemState.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
    }

    public static class State extends BlockEntityRenderState {
        public List<ItemStackRenderState> items = new ArrayList<>();
        public Direction facing = Direction.NORTH;
    }
}
