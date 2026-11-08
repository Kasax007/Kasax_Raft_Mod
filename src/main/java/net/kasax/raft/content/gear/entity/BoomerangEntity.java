package net.kasax.raft.content.gear.entity;

import net.kasax.raft.content.gear.GearContent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A thrown boomerang: flies straight for about 18 blocks (or until it hits something), then turns and flies back to
 * its thrower through anything in the way, hitting mobs on both legs of the trip (5 damage each, once per mob) and
 * carrying back up to three item stacks it passes through (floating debris!). Caught by the thrower it goes back
 * into the inventory; if the thrower is gone it drops.
 */
public class BoomerangEntity extends ThrowableItemProjectile {
    public static final float DAMAGE = 5.0F;
    private static final int OUTWARD_TICKS = 15;
    private boolean returning;
    private boolean creative;
    private int flight;
    private final Set<UUID> hit = new HashSet<>();
    private final List<ItemStack> carried = new ArrayList<>();

    public BoomerangEntity(EntityType<? extends BoomerangEntity> type, Level level) {
        super(type, level);
    }

    public BoomerangEntity(Level level, LivingEntity owner, ItemStack stack, boolean creative) {
        super(GearContent.BOOMERANG_ENTITY, owner, level, stack);
        this.creative = creative;
    }

    @Override
    protected Item getDefaultItem() {
        return GearContent.BOOMERANG;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return entity != getOwner() && entity.isAlive() && entity.isPickable() && !entity.isSpectator()
                && !hit.contains(entity.getUUID()) && !(entity instanceof ItemEntity);
    }

    @Override
    public void tick() {
        baseTick();
        if (!(level() instanceof ServerLevel server)) {
            setPos(position().add(getDeltaMovement()));
            if (tickCount % 3 == 0) level().addParticle(ParticleTypes.CLOUD, getX(), getY(), getZ(), 0, 0, 0);
            return;
        }
        if (tickCount % 6 == 1) {
            server.playSound(null, getX(), getY(), getZ(), GearContent.BOOMERANG_THROW, SoundSource.PLAYERS, 0.5F, 1.4F);
        }
        Vec3 pos = position();
        if (!returning) {
            HitResult result = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (result.getType() == HitResult.Type.ENTITY) {
                strike(server, ((EntityHitResult) result).getEntity());
                setPos(result.getLocation());
                returning = true;
            } else if (result.getType() == HitResult.Type.BLOCK) {
                setPos(result.getLocation());
                server.playSound(null, getX(), getY(), getZ(), SoundEvents.WOOD_HIT, SoundSource.PLAYERS, 0.8F, 1.2F);
                returning = true;
            } else {
                setPos(pos.add(getDeltaMovement()));
            }
            if (++flight >= OUTWARD_TICKS) returning = true;
        } else {
            Entity owner = getOwner();
            if (owner == null || !owner.isAlive() || owner.level() != level() || owner.distanceToSqr(this) > 64 * 64) {
                dropAll(server);
                return;
            }
            Vec3 to = owner.getEyePosition().subtract(0, 0.3, 0).subtract(pos);
            if (to.length() < 1.6) {
                caught(server, owner);
                return;
            }
            Vec3 vel = to.normalize().scale(Math.min(1.5, 0.5 + flight * 0.04));
            flight++;
            setDeltaMovement(vel);
            EntityHitResult e = ProjectileUtil.getEntityHitResult(level(), this, pos, pos.add(vel),
                    getBoundingBox().expandTowards(vel).inflate(0.5), this::canHitEntity);
            if (e != null) strike(server, e.getEntity());
            setPos(pos.add(vel));
        }
        collect();
        if (tickCount > 400) dropAll(server);
    }

    private void strike(ServerLevel server, Entity target) {
        hit.add(target.getUUID());
        var source = damageSources().thrown(this, getOwner());
        if (target.hurtServer(server, source, DAMAGE) && target instanceof LivingEntity living) {
            Vec3 push = getDeltaMovement().normalize();
            living.knockback(0.4, -push.x, -push.z, source, DAMAGE);
        }
        server.playSound(null, getX(), getY(), getZ(), SoundEvents.WOOD_HIT, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private void collect() {
        if (carried.size() >= 3) return;
        for (ItemEntity item : level().getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(0.6))) {
            if (!item.isAlive() || item.hasPickUpDelay()) continue;
            carried.add(item.getItem().copy());
            item.discard();
            if (carried.size() >= 3) break;
        }
    }

    private void caught(ServerLevel server, Entity owner) {
        if (owner instanceof Player player) {
            if (!creative) player.getInventory().placeItemBackInInventory(getItem().copy(), net.minecraft.util.Prediction.SERVER_ONLY);
            for (ItemStack s : carried) player.getInventory().placeItemBackInInventory(s, net.minecraft.util.Prediction.SERVER_ONLY);
            server.playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.4F, 1.2F);
        } else {
            dropAll(server);
            return;
        }
        carried.clear();
        discard();
    }

    private void dropAll(ServerLevel server) {
        if (!creative) spawnAtLocation(server, getItem().copy());
        for (ItemStack s : carried) spawnAtLocation(server, s);
        carried.clear();
        discard();
    }

    public boolean isReturning() {
        return returning;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("returning", returning);
        output.putBoolean("creative", creative);
        output.putInt("flight", flight);
        ValueOutput.TypedOutputList<ItemStack> list = output.list("carried", ItemStack.CODEC);
        for (ItemStack s : carried) list.add(s);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        returning = input.getBooleanOr("returning", true);
        creative = input.getBooleanOr("creative", false);
        flight = input.getIntOr("flight", 0);
        carried.clear();
        input.listOrEmpty("carried", ItemStack.CODEC).forEach(carried::add);
    }
}
