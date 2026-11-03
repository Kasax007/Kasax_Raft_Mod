"""The 12 biomes of the raft dimension (docs/design/beachcraft3.md, section 4.2): data/raft/worldgen/biome/*.json.

Feature lists are built from one master order per generation step (FEATURES below) so that every biome lists its
features in the same relative order (the game rejects feature order cycles).
"""
import os
import re
from collections import OrderedDict

ISLANDS = ['palm_isle', 'jungle_isle', 'mangrove_isle', 'volcanic_isle']
SHALLOWS = ['coral_shallows', 'glowing_lagoon']
DEEP = ['raft_ocean_biome', 'kelp_forest', 'wreck_graveyard', 'abyssal_trench', 'drowned_shelf', 'frost_floes']
ALL = DEEP[:1] + ['kelp_forest', 'coral_shallows', 'glowing_lagoon', 'drowned_shelf', 'abyssal_trench',
                  'wreck_graveyard', 'frost_floes'] + ISLANDS
OCEAN = DEEP + SHALLOWS

NAMES = {
    'raft_ocean_biome': 'Endless Sea', 'kelp_forest': 'Kelp Forest', 'coral_shallows': 'Coral Shallows',
    'glowing_lagoon': 'Glowing Lagoon', 'drowned_shelf': 'Drowned Shelf', 'abyssal_trench': 'Abyssal Trench',
    'wreck_graveyard': 'Wreck Graveyard', 'frost_floes': 'Frost Floes', 'palm_isle': 'Palm Isle',
    'jungle_isle': 'Jungle Isle', 'mangrove_isle': 'Mangrove Isle', 'volcanic_isle': 'Volcanic Isle',
}

# --------------------------------------------------------------------------------------------- climate / looks
# temperature, downfall, water colour, water fog, fog, sky, extra effects/attributes
#
# Water fog colours all have red = 0 on purpose. The game "adapts" the eyes under water: while you stay under water it
# scales the fog colour up until its brightest channel is 1.0 (FogRenderer, water vision), so every dark fog turns
# into a bright one after a few seconds and the dark trench looked like a bright blue pool. That scaling is skipped
# when one channel is exactly 0, so with red = 0 the fog keeps the colour written here: bright turquoise in the
# shallows, deep blue in the open sea, near-black navy in the trench (where only glowing life lights the way).
# All biomes use it, so blending at biome borders never switches between scaled and unscaled fog.
LOOK = {
    'raft_ocean_biome': dict(t=0.7, d=0.5, water='#2a86d8', wfog='#0052b8', fog='#22a1e6', sky='#6cbaea',
                             music='minecraft:music.overworld.cherry_grove'),
    'kelp_forest': dict(t=0.6, d=0.6, water='#2b9a92', wfog='#00866f', fog='#2aa3d6', sky='#6cbaea', wfog_dist=0.85,
                        music='minecraft:music.overworld.lush_caves'),
    'coral_shallows': dict(t=0.9, d=0.5, water='#3ad9e6', wfog='#00d2f5', fog='#3ab2ea', sky='#6cc4f0', wfog_dist=1.6,
                           music='minecraft:music.overworld.flower_forest'),
    'glowing_lagoon': dict(t=0.85, d=0.6, water='#18a7a3', wfog='#00a3ad', fog='#3aa8c8', sky='#6cbaea',
                           wfog_dist=0.9, particles=[('minecraft:glow', 0.006)],
                           music='minecraft:music.overworld.lush_caves'),
    'drowned_shelf': dict(t=0.6, d=0.5, water='#4e7468', wfog='#00644f', fog='#7fa6b8', sky='#7bb4dc', wfog_dist=0.6,
                          music='minecraft:music.overworld.deep_dark'),
    'abyssal_trench': dict(t=0.5, d=0.5, water='#12305c', wfog='#001634', fog='#2a7fc0', sky='#6cbaea',
                           wfog_dist=0.4, music='minecraft:music.overworld.deep_dark'),
    'wreck_graveyard': dict(t=0.6, d=0.5, water='#4a6d8e', wfog='#00405a', fog='#9fb1c0', sky='#90abc4',
                            wfog_dist=0.7, music='minecraft:music.overworld.old_growth_taiga'),
    'frost_floes': dict(t=0.2, d=0.5, water='#3d8fd6', wfog='#0078c8', fog='#c4dcf2', sky='#9ccaee',
                        music='minecraft:music.overworld.frozen_peaks'),
    'palm_isle': dict(t=0.95, d=0.5, water='#3fc8e4', wfog='#00b4f0', fog='#22a1e6', sky='#6cbaea',
                      grass='#7cc45a', foliage='#5ab63c', music='minecraft:music.overworld.cherry_grove'),
    'jungle_isle': dict(t=0.95, d=0.9, water='#2fb4cc', wfog='#00aedc', fog='#2aa4e0', sky='#6cbaea',
                        music='minecraft:music.overworld.jungle'),
    'mangrove_isle': dict(t=0.8, d=0.9, water='#3a7a6a', wfog='#00704c', fog='#9fc8d8', sky='#78b4e0',
                          foliage='#8db127', grass_mod='swamp', wfog_dist=0.85,
                          music='minecraft:music.overworld.swamp'),
    'volcanic_isle': dict(t=1.2, d=0.2, water='#3a8cc0', wfog='#007ab4', fog='#b4aca8', sky='#a2b6c8',
                          grass='#9aa05a', foliage='#8a9a48', particles=[('minecraft:white_ash', 0.025)],
                          music='minecraft:music.overworld.badlands'),
}

