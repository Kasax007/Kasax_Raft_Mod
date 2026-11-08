"""pirate_cove - Saltjaw Brannigan's hidden hideout: a hollow rock hill on a jungle/palm island with a sea cave.

The template owns the whole hill (rock with a grassy, palm-crowned top) so it works wherever the island is: the
cove is placed at a fixed height (hill base y=0 = world 88, the lagoon surface y=11 = world 99, a rock root reaches
down to world 78 so the hill never floats over deeper water). No terrain adaptation: a box-shaped beard cut
straight-walled moats into neighbouring islands, so the island terrain now stays and merges with the hill; the cave,
the lagoon and the sea channel in front of the arch are explicit air/water, so terrain cannot fill them. Inside: a vine-curtained sea arch into a lagoon cavern, a plank dock
with a small moored pirate sloop, a crew deck on the back ledge (hammocks, kegs, galley), a rope bridge high over
the lagoon between two scaffold platforms (one with a cannon aimed at the entrance), the captain's quarters behind a
teak facade (story chest pirate_captain, wanted poster, chart table), the gated treasure grotto with heaps of gold
coins and the treasure chest, and a secret shaft with a rope ladder up to the hilltop.
"""
import math
import random

from .lib import Side, contact, pick, save, spawns, write_structure
from .wreck_fishing_boat import hull

W0 = 88
WL = 99 - W0           # lagoon surface (template y 11)
ROOT = 10              # rock root below the hill base (template y -10 .. -1)
R = 24
PEAK = 38
CAV = (0, WL + 5, 0, 14.0, 12.0, 13.0)     # cavity centre + radii
ROCK = [('minecraft:stone', 8), ('minecraft:andesite', 4), ('minecraft:cobblestone', 2),
        ('minecraft:mossy_cobblestone', 2), ('minecraft:tuff', 1)]
LEDGE = [('minecraft:stone', 4), ('minecraft:cobblestone', 3), ('minecraft:mossy_cobblestone', 2),
         ('minecraft:andesite', 2), ('minecraft:gravel', 1)]
PLANK = [('minecraft:spruce_planks', 5), ('raft:teak_planks', 3), ('raft:weathered_planks', 2)]


BUMPS = [(-3, -3, 22, 27), (9, 2, 15, 20), (-11, 8, 11, 12), (2, 11, 12, 14), (-6, -14, 10, 15)]


def shape(x, z):
    """(v, h): v > 0 inside the hill footprint (v < 0 just outside), h = height above the rim."""
    v = max(1 - math.hypot(x - bx, z - bz) / br for bx, bz, br, a in BUMPS)
    h = max(a * max(0.0, 1 - (math.hypot(x - bx, z - bz) / br) ** 2) ** 0.8 for bx, bz, br, a in BUMPS)
    n = 1.4 * math.sin(x * 0.37 + 1.3) * math.cos(z * 0.29 - 0.4) + 0.9 * math.sin((x + z) * 0.23 + 2.0)
    return v, h + n


def height(x, z, rnd_field=None):
    v, h = shape(x, z)
    if v <= 0:
        return None
    return int(WL + 1 + h)


def in_cavity(x, y, z):
    cx, cy, cz, rx, ry, rz = CAV
    return ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 + ((z - cz) / rz) ** 2 <= 1.0


def in_entrance(x, y, z):
    """Sea arch along +z: 7 wide, rounded top 7 above the water, full depth below."""
    if z < 6 or abs(x) > 3.6:
        return False
    if y <= WL:
        return y >= 3
    return (y - WL - 1) < 5 + math.sqrt(max(0.0, 3.6 ** 2 - x * x)) * 0.6


