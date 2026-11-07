"""Ashen Forge – Tidewarden forge temple built into a volcanic slope (story chapter 6, Ember Sigil).

Pieces (jigsaw, rigid, terrain_adaptation beard_thin, start projected to WORLD_SURFACE_WG):
  ashen_forge/hall   start: forecourt with lava basins and guardian statues, monumental tidestone/basalt
                     facade with lava windows and a glowing emblem, the great hall with lava channels and
                     colonnades, the giant anvil and forge hearth (chimney with smoke), storeroom, tablets.
                     Wrapped in its own volcanic rock mass so it reads as carved into the mountain.
  ashen_forge/vault  behind the hall: rune corridor into a cavern with a lava moat; a narrow bridge leads to the
                     sealed vault (story/ashen_vault) on an island; storage niche (ashen_storage).
Template y=0 of the hall is the ground layer; the hall floor is at y=4.
"""
import math

from .lib import B, Pal, stairs, slab, log, rnd, noise2, look, validate, DIRS, OPP, MODULE

TB = Pal(('raft:tidestone_bricks', 62), ('raft:cracked_tidestone_bricks', 22), ('raft:tidestone', 16), seed='af_tb')
BAS = Pal(('minecraft:basalt[axis=y]', 26), ('minecraft:polished_basalt[axis=y]', 14), ('minecraft:blackstone', 24),
          ('minecraft:polished_blackstone_bricks', 18), ('minecraft:cracked_polished_blackstone_bricks', 12),
          ('minecraft:smooth_basalt', 6), seed='af_bas')
BRICK = Pal(('minecraft:polished_blackstone_bricks', 64), ('minecraft:cracked_polished_blackstone_bricks', 22),
            ('minecraft:polished_blackstone', 14), seed='af_br')
ROCK = Pal(('minecraft:basalt[axis=y]', 24), ('minecraft:blackstone', 20), ('minecraft:tuff', 14), ('raft:pumice', 8),
           ('minecraft:smooth_basalt', 16), ('minecraft:cobbled_deepslate', 12), ('minecraft:magma_block', 1),
           ('minecraft:deepslate', 3), seed='af_rock')
TOP = Pal(('raft:volcanic_ash', 34), ('raft:black_sand', 18), ('minecraft:basalt[axis=y]', 18), ('raft:pumice', 10),
          ('minecraft:tuff', 10), ('minecraft:magma_block', 2), ('minecraft:smooth_basalt', 5), seed='af_top')
FLOOR = Pal(('minecraft:polished_blackstone_bricks', 58), ('minecraft:cracked_polished_blackstone_bricks', 18),
            ('minecraft:polished_blackstone', 14), ('minecraft:polished_basalt[axis=y]', 10), seed='af_fl')
SOUL = 'minecraft:soul_lantern[hanging=true,waterlogged=false]'
CHAIN = 'minecraft:iron_chain[axis=y,waterlogged=false]'
FY = 4            # hall floor layer


def guardian(s, cx, y0, cz, face='north'):
    """Tidewarden smith statue (~11 high) with a hammer on the shoulder; faces `face` (north/south)."""
    sg = -1 if face == 'north' else 1
    # pedestal
    for x in range(cx - 2, cx + 3):
        for z in range(cz - 2, cz + 3):
            s.set(x, y0, z, 'minecraft:polished_blackstone_bricks')
            if abs(x - cx) <= 1 and abs(z - cz) <= 1:
                s.set(x, y0 + 1, z, 'raft:chiseled_tidestone')
            else:
                s.set(x, y0 + 1, z, stairs('minecraft:polished_blackstone_brick_stairs',
                                           'south' if z < cz - 1 else 'north' if z > cz + 1 else ('east' if x < cx else 'west')))
    s.set(cx, y0 + 1, cz + 2 * sg, 'minecraft:gold_block')
    b = y0 + 2
    # legs + robe
    for x in (cx - 1, cx + 1):
        s.set(x, b, cz, 'raft:tidestone'); s.set(x, b + 1, cz, 'raft:tidestone')
    s.set(cx, b, cz, 'raft:tidestone_bricks'); s.set(cx, b + 1, cz, 'raft:tidestone')
    for x in (cx - 1, cx, cx + 1):
        s.set(x, b, cz + sg, stairs('raft:tidestone_brick_stairs', OPP[face]))
    for x in range(cx - 1, cx + 2):
        s.set(x, b + 2, cz, 'raft:tidestone')
    # belt (gold) and apron
    s.set(cx, b + 3, cz, 'minecraft:gold_block')
    s.set(cx - 1, b + 3, cz, 'raft:tidestone_pillar[axis=x]'); s.set(cx + 1, b + 3, cz, 'raft:tidestone_pillar[axis=x]')
    s.set(cx, b + 2, cz + sg, 'minecraft:polished_blackstone_brick_wall')
    # torso, shoulders
    for x in range(cx - 1, cx + 2):
        s.set(x, b + 4, cz, 'raft:tidestone'); s.set(x, b + 5, cz, 'raft:chiseled_tidestone' if x == cx else 'raft:tidestone')
    s.set(cx, b + 4, cz + sg, 'raft:glowing_tidestone')
    s.set(cx - 2, b + 5, cz, stairs('raft:tidestone_brick_stairs', 'east', 'top'))
    s.set(cx + 2, b + 5, cz, stairs('raft:tidestone_brick_stairs', 'west', 'top'))
    # arms: left hangs, right holds hammer on the shoulder
    s.set(cx - 2, b + 4, cz, 'raft:tidestone_brick_wall'); s.set(cx - 2, b + 3, cz, 'raft:tidestone_brick_wall')
    s.set(cx + 2, b + 4, cz, 'raft:tidestone_brick_wall')
    s.set(cx + 2, b + 4, cz + sg, 'raft:tidestone_brick_wall')
    for y in range(b + 5, b + 8):
        s.set(cx + 2, y, cz + sg, 'minecraft:polished_basalt[axis=y]')
    for x in (cx + 1, cx + 2, cx + 3):
        s.set(x, b + 8, cz + sg, 'minecraft:netherite_block' if x != cx + 2 else 'minecraft:iron_block')
        s.set(x, b + 9, cz + sg, 'minecraft:netherite_block' if x != cx + 2 else 'minecraft:iron_block')
    # head with glowing eyes and crest
    s.set(cx, b + 6, cz, 'raft:chiseled_tidestone')
    s.set(cx, b + 6, cz + sg, 'raft:glowing_tidestone')
    s.set(cx, b + 7, cz, 'minecraft:gold_block')
    s.set(cx, b + 8, cz, 'minecraft:lightning_rod[facing=up,powered=false,waterlogged=false]')
    s.set(cx, b + 5, cz - sg, stairs('raft:tidestone_brick_stairs', face))
    for x in (cx - 1, cx, cx + 1):
        s.set(x, b + 1, cz - sg, stairs('raft:tidestone_brick_stairs', face))


