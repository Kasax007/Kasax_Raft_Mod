package net.kasax.raft.content.boss.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The visual body of the Leviathan: a chain of joints (neck, six segment ends, tail stock end) that follows the head
 * like a rope ("follow the leader"). Out of water the joints settle onto the ground (the body drapes over the pool
 * rim when the Leviathan lies stranded); in water they float. Only simulated on the client; the hitbox is the head.
 * Lengths and radii match the boxes of {@code LeviathanModel}.
 */
public final class LeviathanBody {
    /** Distance of the neck joint behind the head pivot (blocks). */
    public static final double NECK = 0.7;
    /** Body segments (the tail stock is the last link). */
    public static final int SEGMENTS = 6;
    public static final int JOINTS = SEGMENTS + 2;
    /** Uniform scale of each segment box (segments 0-2 use the big box, 3-5 the slimmer one). */
    public static final float[] SEGMENT_SCALE = {1.0F, 0.94F, 0.87F, 0.95F, 0.84F, 0.72F};
    /** Box depth in pixels of the two segment shapes and the tail stock. */
    public static final float SEG_A_DEPTH = 26.0F;
    public static final float SEG_B_DEPTH = 24.0F;
    public static final float TAIL_DEPTH = 20.0F;
    /** Link lengths between joint i and i+1 (blocks). */
    public static final double[] LENGTHS = new double[JOINTS - 1];
    /** Half thickness of the body at joint i (blocks), used to rest it on the ground. */
    public static final double[] RADIUS = new double[JOINTS];

    static {
        for (int i = 0; i < SEGMENTS; i++) {
            float depth = i < 3 ? SEG_A_DEPTH : SEG_B_DEPTH;
            float height = i < 3 ? 22.0F : 18.0F;
            LENGTHS[i] = depth * SEGMENT_SCALE[i] / 16.0;
            RADIUS[i] = height * SEGMENT_SCALE[i] / 32.0;
        }
        LENGTHS[SEGMENTS] = TAIL_DEPTH / 16.0;
        RADIUS[SEGMENTS] = 0.5;
        RADIUS[SEGMENTS + 1] = 0.35;
    }

    private final Vec3[] pos = new Vec3[JOINTS];
    private final Vec3[] posO = new Vec3[JOINTS];
    private boolean initialised;

    public void tick(LeviathanEntity entity) {
        Level level = entity.level();
        Vec3 neck = entity.neckPos(1.0F);
        if (!this.initialised || this.pos[0].distanceToSqr(neck) > 64.0) {
            this.reset(entity, neck);
        }
        System.arraycopy(this.pos, 0, this.posO, 0, JOINTS);
        this.pos[0] = neck;
        Vec3 back = entity.forward(1.0F).scale(-1.0);
        boolean dying = entity.isDeadOrDying();
        for (int i = 1; i < JOINTS; i++) {
            Vec3 p = this.pos[i];
            double r = RADIUS[i];
            if (!isWater(level, p)) {
                if (isSolid(level, p.x, p.y - r + 0.25, p.z)) {
                    p = p.add(0.0, 0.2, 0.0);
                } else if (!isSolid(level, p.x, p.y - r - 0.05, p.z)) {
                    p = p.add(0.0, -0.32, 0.0);
                }
            } else if (dying) {
                p = p.add(0.0, -0.03, 0.0);
            }
            Vec3 d = p.subtract(this.pos[i - 1]);
            double len = d.length();
            d = len < 1.0E-4 ? back : d.scale(1.0 / len);
            this.pos[i] = this.pos[i - 1].add(d.scale(LENGTHS[i - 1]));
        }
    }

    private void reset(LeviathanEntity entity, Vec3 neck) {
        Level level = entity.level();
        Vec3 fwd = entity.forward(1.0F);
        Vec3 flatBack = new Vec3(-fwd.x, 0.0, -fwd.z);
        if (flatBack.lengthSqr() < 1.0E-4) {
            flatBack = new Vec3(0.0, 0.0, 1.0);
        }
        flatBack = flatBack.normalize();
        Vec3 dir = isWater(level, neck.add(0.0, -1.5, 0.0)) ? new Vec3(0.0, -1.0, 0.0) : flatBack;
        this.pos[0] = neck;
        float turn = 0.0F;
        for (int i = 1; i < JOINTS; i++) {
            Vec3 next = this.pos[i - 1].add(dir.scale(LENGTHS[i - 1]));
            int tries = 0;
            while (isSolid(level, next.x, next.y, next.z) && tries++ < 6) {
                turn += 50.0F;
                dir = flatBack.yRot(turn * Mth.DEG_TO_RAD);
                next = this.pos[i - 1].add(dir.scale(LENGTHS[i - 1]));
            }
            this.pos[i] = next;
        }
        System.arraycopy(this.pos, 0, this.posO, 0, JOINTS);
        this.initialised = true;
    }

    public boolean isInitialised() {
        return this.initialised;
    }

    /** Interpolated joint position (joint 0 is the neck, recomputed by the renderer from the head). */
    public Vec3 joint(int i, float partialTicks) {
        return this.posO[i].lerp(this.pos[i], partialTicks);
    }

    private static boolean isWater(Level level, Vec3 p) {
        return level.getFluidState(BlockPos.containing(p)).is(FluidTags.WATER);
    }

    private static boolean isSolid(Level level, double x, double y, double z) {
        BlockPos bp = BlockPos.containing(x, y, z);
        return !level.getBlockState(bp).getCollisionShape(level, bp).isEmpty();
    }
}
