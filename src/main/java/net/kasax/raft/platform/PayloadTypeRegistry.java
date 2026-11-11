package net.kasax.raft.platform;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Fabric's PayloadTypeRegistry: the payload types are recorded and registered with NeoForge in
 * {@link RegisterPayloadHandlersEvent}, together with the handlers of {@link ServerPlayNetworking} (server bound) and
 * of the client networking (client bound, looked up when a payload arrives).
 */
public final class PayloadTypeRegistry {
    private static final PayloadTypeRegistry S2C = new PayloadTypeRegistry();
    private static final PayloadTypeRegistry C2S = new PayloadTypeRegistry();
    /** Set by the client layer; a dedicated server never receives client bound payloads. */
    public static volatile BiConsumer<CustomPacketPayload, IPayloadContext> clientDispatcher = (payload, context) -> {
    };

    private final List<Entry<?>> entries = new ArrayList<>();

    private PayloadTypeRegistry() {
    }

    public static PayloadTypeRegistry playS2C() {
        return S2C;
    }

    public static PayloadTypeRegistry playC2S() {
        return C2S;
    }

    public synchronized <T extends CustomPacketPayload> void register(
            CustomPacketPayload.Type<T> id, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        for (Entry<?> e : entries) {
            if (e.type.id().equals(id.id())) {
                throw new IllegalArgumentException("Packet type " + id + " is already registered!");
            }
        }
        entries.add(new Entry<>(id, codec));
    }

    static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        for (Entry<?> e : S2C.entries) {
            e.toClient(registrar);
        }
        for (Entry<?> e : C2S.entries) {
            e.toServer(registrar);
        }
    }

    private record Entry<T extends CustomPacketPayload>(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        void toClient(PayloadRegistrar registrar) {
            registrar.playToClient(type, codec, (payload, context) -> clientDispatcher.accept(payload, context));
        }

        void toServer(PayloadRegistrar registrar) {
            registrar.playToServer(type, codec, ServerPlayNetworking::dispatch);
        }
    }
}
