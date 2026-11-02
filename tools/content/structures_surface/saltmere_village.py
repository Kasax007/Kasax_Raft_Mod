"""Saltmere – abandoned stilt fishing village of Nana Kalea (story chapter 3).

Jigsaw village over shallow water. Every piece uses the same vertical frame: template y=0 is absolute y 72
(start_height 73, no heightmap projection), decks are at local DY=29 (absolute 101), the sea surface is local 27
(absolute 99). Stilts run from the decks down to local 0, so they always reach the sea floor; inside the ground they
are invisible.

Connectors: a parent exit is  name raft:saltmere_out -> target raft:saltmere_in (pool = next pool);
every child has exactly one entry  name raft:saltmere_in  on its north edge (z=0) at deck level.
"""
import math

from .lib import B, Pal, stairs, slab, log, trapdoor, rnd, look, validate, DIRS, OPP, CW, CCW, MODULE

DY = 29          # deck block layer (absolute 101)
WATER = 27       # top water layer (absolute 99)
P = 'saltmere_village'

DECK = Pal(('raft:weathered_planks', 40), ('raft:palm_planks', 30), ('raft:driftwood_planks', 15),
           ('minecraft:spruce_planks', 15), seed='sv_deck')
STILT = Pal(('raft:palm_log[axis=y]', 50), ('raft:driftwood_log[axis=y]', 30), ('minecraft:stripped_spruce_log[axis=y]', 20),
            seed='sv_stilt')
WALL = Pal(('raft:palm_planks', 46), ('minecraft:bamboo_planks', 24), ('minecraft:bamboo_mosaic', 10),
           ('raft:weathered_planks', 20), seed='sv_wall')
WALL2 = Pal(('minecraft:bamboo_planks', 40), ('minecraft:stripped_bamboo_block[axis=y]', 25), ('raft:palm_planks', 25),
            ('raft:weathered_planks', 10), seed='sv_wall2')
WALL3 = Pal(('raft:driftwood_planks', 40), ('raft:weathered_planks', 35), ('raft:palm_planks', 25), seed='sv_wall3')
FLOOR = Pal(('raft:palm_planks', 60), ('minecraft:bamboo_planks', 25), ('raft:weathered_planks', 15), seed='sv_floor')
THATCH = 'raft:palm_thatch'
THATCH_ST = 'raft:palm_thatch_stairs'
THATCH_SL = 'raft:palm_thatch_slab[type=bottom,waterlogged=false]'
POST = 'raft:palm_log[axis=y]'
FENCE = 'raft:palm_fence'
ROPE = 'raft:rope_railing'
LANTERN_COLORS = ('white', 'red', 'orange', 'yellow', 'lime', 'cyan', 'blue', 'purple', 'pink')


def plantern(color, hanging=True):
    return f'raft:paper_lantern_{color}[hanging={"true" if hanging else "false"},waterlogged=false]'


def new(name):
    return B(f'{P}/{name}', seed='sv_' + name)


# ------------------------------------------------------------------------------------------------ building kit
def stilt(s, x, z, top=DY - 1, barn=True):
    for y in range(0, top + 1):
        s.put(x, y, z, STILT)
    if barn:
        r = rnd(s.seed, 'barn', x, z)
        for y in range(WATER - 3, WATER + 1):
            for f, (dx, dz) in DIRS.items():
                c = (x + dx, y, z + dz)
                if c not in s.blocks and r.random() < 0.28:
                    s.set(*c, f'raft:barnacles[{OPP[f]}=true,waterlogged=false]')


def stilts_rect(s, x1, z1, x2, z2, step=4, inner=False):
    xs = sorted(set(list(range(x1, x2 + 1, step)) + [x2]))
    zs = sorted(set(list(range(z1, z2 + 1, step)) + [z2]))
    for x in xs:
        for z in zs:
            if inner or x in (x1, x2) or z in (z1, z2):
                stilt(s, x, z)
    # joists along the stilt rows just above the water
    for z in (z1, z2):
        for x in range(x1, x2 + 1):
            if (x, DY - 1, z) not in s.blocks:
                s.set(x, DY - 1, z, 'minecraft:stripped_spruce_log[axis=x]')
    for x in (x1, x2):
        for z in range(z1, z2 + 1):
            if (x, DY - 1, z) not in s.blocks:
                s.set(x, DY - 1, z, 'minecraft:stripped_spruce_log[axis=z]')


def deck(s, x1, z1, x2, z2, y=DY, pal=DECK, holes=0.0, seed='d'):
    r = rnd(s.seed, 'holes', seed)
    for x in range(x1, x2 + 1):
        for z in range(z1, z2 + 1):
            if holes and r.random() < holes and x not in (x1, x2) and z not in (z1, z2):
                continue
            s.put(x, y, z, pal)


def rail_line(s, cells, y=DY + 1, post_every=4, lamp_every=0, lamp=None):
    for i, (x, z) in enumerate(cells):
        if post_every and i % post_every == 0:
            s.set(x, y, z, FENCE)
            if lamp_every and i % lamp_every == 0:
                s.set(x, y + 1, z, FENCE)
                s.set(x, y + 2, z, lamp or 'minecraft:lantern[hanging=false,waterlogged=false]')
        else:
            s.set(x, y, z, ROPE)


def entry(s, x, z=0, y=DY):
    s.jigsaw(x, y, z, 'north_up', 'raft:saltmere_in', 'raft:saltmere_out', 'minecraft:empty',
             final_state='raft:weathered_planks')


def exit_(s, x, z, facing, pool, prio=0, y=DY):
    s.jigsaw(x, y, z, f'{facing}_up', 'raft:saltmere_out', 'raft:saltmere_in', f'raft:{P}/{pool}',
             final_state='raft:weathered_planks', selection_priority=prio)


def hut(s, x1, z1, x2, z2, fy, wh, walls=WALL, door=None, windows=(), roof='hip', axis='x', overhang=1,
        steep=True, door_block=None, shutters='minecraft:bamboo_trapdoor', glass=False, plate=True):
    """Hut shell: floor, posts, walls, windows, thatch roof; interior is air."""
    deck(s, x1, z1, x2, z2, fy, FLOOR, seed=f'hutfloor{x1}{z1}')
    for y in range(fy + 1, fy + wh + 1):
        for x in range(x1, x2 + 1):
            for z in range(z1, z2 + 1):
                if x in (x1, x2) or z in (z1, z2):
                    if x in (x1, x2) and z in (z1, z2):
                        s.set(x, y, z, POST)
                    else:
                        s.put(x, y, z, walls)
                else:
                    s.set(x, y, z, 'minecraft:air')
    top = fy + wh + 1
    if plate:
        for x in range(x1, x2 + 1):
            s.set(x, top, z1, 'raft:stripped_palm_log[axis=x]'); s.set(x, top, z2, 'raft:stripped_palm_log[axis=x]')
        for z in range(z1 + 1, z2):
            s.set(x1, top, z, 'raft:stripped_palm_log[axis=z]'); s.set(x2, top, z, 'raft:stripped_palm_log[axis=z]')
        for x in range(x1 + 1, x2):
            for z in range(z1 + 1, z2):
                s.set(x, top, z, 'minecraft:air')
    # windows
    for (wx, wz, face, w) in windows:
        along = (1, 0) if face in ('north', 'south') else (0, 1)
        dx, dz = DIRS[face]
        for i in range(w):
            x, z = wx + along[0] * i, wz + along[1] * i
            for y in (fy + 2, fy + 3):
                s.set(x, y, z, 'minecraft:glass_pane' if glass else 'minecraft:air')
            ox, oz = x + dx, z + dz
            if (ox, fy + 1, oz) not in s.blocks or s.is_air(ox, fy + 1, oz):
                s.set(ox, fy + 1, oz, trapdoor('minecraft:bamboo_trapdoor', face, 'top'))   # sill / flower box
        # shutter awning above
        for i in range(w):
            x, z = wx + along[0] * i + dx, wz + along[1] * i + dz
            if s.name_at(x, fy + 4, z) is None:
                s.set(x, fy + 4, z, trapdoor(shutters, face, 'top', open_=False))
    if door:
        dx_, dz_, face = door
        s.set(dx_, fy + 1, dz_, 'minecraft:air'); s.set(dx_, fy + 2, dz_, 'minecraft:air')
        if door_block:
            s.door(dx_, fy + 1, dz_, door_block, face, hinge='left', open_=True)
    y0 = top + (0 if plate else 0)
    if roof == 'hip':
        s.hip_roof(x1, z1, x2, z2, y0, THATCH_ST, fill=THATCH, overhang=overhang, top=THATCH_SL, steep=steep)
    else:
        s.gable_roof(x1, z1, x2, z2, y0, axis, THATCH_ST, fill=THATCH, overhang=overhang, top=THATCH_SL,
                     steep=steep, gable=walls)
    return top


def roof_air(s, x1, z1, x2, z2, y1, y2):
    for y in range(y1, y2 + 1):
        for x in range(x1 + 1, x2):
            for z in range(z1 + 1, z2):
                if (x, y, z) not in s.blocks:
                    s.set(x, y, z, 'minecraft:air')


def moss_roof(s, chance=0.12, seed=1):
    r = rnd(s.seed, 'moss', seed)
    for pos, (name, props, nbt) in list(s.blocks.items()):
        if name == THATCH_ST and r.random() < chance:
            above = (pos[0], pos[1] + 1, pos[2])
            if above not in s.blocks:
                s.set(*above, 'minecraft:moss_carpet')


