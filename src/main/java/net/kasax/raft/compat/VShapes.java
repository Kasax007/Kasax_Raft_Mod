package net.kasax.raft.compat;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/** 1.21.1 port helper: the shape helpers the newer game has on {@code Block} and {@code Shapes}. */
public final class VShapes {
    private VShapes() {
    }

    public static VoxelShape cube(double size) {
        return cube(size, size, size);
    }

    public static VoxelShape cube(double sizeX, double sizeY, double sizeZ) {
        double halfY = sizeY / 2.0;
        return column(sizeX, sizeZ, 8.0 - halfY, 8.0 + halfY);
    }

    public static VoxelShape column(double sizeXZ, double minY, double maxY) {
        return column(sizeXZ, sizeXZ, minY, maxY);
    }

    public static VoxelShape column(double sizeX, double sizeZ, double minY, double maxY) {
        double halfX = sizeX / 2.0;
        double halfZ = sizeZ / 2.0;
        return Block.box(8.0 - halfX, minY, 8.0 - halfZ, 8.0 + halfX, maxY, 8.0 + halfZ);
    }

    public static VoxelShape boxZ(double sizeXY, double minZ, double maxZ) {
        return boxZ(sizeXY, sizeXY, minZ, maxZ);
    }

    public static VoxelShape boxZ(double sizeX, double sizeY, double minZ, double maxZ) {
        double halfY = sizeY / 2.0;
        return boxZ(sizeX, 8.0 - halfY, 8.0 + halfY, minZ, maxZ);
    }

    public static VoxelShape boxZ(double sizeX, double minY, double maxY, double minZ, double maxZ) {
        double halfX = sizeX / 2.0;
        return Block.box(8.0 - halfX, minY, minZ, 8.0 + halfX, maxY, maxZ);
    }

    /** Turns a shape a quarter turn clockwise seen from above (north becomes east), {@code times} times. */
    public static VoxelShape rotateY(VoxelShape shape, int times) {
        times = Math.floorMod(times, 4);
        if (times == 0) return shape;
        VoxelShape out = Shapes.empty();
        for (AABB box : shape.toAabbs()) {
            double x0 = box.minX, x1 = box.maxX, z0 = box.minZ, z1 = box.maxZ;
            for (int i = 0; i < times; i++) {
                double nx0 = 1.0 - z1, nx1 = 1.0 - z0, nz0 = x0, nz1 = x1;
                x0 = nx0; x1 = nx1; z0 = nz0; z1 = nz1;
            }
            out = Shapes.or(out, Shapes.box(x0, box.minY, z0, x1, box.maxY, z1));
        }
        return out.optimize();
    }

    public static Map<Direction, VoxelShape> rotateHorizontal(VoxelShape north) {
        Map<Direction, VoxelShape> map = new EnumMap<>(Direction.class);
        map.put(Direction.NORTH, north);
        map.put(Direction.EAST, rotateY(north, 1));
        map.put(Direction.SOUTH, rotateY(north, 2));
        map.put(Direction.WEST, rotateY(north, 3));
        return map;
    }

    public static Map<Direction.Axis, VoxelShape> rotateHorizontalAxis(VoxelShape zAxis) {
        Map<Direction.Axis, VoxelShape> map = new EnumMap<>(Direction.Axis.class);
        map.put(Direction.Axis.Z, zAxis);
        map.put(Direction.Axis.X, rotateY(zAxis, 1));
        return map;
    }
}
