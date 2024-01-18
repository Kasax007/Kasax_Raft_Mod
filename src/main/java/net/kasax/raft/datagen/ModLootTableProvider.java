package net.kasax.raft.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.kasax.raft.block.ModBlocks;
import net.kasax.raft.item.ModItems;
import net.minecraft.block.Block;
import net.minecraft.data.server.loottable.BlockLootTableGenerator;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.entry.LeafEntry;
import net.minecraft.loot.entry.LootPoolEntry;
import net.minecraft.loot.function.ApplyBonusLootFunction;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;

public class ModLootTableProvider extends FabricBlockLootTableProvider {
    public ModLootTableProvider(FabricDataOutput dataOutput) {
        super(dataOutput);
    }

    @Override
    public void generate() {
        addDrop(ModBlocks.DRIFTWOOD_PORTAL_FRAME);
        addDrop(ModBlocks.DRIFTWOOD_LOG);
        addDrop(ModBlocks.DRIFTWOOD_PLANKS);
        addDrop(ModBlocks.DRIFTWOOD_WOOD);
        addDrop(ModBlocks.STRIPPED_DRIFTWOOD_LOG);
        addDrop(ModBlocks.STRIPPED_DRIFTWOOD_WOOD);
        addDrop(ModBlocks.TITANIUM_BLOCK);
        addDrop(ModBlocks.RAW_TITANIUM_BLOCK);
        addDrop(ModBlocks.DRIFTWOOD_SAPLING);
        addDrop(ModBlocks.ITEM_CATCHER);
        addDrop(ModBlocks.TITANIUM_GRATES);

        addDrop(ModBlocks.TITANIUM_ORE, copperLikeOreDrops(ModBlocks.TITANIUM_ORE, ModItems.RAW_TITANIUM));
        addDrop(ModBlocks.DEEPSLATE_TITANIUM_ORE, copperLikeOreDrops(ModBlocks.DEEPSLATE_TITANIUM_ORE, ModItems.RAW_TITANIUM));

        addDrop(ModBlocks.DRIFTWOOD_LEAVES, leavesDrops(ModBlocks.DRIFTWOOD_LEAVES, ModBlocks.DRIFTWOOD_SAPLING, 0.0025f));
    }
    public LootTable.Builder copperLikeOreDrops(Block drop, Item item) {
        return BlockLootTableGenerator.dropsWithSilkTouch(drop, (LootPoolEntry.Builder)this.applyExplosionDecay(drop,
                ((LeafEntry.Builder) ItemEntry.builder(item)
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider
                                .create(2.0f, 5.0f))))
                        .apply(ApplyBonusLootFunction.oreDrops(Enchantments.FORTUNE))));
    }
}
