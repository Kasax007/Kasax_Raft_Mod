package net.kasax.raft.content.story.net;

import net.kasax.raft.Raft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client: show the Expedition log {@code terminal.raft.<logId>.title/.text} on the terminal screen. */
public record OpenLogPayload(String logId) implements CustomPacketPayload {
    public static final Type<OpenLogPayload> TYPE = new Type<>(Raft.id("open_log"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenLogPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64), OpenLogPayload::logId, OpenLogPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
