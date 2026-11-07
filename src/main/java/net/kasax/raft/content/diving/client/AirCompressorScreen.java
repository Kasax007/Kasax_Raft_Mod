package net.kasax.raft.content.diving.client;

import net.kasax.raft.Raft;
import net.kasax.raft.content.diving.block.AirCompressorBlockEntity;
import net.kasax.raft.content.diving.block.AirCompressorMenu;
import net.kasax.raft.content.diving.item.OxygenTankItem;
import net.kasax.raft.compat.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.kasax.raft.compat.gui.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Air compressor GUI: the tank slot, a fill bar with the air time (or the scooter's FE), the energy column, a pressure
 * gauge whose needle follows the fill level and the hand crank button.
 */
public class AirCompressorScreen extends net.kasax.raft.compat.gui.ContainerScreen<AirCompressorMenu> {
    private static final ResourceLocation TEXTURE = Raft.id("textures/gui/container/air_compressor.png");
    private static final int ENERGY_X = 152, ENERGY_Y = 17, ENERGY_W = 12, ENERGY_H = 52;
    private static final int BAR_X = 66, BAR_Y = 38, BAR_W = 54, BAR_H = 10;
    private static final int GAUGE_CX = 93, GAUGE_CY = 24;
    private static final int CRANK_X = 126, CRANK_Y = 31, CRANK_SIZE = 20;

    public AirCompressorScreen(AirCompressorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        CrankButton crank = new CrankButton(leftPos + CRANK_X, topPos + CRANK_Y);
        crank.setTooltip(Tooltip.create(Component.translatable("container.raft.air_compressor.crank")));
        this.addRenderableWidget(crank);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);

        int energy = Math.round((float) ENERGY_H * menu.energy() / menu.energyCapacity());
        if (energy > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos + ENERGY_X, topPos + ENERGY_Y + ENERGY_H - energy,
                    176, ENERGY_H - energy, ENERGY_W, energy, 256, 256);
        }
        int max = menu.fillCapacity();
        if (max > 0) {
            int w = Math.round((float) BAR_W * menu.fill() / max);
            int v = menu.kind() == AirCompressorBlockEntity.KIND_SCOOTER ? 62 : 52;
            if (w > 0) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos + BAR_X, topPos + BAR_Y, 176, v, w, BAR_H, 256, 256);
            }
        }
        if (menu.isWorking()) {
            // green lamp above the crank
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos + 133, topPos + 22, 176, 72, 6, 6, 256, 256);
        }
        drawNeedle(graphics, max > 0 ? (float) menu.fill() / max : 0.0F);
    }

    /** A 1-px needle from the gauge centre, -135 deg (empty) to +135 deg (full), with a little jitter while pumping. */
    private void drawNeedle(GuiGraphicsExtractor graphics, float fraction) {
        float jitter = menu.isWorking() ? (float) Math.sin(System.currentTimeMillis() / 60.0) * 0.02F : 0.0F;
        double angle = Math.toRadians(-135.0 + 270.0 * Math.min(1.0F, Math.max(0.0F, fraction + jitter)));
        int cx = leftPos + GAUGE_CX, cy = topPos + GAUGE_CY;
        for (int r = 0; r <= 6; r++) {
            int x = cx + (int) Math.round(Math.sin(angle) * r);
            int y = cy - (int) Math.round(Math.cos(angle) * r);
            graphics.fill(x, y, x + 1, y + 1, r == 0 ? 0xFF3A2A1A : 0xFFB8231C);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        Component text;
        if (menu.kind() == AirCompressorBlockEntity.KIND_TANK) {
            text = Component.translatable("container.raft.air_compressor.air", OxygenTankItem.formatTime(menu.fill()),
                    OxygenTankItem.formatTime(menu.fillCapacity()));
        } else if (menu.kind() == AirCompressorBlockEntity.KIND_SCOOTER) {
            text = Component.translatable("container.raft.air_compressor.fe", menu.fill(), menu.fillCapacity());
        } else {
            text = Component.translatable("container.raft.air_compressor.empty");
        }
        graphics.text(this.font, text, BAR_X + (BAR_W - this.font.width(text)) / 2, BAR_Y + BAR_H + 4, 0xFF404040, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        int x = mouseX - leftPos, y = mouseY - topPos;
        if (x >= ENERGY_X && x < ENERGY_X + ENERGY_W && y >= ENERGY_Y && y < ENERGY_Y + ENERGY_H) {
            graphics.setTooltipForNextFrame(this.font,
                    Component.translatable("container.raft.air_compressor.energy", menu.energy(), menu.energyCapacity()), mouseX, mouseY);
        }
    }

    /** The hand crank: four frames of a turning brass wheel; pressing it sends the crank button to the server. */
    private class CrankButton extends AbstractButton {
        CrankButton(int x, int y) {
            super(x, y, CRANK_SIZE, CRANK_SIZE, Component.translatable("container.raft.air_compressor.crank"));
        }

        @Override
        public void onPress() {
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, AirCompressorMenu.BUTTON_CRANK);
            }
        }

        @Override
        protected void renderWidget(net.minecraft.client.gui.GuiGraphics vanilla, int mouseX, int mouseY, float a) {
            GuiGraphicsExtractor graphics = new GuiGraphicsExtractor(vanilla);
            int anim = menu.crankAnimation();
            int frame = anim > 0 ? (AirCompressorBlockEntity.CRANK_COOLDOWN - anim) % 4 : 0;
            int v = 80 + (isHoveredOrFocused() ? CRANK_SIZE : 0);
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, getX(), getY(), 176 + frame * CRANK_SIZE, v, CRANK_SIZE, CRANK_SIZE,
                    256, 256);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }
    }
}
