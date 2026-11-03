"""The skyscrapers of Port Solace.

- deco_tower (Meridian Power Authority): art-deco limestone tower with copper spandrels and ribs, setback at the 7th
  floor, a glowing crystal grid core in the lobby (terminal solace_1); deco_tower_top: stepped crown with copper fins
  and a light that breaks the sea surface.
- glass_tower (Harbour Authority): steel and glass curtain wall, offices, harbour master's room (terminal solace_2);
  glass_tower_top: the snapped-off top floors with bent beams, reaching out of the water.
- fallen_tower: a stump and the rest of an office tower lying on its side across the lot.
"""
import math
import random

from .citylib import (CONCRETE, CONCRETE_DARK, FLOOR, FOUND, FY, GY, PAVE_SLAB, RUBBLE, City, bite, lamp_post,
                      rubble_pile, scatter_growth, slab, stair)
from .common import DIRS, OPP, pick
from . import towerkit as tk

LIMESTONE = [('minecraft:smooth_sandstone', 45), ('minecraft:cut_sandstone', 20), ('minecraft:sandstone', 12),
             ('minecraft:white_terracotta', 13), ('raft:cracked_concrete', 10)]
COPPER_SP = [('minecraft:oxidized_cut_copper', 55), ('minecraft:weathered_cut_copper', 25),
             ('minecraft:oxidized_copper', 20)]
STEEL = [('minecraft:gray_concrete', 50), ('minecraft:polished_andesite', 20), ('raft:rusted_plate', 15),
         ('minecraft:light_gray_concrete', 15)]
GLASS = [('minecraft:light_blue_stained_glass_pane', 72), ('raft:cracked_glass_pane', 18), ('minecraft:water', 10)]


def lot_front(s, W, rnd, depth=1):
    for x in range(W):
        for z in range(depth):
            s.set(x, FY, z, pick(rnd, PAVE_SLAB))
            for y in range(FY + 1, FY + 6):
                s.set(x, y, z, 'minecraft:water')


def front_jig(s, x):
    s.jig(x, GY, 0, 'north', 'front', 'lot', 'minecraft:empty', 'minecraft:stone')


