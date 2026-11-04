package net.kasax.raft.content.vessel.net;

import net.kasax.raft.Raft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;

/**
 * Client -> server: the player used a block of a sailing raft. {@code local} is the block in the raft's own frame,
 * as the client's ray-cast found it (exact even while the raft moves and turns).
 */
public record VesselUsePayload(int entityId, BlockPos local, boolean offHand) implements CustomPacketPayload {
    public static final Type<VesselUsePayload> TYPE = new Type<>(Raft.id("vessel_use"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VesselUsePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, VesselUsePayload::entityId,
            BlockPos.STREAM_CODEC, VesselUsePayload::local,
            ByteBufCodecs.BOOL, VesselUsePayload::offHand,
            VesselUsePayload::new);

    public InteractionHand hand() {
        return this.offHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
