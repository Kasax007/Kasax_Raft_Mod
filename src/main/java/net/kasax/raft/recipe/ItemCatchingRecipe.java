package net.kasax.raft.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

/** The item catcher turns a net into ocean garbage. The recipe type is {@code raft:item_catching}. */
public class ItemCatchingRecipe implements Recipe<SingleRecipeInput> {
    private final Ingredient ingredient;
    private final ItemStack output;

    public ItemCatchingRecipe(Ingredient ingredient, ItemStack output) {
        this.ingredient = ingredient;
        this.output = output;
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return output;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public String getGroup() {
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

    public Ingredient ingredient() {
        return ingredient;
    }

    public ItemStack output() {
        return output;
    }

    public static final MapCodec<ItemCatchingRecipe> CODEC = RecordCodecBuilder.mapCodec(in -> in.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(ItemCatchingRecipe::ingredient),
            ItemStack.STRICT_CODEC.fieldOf("output").forGetter(ItemCatchingRecipe::output)
    ).apply(in, ItemCatchingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ItemCatchingRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, ItemCatchingRecipe::ingredient,
            ItemStack.STREAM_CODEC, ItemCatchingRecipe::output,
            ItemCatchingRecipe::new);
}
