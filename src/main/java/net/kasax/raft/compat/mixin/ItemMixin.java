package net.kasax.raft.compat.mixin;

import net.kasax.raft.compat.ItemProps;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 1.21.1 port helper: registers fuel and compost values given with {@link ItemProps} when the item is built. */
@Mixin(Item.class)
public abstract class ItemMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void raft$applyProps(Item.Properties properties, CallbackInfo ci) {
        ItemProps.apply((Item) (Object) this, properties);
    }
}
