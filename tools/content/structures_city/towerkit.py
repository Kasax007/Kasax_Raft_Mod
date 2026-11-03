"""Tower kit: facades (piers, bands, windows, sills, cornices), floor plates, stair cores, interiors, decay.

A tower is a rectangle x0..x1 / z0..z1 (outer wall ring) with floors every `fh` blocks starting at y0 (slab of the
ground floor). The front of every building is the north side (z0).
"""
import math
import random

from .citylib import (CONCRETE, FLOOR, H4, RUBBLE, City, bite, rubble_pile, slab, stair)
from .common import DIRS, OPP, CW, CCW, is_full, pick

SIDES = ('north', 'east', 'south', 'west')


class Style:
    def __init__(self, **kw):
        self.corner = CONCRETE
        self.pier = CONCRETE
        self.band = CONCRETE
        self.wall = CONCRETE
        self.glass = 'minecraft:glass_pane'
        self.bay = 3
        self.floor = FLOOR
        self.sill = None          # stairs block for outside sills under windows
        self.cornice = None       # stairs block for the roof cornice
        self.parapet = None       # wall block on the roof edge
        self.pier_out = False     # piers stick out one block
        self.lintel = True        # top row of a floor is wall
        self.__dict__.update(kw)


def ring(x0, z0, x1, z1):
    """Yield (x, z, side, i, n) of the wall ring; corners are yielded once with side=None."""
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if x0 < x < x1 and z0 < z < z1:
                continue
            if (x in (x0, x1)) and (z in (z0, z1)):
                yield x, z, None, 0, 0
            elif z == z0:
                yield x, z, 'north', x - x0, x1 - x0 + 1
            elif z == z1:
                yield x, z, 'south', x1 - x, x1 - x0 + 1
            elif x == x1:
                yield x, z, 'east', z - z0, z1 - z0 + 1
            else:
                yield x, z, 'west', z1 - z, z1 - z0 + 1


def shell(s, x0, z0, x1, z1, y0, floors, fh, st, rnd, cell=None, top_band=True):
    """Outer walls of a tower block. cell(side, i, n, k, r) may return a state/palette to override a window cell
    (k = floor index, r = row inside the floor 1..fh-1). Returns the y of the roof slab."""
    top = y0 + floors * fh
    s.wet(x0 + 1, y0 + 1, z0 + 1, x1 - 1, top - 1, z1 - 1)
    for x, z, side, i, n in ring(x0, z0, x1, z1):
        for y in range(y0, top + 1):
            k, r = divmod(y - y0, fh)
            if side is None:
                s.pset(x, y, z, st.corner, rnd)
                continue
            if r == 0 or (y == top and top_band):
                s.pset(x, y, z, st.band, rnd)
                continue
            if i % st.bay == 0:
                s.pset(x, y, z, st.pier, rnd)
                if st.pier_out:
                    dx, _, dz = DIRS[side]
                    s.pset(x + dx, y, z + dz, st.pier, rnd)
                continue
            if st.lintel and r == fh - 1:
                s.pset(x, y, z, st.wall, rnd)
                continue
            v = cell(side, i, n, k, r) if cell else None
            if v is None:
                v = st.glass
            if v == 'sill':
                v = st.wall
            s.pset(x, y, z, v, rnd)
        # sills / ledges
    if st.sill:
        for x, z, side, i, n in ring(x0, z0, x1, z1):
            if side is None or i % st.bay == 0:
                continue
            dx, _, dz = DIRS[side]
            for k in range(1, floors):
                y = y0 + k * fh
                if not s.free(x + dx, y, z + dz):
                    continue
                s.set(x + dx, y, z + dz, stair(st.sill, OPP[side], 'top'))
    if st.cornice:
        for x, z, side, i, n in ring(x0, z0, x1, z1):
            if side is None:
                continue
            dx, _, dz = DIRS[side]
            if s.free(x + dx, top, z + dz):
                s.set(x + dx, top, z + dz, stair(st.cornice, OPP[side], 'top'))
    return top


def slabs(s, x0, z0, x1, z1, y0, floors, fh, st, rnd, skip=None, roof=True):
    for k in range(0, floors + (1 if roof else 0)):
        y = y0 + k * fh
        for x in range(x0 + 1, x1):
            for z in range(z0 + 1, z1):
                if skip and skip(x, y, z):
                    continue
                s.pset(x, y, z, st.floor, rnd)


def parapet(s, x0, z0, x1, z1, y, block, rnd=None, gaps=0.0):
    rnd = rnd or s.rnd
    for x, z, side, i, n in ring(x0, z0, x1, z1):
        if rnd.random() < gaps:
            continue
        s.set(x, y, z, block)