def lava_window(s, x, z_front, y1, y2, depth_dir):
    """Column of lava behind iron bars (front cells at z_front, lava one block behind)."""
    for y in range(y1, y2 + 1):
        s.set(x, y, z_front, 'minecraft:iron_bars')
        s.set(x, y, z_front + depth_dir, 'minecraft:lava')
    s.set(x, y1 - 1, z_front, 'minecraft:magma_block')
    s.set(x, y2 + 1, z_front, 'raft:chiseled_tidestone')


def build_hall():
    s = B('ashen_forge/hall', seed='af_hall')
    X0, X1, Z0, Z1 = 0, 40, 0, 45
    cx = 20
    # ---- rock mass (mountain flank) around and over the temple: a natural mound that peaks over the hall
    # and slopes steeply (columnar basalt cliffs) to the template edges, so it reads as carved into the mountain
    def core_dist(x, z):
        dx = max(5 - x, 0, x - 35)
        dz = max(12 - z, 0, z - 43)
        return math.hypot(dx, dz)
    for x in range(X0, X1 + 1):
        for z in range(9, Z1 + 1):
            if z < 12 and 5 <= x <= 35:
                continue          # the facade stands here
            n = noise2('af_mtn', x, z, 5.0)
            n2 = noise2('af_mtn2', x, z, 2.0)
            bump = math.exp(-((z - 27) / 13.0) ** 2)
            hc = 22 + 7 * bump + (n - 0.5) * 4 + (n2 - 0.5) * 1.5 - abs(x - 20) * 0.12
            d = core_dist(x, z)
            h = hc - d * (2.6 + n2) - (2.5 if d > 0 and n < 0.35 else 0)
            h = int(round(h))
            if h < 1:
                continue
            for y in range(0, h + 1):
                if y < h - 1:
                    s.put(x, y, z, ROCK if d < 1.5 or y < h - 4 else Pal(('minecraft:basalt[axis=y]', 6),
                          ('minecraft:polished_basalt[axis=y]', 2), ('minecraft:smooth_basalt', 2), seed='af_cliff'))
                else:
                    s.put(x, y, z, TOP if y == h else ROCK)
    # basalt column clusters and ash drifts on the slopes
    r = rnd('af_cols')
    for _ in range(26):
        x, z = r.randint(X0, X1), r.randint(12, Z1)
        top = max([y for (xx, y, zz) in s.blocks if xx == x and zz == z] or [-1])
        if top < 2:
            continue
        for y in range(top + 1, top + 1 + r.randint(1, 4)):
            s.set(x, y, z, 'minecraft:basalt[axis=y]' if r.random() < 0.7 else 'minecraft:polished_basalt[axis=y]')
    # ---- forecourt
    for x in range(6, 35):
        for z in range(0, 9):
            s.put(x, 0, z, Pal(('minecraft:polished_blackstone_bricks', 5), ('minecraft:basalt[axis=y]', 3),
                                ('raft:black_sand', 2), ('minecraft:cracked_polished_blackstone_bricks', 2), seed='af_fc'))
            for y in range(1, 12):
                s.set(x, y, z, 'minecraft:air')
    # cheek cliffs: columnar basalt flanks close the forecourt at the sides, so it reads as a cut into the
    # slope (and keeps a neighbouring lava lake or scree out of the court)
    for x in list(range(0, 6)) + list(range(35, 41)):
        inner = 5 - x if x < 6 else x - 35          # 0 next to the court, 5 at the template edge
        for z in range(0, 9):
            n = noise2('af_cheek', x, z, 2.5)
            h = int(round(3 + z * 0.85 + (5 - inner) * 0.35 + (n - 0.5) * 3.0))
            for y in range(0, h + 1):
                s.put(x, y, z, TOP if y == h else (ROCK if y < h - 3 else Pal(
                    ('minecraft:basalt[axis=y]', 6), ('minecraft:polished_basalt[axis=y]', 2),
                    ('minecraft:smooth_basalt', 2), seed='af_cliff')))
            if inner == 0 and z % 4 == 1:            # ember sconces facing the court
                s.set(x, h + 1, z, 'minecraft:magma_block' if z < 4 else 'raft:brazier[lit=true,waterlogged=false]')
    # grand stairs
    for x in range(13, 28):
        for i, z in enumerate(range(2, 6)):
            y = i + 1
            for yy in range(1, y):
                s.put(x, yy, z, BRICK)
            s.set(x, y, z, stairs('minecraft:polished_blackstone_brick_stairs', 'south'))
        for z in range(6, 9):
            for yy in range(1, FY):
                s.put(x, yy, z, BRICK)
            s.put(x, FY, z, FLOOR)
    for x in (13, 27):
        for z in range(2, 9):
            s.set(x, FY + 1 if z >= 6 else min(z - 1, FY) + 1, z, 'minecraft:polished_blackstone_brick_wall')
    # side plinths with guardians and braziers
    for (pa, pb, gx) in ((6, 12, 9), (28, 34, 31)):
        for x in range(pa, pb + 1):
            for z in range(3, 9):
                for y in range(1, FY + 1):
                    s.put(x, y, z, BAS if y < FY else BRICK)
        guardian(s, gx, FY + 1, 6, 'north')
        s.set(pa + 1 if pa == 6 else pb - 1, FY + 1, 3, 'raft:brazier[lit=true,waterlogged=false]')
        s.set(pb - 1 if pa == 6 else pa + 1, FY + 1, 3, 'raft:brazier[lit=true,waterlogged=false]')
        # lava basin in front of the plinth (one row, rimmed on every side)
        for x in range(pa + 1, pb):
            s.set(x, 0, 1, 'minecraft:lava')
            s.set(x, 1, 1, 'minecraft:air')
        for x in range(pa, pb + 1):
            for z in (0, 2):
                s.set(x, 0, z, 'minecraft:polished_blackstone_bricks')
                s.set(x, 1, z, 'minecraft:polished_blackstone_brick_wall' if z == 0 or True else 'minecraft:air')
        for x in (pa, pb):
            s.set(x, 0, 1, 'minecraft:polished_blackstone_bricks'); s.set(x, 1, 1, 'minecraft:polished_blackstone_brick_wall')
    # ---- facade (z 9..11)
    tops = {7: 19, 8: 19, 9: 21, 10: 21, 11: 21, 12: 23, 13: 23, 14: 23}
    for x in range(5, 36):
        dx = abs(x - cx)
        top = 25 if dx <= 5 else 23 if dx <= 8 else 21 if dx <= 11 else 19 if dx <= 13 else 17
        for z in range(9, 12):
            for y in range(1, top + 1):
                buttress = dx >= 12
                if buttress:
                    s.put(x, y, z, BAS)
                else:
                    s.put(x, y, z, TB)
        # trims: step ledges at each ziggurat step (front face)
        s.set(x, top + 1, 9, slab('minecraft:polished_blackstone_brick_slab', 'bottom'))
    # ziggurat crown with glowing rune band
    for x in range(cx - 4, cx + 5):
        s.set(x, 24, 9, 'raft:glowing_tidestone' if x % 2 == 0 else 'raft:chiseled_tidestone')
    for x in (cx - 5, cx + 5):
        s.set(x, 26, 9, 'raft:brazier[lit=true,waterlogged=false]')
    # base course
    for x in range(5, 36):
        s.set(x, FY + 1, 8, stairs('minecraft:polished_blackstone_brick_stairs', 'south')) if not (13 <= x <= 27) else None
    # portal opening with stepped arch
    for x in range(17, 24):
        for y in range(FY + 1, FY + 12):
            ok = y <= FY + 9 or (y == FY + 10 and 18 <= x <= 22) or (y == FY + 11 and 19 <= x <= 21)
            if ok:
                for z in range(9, 12):
                    s.set(x, y, z, 'minecraft:air')
        for z in range(9, 12):
            s.put(x, FY, z, FLOOR)
    for (x, y) in ((17, FY + 10), (23, FY + 10)):
        s.set(x, y, 9, stairs('raft:tidestone_brick_stairs', 'east' if x == 17 else 'west', 'top'))
    for (x, y) in ((18, FY + 11), (22, FY + 11)):
        s.set(x, y, 9, stairs('raft:tidestone_brick_stairs', 'east' if x == 18 else 'west', 'top'))
    # great pillars flanking the portal (2x2, standing proud of the facade)
    for px in (15, 24):
        for x in (px, px + 1):
            for z in (7, 8):
                s.set(x, FY, z, 'raft:chiseled_tidestone')
                for y in range(FY + 1, FY + 14):
                    s.set(x, y, z, 'raft:tidestone_pillar[axis=y]')
                s.set(x, FY + 14, z, 'raft:chiseled_tidestone')
                s.set(x, FY + 15, z, 'minecraft:gold_block' if (x + z) % 2 else 'raft:chiseled_tidestone')
    # lintel with runes across the pillars
    for x in range(15, 26):
        for z in (7, 8):
            s.set(x, FY + 16, z, 'raft:tidestone_bricks')
        s.set(x, FY + 16, 6, slab('raft:tidestone_brick_slab', 'bottom'))
        s.set(x, FY + 15, 7, 'raft:glowing_tidestone' if x in (18, 20, 22) else s.get(x, FY + 15, 7) or 'raft:tidestone_bricks') \
            if 17 <= x <= 23 else None
    # emblem: ring of glowing tidestone with a magma heart above the lintel
    ecx, ecy = cx, FY + 18 + 1
    for x in range(cx - 3, cx + 4):
        for y in range(ecy - 3, ecy + 4):
            d = math.hypot(x - cx, y - ecy)
            if 2.2 < d <= 3.2:
                s.set(x, y, 9, 'raft:glowing_tidestone')
            elif d <= 1.2:
                s.set(x, y, 9, 'minecraft:magma_block' if d > 0.5 else 'minecraft:shroomlight')
            elif d <= 2.2:
                s.set(x, y, 9, 'minecraft:gold_block' if (x + y) % 2 == 0 else 'raft:chiseled_tidestone')
    # rune strips and lava windows
    for x in (13, 27):
        for y in range(FY + 2, FY + 15):
            s.set(x, y, 9, 'raft:glowing_tidestone' if y % 3 == 0 else 'raft:chiseled_tidestone')
    for x in (10, 30):
        lava_window(s, x, 9, FY + 2, FY + 12, 1)
    # ---- the hall
    hx1, hx2, hz1, hz2 = 9, 31, 12, 40
    CEIL = 19
    for x in range(5, 36):
        for z in range(12, 44):
            inside = hx1 <= x <= hx2 and hz1 <= z <= hz2
            for y in range(1, FY):
                s.put(x, y, z, BAS)
            if inside:
                s.put(x, FY, z, FLOOR)
                for y in range(FY + 1, CEIL):
                    s.set(x, y, z, 'minecraft:air')
                s.put(x, CEIL, z, BRICK)
            else:
                for y in range(FY, CEIL + 2):
                    s.put(x, y, z, TB if (x in (hx1 - 1, hx2 + 1) or z == hz2 + 1) else BAS)
            if not inside or True:
                for y in range(CEIL + 1, CEIL + 3):
                    if (x, y, z) not in s.blocks or s.name_at(x, y, z) == 'minecraft:air':
                        s.put(x, y, z, ROCK)
    # coffered ceiling: lower ribs every 5 blocks
    for z in range(hz1, hz2 + 1, 5):
        for x in range(hx1, hx2 + 1):
            s.set(x, CEIL - 1, z, 'minecraft:polished_blackstone_bricks')
    for x in range(hx1, hx2 + 1):
        if s.name_at(x, CEIL - 1, hz1) == 'minecraft:polished_blackstone_bricks':
            pass
    for z in range(hz1, hz2 + 1):
        s.set(cx, CEIL, z, 'raft:glowing_tidestone' if z % 2 == 0 else 'raft:chiseled_tidestone')
    # nave floor: inlaid tidestone line + glowing dots
    for z in range(hz1, hz2 + 1):
        s.set(cx, FY, z, 'raft:tidestone_bricks')
        if z % 4 == 0:
            s.set(cx, FY, z, 'raft:glowing_tidestone')
    for z in range(hz1, hz1 + 2):
        for x in range(hx1, hx2 + 1):
            s.put(x, FY, z, FLOOR)
    # lava channels
    for z in range(14, 37):
        for x in (12, 13, 27, 28):
            s.set(x, FY, z, 'minecraft:lava')
            s.set(x, FY - 1, z, 'minecraft:blackstone')
    for z in (13, 37):
        for x in (12, 13, 27, 28):
            s.set(x, FY, z, 'minecraft:polished_blackstone_bricks')
    # colonnades on the channel edges with arches; low curb walls
    for x in (14, 26):
        for z in range(14, 37):
            col = z in (15, 20, 25, 30, 35)
            if col:
                s.set(x, FY + 1, z, 'raft:chiseled_tidestone')
                for y in range(FY + 2, CEIL - 3):
                    s.set(x, y, z, 'raft:tidestone_pillar[axis=y]')
                s.set(x, CEIL - 3, z, 'raft:chiseled_tidestone')
                s.set(x, CEIL - 2, z, 'raft:tidestone_bricks')
                s.set(x, CEIL - 1, z, 'raft:tidestone_bricks')
            else:
                s.set(x, FY + 1, z, 'minecraft:polished_blackstone_brick_wall' if z % 5 not in (2, 3) else 'minecraft:air')
                s.set(x, CEIL - 1, z, 'raft:tidestone_bricks')
                k = z % 5
                if k in (1, 4):
                    s.set(x, CEIL - 2, z, stairs('raft:tidestone_brick_stairs', 'south' if k == 1 else 'north', 'top'))
        for z in (14, 36):
            pass
    # side walls: alcoves with tablets and soul lanterns
    for z in (17, 22, 32):
        for (wx, face) in ((hx1 - 1, 'east'), (hx2 + 1, 'west')):
            s.set(wx, FY + 1, z, 'minecraft:air'); s.set(wx, FY + 2, z, 'minecraft:air'); s.set(wx, FY + 3, z, 'minecraft:air')
            back = wx - 1 if face == 'east' else wx + 1
            s.set(back, FY + 2, z, 'raft:chiseled_tidestone')
            s.set(back, FY + 3, z, 'raft:glowing_tidestone')
            s.set(wx, FY + 1, z, 'minecraft:polished_blackstone_slab[type=bottom,waterlogged=false]')
            s.set(wx, FY + 4, z, stairs('raft:tidestone_brick_stairs', face, 'top'))
    for z in range(14, 40, 5):
        for wx in (hx1 - 1, hx2 + 1):
            s.set(wx, FY + 6, z, 'raft:glowing_tidestone')
    # hanging lanterns
    for z in (16, 22, 28):
        for x in (cx,):
            for y in range(CEIL - 4, CEIL):
                s.set(x, y, z, CHAIN)
            s.set(x, CEIL - 5, z, SOUL)
        for x in (10, 30):
            s.set(x, CEIL - 1, z, CHAIN); s.set(x, CEIL - 2, z, CHAIN)
            s.set(x, CEIL - 3, z, 'minecraft:lantern[hanging=true,waterlogged=false]')
    # ---- forge dais, giant anvil, hearth
    for x in range(15, 26):
        for z in range(30, 41):
            s.put(x, FY + 1, z, BRICK)
    for x in range(15, 26):
        s.set(x, FY + 1, 30, stairs('minecraft:polished_blackstone_brick_stairs', 'south'))
    D = FY + 2   # standing layer on the dais
    # anvil (long axis x), centre z=33
    az = 33
    for x in range(18, 23):
        for z in range(az - 1, az + 2):
            s.set(x, D, z, 'minecraft:polished_blackstone_bricks')
    for x in range(19, 22):
        s.set(x, D + 1, az, 'minecraft:polished_deepslate')
        s.set(x, D + 1, az - 1, stairs('minecraft:polished_deepslate_stairs', 'south', 'top'))
        s.set(x, D + 1, az + 1, stairs('minecraft:polished_deepslate_stairs', 'north', 'top'))
    for x in range(16, 25):
        for z in range(az - 1, az + 2):
            s.set(x, D + 2, z, 'minecraft:polished_deepslate')
            s.set(x, D + 3, z, 'minecraft:iron_block' if 17 <= x <= 23 else 'minecraft:polished_deepslate')
    s.set(15, D + 3, az, 'minecraft:polished_deepslate')
    s.set(14, D + 3, az, stairs('minecraft:polished_deepslate_stairs', 'east'))
    s.set(15, D + 2, az, stairs('minecraft:polished_deepslate_stairs', 'east', 'top'))
    s.set(25, D + 3, az, slab('minecraft:polished_deepslate_slab', 'bottom'))
    # glowing ingot on the anvil + giant hammer
    s.set(17, D + 4, az, 'minecraft:magma_block')       # glowing ingot
    s.set(18, D + 4, az, 'minecraft:magma_block')
    for x in (22, 23):                                  # giant hammer lying on the face
        for y in (D + 4, D + 5):
            s.set(x, y, az, 'minecraft:netherite_block')
    for x in (19, 20, 21):
        s.set(x, D + 4, az, 'minecraft:polished_basalt[axis=x]')
    # hearth against the back wall
    for x in range(14, 27):
        for z in range(37, 41):
            for y in range(D, D + 9):
                s.put(x, y, z, BRICK)
    for x in range(17, 24):        # mouth
        for y in range(D, D + 4):
            s.set(x, y, 37, 'minecraft:air')
            s.set(x, y, 38, 'minecraft:air')
    for x in range(17, 24):
        s.set(x, D, 38, 'minecraft:lava')                 # fire bed, held in by the lip and magma behind
        s.set(x, D - 1, 38, 'minecraft:blackstone')
        s.set(x, D, 37, slab('minecraft:polished_blackstone_brick_slab', 'bottom'))   # hearth lip
        s.set(x, D, 39, 'minecraft:magma_block')
        s.set(x, D + 1, 39, 'minecraft:magma_block')
    s.set(16, D + 4, 37, stairs('minecraft:polished_blackstone_brick_stairs', 'east', 'top'))
    s.set(24, D + 4, 37, stairs('minecraft:polished_blackstone_brick_stairs', 'west', 'top'))
    for x in range(17, 24):
        s.set(x, D + 4, 37, 'raft:chiseled_tidestone' if x != 20 else 'raft:glowing_tidestone')
        s.set(x, D + 4, 36, stairs('minecraft:polished_blackstone_brick_stairs', 'south', 'top'))
    # hood narrowing to the chimney
    for i, y in enumerate(range(D + 9, CEIL + 1)):
        w = max(1, 5 - i)
        for x in range(cx - w, cx + w + 1):
            for z in range(37, 41):
                s.put(x, y, z, BRICK)
    for y in range(D + 5, CEIL + 14):
        s.set(cx, y, 39, 'minecraft:air' if y < CEIL + 13 else 'minecraft:campfire[facing=north,lit=true,signal_fire=true,waterlogged=false]')
        for (ox, oz) in ((-1, 0), (1, 0), (0, -1), (0, 1)):
            if y > CEIL:
                s.put(cx + ox, y, 39 + oz, BRICK)
    s.set(cx, D + 4, 39, 'minecraft:magma_block')
    # bellows (non flammable): brown terracotta bags in basalt frames
    for bx in (15, 25):
        for z in (35, 36):
            s.set(bx, D, z, 'minecraft:polished_basalt[axis=y]')
            s.set(bx, D + 1, z, 'minecraft:brown_terracotta')
            s.set(bx, D + 2, z, stairs('minecraft:polished_blackstone_brick_stairs', 'north' if z == 35 else 'south'))
    # tools around the dais
    s.set(16, D, 31, 'minecraft:anvil[facing=east]')
    s.set(24, D, 31, 'minecraft:chipped_anvil[facing=west]')
    s.set(16, D, 35, 'minecraft:grindstone[face=floor,facing=east]')
    s.set(24, D, 35, 'minecraft:smithing_table')
    s.set(25, D, 32, 'minecraft:blast_furnace[facing=west,lit=false]')
    # story chest beside a Tidewarden tablet
    s.set(15, D, 33, 'raft:chiseled_tidestone')
    s.set(15, D + 1, 33, 'raft:glowing_tidestone')
    s.chest(16, D, 33, 'raft:chests/story/ashen_forge', facing='east')
    s.set(15, D, 32, 'minecraft:soul_lantern[hanging=false,waterlogged=false]')
    # ---- storeroom (west) and smithy (east)
    for x in range(1, 8):
        for z in range(20, 29):
            for y in range(FY + 1, FY + 6):
                s.set(x, y, z, 'minecraft:air' if (2 <= x <= 7 and 21 <= z <= 27) else (TB.pick(x, y, z) if x == 1 or z in (20, 28) else s.get(x, y, z) or 'minecraft:air'))
            s.put(x, FY, z, FLOOR)
            s.put(x, FY + 6, z, BRICK)
    for z in (23, 24, 25):
        for y in range(FY + 1, FY + 4):
            s.set(8, y, z, 'minecraft:air')
    s.set(8, FY + 4, 24, 'raft:glowing_tidestone')
    s.chest(2, FY + 1, 22, 'raft:chests/ashen_storage', facing='east')
    s.chest(2, FY + 1, 26, 'raft:chests/ashen_storage', facing='east')
    s.set(2, FY + 1, 24, 'minecraft:iron_block'); s.set(2, FY + 2, 24, 'minecraft:raw_iron_block')
    s.set(2, FY + 1, 23, 'minecraft:raw_gold_block'); s.set(2, FY + 1, 25, 'minecraft:coal_block')
    s.set(3, FY + 1, 27, 'minecraft:anvil[facing=north]')
    s.set(5, FY + 1, 27, 'minecraft:cauldron')
    s.set(6, FY + 1, 21, 'minecraft:raw_copper_block'); s.set(5, FY + 1, 21, 'minecraft:copper_block')
    for x in (3, 6):
        s.set(x, FY + 5, 24, CHAIN); s.set(x, FY + 4, 24, SOUL)
    for z in range(21, 28, 2):
        s.set(7, FY + 2, z, 'minecraft:iron_chain[axis=x,waterlogged=false]') if s.is_air(7, FY + 2, z) else None
    # east aisle smithy: blast furnaces and quench basin (well away from lava)
    for z in (17, 18):
        s.set(hx2, FY + 1, z, 'minecraft:blast_furnace[facing=west,lit=false]')
    s.set(hx2, FY + 1, 19, 'minecraft:smithing_table')
    s.set(hx2, FY + 1, 23, 'minecraft:grindstone[face=floor,facing=west]')
    s.set(hx2 - 1, FY + 1, 39, 'minecraft:water_cauldron[level=3]')
    s.set(hx2, FY + 1, 39, 'minecraft:water_cauldron[level=1]')
    # ---- vault passage (south-west), connector to the vault piece
    for z in range(41, Z1 + 1):
        for x in (9, 10, 11):
            s.put(x, FY, z, FLOOR)
            for y in range(FY + 1, FY + 5):
                s.set(x, y, z, 'minecraft:air')
        for x in (8, 12):
            for y in range(FY + 1, FY + 5):
                s.put(x, y, z, TB)
            s.set(x, FY + 3, z, 'raft:glowing_tidestone') if z % 2 == 0 else None
        for x in range(8, 13):
            s.put(x, FY + 5, z, TB)
    for x in (9, 10, 11):
        s.set(x, FY + 4, 41, stairs('raft:tidestone_brick_stairs', 'south', 'top'))
    s.jigsaw(10, FY, Z1, 'south_up', 'raft:ashen_vault_link', 'raft:ashen_vault_entry', 'raft:ashen_forge/vault',
             final_state='minecraft:polished_blackstone_bricks')
    # ---- ember crabs (also spawn via spawn_overrides)
    for (x, z) in ((18.5, 21.5), (23.5, 26.5), (10.5, 37.5), (30.5, 14.5)):
        s.entity(x, FY + 1, z, {'id': 'raft:ember_crab', 'PersistenceRequired': True})
    # ---- decay: cracks, fallen blocks, ash drifts
    r = rnd('af_decay')
    for _ in range(14):
        x, z = r.randint(hx1, hx2), r.randint(hz1, hz2)
        if s.name_at(x, FY + 1, z) in (None, 'minecraft:air') and s.name_at(x, FY, z) not in ('minecraft:lava',):
            s.set(x, FY + 1, z, r.choice(['minecraft:polished_blackstone_brick_slab[type=bottom,waterlogged=false]',
                                          'minecraft:cobbled_deepslate', 'raft:volcanic_ash', 'minecraft:basalt[axis=x]']))
    # remove anything that strayed outside the box
    for p in [p for p in s.blocks if not (X0 <= p[0] <= X1 and Z0 <= p[2] <= Z1) or p[1] < 0]:
        del s.blocks[p]
    return s


