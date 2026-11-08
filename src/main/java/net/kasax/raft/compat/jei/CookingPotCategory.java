package net.kasax.raft.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.placement.HorizontalAlignment;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.RecipeType;
import net.kasax.raft.Raft;
import net.kasax.raft.content.food.FoodContent;
import net.kasax.raft.content.food.block.CookingPotBlockEntity;
import net.kasax.raft.content.food.block.PotRecipes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI page of a cooking pot dish: the ingredients (any order), the dish with its servings, the heat sources the pot
 * can stand on, and how long it cooks.
 */
public class CookingPotCategory extends AbstractRecipeCategory<PotRecipes.Recipe> {
    public static final RecipeType<PotRecipes.Recipe> TYPE = RecipeType.create(Raft.MOD_ID, "cooking_pot", PotRecipes.Recipe.class);
    private static final int W = 150, H = 64;

    public CookingPotCategory(IGuiHelper gui) {
        super(TYPE, Component.translatable("jei.raft.cooking_pot"), gui.createDrawableItemLike(FoodContent.COOKING_POT), W, H);
    }

    /** Items that heat the pot: the blocks of {@code raft:pot_heat_sources}, lava as a bucket, fire as flint and steel. */
    static List<ItemStack> heatSources() {
        List<ItemStack> out = new ArrayList<>();
        BuiltInRegistries.BLOCK.getTagOrEmpty(CookingPotBlockEntity.HEAT).forEach(h -> {
            Block b = h.value();
            Item item = b == Blocks.LAVA ? Items.LAVA_BUCKET : b == Blocks.FIRE ? Items.FLINT_AND_STEEL : b.asItem();
            if (item != Items.AIR && out.stream().noneMatch(s -> s.is(item))) out.add(new ItemStack(item));
        });
        return out;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, PotRecipes.Recipe recipe, IFocusGroup focuses) {
        List<PotRecipes.Need> needs = recipe.needs();
        for (int i = 0; i < needs.size(); i++) {
            builder.addInputSlot(1 + (i % 2) * 18, 1 + (i / 2) * 18).setStandardSlotBackground()
                    .addItemStacks(needs.get(i).examples().get());
        }
        builder.setShapeless(W - 10, 0);
        builder.addOutputSlot(78, 10).setOutputSlotBackground().addItemStack(recipe.resultStack());
        builder.addSlot(RecipeIngredientRole.CATALYST, 1, 45).setStandardSlotBackground().addItemStacks(heatSources());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, PotRecipes.Recipe recipe, IFocusGroup focuses) {
        builder.addAnimatedRecipeArrowWidget(CookingPotBlockEntity.COOK_TIME).setPosition(44, 10);
        builder.addText(Component.translatable("jei.raft.cooking_pot.servings", recipe.servings()), 50, 20)
                .setPosition(100, 9).setColor(0xFF404040);
        builder.addText(Component.translatable("jei.raft.cooking_pot.heat"), W - 24, 18)
                .setPosition(22, 46).setColor(0xFF404040);
        builder.addText(Component.translatable("jei.raft.cooking_pot.time", CookingPotBlockEntity.COOK_TIME / 20), 60, 10)
                .setPosition(40, 28).setTextAlignment(HorizontalAlignment.LEFT).setColor(0xFF6A6A6A);
    }
}
