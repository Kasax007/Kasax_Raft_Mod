package net.kasax.raft.item.custom;

import net.kasax.raft.item.ModItems;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static net.minecraft.sound.SoundEvents.BLOCK_SNIFFER_EGG_CRACK;

public class TrashCubeItem extends Item {
    private static final Random RANDOM = new Random();

    public TrashCubeItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.raft.trash_cube.tooltip").formatted(Formatting.AQUA));
        super.appendTooltip(stack, world, tooltip, context);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        if (!world.isClient) {
            // Define possible drops with their maximum amounts
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
            possibleDrops.put(new ItemStack(Items.PUFFERFISH, RANDOM.nextInt(1) + 1), 5); // common
            possibleDrops.put(createEnchantedFishingRod(), 1); // very rare
            possibleDrops.put(new ItemStack(Items.BUCKET, RANDOM.nextInt(1) + 1), 3); // rare
            possibleDrops.put(new ItemStack(Items.PUFFERFISH_BUCKET, RANDOM.nextInt(1) + 1), 1); // very rare
            possibleDrops.put(new ItemStack(Items.OAK_SAPLING, 1), 5); // rare
            // You can add more saplings or other items as needed

            // Get a random number of drops (between 1 and 3)
            int totalItemCount = RANDOM.nextInt(3) + 1;

            // Create a list to hold selected drops
            List<ItemStack> selectedDrops = new ArrayList<>();

            // Select items until the total item count is reached
            int itemCount = 0;
            while (itemCount < totalItemCount) {
                ItemStack selectedDrop = getRandomDrop(possibleDrops);
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
                Vec3d spawnPos = player.getPos().add(RANDOM.nextDouble() * 2 - 1, 0.5, RANDOM.nextDouble() * 2 - 1);
                ItemEntity itemEntity = new ItemEntity(world, spawnPos.x, spawnPos.y, spawnPos.z, drop);
                world.spawnEntity(itemEntity);
            }

            // Trigger a game event for the action
            world.emitGameEvent(player, GameEvent.ENTITY_PLACE, player.getBlockPos());
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    BLOCK_SNIFFER_EGG_CRACK, SoundCategory.NEUTRAL, 20F,
                    1F / (world.getRandom().nextFloat() * 0.4F + 0.8F));

            // Consume one TrashCube item from the player's hand
            player.getStackInHand(hand).decrement(1);
        }

        return TypedActionResult.success(player.getStackInHand(hand));
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

    private ItemStack createEnchantedFishingRod() {
        ItemStack fishingRod = new ItemStack(Items.FISHING_ROD);

        // Map to hold enchantments and their levels
        Map<Enchantment, Integer> enchantments = new HashMap<>();

        // Generate random levels for each enchantment
        int lureLevel = getRandomEnchantmentLevel(Enchantments.LURE);
        int luckLevel = getRandomEnchantmentLevel(Enchantments.LUCK_OF_THE_SEA);
        int unbreakingLevel = getRandomEnchantmentLevel(Enchantments.UNBREAKING);
        int mendingLevel = getRandomEnchantmentLevel(Enchantments.MENDING);

        // Add enchantments if the level is greater than 0
        if (lureLevel > 0) {
            enchantments.put(Enchantments.LURE, lureLevel);
        }
        if (luckLevel > 0) {
            enchantments.put(Enchantments.LUCK_OF_THE_SEA, luckLevel);
        }
        if (unbreakingLevel > 0) {
            enchantments.put(Enchantments.UNBREAKING, unbreakingLevel);
        }
        if (mendingLevel > 0) {
            enchantments.put(Enchantments.MENDING, mendingLevel);
        }

        // Apply the generated enchantments to the fishing rod
        EnchantmentHelper.set(enchantments, fishingRod);

        // Set random durability
        int maxDurability = fishingRod.getMaxDamage();
        int randomDurability = RANDOM.nextInt(maxDurability);
        fishingRod.setDamage(maxDurability - randomDurability);

        return fishingRod;
    }
    private int getRandomEnchantmentLevel(Enchantment enchantment) {
        int maxLevel = enchantment.getMaxLevel();
        return RANDOM.nextInt(maxLevel + 1); // Random level from 0 to maxLevel (inclusive)
    }
}
