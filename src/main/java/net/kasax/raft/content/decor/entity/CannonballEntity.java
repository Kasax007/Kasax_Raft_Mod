package net.kasax.raft.content.decor.entity;

import net.kasax.raft.content.decor.DecorBlocks;
import net.kasax.raft.content.decor.DecorEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The ball fired by a ship cannon: a heavy iron projectile that flies about 40 blocks, deals 8 damage to what it
 * hits and bursts into smoke without breaking blocks. Trails smoke while it flies.
 */
public class CannonballEntity extends ThrowableItemProjectile {
    public static final float DAMAGE = 8.0F;

    public CannonballEntity(EntityType<? extends CannonballEntity> type, Level level) {
        super(type, level);
    }

    public CannonballEntity(Level level, double x, double y, double z, @Nullable Entity owner) {
        super(DecorEntities.CANNONBALL, x, y, z, level);
        if (owner != null) setOwner(owner);
    }

    @Override
    protected Item getDefaultItem() {
        return DecorBlocks.CANNONBALL;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.02;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() && tickCount > 1) {
            level().addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.15, getZ(), 0, 0.01, 0);
        }
        if (!level().isClientSide() && tickCount > 200) {
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        super.onHitEntity(hitResult);
        Entity target = hitResult.getEntity();
        if (level() instanceof ServerLevel serverLevel) {
            var source = damageSources().thrown(this, getOwner());
            target.hurt(source, DAMAGE);
            if (target instanceof LivingEntity living) {
                Vec3 push = getDeltaMovement().normalize();
                living.knockback(0.8, -push.x, -push.z);
            }
        }
    }

    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);
        if (level() instanceof ServerLevel serverLevel) {
            Vec3 p = hitResult.getLocation();
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, p.x, p.y + 0.2, p.z, 10, 0.3, 0.2, 0.3, 0.02);
            serverLevel.playSound(null, p.x, p.y, p.z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 0.7F,
                    1.4F + random.nextFloat() * 0.2F);
            discard();
        }
    }
}
