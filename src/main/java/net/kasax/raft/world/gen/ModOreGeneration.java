package net.kasax.raft.world.gen;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.kasax.raft.world.biome.ModBiomes;
import net.minecraft.world.gen.GenerationStep;

public class ModOreGeneration {
    public static void generateOres() {
        BiomeModifications.addFeature(biomeSelectionContext -> biomeSelectionContext.getBiomeKey() == ModBiomes.RAFT_OCEAN_BIOME,
                GenerationStep.Feature.UNDERGROUND_ORES, ModPlacedFeatures.TITANIUM_ORE_PLACED_KEY);
    }
}
