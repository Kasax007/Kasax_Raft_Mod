package net.kasax.raft.platform;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/** Fabric's CreativeModeTabEvents.modifyOutputEvent on NeoForge's {@link BuildCreativeModeTabContentsEvent}. */
public final class CreativeModeTabEvents {
    private static final Map<ResourceKey<CreativeModeTab>, Event<ModifyOutput>> EVENTS = new HashMap<>();

    private CreativeModeTabEvents() {
    }

    public static synchronized Event<ModifyOutput> modifyOutputEvent(ResourceKey<CreativeModeTab> key) {
        return EVENTS.computeIfAbsent(key, k -> new Event<>(() -> {
        }));
    }

    static void onBuildContents(BuildCreativeModeTabContentsEvent event) {
        Event<ModifyOutput> modify = EVENTS.get(event.getTabKey());
        if (modify != null) {
            Output output = new Output(event);
            for (ModifyOutput l : modify.listeners()) l.modifyOutput(output);
        }
    }

    @FunctionalInterface
    public interface ModifyOutput {
        void modifyOutput(Output output);
    }

    /** The part of Fabric's FabricCreativeModeTabOutput Beach Craft uses. */
    public static final class Output {
        private final BuildCreativeModeTabContentsEvent event;

        Output(BuildCreativeModeTabContentsEvent event) {
            this.event = event;
        }

        public void accept(ItemStack stack) {
            event.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }

        public void accept(ItemLike item) {
            accept(new ItemStack(item));
        }

        public void insertAfter(ItemLike after, ItemStack... newStacks) {
            insertAfter(new ItemStack(after), newStacks);
        }

        public void insertAfter(ItemStack after, ItemStack... newStacks) {
            ItemStack anchor = after;
            for (ItemStack stack : newStacks) {
                event.insertAfter(anchor, stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                anchor = stack;
            }
        }

        public void insertAfter(ItemLike after, ItemLike... newItems) {
            ItemStack[] stacks = new ItemStack[newItems.length];
            for (int i = 0; i < newItems.length; i++) stacks[i] = new ItemStack(newItems[i]);
            insertAfter(after, stacks);
        }
    }
}