def spiral_stairs(s, x0, z0, x1, z1, y_from, y_to, block, newel=CONCRETE, rnd=None, walls=None, doors=None,
                  fh=5, y0=None):
    """Stair spiral in the shaft rectangle (inclusive), rising one block per cell around its edge. The shaft is
    water; its inner cells form a newel column. walls: palette for a wall ring around the shaft (one block
    outside) with a door opening on side `doors` at each floor (y0 + k*fh)."""
    rnd = rnd or s.rnd
    cells = []
    for x in range(x0, x1 + 1):
        cells.append((x, z0))
    for z in range(z0 + 1, z1 + 1):
        cells.append((x1, z))
    for x in range(x1 - 1, x0 - 1, -1):
        cells.append((x, z1))
    for z in range(z1 - 1, z0, -1):
        cells.append((x0, z))
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            for y in range(y_from, y_to + 1):
                if x0 < x < x1 and z0 < z < z1:
                    s.pset(x, y, z, newel, rnd)
                else:
                    s.set(x, y, z, 'minecraft:water')
                    s.interior.add((x, y, z))
    n = len(cells)
    for j, y in enumerate(range(y_from, y_to + 1)):
        cx, cz = cells[j % n]
        nx, nz = cells[(j + 1) % n]
        d = 'east' if nx > cx else 'west' if nx < cx else 'south' if nz > cz else 'north'
        s.set(cx, y, cz, stair(block, d))
        # landing block under the step's back so the spiral reads as solid
        if j > 0:
            px, pz = cells[(j - 1) % n]
    if walls:
        for x in range(x0 - 1, x1 + 2):
            for z in range(z0 - 1, z1 + 2):
                if x0 <= x <= x1 and z0 <= z <= z1:
                    continue
                for y in range(y_from, y_to + 1):
                    s.pset(x, y, z, walls, rnd)
        if doors and y0 is not None:
            for k in range(0, 100):
                yy = y0 + k * fh
                if yy > y_to:
                    break
                if yy + 1 < y_from:
                    continue
                for side in (doors if isinstance(doors, (list, tuple)) else [doors]):
                    mx, mz = (x0 + x1) // 2, (z0 + z1) // 2
                    if side == 'north':
                        pos = [(mx, z0 - 1), (mx + 1, z0 - 1)]
                    elif side == 'south':
                        pos = [(mx, z1 + 1), (mx + 1, z1 + 1)]
                    elif side == 'east':
                        pos = [(x1 + 1, mz), (x1 + 1, mz + 1)]
                    else:
                        pos = [(x0 - 1, mz), (x0 - 1, mz + 1)]
                    for (px, pz) in pos:
                        for dy in (1, 2, 3):
                            if yy + dy <= y_to:
                                s.set(px, yy + dy, pz, 'minecraft:water')
                                s.interior.add((px, yy + dy, pz))
    return cells


def elevator(s, x0, z0, x1, z1, y_from, y_to, rnd, walls=CONCRETE, cab_y=None):
    """Open elevator shaft (water) with guide chains and a wrecked cab at cab_y."""
    for x in range(x0 - 1, x1 + 2):
        for z in range(z0 - 1, z1 + 2):
            for y in range(y_from, y_to + 1):
                if x0 <= x <= x1 and z0 <= z <= z1:
                    s.set(x, y, z, 'minecraft:water')
                    s.interior.add((x, y, z))
                else:
                    s.pset(x, y, z, walls, rnd)
    for y in range(y_from, y_to + 1):
        s.set(x0, y, z0, 'minecraft:iron_chain[axis=y]')
    if cab_y is not None:
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                s.set(x, cab_y, z, 'raft:rusted_plate')
                s.set(x, cab_y + 3, z, 'raft:rusted_plate' if rnd.random() < 0.7 else 'minecraft:water')


def door_opening(s, x, y, z, h=3, w=2, along='x'):
    for i in range(w):
        for dy in range(h):
            px, pz = (x + i, z) if along == 'x' else (x, z + i)
            s.set(px, y + dy, pz, 'minecraft:water')
            s.interior.add((px, y + dy, pz))


def partition(s, x1, z1, x2, z2, y, h, palette, rnd, doors=()):
    """Interior wall from (x1,z1) to (x2,z2) (axis-aligned) h high; doors = list of (x, z) door cells (2 high)."""
    for x in range(min(x1, x2), max(x1, x2) + 1):
        for z in range(min(z1, z2), max(z1, z2) + 1):
            for dy in range(h):
                if (x, z) in doors and dy < 2:
                    continue
                s.pset(x, y + dy, z, palette, rnd)


