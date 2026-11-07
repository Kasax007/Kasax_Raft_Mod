package net.kasax.raft.compat.mixin;

import net.kasax.raft.compat.ItemProps;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 1.21.1 port helper: dropped story relics ignore the damage types of their tag (fire, explosions, lightning, cactus). */
@Mixin(ItemEntity.class)
public abstract class ItemEntityResistMixin {
    @Shadow
    public abstract ItemStack getItem();

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void raft$resist(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        var tag = ItemProps.DAMAGE_RESISTANT.get(this.getItem().getItem());
        if (tag != null && source.is(tag)) cir.setReturnValue(false);
    }
}
