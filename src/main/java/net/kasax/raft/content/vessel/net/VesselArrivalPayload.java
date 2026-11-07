package net.kasax.raft.content.vessel.net;

import net.kasax.raft.Raft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

/**
 * Server -> client: the player has just been put onto the deck of a sailing raft (teleport, login, dimension change),
 * at {@code local} in the raft's own frame. The client places its player there on its own copy of the raft, and waits
 * in the air until that copy has arrived (it comes with the raft entity, a moment after the teleport).
 */
public record VesselArrivalPayload(int entityId, Vec3 local) implements CustomPacketPayload {
    public static final Type<VesselArrivalPayload> TYPE = new Type<>(Raft.id("vessel_arrival"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VesselArrivalPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, VesselArrivalPayload::entityId,
            net.kasax.raft.compat.Codecs.VEC3, VesselArrivalPayload::local,
            VesselArrivalPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
