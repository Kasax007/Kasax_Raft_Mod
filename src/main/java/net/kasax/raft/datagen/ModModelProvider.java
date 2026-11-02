package net.kasax.raft.datagen;

import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.kasax.raft.block.ModBlocks;
import net.kasax.raft.block.cable.RaftCable;
import net.kasax.raft.item.ModItems;
import net.kasax.raft.util.FurnaceBlocks;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TexturedModel;

public class ModModelProvider extends FabricModelProvider {
    public ModModelProvider(FabricPackOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockModelGenerators generators) {
        generators.createTrivialCube(ModBlocks.DRIFTWOOD_PORTAL_FRAME);

        generators.woodProvider(ModBlocks.DRIFTWOOD_LOG).log(ModBlocks.DRIFTWOOD_LOG).wood(ModBlocks.DRIFTWOOD_WOOD);
        generators.woodProvider(ModBlocks.STRIPPED_DRIFTWOOD_LOG).log(ModBlocks.STRIPPED_DRIFTWOOD_LOG).wood(ModBlocks.STRIPPED_DRIFTWOOD_WOOD);
        generators.createTrivialBlock(ModBlocks.DRIFTWOOD_LEAVES, TexturedModel.LEAVES);
        BlockModelGenerators.BlockFamilyProvider driftwoodPool = generators.family(ModBlocks.DRIFTWOOD_PLANKS);

        generators.woodProvider(ModBlocks.PALM_LOG).log(ModBlocks.PALM_LOG).wood(ModBlocks.PALM_WOOD);
        generators.woodProvider(ModBlocks.STRIPPED_PALM_LOG).log(ModBlocks.STRIPPED_PALM_LOG).wood(ModBlocks.STRIPPED_PALM_WOOD);
        generators.createTrivialBlock(ModBlocks.PALM_LEAVES, TexturedModel.LEAVES);
        generators.createTrivialBlock(ModBlocks.PALM_COCONUT_LEAVES, TexturedModel.LEAVES);
        BlockModelGenerators.BlockFamilyProvider palmwoodPool = generators.family(ModBlocks.PALM_PLANKS);

        generators.createTrivialBlock(ModBlocks.MAKESHIFT_BATTERY, TexturedModel.CUBE_BOTTOM_TOP);
        generators.createTrivialBlock(ModBlocks.MAKESHIFT_SOLAR_PANEL, TexturedModel.CUBE_BOTTOM_TOP);
        generators.createHorizontallyRotatedBlock(ModBlocks.QUARRY, TexturedModel.ORIENTABLE);
        generators.createHorizontallyRotatedBlock(ModBlocks.CHUNK_DESTROYER, TexturedModel.ORIENTABLE);

        generators.createTrivialCube(ModBlocks.TITANIUM_BLOCK);
        generators.createTrivialCube(ModBlocks.TITANIUM_ORE);
        generators.createTrivialCube(ModBlocks.DEEPSLATE_TITANIUM_ORE);
        generators.createTrivialCube(ModBlocks.RAW_TITANIUM_BLOCK);
        generators.createTrivialCube(ModBlocks.TITANIUM_GRATES);
        generators.createCrossBlockWithDefaultItem(ModBlocks.DRIFTWOOD_SAPLING, BlockModelGenerators.PlantType.NOT_TINTED);
        generators.createCrossBlockWithDefaultItem(ModBlocks.PALM_SAPLING, BlockModelGenerators.PlantType.NOT_TINTED);

        driftwoodPool.stairs(ModBlocks.DRIFTWOOD_STAIRS);
        driftwoodPool.slab(ModBlocks.DRIFTWOOD_SLAB);
        driftwoodPool.button(ModBlocks.DRIFTWOOD_BUTTON);
        driftwoodPool.pressurePlate(ModBlocks.DRIFTWOOD_PRESSURE_PLATE);
        driftwoodPool.fence(ModBlocks.DRIFTWOOD_FENCE);
        driftwoodPool.fenceGate(ModBlocks.DRIFTWOOD_FENCE_GATE);

        generators.createDoor(ModBlocks.DRIFTWOOD_DOOR);
        generators.createTrapdoor(ModBlocks.DRIFTWOOD_TRAPDOOR);

        palmwoodPool.stairs(ModBlocks.PALM_STAIRS);
        palmwoodPool.slab(ModBlocks.PALM_SLAB);
        palmwoodPool.button(ModBlocks.PALM_BUTTON);
        palmwoodPool.pressurePlate(ModBlocks.PALM_PRESSURE_PLATE);
        palmwoodPool.fence(ModBlocks.PALM_FENCE);
        palmwoodPool.fenceGate(ModBlocks.PALM_FENCE_GATE);

        generators.createDoor(ModBlocks.PALM_DOOR);
        generators.createTrapdoor(ModBlocks.PALM_TRAPDOOR);

        // decoration and utility blocks of the raft survival set
        generators.createTrivialCube(ModBlocks.RAFT_FOUNDATION);
        generators.createTrivialCube(ModBlocks.REINFORCED_FOUNDATION);
        generators.createTrivialCube(ModBlocks.SCRAP_METAL_BLOCK);
        generators.createTrivialCube(ModBlocks.PLASTIC_BLOCK);
        generators.createTrivialCube(ModBlocks.SAIL_CANVAS);
        generators.createTrivialCube(ModBlocks.SAIL_CANVAS_RED_STRIPED);
        BlockModelGenerators.BlockFamilyProvider thatchPool = generators.family(ModBlocks.PALM_THATCH);
        thatchPool.stairs(ModBlocks.PALM_THATCH_STAIRS);
        thatchPool.slab(ModBlocks.PALM_THATCH_SLAB);

        // Models for all custom furnaces
        FurnaceBlocks.getFurnaces().forEach(furnace ->
                generators.createFurnace(furnace, TexturedModel.ORIENTABLE_ONLY_TOP));
    }

