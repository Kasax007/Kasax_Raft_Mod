package net.kasax.raft.world.gen;

import net.kasax.raft.Raft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

/**
 * The configured features of the mod: titanium ore and the two trees. Their JSON lives in
 * data/raft/worldgen/configured_feature (generated earlier; the Java datagen of the newer game was not ported).
 */
public class ModConfiguredFeatures {
    public static final ResourceKey<ConfiguredFeature<?, ?>> TITANIUM_ORE_KEY = registerKey("titanium_ore");

    public static final ResourceKey<ConfiguredFeature<?, ?>> DRIFTWOOD_KEY = registerKey("driftwood");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PALMWOOD_KEY = registerKey("palmwood");

    public static ResourceKey<ConfiguredFeature<?, ?>> registerKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, Raft.id(name));
    }
}