def hill(s, rnd):
    tops = {}
    for x in range(-R - 4, R + 5):
        for z in range(-R - 4, R + 5):
            v, _ = shape(x, z)
            h = height(x, z)
            if h is None:
                if v > -0.22:                       # underwater skirt and rocks at the waterline
                    top = int(WL - 1 + v * 40 + rnd.uniform(-1, 1))
                    for y in range(-ROOT if v > -0.1 else 0, max(0, top) + 1):
                        if not in_entrance(x, y, z):
                            s.set(x, y, z, pick(rnd, ROCK) if y < top else pick(
                                rnd, [('minecraft:sand', 3), ('minecraft:gravel', 1), ('minecraft:stone', 2)]))
                continue
            tops[(x, z)] = h
            steep = any(abs((height(x + dx, z + dz) or WL) - h) >= 3 for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            for y in range(-ROOT, 0):               # root: the hill stands on rock, also over deeper water
                s.set(x, y, z, pick(rnd, ROCK))
            for y in range(0, h + 1):
                cav = in_cavity(x, y, z) or in_entrance(x, y, z)
                if cav:
                    if y <= 2:
                        s.set(x, y, z, pick(rnd, [('minecraft:sand', 5), ('minecraft:gravel', 2)]))
                    elif y <= WL:
                        s.set(x, y, z, 'minecraft:water')
                    else:
                        s.set(x, y, z, 'minecraft:air')
                elif y == h:
                    if h <= WL + 2:
                        s.set(x, y, z, pick(rnd, [('minecraft:sand', 4), ('raft:shell_sand', 1), ('minecraft:stone', 2)]))
                    elif steep:
                        s.set(x, y, z, pick(rnd, [('minecraft:stone', 3), ('minecraft:andesite', 2),
                                                  ('minecraft:mossy_cobblestone', 1)]))
                    else:
                        s.set(x, y, z, pick(rnd, [('minecraft:grass_block[snowy=false]', 12), ('minecraft:moss_block', 1),
                                                  ('minecraft:rooted_dirt', 1)]))
                elif y >= h - 2 and h > WL + 2 and not steep:
                    s.set(x, y, z, 'minecraft:dirt')
                else:
                    s.set(x, y, z, pick(rnd, ROCK))
    # boulders on the shore
    for _ in range(18):
        a = rnd.uniform(0, 2 * math.pi)
        for rr in range(R + 4, 0, -1):
            x, z = int(rr * math.cos(a)), int(rr * math.sin(a))
            if height(x, z) is not None:
                break
        if in_entrance(x, WL + 1, z) or abs(x) < 6 and z > 0:
            continue
        for dx in range(-1, 2):
            for dz in range(-1, 2):
                if rnd.random() < 0.7:
                    yb = (height(x + dx, z + dz) or WL) + 1
                    s.set(x + dx, yb, z + dz, pick(rnd, [('minecraft:mossy_cobblestone', 2), ('minecraft:stone', 2),
                                                          ('minecraft:andesite', 1)]))
    # sea channel in front of the arch, out to the template's edge: explicit water and air, so a neighbouring island
    # cannot close the entrance
    for x in range(-3, 4):
        for z in range(6, R + 5):
            if height(x, z) is not None:
                continue
            for y in range(3, WL + 9):
                if s.get(x, y, z) is None:
                    s.set(x, y, z, 'minecraft:water' if y <= WL else 'minecraft:air')
    return tops


def ledges(s, rnd):
    """Rock shelves around the lagoon: back (north), west and east, walking level WL+2."""
    for x in range(-15, 16):
        for z in range(-14, 14):
            back = z <= -6
            west = x <= -9 and z <= 6
            east = x >= 9 and z <= 6
            if not (back or west or east):
                continue
            for y in range(3, WL + 2):
                if in_cavity(x, y, z) and not in_entrance(x, y, z):
                    s.set(x, y, z, pick(rnd, LEDGE) if y < WL + 1 else pick(rnd, [('minecraft:stone', 3),
                                                                                  ('minecraft:cobblestone', 2),
                                                                                  ('minecraft:mossy_cobblestone', 1),
                                                                                  ('minecraft:packed_mud', 1)]))
    # ledge edge stones / lips
    for x in range(-15, 16):
        for z in range(-14, 14):
            if s.get(x, WL + 1, z) and s.get(x, WL + 1, z) != 'minecraft:air' and s.get(x, WL + 2, z) == 'minecraft:air':
                nb = [s.get(x + dx, WL + 1, z + dz) for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))]
                if 'minecraft:water' in nb and rnd.random() < 0.15:
                    s.set(x, WL + 2, z, 'minecraft:cobblestone_slab[type=bottom]')


def post(s, x, z, y0, y1, block='minecraft:spruce_log[axis=y]'):
    for y in range(y0, y1 + 1):
        s.set(x, y, z, block)


