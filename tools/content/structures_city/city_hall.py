"""Port Solace City Hall: the tallest tower in the centre of the city (two stacked pieces).

city_hall_base (35 x 48 x 35, explicit waterlogging): grand portico with columns and a pediment, a three-storey
podium (lobby with reception, council chamber, archives), the main tower (21 x 21, quartz piers) with offices, the
mayor's office (story/port_solace_hall, terminal solace_3, radio) on the 10th floor and the watertight air vault
(story/port_solace_vault) on the 11th floor, reached by diving up the flooded stairwell and through an airlock of two
bulkhead doors.
city_hall_top (apply waterlogging): radio room (terminal solace_4), clock storey with four clock faces, copper dome,
lantern cupola and flag pole. Its top reaches far above the sea surface: the landmark of the drowned city.
"""
import math
import random

from .citylib import (CONCRETE, FLOOR, FOUND, FY, GY, H4, RUBBLE, City, bite, lamp_post, rubble_pile,
                      scatter_growth, slab, stair)
from .common import DIRS, OPP, leak_check, pick
from . import towerkit as tk

QUARTZ = [('minecraft:quartz_block', 45), ('minecraft:smooth_quartz', 20), ('minecraft:calcite', 15),
          ('minecraft:diorite', 8), ('minecraft:polished_diorite', 12)]
PIER = [('minecraft:quartz_pillar[axis=y]', 75), ('minecraft:calcite', 15), ('minecraft:polished_diorite', 10)]
SPANDREL = [('minecraft:polished_andesite', 45), ('minecraft:light_gray_terracotta', 25), ('minecraft:andesite', 15),
            ('raft:cracked_concrete', 15)]
HALL_FLOOR = [('minecraft:polished_diorite', 40), ('minecraft:polished_andesite', 30), ('minecraft:smooth_stone', 20),
              ('raft:cracked_concrete', 10)]
VAULT = [('raft:riveted_hull', 60), ('minecraft:polished_deepslate', 25), ('minecraft:iron_block', 15)]
COPPER = [('minecraft:oxidized_copper', 55), ('minecraft:weathered_copper', 25), ('minecraft:oxidized_cut_copper', 20)]

TX0, TZ0, TX1, TZ1 = 7, 8, 27, 28          # tower ring
SX0, SZ0, SX1, SZ1 = 9, 22, 12, 25          # stair shaft (inside the tower)
JX, JZ = 17, 18                             # vertical jigsaw (tower centre)
FH = 4
VAULT_BOX = (14, 10, 26, 20)                # x0, z0, x1, z1 (walls)


