package net.kasax.raft.platform.transfer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;

/** Fabric's ItemVariant: an item with its components, without a count (NeoForge's {@link ItemResource}). */
public final class ItemVariant {
    private final ItemResource resource;

    private ItemVariant(ItemResource resource) {
        this.resource = resource;
    }

    public static ItemVariant of(ItemStack stack) {
        return new ItemVariant(ItemResource.of(stack));
    }

    public boolean isBlank() {
        return resource.isEmpty();
    }

    public ItemResource resource() {
        return resource;
    }

    public ItemStack toStack(int count) {
        return resource.toStack(count);
    }
}
