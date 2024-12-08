package net.kasax.raft.util;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.kasax.raft.Raft;
import net.kasax.raft.block.entity.MakeshiftBatteryBlockEntity;
import net.kasax.raft.screen.MakeshiftBatteryScreenHandler;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class ModPackets {
    public static final Identifier ENERGY_SYNC_PACKET = Identifier.of(Raft.MOD_ID, "energy_sync");
    public static void registerC2SPackets() {
        // Register client-to-server packets if needed
    }

    public static void registerS2CPackets() {
        ClientPlayNetworking.registerGlobalReceiver(EnergySyncPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                // Ensure we have the client player and a screen handler open
                if (context.client().player != null && context.client().player.currentScreenHandler instanceof MakeshiftBatteryScreenHandler screenHandler) {
                    // Only update if the block position matches
                    if (screenHandler.getBlockEntity().getPos().equals(payload.pos())) {
                        screenHandler.setEnergy(payload.energy());
                        screenHandler.setMaxEnergy(payload.maxEnergy());
                    }
                }
            });
        });
    }
}