# ------------------------------------------------------------------------------------------------ art deco
def deco_base():
    rnd = random.Random(4101)
    s = City('port_solace/deco_tower_base', 4101)
    W, D = 23, 25
    s.pfill(0, 0, 0, W - 1, GY, D - 1, FOUND, rnd)
    lot_front(s, W, rnd)
    st = tk.Style(corner=LIMESTONE, pier=LIMESTONE, band=COPPER_SP, wall=LIMESTONE,
                  glass=[('minecraft:gray_stained_glass_pane', 50), ('minecraft:cyan_stained_glass_pane', 20),
                         ('raft:cracked_glass_pane', 20), ('minecraft:water', 10)],
                  bay=2, pier_out=True, floor=FLOOR, cornice='minecraft:smooth_sandstone_stairs')
    x0, z0, x1, z1 = 0, 1, W - 1, D - 1

    def low_cell(side, i, n, k, r):
        if side == 'north' and k <= 1 and 9 <= i <= 13:
            return 'minecraft:water'                    # tall portal
        if k == 0 and r == 1:
            return 'sill'
        return None

    t1 = tk.shell(s, x0, z0, x1, z1, FY, 6, 4, st, rnd, cell=low_cell)          # y 3..27
    tk.slabs(s, x0, z0, x1, z1, FY, 6, 4, st, rnd, skip=lambda x, y, z: y == FY + 4 and 6 <= x <= 16 and 5 <= z <= 16)
    # setback terrace with copper balustrade
    ux0, uz0, ux1, uz1 = 2, 3, W - 3, D - 3
    st2 = tk.Style(**{**st.__dict__, 'cornice': 'minecraft:smooth_sandstone_stairs'})
    t2 = tk.shell(s, ux0, uz0, ux1, uz1, t1, 5, 4, st2, rnd)                     # y 27..47
    assert t2 == 47
    tk.slabs(s, ux0, uz0, ux1, uz1, t1, 5, 4, st2, rnd)
    for x, z, side, i, n in tk.ring(x0, z0, x1, z1):
        s.set(x, t1 + 1, z, 'minecraft:oxidized_copper_bars' if side else 'minecraft:smooth_sandstone')
    # portal: copper frame + sunburst
    for y in range(FY + 1, FY + 9):
        for x in (8, 14):
            s.set(x, y, z0, 'minecraft:oxidized_copper')
            s.set(x, y, z0 - 1, 'minecraft:oxidized_cut_copper_stairs[facing=south,half=bottom]' if y == FY + 1
                  else 'minecraft:oxidized_copper_bars')
    for x in range(8, 15):
        s.set(x, FY + 8, z0, 'minecraft:waxed_oxidized_copper')
    for (dx, dy) in ((0, 0), (-1, 0), (1, 0), (-2, -1), (2, -1), (0, 1)):
        s.set(11 + dx, FY + 9 + dy, z0, 'minecraft:gold_block' if dy >= 0 else 'minecraft:waxed_copper_block')
    # lobby: two storeys, crystal grid core in the middle
    for x in range(6, 17):
        for z in range(5, 17):
            if s.free(x, FY + 4, z):
                s.set(x, FY + 4, z, 'minecraft:water')
    cx, cz = 11, 11
    for y in range(FY + 1, FY + 8):
        for (dx, dz) in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
            s.set(cx + dx, y, cz + dz, 'minecraft:waxed_oxidized_copper' if y in (FY + 1, FY + 7)
                  else 'minecraft:oxidized_copper_bars')
        if y in (FY + 1, FY + 7):
            for (dx, dz) in ((0, -1), (0, 1), (-1, 0), (1, 0)):
                s.set(cx + dx, y, cz + dz, 'raft:glowing_tidestone')
        s.set(cx, y, cz, 'minecraft:sea_lantern' if y % 2 else 'minecraft:light_blue_stained_glass')
    s.set(cx, FY + 2, cz - 3, 'minecraft:amethyst_cluster[facing=up]')
    s.set(cx + 3, FY + 1, cz, 'raft:control_panel[facing=west]')
    s.set(cx - 3, FY + 1, cz, 'raft:control_panel[facing=east]')
    s.block_entity(cx, FY + 1, cz + 3, 'raft:research_terminal[facing=south]', 'raft:research_terminal',
                   {'LogId': 'solace_1'})
    for (x, z) in ((7, 6), (15, 6), (7, 15), (15, 15)):
        for y in range(FY + 1, FY + 8):
            s.set(x, y, z, 'minecraft:smooth_sandstone' if y not in (FY + 1, FY + 7) else 'minecraft:chiseled_sandstone')
    # stairs + elevator core
    tk.spiral_stairs(s, 4, 17, 7, 20, FY + 1, 46, 'minecraft:smooth_sandstone_stairs', newel=LIMESTONE, rnd=rnd)
    tk.elevator(s, 17, 19, 18, 20, FY + 1, 46, rnd, walls=LIMESTONE, cab_y=FY + 13)
    for k in range(0, 11):
        tk.door_opening(s, 17, FY + 4 * k + 1, 18, h=3, w=2)
    # offices
    for k in range(2, 11):
        y = FY + 4 * k + 1
        lo = (x0 + 1, z0 + 1, x1 - 1, z1 - 1) if k < 6 else (ux0 + 1, uz0 + 1, ux1 - 1, uz1 - 1)
        tk.office_floor(s, *lo, y, rnd, loot='raft:chests/port_solace_office' if k in (3, 8) else None,
                        terminal='solace_1' if k == 9 else None,
                        avoid=lambda x, z: (3 <= x <= 8 and 16 <= z <= 21) or (16 <= x <= 19 and 17 <= z <= 21))
        tk.ceiling_lights(s, lo[0] - 1, lo[1] - 1, lo[2] + 1, lo[3] + 1, y + 3, rnd, step=6, chance=0.55)
    # decay
    tk.break_windows(s, ((-1, 0, 0), (W, 47, D)), rnd, missing=0.25, cracked=0.25)
    tk.floor_holes(s, ux0 + 1, uz0 + 1, ux1 - 1, uz1 - 1, [FY + 4 * 8], rnd, n=2, rmax=3)
    bite(s, x1, 22, z1, 4, 5, 4, rnd)
    bite(s, ux0, 42, uz0, 3, 4, 3, rnd)
    rubble_pile(s, x1 - 1, FY + 1, z1 + 1, 2, rnd)
    scatter_growth(s, ((x0, FY + 1, z0), (x1, 46, z1)), rnd, kelp=0.004, grass=0.02, pickles=0.01, lichen=0.012)
    scatter_growth(s, ((x0 - 1, t1 + 1, z0 - 1), (x1 + 1, t1 + 2, z1 + 1)), rnd, kelp=0.04, grass=0.15, pickles=0.02,
                   kelp_h=(3, 9))
    s.grow_barnacles(((-1, FY, -1), (W, 30, D)), 0.12, 4101, support=lambda n: 'sandstone' in n or 'copper' in n)
    front_jig(s, 11)
    s.jig(11, 47, 13, 'up', 'top_socket', 'deco_top', 'tops/deco_tower', 'minecraft:smooth_stone', prio=1)
    return s.done()


