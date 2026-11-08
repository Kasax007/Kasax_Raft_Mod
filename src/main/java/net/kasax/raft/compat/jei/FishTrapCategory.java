package net.kasax.raft.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.kasax.raft.Raft;
import net.kasax.raft.content.food.FoodContent;
import net.kasax.raft.content.food.block.FishTrapCatches;
import net.kasax.raft.content.gear.GearContent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;

/**
 * JEI page of the fish trap: the trap in water, the optional bait, and everything it can catch with its chance.
 */
public class FishTrapCategory extends AbstractRecipeCategory<FishTrapCategory.Page> {
    /** The trap has one page. */
    public enum Page {
        TRAP
    }

    public static final IRecipeType<Page> TYPE = IRecipeType.create(Raft.id("fish_trap"), Page.class);
    private static final int W = 150, H = 72;

    public FishTrapCategory(IGuiHelper gui) {
        super(TYPE, Component.translatable("jei.raft.fish_trap"), gui.createDrawableItemLike(FoodContent.FISH_TRAP), W, H);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Page page, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, 1, 1).setStandardSlotBackground()
                .add(new ItemStack(FoodContent.FISH_TRAP));
        builder.addInputSlot(1, 21).setStandardSlotBackground()
                .addItemStacks(List.of(new ItemStack(GearContent.FISH_BAIT), new ItemStack(GearContent.GOLDEN_LURE)))
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(
                        Component.translatable("jei.raft.fish_trap.bait").withStyle(ChatFormatting.AQUA)));
        List<FishTrapCatches.Catch> catches = FishTrapCatches.CATCHES;
        for (int i = 0; i < catches.size(); i++) {
            FishTrapCatches.Catch c = catches.get(i);
            ItemStack stack = BcJeiPlugin.stack(c.id());
            stack.setCount(c.max());
            String chance = String.format(Locale.ROOT, "%.1f", c.weight() * 100.0 / FishTrapCatches.TOTAL_WEIGHT);
            builder.addOutputSlot(58 + (i % 5) * 18, 1 + (i / 5) * 18).setStandardSlotBackground().add(stack)
                    .addRichTooltipCallback((view, tooltip) -> {
                        tooltip.add(Component.translatable("jei.raft.fish_trap.chance", chance).withStyle(ChatFormatting.GOLD));
                        if (c.max() > c.min()) {
                            tooltip.add(Component.translatable("jei.raft.fish_trap.count", c.min(), c.max())
                                    .withStyle(ChatFormatting.GRAY));
                        }
                    });
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, Page page, IFocusGroup focuses) {
        builder.addRecipeArrowWidget().setPosition(25, 10);
        builder.addText(Component.translatable("jei.raft.fish_trap.time"), W - 2, 30).setPosition(1, 42)
                .setColor(0xFF404040);
    }
}
