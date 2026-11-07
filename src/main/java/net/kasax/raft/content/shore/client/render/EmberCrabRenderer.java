package net.kasax.raft.content.shore.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.kasax.raft.Raft;
import net.kasax.raft.content.shore.client.model.CrabModel;
import net.kasax.raft.content.shore.entity.EmberCrabEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.kasax.raft.compat.render.MobRenderer;
import net.kasax.raft.compat.render.LivingEntityEmissiveLayer;
import net.kasax.raft.compat.render.RenderTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Ember crab: basalt shell with spikes, and glowing ember cracks that pulse slowly (emissive layer). */
public class EmberCrabRenderer extends MobRenderer<EmberCrabEntity, CrabRenderState, CrabModel> {
    private static final ResourceLocation TEXTURE = Raft.id("textures/entity/crab/ember_crab.png");
    private static final ResourceLocation GLOW = Raft.id("textures/entity/crab/ember_crab_glow.png");

    public EmberCrabRenderer(EntityRendererProvider.Context context) {
        super(context, new CrabModel(context.bakeLayer(CrabModel.EMBER_LAYER)), 0.5F);
        this.addLayer(new LivingEntityEmissiveLayer<>(this, s -> GLOW,
                (s, age) -> 0.75F + 0.25F * Mth.sin(age * 0.12F + s.seed),
                this.getModel(), RenderTypes::eyes, false));
    }

    @Override
    public CrabRenderState createRenderState() {
        return new CrabRenderState();
    }

    @Override
    public void extractRenderState(EmberCrabEntity entity, CrabRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        CrabRenderer.extractCrab(entity, entity, state, partialTicks);
        state.ember = true;
    }

    @Override
    public ResourceLocation getTextureLocation(CrabRenderState state) {
        return TEXTURE;
    }

    @Override
    protected void scale(CrabRenderState state, PoseStack poseStack) {
        poseStack.scale(1.3F, 1.3F, 1.3F);
    }
}
