package net.kasax.raft.content.voyage;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.kasax.raft.content.voyage.mixin.AbstractBoatAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The Expedition Raft (spec 8.1): a 3x3 raft on vanilla boat physics with four seats, built-in storage, a fuel slot for
 * the engine and five upgrades that show on the model. It never sinks, never throws riders off, ignores damage from
 * anything but players (three hits within two seconds, or one sneaking hit) and carries everything standing on its deck.
 */
public class ExpeditionRaft extends AbstractBoat implements ContainerEntity, HasCustomInventoryScreen, ExtendedMenuProvider<Integer> {
    public static final int ROW = 27;
    public static final int CARGO_SLOTS = 54;
    public static final int FUEL_SLOT = 54;
    public static final int SIZE = 55;

    /** Speed relative to a vanilla boat; the sail adds +50 %, the running engine +120 % of the raft's own speed. */
    private static final float BASE_SPEED = 0.8F;
    private static final float SAIL_BONUS = 0.5F;
    private static final float ENGINE_BONUS = 1.2F;
    /** Extra damping of the turn rate: a big raft turns slower than a boat. */
    private static final float TURN_DAMPING = 0.93F;
    private static final int HIT_WINDOW = 40;
    /** How far (0..1) a dismounting rider is moved from the seat towards the middle of the deck. */
    private static final double DISMOUNT_TO_CENTRE = 0.6;
    /** Upper limit of the steering player's lead (ticks) used for their dismount spot. */
    private static final double MAX_LEAD_TICKS = 5.0;

    /** Seats (x = port, y = up, z = forward, blocks): helm on the stool at the stern, two midships, one at the bow. */
    private static final Vec3[] SEATS = {
            new Vec3(0.0, 0.78, -1.0),
            new Vec3(0.82, 0.6, -0.12),
            new Vec3(-0.82, 0.6, -0.12),
            new Vec3(0.0, 0.6, 0.88)
    };

