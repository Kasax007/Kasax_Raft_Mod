package net.kasax.raft.content.shore.entity;

import net.kasax.raft.Raft;
import net.kasax.raft.content.shore.ShoreSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

/**
 * Sea otter of the kelp forests: floats on its back at the surface, dives to the sea floor for shells and cracks them
 * on its belly with a stone. Feed it a seashell and it follows you for a while and now and then brings you a pearl
 * or a seashell from its dives.
 */
public class SeaOtterEntity extends Animal {
    public static final TagKey<Item> TREATS = TagKey.create(Registries.ITEM, Raft.id("sea_otter_treats"));
    public static final byte EVENT_CRACK = 84;
    public static final byte EVENT_HAPPY = 85;
    private static final int TRUST_PER_SHELL = 6000;
    private static final int MAX_TRUST = 24000;
    private static final EntityDataAccessor<Boolean> DATA_ON_BACK = SynchedEntityData.defineId(SeaOtterEntity.class, EntityDataSerializers.BOOLEAN);

    public final AnimationState crackAnimation = new AnimationState();
    private float back;
    private float backO;
    private float swim;
    private float swimO;

    private @Nullable EntityReference<Player> trusted;
    private int trustTicks;
    private int giftTimer = 2400;
    private int diveCooldown = 200;
    private boolean diving;
    private boolean carryingFind;
    private int crackTicks;

    public SeaOtterEntity(EntityType<? extends SeaOtterEntity> type, Level level) {
        super(type, level);
        this.setPathfindingMalus(PathType.WATER, 0.0F);
        this.moveControl = new SmoothSwimmingMoveControl<>(this, 85, 12, 0.7F, 1.0F, false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new AmphibiousPathNavigation(this, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new PanicGoal(this, 1.6));
        this.goalSelector.addGoal(1, new GoToWaterGoal());
        this.goalSelector.addGoal(2, new TemptGoal(this, 1.15, stack -> stack.is(TREATS), false));
        this.goalSelector.addGoal(3, new CrackShellGoal());
        this.goalSelector.addGoal(4, new FollowTrustedGoal());
        this.goalSelector.addGoal(5, new DiveGoal());
        this.goalSelector.addGoal(6, new DriftGoal());
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ON_BACK, false);
    }

    public boolean isOnBack() {
        return this.entityData.get(DATA_ON_BACK);
    }

    public float getBackAmount(float partialTicks) {
        return Mth.lerp(partialTicks, this.backO, this.back);
    }

    public float getSwimAmount(float partialTicks) {
        return Mth.lerp(partialTicks, this.swimO, this.swim);
    }

    public @Nullable Player getTrustedPlayer() {
        return this.trusted == null || this.trustTicks <= 0 ? null : this.trusted.getEntity(this.level(), Player.class);
    }

    /** Height of the water surface above the otter, or NaN if it is not in water. */
    private double surfaceY() {
        BlockPos.MutableBlockPos pos = this.blockPosition().mutable();
        if (!this.level().getFluidState(pos).is(FluidTags.WATER)) {
            pos.move(0, -1, 0);
            if (!this.level().getFluidState(pos).is(FluidTags.WATER)) {
                return Double.NaN;
            }
        }
        int guard = 0;
        while (this.level().getFluidState(pos.above()).is(FluidTags.WATER) && guard++ < 64) {
            pos.move(0, 1, 0);
        }
        return pos.getY() + this.level().getFluidState(pos).getHeight(this.level(), pos);
    }

    private boolean atSurface() {
        double s = this.surfaceY();
        return !Double.isNaN(s) && this.getY() > s - 0.9;
    }

