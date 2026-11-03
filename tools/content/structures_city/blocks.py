"""Mid-rise buildings and small lots of Port Solace: hotel, department store, apartment blocks (2 variants), shop row,
corner diner, ruined lot and pocket park. All face north (street jigsaw at z 0)."""
import math
import random

from .citylib import (CONCRETE, CONCRETE_DARK, FLOOR, FOUND, FY, GY, PAVE_SLAB, RUBBLE, City, bench, bite, car,
                      lamp_post, rubble_pile, scatter_growth, slab, stair, tree_pit)
from .common import DIRS, pick
from . import towerkit as tk
from .towers import front_jig, lot_front

BRICK = [('minecraft:bricks', 62), ('minecraft:mud_bricks', 10), ('minecraft:terracotta', 10),
         ('minecraft:granite', 6), ('raft:cracked_concrete', 6), ('minecraft:mossy_cobblestone', 6)]
STONE_TRIM = [('minecraft:smooth_stone', 50), ('minecraft:polished_andesite', 30), ('raft:cracked_concrete', 20)]
PLASTER = [('minecraft:white_terracotta', 50), ('minecraft:calcite', 20), ('minecraft:smooth_sandstone', 20),
           ('raft:cracked_concrete', 10)]
TEAL = [('minecraft:cyan_terracotta', 60), ('minecraft:light_gray_terracotta', 25), ('raft:cracked_concrete', 15)]
WIN = [('minecraft:light_gray_stained_glass_pane', 55), ('raft:cracked_glass_pane', 25), ('minecraft:water', 20)]
AWN = {'red': ('minecraft:mangrove_slab', 'minecraft:birch_slab'), 'green': ('minecraft:warped_slab', 'minecraft:birch_slab'),
       'blue': ('minecraft:prismarine_brick_slab', 'minecraft:birch_slab'),
       'yellow': ('minecraft:bamboo_slab', 'minecraft:birch_slab')}


def awning(s, x0, x1, y, z, colour):
    a, b = AWN[colour]
    for x in range(x0, x1 + 1):
        s.set(x, y, z, slab(a if x % 2 else b, 'top'))


def balcony(s, x0, x1, y, z, rail='minecraft:iron_bars'):
    """Balcony slab at y (floor level) in front of the facade at z (outside), railing above."""
    for x in range(x0, x1 + 1):
        s.set(x, y, z, slab('minecraft:smooth_stone_slab', 'top'))
        s.set(x, y + 1, z, rail)


def water_tank(s, cx, y, cz):
    for (dx, dz) in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
        s.set(cx + dx, y, cz + dz, 'minecraft:spruce_fence')
    for yy in range(y + 1, y + 4):
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                s.set(cx + dx, yy, cz + dz, 'minecraft:spruce_planks' if (dx or dz) else 'minecraft:water')
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            s.set(cx + dx, y + 4, cz + dz, slab('minecraft:spruce_slab'))


def mannequin(s, x, y, z, yaw=180.0):
    s.entity(x + 0.5, y, z + 0.5, {'id': 'minecraft:armor_stand', 'Rotation': [yaw, 0.0], 'ShowArms': True})


