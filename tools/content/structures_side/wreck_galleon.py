"""wreck_galleon - Saltjaw's sunken pirate galleon ("the ghost ship").

The ship is first built upright (curved teak hull, keel, wales, gun deck with ship cannons and gun ports, cargo hold,
stern castle with the captain's cabin and gallery windows, forecastle, three masts with yards and torn sails, jolly
roger), then broken (breach in the hull, collapsed deck, snapped masts), then tilted onto its side (resample) and
sunk into the sea floor (silt), and finally overgrown (barnacles, kelp, seagrass, glow lichen, sea pickles).
Two variants differ in list angle, pitch, which masts broke and where the breach is.
Coordinates of the upright model: x across the beam (0 = centre line), y up (keel = 0), z from stern (0) to bow.
"""
import math
import random

from .lib import (Side, NO_SPILL_PLANTS, contact, overgrow, pick, resample, save, silt, spawns, write_structure)

L = 40
W = 6.4
HOLD, GUN, DECK = 1, 5, 9      # floor levels
QD, FC = 13, 12                # quarterdeck / forecastle floors
POOP, POOP_END = 17, 4         # poop deck floor over the stern cabin (z <= POOP_END)
STERN_END, FORE_START = 9, L - 9
MASTS = {'mizzen': (5, 28, 23, 4), 'main': (18, 36, 25, 7), 'fore': (29, 32, 22, 6)}  # z, top y, yard y, yard half

HULL = [('raft:teak_planks', 70), ('minecraft:dark_oak_planks', 16), ('minecraft:spruce_planks', 8),
        ('minecraft:stripped_dark_oak_wood', 6)]
DECKP = [('minecraft:spruce_planks', 60), ('raft:teak_planks', 25), ('minecraft:dark_oak_planks', 10),
         ('minecraft:stripped_spruce_wood', 5)]


def plan(z):
    t = z / (L - 1)
    if t < 0.12:
        return 0.84 + 0.16 * (t / 0.12)
    if t < 0.58:
        return 1.0
    u = (t - 0.58) / 0.42
    return max(0.0, math.cos(u * math.pi / 2)) ** 0.75


def keel(z):
    t = z / (L - 1)
    if t > 0.78:
        return round((t - 0.78) / 0.22 * 5)
    return 1 if t < 0.06 else 0


def top(z):
    if z <= POOP_END:
        return POOP + 1
    if z <= STERN_END:
        return QD + 1
    if z >= FORE_START:
        return FC + 1
    return DECK + 1


def half_width(z, y):
    h = y - keel(z)
    if h < 0:
        return -1
    f = min(1.0, (h + 0.9) / 5.2) ** 0.55
    w = W * plan(z) * f
    if y > DECK - 1:
        w -= 0.45 * (y - DECK + 1) / 3
    return w


def inside(x, y, z):
    if z < 0 or z >= L or y < keel(z) or y > top(z):
        return False
    return abs(x) <= half_width(z, y) + 0.5


def edge(z, y):
    e = -1
    for x in range(0, 9):
        if inside(x, y, z):
            e = x
    return e


def sail(s, rnd, z, x0, x1, y_top, y_bot, block='raft:sail_canvas', keep=0.85):
    """Torn sail hanging below a yard: ragged lower edge, holes, a few strips."""
    rag = {x: rnd.randint(0, 2) for x in range(x0, x1 + 1)}
    for x in range(x0, x1 + 1):
        for y in range(y_bot, y_top + 1):
            if y < y_bot + rag[x]:
                continue
            if rnd.random() > keep:
                continue
            st = block if rnd.random() < 0.8 else 'minecraft:white_wool'
            if block.endswith('red_striped') and rnd.random() < 0.25:
                st = 'minecraft:red_wool'
            s.set(x, y, z, st)


