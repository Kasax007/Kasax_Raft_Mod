package net.kasax.raft.content.shore.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.kasax.raft.Raft;
import net.kasax.raft.content.shore.client.model.PenguinModel;
import net.kasax.raft.content.shore.entity.PenguinEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Penguin; the chick is a small grey fluffball with a big head. */
public class PenguinRenderer extends MobRenderer<PenguinEntity, PenguinRenderState, PenguinModel> {
    private static final Identifier TEXTURE = Raft.id("textures/entity/penguin/penguin.png");
    private static final Identifier CHICK = Raft.id("textures/entity/penguin/penguin_chick.png");

    public PenguinRenderer(EntityRendererProvider.Context context) {
        super(context, new PenguinModel(context.bakeLayer(PenguinModel.LAYER)), 0.35F);
    }

    @Override
    public PenguinRenderState createRenderState() {
        return new PenguinRenderState();
    }

    @Override
    public void extractRenderState(PenguinEntity entity, PenguinRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.slide = entity.getSlideAmount(partialTicks);
        state.swim = entity.getSwimAmount(partialTicks);
        Vec3 motion = entity.getDeltaMovement();
        double horizontal = motion.horizontalDistance();
        state.swimPitch = horizontal + Math.abs(motion.y) < 0.01 ? 0.0F
                : (float) Mth.clamp(-Math.atan2(motion.y, Math.max(horizontal, 0.02)), -0.9, 0.9);
        state.seed = (entity.getId() * 0.6180339F) % 6.2831855F;
    }

    @Override
    public Identifier getTextureLocation(PenguinRenderState state) {
        return state.isBaby ? CHICK : TEXTURE;
    }

    @Override
    protected void scale(PenguinRenderState state, PoseStack poseStack) {
        float s = state.isBaby ? 0.5F : 0.9F;
        poseStack.scale(s, s, s);
    }
}
