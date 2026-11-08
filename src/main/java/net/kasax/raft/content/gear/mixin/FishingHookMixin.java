package net.kasax.raft.content.gear.mixin;

import net.kasax.raft.content.gear.GearContent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Bait: casting a rod with fish bait or a golden lure in the off hand or inventory uses one of them. Fish bait works
 * like one level of Lure (5 seconds faster bites), the golden lure like Lure I plus Luck of the Sea II.
 */
@Mixin(FishingHook.class)
public abstract class FishingHookMixin {
    @Shadow
    @Final
    @Mutable
    private int luck;

    @Shadow
    @Final
    @Mutable
    private int lureSpeed;

    @Inject(method = "<init>(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;II)V", at = @At("RETURN"))
    private void raft$bait(Player player, Level level, int luck, int lureSpeed, CallbackInfo ci) {
        ItemStack bait = find(player);
        if (bait.isEmpty()) return;
        boolean golden = bait.is(GearContent.GOLDEN_LURE);
        this.lureSpeed += 100;
        if (golden) this.luck += 2;
        if (!player.hasInfiniteMaterials()) bait.shrink(1);
    }

    private static ItemStack find(Player player) {
        ItemStack off = player.getOffhandItem();
        if (off.is(GearContent.GOLDEN_LURE) || off.is(GearContent.FISH_BAIT)) return off;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(GearContent.GOLDEN_LURE)) return s;
        }
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(GearContent.FISH_BAIT)) return s;
        }
        return ItemStack.EMPTY;
    }
}
