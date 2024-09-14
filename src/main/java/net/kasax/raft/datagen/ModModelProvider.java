package net.kasax.raft.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.kasax.raft.block.ModBlocks;
import net.kasax.raft.item.ModItems;
import net.kasax.raft.util.FurnaceBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.data.client.*;
import net.minecraft.item.ArmorItem;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

import static net.minecraft.data.client.TextureMap.getSubId;

public class ModModelProvider extends FabricModelProvider {
    public ModModelProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockStateModelGenerator blockStateModelGenerator) {
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.DRIFTWOOD_PORTAL_FRAME);

        blockStateModelGenerator.registerLog(ModBlocks.DRIFTWOOD_LOG).log(ModBlocks.DRIFTWOOD_LOG).wood(ModBlocks.DRIFTWOOD_WOOD);
        blockStateModelGenerator.registerLog(ModBlocks.STRIPPED_DRIFTWOOD_LOG).log(ModBlocks.STRIPPED_DRIFTWOOD_LOG).wood(ModBlocks.STRIPPED_DRIFTWOOD_WOOD);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.DRIFTWOOD_LEAVES);
        BlockStateModelGenerator.BlockTexturePool driftwoodPool = blockStateModelGenerator.registerCubeAllModelTexturePool(ModBlocks.DRIFTWOOD_PLANKS);


        blockStateModelGenerator.registerLog(ModBlocks.PALM_LOG).log(ModBlocks.PALM_LOG).wood(ModBlocks.PALM_WOOD);
        blockStateModelGenerator.registerLog(ModBlocks.STRIPPED_PALM_LOG).log(ModBlocks.STRIPPED_PALM_LOG).wood(ModBlocks.STRIPPED_PALM_WOOD);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.PALM_LEAVES);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.PALM_COCONUT_LEAVES);
        BlockStateModelGenerator.BlockTexturePool palmwoodPool = blockStateModelGenerator.registerCubeAllModelTexturePool(ModBlocks.PALM_PLANKS);

        //blockStateModelGenerator.registerCubeAllModelTexturePool(ModBlocks.MAKESHIFT_SOLAR_PANEL);
        blockStateModelGenerator.registerSingleton(ModBlocks.MAKESHIFT_BATTERY, TexturedModel.CUBE_BOTTOM_TOP);
        blockStateModelGenerator.registerSingleton(ModBlocks.MAKESHIFT_SOLAR_PANEL, TexturedModel.CUBE_BOTTOM_TOP);

        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.TITANIUM_BLOCK);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.TITANIUM_ORE);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.DEEPSLATE_TITANIUM_ORE);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.RAW_TITANIUM_BLOCK);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.TITANIUM_GRATES);
        blockStateModelGenerator.registerTintableCross(ModBlocks.DRIFTWOOD_SAPLING, BlockStateModelGenerator.TintType.NOT_TINTED);
        blockStateModelGenerator.registerTintableCross(ModBlocks.PALM_SAPLING, BlockStateModelGenerator.TintType.NOT_TINTED);

        driftwoodPool.stairs(ModBlocks.DRIFTWOOD_STAIRS);
        driftwoodPool.slab(ModBlocks.DRIFTWOOD_SLAB);
        driftwoodPool.button(ModBlocks.DRIFTWOOD_BUTTON);
        driftwoodPool.pressurePlate(ModBlocks.DRIFTWOOD_PRESSURE_PLATE);
        driftwoodPool.fence(ModBlocks.DRIFTWOOD_FENCE);
        driftwoodPool.fenceGate(ModBlocks.DRIFTWOOD_FENCE_GATE);

        blockStateModelGenerator.registerDoor(ModBlocks.DRIFTWOOD_DOOR);
        blockStateModelGenerator.registerTrapdoor(ModBlocks.DRIFTWOOD_TRAPDOOR);

        palmwoodPool.stairs(ModBlocks.PALM_STAIRS);
        palmwoodPool.slab(ModBlocks.PALM_SLAB);
        palmwoodPool.button(ModBlocks.PALM_BUTTON);
        palmwoodPool.pressurePlate(ModBlocks.PALM_PRESSURE_PLATE);
        palmwoodPool.fence(ModBlocks.PALM_FENCE);
        palmwoodPool.fenceGate(ModBlocks.PALM_FENCE_GATE);

        blockStateModelGenerator.registerDoor(ModBlocks.PALM_DOOR);
        blockStateModelGenerator.registerTrapdoor(ModBlocks.PALM_TRAPDOOR);

        // Register models for all custom furnaces
        FurnaceBlocks.getFurnaces().forEach(furnace ->
                blockStateModelGenerator.registerCooker(furnace, TexturedModel.ORIENTABLE)
        );
    }

    @Override
    public void generateItemModels(ItemModelGenerator itemModelGenerator) {
        itemModelGenerator.register(ModItems.ANCIENT_BROKEN_RING, Models.GENERATED);
        itemModelGenerator.register(ModItems.ANCIENT_ENERGY, Models.GENERATED);
        itemModelGenerator.register(ModItems.ANCIENT_CRYSTAL, Models.GENERATED);
        itemModelGenerator.register(ModItems.PLASTIC, Models.GENERATED);
        itemModelGenerator.register(ModItems.CIRCUIT_BOARD, Models.GENERATED);
        itemModelGenerator.register(ModItems.TRASH_CUBE, Models.GENERATED);
        itemModelGenerator.register(ModItems.UNREFINED_OCEAN_GARBAGE, Models.GENERATED);
        itemModelGenerator.register(ModItems.COCONUT, Models.GENERATED);
        itemModelGenerator.register(ModItems.PINA_COLADA, Models.GENERATED);

        itemModelGenerator.register(ModItems.RAW_TITANIUM, Models.GENERATED);
        itemModelGenerator.register(ModItems.TITANIUM_INGOT, Models.GENERATED);
        itemModelGenerator.register(ModItems.TITANIUM_NUGGET, Models.GENERATED);

        itemModelGenerator.register(ModItems.LOFI_BEAT_MUSIC_DISC, Models.GENERATED);
        itemModelGenerator.register(ModItems.SOME_GAME_MUSIC_MUSIC_DISC, Models.GENERATED);

        itemModelGenerator.register(ModItems.NET, Models.GENERATED);

        itemModelGenerator.registerArmor(((ArmorItem) ModItems.TITANIUM_HELMET));
        itemModelGenerator.registerArmor(((ArmorItem) ModItems.TITANIUM_CHESTPLATE));
        itemModelGenerator.registerArmor(((ArmorItem) ModItems.TITANIUM_LEGGINGS));
        itemModelGenerator.registerArmor(((ArmorItem) ModItems.TITANIUM_BOOTS));

    }
    public static TextureMap registerSolarPanel(Block block) {
        return (new TextureMap()).put(TextureKey.PARTICLE, getSubId(ModBlocks.MAKESHIFT_SOLAR_PANEL, "_side")).put(TextureKey.BOTTOM, getSubId(ModBlocks.MAKESHIFT_SOLAR_PANEL, "_bottom")).put(TextureKey.TOP, getSubId(ModBlocks.MAKESHIFT_SOLAR_PANEL, "_top")).put(TextureKey.SIDE, getSubId(ModBlocks.MAKESHIFT_SOLAR_PANEL, "_side"));
    }
}