    private static final EntityDataAccessor<Integer> DATA_UPGRADES = SynchedEntityData.defineId(ExpeditionRaft.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_ANCHORED = SynchedEntityData.defineId(ExpeditionRaft.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_FUELED = SynchedEntityData.defineId(ExpeditionRaft.class, EntityDataSerializers.BOOLEAN);

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private @Nullable ResourceKey<LootTable> lootTable;
    private long lootTableSeed;
    private int burnTime;
    private int burnTotal;
    private int hits;
    private long lastHitTime = Long.MIN_VALUE / 2;
    private boolean inputUp;

    /** Server: the player who steered the raft in the last tick and how far it moved then (the server does not
     * simulate a raft a player steers, so its delta movement stays zero). */
    private @Nullable UUID steererId;
    private Vec3 steeredMotion = Vec3.ZERO;
    private @Nullable Vec3 steeredFrom;

    private boolean carryReady;
    private double carryX;
    private double carryY;
    private double carryZ;
    private float carryYaw;

    // client animation state
    private float anchorDrop;
    private float anchorDropO;
    private float sailFill;
    private float sailFillO;
    private float propeller;
    private float propellerO;
    private float propellerSpeed;
    private float turn;
    private float turnO;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> ExpeditionRaft.this.burnTime;
                case 1 -> ExpeditionRaft.this.burnTotal;
                case 2 -> ExpeditionRaft.this.isAnchored() ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public ExpeditionRaft(EntityType<? extends ExpeditionRaft> type, Level level) {
        super(type, level, () -> VoyageContent.EXPEDITION_RAFT_ITEM);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_UPGRADES, 0);
        builder.define(DATA_ANCHORED, false);
        builder.define(DATA_FUELED, false);
    }

    // ------------------------------------------------------------------------------------------- upgrades
    public int getUpgrades() {
        return this.entityData.get(DATA_UPGRADES);
    }

    public void setUpgrades(int mask) {
        this.entityData.set(DATA_UPGRADES, mask & RaftUpgrade.ALL);
        if (!RaftUpgrade.ANCHOR.in(mask)) {
            this.entityData.set(DATA_ANCHORED, false);
        }
    }

    public boolean hasUpgrade(RaftUpgrade upgrade) {
        return upgrade.in(this.getUpgrades());
    }

    public boolean isAnchored() {
        return this.entityData.get(DATA_ANCHORED);
    }

    public boolean isFueled() {
        return this.entityData.get(DATA_FUELED);
    }

    /** True while the outboard motor pushes the raft (used for the propeller, smoke and engine sound). */
    public boolean isEngineRunning() {
        return this.hasUpgrade(RaftUpgrade.ENGINE) && this.isFueled() && !this.isAnchored() && this.getPaddleState(0) && this.getPaddleState(1);
    }

    /** Forward speed relative to a vanilla boat. */
    public float speedFactor() {
        float f = 1.0F;
        if (this.hasUpgrade(RaftUpgrade.SAIL)) {
            f += SAIL_BONUS;
        }
        if (this.hasUpgrade(RaftUpgrade.ENGINE) && this.isFueled()) {
            f += ENGINE_BONUS;
        }
        return BASE_SPEED * f;
    }

    /** Toggles the anchor (only with the anchor upgrade). Returns true if it changed. */
    public boolean toggleAnchor(@Nullable Player player) {
        if (!this.hasUpgrade(RaftUpgrade.ANCHOR)) {
            return false;
        }
        boolean anchored = !this.isAnchored();
        this.entityData.set(DATA_ANCHORED, anchored);
        if (anchored) {
            this.setDeltaMovement(0.0, this.getDeltaMovement().y, 0.0);
        }
        this.playSound(anchored ? SoundEvents.CHAIN_PLACE : SoundEvents.CHAIN_HIT, 1.0F, anchored ? 0.8F : 1.1F);
        if (anchored) {
            this.playSound(SoundEvents.GENERIC_SPLASH, 0.6F, 1.2F);
        }
        if (player != null) {
            player.sendOverlayMessage(Component.translatable(anchored ? "message.raft.expedition_raft.anchor_down" : "message.raft.expedition_raft.anchor_up"));
        }
        return true;
    }

    private InteractionResult applyUpgrade(Player player, ItemStack stack, RaftUpgrade upgrade) {
        if (this.hasUpgrade(upgrade)) {
            if (upgrade == RaftUpgrade.ANCHOR) {
                if (!this.level().isClientSide()) {
                    this.toggleAnchor(player);
                }
                return InteractionResult.SUCCESS;
            }
            if (!this.level().isClientSide()) {
                player.sendOverlayMessage(Component.translatable("message.raft.expedition_raft.has_upgrade", Component.translatable(upgrade.langKey())));
            }
            return InteractionResult.FAIL;
        }
        if (!this.level().isClientSide()) {
            this.setUpgrades(this.getUpgrades() | upgrade.bit());
            stack.consume(1, player);
            this.playSound(installSound(upgrade), 1.0F, 1.0F);
            this.gameEvent(GameEvent.ENTITY_INTERACT, player);
            player.sendOverlayMessage(Component.translatable("message.raft.expedition_raft.installed", Component.translatable(upgrade.langKey())));
        }
        return InteractionResult.SUCCESS;
    }

    private static SoundEvent installSound(RaftUpgrade upgrade) {
        return switch (upgrade) {
            case SAIL -> SoundEvents.WOOL_PLACE;
            case ENGINE -> SoundEvents.SMITHING_TABLE_USE;
            case ANCHOR -> SoundEvents.CHAIN_PLACE;
            case CRATE -> SoundEvents.WOOD_PLACE;
            case LANTERN -> SoundEvents.LANTERN_PLACE;
        };
    }

    // ------------------------------------------------------------------------------------------- seats
    @Override
    protected double rideHeight(EntityDimensions dimensions) {
        return 0.6;
    }

    @Override
    protected int getMaxPassengers() {
        return SEATS.length;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        int index = Math.max(0, this.getPassengers().indexOf(passenger));
        Vec3 seat = SEATS[Math.min(index, SEATS.length - 1)];
        return seat.yRot(-this.getYRot() * Mth.DEG_TO_RAD);
    }

    /**
     * Riders get off onto the deck between their seat and the middle of the raft. The player who steers is a tick or two
     * (plus the ping) ahead of the server, so their spot is moved on by the raft's speed over that time: at full speed a
     * spot at the helm would otherwise lie behind the raft on their screen and they would drop into the sea under it.
     */
    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        double x = Mth.lerp(DISMOUNT_TO_CENTRE, passenger.getX(), this.getX());
        double z = Mth.lerp(DISMOUNT_TO_CENTRE, passenger.getZ(), this.getZ());
        double y = this.getBoundingBox().maxY;
        Vec3 deck = new Vec3(x, y, z);
        if (passenger.getUUID().equals(this.steererId) && passenger instanceof ServerPlayer player) {
            double ticks = Math.min(MAX_LEAD_TICKS, 1.0 + player.connection.latency() / 50.0);
            Vec3 ahead = deck.add(this.steeredMotion.x * ticks, 0.0, this.steeredMotion.z * ticks);
            for (Pose pose : passenger.getDismountPoses()) {
                if (DismountHelper.canDismountTo(this.level(), ahead, passenger, pose)) {
                    passenger.setPose(pose);
                    return ahead;
                }
            }
        }
        for (Pose pose : passenger.getDismountPoses()) {
            if (DismountHelper.canDismountTo(this.level(), deck, passenger, pose)) {
                passenger.setPose(pose);
                return deck;
            }
        }
        return super.getDismountLocationForPassenger(passenger);
    }

