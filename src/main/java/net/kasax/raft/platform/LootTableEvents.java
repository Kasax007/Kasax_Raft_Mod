package net.kasax.raft.platform;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.LootTableLoadEvent;

/**
 * Fabric's LootTableEvents.MODIFY on NeoForge's {@link LootTableLoadEvent}. Beach Craft only adds pools; NeoForge does
 * not say which pack a table came from, so every table counts as built in (Fabric: replaced by a data pack or not).
 */
public final class LootTableEvents {
    public static final Event<Modify> MODIFY = new Event<>(() -> NeoForge.EVENT_BUS.addListener(LootTableLoadEvent.class, e -> {
        LootTable table = e.getTable();
        PoolSink sink = table::addPool;
        for (Modify l : LootTableEvents.MODIFY.listeners()) l.modifyLootTable(e.getKey(), sink, LootTableSource.MOD, e.getRegistries());
    }));

    private LootTableEvents() {
    }

    /** What Beach Craft does with Fabric's table builder: add pools. */
    @FunctionalInterface
    public interface PoolSink {
        void pool(LootPool pool);
    }

    public enum LootTableSource {
        VANILLA(true), MOD(true), DATA_PACK(false), REPLACED(false);

        private final boolean builtin;

        LootTableSource(boolean builtin) {
            this.builtin = builtin;
        }

        public boolean isBuiltin() {
            return builtin;
        }
    }

    @FunctionalInterface
    public interface Modify {
        void modifyLootTable(ResourceKey<LootTable> key, PoolSink tableBuilder, LootTableSource source, HolderLookup.Provider registries);
    }
}
