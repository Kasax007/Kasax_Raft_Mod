package net.kasax.raft.platform.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/** Fabric's HudElementRegistry on NeoForge's GUI layers ({@link RegisterGuiLayersEvent}). */
public final class HudElementRegistry {
    private static final List<java.util.function.Consumer<RegisterGuiLayersEvent>> LAYERS = new ArrayList<>();

    private HudElementRegistry() {
    }

    public static void attachElementAfter(Identifier beforeThis, Identifier id, HudElement element) {
        LAYERS.add(event -> event.registerAbove(beforeThis, id, element::extractRenderState));
    }

    public static void attachElementBefore(Identifier afterThis, Identifier id, HudElement element) {
        LAYERS.add(event -> event.registerBelow(afterThis, id, element::extractRenderState));
    }

    public static void addLast(Identifier id, HudElement element) {
        LAYERS.add(event -> event.registerAboveAll(id, element::extractRenderState));
    }

    static void onRegisterLayers(RegisterGuiLayersEvent event) {
        for (var layer : LAYERS) layer.accept(event);
    }

    @FunctionalInterface
    public interface HudElement {
        void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker);
    }
}
