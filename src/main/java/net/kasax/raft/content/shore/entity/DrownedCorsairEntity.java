package net.kasax.raft.content.shore.entity;

import net.kasax.raft.Raft;
import net.kasax.raft.content.shore.ShoreItems;
import net.kasax.raft.content.shore.ShoreSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Drowned corsair: a cursed undead pirate of Saltjaw's crew. Swims like a drowned, does not burn in sunlight, fights
 * with a cutlass, attacks players on land and in the water and climbs out of the sea onto rafts and decks.
 */
public class DrownedCorsairEntity extends Drowned {
    /** Structures where corsairs may also spawn on dry ground (pirate cove, galleon wreck). */
    public static final TagKey<Structure> HAUNTS = TagKey.create(Registries.STRUCTURE, Raft.id("corsair_haunts"));

    public DrownedCorsairEntity(EntityType<? extends DrownedCorsairEntity> type, Level level) {
        super(type, level);
        this.xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Drowned.createAttributes()
                .add(Attributes.MAX_HEALTH, 24.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.ARMOR, 3.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.0);
    }

    @Override
    protected void addBehaviourGoals() {
        this.goalSelector.addGoal(2, new ZombieAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(7, new RandomStrollGoal(this, 1.0));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
    }

    /** Corsairs chase their prey through the water, not only when the prey is swimming. */
    @Override
    public boolean wantsToSwim() {
        return this.getTarget() != null || super.wantsToSwim();
    }

    @Override
    public boolean okTarget(@Nullable LivingEntity target) {
        return target != null;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        LivingEntity target = this.getTarget();
        // Boarding: climb out of the water onto a raft or a deck next to the target.
        if (!this.level().isClientSide() && target != null && this.isInWater() && this.horizontalCollision
                && target.getY() > this.getY() + 0.4 && this.distanceToSqr(target) < 5.0 * 5.0) {
            Vec3 dir = target.position().subtract(this.position()).multiply(1.0, 0.0, 1.0);
            Vec3 push = dir.lengthSqr() < 1.0E-4 ? Vec3.ZERO : dir.normalize().scale(0.12);
            this.setDeltaMovement(push.x, 0.42, push.z);
        }
    }

    @Override
    public void setBaby(boolean baby) {
        super.setBaby(false);
    }

    @Override
    protected void randomizeReinforcementsChance() {
        this.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE).setBaseValue(0.0);
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        if (random.nextFloat() < 0.6F) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ShoreItems.CUTLASS));
            this.setDropChance(EquipmentSlot.MAINHAND, 0.05F);
        }
    }

    /** Monsters rate spots by darkness; under the always-bright raft sky that would forbid every spawn. */
    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        return 0.0F;
    }

    /** Monsters age twice as fast in bright light and would despawn in seconds under the raft sky. */
    @Override
    protected void updateNoActionTime() {
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.isInWater() ? SoundEvents.DROWNED_AMBIENT_WATER : ShoreSounds.CORSAIR_AMBIENT;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.88F;
    }

    /**
     * In the deep water of wreck graveyards and drowned shelves, or anywhere inside a pirate cove or galleon wreck
     * (also on dry ground there). Never in peaceful.
     */
    public static boolean checkCorsairSpawnRules(EntityType<DrownedCorsairEntity> type, ServerLevelAccessor level,
                                                 EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        if (EntitySpawnReason.isSpawner(reason)) {
            return true;
        }
        boolean haunt = level.getLevel().structureManager().getStructureWithPieceAt(pos, HAUNTS).isValid();
        if (level.getFluidState(pos).is(FluidTags.WATER)) {
            return haunt || pos.getY() < level.getSeaLevel() - 6 && random.nextInt(3) == 0;
        }
        return haunt && level.getBlockState(pos.below()).isValidSpawn(level, pos.below(), type)
                && level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir();
    }
}
