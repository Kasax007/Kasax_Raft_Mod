package net.kasax.raft.platform;

import net.minecraft.world.item.CreativeModeTab;

/** Fabric's FabricItemGroup.builder(): NeoForge's creative tab builder is public. */
public final class FabricItemGroup {
    private FabricItemGroup() {
    }

    public static CreativeModeTab.Builder builder() {
        return CreativeModeTab.builder();
    }
}
