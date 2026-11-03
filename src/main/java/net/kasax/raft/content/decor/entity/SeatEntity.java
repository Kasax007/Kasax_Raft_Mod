package net.kasax.raft.content.decor.entity;

import net.kasax.raft.content.decor.DecorEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The invisible seat shared by all sittable decor blocks (chairs, cushions, deck chair, hammock). It sits at the seat
 * height inside its block, carries one player and disappears as soon as nobody sits on it or the block is gone.
 */
public class SeatEntity extends Entity {
    private BlockPos seatPos = BlockPos.ZERO;
    private Block seatBlock;
    private int age;

    public SeatEntity(EntityType<? extends SeatEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    /**
     * Lets the player sit on the block at {@code pos}, {@code yOffset} blocks above its bottom.
     *
     * @return true when the player sits now (or on the client, when the click should count)
     */
    public static boolean sit(Level level, BlockPos pos, Player player, double yOffset) {
        if (player.isPassenger() || player.isShiftKeyDown() || player.isSpectator()) return false;
        if (level.isClientSide()) return true;
        AABB box = new AABB(pos);
        for (SeatEntity seat : level.getEntitiesOfClass(SeatEntity.class, box)) {
            if (seat.isVehicle()) {
                player.sendOverlayMessage(Component.translatable("message.raft.seat.occupied"));
                return false;
            }
            seat.discard();
        }
        if (player.position().distanceToSqr(Vec3.atCenterOf(pos)) > 16) return false;
        SeatEntity seat = new SeatEntity(DecorEntities.SEAT, level);
        seat.seatPos = pos.immutable();
        seat.seatBlock = level.getBlockState(pos).getBlock();
        seat.setPos(pos.getX() + 0.5, pos.getY() + yOffset, pos.getZ() + 0.5);
        if (!level.addFreshEntity(seat)) return false;
        if (!player.startRiding(seat)) {
            seat.discard();
            return false;
        }
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) return;
        age++;
        if (seatBlock == null) {
            // loaded from disk: adopt the block we sit in
            seatPos = BlockPos.containing(getX(), getY() - 0.01, getZ());
            BlockState state = level().getBlockState(seatPos);
            if (state.isAir()) {
                seatPos = seatPos.below();
                state = level().getBlockState(seatPos);
            }
            seatBlock = state.getBlock();
        }
        boolean blockGone = !level().getBlockState(seatPos).is(seatBlock);
        if (blockGone || (!isVehicle() && age > 2)) {
            ejectPassengers();
            discard();
        }
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (!level().isClientSide()) discard();
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        // step off to the side the player looks at (or any free side), like leaving a boat; else stand on top
        Direction forward = passenger.getDirection();
        int[][] offsets = DismountHelper.offsetsForDirection(forward);
        int[][] order = {offsets[7], offsets[0], offsets[1], offsets[4], offsets[5], offsets[6], offsets[2], offsets[3]};
        for (int[] o : order) {
            for (int dy = 0; dy >= -1; dy--) {
                BlockPos p = seatPos.offset(o[0], dy, o[1]);
                Vec3 v = DismountHelper.findSafeDismountLocation(passenger.getType(), level(), p, true);
                if (v != null && DismountHelper.canDismountTo(level(), passenger,
                        passenger.getDimensions(Pose.STANDING).makeBoundingBox(v))) {
                    return v;
                }
            }
        }
        Vec3 top = new Vec3(getX(), seatPos.getY() + level().getBlockState(seatPos).getCollisionShape(level(), seatPos).max(Direction.Axis.Y), getZ());
        if (DismountHelper.canDismountTo(level(), passenger, passenger.getDimensions(Pose.STANDING).makeBoundingBox(top))) {
            return top;
        }
        return super.getDismountLocationForPassenger(passenger);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE_ENTITY;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }
}
