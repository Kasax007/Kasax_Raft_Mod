package net.kasax.raft.content.marine.client.render;

import net.kasax.raft.Raft;
import net.kasax.raft.content.marine.client.model.JellyfishModel;
import net.kasax.raft.content.marine.client.model.JellyfishRenderState;
import net.kasax.raft.content.marine.entity.JellyfishEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Renders the translucent jellyfish in its variant colour; the glow jellyfish is drawn full-bright. */
public class JellyfishRenderer extends MobRenderer<JellyfishEntity, JellyfishRenderState, JellyfishModel> {
    private static final Identifier[] TEXTURES = {
            Raft.id("textures/entity/jellyfish/moon.png"),
            Raft.id("textures/entity/jellyfish/crimson.png"),
            Raft.id("textures/entity/jellyfish/glow.png")};

    public JellyfishRenderer(EntityRendererProvider.Context context) {
        super(context, new JellyfishModel(context.bakeLayer(JellyfishModel.LAYER)), 0.3F);
        this.shadowStrength = 0.4F;
    }

    @Override
    public Identifier getTextureLocation(JellyfishRenderState state) {
        return TEXTURES[state.variant.ordinal()];
    }

    @Override
    public JellyfishRenderState createRenderState() {
        return new JellyfishRenderState();
    }

    @Override
    public void extractRenderState(JellyfishEntity entity, JellyfishRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.variant = entity.getVariant();
        float a = entity.clientPulseAge + partialTicks;
        float c = a < 6.0F ? a / 6.0F : 1.0F - (a - 6.0F) / 26.0F;
        c = Mth.clamp(c, 0.0F, 1.0F);
        state.contraction = c * c * (3.0F - 2.0F * c);
        Vec3 v = entity.getDeltaMovement();
        float b = state.bodyRot * Mth.DEG_TO_RAD;
        double mx = v.x * Mth.cos(b) + v.z * Mth.sin(b);
        double mz = v.x * Mth.sin(b) - v.z * Mth.cos(b);
        state.dragX = (float) Mth.clamp(mx * 8.0, -0.6, 0.6);
        state.dragZ = (float) Mth.clamp(mz * 8.0, -0.6, 0.6);
        state.rise = (float) Mth.clamp(v.y * 10.0, -1.0, 1.0);
        state.phase = (entity.getId() * 53) % 300;
    }

    @Override
    protected int getBlockLightLevel(JellyfishEntity entity, BlockPos pos) {
        return entity.getVariant() == JellyfishEntity.Variant.GLOW ? 15 : super.getBlockLightLevel(entity, pos);
    }
}
