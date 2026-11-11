package net.kasax.raft.platform.energy;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Team Reborn Energy's SimpleEnergyItem. The charge is kept in the same item component as on Fabric
 * ({@code team_reborn_energy:energy}), so items keep their charge in worlds moved between the two loaders; other mods
 * see such items as NeoForge energy items.
 */
public interface SimpleEnergyItem {
    ResourceLocation COMPONENT_ID = ResourceLocation.fromNamespaceAndPath("team_reborn_energy", "energy");

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
                event.registerItem(Capabilities.EnergyStorage.ITEM, (stack, context) -> new ItemEnergy(stack, energyItem), item);
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

    /** An energy item as NeoForge energy, with its input and output limits. */
    record ItemEnergy(ItemStack stack, SimpleEnergyItem item) implements IEnergyStorage {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            long stored = item.getStoredEnergy(stack);
            long accepted = Math.min(Math.min(toReceive, item.getEnergyMaxInput(stack)), item.getEnergyCapacity(stack) - stored);
            if (accepted <= 0 || stack.getCount() != 1) return 0;
            if (!simulate) item.setStoredEnergy(stack, stored + accepted);
            return (int) accepted;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            long stored = item.getStoredEnergy(stack);
            long taken = Math.min(Math.min(toExtract, item.getEnergyMaxOutput(stack)), stored);
            if (taken <= 0 || stack.getCount() != 1) return 0;
            if (!simulate) item.setStoredEnergy(stack, stored - taken);
            return (int) taken;
        }

        @Override
        public int getEnergyStored() {
            return (int) Math.min(Integer.MAX_VALUE, item.getStoredEnergy(stack));
        }

        @Override
        public int getMaxEnergyStored() {
            return (int) Math.min(Integer.MAX_VALUE, item.getEnergyCapacity(stack));
        }

        @Override
        public boolean canExtract() {
            return item.getEnergyMaxOutput(stack) > 0;
        }

        @Override
        public boolean canReceive() {
            return item.getEnergyMaxInput(stack) > 0;
        }
    }
}
