package net.kasax.raft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.kasax.raft.Raft;
import net.kasax.raft.entity.SharkEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.kasax.raft.compat.render.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Renderer of the shark "Bruce"; the hammerhead uses it too with its own layer, texture and size. */
public class SharkRenderer extends MobRenderer<SharkEntity, SharkRenderState, SharkModel> {
    private static final ResourceLocation TEXTURE = Raft.id("textures/entity/shark.png");

    private final ResourceLocation texture;
    private final float modelScale;

    public SharkRenderer(EntityRendererProvider.Context context) {
        this(context, SharkModel.LAYER, TEXTURE, 1.0F, 0.8F);
    }

    public SharkRenderer(EntityRendererProvider.Context context, ModelLayerLocation layer, ResourceLocation texture, float modelScale,
                         float shadow) {
        super(context, new SharkModel(context.bakeLayer(layer)), shadow);
        this.texture = texture;
        this.modelScale = modelScale;
    }

    @Override
    public ResourceLocation getTextureLocation(SharkRenderState state) {
        return this.texture;
    }

    @Override
    public SharkRenderState createRenderState() {
        return new SharkRenderState();
    }

    @Override
    public void extractRenderState(SharkEntity entity, SharkRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.bite = entity.getBiteAmount(partialTicks);
    }

    @Override
    protected void scale(SharkRenderState state, PoseStack poseStack) {
        if (this.modelScale != 1.0F) {
            poseStack.scale(this.modelScale, this.modelScale, this.modelScale);
        }
    }
}
