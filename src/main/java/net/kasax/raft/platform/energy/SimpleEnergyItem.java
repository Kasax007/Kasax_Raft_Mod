package net.kasax.raft.platform.energy;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * Team Reborn Energy's SimpleEnergyItem. The charge is kept in the same item component as on Fabric
 * ({@code team_reborn_energy:energy}), so items keep their charge in worlds moved between the two loaders; other mods
 * see such items as NeoForge energy items.
 */
public interface SimpleEnergyItem {
    Identifier COMPONENT_ID = Identifier.fromNamespaceAndPath("team_reborn_energy", "energy");

    long getEnergyCapacity(ItemStack stack);

    long getEnergyMaxInput(ItemStack stack);

    long getEnergyMaxOutput(ItemStack stack);

    default long getStoredEnergy(ItemStack stack) {
        return getStoredEnergyUnchecked(stack);
    }

    default void setStoredEnergy(ItemStack stack, long newAmount) {
        setStoredEnergyUnchecked(stack, newAmount);
    }

    default boolean tryUseEnergy(ItemStack stack, long amount) {
        long newAmount = getStoredEnergy(stack) - amount;
        if (newAmount < 0) {
            return false;
        }
        setStoredEnergy(stack, newAmount);
        return true;
    }

    static long getStoredEnergyUnchecked(ItemStack stack) {
        return stack.getOrDefault(Holder.ENERGY, 0L);
    }

    static void setStoredEnergyUnchecked(ItemStack stack, long newAmount) {
        if (newAmount == 0) {
            stack.remove(Holder.ENERGY);
        } else {
            stack.set(Holder.ENERGY, newAmount);
        }
    }

    /** Called in the first registry event, before the mod's own registrations. */
    static void registerComponent() {
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, COMPONENT_ID, Holder.ENERGY);
    }

    static void registerCapabilities(RegisterCapabilitiesEvent event) {
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof SimpleEnergyItem energyItem) {
                event.registerItem(Capabilities.Energy.ITEM, (stack, access) -> new ItemEnergy(access, energyItem), item);
            }
        }
    }

    final class Holder {
        static final DataComponentType<Long> ENERGY = DataComponentType.<Long>builder()
                .persistent(Codec.LONG.validate(v -> v >= 0 ? com.mojang.serialization.DataResult.success(v)
                        : com.mojang.serialization.DataResult.error(() -> "Energy must be non-negative: " + v)))
                .networkSynchronized(ByteBufCodecs.VAR_LONG).build();

        private Holder() {
        }
    }

    /** An energy item as NeoForge energy (through its item access), with its input and output limits. */
    record ItemEnergy(ItemAccess access, SimpleEnergyItem item) implements EnergyHandler {
        private ItemStack stack() {
            return access.getResource().toStack(1);
        }

        private long stored(ItemResource resource) {
            return resource.getOrDefault(Holder.ENERGY, 0L);
        }

        @Override
        public long getAmountAsLong() {
            return access.getAmount() == 1 ? stored(access.getResource()) : 0;
        }

        @Override
        public long getCapacityAsLong() {
            return access.getAmount() == 1 ? item.getEnergyCapacity(stack()) : 0;
        }

        private int change(long delta, net.neoforged.neoforge.transfer.transaction.TransactionContext transaction) {
            ItemResource resource = access.getResource();
            long next = stored(resource) + delta;
            ItemResource changed = next == 0 ? resource.without(Holder.ENERGY) : resource.with(Holder.ENERGY, next);
            return access.exchange(changed, 1, transaction) == 1 ? (int) Math.abs(delta) : 0;
        }

        @Override
        public int insert(int amount, net.neoforged.neoforge.transfer.transaction.TransactionContext transaction) {
            if (access.getAmount() != 1 || amount <= 0) return 0;
            ItemStack stack = stack();
            long accepted = Math.min(Math.min(amount, item.getEnergyMaxInput(stack)), item.getEnergyCapacity(stack) - stored(access.getResource()));
            return accepted <= 0 ? 0 : change(accepted, transaction);
        }

        @Override
        public int extract(int amount, net.neoforged.neoforge.transfer.transaction.TransactionContext transaction) {
            if (access.getAmount() != 1 || amount <= 0) return 0;
            ItemStack stack = stack();
            long taken = Math.min(Math.min(amount, item.getEnergyMaxOutput(stack)), stored(access.getResource()));
            return taken <= 0 ? 0 : change(-taken, transaction);
        }
    }
}
