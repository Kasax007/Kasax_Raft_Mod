package net.kasax.raft.content.shore.entity;

import net.kasax.raft.content.shore.ShoreSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * Ember crab of the volcanic isles: a basalt-shelled crab with glowing cracks. It ignores players that keep their
 * distance, but attacks anyone who comes within a few blocks and sets them on fire. Immune to fire and lava.
 */
public class EmberCrabEntity extends Monster implements CrabAnimated {
    /** Players closer than this are attacked. */
    public static final double AGGRO_RANGE = 7.0;
    private static final int MAX_NEARBY = 5;
    private static final double NEARBY_RANGE = 40.0;

    public final AnimationState pinchAnimation = new AnimationState();
    public final AnimationState snapAnimation = new AnimationState();
    private float sideways;
    private float sidewaysO;

    public EmberCrabEntity(EntityType<? extends EmberCrabEntity> type, Level level) {
        super(type, level);
        this.xpReward = 6;
        this.setPathfindingMalus(PathType.WATER, -1.0F);
        this.setPathfindingMalus(PathType.LAVA, 8.0F);
        this.setPathfindingMalus(PathType.FIRE_IN_NEIGHBOR, 0.0F);
        this.setPathfindingMalus(PathType.FIRE, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 14.0)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.FOLLOW_RANGE, 14.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.35, false));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8, 0.002F));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                (target, level) -> target.distanceToSqr(this) < AGGRO_RANGE * AGGRO_RANGE));
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit) {
            target.igniteForSeconds(2.0F);
            level.broadcastEntityEvent(this, CrabEntity.EVENT_PINCH);
            this.playSound(ShoreSounds.CRAB_PINCH, 1.0F, 0.7F + this.random.nextFloat() * 0.15F);
            level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    6, 0.2, 0.3, 0.2, 0.02);
        }
        return hit;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == CrabEntity.EVENT_PINCH) {
            this.pinchAnimation.start(this.tickCount);
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.sidewaysO = this.sideways;
            double dx = this.getX() - this.xo;
            double dz = this.getZ() - this.zo;
            boolean moving = dx * dx + dz * dz > 2.0E-5;
            this.sideways = Mth.approach(this.sideways, moving ? 1.0F : 0.0F, 0.12F);
            RandomSource random = this.random;
            if (random.nextInt(this.isThreatening() ? 18 : 200) == 0) {
                this.snapAnimation.start(this.tickCount);
                this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), ShoreSounds.CRAB_PINCH, SoundSource.HOSTILE,
                        0.3F, 1.1F + random.nextFloat() * 0.2F, false);
            }
            // embers rising from the cracks of the shell
            if (random.nextInt(6) == 0) {
                double a = random.nextDouble() * Math.PI * 2.0;
                this.level().addParticle(random.nextInt(4) == 0 ? ParticleTypes.SMOKE : ParticleTypes.SMALL_FLAME,
                        this.getX() + Math.cos(a) * 0.25, this.getY() + 0.45, this.getZ() + Math.sin(a) * 0.25,
                        0.0, 0.02 + random.nextDouble() * 0.02, 0.0);
            }
            if (random.nextInt(40) == 0) {
                this.level().addParticle(ParticleTypes.LAVA, this.getX(), this.getY() + 0.4, this.getZ(), 0.0, 0.0, 0.0);
            }
        }
    }

    @Override
    public boolean isThreatening() {
        return this.isAggressive();
    }

    @Override
    public AnimationState pinchAnimation() {
        return this.pinchAnimation;
    }

    @Override
    public AnimationState snapAnimation() {
        return this.snapAnimation;
    }

    @Override
    public float getBurrowAmount(float partialTicks) {
        return 0.0F;
    }

    @Override
    public float getSidewaysAmount(float partialTicks) {
        return Mth.lerp(partialTicks, this.sidewaysO, this.sideways);
    }

    /** Monsters rate spots by darkness, which would forbid every spawn in daylight (these spawn by day too). */
    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        return 0.0F;
    }

    /** Monsters age twice as fast in bright light and would soon despawn in daylight. */
    @Override
    protected void updateNoActionTime() {
    }

    @Override
    public boolean isSensitiveToWater() {
        return true;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return ShoreSounds.EMBER_CRAB_AMBIENT;
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource source) {
        return ShoreSounds.EMBER_CRAB_HURT;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return ShoreSounds.EMBER_CRAB_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(ShoreSounds.CRAB_STEP, 0.2F, 0.8F);
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    /**
     * On the ground of the volcanic isles (the biome decides where), by day too, never in peaceful. Spawning ignores
     * the light, so a local limit keeps the monster cap from filling up with ember crabs: no new one while
     * {@value #MAX_NEARBY} are within {@value #NEARBY_RANGE} blocks.
     */
    public static boolean checkEmberCrabSpawnRules(EntityType<EmberCrabEntity> type, ServerLevelAccessor level, EntitySpawnReason reason,
                                                   BlockPos pos, RandomSource random) {
        if (level.getDifficulty() == Difficulty.PEACEFUL || !checkMobSpawnRules(type, level, reason, pos, random)
                || level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER)) {
            return false;
        }
        return EntitySpawnReason.isSpawner(reason) || random.nextInt(4) == 0
                && level.getEntitiesOfClass(EmberCrabEntity.class, new AABB(pos).inflate(NEARBY_RANGE)).size() < MAX_NEARBY;
    }
}
