package net.kasax.raft.platform;

import java.util.OptionalInt;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Fabric's ExtendedScreenHandlerFactory. {@code player.openMenu(factory)} keeps working: a mixin on
 * {@link ServerPlayer#openMenu(MenuProvider)} sends such menus through {@link #open}, which writes the opening data
 * with the codec of the menu's {@link ExtendedScreenHandlerType}, after the menu was created (as Fabric does).
 */
public interface ExtendedScreenHandlerFactory<D> extends MenuProvider {
    D getScreenOpeningData(ServerPlayer player);

    @SuppressWarnings("unchecked")
    static <D> OptionalInt open(ServerPlayer player, ExtendedScreenHandlerFactory<D> factory) {
        AbstractContainerMenu[] created = new AbstractContainerMenu[1];
        MenuProvider wrapper = new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return factory.getDisplayName();
            }

            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory inventory, Player p) {
                created[0] = factory.createMenu(syncId, inventory, p);
                return created[0];
            }

            @Override
            public boolean shouldTriggerClientSideContainerClosingOnOpen() {
                return factory.shouldTriggerClientSideContainerClosingOnOpen();
            }
        };
        return player.openMenu(wrapper, buf -> {
            if (!(created[0].getType() instanceof ExtendedScreenHandlerType<?, ?> type)) {
                throw new IllegalArgumentException("Extended screen handler factory " + factory + " made a menu without an extended type");
            }
            ((ExtendedScreenHandlerType<?, D>) type).getPacketCodec().encode(buf, factory.getScreenOpeningData(player));
        });
    }
}
