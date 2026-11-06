package net.kasax.raft.gametest.mixin;

import net.kasax.raft.gametest.BcScript;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The player stands still too while a recorded frame is read back (see HoldWorldMixin): no movement, no animation. */
@Mixin(LocalPlayer.class)
public class HoldPlayerMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void bctest$hold(CallbackInfo ci) {
        if (BcScript.holdWorld) {
            ci.cancel();
        }
    }
}
