package net.kasax.raft.content.guide.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * The Seafarer's Almanac: use it to read the guide of Beach Craft. The screen is client-only; the client module sets
 * {@link #openScreen}. An optional bookmark in the item's custom data ({@code {almanac:"diving"}} or
 * {@code {almanac:"creatures/whale"}}) opens that chapter or page, e.g. for quest rewards.
 */
public class AlmanacItem extends Item {
    /** Set by the client init; the server never opens anything. */
    public static Consumer<ItemStack> openScreen = stack -> {
    };

    public AlmanacItem(Properties properties) {
        super(properties);
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return net.kasax.raft.compat.Results.holder(this.use26(level, player, hand), player.getItemInHand(hand));
    }

    private InteractionResult use26(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            openScreen.accept(player.getItemInHand(hand));
        } else {
            level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, player.getSoundSource(), 1.0F, 0.9F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<net.minecraft.network.chat.Component> builderList121, TooltipFlag flag) {
        java.util.function.Consumer<net.minecraft.network.chat.Component> builder = builderList121::add;
        builder.accept(Component.translatable("tooltip.raft.seafarers_almanac").withStyle(ChatFormatting.AQUA));
        builder.accept(Component.translatable("tooltip.raft.seafarers_almanac.1").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, builderList121, flag);
    }
}
