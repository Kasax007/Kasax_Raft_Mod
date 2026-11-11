package net.kasax.raft.content;

import net.kasax.raft.platform.FabricItemGroup;
import net.kasax.raft.Raft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Creative tabs of Beach Craft. Modules add their items with {@link #add(Tab, ItemLike)} and may set the tab icon with
 * {@link #icon(Tab, Supplier)}. What a tab shows and in which order is set by {@link BcTabOrder}: first the items its
 * list names (an item named in another tab's list moves there), then any other item added to the tab, in the order
 * it was added, so new items always show up somewhere. Lang keys: {@code itemgroup.raft.<tab>}.
 *
 * <p>Fabric sorts modded tabs by their id, so the ids carry the position: {@code raft:bc_0_raft} ...
 * {@code raft:bc_9_story}. All ten fit on one page: the upper row holds the home tabs (raft, building, decoration,
 * cozy, nature), the lower row the survival and adventure tabs (kitchen, gear, voyage, creatures, story).
 */
public final class BcTabs {
    public enum Tab {
        RAFT("raft"), BUILDING("building"), DECOR("decor"), COZY("cozy"), NATURE("nature"),
        KITCHEN("kitchen"), GEAR("gear"), VOYAGE("voyage"), CREATURES("creatures"), STORY("story");

        public final String id;

        Tab(String id) {
            this.id = id;
        }

        /** Registry id of the tab, e.g. {@code raft:bc_3_cozy}. */
        public net.minecraft.resources.ResourceLocation key() {
            return Raft.id("bc_" + ordinal() + "_" + id);
        }

        /** Lang key of the title ({@code itemgroup.raft} for the raft tab, as before 3.4). */
        public String titleKey() {
            return this == RAFT ? "itemgroup.raft" : "itemgroup.raft." + id;
        }
    }

    private static final Map<Tab, List<ItemLike>> ENTRIES = new EnumMap<>(Tab.class);
    private static final Map<Tab, Supplier<ItemStack>> ICONS = new EnumMap<>(Tab.class);
    private static final Map<Tab, CreativeModeTab> TABS = new EnumMap<>(Tab.class);

    static {
        for (Tab tab : Tab.values()) {
            ENTRIES.put(tab, new ArrayList<>());
        }
    }

    private BcTabs() {
    }

    public static void add(Tab tab, ItemLike item) {
        ENTRIES.get(tab).add(item);
    }

    public static void icon(Tab tab, Supplier<ItemStack> icon) {
        ICONS.put(tab, icon);
    }

    public static CreativeModeTab tab(Tab tab) {
        return TABS.get(tab);
    }

    /** The items of a tab in display order (see the class comment). */
    public static List<Item> items(Tab tab) {
        Map<Item, Tab> listed = new HashMap<>();
        Map<Tab, List<Item>> lists = new EnumMap<>(Tab.class);
        for (Tab t : Tab.values()) {
            List<Item> list = new ArrayList<>();
            for (String id : BcTabOrder.order(t)) {
                Item item = BuiltInRegistries.ITEM.get(id.contains(":") ? net.minecraft.resources.ResourceLocation.parse(id) : Raft.id(id));
                if (item == Items.AIR || listed.containsKey(item)) continue;
                listed.put(item, t);
                list.add(item);
            }
            lists.put(t, list);
        }
        Set<Item> out = new LinkedHashSet<>(lists.get(tab));
        for (ItemLike like : ENTRIES.get(tab)) {
            Item item = like.asItem();
            if (item != Items.AIR && !listed.containsKey(item)) out.add(item);
        }
        return new ArrayList<>(out);
    }

    /** Called once after all modules are initialised. */
    public static void register() {
        for (Tab tab : Tab.values()) {
            CreativeModeTab built = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, tab.key(),
                    FabricItemGroup.builder()
                            .title(Component.translatable(tab.titleKey()))
                            .icon(() -> {
                                Supplier<ItemStack> icon = ICONS.get(tab);
                                if (icon != null) return icon.get();
                                List<Item> list = items(tab);
                                return list.isEmpty() ? new ItemStack(Items.PRISMARINE_SHARD) : new ItemStack(list.get(0));
                            })
                            .displayItems((params, output) -> {
                                for (Item item : items(tab)) output.accept(item);
                            })
                            .build());
            TABS.put(tab, built);
        }
    }
}