# ------------------------------------------------------------------------------------------------ hotel
def hotel():
    rnd = random.Random(5101)
    s = City('port_solace/hotel', 5101)
    W, D = 25, 16
    s.pfill(0, 0, 0, W - 1, GY, D - 1, FOUND, rnd)
    lot_front(s, W, rnd)
    st = tk.Style(corner=[('minecraft:light_blue_terracotta', 80), ('raft:cracked_concrete', 20)],
                  pier=PLASTER, band=[('minecraft:light_blue_terracotta', 65), ('minecraft:cyan_terracotta', 20),
                                      ('raft:cracked_concrete', 15)],
                  wall=PLASTER, glass=WIN, bay=3, floor=FLOOR, sill='minecraft:smooth_sandstone_stairs',
                  cornice='minecraft:smooth_sandstone_stairs')
    x0, z0, x1, z1 = 0, 1, W - 1, D - 1

    def cell(side, i, n, k, r):
        if side == 'north' and k == 0 and 10 <= i <= 14:
            return 'minecraft:water'
        if side == 'north' and k >= 1 and i % 6 in (1, 2) and r <= 2:
            return 'minecraft:water'           # balcony doors
        return None

    floors = 9
    top = tk.shell(s, x0, z0, x1, z1, FY, floors, 4, st, rnd, cell=cell)
    tk.slabs(s, x0, z0, x1, z1, FY, floors, 4, st, rnd)
    for k in range(1, floors):
        y = FY + 4 * k
        for i in range(0, W - 1, 6):
            balcony(s, i + 1, i + 2, y, 0, rail='raft:steel_railing')
    # entrance canopy
    for x in range(9, 16):
        s.set(x, FY + 4, 0, slab('minecraft:mangrove_slab' if x % 2 else 'minecraft:birch_slab', 'top'))
    for x in (9, 15):
        for y in range(FY + 1, FY + 4):
            s.set(x, y, 0, 'minecraft:iron_bars')
    # blade sign on the north-east corner
    for y in range(FY + 8, top - 1):
        s.set(W, y, 2, 'raft:neon_tube_pink[axis=y]' if y % 4 == 0 else 'minecraft:pink_terracotta')
        s.set(W + 1, y, 2, 'minecraft:white_terracotta' if y % 2 else 'raft:neon_tube_pink[axis=y]')
    for y in (FY + 7, top - 1):
        s.set(W, y, 2, 'minecraft:iron_block')
        s.set(W + 1, y, 2, 'minecraft:iron_block')
    # lobby
    for x in range(3, 10):
        s.set(x, FY + 1, 11, 'minecraft:dark_oak_planks')
        s.set(x, FY + 2, 11, slab('minecraft:smooth_quartz_slab'))
    s.set(6, FY + 1, 12, stair('minecraft:dark_oak_stairs', 'south'))
    s.set(4, FY + 3, 11, 'raft:ship_bell[facing=north]') if False else None
    for x in range(14, 21, 3):
        for z in (5, 6):
            s.set(x, FY + 1, z, stair('minecraft:resin_brick_stairs', 'north' if z == 5 else 'south'))
    s.chest(22, FY + 1, 13, 'raft:chests/port_solace_apartment', facing='west', block='minecraft:barrel')
    # core: stairs at the east end, rooms along a corridor
    tk.spiral_stairs(s, 19, 10, 22, 13, FY + 1, top - 1, 'minecraft:smooth_sandstone_stairs', newel=PLASTER, rnd=rnd,
                     walls=PLASTER, doors='west', fh=4, y0=FY)
    for k in range(1, floors):
        y = FY + 4 * k + 1
        tk.partition(s, 1, 6, 17, 6, y, 3, PLASTER, rnd, doors={(x, 6) for x in range(3, 18, 4)})
        tk.partition(s, 1, 9, 17, 9, y, 3, PLASTER, rnd, doors={(x, 9) for x in range(3, 18, 4)})
        for xr in range(4, 18, 4):
            tk.partition(s, xr, 2, xr, 5, y, 3, PLASTER, rnd)
            tk.partition(s, xr, 10, xr, 14, y, 3, PLASTER, rnd)
        for xr in range(1, 17, 4):
            col = rnd.choice(['red', 'blue', 'yellow', 'white', 'light_blue'])
            s.set(xr + 1, y, 3, 'minecraft:white_wool')
            s.set(xr + 1, y, 4, f'minecraft:{col}_wool')
            s.set(xr + 2, y, 2, 'minecraft:barrel[facing=up,open=false]') if rnd.random() < 0.5 else None
            if rnd.random() < 0.6:
                s.set(xr + 1, y, 13, 'minecraft:white_wool')
                s.set(xr + 1, y, 12, f'minecraft:{col}_wool')
            if rnd.random() < 0.18:
                s.chest(xr + 2, y, 13, 'raft:chests/port_solace_apartment', facing='north')
        for x in range(2, 18, 5):
            s.set(x, y + 2, 7, 'minecraft:lantern[hanging=true]') if rnd.random() < 0.5 else None
    # rooftop pool (dry now) and deck chairs
    for x in range(3, 12):
        for z in range(4, 11):
            edge = x in (3, 11) or z in (4, 10)
            s.set(x, top + 1, z, 'minecraft:light_blue_terracotta' if edge else 'minecraft:water')
            if not edge:
                s.set(x, top, z, 'minecraft:prismarine_bricks')
    tk.parapet(s, x0, z0, x1, z1, top + 1, 'minecraft:smooth_sandstone_slab[type=bottom]', rnd, gaps=0.1)
    tk.break_windows(s, ((-1, 0, 0), (W + 2, top + 2, D)), rnd, missing=0.3, cracked=0.25)
    tk.floor_holes(s, x0 + 1, z0 + 1, x1 - 1, z1 - 1, [FY + 4 * 5], rnd, n=2, rmax=2.5)
    bite(s, 0, top - 2, z1, 4, 5, 4, rnd)
    scatter_growth(s, ((x0, FY + 1, z0), (x1, top, z1)), rnd, kelp=0.004, grass=0.02, pickles=0.01, lichen=0.015)
    scatter_growth(s, ((x0, top + 1, z0), (x1, top + 2, z1)), rnd, kelp=0.06, grass=0.2, kelp_h=(3, 6))
    s.grow_barnacles(((-1, FY, -1), (W + 1, top, D)), 0.12, 5101, support=lambda n: 'terracotta' in n)
    front_jig(s, 12)
    return s.done()


