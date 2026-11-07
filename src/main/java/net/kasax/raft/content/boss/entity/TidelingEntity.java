package net.kasax.raft.content.boss.entity;

import net.kasax.raft.content.boss.BossSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.kasax.raft.compat.ValueInput;
import net.kasax.raft.compat.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Tideling (spec 10): a small fanged fish called from the pool by the Leviathan's roar. It darts at players in water
 * and hops after them on land for 10 s; then it only flops about helplessly and slowly dries out.
 */
public class TidelingEntity extends Monster {
    public static final int LAND_HUNT_TICKS = 200;
    private static final byte EVENT_BITE = 4;
    private static final EntityDataAccessor<Boolean> DATA_FLOPPING = SynchedEntityData.defineId(TidelingEntity.class,
            EntityDataSerializers.BOOLEAN);

    private int landTicks;
    private int hopCooldown;
    private int attackCooldown;
    private boolean summoned;
    /** Client: bite animation countdown. */
    public int biteAnim;

    public TidelingEntity(EntityType<? extends TidelingEntity> type, Level level) {
        super(type, level);
        this.xpReward = 3;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                // 3.2: was 10 health and 3 damage (a scratch through titanium armour)
                .add(Attributes.MAX_HEALTH, 14.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    public void setSummoned() {
        this.summoned = true;
    }

    /** Stranded too long: it only flops about (synced for the animation). */
    public boolean isFlopping() {
        return this.entityData.get(DATA_FLOPPING);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FLOPPING, false);
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    protected void updateNoActionTime() {
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        return level.getFluidState(pos).is(FluidTags.WATER) ? 10.0F : 0.0F;
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return !this.summoned && super.removeWhenFarAway(distSqr);
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageModifier, DamageSource damageSource) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return BossSounds.TIDELING_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return BossSounds.TIDELING_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return BossSounds.TIDELING_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!(this.level() instanceof ServerLevel level)) return false;
        if (source.getEntity() instanceof LeviathanEntity || source.getDirectEntity() instanceof BrineBoltEntity) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag121) {
        ValueOutput output = ValueOutput.of(tag121, this.registryAccess());
        super.addAdditionalSaveData(output.tag());
        output.putBoolean("Summoned", this.summoned);
        output.putInt("LandTicks", this.landTicks);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag121) {
        ValueInput input = ValueInput.of(tag121, this.registryAccess());
        super.readAdditionalSaveData(input.tag());
        this.summoned = input.getBooleanOr("Summoned", false);
        this.landTicks = input.getIntOr("LandTicks", 0);
    }

    
    protected void travelInWater(Vec3 input, double baseGravity, boolean isFalling, double oldY) {
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().scale(0.9));
    }

    /** 1.21.1 port: the newer game moves an entity in water through {@code travelInWater}; 1.21.1 only has travel. */
    @Override
    public void travel(Vec3 input) {
        if (this.isControlledByLocalInstance() && this.isInWater() && this.isAffectedByFluids()
                && !this.canStandOnFluid(this.level().getFluidState(this.blockPosition()))) {
            this.travelInWater(input, this.getGravity(), this.getDeltaMovement().y <= 0.0, this.getY());
            this.calculateEntityAnimation(false);
        } else {
            super.travel(input);
        }
    }

    @Override
    public void baseTick() {
        super.baseTick();
        this.setAirSupply(this.getMaxAirSupply());
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() && this.biteAnim > 0) {
            this.biteAnim--;
        }
    }

    @Override
    protected void customServerAiStep() {
        ServerLevel level = (ServerLevel) this.level();
        super.customServerAiStep();
        if (this.hopCooldown > 0) {
            this.hopCooldown--;
        }
        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }
        boolean water = this.isInWater();
        if (water) {
            this.landTicks = 0;
        } else if (this.onGround()) {
            this.landTicks++;
        }
        this.entityData.set(DATA_FLOPPING, !water && this.landTicks > LAND_HUNT_TICKS);
        LivingEntity target = this.getTarget();
        if (target != null && (!target.isAlive() || target instanceof Player p && (p.isCreative() || p.isSpectator()))) {
            this.setTarget(null);
            target = null;
        }
        if (water) {
            this.swim(target);
        } else if (this.landTicks <= LAND_HUNT_TICKS) {
            this.hop(level, target);
        } else {
            this.flop(level);
        }
        if (target != null && this.landTicks <= LAND_HUNT_TICKS && this.attackCooldown == 0
                && this.getBoundingBox().inflate(0.35).intersects(target.getBoundingBox())) {
            this.attackCooldown = 20;
            if (this.doHurtTarget(target)) {
                level.broadcastEntityEvent(this, EVENT_BITE);
                this.playSound(BossSounds.TIDELING_BITE, 1.0F, 1.0F + this.random.nextFloat() * 0.2F);
            }
        }
    }

    private void swim(LivingEntity target) {
        Vec3 v = this.getDeltaMovement();
        Vec3 want;
        if (target != null) {
            Vec3 d = target.position().add(0.0, target.getBbHeight() * 0.4, 0.0).subtract(this.position());
            double len = d.length();
            want = len < 0.01 ? Vec3.ZERO : d.scale(0.22 / len);
            // break the surface towards prey on land
            if (!target.isInWater() && len < 6.0 && !this.isUnderWater() && this.hopCooldown == 0) {
                want = want.add(0.0, 0.35, 0.0);
                this.hopCooldown = 20;
            }
        } else {
            float a = (this.tickCount + this.getId() * 37) * 0.05F;
            want = new Vec3(Mth.cos(a) * 0.05, Mth.sin(a * 0.7F) * 0.01, Mth.sin(a) * 0.05);
        }
        this.setDeltaMovement(v.add(want.subtract(v).scale(0.2)));
        this.faceMotion();
    }

    private void hop(ServerLevel level, LivingEntity target) {
        if (!this.onGround() || this.hopCooldown > 0) {
            this.faceMotion();
            return;
        }
        Vec3 dir;
        if (target != null) {
            Vec3 d = new Vec3(target.getX() - this.getX(), 0.0, target.getZ() - this.getZ());
            dir = d.lengthSqr() < 1.0E-4 ? Vec3.ZERO : d.normalize();
        } else {
            float a = this.random.nextFloat() * Mth.TWO_PI;
            dir = new Vec3(Mth.cos(a), 0.0, Mth.sin(a)).scale(0.4);
        }
        this.setDeltaMovement(dir.x * 0.38, 0.42, dir.z * 0.38);
        this.hopCooldown = 8 + this.random.nextInt(6);
        this.faceDirection(dir);
        this.flopSound(0.7F, 1.1F + this.random.nextFloat() * 0.2F);
        level.sendParticles(ParticleTypes.SPLASH, this.getX(), this.getY() + 0.1, this.getZ(), 6, 0.2, 0.1, 0.2, 0.1);
    }

    /** Game time of the last flop sound: a tideling hops every half second, a sound for every hop of every tideling
     * flooded the fight (dozens of flops in a few seconds), so one tideling flops audibly at most every 1.2 s. */
    private int lastFlopSound = -100;

    private void flopSound(float volume, float pitch) {
        if (this.tickCount - this.lastFlopSound >= 24) {
            this.lastFlopSound = this.tickCount;
            this.playSound(BossSounds.TIDELING_FLOP, volume, pitch);
        }
    }

    private void flop(ServerLevel level) {
        if (this.onGround() && this.hopCooldown == 0) {
            this.setDeltaMovement((this.random.nextFloat() * 2.0F - 1.0F) * 0.06, 0.32, (this.random.nextFloat() * 2.0F - 1.0F) * 0.06);
            this.hopCooldown = 10 + this.random.nextInt(14);
            this.flopSound(0.6F, 0.9F + this.random.nextFloat() * 0.3F);
        }
        // stranded: it slowly dries out
        if (this.landTicks > LAND_HUNT_TICKS + 200 && this.tickCount % 20 == 0) {
            this.hurt(this.damageSources().dryOut(), 1.0F);
        }
    }

    private void faceMotion() {
        Vec3 v = this.getDeltaMovement();
        if (v.horizontalDistanceSqr() > 1.0E-4) {
            this.faceDirection(v);
        }
        double h = v.horizontalDistance();
        float pitch = (float) -(Mth.atan2(v.y, Math.max(h, 0.05)) * Mth.RAD_TO_DEG);
        this.setXRot(Mth.approach(this.getXRot(), this.isInWater() ? Mth.clamp(pitch, -60.0F, 60.0F) : 0.0F, 8.0F));
    }

    private void faceDirection(Vec3 dir) {
        if (dir.horizontalDistanceSqr() < 1.0E-6) {
            return;
        }
        float yaw = (float) (Mth.atan2(dir.z, dir.x) * Mth.RAD_TO_DEG) - 90.0F;
        this.setYRot(Mth.approachDegrees(this.getYRot(), yaw, 20.0F));
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.getYRot();
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_BITE) {
            this.biteAnim = 8;
        } else {
            super.handleEntityEvent(id);
        }
    }

    /** 0..1 jaw opening of the bite (client). */
    public float getBite(float partialTicks) {
        if (this.biteAnim <= 0) {
            return 0.0F;
        }
        float t = (8.0F - (this.biteAnim - partialTicks)) / 8.0F;
        return t < 0.35F ? t / 0.35F : Math.max(0.0F, 1.0F - (t - 0.35F) / 0.65F);
    }
}
