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

    /** One question and its answers (asked again, the next answer is shown). */
    public record Topic(Component label, List<Component> replies) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Topic> CODEC = StreamCodec.composite(
                ComponentSerialization.TRUSTED_STREAM_CODEC, Topic::label,
                ComponentSerialization.TRUSTED_STREAM_CODEC.apply(ByteBufCodecs.list(8)), Topic::replies,
                Topic::new);
    }

    private static final StreamCodec<RegistryFriendlyByteBuf, List<Topic>> TOPICS = Topic.CODEC.apply(ByteBufCodecs.list(8));
    public static final StreamCodec<RegistryFriendlyByteBuf, NpcDialogPayload> CODEC = StreamCodec.of(
            (buf, p) -> {
                ByteBufCodecs.VAR_INT.encode(buf, p.entityId());
                ByteBufCodecs.STRING_UTF8.encode(buf, p.npc());
                ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, p.name());
                ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, p.greeting());
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, p.gift());
                TOPICS.encode(buf, p.topics());
                ByteBufCodecs.BOOL.encode(buf, p.trade());
            },
            buf -> new NpcDialogPayload(ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf),
                    ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buf), ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buf),
                    ItemStack.OPTIONAL_STREAM_CODEC.decode(buf), TOPICS.decode(buf), ByteBufCodecs.BOOL.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
