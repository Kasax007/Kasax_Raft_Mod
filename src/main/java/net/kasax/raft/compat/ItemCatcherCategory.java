package net.kasax.raft.compat;

import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.kasax.raft.Raft;
import net.kasax.raft.block.ModBlocks;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.LinkedList;
import java.util.List;

public class ItemCatcherCategory implements DisplayCategory<BasicDisplay> {

    public static final Identifier TEXTURE =
            new Identifier(Raft.MOD_ID, "textures/gui/item_collector_gui.png");

    public static final CategoryIdentifier<ItemCatcherDisplay> ITEM_CATCHING =
            CategoryIdentifier.of(Raft.MOD_ID, "item_catching");

    @Override
    public CategoryIdentifier<? extends BasicDisplay> getCategoryIdentifier() {
        return ITEM_CATCHING;
    }

    @Override
    public Text getTitle() {
        return Text.literal("Item Catcher");
    }

    @Override
    public Renderer getIcon() {
        return EntryStacks.of(ModBlocks.ITEM_CATCHER.asItem().getDefaultStack());
    }

    @Override
    public List<Widget> setupDisplay(BasicDisplay display, Rectangle bounds) {
        final Point startPoint = new Point(bounds.getCenterX() - 87, bounds.getCenterY() - 35);
        List<Widget> widgets = new LinkedList<>();
        widgets.add(Widgets.createTexturedWidget(TEXTURE, new Rectangle(startPoint.x, startPoint.y, 175, 82)));

        widgets.add(Widgets.createSlot(new Point(startPoint.x + 80, startPoint.y + 11))
                .entries(display.getInputEntries().get(0)));

        widgets.add(Widgets.createSlot(new Point(startPoint.x + 80, startPoint.y + 59))
                .markOutput().entries(display.getOutputEntries().get(0)));

        return widgets;
    }

    @Override
    public int getDisplayHeight() {
        return 90;
    }
}
