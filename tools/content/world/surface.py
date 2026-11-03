"""Surface (material) rules of the raft dimension: data/raft/worldgen/material_rule/raftdim.json.

Per biome: beaches of sand / shell sand, black sand on volcanic isles, grass and dirt inland, mud in mangroves,
volcanic ash / basalt / blackstone / pumice on volcanic isles, sea floors of sand, gravel, clay and mud.

Speed: the game evaluates this rule for every solid block of every column (top down to the bottom of the world), so
the order matters (profiled: the rule was ~40 % of the world generation time). Cheap checks come first and guard the
expensive ones:
- the biome lookup (a fuzzy 3D lookup per block) only for blocks that can get a surface material at all: above the
  preliminary surface AND within the deepest surface layer (deep_under_floor); every biome rule only uses on_floor /
  under_floor / deep_under_floor layers, which all lie inside that one;
- one biome switch, the most common biomes first; no rules that only put stone on stone.
"""
from collections import OrderedDict

from content.world import terrain

# own natural blocks (module world, section 4.3)
B = {
    'shell_sand': 'raft:shell_sand', 'black_sand': 'raft:black_sand', 'volcanic_ash': 'raft:volcanic_ash',
    'pumice': 'raft:pumice',
}

PATCH = 'raft:raftdim/surface_patch'          # small blotches (16-32 blocks)
PATCH_L = 'raft:raftdim/surface_patch_large'  # larger areas (64-128 blocks)
NOISES = {
    'surface_patch': terrain._octaves(-5, [1.0, 0.5]),
    'surface_patch_large': terrain._octaves(-7, [1.0, 0.5, 0.25]),
}


def block(state, **props):
    if props:
        return OrderedDict(type='minecraft:block', result_state=OrderedDict(id=state, properties=props))
    return OrderedDict(type='minecraft:block', result_state=state)


def cond(c, then):
    return OrderedDict(type='minecraft:condition', if_true=c, then_run=then)


def seq(*rules):
    return OrderedDict(type='minecraft:sequence', sequence=list(rules))


def biome(*ids):
    return OrderedDict(type='minecraft:biome', biome_is=['raft:' + i for i in ids])


def noise(n, lo, hi=1.0e9):
    return OrderedDict(type='minecraft:noise_threshold', noise=n, min_threshold=lo, max_threshold=hi)


def y_above(y, mult=0, add_stone_depth=False):
    return OrderedDict(type='minecraft:y_above', anchor={'absolute': y}, surface_depth_multiplier=mult,
                       add_stone_depth=add_stone_depth)


def not_(c):
    return OrderedDict(type='minecraft:not', invert=c)


ON_FLOOR = 'minecraft:on_floor'                  # the top block
UNDER_FLOOR = 'minecraft:under_floor'            # top 3-6 blocks (surface depth)
DEEP_UNDER_FLOOR = 'minecraft:deep_under_floor'  # a few more below
ON_CEILING = 'minecraft:on_ceiling'
ABOVE_WATER = 'minecraft:not_underwater'          # dry, or at most one block below the water surface
SAND = block('minecraft:sand')
SANDSTONE = block('minecraft:sandstone')
GRAVEL = block('minecraft:gravel')
CLAY = block('minecraft:clay')
MUD = block('minecraft:mud')
DIRT = block('minecraft:dirt')
GRASS = block('minecraft:grass_block', snowy='false')
STONE = block('minecraft:stone')
BASALT = block('minecraft:basalt', axis='y')
SMOOTH_BASALT = block('minecraft:smooth_basalt')
BLACKSTONE = block('minecraft:blackstone')
MAGMA = block('minecraft:magma_block')
ANDESITE = block('minecraft:andesite')
TUFF = block('minecraft:tuff')
COARSE = block('minecraft:coarse_dirt')
PODZOL = block('minecraft:podzol', snowy='false')
MOSS = block('minecraft:moss_block')
SAND_SAFE = seq(cond(ON_CEILING, SANDSTONE), SAND)       # no floating sand above caves


