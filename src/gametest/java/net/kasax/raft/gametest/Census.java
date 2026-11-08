package net.kasax.raft.gametest;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Structure census for the bctest harness: every structure start of the Endless Sea in a square, worked out the way
 * the game generates them (structure set placement, then {@code Structure.findValidGenerationPoint} with every site
 * check of the mod), without generating any chunk. Writes OUT/census/&lt;label&gt;.txt (one line per start) and logs the
 * count per km², the closest two starts of the same kind and the closest two major structures of any kind.
 */
public final class Census {
    /** The major structures (StructureSpacing.ORDER). */
    static final Set<String> MAJOR = Set.copyOf(net.kasax.raft.content.world.StructureSpacing.ORDER);

    private Census() {
    }

    private record Start(String id, double x, double z, BoundingBox box, int drop) {
    }

    /**
     * How far the ground drops below the bottom of the start piece somewhere under the structure (sea floor or land
     * height of the noise, every 4 blocks): where a structure can hang in the water without its footings.
     */
    private static int drop(ChunkGenerator gen, RandomState rs, ServerLevel level, BoundingBox pieceBox, BoundingBox box) {
        int low = Integer.MAX_VALUE;
        for (int x = box.minX(); x <= box.maxX(); x += 4) {
            for (int z = box.minZ(); z <= box.maxZ(); z += 4) {
                low = Math.min(low, gen.getFirstOccupiedHeight(x, z, net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG, level, rs));
            }
        }
        return pieceBox.minY() - low;
    }

