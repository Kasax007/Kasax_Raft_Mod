"""buoy_field - navigation markers of the old Meridian shipping lane, still blinking on the open sea.
Buoys sit in the water surface (y 99), short chains hang below them; a chained supply crate floats between them.
 0 channel: a gate of red (port) and green (starboard) buoys marking a lane, chained crate in the middle
 1 beacon: a big lit marker float (red/white striped drum with a lantern cage) surrounded by small buoys,
   two crates on a chain
Fixed start height 95: the template's y=4 is the water surface (99).
"""
import random

from .lib import Side, contact, pick, save, write_structure

SURF = 4


def buoy(s, x, z, color, chain=SURF):
    s.set(x, SURF, z, f'raft:buoy_{color}')
    for i in range(1, chain + 1):
        s.set(x, SURF - i, z, 'minecraft:iron_chain[axis=y]')


def crate_on_chain(s, rnd, a, b):
    """Floating crate tied to buoy a by a chain at the surface (a, b: (x, z) of buoy and crate)."""
    (ax, az), (bx, bz) = a, b
    s.chest(bx, SURF, bz, 'raft:gameplay/supply_barrel', facing='north', block='raft:wooden_crate')
    s.set(bx, SURF - 1, bz, 'minecraft:barrel[facing=up,open=false]')
    if ax == bx:
        for z in range(min(az, bz) + 1, max(az, bz)):
            s.set(ax, SURF, z, 'minecraft:iron_chain[axis=z]')
    else:
        for x in range(min(ax, bx) + 1, max(ax, bx)):
            s.set(x, SURF, az, 'minecraft:iron_chain[axis=x]')
        if az != bz:
            for z in range(min(az, bz) + 1, max(az, bz) + 1):
                s.set(bx, SURF, z, 'minecraft:iron_chain[axis=z]')


def channel(rnd):
    s = Side('channel')
    for i, z in enumerate((0, 9, 18)):
        buoy(s, 0 + (i % 2), z, 'red')
        buoy(s, 12 - (i % 2), z + 2, 'green')
    crate_on_chain(s, rnd, (1, 9), (4, 9))
    # a drifting life ring and debris near the lane
    s.set(7, SURF, 14, 'raft:palm_planks')
    s.set(8, SURF, 14, 'raft:palm_slab[type=bottom]')
    s.set(6, SURF, 4, 'minecraft:barrel[facing=east,open=false]')
    return s


def beacon(rnd):
    s = Side('beacon')
    c = (6, 6)
    cx, cz = c
    # striped drum float: 3x3, two blocks into the water, one above
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            for y in (SURF - 2, SURF - 1):
                s.set(cx + dx, y, cz + dz, 'minecraft:red_concrete' if y == SURF - 2 else 'minecraft:white_concrete')
            if abs(dx) + abs(dz) < 2:
                s.set(cx + dx, SURF, cz + dz, 'minecraft:red_concrete')
            else:
                s.set(cx + dx, SURF, cz + dz, 'raft:steel_railing')
    for i in range(1, SURF - 1):
        s.set(cx, SURF - 2 - i, cz, 'minecraft:iron_chain[axis=y]')
    # lantern cage on top
    s.set(cx, SURF + 1, cz, 'raft:steel_beam[axis=y]')
    s.set(cx, SURF + 2, cz, 'raft:steel_beam[axis=y]')
    s.set(cx, SURF + 3, cz, 'minecraft:white_concrete')
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        s.set(cx + dx, SURF + 3, cz + dz, 'minecraft:iron_bars')
    s.set(cx, SURF + 4, cz, 'raft:buoy_red')
    s.set(cx + 1, SURF + 1, cz, 'raft:ship_bell[facing=east]')
    s.set(cx - 1, SURF + 1, cz, 'raft:life_ring[facing=west]')
    for (x, z, col) in ((0, 0, 'green'), (13, 2, 'red'), (1, 12, 'red'), (12, 13, 'green')):
        buoy(s, x, z, col)
    crate_on_chain(s, rnd, (12, 13), (9, 13))
    crate_on_chain(s, rnd, (cx + 1, cz + 1), (cx + 4, cz + 1))
    return s


BUILDERS = [channel, beacon]


def main(preview=False):
    out = []
    for v, fn in enumerate(BUILDERS):
        rnd = random.Random(31415912 + v)
        s = fn(rnd)
        path = f'buoy_field/{fn.__name__}'
        save(s, path)
        out.append((path, s))
        if preview:
            print('  preview', contact(s, f'buoy_field_{fn.__name__}', scale=8, ne=False))
    write_structure('buoy_field', [(p, 1) for p, _ in out], None,
                    step='surface_structures', terrain='none', start_height=95, heightmap=None,
                    biomes=['coral_shallows', 'raft_ocean_biome'], spacing=14, separation=5, salt=31415912,
                    max_distance=32)
    return out
