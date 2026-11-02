"""coral_temple – overgrown Tidewarden temple on the sea floor of the shallows (spec 3.2 chapter 7, 11.1).

Template coordinates (x east, z south, y up); entrance in the south, sanctum in the north.
  y 0..2   stepped stylobate (courtyard floor y=2, players walk at y=3)
  south    propylon gate (two rune pylons with a lintel) and a grand stair
  middle   courtyard with a processional way, reflecting basin with a guardian statue, side colonnades,
           two flanking statues, fallen columns, sand drifts with suspicious sand
  north    sanctum hall behind a portico: inner columns, raised dais with raft:tidewarden_altar facing the entrance,
           four raft:sigil_pedestal, story chest raft:chests/story/coral_temple behind the altar, two side chapels with
           raft:chests/coral_temple_offering
Everything is a flooded ruin: the halls are filled with water in the template (the no-spill processor turns water
into air where the world is air, e.g. a temple crown above the sea surface) and every waterloggable block is
waterlogged. Corals, fans, pickles, anemones, barnacles, kelp and seagrass overgrow it.
"""
import math
import random

from bclib.nbt import Structure

from .common import (Deep, MODULE, NO_SPILL, pick, processor_list, rule, spawns, structure_json, structure_set,
                     template_pool, biome_tag, pv)

SID = 'coral_temple'
BRICKS = [('raft:tidestone_bricks', 66), ('raft:cracked_tidestone_bricks', 16), ('raft:mossy_tidestone_bricks', 13),
          ('raft:tidestone', 5)]
PAVE = [('raft:tidestone_bricks', 40), ('raft:tidestone', 25), ('raft:cracked_tidestone_bricks', 15),
        ('raft:mossy_tidestone_bricks', 12), ('raft:coral_stone_bricks', 8)]
CORAL_BLOCKS = ['minecraft:brain_coral_block', 'minecraft:tube_coral_block', 'minecraft:bubble_coral_block',
                'minecraft:fire_coral_block', 'minecraft:horn_coral_block']
CORALS = ['brain', 'tube', 'bubble', 'fire', 'horn']
WATER_TOP = 9     # template water is placed up to this layer; nothing waterloggable above it


def statue_piece(variant=0):
    """Tidewarden statue in local coordinates, base 3x3 centred at (0,0,0), looking south (+z)."""
    t = Structure('statue')
    S = t.set
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            S(dx, 0, dz, 'raft:chiseled_tidestone')
    for dy in (1, 2):
        for dx in (-1, 0, 1):
            S(dx, dy, 0, 'raft:tidestone_bricks')
            S(dx, dy, -1, 'raft:tidestone_bricks')
        S(-1, dy, 1, 'raft:tidestone_brick_stairs[facing=north,half=bottom]')
        S(1, dy, 1, 'raft:tidestone_brick_stairs[facing=north,half=bottom]')
        S(0, dy, 1, 'raft:tidestone_bricks')
    S(0, 3, 0, 'raft:tidestone_bricks'); S(0, 3, -1, 'raft:tidestone_bricks')
    S(-1, 3, 0, 'raft:tidestone_bricks'); S(1, 3, 0, 'raft:tidestone_bricks')
    S(-1, 3, 1, 'raft:tidestone_brick_wall'); S(1, 3, 1, 'raft:tidestone_brick_wall')
    S(0, 4, 0, 'raft:tidestone_bricks')
    S(-1, 4, 0, 'raft:tidestone_brick_stairs[facing=east,half=bottom]')
    S(1, 4, 0, 'raft:tidestone_brick_stairs[facing=west,half=bottom]')
    S(0, 5, 0, 'raft:chiseled_tidestone')
    S(0, 5, 1, 'raft:glowing_tidestone')
    if variant == 1:      # broken: the head lies at the feet
        t.set(0, 5, 0, None); t.set(0, 5, 1, None)
        S(2, 0, 2, 'raft:chiseled_tidestone')
    else:
        S(0, 6, 0, 'raft:tidestone_brick_stairs[facing=south,half=bottom]')
    # trident (prismarine shaft with prongs)
    for dy in range(1, 7):
        S(2, dy, 1, 'minecraft:prismarine_wall')
    S(2, 7, 1, 'minecraft:lightning_rod[facing=up,powered=false]')
    S(1, 6, 1, 'minecraft:lightning_rod[facing=up,powered=false]')
    S(3, 6, 1, 'minecraft:lightning_rod[facing=up,powered=false]')
    return t


