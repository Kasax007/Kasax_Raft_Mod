package net.kasax.raft.content.vessel.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.kasax.raft.content.vessel.RaftMotorMenu;
import net.kasax.raft.compat.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.kasax.raft.compat.gui.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

/**
 * The raft motor's fuel bay: drawn on the game's own furnace window with only the fuel slot and the flame (the input
 * slot, arrow and result slot are painted over), plus the running time of the fuel in the motor.
 */
@Environment(EnvType.CLIENT)
public class RaftMotorScreen extends net.kasax.raft.compat.gui.ContainerScreen<RaftMotorMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/container/furnace.png");
    private static final ResourceLocation LIT = ResourceLocation.withDefaultNamespace("container/furnace/lit_progress");
    private static final int BACKGROUND = 0xFFC6C6C6;

    public RaftMotorScreen(RaftMotorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);
        int x = this.leftPos;
        int y = this.topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        graphics.fill(x + 54, y + 15, x + 74, y + 35, BACKGROUND);      // input slot
        graphics.fill(x + 78, y + 33, x + 104, y + 51, BACKGROUND);     // arrow
        graphics.fill(x + 110, y + 29, x + 138, y + 57, BACKGROUND);    // result slot
        if (this.menu.isBurning()) {
            int h = this.menu.getBurnProgress() + 1;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, LIT, 14, 14, 0, 14 - h, x + 56, y + 36 + 14 - h, 14, h);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        int s = this.menu.getTankSeconds();
        Component time = s > 0
                ? Component.translatable("container.raft.raft_motor.time", String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60))
                : Component.translatable("container.raft.raft_motor.empty");
        graphics.text(this.font, Component.translatable("container.raft.raft_motor.fuel"), 80, 36, 0xFF404040, false);
        graphics.text(this.font, time, 80, 48, s > 0 ? 0xFF404040 : 0xFF9A2A2A, false);
    }
}
