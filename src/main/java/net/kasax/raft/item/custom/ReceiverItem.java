package net.kasax.raft.item.custom;

import net.kasax.raft.util.ModTags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * Shows the direction (cardinal direction and degrees) and the distance to the nearest raft structure.
 * The cooldown protects the server from repeated structure searches.
 */
public class ReceiverItem extends TooltipItem {
    public static final int COOLDOWN_TICKS = 60;
    /** Search radius in chunks. */
    public static final int SEARCH_RADIUS = 64;
    private static final String[] CARDINALS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

    public ReceiverItem(Properties properties) {
        super(properties, "tooltip.raft.receiver");
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return net.kasax.raft.compat.Results.holder(this.use26(level, player, hand), player.getItemInHand(hand));
    }

    private InteractionResult use26(Level level, Player player, InteractionHand hand) {
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            if (player.getCooldowns().isOnCooldown(player.getItemInHand(hand).getItem())) {
                return InteractionResult.FAIL;
            }
            player.getCooldowns().addCooldown(player.getItemInHand(hand).getItem(), COOLDOWN_TICKS);
            BlockPos target = serverLevel.findNearestMapStructure(ModTags.Structures.RECEIVER_TARGETS, player.blockPosition(), SEARCH_RADIUS, false);
            if (target == null) {
                serverPlayer.displayClientMessage(Component.translatable("item.raft.receiver.nothing").withStyle(ChatFormatting.RED), true);
            } else {
                double dx = target.getX() + 0.5 - player.getX();
                double dz = target.getZ() + 0.5 - player.getZ();
                int distance = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
                int degrees = bearing(dx, dz);
                serverPlayer.displayClientMessage(Component.translatable("item.raft.receiver.found",
                        cardinal(degrees), String.format("%03d", degrees), distance).withStyle(ChatFormatting.AQUA), true);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** Compass bearing in degrees: 0 is north, 90 is east. */
    public static int bearing(double dx, double dz) {
        double degrees = Math.toDegrees(Math.atan2(dx, -dz));
        return (int) Math.round((degrees % 360.0 + 360.0) % 360.0) % 360;
    }

    public static String cardinal(int bearing) {
        return CARDINALS[(int) Math.floorMod(Math.round(bearing / 45.0), 8L)];
    }
}