def place_piece(s, piece, x, y, z, quarter_turns):
    p = piece.rotated(quarter_turns)
    for (px, py, pz), b in p.blocks.items():
        s.blocks[(x + px, y + py, z + pz)] = b


def column(s, x, y0, z, top, rnd, broken=False):
    """Column: chiseled base, pillar shaft, capital with stair flare. broken=True stops it short."""
    s.set(x, y0, z, 'raft:chiseled_tidestone')
    h = top
    if broken:
        h = y0 + 1 + int(rnd.random() * (top - y0 - 2))
    for y in range(y0 + 1, h):
        s.set(x, y, z, 'raft:tidestone_pillar[axis=y]')
    if not broken:
        s.set(x, top, z, 'raft:chiseled_tidestone')
        for dx, dz, f in ((1, 0, 'west'), (-1, 0, 'east'), (0, 1, 'north'), (0, -1, 'south')):
            if s.free(x + dx, top, z + dz):
                s.set(x + dx, top, z + dz, f'raft:tidestone_brick_stairs[facing={f},half=top]')
    else:
        s.set(x, h, z, 'raft:cracked_tidestone_bricks' if rnd.random() < 0.5 else 'raft:tidestone_pillar[axis=y]')
    return h


def fallen_column(s, x, y, z, axis, length):
    dx, dz = (1, 0) if axis == 'x' else (0, 1)
    for i in range(length):
        s.set(x + dx * i, y, z + dz * i, f'raft:tidestone_pillar[axis={axis}]' if i else 'raft:chiseled_tidestone')


def build(seed=31415961, variant=0):
    rnd = random.Random(seed)
    s = Deep(f'{SID}/temple_{variant}', seed)
    stylobate(s, rnd)
    gate(s, rnd)
    courtyard(s, rnd)
    obelisks(s)
    colonnades(s, rnd)
    sanctum(s, rnd)
    ruin(s, rnd)
    flood(s)
    overgrowth(s, rnd)
    s.finalize(wet_max_y=WATER_TOP + 2)
    return s


def stylobate(s, rnd):
    for y, m in ((0, 2), (1, 1), (2, 0)):
        for x in range(-18 - m, 19 + m):
            for z in range(-22 - m, 21 + m):
                edge = x in (-18 - m, 18 + m) or z in (-22 - m, 20 + m)
                if y == 2 and not edge:
                    s.pset(x, y, z, PAVE, rnd)
                elif edge:
                    s.set(x, y, z, 'raft:chiseled_tidestone' if (x + z) % 6 == 0 and y == 1 else pick(rnd, BRICKS))
                else:
                    s.pset(x, y, z, BRICKS, rnd)
    # step edges: stairs along the lip of the lower two courses
    for y, m in ((0, 2), (1, 1)):
        for x in range(-18 - m, 19 + m):
            for z, f in ((-22 - m, 'south'), (20 + m, 'north')):
                if rnd.random() < 0.85:
                    s.set(x, y + 1, z, f'raft:tidestone_brick_stairs[facing={f},half=bottom]') if s.free(x, y + 1, z) \
                        else None
        for z in range(-22 - m, 21 + m):
            for x, f in ((-18 - m, 'east'), (18 + m, 'west')):
                if rnd.random() < 0.85 and s.free(x, y + 1, z):
                    s.set(x, y + 1, z, f'raft:tidestone_brick_stairs[facing={f},half=bottom]')
    # foundation below (the ground under the platform)
    for x in range(-20, 21):
        for z in range(-24, 23):
            s.set(x, -1, z, pick(rnd, [('raft:tidestone', 3), ('minecraft:sandstone', 2), ('minecraft:sand', 1)]))


