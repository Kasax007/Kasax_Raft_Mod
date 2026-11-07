package net.kasax.raft.content.encounters.entity;

import net.kasax.raft.content.encounters.EliteBar;
import net.kasax.raft.content.encounters.EncounterSounds;
import net.kasax.raft.content.shore.ShoreEntities;
import net.kasax.raft.content.shore.ShoreItems;
import net.kasax.raft.content.shore.entity.DrownedCorsairEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Corsair Captain: an officer of Saltjaw's cursed crew who guards the treasure of the pirate cove and the galleon.
 * Bigger than a corsair, armoured, with a cutlass and a boss bar. At half health he blows his horn (a long call, two
 * seconds before anything happens) and two corsairs climb out of the deep to help him: once per fight.
 */
public class CorsairCaptainEntity extends DrownedCorsairEntity {
    private static final int RALLY_DELAY = 40;
    private final EliteBar bar = new EliteBar(this, BossEvent.BossBarColor.RED);
    private boolean rallied;
    private int rallyTicks = -1;

    public CorsairCaptainEntity(EntityType<? extends CorsairCaptainEntity> type, Level level) {
        super(type, level);
        this.xpReward = 40;
        this.bar.setVisible(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return DrownedCorsairEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 80.0)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.SCALE, 1.2);
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ShoreItems.CUTLASS));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);   // the loot table drops a cutlass for sure
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        this.bar.tick();
        if (!this.rallied && this.getHealth() < this.getMaxHealth() * 0.5F && this.getTarget() != null) {
            this.rallied = true;
            this.rallyTicks = RALLY_DELAY;
            level.playSound(null, this.getX(), this.getY(), this.getZ(), EncounterSounds.CAPTAIN_RALLY, SoundSource.HOSTILE, 3.0F, 0.9F);
        }
        if (this.rallyTicks > 0) {
            this.rallyTicks--;
            if (this.rallyTicks % 5 == 0) {
                level.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + this.getBbHeight() + 0.3, this.getZ(), 4, 0.4, 0.2, 0.4, 0.02);
            }
            if (this.rallyTicks == 0) {
                this.rally(level);
            }
        }
    }

    /** Two corsairs appear a few blocks away (water or deck), with a puff of spray. */
    private void rally(ServerLevel level) {
        for (int i = 0; i < 2; i++) {
            for (int tries = 0; tries < 12; tries++) {
                float a = this.random.nextFloat() * Mth.TWO_PI;
                double r = 3.0 + this.random.nextDouble() * 3.0;
                BlockPos pos = BlockPos.containing(this.getX() + Mth.cos(a) * r, this.getY(), this.getZ() + Mth.sin(a) * r);
                DrownedCorsairEntity c = ShoreEntities.DROWNED_CORSAIR.create(level, EntitySpawnReason.MOB_SUMMONED);
                if (c == null) {
                    break;
                }
                c.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, this.random.nextFloat() * 360.0F, 0.0F);
                if (!level.noCollision(c) || level.getBlockState(pos.below()).isAir() && !level.getFluidState(pos).isSource()) {
                    continue;
                }
                c.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.MOB_SUMMONED, null);
                c.setTarget(this.getTarget());
                c.setPersistenceRequired();
                level.addFreshEntity(c);
                level.sendParticles(ParticleTypes.SPLASH, c.getX(), c.getY() + 1.0, c.getZ(), 20, 0.4, 0.6, 0.4, 0.1);
                break;
            }
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
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Rallied", this.rallied);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.rallied = input.getBooleanOr("Rallied", false);
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.85F;
    }
}
