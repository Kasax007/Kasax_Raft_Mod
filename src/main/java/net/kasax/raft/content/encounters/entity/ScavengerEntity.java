package net.kasax.raft.content.encounters.entity;

import net.kasax.raft.Raft;
import net.kasax.raft.content.encounters.EliteBar;
import net.kasax.raft.content.encounters.EncounterSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Scavenger: a living sea scavenger who loots the wrecks and ruins of the Endless Sea and defends his finds. Fights
 * with what he carries (fists, a wooden or stone blade, an iron sword or a bow: the bow keeps him at a distance like
 * a skeleton), does not burn in sunlight and calls the others of his gang when he is hit. A Scavenger Chief
 * ({@link #makeChief}) is the elite of a gang: bigger, armoured, with a boss bar.
 */
public class ScavengerEntity extends AbstractSkeleton {
    private final EliteBar bar = new EliteBar(this, BossEvent.BossBarColor.YELLOW);
    private boolean chief;

    public ScavengerEntity(EntityType<? extends ScavengerEntity> type, Level level) {
        super(type, level);
        this.xpReward = 6;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void registerGoals() {
        // no sun avoidance (a living man), no fear of wolves: the weapon goal (bow or melee) comes from reassessWeaponGoal
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, ScavengerEntity.class).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // ------------------------------------------------------------------------------------------- kits
    public enum Kit { LIGHT, ARCHER, MID, RIG, CHIEF }

    /** Equips the scavenger for its place on the difficulty curve (see StructureEncounters). */
    public void equip(Kit kit, RandomSource random) {
        switch (kit) {
            case LIGHT -> this.hand(random.nextFloat() < 0.5F ? Items.WOODEN_SWORD : random.nextFloat() < 0.5F ? Items.WOODEN_AXE : null);
            case ARCHER -> this.hand(Items.BOW);
            case MID -> {
                this.hand(random.nextBoolean() ? Items.STONE_SWORD : Items.STONE_AXE);
                this.wear(EquipmentSlot.CHEST, Items.LEATHER_CHESTPLATE);
            }
            case RIG -> {
                this.hand(random.nextFloat() < 0.4F ? Items.BOW : Items.STONE_SWORD);
                this.wear(EquipmentSlot.CHEST, Items.LEATHER_CHESTPLATE);
                this.wear(EquipmentSlot.LEGS, Items.LEATHER_LEGGINGS);
            }
            case CHIEF -> this.makeChief();
        }
    }

    private void hand(Item item) {
        this.setItemSlot(EquipmentSlot.MAINHAND, item == null ? ItemStack.EMPTY : new ItemStack(item));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.04F);
    }

    private void wear(EquipmentSlot slot, Item item) {
        this.setItemSlot(slot, new ItemStack(item));
        this.setDropChance(slot, 0.04F);
    }

    /** Scavenger Chief: 1.15x size, 50 health, iron sword (7 damage), iron helmet and chestplate, a boss bar. */
    public void makeChief() {
        this.chief = true;
        this.hand(Items.IRON_SWORD);
        this.wear(EquipmentSlot.HEAD, Items.IRON_HELMET);
        this.wear(EquipmentSlot.CHEST, Items.IRON_CHESTPLATE);
        EliteBar.makeElite(this, 1.15, 50.0, 2.0, 4.0);
        this.setCustomName(Component.translatable("entity.raft.scavenger_chief"));
        this.bar.setVisible(true);
        this.xpReward = 25;
    }

    public boolean isChief() {
        return this.chief;
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        // spawn eggs and commands: a light kit; encounters equip their own (equip)
        this.equip(random.nextFloat() < 0.3F ? Kit.ARCHER : Kit.LIGHT, random);
    }

    // ------------------------------------------------------------------------------------------- elite
    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (this.chief) {
            this.bar.tick();
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bar.seen(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bar.unseen(player);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        if (this.chief) {
            // the chief's stash: coins and scrap, often a harpoon gun
            drop(level, "gold_coin", 3 + this.random.nextInt(4));
            drop(level, "scrap_metal", 2 + this.random.nextInt(3));
            if (this.random.nextFloat() < 0.35F) {
                drop(level, "harpoon_gun", 1);
            }
        }
    }

    private void drop(ServerLevel level, String id, int count) {
        BuiltInRegistries.ITEM.getOptional(Raft.id(id)).ifPresent(item -> this.spawnAtLocation(level, new ItemStack(item, count)));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Chief", this.chief);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.chief = input.getBooleanOr("Chief", false);
        this.bar.setVisible(this.chief);
    }

    // ------------------------------------------------------------------------------------------- misc
    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        return 0.0F;
    }

    @Override
    public boolean isShaking() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return EncounterSounds.SCAVENGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return EncounterSounds.SCAVENGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return EncounterSounds.SCAVENGER_DEATH;
    }

    @Override
    protected SoundEvent getStepSound() {
        return net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_LEATHER.value();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        SoundType type = state.getSoundType();
        this.playSound(type.getStepSound(), type.getVolume() * 0.15F, type.getPitch());
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * (this.chief ? 0.85F : 1.0F);
    }
}
