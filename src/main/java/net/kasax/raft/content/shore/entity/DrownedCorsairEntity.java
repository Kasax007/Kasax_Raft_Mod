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
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Drowned corsair: a cursed undead pirate of Saltjaw's crew. Swims like a drowned, does not burn in sunlight, fights
 * with a cutlass, attacks players on land and in the water and climbs out of the sea onto rafts and decks.
 */
public class DrownedCorsairEntity extends Drowned {
    /** Structures where corsairs may also spawn on dry ground (pirate cove, galleon wreck). */
    public static final TagKey<Structure> HAUNTS = TagKey.create(Registries.STRUCTURE, Raft.id("corsair_haunts"));
    /** Natural spawning stops while this many corsairs are within {@link #NEARBY_RANGE} blocks. */
    private static final int MAX_NEARBY = 6;
    private static final double NEARBY_RANGE = 32.0;

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

    /**
     * Like a drowned, a corsair swims after prey in the water and walks out onto beaches after prey on land. Prey on a
     * raft, a boat or a pier above the water is chased swimming too, up to the surface, where the corsair climbs aboard.
     * (1.21.1: added to the drowned's own wish to swim by {@code shore.mixin.DrownedSwimMixin}.)
     */
    public boolean wantsToSwimExtra() {
        LivingEntity target = this.getTarget();
        if (target == null) {
            return false;
        }
        if (target.getVehicle() != null || this.isAboveWater(target)) {
            return true;
        }
        // prey on the shore above: swim up to the surface next to it and climb out there
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        return this.isUnderWater() && target.getY() > this.getY() + 1.5 && dx * dx + dz * dz < 12.0 * 12.0;
    }

    /** True if there is water within three blocks below the target's feet (raft, boat, pier, jetty). */
    private boolean isAboveWater(LivingEntity target) {
        BlockPos.MutableBlockPos pos = target.blockPosition().mutable();
        for (int i = 0; i < 3; i++) {
            if (this.level().getFluidState(pos.move(0, -1, 0)).is(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
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

    /** Monsters rate spots by darkness, which would forbid every spawn in daylight (these spawn by day too). */
    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        return 0.0F;
    }

    /** Monsters age twice as fast in bright light and would soon despawn in daylight. */
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
     * In the dark deep water of wreck graveyards and drowned shelves (like a drowned, but more often), or inside a
     * pirate cove or galleon wreck, there also by day and on dry ground, but not in lit rooms. Never more than
     * {@value #MAX_NEARBY} corsairs around one spot, so a haunt does not fill up with the whole monster cap. Never in
     * peaceful.
     */
    public static boolean checkCorsairSpawnRules(EntityType<DrownedCorsairEntity> type, ServerLevelAccessor level,
                                                 MobSpawnType reason, BlockPos pos, RandomSource random) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        if (MobSpawnType.isSpawner(reason)) {
            return true;
        }
        boolean water = level.getFluidState(pos).is(FluidTags.WATER);
        boolean place;
        if (water && pos.getY() < level.getSeaLevel() - 6 && random.nextInt(12) == 0
                && (MobSpawnType.ignoresLightRequirements(reason) || Monster.isDarkEnoughToSpawn(level, pos, random))) {
            place = true;
        } else {
            place = random.nextInt(4) == 0 && level.getBrightness(LightLayer.BLOCK, pos) <= 8
                    && (water || level.getBlockState(pos.below()).isValidSpawn(level, pos.below(), type)
                    && level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir())
                    && level.getLevel().structureManager().getStructureWithPieceAt(pos, HAUNTS).isValid();
        }
        return place && level.getEntitiesOfClass(DrownedCorsairEntity.class, new AABB(pos).inflate(NEARBY_RANGE)).size() < MAX_NEARBY;
    }
}
