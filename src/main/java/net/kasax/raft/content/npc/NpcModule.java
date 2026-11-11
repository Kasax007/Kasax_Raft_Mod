package net.kasax.raft.content.npc;

import net.kasax.raft.platform.PayloadTypeRegistry;
import net.kasax.raft.platform.ServerPlayNetworking;
import net.kasax.raft.content.ContentModule;
import net.kasax.raft.content.npc.entity.NpcEntity;
import net.kasax.raft.content.npc.net.NpcActionPayload;
import net.kasax.raft.content.npc.net.NpcDialogPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Beach Craft module "npc": the people of the Endless Sea (docs/design/npcs.md) – the lighthouse keeper, the folk of
 * Saltmere, Dr. Voss at the forge and the survivors at the beach bar, the castaway camp, the ice floe and the rig.
 * They are placed by the structure templates (tools/content/npc/place.py), talk according to the story progress and
 * some of them trade. Resources come from tools/content/npc.
 */
public class NpcModule implements ContentModule {
    @Override
    public String name() {
        return "npc";
    }

    @Override
    public void init() {
        NpcContent.register();
        NpcRetrofit.register();
        PayloadTypeRegistry.playS2C().register(NpcDialogPayload.TYPE, NpcDialogPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(NpcActionPayload.TYPE, NpcActionPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(NpcActionPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                Entity e = player.level().getEntity(payload.entityId());
                if (!(e instanceof NpcEntity npc) || !npc.isAlive() || player.distanceToSqr(npc) > 144.0) {
                    return;
                }
                if (payload.action() == NpcActionPayload.TRADE) {
                    npc.openTrades(player);
                } else {
                    npc.stopTalking(player);
                }
            });
        });
    }

    @Override
    public void initClient() {
        net.kasax.raft.content.npc.client.NpcClient.init();
    }
}