def base():
    rnd = random.Random(3101)
    s = City('port_solace/city_hall_base', 3101, mode='explicit')
    W, D = 35, 35
    s.pfill(0, 0, 0, W - 1, GY, D - 1, FOUND, rnd)
    # ---- forecourt and steps
    for x in range(W):
        for z in range(0, 4):
            s.set(x, FY, z, 'minecraft:smooth_stone_slab[type=bottom]' if z == 0 else 'minecraft:polished_andesite')
            for y in range(FY + 1, FY + 6):
                s.set(x, y, z, 'minecraft:water')
    for x in (3, 31):
        s.set(x, FY + 1, 1, 'minecraft:chiseled_quartz_block')
        s.set(x, FY + 2, 1, 'minecraft:quartz_slab[type=bottom]')
        lamp_post(s, x, 2, arm='north', y=FY + 1, h=4, kind='sea')
    # ---- podium (3 floors, 35 wide, z 4..32)
    pod = tk.Style(corner=PIER, pier=PIER, band=SPANDREL, wall=QUARTZ, glass='minecraft:light_blue_stained_glass_pane',
                   bay=3, floor=HALL_FLOOR, sill='minecraft:quartz_stairs', cornice='minecraft:quartz_stairs')
    PX0, PZ0, PX1, PZ1 = 0, 4, W - 1, 32

    def pod_cell(side, i, n, k, r):
        if side == 'north' and k == 0 and 13 <= i <= 21:
            return 'minecraft:water'          # main entrance opening
        if r == 1 and side in ('east', 'west', 'south'):
            return 'sill'
        return None

    tk.shell(s, PX0, PZ0, PX1, PZ1, FY, 3, FH, pod, rnd, cell=pod_cell)
    tk.slabs(s, PX0, PZ0, PX1, PZ1, FY, 3, FH, pod, rnd)
    roof_y = FY + 3 * FH
    tk.parapet(s, PX0, PZ0, PX1, PZ1, roof_y + 1, 'minecraft:quartz_slab[type=bottom]', rnd, gaps=0.15)
    # ---- portico columns + pediment in front of the entrance
    for x in range(9, 26, 3):
        for y in range(FY + 1, roof_y + 1):
            s.set(x, y, 2, 'minecraft:quartz_pillar[axis=y]' if rnd.random() < 0.9 else 'minecraft:calcite')
        s.set(x, FY + 1, 2, 'minecraft:chiseled_quartz_block')
    for x in range(8, 27):
        for z in range(1, 4):
            s.set(x, roof_y + 1, z, 'minecraft:smooth_quartz')
        s.set(x, roof_y + 1, 0, stair('minecraft:quartz_stairs', 'south', 'top'))
    # triangular pediment
    for i in range(0, 10):
        for x in range(8 + i, 27 - i):
            y = roof_y + 2 + i
            if y > roof_y + 8:
                break
            edge = x in (8 + i, 26 - i)
            s.set(x, y, 2, stair('minecraft:quartz_stairs', 'east' if x == 8 + i else 'west') if edge
                  else ('minecraft:chiseled_quartz_block' if (x == 17 and i == 2) else 'minecraft:smooth_quartz'))
    # pediment "clock" medallion
    s.set(17, roof_y + 4, 1, 'minecraft:sea_lantern')
    # ---- main tower above the podium
    tow = tk.Style(corner=PIER, pier=PIER, band=SPANDREL, wall=QUARTZ, glass='minecraft:light_blue_stained_glass_pane',
                   bay=2, floor=HALL_FLOOR, pier_out=True, sill=None, cornice='minecraft:quartz_stairs')
    floors_up = (47 - roof_y) // FH          # 8
    top = tk.shell(s, TX0, TZ0, TX1, TZ1, roof_y, floors_up, FH, tow, rnd)
    assert top == 47, top
    tk.slabs(s, TX0, TZ0, TX1, TZ1, roof_y, floors_up, FH, tow, rnd, roof=True)
    # podium interior below the tower: keep the tower footprint open inside the podium (lobby)
    # ---- stair core (flooded) from the lobby to the roof, door openings to the north every floor
    tk.spiral_stairs(s, SX0, SZ0, SX1, SZ1, FY + 1, 47, 'minecraft:polished_andesite_stairs',
                     newel=[('minecraft:polished_andesite', 70), ('minecraft:andesite', 30)], rnd=rnd,
                     walls=[('minecraft:smooth_stone', 60), ('minecraft:polished_andesite', 40)], doors='north',
                     fh=FH, y0=FY)
    for x in range(SX0, SX1 + 1):          # open shaft through the roof (continues in the top piece)
        for z in range(SZ0, SZ1 + 1):
            if not (SX0 < x < SX1 and SZ0 < z < SZ1):
                b = s.blocks.get((x, 47, z))
                if b and 'stairs' not in b[0]:
                    s.set(x, 47, z, 'minecraft:water')
    for x in range(SX0 - 1, SX1 + 2):      # remove the shaft wall on the roof layer (top piece continues)
        pass
    # ---- elevator shaft with a wrecked cab
    tk.elevator(s, 22, 23, 23, 24, FY + 1, 46, rnd, walls=[('minecraft:smooth_stone', 70), ('raft:rusted_plate', 30)],
                cab_y=FY + 9)
    for k in range(0, 11):
        y = FY + k * FH
        tk.door_opening(s, 22, y + 1, 22, h=3, w=2, along='x')
    # ---- lobby: reception desk, columns, chandelier fallen
    for x in range(12, 23):
        s.set(x, FY + 1, 14, 'minecraft:dark_oak_planks' if x not in (12, 22) else 'minecraft:stripped_dark_oak_log[axis=y]')
        s.set(x, FY + 2, 14, slab('minecraft:polished_diorite_slab', 'bottom'))
    s.set(17, FY + 1, 15, stair('minecraft:dark_oak_stairs', 'south'))
    s.block_entity(15, FY + 3, 14, 'raft:research_terminal[facing=north]', 'raft:research_terminal',
                   {'LogId': 'solace_1'})
    for (x, z) in ((10, 9), (24, 9), (10, 18), (24, 18), (5, 12), (29, 12), (5, 24), (29, 24)):
        for y in range(FY + 1, FY + FH * 2):
            if s.free(x, y, z):
                s.set(x, y, z, 'minecraft:quartz_pillar[axis=y]')
    # fallen chandelier
    for (dx, dz) in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
        s.set(17 + dx, FY + 1, 20 + dz, 'minecraft:lantern[hanging=false]' if (dx + dz) % 2 else 'minecraft:iron_chain[axis=x]')
    for y in range(FY + 5, FY + 8):
        s.set(17, y, 20, 'minecraft:iron_chain[axis=y]')
    # checkered lobby floor
    for x in range(1, W - 1):
        for z in range(5, PZ1):
            if s.is_name(x, FY, z, 'diorite', 'andesite', 'smooth_stone', 'concrete'):
                s.set(x, FY, z, 'minecraft:polished_diorite' if (x + z) % 2 else 'minecraft:polished_andesite')
    # ---- council chamber (west wing, 1st floor): tiered benches facing a dais
    cy = FY + FH + 1
    for row in range(3):
        for z in range(10, 27):
            if z in (17, 18):
                continue
            s.set(2 + row, cy + row, z, stair('minecraft:spruce_stairs', 'west'))
            for yy in range(cy, cy + row):
                s.set(2 + row, yy, z, 'minecraft:spruce_planks')
    for z in range(15, 21):
        s.set(6, cy, z, 'minecraft:dark_oak_planks')
    s.set(6, cy + 1, 17, slab('minecraft:dark_oak_slab', 'bottom'))
    # archives (east wing, 1st and 2nd floor): bookshelves and barrels
    for y0 in (FY + FH, FY + 2 * FH):
        for z in range(10, 30, 3):
            for x in range(29, 33):
                if rnd.random() < 0.8:
                    s.set(x, y0 + 1, z, 'minecraft:bookshelf' if rnd.random() < 0.7 else 'minecraft:chiseled_bookshelf[facing=south]')
                    s.set(x, y0 + 2, z, 'minecraft:bookshelf' if rnd.random() < 0.5 else 'minecraft:water')
        s.chest(32, y0 + 1, 30, 'raft:chests/port_solace_office', facing='north', block='minecraft:barrel')
    # ---- tower floors: offices (2..9), mayor (10), vault (11)
    for k in range(3, 10):
        y = FY + k * FH + 1
        tk.office_floor(s, TX0 + 1, TZ0 + 1, TX1 - 1, TZ1 - 1, y, rnd,
                        loot='raft:chests/port_solace_office' if k in (4, 7) else None,
                        avoid=lambda x, z: (SX0 - 1 <= x <= SX1 + 1 and SZ0 - 2 <= z <= SZ1 + 1)
                        or (20 <= x <= 24 and 21 <= z <= 25))
        tk.ceiling_lights(s, TX0, TZ0, TX1, TZ1, y + FH - 1, rnd, step=6, chance=0.5)
    mayor_office(s, rnd)
    vault(s, rnd)
    # ---- decay
    tk.break_windows(s, ((0, 0, 0), (W - 1, 47, D - 1)), rnd, missing=0.3, cracked=0.3)
    tk.floor_holes(s, TX0 + 1, TZ0 + 1, TX1 - 1, TZ1 - 1, [FY + 4 * FH, FY + 6 * FH], rnd, n=1, rmax=2.5)
    # the east podium corner collapsed
    bite(s, 33, 13, 31, 5, 6, 5, rnd)
    rubble_pile(s, 31, FY + 1, 29, 3, rnd)
    bite(s, 0, 10, 5, 3, 4, 3, rnd)
    # growth outside and in the lobby
    scatter_growth(s, ((0, FY + 1, 0), (W - 1, FY + 2, D - 1)), rnd, kelp=0.01, grass=0.06, pickles=0.01)
    scatter_growth(s, ((0, roof_y + 1, 0), (W - 1, roof_y + 2, D - 1)), rnd, kelp=0.03, grass=0.1, pickles=0.02,
                   kelp_h=(4, 12))
    s.grow_barnacles(((0, FY, 0), (W - 1, 40, D - 1)), 0.1, 3101,
                     support=lambda n: 'quartz' in n or 'diorite' in n or 'calcite' in n)
    scatter_growth(s, ((TX0 + 1, FY + 1, TZ0 + 1), (TX1 - 1, 42, TZ1 - 1)), rnd, kelp=0.0, grass=0.0,
                   pickles=0.0, lichen=0.02)
    # jigsaws: front (to the plaza) and the vertical socket for the top
    s.jig(17, GY, 0, 'north', 'hall_front', 'hall_anchor', 'minecraft:empty', 'minecraft:polished_andesite')
    s.jig(JX, 47, JZ, 'up', 'top_socket', 'hall_top', 'city_hall_top', 'minecraft:polished_diorite', prio=1)
    s.done()
    leaks = leak_check(s)
    assert not leaks, f'city hall vault leaks: {leaks[:5]}'
    return s