    @Override
    public void setInput(boolean left, boolean right, boolean up, boolean down) {
        super.setInput(left, right, up, down);
        this.inputUp = up;
    }

    // ------------------------------------------------------------------------------------------- movement
    @Override
    public void tick() {
        AbstractBoatAccessor boat = (AbstractBoatAccessor) this;
        boat.raft$setOutOfControlTicks(0.0F);
        if (!this.level().isClientSide()) {
            this.trackSteering();
        }
        if (this.isLocalInstanceAuthoritative()) {
            if (this.isAnchored()) {
                this.setDeltaMovement(0.0, this.getDeltaMovement().y, 0.0);
            } else if (this.level().isClientSide() && this.inputUp && this.getControllingPassenger() instanceof Player) {
                // vanilla adds 0.04 per tick when rowing; add the difference to reach this raft's speed (friction 0.9)
                float extra = (this.speedFactor() - 1.0F) * 0.04F / 0.9F;
                float yaw = this.getYRot() * Mth.DEG_TO_RAD;
                this.setDeltaMovement(this.getDeltaMovement().add(-Mth.sin(yaw) * extra, 0.0, Mth.cos(yaw) * extra));
            }
            boat.raft$setDeltaRotation(boat.raft$getDeltaRotation() * TURN_DAMPING);
        }
        super.tick();
        boat.raft$setOutOfControlTicks(0.0F);
        if (this.level() instanceof ServerLevel serverLevel) {
            this.tickEngine(serverLevel);
        } else {
            this.tickClientEffects();
        }
        this.carryStandingEntities();
    }

    /**
     * Remembers who steers the raft and how far their client moved it since the last tick (the moves arrive between
     * ticks). Teleports and other jumps longer than a raft can sail in one tick do not count.
     */
    private void trackSteering() {
        Vec3 now = this.position();
        boolean steered = this.isClientAuthoritative() && this.getControllingPassenger() instanceof Player;
        Vec3 motion = steered && this.steeredFrom != null
                ? new Vec3(now.x - this.steeredFrom.x, 0.0, now.z - this.steeredFrom.z) : Vec3.ZERO;
        this.steeredMotion = motion.lengthSqr() < 2.25 ? motion : Vec3.ZERO;
        this.steererId = steered ? this.getControllingPassenger().getUUID() : null;
        this.steeredFrom = now;
    }

    @Override
    public void move(MoverType type, Vec3 delta) {
        if (type == MoverType.SELF && this.isAnchored()) {
            delta = new Vec3(0.0, delta.y, 0.0);
        }
        super.move(type, delta);
    }

    @Override
    public void push(double x, double y, double z) {
        if (!this.isAnchored()) {
            super.push(x, y, z);
        }
    }

    /**
     * Only blocks and other boats stop the raft. Mobs, items and players on the deck or in the water never block it
     * (the deck carries whatever stands on it, vanilla pushing moves swimmers aside).
     */
    @Override
    public boolean canCollideWith(Entity entity) {
        return entity instanceof AbstractBoat && super.canCollideWith(entity);
    }

