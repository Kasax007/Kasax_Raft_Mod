package net.kasax.raft.platform;

import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

/** Fabric's BiomeSelectors used by Beach Craft. */
public final class BiomeSelectors {
    private BiomeSelectors() {
    }

    public static Predicate<BiomeSelectionContext> all() {
        return context -> true;
    }

    @SafeVarargs
    public static Predicate<BiomeSelectionContext> includeByKey(ResourceKey<Biome>... keys) {
        return includeByKey(Set.of(keys));
    }

    public static Predicate<BiomeSelectionContext> includeByKey(Collection<ResourceKey<Biome>> keys) {
        Set<ResourceKey<Biome>> set = Set.copyOf(keys);
        return context -> set.contains(context.getBiomeKey());
    }

    public static Predicate<BiomeSelectionContext> tag(TagKey<Biome> tag) {
        return context -> context.hasTag(tag);
    }
}
