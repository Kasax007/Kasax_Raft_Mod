package net.kasax.raft.gametest.mixin;

import net.kasax.raft.gametest.BcScript;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * While BcScript.holdWorld is set (a recorded frame is read back from the GPU, during which the client game test
 * framework lets the game run a tick or two), the world stands still like under /tick freeze, on the server and the
 * client alike (same JVM): no world time, no entity ticks, also not for vehicles a player rides. Players are held by
 * HoldPlayerMixin. Without this every recorded frame cost two game ticks and a 30 fps clip ran three times too fast.
 */
@Mixin(TickRateManager.class)
public class HoldWorldMixin {
    @Inject(method = "runsNormally", at = @At("HEAD"), cancellable = true)
    private void bctest$hold(CallbackInfoReturnable<Boolean> cir) {
        if (BcScript.holdWorld) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isEntityFrozen", at = @At("HEAD"), cancellable = true)
    private void bctest$holdEntity(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (BcScript.holdWorld) {
            cir.setReturnValue(!(entity instanceof Player));
        }
    }
}
