package net.kasax.raft.content.marine.entity;

import net.kasax.raft.Raft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;


/**
 * A jellyfish that drifts with the current and pulses its bell to swim. Three variants (synced): moon (white),
 * crimson (red) and glow (cyan, rendered full-bright). Touching it stings: poison for 3 seconds and a little damage,
 * unless the victim wears the {@code raft:wetsuit}. The glow variant drops {@code glow_jelly}, the others
 * {@code jelly_blob} (loot tables {@code entities/jellyfish} and {@code entities/jellyfish_glow}).
 */
public class JellyfishEntity extends WaterAnimal {
    public enum Variant {
        MOON("moon"), CRIMSON("crimson"), GLOW("glow");

        public final String id;

        Variant(String id) {
            this.id = id;
        }

        public static Variant byId(int i) {
            return values()[Math.floorMod(i, values().length)];
        }

        public static Variant byName(String name) {
            for (Variant v : values()) {
                if (v.id.equals(name)) {
                    return v;
                }
            }
            return MOON;
        }
    }

    public static final byte EVENT_PULSE = 92;
    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(JellyfishEntity.class, EntityDataSerializers.INT);
    private static final ResourceKey<LootTable> GLOW_LOOT = ResourceKey.create(Registries.LOOT_TABLE, Raft.id("entities/jellyfish_glow"));
    private static @Nullable Item wetsuit;

    /** Server: ticks until the next pulse. Client: ticks since the last pulse (animation). */
    private int pulseTimer;
    public int clientPulseAge = 100;
    private Vec3 drift = Vec3.ZERO;
    private int stingCooldown;

