package net.kasax.raft.compat.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;

/**
 * 1.21.1 port helper: a render layer written against render states. 1.21.1 calls {@code render(...)} with the
 * entity; this passes the parent's current state on to {@link #submit}.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class RenderLayer<S extends EntityRenderState, M extends Model<?>> extends net.minecraft.client.renderer.entity.layers.RenderLayer {
    private final RenderLayerParent<S, M> parent;

    public RenderLayer(RenderLayerParent<S, M> parent) {
        super((net.minecraft.client.renderer.entity.RenderLayerParent) parent);
        this.parent = parent;
    }

    @Override
    public M getParentModel() {
        return this.parent.getModel();
    }

    public abstract void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, S state, float yRot, float xRot);

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int light, Entity entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        S state = this.parent.currentRenderState();
        if (state != null) this.submit(poseStack, new SubmitNodeCollector(buffers), light, state, netHeadYaw, headPitch);
    }
}
