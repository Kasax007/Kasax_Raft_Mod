package net.kasax.raft.content.gear.mixin;

import net.kasax.raft.content.gear.item.DriftwoodFishingRodItem;
import net.minecraft.client.renderer.entity.FishingHookRenderer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 1.21.1 port: the fishing line starts at the hand that holds a rod. 1.21.1 only checks for the vanilla rod (the
 * line of a Driftwood Fishing Rod in the main hand would start at the other hand); the newer game checks for any rod.
 */
@Mixin(FishingHookRenderer.class)
public abstract class FishingHookRendererMixin {
    @Redirect(method = "getPlayerHandPos", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"))
    private boolean raft$anyRod(ItemStack stack, Item item) {
        return stack.is(item) || (item == Items.FISHING_ROD && stack.getItem() instanceof DriftwoodFishingRodItem);
    }
}
