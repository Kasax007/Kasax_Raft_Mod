package net.kasax.raft.screen;

import net.kasax.raft.Raft;
import net.kasax.raft.compat.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.kasax.raft.compat.gui.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ItemCollectorScreen extends net.kasax.raft.compat.gui.ContainerScreen<ItemCollectorScreenHandler> {
    private static final ResourceLocation TEXTURE = Raft.id("textures/gui/item_collector_gui.png");

    public ItemCollectorScreen(ItemCollectorScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        titleLabelY = 1000;
        inventoryLabelY = 1000;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, imageWidth, imageHeight, 256, 256);

        renderProgressArrow(graphics, x, y);
    }

    private void renderProgressArrow(GuiGraphicsExtractor graphics, int x, int y) {
        if (menu.isCrafting()) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + 85, y + 30, 176, 0, 8, menu.getScaledProgress(), 256, 256);
        }
    }
}
