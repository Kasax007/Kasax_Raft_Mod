package net.kasax.raft.platform.energy;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.kasax.raft.platform.transfer.Transaction;
import net.kasax.raft.platform.transfer.TransactionContext;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import org.jspecify.annotations.Nullable;

/**
 * Team Reborn Energy's EnergyStorage (FE in longs, with transactions), the API the Fabric build of Beach Craft talks
 * to other tech mods with. On NeoForge the same storages are offered to other mods as NeoForge energy
 * ({@link Capabilities.Energy#BLOCK}), and the lookup finds other mods' NeoForge energy blocks too.
 */
public interface EnergyStorage {
    Lookup SIDED = new Lookup();
    /** The item component of stored energy ({@code team_reborn_energy:energy}), see {@link SimpleEnergyItem}. */
    net.minecraft.core.component.DataComponentType<Long> ENERGY_COMPONENT = SimpleEnergyItem.Holder.ENERGY;

    default boolean supportsInsertion() {
        return true;
    }

    long insert(long maxAmount, TransactionContext transaction);

    default boolean supportsExtraction() {
        return true;
    }

    long extract(long maxAmount, TransactionContext transaction);

    long getAmount();

    long getCapacity();

    /** Fabric's BlockApiLookup for energy, with the block entity providers handed to NeoForge as capabilities. */
    final class Lookup {
        private final List<Registration<?>> registrations = new ArrayList<>();

        Lookup() {
        }

        @SafeVarargs
        public final <T extends BlockEntity> void registerForBlockEntity(BiFunction<? super T, @Nullable Direction, @Nullable EnergyStorage> provider,
                                                                          BlockEntityType<T>... types) {
            for (BlockEntityType<T> type : types) {
                registrations.add(new Registration<>(type, provider));
            }
        }

        public void registerCapabilities(RegisterCapabilitiesEvent event) {
            for (Registration<?> r : registrations) {
                r.register(event);
            }
        }

        @Nullable
        public EnergyStorage find(Level level, BlockPos pos, @Nullable Direction side) {
            EnergyHandler cap = level.getCapability(Capabilities.Energy.BLOCK, pos, side);
            if (cap == null) {
                return null;
            }
            return cap instanceof NeoEnergy neo ? neo.storage : new ForeignEnergy(cap);
        }

        private record Registration<T extends BlockEntity>(BlockEntityType<T> type, BiFunction<? super T, Direction, EnergyStorage> provider) {
            void register(RegisterCapabilitiesEvent event) {
                event.registerBlockEntity(Capabilities.Energy.BLOCK, type, (be, side) -> {
                    EnergyStorage storage = provider.apply(be, side);
                    return storage == null ? null : new NeoEnergy(storage);
                });
            }
        }
    }

    /** One of Beach Craft's storages as NeoForge energy, for other mods (in their transaction). */
    final class NeoEnergy implements EnergyHandler {
        final EnergyStorage storage;

        NeoEnergy(EnergyStorage storage) {
            this.storage = storage;
        }

        @Override
        public long getAmountAsLong() {
            return storage.getAmount();
        }

        @Override
        public long getCapacityAsLong() {
            return storage.getCapacity();
        }

        @Override
        public int insert(int amount, net.neoforged.neoforge.transfer.transaction.TransactionContext transaction) {
            if (amount <= 0 || !storage.supportsInsertion()) return 0;
            return (int) Math.min(Integer.MAX_VALUE, storage.insert(amount, Transaction.of(transaction)));
        }

        @Override
        public int extract(int amount, net.neoforged.neoforge.transfer.transaction.TransactionContext transaction) {
            if (amount <= 0 || !storage.supportsExtraction()) return 0;
            return (int) Math.min(Integer.MAX_VALUE, storage.extract(amount, Transaction.of(transaction)));
        }
    }

    /** Another mod's NeoForge energy inside Beach Craft's transactions (both are NeoForge transactions). */
    final class ForeignEnergy implements EnergyStorage {
        private final EnergyHandler target;

        public ForeignEnergy(EnergyHandler target) {
            this.target = target;
        }

        @Override
        public long insert(long maxAmount, TransactionContext transaction) {
            return maxAmount <= 0 ? 0 : target.insert((int) Math.min(Integer.MAX_VALUE, maxAmount), transaction.neo());
        }

        @Override
        public long extract(long maxAmount, TransactionContext transaction) {
            return maxAmount <= 0 ? 0 : target.extract((int) Math.min(Integer.MAX_VALUE, maxAmount), transaction.neo());
        }

        @Override
        public long getAmount() {
            return target.getAmountAsLong();
        }

        @Override
        public long getCapacity() {
            return target.getCapacityAsLong();
        }
    }
}
