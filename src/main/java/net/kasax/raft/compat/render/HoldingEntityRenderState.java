package net.kasax.raft.compat.render;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;

/** 1.21.1 port helper: render data of a living entity that shows the item in its main hand. */
public class HoldingEntityRenderState extends LivingEntityRenderState {
    public final ItemStackRenderState heldItem = new ItemStackRenderState();

    public static void extractHoldingEntityRenderState(LivingEntity entity, HoldingEntityRenderState state, ItemModelResolver resolver) {
        resolver.updateForLiving(state.heldItem, entity.getMainHandItem(), ItemDisplayContext.GROUND, entity);
    }
}
