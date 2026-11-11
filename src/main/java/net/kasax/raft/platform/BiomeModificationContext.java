package net.kasax.raft.platform;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

/** Fabric's BiomeModificationContext (spawns, spawn costs, features) on NeoForge's modifiable biome info. */
public final class BiomeModificationContext {
    private final ModifiableBiomeInfo.BiomeInfo.Builder builder;
    private final RegistryAccess registries;

    BiomeModificationContext(ModifiableBiomeInfo.BiomeInfo.Builder builder, RegistryAccess registries) {
        this.builder = builder;
        this.registries = registries;
    }

    public SpawnSettingsContext getSpawnSettings() {
        return new SpawnSettingsContext();
    }

    public GenerationSettingsContext getGenerationSettings() {
        return new GenerationSettingsContext();
    }

    public final class SpawnSettingsContext {
        public void addSpawn(MobCategory spawnGroup, MobSpawnSettings.SpawnerData spawnEntry) {
            builder.getMobSpawnSettings().addSpawn(spawnGroup, spawnEntry);
        }

        public void setSpawnCost(EntityType<?> entityType, double mass, double gravityLimit) {
            builder.getMobSpawnSettings().addMobCharge(entityType, mass, gravityLimit);
        }

        public void setCreatureSpawnProbability(float probability) {
            builder.getMobSpawnSettings().creatureGenerationProbability(probability);
        }
    }

    public final class GenerationSettingsContext {
        public void addFeature(GenerationStep.Decoration step, ResourceKey<PlacedFeature> placedFeatureKey) {
            // as on Fabric: a feature the biome already has in that step is not added again (it would be a cycle)
            var features = builder.getGenerationSettings().getFeatures(step);
            for (Holder<PlacedFeature> present : features) {
                if (present.is(placedFeatureKey)) {
                    return;
                }
            }
            Holder<PlacedFeature> feature = registries.registryOrThrow(Registries.PLACED_FEATURE).getHolderOrThrow(placedFeatureKey);
            builder.getGenerationSettings().addFeature(step, feature);
        }
    }
}
