package net.kasax.raft.content.gear.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * What is loaded into a Driftwood Fishing Rod (data component {@code raft:tackle}): a stack of Fish Bait and a stack
 * of Golden Lures. Immutable; equality compares the stacks by item, count and components.
 */
public record Tackle(ItemStack bait, ItemStack lure) {
    public static final Tackle EMPTY = new Tackle(ItemStack.EMPTY, ItemStack.EMPTY);
    public static final Codec<Tackle> CODEC = RecordCodecBuilder.create(i -> i.group(
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("bait", ItemStack.EMPTY).forGetter(Tackle::bait),
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("lure", ItemStack.EMPTY).forGetter(Tackle::lure)
    ).apply(i, Tackle::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, Tackle> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC, Tackle::bait, ItemStack.OPTIONAL_STREAM_CODEC, Tackle::lure, Tackle::new);

    public Tackle {
        bait = bait.copy();
        lure = lure.copy();
    }

    public boolean isEmpty() {
        return bait.isEmpty() && lure.isEmpty();
    }

    public ItemStack get(int slot) {
        return (slot == 0 ? bait : lure).copy();
    }

    public Tackle with(int slot, ItemStack stack) {
        return slot == 0 ? new Tackle(stack, lure) : new Tackle(bait, stack);
    }

    /** One bait and one lure used up by a cast. */
    public Tackle afterCast() {
        return new Tackle(bait.copyWithCount(Math.max(0, bait.getCount() - 1)), lure.copyWithCount(Math.max(0, lure.getCount() - 1)));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Tackle t && ItemStack.matches(bait, t.bait) && ItemStack.matches(lure, t.lure);
    }

    @Override
    public int hashCode() {
        return 31 * ItemStack.hashItemAndComponents(bait) + bait.getCount() * 7 + ItemStack.hashItemAndComponents(lure) * 13 + lure.getCount();
    }
}
