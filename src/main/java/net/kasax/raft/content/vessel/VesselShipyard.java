package net.kasax.raft.content.vessel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import net.kasax.raft.Raft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The shipyard: where the blocks of a sailing raft are while it is at sea. Each raft gets a plot, a box of
 * {@value #SIZE_X} x {@value #SIZE_Y} x {@value #SIZE_Z} blocks far out in the same dimension (x and z from
 * {@value #BASE} on, high above the ground). Its blocks stay real blocks there and work as usual (crops grow, furnaces
 * smelt, redstone runs) while the plot's chunks are kept loaded, which they are as long as the raft entity is loaded.
 * The raft draws them at its own position and turns players' clicks into actions on them (see VesselInteractions).
 *
 * <p>Per level: which plots are taken (saved) and which raft sails with which plot right now (not saved).
 */
public final class VesselShipyard extends SavedData {
    public static final int SIZE_X = 48;
    public static final int SIZE_Y = 32;
    public static final int SIZE_Z = 48;
    /** A raft's blocks start this far from the plot's sides, so that it can grow a little while it sails. */
    public static final int MARGIN = 8;
    /** Distance between plots: three chunks of plot and an empty chunk. */
    private static final int STRIDE = 64;
    private static final int ROW = 512;
    /** First block of the shipyard on x and z (chunk aligned, well inside the default world border). */
    public static final int BASE = 28_000_000;
    /** At most this many rafts at sea per dimension. */
    public static final int MAX_PLOTS = 4096;

    /** Keeps a plot's chunks loaded and ticking (block entities, random and scheduled ticks) while its raft is loaded. */
    public static final TicketType TICKET = Registry.register(BuiltInRegistries.TICKET_TYPE, Raft.id("vessel_plot"),
            new TicketType(0L, TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION));

    private static final Codec<VesselShipyard> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.listOf().optionalFieldOf("plots", List.of()).forGetter(s -> new ArrayList<>(s.used))
    ).apply(i, VesselShipyard::new));
    private static final SavedDataType<VesselShipyard> TYPE = new SavedDataType<>(Raft.id("vessel_shipyard"), VesselShipyard::new, CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final IntSortedSet used = new IntAVLTreeSet();
    /** Not saved: the raft that sails with each plot now, and how many holders keep each plot loaded. */
    private final Int2ObjectOpenHashMap<VesselEntity> active = new Int2ObjectOpenHashMap<>();
    private final Int2IntOpenHashMap holds = new Int2IntOpenHashMap();

    public VesselShipyard() {
    }

    /** Mod init: registers the plot ticket type (class initialisation). */
    public static void init() {
    }

    private VesselShipyard(List<Integer> plots) {
        this.used.addAll(plots);
    }

    public static VesselShipyard get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    // ------------------------------------------------------------------------------------------- allocation
    /** A free plot (now taken), or -1 if the shipyard is full. */
    int allocate() {
        for (int i = 0; i < MAX_PLOTS; i++) {
            if (!this.used.contains(i)) {
                this.used.add(i);
                this.setDirty();
                return i;
            }
        }
        return -1;
    }

    void free(int plot) {
        if (this.used.remove(plot)) {
            this.setDirty();
        }
        this.active.remove(plot);
    }

    void attach(int plot, VesselEntity vessel) {
        this.active.put(plot, vessel);
    }

    void detach(int plot, VesselEntity vessel) {
        if (this.active.get(plot) == vessel) {
            this.active.remove(plot);
        }
    }

    @Nullable VesselEntity vessel(int plot) {
        VesselEntity vessel = this.active.get(plot);
        return vessel != null && !vessel.isRemoved() ? vessel : null;
    }

    // ------------------------------------------------------------------------------------------- geometry
    public static int originX(int plot) {
        return BASE + plot % ROW * STRIDE;
    }

    public static int originZ(int plot) {
        return BASE + plot / ROW * STRIDE;
    }

    public static int originY(Level level) {
        return level.getMaxY() - SIZE_Y - 7;
    }

    public static BlockPos origin(Level level, int plot) {
        return new BlockPos(originX(plot), originY(level), originZ(plot));
    }

    /** The plot that holds this position, or -1. */
    public static int plotAt(Level level, int x, int y, int z) {
        if (x < BASE || z < BASE) {
            return -1;
        }
        int dx = x - BASE;
        int dz = z - BASE;
        if (dx % STRIDE >= SIZE_X || dz % STRIDE >= SIZE_Z) {
            return -1;
        }
        int ix = dx / STRIDE;
        int iz = dz / STRIDE;
        int oy = originY(level);
        if (ix >= ROW || y < oy || y >= oy + SIZE_Y) {
            return -1;
        }
        int plot = iz * ROW + ix;
        return plot < MAX_PLOTS ? plot : -1;
    }

    /** Quick test without the level (for hot paths): could this be a shipyard position at all? */
    public static boolean maybePlot(double x, double z) {
        return x >= BASE && z >= BASE;
    }

    /** The sailing raft whose plot holds this position (server), or null. */
    public static @Nullable VesselEntity vesselAt(Level level, BlockPos pos) {
        if (!maybePlot(pos.getX(), pos.getZ()) || !(level instanceof ServerLevel server)) {
            return null;
        }
        int plot = plotAt(level, pos.getX(), pos.getY(), pos.getZ());
        return plot < 0 ? null : get(server).vessel(plot);
    }

    /** Where a plot position is in the world now (at its raft), or null if it is not in the plot of a sailing raft. */
    public static @Nullable Vec3 toWorld(Level level, double x, double y, double z) {
        if (!maybePlot(x, z)) {
            return null;
        }
        VesselEntity vessel = vesselAt(level, BlockPos.containing(x, y, z));
        if (vessel == null || vessel.structure() == null) {
            return null;
        }
        BlockPos o = origin(level, vessel.plot());
        return vessel.currentPose().toWorld(x - o.getX(), y - o.getY(), z - o.getZ());
    }

    // ------------------------------------------------------------------------------------------- loading
    private static List<ChunkPos> chunks(Level level, int plot) {
        List<ChunkPos> list = new ArrayList<>(9);
        int x0 = originX(plot) >> 4;
        int z0 = originZ(plot) >> 4;
        for (int cx = x0; cx <= (originX(plot) + SIZE_X - 1) >> 4; cx++) {
            for (int cz = z0; cz <= (originZ(plot) + SIZE_Z - 1) >> 4; cz++) {
                list.add(new ChunkPos(cx, cz));
            }
        }
        return list;
    }

    /** Keeps the plot's chunks loaded and ticking (counted: every hold needs its release). */
    static void hold(ServerLevel level, int plot) {
        VesselShipyard yard = get(level);
        int n = yard.holds.addTo(plot, 1);
        if (n == 0) {
            for (ChunkPos pos : chunks(level, plot)) {
                // radius 2: ticket level 31, the chunk itself ticks entities, blocks and random ticks
                level.getChunkSource().addTicketWithRadius(TICKET, pos, 2);
            }
        }
    }

    static void release(ServerLevel level, int plot) {
        VesselShipyard yard = get(level);
        int n = yard.holds.get(plot);
        if (n <= 1) {
            yard.holds.remove(plot);
            if (n == 1) {
                for (ChunkPos pos : chunks(level, plot)) {
                    level.getChunkSource().removeTicketWithRadius(TICKET, pos, 2);
                }
            }
        } else {
            yard.holds.put(plot, n - 1);
        }
    }

    /** Are all chunks of the plot loaded (and ticking)? */
    static boolean loaded(ServerLevel level, int plot) {
        for (ChunkPos pos : chunks(level, plot)) {
            if (level.getChunkSource().getChunkNow(pos.x(), pos.z()) == null || !level.shouldTickBlocksAt(pos.pack())) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------------------------------- relocation
    /**
     * Server: something spawns an entity in a plot (drops of a block broken on the raft, an item a dispenser shoots,
     * a mob from a spawn egg): it appears where that spot is in the world, on the raft.
     */
    public static void relocate(ServerLevel level, Entity entity) {
        if (!maybePlot(entity.getX(), entity.getZ())) {
            return;
        }
        VesselEntity vessel = vesselAt(level, entity.blockPosition());
        if (vessel == null || vessel.structure() == null) {
            return;
        }
        BlockPos o = origin(level, vessel.plot());
        VesselPose pose = vessel.currentPose();
        Vec3 at = pose.toWorld(entity.getX() - o.getX(), entity.getY() - o.getY(), entity.getZ() - o.getZ());
        Vec3 motion = entity.getDeltaMovement();
        double rad = Math.toRadians(pose.yaw);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        entity.setDeltaMovement(motion.x * cos - motion.z * sin, motion.y, motion.z * cos + motion.x * sin);
        entity.snapTo(at.x, at.y, at.z, entity.getYRot() + pose.yaw, entity.getXRot());
    }

    /** Server: a block in a plot changed (sent to clients): its raft passes it on to the players who see it. */
    public static void blockChanged(ServerLevel level, BlockPos pos) {
        if (!maybePlot(pos.getX(), pos.getZ())) {
            return;
        }
        VesselEntity vessel = vesselAt(level, pos);
        if (vessel != null) {
            vessel.plotBlockChanged(pos);
        }
    }
}
