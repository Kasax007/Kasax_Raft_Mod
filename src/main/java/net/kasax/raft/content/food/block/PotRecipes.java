package net.kasax.raft.content.food.block;

import net.kasax.raft.Raft;
import net.kasax.raft.content.food.FoodContent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
 *
 * <p>The pot only takes an ingredient when the ingredients in it plus the new one still fit at least one recipe
 * ({@link #canAdd}), so it can never hold a mix that makes nothing; {@link #candidates} tells the player what the
 * contents can still become and {@link #missing} what is left to add. The same lists feed the JEI page of the pot.
 */
public final class PotRecipes {
    /** One ingredient of a recipe: its name in messages, the test, and the items JEI shows for it. */
    public record Need(Supplier<Component> label, Predicate<ItemStack> test, Supplier<List<ItemStack>> examples) {
    }

    /** A dish: result, servings, ingredients and the colour of the stew in the pot (RGB). */
    public record Recipe(String id, Supplier<Item> result, int servings, int color, List<Need> needs) {
        public ItemStack resultStack() {
            return new ItemStack(result.get(), servings);
        }
    }

    public static final TagKey<Item> SEAFOOD = TagKey.create(Registries.ITEM, Raft.id("pot_seafood"));
    public static final List<Recipe> RECIPES = new ArrayList<>();
    /** Colour of plain water in the pot (RGB). */
    public static final int WATER = 0x3F76E4;
    /** Colour of a pot whose contents make no dish (only from worlds before 3.4). */
    public static final int MURKY = 0x6E6A48;

    private static Need item(Supplier<Item> item) {
        return new Need(() -> new ItemStack(item.get()).getHoverName(), s -> s.is(item.get()), () -> List.of(new ItemStack(item.get())));
    }

    private static Need raft(String id) {
        Supplier<Item> item = () -> BuiltInRegistries.ITEM.get(Raft.id(id));
        return new Need(() -> new ItemStack(item.get()).getHoverName(), s -> s.is(item.get()), () -> List.of(new ItemStack(item.get())));
    }

    private static List<ItemStack> tagItems(TagKey<Item> tag) {
        List<ItemStack> out = new ArrayList<>();
        BuiltInRegistries.ITEM.getTagOrEmpty(tag).forEach(h -> out.add(new ItemStack(h)));
        return out;
    }

    static {
        RECIPES.add(new Recipe("island_curry", () -> FoodContent.ISLAND_CURRY, 3, 0xD98A2B, List.of(
                item(() -> FoodContent.COOKED_RICE), item(() -> FoodContent.CHILI_PEPPER),
                item(() -> FoodContent.SWEET_POTATO), raft("coconut"))));
        RECIPES.add(new Recipe("seafood_chowder", () -> FoodContent.SEAFOOD_CHOWDER, 3, 0xE6DCC2, List.of(
                new Need(() -> Component.translatable("message.raft.pot.seafood"), s -> s.is(SEAFOOD), () -> tagItems(SEAFOOD)),
                item(() -> FoodContent.SWEET_POTATO),
                new Need(() -> new ItemStack(Items.KELP).getHoverName(), s -> s.is(Items.KELP) || s.is(Items.DRIED_KELP),
                        () -> List.of(new ItemStack(Items.KELP), new ItemStack(Items.DRIED_KELP))),
                item(() -> FoodContent.SEA_GRAPES))));
        RECIPES.add(new Recipe("sweet_potato_soup", () -> FoodContent.SWEET_POTATO_SOUP, 2, 0xE5772C, List.of(
                item(() -> FoodContent.SWEET_POTATO), item(() -> FoodContent.SWEET_POTATO),
                item(() -> FoodContent.CHILI_PEPPER))));
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

    private static List<ItemStack> present(List<ItemStack> ingredients) {
        return ingredients.stream().filter(s -> !s.isEmpty()).toList();
    }

    /** The recipe made of exactly these ingredients, or null. */
    public static Recipe match(List<ItemStack> ingredients) {
        List<ItemStack> items = present(ingredients);
        for (Recipe r : RECIPES) {
            if (r.needs().size() == items.size() && fits(r, items)) return r;
        }
        return null;
    }

    /** The recipe whose dish this is, or null. */
    public static Recipe byResult(ItemStack dish) {
        for (Recipe r : RECIPES) {
            if (dish.is(r.result().get())) return r;
        }
        return null;
    }

    /** Recipes these ingredients can still become (each item covers a different ingredient of the recipe). */
    public static List<Recipe> candidates(List<ItemStack> ingredients) {
        List<ItemStack> items = present(ingredients);
        List<Recipe> out = new ArrayList<>();
        for (Recipe r : RECIPES) {
            if (items.size() <= r.needs().size() && fits(r, items)) out.add(r);
        }
        return out;
    }

    /** Whether the pot may take this item: with it, the contents still fit at least one recipe. */
    public static boolean canAdd(List<ItemStack> ingredients, ItemStack stack) {
        List<ItemStack> items = new ArrayList<>(present(ingredients));
        items.add(stack.copyWithCount(1));
        return !candidates(items).isEmpty();
    }

    /** The ingredients of the recipe that are not in the pot yet. */
    public static List<Need> missing(Recipe recipe, List<ItemStack> ingredients) {
        List<ItemStack> items = present(ingredients);
        int[] owner = new int[recipe.needs().size()];
        java.util.Arrays.fill(owner, -1);
        if (!assign(recipe.needs(), items, 0, owner)) return recipe.needs();
        List<Need> out = new ArrayList<>();
        for (int n = 0; n < owner.length; n++) {
            if (owner[n] < 0) out.add(recipe.needs().get(n));
        }
        return out;
    }

    /** "a, b and c" for messages. */
    public static Component list(List<Component> parts) {
        MutableComponent out = Component.empty();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) out.append(i == parts.size() - 1 ? Component.translatable("message.raft.pot.and") : Component.literal(", "));
            out.append(parts.get(i));
        }
        return out;
    }

    private static boolean fits(Recipe r, List<ItemStack> items) {
        int[] owner = new int[r.needs().size()];
        java.util.Arrays.fill(owner, -1);
        return assign(r.needs(), items, 0, owner);
    }

    /** Gives every item its own need (owner[need] = item index); backtracking, at most 4 x 4. */
    private static boolean assign(List<Need> needs, List<ItemStack> items, int i, int[] owner) {
        if (i == items.size()) return true;
        for (int n = 0; n < needs.size(); n++) {
            if (owner[n] < 0 && needs.get(n).test().test(items.get(i))) {
                owner[n] = i;
                if (assign(needs, items, i + 1, owner)) return true;
                owner[n] = -1;
            }
        }
        return false;
    }
}
