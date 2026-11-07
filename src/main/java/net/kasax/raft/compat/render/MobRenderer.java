package net.kasax.raft.compat.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Mob;

/** 1.21.1 port helper: a mob renderer in the newer game's shape (see {@link LivingEntityRenderer}). */
public abstract class MobRenderer<T extends Mob, S extends LivingEntityRenderState, M extends EntityModel<? super S>>
        extends LivingEntityRenderer<T, S, M> {
    public MobRenderer(EntityRendererProvider.Context context, M model, float shadow) {
        super(context, model, shadow);
    }

    @Override
    protected boolean shouldShowName(T mob, double distanceToCameraSq) {
        return super.shouldShowName(mob, distanceToCameraSq) && (mob.shouldShowName() || mob.hasCustomName() && mob == this.entityRenderDispatcher.crosshairPickEntity);
    }
}
