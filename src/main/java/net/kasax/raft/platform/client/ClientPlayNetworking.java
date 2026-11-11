package net.kasax.raft.platform.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Fabric's ClientPlayNetworking on NeoForge payloads; handlers run on the client thread, as on Fabric. */
public final class ClientPlayNetworking {
    private static final Map<ResourceLocation, PlayPayloadHandler<?>> HANDLERS = new ConcurrentHashMap<>();

    private ClientPlayNetworking() {
    }

    public static <T extends CustomPacketPayload> boolean registerGlobalReceiver(CustomPacketPayload.Type<T> type, PlayPayloadHandler<T> handler) {
        return HANDLERS.putIfAbsent(type.id(), handler) == null;
    }

    public static void send(CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    public static boolean canSend(CustomPacketPayload.Type<?> type) {
        var connection = Minecraft.getInstance().getConnection();
        return connection != null && connection.hasChannel(type);
    }

    @SuppressWarnings("unchecked")
    static <T extends CustomPacketPayload> void dispatch(T payload, IPayloadContext context) {
        PlayPayloadHandler<T> handler = (PlayPayloadHandler<T>) HANDLERS.get(payload.type().id());
        if (handler != null) {
            Minecraft mc = Minecraft.getInstance();
            handler.receive(payload, new Context() {
                @Override
                public Minecraft client() {
                    return mc;
                }

                @Override
                public LocalPlayer player() {
                    return mc.player;
                }
            });
        }
    }

    @FunctionalInterface
    public interface PlayPayloadHandler<T extends CustomPacketPayload> {
        void receive(T payload, Context context);
    }

    public interface Context {
        Minecraft client();

        LocalPlayer player();
    }
}
