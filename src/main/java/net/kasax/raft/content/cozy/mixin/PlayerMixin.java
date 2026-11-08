package net.kasax.raft.content.cozy.mixin;

import net.kasax.raft.content.cozy.CozyContent;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Rested players get hungry 25 % slower. */
@Mixin(Player.class)
public abstract class PlayerMixin {
    @ModifyVariable(method = "causeFoodExhaustion", at = @At("HEAD"), argsOnly = true)
    private float raft$restedExhaustion(float amount) {
        return ((Player) (Object) this).hasEffect(CozyContent.RESTED) ? amount * 0.75F : amount;
    }
}
