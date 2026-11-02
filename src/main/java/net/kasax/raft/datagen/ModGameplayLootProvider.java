package net.kasax.raft.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.SimpleFabricLootTableSubProvider;
import net.kasax.raft.block.ModBlocks;
import net.kasax.raft.item.ModItems;
import net.kasax.raft.world.OceanDebris;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/** Loot of the barrels that drift in the raft dimension. */
public class ModGameplayLootProvider extends SimpleFabricLootTableSubProvider {
    public ModGameplayLootProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, LootContextParamSets.EMPTY);
    }

    private static LootItem.Builder<?> entry(ItemLike item, int weight, int min, int max) {
        LootItem.Builder<?> builder = LootItem.lootTableItem(item).setWeight(weight);
        if (max > 1) {
            builder.apply(SetItemCountFunction.setCount(ContextIntProviders.between(min, max)));
        }
        return builder;
    }

    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> exporter) {
        exporter.accept(OceanDebris.BARREL_LOOT, LootTable.lootTable().withPool(LootPool.lootPool()
                .add(entry(ModItems.COCONUT, 10, 1, 3))
                .add(entry(Items.COOKED_COD, 10, 1, 2))
                .add(entry(Items.GLASS_BOTTLE, 6, 1, 3))
                .add(entry(Items.STRING, 8, 1, 3))
                .add(entry(ModItems.ROPE, 8, 1, 3))
                .add(entry(Items.IRON_NUGGET, 6, 2, 5))
                .add(entry(Items.COAL, 6, 1, 3))
                .add(entry(ModBlocks.PALM_SAPLING, 4, 1, 1))
                .add(entry(ModBlocks.DRIFTWOOD_SAPLING, 2, 1, 1))
                .add(entry(ModItems.TITANIUM_NUGGET, 2, 1, 2))
                .add(entry(ModItems.CIRCUIT_BOARD, 1, 1, 1))));
    }
}
