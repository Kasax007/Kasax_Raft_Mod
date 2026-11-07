package net.kasax.raft.compat.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.kasax.raft.Raft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * 1.21.1 port of the newer game's {@code minecraft:crafting_transmute} (registered as raft:crafting_transmute):
 * {@code input} plus {@code material} anywhere in the grid give the input item turned into {@code result}, keeping
 * everything the input had (damage, enchantments, name) and adding the result's components (the diving helmet with
 * the angler lure becomes the helmet with a lamp).
 */
public record TransmuteRecipe(String group, CraftingBookCategory category, Ingredient input, Ingredient material, ItemStack result)
        implements CraftingRecipe {
    public static final MapCodec<TransmuteRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.optionalFieldOf("group", "").forGetter(TransmuteRecipe::group),
            CraftingBookCategory.CODEC.optionalFieldOf("category", CraftingBookCategory.MISC).forGetter(TransmuteRecipe::category),
            Ingredient.CODEC_NONEMPTY.fieldOf("input").forGetter(TransmuteRecipe::input),
            Ingredient.CODEC_NONEMPTY.fieldOf("material").forGetter(TransmuteRecipe::material),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(TransmuteRecipe::result)
    ).apply(i, TransmuteRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, TransmuteRecipe> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, TransmuteRecipe::group,
            CraftingBookCategory.STREAM_CODEC, TransmuteRecipe::category,
            Ingredient.CONTENTS_STREAM_CODEC, TransmuteRecipe::input,
            Ingredient.CONTENTS_STREAM_CODEC, TransmuteRecipe::material,
            ItemStack.STREAM_CODEC, TransmuteRecipe::result,
            TransmuteRecipe::new);
    public static final RecipeSerializer<TransmuteRecipe> SERIALIZER = new RecipeSerializer<>() {
        @Override
        public MapCodec<TransmuteRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, TransmuteRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    };

    public static void register() {
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Raft.id("crafting_transmute"), SERIALIZER);
    }

    private ItemStack find(CraftingInput grid) {
        ItemStack in = ItemStack.EMPTY;
        boolean mat = false;
        for (int i = 0; i < grid.size(); i++) {
            ItemStack s = grid.getItem(i);
            if (s.isEmpty()) {
                continue;
            }
            if (in.isEmpty() && this.input.test(s)) {
                in = s;
            } else if (!mat && this.material.test(s)) {
                mat = true;
            } else {
                return ItemStack.EMPTY;
            }
        }
        return mat ? in : ItemStack.EMPTY;
    }

    @Override
    public boolean matches(CraftingInput grid, Level level) {
        return !this.find(grid).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput grid, HolderLookup.Provider registries) {
        ItemStack in = this.find(grid);
        if (in.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack out = in.transmuteCopy(this.result.getItem(), this.result.getCount());
        out.applyComponents(this.result.getComponentsPatch());
        return out;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.result.copy();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }
}
