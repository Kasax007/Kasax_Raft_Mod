package net.kasax.raft.content.encounters;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.kasax.raft.Raft;
import net.kasax.raft.content.encounters.entity.ReefGuardianEntity;
import net.kasax.raft.content.encounters.entity.ScavengerEntity;
import net.kasax.raft.content.encounters.entity.ScavengerEntity.Kit;
import net.kasax.raft.world.dimension.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.Difficulty;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Guarded structures (3.2). When a player first comes close to a Beach Craft structure that has guards, a warning
 * sounds (a horn, the hum of the reef, ...) with a line on the action bar, and a moment later its guards appear
 * around the unopened loot chests: once per structure, saved with the world. The guards stay (no despawn) and their
 * drops reward the fight. Nothing guards the places of the story's people (Saltmere, the castaway camp) or the
 * Leviathan's sanctum, and the guards never block the way: they stand around the chests, not in doorways.
 * <p>
 * The difficulty curve from the first island to the Leviathan, stage by stage (health / damage on Normal):
 * <pre>
 * stage 1 (start, chapters 1-2): abandoned raft, beach shack, fishing boat wreck, old lighthouse, iceberg outpost
 *   scavengers 20 hp, fists 2 or wooden blade 5, a few with bows (arrows 2-4); drowned 20 hp / 3; strays 20 hp / bow
 * stage 2 (chapters 3-4, side wrecks): sunken plane, sunken ruins, cargo ship wreck, Helix Station, rusted rig
 *   scavengers with stone blades 6 and leather (mid and rig, or a bow on the rig), reef guardians 30 hp / beam 5;
 *   elite: Scavenger Chief (rig) 50 hp, armour 4 + iron, iron sword 7
 * stage 3 (chapters 5-7 and the pirate side story): Port Solace, pirate cove, galleon, Ashen Forge, Coral Temple
 *   corsairs 24 hp / cutlass 9, drowned groups, blazes 20 hp / fire, ember crabs 14 hp / 4 + fire;
 *   elites: Corsair Captain 80 hp, armour 6, cutlass 9, calls two corsairs at half health;
 *   Scavenger Chief (Port Solace); Reef Warden (Coral Temple) 80 hp, armour 8, beam 7
 * then the Leviathan (boss module).
 * </pre>
 * Easy has one guard less per group, Hard one more.
 */
public final class StructureEncounters extends SavedData {
    private static final int CHECK_INTERVAL = 20;
    /** Ticks between the warning and the guards (time to get ready). */
    private static final int WARNING_TICKS = 40;
    private static final String GUARD_TAG = "raft_guard";

    private record Guard(String entity, int min, int max, @Nullable Kit kit, @Nullable String elite) {
    }

    private record Encounter(int stage, String alarm, List<Guard> guards) {
    }

    private static Guard g(String entity, int min, int max) {
        return new Guard(entity, min, max, null, null);
    }

    private static Guard scav(int min, int max, Kit kit) {
        return new Guard("raft:scavenger", min, max, kit, null);
    }

    private static Guard elite(String entity, String elite) {
        return new Guard(entity, 1, 1, null, elite);
    }

