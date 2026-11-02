package net.kasax.raft.content.marine.entity;

import net.kasax.raft.content.marine.MarineSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * A huge, gentle whale. It cruises slowly 3–15 blocks below the surface in wide curves, comes up every minute or two
 * to breathe and spout, and sings now and then. It is passive and never despawns while a player is near. Its routes
 * only lead through deep, open water (every waypoint and the way to it are checked for room), and a whale that ends
 * up in shallow water steers back to the deepest water around it.
 */
public class WhaleEntity extends WaterAnimal {
    public static final byte EVENT_SPOUT = 90;
    public static final byte EVENT_SING = 91;
    /** Half the body length in blocks (the head is this far in front of the centre). */
    private static final double HALF_LENGTH = 3.4;

    private int nextBreath;
    private int nextSong;
    private int panicTicks;
    private int shallowTicks;
    /** Client: ticks left of the spout / song animation. */
    public int spoutAnim;
    public int singAnim;

    public WhaleEntity(EntityType<? extends WhaleEntity> type, Level level) {
        super(type, level);
        this.moveControl = new GlideMoveControl(this, 1.6F, 25.0F, 1.2F);
        this.lookControl = new GlideLookControl(this);
        this.nextBreath = 600 + this.random.nextInt(1200);
        this.nextSong = 200 + this.random.nextInt(900);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 120.0)
                .add(Attributes.MOVEMENT_SPEED, 0.012)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new BreatheGoal());
        this.goalSelector.addGoal(2, new CruiseGoal());
    }

    // ------------------------------------------------------------------------------------------------ movement
    @Override
    protected void travelInWater(Vec3 input, double baseGravity, boolean isFalling, double oldY) {
        this.moveRelative(this.getSpeed(), input);
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().scale(0.9));
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public int getMaxHeadXRot() {
        return 4;
    }

    @Override
    public int getMaxHeadYRot() {
        return 4;
    }

    /** Whales stay while a player is within 80 blocks (vanilla still removes them beyond 128). */
    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return distSqr > 80.0 * 80.0;
    }

    /** Water depth (number of water blocks) below and including pos. */
    private int depthAt(BlockPos pos) {
        Level level = this.level();
        int d = 0;
        while (d < 40 && level.getFluidState(pos.below(d)).is(FluidTags.WATER)) {
            d++;
        }
        return d;
    }

    /** Height of the water surface (y of the top water block) above pos, or pos.y if pos is not water. */
    private int surfaceAbove(BlockPos pos) {
        Level level = this.level();
        BlockPos.MutableBlockPos m = pos.mutable();
        int guard = 0;
        while (guard++ < 64 && level.getFluidState(m.above()).is(FluidTags.WATER)) {
            m.move(0, 1, 0);
        }
        return m.getY();
    }

    /** Is there room for the whale's body (about 4 wide, 3 high) at this point? */
    private boolean hasRoom(Vec3 p) {
        Level level = this.level();
        for (int dx = -2; dx <= 2; dx += 2) {
            for (int dz = -2; dz <= 2; dz += 2) {
                for (int dy = 0; dy <= 2; dy++) {
                    BlockPos b = BlockPos.containing(p.x + dx, p.y + dy, p.z + dz);
                    if (!level.getFluidState(b).is(FluidTags.WATER)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /** True if the straight way from here to target is open water for the whole body. */
    private boolean clearPath(Vec3 target) {
        Vec3 from = this.position();
        double dist = from.distanceTo(target);
        int steps = Math.max(1, (int) (dist / 3.0));
        for (int i = 1; i <= steps; i++) {
            if (!this.hasRoom(from.lerp(target, i / (double) steps))) {
                return false;
            }
        }
        return true;
    }

    /** Picks a waypoint 18–36 blocks ahead (biased to the current heading) at cruising depth. */
    private Vec3 findCruiseTarget(boolean deepOnly) {
        RandomSource r = this.random;
        Vec3 best = null;
        int bestDepth = -1;
        for (int attempt = 0; attempt < 12; attempt++) {
            float yaw = this.getYRot() + (attempt < 6 ? (r.nextFloat() - 0.5F) * 120.0F : r.nextFloat() * 360.0F);
            double dist = 18 + r.nextInt(18);
            double x = this.getX() - Mth.sin(yaw * Mth.DEG_TO_RAD) * dist;
            double z = this.getZ() + Mth.cos(yaw * Mth.DEG_TO_RAD) * dist;
            BlockPos probe = BlockPos.containing(x, this.getY() + 1, z);
            int surface = this.surfaceAbove(probe);
            if (!this.level().getFluidState(BlockPos.containing(x, surface, z)).is(FluidTags.WATER)) {
                continue;
            }
            int depth = this.depthAt(new BlockPos(probe.getX(), surface, probe.getZ()));
            if (deepOnly) {
                if (depth > bestDepth) {
                    bestDepth = depth;
                    best = new Vec3(x, surface - Math.min(depth - 4, 6), z);
                }
                continue;
            }
            if (depth < 8) {
                continue;
            }
            // cruise 3–15 blocks below the surface, at least 4 above the floor
            int below = 3 + r.nextInt(Math.max(1, Math.min(13, depth - 7)));
            Vec3 t = new Vec3(x, surface - below - 1.5, z);
            if (this.clearPath(t)) {
                return t;
            }
        }
        return best;
    }

    // ------------------------------------------------------------------------------------------------ life
    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide()) {
            if (this.spoutAnim > 0) {
                this.spoutAnim--;
            }
            if (this.singAnim > 0) {
                this.singAnim--;
            }
            return;
        }
        if (this.panicTicks > 0) {
            this.panicTicks--;
        }
        if (this.nextBreath > 0) {
            this.nextBreath--;
        }
        if (--this.nextSong <= 0) {
            this.nextSong = 900 + this.random.nextInt(1500);
            if (this.isInWater()) {
                this.playSound(MarineSounds.WHALE_SONG, 4.0F, 0.85F + this.random.nextFloat() * 0.3F);
                this.level().broadcastEntityEvent(this, EVENT_SING);
            }
        }
        // a whale in shallow water heads for the deepest water nearby
        if (this.tickCount % 20 == 0) {
            int depth = this.depthAt(BlockPos.containing(this.getX(), this.surfaceAbove(this.blockPosition()), this.getZ()));
            this.shallowTicks = depth < 6 ? this.shallowTicks + 20 : 0;
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt) {
            this.panicTicks = 100;
        }
        return hurt;
    }

    private void spout() {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        level.broadcastEntityEvent(this, EVENT_SPOUT);
        Vec3 hole = this.blowholePosition();
        level.playSound(null, hole.x, hole.y, hole.z, MarineSounds.WHALE_SPOUT, SoundSource.NEUTRAL, 2.5F,
                0.9F + this.random.nextFloat() * 0.2F);
        for (int i = 0; i < 40; i++) {
            double vx = (this.random.nextDouble() - 0.5) * 0.12;
            double vz = (this.random.nextDouble() - 0.5) * 0.12;
            double vy = 0.55 + this.random.nextDouble() * 0.45;
            level.sendParticles(i % 3 == 0 ? ParticleTypes.CLOUD : ParticleTypes.SPLASH, hole.x, hole.y, hole.z, 0, vx, vy, vz, 1.0);
        }
        level.sendParticles(ParticleTypes.SPLASH, hole.x, hole.y + 1.0, hole.z, 60, 0.4, 1.2, 0.4, 0.2);
        level.sendParticles(ParticleTypes.FALLING_WATER, hole.x, hole.y + 2.0, hole.z, 30, 0.7, 0.8, 0.7, 0.0);
    }

    /** The blowhole on top of the head (world coordinates). */
    public Vec3 blowholePosition() {
        float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
        double f = HALF_LENGTH * 0.45;
        return new Vec3(this.getX() - Mth.sin(yaw) * f, this.getY() + this.getBbHeight() * 0.95, this.getZ() + Mth.cos(yaw) * f);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_SPOUT) {
            this.spoutAnim = 30;
        } else if (id == EVENT_SING) {
            this.singAnim = 90;
        } else {
            super.handleEntityEvent(id);
        }
    }

    // ------------------------------------------------------------------------------------------------ sounds
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.DOLPHIN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.DOLPHIN_DEATH;
    }

    @Override
    protected SoundEvent getSwimSound() {
        return SoundEvents.DOLPHIN_SWIM;
    }

    @Override
    public float getVoicePitch() {
        return 0.4F + this.random.nextFloat() * 0.1F;
    }

    @Override
    protected float getSoundVolume() {
        return 2.0F;
    }

    /** Natural spawning: open water 5–25 blocks deep with room for the whole body. */
    public static boolean checkWhaleSpawnRules(EntityType<WhaleEntity> type, ServerLevelAccessor level, EntitySpawnReason reason,
                                               BlockPos pos, RandomSource random) {
        int seaLevel = level.getSeaLevel();
        if (pos.getY() > seaLevel - 5 || pos.getY() < seaLevel - 25) {
            return false;
        }
        return isOpen(level, pos);
    }

    private static boolean isOpen(LevelReader level, BlockPos pos) {
        for (int dx = -3; dx <= 3; dx += 3) {
            for (int dz = -3; dz <= 3; dz += 3) {
                for (int dy = -1; dy <= 3; dy += 2) {
                    if (!level.getFluidState(pos.offset(dx, dy, dz)).is(FluidTags.WATER)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    // ------------------------------------------------------------------------------------------------ goals
    /** Wide, slow curves through deep water; escapes shallow water. */
    private class CruiseGoal extends Goal {
        private Vec3 target;
        private int ticks;

        CruiseGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return WhaleEntity.this.isInWater();
        }

        @Override
        public void start() {
            this.pick();
        }

        private void pick() {
            boolean escape = WhaleEntity.this.shallowTicks > 40;
            this.target = WhaleEntity.this.findCruiseTarget(escape);
            this.ticks = 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            this.ticks++;
            WhaleEntity w = WhaleEntity.this;
            if (this.target == null || this.ticks > 500 || w.position().distanceToSqr(this.target) < 16.0
                    || (w.horizontalCollision && this.ticks > 20) || (w.shallowTicks > 40 && this.ticks > 60)) {
                this.pick();
                if (this.target == null) {
                    w.getMoveControl().setWait();
                    return;
                }
            }
            double speed = w.panicTicks > 0 ? 1.8 : 1.0;
            w.getMoveControl().setWantedPosition(this.target.x, this.target.y, this.target.z, speed);
        }
    }

    /** Every minute or two: rise until the back breaks the surface, spout two or three times, dive again. */
    private class BreatheGoal extends Goal {
        private int ticks;
        private int spouts;
        private Vec3 target;

        BreatheGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            WhaleEntity w = WhaleEntity.this;
            if (w.isInWater() && w.nextBreath <= 0) {
                w.nextBreath = 1200 + w.random.nextInt(1200);
                return true;
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return this.ticks < 320 && WhaleEntity.this.isInWater();
        }

        @Override
        public void start() {
            WhaleEntity w = WhaleEntity.this;
            this.ticks = 0;
            this.spouts = 0;
            int surface = w.surfaceAbove(w.blockPosition());
            float yaw = w.getYRot() * Mth.DEG_TO_RAD;
            // keep gliding forward while rising; the back ends just under the surface (spout hole above water)
            this.target = new Vec3(w.getX() - Mth.sin(yaw) * 30.0, surface + 0.9 - w.getBbHeight(), w.getZ() + Mth.cos(yaw) * 30.0);
            if (!w.clearPath(new Vec3(this.target.x, this.target.y - 1.0, this.target.z))) {
                this.target = new Vec3(w.getX(), this.target.y, w.getZ());
            }
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            WhaleEntity w = WhaleEntity.this;
            this.ticks++;
            w.getMoveControl().setWantedPosition(this.target.x, this.target.y, this.target.z, 0.8);
            boolean atSurface = Math.abs(w.getY() - this.target.y) < 0.8;
            if (atSurface && this.spouts < 3 && this.ticks % 50 == 0) {
                w.spout();
                this.spouts++;
            }
            if (this.spouts >= 3 && this.ticks % 50 == 25) {
                this.ticks = 1000;
            }
        }

        @Override
        public void stop() {
            WhaleEntity.this.getMoveControl().setWait();
        }
    }
}
