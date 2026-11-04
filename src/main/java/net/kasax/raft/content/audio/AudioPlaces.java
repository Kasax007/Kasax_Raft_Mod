package net.kasax.raft.content.audio;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.kasax.raft.Raft;
import net.kasax.raft.content.audio.net.PlacePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tells each client which key place its player is in, for the place music: once a second the server checks the
 * structures with their own theme (inside the bounding box of the structure, or up to 16 blocks above it, so sailing
 * over the drowned city counts) and sends {@link PlacePayload} when the place changes.
 */
public final class AudioPlaces {
    public static final String PORT_SOLACE = "port_solace";
    public static final String SALTMERE = "saltmere_village";
    public static final String SANCTUM = "abyssal_sanctum";
    public static final String CORAL_TEMPLE = "coral_temple";
    private static final List<String> PLACES = List.of(SANCTUM, PORT_SOLACE, SALTMERE, CORAL_TEMPLE);
    private static final Map<UUID, String> SENT = new HashMap<>();

    private AudioPlaces() {
    }

    static void register() {
        ServerTickEvents.END_SERVER_TICK.register(AudioPlaces::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> SENT.remove(handler.player.getUUID()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> SENT.remove(handler.player.getUUID()));
    }

    private static void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 != 7) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            String place = placeOf(player);
            if (!place.equals(SENT.get(player.getUUID())) && ServerPlayNetworking.canSend(player, PlacePayload.TYPE)) {
                SENT.put(player.getUUID(), place);
                ServerPlayNetworking.send(player, new PlacePayload(place));
            }
        }
    }

    /** The key place at the player ("" if none). */
    public static String placeOf(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        var registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        BlockPos pos = player.blockPosition();
        for (String id : PLACES) {
            Structure structure = registry.getValue(Raft.id(id));
            if (structure == null) {
                continue;
            }
            if (level.structureManager().getStructureAt(pos, structure).isValid()
                    || level.structureManager().getStructureAt(pos.below(16), structure).isValid()) {
                return id;
            }
        }
        return "";
    }
}