def gate(s, rnd):
    """Propylon at z 16..20: two pylons x ±4..±8 up to y 11, lintel at y 9..10, rune bands."""
    for sx in (-1, 1):
        for x in range(4, 9):
            for z in range(16, 21):
                X = sx * x
                for y in range(3, 12):
                    taper = (y >= 9 and (x == 8 or z in (16, 20)))
                    if taper:
                        continue
                    edge = x in (4, 8) or z in (16, 20)
                    if y in (5, 8) and edge:
                        st = 'raft:glowing_tidestone' if (x + z) % 2 == 0 else 'raft:chiseled_tidestone'
                    elif edge and x in (4, 8) and z in (16, 20):
                        st = 'raft:tidestone_pillar[axis=y]'
                    else:
                        st = pick(rnd, BRICKS)
                    s.set(X, y, z, st)
        # crown
        for x in range(4, 8):
            for z in range(17, 20):
                s.set(sx * x, 12, z, 'raft:tidestone_brick_slab[type=bottom]' if (x + z) % 2 else 'raft:chiseled_tidestone')
        s.set(sx * 6, 13, 18, 'raft:glowing_tidestone')
    # lintel spanning the passage (x -3..3) with a rune frieze
    for x in range(-3, 4):
        for z in range(16, 21):
            s.set(x, 10, z, pick(rnd, BRICKS))
            s.set(x, 9, z, 'raft:chiseled_tidestone' if z in (16, 20) else pick(rnd, BRICKS))
        s.set(x, 9, 20, 'raft:glowing_tidestone' if x % 2 == 0 else 'raft:chiseled_tidestone')
        s.set(x, 8, 20, 'raft:tidestone_brick_stairs[facing=south,half=top]')
        s.set(x, 8, 16, 'raft:tidestone_brick_stairs[facing=north,half=top]')
    # grand stair down the stylobate in front of the gate
    for x in range(-5, 6):
        s.set(x, 2, 21, 'raft:tidestone_brick_stairs[facing=north,half=bottom]')
        s.set(x, 1, 22, 'raft:tidestone_brick_stairs[facing=north,half=bottom]')
        s.set(x, 0, 23, 'raft:tidestone_brick_stairs[facing=north,half=bottom]')
        s.set(x, 1, 21, 'raft:tidestone_bricks'); s.set(x, 0, 22, 'raft:tidestone_bricks')
    for sx in (-6, 6):
        for (y, z) in ((3, 21), (2, 22), (1, 23)):
            s.set(sx, y - 1, z, 'raft:tidestone_bricks')
            s.set(sx, y, z, 'raft:tidestone_brick_wall')


def courtyard(s, rnd):
    # processional way
    for z in range(-5, 20):
        for x in (-1, 0, 1):
            s.set(x, 2, z, 'raft:chiseled_tidestone' if x == 0 and z % 3 == 0 else 'raft:tidestone_bricks')
        if z % 4 == 0:
            s.set(-2, 2, z, 'raft:glowing_tidestone'); s.set(2, 2, z, 'raft:glowing_tidestone')
    # reflecting basin with the great guardian at (0, 6)
    cx, cz = 0, 6
    for x in range(cx - 4, cx + 5):
        for z in range(cz - 4, cz + 5):
            r = math.hypot(x - cx, z - cz)
            if r <= 3.3:
                s.set(x, 2, z, 'minecraft:sand' if r > 1.5 else 'raft:chiseled_tidestone')
                s.set(x, 1, z, 'raft:tidestone')
            elif r <= 4.4:
                s.set(x, 2, z, 'raft:chiseled_tidestone')
                s.set(x, 3, z, 'raft:tidestone_brick_slab[type=bottom]')
    place_piece(s, statue_piece(0), cx, 3, cz, 0)
    # corals growing in the basin
    for (x, z) in ((cx - 2, cz - 2), (cx + 2, cz + 2), (cx + 2, cz - 1), (cx - 2, cz + 1), (cx, cz + 3)):
        if s.name_at(x, 3, z) is None:
            s.set(x, 3, z, f'minecraft:{rnd.choice(CORALS)}_coral')
    # flanking statues facing the path
    place_piece(s, statue_piece(0), -9, 3, -1, 3)    # looks east
    place_piece(s, statue_piece(1), 9, 3, -1, 1)     # looks west, broken head
    for (x, z) in ((-9, -1), (9, -1)):
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                if s.name_at(x + dx, 2, z + dz) not in (None,):
                    s.set(x + dx, 2, z + dz, 'raft:tidestone_bricks' if abs(dx) == 2 or abs(dz) == 2 else
                          'raft:chiseled_tidestone')


