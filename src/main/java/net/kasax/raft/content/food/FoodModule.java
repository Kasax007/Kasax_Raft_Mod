package net.kasax.raft.content.food;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.kasax.raft.Raft;
import net.kasax.raft.content.ContentModule;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

import java.util.Map;
import java.util.Set;

/**
 * Beach Craft 3.3 module "food": island crops, dishes, the cooking pot and fish trap. Wild crops grow on the islands
 * (and sea grapes in the warm shallows); seeds also come with flotsam barrels and in island chests.
 */
public class FoodModule implements ContentModule {
    @Override
    public String name() {
        return "food";
    }

    @Override
    public void init() {
        FoodContent.init();
        worldgen();
        loot();
        emptyPotEvenWithOffhandItem();
    }

    /**
     * Sneak-use with an empty main hand gives the ingredients of a cooking pot back. Vanilla skips block use while
     * sneaking with anything in either hand, so a torch or shield in the off hand would block it; this event comes
     * before that check.
     */
    private static void emptyPotEvenWithOffhandItem() {
        net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (hand != net.minecraft.world.InteractionHand.MAIN_HAND || !player.isSecondaryUseActive()
                    || !player.getMainHandItem().isEmpty() || player.isSpectator()) {
                return net.minecraft.world.InteractionResult.PASS;
            }
            if (!(level.getBlockEntity(hit.getBlockPos()) instanceof net.kasax.raft.content.food.block.CookingPotBlockEntity pot)) {
                return net.minecraft.world.InteractionResult.PASS;
            }
            if (!level.isClientSide()) pot.useEmptyHand(player);
            return net.minecraft.world.InteractionResult.SUCCESS;
        });
    }

    @Override
    public void initClient() {
        net.kasax.raft.content.food.client.FoodClient.init();
    }

    private static ResourceKey<Biome> biome(String name) {
        return ResourceKey.create(Registries.BIOME, Raft.id(name));
    }

    private static void patch(String feature, String... biomes) {
        ResourceKey<PlacedFeature> key = ResourceKey.create(Registries.PLACED_FEATURE, Raft.id(feature));
        @SuppressWarnings("unchecked")
        ResourceKey<Biome>[] keys = java.util.Arrays.stream(biomes).map(FoodModule::biome).toArray(ResourceKey[]::new);
        BiomeModifications.addFeature(BiomeSelectors.includeByKey(keys), GenerationStep.Decoration.VEGETAL_DECORATION, key);
    }

    private static void worldgen() {
        patch("patch_wild_sweet_potato", "palm_isle", "jungle_isle");
        patch("patch_wild_chili", "jungle_isle", "volcanic_isle");
        patch("patch_wild_banana", "jungle_isle", "palm_isle");
        patch("patch_wild_rice", "mangrove_isle", "jungle_isle");
        patch("patch_sea_grapes", "coral_shallows", "glowing_lagoon");
    }

    private static LootPool.Builder pool(float chance, Map<Item, Integer> items, int min, int max) {
        LootPool.Builder b = LootPool.lootPool().when(LootItemRandomChanceCondition.randomChance(chance));
        items.forEach((item, weight) -> b.add(LootItem.lootTableItem(item).setWeight(weight)
                .apply(SetItemCountFunction.setCount(ContextIntProviders.between(min, max)))));
        return b;
    }

    private static ResourceKey<LootTable> table(String path) {
        return ResourceKey.create(Registries.LOOT_TABLE, Raft.id(path));
    }

    private static void loot() {
        Map<Item, Integer> seeds = Map.of(FoodContent.SWEET_POTATO, 3, FoodContent.CHILI_SEEDS, 3,
                FoodContent.BANANA_SHOOT, 2, FoodContent.RICE, 3, FoodContent.SEA_GRAPES, 2);
        Map<Item, Integer> pantry = Map.of(FoodContent.BANANA, 3, FoodContent.COOKED_RICE, 2, FoodContent.BAKED_SWEET_POTATO, 2,
                FoodContent.SUSHI_ROLL, 2, FoodContent.BANANA_BREAD, 1, FoodContent.CHILI_PEPPER, 2);
        Set<ResourceKey<LootTable>> island = Set.of(table("chests/castaway_camp"), table("chests/beach_shack"),
                table("chests/abandoned_raft"), table("chests/saltmere_house"), table("chests/saltmere_market"),
                table("chests/story/castaway"), table("chests/wreck_fishing"));
        ResourceKey<LootTable> barrel = table("gameplay/ocean_barrel");
        ResourceKey<LootTable> supply = table("gameplay/supply_barrel");
        LootTableEvents.MODIFY.register((key, builder, source, registries) -> {
            if (key.equals(barrel)) builder.pool(pool(0.08F, seeds, 1, 2).build());
            if (key.equals(supply)) builder.pool(pool(0.5F, seeds, 1, 3).build());
            if (island.contains(key)) {
                builder.pool(pool(0.7F, seeds, 1, 3).build());
                builder.pool(pool(0.5F, pantry, 1, 3).build());
            }
            if (key.equals(BuiltInLootTables.SHIPWRECK_SUPPLY)) builder.pool(pool(0.35F, seeds, 1, 3).build());
            if (key.equals(BuiltInLootTables.BURIED_TREASURE)) builder.pool(pool(0.3F, Map.of(FoodContent.BANANA_SHOOT, 1), 1, 2).build());
        });
    }
}
