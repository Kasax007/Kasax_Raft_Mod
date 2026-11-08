"""beach_shack - abandoned beach bars of happier days on Palm Isle and Jungle Isle beaches.
 0 tiki_bar: deck on palm stilts, open bar counter under a thatched gable roof with overhang, string lights,
   paper lanterns, stools, tables; on the sand umbrellas with deck chairs, tiki torches along the path, hammock
 1 surf_shack: two-storey rental hut with a sun deck on top, ladder, board rack, kiosk window, umbrellas and towels
Template y=0 is the ground (start height -1 under WORLD_SURFACE_WG).
"""
import random

from .lib import Side, contact, pick, save, write_structure

G = 0
PLANK = [('raft:weathered_planks', 5), ('raft:palm_planks', 3), ('raft:driftwood_planks', 2)]
LANTERNS = ['raft:paper_lantern_red', 'raft:paper_lantern_orange', 'raft:paper_lantern_yellow',
            'raft:paper_lantern_pink', 'raft:paper_lantern_cyan', 'raft:paper_lantern_lime']


def tiki(s, x, z, lit=True, y=G + 1):
    s.set(x, y, z, f'raft:tiki_torch[half=lower,lit={str(lit).lower()}]')
    s.set(x, y + 1, z, f'raft:tiki_torch[half=upper,lit={str(lit).lower()}]')


def umbrella(s, rnd, x, z, color, chairs=2):
    s.set(x, G + 1, z, f'raft:beach_umbrella_{color}[half=lower]')
    s.set(x, G + 2, z, f'raft:beach_umbrella_{color}[half=upper]')
    for i, (dx, f) in enumerate(((-1, 'south'), (1, 'south'))):
        if i < chairs:
            s.set(x + dx, G + 1, z + 1, f'raft:deck_chair[facing={f}]')
    s.set(x, G + 1, z + 2, f'minecraft:{rnd.choice(["red", "light_blue", "yellow", "white"])}_carpet')


def gable_roof(s, x0, x1, z_front, z_back, y_eave, mat_st, mat_full, gable='raft:palm_planks'):
    """Gable roof with the ridge along x; eaves at z_front-1 and z_back+1 (overhang)."""
    zf, zb = z_front - 1, z_back + 1
    mid = (zf + zb) / 2
    for z in range(zf, zb + 1):
        if z < mid:
            y = y_eave + (z - zf)
            face = 'south'
        elif z > mid:
            y = y_eave + (zb - z)
            face = 'north'
        else:
            y = y_eave + (z - zf)
            face = None
        for x in range(x0 - 1, x1 + 2):
            s.set(x, y, z, f'{mat_st}[facing={face},half=bottom]' if face else mat_full)
        # gable triangles
        for x in (x0, x1):
            for yy in range(y_eave, y):
                if zf < z < zb:
                    s.set(x, yy, z, gable)


