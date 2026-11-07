package net.kasax.raft.content.gear.mixin;

import net.kasax.raft.content.gear.GearContent;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The captain's spyglass zooms twice as far as the vanilla spyglass (field of view x0.05 instead of x0.1). */
@Mixin(AbstractClientPlayer.class)
public abstract class ClientPlayerZoomMixin {
    @Inject(method = "getFieldOfViewModifier", at = @At("RETURN"), cancellable = true)
    private void raft$captainsZoom(boolean firstPerson, float effectScale, CallbackInfoReturnable<Float> cir) {
        AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;
        if (firstPerson && self.isScoping() && self.getUseItem().is(GearContent.CAPTAINS_SPYGLASS)) {
            cir.setReturnValue(0.05F);
        }
    }
}
