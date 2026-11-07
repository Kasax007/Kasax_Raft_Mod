package net.kasax.raft.util;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.kasax.raft.item.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

/** Adds the mod's items to the vanilla shipwreck treasure and fishing treasure loot tables. */
public class ModLootTableModifiers {

    private static LootPool.Builder extraPool(Item item, float chance, int minCount, int maxCount) {
        return LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .when(LootItemRandomChanceCondition.randomChance(chance))
                .add(LootItem.lootTableItem(item))
                .apply(SetItemCountFunction.setCount(UniformGenerator.between(minCount, maxCount)));
    }

    public static void modifyLootTables() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            if (BuiltInLootTables.SHIPWRECK_TREASURE.equals(key)) {
                tableBuilder.pool(extraPool(ModItems.ANCIENT_CRYSTAL, 0.5f, 1, 5).build());
                tableBuilder.pool(extraPool(ModItems.ANCIENT_BROKEN_RING, 0.6f, 1, 2).build());
                tableBuilder.pool(extraPool(ModItems.LOFI_BEAT_MUSIC_DISC, 0.1f, 1, 1).build());
                tableBuilder.pool(extraPool(ModItems.CRYSTAL_CASTLES_MUSIC_DISC, 0.1f, 1, 1).build());
                tableBuilder.pool(extraPool(ModItems.SOME_GAME_MUSIC_MUSIC_DISC, 0.1f, 1, 1).build());
            }
            if (BuiltInLootTables.FISHING_TREASURE.equals(key)) {
                tableBuilder.pool(extraPool(ModItems.ANCIENT_BROKEN_RING, 0.25f, 1, 1).build());
            }
            if (BuiltInLootTables.BURIED_TREASURE.equals(key)) {
                tableBuilder.pool(extraPool(ModItems.ANCIENT_BROKEN_RING, 0.8f, 1, 3).build());
            }
            if (BuiltInLootTables.UNDERWATER_RUIN_BIG.equals(key) || BuiltInLootTables.UNDERWATER_RUIN_SMALL.equals(key)) {
                tableBuilder.pool(extraPool(ModItems.ANCIENT_BROKEN_RING, 0.3f, 1, 1).build());
            }
        });
    }
}
