package net.kasax.raft.gametest.mixin;

import net.kasax.raft.gametest.BcScript;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A detached film camera (BcScript.freeCam): the camera leaves the player, who is drawn like in third person. */
@Mixin(Camera.class)
public abstract class FreeCamMixin {
    @Shadow
    private boolean detached;

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Inject(method = "alignWithEntity", at = @At("TAIL"))
    private void bctest$freeCam(float partialTicks, CallbackInfo ci) {
        double[] f = BcScript.freeCam;
        if (f != null) {
            this.setRotation((float) f[3], (float) f[4]);
            this.setPosition(f[0], f[1], f[2]);
            this.detached = true;
        }
    }
}
