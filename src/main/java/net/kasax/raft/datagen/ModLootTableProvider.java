package net.kasax.raft.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootSubProvider;
import net.kasax.raft.block.ModBlocks;
import net.kasax.raft.block.cable.RaftCable;
import net.kasax.raft.item.ModItems;
import net.kasax.raft.util.FurnaceBlocks;
import net.kasax.raft.util.StoredEnergy;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.concurrent.CompletableFuture;

public class ModLootTableProvider extends FabricBlockLootSubProvider {

    public ModLootTableProvider(FabricPackOutput dataOutput, CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(dataOutput, registryLookup);
    }

    @Override
    public void generate() {
        dropSelf(ModBlocks.DRIFTWOOD_PORTAL_FRAME);
        dropSelf(ModBlocks.DRIFTWOOD_LOG);
        dropSelf(ModBlocks.DRIFTWOOD_PLANKS);
        dropSelf(ModBlocks.DRIFTWOOD_WOOD);
        dropSelf(ModBlocks.STRIPPED_DRIFTWOOD_LOG);
        dropSelf(ModBlocks.STRIPPED_DRIFTWOOD_WOOD);
        dropSelf(ModBlocks.PALM_LOG);
        dropSelf(ModBlocks.PALM_PLANKS);
        dropSelf(ModBlocks.PALM_WOOD);
        dropSelf(ModBlocks.STRIPPED_PALM_LOG);
        dropSelf(ModBlocks.STRIPPED_PALM_WOOD);
        dropSelf(ModBlocks.TITANIUM_BLOCK);
        dropSelf(ModBlocks.RAW_TITANIUM_BLOCK);
        // driftwood is no tree (3.5): the sapling is in no loot any more, but old worlds keep theirs and it still drops itself
        dropSelf(ModBlocks.DRIFTWOOD_SAPLING);
        dropSelf(ModBlocks.PALM_SAPLING);
        dropSelf(ModBlocks.ITEM_CATCHER);
        dropSelf(ModBlocks.TITANIUM_GRATES);
        dropSelf(ModBlocks.DRIFTWOOD_STAIRS);
        add(ModBlocks.DRIFTWOOD_SLAB, createSlabItemTable(ModBlocks.DRIFTWOOD_SLAB));
        dropSelf(ModBlocks.DRIFTWOOD_FENCE);
        dropSelf(ModBlocks.DRIFTWOOD_FENCE_GATE);
        dropSelf(ModBlocks.DRIFTWOOD_BUTTON);
        add(ModBlocks.DRIFTWOOD_DOOR, createDoorTable(ModBlocks.DRIFTWOOD_DOOR));
        dropSelf(ModBlocks.DRIFTWOOD_PRESSURE_PLATE);
        dropSelf(ModBlocks.DRIFTWOOD_TRAPDOOR);
        dropSelf(ModBlocks.PALM_STAIRS);
        add(ModBlocks.PALM_SLAB, createSlabItemTable(ModBlocks.PALM_SLAB));
        dropSelf(ModBlocks.PALM_FENCE);
        dropSelf(ModBlocks.PALM_FENCE_GATE);
        dropSelf(ModBlocks.PALM_BUTTON);
        add(ModBlocks.PALM_DOOR, createDoorTable(ModBlocks.PALM_DOOR));
        dropSelf(ModBlocks.PALM_PRESSURE_PLATE);
        dropSelf(ModBlocks.PALM_TRAPDOOR);
        add(ModBlocks.MAKESHIFT_SOLAR_PANEL, machineDrops(ModBlocks.MAKESHIFT_SOLAR_PANEL));
        add(ModBlocks.MAKESHIFT_BATTERY, machineDrops(ModBlocks.MAKESHIFT_BATTERY));
        add(ModBlocks.QUARRY, machineDrops(ModBlocks.QUARRY));
        add(ModBlocks.CHUNK_DESTROYER, machineDrops(ModBlocks.CHUNK_DESTROYER));

        add(ModBlocks.TITANIUM_ORE, titaniumOreDrops(ModBlocks.TITANIUM_ORE, ModItems.RAW_TITANIUM));
        add(ModBlocks.DEEPSLATE_TITANIUM_ORE, titaniumOreDrops(ModBlocks.DEEPSLATE_TITANIUM_ORE, ModItems.RAW_TITANIUM));

        // leaves (shears or silk touch) or now and then a stick, like mangrove leaves: driftwood is no tree (3.5)
        add(ModBlocks.DRIFTWOOD_LEAVES, createMangroveLeavesDrops(ModBlocks.DRIFTWOOD_LEAVES));
        add(ModBlocks.PALM_LEAVES, withRopeChance(createLeavesDrops(ModBlocks.PALM_LEAVES, ModBlocks.PALM_SAPLING, 0.05f)));
        add(ModBlocks.PALM_COCONUT_LEAVES, fruitLeaveDrops(ModBlocks.PALM_COCONUT_LEAVES, ModItems.COCONUT));

        // decoration and survival blocks
        dropSelf(ModBlocks.RAFT_FOUNDATION);
        dropSelf(ModBlocks.REINFORCED_FOUNDATION);
        dropSelf(ModBlocks.SCRAP_METAL_BLOCK);
        dropSelf(ModBlocks.PLASTIC_BLOCK);
        dropSelf(ModBlocks.PALM_THATCH);
        dropSelf(ModBlocks.PALM_THATCH_STAIRS);
        add(ModBlocks.PALM_THATCH_SLAB, createSlabItemTable(ModBlocks.PALM_THATCH_SLAB));
        dropSelf(ModBlocks.SAIL_CANVAS);
        dropSelf(ModBlocks.SAIL_CANVAS_RED_STRIPED);
        dropSelf(ModBlocks.ROPE_LADDER);
        dropSelf(ModBlocks.ROPE_COIL);
        dropSelf(ModBlocks.SHIP_WHEEL);
        dropSelf(ModBlocks.SHARK_JAW_TROPHY);
        dropSelf(ModBlocks.RAFT_FLAG);
        dropSelf(ModBlocks.COCONUT_LANTERN);
        dropSelf(ModBlocks.CROP_PLOT);
        dropSelf(ModBlocks.SIMPLE_GRILL);
        dropSelf(ModBlocks.WATER_PURIFIER);
        add(ModBlocks.SCARECROW, createDoorTable(ModBlocks.SCARECROW));

        // Cables
        for (RaftCable.Cables cable : RaftCable.Cables.values()) {
            dropSelf(cable.block);
        }

        // All custom furnaces
        FurnaceBlocks.getFurnaces().forEach(this::dropSelf);
    }

