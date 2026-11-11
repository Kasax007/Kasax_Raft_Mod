package net.kasax.raft.content.guide;

import net.kasax.raft.platform.ServerEntityWorldChangeEvents;
import net.kasax.raft.platform.ServerPlayerEvents;
import net.kasax.raft.world.dimension.ModDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Gives every player one Seafarer's Almanac the first time they reach the raft dimension (spec 15). */
public final class GuideEvents {
    private GuideEvents() {
    }

    static void register() {
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> check(player));
        ServerPlayerEvents.JOIN.register(GuideEvents::check);
    }

    private static void check(ServerPlayer player) {
        if (player.isSpectator() || player.level().dimension() != ModDimensions.RAFTDIM_LEVEL_KEY
                || Boolean.TRUE.equals(GuideContent.ALMANAC_GIVEN.get(player))) {
            return;
        }
        GuideContent.ALMANAC_GIVEN.set(player, true);
        give(player, new ItemStack(GuideContent.SEAFARERS_ALMANAC));
        player.sendSystemMessage(Component.translatable("message.raft.almanac.given").withStyle(ChatFormatting.AQUA));
    }

    /** Into the backpack part of the inventory first, so the selected hotbar slot stays as it was. */
    private static void give(ServerPlayer player, ItemStack stack) {
        Inventory inv = player.getInventory();
        for (int slot = Inventory.getSelectionSize(); slot < Inventory.INVENTORY_SIZE; slot++) {
            if (inv.getItem(slot).isEmpty()) {
                inv.setItem(slot, stack);
                return;
            }
        }
        inv.placeItemBackInInventory(stack);
    }
}
