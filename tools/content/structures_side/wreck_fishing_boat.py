"""wreck_fishing_boat - three small fishing boat wrecks (different shapes, not just materials):
 0 trawler: steel-and-wood trawler with wheelhouse and A-frame gantry, lying on its side, nets spilling out
 1 sloop: open wooden fishing boat broken in two, mast and sail fallen onto the sand, fish crates and rope
 2 capsized: painted crab boat upside down, keel up, traps (crates) and buoys around, a hole into the hull
"""
import math
import random

from .lib import (Side, NO_SPILL_PLANTS, contact, overgrow, pick, resample, save, silt, write_structure)


def hull(s, rnd, L, hw, depth, paint, keel_rise=2, stern=0.8, wood='minecraft:spruce_planks', deck=True,
         top_paint='minecraft:white_concrete', floor_block='minecraft:spruce_planks'):
    """Small hull along z (stern 0 -> bow L-1), centre line x=0, keel y=0, gunwale y=depth.
    paint: list of (y_max, palette) bands from the keel up. Returns inside(x, y, z)."""
    def plan(z):
        t = z / (L - 1)
        if t < 0.15:
            return stern + (1 - stern) * t / 0.15
        if t < 0.55:
            return 1.0
        u = (t - 0.55) / 0.45
        return max(0.0, math.cos(u * math.pi / 2)) ** 0.7

    def keel(z):
        t = z / (L - 1)
        return round(max(0, t - 0.75) / 0.25 * keel_rise)

    def inside(x, y, z):
        if z < 0 or z >= L or y < keel(z) or y > depth:
            return False
        h = y - keel(z)
        f = min(1.0, (h + 0.7) / max(2.0, depth * 0.55)) ** 0.6
        return abs(x) <= hw * plan(z) * f + 0.5

    for z in range(L):
        for y in range(depth + 1):
            for x in range(-int(hw) - 2, int(hw) + 3):
                if not inside(x, y, z):
                    continue
                shell = (not inside(x - 1, y, z) or not inside(x + 1, y, z) or not inside(x, y, z - 1)
                         or not inside(x, y, z + 1) or not inside(x, y - 1, z))
                if shell:
                    if y == depth:
                        st = top_paint
                    else:
                        st = wood
                        for ymax, pal in paint:
                            if y <= ymax:
                                st = pick(rnd, pal)
                                break
                    s.set(x, y, z, st)
                elif y == 1 and deck:
                    s.set(x, y, z, floor_block)
                else:
                    s.set(x, y, z, 'minecraft:water')
    return inside


def net(s, rnd, x0, y0, z0, x1, z1, chance=0.6):
    for x in range(min(x0, x1), max(x0, x1) + 1):
        for z in range(min(z0, z1), max(z0, z1) + 1):
            if rnd.random() < chance and s.get(x, y0, z) in (None, 'minecraft:water'):
                s.set(x, y0, z, 'minecraft:cobweb')


def normalise(s):
    (x0, y0, z0), _ = s.bounds()
    t = Side(s.name, seed=1)
    for (x, y, z), b in s.blocks.items():
        t.blocks[(x - x0, y - y0, z - z0)] = b
    t.entities = [(x - x0, y - y0, z - z0, n) for (x, y, z, n) in s.entities]
    return t


