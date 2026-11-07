package net.kasax.raft.compat.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.kasax.raft.compat.ARGB;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

/** 1.21.1 port helper: a glowing texture drawn over the model at full brightness, with a pulsing strength. */
public class LivingEntityEmissiveLayer<S extends LivingEntityRenderState, M extends EntityModel<? super S>> extends RenderLayer<S, M> {
    public interface AlphaFunction<S> {
        float apply(S state, float ageInTicks);
    }

    private final Function<S, ResourceLocation> texture;
    private final AlphaFunction<S> alpha;
    private final M model;
    private final Function<ResourceLocation, RenderType> renderType;

    public LivingEntityEmissiveLayer(RenderLayerParent<S, M> parent, Function<S, ResourceLocation> texture, AlphaFunction<S> alpha,
                                     M model, Function<ResourceLocation, RenderType> renderType, boolean alwaysVisible) {
        super(parent);
        this.texture = texture;
        this.alpha = alpha;
        this.model = model;
        this.renderType = renderType;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, S state, float yRot, float xRot) {
        if (state.isInvisible) return;
        float a = Math.max(0.0F, Math.min(1.0F, this.alpha.apply(state, state.ageInTicks)));
        this.model.renderToBuffer(poseStack, collector.buffers().getBuffer(this.renderType.apply(this.texture.apply(state))), 15728640,
                OverlayTexture.NO_OVERLAY, ARGB.colorFromFloat(1.0F, a, a, a));
    }
}
