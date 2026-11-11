package net.kasax.raft.world.gen;

import net.kasax.raft.platform.BiomeModifications;
import net.kasax.raft.platform.BiomeSelectors;
import net.kasax.raft.world.biome.ModBiomes;
import net.minecraft.world.level.levelgen.GenerationStep;

public class ModWorldGeneration {
    public static void generateModWorldGen() {
        // Titanium ore in the raft ocean
        BiomeModifications.addFeature(BiomeSelectors.includeByKey(ModBiomes.RAFT_OCEAN_BIOME),
                GenerationStep.Decoration.UNDERGROUND_ORES, ModPlacedFeatures.TITANIUM_ORE_PLACED_KEY);
        // Palm trees
        BiomeModifications.addFeature(BiomeSelectors.includeByKey(ModBiomes.RAFT_OCEAN_BIOME),
                GenerationStep.Decoration.VEGETAL_DECORATION, ModPlacedFeatures.PALM_PLACED_KEY);
        // Driftwood trees wash up on overworld beaches (needed for the portal frame)
        BiomeModifications.addFeature(BiomeSelectors.tag(net.minecraft.tags.BiomeTags.IS_BEACH),
                GenerationStep.Decoration.VEGETAL_DECORATION, ModPlacedFeatures.DRIFTWOOD_BEACH_PLACED_KEY);
    }
}
