package net.kasax.raft.gametest.mixin;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Opens a creative tab for the "tab" command (1.21.1 harness). */
@Mixin(CreativeModeInventoryScreen.class)
public interface CreativeScreenInvoker {
    @Invoker("selectTab")
    void bc$selectTab(CreativeModeTab tab);

    /** NeoForge pages the creative tabs: the pages, to show the one with the tab. */
    @org.spongepowered.asm.mixin.gen.Accessor("pages")
    java.util.List<net.neoforged.neoforge.client.gui.CreativeTabsScreenPage> bc$pages();
}
