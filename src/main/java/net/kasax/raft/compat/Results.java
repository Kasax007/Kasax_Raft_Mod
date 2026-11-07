package net.kasax.raft.compat;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;

/** 1.21.1 port helper: wraps the plain interaction result of an item's use into the holder 1.21.1 expects. */
public final class Results {
    private Results() {
    }

    public static InteractionResultHolder<ItemStack> holder(InteractionResult result, ItemStack stack) {
        return new InteractionResultHolder<>(result, stack);
    }
}
