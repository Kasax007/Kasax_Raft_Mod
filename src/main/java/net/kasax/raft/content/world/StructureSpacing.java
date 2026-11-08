package net.kasax.raft.content.world;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;

/**
 * Room between the major structures of the Endless Sea (3.4). The structure sets keep two structures of the same kind
 * apart (spacing / separation); this keeps major structures of different kinds apart too: a major structure does not
 * start where the middle of its box is closer than {@link #DISTANCE} blocks (horizontally) to the middle of a major
 * structure of another kind that comes earlier in {@link #ORDER} and really generates there. The order puts the story
 * places first (the rare island places before the sea places), so a story place never gives way to a wreck or a rig.
 *
 * <p>Like {@link StructurePriority} the check runs in {@code Structure.findValidGenerationPoint}, so the generation,
 * {@code /locate}, the Expedition Journal and treasure charts all agree. The structures it asks about come earlier in
 * the order, so the questions never go round in a circle. {@code tools/content/world/survey.py} estimates the numbers
 * with the same rule.
 */
public final class StructureSpacing {
    /** Smallest distance (blocks, between the middles of the boxes) of two major structures of different kinds. */
    public static final int DISTANCE = 384;
    /**
     * The major structures; earlier ones stay when two of them would come too close. The order follows the ranks of
     * {@link StructurePriority} (story places, then places with people, then the rest), so every question this check
     * and the overlap check ask goes to a structure further up: the chain of questions ends after a few steps.
     */
    public static final List<String> ORDER = List.of("ashen_forge", "old_lighthouse", "port_solace", "saltmere_village",
            "helix_station", "abyssal_sanctum", "coral_temple", "pirate_cove", "rusted_rig", "iceberg_outpost",
            "wreck_galleon", "wreck_cargo_ship", "sunken_plane");
    /** extra chunks searched around the distance: a box middle can lie this far from its start chunk. */
    private static final int REACH = 8;
    private static final Map<String, Integer> INDEX = new ConcurrentHashMap<>();

    static {
        for (int i = 0; i < ORDER.size(); i++) {
            INDEX.put("raft:" + ORDER.get(i), i);
        }
    }

    private StructureSpacing() {
    }

    /** Index in {@link #ORDER}, -1 for a structure that is not major. */
    public static int index(ResourceLocation id) {
        return id == null ? -1 : INDEX.getOrDefault(id.toString(), -1);
    }

    /**
     * True when the structure (id, its box) would start too close to an earlier major structure of another kind.
     */
    public static boolean tooClose(ResourceLocation self, Structure.GenerationContext context, BoundingBox box) {
        int mine = index(self);
        if (mine <= 0) {
            return false;
        }
        double mx = (box.minX() + box.maxX() + 1) / 2.0;
        double mz = (box.minZ() + box.maxZ() + 1) / 2.0;
        ChunkPos here = context.chunkPos();
        int range = DISTANCE / 16 + REACH;
        long seed = context.seed();
        var structures = context.registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (Holder.Reference<StructureSet> set : context.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET).listElements().toList()) {
            if (!(set.value().placement() instanceof RandomSpreadStructurePlacement placement)) {
                continue;
            }
            for (StructureSet.StructureSelectionEntry entry : set.value().structures()) {
                Holder<Structure> other = entry.structure();
                int theirs = index(structures.getKey(other.value()));
                if (theirs < 0 || theirs >= mine) {
                    continue;
                }
                int spacing = placement.spacing();
                for (int gx = Math.floorDiv(here.x - range, spacing); gx <= Math.floorDiv(here.x + range, spacing); gx++) {
                    for (int gz = Math.floorDiv(here.z - range, spacing); gz <= Math.floorDiv(here.z + range, spacing); gz++) {
                        ChunkPos at = placement.getPotentialStructureChunk(seed, gx * spacing, gz * spacing);
                        if (Math.abs(at.x - here.x) > range || Math.abs(at.z - here.z) > range
                                || !placement.applyAdditionalChunkRestrictions(at.x, at.z, seed)) {
                            continue;
                        }
                        BoundingBox b = StructurePriority.boxOf(other, context, at);
                        if (b == null) {
                            continue;
                        }
                        double dx = (b.minX() + b.maxX() + 1) / 2.0 - mx;
                        double dz = (b.minZ() + b.maxZ() + 1) / 2.0 - mz;
                        if (dx * dx + dz * dz < (double) DISTANCE * DISTANCE) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
