package net.kasax.raft.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.kasax.raft.Raft;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 3.4.2: machines keep their energy when they are broken. The drop carries the energy in the {@code raft:stored_energy}
 * item component (copied by the block's loot table, {@code copy_components} from the block entity), the tooltip shows
 * it, and the machine gets it back when it is placed again. An empty machine drops a plain item, so it still stacks with
 * new ones; items with different amounts never stack (their components differ), and two with the same amount stack
 * without changing what each one holds.
 *
 * <p>Every block entity with an energy storage that keeps it (battery, solar panel, quarry, chunk destroyer, wind
 * turbine, air compressor) uses {@link #fromItem}, {@link #toItem} and {@link #removeFromTag} and registers its block
 * with {@link #register}. Cables do not: they only pass energy on (a buffer of a few ticks), and cables holding
 * different amounts would no longer stack.
 *
 * <p>1.21.1 port: the block entity hooks of this version take a component input (protected in BlockEntity, so the
 * machines read the component themselves) and a {@link CompoundTag} (the newer game passes a component getter and a
 * value output).
 */
public final class StoredEnergy {
    /** FE in a machine's item. Only set when the machine held energy. */
    public static final DataComponentType<Long> COMPONENT = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
            Raft.id("stored_energy"), DataComponentType.<Long>builder()
                    .persistent(Codec.LONG.validate(v -> v >= 0 ? DataResult.success(v)
                            : DataResult.error(() -> "Negative energy: " + v)))
                    .networkSynchronized(ByteBufCodecs.VAR_LONG)
                    .build());

    /** Key of the energy in the machines' block entity data (all of them save it as "Energy"). */
    public static final String TAG = "Energy";

    private static final Map<Block, Long> CAPACITY = new HashMap<>();

    private StoredEnergy() {
    }

    /** Loads the class (registers the component). Called first thing in {@link Raft#onInitialize()}. */
    public static void init() {
    }

    /** Remembers the capacity of a machine that keeps its energy, for the tooltip of its item. */
    public static void register(Block block, long capacity) {
        CAPACITY.put(block, capacity);
    }

    public static long capacity(Block block) {
        return CAPACITY.getOrDefault(block, 0L);
    }

    /**
     * The energy a placed item brings into a machine that holds at most {@code capacity}: the item's component, read by
     * the block entity ({@code components.get(StoredEnergy.COMPONENT)}, so the component is used up and not kept as an
     * extra component of the block entity), or 0 without it.
     */
    public static long fromItem(@Nullable Long amount, long capacity) {
        return amount == null ? 0L : Math.max(0L, Math.min(capacity, amount));
    }

    /** Puts the machine's energy into the item it drops (nothing when it is empty). */
    public static void toItem(DataComponentMap.Builder components, long amount) {
        if (amount > 0) {
            components.set(COMPONENT, amount);
        }
    }

    /**
     * Creative pick-block with data copies the block entity's data and its components into the item: the energy goes
     * into the component only, so it is not in the item twice.
     */
    public static void removeFromTag(CompoundTag tag) {
        tag.remove(TAG);
    }

    /** "Energy: 120,000 / 500,000 FE" under the item's name, when the item holds energy. */
    public static void appendTooltip(ItemStack stack, Block block, Consumer<Component> builder) {
        Long amount = stack.get(COMPONENT);
        if (amount == null || amount <= 0) {
            return;
        }
        long capacity = capacity(block);
        builder.accept((capacity > 0
                ? Component.translatable("tooltip.raft.stored_energy", format(amount), format(capacity))
                : Component.translatable("tooltip.raft.stored_energy.plain", format(amount))).withStyle(ChatFormatting.YELLOW));
    }

    public static String format(long amount) {
        return String.format(Locale.ROOT, "%,d", amount);
    }
}
