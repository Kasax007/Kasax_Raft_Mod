package net.kasax.raft.content.boss.entity;

import net.kasax.raft.content.boss.BossEntities;
import net.kasax.raft.content.boss.BossSounds;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A glowing blob of brine spat by the Leviathan: 7 damage (9 in the last phase) and Slowness II for 2 s on hit, bursts into spray on blocks
 * and water. Flies in a shallow arc and leaves a teal trail.
 */
public class BrineBoltEntity extends Projectile {
    /** Damage on hit (3.2: 7, 9 in the last phase; was 5), set by the Leviathan when it spits. */
    private float damage = 5.0F;

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public static final double GRAVITY = 0.025;
    private static final int TEAL = 0x45F0E0;

    public BrineBoltEntity(EntityType<? extends BrineBoltEntity> type, Level level) {
        super(type, level);
    }

    public BrineBoltEntity(Level level, LivingEntity owner, Vec3 pos) {
        this(BossEntities.BRINE_BOLT, level);
        this.setOwner(owner);
        this.setPos(pos.x, pos.y - 0.25, pos.z);
    }

    @Override
    protected double getDefaultGravity() {
        return GRAVITY;
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 movement = this.getDeltaMovement();
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        this.hitTargetOrDeflectSelf(hit);
        if (this.isRemoved()) {
            return;
        }
        double x = this.getX() + movement.x;
        double y = this.getY() + movement.y;
        double z = this.getZ() + movement.z;
        this.updateRotation();
        if (this.level().isClientSide()) {
            for (int i = 0; i < 2; i++) {
                double f = i * 0.5;
                this.level().addParticle(ParticleTypes.GLOW, this.getX() - movement.x * f, this.getY() + 0.25 - movement.y * f,
                        this.getZ() - movement.z * f, 0.0, 0.0, 0.0);
            }
            this.level().addParticle(ParticleTypes.FALLING_WATER, this.getX(), this.getY() + 0.1, this.getZ(), 0.0, 0.0, 0.0);
        }
        if (this.tickCount > 4 && this.isInWater() || this.tickCount > 120) {
            this.burst();
            return;
        }
        this.setDeltaMovement(movement.scale(0.995));
        this.applyGravity();
        this.setPos(x, y, z);
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !(target instanceof LeviathanEntity) && !(target instanceof TidelingEntity)
                && !(target instanceof BrineBoltEntity);
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        super.onHitEntity(hitResult);
        if (this.level() instanceof ServerLevel serverLevel) {
            Entity target = hitResult.getEntity();
            LivingEntity owner = this.getOwner() instanceof LivingEntity living ? living : null;
            DamageSource source = this.damageSources().mobProjectile(this, owner);
            if (target.hurt(source, this.damage)) {
                EnchantmentHelper.doPostAttackEffects(serverLevel, target, source);
                if (target instanceof LivingEntity living) {
                    living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1), owner);
                }
            }
        }
        this.burst();
    }

    @Override
    protected void onHitBlock(BlockHitResult hitResult) {
        super.onHitBlock(hitResult);
        this.burst();
    }

    private void burst() {
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SPLASH, this.getX(), this.getY() + 0.25, this.getZ(), 30, 0.3, 0.3, 0.3, 0.3);
            level.sendParticles(new DustParticleOptions(net.minecraft.world.phys.Vec3.fromRGB24(TEAL).toVector3f(), 1.4F), this.getX(), this.getY() + 0.25, this.getZ(), 12, 0.35, 0.35, 0.35, 0.0);
            level.sendParticles(ParticleTypes.GLOW, this.getX(), this.getY() + 0.25, this.getZ(), 6, 0.2, 0.2, 0.2, 0.05);
            level.playSound(null, this.getX(), this.getY(), this.getZ(), BossSounds.BRINE_BOLT_HIT, SoundSource.HOSTILE, 1.2F,
                    0.9F + this.random.nextFloat() * 0.3F);
            this.discard();
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        this.setDeltaMovement(new net.minecraft.world.phys.Vec3(packet.getXa(), packet.getYa(), packet.getZa()));
    }
}
