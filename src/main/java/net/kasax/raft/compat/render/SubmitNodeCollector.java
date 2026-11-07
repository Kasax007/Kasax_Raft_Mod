package net.kasax.raft.compat.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 1.21.1 port helper: the newer game queues draw calls ("submits") and draws them later; 1.21.1 draws at once into
 * a buffer source. This class offers the newer calls and draws them immediately.
 */
public final class SubmitNodeCollector {
    private final MultiBufferSource buffers;

    public SubmitNodeCollector(MultiBufferSource buffers) {
        this.buffers = buffers;
    }

    public MultiBufferSource buffers() {
        return this.buffers;
    }

    public SubmitNodeCollector order(int order) {
        return this;
    }

    public <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType, int light, int overlay,
                                int outlineColor) {
        submitModel(model, state, poseStack, renderType, light, overlay, -1, null, outlineColor);
    }

    public <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, ResourceLocation texture, int light, int overlay,
                                int outlineColor) {
        submitModel(model, state, poseStack, model.renderType(texture), light, overlay, -1, null, outlineColor);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType, int light, int overlay,
                                int tintColor, @Nullable Object sprite, int outlineColor) {
        ((Model) model).setupAnim(state);
        model.renderToBuffer(poseStack, this.buffers.getBuffer(renderType), light, overlay, tintColor);
    }

    public void submitCustomGeometry(PoseStack poseStack, RenderType renderType, CustomGeometryRenderer renderer) {
        renderer.render(poseStack.last(), this.buffers.getBuffer(renderType));
    }

    public void submitShapeOutline(PoseStack poseStack, VoxelShape shape, RenderType renderType, int color, float width, boolean unused) {
        float a = (color >>> 24 & 255) / 255.0F, r = (color >> 16 & 255) / 255.0F, g = (color >> 8 & 255) / 255.0F, b = (color & 255) / 255.0F;
        LevelRenderer.renderVoxelShape(poseStack, this.buffers.getBuffer(renderType), shape, 0.0, 0.0, 0.0, r, g, b, a, true);
    }

    @FunctionalInterface
    public interface CustomGeometryRenderer {
        void render(PoseStack.Pose pose, VertexConsumer buffer);
    }
}
