package net.kasax.raft.content.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.kasax.raft.content.world.mixin.StructureTemplateAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.jspecify.annotations.Nullable;

/**
 * The bottom of a Beach Craft structure piece, read from its template (so every chunk sees the same, whatever has
 * been placed yet): the footing columns (lowest full block in the two bottom layers), the columns the template fills
 * at all, and its main bottom block. Used by StructureFoundations (footings and the rock skirt) and by the site check
 * that settles a structure onto the real ground (StructureSiteMixin).
 */
public final class StructureFootprint {
    /** A footing column of a template: local x, y (0 or 1), z, the block there. */
    public record Foot(int x, int y, int z, BlockState state) {
    }

    /** footings, every column with any block of the template, the most common plain bottom block (or null). */
    public record Template(List<Foot> feet, Set<Long> covered, @Nullable BlockState main) {
    }

    /** A footing in the world: column, y of its bottom block, block, piece box bottom. */
    public record WorldFoot(int x, int y, int z, BlockState state) {
    }

    private static final Set<String> PRECIOUS = Set.of("gold_block", "iron_block", "diamond_block", "emerald_block",
            "lapis_block", "copper_block", "netherite_block", "redstone_block", "amethyst_block", "titanium_block",
            "coal_block", "bone_block");
    private static final Map<Identifier, Template> CACHE = new ConcurrentHashMap<>();

    private StructureFootprint() {
    }

    /** Template id of a Beach Craft piece, or null. */
    public static @Nullable Identifier templateOf(StructurePiece piece) {
        if (!(piece instanceof PoolElementStructurePiece p) || !(p.getElement() instanceof SinglePoolElement single)) {
            return null;
        }
        try {
            Identifier id = single.getTemplateLocation();
            return "raft".equals(id.getNamespace()) ? id : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static @Nullable Template template(StructureTemplateManager manager, Identifier id) {
        Template t = CACHE.get(id);
        if (t == null) {
            StructureTemplate st = manager.get(id).orElse(null);
            if (st == null || ((StructureTemplateAccessor) st).raft$palettes().isEmpty()) {
                return null;
            }
            t = read(((StructureTemplateAccessor) st).raft$palettes().getFirst().blocks());
            CACHE.put(id, t);
        }
        return t;
    }

    private static Template read(List<StructureTemplate.StructureBlockInfo> blocks) {
        Map<Long, Foot> feet = new HashMap<>();
        Set<Long> covered = new HashSet<>();
        Map<BlockState, Integer> count = new HashMap<>();
        for (StructureTemplate.StructureBlockInfo info : blocks) {
            BlockState s = info.state();
            if (s.is(Blocks.STRUCTURE_VOID) || s.is(Blocks.JIGSAW)) {
                continue;
            }
            long col = BlockPos.asLong(info.pos().getX(), 0, info.pos().getZ());
            covered.add(col);
            if (info.pos().getY() > 1 || !full(s) || info.nbt() != null) {
                continue;
            }
            Foot f = feet.get(col);
            if (f == null || info.pos().getY() < f.y()) {
                feet.put(col, new Foot(info.pos().getX(), info.pos().getY(), info.pos().getZ(), s));
            }
        }
        for (Foot f : feet.values()) {
            if (plain(f.state())) {
                count.merge(f.state(), 1, Integer::sum);
            }
        }
        BlockState main = count.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
        return new Template(new ArrayList<>(feet.values()), covered, main);
    }

    /** A full block a structure stands on (no fluid, no block entity). */
    static boolean full(BlockState s) {
        return !s.isAir() && s.getFluidState().isEmpty() && !s.hasBlockEntity()
                && s.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
    }

    /** A plain building block for a footing: full, no light, no magma, no ore, not a log or pillar. */
    static boolean plain(BlockState s) {
        if (!full(s) || s.getLightEmission() > 0 || s.is(Blocks.MAGMA_BLOCK) || s.hasProperty(BlockStateProperties.AXIS)) {
            return false;
        }
        String path = BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
        return !path.contains("_ore") && !path.startsWith("raw_") && !PRECIOUS.contains(path);
    }

    public static StructurePlaceSettings settings(PoolElementStructurePiece piece) {
        return new StructurePlaceSettings().setRotation(piece.getRotation());
    }

    /** The footing columns of a piece in world coordinates. */
    public static List<WorldFoot> feet(PoolElementStructurePiece piece, Template t) {
        StructurePlaceSettings set = settings(piece);
        BlockPos origin = piece.getPosition();
        List<WorldFoot> out = new ArrayList<>(t.feet().size());
        for (Foot f : t.feet()) {
            BlockPos w = StructureTemplate.calculateRelativePosition(set, new BlockPos(f.x(), f.y(), f.z())).offset(origin);
            out.add(new WorldFoot(w.getX(), w.getY(), w.getZ(), f.state().rotate(piece.getRotation())));
        }
        return out;
    }

    /** The columns a piece's template fills (world x z packed with BlockPos.asLong(x, 0, z)). */
    public static Set<Long> covered(PoolElementStructurePiece piece, Template t) {
        StructurePlaceSettings set = settings(piece);
        BlockPos origin = piece.getPosition();
        Set<Long> out = new HashSet<>(t.covered().size() * 2);
        for (long c : t.covered()) {
            BlockPos w = StructureTemplate.calculateRelativePosition(set, new BlockPos(BlockPos.getX(c), 0, BlockPos.getZ(c))).offset(origin);
            out.add(BlockPos.asLong(w.getX(), 0, w.getZ()));
        }
        return out;
    }
}