# --------------------------------------------------------------------------------------------- spawns
# (category, entity, weight, min, max) as in the spec. Only vanilla mobs and the 2.0 shark go into the biome JSON: the
# creatures of the other 3.0 modules are added at runtime by content/world/WorldSpawns.java (looked up by id, so a
# missing creature cannot break the biome registry). Keep both tables in sync.
SPAWNS = {
    'raft_ocean_biome': [('water_creature', 'raft:shark', 3, 1, 1), ('water_creature', 'raft:whale', 1, 1, 1),
                         ('water_ambient', 'raft:jellyfish', 4, 2, 4), ('water_creature', 'raft:hammerhead_shark', 1, 1, 1),
                         ('water_creature', 'minecraft:squid', 4, 1, 4), ('water_creature', 'minecraft:dolphin', 2, 1, 2),
                         ('water_ambient', 'minecraft:cod', 10, 3, 6)],
    'kelp_forest': [('water_creature', 'raft:sea_otter', 4, 1, 3), ('water_ambient', 'raft:jellyfish', 2, 1, 3),
                    ('water_ambient', 'raft:moray_eel', 1, 1, 1),
                    ('water_ambient', 'minecraft:cod', 10, 3, 6), ('water_ambient', 'minecraft:salmon', 5, 1, 5)],
    'coral_shallows': [('water_creature', 'raft:manta_ray', 3, 1, 2), ('water_ambient', 'raft:moray_eel', 2, 1, 1),
                       ('creature', 'minecraft:turtle', 5, 2, 5),
                       ('water_ambient', 'minecraft:tropical_fish', 25, 8, 8),
                       ('water_ambient', 'minecraft:pufferfish', 15, 1, 3)],
    'glowing_lagoon': [('water_ambient', 'raft:jellyfish', 6, 2, 5), ('water_creature', 'raft:manta_ray', 1, 1, 1),
                       ('underground_water_creature', 'minecraft:glow_squid', 10, 2, 4)],
    'drowned_shelf': [('water_ambient', 'raft:moray_eel', 2, 1, 1), ('monster', 'raft:drowned_corsair', 1, 1, 2),
                      ('monster', 'minecraft:drowned', 5, 1, 2), ('water_ambient', 'minecraft:cod', 10, 3, 6)],
    'abyssal_trench': [('underground_water_creature', 'raft:anglerfish', 6, 1, 2), ('water_ambient', 'raft:jellyfish', 2, 1, 3),
                       ('underground_water_creature', 'minecraft:glow_squid', 3, 1, 3)],
    'wreck_graveyard': [('monster', 'raft:drowned_corsair', 3, 1, 3), ('water_creature', 'raft:shark', 3, 1, 1),
                        ('monster', 'minecraft:drowned', 5, 1, 2)],
    'frost_floes': [('creature', 'raft:penguin', 6, 2, 5), ('creature', 'minecraft:polar_bear', 1, 1, 2),
                    ('water_ambient', 'minecraft:salmon', 15, 1, 5), ('water_creature', 'raft:whale', 1, 1, 1)],
    'palm_isle': [('creature', 'raft:crab', 8, 2, 4), ('creature', 'raft:seagull', 6, 2, 5)],
    'jungle_isle': [('creature', 'minecraft:parrot', 40, 1, 2), ('creature', 'minecraft:ocelot', 2, 1, 3),
                    ('creature', 'raft:seagull', 2, 1, 3), ('creature', 'raft:crab', 3, 1, 3)],
    'mangrove_isle': [('creature', 'minecraft:frog', 10, 2, 5), ('creature', 'raft:crab', 4, 1, 3),
                      ('monster', 'minecraft:slime', 1, 1, 1)],
    'volcanic_isle': [('monster', 'raft:ember_crab', 5, 1, 3), ('creature', 'raft:seagull', 1, 1, 2)],
}