    /** A machine drops itself with the energy it held (3.5: the raft:stored_energy component, see StoredEnergy). */
    private LootTable.Builder machineDrops(Block block) {
        return LootTable.lootTable().withPool(applyExplosionCondition(block, LootPool.lootPool()
                .setRolls(ContextIntProviders.exactly(1))
                .add(LootItem.lootTableItem(block)
                        .apply(CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY)
                                .include(StoredEnergy.COMPONENT)))));
    }

    /** Palm fibres: broken palm leaves (without shears) sometimes give a piece of rope. */
    private LootTable.Builder withRopeChance(LootTable.Builder builder) {
        return builder.withPool(LootPool.lootPool()
                .when(doesNotHaveShearsOrSilkTouch())
                .when(LootItemRandomChanceCondition.randomChance(0.1f))
                .add(LootItem.lootTableItem(ModItems.ROPE)));
    }

    /** Ore that drops 2 to 5 raw items, with fortune; silk touch gives the ore block. */
    public LootTable.Builder titaniumOreDrops(Block ore, Item raw) {
        return createSilkTouchDispatchTable(ore,
                applyExplosionDecay(ore,
                        LootItem.lootTableItem(raw)
                                .apply(SetItemCountFunction.setCount(ContextIntProviders.between(2, 5)))
                                .apply(ApplyBonusCount.addOreBonusCount(enchantments.getOrThrow(Enchantments.FORTUNE)))));
    }

    /** Coconut leaves: shears give the leaves, otherwise a small chance to drop a coconut. */
    public LootTable.Builder fruitLeaveDrops(Block leaves, Item fruit) {
        return createShearsDispatchTable(leaves,
                applyExplosionDecay(leaves,
                        LootItem.lootTableItem(fruit)
                                .apply(SetItemCountFunction.setCount(ContextIntProviders.exactly(1)))
                                .when(LootItemRandomChanceCondition.randomChance(0.5f)) // 50% chance
                                .when(LootItemRandomChanceCondition.randomChance(0.2f)) // and 20% chance
                                .apply(ApplyBonusCount.addOreBonusCount(enchantments.getOrThrow(Enchantments.FORTUNE)))));
    }
}
