package net.kasax.raft.platform.transfer;

import net.minecraft.world.item.ItemStack;

/** Fabric's ItemVariant: an item with its components, without a count. */
public final class ItemVariant {
    private final ItemStack stack;

    private ItemVariant(ItemStack stack) {
        this.stack = stack.copyWithCount(1);
    }

    public static ItemVariant of(ItemStack stack) {
        return new ItemVariant(stack);
    }

    public boolean isBlank() {
        return stack.isEmpty();
    }

    public ItemStack toStack(int count) {
        return stack.copyWithCount(count);
    }
}