# ------------------------------------------------------------------------------------------------ department store
def dept_store():
    rnd = random.Random(5201)
    s = City('port_solace/dept_store', 5201)
    W, D = 31, 23
    s.pfill(0, 0, 0, W - 1, GY, D - 1, FOUND, rnd)
    lot_front(s, W, rnd)
    stone = [('minecraft:polished_diorite', 45), ('minecraft:smooth_quartz', 25), ('minecraft:calcite', 15),
             ('raft:cracked_concrete', 15)]
    st = tk.Style(corner=[('minecraft:polished_granite', 80), ('raft:cracked_concrete', 20)], pier=stone,
                  band=[('minecraft:polished_granite', 60), ('minecraft:granite', 25), ('raft:cracked_concrete', 15)],
                  wall=stone, glass=WIN, bay=5, floor=FLOOR, cornice='minecraft:polished_granite_stairs')
    x0, z0, x1, z1 = 0, 1, W - 1, D - 1

    def cell(side, i, n, k, r):
        if k == 0 and side == 'north':
            if 13 <= i <= 17 and r <= 3:
                return 'minecraft:water'
            return 'minecraft:glass_pane' if r <= 3 else None
        return None

    floors = 5
    top = tk.shell(s, x0, z0, x1, z1, FY, floors, 5, st, rnd, cell=cell)
    ax0, az0, ax1, az1 = 12, 8, 18, 15
    tk.slabs(s, x0, z0, x1, z1, FY, floors, 5, st, rnd,
             skip=lambda x, y, z: y > FY and ax0 <= x <= ax1 and az0 <= z <= az1)
    awning(s, 1, W - 2, FY + 5, 0, 'red')
    # atrium railings and a kelp garden at the bottom
    for k in range(1, floors):
        y = FY + 5 * k + 1
        for x in range(ax0 - 1, ax1 + 2):
            for z in (az0 - 1, az1 + 1):
                s.set(x, y, z, 'minecraft:iron_bars')
        for z in range(az0 - 1, az1 + 2):
            for x in (ax0 - 1, ax1 + 1):
                s.set(x, y, z, 'minecraft:iron_bars')
    for x in range(ax0, ax1 + 1):
        for z in range(az0, az1 + 1):
            s.set(x, FY, z, 'minecraft:mud')
            if rnd.random() < 0.3:
                s.kelp(x, FY + 1, z, rnd.randint(10, 24), glow=rnd.random() < 0.3)
    # escalator stairs between the floors (east side)
    for k in range(0, floors - 1):
        yb = FY + 5 * k + 1
        for i in range(5):
            for x in (24, 25):
                s.set(x, yb + i, 4 + i, stair('minecraft:polished_andesite_stairs', 'south'))
                for dy in (1, 2, 3):
                    if s.blocks.get((x, yb + i + dy, 4 + i), ('',))[0] not in ('minecraft:water',):
                        if yb + i + dy == FY + 5 * (k + 1):
                            s.set(x, yb + i + dy, 4 + i, 'minecraft:water')
    # floors: displays, shelves, mannequins, counters
    for k in range(floors):
        y = FY + 5 * k + 1
        for x in range(2, W - 2):
            if 9 <= x <= 21:
                continue
            for z in range(3, D - 3, 4):
                if s.free(x, y, z) and rnd.random() < 0.55:
                    s.set(x, y, z, 'minecraft:oak_shelf[facing=south]' if x % 2 else 'minecraft:barrel[facing=up,open=false]')
                    if rnd.random() < 0.4:
                        s.set(x, y + 1, z, 'minecraft:oak_shelf[facing=south]')
        for (mx, mz) in ((4, 2), (6, 2), (8, 2), (22, 2)):
            if k == 0 or rnd.random() < 0.5:
                mannequin(s, mx, y, mz, yaw=rnd.choice([0.0, 180.0, 90.0]))
        for x in range(10, 21):
            if s.free(x, y, D - 4):
                s.set(x, y, D - 4, 'minecraft:polished_diorite' if x % 5 else 'minecraft:dark_oak_planks')
        s.chest(2 + (k * 7) % 25, y, D - 2, 'raft:chests/port_solace_shop', facing='north',
                block='minecraft:barrel' if k % 2 else 'minecraft:chest')
        tk.ceiling_lights(s, x0, z0, x1, z1, y + 4, rnd, step=6, chance=0.5)
    # rooftop billboard
    for x in range(6, 25):
        for y in range(top + 3, top + 7):
            s.set(x, y, D - 3, 'minecraft:red_terracotta' if (y - top) in (4, 5) and 8 <= x <= 22 else 'minecraft:white_terracotta')
        s.set(x, top + 2, D - 3, 'raft:neon_tube_cyan[axis=x]' if x % 2 else 'raft:rusted_plate')
    for x in (7, 15, 23):
        s.set(x, top + 1, D - 3, 'raft:steel_beam[axis=y]')
        s.set(x, top + 2, D - 3, 'raft:steel_beam[axis=y]')
    tk.parapet(s, x0, z0, x1, z1, top + 1, 'minecraft:polished_granite_slab[type=bottom]', rnd, gaps=0.1)
    tk.break_windows(s, ((-1, 0, -1), (W, top + 2, D)), rnd, missing=0.35, cracked=0.25)
    bite(s, x1, FY + 18, z1, 5, 6, 5, rnd)
    rubble_pile(s, x1 - 3, FY + 1, z1 - 2, 3, rnd)
    scatter_growth(s, ((x0, FY + 1, z0), (x1, top, z1)), rnd, kelp=0.003, grass=0.02, pickles=0.01, lichen=0.012)
    s.grow_barnacles(((-1, FY, -1), (W, top, D)), 0.12, 5201, support=lambda n: 'granite' in n or 'diorite' in n)
    front_jig(s, 15)
    return s.done()


