package net.kasax.raft.content.guide.client;

import java.util.ArrayList;
import java.util.List;

/**
 * The table of contents of the Seafarer's Almanac: chapters, their sections, item icons and creature portraits.
 * Texts come from the lang file ({@code almanac.raft.<key>.title / .text}, written by tools/content/guide/texts.py).
 * Items and entities of other modules are looked up by id when the book opens; missing ones are left out.
 */
final class AlmanacBook {
    private AlmanacBook() {
    }

    /**
     * One part of a chapter. key: lang key below {@code almanac.raft.}; icons: item ids shown under the title (under
     * the plate for creatures); entity: the creature drawn on a full-page plate on the left, its text on the right
     * (or null); newSpread: starts on a fresh double page; warning: the spoiler page (red ink, wax seal).
     */
    record Section(String id, String key, List<String> icons, String entity, boolean newSpread, boolean warning) {
        static Section text(String key, String... icons) {
            return new Section("", key, List.of(icons), null, false, false);
        }

        static Section creature(String id, String... drops) {
            return new Section(id, "creatures." + id, List.of(drops), "raft:" + id, true, false);
        }
    }

    /** The people of the sea (npc module), in the order of the story. */
    private static final List<String> PEOPLE = List.of("keeper_fenn", "nalu", "old_tamo", "dr_voss", "teo", "kai",
            "dr_lund", "okafor");

    record Chapter(String id, String icon, List<Section> sections) {
        String titleKey() {
            return "almanac.raft." + id + ".title";
        }
    }