    /**
     * The heavy raft is not shoved around by players and mobs that bump into it or stand on its deck edge (it still pushes
     * swimmers aside).
     */
    @Override
    public boolean isPushable() {
        return false;
    }

    /**
     * Players stand on the deck of the raft on their own client, which also carries them along. The server's copy of the
     * raft is a few ticks ahead of the one the client sees, so the server must not test players against it: it would
     * reject their moves as "inside an entity" and pull them back until they fall off.
     */
    @Override
    public boolean canBeCollidedWith(@Nullable Entity other) {
        return !(other instanceof Player) || this.level().isClientSide();
    }

    /** No oar splashes while the motor does the work. */
    @Override
    protected @Nullable SoundEvent getPaddleSound() {
        return this.isEngineRunning() ? null : super.getPaddleSound();
    }

    /** The raft floats over bubble columns instead of being pulled under. */
    @Override
    public void onAboveBubbleColumn(boolean dragDown, BlockPos pos) {
    }

    private void tickEngine(ServerLevel level) {
        boolean engine = this.hasUpgrade(RaftUpgrade.ENGINE);
        boolean throttle = engine && !this.isAnchored() && this.getControllingPassenger() != null
                && this.getPaddleState(0) && this.getPaddleState(1);
        if (throttle && this.burnTime > 0) {
            this.burnTime--;
        }
        if (throttle && this.burnTime <= 0) {
            ItemStack fuel = this.items.get(FUEL_SLOT);
            int duration = isFuel(fuel) ? this.burnDuration(level, fuel) : 0;
            if (duration > 0) {
                this.burnTime = duration;
                this.burnTotal = duration;
                ItemStackTemplate remainder = fuel.getItem().getCraftingRemainder();
                fuel.shrink(1);
                if (fuel.isEmpty() && remainder != null) {
                    this.items.set(FUEL_SLOT, remainder.create());
                }
            }
        }
        boolean fueled = engine && (this.burnTime > 0 || isFuel(this.items.get(FUEL_SLOT)));
        if (fueled != this.isFueled()) {
            this.entityData.set(DATA_FUELED, fueled);
        }
    }

    public static boolean isFuel(ItemStack stack) {
        return !stack.isEmpty() && stack.has(DataComponents.COOKING_FUEL);
    }

    /** Burn time of a fuel item in a furnace (the same data driven value the furnace uses). */
    private int burnDuration(ServerLevel level, ItemStack stack) {
        FurnaceBlockEntity probe = new FurnaceBlockEntity(this.blockPosition(), Blocks.FURNACE.defaultBlockState());
        LootContext context = new LootContext.Builder(new LootParams.Builder(level)
                .withParameter(LootContextParams.BLOCK_STATE, Blocks.FURNACE.defaultBlockState())
                .withParameter(LootContextParams.BLOCK_ENTITY, probe)
                .withParameter(LootContextParams.ORIGIN, this.position())
                .withParameter(LootContextParams.CONTAINER, this)
                .create(LootContextParamSets.CONTAINER_PROCESS))
                .create(Optional.empty());
        return ResolvableInt.getFromItem(stack, DataComponents.COOKING_FUEL, CookingFuel::burnTime, context, 0);
    }

    private Vec3 local(double x, double y, double z) {
        return new Vec3(x, y, z).yRot(-this.getYRot() * Mth.DEG_TO_RAD).add(this.position());
    }

