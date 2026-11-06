package net.kasax.raft.content.shore.entity;

import net.kasax.raft.Raft;
import net.kasax.raft.block.custom.ScarecrowBlock;
import net.kasax.raft.content.shore.ShoreSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

/**
 * Seagull: flies in flocks over islands and rafts, lands on posts, foundations and beaches, floats on the water,
 * pecks at crops on farmland and crop plots (one growth stage back, unless a scarecrow stands within 16 blocks)
 * and snatches food that lies on the ground.
 */
public class SeagullEntity extends Animal {
    public static final TagKey<Block> PERCHES = TagKey.create(Registries.BLOCK, Raft.id("seagull_perches"));
    public static final TagKey<Block> SPAWNABLE_ON = TagKey.create(Registries.BLOCK, Raft.id("seagull_spawnable_on"));
    public static final int SCARECROW_RADIUS = 16;
    /** How far a gull looks for crops around itself, and how far away it still remembers a field. */
    private static final int CROP_SEARCH = 24;
    private static final int FIELD_MEMORY = 64;
    public static final byte EVENT_PECK = 82;
    public static final byte EVENT_CALL = 83;
    private static final int MAX_FLOCK = 7;

    public final AnimationState peckAnimation = new AnimationState();
    public final AnimationState callAnimation = new AnimationState();

    // client animation
    private float flyAmount;
    private float flyAmountO;
    private float flapPhase;
    private float flapPhaseO;
    private float flapPower;
    private float flapPowerO;
    private float roll;
    private float rollO;
    private int glideTicks;

    // server state
    private @Nullable SeagullEntity leader;
    private int followers;
    private int flightTicks;
    private int flightLimit = 400;
    private int restTicks;
    private int peckCooldown = 200;
    private int stealCooldown = 40;
    /** The food item this gull is diving for (so the rest of the flock picks other items). */
    private @Nullable ItemEntity stealTarget;
    /** Gulls spot food lying in the open from far away (blocks, each direction). */
    private static final double STEAL_RANGE = 22.0;
    /** Distance (blocks) from which a flying gull dives straight at food instead of following a path. */
    private static final double SWOOP_RANGE = 7.0;
    /** The last field this gull raided; it keeps coming back to it while there is something left to peck. */
    private @Nullable BlockPos field;
    private int eatTicks;
    private int fleeTicks;
    private @Nullable BlockPos home;
    private final Vec3 flockOffset;
    private float nextFlap = 1.0F;

