package net.kasax.raft.content.vessel.net;

import net.kasax.raft.Raft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server -> client: using a raft block did nothing (no block action, no item use on it). As in vanilla, the client
 * then uses the held item in the air (eat, drink, throw, scoop water through the deck).
 */
public record VesselUseResultPayload(int entityId) implements CustomPacketPayload {
    public static final Type<VesselUseResultPayload> TYPE = new Type<>(Raft.id("vessel_use_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VesselUseResultPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, VesselUseResultPayload::entityId,
            VesselUseResultPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
