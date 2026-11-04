package net.kasax.raft.content.npc.net;

import net.kasax.raft.Raft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client -> server: the player chose something in the conversation screen.
 *
 * @param entityId the NPC
 * @param action   {@link #TRADE} (open the trade screen) or {@link #BYE} (the conversation is over)
 */
public record NpcActionPayload(int entityId, int action) implements CustomPacketPayload {
    public static final int TRADE = 0;
    public static final int BYE = 1;
    public static final Type<NpcActionPayload> TYPE = new Type<>(Raft.id("npc_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NpcActionPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, NpcActionPayload::entityId,
            ByteBufCodecs.VAR_INT, NpcActionPayload::action,
            NpcActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
