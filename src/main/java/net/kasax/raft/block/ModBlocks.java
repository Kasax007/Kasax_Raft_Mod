package net.kasax.raft.block;


import net.kasax.raft.Raft;
import net.kasax.raft.block.custom.*;
import net.kasax.raft.world.gen.ModConfiguredFeatures;
import net.kasax.raft.world.tree.DriftwoodSaplingBlock;
import net.kasax.raft.world.tree.PalmSaplingBlock;
import net.minecraft.block.*;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

import java.util.Optional;

public class ModBlocks {

    public static final Block DRIFTWOOD_PORTAL_FRAME = registerBlock("driftwood_portal_frame",
            new DriftwoodPortalFrameBlock(AbstractBlock.Settings.copy(Blocks.STONE).sounds(BlockSoundGroup.WOOD)));

    public static final Block DRIFTWOOD_LOG = registerBlock("driftwood_log",
            new PillarBlock(AbstractBlock.Settings.copy(Blocks.OAK_LOG)));
    public static final Block DRIFTWOOD_WOOD = registerBlock("driftwood_wood",
            new PillarBlock(AbstractBlock.Settings.copy(Blocks.OAK_WOOD)));
    public static final Block STRIPPED_DRIFTWOOD_LOG = registerBlock("stripped_driftwood_log",
            new PillarBlock(AbstractBlock.Settings.copy(Blocks.STRIPPED_OAK_LOG)));
    public static final Block STRIPPED_DRIFTWOOD_WOOD = registerBlock("stripped_driftwood_wood",
            new PillarBlock(AbstractBlock.Settings.copy(Blocks.STRIPPED_OAK_WOOD)));

    public static final Block DRIFTWOOD_PLANKS = registerBlock("driftwood_planks",
            new Block(AbstractBlock.Settings.copy(Blocks.OAK_PLANKS)));

    public static final Block PALM_LOG = registerBlock("palm_log",
            new PillarBlock(AbstractBlock.Settings.copy(Blocks.OAK_LOG)));
    public static final Block PALM_WOOD = registerBlock("palm_wood",
            new PillarBlock(AbstractBlock.Settings.copy(Blocks.OAK_WOOD)));
    public static final Block STRIPPED_PALM_LOG = registerBlock("stripped_palm_log",
            new PillarBlock(AbstractBlock.Settings.copy(Blocks.STRIPPED_OAK_LOG)));
    public static final Block STRIPPED_PALM_WOOD = registerBlock("stripped_palm_wood",
            new PillarBlock(AbstractBlock.Settings.copy(Blocks.STRIPPED_OAK_WOOD)));
    public static final Block PALM_PLANKS = registerBlock("palm_planks",
            new Block(AbstractBlock.Settings.copy(Blocks.OAK_PLANKS)));

    public static final Block PALM_SAPLING = registerBlock("palm_sapling",
            new PalmSaplingBlock(new SaplingGenerator(
                    Raft.MOD_ID,
                    0.1F,
                    Optional.empty(),
                    Optional.empty(),
                    Optional.of(ModConfiguredFeatures.PALMWOOD_KEY),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty()
            ), AbstractBlock.Settings.copy(Blocks.OAK_SAPLING)));
    public static final Block PALM_LEAVES = registerBlock("palm_leaves",
            new PalmLeaves(AbstractBlock.Settings.copy(Blocks.OAK_LEAVES).nonOpaque()));

    public static final Block PALM_COCONUT_LEAVES = registerBlock("palm_coconut_leaves",
            new PalmCoconutLeaves(AbstractBlock.Settings.copy(Blocks.OAK_LEAVES).nonOpaque()));

    public static final Block DRIFTWOOD_STAIRS = registerBlock("driftwood_stairs",
            new StairsBlock(ModBlocks.DRIFTWOOD_PLANKS.getDefaultState(), AbstractBlock.Settings.copy(Blocks.OAK_WOOD)));

    public static final Block DRIFTWOOD_SLAB = registerBlock("driftwood_slab",
            new SlabBlock(AbstractBlock.Settings.copy(Blocks.OAK_WOOD)));

    public static final Block DRIFTWOOD_BUTTON = registerBlock("driftwood_button",
            new ButtonBlock(BlockSetType.OAK, 10, AbstractBlock.Settings.copy(Blocks.OAK_WOOD).noCollision()));

    public static final Block DRIFTWOOD_PRESSURE_PLATE = registerBlock("driftwood_pressure_plate",
            new PressurePlateBlock(BlockSetType.OAK, AbstractBlock.Settings.copy(Blocks.OAK_WOOD)));

    public static final Block DRIFTWOOD_FENCE = registerBlock("driftwood_fence",
            new FenceBlock(AbstractBlock.Settings.copy(Blocks.OAK_WOOD)));

    public static final Block DRIFTWOOD_FENCE_GATE = registerBlock("driftwood_fence_gate",
            new FenceGateBlock(WoodType.OAK, AbstractBlock.Settings.copy(Blocks.OAK_WOOD)));