def build_upright(v, rnd):
    s = Side('wreck_galleon', seed=v)
    # ---------------------------------------------------------------- hull shell and decks
    for z in range(L):
        for y in range(0, top(z) + 1):
            for x in range(-8, 9):
                if not inside(x, y, z):
                    continue
                shell = (not inside(x - 1, y, z) or not inside(x + 1, y, z) or not inside(x, y, z - 1)
                         or not inside(x, y, z + 1) or not inside(x, y - 1, z))
                if shell:
                    if y == keel(z) and x == 0:
                        st = 'minecraft:dark_oak_log[axis=z]'
                    elif y <= keel(z) + 1:
                        st = pick(rnd, [('minecraft:dark_oak_planks', 6), ('raft:teak_planks', 4)])
                    elif y == top(z):
                        st = 'minecraft:stripped_dark_oak_wood'
                    elif 5 <= y <= DECK - 2 and abs(x) > 1:
                        st = pick(rnd, [('minecraft:mangrove_planks', 6), ('raft:teak_planks', 2),
                                        ('minecraft:dark_oak_planks', 1)])
                    elif y in (4, DECK - 1, QD - 1) and z not in (0,):
                        st = 'minecraft:stripped_dark_oak_log[axis=z]' if abs(x) > 1 else 'minecraft:stripped_dark_oak_wood'
                    else:
                        st = pick(rnd, HULL)
                    s.set(x, y, z, st)
                else:
                    if y == HOLD or y == GUN or y == DECK:
                        s.set(x, y, z, pick(rnd, DECKP))
                    elif z <= STERN_END and y == QD:
                        s.set(x, y, z, pick(rnd, DECKP))
                    elif z <= POOP_END and y == POOP:
                        s.set(x, y, z, pick(rnd, DECKP))
                    elif z >= FORE_START and y == FC:
                        s.set(x, y, z, pick(rnd, DECKP))
                    else:
                        s.set(x, y, z, 'minecraft:water')
    # bow stem post and figurehead
    for y in range(keel(L - 1), FC + 2):
        s.set(0, y, L - 1, 'minecraft:dark_oak_log[axis=y]')
    s.set(0, DECK - 1, L, 'raft:figurehead[facing=south]')
    s.line((0, FC + 1, L - 1), (0, FC + 5, L + 6), 'minecraft:stripped_spruce_log[axis=z]')   # bowsprit
    s.set(0, FC + 6, L + 6, 'minecraft:spruce_fence')
    # rails on top of the bulwarks
    for z in range(L):
        t = top(z)
        e = edge(z, t)
        for x in (-e, e):
            if e > 0 and rnd.random() < 0.8:
                s.set(x, t + 1, z, 'raft:teak_fence')
    # ---------------------------------------------------------------- stern castle: transom windows, bulkhead, cabin
    for x in range(-4, 5):
        for y in (11, 12):
            if inside(x, y, 0) and abs(x) < edge(0, y):
                st = 'minecraft:stripped_dark_oak_wood' if x % 2 == 0 else pick(
                    rnd, [('minecraft:glass_pane', 3), ('raft:cracked_glass_pane', 3), ('minecraft:water', 3)])
                s.set(x, y, 0, st)
    for x in range(-3, 4):
        s.set(x, QD - 3, 0, 'minecraft:dark_oak_slab[type=top]' if abs(x) < 3 else 'minecraft:stripped_dark_oak_wood')
    zb = STERN_END
    e = edge(zb, DECK + 1)
    for x in range(-e, e + 1):
        for y in range(DECK + 1, QD):
            s.set(x, y, zb, pick(rnd, HULL))
        if abs(x) == 2:
            s.set(x, DECK + 2, zb, pick(rnd, [('minecraft:glass_pane', 1), ('raft:cracked_glass_pane', 1)]))
    s.set(0, DECK + 1, zb, 'raft:teak_door[facing=south,half=lower,hinge=left,open=true,powered=false]')
    s.set(0, DECK + 2, zb, 'raft:teak_door[facing=south,half=upper,hinge=left,open=true,powered=false]')
    s.set(-2, DECK + 3, zb + 1, 'raft:ship_lantern[hanging=false]')
    s.set(2, DECK + 3, zb + 1, 'minecraft:soul_lantern[hanging=false]')
    # cabin interior (z 1..8, y 10..12)
    for z in range(2, 8):
        for x in (-1, 0, 1):
            s.set(x, DECK + 1, z, 'minecraft:red_carpet')
    s.set(0, DECK + 1, 3, 'raft:chart_table[facing=south]')
    s.set(0, DECK + 2, 3, 'raft:rum_bottles[bottles=2,facing=east]')
    s.set(1, DECK + 1, 3, 'raft:driftwood_chair[facing=west]')
    ce = edge(1, DECK + 1)
    s.chest(-ce + 1, DECK + 1, 1, 'raft:chests/wreck_galleon', facing='south', block='raft:treasure_chest')
    s.set(-ce + 2, DECK + 1, 1, 'raft:gold_coin_pile[layers=3]')
    s.set(-ce + 1, DECK + 1, 2, 'raft:gold_coin_pile[layers=1]')
    s.set(ce - 1, DECK + 1, 2, 'minecraft:red_bed[facing=north,occupied=false,part=foot]')
    s.set(ce - 1, DECK + 1, 1, 'minecraft:red_bed[facing=north,occupied=false,part=head]')
    e6 = edge(6, DECK + 1)
    s.set(-e6 + 1, DECK + 1, 6, 'minecraft:bookshelf')
    s.set(-e6 + 1, DECK + 2, 6, 'raft:driftwood_shelf[facing=east]')
    s.set(e6 - 1, DECK + 1, 7, 'raft:spyglass_stand[facing=north]')
    s.set(-2, DECK + 2, zb - 1, 'raft:wanted_poster[facing=north]')
    s.set(2, DECK + 1, 6, 'minecraft:skeleton_skull[rotation=6]')
    s.set(0, QD - 1, 2, 'raft:ship_lantern[hanging=true]')
    s.set(0, QD - 1, 7, 'minecraft:soul_lantern[hanging=true]')
    # quarterdeck: wheel, stairs down to the main deck, jolly roger at the stern rail
    s.set(0, QD + 1, 7, 'minecraft:stripped_spruce_log[axis=y]')
    s.set(0, QD + 1, 8, 'raft:ship_wheel[facing=south]')
    for side in (-3, 3):
        for i in range(4):
            s.set(side, DECK + 1 + i, zb + 4 - i, 'raft:teak_stairs[facing=north,half=bottom]')
            s.set(side, DECK + 1 + i - 1, zb + 4 - i, 'raft:teak_planks') if i > 0 else None
        s.set(side, QD, zb, 'minecraft:water')
        s.set(side, QD + 1, zb, 'minecraft:water')
    for x in range(-3, 4):                       # upper stern windows (navigator's room under the poop deck)
        if inside(x, QD + 2, 0) and abs(x) < edge(0, QD + 2):
            s.set(x, QD + 2, 0, 'minecraft:stripped_dark_oak_wood' if x % 2 == 0 else pick(
                rnd, [('minecraft:glass_pane', 2), ('raft:cracked_glass_pane', 2), ('minecraft:water', 1)]))
    e4 = edge(POOP_END, QD + 1)
    for x in range(-e4 + 1, e4):
        for y in range(QD + 1, POOP):
            s.set(x, y, POOP_END, pick(rnd, HULL) if x != 0 or y > QD + 2 else 'minecraft:water')
    s.set(2, QD + 2, POOP_END, 'minecraft:glass_pane')
    s.set(-2, QD + 2, POOP_END, 'raft:cracked_glass_pane')
    s.set(-2, QD + 1, 1, 'raft:chart_table[facing=east]')
    s.set(2, QD + 1, 1, 'minecraft:barrel[facing=up,open=false]')
    s.set(0, POOP - 1, 2, 'minecraft:soul_lantern[hanging=true]')
    for x in (-3, 0, 3):                          # great stern lanterns
        s.set(x, POOP + 2, 0, 'minecraft:stripped_dark_oak_log[axis=y]')
        s.set(x, POOP + 3, 0, 'raft:ship_lantern[hanging=false]')
    for y in range(POOP + 1, POOP + 6):           # ensign staff with the jolly roger
        s.set(0, y, 2, 'minecraft:spruce_fence' if y > POOP + 1 else 'minecraft:stripped_spruce_log[axis=y]')
    s.set(0, POOP + 5, 1, 'raft:jolly_roger[facing=north]')
    # ---------------------------------------------------------------- forecastle
    zf = FORE_START
    e = edge(zf, DECK + 1)
    for x in range(-e, e + 1):
        for y in range(DECK + 1, FC):
            s.set(x, y, zf, pick(rnd, HULL))
    s.set(0, DECK + 1, zf, 'minecraft:water')
    s.set(0, DECK + 2, zf, 'minecraft:water')
    s.set(1, DECK + 2, zf - 1, 'raft:ship_bell[facing=north]')
    for i in range(3):
        s.set(-3, DECK + 1 + i, zf - 3 + i, 'raft:teak_stairs[facing=south,half=bottom]')
    s.set(-3, FC, zf, 'minecraft:water')
    s.set(-3, FC + 1, zf, 'minecraft:water')
    for (x, z) in ((1, zf + 2), (-1, zf + 3), (2, zf + 4)):
        s.set(x, DECK + 1, z, pick(rnd, [('minecraft:barrel[facing=up,open=false]', 2),
                                         ('raft:wooden_crate[facing=north,open=false]', 2),
                                         ('raft:powder_keg', 1)]))
    s.set(0, FC - 1, zf + 3, 'raft:ship_lantern[hanging=true]')
    ef = edge(zf + 2, DECK + 1)
    for y in (DECK + 1, DECK + 2):
        s.set(ef - 1, y, zf + 1, 'raft:cargo_net[facing=west]')
    # ---------------------------------------------------------------- gun deck: cannons through ports, hammocks
    gy = GUN + 1
    for z in range(11, 29, 4):
        e = edge(z, gy)
        for sx, face in ((1, 'east'), (-1, 'west')):
            for y in (gy, gy + 1):
                s.set(sx * e, y, z, 'minecraft:water')
            s.set(sx * e, gy + 2, z, 'minecraft:stripped_dark_oak_wood')
            s.set(sx * (e - 1), gy, z, f'raft:ship_cannon[facing={face}]')
        if z + 2 < 28:
            hx = rnd.choice((-2, 2))
            s.set(hx, gy, z + 1, 'raft:hammock[facing=south,occupied=false,part=foot]')
            s.set(hx, gy, z + 2, 'raft:hammock[facing=south,occupied=false,part=head]')
        if (z // 4) % 2 == 0:
            s.set(0, DECK - 1, z + 2, 'raft:ship_lantern[hanging=true]')
        s.set(rnd.choice((-1, 1)), gy, z + 2, 'raft:powder_keg')
    # main deck cannons in bulwark openings
    for z in (13, 24):
        e = edge(z, DECK + 1)
        for sx, face in ((1, 'east'), (-1, 'west')):
            s.set(sx * e, DECK + 1, z, 'minecraft:water')
            s.set(sx * e, DECK + 2, z, 'minecraft:water')
            s.set(sx * (e - 1), DECK + 1, z, f'raft:ship_cannon[facing={face}]')
    # main hatch with grating, capstan
    for z in (15, 16):
        for x in (-1, 0, 1):
            s.set(x, DECK, z, 'minecraft:water')
            s.set(x, GUN, z, 'minecraft:water')
    s.set(-1, DECK, 15, 'minecraft:spruce_trapdoor[facing=north,half=top,open=false]')
    s.set(0, DECK, 15, 'minecraft:spruce_trapdoor[facing=north,half=top,open=false]')
    s.set(0, DECK + 1, 22, 'minecraft:stripped_spruce_log[axis=y]')
    s.set(0, DECK + 2, 22, 'minecraft:spruce_fence')
    # ---------------------------------------------------------------- hold: cargo, ballast, bones
    for z in range(4, 34, 6):
        for y in list(range(HOLD + 1, GUN)) + list(range(GUN + 1, DECK)):
            if inside(0, y, z) and s.get(0, y, z) == 'minecraft:water':
                s.set(0, y, z, 'minecraft:stripped_dark_oak_log[axis=y]')
    hy = HOLD + 1
    cargo = [('minecraft:barrel[facing=up,open=false]', 4), ('raft:wooden_crate[facing=north,open=false]', 3),
             ('raft:powder_keg', 2), ('minecraft:gravel', 1)]
    for z in range(8, 31):
        e = edge(z, hy)
        for x in range(-e + 1, e):
            if s.get(x, hy, z) != 'minecraft:water' or abs(x) < 1:
                continue
            r = rnd.random()
            if r < 0.30:
                s.set(x, hy, z, pick(rnd, cargo))
                if rnd.random() < 0.35 and s.get(x, hy + 1, z) == 'minecraft:water':
                    s.set(x, hy + 1, z, pick(rnd, cargo[:2]))
            elif r < 0.36:
                s.set(x, hy, z, f'raft:gold_coin_pile[layers={rnd.randint(1, 3)}]')
    s.chest(-2, hy, 19, 'raft:chests/wreck_galleon', facing='east')
    s.set(-2, hy, 20, 'raft:gold_coin_pile[layers=2]')
    s.set(2, hy, 12, 'minecraft:bone_block[axis=x]')
    s.set(1, hy, 26, 'minecraft:skeleton_skull[rotation=3]')
    s.set(0, GUN - 1, 20, 'minecraft:soul_lantern[hanging=true]')
    s.set(0, GUN - 1, 10, 'minecraft:soul_lantern[hanging=true]')
    # ---------------------------------------------------------------- masts, yards, sails
    broken = {'main': (28, 33)[v % 2], 'fore': (20, 99)[v % 2], 'mizzen': (99, 20)[v % 2]}
    for name, (mz, mtop, yard, yh) in MASTS.items():
        tip = min(mtop, broken[name])
        for y in range(HOLD + 1, tip + 1):
            s.set(0, y, mz, 'minecraft:spruce_log[axis=y]')
        if broken[name] < mtop:
            s.set(0, tip + 1, mz, 'minecraft:spruce_fence')       # splinters
            s.set(1, tip, mz, 'minecraft:spruce_fence')
        else:
            s.set(0, mtop + 1, mz, 'minecraft:spruce_fence')
        if yard <= tip:
            for x in range(-yh, yh + 1):
                if rnd.random() < 0.92:
                    s.set(x, yard, mz + 1, 'minecraft:stripped_spruce_log[axis=x]')
            blk = 'raft:sail_canvas_red_striped' if name == 'mizzen' else 'raft:sail_canvas'
            sail(s, rnd, mz + 2, -yh + 1, yh - 1, yard - 1, yard - 9 if name == 'main' else yard - 7, blk,
                 keep=0.8 if name != 'fore' else 0.55)
        if tip >= mtop - 1:                           # topsail yard on intact masts
            for x in range(-yh + 2, yh - 1):
                s.set(x, mtop - 3, mz + 1, 'minecraft:stripped_spruce_log[axis=x]')
            sail(s, rnd, mz + 2, -yh + 3, yh - 3, mtop - 4, mtop - 7, 'raft:sail_canvas', keep=0.7)
        if name == 'main' and tip >= 28:              # crow's nest
            for x in range(-2, 3):
                for z in range(mz - 2, mz + 3):
                    if (abs(x) == 2 or abs(z - mz) == 2) and rnd.random() < 0.85:
                        s.set(x, 27, z, 'minecraft:spruce_slab[type=bottom]')
                        if abs(x) == 2 and abs(z - mz) == 2:
                            s.set(x, 28, z, 'minecraft:spruce_fence')

    return s


def wreck(s, v, rnd):
    """Breach, collapsed deck, decay of the upright model."""
    side = -1 if v % 2 == 0 else 1          # breach on the upper (visible) side after the roll
    z0 = 20 + 2 * (v % 2)
    for z in range(z0, z0 + 6):
        for y in range(2, 8):
            for x in range(-9, 10):
                if x * side <= 0:
                    continue
                d = abs(z - (z0 + 2.5)) / 3.2 + abs(y - 4.5) / 3.5
                if d < 1.0 + rnd.random() * 0.35 and s.get(x, y, z) is not None:
                    s.set(x, y, z, 'minecraft:water')
    for z in range(z0 + 1, z0 + 5):
        for x in range(-4, 5):
            if s.get(x, DECK, z) and rnd.random() < 0.75:
                s.set(x, DECK, z, 'minecraft:water')
                if rnd.random() < 0.4:
                    s.set(x, GUN + 1, z, rnd.choice(['minecraft:spruce_slab[type=bottom]',
                                                     'raft:teak_slab[type=bottom]', 'minecraft:spruce_planks']))
    # random rot holes in the upper hull and decks
    for pos, (name, props, nbt) in list(s.blocks.items()):
        x, y, z = pos
        if name in ('raft:teak_planks', 'minecraft:dark_oak_planks', 'minecraft:spruce_planks') and y >= 6:
            if rnd.random() < 0.035 + 0.01 * (y - 6):
                s.set(x, y, z, 'minecraft:water' if inside(x, y, z) else None)
        elif name == 'raft:teak_fence' and rnd.random() < 0.3:
            s.set(x, y, z, None)


def fallen_mast(s, rnd, x, y, z0, z1, yard_z):
    """Snapped topmast lying on the sea floor with its yard and the sail draped over the sand."""
    for z in range(z0, z1 + 1):
        s.set(x, y, z, 'minecraft:spruce_log[axis=z]')
    for dx in range(-4, 5):
        if rnd.random() < 0.85:
            s.set(x + dx, y, yard_z, 'minecraft:stripped_spruce_log[axis=x]')
    for dx in range(-3, 4):
        for dz in range(1, 5):
            if rnd.random() < 0.7 and s.get(x + dx, y, yard_z + dz) is None:
                s.set(x + dx, y, yard_z + dz, 'minecraft:white_carpet')
    s.set(x + 1, y, z1, 'minecraft:spruce_fence')


VARIANTS = [dict(roll=19, pitch=-4, yaw=0), dict(roll=-24, pitch=5, yaw=0)]


def build(v):
    rnd = random.Random(31415900 + v)
    up = build_upright(v, rnd)
    wreck(up, v, rnd)
    p = VARIANTS[v]
    s = resample(up, roll=p['roll'], pitch=p['pitch'], pivot=(0, 0, L // 2))
    (x0, y0, z0), (x1, y1, z1) = s.bounds()
    # move so that the lowest hull block is at y=0 and x/z start at 0
    moved = Side(s.name, seed=v)
    for (x, y, z), b in s.blocks.items():
        moved.blocks[(x - x0, y - y0, z - z0)] = b
    s = moved
    (x0, y0, z0), (x1, y1, z1) = s.bounds()
    floor = 2
    silt(s, floor, depth=2, seed=v + 3)
    # fallen topmast on the low side of the hull
    low_x = x1 + 3 if p['roll'] > 0 else x0 - 3
    fallen_mast(s, rnd, low_x, floor + 1, 9, 22, 16)
    # spilled cargo through the breach on the floor
    for i in range(7):
        x = rnd.randint(x0 + 2, x1 - 2)
        z = rnd.randint(z0 + 14, z0 + 30)
        for y in range(floor + 1, floor + 4):
            if s.get(x, y, z) is None:
                s.set(x, y, z, pick(rnd, [('minecraft:barrel[facing=up,open=false]', 2),
                                          ('raft:wooden_crate[facing=east,open=false]', 2),
                                          ('raft:gold_coin_pile[layers=1]', 1)]))
                break
    overgrow(s, seed=v * 7 + 1, barnacle=0.07, kelp=0.035, kelp_h=(5, 14), grass=0.12, pickles=0.02, anemone=0.006,
             kelp_max_y=floor + 2)
    # ghostly glow: some barnacles become glow lichen
    for pos, (name, props, nbt) in list(s.blocks.items()):
        if name == 'raft:barnacles' and rnd.random() < 0.10:
            q = dict(props)
            s.blocks[pos] = ('minecraft:glow_lichen', q, nbt)
    return s


def main(preview=False):
    out = []
    for v in range(len(VARIANTS)):
        s = build(v)
        path = f'wreck_galleon/galleon_{v}'
        save(s, path, floor_y=2)
        out.append((path, s))
        if preview:
            (a, b, c), (d, e, f) = s.bounds()
            print('  preview', contact(s, f'wreck_galleon_{v}', cuts=(9, 14), slices_y=[3, 7, 11, 15]))
    write_structure('wreck_galleon', [(p, 1) for p, _ in out], [NO_SPILL_PLANTS],
                    step='surface_structures', terrain='beard_thin', start_height=-3, heightmap='OCEAN_FLOOR_WG',
                    biomes=['wreck_graveyard', 'raft_ocean_biome'], spacing=26, separation=9, salt=31415901,
                    spawn_overrides={'monster': spawns([('raft:drowned_corsair', 6, 1, 2), ('minecraft:drowned', 2, 1, 1)])},
                    max_distance=80)
    return out
