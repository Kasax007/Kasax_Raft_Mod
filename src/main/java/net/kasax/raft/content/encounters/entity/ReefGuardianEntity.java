package net.kasax.raft.content.encounters.entity;

import net.kasax.raft.Raft;
import net.kasax.raft.content.encounters.EliteBar;
import net.kasax.raft.content.encounters.EncounterSounds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Reef guardian: a guardian overgrown by the reef that watches sunken ruins and the coral temple. Like a guardian it
 * charges a beam for a few seconds before it hits (clearly telegraphed: the beam glows brighter and the guardian
 * hums) and its spikes hurt attackers. The Reef Warden ({@link #makeWarden}) is the temple's elite, with a boss bar.
 */
public class ReefGuardianEntity extends Guardian {
    private final EliteBar bar = new EliteBar(this, BossEvent.BossBarColor.PINK);
    private boolean warden;

    public ReefGuardianEntity(EntityType<? extends ReefGuardianEntity> type, Level level) {
        super(type, level);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Guardian.createAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.ARMOR, 4.0);
    }

    /** Reef Warden: 1.7x size, 80 health, 7 beam damage, a boss bar. */
    public void makeWarden() {
        this.warden = true;
        EliteBar.makeElite(this, 1.7, 80.0, 7.0, 8.0);
        this.setCustomName(Component.translatable("entity.raft.reef_warden"));
        this.bar.setVisible(true);
        this.xpReward = 40;
    }

    public boolean isWarden() {
        return this.warden;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (this.warden) {
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
        if (this.warden) {
            drop(level, "pearl", 3 + this.random.nextInt(3));
            drop(level, "sea_glass_shard", 2 + this.random.nextInt(3));
            this.spawnAtLocation(level, new ItemStack(Items.PRISMARINE_CRYSTALS, 3 + this.random.nextInt(4)));
            if (this.random.nextFloat() < 0.3F) {
                drop(level, "ancient_energy", 1);
            }
        }
    }

    private void drop(ServerLevel level, String id, int count) {
        BuiltInRegistries.ITEM.getOptional(Raft.id(id)).ifPresent(item -> this.spawnAtLocation(level, new ItemStack(item, count)));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Warden", this.warden);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.warden = input.getBooleanOr("Warden", false);
        this.bar.setVisible(this.warden);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.isInWater() ? EncounterSounds.REEF_GUARDIAN_AMBIENT : super.getAmbientSound();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return this.isInWater() ? EncounterSounds.REEF_GUARDIAN_HURT : super.getHurtSound(source);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return this.isInWater() ? EncounterSounds.REEF_GUARDIAN_DEATH : super.getDeathSound();
    }
}
