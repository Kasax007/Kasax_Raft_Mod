package net.kasax.raft.block;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.kasax.raft.Raft;
import net.kasax.raft.world.tree.DriftwoodSaplingGenerator;
import net.kasax.raft.world.tree.ModSaplingBlock;
import net.minecraft.block.*;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public class ModBlocks {

    public static final Block DRIFTWOOD_PORTAL_FRAME = registerBlock("driftwood_portal_frame",
            new Block(FabricBlockSettings.copyOf(Blocks.STONE).sounds(BlockSoundGroup.WOOD)));

    public static final Block DRIFTWOOD_LOG = registerBlock("driftwood_log",
            new PillarBlock(FabricBlockSettings.copyOf(Blocks.OAK_LOG).strength(4f)));
    public static final Block DRIFTWOOD_WOOD = registerBlock("driftwood_wood",
            new PillarBlock(FabricBlockSettings.copyOf(Blocks.OAK_WOOD).strength(4f)));
    public static final Block STRIPPED_DRIFTWOOD_LOG = registerBlock("stripped_driftwood_log",
            new PillarBlock(FabricBlockSettings.copyOf(Blocks.STRIPPED_OAK_LOG).strength(4f)));
    public static final Block STRIPPED_DRIFTWOOD_WOOD = registerBlock("stripped_driftwood_wood",
            new PillarBlock(FabricBlockSettings.copyOf(Blocks.STRIPPED_OAK_WOOD).strength(4f)));

    public static final Block DRIFTWOOD_PLANKS = registerBlock("driftwood_planks",
            new Block(FabricBlockSettings.copyOf(Blocks.OAK_PLANKS).strength(4f)));
    public static final Block DRIFTWOOD_LEAVES = registerBlock("driftwood_leaves",
            new LeavesBlock(FabricBlockSettings.copyOf(Blocks.OAK_LEAVES).strength(4f).nonOpaque()));

    public static final Block DRIFTWOOD_SAPLING = registerBlock("driftwood_sapling",
            new SaplingBlock(new DriftwoodSaplingGenerator(), FabricBlockSettings.copyOf(Blocks.OAK_SAPLING)));


    public static final Block TITANIUM_ORE = registerBlock("titanium_ore",
            new LeavesBlock(FabricBlockSettings.copyOf(Blocks.DIAMOND_ORE).strength(4f)));
    public static final Block DEEPSLATE_TITANIUM_ORE = registerBlock("deepslate_titanium_ore",
            new LeavesBlock(FabricBlockSettings.copyOf(Blocks.DEEPSLATE_DIAMOND_ORE).strength(4f)));

    public static final Block RAW_TITANIUM_BLOCK = registerBlock("raw_titanium_block",
            new LeavesBlock(FabricBlockSettings.copyOf(Blocks.DIAMOND_BLOCK).strength(4f)));

    public static final Block TITANIUM_BLOCK = registerBlock("titanium_block",
            new LeavesBlock(FabricBlockSettings.copyOf(Blocks.NETHERITE_BLOCK).strength(4f)));

    private static Block registerBlock(String name, Block block) {
        registerBlockItem(name, block);
        return Registry.register(Registries.BLOCK, new Identifier(Raft.MOD_ID, name), block);
    }

    private static Item registerBlockItem(String name, Block block) {
        return Registry.register(Registries.ITEM, new Identifier(Raft.MOD_ID, name),
                new BlockItem(block, new FabricItemSettings()));
    }
    public static void registerModBlocks() {
        Raft.LOGGER.info("Registering ModBlocks for " + Raft.MOD_ID);
    }
}