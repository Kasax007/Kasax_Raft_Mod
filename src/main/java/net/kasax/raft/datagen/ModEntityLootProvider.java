package net.kasax.raft.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricEntityLootSubProvider;
import net.kasax.raft.entity.ModEntities;
import net.kasax.raft.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

import java.util.concurrent.CompletableFuture;

public class ModEntityLootProvider extends FabricEntityLootSubProvider {
    public ModEntityLootProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(output, registryLookup);
    }

    @Override
    public void generate() {
        // Bruce the shark: shark meat (1 to 3) and sometimes a tooth (0 to 2).
        add(ModEntities.SHARK, LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .add(LootItem.lootTableItem(ModItems.SHARK_MEAT)
                                .apply(SetItemCountFunction.setCount(ContextIntProviders.between(1, 3)))))
                .withPool(LootPool.lootPool()
                        .add(LootItem.lootTableItem(ModItems.SHARK_TOOTH)
                                .apply(SetItemCountFunction.setCount(ContextIntProviders.between(0, 2))))));
    }
}
