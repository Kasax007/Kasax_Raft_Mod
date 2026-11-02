package net.kasax.raft.screen;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.kasax.raft.Raft;
import net.kasax.raft.util.BlockPosPayload;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public class ModScreenHandlers {
    public static final MenuType<ItemCollectorScreenHandler> ITEM_COLLECTOR_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, Raft.id("item_collecting"),
                    new MenuType<>(ItemCollectorScreenHandler::new, FeatureFlags.VANILLA_SET));

    public static final MenuType<MakeshiftBatteryScreenHandler> MAKESHIFT_BATTERY_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, Raft.id("makeshift_battery"),
                    new ExtendedMenuType<>(MakeshiftBatteryScreenHandler::new, BlockPosPayload.PACKET_CODEC));

    public static void registerScreenHandlers() {
        Raft.LOGGER.info("Registering Screen Handlers for " + Raft.MOD_ID);
    }
}
