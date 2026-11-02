package net.kasax.raft.content.marine.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

/**
 * A manta ray: glides through the shallows in slow, wide loops around its home spot, flapping its wings, and now and
 * then leaps out of the water with a splash. Passive; it flees a little faster when hurt.
 */
public class MantaRayEntity extends WaterAnimal {
    private @Nullable BlockPos home;
    private int panicTicks;
    private int nextLeap;

    public MantaRayEntity(EntityType<? extends MantaRayEntity> type, Level level) {
        super(type, level);
        this.moveControl = new GlideMoveControl(this, 3.0F, 20.0F, 1.5F);
        this.lookControl = new GlideLookControl(this);
        this.nextLeap = 2400 + this.random.nextInt(3600);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.016);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new LeapGoal());
        this.goalSelector.addGoal(2, new LoopGoal());
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
                                                  @Nullable SpawnGroupData groupData) {
        this.home = this.blockPosition();
        return super.finalizeSpawn(level, difficulty, reason, groupData);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (this.home != null) {
            output.putInt("HomeX", this.home.getX());
            output.putInt("HomeY", this.home.getY());
            output.putInt("HomeZ", this.home.getZ());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        int y = input.getIntOr("HomeY", Integer.MIN_VALUE);
        if (y != Integer.MIN_VALUE) {
            this.home = new BlockPos(input.getIntOr("HomeX", 0), y, input.getIntOr("HomeZ", 0));
        }
    }

    @Override
    protected void travelInWater(Vec3 input, double baseGravity, boolean isFalling, double oldY) {
        this.moveRelative(this.getSpeed(), input);
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().scale(0.9));
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return distSqr > 64.0 * 64.0;
    }

    @Override
    public int getMaxHeadXRot() {
        return 5;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide()) {
            if (this.panicTicks > 0) {
                this.panicTicks--;
            }
            if (this.nextLeap > 0) {
                this.nextLeap--;
            }
        }
        if (this.level().isClientSide() && this.isInWater() && this.random.nextInt(30) == 0) {
            this.level().addParticle(ParticleTypes.BUBBLE, this.getRandomX(1.0), this.getY() + 0.2, this.getRandomZ(1.0), 0.0, 0.05, 0.0);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt) {
            this.panicTicks = 80;
        }
        return hurt;
    }

    private int surfaceAbove(BlockPos pos) {
        BlockPos.MutableBlockPos m = pos.mutable();
        int guard = 0;
        while (guard++ < 40 && this.level().getFluidState(m.above()).is(FluidTags.WATER)) {
            m.move(0, 1, 0);
        }
        return m.getY();
    }

    private int floorBelow(BlockPos pos) {
        BlockPos.MutableBlockPos m = pos.mutable();
        int guard = 0;
        while (guard++ < 40 && this.level().getFluidState(m.below()).is(FluidTags.WATER)) {
            m.move(0, -1, 0);
        }
        return m.getY();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.GUARDIAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GUARDIAN_DEATH;
    }

    @Override
    protected SoundEvent getSwimSound() {
        return SoundEvents.DOLPHIN_SWIM;
    }

    @Override
    public float getVoicePitch() {
        return 0.7F + this.random.nextFloat() * 0.2F;
    }

    /** Natural spawning: shallow water, 2–15 blocks below the surface. */
    public static boolean checkMantaSpawnRules(EntityType<MantaRayEntity> type, ServerLevelAccessor level, EntitySpawnReason reason,
                                               BlockPos pos, RandomSource random) {
        int sea = level.getSeaLevel();
        return pos.getY() <= sea - 2 && pos.getY() >= sea - 15
                && level.getFluidState(pos).is(FluidTags.WATER)
                && level.getFluidState(pos.above()).is(FluidTags.WATER)
                && level.getFluidState(pos.north(2)).is(FluidTags.WATER)
                && level.getFluidState(pos.south(2)).is(FluidTags.WATER)
                && level.getFluidState(pos.east(2)).is(FluidTags.WATER)
                && level.getFluidState(pos.west(2)).is(FluidTags.WATER);
    }

    /** Slow loops (radius 5–11) around a centre near home; the centre wanders, the loop direction changes now and then. */
    private class LoopGoal extends Goal {
        private double cx, cz, cy, radius;
        private int dir = 1;
        private int ticks;
        private int loopTime;

        LoopGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return MantaRayEntity.this.isInWater();
        }

        @Override
        public void start() {
            this.newLoop();
        }

        private void newLoop() {
            MantaRayEntity m = MantaRayEntity.this;
            RandomSource r = m.random;
            BlockPos base = m.home != null && m.home.distSqr(m.blockPosition()) < 40 * 40 ? m.home : m.blockPosition();
            if (m.home == null) {
                m.home = m.blockPosition();
            }
            for (int i = 0; i < 10; i++) {
                double x = base.getX() + (r.nextDouble() - 0.5) * 24.0;
                double z = base.getZ() + (r.nextDouble() - 0.5) * 24.0;
                BlockPos p = BlockPos.containing(x, m.getY(), z);
                if (!m.level().getFluidState(p).is(FluidTags.WATER)) {
                    continue;
                }
                int top = m.surfaceAbove(p);
                int floor = m.floorBelow(p);
                if (top - floor < 3) {
                    continue;
                }
                this.cx = x;
                this.cz = z;
                this.cy = Mth.clamp(floor + 1.5 + r.nextDouble() * Math.max(0, top - floor - 3), floor + 1.2, top - 1.0);
                this.radius = 5.0 + r.nextDouble() * 6.0;
                this.dir = r.nextBoolean() ? 1 : -1;
                this.loopTime = 300 + r.nextInt(500);
                this.ticks = 0;
                return;
            }
            this.cx = m.getX();
            this.cz = m.getZ();
            this.cy = m.getY();
            this.radius = 4.0;
            this.loopTime = 200;
            this.ticks = 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            MantaRayEntity m = MantaRayEntity.this;
            if (++this.ticks > this.loopTime || (m.horizontalCollision && this.ticks % 20 == 0)) {
                if (m.horizontalCollision) {
                    this.dir = -this.dir;
                }
                this.newLoop();
            }
            double angle = Math.atan2(m.getZ() - this.cz, m.getX() - this.cx) + this.dir * 0.7;
            double tx = this.cx + Math.cos(angle) * this.radius;
            double tz = this.cz + Math.sin(angle) * this.radius;
            double ty = this.cy + Math.sin(this.ticks * 0.02) * 0.8;
            BlockPos tp = BlockPos.containing(tx, ty, tz);
            if (!m.level().getFluidState(tp).is(FluidTags.WATER)) {
                this.dir = -this.dir;
                return;
            }
            m.getMoveControl().setWantedPosition(tx, ty, tz, m.panicTicks > 0 ? 2.0 : 1.0);
        }
    }

    /** Rarely: speed up towards the surface and leap out of the water. */
    private class LeapGoal extends Goal {
        private int ticks;

        LeapGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            MantaRayEntity m = MantaRayEntity.this;
            if (m.nextLeap > 0 || !m.isInWater()) {
                return false;
            }
            m.nextLeap = 2400 + m.random.nextInt(3600);
            BlockPos p = m.blockPosition();
            return m.surfaceAbove(p) - m.getY() < 6 && m.surfaceAbove(p) - m.floorBelow(p) >= 4;
        }

        @Override
        public boolean canContinueToUse() {
            return this.ticks < 60;
        }

        @Override
        public void start() {
            this.ticks = 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            MantaRayEntity m = MantaRayEntity.this;
            this.ticks++;
            if (m.isInWater()) {
                float yaw = m.getYRot() * Mth.DEG_TO_RAD;
                Vec3 look = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
                m.setDeltaMovement(m.getDeltaMovement().add(look.scale(0.03)).add(0, 0.09, 0));
                m.setXRot(m.rotlerpPitch(-35.0F));
                if (m.getDeltaMovement().y > 0.6) {
                    this.ticks = 100;
                }
            }
        }
    }

    private float rotlerpPitch(float target) {
        return this.getXRot() + Mth.clamp(target - this.getXRot(), -4.0F, 4.0F);
    }
}
