package net.kasax.raft.content.vessel;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.kasax.raft.Raft;
import net.kasax.raft.content.vessel.block.HelmBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Turns a raft of blocks into a {@link VesselEntity} (set sail) and back (drop anchor). Setting sail moves the blocks
 * (with their block entities) into a shipyard plot ({@link VesselShipyard}) where they keep working while the raft
 * sails; dropping anchor moves them back onto the grid.
 *
 * <p>What belongs to the raft: the raft foundations connected to the one under the helm (the hull, tag
 * {@code raft:vessel_hulls}), and every block connected to them that stands inside the hull's outline (holes in the
 * hull count as inside), from the lowest hull layer up to {@link VesselStructure#MAX_HEIGHT} blocks.
 */
public final class VesselAssembler {
    /** Removal: clients only, no shape updates, no drops, no block entity side effects (contents are saved). */
    private static final int FLAGS_LIFT = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS
            | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS | Block.UPDATE_SKIP_ON_PLACE;
    /** Placement: clients only and no shape updates until every block is in place. */
    private static final int FLAGS_PLACE = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private VesselAssembler() {
    }

    private record Scan(@Nullable Component error, LongOpenHashSet blocks, LongOpenHashSet hull) {
        static Scan fail(Component error) {
            return new Scan(error, new LongOpenHashSet(), new LongOpenHashSet());
        }
    }

    // ------------------------------------------------------------------------------------------- set sail
    /** A raft waiting for its shipyard plot to load before it sets sail. */
    private record Launch(ServerLevel level, BlockPos helm, UUID player, int plot, int[] age) {
    }

    private static final List<Launch> LAUNCHES = new ArrayList<>();
    /** Give up when a plot has not loaded after this many ticks. */
    private static final int LAUNCH_TIMEOUT = 1200;

    /**
     * The helm at {@code helmPos} was used: check the raft, then take a shipyard plot and set sail as soon as its
     * chunks are loaded (a tick or two; the first time a plot is used its chunks are generated, which takes longer).
     */
    public static void setSail(ServerLevel level, BlockPos helmPos, ServerPlayer player) {
        if (player.isSpectator() || !(level.getBlockState(helmPos).getBlock() instanceof HelmBlock)
                || VesselShipyard.plotAt(level, helmPos.getX(), helmPos.getY(), helmPos.getZ()) >= 0) {
            return;
        }
        for (Launch launch : LAUNCHES) {
            if (launch.level() == level && launch.helm().equals(helmPos)) {
                return;
            }
        }
        Scan scan = scan(level, helmPos);
        if (scan.error() != null) {
            refuse(level, helmPos, player, scan.error());
            return;
        }
        VesselShipyard yard = VesselShipyard.get(level);
        int plot = yard.allocate();
        if (plot < 0) {
            refuse(level, helmPos, player, Component.translatable("message.raft.vessel.shipyard_full"));
            return;
        }
        VesselShipyard.hold(level, plot);
        LAUNCHES.add(new Launch(level, helmPos.immutable(), player.getUUID(), plot, new int[1]));
        player.sendOverlayMessage(Component.translatable("message.raft.vessel.setting_sail"));
        level.playSound(null, helmPos, SoundEvents.WOODEN_TRAPDOOR_OPEN, SoundSource.BLOCKS, 1.0F, 0.6F);
    }

    private static void refuse(ServerLevel level, BlockPos helmPos, ServerPlayer player, Component error) {
        Raft.LOGGER.info("Raft at {} cannot set sail: {}", helmPos.toShortString(), error.getString());
        player.sendOverlayMessage(error);
        level.playSound(null, helmPos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 0.6F, 0.7F);
    }

    /** Server tick: rafts whose plot has loaded set sail now. */
    static void tickLaunches() {
        for (Iterator<Launch> it = LAUNCHES.iterator(); it.hasNext(); ) {
            Launch launch = it.next();
            ServerLevel level = launch.level();
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(launch.player());
            boolean ready = VesselShipyard.loaded(level, launch.plot());
            if (!ready && ++launch.age()[0] < LAUNCH_TIMEOUT && player != null) {
                continue;
            }
            it.remove();
            VesselEntity vessel = null;
            if (ready && player != null && player.level() == level && level.getBlockState(launch.helm()).getBlock() instanceof HelmBlock helm) {
                Scan scan = scan(level, launch.helm());
                if (scan.error() != null) {
                    refuse(level, launch.helm(), player, scan.error());
                } else {
                    vessel = build(level, launch.helm(), level.getBlockState(launch.helm()).getValue(HelmBlock.FACING), scan, launch.plot());
                    level.playSound(null, launch.helm(), SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.6F, 0.8F);
                    vessel.takeHelm(player);
                    Raft.LOGGER.info("Raft set sail at {} with {} blocks (plot {})", launch.helm().toShortString(), scan.blocks().size(),
                            launch.plot());
                }
            } else if (player != null && !ready) {
                refuse(level, launch.helm(), player, Component.translatable("message.raft.vessel.not_loaded"));
            }
            VesselShipyard.release(level, launch.plot());
            if (vessel == null) {
                VesselShipyard.get(level).free(launch.plot());
            }
        }
    }

    /** Server stopping: give up waiting launches and free their plots (nothing was moved yet). */
    static void abortLaunches() {
        for (Launch launch : LAUNCHES) {
            VesselShipyard.release(launch.level(), launch.plot());
            VesselShipyard.get(launch.level()).free(launch.plot());
        }
        LAUNCHES.clear();
    }

    /** Can this world block be part of a raft at all (water, air, fire and sea plants stay in the world)? */
    static boolean capturable(BlockState state) {
        return !state.isAir() && !(state.getBlock() instanceof LiquidBlock) && !state.is(Blocks.BUBBLE_COLUMN)
                && !state.is(BlockTags.FIRE) && !state.is(Blocks.KELP) && !state.is(Blocks.KELP_PLANT)
                && !state.is(Blocks.SEAGRASS) && !state.is(Blocks.TALL_SEAGRASS);
    }

    private static Scan scan(ServerLevel level, BlockPos helmPos) {
        // the hull block under the helm (through the deck)
        BlockPos seed = null;
        BlockPos.MutableBlockPos cursor = helmPos.mutable();
        for (int i = 0; i < 6; i++) {
            cursor.move(Direction.DOWN);
            BlockState state = level.getBlockState(cursor);
            if (state.is(VesselContent.HULLS)) {
                seed = cursor.immutable();
                break;
            }
            if (!capturable(state)) {
                break;
            }
        }
        if (seed == null) {
            return Scan.fail(Component.translatable("message.raft.vessel.no_hull"));
        }
        // the hull: connected foundations
        LongOpenHashSet hull = new LongOpenHashSet();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        hull.add(seed.asLong());
        queue.add(seed);
        int minX = seed.getX();
        int maxX = minX;
        int minY = seed.getY();
        int minZ = seed.getZ();
        int maxZ = minZ;
        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (hull.contains(n.asLong())) {
                    continue;
                }
                if (!level.isLoaded(n)) {
                    return Scan.fail(Component.translatable("message.raft.vessel.not_loaded"));
                }
                if (!level.getBlockState(n).is(VesselContent.HULLS)) {
                    continue;
                }
                hull.add(n.asLong());
                minX = Math.min(minX, n.getX());
                maxX = Math.max(maxX, n.getX());
                minY = Math.min(minY, n.getY());
                minZ = Math.min(minZ, n.getZ());
                maxZ = Math.max(maxZ, n.getZ());
                if (hull.size() > VesselStructure.MAX_BLOCKS) {
                    return Scan.fail(Component.translatable("message.raft.vessel.too_big", VesselStructure.MAX_BLOCKS));
                }
                if (maxX - minX + 1 > VesselStructure.MAX_SPAN || maxZ - minZ + 1 > VesselStructure.MAX_SPAN) {
                    return Scan.fail(Component.translatable("message.raft.vessel.too_wide", VesselStructure.MAX_SPAN));
                }
                queue.add(n);
            }
        }
        // the outline: hull columns plus the holes they enclose
        int w = maxX - minX + 1;
        int d = maxZ - minZ + 1;
        boolean[] hullColumn = new boolean[w * d];
        for (long key : hull) {
            hullColumn[(BlockPos.getX(key) - minX) * d + BlockPos.getZ(key) - minZ] = true;
        }
        boolean[] outside = new boolean[w * d];
        ArrayDeque<int[]> open = new ArrayDeque<>();
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                if ((x == 0 || z == 0 || x == w - 1 || z == d - 1) && !hullColumn[x * d + z]) {
                    outside[x * d + z] = true;
                    open.add(new int[]{x, z});
                }
            }
        }
        int[][] steps = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!open.isEmpty()) {
            int[] c = open.poll();
            for (int[] s : steps) {
                int x = c[0] + s[0];
                int z = c[1] + s[1];
                if (x >= 0 && z >= 0 && x < w && z < d && !hullColumn[x * d + z] && !outside[x * d + z]) {
                    outside[x * d + z] = true;
                    open.add(new int[]{x, z});
                }
            }
        }
        // everything connected inside the outline, from the hull bottom up
        LongOpenHashSet blocks = new LongOpenHashSet(hull);
        for (long key : hull) {
            queue.add(BlockPos.of(key));
        }
        int topY = minY + VesselStructure.MAX_HEIGHT - 1;
        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos n = p.relative(dir);
                long key = n.asLong();
                if (blocks.contains(key) || n.getY() < minY || n.getY() > topY) {
                    continue;
                }
                int cx = n.getX() - minX;
                int cz = n.getZ() - minZ;
                if (cx < 0 || cz < 0 || cx >= w || cz >= d || outside[cx * d + cz]) {
                    continue;
                }
                BlockState state = level.getBlockState(n);
                if (!capturable(state)) {
                    continue;
                }
                if (state.is(VesselContent.NOT_SEAWORTHY)) {
                    return Scan.fail(Component.translatable("message.raft.vessel.not_seaworthy", state.getBlock().getName()));
                }
                blocks.add(key);
                if (blocks.size() > VesselStructure.MAX_BLOCKS) {
                    return Scan.fail(Component.translatable("message.raft.vessel.too_big", VesselStructure.MAX_BLOCKS));
                }
                queue.add(n);
            }
        }
        if (!blocks.contains(helmPos.asLong())) {
            return Scan.fail(Component.translatable("message.raft.vessel.no_hull"));
        }
        // it must float: water under at least half of the hull's bottom
        Long2ObjectOpenHashMap<BlockPos> lowest = new Long2ObjectOpenHashMap<>();
        for (long key : hull) {
            long column = BlockPos.asLong(BlockPos.getX(key), 0, BlockPos.getZ(key));
            BlockPos cur = lowest.get(column);
            if (cur == null || BlockPos.getY(key) < cur.getY()) {
                lowest.put(column, BlockPos.of(key));
            }
        }
        int wet = 0;
        for (BlockPos p : lowest.values()) {
            if (level.getFluidState(p.below()).is(FluidTags.WATER) || level.getFluidState(p).is(FluidTags.WATER)) {
                wet++;
            }
        }
        if (wet * 2 < lowest.size()) {
            return Scan.fail(Component.translatable("message.raft.vessel.not_floating"));
        }
        return new Scan(null, blocks, hull);
    }

    /** The rotation that turns {@code from} into {@code to}. */
    static Rotation rotation(Direction from, Direction to) {
        for (Rotation r : Rotation.values()) {
            if (r.rotate(from) == to) {
                return r;
            }
        }
        return Rotation.NONE;
    }

    private static CompoundTag saveBlockEntity(BlockEntity be, ServerLevel level) {
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        be.saveWithId(out);
        return out.buildResult();
    }

    private static VesselEntity build(ServerLevel level, BlockPos helmPos, Direction facing, Scan scan, int plot) {
        Rotation toLocal = rotation(facing, Direction.SOUTH);
        LongArrayList order = new LongArrayList(scan.blocks());
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        BlockPos[] rel = new BlockPos[order.size()];
        for (int i = 0; i < rel.length; i++) {
            BlockPos r = BlockPos.of(order.getLong(i)).subtract(helmPos).rotate(toLocal);
            rel[i] = r;
            minX = Math.min(minX, r.getX());
            minY = Math.min(minY, r.getY());
            minZ = Math.min(minZ, r.getZ());
            maxX = Math.max(maxX, r.getX());
            maxZ = Math.max(maxZ, r.getZ());
        }
        // the raft's frame is its plot: the blocks start MARGIN blocks in from the plot's sides, the hull bottom at y 0
        BlockPos shift = new BlockPos(minX - VesselShipyard.MARGIN, minY, minZ - VesselShipyard.MARGIN);
        BlockPos origin = VesselShipyard.origin(level, plot);
        clearPlot(level, plot);
        List<BlockPos> plotPositions = new ArrayList<>(rel.length);
        List<CompoundTag> blockEntities = new ArrayList<>(rel.length);
        for (int i = 0; i < rel.length; i++) {
            BlockPos world = BlockPos.of(order.getLong(i));
            BlockPos at = origin.offset(rel[i].subtract(shift));
            BlockEntity be = level.getBlockEntity(world);
            level.setBlock(at, level.getBlockState(world).rotate(toLocal), FLAGS_PLACE);
            plotPositions.add(at);
            blockEntities.add(be != null ? saveBlockEntity(be, level) : null);
        }
        loadBlockEntities(level, plotPositions, blockEntities);
        updateAll(level, plotPositions);
        Long2ObjectOpenHashMap<BlockState> states = new Long2ObjectOpenHashMap<>();
        for (BlockPos at : plotPositions) {
            BlockState state = level.getBlockState(at);
            if (!state.isAir()) {
                states.put(at.subtract(origin).asLong(), state);
            }
        }
        BlockPos helmLocal = BlockPos.ZERO.subtract(shift);
        Vec3 pivot = new Vec3(VesselShipyard.MARGIN + (maxX - minX + 1) * 0.5, 0.0, VesselShipyard.MARGIN + (maxZ - minZ + 1) * 0.5);
        VesselStructure structure = new VesselStructure(helmLocal, pivot, states);
        float yaw = facing.toYRot();
        // the pivot's world position: the helm's block centre maps onto itself
        VesselPose zero = new VesselPose(0.0, 0.0, 0.0, yaw, pivot);
        Vec3 helmCentre = zero.toWorld(helmLocal.getX() + 0.5, helmLocal.getY() + 0.5, helmLocal.getZ() + 0.5);
        Vec3 pos = new Vec3(helmPos.getX() + 0.5 - helmCentre.x, helmPos.getY() + 0.5 - helmCentre.y, helmPos.getZ() + 0.5 - helmCentre.z);
        VesselPose pose = new VesselPose(pos, yaw, pivot);

        List<VesselEntity.Stowed> stowed = stowHangingEntities(level, scan.blocks(), pose, facing);
        liftBlocks(level, scan);

        VesselEntity vessel = new VesselEntity(VesselContent.VESSEL, level);
        vessel.setup(structure, plot, stowed, pos, yaw);
        level.addFreshEntity(vessel);
        return vessel;
    }

    /** Loads saved block entity data into the block entities just created by placing their blocks. */
    private static void loadBlockEntities(ServerLevel level, List<BlockPos> positions, List<CompoundTag> data) {
        for (int i = 0; i < positions.size(); i++) {
            CompoundTag tag = data.get(i);
            if (tag == null) {
                continue;
            }
            BlockPos pos = positions.get(i);
            BlockEntity be = level.getBlockEntity(pos);
            if (be != null) {
                be.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
                be.setChanged();
                BlockState state = level.getBlockState(pos);
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
            }
        }
    }

    /** After placing blocks without updates: tell their neighbours and fix shapes (fences, redstone, ...). */
    private static void updateAll(ServerLevel level, List<BlockPos> positions) {
        for (BlockPos pos : positions) {
            BlockState state = level.getBlockState(pos);
            level.updateNeighborsAt(pos, state.getBlock());
            state.updateNeighbourShapes(level, pos, Block.UPDATE_ALL);
        }
    }

    /** Removes everything from a plot (no drops, block entity contents are not spilled). */
    static void clearPlot(ServerLevel level, int plot) {
        BlockPos origin = VesselShipyard.origin(level, plot);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = 0; x < VesselShipyard.SIZE_X; x++) {
            for (int z = 0; z < VesselShipyard.SIZE_Z; z++) {
                for (int y = 0; y < VesselShipyard.SIZE_Y; y++) {
                    cursor.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    if (!level.getBlockState(cursor).isAir()) {
                        level.setBlock(cursor, Blocks.AIR.defaultBlockState(), FLAGS_LIFT);
                    }
                }
            }
        }
    }

    /** Item frames and paintings hanging on the raft go into its hold while it sails. */
    private static List<VesselEntity.Stowed> stowHangingEntities(ServerLevel level, LongOpenHashSet blocks, VesselPose pose,
                                                                 Direction bow) {
        List<VesselEntity.Stowed> stowed = new ArrayList<>();
        AABB area = null;
        for (long key : blocks) {
            AABB cell = new AABB(BlockPos.of(key));
            area = area == null ? cell : area.minmax(cell);
        }
        if (area == null) {
            return stowed;
        }
        for (HangingEntity hanging : level.getEntitiesOfClass(HangingEntity.class, area.inflate(1.0))) {
            BlockPos support = hanging.getPos().relative(hanging.getDirection().getOpposite());
            if (!blocks.contains(support.asLong()) && !blocks.contains(hanging.getPos().asLong())) {
                continue;
            }
            TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
            if (!hanging.saveAsPassenger(out)) {
                continue;
            }
            Vec3 at = Vec3.atCenterOf(hanging.getPos());
            stowed.add(new VesselEntity.Stowed(pose.toLocal(at), out.buildResult(), bow));
            hanging.discard();
        }
        return stowed;
    }

    /**
     * Takes the raft's blocks out of the world without drops. The sea flows back where the raft was below the
     * waterline (the highest open water next to the hull), so no hole is left in the water.
     */
    private static void liftBlocks(ServerLevel level, Scan scan) {
        int waterline = Integer.MIN_VALUE;
        for (long key : scan.hull()) {
            BlockPos p = BlockPos.of(key);
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos n = p.relative(d);
                if (!scan.blocks().contains(n.asLong()) && level.getFluidState(n).is(FluidTags.WATER) && level.getFluidState(n).isSource()) {
                    waterline = Math.max(waterline, n.getY());
                }
            }
        }
        List<BlockPos> positions = new ArrayList<>(scan.blocks().size());
        List<BlockState> old = new ArrayList<>(scan.blocks().size());
        for (long key : scan.blocks()) {
            BlockPos p = BlockPos.of(key);
            positions.add(p);
            old.add(level.getBlockState(p));
            level.setBlock(p, Blocks.AIR.defaultBlockState(), FLAGS_LIFT);
        }
        BlockState water = Blocks.WATER.defaultBlockState();
        for (BlockPos p : positions) {
            if (p.getY() <= waterline) {
                level.setBlock(p, water, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }
        for (int i = 0; i < positions.size(); i++) {
            BlockPos p = positions.get(i);
            level.updateNeighborsAt(p, old.get(i).getBlock());
            level.getBlockState(p).updateNeighbourShapes(level, p, Block.UPDATE_ALL);
        }
    }

    // ------------------------------------------------------------------------------------------- sailing checks
    /**
     * Can the raft be at this pose: inside the world border and loaded chunks, no land, other solid block or other raft
     * in any of its outer cells, and water under at least a third of its keel. Lily pads in the way are collected.
     */
    static boolean fits(VesselEntity self, VesselStructure structure, VesselPose pose, List<BlockPos> lilyPads) {
        Level level = self.level();
        AABB bounds = structure.worldBounds(pose);
        if (!level.getWorldBorder().isWithinBounds(bounds)
                || !level.hasChunksAt(Mth.floor(bounds.minX), Mth.floor(bounds.minZ), Mth.floor(bounds.maxX), Mth.floor(bounds.maxZ))) {
            return false;
        }
        boolean[] blocked = {false};
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        structure.forEachExteriorCell(pose, cell -> {
            int x0 = Mth.floor(cell.minX);
            int x1 = Mth.floor(cell.maxX);
            int y0 = Mth.floor(cell.minY) - 1;
            int y1 = Mth.floor(cell.maxY);
            int z0 = Mth.floor(cell.minZ);
            int z1 = Mth.floor(cell.maxZ);
            for (int x = x0; x <= x1; x++) {
                for (int y = y0; y <= y1; y++) {
                    for (int z = z0; z <= z1; z++) {
                        cursor.set(x, y, z);
                        BlockState state = level.getBlockState(cursor);
                        if (state.isAir()) {
                            continue;
                        }
                        if (state.is(Blocks.LILY_PAD)) {
                            lilyPads.add(cursor.immutable());
                            continue;
                        }
                        VoxelShape shape = state.getCollisionShape(level, cursor, CollisionContext.empty());
                        if (!shape.isEmpty() && shape.bounds().move(x, y, z).intersects(cell)) {
                            for (AABB box : shape.toAabbs()) {
                                if (box.move(x, y, z).intersects(cell)) {
                                    blocked[0] = true;
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
            return false;
        });
        if (blocked[0]) {
            return false;
        }
        // other sailing rafts are in the way too
        for (VesselEntity other : VesselColliders.in(level)) {
            VesselStructure os = other.structure();
            if (other == self || os == null || other.isDocked() || !other.getBoundingBox().intersects(bounds)) {
                continue;
            }
            VesselPose op = other.currentPose();
            structure.forEachExteriorCell(pose, cell -> blocked[0] = os.forEachBox(op, cell, box -> true));
            if (blocked[0]) {
                return false;
            }
        }
        long[] keel = structure.keel();
        int wet = 0;
        for (long key : keel) {
            Vec3 c = pose.toWorld(BlockPos.getX(key) + 0.5, 0.5, BlockPos.getZ(key) + 0.5);
            if (VesselEntity.wet(level, BlockPos.containing(c))) {
                wet++;
            }
        }
        return wet * 3 >= keel.length;
    }

    // ------------------------------------------------------------------------------------------- drop anchor
    /** Where may a free world block be replaced by a raft block when it drops anchor? */
    private static boolean replaceable(BlockState state) {
        return state.isAir() || state.canBeReplaced() || state.getBlock() instanceof LiquidBlock || state.is(Blocks.KELP)
                || state.is(Blocks.KELP_PLANT) || state.is(Blocks.BUBBLE_COLUMN) || state.is(Blocks.LILY_PAD);
    }

    private static boolean canPlace(Level level, VesselStructure structure, VesselPose pose) {
        for (long key : structure.blocks().keySet()) {
            BlockPos w = pose.worldBlock(BlockPos.getX(key), BlockPos.getY(key), BlockPos.getZ(key));
            if (!level.isInWorldBounds(w) || !level.hasChunkAt(w) || !level.getWorldBorder().isWithinBounds(w)
                    || !replaceable(level.getBlockState(w))) {
                return false;
            }
        }
        return true;
    }

    /** The pose that puts the blocks of the raft exactly onto the grid at this yaw (a multiple of 90 degrees). */
    private static VesselPose aligned(VesselStructure structure, Vec3 pos, float yaw) {
        VesselPose pose = new VesselPose(pos, yaw, structure.pivot());
        Vec3 c = pose.toWorld(0.5, 0.5, 0.5);
        double ax = Math.round(c.x - 0.5) + 0.5 - c.x;
        double ay = Math.round(c.y - 0.5) + 0.5 - c.y;
        double az = Math.round(c.z - 0.5) + 0.5 - c.z;
        return pose.moved(ax, ay, az);
    }

    /**
     * The nearest free grid pose for dropping anchor: the nearest quarter turn (or the next one when the raft is
     * half way between), the nearest block alignment, then one block further in each direction. Null if all are taken.
     */
    static @Nullable VesselPose findDockPose(VesselEntity vessel) {
        VesselStructure structure = vessel.structure();
        if (structure == null) {
            return null;
        }
        float yaw = Mth.wrapDegrees(vessel.getYRot());
        float snapped = Math.round(yaw / 90.0F) * 90.0F;
        float other = snapped + (yaw > snapped ? 90.0F : -90.0F);
        float[] yaws = Math.abs(yaw - snapped) < 20.0F ? new float[]{snapped} : new float[]{snapped, other};
        int[][] offsets = {{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        for (float y : yaws) {
            VesselPose base = aligned(structure, vessel.position(), y);
            for (int[] o : offsets) {
                VesselPose candidate = base.moved(o[0], 0.0, o[1]);
                if (canPlace(vessel.level(), structure, candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    /**
     * Moves the raft's blocks from its plot back into the world at an aligned pose (block entities with their contents,
     * stowed frames) and empties the plot.
     */
    static boolean dock(VesselEntity vessel, VesselPose pose) {
        VesselStructure structure = vessel.structure();
        if (structure == null || vessel.plot() < 0 || !(vessel.level() instanceof ServerLevel level) || !canPlace(level, structure, pose)
                || !VesselShipyard.loaded(level, vessel.plot())) {
            return false;
        }
        Raft.LOGGER.info("Raft dropped anchor at {} (yaw {})", BlockPos.containing(pose.position()).toShortString(), pose.yaw);
        Rotation rotation = rotation(Direction.SOUTH, Direction.fromYRot(pose.yaw));
        BlockPos origin = VesselShipyard.origin(level, vessel.plot());
        List<BlockPos> placed = new ArrayList<>(structure.blockCount());
        List<CompoundTag> blockEntities = new ArrayList<>(structure.blockCount());
        List<BlockPos> sources = new ArrayList<>(structure.blockCount());
        for (long key : structure.blocks().keySet()) {
            BlockPos from = origin.offset(BlockPos.getX(key), BlockPos.getY(key), BlockPos.getZ(key));
            BlockState state = level.getBlockState(from);
            if (state.isAir()) {
                continue;
            }
            BlockEntity be = level.getBlockEntity(from);
            BlockPos to = pose.worldBlock(BlockPos.getX(key), BlockPos.getY(key), BlockPos.getZ(key));
            level.setBlock(to, state.rotate(rotation), FLAGS_PLACE);
            placed.add(to);
            sources.add(from);
            blockEntities.add(be != null ? saveBlockEntity(be, level) : null);
        }
        loadBlockEntities(level, placed, blockEntities);
        clearPlot(level, vessel.plot());
        updateAll(level, placed);
        restoreStowed(level, vessel, pose);
        Vec3 c = pose.position();
        level.playSound(null, c.x, c.y, c.z, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.6F, 1.2F);
        return true;
    }

    private static void restoreStowed(ServerLevel level, VesselEntity vessel, VesselPose pose) {
        Direction bow = Direction.fromYRot(pose.yaw);
        for (VesselEntity.Stowed s : vessel.stowed()) {
            Rotation rotation = rotation(s.bow(), bow);
            Vec3 at = pose.toWorld(s.local());
            CompoundTag tag = s.tag().copy();
            ListTag posTag = new ListTag();
            posTag.add(DoubleTag.valueOf(at.x));
            posTag.add(DoubleTag.valueOf(at.y));
            posTag.add(DoubleTag.valueOf(at.z));
            tag.put("Pos", posTag);
            tag.remove("UUID");
            if (tag.contains("block_pos")) {
                tag.store("block_pos", BlockPos.CODEC, BlockPos.containing(at));
            }
            EntityType.create(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag), level,
                    new EntitySpawnRequest(EntitySpawnReason.LOAD, true)).ifPresent(entity -> {
                float yRot = entity.rotate(rotation);
                entity.snapTo(at.x, at.y, at.z, yRot, entity.getXRot());
                level.addFreshEntity(entity);
            });
        }
        vessel.stowed().clear();
    }

    /** The raft was killed by a command: drop anchor where it is, or drop everything as items (at the raft). */
    static void emergencyDock(VesselEntity vessel) {
        VesselStructure structure = vessel.structure();
        if (structure == null || vessel.plot() < 0 || !(vessel.level() instanceof ServerLevel level)) {
            return;
        }
        if (!VesselShipyard.loaded(level, vessel.plot())) {
            // the plot cannot be reached now: its blocks stay there, the plot stays taken
            Raft.LOGGER.warn("Raft at {} removed while its plot {} was not loaded", vessel.blockPosition().toShortString(), vessel.plot());
            return;
        }
        VesselPose pose = findDockPose(vessel);
        if (pose != null && dock(vessel, pose)) {
            return;
        }
        // broken as by a player: containers spill, shulker boxes keep their contents; the drops spawn in the plot and
        // appear at the raft (see VesselShipyard.relocate)
        BlockPos origin = VesselShipyard.origin(level, vessel.plot());
        for (long key : structure.blocks().keySet()) {
            level.destroyBlock(origin.offset(BlockPos.getX(key), BlockPos.getY(key), BlockPos.getZ(key)), true);
        }
        clearPlot(level, vessel.plot());
        Vec3 at = vessel.position().add(0.0, 1.0, 0.0);
        for (VesselEntity.Stowed s : vessel.stowed()) {
            EntityType.create(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), s.tag()), level,
                    new EntitySpawnRequest(EntitySpawnReason.LOAD, true)).ifPresent(entity -> {
                entity.snapTo(at.x, at.y, at.z, entity.getYRot(), entity.getXRot());
                if (entity instanceof HangingEntity hanging) {
                    hanging.dropItem(level, null);
                }
            });
        }
        vessel.stowed().clear();
    }
}