    private void tickClientEffects() {
        this.anchorDropO = this.anchorDrop;
        this.anchorDrop = Mth.approach(this.anchorDrop, this.isAnchored() ? 1.0F : 0.0F, 0.035F);
        double dx = this.getX() - this.xo;
        double dz = this.getZ() - this.zo;
        float speed = (float) Math.sqrt(dx * dx + dz * dz);
        this.sailFillO = this.sailFill;
        this.sailFill += (Mth.clamp(speed / 0.45F, 0.0F, 1.0F) - this.sailFill) * 0.08F;
        this.turnO = this.turn;
        this.turn += (Mth.wrapDegrees(this.getYRot() - this.yRotO) - this.turn) * 0.25F;
        boolean running = this.isEngineRunning();
        this.propellerSpeed = Mth.approach(this.propellerSpeed, running ? 1.1F : 0.0F, running ? 0.08F : 0.02F);
        this.propellerO = this.propeller;
        this.propeller += this.propellerSpeed;
        if (this.propeller > Mth.TWO_PI * 8) {
            this.propeller -= Mth.TWO_PI * 8;
            this.propellerO -= Mth.TWO_PI * 8;
        }
        if (running) {
            if (this.tickCount % 3 == 0) {
                Vec3 exhaust = this.local(-1.0, 1.4, -1.7);
                this.level().addParticle(ParticleTypes.SMOKE, exhaust.x, exhaust.y, exhaust.z, 0.0, 0.04, 0.0);
            }
            Vec3 prop = this.local(-1.0, 0.0, -2.1);
            this.level().addParticle(ParticleTypes.BUBBLE, prop.x + (this.random.nextDouble() - 0.5) * 0.3, prop.y,
                    prop.z + (this.random.nextDouble() - 0.5) * 0.3, 0.0, 0.05, 0.0);
        }
        if (speed > 0.25F && this.random.nextFloat() < speed) {
            double side = (this.random.nextDouble() - 0.5) * 3.0;
            Vec3 bow = this.local(side, 0.45, 1.6);
            this.level().addParticle(ParticleTypes.SPLASH, bow.x, bow.y, bow.z, 0.0, 0.1, 0.0);
        }
    }