def obelisks(s):
    """Two rune obelisks inside the gate and two before the portico mark the processional way."""
    for (x, z, h) in ((-4, 13, 6), (4, 13, 6), (-4, -2, 5), (4, -2, 5)):
        s.set(x, 3, z, 'raft:chiseled_tidestone')
        for y in range(4, 3 + h):
            s.set(x, y, z, 'raft:tidestone_pillar[axis=y]')
        s.set(x, 3 + h, z, 'raft:glowing_tidestone')
        s.set(x, 4 + h, z, 'raft:tidestone_brick_slab[type=bottom]')
        for dx, dz, f in ((1, 0, 'west'), (-1, 0, 'east'), (0, 1, 'north'), (0, -1, 'south')):
            s.set(x + dx, 3, z + dz, f'raft:tidestone_brick_stairs[facing={f},half=bottom]')


def colonnades(s, rnd):
    """East and west colonnades z -4..15: outer wall x=±17, columns x=±13, roof y=8 (partly collapsed)."""
    for sx in (-1, 1):
        # outer wall with niches
        for z in range(-4, 16):
            for y in range(3, 9):
                niche = z % 5 == 2 and y in (4, 5)
                X = sx * 17
                if niche:
                    s.set(X, y, z, 'minecraft:water')
                    s.set(X + sx, y, z, pick(rnd, BRICKS))
                else:
                    s.set(X, y, z, 'raft:glowing_tidestone' if y == 7 and z % 5 == 2 else pick(rnd, BRICKS))
            s.set(sx * 17, 3, z, 'raft:chiseled_tidestone') if z % 5 == 0 else None
            s.set(sx * 17, 9, z, 'raft:tidestone_brick_slab[type=bottom]')
        # columns
        for z in range(-3, 16, 4):
            broken = (sx == 1 and z in (5, 9)) or (sx == -1 and z == 13)
            column(s, sx * 13, 3, z, 8, rnd, broken=broken)
        # roof between columns and wall
        for z in range(-4, 16):
            for x in range(13, 18):
                X = sx * x
                collapsed = (sx == 1 and 3 <= z <= 11) or (sx == -1 and 10 <= z <= 15)
                if collapsed and rnd.random() < 0.85:
                    continue
                if x == 13:
                    s.set(X, 8, z, 'raft:chiseled_tidestone' if (z + 3) % 4 == 0 else 'raft:tidestone_brick_slab[type=top]')
                else:
                    s.set(X, 8, z, pick(rnd, BRICKS))
        # corners: walls closing the courtyard at z=-5 and z=16 between colonnade and gate/sanctum
        for x in range(9, 18):
            for y in range(3, 7 - (1 if x % 3 == 0 else 0)):
                s.set(sx * x, y, 16, pick(rnd, BRICKS))
        for x in range(12, 18):
            for y in range(3, 8):
                s.set(sx * x, y, -5, pick(rnd, BRICKS))
    # low walls between gate pylons and colonnades
    for sx in (-1, 1):
        for x in range(9, 13):
            s.set(sx * x, 7, 16, 'raft:tidestone_brick_wall')