def build_vault():
    s = B('ashen_forge/vault', seed='af_vault')
    F = 5                      # floor layer (aligned with the hall floor)
    W, Dz = 27, 31
    cx = 13
    ccx, ccz, R = 13, 18, 11.5     # cavern
    TOPY = F + 15
    # solid rock body (rounded top), then carve
    for x in range(0, W):
        for z in range(0, Dz):
            n = noise2('af_vmtn', x, z, 4.0)
            d = math.hypot(x - ccx, z - ccz)
            dc = abs(x - cx) if z < ccz else 99          # corridor zone
            rim = min(R + 3.2 + (n - 0.5) * 2.0 - d, 4.5 - dc + 2.0 * (n - 0.3))
            rim = max(rim, R + 3.2 + (n - 0.5) * 2.0 - d)
            if dc <= 4:
                rim = max(rim, 3.0)
            if rim <= 0:
                continue
            h = TOPY - max(0, d - 10) * 0.35 + (n - 0.5) * 3
            h = int(round(min(h, F + 2 + rim * 4.5)))
            for y in range(0, h + 1):
                s.put(x, y, z, ROCK if y < h else TOP)
    # corridor
    for z in range(0, 8):
        for x in range(cx - 1, cx + 2):
            s.put(x, F, z, FLOOR)
            for y in range(F + 1, F + 5):
                s.set(x, y, z, 'minecraft:air')
        for x in (cx - 2, cx + 2):
            for y in range(F + 1, F + 5):
                s.put(x, y, z, TB)
            if z % 2 == 0:
                s.set(x, F + 3, z, 'raft:glowing_tidestone')
        for x in range(cx - 2, cx + 3):
            s.put(x, F + 5, z, TB)
    # rune gate at the cavern mouth
    for y in range(F + 1, F + 6):
        s.set(cx - 2, y, 7, 'raft:tidestone_pillar[axis=y]'); s.set(cx + 2, y, 7, 'raft:tidestone_pillar[axis=y]')
    for x in range(cx - 2, cx + 3):
        s.set(x, F + 6, 7, 'raft:chiseled_tidestone' if x != cx else 'raft:glowing_tidestone')
    # cavern: dome, floor ring, moat, island
    for x in range(0, W):
        for z in range(6, Dz):
            d = math.hypot(x - ccx, (z - ccz) * 1.0)
            if d > R + 0.3:
                continue
            # dome
            for y in range(F + 1, F + 13):
                dd = math.hypot(d, (y - F) * 1.05)
                if dd <= R + 0.3:
                    s.set(x, y, z, 'minecraft:air')
            if d > 8.6:                     # walkway ring
                s.put(x, F, z, FLOOR)
            elif d > 4.6:                   # lava moat (3 deep)
                for y in range(1, F):
                    s.set(x, y, z, 'minecraft:lava' if y >= F - 2 else 'minecraft:blackstone')
                s.set(x, F, z, 'minecraft:air')
            else:                           # island
                for y in range(1, F + 1):
                    s.put(x, y, z, BAS if y < F else Pal(('raft:tidestone_bricks', 3), ('minecraft:polished_blackstone_bricks', 2), seed='af_isl'))
    # rim of the moat: low ledge so it is not too easy to fall in
    for x in range(0, W):
        for z in range(6, Dz):
            d = math.hypot(x - ccx, z - ccz)
            if 8.6 < d <= 9.6 and s.name_at(x, F, z) and s.name_at(x, F, z) != 'minecraft:air':
                s.set(x, F, z, 'minecraft:polished_blackstone_bricks')
    # the bridge (3 wide, one plank broken), arches below
    for z in range(ccz - 9, ccz - 3):
        for x in range(cx - 1, cx + 2):
            if (x, z) in ((cx - 1, ccz - 6), (cx + 1, ccz - 7)):
                continue
            s.set(x, F, z, 'minecraft:polished_blackstone_bricks' if x == cx else slab('minecraft:polished_blackstone_brick_slab', 'top'))
    for z in (ccz - 8, ccz - 5):
        for x in (cx - 1, cx + 1):
            s.set(x, F + 1, z, 'raft:brazier[lit=true,waterlogged=false]') if (x == cx - 1 and z == ccz - 8) or (x == cx + 1 and z == ccz - 5) else None
    # vault building on the island
    vx1, vx2, vz1, vz2 = ccx - 3, ccx + 3, ccz - 3, ccz + 3
    for x in range(vx1, vx2 + 1):
        for z in range(vz1, vz2 + 1):
            s.set(x, F, z, 'raft:chiseled_tidestone' if (x + z) % 2 else 'raft:tidestone_bricks')
            for y in range(F + 1, F + 6):
                edge = x in (vx1, vx2) or z in (vz1, vz2)
                if edge:
                    corner = x in (vx1, vx2) and z in (vz1, vz2)
                    s.set(x, y, z, 'minecraft:gold_block' if corner and y == F + 5 else
                          'raft:tidestone_pillar[axis=y]' if corner else
                          ('raft:glowing_tidestone' if y == F + 3 and (x + z) % 2 == 0 else TB.pick(x, y, z)))
                else:
                    s.set(x, y, z, 'minecraft:air')
    for i, y in enumerate(range(F + 6, F + 9)):
        for x in range(vx1 + i, vx2 + 1 - i):
            for z in range(vz1 + i, vz2 + 1 - i):
                edge = x in (vx1 + i, vx2 - i) or z in (vz1 + i, vz2 - i)
                if edge or y == F + 8:
                    s.set(x, y, z, 'raft:tidestone_bricks' if y < F + 8 else 'raft:glowing_tidestone')
                else:
                    s.set(x, y, z, 'minecraft:air')
    s.set(ccx, F + 9, ccz, 'minecraft:gold_block')
    # doorway (north) with a broken bar gate
    for y in (F + 1, F + 2, F + 3):
        s.set(ccx, y, vz1, 'minecraft:air')
    s.set(ccx - 1, F + 1, vz1, 'minecraft:iron_bars'); s.set(ccx + 1, F + 2, vz1, 'minecraft:iron_bars')
    s.set(ccx, F + 4, vz1, 'minecraft:gold_block')
    # inside: the Ember Sigil chest between two sigil pedestals
    s.chest(ccx, F + 1, vz2 - 1, 'raft:chests/story/ashen_vault', facing='north', block='raft:treasure_chest')
    s.set(ccx - 1, F + 1, vz2 - 1, 'raft:sigil_pedestal[waterlogged=false]')
    s.set(ccx + 1, F + 1, vz2 - 1, 'raft:sigil_pedestal[waterlogged=false]')
    s.set(ccx - 1, F + 2, vz2 - 1, 'minecraft:soul_lantern[hanging=false,waterlogged=false]')
    s.set(ccx + 1, F + 2, vz2 - 1, 'minecraft:soul_lantern[hanging=false,waterlogged=false]')
    s.set(ccx, F + 4, ccz, CHAIN); s.set(ccx, F + 3, ccz, SOUL)
    # storage niche on the east side of the ring
    nx, nz = ccx + 10, ccz
    for z in range(nz - 1, nz + 2):
        for y in range(F + 1, F + 4):
            s.set(nx + 1, y, z, 'minecraft:air')
        s.put(nx + 1, F, z, FLOOR)
    s.chest(nx + 1, F + 1, nz, 'raft:chests/ashen_storage', facing='west', block='raft:wooden_crate')
    s.set(nx + 1, F + 1, nz - 1, 'minecraft:iron_block')
    s.set(nx + 1, F + 1, nz + 1, 'minecraft:raw_gold_block')
    s.set(nx + 1, F + 3, nz, 'raft:glowing_tidestone')
    # lava falls behind bars in the cavern wall (west) and ceiling glow
    for z in (ccz - 2, ccz + 2):
        x = ccx - 11
        for y in range(F + 1, F + 7):
            s.set(x, y, z, 'minecraft:iron_bars')
            s.set(x - 1, y, z, 'minecraft:lava')
        s.set(x - 1, F, z, 'minecraft:blackstone')
        s.set(x, F + 7, z, 'minecraft:magma_block')
    for (x, z) in ((ccx - 5, ccz - 4), (ccx + 5, ccz + 4), (ccx + 4, ccz - 6), (ccx - 6, ccz + 5)):
        top = max(y for (xx, y, zz) in s.blocks if xx == x and zz == z and s.name_at(xx, y, zz) == 'minecraft:air') + 1
        s.set(x, top, z, 'minecraft:magma_block')
        for y in range(top - 3, top):
            s.set(x, y, z, CHAIN)
        s.set(x, top - 4, z, SOUL)
    # ember crabs
    for (x, z) in ((ccx - 9.5, ccz + 0.5), (ccx + 8.5, ccz + 4.5)):
        s.entity(x, F + 1, z, {'id': 'raft:ember_crab', 'PersistenceRequired': True})
    s.jigsaw(cx, F, 0, 'north_up', 'raft:ashen_vault_entry', 'raft:ashen_vault_link', 'minecraft:empty',
             final_state='minecraft:polished_blackstone_bricks')
    for p in [p for p in s.blocks if not (0 <= p[0] < W and 0 <= p[2] < Dz) or p[1] < 0]:
        del s.blocks[p]
    return s


