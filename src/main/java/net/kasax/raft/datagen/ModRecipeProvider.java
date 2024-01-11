package net.kasax.raft.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.kasax.raft.block.ModBlocks;
import net.kasax.raft.item.ModItems;
import net.kasax.raft.util.ModTags;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder;
import net.minecraft.item.ItemConvertible;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.function.Consumer;

public class ModRecipeProvider extends FabricRecipeProvider {
    private static final List<ItemConvertible> ENEREGY_SMELTERS = List.of(ModItems.ANCIENT_BROKEN_RING);
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

        offerBarkBlockRecipe(exporter, ModBlocks.DRIFTWOOD_WOOD, ModBlocks.DRIFTWOOD_LOG);
        offerBarkBlockRecipe(exporter, ModBlocks.STRIPPED_DRIFTWOOD_WOOD, ModBlocks.STRIPPED_DRIFTWOOD_LOG);
        offerPlanksRecipe(exporter, ModBlocks.DRIFTWOOD_PLANKS, ModTags.Items.DRIFTWOOD_LOGS, 4);

        offerSmelting(exporter, ENEREGY_SMELTERS, RecipeCategory.MISC, ModItems.ANCIENT_ENERGY,
                0.7f, 200, "misc");

        offerBlasting(exporter, ENEREGY_SMELTERS, RecipeCategory.MISC, ModItems.ANCIENT_ENERGY,
                0.7f, 100, "misc");
    }
}
