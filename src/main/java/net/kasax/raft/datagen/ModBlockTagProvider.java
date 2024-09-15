package net.kasax.raft.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.kasax.raft.block.ModBlocks;
import net.kasax.raft.util.FurnaceBlocks;
import net.kasax.raft.util.ModTags;
import net.minecraft.block.Block;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends FabricTagProvider.BlockTagProvider {
    public ModBlockTagProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup arg) {
        getOrCreateTagBuilder(ModTags.Blocks.RAFT_MOD_BLOCK)
                .add(ModBlocks.DRIFTWOOD_PORTAL_FRAME)
                .add(ModBlocks.DRIFTWOOD_LEAVES)
                .add(ModBlocks.DRIFTWOOD_LOG)
                .add(ModBlocks.DRIFTWOOD_WOOD)
                .add(ModBlocks.DRIFTWOOD_PLANKS)
                .add(ModBlocks.DRIFTWOOD_STAIRS)
                .add(ModBlocks.DRIFTWOOD_SLAB)
                .add(ModBlocks.DRIFTWOOD_BUTTON)
                .add(ModBlocks.DRIFTWOOD_PRESSURE_PLATE)
                .add(ModBlocks.DRIFTWOOD_FENCE)
                .add(ModBlocks.DRIFTWOOD_FENCE_GATE)
                .add(ModBlocks.DRIFTWOOD_DOOR)
                .add(ModBlocks.DRIFTWOOD_TRAPDOOR)
                .add(ModBlocks.PALM_STAIRS)
                .add(ModBlocks.PALM_SLAB)
                .add(ModBlocks.PALM_BUTTON)
                .add(ModBlocks.PALM_PRESSURE_PLATE)
                .add(ModBlocks.PALM_FENCE)
                .add(ModBlocks.PALM_FENCE_GATE)
                .add(ModBlocks.PALM_DOOR)
                .add(ModBlocks.PALM_TRAPDOOR)
                .add(ModBlocks.STRIPPED_DRIFTWOOD_LOG)
                .add(ModBlocks.STRIPPED_DRIFTWOOD_WOOD)
                .add(ModBlocks.TITANIUM_BLOCK)
                .add(ModBlocks.RAW_TITANIUM_BLOCK)
                .add(ModBlocks.TITANIUM_ORE)
                .add(ModBlocks.DEEPSLATE_TITANIUM_ORE)
                .add(ModBlocks.TITANIUM_GRATES)
                .add(ModBlocks.ITEM_CATCHER)
                .add(ModBlocks.MAKESHIFT_SOLAR_PANEL)
                .add(ModBlocks.MAKESHIFT_BATTERY)
                .add(ModBlocks.QUARRY);

        // Add all furnaces to the RAFT_MOD_BLOCK tag
        FurnaceBlocks.getFurnaces().forEach(furnace ->
                getOrCreateTagBuilder(ModTags.Blocks.RAFT_MOD_BLOCK).add(furnace)
        );

        getOrCreateTagBuilder(BlockTags.AXE_MINEABLE)
                .add(ModBlocks.DRIFTWOOD_PORTAL_FRAME);

        getOrCreateTagBuilder(BlockTags.PICKAXE_MINEABLE)
                .add(ModBlocks.RAW_TITANIUM_BLOCK)
                .add(ModBlocks.TITANIUM_ORE)
                .add(ModBlocks.TITANIUM_BLOCK)
                .add(ModBlocks.DEEPSLATE_TITANIUM_ORE)
                .add(ModBlocks.TITANIUM_GRATES)
                .add(ModBlocks.MAKESHIFT_SOLAR_PANEL)
                .add(ModBlocks.QUARRY)
                .add(ModBlocks.CHUNK_DESTROYER)
                .add(ModBlocks.MAKESHIFT_BATTERY);

        // Add all furnaces to the PICKAXE_MINEBALE tag
        FurnaceBlocks.getFurnaces().forEach(furnace ->
                getOrCreateTagBuilder(BlockTags.PICKAXE_MINEABLE).add(furnace)
        );

        getOrCreateTagBuilder(BlockTags.NEEDS_IRON_TOOL)
                .add(ModBlocks.DRIFTWOOD_PORTAL_FRAME)
                .add(ModBlocks.MAKESHIFT_SOLAR_PANEL)
                .add(ModBlocks.QUARRY)
                .add(ModBlocks.CHUNK_DESTROYER)
                .add(ModBlocks.MAKESHIFT_BATTERY);

        // Add all furnaces to the NEEDS_IRON_TOOL tag
        FurnaceBlocks.getFurnaces().forEach(furnace ->
                getOrCreateTagBuilder(BlockTags.NEEDS_IRON_TOOL).add(furnace)
        );

        getOrCreateTagBuilder(BlockTags.NEEDS_DIAMOND_TOOL)
                .add(ModBlocks.RAW_TITANIUM_BLOCK)
                .add(ModBlocks.TITANIUM_ORE)
                .add(ModBlocks.DEEPSLATE_TITANIUM_ORE)
                .add(ModBlocks.TITANIUM_BLOCK)
                .add(ModBlocks.TITANIUM_GRATES);

        getOrCreateTagBuilder(BlockTags.LOGS_THAT_BURN)
                .add(ModBlocks.DRIFTWOOD_LOG)
                .add(ModBlocks.DRIFTWOOD_WOOD)
                .add(ModBlocks.STRIPPED_DRIFTWOOD_WOOD)
                .add(ModBlocks.STRIPPED_DRIFTWOOD_LOG);

        getOrCreateTagBuilder(BlockTags.LOGS_THAT_BURN)
                .add(ModBlocks.PALM_LOG)
                .add(ModBlocks.PALM_WOOD)
                .add(ModBlocks.STRIPPED_PALM_WOOD)
                .add(ModBlocks.STRIPPED_PALM_LOG);

        getOrCreateTagBuilder(BlockTags.FENCES)
                .add(ModBlocks.DRIFTWOOD_FENCE);
        getOrCreateTagBuilder(BlockTags.FENCE_GATES)
                .add(ModBlocks.DRIFTWOOD_FENCE_GATE);
        getOrCreateTagBuilder(BlockTags.FENCES)
                .add(ModBlocks.PALM_FENCE);
        getOrCreateTagBuilder(BlockTags.FENCE_GATES)
                .add(ModBlocks.PALM_FENCE_GATE);
    }
}