# ----------------------------------------------------------------------------------------------------- trawler
def trawler(rnd):
    s = Side('trawler')
    L, D = 17, 5
    paint = [(1, [('minecraft:red_terracotta', 5), ('raft:rusty_hull', 2)]),
             (3, [('minecraft:blue_terracotta', 6), ('raft:rusty_hull', 1)]),
             (4, [('minecraft:white_terracotta', 5), ('minecraft:light_gray_terracotta', 1)])]
    inside = hull(s, rnd, L, 3.4, D, paint, keel_rise=2, stern=0.85, wood='raft:riveted_hull',
                  top_paint='raft:weathered_planks')
    for z in range(1, L - 1):                       # deck with hatch
        for x in range(-3, 4):
            if inside(x, D - 1, z) and s.get(x, D - 1, z) == 'minecraft:water':
                s.set(x, D - 1, z, pick(rnd, [('raft:weathered_planks', 5), ('minecraft:spruce_planks', 2)]))
    for x in (-1, 0, 1):
        for z in (9, 10):
            s.set(x, D - 1, z, 'minecraft:water')
    s.set(0, D - 1, 9, 'minecraft:spruce_trapdoor[facing=north,half=top,open=true]')
    # wheelhouse
    for z in range(2, 7):
        for x in range(-2, 3):
            for y in range(D, D + 4):
                wall = abs(x) == 2 or z in (2, 6)
                if y == D + 3:
                    s.set(x, y, z, 'minecraft:white_concrete' if abs(x) < 2 and 2 < z < 6 else 'raft:rusty_hull')
                elif wall:
                    win = y == D + 2 and (abs(x) < 2 or z in (3, 5))
                    s.set(x, y, z, pick(rnd, [('minecraft:glass_pane', 1), ('raft:cracked_glass_pane', 2),
                                              ('minecraft:water', 2)]) if win else pick(
                        rnd, [('minecraft:white_concrete', 5), ('minecraft:light_gray_concrete', 1)]))
                else:
                    s.set(x, y, z, 'minecraft:water')
    s.set(0, D, 6, 'minecraft:water')
    s.set(0, D + 1, 6, 'minecraft:water')
    s.set(0, D, 5, 'raft:control_panel[facing=south]')
    s.set(-1, D, 3, 'raft:wooden_crate[facing=east,open=false]')
    s.chest(1, D, 3, 'raft:chests/wreck_fishing', facing='west', block='minecraft:barrel')
    s.set(0, D + 2, 4, 'minecraft:lantern[hanging=true]')
    s.set(2, D + 1, 4, 'raft:life_ring[facing=east]')
    # A-frame gantry at the stern + net drum
    for x in (-3, 3):
        s.line((x, D, 0), (x // 3 * 1, D + 6, 0), 'raft:steel_beam[axis=y]')
    for x in range(-1, 2):
        s.set(x, D + 6, 0, 'raft:steel_beam[axis=x]')
    s.set(0, D + 5, 0, 'minecraft:iron_chain[axis=y]')
    for x in range(-2, 3):
        s.set(x, D, 8, 'minecraft:stripped_dark_oak_log[axis=x]')
        s.set(x, D + 1, 8, 'minecraft:cobweb' if rnd.random() < 0.7 else 'minecraft:water')
    # mast with boom
    for y in range(D, D + 7):
        s.set(0, y, 12, 'raft:steel_beam[axis=y]')
    s.line((0, D + 5, 12), (0, D + 2, 16), 'raft:steel_beam[axis=z]')
    s.set(0, D + 7, 12, 'minecraft:lantern[hanging=false]')
    for z in range(11, 15):
        s.set(3 if z % 2 else -3, D, z, 'raft:wooden_crate[facing=north,open=false]')
    s.set(-2, D, 14, 'raft:rope_coil')
    s.set(2, D, 15, 'minecraft:barrel[facing=up,open=false]')
    # below deck: fish hold
    s.set(-1, 1, 10, 'minecraft:barrel[facing=up,open=false]')
    s.set(1, 1, 11, 'raft:wooden_crate[facing=north,open=false]')
    s.set(0, 2, 7, 'minecraft:lantern[hanging=false]')
    s = resample(s, roll=72, pitch=-6, pivot=(0, 0, L // 2))
    s = normalise(s)
    (x0, y0, z0), (x1, y1, z1) = s.bounds()
    silt(s, 2, depth=2, seed=21)
    net(s, rnd, x1 + 1, 3, 4, x1 + 6, 12, 0.55)        # net spilled over the sand
    s.set(x1 + 5, 3, 13, 'raft:buoy_red')
    s.set(x1 + 3, 3, 9, 'minecraft:barrel[facing=east,open=false]')
    return s


# ------------------------------------------------------------------------------------------------------- sloop
def sloop(rnd):
    s = Side('sloop')
    L, D = 13, 4
    paint = [(2, [('minecraft:spruce_planks', 5), ('raft:weathered_planks', 2), ('minecraft:dark_oak_planks', 1)]),
             (3, [('minecraft:cyan_terracotta', 4), ('raft:weathered_planks', 1)])]
    inside = hull(s, rnd, L, 2.9, D, paint, keel_rise=2, stern=0.6, wood='minecraft:spruce_planks',
                  top_paint='minecraft:stripped_spruce_log[axis=z]')
    # thwarts (benches) and gear
    for z in (4, 8):
        for x in range(-2, 3):
            if inside(x, 2, z):
                s.set(x, 2, z, 'minecraft:spruce_slab[type=top]')
    s.chest(0, 1, 2, 'raft:chests/wreck_fishing', facing='south', block='minecraft:barrel')
    s.set(-1, 1, 6, 'raft:wooden_crate[facing=east,open=false]')
    s.set(1, 1, 6, 'raft:rope_coil')
    s.set(0, 1, 10, 'minecraft:cobweb')
    s.set(1, 2, 3, 'minecraft:lantern[hanging=false]')
    for y in range(1, 3):
        s.set(0, y, 7, 'minecraft:spruce_log[axis=y]')            # mast stump
    # break in two: the bow half slides off and lies tilted
    stern, bow = Side('a'), Side('b')
    for (x, y, z), b in s.blocks.items():
        cut = 7 + (x + y) % 2
        (stern if z < cut else bow).blocks[(x, y, z)] = b
    stern = resample(stern, roll=-12, pitch=-8, pivot=(0, 0, 7))
    bow = resample(bow, roll=28, pitch=14, yaw=22, pivot=(0, 0, 7))
    out = Side('sloop', seed=2)
    out.paste(stern, 0, 0, 0)
    (_, by0, _), _ = bow.bounds()
    (_, sy0, _), _ = stern.bounds()
    out.paste(bow, 2, sy0 - by0, 3)
    out = normalise(out)
    (x0, y0, z0), (x1, y1, z1) = out.bounds()
    silt(out, 1, depth=1, chance=0.7, seed=22)
    # fallen mast with a torn sail spread over the sand, beside the boat
    mx = x1 + 2
    for z in range(z0 + 1, z0 + 10):
        out.set(mx, 2, z, 'minecraft:spruce_log[axis=z]')
    for x in range(mx + 1, mx + 5):
        for z in range(z0 + 2, z0 + 9):
            # the sail lies flat in the sand (wool flush with the floor): carpets would wash away under water
            if rnd.random() < 0.65:
                out.set(x, 1, z, 'minecraft:white_wool' if rnd.random() < 0.8 else 'minecraft:light_gray_wool')
    for (dx, dz) in ((-2, 3), (-3, 8), (-1, 12)):
        out.set(x0 + dx + 2, 2, z0 + dz, pick(rnd, [('raft:wooden_crate[facing=north,open=false]', 2),
                                                     ('minecraft:barrel[facing=up,open=false]', 1)]))
    net(out, rnd, x0 - 2, 2, z1 - 3, x0 + 1, z1 + 1, 0.5)
    return out


# --------------------------------------------------------------------------------------------------- capsized
def capsized(rnd):
    s = Side('capsized')
    L, D = 14, 5
    paint = [(1, [('minecraft:red_terracotta', 5), ('raft:weathered_planks', 1)]),
             (3, [('minecraft:green_terracotta', 5), ('minecraft:lime_terracotta', 1), ('raft:weathered_planks', 1)]),
             (4, [('minecraft:white_terracotta', 4), ('raft:weathered_planks', 1)])]
    inside = hull(s, rnd, L, 3.2, D, paint, keel_rise=2, stern=0.8, wood='minecraft:spruce_planks',
                  top_paint='minecraft:stripped_dark_oak_log[axis=z]', deck=False)
    for z in range(L):                                             # keel
        if s.get(0, 0, z):
            s.set(0, 0, z, 'minecraft:dark_oak_log[axis=z]')
    # small cuddy cabin at the bow (ends up underneath)
    for z in range(8, 12):
        for x in (-2, 2):
            for y in range(D + 1, D + 3):
                s.set(x, y, z, 'raft:weathered_planks')
        for x in range(-2, 3):
            s.set(x, D + 3, z, 'minecraft:spruce_slab[type=bottom]')
    # gear under the hull
    s.chest(1, 1, 5, 'raft:chests/wreck_fishing', facing='west')
    s.set(-1, 1, 6, 'raft:wooden_crate[facing=east,open=false]')
    s.set(0, 1, 3, 'raft:rope_coil')
    s = resample(s, roll=180, pivot=(0, D / 2, 0))
    s = resample(s, roll=-8, pitch=4, pivot=(0, 0, L // 2))
    s = normalise(s)
    (x0, y0, z0), (x1, y1, z1) = s.bounds()
    # a hole in the bottom (now on top) to get in
    holes = 0
    for (x, y, z), b in list(s.blocks.items()):
        if y >= y1 - 1 and 5 <= z <= 7 and abs(x - (x0 + x1) / 2) <= 1.2 and b[0] != 'minecraft:water':
            s.set(x, y, z, 'minecraft:water')
            holes += 1
    silt(s, 2, depth=2, seed=23)
    # crab traps and buoys around
    for i, (dx, dz) in enumerate(((-3, 2), (-2, 6), (x1 - x0 + 2, 4), (x1 - x0 + 3, 9), (2, z1 - z0 + 2))):
        x, z = x0 + dx, z0 + dz
        s.set(x, 3, z, 'raft:wooden_crate[facing=north,open=false]' if i % 2 == 0 else 'minecraft:cobweb')
        if i in (1, 3):
            s.set(x, 4, z, 'minecraft:iron_chain[axis=y]')
            s.set(x, 5, z, 'raft:buoy_green' if i == 1 else 'raft:buoy_red')
    return s


BUILDERS = [trawler, sloop, capsized]


def main(preview=False):
    out = []
    for v, fn in enumerate(BUILDERS):
        rnd = random.Random(31415903 + v)
        s = fn(rnd)
        overgrow(s, seed=40 + v, barnacle=0.06, kelp=0.025, kelp_h=(3, 10), grass=0.07, pickles=0.03,
                 kelp_max_y=3, starfish=0.01)
        path = f'wreck_fishing_boat/{fn.__name__}'
        save(s, path, floor_y=2)
        out.append((path, s))
        if preview:
            print('  preview', contact(s, f'wreck_fishing_{fn.__name__}', scale=7))
    write_structure('wreck_fishing_boat', [(p, 1) for p, _ in out], [NO_SPILL_PLANTS],
                    step='surface_structures', terrain='beard_thin', start_height=-3, heightmap='OCEAN_FLOOR_WG',
                    biomes=['raft_ocean_biome', 'kelp_forest', 'wreck_graveyard', 'abyssal_trench', 'drowned_shelf',
                            'frost_floes', 'coral_shallows', 'glowing_lagoon'],
                    spacing=12, separation=4, salt=31415903, max_distance=48)
    return out
