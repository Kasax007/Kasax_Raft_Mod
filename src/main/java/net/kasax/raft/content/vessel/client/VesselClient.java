package net.kasax.raft.content.vessel.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.kasax.raft.content.vessel.VesselColliders;
import net.kasax.raft.content.vessel.VesselContent;
import net.kasax.raft.content.vessel.VesselEntity;
import net.kasax.raft.content.vessel.net.VesselDataPayload;
import net.kasax.raft.content.vessel.net.VesselUsePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;

/** Client-only init of the vessel module: the raft renderer, the block payload and the use key on raft blocks. */
@Environment(EnvType.CLIENT)
public final class VesselClient {
    /** Set when a use was sent; holding the use key does not repeat it (no flickering in and out of the helm). */
    private static boolean useHeld;

    private VesselClient() {
    }

    public static void init() {
        EntityRendererRegistry.register(VesselContent.VESSEL, VesselRenderer::new);
        ClientPlayNetworking.registerGlobalReceiver(VesselDataPayload.TYPE, (payload, context) -> {
            Minecraft mc = context.client();
            mc.execute(() -> {
                Entity entity = mc.level == null ? null : mc.level.getEntity(payload.entityId());
                if (entity instanceof VesselEntity vessel) {
                    vessel.setClientStructure(payload.structure());
                } else {
                    VesselDataPayload.putPending(payload.entityId(), payload.structure());
                }
            });
        });
        VesselEntity.clientUse = VesselClient::use;
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (useHeld && !mc.options.keyUse.isDown()) {
                useHeld = false;
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> {
            VesselColliders.clear(true);
            VesselDataPayload.clearPending();
        });
    }

    private static void use(VesselEntity vessel, InteractionHand hand) {
        BlockPos local = VesselPicker.targetOf(vessel);
        if (local == null || useHeld) {
            return;
        }
        useHeld = true;
        ClientPlayNetworking.send(new VesselUsePayload(vessel.getId(), local, hand == InteractionHand.OFF_HAND));
    }
}
