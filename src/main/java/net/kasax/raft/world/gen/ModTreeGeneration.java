package net.kasax.raft.world.gen;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.kasax.raft.world.biome.ModBiomes;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.gen.GenerationStep;

public class ModTreeGeneration {
    public static void generateTrees() {
        BiomeModifications.addFeature(BiomeSelectors.includeByKey(ModBiomes.RAFT_OCEAN_BIOME),
                GenerationStep.Feature.VEGETAL_DECORATION, ModPlacedFeatures.PALM_PLACED_KEY);
    }
}
