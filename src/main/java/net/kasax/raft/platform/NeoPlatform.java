package net.kasax.raft.platform;

import net.kasax.raft.platform.energy.EnergyStorage;
import net.kasax.raft.platform.energy.SimpleEnergyItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Bootstrap of the NeoForge loader layer (see the package description): called from the {@code @Mod} constructor.
 */
public final class NeoPlatform {
    private static IEventBus modBus;
    private static Dist dist;
    private static boolean initialized;

    private NeoPlatform() {
    }

    private static Runnable commonInit;

    public static void init(IEventBus bus, ModContainer container, Dist side, Runnable modInit) {
        modBus = bus;
        commonInit = modInit;
        dist = side;
        bus.addListener(EventPriority.HIGHEST, RegisterEvent.class, NeoPlatform::onRegister);
        bus.addListener(EntityAttributeCreationEvent.class, FabricDefaultAttributeRegistry::onAttributes);
        bus.addListener(RegisterPayloadHandlersEvent.class, PayloadTypeRegistry::onRegisterPayloads);
        bus.addListener(RegisterCapabilitiesEvent.class, NeoPlatform::onCapabilities);
        bus.addListener(BuildCreativeModeTabContentsEvent.class, ItemGroupEvents::onBuildContents);
        bus.addListener(RegisterEvent.class, BiomeModifications::onRegister);
        if (side.isClient()) {
            net.kasax.raft.platform.client.ClientPlatform.init(bus);
        }
    }

    public static IEventBus modBus() {
        return modBus;
    }

    public static boolean isClient() {
        return dist != null && dist.isClient();
    }

    /** Fabric order: every mod registration (common init), then the client init; all registries are open now. */
    private static void onRegister(RegisterEvent event) {
        if (initialized) {
            return;
        }
        initialized = true;
        SimpleEnergyItem.registerComponent();
        commonInit.run();
        if (dist.isClient()) {
            net.kasax.raft.platform.client.ClientPlatform.runClientInit();
        }
    }

    private static void onCapabilities(RegisterCapabilitiesEvent event) {
        EnergyStorage.SIDED.registerCapabilities(event);
        SimpleEnergyItem.registerCapabilities(event);
        net.kasax.raft.platform.transfer.ItemStorage.registerCapabilities(event);
    }
}