    public JellyfishEntity(EntityType<? extends JellyfishEntity> type, Level level) {
        super(type, level);
        this.pulseTimer = 20 + this.random.nextInt(40);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 6.0).add(Attributes.MOVEMENT_SPEED, 0.02);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VARIANT, 0);
    }

    public Variant getVariant() {
        return Variant.byId(this.entityData.get(DATA_VARIANT));
    }

    public void setVariant(Variant variant) {
        this.entityData.set(DATA_VARIANT, variant.ordinal());
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("Variant", this.getVariant().id);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setVariant(Variant.byName(input.getStringOr("Variant", "moon")));
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
                                                  @Nullable SpawnGroupData groupData) {
        this.setVariant(pickVariant(level, this.blockPosition(), this.random));
        return super.finalizeSpawn(level, difficulty, reason, groupData);
    }

    /** Glowing lagoons and the trench breed glow jellies; elsewhere moon and crimson jellies, glow ones are rare. */
    private static Variant pickVariant(ServerLevelAccessor level, BlockPos pos, RandomSource random) {
        var biome = level.getBiome(pos);
        ResourceKey<Biome> key = biome.unwrapKey().orElse(null);
        String path = key == null ? "" : key.identifier().getPath();
        if (path.equals("glowing_lagoon")) {
            return random.nextInt(6) == 0 ? Variant.MOON : Variant.GLOW;
        }
        if (path.equals("abyssal_trench")) {
            return random.nextInt(3) == 0 ? Variant.CRIMSON : Variant.GLOW;
        }
        int r = random.nextInt(10);
        return r < 5 ? Variant.MOON : r < 9 ? Variant.CRIMSON : Variant.GLOW;
    }

    /** Glow jellies use their own loot table ({@code raft:entities/jellyfish_glow}). */
    @Override
    protected void dropFromLootTable(ServerLevel level, DamageSource source, boolean playerKilled) {
        if (this.getVariant() == Variant.GLOW) {
            this.dropFromLootTable(level, source, playerKilled, GLOW_LOOT);
        } else {
            super.dropFromLootTable(level, source, playerKilled);
        }
    }

    // ------------------------------------------------------------------------------------------------ movement
    @Override
    public void travel(Vec3 input) {
        this.move(MoverType.SELF, this.getDeltaMovement());
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide()) {
            this.clientPulseAge++;
            if (this.getVariant() == Variant.GLOW && this.random.nextInt(12) == 0) {
                this.level().addParticle(ParticleTypes.GLOW, this.getRandomX(0.5), this.getY() + this.random.nextDouble() * 0.4,
                        this.getRandomZ(0.5), 0.0, -0.01, 0.0);
            }
            return;
        }
        if (this.isInWater()) {
            Vec3 v = this.getDeltaMovement().scale(0.94);
            // slow sinking between pulses plus a gentle current
            v = v.add(this.drift.x * 0.004, -0.0025, this.drift.z * 0.004);
            if (--this.pulseTimer <= 0) {
                this.pulse();
                v = v.add(this.drift.x * 0.05, 0.07 + this.random.nextDouble() * 0.04, this.drift.z * 0.05);
            }
            // stay out of the surface layer and off the floor
            BlockPos p = this.blockPosition();
            if (!this.level().getFluidState(p.above()).is(FluidTags.WATER)) {
                v = new Vec3(v.x, Math.min(v.y, -0.01), v.z);
            }
            this.setDeltaMovement(v);
        } else {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.04, 0.0).multiply(0.9, 1.0, 0.9));
        }
        if (this.stingCooldown > 0) {
            this.stingCooldown--;
        } else if (this.isAlive()) {
            this.stingTouching((ServerLevel) this.level());
        }
    }

    private void pulse() {
        this.pulseTimer = 40 + this.random.nextInt(40);
        if (this.drift == Vec3.ZERO || this.random.nextInt(4) == 0) {
            float a = this.random.nextFloat() * Mth.TWO_PI;
            this.drift = new Vec3(Mth.cos(a), 0, Mth.sin(a));
        }
        this.level().broadcastEntityEvent(this, EVENT_PULSE);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_PULSE) {
            this.clientPulseAge = 0;
        } else {
            super.handleEntityEvent(id);
        }
    }

    // ------------------------------------------------------------------------------------------------ sting
    private static @Nullable Item wetsuit() {
        if (wetsuit == null) {
            Item item = BuiltInRegistries.ITEM.getValue(Raft.id("wetsuit"));
            wetsuit = item == Items.AIR ? null : item;
        }
        return wetsuit;
    }

    /** True if the entity wears the wetsuit (immune to stings). */
    public static boolean isProtected(LivingEntity entity) {
        Item suit = wetsuit();
        return suit != null && entity.getItemBySlot(EquipmentSlot.LEGS).is(suit);
    }

    private void stingTouching(ServerLevel level) {
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.15), this::canSting)) {
            if (isProtected(e)) {
                continue;
            }
            if (e.hurtServer(level, this.damageSources().mobAttack(this), 1.0F)) {
                e.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0), this);
                this.playSound(SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH, 0.8F, 1.4F);
                this.stingCooldown = 20;
            }
        }
    }

    private boolean canSting(Entity e) {
        if (e == this || !e.isAlive() || e instanceof WaterAnimal || e instanceof AbstractMarineMonster) {
            return false;
        }
        return !(e instanceof Player p) || !p.isSpectator() && !p.isCreative();
    }

    // ------------------------------------------------------------------------------------------------ misc
    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return true;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SLIME_HURT_SMALL;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SLIME_DEATH_SMALL;
    }

    @Override
    protected float getSoundVolume() {
        return 0.5F;
    }

    /** Natural spawning: water at least 2 blocks below the surface and 40 at most. */
    public static boolean checkJellyfishSpawnRules(EntityType<JellyfishEntity> type, ServerLevelAccessor level, EntitySpawnReason reason,
                                                   BlockPos pos, RandomSource random) {
        int sea = level.getSeaLevel();
        return pos.getY() <= sea - 2 && pos.getY() >= sea - 60
                && level.getFluidState(pos).is(FluidTags.WATER)
                && level.getFluidState(pos.above()).is(FluidTags.WATER);
    }
}