    public static List<String> run(MinecraftServer server, int half, int cx, int cz, Path out, String label, boolean withDrop) {
        ServerLevel level = StructAudit.sea(server);
        ChunkGenerator gen = level.getChunkSource().getGenerator();
        ChunkGeneratorStructureState state = level.getChunkSource().getGeneratorState();
        RandomState rs = level.getChunkSource().randomState();
        Climate.Sampler sampler = rs.createClimateSampler(SamplerContext.builder().enableCaches().build());
        long seed = state.getLevelSeed();
        var structures = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        int c0x = Math.floorDiv(cx - half, 16), c1x = Math.floorDiv(cx + half - 1, 16);
        int c0z = Math.floorDiv(cz - half, 16), c1z = Math.floorDiv(cz + half - 1, 16);
        List<Start> starts = new ArrayList<>();
        Map<String, Integer> tried = new TreeMap<>();
        long t0 = System.currentTimeMillis();
        for (Holder<StructureSet> set : state.possibleStructureSets()) {
            if (!(set.value().placement() instanceof RandomSpreadStructurePlacement placement)) continue;
            var possible = gen.getBiomeSource().possibleBiomes();
            if (set.value().structures().stream().noneMatch(e -> e.structure().value().biomes().stream().anyMatch(possible::contains))) continue;
            int sp = placement.spacing();
            for (int gx = Math.floorDiv(c0x, sp); gx <= Math.floorDiv(c1x, sp); gx++) {
                for (int gz = Math.floorDiv(c0z, sp); gz <= Math.floorDiv(c1z, sp); gz++) {
                    ChunkPos at = placement.getPotentialStructureChunk(seed, gx * sp, gz * sp);
                    if (at.x() < c0x || at.x() > c1x || at.z() < c0z || at.z() > c1z) continue;
                    if (!placement.isStructureChunk(state, at.x(), at.z())) continue;
                    for (StructureSet.StructureSelectionEntry entry : set.value().structures()) {
                        Structure s = entry.structure().value();
                        Identifier id = structures.getKey(s);
                        if (id == null) continue;
                        tried.merge(id.toString(), 1, Integer::sum);
                        Structure.GenerationContext ctx = new Structure.GenerationContext(level.registryAccess(), gen,
                                gen.getBiomeSource(), sampler, rs, server.getStructureTemplateManager(), seed, at, level,
                                s.biomes()::contains);
                        var stub = s.findValidGenerationPoint(ctx);
                        if (stub.isEmpty()) continue;
                        var pieces = stub.get().getPiecesBuilder();
                        if (pieces.isEmpty()) continue;
                        BoundingBox b = pieces.getBoundingBox();
                        int drop = withDrop && MAJOR.contains(id.getPath())
                                ? drop(gen, rs, level, pieces.build().pieces().getFirst().getBoundingBox(), b) : 0;
                        starts.add(new Start(id.toString(), (b.minX() + b.maxX() + 1) / 2.0, (b.minZ() + b.maxZ() + 1) / 2.0, b, drop));
                        break;          // one structure per set and chunk (our sets have one structure each)
                    }
                }
            }
        }
        double km2 = (2.0 * half / 1000.0) * (2.0 * half / 1000.0);
        Map<String, List<Start>> by = new TreeMap<>();
        for (String k : tried.keySet()) by.put(k, new ArrayList<>());
        for (Start s : starts) by.computeIfAbsent(s.id(), k -> new ArrayList<>()).add(s);
        List<String> lines = new ArrayList<>();
        lines.add(String.format(Locale.ROOT, "CENSUS %s square %d blocks around %d %d (%.2f km2), %d starts, %.1f s",
                label, 2 * half, cx, cz, km2, starts.size(), (System.currentTimeMillis() - t0) / 1000.0));
        int majors = 0;
        int ours = 0;
        for (Map.Entry<String, List<Start>> e : by.entrySet()) {
            List<Start> l = e.getValue();
            double closest = Double.NaN;
            for (int i = 0; i < l.size(); i++) {
                for (int k = i + 1; k < l.size(); k++) {
                    double d = Math.hypot(l.get(i).x() - l.get(k).x(), l.get(i).z() - l.get(k).z());
                    if (Double.isNaN(closest) || d < closest) closest = d;
                }
            }
            boolean major = e.getKey().startsWith("raft:") && MAJOR.contains(e.getKey().substring(5));
            if (major) majors += l.size();
            if (e.getKey().startsWith("raft:")) ours += l.size();
            lines.add(String.format(Locale.ROOT, "CENSUS %s %-28s %4d  %7.3f/km2  closest same %6.0f  (candidates %d)%s",
                    label, e.getKey(), l.size(), l.size() / km2, closest, tried.getOrDefault(e.getKey(), 0), major ? "  major" : ""));
        }
        // closest two majors of any kind, and of different kinds
        double any = Double.NaN, diff = Double.NaN;
        String diffPair = "";
        List<Double> nearest = new ArrayList<>();
        for (Start a : starts) {
            if (!a.id().startsWith("raft:") || !MAJOR.contains(a.id().substring(5))) continue;
            double near = Double.NaN;
            for (Start b : starts) {
                if (a == b || !b.id().startsWith("raft:") || !MAJOR.contains(b.id().substring(5))) continue;
                double d = Math.hypot(a.x() - b.x(), a.z() - b.z());
                if (Double.isNaN(near) || d < near) near = d;
                if (Double.isNaN(any) || d < any) any = d;
                if (!a.id().equals(b.id()) && (Double.isNaN(diff) || d < diff)) {
                    diff = d;
                    diffPair = a.id() + " " + (int) a.x() + "," + (int) a.z() + " / " + b.id() + " " + (int) b.x() + "," + (int) b.z();
                }
            }
            if (!Double.isNaN(near)) nearest.add(near);
        }
        nearest.sort(Double::compare);
        lines.add(String.format(Locale.ROOT, "CENSUS %s all Beach Craft %d (%.2f/km2), majors %d (%.3f/km2); closest two majors %.0f, of different kinds %.0f (%s); median nearest major %.0f",
                label, ours, ours / km2, majors, majors / km2, any, diff, diffPair,
                nearest.isEmpty() ? Double.NaN : nearest.get(nearest.size() / 2)));
        List<String> file = new ArrayList<>(lines);
        for (Start s : starts) {
            file.add(String.format(Locale.ROOT, "%s %.0f %.0f %s drop %d", s.id(), s.x(), s.z(), StructAudit.box(s.box()), s.drop()));
        }
        try {
            Files.createDirectories(out.resolve("census"));
            Files.write(out.resolve("census").resolve(label + ".txt"), file);
        } catch (IOException e) {
            BcScript.LOG.warn("[bctest] census: {}", e.toString());
        }
        return lines;
    }
}
