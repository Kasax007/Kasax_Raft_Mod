"""iceberg_outpost - a Meridian Expedition weather and research hut on an ice floe in the Frost Floes.

The floe is part of the template (packed/blue ice with a snow crust, a submerged ice keel and an ice ridge); on it
stand the hut of corrugated metal (porthole windows, stove with chimney, bunks, desk with a research terminal and
radio, chart table), a generator shed, an antenna mast with a satellite dish, fuel barrels, a sled and snow drifts.
Fixed start height 90: the floe's top ice layer is the template's y=10 (= world y 100, one above the sea).
"""
import math
import random

from .lib import Side, contact, pick, save, spawns, write_structure

TOP = 10                    # template y of the top ice layer (world 100)
ICE = [('minecraft:packed_ice', 6), ('minecraft:ice', 1), ('minecraft:blue_ice', 1), ('minecraft:snow_block', 2)]
WALL = [('raft:corrugated_metal', 7), ('raft:rusted_plate', 1)]


def floe(s, rnd):
    """A natural iceberg: irregular outline (overlapping wobbly blobs), a low snow shelf for the camp, a submerged
    rim around it, a jagged tapering peak in the north-west and a deep keel that narrows downwards."""
    blobs = [(0, 0, 11), (9, 4, 7), (-8, 3, 7), (2, -9, 7), (-4, 9, 6), (8, 11, 5), (-12, -9, 8)]
    ph = [rnd.random() * math.tau for _ in range(len(blobs) * 2)]
    peak = (-13, -10)
    for x in range(-26, 26):
        for z in range(-26, 26):
            inside = 0.0
            for i, (bx, bz, r) in enumerate(blobs):
                a = math.atan2(z - bz, x - bx)
                rr = r * (1 + 0.16 * math.sin(3 * a + ph[2 * i]) + 0.09 * math.sin(7 * a + ph[2 * i + 1]))
                inside = max(inside, 1 - math.hypot(x - bx, z - bz) / rr)
            if inside <= 0:
                continue
            top = TOP - 2 if inside < 0.07 else TOP - 1 if inside < 0.16 else TOP
            p = max(0.0, 1 - math.hypot(x - peak[0], z - peak[1]) / 10.0)
            if p > 0 and top == TOP:
                top += int(p ** 1.4 * 15 + rnd.random() * 2.2 * p)
            depth = int(3 + inside * 15 + rnd.random() * 2)          # keel: deepest under the middle
            band = (x * 3 + z * 5 + int(rnd.random() * 3)) % 11 < 2  # streaks of blue ice
            for y in range(TOP - depth, top + 1):
                if y == top and top >= TOP and p < 0.35:
                    b = 'minecraft:snow_block'
                elif band:
                    b = 'minecraft:blue_ice'
                else:
                    b = pick(rnd, [('minecraft:packed_ice', 9), ('minecraft:ice', 1)])
                s.set(x, y, z, b)
            if top == TOP and p < 0.2 and inside > 0.2 and rnd.random() < 0.6:
                s.set(x, TOP + 1, z, f'minecraft:snow[layers={rnd.randint(1, 3)}]')


