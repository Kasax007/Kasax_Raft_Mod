package net.kasax.raft.content.npc.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.kasax.raft.Raft;
import net.kasax.raft.content.npc.net.NpcActionPayload;
import net.kasax.raft.content.npc.net.NpcDialogPayload;
import net.kasax.raft.compat.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.kasax.raft.compat.gui.KeyEvent;
import net.kasax.raft.compat.gui.MouseButtonEvent;
import net.kasax.raft.compat.gui.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * A conversation with a person of the sea: a weathered driftwood panel at the bottom of the screen with a live
 * portrait (the NPC follows the mouse with its eyes), the name and title, the words typed out a few letters per tick,
 * and the questions the player can ask (click, or press 1-5). The game keeps running (multiplayer-safe); the screen
 * closes when the NPC is gone or the player walks away.
 */
public class NpcDialogScreen extends net.kasax.raft.compat.gui.BaseScreen {
    private static final ResourceLocation PANEL = Raft.id("npc/dialog");
    private static final int INK = 0xFF3B2A1C, FADED = 0xFF7A6248, NAME = 0xFF7A3E12, HOVER = 0x30A0703C,
            CHOSEN = 0xFF2F6E8E, GIFT = 0xFF2E6B2E;
    private static final int LINES = 5, CHARS_PER_TICK = 2;

    private record Option(Component label, Runnable action) {
    }

    private final NpcDialogPayload dialog;
    private final List<Option> options = new ArrayList<>();
    private final List<List<String>> pages = new ArrayList<>();
    /** How often each topic was asked (topic index -> count), so a repeated question gets the next answer. */
    private final java.util.Map<Integer, Integer> asked = new java.util.HashMap<>();
    private Component text;
    private int chosen = -1;
    private int page;
    private int typed;
    private int ticks;
    private boolean sentBye;

    public NpcDialogScreen(NpcDialogPayload dialog) {
        super(dialog.name());
        this.dialog = dialog;
        this.text = dialog.greeting();
    }

    // ------------------------------------------------------------------------------------------ layout
    private int panelW() {
        return Math.min(this.width - 16, 400);
    }

    private int panelH() {
        return 152;
    }

    private int left() {
        return (this.width - this.panelW()) / 2;
    }

    private int top() {
        return this.height - this.panelH() - 8;
    }

    private int textX() {
        return this.left() + 100;
    }

    private int textW() {
        return this.panelW() - 100 - 12;
    }

    private int textY() {
        return this.top() + 36;
    }

    private int optionY() {
        return this.top() + this.panelH() - 12 - 3 * 12;
    }

    @Override
    protected void init() {
        this.options.clear();
        List<NpcDialogPayload.Topic> topics = this.dialog.topics();
        for (int i = 0; i < topics.size(); i++) {
            NpcDialogPayload.Topic t = topics.get(i);
            int index = i;
            this.options.add(new Option(t.label(), () -> {
                if (t.replies().isEmpty()) {
                    return;
                }
                int n = this.asked.merge(index, 1, Integer::sum) - 1;
                this.show(t.replies().get(n % t.replies().size()), index);
            }));
        }
        if (this.dialog.trade()) {
            this.options.add(new Option(Component.translatable("npc.raft.ask.trade"), () -> {
                ClientPlayNetworking.send(new NpcActionPayload(this.dialog.entityId(), NpcActionPayload.TRADE));
                this.sentBye = true;
            }));
        }
        this.options.add(new Option(Component.translatable("npc.raft.ask.bye"), this::onClose));
        this.layoutText();
    }

    private void show(Component reply, int option) {
        this.text = reply;
        this.chosen = option;
        this.page = 0;
        this.typed = 0;
        this.layoutText();
    }

    private void layoutText() {
        List<String> lines = new ArrayList<>();
        for (String paragraph : this.text.getString().split("\n")) {
            for (FormattedText line : this.font.getSplitter().splitLines(paragraph, this.textW(), Style.EMPTY)) {
                lines.add(line.getString());
            }
        }
        this.pages.clear();
        for (int i = 0; i < lines.size(); i += LINES) {
            this.pages.add(new ArrayList<>(lines.subList(i, Math.min(lines.size(), i + LINES))));
        }
        if (this.pages.isEmpty()) {
            this.pages.add(List.of());
        }
        this.page = Math.min(this.page, this.pages.size() - 1);
    }

    private int pageChars() {
        int n = 0;
        for (String s : this.pages.get(this.page)) {
            n += s.length();
        }
        return n;
    }

    private boolean typing() {
        return this.typed < this.pageChars();
    }

    /** Space, Enter or a click into the text: show the whole page, then turn to the next one. */
    private void advance() {
        if (this.typing()) {
            this.typed = this.pageChars();
        } else if (this.page < this.pages.size() - 1) {
            this.page++;
            this.typed = 0;
        }
    }

    // ------------------------------------------------------------------------------------------ ticking and input
    private LivingEntity npc() {
        if (this.minecraft == null || this.minecraft.level == null) {
            return null;
        }
        Entity e = this.minecraft.level.getEntity(this.dialog.entityId());
        return e instanceof LivingEntity l && l.isAlive() ? l : null;
    }

