package net.kasax.raft.content.vessel.net;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.kasax.raft.Raft;
import net.kasax.raft.content.vessel.VesselStructure;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.Nullable;

/**
 * Server -> client: the blocks of a sailing raft, sent when the player starts tracking it (right after the entity
 * itself). If the entity is not there yet on the client, the blocks wait in {@link #PENDING} until it ticks.
 */
public record VesselDataPayload(int entityId, VesselStructure structure) implements CustomPacketPayload {
    public static final Type<VesselDataPayload> TYPE = new Type<>(Raft.id("vessel_data"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VesselDataPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, VesselDataPayload::entityId,
            VesselStructure.CLIENT_CODEC, VesselDataPayload::structure,
            VesselDataPayload::new);

    /** Client: blocks that arrived before their entity (by entity id). */
    private static final Int2ObjectOpenHashMap<VesselStructure> PENDING = new Int2ObjectOpenHashMap<>();

    public static synchronized void putPending(int entityId, VesselStructure structure) {
        if (PENDING.size() > 64) {
            PENDING.clear();
        }
        PENDING.put(entityId, structure);
    }

    public static synchronized @Nullable VesselStructure takePending(int entityId) {
        return PENDING.isEmpty() ? null : PENDING.remove(entityId);
    }

    public static synchronized void clearPending() {
        PENDING.clear();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
