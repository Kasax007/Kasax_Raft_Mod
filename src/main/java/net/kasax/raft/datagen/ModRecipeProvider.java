package net.kasax.raft.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.kasax.raft.block.ModBlocks;
import net.kasax.raft.config.FurnaceData;
import net.kasax.raft.item.ModItems;
import net.kasax.raft.util.FurnaceBlocks;
import net.kasax.raft.util.ModTags;
import net.minecraft.data.server.recipe.RecipeExporter;
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.recipe.book.RecipeCategory.MISC;

public class ModRecipeProvider extends FabricRecipeProvider {
    private static final List<ItemConvertible> ENEREGY_SMELTERS = List.of(ModItems.ANCIENT_BROKEN_RING);
    private static final List<ItemConvertible> TITANIUM_SMELTERS = List.of(ModItems.RAW_TITANIUM);
    private static final List<ItemConvertible> BALL_SMELTERS = List.of(ModItems.UNREFINED_OCEAN_GARBAGE);

    public ModRecipeProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }


    @Override
    public void generate(RecipeExporter exporter) {
        ShapedRecipeJsonBuilder.create(MISC, ModBlocks.DRIFTWOOD_PORTAL_FRAME.asItem(), 1)
                .pattern(" S ")
                .pattern("SRS")
                .pattern(" S ")
                .input('S', ModItems.ANCIENT_ENERGY)
                .input('R', ModBlocks.DRIFTWOOD_LOG.asItem())
                .criterion(hasItem(ModItems.ANCIENT_BROKEN_RING), conditionsFromItem(ModItems.ANCIENT_BROKEN_RING))
                .criterion(hasItem(ModBlocks.DRIFTWOOD_LOG.asItem()), conditionsFromItem(ModBlocks.DRIFTWOOD_LOG.asItem()))
                .offerTo(exporter, Identifier.of(getRecipeName(ModBlocks.DRIFTWOOD_PORTAL_FRAME.asItem())));

        ShapedRecipeJsonBuilder.create(MISC, ModItems.TITANIUM_INGOT, 1)
                .pattern("SSS")
                .pattern("SSS")
                .pattern("SSS")
                .input('S', ModItems.TITANIUM_NUGGET)
                .criterion(hasItem(ModItems.TITANIUM_NUGGET), conditionsFromItem(ModItems.TITANIUM_NUGGET))
                .offerTo(exporter, Identifier.of(getRecipeName(ModItems.TITANIUM_INGOT)));

        ShapedRecipeJsonBuilder.create(MISC, ModBlocks.TITANIUM_BLOCK.asItem(), 1)
                .pattern("SSS")
                .pattern("SSS")
                .pattern("SSS")
                .input('S', ModItems.TITANIUM_INGOT)
                .criterion(hasItem(ModItems.TITANIUM_INGOT), conditionsFromItem(ModItems.TITANIUM_INGOT))
                .offerTo(exporter, Identifier.of(getRecipeName(ModBlocks.TITANIUM_BLOCK.asItem())));

        ShapedRecipeJsonBuilder.create(MISC, ModBlocks.RAW_TITANIUM_BLOCK.asItem(), 1)
                .pattern("SSS")
                .pattern("SSS")
                .pattern("SSS")
                .input('S', ModItems.RAW_TITANIUM)
                .criterion(hasItem(ModItems.RAW_TITANIUM), conditionsFromItem(ModItems.RAW_TITANIUM))
                .offerTo(exporter, Identifier.of(getRecipeName(ModBlocks.RAW_TITANIUM_BLOCK.asItem())));

        ShapedRecipeJsonBuilder.create(MISC, ModBlocks.TITANIUM_GRATES.asItem(), 1)
                .pattern("S S")
                .pattern(" S ")
                .pattern("S S")
                .input('S', ModItems.TITANIUM_INGOT)
                .criterion(hasItem(ModItems.TITANIUM_INGOT), conditionsFromItem(ModItems.TITANIUM_INGOT))
                .offerTo(exporter, Identifier.of(getRecipeName(ModBlocks.TITANIUM_GRATES.asItem())));

        ShapedRecipeJsonBuilder.create(MISC, ModBlocks.ITEM_CATCHER.asItem(), 1)
                .pattern("SSS")
                .pattern("S S")
                .pattern("SSS")
                .input('S', ModBlocks.DRIFTWOOD_LOG.asItem())
                .criterion(hasItem(ModBlocks.DRIFTWOOD_LOG.asItem()), conditionsFromItem(ModBlocks.DRIFTWOOD_LOG.asItem()))
                .offerTo(exporter, Identifier.of(getRecipeName(ModBlocks.ITEM_CATCHER.asItem())));

        ShapedRecipeJsonBuilder.create(MISC, ModItems.NET, 1)
                .pattern("S S")
                .pattern("SSS")
                .pattern("S S")
                .input('S', Items.STRING)
                .criterion(hasItem(Items.STRING), conditionsFromItem(Items.STRING))
                .offerTo(exporter, Identifier.of(getRecipeName(ModItems.NET)));


        ShapedRecipeJsonBuilder.create(MISC, ModItems.ENERGY_STAFF, 1)
                .pattern(" S ")
                .pattern(" R ")
                .pattern(" D ")
                .input('S', ModItems.ANCIENT_ENERGY)
                .input('R', ModItems.ANCIENT_CRYSTAL)
                .input('D', Items.STICK)
                .criterion(hasItem(ModItems.ANCIENT_ENERGY), conditionsFromItem(ModItems.ANCIENT_ENERGY))
                .criterion(hasItem(ModItems.ANCIENT_CRYSTAL), conditionsFromItem(ModItems.ANCIENT_CRYSTAL))
                .criterion(hasItem(Items.STICK), conditionsFromItem(Items.STICK))
                .offerTo(exporter, Identifier.of(getRecipeName(ModItems.ENERGY_STAFF)));

        ShapedRecipeJsonBuilder.create(MISC, ModItems.TITANIUM_HELMET, 1)
                .pattern("SSS")
                .pattern("S S")
                .pattern("   ")
                .input('S', ModItems.TITANIUM_INGOT)
                .criterion(hasItem(ModItems.TITANIUM_INGOT), conditionsFromItem(ModItems.TITANIUM_INGOT))
                .offerTo(exporter, Identifier.of(getRecipeName(ModItems.TITANIUM_HELMET)));

        ShapedRecipeJsonBuilder.create(MISC, ModItems.TITANIUM_CHESTPLATE, 1)
                .pattern("S S")
                .pattern("SSS")
                .pattern("SSS")
                .input('S', ModItems.TITANIUM_INGOT)
                .criterion(hasItem(ModItems.TITANIUM_INGOT), conditionsFromItem(ModItems.TITANIUM_INGOT))
                .offerTo(exporter, Identifier.of(getRecipeName(ModItems.TITANIUM_CHESTPLATE)));

        ShapedRecipeJsonBuilder.create(MISC, ModItems.TITANIUM_LEGGINGS, 1)
                .pattern("SSS")
                .pattern("S S")
                .pattern("S S")
                .input('S', ModItems.TITANIUM_INGOT)
                .criterion(hasItem(ModItems.TITANIUM_INGOT), conditionsFromItem(ModItems.TITANIUM_INGOT))
                .offerTo(exporter, Identifier.of(getRecipeName(ModItems.TITANIUM_LEGGINGS)));

        ShapedRecipeJsonBuilder.create(MISC, ModItems.TITANIUM_BOOTS, 1)
                .pattern("   ")
                .pattern("S S")
                .pattern("S S")
                .input('S', ModItems.TITANIUM_INGOT)
                .criterion(hasItem(ModItems.TITANIUM_INGOT), conditionsFromItem(ModItems.TITANIUM_INGOT))
                .offerTo(exporter, Identifier.of(getRecipeName(ModItems.TITANIUM_BOOTS)));

        createDoorRecipe(ModBlocks.DRIFTWOOD_DOOR.asItem(), Ingredient.ofItems(ModBlocks.DRIFTWOOD_PLANKS.asItem())).criterion(hasItem(ModBlocks.DRIFTWOOD_PLANKS.asItem()), conditionsFromItem(ModBlocks.DRIFTWOOD_PLANKS.asItem()))
                .offerTo(exporter, Identifier.of(getRecipeName(ModBlocks.DRIFTWOOD_DOOR.asItem())));
        createSlabRecipe(MISC, ModBlocks.DRIFTWOOD_SLAB.asItem(), Ingredient.ofItems(ModBlocks.DRIFTWOOD_PLANKS.asItem())).criterion(hasItem(ModBlocks.DRIFTWOOD_PLANKS.asItem()), conditionsFromItem(ModBlocks.DRIFTWOOD_PLANKS.asItem()))
                .offerTo(exporter, Identifier.of(getRecipeName(ModBlocks.DRIFTWOOD_SLAB.asItem())));
        createStairsRecipe(ModBlocks.DRIFTWOOD_STAIRS.asItem(), Ingredient.ofItems(ModBlocks.DRIFTWOOD_PLANKS.asItem())).criterion(hasItem(ModBlocks.DRIFTWOOD_PLANKS.asItem()), conditionsFromItem(ModBlocks.DRIFTWOOD_PLANKS.asItem()))
                .offerTo(exporter, Identifier.of(getRecipeName(ModBlocks.DRIFTWOOD_STAIRS.asItem())));
        createPressurePlateRecipe(MISC, ModBlocks.DRIFTWOOD_PRESSURE_PLATE.asItem(), Ingredient.ofItems(ModBlocks.DRIFTWOOD_PLANKS.asItem())).criterion(hasItem(ModBlocks.DRIFTWOOD_PLANKS.asItem()), conditionsFromItem(ModBlocks.DRIFTWOOD_PLANKS.asItem()))
                .offerTo(exporter, Identifier.of(getRecipeName(ModBlocks.DRIFTWOOD_PRESSURE_PLATE.asItem())));
        createFenceRecipe(ModBlocks.DRIFTWOOD_FENCE.asItem(), Ingredient.ofItems(ModBlocks.DRIFTWOOD_PLANKS.asItem())).criterion(hasItem(ModBlocks.DRIFTWOOD_PLANKS.asItem()), conditionsFromItem(ModBlocks.DRIFTWOOD_PLANKS.asItem()))
                .offerTo(exporter, Identifier.of(getRecipeName(ModBlocks.DRIFTWOOD_FENCE.asItem())));
        createFenceGateRecipe(ModBlocks.DRIFTWOOD_FENCE_GATE.asItem(), Ingredient.ofItems(ModBlocks.DRIFTWOOD_PLANKS.asItem())).criterion(hasItem(ModBlocks.DRIFTWOOD_PLANKS.asItem()), conditionsFromItem(ModBlocks.DRIFTWOOD_PLANKS.asItem()))
                .offerTo(exporter, Identifier.of(getRecipeName(ModBlocks.DRIFTWOOD_FENCE_GATE.asItem())));

        ShapedRecipeJsonBuilder.create(MISC, ModBlocks.DRIFTWOOD_TRAPDOOR, 2)
                .pattern("   ")
                .pattern("SSS")
                .pattern("SSS")
                .input('S', ModBlocks.DRIFTWOOD_PLANKS.asItem())
                .criterion(hasItem(ModBlocks.DRIFTWOOD_PLANKS.asItem()), conditionsFromItem(ModBlocks.DRIFTWOOD_PLANKS.asItem()))
                .offerTo(exporter, Identifier.of(getRecipeName(ModBlocks.DRIFTWOOD_TRAPDOOR.asItem())));

        ShapedRecipeJsonBuilder.create(MISC, ModItems.CIRCUIT_BOARD, 1)
                .pattern("BCB")
                .pattern("SSS")
                .pattern("   ")
                .input('S', ModItems.PLASTIC)
                .input('B', Items.COPPER_INGOT)
                .input('C', Items.SLIME_BALL)
                .criterion(hasItem(ModItems.PLASTIC), conditionsFromItem(ModItems.PLASTIC))
                .criterion(hasItem(Items.COPPER_INGOT), conditionsFromItem(Items.COPPER_INGOT))
                .criterion(hasItem(Items.SLIME_BALL), conditionsFromItem(Items.SLIME_BALL))
                .offerTo(exporter, Identifier.of(getRecipeName(ModItems.CIRCUIT_BOARD)));

        ShapedRecipeJsonBuilder.create(MISC, ModItems.RAFT_TELEPORTER, 1)
                .pattern(" S ")
                .pattern("BSB")
                .pattern("SSS")
                .input('S', ModItems.TITANIUM_INGOT)
                .input('B', ModItems.ANCIENT_ENERGY)
                .criterion(hasItem(ModItems.TITANIUM_INGOT), conditionsFromItem(ModItems.TITANIUM_INGOT))
                .criterion(hasItem(ModItems.ANCIENT_ENERGY), conditionsFromItem(ModItems.ANCIENT_ENERGY))
                .offerTo(exporter, Identifier.of(getRecipeName(ModItems.RAFT_TELEPORTER)));

        ShapedRecipeJsonBuilder.create(MISC, ModItems.METAL_DETECTOR, 1)
                .pattern("SBS")
                .pattern(" S ")
                .pattern("SSS")
                .input('S', ModItems.TITANIUM_INGOT)
                .input('B', ModItems.CIRCUIT_BOARD)
                .criterion(hasItem(ModItems.TITANIUM_INGOT), conditionsFromItem(ModItems.TITANIUM_INGOT))
                .criterion(hasItem(ModItems.CIRCUIT_BOARD), conditionsFromItem(ModItems.CIRCUIT_BOARD))
                .offerTo(exporter, Identifier.of(getRecipeName(ModItems.METAL_DETECTOR)));

        ShapedRecipeJsonBuilder.create(MISC, ModBlocks.MAKESHIFT_BATTERY, 1)
                .pattern("CSC")
                .pattern("CBC")
                .pattern("CSC")
                .input('S', ModItems.CIRCUIT_BOARD)
                .input('C', Items.IRON_INGOT)
                .input('B', Items.REDSTONE)
                .criterion(hasItem(ModItems.CIRCUIT_BOARD), conditionsFromItem(ModItems.CIRCUIT_BOARD))
                .criterion(hasItem(Items.IRON_INGOT), conditionsFromItem(Items.IRON_INGOT))
                .criterion(hasItem(Items.REDSTONE), conditionsFromItem(Items.REDSTONE))
                .offerTo(exporter, Identifier.of(getRecipeName(ModBlocks.MAKESHIFT_BATTERY.asItem())));

        ShapedRecipeJsonBuilder.create(MISC, ModBlocks.MAKESHIFT_SOLAR_PANEL, 1)
                .pattern("SSS")
                .pattern("CCC")
                .pattern("CBC")
                .input('S', ModItems.CIRCUIT_BOARD)
                .input('C', Items.IRON_INGOT)
                .input('B', Items.REDSTONE)
                .criterion(hasItem(ModItems.CIRCUIT_BOARD), conditionsFromItem(ModItems.CIRCUIT_BOARD))
                .criterion(hasItem(Items.IRON_INGOT), conditionsFromItem(Items.IRON_INGOT))
                .criterion(hasItem(Items.REDSTONE), conditionsFromItem(Items.REDSTONE))
                .offerTo(exporter, Identifier.of(getRecipeName(ModBlocks.MAKESHIFT_SOLAR_PANEL.asItem())));

        offerShapelessRecipe(exporter,ModBlocks.DRIFTWOOD_BUTTON.asItem(), ModBlocks.DRIFTWOOD_PLANKS.asItem(), "MISC4", 1);

        offerShapelessRecipe(exporter, ModItems.TITANIUM_NUGGET, ModItems.TITANIUM_INGOT, "MISC1", 9);
        offerShapelessRecipe(exporter, ModItems.TITANIUM_INGOT, ModBlocks.TITANIUM_BLOCK.asItem(), "MISC2", 9);
        offerShapelessRecipe(exporter, ModItems.RAW_TITANIUM, ModBlocks.RAW_TITANIUM_BLOCK.asItem(), "MISC3", 9);

        offerBarkBlockRecipe(exporter, ModBlocks.DRIFTWOOD_WOOD, ModBlocks.DRIFTWOOD_LOG);
        offerBarkBlockRecipe(exporter, ModBlocks.STRIPPED_DRIFTWOOD_WOOD, ModBlocks.STRIPPED_DRIFTWOOD_LOG);
        offerPlanksRecipe(exporter, ModBlocks.DRIFTWOOD_PLANKS, ModTags.Items.DRIFTWOOD_LOGS, 4);


        createDoorRecipe(ModBlocks.PALM_DOOR.asItem(), Ingredient.ofItems(ModBlocks.PALM_PLANKS.asItem())).criterion(hasItem(ModBlocks.PALM_PLANKS.asItem()), conditionsFromItem(ModBlocks.PALM_PLANKS.asItem()))
                .offerTo(exporter, Identifier.of(getRecipeName(ModBlocks.PALM_DOOR.asItem())));
        createSlabRecipe(MISC, ModBlocks.PALM_SLAB.asItem(), Ingredient.ofItems(ModBlocks.PALM_PLANKS.asItem())).criterion(hasItem(ModBlocks.PALM_PLANKS.asItem()), conditionsFromItem(ModBlocks.PALM_PLANKS.asItem()))
                .offerTo(exporter, Identifier.of(getRecipeName(ModBlocks.PALM_SLAB.asItem())));
        createStairsRecipe(ModBlocks.PALM_STAIRS.asItem(), Ingredient.ofItems(ModBlocks.PALM_PLANKS.asItem())).criterion(hasItem(ModBlocks.PALM_PLANKS.asItem()), conditionsFromItem(ModBlocks.PALM_PLANKS.asItem()))
                .offerTo(exporter, Identifier.of(getRecipeName(ModBlocks.PALM_STAIRS.asItem())));
        createPressurePlateRecipe(MISC, ModBlocks.PALM_PRESSURE_PLATE.asItem(), Ingredient.ofItems(ModBlocks.PALM_PLANKS.asItem())).criterion(hasItem(ModBlocks.PALM_PLANKS.asItem()), conditionsFromItem(ModBlocks.PALM_PLANKS.asItem()))
                .offerTo(exporter, Identifier.of(getRecipeName(ModBlocks.PALM_PRESSURE_PLATE.asItem())));
        createFenceRecipe(ModBlocks.PALM_FENCE.asItem(), Ingredient.ofItems(ModBlocks.PALM_PLANKS.asItem())).criterion(hasItem(ModBlocks.PALM_PLANKS.asItem()), conditionsFromItem(ModBlocks.PALM_PLANKS.asItem()))
                .offerTo(exporter, Identifier.of(getRecipeName(ModBlocks.PALM_FENCE.asItem())));
        createFenceGateRecipe(ModBlocks.PALM_FENCE_GATE.asItem(), Ingredient.ofItems(ModBlocks.PALM_PLANKS.asItem())).criterion(hasItem(ModBlocks.PALM_PLANKS.asItem()), conditionsFromItem(ModBlocks.PALM_PLANKS.asItem()))
                .offerTo(exporter, Identifier.of(getRecipeName(ModBlocks.PALM_FENCE_GATE.asItem())));

        ShapedRecipeJsonBuilder.create(MISC, ModBlocks.PALM_TRAPDOOR, 2)
                .pattern("   ")
                .pattern("SSS")
                .pattern("SSS")
                .input('S', ModBlocks.PALM_PLANKS.asItem())
                .criterion(hasItem(ModBlocks.PALM_PLANKS.asItem()), conditionsFromItem(ModBlocks.PALM_PLANKS.asItem()))
                .offerTo(exporter, Identifier.of(getRecipeName(ModBlocks.PALM_TRAPDOOR.asItem())));
        offerShapelessRecipe(exporter,ModBlocks.PALM_BUTTON.asItem(), ModBlocks.PALM_PLANKS.asItem(), "MISC4", 1);
        offerBarkBlockRecipe(exporter, ModBlocks.PALM_WOOD, ModBlocks.PALM_LOG);
        offerBarkBlockRecipe(exporter, ModBlocks.STRIPPED_PALM_WOOD, ModBlocks.STRIPPED_PALM_LOG);
        offerPlanksRecipe(exporter, ModBlocks.PALM_PLANKS, ModTags.Items.PALM_LOGS, 4);

        ShapedRecipeJsonBuilder.create(MISC, ModItems.PINA_COLADA, 1)
                .pattern(" S ")
                .pattern(" C ")
                .pattern(" B ")
                .input('S', Items.SUGAR)
                .input('C', ModItems.COCONUT)
                .input('B', Items.BOWL)
                .criterion(hasItem(Items.SUGAR), conditionsFromItem(Items.SUGAR))
                .criterion(hasItem(ModItems.COCONUT), conditionsFromItem(ModItems.COCONUT))
                .criterion(hasItem(Items.BOWL), conditionsFromItem(Items.BOWL))
                .offerTo(exporter, Identifier.of(getRecipeName(ModItems.PINA_COLADA)));

        offerSmelting(exporter, ENEREGY_SMELTERS, MISC, ModItems.ANCIENT_ENERGY,
                0.7f, 200, "misc");

        offerBlasting(exporter, ENEREGY_SMELTERS, MISC, ModItems.ANCIENT_ENERGY,
                0.7f, 100, "misc");

        offerSmelting(exporter, TITANIUM_SMELTERS, MISC, ModItems.TITANIUM_INGOT,
                0.7f, 200, "misc");

        offerBlasting(exporter, TITANIUM_SMELTERS, MISC, ModItems.TITANIUM_INGOT,
                0.7f, 100, "misc");

        offerSmelting(exporter, BALL_SMELTERS, MISC, ModItems.TRASH_CUBE,
                0.7f, 200, "misc");

        // Generate Furnace Recipes
        FurnaceData.furnaceData.forEach(data -> {
            String furnaceName = data.getName();

            if ("brick_furnace".equals(furnaceName)) {
                // Recipe for Brick Furnace
                ShapedRecipeJsonBuilder.create(MISC, FurnaceBlocks.getFurnaceByName(furnaceName).asItem(), 1)
                        .pattern("BBB")
                        .pattern("BFB")
                        .pattern("BAB")
                        .input('B', Items.BRICK)
                        .input('F', Items.FURNACE)
                        .input('A', ModItems.ANCIENT_ENERGY)
                        .criterion(hasItem(Items.FURNACE), conditionsFromItem(Items.FURNACE))
                        .criterion(hasItem(Items.BRICK), conditionsFromItem(Items.BRICK))
                        .criterion(hasItem(ModItems.ANCIENT_ENERGY), conditionsFromItem(ModItems.ANCIENT_ENERGY))
                        .offerTo(exporter, Identifier.of("raft", furnaceName));
            }

            if ("titanium_furnace".equals(furnaceName)) {
                // Recipe for Titanium Furnace
                ShapedRecipeJsonBuilder.create(MISC, FurnaceBlocks.getFurnaceByName(furnaceName).asItem(), 1)
                        .pattern("TTT")
                        .pattern("TBT")
                        .pattern("TTT")
                        .input('T', ModBlocks.TITANIUM_BLOCK.asItem())
                        .input('B', FurnaceBlocks.getFurnaceByName("brick_furnace").asItem())
                        .criterion(hasItem(ModBlocks.TITANIUM_BLOCK.asItem()), conditionsFromItem(ModBlocks.TITANIUM_BLOCK.asItem()))
                        .criterion(hasItem(FurnaceBlocks.getFurnaceByName("brick_furnace").asItem()), conditionsFromItem(FurnaceBlocks.getFurnaceByName("brick_furnace").asItem()))
                        .offerTo(exporter, Identifier.of("raft", furnaceName));
            }
        });


    }
}
