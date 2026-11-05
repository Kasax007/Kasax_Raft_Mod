package net.kasax.raft.content.audio.net;

import net.kasax.raft.Raft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client: the key place the player is in now (a structure with its own music), "" for none. */
public record PlacePayload(String place) implements CustomPacketPayload {
    public static final Type<PlacePayload> TYPE = new Type<>(Raft.id("audio_place"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PlacePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64), PlacePayload::place, PlacePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
