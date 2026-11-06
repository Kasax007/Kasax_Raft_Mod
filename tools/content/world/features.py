"""World generation features of the raft dimension (docs/design/beachcraft3.md, section 4.3 "Features"):
data/raft/worldgen/feature/*.json and data/raft/worldgen/placed_feature/*.json.

26.3 format: a feature file is the feature itself ({"type": ..., config fields inline}); a placed feature names it
and lists placement modifiers. Patches (flowers, grass, shells) use the vanilla pattern: count tries + trapezoid
offset + block predicate (there is no random_patch any more). The placed feature ids are the ones biomes.py lists.
Every block used here is a vanilla block, a 2.0 Beach Craft block or a block of this module, so the files always
load; blocks of other 3.0 modules only appear as `optional_blocks` of raft:rubble (looked up when placing).
"""
import copy
import json
import os
from collections import OrderedDict

from bclib.paths import VANILLA

FEATURES = OrderedDict()   # name -> feature json
PLACED = OrderedDict()     # name -> placed feature json


# --------------------------------------------------------------------------------------------- helpers
def st(block, **props):
    """Block state (simple provider when used as a provider)."""
    d = OrderedDict(id=block if ':' in block else 'raft:' + block)
    if props:
        d['properties'] = OrderedDict((k, str(v).lower() if isinstance(v, bool) else str(v)) for k, v in props.items())
    return d


def weighted(entries):
    return OrderedDict(type='minecraft:weighted', entries=[OrderedDict(data=s, weight=w) for s, w in entries])


def rand_int(prop, source, lo, hi):
    return OrderedDict(type='minecraft:randomized_int', property=prop, source=source,
                       values=uniform(lo, hi))


def uniform(lo, hi):
    return OrderedDict(type='minecraft:uniform', max_inclusive=hi, min_inclusive=lo)


def wet_or_dry(wet, dry):
    """Provider: the waterlogged variant where the block lands in water, else the dry one."""
    return OrderedDict(type='minecraft:rule_based', fallback=dry, rules=[OrderedDict(
        if_true=OrderedDict(type='minecraft:matching_fluids', fluids='minecraft:water'), then=wet)])


def simple(provider):
    return OrderedDict(type='minecraft:simple_block', to_place=provider)


def blocks(*ids, offset=None):
    d = OrderedDict(type='minecraft:matching_blocks', blocks=[b if ':' in b else 'raft:' + b for b in ids])
    if offset:
        d['offset'] = list(offset)
    return d


def tag_pred(tag, offset=None):
    d = OrderedDict(type='minecraft:matching_block_tag', tag=tag)
    if offset:
        d['offset'] = list(offset)
    return d


def all_of(*ps):
    return OrderedDict(type='minecraft:all_of', predicates=list(ps))


def any_of(*ps):
    return OrderedDict(type='minecraft:any_of', predicates=list(ps))


def not_(p):
    return OrderedDict(type='minecraft:not', predicate=p)


WATER = blocks('minecraft:water')
AIR = tag_pred('minecraft:air')
STURDY_BELOW = OrderedDict(type='minecraft:has_sturdy_face', direction='up', offset=[0, -1, 0])


# placement modifiers
def rarity(n):
    return OrderedDict(type='minecraft:rarity_filter', chance=n)


def count(n):
    return OrderedDict(type='minecraft:count', count=n)


IN_SQUARE = OrderedDict(type='minecraft:in_square')
BIOME = OrderedDict(type='minecraft:biome')


def heightmap(h):
    return OrderedDict(type='minecraft:heightmap', heightmap=h)


def offset(xz, y):
    def tz(r):
        return OrderedDict(type='minecraft:trapezoid', max=r, min=-r, plateau=0)
    return OrderedDict(type='minecraft:offset', x=tz(xz), y=tz(y), z=tz(xz))


def predicate(p):
    return OrderedDict(type='minecraft:block_predicate_filter', predicate=p)


