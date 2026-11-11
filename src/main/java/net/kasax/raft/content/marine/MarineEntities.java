package net.kasax.raft.content.marine;

import net.kasax.raft.platform.FabricDefaultAttributeRegistry;
import net.kasax.raft.content.BcReg;
import net.kasax.raft.content.marine.entity.AnglerfishEntity;
import net.kasax.raft.content.marine.entity.HammerheadSharkEntity;
import net.kasax.raft.content.marine.entity.JellyfishEntity;
import net.kasax.raft.content.marine.entity.MantaRayEntity;
import net.kasax.raft.content.marine.entity.MorayEelEntity;
import net.kasax.raft.content.marine.entity.WhaleEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Sea creatures of Beach Craft 3 (spec 7.2). Categories (for the biome spawn lists): hammerhead_shark, whale,
 * manta_ray = water_creature; jellyfish, moray_eel = water_ambient; anglerfish = underground_water_creature.
 */
public final class MarineEntities {
    public static EntityType<HammerheadSharkEntity> HAMMERHEAD_SHARK;
    public static EntityType<WhaleEntity> WHALE;
    public static EntityType<MantaRayEntity> MANTA_RAY;
    public static EntityType<JellyfishEntity> JELLYFISH;
    public static EntityType<AnglerfishEntity> ANGLERFISH;
    public static EntityType<MorayEelEntity> MORAY_EEL;

    private MarineEntities() {
    }

    static void register() {
        HAMMERHEAD_SHARK = BcReg.entity("hammerhead_shark", EntityType.Builder.of(HammerheadSharkEntity::new, MobCategory.WATER_CREATURE)
                .sized(2.0F, 1.0F).eyeHeight(0.5F).clientTrackingRange(10));
        WHALE = BcReg.entity("whale", EntityType.Builder.of(WhaleEntity::new, MobCategory.WATER_CREATURE)
                .sized(6.0F, 3.0F).eyeHeight(1.6F).clientTrackingRange(16));
        MANTA_RAY = BcReg.entity("manta_ray", EntityType.Builder.of(MantaRayEntity::new, MobCategory.WATER_CREATURE)
                .sized(2.2F, 0.5F).eyeHeight(0.25F).clientTrackingRange(10));
        JELLYFISH = BcReg.entity("jellyfish", EntityType.Builder.of(JellyfishEntity::new, MobCategory.WATER_AMBIENT)
                .sized(0.6F, 0.8F).eyeHeight(0.5F).clientTrackingRange(8));
        ANGLERFISH = BcReg.entity("anglerfish", EntityType.Builder.of(AnglerfishEntity::new, MobCategory.UNDERGROUND_WATER_CREATURE)
                .sized(0.9F, 0.7F).eyeHeight(0.4F).clientTrackingRange(8));
        MORAY_EEL = BcReg.entity("moray_eel", EntityType.Builder.of(MorayEelEntity::new, MobCategory.WATER_AMBIENT)
                // hitbox narrower than the 1.2 long body, so the eel fits into one-block crevices
                .sized(0.6F, 0.4F).eyeHeight(0.25F).clientTrackingRange(8));

        FabricDefaultAttributeRegistry.register(HAMMERHEAD_SHARK, HammerheadSharkEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(WHALE, WhaleEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(MANTA_RAY, MantaRayEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(JELLYFISH, JellyfishEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(ANGLERFISH, AnglerfishEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(MORAY_EEL, MorayEelEntity.createAttributes());

        SpawnPlacements.register(HAMMERHEAD_SHARK, SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                HammerheadSharkEntity::checkHammerheadSpawnRules);
        SpawnPlacements.register(WHALE, SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, WhaleEntity::checkWhaleSpawnRules);
        SpawnPlacements.register(MANTA_RAY, SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MantaRayEntity::checkMantaSpawnRules);
        SpawnPlacements.register(JELLYFISH, SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                JellyfishEntity::checkJellyfishSpawnRules);
        SpawnPlacements.register(ANGLERFISH, SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                AnglerfishEntity::checkAnglerSpawnRules);
        SpawnPlacements.register(MORAY_EEL, SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MorayEelEntity::checkMoraySpawnRules);
    }
}