def b(name, **props):
    return block(B[name], **props)


def beach_line(top=99, mult=1):
    """True below the (wobbly) beach line: y < top + surface depth."""
    return not_(y_above(top, mult))


def island_rules():
    shell = b('shell_sand')
    black = b('black_sand')
    ash = b('volcanic_ash')
    pumice = b('pumice')
    beach_top = seq(cond(noise(PATCH, 0.28), shell), SAND_SAFE)
    palm = seq(
        cond(ON_FLOOR, seq(cond(beach_line(), beach_top),
                           cond(ABOVE_WATER, seq(cond(noise(PATCH, 0.45), COARSE), GRASS)),
                           SAND_SAFE)),
        cond(UNDER_FLOOR, seq(cond(beach_line(98, 1), SAND_SAFE), DIRT)),
        cond(beach_line(97, 1), SANDSTONE))   # deep_under_floor (the gate of the whole surface)
    jungle = seq(
        cond(ON_FLOOR, seq(cond(beach_line(98, 1), beach_top),
                           cond(ABOVE_WATER, seq(cond(noise(PATCH, 0.38), PODZOL),
                                                 cond(noise(PATCH, -1e9, -0.5), MOSS), GRASS)),
                           SAND_SAFE)),
        cond(UNDER_FLOOR, seq(cond(beach_line(97, 1), SAND_SAFE), DIRT)),
        cond(beach_line(96, 1), SANDSTONE))
    mangrove = seq(
        cond(ON_FLOOR, seq(cond(not_(y_above(101)), MUD),
                           cond(ABOVE_WATER, seq(cond(noise(PATCH, 0.2), MUD), GRASS)),
                           MUD)),
        cond(UNDER_FLOOR, MUD),
        seq(cond(noise(PATCH_L, 0.0), CLAY), MUD))
    # volcanic: black sand beach, ash fields with basalt / blackstone / pumice outcrops, basalt peaks with magma
    volc_inland = seq(
        cond(y_above(150, 1), seq(cond(noise(PATCH, 0.3), MAGMA), cond(noise(PATCH, -0.15, 0.3), BLACKSTONE),
                                  BASALT)),
        cond(noise(PATCH, 0.32), BASALT),
        cond(noise(PATCH, -1e9, -0.38), pumice),
        cond(noise(PATCH_L, 0.25), BLACKSTONE),
        ash)
    volcanic = seq(
        cond(ON_FLOOR, seq(cond(beach_line(), seq(cond(ON_CEILING, BASALT), black)),
                           cond(ABOVE_WATER, volc_inland),
                           black)),
        cond(UNDER_FLOOR, seq(cond(beach_line(98, 1), seq(cond(ON_CEILING, BASALT), black)),
                              cond(y_above(150, 1), BASALT),
                              seq(cond(noise(PATCH_L, 0.3), BLACKSTONE), cond(ON_CEILING, BASALT), ash))),
        seq(cond(noise(PATCH_L, 0.1), BLACKSTONE), BASALT))
    return {'palm_isle': palm, 'jungle_isle': jungle, 'mangrove_isle': mangrove, 'volcanic_isle': volcanic}


def floor(top, under=None, deep=None):
    """Sea floor: top (rule for the top block), under (next blocks), deep (below that; no condition needed, the
    whole surface is gated by deep_under_floor). Stone there is left out: the block is stone already."""
    rules = [cond(ON_FLOOR, top)]
    if under is not None:
        rules.append(cond(UNDER_FLOOR, under))
    if deep is not None and deep is not STONE:
        rules.append(deep)
    return seq(*rules)