def bunting_beam(s, x1, x2, z, y, kind='raft:bunting_rainbow', lights=None, axis='x'):
    """Horizontal beam with bunting / string lights on both faces and lanterns below."""
    if axis == 'x':
        for x in range(x1, x2 + 1):
            s.set(x, y, z, 'raft:stripped_palm_log[axis=x]')
            s.set(x, y, z - 1, f'{kind}[facing=north,waterlogged=false]') if (x, y, z - 1) not in s.blocks else None
            if lights and (x, y, z + 1) not in s.blocks:
                s.set(x, y, z + 1, f'{lights}[facing=south,waterlogged=false]')
    else:
        for zz in range(x1, x2 + 1):
            s.set(z, y, zz, 'raft:stripped_palm_log[axis=z]')
            s.set(z - 1, y, zz, f'{kind}[facing=west,waterlogged=false]') if (z - 1, y, zz) not in s.blocks else None
            if lights and (z + 1, y, zz) not in s.blocks:
                s.set(z + 1, y, zz, f'{lights}[facing=east,waterlogged=false]')


def drying_rack(s, x, z, y=DY + 1, length=3, axis='x'):
    """Two fence posts with a rope line and dried fish (dried kelp blocks / trapdoors)."""
    dx, dz = (1, 0) if axis == 'x' else (0, 1)
    s.set(x, y, z, FENCE); s.set(x, y + 1, z, FENCE)
    ex, ez = x + dx * (length + 1), z + dz * (length + 1)
    s.set(ex, y, ez, FENCE); s.set(ex, y + 1, ez, FENCE)
    for i in range(1, length + 1):
        cx, cz = x + dx * i, z + dz * i
        s.set(cx, y + 1, cz, ROPE)
        s.set(cx, y, cz, 'minecraft:dried_kelp_block' if i % 2 else 'minecraft:air')


def market_stall(s, x1, z1, facing, cloth=('red', 'white'), wares=None, chest=False):
    """3 wide, 2 deep stall: counter, posts, striped canopy. facing = side the customer stands."""
    dxf, dzf = DIRS[facing]
    along = (1, 0) if facing in ('north', 'south') else (0, 1)
    back = (-dxf, -dzf)
    cells = [(x1 + along[0] * i, z1 + along[1] * i) for i in range(3)]
    for i, (x, z) in enumerate(cells):
        # counter in front row, storage in back row
        s.set(x, DY + 1, z, 'minecraft:barrel[facing=up,open=false]' if i != 1 else stairs('minecraft:spruce_stairs', OPP[facing], 'top'))
        bx, bz = x + back[0], z + back[1]
        s.set(bx, DY + 1, bz, 'minecraft:air')
        if chest and i == 1:
            s.chest(bx, DY + 1, bz, 'raft:chests/saltmere_market', facing=facing, block='minecraft:barrel')
        elif i != 1:
            s.set(bx, DY + 1, bz, 'raft:wooden_crate[facing=%s,open=false]' % facing)
        for y in range(DY + 2, DY + 4):
            s.set(x, y, z, 'minecraft:air') if i == 1 else None
        # canopy (striped wool carpet look: wool blocks + carpets)
        for k, (cx_, cz_) in enumerate(((x, z), (bx, bz), (x + dxf, z + dzf))):
            col = cloth[(i + k) % 2]
            s.set(cx_, DY + 4, cz_, f'minecraft:{col}_wool' if k < 2 else f'minecraft:{col}_carpet')
    # posts
    for (x, z) in (cells[0], cells[2]):
        for y in (DY + 2, DY + 3):
            s.set(x, y, z, FENCE)
        bx, bz = x + back[0], z + back[1]
        for y in (DY + 2, DY + 3):
            s.set(bx, y, bz, FENCE)
    # wares on the counter
    w = wares or ['minecraft:melon', 'raft:seashells[shells=3,waterlogged=false]', 'minecraft:pumpkin']
    for i, (x, z) in enumerate(cells):
        if i != 1 and w:
            s.set(x, DY + 2, z, w[i % len(w)]) if 'seashells' in w[i % len(w)] or 'pickle' in w[i % len(w)] else None
    mid = cells[1]
    s.set(mid[0], DY + 2, mid[1], w[1] if len(w) > 1 else 'minecraft:air')
    s.set(cells[0][0], DY + 2, cells[0][1], plantern(cloth[0], False))


def tidewarden_statue(s, cx, fy, cz, face='north'):
    """Statue of a Tidewarden (6 high) facing `face`, built along the x axis (face north/south)."""
    sgn = -1 if face == 'north' else 1        # front direction in z
    TB = 'raft:tidestone_bricks'
    # pedestal
    for x in range(cx - 2, cx + 3):
        for z in range(cz - 1, cz + 2):
            s.set(x, fy + 1, z, 'raft:chiseled_tidestone' if abs(x - cx) < 2 else TB)
    for x in range(cx - 2, cx + 3):
        s.set(x, fy + 1, cz + 2 * sgn, stairs('raft:tidestone_brick_stairs', OPP[face]))
    s.set(cx, fy + 1, cz + 2 * sgn, 'minecraft:gold_block')
    # robe (wide at the hem)
    for x in range(cx - 1, cx + 2):
        s.set(x, fy + 2, cz, 'raft:tidestone')
        s.set(x, fy + 3, cz, 'raft:tidestone')
    s.set(cx - 1, fy + 2, cz + sgn, stairs('raft:tidestone_brick_stairs', OPP[face]))
    s.set(cx + 1, fy + 2, cz + sgn, stairs('raft:tidestone_brick_stairs', OPP[face]))
    s.set(cx, fy + 2, cz + sgn, slab('raft:tidestone_brick_slab', 'bottom'))
    # belt with gold inlay
    s.set(cx, fy + 4, cz, 'minecraft:gold_block')
    s.set(cx - 1, fy + 4, cz, 'raft:tidestone_pillar[axis=x]')
    s.set(cx + 1, fy + 4, cz, 'raft:tidestone_pillar[axis=x]')
    # chest/shoulders
    for x in range(cx - 1, cx + 2):
        s.set(x, fy + 5, cz, 'raft:chiseled_tidestone' if x == cx else 'raft:tidestone')
    s.set(cx - 2, fy + 5, cz, stairs('raft:tidestone_brick_stairs', 'east', 'top'))
    s.set(cx + 2, fy + 5, cz, stairs('raft:tidestone_brick_stairs', 'west', 'top'))
    s.set(cx, fy + 5, cz + sgn, 'raft:glowing_tidestone')   # glowing heart rune
    # arms: left holds a staff with a glowing orb, right lowered with an open hand
    s.set(cx - 2, fy + 4, cz, 'raft:tidestone_brick_wall')
    s.set(cx + 2, fy + 4, cz, 'raft:tidestone_brick_wall')
    s.set(cx + 2, fy + 3, cz + sgn, 'raft:tidestone_brick_wall')
    for y in range(fy + 2, fy + 8):
        s.set(cx + 2, y, cz + sgn, 'raft:tidestone_pillar[axis=y]') if y != fy + 3 else None
    s.set(cx + 2, fy + 8, cz + sgn, 'raft:glowing_tidestone')
    s.set(cx + 2, fy + 9, cz + sgn, 'minecraft:lightning_rod[facing=up,powered=false,waterlogged=false]')
    # head with crown
    s.set(cx, fy + 6, cz, 'raft:chiseled_tidestone')
    s.set(cx, fy + 6, cz + sgn, stairs('raft:tidestone_brick_stairs', face, 'top'))  # face / beard
    s.set(cx, fy + 7, cz, 'minecraft:gold_block')
    s.set(cx - 1, fy + 7, cz, 'minecraft:lightning_rod[facing=up,powered=false,waterlogged=false]')
    s.set(cx + 1, fy + 7, cz, 'minecraft:lightning_rod[facing=up,powered=false,waterlogged=false]')
    s.set(cx, fy + 8, cz, 'minecraft:lightning_rod[facing=up,powered=false,waterlogged=false]')
    # cape behind
    s.set(cx, fy + 3, cz - sgn, stairs('raft:tidestone_brick_stairs', face))
    s.set(cx - 1, fy + 2, cz - sgn, stairs('raft:tidestone_brick_stairs', face))
    s.set(cx + 1, fy + 2, cz - sgn, stairs('raft:tidestone_brick_stairs', face))
    s.set(cx, fy + 2, cz - sgn, 'raft:tidestone')
    s.set(cx, fy + 4, cz - sgn, stairs('raft:tidestone_brick_stairs', face))


