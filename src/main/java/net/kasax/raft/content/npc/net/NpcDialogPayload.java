package net.kasax.raft.content.npc.net;

import net.kasax.raft.Raft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Server -> client: open the conversation with a person of the sea.
 *
 * @param entityId the NPC (for its portrait; the conversation ends when it is gone or far away)
 * @param npc      character id ({@code keeper_fenn}, ...): title key {@code npc.raft.<npc>.title}
 * @param name     the NPC's display name
 * @param greeting what the NPC says first (depends on the player's story progress)
 * @param gift     what the NPC just handed over (empty: nothing)
 * @param topics   the questions the player can ask, with the answers
 * @param trade    the NPC trades (shows the trade option)
 */
public record NpcDialogPayload(int entityId, String npc, Component name, Component greeting, ItemStack gift,
                               List<Topic> topics, boolean trade) implements CustomPacketPayload {
    public static final Type<NpcDialogPayload> TYPE = new Type<>(Raft.id("npc_dialog"));

    /** One question and its answer. */
    public record Topic(Component label, Component reply) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Topic> CODEC = StreamCodec.composite(
                ComponentSerialization.TRUSTED_STREAM_CODEC, Topic::label,
                ComponentSerialization.TRUSTED_STREAM_CODEC, Topic::reply,
                Topic::new);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, NpcDialogPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, NpcDialogPayload::entityId,
            ByteBufCodecs.STRING_UTF8, NpcDialogPayload::npc,
            ComponentSerialization.TRUSTED_STREAM_CODEC, NpcDialogPayload::name,
            ComponentSerialization.TRUSTED_STREAM_CODEC, NpcDialogPayload::greeting,
            ItemStack.OPTIONAL_STREAM_CODEC, NpcDialogPayload::gift,
            Topic.CODEC.apply(ByteBufCodecs.list(8)), NpcDialogPayload::topics,
            ByteBufCodecs.BOOL, NpcDialogPayload::trade,
            NpcDialogPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