def dock(s, rnd):
    # pier from the back ledge into the lagoon (west of the moored sloop)
    for z in range(-6, 6):
        for x in (-3, -2, -1):
            s.set(x, WL + 1, z, pick(rnd, PLANK))
        if z % 3 == 0:
            for x in (-4, 0):
                post(s, x, z, 3, WL + 2)
                s.set(x, WL + 3, z, 'minecraft:spruce_fence')
            if z % 6 == 0:
                s.set(-4, WL + 4, z, 'minecraft:lantern[hanging=false]')
    s.set(-2, WL + 2, 5, 'raft:rope_coil')
    s.set(-3, WL + 2, 3, 'raft:wooden_crate[facing=north,open=false]')
    s.set(-3, WL + 2, 2, 'minecraft:barrel[facing=up,open=false]')
    s.set(-3, WL + 3, 3, 'minecraft:barrel[facing=up,open=false]')
    s.set(-1, WL + 2, -4, 'raft:powder_keg')
    # cargo hoist on the back ledge
    post(s, -5, -7, WL + 2, WL + 7)
    for x in range(-4, -1):
        s.set(x, WL + 7, -7, 'minecraft:stripped_spruce_log[axis=x]')
    for y in range(WL + 4, WL + 7):
        s.set(-2, y, -7, 'minecraft:iron_chain[axis=y]')
    s.set(-2, WL + 3, -7, 'raft:wooden_crate[facing=east,open=false]')


def sloop(s, rnd):
    """Small pirate sloop moored at the pier: teak hull, mast with furled sail, cannon, flag."""
    b = Side('sloop')
    paint = [(1, [('raft:teak_planks', 5), ('minecraft:dark_oak_planks', 2)]),
             (3, [('raft:teak_planks', 4), ('minecraft:stripped_dark_oak_wood', 1)])]
    hull(b, rnd, 13, 2.9, 4, paint, keel_rise=2, stern=0.75, wood='raft:teak_planks',
         top_paint='minecraft:stripped_dark_oak_log[axis=z]', floor_block='minecraft:spruce_planks')
    for z in range(1, 12):                       # deck at the gunwale level - 1
        for x in range(-2, 3):
            if b.get(x, 3, z) == 'minecraft:water':
                b.set(x, 3, z, pick(rnd, [('minecraft:spruce_planks', 3), ('raft:teak_planks', 1)]))
    for y in range(4, 14):
        b.set(0, y, 6, 'minecraft:spruce_log[axis=y]')
    for x in range(-3, 4):
        b.set(x, 12, 7, 'minecraft:stripped_spruce_log[axis=x]')
        if abs(x) < 3:
            b.set(x, 11, 7, 'raft:sail_canvas_red_striped' if x % 2 else 'raft:sail_canvas')
    b.set(0, 14, 6, 'minecraft:spruce_fence')
    b.set(0, 13, 5, 'raft:jolly_roger[facing=north]')
    b.set(0, 4, 10, 'raft:ship_cannon[facing=south]')
    b.set(1, 4, 2, 'minecraft:barrel[facing=up,open=false]')
    b.set(-1, 4, 2, 'raft:ship_lantern[hanging=false]')
    b.set(0, 4, 1, 'raft:ship_wheel[facing=south]')
    b.set(0, 4, 0, 'minecraft:stripped_spruce_log[axis=y]')
    # paste: hull along z, keel at y WL-3, beside the pier (x 1..7, z -4..8)
    for (x, y, z), blk in b.blocks.items():
        if blk[0] == 'minecraft:water' and y > 3:
            continue
        s.blocks[(x + 4, y + WL - 3, z - 4)] = blk
    # mooring lines to the pier bollards
    s.set(0, WL + 4, -3, 'minecraft:iron_chain[axis=x]')
    s.set(0, WL + 4, 3, 'minecraft:iron_chain[axis=x]')