def sanctum(s, rnd):
    """Hall x -11..11, z -22..-6; portico z -5..-3; dais at the north end with the altar."""
    x1, x2, z1, z2 = -11, 11, -22, -6
    for x in range(x1, x2 + 1):
        for z in range(z1, z2 + 1):
            edge = x in (x1, x2) or z in (z1, z2)
            for y in range(3, 11):
                if edge:
                    corner = x in (x1, x2) and z in (z1, z2)
                    if corner:
                        st = 'raft:tidestone_pillar[axis=y]'
                    elif y == 3:
                        st = 'raft:chiseled_tidestone'
                    elif y == 7 and (x + z) % 4 == 0:
                        st = 'raft:glowing_tidestone'
                    elif y == 10:
                        st = 'raft:chiseled_tidestone'
                    else:
                        st = pick(rnd, BRICKS)
                    s.set(x, y, z, st)
            # roof: flat slab at y=11, stepped crown above
            s.set(x, 11, z, pick(rnd, BRICKS))
    for (m, y) in ((2, 12), (5, 13)):
        for x in range(x1 + m, x2 - m + 1):
            for z in range(z1 + m, z2 - m + 1):
                edge = x in (x1 + m, x2 - m) or z in (z1 + m, z2 - m)
                s.set(x, y, z, 'raft:chiseled_tidestone' if edge and (x + z) % 4 == 0 else pick(rnd, BRICKS))
    lantern_tower(s, rnd, 0, -14)
    # door: 3 wide x 5 high in the south wall, framed
    for x in (-1, 0, 1):
        for y in range(3, 8):
            s.set(x, y, z2, 'minecraft:water')
    for y in range(3, 9):
        for x in (-2, 2):
            s.set(x, y, z2, 'raft:tidestone_pillar[axis=y]')
    for x in range(-2, 3):
        s.set(x, 8, z2, 'raft:glowing_tidestone' if x == 0 else 'raft:chiseled_tidestone')
    # side windows (slits) and chapel openings
    for z in (-18, -14, -10):
        for sx in (x1, x2):
            for y in (6, 7):
                s.set(sx, y, z, 'minecraft:water')
    # hall interior water and floor
    for x in range(x1 + 1, x2):
        for z in range(z1 + 1, z2):
            s.set(x, 2, z, 'raft:tidestone_bricks' if (x + z) % 2 else 'raft:tidestone')
            for y in range(3, 11):
                s.set(x, y, z, 'minecraft:water')
    # aisle and inner columns
    for z in range(z1 + 1, z2):
        s.set(0, 2, z, 'raft:chiseled_tidestone' if z % 2 else 'raft:coral_stone_bricks')
    for z in (-19, -15, -11, -8):
        for sx in (-6, 6):
            column(s, sx, 3, z, 10, rnd, broken=(sx == 6 and z == -11))
    # dais: two steps up to y=4 (z -21..-15)
    for x in range(-5, 6):
        for z in range(-21, -14):
            s.set(x, 3, z, 'raft:chiseled_tidestone' if (x + z) % 3 == 0 else 'raft:tidestone_bricks')
        s.set(x, 3, -14, 'raft:tidestone_brick_stairs[facing=north,half=bottom]')
    for z in range(-21, -14):
        s.set(-6, 3, z, 'raft:tidestone_brick_stairs[facing=east,half=bottom]')
        s.set(6, 3, z, 'raft:tidestone_brick_stairs[facing=west,half=bottom]')
    # the Tidewarden altar facing the entrance (south), four sigil pedestals, story chest behind
    s.set(0, 4, -18, 'raft:tidewarden_altar[coral=false,ember=false,facing=south,pearl=false,storm=false]')
    for (x, z) in ((-3, -20), (3, -20), (-3, -16), (3, -16)):
        s.set(x, 4, z, 'raft:sigil_pedestal')
    s.chest(0, 4, -21, 'raft:chests/story/coral_temple', facing='south')
    # rune disc on the back wall behind the altar
    disc = ['.ggg.', 'g.c.g', 'gcGcg', 'g.c.g', '.ggg.']
    for i, row in enumerate(disc):
        for j, c in enumerate(row):
            st = {'g': 'raft:glowing_tidestone', 'c': 'raft:chiseled_tidestone', 'G': 'minecraft:sea_lantern',
                  '.': None}[c]
            if st:
                s.set(-2 + j, 9 - i, z1, st)
    # side chapels with offering chests (east and west)
    for sx, f in ((-1, 'east'), (1, 'west')):
        X = sx * 10
        s.chest(X, 3, -12, 'raft:chests/coral_temple_offering', facing=f)
        s.set(X, 3, -13, 'minecraft:sea_pickle[pickles=4]')
        s.set(X, 3, -11, 'raft:sea_anemone_glow')
        s.set(X, 4, -12, 'raft:tidestone_brick_slab[type=bottom]')
        s.set(sx * 11, 5, -12, 'raft:glowing_tidestone')
    # portico in front of the hall
    for x in (-8, -4, 4, 8):
        column(s, x, 3, -4, 9, rnd, broken=(x == 8))
    for x in range(-9, 10):
        if not (x >= 6):                   # the east end of the pediment has fallen
            s.set(x, 10, -4, 'raft:chiseled_tidestone' if x % 4 == 0 else pick(rnd, BRICKS))
            s.set(x, 10, -5, pick(rnd, BRICKS))
            s.set(x, 11, -5, 'raft:tidestone_brick_stairs[facing=south,half=bottom]')
    for x in range(-11, 12):
        for z in (-5, -4, -3):
            s.set(x, 2, z, 'raft:tidestone_bricks' if (x + z) % 2 else 'raft:chiseled_tidestone')
    # lights on the dais
    for (x, z) in ((-5, -21), (5, -21)):
        s.set(x, 4, z, 'raft:glowing_tidestone')
        s.set(x, 5, z, 'raft:tidestone_brick_wall')
        s.set(x, 6, z, 'minecraft:sea_lantern')


