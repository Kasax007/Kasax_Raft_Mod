package net.kasax.raft.block.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.kasax.raft.block.entity.SimpleGrillBlockEntity;
import net.kasax.raft.compat.render.SubmitNodeCollector;
import net.kasax.raft.compat.render.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.kasax.raft.compat.render.BlockEntityRenderState;
import net.kasax.raft.compat.render.ItemModelResolver;
import net.kasax.raft.compat.render.ItemStackRenderState;
import net.kasax.raft.compat.render.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Renders the food lying on the grill. */
public class SimpleGrillBlockEntityRenderer implements BlockEntityRenderer<SimpleGrillBlockEntity, SimpleGrillBlockEntityRenderer.State> {
    private final ItemModelResolver itemModelResolver;

    public SimpleGrillBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = new ItemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SimpleGrillBlockEntity grill, State state, float partialTicks, Vec3 cameraPosition,
                                   @Nullable Object breakProgress) {
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
            poseStack.mulPose(Axis.YP.rotationDegrees(-state.facing.toYRot()));
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));
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
