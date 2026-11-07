"""sunken_ruins - small Tidewarden ruins on the sea floor, five shapes:
 0 colonnade: ring of fluted tidestone columns on a stepped platform, a lintel still bridging two, one fallen
 1 arch: a tall gate arch with a glowing keystone, broken wall stubs and a scatter of blocks
 2 head: a giant toppled statue head with glowing eyes and a crown, half buried, a stone hand nearby
 3 stairs: a grand stair to nowhere with balustrades and two sentinel posts, ending in broken air
 4 shrine: a small domed shrine on four pillars, partly collapsed, a sigil pedestal with offerings inside
Template y=1 is the sea floor (start height -2 under OCEAN_FLOOR_WG), y=0 is buried foundation.
"""
import math
import random

from .lib import Side, NO_SPILL_PLANTS, contact, overgrow, pick, save, wear, write_structure

BR = [('raft:tidestone_bricks', 6), ('raft:cracked_tidestone_bricks', 2), ('raft:mossy_tidestone_bricks', 2)]
STONE = [('raft:tidestone', 5), ('raft:tidestone_bricks', 3), ('raft:mossy_tidestone_bricks', 2)]
PAVE = [('raft:tidestone_bricks', 4), ('raft:cracked_tidestone_bricks', 2), ('raft:mossy_tidestone_bricks', 2),
        ('raft:coral_stone_bricks', 2), ('minecraft:sand', 1), ('minecraft:gravel', 1)]
CORAL = ['tube', 'brain', 'bubble', 'fire', 'horn']
F = 1   # floor level in the template


def coral_growth(s, rnd, n, box):
    """Clusters of coral blocks with fans and plants on the ruins."""
    (x0, y0, z0), (x1, y1, z1) = box
    for _ in range(n):
        x, y, z = rnd.randint(x0, x1), rnd.randint(y0, y1), rnd.randint(z0, z1)
        if not s.free(x, y, z) or not (s.is_solid(x, y - 1, z) or s.is_solid(x - 1, y, z) or s.is_solid(x + 1, y, z)):
            continue
        c = rnd.choice(CORAL)
        s.set(x, y, z, f'minecraft:{c}_coral_block')
        for dx, dz, f in ((1, 0, 'east'), (-1, 0, 'west'), (0, 1, 'south'), (0, -1, 'north')):
            if s.free(x + dx, y, z + dz) and rnd.random() < 0.5:
                s.set(x + dx, y, z + dz, f'minecraft:{c}_coral_wall_fan[facing={f}]')
        if s.free(x, y + 1, z):
            s.set(x, y + 1, z, f'minecraft:{rnd.choice(CORAL)}_coral' if rnd.random() < 0.6
                  else f'minecraft:{rnd.choice(CORAL)}_coral_fan')


def paving(s, rnd, cx, cz, r, missing=0.25):
    for x in range(cx - r, cx + r + 1):
        for z in range(cz - r, cz + r + 1):
            d = math.hypot(x - cx, z - cz)
            if d <= r + 0.3 and rnd.random() > missing * (d / r):
                s.set(x, 0, z, pick(rnd, STONE))
                s.set(x, F, z, pick(rnd, PAVE))


def column(s, rnd, x, z, y0, h, cap=True):
    """Fluted column: base block, pillar shaft, chiseled capital."""
    s.set(x, y0, z, 'raft:chiseled_tidestone')
    for y in range(y0 + 1, y0 + h):
        s.set(x, y, z, 'raft:tidestone_pillar[axis=y]')
    if cap:
        s.set(x, y0 + h, z, 'raft:chiseled_tidestone')