def lantern_tower(s, rnd, cx, cz):
    """Landmark over the altar: 7x7 tower y 12..16 with open windows, stepped cap, glowing finial (full blocks
    only, so it may break the sea surface in very shallow water)."""
    for x in range(cx - 3, cx + 4):
        for z in range(cz - 3, cz + 4):
            edge = abs(x - cx) == 3 or abs(z - cz) == 3
            corner = abs(x - cx) == 3 and abs(z - cz) == 3
            for y in range(12, 17):
                if not edge:
                    s.blocks.pop((x, y, z), None)      # hollow: the light shaft opens into the hall below
                    continue
                window = not corner and abs((x - cx) if abs(z - cz) == 3 else (z - cz)) <= 1 and 13 <= y <= 15
                if window:
                    s.blocks.pop((x, y, z), None)
                elif corner:
                    s.set(x, y, z, 'raft:tidestone_pillar[axis=y]')
                elif y == 16:
                    s.set(x, y, z, 'raft:glowing_tidestone' if (x + z) % 2 == 0 else 'raft:chiseled_tidestone')
                else:
                    s.pset(x, y, z, BRICKS, rnd)
            if not edge:
                s.blocks.pop((x, 11, z), None)
    for (m, y) in ((3, 17), (2, 18), (1, 19)):
        for x in range(cx - m, cx + m + 1):
            for z in range(cz - m, cz + m + 1):
                if abs(x - cx) == m or abs(z - cz) == m:
                    s.set(x, y, z, 'raft:chiseled_tidestone' if (x + z) % 2 == 0 else 'raft:tidestone_bricks')
                elif y == 17:
                    s.set(x, y, z, 'raft:tidestone_bricks')
    s.set(cx, 19, cz, 'raft:glowing_tidestone')
    s.set(cx, 20, cz, 'raft:glowing_tidestone')
    s.set(cx, 21, cz, 'raft:chiseled_tidestone')


