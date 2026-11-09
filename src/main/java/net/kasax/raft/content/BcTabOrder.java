package net.kasax.raft.content;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The order of the items in the Beach Craft creative tabs, in groups (one line per group). Ids without namespace are
 * {@code raft:} ids. An item named here shows in this tab even when its module adds it to another one (the palm and
 * driftwood wood sets moved from the raft tab to Building, the coconut drinks to the Island Kitchen, the shark drops to
 * Creatures); items not named anywhere are shown after the listed ones in the tab their module chose. Ids of items that
 * do not exist are skipped.
 */
public final class BcTabOrder {
    private static final Map<BcTabs.Tab, List<String>> ORDER = new EnumMap<>(BcTabs.Tab.class);

    private BcTabOrder() {
    }

    private static void tab(BcTabs.Tab tab, String... groups) {
        List<String> ids = new java.util.ArrayList<>();
        for (String group : groups) {
            for (String id : group.trim().split("\\s+")) {
                if (!id.isEmpty()) ids.add(id);
            }
        }
        ORDER.put(tab, List.copyOf(ids));
    }

    public static List<String> order(BcTabs.Tab tab) {
        return ORDER.getOrDefault(tab, List.of());
    }

    static {
        tab(BcTabs.Tab.RAFT,
                // from the sea: the hook, what it brings in and what that turns into
                "plastic_hook paddle net item_catcher plastic scrap_metal rope unrefined_ocean_garbage trash_cube",
                // the raft itself
                "raft_foundation reinforced_foundation plastic_block scrap_metal_block rope_ladder rope_coil sail_canvas"
                        + " sail_canvas_red_striped ship_wheel raft_flag coconut_lantern shark_jaw_trophy",
                // staying alive
                "water_purifier fresh_water simple_grill crop_plot scarecrow brick_furnace titanium_furnace",
                // titanium
                "titanium_ore deepslate_titanium_ore raw_titanium raw_titanium_block titanium_nugget titanium_ingot"
                        + " titanium_block titanium_grates titanium_helmet titanium_chestplate titanium_leggings titanium_boots",
                // power and machines
                "makeshift_solar_panel makeshift_battery copper_cable titanium_cable circuit_board receiver metal_detector"
                        + " quarry chunk_destroyer",
                // the ancient technology
                "ancient_broken_ring ancient_crystal ancient_energy energy_staff raft_teleporter driftwood_portal_frame",
                "some_game_music_music_disc lofi_beat_music_disc crystal_castles_music_disc");
        tab(BcTabs.Tab.BUILDING,
                // wood, one set per kind: logs, planks, stairs, slab, fence, gate, door, trapdoor, button, plate
                "driftwood_log driftwood_wood stripped_driftwood_log stripped_driftwood_wood driftwood_planks"
                        + " driftwood_stairs driftwood_slab driftwood_fence driftwood_fence_gate driftwood_door"
                        + " driftwood_trapdoor driftwood_button driftwood_pressure_plate",
                "palm_log palm_wood stripped_palm_log stripped_palm_wood palm_planks palm_stairs palm_slab palm_fence"
                        + " palm_fence_gate palm_door palm_trapdoor palm_button palm_pressure_plate",
                "teak_planks teak_stairs teak_slab teak_fence teak_fence_gate teak_door teak_trapdoor",
                "weathered_planks weathered_stairs weathered_slab weathered_fence weathered_fence_gate weathered_door"
                        + " weathered_trapdoor",
                "palm_thatch palm_thatch_stairs palm_thatch_slab driftwood_lattice",
                // stone
                "coral_stone coral_stone_stairs coral_stone_slab coral_stone_wall polished_coral_stone"
                        + " polished_coral_stone_stairs polished_coral_stone_slab coral_stone_bricks coral_stone_brick_stairs"
                        + " coral_stone_brick_slab coral_stone_brick_wall mossy_coral_stone_bricks"
                        + " mossy_coral_stone_brick_stairs mossy_coral_stone_brick_slab mossy_coral_stone_brick_wall",
                "tidestone tidestone_stairs tidestone_slab tidestone_wall tidestone_bricks tidestone_brick_stairs"
                        + " tidestone_brick_slab tidestone_brick_wall cracked_tidestone_bricks mossy_tidestone_bricks"
                        + " mossy_tidestone_brick_stairs mossy_tidestone_brick_slab mossy_tidestone_brick_wall"
                        + " chiseled_tidestone tidestone_pillar glowing_tidestone",
                "shell_tiles shell_tile_stairs shell_tile_slab shell_tile_wall",
                "pumice_bricks pumice_brick_stairs pumice_brick_slab pumice_brick_wall",
                // glass
                "aqua_sea_glass aqua_sea_glass_pane green_sea_glass green_sea_glass_pane blue_sea_glass"
                        + " blue_sea_glass_pane cracked_glass cracked_glass_pane",
                // the drowned city
                "cracked_concrete cracked_concrete_stairs cracked_concrete_slab cracked_concrete_wall mossy_concrete"
                        + " rebar_concrete asphalt asphalt_stairs asphalt_slab asphalt_line",
                // metal and ships
                "riveted_hull rusty_hull rusted_plate corrugated_metal corrugated_metal_stairs corrugated_metal_slab"
                        + " steel_beam catwalk steel_railing porthole bulkhead_door metal_hatch rusty_pipe rusty_pipe_valve",
                "shipping_container_red shipping_container_blue shipping_container_green control_panel server_rack"
                        + " satellite_dish wind_turbine");
        tab(BcTabs.Tab.DECOR,
                // lights
                "string_lights_warm string_lights_colorful bunting_red_white bunting_rainbow paper_lantern_white"
                        + " paper_lantern_red paper_lantern_orange paper_lantern_yellow paper_lantern_lime paper_lantern_cyan"
                        + " paper_lantern_blue paper_lantern_purple paper_lantern_pink",
                "ship_lantern driftwood_lamp seashell_lamp jelly_lamp tiki_torch brazier neon_tube_cyan neon_tube_pink",
                // furniture
                "driftwood_chair palm_chair driftwood_table palm_table driftwood_shelf deck_chair hammock",
                "cushion_white cushion_red cushion_blue cushion_yellow cushion_green woven_rug beach_umbrella_red"
                        + " beach_umbrella_blue beach_umbrella_yellow",
                "wind_chime aquarium hanging_planter",
                // on deck
                "ship_bell chart_table spyglass_stand figurehead life_ring rope_railing cargo_net ship_anchor"
                        + " ship_cannon cannonball buoy_red buoy_green lighthouse_lens",
                // pirates and treasure
                "treasure_chest gold_coin gold_coin_pile jolly_roger wanted_poster rum_bottles powder_keg wooden_crate");
        tab(BcTabs.Tab.COZY,
                // seats, tables
                "palm_bench teak_bench bar_stool rattan_armchair sun_lounger teak_table bamboo_table serving_plate",
                // storage
                "palm_shelf teak_shelf weathered_shelf wicker_basket",
                // lights
                "hurricane_lantern sea_glass_lamp driftwood_sconce shell_candle",
                // floor and walls
                "beach_towel_red beach_towel_blue beach_towel_yellow rope_rug bamboo_blinds shell_garland fishing_net"
                        + " surfboard_red surfboard_blue surfboard_yellow fish_trophy",
                // plants
                "potted_monstera coconut_planter flower_box coral_vase",
                // music and outdoors
                "steel_drum conch_shell thatch_parasol dock_bollard");
        tab(BcTabs.Tab.NATURE,
                // ground
                "shell_sand black_sand volcanic_ash pumice bubble_vent",
                // trees (the driftwood sapling is in no tab since 3.5: driftwood is no tree, old saplings stay valid)
                "palm_sapling palm_leaves palm_coconut_leaves driftwood_leaves",
                // beach plants and flowers
                "beach_grass sea_oats tropical_fern aloe_vera pineapple_bush pineapple pineapple_crown",
                "hibiscus bird_of_paradise plumeria beach_morning_glory tropical_orchid spider_lily heliconia",
                // under water
                "glow_kelp sea_anemone_red sea_anemone_purple sea_anemone_glow tube_worms giant_clam starfish_orange"
                        + " starfish_purple starfish_red sea_urchin urchin_spine barnacles",
                // finds
                "seashell pearl sea_glass_shard");
        tab(BcTabs.Tab.KITCHEN,
                // find it in the wild
                "wild_sweet_potato wild_chili wild_banana wild_rice",
                // grow it
                "sweet_potato chili_seeds chili_pepper banana_shoot banana rice sea_grapes coconut",
                // the kitchen
                "cooking_pot fish_trap",
                // snacks and simple food
                "baked_sweet_potato cooked_rice sushi_roll banana_bread coconut_milk",
                // treats and drinks
                "fruit_salad coconut_ice_cream tropical_smoothie pina_colada",
                // dishes from the pot and the bowl
                "chili_crab sweet_potato_soup island_curry seafood_chowder");
        tab(BcTabs.Tab.GEAR,
                // weapons
                "shark_tooth_spear machete boomerang",
                // armour sets
                "shell_helmet shell_chestplate shell_leggings shell_boots",
                "explorer_helmet explorer_chestplate explorer_leggings explorer_boots",
                // finding the way
                "wreck_compass captains_spyglass",
                // fishing
                "driftwood_fishing_rod fish_bait golden_lure");
        tab(BcTabs.Tab.VOYAGE,
                // boats
                "driftwood_raft driftwood_chest_raft palm_boat palm_chest_boat",
                // a raft of your own
                "helm sail mast oar raft_motor anchor_winch",
                // the expedition raft and its upgrades
                "expedition_raft raft_sail raft_engine raft_anchor raft_crate raft_lantern",
                // diving
                "diving_helmet oxygen_tank titanium_oxygen_tank wetsuit flippers air_compressor sea_scooter harpoon_gun"
                        + " harpoon");
        tab(BcTabs.Tab.CREATURES,
                // on the shore
                "crab_spawn_egg crab_claw cooked_crab_claw ember_crab_spawn_egg ember_shell seagull_spawn_egg"
                        + " penguin_spawn_egg sea_otter_spawn_egg",
                // in the sea
                "shark_spawn_egg shark_tooth shark_meat cooked_shark_meat hammerhead_shark_spawn_egg whale_spawn_egg"
                        + " manta_ray_spawn_egg jellyfish_spawn_egg jelly_blob glow_jelly",
                // in the deep
                "anglerfish_spawn_egg angler_lure moray_eel_spawn_egg eel_fillet cooked_eel_fillet reef_guardian_spawn_egg",
                // pirates and scavengers
                "drowned_corsair_spawn_egg corsair_captain_spawn_egg cutlass scavenger_spawn_egg",
                // people of the sea
                "keeper_fenn_spawn_egg nalu_spawn_egg old_tamo_spawn_egg teo_spawn_egg kai_spawn_egg okafor_spawn_egg"
                        + " dr_voss_spawn_egg dr_lund_spawn_egg");
        tab(BcTabs.Tab.STORY,
                // read and listen
                "seafarers_almanac expedition_journal message_in_a_bottle supply_barrel expedition_radio research_terminal",
                // the sigils and the Tidewardens
                "coral_sigil pearl_sigil storm_sigil ember_sigil sigil_pedestal tidewarden_key tidewarden_altar tide_seal"
                        + " sanctum_tidestone sanctum_glass",
                // the Leviathan
                "leviathan_altar tideling_spawn_egg leviathan_spawn_egg leviathan_scale heart_of_the_tide tidebreaker"
                        + " leviathan_trophy");
    }
}