    public SeagullEntity(EntityType<? extends SeagullEntity> type, Level level) {
        super(type, level);
        this.moveControl = new SeagullMoveControl(this);
        this.setPathfindingMalus(PathType.WATER, 0.0F);
        this.setPathfindingMalus(PathType.FIRE_IN_NEIGHBOR, -1.0F);
        this.setPathfindingMalus(PathType.FIRE, -1.0F);
        this.setPathfindingMalus(PathType.LEAVES, 4.0F);
        RandomSource r = this.random;
        // a place in a loose V behind the leader, in the leader's frame (x to the side, z forward): it turns with the
        // leader, so the flock keeps its shape in curves
        this.flockOffset = new Vec3((r.nextBoolean() ? 1.0 : -1.0) * (1.6 + r.nextFloat() * 2.6), (r.nextFloat() - 0.4F) * 2.0F,
                -(1.2 + r.nextFloat() * 4.0));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 6.0)
                .add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FleeGoal());
        this.goalSelector.addGoal(1, new StealFoodGoal());
        this.goalSelector.addGoal(2, new PeckCropGoal());
        this.goalSelector.addGoal(3, new FollowFlockGoal());
        this.goalSelector.addGoal(4, new LandGoal());
        this.goalSelector.addGoal(5, new WanderGoal());
        this.goalSelector.addGoal(6, new HopGoal());
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                                  EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
        this.home = this.blockPosition();
        this.restTicks = 40 + this.random.nextInt(200);
        if (groupData == null) {
            groupData = new AgeableMob.AgeableMobGroupData(false);
        }
        return super.finalizeSpawn(level, difficulty, reason, groupData);
    }

    @Override
    protected boolean canBeABaby() {
        return false;
    }

    // ------------------------------------------------------------------------------------------ state
    public boolean isFlying() {
        return !this.onGround() && !this.isInWater();
    }

    public boolean isFloating() {
        return this.isInWater() && !this.isUnderWater();
    }

    public boolean isFlockLeader() {
        return this.leader == null && this.followers > 0;
    }

    private boolean isRaftBlock(BlockState state) {
        return state.is(PERCHES);
    }

    /**
     * True every {@code interval} ticks on a tick where the goal selector runs its full update. Goals are only
     * checked on every other tick (shifted by the entity id), so a plain {@code tickCount % n} would never be true
     * for half of all gulls.
     */
    boolean aiTick(int interval) {
        return (this.tickCount + this.getId()) % interval == 0;
    }

    /** True if a scarecrow keeps gulls away from this spot. */
    public static boolean scarecrowGuards(ServerLevel level, BlockPos pos) {
        return ScarecrowBlock.isNear(level, pos, SCARECROW_RADIUS);
    }

    // ------------------------------------------------------------------------------------------ ticking
    @Override
    public void aiStep() {
        super.aiStep();
        Vec3 motion = this.getDeltaMovement();
        if (!this.onGround() && !this.isInWater() && motion.y < 0.0) {
            // wings spread: a gull glides down instead of dropping
            this.setDeltaMovement(motion.multiply(1.0, 0.6, 1.0));
        }
        if (this.isInWater()) {
            // floats like a duck, bobbing on the waves
            double depth = this.getFluidHeight(FluidTags.WATER);
            if (depth > 0.18) {
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.8, 0.5, 0.8).add(0.0, Math.min(0.08, depth * 0.25), 0.0));
            }
        }
        if (this.level().isClientSide()) {
            this.tickClientAnimation();
        }
    }

    private void tickClientAnimation() {
        this.flyAmountO = this.flyAmount;
        this.flapPhaseO = this.flapPhase;
        this.flapPowerO = this.flapPower;
        this.rollO = this.roll;
        boolean flying = this.isFlying();
        this.flyAmount = Mth.approach(this.flyAmount, flying ? 1.0F : 0.0F, flying ? 0.25F : 0.15F);
        Vec3 motion = this.getDeltaMovement();
        float targetPower;
        if (!flying) {
            targetPower = 0.0F;
            this.glideTicks = 0;
        } else if (motion.y > 0.02 || motion.horizontalDistanceSqr() < 0.004) {
            targetPower = 1.0F;          // climbing or hovering: strong flaps
            this.glideTicks = 0;
        } else if (motion.y < -0.03) {
            targetPower = 0.0F;          // gliding down
        } else {
            if (this.glideTicks > 0) {
                this.glideTicks--;
                targetPower = 0.0F;
            } else {
                targetPower = 0.6F;
                if (this.random.nextInt(40) == 0) {
                    this.glideTicks = 20 + this.random.nextInt(50);
                }
            }
        }
        this.flapPower = Mth.approach(this.flapPower, targetPower, 0.1F);
        this.flapPhase += 0.25F + 0.55F * this.flapPower;
        float turn = Mth.wrapDegrees(this.getYRot() - this.yRotO);
        float targetRoll = flying ? Mth.clamp(turn * 2.2F, -25.0F, 25.0F) : 0.0F;
        this.roll += (targetRoll - this.roll) * 0.1F;
    }

    public float getFlyAmount(float partialTicks) {
        return Mth.lerp(partialTicks, this.flyAmountO, this.flyAmount);
    }

    public float getFlapPhase(float partialTicks) {
        return Mth.lerp(partialTicks, this.flapPhaseO, this.flapPhase);
    }

    public float getFlapPower(float partialTicks) {
        return Mth.lerp(partialTicks, this.flapPowerO, this.flapPower);
    }

    public float getRoll(float partialTicks) {
        return Mth.lerp(partialTicks, this.rollO, this.roll);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (this.home == null) {
            this.home = this.blockPosition();
        }
        if (this.isFlying()) {
            this.flightTicks++;
        } else {
            this.flightTicks = 0;
            if (this.restTicks > 0) {
                this.restTicks--;
            }
        }
        if (this.peckCooldown > 0) {
            this.peckCooldown--;
        }
        if (this.stealCooldown > 0) {
            this.stealCooldown--;
        }
        if (this.fleeTicks > 0) {
            this.fleeTicks--;
        }
        if (this.tickCount % 40 == 0) {
            this.updateFlock(level);
        }
        ItemStack held = this.getMainHandItem();
        if (!held.isEmpty() && --this.eatTicks <= 0) {
            this.eat(level, held);
        }
    }

    private void updateFlock(ServerLevel level) {
        if (this.leader != null && (!this.leader.isAlive() || this.leader.leader != null || this.leader.distanceToSqr(this) > 48 * 48)) {
            this.leader = null;
        }
        List<SeagullEntity> near = level.getEntitiesOfClass(SeagullEntity.class, this.getBoundingBox().inflate(24.0), g -> g != this);
        int count = 0;
        for (SeagullEntity g : near) {
            if (g.leader == this) {
                count++;
            }
        }
        this.followers = count;
        if (this.leader == null && this.followers == 0) {
            for (SeagullEntity g : near) {
                if (g.leader == null && g.followers < MAX_FLOCK - 1 && (g.followers > 0 || g.getId() < this.getId())) {
                    this.leader = g;
                    g.followers++;
                    break;
                }
            }
        }
    }

    private void eat(ServerLevel level, ItemStack held) {
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, held.getItem()), this.getX(), this.getY() + 0.4, this.getZ(),
                8, 0.1, 0.1, 0.1, 0.05);
        this.playSound(ShoreSounds.SEAGULL_PECK, 0.8F, 1.0F);
        this.heal(2.0F);
        this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && this.isAlive()) {
            this.fleeTicks = 60;
            this.restTicks = 0;
            ItemStack held = this.getMainHandItem();
            if (!held.isEmpty()) {
                this.spawnAtLocation(level, held.copy());
                this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            }
        }
        return hurt;
    }

    @Override
    public void playAmbientSound() {
        super.playAmbientSound();
        this.level().broadcastEntityEvent(this, EVENT_CALL);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_PECK) {
            this.peckAnimation.start(this.tickCount);
        } else if (id == EVENT_CALL) {
            this.callAnimation.start(this.tickCount);
        } else {
            super.handleEntityEvent(id);
        }
    }

    // ------------------------------------------------------------------------------------------ movement
    /** Gulls cruise quickly once they are up in the air. */
    @Override
    protected float getFlyingSpeed() {
        return this.isFlying() ? 0.05F : 0.02F;
    }

    @Override
    protected boolean omnidirectionalAirMover() {
        return true;
    }

    @Override
    protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
    }

    @Override
    protected boolean isFlapping() {
        return this.isFlying() && this.flyDist > this.nextFlap;
    }

    @Override
    protected void onFlap() {
        this.playSound(ShoreSounds.SEAGULL_FLAP, 0.12F, 0.9F + this.random.nextFloat() * 0.2F);
        this.nextFlap = this.flyDist + 1.2F;
    }

    /** This gull's place in its leader's flock right now (the offset turned with the leader's heading). */
    private Vec3 flockSlot(SeagullEntity lead) {
        return lead.position().add(this.flockOffset.yRot(-lead.getYRot() * Mth.DEG_TO_RAD));
    }

    private void flyTo(Vec3 target, double speed) {
        this.getNavigation().moveTo(target.x, target.y, target.z, speed);
    }

    /** A point high above the ground or the sea near the given column. */
    private Vec3 airAbove(double x, double z, double minUp, double maxUp) {
        int ix = Mth.floor(x);
        int iz = Mth.floor(z);
        int ground = this.level().getHeight(Heightmap.Types.MOTION_BLOCKING, ix, iz);
        int base = Math.max(ground, this.level().getSeaLevel());
        return new Vec3(x, base + minUp + this.random.nextDouble() * (maxUp - minUp), z);
    }

    private @Nullable Player raftPlayerNear() {
        Player nearest = null;
        double best = 40.0 * 40.0;
        for (Player player : this.level().players()) {
            if (player.isSpectator()) {
                continue;
            }
            double d = player.distanceToSqr(this);
            if (d < best && this.isRaftBlock(this.level().getBlockState(player.getBlockPosBelowThatAffectsMyMovement()))) {
                best = d;
                nearest = player;
            }
        }
        return nearest;
    }

    private Vec3 pickWanderTarget() {
        RandomSource r = this.random;
        if (this.leader != null) {
            return this.flockSlot(this.leader).add(0.0, 1.0, 0.0);
        }
        Player raftPlayer = r.nextInt(4) == 0 ? this.raftPlayerNear() : null;
        if (raftPlayer != null) {
            double a = r.nextDouble() * Math.PI * 2.0;
            double radius = 5.0 + r.nextDouble() * 6.0;
            return new Vec3(raftPlayer.getX() + Math.cos(a) * radius, raftPlayer.getY() + 5.0 + r.nextDouble() * 5.0,
                    raftPlayer.getZ() + Math.sin(a) * radius);
        }
        if (this.field != null && r.nextInt(3) == 0 && this.field.distSqr(this.blockPosition()) < FIELD_MEMORY * FIELD_MEMORY) {
            return this.airAbove(this.field.getX() + (r.nextDouble() - 0.5) * 12.0, this.field.getZ() + (r.nextDouble() - 0.5) * 12.0,
                    5.0, 10.0);
        }
        double cx = this.getX();
        double cz = this.getZ();
        if (this.home != null && this.home.distSqr(this.blockPosition()) > 56 * 56) {
            cx = (cx + this.home.getX()) * 0.5;
            cz = (cz + this.home.getZ()) * 0.5;
        }
        double a = r.nextDouble() * Math.PI * 2.0;
        double dist = 8.0 + r.nextDouble() * 14.0;
        return this.airAbove(cx + Math.cos(a) * dist, cz + Math.sin(a) * dist, 5.0, 14.0);
    }

    /** Finds a spot to land: raft blocks and posts first, then beaches, roofs and rocks; null over open water. */
    private @Nullable BlockPos findLandingSpot(Vec3 around, int radius) {
        BlockPos best = null;
        int bestScore = 0;
        RandomSource r = this.random;
        Level level = this.level();
        for (int i = 0; i < 28; i++) {
            int x = Mth.floor(around.x) + r.nextInt(radius * 2 + 1) - radius;
            int z = Mth.floor(around.z) + r.nextInt(radius * 2 + 1) - radius;
            BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
            BlockPos below = top.below();
            BlockState ground = level.getBlockState(below);
            if (!level.getFluidState(below).isEmpty() || !level.getFluidState(top).isEmpty() || !level.isEmptyBlock(top.above())) {
                continue;
            }
            if (!ground.isFaceSturdy(level, below, net.minecraft.core.Direction.UP) && !ground.is(PERCHES)) {
                continue;
            }
            int score = ground.is(PERCHES) ? 10 : ground.is(SPAWNABLE_ON) ? 4 : 2;
            score += r.nextInt(3);
            if (score > bestScore) {
                bestScore = score;
                best = top;
            }
        }
        return best;
    }

    // ------------------------------------------------------------------------------------------ sounds
    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return ShoreSounds.SEAGULL_AMBIENT;
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource source) {
        return ShoreSounds.SEAGULL_HURT;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return ShoreSounds.SEAGULL_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(net.minecraft.sounds.SoundEvents.PARROT_STEP, 0.15F, 1.0F);
    }

    @Override
    public int getAmbientSoundInterval() {
        return 160;
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.NEUTRAL;
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
    public boolean isPushable() {
        return true;
    }

    @Override
    protected void doPush(Entity entity) {
        if (!(entity instanceof Player)) {
            super.doPush(entity);
        }
    }

    @Override
    public Vec3 getLeashOffset() {
        return new Vec3(0.0, 0.5F * this.getEyeHeight(), this.getBbWidth() * 0.4F);
    }

    // ------------------------------------------------------------------------------------------ save
    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (this.home != null) {
            output.putIntArray("Home", new int[]{this.home.getX(), this.home.getY(), this.home.getZ()});
        }
        output.putInt("EatTicks", this.eatTicks);
        if (this.field != null) {
            output.putIntArray("Field", new int[]{this.field.getX(), this.field.getY(), this.field.getZ()});
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        input.getIntArray("Home").ifPresent(a -> {
            if (a.length == 3) {
                this.home = new BlockPos(a[0], a[1], a[2]);
            }
        });
        this.eatTicks = input.getIntOr("EatTicks", 0);
        input.getIntArray("Field").ifPresent(a -> {
            if (a.length == 3) {
                this.field = new BlockPos(a[0], a[1], a[2]);
            }
        });
    }

    // ------------------------------------------------------------------------------------------ spawning
    public static boolean checkSeagullSpawnRules(EntityType<SeagullEntity> type, ServerLevelAccessor level, EntitySpawnReason reason,
                                                 BlockPos pos, RandomSource random) {
        return level.getBlockState(pos.below()).is(SPAWNABLE_ON)
                && (EntitySpawnReason.ignoresLightRequirements(reason) || isBrightEnoughToSpawn(level, pos));
    }

    // ------------------------------------------------------------------------------------------ AI
    /** Smooth banking flight in the air, small steps on the ground. */
    private static class SeagullMoveControl extends MoveControl<SeagullEntity> {
        SeagullMoveControl(SeagullEntity gull) {
            super(gull);
        }

        @Override
        public void tick() {
            SeagullEntity gull = this.mob;
            if (this.operation == MoveControl.Operation.MOVE_TO) {
                this.operation = MoveControl.Operation.WAIT;
                double dx = this.wantedX - gull.getX();
                double dy = this.wantedY - gull.getY();
                double dz = this.wantedZ - gull.getZ();
                double horizontal = Math.sqrt(dx * dx + dz * dz);
                if (dx * dx + dy * dy + dz * dz < 2.5E-7) {
                    gull.setYya(0.0F);
                    gull.setZza(0.0F);
                    return;
                }
                boolean air = !gull.onGround() || dy > 0.7 || horizontal > 3.0;
                float yRotD = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
                // wide curves in open flight, tight turns when swooping onto a spot close by
                gull.setYRot(this.rotlerp(gull.getYRot(), yRotD, !air ? 30.0F : horizontal < 4.0 ? 28.0F : 12.0F));
                gull.yBodyRot = gull.getYRot();
                if (air) {
                    gull.setNoGravity(true);
                    float speed = (float) (this.speedModifier * gull.getAttributeValue(Attributes.FLYING_SPEED));
                    if (horizontal < 2.5) {
                        // brake before the target instead of overshooting and circling it
                        speed *= 0.5F;
                    }
                    gull.setSpeed(speed);
                    gull.setYya((float) Mth.clamp(dy * 0.35, -speed, speed));
                } else {
                    gull.setNoGravity(false);
                    float speed = (float) (this.speedModifier * gull.getAttributeValue(Attributes.MOVEMENT_SPEED));
                    gull.setSpeed(speed);
                    gull.setYya(0.0F);
                }
            } else {
                gull.setNoGravity(false);
                gull.setYya(0.0F);
                gull.setZza(0.0F);
            }
        }
    }

    /** After being hurt the gull flaps away from the danger. */
    private class FleeGoal extends Goal {
        FleeGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return SeagullEntity.this.fleeTicks > 0;
        }

        @Override
        public void start() {
            SeagullEntity gull = SeagullEntity.this;
            LivingEntity attacker = gull.getLastHurtByMob();
            Vec3 away;
            if (attacker != null) {
                Vec3 dir = gull.position().subtract(attacker.position()).multiply(1.0, 0.0, 1.0);
                away = dir.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : dir.normalize();
            } else {
                double a = gull.random.nextDouble() * Math.PI * 2.0;
                away = new Vec3(Math.cos(a), 0.0, Math.sin(a));
            }
            Vec3 target = gull.airAbove(gull.getX() + away.x * 16.0, gull.getZ() + away.z * 16.0, 7.0, 12.0);
            gull.flyTo(target, 1.6);
        }

        @Override
        public boolean canContinueToUse() {
            return SeagullEntity.this.fleeTicks > 0 && !SeagullEntity.this.getNavigation().isDone();
        }
    }

    /** Swoops onto food lying on the ground and carries it off in its beak. */
    private class StealFoodGoal extends Goal {
        private @Nullable ItemEntity target;
        private int ticks;

        StealFoodGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            SeagullEntity gull = SeagullEntity.this;
            if (gull.stealCooldown > 0 || !gull.getMainHandItem().isEmpty() || !gull.aiTick(10)) {
                return false;
            }
            List<ItemEntity> items = gull.level().getEntitiesOfClass(ItemEntity.class, gull.getBoundingBox().inflate(STEAL_RANGE, STEAL_RANGE, STEAL_RANGE),
                    e -> e.isAlive() && e.getItem().has(DataComponents.FOOD) && !e.isInWater());
            if (items.isEmpty()) {
                return false;
            }
            List<SeagullEntity> rivals = gull.level().getEntitiesOfClass(SeagullEntity.class,
                    gull.getBoundingBox().inflate(STEAL_RANGE * 2.0), g -> g != gull && g.stealTarget != null);
            ItemEntity best = null;
            double bestDist = Double.MAX_VALUE;
            for (ItemEntity item : items) {
                double d = item.distanceToSqr(gull);
                // another gull already dives for this one (unless it is a whole stack: enough for two)
                boolean taken = item.getItem().getCount() < 2 && rivals.stream().anyMatch(g -> g.stealTarget == item);
                if (d < bestDist && !taken) {
                    bestDist = d;
                    best = item;
                }
            }
            if (best != null && !gull.hasLineOfSight(best)) {
                // food inside a hut or under a roof: not worth the trouble for a while
                gull.stealCooldown = 60;
                best = null;
            }
            this.target = best;
            return best != null;
        }

        @Override
        public void start() {
            this.ticks = 0;
            SeagullEntity.this.stealTarget = this.target;
            if (this.target != null) {
                SeagullEntity.this.flyTo(this.target.position().add(0.0, 0.2, 0.0), 1.4);
            }
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return this.target != null && this.target.isAlive() && SeagullEntity.this.getMainHandItem().isEmpty()
                    && SeagullEntity.this.fleeTicks == 0 && this.ticks < 300;
        }

        @Override
        public void tick() {
            SeagullEntity gull = SeagullEntity.this;
            if (this.target == null) {
                return;
            }
            this.ticks++;
            double distSqr = gull.distanceToSqr(this.target);
            Vec3 aim = this.target.position().add(0.0, 0.2, 0.0);
            if (gull.isFlying() && distSqr < SWOOP_RANGE * SWOOP_RANGE) {
                // final swoop: dive straight at the food (a path would make the fast gull overshoot and circle it)
                gull.getNavigation().stop();
                Vec3 to = aim.subtract(gull.position());
                double len = to.length();
                if (len > 1.0E-3) {
                    double v = Math.min(0.42, 0.14 + len * 0.05);
                    gull.setDeltaMovement(gull.getDeltaMovement().scale(0.4).add(to.scale(0.6 * v / len)));
                    float yaw = (float) (Mth.atan2(to.z, to.x) * Mth.RAD_TO_DEG) - 90.0F;
                    gull.setYRot(yaw);
                    gull.yBodyRot = yaw;
                }
            } else if (gull.aiTick(10) || gull.getNavigation().isDone() && gull.aiTick(4)) {
                gull.flyTo(aim, distSqr < 8.0 * 8.0 ? 0.9 : 1.4);
            }
            gull.getLookControl().setLookAt(this.target);
            if (distSqr < 2.6 && gull.level() instanceof ServerLevel level) {
                ItemStack stack = this.target.getItem();
                ItemStack taken = stack.split(1);
                if (stack.isEmpty()) {
                    this.target.discard();
                } else {
                    this.target.setItem(stack);
                }
                gull.setItemSlot(EquipmentSlot.MAINHAND, taken);
                gull.setGuaranteedDrop(EquipmentSlot.MAINHAND);
                gull.eatTicks = 100 + gull.random.nextInt(140);
                gull.stealCooldown = 400 + gull.random.nextInt(400);
                gull.restTicks = 0;
                gull.playSound(ShoreSounds.SEAGULL_STEAL, 1.0F, 1.0F + (gull.random.nextFloat() - 0.5F) * 0.2F);
                level.broadcastEntityEvent(gull, EVENT_CALL);
                // off it goes with the loot
                gull.flyTo(gull.airAbove(gull.getX() + (gull.random.nextDouble() - 0.5) * 24.0,
                        gull.getZ() + (gull.random.nextDouble() - 0.5) * 24.0, 6.0, 10.0), 1.4);
                this.target = null;
            }
        }

        @Override
        public void stop() {
            if (this.target != null && this.ticks >= 300) {
                // gave up (could not reach it in time)
                SeagullEntity.this.stealCooldown = Math.max(SeagullEntity.this.stealCooldown, 200);
            }
            this.target = null;
            SeagullEntity.this.stealTarget = null;
        }
    }

    /** Lands next to a growing crop on farmland or a crop plot and pecks it back one growth stage. */
    private class PeckCropGoal extends Goal {
        private @Nullable BlockPos crop;
        private int peckTicks;
        private int approachTicks;

        PeckCropGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            SeagullEntity gull = SeagullEntity.this;
            if (gull.peckCooldown > 0 || !gull.aiTick(20) || !(gull.level() instanceof ServerLevel level)) {
                return false;
            }
            this.crop = this.findCrop(level);
            return this.crop != null;
        }

        private @Nullable BlockPos findCrop(ServerLevel level) {
            SeagullEntity gull = SeagullEntity.this;
            RandomSource r = gull.random;
            BlockPos origin = gull.blockPosition();
            // back to the known field first
            if (gull.field != null) {
                if (gull.field.distSqr(origin) < FIELD_MEMORY * FIELD_MEMORY) {
                    for (int i = 0; i < 24; i++) {
                        BlockPos crop = this.cropAt(level, gull.field.getX() + r.nextInt(11) - 5,
                                gull.field.getZ() + r.nextInt(11) - 5, gull.field.getY());
                        if (crop != null) {
                            return crop;
                        }
                    }
                }
                // eaten bare, guarded by a scarecrow or too far away: forget it
                gull.field = null;
            }
            for (int i = 0; i < 64; i++) {
                BlockPos crop = this.cropAt(level, origin.getX() + r.nextInt(CROP_SEARCH * 2 + 1) - CROP_SEARCH,
                        origin.getZ() + r.nextInt(CROP_SEARCH * 2 + 1) - CROP_SEARCH, origin.getY());
                if (crop != null) {
                    return crop;
                }
            }
            return null;
        }

        /** The crop on top of this column if a gull may peck it (crops do not block motion: it is the top block). */
        private @Nullable BlockPos cropAt(ServerLevel level, int x, int z, int nearY) {
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            if (Math.abs(y - nearY) > 24) {
                return null;
            }
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof CropBlock cropBlock && cropBlock.getAge(state) > 0
                    && level.getBlockState(pos.below()).getBlock() instanceof FarmlandBlock
                    && level.canSeeSky(pos.above())
                    && !scarecrowGuards(level, pos)) {
                return pos;
            }
            return null;
        }

        @Override
        public void start() {
            this.peckTicks = 0;
            this.approachTicks = 0;
            if (this.crop != null) {
                SeagullEntity.this.field = this.crop;
                SeagullEntity.this.flyTo(Vec3.atBottomCenterOf(this.crop).add(0.0, 0.1, 0.0), 1.2);
            }
        }

        @Override
        public boolean canContinueToUse() {
            SeagullEntity gull = SeagullEntity.this;
            if (this.crop == null || gull.fleeTicks > 0 || !(gull.level() instanceof ServerLevel level)) {
                return false;
            }
            BlockState state = level.getBlockState(this.crop);
            if (!(state.getBlock() instanceof CropBlock cropBlock) || cropBlock.getAge(state) == 0) {
                return false;
            }
            Player near = level.getNearestPlayer(gull, 3.0);
            return near == null || near.isCrouching();
        }

        @Override
        public void tick() {
            SeagullEntity gull = SeagullEntity.this;
            if (this.crop == null || !(gull.level() instanceof ServerLevel level)) {
                return;
            }
            Vec3 target = Vec3.atBottomCenterOf(this.crop);
            double dx = target.x - gull.getX();
            double dz = target.z - gull.getZ();
            boolean close = dx * dx + dz * dz < 1.0 && Math.abs(target.y - gull.getY()) < 1.2;
            gull.getLookControl().setLookAt(target.x, target.y, target.z);
            if (!close) {
                if (++this.approachTicks > 300) {
                    // cannot get there (fenced in, roofed over): give up and forget the field
                    this.crop = null;
                    gull.field = null;
                    return;
                }
                if (gull.getNavigation().isDone() && gull.aiTick(4)) {
                    gull.flyTo(target.add(0.0, 0.1, 0.0), 1.2);
                }
                return;
            }
            gull.getNavigation().stop();
            this.peckTicks++;
            if (this.peckTicks % 10 == 0) {
                level.broadcastEntityEvent(gull, EVENT_PECK);
                gull.playSound(ShoreSounds.SEAGULL_PECK, 0.7F, 0.9F + gull.random.nextFloat() * 0.3F);
                BlockState state = level.getBlockState(this.crop);
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                        target.x, target.y + 0.2, target.z, 5, 0.15, 0.1, 0.15, 0.05);
            }
            if (this.peckTicks >= 40) {
                BlockState state = level.getBlockState(this.crop);
                if (state.getBlock() instanceof CropBlock cropBlock && cropBlock.getAge(state) > 0) {
                    level.setBlock(this.crop, cropBlock.getStateForAge(cropBlock.getAge(state) - 1), Block.UPDATE_CLIENTS);
                }
                gull.peckCooldown = 600 + gull.random.nextInt(600);
                gull.restTicks = 20 + gull.random.nextInt(60);
                this.crop = null;
            }
        }

        @Override
        public void stop() {
            this.crop = null;
            SeagullEntity.this.peckCooldown = Math.max(SeagullEntity.this.peckCooldown, 200);
        }
    }

    /**
     * Flock members fly in formation: while the leader flies, each gull steers every tick towards a point a few blocks
     * ahead of its place in the V (so it flies wide, smooth curves instead of turning on the spot), a bit faster when
     * it lags behind and a bit slower when it is ahead. When the leader lands they land near it. 3.1.0 gave every
     * gull a new path to a fixed point next to the leader every 8 ticks and stopped following within 4 blocks: the
     * flock kept turning away and catching up, and banked hard at every jerk.
     */
    private class FollowFlockGoal extends Goal {
        FollowFlockGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            SeagullEntity gull = SeagullEntity.this;
            SeagullEntity lead = gull.leader;
            if (lead == null || !lead.isAlive()) {
                return false;
            }
            if (!lead.isFlying()) {
                // leader sits: take off only if far away
                return gull.distanceToSqr(lead) > 14 * 14;
            }
            return gull.distanceToSqr(lead) < 40 * 40;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            SeagullEntity.this.getNavigation().stop();
        }

        @Override
        public void tick() {
            SeagullEntity gull = SeagullEntity.this;
            SeagullEntity lead = gull.leader;
            if (lead == null) {
                return;
            }
            if (!lead.isFlying()) {
                if (gull.aiTick(8) || gull.getNavigation().isDone() && gull.aiTick(4)) {
                    gull.flyTo(gull.flockSlot(lead).add(0.0, 3.0, 0.0), 1.0);
                }
                return;
            }
            Vec3 slot = gull.flockSlot(lead).add(0.0, 0.5, 0.0);
            Vec3 heading = Vec3.directionFromRotation(0.0F, lead.getYRot());
            Vec3 toSlot = slot.subtract(gull.position());
            double along = toSlot.dot(heading);                 // > 0: the gull is behind its place
            double speed = Mth.clamp(1.0 + along * 0.08, 0.8, 1.45);
            Vec3 aim = slot.add(heading.scale(6.0));
            gull.getNavigation().stop();
            gull.getMoveControl().setWantedPosition(aim.x, aim.y, aim.z, speed);
        }

        @Override
        public boolean canContinueToUse() {
            SeagullEntity lead = SeagullEntity.this.leader;
            return lead != null && lead.isAlive()
                    && (lead.isFlying() ? SeagullEntity.this.distanceToSqr(lead) < 40 * 40 : SeagullEntity.this.distanceToSqr(lead) > 10 * 10);
        }
    }

    /** After some time in the air the gull looks for a post, a raft or a beach to sit on (or the water). */
    private class LandGoal extends Goal {
        private @Nullable Vec3 spot;
        private int time;

        LandGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            SeagullEntity gull = SeagullEntity.this;
            if (!gull.isFlying()) {
                return false;
            }
            SeagullEntity lead = gull.leader;
            boolean leaderSits = lead != null && !lead.isFlying() && gull.distanceToSqr(lead) < 14 * 14;
            if (!leaderSits && gull.flightTicks < gull.flightLimit) {
                return false;
            }
            Vec3 around = leaderSits ? lead.position() : gull.position();
            BlockPos pos = gull.findLandingSpot(around, leaderSits ? 5 : 12);
            if (pos != null) {
                this.spot = Vec3.atBottomCenterOf(pos);
            } else if (gull.flightTicks > gull.flightLimit * 2 || leaderSits) {
                // nothing to sit on out here: settle on the water
                this.spot = new Vec3(gull.getX() + (gull.random.nextDouble() - 0.5) * 8.0, gull.level().getSeaLevel() + 0.1,
                        gull.getZ() + (gull.random.nextDouble() - 0.5) * 8.0);
            } else {
                return false;
            }
            return true;
        }

        @Override
        public void start() {
            this.time = 0;
            if (this.spot != null) {
                SeagullEntity.this.flyTo(this.spot, 1.0);
            }
        }

        @Override
        public boolean canContinueToUse() {
            SeagullEntity gull = SeagullEntity.this;
            return this.spot != null && this.time < 400 && gull.isFlying() && gull.fleeTicks == 0;
        }

        @Override
        public void tick() {
            this.time++;
            SeagullEntity gull = SeagullEntity.this;
            if (this.spot != null && (gull.getNavigation().isDone() && this.time % 5 == 0 || this.time % 40 == 0)) {
                gull.flyTo(this.spot, 1.0);
            }
        }

        @Override
        public void stop() {
            SeagullEntity gull = SeagullEntity.this;
            this.spot = null;
            if (!gull.isFlying()) {
                gull.restTicks = 300 + gull.random.nextInt(600);
                gull.flightLimit = 300 + gull.random.nextInt(500);
            } else {
                gull.flightTicks = Math.max(0, gull.flightTicks - 100);
            }
        }
    }

    /** Takes off after resting and cruises around the island (or circles a raft with a player on it). */
    private class WanderGoal extends Goal {
        WanderGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            SeagullEntity gull = SeagullEntity.this;
            if (gull.isFlying()) {
                return gull.getNavigation().isDone();
            }
            return gull.restTicks <= 0 && (gull.leader == null || gull.leader.isFlying());
        }

        @Override
        public void start() {
            SeagullEntity gull = SeagullEntity.this;
            Vec3 target = gull.pickWanderTarget();
            if (!gull.isFlying()) {
                // take off: first straight up a bit
                gull.setDeltaMovement(gull.getDeltaMovement().add(0.0, 0.35, 0.0));
                gull.restTicks = 0;
            }
            gull.flyTo(target, 1.0);
        }

        @Override
        public boolean canContinueToUse() {
            return !SeagullEntity.this.getNavigation().isDone();
        }
    }

    /** Short hops on the ground while resting. */
    private class HopGoal extends Goal {
        HopGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            SeagullEntity gull = SeagullEntity.this;
            return gull.onGround() && !gull.isInWater() && gull.random.nextInt(120) == 0;
        }

        @Override
        public void start() {
            SeagullEntity gull = SeagullEntity.this;
            double a = gull.random.nextDouble() * Math.PI * 2.0;
            double d = 1.0 + gull.random.nextDouble() * 1.5;
            gull.getNavigation().moveTo(gull.getX() + Math.cos(a) * d, gull.getY(), gull.getZ() + Math.sin(a) * d, 0.7);
        }

        @Override
        public boolean canContinueToUse() {
            return SeagullEntity.this.onGround() && !SeagullEntity.this.getNavigation().isDone();
        }
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }
}