def ruin(s, rnd):
    """Storytelling decay: roof holes, fallen columns, sand drifts, suspicious sand."""
    # holes in the hall roof (light shafts) and debris below
    for (hx, hz, r) in ((4, -10, 2.2), (-6, -17, 1.6), (7, -19, 1.3)):
        for x in range(hx - 3, hx + 4):
            for z in range(hz - 3, hz + 4):
                if math.hypot(x - hx, z - hz) <= r + rnd.random() * 0.8:
                    for y in (11, 12, 13):
                        if s.name_at(x, y, z):
                            del s.blocks[(x, y, z)]
                    if s.name_at(x, 3, z) == 'minecraft:water' and rnd.random() < 0.35:
                        s.set(x, 3, z, pick(rnd, [('raft:cracked_tidestone_bricks', 3),
                                                  ('raft:tidestone_brick_slab[type=bottom]', 2)]))
    # fallen columns in the courtyard
    fallen_column(s, 10, 3, 6, 'x', 4)
    fallen_column(s, 7, 3, 10, 'z', 3)
    fallen_column(s, -12, 3, 12, 'x', 3)
    # broken top of the west gate pylon
    for (x, y, z), b in list(s.blocks.items()):
        if x <= -4 and y >= 11 and 16 <= z <= 20 and rnd.random() < 0.7:
            del s.blocks[(x, y, z)]
    for (x, z) in ((-10, 18), (-11, 17), (-10, 19)):
        s.set(x, 3, z, pick(rnd, BRICKS))
    # sand drifts against walls, some suspicious sand with Tidewarden relics
    drifts = [(-16, -3), (-16, -2), (-15, -3), (16, 14), (15, 14), (16, 13), (-10, -7), (10, -7), (11, -7)]
    for i, (x, z) in enumerate(drifts):
        if s.name_at(x, 3, z) in (None, 'minecraft:water'):
            if i % 3 == 0:
                s.block_entity(x, 3, z, 'minecraft:suspicious_sand[dusted=0]', 'minecraft:brushable_block',
                               {'LootTable': 'minecraft:archaeology/ocean_ruin_warm'})
            else:
                s.set(x, 3, z, 'minecraft:sand')
    # crack the paving
    for (x, y, z), b in list(s.blocks.items()):
        if y == 2 and b[0] == 'raft:tidestone_bricks' and rnd.random() < 0.08:
            s.set(x, y, z, 'minecraft:sand' if rnd.random() < 0.5 else 'minecraft:gravel')


def flood(s):
    """Water in the open courtyard and under the colonnade roofs up to WATER_TOP (never on top of solid blocks
    above it), so terrain never fills the ruin."""
    (x0, y0, z0), (x1, y1, z1) = s.bounds()
    for x in range(-18, 19):
        for z in range(-22, 21):
            for y in range(3, WATER_TOP + 1):
                if (x, y, z) not in s.blocks:
                    s.set(x, y, z, 'minecraft:water')


def overgrowth(s, rnd):
    (x0, y0, z0), (x1, y1, z1) = s.bounds()
    # coral blocks taking over the lower courses of walls and columns
    for (x, y, z), b in list(s.blocks.items()):
        if 3 <= y <= 5 and b[0] in ('raft:tidestone_bricks', 'raft:cracked_tidestone_bricks',
                                    'raft:mossy_tidestone_bricks', 'raft:tidestone'):
            exposed = any(s.name_at(x + dx, y, z + dz) in (None, 'minecraft:water') for dx, dz in
                          ((1, 0), (-1, 0), (0, 1), (0, -1)))
            if exposed and rnd.random() < 0.10 * (6 - y) / 3:
                s.set(x, y, z, rnd.choice(CORAL_BLOCKS))
    # coral clusters on the stylobate edges and courtyard
    cands = [(x, y, z) for (x, y, z) in list(s.blocks) if s.free(x, y + 1, z) and s.is_solid(x, y, z) and
             s.blocks[(x, y, z)][0] != 'minecraft:water' and y + 1 <= WATER_TOP]
    rnd.shuffle(cands)
    placed = 0
    for (x, y, z) in cands:
        if placed > 420:
            break
        pos = (x, y + 1, z)
        if s.name_at(*pos) not in (None, 'minecraft:water'):
            continue
        r = rnd.random()
        c = rnd.choice(CORALS)
        if r < 0.22:
            s.set(*pos, f'minecraft:{c}_coral')
        elif r < 0.40:
            s.set(*pos, f'minecraft:{c}_coral_fan')
        elif r < 0.52:
            s.set(*pos, f'minecraft:sea_pickle[pickles={1 + int(rnd.random() * 4)}]')
        elif r < 0.62:
            s.set(*pos, pick(rnd, [('raft:sea_anemone_red', 3), ('raft:sea_anemone_purple', 3),
                                   ('raft:sea_anemone_glow', 2)]))
        elif r < 0.78:
            s.seagrass(*pos, tall=r < 0.70)
        elif r < 0.83 and y + 1 <= 4:
            s.kelp(*pos, 3 + int(rnd.random() * 5))
        elif r < 0.86:
            s.set(*pos, f'raft:starfish_{rnd.choice(["orange", "purple", "red"])}[face=floor,facing=north]')
        elif r < 0.88:
            s.set(*pos, f'raft:giant_clam[facing={rnd.choice(["north", "south", "east", "west"])},open=false,'
                        f'pearl=false]')
        elif r < 0.90:
            s.set(*pos, 'raft:sea_urchin')
        placed += 1
    # wall fans on vertical faces
    n = 0
    for (x, y, z), b in list(s.blocks.items()):
        if n > 140 or y > WATER_TOP or not s.is_solid(x, y, z):
            continue
        for dx, dz, f in ((1, 0, 'east'), (-1, 0, 'west'), (0, 1, 'south'), (0, -1, 'north')):
            if s.name_at(x + dx, y, z + dz) in (None, 'minecraft:water') and rnd.random() < 0.025:
                s.set(x + dx, y, z + dz, f'minecraft:{rnd.choice(CORALS)}_coral_wall_fan[facing={f}]')
                n += 1
                break
    s.grow_barnacles(((x0, 3, z0), (x1, WATER_TOP, z1)), 0.05, seed=3)
    # moray eels in the dark side chapels
    for (x, z) in ((-9, -13), (9, -11)):
        s.entity(x + 0.5, 3.2, z + 0.5, {'id': 'raft:moray_eel', 'PersistenceRequired': True})


