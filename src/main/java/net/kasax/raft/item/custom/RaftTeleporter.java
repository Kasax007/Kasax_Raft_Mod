package net.kasax.raft.item.custom;

import net.kasax.raft.util.TeleporterUtil;
import net.kasax.raft.world.dimension.ModDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

public class RaftTeleporter extends Item {
    private final ResourceKey<Level> dimKey = ModDimensions.RAFTDIM_LEVEL_KEY;

    public RaftTeleporter(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            if (TeleporterUtil.movePlayer(dimKey, level, player)) {
                if (!player.isCreative()) {
                    stack.hurtAndBreak(1, player, hand);
                }
                player.getCooldowns().addCooldown(stack, 20);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.raft.raft_teleporter.tooltip").withStyle(ChatFormatting.AQUA));
        super.appendHoverText(stack, context, display, builder, flag);
    }
}
