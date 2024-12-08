package net.kasax.raft.util;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.kasax.raft.Raft;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record EnergySyncPayload(BlockPos pos, long energy, long maxEnergy) implements CustomPayload {
    public static final CustomPayload.Id<EnergySyncPayload> ID = new CustomPayload.Id<>(Raft.id("energy_sync"));
    public static final PacketCodec<RegistryByteBuf, EnergySyncPayload> CODEC = PacketCodec.tuple(
            BlockPos.PACKET_CODEC, EnergySyncPayload::pos,
            PacketCodecs.VAR_LONG, EnergySyncPayload::energy,
            PacketCodecs.VAR_LONG, EnergySyncPayload::maxEnergy,
            EnergySyncPayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
