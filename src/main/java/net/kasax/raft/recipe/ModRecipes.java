package net.kasax.raft.recipe;

import net.kasax.raft.Raft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

public class ModRecipes {
    public static final String ITEM_CATCHING_ID = "item_catching";

    public static final RecipeType<ItemCatchingRecipe> ITEM_CATCHING_TYPE = Registry.register(
            BuiltInRegistries.RECIPE_TYPE, Raft.id(ITEM_CATCHING_ID), new RecipeType<ItemCatchingRecipe>() {
                @Override
                public String toString() {
                    return ITEM_CATCHING_ID;
                }
            });

    public static final RecipeSerializer<ItemCatchingRecipe> ITEM_CATCHING_SERIALIZER = Registry.register(
            BuiltInRegistries.RECIPE_SERIALIZER, Raft.id(ITEM_CATCHING_ID),
            new RecipeSerializer<ItemCatchingRecipe>() {
                @Override
                public com.mojang.serialization.MapCodec<ItemCatchingRecipe> codec() {
                    return ItemCatchingRecipe.CODEC;
                }

                @Override
                public net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, ItemCatchingRecipe> streamCodec() {
                    return ItemCatchingRecipe.STREAM_CODEC;
                }
            });

    public static void registerRecipes() {
        // static initializers do the registration; raft:crafting_transmute stands in for the newer game's recipe type
        net.kasax.raft.compat.recipe.TransmuteRecipe.register();
    }
}
