package net.kasax.raft.content.guide.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.kasax.raft.Raft;
import net.kasax.raft.content.guide.client.AlmanacBook.Chapter;
import net.kasax.raft.content.guide.client.AlmanacBook.Section;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The Seafarer's Almanac: an open logbook in oiled sea-green leather. Spread 0 is the contents (chapter buttons with
 * icons and page numbers); every chapter starts on a new double page. Text comes from the lang file and is laid out on
 * the ruled lines of the pages ({@code # } heading, {@code * } bullet, {@code ---} new page). Creature pages show the
 * real entity in a framed plate. The book remembers the last double page; an item bookmark
 * ({@code custom_data={almanac:"chapter"}}, {@code "chapter/section"} or {@code "chapter+2"} for the third double page
 * of a chapter) opens that page instead.
 */
public class AlmanacScreen extends Screen {
    private static final Identifier TEXTURE = Raft.id("textures/gui/almanac.png");
    private static final int TEX_W = 512, TEX_H = 256;
    private static final int W = 320, H = 200, RIBBON_H = 26;
    private static final int[] TEXT_X = {22, 170};
    private static final int TEXT_W = 128, ROW0 = 16, ROW_H = 10, ROWS = 16, PAGE_NO_Y = 179;
    private static final int PLATE_H = 118;

    private static final int INK = 0xFF3B2A1C, FADED = 0xFF8A7656, TITLE = 0xFF1F4E4A, HEADING = 0xFF1F6E6E,
            WARN = 0xFF8E2A20, HOVER = 0x283B2A1C;

    private static final int TEXT = 0, HEAD = 1, BULLET = 2, BULLET_CONT = 3, BLANK = 4;

    private record Line(FormattedCharSequence text, int kind) {
    }

    /** One book page. section == null: an empty page (padding at the end of a chapter). */
    private record Page(Chapter chapter, Section section, boolean first, List<Line> lines) {
    }

    /** Last double page read (kept while the game runs). */
    private static int lastSpread;

    private final String bookmark;
    private final List<Chapter> chapters;
    private final List<Page> pages = new ArrayList<>();
    private final List<Integer> chapterSpread = new ArrayList<>();
    private final Map<String, Integer> sectionSpread = new HashMap<>();
    private final Map<String, ItemStack> stacks = new HashMap<>();
    private final Map<String, LivingEntity> creatures = new HashMap<>();
    private int spread;
    private int left;
    private int top;
    private long openedAt;

    public AlmanacScreen(ItemStack book) {
        super(Component.translatable("item.raft.seafarers_almanac"));
        CustomData data = book.get(DataComponents.CUSTOM_DATA);
        this.bookmark = data == null ? "" : data.copyTag().getStringOr("almanac", "");
        this.chapters = AlmanacBook.chapters(leviathanDefeated());
    }

    /** The Leviathan chapter shows a spoiler warning first, until the player has the story/leviathan advancement. */
    private static boolean leviathanDefeated() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return false;
        }
        ClientAdvancements advancements = connection.getAdvancements();
        AdvancementHolder holder = advancements.get(Raft.id("story/leviathan"));
        AdvancementProgress progress = holder == null ? null : advancements.progress().get(holder);
        return progress != null && progress.isDone();
    }

    @Override
    protected void init() {
        this.left = (this.width - W) / 2;
        this.top = Math.max(2, (this.height - H - RIBBON_H + 6) / 2);
        boolean firstInit = this.pages.isEmpty();
        paginate();
        if (firstInit) {
            this.openedAt = System.currentTimeMillis();
            Integer target = bookmarkSpread(this.bookmark);
            this.spread = Mth.clamp(target != null ? target : lastSpread, 0, maxSpread());
        } else {
            this.spread = Mth.clamp(this.spread, 0, maxSpread());
        }
        lastSpread = this.spread;
    }

    /** "diving", "creatures/whale" or with a page offset "survival+2" (double pages after the start). */
    private Integer bookmarkSpread(String mark) {
        if (mark.isEmpty()) {
            return null;
        }
        int plus = mark.indexOf('+');
        int offset = 0;
        if (plus > 0) {
            try {
                offset = Integer.parseInt(mark.substring(plus + 1));
            } catch (NumberFormatException ignored) {
            }
            mark = mark.substring(0, plus);
        }
        Integer start = this.sectionSpread.get(mark);
        return start == null ? null : start + offset;
    }

    private int maxSpread() {
        return this.pages.size() / 2;
    }

    // ------------------------------------------------------------------------------------------- layout
    private void paginate() {
        this.pages.clear();
        this.chapterSpread.clear();
        this.sectionSpread.clear();
        for (Chapter chapter : this.chapters) {
            int start = this.pages.size();
            this.chapterSpread.add(start / 2 + 1);
            this.sectionSpread.put(chapter.id(), start / 2 + 1);
            for (Section section : chapter.sections()) {
                if (section.newSpread() && (this.pages.size() - start) % 2 == 1) {
                    this.pages.add(new Page(chapter, null, false, List.of()));
                }
                if (!section.id().isEmpty()) {
                    this.sectionSpread.put(chapter.id() + "/" + section.id(), this.pages.size() / 2 + 1);
                }
                layoutSection(chapter, section);
            }
            if ((this.pages.size() - start) % 2 == 1) {
                this.pages.add(new Page(chapter, null, false, List.of()));
            }
        }
    }

    /** Rows taken by the title, icons or seal at the top of a section's first page. */
    private int headerRows(Section s) {
        int rows = 2;                                   // title + divider
        if (s.warning()) {
            rows += 4;
        }
        if (!icons(s).isEmpty()) {
            rows += 2;
        }
        return rows;
    }

    private void layoutSection(Chapter chapter, Section section) {
        String text = Component.translatable("almanac.raft." + section.key() + ".text").getString();
        boolean first = true;
        int capacity = ROWS - headerRows(section);
        if (section.entity() != null) {
            // a creature: the left page is its plate (title, portrait, drops), the text starts on the right page
            this.pages.add(new Page(chapter, section, true, List.of()));
            first = false;
            capacity = ROWS;
        }
        List<Line> lines = new ArrayList<>();
        int prevKind = -1;
        for (String raw : text.split("\n")) {
            String para = raw.strip();
            if (para.isEmpty()) {
                continue;
            }
            if (para.equals("---")) {
                if (!lines.isEmpty()) {
                    this.pages.add(new Page(chapter, section, first, lines));
                    lines = new ArrayList<>();
                    first = false;
                    capacity = ROWS;
                }
                prevKind = -1;
                continue;
            }
            int kind = para.startsWith("# ") ? HEAD : para.startsWith("* ") ? BULLET : TEXT;
            String body = kind == TEXT ? para : para.substring(2);
            List<Line> block = new ArrayList<>();
            if (kind == BULLET) {
                List<FormattedCharSequence> wrapped = this.font.split(Component.literal(body), TEXT_W - 7);
                for (int i = 0; i < wrapped.size(); i++) {
                    block.add(new Line(wrapped.get(i), i == 0 ? BULLET : BULLET_CONT));
                }
            } else {
                Component c = kind == HEAD ? Component.literal(body).withStyle(st -> st.withBold(true)) : Component.literal(body);
                for (FormattedCharSequence seq : this.font.split(c, TEXT_W)) {
                    block.add(new Line(seq, kind));
                }
            }
            boolean gap = !lines.isEmpty() && prevKind != HEAD && !(prevKind == BULLET && kind == BULLET);
            int free = capacity - lines.size() - (gap ? 1 : 0);
            // headings keep two lines of their paragraph, short paragraphs stay whole, nothing leaves a lone line
            boolean newPage = kind == HEAD ? free < 3 : block.size() <= 3 ? free < block.size() : free < 2;
            if (newPage && !lines.isEmpty()) {
                this.pages.add(new Page(chapter, section, first, lines));
                lines = new ArrayList<>();
                first = false;
                capacity = ROWS;
                gap = false;
            }
            if (gap) {
                lines.add(new Line(FormattedCharSequence.EMPTY, BLANK));
            }
            for (Line line : block) {
                if (lines.size() >= capacity) {
                    this.pages.add(new Page(chapter, section, first, lines));
                    lines = new ArrayList<>();
                    first = false;
                    capacity = ROWS;
                }
                lines.add(line);
            }
            prevKind = kind;
        }
        if (!lines.isEmpty() || first) {
            this.pages.add(new Page(chapter, section, first, lines));
        }
    }

    // ------------------------------------------------------------------------------------------- input
    private void turnTo(int to) {
        int s = Mth.clamp(to, 0, maxSpread());
        if (s != this.spread && this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
        }
        this.spread = s;
        lastSpread = s;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == InputConstants.KEY_RIGHT || key == InputConstants.KEY_PAGEDOWN) {
            turnTo(this.spread + 1);
            return true;
        }
        if (key == InputConstants.KEY_LEFT || key == InputConstants.KEY_PAGEUP) {
            turnTo(this.spread - 1);
            return true;
        }
        if (key == InputConstants.KEY_HOME) {
            turnTo(0);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (scrollY != 0) {
            turnTo(this.spread + (scrollY < 0 ? 1 : -1));
        }
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            double mx = event.x(), my = event.y();
            if (overPrev(mx, my)) {
                turnTo(this.spread - 1);
                return true;
            }
            if (overNext(mx, my)) {
                turnTo(this.spread + 1);
                return true;
            }
            if (overRibbon(mx, my)) {
                turnTo(0);
                return true;
            }
            if (this.spread == 0) {
                int hit = contentsEntryAt(mx, my);
                if (hit >= 0) {
                    turnTo(this.chapterSpread.get(hit));
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private boolean overPrev(double mx, double my) {
        int x = this.left + TEXT_X[0] - 2, y = this.top + PAGE_NO_Y - 1;
        return this.spread > 0 && mx >= x && mx < x + 18 && my >= y - 2 && my < y + 12;
    }

    private boolean overNext(double mx, double my) {
        int x = this.left + TEXT_X[1] + TEXT_W - 16, y = this.top + PAGE_NO_Y - 1;
        return this.spread < maxSpread() && mx >= x && mx < x + 18 && my >= y - 2 && my < y + 12;
    }

    private boolean overRibbon(double mx, double my) {
        int x = this.left + W / 2 - 6, y = this.top + H - 4;
        return mx >= x && mx < x + 12 && my >= y && my < y + RIBBON_H;
    }

    /**
     * Contents entries: 6 on the left page (under the heading), 8 on the right page, two ruled rows each. With more
     * than 14 chapters the entries move closer together (16 px apart: 7 left, 10 right).
     */
    private int[] contentsRect(int i) {
        if (this.chapters.size() <= 14) {
            int page = i < 6 ? 0 : 1;
            int row = page == 0 ? 4 + 2 * i : 2 * (i - 6);
            return new int[]{this.left + TEXT_X[page] - 2, this.top + ROW0 + ROW_H * row - 5, TEXT_W + 4, 19};
        }
        int page = i < 7 ? 0 : 1;
        int y = page == 0 ? ROW0 + ROW_H * 4 - 5 + 16 * i : ROW0 - 5 + 16 * (i - 7);
        return new int[]{this.left + TEXT_X[page] - 2, this.top + y, TEXT_W + 4, 16};
    }

    private int contentsEntryAt(double mx, double my) {
        for (int i = 0; i < this.chapters.size(); i++) {
            int[] r = contentsRect(i);
            if (mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        for (LivingEntity e : this.creatures.values()) {
            if (e != null) {
                e.tickCount++;
            }
        }
    }

    // ------------------------------------------------------------------------------------------- drawing
    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        boolean ribbonHover = overRibbon(mouseX, mouseY);
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.left + W / 2 - 6, this.top + H - 4 + (ribbonHover ? 1 : 0),
                ribbonHover ? 372 : 360, 0, 12, RIBBON_H, TEX_W, TEX_H);
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.left, this.top, 0, 0, W, H, TEX_W, TEX_H);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        if (this.spread == 0) {
            drawContents(g, mouseX, mouseY);
        } else {
            int index = (this.spread - 1) * 2;
            for (int side = 0; side < 2; side++) {
                if (index + side < this.pages.size()) {
                    drawPage(g, this.pages.get(index + side), side, mouseX, mouseY, a);
                    String num = String.valueOf(index + side + 1);
                    int cx = this.left + TEXT_X[side] + TEXT_W / 2;
                    g.text(this.font, num, cx - this.font.width(num) / 2, this.top + PAGE_NO_Y, FADED, false);
                }
            }
        }
        drawArrow(g, mouseX, mouseY, false);
        drawArrow(g, mouseX, mouseY, true);
        if (overRibbon(mouseX, mouseY)) {
            g.requestCursor(CursorTypes.POINTING_HAND);
            g.setTooltipForNextFrame(this.font, Component.translatable("gui.raft.almanac.contents"), mouseX, mouseY);
        }
    }

    private void drawArrow(GuiGraphicsExtractor g, int mouseX, int mouseY, boolean next) {
        if (next ? this.spread >= maxSpread() : this.spread <= 0) {
            return;
        }
        boolean hover = next ? overNext(mouseX, mouseY) : overPrev(mouseX, mouseY);
        int x = next ? this.left + TEXT_X[1] + TEXT_W - 16 : this.left + TEXT_X[0] - 2;
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, this.top + PAGE_NO_Y - 1, hover ? 338 : 320, next ? 0 : 10,
                18, 10, TEX_W, TEX_H);
        if (hover) {
            g.requestCursor(CursorTypes.POINTING_HAND);
        }
    }

    private int rowY(int row) {
        return this.top + ROW0 + ROW_H * row;
    }

    private void drawContents(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int x = this.left + TEXT_X[0];
        Component title = Component.translatable("gui.raft.almanac.title").withStyle(s -> s.withBold(true));
        centered(g, title, x + TEXT_W / 2, rowY(0), TITLE);
        divider(g, x, rowY(0) + 11);
        Component sub = Component.translatable("gui.raft.almanac.subtitle").withStyle(s -> s.withItalic(true));
        centered(g, sub, x + TEXT_W / 2, rowY(2), FADED);
        int hover = contentsEntryAt(mouseX, mouseY);
        for (int i = 0; i < this.chapters.size(); i++) {
            Chapter c = this.chapters.get(i);
            int[] r = contentsRect(i);
            if (i == hover) {
                g.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], HOVER);
                g.requestCursor(CursorTypes.POINTING_HAND);
            }
            ItemStack icon = stack(c.icon());
            boolean compact = r[3] < 19;
            if (!icon.isEmpty()) {
                g.item(icon, r[0] + 2, r[1] + (compact ? 0 : 1));
            }
            int ty = r[1] + (compact ? 4 : 5);
            String pageNo = String.valueOf((this.chapterSpread.get(i) - 1) * 2 + 1);
            int numX = r[0] + r[2] - 3 - this.font.width(pageNo);
            Component name = Component.translatable(c.titleKey());
            int nameX = r[0] + 21;
            int maxName = numX - 4 - nameX;
            FormattedCharSequence nameSeq = this.font.width(name) > maxName
                    ? this.font.split(name, maxName).get(0) : name.getVisualOrderText();
            g.text(this.font, nameSeq, nameX, ty, i == hover ? HEADING : INK, false);
            // dotted leader to the page number, like a real table of contents
            for (int dx = nameX + this.font.width(nameSeq) + 3; dx < numX - 3; dx += 3) {
                g.fill(dx, ty + 7, dx + 1, ty + 8, 0x803B2A1C);
            }
            g.text(this.font, pageNo, numX, ty, FADED, false);
        }
        Component hint = Component.translatable("gui.raft.almanac.hint").withStyle(s -> s.withItalic(true));
        centered(g, hint, x + TEXT_W / 2, this.top + PAGE_NO_Y, FADED);
    }

    /** Centred text without the drop shadow (ink on paper). */
    private void centered(GuiGraphicsExtractor g, Component text, int cx, int y, int color) {
        g.text(this.font, text, cx - this.font.width(text) / 2, y, color, false);
    }

    private void divider(GuiGraphicsExtractor g, int x, int y) {
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 320, 30, 128, 5, TEX_W, TEX_H);
    }

    private void drawPage(GuiGraphicsExtractor g, Page p, int side, int mouseX, int mouseY, float partial) {
        int x = this.left + TEXT_X[side];
        if (p.section() == null) {
            // an empty page at the end of a chapter: a faded compass rose
            g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + TEXT_W / 2 - 15, this.top + 82, 480, 0, 31, 31, TEX_W, TEX_H);
            return;
        }
        Section s = p.section();
        int row = 0;
        if (p.first()) {
            Component title = Component.translatable("almanac.raft." + s.key() + ".title").withStyle(st -> st.withBold(true));
            float scale = Math.min(1.25F, TEXT_W / (float) Math.max(1, this.font.width(title)));
            g.pose().pushMatrix();
            g.pose().translate(x, rowY(0) + (scale > 1.0F ? -1 : 0));
            g.pose().scale(scale, scale);
            g.text(this.font, title, 0, 0, s.warning() ? WARN : TITLE, false);
            g.pose().popMatrix();
            divider(g, x, rowY(0) + 11);
            row = 2;
            if (s.warning()) {
                g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + TEXT_W / 2 - 13, rowY(2) - 3, 450, 0, 26, 26, TEX_W, TEX_H);
                row += 4;
            }
            if (s.entity() != null) {
                drawPortrait(g, s.entity(), x, rowY(2) - 1, mouseX, mouseY, partial);
                row += 12;
            }
            List<ItemStack> icons = icons(s);
            if (!icons.isEmpty()) {
                int iy = rowY(row) - 1;
                for (int i = 0; i < icons.size() && i < 7; i++) {
                    int ix = x + i * 18;
                    g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, ix, iy, 384, 0, 18, 18, TEX_W, TEX_H);
                    g.item(icons.get(i), ix + 1, iy + 1);
                    if (mouseX >= ix && mouseX < ix + 18 && mouseY >= iy && mouseY < iy + 18) {
                        g.setTooltipForNextFrame(this.font, icons.get(i), mouseX, mouseY);
                    }
                }
                row += 2;
            }
        }
        int color = s.warning() ? WARN : INK;
        for (Line line : p.lines()) {
            int y = rowY(row);
            switch (line.kind()) {
                case HEAD -> g.text(this.font, line.text(), x, y, HEADING, false);
                case BULLET -> {
                    g.fill(x + 1, y + 3, x + 4, y + 6, HEADING);
                    g.text(this.font, line.text(), x + 7, y, color, false);
                }
                case BULLET_CONT -> g.text(this.font, line.text(), x + 7, y, color, false);
                case BLANK -> {
                }
                default -> g.text(this.font, line.text(), x, y, color, false);
            }
            row++;
        }
    }

    // ------------------------------------------------------------------------------------------- items and creatures
    private List<ItemStack> icons(Section s) {
        List<ItemStack> list = new ArrayList<>();
        for (String id : s.icons()) {
            ItemStack stack = stack(id);
            if (!stack.isEmpty()) {
                list.add(stack);
            }
        }
        return list;
    }

    private ItemStack stack(String id) {
        return this.stacks.computeIfAbsent(id, k -> {
            Identifier ident = Identifier.tryParse(k);
            Item item = ident == null ? Items.AIR : BuiltInRegistries.ITEM.getValue(ident);
            return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
        });
    }

    private LivingEntity creature(String id) {
        if (this.minecraft == null || this.minecraft.level == null) {
            return null;
        }
        if (this.creatures.containsKey(id)) {
            return this.creatures.get(id);
        }
        LivingEntity living = null;
        Identifier ident = Identifier.tryParse(id);
        Optional<EntityType<?>> type = ident == null ? Optional.empty() : BuiltInRegistries.ENTITY_TYPE.getOptional(ident);
        if (type.isPresent()) {
            try {
                Entity e = type.get().create(this.minecraft.level, EntitySpawnReason.LOAD);
                if (e instanceof LivingEntity l) {
                    l.setId(-3000 - this.creatures.size());   // never added to the level, but rendering wants an id
                    living = l;
                }
            } catch (RuntimeException ex) {
                Raft.LOGGER.warn("Almanac: cannot draw {}: {}", id, ex.toString());
            }
        }
        this.creatures.put(id, living);
        return living;
    }

    /**
     * Per creature: size factor, view angle (180 = facing the reader, 270 = side view) and camera tilt (degrees,
     * negative = from above). Long models reach beyond their hitbox, flat ones look best from above.
     */
    private static float[] portraitTweak(String id) {
        return switch (id) {
            case "raft:whale" -> new float[]{1.3F, 245.0F, -12.0F};
            case "raft:shark" -> new float[]{0.95F, 245.0F, -12.0F};
            case "raft:hammerhead_shark" -> new float[]{0.85F, 215.0F, -30.0F};
            case "raft:moray_eel" -> new float[]{0.4F, 235.0F, -15.0F};
            case "raft:manta_ray" -> new float[]{1.15F, 205.0F, -40.0F};
            case "raft:anglerfish" -> new float[]{0.75F, 240.0F, -10.0F};
            case "raft:tideling" -> new float[]{0.55F, 235.0F, -12.0F};
            case "raft:jellyfish" -> new float[]{0.75F, 215.0F, -12.0F};
            case "raft:crab", "raft:ember_crab" -> new float[]{0.5F, 200.0F, -22.0F};
            case "raft:seagull" -> new float[]{0.75F, 215.0F, -12.0F};
            case "raft:sea_otter" -> new float[]{0.8F, 215.0F, -12.0F};
            default -> new float[]{1.0F, 215.0F, -12.0F};
        };
    }

    private void drawPortrait(GuiGraphicsExtractor g, String id, int x, int y, int mouseX, int mouseY, float partial) {
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 320, 40, TEXT_W, PLATE_H, TEX_W, TEX_H);
        LivingEntity entity = creature(id);
        if (entity == null) {
            return;
        }
        int x0 = x + 2, y0 = y + 2, x1 = x + TEXT_W - 2, y1 = y + PLATE_H - 2;
        float[] tweak = portraitTweak(id);
        float bbW = Math.max(0.2F, entity.getBbWidth()), bbH = Math.max(0.2F, entity.getBbHeight());
        float size = Math.min(70.0F / bbH, 84.0F / bbW) * tweak[0];
        // a three-quarter view that slowly sways, a little from above
        float time = (System.currentTimeMillis() - this.openedAt) / 1000.0F;
        float yaw = tweak[1] + Mth.sin(time * 0.6F) * 15.0F;
        try {
            EntityRenderer<? super LivingEntity, ?> renderer = this.minecraft.getEntityRenderDispatcher().getRenderer(entity);
            EntityRenderState state = renderer.createRenderState(entity, partial);
            state.shadowPieces.clear();
            state.outlineColor = 0;
            if (state instanceof LivingEntityRenderState living) {
                living.bodyRot = yaw;
                living.yRot = 0.0F;
                living.xRot = 0.0F;
                living.boundingBoxWidth = living.boundingBoxWidth / living.scale;
                living.boundingBoxHeight = living.boundingBoxHeight / living.scale;
                living.scale = 1.0F;
            }
            Quaternionf tilt = new Quaternionf().rotateX(tweak[2] * Mth.DEG_TO_RAD);
            Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI).mul(tilt);
            Vector3f translation = new Vector3f(0.0F, state.boundingBoxHeight / 2.0F + 0.0625F, 0.0F);
            g.entity(state, size, translation, rotation, tilt, x0, y0, x1, y1);
        } catch (RuntimeException ex) {
            Raft.LOGGER.warn("Almanac: cannot draw {}: {}", id, ex.toString());
            this.creatures.put(id, null);
        }
    }
}
