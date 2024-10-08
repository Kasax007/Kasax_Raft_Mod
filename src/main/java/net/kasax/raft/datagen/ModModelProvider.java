package net.kasax.raft.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.kasax.raft.block.ModBlocks;
import net.kasax.raft.item.ModItems;
import net.minecraft.data.client.BlockStateModelGenerator;
import net.minecraft.data.client.ItemModelGenerator;
import net.minecraft.data.client.Models;
import net.minecraft.item.ArmorItem;
import net.minecraft.util.Identifier;

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

        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.TITANIUM_BLOCK);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.TITANIUM_ORE);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.DEEPSLATE_TITANIUM_ORE);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.RAW_TITANIUM_BLOCK);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.TITANIUM_GRATES);
        blockStateModelGenerator.registerTintableCross(ModBlocks.DRIFTWOOD_SAPLING, BlockStateModelGenerator.TintType.NOT_TINTED);

        driftwoodPool.stairs(ModBlocks.DRIFTWOOD_STAIRS);
        driftwoodPool.slab(ModBlocks.DRIFTWOOD_SLAB);
        driftwoodPool.button(ModBlocks.DRIFTWOOD_BUTTON);
        driftwoodPool.pressurePlate(ModBlocks.DRIFTWOOD_PRESSURE_PLATE);
        driftwoodPool.fence(ModBlocks.DRIFTWOOD_FENCE);
        driftwoodPool.fenceGate(ModBlocks.DRIFTWOOD_FENCE_GATE);

        blockStateModelGenerator.registerDoor(ModBlocks.DRIFTWOOD_DOOR);
        blockStateModelGenerator.registerTrapdoor(ModBlocks.DRIFTWOOD_TRAPDOOR);
    }

    @Override
    public void generateItemModels(ItemModelGenerator itemModelGenerator) {
        itemModelGenerator.register(ModItems.ANCIENT_BROKEN_RING, Models.GENERATED);
        itemModelGenerator.register(ModItems.ANCIENT_ENERGY, Models.GENERATED);
        itemModelGenerator.register(ModItems.ANCIENT_CRYSTAL, Models.GENERATED);
        itemModelGenerator.register(ModItems.PLASTIC, Models.GENERATED);
        itemModelGenerator.register(ModItems.CIRCUIT_BOARD, Models.GENERATED);
        itemModelGenerator.register(ModItems.TRASH_CUBE, Models.GENERATED);

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
}
