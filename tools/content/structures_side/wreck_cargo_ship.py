"""wreck_cargo_ship - a rusty steel freighter of the Meridian supply line, broken in two on the sea floor.

Built upright as one ship (box hull with rounded bilge, raked bow with forecastle, round stern with rudder and
propeller, white superstructure with mess, cabins and bridge, funnel, radar mast, containers on deck, engine room,
cargo hold), then cut at the break: the stern half settles with its broken end down, the bow half lies yawed and
listing beside it. Spilled shipping containers, cargo and twisted beams fill the gap. Flooded, rusty, overgrown.
Upright model: x across (0 = centre), y up (keel 0, main deck 8), z from stern (0) to bow (L-1).
"""
import math
import random

from .lib import (Side, NO_SPILL_PLANTS, contact, overgrow, pick, resample, save, silt, spawns, write_structure)

L = 52
HW = 5.5
DECK = 8
FC = 10
BREAK = (25, 29)        # stern part z < 25, bow part z >= 29
SUPER = (5, 13)         # superstructure z range
CONT = ['raft:shipping_container_red', 'raft:shipping_container_blue', 'raft:shipping_container_green']
LOWER = [('raft:rusty_hull', 7), ('raft:riveted_hull', 2), ('raft:rusted_plate', 1)]
UPPER = [('raft:riveted_hull', 7), ('raft:rusty_hull', 3)]
WHITE = [('minecraft:white_concrete', 6), ('minecraft:light_gray_concrete', 2), ('raft:rusty_hull', 1),
         ('minecraft:white_terracotta', 1)]
DECKP = [('raft:rusted_plate', 8), ('raft:rusty_hull', 1), ('minecraft:iron_block', 0)]


def keel(z):
    if z > L - 7:
        return min(4, z - (L - 7))
    if z < 3:
        return 3 - z
    return 0


def top(z):
    return FC + 1 if z >= L - 7 else DECK + 1


def half_width(z, y):
    h = y - keel(z)
    if h < 0:
        return -1
    base = {0: 3.4, 1: 4.6}.get(h, HW)
    t = z / (L - 1)
    if t > 0.74:
        u = (t - 0.74) / 0.26
        f = max(0.0, math.cos(u * math.pi / 2)) ** (0.9 - 0.5 * min(1, y / 10))
        base *= f
    elif z < 4:
        base *= 0.78 + 0.22 * z / 4
    return base


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


def container(s, x0, y0, z0, axis, color, length=6, open_end=False, rnd=None):
    """2 wide, 2 high, `length` long container box of ribbed walls (hollow, water inside)."""
    ln, wd = (length, 2)
    for i in range(ln):
        for j in range(wd):
            for k in range(2):
                x, z = (x0 + j, z0 + i) if axis == 'z' else (x0 + i, z0 + j)
                s.set(x, y0 + k, z, f'{color}[axis={axis}]')
    if open_end and rnd is not None:
        for j in range(wd):
            for k in range(2):
                x, z = (x0 + j, z0 + ln - 1) if axis == 'z' else (x0 + ln - 1, z0 + j)
                s.set(x, y0 + k, z, 'minecraft:water')