# ------------------------------------------------------------------------------------------------ interiors
def ceiling_lights(s, x0, z0, x1, z1, y_ceil, rnd, step=5, light='minecraft:sea_lantern', chance=0.8):
    for x in range(x0 + 2, x1 - 1, step):
        for z in range(z0 + 2, z1 - 1, step):
            if rnd.random() < chance and is_full(s.blocks.get((x, y_ceil, z), ('minecraft:air',))[0]):
                s.set(x, y_ceil, z, light)


def office_floor(s, x0, z0, x1, z1, y, rnd, loot=None, terminal=None, avoid=None, h=4):
    """Open-plan office on the floor whose first free layer is y (inside x0..x1, z0..z1)."""
    avoid = avoid or (lambda x, z: False)
    desk = rnd.choice(['minecraft:spruce_slab', 'minecraft:dark_oak_slab', 'minecraft:birch_slab'])
    chair = rnd.choice(['minecraft:dark_oak_stairs', 'minecraft:spruce_stairs', 'minecraft:mangrove_stairs'])
    placed_loot = 0
    for z in range(z0 + 2, z1 - 1, 4):
        for x in range(x0 + 2, x1 - 2, 4):
            if any(avoid(xx, zz) for xx in (x - 1, x, x + 1, x + 2) for zz in (z - 1, z, z + 1, z + 2)):
                continue
            if rnd.random() < 0.15:
                continue
            for dx in (0, 1):
                s.set(x + dx, y, z, slab(desk, 'top'))
                s.set(x + dx, y, z + 1, slab(desk, 'top'))
            if rnd.random() < 0.8:
                s.set(x, y, z - 1, stair(chair, 'north'))
            if rnd.random() < 0.6:
                s.set(x + 1, y, z + 2, stair(chair, 'south'))
            if rnd.random() < 0.7:
                s.set(x, y + 1, z, 'minecraft:gray_stained_glass_pane')
            if rnd.random() < 0.4:
                s.set(x + 1, y + 1, z + 1, 'minecraft:black_stained_glass_pane')
            if rnd.random() < 0.15:
                s.set(x + 1, y + 1, z, 'minecraft:decorated_pot')
    # filing cabinets along the side walls
    for z in range(z0 + 1, z1, 2):
        for x in (x0 + 1, x1 - 1):
            if avoid(x, z) or not s.free(x, y, z):
                continue
            r = rnd.random()
            if r < 0.25:
                s.set(x, y, z, 'minecraft:barrel[facing=up,open=false]')
                if rnd.random() < 0.5:
                    s.set(x, y + 1, z, 'minecraft:barrel[facing=up,open=false]')
            elif r < 0.35:
                s.set(x, y, z, 'minecraft:chiseled_bookshelf[facing=' + ('east' if x == x0 + 1 else 'west') + ']')
    spots = [(x0 + 1, z0 + 1), (x1 - 1, z1 - 1), (x1 - 1, z0 + 1), (x0 + 1, z1 - 1)]
    rnd.shuffle(spots)
    if loot:
        for (x, z) in spots:
            if not avoid(x, z) and s.free(x, y, z):
                s.chest(x, y, z, loot, facing='south' if z == z0 + 1 else 'north', block='minecraft:barrel'
                        if rnd.random() < 0.5 else 'minecraft:chest')
                break
    if terminal:
        for (x, z) in spots[::-1]:
            if not avoid(x, z) and s.free(x, y, z) and s.free(x, y + 1, z):
                f = 'south' if z == z0 + 1 else 'north'
                s.set(x, y, z, slab(desk, 'top'))
                s.block_entity(x, y + 1, z, f'raft:research_terminal[facing={f}]', 'raft:research_terminal',
                               {'LogId': terminal})
                break


