package net.kasax.raft.util;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.kasax.raft.item.ModItems;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.entry.LootPoolEntry;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ModLootTableModifiers {
    private static final Identifier FISHING_TREASURE_ID =
            Identifier.of("minecraft", "gameplay/fishing/treasure");
    private static final Identifier SHIPWRECK_TREASURE_ID =
            Identifier.of("minecraft", "chests/shipwreck_treasure");

    public static void modifyLootTables() {
        //LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
        //    if(FISHING_TREASURE_ID.equals(key)) {
        //               .rolls(ConstantLootNumberProvider.create(1)) //adds extra item
        //                .conditionally(RandomChanceLootCondition.builder(0.25f)) //drop chance 25%
        //                .with(ItemEntry.builder(ModItems.ANCIENT_BROKEN_RING))
        //                .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1.0f, 4.0f)).build()); //amount
        //        tableBuilder.pool(poolBuilder.build());
        //    }
        //});
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            if(SHIPWRECK_TREASURE_ID.equals(key)) {
                LootPool.Builder poolBuilder = LootPool.builder()
                .rolls(ConstantLootNumberProvider.create(1)) //adds extra item
                .conditionally(RandomChanceLootCondition.builder(0.5f)) //drop chance 25%
                .with(ItemEntry.builder(ModItems.ANCIENT_CRYSTAL))
                .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1.0f, 5.0f)).build()); //amount
                tableBuilder.pool(poolBuilder.build());
            }
        });
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            if(SHIPWRECK_TREASURE_ID.equals(key)) {
                LootPool.Builder poolBuilder = LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1)) //adds extra item
                        .conditionally(RandomChanceLootCondition.builder(0.1f)) //drop chance 25%
                        .with(ItemEntry.builder(ModItems.LOFI_BEAT_MUSIC_DISC))
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1.0f, 5.0f)).build()); //amount
                tableBuilder.pool(poolBuilder.build());
            }
        });
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            if(SHIPWRECK_TREASURE_ID.equals(key)) {
                LootPool.Builder poolBuilder = LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1)) //adds extra item
                        .conditionally(RandomChanceLootCondition.builder(0.1f)) //drop chance 25%
                        .with(ItemEntry.builder(ModItems.SOME_GAME_MUSIC_MUSIC_DISC))
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1.0f, 5.0f)).build()); //amount
                tableBuilder.pool(poolBuilder.build());
            }
        });
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            if(FISHING_TREASURE_ID.equals(key)) {
                LootPool.Builder poolBuilder = LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1)) //adds extra item
                        .conditionally(RandomChanceLootCondition.builder(0.05f)) //drop chance 25%
                        .with(ItemEntry.builder(ModItems.ANCIENT_BROKEN_RING))
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1.0f, 1.0f)).build()); //amount
                tableBuilder.pool(poolBuilder.build());
            }
        });
        //LootTableEvents.REPLACE.register((resourceManager, lootManager, key, original, source) -> {
        //    if(FISHING_TREASURE_ID.equals(key)) {
        //        List<LootPoolEntry> entries = new ArrayList<>(Arrays.asList(original.pools[0].entries));
        //        entries.add(ItemEntry.builder(ModItems.ANCIENT_BROKEN_RING).build());
//
        //        LootPool.Builder pool = LootPool.builder().with(entries);
        //        return LootTable.builder().pool(pool).build();
        //    }
        //    return null;
        //});

    }
}
