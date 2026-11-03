package net.kasax.raft.content.voyage;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/** An Expedition Raft upgrade: use it on the raft to install it (the raft handles the interaction). */
public class RaftUpgradeItem extends Item {
    private final RaftUpgrade upgrade;

    public RaftUpgradeItem(Properties properties, RaftUpgrade upgrade) {
        super(properties);
        this.upgrade = upgrade;
    }

    public RaftUpgrade upgrade() {
        return this.upgrade;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder,
                                TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.raft." + this.upgrade.itemId).withStyle(ChatFormatting.AQUA));
        builder.accept(Component.translatable("tooltip.raft.raft_upgrade").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, display, builder, flag);
    }
}
