package net.kasax.raft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.kasax.raft.Raft;
import net.kasax.raft.entity.SharkEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class SharkRenderer extends MobRenderer<SharkEntity, LivingEntityRenderState, SharkModel> {
    private static final Identifier TEXTURE = Raft.id("textures/entity/shark.png");

    public SharkRenderer(EntityRendererProvider.Context context) {
        super(context, new SharkModel(context.bakeLayer(SharkModel.LAYER)), 0.8F);
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    @Override
    protected void scale(LivingEntityRenderState state, PoseStack poseStack) {
        poseStack.scale(1.5F, 1.5F, 1.5F);
    }
}
