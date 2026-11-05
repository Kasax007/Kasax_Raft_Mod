package net.kasax.raft.content.vessel.mixin.client;

import net.kasax.raft.content.vessel.client.VesselClient;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A click (attack) on a sailing raft: in creative mode it breaks the block under the crosshair at once. */
@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeAttackMixin {
    @Inject(method = "attack", at = @At("HEAD"))
    private void raft$attackRaft(Player player, Entity entity, CallbackInfo ci) {
        VesselClient.attackedRaft(entity);
    }
}
