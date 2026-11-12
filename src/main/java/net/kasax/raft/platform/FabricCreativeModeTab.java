package net.kasax.raft.platform;

import net.minecraft.world.item.CreativeModeTab;

/** Fabric's FabricCreativeModeTab.builder(): NeoForge's creative tab builder is public. */
public final class FabricCreativeModeTab {
    private FabricCreativeModeTab() {
    }

    public static CreativeModeTab.Builder builder() {
        return CreativeModeTab.builder();
    }
}
