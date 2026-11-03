package net.kasax.raft.content.story.client;

import net.kasax.raft.Raft;
import net.kasax.raft.content.story.StoryContent;
import net.kasax.raft.content.story.StoryProgress;
import net.kasax.raft.content.story.StoryUtil;
import net.kasax.raft.content.story.net.OpenJournalPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The Expedition Journal: a leather-bound two-page spread. Left page: the current chapter, its goal and a hint, the
 * four Tide Sigils and the chart timer. Right page: the voyage so far (chapters done are ticked, the current one is
 * marked, later ones stay unknown).
 */
public class JournalScreen extends Screen {
    private static final Identifier TEXTURE = Raft.id("textures/gui/journal.png");
    private static final int W = 320, H = 200;
    private static final int INK = 0xFF3B2A1C, FADED = 0xFF8A7656, TEAL = 0xFF1F6E6E, GOLD = 0xFF9A6A12, GREEN = 0xFF2F6E3A;

    private final OpenJournalPayload data;
    private final int stage;
    private final long openedAt;

    public JournalScreen(OpenJournalPayload data) {
        super(Component.translatable("journal.raft.heading"));
        this.data = data;
        this.stage = StoryProgress.stage(data.doneMask());
        this.openedAt = System.currentTimeMillis();
    }

    private int left() {
        return (this.width - W) / 2;
    }

    private int top() {
        return Math.max(2, (this.height - H) / 2);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, left(), top(), 0.0F, 0.0F, W, H, W, H);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        int x0 = left(), y0 = top();
        drawLeftPage(g, x0 + 20, y0 + 16, 130);
        drawRightPage(g, x0 + 176, y0 + 16, 126);
    }

    private int wrapped(GuiGraphicsExtractor g, Component text, int x, int y, int width, int color) {
        List<FormattedCharSequence> lines = this.font.split(text, width);
        for (FormattedCharSequence line : lines) {
            g.text(this.font, line, x, y, color, false);
            y += 10;
        }
        return y;
    }

    private void drawLeftPage(GuiGraphicsExtractor g, int x, int y, int w) {
        String key = stage >= StoryProgress.DONE ? "done" : StoryProgress.CHAPTERS[stage];
        Component heading = stage < StoryProgress.DONE ? Component.translatable("journal.raft.chapter", StoryProgress.CHAPTER_NUMBER[stage])
                : Component.translatable("journal.raft.heading");
        g.centeredText(this.font, heading, x + w / 2, y, TEAL);
        g.fill(x + 10, y + 11, x + w - 10, y + 12, 0x803B2A1C);
        // chapter title, up to 1.5x (smaller when it would not fit the page)
        Component title = Component.translatable("journal.raft." + key + ".title");
        float scale = Math.min(1.5F, w / (float) Math.max(1, this.font.width(title)));
        g.pose().pushMatrix();
        g.pose().translate(x, y + 18 + (1.5F - scale) * 4);
        g.pose().scale(scale, scale);
        g.text(this.font, title, 0, 0, INK, false);
        g.pose().popMatrix();
        int cy = y + 34;
        g.text(this.font, Component.translatable("journal.raft.goal").withStyle(s -> s.withBold(true)), x, cy, GOLD, false);
        cy = wrapped(g, Component.translatable("journal.raft." + key + ".goal"), x, cy + 11, w, INK) + 3;
        g.text(this.font, Component.translatable("journal.raft.hint").withStyle(s -> s.withBold(true)), x, cy, GOLD, false);
        wrapped(g, Component.translatable("journal.raft." + key + ".hint").withStyle(s -> s.withItalic(true)), x, cy + 11, w, FADED);

        // the four Tide Sigils
        int sy = y + 116;
        g.text(this.font, Component.translatable("journal.raft.sigils"), x, sy, FADED, false);
        String[] slots = {"coral", "pearl", "storm", "ember"};
        for (int i = 0; i < 4; i++) {
            int sx = x + 2 + i * 22;
            g.fill(sx - 1, sy + 11, sx + 17, sy + 29, 0x403B2A1C);
            g.outline(sx - 1, sy + 11, 18, 18, 0x803B2A1C);
            if ((data.sigils() & (1 << i)) != 0) {
                g.item(new ItemStack(StoryContent.sigil(slots[i])), sx, sy + 12);
            } else {
                g.text(this.font, "?", sx + 6, sy + 16, FADED, false);
            }
        }
        // chart status
        Component chart;
        if (stage >= StoryProgress.DONE) {
            chart = Component.translatable("journal.raft.map_done");
        } else if (!data.onSea()) {
            chart = Component.translatable("journal.raft.map_not_sea");
        } else {
            long waited = (System.currentTimeMillis() - openedAt) / 50;
            long left = data.mapWait() - waited;
            chart = left > 0 ? Component.translatable("journal.raft.map_wait", StoryUtil.time(left))
                    : Component.translatable("journal.raft.map_ready");
        }
        wrapped(g, chart.copy().withStyle(s -> s.withItalic(true)), x, y + 152, w, TEAL);
    }

    private void drawRightPage(GuiGraphicsExtractor g, int x, int y, int w) {
        g.centeredText(this.font, Component.translatable("journal.raft.progress"), x + w / 2, y, TEAL);
        g.fill(x + 10, y + 11, x + w - 10, y + 12, 0x803B2A1C);
        int ly = y + 20;
        for (int i = 0; i <= StoryProgress.DONE; i++) {
            boolean done = i < StoryProgress.DONE && (data.doneMask() & (1 << i)) != 0;
            boolean current = i == stage;
            String key = i == StoryProgress.DONE ? "done" : StoryProgress.CHAPTERS[i];
            int bx = x, by = ly + 1;
            g.outline(bx, by, 7, 7, done ? GREEN : FADED);
            if (done) {                                   // a little tick mark
                g.fill(bx + 1, by + 3, bx + 2, by + 5, GREEN);
                g.fill(bx + 2, by + 4, bx + 3, by + 6, GREEN);
                g.fill(bx + 3, by + 3, bx + 4, by + 5, GREEN);
                g.fill(bx + 4, by + 2, bx + 5, by + 4, GREEN);
                g.fill(bx + 5, by + 1, bx + 6, by + 3, GREEN);
            }
            Component name;
            int color;
            if (done || current) {
                name = Component.translatable("journal.raft." + key + ".title");
                color = done ? INK : GOLD;
                if (current) name = name.copy().withStyle(s -> s.withBold(true));
            } else {
                name = Component.translatable("journal.raft.unknown");
                color = FADED;
            }
            if (current) {
                g.text(this.font, ">", bx - 7, ly, GOLD, false);
            }
            g.text(this.font, name, x + 11, ly, color, false);
            ly += 10;
        }
    }
}
