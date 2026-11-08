package net.kasax.raft.content.gear.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.kasax.raft.Raft;
import net.kasax.raft.content.gear.menu.TackleMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * The tackle box of the Driftwood Fishing Rod: the bait slot and the lure slot (with faint pictures of what goes in),
 * each with its name and what it does beside it.
 */
@Environment(EnvType.CLIENT)
public class TackleScreen extends AbstractContainerScreen<TackleMenu> {
    private static final Identifier TEXTURE = Raft.id("textures/gui/container/tackle_box.png");

    public TackleScreen(TackleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, TackleMenu.HEIGHT);
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth,
                this.imageHeight, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        int x = TackleMenu.SLOT_X + 22;
        graphics.text(this.font, Component.translatable("item.raft.fish_bait"), x, TackleMenu.BAIT_Y, 0xFF404040, false);
        graphics.text(this.font, Component.translatable("container.raft.tackle_box.bait"), x, TackleMenu.BAIT_Y + 9, 0xFF707070, false);
        graphics.text(this.font, Component.translatable("item.raft.golden_lure"), x, TackleMenu.LURE_Y, 0xFF404040, false);
        graphics.text(this.font, Component.translatable("container.raft.tackle_box.lure"), x, TackleMenu.LURE_Y + 9, 0xFF707070, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        // hint over the empty slots
        for (int i = 0; i < 2; i++) {
            var slot = this.menu.slots.get(i);
            int x = this.leftPos + slot.x, y = this.topPos + slot.y;
            if (!slot.hasItem() && this.menu.getCarried().isEmpty() && mouseX >= x - 1 && mouseX < x + 17
                    && mouseY >= y - 1 && mouseY < y + 17) {
                graphics.setTooltipForNextFrame(Component.translatable(i == 0 ? "container.raft.tackle_box.bait_hint"
                        : "container.raft.tackle_box.lure_hint"), mouseX, mouseY);
            }
        }
    }
}