def mayor_office(s, rnd):
    """10th floor, front half of the tower: wood panelling, desk, radio, the hall chest and memo terminal."""
    y0 = FY + 9 * FH            # slab
    y = y0 + 1
    x0, z0, x1, z1 = TX0 + 1, TZ0 + 1, TX1 - 1, 17
    tk.partition(s, x0, z1 + 1, x1, z1 + 1, y, 3, [('minecraft:dark_oak_planks', 60),
                                                    ('minecraft:stripped_dark_oak_log[axis=y]', 25),
                                                    ('minecraft:spruce_planks', 15)], rnd, doors={(11, z1 + 1), (12, z1 + 1)})
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            s.set(x, y0, z, 'minecraft:dark_oak_planks' if (x + z) % 3 else 'minecraft:spruce_planks')
            for yy in range(y, y + 3):
                if s.blocks.get((x, yy, z), ('',))[0] != 'minecraft:water':
                    s.set(x, yy, z, 'minecraft:water')
    # desk facing the door (north side is the window front)
    for x in range(15, 20):
        s.set(x, y, 12, 'minecraft:dark_oak_planks' if x in (15, 19) else slab('minecraft:dark_oak_slab', 'top'))
    s.set(17, y, 11, stair('minecraft:dark_oak_stairs', 'north'))      # mayor's chair behind the desk
    s.set(16, y + 1, 12, 'raft:expedition_radio[facing=south]')
    s.set(18, y + 1, 12, 'minecraft:lantern[hanging=false]')
    s.chest(17, y, 9, 'raft:chests/story/port_solace_hall', facing='south', block='minecraft:chest')
    # side desk with the memo terminal
    s.set(24, y, 15, slab('minecraft:dark_oak_slab', 'top'))
    s.block_entity(24, y + 1, 15, 'raft:research_terminal[facing=west]', 'raft:research_terminal',
                   {'LogId': 'solace_3'})
    s.set(23, y, 15, stair('minecraft:dark_oak_stairs', 'east'))
    # bookshelves on the side walls, flag of the city (red/white panes)
    for z in range(9, 17):
        s.set(x0, y, z, 'minecraft:bookshelf')
        s.set(x0, y + 1, z, 'minecraft:bookshelf' if rnd.random() < 0.8 else 'minecraft:chiseled_bookshelf[facing=east]')
    for z in range(10, 15):
        s.set(x1, y + 1, z, 'minecraft:oak_shelf[facing=west]')
    s.set(x1, y, 16, 'minecraft:barrel[facing=up,open=false]')
    # couch + coffee table for visitors
    for x in range(14, 19):
        s.set(x, y, 16, stair('minecraft:resin_brick_stairs', 'south'))
    s.set(12, y, 15, 'minecraft:decorated_pot')
    for (x, z) in ((10, 10), (24, 10), (10, 16), (24, 12)):
        s.set(x, y + 2, z, 'minecraft:lantern[hanging=true]')


