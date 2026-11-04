package net.kasax.raft.content.vessel;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The sailing rafts of each side (server, client), so that entity collisions can find them cheaply: when no raft is
 * at sea the collision hook costs one isEmpty() check. Rafts add themselves while they tick and leave on removal.
 */
public final class VesselColliders {
    private static final List<VesselEntity> SERVER = new CopyOnWriteArrayList<>();
    private static final List<VesselEntity> CLIENT = new CopyOnWriteArrayList<>();

    private VesselColliders() {
    }

    private static List<VesselEntity> side(boolean client) {
        return client ? CLIENT : SERVER;
    }

    static void add(VesselEntity vessel) {
        boolean client = vessel.level().isClientSide();
        List<VesselEntity> list = side(client);
        if (!list.contains(vessel)) {
            if (client) {
                // a client has one level at a time: forget rafts of the level it left
                list.removeIf(v -> v.level() != vessel.level());
            }
            list.add(vessel);
        }
    }

    static void remove(VesselEntity vessel) {
        side(vessel.level().isClientSide()).remove(vessel);
    }

    /** Forget all client rafts (leaving a world) or all server rafts (server stopped). */
    public static void clear(boolean client) {
        side(client).clear();
    }

    /** The rafts in a level (removed or foreign ones are dropped on the way). */
    public static List<VesselEntity> in(Level level) {
        List<VesselEntity> list = side(level.isClientSide());
        if (list.isEmpty()) {
            return List.of();
        }
        List<VesselEntity> out = new ArrayList<>(2);
        for (VesselEntity v : list) {
            if (v.isRemoved()) {
                list.remove(v);
            } else if (v.level() == level) {
                out.add(v);
            }
        }
        return out;
    }

    /**
     * The block boxes of all rafts that touch {@code area}, as seen by {@code source}; null if there are none. Players
     * collide with rafts only on their own client: the server's raft is a few ticks ahead of the one the client sees,
     * so testing players against it would make the server reject their moves (see the Expedition Raft). Passengers of
     * a raft (the helmsman) and rafts themselves do not collide with rafts.
     */
    public static @Nullable List<VoxelShape> collisions(Level level, @Nullable Entity source, AABB area) {
        if (source == null || source instanceof VesselEntity) {
            return null;
        }
        List<VesselEntity> list = side(level.isClientSide());
        if (list.isEmpty()) {
            return null;
        }
        if (source instanceof Player && !level.isClientSide()) {
            return null;
        }
        if (source.isPassenger() && source.getRootVehicle() instanceof VesselEntity) {
            return null;
        }
        List<VoxelShape> out = null;
        for (VesselEntity vessel : list) {
            if (vessel.level() != level || vessel.isRemoved() || !vessel.getBoundingBox().intersects(area)) {
                continue;
            }
            VesselStructure structure = vessel.structure();
            if (structure == null) {
                continue;
            }
            if (out == null) {
                out = new ArrayList<>();
            }
            List<VoxelShape> target = out;
            structure.collectBoxes(vessel.currentPose(), area, box -> target.add(Shapes.create(box)));
        }
        return out == null || out.isEmpty() ? null : out;
    }

    /** True if the box is on or just above a raft (lenient: the server's raft runs a little ahead of the client's). */
    public static boolean isAboard(Entity entity) {
        List<VesselEntity> list = side(entity.level().isClientSide());
        if (list.isEmpty()) {
            return false;
        }
        AABB box = entity.getBoundingBox();
        for (VesselEntity vessel : list) {
            if (vessel.level() == entity.level() && !vessel.isRemoved() && vessel.getBoundingBox().inflate(2.5, 4.0, 2.5).intersects(box)) {
                return true;
            }
        }
        return false;
    }
}
