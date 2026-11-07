package net.kasax.raft.content.gear.mixin;

import net.kasax.raft.content.gear.GearContent;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Looking through the captain's spyglass counts as scoping (overlay, slow turning), like the vanilla spyglass. */
@Mixin(Player.class)
public abstract class PlayerScopingMixin {
    @Inject(method = "isScoping", at = @At("RETURN"), cancellable = true)
    private void raft$captainsSpyglass(CallbackInfoReturnable<Boolean> cir) {
        Player self = (Player) (Object) this;
        if (!cir.getReturnValueZ() && self.isUsingItem() && self.getUseItem().is(GearContent.CAPTAINS_SPYGLASS)) {
            cir.setReturnValue(true);
        }
    }
}
