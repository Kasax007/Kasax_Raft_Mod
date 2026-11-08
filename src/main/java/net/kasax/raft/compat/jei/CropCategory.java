package net.kasax.raft.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.RecipeType;
import net.kasax.raft.Raft;
import net.kasax.raft.block.ModBlocks;
import net.kasax.raft.content.food.FoodContent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI page "Island Crops": what to plant where and what it gives - the four island crops on farmland or a crop
 * plot, sea grapes under water, and the wild plants of the islands (break them).
 */
public class CropCategory extends AbstractRecipeCategory<CropCategory.Crop> {
    /** A plant (seed or wild plant), where it goes (empty for wild plants) and what it gives; note is a lang key. */
    public record Crop(ItemStack plant, List<ItemStack> soil, List<ItemStack> harvest, String note) {
    }

    public static final RecipeType<Crop> TYPE = RecipeType.create(Raft.MOD_ID, "island_crops", Crop.class);
    private static final int W = 150, H = 50;

    public CropCategory(IGuiHelper gui) {
        super(TYPE, Component.translatable("jei.raft.island_crops"), gui.createDrawableItemLike(FoodContent.CHILI_PEPPER), W, H);
    }

    private static ItemStack s(net.minecraft.world.level.ItemLike item) {
        return new ItemStack(item);
    }

    public static List<Crop> recipes() {
        List<ItemStack> farm = List.of(s(ModBlocks.CROP_PLOT), s(Items.FARMLAND));
        List<ItemStack> sea = List.of(s(Items.SAND), s(Items.GRAVEL), BcJeiPlugin.stack("raft:shell_sand"));
        List<Crop> out = new ArrayList<>();
        out.add(new Crop(s(FoodContent.SWEET_POTATO), farm, List.of(s(FoodContent.SWEET_POTATO)), "jei.raft.crops.farm"));
        out.add(new Crop(s(FoodContent.CHILI_SEEDS), farm, List.of(s(FoodContent.CHILI_PEPPER), s(FoodContent.CHILI_SEEDS)), "jei.raft.crops.farm"));
        out.add(new Crop(s(FoodContent.BANANA_SHOOT), farm, List.of(s(FoodContent.BANANA), s(FoodContent.BANANA_SHOOT)), "jei.raft.crops.farm"));
        out.add(new Crop(s(FoodContent.RICE), farm, List.of(s(FoodContent.RICE)), "jei.raft.crops.farm"));
        out.add(new Crop(s(FoodContent.SEA_GRAPES), sea, List.of(s(FoodContent.SEA_GRAPES)), "jei.raft.crops.sea_grapes"));
        out.add(new Crop(s(FoodContent.WILD_SWEET_POTATO), List.of(), List.of(s(FoodContent.SWEET_POTATO)), "jei.raft.crops.wild"));
        out.add(new Crop(s(FoodContent.WILD_CHILI), List.of(), List.of(s(FoodContent.CHILI_PEPPER), s(FoodContent.CHILI_SEEDS)), "jei.raft.crops.wild"));
        out.add(new Crop(s(FoodContent.WILD_BANANA), List.of(), List.of(s(FoodContent.BANANA), s(FoodContent.BANANA_SHOOT)), "jei.raft.crops.wild"));
        out.add(new Crop(s(FoodContent.WILD_RICE), List.of(), List.of(s(FoodContent.RICE)), "jei.raft.crops.wild"));
        return out;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Crop crop, IFocusGroup focuses) {
        builder.addInputSlot(1, 1).setStandardSlotBackground().addItemStack(crop.plant());
        if (!crop.soil().isEmpty()) {
            builder.addSlot(RecipeIngredientRole.CATALYST, 1, 19).setStandardSlotBackground().addItemStacks(crop.soil())
                    .addRichTooltipCallback((view, tooltip) -> tooltip.add(
                            Component.translatable("jei.raft.crops.soil").withStyle(ChatFormatting.AQUA)));
        }
        for (int i = 0; i < crop.harvest().size(); i++) {
            builder.addOutputSlot(60 + i * 18, 10).setStandardSlotBackground().addItemStack(crop.harvest().get(i));
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, Crop crop, IFocusGroup focuses) {
        builder.addRecipeArrowWidget().setPosition(28, 10);
        builder.addText(Component.translatable(crop.note()), W - 22, 12).setPosition(22, 38).setColor(0xFF404040);
    }
}
