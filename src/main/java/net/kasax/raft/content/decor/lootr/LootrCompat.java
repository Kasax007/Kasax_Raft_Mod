package net.kasax.raft.content.decor.lootr;

import net.fabricmc.loader.api.FabricLoader;
import net.kasax.raft.content.decor.block.DecorChestBlockEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;

/**
 * Optional Lootr support for the wooden crates and treasure chests of the structures (3.4). Lootr turns vanilla
 * chests and barrels that carry a loot table into its own per-player containers by itself; Beach Craft's own
 * containers it does not know. With Lootr installed, a crate or treasure chest that still has its loot table keeps
 * it: every player who opens it gets their own roll of that table through Lootr (its wrapper, LootrDecorChest, makes
 * the block entity a Lootr container while it keeps its look). Without Lootr nothing changes: the first player rolls
 * the loot, like a vanilla chest.
 *
 * <p>This class does not touch any Lootr class; LootrBridge does, and it is only loaded when Lootr is there.
 */
public final class LootrCompat {
    public static final boolean LOADED = FabricLoader.getInstance().isModLoaded("lootr");

    private LootrCompat() {
    }

    /** True when Lootr handles this container (Lootr installed and active here, the container has a loot table). */
    public static boolean handles(DecorChestBlockEntity chest) {
        return LOADED && chest.getLootTable() != null && chest.getLevel() != null && LootrBridge.active(chest);
    }

    /** Opens the player's own inventory of this container through Lootr (server side). */
    public static void open(DecorChestBlockEntity chest, ServerPlayer player) {
        LootrBridge.open(chest, player);
    }

    /** True when the container is the player's Lootr inventory of this chest (keeps the lid open while it is open). */
    public static boolean isOwnMenu(Container container, DecorChestBlockEntity chest) {
        return LOADED && LootrBridge.isOwnMenu(container, chest);
    }
}
