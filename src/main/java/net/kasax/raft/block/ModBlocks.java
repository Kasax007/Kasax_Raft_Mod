package net.kasax.raft.block;


import net.kasax.raft.Raft;
import net.kasax.raft.block.cable.RaftCable;
import net.kasax.raft.block.custom.*;
import net.kasax.raft.item.custom.TooltipBlockItem;
import net.kasax.raft.item.custom.TooltipDoubleBlockItem;
import net.kasax.raft.world.gen.ModTreeGrowers;
import net.kasax.raft.world.tree.DriftwoodSaplingBlock;
import net.kasax.raft.world.tree.PalmSaplingBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;

import java.util.Arrays;
import java.util.function.Function;
import net.kasax.raft.compat.VShapes;
import net.kasax.raft.compat.ItemProps;

public class ModBlocks {

    public static final Block DRIFTWOOD_PORTAL_FRAME = register("driftwood_portal_frame",
            DriftwoodPortalFrameBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(1.5f, 6.0f).sound(SoundType.WOOD),
            "tooltip.raft.driftwood_portal_frame.tooltip");

    public static final Block RAFT_PORTAL = registerBlockOnly("raft_portal",
            RaftPortalBlock::new, BlockBehaviour.Properties.of()
                    .noCollission().noOcclusion().strength(-1.0F).sound(SoundType.GLASS)
                    .lightLevel(state -> 11).pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK));

    public static final Block DRIFTWOOD_LOG = register("driftwood_log",
            RotatedPillarBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG));
    public static final Block DRIFTWOOD_WOOD = register("driftwood_wood",
            RotatedPillarBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_WOOD));
    public static final Block STRIPPED_DRIFTWOOD_LOG = register("stripped_driftwood_log",
            RotatedPillarBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_LOG));
    public static final Block STRIPPED_DRIFTWOOD_WOOD = register("stripped_driftwood_wood",
            RotatedPillarBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_WOOD));
    public static final Block DRIFTWOOD_PLANKS = register("driftwood_planks",
            Block::new, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS));

    public static final Block PALM_LOG = register("palm_log",
            RotatedPillarBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG));
    public static final Block PALM_WOOD = register("palm_wood",
            RotatedPillarBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_WOOD));
    public static final Block STRIPPED_PALM_LOG = register("stripped_palm_log",
            RotatedPillarBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_LOG));
    public static final Block STRIPPED_PALM_WOOD = register("stripped_palm_wood",
            RotatedPillarBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_WOOD));
    public static final Block PALM_PLANKS = register("palm_planks",
            Block::new, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS));

    public static final Block PALM_SAPLING = register("palm_sapling",
            p -> new PalmSaplingBlock(ModTreeGrowers.PALM, p), BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING));
    public static final Block PALM_LEAVES = register("palm_leaves",
            PalmLeaves::new, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES).noOcclusion());
    public static final Block PALM_COCONUT_LEAVES = register("palm_coconut_leaves",
            PalmCoconutLeaves::new, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES).noOcclusion());

    public static final Block DRIFTWOOD_STAIRS = register("driftwood_stairs",
            p -> new StairBlock(ModBlocks.DRIFTWOOD_PLANKS.defaultBlockState(), p), BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS));
    public static final Block DRIFTWOOD_SLAB = register("driftwood_slab",
            SlabBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SLAB));
    public static final Block DRIFTWOOD_BUTTON = register("driftwood_button",
            p -> new ButtonBlock(BlockSetType.OAK, 30, p), BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_BUTTON));
    public static final Block DRIFTWOOD_PRESSURE_PLATE = register("driftwood_pressure_plate",
            p -> new PressurePlateBlock(BlockSetType.OAK, p), BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PRESSURE_PLATE));
    public static final Block DRIFTWOOD_FENCE = register("driftwood_fence",
            FenceBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE));
    public static final Block DRIFTWOOD_FENCE_GATE = register("driftwood_fence_gate",
            p -> new FenceGateBlock(WoodType.OAK, p), BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE_GATE));
    public static final Block DRIFTWOOD_DOOR = register("driftwood_door",
            p -> new DoorBlock(BlockSetType.OAK, p), BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_DOOR));
    public static final Block DRIFTWOOD_TRAPDOOR = register("driftwood_trapdoor",
            p -> new TrapDoorBlock(BlockSetType.OAK, p), BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_TRAPDOOR));

    public static final Block PALM_STAIRS = register("palm_stairs",
            p -> new StairBlock(ModBlocks.PALM_PLANKS.defaultBlockState(), p), BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS));
    public static final Block PALM_SLAB = register("palm_slab",
            SlabBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SLAB));
    public static final Block PALM_BUTTON = register("palm_button",
            p -> new ButtonBlock(BlockSetType.OAK, 30, p), BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_BUTTON));
    public static final Block PALM_PRESSURE_PLATE = register("palm_pressure_plate",
            p -> new PressurePlateBlock(BlockSetType.OAK, p), BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PRESSURE_PLATE));
    public static final Block PALM_FENCE = register("palm_fence",
            FenceBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE));
    public static final Block PALM_FENCE_GATE = register("palm_fence_gate",
            p -> new FenceGateBlock(WoodType.OAK, p), BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE_GATE));
    public static final Block PALM_DOOR = register("palm_door",
            p -> new DoorBlock(BlockSetType.OAK, p), BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_DOOR));
    public static final Block PALM_TRAPDOOR = register("palm_trapdoor",
            p -> new TrapDoorBlock(BlockSetType.OAK, p), BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_TRAPDOOR));

    public static final Block DRIFTWOOD_LEAVES = register("driftwood_leaves",
            p -> new LeavesBlock(p),
            BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES).noOcclusion());
    public static final Block DRIFTWOOD_SAPLING = register("driftwood_sapling",
            p -> new DriftwoodSaplingBlock(ModTreeGrowers.DRIFTWOOD, p), BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING));

    public static final Block TITANIUM_ORE = register("titanium_ore",
            p -> new net.minecraft.world.level.block.DropExperienceBlock(net.minecraft.util.valueproviders.UniformInt.of(3, 7), p),
            BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_ORE).strength(4f));
    public static final Block DEEPSLATE_TITANIUM_ORE = register("deepslate_titanium_ore",
            p -> new net.minecraft.world.level.block.DropExperienceBlock(net.minecraft.util.valueproviders.UniformInt.of(3, 7), p),
            BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_DIAMOND_ORE).strength(4.5f, 4f));
    public static final Block RAW_TITANIUM_BLOCK = register("raw_titanium_block",
            Block::new, BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_BLOCK).strength(5f, 6f));
    public static final Block TITANIUM_BLOCK = register("titanium_block",
            Block::new, BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK).strength(5f, 6f));

    public static final Block ITEM_CATCHER = register("item_catcher",
            ItemCatcher::new, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).noOcclusion());

    public static final Block MAKESHIFT_SOLAR_PANEL = register("makeshift_solar_panel",
            MakeshiftSolarPanelBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK),
            "tooltip.raft.makeshift_solar_panel.tooltip");
    public static final Block MAKESHIFT_BATTERY = register("makeshift_battery",
            MakeshiftBatteryBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK),
            "tooltip.raft.makeshift_battery.tooltip1", "tooltip.raft.makeshift_battery.tooltip2");
    public static final Block QUARRY = register("quarry",
            QuarryBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK),
            "tooltip.raft.quarry.tooltip1", "tooltip.raft.quarry.tooltip2");
    public static final Block CHUNK_DESTROYER = register("chunk_destroyer",
            ChunkDestroyer::new, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK),
            "tooltip.raft.chunk_destroyer.tooltip1", "tooltip.raft.chunk_destroyer.tooltip2");
    public static final Block TITANIUM_GRATES = register("titanium_grates",
            TitaniumGrates::new, BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK).noOcclusion().strength(4f));

    // ---- decoration ----

    public static final Block PALM_THATCH = register("palm_thatch",
            Block::new, BlockBehaviour.Properties.ofFullCopy(Blocks.HAY_BLOCK).strength(0.5f));
    public static final Block PALM_THATCH_STAIRS = register("palm_thatch_stairs",
            p -> new StairBlock(ModBlocks.PALM_THATCH.defaultBlockState(), p), BlockBehaviour.Properties.ofFullCopy(Blocks.HAY_BLOCK).strength(0.5f));
    public static final Block PALM_THATCH_SLAB = register("palm_thatch_slab",
            SlabBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.HAY_BLOCK).strength(0.5f));

    public static final Block RAFT_FOUNDATION = register("raft_foundation",
            Block::new, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(2.5f, 6.0f));
    public static final Block REINFORCED_FOUNDATION = register("reinforced_foundation",
            Block::new, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(5.0f, 14.0f));
    public static final Block SCRAP_METAL_BLOCK = register("scrap_metal_block",
            Block::new, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3.0f, 6.0f));
    public static final Block PLASTIC_BLOCK = register("plastic_block",
            Block::new, BlockBehaviour.Properties.ofFullCopy(Blocks.WHITE_CONCRETE).strength(1.5f, 4.0f).sound(SoundType.COPPER));

    public static final Block ROPE_LADDER = register("rope_ladder",
            LadderBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.LADDER).sound(SoundType.WOOL));
    public static final Block ROPE_COIL = register("rope_coil",
            p -> new ShapedBlock(p, VShapes.column(12.0, 0.0, 6.0)),
            BlockBehaviour.Properties.ofFullCopy(Blocks.WHITE_WOOL).noOcclusion().strength(0.4f));
    public static final Block SAIL_CANVAS = register("sail_canvas",
            Block::new, BlockBehaviour.Properties.ofFullCopy(Blocks.WHITE_WOOL));
    public static final Block SAIL_CANVAS_RED_STRIPED = register("sail_canvas_red_striped",
            Block::new, BlockBehaviour.Properties.ofFullCopy(Blocks.WHITE_WOOL));

    public static final Block SHIP_WHEEL = register("ship_wheel",
            p -> new WallDecorationBlock(p, Block.box(0.0, 0.0, 12.0, 16.0, 16.0, 16.0)),
            BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(1.5f).noOcclusion().noCollission());
    public static final Block SHARK_JAW_TROPHY = register("shark_jaw_trophy",
            p -> new WallDecorationBlock(p, Block.box(1.0, 1.0, 13.0, 15.0, 15.0, 16.0)),
            BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(1.0f).noOcclusion().noCollission());
    public static final Block RAFT_FLAG = register("raft_flag",
            RaftFlagBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(0.6f).noOcclusion().noCollission());
    public static final Block COCONUT_LANTERN = register("coconut_lantern",
            LanternBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.LANTERN).sound(SoundType.WOOD).lightLevel(state -> 13));

    // ---- functional ----

    public static final Block CROP_PLOT = register("crop_plot",
            CropPlotBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.FARMLAND).sound(SoundType.WOOD).strength(0.8f),
            "tooltip.raft.crop_plot.tooltip");
    public static final Block SIMPLE_GRILL = register("simple_grill",
            SimpleGrillBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.0f).noOcclusion()
                    .lightLevel(state -> state.getValue(SimpleGrillBlock.LIT) ? 13 : 0),
            "tooltip.raft.simple_grill.tooltip");
    public static final Block WATER_PURIFIER = register("water_purifier",
            WaterPurifierBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.5f)
                    .lightLevel(state -> state.getValue(WaterPurifierBlock.LIT) ? 7 : 0),
            "tooltip.raft.water_purifier.tooltip");
    public static final Block SCARECROW = registerBlockOnly("scarecrow",
            ScarecrowBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE).strength(0.8f).noOcclusion()
                    .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY));
    public static final Item SCARECROW_ITEM = registerBlockItem("scarecrow", SCARECROW,
            new TooltipDoubleBlockItem(SCARECROW,
                    new Item.Properties(),
                    "tooltip.raft.scarecrow.tooltip"));

    public static void registerCables() {
        Arrays.stream(RaftCable.Cables.values()).forEach(cable -> {
            ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, Raft.id(cable.name));
            Registry.register(BuiltInRegistries.BLOCK, blockKey, cable.block);
            ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Raft.id(cable.name));
            Registry.register(BuiltInRegistries.ITEM, itemKey, new TooltipBlockItem(cable.block,
                    new Item.Properties(),
                    Component.translatable("tooltip.raft.cable.max_input", cable.tier.getMaxInput()).withStyle(ChatFormatting.AQUA)));
        });
    }

    /** Registers a block together with its block item. */
    public static Block register(String name, Function<BlockBehaviour.Properties, Block> factory,
                                 BlockBehaviour.Properties properties, String... tooltips) {
        Block block = registerBlockOnly(name, factory, properties);
        registerBlockItem(name, block, tooltips);
        return block;
    }

    /** Registers a block without an item (portal block, wall variants, ...). */
    public static Block registerBlockOnly(String name, Function<BlockBehaviour.Properties, Block> factory,
                                          BlockBehaviour.Properties properties) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Raft.id(name));
        Block block = factory.apply(properties);
        return Registry.register(BuiltInRegistries.BLOCK, key, block);
    }

    public static Item registerBlockItem(String name, Block block, String... tooltips) {
        return registerBlockItem(name, block, new Item.Properties(), tooltips);
    }

    /** Registers a ready made block item for the block. */
    public static Item registerBlockItem(String name, Block block, BlockItem item) {
        return Registry.register(BuiltInRegistries.ITEM, ResourceKey.create(Registries.ITEM, Raft.id(name)), item);
    }

    /**
     * In 26.3 furnace fuel is the item's cooking_fuel component: wooden blocks of the driftwood and palm sets burn like
     * their oak counterparts (logs, planks, stairs, fences... 300 ticks, slabs 150, doors 200, buttons 100).
     */
    private static Item.Properties withWoodFuel(String name, Item.Properties properties) {
        String n = name.startsWith("stripped_") ? name.substring("stripped_".length()) : name;
        if ((n.startsWith("driftwood_") || n.startsWith("palm_")) && n.endsWith("_leaves")) {
            return ItemProps.compostable(properties, ItemProps.COMPOSTABLE_LOW);
        }
        if ((n.startsWith("driftwood_") || n.startsWith("palm_")) && n.endsWith("_sapling")) {
            return ItemProps.cookingFuel(ItemProps.compostable(properties, ItemProps.COMPOSTABLE_LOW), ItemProps.COOKING_TIME_DRY_PLANTS);
        }
        boolean wood = (n.startsWith("driftwood_") || n.startsWith("palm_")) && !n.contains("leaves") && !n.contains("sapling")
                && !n.contains("thatch") && !n.contains("portal") && !n.contains("coconut");
        if (!wood) return properties;
        var time = n.endsWith("_slab") ? ItemProps.COOKING_TIME_WOOD_SLABS
                : n.endsWith("_door") ? ItemProps.COOKING_TIME_WOOD_ITEMS_LARGE
                : n.endsWith("_button") ? ItemProps.COOKING_TIME_WOOD_ITEMS_EXTRA_SMALL
                : ItemProps.COOKING_TIME_WOOD_BLOCKS;
        return ItemProps.cookingFuel(properties, time);
    }

    public static Item registerBlockItem(String name, Block block, Item.Properties properties, String... tooltips) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Raft.id(name));
        properties = withWoodFuel(name, properties);
        Item item = tooltips.length == 0 ? new BlockItem(block, properties) : new TooltipBlockItem(block, properties, tooltips);
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    public static void registerModBlocks() {
        Raft.LOGGER.info("Registering ModBlocks for " + Raft.MOD_ID);
    }
}
