package net.kasax.raft.util;

import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
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
            new Identifier("minecraft", "gameplay/fishing/treasure");
    private static final Identifier SUSPICIOUS_SAND_ID =
            new Identifier("minecraft", "archaeology/desert_pyramid");

    public static void modifyLootTables() {
        //LootTableEvents.MODIFY.register((resourceManager, lootManager, id, tableBuilder, source) -> {
        //    if(FISHING_TREASURE_ID.equals(id)) {
        //               .rolls(ConstantLootNumberProvider.create(1)) //adds extra item
        //                .conditionally(RandomChanceLootCondition.builder(0.25f)) //drop chance 25%
        //                .with(ItemEntry.builder(ModItems.ANCIENT_BROKEN_RING))
        //                .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1.0f, 4.0f)).build()); //amount
        //        tableBuilder.pool(poolBuilder.build());
        //    }
        //});
        LootTableEvents.REPLACE.register((resourceManager, lootManager, id, original, source) -> {
            if(SUSPICIOUS_SAND_ID.equals(id)) {
                List<LootPoolEntry> entries = new ArrayList<>(Arrays.asList(original.pools[0].entries));
                entries.add(ItemEntry.builder(ModItems.ANCIENT_CRYSTAL).build());

                LootPool.Builder pool = LootPool.builder().with(entries);
                return LootTable.builder().pool(pool).build();
            }
            return null;
        });
        LootTableEvents.REPLACE.register((resourceManager, lootManager, id, original, source) -> {
            if(FISHING_TREASURE_ID.equals(id)) {
                List<LootPoolEntry> entries = new ArrayList<>(Arrays.asList(original.pools[0].entries));
                entries.add(ItemEntry.builder(ModItems.ANCIENT_BROKEN_RING).build());

                LootPool.Builder pool = LootPool.builder().with(entries);
                return LootTable.builder().pool(pool).build();
            }
            return null;
        });

    }
}