# ------------------------------------------------------------------------------------------------ pieces
def plaza():
    s = new('plaza')
    W = 25
    corner = 3
    cells = []
    for x in range(W):
        for z in range(W):
            if min(x, W - 1 - x) + min(z, W - 1 - z) < corner:
                continue
            cells.append((x, z))
    cs = set(cells)
    # stilts: grid every 4 on the outline and a few inside
    for (x, z) in cells:
        edge = any((x + dx, z + dz) not in cs for dx, dz in DIRS.values())
        if (edge and (x + z) % 4 == 0) or (not edge and x % 6 == 0 and z % 6 == 0):
            stilt(s, x, z)
        if edge and (x, DY - 1, z) not in s.blocks:
            s.set(x, DY - 1, z, 'minecraft:stripped_spruce_log[axis=y]')
    # deck with a pattern: diagonal boards of mixed planks, a darker ring
    for (x, z) in cells:
        d = max(abs(x - 12), abs(z - 12))
        if d in (5,):
            s.set(x, DY, z, 'minecraft:stripped_spruce_wood[axis=y]')
        else:
            s.put(x, DY, z, DECK)
    # rope railing along the outline, leaving gaps at the connectors
    gaps = {(7, 0), (17, 0), (24, 7), (24, 17), (7, 24), (17, 24), (0, 7), (0, 17)}
    for (x, z) in cells:
        edge = any((x + dx, z + dz) not in cs for dx, dz in DIRS.values())
        if not edge:
            continue
        near_gap = any(abs(x - gx) + abs(z - gz) <= 1 for gx, gz in gaps)
        if near_gap:
            continue
        s.set(x, DY + 1, z, FENCE if (x + z) % 4 == 0 else ROPE)
    # well: 3x3 opening to the sea with winch
    for x in range(11, 14):
        for z in range(11, 14):
            s.set(x, DY, z, 'minecraft:air')
            s.set(x, DY - 1, z, 'minecraft:air')
    for x in range(10, 15):
        for z in range(10, 15):
            if x in (10, 14) or z in (10, 14):
                s.set(x, DY, z, 'raft:stripped_palm_log[axis=x]' if z in (10, 14) else 'raft:stripped_palm_log[axis=z]')
                s.set(x, DY + 1, z, slab('raft:palm_slab', 'bottom')) if (x + z) % 2 == 0 else s.set(x, DY + 1, z, 'minecraft:bamboo_trapdoor[facing=north,half=bottom,open=false,powered=false,waterlogged=false]')
    for x in (10, 14):
        for y in range(DY + 1, DY + 5):
            s.set(x, y, 12, POST)
    for x in range(10, 15):
        s.set(x, DY + 5, 12, 'raft:stripped_palm_log[axis=x]')
    s.set(13, DY + 4, 12, 'minecraft:air')
    for y in range(WATER, DY + 5):
        s.set(12, y, 12, 'minecraft:iron_chain[axis=y,waterlogged=false]')
    s.set(12, DY + 5, 12, 'minecraft:oak_log[axis=x]')
    s.set(15, DY + 1, 12, 'minecraft:cauldron')
    s.set(15, DY + 1, 13, 'minecraft:barrel[facing=up,open=false]')
    # bunting frame: four tall poles and beams across the plaza
    poles = [(4, 4), (20, 4), (4, 20), (20, 20)]
    for (x, z) in poles:
        for y in range(DY + 1, DY + 7):
            s.set(x, y, z, POST)
    bunting_beam(s, 5, 19, 4, DY + 6, 'raft:bunting_rainbow', 'raft:string_lights_colorful')
    bunting_beam(s, 5, 19, 20, DY + 6, 'raft:bunting_red_white', 'raft:string_lights_warm')
    bunting_beam(s, 5, 19, 4, DY + 6, 'raft:bunting_rainbow', None, axis='z')   # west beam (x=4)
    bunting_beam(s, 5, 19, 20, DY + 6, 'raft:bunting_red_white', None, axis='z')  # east beam (x=20)
    # paper lanterns hanging under the beams
    for i, x in enumerate(range(6, 19, 3)):
        s.set(x, DY + 5, 4, plantern(LANTERN_COLORS[i % 9]))
        s.set(x, DY + 5, 20, plantern(LANTERN_COLORS[(i + 4) % 9]))
    for i, z in enumerate(range(7, 18, 4)):
        s.set(4, DY + 5, z, plantern(LANTERN_COLORS[(i + 2) % 9]))
        s.set(20, DY + 5, z, plantern(LANTERN_COLORS[(i + 6) % 9]))
    # tiki torches at the entrances
    for (x, z) in ((6, 2), (8, 2), (16, 2), (18, 2), (22, 6), (22, 8), (22, 16), (22, 18), (6, 22), (8, 22),
                   (2, 16), (2, 18)):
        s.tall(x, DY + 1, z, 'raft:tiki_torch', 'lit=true')
    # market stalls
    market_stall(s, 6, 8, 'east', ('red', 'white'), ['minecraft:melon', 'minecraft:sea_pickle[pickles=3,waterlogged=false]', 'minecraft:pumpkin'], chest=True)
    market_stall(s, 16, 7, 'south', ('cyan', 'white'), ['minecraft:hay_block[axis=y]', 'raft:seashells[shells=4,waterlogged=false]', 'minecraft:dried_kelp_block'])
    market_stall(s, 18, 15, 'west', ('yellow', 'orange'), ['minecraft:melon', 'minecraft:sea_pickle[pickles=2,waterlogged=false]', 'minecraft:hay_block[axis=x]'])
    # communal table with cushions and a fire bowl
    for x in range(9, 12):
        s.set(x, DY + 1, 17, 'raft:palm_table[waterlogged=false]')
    for x in range(9, 12):
        s.set(x, DY + 1, 16, f'raft:cushion_{("red", "yellow", "blue")[x % 3]}')
        s.set(x, DY + 1, 18, f'raft:cushion_{("green", "white", "red")[x % 3]}')
    s.set(13, DY + 1, 18, 'raft:brazier[lit=false,waterlogged=false]')
    s.set(10, DY + 2, 17, 'raft:seashell_lamp[waterlogged=false]')
    # benches along the south rail
    for x in range(12, 16):
        s.set(x, DY + 1, 22, stairs('raft:palm_stairs', 'south'))
    # left-behind things: overturned crate, net, a fishing rod rack
    s.set(19, DY + 1, 10, 'raft:wooden_crate[facing=west,open=true]')
    s.set(20, DY + 1, 11, 'raft:rope_coil')
    s.set(5, DY + 1, 15, 'minecraft:barrel[facing=up,open=false]')
    s.set(5, DY + 2, 15, 'raft:jelly_lamp[hanging=false,waterlogged=false]')
    s.set(5, DY + 1, 16, 'minecraft:barrel[facing=north,open=false]')
    # deck damage: a few missing boards near the edge
    for (x, z) in ((2, 10), (3, 10), (22, 13), (14, 23)):
        s.set(x, DY, z, 'minecraft:air')
    # connectors
    exit_(s, 7, 0, 'north', 'elder', prio=10)
    exit_(s, 24, 7, 'east', 'shrine', prio=9)
    exit_(s, 7, 24, 'south', 'docks', prio=8)
    exit_(s, 17, 0, 'north', 'walkways')
    exit_(s, 24, 17, 'east', 'walkways')
    exit_(s, 17, 24, 'south', 'walkways')
    exit_(s, 0, 7, 'west', 'walkways')
    exit_(s, 0, 17, 'west', 'huts')
    return s


def walk_straight(name='walk_straight', L=9, lamp=True):
    s = new(name)
    deck(s, 0, 0, 2, L - 1, seed=name)
    for z in range(0, L, 4):
        stilt(s, 0, z); stilt(s, 2, z)
    stilt(s, 0, L - 1); stilt(s, 2, L - 1)
    for z in range(L):
        s.set(1, DY - 1, z, 'minecraft:stripped_spruce_log[axis=z]')
    rail_line(s, [(0, z) for z in range(1, L - 1)], post_every=4, lamp_every=8 if lamp else 0)
    rail_line(s, [(2, z) for z in range(1, L - 1)], post_every=4)
    if lamp:
        s.set(2, DY + 2, 4, plantern('orange', False)) if s.name_at(2, DY + 1, 4) == FENCE else None
    entry(s, 1)
    exit_(s, 1, L - 1, 'south', 'junctions')
    return s


def walk_bridge():
    """Rope bridge with sag, 3 wide (walk in the middle), 13 long."""
    s = new('walk_bridge')
    L = 13
    prof = {0: ('full', DY), 1: ('full', DY), 2: ('bottom', DY), 3: ('bottom', DY), 4: ('top', DY - 1),
            5: ('top', DY - 1), 6: ('top', DY - 1), 7: ('top', DY - 1), 8: ('top', DY - 1), 9: ('bottom', DY),
            10: ('bottom', DY), 11: ('full', DY), 12: ('full', DY)}
    for z, (kind, y) in prof.items():
        for x in range(3):
            if kind == 'full':
                s.put(x, y, z, DECK)
            else:
                st = 'minecraft:bamboo_slab' if (z + x) % 3 else 'raft:palm_slab'
                if x == 1 and z == 6:
                    continue  # missing board
                s.set(x, y, z, slab(st, kind))
        surf = y + (1 if kind != 'bottom' else 1)
        if 0 < z < L - 1:
            s.set(0, surf, z, ROPE); s.set(2, surf, z, ROPE)
    # anchor posts (tall) at both ends with lanterns
    for z in (0, L - 1):
        stilt(s, 0, z); stilt(s, 2, z)
        for x in (0, 2):
            for y in range(DY + 1, DY + 4):
                s.set(x, y, z, POST)
            s.set(x, DY + 4, z, 'raft:paper_lantern_yellow[hanging=false,waterlogged=false]')
    # middle support stilt pair (old) and hanging chains
    stilt(s, 0, 6, top=DY - 2); stilt(s, 2, 6, top=DY - 2)
    entry(s, 1)
    exit_(s, 1, L - 1, 'south', 'junctions')
    return s


def walk_short():
    s = new('walk_short')
    L = 5
    deck(s, 0, 0, 2, L - 1, seed='ws')
    stilt(s, 0, 0); stilt(s, 2, 0); stilt(s, 0, L - 1); stilt(s, 2, L - 1)
    rail_line(s, [(0, z) for z in range(1, L - 1)], post_every=3)
    rail_line(s, [(2, z) for z in range(1, L - 1)], post_every=3)
    entry(s, 1)
    exit_(s, 1, L - 1, 'south', 'huts')
    return s


