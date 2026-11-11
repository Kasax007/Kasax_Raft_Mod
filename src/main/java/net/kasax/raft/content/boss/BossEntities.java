package net.kasax.raft.content.boss;

import net.kasax.raft.platform.FabricDefaultAttributeRegistry;
import net.kasax.raft.content.BcReg;
import net.kasax.raft.content.boss.entity.BrineBoltEntity;
import net.kasax.raft.content.boss.entity.LeviathanEntity;
import net.kasax.raft.content.boss.entity.TidelingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Entities of the final fight (spec 10): the Leviathan (only summoned at the leviathan_altar, never spawns naturally),
 * its tideling minions and the brine bolt it spits.
 */
public final class BossEntities {
    public static EntityType<LeviathanEntity> LEVIATHAN;
    public static EntityType<TidelingEntity> TIDELING;
    public static EntityType<BrineBoltEntity> BRINE_BOLT;

    private BossEntities() {
    }

    static void register() {
        LEVIATHAN = BcReg.entity("leviathan", EntityType.Builder.of(LeviathanEntity::new, MobCategory.MONSTER)
                .sized(3.0F, 2.5F).eyeHeight(1.4F).fireImmune().clientTrackingRange(16).updateInterval(2));
        TIDELING = BcReg.entity("tideling", EntityType.Builder.of(TidelingEntity::new, MobCategory.MONSTER)
                .sized(0.7F, 0.7F).eyeHeight(0.4F).clientTrackingRange(8));
        BRINE_BOLT = BcReg.entity("brine_bolt", EntityType.Builder.<BrineBoltEntity>of(BrineBoltEntity::new, MobCategory.MISC).sized(0.5F, 0.5F).clientTrackingRange(6).updateInterval(10));

        FabricDefaultAttributeRegistry.register(LEVIATHAN, LeviathanEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(TIDELING, TidelingEntity.createAttributes());

        // no natural spawns (the leviathan comes from its altar, tidelings from the leviathan); placements only keep
        // /summon and spawn eggs sensible
        SpawnPlacements.register(TIDELING, SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> level.getFluidState(pos).isSource());
    }
}