def water_depth(n):
    return OrderedDict(type='minecraft:surface_water_depth_filter', max_water_depth=n)


def clear(d):
    """raft:clear_of_structures (ClearOfStructuresFilter): no feature within d blocks of a structure piece, so nothing
    grows into the rooms of a structure (basalt columns filled the Ashen Forge, trees grew into the lighthouse)."""
    return OrderedDict(type='raft:clear_of_structures', distance=d)


# vanilla placed features the biomes use as raft:<name> copies with clear(d) at the end (biomes.guarded): every
# feature that builds or replaces blocks next to structures. Distances: the reach of the feature from its position.
GUARDED_VANILLA = OrderedDict([
    ('lake_lava_surface', 9), ('iceberg_packed', 6), ('iceberg_blue', 6), ('blue_ice', 2),
    ('small_basalt_columns', 10), ('large_basalt_columns', 10),
    ('ore_dirt', 4), ('ore_gravel', 4), ('ore_granite_upper', 4), ('ore_granite_lower', 4), ('ore_diorite_upper', 4),
    ('ore_diorite_lower', 4), ('ore_andesite_upper', 4), ('ore_andesite_lower', 4), ('ore_tuff', 4),
    ('ore_coal_upper', 3), ('ore_coal_lower', 3), ('ore_iron_upper', 3), ('ore_iron_middle', 3), ('ore_iron_small', 2),
    ('ore_gold', 3), ('ore_gold_lower', 3), ('ore_redstone', 3), ('ore_redstone_lower', 3), ('ore_diamond', 2),
    ('ore_diamond_medium', 3), ('ore_diamond_large', 3), ('ore_diamond_buried', 3), ('ore_lapis', 2),
    ('ore_lapis_buried', 2), ('ore_copper', 3), ('ore_magma', 4), ('underwater_magma', 2),
    ('disk_sand', 4), ('disk_clay', 3), ('disk_gravel', 3),
    ('spring_water', 2), ('spring_lava', 2),
    ('bamboo_light', 1), ('trees_jungle', 6), ('trees_mangrove', 6),
    ('patch_grass_jungle', 0), ('patch_grass_normal', 0), ('patch_melon', 0), ('vines', 0), ('patch_waterlily', 0),
    ('kelp_warm', 0), ('kelp_cold', 0),
])


def guarded_vanilla():
    for name, d in GUARDED_VANILLA.items():
        v = vanilla('placed_feature/' + name)
        placed(name, v['feature'], list(v['placement']) + [clear(d)])


def feature(name, data):
    FEATURES[name] = data
    return 'raft:' + name


def placed(name, feat, placement):
    PLACED[name] = OrderedDict(feature=feat, placement=placement)


def land_patch(name, feat, tries, xz=7, y=3, per_chunk=None, below=None):
    """Plants on land: per_chunk = rarity int (1 in n chunks) or ('count', n)."""
    pl = []
    if isinstance(per_chunk, int) and per_chunk > 1:
        pl.append(rarity(per_chunk))
    elif isinstance(per_chunk, tuple):
        pl.append(count(per_chunk[1]))
    pl += [IN_SQUARE, heightmap('WORLD_SURFACE_WG'), BIOME, count(tries), offset(xz, y)]
    pl.append(predicate(all_of(AIR, below) if below else AIR))
    pl.append(clear(0))
    placed(name, feat, pl)


def flower_patch(name, feat, tries, xz, per_chunk, below=None):
    """a clump of flowers: tries spread over +-xz, each one on the ground of its own column"""
    pl = [rarity(per_chunk)] if isinstance(per_chunk, int) and per_chunk > 1 else [count(per_chunk[1])] if isinstance(per_chunk, tuple) else []
    tz = OrderedDict(type='minecraft:trapezoid', max=xz, min=-xz, plateau=0)
    pl += [IN_SQUARE, count(tries), OrderedDict(type='minecraft:offset', x=tz, y=0, z=tz),
           heightmap('MOTION_BLOCKING_NO_LEAVES'), BIOME, predicate(all_of(AIR, below) if below else AIR), clear(0)]
    placed(name, feat, pl)


