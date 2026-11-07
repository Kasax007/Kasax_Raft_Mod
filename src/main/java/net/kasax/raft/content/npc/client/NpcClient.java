package net.kasax.raft.content.npc.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.kasax.raft.content.npc.NpcCharacter;
import net.kasax.raft.content.npc.NpcContent;
import net.kasax.raft.content.npc.net.NpcDialogPayload;
import net.minecraft.client.Minecraft;

/** Client-only init of the npc module: renderers and the conversation screen. */
public final class NpcClient {
    private NpcClient() {
    }

    public static void init() {
        NpcAccessories.register();
        for (NpcCharacter c : NpcCharacter.values()) {
            EntityRendererRegistry.register(NpcContent.TYPES.get(c), context -> new NpcRenderer(context, c));
        }
        ClientPlayNetworking.registerGlobalReceiver(NpcDialogPayload.TYPE, (payload, context) -> {
            Minecraft mc = context.client();
            mc.execute(() -> mc.setScreen(new NpcDialogScreen(payload)));
        });
    }
}
