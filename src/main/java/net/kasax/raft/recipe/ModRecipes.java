package net.kasax.raft.recipe;

import net.kasax.raft.Raft;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModRecipes {
    public static void registerRecipes() {
        Registry.register(Registries.RECIPE_SERIALIZER, new Identifier(Raft.MOD_ID, ItemCatchingRecipe.Serializer.ID),
                ItemCatchingRecipe.Serializer.INSTANCE);

        Registry.register(Registries.RECIPE_TYPE, new Identifier(Raft.MOD_ID, ItemCatchingRecipe.Type.ID),
                ItemCatchingRecipe.Type.INSTANCE);
    }

}
