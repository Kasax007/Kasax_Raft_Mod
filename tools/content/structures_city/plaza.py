"""Founders' Plaza: the start piece of Port Solace (47 x 47).

A square roundabout: outer sidewalks, a ring road (asphalt with lane lines, wrecked cars) and a raised central
island with the dry fountain, the bronze statue of the founder holding up a glowing crystal (a beacon you can see
through the murky water), benches, lamps, kelp planters and an information kiosk (terminal solace_1).
Exits: east/south/west avenues; north: City Hall.
"""
import math
import random

from .citylib import (ASPHALT, FOUND, FY, GY, NS, PAVE_BASE, PAVE_SLAB, City, bench, bus_stop, car, fallen_lamp,
                      lamp_post, manhole, newspaper_box, rubble_pile, scatter_growth, slab, stair)
from .common import pick

N = 47
C = 23
BRONZE = [('minecraft:oxidized_copper', 45), ('minecraft:weathered_copper', 25), ('minecraft:oxidized_cut_copper', 20),
          ('minecraft:exposed_copper', 10)]
ISLAND = [('minecraft:polished_andesite', 40), ('minecraft:smooth_stone', 25), ('minecraft:stone_bricks', 15),
          ('minecraft:cracked_stone_bricks', 10), ('raft:cracked_concrete', 10)]


def statue(s, cx, py, cz, rnd):
    """Founder figure facing south (towards +z), right arm raised with a glowing crystal. py = pedestal top."""
    B = lambda x, y, z: s.pset(x, y, z, BRONZE, rnd)  # noqa: E731
    # legs + boots
    for y in range(py, py + 4):
        for x in (cx - 1, cx + 1):
            for z in (cz, cz + 1):
                B(x, y, z)
    s.set(cx, py, cz + 1, 'minecraft:oxidized_cut_copper')
    # long coat
    for y in range(py + 3, py + 8):
        for x in range(cx - 1, cx + 2):
            for z in (cz, cz + 1):
                B(x, y, z)
    for x in range(cx - 1, cx + 2):
        s.set(x, py + 3, cz + 2, stair('minecraft:oxidized_cut_copper_stairs', 'north'))
        s.set(x, py + 2, cz - 1 + 0, 'minecraft:water') if False else None
    # shoulders, head, hat
    for x in range(cx - 2, cx + 3):
        B(x, py + 7, cz)
        B(x, py + 7, cz + 1)
    B(cx, py + 8, cz)
    B(cx, py + 8, cz + 1)
    B(cx, py + 9, cz)
    B(cx, py + 9, cz + 1)
    s.set(cx, py + 10, cz, slab('minecraft:oxidized_cut_copper_slab'))
    s.set(cx, py + 10, cz + 1, slab('minecraft:oxidized_cut_copper_slab'))
    # left arm down, holding a book
    for y in range(py + 4, py + 7):
        B(cx - 2, y, cz)
    s.set(cx - 2, py + 4, cz - 1, 'minecraft:oxidized_cut_copper_slab[type=top]')
    # right arm raised
    for y in range(py + 8, py + 12):
        B(cx + 2, y, cz)
    s.set(cx + 2, py + 12, cz, 'minecraft:oxidized_cut_copper')
    s.set(cx + 2, py + 13, cz, 'minecraft:sea_lantern')
    s.set(cx + 2, py + 14, cz, 'minecraft:light_blue_stained_glass')
    s.set(cx + 2, py + 15, cz, 'minecraft:end_rod[facing=up]')
    for f, dx, dz in (('n', 0, -1), ('s', 0, 1), ('e', 1, 0), ('w', -1, 0)):
        s.set(cx + 2 + dx, py + 13, cz + dz, 'minecraft:light_blue_stained_glass_pane')
    # cape behind
    for y in range(py + 4, py + 8):
        for x in range(cx - 1, cx + 2):
            B(x, y, cz - 1) if y > py + 4 else None
    s.set(cx - 1, py + 4, cz - 1, stair('minecraft:oxidized_cut_copper_stairs', 'south', 'top'))
    s.set(cx + 1, py + 4, cz - 1, stair('minecraft:oxidized_cut_copper_stairs', 'south', 'top'))


