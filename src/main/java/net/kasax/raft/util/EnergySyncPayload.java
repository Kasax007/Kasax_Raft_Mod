package net.kasax.raft.util;

import net.kasax.raft.Raft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record EnergySyncPayload(BlockPos pos, long energy, long maxEnergy) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<EnergySyncPayload> ID = new CustomPacketPayload.Type<>(Raft.id("energy_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EnergySyncPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, EnergySyncPayload::pos,
            ByteBufCodecs.VAR_LONG, EnergySyncPayload::energy,
            ByteBufCodecs.VAR_LONG, EnergySyncPayload::maxEnergy,
            EnergySyncPayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
