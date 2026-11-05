package net.kasax.raft.content.vessel.net;

import net.kasax.raft.Raft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client -> server: the player began ({@code done} false) or finished ({@code done} true) breaking a block of a sailing
 * raft ({@code local}: in the raft's frame). The client counts the progress; the server checks the time it took.
 */
public record VesselBreakPayload(int entityId, BlockPos local, boolean done) implements CustomPacketPayload {
    public static final Type<VesselBreakPayload> TYPE = new Type<>(Raft.id("vessel_break"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VesselBreakPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, VesselBreakPayload::entityId,
            BlockPos.STREAM_CODEC, VesselBreakPayload::local,
            ByteBufCodecs.BOOL, VesselBreakPayload::done,
            VesselBreakPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
