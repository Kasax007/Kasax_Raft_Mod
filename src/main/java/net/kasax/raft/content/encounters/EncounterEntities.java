package net.kasax.raft.content.encounters;

import net.kasax.raft.platform.FabricDefaultAttributeRegistry;
import net.kasax.raft.content.BcReg;
import net.kasax.raft.content.encounters.entity.CorsairCaptainEntity;
import net.kasax.raft.content.encounters.entity.ReefGuardianEntity;
import net.kasax.raft.content.encounters.entity.ScavengerEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The enemies of 3.2: scavengers (early and mid game, the chief as a mid-game elite), reef guardians (ruins and the
 * coral temple, the Reef Warden as the temple's elite) and the corsair captain (pirate cove and galleon).
 * They come with the guarded structures (StructureEncounters) and from spawn eggs; reef guardians follow the
 * guardian's spawn rules where a data pack lists them.
 */
public final class EncounterEntities {
    public static EntityType<ScavengerEntity> SCAVENGER;
    public static EntityType<ReefGuardianEntity> REEF_GUARDIAN;
    public static EntityType<CorsairCaptainEntity> CORSAIR_CAPTAIN;

    private EncounterEntities() {
    }

    public static void register() {
        SCAVENGER = BcReg.entity("scavenger", EntityType.Builder.of(ScavengerEntity::new, MobCategory.MONSTER)
                .sized(0.6F, 1.95F).eyeHeight(1.62F).ridingOffset(-0.7F).clientTrackingRange(8).notInPeaceful());
        REEF_GUARDIAN = BcReg.entity("reef_guardian", EntityType.Builder.of(ReefGuardianEntity::new, MobCategory.MONSTER)
                .sized(0.85F, 0.85F).eyeHeight(0.425F).clientTrackingRange(8).notInPeaceful());
        CORSAIR_CAPTAIN = BcReg.entity("corsair_captain", EntityType.Builder.of(CorsairCaptainEntity::new, MobCategory.MONSTER)
                .sized(0.6F, 1.95F).eyeHeight(1.74F).passengerAttachments(2.0125F).ridingOffset(-0.7F)
                .clientTrackingRange(10).notInPeaceful());

        FabricDefaultAttributeRegistry.register(SCAVENGER, ScavengerEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(REEF_GUARDIAN, ReefGuardianEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(CORSAIR_CAPTAIN, CorsairCaptainEntity.createAttributes());

        SpawnPlacements.register(REEF_GUARDIAN, SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Guardian::checkGuardianSpawnRules);
    }
}