def build_all(preview=True):
    from . import worldgen as wg
    out = {}
    for fn in (build_hall, build_vault):
        s = fn()
        s.finalize()
        validate(s, s.name)
        out[s.name] = (s.save(MODULE), s.size())
        if preview:
            tag = 'af_' + s.name.split('/')[-1]
            look(s, tag, cuts=(FY + 3, FY + 9) if 'hall' in s.name else (7, 10))
    wg.processor_list('ashen_forge_cracks', [
        wg.rule('raft:tidestone_bricks', 0.10, 'raft:cracked_tidestone_bricks'),
        wg.rule('minecraft:polished_blackstone_bricks', 0.08, 'minecraft:cracked_polished_blackstone_bricks'),
        wg.rule('minecraft:basalt', 0.05, 'minecraft:magma_block'),
    ])
    wg.pool('ashen_forge/start', [('raft:ashen_forge/hall', 1, 'raft:ashen_forge_cracks', 'rigid')])
    wg.pool('ashen_forge/vault', [('raft:ashen_forge/vault', 1, 'raft:ashen_forge_cracks', 'rigid')])
    wg.structure('ashen_forge', 'raft:ashen_forge/start', 'surface_structures', 'beard_thin', 0, 'WORLD_SURFACE_WG',
                 size=1, max_dist=80, spawn_overrides={
                     'monster': {'bounding_box': 'piece', 'spawns': [
                         {'type': 'raft:ember_crab', 'count': {'type': 'minecraft:uniform', 'min_inclusive': 1,
                                                               'max_inclusive': 3}, 'weight': 1}]}})
    wg.structure_set('ashen_forge', 40, 18, 31415923)
    wg.biome_tag('ashen_forge', ['volcanic_isle'])
    return out


if __name__ == '__main__':
    for k, v in build_all().items():
        print(k, v[1])
