package net.kasax.raft.content.diving.mixin;

import net.kasax.raft.content.diving.DivingEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Diving helmet + oxygen tank: under water a player breathes from the tank instead of losing air. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityAirMixin {
    @Inject(method = "decreaseAirSupply", at = @At("HEAD"), cancellable = true)
    private void raft$breatheFromTank(int currentSupply, CallbackInfoReturnable<Integer> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof Player && DivingEvents.breatheFromTank(self)) {
            cir.setReturnValue(Math.min(currentSupply + 4, self.getMaxAirSupply()));
        }
    }
}