def hut(s, rnd):
    x0, x1, z0, z1 = -4, 4, -2, 4
    F = TOP + 1                                      # floor level
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            s.set(x, F, z, 'minecraft:spruce_planks' if x0 < x < x1 and z0 < z < z1 else 'raft:rusted_plate')
            for y in range(F + 1, F + 4):
                wall = x in (x0, x1) or z in (z0, z1)
                if wall:
                    corner = x in (x0, x1) and z in (z0, z1)
                    s.set(x, y, z, 'raft:steel_beam[axis=y]' if corner else pick(rnd, WALL))
                else:
                    s.set(x, y, z, 'minecraft:air')
    # gable roof of corrugated metal stairs, ridge along x
    for z in range(z0 - 1, z1 + 2):
        dz = min(z - (z0 - 1), (z1 + 1) - z)
        y = F + 3 + min(dz, 3)                       # eaves beside the wall top, then resting on the walls
        face = 'south' if z <= (z0 + z1) // 2 else 'north'
        for x in range(x0 - 1, x1 + 2):
            mid = z == (z0 + z1) // 2 + (0 if (z1 - z0) % 2 == 0 else 1)
            s.set(x, y, z, 'raft:corrugated_metal_slab[type=bottom]' if dz >= 3 else
                  f'raft:corrugated_metal_stairs[facing={face},half=bottom]')
            if dz >= 3 and rnd.random() < 0.6:
                s.set(x, y + 1, z, f'minecraft:snow[layers={rnd.randint(1, 2)}]')
        for x in (x0, x1):
            for yy in range(F + 4, y):
                s.set(x, yy, z, pick(rnd, WALL))
        for x in range(x0 + 1, x1):
            for yy in range(F + 4, y):
                s.set(x, yy, z, 'minecraft:air')
    # portholes, door with a small porch
    for (x, z) in ((x0, 1), (x1, 1), (-2, z1), (2, z1)):
        s.set(x, F + 2, z, 'raft:porthole')
    s.set(0, F + 1, z0, 'raft:bulkhead_door[facing=north,half=lower,hinge=left,open=false,powered=false]')
    s.set(0, F + 2, z0, 'raft:bulkhead_door[facing=north,half=upper,hinge=left,open=false,powered=false]')
    for x in (-1, 0, 1):
        s.set(x, F, z0 - 1, 'raft:catwalk[type=bottom]')
        s.set(x, F, z0 - 2, 'raft:catwalk[type=bottom]')
    s.set(1, F + 2, z0 - 1, 'raft:ship_lantern[hanging=true]')
    # interior
    s.set(-3, F + 1, 3, 'minecraft:light_blue_bed[facing=north,occupied=false,part=foot]')
    s.set(-3, F + 1, 2, 'minecraft:light_blue_bed[facing=north,occupied=false,part=head]')
    s.set(-3, F + 1, 1, 'raft:cushion_blue')
    s.set(3, F + 1, 3, 'minecraft:smoker[facing=west,lit=true]')
    for y in range(F + 2, F + 9):
        s.set(3, y, 3, 'raft:rusty_pipe[axis=y]')
    s.set(3, F + 9, 3, 'minecraft:campfire[facing=north,lit=false,signal_fire=false]')
    s.set(3, F + 1, 2, 'minecraft:barrel[facing=up,open=false]')
    s.chest(3, F + 1, -1, 'raft:chests/iceberg_outpost', facing='west')
    s.block_entity(-1, F + 1, 3, 'raft:research_terminal[facing=north]', 'raft:research_terminal', {'LogId': 'lab_misc_2'})
    s.set(0, F + 1, 3, 'raft:expedition_radio[facing=north]')
    s.set(1, F + 1, 3, 'raft:server_rack[facing=north]')
    s.set(-3, F + 1, -1, 'raft:chart_table[facing=east]')
    s.set(-2, F + 1, -1, 'raft:driftwood_chair[facing=west]')
    s.set(-3, F + 2, 0, 'raft:driftwood_shelf[facing=east]')
    s.set(0, F + 1, 1, 'raft:woven_rug')
    s.set(0, F + 5, 1, 'minecraft:lantern[hanging=true]')
    # generator shed (lean-to) on the east side
    for z in range(0, 4):
        for y in range(F, F + 3):
            s.set(x1 + 3, y, z, pick(rnd, WALL))
        s.set(x1 + 1, F + 3, z, 'raft:corrugated_metal_slab[type=bottom]')
        s.set(x1 + 2, F + 3, z, 'raft:corrugated_metal_slab[type=bottom]')
        s.set(x1 + 3, F + 3, z, 'raft:corrugated_metal_slab[type=bottom]')
    s.set(x1 + 2, F + 1, 1, 'minecraft:blast_furnace[facing=west,lit=false]')
    s.set(x1 + 2, F + 1, 2, 'raft:rusty_pipe_valve[axis=z]')
    s.set(x1 + 1, F + 1, 3, 'minecraft:barrel[facing=up,open=false]')
    s.set(x1 + 2, F + 1, 3, 'minecraft:barrel[facing=up,open=false]')
    s.set(x1 + 2, F + 2, 3, 'minecraft:barrel[facing=up,open=false]')
    # antenna mast with dish, guy lines (chains)
    mx, mz = -7, -3
    for y in range(F, F + 10):
        s.set(mx, y, mz, 'raft:steel_beam[axis=y]')
    s.set(mx, F + 10, mz, 'minecraft:lightning_rod[facing=up,powered=false]')
    s.set(mx + 1, F + 7, mz, 'raft:satellite_dish[facing=east]')
    # weather station: anemometer cups on a post, a thermometer box
    for y in range(F, F + 4):
        s.set(6, y, 3, 'minecraft:spruce_fence')
    s.set(6, F + 4, 3, 'minecraft:lightning_rod[facing=up,powered=false]')
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        s.set(6 + dx, F + 4, 3 + dz, 'minecraft:iron_trapdoor[facing=north,half=bottom,open=false]')
    s.set(6, F + 2, 4, 'minecraft:white_concrete')
    # a rowboat frozen into the edge of the floe
    for z in range(9, 15):
        hw = 1 if 10 <= z <= 13 else 0
        for x in range(8 - hw, 9 + hw):
            edge = abs(x - 8) == hw or z in (9, 14)
            s.set(x, TOP, z, 'minecraft:packed_ice' if not edge else 'minecraft:spruce_planks')
            if edge:
                s.set(x, TOP + 1, z, 'minecraft:spruce_slab[type=bottom]')
            else:
                s.set(x, TOP + 1, z, 'minecraft:snow[layers=2]')
    s.set(8, TOP + 1, 12, 'minecraft:spruce_slab[type=bottom]')
    s.set(9, TOP + 1, 15, 'minecraft:spruce_trapdoor[facing=south,half=bottom,open=true]')
    # sled, fuel drums, flags, crates
    for z in range(5, 9):
        s.set(-8, F, z, 'minecraft:spruce_slab[type=bottom]')
        s.set(-6, F, z, 'minecraft:spruce_slab[type=bottom]')
    s.set(-7, F, 5, 'raft:wooden_crate[facing=south,open=false]')
    s.set(-7, F, 6, 'raft:wooden_crate[facing=south,open=false]')
    s.set(-7, F + 1, 5, 'minecraft:white_carpet')
    for (x, z) in ((6, -4), (7, -4), (6, -5)):
        s.set(x, F, z, 'minecraft:red_concrete' if (x + z) % 2 else 'minecraft:barrel[facing=up,open=false]')
    for (x, z, c) in ((9, 6, 'red'), (-12, 1, 'orange'), (2, -14, 'red')):
        s.set(x, F, z, 'minecraft:spruce_fence')
        s.set(x, F + 1, z, 'minecraft:spruce_fence')
        s.set(x, F + 2, z, f'minecraft:{c}_wool')


