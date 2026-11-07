package net.kasax.raft.content.voyage.mixin;

import net.kasax.raft.content.voyage.ExpeditionRaft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The Expedition Raft decides per entity whether it can be collided with (servers do not test players against it). */
@Mixin(Entity.class)
public abstract class EntityCollideMixin {
    @Inject(method = "canCollideWith", at = @At("HEAD"), cancellable = true)
    private void raft$expeditionRaft(Entity other, CallbackInfoReturnable<Boolean> cir) {
        if (other instanceof ExpeditionRaft raft && !raft.canBeCollidedWith((Entity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
