"""rusted_rig - the abandoned Meridian drilling and research platform ("the Rusted Giant").

Four riveted steel legs stand from world y 20 (the template's y=0, fixed start height, no projection) through the
sea to a cellar deck (y 104) and the main deck (y 110). On the main deck: a lattice drilling derrick (the landmark
visible from afar), the accommodation block with mess, control room (terminal rig_1), bunks and office (terminal
rig_2), a helipad on its roof, a radio hut with a lattice mast (expedition_radio, dishes), two pedestal cranes
(one collapsed into the sea), container laydown, wind turbines, a flare boom and a lifeboat on davits.
Below: X-braced legs with barnacles, a boat landing with ladders at the waterline.
"""
import math
import random

from .lib import Side, contact, pick, save, spawns, wear, write_structure

W0 = 20               # world y of the template's y=0
SEA = 99 - W0         # top water layer (t 79)
LAND = 101 - W0       # boat landing
CELLAR = 104 - W0     # cellar deck
DECK = 110 - W0       # main deck floor (t 90)
LEGS = [(-12, -12), (10, -12), (-12, 10), (10, 10)]     # 3x3 legs (min corner)
PLATE = [('raft:rusted_plate', 6), ('raft:riveted_hull', 2), ('raft:rusty_hull', 1)]
WHITE = [('minecraft:white_concrete', 6), ('raft:corrugated_metal', 3), ('raft:rusty_hull', 1)]


def beam_line(s, a, b):
    (x1, y1, z1), (x2, y2, z2) = a, b
    dx, dy, dz = abs(x2 - x1), abs(y2 - y1), abs(z2 - z1)
    axis = 'y' if dy >= max(dx, dz) else ('x' if dx >= dz else 'z')
    s.line(a, b, f'raft:steel_beam[axis={axis}]')


def legs(s, rnd):
    for (lx, lz) in LEGS:
        for y in range(0, DECK):
            for dx in (0, 1, 2):
                for dz in (0, 1, 2):
                    if dx == 1 and dz == 1 and 2 < y < DECK - 1:
                        s.set(lx + dx, y, lz + dz, 'minecraft:water' if y <= SEA else 'minecraft:air')
                        continue
                    if y < SEA - 2:
                        st = pick(rnd, [('raft:rusty_hull', 6), ('raft:riveted_hull', 3)])
                    elif y <= SEA + 2:
                        st = 'raft:rusty_hull'
                    else:
                        st = pick(rnd, [('raft:riveted_hull', 6), ('raft:rusty_hull', 2)])
                    s.set(lx + dx, y, lz + dz, st)
            if y % 12 == 6 and y < SEA + 3:                  # stiffener rings
                for dx in (-1, 3):
                    for dz in (0, 1, 2):
                        s.set(lx + dx, y, lz + dz, 'raft:rusted_plate')
                        s.set(lx + dz, y, lz + dx, 'raft:rusted_plate')
        for dx in (-1, 3):                                   # foot pads
            for dz in (-1, 3):
                for y in (0, 1):
                    s.set(lx + dx, y, lz + dz, 'raft:rusty_hull')
    # horizontal braces and X-braces between the legs on all four faces
    levels = [24, 46, 68, CELLAR - 1]
    faces = [((-10, -11), (10, -11)), ((-10, 11), (10, 11)), ((-11, -10), (-11, 10)), ((11, -10), (11, 10))]
    for y in levels:
        for (a, b) in faces:
            beam_line(s, (a[0], y, a[1]), (b[0], y, b[1]))
    for y0, y1 in zip(levels, levels[1:]):
        for (a, b) in faces:
            beam_line(s, (a[0], y0, a[1]), (b[0], y1, b[1]))
            beam_line(s, (b[0], y0, b[1]), (a[0], y1, a[1]))


def catwalk_deck(s, rnd, y, x0, x1, z0, z1, railing=True, solid=0.0):
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if s.get(x, y, z) is None:
                s.set(x, y, z, pick(rnd, PLATE) if rnd.random() < solid else 'raft:catwalk[type=top]')
            if railing and (x in (x0, x1) or z in (z0, z1)) and s.get(x, y + 1, z) is None:
                s.set(x, y + 1, z, 'raft:steel_railing')


def ladder(s, x, z, y0, y1, facing):
    for y in range(y0, y1 + 1):
        s.set(x, y, z, f'minecraft:ladder[facing={facing}]')


