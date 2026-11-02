package net.kasax.raft.content.marine.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

/**
 * A moray eel. It finds a hole in the reef or a ruin (a water block walled in on most sides), sits in it with its
 * head out, gaping, and lunges at anyone who swims within 4 blocks. After a bite it pulls back into its hole.
 * It never strays more than a few blocks from home.
 */
public class MorayEelEntity extends AbstractMarineMonster {
    private static final int LUNGE_RANGE = 4;
    private @Nullable BlockPos home;
    private int homeSearchCooldown;
    /** Synced via entity events: client side "is lunging" for the wide-open jaw. */
    public int lungeAnim;

    public MorayEelEntity(EntityType<? extends MorayEelEntity> type, Level level) {
        super(type, level, 40, 15);
        this.xpReward = 6;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 16.0)
                .add(Attributes.MOVEMENT_SPEED, 1.0)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 8.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new LungeGoal());
        this.goalSelector.addGoal(3, new HideInHoleGoal());
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true,
                (target, level) -> target.isInWater() && !target.isSpectator()
                        && MorayEelEntity.this.distanceToSqr(target) < (LUNGE_RANGE + 1) * (LUNGE_RANGE + 1)));
    }

    @Override
    protected boolean sinksWhenIdle() {
        return false;
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
                                                  @Nullable SpawnGroupData groupData) {
        this.home = findHole(level, this.blockPosition(), this.random);
        return super.finalizeSpawn(level, difficulty, reason, groupData);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (this.home != null) {
            output.putInt("HoleX", this.home.getX());
            output.putInt("HoleY", this.home.getY());
            output.putInt("HoleZ", this.home.getZ());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        int y = input.getIntOr("HoleY", Integer.MIN_VALUE);
        if (y != Integer.MIN_VALUE) {
            this.home = new BlockPos(input.getIntOr("HoleX", 0), y, input.getIntOr("HoleZ", 0));
        }
    }

    public @Nullable BlockPos getHome() {
        return this.home;
    }

    /** How walled-in a water block is: solid neighbours (0..6). */
    private static int enclosure(LevelReader level, BlockPos p) {
        int n = 0;
        for (Direction d : Direction.values()) {
            BlockPos q = p.relative(d);
            if (level.getBlockState(q).isFaceSturdy(level, q, d.getOpposite())) {
                n++;
            }
        }
        return n;
    }

    /** The best hole (most walled-in water block with an open side) within 8 blocks, or the start if none. */
    private static BlockPos findHole(LevelReader level, BlockPos start, RandomSource random) {
        BlockPos best = null;
        int bestScore = 2;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int i = 0; i < 300; i++) {
            m.set(start.getX() + random.nextInt(17) - 8, start.getY() + random.nextInt(9) - 4, start.getZ() + random.nextInt(17) - 8);
            if (!level.getFluidState(m).is(FluidTags.WATER)) {
                continue;
            }
            int e = enclosure(level, m);
            if (e > bestScore && e < 6) {
                bestScore = e;
                best = m.immutable();
            }
        }
        return best != null ? best : start;
    }

    /** Direction from the hole to open water (where the head should point). */
    private @Nullable Direction openSide(BlockPos hole) {
        Level level = this.level();
        Direction best = null;
        int bestOpen = -1;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            int open = 0;
            for (int i = 1; i <= 3; i++) {
                if (level.getFluidState(hole.relative(d, i)).is(FluidTags.WATER)) {
                    open++;
                } else {
                    break;
                }
            }
            if (open > bestOpen) {
                bestOpen = open;
                best = d;
            }
        }
        return best;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide()) {
            if (this.lungeAnim > 0) {
                this.lungeAnim--;
            }
            return;
        }
        if (this.home == null && --this.homeSearchCooldown <= 0) {
            this.homeSearchCooldown = 200;
            this.home = findHole(this.level(), this.blockPosition(), this.random);
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_BITE) {
            this.lungeAnim = 14;
        }
        super.handleEntityEvent(id);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SALMON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SALMON_DEATH;
    }

    @Override
    protected SoundEvent getFlopSound() {
        return SoundEvents.SALMON_FLOP;
    }

    @Override
    public void playAttackSound() {
        this.playSound(SoundEvents.EVOKER_FANGS_ATTACK, 0.6F, 1.8F);
    }

    /** Natural spawning: water near the floor of reefs and ruins (solid block within 2 below), never in peaceful. */
    public static boolean checkMoraySpawnRules(EntityType<MorayEelEntity> type, ServerLevelAccessor level, EntitySpawnReason reason,
                                               BlockPos pos, RandomSource random) {
        if (level.getDifficulty() == Difficulty.PEACEFUL || pos.getY() > level.getSeaLevel() - 3) {
            return false;
        }
        if (!level.getFluidState(pos).is(FluidTags.WATER) || !level.getFluidState(pos.above()).is(FluidTags.WATER)) {
            return false;
        }
        return !level.getFluidState(pos.below()).is(FluidTags.WATER) || !level.getFluidState(pos.below(2)).is(FluidTags.WATER);
    }

    // ------------------------------------------------------------------------------------------------ goals
    /** Return to the hole and sit in it, facing open water. */
    private class HideInHoleGoal extends Goal {
        HideInHoleGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return MorayEelEntity.this.home != null && MorayEelEntity.this.getTarget() == null && MorayEelEntity.this.isInWater();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            MorayEelEntity e = MorayEelEntity.this;
            BlockPos h = e.home;
            if (h == null) {
                return;
            }
            Vec3 c = Vec3.atCenterOf(h).subtract(0, 0.3, 0);
            double d = e.position().distanceToSqr(c);
            if (d > 1.0) {
                if (e.tickCount % 10 == 0) {
                    e.getNavigation().moveTo(c.x, c.y, c.z, 0.8);
                }
            } else {
                e.getNavigation().stop();
                e.setDeltaMovement(e.getDeltaMovement().scale(0.5).add(c.subtract(e.position()).scale(0.05)));
                Direction out = e.openSide(h);
                if (out != null) {
                    // head out of the hole, looking into open water
                    e.getLookControl().setLookAt(c.x + out.getStepX() * 4, c.y + 0.2, c.z + out.getStepZ() * 4, 10.0F, 10.0F);
                    float yaw = out.toYRot();
                    e.setYRot(e.rotlerpYaw(yaw, 6.0F));
                    e.yBodyRot = e.getYRot();
                }
            }
        }
    }

    private float rotlerpYaw(float target, float max) {
        float diff = net.minecraft.util.Mth.wrapDegrees(target - this.getYRot());
        return this.getYRot() + net.minecraft.util.Mth.clamp(diff, -max, max);
    }

    /** Lunge at a target within range, bite, then pull back towards the hole. */
    private class LungeGoal extends Goal {
        private int lunge;
        private int cooldown;
        private int retreat;

        LungeGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = MorayEelEntity.this.getTarget();
            return t != null && t.isAlive() && t.isInWater();
        }

        @Override
        public boolean canContinueToUse() {
            MorayEelEntity e = MorayEelEntity.this;
            LivingEntity t = e.getTarget();
            if (t == null || !t.isAlive()) {
                return false;
            }
            // territorial: give up when the target leaves the area around the hole
            BlockPos h = e.home != null ? e.home : e.blockPosition();
            return t.distanceToSqr(Vec3.atCenterOf(h)) < 9.0 * 9.0;
        }

        @Override
        public void start() {
            this.lunge = 0;
            this.retreat = 0;
        }

        @Override
        public void stop() {
            MorayEelEntity e = MorayEelEntity.this;
            if (e.getTarget() != null && e.home != null && e.getTarget().distanceToSqr(Vec3.atCenterOf(e.home)) >= 81.0) {
                e.setTarget(null);
            }
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            MorayEelEntity e = MorayEelEntity.this;
            LivingEntity t = e.getTarget();
            if (t == null) {
                return;
            }
            e.getLookControl().setLookAt(t, 40.0F, 40.0F);
            if (this.cooldown > 0) {
                this.cooldown--;
            }
            if (this.retreat > 0) {
                this.retreat--;
                if (e.home != null) {
                    Vec3 c = Vec3.atCenterOf(e.home);
                    e.setDeltaMovement(e.getDeltaMovement().scale(0.7).add(c.subtract(e.position()).normalize().scale(0.12)));
                }
                return;
            }
            if (this.lunge > 0) {
                this.lunge--;
                Vec3 dir = t.position().add(0, t.getBbHeight() * 0.4, 0).subtract(e.position()).normalize();
                e.setDeltaMovement(e.getDeltaMovement().scale(0.5).add(dir.scale(0.45)));
                if (e.isWithinMeleeAttackRange(t) && e.level() instanceof ServerLevel level) {
                    e.doHurtTarget(level, t);
                    this.lunge = 0;
                    this.cooldown = 40;
                    this.retreat = 25;
                }
                return;
            }
            if (this.cooldown <= 0 && e.distanceToSqr(t) < LUNGE_RANGE * LUNGE_RANGE && e.getSensing().hasLineOfSight(t)) {
                this.lunge = 10;
                e.level().broadcastEntityEvent(e, EVENT_BITE);
                e.playSound(SoundEvents.FISH_SWIM, 0.8F, 1.4F);
            } else if (e.home != null && e.position().distanceToSqr(Vec3.atCenterOf(e.home)) > 2.0) {
                // wait at the hole, ready to strike
                Vec3 c = Vec3.atCenterOf(e.home);
                e.getNavigation().moveTo(c.x, c.y - 0.3, c.z, 1.0);
            } else {
                e.getNavigation().stop();
            }
        }
    }
}
