package net.kasax.raft.content.shore.client.render;

import net.kasax.raft.Raft;
import net.kasax.raft.content.shore.client.model.SeaOtterModel;
import net.kasax.raft.content.shore.entity.SeaOtterEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.kasax.raft.compat.render.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Sea otter. */
public class SeaOtterRenderer extends MobRenderer<SeaOtterEntity, SeaOtterRenderState, SeaOtterModel> {
    private static final ResourceLocation TEXTURE = Raft.id("textures/entity/sea_otter.png");

    public SeaOtterRenderer(EntityRendererProvider.Context context) {
        super(context, new SeaOtterModel(context.bakeLayer(SeaOtterModel.LAYER)), 0.4F);
    }

    @Override
    public SeaOtterRenderState createRenderState() {
        return new SeaOtterRenderState();
    }

    @Override
    public void extractRenderState(SeaOtterEntity entity, SeaOtterRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.back = entity.getBackAmount(partialTicks);
        state.swim = entity.getSwimAmount(partialTicks);
        Vec3 motion = entity.getDeltaMovement();
        double horizontal = motion.horizontalDistance();
        state.swimPitch = horizontal + Math.abs(motion.y) < 0.01 ? 0.0F
                : (float) Mth.clamp(-Math.atan2(motion.y, Math.max(horizontal, 0.02)), -1.0, 1.0);
        state.crack.copyFrom(entity.crackAnimation);
        state.carrying = false;
        state.seed = (entity.getId() * 0.6180339F) % 6.2831855F;
    }

    @Override
    public ResourceLocation getTextureLocation(SeaOtterRenderState state) {
        return TEXTURE;
    }
}
