package net.kasax.raft.content.world;

import com.mojang.datafixers.util.Either;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.kasax.raft.Raft;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

/**
 * Keeps the structures of the Endless Sea from growing into each other.
 *
 * <p>Structures are placed independently, so a Rusted Rig could stand on the Abyssal Sanctum (its legs broke the
 * sanctum's walls and flooded it) or ruins could cut into the Helix Station. Every Beach Craft structure has a rank:
 * story places ({@code #raft:priority/story}) first, places with a person ({@code #raft:priority/people}) second, all
 * others last. A structure does not start where its box (plus a small gap) would touch the box of a structure of a
 * higher rank that starts nearby. The check runs in {@code Structure.findValidGenerationPoint}, which both the real
 * generation and the structure search ({@code /locate}, treasure maps) use, so a map never leads to a place that is
 * not there. The boxes of the higher ranked structures are worked out the same way the game generates them (same
 * seed, same chunk), and cached.
 */
public final class StructurePriority {
    public static final TagKey<Structure> STORY = TagKey.create(Registries.STRUCTURE, Raft.id("priority/story"));
    public static final TagKey<Structure> PEOPLE = TagKey.create(Registries.STRUCTURE, Raft.id("priority/people"));
    /** blocks kept free between two structures. */
    private static final int GAP = 3;
    /** chunks around a start searched for structures of a higher rank (they reach at most ~9 chunks). */
    private static final int RANGE = 12;
    private static final BoundingBox NONE = new BoundingBox(0, 0, 0, 0, 0, 0);
    private record Key(Structure structure, long seed, long chunk) {
    }
    private static final ConcurrentHashMap<Key, BoundingBox> BOXES = new ConcurrentHashMap<>();

    private StructurePriority() {
    }

    private static int rank(Holder<Structure> s) {
        return s.is(STORY) ? 0 : s.is(PEOPLE) ? 1 : 2;
    }

    /**
     * The stub to use: with its pieces built (once, so the generation does not build them a second time with a used
     * random), or empty when the structure would touch a structure of a higher rank.
     */
    public static Optional<Structure.GenerationStub> check(Structure self, Structure.GenerationContext context,
                                                           Structure.GenerationStub stub) {
        Registry<Structure> structures = context.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        Holder<Structure> holder = structures.wrapAsHolder(self);
        int rank = rank(holder);
        if (rank == 0) {
            return Optional.of(stub);
        }
        StructurePiecesBuilder pieces = stub.getPiecesBuilder();
        Structure.GenerationStub built = new Structure.GenerationStub(stub.position(), Either.right(pieces));
        if (pieces.isEmpty()) {
            return Optional.of(built);
        }
        BoundingBox box = pieces.getBoundingBox().inflatedBy(GAP);
        ChunkPos here = context.chunkPos();
        long seed = context.seed();
        for (Holder<StructureSet> set : context.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET).listElements().toList()) {
            if (!(set.value().placement() instanceof RandomSpreadStructurePlacement placement)) {
                continue;
            }
            for (StructureSet.StructureSelectionEntry entry : set.value().structures()) {
                Holder<Structure> other = entry.structure();
                if (rank(other) >= rank) {
                    continue;
                }
                int spacing = placement.spacing();
                for (int gx = Math.floorDiv(here.x() - RANGE, spacing); gx <= Math.floorDiv(here.x() + RANGE, spacing); gx++) {
                    for (int gz = Math.floorDiv(here.z() - RANGE, spacing); gz <= Math.floorDiv(here.z() + RANGE, spacing); gz++) {
                        ChunkPos at = placement.getPotentialStructureChunk(seed, gx * spacing, gz * spacing);
                        if (Math.abs(at.x() - here.x()) > RANGE || Math.abs(at.z() - here.z()) > RANGE
                                || !placement.applyAdditionalChunkRestrictions(at.x(), at.z(), seed)) {
                            continue;
                        }
                        BoundingBox theirs = boxOf(other, context, at);
                        if (theirs != null && theirs.intersects(box)) {
                            return Optional.empty();
                        }
                    }
                }
            }
        }
        return Optional.of(built);
    }

    /** The box of that structure if it starts in that chunk (as the game would generate it), else null. */
    private static BoundingBox boxOf(Holder<Structure> structure, Structure.GenerationContext context, ChunkPos at) {
        Key key = new Key(structure.value(), context.seed(), ChunkPos.pack(at.x(), at.z()));
        BoundingBox cached = BOXES.get(key);
        if (cached == null) {
            Structure.GenerationContext there = new Structure.GenerationContext(context.registryAccess(),
                    context.chunkGenerator(), context.biomeSource(), context.climateSampler(), context.randomState(),
                    context.structureTemplateManager(), context.seed(), at, context.heightAccessor(),
                    structure.value().biomes()::contains);
            cached = structure.value().findValidGenerationPoint(there)
                    .map(Structure.GenerationStub::getPiecesBuilder)
                    .filter(b -> !b.isEmpty())
                    .map(StructurePiecesBuilder::getBoundingBox)
                    .orElse(NONE);
            if (BOXES.size() > 20000) {
                BOXES.clear();
            }
            BOXES.put(key, cached);
        }
        return cached == NONE ? null : cached;
    }
}
