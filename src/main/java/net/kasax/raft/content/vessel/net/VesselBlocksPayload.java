package net.kasax.raft.content.vessel.net;

import net.kasax.raft.Raft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server -> client: blocks of a sailing raft changed (grown crops, a lit furnace, a placed or broken block). Packed
 * positions in the raft's frame (see VesselStructure.pack) with their new block state ids (0 = air: removed).
 */
public record VesselBlocksPayload(int entityId, int[] positions, int[] states) implements CustomPacketPayload {
    public static final Type<VesselBlocksPayload> TYPE = new Type<>(Raft.id("vessel_blocks"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VesselBlocksPayload> CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.entityId());
                buf.writeVarIntArray(p.positions());
                buf.writeVarIntArray(p.states());
            },
            buf -> new VesselBlocksPayload(buf.readVarInt(), buf.readVarIntArray(65536), buf.readVarIntArray(65536)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
