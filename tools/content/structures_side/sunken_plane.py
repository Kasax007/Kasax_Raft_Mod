"""sunken_plane - a crashed Meridian Air airliner on the sea floor.

Built upright first: round white fuselage with a blue livery stripe and window band, nose with cockpit glazing,
cabin with seat rows and an aisle, overhead lights, cockpit with control panels, cargo hold under the floor,
swept wings with two engines, tail fin and stabilisers. Then it crashes: the tail section tears off and lies
twisted behind the plane, the starboard wing snaps and its outer half lies on the sand, the fuselage tilts and digs
its nose into the floor (silt). Flooded, with barnacles, kelp and suitcases scattered on the sand.
Upright model: x across (0 = centre), y up (fuselage centre y=4), z from tail (0) to nose.
"""
import math
import random

from .lib import Side, NO_SPILL_PLANTS, contact, overgrow, pick, resample, save, silt, write_structure

L = 32
CY = 4            # fuselage centre
R = 3.3
FLOOR = CY - 1    # cabin floor
SKIN = [('minecraft:white_concrete', 6), ('minecraft:quartz_block', 2), ('minecraft:smooth_quartz', 1),
        ('minecraft:white_terracotta', 1)]
BELLY = [('minecraft:light_gray_concrete', 5), ('minecraft:white_concrete', 2), ('raft:rusty_hull', 1)]


def radius(z):
    if z >= L - 5:                      # nose cone
        t = (z - (L - 5)) / 5
        return R * math.sqrt(max(0.0, 1 - t ** 2.2)) + 0.4
    if z < 7:                           # tail cone (rises)
        return 1.6 + (R - 1.6) * z / 7
    return R


def centre(z):
    return CY + (max(0, 7 - z) * 0.35 if z < 7 else 0)


def inside(x, y, z):
    if z < 0 or z >= L:
        return False
    return math.hypot(x, (y - centre(z)) * 1.05) <= radius(z) + 0.2