def tiki_bar(rnd):
    s = Side('tiki_bar')
    X1, Z1 = 12, 9
    # stilts and deck
    for x in range(0, X1 + 1):
        for z in range(0, Z1 + 1):
            s.set(x, G + 1, z, pick(rnd, PLANK))
    for x in (0, 6, X1):
        for z in (0, Z1):
            s.set(x, G, z, 'raft:palm_log[axis=y]')
            s.set(x, G + 1, z, 'raft:palm_log[axis=y]')
    for x in range(0, X1 + 1):                      # railing, gap for the steps
        if x not in (5, 6, 7):
            s.set(x, G + 2, 0, 'raft:palm_fence')
    for z in range(0, Z1 + 1):
        s.set(0, G + 2, z, 'raft:palm_fence')
        s.set(X1, G + 2, z, 'raft:palm_fence')
    for x in (5, 6, 7):
        s.set(x, G + 1, -1, 'raft:palm_stairs[facing=south,half=bottom]')
        s.set(x, G + 2, 0, 'minecraft:air')
    # bar building (back half): walls, counter, header beam
    bx0, bx1, bz0, bz1 = 2, 10, 5, 9
    for x in range(bx0, bx1 + 1):
        for y in range(G + 2, G + 5):
            s.set(x, y, bz1, pick(rnd, PLANK))
    for z in range(bz0, bz1 + 1):
        for y in range(G + 2, G + 5):
            for x in (bx0, bx1):
                s.set(x, y, z, pick(rnd, PLANK))
    for x in (bx0, bx1):
        for y in range(G + 2, G + 5):
            s.set(x, y, bz0, 'raft:palm_log[axis=y]')
            s.set(x, y, bz1, 'raft:palm_log[axis=y]')
    s.set(bx0, G + 3, 7, 'raft:green_sea_glass_pane')
    s.set(bx1, G + 3, 7, 'raft:aqua_sea_glass_pane')
    for x in range(bx0 + 1, bx1):
        s.set(x, G + 2, bz0, 'raft:palm_planks' if x != 6 else 'raft:palm_trapdoor[facing=south,half=top,open=false]')
        s.set(x, G + 3, bz0, 'raft:palm_slab[type=bottom]' if x != 6 else 'minecraft:air')
        s.set(x, G + 4, bz0, 'raft:stripped_palm_log[axis=x]')
        for z in range(bz0 + 1, bz1):
            for y in (G + 2, G + 3):
                s.set(x, y, z, 'minecraft:air')
    s.set(6, G + 2, bz0, 'minecraft:air')
    for x in range(bx0 + 1, bx1):
        s.set(x, G + 4, bz0 - 1, 'raft:string_lights_colorful[facing=south]')
    # behind the counter: shelves, bottles, grill, chest
    for x in (3, 4, 8, 9):
        s.set(x, G + 3, bz1 - 1, 'raft:driftwood_shelf[facing=north]')
    s.set(3, G + 2, bz1 - 1, 'minecraft:barrel[facing=north,open=false]')
    s.set(4, G + 2, bz1 - 1, 'raft:rum_bottles[bottles=3,facing=north]')
    s.set(5, G + 2, bz1 - 1, 'raft:palm_table')
    s.set(5, G + 3, bz1 - 1, 'raft:rum_bottles[bottles=2,facing=north]')
    s.set(7, G + 2, bz1 - 1, 'raft:simple_grill[lit=false]')
    s.chest(8, G + 2, bz1 - 1, 'raft:chests/beach_shack', facing='north', block='raft:wooden_crate')
    s.set(9, G + 2, bz1 - 1, 'raft:water_purifier[facing=north,lit=false]')
    s.set(6, G + 4, 7, 'raft:paper_lantern_orange[hanging=true]')
    s.set(4, G + 3, bz0, 'raft:rum_bottles[bottles=1,facing=south]')
    s.set(8, G + 3, bz0, 'raft:seashell_lamp')
    # stools in front of the counter, tables on the front deck
    for x in (3, 5, 7, 9):
        s.set(x, G + 2, bz0 - 1, 'raft:palm_chair[facing=south]' if x != 7 else 'raft:cushion_red')
    for (x, z, c) in ((2, 2, 'raft:paper_lantern_red'), (10, 2, 'raft:paper_lantern_cyan')):
        s.set(x, G + 2, z, 'raft:palm_table')
        s.set(x, G + 3, z, f'{c}[hanging=false]')
        s.set(x - 1, G + 2, z, 'raft:palm_chair[facing=east]')
        s.set(x + 1, G + 2, z, 'raft:palm_chair[facing=west]')
    s.set(4, G + 2, 1, 'raft:potted_hibiscus')
    s.set(8, G + 2, 1, 'raft:potted_palm_sapling')
    # roof
    gable_roof(s, bx0 - 1, bx1 + 1, bz0 - 1, bz1, G + 5, 'raft:palm_thatch_stairs', 'raft:palm_thatch')
    for x in (bx0 - 1, bx1 + 1):                   # front posts carrying the overhang
        for y in range(G + 2, G + 5):
            s.set(x, y, bz0 - 1, 'raft:palm_log[axis=y]')
    s.set(bx1 + 1, G + 4, bz0 - 2, 'raft:wind_chime')
    s.set(bx0 - 1, G + 4, bz0 - 2, 'raft:paper_lantern_red[hanging=true]')
    s.set(0, G + 3, 0, 'raft:paper_lantern_yellow[hanging=false]')
    s.set(X1, G + 3, 0, 'raft:paper_lantern_pink[hanging=false]')
    # beach in front: path, tiki torches, umbrellas, hammock
    for z in range(-2, -11, -1):
        s.set(6, G, z, pick(rnd, [('raft:palm_planks', 2), ('raft:weathered_planks', 1), ('minecraft:sand', 1)]))
        if z % 3 == 0:
            tiki(s, 4, z)
            tiki(s, 8, z, lit=z != -6)
    umbrella(s, rnd, 1, -6, 'red')
    umbrella(s, rnd, 11, -8, 'blue')
    umbrella(s, rnd, 13, -3, 'yellow', chairs=1)
    for x in (15, 18):
        for y in range(G + 1, G + 4):
            s.set(x, y, 4, 'raft:palm_log[axis=y]')
    s.set(16, G + 1, 4, 'raft:hammock[facing=east,occupied=false,part=foot]')
    s.set(17, G + 1, 4, 'raft:hammock[facing=east,occupied=false,part=head]')
    s.set(15, G + 4, 4, 'raft:palm_leaves[distance=1,persistent=true,waterlogged=false]')
    s.set(18, G + 4, 4, 'raft:palm_leaves[distance=1,persistent=true,waterlogged=false]')
    # sand castle
    for (x, z) in ((-3, -4), (-2, -4), (-3, -3), (-2, -3)):
        s.set(x, G + 1, z, 'minecraft:sandstone')
    s.set(-3, G + 2, -4, 'minecraft:sandstone_wall')
    s.set(-2, G + 2, -3, 'minecraft:sandstone_wall')
    s.set(-4, G + 1, -2, 'raft:seashells[shells=2]')
    return s


