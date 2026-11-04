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
import net.minecraft.world.entity.Entity;
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
import java.util.List;

/**
 * Turns a raft of blocks into a {@link VesselEntity} (set sail) and back (drop anchor).
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
    /** The helm at {@code helmPos} was used: build the vessel and seat the player, or tell them what is wrong. */
    public static void setSail(ServerLevel level, BlockPos helmPos, ServerPlayer player) {
        if (player.isSpectator()) {
            return;
        }
        BlockState helm = level.getBlockState(helmPos);
        if (!(helm.getBlock() instanceof HelmBlock)) {
            return;
        }
        Scan scan = scan(level, helmPos);
        if (scan.error() != null) {
            Raft.LOGGER.info("Raft at {} cannot set sail: {}", helmPos.toShortString(), scan.error().getString());
            player.sendOverlayMessage(scan.error());
            level.playSound(null, helmPos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 0.6F, 0.7F);
            return;
        }
        VesselEntity vessel = build(level, helmPos, helm.getValue(HelmBlock.FACING), scan);
        level.playSound(null, helmPos, SoundEvents.WOODEN_TRAPDOOR_OPEN, SoundSource.BLOCKS, 1.0F, 0.6F);
        level.playSound(null, helmPos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.6F, 0.8F);
        vessel.takeHelm(player);
        Raft.LOGGER.info("Raft set sail at {} with {} blocks", helmPos.toShortString(), scan.blocks().size());
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

    private static VesselEntity build(ServerLevel level, BlockPos helmPos, Direction facing, Scan scan) {
        Rotation toLocal = rotation(facing, Direction.SOUTH);
        LongArrayList order = new LongArrayList(scan.blocks());
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        BlockPos[] rel = new BlockPos[order.size()];
        for (int i = 0; i < rel.length; i++) {
            BlockPos r = BlockPos.of(order.getLong(i)).subtract(helmPos).rotate(toLocal);
            rel[i] = r;
            minX = Math.min(minX, r.getX());
            minY = Math.min(minY, r.getY());
            minZ = Math.min(minZ, r.getZ());
            maxX = Math.max(maxX, r.getX());
            maxY = Math.max(maxY, r.getY());
            maxZ = Math.max(maxZ, r.getZ());
        }
        BlockPos shift = new BlockPos(minX, minY, minZ);
        Long2ObjectOpenHashMap<BlockState> states = new Long2ObjectOpenHashMap<>();
        Long2ObjectOpenHashMap<CompoundTag> blockEntities = new Long2ObjectOpenHashMap<>();
        for (int i = 0; i < rel.length; i++) {
            BlockPos world = BlockPos.of(order.getLong(i));
            BlockPos local = rel[i].subtract(shift);
            BlockState state = level.getBlockState(world);
            states.put(local.asLong(), state.rotate(toLocal));
            BlockEntity be = level.getBlockEntity(world);
            if (be != null) {
                blockEntities.put(local.asLong(), saveBlockEntity(be, level));
            }
        }
        BlockPos helmLocal = BlockPos.ZERO.subtract(shift);
        VesselStructure structure = new VesselStructure(maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1, helmLocal, states, blockEntities);
        float yaw = facing.toYRot();
        // the pivot's world position: the helm's block centre maps onto itself
        VesselPose zero = new VesselPose(0.0, 0.0, 0.0, yaw, structure.pivot());
        Vec3 helmCentre = zero.toWorld(helmLocal.getX() + 0.5, helmLocal.getY() + 0.5, helmLocal.getZ() + 0.5);
        Vec3 pos = new Vec3(helmPos.getX() + 0.5 - helmCentre.x, helmPos.getY() + 0.5 - helmCentre.y, helmPos.getZ() + 0.5 - helmCentre.z);
        VesselPose pose = new VesselPose(pos, yaw, structure.pivot());

        List<VesselEntity.Stowed> stowed = stowHangingEntities(level, scan.blocks(), pose, facing);
        liftBlocks(level, scan);

        VesselEntity vessel = new VesselEntity(VesselContent.VESSEL, level);
        vessel.setup(structure, stowed, pos, yaw);
        level.addFreshEntity(vessel);
        return vessel;
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
     * Can the raft be at this pose: inside the world border and loaded chunks, no land or other solid block in any of
     * its outer cells, and water under at least a third of its keel. Lily pads in the way are collected (to break).
     */
    static boolean fits(Level level, VesselStructure structure, VesselPose pose, List<BlockPos> lilyPads) {
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

    /** Places the raft's blocks at an aligned pose (block entities and stowed frames included). */
    static boolean dock(VesselEntity vessel, VesselPose pose) {
        VesselStructure structure = vessel.structure();
        if (structure == null || !(vessel.level() instanceof ServerLevel level) || !canPlace(level, structure, pose)) {
            return false;
        }
        Raft.LOGGER.info("Raft dropped anchor at {} (yaw {})", BlockPos.containing(pose.position()).toShortString(), pose.yaw);
        Rotation rotation = rotation(Direction.SOUTH, Direction.fromYRot(pose.yaw));
        List<BlockPos> placed = new ArrayList<>(structure.blockCount());
        List<BlockState> states = new ArrayList<>(structure.blockCount());
        for (Long2ObjectMap.Entry<BlockState> e : structure.blocks().long2ObjectEntrySet()) {
            long key = e.getLongKey();
            BlockPos w = pose.worldBlock(BlockPos.getX(key), BlockPos.getY(key), BlockPos.getZ(key));
            BlockState state = e.getValue().rotate(rotation);
            level.setBlock(w, state, FLAGS_PLACE);
            placed.add(w);
            states.add(state);
        }
        for (Long2ObjectMap.Entry<CompoundTag> e : structure.blockEntities().long2ObjectEntrySet()) {
            long key = e.getLongKey();
            BlockPos w = pose.worldBlock(BlockPos.getX(key), BlockPos.getY(key), BlockPos.getZ(key));
            BlockEntity be = level.getBlockEntity(w);
            if (be != null) {
                be.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), e.getValue()));
                be.setChanged();
                BlockState state = level.getBlockState(w);
                level.sendBlockUpdated(w, state, state, Block.UPDATE_ALL);
            }
        }
        for (int i = 0; i < placed.size(); i++) {
            BlockPos w = placed.get(i);
            BlockState state = level.getBlockState(w);
            level.updateNeighborsAt(w, state.getBlock());
            state.updateNeighbourShapes(level, w, Block.UPDATE_ALL);
        }
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

    /** The raft was killed by a command: drop anchor where it is, or drop everything as items. */
    static void emergencyDock(VesselEntity vessel) {
        VesselStructure structure = vessel.structure();
        if (structure == null || !(vessel.level() instanceof ServerLevel level)) {
            return;
        }
        VesselPose pose = findDockPose(vessel);
        if (pose != null && dock(vessel, pose)) {
            return;
        }
        Vec3 at = vessel.position().add(0.0, 1.0, 0.0);
        for (Long2ObjectMap.Entry<BlockState> e : structure.blocks().long2ObjectEntrySet()) {
            ItemStack stack = new ItemStack(e.getValue().getBlock().asItem());
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, at.x, at.y, at.z, stack);
            }
            CompoundTag tag = structure.blockEntities().get(e.getLongKey());
            if (tag != null) {
                BlockEntity be = BlockEntity.loadStatic(BlockPos.containing(at), e.getValue(), tag, level.registryAccess());
                if (be instanceof Container container) {
                    Containers.dropContents(level, BlockPos.containing(at), container);
                }
            }
        }
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

    /** For callers outside this package (e.g. the use payload). */
    public static boolean isHelmOf(Entity entity, BlockPos local) {
        return entity instanceof VesselEntity vessel && vessel.structure() != null && vessel.structure().helm().equals(local);
    }
}
