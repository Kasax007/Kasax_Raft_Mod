package net.kasax.raft.compat;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.kasax.raft.recipe.ItemCatchingRecipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ItemCatcherDisplay extends BasicDisplay {
    public ItemCatcherDisplay(List<EntryIngredient> inputs, List<EntryIngredient> outputs) {
        super(inputs, outputs);
    }

    public ItemCatcherDisplay(ItemCatchingRecipe recipe) {
        super(recipe.getIngredients(), recipe.getOutput(HOW GO I GET IT AS LIST HERE));
    }

    private static List<EntryIngredient> getInputList(ItemCatchingRecipe recipe) {
        if(recipe == null) return Collections.emptyList();
        List<EntryIngredient> list = new ArrayList<>();
        list.add(EntryIngredients.ofIngredient(recipe.getIngredients().get(0)));
        return list;
    }


    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return ItemCatcherCategory.ITEM_CATCHING;
    }
}
