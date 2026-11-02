package net.kasax.raft.world.biome;

import net.kasax.raft.Raft;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.Carvers;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ARGB;
import net.minecraft.world.attribute.BackgroundMusic;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class ModBiomes {
    public static final ResourceKey<Biome> RAFT_OCEAN_BIOME = ResourceKey.create(Registries.BIOME, Raft.id("raft_ocean_biome"));

    public static void bootstrap(BootstrapContext<Biome> context) {
        context.register(RAFT_OCEAN_BIOME, raftOceanBiome(context));
    }

    public static void globalOverworldGeneration(BiomeGenerationSettings.Builder builder) {
        builder.addCarver(Carvers.CAVE);
        builder.addCarver(Carvers.CAVE_EXTRA_UNDERGROUND);
        builder.addCarver(Carvers.CANYON);
        BiomeDefaultFeatures.addDefaultMonsterRoom(builder);
        BiomeDefaultFeatures.addDefaultUndergroundVariety(builder);
        BiomeDefaultFeatures.addDefaultCrystalFormations(builder);
    }

    public static Biome raftOceanBiome(BootstrapContext<Biome> context) {
        MobSpawnSettings.Builder spawns = new MobSpawnSettings.Builder();
        // squid, tropical fish, dolphins, drowned and the usual night monsters and cave mobs
        BiomeDefaultFeatures.warmOceanSpawns(spawns, 1, 3);

        HolderGetter<PlacedFeature> placedFeatures = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<WorldCarver> carvers = context.lookup(Registries.CARVER);
        BiomeGenerationSettings.Builder biomeBuilder = new BiomeGenerationSettings.Builder(placedFeatures, carvers);

        globalOverworldGeneration(biomeBuilder);
        BiomeDefaultFeatures.addDefaultOres(biomeBuilder);
        BiomeDefaultFeatures.addDesertVegetation(biomeBuilder);
        BiomeDefaultFeatures.addDesertExtraDecoration(biomeBuilder);
        BiomeDefaultFeatures.addLukeWarmKelp(biomeBuilder);

        return new Biome.BiomeBuilder()
                .hasPrecipitation(true)
                .downfall(0.4f)
                .temperature(0.7f)
                .mobSpawnSettings(spawns.build())
                .generationSettings(biomeBuilder.build())
                .setAttribute(EnvironmentAttributes.SKY_COLOR, ARGB.vector3fFromRGB24(0x6cbaea))
                .setAttribute(EnvironmentAttributes.FOG_COLOR, ARGB.vector3fFromRGB24(0x22a1e6))
                .setAttribute(EnvironmentAttributes.WATER_FOG_COLOR, ARGB.vector3fFromRGB24(0x1833aa))
                .setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(SoundEvents.MUSIC_BIOME_CHERRY_GROVE))
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .waterColor(0x2faee9)
                        .build())
                .build();
    }
}