def colonnade(rnd):
    s = Side('colonnade')
    paving(s, rnd, 0, 0, 7)
    for x in range(-5, 6):                      # stepped platform
        for z in range(-5, 6):
            if math.hypot(x, z) <= 5.3:
                s.set(x, F + 1, z, pick(rnd, BR))
                if math.hypot(x, z) > 4.3:
                    s.set(x, F + 1, z, 'raft:tidestone_brick_slab[type=bottom]')
    heights = [9, 9, 4, 7, 2, 9, 6]
    pts = []
    for i in range(7):
        a = i / 7 * 2 * math.pi + 0.3
        x, z = round(4 * math.cos(a)), round(4 * math.sin(a))
        pts.append((x, z))
        column(s, rnd, x, z, F + 2, heights[i], cap=heights[i] >= 9)
        if heights[i] < 9:
            s.set(x, F + 2 + heights[i], z, 'raft:tidestone_brick_slab[type=bottom]')
    # lintel between the two full columns 0 and 1
    (ax, az), (bx, bz) = pts[0], pts[1]
    s.line((ax, F + 12, az), (bx, F + 12, bz), 'raft:tidestone_bricks')
    s.set((ax + bx) // 2, F + 12, (az + bz) // 2, 'raft:glowing_tidestone')
    # fallen column on the floor (lies along x)
    for i in range(7):
        s.set(7 + (i > 3), F + 1, -2 + i, 'raft:tidestone_pillar[axis=z]')
    s.set(8, F + 1, 5, 'raft:chiseled_tidestone')
    # altar stone in the middle with the chest
    s.set(0, F + 2, 0, 'raft:chiseled_tidestone')
    s.set(0, F + 3, 0, 'raft:sigil_pedestal')
    s.chest(1, F + 2, 0, 'raft:chests/sunken_ruins', facing='east')
    s.set(-1, F + 2, 0, 'raft:glowing_tidestone')
    return s


def arch(rnd):
    s = Side('arch')
    paving(s, rnd, 0, 0, 6, missing=0.35)
    w, h = 4, 10          # half width, height of the opening
    for x in range(-w - 1, w + 2):
        for y in range(F + 1, F + h + 3):
            inner = abs(x) <= w - 1 and (y - F - 1) < (h - 3) + math.sqrt(max(0, (w - 0.5) ** 2 - x ** 2)) * 0.9
            outer = abs(x) <= w + 1 and (y - F - 1) < (h - 2) + math.sqrt(max(0, (w + 1.5) ** 2 - x ** 2)) * 0.9
            if outer and not inner:
                for z in (0, 1):
                    s.set(x, y, z, pick(rnd, BR))
    # pillar jambs with a chiseled rune band, plinths
    for (x, y, z), b in list(s.blocks.items()):
        if abs(x) in (w, w + 1) and F + 1 < y < F + h - 2:
            s.set(x, y, z, 'raft:chiseled_tidestone' if y in (F + 5, F + 6) and abs(x) == w
                  else 'raft:tidestone_pillar[axis=y]')
    for x in (-w - 2, w + 2):
        for z in (-1, 0, 1, 2):
            s.set(x, F + 1, z, 'raft:tidestone_brick_slab[type=bottom]')
    # keystone + rune band
    top_y = max(y for (x, y, z) in s.blocks if x == 0)
    s.set(0, top_y - 1, 0, 'raft:glowing_tidestone')
    s.set(0, top_y - 1, 1, 'raft:glowing_tidestone')
    for x in (-w - 1, w + 1):
        for z in (0, 1):
            s.set(x, F + 1, z, 'raft:chiseled_tidestone')
    # crumble the top on one side
    for (x, y, z) in list(s.blocks):
        if x > 1 and y > top_y - 3 and rnd.random() < 0.6:
            s.set(x, y, z, None)
    # broken walls left and right
    for side in (-1, 1):
        for i in range(2, 7):
            hh = max(1, 5 - i + rnd.randint(-1, 1))
            for y in range(F + 1, F + 1 + hh):
                s.set(side * (w + i), y, 0, pick(rnd, BR))
        s.set(side * (w + 2), F + 1, 1, 'raft:tidestone_brick_stairs[facing=north,half=bottom]')
    for _ in range(9):
        x, z = rnd.randint(-7, 7), rnd.randint(-5, 6)
        if s.get(x, F + 1, z) is None:
            s.set(x, F + 1, z, pick(rnd, BR + [('raft:tidestone_brick_slab[type=bottom]', 3)]))
    s.chest(-2, F + 1, 3, 'raft:chests/sunken_ruins', facing='south')
    s.set(-3, F + 1, 3, 'raft:tidestone_brick_slab[type=bottom]')
    s.set(2, F + 1, -2, 'raft:sigil_pedestal')
    return s


def head(rnd):
    """Toppled colossal Tidewarden head (moai-like long face), leaning back, half buried, eyes still glowing."""
    from .lib import resample
    h = Side('h')
    for x in range(-3, 4):
        for z in range(-3, 3):
            for y in range(0, 11):
                w = 3 if y >= 2 else 2                       # narrower jaw/chin at the bottom
                if abs(x) > w or (y >= 9 and abs(x) == 3 and z in (-3, 2)):
                    continue
                h.set(x, y, z, pick(rnd, [('raft:tidestone', 5), ('raft:tidestone_bricks', 2),
                                          ('raft:mossy_tidestone_bricks', 2)]))
    f = 3                                                    # face plane z = 3 (features stick out)
    for x in range(-3, 4):
        h.set(x, 7, f, 'raft:tidestone_brick_stairs[facing=north,half=top]')          # heavy brow
    for ex in (-2, -1, 1, 2):
        h.set(ex, 6, f - 1, 'raft:glowing_tidestone' if abs(ex) == 2 else 'raft:tidestone')
    for y in range(3, 7):
        h.set(0, y, f, 'raft:tidestone' if y > 3 else 'raft:tidestone_stairs[facing=north,half=bottom]')   # nose
    for x in (-1, 1):
        h.set(x, 3, f, 'raft:tidestone_slab[type=bottom]')
    for x in (-1, 0, 1):
        h.set(x, 2, f, 'raft:tidestone_brick_stairs[facing=north,half=top]')          # lips
    h.set(0, 1, f - 1, 'raft:tidestone')
    for side in (-1, 1):                                     # long ears
        for y in range(3, 8):
            h.set(side * 4, y, 0, 'raft:tidestone_pillar[axis=y]')
        h.set(side * 4, 2, 0, 'raft:glowing_tidestone')
    for x in range(-3, 4):                                   # crown band with spikes
        for z in range(-3, 3):
            if abs(x) == 3 or z in (-3, 2):
                h.set(x, 10, z, 'raft:chiseled_tidestone')
                if (x + z) % 2 == 0:
                    h.set(x, 11, z, 'raft:tidestone_brick_wall')
    h.set(0, 11, 2, 'raft:glowing_tidestone')
    h.set(0, 12, 2, 'raft:tidestone_brick_wall')
    t = resample(h, roll=12, pitch=-28, pivot=(0, 0, 0))     # tipped over backwards, face to the sky
    s = Side('head')
    paving(s, rnd, 0, 0, 7, missing=0.5)
    (x0, y0, z0), _ = t.bounds()
    for (x, y, z), b in t.blocks.items():
        s.blocks[(x, y - y0 - 1, z - 1)] = b                 # sunk one block into the floor
    s.chest(-5, F + 1, 4, 'raft:chests/sunken_ruins', facing='east')
    s.set(-5, F + 1, 5, 'raft:tidestone_brick_slab[type=bottom]')
    # broken neck stump and a hand nearby, palm up
    for x in range(-2, 3):
        for z in range(-6, -3):
            s.set(x, F + 1, z, pick(rnd, STONE))
    hx, hz = 5, -6
    for dx in range(3):
        for dz in range(2):
            s.set(hx + dx, F + 1, hz + dz, 'raft:tidestone')
    for dx in range(3):
        s.set(hx + dx, F + 2, hz + 1, 'raft:tidestone_pillar[axis=y]')
        s.set(hx + dx, F + 3, hz + 1, 'raft:tidestone_brick_slab[type=bottom]')
    s.set(hx - 1, F + 2, hz, 'raft:tidestone_pillar[axis=y]')
    s.set(hx + 1, F + 2, hz, 'minecraft:sea_pickle[pickles=3]')
    return s


def stairs(rnd):
    s = Side('stairs')
    paving(s, rnd, 0, 4, 6, missing=0.4)
    W = 3
    for i in range(7):                              # rising toward -z
        z = 6 - i
        for x in range(-W, W + 1):
            for y in range(F, F + 1 + i):
                s.set(x, y, z, pick(rnd, BR))
            s.set(x, F + 1 + i, z, 'raft:tidestone_brick_stairs[facing=north,half=bottom]')
        for x in (-W - 1, W + 1):                  # balustrade
            for y in range(F, F + 2 + i):
                s.set(x, y, z, pick(rnd, BR))
            s.set(x, F + 2 + i, z, 'raft:tidestone_brick_wall')
    # landing and sentinel posts, then broken edge
    for z in (-1, -2, -3):
        for x in range(-W - 1, W + 2):
            for y in range(F, F + 8):
                s.set(x, y, z, pick(rnd, BR))
    for x in (-W - 1, W + 1):
        column(s, rnd, x, -2, F + 8, 5)
        s.set(x, F + 14, -2, 'raft:glowing_tidestone')
    for x in range(-W - 1, W + 2):
        if rnd.random() < 0.6:
            s.set(x, F + 7, -4, pick(rnd, BR))
        if rnd.random() < 0.3:
            s.set(x, F + 6, -5, pick(rnd, BR))
    for (x, y, z) in list(s.blocks):          # erosion of the stair edges
        b = s.get(x, y, z)
        if b and 'stairs' in b and abs(x) == W and rnd.random() < 0.35:
            s.set(x, y, z, None)
    s.chest(0, F + 8, -2, 'raft:chests/sunken_ruins', facing='south')
    s.set(-1, F + 8, -3, 'raft:sigil_pedestal')
    s.set(1, F + 8, -3, 'raft:sigil_pedestal')
    return s


def shrine(rnd):
    s = Side('shrine')
    paving(s, rnd, 0, 0, 6)
    for x in range(-3, 4):
        for z in range(-3, 4):
            s.set(x, F + 1, z, pick(rnd, BR))
    for x, z in ((-3, -3), (3, -3), (-3, 3), (3, 3)):
        column(s, rnd, x, z, F + 2, 5)
    for x in range(-3, 4):                          # architrave ring
        for z in range(-3, 4):
            if abs(x) == 3 or abs(z) == 3:
                s.set(x, F + 7, z, pick(rnd, BR))
    for x in range(-3, 4):                          # dome
        for y in range(F + 8, F + 12):
            for z in range(-3, 4):
                d = math.sqrt(x * x + z * z + ((y - F - 7) * 1.25) ** 2)
                if 2.6 <= d <= 3.7:
                    s.set(x, y, z, pick(rnd, STONE))
    s.set(0, F + 12, 0, 'raft:glowing_tidestone')
    for (x, y, z) in list(s.blocks):               # collapsed corner of the dome
        if x > 0 and z > 0 and y >= F + 7 and rnd.random() < 0.75:
            s.set(x, y, z, None)
    s.set(3, F + 2, 3, 'raft:chiseled_tidestone')
    for (x, z) in ((4, 4), (5, 3), (4, 5), (6, 5)):
        s.set(x, F + 1, z, pick(rnd, STONE))
    s.set(0, F + 2, 0, 'raft:sigil_pedestal')
    s.set(0, F + 3, 0, 'raft:glowing_tidestone')
    s.chest(0, F + 2, -2, 'raft:chests/sunken_ruins', facing='south')
    for (x, z) in ((-1, 1), (1, -1), (-2, 0)):
        s.set(x, F + 2, z, rnd.choice(['raft:seashells[shells=3]', 'raft:sea_glass_scatter[pebbles=2]',
                                       'minecraft:sea_pickle[pickles=2]', 'raft:gold_coin_pile[layers=1]']))
    s.set(0, F + 2, 3, 'raft:tidestone_brick_stairs[facing=north,half=bottom]')
    return s


BUILDERS = [colonnade, arch, head, stairs, shrine]


def main(preview=False):
    out = []
    for v, fn in enumerate(BUILDERS):
        rnd = random.Random(31415907 + v)
        s = fn(rnd)
        (x0, y0, z0), (x1, y1, z1) = s.bounds()
        coral_growth(s, rnd, 14, ((x0, F + 1, z0), (x1, y1, z1)))
        overgrow(s, seed=70 + v, barnacle=0.05, kelp=0.05, kelp_h=(4, 14), grass=0.16, pickles=0.03, glow=0.3,
                 anemone=0.03, starfish=0.03, kelp_max_y=F + 2)
        path = f'sunken_ruins/{fn.__name__}'
        save(s, path, anchored=True, ground_layers=3)
        out.append((path, s))
        if preview:
            print('  preview', contact(s, f'sunken_ruins_{fn.__name__}', scale=6))
    write_structure('sunken_ruins', [(p, 1) for p, _ in out],
                    [NO_SPILL_PLANTS, wear([('raft:tidestone_bricks', 'raft:cracked_tidestone_bricks', 0.08),
                                            ('raft:tidestone_bricks', 'raft:mossy_tidestone_bricks', 0.08)])],
                    step='surface_structures', terrain='beard_thin', start_height=-2, heightmap='OCEAN_FLOOR_WG',
                    biomes=['coral_shallows', 'kelp_forest', 'raft_ocean_biome', 'glowing_lagoon'],
                    spacing=18, separation=7, salt=31415907, max_distance=40,
                    spawn_overrides=None)
    return out
