package net.kasax.raft.compat;

import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.kasax.raft.block.ModBlocks;
import net.kasax.raft.recipe.ItemCatchingRecipe;
import net.kasax.raft.screen.ItemCollectorScreen;


public class RaftReiClientPlugin implements REIClientPlugin {

    @Override
    public void registerCategories(CategoryRegistry registry) {
        registry.add(new ItemCatcherCategory());

        registry.addWorkstations(ItemCatcherCategory.ITEM_CATCHING, EntryStacks.of(ModBlocks.ITEM_CATCHER));
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        registry.registerRecipeFiller(ItemCatchingRecipe.class, ItemCatchingRecipe.Type.INSTANCE,
                itemCatchingRecipeRecipeEntry -> new ItemCatcherDisplay((ItemCatchingRecipe) itemCatchingRecipeRecipeEntry.value()));
    }

    @Override
    public void registerScreens(ScreenRegistry registry) {
        registry.registerClickArea(screen -> new Rectangle(75, 30, 20, 30), ItemCollectorScreen.class,
                ItemCatcherCategory.ITEM_CATCHING);
    }
}
