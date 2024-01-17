package net.kasax.raft.screen;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.kasax.raft.Raft;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public class ModScreenHandlers {
    public static final ScreenHandlerType<ItemCollectorScreenHandler> ITEM_COLLECTOR_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, new Identifier(Raft.MOD_ID, "item_collecting"),
                    new ExtendedScreenHandlerType<>(ItemCollectorScreenHandler::new));

    public static void registerScreenHandlers() {
        Raft.LOGGER.info("Registering Screen Handlers for " + Raft.MOD_ID);
    }
}
