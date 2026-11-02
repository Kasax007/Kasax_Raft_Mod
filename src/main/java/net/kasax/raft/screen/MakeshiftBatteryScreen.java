package net.kasax.raft.screen;

import net.kasax.raft.Raft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class MakeshiftBatteryScreen extends AbstractContainerScreen<MakeshiftBatteryScreenHandler> {
    private static final Identifier TEXTURE = Raft.id("textures/gui/makeshift_battery_screen.png");

    public MakeshiftBatteryScreen(MakeshiftBatteryScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 256, 256);

        int energyBarSize = Mth.ceil(this.menu.getEnergyPercent() * 66);
        graphics.fill(this.leftPos + 144, this.topPos + 10 + 66 - energyBarSize, this.leftPos + 144 + 20, this.topPos + 10 + 66, 0xFFD4AF37);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (isHovering(144, 10, 20, 66, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(this.font,
                    Component.literal(this.menu.getEnergy() + " / " + this.menu.getMaxEnergy() + " FE"), mouseX, mouseY);
        }
    }
}
