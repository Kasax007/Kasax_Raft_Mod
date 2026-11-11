package net.kasax.raft.platform.transfer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import org.jetbrains.annotations.Nullable;

/**
 * Fabric's ItemStorage on NeoForge's item handler capability. Like Fabric's transfer API, every Beach Craft block
 * entity that is a container can be reached by other mods' pipes ({@link #registerCapabilities}).
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
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type, (be, side) -> {
                if (be instanceof WorldlyContainer sided && side != null) {
                    return new SidedInvWrapper(sided, side);
                }
                return be instanceof Container container ? new InvWrapper(container) : null;
            });
        }
    }

    public static final class Lookup {
        @Nullable
        public Storage<ItemVariant> find(ServerLevel level, BlockPos pos, @Nullable Direction side) {
            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
            return handler == null ? null : new HandlerStorage(handler);
        }
    }

    /** Inserts are worked out by simulating and carried out when the outermost transaction commits. */
    private static final class HandlerStorage extends SnapshotParticipant<Integer> implements Storage<ItemVariant> {
        private final IItemHandler handler;
        private final java.util.List<ItemStack> pending = new java.util.ArrayList<>();

        HandlerStorage(IItemHandler handler) {
            this.handler = handler;
        }

        @Override
        public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            if (resource.isBlank() || maxAmount <= 0) {
                return 0;
            }
            ItemStack stack = resource.toStack((int) Math.min(Integer.MAX_VALUE, maxAmount));
            ItemStack rest = ItemHandlerHelper.insertItemStacked(handler, stack, true);
            int inserted = stack.getCount() - rest.getCount();
            if (inserted > 0) {
                updateSnapshots(transaction);
                pending.add(resource.toStack(inserted));
            }
            return inserted;
        }

        @Override
        protected Integer createSnapshot() {
            return pending.size();
        }

        @Override
        protected void readSnapshot(Integer snapshot) {
            while (pending.size() > snapshot) {
                pending.remove(pending.size() - 1);
            }
        }

        @Override
        protected void onFinalCommit() {
            for (ItemStack stack : pending) {
                ItemHandlerHelper.insertItemStacked(handler, stack, false);
            }
            pending.clear();
        }
    }
}
