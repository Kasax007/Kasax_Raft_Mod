package net.kasax.raft.block.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.kasax.raft.block.entity.ItemCollectorBlockEntity;
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

/** Shows the last caught item floating in the item catcher. */
public class ItemCatcherBlockEntityRenderer implements BlockEntityRenderer<ItemCollectorBlockEntity, ItemCatcherBlockEntityRenderer.State> {
    private final ItemModelResolver itemModelResolver;

    public ItemCatcherBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = new ItemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ItemCollectorBlockEntity entity, State state, float partialTicks, Vec3 cameraPosition,
                                   @Nullable Object breakProgress) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTicks, cameraPosition, breakProgress);
        state.item.clear();
        this.itemModelResolver.updateForTopItem(state.item, entity.getRenderStack(), ItemDisplayContext.GUI,
                entity.getLevel(), null, (int) entity.getBlockPos().asLong());
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.item.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5f, 0.75f, 0.5f);
        poseStack.scale(0.35f, 0.35f, 0.35f);
        poseStack.mulPose(Axis.XP.rotationDegrees(270));
        state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    public static class State extends BlockEntityRenderState {
        public final ItemStackRenderState item = new ItemStackRenderState();
    }
}