def ocean_rules():
    sand_floor = floor(seq(cond(noise(PATCH, 0.36), GRAVEL), SAND_SAFE), SAND_SAFE, SANDSTONE)
    kelp = floor(seq(cond(noise(PATCH, 0.3), ANDESITE), cond(noise(PATCH, -0.05, 0.3), GRAVEL),
                     cond(noise(PATCH_L, 0.2), CLAY), SAND_SAFE),
                 seq(cond(noise(PATCH, -0.05), GRAVEL), SAND_SAFE), STONE)
    coral = floor(seq(cond(noise(PATCH, 0.42), GRAVEL), SAND_SAFE), SAND_SAFE, SANDSTONE)
    lagoon = floor(seq(cond(noise(PATCH, 0.25), CLAY), cond(noise(PATCH, -1e9, -0.45), GRAVEL), SAND_SAFE),
                   SAND_SAFE, SANDSTONE)
    shelf = floor(seq(cond(noise(PATCH, 0.3), CLAY), cond(noise(PATCH, -1e9, -0.3), MUD),
                      cond(noise(PATCH_L, 0.1), SAND_SAFE), GRAVEL),
                  seq(cond(noise(PATCH_L, 0.1), SAND_SAFE), GRAVEL), STONE)
    trench = floor(seq(cond(noise(PATCH, 0.3), BASALT), cond(noise(PATCH, -1e9, -0.3), TUFF),
                       cond(noise(PATCH_L, 0.0), SAND_SAFE), GRAVEL),
                   seq(cond(noise(PATCH_L, 0.0), SAND_SAFE), GRAVEL), None)
    wreck = floor(seq(cond(noise(PATCH, 0.15), GRAVEL), cond(noise(PATCH, -1e9, -0.4), CLAY), SAND_SAFE),
                  seq(cond(noise(PATCH, 0.15), GRAVEL), SAND_SAFE), SANDSTONE)
    frost = floor(seq(cond(noise(PATCH, 0.1), GRAVEL), cond(noise(PATCH_L, 0.35), CLAY), SAND_SAFE),
                  seq(cond(noise(PATCH, 0.1), GRAVEL), SAND_SAFE), STONE)
    return {'raft_ocean_biome': sand_floor, 'kelp_forest': kelp, 'coral_shallows': coral, 'glowing_lagoon': lagoon,
            'drowned_shelf': shelf, 'abyssal_trench': trench, 'wreck_graveyard': wreck, 'frost_floes': frost}


# biome switch order: the most common biomes first (shares of the area, spec 4.1/4.2)
BIOME_ORDER = ['raft_ocean_biome', 'kelp_forest', 'drowned_shelf', 'coral_shallows', 'abyssal_trench', 'frost_floes',
               'wreck_graveyard', 'palm_isle', 'jungle_isle', 'glowing_lagoon', 'mangrove_isle', 'volcanic_isle']


def material_rule():
    """The vanilla order (bedrock, copper vein, iron vein, surface, deepslate), with the surface part gated so that
    the biome is only looked up for blocks that can get a surface material. The deep part stays flat on purpose:
    extra height checks in front of the ore veins cost more than the vein lookups they would skip (counted with
    an evaluation model of real columns: 16 vs 10 evaluations per block)."""
    rules = dict(ocean_rules(), **island_rules())
    assert sorted(rules) == sorted(BIOME_ORDER)
    switch = seq(*[cond(biome(b), rules[b]) for b in BIOME_ORDER])
    return seq(
        'minecraft:bedrock_floor',
        'minecraft:overworld/copper_ore_vein',
        'minecraft:overworld/iron_ore_vein',
        cond(OrderedDict(type='minecraft:above_preliminary_surface'), cond(DEEP_UNDER_FLOOR, switch)),
        cond(OrderedDict(type='minecraft:vertical_gradient', random_name='minecraft:deepslate',
                         true_at_and_below={'absolute': 0}, false_at_and_above={'absolute': 8}),
             block('minecraft:deepslate', axis='y')))


def write(module):
    from bclib import assets
    for name, params in NOISES.items():
        assets.write_json(module, f'data/raft/worldgen/noise/raftdim/{name}.json', params)
    assets.write_json(module, 'data/raft/worldgen/material_rule/raftdim.json', material_rule())
