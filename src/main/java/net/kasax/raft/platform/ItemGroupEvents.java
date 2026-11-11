package net.kasax.raft.platform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/** Fabric's ItemGroupEvents.modifyEntriesEvent on NeoForge's {@link BuildCreativeModeTabContentsEvent}. */
public final class ItemGroupEvents {
    private static final Map<ResourceKey<CreativeModeTab>, Event<ModifyEntries>> EVENTS = new HashMap<>();

    private ItemGroupEvents() {
    }

    public static synchronized Event<ModifyEntries> modifyEntriesEvent(ResourceKey<CreativeModeTab> key) {
        return EVENTS.computeIfAbsent(key, k -> new Event<>(() -> {
        }));
    }

    static void onBuildContents(BuildCreativeModeTabContentsEvent event) {
        Event<ModifyEntries> modify = EVENTS.get(event.getTabKey());
        if (modify != null) {
            Entries entries = new Entries(event);
            for (ModifyEntries l : modify.listeners()) l.modifyEntries(entries);
        }
    }

    @FunctionalInterface
    public interface ModifyEntries {
        void modifyEntries(Entries entries);
    }

    /** The part of Fabric's FabricItemGroupEntries Beach Craft uses. */
    public static final class Entries {
        private final BuildCreativeModeTabContentsEvent event;

        Entries(BuildCreativeModeTabContentsEvent event) {
            this.event = event;
        }

        public void accept(ItemStack stack) {
            event.accept(stack);
        }

        public void accept(ItemLike item) {
            event.accept(item);
        }

        public void addAfter(ItemLike after, ItemStack... newStacks) {
            addAfter(new ItemStack(after), newStacks);
        }

        public void addAfter(ItemStack after, ItemStack... newStacks) {
            List<ItemStack> list = new ArrayList<>(List.of(newStacks));
            ItemStack anchor = after;
            for (ItemStack stack : list) {
                event.insertAfter(anchor, stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                anchor = stack;
            }
        }

        public void addAfter(ItemLike after, ItemLike... newItems) {
            ItemStack[] stacks = new ItemStack[newItems.length];
            for (int i = 0; i < newItems.length; i++) stacks[i] = new ItemStack(newItems[i]);
            addAfter(after, stacks);
        }
    }
}
