package net.kasax.raft.content.marine.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

/**
 * Base class of the hostile sea creatures (shark, hammerhead, anglerfish, moray eel). It behaves like a fish that bites:
 * it breathes water, dries out and flops on land, swims with smooth steering and is not bound to darkness: sharks
 * hunt in the sunlit surface water, so the darkness-based rules of {@link Monster} (spawn value, faster ageing in
 * light) are replaced by water-based ones.
 */
public abstract class AbstractMarineMonster extends Monster {
    /** Entity event: the mob snaps its jaws (attack animation on the client). */
    public static final byte EVENT_BITE = 4;

    /** Client-side countdown of the bite animation (ticks). */
    public int biteAnimation;

    protected AbstractMarineMonster(EntityType<? extends AbstractMarineMonster> type, Level level, int maxTurnX, int maxTurnY) {
        super(type, level);
        this.setPathfindingMalus(PathType.WATER, 0.0F);
        this.moveControl = new SmoothSwimmingMoveControl<>(this, maxTurnX, maxTurnY, 0.02F, 0.1F, true);
        this.lookControl = new SmoothSwimmingLookControl(this, 10);
    }

    /** Monster rates a spot by darkness, which fails in sunlit water. Sea creatures rate water instead. */
    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        return level.getFluidState(pos).is(FluidTags.WATER) ? 10.0F : 0.0F;
    }

    /** Monster ages twice as fast in bright light (it would soon despawn in sunlit water). */
    @Override
    protected void updateNoActionTime() {
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WaterBoundPathNavigation(this, level);
    }

    @Override
    protected void travelInWater(Vec3 input, double baseGravity, boolean isFalling, double oldY) {
        this.moveRelative(this.getSpeed(), input);
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().scale(0.9));
        if (this.getTarget() == null && this.sinksWhenIdle()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.005, 0.0));
        }
    }

    /** Idle drift towards the floor (sharks) – lurkers that hover in place return false. */
    protected boolean sinksWhenIdle() {
        return true;
    }

    @Override
    public boolean checkSpawnObstruction(LevelReader level) {
        return level.isUnobstructed(this);
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    /** Sea monsters cannot leave the water: a shark circling the raft must not keep the player out of the hammock. */
    @Override
    public boolean isPreventingPlayerRest(ServerLevel level, Player player) {
        return false;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 120;
    }

    @Override
    public void baseTick() {
        int air = this.getAirSupply();
        super.baseTick();
        if (this.level() instanceof ServerLevel serverLevel) {
            if (this.isAlive() && !this.isInWater()) {
                this.setAirSupply(air - 1);
                if (this.shouldTakeDrowningDamage()) {
                    this.setAirSupply(0);
                    this.hurtServer(serverLevel, this.damageSources().drown(), 2.0F);
                }
            } else {
                this.setAirSupply(300);
            }
        }
    }

    @Override
    public void aiStep() {
        if (!this.isInWater() && this.onGround() && this.verticalCollision) {
            this.setDeltaMovement(this.getDeltaMovement().add((this.random.nextFloat() * 2.0F - 1.0F) * 0.05F, 0.4F,
                    (this.random.nextFloat() * 2.0F - 1.0F) * 0.05F));
            this.setOnGround(false);
            this.needsSync = true;
            this.playSound(this.getFlopSound(), this.getSoundVolume(), this.getVoicePitch());
        }
        super.aiStep();
        if (this.biteAnimation > 0) {
            this.biteAnimation--;
        }
    }

    protected SoundEvent getFlopSound() {
        return SoundEvents.COD_FLOP;
    }

    /** Every melee bite shows the jaw animation on the clients. */
    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        this.level().broadcastEntityEvent(this, EVENT_BITE);
        return super.doHurtTarget(level, target);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_BITE) {
            this.biteAnimation = 10;
        } else {
            super.handleEntityEvent(id);
        }
    }

    /** 0..1 how far the jaw is open for the bite animation (client). */
    public float getBiteAmount(float partialTicks) {
        if (this.biteAnimation <= 0) {
            return 0.0F;
        }
        float t = (10.0F - (this.biteAnimation - partialTicks)) / 10.0F;
        // snap open fast, close a little slower
        return t < 0.3F ? t / 0.3F : Math.max(0.0F, 1.0F - (t - 0.3F) / 0.7F);
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
    }

    /** True if the column at pos is water for at least {@code depth} blocks downwards. */
    protected static boolean isWaterColumn(LevelReader level, BlockPos pos, int depth) {
        for (int i = 0; i < depth; i++) {
            if (!level.getFluidState(pos.below(i)).is(FluidTags.WATER)) {
                return false;
            }
        }
        return true;
    }
}
