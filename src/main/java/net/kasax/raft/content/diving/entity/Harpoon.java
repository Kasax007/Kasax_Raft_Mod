package net.kasax.raft.content.diving.entity;

import net.kasax.raft.content.diving.DivingContent;
import net.kasax.raft.content.diving.DivingSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * The bolt of the harpoon gun. Unlike an arrow it keeps its speed under water and falls only slowly there; it deals
 * 8 damage when it hits under water and 4 in the air, bounces off its target like a trident and can be picked up again.
 */
public class Harpoon extends AbstractArrow {
    public static final float DAMAGE_WATER = 8.0F;
    public static final float DAMAGE_AIR = 4.0F;

    private boolean dealtDamage;

    public Harpoon(EntityType<? extends Harpoon> type, Level level) {
        super(type, level);
    }

    public Harpoon(Level level, LivingEntity owner, ItemStack pickupItem, @Nullable ItemStack weapon) {
        super(DivingContent.HARPOON, owner, level, pickupItem, weapon);
    }

    /** A harpoon fired by a dispenser. */
    public Harpoon(Level level, double x, double y, double z, ItemStack pickupItem) {
        super(DivingContent.HARPOON, x, y, z, level, pickupItem, null);
        this.pickup = Pickup.ALLOWED;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() && !this.isInGround() && this.isInWater() && this.getDeltaMovement().lengthSqr() > 0.01) {
            Vec3 v = this.getDeltaMovement();
            this.level().addParticle(ParticleTypes.BUBBLE, this.getX() - v.x * 0.5, this.getY() - v.y * 0.5, this.getZ() - v.z * 0.5,
                    0.0, 0.02, 0.0);
        }
    }

    @Override
    protected double getDefaultGravity() {
        // a slow sink under water, a normal arc in the air
        return this.isInWater() ? 0.008 : 0.05;
    }

    @Override
    protected float getWaterInertia() {
        return 0.99F;
    }

    @Override
    protected @Nullable EntityHitResult findHitEntity(Vec3 from, Vec3 to) {
        return this.dealtDamage ? null : super.findHitEntity(from, to);
    }

    @Override
    protected Collection<EntityHitResult> findHitEntities(Vec3 from, Vec3 to) {
        EntityHitResult hit = this.findHitEntity(from, to);
        return hit != null ? List.of(hit) : List.of();
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        Entity target = hitResult.getEntity();
        boolean underWater = this.isInWater() || target.isInWater();
        float damage = underWater ? DAMAGE_WATER : DAMAGE_AIR;
        Entity owner = this.getOwner();
        DamageSource source = this.damageSources().trident(this, owner == null ? this : owner);
        if (this.level() instanceof ServerLevel serverLevel && this.getWeaponItem() != null) {
            damage = EnchantmentHelper.modifyDamage(serverLevel, this.getWeaponItem(), target, source, damage);
        }
        this.dealtDamage = true;
        boolean hurt = target.hurtOrSimulate(source, damage);
        if (hurt && target instanceof LivingEntity living) {
            this.doKnockback(living, source);
            this.doPostHurtEffects(living);
        }
        if (target.projectileReceivesSideEffectsOnHit(hurt)) {
            this.playSound(DivingSounds.HARPOON_HIT, 1.0F, 1.0F);
            this.deflect(ProjectileDeflection.REVERSE, target, this.owner, false, new Vec3(0.02, 0.15, 0.02));
        }
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(DivingContent.HARPOON_ITEM);
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return DivingSounds.HARPOON_HIT_GROUND;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.dealtDamage = input.getBooleanOr("DealtDamage", false);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("DealtDamage", this.dealtDamage);
    }
}
