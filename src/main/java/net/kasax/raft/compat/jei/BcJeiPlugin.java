package net.kasax.raft.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.kasax.raft.Raft;
import net.kasax.raft.block.ModBlocks;
import net.kasax.raft.content.food.FoodContent;
import net.kasax.raft.content.food.block.PotRecipes;
import net.kasax.raft.content.gear.GearContent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import java.util.List;

/**
 * Optional JEI support (loaded through the {@code jei_mod_plugin} entrypoint only when JEI is installed; nothing
 * else in the mod refers to JEI). Recipe pages for the Beach Craft mechanics that have no crafting recipe:
 * the cooking pot, the fish trap and growing the island crops; and info pages for bait, the golden lure, the
 * Driftwood Fishing Rod, the cooking pot, the fish trap and the crop plot.
 */
@JeiPlugin
public class BcJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return Raft.id("jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new CookingPotCategory(gui), new FishTrapCategory(gui), new CropCategory(gui));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(CookingPotCategory.TYPE, PotRecipes.RECIPES);
        registration.addRecipes(FishTrapCategory.TYPE, List.of(FishTrapCategory.Page.TRAP));
        registration.addRecipes(CropCategory.TYPE, CropCategory.recipes());

        info(registration, FoodContent.COOKING_POT, "cooking_pot");
        info(registration, FoodContent.FISH_TRAP, "fish_trap");
        info(registration, ModBlocks.CROP_PLOT, "crop_plot");
        info(registration, GearContent.FISH_BAIT, "fish_bait");
        info(registration, GearContent.GOLDEN_LURE, "golden_lure");
        info(registration, GearContent.DRIFTWOOD_FISHING_ROD, "driftwood_fishing_rod");
        info(registration, Items.FISHING_ROD, "fishing_rod");
    }

    private static void info(IRecipeRegistration registration, ItemLike item, String key) {
        registration.addItemStackInfo(new ItemStack(item), Component.translatable("jei.raft.info." + key));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(CookingPotCategory.TYPE, FoodContent.COOKING_POT);
        registration.addRecipeCatalysts(FishTrapCategory.TYPE, FoodContent.FISH_TRAP);
        registration.addRecipeCatalysts(CropCategory.TYPE, ModBlocks.CROP_PLOT, Items.FARMLAND);
    }

    static ItemStack stack(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }
}
