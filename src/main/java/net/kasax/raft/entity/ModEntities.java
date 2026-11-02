package net.kasax.raft.entity;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.kasax.raft.Raft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;

public class ModEntities {
    public static final ResourceKey<EntityType<?>> SHARK_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Raft.id("shark"));

    public static final EntityType<SharkEntity> SHARK = Registry.register(BuiltInRegistries.ENTITY_TYPE, SHARK_KEY,
            EntityType.Builder.of(SharkEntity::new, MobCategory.WATER_CREATURE)
                    .sized(1.5F, 0.9F)
                    .eyeHeight(0.45F)
                    .clientTrackingRange(10)
                    .build(SHARK_KEY));

    public static void register() {
        FabricDefaultAttributeRegistry.register(SHARK, SharkEntity.createAttributes());
        SpawnPlacements.register(SHARK, SpawnPlacementTypes.IN_WATER, Heightmap.Types.OCEAN_FLOOR, SharkEntity::checkSharkSpawnRules);
        Raft.LOGGER.info("Registering entities for " + Raft.MOD_ID);
    }
}
