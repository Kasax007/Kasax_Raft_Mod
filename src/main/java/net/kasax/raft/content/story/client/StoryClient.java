package net.kasax.raft.content.story.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.kasax.raft.content.story.item.ExpeditionJournalItem;
import net.kasax.raft.content.story.net.OpenJournalPayload;
import net.kasax.raft.content.story.net.OpenLogPayload;
import net.minecraft.client.Minecraft;

/** Client-only init of the "story" module: the journal and terminal screens, the journal's chart timer. */
public final class StoryClient {
    private StoryClient() {
    }

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(OpenJournalPayload.TYPE, (payload, context) -> {
            Minecraft mc = context.client();
            mc.execute(() -> mc.setScreen(new JournalScreen(payload)));
        });
        ClientPlayNetworking.registerGlobalReceiver(OpenLogPayload.TYPE, (payload, context) -> {
            Minecraft mc = context.client();
            mc.execute(() -> mc.setScreen(new TerminalScreen(payload.logId())));
        });
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.level != null) {
                ExpeditionJournalItem.clientGameTime = mc.level.getGameTime();
            }
        });
    }
}
