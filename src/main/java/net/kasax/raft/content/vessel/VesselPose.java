package net.kasax.raft.content.vessel;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Where a vessel is: the world position of its pivot and its yaw (Minecraft convention: 0 = the bow points south,
 * 90 = west). Converts between the vessel's local block frame (helm facing south, blocks at 0..size) and the world.
 * <pre>
 * world = pos + Ry(-yaw) * (local - pivot)        local = Ry(yaw) * (world - pos) + pivot
 * </pre>
 */
public final class VesselPose {
    public final double x;
    public final double y;
    public final double z;
    public final float yaw;
    private final Vec3 pivot;
    private final double cos;
    private final double sin;

    public VesselPose(double x, double y, double z, float yaw, Vec3 pivot) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pivot = pivot;
        // exact values for quarter turns, so docked blocks land exactly on the grid
        if (yaw % 90.0F == 0.0F) {
            int quarter = Math.floorMod(Math.round(yaw / 90.0F), 4);
            this.cos = quarter == 0 ? 1.0 : quarter == 2 ? -1.0 : 0.0;
            this.sin = quarter == 1 ? 1.0 : quarter == 3 ? -1.0 : 0.0;
        } else {
            double rad = Math.toRadians(yaw);
            this.cos = Math.cos(rad);
            this.sin = Math.sin(rad);
        }
    }

    public VesselPose(Vec3 pos, float yaw, Vec3 pivot) {
        this(pos.x, pos.y, pos.z, yaw, pivot);
    }

    public Vec3 position() {
        return new Vec3(this.x, this.y, this.z);
    }

    public VesselPose moved(double dx, double dy, double dz) {
        return new VesselPose(this.x + dx, this.y + dy, this.z + dz, this.yaw, this.pivot);
    }

    public VesselPose withYaw(float yaw) {
        return new VesselPose(this.x, this.y, this.z, yaw, this.pivot);
    }

    // ------------------------------------------------------------------------------------------- local -> world
    public double worldX(double lx, double lz) {
        return this.x + (lx - this.pivot.x) * this.cos - (lz - this.pivot.z) * this.sin;
    }

    public double worldY(double ly) {
        return this.y + ly - this.pivot.y;
    }

    public double worldZ(double lx, double lz) {
        return this.z + (lz - this.pivot.z) * this.cos + (lx - this.pivot.x) * this.sin;
    }

    public Vec3 toWorld(double lx, double ly, double lz) {
        return new Vec3(this.worldX(lx, lz), this.worldY(ly), this.worldZ(lx, lz));
    }

    public Vec3 toWorld(Vec3 local) {
        return this.toWorld(local.x, local.y, local.z);
    }

    /** The world block that the centre of a local block falls into. Exact when the pose is aligned to the grid. */
    public BlockPos worldBlock(int lx, int ly, int lz) {
        return BlockPos.containing(this.worldX(lx + 0.5, lz + 0.5), this.worldY(ly + 0.5), this.worldZ(lx + 0.5, lz + 0.5));
    }

    /** Axis-aligned world box around a local box (exact for multiples of 90 degrees, a little larger in between). */
    public AABB toWorld(AABB local) {
        double cx = (local.minX + local.maxX) * 0.5;
        double cz = (local.minZ + local.maxZ) * 0.5;
        double hx = (local.maxX - local.minX) * 0.5;
        double hz = (local.maxZ - local.minZ) * 0.5;
        double ac = Math.abs(this.cos);
        double as = Math.abs(this.sin);
        double ex = ac * hx + as * hz;
        double ez = as * hx + ac * hz;
        double wx = this.worldX(cx, cz);
        double wz = this.worldZ(cx, cz);
        double dy = this.y - this.pivot.y;
        return new AABB(wx - ex, local.minY + dy, wz - ez, wx + ex, local.maxY + dy, wz + ez);
    }

    // ------------------------------------------------------------------------------------------- world -> local
    public double localX(double wx, double wz) {
        double dx = wx - this.x;
        double dz = wz - this.z;
        return this.pivot.x + dx * this.cos + dz * this.sin;
    }

    public double localY(double wy) {
        return wy - this.y + this.pivot.y;
    }

    public double localZ(double wx, double wz) {
        double dx = wx - this.x;
        double dz = wz - this.z;
        return this.pivot.z + dz * this.cos - dx * this.sin;
    }

    public Vec3 toLocal(Vec3 world) {
        return new Vec3(this.localX(world.x, world.z), this.localY(world.y), this.localZ(world.x, world.z));
    }

    /** Axis-aligned local box around a world box (rotation-conservative). */
    public AABB toLocal(AABB world) {
        double cx = (world.minX + world.maxX) * 0.5;
        double cz = (world.minZ + world.maxZ) * 0.5;
        double hx = (world.maxX - world.minX) * 0.5;
        double hz = (world.maxZ - world.minZ) * 0.5;
        double ac = Math.abs(this.cos);
        double as = Math.abs(this.sin);
        double ex = ac * hx + as * hz;
        double ez = as * hx + ac * hz;
        double lx = this.localX(cx, cz);
        double lz = this.localZ(cx, cz);
        return new AABB(lx - ex, this.localY(world.minY), lz - ez, lx + ex, this.localY(world.maxY), lz + ez);
    }

    @Override
    public String toString() {
        return String.format(java.util.Locale.ROOT, "(%.2f %.2f %.2f yaw %.1f)", this.x, this.y, this.z, this.yaw);
    }
}
