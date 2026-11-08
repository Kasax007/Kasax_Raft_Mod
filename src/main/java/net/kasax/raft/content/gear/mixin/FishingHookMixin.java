package net.kasax.raft.content.gear.mixin;

import net.kasax.raft.content.gear.GearContent;
import net.kasax.raft.content.gear.item.DriftwoodFishingRodItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Bait for plain fishing rods: casting a rod with fish bait or a golden lure in the off hand or inventory uses one
 * of them. Fish bait works like one level of Lure (5 seconds faster bites), the golden lure like Lure I plus Luck of
 * the Sea II. The Driftwood Fishing Rod uses only the bait loaded into it (DriftwoodFishingRodItem), so a cast with it
 * takes nothing from the inventory; and the hook stays out while the player holds any fishing rod, not only the
 * vanilla one.
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
        if (castsDriftwoodRod(player)) return;
        ItemStack bait = find(player);
        if (bait.isEmpty()) return;
        boolean golden = bait.is(GearContent.GOLDEN_LURE);
        this.lureSpeed += 100;
        if (golden) this.luck += 2;
        if (!player.hasInfiniteMaterials()) bait.shrink(1);
    }

    @Redirect(method = "shouldStopFishing", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;is(Ljava/lang/Object;)Z"))
    private boolean raft$anyRod(ItemStack stack, Object item) {
        // ItemStack.is(Item) is the generic TypedInstance.is(T), erased to is(Object)
        return stack.is((Item) item) || (item == Items.FISHING_ROD && stack.getItem() instanceof DriftwoodFishingRodItem);
    }

    /** The rod that casts is the one in the main hand if that is a rod, else the one in the off hand. */
    private static boolean castsDriftwoodRod(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof FishingRodItem) return main.getItem() instanceof DriftwoodFishingRodItem;
        return player.getOffhandItem().getItem() instanceof DriftwoodFishingRodItem;
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