    /**
     * Keeps entities on the deck and moves them along with the raft (translation and turning). Each side handles the
     * entities it is authoritative for: the server its mobs and items, a client its own player. Anything resting on or
     * falling onto the deck is held at deck height, so nothing sinks into the raft while it bobs, turns or speeds up.
     */
    private void carryStandingEntities() {
        double x = this.getX();
        double y = this.getY();
        double z = this.getZ();
        float yaw = this.getYRot();
        double dx = 0.0;
        double dy = 0.0;
        double dz = 0.0;
        float dyaw = 0.0F;
        if (this.carryReady) {
            dx = x - this.carryX;
            dy = y - this.carryY;
            dz = z - this.carryZ;
            dyaw = Mth.wrapDegrees(yaw - this.carryYaw);
            if (dx * dx + dy * dy + dz * dz > 25.0) {
                // teleported: do not drag anything along
                dx = dy = dz = 0.0;
                dyaw = 0.0F;
            }
        }
        this.carryReady = true;
        this.carryX = x;
        this.carryY = y;
        this.carryZ = z;
        this.carryYaw = yaw;

        AABB now = this.getBoundingBox();
        AABB old = now.move(-dx, -dy, -dz);
        double oldTop = old.maxY;
        double top = now.maxY;
        AABB zone = new AABB(Math.min(old.minX, now.minX), Math.min(oldTop, top) - 4.0, Math.min(old.minZ, now.minZ),
                Math.max(old.maxX, now.maxX), Math.max(oldTop, top) + 0.8, Math.max(old.maxZ, now.maxZ));
        List<Entity> riders = this.level().getEntities(this, zone, e -> !e.isPassenger() && !e.noPhysics && !e.isSpectator()
                && e.isLocalInstanceAuthoritative() && !(e instanceof AbstractBoat) && !e.isNoGravity());
        if (riders.isEmpty()) {
            return;
        }
        boolean moving = dx != 0.0 || dy != 0.0 || dz != 0.0 || dyaw != 0.0F;
        float rad = dyaw * Mth.DEG_TO_RAD;
        float cos = Mth.cos(rad);
        float sin = Mth.sin(rad);
        for (Entity e : riders) {
            if (e.getX() < old.minX || e.getX() > old.maxX || e.getZ() < old.minZ || e.getZ() > old.maxZ) {
                continue;
            }
            double feet = e.getY();
            // fell through the deck this tick (fast fall) or rests in/just above it; never something swimming below
            boolean crossed = e.yo >= oldTop - 0.05 && feet < oldTop;
            if (feet < oldTop - 0.45 && !crossed) {
                continue;
            }
            Vec3 motion = e.getDeltaMovement();
            boolean onDeck = feet <= oldTop + 0.06 && (motion.y <= 0.0 || feet < oldTop);
            double tx = e.getX();
            double tz = e.getZ();
            if (moving) {
                double rx = e.getX() - old.getCenter().x;
                double rz = e.getZ() - old.getCenter().z;
                tx = now.getCenter().x + rx * cos - rz * sin;
                tz = now.getCenter().z + rx * sin + rz * cos;
            }
            double ty = onDeck ? top : feet;
            if (tx == e.getX() && ty == feet && tz == e.getZ() && dyaw == 0.0F) {
                continue;
            }
            Vec3 delta = new Vec3(tx - e.getX(), ty - feet, tz - e.getZ());
            if (this.level().noBlockCollision(e, e.getBoundingBox().move(delta))) {
                e.setPos(tx, ty, tz);
            } else if (onDeck && this.level().noBlockCollision(e, e.getBoundingBox().move(0.0, delta.y, 0.0))) {
                e.setPos(e.getX(), ty, e.getZ());
            }
            if (onDeck) {
                e.setDeltaMovement(motion.x, Math.max(0.0, motion.y), motion.z);
                e.setOnGround(true);
                e.resetFallDistance();
            }
            if (dyaw != 0.0F) {
                e.setYRot(e.getYRot() + dyaw);
                e.setYHeadRot(e.getYHeadRot() + dyaw);
                if (e instanceof LivingEntity living) {
                    living.yBodyRot += dyaw;
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------- damage
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (this.isRemoved()) {
            return true;
        }
        // waves, rain, mobs, sharks, arrows and explosions do not hurt the raft: only a player's own hits count
        if (!(source.getDirectEntity() instanceof Player player) || source.getEntity() != player || this.isInvulnerableToBase(source)) {
            return false;
        }
        this.setHurtDir(-this.getHurtDir());
        this.setHurtTime(10);
        this.markHurt();
        this.gameEvent(GameEvent.ENTITY_DAMAGE, player);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WOOD_HIT, SoundSource.NEUTRAL, 1.0F,
                0.8F + this.random.nextFloat() * 0.2F);
        if (player.getAbilities().instabuild) {
            this.breakApart(level, false);
            return true;
        }
        long now = level.getGameTime();
        if (now - this.lastHitTime > HIT_WINDOW) {
            this.hits = 0;
        }
        this.lastHitTime = now;
        this.hits++;
        this.setDamage(Math.min(40.0F, this.hits * 14.0F));
        if (this.hits >= 3 || player.isShiftKeyDown()) {
            this.breakApart(level, true);
        } else {
            int left = 3 - this.hits;
            player.sendOverlayMessage(left == 1 ? Component.translatable("message.raft.expedition_raft.hit_last")
                    : Component.translatable("message.raft.expedition_raft.hit", left));
        }
        return true;
    }

    private void breakApart(ServerLevel level, boolean dropItem) {
        if (dropItem && level.getGameRules().get(GameRules.ENTITY_DROPS)) {
            ItemStack stack = new ItemStack(this.getDropItem());
            stack.set(VoyageContent.RAFT_UPGRADES, this.getUpgrades());
            stack.set(DataComponents.CUSTOM_NAME, this.getCustomName());
            this.spawnAtLocation(level, stack);
        }
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WOOD_BREAK, SoundSource.NEUTRAL, 1.0F, 0.9F);
        this.kill(level);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide() && reason.shouldDestroy()) {
            Containers.dropContents(this.level(), this, this);
        }
        super.remove(reason);
    }

