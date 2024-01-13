package net.kasax.raft.world.biome;

import net.kasax.raft.Raft;
import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeEffects;
import net.minecraft.world.biome.GenerationSettings;
import net.minecraft.world.biome.SpawnSettings;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.DefaultBiomeFeatures;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureKeys;

public class ModBiomes {
    public static final RegistryKey<Biome> RAFT_OCEAN_BIOME = RegistryKey.of(RegistryKeys.BIOME,
            new Identifier(Raft.MOD_ID, "raft_ocean_biome"));

    public static void bootstrap(Registerable<Biome> context) {
        context.register(RAFT_OCEAN_BIOME, raftOceanBiome(context));
    }

    public static void globalOverworldGeneration(GenerationSettings.LookupBackedBuilder builder) {
        DefaultBiomeFeatures.addLandCarvers(builder);
        DefaultBiomeFeatures.addAmethystGeodes(builder);
        DefaultBiomeFeatures.addDungeons(builder);
        DefaultBiomeFeatures.addMineables(builder);
    }

    public static Biome raftOceanBiome(Registerable<Biome> context) {
        SpawnSettings.Builder spawnBuilder = new SpawnSettings.Builder();

        DefaultBiomeFeatures.addFarmAnimals(spawnBuilder);
        DefaultBiomeFeatures.addBatsAndMonsters(spawnBuilder);
        DefaultBiomeFeatures.addOceanMobs(spawnBuilder, 1, 3, 1);
        DefaultBiomeFeatures.addWarmOceanMobs(spawnBuilder, 1, 3);
        DefaultBiomeFeatures.addCaveMobs(spawnBuilder);


        GenerationSettings.LookupBackedBuilder biomeBuilder =
                new GenerationSettings.LookupBackedBuilder(context.getRegistryLookup(RegistryKeys.PLACED_FEATURE),
                        context.getRegistryLookup(RegistryKeys.CONFIGURED_CARVER));

        globalOverworldGeneration(biomeBuilder);
        DefaultBiomeFeatures.addDefaultOres(biomeBuilder);
        DefaultBiomeFeatures.addExtraGoldOre(biomeBuilder);
        DefaultBiomeFeatures.addEmeraldOre(biomeBuilder);
        DefaultBiomeFeatures.addDesertVegetation(biomeBuilder);
        DefaultBiomeFeatures.addDesertFeatures(biomeBuilder);
        DefaultBiomeFeatures.addKelp(biomeBuilder);
        DefaultBiomeFeatures.addSeagrassOnStone(biomeBuilder);
        //biomeBuilder.feature(GenerationStep.Feature.SURFACE_STRUCTURES, (RegistryEntry<PlacedFeature>) StructureKeys.SHIPWRECK);
        //biomeBuilder.addFeature(1, (RegistryEntry<PlacedFeature>) StructureKeys.SHIPWRECK);


        return new Biome.Builder()
                .precipitation(true)
                .downfall(0.4f)
                .temperature(0.7f)
                .generationSettings(biomeBuilder.build())
                .spawnSettings(spawnBuilder.build())
                .effects((new BiomeEffects.Builder())
                        .waterColor(0x2faee9)
                        .waterFogColor(0x1833aa)
                        .skyColor(0x6cbaea)
                        .fogColor(0x22a1e6)
                        .build())
                .build();
    }
}
