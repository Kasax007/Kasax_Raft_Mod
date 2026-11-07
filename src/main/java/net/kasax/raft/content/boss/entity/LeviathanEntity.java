package net.kasax.raft.content.boss.entity;

import net.kasax.raft.content.boss.BossEntities;
import net.kasax.raft.content.boss.BossSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.kasax.raft.compat.ValueInput;
import net.kasax.raft.compat.ValueOutput;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The Leviathan (spec 10): a colossal sea serpent bound in the pool of the Abyssal Sanctum. Only the head has a hitbox
 * (3.0 x 2.5); the long body is visual ({@link LeviathanBody}). The fight is a state machine driven directly by the
 * server (no path finding): it circles submerged and invulnerable, rises to spit brine bolts, lunges across the floor
 * ring and lies stranded (x1.5 damage taken), and from half health also roars (pushback + tidelings) and stirs a
 * whirlpool that drags players into the pool. Below a quarter it is faster and chains its attacks.
 * <p>Arena contract: it lives in the pool around its home (= spawn point, pool centre 4 below the altar) and never
 * leaves it for more than a lunge: out of water for over 8 s it leaps back; further than 30 blocks from home it
 * returns at once.
 */
public class LeviathanEntity extends Monster {
    /** At most this many tidelings alive at once (spec 10: 3-4 per roar; more swarmed players in tests). */
    private static final int MAX_TIDELINGS = 5;

    public static final int S_INTRO = 0;
    public static final int S_CIRCLE = 1;
    public static final int S_RISE_SPIT = 2;
    public static final int S_LUNGE_AIM = 3;
    public static final int S_LUNGE_LEAP = 4;
    public static final int S_STRANDED = 5;
    public static final int S_RETURN = 6;
    public static final int S_TIDAL_ROAR = 7;
    public static final int S_WHIRLPOOL = 8;
    public static final int S_LEAP_BACK = 9;

    /** Head pivot (centre of the hitbox) above the entity position. */
    public static final double PIVOT = 1.25;
    public static final int DEATH_TICKS = 90;
    private static final double LEASH = 30.0;
    private static final double ARENA = 24.0;
    private static final double GRAVITY = 0.09;
    private static final int TEAL = 0x3FE6D8;

    private static final EntityDataAccessor<Integer> DATA_STATE = SynchedEntityData.defineId(LeviathanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_PHASE = SynchedEntityData.defineId(LeviathanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_JAW = SynchedEntityData.defineId(LeviathanEntity.class, EntityDataSerializers.INT);

    private final ServerBossEvent bossEvent = new ServerBossEvent(this.getDisplayName(),
            BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10);

    // ---------------------------------------------------------------- server state
    private @Nullable BlockPos home;
    private double surfaceY = Double.NaN;
    private double bottomY;
    private double poolRadius = 7.0;
    private int state = S_INTRO;
    private int stateTicks;
    private int stateLength;
    private int lastAttack = -1;
    private int repeatCount;
    private float circleAngle;
    private float circleDir = 1.0F;
    private int shotsLeft;
    private int outOfWaterTicks;
    private int stuckTicks;
    private int noPlayerTicks;
    private int biteCooldown;
    private boolean pendingRoar;
    private boolean surfaced;
    private Vec3 aim = Vec3.ZERO;
    private @Nullable DamageSource deathSource;
    private boolean deathByPlayer;

    // ---------------------------------------------------------------- client state
    public final LeviathanBody body = new LeviathanBody();
    public float jaw;
    public float jawO;
    public int clientStateTicks;

    public LeviathanEntity(EntityType<? extends LeviathanEntity> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        this.setNoGravity(true);
        this.setPersistenceRequired();
        this.lookControl = new LookControl(this) {
            @Override
            public void tick() {
            }
        };
        this.bossEvent.setDarkenScreen(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 600.0)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.STEP_HEIGHT, 1.6);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STATE, S_INTRO);
        builder.define(DATA_PHASE, 1);
        builder.define(DATA_JAW, 0);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (DATA_STATE.equals(accessor)) {
            this.clientStateTicks = 0;
        }
    }

