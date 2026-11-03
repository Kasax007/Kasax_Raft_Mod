package net.kasax.raft.content.story;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Small helpers shared by the story items and blocks (server side). */
public final class StoryUtil {
    private StoryUtil() {
    }

    /** Puts the stack into the inventory or drops it at the player's feet. */
    public static void give(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack) && !stack.isEmpty()) {
            player.drop(stack, false, Prediction.SERVER_ONLY);
        }
    }

    /** A short status line above the hotbar ("message.raft.<key>"). */
    public static void tell(Player player, String key, Object... args) {
        if (player instanceof ServerPlayer sp) {
            sp.sendOverlayMessage(Component.translatable("message.raft." + key, args).withStyle(ChatFormatting.AQUA));
        }
    }

    /** m:ss for a number of ticks. */
    public static String time(long ticks) {
        long s = Math.max(0, (ticks + 19) / 20);
        return (s / 60) + ":" + String.format("%02d", s % 60);
    }
}
