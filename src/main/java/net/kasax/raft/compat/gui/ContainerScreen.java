package net.kasax.raft.compat.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * 1.21.1 port helper: a container screen written with the newer game's method names: the container texture is
 * drawn in {@code extractBackground}, labels in {@code extractLabels}, tooltips in {@code extractTooltip}.
 */
public abstract class ContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    private GuiGraphicsExtractor extractor;

    protected ContainerScreen(T menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    protected ContainerScreen(T menu, Inventory inventory, Component title, int width, int height) {
        super(menu, inventory, title);
        this.imageWidth = width;
        this.imageHeight = height;
        this.inventoryLabelY = height - 94;
    }

    protected final GuiGraphicsExtractor wrap(GuiGraphics g) {
        if (this.extractor == null || this.extractor.vanilla() != g) this.extractor = new GuiGraphicsExtractor(g);
        return this.extractor;
    }

    @Override
    public final void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        GuiGraphicsExtractor e = this.wrap(g);
        this.extractRenderState(e, mouseX, mouseY, partialTick);
        e.flushTooltip();
    }

    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.render(g.vanilla(), mouseX, mouseY, partialTick);
        this.renderTooltip(g.vanilla(), mouseX, mouseY);
    }

    @Override
    public final void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.extractBackground(this.wrap(g), mouseX, mouseY, partialTick);
    }

    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g.vanilla(), mouseX, mouseY, partialTick);
    }

    @Override
    protected final void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
    }

    @Override
    protected final void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        this.extractLabels(this.wrap(g), mouseX, mouseY);
    }

    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.renderLabels(g.vanilla(), mouseX, mouseY);
    }

    @Override
    protected final void renderTooltip(GuiGraphics g, int mouseX, int mouseY) {
        this.extractTooltip(this.wrap(g), mouseX, mouseY);
    }

    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.renderTooltip(g.vanilla(), mouseX, mouseY);
    }

    @Override
    public final boolean keyPressed(int key, int scancode, int modifiers) {
        return this.keyPressed(new KeyEvent(key, scancode, modifiers));
    }

    public boolean keyPressed(KeyEvent event) {
        return super.keyPressed(event.key(), event.scancode(), event.modifiers());
    }

    @Override
    public final boolean mouseClicked(double x, double y, int button) {
        return this.mouseClicked(new MouseButtonEvent(x, y, button), false);
    }

    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return super.mouseClicked(event.x(), event.y(), event.button());
    }
}
