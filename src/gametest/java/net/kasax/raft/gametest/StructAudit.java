package net.kasax.raft.gametest;

import com.mojang.datafixers.util.Pair;
import net.kasax.raft.gametest.mixin.StructureTemplateAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Structure audit for the bctest harness: compares a generated structure, block by block, with its templates and
 * checks the people (NPCs) the templates place.
 *
 * <ul>
 * <li>intrusion: the template has air there, the world has something else (terrain or a feature grew into a room);
 *     split into solid blocks, fluids and decoration (plants, snow layers...)</li>
 * <li>missing: the template has a block there, the world has air or a fluid</li>
 * <li>replaced: the template has a block there, the world a different one (processors such as cracked bricks show
 *     up here too: they are expected)</li>
 * <li>loose: sand, gravel, ash... with nothing below (falls at the first block update), item and falling block
 *     entities in the structure (blocks that already broke)</li>
 * <li>npc: every person the templates place: found or not, how far from its spot, feet and head free, floor below,
 *     duplicates</li>
 * </ul>
 * The report goes to OUT/audit/&lt;label&gt;.txt; the summary line (AUDIT) to the log.
 */
public final class StructAudit {
    private StructAudit() {
    }

    /** Expected block at a world position: the last piece that places something there wins, like at generation. */
    private record Exp(BlockState state, int piece, String template, BlockPos local) {
    }

    /** Result for the script: the summary, the box of the structure and the person spots. */
    public record Result(String summary, BoundingBox box, List<double[]> npcs) {
    }

    public static ServerLevel sea(MinecraftServer server) {
        return server.getLevel(ResourceKey.create(Registries.DIMENSION, Identifier.parse("raft:raftdim")));
    }

    /** The nearest start of that structure from (x, z) in the Endless Sea, or null. */
    public static StructureStart find(ServerLevel level, String id, BlockPos from) {
        var reg = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        Holder<Structure> holder = reg.get(ResourceKey.create(Registries.STRUCTURE, Identifier.parse(id))).orElse(null);
        if (holder == null) return null;
        Pair<BlockPos, Holder<Structure>> found = level.getChunkSource().getGenerator()
                .findNearestMapStructure(level, HolderSet.direct(holder), from, 100, false);
        if (found == null) return null;
        BlockPos bp = found.getFirst();
        StructureStart st = level.getChunk(bp.getX() >> 4, bp.getZ() >> 4, ChunkStatus.STRUCTURE_STARTS)
                .getStartForStructure(holder.value());
        return st == null || !st.isValid() ? null : st;
    }

    private static String name(Block b) {
        String s = BuiltInRegistries.BLOCK.getKey(b).toString();
        return s.startsWith("minecraft:") ? s.substring(10) : s;
    }

    private static String name(BlockState s) {
        return name(s.getBlock());
    }

    private static void inc(Map<String, Integer> m, String k) {
        m.merge(k, 1, Integer::sum);
    }

