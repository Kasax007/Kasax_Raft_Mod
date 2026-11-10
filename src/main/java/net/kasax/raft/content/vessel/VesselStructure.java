package net.kasax.raft.content.vessel;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.kasax.raft.content.vessel.block.OarBlock;
import net.kasax.raft.content.vessel.block.RaftMotorBlock;
import net.kasax.raft.content.vessel.block.SailBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.kasax.raft.compat.ValueInput;
import net.kasax.raft.compat.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * The blocks of a sailing raft in its own frame, which is its shipyard plot: x/z = 0..{@link VesselShipyard#SIZE_X},
 * y = 0 at the lowest hull layer, the helm faces south (+z, the bow). The real blocks (with their block entities) are in
 * the plot; this is the copy the raft collides with and draws, plus what is derived from it: collision boxes, the outer
 * cells that can touch land, keel samples for the water check and the propulsion counts. The pivot (the vessel
 * entity's position) stays where it was when the raft set sail, whatever is built or broken later.
 */
public final class VesselStructure {
    /** Limits when setting sail (built on the grid). */
    public static final int MAX_BLOCKS = 1024;
    public static final int MAX_SPAN = 32;
    public static final int MAX_HEIGHT = 24;
    /** A raft at sea may grow up to this many blocks (and to the edges of its plot). */
    public static final int MAX_BLOCKS_AT_SEA = 2048;
    private static final AABB FULL_CELL = new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);

    private final BlockPos helm;
    private final Vec3 pivot;
    private final Long2ObjectOpenHashMap<BlockState> blocks;

    // derived
    private final Long2ObjectOpenHashMap<AABB[]> boxes = new Long2ObjectOpenHashMap<>();
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;
    private final long[] exterior;
    private final long[] keel;
    private final long[] motors;
    private final int sails;
    private final int oars;
    /** An anchor winch on board has its anchor down: the raft holds its position. */
    private final boolean anchorDown;
    private final double radius;
    /** Columns (x, z) inside the hull's outline (hull columns and the holes they enclose), computed when first needed. */
    private @Nullable boolean[] outline;

    public VesselStructure(BlockPos helm, Vec3 pivot, Long2ObjectOpenHashMap<BlockState> blocks) {
        this.helm = helm.immutable();
        this.pivot = pivot;
        this.blocks = blocks;

        LongArrayList ext = new LongArrayList();
        LongArrayList motorList = new LongArrayList();
        int sailCount = 0;
        int oarCount = 0;
        boolean anchorDown = false;
        int x0 = Integer.MAX_VALUE;
        int y0 = Integer.MAX_VALUE;
        int z0 = Integer.MAX_VALUE;
        int x1 = Integer.MIN_VALUE;
        int y1 = Integer.MIN_VALUE;
        int z1 = Integer.MIN_VALUE;
        for (Long2ObjectMap.Entry<BlockState> e : this.blocks.long2ObjectEntrySet()) {
            long key = e.getLongKey();
            BlockState state = e.getValue();
            int x = BlockPos.getX(key);
            int y = BlockPos.getY(key);
            int z = BlockPos.getZ(key);
            x0 = Math.min(x0, x);
            y0 = Math.min(y0, y);
            z0 = Math.min(z0, z);
            x1 = Math.max(x1, x + 1);
            y1 = Math.max(y1, y + 1);
            z1 = Math.max(z1, z + 1);
            VoxelShape shape = state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty());
            if (!shape.isEmpty()) {
                List<AABB> list = shape.toAabbs();
                AABB[] arr = new AABB[list.size()];
                for (int i = 0; i < arr.length; i++) {
                    arr[i] = list.get(i).move(x, y, z);
                }
                this.boxes.put(key, arr);
            }
            if (!this.blocks.containsKey(BlockPos.asLong(x + 1, y, z)) || !this.blocks.containsKey(BlockPos.asLong(x - 1, y, z))
                    || !this.blocks.containsKey(BlockPos.asLong(x, y, z + 1)) || !this.blocks.containsKey(BlockPos.asLong(x, y, z - 1))) {
                ext.add(key);
            }
            if (state.getBlock() instanceof SailBlock) {
                if (!state.getValue(SailBlock.FURLED)) {
                    sailCount++;          // a furled sail catches no wind
                }
            } else if (state.getBlock() instanceof net.kasax.raft.content.vessel.block.AnchorWinchBlock
                    && state.getValue(net.kasax.raft.content.vessel.block.AnchorWinchBlock.DROPPED)) {
                anchorDown = true;
            } else if (state.getBlock() instanceof OarBlock) {
                oarCount++;
            } else if (state.getBlock() instanceof RaftMotorBlock) {
                motorList.add(key);
            }
        }
        if (this.blocks.isEmpty()) {
            x0 = y0 = z0 = 0;
            x1 = y1 = z1 = 1;
        }
        this.minX = x0;
        this.minY = y0;
        this.minZ = z0;
        this.maxX = x1;
        this.maxY = y1;
        this.maxZ = z1;
        this.exterior = ext.toLongArray();
        // at most 24 evenly spread samples of the lowest layer are enough to tell water from land
        LongArrayList bottom = new LongArrayList();
        for (long key : this.blocks.keySet()) {
            if (BlockPos.getY(key) == this.minY) {
                bottom.add(key);
            }
        }
        bottom.sort(Long::compare);
        long[] all = bottom.toLongArray();
        int n = Math.min(all.length, 24);
        this.keel = new long[n];
        for (int i = 0; i < n; i++) {
            this.keel[i] = all[(int) ((long) i * all.length / n)];
        }
        this.motors = motorList.toLongArray();
        this.sails = sailCount;
        this.oars = oarCount;
        this.anchorDown = anchorDown;
        double r2 = 0.0;
        for (int cx : new int[]{x0, x1}) {
            for (int cz : new int[]{z0, z1}) {
                double dx = cx - pivot.x;
                double dz = cz - pivot.z;
                r2 = Math.max(r2, dx * dx + dz * dz);
            }
        }
        this.radius = Math.sqrt(r2);
    }

    /**
     * Is the column (x, z) inside the hull's outline (a column with a hull block, or a hole the hull encloses)? Blocks
     * that belong to the raft stand there (see VesselAssembler.scan).
     */
    public boolean insideOutline(int x, int z) {
        if (x < this.minX || z < this.minZ || x >= this.maxX || z >= this.maxZ) {
            return false;
        }
        int w = this.maxX - this.minX;
        int d = this.maxZ - this.minZ;
        boolean[] grid = this.outline;
        if (grid == null) {
            boolean[] hull = new boolean[w * d];
            for (Long2ObjectMap.Entry<BlockState> e : this.blocks.long2ObjectEntrySet()) {
                if (e.getValue().is(VesselContent.HULLS)) {
                    hull[(BlockPos.getX(e.getLongKey()) - this.minX) * d + BlockPos.getZ(e.getLongKey()) - this.minZ] = true;
                }
            }
            boolean[] outside = new boolean[w * d];
            java.util.ArrayDeque<int[]> open = new java.util.ArrayDeque<>();
            for (int cx = 0; cx < w; cx++) {
                for (int cz = 0; cz < d; cz++) {
                    if ((cx == 0 || cz == 0 || cx == w - 1 || cz == d - 1) && !hull[cx * d + cz]) {
                        outside[cx * d + cz] = true;
                        open.add(new int[]{cx, cz});
                    }
                }
            }
            int[][] steps = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            while (!open.isEmpty()) {
                int[] c = open.poll();
                for (int[] st : steps) {
                    int nx = c[0] + st[0];
                    int nz = c[1] + st[1];
                    if (nx >= 0 && nz >= 0 && nx < w && nz < d && !hull[nx * d + nz] && !outside[nx * d + nz]) {
                        outside[nx * d + nz] = true;
                        open.add(new int[]{nx, nz});
                    }
                }
            }
            grid = new boolean[w * d];
            for (int i = 0; i < grid.length; i++) {
                grid[i] = !outside[i];
            }
            this.outline = grid;
        }
        return grid[(x - this.minX) * d + z - this.minZ];
    }

    /** A copy with some blocks changed (air or null removes the block). */
    public VesselStructure withChanges(Long2ObjectMap<BlockState> changes) {
        Long2ObjectOpenHashMap<BlockState> copy = new Long2ObjectOpenHashMap<>(this.blocks);
        for (Long2ObjectMap.Entry<BlockState> e : changes.long2ObjectEntrySet()) {
            BlockState state = e.getValue();
            if (state == null || state.isAir()) {
                copy.remove(e.getLongKey());
            } else {
                copy.put(e.getLongKey(), state);
            }
        }
        return new VesselStructure(this.helm, this.pivot, copy);
    }

    // ------------------------------------------------------------------------------------------- access
    public BlockPos helm() {
        return this.helm;
    }

    public Vec3 pivot() {
        return this.pivot;
    }

    public double radius() {
        return this.radius;
    }

    public int blockCount() {
        return this.blocks.size();
    }

    public boolean isEmpty() {
        return this.blocks.isEmpty();
    }

    public int sails() {
        return this.sails;
    }

    public int oars() {
        return this.oars;
    }

    public boolean anchorDown() {
        return this.anchorDown;
    }

    public long[] motors() {
        return this.motors;
    }

    public long[] keel() {
        return this.keel;
    }

    public int minX() {
        return this.minX;
    }

    public int maxX() {
        return this.maxX;
    }

    public int minZ() {
        return this.minZ;
    }

    public int maxZ() {
        return this.maxZ;
    }

    public @Nullable BlockState get(BlockPos pos) {
        return this.blocks.get(pos.asLong());
    }

    public @Nullable BlockState get(long key) {
        return this.blocks.get(key);
    }

    public Long2ObjectOpenHashMap<BlockState> blocks() {
        return this.blocks;
    }

    /** The box around all blocks, in the raft's frame. */
    public AABB localBounds() {
        return new AABB(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);
    }

    /** The world box around the whole raft at a pose. */
    public AABB worldBounds(VesselPose pose) {
        return pose.toWorld(this.localBounds());
    }

    // ------------------------------------------------------------------------------------------- collision
    /**
     * Calls {@code out} with the world boxes of all collision boxes that touch {@code query} (until it returns true).
     * Returns true if {@code out} stopped the search.
     */
    public boolean forEachBox(VesselPose pose, AABB query, Predicate<AABB> out) {
        AABB local = pose.toLocal(query);
        // one cell more around: some shapes reach into the next cell (fences and walls are 1.5 high)
        int x0 = Math.max(this.minX, (int) Math.floor(local.minX) - 1);
        int x1 = Math.min(this.maxX - 1, (int) Math.floor(local.maxX) + 1);
        int y0 = Math.max(this.minY, (int) Math.floor(local.minY) - 1);
        int y1 = Math.min(this.maxY - 1, (int) Math.floor(local.maxY));
        int z0 = Math.max(this.minZ, (int) Math.floor(local.minZ) - 1);
        int z1 = Math.min(this.maxZ - 1, (int) Math.floor(local.maxZ) + 1);
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    AABB[] arr = this.boxes.get(BlockPos.asLong(x, y, z));
                    if (arr == null) {
                        continue;
                    }
                    for (AABB box : arr) {
                        AABB world = pose.toWorld(box);
                        if (world.intersects(query) && out.test(world)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public void collectBoxes(VesselPose pose, AABB query, Consumer<AABB> out) {
        this.forEachBox(pose, query, box -> {
            out.accept(box);
            return false;
        });
    }

    /**
     * True if something of the raft is under the box: a collision box whose top lies at most {@code below} blocks
     * under the box's bottom (and not above it), i.e. the entity stands on the raft or is in the air over its deck.
     */
    public boolean isOver(VesselPose pose, AABB box, double below) {
        double feet = box.minY;
        AABB region = new AABB(box.minX, feet - below, box.minZ, box.maxX, feet + 0.05, box.maxZ);
        return this.forEachBox(pose, region, b -> b.maxY <= feet + 0.05);
    }

    /** Highest top of a collision box that the box sinks into by at most {@code depth}, or NaN. */
    public double sunkTop(VesselPose pose, AABB box, double depth) {
        double feet = box.minY;
        double[] best = {Double.NaN};
        AABB region = new AABB(box.minX + 1.0E-3, feet, box.minZ + 1.0E-3, box.maxX - 1.0E-3, feet + depth, box.maxZ - 1.0E-3);
        this.forEachBox(pose, region, b -> {
            if (b.maxY > feet + 1.0E-4 && b.maxY <= feet + depth && b.minY < feet + 1.0E-4 && (Double.isNaN(best[0]) || b.maxY > best[0])) {
                best[0] = b.maxY;
            }
            return false;
        });
        return best[0];
    }

    /** The world boxes of the outer cells (whole cells), used to keep the raft out of land. */
    public void forEachExteriorCell(VesselPose pose, Predicate<AABB> out) {
        for (long key : this.exterior) {
            AABB cell = FULL_CELL.move(BlockPos.getX(key), BlockPos.getY(key), BlockPos.getZ(key)).deflate(0.02);
            if (out.test(pose.toWorld(cell))) {
                return;
            }
        }
    }

    /** Ray-cast in local coordinates against the block outline shapes (exact, as the blocks are drawn). */
    public @Nullable BlockHitResult clip(Vec3 from, Vec3 to) {
        return BlockGetter.traverseBlocks(from, to, this, (self, pos) -> {
            BlockState state = self.blocks.get(pos.asLong());
            if (state == null) {
                return null;
            }
            VoxelShape shape = state.getShape(EmptyBlockGetter.INSTANCE, pos);
            return shape.isEmpty() ? null : shape.clip(from, to, pos);
        }, self -> null);
    }

    /**
     * Like {@link #clip}, against the blocks' collision shapes (local coordinates): what stops a player, an item or the
     * Plastic Hook's line of sight.
     */
    public @Nullable BlockHitResult clipCollision(Vec3 from, Vec3 to) {
        return BlockGetter.traverseBlocks(from, to, this, (self, pos) -> {
            BlockState state = self.blocks.get(pos.asLong());
            if (state == null) {
                return null;
            }
            VoxelShape shape = state.getCollisionShape(EmptyBlockGetter.INSTANCE, pos);
            return shape.isEmpty() ? null : shape.clip(from, to, pos);
        }, self -> null);
    }

    // ------------------------------------------------------------------------------------------- save / load
    private static int pack(int x, int y, int z) {
        return x | y << 8 | z << 16;
    }

    public static long unpack(int packed) {
        return BlockPos.asLong(packed & 0xFF, packed >> 8 & 0xFF, packed >> 16 & 0xFF);
    }

    public static int pack(long key) {
        return pack(BlockPos.getX(key), BlockPos.getY(key), BlockPos.getZ(key));
    }

    /** The blocks are saved with the raft too, so that it can be stood on and drawn before its plot is loaded again. */
    public void save(ValueOutput out) {
        out.putIntArray("Helm", new int[]{this.helm.getX(), this.helm.getY(), this.helm.getZ()});
        out.putDouble("PivotX", this.pivot.x);
        out.putDouble("PivotY", this.pivot.y);
        out.putDouble("PivotZ", this.pivot.z);
        Object2IntOpenHashMap<BlockState> index = new Object2IntOpenHashMap<>();
        List<CompoundTag> palette = new ArrayList<>();
        int[] data = new int[this.blocks.size() * 2];
        int i = 0;
        for (Long2ObjectMap.Entry<BlockState> e : this.blocks.long2ObjectEntrySet()) {
            BlockState state = e.getValue();
            int id = index.computeIfAbsent(state, s -> {
                palette.add(NbtUtils.writeBlockState((BlockState) s));
                return palette.size() - 1;
            });
            data[i++] = pack(e.getLongKey());
            data[i++] = id;
        }
        out.store("Palette", CompoundTag.CODEC.listOf(), palette);
        out.putIntArray("Blocks", data);
    }

    public static @Nullable VesselStructure load(ValueInput in) {
        int[] helm = in.getIntArray("Helm").orElse(null);
        int[] data = in.getIntArray("Blocks").orElse(null);
        List<CompoundTag> palette = in.read("Palette", CompoundTag.CODEC.listOf()).orElse(List.of());
        if (helm == null || helm.length != 3 || data == null) {
            return null;
        }
        HolderGetter<Block> lookup = in.lookup().lookupOrThrow(Registries.BLOCK);
        BlockState[] states = new BlockState[palette.size()];
        for (int i = 0; i < states.length; i++) {
            states[i] = NbtUtils.readBlockState(lookup, palette.get(i));
        }
        Long2ObjectOpenHashMap<BlockState> blocks = new Long2ObjectOpenHashMap<>();
        for (int i = 0; i + 1 < data.length; i += 2) {
            int id = data[i + 1];
            if (id >= 0 && id < states.length && !states[id].isAir()) {
                blocks.put(unpack(data[i]), states[id]);
            }
        }
        Vec3 pivot = new Vec3(in.getDoubleOr("PivotX", 0.0), in.getDoubleOr("PivotY", 0.0), in.getDoubleOr("PivotZ", 0.0));
        return new VesselStructure(new BlockPos(helm[0], helm[1], helm[2]), pivot, blocks);
    }

    // ------------------------------------------------------------------------------------------- network
    /** Blocks only (clients need no block entity data to draw and collide). */
    public static final StreamCodec<RegistryFriendlyByteBuf, VesselStructure> CLIENT_CODEC = StreamCodec.of(
            (buf, s) -> {
                buf.writeBlockPos(s.helm);
                buf.writeDouble(s.pivot.x);
                buf.writeDouble(s.pivot.y);
                buf.writeDouble(s.pivot.z);
                buf.writeVarInt(s.blocks.size());
                for (Long2ObjectMap.Entry<BlockState> e : s.blocks.long2ObjectEntrySet()) {
                    buf.writeVarInt(pack(e.getLongKey()));
                    buf.writeVarInt(Block.getId(e.getValue()));
                }
            },
            buf -> {
                BlockPos helm = buf.readBlockPos();
                Vec3 pivot = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
                int n = buf.readVarInt();
                if (n < 0 || n > 65536) {
                    throw new IllegalArgumentException("Bad vessel block count " + n);
                }
                Long2ObjectOpenHashMap<BlockState> blocks = new Long2ObjectOpenHashMap<>(n);
                for (int i = 0; i < n; i++) {
                    long key = unpack(buf.readVarInt());
                    BlockState state = Block.stateById(buf.readVarInt());
                    if (!state.isAir()) {
                        blocks.put(key, state);
                    }
                }
                return new VesselStructure(helm, pivot, blocks);
            });
}
