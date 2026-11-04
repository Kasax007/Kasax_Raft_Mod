"""castaway_camp - tiny camps of stranded survivors on Palm Isle beaches, three shapes:
 0 lean_to: palm-log lean-to with a thatched slope, log seats around a cold campfire, signal pyre, SOS in stones
 1 tent: canvas A-frame tent, hammock between two palms posts, rain cauldron, drying rack, HELP in driftwood
 2 boat_shelter: an upturned lifeboat propped on logs as a roof, stone fire ring, tally-mark post, SOS in shells
Template y=0 is the ground (start height -1 under WORLD_SURFACE_WG); only marked ground cells are replaced.
"""
import random

from .lib import Side, contact, pick, save, write_structure

G = 0
FONT = {
    'S': ['###', '#..', '###', '..#', '###'],
    'O': ['###', '#.#', '#.#', '#.#', '###'],
    'H': ['#.#', '#.#', '###', '#.#', '#.#'],
    'E': ['###', '#..', '##.', '#..', '###'],
    'L': ['#..', '#..', '#..', '#..', '###'],
    'P': ['###', '#.#', '###', '#..', '#..'],
}


def word(s, rnd, text, x0, z0, block, ground=True):
    """Letters lying on the ground, reading along +x (rows along +z)."""
    x = x0
    for ch in text:
        rows = FONT[ch]
        for dz, row in enumerate(rows):
            for dx, c in enumerate(row):
                if c == '#':
                    if ground:
                        s.set(x + dx, G, z0 + dz, pick(rnd, block))
                    else:
                        s.set(x + dx, G + 1, z0 + dz, pick(rnd, block))
        x += 4


def ground_patch(s, rnd, cx, cz, r, pal):
    for x in range(cx - r, cx + r + 1):
        for z in range(cz - r, cz + r + 1):
            if (x - cx) ** 2 + (z - cz) ** 2 <= r * r + 0.5 and rnd.random() < 0.8:
                s.set(x, G, z, pick(rnd, pal))


def campfire(s, rnd, x, z, lit=False, ring=True):
    s.set(x, G + 1, z, f'minecraft:campfire[facing=north,lit={str(lit).lower()},signal_fire=false]')
    if ring:
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (-1, -1), (1, -1), (-1, 1)):
            s.set(x + dx, G, z + dz, pick(rnd, [('minecraft:cobblestone', 3), ('minecraft:stone', 1),
                                                ('raft:pumice', 1)]))


def tiki(s, x, z, lit=True):
    s.set(x, G + 1, z, f'raft:tiki_torch[half=lower,lit={str(lit).lower()}]')
    s.set(x, G + 2, z, f'raft:tiki_torch[half=upper,lit={str(lit).lower()}]')


def lean_to(rnd):
    s = Side('lean_to')
    ground_patch(s, rnd, 4, 4, 4, [('minecraft:sand', 5), ('raft:shell_sand', 2), ('minecraft:coarse_dirt', 1)])
    # frame: two forked posts, a ridge log, a thatched slope down to the back
    for x in (1, 6):
        for y in range(G + 1, G + 4):
            s.set(x, y, 2, 'raft:palm_log[axis=y]')
        s.set(x, G + 4, 2, 'raft:palm_fence')
    for x in range(0, 8):
        s.set(x, G + 4, 2, 'raft:stripped_palm_log[axis=x]')
        s.set(x, G + 3, 1, 'raft:palm_thatch_stairs[facing=south,half=bottom]')
        s.set(x, G + 2, 0, 'raft:palm_thatch_stairs[facing=south,half=bottom]')
        s.set(x, G + 1, -1, 'raft:palm_thatch_slab[type=bottom]')
    for x in range(1, 7):                           # bed of leaves and the gear under the slope
        s.set(x, G + 1, 1, 'raft:palm_thatch_slab[type=bottom]' if x not in (2, 5) else 'minecraft:air')
    s.set(2, G + 1, 1, 'minecraft:green_carpet')
    s.chest(5, G + 1, 1, 'raft:chests/castaway_camp', facing='south', block='raft:wooden_crate')
    s.set(4, G + 1, 1, 'raft:rope_coil')
    s.set(6, G + 1, 3, 'raft:coconut_lantern[hanging=false]')
    # fire with log seats
    campfire(s, rnd, 4, 6)
    s.set(2, G + 1, 6, 'raft:stripped_palm_log[axis=z]')
    s.set(6, G + 1, 7, 'raft:stripped_palm_log[axis=z]')
    s.set(4, G + 1, 8, 'raft:stripped_palm_log[axis=x]')
    s.set(5, G + 1, 5, 'minecraft:barrel[facing=up,open=false]')
    # drying rack: fences with a beam and fish (kelp) hanging
    for z in (4, 7):
        s.set(9, G + 1, z, 'raft:palm_fence')
        s.set(9, G + 2, z, 'raft:palm_fence')
    for z in range(4, 8):
        s.set(9, G + 3, z, 'raft:palm_slab[type=bottom]')
    s.set(9, G + 2, 5, 'minecraft:dried_kelp_block')
    # signal pyre (unlit) and SOS in stones on the beach
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            s.set(13 + dx, G + 1, 3 + dz, 'raft:driftwood_log[axis=x]' if dz else 'raft:driftwood_log[axis=z]')
    s.set(13, G + 2, 3, 'raft:driftwood_log[axis=y]')
    s.set(13, G + 3, 3, 'minecraft:dead_bush')
    word(s, rnd, 'SOS', 1, 11, [('minecraft:cobblestone', 3), ('minecraft:stone', 2), ('raft:pumice', 2),
                                ('minecraft:mossy_cobblestone', 1)])
    tiki(s, 0, 6, lit=False)
    return s