def build_upright(rnd):
    s = Side('wreck_cargo_ship')
    for z in range(L):
        for y in range(0, top(z) + 1):
            for x in range(-7, 8):
                if not inside(x, y, z):
                    continue
                shell = (not inside(x - 1, y, z) or not inside(x + 1, y, z) or not inside(x, y, z - 1)
                         or not inside(x, y, z + 1) or not inside(x, y - 1, z))
                if shell:
                    if y == top(z):
                        st = 'raft:riveted_hull'
                    elif y == 4:
                        st = 'minecraft:white_concrete' if rnd.random() < 0.7 else 'raft:rusty_hull'  # boot top line
                    else:
                        st = pick(rnd, LOWER if y < 4 else UPPER)
                    s.set(x, y, z, st)
                elif y == DECK or (z >= L - 7 and y == FC):
                    s.set(x, y, z, pick(rnd, DECKP))
                elif y == 0 + keel(z) + 1 and z < SUPER[1] + 1:
                    s.set(x, y, z, 'raft:rusted_plate')
                else:
                    s.set(x, y, z, 'minecraft:water')
    # railings
    for z in range(L):
        e = edge(z, top(z))
        if e > 0:
            for x in (-e, e):
                s.set(x, top(z) + 1, z, 'raft:steel_railing')
    # rudder + propeller
    for y in range(1, 6):
        s.set(0, y, -1, 'raft:rusted_plate')
        s.set(0, y, -2, 'raft:rusty_hull')
    s.set(0, 2, 0, 'minecraft:iron_block')
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        s.set(dx, 2 + dy, -1, 'minecraft:oxidized_cut_copper')
    # bulkheads (hold / engine room)
    for z in (SUPER[1] + 2, BREAK[1] + 16):
        for x in range(-6, 7):
            for y in range(1, DECK):
                if inside(x, y, z):
                    s.set(x, y, z, pick(rnd, [('raft:riveted_hull', 3), ('raft:rusty_hull', 2)]))
        s.set(0, 2, z, 'raft:bulkhead_door[facing=north,half=lower,hinge=left,open=true,powered=false]')
        s.set(0, 3, z, 'raft:bulkhead_door[facing=north,half=upper,hinge=left,open=true,powered=false]')
    # ---------------------------------------------------------------- engine room (stern, below deck)
    for z in range(5, 12):
        for x in range(-2, 3):
            for y in range(2, 4):
                s.set(x, y, z, pick(rnd, [('minecraft:iron_block', 3), ('minecraft:polished_deepslate', 2),
                                          ('raft:rusty_hull', 1)]))
    for z in (6, 8, 10):
        for y in range(4, DECK):
            s.set(-1 if z != 8 else 1, y, z, 'raft:rusty_pipe[axis=y]' if y != 5 else 'raft:rusty_pipe_valve[axis=y]')
    for z in range(3, 14):
        s.set(-3, DECK - 1, z, 'raft:rusty_pipe[axis=z]')
        s.set(3, DECK - 1, z, 'raft:rusty_pipe[axis=z]' if z % 4 else 'raft:rusty_pipe_valve[axis=z]')
    for z in range(4, 13):
        for x in (-4, 4):
            if s.get(x, 4, z) == 'minecraft:water':
                s.set(x, 4, z, 'raft:catwalk[type=bottom]')
    s.chest(4, 2, 13, 'raft:chests/wreck_cargo', facing='west', block='minecraft:barrel')
    s.set(-4, 2, 12, 'minecraft:blast_furnace[facing=east,lit=false]')
    s.set(-4, 2, 11, 'minecraft:anvil[facing=east]')
    s.set(0, DECK - 1, 4, 'minecraft:oxidized_copper_lantern[hanging=true]')
    s.set(0, DECK - 1, 12, 'minecraft:oxidized_copper_lantern[hanging=true]')
    # ---------------------------------------------------------------- superstructure (3 levels + bridge roof)
    z0, z1 = SUPER
    levels = [DECK, DECK + 4, DECK + 8]
    roof = DECK + 12
    for z in range(z0, z1 + 1):
        for x in range(-4, 5):
            for y in range(DECK + 1, roof + 1):
                wall = abs(x) == 4 or z in (z0, z1)
                if y in levels or y == roof:
                    s.set(x, y, z, pick(rnd, WHITE) if wall else pick(rnd, [('raft:rusted_plate', 4),
                                                                            ('minecraft:white_concrete', 1)]))
                elif wall:
                    win = (y - DECK) % 4 == 2 and ((z in (z0, z1) and abs(x) % 2 == 1 and abs(x) < 4)
                                                   or (abs(x) == 4 and z % 2 == 0 and z0 < z < z1))
                    if win:
                        s.set(x, y, z, pick(rnd, [('raft:cracked_glass_pane', 3), ('minecraft:glass_pane', 2),
                                                  ('minecraft:water', 3)]))
                    else:
                        s.set(x, y, z, pick(rnd, WHITE))
                else:
                    s.set(x, y, z, 'minecraft:water')
    # bridge: wide windows to the front, wings
    by = DECK + 9
    for x in range(-3, 4):
        s.set(x, by + 1, z1, pick(rnd, [('raft:cracked_glass_pane', 2), ('minecraft:water', 3)]))
        s.set(x, by, z1, pick(rnd, [('raft:cracked_glass_pane', 2), ('minecraft:glass_pane', 1), ('minecraft:water', 2)]))
    for x in (-6, -5, 5, 6):
        for z in (z1 - 2, z1 - 1, z1):
            s.set(x, by - 1, z, 'raft:rusted_plate')
        s.set(x, by, z1, 'raft:steel_railing')
        s.set(x, by, z1 - 2, 'raft:steel_railing')
    for x in (-6, 6):
        s.set(x, by, z1 - 1, 'raft:steel_railing')
    for x in (-2, 0, 2):
        s.set(x, by, z1 - 1, 'raft:control_panel[facing=south]')
    s.set(-3, by, z1 - 3, 'raft:chart_table[facing=east]')
    s.set(3, by, z1 - 3, 'raft:expedition_radio[facing=west]')
    s.set(3, by - 0, z0 + 1, 'raft:server_rack[facing=south]')
    s.set(0, roof - 1, z0 + 4, 'minecraft:oxidized_copper_lantern[hanging=true]')
    # cabins (level 2)
    cy = DECK + 5
    s.set(-3, cy, z0 + 2, 'minecraft:light_gray_bed[facing=north,occupied=false,part=foot]')
    s.set(-3, cy, z0 + 1, 'minecraft:light_gray_bed[facing=north,occupied=false,part=head]')
    s.set(3, cy, z0 + 2, 'minecraft:light_gray_bed[facing=north,occupied=false,part=foot]')
    s.set(3, cy, z0 + 1, 'minecraft:light_gray_bed[facing=north,occupied=false,part=head]')
    s.chest(3, cy, z1 - 1, 'raft:chests/wreck_cargo', facing='west')
    s.set(-3, cy, z1 - 1, 'minecraft:barrel[facing=up,open=false]')
    s.set(0, cy, z0 + 4, 'raft:driftwood_table')
    s.set(-1, cy, z0 + 4, 'raft:driftwood_chair[facing=east]')
    s.set(0, cy + 2, z0 + 4, 'minecraft:lantern[hanging=true]')
    # mess (level 1)
    my = DECK + 1
    s.set(-3, my, z0 + 1, 'minecraft:smoker[facing=east,lit=false]')
    s.set(-3, my, z0 + 2, 'minecraft:furnace[facing=east,lit=false]')
    s.set(-3, my, z0 + 3, 'minecraft:barrel[facing=east,open=false]')
    for z in (z0 + 4, z0 + 5, z0 + 6):
        s.set(1, my, z, 'minecraft:spruce_slab[type=top]')
    s.set(0, my, z0 + 5, 'raft:driftwood_chair[facing=east]')
    s.set(2, my, z0 + 4, 'raft:driftwood_chair[facing=west]')
    # doors to the deck and a ladder shaft
    s.set(0, DECK + 1, z1, 'raft:bulkhead_door[facing=south,half=lower,hinge=left,open=true,powered=false]')
    s.set(0, DECK + 2, z1, 'raft:bulkhead_door[facing=south,half=upper,hinge=left,open=true,powered=false]')
    for y in range(DECK + 1, roof):
        s.set(-3, y, z0 + 5, 'minecraft:ladder[facing=east]')
        if y in levels:
            s.set(-3, y, z0 + 5, 'minecraft:water')
    # funnel
    for y in range(DECK + 1, roof + 5):
        for x in range(-2, 3):
            for z in range(1, 5):
                d = math.hypot(x, (z - 2.5) * 1.15)
                if d <= 2.1:
                    if d > 1.1:
                        st = 'minecraft:black_concrete' if y >= roof + 3 else (
                            'minecraft:red_terracotta' if rnd.random() < 0.8 else 'raft:rusty_hull')
                        s.set(x, y, z, st)
                    else:
                        s.set(x, y, z, 'minecraft:water')
    # radar mast on the roof
    for y in range(roof + 1, roof + 7):
        s.set(0, y, z0 + 4, 'raft:steel_beam[axis=y]')
    s.set(0, roof + 7, z0 + 4, 'raft:satellite_dish[facing=south]')
    s.set(1, roof + 5, z0 + 4, 'raft:steel_beam[axis=x]')
    s.set(-1, roof + 5, z0 + 4, 'raft:steel_beam[axis=x]')
    for x in range(-4, 5):
        s.set(x, roof + 1, z0, 'raft:steel_railing')
        s.set(x, roof + 1, z1, 'raft:steel_railing')
    for z in range(z0, z1 + 1):
        s.set(-4, roof + 1, z, 'raft:steel_railing')
        s.set(4, roof + 1, z, 'raft:steel_railing')
    s.set(4, DECK + 2, z1 + 1, 'raft:life_ring[facing=south]')
    # ---------------------------------------------------------------- deck containers (stern part)
    for i, z in enumerate((16, 22)):
        for j, x in enumerate((-4, -1, 2)):
            h = 1 + (i + j) % 2
            for k in range(h):
                if rnd.random() < 0.85:
                    container(s, x, DECK + 1 + 2 * k, z, 'z', rnd.choice(CONT), length=5)
    # ---------------------------------------------------------------- bow part: hold, forecastle
    for z in range(31, 44):
        for x in range(-3, 4):
            if rnd.random() < 0.7:
                s.set(x, DECK, z, 'minecraft:water')          # missing hatch covers
    for z in range(31, 44, 6):
        for x in (-4, -1, 2):
            if rnd.random() < 0.8:
                container(s, x, 1, z, 'z', rnd.choice(CONT), length=5)
                if rnd.random() < 0.5:
                    container(s, x, 3, z, 'z', rnd.choice(CONT), length=5, open_end=True, rnd=rnd)
    s.chest(4, 1, 42, 'raft:chests/wreck_cargo', facing='west', block='raft:wooden_crate')
    for z in range(L - 7, L - 1):
        e = edge(z, FC - 1)
        if e > 1:
            s.set(e, FC - 1, z, 'raft:rusty_hull')
    s.set(0, FC + 1, L - 5, 'minecraft:polished_deepslate')        # windlass
    s.set(1, FC + 1, L - 5, 'minecraft:iron_chain[axis=x]')
    s.set(-1, FC + 1, L - 5, 'minecraft:iron_chain[axis=x]')
    ea = edge(L - 4, 6)
    s.set(ea + 1, 6, L - 4, 'raft:ship_anchor[facing=east]')
    for y in range(FC + 1, FC + 8):
        s.set(0, y, L - 7, 'raft:steel_beam[axis=y]')
    s.set(0, FC + 8, L - 7, 'minecraft:lantern[hanging=false]')
    return s


