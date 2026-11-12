package net.kasax.raft.platform;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;

/** Fabric's ServerChunkEvents.CHUNK_LOAD on NeoForge's {@link ChunkEvent.Load} (server levels only). */
public final class ServerChunkEvents {
    public static final Event<Load> CHUNK_LOAD = new Event<>(() -> NeoForge.EVENT_BUS.addListener(ChunkEvent.Load.class, e -> {
        if (e.getLevel() instanceof ServerLevel level && e.getChunk() instanceof LevelChunk chunk) {
            for (Load l : ServerChunkEvents.CHUNK_LOAD.listeners()) l.onChunkLoad(level, chunk, e.isNewChunk());
        }
    }));

    private ServerChunkEvents() {
    }

    @FunctionalInterface
    public interface Load {
        void onChunkLoad(ServerLevel level, LevelChunk chunk, boolean generated);
    }
}
