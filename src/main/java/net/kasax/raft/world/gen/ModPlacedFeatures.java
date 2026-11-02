package net.kasax.raft.world.gen;

import net.kasax.raft.Raft;
import net.kasax.raft.block.ModBlocks;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;

import java.util.List;

public class ModPlacedFeatures {

    public static final ResourceKey<PlacedFeature> TITANIUM_ORE_PLACED_KEY = registerKey("titanium_ore_placed");
    public static final ResourceKey<PlacedFeature> PALM_PLACED_KEY = registerKey("palm_placed");
    /** Rare driftwood trees on overworld beaches, so the portal frame can be built before the first trip. */
    public static final ResourceKey<PlacedFeature> DRIFTWOOD_BEACH_PLACED_KEY = registerKey("driftwood_beach_placed");

    public static void bootstrap(BootstrapContext<PlacedFeature> context) {
        HolderGetter<Feature> features = context.lookup(Registries.FEATURE);

        register(context, TITANIUM_ORE_PLACED_KEY, features.getOrThrow(ModConfiguredFeatures.TITANIUM_ORE_KEY),
                ModOrePlacement.modifiersWithCount(3, //veins per chunk
                        HeightRangePlacement.triangle(VerticalAnchor.absolute(-80), VerticalAnchor.absolute(20))));

        register(context, PALM_PLACED_KEY, features.getOrThrow(ModConfiguredFeatures.PALMWOOD_KEY),
                VegetationPlacements.treePlacement(PlacementUtils.countExtra(1, 0.005f, 1), ModBlocks.PALM_SAPLING));

        register(context, DRIFTWOOD_BEACH_PLACED_KEY, features.getOrThrow(ModConfiguredFeatures.DRIFTWOOD_KEY),
                VegetationPlacements.treePlacement(net.minecraft.world.level.levelgen.placement.RarityFilter.onAverageOnceEvery(6), ModBlocks.DRIFTWOOD_SAPLING));
    }

    public static ResourceKey<PlacedFeature> registerKey(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, Raft.id(name));
    }

    private static void register(BootstrapContext<PlacedFeature> context, ResourceKey<PlacedFeature> key, Holder<Feature> configuration,
                                 List<PlacementModifier> modifiers) {
        context.register(key, new PlacedFeature(configuration, List.copyOf(modifiers)));
    }
}