def junction_t():
    s = new('junction_t')
    deck(s, 0, 0, 8, 8, seed='jt')
    stilts_rect(s, 0, 0, 8, 8, step=4)
    stilt(s, 4, 4, top=DY - 1)
    # railing except at openings
    edge = [(x, 0) for x in range(9)] + [(x, 8) for x in range(9)] + [(0, z) for z in range(1, 8)] + [(8, z) for z in range(1, 8)]
    for (x, z) in edge:
        if (x, z) in ((3, 0), (4, 0), (5, 0), (0, 3), (0, 4), (0, 5), (8, 3), (8, 4), (8, 5), (3, 8), (4, 8), (5, 8)):
            continue
        s.set(x, DY + 1, z, FENCE if (x in (0, 8) and z in (0, 8)) else ROPE)
    for (x, z) in ((0, 0), (8, 0), (0, 8), (8, 8)):
        s.set(x, DY + 2, z, FENCE)
        s.set(x, DY + 3, z, plantern(('red', 'cyan', 'yellow', 'pink')[(x + z) % 4], False))
    # centre lamp pole with string lights and hanging lanterns
    for y in range(DY + 1, DY + 5):
        s.set(4, y, 4, POST)
    for (dx, dz, f) in ((1, 0, 'east'), (-1, 0, 'west'), (0, 1, 'south'), (0, -1, 'north')):
        s.set(4 + dx, DY + 4, 4 + dz, f'raft:string_lights_warm[facing={f},waterlogged=false]')
    s.set(4, DY + 5, 4, 'raft:ship_lantern[hanging=false,waterlogged=false]')
    # props: drying rack, barrel, net
    s.set(1, DY + 1, 1, 'minecraft:barrel[facing=up,open=false]')
    s.set(1, DY + 2, 1, 'raft:rope_coil')
    s.set(7, DY + 1, 7, stairs('raft:palm_stairs', 'south'))
    s.set(6, DY + 1, 7, stairs('raft:palm_stairs', 'south'))
    s.set(7, DY + 1, 1, 'raft:wooden_crate[facing=west,open=false]')
    entry(s, 4)
    exit_(s, 0, 4, 'west', 'huts')
    exit_(s, 8, 4, 'east', 'huts')
    exit_(s, 4, 8, 'south', 'outer')
    return s


def junction_l():
    s = new('junction_l')
    deck(s, 0, 0, 6, 6, seed='jl')
    stilts_rect(s, 0, 0, 6, 6, step=3)
    edge = [(x, 0) for x in range(7)] + [(x, 6) for x in range(7)] + [(0, z) for z in range(1, 6)] + [(6, z) for z in range(1, 6)]
    for (x, z) in edge:
        if (x, z) in ((2, 0), (3, 0), (4, 0), (6, 2), (6, 3), (6, 4), (2, 6), (3, 6), (4, 6)):
            continue
        s.set(x, DY + 1, z, FENCE if (x in (0, 6) and z in (0, 6)) else ROPE)
    s.set(0, DY + 2, 6, FENCE); s.set(0, DY + 3, 6, 'raft:ship_lantern[hanging=false,waterlogged=false]')
    s.set(0, DY + 2, 0, FENCE); s.set(0, DY + 3, 0, plantern('lime', False))
    # small fish drying rack along the west side
    s.set(1, DY + 1, 2, FENCE); s.set(1, DY + 2, 2, FENCE)
    s.set(1, DY + 1, 4, FENCE); s.set(1, DY + 2, 4, FENCE)
    s.set(1, DY + 2, 3, ROPE)
    s.set(1, DY + 1, 3, 'minecraft:dried_kelp_block')
    s.set(5, DY + 1, 5, 'raft:deck_chair[facing=south]')
    entry(s, 3)
    exit_(s, 6, 3, 'east', 'huts')
    exit_(s, 3, 6, 'south', 'huts')
    return s


def end_cap():
    s = new('end_cap')
    deck(s, 0, 0, 2, 1, seed='ec')
    stilt(s, 0, 1); stilt(s, 2, 1)
    s.set(0, DY + 1, 1, FENCE); s.set(2, DY + 1, 1, FENCE); s.set(1, DY + 1, 1, ROPE)
    s.set(0, DY + 2, 1, FENCE); s.set(0, DY + 3, 1, plantern('white', False))
    s.set(2, DY + 1, 0, 'raft:rope_coil')
    entry(s, 1)
    return s


def fishing_platform():
    s = new('fishing_platform')
    deck(s, 0, 0, 6, 6, seed='fp')
    stilts_rect(s, 0, 0, 6, 6, step=3)
    for (x, z) in [(x, 6) for x in range(7)] + [(0, z) for z in range(1, 6)] + [(6, z) for z in range(1, 6)]:
        s.set(x, DY + 1, z, FENCE if (x + z) % 3 == 0 else ROPE)
    # small sun roof on 4 posts
    for (x, z) in ((1, 3), (5, 3), (1, 6), (5, 6)):
        for y in range(DY + 1, DY + 4):
            s.set(x, y, z, FENCE)
    for x in range(0, 7):
        for z in range(2, 7):
            s.set(x, DY + 4, z, THATCH_SL)
    s.set(2, DY + 1, 5, 'raft:deck_chair[facing=south]')
    s.set(4, DY + 1, 5, 'raft:deck_chair[facing=south]')
    s.set(3, DY + 1, 5, 'minecraft:barrel[facing=up,open=false]')
    s.set(3, DY + 2, 5, 'raft:rum_bottles[bottles=2,facing=south,waterlogged=false]')
    s.set(5, DY + 1, 1, 'minecraft:cauldron')
    s.chest(1, DY + 1, 1, 'raft:chests/saltmere_house', facing='east', block='minecraft:barrel')
    s.set(3, DY + 3, 4, plantern('orange'))
    entry(s, 3)
    return s


# -- huts --------------------------------------------------------------------------------------------
def porch(s, W, depth=3, fy=DY, steps=False):
    """Porch / gangway in front of a hut (z 0..depth-1) with railing; returns centre x."""
    cx = W // 2
    deck(s, cx - 1, 0, cx + 1, depth - 1, seed='porch')
    stilt(s, cx - 1, 0); stilt(s, cx + 1, 0)
    for z in range(1, depth):
        s.set(cx - 1, DY + 1, z, ROPE); s.set(cx + 1, DY + 1, z, ROPE)
    s.set(cx - 1, DY + 1, 0, FENCE); s.set(cx + 1, DY + 1, 0, FENCE)
    entry(s, cx)
    return cx


def hut_fisher():
    s = new('hut_fisher')
    W, D = 9, 13
    cx = porch(s, W, 3)
    # front deck (work area) z 3..5
    deck(s, 0, 3, W - 1, 5, seed='ff')
    stilts_rect(s, 0, 3, W - 1, D - 1, step=4)
    top = hut(s, 1, 6, W - 2, D - 1, DY, 4, WALL, door=(cx, 6, 'north'),
              windows=[(1, 8, 'west', 2), (W - 2, 8, 'east', 2), (3, D - 1, 'south', 1)], roof='hip')
    for (x, z) in ((0, 3), (W - 1, 3)):
        s.set(x, DY + 1, z, FENCE); s.set(x, DY + 2, z, FENCE); s.set(x, DY + 3, z, plantern('orange', False))
    for x in range(1, W - 1):
        if x not in (cx - 1, cx, cx + 1):
            s.set(x, DY + 1, 3, ROPE)
    for z in (4, 5):
        s.set(0, DY + 1, z, ROPE); s.set(W - 1, DY + 1, z, ROPE)
    # work area: fish barrels, nets on the wall, drying rack, rod rack
    s.set(1, DY + 1, 5, 'minecraft:barrel[facing=up,open=false]')
    s.set(2, DY + 1, 5, 'minecraft:barrel[facing=up,open=false]')
    s.set(W - 2, DY + 1, 4, 'minecraft:cauldron')
    for x in (1, 2, W - 3, W - 2):
        s.set(x, DY + 2, 5, 'raft:cargo_net[facing=north,waterlogged=false]') if s.name_at(x, DY + 2, 6) else None
    # inside: hammock, chest, stove, shelf, rods
    s.hammock(2, DY + 1, D - 3, 'east')
    s.chest(W - 3, DY + 1, D - 2, 'raft:chests/saltmere_house', facing='north')
    s.set(W - 3, DY + 1, 7, 'raft:simple_grill[lit=false]')
    s.set(2, DY + 1, 7, 'raft:driftwood_table[waterlogged=false]')
    s.set(2, DY + 2, 7, 'raft:seashell_lamp[waterlogged=false]')
    s.set(W - 3, DY + 3, D - 2, 'raft:driftwood_shelf[facing=north]')
    s.set(cx, DY + 4, 9, plantern('red'))
    s.set(cx, DY + 1, 10, 'raft:woven_rug')
    roof_air(s, 1, 6, W - 2, D - 1, top, top + 8)
    moss_roof(s)
    return s