    public static final Block DRIFTWOOD_DOOR = registerBlock("driftwood_door",
            new DoorBlock(BlockSetType.OAK, AbstractBlock.Settings.copy(Blocks.OAK_PLANKS).nonOpaque()));

    public static final Block DRIFTWOOD_TRAPDOOR = registerBlock("driftwood_trapdoor",
            new TrapdoorBlock(BlockSetType.OAK, AbstractBlock.Settings.copy(Blocks.OAK_PLANKS).nonOpaque()));

    public static final Block PALM_STAIRS = registerBlock("palm_stairs",
            new StairsBlock(ModBlocks.DRIFTWOOD_PLANKS.getDefaultState(), AbstractBlock.Settings.copy(Blocks.OAK_WOOD)));

    public static final Block PALM_SLAB = registerBlock("palm_slab",
            new SlabBlock(AbstractBlock.Settings.copy(Blocks.OAK_WOOD)));

    public static final Block PALM_BUTTON = registerBlock("palm_button",
            new ButtonBlock(BlockSetType.OAK, 10, AbstractBlock.Settings.copy(Blocks.OAK_WOOD).noCollision()));

    public static final Block PALM_PRESSURE_PLATE = registerBlock("palm_pressure_plate",
            new PressurePlateBlock(BlockSetType.OAK, AbstractBlock.Settings.copy(Blocks.OAK_WOOD)));

    public static final Block PALM_FENCE = registerBlock("palm_fence",
            new FenceBlock(AbstractBlock.Settings.copy(Blocks.OAK_WOOD)));

    public static final Block PALM_FENCE_GATE = registerBlock("palm_fence_gate",
            new FenceGateBlock(WoodType.OAK, AbstractBlock.Settings.copy(Blocks.OAK_WOOD)));

    public static final Block PALM_DOOR = registerBlock("palm_door",
            new DoorBlock(BlockSetType.OAK, AbstractBlock.Settings.copy(Blocks.OAK_PLANKS).nonOpaque()));

    public static final Block PALM_TRAPDOOR = registerBlock("palm_trapdoor",
            new TrapdoorBlock(BlockSetType.OAK, AbstractBlock.Settings.copy(Blocks.OAK_PLANKS).nonOpaque()));
    public static final Block DRIFTWOOD_LEAVES = registerBlock("driftwood_leaves",
            new LeavesBlock(AbstractBlock.Settings.copy(Blocks.OAK_LEAVES).nonOpaque()));

    public static final Block DRIFTWOOD_SAPLING = registerBlock("driftwood_sapling",
            new DriftwoodSaplingBlock (new SaplingGenerator(
                    Raft.MOD_ID,
                    0.1F,
                    Optional.empty(),
                    Optional.empty(),
                    Optional.of(ModConfiguredFeatures.DRIFTWOOD_KEY),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty()
            ), AbstractBlock.Settings.copy(Blocks.OAK_SAPLING)));


    public static final Block TITANIUM_ORE = registerBlock("titanium_ore",
            new Block(AbstractBlock.Settings.copy(Blocks.DIAMOND_ORE).strength(4f)));
    public static final Block DEEPSLATE_TITANIUM_ORE = registerBlock("deepslate_titanium_ore",
            new Block(AbstractBlock.Settings.copy(Blocks.DEEPSLATE_DIAMOND_ORE).strength(4f)));

    public static final Block RAW_TITANIUM_BLOCK = registerBlock("raw_titanium_block",
            new Block(AbstractBlock.Settings.copy(Blocks.DIAMOND_BLOCK).strength(4f)));

    public static final Block TITANIUM_BLOCK = registerBlock("titanium_block",
            new Block(AbstractBlock.Settings.copy(Blocks.NETHERITE_BLOCK).strength(4f)));

    public static final Block ITEM_CATCHER = registerBlock("item_catcher",
            new ItemCatcher(AbstractBlock.Settings.copy(Blocks.OAK_PLANKS).nonOpaque()));

    public static final Block MAKESHIFT_SOLAR_PANEL = registerBlock("makeshift_solar_panel",
            new MakeshiftSolarPanelBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK)));

    public static final Block MAKESHIFT_BATTERY = registerBlock("makeshift_battery",
            new MakeshiftBatteryBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK)));

    public static final Block TITANIUM_GRATES = registerBlock("titanium_grates",
            new TitaniumGrates(AbstractBlock.Settings.copy(Blocks.COPPER_BLOCK).nonOpaque().strength(4f)));

    private static Block registerBlock(String name, Block block) {
        registerBlockItem(name, block);
        return Registry.register(Registries.BLOCK, Identifier.of(Raft.MOD_ID, name), block);
    }

    private static Item registerBlockItem(String name, Block block) {
        return Registry.register(Registries.ITEM, Identifier.of(Raft.MOD_ID, name),
                new BlockItem(block, new Item.Settings()));
    }
    public static void registerModBlocks() {
        Raft.LOGGER.info("Registering ModBlocks for " + Raft.MOD_ID);
    }
}
