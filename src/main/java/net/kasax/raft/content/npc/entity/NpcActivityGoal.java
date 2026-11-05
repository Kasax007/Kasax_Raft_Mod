package net.kasax.raft.content.npc.entity;

import net.kasax.raft.content.npc.NpcCharacter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

/**
 * What a person of the sea does when nobody talks to them ({@link NpcCharacter.Idle}): walk to a place of interest and
 * work or read there, watch the sea through a spyglass, fish from the pier, or sit by the camp fire. Between two
 * activities the NPC strolls or looks around (the vanilla goals with lower priority).
 */
final class NpcActivityGoal extends Goal {
    private enum Phase { NONE, WALK, WORK, GAZE, FISH, SIT }

    private final NpcEntity npc;
    private Phase phase = Phase.NONE;
    private Phase after = Phase.NONE;
    private long nextStart;
    private int timer;
    private int beat;
    private int step;
    private int cycles;
    private @Nullable BlockPos target;
    private @Nullable Vec3 lookAt;
    private @Nullable BlockPos fishSpot;
    private boolean fishSearched;
    private @Nullable BlockPos seat;
    private @Nullable BlockPos fire;
    private boolean seatSearched;

    NpcActivityGoal(NpcEntity npc) {
        this.npc = npc;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    private ServerLevel level() {
        return (ServerLevel) this.npc.level();
    }

    @Override
    public boolean canUse() {
        if (this.npc.isTalking() || !this.npc.hasHome() || !(this.npc.level() instanceof ServerLevel level)
                || level.getGameTime() < this.nextStart) {
            return false;
        }
        if (this.plan()) {
            return true;
        }
        // nothing to do right now (no place found or no path): try again later, not every other tick
        this.nextStart = level.getGameTime() + 60 + this.npc.getRandom().nextInt(80);
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return this.phase != Phase.NONE && !this.npc.isTalking();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    // ------------------------------------------------------------------------------------------ planning
    private boolean plan() {
        NpcCharacter.Idle idle = this.npc.character().idle;
        return switch (idle) {
            case SIT -> this.planSit();
            case FISH -> this.planFish() || this.planWork();
            case GAZE -> (this.npc.getRandom().nextInt(5) < 2 && this.planGaze()) || this.planWork();
            case WORK, READ -> this.planWork();
        };
    }

    private boolean planWork() {
        List<BlockPos> places = this.npc.places();
        if (places.isEmpty()) {
            return false;
        }
        BlockPos place = places.get(this.npc.getRandom().nextInt(places.size()));
        // a lantern or chime up high: stand on the floor below it and look up, never climb up to it
        BlockPos home = this.npc.getHomePosition();
        BlockPos stand = place.getY() > home.getY() + 1 ? new BlockPos(place.getX(), home.getY(), place.getZ()) : place;
        if (!this.walkTo(stand, Phase.WORK, 1)) {
            return false;
        }
        this.target = place;
        return true;
    }

    private boolean planGaze() {
        Vec3 eye = this.npc.getEyePosition();
        Vec3 best = null;
        for (int i = 0; i < 8 && best == null; i++) {
            float yaw = this.npc.getRandom().nextFloat() * 360.0F;
            Vec3 dir = Vec3.directionFromRotation(-4.0F, yaw);
            Vec3 end = eye.add(dir.scale(14.0));
            if (this.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.npc))
                    .getType() == HitResult.Type.MISS) {
                best = eye.add(dir.scale(30.0));
            }
        }
        if (best == null) {
            return false;
        }
        this.lookAt = best;
        this.phase = Phase.GAZE;
        this.timer = 80 + this.npc.getRandom().nextInt(80);
        return true;
    }

    private boolean planFish() {
        if (!this.fishSearched) {
            this.fishSearched = true;
            this.fishSpot = this.findFishSpot();
        }
        if (this.fishSpot == null) {
            return false;
        }
        BlockPos home = this.npc.getHomePosition();
        if (this.npc.position().distanceToSqr(Vec3.atBottomCenterOf(home)) > 2.25) {
            return this.walkTo(home, Phase.FISH, 0);
        }
        this.phase = Phase.FISH;
        return true;
    }

