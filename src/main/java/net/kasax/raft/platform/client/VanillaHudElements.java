package net.kasax.raft.platform.client;

import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** Fabric's VanillaHudElements: the ids of NeoForge's vanilla GUI layers Beach Craft attaches to. */
public final class VanillaHudElements {
    public static final Identifier AIR_BAR = VanillaGuiLayers.AIR_LEVEL;
    public static final Identifier HOTBAR = VanillaGuiLayers.HOTBAR;
    public static final Identifier HEALTH_BAR = VanillaGuiLayers.PLAYER_HEALTH;

    private VanillaHudElements() {
    }
}