def build_upright(rnd):
    s = Side('plane')
    for z in range(L):
        for y in range(-1, 11):
            for x in range(-5, 6):
                if not inside(x, y, z):
                    continue
                shell = any(not inside(x + dx, y + dy, z + dz) for dx, dy, dz in (
                    (1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)))
                if shell:
                    yy = y - centre(z)
                    if 0.5 <= yy <= 1.5 and abs(x) >= 2 and 8 <= z <= L - 7:
                        st = pick(rnd, [('minecraft:glass_pane', 2), ('raft:cracked_glass_pane', 2)]) if z % 2 else \
                            'minecraft:white_concrete'
                    elif -0.5 <= yy < 0.5 and abs(x) >= 2:
                        st = 'minecraft:blue_concrete' if rnd.random() < 0.9 else 'minecraft:light_blue_concrete'
                    elif yy < -1.5:
                        st = pick(rnd, BELLY)
                    else:
                        st = pick(rnd, SKIN)
                    s.set(x, y, z, st)
                elif y == FLOOR and 6 < z < L - 2:
                    s.set(x, y, z, 'minecraft:smooth_stone_slab[type=top]')
                elif y < FLOOR and 6 < z < L - 3:
                    s.set(x, y, z, 'minecraft:water')          # cargo hold
                else:
                    s.set(x, y, z, 'minecraft:water')
    # cockpit glazing on the nose
    for z in range(L - 4, L - 1):
        for x in range(-2, 3):
            for y in range(CY + 1, CY + 3):
                b = s.get(x, y, z)
                if b and 'concrete' in b and inside(x, y, z) and not inside(x, y + 1, z):
                    s.set(x, y, z, pick(rnd, [('minecraft:black_stained_glass_pane', 2), ('raft:cracked_glass_pane', 1)]))
    # seats (2 + aisle + 2), facing the nose; overhead lights
    for z in range(8, L - 7, 2):
        for x in (-2, -1, 1, 2):
            if rnd.random() < 0.9:
                s.set(x, FLOOR + 1, z, 'minecraft:dark_prismarine_stairs[facing=north,half=bottom]')
        if z % 6 == 0:
            s.set(0, CY + 2, z, 'minecraft:sea_lantern' if rnd.random() < 0.5 else 'minecraft:glowstone')
    for z in range(8, L - 7, 4):
        s.set(rnd.choice((-2, 2)), FLOOR + 1, z + 1, pick(rnd, [('minecraft:brown_wool', 1), ('minecraft:blue_wool', 1),
                                                               ('minecraft:air', 2)]))
    # galley + bulkhead behind the cockpit
    zb = L - 6
    for x in range(-3, 4):
        for y in range(FLOOR + 1, CY + 3):
            if inside(x, y, zb) and abs(x) != 0:
                s.set(x, y, zb, 'minecraft:light_gray_concrete')
    s.set(0, FLOOR + 1, zb, 'minecraft:water')
    s.set(0, FLOOR + 2, zb, 'minecraft:water')
    s.set(-2, FLOOR + 1, zb - 1, 'minecraft:barrel[facing=up,open=false]')
    s.chest(2, FLOOR + 1, zb - 1, 'raft:chests/sunken_plane', facing='west')
    # cockpit: two panels, two seats
    s.set(-1, FLOOR + 1, L - 3, 'raft:control_panel[facing=north]')
    s.set(1, FLOOR + 1, L - 3, 'raft:control_panel[facing=north]')
    s.set(-1, FLOOR + 1, L - 4, 'minecraft:dark_prismarine_stairs[facing=north,half=bottom]')
    s.set(1, FLOOR + 1, L - 4, 'minecraft:dark_prismarine_stairs[facing=north,half=bottom]')
    s.set(0, FLOOR + 1, L - 3, 'raft:expedition_radio[facing=north]')
    # cargo hold: suitcases (wool), crates, a chest
    for z in range(9, L - 8):
        for x in (-2, -1, 1, 2):
            if inside(x, FLOOR - 1, z) and rnd.random() < 0.35:
                s.set(x, FLOOR - 1, z, pick(rnd, [('minecraft:brown_wool', 2), ('minecraft:black_wool', 1),
                                                  ('minecraft:red_wool', 1), ('raft:wooden_crate[facing=north,open=false]', 2)]))
    s.chest(0, FLOOR - 1, 14, 'raft:chests/sunken_plane', facing='south', block='raft:wooden_crate')
    # wings (port intact, starboard snapped later), engines
    for side in (-1, 1):
        for i in range(0, 15):
            x = side * (3 + i)
            sweep = int(i * 0.45)
            chord = max(2, 7 - i // 2)
            for dz in range(chord):
                z = 12 + sweep + dz
                st = 'minecraft:light_gray_concrete' if dz in (0, chord - 1) else pick(
                    rnd, [('minecraft:white_concrete', 4), ('minecraft:iron_block', 1)])
                s.set(x, CY - 2, z, st)
            if i == 14:
                s.set(x, CY - 1, 12 + sweep + 1, 'minecraft:red_concrete')       # wing tip light
        ex = side * 8
        for z in range(13, 19):
            for dy, dx in ((0, -1), (0, 1), (-1, 0), (1, 0), (0, 0)):
                st = 'minecraft:gray_concrete' if (dy, dx) != (0, 0) else ('minecraft:black_concrete' if z > 13 else
                                                                         'minecraft:iron_block')
                s.set(ex + dx, CY - 4 + dy, z, st)
        s.set(ex, CY - 3, 16, 'minecraft:light_gray_concrete')
    # tail: fin + stabilisers
    for z in range(0, 7):
        top = 6 - z // 2
        for y in range(1, top + 1):
            s.set(0, round(centre(z) + radius(z)) + y, z, 'minecraft:white_concrete' if y < top else 'minecraft:blue_concrete')
    for side in (-1, 1):
        for i in range(1, 6):
            for z in range(1, 4 - i // 3):
                s.set(side * (1 + i), round(centre(z)) + 1, z + i // 2, 'minecraft:white_concrete')
    return s


def build(v=0):
    rnd = random.Random(31415906)
    up = build_upright(rnd)
    # split: tail section (z < 6), starboard outer wing (x > 9), main body
    tail, wing, body = Side('t'), Side('w'), Side('b')
    for (x, y, z), b in up.blocks.items():
        if z < 6 + (x + y) % 2:
            tail.blocks[(x, y, z)] = b
        elif x > 9 + (z % 2) and y <= CY - 1:
            wing.blocks[(x, y, z)] = b
        else:
            body.blocks[(x, y, z)] = b
    body = resample(body, roll=-14, pitch=6, pivot=(0, 0, L // 2))
    tail = resample(tail, roll=60, pitch=-10, yaw=35, pivot=(0, CY, 3))
    wing = resample(wing, roll=-8, yaw=-20, pivot=(12, CY, 15))
    s = Side('sunken_plane', seed=6)
    (bx0, by0, bz0), (bx1, _, bz1) = body.bounds()
    s.paste(body, -bx0, -by0, -bz0 + 8)
    (tx0, ty0, tz0), (tx1, _, tz1) = tail.bounds()
    s.paste(tail, -tx0 + 2, -ty0, -tz0)
    (wx0, wy0, wz0), _ = wing.bounds()
    s.paste(wing, -wx0 + (bx1 - bx0) + 3, -wy0 + 1, -wz0 + 18)
    # debris trail and suitcases on the sand
    (x0, y0, z0), (x1, y1, z1) = s.bounds()
    floor = 2
    silt(s, floor, depth=1, chance=0.45, seed=7)
    for i in range(12):
        x, z = rnd.randint(x0, x1), rnd.randint(z0, z0 + 14)
        if s.get(x, floor + 1, z) is None:
            s.set(x, floor + 1, z, pick(rnd, [('minecraft:brown_wool', 2), ('minecraft:blue_wool', 1),
                                              ('minecraft:white_concrete', 2), ('minecraft:light_gray_concrete', 1),
                                              ('raft:wooden_crate[facing=east,open=false]', 1)]))
    s.chest(x1 - 2, floor + 1, z0 + 4, 'raft:chests/sunken_plane', facing='west', block='minecraft:barrel')
    overgrow(s, seed=17, barnacle=0.07, kelp=0.04, kelp_h=(4, 12), grass=0.12, pickles=0.03, anemone=0.01,
             starfish=0.01, kelp_max_y=floor + 2)
    return s


def seaplane():
    """Expedition float plane: high wing, two pontoons, nose dug into the sand, tail in the air."""
    rnd = random.Random(31415916)
    s = Side('seaplane')
    WH = [('minecraft:white_concrete', 6), ('minecraft:white_terracotta', 1)]
    for z in range(0, 14):
        w = 1 if z >= 3 else 0
        for x in range(-w, w + 1):
            for y in range(0, 3 if z >= 3 else 2):
                if z < 3:
                    y += 1
                edge = abs(x) == w or y in (0, 2) or z in (0, 13)
                if edge:
                    st = 'minecraft:red_concrete' if y == 1 and abs(x) == w and z >= 3 else pick(rnd, WH)
                    s.set(x, y, z, st)
                else:
                    s.set(x, y, z, 'minecraft:water')
    for z in (9, 10):                                   # cockpit glazing
        for x in (-1, 0, 1):
            s.set(x, 2, z, pick(rnd, [('minecraft:glass_pane', 1), ('raft:cracked_glass_pane', 1)]))
        s.set(-1, 1, z, 'minecraft:glass_pane')
        s.set(1, 1, z, 'raft:cracked_glass_pane')
    s.set(0, 1, 11, 'raft:control_panel[facing=south]')
    s.set(0, 1, 9, 'minecraft:dark_prismarine_stairs[facing=north,half=bottom]')
    s.chest(0, 1, 5, 'raft:chests/sunken_plane', facing='south', block='raft:wooden_crate')
    s.set(0, 1, 6, 'minecraft:white_wool')
    s.set(0, 1, 4, 'minecraft:white_wool')
    for dx, dy in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):          # engine + propeller
        s.set(dx, 1 + dy, 14, 'minecraft:iron_block' if (dx, dy) == (0, 0) else 'minecraft:gray_concrete')
    s.set(0, 1, 15, 'minecraft:iron_block')
    for dx, dy in ((0, 1), (0, 2), (0, -1), (0, -2), (1, 0), (-2, 0)):
        s.set(dx, 1 + dy, 16, 'minecraft:iron_trapdoor[facing=north,half=bottom,open=true]')
    for x in range(-9, 10):                             # high wing with red tips
        for z in (8, 9, 10):
            s.set(x, 3, z, 'minecraft:red_concrete' if abs(x) >= 8 else pick(rnd, WH + [('minecraft:light_gray_concrete', 2)]))
    for side in (-1, 1):                                # struts and pontoons
        s.line((side * 4, 2, 9), (side * 3, -2, 8), 'raft:steel_beam[axis=y]')
        s.line((side * 2, -1, 4), (side * 3, -2, 4), 'raft:steel_beam[axis=y]')
        for z in range(1, 13):
            s.set(side * 3, -3, z, 'minecraft:red_concrete' if z < 12 else 'minecraft:white_concrete')
            s.set(side * 3, -4, z, 'minecraft:white_concrete' if 1 < z < 12 else None)
    for y in range(3, 7):                               # tail fin and stabilisers
        s.set(0, y, 0 + (y - 3) // 2, 'minecraft:red_concrete' if y > 4 else 'minecraft:white_concrete')
    for x in range(-4, 5):
        s.set(x, 2, 0, 'minecraft:white_concrete' if abs(x) < 4 else 'minecraft:red_concrete')
    # snap one pontoon off, crash nose down
    s = resample(s, roll=12, pitch=34, pivot=(0, 0, 12))
    (x0, y0, z0), _ = s.bounds()
    t = Side('seaplane', seed=3)
    for (x, y, z), b in s.blocks.items():
        t.blocks[(x - x0, y - y0, z - z0)] = b
    s = t
    silt(s, 1, depth=1, chance=0.6, seed=8)
    (x0, y0, z0), (x1, y1, z1) = s.bounds()
    for i in range(5):
        s.set(rnd.randint(x0, x1), 2, rnd.randint(z0 - 3, z0 + 3), pick(rnd, [('minecraft:white_wool', 2),
                                                                           ('raft:wooden_crate[facing=north,open=false]', 1)]))
    overgrow(s, seed=19, barnacle=0.07, kelp=0.05, kelp_h=(4, 12), grass=0.12, pickles=0.03, kelp_max_y=2)
    return s


def main(preview=False):
    out = []
    for path, s in (('sunken_plane/airliner_0', build()), ('sunken_plane/seaplane_1', seaplane())):
        save(s, path, floor_y=2)
        out.append((path, s))
        if preview:
            print('  preview', contact(s, path.replace('/', '_'), cuts=(6,), scale=6))
    write_structure('sunken_plane', [(p, 1) for p, _ in out], [NO_SPILL_PLANTS],
                    step='surface_structures', terrain='beard_thin', start_height=-3, heightmap='OCEAN_FLOOR_WG',
                    biomes=['kelp_forest', 'raft_ocean_biome', 'coral_shallows'], spacing=28, separation=10,
                    salt=31415906, max_distance=64)
    return out