    @Override
    public void generateItemModels(ItemModelGenerators generators) {
        generators.generateFlatItem(ModItems.ANCIENT_BROKEN_RING, ModelTemplates.FLAT_ITEM);
        generators.generateFlatItem(ModItems.ANCIENT_ENERGY, ModelTemplates.FLAT_ITEM);
        generators.generateFlatItem(ModItems.ANCIENT_CRYSTAL, ModelTemplates.FLAT_ITEM);
        generators.generateFlatItem(ModItems.PLASTIC, ModelTemplates.FLAT_ITEM);
        generators.generateFlatItem(ModItems.CIRCUIT_BOARD, ModelTemplates.FLAT_ITEM);
        generators.generateFlatItem(ModItems.TRASH_CUBE, ModelTemplates.FLAT_ITEM);
        generators.generateFlatItem(ModItems.UNREFINED_OCEAN_GARBAGE, ModelTemplates.FLAT_ITEM);
        generators.generateFlatItem(ModItems.COCONUT, ModelTemplates.FLAT_ITEM);
        generators.generateFlatItem(ModItems.PINA_COLADA, ModelTemplates.FLAT_ITEM);

        generators.generateFlatItem(ModItems.RAW_TITANIUM, ModelTemplates.FLAT_ITEM);
        generators.generateFlatItem(ModItems.TITANIUM_INGOT, ModelTemplates.FLAT_ITEM);
        generators.generateFlatItem(ModItems.TITANIUM_NUGGET, ModelTemplates.FLAT_ITEM);

        generators.generateFlatItem(ModItems.LOFI_BEAT_MUSIC_DISC, ModelTemplates.FLAT_ITEM);
        generators.generateFlatItem(ModItems.CRYSTAL_CASTLES_MUSIC_DISC, ModelTemplates.FLAT_ITEM);
        generators.generateFlatItem(ModItems.SOME_GAME_MUSIC_MUSIC_DISC, ModelTemplates.FLAT_ITEM);

        generators.generateFlatItem(ModItems.NET, ModelTemplates.FLAT_ITEM);

        generators.generateFlatItem(ModItems.ROPE, ModelTemplates.FLAT_ITEM);
        generators.generateFlatItem(ModItems.SCRAP_METAL, ModelTemplates.FLAT_ITEM);
        generators.generateFlatItem(ModItems.SHARK_TOOTH, ModelTemplates.FLAT_ITEM);
        generators.generateFlatItem(ModItems.SHARK_MEAT, ModelTemplates.FLAT_ITEM);
        generators.generateFlatItem(ModItems.COOKED_SHARK_MEAT, ModelTemplates.FLAT_ITEM);
        generators.generateFlatItem(ModItems.FRESH_WATER, ModelTemplates.FLAT_ITEM);
        generators.generateFlatItem(ModItems.COCONUT_MILK, ModelTemplates.FLAT_ITEM);
        generators.generateFlatItem(ModItems.RECEIVER, ModelTemplates.FLAT_ITEM);
        generators.generateFlatItem(ModItems.PLASTIC_HOOK, ModelTemplates.FLAT_HANDHELD_ITEM);
        generators.generateFlatItem(ModItems.PADDLE, ModelTemplates.FLAT_HANDHELD_ITEM);
        generators.generateFlatItem(ModItems.SHARK_SPAWN_EGG, ModelTemplates.FLAT_ITEM);

        generators.generateFlatItem(ModItems.TITANIUM_HELMET, ModelTemplates.FLAT_ITEM);
        generators.generateFlatItem(ModItems.TITANIUM_CHESTPLATE, ModelTemplates.FLAT_ITEM);
        generators.generateFlatItem(ModItems.TITANIUM_LEGGINGS, ModelTemplates.FLAT_ITEM);
        generators.generateFlatItem(ModItems.TITANIUM_BOOTS, ModelTemplates.FLAT_ITEM);

        // These items have hand written (Blockbench) models under resources/assets/raft/models/item.
        generators.declareCustomModelItem(ModItems.METAL_DETECTOR);
        generators.declareCustomModelItem(ModItems.RAFT_TELEPORTER);
        generators.declareCustomModelItem(ModBlocks.ITEM_CATCHER.asItem());
        for (RaftCable.Cables cable : RaftCable.Cables.values()) {
            generators.declareCustomModelItem(cable.asItem());
        }
        // Block items whose models are written by hand (models/item/<name>.json).
        for (var block : new net.minecraft.world.level.block.Block[] {
                ModBlocks.ROPE_LADDER, ModBlocks.ROPE_COIL, ModBlocks.SHIP_WHEEL, ModBlocks.SHARK_JAW_TROPHY, ModBlocks.RAFT_FLAG,
                ModBlocks.COCONUT_LANTERN, ModBlocks.CROP_PLOT, ModBlocks.SIMPLE_GRILL, ModBlocks.WATER_PURIFIER, ModBlocks.SCARECROW}) {
            generators.declareCustomModelItem(block.asItem());
        }
        // The energy staff has a 3D model in the hand and a flat sprite in the GUI:
        // resources/assets/raft/items/energy_staff.json (select on display_context).
    }

    @Override
    public String getName() {
        return "Raft Model Definitions";
    }
}
