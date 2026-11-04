package net.kasax.raft.content.vessel;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.kasax.raft.content.vessel.block.RaftMotorBlockEntity;
import net.kasax.raft.content.vessel.net.VesselDataPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PositionPath;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.core.PositionAndRotation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/**
 * A raft built by players, sailing as one piece (see docs/design/raft-building.md). The blocks live in a
 * {@link VesselStructure}; the entity's position is the structure's pivot and its yaw the bow direction.
 *
 * <p>The server simulates the raft (helmsman input, speed, turning, land and water checks) and carries mobs and items
 * on board; clients get the blocks once ({@link VesselDataPayload}), interpolate the pose (the yaw comes as an exact
 * float, not as a packed rotation) and carry their own player. When nobody is at the helm the raft brakes, glides onto
 * the block grid and turns back into blocks ({@link VesselAssembler#dock}).
 */
public class VesselEntity extends Entity {
    public static final byte SAILING = 0;
    public static final byte STOPPING = 1;
    public static final byte ALIGNING = 2;
    public static final byte DOCKED = 3;

    private static final EntityDataAccessor<Float> DATA_YAW = SynchedEntityData.defineId(VesselEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Byte> DATA_STATE = SynchedEntityData.defineId(VesselEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> DATA_SPEED = SynchedEntityData.defineId(VesselEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_MOTOR = SynchedEntityData.defineId(VesselEntity.class, EntityDataSerializers.BOOLEAN);

    /** Rowing power of the helmsman alone; oars add 1, sails 3, running motors 6. */
    private static final float BASE_POWER = 0.5F;
    private static final float OAR_POWER = 1.0F;
    private static final float SAIL_POWER = 3.0F;
    private static final float MOTOR_POWER = 6.0F;
    private static final float TOP_SPEED = 0.45F;
    /** How far (blocks) above the deck something still sails along: jumping players, birds, falling items. */
    private static final double CARRY_HEIGHT = 3.0;
    /** Server ticks between the server's raft and what clients see (interpolation). */
    private static final int CLIENT_LAG_TICKS = 2;

    /** Set by the client module: sends the block the player aims at to the server (or passes, see VesselClient). */
    public static BiFunction<VesselEntity, InteractionHand, InteractionResult> clientUse = (vessel, hand) -> InteractionResult.PASS;

    private @Nullable VesselStructure structure;
    private final List<Stowed> stowed = new ArrayList<>();
    private float speed;
    private float yawRate;
    private @Nullable VesselPose dockTarget;
    private int dockWait;
    private int dockedTicks;
    private boolean warnedNoRoom;
    private int bumpCooldown;
    private int passengerTurnTick = -1;
    /** Autopilot (NBT {@code Autopilot:{Forward:1b,Turn:-1b,Ticks:200}}, for map makers and tests): sails as if a
     * helmsman held these keys for that many ticks, even with nobody at the helm. */
    private int autoForward;
    private int autoTurn;
    private int autoTicks;
    private int helmToggleTick = -100;
    private boolean fuelWarned;
    private final VesselPose[] history = new VesselPose[CLIENT_LAG_TICKS + 2];
    private int historyIndex;
    private boolean clientReady;
    /** The client's draw cache (a VesselClientModel); only touched by the renderer. */
    public @Nullable Object clientModel;

    /**
     * An item frame or painting that hung on the raft, stowed while sailing: its position on the raft, its saved data
     * and the bow direction when it was stowed (its own direction in the data is relative to that).
     */
    public record Stowed(Vec3 local, CompoundTag tag, Direction bow) {
    }

    public VesselEntity(EntityType<? extends VesselEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_YAW, 0.0F);
        builder.define(DATA_STATE, SAILING);
        builder.define(DATA_SPEED, 0.0F);
        builder.define(DATA_MOTOR, false);
    }

    @Override
    protected InterpolationHandler createInterpolationHandler() {
        return this.level().isClientSide() ? new Interpolation(this) : InterpolationHandler.NO_OP;
    }

    /** Server: a freshly built raft at its first pose. */
    void setup(VesselStructure structure, List<Stowed> stowed, Vec3 pos, float yaw) {
        this.structure = structure;
        this.stowed.clear();
        this.stowed.addAll(stowed);
        this.applyPose(new VesselPose(pos, yaw, structure.pivot()));
        this.setOldPosAndRot();
    }

    // ------------------------------------------------------------------------------------------- access
    public @Nullable VesselStructure structure() {
        return this.structure;
    }

    public List<Stowed> stowed() {
        return this.stowed;
    }

    public byte getState() {
        return this.entityData.get(DATA_STATE);
    }

    private void setState(byte state) {
        this.entityData.set(DATA_STATE, state);
    }

    public boolean isDocked() {
        return this.getState() == DOCKED;
    }

    public float getSpeed() {
        return this.entityData.get(DATA_SPEED);
    }

    public boolean isMotorRunning() {
        return this.entityData.get(DATA_MOTOR);
    }

    /** The pose now (structure must be known). */
    public VesselPose currentPose() {
        return new VesselPose(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.structure.pivot());
    }

    /** The pose drawn at a partial tick (client). */
    public VesselPose renderPose(float partialTicks) {
        return new VesselPose(Mth.lerp(partialTicks, this.xOld, this.getX()), Mth.lerp(partialTicks, this.yOld, this.getY()),
                Mth.lerp(partialTicks, this.zOld, this.getZ()), this.getYRot(partialTicks), this.structure.pivot());
    }

    private void applyPose(VesselPose pose) {
        float yaw = Mth.wrapDegrees(pose.yaw);
        this.setYRot(yaw);
        this.setPos(pose.x, pose.y, pose.z);
        this.setBoundingBox(this.makeBoundingBox(this.position()));
        if (!this.level().isClientSide()) {
            this.entityData.set(DATA_YAW, yaw);
        }
    }

    @Override
    protected AABB makeBoundingBox(Vec3 position) {
        if (this.structure == null) {
            return super.makeBoundingBox(position);
        }
        return this.structure.worldBounds(new VesselPose(position, this.getYRot(), this.structure.pivot()));
    }

    // ------------------------------------------------------------------------------------------- tick
    @Override
    public void tick() {
        this.firstTick = false;
        if (this.level().isClientSide()) {
            this.clientTick();
        } else {
            this.serverTick();
        }
    }

    private void serverTick() {
        if (this.structure == null) {
            this.discard();
            return;
        }
        VesselColliders.add(this);
        if (this.bumpCooldown > 0) {
            this.bumpCooldown--;
        }
        VesselPose from = this.currentPose();
        switch (this.getState()) {
            case SAILING -> this.sail();
            case STOPPING -> this.stop();
            case ALIGNING -> this.align();
            default -> {
                if (++this.dockedTicks > 3) {
                    this.discard();
                }
                return;
            }
        }
        if (this.isRemoved()) {
            return;
        }
        VesselPose to = this.currentPose();
        this.carry(from, to);
        this.history[this.historyIndex++ % this.history.length] = to;
        this.entityData.set(DATA_SPEED, this.speed);
    }

    /** The pose the clients saw a few ticks ago (their raft is behind the server's). */
    private VesselPose clientPose() {
        VesselPose pose = this.history[Math.floorMod(this.historyIndex - 1 - CLIENT_LAG_TICKS, this.history.length)];
        return pose != null ? pose : this.currentPose();
    }

    // ------------------------------------------------------------------------------------------- sailing
    private int weight() {
        return Math.max(1, this.structure.blockCount());
    }

    private void sail() {
        int forward;
        int turn;
        if (this.getControllingPassenger() instanceof ServerPlayer pilot) {
            Input input = pilot.getLastClientInput();
            forward = (input.forward() ? 1 : 0) - (input.backward() ? 1 : 0);
            turn = (input.right() ? 1 : 0) - (input.left() ? 1 : 0);
        } else if (this.autoTicks > 0) {
            this.autoTicks--;
            forward = this.autoForward;
            turn = this.autoTurn;
        } else {
            this.setState(STOPPING);
            this.dockWait = 0;
            this.stop();
            return;
        }
        boolean motor = forward != 0 && this.burnFuel();
        if (motor != this.isMotorRunning()) {
            this.entityData.set(DATA_MOTOR, motor);
        }
        if (forward != 0 && !motor && this.structure.motors().length > 0 && !this.fuelWarned
                && this.getControllingPassenger() instanceof Player pilot) {
            this.fuelWarned = true;
            pilot.sendOverlayMessage(Component.translatable("message.raft.vessel.motor_empty"));
        }
        float power = BASE_POWER + this.structure.oars() * OAR_POWER + this.structure.sails() * SAIL_POWER
                + (motor ? this.fueledMotors() * MOTOR_POWER : 0.0F);
        float weight = this.weight();
        float top = TOP_SPEED * power / (power + weight / 8.0F);
        float target = forward > 0 ? top : forward < 0 ? -top * 0.4F : 0.0F;
        float accel = top / (12.0F + weight / 16.0F) + 0.0015F;
        if (forward == 0) {
            this.speed *= 0.97F;
        }
        this.speed = Mth.approach(this.speed, target, accel);
        float maxTurn = Mth.clamp(3.5F * Mth.sqrt(24.0F / Math.max(24.0F, weight)), 0.6F, 3.5F);
        maxTurn *= 0.4F + 0.6F * Math.min(1.0F, Math.abs(this.speed) / Math.max(0.05F, top));
        this.yawRate = Mth.approach(this.yawRate, turn * maxTurn, maxTurn / 6.0F);
        if (turn == 0) {
            this.yawRate *= 0.8F;
        }
        this.moveAndCollide();
        if (this.tickCount % 20 == 0 && Math.abs(this.speed) > 0.02F) {
            this.playSound(SoundEvents.BOAT_PADDLE_WATER, 0.5F, 0.8F + this.random.nextFloat() * 0.3F);
        }
    }

    /** Running motors use one tick of fuel each; true while at least one motor has fuel. */
    private boolean burnFuel() {
        boolean any = false;
        for (long key : this.structure.motors()) {
            CompoundTag tag = this.structure.blockEntities().get(key);
            if (tag == null) {
                continue;
            }
            int fuel = tag.getIntOr(RaftMotorBlockEntity.FUEL_KEY, 0);
            if (fuel > 0) {
                tag.putInt(RaftMotorBlockEntity.FUEL_KEY, fuel - 1);
                any = true;
            }
        }
        return any;
    }

    private int fueledMotors() {
        int n = 0;
        for (long key : this.structure.motors()) {
            CompoundTag tag = this.structure.blockEntities().get(key);
            if (tag != null && tag.getIntOr(RaftMotorBlockEntity.FUEL_KEY, 0) > 0) {
                n++;
            }
        }
        return Math.max(n, 1);
    }

    /** Moves by speed and turns by yaw rate; on land it slides along the shore or stops. */
    private void moveAndCollide() {
        if (Math.abs(this.speed) < 1.0E-4F && Math.abs(this.yawRate) < 1.0E-4F) {
            this.speed = 0.0F;
            this.yawRate = 0.0F;
            return;
        }
        VesselPose cur = this.currentPose();
        float newYaw = cur.yaw + this.yawRate;
        double rad = newYaw * Mth.DEG_TO_RAD;
        double mx = -Math.sin(rad) * this.speed;
        double mz = Math.cos(rad) * this.speed;
        float before = Math.abs(this.speed);
        if (this.tryPose(cur.moved(mx, 0.0, mz).withYaw(newYaw))) {
            return;
        }
        if (this.yawRate != 0.0F && this.tryPose(cur.moved(mx, 0.0, mz))) {
            this.yawRate = 0.0F;
        } else if (this.yawRate != 0.0F && this.tryPose(cur.withYaw(newYaw))) {
            this.speed *= 0.3F;
        } else if (Math.abs(mx) > 1.0E-3 && Math.abs(mx) >= Math.abs(mz) && this.tryPose(cur.moved(mx, 0.0, 0.0))) {
            this.speed *= 0.85F;
            this.yawRate = 0.0F;
        } else if (Math.abs(mz) > 1.0E-3 && this.tryPose(cur.moved(0.0, 0.0, mz))) {
            this.speed *= 0.85F;
            this.yawRate = 0.0F;
        } else if (Math.abs(mx) > 1.0E-3 && this.tryPose(cur.moved(mx, 0.0, 0.0))) {
            this.speed *= 0.85F;
            this.yawRate = 0.0F;
        } else {
            this.speed = 0.0F;
            this.yawRate = 0.0F;
        }
        if (before > 0.08F && this.bumpCooldown == 0) {
            this.bumpCooldown = 15;
            this.playSound(SoundEvents.WOOD_HIT, 1.0F, 0.6F);
            this.playSound(SoundEvents.GENERIC_SPLASH, 0.4F, 1.3F);
        }
    }

    private boolean tryPose(VesselPose pose) {
        List<BlockPos> lilyPads = new ArrayList<>(0);
        if (!VesselAssembler.fits(this.level(), this.structure, pose, lilyPads)) {
            return false;
        }
        this.applyPose(pose);
        for (BlockPos pad : lilyPads) {
            this.level().destroyBlock(pad, true, this);
        }
        return true;
    }

    // ------------------------------------------------------------------------------------------- docking
    private void stop() {
        if (this.autoTicks > 0 || this.getControllingPassenger() != null) {
            this.setState(SAILING);
            return;
        }
        this.speed = Mth.approach(this.speed * 0.85F, 0.0F, 0.01F);
        this.yawRate = Mth.approach(this.yawRate * 0.7F, 0.0F, 0.2F);
        if (this.isMotorRunning()) {
            this.entityData.set(DATA_MOTOR, false);
        }
        if (Math.abs(this.speed) > 0.004F || Math.abs(this.yawRate) > 0.05F) {
            this.moveAndCollide();
            return;
        }
        this.speed = 0.0F;
        this.yawRate = 0.0F;
        if (this.dockWait > 0) {
            this.dockWait--;
            return;
        }
        VesselPose target = VesselAssembler.findDockPose(this);
        if (target == null) {
            this.dockWait = 60;
            if (!this.warnedNoRoom) {
                this.warnedNoRoom = true;
                this.tellAboard(Component.translatable("message.raft.vessel.no_room"));
            }
            return;
        }
        this.dockTarget = target;
        this.setState(ALIGNING);
    }

    private void align() {
        VesselPose target = this.dockTarget;
        if (target == null) {
            this.setState(STOPPING);
            return;
        }
        VesselPose cur = this.currentPose();
        float dyaw = Mth.wrapDegrees(target.yaw - cur.yaw);
        double dx = target.x - cur.x;
        double dy = target.y - cur.y;
        double dz = target.z - cur.z;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (Math.abs(dyaw) <= 2.0F && dist <= 0.06) {
            this.applyPose(target);
            if (VesselAssembler.dock(this, target)) {
                this.setState(DOCKED);
                this.dockedTicks = 0;
                this.ejectPassengers();
                this.tellAboard(Component.translatable("message.raft.vessel.anchored"));
            } else {
                this.dockTarget = null;
                this.dockWait = 40;
                this.setState(STOPPING);
            }
            return;
        }
        float stepYaw = Mth.clamp(dyaw, -2.0F, 2.0F);
        double step = dist > 0.06 ? 0.06 / dist : 1.0;
        this.applyPose(cur.moved(dx * step, dy * step, dz * step).withYaw(cur.yaw + stepYaw));
    }

    private void tellAboard(Component message) {
        AABB area = this.getBoundingBox().inflate(4.0, 6.0, 4.0);
        for (Player player : this.level().players()) {
            if (area.contains(player.position())) {
                player.sendOverlayMessage(message);
            }
        }
    }

    // ------------------------------------------------------------------------------------------- carrying
    /**
     * Moves everything that rests on the raft or is in the air above its deck along with it (translation and turning).
     * Each side carries what it is authoritative for: the server its mobs and items, a client its own player.
     */
    private void carry(VesselPose from, VesselPose to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        float dyaw = Mth.wrapDegrees(to.yaw - from.yaw);
        if (dx * dx + dy * dy + dz * dz > 64.0) {
            return;
        }
        boolean moving = dx != 0.0 || dy != 0.0 || dz != 0.0 || dyaw != 0.0F;
        AABB zone = this.structure.worldBounds(from).expandTowards(0.0, CARRY_HEIGHT + 2.0, 0.0).inflate(0.5);
        List<Entity> riders = this.level().getEntities(this, zone, e -> !(e instanceof VesselEntity) && !e.isPassenger()
                && !e.isSpectator() && !e.noPhysics && !(e instanceof HangingEntity) && !(e instanceof Projectile)
                && e.isLocalInstanceAuthoritative()
                && e.getPistonPushReaction() != PushReaction.IGNORE_ENTITY);
        float rad = -dyaw * Mth.DEG_TO_RAD;
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        for (Entity e : riders) {
            AABB box = e.getBoundingBox();
            if (!this.structure.isOver(from, box, CARRY_HEIGHT)) {
                continue;
            }
            if (moving) {
                double rx = e.getX() - from.x;
                double rz = e.getZ() - from.z;
                double tx = to.x + rx * cos + rz * sin;
                double ty = e.getY() + dy;
                double tz = to.z + rz * cos - rx * sin;
                double mx = tx - e.getX();
                double mz = tz - e.getZ();
                if (!this.level().noBlockCollision(e, box.move(mx, dy, mz))) {
                    continue;
                }
                e.setPos(tx, ty, tz);
                if (dyaw != 0.0F) {
                    e.setYRot(e.getYRot() + dyaw);
                    e.setYHeadRot(e.getYHeadRot() + dyaw);
                    if (e instanceof LivingEntity living) {
                        living.yBodyRot += dyaw;
                    }
                }
                if (this.level().isClientSide()) {
                    VesselCarry.carried(e, mx, dy, mz, dyaw);
                }
            }
            // never let anything sink into the deck (e.g. in the tick the raft was built under it)
            double top = this.structure.sunkTop(to, e.getBoundingBox(), 0.4);
            if (!Double.isNaN(top) && this.level().noBlockCollision(e, e.getBoundingBox().move(0.0, top - e.getY(), 0.0))) {
                double lift = top - e.getY();
                e.setPos(e.getX(), top, e.getZ());
                if (e.getDeltaMovement().y < 0.0) {
                    e.setDeltaMovement(e.getDeltaMovement().multiply(1.0, 0.0, 1.0));
                }
                e.setOnGround(true);
                if (this.level().isClientSide()) {
                    VesselCarry.carried(e, 0.0, lift, 0.0, 0.0F);
                }
            }
            if (e.onGround() || e.getDeltaMovement().y >= 0.0) {
                e.resetFallDistance();
            }
        }
    }

    // ------------------------------------------------------------------------------------------- client
    private void clientTick() {
        if (this.structure == null) {
            VesselStructure pending = VesselDataPayload.takePending(this.getId());
            if (pending != null) {
                this.setClientStructure(pending);
            }
            this.clientReady = true;
            return;
        }
        this.clientReady = true;
        VesselColliders.add(this);
        VesselPose from = new VesselPose(this.xo, this.yo, this.zo, this.yRotO, this.structure.pivot());
        VesselPose to = this.currentPose();
        if (!this.isDocked()) {
            this.carry(from, to);
        }
        this.clientEffects();
    }

    /** Client: the blocks arrived from the server. */
    public void setClientStructure(VesselStructure structure) {
        this.structure = structure;
        this.clientModel = null;
        this.setBoundingBox(this.makeBoundingBox(this.position()));
    }

    private void clientEffects() {
        float speed = this.getSpeed();
        if (Math.abs(speed) > 0.06F && this.random.nextFloat() < Math.abs(speed) * 2.5F) {
            // spray along the bow (or the stern when going backwards)
            VesselPose pose = this.currentPose();
            double lx = this.random.nextDouble() * this.structure.sizeX();
            double lz = speed > 0 ? this.structure.sizeZ() + 0.2 : -0.2;
            Vec3 p = pose.toWorld(lx, 0.9, lz);
            this.level().addParticle(ParticleTypes.SPLASH, p.x, p.y, p.z, 0.0, 0.1, 0.0);
        }
        if (this.isMotorRunning() && this.tickCount % 3 == 0) {
            VesselPose pose = this.currentPose();
            for (long key : this.structure.motors()) {
                Vec3 p = pose.toWorld(BlockPos.getX(key) + 0.5, BlockPos.getY(key) + 1.0, BlockPos.getZ(key) + 0.5);
                this.level().addParticle(ParticleTypes.SMOKE, p.x, p.y, p.z, 0.0, 0.05, 0.0);
            }
            if (this.tickCount % 6 == 0) {
                this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.NOTE_BLOCK_BASEDRUM.value(),
                        SoundSource.NEUTRAL, 0.3F, 0.5F + this.random.nextFloat() * 0.08F, false);
            }
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_YAW.equals(key) && this.level().isClientSide()) {
            float yaw = this.entityData.get(DATA_YAW);
            if (!this.clientReady) {
                this.setYRot(yaw);
                this.yRotO = yaw;
                this.setBoundingBox(this.makeBoundingBox(this.position()));
            } else if (this.interpolationHandler instanceof Interpolation interpolation) {
                interpolation.yawTo(yaw);
            }
        }
    }

    /**
     * Client interpolation of a server-driven raft: the position from the movement packets, the yaw from the exact
     * synced float (packed rotations would make the raft turn in 1.4 degree steps).
     */
    static final class Interpolation implements InterpolationHandler {
        private static final int STEPS = 3;
        private final VesselEntity vessel;
        private @Nullable Vec3 targetPos;
        private float targetYaw;
        private boolean hasYaw;
        private int steps;

        Interpolation(VesselEntity vessel) {
            this.vessel = vessel;
        }

        @Override
        public @Nullable PositionAndRotation target() {
            return this.steps > 0 && this.targetPos != null ? PositionAndRotation.of(this.targetPos, this.targetYaw, 0.0F) : null;
        }

        @Override
        public boolean interpolateTo(@Nullable PositionPath position, float yRot, float xRot, boolean hasRotation) {
            if (position == null) {
                return true;
            }
            Vec3 end = position.endPosition();
            if (!this.hasYaw) {
                this.targetYaw = this.vessel.getYRot();
            }
            if (end.distanceToSqr(this.vessel.position()) > 64.0) {
                this.vessel.setPos(end);
                this.vessel.setOldPosAndRot();
            }
            this.targetPos = end;
            this.steps = STEPS;
            return true;
        }

        void yawTo(float yaw) {
            this.targetYaw = yaw;
            this.hasYaw = true;
            if (this.targetPos == null || this.steps <= 0) {
                this.targetPos = this.vessel.position();
            }
            this.steps = STEPS;
        }

        @Override
        public void interpolate() {
            if (this.steps <= 0 || this.targetPos == null) {
                return;
            }
            double alpha = 1.0 / this.steps;
            float yaw = (float) Mth.rotLerp(alpha, this.vessel.getYRot(), this.targetYaw);
            this.vessel.setYRot(Mth.wrapDegrees(yaw));
            this.vessel.setPos(this.vessel.position().lerp(this.targetPos, alpha));
            this.vessel.setBoundingBox(this.vessel.makeBoundingBox(this.vessel.position()));
            this.steps--;
        }

        @Override
        public void applyPredictedMovement(Vec3 delta) {
        }

        @Override
        public boolean hasActiveInterpolation() {
            return this.steps > 0;
        }

        @Override
        public void cancel() {
            this.steps = 0;
        }
    }

    // ------------------------------------------------------------------------------------------- tracking
    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (this.structure != null) {
            ServerPlayNetworking.send(player, new VesselDataPayload(this.getId(), this.structure));
        }
    }