def sea_patch(name, feat, tries, xz=7, per_chunk=None, extra=None):
    """Things on the sea floor: tries spread around a random point of the chunk, each on the ocean floor in water."""
    pl = []
    if isinstance(per_chunk, int) and per_chunk > 1:
        pl.append(rarity(per_chunk))
    elif isinstance(per_chunk, tuple):
        pl.append(count(per_chunk[1]))
    pl += [IN_SQUARE, count(tries), offset(xz, 0), heightmap('OCEAN_FLOOR'),
           predicate(all_of(WATER, STURDY_BELOW, *(extra or []))), BIOME]
    placed(name, feat, pl)


def vanilla(rel):
    with open(os.path.join(VANILLA, 'data', 'minecraft', 'worldgen', rel + '.json'), encoding='utf-8') as f:
        return json.load(f, object_pairs_hook=OrderedDict)


ISLAND_GROUND = ['minecraft:sand', 'raft:shell_sand', 'minecraft:grass_block', 'minecraft:coarse_dirt',
                 'minecraft:podzol', 'minecraft:dirt', 'minecraft:moss_block']
BEACH_GROUND = ['minecraft:sand', 'raft:shell_sand', 'raft:black_sand', 'minecraft:gravel']


# --------------------------------------------------------------------------------------------- trees and logs
def trees():
    palm = vanilla_raft_palm()
    # palms anywhere on palm isles (the 2.0 feature only places on plain sand): grass under the trunk becomes dirt
    palm['below_trunk_provider'] = OrderedDict(type='minecraft:rule_based', rules=[OrderedDict(
        if_true=blocks('minecraft:grass_block', 'minecraft:podzol', 'minecraft:coarse_dirt'), then=st('minecraft:dirt'))],
        fallback=st('minecraft:sand'))
    f = feature('palm_tree', palm)
    on_ground = predicate(all_of(AIR, blocks(*ISLAND_GROUND, offset=(0, -1, 0))))
    placed('palm_trees', f, [count(OrderedDict(type='minecraft:weighted_list', distribution=[
        OrderedDict(data=1, weight=3), OrderedDict(data=2, weight=4), OrderedDict(data=3, weight=2)])),
        IN_SQUARE, water_depth(0), heightmap('OCEAN_FLOOR'), BIOME, on_ground, clear(4)])
    placed('palm_trees_sparse', f, [rarity(3), IN_SQUARE, water_depth(0), heightmap('OCEAN_FLOOR'), BIOME, on_ground,
                                    clear(4)])

    # bleached driftwood logs washed up on the beach
    f = feature('fallen_driftwood', OrderedDict(
        type='minecraft:fallen_tree', log_decorators=[], log_length=uniform(3, 7), stump_decorators=[],
        trunk_provider=st('driftwood_log', axis='y')))
    placed('fallen_driftwood', f, [rarity(5), IN_SQUARE, water_depth(0), heightmap('OCEAN_FLOOR'), BIOME,
                                   predicate(all_of(AIR, blocks(*BEACH_GROUND, offset=(0, -1, 0)))), clear(7)])


def vanilla_raft_palm():
    """The 2.0 palm tree (raft:palmwood, datagen output) as the base of the island palms."""
    from bclib.paths import GENERATED
    with open(os.path.join(GENERATED, 'data', 'raft', 'worldgen', 'feature', 'palmwood.json'), encoding='utf-8') as f:
        return json.load(f, object_pairs_hook=OrderedDict)


