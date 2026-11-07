package net.kasax.raft.content.diving.client;

import net.kasax.raft.Raft;
import net.kasax.raft.content.diving.DivingGear;
import net.kasax.raft.content.diving.item.OxygenTankItem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.kasax.raft.compat.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.kasax.raft.compat.gui.RenderPipelines;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * The tank gauge: a row of ten oxygen capsules right above the air bubbles while the player is under water with a tank
 * on. Capsules drain from left to right, turn amber in the last quarter and blink red in the last 15 seconds. Without
 * a helmet the row is dimmed (the tank cannot be used).
 */
public final class OxygenHud {
    private OxygenHud() {
    }

    public static final ResourceLocation ID = Raft.id("oxygen_tank");
    private static final ResourceLocation FULL = Raft.id("hud/diving/oxygen_full");
    private static final ResourceLocation HALF = Raft.id("hud/diving/oxygen_half");
    private static final ResourceLocation EMPTY = Raft.id("hud/diving/oxygen_empty");
    private static final ResourceLocation LOW = Raft.id("hud/diving/oxygen_low");
    private static final ResourceLocation LOW_HALF = Raft.id("hud/diving/oxygen_low_half");
    private static final ResourceLocation ICON = Raft.id("hud/diving/oxygen_icon");

    public static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.gameMode == null || !mc.gameMode.canHurtPlayer()) {
            return;
        }
        if (!player.isEyeInFluid(FluidTags.WATER)) {
            return;
        }
        ItemStack tank = DivingGear.tank(player);
        if (tank.isEmpty()) {
            return;
        }
        int capacity = Math.max(1, OxygenTankItem.capacity(tank));
        int air = OxygenTankItem.getAir(tank);
        boolean usable = DivingGear.wearsHelmet(player);
        int ticks = mc.gui.getGuiTicks();
        boolean low = air <= 300;
        boolean amber = air * 4 <= capacity;
        // ten capsules, each worth a tenth of the tank (halves for finer reading)
        int halves = Mth.ceil(air * 20.0F / capacity);
        int xRight = graphics.guiWidth() / 2 + 91;
        // one row above the air bubbles (they move up when the vehicle's hearts need more than one row)
        int y = graphics.guiHeight() - 49 - 10 - (vehicleHeartRows(player) > 1 ? (vehicleHeartRows(player) - 1) * 10 : 0);
        int alpha = usable ? 255 : 110;
        int color = (alpha << 24) | 0xFFFFFF;
        boolean blinkOff = low && air > 0 && (ticks / 6) % 2 == 0;
        for (int i = 0; i < 10; i++) {
            int x = xRight - i * 8 - 9;
            int filled = halves - (9 - i) * 2;
            ResourceLocation sprite;
            if (filled >= 2) {
                sprite = amber ? LOW : FULL;
            } else if (filled == 1) {
                sprite = amber ? LOW_HALF : HALF;
            } else {
                sprite = EMPTY;
            }
            if (blinkOff && sprite != EMPTY) {
                sprite = EMPTY;
            }
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, 9, 9, color);
        }
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ICON, xRight - 10 * 8 - 10, y, 9, 9, color);
    }

    /** Rows of vehicle hearts the vanilla HUD shows (same rule as the vanilla air bar). */
    private static int vehicleHeartRows(LocalPlayer player) {
        if (player.getVehicle() instanceof net.minecraft.world.entity.LivingEntity vehicle && vehicle.showVehicleHealth()) {
            int hearts = Math.min(30, (int) (vehicle.getMaxHealth() + 0.5F) / 2);
            return Mth.ceil(hearts / 10.0F);
        }
        return 0;
    }
}