    @Override
    public void onRemoval(RemovalReason reason) {
        super.onRemoval(reason);
        VesselColliders.remove(this);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide() && reason == RemovalReason.KILLED && this.structure != null && !this.isDocked()) {
            // /kill: never lose a raft – drop anchor right here, or drop everything as items
            this.setState(DOCKED);
            VesselAssembler.emergencyDock(this);
        }
        super.remove(reason);
    }

    // ------------------------------------------------------------------------------------------- passengers
    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().isEmpty() && passenger instanceof Player && !this.isDocked();
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return this.getFirstPassenger() instanceof Player player ? player : null;
    }

    /** The server sails the raft, not the helmsman's client. */
    @Override
    public boolean isClientAuthoritative() {
        return false;
    }

    @Override
    protected boolean isLocalClientAuthoritative() {
        return false;
    }

    /** A raft stays in the world when its helmsman logs out (it is not stored with the player). */
    @Override
    public boolean hasExactlyOnePlayerPassenger() {
        return false;
    }

    private Vec3 seatLocal() {
        BlockPos helm = this.structure.helm();
        // the stool of the helm (facing south in the raft's frame): top 7.5 px high, centred 3.25 px from the stern side;
        // a sitting player's hips are 0.15 above the attachment point
        return new Vec3(helm.getX() + 0.5, helm.getY() + 0.32, helm.getZ() + 0.2);
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction moveFunction) {
        if (this.structure == null) {
            super.positionRider(passenger, moveFunction);
            return;
        }
        Vec3 seat = this.currentPose().toWorld(this.seatLocal());
        Vec3 offset = passenger.getVehicleAttachmentPoint(this);
        moveFunction.accept(passenger, seat.x - offset.x, seat.y - offset.y, seat.z - offset.z);
        // the helmsman turns with the raft (once per tick, on the side that owns their rotation)
        if (this.passengerTurnTick != this.tickCount && passenger.isLocalInstanceAuthoritative()) {
            this.passengerTurnTick = this.tickCount;
            float dyaw = Mth.wrapDegrees(this.getYRot() - this.yRotO);
            if (dyaw != 0.0F) {
                passenger.setYRot(passenger.getYRot() + dyaw);
                passenger.setYHeadRot(passenger.getYHeadRot() + dyaw);
            }
        }
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        if (this.structure == null) {
            return super.getDismountLocationForPassenger(passenger);
        }
        // stand up on the stool, where the helmsman's own client sees the helm
        Vec3 seat = this.seatLocal();
        VesselPose pose = passenger instanceof Player ? this.clientPose() : this.currentPose();
        return pose.toWorld(seat.x, this.structure.helm().getY() + 7.5 / 16.0 + 0.01, seat.z);
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (!this.level().isClientSide() && this.getState() == SAILING && this.getPassengers().isEmpty()) {
            this.setState(STOPPING);
            this.dockWait = 0;
            this.warnedNoRoom = false;
            if (passenger instanceof Player player) {
                player.sendOverlayMessage(Component.translatable("message.raft.vessel.anchoring"));
            }
        }
    }

    // ------------------------------------------------------------------------------------------- interaction
    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (this.level().isClientSide()) {
            return clientUse.apply(this, hand);
        }
        return InteractionResult.PASS;
    }

    /** Server: a player used a block of the raft (sent by their client with the exact block). */
    public void useBlock(ServerPlayer player, BlockPos local, InteractionHand hand) {
        if (this.structure == null || this.isDocked()) {
            return;
        }
        BlockState state = this.structure.get(local);
        if (state == null) {
            return;
        }
        Vec3 center = this.clientPose().toWorld(local.getX() + 0.5, local.getY() + 0.5, local.getZ() + 0.5);
        double reach = player.blockInteractionRange() + 3.0;
        if (player.getEyePosition().distanceToSqr(center) > reach * reach) {
            return;
        }
        if (local.equals(this.structure.helm())) {
            if (this.tickCount - this.helmToggleTick < 10) {
                return;
            }
            this.helmToggleTick = this.tickCount;
            if (this.hasPassenger(player)) {
                player.stopRiding();
            } else if (!this.getPassengers().isEmpty()) {
                player.sendOverlayMessage(Component.translatable("message.raft.vessel.helm_taken"));
            } else {
                this.takeHelm(player);
            }
            return;
        }
        ItemStack stack = player.getItemInHand(hand);
        CompoundTag motor = this.structure.blockEntities().get(local.asLong());
        if (state.is(VesselContent.RAFT_MOTOR) && motor != null && player.level() instanceof ServerLevel level) {
            int fuel = motor.getIntOr(RaftMotorBlockEntity.FUEL_KEY, 0);
            if (VesselFuel.isFuel(stack)) {
                motor.putInt(RaftMotorBlockEntity.FUEL_KEY, VesselFuel.refuel(level, BlockPos.containing(center), player, stack, fuel));
            } else {
                VesselFuel.showFuel(player, fuel);
            }
            return;
        }
        player.sendOverlayMessage(Component.translatable("message.raft.vessel.anchor_first"));
    }

    /** Seats the player at the helm and sails on (also stops a raft that is about to drop anchor). */
    public void takeHelm(ServerPlayer player) {
        if (this.isDocked()) {
            return;
        }
        if (player.isPassenger()) {
            player.stopRiding();
        }
        if (player.startRiding(this, true, true)) {
            this.setState(SAILING);
            this.fuelWarned = false;
            this.dockTarget = null;
            this.warnedNoRoom = false;
            player.sendOverlayMessage(Component.translatable("message.raft.vessel.controls"));
        }
    }

    // ------------------------------------------------------------------------------------------- physics off
    @Override
    public void move(MoverType type, Vec3 delta) {
    }

    @Override
    public void push(double x, double y, double z) {
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean skipAttackInteraction(Entity source) {
        return true;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    public boolean ignoreExplosion(Explosion explosion) {
        return true;
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE_ENTITY;
    }

    @Override
    public boolean canUsePortal(boolean ignorePassenger) {
        return false;
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    @Override
    public boolean shouldBeSaved() {
        return !this.isDocked() && this.structure != null && super.shouldBeSaved();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double d = 96.0 + (this.structure == null ? 0.0 : this.structure.radius() * 2.0);
        return distance < d * d;
    }

    // ------------------------------------------------------------------------------------------- save
    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        if (this.structure != null) {
            this.structure.save(output.child("Raft"));
        }
        output.putFloat("Speed", this.speed);
        output.putFloat("YawRate", this.yawRate);
        if (this.autoTicks > 0) {
            ValueOutput auto = output.child("Autopilot");
            auto.putByte("Forward", (byte) this.autoForward);
            auto.putByte("Turn", (byte) this.autoTurn);
            auto.putInt("Ticks", this.autoTicks);
        }
        ValueOutput.ValueOutputList list = output.childrenList("Stowed");
        for (Stowed s : this.stowed) {
            ValueOutput child = list.addChild();
            child.putDouble("X", s.local().x);
            child.putDouble("Y", s.local().y);
            child.putDouble("Z", s.local().z);
            child.store("Entity", CompoundTag.CODEC, s.tag());
            child.putInt("Bow", s.bow().get2DDataValue());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.structure = input.child("Raft").map(VesselStructure::load).orElse(null);
        this.speed = input.getFloatOr("Speed", 0.0F);
        this.yawRate = input.getFloatOr("YawRate", 0.0F);
        ValueInput auto = input.childOrEmpty("Autopilot");
        this.autoForward = Mth.clamp(auto.getByteOr("Forward", (byte) 0), -1, 1);
        this.autoTurn = Mth.clamp(auto.getByteOr("Turn", (byte) 0), -1, 1);
        this.autoTicks = Math.max(0, auto.getIntOr("Ticks", 0));
        this.stowed.clear();
        input.childrenListOrEmpty("Stowed").forEach(child -> child.read("Entity", CompoundTag.CODEC).ifPresent(tag ->
                this.stowed.add(new Stowed(new Vec3(child.getDoubleOr("X", 0.0), child.getDoubleOr("Y", 0.0), child.getDoubleOr("Z", 0.0)), tag,
                        Direction.from2DDataValue(child.getIntOr("Bow", 0))))));
        // a raft loaded from disk has nobody at the helm: it drops anchor
        this.setState(STOPPING);
        this.dockWait = 0;
        this.entityData.set(DATA_YAW, Mth.wrapDegrees(this.getYRot()));
        if (this.structure != null) {
            this.setBoundingBox(this.makeBoundingBox(this.position()));
        }
    }

    /** Server: is water at or right under this world cell (for the float check)? */
    static boolean wet(Level level, BlockPos pos) {
        return level.getFluidState(pos).is(FluidTags.WATER) || level.getFluidState(pos.below()).is(FluidTags.WATER)
                || level.getBlockState(pos).is(Blocks.BUBBLE_COLUMN);
    }
}