def crew_ledge(s, rnd):
    y = WL + 2
    for (x, z) in ((-7, -10), (-4, -12), (4, -11)):
        s.set(x, y, z, 'raft:hammock[facing=east,occupied=false,part=foot]')
        s.set(x + 1, y, z, 'raft:hammock[facing=east,occupied=false,part=head]')
    s.set(5, y, -8, 'raft:driftwood_table')
    s.set(5, y + 1, -8, 'raft:rum_bottles[bottles=3,facing=south]')
    s.set(4, y, -8, 'raft:driftwood_chair[facing=east]')
    s.set(6, y, -8, 'raft:driftwood_chair[facing=west]')
    s.set(7, y, -11, 'minecraft:water_cauldron[level=1]')
    s.set(7, y - 1, -11, 'minecraft:campfire[facing=north,lit=true,signal_fire=false]')
    s.set(7, y - 1, -11, 'minecraft:stone')
    s.set(8, y, -11, 'minecraft:campfire[facing=west,lit=true,signal_fire=false]')
    for (x, z) in ((-9, -8), (-10, -7), (-9, -7), (9, -9)):
        s.set(x, y, z, pick(rnd, [('raft:powder_keg', 2), ('minecraft:barrel[facing=up,open=false]', 2),
                                  ('raft:wooden_crate[facing=north,open=false]', 1)]))
    s.set(-10, y + 1, -7, 'raft:powder_keg')
    s.set(0, y, -12, 'raft:brazier[lit=true]')
    s.set(-2, y, -9, 'raft:ship_lantern[hanging=false]')
    s.set(2, y, -13, 'raft:wooden_crate[facing=south,open=false]')
    s.chest(1, y, -13, 'raft:chests/pirate_treasure', facing='south', block='minecraft:barrel')


def captain(s, rnd):
    """Captain's quarters dug into the west wall, teak facade facing the lagoon (east)."""
    fx = -15
    x0, x1, z0, z1 = -20, fx - 1, -4, 3
    y0 = WL + 2
    for x in range(x0 - 1, fx + 1):
        for z in range(z0 - 1, z1 + 2):
            for y in range(y0 - 1, y0 + 5):
                inner = x0 <= x <= x1 and z0 <= z <= z1 and y0 <= y <= y0 + 3
                if inner:
                    s.set(x, y, z, 'minecraft:air')
                elif y == y0 - 1 and x0 <= x <= x1 and z0 <= z <= z1:
                    s.set(x, y, z, pick(rnd, [('raft:teak_planks', 3), ('minecraft:spruce_planks', 2)]))
                elif x == fx and y < y0 + 4:
                    s.set(x, y, z, 'minecraft:stripped_dark_oak_log[axis=y]' if z in (z0 - 1, z1 + 1, 0) else
                          pick(rnd, [('raft:teak_planks', 4), ('minecraft:dark_oak_planks', 1)]))
                elif s.get(x, y, z) in (None, 'minecraft:air', 'minecraft:water'):
                    s.set(x, y, z, pick(rnd, ROCK))
    # facade: door, windows, roof beam, lanterns, flag
    s.set(fx, y0, -2, 'raft:teak_door[facing=east,half=lower,hinge=left,open=false,powered=false]')
    s.set(fx, y0 + 1, -2, 'raft:teak_door[facing=east,half=upper,hinge=left,open=false,powered=false]')
    for z in (-3, 2):
        s.set(fx, y0 + 1, z, 'minecraft:glass_pane')
    for z in range(z0 - 1, z1 + 2):
        s.set(fx + 1, y0 + 4, z, 'raft:teak_slab[type=bottom]')
    s.set(fx + 1, y0 + 3, -1, 'raft:wanted_poster[facing=east]')
    s.set(fx + 1, y0 + 2, 1, 'raft:jolly_roger[facing=east]')
    s.set(fx + 1, y0 + 2, -4, 'minecraft:wall_torch[facing=east]')
    # inside: bed, desk, story chest, shelves, coins
    s.set(x0, y0, z1, 'minecraft:red_bed[facing=west,occupied=false,part=head]')
    s.set(x0 + 1, y0, z1, 'minecraft:red_bed[facing=west,occupied=false,part=foot]')
    s.set(x0 + 2, y0, z0, 'raft:chart_table[facing=south]')
    s.set(x0 + 2, y0, z0 + 1, 'raft:driftwood_chair[facing=north]')
    s.chest(x0, y0, z0, 'raft:chests/story/pirate_captain', facing='east', block='raft:treasure_chest')
    s.set(x0, y0, z0 + 1, 'raft:gold_coin_pile[layers=2]')
    s.set(x0, y0 + 1, -1, 'raft:driftwood_shelf[facing=east]')
    s.set(x0, y0, -1, 'minecraft:bookshelf')
    s.set(x0, y0, 0, 'minecraft:bookshelf')
    s.set(x0, y0 + 1, 0, 'raft:rum_bottles[bottles=2,facing=east]')
    s.set(x0 + 3, y0, z1, 'raft:spyglass_stand[facing=west]')
    s.set(x0 + 2, y0 + 1, z1 + 1, 'raft:wanted_poster[facing=north]')
    for x in range(x0 + 1, x1):
        for z in range(z0 + 1, z1):
            if s.get(x, y0, z) == 'minecraft:air':
                s.set(x, y0, z, 'minecraft:red_carpet')
    s.set(x0 + 3, y0 + 3, 0, 'raft:ship_lantern[hanging=true]')
    s.set(x0 + 1, y0, z0 + 1, 'minecraft:skeleton_skull[rotation=12]')


