package net.kasax.raft.compat.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * 1.21.1 port helper: the GUI drawing calls of the newer game ("extract" a GUI frame), drawn with 1.21.1's
 * {@link GuiGraphics}. Tooltips set for the frame are drawn when {@link #flushTooltip()} runs (end of render).
 */
public final class GuiGraphicsExtractor {
    private final GuiGraphics g;
    private final Pose pose;
    private Runnable tooltip;

    public GuiGraphicsExtractor(GuiGraphics g) {
        this.g = g;
        this.pose = new Pose(g.pose());
    }

    public GuiGraphics vanilla() {
        return this.g;
    }

    public Pose pose() {
        return this.pose;
    }

    public int guiWidth() {
        return this.g.guiWidth();
    }

    public int guiHeight() {
        return this.g.guiHeight();
    }

    public void fill(int x0, int y0, int x1, int y1, int color) {
        this.g.fill(x0, y0, x1, y1, color);
    }

    public void fill(RenderPipelines pipeline, int x0, int y0, int x1, int y1, int color) {
        this.g.fill(x0, y0, x1, y1, color);
    }

    public void fillGradient(int x0, int y0, int x1, int y1, int col1, int col2) {
        this.g.fillGradient(x0, y0, x1, y1, col1, col2);
    }

    public void outline(int x, int y, int width, int height, int color) {
        this.g.renderOutline(x, y, width, height, color);
    }

    public void text(Font font, @Nullable String str, int x, int y, int color) {
        if (str != null) this.g.drawString(font, str, x, y, color, true);
    }

    public void text(Font font, @Nullable String str, int x, int y, int color, boolean shadow) {
        if (str != null) this.g.drawString(font, str, x, y, color, shadow);
    }

    public void text(Font font, FormattedCharSequence str, int x, int y, int color) {
        this.g.drawString(font, str, x, y, color, true);
    }

    public void text(Font font, FormattedCharSequence str, int x, int y, int color, boolean shadow) {
        this.g.drawString(font, str, x, y, color, shadow);
    }

    public void text(Font font, Component str, int x, int y, int color) {
        this.g.drawString(font, str, x, y, color, true);
    }

    public void text(Font font, Component str, int x, int y, int color, boolean shadow) {
        this.g.drawString(font, str, x, y, color, shadow);
    }

    public void centeredText(Font font, String str, int x, int y, int color) {
        this.g.drawCenteredString(font, str, x, y, color);
    }

    public void centeredText(Font font, Component str, int x, int y, int color) {
        this.g.drawCenteredString(font, str, x, y, color);
    }

    public void centeredText(Font font, FormattedCharSequence str, int x, int y, int color) {
        this.g.drawCenteredString(font, str, x, y, color);
    }

    public int textWithWordWrap(Font font, FormattedText text, int x, int y, int width, int color) {
        this.g.drawWordWrap(font, text, x, y, width, color);
        return y + font.wordWrapHeight(text, width);
    }

    public void blit(RenderPipelines pipeline, ResourceLocation texture, int x, int y, float u, float v, int width, int height,
                     int textureWidth, int textureHeight) {
        this.g.blit(texture, x, y, u, v, width, height, textureWidth, textureHeight);
    }

    public void blit(RenderPipelines pipeline, ResourceLocation texture, int x, int y, float u, float v, int width, int height,
                     int textureWidth, int textureHeight, int color) {
        setColor(color);
        this.g.blit(texture, x, y, u, v, width, height, textureWidth, textureHeight);
        this.g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public void blit(RenderPipelines pipeline, ResourceLocation texture, int x, int y, float u, float v, int width, int height,
                     int srcWidth, int srcHeight, int textureWidth, int textureHeight) {
        this.g.blit(texture, x, y, width, height, u, v, srcWidth, srcHeight, textureWidth, textureHeight);
    }

    public void blitSprite(RenderPipelines pipeline, ResourceLocation sprite, int textureWidth, int textureHeight, int u, int v,
                           int x, int y, int width, int height) {
        this.g.blitSprite(sprite, textureWidth, textureHeight, u, v, x, y, width, height);
    }

    public void blitSprite(RenderPipelines pipeline, ResourceLocation sprite, int x, int y, int width, int height) {
        this.g.blitSprite(sprite, x, y, width, height);
    }

    public void blitSprite(RenderPipelines pipeline, ResourceLocation sprite, int x, int y, int width, int height, int color) {
        setColor(color);
        this.g.blitSprite(sprite, x, y, width, height);
        this.g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void setColor(int color) {
        this.g.setColor((color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F, (color >>> 24) / 255.0F);
    }

    public void item(ItemStack stack, int x, int y) {
        this.g.renderItem(stack, x, y);
    }

    public void item(ItemStack stack, int x, int y, int seed) {
        this.g.renderItem(stack, x, y, seed);
    }

    public void item(LivingEntity owner, ItemStack stack, int x, int y, int seed) {
        this.g.renderItem(owner, stack, x, y, seed);
    }

    public void itemDecorations(Font font, ItemStack stack, int x, int y) {
        this.g.renderItemDecorations(font, stack, x, y);
    }

    public void setTooltipForNextFrame(Font font, Component text, int x, int y) {
        this.tooltip = () -> this.g.renderTooltip(font, text, x, y);
    }

    public void setTooltipForNextFrame(Font font, List<? extends FormattedCharSequence> lines, int x, int y) {
        this.tooltip = () -> this.g.renderTooltip(font, lines, x, y);
    }

    public void setTooltipForNextFrame(Font font, ItemStack stack, int x, int y) {
        this.tooltip = () -> this.g.renderTooltip(font, stack, x, y);
    }

    public void setTooltipForNextFrame(Font font, List<Component> texts, Optional<TooltipComponent> image, int x, int y) {
        this.tooltip = () -> this.g.renderTooltip(font, texts, image, x, y);
    }

    public void requestCursor(Object cursor) {
    }

    /** Draws the tooltip set during this frame (1.21.1 has no deferred tooltip). */
    public void flushTooltip() {
        if (this.tooltip != null) {
            Runnable t = this.tooltip;
            this.tooltip = null;
            t.run();
        }
    }

    /** The newer game's 2D matrix stack, on top of 1.21.1's pose stack. */
    public static final class Pose {
        private final PoseStack stack;

        Pose(PoseStack stack) {
            this.stack = stack;
        }

        public Pose pushMatrix() {
            this.stack.pushPose();
            return this;
        }

        public Pose popMatrix() {
            this.stack.popPose();
            return this;
        }

        public Pose translate(float x, float y) {
            this.stack.translate(x, y, 0.0F);
            return this;
        }

        public Pose scale(float x, float y) {
            this.stack.scale(x, y, 1.0F);
            return this;
        }

        public Pose scale(float s) {
            this.stack.scale(s, s, 1.0F);
            return this;
        }

        public Pose rotate(float radians) {
            this.stack.mulPose(Axis.ZP.rotation(radians));
            return this;
        }

        public PoseStack vanilla() {
            return this.stack;
        }
    }
}