# --------------------------------------------------------------------------------------------- island plants
def island_plants():
    f = feature('beach_grass', simple(st('beach_grass')))
    land_patch('patch_beach_grass', f, 24, per_chunk=('count', 2))
    f = feature('sea_oats', simple(st('sea_oats', half='lower')))
    land_patch('patch_sea_oats', f, 14, xz=5, y=2, per_chunk=2,
               below=blocks('minecraft:sand', 'raft:shell_sand', offset=(0, -1, 0)))
    f = feature('island_grass', simple(weighted([(st('minecraft:short_grass'), 6), (st('minecraft:fern'), 1)])))
    land_patch('patch_island_grass', f, 32, per_chunk=('count', 2),
               below=blocks('minecraft:grass_block', offset=(0, -1, 0)))
    # flowers (placed before the grasses, which would otherwise take every free spot): a clump in most chunks. Every
    # try finds the ground of its own column (heightmap after the offset, leaves ignored), so the clumps also work on
    # slopes and under the jungle canopy.
    f = feature('hibiscus', simple(st('hibiscus')))
    flower_patch('patch_hibiscus', f, 10, 4, ('count', 1))
    f = feature('bird_of_paradise', simple(st('bird_of_paradise', half='lower')))
    flower_patch('patch_bird_of_paradise', f, 8, 3, ('count', 1))
    f = feature('heliconia', simple(st('heliconia', half='lower')))
    flower_patch('patch_heliconia', f, 6, 3, 2)
    for name, rar in (('plumeria', ('count', 1)), ('tropical_orchid', 2), ('spider_lily', 2), ('aloe_vera', 3)):
        flower_patch(f'patch_{name}', feature(name, simple(st(name))), 8, 3, rar)
    flower_patch('patch_beach_morning_glory', feature('beach_morning_glory', simple(st('beach_morning_glory'))), 10, 4, 2,
                 below=blocks('minecraft:sand', 'raft:shell_sand', offset=(0, -1, 0)))
    f = feature('tropical_fern', simple(st('tropical_fern')))
    land_patch('patch_tropical_fern', f, 24, xz=6, per_chunk=('count', 2))
    f = feature('pineapple_bush', simple(rand_int('age', st('pineapple_bush', age=3), 1, 3)))
    land_patch('patch_pineapple_bush', f, 8, xz=4, y=2, per_chunk=7,
               below=blocks('minecraft:grass_block', 'minecraft:sand', 'minecraft:coarse_dirt', 'minecraft:podzol',
                            offset=(0, -1, 0)))

    # seashells and sea glass on the beach (in and out of the water at the shore line)
    shells = wet_or_dry(rand_int('shells', st('seashells', shells=1, waterlogged=True), 1, 4),
                        rand_int('shells', st('seashells', shells=1, waterlogged=False), 1, 4))
    glass = wet_or_dry(rand_int('pebbles', st('sea_glass_scatter', pebbles=1, waterlogged=True), 1, 3),
                       rand_int('pebbles', st('sea_glass_scatter', pebbles=1, waterlogged=False), 1, 3))
    f = feature('seashells', simple(shells))
    g = feature('sea_glass', simple(glass))
    beach_spot = predicate(all_of(any_of(AIR, WATER), blocks(*BEACH_GROUND, offset=(0, -1, 0))))
    for name, feat, tries, rar in (('patch_seashells', f, 10, 1), ('patch_sea_glass', g, 6, 3)):
        placed(name, feat, ([rarity(rar)] if rar > 1 else []) + [
            IN_SQUARE, count(tries), offset(6, 0), heightmap('OCEAN_FLOOR'), water_depth(2), beach_spot, BIOME, clear(0)])
    sea_patch('seashells_reef', f, 6, xz=6, per_chunk=2, extra=[blocks(*BEACH_GROUND, 'minecraft:clay', offset=(0, -1, 0))])
    sea_patch('sea_glass_reef', g, 4, xz=6, per_chunk=4, extra=[blocks(*BEACH_GROUND, offset=(0, -1, 0))])

    # boulders
    for name, block, ground, rar in (
            ('boulder_andesite', 'minecraft:andesite', ISLAND_GROUND, 6),
            ('boulder_mossy', 'minecraft:mossy_cobblestone', ISLAND_GROUND, 4),
            ('boulder_pumice', 'raft:pumice', ['raft:volcanic_ash', 'raft:black_sand', 'minecraft:basalt',
                                               'minecraft:blackstone', 'minecraft:smooth_basalt'], 3)):
        f = feature(name, OrderedDict(type='minecraft:block_blob', can_place_on=blocks(*ground), state=st(block)))
        placed(name, f, [rarity(rar), IN_SQUARE, water_depth(0), heightmap('MOTION_BLOCKING'), BIOME, clear(4)])

    # volcanic ash drifts over the black sand / basalt
    # (pumice where the ash would have nothing solid below it, so it cannot fall)
    ash_or_pumice = OrderedDict(type='minecraft:rule_based', fallback=st('volcanic_ash'), rules=[OrderedDict(
        if_true=not_(OrderedDict(type='minecraft:solid', offset=[0, -1, 0])), then=st('pumice'))])
    f = feature('volcanic_ash_dunes', OrderedDict(
        type='minecraft:disk', half_height=1, radius=uniform(2, 5), state_provider=ash_or_pumice,
        target=blocks('raft:black_sand', 'minecraft:basalt', 'minecraft:blackstone', 'minecraft:smooth_basalt')))
    placed('patch_volcanic_ash_dunes', f, [count(2), IN_SQUARE, heightmap('WORLD_SURFACE_WG'), water_depth(0), BIOME,
                                           clear(6)])

    # crater lava lake of the big volcanoes (one pass per chunk)
    f = feature('crater_lava', OrderedDict(type='raft:crater_lava', floor_y=164))
    placed('crater_lava', f, [BIOME])


