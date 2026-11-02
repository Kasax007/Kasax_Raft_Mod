package net.kasax.raft.content.marine.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.kasax.raft.Raft;
import net.kasax.raft.content.marine.client.model.WhaleModel;
import net.kasax.raft.content.marine.client.model.WhaleRenderState;
import net.kasax.raft.content.marine.entity.WhaleEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

/** Renders the whale at twice the model size; the body reaches beyond the hitbox, so culling uses a bigger box. */
public class WhaleRenderer extends MobRenderer<WhaleEntity, WhaleRenderState, WhaleModel> {
    private static final Identifier TEXTURE = Raft.id("textures/entity/whale.png");

    public WhaleRenderer(EntityRendererProvider.Context context) {
        super(context, new WhaleModel(context.bakeLayer(WhaleModel.LAYER)), 3.0F);
    }

    @Override
    public Identifier getTextureLocation(WhaleRenderState state) {
        return TEXTURE;
    }

    @Override
    public WhaleRenderState createRenderState() {
        return new WhaleRenderState();
    }

    @Override
    public void extractRenderState(WhaleEntity entity, WhaleRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        float spout = Math.max(0.0F, entity.spoutAnim - partialTicks) / 30.0F;
        state.spout = Mth.sin(spout * Mth.PI);
        float song = Math.max(0.0F, entity.singAnim - partialTicks) / 90.0F;
        state.song = Mth.clamp(Mth.sin(song * Mth.PI) * 1.6F, 0.0F, 1.0F);
    }

    @Override
    protected void scale(WhaleRenderState state, PoseStack poseStack) {
        poseStack.scale(2.0F, 2.0F, 2.0F);
    }

    @Override
    protected AABB getBoundingBoxForCulling(WhaleEntity entity, float partialTicks) {
        return super.getBoundingBoxForCulling(entity, partialTicks).inflate(2.5, 0.5, 2.5);
    }
}