def surf_shack(rnd):
    s = Side('surf_shack')
    # 6x6 hut, two storeys: kiosk below, sun deck on top
    for x in range(0, 7):
        for z in range(0, 7):
            s.set(x, G + 1, z, pick(rnd, PLANK))
            wall = x in (0, 6) or z in (0, 6)
            for y in range(G + 2, G + 5):
                if wall:
                    corner = x in (0, 6) and z in (0, 6)
                    s.set(x, y, z, 'raft:palm_log[axis=y]' if corner else (
                        'minecraft:light_blue_terracotta' if (x + z + y) % 5 else 'minecraft:white_terracotta'))
                else:
                    s.set(x, y, z, 'minecraft:air')
            s.set(x, G + 5, z, pick(rnd, PLANK))
    for x in range(1, 6):                           # kiosk window with a hatch awning
        s.set(x, G + 3, 0, 'minecraft:air')
        s.set(x, G + 2, 0, 'raft:palm_slab[type=top]')
        s.set(x, G + 4, -1, 'raft:palm_trapdoor[facing=north,half=top,open=false]')
    s.set(6, G + 2, 3, 'raft:palm_door[facing=east,half=lower,hinge=left,open=false,powered=false]')
    s.set(6, G + 3, 3, 'raft:palm_door[facing=east,half=upper,hinge=left,open=false,powered=false]')
    s.set(0, G + 3, 3, 'raft:aqua_sea_glass_pane')
    s.chest(1, G + 2, 5, 'raft:chests/beach_shack', facing='east')
    s.set(5, G + 2, 5, 'minecraft:barrel[facing=up,open=false]')
    s.set(3, G + 2, 5, 'raft:driftwood_table')
    s.set(3, G + 3, 5, 'raft:seashell_lamp')
    s.set(3, G + 4, 3, 'raft:paper_lantern_white[hanging=true]')
    s.set(5, G + 3, 1, 'raft:life_ring[facing=north]')
    # sun deck: railing, umbrella, chairs, bunting
    for x in range(0, 7):
        for z in range(0, 7):
            if x in (0, 6) or z in (0, 6):
                s.set(x, G + 6, z, 'raft:palm_fence')
    s.set(3, G + 6, 3, 'raft:beach_umbrella_red[half=lower]')
    s.set(3, G + 7, 3, 'raft:beach_umbrella_red[half=upper]')
    s.set(2, G + 6, 4, 'raft:deck_chair[facing=north]')
    s.set(4, G + 6, 4, 'raft:deck_chair[facing=north]')
    s.set(5, G + 6, 1, 'raft:spyglass_stand[facing=north]')
    for y in range(G + 2, G + 7):
        s.set(7, y, 1, 'raft:rope_ladder[facing=east]')
    s.set(6, G + 6, 1, 'minecraft:air')
    for x in range(1, 6):
        s.set(x, G + 4, -1, 'raft:bunting_red_white[facing=north]') if x % 2 else None
    # board rack (upright "boards" of coloured wool) and canoe on the sand
    for i, c in enumerate(('red', 'yellow', 'cyan')):
        s.set(-2, G + 1, 1 + i * 2, f'minecraft:{c}_wool')
        s.set(-2, G + 2, 1 + i * 2, f'minecraft:{c}_wool')
        s.set(-2, G + 3, 1 + i * 2, f'minecraft:{c}_carpet')
    for z in range(1, 6):
        s.set(-3, G + 1, z, 'raft:palm_fence' if z in (1, 5) else 'minecraft:air')
    for z in range(9, 14):
        s.set(2, G + 1, z, 'raft:palm_slab[type=bottom]' if z in (9, 13) else 'raft:palm_stairs[facing=east,half=bottom]')
        s.set(4, G + 1, z, 'raft:palm_slab[type=bottom]' if z in (9, 13) else 'raft:palm_stairs[facing=west,half=bottom]')
    s.set(3, G + 1, 9, 'raft:palm_slab[type=bottom]')
    s.set(3, G + 1, 13, 'raft:palm_slab[type=bottom]')
    umbrella(s, rnd, 9, -4, 'yellow')
    umbrella(s, rnd, -3, -6, 'blue', chairs=1)
    for (x, z) in ((8, 2), (8, 6), (-1, -2)):
        tiki(s, x, z)
    s.set(9, G + 1, 4, 'raft:driftwood_lamp')
    return s


BUILDERS = [tiki_bar, surf_shack]


def main(preview=False):
    out = []
    for v, fn in enumerate(BUILDERS):
        rnd = random.Random(31415911 + v)
        s = fn(rnd)
        path = f'beach_shack/{fn.__name__}'
        save(s, path, anchored=True)
        out.append((path, s))
        if preview:
            print('  preview', contact(s, f'beach_shack_{fn.__name__}', scale=7))
    write_structure('beach_shack', [(p, 1) for p, _ in out], None,
                    step='surface_structures', terrain='beard_thin', start_height=-1, heightmap='WORLD_SURFACE_WG',
                    biomes=['palm_isle', 'jungle_isle'], spacing=24, separation=10, salt=31415911, max_distance=32)
    return out
