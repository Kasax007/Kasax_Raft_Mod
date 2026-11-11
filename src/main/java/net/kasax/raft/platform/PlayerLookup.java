package net.kasax.raft.platform;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;

/** Fabric's PlayerLookup: the players that track an entity (its tracker's list) or see a chunk. */
public final class PlayerLookup {
    private PlayerLookup() {
    }

    public static Collection<ServerPlayer> tracking(Entity entity) {
        if (!(entity.level() instanceof ServerLevel level)) {
            throw new IllegalArgumentException("Only supported on server worlds!");
        }
        ChunkMap.TrackedEntity tracked = level.getChunkSource().chunkMap.entityMap.get(entity.getId());
        if (tracked == null) {
            return List.of();
        }
        List<ServerPlayer> players = new ArrayList<>(tracked.seenBy.size());
        for (ServerPlayerConnection connection : tracked.seenBy) {
            players.add(connection.getPlayer());
        }
        return players;
    }

    public static Collection<ServerPlayer> tracking(ServerLevel level, BlockPos pos) {
        return level.getChunkSource().chunkMap.getPlayers(new ChunkPos(pos), false);
    }

    public static Collection<ServerPlayer> tracking(ServerLevel level, ChunkPos pos) {
        return level.getChunkSource().chunkMap.getPlayers(pos, false);
    }
}
