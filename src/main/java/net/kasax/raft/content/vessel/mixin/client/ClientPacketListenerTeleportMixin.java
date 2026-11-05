package net.kasax.raft.content.vessel.mixin.client;

import net.kasax.raft.content.vessel.client.VesselClient;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The server moved the own player (teleport, login, dimension change): a wait for a raft from an earlier move ends here.
 * If this move is onto a raft too, the server's note about it comes right after this packet (VesselArrivalPayload).
 */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerTeleportMixin {
    @Inject(method = "handleMovePlayer", at = @At("TAIL"))
    private void raft$movedByServer(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
        VesselClient.movedByServer();
    }
}
