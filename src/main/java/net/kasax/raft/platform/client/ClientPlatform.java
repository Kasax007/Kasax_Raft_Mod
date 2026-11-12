package net.kasax.raft.platform.client;

import net.kasax.raft.RaftClient;
import net.kasax.raft.platform.PayloadTypeRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client half of the loader layer (only loaded on a client). */
public final class ClientPlatform {
    private ClientPlatform() {
    }

    public static void init(IEventBus modBus) {
        PayloadTypeRegistry.clientDispatcher = ClientPlayNetworking::dispatch;
        modBus.addListener(EntityRenderersEvent.RegisterLayerDefinitions.class, ModelLayerRegistry::onRegisterLayers);
        modBus.addListener(EntityRenderersEvent.AddLayers.class, LivingEntityRenderLayerRegistrationCallback::onAddLayers);
        modBus.addListener(net.neoforged.neoforge.client.event.RegisterGuiLayersEvent.class, HudElementRegistry::onRegisterLayers);
    }

    /** Fabric's client entrypoint, run right after the common init. */
    public static void runClientInit() {
        new RaftClient().onInitializeClient();
    }
}
