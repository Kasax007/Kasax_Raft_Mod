package net.kasax.raft.content.marine.entity;

import net.kasax.raft.content.audio.AudioSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * The anglerfish of the abyssal trench: it hangs almost still in the dark with its glowing lure bobbing, creeps
 * closer to whatever comes near and then darts forward to bite. After a bite it backs off and lurks again.
 */
public class AnglerfishEntity extends AbstractMarineMonster {
    public AnglerfishEntity(EntityType<? extends AnglerfishEntity> type, Level level) {
        super(type, level, 60, 8);
        this.xpReward = 7;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 18.0)
                .add(Attributes.MOVEMENT_SPEED, 1.0)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new DartAttackGoal());
        this.goalSelector.addGoal(4, new LurkGoal());
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true,
                target -> target.isInWater() && !target.isSpectator()));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractFish.class, 40, true, false, null));
    }

    @Override
    protected boolean sinksWhenIdle() {
        return false;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide() && this.isInWater() && this.random.nextInt(25) == 0) {
            // a faint glow drifting from the lure
            this.level().addParticle(ParticleTypes.GLOW, this.getX(), this.getY() + this.getBbHeight() + 0.3, this.getZ(), 0.0, 0.01, 0.0);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        // the hum of the glowing lure, now and then (quiet, carries only a few blocks)
        return this.isInWater() && this.random.nextInt(3) == 0 ? AudioSounds.ANGLERFISH_AMBIENT : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return AudioSounds.ANGLERFISH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AudioSounds.ANGLERFISH_DEATH;
    }

    @Override
    public void playAttackSound() {
        this.playSound(AudioSounds.ANGLERFISH_BITE, 1.0F, 0.9F + this.random.nextFloat() * 0.2F);
    }

    @Override
    public float getVoicePitch() {
        return 0.9F + this.random.nextFloat() * 0.2F;
    }

    /** Natural spawning: deep water only (45+ blocks below the surface), never in peaceful. */
    public static boolean checkAnglerSpawnRules(EntityType<AnglerfishEntity> type, ServerLevelAccessor level, MobSpawnType reason,
                                                BlockPos pos, RandomSource random) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        return pos.getY() <= level.getSeaLevel() - 45 && isWaterColumn(level, pos.above(), 2);
    }

    /** Hover in place, drifting to a new spot nearby every now and then. */
    private class LurkGoal extends Goal {
        private int ticks;

        LurkGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return AnglerfishEntity.this.getTarget() == null && AnglerfishEntity.this.isInWater();
        }

        @Override
        public void tick() {
            AnglerfishEntity a = AnglerfishEntity.this;
            if (++this.ticks % 160 == 0 && a.random.nextInt(3) == 0) {
                Vec3 p = a.position().add((a.random.nextDouble() - 0.5) * 8, (a.random.nextDouble() - 0.5) * 3, (a.random.nextDouble() - 0.5) * 8);
                if (a.level().getFluidState(BlockPos.containing(p)).is(FluidTags.WATER)) {
                    a.getNavigation().moveTo(p.x, p.y, p.z, 0.35);
                }
            }
            a.setDeltaMovement(a.getDeltaMovement().multiply(0.9, 0.8, 0.9));
        }
    }

    /** Creep closer, then a fast dart and bite, then back off for a moment. */
    private class DartAttackGoal extends Goal {
        private int cooldown;
        private int dartTicks;
        private int backOff;
        /** Ticks until the path is computed again (pathfinding every tick is expensive). */
        private int repath;

        DartAttackGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = AnglerfishEntity.this.getTarget();
            return t != null && t.isAlive() && t.isInWater();
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse() && AnglerfishEntity.this.distanceToSqr(AnglerfishEntity.this.getTarget()) < 24 * 24;
        }

        @Override
        public void start() {
            this.dartTicks = 0;
            this.backOff = 0;
            this.repath = 0;
        }

        @Override
        public void stop() {
            AnglerfishEntity.this.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            AnglerfishEntity a = AnglerfishEntity.this;
            LivingEntity t = a.getTarget();
            if (t == null) {
                return;
            }
            a.getLookControl().setLookAt(t, 30.0F, 30.0F);
            double d = a.distanceToSqr(t);
            if (this.cooldown > 0) {
                this.cooldown--;
            }
            if (this.repath > 0) {
                this.repath--;
            }
            if (this.backOff > 0) {
                this.backOff--;
                if (this.repath <= 0) {
                    this.repath = 10;
                    Vec3 away = a.position().subtract(t.position()).normalize();
                    a.getNavigation().moveTo(a.getX() + away.x * 4, a.getY() + away.y * 2, a.getZ() + away.z * 4, 0.6);
                }
                return;
            }
            if (this.dartTicks > 0) {
                this.dartTicks--;
                Vec3 dir = t.getEyePosition().subtract(a.position()).normalize();
                a.setDeltaMovement(a.getDeltaMovement().scale(0.6).add(dir.scale(0.38)));
                if (a.isWithinMeleeAttackRange(t) && this.cooldown <= 0 && a.level() instanceof ServerLevel level) {
                    a.doHurtTarget(t);
                    this.cooldown = 30;
                    this.dartTicks = 0;
                    this.backOff = 30;
                    this.repath = 0;
                }
                return;
            }
            if (d < 6.0 * 6.0 && this.cooldown <= 0 && a.getSensing().hasLineOfSight(t)) {
                this.dartTicks = 14;
                a.playSound(SoundEvents.FISH_SWIM, 0.8F, 0.6F);
                a.level().broadcastEntityEvent(a, EVENT_BITE);
            } else if (this.repath <= 0) {
                // creep closer slowly (a new path every half second is enough at this speed)
                this.repath = 10;
                a.getNavigation().moveTo(t, 0.55);
            }
        }
    }
}
