package net.kasax.raft.content.world;

import net.kasax.raft.platform.LootTableEvents;
import net.kasax.raft.Raft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Richer structure chests (3.2): with less flotsam at sea, exploring is the main way to good loot. Every Beach Craft
 * structure chest gets a themed bonus pool (1 to 3 more stacks: Beach Craft materials, raft parts, food and seeds)
 * and a rare highlight (an Expedition Raft upgrade, gear or a weapon) on top of its own table. Story chests stay as
 * they are. Items are looked up by id, so a module that is missing just leaves its items out.
 */
public final class StructureLoot {
    private record Entry(String id, int weight, int min, int max) {
    }

    private record Theme(List<Entry> bonus, String highlight, float chance) {
    }

    private static Entry e(String id, int weight, int min, int max) {
        return new Entry(id, weight, min, max);
    }

    private static final Map<String, Theme> THEMES = Map.ofEntries(
            Map.entry("abandoned_raft", new Theme(List.of(e("raft:raft_foundation", 4, 2, 4), e("raft:rope", 3, 1, 3),
                    e("raft:sail_canvas", 2, 1, 2), e("raft:paddle", 2, 1, 1), e("raft:net", 2, 1, 1)), "raft:raft_sail", 0.10F)),
            Map.entry("castaway_camp", new Theme(List.of(e("raft:coconut", 4, 1, 3), e("raft:fresh_water", 4, 1, 2),
                    e("raft:palm_sapling", 3, 1, 2), e("raft:pineapple", 2, 1, 2), e("raft:rope", 3, 1, 3)), "raft:harpoon", 0.12F)),
            Map.entry("beach_shack", new Theme(List.of(e("raft:pina_colada", 3, 1, 2), e("raft:coconut", 4, 1, 3),
                    e("raft:pineapple", 3, 1, 2), e("raft:palm_sapling", 2, 1, 2), e("raft:seashell", 3, 1, 3)), "raft:flippers", 0.12F)),
            Map.entry("lighthouse_storage", new Theme(List.of(e("minecraft:coal", 3, 2, 6), e("raft:rope", 3, 1, 3),
                    e("minecraft:spyglass", 1, 1, 1), e("raft:fresh_water", 3, 1, 2), e("raft:gold_coin", 2, 1, 3)), "raft:raft_lantern", 0.15F)),
            Map.entry("iceberg_outpost", new Theme(List.of(e("minecraft:cooked_cod", 4, 2, 4), e("raft:fresh_water", 3, 1, 2),
                    e("minecraft:iron_ingot", 3, 1, 3), e("raft:titanium_nugget", 2, 2, 5)), "raft:wetsuit", 0.12F)),
            Map.entry("rusted_rig", new Theme(List.of(e("raft:circuit_board", 3, 1, 2), e("raft:titanium_nugget", 3, 2, 6),
                    e("raft:scrap_metal", 4, 2, 5), e("raft:raw_titanium", 2, 1, 3)), "raft:raft_engine", 0.12F)),
            Map.entry("helix_storage", new Theme(List.of(e("raft:circuit_board", 3, 1, 2), e("raft:oxygen_tank", 2, 1, 1),
                    e("raft:titanium_ingot", 2, 1, 2), e("minecraft:glow_ink_sac", 3, 1, 3)), "raft:diving_helmet", 0.12F)),
            Map.entry("wreck_cargo", new Theme(List.of(e("raft:plastic", 4, 2, 5), e("raft:scrap_metal", 4, 2, 5),
                    e("raft:rope", 3, 1, 3), e("raft:raft_foundation", 2, 2, 4), e("raft:titanium_nugget", 2, 1, 4)), "raft:raft_crate", 0.12F)),
            Map.entry("wreck_fishing", new Theme(List.of(e("raft:net", 2, 1, 1), e("raft:plastic_hook", 2, 1, 1),
                    e("minecraft:cod", 4, 2, 4), e("minecraft:salmon", 3, 1, 3), e("raft:rope", 3, 1, 3)), "raft:angler_lure", 0.10F)),
            Map.entry("wreck_galleon", new Theme(List.of(e("raft:gold_coin", 4, 2, 6), e("minecraft:gunpowder", 3, 2, 4),
                    e("raft:rope", 3, 1, 3), e("raft:cannonball", 2, 2, 4)), "raft:cutlass", 0.12F)),
            Map.entry("sunken_plane", new Theme(List.of(e("raft:circuit_board", 3, 1, 2), e("minecraft:compass", 1, 1, 1),
                    e("raft:plastic", 3, 2, 4), e("minecraft:iron_ingot", 3, 1, 3)), "raft:sea_scooter", 0.08F)),
            Map.entry("sunken_ruins", new Theme(List.of(e("minecraft:prismarine_shard", 3, 1, 4), e("raft:gold_coin", 3, 1, 3),
                    e("raft:sea_glass_shard", 3, 1, 3), e("raft:pearl", 1, 1, 1)), "raft:ancient_energy", 0.10F)),
            Map.entry("saltmere_house", new Theme(List.of(e("minecraft:cod", 3, 2, 4), e("raft:coconut", 3, 1, 2),
                    e("raft:rope", 3, 1, 3), e("raft:gold_coin", 2, 1, 3)), "raft:harpoon", 0.08F)),
            Map.entry("saltmere_market", new Theme(List.of(e("raft:gold_coin", 4, 2, 5), e("raft:pineapple", 3, 1, 2),
                    e("raft:palm_sapling", 2, 1, 2), e("raft:seashell", 3, 1, 3)), "raft:net", 0.15F)),
            Map.entry("port_solace_apartment", new Theme(List.of(e("raft:gold_coin", 3, 1, 4), e("raft:pina_colada", 2, 1, 2),
                    e("minecraft:book", 2, 1, 2), e("raft:plastic", 3, 1, 3)), "raft:receiver", 0.08F)),
            Map.entry("port_solace_office", new Theme(List.of(e("raft:circuit_board", 3, 1, 2), e("raft:gold_coin", 3, 1, 4),
                    e("minecraft:paper", 3, 2, 5)), "raft:metal_detector", 0.10F)),
            Map.entry("port_solace_shop", new Theme(List.of(e("raft:gold_coin", 4, 2, 6), e("raft:coconut", 3, 1, 3),
                    e("raft:pineapple", 3, 1, 2)), "raft:sea_scooter", 0.06F)),
            Map.entry("ashen_storage", new Theme(List.of(e("raft:titanium_ingot", 3, 1, 3), e("raft:ember_shell", 2, 1, 2),
                    e("minecraft:obsidian", 2, 1, 3)), "raft:titanium_chestplate", 0.08F)),
            Map.entry("coral_temple_offering", new Theme(List.of(e("raft:pearl", 3, 1, 2), e("raft:gold_coin", 3, 2, 5),
                    e("raft:sea_glass_shard", 3, 1, 3)), "raft:titanium_helmet", 0.06F)),
            Map.entry("pirate_treasure", new Theme(List.of(e("raft:gold_coin", 5, 4, 12), e("raft:cannonball", 2, 2, 4),
                    e("minecraft:gunpowder", 2, 2, 4)), "raft:cutlass", 0.15F))
    );