    @Override
    public void tick() {
        this.ticks++;
        this.typed += CHARS_PER_TICK;
        LivingEntity npc = this.npc();
        if (npc == null || this.minecraft.player == null || this.minecraft.player.distanceToSqr(npc) > 100.0) {
            this.onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key >= InputConstants.KEY_1 && key <= InputConstants.KEY_9) {
            int i = key - InputConstants.KEY_1;
            if (i < this.options.size()) {
                this.choose(i);
                return true;
            }
        }
        if (event.isSelection() || event.isConfirmation()) {
            this.advance();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int hit = this.optionAt(event.x(), event.y());
        if (hit >= 0) {
            this.choose(hit);
            return true;
        }
        if (event.x() >= this.left() && event.x() < this.left() + this.panelW() && event.y() >= this.top()
                && event.y() < this.optionY() - 2) {
            this.advance();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void choose(int i) {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
        this.options.get(i).action().run();
    }

    @Override
    public void removed() {
        if (!this.sentBye) {
            this.sentBye = true;
            ClientPlayNetworking.send(new NpcActionPayload(this.dialog.entityId(), NpcActionPayload.BYE));
        }
        super.removed();
    }

    /** Options right of the portrait, in two columns of three rows. */
    private int[] optionRect(int i) {
        int col = i / 3, row = i % 3;
        int colW = this.textW() / 2;
        return new int[]{this.textX() + col * colW, this.optionY() + row * 12, colW - 4, 12};
    }

    private int optionAt(double mx, double my) {
        for (int i = 0; i < this.options.size(); i++) {
            int[] r = this.optionRect(i);
            if (mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]) {
                return i;
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------------------------------ drawing
    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        // no blur: the world stays visible, only darkened a little behind the panel
        g.fillGradient(0, this.height / 2, this.width, this.height, 0x00000000, 0x90000000);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        int x = this.left(), y = this.top(), w = this.panelW(), h = this.panelH();
        g.blitSprite(RenderPipelines.GUI_TEXTURED, PANEL, x, y, w, h);
        this.drawPortrait(g, x + 12, y + 12, mouseX, mouseY);

        // name and title
        int tx = this.textX();
        g.text(this.font, this.dialog.name().copy().withStyle(s -> s.withBold(true)), tx, y + 12, NAME, false);
        Component title = Component.translatable("npc.raft." + this.dialog.npc() + ".title").withStyle(s -> s.withItalic(true));
        g.text(this.font, title, tx, y + 22, FADED, false);
        g.fill(tx, y + 32, tx + this.textW(), y + 33, 0x603B2A1C);

        // the words, typed out
        int left = this.typed;
        int ly = this.textY();
        for (String line : this.pages.get(this.page)) {
            if (left <= 0) {
                break;
            }
            String shown = line.length() <= left ? line : line.substring(0, left);
            g.text(this.font, shown, tx, ly, INK, false);
            left -= line.length();
            ly += 10;
        }
        if (!this.typing() && this.page < this.pages.size() - 1 && (this.ticks / 8) % 2 == 0) {
            g.text(this.font, "▼", tx + this.textW() - 6, this.textY() + LINES * 10 - 9, FADED, false);
        }
        // the gift under the greeting (below the text area, so it can show on every page of the greeting)
        if (!this.dialog.gift().isEmpty() && this.chosen < 0 && (this.page > 0 || !this.typing())) {
            int gy = this.optionY() - 15;
            g.item(this.dialog.gift(), tx, gy - 4);
            g.text(this.font, Component.translatable("npc.raft.received", this.dialog.gift().getHoverName()), tx + 20, gy, GIFT, false);
        }

        // options
        int hover = this.optionAt(mouseX, mouseY);
        for (int i = 0; i < this.options.size(); i++) {
            int[] r = this.optionRect(i);
            boolean over = i == hover;
            if (over) {
                g.fill(r[0] - 2, r[1] - 1, r[0] + r[2], r[1] + r[3] - 1, HOVER);
            }
            int color = i == this.chosen ? CHOSEN : over ? NAME : INK;
            String label = this.font.plainSubstrByWidth((i + 1) + ". " + this.options.get(i).label().getString(), r[2]);
            g.text(this.font, label, r[0], r[1] + 1, color, false);
        }
    }

    private void drawPortrait(GuiGraphicsExtractor g, int x, int y, int mouseX, int mouseY) {
        int w = 76, h = this.panelH() - 24;
        // frame: dark wood, then a little seascape behind the person
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xFF4A3524);
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF8C6B4A);
        g.fillGradient(x, y, x + w, y + h * 2 / 3, 0xFF8FCBEA, 0xFFCDE9F2);
        g.fillGradient(x, y + h * 2 / 3, x + w, y + h, 0xFF2F86B4, 0xFF1C5A80);
        g.fill(x, y + h * 2 / 3, x + w, y + h * 2 / 3 + 1, 0xFFE8F6FA);
        LivingEntity npc = this.npc();
        if (npc != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(g.vanilla(), x, y, x + w, y + h, 54, 0.0F, mouseX, mouseY, npc);
        }
    }
}
