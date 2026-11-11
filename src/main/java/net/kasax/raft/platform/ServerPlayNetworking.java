package net.kasax.raft.platform;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Fabric's ServerPlayNetworking on NeoForge payloads; handlers run on the server thread, as on Fabric. */
public final class ServerPlayNetworking {
    private static final Map<ResourceLocation, PlayPayloadHandler<?>> HANDLERS = new ConcurrentHashMap<>();

    private ServerPlayNetworking() {
    }

    public static <T extends CustomPacketPayload> boolean registerGlobalReceiver(CustomPacketPayload.Type<T> type, PlayPayloadHandler<T> handler) {
        return HANDLERS.putIfAbsent(type.id(), handler) == null;
    }

    public static void send(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    public static boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type) {
        return player.connection != null && player.connection.hasChannel(type);
    }

    @SuppressWarnings("unchecked")
    static <T extends CustomPacketPayload> void dispatch(T payload, IPayloadContext context) {
        PlayPayloadHandler<T> handler = (PlayPayloadHandler<T>) HANDLERS.get(payload.type().id());
        if (handler != null && context.player() instanceof ServerPlayer player) {
            handler.receive(payload, () -> player);
        }
    }

    @FunctionalInterface
    public interface PlayPayloadHandler<T extends CustomPacketPayload> {
        void receive(T payload, Context context);
    }

    @FunctionalInterface
    public interface Context {
        ServerPlayer player();

        default MinecraftServer server() {
            return player().server;
        }
    }
}