def treasure(s, rnd):
    """Treasure grotto in the east wall behind an iron gate: a hill of gold coins, the chest, skulls."""
    gx = 15
    x0, x1, z0, z1 = gx + 1, 21, -4, 3
    y0 = WL + 2
    cx, cz = 18, -1
    for x in range(gx, x1 + 2):
        for z in range(z0 - 1, z1 + 2):
            for y in range(y0 - 1, y0 + 6):
                d = math.sqrt(((x - cx) / 3.6) ** 2 + ((z - cz) / 4.2) ** 2 + ((y - y0 - 1) / 3.2) ** 2)
                if d <= 1.0 and y >= y0:
                    s.set(x, y, z, 'minecraft:air')
                elif s.get(x, y, z) in (None, 'minecraft:air', 'minecraft:water'):
                    s.set(x, y, z, pick(rnd, ROCK))
    for z in range(-2, 1):                      # passage from the ledge
        for y in (y0, y0 + 1, y0 + 2):
            for x in range(gx - 1, gx + 2):
                s.set(x, y, z, 'minecraft:air')
        s.set(gx - 1, y0 - 1, z, 'minecraft:cobblestone')
    for z in (-2, 0):
        for y in (y0, y0 + 1, y0 + 2):
            s.set(gx, y, z, 'minecraft:iron_bars')
    s.set(gx, y0 + 2, -1, 'minecraft:iron_bars')
    # coins heaped towards the chest
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if s.get(x, y0, z) != 'minecraft:air':
                continue
            d = math.hypot(x - 19, z - (-1))
            layers = max(0, int(8 - d * 2.2 + rnd.uniform(-1, 1)))
            if layers >= 1:
                s.set(x, y0, z, f'raft:gold_coin_pile[layers={min(8, layers)}]')
            if d < 1.2:
                s.set(x, y0, z, 'raft:gold_coin_pile[layers=8]')
    s.chest(19, y0 + 1, -1, 'raft:chests/pirate_treasure', facing='west', block='raft:treasure_chest')
    s.set(20, y0, -1, 'minecraft:gold_block')
    s.set(18, y0, 1, 'minecraft:skeleton_skull[rotation=4]')
    s.set(17, y0, -3, 'raft:rum_bottles[bottles=1,facing=west]')
    s.set(20, y0, 1, 'raft:powder_keg')
    s.set(18, y0 + 3, -1, 'raft:ship_lantern[hanging=true]')
    s.set(17, y0, 1, 'raft:brazier[lit=true]')


def bridge(s, rnd):
    """Scaffold platforms on the west and east walls joined by a sagging rope bridge over the lagoon."""
    by = WL + 8
    for (x0, x1) in ((-12, -8), (8, 12)):
        for x in range(x0, x1 + 1):
            for z in range(1, 6):
                s.set(x, by, z, pick(rnd, PLANK))
        for (px, pz) in ((x0 if x0 > 0 else x1, 1), (x0 if x0 > 0 else x1, 5)):
            post(s, px, pz, WL + 2, by - 1)
        s.set(x0 if x0 < 0 else x1, by + 1, 3, 'minecraft:lantern[hanging=false]')
    for x in range(-7, 8):
        sag = int(round(1.6 * (1 - (x / 7.5) ** 2)))
        s.set(x, by - sag, 3, 'minecraft:spruce_slab[type=bottom]' if x % 2 else 'raft:weathered_slab[type=bottom]')
        s.set(x, by - sag + 1, 2, 'raft:rope_railing')
        s.set(x, by - sag + 1, 4, 'raft:rope_railing')
        for z in (2, 4):
            if s.get(x, by - sag, z) in (None, 'minecraft:air'):
                s.set(x, by - sag, z, 'minecraft:air')
    # ladders up to both platforms
    for y in range(WL + 2, by):
        s.set(-8, y, 6, 'minecraft:ladder[facing=south]')
        s.set(8, y, 6, 'minecraft:ladder[facing=south]')
    for y in range(WL + 2, by):
        for x in (-8, 8):
            if s.get(x, y, 7) in (None, 'minecraft:air', 'minecraft:water'):
                s.set(x, y, 7, 'minecraft:spruce_planks')
    # cannon aimed at the entrance, lookout
    s.set(10, by + 1, 5, 'raft:ship_cannon[facing=south]')
    s.set(11, by + 1, 4, 'raft:powder_keg')
    s.set(-10, by + 1, 4, 'raft:spyglass_stand[facing=south]')


