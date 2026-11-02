package net.kasax.raft.screen;

import net.kasax.raft.Raft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class WaterPurifierScreen extends AbstractContainerScreen<WaterPurifierMenu> {
    private static final Identifier TEXTURE = Raft.id("textures/gui/water_purifier_gui.png");

    public WaterPurifierScreen(WaterPurifierMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        if (menu.isLit()) {
            int flame = menu.getBurnProgress();
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos + 57, topPos + 36 + 12 - flame, 176, 12 - flame, 14, flame + 1, 256, 256);
        }
        int arrow = menu.getCookProgress();
        if (arrow > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos + 79, topPos + 34, 176, 14, arrow, 16, 256, 256);
        }
    }
}