    private StructureLoot() {
    }

    private static Optional<Item> item(String id) {
        return BuiltInRegistries.ITEM.getOptional(Identifier.parse(id));
    }

    public static void register() {
        LootTableEvents.MODIFY.register((key, builder, source, registries) -> {
            Identifier id = key.identifier();
            if (!source.isBuiltin() || !Raft.MOD_ID.equals(id.getNamespace()) || !id.getPath().startsWith("chests/")) {
                return;
            }
            Theme theme = THEMES.get(id.getPath().substring("chests/".length()));
            if (theme == null) {
                return;
            }
            LootPool.Builder bonus = LootPool.lootPool().setRolls(ContextIntProviders.between(1, 3));
            for (Entry en : theme.bonus()) {
                item(en.id()).ifPresent(it -> bonus.add(LootItem.lootTableItem(it).setWeight(en.weight())
                        .apply(SetItemCountFunction.setCount(ContextIntProviders.between(en.min(), en.max())))));
            }
            builder.pool(bonus.build());
            item(theme.highlight()).ifPresent(it -> builder.pool(LootPool.lootPool().setRolls(ContextIntProviders.exactly(1))
                    .add(LootItem.lootTableItem(it).setWeight(Math.max(1, Math.round(theme.chance() * 100))))
                    .add(EmptyLootItem.emptyItem().setWeight(Math.max(1, 100 - Math.round(theme.chance() * 100))))
                    .build()));
        });
    }
}