def vault(s, rnd):
    """11th floor: watertight vault with an airlock (two bulkhead doors) from the stair landing."""
    y0 = FY + 10 * FH          # 43 slab
    y = y0 + 1                 # 44..46 inside, ceiling 47 = roof slab
    vx0, vz0, vx1, vz1 = VAULT_BOX
    # outer walls
    for x in range(vx0, vx1 + 1):
        for z in range(vz0, vz1 + 1):
            s.pset(x, y0, z, VAULT, rnd)
            s.pset(x, 47, z, VAULT, rnd)
            for yy in range(y, 47):
                if x in (vx0, vx1) or z in (vz0, vz1):
                    s.pset(x, yy, z, VAULT, rnd)
    # airlock chamber at x = vx0+1, z 14..16, inner wall at x = vx0+2
    ax = vx0 + 1
    for z in range(vz0 + 1, vz1):
        for yy in range(y, 47):
            s.pset(vx0 + 2, yy, z, VAULT, rnd)
    for yy in range(y, 47):
        for z in (13, 17):
            s.pset(ax, yy, z, VAULT, rnd)
    for z in range(vz0 + 1, vz1):
        if 13 < z < 17:
            continue
        for yy in range(y, 47):
            s.pset(ax, yy, z, VAULT, rnd)
    s.air(ax, y, 14, ax, 46, 16)
    s.door(vx0, y, 15, 'raft:bulkhead_door', 'east', hinge='left')
    s.door(vx0 + 2, y, 15, 'raft:bulkhead_door', 'east', hinge='left')
    s.set(ax, 46, 15, 'minecraft:lantern[hanging=true]')
    s.dry.add((ax, 46, 15))
    # vault room
    ix0, iz0, ix1, iz1 = vx0 + 3, vz0 + 1, vx1 - 1, vz1 - 1
    s.air(ix0, y, iz0, ix1, 46, iz1)
    for x in range(ix0, ix1 + 1):
        for z in range(iz0, iz1 + 1):
            s.set(x, y0, z, 'minecraft:polished_deepslate' if (x + z) % 2 else 'minecraft:deepslate_tiles')
    for (x, z) in ((ix0 + 2, iz0 + 2), (ix1 - 2, iz0 + 2), (ix0 + 2, iz1 - 2), (ix1 - 2, iz1 - 2)):
        s.set(x, 47, z, 'minecraft:sea_lantern')
    # safe-deposit wall (east)
    for z in range(iz0, iz1 + 1):
        for yy in (y, y + 1, y + 2):
            s.set(ix1, yy, z, 'minecraft:iron_block' if (z + yy) % 2 else 'minecraft:chiseled_tuff_bricks')
    # treasure: sigil chest on a dais, gold piles, crates
    cx, cz = (ix0 + ix1) // 2 + 1, (iz0 + iz1) // 2
    s.set(cx, y, cz, 'raft:sigil_pedestal[waterlogged=false]')
    s.chest(cx + 1, y, cz, 'raft:chests/story/port_solace_vault', facing='west', block='raft:treasure_chest')
    s.set(cx + 1, y, cz, f'raft:treasure_chest[facing=west,open=false,waterlogged=false]',
          s.blocks[(cx + 1, y, cz)][2])
    for (x, z, n) in ((ix0 + 1, iz0, 5), (ix0 + 2, iz0, 3), (ix1 - 1, iz1, 6), (ix1 - 2, iz1, 2), (ix0, iz1, 4)):
        s.set(x, y, z, f'raft:gold_coin_pile[layers={n}]')
    s.set(ix0, y, iz0, 'minecraft:gold_block')
    s.set(ix1 - 1, y, iz0, 'raft:wooden_crate[facing=west,open=false]')
    s.set(ix1 - 1, y + 1, iz0, 'minecraft:lantern[hanging=false]')
    s.set(ix0, y, cz, 'raft:woven_rug')
    s.set(ix0 + 1, y, cz, 'raft:woven_rug')
    # portholes so a diver sees the glow from the landing
    s.set(vx0, y + 1, 12, 'raft:porthole')
    s.set(vx0, y + 1, 18, 'raft:porthole')
    # a terminal-less 'warning' plate next to the outer door
    s.set(vx0 - 1, y + 2, 14, 'minecraft:yellow_terracotta') if s.free(vx0 - 1, y + 2, 14) else None
    # landing: the corridor from the stair door to the airlock stays flooded
    for x in range(TX0 + 1, vx0):
        for z in range(TZ0 + 1, SZ0 - 1):
            for yy in range(y, 47):
                if s.free(x, yy, z) or s.is_name(x, yy, z, 'slab', 'stairs', 'barrel', 'bookshelf', 'pane', 'pot'):
                    s.set(x, yy, z, 'minecraft:water')
    for (x, z) in ((10, 12), (10, 18)):
        s.set(x, 46, z, 'minecraft:lantern[hanging=true]')