def deco_top():
    rnd = random.Random(4102)
    s = City('port_solace/deco_tower_top', 4102)
    Y = 48
    st = tk.Style(corner=LIMESTONE, pier=LIMESTONE, band=COPPER_SP, wall=LIMESTONE,
                  glass=[('minecraft:gray_stained_glass_pane', 60), ('raft:cracked_glass_pane', 25), ('minecraft:water', 15)],
                  bay=2, pier_out=True, floor=FLOOR, cornice='minecraft:smooth_sandstone_stairs')
    # tiers: 15x15 (2 floors), 11x11 (2 floors), 7x7 (1 floor), spire
    tiers = [(4, 5, 18, 19, 2), (6, 7, 16, 17, 1), (8, 9, 14, 15, 1)]
    y = Y
    for (a, b, c, d, n) in tiers:
        top = tk.shell(s, a, b, c, d, y, n, 4, st, rnd)
        tk.slabs(s, a, b, c, d, y, n, 4, st, rnd)
        # copper fins on the corners of each tier
        for (fx, fz) in ((a - 1, b - 1), (c + 1, b - 1), (a - 1, d + 1), (c + 1, d + 1)):
            for yy in range(y, top + 3):
                s.set(fx, yy, fz, 'minecraft:oxidized_copper' if yy < top + 2 else 'minecraft:oxidized_lightning_rod[facing=up]')
        y = top
    # spire with a beacon light (seen from the raft at night)
    for yy in range(y + 1, y + 6):
        s.set(11, yy, 12, 'minecraft:waxed_oxidized_cut_copper' if yy < y + 3 else 'minecraft:oxidized_copper_bars')
        if yy == y + 2:
            for (dx, dz) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                s.set(11 + dx, yy, 12 + dz, 'minecraft:oxidized_cut_copper_stairs[facing=' +
                      {(1, 0): 'west', (-1, 0): 'east', (0, 1): 'north', (0, -1): 'south'}[(dx, dz)] + ',half=top]')
    s.set(11, y + 6, 12, 'minecraft:sea_lantern')
    s.set(11, y + 7, 12, 'minecraft:oxidized_lightning_rod[facing=up]')
    # observation floor below the spire: telescope, benches
    s.set(9, Y + 9, 10, 'raft:spyglass_stand[facing=north]')
    s.set(13, Y + 9, 14, 'minecraft:lantern[hanging=false]')
    tk.break_windows(s, ((0, Y, 0), (30, Y + 30, 30)), rnd, missing=0.25, cracked=0.25)
    bite(s, 18, Y + 6, 19, 2.5, 3, 2.5, rnd)
    s.grow_barnacles(((0, Y, 0), (30, Y + 4, 30)), 0.15, 4102, support=lambda n: 'sandstone' in n)
    s.jig(11, Y, 13, 'down', 'deco_top', 'top_socket', 'minecraft:empty', 'minecraft:smooth_stone')
    return s.done()