    // ------------------------------------------------------------------------------------------ ticking
    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.backO = this.back;
            this.swimO = this.swim;
            this.back = Mth.approach(this.back, this.isOnBack() ? 1.0F : 0.0F, 0.08F);
            boolean swimming = this.isInWater() && (!this.isOnBack() || this.getDeltaMovement().horizontalDistanceSqr() > 0.004);
            this.swim = Mth.approach(this.swim, swimming ? 1.0F : 0.0F, 0.1F);
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (this.trustTicks > 0) {
            this.trustTicks--;
        }
        if (this.giftTimer > 0) {
            this.giftTimer--;
        }
        if (this.diveCooldown > 0) {
            this.diveCooldown--;
        }
        boolean onBack = this.isInWater() && !this.diving && this.atSurface()
                && this.getDeltaMovement().horizontalDistanceSqr() < 0.012;
        this.entityData.set(DATA_ON_BACK, onBack);
    }

    /** Buoyant: floats at the surface unless it is diving. */
    @Override
    protected void travelInWater(Vec3 input, double baseGravity, boolean isFalling, double oldY) {
        this.moveRelative(this.getSpeed(), input);
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().scale(0.86));
        if (!this.diving) {
            double s = this.surfaceY();
            if (!Double.isNaN(s)) {
                double want = s - 0.28;
                double dy = want - this.getY();
                this.setDeltaMovement(this.getDeltaMovement().add(0.0, Mth.clamp(dy * 0.08, -0.03, 0.04), 0.0));
            }
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
        return level.getFluidState(pos).is(FluidTags.WATER) ? 10.0F : 0.0F;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(TREATS)) {
            if (this.level() instanceof ServerLevel level) {
                this.usePlayerItem(player, hand, stack);
                boolean first = this.trusted == null || !this.trusted.matches(player);
                this.trusted = EntityReference.of(player);
                this.trustTicks = Math.min(MAX_TRUST, (first ? 0 : this.trustTicks) + TRUST_PER_SHELL);
                if (first) {
                    this.giftTimer = Math.min(this.giftTimer, 400 + this.random.nextInt(400));
                }
                this.setPersistenceRequired();
                level.broadcastEntityEvent(this, EVENT_HAPPY);
                this.playSound(net.minecraft.sounds.SoundEvents.GENERIC_EAT.value(), 0.8F, 1.2F);
                this.playSound(ShoreSounds.SEA_OTTER_AMBIENT, 1.0F, 1.3F);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_CRACK) {
            this.crackAnimation.start(this.tickCount);
        } else if (id == EVENT_HAPPY) {
            for (int i = 0; i < 6; i++) {
                this.level().addParticle(ParticleTypes.HEART, this.getRandomX(0.6), this.getY() + 0.5 + this.random.nextDouble() * 0.3,
                        this.getRandomZ(0.6), 0.0, 0.05, 0.0);
            }
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt) {
            this.crackTicks = 0;
            if (source.getEntity() instanceof Player player && this.trusted != null && this.trusted.matches(player)) {
                this.trustTicks = 0;
            }
        }
        return hurt;
    }

    private ItemStack rollFind() {
        boolean pearl = this.random.nextInt(10) < 3;
        Item item = BuiltInRegistries.ITEM.getValue(Raft.id(pearl ? "pearl" : "seashell"));
        if (item == Items.AIR) {
            item = BuiltInRegistries.ITEM.getValue(Raft.id("seashell"));
        }
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    private void giveGift(ServerLevel level, Player player) {
        ItemStack gift = this.rollFind();
        if (gift.isEmpty()) {
            return;
        }
        Vec3 dir = player.position().subtract(this.position());
        ItemEntity entity = new ItemEntity(level, this.getX(), this.getY() + 0.5, this.getZ(), gift);
        Vec3 toss = dir.lengthSqr() < 1.0E-4 ? Vec3.ZERO : dir.normalize().scale(0.25);
        entity.setDeltaMovement(toss.x, 0.3, toss.z);
        entity.setDefaultPickUpDelay();
        level.addFreshEntity(entity);
        level.broadcastEntityEvent(this, EVENT_HAPPY);
        this.playSound(ShoreSounds.SEA_OTTER_GIFT, 1.0F, 1.0F);
        this.giftTimer = 1200 + this.random.nextInt(1800);
    }

    // ------------------------------------------------------------------------------------------ animal
    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    public boolean canMate(Animal partner) {
        return false;
    }

    @Override
    protected boolean canBeABaby() {
        return false;
    }

    /** Wild otters come and go with the kelp forest; one you have fed stays. */
    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return this.trusted == null && !this.isPersistenceRequired();
    }

    // ------------------------------------------------------------------------------------------ sounds
    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return ShoreSounds.SEA_OTTER_AMBIENT;
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource source) {
        return ShoreSounds.SEA_OTTER_HURT;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return ShoreSounds.SEA_OTTER_DEATH;
    }

    @Override
    protected SoundEvent getSwimSound() {
        return net.minecraft.sounds.SoundEvents.DOLPHIN_SWIM;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 220;
    }

    // ------------------------------------------------------------------------------------------ save
    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        EntityReference.store(this.trusted, output, "Trusted");
        output.putInt("TrustTicks", this.trustTicks);
        output.putInt("GiftTimer", this.giftTimer);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.trusted = EntityReference.read(input, "Trusted");
        this.trustTicks = input.getIntOr("TrustTicks", 0);
        this.giftTimer = input.getIntOr("GiftTimer", 2400);
    }

    /** In the upper water layer of the kelp forest, close to the surface. */
    public static boolean checkSeaOtterSpawnRules(EntityType<SeaOtterEntity> type, ServerLevelAccessor level, EntitySpawnReason reason,
                                                  BlockPos pos, RandomSource random) {
        int seaLevel = level.getSeaLevel();
        return pos.getY() >= seaLevel - 4 && pos.getY() <= seaLevel
                && level.getFluidState(pos).is(FluidTags.WATER)
                && (level.getBlockState(pos.above()).isAir() || level.getBlockState(pos.above()).is(Blocks.WATER));
    }

    // ------------------------------------------------------------------------------------------ goals
    /** Gets back into the water when it ended up on land. */
    private class GoToWaterGoal extends MoveToBlockGoal {
        GoToWaterGoal() {
            super(SeaOtterEntity.this, 1.1, 12, 4);
        }

        @Override
        public boolean canUse() {
            return !SeaOtterEntity.this.isInWater() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return !SeaOtterEntity.this.isInWater() && super.canContinueToUse();
        }

        @Override
        protected boolean isValidTarget(LevelReader level, BlockPos pos) {
            return level.getFluidState(pos).is(FluidTags.WATER) && level.getFluidState(pos.below()).is(FluidTags.WATER);
        }
    }

    /** Lies on its back after a dive and bangs the shell open with a stone on its belly. */
    private class CrackShellGoal extends Goal {
        CrackShellGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            SeaOtterEntity otter = SeaOtterEntity.this;
            return otter.carryingFind && !otter.diving && otter.isInWater() && otter.atSurface();
        }

        @Override
        public void start() {
            SeaOtterEntity otter = SeaOtterEntity.this;
            otter.crackTicks = 60;
            otter.getNavigation().stop();
            otter.level().broadcastEntityEvent(otter, EVENT_CRACK);
        }

        @Override
        public boolean canContinueToUse() {
            return SeaOtterEntity.this.crackTicks > 0 && SeaOtterEntity.this.isInWater();
        }

        @Override
        public void tick() {
            SeaOtterEntity otter = SeaOtterEntity.this;
            otter.crackTicks--;
            otter.getNavigation().stop();
            if (otter.crackTicks % 12 == 6) {
                otter.playSound(ShoreSounds.SEA_OTTER_CRACK, 0.8F, 0.9F + otter.random.nextFloat() * 0.3F);
                if (otter.level() instanceof ServerLevel level) {
                    level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, Items.BONE_MEAL), otter.getX(), otter.getY() + 0.45,
                            otter.getZ(), 3, 0.08, 0.05, 0.08, 0.04);
                }
            }
        }

        @Override
        public void stop() {
            SeaOtterEntity otter = SeaOtterEntity.this;
            if (otter.crackTicks <= 0 && otter.level() instanceof ServerLevel level) {
                otter.carryingFind = false;
                Player friend = otter.getTrustedPlayer();
                if (friend != null && otter.giftTimer <= 0 && friend.distanceToSqr(otter) < 16 * 16) {
                    otter.giveGift(level, friend);
                } else {
                    otter.heal(1.0F);
                }
            }
            otter.crackTicks = 0;
        }
    }

    /** Swims after the player who fed it. */
    private class FollowTrustedGoal extends Goal {
        private @Nullable Player friend;

        FollowTrustedGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Player player = SeaOtterEntity.this.getTrustedPlayer();
            if (player == null || player.isSpectator() || player.distanceToSqr(SeaOtterEntity.this) > 48 * 48
                    || player.distanceToSqr(SeaOtterEntity.this) < 5 * 5) {
                return false;
            }
            this.friend = player;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return this.friend != null && this.friend.isAlive() && SeaOtterEntity.this.trustTicks > 0
                    && this.friend.distanceToSqr(SeaOtterEntity.this) > 3 * 3 && this.friend.distanceToSqr(SeaOtterEntity.this) < 48 * 48;
        }

        @Override
        public void tick() {
            SeaOtterEntity otter = SeaOtterEntity.this;
            if (this.friend == null) {
                return;
            }
            otter.getLookControl().setLookAt(this.friend, 10.0F, otter.getMaxHeadXRot());
            if (otter.tickCount % 10 == 0 || otter.getNavigation().isDone()) {
                double y = otter.isInWater() && this.friend.isInWater() ? this.friend.getY() : this.friend.getY();
                otter.getNavigation().moveTo(this.friend.getX(), y, this.friend.getZ(), 1.25);
            }
        }

        @Override
        public void stop() {
            this.friend = null;
            SeaOtterEntity.this.getNavigation().stop();
        }
    }

    /** Dives to the sea floor, forages for a moment and comes back up with a shell. */
    private class DiveGoal extends Goal {
        private @Nullable BlockPos floor;
        private int forage;
        private int time;

        DiveGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            SeaOtterEntity otter = SeaOtterEntity.this;
            if (otter.diveCooldown > 0 || otter.carryingFind || !otter.isInWater() || !otter.atSurface()) {
                return false;
            }
            BlockPos.MutableBlockPos pos = otter.blockPosition().mutable();
            RandomSource r = otter.random;
            pos.move(r.nextInt(9) - 4, 0, r.nextInt(9) - 4);
            int depth = 0;
            while (otter.level().getFluidState(pos.below()).is(FluidTags.WATER) && depth < 28) {
                pos.move(0, -1, 0);
                depth++;
            }
            if (depth < 3 || depth >= 28) {
                otter.diveCooldown = 100;
                return false;
            }
            this.floor = pos.immutable();
            return true;
        }

        @Override
        public void start() {
            SeaOtterEntity otter = SeaOtterEntity.this;
            otter.diving = true;
            this.forage = 0;
            this.time = 0;
            if (this.floor != null) {
                otter.getNavigation().moveTo(this.floor.getX() + 0.5, this.floor.getY() + 0.2, this.floor.getZ() + 0.5, 1.2);
            }
        }

        @Override
        public boolean canContinueToUse() {
            return this.floor != null && this.time < 500 && SeaOtterEntity.this.isInWater() && SeaOtterEntity.this.getAirSupply() > 200;
        }

        @Override
        public void tick() {
            SeaOtterEntity otter = SeaOtterEntity.this;
            this.time++;
            if (this.floor == null) {
                return;
            }
            double d = otter.distanceToSqr(Vec3.atBottomCenterOf(this.floor));
            if (this.forage == 0 && d > 2.5) {
                if (otter.getNavigation().isDone()) {
                    otter.getNavigation().moveTo(this.floor.getX() + 0.5, this.floor.getY() + 0.2, this.floor.getZ() + 0.5, 1.2);
                }
                otter.setDeltaMovement(otter.getDeltaMovement().add(0.0, -0.012, 0.0));
                return;
            }
            this.forage++;
            otter.getNavigation().stop();
            if (this.forage % 15 == 0 && otter.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.BUBBLE, otter.getX(), otter.getY() + 0.2, otter.getZ(), 4, 0.2, 0.1, 0.2, 0.02);
            }
            if (this.forage > 50) {
                otter.carryingFind = true;
                this.floor = null;
            }
        }

        @Override
        public void stop() {
            SeaOtterEntity otter = SeaOtterEntity.this;
            otter.diving = false;
            otter.diveCooldown = 500 + otter.random.nextInt(900);
            this.floor = null;
        }
    }

    /** Lazily drifts around on the surface. */
    private class DriftGoal extends Goal {
        DriftGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            SeaOtterEntity otter = SeaOtterEntity.this;
            return otter.isInWater() && otter.getNavigation().isDone() && otter.random.nextInt(80) == 0;
        }

        @Override
        public void start() {
            SeaOtterEntity otter = SeaOtterEntity.this;
            double a = otter.random.nextDouble() * Math.PI * 2.0;
            double dist = 3.0 + otter.random.nextDouble() * 6.0;
            double x = otter.getX() + Math.cos(a) * dist;
            double z = otter.getZ() + Math.sin(a) * dist;
            BlockPos column = BlockPos.containing(x, otter.getY(), z);
            if (otter.level().getFluidState(column).is(FluidTags.WATER)) {
                otter.getNavigation().moveTo(x, otter.getY(), z, 0.6);
            }
        }

        @Override
        public boolean canContinueToUse() {
            return !SeaOtterEntity.this.getNavigation().isDone();
        }
    }
}
