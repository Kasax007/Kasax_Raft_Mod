package net.kasax.raft.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/** The item catcher turns a net into ocean garbage. The recipe type is {@code raft:item_catching}. */
public class ItemCatchingRecipe implements Recipe<SingleRecipeInput> {
    private final Ingredient ingredient;
    private final ItemStackTemplate output;
    private @Nullable PlacementInfo placementInfo;

    public ItemCatchingRecipe(Ingredient ingredient, ItemStackTemplate output) {
        this.ingredient = ingredient;
        this.output = output;
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input) {
        return output.create();
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public RecipeSerializer<ItemCatchingRecipe> getSerializer() {
        return ModRecipes.ITEM_CATCHING_SERIALIZER;
    }

    @Override
    public RecipeType<ItemCatchingRecipe> getType() {
        return ModRecipes.ITEM_CATCHING_TYPE;
    }

    @Override
    public PlacementInfo placementInfo() {
        if (placementInfo == null) {
            placementInfo = PlacementInfo.create(ingredient);
        }
        return placementInfo;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    public Ingredient ingredient() {
        return ingredient;
    }

    public ItemStackTemplate output() {
        return output;
    }

    public static final MapCodec<ItemCatchingRecipe> CODEC = RecordCodecBuilder.mapCodec(in -> in.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(ItemCatchingRecipe::ingredient),
            ItemStackTemplate.CODEC.fieldOf("output").forGetter(ItemCatchingRecipe::output)
    ).apply(in, ItemCatchingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ItemCatchingRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, ItemCatchingRecipe::ingredient,
            ItemStackTemplate.STREAM_CODEC, ItemCatchingRecipe::output,
            ItemCatchingRecipe::new);
}