def main_deck(s, rnd):
    for x in range(-14, 15):
        for z in range(-14, 15):
            st = pick(rnd, PLATE) if rnd.random() < 0.82 else 'raft:catwalk[type=bottom]'
            s.set(x, DECK, z, st)
            if x in (-14, 14) or z in (-14, 14):
                s.set(x, DECK, z, 'minecraft:yellow_concrete' if (x + z) % 4 == 0 else 'raft:riveted_hull')
                s.set(x, DECK + 1, z, 'raft:steel_railing' if rnd.random() < 0.85 else None)
    # deck girders underneath
    for x in range(-14, 15, 4):
        for z in range(-14, 15):
            s.set(x, DECK - 1, z, 'raft:steel_beam[axis=z]')
    # collapsed corner (south-west): plates hang down to the cellar deck
    for x in range(-14, -8):
        for z in range(9, 15):
            if (x + 14) + (14 - z) < 7:
                s.set(x, DECK, z, None)
                s.set(x, DECK + 1, z, None)
                s.set(x, DECK - 1, z, None)
    for i in range(5):
        s.set(-13 + i, DECK - 1 - i // 2, 13 - i, 'raft:rusted_plate')


def derrick(s, rnd, cx=-4, cz=0):
    base, topw, h = 3, 1, 30
    y0 = DECK + 1
    for x in range(cx - base - 1, cx + base + 2):          # drill floor
        for z in range(cz - base - 1, cz + base + 2):
            s.set(x, y0, z, 'raft:catwalk[type=bottom]' if (x + z) % 3 else 'raft:rusted_plate')
    corners = [(-1, -1), (1, -1), (-1, 1), (1, 1)]
    for sx, sz in corners:
        beam_line(s, (cx + sx * base, y0, cz + sz * base), (cx + sx * topw, y0 + h, cz + sz * topw))
    for k in range(6, h, 6):
        w = round(base - (base - topw) * k / h)
        for (ax, az), (bx, bz) in (((-1, -1), (1, -1)), ((-1, 1), (1, 1)), ((-1, -1), (-1, 1)), ((1, -1), (1, 1))):
            beam_line(s, (cx + ax * w, y0 + k, cz + az * w), (cx + bx * w, y0 + k, cz + bz * w))
    for k0 in range(0, h - 6, 6):
        w0 = round(base - (base - topw) * k0 / h)
        w1 = round(base - (base - topw) * (k0 + 6) / h)
        for side in (-1, 1):
            beam_line(s, (cx - w0, y0 + k0, cz + side * w0), (cx + w1, y0 + k0 + 6, cz + side * w1))
            beam_line(s, (cx + side * w0, y0 + k0, cz - w0), (cx + side * w1, y0 + k0 + 6, cz + w1))
    for x in range(cx - 2, cx + 3):                         # crown platform + light
        for z in range(cz - 2, cz + 3):
            s.set(x, y0 + h + 1, z, 'raft:catwalk[type=bottom]')
            if abs(x - cx) == 2 or abs(z - cz) == 2:
                s.set(x, y0 + h + 2, z, 'raft:steel_railing')
    s.set(cx, y0 + h + 2, cz, 'raft:steel_beam[axis=y]')
    s.set(cx, y0 + h + 3, cz, 'raft:ship_lantern[hanging=false]')
    # travelling block on a chain, drill pipe, pipe rack
    for y in range(y0 + 12, y0 + h + 1):
        s.set(cx, y, cz, 'minecraft:iron_chain[axis=y]')
    s.set(cx, y0 + 11, cz, 'minecraft:iron_block')
    for y in range(y0 + 1, y0 + 11):
        s.set(cx, y, cz, 'raft:rusty_pipe[axis=y]')
    for y in range(DECK - 1, CELLAR, -1):
        s.set(cx, y, cz, 'raft:rusty_pipe[axis=y]')
    for z in range(cz - 2, cz + 3):
        s.set(cx + 5, y0 + 1, z, 'raft:rusty_pipe[axis=z]')
        s.set(cx + 6, y0 + 1, z, 'raft:rusty_pipe[axis=z]')
    s.set(cx - base, y0 + 1, cz + base - 1, 'raft:control_panel[facing=east]')


def quarters(s, rnd):
    x0, x1, z0, z1 = 4, 13, -13, -4
    floors = [DECK, DECK + 4]
    roof = DECK + 8
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            for y in range(DECK + 1, roof + 1):
                wall = x in (x0, x1) or z in (z0, z1)
                if y in floors or y == roof:
                    s.set(x, y, z, pick(rnd, WHITE) if wall else pick(rnd, PLATE))
                elif wall:
                    corner = x in (x0, x1) and z in (z0, z1)
                    win = (y - DECK) % 4 == 2 and not corner and (x + z) % 3 == 0
                    s.set(x, y, z, 'raft:steel_beam[axis=y]' if corner else ('raft:porthole' if win else pick(rnd, WHITE)))
                else:
                    s.set(x, y, z, 'minecraft:air')
    # inner wall splitting each floor into two rooms
    for z in range(z0 + 1, z1):
        for y in list(range(DECK + 1, DECK + 4)) + list(range(DECK + 5, DECK + 8)):
            s.set(8, y, z, pick(rnd, WHITE) if z != -8 else 'minecraft:air')
    s.set(8, DECK + 1, -8, 'raft:bulkhead_door[facing=east,half=lower,hinge=left,open=true,powered=false]')
    s.set(8, DECK + 2, -8, 'raft:bulkhead_door[facing=east,half=upper,hinge=left,open=true,powered=false]')
    # entrance + stair ladder
    s.set(x0, DECK + 1, -6, 'raft:bulkhead_door[facing=west,half=lower,hinge=left,open=false,powered=false]')
    s.set(x0, DECK + 2, -6, 'raft:bulkhead_door[facing=west,half=upper,hinge=left,open=false,powered=false]')
    ladder(s, 12, -5, DECK + 1, DECK + 7, 'north')
    s.set(12, DECK + 4, -5, 'minecraft:air')
    s.set(12, DECK + 4, -5, 'minecraft:ladder[facing=north]')
    ladder(s, 12, -5, DECK + 8, DECK + 8, 'north')
    s.set(12, roof, -5, 'minecraft:ladder[facing=north]')
    # ground floor west: mess
    s.set(5, DECK + 1, -12, 'minecraft:smoker[facing=east,lit=false]')
    s.set(5, DECK + 1, -11, 'minecraft:furnace[facing=east,lit=false]')
    s.set(5, DECK + 1, -10, 'minecraft:barrel[facing=east,open=false]')
    for z in (-11, -10, -9):
        s.set(7, DECK + 1, z, 'raft:driftwood_table')
    s.set(6, DECK + 1, -10, 'raft:driftwood_chair[facing=east]')
    s.set(6, DECK + 3, -9, 'minecraft:lantern[hanging=true]')
    # ground floor east: control room with terminal rig_1
    s.set(10, DECK + 1, -12, 'raft:control_panel[facing=south]')
    s.set(11, DECK + 1, -12, 'raft:control_panel[facing=south]')
    s.block_entity(12, DECK + 1, -12, 'raft:research_terminal[facing=south]', 'raft:research_terminal', {'LogId': 'rig_1'})
    s.set(9, DECK + 1, -12, 'raft:server_rack[facing=south]')
    s.set(12, DECK + 1, -10, 'raft:server_rack[facing=west]')
    s.set(10, DECK + 3, -9, 'minecraft:lantern[hanging=true]')
    # upper floor west: bunks; east: office with terminal rig_2 and the chest
    for z in (-12, -10):
        s.set(5, DECK + 5, z, 'minecraft:light_gray_bed[facing=east,occupied=false,part=foot]')
        s.set(6, DECK + 5, z, 'minecraft:light_gray_bed[facing=east,occupied=false,part=head]')
    s.set(7, DECK + 5, -5, 'minecraft:barrel[facing=up,open=false]')
    s.chest(5, DECK + 5, -5, 'raft:chests/rusted_rig', facing='east')
    s.block_entity(10, DECK + 5, -12, 'raft:research_terminal[facing=south]', 'raft:research_terminal', {'LogId': 'rig_2'})
    s.set(11, DECK + 5, -12, 'raft:chart_table[facing=south]')
    s.set(10, DECK + 5, -11, 'raft:driftwood_chair[facing=north]')
    s.set(9, DECK + 5, -5, 'raft:driftwood_shelf[facing=south]')
    s.set(10, DECK + 7, -8, 'minecraft:lantern[hanging=true]')
    s.set(6, DECK + 7, -8, 'minecraft:lantern[hanging=true]')
    # helipad on the roof, cantilevered over the deck edge (east and north)
    hx0, hx1, hz0, hz1 = 4, 18, -18, -4
    hy = roof + 1
    for x in range(hx0, hx1 + 1):
        for z in range(hz0, hz1 + 1):
            d = max(abs(x - (hx0 + hx1) / 2), abs(z - (hz0 + hz1) / 2))
            if abs(x - (hx0 + hx1) / 2) + abs(z - (hz0 + hz1) / 2) > 12.5:
                continue
            edge = d >= 6.5 or abs(x - (hx0 + hx1) / 2) + abs(z - (hz0 + hz1) / 2) > 11.5
            s.set(x, hy, z, 'minecraft:yellow_concrete' if edge else pick(
                rnd, [('minecraft:gray_concrete', 6), ('raft:rusted_plate', 1)]))
            if edge and (x + z) % 5 == 0:
                s.set(x, hy + 1, z, 'raft:ship_lantern[hanging=false]')
    cx, cz = (hx0 + hx1) // 2, (hz0 + hz1) // 2
    for dz in range(-3, 4):                                   # the H
        s.set(cx - 2, hy, cz + dz, 'minecraft:white_concrete')
        s.set(cx + 2, hy, cz + dz, 'minecraft:white_concrete')
    for dx in range(-1, 2):
        s.set(cx + dx, hy, cz, 'minecraft:white_concrete')
    for (x, z) in ((18, -11), (11, -18), (16, -15)):            # struts under the cantilever
        beam_line(s, (x, hy - 1, z), (min(x, 13), DECK + 1, max(z, -13)))
    s.set(12, hy, -5, 'minecraft:air')
    s.set(12, hy - 1, -5, 'minecraft:ladder[facing=north]')


def radio(s, rnd):
    x0, x1, z0, z1 = -13, -9, 4, 8
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            for y in range(DECK + 1, DECK + 5):
                wall = x in (x0, x1) or z in (z0, z1)
                if y == DECK + 4:
                    s.set(x, y, z, 'raft:corrugated_metal_slab[type=bottom]')
                elif wall:
                    s.set(x, y, z, 'raft:porthole' if y == DECK + 2 and (x + z) % 2 == 0 and
                          not (x in (x0, x1) and z in (z0, z1)) else pick(rnd, WHITE))
                else:
                    s.set(x, y, z, 'minecraft:air')
    s.set(x1, DECK + 1, 6, 'raft:bulkhead_door[facing=east,half=lower,hinge=left,open=true,powered=false]')
    s.set(x1, DECK + 2, 6, 'raft:bulkhead_door[facing=east,half=upper,hinge=left,open=true,powered=false]')
    s.set(-12, DECK + 1, 7, 'raft:driftwood_table')
    s.set(-12, DECK + 2, 7, 'raft:expedition_radio[facing=east]')
    s.set(-11, DECK + 1, 7, 'raft:driftwood_chair[facing=west]')
    s.set(-12, DECK + 1, 5, 'raft:server_rack[facing=east]')
    s.chest(-10, DECK + 1, 7, 'raft:chests/rusted_rig', facing='west', block='raft:wooden_crate')
    s.set(-11, DECK + 3, 6, 'minecraft:lantern[hanging=true]')
    # lattice mast
    mx, mz = -11, 6
    for y in range(DECK + 5, DECK + 28):
        s.set(mx, y, mz, 'raft:steel_beam[axis=y]')
        if y % 4 == 0:
            for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                s.set(mx + dx, y, mz + dz, 'raft:steel_railing')
    s.set(mx + 1, DECK + 16, mz, 'raft:satellite_dish[facing=east]')
    s.set(mx, DECK + 22, mz - 1, 'raft:satellite_dish[facing=north]')
    s.set(mx, DECK + 28, mz, 'minecraft:lightning_rod[facing=up,powered=false]')
    s.set(mx - 1, DECK + 26, mz, 'raft:ship_lantern[hanging=true]')
    for (dx, dz) in ((-3, -2), (2, 3), (-2, 3)):
        s.line((mx, DECK + 18, mz), (mx + dx, DECK + 5, mz + dz), 'minecraft:iron_chain[axis=y]')


def crane(s, rnd, px, pz, tip, broken=False):
    """Pedestal crane: column, cab with control panel, lattice boom to `tip`."""
    for y in range(DECK + 1, DECK + 7):
        for dx in (0, 1):
            for dz in (0, 1):
                s.set(px + dx, y, pz + dz, pick(rnd, [('raft:riveted_hull', 3), ('minecraft:yellow_concrete', 2),
                                                      ('raft:rusty_hull', 1)]))
    cy = DECK + 7
    for dx in (-1, 0, 1, 2):
        for dz in (-1, 0, 1, 2):
            s.set(px + dx, cy, pz + dz, 'raft:rusted_plate')
            for y in (cy + 1, cy + 2):
                wall = dx in (-1, 2) or dz in (-1, 2)
                s.set(px + dx, y, pz + dz, ('minecraft:yellow_concrete' if y == cy + 1 else
                                            pick(rnd, [('raft:cracked_glass_pane', 1), ('minecraft:glass_pane', 1)]))
                      if wall else 'minecraft:air')
            s.set(px + dx, cy + 3, pz + dz, 'minecraft:yellow_concrete')
    s.set(px, cy + 1, pz, 'raft:control_panel[facing=north]')
    a = (px + 1, cy + 3, pz + 1)
    if broken:
        mid = ((a[0] + tip[0]) // 2, a[1] + 4, (a[2] + tip[2]) // 2)
        beam_line(s, a, mid)
        beam_line(s, (a[0], a[1] + 1, a[2]), (mid[0], mid[1] + 1, mid[2]))
        beam_line(s, mid, tip)
        beam_line(s, (mid[0], mid[1] + 1, mid[2]), (tip[0], tip[1] + 1, tip[2]))
    else:
        beam_line(s, a, tip)
        beam_line(s, (a[0], a[1] + 1, a[2]), (tip[0], tip[1] + 1, tip[2]))
        for y in range(tip[1] - 8, tip[1]):
            s.set(tip[0], y, tip[2], 'minecraft:iron_chain[axis=y]')
        # a heavy core as the crane's weight: an anvil would fall at the first block update next to it
        s.set(tip[0], tip[1] - 9, tip[2], 'minecraft:heavy_core[waterlogged=false]')


def extras(s, rnd):
    # containers laydown (south-east), some stacked, one open with crates
    cols = ['raft:shipping_container_red', 'raft:shipping_container_blue', 'raft:shipping_container_green']
    for i, (x, z, h) in enumerate(((3, 5, 2), (6, 5, 1), (9, 5, 2), (3, 11, 1))):
        for k in range(h):
            c = cols[(i + k) % 3]
            for dz in range(5 if i < 3 else 1):
                for dx in range(2):
                    for dy in range(2):
                        if i == 3:
                            for xx in range(5):
                                s.set(x + xx, DECK + 1 + dy, z + dx, f'{c}[axis=x]')
                        else:
                            s.set(x + dx, DECK + 1 + 2 * k + dy, z + dz, f'{c}[axis=z]')
    s.chest(10, DECK + 1, 12, 'raft:chests/rusted_rig', facing='north', block='raft:wooden_crate')
    s.set(11, DECK + 1, 12, 'minecraft:barrel[facing=up,open=false]')
    # wind turbines on posts at the west edge (rotor faces west, free 3x3 in front)
    for z in (-6, 1):
        for y in range(DECK + 1, DECK + 5):
            s.set(-14, y, z, 'raft:steel_beam[axis=y]')
        s.set(-14, DECK + 5, z, 'raft:wind_turbine[facing=west]')
    # flare boom to the east
    beam_line(s, (14, DECK + 1, 2), (22, DECK + 7, 2))
    beam_line(s, (14, DECK + 1, 4), (22, DECK + 7, 3))
    s.set(23, DECK + 8, 2, 'raft:brazier[lit=false]')
    s.set(23, DECK + 7, 2, 'minecraft:iron_bars')
    # lifeboat on davits over the south edge
    for x in (-8, -2):
        beam_line(s, (x, DECK + 1, 14), (x, DECK + 5, 16))
        s.set(x, DECK + 4, 16, 'minecraft:iron_chain[axis=y]')
    for x in range(-8, -1):
        for dz in (0, 1, 2):
            s.set(x, DECK + 2, 15 + dz, 'minecraft:orange_concrete')
            s.set(x, DECK + 3, 15 + dz, 'minecraft:white_concrete' if dz == 1 or x in (-8, -2) else 'minecraft:orange_concrete')
    s.set(-5, DECK + 3, 16, 'minecraft:white_concrete')
    # life rings, lights, barrels on the deck
    for (x, z) in ((-13, -13), (13, 13), (13, -1), (-13, -1)):
        s.set(x, DECK + 1, z, 'raft:ship_lantern[hanging=false]')
    for (x, z) in ((0, -12), (1, -12), (0, -11), (-9, 12)):
        s.set(x, DECK + 1, z, 'minecraft:barrel[facing=up,open=false]')
    s.set(1, DECK + 2, -12, 'minecraft:barrel[facing=up,open=false]')


def lower(s, rnd):
    # cellar deck: catwalk ring between the legs, pumps and pipes
    catwalk_deck(s, rnd, CELLAR, -10, 10, -10, 10, railing=True, solid=0.3)
    for x in range(-6, 7):
        for z in range(-6, 7):
            if s.get(x, CELLAR, z) == 'raft:catwalk[type=top]' and abs(x) < 6 and abs(z) < 6:
                s.set(x, CELLAR, z, None)
    for (x, z) in ((-8, -8), (8, 8), (-8, 8)):
        s.set(x, CELLAR + 1, z, 'minecraft:blast_furnace[facing=north,lit=false]')
        for y in range(CELLAR + 2, DECK - 1):
            s.set(x, y, z, 'raft:rusty_pipe[axis=y]' if y != CELLAR + 3 else 'raft:rusty_pipe_valve[axis=y]')
    for x in range(-9, 10):
        s.set(x, CELLAR + 1, 9, 'raft:rusty_pipe[axis=x]')
    s.chest(8, CELLAR + 1, -8, 'raft:chests/rusted_rig', facing='west', block='minecraft:barrel')
    # boat landing at the waterline with ladders up the north-west leg
    for x in range(-18, -12):
        for z in range(-13, -8):
            s.set(x, LAND, z, 'raft:catwalk[type=top]')
    for z in range(-13, -8):
        s.set(-18, LAND + 1, z, 'raft:steel_railing')
    s.set(-17, LAND + 1, -13, 'minecraft:spruce_fence')
    s.set(-17, LAND + 1, -9, 'minecraft:spruce_fence')
    ladder(s, -13, -11, LAND + 1, DECK + 1, 'west')
    s.set(-13, DECK + 1, -11, 'minecraft:air')
    s.set(-14, DECK, -11, 'raft:catwalk[type=bottom]')
    s.set(-13, DECK, -11, 'minecraft:ladder[facing=west]')
    # barnacles and rust stains on the legs below the surface
    s.grow_barnacles(((-14, 0, -14), (14, SEA, 14)), 0.12, 99)


def build():
    rnd = random.Random(31415905)
    s = Side('rusted_rig')
    legs(s, rnd)
    main_deck(s, rnd)
    lower(s, rnd)
    derrick(s, rnd)
    quarters(s, rnd)
    radio(s, rnd)
    crane(s, rnd, -13, -13, (-24, DECK + 16, -22))
    crane(s, rnd, 12, 12, (21, SEA - 4, 22), broken=True)
    extras(s, rnd)
    return s


def main(preview=False):
    s = build()
    path = 'rusted_rig/rig_0'
    (_, y0, _), _ = s.bounds()
    assert y0 == 0, y0
    save(s, path)
    if preview:
        print('  preview', contact(s, 'rusted_rig', cuts=(DECK + 3, DECK + 7), scale=3))
    write_structure('rusted_rig', [(path, 1)],
                    [wear([('raft:riveted_hull', 'raft:rusty_hull', 0.12), ('raft:rusted_plate', 'raft:rusty_hull', 0.04)])],
                    step='surface_structures', terrain='none', start_height=W0, heightmap=None,
                    biomes=['raft_ocean_biome', 'frost_floes'], spacing=84, separation=52, salt=31415905,
                    max_distance=64,
                    spawn_overrides={'monster': spawns([('minecraft:drowned', 2, 1, 2), ('raft:drowned_corsair', 1, 1, 1)])})
    return [(path, s)]