def apartment_unit(s, x0, z0, x1, z1, y, rnd, loot=None, front='north'):
    """One flat in the rectangle (inclusive interior cells); y = first free layer; 3 rows high."""
    wall = [('minecraft:white_terracotta', 50), ('minecraft:smooth_sandstone', 25), ('minecraft:calcite', 15),
            ('raft:cracked_concrete', 10)]
    w = x1 - x0 + 1
    mid = x0 + w // 2
    # partition between living (front half) and bedroom/kitchen (back half)
    zm = (z0 + z1) // 2
    doors = {(mid, zm), (mid + 1, zm)} if rnd.random() < 0.5 else {(x0 + 1, zm), (x0 + 2, zm)}
    partition(s, x0, zm, x1, zm, y, 3, wall, rnd, doors=doors)
    # living room: couch + table + TV / bookshelf
    couch = rnd.choice(['minecraft:resin_brick_stairs', 'minecraft:mangrove_stairs', 'minecraft:warped_stairs',
                        'minecraft:crimson_stairs'])
    cz = z0 + 1
    for x in range(x0 + 1, min(x1, x0 + 4)):
        s.set(x, y, cz, stair(couch, 'north'))
    s.set(x0, y, cz, stair(couch, 'east')) if s.free(x0, y, cz) else None
    if zm - z0 >= 4:
        s.set(x0 + 2, y, cz + 2, slab('minecraft:spruce_slab', 'top'))
    s.set(x1, y, z0 + 1, 'minecraft:bookshelf')
    s.set(x1, y + 1, z0 + 1, 'minecraft:bookshelf' if rnd.random() < 0.6 else 'minecraft:water')
    if zm - z0 >= 3:
        s.set(x1, y, zm - 1, 'minecraft:black_concrete')
        s.set(x1, y + 1, zm - 1, 'minecraft:black_stained_glass_pane')
    # back half: kitchen on one side, bed on the other
    bz = zm + 1
    if z1 - bz >= 1:
        s.set(x0, y, z1, 'minecraft:smoker[facing=north,lit=false]')
        s.set(x0 + 1, y, z1, 'minecraft:smooth_quartz' if rnd.random() < 0.6 else 'minecraft:polished_diorite')
        s.set(x0 + 1, y + 1, z1, 'minecraft:light_gray_stained_glass_pane' if rnd.random() < 0.4 else 'minecraft:water')
        if x1 - x0 >= 5:
            s.set(x0 + 2, y, z1, 'minecraft:iron_block')
            s.set(x0 + 2, y + 1, z1, 'minecraft:iron_block')
        # bed: two wool blocks along z on the far side
        col = rnd.choice(['red', 'blue', 'light_blue', 'yellow', 'green', 'pink', 'cyan'])
        s.set(x1, y, z1, 'minecraft:white_wool')
        s.set(x1, y, z1 - 1, f'minecraft:{col}_wool') if z1 - 1 > zm else None
        if x1 - 1 > x0 + 2:
            s.set(x1 - 1, y, z1, 'minecraft:barrel[facing=up,open=false]')
            if loot and rnd.random() < 0.7:
                s.chest(x1 - 1, y, z1, loot, facing='north', block='minecraft:chest')
            s.set(x1 - 1, y + 1, z1, 'minecraft:lantern[hanging=false]') if rnd.random() < 0.5 else None


def shop_floor(s, x0, z0, x1, z1, y, rnd, loot=None, kind='general'):
    """Ground-floor shop: counter across the back, shelves along the sides, displays near the windows."""
    counter_z = z1 - 2
    for x in range(x0 + 1, x1 - 1):
        s.set(x, y, counter_z, 'minecraft:polished_diorite' if kind != 'diner' else 'minecraft:smooth_quartz')
        s.set(x, y + 1, counter_z, slab('minecraft:smooth_quartz_slab', 'bottom') if rnd.random() < 0.7
              else 'minecraft:water')
    s.set(x1 - 1, y, counter_z, 'minecraft:water')
    for z in range(z0 + 1, counter_z):
        for x in (x0, x1):
            s.set(x, y, z, 'minecraft:oak_shelf[facing=' + ('east' if x == x0 else 'west') + ']')
            if rnd.random() < 0.7:
                s.set(x, y + 1, z, 'minecraft:oak_shelf[facing=' + ('east' if x == x0 else 'west') + ']')
    if kind == 'diner':
        for x in range(x0 + 1, x1 - 1, 3):
            s.set(x, y, z0 + 2, 'raft:driftwood_table')
            s.set(x, y, z0 + 1, 'raft:driftwood_chair[facing=south]')
            s.set(x, y, z0 + 3, 'raft:driftwood_chair[facing=north]')
    else:
        for x in range(x0 + 2, x1 - 1, 3):
            s.set(x, y, z0 + 3, 'minecraft:barrel[facing=up,open=false]')
            s.set(x, y + 1, z0 + 3, 'minecraft:decorated_pot' if rnd.random() < 0.5 else 'minecraft:water')
    if loot:
        s.chest(x0 + 1, y, z1, loot, facing='north', block='minecraft:barrel' if rnd.random() < 0.5 else 'minecraft:chest')