def hut_netmaker():
    s = new('hut_netmaker')
    W, D = 11, 12
    cx = porch(s, W, 3)
    stilts_rect(s, 0, 3, W - 1, D - 1, step=5)
    deck(s, 0, 3, W - 1, D - 1, seed='nm')
    # open-sided workshop: posts with half walls, big nets hanging
    x1, z1, x2, z2 = 1, 4, W - 2, D - 2
    for (x, z) in ((x1, z1), (x2, z1), (x1, z2), (x2, z2), (cx, z2), (x1, 8), (x2, 8)):
        for y in range(DY + 1, DY + 5):
            s.set(x, y, z, POST)
    for x in range(x1, x2 + 1):
        for z in (z1, z2):
            if s.name_at(x, DY + 1, z) is None:
                s.set(x, DY + 1, z, slab('minecraft:bamboo_slab', 'top') if x != cx or z != z1 else 'minecraft:air')
    for z in range(z1, z2 + 1):
        for x in (x1, x2):
            if s.name_at(x, DY + 1, z) is None:
                s.set(x, DY + 1, z, slab('minecraft:bamboo_slab', 'top'))
    # back wall full with nets
    for x in range(x1 + 1, x2):
        for y in range(DY + 2, DY + 5):
            if s.name_at(x, y, z2) is None:
                s.put(x, y, z2, WALL2)
    for x in range(x1 + 1, x2):
        if x != cx:
            s.set(x, DY + 3, z2 - 1, 'raft:cargo_net[facing=north,waterlogged=false]')
            s.set(x, DY + 2, z2 - 1, 'raft:cargo_net[facing=north,waterlogged=false]')
    for x in range(x1, x2 + 1):
        s.set(x, DY + 5, z1, 'raft:stripped_palm_log[axis=x]'); s.set(x, DY + 5, z2, 'raft:stripped_palm_log[axis=x]')
    for z in range(z1, z2 + 1):
        s.set(x1, DY + 5, z, 'raft:stripped_palm_log[axis=z]'); s.set(x2, DY + 5, z, 'raft:stripped_palm_log[axis=z]')
    s.gable_roof(x1, z1, x2, z2, DY + 6, 'z', THATCH_ST, fill=THATCH, overhang=1, top=THATCH_SL, steep=True, gable=WALL2)
    roof_air(s, x1, z1, x2, z2, DY + 1, DY + 16)
    for x in range(x1 + 1, x2):
        for z in range(z1 + 1, z2):
            if s.name_at(x, DY + 5, z) is None:
                s.set(x, DY + 5, z, 'minecraft:air')
    # inside: loom, nets drying on a line, spools, chest
    s.set(x1 + 1, DY + 1, z1 + 2, 'minecraft:loom[facing=east]')
    s.set(x1 + 1, DY + 1, z1 + 3, 'raft:rope_coil')
    s.set(x1 + 1, DY + 2, z1 + 3, 'raft:rope_coil')
    s.set(x2 - 1, DY + 1, z1 + 2, 'raft:wooden_crate[facing=west,open=true]')
    s.chest(x2 - 1, DY + 1, z1 + 3, 'raft:chests/saltmere_house', facing='west')
    for z in range(z1 + 1, z2 - 1):
        s.set(cx, DY + 4, z, ROPE) if z != z1 + 3 else s.set(cx, DY + 4, z, FENCE)
    for z in range(z1 + 2, z2 - 1, 2):
        s.set(cx, DY + 3, z, 'minecraft:cobweb')          # hanging net mesh
    s.set(cx - 2, DY + 4, z1 + 3, plantern('cyan'))
    s.set(cx + 2, DY + 4, z1 + 3, plantern('yellow'))
    s.set(cx - 2, DY + 1, z2 - 2, 'raft:cushion_blue'); s.set(cx + 2, DY + 1, z2 - 2, 'raft:cushion_red')
    # nets outside on the railing posts
    for z in range(4, D - 1):
        s.set(0, DY + 1, z, ROPE); s.set(W - 1, DY + 1, z, ROPE)
    moss_roof(s, 0.08, 2)
    return s


def hut_cookhouse():
    s = new('hut_cookhouse')
    W, D = 11, 13
    cx = porch(s, W, 3)
    stilts_rect(s, 0, 3, W - 1, D - 1, step=5)
    deck(s, 0, 3, W - 1, D - 1, seed='ck')
    x1, z1, x2, z2 = 1, 4, W - 2, D - 2
    # stone hearth platform at the back (on the planks)
    for x in range(x1 + 1, x2):
        for z in range(z2 - 2, z2):
            s.set(x, DY, z, 'minecraft:mud_bricks' if (x + z) % 3 else 'minecraft:packed_mud')
    # walls: low walls, big openings (it's a kitchen hut)
    for y in range(DY + 1, DY + 5):
        for x in range(x1, x2 + 1):
            for z in range(z1, z2 + 1):
                edge = x in (x1, x2) or z in (z1, z2)
                if not edge:
                    s.set(x, y, z, 'minecraft:air'); continue
                if x in (x1, x2) and z in (z1, z2) or (x in (x1, x2) and z == 8):
                    s.set(x, y, z, POST)
                elif y == DY + 1 or z == z2:
                    s.put(x, y, z, WALL3)
                else:
                    s.set(x, y, z, 'minecraft:air')
    s.set(cx, DY + 1, z1, 'minecraft:air')
    # oven of bricks with smoker and chimney
    for x in range(cx - 1, cx + 2):
        s.set(x, DY + 1, z2 - 1, 'minecraft:bricks' if x != cx else 'minecraft:smoker[facing=north,lit=false]')
        s.set(x, DY + 2, z2 - 1, 'minecraft:bricks' if x != cx else 'minecraft:furnace[facing=north,lit=false]')
        s.set(x, DY + 3, z2 - 1, stairs('minecraft:brick_stairs', 'south'))
    for y in range(DY + 3, DY + 14):
        s.set(cx, y, z2 - 1, 'minecraft:bricks')
    s.set(cx, DY + 14, z2 - 1, 'minecraft:campfire[facing=north,lit=false,signal_fire=false,waterlogged=false]')
    s.set(cx - 2, DY + 1, z2 - 1, 'minecraft:water_cauldron[level=2]')
    s.set(cx + 2, DY + 1, z2 - 1, 'raft:simple_grill[lit=false]')
    s.set(cx - 3, DY + 1, z2 - 1, 'minecraft:barrel[facing=up,open=false]')
    s.set(cx + 3, DY + 1, z2 - 1, 'minecraft:barrel[facing=up,open=false]')
    s.chest(cx + 3, DY + 2, z2 - 1, 'raft:chests/saltmere_house', facing='north', block='minecraft:barrel')
    # long table with cushions
    for z in range(z1 + 2, z1 + 5):
        s.set(cx, DY + 1, z, 'raft:palm_table[waterlogged=false]')
        s.set(cx - 1, DY + 1, z, f'raft:cushion_{("yellow", "green", "red")[z % 3]}')
        s.set(cx + 1, DY + 1, z, f'raft:cushion_{("blue", "white", "yellow")[z % 3]}')
    s.set(cx, DY + 2, z1 + 3, 'minecraft:candle[candles=3,lit=false,waterlogged=false]')
    # drying fish racks outside on the side decks
    for x in range(x1, x2 + 1):
        s.set(x, DY + 5, z1, 'raft:stripped_palm_log[axis=x]'); s.set(x, DY + 5, z2, 'raft:stripped_palm_log[axis=x]')
    for z in range(z1, z2 + 1):
        s.set(x1, DY + 5, z, 'raft:stripped_palm_log[axis=z]'); s.set(x2, DY + 5, z, 'raft:stripped_palm_log[axis=z]')
    for x in range(x1 + 1, x2):
        for z in range(z1 + 1, z2):
            if s.name_at(x, DY + 5, z) is None:
                s.set(x, DY + 5, z, 'minecraft:air')
    s.gable_roof(x1, z1, x2, z2, DY + 6, 'z', THATCH_ST, fill=THATCH, overhang=1, top=THATCH_SL, steep=True, gable=WALL3)
    roof_air(s, x1, z1, x2, z2, DY + 6, DY + 16)
    for y in range(DY + 6, DY + 15):
        s.set(cx, y, z2 - 1, 'minecraft:bricks')
    s.set(cx - 2, DY + 4, z1 + 3, plantern('orange')); s.set(cx + 2, DY + 4, z1 + 3, plantern('red'))
    # hanging dried fish (kelp) under the eaves
    for x in (x1 + 1, x2 - 1):
        s.set(x, DY + 4, z1 + 1, 'minecraft:dried_kelp_block')
    # railings
    for z in range(4, D):
        s.set(0, DY + 1, z, ROPE); s.set(W - 1, DY + 1, z, ROPE)
    for x in range(0, W):
        if x not in (cx - 1, cx, cx + 1):
            s.set(x, DY + 1, 3, ROPE)
    moss_roof(s, 0.1, 3)
    return s


