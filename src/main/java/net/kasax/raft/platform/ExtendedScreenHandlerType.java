package net.kasax.raft.platform;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.network.IContainerFactory;

/**
 * Fabric's ExtendedScreenHandlerType: a menu type whose opening data travels with the open screen packet. On NeoForge
 * the data is NeoForge's extra menu data, written by {@link ExtendedScreenHandlerFactory#open}.
 */
public class ExtendedScreenHandlerType<T extends AbstractContainerMenu, D> extends MenuType<T> {
    private final StreamCodec<? super RegistryFriendlyByteBuf, D> packetCodec;

    public ExtendedScreenHandlerType(ExtendedFactory<T, D> factory, StreamCodec<? super RegistryFriendlyByteBuf, D> packetCodec) {
        super((IContainerFactory<T>) (syncId, inventory, buf) -> factory.create(syncId, inventory, packetCodec.decode(buf)),
                FeatureFlags.VANILLA_SET);
        this.packetCodec = packetCodec;
    }

    public StreamCodec<? super RegistryFriendlyByteBuf, D> getPacketCodec() {
        return packetCodec;
    }

    @FunctionalInterface
    public interface ExtendedFactory<T extends AbstractContainerMenu, D> {
        T create(int syncId, Inventory inventory, D data);
    }
}
