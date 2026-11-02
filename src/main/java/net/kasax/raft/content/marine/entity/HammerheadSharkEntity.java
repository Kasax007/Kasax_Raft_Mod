package net.kasax.raft.content.marine.entity;

import net.kasax.raft.entity.SharkEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * The hammerhead: a bigger, stronger shark of the deep open sea. Same rules as the shark (hunts swimmers only,
 * circles and bites rafts), but tougher and it hits harder. It spawns deeper than the common shark.
 */
public class HammerheadSharkEntity extends SharkEntity {
    public HammerheadSharkEntity(EntityType<? extends HammerheadSharkEntity> type, Level level) {
        super(type, level);
        this.xpReward = 14;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 1.25)
                .add(Attributes.ATTACK_DAMAGE, 7.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.ARMOR, 2.0);
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.85F;
    }

    /** Natural spawning: deeper water (6–50 blocks below the surface), never in peaceful. */
    public static boolean checkHammerheadSpawnRules(EntityType<HammerheadSharkEntity> type, ServerLevelAccessor level,
                                                    EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        int seaLevel = level.getSeaLevel();
        return pos.getY() <= seaLevel - 6
                && pos.getY() >= seaLevel - 50
                && isWaterColumn(level, pos.above(2), 4);
    }
}
