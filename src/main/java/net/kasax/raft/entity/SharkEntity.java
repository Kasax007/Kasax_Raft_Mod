package net.kasax.raft.entity;

import net.kasax.raft.content.audio.AudioSounds;
import net.kasax.raft.content.marine.entity.AbstractMarineMonster;
import net.kasax.raft.content.marine.entity.SharkRaftGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;

/**
 * "Bruce", the shark of the raft dimension. It hunts players that are in the water, dries out on land and – the
 * classic raft threat – circles rafts that a player stands on and now and then bites an edge
 * {@code raft:raft_foundation} until it breaks ({@link SharkRaftGoal}, game rule {@code raft:shark_bites}).
 * Reinforced foundations are immune. Hitting a biting shark scares it away for a while.
 */
public class SharkEntity extends AbstractMarineMonster {
    protected SharkRaftGoal raftGoal;

    public SharkEntity(EntityType<? extends SharkEntity> type, Level level) {
        super(type, level, 85, 10);
        this.xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 26.0)
                .add(Attributes.MOVEMENT_SPEED, 1.3)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.FOLLOW_RANGE, 28.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4);
    }

    @Override
    protected void registerGoals() {
        this.raftGoal = new SharkRaftGoal(this);
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(3, this.raftGoal);
        this.goalSelector.addGoal(4, new RandomSwimmingGoal(this, 1.0, 10));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // Sharks only hunt players that are in the water.
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false,
                (target, level) -> target.isInWater() && !target.isSpectator()));
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        // Sharks only hunt swimmers: a player who climbs back onto the raft is safe again (a shark hit from the
        // raft flees instead, see SharkRaftGoal#onHurt).
        if (this.getTarget() instanceof Player player && !player.isInWater()
                && (this.getLastHurtByMob() != player || SharkRaftGoal.isOnRaft(player))) {
            this.setTarget(null);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && this.raftGoal != null) {
            this.raftGoal.onHurt(source);
        }
        return hurt;
    }


    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return AudioSounds.SHARK_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AudioSounds.SHARK_DEATH;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.isInWater() ? AudioSounds.SHARK_AMBIENT : null;
    }

    @Override
    protected SoundEvent getSwimSound() {
        return SoundEvents.DOLPHIN_SWIM;
    }

    @Override
    public float getVoicePitch() {
        // the shark's own sounds are made for its size (the vanilla dolphin sounds it used before were pitched down)
        return super.getVoicePitch() * 0.95F;
    }

    @Override
    public void playAttackSound() {
        this.playSound(AudioSounds.SHARK_BITE, 1.0F, 0.9F + this.random.nextFloat() * 0.2F);
    }

    /** Natural spawning: in the water in the upper part of the ocean, never in peaceful. */
    public static boolean checkSharkSpawnRules(EntityType<? extends SharkEntity> type, ServerLevelAccessor level, EntitySpawnReason reason,
                                               BlockPos pos, RandomSource random) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        int seaLevel = level.getSeaLevel();
        return pos.getY() >= seaLevel - 20
                && pos.getY() <= seaLevel
                && level.getFluidState(pos).is(FluidTags.WATER)
                && level.getBlockState(pos.above()).is(Blocks.WATER);
    }
}