CARVERS = {b: ['minecraft:cave'] for b in ALL}
for _b in ('coral_shallows', 'glowing_lagoon', 'drowned_shelf'):
    CARVERS[_b] = []

# --------------------------------------------------------------------------------------------- features
VANILLA_ORES = ['minecraft:ore_dirt', 'minecraft:ore_gravel', 'minecraft:ore_granite_upper',
                'minecraft:ore_granite_lower', 'minecraft:ore_diorite_upper', 'minecraft:ore_diorite_lower',
                'minecraft:ore_andesite_upper', 'minecraft:ore_andesite_lower', 'minecraft:ore_tuff',
                'minecraft:ore_coal_upper', 'minecraft:ore_coal_lower', 'minecraft:ore_iron_upper',
                'minecraft:ore_iron_middle', 'minecraft:ore_iron_small', 'minecraft:ore_gold',
                'minecraft:ore_gold_lower', 'minecraft:ore_redstone', 'minecraft:ore_redstone_lower',
                'minecraft:ore_diamond', 'minecraft:ore_diamond_medium', 'minecraft:ore_diamond_large',
                'minecraft:ore_diamond_buried', 'minecraft:ore_lapis', 'minecraft:ore_lapis_buried',
                'minecraft:ore_copper']

# step index -> ordered [(placed feature, [biomes])]
FEATURES = {
    1: [('minecraft:lake_lava_surface', ['volcanic_isle'])],
    2: [('minecraft:iceberg_packed', ['frost_floes']), ('minecraft:iceberg_blue', ['frost_floes']),
        ('raft:crater_lava', ['volcanic_isle']),
        ('raft:boulder_andesite', ['palm_isle']), ('raft:boulder_mossy', ['jungle_isle']),
        ('raft:boulder_pumice', ['volcanic_isle'])],
    4: [('minecraft:blue_ice', ['frost_floes']),
        ('minecraft:small_basalt_columns', ['volcanic_isle']), ('minecraft:large_basalt_columns', ['volcanic_isle']),
        ('raft:ice_floe', ['frost_floes'])],
    6: [(o, ALL) for o in VANILLA_ORES] + [
        ('raft:titanium_ore_placed', ALL),
        ('minecraft:ore_magma', ['volcanic_isle', 'abyssal_trench']),
        ('minecraft:underwater_magma', ['raft_ocean_biome', 'abyssal_trench', 'kelp_forest', 'wreck_graveyard']),
        ('minecraft:disk_sand', ['raft_ocean_biome', 'coral_shallows', 'glowing_lagoon', 'kelp_forest',
                                 'wreck_graveyard', 'palm_isle', 'jungle_isle']),
        ('minecraft:disk_clay', ['raft_ocean_biome', 'kelp_forest', 'glowing_lagoon', 'drowned_shelf',
                                 'mangrove_isle', 'frost_floes']),
        ('minecraft:disk_gravel', ['raft_ocean_biome', 'kelp_forest', 'drowned_shelf', 'wreck_graveyard',
                                   'frost_floes', 'abyssal_trench'])],
    7: [('minecraft:basalt_blobs', ['volcanic_isle']), ('minecraft:blackstone_blobs', ['volcanic_isle'])],
    8: [('minecraft:spring_water', ['jungle_isle', 'palm_isle', 'mangrove_isle']),
        ('minecraft:spring_lava', ['volcanic_isle'])],
    9: [
        # land: trees first, then ground plants
        ('raft:palm_trees', ['palm_isle']),
        ('raft:palm_placed', ['palm_isle', 'jungle_isle', 'raft_ocean_biome']),
        ('raft:palm_trees_sparse', ['jungle_isle']),
        ('minecraft:bamboo_light', ['jungle_isle']),
        ('minecraft:trees_jungle', ['jungle_isle']),
        ('minecraft:trees_mangrove', ['mangrove_isle']),
        ('raft:fallen_driftwood', ['palm_isle', 'jungle_isle', 'mangrove_isle']),
        ('minecraft:patch_grass_jungle', ['jungle_isle']),
        ('minecraft:patch_grass_normal', ['mangrove_isle']),
        ('raft:patch_beach_grass', ['palm_isle', 'jungle_isle']),
        ('raft:patch_sea_oats', ['palm_isle', 'jungle_isle']),
        ('raft:patch_island_grass', ['palm_isle']),
        ('raft:patch_hibiscus', ['palm_isle', 'jungle_isle']),
        ('raft:patch_bird_of_paradise', ['palm_isle', 'jungle_isle']),
        ('raft:patch_tropical_fern', ['jungle_isle', 'mangrove_isle']),
        ('raft:patch_pineapple_bush', ['palm_isle', 'jungle_isle']),
        ('minecraft:patch_melon', ['jungle_isle']),
        ('minecraft:vines', ['jungle_isle']),
        ('minecraft:patch_waterlily', ['mangrove_isle']),
        ('raft:patch_seashells', ['palm_isle', 'jungle_isle', 'mangrove_isle']),
        ('raft:patch_sea_glass', ['palm_isle', 'jungle_isle', 'volcanic_isle']),
        ('raft:patch_volcanic_ash_dunes', ['volcanic_isle']),
        # sea floor: reefs, kelp, then small plants
        ('raft:coral_reef', ['coral_shallows']),
        ('raft:rare_tube_coral', ['raft_ocean_biome']),
        ('raft:tall_kelp', ['kelp_forest']),
        ('minecraft:kelp_warm', ['raft_ocean_biome', 'wreck_graveyard', 'glowing_lagoon']),
        ('minecraft:kelp_cold', ['frost_floes']),
        ('raft:glow_kelp_patch', ['kelp_forest', 'glowing_lagoon']),
        ('raft:concrete_rubble', ['drowned_shelf']),
        ('raft:wreck_debris', ['wreck_graveyard']),
        ('raft:bubble_vents', ['glowing_lagoon', 'abyssal_trench']),
        ('raft:bubble_vents_sparse', ['raft_ocean_biome']),
        ('raft:tube_worms', ['abyssal_trench']),
        ('raft:tube_worms_sparse', ['glowing_lagoon']),
        ('raft:anemones', ['coral_shallows']),
        ('raft:glow_anemones', ['glowing_lagoon']),
        ('raft:glow_anemones_sparse', ['abyssal_trench']),
        ('raft:giant_clams', ['coral_shallows', 'glowing_lagoon']),
        ('raft:starfish', ['coral_shallows', 'glowing_lagoon', 'kelp_forest']),
        ('raft:sea_urchins', ['coral_shallows', 'kelp_forest']),
        ('raft:barnacles', ['kelp_forest', 'drowned_shelf', 'wreck_graveyard', 'raft_ocean_biome']),
        ('raft:seashells_reef', ['coral_shallows', 'glowing_lagoon']),
        ('raft:sea_glass_reef', ['coral_shallows']),
        ('minecraft:sea_pickle', ['coral_shallows']),
        ('minecraft:seagrass_warm', ['coral_shallows', 'glowing_lagoon']),
        ('minecraft:seagrass_deep_warm', ['raft_ocean_biome', 'kelp_forest']),
        ('minecraft:seagrass_normal', ['drowned_shelf', 'wreck_graveyard']),
        ('minecraft:seagrass_cold', ['frost_floes']),
        ('minecraft:seagrass_swamp', ['mangrove_isle']),
    ],
    # top_layer_modification: last, after everything else of the chunk is placed
    10: [('raft:gravity_support', ALL)],
}


