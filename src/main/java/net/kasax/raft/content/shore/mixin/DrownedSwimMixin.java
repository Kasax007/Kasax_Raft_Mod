package net.kasax.raft.content.shore.mixin;

import net.kasax.raft.content.shore.entity.DrownedCorsairEntity;
import net.minecraft.world.entity.monster.Drowned;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A drowned corsair also swims after prey on rafts, boats and piers, and up to prey on the shore (1.21.1 port). */
@Mixin(Drowned.class)
public abstract class DrownedSwimMixin {
    @Inject(method = "wantsToSwim", at = @At("RETURN"), cancellable = true)
    private void raft$corsairSwims(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && (Object) this instanceof DrownedCorsairEntity corsair && corsair.wantsToSwimExtra()) {
            cir.setReturnValue(true);
        }
    }
}