def hut_house(variant):
    s = new(f'hut_house_{variant}')
    if variant == 'a':
        W, D, walls, roof, axis, accent = 9, 11, WALL, 'hip', 'x', 'cyan'
    elif variant == 'b':
        W, D, walls, roof, axis, accent = 9, 12, WALL2, 'gable', 'z', 'pink'
    else:
        W, D, walls, roof, axis, accent = 11, 12, WALL3, 'hip', 'x', 'yellow'
    cx = porch(s, W, 3)
    deck(s, 0, 3, W - 1, 4, seed=f'h{variant}')
    stilts_rect(s, 0, 3, W - 1, D - 1, step=4)
    x1, z1, x2, z2 = 1, 5, W - 2, D - 1
    deck(s, 0, 5, W - 1, D - 1, seed=f'h2{variant}')
    fy = DY + (1 if variant == 'c' else 0)
    if fy > DY:
        s.set(cx, DY + 1, 4, stairs('raft:palm_stairs', 'south'))
        s.set(cx - 1, DY + 1, 4, slab('raft:palm_slab', 'bottom'))
        s.set(cx + 1, DY + 1, 4, slab('raft:palm_slab', 'bottom'))
    top = hut(s, x1, z1, x2, z2, fy, 4, walls, door=(cx, z1, 'north'),
              windows=[(x1, z1 + 2, 'west', 2), (x2, z1 + 2, 'east', 2), (cx - 1, z2, 'south', 3)],
              roof=roof, axis=axis, door_block='minecraft:bamboo_door' if variant != 'b' else None,
              glass=(variant == 'c'))
    roof_air(s, x1, z1, x2, z2, top, top + 10)
    # porch railing + lanterns
    for x in range(0, W):
        if x not in (cx - 1, cx, cx + 1):
            s.set(x, DY + 1, 3, ROPE)
    for (x, z) in ((0, 3), (W - 1, 3)):
        s.set(x, DY + 1, z, FENCE); s.set(x, DY + 2, z, FENCE); s.set(x, DY + 3, z, plantern(accent, False))
    for z in range(4, D):
        if s.name_at(0, DY + 1, z) is None:
            s.set(0, DY + 1, z, ROPE)
        if s.name_at(W - 1, DY + 1, z) is None:
            s.set(W - 1, DY + 1, z, ROPE)
    # string lights along the front wall plate
    for x in range(x1 + 1, x2):
        if x != cx and s.name_at(x, fy + 4, z1 - 1) is None:
            s.set(x, fy + 4, z1 - 1, 'raft:string_lights_warm[facing=north,waterlogged=false]')
    # interior
    ix1, iz1, ix2, iz2 = x1 + 1, z1 + 1, x2 - 1, z2 - 1
    if variant == 'a':
        s.bed(ix1, fy + 1, iz2 - 1, 'cyan', 'south')
        s.chest(ix2, fy + 1, iz2, 'raft:chests/saltmere_house', facing='west')
        s.set(ix2, fy + 1, iz1, 'raft:driftwood_table[waterlogged=false]')
        s.set(ix2 - 1, fy + 1, iz1, 'raft:driftwood_chair[facing=east,waterlogged=false]')
        s.set(ix2, fy + 2, iz1, 'raft:potted_hibiscus')
        s.set(cx, fy + 1, iz1 + 2, 'raft:woven_rug')
        s.set(ix1, fy + 3, iz1, 'raft:driftwood_shelf[facing=east]')
        s.set(cx, fy + 4, iz1 + 2, 'raft:hanging_planter')
        s.set(ix2, fy + 4, iz2 - 1, plantern('cyan'))
    elif variant == 'b':
        s.hammock(ix1, fy + 1, iz1 + 1, 'south')
        s.hammock(ix2, fy + 1, iz1 + 1, 'south')
        s.chest(cx, fy + 1, iz2, 'raft:chests/saltmere_house', facing='north')
        s.set(cx - 1, fy + 1, iz2, 'minecraft:barrel[facing=up,open=false]')
        s.set(cx + 1, fy + 1, iz2, 'raft:wooden_crate[facing=north,open=false]')
        s.set(cx, fy + 4, iz1 + 2, 'raft:wind_chime[waterlogged=false]')
        s.set(cx + 1, fy + 2, iz2, 'raft:seashell_lamp[waterlogged=false]')
        s.set(cx, fy + 1, iz1 + 2, 'raft:woven_rug')
    else:
        s.bed(ix1, fy + 1, iz1, 'yellow', 'south')
        s.bed(ix1 + 1, fy + 1, iz1, 'orange', 'south')
        s.chest(ix2, fy + 1, iz1, 'raft:chests/saltmere_house', facing='west')
        s.set(ix2, fy + 1, iz2, 'raft:palm_table[waterlogged=false]')
        s.set(ix2 - 1, fy + 1, iz2, 'raft:palm_chair[facing=east,waterlogged=false]')
        s.set(ix2, fy + 2, iz2, 'minecraft:candle[candles=2,lit=false,waterlogged=false]')
        s.set(ix2 - 2, fy + 1, iz2, 'raft:aquarium')
        s.set(cx, fy + 4, iz1 + 2, 'raft:hanging_planter')
        s.set(ix1, fy + 1, iz2, 'raft:driftwood_lamp[waterlogged=false]')
        s.set(cx, fy + 1, iz2, 'raft:woven_rug')
    # abandoned touches
    r = rnd(s.seed, 'webs')
    for _ in range(2):
        x, z = r.randint(ix1, ix2), r.randint(iz1, iz2)
        if s.is_air(x, fy + 4, z):
            s.set(x, fy + 4, z, 'minecraft:cobweb')
    moss_roof(s, 0.1, variant)
    return s


def hut_market():
    s = new('hut_market')
    W, D = 11, 11
    cx = porch(s, W, 2)
    deck(s, 0, 2, W - 1, D - 1, seed='mk')
    stilts_rect(s, 0, 2, W - 1, D - 1, step=5)
    # roofed market hall on 6 posts, stalls around
    x1, z1, x2, z2 = 0, 2, W - 1, D - 1
    for (x, z) in ((1, 3), (W - 2, 3), (1, D - 2), (W - 2, D - 2), (1, 6), (W - 2, 6)):
        for y in range(DY + 1, DY + 5):
            s.set(x, y, z, POST)
    for x in range(1, W - 1):
        s.set(x, DY + 5, 3, 'raft:stripped_palm_log[axis=x]'); s.set(x, DY + 5, D - 2, 'raft:stripped_palm_log[axis=x]')
    for z in range(3, D - 1):
        s.set(1, DY + 5, z, 'raft:stripped_palm_log[axis=z]'); s.set(W - 2, DY + 5, z, 'raft:stripped_palm_log[axis=z]')
    s.hip_roof(1, 3, W - 2, D - 2, DY + 6, THATCH_ST, fill=THATCH, overhang=1, top=THATCH_SL, steep=True)
    roof_air(s, 1, 3, W - 2, D - 2, DY + 1, DY + 16)
    for x in range(2, W - 2):
        for z in range(4, D - 2):
            if s.name_at(x, DY + 5, z) is None:
                s.set(x, DY + 5, z, 'minecraft:air')
    market_stall(s, 2, D - 3, 'north', ('lime', 'white'), ['minecraft:melon', 'raft:seashells[shells=2,waterlogged=false]', 'minecraft:pumpkin'], chest=True)
    market_stall(s, 2, 5, 'east', ('purple', 'white'), ['minecraft:hay_block[axis=y]', 'minecraft:sea_pickle[pickles=4,waterlogged=false]', 'minecraft:dried_kelp_block'])
    market_stall(s, W - 3, 5, 'west', ('blue', 'white'), ['minecraft:dried_kelp_block', 'raft:seashells[shells=4,waterlogged=false]', 'minecraft:melon'])
    for x in range(2, W - 2, 2):
        s.set(x, DY + 4, 3, plantern(LANTERN_COLORS[x % 9]))
    s.set(cx, DY + 4, 6, 'raft:ship_lantern[hanging=true,waterlogged=false]')
    s.set(cx, DY + 1, 7, 'raft:woven_rug')
    for z in range(2, D):
        s.set(0, DY + 1, z, ROPE) if s.name_at(0, DY + 1, z) is None else None
        s.set(W - 1, DY + 1, z, ROPE) if s.name_at(W - 1, DY + 1, z) is None else None
    for x in range(0, W):
        if s.name_at(x, DY + 1, D - 1) is None:
            s.set(x, DY + 1, D - 1, ROPE)
    return s


def elder():
    """Nana Kalea's house: a large raised fale with a veranda, reached over a gangway from the plaza."""
    s = new('elder')
    W, D = 15, 19
    cx = W // 2
    # gangway z 0..4
    deck(s, cx - 1, 0, cx + 1, 4, seed='eg')
    for z in (0, 4):
        stilt(s, cx - 1, z); stilt(s, cx + 1, z)
    for z in range(1, 5):
        s.set(cx - 1, DY + 1, z, ROPE); s.set(cx + 1, DY + 1, z, ROPE)
    entry(s, cx)
    # veranda platform z 5..D-1, x 0..W-1 at DY+1 (raised one step), house body inset
    fy = DY + 1
    for x in range(0, W):
        for z in range(5, D):
            s.put(x, fy, z, FLOOR)
            s.set(x, DY, z, 'minecraft:stripped_spruce_log[axis=x]') if (z in (5, D - 1)) else None
    stilts_rect(s, 0, 5, W - 1, D - 1, step=4, inner=False)
    for x in range(0, W, 4):
        for z in range(5, D, 4):
            stilt(s, x, z, top=fy - 1)
    # steps up
    for x in (cx - 1, cx, cx + 1):
        s.set(x, DY + 1, 5, stairs('raft:palm_stairs', 'south'))
    # veranda railing with lantern posts
    edge = [(x, 5) for x in range(W)] + [(x, D - 1) for x in range(W)] + [(0, z) for z in range(6, D - 1)] + [(W - 1, z) for z in range(6, D - 1)]
    for (x, z) in edge:
        if z == 5 and abs(x - cx) <= 1:
            continue
        corner = x in (0, W - 1) and z in (5, D - 1)
        s.set(x, fy + 1, z, FENCE if corner or (x + z) % 4 == 0 else ROPE)
        if corner:
            s.set(x, fy + 2, z, FENCE); s.set(x, fy + 3, z, 'raft:tiki_torch[half=lower,lit=true]'); s.set(x, fy + 4, z, 'raft:tiki_torch[half=upper,lit=true]')
    # house body
    x1, z1, x2, z2 = 2, 8, W - 3, D - 3
    top = hut(s, x1, z1, x2, z2, fy, 5, WALL, door=(cx, z1, 'north'),
              windows=[(x1, z1 + 2, 'west', 2), (x1, z1 + 6, 'west', 1), (x2, z1 + 2, 'east', 2), (x2, z1 + 6, 'east', 1),
                       (cx - 2, z2, 'south', 2), (cx + 1, z2, 'south', 2), (cx - 3, z1, 'north', 2), (cx + 2, z1, 'north', 2)],
              roof='hip', door_block='minecraft:jungle_door', glass=True, shutters='minecraft:jungle_trapdoor')
    roof_air(s, x1, z1, x2, z2, top, top + 14)
    # carved posts at the door (totem-ish) and a canopy
    for x in (cx - 1, cx + 1):
        for y in range(fy + 1, fy + 4):
            s.set(x, y, z1 - 1, 'minecraft:stripped_jungle_log[axis=y]')
        s.set(x, fy + 4, z1 - 1, plantern('red', False))
    for x in range(cx - 2, cx + 3):
        s.set(x, fy + 5, z1 - 1, THATCH_SL)
        s.set(x, fy + 5, z1 - 2, THATCH_SL)
    # interior
    ix1, iz1, ix2, iz2 = x1 + 1, z1 + 1, x2 - 1, z2 - 1
    s.bed(ix1, fy + 1, iz2 - 1, 'magenta', 'south')
    s.set(ix1 + 1, fy + 1, iz2, 'raft:driftwood_lamp[waterlogged=false]')
    # desk with the story chest
    s.set(ix2, fy + 1, iz2, 'raft:chart_table[facing=west]')
    s.set(ix2, fy + 1, iz2 - 1, 'raft:chart_table[facing=west]')
    s.chest(ix2, fy + 1, iz2 - 2, 'raft:chests/story/saltmere_elder', facing='west')
    s.set(ix2 - 1, fy + 1, iz2 - 1, 'raft:palm_chair[facing=east,waterlogged=false]')
    s.set(ix2, fy + 1, iz1, 'minecraft:bookshelf'); s.set(ix2, fy + 2, iz1, 'minecraft:bookshelf')
    s.set(ix2 - 1, fy + 1, iz1, 'minecraft:bookshelf'); s.set(ix2 - 1, fy + 2, iz1, 'raft:potted_tropical_fern')
    s.set(ix1, fy + 1, iz1, 'raft:aquarium'); s.set(ix1 + 1, fy + 1, iz1, 'raft:aquarium')
    s.set(ix1, fy + 2, iz1, 'raft:potted_hibiscus')
    s.set(ix1, fy + 3, iz1 + 3, 'raft:driftwood_shelf[facing=east]')
    s.set(ix1, fy + 1, iz1 + 3, 'raft:expedition_radio[facing=east,waterlogged=false]')
    for x in range(cx - 1, cx + 2):
        for z in range(iz1 + 2, iz1 + 5):
            s.set(x, fy + 1, z, 'raft:woven_rug')
    s.set(cx - 1, fy + 1, iz1 + 1, 'raft:cushion_red'); s.set(cx + 1, fy + 1, iz1 + 1, 'raft:cushion_yellow')
    s.set(cx, fy + 5, iz1 + 3, 'raft:ship_lantern[hanging=true,waterlogged=false]')
    s.set(ix1, fy + 5, iz2, 'raft:hanging_planter'); s.set(ix2, fy + 5, iz1 + 2, 'raft:hanging_planter')
    s.set(cx, fy + 4, iz2, 'raft:figurehead[facing=north,waterlogged=false]')
    # veranda comfort: hammock, deck chairs, wind chime
    s.set(W - 2, fy + 1, 6, 'raft:deck_chair[facing=north]')
    s.set(W - 3, fy + 1, 6, 'raft:deck_chair[facing=north]')
    s.set(1, fy + 1, 6, 'raft:palm_table[waterlogged=false]')
    s.set(1, fy + 2, 6, 'raft:potted_palm_sapling')
    s.set(1, fy + 1, D - 2, 'minecraft:barrel[facing=up,open=false]')
    s.set(W - 2, fy + 1, D - 2, 'raft:wooden_crate[facing=north,open=false]')
    s.set(x1 - 1, fy + 4, z1 - 1, 'raft:wind_chime[waterlogged=false]')
    s.set(x2 + 1, fy + 4, z1 - 1, 'raft:wind_chime[waterlogged=false]')
    # bunting along the eave
    for x in range(x1, x2 + 1):
        if s.name_at(x, fy + 5, z1 - 1) is None:
            s.set(x, fy + 5, z1 - 1, 'raft:bunting_rainbow[facing=north,waterlogged=false]')
    moss_roof(s, 0.06, 4)
    return s


