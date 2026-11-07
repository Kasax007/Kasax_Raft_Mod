package net.kasax.raft.compat.render;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** 1.21.1 port helper: fills an {@link ItemStackRenderState} (the stack is drawn directly later). */
public class ItemModelResolver {
    public void updateForLiving(ItemStackRenderState state, ItemStack stack, ItemDisplayContext context, LivingEntity entity) {
        state.stack = stack.copy();
        state.displayContext = context;
        state.owner = entity;
        state.level = entity.level();
        state.seed = entity.getId();
    }

    public void updateForTopItem(ItemStackRenderState state, ItemStack stack, ItemDisplayContext context, @Nullable Level level,
                                 @Nullable Object owner, int seed) {
        state.stack = stack.copy();
        state.displayContext = context;
        state.owner = owner instanceof LivingEntity l ? l : null;
        state.level = level;
        state.seed = seed;
    }
}
