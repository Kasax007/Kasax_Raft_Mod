package net.kasax.raft.content.story.client;

import net.kasax.raft.Raft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * A Meridian Expedition terminal: a cream steel CRT monitor with green phosphor text. The log is typed out a few
 * characters per tick (click or press a key to show it all), paged with the arrow buttons or the arrow keys.
 */
public class TerminalScreen extends Screen {
    private static final Identifier TEXTURE = Raft.id("textures/gui/terminal.png");
    private static final int W = 300, H = 200;
    private static final int TEXT_X = 28, TEXT_Y = 52, TEXT_W = 244, LINES = 9;
    private static final int HEADER = 0xFF3FAF80, TITLE = 0xFFD0FFE4, TEXT = 0xFF7FF0B8, DIM = 0xFF2F7A5A;
    private static final int CHARS_PER_TICK = 3;

    private final String logId;
    private final List<List<String>> pages = new ArrayList<>();
    private String title = "";
    private int page;
    private int typed;
    private int ticks;
    private PageButton forward;
    private PageButton back;

    public TerminalScreen(String logId) {
        super(Component.translatable("terminal.raft.header"));
        this.logId = logId;
    }

    private int left() {
        return (this.width - W) / 2;
    }

    private int top() {
        return Math.max(2, (this.height - H) / 2);
    }

    @Override
    protected void init() {
        String id = net.minecraft.locale.Language.getInstance().has("terminal.raft." + logId + ".title") ? logId : "unknown";
        title = I18n.get("terminal.raft." + id + ".title");
        List<String> lines = new ArrayList<>();
        for (String paragraph : I18n.get("terminal.raft." + id + ".text").split("\n")) {
            for (FormattedText line : this.font.getSplitter().splitLines(paragraph, TEXT_W, Style.EMPTY)) {
                lines.add(line.getString());
            }
            lines.add("");
        }
        while (!lines.isEmpty() && lines.getLast().isEmpty()) lines.removeLast();
        pages.clear();
        for (int i = 0; i < lines.size(); i += LINES) {
            List<String> p = new ArrayList<>(lines.subList(i, Math.min(lines.size(), i + LINES)));
            while (!p.isEmpty() && p.getFirst().isEmpty()) p.removeFirst();
            pages.add(p);
        }
        if (pages.isEmpty()) pages.add(List.of());
        page = Math.min(page, pages.size() - 1);
        int x = left(), y = top();
        forward = addRenderableWidget(new PageButton(x + W - 70, y + 170, true, b -> turn(1), true));
        back = addRenderableWidget(new PageButton(x + 47, y + 170, false, b -> turn(-1), true));
        updateButtons();
    }

    private void turn(int d) {
        int next = Math.max(0, Math.min(pages.size() - 1, page + d));
        if (next != page) {
            page = next;
            typed = 0;
        }
        updateButtons();
    }

    private void updateButtons() {
        if (forward != null) forward.visible = page < pages.size() - 1;
        if (back != null) back.visible = page > 0;
    }

    private int pageChars() {
        int n = 0;
        for (String s : pages.get(page)) n += s.length() + 1;
        return n;
    }

    @Override
    public void tick() {
        ticks++;
        typed += CHARS_PER_TICK;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (super.keyPressed(event)) return true;
        if (event.isRight()) {
            if (typed < pageChars()) typed = pageChars(); else turn(1);
            return true;
        }
        if (event.isLeft()) {
            turn(-1);
            return true;
        }
        if (event.isSelection() || event.isConfirmation()) {
            if (typed < pageChars()) typed = pageChars(); else turn(1);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        if (typed < pageChars()) {
            typed = pageChars();
            return true;
        }
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
        int x = left(), y = top();
        // header bar
        g.text(this.font, Component.translatable("terminal.raft.header"), x + TEXT_X, y + 24, HEADER, false);
        String pg = I18n.get("terminal.raft.page", page + 1, pages.size());
        g.text(this.font, pg, x + TEXT_X + TEXT_W - this.font.width(pg), y + 24, DIM, false);
        g.fill(x + TEXT_X, y + 34, x + TEXT_X + TEXT_W, y + 35, DIM);
        g.text(this.font, "> " + title, x + TEXT_X, y + 38, TITLE, false);
        // typed log text
        int left = typed;
        int ly = y + TEXT_Y;
        String cursorLine = null;
        int cursorX = 0;
        for (String line : pages.get(page)) {
            if (left <= 0) break;
            String shown = line.length() <= left ? line : line.substring(0, left);
            g.text(this.font, shown, x + TEXT_X, ly, TEXT, false);
            left -= line.length() + 1;
            cursorLine = shown;
            cursorX = x + TEXT_X + this.font.width(shown);
            ly += 10;
        }
        if ((ticks / 8) % 2 == 0) {
            int cy = cursorLine == null ? y + TEXT_Y : ly - 10;
            g.fill(cursorX + 1, cy, cursorX + 6, cy + 8, TEXT);
        }
        // CRT scanlines and a soft flicker
        int flicker = (ticks % 37 == 0) ? 0x18 : 0x0C;
        for (int sy = y + 20; sy < y + 158; sy += 2) {
            g.fill(x + 20, sy, x + W - 20, sy + 1, (flicker << 24));
        }
        g.centeredText(this.font, Component.translatable("terminal.raft.keys"), x + W / 2, y + 150, DIM);
    }
}
