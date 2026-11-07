package net.kasax.raft.world.gen;

import net.kasax.raft.Raft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/** Placed features of the mod (JSON in data/raft/worldgen/placed_feature). */
public class ModPlacedFeatures {
    public static final ResourceKey<PlacedFeature> TITANIUM_ORE_PLACED_KEY = registerKey("titanium_ore_placed");
    public static final ResourceKey<PlacedFeature> PALM_PLACED_KEY = registerKey("palm_placed");
    /** Rare driftwood trees on overworld beaches, so the portal frame can be built before the first trip. */
    public static final ResourceKey<PlacedFeature> DRIFTWOOD_BEACH_PLACED_KEY = registerKey("driftwood_beach_placed");

    public static ResourceKey<PlacedFeature> registerKey(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, Raft.id(name));
    }
}
