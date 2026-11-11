package net.kasax.raft.util;

import net.kasax.raft.platform.client.ClientPlayNetworking;
import net.kasax.raft.screen.MakeshiftBatteryScreenHandler;
import net.minecraft.client.Minecraft;

public class ModPackets {
    public static void registerC2SPackets() {
        // Register client-to-server packets if needed
    }

    public static void registerS2CPackets() {
        ClientPlayNetworking.registerGlobalReceiver(EnergySyncPayload.ID, (payload, context) -> {
            Minecraft client = context.client();
            client.execute(() -> {
                // Ensure we have the client player and a screen handler open
                if (client.player != null && client.player.containerMenu instanceof MakeshiftBatteryScreenHandler screenHandler) {
                    // Only update if the block position matches
                    if (screenHandler.getBlockEntity().getBlockPos().equals(payload.pos())) {
                        screenHandler.setEnergy(payload.energy());
                        screenHandler.setMaxEnergy(payload.maxEnergy());
                    }
                }
            });
        });
    }
}
