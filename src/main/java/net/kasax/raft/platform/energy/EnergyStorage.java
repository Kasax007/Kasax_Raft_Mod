package net.kasax.raft.platform.energy;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.kasax.raft.platform.transfer.SnapshotParticipant;
import net.kasax.raft.platform.transfer.Transaction;
import net.kasax.raft.platform.transfer.TransactionContext;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/**
 * Team Reborn Energy's EnergyStorage (FE in longs, with transactions), the API the Fabric build of Beach Craft talks
 * to other tech mods with. On NeoForge the same storages are offered to other mods as NeoForge energy
 * ({@link Capabilities.EnergyStorage#BLOCK}), and the lookup finds other mods' NeoForge energy blocks too.
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
            IEnergyStorage cap = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, side);
            if (cap == null) {
                return null;
            }
            return cap instanceof NeoEnergy neo ? neo.storage : new ForeignEnergy(cap);
        }

        private record Registration<T extends BlockEntity>(BlockEntityType<T> type, BiFunction<? super T, Direction, EnergyStorage> provider) {
            void register(RegisterCapabilitiesEvent event) {
                event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, type, (be, side) -> {
                    EnergyStorage storage = provider.apply(be, side);
                    return storage == null ? null : new NeoEnergy(storage);
                });
            }
        }
    }

    /** One of Beach Craft's storages as NeoForge energy, for other mods. */
    final class NeoEnergy implements IEnergyStorage {
        final EnergyStorage storage;

        NeoEnergy(EnergyStorage storage) {
            this.storage = storage;
        }

        private static int clamp(long value) {
            return (int) Math.max(0, Math.min(Integer.MAX_VALUE, value));
        }

        private long run(boolean simulate, java.util.function.ToLongFunction<TransactionContext> action) {
            TransactionContext outer = Transaction.getCurrentUnsafe();
            try (Transaction tx = outer == null ? Transaction.openOuter() : outer.openNested()) {
                long amount = action.applyAsLong(tx);
                if (!simulate) {
                    tx.commit();
                }
                return amount;
            }
        }

        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            if (toReceive <= 0 || !storage.supportsInsertion()) return 0;
            return clamp(run(simulate, tx -> storage.insert(toReceive, tx)));
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            if (toExtract <= 0 || !storage.supportsExtraction()) return 0;
            return clamp(run(simulate, tx -> storage.extract(toExtract, tx)));
        }

        @Override
        public int getEnergyStored() {
            return clamp(storage.getAmount());
        }

        @Override
        public int getMaxEnergyStored() {
            return clamp(storage.getCapacity());
        }

        @Override
        public boolean canExtract() {
            return storage.supportsExtraction();
        }

        @Override
        public boolean canReceive() {
            return storage.supportsInsertion();
        }
    }

    /**
     * Another mod's NeoForge energy inside Beach Craft's transactions: amounts are simulated, and what was moved is
     * carried out when the outermost transaction commits.
     */
    final class ForeignEnergy extends SnapshotParticipant<Long> implements EnergyStorage {
        private final IEnergyStorage target;
        private long pending;

        public ForeignEnergy(IEnergyStorage target) {
            this.target = target;
        }

        @Override
        public boolean supportsInsertion() {
            return target.canReceive();
        }

        @Override
        public boolean supportsExtraction() {
            return target.canExtract();
        }

        @Override
        public long insert(long maxAmount, TransactionContext transaction) {
            if (maxAmount <= 0 || pending < 0) return 0;
            long room = target.receiveEnergy(Integer.MAX_VALUE, true) - pending;
            long accepted = Math.min(Math.min(maxAmount, Integer.MAX_VALUE), room);
            if (accepted <= 0) return 0;
            updateSnapshots(transaction);
            pending += accepted;
            return accepted;
        }

        @Override
        public long extract(long maxAmount, TransactionContext transaction) {
            if (maxAmount <= 0 || pending > 0) return 0;
            long available = target.extractEnergy(Integer.MAX_VALUE, true) + pending;
            long taken = Math.min(Math.min(maxAmount, Integer.MAX_VALUE), available);
            if (taken <= 0) return 0;
            updateSnapshots(transaction);
            pending -= taken;
            return taken;
        }

        @Override
        public long getAmount() {
            return target.getEnergyStored() + pending;
        }

        @Override
        public long getCapacity() {
            return target.getMaxEnergyStored();
        }

        @Override
        protected Long createSnapshot() {
            return pending;
        }

        @Override
        protected void readSnapshot(Long snapshot) {
            pending = snapshot;
        }

        @Override
        protected void onFinalCommit() {
            long amount = pending;
            pending = 0;
            if (amount > 0) {
                target.receiveEnergy((int) Math.min(Integer.MAX_VALUE, amount), false);
            } else if (amount < 0) {
                target.extractEnergy((int) Math.min(Integer.MAX_VALUE, -amount), false);
            }
        }
    }
}