def features_of(biome, available=None):
    steps = []
    last = max(FEATURES)
    for step in range(last + 1):
        lst = [f for f, bs in FEATURES.get(step, []) if biome in bs and (available is None or available(f))]
        steps.append(lst)
    while steps and not steps[-1]:
        steps.pop()
    return steps


JSON_SPAWNS = {'raft:shark'}


def spawns_of(biome, available=None):
    by = OrderedDict()
    for cat, ent, w, lo, hi in SPAWNS[biome]:
        if not (ent.startswith('minecraft:') or ent in JSON_SPAWNS):
            continue
        if available is not None and not available(ent):
            continue
        e = OrderedDict(type=ent, count=(lo if lo == hi else OrderedDict(type='minecraft:uniform', max_inclusive=hi,
                                                                          min_inclusive=lo)), weight=w)
        by.setdefault(cat, []).append(e)
    return by


def biome_json(b, feature_ok=None, entity_ok=None):
    L = LOOK[b]
    attrs = OrderedDict()
    music = OrderedDict(default=OrderedDict(max_delay=24000, min_delay=12000, sound=L['music']))
    music['underwater'] = OrderedDict(max_delay=24000, min_delay=12000, sound='minecraft:music.under_water')
    attrs['minecraft:audio/background_music'] = music
    attrs['minecraft:gameplay/natural_mob_spawns'] = OrderedDict(
        argument=OrderedDict(spawn_costs={}, spawns_by_category=spawns_of(b, entity_ok)), modifier='overlay')
    if L.get('particles'):
        attrs['minecraft:visual/ambient_particles'] = OrderedDict(
            argument=[OrderedDict(particle={'type': p}, probability=pr) for p, pr in L['particles']], modifier='append')
    attrs['minecraft:visual/fog_color'] = L['fog']
    attrs['minecraft:visual/sky_color'] = L['sky']
    assert L['wfog'].startswith('#00'), b + ': water fog needs red = 0 (see LOOK)'
    attrs['minecraft:visual/water_fog_color'] = L['wfog']
    if L.get('wfog_dist'):
        attrs['minecraft:visual/water_fog_end_distance'] = OrderedDict(argument=L['wfog_dist'], modifier='multiply')
    eff = OrderedDict()
    if L.get('foliage'):
        eff['foliage_color'] = L['foliage']
    if L.get('grass'):
        eff['grass_color'] = L['grass']
    if L.get('grass_mod'):
        eff['grass_color_modifier'] = L['grass_mod']
    eff['water_color'] = L['water']
    return OrderedDict([
        ('attributes', attrs),
        ('carvers', CARVERS[b]),
        ('downfall', L['d']),
        ('effects', eff),
        ('features', features_of(b, feature_ok)),
        ('has_precipitation', False),
        ('temperature', L['t']),
    ])