def build():
    rnd = random.Random(2001)
    s = City('port_solace/plaza', 2001)
    island = (10, 36)
    for x in range(N):
        for z in range(N):
            s.pfill(x, 0, z, x, 1, z, FOUND, rnd)
            outer = x < 3 or x > N - 4 or z < 3 or z > N - 4
            exit_ = (abs(z - C) <= 3 and (x < 3 or x > N - 4)) or (abs(x - C) <= 3 and z > N - 4)
            outer = outer and not exit_
            on_island = island[0] <= x <= island[1] and island[0] <= z <= island[1]
            if outer:
                s.pset(x, GY, z, PAVE_BASE, rnd)
                s.set(x, FY, z, pick(rnd, PAVE_SLAB))
                top = FY
            elif on_island:
                s.pset(x, GY, z, PAVE_BASE, rnd)
                d = max(abs(x - C), abs(z - C))
                if (x + z) % 4 == 0 and d > 9:
                    s.set(x, FY, z, 'minecraft:chiseled_stone_bricks' if rnd.random() < 0.7 else 'minecraft:mossy_stone_bricks')
                elif d in (12, 13):
                    s.set(x, FY, z, 'minecraft:stone_bricks' if rnd.random() < 0.8 else 'minecraft:cracked_stone_bricks')
                else:
                    s.pset(x, FY, z, ISLAND, rnd)
                top = FY
            else:
                # ring road; lane line in its middle
                ring_mid = (x in (6, N - 7) and 6 <= z <= N - 7) or (z in (6, N - 7) and 6 <= x <= N - 7)
                if ring_mid and (x + z) % 4 < 2:
                    axis = 'z' if x in (6, N - 7) else 'x'
                    s.set(x, GY, z, f'raft:asphalt_line[axis={axis}]')
                else:
                    s.pset(x, GY, z, ASPHALT, rnd)
                top = GY
            for y in range(top + 1, GY + 12):
                s.set(x, y, z, 'minecraft:water')
    # zebra crossings from the outer sidewalk to the island at the four exits
    for k in range(3, 10):
        for i in range(C - 3, C + 4):
            if i % 2 == 0:
                s.set(i, GY, k, 'raft:asphalt_line[axis=z]')
                s.set(i, GY, N - 1 - k, 'raft:asphalt_line[axis=z]')
                s.set(k, GY, i, 'raft:asphalt_line[axis=x]')
                s.set(N - 1 - k, GY, i, 'raft:asphalt_line[axis=x]')
    # island curb
    for x in range(island[0], island[1] + 1):
        for z in range(island[0], island[1] + 1):
            if x in island or z in island:
                s.set(x, FY, z, 'minecraft:polished_andesite' if (x + z) % 2 else 'minecraft:stone_bricks')
    # ---- fountain (dry: sand and seagrass in the bowl)
    for x in range(C - 9, C + 10):
        for z in range(C - 9, C + 10):
            d = math.hypot(x - C, z - C)
            if d <= 8.6:
                if d > 7.6:
                    s.set(x, FY + 1, z, 'minecraft:stone_bricks' if rnd.random() < 0.85 else 'minecraft:mossy_stone_bricks')
                    s.set(x, FY + 2, z, slab('minecraft:smooth_stone_slab'))
                else:
                    s.set(x, FY, z, 'minecraft:sand' if rnd.random() < 0.6 else 'minecraft:gravel')
                    if 3.6 < d <= 4.6:
                        s.set(x, FY + 1, z, 'minecraft:polished_andesite')
                        s.set(x, FY + 2, z, 'minecraft:polished_andesite')
                        s.set(x, FY + 3, z, slab('minecraft:polished_andesite_slab'))
                    elif d <= 3.6:
                        s.set(x, FY + 1, z, 'minecraft:sand')
                        s.set(x, FY + 2, z, 'minecraft:sand')
                        s.set(x, FY + 3, z, 'minecraft:sand')
    # broken fountain basin section
    for x in range(C + 5, C + 9):
        for z in range(C + 3, C + 7):
            if math.hypot(x - C, z - C) > 7.0 and rnd.random() < 0.7:
                s.set(x, FY + 2, z, 'minecraft:water')
                if rnd.random() < 0.5:
                    s.set(x, FY + 1, z, 'minecraft:water')
    rubble_pile(s, C + 9, FY + 1, C + 6, 1, rnd)
    # pedestal
    for x in range(C - 2, C + 3):
        for z in range(C - 2, C + 3):
            s.set(x, FY + 4, z, 'minecraft:chiseled_stone_bricks' if abs(x - C) == 2 and abs(z - C) == 2
                  else 'minecraft:polished_andesite')
    for x in range(C - 1, C + 2):
        for z in range(C - 1, C + 2):
            s.set(x, FY + 5, z, 'minecraft:polished_andesite')
            s.set(x, FY + 6, z, 'minecraft:chiseled_stone_bricks' if (x, z) != (C, C) else 'minecraft:polished_andesite')
    for f, dx, dz in (('north', 0, -2), ('south', 0, 2), ('east', 2, 0), ('west', -2, 0)):
        s.set(C + dx, FY + 5, C + dz, stair('minecraft:polished_andesite_stairs', 'north' if dz < 0 else 'south'
                                            if dz > 0 else 'east' if dx > 0 else 'west', 'bottom'))
    # plaque on the south face of the pedestal
    s.set(C, FY + 5, C + 2, 'minecraft:oak_wall_sign[facing=south]', {
        'id': 'minecraft:sign', 'is_waxed': True,
        'front_text': {'messages': ['', 'PORT SOLACE', 'founded by the', 'Meridian Expedition'],
                       'color': 'black', 'has_glowing_text': True},
        'back_text': {'messages': ['', '', '', ''], 'color': 'black', 'has_glowing_text': False}})
    statue(s, C - 1, FY + 7, C - 1, rnd)
    # ---- island furniture: lamps, benches, planters, flags
    for (x, z, arm) in ((C - 11, C - 11, 'east'), (C + 11, C - 11, 'west'), (C - 11, C + 11, 'east'),
                        (C + 11, C + 11, 'west'), (C, C - 11, 'south'), (C, C + 11, 'north'),
                        (C - 11, C, 'east'), (C + 11, C, 'west')):
        lamp_post(s, x, z, arm=arm, y=FY + 1, h=5, broken=rnd.random() < 0.15,
                  kind='sea' if rnd.random() < 0.5 else 'lantern')
    for (x, z, face, along) in ((C - 3, C - 10, 'south', 'x'), (C + 1, C + 10, 'north', 'x'),
                                (C - 10, C + 1, 'east', 'z'), (C + 10, C - 3, 'west', 'z')):
        bench(s, x, FY + 1, z, face, length=3, along=along)
    for (px, pz) in ((13, 13), (33, 13), (13, 33), (33, 33)):
        for x in range(px - 1, px + 2):
            for z in range(pz - 1, pz + 2):
                if (x, z) == (px, pz):
                    s.set(x, FY, z, 'minecraft:mud')
                    s.set(x, FY + 1, z, 'minecraft:mud')
                    s.kelp(x, FY + 2, z, rnd.randint(9, 17), glow=rnd.random() < 0.4)
                else:
                    s.set(x, FY + 1, z, 'minecraft:stone_brick_wall')
    # flagpoles before City Hall (north side of the island)
    for x in (C - 6, C + 6):
        for y in range(FY + 1, FY + 12):
            s.set(x, y, 12, 'minecraft:iron_bars')
        s.set(x, FY + 12, 12, 'minecraft:lightning_rod[facing=up]')
        for y in range(FY + 8, FY + 11):
            s.set(x + 1, y, 12, 'minecraft:red_stained_glass_pane' if y > FY + 9 or x < C else 'minecraft:water')
    # information kiosk with the grid terminal
    kx, kz = 31, 16
    for x in range(kx, kx + 3):
        for z in range(kz, kz + 3):
            for y in range(FY + 1, FY + 4):
                edge = x in (kx, kx + 2) and z in (kz, kz + 2)
                if edge:
                    s.set(x, y, z, 'minecraft:dark_oak_fence')
            s.set(x, FY + 4, z, slab('minecraft:dark_oak_slab', 'bottom'))
    s.set(kx + 1, FY + 1, kz + 1, 'minecraft:polished_andesite')
    s.block_entity(kx + 1, FY + 2, kz + 1, 'raft:research_terminal[facing=south]', 'raft:research_terminal',
                   {'LogId': 'solace_1'})
    s.set(kx + 1, FY + 4, kz + 1, 'raft:neon_tube_cyan[axis=x]')
    newspaper_box(s, kx - 1, FY + 1, kz + 2, 'yellow')
    s.chest(kx + 3, FY + 1, kz + 1, 'raft:chests/port_solace_shop', facing='east', block='minecraft:barrel')
    # outer sidewalk: bus stop and lamps
    bus_stop(s, 30, FY + 1, N - 1, along='x', facing='north', length=5)
    for (x, z, arm) in ((1, 8, 'east'), (N - 2, 38, 'west'), (38, 1, 'south'), (8, N - 2, 'north'),
                        (1, 38, 'east'), (N - 2, 8, 'west')):
        lamp_post(s, x, z, arm=arm, y=FY, h=5, broken=rnd.random() < 0.25)
    # traffic on the ring road
    car(s, 4, GY + 1, 14, along='z', rnd=rnd, wreck=0.4)
    car(s, 26, GY + 1, 39, along='x', rnd=rnd, wreck=0.3)
    car(s, 39, GY + 1, 26, along='z', rnd=rnd, wreck=0.5, flipped=True)
    car(s, 14, GY + 1, 4, along='x', rnd=rnd, wreck=0.2)
    fallen_lamp(s, 38, GY + 1, 5, along='z', length=4)
    for (x, z) in ((6, 30), (30, 6), (40, 40), (6, 6)):
        manhole(s, x, GY, z)
    scatter_growth(s, ((0, GY + 1, 0), (N - 1, FY + 4, N - 1)), rnd, kelp=0.004, grass=0.08, pickles=0.015,
                   kelp_h=(6, 18))
    s.grow_barnacles(((0, GY + 1, 0), (N - 1, FY + 14, N - 1)), 0.18, 77,
                     support=lambda n: 'copper' in n or 'stone_brick' in n or 'concrete' in n)
    # jigsaws
    s.jig(C, GY, 0, 'north', 'hall_anchor', 'hall_front', 'city_hall', 'minecraft:stone', sel=3, prio=2)
    s.jig(N - 1, GY, C, 'east', 'street', 'street', 'avenues_power', 'raft:asphalt', sel=1)
    s.jig(C, GY, N - 1, 'south', 'street', 'street', 'avenues_harbour', 'raft:asphalt', sel=1)
    s.jig(0, GY, C, 'west', 'street', 'street', 'avenues_market', 'raft:asphalt', sel=1)
    return s.done()
