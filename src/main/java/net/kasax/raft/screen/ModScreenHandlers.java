package net.kasax.raft.screen;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.kasax.raft.Raft;
import net.kasax.raft.util.BlockPosPayload;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public class ModScreenHandlers {
    public static final ScreenHandlerType<ItemCollectorScreenHandler> ITEM_COLLECTOR_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(Raft.MOD_ID, "item_collecting"),
                    new ScreenHandlerType<>(ItemCollectorScreenHandler::new, FeatureSet.empty()));

    public static final ScreenHandlerType<MakeshiftBatteryScreenHandler> MAKESHIFT_BATTERY_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(Raft.MOD_ID, "makeshift_battery"),
                    new ExtendedScreenHandlerType<>(MakeshiftBatteryScreenHandler::new, BlockPosPayload.PACKET_CODEC));

    public static void registerScreenHandlers() {
        Raft.LOGGER.info("Registering Screen Handlers for " + Raft.MOD_ID);
    }
}