def break_ship(up, rnd):
    """Split into the stern and bow halves with ragged broken ends and protruding beams."""
    stern, bow = Side('stern'), Side('bow')
    a, b = BREAK
    rag = {}
    for (x, y, z), blk in up.blocks.items():
        key = (x, y)
        if key not in rag:
            rag[key] = (rnd.randint(-2, 1), rnd.randint(-1, 2))
        if z < a + rag[key][0]:
            stern.blocks[(x, y, z)] = blk
        elif z >= b + rag[key][1]:
            bow.blocks[(x, y, z)] = blk
    for part, zz, d in ((stern, a - 1, 1), (bow, b, -1)):
        for _ in range(7):
            x, y = rnd.randint(-4, 4), rnd.randint(1, DECK)
            for i in range(rnd.randint(1, 3)):
                part.set(x, y, zz + d * i, 'raft:steel_beam[axis=z]')
        for _ in range(3):
            x, y = rnd.randint(-4, 4), rnd.randint(2, DECK - 1)
            for i in range(2):
                part.set(x, y, zz + d * i, 'raft:rusty_pipe[axis=z]')
    return stern, bow


def place(dst, part, dx, dz):
    (x0, y0, z0), _ = part.bounds()
    for (x, y, z), blk in part.blocks.items():
        dst.blocks[(x - x0 + dx, y - y0, z - z0 + dz)] = blk


