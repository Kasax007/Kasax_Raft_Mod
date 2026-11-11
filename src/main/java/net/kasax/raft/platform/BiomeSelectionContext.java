package net.kasax.raft.platform;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

/** Fabric's BiomeSelectionContext: the biome a modification is asked about. */
public final class BiomeSelectionContext {
    private final Holder<Biome> holder;
    private final ResourceKey<Biome> key;

    BiomeSelectionContext(Holder<Biome> holder) {
        this.holder = holder;
        this.key = holder.unwrapKey().orElseThrow();
    }

    public ResourceKey<Biome> getBiomeKey() {
        return key;
    }

    public Biome getBiome() {
        return holder.value();
    }

    public Holder<Biome> getBiomeRegistryEntry() {
        return holder;
    }

    public boolean hasTag(TagKey<Biome> tag) {
        return holder.is(tag);
    }
}