    private boolean planSit() {
        if (!this.seatSearched) {
            this.seatSearched = true;
            this.findSeat();
        }
        BlockPos s = this.seat != null ? this.seat : this.npc.getHomePosition();
        if (this.npc.position().distanceToSqr(Vec3.atBottomCenterOf(s)) > 1.0) {
            return this.walkTo(s, Phase.SIT, 0);
        }
        this.phase = Phase.SIT;
        return true;
    }

    private boolean walkTo(BlockPos pos, Phase then, int accuracy) {
        Path path = this.npc.getNavigation().createPath(pos, accuracy);
        if (path == null) {
            return false;
        }
        this.target = pos;
        this.after = then;
        this.phase = Phase.WALK;
        this.timer = 240;
        this.npc.getNavigation().moveTo(path, 0.5);
        return true;
    }

    // ------------------------------------------------------------------------------------------ phases
    @Override
    public void start() {
        this.begin(this.phase);
    }

    private void begin(Phase p) {
        this.phase = p;
        this.beat = 0;
        this.step = 0;
        this.cycles = 0;
        switch (p) {
            case WORK -> {
                this.npc.getNavigation().stop();
                this.timer = 60 + this.npc.getRandom().nextInt(100);
                this.beat = 10;
            }
            case GAZE -> {
                this.npc.getNavigation().stop();
                this.npc.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.SPYGLASS));
                this.npc.startUsingItem(InteractionHand.MAIN_HAND);
                this.level().playSound(null, this.npc.getX(), this.npc.getY(), this.npc.getZ(), SoundEvents.SPYGLASS_USE,
                        SoundSource.NEUTRAL, 0.6F, 1.0F);
            }
            case FISH -> {
                this.npc.getNavigation().stop();
                this.beat = 20;
            }
            case SIT -> {
                this.npc.getNavigation().stop();
                this.npc.setSitting(true);
                this.timer = 700 + this.npc.getRandom().nextInt(900);
                this.faceFire();
            }
            default -> {
            }
        }
    }

    /** A player standing close by gets a glance (working, reading or sitting people notice visitors). */
    private boolean glanceAtVisitor() {
        net.minecraft.world.entity.player.Player p = this.level().getNearestPlayer(this.npc, 3.5);
        if (p == null || p.isSpectator()) {
            return false;
        }
        this.npc.getLookControl().setLookAt(p, 30.0F, 30.0F);
        return true;
    }

    @Override
    public void tick() {
        if ((this.phase == Phase.WORK || this.phase == Phase.SIT) && this.glanceAtVisitor()) {
            if (--this.timer <= 0) {
                if (this.phase == Phase.SIT) {
                    this.npc.setSitting(false);
                }
                this.end(120);
            }
            return;
        }
        switch (this.phase) {
            case WALK -> this.tickWalk();
            case WORK -> this.tickWork();
            case GAZE -> {
                if (this.lookAt != null) {
                    this.npc.getLookControl().setLookAt(this.lookAt.x, this.lookAt.y, this.lookAt.z, 10.0F, 30.0F);
                }
                if (--this.timer <= 0) {
                    this.end(160);
                }
            }
            case FISH -> this.tickFish();
            case SIT -> {
                if (this.fire != null) {
                    Vec3 f = Vec3.atCenterOf(this.fire);
                    this.npc.getLookControl().setLookAt(f.x, f.y + 0.2, f.z, 10.0F, 30.0F);
                }
                if (--this.timer <= 0) {
                    this.npc.setSitting(false);
                    this.end(300 + this.npc.getRandom().nextInt(300));
                }
            }
            default -> {
            }
        }
    }

    private void tickWalk() {
        if (this.target == null) {
            this.end(40);
            return;
        }
        double reach = this.after == Phase.WORK ? 2.6 : 1.1;
        double d = this.npc.position().distanceToSqr(Vec3.atBottomCenterOf(this.target));
        if (this.after == Phase.WORK) {
            Vec3 c = Vec3.atCenterOf(this.target);
            double dx = c.x - this.npc.getX(), dz = c.z - this.npc.getZ();
            d = dx * dx + dz * dz;               // horizontal: places up high are worked on from the floor
        }
        if (d < reach * reach) {
            this.begin(this.after);
        } else if (--this.timer <= 0) {
            this.end(100);
        } else if (this.npc.getNavigation().isDone()) {
            if (d < 9.0) {
                this.begin(this.after);
            } else {
                this.end(100);
            }
        }
    }

    private void tickWork() {
        if (this.target == null) {
            this.end(40);
            return;
        }
        Vec3 c = Vec3.atCenterOf(this.target);
        if (this.npc.character().idle == NpcCharacter.Idle.READ) {
            // reading the notes in hand: head bowed, now and then a glance at the place
            if (this.step == 0) {
                Vec3 ahead = this.npc.eyeAhead(1.0);
                this.npc.getLookControl().setLookAt(ahead.x, this.npc.getEyeY() - 1.4, ahead.z, 10.0F, 40.0F);
            } else {
                this.npc.getLookControl().setLookAt(c.x, c.y, c.z, 10.0F, 30.0F);
            }
            if (--this.beat <= 0) {
                this.step = this.step == 0 && this.npc.getRandom().nextInt(3) == 0 ? 1 : 0;
                this.beat = 30 + this.npc.getRandom().nextInt(40);
            }
        } else {
            this.npc.getLookControl().setLookAt(c.x, c.y, c.z, 10.0F, 30.0F);
            if (--this.beat <= 0) {
                this.npc.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT);
                this.beat = 14 + this.npc.getRandom().nextInt(26);
            }
        }
        if (--this.timer <= 0) {
            this.end(80 + this.npc.getRandom().nextInt(160));
        }
    }

    private void tickFish() {
        if (this.fishSpot == null) {
            this.end(100);
            return;
        }
        ServerLevel level = this.level();
        Vec3 s = new Vec3(this.fishSpot.getX() + 0.5, this.fishSpot.getY() + 0.95, this.fishSpot.getZ() + 0.5);
        this.npc.getLookControl().setLookAt(s.x, s.y, s.z, 10.0F, 30.0F);
        if (--this.beat > 0) {
            return;
        }
        switch (this.step) {
            case 0 -> {   // cast
                this.npc.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT);
                level.playSound(null, this.npc.getX(), this.npc.getY(), this.npc.getZ(), SoundEvents.FISHING_BOBBER_THROW,
                        SoundSource.NEUTRAL, 0.5F, 0.4F / (this.npc.getRandom().nextFloat() * 0.4F + 0.8F));
                this.beat = 18;
                this.step = 1;
            }
            case 1 -> {   // the float lands
                level.sendParticles(ParticleTypes.SPLASH, s.x, s.y, s.z, 10, 0.15, 0.0, 0.15, 0.05);
                this.beat = 70 + this.npc.getRandom().nextInt(140);
                this.step = 2;
            }
            case 2 -> {   // a bite
                level.sendParticles(ParticleTypes.FISHING, s.x, s.y, s.z, 8, 0.1, 0.0, 0.1, 0.02);
                level.sendParticles(ParticleTypes.SPLASH, s.x, s.y, s.z, 16, 0.2, 0.0, 0.2, 0.1);
                level.playSound(null, s.x, s.y, s.z, SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.NEUTRAL, 0.25F, 1.0F);
                this.beat = 8;
                this.step = 3;
            }
            default -> {  // reel in, sometimes with a fish
                this.npc.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT);
                level.playSound(null, this.npc.getX(), this.npc.getY(), this.npc.getZ(), SoundEvents.FISHING_BOBBER_RETRIEVE,
                        SoundSource.NEUTRAL, 0.6F, 0.9F + this.npc.getRandom().nextFloat() * 0.2F);
                if (this.npc.getRandom().nextInt(5) < 3) {
                    var fish = this.npc.getRandom().nextBoolean() ? Items.COD : Items.SALMON;
                    Vec3 mid = s.add(this.npc.getEyePosition()).scale(0.5);
                    level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, fish), mid.x, mid.y + 0.4, mid.z, 6, 0.15, 0.15, 0.15, 0.05);
                    level.sendParticles(ParticleTypes.SPLASH, s.x, s.y, s.z, 12, 0.2, 0.0, 0.2, 0.1);
                }
                this.beat = 40 + this.npc.getRandom().nextInt(50);
                this.step = 0;
                if (++this.cycles >= 3 + this.npc.getRandom().nextInt(3)) {
                    this.end(120 + this.npc.getRandom().nextInt(160));
                }
            }
        }
    }

    private void end(int pause) {
        this.phase = Phase.NONE;
        this.nextStart = this.level().getGameTime() + pause;
    }

    @Override
    public void stop() {
        boolean interrupted = this.phase != Phase.NONE;
        if (this.npc.isUsingItem()) {
            this.npc.stopUsingItem();
        }
        if (this.npc.getMainHandItem().is(Items.SPYGLASS) && !this.npc.heldItem().is(Items.SPYGLASS)) {
            this.npc.setItemSlot(EquipmentSlot.MAINHAND, this.npc.heldItem());
        }
        if (this.npc.isSitting() && !this.npc.isTalking()) {
            this.npc.setSitting(false);
        }
        this.npc.getNavigation().stop();
        this.phase = Phase.NONE;
        if (interrupted) {
            // a conversation interrupted the activity: pick it up again soon after
            this.nextStart = this.level().getGameTime() + 20;
        }
    }

    // ------------------------------------------------------------------------------------------ spots
    /** The nearest sea surface 2-7 blocks from home and 1-6 blocks below the feet (the fisher's float). */
    private @Nullable BlockPos findFishSpot() {
        ServerLevel level = this.level();
        BlockPos home = this.npc.getHomePosition();
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (int dx = -7; dx <= 7; dx++) {
            for (int dz = -7; dz <= 7; dz++) {
                double h = dx * dx + dz * dz;
                if (h < 4 || h > 49 || h >= bestD) {
                    continue;
                }
                for (int dy = -1; dy >= -6; dy--) {
                    BlockPos p = home.offset(dx, dy, dz);
                    BlockState st = level.getBlockState(p);
                    if (st.getFluidState().is(Fluids.WATER)) {
                        if (level.getBlockState(p.above()).isAir()) {
                            best = p;
                            bestD = h;
                        }
                        break;
                    }
                    if (!st.isAir()) {
                        break;
                    }
                }
            }
        }
        return best;
    }

    /** A free cell two blocks from the nearest camp fire, on the side closest to home (else home itself). */
    private void findSeat() {
        ServerLevel level = this.level();
        BlockPos home = this.npc.getHomePosition();
        BlockPos found = null;
        for (BlockPos p : BlockPos.betweenClosed(home.offset(-7, -2, -7), home.offset(7, 2, 7))) {
            if (level.getBlockState(p).is(BlockTags.CAMPFIRES)
                    && (found == null || p.distSqr(home) < found.distSqr(home))) {
                found = p.immutable();
            }
        }
        this.fire = found;
        if (found == null) {
            return;
        }
        BlockPos bestSeat = null;
        int[][] around = {{2, 0}, {-2, 0}, {0, 2}, {0, -2}, {2, 1}, {2, -1}, {-2, 1}, {-2, -1}, {1, 2}, {-1, 2}, {1, -2}, {-1, -2}};
        for (int[] o : around) {
            for (int dy = 0; dy >= -1; dy--) {
                BlockPos c = found.offset(o[0], dy, o[1]);
                if (level.getBlockState(c).getCollisionShape(level, c).isEmpty()
                        && level.getBlockState(c.above()).getCollisionShape(level, c.above()).isEmpty()
                        && !level.getBlockState(c.below()).getCollisionShape(level, c.below()).isEmpty()
                        && level.getFluidState(c).isEmpty()) {
                    if (bestSeat == null || c.distSqr(home) < bestSeat.distSqr(home)) {
                        bestSeat = c;
                    }
                    break;
                }
            }
        }
        this.seat = bestSeat;
    }

    private void faceFire() {
        if (this.fire == null) {
            return;
        }
        double dx = this.fire.getX() + 0.5 - this.npc.getX();
        double dz = this.fire.getZ() + 0.5 - this.npc.getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        this.npc.setYRot(yaw);
        this.npc.setYBodyRot(yaw);
        this.npc.setYHeadRot(yaw);
    }
}