    // ------------------------------------------------------------------------------------------- interaction
    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() instanceof RaftUpgradeItem upgradeItem) {
            return this.applyUpgrade(player, stack, upgradeItem.upgrade());
        }
        if (player.isSecondaryUseActive()) {
            if (!this.level().isClientSide()) {
                this.openCustomInventoryScreen(player);
            }
            return InteractionResult.SUCCESS;
        }
        return super.interact(player, hand, location);
    }

    @Override
    public void openCustomInventoryScreen(Player player) {
        player.openMenu(this);
        if (!player.level().isClientSide()) {
            this.gameEvent(GameEvent.CONTAINER_OPEN, player);
        }
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        if (this.lootTable != null && player.isSpectator()) {
            return null;
        }
        this.unpackChestVehicleLootTable(inventory.player);
        return new ExpeditionRaftMenu(containerId, inventory, this, this.data, this.getUpgrades(), this);
    }

    @Override
    public Integer getScreenOpeningData(ServerPlayer player) {
        return this.getUpgrades();
    }

    // ------------------------------------------------------------------------------------------- container
    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == FUEL_SLOT) {
            return this.hasUpgrade(RaftUpgrade.ENGINE) && isFuel(stack);
        }
        return slot < ROW || this.hasUpgrade(RaftUpgrade.CRATE);
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.getChestVehicleItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        return this.removeChestVehicleItem(slot, count);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return this.removeChestVehicleItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.setChestVehicleItem(slot, stack);
    }

    @Override
    public @Nullable SlotAccess getSlot(int slot) {
        return this.getChestVehicleSlot(slot);
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(Player player) {
        return this.isChestVehicleStillValid(player);
    }

    @Override
    public void clearContent() {
        this.clearChestVehicleContent();
    }

    @Override
    public @Nullable ResourceKey<LootTable> getContainerLootTable() {
        return this.lootTable;
    }

    @Override
    public void setContainerLootTable(@Nullable ResourceKey<LootTable> lootTable) {
        this.lootTable = lootTable;
    }

    @Override
    public long getContainerLootTableSeed() {
        return this.lootTableSeed;
    }

    @Override
    public void setContainerLootTableSeed(long seed) {
        this.lootTableSeed = seed;
    }

    @Override
    public NonNullList<ItemStack> getItemStacks() {
        return this.items;
    }

    @Override
    public void clearItemStacks() {
        this.items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    }

    @Override
    public void stopOpen(ContainerUser user) {
        this.level().gameEvent(GameEvent.CONTAINER_CLOSE, this.position(), GameEvent.Context.of(user.getLivingEntity()));
    }

    // ------------------------------------------------------------------------------------------- save / components
    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        this.addChestVehicleSaveData(output);
        output.putInt("Upgrades", this.getUpgrades());
        output.putBoolean("Anchored", this.isAnchored());
        output.putInt("BurnTime", this.burnTime);
        output.putInt("BurnTotal", this.burnTotal);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.readChestVehicleSaveData(input);
        this.setUpgrades(input.getIntOr("Upgrades", 0));
        this.entityData.set(DATA_ANCHORED, this.hasUpgrade(RaftUpgrade.ANCHOR) && input.getBooleanOr("Anchored", false));
        this.burnTime = input.getIntOr("BurnTime", 0);
        this.burnTotal = input.getIntOr("BurnTotal", 0);
        this.entityData.set(DATA_FUELED, this.hasUpgrade(RaftUpgrade.ENGINE) && (this.burnTime > 0 || isFuel(this.items.get(FUEL_SLOT))));
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        this.applyImplicitComponentIfPresent(components, VoyageContent.RAFT_UPGRADES);
    }

    @Override
    protected <T> boolean applyImplicitComponent(DataComponentType<T> type, T value) {
        if (type == VoyageContent.RAFT_UPGRADES) {
            this.setUpgrades(castComponentValue(VoyageContent.RAFT_UPGRADES, value));
            return true;
        }
        return super.applyImplicitComponent(type, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> @Nullable T get(DataComponentType<? extends T> type) {
        if (type == VoyageContent.RAFT_UPGRADES) {
            return castComponentValue((DataComponentType<T>) type, this.getUpgrades());
        }
        return super.get(type);
    }

    // ------------------------------------------------------------------------------------------- client animation getters
    public float getAnchorDrop(float partialTicks) {
        return Mth.lerp(partialTicks, this.anchorDropO, this.anchorDrop);
    }

    public float getSailFill(float partialTicks) {
        return Mth.lerp(partialTicks, this.sailFillO, this.sailFill);
    }

    public float getPropellerAngle(float partialTicks) {
        return Mth.lerp(partialTicks, this.propellerO, this.propeller);
    }

    public float getTurn(float partialTicks) {
        return Mth.lerp(partialTicks, this.turnO, this.turn);
    }
}