    static List<Chapter> chapters(boolean leviathanDefeated) {
        List<Chapter> list = new ArrayList<>();
        list.add(simple("welcome", "raft:seafarers_almanac", "raft:seafarers_almanac", "raft:expedition_journal",
                "minecraft:compass", "raft:raft_foundation"));
        list.add(simple("portal", "raft:driftwood_portal_frame", "raft:ancient_broken_ring", "raft:ancient_energy",
                "raft:ancient_crystal", "raft:energy_staff", "raft:driftwood_log", "raft:driftwood_portal_frame",
                "raft:raft_teleporter"));
        list.add(simple("survival", "raft:raft_foundation", "raft:plastic_hook", "raft:item_catcher", "raft:net",
                "raft:raft_foundation", "raft:water_purifier", "raft:simple_grill", "raft:crop_plot"));
        list.add(simple("building", "raft:tidestone_bricks", "raft:weathered_planks", "raft:teak_planks",
                "raft:coral_stone_bricks", "raft:tidestone_bricks", "raft:aqua_sea_glass", "raft:riveted_hull",
                "raft:paper_lantern_red"));
        list.add(simple("technology", "raft:wind_turbine", "raft:makeshift_solar_panel", "raft:wind_turbine",
                "raft:makeshift_battery", "raft:copper_cable", "raft:quarry", "raft:titanium_furnace",
                "raft:circuit_board"));
        list.add(new Chapter("sailing", "raft:expedition_raft", List.of(
                Section.text("sailing", "raft:palm_boat", "raft:driftwood_raft", "raft:paddle", "raft:expedition_raft",
                        "raft:raft_sail", "raft:raft_engine", "raft:raft_anchor"),
                // rafts built from blocks (module vessel); bookmark {almanac:"sailing/shipwright"}
                new Section("shipwright", "sailing.shipwright", List.of("raft:helm", "raft:raft_foundation", "raft:sail",
                        "raft:oar", "raft:raft_motor"), null, true, false))));
        list.add(simple("diving", "raft:diving_helmet", "raft:diving_helmet", "raft:oxygen_tank", "raft:wetsuit",
                "raft:flippers", "raft:air_compressor", "raft:harpoon_gun", "raft:sea_scooter"));
        list.add(simple("biomes", "raft:glow_kelp", "raft:glow_kelp", "raft:sea_anemone_glow", "raft:giant_clam",
                "raft:starfish_orange", "raft:pineapple", "raft:black_sand", "raft:hibiscus"));
        List<Section> places = new ArrayList<>();
        places.add(Section.text("places", "raft:treasure_chest", "raft:wooden_crate", "raft:gold_coin",
                "raft:jolly_roger", "raft:expedition_radio", "minecraft:spyglass", "raft:receiver"));
        // the people of the sea (npc module): an introduction and one portrait page each
        places.add(new Section("people", "people", List.of("raft:gold_coin", "raft:pearl", "minecraft:cookie"), null, true, false));
        for (String id : PEOPLE) {
            places.add(new Section(id, "people." + id, List.of(), "raft:" + id, true, false));
        }
        list.add(new Chapter("places", "raft:treasure_chest", places));
        List<Section> creatures = new ArrayList<>();
        creatures.add(Section.text("creatures", "raft:shark_tooth", "raft:crab_claw", "raft:eel_fillet",
                "raft:jelly_blob", "raft:glow_jelly", "raft:angler_lure", "minecraft:feather"));
        creatures.add(Section.creature("shark", "raft:shark_meat", "raft:shark_tooth", "raft:shark_jaw_trophy"));
        creatures.add(Section.creature("hammerhead_shark", "raft:shark_tooth", "raft:shark_meat"));
        creatures.add(Section.creature("whale"));
        creatures.add(Section.creature("manta_ray", "minecraft:leather"));
        creatures.add(Section.creature("jellyfish", "raft:jelly_blob", "raft:glow_jelly", "raft:jelly_lamp"));
        creatures.add(Section.creature("anglerfish", "raft:angler_lure", "minecraft:cod", "minecraft:sea_lantern"));
        creatures.add(Section.creature("moray_eel", "raft:eel_fillet", "raft:cooked_eel_fillet"));
        creatures.add(Section.creature("crab", "raft:crab_claw", "raft:cooked_crab_claw", "raft:seashell"));
        creatures.add(Section.creature("ember_crab", "raft:crab_claw", "raft:ember_shell"));
        creatures.add(Section.creature("seagull", "minecraft:feather", "raft:scarecrow"));
        creatures.add(Section.creature("penguin", "minecraft:feather", "minecraft:cod"));
        creatures.add(Section.creature("sea_otter", "raft:seashell", "raft:pearl"));
        creatures.add(Section.creature("drowned_corsair", "raft:gold_coin", "minecraft:bone", "raft:cutlass"));
        list.add(new Chapter("creatures", "raft:crab_claw", creatures));
        list.add(simple("story", "raft:expedition_journal", "raft:expedition_journal", "minecraft:map",
                "raft:message_in_a_bottle", "raft:research_terminal", "raft:expedition_radio"));
        List<Section> boss = new ArrayList<>();
        if (!leviathanDefeated) {
            boss.add(new Section("warning", "leviathan.warning", List.of(), null, false, true));
        }
        boss.add(new Section("", "leviathan", List.of("raft:leviathan_scale", "raft:heart_of_the_tide",
                "raft:tidebreaker", "raft:leviathan_trophy"), null, true, false));
        boss.add(new Section("tidelings", "leviathan.tidelings", List.of("minecraft:cod", "minecraft:prismarine_crystals"),
                "raft:tideling", true, false));
        list.add(new Chapter("leviathan", "raft:leviathan_scale", boss));
        list.add(simple("titanium", "raft:titanium_ingot", "raft:titanium_ore", "raft:raw_titanium",
                "raft:titanium_ingot", "raft:titanium_chestplate", "raft:metal_detector", "raft:receiver", "raft:cutlass"));
        // Beach Craft 3.3: cozy living, the island kitchen and new gear
        list.add(new Chapter("cozy", "raft:hurricane_lantern", List.of(
                Section.text("cozy", "raft:palm_bench", "raft:rattan_armchair", "raft:hurricane_lantern",
                        "raft:beach_towel_blue", "raft:potted_monstera", "raft:thatch_parasol", "raft:steel_drum"),
                new Section("furniture", "cozy.furniture", List.of("raft:teak_bench", "raft:bar_stool", "raft:sun_lounger",
                        "raft:teak_table", "raft:serving_plate", "raft:palm_shelf", "raft:wicker_basket"), null, true, false),
                new Section("lights", "cozy.lights", List.of("raft:shell_candle", "raft:sea_glass_lamp", "raft:driftwood_sconce",
                        "raft:conch_shell", "raft:surfboard_red", "raft:fish_trophy", "raft:shell_tiles"), null, true, false))));
        list.add(new Chapter("kitchen", "raft:island_curry", List.of(
                Section.text("kitchen", "raft:sweet_potato", "raft:chili_pepper", "raft:banana", "raft:rice",
                        "raft:sea_grapes", "raft:wild_banana", "raft:crop_plot"),
                new Section("pot", "kitchen.pot", List.of("raft:cooking_pot", "raft:island_curry", "raft:seafood_chowder",
                        "raft:sweet_potato_soup", "raft:fish_trap", "raft:fish_bait", "minecraft:cod"), null, true, false),
                new Section("dishes", "kitchen.dishes", List.of("raft:sushi_roll", "raft:banana_bread", "raft:fruit_salad",
                        "raft:chili_crab", "raft:tropical_smoothie", "raft:coconut_ice_cream", "raft:baked_sweet_potato"),
                        null, true, false))));
        list.add(new Chapter("gear", "raft:boomerang", List.of(
                Section.text("gear", "raft:shark_tooth_spear", "raft:boomerang", "raft:machete"),
                new Section("armour", "gear.armour", List.of("raft:shell_helmet", "raft:shell_chestplate", "raft:shell_leggings",
                        "raft:shell_boots", "raft:explorer_helmet", "raft:explorer_chestplate", "raft:explorer_boots"),
                        null, true, false),
                new Section("tools", "gear.tools", List.of("raft:wreck_compass", "raft:captains_spyglass"), null, true, false),
                new Section("fishing", "gear.fishing", List.of("raft:driftwood_fishing_rod", "raft:fish_bait",
                        "raft:golden_lure", "minecraft:fishing_rod"), null, true, false))));
        list.add(simple("tips", "raft:coconut", "raft:coconut", "raft:hammock", "raft:scarecrow", "raft:bubble_vent",
                "raft:seashell", "raft:supply_barrel"));
        return list;
    }

    private static Chapter simple(String id, String icon, String... icons) {
        return new Chapter(id, icon, List.of(Section.text(id, icons)));
    }
}
