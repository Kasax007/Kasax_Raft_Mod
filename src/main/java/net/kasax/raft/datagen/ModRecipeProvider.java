package net.kasax.raft.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.kasax.raft.block.ModBlocks;
import net.kasax.raft.item.ModItems;
import net.kasax.raft.util.ModTags;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.Items;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.feature.MiscConfiguredFeatures;

import java.util.List;
import java.util.function.Consumer;

public class ModRecipeProvider extends FabricRecipeProvider {
    private static final List<ItemConvertible> ENEREGY_SMELTERS = List.of(ModItems.ANCIENT_BROKEN_RING);
    private static final List<ItemConvertible> TITANIUM_SMELTERS = List.of(ModItems.RAW_TITANIUM);
    public ModRecipeProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generate(Consumer<RecipeJsonProvider> exporter) {
        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ModBlocks.DRIFTWOOD_PORTAL_FRAME.asItem(), 1)
                .pattern(" S ")
                .pattern("SRS")
                .pattern(" S ")
                .input('S', ModItems.ANCIENT_ENERGY)
                .input('R', ModBlocks.DRIFTWOOD_LOG.asItem())
                .criterion(hasItem(ModItems.ANCIENT_BROKEN_RING), conditionsFromItem(ModItems.ANCIENT_BROKEN_RING))
                .criterion(hasItem(ModBlocks.DRIFTWOOD_LOG.asItem()), conditionsFromItem(ModBlocks.DRIFTWOOD_LOG.asItem()))
                .offerTo(exporter, new Identifier(getRecipeName(ModBlocks.DRIFTWOOD_PORTAL_FRAME.asItem())));

        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ModItems.TITANIUM_INGOT, 1)
                .pattern("SSS")
                .pattern("SSS")
                .pattern("SSS")
                .input('S', ModItems.TITANIUM_NUGGET)
                .criterion(hasItem(ModItems.TITANIUM_NUGGET), conditionsFromItem(ModItems.TITANIUM_NUGGET))
                .offerTo(exporter, new Identifier(getRecipeName(ModItems.TITANIUM_INGOT)));

        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ModBlocks.TITANIUM_BLOCK.asItem(), 1)
                .pattern("SSS")
                .pattern("SSS")
                .pattern("SSS")
                .input('S', ModItems.TITANIUM_INGOT)
                .criterion(hasItem(ModItems.TITANIUM_INGOT), conditionsFromItem(ModItems.TITANIUM_INGOT))
                .offerTo(exporter, new Identifier(getRecipeName(ModBlocks.TITANIUM_BLOCK.asItem())));

        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ModBlocks.RAW_TITANIUM_BLOCK.asItem(), 1)
                .pattern("SSS")
                .pattern("SSS")
                .pattern("SSS")
                .input('S', ModItems.RAW_TITANIUM)
                .criterion(hasItem(ModItems.RAW_TITANIUM), conditionsFromItem(ModItems.RAW_TITANIUM))
                .offerTo(exporter, new Identifier(getRecipeName(ModBlocks.RAW_TITANIUM_BLOCK.asItem())));

        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ModBlocks.TITANIUM_GRATES.asItem(), 1)
                .pattern("S S")
                .pattern(" S ")
                .pattern("S S")
                .input('S', ModItems.TITANIUM_INGOT)
                .criterion(hasItem(ModItems.TITANIUM_INGOT), conditionsFromItem(ModItems.TITANIUM_INGOT))
                .offerTo(exporter, new Identifier(getRecipeName(ModBlocks.TITANIUM_GRATES.asItem())));

        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ModBlocks.ITEM_CATCHER.asItem(), 1)
                .pattern("SSS")
                .pattern("S S")
                .pattern("SSS")
                .input('S', ModBlocks.DRIFTWOOD_LOG.asItem())
                .criterion(hasItem(ModBlocks.DRIFTWOOD_LOG.asItem()), conditionsFromItem(ModBlocks.DRIFTWOOD_LOG.asItem()))
                .offerTo(exporter, new Identifier(getRecipeName(ModBlocks.ITEM_CATCHER.asItem())));

        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ModItems.NET, 1)
                .pattern("S S")
                .pattern("SSS")
                .pattern("S S")
                .input('S', Items.STRING)
                .criterion(hasItem(Items.STRING), conditionsFromItem(Items.STRING))
                .offerTo(exporter, new Identifier(getRecipeName(ModItems.NET)));


        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ModItems.ENERGY_STAFF, 1)
                .pattern(" S ")
                .pattern(" R ")
                .pattern(" D ")
                .input('S', ModItems.ANCIENT_ENERGY)
                .input('R', ModItems.ANCIENT_CRYSTAL)
                .input('D', Items.STICK)
                .criterion(hasItem(ModItems.ANCIENT_ENERGY), conditionsFromItem(ModItems.ANCIENT_ENERGY))
                .criterion(hasItem(ModItems.ANCIENT_CRYSTAL), conditionsFromItem(ModItems.ANCIENT_CRYSTAL))
                .criterion(hasItem(Items.STICK), conditionsFromItem(Items.STICK))
                .offerTo(exporter, new Identifier(getRecipeName(ModItems.ENERGY_STAFF)));

        offerShapelessRecipe(exporter, ModItems.TITANIUM_NUGGET, ModItems.TITANIUM_INGOT, "MISC1", 9);
        offerShapelessRecipe(exporter, ModItems.TITANIUM_INGOT, ModBlocks.TITANIUM_BLOCK.asItem(), "MISC2", 9);
        offerShapelessRecipe(exporter, ModItems.RAW_TITANIUM, ModBlocks.RAW_TITANIUM_BLOCK.asItem(), "MISC3", 9);

        offerBarkBlockRecipe(exporter, ModBlocks.DRIFTWOOD_WOOD, ModBlocks.DRIFTWOOD_LOG);
        offerBarkBlockRecipe(exporter, ModBlocks.STRIPPED_DRIFTWOOD_WOOD, ModBlocks.STRIPPED_DRIFTWOOD_LOG);
        offerPlanksRecipe(exporter, ModBlocks.DRIFTWOOD_PLANKS, ModTags.Items.DRIFTWOOD_LOGS, 4);

        offerSmelting(exporter, ENEREGY_SMELTERS, RecipeCategory.MISC, ModItems.ANCIENT_ENERGY,
                0.7f, 200, "misc");

        offerBlasting(exporter, ENEREGY_SMELTERS, RecipeCategory.MISC, ModItems.ANCIENT_ENERGY,
                0.7f, 100, "misc");

        offerSmelting(exporter, TITANIUM_SMELTERS, RecipeCategory.MISC, ModItems.TITANIUM_INGOT,
                0.7f, 200, "misc");

        offerBlasting(exporter, TITANIUM_SMELTERS, RecipeCategory.MISC, ModItems.TITANIUM_INGOT,
                0.7f, 100, "misc");
    }
}
