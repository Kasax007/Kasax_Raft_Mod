"""abandoned_raft - rafts of castaways who did not make it, drifting on the open sea (deck of raft_foundation at
y 99 replacing the water surface; fixed start height, the template's y=1 is the deck).
 0 survivor: compact raft with mast and patched sail, crop plots, purifier, grill, hammock with a skeleton
 1 homestead: big two-level raft with a thatched hut, lookout deck, crop rows, scarecrow, rain barrel
 2 wrecked: raft torn into three drifting pieces by a shark, sail in the water, floating barrels and debris
"""
import random

from .lib import Side, NO_SPILL_PLANTS, contact, pick, save, write_structure

DECK = 1          # template y of the foundation layer (= world y 99)
FOUND = [('raft:raft_foundation', 10), ('raft:reinforced_foundation', 1)]
PLANK = [('raft:palm_planks', 5), ('raft:driftwood_planks', 4), ('raft:weathered_planks', 2)]


def deck(s, rnd, cells, missing=0.0):
    for (x, z) in cells:
        if rnd.random() < missing:
            continue
        s.set(x, DECK, z, pick(rnd, FOUND))
        if rnd.random() < 0.18:   # barnacles under the raft
            s.set(x, DECK - 1, z, 'raft:barnacles[up=true,down=false,north=false,south=false,east=false,west=false]')


def rect(x0, z0, x1, z1):
    return [(x, z) for x in range(x0, x1 + 1) for z in range(z0, z1 + 1)]


def floats(s, rnd, cells, n):
    for (x, z) in rnd.sample(cells, n):
        s.set(x, DECK - 1, z, 'minecraft:barrel[facing=up,open=false]')


def mast(s, rnd, x, z, h, sail_w, block='raft:sail_canvas', torn=0.15, facing='south'):
    y0 = DECK + 1
    for y in range(y0, y0 + h):
        s.set(x, y, z, 'raft:palm_log[axis=y]')
    yard = y0 + h - 1
    for dx in range(-sail_w, sail_w + 1):
        if dx:
            s.set(x + dx, yard, z, 'raft:stripped_palm_log[axis=x]')
    dz = 1 if facing == 'south' else -1
    for dx in range(-sail_w + 1, sail_w):
        bottom = y0 + 2 + rnd.randint(0, 1)
        for y in range(bottom, yard):
            if rnd.random() > torn:
                s.set(x + dx, y, z + dz, block if rnd.random() < 0.85 else 'raft:sail_canvas_red_striped')
    s.set(x, yard + 1, z, f'raft:raft_flag[facing={"east" if facing == "south" else "west"}]')


