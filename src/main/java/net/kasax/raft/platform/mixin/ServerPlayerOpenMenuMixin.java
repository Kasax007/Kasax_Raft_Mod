package net.kasax.raft.platform.mixin;

import java.util.OptionalInt;
import net.kasax.raft.platform.ExtendedMenuProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Menus of an extended factory carry their opening data (Fabric does this in the same place). */
@Mixin(ServerPlayer.class)
abstract class ServerPlayerOpenMenuMixin {
    @Inject(method = "openMenu(Lnet/minecraft/world/MenuProvider;)Ljava/util/OptionalInt;", at = @At("HEAD"), cancellable = true)
    private void raft$openExtended(MenuProvider provider, CallbackInfoReturnable<OptionalInt> cir) {
        if (provider instanceof ExtendedMenuProvider<?> factory) {
            cir.setReturnValue(ExtendedMenuProvider.open((ServerPlayer) (Object) this, factory));
        }
    }
}
