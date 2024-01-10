package net.kasax.raft.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.kasax.raft.item.ModItems;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;

public class ModRecipeProvider extends FabricRecipeProvider {
    public ModRecipeProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generate(Consumer<RecipeJsonProvider> exporter) {
        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ModItems.ANCIENT_ENERGY, 1)
                .pattern("SSS")
                .pattern("S S")
                .pattern("SSS")
                .input('S', ModItems.ANCIENT_BROKEN_RING)
                .criterion(hasItem(ModItems.ANCIENT_BROKEN_RING), conditionsFromItem(ModItems.ANCIENT_BROKEN_RING))
                .offerTo(exporter, new Identifier(getRecipeName(ModItems.ANCIENT_ENERGY)));

    }
}
