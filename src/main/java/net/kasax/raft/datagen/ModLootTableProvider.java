package net.kasax.raft.datagen;


import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.kasax.raft.block.ModBlocks;
import net.kasax.raft.item.ModItems;
import net.kasax.raft.util.FurnaceBlocks;
import net.minecraft.block.Block;
import net.minecraft.data.server.loottable.BlockLootTableGenerator;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.entry.LeafEntry;
import net.minecraft.loot.entry.LootPoolEntry;
import net.minecraft.loot.function.ApplyBonusLootFunction;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.concurrent.CompletableFuture;

public class ModLootTableProvider extends FabricBlockLootTableProvider {


    public ModLootTableProvider(FabricDataOutput dataOutput, CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup) {
        super(dataOutput, registryLookup);
    }

    @Override
    public void generate() {
        addDrop(ModBlocks.DRIFTWOOD_PORTAL_FRAME);
        addDrop(ModBlocks.DRIFTWOOD_LOG);
        addDrop(ModBlocks.DRIFTWOOD_PLANKS);
        addDrop(ModBlocks.DRIFTWOOD_WOOD);
        addDrop(ModBlocks.STRIPPED_DRIFTWOOD_LOG);
        addDrop(ModBlocks.STRIPPED_DRIFTWOOD_WOOD);
        addDrop(ModBlocks.PALM_LOG);
        addDrop(ModBlocks.PALM_PLANKS);
        addDrop(ModBlocks.PALM_WOOD);
        addDrop(ModBlocks.STRIPPED_PALM_LOG);
        addDrop(ModBlocks.STRIPPED_PALM_WOOD);
        addDrop(ModBlocks.TITANIUM_BLOCK);
        addDrop(ModBlocks.RAW_TITANIUM_BLOCK);
        addDrop(ModBlocks.DRIFTWOOD_SAPLING);
        addDrop(ModBlocks.PALM_SAPLING);
        addDrop(ModBlocks.ITEM_CATCHER);
        addDrop(ModBlocks.TITANIUM_GRATES);
        addDrop(ModBlocks.DRIFTWOOD_STAIRS);
        addDrop(ModBlocks.DRIFTWOOD_SLAB, slabDrops(ModBlocks.DRIFTWOOD_SLAB));
        addDrop(ModBlocks.DRIFTWOOD_FENCE);
        addDrop(ModBlocks.DRIFTWOOD_FENCE_GATE);
        addDrop(ModBlocks.DRIFTWOOD_BUTTON);
        addDrop(ModBlocks.DRIFTWOOD_DOOR, doorDrops(ModBlocks.DRIFTWOOD_DOOR));
        addDrop(ModBlocks.DRIFTWOOD_PRESSURE_PLATE);
        addDrop(ModBlocks.DRIFTWOOD_TRAPDOOR);
        addDrop(ModBlocks.PALM_STAIRS);
        addDrop(ModBlocks.PALM_SLAB, slabDrops(ModBlocks.PALM_SLAB));
        addDrop(ModBlocks.PALM_FENCE);
        addDrop(ModBlocks.PALM_FENCE_GATE);
        addDrop(ModBlocks.PALM_BUTTON);
        addDrop(ModBlocks.PALM_DOOR, doorDrops(ModBlocks.PALM_DOOR));
        addDrop(ModBlocks.PALM_PRESSURE_PLATE);
        addDrop(ModBlocks.PALM_TRAPDOOR);
        addDrop(ModBlocks.MAKESHIFT_SOLAR_PANEL);
        addDrop(ModBlocks.MAKESHIFT_BATTERY);
        addDrop(ModBlocks.QUARRY);
        addDrop(ModBlocks.CHUNK_DESTROYER);

        addDrop(ModBlocks.TITANIUM_ORE, copperLikeOreDrops(ModBlocks.TITANIUM_ORE, ModItems.RAW_TITANIUM));
        addDrop(ModBlocks.DEEPSLATE_TITANIUM_ORE, copperLikeOreDrops(ModBlocks.DEEPSLATE_TITANIUM_ORE, ModItems.RAW_TITANIUM));

        addDrop(ModBlocks.DRIFTWOOD_LEAVES, leavesDrops(ModBlocks.DRIFTWOOD_LEAVES, ModBlocks.DRIFTWOOD_SAPLING, 0.0025f));
        addDrop(ModBlocks.PALM_LEAVES, leavesDrops(ModBlocks.PALM_LEAVES, ModBlocks.PALM_SAPLING, 0.05f));
        addDrop(ModBlocks.PALM_COCONUT_LEAVES, fruitLeaveDrops(ModBlocks.PALM_COCONUT_LEAVES, ModItems.COCONUT));

        // Register models for all custom furnaces
        FurnaceBlocks.getFurnaces().forEach(furnace ->
                addDrop(furnace)
        );
    }
    public LootTable.Builder copperLikeOreDrops(Block drop, Item item) {
        RegistryWrapper.Impl<Enchantment> impl = this.registryLookup.getWrapperOrThrow(RegistryKeys.ENCHANTMENT);
        return this.dropsWithSilkTouch(drop, (LootPoolEntry.Builder)this.applyExplosionDecay(drop,
                ((LeafEntry.Builder) ItemEntry.builder(item)
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider
                                .create(2.0f, 5.0f))))
                        .apply(ApplyBonusLootFunction.oreDrops(impl.getOrThrow(Enchantments.FORTUNE)))));
    }
    public LootTable.Builder fruitLeaveDrops(Block drop, Item item) {
        RegistryWrapper.Impl<Enchantment> impl = this.registryLookup.getWrapperOrThrow(RegistryKeys.ENCHANTMENT);
        return this.dropsWithShears(drop,
                (LootPoolEntry.Builder) this.applyExplosionDecay(drop,
                        ((LeafEntry.Builder) ItemEntry.builder(item)
                                .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1.0f, 1.0f)))
                                .conditionally(RandomChanceLootCondition.builder(0.5f)) // 50% chance to add this pool
                                .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1.0f, 1.0f)))
                                .conditionally(RandomChanceLootCondition.builder(0.2f)) // 20% chance to add this pool
                                .apply(ApplyBonusLootFunction.oreDrops(impl.getOrThrow(Enchantments.FORTUNE))
                                        ))));
    }
}