# ------------------------------------------------------------------------------------------------ glass tower
def glass_base():
    rnd = random.Random(4201)
    s = City('port_solace/glass_tower_base', 4201)
    W, D = 19, 21
    s.pfill(0, 0, 0, W - 1, GY, D - 1, FOUND, rnd)
    lot_front(s, W, rnd)
    st = tk.Style(corner=STEEL, pier=[('raft:steel_beam[axis=y]', 70), ('minecraft:gray_concrete', 30)],
                  band=[('minecraft:gray_concrete', 75), ('minecraft:polished_andesite', 15), ('raft:cracked_concrete', 10)],
                  wall=STEEL, glass=GLASS, bay=3, lintel=False, floor=FLOOR)
    x0, z0, x1, z1 = 0, 1, W - 1, D - 1

    def cell(side, i, n, k, r):
        if side == 'north' and k == 0 and 7 <= i <= 11:
            return 'minecraft:water' if r < 3 else 'minecraft:glass_pane'
        return None

    top = tk.shell(s, x0, z0, x1, z1, FY, 11, 4, st, rnd, cell=cell)
    assert top == 47
    tk.slabs(s, x0, z0, x1, z1, FY, 11, 4, st, rnd)
    # canopy over the entrance
    for x in range(6, 13):
        s.set(x, FY + 4, 0, slab('minecraft:polished_andesite_slab', 'top'))
    s.set(6, FY + 3, 0, 'raft:steel_beam[axis=y]')
    s.set(12, FY + 3, 0, 'raft:steel_beam[axis=y]')
    # core: stairs + elevators in the middle (concrete)
    tk.spiral_stairs(s, 8, 12, 11, 15, FY + 1, 46, 'minecraft:polished_andesite_stairs', newel=CONCRETE_DARK, rnd=rnd,
                     walls=CONCRETE_DARK, doors='north', fh=4, y0=FY)
    tk.elevator(s, 13, 12, 14, 13, FY + 1, 46, rnd, walls=CONCRETE_DARK, cab_y=FY + 21)
    for k in range(0, 11):
        tk.door_opening(s, 13, FY + 4 * k + 1, 11, h=3, w=2)
    # lobby
    for x in range(4, 15):
        s.set(x, FY + 1, 6, 'minecraft:polished_andesite' if x not in (4, 14) else 'minecraft:gray_concrete')
        s.set(x, FY + 2, 6, slab('minecraft:smooth_quartz_slab'))
    s.set(9, FY + 1, 7, stair('minecraft:dark_oak_stairs', 'north'))
    for k in range(1, 11):
        y = FY + 4 * k + 1
        tk.office_floor(s, x0 + 1, z0 + 1, x1 - 1, z1 - 1, y, rnd,
                        loot='raft:chests/port_solace_office' if k in (2, 6, 9) else None,
                        terminal='solace_2' if k == 4 else None,
                        avoid=lambda x, z: 6 <= x <= 15 and 10 <= z <= 17)
        tk.ceiling_lights(s, x0, z0, x1, z1, y + 3, rnd, step=5, chance=0.5)
    # harbour master's room on floor 4: chart, life ring, bell
    y = FY + 4 * 4 + 1
    s.set(x1 - 1, y + 1, 4, 'raft:life_ring[facing=west]')
    s.set(x0 + 1, y + 1, 4, 'raft:ship_bell[facing=east]')
    s.set(x0 + 2, y, 3, 'raft:ship_lantern[hanging=false]')
    tk.break_windows(s, ((-1, 0, 0), (W, 47, D)), rnd, missing=0.15, cracked=0.15)
    tk.floor_holes(s, x0 + 1, z0 + 1, x1 - 1, z1 - 1, [FY + 4 * 3, FY + 4 * 7], rnd, n=1, rmax=2.5)
    bite(s, x0, 30, z1, 4, 6, 4, rnd)
    rubble_pile(s, x0 + 2, FY + 1, z1 - 2, 2, rnd)
    scatter_growth(s, ((x0, FY + 1, z0), (x1, 46, z1)), rnd, kelp=0.004, grass=0.02, pickles=0.01, lichen=0.015)
    s.grow_barnacles(((-1, FY, -1), (W, 40, D)), 0.1, 4201, support=lambda n: 'concrete' in n or 'beam' in n)
    front_jig(s, 9)
    s.jig(9, 47, 11, 'up', 'top_socket', 'glass_top', 'tops/glass_tower', 'minecraft:smooth_stone', prio=1)
    return s.done()


