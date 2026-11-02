package net.kasax.raft.block;

import net.kasax.raft.Raft;
import net.kasax.raft.block.cable.RaftCable;
import net.kasax.raft.block.custom.*;
import net.kasax.raft.item.custom.TooltipBlockItem;
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
import net.minecraft.world.level.block.sounds.AmbientLeavesBlockSoundPlayer;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;

import java.util.Arrays;
import java.util.function.Function;

public class ModBlocks {

    public static final Block DRIFTWOOD_PORTAL_FRAME = register("driftwood_portal_frame",
            DriftwoodPortalFrameBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).sound(SoundType.WOOD),
            "tooltip.raft.driftwood_portal_frame.tooltip");

    public static final Block RAFT_PORTAL = registerBlockOnly("raft_portal",
            RaftPortalBlock::new, BlockBehaviour.Properties.of()
                    .noCollision().noOcclusion().strength(-1.0F).sound(SoundType.GLASS)
                    .lightLevel(state -> 11).pushReaction(net.minecraft.world.level.material.PushReaction.IMMOVEABLE)
                    .noLootTable());

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
            p -> new LeavesBlock(AmbientLeavesBlockSoundPlayer.noAmbientSound(), p),
            BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES).noOcclusion());
    public static final Block DRIFTWOOD_SAPLING = register("driftwood_sapling",
            p -> new DriftwoodSaplingBlock(ModTreeGrowers.DRIFTWOOD, p), BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING));

    public static final Block TITANIUM_ORE = register("titanium_ore",
            Block::new, BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_ORE).strength(4f));
    public static final Block DEEPSLATE_TITANIUM_ORE = register("deepslate_titanium_ore",
            Block::new, BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_DIAMOND_ORE).strength(4f));
    public static final Block RAW_TITANIUM_BLOCK = register("raw_titanium_block",
            Block::new, BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_BLOCK).strength(4f));
    public static final Block TITANIUM_BLOCK = register("titanium_block",
            Block::new, BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK).strength(4f));

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
            TitaniumGrates::new, BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK.weathering().unaffected()).noOcclusion().strength(4f));

    public static void registerCables() {
        Arrays.stream(RaftCable.Cables.values()).forEach(cable -> {
            ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, Raft.id(cable.name));
            Registry.register(BuiltInRegistries.BLOCK, blockKey, cable.block);
            ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Raft.id(cable.name));
            Registry.register(BuiltInRegistries.ITEM, itemKey, new TooltipBlockItem(cable.block,
                    new Item.Properties().setId(itemKey).useBlockDescriptionPrefix(),
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
        Block block = factory.apply(properties.setId(key));
        return Registry.register(BuiltInRegistries.BLOCK, key, block);
    }

    public static Item registerBlockItem(String name, Block block, String... tooltips) {
        return registerBlockItem(name, block, new Item.Properties(), tooltips);
    }

    public static Item registerBlockItem(String name, Block block, Item.Properties properties, String... tooltips) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Raft.id(name));
        properties = properties.setId(key).useBlockDescriptionPrefix();
        Item item = tooltips.length == 0 ? new BlockItem(block, properties) : new TooltipBlockItem(block, properties, tooltips);
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    public static void registerModBlocks() {
        Raft.LOGGER.info("Registering ModBlocks for " + Raft.MOD_ID);
    }
}