# --------------------------------------------------------------------------------------------- sea life
def sea_life():
    # tall kelp of the kelp forest, glow kelp patches
    kelp = OrderedDict(type='minecraft:block_column', allowed_placement=all_of(WATER, blocks('minecraft:water', offset=(0, 1, 0))),
                       direction='up', layers=[
                           OrderedDict(height=uniform(10, 40), provider=st('minecraft:kelp_plant')),
                           OrderedDict(height=1, provider=rand_int('age', st('minecraft:kelp', age=0), 20, 23))],
                       prioritize_tip=True)
    f = feature('tall_kelp', kelp)
    placed('tall_kelp', f, [OrderedDict(type='minecraft:noise_based_count', noise_factor=60.0, noise_offset=0.45,
                                        noise_to_count_ratio=70),
                            IN_SQUARE, heightmap('OCEAN_FLOOR'),
                            predicate(all_of(WATER, blocks('minecraft:water', offset=(0, 1, 0)), STURDY_BELOW,
                                             not_(tag_pred('minecraft:cannot_support_kelp', offset=(0, -1, 0))))),
                            BIOME, clear(0)])
    glow = OrderedDict(type='minecraft:block_column', allowed_placement=all_of(WATER, blocks('minecraft:water', offset=(0, 1, 0))),
                       direction='up', layers=[
                           OrderedDict(height=uniform(3, 14), provider=st('glow_kelp_plant')),
                           OrderedDict(height=1, provider=rand_int('age', st('glow_kelp', age=0), 18, 23))],
                       prioritize_tip=True)
    f = feature('glow_kelp', glow)
    sea_patch('glow_kelp_patch', f, 10, xz=5, per_chunk=2,
              extra=[blocks('minecraft:water', offset=(0, 1, 0)),
                     not_(tag_pred('minecraft:cannot_support_kelp', offset=(0, -1, 0))), clear(0)])

    # coral reef: the vanilla warm ocean reef shapes, decorated with our anemones, starfish and urchins as well
    deco = vanilla('feature/coral/block_decoration')
    sel = deco['features'][0]['feature']['features']
    wet = dict(waterlogged=True)
    sel.extend([
        OrderedDict(data=OrderedDict(feature=simple(weighted([(st('sea_anemone_red', **wet), 2),
                                                              (st('sea_anemone_purple', **wet), 2)])),
                                     placement=[]), weight=6),
        OrderedDict(data=OrderedDict(feature=simple(starfish_provider()), placement=[]), weight=2),
        OrderedDict(data=OrderedDict(feature=simple(st('sea_urchin', **wet)), placement=[]), weight=1),
    ])
    feature('coral/reef_decoration', deco)
    for kind in ('tube', 'brain', 'bubble', 'fire', 'horn'):
        blk = vanilla(f'feature/coral/{kind}_block')
        blk['features'][1]['feature'] = 'raft:coral/reef_decoration'
        feature(f'coral/{kind}_block', blk)
    reef = vanilla('feature/warm_ocean_vegetation')
    text = json.dumps(reef).replace('"minecraft:coral/', '"raft:coral/')
    f = feature('coral_reef', json.loads(text, object_pairs_hook=OrderedDict))
    placed('coral_reef', f, [OrderedDict(type='minecraft:noise_based_count', noise_factor=400.0, noise_offset=0.1,
                                         noise_to_count_ratio=22), IN_SQUARE, heightmap('OCEAN_FLOOR_WG'), BIOME,
                             clear(6)])
    # rare tube coral heads on the open sea floor
    tube = vanilla('feature/warm_ocean_vegetation')
    tube['features'] = [e for e in tube['features'] if 'tube_block' in json.dumps(e)]
    f = feature('tube_coral', tube)
    placed('rare_tube_coral', f, [rarity(10), IN_SQUARE, heightmap('OCEAN_FLOOR_WG'), BIOME, clear(4)])

    # anemones, glowing anemones, tube worms, clams, starfish, urchins
    f = feature('anemones', simple(weighted([(st('sea_anemone_red', **wet), 1), (st('sea_anemone_purple', **wet), 1)])))
    sea_patch('anemones', f, 14, xz=6, per_chunk=('count', 2))
    f = feature('glow_anemone', simple(st('sea_anemone_glow', **wet)))
    sea_patch('glow_anemones', f, 12, xz=6, per_chunk=('count', 2))
    sea_patch('glow_anemones_sparse', f, 6, xz=4, per_chunk=3)
    f = feature('tube_worms', simple(st('tube_worms', **wet)))
    sea_patch('tube_worms', f, 18, xz=5, per_chunk=('count', 3))
    sea_patch('tube_worms_sparse', f, 10, xz=4, per_chunk=2)
    clams = []
    for facing in ('north', 'east', 'south', 'west'):
        clams.append((st('giant_clam', facing=facing, open=False, pearl=True, waterlogged=True), 1))
        clams.append((st('giant_clam', facing=facing, open=False, pearl=False, waterlogged=True), 2))
    f = feature('giant_clam', simple(weighted(clams)))
    sea_patch('giant_clams', f, 3, xz=6, per_chunk=3)
    f = feature('starfish', simple(starfish_provider()))
    sea_patch('starfish', f, 5, xz=7, per_chunk=('count', 1))
    f = feature('sea_urchin', simple(st('sea_urchin', **wet)))
    sea_patch('sea_urchins', f, 6, xz=5, per_chunk=2)

    # barnacles on rocks, stone walls and the wood of wrecks
    f = feature('barnacles', OrderedDict(
        type='minecraft:multiface_growth', block='raft:barnacles', can_be_placed_on=[
            'minecraft:stone', 'minecraft:andesite', 'minecraft:tuff', 'minecraft:basalt', 'minecraft:smooth_basalt',
            'minecraft:cobblestone', 'minecraft:mossy_cobblestone', 'minecraft:deepslate', 'minecraft:sandstone',
            'minecraft:oak_planks', 'minecraft:spruce_planks', 'minecraft:dark_oak_planks', 'minecraft:oak_log',
            'minecraft:spruce_log', 'minecraft:dark_oak_log', 'minecraft:stripped_oak_log',
            'minecraft:stripped_spruce_log', 'minecraft:stripped_dark_oak_log', 'raft:driftwood_log',
            'raft:driftwood_planks', 'raft:driftwood_wood'],
        can_place_on_ceiling=True, can_place_on_floor=True, can_place_on_wall=True, chance_of_spreading=0.6,
        search_range=6))
    placed('barnacles', f, [count(10), IN_SQUARE, heightmap('OCEAN_FLOOR_WG'), offset(0, 2), BIOME])

    # bubble vents (with their bubble columns) in small clusters
    f = feature('bubble_vents', OrderedDict(type='raft:bubble_vents', count=uniform(1, 3), spread=2,
                                            vent='raft:bubble_vent'))
    placed('bubble_vents', f, [rarity(6), IN_SQUARE, BIOME, clear(3)])
    placed('bubble_vents_sparse', f, [rarity(18), IN_SQUARE, BIOME, clear(3)])

    # rubble of the drowned city and wreck debris
    rubble = OrderedDict(type='raft:rubble', optional_blocks=['raft:cracked_concrete', 'raft:mossy_concrete',
                                                              'raft:rebar_concrete', 'raft:rusted_plate'],
                         optional_chance=0.85, pieces=uniform(3, 9), spread=3, stack_chance=0.35,
                         state_provider=weighted([(st('minecraft:cobblestone'), 3), (st('minecraft:mossy_cobblestone'), 2),
                                                  (st('minecraft:andesite'), 2), (st('minecraft:cracked_stone_bricks'), 2),
                                                  (st('minecraft:iron_bars', waterlogged=True), 1)]))
    f = feature('concrete_rubble', rubble)
    placed('concrete_rubble', f, [count(OrderedDict(type='minecraft:weighted_list', distribution=[
        OrderedDict(data=0, weight=1), OrderedDict(data=1, weight=2), OrderedDict(data=2, weight=1)])),
        IN_SQUARE, BIOME, clear(4)])
    debris = OrderedDict(type='raft:rubble', optional_blocks=['raft:weathered_planks', 'raft:teak_planks',
                                                              'raft:rusty_hull', 'raft:riveted_hull'],
                         optional_chance=0.35, pieces=uniform(2, 7), spread=3, stack_chance=0.25,
                         state_provider=weighted([(st('driftwood_log', axis='x'), 3), (st('driftwood_planks'), 3),
                                                  (st('minecraft:spruce_planks'), 2), (st('minecraft:dark_oak_planks'), 1),
                                                  (st('minecraft:stripped_spruce_log', axis='x'), 2),
                                                  (st('minecraft:barrel', facing='up'), 1),
                                                  (st('minecraft:iron_chain', axis='x', waterlogged=True), 1)]))
    f = feature('wreck_debris', debris)
    placed('wreck_debris', f, [rarity(2), IN_SQUARE, BIOME, clear(4)])

    # ice floes and snow on the cold sea
    f = feature('ice_floe', OrderedDict(type='raft:ice_floe', radius=uniform(3, 8), ridge_chance=0.3))
    placed('ice_floe', f, [count(uniform(0, 2)), IN_SQUARE, BIOME, clear(9)])


def ground_support():
    """Last feature of every biome: sand / gravel / ash right above a cave becomes solid (see GravitySupportFeature)."""
    f = feature('gravity_support', OrderedDict(type='raft:gravity_support'))
    placed('gravity_support', f, [])


def starfish_provider():
    entries = []
    for colour in ('orange', 'purple', 'red'):
        for facing in ('north', 'east', 'south', 'west'):
            entries.append((st(f'starfish_{colour}', face='floor', facing=facing, waterlogged=True), 1))
    return weighted(entries)


def write(module):
    from bclib import assets
    FEATURES.clear()
    PLACED.clear()
    trees()
    island_plants()
    sea_life()
    ground_support()
    guarded_vanilla()
    for name, data in FEATURES.items():
        assets.write_json(module, f'data/raft/worldgen/feature/{name}.json', data)
    for name, data in PLACED.items():
        assets.write_json(module, f'data/raft/worldgen/placed_feature/{name}.json', data)
