package net.kasax.raft.content.food.block;

import net.kasax.raft.Raft;
import net.kasax.raft.content.food.FoodContent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * The dishes of the cooking pot. Each recipe is a set of up to four ingredients (one item each, any order); the pot
 * cooks for 10 seconds over heat and then holds a number of servings, ladled out with bowls.
 */
public final class PotRecipes {
    public record Need(String name, Predicate<ItemStack> test) {
    }

    public record Recipe(Supplier<Item> result, int servings, List<Need> needs) {
    }

    public static final TagKey<Item> SEAFOOD = TagKey.create(Registries.ITEM, Raft.id("pot_seafood"));
    public static final List<Recipe> RECIPES = new ArrayList<>();

    private static Need item(Supplier<Item> item) {
        return new Need("item", s -> s.is(item.get()));
    }

    private static Need raft(String id) {
        return new Need(id, s -> s.is(BuiltInRegistries.ITEM.getValue(Raft.id(id))));
    }

    static {
        RECIPES.add(new Recipe(() -> FoodContent.ISLAND_CURRY, 3, List.of(item(() -> FoodContent.COOKED_RICE),
                item(() -> FoodContent.CHILI_PEPPER), item(() -> FoodContent.SWEET_POTATO), raft("coconut"))));
        RECIPES.add(new Recipe(() -> FoodContent.SEAFOOD_CHOWDER, 3, List.of(new Need("seafood", s -> s.is(SEAFOOD)),
                item(() -> FoodContent.SWEET_POTATO), new Need("kelp", s -> s.is(Items.KELP) || s.is(Items.DRIED_KELP)),
                item(() -> FoodContent.SEA_GRAPES))));
        RECIPES.add(new Recipe(() -> FoodContent.SWEET_POTATO_SOUP, 2, List.of(item(() -> FoodContent.SWEET_POTATO),
                item(() -> FoodContent.SWEET_POTATO), item(() -> FoodContent.CHILI_PEPPER))));
    }

    private PotRecipes() {
    }

    public static boolean isIngredient(ItemStack stack) {
        for (Recipe r : RECIPES) {
            for (Need n : r.needs()) {
                if (n.test().test(stack)) return true;
            }
        }
        return false;
    }

    /** The recipe made of exactly these ingredients, or null. */
    public static Recipe match(List<ItemStack> ingredients) {
        List<ItemStack> items = ingredients.stream().filter(s -> !s.isEmpty()).toList();
        for (Recipe r : RECIPES) {
            if (r.needs().size() == items.size() && assign(r.needs(), items, 0, new boolean[items.size()])) return r;
        }
        return null;
    }

    private static boolean assign(List<Need> needs, List<ItemStack> items, int i, boolean[] used) {
        if (i == needs.size()) return true;
        for (int k = 0; k < items.size(); k++) {
            if (!used[k] && needs.get(i).test().test(items.get(k))) {
                used[k] = true;
                if (assign(needs, items, i + 1, used)) return true;
                used[k] = false;
            }
        }
        return false;
    }
}
