package net.kasax.raft.compat.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** 1.21.1 port helper: a block entity renderer in the newer game's shape (create, extract, submit). */
public interface BlockEntityRenderer<T extends BlockEntity, S extends BlockEntityRenderState>
        extends net.minecraft.client.renderer.blockentity.BlockEntityRenderer<T> {
    S createRenderState();

    default void extractRenderState(T blockEntity, S state, float partialTicks, Vec3 cameraPosition, @Nullable Object breakProgress) {
        state.blockPos = blockEntity.getBlockPos();
        state.blockState = blockEntity.getBlockState();
        state.partialTick = partialTicks;
    }

    void submit(S state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera);

    @Override
    default void render(T blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        S state = this.createRenderState();
        this.extractRenderState(blockEntity, state, partialTick, Vec3.ZERO, null);
        state.lightCoords = light;
        this.submit(state, poseStack, new SubmitNodeCollector(buffers), EntityRenderer.CAMERA);
    }
}
