package net.kasax.raft.content.voyage.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.kasax.raft.content.voyage.ExpeditionRaftMenu;
import net.kasax.raft.compat.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.kasax.raft.compat.gui.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;

/** Raft storage: 3 or 6 rows of cargo, plus the engine panel (fuel slot, flame) and the anchor lever on the right. */
@Environment(EnvType.CLIENT)
public class ExpeditionRaftScreen extends net.kasax.raft.compat.gui.ContainerScreen<ExpeditionRaftMenu> {
    private static final ResourceLocation TEXTURE = net.kasax.raft.Raft.id("textures/gui/expedition_raft.png");
    private static final int WIDTH = 210;
    private static final int BOTTOM_V = 126;
    private static final int FLAME_X = 187;
    private static final int FLAME_Y = 19;

    private final int rows;
    private @Nullable Button anchorButton;
    private boolean shownAnchored;

    public ExpeditionRaftScreen(ExpeditionRaftMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, 114 + menu.getRows() * 18);
        this.rows = menu.getRows();
    }

    @Override
    protected void init() {
        super.init();
        this.anchorButton = null;
        if (this.menu.hasAnchor()) {
            this.shownAnchored = this.menu.isAnchored();
            this.anchorButton = this.addRenderableWidget(Button.builder(anchorLabel(this.shownAnchored), b -> {
                        if (this.minecraft != null && this.minecraft.gameMode != null) {
                            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, ExpeditionRaftMenu.BUTTON_ANCHOR);
                        }
                    })
                    .bounds(this.leftPos + 179, this.topPos + 55, 26, 14)
                    .tooltip(Tooltip.create(anchorTooltip(this.shownAnchored)))
                    .build());
        }
    }

    private static Component anchorLabel(boolean anchored) {
        return Component.translatable(anchored ? "container.raft.expedition_raft.anchor_up_short" : "container.raft.expedition_raft.anchor_down_short");
    }

    private static Component anchorTooltip(boolean anchored) {
        return Component.translatable(anchored ? "container.raft.expedition_raft.anchor_up" : "container.raft.expedition_raft.anchor_down");
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.anchorButton != null && this.menu.isAnchored() != this.shownAnchored) {
            this.shownAnchored = this.menu.isAnchored();
            this.anchorButton.setMessage(anchorLabel(this.shownAnchored));
            this.anchorButton.setTooltip(Tooltip.create(anchorTooltip(this.shownAnchored)));
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);
        int top = this.rows * 18 + 17;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, top, 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos + top, 0, BOTTOM_V, this.imageWidth, 96, 256, 256);
        if (this.menu.hasEngine()) {
            if (this.menu.isBurning()) {
                int flame = this.menu.getBurnProgress();
                graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos + FLAME_X, this.topPos + FLAME_Y + 13 - flame,
                        212, 13 - flame, 14, flame + 1, 256, 256);
            }
        } else {
            // no engine: the fuel bay is closed with a hatch
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos + 184, this.topPos + 17, 230, 0, 22, 36, 256, 256);
        }
        if (!this.menu.hasAnchor()) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos + 179, this.topPos + 55, 212, 40, 26, 14, 256, 256);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (this.hoveredSlot == null && this.isHovering(181, 16, 26, 38, mouseX, mouseY)) {
            String key = !this.menu.hasEngine() ? "container.raft.expedition_raft.no_engine"
                    : this.menu.isBurning() ? "container.raft.expedition_raft.engine_running" : "container.raft.expedition_raft.engine_fuel";
            graphics.setTooltipForNextFrame(this.font, Component.translatable(key), mouseX, mouseY);
        } else if (this.anchorButton == null && this.isHovering(179, 55, 26, 14, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(this.font, Component.translatable("container.raft.expedition_raft.no_anchor"), mouseX, mouseY);
        }
    }
}