def tent(rnd):
    s = Side('tent')
    ground_patch(s, rnd, 5, 5, 5, [('minecraft:sand', 6), ('raft:shell_sand', 2)])
    # canvas A-frame tent (stepped profile, 7 wide, 4 high), 5 long along z, door in the front gable (z=0)
    prof = {0: (1,), 1: (1, 2), 2: (2, 3), 3: (3, 4), 4: (2, 3), 5: (1, 2), 6: (1,)}
    inner = {(2, 1), (3, 1), (3, 2), (4, 1)}
    for z in range(0, 7):
        gable = z in (0, 6)
        for x, ys in prof.items():
            for y in ys:
                st = 'raft:sail_canvas' if (x + y + z) % 4 else 'minecraft:white_wool'
                if x in (0, 6) and z % 2 == 0:
                    st = 'raft:sail_canvas_red_striped'
                s.set(x - 1, G + y, z, st)
        for (x, y) in inner:
            s.set(x - 1, G + y, z, ('raft:sail_canvas' if (x, y) != (3, 1) and (x, y) != (3, 2) else 'minecraft:air')
                  if gable and z == 6 else ('minecraft:air' if not gable or (x, y) in ((3, 1), (3, 2)) else
                                            'raft:sail_canvas'))
    for z in (-1, 7):                                   # tent poles and guy ropes
        s.set(2, G + 1, z, 'raft:palm_fence')
        s.set(2, G + 2, z, 'raft:palm_fence')
        s.set(2, G + 3, z, 'raft:palm_fence')
    s.set(1, G + 1, 4, 'raft:cushion_blue')
    s.chest(3, G + 1, 4, 'raft:chests/castaway_camp', facing='west', block='raft:wooden_crate')
    s.set(2, G + 1, 5, 'raft:driftwood_lamp')
    for z in range(1, 4):
        s.set(2, G + 1, z, 'raft:woven_rug')
    # hammock between two posts
    for x in (7, 10):
        for y in range(G + 1, G + 4):
            s.set(x, y, 3, 'raft:palm_log[axis=y]')
        s.set(x, G + 4, 3, 'raft:palm_leaves[distance=1,persistent=true,waterlogged=false]')
    s.set(8, G + 1, 3, 'raft:hammock[facing=east,occupied=false,part=foot]')
    s.set(9, G + 1, 3, 'raft:hammock[facing=east,occupied=false,part=head]')
    # rain cauldron, fire, tiki torches
    s.set(6, G + 1, 7, 'minecraft:water_cauldron[level=3]')
    campfire(s, rnd, 4, 8, lit=True)
    s.set(5, G + 1, 9, 'raft:simple_grill[lit=false]')
    tiki(s, 0, 7)
    tiki(s, 8, 7)
    s.set(10, G + 1, 6, 'minecraft:barrel[facing=up,open=false]')
    s.set(10, G + 2, 6, 'raft:coconut_lantern[hanging=false]')
    word(s, rnd, 'HELP', -1, 12, [('minecraft:stripped_spruce_log[axis=x]', 3), ('minecraft:spruce_log[axis=x]', 1)],
         ground=False)
    return s


