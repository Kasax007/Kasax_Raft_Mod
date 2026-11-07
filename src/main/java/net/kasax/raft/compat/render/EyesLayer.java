package net.kasax.raft.compat.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** 1.21.1 port helper: a full-bright glow layer drawn with the parent's model. */
public abstract class EyesLayer<S extends EntityRenderState, M extends Model<?>> extends RenderLayer<S, M> {
    public EyesLayer(RenderLayerParent<S, M> parent) {
        super(parent);
    }

    public abstract RenderType renderType();

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, S state, float yRot, float xRot) {
        this.getParentModel().renderToBuffer(poseStack, collector.buffers().getBuffer(this.renderType()), 15728640, OverlayTexture.NO_OVERLAY, -1);
    }
}