def shrine():
    """Shrine of the Tidewarden: open pavilion with the statue; the Coral Sigil chest stands behind it."""
    s = new('shrine')
    W, D = 13, 17
    cx = W // 2
    # short rope bridge gangway z 0..3
    for z in range(0, 4):
        for x in range(cx - 1, cx + 2):
            s.set(x, DY, z, slab('minecraft:bamboo_slab', 'top') if z in (1, 2) else DECK.pick(x, DY, z))
        s.set(cx - 1, DY + 1, z, ROPE); s.set(cx + 1, DY + 1, z, ROPE)
    stilt(s, cx - 1, 0); stilt(s, cx + 1, 0); stilt(s, cx - 1, 3); stilt(s, cx + 1, 3)
    entry(s, cx)
    # platform of coral stone (heavy, old) on stilts
    fy = DY
    for x in range(0, W):
        for z in range(4, D):
            s.put(x, fy, z, Pal(('raft:coral_stone_bricks', 6), ('raft:mossy_coral_stone_bricks', 2),
                                ('raft:polished_coral_stone', 2), seed='sh_floor'))
    stilts_rect(s, 0, 4, W - 1, D - 1, step=4)
    for x in range(2, W - 2, 4):
        for z in range(8, D - 1, 4):
            stilt(s, x, z)
    # inner tidestone floor and carpet of offerings
    for x in range(3, W - 3):
        for z in range(7, D - 2):
            s.set(x, fy, z, 'raft:tidestone_bricks' if (x + z) % 2 else 'raft:tidestone')
    for z in range(5, 11):
        s.set(cx, fy, z, 'raft:polished_coral_stone')
    # pavilion: 6 coral-stone columns, tidestone beams, tall thatch roof
    x1, z1, x2, z2 = 2, 6, W - 3, D - 2
    cols = [(x1, z1), (x2, z1), (x1, z2), (x2, z2), (x1, (z1 + z2) // 2), (x2, (z1 + z2) // 2)]
    for (x, z) in cols:
        s.set(x, fy + 1, z, 'raft:chiseled_tidestone')
        for y in range(fy + 2, fy + 8):
            s.set(x, y, z, 'raft:tidestone_pillar[axis=y]')
        s.set(x, fy + 8, z, 'raft:chiseled_tidestone')
    for x in range(x1, x2 + 1):
        s.set(x, fy + 9, z1, 'raft:tidestone_bricks'); s.set(x, fy + 9, z2, 'raft:tidestone_bricks')
    for z in range(z1, z2 + 1):
        s.set(x1, fy + 9, z, 'raft:tidestone_bricks'); s.set(x2, fy + 9, z, 'raft:tidestone_bricks')
    for x in range(x1 + 1, x2):
        s.set(x, fy + 9, z1, 'raft:glowing_tidestone' if x == cx else 'raft:tidestone_bricks')
    s.hip_roof(x1, z1, x2, z2, fy + 10, THATCH_ST, fill=THATCH, overhang=1, top=THATCH_SL, steep=True)
    roof_air(s, x1, z1, x2, z2, fy + 1, fy + 22)
    for x in range(x1 + 1, x2):
        for z in range(z1 + 1, z2):
            if s.name_at(x, fy + 9, z) is None:
                s.set(x, fy + 9, z, 'minecraft:air')
    # back wall (behind the statue) of coral stone with a niche for the chest
    for x in range(x1 + 1, x2):
        for y in range(fy + 1, fy + 9):
            s.put(x, y, z2, Pal(('raft:coral_stone_bricks', 5), ('raft:mossy_coral_stone_bricks', 2), ('raft:tidestone_bricks', 2), seed='sh_back'))
    for y in range(fy + 2, fy + 8, 2):
        s.set(cx, y, z2, 'raft:glowing_tidestone')
    # statue facing the entrance (north), chest behind it in front of the back wall
    sz = z2 - 3
    tidewarden_statue(s, cx, fy, sz, 'north')
    s.chest(cx, fy + 1, z2 - 1, 'raft:chests/story/saltmere_shrine', facing='north')
    s.set(cx - 1, fy + 1, z2 - 1, 'minecraft:candle[candles=3,lit=true,waterlogged=false]')
    s.set(cx + 1, fy + 1, z2 - 1, 'minecraft:candle[candles=2,lit=true,waterlogged=false]')
    # offerings in front of the statue
    s.set(cx - 2, fy + 1, sz - 3, 'raft:giant_clam[facing=north,open=true,pearl=false,waterlogged=false]')
    s.set(cx + 2, fy + 1, sz - 3, 'raft:seashells[shells=4,waterlogged=false]')
    s.set(cx - 1, fy + 1, sz - 3, 'minecraft:sea_pickle[pickles=3,waterlogged=false]')
    s.set(cx + 1, fy + 1, sz - 3, 'minecraft:candle[candles=4,lit=true,waterlogged=false]')
    s.set(cx - 3, fy + 1, sz - 1, 'minecraft:decorated_pot[cracked=false,facing=north,waterlogged=false]')
    s.set(cx + 3, fy + 1, sz - 1, 'minecraft:decorated_pot[cracked=true,facing=north,waterlogged=false]')
    # coral planters along the sides (dead coral fans on stone)
    for z in range(z1 + 1, z2 - 1, 2):
        for x in (x1 + 1, x2 - 1):
            s.set(x, fy + 1, z, 'raft:coral_stone_brick_wall' if z % 4 == 1 else 'minecraft:brain_coral_block')
            s.set(x, fy + 2, z, 'raft:paper_lantern_cyan[hanging=false,waterlogged=false]' if z % 4 == 1 else 'minecraft:dead_tube_coral_fan[waterlogged=false]')
    # hanging cyan and white lanterns from the beams
    for x in range(x1 + 2, x2 - 1, 3):
        s.set(x, fy + 8, z1, plantern('cyan')) if s.name_at(x, fy + 8, z1) is None else None
        s.set(x, fy + 8, z1 + 1, plantern('white'))
    # entrance steps and two braziers
    for x in range(cx - 1, cx + 2):
        s.set(x, fy + 1, 4, 'minecraft:air')
    s.set(cx - 2, fy + 1, 5, 'raft:brazier[lit=true,waterlogged=false]')
    s.set(cx + 2, fy + 1, 5, 'raft:brazier[lit=true,waterlogged=false]')
    # low coral wall around the platform edge
    for x in range(0, W):
        for z in (4, D - 1):
            if not (z == 4 and abs(x - cx) <= 1):
                s.set(x, fy + 1, z, 'raft:coral_stone_brick_wall')
    for z in range(5, D - 1):
        s.set(0, fy + 1, z, 'raft:coral_stone_brick_wall'); s.set(W - 1, fy + 1, z, 'raft:coral_stone_brick_wall')
    # moss/vines: old place
    s.replace(lambda n: n == 'raft:coral_stone_bricks', 'raft:mossy_coral_stone_bricks', chance=0.15, seed=11)
    return s


def docks():
    """Boat dock with a moored sailing boat (decor) and buoys."""
    s = new('docks')
    W, D = 15, 21
    cx = 3
    # gangway from the plaza connector (x=cx) south
    deck(s, cx - 1, 0, cx + 1, 8, seed='dg')
    for z in (0, 4, 8):
        stilt(s, cx - 1, z); stilt(s, cx + 1, z)
    for z in range(1, 8):
        s.set(cx - 1, DY + 1, z, ROPE if z % 3 else FENCE); s.set(cx + 1, DY + 1, z, ROPE if z % 3 else FENCE)
    entry(s, cx)
    # main pier x 0..6, z 9..D-1
    deck(s, 0, 9, 6, D - 1, seed='dp')
    stilts_rect(s, 0, 9, 6, D - 1, step=4)
    # lower landing on the east side (just above the water) with a ladder
    for z in range(12, D - 1):
        for x in (7, 8):
            s.put(x, DY - 1, z, DECK)
        stilt(s, 8, z, top=DY - 2) if z % 4 == 0 else None
    # bollards with rope and lanterns
    for (x, z) in ((0, 9), (6, 9), (0, D - 1), (6, D - 1), (0, 15)):
        s.set(x, DY + 1, z, 'raft:stripped_palm_log[axis=y]')
        s.set(x, DY + 2, z, FENCE)
        s.set(x, DY + 3, z, 'minecraft:lantern[hanging=false,waterlogged=false]')
    for z in range(10, D - 1):
        if s.name_at(0, DY + 1, z) is None:
            s.set(0, DY + 1, z, ROPE)
    for x in range(1, 6):
        s.set(x, DY + 1, D - 1, ROPE)
    s.set(6, DY + 1, 10, ROPE); s.set(6, DY + 1, 11, ROPE)
    # cargo on the pier
    s.set(1, DY + 1, 10, 'raft:wooden_crate[facing=south,open=false]')
    s.set(2, DY + 1, 10, 'raft:wooden_crate[facing=south,open=false]')
    s.set(1, DY + 2, 10, 'raft:wooden_crate[facing=east,open=false]')
    s.set(1, DY + 1, 11, 'minecraft:barrel[facing=up,open=false]')
    s.chest(1, DY + 1, 12, 'raft:chests/saltmere_house', facing='east', block='minecraft:barrel')
    s.set(5, DY + 1, D - 2, 'raft:rope_coil')
    s.set(4, DY + 1, D - 2, 'raft:ship_anchor[facing=north,waterlogged=false]')
    s.set(3, DY + 1, 17, 'minecraft:cauldron')
    s.set(2, DY + 1, 17, stairs('raft:palm_stairs', 'east'))
    # moored boat east of the landing: hull x 10..13, z 10..19, bottom at WATER-1
    bx1, bx2, bz1, bz2 = 10, 13, 10, 19
    for z in range(bz1, bz2 + 1):
        taper = 1 if z in (bz1, bz2) else 0
        for x in range(bx1 + taper, bx2 + 1 - taper):
            s.set(x, WATER - 1, z, 'raft:palm_planks')                       # keel / bottom
            s.set(x, WATER, z, 'raft:palm_planks' if x in (bx1 + taper, bx2 - taper) else 'minecraft:spruce_planks')
        # gunwale
        for x in (bx1 + taper, bx2 - taper):
            s.set(x, WATER + 1, z, slab('raft:palm_slab', 'bottom'))
        for x in range(bx1 + taper + 1, bx2 - taper):
            s.set(x, WATER + 1, z, 'minecraft:air')
    for x in range(bx1 + 1, bx2):
        s.set(x, WATER + 1, bz1, stairs('raft:palm_stairs', 'south'))
        s.set(x, WATER + 1, bz2, stairs('raft:palm_stairs', 'north'))
    # mast and sail
    mx, mz = 11, 14
    for y in range(WATER + 1, WATER + 10):
        s.set(mx, y, mz, 'minecraft:stripped_spruce_log[axis=y]')
    for z in range(mz - 3, mz + 4):
        s.set(mx, WATER + 3, z, 'minecraft:stripped_spruce_log[axis=z]') if z != mz else None
    for y in range(WATER + 4, WATER + 9):
        half = max(0, 3 - (y - (WATER + 4)) // 2)
        for z in range(mz - half, mz + half + 1):
            if z != mz:
                s.set(mx + 1, y, z, 'raft:sail_canvas_red_striped' if (y + z) % 3 == 0 else 'raft:sail_canvas')
    s.set(mx, WATER + 10, mz, 'raft:raft_flag[facing=east]')
    s.set(12, WATER + 1, 17, 'minecraft:barrel[facing=up,open=false]')
    s.set(12, WATER + 1, 12, 'raft:wooden_crate[facing=north,open=false]')
    s.set(10, WATER + 2, 15, 'minecraft:spruce_fence')   # mooring cleat
    s.set(9, WATER + 2, 15, ROPE)
    # buoys in the water
    s.set(14, WATER, 8, 'raft:buoy_red[waterlogged=false]')
    s.set(14, WATER, D - 1, 'raft:buoy_green[waterlogged=false]')
    return s


# ------------------------------------------------------------------------------------------------ build
PIECES = {
    'plaza': plaza, 'walk_straight': walk_straight, 'walk_bridge': walk_bridge,
    'walk_long': lambda: walk_straight('walk_long', 15, True), 'walk_short': walk_short,
    'junction_t': junction_t, 'junction_l': junction_l, 'end_cap': end_cap, 'fishing_platform': fishing_platform,
    'hut_fisher': hut_fisher, 'hut_netmaker': hut_netmaker, 'hut_cookhouse': hut_cookhouse,
    'hut_house_a': lambda: hut_house('a'), 'hut_house_b': lambda: hut_house('b'), 'hut_house_c': lambda: hut_house('c'),
    'hut_market': hut_market, 'elder': elder, 'shrine': shrine, 'docks': docks,
}


def build_piece(name):
    s = PIECES[name]()
    # make sure every piece spans local y 0 (stilts) so the deck sits at absolute 101 everywhere
    ys = [p[1] for p in s.blocks]
    assert min(ys) == 0, (name, min(ys))
    # barnacles must not widen the bounding box (connectors have to stay on its edge)
    core = [p for p, b in s.blocks.items() if b[0] != 'raft:barnacles']
    x0, x1 = min(p[0] for p in core), max(p[0] for p in core)
    z0, z1 = min(p[2] for p in core), max(p[2] for p in core)
    for p in [p for p, b in s.blocks.items() if b[0] == 'raft:barnacles']:
        if not (x0 <= p[0] <= x1 and z0 <= p[2] <= z1):
            del s.blocks[p]
    s.finalize()
    return s


def build_all(preview=True):
    from . import worldgen as wg
    out = {}
    pieces = {}
    for name in PIECES:
        s = build_piece(name)
        validate(s, name)
        (x0, y0, z0), _ = s.bounds()
        if (x0, y0, z0) != (0, 0, 0) and name != 'plaza':
            # entry connectors must sit on the north edge: nothing may lie at z < 0
            assert z0 >= 0, (name, 'z0', z0)
        pieces[name] = s
        out[s.name] = (s.save(MODULE), s.size())
        if preview and name in ('plaza', 'elder', 'shrine', 'docks', 'hut_house_c', 'hut_cookhouse'):
            look(s, 'sv_' + name, ne=False)
    # worldgen
    proc = 'raft:saltmere_wear'
    wg.processor_list('saltmere_wear', [
        wg.rule('raft:palm_planks', 0.10, 'raft:weathered_planks'),
        wg.rule('minecraft:spruce_planks', 0.15, 'raft:weathered_planks'),
        wg.rule('raft:coral_stone_bricks', 0.10, 'raft:mossy_coral_stone_bricks'),
    ])

    def el(n, w=1):
        return (f'raft:{P}/{n}', w, proc, 'rigid')
    wg.pool(f'{P}/plaza', [el('plaza')])
    wg.pool(f'{P}/elder', [el('elder')], fallback=f'raft:{P}/ends')
    wg.pool(f'{P}/shrine', [el('shrine')], fallback=f'raft:{P}/ends')
    wg.pool(f'{P}/docks', [el('docks')], fallback=f'raft:{P}/ends')
    wg.pool(f'{P}/walkways', [el('walk_straight', 3), el('walk_bridge', 2), el('walk_long', 1)], fallback=f'raft:{P}/ends')
    wg.pool(f'{P}/junctions', [el('junction_t', 2), el('junction_l', 2)], fallback=f'raft:{P}/ends')
    wg.pool(f'{P}/outer', [el('walk_short', 3), el('fishing_platform', 1)], fallback=f'raft:{P}/ends')
    wg.pool(f'{P}/huts', [el('hut_fisher', 2), el('hut_netmaker', 1), el('hut_cookhouse', 1), el('hut_house_a', 2),
                          el('hut_house_b', 2), el('hut_house_c', 2), el('hut_market', 1)], fallback=f'raft:{P}/ends')
    wg.pool(f'{P}/ends', [el('end_cap')])
    wg.structure('saltmere_village', f'raft:{P}/plaza', 'surface_structures', 'none', 73, None, size=6, max_dist=80)
    wg.structure_set('saltmere_village', 28, 10, 31415922)
    wg.biome_tag('saltmere_village', ['coral_shallows', 'glowing_lagoon'])
    return out


if __name__ == '__main__':
    for k, v in build_all().items():
        print(k, v[1])
