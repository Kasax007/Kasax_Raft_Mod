package net.kasax.raft.platform;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Fabric's UseItemCallback on NeoForge's {@link PlayerInteractEvent.RightClickItem} (both sides, like Fabric). */
@FunctionalInterface
public interface UseItemCallback {
    Event<UseItemCallback> EVENT = new Event<>(() -> NeoForge.EVENT_BUS.addListener(PlayerInteractEvent.RightClickItem.class, e -> {
        for (UseItemCallback l : UseItemCallback.EVENT.listeners()) {
            InteractionResultHolder<ItemStack> result = l.interact(e.getEntity(), e.getLevel(), e.getHand());
            if (result.getResult() != InteractionResult.PASS) {
                e.setCanceled(true);
                e.setCancellationResult(result.getResult());
                return;
            }
        }
    }));

    InteractionResultHolder<ItemStack> interact(Player player, Level world, InteractionHand hand);
}
