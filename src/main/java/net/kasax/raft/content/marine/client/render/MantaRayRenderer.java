package net.kasax.raft.content.marine.client.render;

import net.kasax.raft.Raft;
import net.kasax.raft.content.marine.client.model.MantaRayModel;
import net.kasax.raft.content.marine.client.model.MantaRayRenderState;
import net.kasax.raft.content.marine.entity.MantaRayEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.kasax.raft.compat.render.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

/** Renders the manta ray; it banks into its turns. */
public class MantaRayRenderer extends MobRenderer<MantaRayEntity, MantaRayRenderState, MantaRayModel> {
    private static final ResourceLocation TEXTURE = Raft.id("textures/entity/manta_ray.png");

    public MantaRayRenderer(EntityRendererProvider.Context context) {
        super(context, new MantaRayModel(context.bakeLayer(MantaRayModel.LAYER)), 1.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(MantaRayRenderState state) {
        return TEXTURE;
    }

    @Override
    public MantaRayRenderState createRenderState() {
        return new MantaRayRenderState();
    }

    @Override
    public void extractRenderState(MantaRayEntity entity, MantaRayRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        float turn = Mth.wrapDegrees(entity.yBodyRot - entity.yBodyRotO);
        state.bank = Mth.clamp(-turn * 0.12F, -0.45F, 0.45F);
        state.phase = (entity.getId() * 37) % 200;
    }

    @Override
    protected AABB getBoundingBoxForCulling(MantaRayEntity entity, float partialTicks) {
        return super.getBoundingBoxForCulling(entity, partialTicks).inflate(0.6, 0.6, 0.6);
    }
}
