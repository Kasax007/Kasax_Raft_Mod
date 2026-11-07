package net.kasax.raft.compat.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 1.21.1 port helper: a screen written with the newer game's method names ({@code extractRenderState},
 * {@code extractBackground}, key and mouse events), driven by 1.21.1's {@code render}, {@code keyPressed} and
 * {@code mouseClicked}.
 */
public abstract class BaseScreen extends Screen {
    private GuiGraphicsExtractor extractor;

    protected BaseScreen(Component title) {
        super(title);
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
    }

    @Override
    public final void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.extractBackground(this.wrap(g), mouseX, mouseY, partialTick);
    }

    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g.vanilla(), mouseX, mouseY, partialTick);
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
