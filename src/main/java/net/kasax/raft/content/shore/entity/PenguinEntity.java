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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.kasax.raft.compat.ValueInput;
import net.kasax.raft.compat.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Penguin of the frost floes: waddles over the ice, slides on its belly across ice and snow, dives into the sea to
 * hunt fish and swims fast. Breeds with cod; the chicks are grey and fluffy.
 */
public class PenguinEntity extends Animal {
    public static final TagKey<Block> SLIDES_ON = TagKey.create(Registries.BLOCK, Raft.id("penguin_slides_on"));
    public static final TagKey<Block> SPAWNABLE_ON = TagKey.create(Registries.BLOCK, Raft.id("penguin_spawnable_on"));
    public static final TagKey<Item> FOOD = TagKey.create(Registries.ITEM, Raft.id("penguin_food"));
    private static final ResourceLocation SLIDE_SPEED_ID = Raft.id("penguin_slide");
    private static final AttributeModifier SLIDE_SPEED = new AttributeModifier(SLIDE_SPEED_ID, 0.9, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    private static final EntityDataAccessor<Boolean> DATA_SLIDING = SynchedEntityData.defineId(PenguinEntity.class, EntityDataSerializers.BOOLEAN);

    private float slide;
    private float slideO;
    private float swim;
    private float swimO;
    private int slideTicks;
    private int slideCooldown;
    private int huntCooldown = 400;
    private int swimTicks;

    public PenguinEntity(EntityType<? extends PenguinEntity> type, Level level) {
        super(type, level);
        this.setPathfindingMalus(PathType.WATER, 0.0F);
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 12, 0.8F, 1.0F, false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return net.minecraft.world.entity.Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.FOLLOW_RANGE, 16.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new AmphibiousPathNavigation(this, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new PanicGoal(this, 1.7));
        this.goalSelector.addGoal(1, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(2, new TemptGoal(this, 1.15, stack -> stack.is(FOOD), false));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.5, true));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
        this.goalSelector.addGoal(5, new GoToWaterGoal());
        this.goalSelector.addGoal(5, new GoToIceGoal());
        this.goalSelector.addGoal(6, new RandomSwimmingGoal(this, 1.0, 30));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.9, 0.003F));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        // dives for fish now and then
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, AbstractFish.class, 20, false, false,
                target -> this.huntCooldown <= 0 && !this.isBaby() && target.isInWater()));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SLIDING, false);
    }

    public boolean isSliding() {
        return this.entityData.get(DATA_SLIDING);
    }

    private void setSliding(boolean sliding) {
        this.entityData.set(DATA_SLIDING, sliding);
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(SLIDE_SPEED_ID);
            if (sliding) {
                speed.addTransientModifier(SLIDE_SPEED);
            }
        }
    }

    public float getSlideAmount(float partialTicks) {
        return Mth.lerp(partialTicks, this.slideO, this.slide);
    }

    public float getSwimAmount(float partialTicks) {
        return Mth.lerp(partialTicks, this.swimO, this.swim);
    }

    // ------------------------------------------------------------------------------------------ ticking
    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.slideO = this.slide;
            this.swimO = this.swim;
            this.slide = Mth.approach(this.slide, this.isSliding() ? 1.0F : 0.0F, 0.2F);
            boolean swimming = this.isInWater() && !this.onGround();
            this.swim = Mth.approach(this.swim, swimming ? 1.0F : 0.0F, 0.15F);
            if (this.isSliding() && this.onGround() && this.random.nextInt(3) == 0) {
                BlockState below = this.level().getBlockState(this.getBlockPosBelowThatAffectsMyMovement());
                if (!below.isAir()) {
                    this.level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, below),
                            this.getX(), this.getY() + 0.1, this.getZ(), 0.0, 0.05, 0.0);
                }
            }
        }
    }

    @Override
    protected void customServerAiStep() {
        ServerLevel level = (ServerLevel) this.level();
        super.customServerAiStep();
        if (this.huntCooldown > 0) {
            this.huntCooldown--;
        }
        if (this.slideCooldown > 0) {
            this.slideCooldown--;
        }
        this.swimTicks = this.isInWater() ? this.swimTicks + 1 : 0;
        if (this.getTarget() != null && (this.huntCooldown > 0 || !this.getTarget().isInWater())) {
            this.setTarget(null);
        }

        boolean onSlideBlock = this.onGround() && level.getBlockState(this.getBlockPosBelowThatAffectsMyMovement()).is(SLIDES_ON);
        boolean moving = this.getDeltaMovement().horizontalDistanceSqr() > 0.0016;
        if (this.isSliding()) {
            if (--this.slideTicks <= 0 || !onSlideBlock && this.onGround() || this.isInWater() || !moving && this.slideTicks < 30) {
                this.setSliding(false);
                this.slideCooldown = 60 + this.random.nextInt(100);
            }
        } else if (onSlideBlock && moving && this.slideCooldown <= 0 && !this.isBaby()
                && (this.random.nextInt(40) == 0 || this.getLastHurtByMob() != null)) {
            this.setSliding(true);
            this.slideTicks = 40 + this.random.nextInt(60);
            this.playSound(ShoreSounds.PENGUIN_SLIDE, 0.8F, 1.0F + (this.random.nextFloat() - 0.5F) * 0.2F);
        }
    }

    /** Swims like a torpedo; drifts up to the surface when it has nothing to do. */
    
    protected void travelInWater(Vec3 input, double baseGravity, boolean isFalling, double oldY) {
        this.moveRelative(this.getSpeed(), input);
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().scale(0.88));
        if (this.getTarget() == null && this.getNavigation().isDone()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, 0.006, 0.0));
        }
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
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public int getMaxAirSupply() {
        return 4800;
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        if (level.getFluidState(pos).is(FluidTags.WATER)) {
            return 4.0F;
        }
        return level.getBlockState(pos.below()).is(SLIDES_ON) ? 10.0F : 0.0F;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (!(this.level() instanceof ServerLevel level)) return false;
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living && !living.isAlive()) {
            this.huntCooldown = 1200 + this.random.nextInt(1200);
            this.heal(2.0F);
            this.playSound(net.minecraft.sounds.SoundEvents.GENERIC_EAT, 0.6F, 1.3F);
        }
        return hit;
    }

    @Override
    public boolean hurt(DamageSource source, float damage) {
        if (!(this.level() instanceof ServerLevel level)) return false;
        boolean hurt = super.hurt(source, damage);
        if (hurt) {
            this.setTarget(null);
        }
        return hurt;
    }

    // ------------------------------------------------------------------------------------------ breeding
    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(FOOD);
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return ShoreEntities.PENGUIN.create(level);
    }

    // ------------------------------------------------------------------------------------------ sounds
    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return ShoreSounds.PENGUIN_AMBIENT;
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource source) {
        return ShoreSounds.PENGUIN_HURT;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return ShoreSounds.PENGUIN_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        if (!this.isSliding()) {
            this.playSound(ShoreSounds.PENGUIN_STEP, 0.25F, 1.0F);
        }
    }

    @Override
    protected SoundEvent getSwimSound() {
        return net.minecraft.sounds.SoundEvents.DOLPHIN_SWIM;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    // ------------------------------------------------------------------------------------------ save
    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag121) {
        ValueOutput output = ValueOutput.of(tag121, this.registryAccess());
        super.addAdditionalSaveData(output.tag());
        output.putInt("HuntCooldown", this.huntCooldown);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag121) {
        ValueInput input = ValueInput.of(tag121, this.registryAccess());
        super.readAdditionalSaveData(input.tag());
        this.huntCooldown = input.getIntOr("HuntCooldown", 400);
    }

    public static boolean checkPenguinSpawnRules(EntityType<PenguinEntity> type, ServerLevelAccessor level, MobSpawnType reason,
                                                 BlockPos pos, RandomSource random) {
        return level.getBlockState(pos.below()).is(SPAWNABLE_ON)
                && (MobSpawnType.ignoresLightRequirements(reason) || isBrightEnoughToSpawn(level, pos));
    }

    // ------------------------------------------------------------------------------------------ goals
    /** Every now and then a penguin walks to the edge of the floe and jumps into the sea. */
    private class GoToWaterGoal extends MoveToBlockGoal {
        GoToWaterGoal() {
            super(PenguinEntity.this, 1.0, 10, 3);
        }

        @Override
        public boolean canUse() {
            return !PenguinEntity.this.isInWater() && !PenguinEntity.this.isBaby() && PenguinEntity.this.random.nextInt(500) == 0
                    && super.canUse();
        }

        @Override
        protected boolean isValidTarget(LevelReader level, BlockPos pos) {
            return level.getFluidState(pos).is(FluidTags.WATER) && level.getBlockState(pos.above()).isAir();
        }

        @Override
        public boolean canContinueToUse() {
            return !PenguinEntity.this.isInWater() && super.canContinueToUse();
        }
    }

    /** After a swim it climbs back onto the ice (or any shore). */
    private class GoToIceGoal extends MoveToBlockGoal {
        GoToIceGoal() {
            super(PenguinEntity.this, 1.0, 16, 4);
        }

        @Override
        public boolean canUse() {
            return PenguinEntity.this.isInWater() && PenguinEntity.this.swimTicks > 400 && PenguinEntity.this.getTarget() == null
                    && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return PenguinEntity.this.isInWater() && super.canContinueToUse();
        }

        @Override
        protected boolean isValidTarget(LevelReader level, BlockPos pos) {
            BlockState state = level.getBlockState(pos);
            return (state.is(SLIDES_ON) || state.is(SPAWNABLE_ON)) && level.getBlockState(pos.above()).isAir()
                    && level.getBlockState(pos.above(2)).isAir();
        }
    }
}
