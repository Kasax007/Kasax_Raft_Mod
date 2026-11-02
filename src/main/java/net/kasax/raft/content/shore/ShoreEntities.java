package net.kasax.raft.content.shore;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.kasax.raft.content.BcReg;
import net.kasax.raft.content.shore.entity.CrabEntity;
import net.kasax.raft.content.shore.entity.DrownedCorsairEntity;
import net.kasax.raft.content.shore.entity.EmberCrabEntity;
import net.kasax.raft.content.shore.entity.PenguinEntity;
import net.kasax.raft.content.shore.entity.SeaOtterEntity;
import net.kasax.raft.content.shore.entity.SeagullEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Entity types of the shore module: beach and surface creatures plus the drowned corsairs.
 * Natural spawns come from the biome JSONs of the world module; the spawn rules here decide where exactly.
 */
public final class ShoreEntities {
    public static EntityType<CrabEntity> CRAB;
    public static EntityType<EmberCrabEntity> EMBER_CRAB;
    public static EntityType<SeagullEntity> SEAGULL;
    public static EntityType<PenguinEntity> PENGUIN;
    public static EntityType<SeaOtterEntity> SEA_OTTER;
    public static EntityType<DrownedCorsairEntity> DROWNED_CORSAIR;

    private ShoreEntities() {
    }

    public static void register() {
        CRAB = BcReg.entity("crab", EntityType.Builder.of(CrabEntity::new, MobCategory.CREATURE)
                .sized(0.6F, 0.4F).eyeHeight(0.32F).clientTrackingRange(8));
        EMBER_CRAB = BcReg.entity("ember_crab", EntityType.Builder.of(EmberCrabEntity::new, MobCategory.MONSTER)
                .sized(0.8F, 0.5F).eyeHeight(0.4F).fireImmune().clientTrackingRange(8));
        SEAGULL = BcReg.entity("seagull", EntityType.Builder.of(SeagullEntity::new, MobCategory.CREATURE)
                .sized(0.5F, 0.6F).eyeHeight(0.5F).clientTrackingRange(10));
        PENGUIN = BcReg.entity("penguin", EntityType.Builder.of(PenguinEntity::new, MobCategory.CREATURE)
                .sized(0.5F, 0.9F).eyeHeight(0.8F).clientTrackingRange(10));
        SEA_OTTER = BcReg.entity("sea_otter", EntityType.Builder.of(SeaOtterEntity::new, MobCategory.WATER_CREATURE)
                .sized(0.7F, 0.4F).eyeHeight(0.3F).clientTrackingRange(10));
        DROWNED_CORSAIR = BcReg.entity("drowned_corsair", EntityType.Builder.of(DrownedCorsairEntity::new, MobCategory.MONSTER)
                .sized(0.6F, 1.95F).eyeHeight(1.74F).passengerAttachments(2.0125F).ridingOffset(-0.7F)
                .clientTrackingRange(8).notInPeaceful());

        FabricDefaultAttributeRegistry.register(CRAB, CrabEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(EMBER_CRAB, EmberCrabEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(SEAGULL, SeagullEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(PENGUIN, PenguinEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(SEA_OTTER, SeaOtterEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(DROWNED_CORSAIR, DrownedCorsairEntity.createAttributes());

        SpawnPlacements.register(CRAB, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                CrabEntity::checkCrabSpawnRules);
        SpawnPlacements.register(EMBER_CRAB, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                EmberCrabEntity::checkEmberCrabSpawnRules);
        SpawnPlacements.register(SEAGULL, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                SeagullEntity::checkSeagullSpawnRules);
        SpawnPlacements.register(PENGUIN, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                PenguinEntity::checkPenguinSpawnRules);
        SpawnPlacements.register(SEA_OTTER, SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING,
                SeaOtterEntity::checkSeaOtterSpawnRules);
        SpawnPlacements.register(DROWNED_CORSAIR, SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                DrownedCorsairEntity::checkCorsairSpawnRules);
    }
}
