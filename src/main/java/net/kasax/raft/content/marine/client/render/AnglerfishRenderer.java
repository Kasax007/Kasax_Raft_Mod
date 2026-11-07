package net.kasax.raft.content.marine.client.render;

import net.kasax.raft.Raft;
import net.kasax.raft.content.marine.client.model.AnglerfishModel;
import net.kasax.raft.content.marine.client.model.AnglerfishRenderState;
import net.kasax.raft.content.marine.entity.AnglerfishEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.kasax.raft.compat.render.MobRenderer;
import net.kasax.raft.compat.render.EyesLayer;
import net.minecraft.client.renderer.RenderType;
import net.kasax.raft.compat.render.RenderTypes;
import net.minecraft.resources.ResourceLocation;

/** Renders the anglerfish; its lure bulb (and a few photophores) glow full-bright through an emissive layer. */
public class AnglerfishRenderer extends MobRenderer<AnglerfishEntity, AnglerfishRenderState, AnglerfishModel> {
    private static final ResourceLocation TEXTURE = Raft.id("textures/entity/anglerfish.png");
    private static final RenderType GLOW = RenderTypes.eyes(Raft.id("textures/entity/anglerfish_glow.png"));

    public AnglerfishRenderer(EntityRendererProvider.Context context) {
        super(context, new AnglerfishModel(context.bakeLayer(AnglerfishModel.LAYER)), 0.5F);
        this.addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return GLOW;
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(AnglerfishRenderState state) {
        return TEXTURE;
    }

    @Override
    public AnglerfishRenderState createRenderState() {
        return new AnglerfishRenderState();
    }

    @Override
    public void extractRenderState(AnglerfishEntity entity, AnglerfishRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.bite = entity.getBiteAmount(partialTicks);
    }
}