# ------------------------------------------------------------------------------------------------ apartments
def apartment(name, seed, W, D, floors, wall, band, pier, glass, roof='tank', balconies=True):
    rnd = random.Random(seed)
    s = City(f'port_solace/{name}', seed)
    s.pfill(0, 0, 0, W - 1, GY, D - 1, FOUND, rnd)
    lot_front(s, W, rnd)
    st = tk.Style(corner=pier, pier=pier, band=band, wall=wall, glass=glass, bay=3, floor=FLOOR,
                  sill='minecraft:stone_brick_stairs', cornice='minecraft:stone_brick_stairs')
    x0, z0, x1, z1 = 0, 1, W - 1, D - 1
    mid = W // 2

    def cell(side, i, n, k, r):
        if side == 'north' and k == 0 and abs(i - mid) <= 1:
            return 'minecraft:water'
        if balconies and side == 'north' and k >= 1 and i % 6 in (1, 2) and r <= 2 and (k + i // 6) % 2 == 0:
            return 'minecraft:water'
        return None

    top = tk.shell(s, x0, z0, x1, z1, FY, floors, 4, st, rnd, cell=cell)
    tk.slabs(s, x0, z0, x1, z1, FY, floors, 4, st, rnd)
    if balconies:
        for k in range(1, floors):
            for i in range(0, W - 1, 6):
                if (k + i // 6) % 2 == 0:
                    balcony(s, i + 1, i + 2, FY + 4 * k, 0)
    # entrance hood
    for x in range(mid - 2, mid + 3):
        s.set(x, FY + 4, 0, slab('minecraft:smooth_stone_slab', 'top'))
    # core in the middle back, units left and right
    sx0 = mid - 2
    tk.spiral_stairs(s, sx0, D - 6, sx0 + 3, D - 3, FY + 1, top - 1, 'minecraft:stone_brick_stairs',
                     newel=STONE_TRIM, rnd=rnd, walls=STONE_TRIM, doors='north', fh=4, y0=FY)
    for k in range(1, floors):
        y = FY + 4 * k + 1
        tk.apartment_unit(s, x0 + 1, z0 + 1, sx0 - 2, z1 - 1, y, rnd,
                          loot='raft:chests/port_solace_apartment' if rnd.random() < 0.45 else None)
        tk.apartment_unit(s, sx0 + 5, z0 + 1, x1 - 1, z1 - 1, y, rnd,
                          loot='raft:chests/port_solace_apartment' if rnd.random() < 0.45 else None)
        tk.partition(s, sx0 - 1, z0 + 1, sx0 - 1, z1 - 1, y, 3, wall, rnd, doors={(sx0 - 1, z0 + 2)})
        tk.partition(s, sx0 + 4, z0 + 1, sx0 + 4, z1 - 1, y, 3, wall, rnd, doors={(sx0 + 4, z0 + 2)})
        s.set(mid, y + 2, z0 + 2, 'minecraft:lantern[hanging=true]')
    # ground floor: mailboxes, lobby, a small shop corner
    for x in range(1, mid - 2):
        s.set(x, FY + 1, D - 3, 'minecraft:iron_trapdoor[facing=north,half=bottom,open=true]')
    tk.shop_floor(s, mid + 3, 2, x1 - 1, z1 - 1, FY + 1, rnd, loot='raft:chests/port_solace_shop', kind='general')
    # roof
    tk.parapet(s, x0, z0, x1, z1, top + 1, 'minecraft:stone_brick_wall', rnd, gaps=0.15)
    if roof == 'tank':
        water_tank(s, 4, top + 1, D - 5)
        for x in range(8, W - 3):
            s.set(x, top + 3, 5, 'minecraft:iron_chain[axis=x]')
        s.set(7, top + 1, 5, 'minecraft:spruce_fence'); s.set(7, top + 2, 5, 'minecraft:spruce_fence')
        s.set(7, top + 3, 5, 'minecraft:spruce_fence')
        s.set(W - 3, top + 1, 5, 'minecraft:spruce_fence'); s.set(W - 3, top + 2, 5, 'minecraft:spruce_fence')
        s.set(W - 3, top + 3, 5, 'minecraft:spruce_fence')
    else:
        s.set(W - 4, top + 1, D - 4, 'raft:satellite_dish[facing=south]')
        for y in range(top + 1, top + 6):
            s.set(3, y, 3, 'minecraft:iron_bars')
        s.set(3, top + 6, 3, 'minecraft:lightning_rod[facing=up]')
    tk.break_windows(s, ((-1, 0, -1), (W, top + 2, D)), rnd, missing=0.3, cracked=0.25)
    tk.floor_holes(s, x0 + 1, z0 + 1, x1 - 1, z1 - 1, [FY + 4 * (floors - 2)], rnd, n=1, rmax=2.5)
    bite(s, x1 if seed % 2 else x0, top - 3, z0 + 2, 3.5, 5, 3.5, rnd)
    scatter_growth(s, ((x0, FY + 1, z0), (x1, top, z1)), rnd, kelp=0.004, grass=0.02, pickles=0.01, lichen=0.015)
    scatter_growth(s, ((x0, top + 1, z0), (x1, top + 2, z1)), rnd, kelp=0.05, grass=0.2, kelp_h=(3, 10))
    s.grow_barnacles(((-1, FY, -1), (W, top, D)), 0.14, seed, support=lambda n: 'brick' in n or 'terracotta' in n)
    front_jig(s, mid)
    return s.done()


# ------------------------------------------------------------------------------------------------ shops
def shop_row():
    rnd = random.Random(5401)
    s = City('port_solace/shop_row', 5401)
    W, D = 27, 13
    s.pfill(0, 0, 0, W - 1, GY, D - 1, FOUND, rnd)
    lot_front(s, W, rnd)
    fronts = [(0, 8, BRICK, 'red', 'raft:neon_tube_pink'), (9, 17, PLASTER, 'green', 'raft:neon_tube_cyan'),
              (18, 26, TEAL, 'blue', 'raft:neon_tube_pink')]
    for i, (a, b, wall, aw, neon) in enumerate(fronts):
        floors = 3 if i != 1 else 2
        st = tk.Style(corner=STONE_TRIM, pier=wall, band=STONE_TRIM, wall=wall, glass=WIN, bay=4, floor=FLOOR,
                      sill='minecraft:stone_brick_stairs', cornice='minecraft:stone_brick_stairs')

        def cell(side, ii, n, k, r, a=a):
            if side == 'north' and k == 0:
                if ii in (3, 4) and r <= 2:
                    return 'minecraft:water'
                return 'minecraft:glass_pane' if r <= 2 else None
            return None

        top = tk.shell(s, a, 1, b, D - 1, FY, floors, 4, st, rnd, cell=cell)
        tk.slabs(s, a, 1, b, D - 1, FY, floors, 4, st, rnd)
        awning(s, a + 1, b - 1, FY + 4, 0, aw)
        for x in range(a + 2, b - 1):
            s.set(x, FY + 5, 0, f'{neon}[axis=x]' if x % 2 else 'minecraft:water')
        tk.shop_floor(s, a + 1, 2, b - 1, D - 2, FY + 1, rnd, loot='raft:chests/port_solace_shop',
                      kind='diner' if i == 1 else 'general')
        for k in range(1, floors):
            tk.apartment_unit(s, a + 1, 2, b - 1, D - 2, FY + 4 * k + 1, rnd,
                              loot='raft:chests/port_solace_apartment' if rnd.random() < 0.4 else None)
        tk.parapet(s, a, 1, b, D - 1, top + 1, 'minecraft:stone_brick_wall', rnd, gaps=0.2)
        # hatch in every floor so divers can swim up
        for k in range(1, floors + 1):
            s.set(b - 2, FY + 4 * k, D - 3, 'minecraft:water')
    tk.break_windows(s, ((-1, 0, -1), (W, 20, D)), rnd, missing=0.35, cracked=0.25)
    bite(s, 26, 12, 12, 3, 4, 3, rnd)
    scatter_growth(s, ((0, FY + 1, 0), (W - 1, 20, D - 1)), rnd, kelp=0.01, grass=0.05, pickles=0.01, lichen=0.012)
    s.grow_barnacles(((-1, FY, -1), (W, 18, D)), 0.15, 5401, support=lambda n: 'brick' in n or 'terracotta' in n)
    front_jig(s, 13)
    return s.done()


def diner():
    rnd = random.Random(5501)
    s = City('port_solace/diner', 5501)
    W, D = 17, 13
    s.pfill(0, 0, 0, W - 1, GY, D - 1, FOUND, rnd)
    lot_front(s, W, rnd)
    chrome = [('minecraft:white_concrete', 45), ('minecraft:iron_block', 15), ('minecraft:white_terracotta', 25),
              ('raft:cracked_concrete', 15)]
    st = tk.Style(corner=chrome, pier=chrome, band=[('minecraft:red_terracotta', 85), ('raft:cracked_concrete', 15)],
                  wall=chrome, glass=[('minecraft:light_blue_stained_glass_pane', 60), ('raft:cracked_glass_pane', 25),
                                      ('minecraft:water', 15)], bay=4, floor=FLOOR, lintel=False)

    def cell(side, i, n, k, r):
        if side == 'north' and i in (7, 8, 9) and r <= 2:
            return 'minecraft:water'
        return None

    top = tk.shell(s, 0, 1, W - 1, D - 1, FY, 1, 5, st, rnd, cell=cell)
    tk.slabs(s, 0, 1, W - 1, D - 1, FY, 1, 5, st, rnd)
    for x in range(-1, W + 1):
        s.set(x, top, 0, slab('minecraft:red_nether_brick_slab', 'top'))
    # booths along the windows
    for x in range(1, 15, 4):
        if x in (5, 9) and False:
            continue
        s.set(x, FY + 1, 2, stair('minecraft:red_nether_brick_stairs', 'north'))
        s.set(x + 1, FY + 1, 2, 'raft:driftwood_table')
        s.set(x + 2, FY + 1, 2, stair('minecraft:red_nether_brick_stairs', 'north'))
    for x in range(2, 15):
        s.set(x, FY + 1, 8, 'minecraft:smooth_quartz' if x % 5 else 'minecraft:red_terracotta')
        s.set(x, FY + 2, 8, slab('minecraft:smooth_quartz_slab'))
        if x % 2 == 0:
            s.set(x, FY + 1, 6, 'minecraft:nether_brick_fence')
    s.set(14, FY + 1, 11, 'minecraft:jukebox')
    s.set(2, FY + 1, 11, 'minecraft:smoker[facing=north,lit=false]')
    s.set(3, FY + 1, 11, 'minecraft:smoker[facing=north,lit=false]')
    s.chest(5, FY + 1, 11, 'raft:chests/port_solace_shop', facing='north')
    s.set(8, FY + 4, 5, 'minecraft:sea_lantern')
    # roof sign: neon frame
    for x in range(3, 14):
        s.set(x, top + 1, 2, 'minecraft:red_terracotta' if 4 <= x <= 12 else 'minecraft:iron_block')
        s.set(x, top + 2, 2, 'raft:neon_tube_pink[axis=x]' if x % 2 else 'minecraft:white_concrete')
        s.set(x, top + 3, 2, 'minecraft:red_terracotta' if 4 <= x <= 12 else 'minecraft:iron_block')
    tk.break_windows(s, ((-1, 0, -1), (W, 12, D)), rnd, missing=0.3, cracked=0.3)
    car(s, 1, FY + 1, 0, along='x', rnd=rnd, colour='red', wreck=0.4) if False else None
    scatter_growth(s, ((0, FY + 1, 0), (W - 1, top + 2, D - 1)), rnd, kelp=0.02, grass=0.08, pickles=0.02, lichen=0.01)
    s.grow_barnacles(((-1, FY, -1), (W, top + 2, D)), 0.15, 5501, support=lambda n: 'concrete' in n)
    front_jig(s, 8)
    return s.done()


# ------------------------------------------------------------------------------------------------ small lots
def rubble_lot():
    rnd = random.Random(5601)
    s = City('port_solace/rubble_lot', 5601)
    W, D = 15, 13
    s.pfill(0, 0, 0, W - 1, GY, D - 1, FOUND, rnd)
    lot_front(s, W, rnd)
    for x in range(W):
        for z in range(1, D):
            s.set(x, FY, z, pick(rnd, FLOOR))
            for y in range(FY + 1, FY + 10):
                s.set(x, y, z, 'minecraft:water')
    # standing wall fragments
    for x in range(0, W):
        hh = int(9 - abs(x - 4) * 1.4 + rnd.random() * 2)
        for y in range(FY + 1, FY + 1 + max(0, hh)):
            s.set(x, y, D - 1, pick(rnd, BRICK) if (x % 3 or y % 4 == 0) else 'minecraft:water')
    for z in range(1, D):
        hh = int(6 - abs(z - 8) + rnd.random() * 2)
        for y in range(FY + 1, FY + 1 + max(0, hh)):
            s.set(0, y, z, pick(rnd, BRICK))
    for _ in range(4):
        rubble_pile(s, rnd.randint(2, W - 3), FY + 1, rnd.randint(3, D - 3), rnd.randint(1, 3), rnd, height=3)
    car(s, 8, FY + 1, 3, along='x', rnd=rnd, wreck=0.6, flipped=True)
    s.chest(2, FY + 1, D - 2, 'raft:chests/port_solace_apartment', facing='north', block='minecraft:barrel')
    s.line((1, FY + 1, 2), (6, FY + 5, D - 2), 'raft:steel_beam[axis=z]')
    scatter_growth(s, ((0, FY + 1, 0), (W - 1, FY + 6, D - 1)), rnd, kelp=0.05, grass=0.15, pickles=0.03,
                   kelp_h=(5, 16))
    s.grow_barnacles(((0, FY, 0), (W - 1, FY + 10, D - 1)), 0.2, 5601)
    front_jig(s, 7)
    return s.done()


def park_lot():
    rnd = random.Random(5701)
    s = City('port_solace/park_lot', 5701)
    W, D = 15, 13
    s.pfill(0, 0, 0, W - 1, GY, D - 1, FOUND, rnd)
    lot_front(s, W, rnd)
    for x in range(W):
        for z in range(1, D):
            path = x == 7 or z == 6
            s.set(x, FY, z, pick(rnd, PAVE_SLAB) if path else ('minecraft:mud' if rnd.random() < 0.3 else 'minecraft:sand'))
            if not path:
                s.set(x, FY - 1, z, 'minecraft:dirt')
            for y in range(FY + 1, FY + 10):
                s.set(x, y, z, 'minecraft:water')
    for x in range(W):
        for z in (1, D - 1):
            s.set(x, FY + 1, z, 'minecraft:spruce_fence' if rnd.random() < 0.8 else 'minecraft:water')
    # gazebo
    gx, gz = 10, 9
    for (dx, dz) in ((-2, -2), (2, -2), (-2, 2), (2, 2)):
        for y in range(FY + 1, FY + 4):
            s.set(gx + dx, y, gz + dz, 'minecraft:white_terracotta' if False else 'minecraft:birch_fence')
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            s.set(gx + dx, FY + 4, gz + dz, slab('minecraft:dark_oak_slab'))
    s.set(gx, FY + 5, gz, slab('minecraft:dark_oak_slab'))
    bench(s, 2, FY + 1, 5, 'south', length=3, along='x')
    bench(s, 2, FY + 1, 8, 'north', length=3, along='x')
    lamp_post(s, 7, 6, arm='east', y=FY + 1, h=4, kind='lantern')
    for (x, z) in ((3, 3), (12, 3), (3, 10)):
        h = rnd.randint(4, 6)
        for y in range(FY + 1, FY + 1 + h):
            s.set(x, y, z, 'minecraft:stripped_oak_log[axis=y]')
        for (dx, dz) in ((1, 0), (-1, 0), (0, 1)):
            s.set(x + dx, FY + h, z + dz, 'minecraft:oak_fence')
    # swing
    for y in range(FY + 1, FY + 5):
        s.set(1, y, 10, 'minecraft:spruce_fence')
        s.set(4, y, 10, 'minecraft:spruce_fence')
    for x in (1, 2, 3, 4):
        s.set(x, FY + 5, 10, slab('minecraft:spruce_slab'))
    for y in (FY + 2, FY + 3, FY + 4):
        s.set(2, y, 10, 'minecraft:iron_chain[axis=y]')
    s.set(2, FY + 1, 10, slab('minecraft:spruce_slab', 'top'))
    for _ in range(14):
        x, z = rnd.randint(1, W - 2), rnd.randint(2, D - 2)
        if s.on_top(x, FY + 1, z):
            s.kelp(x, FY + 1, z, rnd.randint(6, 18), glow=rnd.random() < 0.35)
    scatter_growth(s, ((0, FY + 1, 0), (W - 1, FY + 2, D - 1)), rnd, kelp=0.0, grass=0.4, pickles=0.04)
    front_jig(s, 7)
    return s.done()


def build_all():
    return {
        'hotel': hotel(),
        'dept_store': dept_store(),
        'apartment_brick': apartment('apartment_brick', 5301, 25, 14, 7, BRICK, STONE_TRIM, STONE_TRIM, WIN, 'tank'),
        'apartment_teal': apartment('apartment_teal', 5302, 19, 15, 6, PLASTER, TEAL, TEAL, WIN, 'dish', balconies=True),
        'shop_row': shop_row(),
        'diner': diner(),
        'rubble_lot': rubble_lot(),
        'park_lot': park_lot(),
    }