def rail(s, rnd, cells, gap=0.25):
    cs = set(cells)
    for (x, z) in cells:
        edge = any((x + dx, z + dz) not in cs for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        if edge and s.get(x, DECK + 1, z) is None and rnd.random() > gap:
            s.set(x, DECK + 1, z, 'raft:rope_railing')


def crops(s, rnd, cells):
    for (x, z) in cells:
        s.set(x, DECK + 1, z, 'raft:crop_plot')
        s.set(x, DECK + 2, z, pick(rnd, [('raft:pineapple_crop[age=5]', 2), ('minecraft:potatoes[age=6]', 2),
                                         ('minecraft:wheat[age=4]', 1), ('minecraft:carrots[age=7]', 1),
                                         ('minecraft:dead_bush', 1)]))


def survivor(rnd):
    s = Side('survivor')
    cells = rect(0, 0, 4, 5) + [(5, 2), (5, 3), (-1, 2)]
    deck(s, rnd, cells)
    floats(s, rnd, cells, 4)
    for (x, z) in rect(1, 1, 3, 2):
        s.set(x, DECK + 1, z, 'raft:palm_slab[type=bottom]')
    mast(s, rnd, 2, 3, 8, 3)
    crops(s, rnd, [(0, 0), (1, 0), (0, 1)])
    s.set(4, DECK + 1, 0, 'raft:water_purifier[facing=west,lit=false]')
    s.set(4, DECK + 1, 1, 'raft:simple_grill[lit=false]')
    s.chest(0, DECK + 1, 5, 'raft:chests/abandoned_raft', facing='east')
    s.set(1, DECK + 1, 5, 'raft:rope_coil')
    s.set(3, DECK + 1, 5, 'raft:hammock[facing=east,occupied=false,part=foot]')
    s.set(4, DECK + 1, 5, 'raft:hammock[facing=east,occupied=false,part=head]')
    s.set(4, DECK + 2, 5, 'minecraft:skeleton_skull[rotation=4]')
    s.set(5, DECK + 1, 2, 'raft:item_catcher[has_net=true]')
    s.set(0, DECK + 1, 3, 'raft:coconut_lantern[hanging=false]')
    s.set(-1, DECK + 1, 2, 'minecraft:bone_block[axis=z]')
    rail(s, rnd, cells, gap=0.5)
    return s


def homestead(rnd):
    s = Side('homestead')
    cells = rect(0, 0, 7, 7) + rect(8, 2, 9, 5) + rect(-2, 3, -1, 6)
    deck(s, rnd, cells, missing=0.04)
    floats(s, rnd, cells, 6)
    # hut 4x4 with thatched roof, lookout on top
    for x in range(0, 4):
        for z in range(0, 4):
            s.set(x, DECK + 1, z, pick(rnd, PLANK))
            wall = x in (0, 3) or z in (0, 3)
            for y in (DECK + 2, DECK + 3):
                if wall:
                    corner = x in (0, 3) and z in (0, 3)
                    if corner:
                        s.set(x, y, z, 'raft:palm_log[axis=y]')
                    elif z == 3 and x in (1, 2) and y == DECK + 2:
                        s.set(x, y, z, 'minecraft:air' if x == 1 else 'raft:palm_planks')
                    elif y == DECK + 3 and (x == 0 or x == 3) and z in (1, 2):
                        s.set(x, y, z, 'minecraft:air')
                    else:
                        s.set(x, y, z, pick(rnd, PLANK))
                else:
                    s.set(x, y, z, 'minecraft:air')
    s.set(1, DECK + 3, 3, 'minecraft:air')
    for x in range(-1, 5):
        for z in range(-1, 5):
            s.set(x, DECK + 4, z, 'raft:palm_thatch_slab[type=bottom]' if x in (-1, 4) or z in (-1, 4)
                  else 'raft:palm_thatch')
    for x in range(0, 4):
        for z in range(0, 4):
            if x in (0, 3) or z in (0, 3):
                s.set(x, DECK + 5, z, 'raft:rope_railing' if rnd.random() < 0.8 else None)
    s.set(1, DECK + 5, 1, 'raft:spyglass_stand[facing=east]')
    for y in range(DECK + 2, DECK + 5):
        s.set(4, y, 2, 'raft:rope_ladder[facing=east]')
    s.set(4, DECK + 4, 2, 'raft:palm_thatch_slab[type=bottom]')
    s.set(4, DECK + 4, 2, 'minecraft:air')
    s.set(1, DECK + 2, 1, 'raft:hammock[facing=east,occupied=false,part=foot]')
    s.set(2, DECK + 2, 1, 'raft:hammock[facing=east,occupied=false,part=head]')
    s.chest(2, DECK + 2, 2, 'raft:chests/abandoned_raft', facing='north')
    s.set(1, DECK + 2, 2, 'raft:driftwood_table')
    s.set(1, DECK + 3, 2, 'raft:coconut_lantern[hanging=false]')
    # crop rows, scarecrow, purifier, rain barrel
    crops(s, rnd, [(x, z) for x in range(5, 8) for z in (0, 1)] + [(6, 3), (7, 3)])
    s.set(5, DECK + 1, 4, 'raft:scarecrow[facing=south,half=lower]')
    s.set(5, DECK + 2, 4, 'raft:scarecrow[facing=south,half=upper]')
    s.set(7, DECK + 1, 6, 'minecraft:water_cauldron[level=2]')
    s.set(6, DECK + 1, 6, 'raft:water_purifier[facing=north,lit=false]')
    s.set(5, DECK + 1, 7, 'raft:simple_grill[lit=false]')
    s.set(9, DECK + 1, 3, 'raft:item_catcher[has_net=true]')
    s.set(9, DECK + 1, 4, 'raft:item_catcher[has_net=false]')
    mast(s, rnd, -1, 5, 6, 1, torn=0.3, facing='north')
    s.set(3, DECK + 1, 6, 'minecraft:barrel[facing=up,open=false]')
    s.set(3, DECK + 2, 6, 'raft:coconut_lantern[hanging=false]')
    rail(s, rnd, cells, gap=0.35)
    return s


def wrecked(rnd):
    s = Side('wrecked')
    pieces = [(rect(0, 0, 3, 3) + [(4, 1)], (0, 0)), (rect(0, 0, 2, 1), (7, 2)), (rect(0, 0, 1, 2), (2, 7))]
    allc = []
    for cells, (ox, oz) in pieces:
        cs = [(x + ox, z + oz) for x, z in cells]
        deck(s, rnd, cs, missing=0.1)
        floats(s, rnd, cs, 1)
        allc += cs
    s.chest(1, DECK + 1, 1, 'raft:chests/abandoned_raft', facing='south')
    s.set(2, DECK + 1, 2, 'raft:crop_plot')
    s.set(2, DECK + 2, 2, 'minecraft:dead_bush')
    s.set(3, DECK + 1, 0, 'raft:rope_coil')
    s.set(0, DECK + 1, 3, 'minecraft:skeleton_skull[rotation=10]')
    s.set(1, DECK + 1, 3, 'minecraft:bone_block[axis=x]')
    s.set(8, DECK + 1, 3, 'raft:water_purifier[facing=south,lit=false]')
    s.set(2, DECK + 1, 8, 'raft:simple_grill[lit=false]')
    # snapped mast and the sail floating in the water
    s.set(3, DECK + 1, 3, 'raft:palm_log[axis=y]')
    s.set(3, DECK + 2, 3, 'raft:palm_fence')
    for z in range(4, 10):
        s.set(5, DECK, z, 'raft:palm_log[axis=z]')
    for x in range(6, 9):
        for z in range(5, 10):
            if rnd.random() < 0.7:
                s.set(x, DECK, z, 'raft:sail_canvas' if rnd.random() < 0.8 else 'raft:sail_canvas_red_striped')
    for (x, z) in ((-2, 5), (6, -2), (10, 7), (-1, 9)):
        s.set(x, DECK, z, pick(rnd, [('minecraft:barrel[facing=north,open=false]', 2),
                                      ('raft:wooden_crate[facing=east,open=false]', 1),
                                      ('raft:palm_planks', 1)]))
    return s


BUILDERS = [survivor, homestead, wrecked]


def main(preview=False):
    out = []
    for v, fn in enumerate(BUILDERS):
        rnd = random.Random(31415909 + v)
        s = fn(rnd)
        path = f'abandoned_raft/{fn.__name__}'
        save(s, path)
        out.append((path, s))
        if preview:
            print('  preview', contact(s, f'abandoned_raft_{fn.__name__}', scale=8, ne=True))
    write_structure('abandoned_raft', [(p, 1) for p, _ in out], [NO_SPILL_PLANTS],
                    step='surface_structures', terrain='none', start_height=98, heightmap=None,
                    biomes=['raft_ocean_biome', 'kelp_forest', 'coral_shallows'],
                    spacing=20, separation=8, salt=31415909, max_distance=32)
    return out
