package net.kasax.raft.item.custom;

import net.kasax.raft.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

public class TrashCubeItem extends Item {
    private static final Random RANDOM = new Random();

    public TrashCubeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<net.minecraft.network.chat.Component> builderList121, TooltipFlag flag) {
        java.util.function.Consumer<net.minecraft.network.chat.Component> builder = builderList121::add;
        builder.accept(Component.translatable("tooltip.raft.trash_cube.tooltip").withStyle(ChatFormatting.AQUA));
        super.appendHoverText(stack, context, builderList121, flag);
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return net.kasax.raft.compat.Results.holder(this.use26(level, player, hand), player.getItemInHand(hand));
    }

    private InteractionResult use26(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            // Define possible drops with their weights
            Map<ItemStack, Integer> possibleDrops = new HashMap<>();
            possibleDrops.put(new ItemStack(ModItems.PLASTIC, 1), 5); // rare
            possibleDrops.put(new ItemStack(ModItems.TITANIUM_NUGGET, RANDOM.nextInt(3) + 1), 10); // rare
            possibleDrops.put(new ItemStack(Items.GOLD_NUGGET, RANDOM.nextInt(3) + 1), 20); // common
            possibleDrops.put(new ItemStack(Items.DIAMOND, 1), 1); // very rare
            possibleDrops.put(new ItemStack(Items.COPPER_INGOT, 1), 5); // rare
            possibleDrops.put(new ItemStack(Items.IRON_NUGGET, RANDOM.nextInt(3) + 1), 20); // common
            possibleDrops.put(new ItemStack(Items.EMERALD, 1), 1); // very rare
            possibleDrops.put(new ItemStack(Items.SLIME_BALL, 1), 5); // rare
            possibleDrops.put(new ItemStack(Items.KELP, RANDOM.nextInt(3) + 1), 20); // common
            possibleDrops.put(new ItemStack(Items.STICK, RANDOM.nextInt(3) + 1), 10); // common
            possibleDrops.put(new ItemStack(Items.TROPICAL_FISH, RANDOM.nextInt(3) + 1), 10); // common
            possibleDrops.put(new ItemStack(Items.COD, RANDOM.nextInt(3) + 1), 10); // common
            possibleDrops.put(new ItemStack(Items.SALMON, RANDOM.nextInt(3) + 1), 10); // common
            possibleDrops.put(new ItemStack(Items.PUFFERFISH, 1), 5); // common
            possibleDrops.put(new ItemStack(Items.BUCKET, 1), 3); // rare
            possibleDrops.put(new ItemStack(Items.PUFFERFISH_BUCKET, 1), 1); // very rare
            possibleDrops.put(new ItemStack(Items.OAK_SAPLING, 1), 5); // rare

            // Get a random number of drops (between 1 and 3)
            int totalItemCount = RANDOM.nextInt(3) + 1;

            // Create a list to hold selected drops
            List<ItemStack> selectedDrops = new ArrayList<>();

            // Select items until the total item count is reached
            int itemCount = 0;
            while (itemCount < totalItemCount) {
                ItemStack selectedDrop = getRandomDrop(possibleDrops).copy();
                int remainingItems = totalItemCount - itemCount;

                // Adjust drop count to not exceed the total item count
                if (selectedDrop.getCount() > remainingItems) {
                    selectedDrop.setCount(remainingItems);
                }

                selectedDrops.add(selectedDrop);
                itemCount += selectedDrop.getCount();
            }

            // Spawn the selected items around the player
            for (ItemStack drop : selectedDrops) {
                Vec3 spawnPos = player.position().add(RANDOM.nextDouble() * 2 - 1, 0.5, RANDOM.nextDouble() * 2 - 1);
                level.addFreshEntity(new ItemEntity(level, spawnPos.x, spawnPos.y, spawnPos.z, drop));
            }

            // Trigger a game event for the action
            level.gameEvent(player, GameEvent.ENTITY_PLACE, player.blockPosition());
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SNIFFER_EGG_CRACK, SoundSource.NEUTRAL, 1.0F,
                    1F / (level.getRandom().nextFloat() * 0.4F + 0.8F));

            // Consume one trash cube from the player's hand
            player.getItemInHand(hand).consume(1, player);
        }

        return InteractionResult.SUCCESS;
    }

    private ItemStack getRandomDrop(Map<ItemStack, Integer> possibleDrops) {
        List<ItemStack> items = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();

        for (Map.Entry<ItemStack, Integer> entry : possibleDrops.entrySet()) {
            items.add(entry.getKey());
            weights.add(entry.getValue());
        }

        int totalWeight = weights.stream().mapToInt(Integer::intValue).sum();
        int randomIndex = RANDOM.nextInt(totalWeight);

        for (int i = 0; i < items.size(); i++) {
            randomIndex -= weights.get(i);
            if (randomIndex < 0) {
                return items.get(i);
            }
        }

        return ItemStack.EMPTY; // Fallback in case of an error
    }
}
