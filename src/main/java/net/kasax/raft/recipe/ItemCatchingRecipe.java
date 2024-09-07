package net.kasax.raft.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.RecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

public class ItemCatchingRecipe implements Recipe<RecipeInput> {
    private final ItemStack output;
    private final Ingredient ingredient;

    public ItemCatchingRecipe(Ingredient ingredient, ItemStack itemStack) {
        this.output = itemStack;
        this.ingredient = ingredient;
    }

    @Override
    public boolean matches(RecipeInput input, World world) {
        if(world.isClient()) {
            return false;
        }

        return ingredient.test(input.getStackInSlot(0)); // ingredient from recipe json // inventory.getStack(0) block entity slot index
    }

    @Override
    public ItemStack craft(RecipeInput input, RegistryWrapper.WrapperLookup lookup) {
        return output.copy();
    }

    @Override
    public boolean fits(int width, int height) {
        return true;
    }


    @Override
    public ItemStack getResult(RegistryWrapper.WrapperLookup registriesLookup) {
        return output;
    }

    @Override
    public DefaultedList<Ingredient> getIngredients() {
        DefaultedList<Ingredient> list = DefaultedList.ofSize(1);
        list.add(ingredient);
        return list;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return Type.INSTANCE;
    }

    public static class Type implements RecipeType<ItemCatchingRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "item_catching";
    }

    public static class Serializer implements RecipeSerializer<ItemCatchingRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        public static final String ID = "item_catching";

        public static final MapCodec<ItemCatchingRecipe> CODEC = RecordCodecBuilder.mapCodec(in -> in.group(
                Ingredient.DISALLOW_EMPTY_CODEC.fieldOf("ingredient").forGetter(itemCatchingRecipe -> itemCatchingRecipe.ingredient),
                ItemStack.VALIDATED_CODEC.fieldOf("output").forGetter(r -> r.output)
        ).apply(in, ItemCatchingRecipe::new));

        public static final PacketCodec<RegistryByteBuf, ItemCatchingRecipe> PACKET_CODEC = PacketCodec.ofStatic(Serializer::write, Serializer::read);

        @Override
        public MapCodec<ItemCatchingRecipe> codec() {
            return CODEC;
        }

        @Override
        public PacketCodec<RegistryByteBuf, ItemCatchingRecipe> packetCodec() {
            return PACKET_CODEC;
        }

        public static ItemCatchingRecipe read(RegistryByteBuf buf) {

            Ingredient ingredient = Ingredient.PACKET_CODEC.decode(buf);

            ItemStack output = ItemStack.PACKET_CODEC.decode(buf);

            return new ItemCatchingRecipe(ingredient, output);
        }


        public static void write(RegistryByteBuf buf, ItemCatchingRecipe recipe) {

            Ingredient.PACKET_CODEC.encode(buf, recipe.ingredient);

            ItemStack.PACKET_CODEC.encode(buf, recipe.output);
        }
    }
}