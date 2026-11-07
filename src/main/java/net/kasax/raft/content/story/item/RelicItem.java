package net.kasax.raft.content.story.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.function.Consumer;

/**
 * A Tidewarden relic: the four Tide Sigils and the Tidewarden Key. Epic, glinting, one per stack; as item entities
 * they ignore fire, lava, lightning and explosions (damage resistant component) and never despawn (StoryEvents).
 */
public class RelicItem extends Item {
    private final String tooltipKey;

    public RelicItem(Properties properties, String tooltipKey) {
        super(properties);
        this.tooltipKey = tooltipKey;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<net.minecraft.network.chat.Component> builderList121, TooltipFlag flag) {
        java.util.function.Consumer<net.minecraft.network.chat.Component> builder = builderList121::add;
        builder.accept(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        builder.accept(Component.translatable("tooltip.raft." + tooltipKey + ".0").withStyle(ChatFormatting.AQUA));
        builder.accept(Component.translatable("tooltip.raft." + tooltipKey + ".1").withStyle(ChatFormatting.GRAY));
    }
}