    // ================================================================================================ basics
    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                                  @Nullable SpawnGroupData groupData) {
        this.home = this.blockPosition();
        this.setPersistenceRequired();
        return super.finalizeSpawn(level, difficulty, reason, groupData);
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    protected void updateNoActionTime() {
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        return level.getFluidState(pos).is(FluidTags.WATER) ? 10.0F : 0.0F;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity entity) {
        // nothing moves the Leviathan
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    protected boolean canRide(Entity vehicle) {
        return false;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return false;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageModifier, DamageSource damageSource) {
        return false;
    }

    @Override
    public void knockback(double power, double xd, double zd) {
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.is(DamageTypes.DROWN) || source.is(DamageTypes.FALL) || source.is(DamageTypes.IN_WALL)
                || source.is(DamageTypes.CRAMMING) || source.is(DamageTypes.FLY_INTO_WALL) || source.is(DamageTypes.DRY_OUT)
                || source.is(DamageTypeTags.IS_FIRE) || super.isInvulnerableTo(source);
    }

    @Override
    public float getSoundVolume() {
        return 3.0F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 220;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return BossSounds.LEVIATHAN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return BossSounds.LEVIATHAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return BossSounds.LEVIATHAN_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
    }

    /** Pure velocity movement: the state machine sets the motion, collisions come from move(). */
    @Override
    public void travel(Vec3 input) {
        this.move(MoverType.SELF, this.getDeltaMovement());
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag121) {
        ValueOutput output = ValueOutput.of(tag121, this.registryAccess());
        super.addAdditionalSaveData(output.tag());
        if (this.home != null) {
            output.putInt("HomeX", this.home.getX());
            output.putInt("HomeY", this.home.getY());
            output.putInt("HomeZ", this.home.getZ());
        }
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag121) {
        ValueInput input = ValueInput.of(tag121, this.registryAccess());
        super.readAdditionalSaveData(input.tag());
        if (input.getInt("HomeY").isPresent()) {
            this.home = new BlockPos(input.getIntOr("HomeX", 0), input.getIntOr("HomeY", 0), input.getIntOr("HomeZ", 0));
            this.state = S_CIRCLE;
        }
        if (this.hasCustomName()) {
            this.bossEvent.setName(this.getDisplayName());
        }
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        this.bossEvent.removeAllPlayers();
    }

    // ================================================================================================ geometry
    public Vec3 pivot() {
        return this.position().add(0.0, PIVOT, 0.0);
    }

    /** Unit vector the head points to (yaw + pitch). */
    public Vec3 forward(float partialTicks) {
        return Vec3.directionFromRotation(this.getViewXRot(partialTicks), this.getViewYRot(partialTicks));
    }

    /** Neck joint of the body chain, just behind the head (interpolated). */
    public Vec3 neckPos(float partialTicks) {
        return this.getPosition(partialTicks).add(0.0, PIVOT, 0.0).subtract(this.forward(partialTicks).scale(LeviathanBody.NECK));
    }

    private Vec3 centre() {
        BlockPos h = this.home == null ? this.blockPosition() : this.home;
        return new Vec3(h.getX() + 0.5, this.surfaceY, h.getZ() + 0.5);
    }

    /** Fully below the surface: the scales of the deep cannot be hurt. */
    public boolean isSubmerged() {
        return this.isInWater() && !Double.isNaN(this.surfaceY) && this.getBoundingBox().maxY < this.surfaceY - 0.15;
    }

    public int getState() {
        return this.entityData.get(DATA_STATE);
    }

    public int getPhase() {
        return this.entityData.get(DATA_PHASE);
    }

    public float getJaw(float partialTicks) {
        return Mth.lerp(partialTicks, this.jawO, this.jaw);
    }

    private void setJaw(int percent) {
        this.entityData.set(DATA_JAW, percent);
    }

    private void scanArena(ServerLevel level) {
        if (this.home == null) {
            this.home = this.blockPosition();
        }
        BlockPos.MutableBlockPos p = this.home.mutable();
        if (level.getFluidState(p).is(FluidTags.WATER)) {
            int n = 0;
            while (n++ < 32 && level.getFluidState(p.above()).is(FluidTags.WATER)) {
                p.move(0, 1, 0);
            }
            this.surfaceY = p.getY() + 1.0;
            p.set(this.home);
            n = 0;
            while (n++ < 32 && level.getFluidState(p.below()).is(FluidTags.WATER)) {
                p.move(0, -1, 0);
            }
            this.bottomY = p.getY();
            double best = 14.0;
            int y = (int) Math.floor(this.surfaceY - 1.0);
            for (int a = 0; a < 16; a++) {
                double ang = a * Math.PI / 8.0;
                double r = 1.0;
                while (r < 14.0) {
                    BlockPos q = BlockPos.containing(this.home.getX() + 0.5 + Math.cos(ang) * r, y, this.home.getZ() + 0.5 + Math.sin(ang) * r);
                    if (!level.getFluidState(q).is(FluidTags.WATER)) {
                        break;
                    }
                    r += 0.5;
                }
                best = Math.min(best, r - 0.5);
            }
            this.poolRadius = Mth.clamp(best, 2.0, 12.0);
        } else {
            this.surfaceY = this.home.getY() + 4.0;
            this.bottomY = this.home.getY() - 2.0;
            this.poolRadius = 6.0;
        }
    }

    // ================================================================================================ tick
    @Override
    public void tick() {
        super.tick();
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.getYRot();
        if (this.level().isClientSide()) {
            this.clientStateTicks++;
            this.jawO = this.jaw;
            this.jaw += (this.entityData.get(DATA_JAW) / 100.0F - this.jaw) * 0.35F;
            this.body.tick(this);
            this.clientEffects();
        }
    }

    private void clientEffects() {
        if (this.isDeadOrDying()) {
            return;
        }
        if (this.random.nextInt(3) == 0) {
            Vec3 p = this.body.isInitialised() ? this.body.joint(1 + this.random.nextInt(LeviathanBody.JOINTS - 1), 1.0F) : this.pivot();
            this.level().addParticle(this.isInWater() ? ParticleTypes.GLOW : ParticleTypes.END_ROD,
                    p.x + this.random.nextGaussian() * 0.5, p.y + this.random.nextGaussian() * 0.4, p.z + this.random.nextGaussian() * 0.5,
                    0.0, 0.01, 0.0);
        }
        int s = this.getState();
        if (s == S_STRANDED && this.random.nextInt(2) == 0) {
            Vec3 p = this.pivot();
            this.level().addParticle(ParticleTypes.FALLING_WATER, p.x + this.random.nextGaussian(), p.y + 0.6, p.z + this.random.nextGaussian(),
                    0.0, 0.0, 0.0);
        }
    }

    @Override
    protected void customServerAiStep() {
        ServerLevel level = (ServerLevel) this.level();
        super.customServerAiStep();
        if (Double.isNaN(this.surfaceY)) {
            this.scanArena(level);
            if (this.state == S_INTRO) {
                this.setState(S_INTRO, 70);
            }
        }
        if (this.tickCount % 10 == 0) {
            this.updateBossBar(level);
        }
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        if (this.biteCooldown > 0) {
            this.biteCooldown--;
        }
        this.updatePhase(level);

        List<Player> players = this.players(level);
        if (players.isEmpty()) {
            if (++this.noPlayerTicks > 600 && this.getHealth() < this.getMaxHealth()) {
                // nobody fights: the deep heals it (lets a group retry)
                this.setHealth(this.getMaxHealth());
                this.entityData.set(DATA_PHASE, 1);
                this.pendingRoar = false;
            }
        } else {
            this.noPlayerTicks = 0;
        }

        // arena guard rails: never stuck on land, never far from home
        if (this.isInWater()) {
            this.outOfWaterTicks = 0;
        } else {
            this.outOfWaterTicks++;
        }
        double fromHome = this.home == null ? 0.0 : Math.sqrt(this.home.distToCenterSqr(this.getX(), this.home.getY() + 0.5, this.getZ()));
        if (this.outOfWaterTicks > 600 || fromHome > LEASH + 10.0) {
            this.teleportHome(level);
        } else if ((this.outOfWaterTicks > 160 || fromHome > LEASH) && this.state != S_LUNGE_LEAP && this.state != S_LEAP_BACK) {
            this.startLeapBack(level);
        }

        switch (this.state) {
            case S_INTRO -> this.tickIntro(level);
            case S_CIRCLE -> this.tickCircle(level, players);
            case S_RISE_SPIT -> this.tickRiseSpit(level, players);
            case S_LUNGE_AIM -> this.tickLungeAim(level, players);
            case S_LUNGE_LEAP -> this.tickLungeLeap(level);
            case S_STRANDED -> this.tickStranded(level);
            case S_RETURN -> this.tickReturn(level);
            case S_TIDAL_ROAR -> this.tickTidalRoar(level, players);
            case S_WHIRLPOOL -> this.tickWhirlpool(level, players);
            case S_LEAP_BACK -> this.tickLeapBack(level);
            default -> this.setState(S_CIRCLE, 40);
        }
        this.stateTicks++;
        this.contactBite(level);
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.getYRot();
    }

    private void setState(int newState, int length) {
        this.state = newState;
        this.stateTicks = 0;
        this.stateLength = length;
        this.surfaced = false;
        this.entityData.set(DATA_STATE, newState);
    }

    private float speedMul() {
        return switch (this.getPhase()) {
            case 3 -> 1.35F;
            case 2 -> 1.15F;
            default -> 1.0F;
        };
    }

    private void updatePhase(ServerLevel level) {
        float hp = this.getHealth() / this.getMaxHealth();
        int phase = hp <= 0.25F ? 3 : hp <= 0.5F ? 2 : 1;
        if (phase > this.getPhase()) {
            this.entityData.set(DATA_PHASE, phase);
            this.pendingRoar = true;
            this.playSound(BossSounds.LEVIATHAN_ROAR, 3.0F, 0.8F);
            level.sendParticles(ParticleTypes.GLOW, this.getX(), this.getY() + PIVOT, this.getZ(), 60, 1.5, 1.0, 1.5, 0.2);
        }
    }

    private List<Player> players(ServerLevel level) {
        Vec3 c = this.home == null ? this.position() : Vec3.atCenterOf(this.home);
        AABB box = new AABB(c, c).inflate(ARENA, 16.0, ARENA);
        return level.getEntitiesOfClass(Player.class, box, p -> p.isAlive() && !p.isSpectator() && !p.isCreative());
    }

    private void updateBossBar(ServerLevel level) {
        Vec3 c = this.home == null ? this.position() : Vec3.atCenterOf(this.home);
        List<ServerPlayer> inRange = new ArrayList<>();
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(c) < 48.0 * 48.0) {
                inRange.add(p);
            }
        }
        for (ServerPlayer p : new ArrayList<>(this.bossEvent.getPlayers())) {
            if (!inRange.contains(p)) {
                this.bossEvent.removePlayer(p);
            }
        }
        for (ServerPlayer p : inRange) {
            this.bossEvent.addPlayer(p);
        }
    }

    // ================================================================================================ movement
    /** Steers the head pivot towards dest (in water: no gravity). */
    private void swimTo(Vec3 dest, double maxSpeed, double accel) {
        Vec3 d = dest.subtract(this.pivot());
        double dist = d.length();
        Vec3 desired = dist < 1.0E-3 ? Vec3.ZERO : d.scale(Math.min(maxSpeed, dist * 0.25) / dist);
        Vec3 v = this.getDeltaMovement();
        this.setDeltaMovement(v.add(desired.subtract(v).scale(accel)));
    }

    private void faceDirection(Vec3 dir, float maxYaw, float maxPitch) {
        if (dir.lengthSqr() < 1.0E-5) {
            return;
        }
        double h = dir.horizontalDistance();
        float yaw = (float) (Mth.atan2(dir.z, dir.x) * Mth.RAD_TO_DEG) - 90.0F;
        float pitch = (float) -(Mth.atan2(dir.y, h) * Mth.RAD_TO_DEG);
        if (h > 0.02) {
            this.setYRot(Mth.approachDegrees(this.getYRot(), yaw, maxYaw));
        }
        this.setXRot(Mth.approach(this.getXRot(), Mth.clamp(pitch, -75.0F, 75.0F), maxPitch));
    }

    private void faceMotion() {
        this.faceDirection(this.getDeltaMovement(), 9.0F, 6.0F);
    }

    private void faceTarget(Entity target, float maxYaw) {
        this.faceDirection(target.getEyePosition().subtract(this.pivot()), maxYaw, 5.0F);
    }

    private void airPhysics(double friction) {
        Vec3 v = this.getDeltaMovement();
        if (this.onGround()) {
            v = new Vec3(v.x * friction, Math.max(v.y, 0.0) - GRAVITY, v.z * friction);
        } else {
            v = new Vec3(v.x * 0.98, v.y - GRAVITY, v.z * 0.98);
        }
        this.setDeltaMovement(v);
    }

    private Vec3 circlePoint(float angle, double radius, double depth) {
        Vec3 c = this.centre();
        return new Vec3(c.x + Mth.cos(angle) * radius, this.surfaceY - depth, c.z + Mth.sin(angle) * radius);
    }

    private double cruiseRadius() {
        return Mth.clamp(this.poolRadius - 3.2, 1.2, 4.0);
    }

    private double cruiseDepth() {
        return Math.min(3.6, Math.max(1.6, (this.surfaceY - this.bottomY) * 0.5));
    }

    /** Point on the pool rim side towards p (horizontal), at radius r from the centre. */
    private Vec3 poolPointTowards(Vec3 p, double r, double depth) {
        Vec3 c = this.centre();
        Vec3 d = new Vec3(p.x - c.x, 0.0, p.z - c.z);
        d = d.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : d.normalize();
        return new Vec3(c.x + d.x * r, this.surfaceY - depth, c.z + d.z * r);
    }

    private void checkSurfacing(ServerLevel level) {
        Vec3 pv = this.pivot();
        if (!this.surfaced && pv.y > this.surfaceY - 0.6 && this.getDeltaMovement().y > 0.0) {
            this.surfaced = true;
            this.playSound(BossSounds.LEVIATHAN_EMERGE, 3.0F, 0.9F + this.random.nextFloat() * 0.2F);
            this.splash(level, pv.x, pv.z, 2.2, 70);
        }
    }

    private void splash(ServerLevel level, double x, double z, double spread, int count) {
        level.sendParticles(ParticleTypes.SPLASH, x, this.surfaceY + 0.1, z, count, spread, 0.4, spread, 0.6);
        level.sendParticles(ParticleTypes.BUBBLE, x, this.surfaceY - 0.6, z, count / 2, spread, 0.5, spread, 0.15);
        level.sendParticles(ParticleTypes.CLOUD, x, this.surfaceY + 0.3, z, count / 6, spread * 0.6, 0.2, spread * 0.6, 0.05);
    }

    // ================================================================================================ states
    private void tickIntro(ServerLevel level) {
        // rises from the depths, roars once, then the fight begins
        Vec3 c = this.centre();
        if (this.stateTicks < 34) {
            this.swimTo(new Vec3(c.x, this.surfaceY + 0.9, c.z), 0.22, 0.12);
            this.faceDirection(new Vec3(0.0, 1.0, 0.0).add(this.forward(1.0F).scale(0.3)), 6.0F, 4.0F);
            if (this.stateTicks % 4 == 0) {
                level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, c.x, this.surfaceY - 1.0, c.z, 20, 2.0, 1.0, 2.0, 0.1);
            }
            this.checkSurfacing(level);
        } else {
            this.swimTo(new Vec3(c.x, this.surfaceY + 0.9, c.z), 0.05, 0.2);
            Player p = level.getNearestPlayer(this, ARENA);
            if (p != null) {
                this.faceTarget(p, 6.0F);
            } else {
                this.setXRot(Mth.approach(this.getXRot(), -20.0F, 3.0F));
            }
            if (this.stateTicks == 36) {
                this.setJaw(100);
                this.playSound(BossSounds.LEVIATHAN_ROAR, 4.0F, 0.85F);
                this.shockRing(level, 2.0, 0);
            }
            if (this.stateTicks == 64) {
                this.setJaw(0);
            }
        }
        if (this.stateTicks >= this.stateLength) {
            this.setState(S_CIRCLE, 50);
        }
    }

    private void tickCircle(ServerLevel level, List<Player> players) {
        double r = this.cruiseRadius();
        double speed = 0.34 * this.speedMul();
        this.circleAngle += (float) (this.circleDir * speed / Math.max(r, 1.0));
        double depth = this.cruiseDepth() + Mth.sin(this.tickCount * 0.09F) * 0.5;
        this.swimTo(this.circlePoint(this.circleAngle, r, depth), speed, 0.14);
        this.faceMotion();
        if (this.getState() == S_CIRCLE && this.entityData.get(DATA_JAW) > 0 && this.biteCooldown < 20) {
            this.setJaw(0);
        }
        if (this.stateTicks >= this.stateLength) {
            if (players.isEmpty()) {
                this.setState(S_CIRCLE, 60);
                if (this.random.nextInt(4) == 0) {
                    this.circleDir = -this.circleDir;
                }
            } else {
                this.chooseAttack(level, players);
            }
        }
    }

    private int circleLength() {
        return this.getPhase() == 3 ? 22 + this.random.nextInt(18) : 40 + this.random.nextInt(41);
    }

    private void endAttack(ServerLevel level) {
        this.setJaw(0);
        List<Player> players = this.players(level);
        if (this.getPhase() == 3 && !players.isEmpty() && this.random.nextFloat() < 0.6F) {
            this.chooseAttack(level, players);
        } else {
            this.setState(S_CIRCLE, this.circleLength());
            if (this.random.nextInt(3) == 0) {
                this.circleDir = -this.circleDir;
            }
        }
    }

    private @Nullable Player lungeTarget(List<Player> players) {
        Vec3 c = this.centre();
        Player best = null;
        double bestD = Double.MAX_VALUE;
        for (Player p : players) {
            double h = Math.sqrt(Mth.square(p.getX() - c.x) + Mth.square(p.getZ() - c.z));
            if (!p.isInWater() && h > this.poolRadius + 1.0 && h < 21.0 && Math.abs(p.getY() - this.surfaceY) < 5.0) {
                double d = p.distanceToSqr(this);
                if (d < bestD) {
                    bestD = d;
                    best = p;
                }
            }
        }
        return best;
    }

    private void chooseAttack(ServerLevel level, List<Player> players) {
        Player lunge = this.lungeTarget(players);
        int phase = this.getPhase();
        int next;
        if (this.pendingRoar) {
            this.pendingRoar = false;
            next = S_TIDAL_ROAR;
        } else {
            int[] options = {S_RISE_SPIT, S_LUNGE_AIM, S_TIDAL_ROAR, S_WHIRLPOOL};
            int[] weights = new int[4];
            weights[0] = 3;
            weights[1] = lunge != null ? 3 : 0;
            if (phase >= 2) {
                weights[2] = this.lastAttack == S_TIDAL_ROAR || this.countTidelings(level) >= MAX_TIDELINGS - 1 ? 0 : 2;
                Vec3 c = this.centre();
                boolean near = players.stream().anyMatch(p -> p.distanceToSqr(c.x, p.getY(), c.z) < 13.0 * 13.0);
                weights[3] = this.lastAttack == S_WHIRLPOOL || !near ? 0 : 2;
            }
            for (int i = 0; i < 4; i++) {
                if (options[i] == this.lastAttack && this.repeatCount >= 1) {
                    weights[i] = Math.min(weights[i], 1);
                }
            }
            int total = 0;
            for (int w : weights) {
                total += w;
            }
            next = S_RISE_SPIT;
            if (total > 0) {
                int roll = this.random.nextInt(total);
                for (int i = 0; i < 4; i++) {
                    roll -= weights[i];
                    if (roll < 0) {
                        next = options[i];
                        break;
                    }
                }
            }
        }
        this.repeatCount = next == this.lastAttack ? this.repeatCount + 1 : 0;
        this.lastAttack = next;
        Player target = next == S_LUNGE_AIM ? lunge : players.get(this.random.nextInt(players.size()));
        this.setTarget(target);
        boolean fast = phase == 3;
        switch (next) {
            case S_RISE_SPIT -> {
                this.shotsLeft = fast ? 5 : 3 + this.random.nextInt(3);
                this.setState(S_RISE_SPIT, 30);
                this.playSound(BossSounds.LEVIATHAN_CHARGE, 2.0F, 0.7F);
            }
            case S_LUNGE_AIM -> {
                this.setState(S_LUNGE_AIM, fast ? 22 : 32);
                this.playSound(BossSounds.LEVIATHAN_AMBIENT, 3.0F, 0.6F);
            }
            case S_TIDAL_ROAR -> this.setState(S_TIDAL_ROAR, 100);
            case S_WHIRLPOOL -> {
                this.setState(S_WHIRLPOOL, 115);
                this.playSound(BossSounds.LEVIATHAN_WHIRLPOOL, 3.0F, 0.8F);
            }
            default -> this.setState(S_CIRCLE, 40);
        }
    }

    // ------------------------------------------------------------------------------------------- RISE_SPIT
    private void tickRiseSpit(ServerLevel level, List<Player> players) {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || players.isEmpty()) {
            this.endAttack(level);
            return;
        }
        Vec3 rise = this.poolPointTowards(target.position(), Math.min(this.cruiseRadius(), 2.5), -0.9);
        int t = this.stateTicks;
        if (t < 30) {
            this.swimTo(rise, 0.3 * this.speedMul(), 0.16);
            if (t < 12) {
                this.faceMotion();
                if (t % 3 == 0) {
                    level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, rise.x, this.surfaceY - 0.8, rise.z, 12, 1.0, 0.5, 1.0, 0.1);
                }
            } else {
                this.faceTarget(target, 8.0F);
            }
            this.checkSurfacing(level);
            return;
        }
        this.swimTo(rise, 0.08, 0.3);
        this.faceTarget(target, 10.0F);
        int cycle = this.getPhase() == 3 ? 13 : 19;
        int ct = (t - 30) % cycle;
        if (this.shotsLeft > 0) {
            Vec3 mouth = this.mouthPos();
            if (ct == 0) {
                this.setJaw(55);
                this.playSound(BossSounds.LEVIATHAN_CHARGE, 1.5F, 1.2F + this.random.nextFloat() * 0.2F);
            }
            if (ct < cycle - 5) {
                level.sendParticles(new DustParticleOptions(net.minecraft.world.phys.Vec3.fromRGB24(TEAL).toVector3f(), 1.6F), mouth.x, mouth.y, mouth.z, 4, 0.5, 0.4, 0.5, 0.0);
                level.sendParticles(ParticleTypes.GLOW, mouth.x, mouth.y, mouth.z, 2, 0.3, 0.3, 0.3, 0.02);
            }
            if (ct == cycle - 5) {
                this.setJaw(100);
                this.spitBolt(level, target, mouth);
                this.shotsLeft--;
            }
            if (ct == cycle - 2) {
                this.setJaw(20);
            }
        } else if (ct > 10) {
            this.endAttack(level);
        }
    }

    private Vec3 mouthPos() {
        return this.pivot().add(this.forward(1.0F).scale(1.9)).add(0.0, -0.25, 0.0);
    }

    private void spitBolt(ServerLevel level, LivingEntity target, Vec3 mouth) {
        BrineBoltEntity bolt = new BrineBoltEntity(level, this, mouth);
        double speed = this.getPhase() == 3 ? 1.25 : 1.05;
        Vec3 aimAt = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
        double dist = aimAt.distanceTo(mouth);
        double time = dist / speed;
        Vec3 lead = target.getDeltaMovement().multiply(1.0, 0.0, 1.0).scale(time * 0.6);
        aimAt = aimAt.add(lead).add(0.0, 0.5 * BrineBoltEntity.GRAVITY * time * time, 0.0);
        Vec3 d = aimAt.subtract(mouth);
        bolt.shoot(d.x, d.y, d.z, (float) speed, this.getPhase() == 3 ? 1.0F : 2.5F);
        level.addFreshEntity(bolt);
        this.playSound(BossSounds.LEVIATHAN_SPIT, 2.5F, 0.8F + this.random.nextFloat() * 0.2F);
        level.sendParticles(ParticleTypes.SPLASH, mouth.x, mouth.y, mouth.z, 20, 0.4, 0.3, 0.4, 0.3);
    }

    // ------------------------------------------------------------------------------------------- LUNGE
    private void tickLungeAim(ServerLevel level, List<Player> players) {
        LivingEntity target = this.getTarget();
        int lockAt = this.stateLength - 14;
        if (target == null || !target.isAlive()) {
            this.endAttack(level);
            return;
        }
        if (this.stateTicks <= lockAt) {
            this.aim = new Vec3(target.getX(), target.getY(), target.getZ());
            Vec3 c = this.centre();
            Vec3 flat = new Vec3(this.aim.x - c.x, 0.0, this.aim.z - c.z);
            double h = flat.length();
            double maxR = 19.0;
            if (h > maxR) {
                this.aim = new Vec3(c.x + flat.x / h * maxR, this.aim.y, c.z + flat.z / h * maxR);
            }
        }
        Vec3 launch = this.poolPointTowards(this.aim, Math.max(0.5, this.poolRadius - 2.4), 2.4);
        this.swimTo(launch, 0.36 * this.speedMul(), 0.2);
        this.faceDirection(this.aim.subtract(this.pivot()), 10.0F, 5.0F);
        if (this.stateTicks % 2 == 0) {
            level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, launch.x, this.surfaceY - 0.6, launch.z, 10, 1.2, 0.3, 1.2, 0.05);
        }
        if (this.stateTicks >= lockAt - 6) {
            // the landing zone glows on the floor: get out of the circle!
            for (int i = 0; i < 16; i++) {
                double a = i * Math.PI / 8.0 + this.stateTicks * 0.15;
                level.sendParticles(new DustParticleOptions(net.minecraft.world.phys.Vec3.fromRGB24(0xFF5A3C).toVector3f(), 1.8F), this.aim.x + Math.cos(a) * 4.0, this.aim.y + 0.15,
                        this.aim.z + Math.sin(a) * 4.0, 1, 0.05, 0.0, 0.05, 0.0);
            }
            level.sendParticles(ParticleTypes.FALLING_WATER, this.aim.x, this.aim.y + 0.3, this.aim.z, 6, 1.5, 0.1, 1.5, 0.0);
        }
        if (this.stateTicks >= this.stateLength) {
            this.launchTowards(level, this.aim, 4.0);
            this.setState(S_LUNGE_LEAP, 70);
            this.setJaw(80);
            this.playSound(BossSounds.LEVIATHAN_ROAR, 3.0F, 1.1F);
        }
    }

    /** Ballistic jump of the entity position onto `land` with an apex `lift` blocks above the higher end. */
    private void launchTowards(ServerLevel level, Vec3 land, double lift) {
        Vec3 from = this.position();
        double apex = Math.max(from.y, land.y) + lift + from.distanceTo(land) * 0.08;
        double vy = Math.sqrt(2.0 * GRAVITY * Math.max(0.5, apex - from.y));
        double tUp = vy / GRAVITY;
        double tDown = Math.sqrt(2.0 * Math.max(0.2, apex - land.y) / GRAVITY);
        double time = tUp + tDown;
        Vec3 flat = new Vec3(land.x - from.x, 0.0, land.z - from.z).scale(1.0 / time);
        this.setDeltaMovement(flat.x, vy, flat.z);
        this.playSound(BossSounds.LEVIATHAN_EMERGE, 3.0F, 0.8F);
        Vec3 pv = this.pivot();
        this.splash(level, pv.x, pv.z, 2.0, 90);
    }

    private void tickLungeLeap(ServerLevel level) {
        Vec3 v = this.getDeltaMovement();
        this.setDeltaMovement(v.x, v.y - GRAVITY, v.z);
        this.faceDirection(this.getDeltaMovement(), 12.0F, 8.0F);
        if (this.stateTicks % 2 == 0) {
            Vec3 pv = this.pivot();
            level.sendParticles(ParticleTypes.FALLING_WATER, pv.x, pv.y - 0.5, pv.z, 6, 1.0, 0.6, 1.0, 0.0);
        }
        boolean landed = this.stateTicks > 3 && (this.onGround() || (this.horizontalCollision && v.y <= 0.0));
        boolean backInPool = this.stateTicks > 8 && this.isInWater() && v.y < 0.0;
        if (backInPool) {
            this.splash(level, this.getX(), this.getZ(), 2.0, 60);
            this.playSound(BossSounds.LEVIATHAN_DIVE, 3.0F, 0.9F);
            this.endAttack(level);
        } else if (landed || this.stateTicks > this.stateLength) {
            this.slam(level);
            this.setState(S_STRANDED, 80);
            this.setJaw(35);
        }
    }

    private void slam(ServerLevel level) {
        Vec3 c = this.position();
        this.playSound(BossSounds.LEVIATHAN_SLAM, 4.0F, 0.8F);
        level.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.5, c.z, 4, 1.5, 0.3, 1.5, 0.0);
        BlockState floor = level.getBlockState(BlockPos.containing(c.x, c.y - 0.5, c.z));
        if (!floor.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, floor), c.x, c.y + 0.2, c.z, 90, 2.5, 0.2, 2.5, 0.3);
        }
        level.sendParticles(ParticleTypes.SPLASH, c.x, c.y + 0.5, c.z, 80, 2.5, 0.4, 2.5, 0.5);
        this.shockRing(level, 1.0, 1);
        AABB box = this.getBoundingBox().inflate(4.0, 1.5, 4.0);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != this && e.isAlive() && !(e instanceof TidelingEntity))) {
            double dx = e.getX() - c.x;
            double dz = e.getZ() - c.z;
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d > 5.5) {
                continue;
            }
            e.hurt(this.damageSources().mobAttack(this), 8.0F);
            Vec3 push = d < 0.01 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(dx / d, 0.0, dz / d);
            e.push(push.x * 1.3, 0.55, push.z * 1.3);
            e.hurtMarked = true;
        }
        this.setDeltaMovement(0.0, Math.min(0.0, this.getDeltaMovement().y), 0.0);
    }

    private void tickStranded(ServerLevel level) {
        this.airPhysics(0.5);
        if (this.isInWater()) {
            this.setState(S_RETURN, 40);
            return;
        }
        float tilt = Mth.sin(this.stateTicks * 0.5F) * 6.0F;
        this.setXRot(Mth.approach(this.getXRot(), 8.0F + tilt, 4.0F));
        if (this.stateTicks % 20 == 10) {
            this.playSound(BossSounds.LEVIATHAN_FLOP, 2.5F, 0.8F + this.random.nextFloat() * 0.2F);
            this.setJaw(this.random.nextBoolean() ? 50 : 25);
            level.sendParticles(ParticleTypes.SPLASH, this.getX(), this.getY() + 0.3, this.getZ(), 30, 1.5, 0.2, 1.5, 0.3);
        }
        if (this.stateTicks >= this.stateLength) {
            this.setJaw(10);
            this.setState(S_RETURN, 120);
        }
    }

    private void tickReturn(ServerLevel level) {
        Vec3 c = this.centre();
        if (this.isInWater() && this.getY() < this.surfaceY - 0.6) {
            this.playSound(BossSounds.LEVIATHAN_DIVE, 3.0F, 1.0F);
            this.splash(level, this.getX(), this.getZ(), 1.6, 50);
            this.endAttack(level);
            return;
        }
        if (this.isInWater()) {
            // over the water: dive in
            this.swimTo(new Vec3(c.x, this.surfaceY - this.cruiseDepth(), c.z), 0.3, 0.2);
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.05, 0.0));
            this.faceMotion();
            return;
        }
        Vec3 flat = new Vec3(c.x - this.getX(), 0.0, c.z - this.getZ());
        double h = flat.length();
        Vec3 dir = h < 1.0E-3 ? Vec3.ZERO : flat.scale(1.0 / h);
        double speed = 0.2 * this.speedMul();
        Vec3 v = this.getDeltaMovement();
        // slither: a sideways wiggle on top of the crawl
        Vec3 side = new Vec3(-dir.z, 0.0, dir.x).scale(Mth.sin(this.stateTicks * 0.45F) * 0.08);
        Vec3 want = dir.scale(speed).add(side);
        this.setDeltaMovement(v.x + (want.x - v.x) * 0.3, v.y, v.z + (want.z - v.z) * 0.3);
        this.airPhysics(1.0);
        this.faceDirection(new Vec3(dir.x, -0.15, dir.z), 7.0F, 3.0F);
        if (this.horizontalCollision) {
            this.stuckTicks++;
        } else {
            this.stuckTicks = Math.max(0, this.stuckTicks - 1);
        }
        if (this.stateTicks % 12 == 0) {
            this.playSound(BossSounds.LEVIATHAN_FLOP, 1.5F, 1.0F);
        }
        if (this.stuckTicks > 20 || this.stateTicks >= this.stateLength) {
            this.startLeapBack(level);
        }
    }

    private void startLeapBack(ServerLevel level) {
        Vec3 c = this.centre();
        this.stuckTicks = 0;
        this.setState(S_LEAP_BACK, 80);
        this.setJaw(40);
        this.launchTowards(level, new Vec3(c.x, this.surfaceY - 2.0, c.z), 3.0);
    }

    private void tickLeapBack(ServerLevel level) {
        if (this.isInWater() && this.stateTicks > 4) {
            this.playSound(BossSounds.LEVIATHAN_DIVE, 3.0F, 0.9F);
            this.splash(level, this.getX(), this.getZ(), 2.0, 70);
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.3, 0.3, 0.3));
            this.endAttack(level);
            return;
        }
        Vec3 v = this.getDeltaMovement();
        this.setDeltaMovement(v.x, v.y - GRAVITY, v.z);
        this.faceDirection(this.getDeltaMovement(), 12.0F, 8.0F);
        if (this.onGround() && this.stateTicks > 6 || this.stateTicks > this.stateLength) {
            // landed on stone again: try once more from here
            this.startLeapBack(level);
        }
    }

    private void teleportHome(ServerLevel level) {
        Vec3 c = this.centre();
        level.sendParticles(ParticleTypes.GLOW, this.getX(), this.getY() + 1.0, this.getZ(), 60, 1.5, 1.0, 1.5, 0.1);
        this.setPos(c.x, this.surfaceY - this.cruiseDepth() - PIVOT, c.z);
        this.setDeltaMovement(Vec3.ZERO);
        this.outOfWaterTicks = 0;
        this.splash(level, c.x, c.z, 2.0, 60);
        this.setState(S_CIRCLE, 40);
    }

    // ------------------------------------------------------------------------------------------- TIDAL_ROAR
    private void tickTidalRoar(ServerLevel level, List<Player> players) {
        Vec3 c = this.centre();
        Vec3 up = new Vec3(c.x, this.surfaceY + 1.3, c.z);
        int t = this.stateTicks;
        LivingEntity target = this.getTarget();
        if (t < 26) {
            this.swimTo(up, 0.3 * this.speedMul(), 0.18);
            if (target != null) {
                this.faceTarget(target, 8.0F);
            } else {
                this.faceMotion();
            }
            this.checkSurfacing(level);
            return;
        }
        this.swimTo(up, 0.06, 0.3);
        if (t < 46) {
            // rears back, water streams up around it
            this.setXRot(Mth.approach(this.getXRot(), -38.0F, 4.0F));
            if (t == 26) {
                this.setJaw(45);
                this.playSound(BossSounds.LEVIATHAN_CHARGE, 3.0F, 0.6F);
            }
            for (int i = 0; i < 3; i++) {
                double a = this.random.nextDouble() * Math.PI * 2.0;
                double r = 2.0 + this.random.nextDouble() * (this.poolRadius - 2.0);
                level.sendParticles(ParticleTypes.SPLASH, c.x + Math.cos(a) * r, this.surfaceY + 0.2, c.z + Math.sin(a) * r, 4, 0.2, 0.6, 0.2, 0.2);
            }
            return;
        }
        if (t == 46) {
            this.setJaw(100);
            this.playSound(BossSounds.LEVIATHAN_ROAR, 5.0F, this.getPhase() == 3 ? 0.95F : 0.8F);
            this.shockRing(level, 3.0, 2);
            Vec3 pv = this.pivot();
            for (Player p : players) {
                Vec3 d = new Vec3(p.getX() - pv.x, 0.0, p.getZ() - pv.z);
                double dist = d.length();
                if (dist > 18.0) {
                    continue;
                }
                Vec3 n = dist < 0.01 ? new Vec3(1.0, 0.0, 0.0) : d.scale(1.0 / dist);
                double s = 0.6 + 1.4 * (1.0 - dist / 18.0);
                p.push(n.x * s, 0.45, n.z * s);
                p.hurtMarked = true;
                if (dist < 9.0) {
                    p.hurt(this.damageSources().mobAttack(this), this.getPhase() == 3 ? 4.0F : 2.0F);
                }
            }
            this.summonTidelings(level, players);
        }
        if (t > 46 && t < 76) {
            float shake = Mth.sin(t * 1.3F) * 8.0F;
            this.setYRot(this.getYRot() + shake * 0.3F);
            this.setXRot(Mth.approach(this.getXRot(), -15.0F, 3.0F));
            if (t % 3 == 0) {
                Vec3 m = this.mouthPos();
                level.sendParticles(ParticleTypes.CLOUD, m.x, m.y, m.z, 3, 0.3, 0.3, 0.3, 0.15);
            }
        }
        if (t == 76) {
            this.setJaw(0);
        }
        if (t >= 78) {
            this.swimTo(new Vec3(c.x, this.surfaceY - this.cruiseDepth(), c.z), 0.3, 0.2);
            this.faceMotion();
        }
        if (t >= this.stateLength) {
            this.endAttack(level);
        }
    }

    /** Expanding ring of water and spray, kind 0 = intro, 1 = slam, 2 = roar. */
    private void shockRing(ServerLevel level, double strength, int kind) {
        Vec3 o = kind == 1 ? this.position().add(0.0, 0.3, 0.0) : new Vec3(this.getX(), this.surfaceY + 0.2, this.getZ());
        int n = 48;
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2.0 / n;
            double vx = Math.cos(a) * 0.6 * strength;
            double vz = Math.sin(a) * 0.6 * strength;
            level.sendParticles(ParticleTypes.CLOUD, o.x + Math.cos(a) * 1.5, o.y, o.z + Math.sin(a) * 1.5, 0, vx, 0.02, vz, 1.0);
            level.sendParticles(ParticleTypes.SPLASH, o.x + Math.cos(a) * 2.0, o.y, o.z + Math.sin(a) * 2.0, 0, vx * 1.5, 0.3, vz * 1.5, 1.0);
        }
        if (kind == 2) {
            level.sendParticles(ParticleTypes.FLASH, o.x, o.y + 1.0, o.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private int countTidelings(ServerLevel level) {
        Vec3 c = this.home == null ? this.position() : Vec3.atCenterOf(this.home);
        return level.getEntitiesOfClass(TidelingEntity.class, new AABB(c, c).inflate(ARENA + 8.0), Entity::isAlive).size();
    }

    private void summonTidelings(ServerLevel level, List<Player> players) {
        int count = Math.min(this.getPhase() == 3 ? 4 : 3 + this.random.nextInt(2), MAX_TIDELINGS - this.countTidelings(level));
        Vec3 c = this.centre();
        for (int i = 0; i < count; i++) {
            Player p = players.isEmpty() ? null : players.get(i % players.size());
            double baseA = p == null ? this.random.nextDouble() * Math.PI * 2.0 : Math.atan2(p.getZ() - c.z, p.getX() - c.x);
            double a = baseA + (i - (count - 1) / 2.0) * 0.55;
            double r = Math.max(1.0, this.poolRadius - 1.5);
            double x = c.x + Math.cos(a) * r;
            double z = c.z + Math.sin(a) * r;
            TidelingEntity t = BossEntities.TIDELING.create(level);
            if (t == null) {
                continue;
            }
            t.moveTo(x, this.surfaceY - 1.0, z, (float) (a * Mth.RAD_TO_DEG) - 90.0F, 0.0F);
            t.finalizeSpawn(level, level.getCurrentDifficultyAt(t.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            t.setSummoned();
            if (p != null) {
                t.setTarget(p);
            }
            t.setDeltaMovement(Math.cos(a) * 0.45, 0.65, Math.sin(a) * 0.45);
            level.addFreshEntity(t);
            level.sendParticles(ParticleTypes.SPLASH, x, this.surfaceY + 0.1, z, 25, 0.4, 0.3, 0.4, 0.3);
            level.sendParticles(ParticleTypes.GLOW, x, this.surfaceY, z, 8, 0.3, 0.3, 0.3, 0.05);
        }
    }

    // ------------------------------------------------------------------------------------------- WHIRLPOOL
    private void tickWhirlpool(ServerLevel level, List<Player> players) {
        Vec3 c = this.centre();
        int t = this.stateTicks;
        double r = Math.min(2.6, this.cruiseRadius());
        float spin = t < 20 ? 0.12F + t * 0.008F : (t < 85 ? 0.3F : 0.3F - (t - 85) * 0.008F);
        this.circleAngle += this.circleDir * spin * this.speedMul();
        this.swimTo(this.circlePoint(this.circleAngle, r, 1.15), 0.55 * this.speedMul(), 0.25);
        this.faceMotion();
        // the vortex on the surface
        int arms = 3;
        for (int k = 0; k < arms; k++) {
            for (int j = 0; j < 4; j++) {
                double rr = 1.0 + j * (this.poolRadius - 1.0) / 4.0 + this.random.nextDouble() * 0.6;
                double a = this.circleAngle * 1.4 + k * Math.PI * 2.0 / arms - rr * 0.45;
                double x = c.x + Math.cos(a) * rr;
                double z = c.z + Math.sin(a) * rr;
                double tx = -Math.sin(a) * this.circleDir * 0.35 - Math.cos(a) * 0.12;
                double tz = Math.cos(a) * this.circleDir * 0.35 - Math.sin(a) * 0.12;
                level.sendParticles(ParticleTypes.SPLASH, x, this.surfaceY + 0.05, z, 0, tx, 0.1, tz, 1.0);
                if (j % 2 == 0) {
                    level.sendParticles(ParticleTypes.BUBBLE, x, this.surfaceY - 0.5, z, 0, tx, -0.05, tz, 1.0);
                }
            }
        }
        if (t == 20) {
            this.playSound(BossSounds.LEVIATHAN_WHIRLPOOL, 3.0F, 0.6F);
        }
        if (t >= 20 && t < 80) {
            if (t % 20 == 0) {
                this.playSound(BossSounds.LEVIATHAN_WHIRLPOOL, 2.5F, 0.7F);
            }
            for (Player p : players) {
                Vec3 d = new Vec3(c.x - p.getX(), 0.0, c.z - p.getZ());
                double dist = d.length();
                if (dist > 12.0 || p.getY() > this.surfaceY + 6.0) {
                    continue;
                }
                Vec3 n = dist < 0.3 ? Vec3.ZERO : d.scale(1.0 / dist);
                double s = (p.isInWater() ? 0.07 : 0.055) + 0.035 * (1.0 - dist / 12.0);
                Vec3 tangent = new Vec3(-n.z, 0.0, n.x).scale(this.circleDir * (p.isInWater() ? 0.05 : 0.02));
                p.push(n.x * s + tangent.x, p.isInWater() ? -0.035 : 0.0, n.z * s + tangent.z);
                p.hurtMarked = true;
                if (t % 4 == 0 && !p.isInWater()) {
                    level.sendParticles(ParticleTypes.CLOUD, p.getX() - n.x, p.getY() + 0.2, p.getZ() - n.z, 0, n.x * 0.4, 0.0, n.z * 0.4, 1.0);
                }
            }
        }
        if (t >= this.stateLength) {
            this.endAttack(level);
        }
    }

    // ------------------------------------------------------------------------------------------- bite
    private void contactBite(ServerLevel level) {
        if (this.biteCooldown > 0 || this.state == S_INTRO || this.state == S_STRANDED || this.isDeadOrDying()) {
            return;
        }
        Vec3 mouth = this.mouthPos();
        AABB box = new AABB(mouth, mouth).inflate(1.8, 1.4, 1.8);
        for (Player p : level.getEntitiesOfClass(Player.class, box, p -> p.isAlive() && !p.isSpectator() && !p.isCreative())) {
            if (this.doHurtTarget(p)) {
                this.biteCooldown = 30;
                this.setJaw(90);
                this.playSound(BossSounds.LEVIATHAN_BITE, 2.0F, 0.9F);
                return;
            }
        }
    }

    // ================================================================================================ damage
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!(this.level() instanceof ServerLevel level)) return false;
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(source, amount);
        }
        Entity attacker = source.getEntity();
        if (attacker instanceof LeviathanEntity || attacker instanceof TidelingEntity || source.getDirectEntity() instanceof BrineBoltEntity) {
            return false;
        }
        if (this.state == S_INTRO || this.isSubmerged()) {
            if (!this.isDeadOrDying() && this.tickCount % 4 == 0) {
                this.playSound(BossSounds.LEVIATHAN_DEFLECT, 1.5F, 0.7F + this.random.nextFloat() * 0.2F);
                Vec3 pv = this.pivot();
                level.sendParticles(ParticleTypes.BUBBLE, pv.x, pv.y + 0.5, pv.z, 20, 1.0, 0.6, 1.0, 0.1);
            }
            return false;
        }
        if (this.state == S_STRANDED) {
            amount *= 1.5F;
        }
        return super.hurt(source, amount);
    }

    // ================================================================================================ death
    @Override
    protected void dropAllDeathLoot(ServerLevel level, DamageSource source) {
        // the loot comes out of the light burst at the end of the death animation
        this.deathSource = source;
        this.deathByPlayer = this.lastHurtByPlayer != null;
    }

    @Override
    protected void tickDeath() {
        this.deathTime++;
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 pv = this.pivot();
        if (this.deathTime == 1) {
            this.setJaw(70);
            this.bossEvent.setProgress(0.0F);
            Vec3 c = this.home == null ? this.position() : Vec3.atCenterOf(this.home);
            for (TidelingEntity t : level.getEntitiesOfClass(TidelingEntity.class, new AABB(c, c).inflate(ARENA + 8.0), Entity::isAlive)) {
                level.sendParticles(ParticleTypes.GLOW, t.getX(), t.getY() + 0.3, t.getZ(), 10, 0.2, 0.2, 0.2, 0.05);
                t.discard();
            }
        }
        if (this.isInWater()) {
            this.setDeltaMovement(this.getDeltaMovement().scale(0.8).add(0.0, -0.012, 0.0));
        } else {
            this.airPhysics(0.6);
        }
        this.setXRot(Mth.approach(this.getXRot(), 25.0F, 1.0F));
        if (this.deathTime % 2 == 0) {
            level.sendParticles(ParticleTypes.GLOW, pv.x, pv.y, pv.z, 6, 1.4, 0.9, 1.4, 0.04);
            level.sendParticles(ParticleTypes.END_ROD, pv.x, pv.y, pv.z, 2, 1.0, 0.8, 1.0, 0.03);
            if (this.isInWater()) {
                level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, pv.x, pv.y, pv.z, 4, 1.0, 0.5, 1.0, 0.05);
            }
        }
        if (this.deathTime == 40) {
            this.setJaw(20);
        }
        if (this.deathTime == 70) {
            // the light burst: what the deep held is set free
            this.playSound(BossSounds.LEVIATHAN_BURST, 5.0F, 1.0F);
            level.sendParticles(ParticleTypes.FLASH, pv.x, pv.y, pv.z, 2, 0.2, 0.2, 0.2, 0.0);
            level.sendParticles(ParticleTypes.END_ROD, pv.x, pv.y, pv.z, 160, 0.4, 0.4, 0.4, 0.55);
            level.sendParticles(ParticleTypes.GLOW, pv.x, pv.y, pv.z, 120, 2.0, 1.5, 2.0, 0.4);
            level.sendParticles(ParticleTypes.SPLASH, pv.x, this.surfaceY + 0.1, pv.z, 160, 3.0, 0.4, 3.0, 0.8);
        }
        if (this.deathTime == 74) {
            this.releaseRewards(level);
        }
        if (this.deathTime >= DEATH_TICKS && !this.isRemoved()) {
            level.broadcastEntityEvent(this, (byte) 60);
            this.remove(RemovalReason.KILLED);
        }
    }

    private void releaseRewards(ServerLevel level) {
        Vec3 c = Double.isNaN(this.surfaceY) ? this.pivot() : new Vec3(this.getX(), Math.max(this.getY() + PIVOT, this.surfaceY + 1.0), this.getZ());
        Player killer = this.lastHurtByPlayer instanceof Player p ? p : level.getNearestPlayer(this, 48.0);
        if (this.deathSource != null && level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
            AABB before = this.getBoundingBox().inflate(3.0);
            List<ItemEntity> old = level.getEntitiesOfClass(ItemEntity.class, before);
            this.dropFromLootTable(this.deathSource, this.deathByPlayer);
            Vec3 ring = killer != null ? killer.position() : c.add(this.poolRadius + 2.0, 0.0, 0.0);
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, before.inflate(1.0), e -> !old.contains(e))) {
                item.setPos(c.x, c.y, c.z);
                Vec3 to = new Vec3(ring.x - c.x, 0.0, ring.z - c.z);
                double h = to.length();
                double t = 22.0;
                Vec3 v = h < 0.1 ? new Vec3(0.0, 0.5, 0.0) : new Vec3(to.x / t, 0.55, to.z / t);
                item.setDeltaMovement(v.add(this.random.nextGaussian() * 0.04, this.random.nextDouble() * 0.1, this.random.nextGaussian() * 0.04));
                item.setUnlimitedLifetime();
                item.setInvulnerable(true);
            }
        }
        if (this.deathByPlayer) {
            ExperienceOrb.award(level, c, 500);
        }
    }

    @Override
    public boolean shouldShowName() {
        return false;
    }
}
