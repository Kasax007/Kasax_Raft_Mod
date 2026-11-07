package net.kasax.raft.content.vessel.net;

import net.kasax.raft.Raft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

/**
 * Client -> server: the player used (right click) a block of a sailing raft. {@code local} is the block in the raft's
 * own frame as the client's ray-cast found it, {@code hit} the exact point (raft frame) and {@code face} the side.
 */
public record VesselUsePayload(int entityId, BlockPos local, Vec3 hit, Direction face, boolean inside) implements CustomPacketPayload {
    public static final Type<VesselUsePayload> TYPE = new Type<>(Raft.id("vessel_use"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VesselUsePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, VesselUsePayload::entityId,
            BlockPos.STREAM_CODEC, VesselUsePayload::local,
            net.kasax.raft.compat.Codecs.VEC3, VesselUsePayload::hit,
            Direction.STREAM_CODEC, VesselUsePayload::face,
            ByteBufCodecs.BOOL, VesselUsePayload::inside,
            VesselUsePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
