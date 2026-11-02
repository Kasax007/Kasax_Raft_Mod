package net.kasax.raft.content.shore.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.kasax.raft.Raft;
import net.kasax.raft.content.shore.client.model.CrabModel;
import net.kasax.raft.content.shore.entity.CrabAnimated;
import net.kasax.raft.content.shore.entity.CrabEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

/** Beach crab in three shell colours; the babies are half size. */
public class CrabRenderer extends MobRenderer<CrabEntity, CrabRenderState, CrabModel> {
    private static final Identifier[] TEXTURES = {
            Raft.id("textures/entity/crab/crab_red.png"),
            Raft.id("textures/entity/crab/crab_blue.png"),
            Raft.id("textures/entity/crab/crab_sand.png")};

    public CrabRenderer(EntityRendererProvider.Context context) {
        super(context, new CrabModel(context.bakeLayer(CrabModel.LAYER)), 0.35F);
    }

    @Override
    public CrabRenderState createRenderState() {
        return new CrabRenderState();
    }

    @Override
    public void extractRenderState(CrabEntity entity, CrabRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        extractCrab(entity, entity, state, partialTicks);
        state.variant = entity.getVariant().ordinal();
    }

    static void extractCrab(Entity entity, CrabAnimated crab, CrabRenderState state, float partialTicks) {
        state.burrow = crab.getBurrowAmount(partialTicks);
        state.sideways = crab.getSidewaysAmount(partialTicks);
        state.threatening = crab.isThreatening();
        state.pinch.copyFrom(crab.pinchAnimation());
        state.snap.copyFrom(crab.snapAnimation());
        state.seed = (entity.getId() * 0.6180339F) % 6.2831855F;
    }

    @Override
    public Identifier getTextureLocation(CrabRenderState state) {
        return TEXTURES[Math.floorMod(state.variant, TEXTURES.length)];
    }

    @Override
    protected void scale(CrabRenderState state, PoseStack poseStack) {
        if (state.isBaby) {
            poseStack.scale(0.5F, 0.5F, 0.5F);
        }
    }

    @Override
    protected float getShadowRadius(CrabRenderState state) {
        return super.getShadowRadius(state) * (1.0F - state.burrow * 0.7F) * (state.isBaby ? 0.5F : 1.0F);
    }
}