def verify(s):
    rep = {}
    alt = [(p, b[1]) for p, b in s.blocks.items() if b[0] == 'raft:tidewarden_altar']
    rep['altar'] = alt
    rep['pedestals'] = sorted(p for p, b in s.blocks.items() if b[0] == 'raft:sigil_pedestal')
    rep['chests'] = sorted((b[2]['LootTable'], p) for p, b in s.blocks.items() if b[2] and 'LootTable' in b[2])
    # path from the gate to the altar must be open water
    blocked = [z for z in range(-17, 21) if s.name_at(0, 3, z) not in ('minecraft:water',) and
               not (s.name_at(0, 3, z) or '').startswith(('minecraft:', 'raft:sea', 'raft:star'))]
    rep['aisle_blocked'] = blocked
    rep['waterloggable_dry'] = sum(1 for p, b in s.blocks.items() if b[1].get('waterlogged') == 'false')
    rep['entities'] = [e[3]['id'] for e in s.entities]
    return rep


def write_json_files():
    structure_json(SID, f'raft:{SID}/start', 'surface_structures', 'beard_box', -1, 'OCEAN_FLOOR_WG', size=1,
                   max_distance=80, spawn_overrides={
                       'monster': spawns([('raft:moray_eel', 4, 1, 2), ('minecraft:drowned', 1, 1, 1)]),
                       'water_ambient': spawns([('raft:moray_eel', 3, 1, 1), ('minecraft:tropical_fish', 8, 3, 6),
                                                ('minecraft:pufferfish', 2, 1, 2)]),
                   })
    structure_set(SID, 32, 12, 31415961)
    template_pool(f'raft:{SID}/start', [(f'raft:{SID}/temple_0', 1, f'raft:{SID}')])
    processor_list(f'raft:{SID}', [NO_SPILL, {'processor_type': 'minecraft:rule', 'rules': [
        rule('raft:tidestone_bricks', 'raft:cracked_tidestone_bricks', probability=0.08),
        rule('raft:tidestone_bricks', 'raft:mossy_tidestone_bricks', probability=0.08),
        rule('raft:tidestone', 'raft:mossy_tidestone_bricks', probability=0.05),
    ]}])
    biome_tag(SID, ['coral_shallows', 'glowing_lagoon'])


def main():
    s = build()
    s.save(MODULE, f'{SID}/temple_0')
    write_json_files()
    return s


def preview(s):
    return pv(s, 'temple', cuts=(4, 8), slices_y=[2, 3, 4, 6, 8, 10, 11, 13])
