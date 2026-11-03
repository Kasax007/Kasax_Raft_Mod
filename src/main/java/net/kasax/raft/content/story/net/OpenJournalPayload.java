package net.kasax.raft.content.story.net;

import net.kasax.raft.Raft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server -> client: open the Expedition Journal screen.
 *
 * @param doneMask  bit i = chapter i of {@code StoryProgress.CHAPTERS} done
 * @param sigils    bit 0..3 = the player carries the coral, pearl, storm, ember sigil (or has used them)
 * @param mapWait   ticks until the journal can draw the next chart (0 = ready)
 * @param onSea     the player is in the raft dimension (charts only work there)
 */
public record OpenJournalPayload(int doneMask, int sigils, int mapWait, boolean onSea) implements CustomPacketPayload {
    public static final Type<OpenJournalPayload> TYPE = new Type<>(Raft.id("open_journal"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenJournalPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OpenJournalPayload::doneMask,
            ByteBufCodecs.VAR_INT, OpenJournalPayload::sigils,
            ByteBufCodecs.VAR_INT, OpenJournalPayload::mapWait,
            ByteBufCodecs.BOOL, OpenJournalPayload::onSea,
            OpenJournalPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