    private static final Map<String, Encounter> TABLE = Map.ofEntries(
            // ---- stage 1: light threats
            Map.entry("abandoned_raft", new Encounter(1, "scavengers", List.of(scav(1, 2, Kit.LIGHT)))),
            Map.entry("beach_shack", new Encounter(1, "scavengers", List.of(scav(1, 1, Kit.LIGHT), scav(1, 1, Kit.ARCHER)))),
            Map.entry("wreck_fishing_boat", new Encounter(1, "deep", List.of(g("minecraft:drowned", 2, 2)))),
            Map.entry("old_lighthouse", new Encounter(1, "scavengers", List.of(scav(2, 2, Kit.LIGHT), scav(1, 1, Kit.ARCHER)))),
            Map.entry("iceberg_outpost", new Encounter(1, "scavengers", List.of(g("minecraft:stray", 2, 2)))),
            // ---- stage 2: tougher and mixed
            Map.entry("sunken_plane", new Encounter(2, "deep", List.of(g("minecraft:drowned", 2, 3)))),
            Map.entry("sunken_ruins", new Encounter(2, "reef", List.of(g("raft:reef_guardian", 2, 2), g("minecraft:drowned", 1, 1)))),
            Map.entry("wreck_cargo_ship", new Encounter(2, "scavengers", List.of(scav(2, 2, Kit.MID), scav(1, 1, Kit.ARCHER),
                    g("minecraft:drowned", 2, 2)))),
            Map.entry("helix_station", new Encounter(2, "deep", List.of(g("minecraft:drowned", 3, 3), g("raft:reef_guardian", 1, 1)))),
            Map.entry("rusted_rig", new Encounter(2, "scavengers", List.of(scav(3, 3, Kit.RIG),
                    new Guard("raft:scavenger", 1, 1, Kit.CHIEF, "chief")))),
            // ---- stage 3: a real fight before the Leviathan
            Map.entry("pirate_cove", new Encounter(3, "corsairs", List.of(g("raft:drowned_corsair", 3, 3),
                    elite("raft:corsair_captain", "captain")))),
            Map.entry("wreck_galleon", new Encounter(3, "corsairs", List.of(g("raft:drowned_corsair", 3, 3),
                    elite("raft:corsair_captain", "captain")))),
            Map.entry("port_solace", new Encounter(3, "deep", List.of(g("minecraft:drowned", 4, 4), g("raft:drowned_corsair", 2, 2),
                    scav(2, 2, Kit.RIG), new Guard("raft:scavenger", 1, 1, Kit.CHIEF, "chief")))),
            Map.entry("ashen_forge", new Encounter(3, "forge", List.of(g("raft:ember_crab", 4, 4), g("minecraft:blaze", 2, 2),
                    g("minecraft:magma_cube", 2, 2)))),
            Map.entry("coral_temple", new Encounter(3, "reef", List.of(g("raft:reef_guardian", 3, 3),
                    new Guard("raft:reef_guardian", 1, 1, null, "warden")))));