def build(v=0):
    rnd = random.Random(31415902)
    up = build_upright(rnd)
    stern, bow = break_ship(up, rnd)
    stern = resample(stern, roll=6, pitch=-7, pivot=(0, 0, 0))
    bow = resample(bow, roll=-15, pitch=8, yaw=-24, pivot=(0, 0, L - 1))
    s = Side('wreck_cargo_ship', seed=2)
    place(s, stern, 0, 0)
    (_, _, _), (sx1, _, sz1) = s.bounds()
    place(s, bow, -2, sz1 + 4)
    (x0, y0, z0), (x1, y1, z1) = s.bounds()
    floor = 2
    silt(s, floor, depth=2, seed=5)
    # spilled containers and cargo in the gap and around
    spots = [(x0 - 1, sz1 - 1, 'x'), (x1 - 4, sz1 + 2, 'z'), (x0 + 3, sz1 + 1, 'x'), (x1 + 1, sz1 - 8, 'z'),
             (x0 - 3, sz1 + 10, 'z')]
    for i, (x, z, ax) in enumerate(spots):
        y = floor + (0 if i % 2 else 1)
        container(s, x, y, z, ax, CONT[i % 3], length=6, open_end=(i == 2), rnd=rnd)
        if i == 1:
            container(s, x, y + 2, z + 1, ax, CONT[(i + 1) % 3], length=5)
    for i in range(10):
        x = rnd.randint(x0, x1)
        z = rnd.randint(sz1 - 3, sz1 + 8)
        for y in range(floor + 1, floor + 4):
            if s.get(x, y, z) is None:
                s.set(x, y, z, pick(rnd, [('minecraft:barrel[facing=up,open=false]', 3),
                                          ('raft:wooden_crate[facing=north,open=false]', 3),
                                          ('raft:plastic_block', 1), ('minecraft:white_wool', 1)]))
                break
    s.chest(x0 + 4, floor + 1, sz1 + 2, 'raft:chests/wreck_cargo', facing='east', block='raft:wooden_crate')
    overgrow(s, seed=11, barnacle=0.08, kelp=0.04, kelp_h=(5, 15), grass=0.12, pickles=0.02, kelp_max_y=floor + 2)
    for pos, (name, props, nbt) in list(s.blocks.items()):
        if name == 'raft:barnacles' and rnd.random() < 0.06:
            s.blocks[pos] = ('minecraft:glow_lichen', dict(props), nbt)
    return s


def main(preview=False):
    s = build()
    path = 'wreck_cargo_ship/freighter_0'
    save(s, path, floor_y=2)
    if preview:
        print('  preview', contact(s, 'wreck_cargo_ship', cuts=(10, 16), slices_y=[3, 9, 14, 18]))
    write_structure('wreck_cargo_ship', [(path, 1)], [NO_SPILL_PLANTS],
                    step='surface_structures', terrain='beard_thin', start_height=-3, heightmap='OCEAN_FLOOR_WG',
                    biomes=['wreck_graveyard', 'raft_ocean_biome', 'kelp_forest'], spacing=24, separation=8,
                    salt=31415902,
                    spawn_overrides={'monster': spawns([('minecraft:drowned', 4, 1, 2), ('raft:drowned_corsair', 1, 1, 1)])},
                    max_distance=80)
    return [(path, s)]