def lights(s, rnd):
    """Lanterns hanging on chains from the cave ceiling."""
    for (x, z) in ((-5, -3), (5, 2), (0, -9), (-10, -2), (10, -4), (0, 8)):
        y = WL + 3
        while s.get(x, y + 1, z) == 'minecraft:air' and y < 40:
            y += 1
        top = y
        for yy in range(max(WL + 6, top - 4), top + 1):
            s.set(x, yy, z, 'minecraft:iron_chain[axis=y]')
        s.set(x, max(WL + 6, top - 4) - 1, z, 'raft:ship_lantern[hanging=true]')
    for (x, z, f) in ((-13, -6, 'east'), (13, -6, 'west'), (-6, -13, 'south'), (6, -12, 'south')):
        y = WL + 4
        if s.get(x, y, z) == 'minecraft:air':
            s.set(x, y, z, f'minecraft:wall_torch[facing={f}]')


def shaft(s, rnd, tops):
    """Secret way in from the hilltop: 2x2 shaft with a rope ladder, hidden by bushes."""
    sx, sz = -5, -11
    top = max(tops.get((sx, sz), PEAK), tops.get((sx + 1, sz), PEAK))
    for y in range(WL + 2, top + 2):
        for dx in (0, 1):
            for dz in (0, 1):
                if not in_cavity(sx + dx, y, sz + dz):
                    s.set(sx + dx, y, sz + dz, 'minecraft:air')
    for y in range(WL + 2, top + 1):
        if not in_cavity(sx, y, sz - 1) or s.get(sx, y, sz - 1) != 'minecraft:air':
            s.set(sx, y, sz - 1, pick(rnd, ROCK))
        s.set(sx, y, sz, 'raft:rope_ladder[facing=south]')
    for dx in range(-1, 3):
        for dz in range(-1, 3):
            if dx in (-1, 2) or dz in (-1, 2):
                h = tops.get((sx + dx, sz + dz))
                if h:
                    s.set(sx + dx, h + 1, sz + dz, 'minecraft:jungle_leaves[distance=1,persistent=true,waterlogged=false]')
    s.set(sx + 1, top + 1, sz + 1, 'minecraft:spruce_trapdoor[facing=north,half=bottom,open=true]')


