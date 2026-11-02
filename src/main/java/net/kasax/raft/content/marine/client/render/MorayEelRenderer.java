package net.kasax.raft.content.marine.client.render;

import net.kasax.raft.Raft;
import net.kasax.raft.content.marine.client.model.MorayEelModel;
import net.kasax.raft.content.marine.client.model.MorayEelRenderState;
import net.kasax.raft.content.marine.entity.MorayEelEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Renders the moray eel. */
public class MorayEelRenderer extends MobRenderer<MorayEelEntity, MorayEelRenderState, MorayEelModel> {
    private static final Identifier TEXTURE = Raft.id("textures/entity/moray_eel.png");

    public MorayEelRenderer(EntityRendererProvider.Context context) {
        super(context, new MorayEelModel(context.bakeLayer(MorayEelModel.LAYER)), 0.4F);
    }

    @Override
    public Identifier getTextureLocation(MorayEelRenderState state) {
        return TEXTURE;
    }

    @Override
    public MorayEelRenderState createRenderState() {
        return new MorayEelRenderState();
    }

    @Override
    public void extractRenderState(MorayEelEntity entity, MorayEelRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        float l = Math.max(0.0F, entity.lungeAnim - partialTicks) / 14.0F;
        state.lunge = Mth.sin(l * Mth.PI);
        state.hiding = entity.getDeltaMovement().lengthSqr() < 4.0E-4;
        state.phase = (entity.getId() * 29) % 200;
    }
}
