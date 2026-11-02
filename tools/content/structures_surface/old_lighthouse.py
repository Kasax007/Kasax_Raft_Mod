"""Old Lighthouse (story chapter 2, keeper Tobias Fenn).

Pieces (jigsaw, terrain_adaptation beard_thin, projected to WORLD_SURFACE_WG):
  old_lighthouse/compound_a|b  rigid start piece: rocky knoll with the striped tower (spiral stairs, landings,
                               gallery, lantern room with the lighthouse lens), keeper's cottage, storage shed,
                               vegetable garden, paths, lamp posts, grave of the keeper's wife.
  old_lighthouse/jetty         terrain_matching boardwalk + pier head with buoys (attached at the east path end;
                               over water it lies one block above the sea, on land it becomes a raised boardwalk).
Template y=0 is the ground layer (beard_thin flattens the terrain to it).
"""
import math

from .lib import B, Pal, stairs, slab, log, trapdoor, rnd, noise2, look, sign, DIRS, OPP, MODULE

# ------------------------------------------------------------------------------------------------ palettes
WHITE = Pal(('minecraft:white_concrete', 55), ('minecraft:calcite', 30), ('minecraft:white_terracotta', 9),
            ('minecraft:diorite', 6), seed='lh_white')
RED = Pal(('minecraft:red_concrete', 62), ('minecraft:red_terracotta', 26), ('minecraft:red_nether_bricks', 12),
          seed='lh_red')
SBRICK = Pal(('minecraft:stone_bricks', 60), ('minecraft:cracked_stone_bricks', 18), ('minecraft:mossy_stone_bricks', 16),
             ('minecraft:andesite', 6), seed='lh_sb')
ROCK = Pal(('minecraft:stone', 34), ('minecraft:andesite', 22), ('minecraft:cobblestone', 14),
           ('minecraft:mossy_cobblestone', 12), ('minecraft:tuff', 10), ('minecraft:diorite', 4), ('minecraft:gravel', 4),
           seed='lh_rock')
ROCK_TOP = Pal(('minecraft:moss_block', 22), ('minecraft:stone', 20), ('minecraft:andesite', 16),
               ('minecraft:mossy_cobblestone', 16), ('minecraft:coarse_dirt', 10), ('minecraft:grass_block', 10),
               ('minecraft:gravel', 6), seed='lh_rtop')
PATH = Pal(('minecraft:dirt_path', 52), ('minecraft:gravel', 16), ('minecraft:coarse_dirt', 14),
           ('minecraft:cobblestone', 8), ('minecraft:mossy_cobblestone', 4), ('minecraft:packed_mud', 6), seed='lh_path')
PLASTER = Pal(('minecraft:white_terracotta', 20), ('minecraft:calcite', 50), ('minecraft:white_concrete', 18),
              ('minecraft:diorite', 12), seed='lh_plaster')
FLOOR = Pal(('minecraft:spruce_planks', 80), ('minecraft:stripped_spruce_wood', 8), ('minecraft:oak_planks', 12),
            seed='lh_floor')
WEATHERED = Pal(('raft:weathered_planks', 80), ('minecraft:spruce_planks', 12), ('minecraft:stripped_spruce_wood', 8),
                seed='lh_wp')
ROOF = Pal(('minecraft:deepslate_tiles', 70), ('minecraft:cracked_deepslate_tiles', 18), ('minecraft:cobbled_deepslate', 12),
           seed='lh_roof')

TC = (31, 13)          # tower centre (x, z)
Y_FLOOR = 3            # tower ground floor blocks (stand at 4)
Y_TOP = 31             # last tower wall layer
Y_DECK = 32            # gallery / lantern deck
R_BASE, R_TOP = 5.5, 4.0


def r_at(y):
    return R_BASE - (y - (Y_FLOOR + 1)) * (R_BASE - R_TOP) / (Y_TOP - Y_FLOOR - 1)


def dist(x, z):
    return math.hypot(x - TC[0], z - TC[1])


def inward(x, z):
    """Horizontal facing pointing from (x,z) towards the tower centre."""
    dx, dz = TC[0] - x, TC[1] - z
    if abs(dx) >= abs(dz):
        return 'east' if dx > 0 else 'west'
    return 'south' if dz > 0 else 'north'


