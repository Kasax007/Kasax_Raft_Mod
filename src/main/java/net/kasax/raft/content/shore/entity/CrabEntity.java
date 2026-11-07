package net.kasax.raft.content.shore.entity;

import net.kasax.raft.Raft;
import net.kasax.raft.content.shore.ShoreEntities;
import net.kasax.raft.content.shore.ShoreSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.kasax.raft.compat.ValueInput;
import net.kasax.raft.compat.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * Beach crab: scuttles sideways along the sand, digs itself in when a player sprints close (only the eye stalks stay
 * above the sand), and pinches back when it is hit. Three shell colours; breeds with kelp.
 */
public class CrabEntity extends Animal implements CrabAnimated {
    public static final TagKey<Block> BURROWABLE = TagKey.create(Registries.BLOCK, Raft.id("crab_burrowable"));
    public static final TagKey<Block> SPAWNABLE_ON = TagKey.create(Registries.BLOCK, Raft.id("crab_spawnable_on"));
    public static final TagKey<Item> FOOD = TagKey.create(Registries.ITEM, Raft.id("crab_food"));
    public static final byte EVENT_PINCH = 80;

    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(CrabEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_HIDDEN = SynchedEntityData.defineId(CrabEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_ANGRY = SynchedEntityData.defineId(CrabEntity.class, EntityDataSerializers.BOOLEAN);

    public final AnimationState pinchAnimation = new AnimationState();
    public final AnimationState snapAnimation = new AnimationState();
    private float burrow;
    private float burrowO;
    private float sideways;
    private float sidewaysO;
    private int hideTicks;
    private int angerTicks;

    public CrabEntity(EntityType<? extends CrabEntity> type, Level level) {
        super(type, level);
        this.setPathfindingMalus(PathType.WATER, 2.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return net.minecraft.world.entity.Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.FOLLOW_RANGE, 12.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new HideGoal());
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.6) {
            @Override
            public boolean canUse() {
                return CrabEntity.this.isBaby() && super.canUse();
            }
        });
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.5, true));
        // Not on sand: no place to hide, so it scuttles away from sprinting players instead.
        this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Player.class, 7.0F, 1.3, 1.6,
                e -> e instanceof Player p && p.isSprinting() && !p.isSpectator() && this.getTarget() == null && !this.canBurrowHere()));
        this.goalSelector.addGoal(4, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(5, new TemptGoal(this, 1.1, stack -> stack.is(FOOD), false));
        this.goalSelector.addGoal(6, new FollowParentGoal(this, 1.1));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.9, 0.002F));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                return !CrabEntity.this.isBaby() && super.canUse();
            }
        });
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VARIANT, 0);
        builder.define(DATA_HIDDEN, false);
        builder.define(DATA_ANGRY, false);
    }

    // ------------------------------------------------------------------------------------------ variant
    public enum Variant {
        RED("red"), BLUE("blue"), SAND("sand");

        public final String name;

        Variant(String name) {
            this.name = name;
        }

        public static Variant byId(int id) {
            Variant[] values = values();
            return values[Math.floorMod(id, values.length)];
        }

        public static Variant random(RandomSource random) {
            int r = random.nextInt(20);
            return r < 9 ? RED : r < 16 ? SAND : BLUE;
        }
    }

    public Variant getVariant() {
        return Variant.byId(this.entityData.get(DATA_VARIANT));
    }

    public void setVariant(Variant variant) {
        this.entityData.set(DATA_VARIANT, variant.ordinal());
    }

    /** All crabs of a natural spawn group share one shell colour. */
    private static final class CrabGroupData extends AgeableMob.AgeableMobGroupData {
        final Variant variant;

        CrabGroupData(Variant variant) {
            super(0.15F);
            this.variant = variant;
        }
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                                  MobSpawnType reason, @Nullable SpawnGroupData groupData) {
        if (groupData instanceof CrabGroupData crabGroup) {
            this.setVariant(crabGroup.variant);
        } else {
            Variant variant = Variant.random(level.getRandom());
            this.setVariant(variant);
            groupData = new CrabGroupData(variant);
        }
        return super.finalizeSpawn(level, difficulty, reason, groupData);
    }

    // ------------------------------------------------------------------------------------------ state
    public boolean isHidden() {
        return this.entityData.get(DATA_HIDDEN);
    }

    private void setHidden(boolean hidden) {
        this.entityData.set(DATA_HIDDEN, hidden);
    }

    @Override
    public boolean isThreatening() {
        return this.entityData.get(DATA_ANGRY);
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
        return Mth.lerp(partialTicks, this.burrowO, this.burrow);
    }

    @Override
    public float getSidewaysAmount(float partialTicks) {
        return Mth.lerp(partialTicks, this.sidewaysO, this.sideways);
    }

    public boolean canBurrowHere() {
        return this.onGround() && !this.isInWater() && this.level().getBlockState(this.blockPosition().below()).is(BURROWABLE);
    }

    private void burrow(ServerLevel level) {
        this.setHidden(true);
        this.hideTicks = 100 + this.random.nextInt(120);
        this.getNavigation().stop();
        this.playSound(ShoreSounds.CRAB_BURROW, 0.8F, 1.0F + (this.random.nextFloat() - 0.5F) * 0.2F);
        this.sandBurst(level, 14);
    }

    private void unburrow(ServerLevel level) {
        this.setHidden(false);
        this.hideTicks = 0;
        this.playSound(ShoreSounds.CRAB_BURROW, 0.6F, 1.3F);
        this.sandBurst(level, 8);
    }

    private void sandBurst(ServerLevel level, int count) {
        BlockState below = level.getBlockState(this.blockPosition().below());
        if (!below.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, below),
                    this.getX(), this.getY() + 0.1, this.getZ(), count, 0.25, 0.05, 0.25, 0.08);
        }
    }

    @Override
    protected void customServerAiStep() {
        ServerLevel level = (ServerLevel) this.level();
        super.customServerAiStep();
        if (this.angerTicks > 0) {
            this.angerTicks--;
        } else if (this.getTarget() != null) {
            this.setTarget(null);
        }
        this.entityData.set(DATA_ANGRY, this.getTarget() != null);

        if (!this.isHidden()) {
            if (this.tickCount % 4 == 0 && this.getTarget() == null && !this.isBaby() && this.canBurrowHere()
                    && this.sprintingPlayerNear(level, 7.0)) {
                this.burrow(level);
            }
        } else {
            if (this.sprintingPlayerNear(level, 9.0)) {
                this.hideTicks = Math.max(this.hideTicks, 60);
            }
            if (--this.hideTicks <= 0 || this.getTarget() != null || !this.level().getBlockState(this.blockPosition().below()).is(BURROWABLE)) {
                this.unburrow(level);
            }
        }
    }

    private boolean sprintingPlayerNear(ServerLevel level, double range) {
        for (Player player : level.players()) {
            if (!player.isSpectator() && player.isSprinting() && player.distanceToSqr(this) < range * range) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.burrowO = this.burrow;
            this.sidewaysO = this.sideways;
            // digging in takes almost a second, with sand flying the whole time, so it can be seen (3.1.0: a third
            // of a second and one puff); coming out stays quick
            this.burrow = Mth.approach(this.burrow, this.isHidden() ? 1.0F : 0.0F, this.isHidden() ? 0.06F : 0.2F);
            if (this.isHidden() && this.burrow < 1.0F) {
                BlockState below = this.level().getBlockState(this.blockPosition().below());
                if (!below.isAir()) {
                    for (int i = 0; i < 3; i++) {
                        this.level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, below),
                                this.getX() + (this.random.nextDouble() - 0.5) * 0.6, this.getY() + 0.1,
                                this.getZ() + (this.random.nextDouble() - 0.5) * 0.6,
                                (this.random.nextDouble() - 0.5) * 0.3, 0.25 + this.random.nextDouble() * 0.2,
                                (this.random.nextDouble() - 0.5) * 0.3);
                    }
                }
            }
            double dx = this.getX() - this.xo;
            double dz = this.getZ() - this.zo;
            boolean moving = dx * dx + dz * dz > 2.0E-5;
            this.sideways = Mth.approach(this.sideways, moving ? 1.0F : 0.0F, 0.12F);
            if (!this.isHidden() && this.random.nextInt(this.isThreatening() ? 22 : 260) == 0) {
                this.snapAnimation.start(this.tickCount);
                this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), ShoreSounds.CRAB_PINCH, SoundSource.NEUTRAL,
                        0.25F, 1.5F + this.random.nextFloat() * 0.3F, false);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float damage) {
        if (!(this.level() instanceof ServerLevel level)) return false;
        boolean hurt = super.hurt(source, damage);
        if (hurt && source.getEntity() instanceof LivingEntity && !this.isBaby()) {
            this.angerTicks = 300;
            if (this.isHidden()) {
                this.unburrow(level);
            }
        }
        return hurt;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (!(this.level() instanceof ServerLevel level)) return false;
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            level.broadcastEntityEvent(this, EVENT_PINCH);
            this.playSound(ShoreSounds.CRAB_PINCH, 1.0F, 0.9F + this.random.nextFloat() * 0.2F);
        }
        return hit;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_PINCH) {
            this.pinchAnimation.start(this.tickCount);
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public boolean isPushable() {
        return !this.isHidden() && super.isPushable();
    }

    // ------------------------------------------------------------------------------------------ breeding
    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(FOOD);
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        CrabEntity baby = ShoreEntities.CRAB.create(level);
        if (baby != null) {
            Variant variant = partner instanceof CrabEntity other && this.random.nextBoolean() ? other.getVariant() : this.getVariant();
            baby.setVariant(variant);
        }
        return baby;
    }

    // ------------------------------------------------------------------------------------------ sounds
    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return this.isHidden() ? null : ShoreSounds.CRAB_AMBIENT;
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource source) {
        return ShoreSounds.CRAB_HURT;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return ShoreSounds.CRAB_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(ShoreSounds.CRAB_STEP, 0.15F, 1.0F);
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }

    // ------------------------------------------------------------------------------------------ save
    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag121) {
        ValueOutput output = ValueOutput.of(tag121, this.registryAccess());
        super.addAdditionalSaveData(output.tag());
        output.putString("Variant", this.getVariant().name);
        output.putBoolean("Hidden", this.isHidden());
        output.putInt("HideTicks", this.hideTicks);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag121) {
        ValueInput input = ValueInput.of(tag121, this.registryAccess());
        super.readAdditionalSaveData(input.tag());
        String name = input.getStringOr("Variant", "red");
        for (Variant v : Variant.values()) {
            if (v.name.equals(name)) {
                this.setVariant(v);
            }
        }
        this.setHidden(input.getBooleanOr("Hidden", false));
        this.hideTicks = input.getIntOr("HideTicks", 0);
    }

    // ------------------------------------------------------------------------------------------ spawning
    public static boolean checkCrabSpawnRules(EntityType<CrabEntity> type, ServerLevelAccessor level, MobSpawnType reason,
                                              BlockPos pos, RandomSource random) {
        return level.getBlockState(pos.below()).is(SPAWNABLE_ON)
                && (MobSpawnType.ignoresLightRequirements(reason) || isBrightEnoughToSpawn(level, pos));
    }

    /** Keeps the crab still while it is dug in. */
    private class HideGoal extends Goal {
        HideGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return CrabEntity.this.isHidden();
        }

        @Override
        public void start() {
            CrabEntity.this.getNavigation().stop();
        }

        @Override
        public void tick() {
            CrabEntity.this.getNavigation().stop();
            CrabEntity.this.setZza(0.0F);
            CrabEntity.this.setXxa(0.0F);
        }
    }
}
