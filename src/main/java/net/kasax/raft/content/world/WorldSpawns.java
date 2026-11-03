package net.kasax.raft.content.world;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.kasax.raft.Raft;
import net.kasax.raft.world.biome.ModBiomes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.Biome;

import java.util.List;
import java.util.Optional;

/**
 * Natural spawns of the Beach Craft 3 creatures in the biomes of the raft dimension (docs/design/beachcraft3.md 4.2).
 * <p>
 * Vanilla mobs and the shark are in the biome JSON. The creatures of the other content modules are added here when
 * the biomes are loaded, looked up by id: a creature that is not registered is skipped instead of breaking the whole
 * biome registry. The spawn category is the one of the entity type, so the mob counts against the right mob cap.
 */
public final class WorldSpawns {
    private record Spawn(ResourceKey<Biome> biome, String entity, int weight, int min, int max) {
    }

    private static final List<Spawn> SPAWNS = List.of(
            new Spawn(ModBiomes.RAFT_OCEAN_BIOME, "whale", 1, 1, 1),
            new Spawn(ModBiomes.RAFT_OCEAN_BIOME, "jellyfish", 4, 2, 4),
            new Spawn(ModBiomes.RAFT_OCEAN_BIOME, "hammerhead_shark", 1, 1, 1),
            new Spawn(ModBiomes.KELP_FOREST, "sea_otter", 4, 1, 3),
            new Spawn(ModBiomes.KELP_FOREST, "jellyfish", 2, 1, 3),
            new Spawn(ModBiomes.KELP_FOREST, "moray_eel", 1, 1, 1),
            new Spawn(ModBiomes.CORAL_SHALLOWS, "manta_ray", 3, 1, 2),
            new Spawn(ModBiomes.CORAL_SHALLOWS, "moray_eel", 2, 1, 1),
            new Spawn(ModBiomes.GLOWING_LAGOON, "jellyfish", 6, 2, 5),
            new Spawn(ModBiomes.GLOWING_LAGOON, "manta_ray", 1, 1, 1),
            new Spawn(ModBiomes.DROWNED_SHELF, "moray_eel", 2, 1, 1),
            new Spawn(ModBiomes.DROWNED_SHELF, "drowned_corsair", 1, 1, 2),
            new Spawn(ModBiomes.ABYSSAL_TRENCH, "anglerfish", 4, 1, 2),
            new Spawn(ModBiomes.ABYSSAL_TRENCH, "jellyfish", 2, 1, 3),
            new Spawn(ModBiomes.WRECK_GRAVEYARD, "drowned_corsair", 3, 1, 3),
            new Spawn(ModBiomes.FROST_FLOES, "penguin", 6, 2, 5),
            new Spawn(ModBiomes.FROST_FLOES, "whale", 1, 1, 1),
            new Spawn(ModBiomes.PALM_ISLE, "crab", 8, 2, 4),
            new Spawn(ModBiomes.PALM_ISLE, "seagull", 6, 2, 5),
            new Spawn(ModBiomes.JUNGLE_ISLE, "seagull", 2, 1, 3),
            new Spawn(ModBiomes.JUNGLE_ISLE, "crab", 3, 1, 3),
            new Spawn(ModBiomes.MANGROVE_ISLE, "crab", 4, 1, 3),
            new Spawn(ModBiomes.VOLCANIC_ISLE, "ember_crab", 5, 1, 3),
            new Spawn(ModBiomes.VOLCANIC_ISLE, "seagull", 1, 1, 2));

    private WorldSpawns() {
    }

    public static void init() {
        BiomeModifications.create(Raft.id("world_creature_spawns")).add(ModificationPhase.ADDITIONS,
                context -> context.getBiomeKey().identifier().getNamespace().equals(Raft.MOD_ID),
                (selection, context) -> {
                    for (Spawn spawn : SPAWNS) {
                        if (!spawn.biome().equals(selection.getBiomeKey())) {
                            continue;
                        }
                        Optional<EntityType<?>> type = BuiltInRegistries.ENTITY_TYPE.getOptional(Raft.id(spawn.entity()));
                        type.ifPresent(t -> context.getMobSpawnSettings().addSpawn(t, spawn.weight(), spawn.min(), spawn.max()));
                    }
                });
    }
}
