"""Worldgen JSON of Port Solace (26.3 formats): structure, structure set, template pools, processor list, biome tag."""
from .common import (MODULE, NO_SPILL, biome_tag, processor_list, rule, spawns, structure_json, structure_set,
                     write_json)

SID = 'port_solace'
P = 'raft:port_solace'
PROC = f'{P}/city'


def pool(name, elements, fallback='minecraft:empty'):
    """elements: (template, weight[, override_liquid])"""
    els = []
    for e in elements:
        loc, w = e[0], e[1]
        el = {'element_type': 'minecraft:single_pool_element', 'location': f'{P}/{loc}', 'processors': PROC,
              'projection': 'rigid'}
        if len(e) > 2 and e[2]:
            el['override_liquid_settings'] = e[2]
        els.append({'element': el, 'weight': w})
    return write_json(MODULE, f'data/raft/worldgen/template_pool/port_solace/{name}.json',
                      {'elements': els, 'fallback': fallback if ':' in fallback else f'{P}/{fallback}'})


def write_all():
    files = []
    # processors: water of the template never spills above the sea surface; a little extra wear per placement
    files.append(processor_list(PROC, [
        NO_SPILL,
        {'processor_type': 'minecraft:rule', 'rules': [
            rule('minecraft:light_gray_concrete', 'raft:cracked_concrete', probability=0.08),
            rule('minecraft:smooth_stone', 'raft:mossy_concrete', probability=0.05),
            rule('raft:asphalt', 'minecraft:gravel', probability=0.015),
        ]},
    ]))
    files += [
        pool('plaza', [('plaza', 1)]),
        pool('city_hall', [('city_hall_base', 1, 'ignore_waterlogging')]),
        pool('city_hall_top', [('city_hall_top', 1)]),
        pool('avenues_power', [('avenue_power', 1)], 'ends'),
        pool('avenues_harbour', [('avenue_harbour', 1)], 'ends'),
        pool('avenues_market', [('avenue_market', 1)], 'ends'),
        pool('junctions', [('crossroad', 2), ('tee', 2)], 'ends'),
        pool('streets', [('street_a', 2), ('street_b', 2)], 'ends'),
        pool('ends', [('end_barricade', 1), ('end_sinkhole', 1)]),
        pool('lots_deco', [('deco_tower_base', 1)], 'small_lots'),
        pool('lots_glass', [('glass_tower_base', 1)], 'small_lots'),
        pool('lots_mid', [('hotel', 2), ('dept_store', 3), ('apartment_brick', 1), ('shop_row', 1)], 'small_lots'),
        pool('buildings', [('apartment_brick', 3), ('apartment_teal', 3), ('shop_row', 3), ('diner', 1),
                           ('fallen_tower', 2), ('hotel', 1), ('park_lot', 1), ('rubble_lot', 1)], 'small_lots'),
        pool('lots_shallow', [('apartment_teal', 3), ('apartment_brick', 2), ('shop_row', 2), ('diner', 1),
                              ('park_lot', 1), ('rubble_lot', 1)], 'small_lots'),
        pool('small_lots', [('diner', 1), ('park_lot', 2), ('rubble_lot', 2)]),
        pool('tops/deco_tower', [('deco_tower_top', 1)]),
        pool('tops/glass_tower', [('glass_tower_top', 1)]),
    ]
    overrides = {
        'monster': spawns([('minecraft:drowned', 4, 1, 2), ('raft:drowned_corsair', 2, 1, 2),
                           ('raft:moray_eel', 2, 1, 1)]),
        'water_ambient': spawns([('raft:moray_eel', 2, 1, 1), ('minecraft:cod', 6, 2, 4)]),
    }
    # max_distance 104: the game hands pieces only to chunks up to 8 chunks (128 blocks) from the start chunk; the
    # 47-block plaza puts its center up to 23 blocks from the start, so 112 let 1 in 6 cities reach a 9th chunk
    # whose part was never built (layout.py: 0 of 60 cities beyond 8 chunks with 104, same number of pieces)
    files.append(structure_json(SID, f'{P}/plaza', 'surface_structures', 'beard_thin', -2, heightmap='OCEAN_FLOOR_WG',
                                size=6, max_distance=104, spawn_overrides=overrides, liquid='apply_waterlogging'))
    files.append(structure_set(SID, 48, 20, 31415931))
    files.append(biome_tag(SID, ['drowned_shelf']))
    return files