def top():
    """Crown of City Hall (apply waterlogging): radio room, clock storey, copper dome, cupola, flag pole."""
    rnd = random.Random(3102)
    s = City('port_solace/city_hall_top', 3102, mode='apply')
    Y = 48
    cx0, cz0, cx1, cz1 = 9, 10, 25, 26
    crown = tk.Style(corner=PIER, pier=PIER, band=SPANDREL, wall=QUARTZ,
                     glass='minecraft:light_blue_stained_glass_pane', bay=2, floor=HALL_FLOOR,
                     cornice='minecraft:quartz_stairs')
    # setback terrace ring on the tower roof (x 7..27): balustrade
    for x in range(TX0, TX1 + 1):
        for z in range(TZ0, TZ1 + 1):
            if x in (TX0, TX1) or z in (TZ0, TZ1):
                s.set(x, Y, z, 'minecraft:quartz_slab[type=bottom]' if (x + z) % 2 else 'minecraft:quartz_pillar[axis=y]')
    # radio room (12th floor), 4 high
    t1 = tk.shell(s, cx0, cz0, cx1, cz1, Y, 1, 4, crown, rnd)
    tk.slabs(s, cx0, cz0, cx1, cz1, Y, 1, 4, crown, rnd, skip=lambda x, y, z: (SX0 <= x <= SX1 and SZ0 <= z <= SZ1))
    # clock storey: solid walls with a clock face on every side (5 high)
    t2 = t1 + 6
    for x in range(cx0, cx1 + 1):
        for z in range(cz0, cz1 + 1):
            edge = x in (cx0, cx1) or z in (cz0, cz1)
            for y in range(t1 + 1, t2 + 1):
                if edge:
                    s.pset(x, y, z, QUARTZ, rnd)
                elif y < t2:
                    s.set(x, y, z, 'minecraft:water')
                    s.interior.add((x, y, z))
                else:
                    s.pset(x, y, z, HALL_FLOOR, rnd)
    mx, mz = (cx0 + cx1) // 2, (cz0 + cz1) // 2
    my = t1 + 3
    for side in ('north', 'south', 'east', 'west'):
        for a in range(-3, 4):
            for b in range(-3, 4):
                d = math.hypot(a, b)
                if d > 3.3:
                    continue
                st = 'minecraft:white_concrete' if d < 2.6 else 'minecraft:gold_block'
                if (a, b) in ((0, 0), (0, 1), (0, 2), (1, 0), (2, 0)):
                    st = 'minecraft:black_concrete'
                if side == 'north':
                    p = (mx + a, my + b, cz0)
                elif side == 'south':
                    p = (mx - a, my + b, cz1)
                elif side == 'east':
                    p = (cx1, my + b, mz + a)
                else:
                    p = (cx0, my + b, mz - a)
                s.set(*p, st)
        dx, _, dz = DIRS[side]
    for x in range(cx0, cx1 + 1):
        for z in range(cz0, cz1 + 1):
            if x in (cx0, cx1) or z in (cz0, cz1):
                dx = -1 if x == cx0 else 1 if x == cx1 else 0
                dz = -1 if z == cz0 else 1 if z == cz1 else 0
                s.set(x + dx, t2, z + dz, stair('minecraft:quartz_stairs', 'south' if dz < 0 else 'north' if dz > 0
                                                else 'east' if dx < 0 else 'west', 'top'))
    # corner urns
    for (x, z) in ((cx0, cz0), (cx1, cz0), (cx0, cz1), (cx1, cz1)):
        s.set(x, t2 + 1, z, 'minecraft:quartz_pillar[axis=y]')
        s.set(x, t2 + 2, z, 'minecraft:oxidized_lightning_rod[facing=up]')
    # drum + dome
    dy0 = t2 + 1
    for y in range(dy0, dy0 + 3):
        for x in range(mx - 6, mx + 7):
            for z in range(mz - 6, mz + 7):
                d = math.hypot(x - mx, z - mz)
                if 5.2 < d <= 6.3:
                    s.set(x, y, z, 'minecraft:quartz_pillar[axis=y]' if (x + z) % 2 else
                          ('minecraft:light_blue_stained_glass' if y == dy0 + 1 else 'minecraft:smooth_quartz'))
                elif d <= 5.2:
                    s.set(x, y, z, 'minecraft:water')
                    s.interior.add((x, y, z))
    domey = dy0 + 3
    for x in range(mx - 7, mx + 8):
        for z in range(mz - 7, mz + 8):
            for y in range(domey, domey + 8):
                d = math.sqrt((x - mx) ** 2 + (z - mz) ** 2 + ((y - domey) * 1.15) ** 2)
                if 5.0 < d <= 6.4:
                    s.pset(x, y, z, COPPER, rnd)
                elif d <= 5.0:
                    s.set(x, y, z, 'minecraft:water')
                    s.interior.add((x, y, z))
    # ribs
    for (dx, dz) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        for y in range(domey, domey + 7):
            r = math.sqrt(max(0.0, 6.4 ** 2 - ((y - domey) * 1.15) ** 2))
            px, pz = mx + round(dx * r), mz + round(dz * r)
            s.set(px, y, pz, 'minecraft:waxed_copper_block' if y % 2 else 'minecraft:exposed_cut_copper')
    # cupola + spire
    cy = domey + 7
    for x in range(mx - 1, mx + 2):
        for z in range(mz - 1, mz + 2):
            s.set(x, cy, z, 'minecraft:oxidized_cut_copper')
            corner = abs(x - mx) == 1 and abs(z - mz) == 1
            s.set(x, cy + 1, z, 'minecraft:quartz_pillar[axis=y]' if corner else 'minecraft:light_blue_stained_glass_pane')
            s.set(x, cy + 2, z, 'minecraft:quartz_pillar[axis=y]' if corner else 'minecraft:light_blue_stained_glass_pane')
            s.set(x, cy + 3, z, 'minecraft:oxidized_cut_copper_slab[type=bottom]' if corner else 'minecraft:oxidized_cut_copper')
    s.set(mx, cy + 1, mz, 'minecraft:sea_lantern')
    s.set(mx, cy + 2, mz, 'minecraft:glowstone')
    for y in range(cy + 4, cy + 10):
        s.set(mx, y, mz, 'minecraft:iron_bars' if y < cy + 9 else 'minecraft:lightning_rod[facing=up]')
    for y in range(cy + 6, cy + 9):
        s.set(mx + 1, y, mz, 'minecraft:red_stained_glass_pane')
        s.set(mx + 2, y, mz, 'minecraft:white_stained_glass_pane' if y != cy + 7 else 'minecraft:red_stained_glass_pane')
    # stairs continue from the base shaft up to the clock storey
    tk.spiral_stairs(s, SX0, SZ0, SX1, SZ1, Y, t1 + 4, 'minecraft:polished_andesite_stairs',
                     newel=[('minecraft:polished_andesite', 100)], rnd=rnd)
    # radio room: emergency terminal, radio, desks
    ry = Y + 1
    s.set(20, ry, 13, slab('minecraft:dark_oak_slab', 'top'))
    s.set(21, ry, 13, slab('minecraft:dark_oak_slab', 'top'))
    s.block_entity(20, ry + 1, 13, 'raft:research_terminal[facing=south]', 'raft:research_terminal',
                   {'LogId': 'solace_4'})
    s.set(21, ry + 1, 13, 'raft:expedition_radio[facing=south]')
    s.set(20, ry, 12, stair('minecraft:dark_oak_stairs', 'north'))
    for z in range(17, 24):
        s.set(24, ry, z, 'raft:server_rack[facing=west]')
    s.set(15, ry + 2, 15, 'minecraft:lantern[hanging=true]')
    s.set(19, ry + 2, 20, 'minecraft:lantern[hanging=true]')
    s.set(16, ry, 21, 'raft:control_panel[facing=north]')
    # clock machinery: gears from copper, chains
    for y in range(t1 + 1, t2):
        s.set(mx, y, mz, 'minecraft:iron_chain[axis=y]')
    s.set(mx - 1, t1 + 2, mz, 'minecraft:waxed_copper_grate')
    s.set(mx + 1, t1 + 2, mz, 'minecraft:waxed_copper_grate')
    tk.break_windows(s, ((0, Y, 0), (40, Y + 40, 40)), rnd, missing=0.35, cracked=0.35)
    bite(s, cx1, t1 + 2, cz0, 2.5, 2.5, 2.5, rnd)
    s.grow_barnacles(((0, Y, 0), (40, Y + 4, 40)), 0.1, 3102, support=lambda n: 'quartz' in n)
    s.jig(JX, Y, JZ, 'down', 'hall_top', 'top_socket', 'minecraft:empty', 'minecraft:polished_diorite')
    return s.done()


def build_all():
    return {'city_hall_base': base(), 'city_hall_top': top()}
