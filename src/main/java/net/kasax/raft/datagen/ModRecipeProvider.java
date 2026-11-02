package net.kasax.raft.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.kasax.raft.block.ModBlocks;
import net.kasax.raft.block.cable.RaftCable;
import net.kasax.raft.config.FurnaceData;
import net.kasax.raft.item.ModItems;
import net.kasax.raft.util.FurnaceBlocks;
import net.kasax.raft.util.ModTags;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ItemLike;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends FabricRecipeProvider {
    public ModRecipeProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, BootstrapContext<Recipe<?>> recipeOutput,
                                                  BootstrapContext<Advancement> advancementOutput) {
        return new RecipeProvider(recipeOutput, advancementOutput) {
            private void conversion(ItemLike result, ItemLike from, int count, String id) {
                shapeless(RecipeCategory.MISC, result, count)
                        .requires(from)
                        .unlockedBy(getHasName(from), has(from))
                        .save(output, id);
            }

            private void woodSet(String name, ItemLike planks, ItemLike slab, ItemLike stairs, ItemLike door, ItemLike trapdoor,
                                 ItemLike fence, ItemLike fenceGate, ItemLike button, ItemLike pressurePlate,
                                 ItemLike log, ItemLike wood, ItemLike strippedLog, ItemLike strippedWood, net.minecraft.tags.TagKey<Item> logs) {
                Ingredient plank = Ingredient.of(planks);
                String has = getHasName(planks);
                doorBuilder(door, plank).unlockedBy(has, has(planks)).save(output);
                slab(RecipeCategory.BUILDING_BLOCKS, slab, planks);
                stairBuilder(stairs, plank).unlockedBy(has, has(planks)).save(output);
                pressurePlate(pressurePlate, planks);
                fenceBuilder(fence, plank).unlockedBy(has, has(planks)).save(output);
                fenceGateBuilder(fenceGate, plank).unlockedBy(has, has(planks)).save(output);
                trapdoorBuilder(trapdoor, plank).unlockedBy(has, has(planks)).save(output);
                buttonBuilder(button, plank).unlockedBy(has, has(planks)).save(output);
                woodFromLogs(wood, log);
                woodFromLogs(strippedWood, strippedLog);
                planksFromLogs(planks, logs, 4);
            }

            @Override
            public void buildRecipes() {
                shaped(RecipeCategory.MISC, ModBlocks.DRIFTWOOD_PORTAL_FRAME)
                        .pattern(" S ")
                        .pattern("SRS")
                        .pattern(" S ")
                        .define('S', ModItems.ANCIENT_ENERGY)
                        .define('R', ModBlocks.DRIFTWOOD_LOG)
                        .unlockedBy(getHasName(ModItems.ANCIENT_BROKEN_RING), has(ModItems.ANCIENT_BROKEN_RING))
                        .unlockedBy(getHasName(ModBlocks.DRIFTWOOD_LOG), has(ModBlocks.DRIFTWOOD_LOG))
                        .save(output);

                shaped(RecipeCategory.MISC, ModItems.TITANIUM_INGOT)
                        .pattern("SSS").pattern("SSS").pattern("SSS")
                        .define('S', ModItems.TITANIUM_NUGGET)
                        .unlockedBy(getHasName(ModItems.TITANIUM_NUGGET), has(ModItems.TITANIUM_NUGGET))
                        .save(output);

                shaped(RecipeCategory.MISC, ModBlocks.TITANIUM_BLOCK)
                        .pattern("SSS").pattern("SSS").pattern("SSS")
                        .define('S', ModItems.TITANIUM_INGOT)
                        .unlockedBy(getHasName(ModItems.TITANIUM_INGOT), has(ModItems.TITANIUM_INGOT))
                        .save(output);

                shaped(RecipeCategory.MISC, ModBlocks.RAW_TITANIUM_BLOCK)
                        .pattern("SSS").pattern("SSS").pattern("SSS")
                        .define('S', ModItems.RAW_TITANIUM)
                        .unlockedBy(getHasName(ModItems.RAW_TITANIUM), has(ModItems.RAW_TITANIUM))
                        .save(output);

                shaped(RecipeCategory.MISC, ModBlocks.TITANIUM_GRATES)
                        .pattern("S S").pattern(" S ").pattern("S S")
                        .define('S', ModItems.TITANIUM_INGOT)
                        .unlockedBy(getHasName(ModItems.TITANIUM_INGOT), has(ModItems.TITANIUM_INGOT))
                        .save(output);

                shaped(RecipeCategory.MISC, ModBlocks.ITEM_CATCHER)
                        .pattern("SSS").pattern("S S").pattern("SSS")
                        .define('S', ModBlocks.DRIFTWOOD_LOG)
                        .unlockedBy(getHasName(ModBlocks.DRIFTWOOD_LOG), has(ModBlocks.DRIFTWOOD_LOG))
                        .save(output);

                shaped(RecipeCategory.MISC, ModItems.NET)
                        .pattern("S S").pattern("SSS").pattern("S S")
                        .define('S', Items.STRING)
                        .unlockedBy(getHasName(Items.STRING), has(Items.STRING))
                        .save(output);

                shaped(RecipeCategory.MISC, ModItems.ENERGY_STAFF)
                        .pattern(" S ").pattern(" R ").pattern(" D ")
                        .define('S', ModItems.ANCIENT_ENERGY)
                        .define('R', ModItems.ANCIENT_CRYSTAL)
                        .define('D', Items.STICK)
                        .unlockedBy(getHasName(ModItems.ANCIENT_ENERGY), has(ModItems.ANCIENT_ENERGY))
                        .unlockedBy(getHasName(ModItems.ANCIENT_CRYSTAL), has(ModItems.ANCIENT_CRYSTAL))
                        .unlockedBy(getHasName(Items.STICK), has(Items.STICK))
                        .save(output);

                shaped(RecipeCategory.COMBAT, ModItems.TITANIUM_HELMET)
                        .pattern("SSS").pattern("S S")
                        .define('S', ModItems.TITANIUM_INGOT)
                        .unlockedBy(getHasName(ModItems.TITANIUM_INGOT), has(ModItems.TITANIUM_INGOT))
                        .save(output);
                shaped(RecipeCategory.COMBAT, ModItems.TITANIUM_CHESTPLATE)
                        .pattern("S S").pattern("SSS").pattern("SSS")
                        .define('S', ModItems.TITANIUM_INGOT)
                        .unlockedBy(getHasName(ModItems.TITANIUM_INGOT), has(ModItems.TITANIUM_INGOT))
                        .save(output);
                shaped(RecipeCategory.COMBAT, ModItems.TITANIUM_LEGGINGS)
                        .pattern("SSS").pattern("S S").pattern("S S")
                        .define('S', ModItems.TITANIUM_INGOT)
                        .unlockedBy(getHasName(ModItems.TITANIUM_INGOT), has(ModItems.TITANIUM_INGOT))
                        .save(output);
                shaped(RecipeCategory.COMBAT, ModItems.TITANIUM_BOOTS)
                        .pattern("S S").pattern("S S")
                        .define('S', ModItems.TITANIUM_INGOT)
                        .unlockedBy(getHasName(ModItems.TITANIUM_INGOT), has(ModItems.TITANIUM_INGOT))
                        .save(output);

                shaped(RecipeCategory.MISC, ModItems.CIRCUIT_BOARD)
                        .pattern("BCB").pattern("SSS")
                        .define('S', ModItems.PLASTIC)
                        .define('B', Items.COPPER_INGOT)
                        .define('C', Items.SLIME_BALL)
                        .unlockedBy(getHasName(ModItems.PLASTIC), has(ModItems.PLASTIC))
                        .unlockedBy(getHasName(Items.COPPER_INGOT), has(Items.COPPER_INGOT))
                        .unlockedBy(getHasName(Items.SLIME_BALL), has(Items.SLIME_BALL))
                        .save(output);

                shaped(RecipeCategory.MISC, ModItems.RAFT_TELEPORTER)
                        .pattern(" S ").pattern("BSB").pattern("SSS")
                        .define('S', ModItems.TITANIUM_INGOT)
                        .define('B', ModItems.ANCIENT_ENERGY)
                        .unlockedBy(getHasName(ModItems.TITANIUM_INGOT), has(ModItems.TITANIUM_INGOT))
                        .unlockedBy(getHasName(ModItems.ANCIENT_ENERGY), has(ModItems.ANCIENT_ENERGY))
                        .save(output);

                shaped(RecipeCategory.MISC, ModItems.METAL_DETECTOR)
                        .pattern("SBS").pattern(" S ").pattern("SSS")
                        .define('S', ModItems.TITANIUM_INGOT)
                        .define('B', ModItems.CIRCUIT_BOARD)
                        .unlockedBy(getHasName(ModItems.TITANIUM_INGOT), has(ModItems.TITANIUM_INGOT))
                        .unlockedBy(getHasName(ModItems.CIRCUIT_BOARD), has(ModItems.CIRCUIT_BOARD))
                        .save(output);

                shaped(RecipeCategory.MISC, ModBlocks.MAKESHIFT_BATTERY)
                        .pattern("CSC").pattern("CBC").pattern("CSC")
                        .define('S', ModItems.CIRCUIT_BOARD)
                        .define('C', Items.IRON_INGOT)
                        .define('B', Items.REDSTONE)
                        .unlockedBy(getHasName(ModItems.CIRCUIT_BOARD), has(ModItems.CIRCUIT_BOARD))
                        .unlockedBy(getHasName(Items.IRON_INGOT), has(Items.IRON_INGOT))
                        .unlockedBy(getHasName(Items.REDSTONE), has(Items.REDSTONE))
                        .save(output);

                shaped(RecipeCategory.MISC, ModBlocks.MAKESHIFT_SOLAR_PANEL)
                        .pattern("SSS").pattern("CCC").pattern("CBC")
                        .define('S', ModItems.CIRCUIT_BOARD)
                        .define('C', Items.IRON_INGOT)
                        .define('B', Items.REDSTONE)
                        .unlockedBy(getHasName(ModItems.CIRCUIT_BOARD), has(ModItems.CIRCUIT_BOARD))
                        .unlockedBy(getHasName(Items.IRON_INGOT), has(Items.IRON_INGOT))
                        .unlockedBy(getHasName(Items.REDSTONE), has(Items.REDSTONE))
                        .save(output);

                shaped(RecipeCategory.MISC, ModBlocks.QUARRY)
                        .pattern("BSB").pattern("STS").pattern("BCB")
                        .define('S', ModItems.CIRCUIT_BOARD)
                        .define('C', Items.DIAMOND_PICKAXE)
                        .define('B', Items.IRON_BLOCK)
                        .define('T', ModItems.TITANIUM_INGOT)
                        .unlockedBy(getHasName(ModItems.CIRCUIT_BOARD), has(ModItems.CIRCUIT_BOARD))
                        .unlockedBy(getHasName(Items.DIAMOND_PICKAXE), has(Items.DIAMOND_PICKAXE))
                        .unlockedBy(getHasName(Items.IRON_BLOCK), has(Items.IRON_BLOCK))
                        .unlockedBy(getHasName(ModItems.TITANIUM_INGOT), has(ModItems.TITANIUM_INGOT))
                        .save(output);

                shaped(RecipeCategory.MISC, ModBlocks.CHUNK_DESTROYER)
                        .pattern("BSB").pattern("STS").pattern("BCB")
                        .define('S', ModItems.CIRCUIT_BOARD)
                        .define('C', Items.DIAMOND_PICKAXE)
                        .define('B', ModBlocks.TITANIUM_BLOCK)
                        .define('T', ModBlocks.QUARRY)
                        .unlockedBy(getHasName(ModItems.CIRCUIT_BOARD), has(ModItems.CIRCUIT_BOARD))
                        .unlockedBy(getHasName(Items.DIAMOND_PICKAXE), has(Items.DIAMOND_PICKAXE))
                        .unlockedBy(getHasName(ModBlocks.TITANIUM_BLOCK), has(ModBlocks.TITANIUM_BLOCK))
                        .unlockedBy(getHasName(ModBlocks.QUARRY), has(ModBlocks.QUARRY))
                        .save(output);

                conversion(ModItems.TITANIUM_NUGGET, ModItems.TITANIUM_INGOT, 9, "raft:titanium_nugget_from_titanium_ingot");
                conversion(ModItems.TITANIUM_INGOT, ModBlocks.TITANIUM_BLOCK, 9, "raft:titanium_ingot_from_titanium_block");
                conversion(ModItems.RAW_TITANIUM, ModBlocks.RAW_TITANIUM_BLOCK, 9, "raft:raw_titanium_from_raw_titanium_block");

                woodSet("driftwood", ModBlocks.DRIFTWOOD_PLANKS, ModBlocks.DRIFTWOOD_SLAB, ModBlocks.DRIFTWOOD_STAIRS,
                        ModBlocks.DRIFTWOOD_DOOR, ModBlocks.DRIFTWOOD_TRAPDOOR, ModBlocks.DRIFTWOOD_FENCE, ModBlocks.DRIFTWOOD_FENCE_GATE,
                        ModBlocks.DRIFTWOOD_BUTTON, ModBlocks.DRIFTWOOD_PRESSURE_PLATE,
                        ModBlocks.DRIFTWOOD_LOG, ModBlocks.DRIFTWOOD_WOOD, ModBlocks.STRIPPED_DRIFTWOOD_LOG, ModBlocks.STRIPPED_DRIFTWOOD_WOOD,
                        ModTags.Items.DRIFTWOOD_LOGS);
                woodSet("palm", ModBlocks.PALM_PLANKS, ModBlocks.PALM_SLAB, ModBlocks.PALM_STAIRS,
                        ModBlocks.PALM_DOOR, ModBlocks.PALM_TRAPDOOR, ModBlocks.PALM_FENCE, ModBlocks.PALM_FENCE_GATE,
                        ModBlocks.PALM_BUTTON, ModBlocks.PALM_PRESSURE_PLATE,
                        ModBlocks.PALM_LOG, ModBlocks.PALM_WOOD, ModBlocks.STRIPPED_PALM_LOG, ModBlocks.STRIPPED_PALM_WOOD,
                        ModTags.Items.PALM_LOGS);

                shaped(RecipeCategory.FOOD, ModItems.PINA_COLADA)
                        .pattern(" S ").pattern(" C ").pattern(" B ")
                        .define('S', Items.SUGAR)
                        .define('C', ModItems.COCONUT)
                        .define('B', Items.BOWL)
                        .unlockedBy(getHasName(Items.SUGAR), has(Items.SUGAR))
                        .unlockedBy(getHasName(ModItems.COCONUT), has(ModItems.COCONUT))
                        .unlockedBy(getHasName(Items.BOWL), has(Items.BOWL))
                        .save(output);

                oreSmelting(List.of(ModItems.ANCIENT_BROKEN_RING), RecipeCategory.MISC, CookingBookCategory.MISC,
                        ModItems.ANCIENT_ENERGY, 0.7f, 200, "misc");
                oreBlasting(List.of(ModItems.ANCIENT_BROKEN_RING), RecipeCategory.MISC, CookingBookCategory.MISC,
                        ModItems.ANCIENT_ENERGY, 0.7f, 100, "misc");
                oreSmelting(List.of(ModItems.RAW_TITANIUM), RecipeCategory.MISC, CookingBookCategory.MISC,
                        ModItems.TITANIUM_INGOT, 0.7f, 200, "misc");
                oreBlasting(List.of(ModItems.RAW_TITANIUM), RecipeCategory.MISC, CookingBookCategory.MISC,
                        ModItems.TITANIUM_INGOT, 0.7f, 100, "misc");
                oreSmelting(List.of(ModItems.UNREFINED_OCEAN_GARBAGE), RecipeCategory.MISC, CookingBookCategory.MISC,
                        ModItems.TRASH_CUBE, 0.7f, 200, "misc");

                // Furnaces
                FurnaceData.furnaceData.forEach(data -> {
                    String furnaceName = data.getName();

                    if ("brick_furnace".equals(furnaceName)) {
                        shaped(RecipeCategory.MISC, FurnaceBlocks.getFurnaceByName(furnaceName))
                                .pattern("BBB").pattern("BFB").pattern("BAB")
                                .define('B', Items.BRICK)
                                .define('F', Items.FURNACE)
                                .define('A', ModItems.ANCIENT_ENERGY)
                                .unlockedBy(getHasName(Items.FURNACE), has(Items.FURNACE))
                                .unlockedBy(getHasName(Items.BRICK), has(Items.BRICK))
                                .unlockedBy(getHasName(ModItems.ANCIENT_ENERGY), has(ModItems.ANCIENT_ENERGY))
                                .save(output);
                    }

                    if ("titanium_furnace".equals(furnaceName)) {
                        ItemLike brickFurnace = FurnaceBlocks.getFurnaceByName("brick_furnace");
                        shaped(RecipeCategory.MISC, FurnaceBlocks.getFurnaceByName(furnaceName))
                                .pattern("TTT").pattern("TBT").pattern("TTT")
                                .define('T', ModBlocks.TITANIUM_BLOCK)
                                .define('B', brickFurnace)
                                .unlockedBy(getHasName(ModBlocks.TITANIUM_BLOCK), has(ModBlocks.TITANIUM_BLOCK))
                                .unlockedBy(getHasName(brickFurnace), has(brickFurnace))
                                .save(output);
                    }
                });

                // Cables
                for (RaftCable.Cables cable : RaftCable.Cables.values()) {
                    if ("copper_cable".equals(cable.name)) {
                        shaped(RecipeCategory.REDSTONE, cable.block, 6)
                                .pattern("CCC").pattern("RRR")
                                .define('C', Items.COPPER_INGOT)
                                .define('R', Items.REDSTONE)
                                .unlockedBy(getHasName(Items.COPPER_INGOT), has(Items.COPPER_INGOT))
                                .save(output);
                    }

                    if ("titanium_cable".equals(cable.name)) {
                        shaped(RecipeCategory.REDSTONE, cable.block, 6)
                                .pattern("TTT").pattern("RRR")
                                .define('T', ModItems.TITANIUM_INGOT)
                                .define('R', Items.REDSTONE)
                                .unlockedBy(getHasName(ModItems.TITANIUM_INGOT), has(ModItems.TITANIUM_INGOT))
                                .save(output);
                    }
                }
            }
        };
    }

    @Override
    public String getName() {
        return "Raft Recipes";
    }
}
