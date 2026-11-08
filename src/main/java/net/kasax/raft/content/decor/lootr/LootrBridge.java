package net.kasax.raft.content.decor.lootr;

import net.kasax.raft.content.decor.block.DecorChestBlockEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import noobanidus.mods.lootr.common.api.LootrAPI;
import noobanidus.mods.lootr.common.api.data.inventory.ILootrInventory;

/** The calls into Lootr (only loaded when Lootr is installed, see LootrCompat). */
final class LootrBridge {
    private LootrBridge() {
    }

    static boolean active(DecorChestBlockEntity chest) {
        return !LootrAPI.isDisabled() && !LootrAPI.isDimensionBlocked(chest.getLevel().dimension())
                && !LootrAPI.isLootTableBlacklisted(chest.getLootTable());
    }

    static void open(DecorChestBlockEntity chest, ServerPlayer player) {
        LootrAPI.handleProviderOpen(LootrDecorChest.of(chest), player);
    }

    static boolean isOwnMenu(Container container, DecorChestBlockEntity chest) {
        return container instanceof ILootrInventory inventory && chest.hasLootrId()
                && inventory.getInfo().getInfoUUID().equals(chest.lootrId());
    }
}