    private static String top(Map<String, Integer> m, int n) {
        StringBuilder sb = new StringBuilder();
        m.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).limit(n)
                .forEach(e -> sb.append(sb.isEmpty() ? "" : ", ").append(e.getKey()).append(' ').append(e.getValue()));
        return sb.toString();
    }

    private static int sum(Map<String, Integer> m) {
        return m.values().stream().mapToInt(Integer::intValue).sum();
    }

    private static boolean isAir(BlockState s) {
        return s.isAir();
    }

    private static boolean ignored(BlockState s) {
        return s.is(Blocks.JIGSAW) || s.is(Blocks.STRUCTURE_BLOCK) || s.is(Blocks.STRUCTURE_VOID);
    }

    /**
     * Audits the nearest such structure from (x, z): generates its chunks (full status), compares and writes the
     * report. Must run on the server thread.
     */
    public static Result audit(MinecraftServer server, String id, BlockPos from, Path outDir, String label) {
        ServerLevel level = sea(server);
        StructureStart st = find(level, id, from);
        if (st == null) {
            return new Result("AUDIT " + label + " " + id + ": none found near " + from.toShortString(), null, List.of());
        }
        BoundingBox bb = st.getBoundingBox();
        for (int cx = bb.minX() >> 4; cx <= bb.maxX() >> 4; cx++) {
            for (int cz = bb.minZ() >> 4; cz <= bb.maxZ() >> 4; cz++) {
                level.getChunk(cx, cz);                         // full status: every feature that can reach it ran
            }
        }
        var templates = server.getStructureTemplateManager();
        List<String> lines = new ArrayList<>();
        lines.add("structure " + id + " bbox " + box(bb));

        // expected blocks, piece by piece (later pieces overwrite earlier ones, structure void keeps what is there)
        Map<Long, Exp> exp = new HashMap<>();
        List<double[]> npcSpots = new ArrayList<>();
        List<String> npcTypes = new ArrayList<>();
        List<String> pieceNames = new ArrayList<>();
        int pi = 0, skipped = 0;
        for (StructurePiece pc : st.getPieces()) {
            int index = pi++;
            if (!(pc instanceof PoolElementStructurePiece pe) || !(pe.getElement() instanceof SinglePoolElement el)) {
                pieceNames.add("other " + pc.getClass().getSimpleName());
                lines.add("piece " + index + " " + pc.getClass().getSimpleName() + " " + box(pc.getBoundingBox()) + " (not checked)");
                continue;
            }
            Identifier tid;
            try {
                tid = el.getTemplateLocation();
            } catch (RuntimeException e) {
                pieceNames.add("inline");
                continue;
            }
            StructureTemplate t = templates.get(tid).orElse(null);
            if (t == null) {
                pieceNames.add(tid + " (missing)");
                lines.add("piece " + index + " " + tid + " TEMPLATE MISSING");
                continue;
            }
            pieceNames.add(tid.toString());
            // pieces that follow the terrain (gravity processor, terrain matching) are moved up and down block by
            // block: their template positions say nothing, only their people are checked
            boolean follows = el.getProjection() == net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool.Projection.TERRAIN_MATCHING
                    || ((net.kasax.raft.gametest.mixin.SinglePoolElementAccess) el).bc$processors().value().list().stream()
                    .anyMatch(pr -> pr instanceof net.minecraft.world.level.levelgen.structure.templatesystem.GravityProcessor);
            StructurePlaceSettings set = new StructurePlaceSettings().setRotation(pe.getRotation()).setMirror(Mirror.NONE);
            BlockPos origin = pe.getPosition();
            lines.add("piece " + index + " " + tid + " " + pe.getRotation() + " origin " + origin.toShortString() + " " + box(pc.getBoundingBox())
                    + (follows ? " (follows the terrain: blocks not compared)" : ""));
            var pals = ((StructureTemplateAccess) t).bc$palettes();
            if (pals.isEmpty() || follows) skipped++;
            if (pals.isEmpty()) continue;
            StructureTemplate.Palette pal = set.getRandomPalette(pals, origin);
            for (StructureTemplate.StructureBlockInfo info : pal.blocks()) {
                if (follows || ignored(info.state())) continue;
                BlockPos wp = StructureTemplate.calculateRelativePosition(set, info.pos()).offset(origin);
                exp.put(wp.asLong(), new Exp(info.state().rotate(pe.getRotation()), index, tid.getPath(), info.pos()));
            }
            for (StructureTemplate.StructureEntityInfo e : ((StructureTemplateAccess) t).bc$entities()) {
                String eid = e.nbt.getString("id").orElse("");
                EntityType<?> type = eid.isEmpty() ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(eid)).orElse(null);
                if (type == null || !eid.startsWith("raft:")) continue;
                Vec3 at = StructureTemplate.transform(e.pos, Mirror.NONE, pe.getRotation(), BlockPos.ZERO)
                        .add(origin.getX(), origin.getY(), origin.getZ());
                // people only (crabs and other creatures roam)
                if (!net.kasax.raft.content.npc.NpcContent.TYPES.containsValue(type)) continue;
                npcSpots.add(new double[]{at.x, at.y, at.z});
                npcTypes.add(eid);
            }
        }

        // compare
        Map<String, Integer> solid = new TreeMap<>(), fluid = new TreeMap<>(), deco = new TreeMap<>();
        Map<String, Integer> missing = new TreeMap<>(), replaced = new TreeMap<>();
        Map<String, Integer> cells = new HashMap<>();             // intrusions (solid) per 4x4x4 cell of a template
        Map<String, List<BlockPos>> samples = new LinkedHashMap<>();
        int checked = 0;
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        for (Map.Entry<Long, Exp> en : exp.entrySet()) {
            BlockPos wp = BlockPos.of(en.getKey());
            Exp x = en.getValue();
            BlockState w = level.getBlockState(wp);
            checked++;
            if (isAir(x.state())) {
                if (isAir(w)) continue;
                String k = name(w);
                if (!w.getFluidState().isEmpty() && w.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock) {
                    inc(fluid, k);
                } else if (w.isSolid() || !w.getCollisionShape(level, wp).isEmpty()) {
                    inc(solid, k);
                    String cell = x.template() + " " + (x.local().getX() >> 2 << 2) + "," + (x.local().getY() >> 2 << 2) + "," + (x.local().getZ() >> 2 << 2);
                    inc(cells, cell);
                    samples.computeIfAbsent("solid " + k, q -> new ArrayList<>()).add(wp);
                } else {
                    inc(deco, k);
                    samples.computeIfAbsent("deco " + k, q -> new ArrayList<>()).add(wp);
                }
            } else if (w.getBlock() != x.state().getBlock()) {
                if (isAir(w) || (w.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock)) {
                    inc(missing, name(x.state()) + (isAir(w) ? "" : " (" + name(w) + ")"));
                    samples.computeIfAbsent("missing " + name(x.state()), q -> new ArrayList<>()).add(wp);
                } else {
                    inc(replaced, name(x.state()) + " -> " + name(w));
                    samples.computeIfAbsent("replaced " + name(x.state()) + " -> " + name(w), q -> new ArrayList<>()).add(wp);
                }
            }
        }

        // loose blocks (fall at the first update), dropped items and falling blocks in the box
        Map<String, Integer> loose = new TreeMap<>();
        for (int y = bb.minY(); y <= bb.maxY(); y++) {
            for (int z = bb.minZ(); z <= bb.maxZ(); z++) {
                for (int xx = bb.minX(); xx <= bb.maxX(); xx++) {
                    BlockState s = level.getBlockState(mp.set(xx, y, z));
                    if (s.getBlock() instanceof FallingBlock && FallingBlock.isFree(level.getBlockState(mp.set(xx, y - 1, z)))) {
                        inc(loose, name(s));
                        samples.computeIfAbsent("loose " + name(s), q -> new ArrayList<>()).add(new BlockPos(xx, y, z));
                    }
                }
            }
        }
        AABB area = AABB.of(bb).inflate(3);
        Map<String, Integer> items = new TreeMap<>();
        for (ItemEntity ie : level.getEntitiesOfClass(ItemEntity.class, area)) {
            items.merge(BuiltInRegistries.ITEM.getKey(ie.getItem().getItem()).getPath(), ie.getItem().getCount(), Integer::sum);
        }
        int falling = level.getEntitiesOfClass(FallingBlockEntity.class, area).size();

        // people
        List<String> npcLines = new ArrayList<>();
        List<double[]> npcFound = new ArrayList<>();
        int npcBad = 0;
        for (int i = 0; i < npcSpots.size(); i++) {
            double[] s = npcSpots.get(i);
            String eid = npcTypes.get(i);
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(eid)).orElseThrow();
            Vec3 spot = new Vec3(s[0], s[1], s[2]);
            List<Entity> near = new ArrayList<>(level.getEntities((Entity) null, new AABB(spot, spot).inflate(48), e -> e.getType() == type));
            near.sort(Comparator.comparingDouble(e -> e.position().distanceToSqr(spot)));
            BlockPos spotPos = BlockPos.containing(spot);
            String spotInfo = "spot " + spotPos.toShortString() + " feet " + name(level.getBlockState(spotPos))
                    + " head " + name(level.getBlockState(spotPos.above())) + " floor " + name(level.getBlockState(spotPos.below()));
            if (near.isEmpty()) {
                npcLines.add("NPC " + eid + " NOT FOUND within 48 of " + spotInfo);
                npcBad++;
                continue;
            }
            Entity n = near.getFirst();
            BlockPos np = n.blockPosition();
            boolean free = level.noCollision(n);
            BlockState floor = level.getBlockState(np.below());
            boolean onFloor = !floor.getCollisionShape(level, np.below()).isEmpty() || n.onGround();
            boolean wet = n.isInWater();
            String verdict = free && onFloor && !wet && !n.isInWall() ? "ok" : "PROBLEM";
            if (!verdict.equals("ok")) npcBad++;
            npcLines.add(String.format(Locale.ROOT, "NPC %s %s at %.1f %.1f %.1f (%.1f from spot) free=%s inWall=%s floor=%s water=%s feet=%s head=%s count48=%d | %s",
                    eid, verdict, n.getX(), n.getY(), n.getZ(), Math.sqrt(n.position().distanceToSqr(spot)), free, n.isInWall(),
                    name(floor), wet, name(level.getBlockState(np)), name(level.getBlockState(np.above())), near.size(), spotInfo));
            npcFound.add(new double[]{n.getX(), n.getY(), n.getZ()});
        }

        // worst cells (where terrain or features fill rooms)
        List<Map.Entry<String, Integer>> worst = new ArrayList<>(cells.entrySet());
        worst.sort(Map.Entry.<String, Integer>comparingByValue().reversed());

        StringBuilder sum = new StringBuilder();
        sum.append(String.format(Locale.ROOT, "AUDIT %s %s at %s rot %s pieces %d (%d follow the terrain) checked %d | solid %d [%s] | fluid %d [%s] | deco %d [%s] | missing %d [%s] | replaced %d [%s] | loose %d [%s] | items %d [%s] falling %d | npc %d/%d ok",
                label, id, bb.getCenter().toShortString(),
                st.getPieces().isEmpty() || !(st.getPieces().getFirst() instanceof PoolElementStructurePiece p0) ? "?" : p0.getRotation(),
                st.getPieces().size(), skipped, checked,
                sum(solid), top(solid, 5), sum(fluid), top(fluid, 3), sum(deco), top(deco, 5), sum(missing), top(missing, 5),
                sum(replaced), top(replaced, 6), sum(loose), top(loose, 4), sum(items), top(items, 5), falling,
                npcSpots.size() - npcBad, npcSpots.size()));
        lines.add(sum.toString());
        lines.addAll(npcLines);
        lines.add("worst cells (template, local x,y,z of a 4x4x4 cell: solid blocks in air of the template):");
        for (int i = 0; i < Math.min(15, worst.size()); i++) lines.add("  " + worst.get(i).getKey() + " : " + worst.get(i).getValue());
        lines.add("samples (world positions, first 12 per kind):");
        for (Map.Entry<String, List<BlockPos>> e : samples.entrySet()) {
            StringBuilder sb = new StringBuilder("  " + e.getKey() + " (" + e.getValue().size() + "):");
            for (int i = 0; i < Math.min(12, e.getValue().size()); i++) sb.append(' ').append(e.getValue().get(i).toShortString().replace(" ", ""));
            lines.add(sb.toString());
        }
        lines.add("all solid intrusions: " + solid);
        lines.add("all deco intrusions: " + deco);
        lines.add("all missing: " + missing);
        lines.add("all replaced: " + replaced);
        try {
            Files.createDirectories(outDir.resolve("audit"));
            Files.write(outDir.resolve("audit").resolve(label + ".txt"), lines);
        } catch (IOException e) {
            BcScript.LOG.warn("[bctest] audit report: {}", e.toString());
        }
        String npcSummary = String.join(" || ", npcLines);
        return new Result(sum + (npcSummary.isEmpty() ? "" : " || " + npcSummary), bb, npcFound);
    }

    public static String box(BoundingBox b) {
        return b.minX() + " " + b.minY() + " " + b.minZ() + " .. " + b.maxX() + " " + b.maxY() + " " + b.maxZ();
    }

    /**
     * A camera spot (eye position, yaw, pitch) 2.5-5 blocks from the person's eyes, in free air with a clear view of the
     * face: in front of them if possible, else around them. Null when there is none (the person is walled in).
     */
    public static double[] npcCamera(ServerLevel level, Entity npc) {
        Vec3 eye = npc.getEyePosition();
        float facing = npc.getYHeadRot();
        double[] dists = {3.5, 4.0, 3.0, 4.5, 2.5, 5.0};
        int[] offs = {0, 25, -25, 50, -50, 80, -80, 115, -115, 150, -150, 180};
        for (ClipContext.Block mode : new ClipContext.Block[]{ClipContext.Block.OUTLINE, ClipContext.Block.COLLIDER}) {
            for (int off : offs) {
                for (double d : dists) {
                    double a = Math.toRadians(facing + off);
                    // yaw 0 looks to +z; a point in front of the person: (-sin, cos)
                    Vec3 cam = eye.add(-Math.sin(a) * d, 0.15, Math.cos(a) * d);
                    AABB box = new AABB(cam.add(-0.35, -0.35, -0.35), cam.add(0.35, 0.35, 0.35));
                    if (!level.noCollision(box)) continue;
                    BlockPos cp = BlockPos.containing(cam);
                    if (!level.getFluidState(cp).isEmpty() || !level.getBlockState(cp).isAir()) continue;
                    HitResult hit = level.clip(new ClipContext(cam, eye, mode, ClipContext.Fluid.ANY, CollisionContext.empty()));
                    if (hit.getType() != HitResult.Type.MISS) continue;
                    float[] r = BcScript.aim(cam.x, cam.y, cam.z, eye);
                    return new double[]{cam.x, cam.y, cam.z, r[0], r[1]};
                }
            }
        }
        return null;
    }

    /**
     * An overview camera for a structure box: out from the center towards one of the four diagonals (dir 0-3), above
     * the terrain there, looking at the center. Eye position, yaw, pitch.
     */
    public static double[] overview(ServerLevel level, BoundingBox bb, int dir) {
        Vec3 c = Vec3.atCenterOf(bb.getCenter());
        double size = Math.max(bb.getXSpan(), Math.max(bb.getZSpan(), 16));
        double dist = size * 0.55 + 8;
        double a = Math.toRadians(45 + 90 * dir);
        double x = c.x + Math.cos(a) * dist, z = c.z + Math.sin(a) * dist;
        level.getChunk((int) Math.floor(x) >> 4, (int) Math.floor(z) >> 4);
        int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
        // a little above the top of the structure (and the ground under the camera), looking down at its middle
        double y = Math.max(ground + 3, bb.maxY() + Math.min(12, size * 0.15));
        float[] r = BcScript.aim(x, y, z, new Vec3(c.x, (bb.minY() + bb.maxY()) / 2.0 + 2, c.z));
        return new double[]{x, y, z, r[0], r[1]};
    }
}