def furnish_rooms_generic(s, x0, z0, x1, z1, y, rnd):
    """Clutter for back rooms: crates, barrels, rubble."""
    for _ in range(3):
        x = rnd.randint(x0, x1)
        z = rnd.randint(z0, z1)
        if s.free(x, y, z):
            s.set(x, y, z, rnd.choice(['minecraft:barrel[facing=up,open=false]', 'raft:wooden_crate[facing=north,open=false]',
                                       'raft:cracked_concrete_slab[type=bottom]']))


# ------------------------------------------------------------------------------------------------ decay
def break_windows(s, box, rnd, missing=0.35, cracked=0.3, glass_names=('glass_pane',)):
    (x0, y0, z0), (x1, y1, z1) = box
    for x in range(x0, x1 + 1):
        for y in range(y0, y1 + 1):
            for z in range(z0, z1 + 1):
                b = s.blocks.get((x, y, z))
                if not b or not any(g in b[0] for g in glass_names):
                    continue
                r = rnd.random()
                if r < missing:
                    s.set(x, y, z, 'minecraft:water')
                elif r < missing + cracked:
                    s.set(x, y, z, 'raft:cracked_glass_pane')


def floor_holes(s, x0, z0, x1, z1, ys, rnd, n=2, rmax=3, rubble=True):
    """Collapsed patches in floor slabs (ys) with debris on the floor below."""
    for y in ys:
        for _ in range(n):
            cx = rnd.randint(x0 + 2, x1 - 2)
            cz = rnd.randint(z0 + 2, z1 - 2)
            r = rnd.uniform(1.2, rmax)
            for x in range(int(cx - r - 1), int(cx + r + 2)):
                for z in range(int(cz - r - 1), int(cz + r + 2)):
                    if math.hypot(x - cx, z - cz) <= r + rnd.random() * 0.6 and x0 < x < x1 and z0 < z < z1:
                        b = s.blocks.get((x, y, z))
                        if b and is_full(b[0]) and b[2] is None:
                            s.set(x, y, z, 'minecraft:water')
                            s.interior.add((x, y, z))
            if rubble:
                # debris on the next solid floor below
                yy = y - 1
                while yy > 0 and s.free(int(cx), yy, int(cz)):
                    yy -= 1
                rubble_pile(s, int(cx), yy + 1, int(cz), max(1, int(r)), rnd, height=2)


def wear(s, rnd, swaps):
    """swaps: {block: [(state, weight)...]} applied to `chance` part of the matching blocks."""
    for pos, (name, props, nbt) in list(s.blocks.items()):
        if name in swaps:
            chance, pal = swaps[name]
            if rnd.random() < chance:
                st = pick(rnd, pal)
                s.set(*pos, st)


def broken_top(s, x0, z0, x1, z1, y_lo, y_hi, rnd, beams='raft:steel_beam[axis=y]', jag=6, seed_peaks=3):
    """Jagged break line: everything above a noisy height (y_lo..y_hi) inside the box is removed (outside the
    building -> sea, inside -> water); steel beams stick out of the break."""
    peaks = [(rnd.randint(x0, x1), rnd.randint(z0, z1), rnd.randint(y_hi - 3, y_hi)) for _ in range(seed_peaks)]

    def cut_h(x, z):
        h = y_lo
        for (px, pz, py) in peaks:
            d = math.hypot(x - px, z - pz)
            h = max(h, py - d * (y_hi - y_lo) / jag)
        return h + rnd.random() * 1.5

    for x in range(x0 - 1, x1 + 2):
        for z in range(z0 - 1, z1 + 2):
            h = cut_h(x, z)
            for y in range(int(h) + 1, y_hi + 12):
                b = s.blocks.get((x, y, z))
                if b is None or 'jigsaw' in b[0]:
                    continue
                if b[2] is not None:
                    continue
                if (x, y, z) in s.interior:
                    s.set(x, y, z, 'minecraft:water')
                else:
                    del s.blocks[(x, y, z)]
    # beams sticking out at pier positions
    for x, z in ((x0, z0), (x1, z0), (x0, z1), (x1, z1)):
        if rnd.random() < 0.7:
            h = int(cut_h(x, z))
            for y in range(h + 1, h + 1 + rnd.randint(2, 5)):
                s.set(x, y, z, beams)
    for _ in range(4):
        x = rnd.choice([x0, x1, rnd.randint(x0, x1)])
        z = rnd.choice([z0, z1, rnd.randint(z0, z1)])
        h = int(cut_h(x, z))
        for y in range(h + 1, h + 1 + rnd.randint(1, 3)):
            s.set(x, y, z, 'minecraft:iron_bars' if rnd.random() < 0.5 else beams)
