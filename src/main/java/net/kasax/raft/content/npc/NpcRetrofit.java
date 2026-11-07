package net.kasax.raft.content.npc;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.kasax.raft.Raft;
import net.kasax.raft.content.npc.entity.NpcEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * People for structures that were generated before the npc module existed (worlds from Beach Craft 3.0).
 *
 * <p>New structures get their person from the template (tools/content/npc/place.py). For every structure start that
 * loads, this class works out where the template puts its person; once that spot and the chunks around it have their
 * entities loaded it looks for the person nearby. A structure that already has its person (every structure generated
 * with this module) is only marked as done; a structure without one gets the person spawned at the spot. The mark is a
 * chunk attachment ({@link NpcContent#SPOTS_DONE}), so this happens once per structure: a person removed later by a
 * creative player stays removed.
 */
public final class NpcRetrofit {
    /** Template-local spots, the same as in tools/content/npc/place.py (keep both in sync). */
    private record Spot(String template, NpcCharacter npc, double x, double y, double z, float yaw) {
    }

    private static final List<Spot> SPOTS = List.of(
            new Spot("raft:old_lighthouse/compound_a", NpcCharacter.KEEPER_FENN, 12.5, 2.0, 20.5, 180.0F),
            new Spot("raft:old_lighthouse/compound_b", NpcCharacter.KEEPER_FENN, 12.5, 2.0, 20.5, 180.0F),
            new Spot("raft:saltmere_village/elder", NpcCharacter.NALU, 7.5, 31.0, 6.5, 180.0F),
            new Spot("raft:saltmere_village/docks", NpcCharacter.OLD_TAMO, 5.5, 30.0, 16.5, -90.0F),
            new Spot("raft:ashen_forge/hall", NpcCharacter.DR_VOSS, 4.5, 5.0, 24.5, 90.0F),
            new Spot("raft:beach_shack/tiki_bar", NpcCharacter.TEO, 9.5, 2.0, 16.5, 180.0F),
            new Spot("raft:castaway_camp/tent", NpcCharacter.KAI, 3.5, 1.0, 9.5, -90.0F),
            new Spot("raft:castaway_camp/lean_to", NpcCharacter.KAI, 3.5, 1.0, 7.5, -90.0F),
            new Spot("raft:castaway_camp/boat_shelter", NpcCharacter.KAI, 7.5, 1.0, 8.5, 0.0F),
            new Spot("raft:iceberg_outpost/outpost_0", NpcCharacter.DR_LUND, 21.5, 21.0, 18.5, 180.0F),
            new Spot("raft:rusted_rig/rig_0", NpcCharacter.OKAFOR, 34.5, 91.0, 12.5, 180.0F));

    private record Pending(String key, NpcCharacter npc, Vec3 at, float yaw) {
    }

    private static final Map<ResourceKey<Level>, Map<String, Pending>> PENDING = new HashMap<>();

    private NpcRetrofit() {
    }

    static void register() {
        ServerChunkEvents.CHUNK_LOAD.register((level, chunk) -> onChunkLoad(level, chunk));
        ServerTickEvents.END_WORLD_TICK.register(level -> {
            if (level.getGameTime() % 40L == 0L) {
                process(level);
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> PENDING.clear());
    }

    private static void onChunkLoad(ServerLevel level, LevelChunk chunk) {
        Map<?, StructureStart> starts = chunk.getAllStarts();
        if (starts.isEmpty()) {
            return;
        }
        for (StructureStart start : starts.values()) {
            if (!start.isValid()) {
                continue;
            }
            for (StructurePiece piece : start.getPieces()) {
                if (!(piece instanceof PoolElementStructurePiece p) || !(p.getElement() instanceof SinglePoolElement element)) {
                    continue;
                }
                ResourceLocation template;
                try {
                    template = ((net.kasax.raft.compat.mixin.SinglePoolElementTemplateAccess) element).raft$template().left().orElseThrow();
                } catch (RuntimeException e) {
                    continue;                                   // an inline template, not one of ours
                }
                for (Spot s : SPOTS) {
                    if (!s.template().equals(template.toString())) {
                        continue;
                    }
                    Rotation rotation = p.getRotation();
                    Vec3 local = StructureTemplate.transform(new Vec3(s.x(), s.y(), s.z()), Mirror.NONE, rotation, BlockPos.ZERO);
                    BlockPos origin = p.getPosition();
                    Vec3 at = local.add(origin.getX(), origin.getY(), origin.getZ());
                    BlockPos pos = BlockPos.containing(at);
                    String key = s.npc().id + "@" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
                    float yaw = s.yaw() + switch (rotation) {
                        case CLOCKWISE_90 -> 90.0F;
                        case CLOCKWISE_180 -> 180.0F;
                        case COUNTERCLOCKWISE_90 -> 270.0F;
                        default -> 0.0F;
                    };
                    PENDING.computeIfAbsent(level.dimension(), k -> new HashMap<>()).putIfAbsent(key, new Pending(key, s.npc(), at, yaw));
                }
            }
        }
    }

    private static void process(ServerLevel level) {
        Map<String, Pending> pending = PENDING.get(level.dimension());
        if (pending == null || pending.isEmpty()) {
            return;
        }
        for (Iterator<Pending> it = pending.values().iterator(); it.hasNext(); ) {
            Pending p = it.next();
            BlockPos pos = BlockPos.containing(p.at());
            ChunkPos cp = new ChunkPos(pos);
            if (!entitiesLoadedAround(level, cp)) {
                continue;                                       // decide only when everything around is loaded
            }
            LevelChunk chunk = level.getChunkSource().getChunkNow(cp.x, cp.z);
            if (chunk == null) {
                continue;
            }
            it.remove();
            List<String> done = chunk.getAttachedOrElse(NpcContent.SPOTS_DONE, List.of());
            if (done.contains(p.key())) {
                continue;
            }
            EntityType<NpcEntity> type = NpcContent.TYPES.get(p.npc());
            if (level.getEntities(type, new AABB(pos).inflate(24.0), e -> true).isEmpty()) {
                spawn(level, type, p);
            }
            List<String> marked = new ArrayList<>(done);
            marked.add(p.key());
            chunk.setAttached(NpcContent.SPOTS_DONE, marked);
            chunk.setUnsaved(true);
        }
    }

    private static boolean entitiesLoadedAround(ServerLevel level, ChunkPos cp) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (!level.areEntitiesLoaded(ChunkPos.asLong(cp.x + dx, cp.z + dz))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void spawn(ServerLevel level, EntityType<NpcEntity> type, Pending p) {
        NpcEntity npc = type.create(level);
        if (npc == null) {
            return;
        }
        npc.moveTo(p.at().x, p.at().y, p.at().z, p.yaw(), 0.0F);
        // the place may have changed since it was built (the player built or dug there): stand on top
        for (int i = 0; i < 4 && !level.noCollision(npc); i++) {
            npc.setPos(npc.getX(), npc.getY() + 1.0, npc.getZ());
        }
        if (!level.noCollision(npc)) {
            Raft.LOGGER.info("Beach Craft: no room for {} at {}", p.npc().id, BlockPos.containing(p.at()));
            return;
        }
        npc.setYBodyRot(p.yaw());
        npc.setYHeadRot(p.yaw());
        npc.finalizeSpawn(level, level.getCurrentDifficultyAt(npc.blockPosition()), MobSpawnType.STRUCTURE, null);
        level.addFreshEntity(npc);
        Raft.LOGGER.info("Beach Craft: {} moved into an older structure at {}", p.npc().id, npc.blockPosition());
    }
}
