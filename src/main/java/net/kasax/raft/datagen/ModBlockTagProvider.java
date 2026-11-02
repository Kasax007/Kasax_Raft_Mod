package net.kasax.raft.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.kasax.raft.block.ModBlocks;
import net.kasax.raft.block.cable.RaftCable;
import net.kasax.raft.util.FurnaceBlocks;
import net.kasax.raft.util.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockItemTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends FabricTagsProvider<Block> {
    public ModBlockTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, Registries.BLOCK, registriesFuture);
    }

    public static ResourceKey<Block> key(Block block) {
        return BuiltInRegistries.BLOCK.getResourceKey(block).orElseThrow();
    }

    private void addAll(TagKey<Block> tag, Block... blocks) {
        for (Block block : blocks) {
            tag(tag).add(key(block));
        }
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        addAll(ModTags.Blocks.RAFT_MOD_BLOCK,
                ModBlocks.DRIFTWOOD_PORTAL_FRAME,
                ModBlocks.DRIFTWOOD_LEAVES,
                ModBlocks.DRIFTWOOD_LOG,
                ModBlocks.DRIFTWOOD_WOOD,
                ModBlocks.DRIFTWOOD_PLANKS,
                ModBlocks.DRIFTWOOD_STAIRS,
                ModBlocks.DRIFTWOOD_SLAB,
                ModBlocks.DRIFTWOOD_BUTTON,
                ModBlocks.DRIFTWOOD_PRESSURE_PLATE,
                ModBlocks.DRIFTWOOD_FENCE,
                ModBlocks.DRIFTWOOD_FENCE_GATE,
                ModBlocks.DRIFTWOOD_DOOR,
                ModBlocks.DRIFTWOOD_TRAPDOOR,
                ModBlocks.PALM_STAIRS,
                ModBlocks.PALM_SLAB,
                ModBlocks.PALM_BUTTON,
                ModBlocks.PALM_PRESSURE_PLATE,
                ModBlocks.PALM_FENCE,
                ModBlocks.PALM_FENCE_GATE,
                ModBlocks.PALM_DOOR,
                ModBlocks.PALM_TRAPDOOR,
                ModBlocks.STRIPPED_DRIFTWOOD_LOG,
                ModBlocks.STRIPPED_DRIFTWOOD_WOOD,
                ModBlocks.TITANIUM_BLOCK,
                ModBlocks.RAW_TITANIUM_BLOCK,
                ModBlocks.TITANIUM_ORE,
                ModBlocks.DEEPSLATE_TITANIUM_ORE,
                ModBlocks.TITANIUM_GRATES,
                ModBlocks.ITEM_CATCHER,
                ModBlocks.MAKESHIFT_SOLAR_PANEL,
                ModBlocks.MAKESHIFT_BATTERY,
                ModBlocks.QUARRY);

        // Add all furnaces to the RAFT_MOD_BLOCK tag
        FurnaceBlocks.getFurnaces().forEach(furnace -> addAll(ModTags.Blocks.RAFT_MOD_BLOCK, furnace));

        for (RaftCable.Cables cable : RaftCable.Cables.values()) {
            addAll(ModTags.Blocks.RAFT_MOD_BLOCK, cable.block);
        }

        addAll(BlockTags.MINEABLE_WITH_AXE, ModBlocks.DRIFTWOOD_PORTAL_FRAME);

        addAll(BlockTags.MINEABLE_WITH_PICKAXE,
                ModBlocks.RAW_TITANIUM_BLOCK,
                ModBlocks.TITANIUM_ORE,
                ModBlocks.TITANIUM_BLOCK,
                ModBlocks.DEEPSLATE_TITANIUM_ORE,
                ModBlocks.TITANIUM_GRATES,
                ModBlocks.MAKESHIFT_SOLAR_PANEL,
                ModBlocks.QUARRY,
                ModBlocks.CHUNK_DESTROYER,
                ModBlocks.MAKESHIFT_BATTERY);

        // Add all furnaces and cables to the pickaxe tag
        FurnaceBlocks.getFurnaces().forEach(furnace -> addAll(BlockTags.MINEABLE_WITH_PICKAXE, furnace));
        for (RaftCable.Cables cable : RaftCable.Cables.values()) {
            addAll(BlockTags.MINEABLE_WITH_PICKAXE, cable.block);
        }

        addAll(BlockTags.NEEDS_IRON_TOOL,
                ModBlocks.DRIFTWOOD_PORTAL_FRAME,
                ModBlocks.MAKESHIFT_SOLAR_PANEL,
                ModBlocks.QUARRY,
                ModBlocks.CHUNK_DESTROYER,
                ModBlocks.MAKESHIFT_BATTERY);

        FurnaceBlocks.getFurnaces().forEach(furnace -> addAll(BlockTags.NEEDS_IRON_TOOL, furnace));
        for (RaftCable.Cables cable : RaftCable.Cables.values()) {
            addAll(BlockTags.NEEDS_IRON_TOOL, cable.block);
        }

        addAll(BlockTags.NEEDS_DIAMOND_TOOL,
                ModBlocks.RAW_TITANIUM_BLOCK,
                ModBlocks.TITANIUM_ORE,
                ModBlocks.DEEPSLATE_TITANIUM_ORE,
                ModBlocks.TITANIUM_BLOCK,
                ModBlocks.TITANIUM_GRATES);

        addAll(BlockItemTags.LOGS_THAT_BURN.block(),
                ModBlocks.DRIFTWOOD_LOG,
                ModBlocks.DRIFTWOOD_WOOD,
                ModBlocks.STRIPPED_DRIFTWOOD_WOOD,
                ModBlocks.STRIPPED_DRIFTWOOD_LOG,
                ModBlocks.PALM_LOG,
                ModBlocks.PALM_WOOD,
                ModBlocks.STRIPPED_PALM_WOOD,
                ModBlocks.STRIPPED_PALM_LOG);

        addAll(BlockTags.FENCES, ModBlocks.DRIFTWOOD_FENCE, ModBlocks.PALM_FENCE);
        addAll(BlockTags.FENCE_GATES, ModBlocks.DRIFTWOOD_FENCE_GATE, ModBlocks.PALM_FENCE_GATE);

        addAll(BlockTags.PORTALS, ModBlocks.RAFT_PORTAL);

        // Wood set blocks belong to the vanilla wood tags too, so redstone, villagers and recipes treat them as wood.
        addAll(BlockTags.PLANKS, ModBlocks.DRIFTWOOD_PLANKS, ModBlocks.PALM_PLANKS);
        addAll(BlockTags.WOODEN_STAIRS, ModBlocks.DRIFTWOOD_STAIRS, ModBlocks.PALM_STAIRS);
        addAll(BlockTags.WOODEN_SLABS, ModBlocks.DRIFTWOOD_SLAB, ModBlocks.PALM_SLAB);
        addAll(BlockTags.WOODEN_DOORS, ModBlocks.DRIFTWOOD_DOOR, ModBlocks.PALM_DOOR);
        addAll(BlockTags.WOODEN_TRAPDOORS, ModBlocks.DRIFTWOOD_TRAPDOOR, ModBlocks.PALM_TRAPDOOR);
        addAll(BlockTags.WOODEN_BUTTONS, ModBlocks.DRIFTWOOD_BUTTON, ModBlocks.PALM_BUTTON);
        addAll(BlockTags.WOODEN_PRESSURE_PLATES, ModBlocks.DRIFTWOOD_PRESSURE_PLATE, ModBlocks.PALM_PRESSURE_PLATE);
        addAll(BlockTags.WOODEN_FENCES, ModBlocks.DRIFTWOOD_FENCE, ModBlocks.PALM_FENCE);
        addAll(BlockTags.LEAVES, ModBlocks.DRIFTWOOD_LEAVES, ModBlocks.PALM_LEAVES, ModBlocks.PALM_COCONUT_LEAVES);
        addAll(BlockTags.SAPLINGS, ModBlocks.DRIFTWOOD_SAPLING, ModBlocks.PALM_SAPLING);
        addAll(BlockTags.MINEABLE_WITH_AXE,
                ModBlocks.DRIFTWOOD_LOG, ModBlocks.DRIFTWOOD_WOOD, ModBlocks.STRIPPED_DRIFTWOOD_LOG, ModBlocks.STRIPPED_DRIFTWOOD_WOOD,
                ModBlocks.DRIFTWOOD_PLANKS, ModBlocks.DRIFTWOOD_STAIRS, ModBlocks.DRIFTWOOD_SLAB, ModBlocks.DRIFTWOOD_FENCE,
                ModBlocks.DRIFTWOOD_FENCE_GATE, ModBlocks.DRIFTWOOD_DOOR, ModBlocks.DRIFTWOOD_TRAPDOOR, ModBlocks.DRIFTWOOD_BUTTON,
                ModBlocks.DRIFTWOOD_PRESSURE_PLATE,
                ModBlocks.PALM_LOG, ModBlocks.PALM_WOOD, ModBlocks.STRIPPED_PALM_LOG, ModBlocks.STRIPPED_PALM_WOOD,
                ModBlocks.PALM_PLANKS, ModBlocks.PALM_STAIRS, ModBlocks.PALM_SLAB, ModBlocks.PALM_FENCE,
                ModBlocks.PALM_FENCE_GATE, ModBlocks.PALM_DOOR, ModBlocks.PALM_TRAPDOOR, ModBlocks.PALM_BUTTON,
                ModBlocks.PALM_PRESSURE_PLATE, ModBlocks.ITEM_CATCHER);
        addAll(BlockTags.MINEABLE_WITH_HOE, ModBlocks.DRIFTWOOD_LEAVES, ModBlocks.PALM_LEAVES, ModBlocks.PALM_COCONUT_LEAVES,
                ModBlocks.PALM_THATCH, ModBlocks.PALM_THATCH_STAIRS, ModBlocks.PALM_THATCH_SLAB,
                ModBlocks.SAIL_CANVAS, ModBlocks.SAIL_CANVAS_RED_STRIPED, ModBlocks.ROPE_COIL);

        // ---- raft decoration and survival blocks ----
        addAll(ModTags.Blocks.RAFT_MOD_BLOCK,
                ModBlocks.PALM_THATCH, ModBlocks.PALM_THATCH_STAIRS, ModBlocks.PALM_THATCH_SLAB,
                ModBlocks.RAFT_FOUNDATION, ModBlocks.REINFORCED_FOUNDATION, ModBlocks.SCRAP_METAL_BLOCK, ModBlocks.PLASTIC_BLOCK,
                ModBlocks.ROPE_LADDER, ModBlocks.ROPE_COIL, ModBlocks.SAIL_CANVAS, ModBlocks.SAIL_CANVAS_RED_STRIPED,
                ModBlocks.SHIP_WHEEL, ModBlocks.SHARK_JAW_TROPHY, ModBlocks.RAFT_FLAG, ModBlocks.COCONUT_LANTERN,
                ModBlocks.CROP_PLOT, ModBlocks.SIMPLE_GRILL, ModBlocks.WATER_PURIFIER, ModBlocks.SCARECROW);
        addAll(BlockTags.MINEABLE_WITH_AXE,
                ModBlocks.RAFT_FOUNDATION, ModBlocks.REINFORCED_FOUNDATION, ModBlocks.ROPE_LADDER, ModBlocks.SHIP_WHEEL,
                ModBlocks.SHARK_JAW_TROPHY, ModBlocks.RAFT_FLAG, ModBlocks.COCONUT_LANTERN, ModBlocks.CROP_PLOT, ModBlocks.SCARECROW);
        addAll(BlockTags.MINEABLE_WITH_PICKAXE,
                ModBlocks.SCRAP_METAL_BLOCK, ModBlocks.PLASTIC_BLOCK, ModBlocks.SIMPLE_GRILL, ModBlocks.WATER_PURIFIER);
        addAll(BlockTags.NEEDS_STONE_TOOL, ModBlocks.SCRAP_METAL_BLOCK);
        addAll(BlockTags.CLIMBABLE, ModBlocks.ROPE_LADDER);
        addAll(BlockTags.WOODEN_SLABS, ModBlocks.PALM_THATCH_SLAB);
        addAll(BlockTags.SLABS, ModBlocks.PALM_THATCH_SLAB);
        addAll(BlockTags.STAIRS, ModBlocks.PALM_THATCH_STAIRS);
        // The crop plot is farmland in every way that matters to crops.
        addAll(BlockTags.SUPPORTS_CROPS, ModBlocks.CROP_PLOT);
        addAll(BlockTags.GROWS_CROPS, ModBlocks.CROP_PLOT);
    }
}
