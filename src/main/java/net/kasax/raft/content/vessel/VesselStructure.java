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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * The blocks of a built raft in its own frame: x/z = 0..size, y = 0 at the lowest hull layer, the helm faces south
 * (+z, the bow). Holds the block states, the block entity data (server only) and what is derived from them:
 * collision boxes, the outer cells that can touch land, keel samples for the water check and the propulsion counts.
 * The pivot (the vessel entity's position) is the centre of the footprint at the bottom.
 */
public final class VesselStructure {
    public static final int MAX_BLOCKS = 1024;
    public static final int MAX_SPAN = 32;
    public static final int MAX_HEIGHT = 24;
    /** Upper bound of the distance between a block corner and the pivot. */
    public static final double MAX_RADIUS = Math.sqrt(2.0) * MAX_SPAN * 0.5 + 1.0;
    private static final AABB FULL_CELL = new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);

    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;
    private final BlockPos helm;
    private final Vec3 pivot;
    private final Long2ObjectOpenHashMap<BlockState> blocks;
    private final Long2ObjectOpenHashMap<CompoundTag> blockEntities;

    // derived
    private final Long2ObjectOpenHashMap<AABB[]> boxes = new Long2ObjectOpenHashMap<>();
    private final long[] exterior;
    private final long[] keel;
    private final long[] motors;
    private final int sails;
    private final int oars;
    private final double radius;

    public VesselStructure(int sizeX, int sizeY, int sizeZ, BlockPos helm, Long2ObjectOpenHashMap<BlockState> blocks,
                           Long2ObjectOpenHashMap<CompoundTag> blockEntities) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.helm = helm.immutable();
        this.blocks = blocks;
        this.blockEntities = blockEntities;
        this.pivot = new Vec3(sizeX * 0.5, 0.0, sizeZ * 0.5);

        LongArrayList ext = new LongArrayList();
        LongArrayList bottom = new LongArrayList();
        LongArrayList motorList = new LongArrayList();
        int sailCount = 0;
        int oarCount = 0;
        double r2 = 0.0;
        for (Long2ObjectMap.Entry<BlockState> e : this.blocks.long2ObjectEntrySet()) {
            long key = e.getLongKey();
            BlockState state = e.getValue();
            int x = BlockPos.getX(key);
            int y = BlockPos.getY(key);
            int z = BlockPos.getZ(key);
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
            if (y == 0) {
                bottom.add(key);
            }
            if (state.getBlock() instanceof SailBlock) {
                sailCount++;
            } else if (state.getBlock() instanceof OarBlock) {
                oarCount++;
            } else if (state.getBlock() instanceof RaftMotorBlock) {
                motorList.add(key);
            }
            for (int cx = x; cx <= x + 1; cx++) {
                for (int cz = z; cz <= z + 1; cz++) {
                    double dx = cx - this.pivot.x;
                    double dz = cz - this.pivot.z;
                    r2 = Math.max(r2, dx * dx + dz * dz);
                }
            }
        }
        this.exterior = ext.toLongArray();
        // at most 24 evenly spread keel samples are enough to tell water from land
        long[] all = bottom.toLongArray();
        int n = Math.min(all.length, 24);
        this.keel = new long[n];
        for (int i = 0; i < n; i++) {
            this.keel[i] = all[(int) ((long) i * all.length / n)];
        }
        this.motors = motorList.toLongArray();
        this.sails = sailCount;
        this.oars = oarCount;
        this.radius = Math.sqrt(r2);
    }

    // ------------------------------------------------------------------------------------------- access
    public int sizeX() {
        return this.sizeX;
    }

    public int sizeY() {
        return this.sizeY;
    }

    public int sizeZ() {
        return this.sizeZ;
    }

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

    public int sails() {
        return this.sails;
    }

    public int oars() {
        return this.oars;
    }

    public long[] motors() {
        return this.motors;
    }

    public long[] keel() {
        return this.keel;
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

    /** Block entity data (with "id") by local position; empty on clients. */
    public Long2ObjectOpenHashMap<CompoundTag> blockEntities() {
        return this.blockEntities;
    }

    public VesselPose pose(Vec3 pos, float yaw) {
        return new VesselPose(pos, yaw, this.pivot);
    }

    /** The world box around the whole raft at a pose. */
    public AABB worldBounds(VesselPose pose) {
        return pose.toWorld(new AABB(0.0, 0.0, 0.0, this.sizeX, this.sizeY, this.sizeZ));
    }

    // ------------------------------------------------------------------------------------------- collision
    /**
     * Calls {@code out} with the world boxes of all collision boxes that touch {@code query} (until it returns true).
     * Returns true if {@code out} stopped the search.
     */
    public boolean forEachBox(VesselPose pose, AABB query, Predicate<AABB> out) {
        AABB local = pose.toLocal(query);
        // one cell more around: some shapes reach into the next cell (fences and walls are 1.5 high)
        int x0 = Math.max(0, (int) Math.floor(local.minX) - 1);
        int x1 = Math.min(this.sizeX - 1, (int) Math.floor(local.maxX) + 1);
        int y0 = Math.max(0, (int) Math.floor(local.minY) - 1);
        int y1 = Math.min(this.sizeY - 1, (int) Math.floor(local.maxY));
        int z0 = Math.max(0, (int) Math.floor(local.minZ) - 1);
        int z1 = Math.min(this.sizeZ - 1, (int) Math.floor(local.maxZ) + 1);
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

    // ------------------------------------------------------------------------------------------- save / load
    private static int pack(int x, int y, int z) {
        return x | y << 8 | z << 16;
    }

    private static long unpack(int packed) {
        return BlockPos.asLong(packed & 0xFF, packed >> 8 & 0xFF, packed >> 16 & 0xFF);
    }

    public void save(ValueOutput out) {
        out.putIntArray("Size", new int[]{this.sizeX, this.sizeY, this.sizeZ});
        out.putIntArray("Helm", new int[]{this.helm.getX(), this.helm.getY(), this.helm.getZ()});
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
            long key = e.getLongKey();
            data[i++] = pack(BlockPos.getX(key), BlockPos.getY(key), BlockPos.getZ(key));
            data[i++] = id;
        }
        out.store("Palette", CompoundTag.CODEC.listOf(), palette);
        out.putIntArray("Blocks", data);
        ValueOutput.ValueOutputList list = out.childrenList("BlockEntities");
        for (Long2ObjectMap.Entry<CompoundTag> e : this.blockEntities.long2ObjectEntrySet()) {
            long key = e.getLongKey();
            ValueOutput child = list.addChild();
            child.putInt("Pos", pack(BlockPos.getX(key), BlockPos.getY(key), BlockPos.getZ(key)));
            child.store("Data", CompoundTag.CODEC, e.getValue());
        }
    }

    public static @Nullable VesselStructure load(ValueInput in) {
        int[] size = in.getIntArray("Size").orElse(null);
        int[] helm = in.getIntArray("Helm").orElse(null);
        int[] data = in.getIntArray("Blocks").orElse(null);
        List<CompoundTag> palette = in.read("Palette", CompoundTag.CODEC.listOf()).orElse(List.of());
        if (size == null || size.length != 3 || helm == null || helm.length != 3 || data == null) {
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
        Long2ObjectOpenHashMap<CompoundTag> blockEntities = new Long2ObjectOpenHashMap<>();
        in.childrenListOrEmpty("BlockEntities").forEach(child -> {
            long key = unpack(child.getIntOr("Pos", 0));
            child.read("Data", CompoundTag.CODEC).ifPresent(tag -> {
                if (blocks.containsKey(key)) {
                    blockEntities.put(key, tag);
                }
            });
        });
        if (blocks.isEmpty()) {
            return null;
        }
        return new VesselStructure(size[0], size[1], size[2], new BlockPos(helm[0], helm[1], helm[2]), blocks, blockEntities);
    }

    // ------------------------------------------------------------------------------------------- network
    /** Blocks only (clients need no block entity data to draw and collide). */
    public static final StreamCodec<RegistryFriendlyByteBuf, VesselStructure> CLIENT_CODEC = StreamCodec.of(
            (buf, s) -> {
                buf.writeVarInt(s.sizeX);
                buf.writeVarInt(s.sizeY);
                buf.writeVarInt(s.sizeZ);
                buf.writeBlockPos(s.helm);
                buf.writeVarInt(s.blocks.size());
                for (Long2ObjectMap.Entry<BlockState> e : s.blocks.long2ObjectEntrySet()) {
                    long key = e.getLongKey();
                    buf.writeVarInt(pack(BlockPos.getX(key), BlockPos.getY(key), BlockPos.getZ(key)));
                    buf.writeVarInt(Block.getId(e.getValue()));
                }
            },
            buf -> {
                int sx = Math.min(buf.readVarInt(), 255);
                int sy = Math.min(buf.readVarInt(), 255);
                int sz = Math.min(buf.readVarInt(), 255);
                BlockPos helm = buf.readBlockPos();
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
                return new VesselStructure(sx, sy, sz, helm, blocks, new Long2ObjectOpenHashMap<>());
            });
}
