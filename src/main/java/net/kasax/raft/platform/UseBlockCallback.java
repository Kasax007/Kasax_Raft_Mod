package net.kasax.raft.platform;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Fabric's UseBlockCallback on NeoForge's {@link PlayerInteractEvent.RightClickBlock} (both sides, like Fabric). */
@FunctionalInterface
public interface UseBlockCallback {
    Event<UseBlockCallback> EVENT = new Event<>(() -> NeoForge.EVENT_BUS.addListener(PlayerInteractEvent.RightClickBlock.class, e -> {
        for (UseBlockCallback l : UseBlockCallback.EVENT.listeners()) {
            InteractionResult result = l.interact(e.getEntity(), e.getLevel(), e.getHand(), e.getHitVec());
            if (result != InteractionResult.PASS) {
                e.setCanceled(true);
                e.setCancellationResult(result);
                return;
            }
        }
    }));

    InteractionResult interact(Player player, Level world, InteractionHand hand, BlockHitResult hitResult);
}
