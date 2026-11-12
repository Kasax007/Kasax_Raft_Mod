package net.kasax.raft.platform.transfer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;
import org.jspecify.annotations.Nullable;

/**
 * Fabric's ItemStorage on NeoForge's item capability. Like Fabric's transfer API, every Beach Craft block entity that is
 * a container can be reached by other mods' pipes ({@link #registerCapabilities}).
 */
public final class ItemStorage {
    public static final Lookup SIDED = new Lookup();

    private ItemStorage() {
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        for (BlockEntityType<?> type : BuiltInRegistries.BLOCK_ENTITY_TYPE) {
            var key = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type);
            if (key == null || !net.kasax.raft.Raft.MOD_ID.equals(key.getNamespace())) {
                continue;
            }
            event.registerBlockEntity(Capabilities.Item.BLOCK, type, (be, side) -> {
                if (be instanceof WorldlyContainer sided && side != null) {
                    return new WorldlyContainerWrapper(sided, side);
                }
                return be instanceof Container container ? VanillaContainerWrapper.of(container) : null;
            });
        }
    }

    public static final class Lookup {
        public @Nullable Storage<ItemVariant> find(ServerLevel level, BlockPos pos, @Nullable Direction side) {
            ResourceHandler<ItemResource> handler = level.getCapability(Capabilities.Item.BLOCK, pos, side);
            if (handler == null) {
                return null;
            }
            return (resource, maxAmount, transaction) -> resource.isBlank() || maxAmount <= 0 ? 0
                    : handler.insert(resource.resource(), (int) Math.min(Integer.MAX_VALUE, maxAmount), transaction.neo());
        }
    }
}