def main(preview=False):
    rnd = random.Random(31415910)
    s = Side('iceberg_outpost')
    floe(s, rnd)
    hut(s, rnd)
    # clean junk snow inside the hut walls: snow only stays on full blocks
    for (x, y, z), b in list(s.blocks.items()):
        if b[0] == 'minecraft:snow':
            below = s.blocks.get((x, y - 1, z))
            if below is None or below[0] in ('minecraft:snow', 'minecraft:air') or not (
                    'ice' in below[0] or 'snow_block' in below[0] or 'corrugated_metal_slab' in below[0]):
                s.set(x, y, z, None)
    path = 'iceberg_outpost/outpost_0'
    (_, y0, _), _ = s.bounds()
    save(s, path, anchored=True)
    if preview:
        print('  preview', contact(s, 'iceberg_outpost', cuts=(TOP + 3,), scale=6))
    write_structure('iceberg_outpost', [(path, 1)], None,
                    step='surface_structures', terrain='none', start_height=100 - (TOP - y0), heightmap=None,
                    biomes=['frost_floes'], spacing=24, separation=8, salt=31415910, max_distance=40,
                    spawn_overrides={'creature': spawns([('raft:penguin', 6, 2, 4), ('minecraft:polar_bear', 1, 1, 1)])})
    return [(path, s)]