def glass_top():
    rnd = random.Random(4202)
    s = City('port_solace/glass_tower_top', 4202)
    Y = 48
    W, D = 19, 21
    st = tk.Style(corner=STEEL, pier=[('raft:steel_beam[axis=y]', 70), ('minecraft:gray_concrete', 30)],
                  band=[('minecraft:gray_concrete', 75), ('minecraft:polished_andesite', 15), ('raft:cracked_concrete', 10)],
                  wall=STEEL, glass=GLASS, bay=3, lintel=False, floor=FLOOR)
    x0, z0, x1, z1 = 0, 1, W - 1, D - 1
    top = tk.shell(s, x0, z0, x1, z1, Y, 4, 4, st, rnd)
    tk.slabs(s, x0, z0, x1, z1, Y, 4, 4, st, rnd)
    tk.spiral_stairs(s, 8, 12, 11, 15, Y, top - 1, 'minecraft:polished_andesite_stairs', newel=CONCRETE_DARK, rnd=rnd)
    for k in range(0, 3):
        tk.office_floor(s, x0 + 1, z0 + 1, x1 - 1, z1 - 1, Y + 4 * k + 1, rnd,
                        avoid=lambda x, z: 6 <= x <= 15 and 10 <= z <= 17)
    # rooftop gear that slid to the edge
    s.set(3, top + 1, 4, 'raft:satellite_dish[facing=north]')
    tk.break_windows(s, ((-1, Y, 0), (W, Y + 30, D)), rnd, missing=0.3, cracked=0.3)
    # snapped top: the break runs diagonally, high in the south-west corner
    tk.broken_top(s, x0, z0, x1, z1, Y + 3, top, rnd, jag=10, seed_peaks=2)
    s.jig(9, Y, 11, 'down', 'glass_top', 'top_socket', 'minecraft:empty', 'minecraft:smooth_stone')
    return s.done()


