package net.kasax.raft.content.story.item;

import net.kasax.raft.Raft;
import net.kasax.raft.content.story.StoryUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.List;
import java.util.function.Consumer;

/** A corked bottle from the drifting debris. Use: pull the cork and get a random lore letter and the empty bottle. */
public class MessageInABottleItem extends Item {
    public static final ResourceKey<LootTable> LETTERS = ResourceKey.create(Registries.LOOT_TABLE, Raft.id("story/bottle_letters"));

    public MessageInABottleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 1.0F, 1.3F);
        if (level instanceof ServerLevel server) {
            LootTable table = server.getServer().reloadableRegistries().getLootTable(LETTERS);
            LootParams params = new LootParams.Builder(server)
                    .withParameter(LootContextParams.ORIGIN, player.position())
                    .withParameter(LootContextParams.THIS_ENTITY, player)
                    .create(LootContextParamSets.CHEST);
            List<ItemStack> loot = table.getRandomItems(params);
            if (loot.isEmpty()) {
                StoryUtil.tell(player, "bottle.empty");
            }
            for (ItemStack letter : loot) {
                StoryUtil.give(player, letter);
            }
            server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
            stack.consume(1, player);
            StoryUtil.give(player, new ItemStack(Items.GLASS_BOTTLE));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder,
                                TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.raft.message_in_a_bottle.0").withStyle(ChatFormatting.GRAY));
    }
}
