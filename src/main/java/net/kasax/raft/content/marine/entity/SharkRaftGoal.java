package net.kasax.raft.content.marine.entity;

import net.kasax.raft.block.ModBlocks;
import net.kasax.raft.content.marine.MarineGameRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The shark's raft raid: when a player stands on a raft of foundations floating in the water, the shark circles it
 * at the surface (dorsal fin out of the water), and every 15–45 seconds swims to an edge {@code raft:raft_foundation}
 * and bites it. After {@link #BITES_TO_BREAK} bites the block breaks. Reinforced foundations are never bitten.
 * A shark that is hurt while raiding flees for a few seconds. Biting needs the game rule {@code raft:shark_bites}.
 */
public class SharkRaftGoal extends Goal {
    public static final int BITES_TO_BREAK = 4;
    private static final int SCAN_RANGE = 24;
    private static final int MAX_RAFT_BLOCKS = 512;

    private enum Phase { CIRCLE, APPROACH, BITE, RETREAT }

    private final AbstractMarineMonster shark;
    private @Nullable Player player;
    private Phase phase = Phase.CIRCLE;
    private final List<BlockPos> raftBlocks = new ArrayList<>();
    private double centerX, centerZ, radius;
    private int raftY;
    private double swimY;
    private int direction = 1;
    /** Shark tick count before which no new bite starts (survives goal restarts). */
    private int nextBiteTick;
    /** Shark tick count until which the shark flees after being hurt. */
    private int scaredUntil;
    private int phaseTicks;
    private int bites;
    private int rescanTicks;
    private int nextScan;
    private @Nullable BlockPos edge;
    private @Nullable Vec3 biteSpot;
    private boolean active;

    public SharkRaftGoal(AbstractMarineMonster shark) {
        this.shark = shark;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    public boolean isActive() {
        return this.active;
    }

    @Override
    public boolean canUse() {
        if (this.shark.getTarget() != null || !this.shark.isInWater()) {
            return false;
        }
        if (--this.nextScan > 0) {
            return false;
        }
        this.nextScan = 30 + this.shark.getRandom().nextInt(30);
        Player found = this.findRaftPlayer();
        if (found == null) {
            return false;
        }
        this.player = found;
        return this.scanRaft(found);
    }

    @Override
    public boolean canContinueToUse() {
        return this.player != null && this.player.isAlive() && !this.player.isSpectator()
                && this.shark.getTarget() == null && !this.raftBlocks.isEmpty()
                && this.shark.distanceToSqr(this.centerX, this.raftY, this.centerZ) < 48 * 48;
    }

    @Override
    public void start() {
        this.active = true;
        this.phase = this.shark.tickCount < this.scaredUntil ? Phase.RETREAT : Phase.CIRCLE;
        this.phaseTicks = 0;
        this.nextBiteTick = Math.max(this.nextBiteTick, this.shark.tickCount + 200 + this.shark.getRandom().nextInt(300));
        this.direction = this.shark.getRandom().nextBoolean() ? 1 : -1;
        this.rescanTicks = 0;
    }

    @Override
    public void stop() {
        this.clearProgress();
        this.active = false;
        this.player = null;
        this.raftBlocks.clear();
        this.edge = null;
        this.shark.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    /** Called by the shark when it is hurt: a raiding shark (or one hit from a raft) lets go and flees. */
    public void onHurt(DamageSource source) {
        boolean fromRaft = source.getEntity() instanceof Player p && isOnRaft(p);
        if (this.active || fromRaft) {
            this.clearProgress();
            this.edge = null;
            this.phase = Phase.RETREAT;
            this.phaseTicks = 0;
            this.scaredUntil = this.shark.tickCount + 140;
            this.nextBiteTick = this.shark.tickCount + 500 + this.shark.getRandom().nextInt(400);
        }
    }

    /** True while the shark flees after being hurt on a raid. */
    public boolean isScared() {
        return this.shark.tickCount < this.scaredUntil;
    }

    @Override
    public void tick() {
        this.phaseTicks++;
        if (++this.rescanTicks >= 100) {
            this.rescanTicks = 0;
            if (this.player == null || !isOnRaft(this.player) || !this.scanRaft(this.player)) {
                this.raftBlocks.clear();
                return;
            }
        }
        switch (this.phase) {
            case CIRCLE -> this.tickCircle();
            case APPROACH -> this.tickApproach();
            case BITE -> this.tickBite();
            case RETREAT -> this.tickRetreat();
        }
    }

    // ------------------------------------------------------------------------------------------------ phases
    private void tickCircle() {
        double angle = Math.atan2(this.shark.getZ() - this.centerZ, this.shark.getX() - this.centerX);
        double ahead = angle + this.direction * Math.min(0.9, 4.0 / this.radius);
        double tx = this.centerX + Math.cos(ahead) * this.radius;
        double tz = this.centerZ + Math.sin(ahead) * this.radius;
        if (this.phaseTicks % 5 == 0) {
            if (!this.isOpenWater(tx, tz)) {
                this.direction = -this.direction;
            } else {
                this.shark.getNavigation().moveTo(tx, this.swimY, tz, 1.0);
            }
        }
        // keep the fin above the water while circling
        double dy = this.swimY - this.shark.getY();
        if (Math.abs(dy) > 0.15) {
            this.shark.setDeltaMovement(this.shark.getDeltaMovement().add(0.0, Mth.clamp(dy * 0.02, -0.02, 0.02), 0.0));
        }
        if (this.shark.tickCount < this.nextBiteTick) {
            return;
        }
        if (this.shark.level() instanceof ServerLevel level && MarineGameRules.sharkBites(level) && this.pickEdge()) {
            this.phase = Phase.APPROACH;
            this.phaseTicks = 0;
        } else {
            this.nextBiteTick = this.shark.tickCount + 200;
        }
    }

    private void tickApproach() {
        if (this.edge == null || this.biteSpot == null || !this.isBiteable(this.edge)) {
            this.backToCircle(100);
            return;
        }
        this.shark.getLookControl().setLookAt(this.edge.getX() + 0.5, this.edge.getY() + 0.5, this.edge.getZ() + 0.5);
        if (this.phaseTicks % 10 == 1) {
            this.shark.getNavigation().moveTo(this.biteSpot.x, this.biteSpot.y, this.biteSpot.z, 1.3);
        }
        if (this.shark.position().distanceToSqr(this.biteSpot) < 1.6 * 1.6
                || this.shark.getBoundingBox().inflate(0.9).intersects(new net.minecraft.world.phys.AABB(this.edge))) {
            this.phase = Phase.BITE;
            this.phaseTicks = 0;
            this.bites = 0;
            this.shark.getNavigation().stop();
        } else if (this.phaseTicks > 240) {
            this.backToCircle(200);
        }
    }

    private void tickBite() {
        if (this.edge == null || !this.isBiteable(this.edge) || !(this.shark.level() instanceof ServerLevel level)) {
            this.backToCircle(200);
            return;
        }
        Vec3 c = Vec3.atCenterOf(this.edge);
        this.shark.getLookControl().setLookAt(c.x, c.y, c.z, 30.0F, 30.0F);
        // hold position against the raft edge
        Vec3 pull = c.subtract(this.shark.position()).multiply(1.0, 0.0, 1.0);
        if (pull.lengthSqr() > 1.0) {
            this.shark.setDeltaMovement(this.shark.getDeltaMovement().add(pull.normalize().scale(0.02)));
        }
        if (this.phaseTicks % 22 != 0) {
            return;
        }
        this.bites++;
        level.broadcastEntityEvent(this.shark, AbstractMarineMonster.EVENT_BITE);
        BlockState state = level.getBlockState(this.edge);
        level.playSound(null, this.edge, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.HOSTILE, 0.7F,
                1.2F + this.shark.getRandom().nextFloat() * 0.3F);
        level.playSound(null, this.edge, SoundEvents.EVOKER_FANGS_ATTACK, SoundSource.HOSTILE, 0.6F, 1.4F);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), c.x, c.y - 0.3, c.z, 14, 0.35, 0.25, 0.35, 0.1);
        level.sendParticles(ParticleTypes.BUBBLE, c.x, c.y - 0.6, c.z, 8, 0.4, 0.2, 0.4, 0.05);
        // a little tug on each bite
        this.shark.setDeltaMovement(this.shark.getDeltaMovement().add(c.subtract(this.shark.position()).normalize().scale(0.15)));
        if (this.bites >= BITES_TO_BREAK) {
            level.destroyBlockProgress(this.shark.getId(), this.edge, -1);
            level.destroyBlock(this.edge, false, this.shark);
            level.playSound(null, this.edge, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.HOSTILE, 0.6F, 1.1F);
            this.raftBlocks.remove(this.edge);
            this.edge = null;
            this.backToCircle(500 + this.shark.getRandom().nextInt(500));
        } else {
            level.destroyBlockProgress(this.shark.getId(), this.edge, this.bites * 10 / BITES_TO_BREAK - 1);
        }
    }

    private void tickRetreat() {
        double dx = this.shark.getX() - this.centerX;
        double dz = this.shark.getZ() - this.centerZ;
        double len = Math.max(0.1, Math.sqrt(dx * dx + dz * dz));
        double dist = this.radius + 12.0;
        if (this.phaseTicks % 10 == 1) {
            this.shark.getNavigation().moveTo(this.centerX + dx / len * dist, this.swimY - 2.0, this.centerZ + dz / len * dist, 1.6);
        }
        if (this.shark.tickCount >= this.scaredUntil && this.phaseTicks > 40) {
            this.phase = Phase.CIRCLE;
            this.phaseTicks = 0;
        }
    }

    private void backToCircle(int cooldown) {
        this.clearProgress();
        this.edge = null;
        this.phase = Phase.CIRCLE;
        this.phaseTicks = 0;
        this.nextBiteTick = this.shark.tickCount + cooldown;
    }

    private void clearProgress() {
        if (this.edge != null && this.shark.level() instanceof ServerLevel level) {
            level.destroyBlockProgress(this.shark.getId(), this.edge, -1);
        }
        this.bites = 0;
    }

    // ------------------------------------------------------------------------------------------------ raft detection
    private @Nullable Player findRaftPlayer() {
        Level level = this.shark.level();
        Player best = null;
        double bestDist = SCAN_RANGE * SCAN_RANGE;
        for (Player p : level.players()) {
            if (p.isSpectator() || p.isCreative() && p.getAbilities().flying) {
                continue;
            }
            double d = p.distanceToSqr(this.shark);
            if (d < bestDist && isOnRaft(p)) {
                best = p;
                bestDist = d;
            }
        }
        return best;
    }

    private static boolean isFoundation(BlockState state) {
        return state.is(ModBlocks.RAFT_FOUNDATION) || state.is(ModBlocks.REINFORCED_FOUNDATION);
    }

    /** A player stands on (or just above) a foundation that floats in the water. */
    public static boolean isOnRaft(Player player) {
        if (player.isInWater() || player.isPassenger()) {
            return false;
        }
        Level level = player.level();
        BlockPos feet = player.blockPosition();
        for (int i = 1; i <= 3; i++) {
            BlockPos p = feet.below(i);
            BlockState s = level.getBlockState(p);
            if (isFoundation(s)) {
                return touchesWater(level, p);
            }
            if (!s.isAir() && i > 1) {
                return false;
            }
        }
        return false;
    }

    private static boolean touchesWater(Level level, BlockPos pos) {
        if (level.getFluidState(pos.below()).is(FluidTags.WATER)) {
            return true;
        }
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (level.getFluidState(pos.relative(d)).is(FluidTags.WATER) || level.getFluidState(pos.relative(d).below()).is(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }

    /** Flood-fills the raft below the player and computes the circle around it. */
    private boolean scanRaft(Player p) {
        Level level = this.shark.level();
        BlockPos start = null;
        for (int i = 1; i <= 3 && start == null; i++) {
            BlockPos b = p.blockPosition().below(i);
            if (isFoundation(level.getBlockState(b))) {
                start = b;
            }
        }
        if (start == null) {
            return false;
        }
        this.raftBlocks.clear();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start);
        int minX = start.getX(), maxX = start.getX(), minZ = start.getZ(), maxZ = start.getZ();
        while (!queue.isEmpty() && this.raftBlocks.size() < MAX_RAFT_BLOCKS) {
            BlockPos b = queue.poll();
            this.raftBlocks.add(b);
            minX = Math.min(minX, b.getX());
            maxX = Math.max(maxX, b.getX());
            minZ = Math.min(minZ, b.getZ());
            maxZ = Math.max(maxZ, b.getZ());
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos n = b.relative(d);
                if (seen.add(n) && isFoundation(level.getBlockState(n))) {
                    queue.add(n);
                }
            }
        }
        this.raftY = start.getY();
        this.centerX = (minX + maxX + 1) / 2.0;
        this.centerZ = (minZ + maxZ + 1) / 2.0;
        this.radius = Math.max(maxX - minX + 1, maxZ - minZ + 1) / 2.0 * 1.15 + 3.5;
        // water surface next to the raft: the highest water block at or below the foundation level
        int surface = this.raftY;
        BlockPos probe = BlockPos.containing(this.centerX + this.radius, this.raftY, this.centerZ);
        for (int i = 0; i < 6; i++) {
            if (level.getFluidState(probe.below(i)).is(FluidTags.WATER)) {
                surface = probe.getY() - i;
                break;
            }
        }
        // feet just above the top water block: the body stays under water, the dorsal fin cuts the surface
        this.swimY = surface + 0.05;
        return true;
    }

    private boolean isOpenWater(double x, double z) {
        BlockPos p = BlockPos.containing(x, this.swimY + 0.5, z);
        Level level = this.shark.level();
        return level.getFluidState(p).is(FluidTags.WATER) && level.getFluidState(p.below()).is(FluidTags.WATER);
    }

    private boolean isBiteable(BlockPos pos) {
        return this.shark.level().getBlockState(pos).is(ModBlocks.RAFT_FOUNDATION);
    }

    /** Picks the edge raft_foundation closest to the shark and the water spot next to it. */
    private boolean pickEdge() {
        Level level = this.shark.level();
        BlockPos best = null;
        Vec3 bestSpot = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos b : this.raftBlocks) {
            if (!level.getBlockState(b).is(ModBlocks.RAFT_FOUNDATION)) {
                continue;
            }
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos n = b.relative(d);
                if (isFoundation(level.getBlockState(n))) {
                    continue;
                }
                BlockPos water = level.getFluidState(n).is(FluidTags.WATER) ? n
                        : level.getFluidState(n.below()).is(FluidTags.WATER) ? n.below() : null;
                if (water == null) {
                    continue;
                }
                Vec3 spot = new Vec3(water.getX() + 0.5 + d.getStepX() * 0.6, water.getY(), water.getZ() + 0.5 + d.getStepZ() * 0.6);
                double dist = spot.distanceToSqr(this.shark.position());
                if (dist < bestDist) {
                    bestDist = dist;
                    best = b;
                    bestSpot = spot;
                }
            }
        }
        this.edge = best;
        this.biteSpot = bestSpot;
        return best != null;
    }
}
