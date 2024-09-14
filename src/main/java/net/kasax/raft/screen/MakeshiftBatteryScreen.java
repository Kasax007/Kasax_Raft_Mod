package net.kasax.raft.screen;

import net.kasax.raft.Raft;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class MakeshiftBatteryScreen extends HandledScreen<MakeshiftBatteryScreenHandler> {
    private static final Identifier TEXTURE = Raft.id("textures/gui/makeshift_battery_screen.png");
    public MakeshiftBatteryScreen(MakeshiftBatteryScreenHandler handler, PlayerInventory inventory, Text title) {
            super(handler, inventory, title);
        }

        @Override
        protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
            context.drawTexture(TEXTURE, this.x, this.y, 0, 0, this.backgroundWidth, this.backgroundHeight);

            int energyBarSize = MathHelper.ceil(this.handler.getEnergyPercent() * 66);
            context.fill(this.x + 144, this.y + 10 + 66 - energyBarSize, this.x + 144 + 20, this.y + 10 + 66, 0xFFD4AF37);
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            super.render(context, mouseX, mouseY, delta);
            drawMouseoverTooltip(context, mouseX, mouseY);

            if (isPointWithinBounds(144, 10, 20, 66, mouseX, mouseY)) {
                context.drawTooltip(this.textRenderer, Text.literal(this.handler.getEnergy() + " / " + this.handler.getMaxEnergy() + " FE"), mouseX, mouseY);
            }
        }
    }