    private static final Codec<StructureEncounters> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.listOf().optionalFieldOf("done", List.of()).forGetter(s -> new ArrayList<>(s.done))
    ).apply(i, StructureEncounters::new));
    private static final SavedDataType<StructureEncounters> TYPE = new SavedDataType<>(Raft.id("structure_encounters"),
            StructureEncounters::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Set<String> done = new HashSet<>();
    /** Not saved: warnings given, guards still to come. */
    private final List<Pending> pending = new ArrayList<>();

    private record Pending(String key, Encounter encounter, BoundingBox box, ServerPlayer player, long at) {
    }

    public StructureEncounters() {
    }

    private StructureEncounters(List<String> done) {
        this.done.addAll(done);
    }

    public static StructureEncounters get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public static void register() {
        // (environment BC_NO_ENCOUNTERS: off, for tests that need quiet structures)
        if (System.getenv("BC_NO_ENCOUNTERS") == null) {
            ServerTickEvents.END_LEVEL_TICK.register(StructureEncounters::tick);
        }
    }

    private static void tick(ServerLevel level) {
        if (!level.dimension().equals(ModDimensions.RAFTDIM_LEVEL_KEY)) {
            return;
        }
        StructureEncounters data = null;
        long now = level.getGameTime();
        if (now % CHECK_INTERVAL == 0 && level.getDifficulty() != Difficulty.PEACEFUL) {
            for (ServerPlayer player : level.players()) {
                if (player.isSpectator() || player.isCreative() && System.getenv("BC_ENCOUNTERS_CREATIVE") == null) {
                    continue;
                }
                if (data == null) {
                    data = get(level);
                }
                data.check(level, player, now);
            }
        }
        if (data == null && now % 5 == 0) {
            data = get(level);
        }
        if (data != null && !data.pending.isEmpty()) {
            data.spawnDue(level, now);
        }
    }

    // ------------------------------------------------------------------------------------------- trigger
    private void check(ServerLevel level, ServerPlayer player, long now) {
        BlockPos at = player.blockPosition();
        var registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        List<StructureStart> starts = level.structureManager().startsForStructure(at.getX() >> 4, at.getZ() >> 4, s -> {
            Identifier id = registry.getKey(s);
            return id != null && id.getNamespace().equals(Raft.MOD_ID) && TABLE.containsKey(id.getPath());
        });
        for (StructureStart start : starts) {
            Identifier id = registry.getKey(start.getStructure());
            if (id == null) {
                continue;
            }
            String key = id.getPath() + "@" + start.getChunkPos().x() + "," + start.getChunkPos().z();
            if (this.done.contains(key) || this.pending.stream().anyMatch(p -> p.key.equals(key))) {
                continue;
            }
            BoundingBox box = start.getBoundingBox();
            if (at.getX() < box.minX() - 8 || at.getX() > box.maxX() + 8 || at.getZ() < box.minZ() - 8 || at.getZ() > box.maxZ() + 8
                    || at.getY() < box.minY() - 16 || at.getY() > box.maxY() + 16) {
                continue;
            }
            Encounter encounter = TABLE.get(id.getPath());
            this.pending.add(new Pending(key, encounter, box, player, now + WARNING_TICKS));
            player.sendOverlayMessage(Component.translatable("encounter.raft." + encounter.alarm()));
            level.playSound(null, player.getX(), player.getY(), player.getZ(), alarm(encounter.alarm()), SoundSource.HOSTILE, 1.2F, 1.0F);
            Raft.LOGGER.info("Encounter {} (stage {}) woke up for {}", key, encounter.stage(), player.getName().getString());
        }
    }

    private static SoundEvent alarm(String kind) {
        return switch (kind) {
            case "deep" -> EncounterSounds.ALARM_DEEP;
            case "corsairs" -> EncounterSounds.ALARM_CORSAIRS;
            case "reef" -> EncounterSounds.ALARM_REEF;
            case "forge" -> EncounterSounds.ALARM_FORGE;
            default -> EncounterSounds.ALARM_SCAVENGERS;
        };
    }

    // ------------------------------------------------------------------------------------------- spawning
    private void spawnDue(ServerLevel level, long now) {
        for (int i = this.pending.size() - 1; i >= 0; i--) {
            Pending p = this.pending.get(i);
            if (now < p.at) {
                continue;
            }
            this.pending.remove(i);
            int n = this.spawn(level, p);
            this.done.add(p.key);
            this.setDirty();
            Raft.LOGGER.info("Encounter {}: {} guards", p.key, n);
        }
    }

    private int spawn(ServerLevel level, Pending p) {
        RandomSource random = level.getRandom();
        List<BlockPos> anchors = anchors(level, p.box);
        BlockPos center = p.box.getCenter();
        if (anchors.isEmpty()) {
            anchors.add(center);
        }
        int spawned = 0;
        int next = 0;
        for (Guard guard : p.encounter.guards()) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(guard.entity())).orElse(null);
            if (type == null) {
                continue;
            }
            int count = guard.min() + (guard.max() > guard.min() ? random.nextInt(guard.max() - guard.min() + 1) : 0);
            if (guard.elite() == null) {
                if (level.getDifficulty() == Difficulty.EASY) {
                    count = Math.max(1, count - 1);
                } else if (level.getDifficulty() == Difficulty.HARD) {
                    count++;
                }
            }
            for (int k = 0; k < count; k++) {
                BlockPos anchor = anchors.get(next++ % anchors.size());
                BlockPos pos = spot(level, type, anchor, p.box, p.player, random, 7);
                if (pos == null) {
                    pos = spot(level, type, center, p.box, p.player, random, 12);
                }
                if (pos == null) {
                    // narrow decks and catwalks (the rig): anywhere in the structure where it can stand
                    pos = anywhere(level, type, p.box, p.player, random);
                }
                if (pos != null && this.summon(level, type, guard, pos, random)) {
                    spawned++;
                }
            }
        }
        return spawned;
    }

    /** Unopened loot containers of the structure (where the guards stand), the first three by distance to its centre. */
    private static List<BlockPos> anchors(ServerLevel level, BoundingBox box) {
        List<BlockPos> out = new ArrayList<>();
        for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
            for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be instanceof RandomizableContainer c && c.getLootTable() != null && box.isInside(be.getBlockPos())) {
                        out.add(be.getBlockPos());
                    }
                }
            }
        }
        BlockPos center = box.getCenter();
        out.sort(Comparator.comparingDouble(p -> p.distSqr(center)));
        return new ArrayList<>(out.subList(0, Math.min(3, out.size())));
    }

    private enum Medium { LAND, WATER, BOTH, AIR }

    private static Medium medium(EntityType<?> type) {
        String path = BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath();
        return switch (path) {
            case "reef_guardian", "guardian" -> Medium.WATER;
            case "drowned", "drowned_corsair", "corsair_captain" -> Medium.BOTH;
            case "blaze" -> Medium.AIR;
            default -> Medium.LAND;
        };
    }

    /** A free spot near the anchor inside the structure where the guard can stand (or swim), not next to the player. */
    private static @Nullable BlockPos spot(ServerLevel level, EntityType<?> type, BlockPos anchor, BoundingBox box,
                                           ServerPlayer player, RandomSource random, int reach) {
        BoundingBox area = box.inflatedBy(2);
        for (int tries = 0; tries < 96; tries++) {
            int r = tries < 32 ? Math.min(4, reach) : reach;
            BlockPos pos = anchor.offset(random.nextInt(2 * r + 1) - r, random.nextInt(9) - 4, random.nextInt(2 * r + 1) - r);
            if (area.isInside(pos) && fits(level, type, pos, player)) {
                return pos;
            }
        }
        return null;
    }

    private static @Nullable BlockPos anywhere(ServerLevel level, EntityType<?> type, BoundingBox box, ServerPlayer player,
                                               RandomSource random) {
        for (int tries = 0; tries < 400; tries++) {
            BlockPos pos = new BlockPos(box.minX() + random.nextInt(box.getXSpan()), box.minY() + random.nextInt(box.getYSpan()),
                    box.minZ() + random.nextInt(box.getZSpan()));
            if (level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null && fits(level, type, pos, player)) {
                return pos;
            }
        }
        return null;
    }

    /** The guard fits there (free space; water, floor or air as it needs) and is not right next to the player. */
    private static boolean fits(ServerLevel level, EntityType<?> type, BlockPos pos, ServerPlayer player) {
        if (pos.distToCenterSqr(player.position()) < 6 * 6) {
            return false;
        }
        AABB aabb = type.getSpawnAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        if (!level.noCollision(aabb)) {
            return false;
        }
        boolean water = level.getFluidState(pos).is(FluidTags.WATER);
        boolean floor = level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
        return switch (medium(type)) {
            case WATER -> water && level.getFluidState(pos.above()).is(FluidTags.WATER);
            case BOTH -> water || floor;
            case LAND -> floor && !water && level.getFluidState(pos.above()).isEmpty();
            case AIR -> !water;
        };
    }

    private boolean summon(ServerLevel level, EntityType<?> type, Guard guard, BlockPos pos, RandomSource random) {
        Entity entity = type.create(level, EntitySpawnReason.EVENT);
        if (!(entity instanceof Mob mob)) {
            return false;
        }
        mob.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.EVENT, null);
        if (mob instanceof ScavengerEntity scavenger && guard.kit() != null) {
            scavenger.equip(guard.kit(), random);
        }
        if (mob instanceof ReefGuardianEntity guardian && "warden".equals(guard.elite())) {
            guardian.makeWarden();
        }
        mob.setPersistenceRequired();
        mob.addTag(GUARD_TAG);
        level.addFreshEntityWithPassengers(mob);
        level.sendParticles(mob.isInWater() ? ParticleTypes.BUBBLE_POP : ParticleTypes.POOF,
                mob.getX(), mob.getY() + mob.getBbHeight() * 0.5, mob.getZ(), 12, 0.3, 0.5, 0.3, 0.02);
        return true;
    }
}