def boat_shelter(rnd):
    s = Side('boat_shelter')
    ground_patch(s, rnd, 4, 5, 5, [('minecraft:sand', 6), ('minecraft:gravel', 1), ('raft:shell_sand', 2)])
    # upturned lifeboat as a roof, 9 long, propped on log stacks
    L = 9
    for z in range(L):
        t = z / (L - 1)
        hw = 2 if 0.15 < t < 0.85 else 1
        for x in range(-hw, hw + 1):
            y = G + 3 + (1 if abs(x) < hw else 0)
            s.set(4 + x, y + (1 if abs(x) == 0 else 0), z,
                  'minecraft:orange_terracotta' if abs(x) == hw else 'raft:weathered_planks')
        s.set(4 - hw - 1, G + 3, z, 'minecraft:white_terracotta') if hw == 2 else None
        s.set(4 + hw + 1, G + 3, z, 'minecraft:white_terracotta') if hw == 2 else None
        for x in range(-hw + 1, hw):                  # closed hull: one piece, no diagonal-only steps
            s.set(4 + x, G + 3, z, 'raft:weathered_planks')
        s.set(4, G + 4, z, 'raft:weathered_planks')
    for x in (1, 7):
        for z in (2, 6):                              # the props stand right under the gunwales
            s.set(x, G + 1, z, 'raft:driftwood_log[axis=y]')
            s.set(x, G + 2, z, 'raft:driftwood_log[axis=y]')
    for x in range(1, 8):
        for y in (G + 1, G + 2):
            if s.get(x, y, 1) is None:
                s.set(x, y, 1, 'minecraft:air')
    # back wall of stacked crates/sail, sleeping place
    for x in range(2, 7):
        s.set(x, G + 1, 0, 'raft:wooden_crate[facing=south,open=false]' if x % 2 else 'minecraft:barrel[facing=south,open=false]')
        s.set(x, G + 2, 0, 'raft:sail_canvas')
    s.chest(3, G + 1, 0, 'raft:chests/castaway_camp', facing='south', block='raft:wooden_crate')
    s.set(5, G + 1, 3, 'minecraft:light_blue_bed[facing=north,occupied=false,part=foot]')
    s.set(5, G + 1, 2, 'minecraft:light_blue_bed[facing=north,occupied=false,part=head]')
    s.set(2, G + 1, 4, 'raft:driftwood_chair[facing=east]')
    s.set(3, G + 1, 4, 'raft:driftwood_table')
    s.set(3, G + 2, 4, 'raft:coconut_lantern[hanging=false]')
    # stone fire ring, tally post, paddle stuck in the sand (fence)
    campfire(s, rnd, 4, 10)
    for y in range(G + 1, G + 4):
        s.set(9, y, 4, 'raft:stripped_driftwood_log[axis=y]')
    s.set(9, G + 2, 3, 'minecraft:spruce_button[face=wall,facing=north,powered=false]')
    s.set(9, G + 3, 3, 'minecraft:spruce_button[face=wall,facing=north,powered=false]')
    s.set(10, G + 1, 7, 'raft:palm_fence')
    s.set(10, G + 2, 7, 'raft:palm_trapdoor[facing=west,half=bottom,open=true]')
    word(s, rnd, 'SOS', -2, 13, [('minecraft:calcite', 4), ('minecraft:diorite', 1), ('minecraft:bone_block', 1)])
    for (x, z) in ((-3, 12), (2, 19), (9, 14), (11, 18)):
        s.set(x, G + 1, z, 'raft:seashells[shells=3]')
    s.set(-1, G + 1, 4, 'raft:item_catcher[has_net=false]')
    return s


BUILDERS = [lean_to, tent, boat_shelter]


def main(preview=False):
    out = []
    for v, fn in enumerate(BUILDERS):
        rnd = random.Random(31415908 + v)
        s = fn(rnd)
        path = f'castaway_camp/{fn.__name__}'
        save(s, path, anchored=True)
        out.append((path, s))
        if preview:
            print('  preview', contact(s, f'castaway_camp_{fn.__name__}', scale=8))
    write_structure('castaway_camp', [(p, 1) for p, _ in out], None,
                    step='surface_structures', terrain='beard_thin', start_height=-1, heightmap='WORLD_SURFACE_WG',
                    biomes=['palm_isle'], spacing=16, separation=6, salt=31415908, max_distance=32)
    return out
