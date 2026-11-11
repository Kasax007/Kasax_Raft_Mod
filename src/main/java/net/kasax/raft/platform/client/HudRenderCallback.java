package net.kasax.raft.platform.client;

import net.kasax.raft.platform.Event;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Fabric's HudRenderCallback: drawn after the vanilla HUD, as on Fabric. */
@FunctionalInterface
public interface HudRenderCallback {
    Event<HudRenderCallback> EVENT = new Event<>(() -> NeoForge.EVENT_BUS.addListener(RenderGuiEvent.Post.class, e -> {
        for (HudRenderCallback l : HudRenderCallback.EVENT.listeners()) l.onHudRender(e.getGuiGraphics(), e.getPartialTick());
    }));

    void onHudRender(GuiGraphics drawContext, DeltaTracker tickCounter);
}