def greenery(s, rnd, tops):
    """Palms and bushes on the hill, vines over the sea arch."""
    palms = [(-8, 6), (7, -9), (12, 10), (-14, -10), (2, 14), (-3, -4), (15, 0), (-12, 14)]
    for (px, pz) in palms:
        h = tops.get((px, pz))
        if not h or h <= WL + 2:
            continue
        th = rnd.randint(5, 8)
        lean = rnd.choice(((1, 0), (0, 1), (-1, 0), (0, -1)))
        x, z = px, pz
        for i in range(th):
            if i == th // 2:
                x, z = x + lean[0], z + lean[1]
            s.set(x, h + 1 + i, z, 'raft:palm_log[axis=y]')
        ty = h + th
        s.set(x, ty + 1, z, 'raft:palm_leaves[distance=1,persistent=true,waterlogged=false]')
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            for k in (1, 2, 3):
                s.set(x + dx * k, ty + (1 if k < 3 else 0), z + dz * k,
                      'raft:palm_leaves[distance=1,persistent=true,waterlogged=false]')
        s.set(x + 1, ty, z + 1, 'raft:palm_coconut_leaves[distance=1,persistent=true,waterlogged=false]') \
            if rnd.random() < 0.5 else None
    for (x, z), h in tops.items():
        if h > WL + 3 and s.get(x, h, z) == 'minecraft:grass_block[snowy=false]' and s.get(x, h + 1, z) is None:
            r = rnd.random()
            if r < 0.09:
                s.set(x, h + 1, z, 'minecraft:jungle_leaves[distance=1,persistent=true,waterlogged=false]')
                if rnd.random() < 0.4 and s.get(x, h + 2, z) is None:
                    s.set(x, h + 2, z, 'minecraft:jungle_leaves[distance=1,persistent=true,waterlogged=false]')
            elif r < 0.16:
                s.set(x, h + 1, z, 'minecraft:fern' if rnd.random() < 0.5 else 'raft:tropical_fern')
            elif r < 0.22:
                s.set(x, h + 1, z, 'minecraft:short_grass')
            elif r < 0.24:
                s.set(x, h + 1, z, 'raft:hibiscus')
    # vine curtain over the sea arch (outer face)
    for x in range(-5, 6):
        zf = None
        for z in range(R, 5, -1):
            b = s.get(x, WL + 6, z)
            if b and b not in ('minecraft:air', 'minecraft:water'):
                zf = z
                break
        if zf is None:
            continue
        for y in range(WL + 2, WL + 11):
            if s.get(x, y, zf + 1) is None and s.get(x, y, zf) not in (None, 'minecraft:air', 'minecraft:water'):
                if rnd.random() < 0.75:
                    s.set(x, y, zf + 1, 'minecraft:vine[east=false,north=true,south=false,up=false,west=false]')
    for x in range(-3, 4):                            # vines hanging inside the arch mouth
        for z in range(14, 22):
            top = None
            for y in range(WL + 1, WL + 12):
                if s.get(x, y, z) == 'minecraft:air' and s.get(x, y + 1, z) not in (None, 'minecraft:air', 'minecraft:water'):
                    top = y
                    break
            if top and rnd.random() < 0.35:
                for k in range(rnd.randint(1, 4)):
                    if s.get(x, top - k, z) == 'minecraft:air' and top - k > WL + 1:
                        s.set(x, top - k, z, 'minecraft:cave_vines_plant[berries=false]' if k < 3 else
                              'minecraft:cave_vines[age=20,berries=true]')


def build():
    rnd = random.Random(31415904)
    s = Side('pirate_cove')
    tops = hill(s, rnd)
    ledges(s, rnd)
    dock(s, rnd)
    sloop(s, rnd)
    crew_ledge(s, rnd)
    captain(s, rnd)
    treasure(s, rnd)
    bridge(s, rnd)
    shaft(s, rnd, tops)
    lights(s, rnd)
    greenery(s, rnd, tops)
    # lagoon floor life
    for x in range(-12, 13):
        for z in range(-6, 22):
            if s.get(x, 3, z) == 'minecraft:water' and s.get(x, 2, z) in ('minecraft:sand', 'minecraft:gravel'):
                r = rnd.random()
                if r < 0.18:
                    s.set(x, 3, z, 'minecraft:seagrass')
                elif r < 0.21:
                    s.kelp(x, 3, z, rnd.randint(3, 7))
                elif r < 0.23:
                    s.set(x, 3, z, 'minecraft:sea_pickle[pickles=3]')
    s.chest(-3, 3, 9, 'raft:chests/pirate_treasure', facing='north')      # dropped chest on the lagoon floor
    return s


def main(preview=False):
    s = build()
    path = 'pirate_cove/cove_0'
    (_, y0, _), _ = s.bounds()
    assert y0 == -ROOT, y0
    save(s, path)
    if preview:
        print('  preview', contact(s, 'pirate_cove', cuts=(WL + 3, WL + 9), scale=4, slices_y=[WL - 2, WL + 2, WL + 4, WL + 8]))
    write_structure('pirate_cove', [(path, 1)], None,
                    step='surface_structures', terrain='none', start_height=W0 - ROOT, heightmap=None,
                    biomes=['jungle_isle', 'palm_isle'], spacing=52, separation=22, salt=31415904, max_distance=64,
                    spawn_overrides={'monster': spawns([('raft:drowned_corsair', 6, 1, 2), ('minecraft:skeleton', 1, 1, 1)])})
    return [(path, s)]