def band(y):
    return RED if ((y - (Y_FLOOR + 1)) // 4) % 2 == 1 else WHITE


# ------------------------------------------------------------------------------------------------ parts
def rock_knoll(s):
    cx, cz = TC
    R = 12.5
    for x in range(cx - 14, cx + 15):
        for z in range(cz - 14, cz + 15):
            d = dist(x, z)
            n = noise2('lh_knoll', x, z, 3.5)
            reach = R * (0.75 + 0.45 * n)
            if d > reach:
                continue
            h = 4.2 * (1 - (d / reach) ** 1.4) + (n - 0.5) * 1.6
            h = int(round(h))
            if d < 7.2:
                h = max(h, 3)
            for y in range(0, h + 1):
                s.put(x, y, z, ROCK if y < h else ROCK_TOP)
    # boulders around the knoll and in the yard
    r = rnd('lh_boulders')
    for (bx, bz, br) in ((cx + 9, cz + 8, 2.0), (cx - 9, cz - 6, 1.6), (cx + 11, cz - 3, 1.7), (cx + 4, cz + 12, 1.4),
                         (cx - 4, cz - 11, 1.8), (cx + 10, cz + 16, 1.5), (5, 9, 1.6), (3, 28, 1.3), (40, 26, 1.9)):
        for x in range(int(bx - br - 1), int(bx + br + 2)):
            for z in range(int(bz - br - 1), int(bz + br + 2)):
                for y in range(0, int(br) + 2):
                    if math.hypot(x - bx, (y - 0.3) * 1.3, z - bz) <= br + (r.random() - 0.5) * 0.6:
                        s.put(x, y, z, ROCK)


def plinth_and_tower(s):
    cx, cz = TC
    # foundation plinth (stepped), terrace floor
    for x in range(cx - 8, cx + 9):
        for z in range(cz - 8, cz + 9):
            d = dist(x, z)
            for y in range(0, Y_FLOOR + 1):
                rr = 7.0 - 0.25 * y
                if d <= rr:
                    s.put(x, y, z, SBRICK)
            if d <= 6.6 and d > R_BASE + 0.3:
                s.put(x, Y_FLOOR, z, Pal(('minecraft:polished_andesite', 6), ('minecraft:stone_bricks', 3),
                                         ('minecraft:mossy_stone_bricks', 1), seed='lh_terr'))
    # terrace edge: low wall with gaps and lantern posts
    for x in range(cx - 8, cx + 9):
        for z in range(cz - 8, cz + 9):
            d = dist(x, z)
            if 6.1 < d <= 6.6 + 0.3 and s.name_at(x, Y_FLOOR, z):
                ang = math.degrees(math.atan2(z - cz, x - cx)) % 360
                if 160 < ang < 200:      # opening towards the stairs (west)
                    continue
                if rnd('lh_par', x, z).random() < 0.12:
                    continue
                s.set(x, Y_FLOOR + 1, z, 'minecraft:stone_brick_wall')
    # tower shell
    for y in range(Y_FLOOR + 1, Y_TOP + 1):
        r = r_at(y)
        for x in range(cx - 7, cx + 8):
            for z in range(cz - 7, cz + 8):
                d = dist(x, z)
                if d <= r + 0.3:
                    if d > r - 1.25 + 0.3:
                        s.put(x, y, z, band(y))
                    else:
                        s.set(x, y, z, 'minecraft:air')
    # band trims: a thin stone line at each colour change looks like old masonry joints
    # interior floor
    for x in range(cx - 6, cx + 7):
        for z in range(cz - 6, cz + 7):
            if dist(x, z) <= R_BASE - 0.9:
                s.put(x, Y_FLOOR, z, Pal(('minecraft:polished_andesite', 5), ('minecraft:andesite', 2),
                                         ('minecraft:stone_bricks', 3), seed='lh_tfl'))
    # door on the west side with a hood
    dx = cx - 5
    for x in range(cx - 6, cx - 3):
        for y in (Y_FLOOR + 1, Y_FLOOR + 2):
            s.set(x, y, cz, 'minecraft:air')
    s.door(cx - 5, Y_FLOOR + 1, cz, 'minecraft:spruce_door', 'west', hinge='left')
    s.set(cx - 6, Y_FLOOR + 3, cz, stairs('minecraft:stone_brick_stairs', 'east', 'top'))
    s.set(cx - 6, Y_FLOOR + 3, cz - 1, stairs('minecraft:stone_brick_stairs', 'south', 'top'))
    s.set(cx - 6, Y_FLOOR + 3, cz + 1, stairs('minecraft:stone_brick_stairs', 'north', 'top'))
    s.set(cx - 7, Y_FLOOR + 1, cz - 1, 'minecraft:stone_brick_wall')
    s.set(cx - 7, Y_FLOOR + 2, cz - 1, 'minecraft:lantern[hanging=false,waterlogged=false]')
    s.set(cx - 7, Y_FLOOR + 1, cz + 1, 'minecraft:potted_red_tulip')
    # ship bell beside the door (rings a welcome)
    s.set(cx - 6, Y_FLOOR + 2, cz + 1, 'raft:ship_bell[facing=west,waterlogged=false]')
    # stairs up the rock to the terrace (west)
    for i, xx in enumerate(range(cx - 10, cx - 6)):
        top = i + 1
        for z in range(cz - 1, cz + 2):
            for y in range(0, top):
                s.put(xx, y, z, SBRICK)
            if top <= Y_FLOOR:
                s.set(xx, top, z, stairs('minecraft:stone_brick_stairs', 'east'))
            for y in range(top + 1, top + 4):
                if s.name_at(xx, y, z) and 'door' not in s.name_at(xx, y, z):
                    s.set(xx, y, z, 'minecraft:air')
                elif not s.name_at(xx, y, z):
                    s.set(xx, y, z, 'minecraft:air')
    # side walls of the stair cut with lamp
    s.set(cx - 10, 1, cz - 2, 'minecraft:stone_brick_wall'); s.set(cx - 10, 2, cz - 2, 'minecraft:lantern[hanging=false,waterlogged=false]')
    s.set(cx - 10, 1, cz + 2, 'minecraft:stone_brick_wall'); s.set(cx - 10, 2, cz + 2, 'minecraft:lantern[hanging=false,waterlogged=false]')

    # windows: narrow 1x2 openings along the spiral
    for (y, ang) in ((8, 225), (11, 315), (14, 45), (17, 135), (20, 270), (24, 0), (27, 90), (29, 200)):
        a = math.radians(ang)
        for k in range(2):
            yy = y + k
            r = r_at(yy)
            for step in (0.0, 0.5, 1.0):
                wx = int(round(cx + math.cos(a) * (r - step)))
                wz = int(round(cz + math.sin(a) * (r - step)))
                if s.name_at(wx, yy, wz) and s.name_at(wx, yy, wz) != 'minecraft:air':
                    s.set(wx, yy, wz, 'minecraft:glass_pane' if not (y == 20 and k == 1) else 'minecraft:air')
                    break
    # band joints: dark stone lines between colour bands (every 4th layer, sparse)
    spiral_stairs(s)
    landings(s)
    gallery_and_lantern(s)


def spiral_stairs(s):
    cx, cz = TC
    ring = [(0, -1), (1, -1), (1, 0), (1, 1), (0, 1), (-1, 1), (-1, 0), (-1, -1)]   # clockwise from north
    start = 6   # first step on the west cell (next to the door)
    top_k = 2 * Y_DECK - 7          # last step, level with the deck (stand at Y_DECK + 1)
    for k in range(0, top_k + 1):
        rx, rz = ring[(start + k) % 8]
        x, z = cx + rx, cz + rz
        if k % 2 == 0:
            y = Y_FLOOR + 1 + k // 2
            s.set(x, y, z, slab('minecraft:spruce_slab', 'bottom'))
        else:
            y = Y_FLOOR + 1 + (k - 1) // 2
            s.set(x, y, z, slab('minecraft:spruce_slab', 'top'))
        # keep the head room above every step clear of anything we may add later
        for hy in range(y + 1, y + 3):
            if s.name_at(x, hy, z) in (None, 'minecraft:air'):
                s.set(x, hy, z, 'minecraft:air')
    for y in range(Y_FLOOR + 1, Y_DECK + 1):
        s.set(cx, y, cz, 'minecraft:polished_deepslate_wall')
    return top_k


def landings(s):
    cx, cz = TC
    for L, kind in ((12, 'store'), (22, 'watch')):
        k_land = 2 * L - 7          # the step that is level with the landing
        for x in range(cx - 5, cx + 6):
            for z in range(cz - 5, cz + 6):
                d = dist(x, z)
                if d <= r_at(L) - 0.95 and max(abs(x - cx), abs(z - cz)) >= 2:
                    s.set(x, L, z, slab('minecraft:spruce_slab', 'top') if d > 2.9 else 'minecraft:spruce_planks')
        # props along the wall
        if kind == 'store':
            s.chest(cx + 3, L + 1, cz - 2, 'raft:chests/lighthouse_storage', facing='west', block='minecraft:barrel')
            s.set(cx + 3, L + 1, cz - 1, 'minecraft:barrel[facing=up,open=false]')
            s.set(cx + 3, L + 2, cz - 1, 'raft:rope_coil')
            s.set(cx + 2, L + 1, cz + 3, 'minecraft:cauldron')
            s.set(cx - 3, L + 1, cz + 2, 'raft:wooden_crate[facing=east,open=false]')
            s.set(cx - 2, L + 1, cz + 3, 'minecraft:lantern[hanging=false,waterlogged=false]')
            s.set(cx - 3, L + 1, cz - 2, 'minecraft:cobweb')
        else:
            s.set(cx + 3, L + 1, cz, 'raft:spyglass_stand[facing=east,waterlogged=false]')
            s.set(cx - 3, L + 1, cz + 1, 'raft:driftwood_chair[facing=east,waterlogged=false]')
            s.set(cx - 3, L + 1, cz - 1, 'minecraft:spruce_trapdoor[facing=east,half=bottom,open=false,powered=false,waterlogged=false]')
            s.set(cx + 1, L + 1, cz + 3, 'raft:driftwood_shelf[facing=north]')
            s.set(cx - 1, L + 1, cz - 3, 'minecraft:candle[candles=3,lit=false,waterlogged=false]')
            s.set(cx + 2, L + 1, cz - 3, 'minecraft:lantern[hanging=false,waterlogged=false]')


def gallery_and_lantern(s):
    cx, cz = TC
    # corbels under the gallery
    for x in range(cx - 8, cx + 9):
        for z in range(cz - 8, cz + 9):
            d = dist(x, z)
            if r_at(Y_TOP) + 0.3 < d <= r_at(Y_TOP) + 1.3:
                s.set(x, Y_TOP, z, stairs('minecraft:stone_brick_stairs', inward(x, z), 'top'))
            if r_at(Y_TOP - 1) + 0.3 < d <= r_at(Y_TOP - 1) + 0.9 and not s.name_at(x, Y_TOP - 1, z):
                s.set(x, Y_TOP - 1, z, stairs('minecraft:stone_brick_stairs', inward(x, z), 'top'))
    # deck
    RG = 6.3
    for x in range(cx - 8, cx + 9):
        for z in range(cz - 8, cz + 9):
            d = dist(x, z)
            if d <= RG:
                s.set(x, Y_DECK, z, 'minecraft:smooth_stone' if d > 3.3 else 'minecraft:polished_andesite')
            if RG - 0.75 < d <= RG and d > 0:
                s.set(x, Y_DECK + 1, z, 'minecraft:iron_bars')
    # posts of the railing and a broken section
    for ang in range(0, 360, 45):
        a = math.radians(ang + 22.5)
        px, pz = int(round(cx + math.cos(a) * (RG - 0.4))), int(round(cz + math.sin(a) * (RG - 0.4)))
        if s.name_at(px, Y_DECK + 1, pz) == 'minecraft:iron_bars':
            s.set(px, Y_DECK + 1, pz, 'minecraft:polished_blackstone_wall')
            s.set(px, Y_DECK + 2, pz, 'minecraft:polished_blackstone_slab[type=bottom,waterlogged=false]')
    for x in range(cx + 4, cx + 7):
        for z in range(cz + 3, cz + 6):
            if s.name_at(x, Y_DECK + 1, z) == 'minecraft:iron_bars' and rnd('lh_bars', x, z).random() < 0.7:
                s.set(x, Y_DECK + 1, z, 'minecraft:air')
    # stair opening in the deck around the spiral (the top steps sit in it)
    ring = [(0, -1), (1, -1), (1, 0), (1, 1), (0, 1), (-1, 1), (-1, 0), (-1, -1)]
    start = 6
    top_k = 2 * Y_DECK - 7
    for k in range(top_k - 7, top_k + 1):
        rx, rz = ring[(start + k) % 8]
        x, z = cx + rx, cz + rz
        y = Y_FLOOR + 1 + (k // 2 if k % 2 == 0 else (k - 1) // 2)
        if y < Y_DECK:
            s.set(x, Y_DECK, z, 'minecraft:air')
    for k in range(top_k - 1, top_k + 1):
        rx, rz = ring[(start + k) % 8]
        x, z = cx + rx, cz + rz
        s.set(x, Y_DECK, z, slab('minecraft:spruce_slab', 'bottom' if k % 2 == 0 else 'top'))
    # lantern room: radius 3 ring of glass with black mullions
    RL = 3.0
    for y in range(Y_DECK + 1, Y_DECK + 5):
        for x in range(cx - 4, cx + 5):
            for z in range(cz - 4, cz + 5):
                d = dist(x, z)
                if RL - 0.7 < d <= RL + 0.3:
                    rx, rz = x - cx, z - cz
                    mull = (abs(rx) == abs(rz)) or rx == 0 or rz == 0
                    if y == Y_DECK + 1:
                        s.set(x, y, z, 'minecraft:polished_blackstone_bricks')
                    elif mull:
                        s.set(x, y, z, 'minecraft:polished_blackstone_wall')
                    else:
                        s.set(x, y, z, 'minecraft:glass_pane')
                elif d <= RL - 0.7 and s.name_at(x, y, z) is None:
                    s.set(x, y, z, 'minecraft:air')
    # door to the gallery (east)
    s.door(cx + 3, Y_DECK + 1, cz, 'minecraft:dark_oak_door', 'east', hinge='right')
    s.set(cx + 3, Y_DECK + 3, cz, 'minecraft:polished_blackstone_wall')
    # lens on the centre column + brass ring
    s.set(cx, Y_DECK + 1, cz, 'raft:lighthouse_lens')
    s.set(cx, Y_DECK + 2, cz, 'raft:lighthouse_lens')
    s.set(cx, Y_DECK + 3, cz, 'minecraft:lightning_rod[facing=up,powered=false,waterlogged=false]')
    # roof: cornice + copper dome + finial
    yR = Y_DECK + 5
    for x in range(cx - 5, cx + 6):
        for z in range(cz - 5, cz + 6):
            d = dist(x, z)
            if d <= RL + 0.3:
                s.set(x, yR, z, 'minecraft:polished_blackstone_bricks' if d > RL - 0.7 else 'minecraft:waxed_weathered_cut_copper')
            elif d <= RL + 1.3:
                s.set(x, yR, z, slab('minecraft:polished_blackstone_slab', 'top'))
    for i, rr in enumerate((2.6, 1.8, 0.9)):
        y = yR + 1 + i
        for x in range(cx - 4, cx + 5):
            for z in range(cz - 4, cz + 5):
                d = dist(x, z)
                if rr - 1.0 < d <= rr + 0.35:
                    s.set(x, y, z, stairs('minecraft:waxed_weathered_cut_copper_stairs', inward(x, z)))
                elif d <= rr - 1.0:
                    s.set(x, y, z, 'minecraft:waxed_weathered_cut_copper')
    s.set(cx, yR + 4, cz, 'minecraft:waxed_weathered_copper')
    s.set(cx, yR + 5, cz, 'minecraft:lightning_rod[facing=up,powered=false,waterlogged=false]')
    s.set(cx, yR + 6, cz, 'minecraft:lightning_rod[facing=up,powered=false,waterlogged=false]')
    # hanging lanterns under the gallery for the night look
    for (ox, oz) in ((5, 0), (-5, 0), (0, 5), (0, -5)):
        x, z = cx + ox, cz + oz
        if s.name_at(x, Y_TOP, z) and 'stairs' in s.name_at(x, Y_TOP, z):
            s.set(x, Y_TOP - 1, z, 'minecraft:lantern[hanging=true,waterlogged=false]') if s.name_at(x, Y_TOP - 1, z) is None else None


def cottage(s):
    x1, x2, z1, z2 = 7, 18, 17, 25
    # foundation and floor
    s.box(x1, 0, z1, x2, 1, z2, SBRICK)
    s.box(x1 + 1, 1, z1 + 1, x2 - 1, 1, z2 - 1, FLOOR)
    # walls: stone base course, plaster above, log frame
    for y in range(2, 6):
        for x in range(x1, x2 + 1):
            for z in range(z1, z2 + 1):
                edge = x in (x1, x2) or z in (z1, z2)
                if not edge:
                    s.set(x, y, z, 'minecraft:air')
                    continue
                post = (x in (x1, x2) and z in (z1, z2)) or (z in (z1, z2) and (x - x1) % 4 == 0 and y > 2) or \
                       (x in (x1, x2) and (z - z1) % 4 == 0 and y > 2)
                if post:
                    s.set(x, y, z, log('minecraft:stripped_spruce_log'))
                elif y == 2:
                    s.put(x, y, z, Pal(('minecraft:cobblestone', 5), ('minecraft:mossy_cobblestone', 2),
                                       ('minecraft:stone_bricks', 3), seed='lh_course'))
                else:
                    s.put(x, y, z, PLASTER)
    # wall plate
    for x in range(x1, x2 + 1):
        s.set(x, 6, z1, log('minecraft:stripped_spruce_log', 'x')); s.set(x, 6, z2, log('minecraft:stripped_spruce_log', 'x'))
    for z in range(z1, z2 + 1):
        s.set(x1, 6, z, log('minecraft:stripped_spruce_log', 'z')); s.set(x2, 6, z, log('minecraft:stripped_spruce_log', 'z'))
    # ceiling beams
    for x in range(x1 + 2, x2 - 1, 3):
        for z in range(z1 + 1, z2):
            s.set(x, 6, z, log('minecraft:stripped_spruce_log', 'z'))
    for x in range(x1 + 1, x2):
        for z in range(z1 + 1, z2):
            if s.name_at(x, 6, z) is None:
                s.set(x, 6, z, 'minecraft:air')
    # roof (ridge along x), slate, gables plaster
    s.gable_roof(x1, z1, x2, z2, 6, 'x', 'minecraft:deepslate_tile_stairs', overhang=1,
                 top='minecraft:deepslate_tile_slab[type=bottom,waterlogged=false]', gable=PLASTER,
                 under='minecraft:spruce_stairs')
    # attic air
    for y in range(7, 12):
        for x in range(x1 + 1, x2):
            for z in range(z1 + 1, z2):
                if s.name_at(x, y, z) is None:
                    s.set(x, y, z, 'minecraft:air')
    # moss patches on the roof (old slate)
    for (pos, (name, props, nbt)) in list(s.blocks.items()):
        if name == 'minecraft:deepslate_tile_stairs' and rnd('lh_moss', *pos).random() < 0.10:
            s.set(*pos, f"minecraft:cobbled_deepslate_stairs[facing={props['facing']},half={props['half']},shape=straight,waterlogged=false]")
    # chimney on the west gable
    cxm = x1 - 1
    for y in range(0, 14):
        for z in range(20, 23):
            if y < 12 or z == 21:
                s.put(cxm, y, z, Pal(('minecraft:bricks', 8), ('minecraft:stone_bricks', 1), ('minecraft:mud_bricks', 1), seed='lh_ch'))
    s.set(cxm, 12, 20, 'minecraft:brick_wall'); s.set(cxm, 12, 22, 'minecraft:brick_wall')
    s.set(cxm, 14, 21, 'minecraft:cobweb')
    # hearth inside west wall
    s.set(x1, 2, 21, 'minecraft:air'); s.set(x1, 3, 21, 'minecraft:air')
    s.set(x1, 2, 21, 'minecraft:campfire[facing=east,lit=false,signal_fire=false,waterlogged=false]')
    s.set(x1, 4, 21, 'minecraft:bricks')
    s.set(x1 + 1, 2, 20, 'minecraft:smoker[facing=east,lit=false]')
    s.set(x1 + 1, 2, 22, 'minecraft:barrel[facing=up,open=false]')
    s.set(x1 + 1, 3, 22, 'minecraft:flower_pot')
    s.set(x1 + 1, 3, 20, 'minecraft:lantern[hanging=false,waterlogged=false]')
    # kitchen shelf + table + chairs
    s.set(x1 + 1, 4, 18, 'raft:driftwood_shelf[facing=east]')
    s.set(x1 + 1, 2, 18, 'minecraft:crafting_table')
    s.set(x1 + 1, 2, 19, 'minecraft:barrel[facing=up,open=false]')
    s.set(11, 2, 21, 'raft:driftwood_table[waterlogged=false]')
    s.set(11, 2, 20, 'raft:driftwood_chair[facing=south,waterlogged=false]')
    s.set(11, 2, 22, 'raft:driftwood_chair[facing=north,waterlogged=false]')
    s.set(12, 2, 21, 'raft:driftwood_chair[facing=west,waterlogged=false]')
    # bedroom/office (east end)
    s.bed(17, 2, 23, 'red', 'north')   # foot (17,23) head (17,22)
    s.set(17, 2, 24, 'raft:wooden_crate[facing=west,open=false]')
    s.set(16, 2, 21, 'minecraft:spruce_trapdoor[facing=east,half=top,open=false,powered=false,waterlogged=false]')
    s.set(16, 3, 21, 'minecraft:candle[candles=2,lit=false,waterlogged=false]')
    # desk with the story chest
    s.set(14, 2, 18, 'raft:chart_table[facing=south]')
    s.set(15, 2, 18, 'raft:chart_table[facing=south]')
    s.chest(13, 2, 18, 'raft:chests/story/lighthouse', facing='south')
    s.set(14, 2, 19, 'raft:driftwood_chair[facing=north,waterlogged=false]')
    s.set(16, 2, 18, 'raft:expedition_radio[facing=south,waterlogged=false]')
    s.set(17, 2, 18, 'minecraft:bookshelf'); s.set(17, 3, 18, 'minecraft:bookshelf'); s.set(17, 4, 18, 'minecraft:bookshelf')
    s.set(17, 2, 19, 'minecraft:bookshelf'); s.set(17, 3, 19, 'minecraft:chiseled_bookshelf[facing=west,slot_0_occupied=true,slot_1_occupied=false,slot_2_occupied=true,slot_3_occupied=true,slot_4_occupied=false,slot_5_occupied=false]')
    s.set(13, 3, 18, 'minecraft:air')
    s.set(15, 4, 18, 'raft:driftwood_shelf[facing=south]')
    # rug + hanging lanterns
    for x in range(12, 16):
        for z in range(20, 24):
            if s.name_at(x, 2, z) is None:
                s.set(x, 2, z, 'raft:woven_rug')
    s.set(10, 5, 21, 'minecraft:lantern[hanging=true,waterlogged=false]')
    s.set(15, 5, 21, 'minecraft:lantern[hanging=true,waterlogged=false]')
    # windows (2 wide, panes) with shutters
    def window(xa, za, face, width=2):
        dx, dz = DIRS[face]
        along = (1, 0) if face in ('north', 'south') else (0, 1)
        for i in range(width):
            x, z = xa + along[0] * i, za + along[1] * i
            s.set(x, 3, z, 'minecraft:glass_pane'); s.set(x, 4, z, 'minecraft:glass_pane')
            # sill outside
            s.set(x + dx, 2, z + dz, stairs('minecraft:spruce_stairs', OPP[face], 'top')) if s.name_at(x + dx, 2, z + dz) is None else None
        for i in (-1, width):
            x, z = xa + along[0] * i + dx, za + along[1] * i + dz
            if s.name_at(x, 3, z) is None:
                s.set(x, 3, z, trapdoor('minecraft:spruce_trapdoor', face, open_=True))
                s.set(x, 4, z, trapdoor('minecraft:spruce_trapdoor', face, open_=True))
    window(9, z1, 'north'); window(13, z1, 'north'); window(9, z2, 'south')
    window(x2, 20, 'east'); window(x1, 18, 'west', 1)
    # broken pane
    s.set(14, 4, z1, 'minecraft:air')
    # door (south, open) with porch
    s.door(13, 2, z2, 'minecraft:spruce_door', 'south', hinge='right', open_=True)
    s.set(16, 2, z2, log('minecraft:stripped_spruce_log'))
    sign(s, 14, 3, z2 + 1, 'minecraft:spruce_wall_sign[facing=south,waterlogged=false]',
         ['', 'T. FENN', 'Keeper of', 'the Light'])
    # porch deck + roof
    for x in range(11, 17):
        for z in range(z2 + 1, z2 + 4):
            s.set(x, 0, z, SBRICK.pick(x, 0, z))
            s.set(x, 1, z, 'minecraft:spruce_planks' if z < z2 + 3 else stairs('minecraft:spruce_stairs', 'north'))
    for (px, pz) in ((11, z2 + 2), (16, z2 + 2)):
        for y in range(2, 5):
            s.set(px, y, pz, 'minecraft:spruce_fence')
    for x in range(10, 18):
        for z in range(z2 + 1, z2 + 4):
            s.set(x, 5, z, slab('minecraft:spruce_slab', 'bottom'))
    s.set(13, 4, z2 + 2, 'minecraft:lantern[hanging=true,waterlogged=false]')
    s.set(15, 2, z2 + 1, stairs('minecraft:spruce_stairs', 'north'))           # bench
    s.set(16, 2, z2 + 1, 'minecraft:spruce_trapdoor[facing=west,half=bottom,open=true,powered=false,waterlogged=false]')
    s.set(11, 2, z2 + 1, 'minecraft:potted_dandelion')
    s.set(12, 2, z2 + 1, 'raft:potted_hibiscus')
    # woodpile and rain barrel on the east side
    for z in range(z1 + 1, z1 + 5):
        for y in range(1, 3):
            s.set(x2 + 1, y, z, log('minecraft:spruce_log', 'z'))
    s.set(x2 + 1, 3, z1 + 2, log('minecraft:spruce_log', 'z'))
    s.set(x2 + 1, 1, z2 - 1, 'minecraft:water_cauldron[level=3]')
    s.set(x2 + 1, 1, z2 - 2, 'minecraft:barrel[facing=up,open=false]')
    # flower bed along the north wall
    for x in range(x1, x2 + 1):
        z = z1 - 1
        s.set(x, 0, z, 'minecraft:rooted_dirt' if x % 3 else 'minecraft:coarse_dirt')
        p = rnd('lh_fl', x).random()
        if p < 0.25:
            s.set(x, 1, z, 'raft:hibiscus')
        elif p < 0.4:
            s.set(x, 1, z, 'minecraft:short_grass')
        elif p < 0.55:
            s.set(x, 1, z, 'minecraft:poppy')
        elif p < 0.65:
            s.tall(x, 1, z, 'raft:bird_of_paradise')


def shed(s):
    x1, x2, z1, z2 = 24, 29, 28, 33
    s.box(x1, 0, z1, x2, 0, z2, Pal(('minecraft:cobblestone', 4), ('minecraft:gravel', 1), ('minecraft:stone', 2), seed='lh_shf'))
    for y in range(1, 5):
        for x in range(x1, x2 + 1):
            for z in range(z1, z2 + 1):
                edge = x in (x1, x2) or z in (z1, z2)
                if not edge:
                    s.set(x, y, z, 'minecraft:air'); continue
                if x in (x1, x2) and z in (z1, z2):
                    s.set(x, y, z, log('minecraft:spruce_log'))
                elif y == 4 and x == x1:
                    s.set(x, y, z, log('minecraft:spruce_log', 'z'))
                else:
                    s.put(x, y, z, WEATHERED)
    # lean-to roof of corrugated metal sloping east
    for x in range(x1 - 1, x2 + 2):
        y = 5 if x <= x1 + 1 else (4 if x >= x2 else 5)
        for z in range(z1 - 1, z2 + 2):
            if x <= x1 + 2:
                s.set(x, 5, z, 'raft:corrugated_metal_slab[type=bottom,waterlogged=false]' if x == x1 - 1 else 'raft:corrugated_metal')
            else:
                s.set(x, 5, z, 'raft:corrugated_metal_slab[type=bottom,waterlogged=false]')
    for x in range(x1 + 1, x2):
        for z in range(z1 + 1, z2):
            if s.name_at(x, 4, z) == 'minecraft:air' and x > x1 + 2:
                pass
    # hole in the roof + vine
    s.set(x1 + 3, 5, z1 + 2, 'minecraft:air'); s.set(x1 + 4, 5, z1 + 2, 'minecraft:air')
    s.set(x1 + 3, 4, z1 + 2, 'minecraft:air')
    # door west
    s.door(x1, 1, z1 + 3, 'minecraft:spruce_door', 'west', hinge='left')
    s.set(x1, 3, z1 + 3, WEATHERED.pick(0, 0, 0))
    # contents
    s.chest(x2 - 1, 1, z1 + 1, 'raft:chests/lighthouse_storage', facing='west')
    s.set(x2 - 1, 1, z1 + 2, 'minecraft:barrel[facing=up,open=false]')
    s.set(x2 - 1, 2, z1 + 2, 'raft:rope_coil')
    s.set(x2 - 1, 1, z2 - 1, 'raft:wooden_crate[facing=west,open=false]')
    s.set(x2 - 2, 1, z2 - 1, 'minecraft:cauldron')
    s.set(x1 + 1, 1, z2 - 1, 'minecraft:barrel[facing=up,open=false]')
    s.set(x1 + 1, 2, z2 - 1, 'minecraft:barrel[facing=up,open=false]')
    s.set(x1 + 2, 1, z1 + 1, 'minecraft:grindstone[face=floor,facing=north]')
    s.set(x1 + 3, 3, z2 - 1, 'raft:ship_anchor[facing=north,waterlogged=false]')
    s.set(x1 + 1, 4, z1 + 1, 'minecraft:cobweb')
    s.set(x2 - 1, 3, z1 + 1, 'minecraft:cobweb')
    s.set(x1 + 2, 3, z1 + 1, 'minecraft:lantern[hanging=false,waterlogged=false]') if False else None
    s.set(x1 + 2, 4, z1 + 3, 'minecraft:lantern[hanging=true,waterlogged=false]')
    # outside: life ring and oars
    s.set(x2 + 1, 2, z1 + 3, 'raft:life_ring[facing=east,waterlogged=false]')
    s.set(x1 + 2, 1, z2 + 1, 'raft:rope_coil')
    # upturned rowboat next to the shed
    for i in range(5):
        x = x1 + i
        z = z2 + 2
        s.set(x, 1, z, stairs('minecraft:spruce_stairs', 'south', 'top' if False else 'bottom') if i in (0, 4) else slab('minecraft:spruce_slab', 'top'))
        s.set(x, 1, z + 1, stairs('minecraft:spruce_stairs', 'north') if i in (0, 4) else slab('minecraft:spruce_slab', 'top'))


def garden(s):
    x1, x2, z1, z2 = 4, 15, 31, 40
    # fence
    for x in range(x1, x2 + 1):
        for z in (z1, z2):
            s.set(x, 1, z, 'minecraft:spruce_fence')
    for z in range(z1, z2 + 1):
        for x in (x1, x2):
            s.set(x, 1, z, 'minecraft:spruce_fence')
    s.set(10, 1, z1, 'minecraft:spruce_fence_gate[facing=north,in_wall=false,open=true,powered=false]')
    # broken fence bit
    s.set(x1, 1, 36, 'minecraft:air'); s.set(x1, 1, 37, 'minecraft:air')
    crops = ['minecraft:carrots[age=7]', 'minecraft:potatoes[age=7]', 'minecraft:beetroots[age=3]', 'raft:pineapple_crop[age=7]']
    for z in range(z1 + 1, z2):
        for x in range(x1 + 1, x2):
            r = rnd('lh_gd', x, z).random()
            if x in (x1 + 1, x2 - 1) or z in (z1 + 1, z2 - 1):
                s.set(x, 0, z, 'minecraft:coarse_dirt' if r < 0.5 else 'minecraft:rooted_dirt')
                if r < 0.35:
                    s.set(x, 1, z, 'minecraft:short_grass')
                elif r < 0.45:
                    s.set(x, 1, z, 'raft:hibiscus')
                elif r < 0.5:
                    s.set(x, 1, z, 'minecraft:poppy')
                else:
                    s.set(x, 1, z, 'minecraft:air')
                continue
            if x == 9:   # water channel + path
                s.set(x, 0, z, 'minecraft:water' if z % 4 else 'minecraft:mossy_cobblestone')
                s.set(x, 1, z, 'minecraft:air')
                continue
            s.set(x, 0, z, 'minecraft:farmland[moisture=7]')
            row = (z - z1 - 2) // 2
            c = crops[(row + (1 if x > 9 else 0)) % len(crops)]
            if r < 0.15:
                s.set(x, 1, z, 'minecraft:short_grass')   # overgrown
            elif r < 0.22:
                s.set(x, 1, z, 'minecraft:air')
            else:
                s.set(x, 1, z, c)
    # scarecrow, composter, watering bucket
    s.tall(12, 1, 33, 'raft:scarecrow', 'facing=north')
    s.set(12, 0, 33, 'minecraft:coarse_dirt')
    s.set(x1 + 1, 1, z2 - 1, 'minecraft:composter[level=5]')
    s.set(x2 - 1, 1, z2 - 1, 'minecraft:water_cauldron[level=2]')
    s.set(x1 + 1, 1, z1 + 1, 'raft:pineapple_bush[age=3]')


def grave(s):
    # small grave of the keeper's wife on the knoll edge, overlooking the sea
    gx, gz = 39, 24
    s.set(gx, 1, gz, 'minecraft:mossy_cobblestone_wall')
    s.set(gx, 2, gz, 'minecraft:mossy_cobblestone_slab[type=bottom,waterlogged=false]') if False else None
    s.set(gx, 2, gz, 'minecraft:candle[candles=1,lit=false,waterlogged=false]')
    for z in (gz + 1, gz + 2):
        s.set(gx, 0, z, 'minecraft:coarse_dirt')
        s.set(gx, 1, z, 'raft:hibiscus' if z == gz + 1 else 'minecraft:lily_of_the_valley')
    s.set(gx + 1, 1, gz + 1, 'raft:seashells[shells=3,waterlogged=false]')
    # bench looking out
    s.set(gx - 2, 1, gz + 1, stairs('minecraft:spruce_stairs', 'west'))
    s.set(gx - 2, 1, gz, 'minecraft:spruce_trapdoor[facing=north,half=bottom,open=true,powered=false,waterlogged=false]')
    s.set(gx - 2, 1, gz + 2, 'minecraft:spruce_trapdoor[facing=south,half=bottom,open=true,powered=false,waterlogged=false]')


def paths(s):
    def lay(pts, width=2):
        for (ax, az), (bx, bz) in zip(pts, pts[1:]):
            n = max(abs(bx - ax), abs(bz - az))
            for i in range(n + 1):
                t = i / max(1, n)
                x, z = round(ax + (bx - ax) * t), round(az + (bz - az) * t)
                for ox in range(width):
                    for oz in range(width):
                        px, pz = x + ox, z + oz
                        cur = s.name_at(px, 0, pz)
                        if cur is None or cur in ('minecraft:grass_block', 'minecraft:coarse_dirt'):
                            s.put(px, 0, pz, PATH)
                            if s.name_at(px, 1, pz) is None:
                                s.set(px, 1, pz, 'minecraft:air')
    lay([(43, 37), (32, 37), (22, 36), (21, 31), (17, 29), (13, 28)])     # jetty -> porch
    lay([(21, 31), (21, 22), (20, 14)])                                     # -> tower stairs
    lay([(20, 13), (21, 13)], 1)
    lay([(21, 31), (23, 31)], 1)                                            # -> shed door
    lay([(13, 28), (10, 29), (10, 30)], 1)                                  # -> garden gate
    lay([(32, 37), (36, 30), (37, 26)], 1)                                  # -> bench and grave
    # lamp posts along the path
    for (x, y, z) in ((40, 1, 39), (28, 1, 39), (23, 1, 26), (23, 1, 17)):
        s.lamp_post(x, y, z, 2, 'minecraft:spruce_fence')
    # stepping stones of the jetty landing
    for z in range(36, 40):
        s.set(43, 0, z, 'minecraft:dirt_path') if s.name_at(43, 0, z) is None else None


def jetty_link(s):
    s.jigsaw(43, 0, 37, 'east_up', 'raft:lighthouse_path', 'raft:lighthouse_jetty', 'raft:old_lighthouse/jetty',
             final_state='minecraft:dirt_path')


def build_compound(variant):
    s = B(f'old_lighthouse/compound_{variant}', seed=f'lh{variant}')
    rock_knoll(s)
    plinth_and_tower(s)
    cottage(s)
    shed(s)
    garden(s)
    grave(s)
    paths(s)
    for pos in [p for p in s.blocks if not (0 <= p[0] <= 43 and 0 <= p[2] <= 43)]:
        del s.blocks[pos]
    jetty_link(s)
    if variant == 'b':
        # variant b: storm-worn and overgrown – vines on the tower, moss, wild garden, broken fences, shed roof down
        s.replace(lambda n: n == 'minecraft:cobblestone', 'minecraft:mossy_cobblestone', chance=0.45, seed=7)
        s.replace(lambda n: n == 'minecraft:stone_bricks', 'minecraft:mossy_stone_bricks', chance=0.3, seed=8)
        s.replace(lambda n: n == 'minecraft:spruce_fence', 'minecraft:air', chance=0.18, seed=9)
        r = rnd('lh_b')
        cx, cz = TC
        for y in range(Y_FLOOR + 1, Y_FLOOR + 12):
            for x in range(cx - 8, cx + 9):
                for z in range(cz - 8, cz + 9):
                    if s.name_at(x, y, z) is not None:
                        continue
                    ang = math.degrees(math.atan2(z - cz, x - cx)) % 360
                    if not (150 < ang < 300):        # north-west side only
                        continue
                    for f, (dx, dz) in DIRS.items():
                        nb = s.name_at(x + dx, y, z + dz)
                        if nb and ('concrete' in nb or 'calcite' in nb or 'terracotta' in nb or 'nether_bricks' in nb
                                   or 'diorite' in nb) and r.random() < 0.55 - (y - Y_FLOOR) * 0.04:
                            s.set(x, y, z, f'minecraft:vine[{f}=true]')
                            break
        for (pos, (name, props, nbt)) in list(s.blocks.items()):
            if name.startswith('minecraft:carrots') or name.startswith('minecraft:potatoes') or name.startswith('minecraft:beetroots'):
                q = r.random()
                if q < 0.35:
                    s.set(*pos, 'minecraft:short_grass')
                elif q < 0.45:
                    s.set(*pos, 'minecraft:tall_grass[half=lower]')
                    s.set(pos[0], pos[1] + 1, pos[2], 'minecraft:tall_grass[half=upper]')
        for x in range(25, 29):
            for z in range(29, 33):
                if s.name_at(x, 5, z) and 'corrugated' in s.name_at(x, 5, z) and r.random() < 0.6:
                    s.set(x, 5, z, 'minecraft:air')
        s.set(26, 1, 30, 'raft:corrugated_metal_slab[type=bottom,waterlogged=false]')
        s.set(27, 1, 31, 'raft:corrugated_metal_slab[type=bottom,waterlogged=false]')
    s.finalize()
    return s


# ------------------------------------------------------------------------------------------------ jetty
def build_jetty():
    s = B('old_lighthouse/jetty', seed='lhj')
    deck = Pal(('raft:weathered_planks', 55), ('minecraft:spruce_planks', 30), ('minecraft:stripped_spruce_wood', 15), seed='lhj_d')
    L = 18          # boardwalk length (x 0..L-1), 3 wide (z 2..4); head x L..L+6, 7 wide (z 0..6)
    for x in range(0, L):
        for z in range(2, 5):
            s.put(x, 1, z, deck)
        if x % 3 == 0:
            for z in (1, 5):
                s.set(x, 0, z, log('minecraft:spruce_log'))
                s.set(x, 1, z, log('minecraft:spruce_log'))
                s.set(x, 2, z, 'minecraft:spruce_fence')
        else:
            for z in (1, 5):
                s.set(x, 2, z, 'raft:rope_railing')
        if x % 3 == 1:
            s.set(x, 0, 3, log('minecraft:spruce_log'))
    # broken plank
    s.set(10, 1, 3, 'minecraft:air')
    s.set(11, 1, 2, slab('minecraft:spruce_slab', 'top'))
    for x in range(L, L + 7):
        for z in range(0, 7):
            s.put(x, 1, z, deck)
            if (x - L) % 3 == 0 and z in (0, 3, 6):
                s.set(x, 0, z, log('minecraft:spruce_log'))
    # bollards, railing, lantern posts
    for (x, z) in ((L, 0), (L, 6), (L + 6, 0), (L + 6, 6)):
        s.set(x, 2, z, log('minecraft:stripped_spruce_log'))
        s.set(x, 3, z, 'minecraft:spruce_fence')
        s.set(x, 4, z, 'minecraft:lantern[hanging=false,waterlogged=false]')
    for x in range(L + 1, L + 6):
        s.set(x, 2, 0, 'raft:rope_railing'); s.set(x, 2, 6, 'raft:rope_railing')
    for z in range(1, 6):
        s.set(L + 6, 2, z, 'raft:rope_railing') if z != 3 else s.set(L + 6, 2, z, log('minecraft:stripped_spruce_log'))
    s.set(L + 6, 3, 3, 'raft:ship_bell[facing=west,waterlogged=false]') if False else None
    s.set(L + 5, 2, 3, 'raft:ship_bell[facing=west,waterlogged=false]')
    s.set(L + 6, 3, 3, 'minecraft:spruce_fence')
    s.set(L + 6, 4, 3, 'minecraft:spruce_fence')
    s.set(L + 7, 4, 3, 'raft:life_ring[facing=east,waterlogged=false]')
    # props on the head: crates, nets, fishing barrel, bench
    s.set(L + 1, 2, 1, 'raft:wooden_crate[facing=south,open=false]')
    s.set(L + 2, 2, 1, 'minecraft:barrel[facing=up,open=false]')
    s.set(L + 1, 3, 1, 'raft:rope_coil')
    s.set(L + 1, 2, 5, stairs('minecraft:spruce_stairs', 'south'))
    s.set(L + 2, 2, 5, stairs('minecraft:spruce_stairs', 'south'))
    s.set(L + 4, 2, 5, 'minecraft:cauldron')
    # buoys in the water off the head (local y 0 = sea surface over water)
    s.set(L + 9, 0, -1, 'raft:buoy_red[waterlogged=false]')
    s.set(L + 9, 0, 7, 'raft:buoy_green[waterlogged=false]')
    s.set(L + 12, 0, 3, 'raft:buoy_red[waterlogged=false]')
    # moored rowboat on the south side of the head
    bz = 8
    for i in range(5):
        x = L + 1 + i
        if i in (0, 4):
            s.set(x, 0, bz, stairs('minecraft:spruce_stairs', 'east' if i == 0 else 'west'))
            s.set(x, 0, bz + 1, stairs('minecraft:spruce_stairs', 'east' if i == 0 else 'west'))
        else:
            s.set(x, 0, bz, slab('minecraft:spruce_slab', 'bottom'))
            s.set(x, 0, bz + 1, slab('minecraft:spruce_slab', 'bottom'))
    s.set(L + 3, 1, bz, 'minecraft:spruce_trapdoor[facing=north,half=bottom,open=false,powered=false,waterlogged=false]')
    s.set(L + 1, 1, 7, 'minecraft:spruce_fence')   # mooring line post
    # connector at the land end (faces west)
    s.jigsaw(0, 1, 3, 'west_up', 'raft:lighthouse_jetty', 'raft:lighthouse_path', 'minecraft:empty',
             final_state='raft:weathered_planks')
    # lamp at the boardwalk start
    s.set(0, 2, 1, 'minecraft:spruce_fence'); s.set(0, 3, 1, 'minecraft:lantern[hanging=false,waterlogged=false]')
    s.finalize()
    return s


def build_all(preview=True):
    out = {}
    for v in ('a', 'b'):
        s = build_compound(v)
        p = s.save(MODULE)
        out[s.name] = (p, s.size())
        if preview and v == 'a':
            look(s, 'lh_a', cuts=(5, 13, 23, 33))
    j = build_jetty()
    out[j.name] = (j.save(MODULE), j.size())
    if preview:
        look(j, 'lh_jetty', ne=False)
    return out


if __name__ == '__main__':
    print(build_all())
