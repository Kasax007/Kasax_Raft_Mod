package net.kasax.raft.content;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
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
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Creative tabs of Beach Craft 3. Modules add their items with {@link #add(Tab, ItemLike)} (in the order they should
 * show up) and may set the tab icon with {@link #icon(Tab, Supplier)}. Lang keys: {@code itemgroup.raft.<tab>}.
 */
public final class BcTabs {
    public enum Tab {
        BUILDING("building"), DECOR("decor"), NATURE("nature"), VOYAGE("voyage"), CREATURES("creatures"), STORY("story");

        public final String id;

        Tab(String id) {
            this.id = id;
        }
    }

    private static final Map<Tab, List<ItemLike>> ENTRIES = new EnumMap<>(Tab.class);
    private static final Map<Tab, Supplier<ItemStack>> ICONS = new EnumMap<>(Tab.class);

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

    /** Called once after all modules are initialised. */
    public static void register() {
        for (Tab tab : Tab.values()) {
            Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Raft.id("bc_" + tab.id),
                    FabricItemGroup.builder()
                            .title(Component.translatable("itemgroup.raft." + tab.id))
                            .icon(() -> {
                                Supplier<ItemStack> icon = ICONS.get(tab);
                                if (icon != null) return icon.get();
                                List<ItemLike> list = ENTRIES.get(tab);
                                return list.isEmpty() ? new ItemStack(Items.PRISMARINE_SHARD) : new ItemStack(list.get(0));
                            })
                            .displayItems((params, output) -> {
                                for (ItemLike item : ENTRIES.get(tab)) {
                                    if (item.asItem() != Items.AIR) output.accept(item);
                                }
                            })
                            .build());
        }
    }
}