# ------------------------------------------------------------------------------------------------ fallen tower
def fallen_tower():
    """A tower snapped at the 3rd floor and fell backwards across the lot (lying on its back, windows up)."""
    rnd = random.Random(4301)
    s = City('port_solace/fallen_tower', 4301)
    W, D = 17, 44
    s.pfill(0, 0, 0, W - 1, GY, D - 1, FOUND, rnd)
    lot_front(s, W, rnd)
    st = tk.Style(corner=CONCRETE_DARK, pier=CONCRETE, band=CONCRETE_DARK, wall=CONCRETE,
                  glass=[('minecraft:gray_stained_glass_pane', 40), ('raft:cracked_glass_pane', 30), ('minecraft:water', 30)],
                  bay=3, floor=FLOOR)
    # stump: 3 floors, jagged
    x0, z0, x1, z1 = 2, 1, 14, 13
    top = tk.shell(s, x0, z0, x1, z1, FY, 4, 4, st, rnd)
    tk.slabs(s, x0, z0, x1, z1, FY, 4, 4, st, rnd)
    tk.office_floor(s, x0 + 1, z0 + 1, x1 - 1, z1 - 1, FY + 1, rnd, loot='raft:chests/port_solace_office')
    tk.broken_top(s, x0, z0, x1, z1, FY + 8, FY + 15, rnd, jag=8, seed_peaks=2)
    # the fallen part: a box lying along +z, its former front facing up
    fz0, fz1 = 16, 42
    fy0 = FY + 1
    fx0, fx1 = 2, 14
    h = 11          # lying: former width -> height
    for x in range(fx0, fx1 + 1):
        for z in range(fz0, fz1 + 1):
            for y in range(fy0, fy0 + h):
                edge_x = x in (fx0, fx1)
                edge_y = y in (fy0, fy0 + h - 1)
                floor_plate = (z - fz0) % 4 == 0
                if edge_x or edge_y or floor_plate:
                    if y == fy0 + h - 1 and not edge_x and not floor_plate:
                        # former facade, now facing up: windows
                        s.set(x, y, z, pick(rnd, st.glass) if (x - fx0) % 3 else pick(rnd, CONCRETE))
                    elif edge_x and not floor_plate and fy0 < y < fy0 + h - 1:
                        s.set(x, y, z, pick(rnd, st.glass) if (y - fy0) % 3 else pick(rnd, CONCRETE))
                    else:
                        s.set(x, y, z, pick(rnd, CONCRETE))
                else:
                    s.set(x, y, z, 'minecraft:water')
                    s.interior.add((x, y, z))
    # furniture fallen against the former side walls (now floors): desks on their sides, barrels
    for z in range(fz0 + 1, fz1, 4):
        for dz in (1, 2):
            if rnd.random() < 0.5:
                s.set(rnd.randint(fx0 + 1, fx1 - 1), fy0 + 1, z + dz, rnd.choice([
                    'minecraft:barrel[facing=east,open=false]', 'minecraft:spruce_slab[type=bottom]',
                    'minecraft:chiseled_bookshelf[facing=up]' if False else 'minecraft:bookshelf',
                    'raft:cracked_concrete_slab[type=bottom]']))
    s.chest(fx0 + 2, fy0 + 1, fz0 + 10, 'raft:chests/port_solace_office', facing='east', block='minecraft:barrel')
    s.block_entity(fx1 - 2, fy0 + 1, fz0 + 18, 'raft:research_terminal[facing=north]', 'raft:research_terminal',
                   {'LogId': 'solace_2'})
    # the break: the fallen part's end near the stump is shattered
    bite(s, 8, fy0 + 5, fz0, 5, 6, 3, rnd)
    bite(s, 4, fy0 + 9, fz1 - 6, 3, 3, 4, rnd)
    for _ in range(5):
        rubble_pile(s, rnd.randint(1, W - 2), FY + 1, rnd.randint(13, 17), 2, rnd)
    # beams between stump and fallen part
    for x in (4, 8, 12):
        s.line((x, FY + 8, 14), (x, fy0 + 3, fz0 - 1), 'raft:steel_beam[axis=z]')
    tk.break_windows(s, ((0, 0, 0), (W, 40, D)), rnd, missing=0.2, cracked=0.2)
    scatter_growth(s, ((0, FY + 1, 0), (W - 1, fy0 + h + 1, D - 1)), rnd, kelp=0.03, grass=0.1, pickles=0.02,
                   kelp_h=(5, 14), lichen=0.01)
    s.grow_barnacles(((0, FY, 0), (W - 1, fy0 + h + 2, D - 1)), 0.2, 4301, support=lambda n: 'concrete' in n)
    front_jig(s, 8)
    return s.done()


def build_all():
    return {'deco_tower_base': deco_base(), 'deco_tower_top': deco_top(), 'glass_tower_base': glass_base(),
            'glass_tower_top': glass_top(), 'fallen_tower': fallen_tower()}