# --------------------------------------------------------------------------------------------- tags
BIOME_TAGS = {
    'raft:is_island': ISLANDS,
    'raft:is_shallow': SHALLOWS,
    'raft:is_deep': ['raft_ocean_biome', 'kelp_forest', 'wreck_graveyard', 'abyssal_trench', 'drowned_shelf',
                     'frost_floes'],
    'raft:is_raft_dimension': ALL,
    # Vanilla tags that 26.3 code actually reads (see section 14: is_ocean / is_deep_ocean are left out on purpose,
    # in 26.3 they only place vanilla ocean monuments and nether ruined portals).
    'minecraft:water_on_map_outlines': OCEAN,                    # exploration maps draw these as water
    'minecraft:has_structure/shipwreck': ['raft_ocean_biome', 'wreck_graveyard', 'kelp_forest'],
    'minecraft:allows_tropical_fish_spawns_at_any_height': SHALLOWS,   # reef fish down at the reef floor
    'minecraft:produces_corals_from_bonemeal': SHALLOWS,
    'minecraft:polar_bears_spawn_on_alternate_blocks': ['frost_floes'],  # polar bears on ice floes
    'minecraft:spawns_warm_variant_frogs': ['mangrove_isle', 'jungle_isle'],
    'minecraft:allows_surface_slime_spawns': ['mangrove_isle'],
}


def write(module, feature_ok=None, entity_ok=None):
    from bclib import assets
    for b in ALL:
        assets.write_json(module, f'data/raft/worldgen/biome/{b}.json', biome_json(b, feature_ok, entity_ok))
    for tag, bs in BIOME_TAGS.items():
        if not bs:
            continue
        assets.tag(module, 'worldgen/biome', tag, ['raft:' + x for x in bs])
    assets.lang(module, {f'biome.raft.{b}': n for b, n in NAMES.items()})
