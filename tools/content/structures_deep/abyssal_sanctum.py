"""abyssal_sanctum – the Leviathan's arena on the deep sea floor (spec 9.2, 10, 11.1).

Layout (template coordinates before save; x east, z south, y up; the dome centre is x=0, z=0):
- Stepped plinth y 0..12 (three tiers) with an unbreakable core of raft:sanctum_tidestone and a decorative tidestone skin.
- Arena floor y=12 (players walk at y=13). Central pool radius 7, water y 2..12 (11 deep), bottom y=1.
- Air dome: drum r=22 up to y=18, then an elliptic cap to y=31 (inside). Shell 2-3 blocks of sanctum_tidestone ribs and
  sanctum_glass panels, no gaps (checked by enclosure_check / leak_check).
- raft:leviathan_altar at (0, 13, 9) facing north: the pool centre (0, *, 0) is exactly 9 blocks in front of it and the
  boss spawns at (0, 9, 0) = 4 below the altar (contract 9.2).
- Entrance (south): grand stair -> flooded gatehouse vestibule -> 5x5 raft:tide_seal (x -2..2, y 13..17, z 24) ->
  short dry passage -> dome. The seal is the only way in.
- Reward room (north), only reachable through the arena: raft:chests/sanctum_reward.
"""
import math
import random

from .common import (Deep, MODULE, NO_SPAWNS, enclosure_check, is_full, leak_check, pick, structure_json,
                     structure_set, template_pool, biome_tag)

SID = 'abyssal_sanctum'
SANCTUM = 'raft:sanctum_tidestone'
SGLASS = 'raft:sanctum_glass'
SEAL = 'raft:tide_seal[open=false]'
UNBREAKABLE = {'raft:sanctum_tidestone', 'raft:sanctum_glass', 'raft:tide_seal'}

FLOOR_Y = 12          # arena floor block layer
POOL_R = 7
POOL_BOTTOM = 1       # pool floor block; water from 2 to 12
DRUM_TOP = 18
IN_RX, IN_RY = 22.0, 13.0     # inner cap semi-axes (cap centre y = DRUM_TOP)
OUT_RX, OUT_RY = 24.6, 15.4
ALTAR = (0, FLOOR_Y + 1, 9)
SEAL_Z = 24

SKIN = [('raft:tidestone_bricks', 62), ('raft:cracked_tidestone_bricks', 16), ('raft:mossy_tidestone_bricks', 14),
        ('raft:tidestone', 8)]
FLOOR = [('raft:tidestone_bricks', 78), ('raft:cracked_tidestone_bricks', 12), ('raft:tidestone', 10)]


def rad(x, z):
    return math.hypot(x, z)


def ang(x, z):
    return math.degrees(math.atan2(z, x)) % 360


def inner(x, y, z):
    """Inside the dome's air volume (above the floor)."""
    r = rad(x, z)
    if y <= FLOOR_Y:
        return False
    if y <= DRUM_TOP:
        return r <= IN_RX
    return (r / IN_RX) ** 2 + ((y - DRUM_TOP) / IN_RY) ** 2 <= 1.0


def outer(x, y, z):
    r = rad(x, z)
    if y < FLOOR_Y:
        return False
    if y <= DRUM_TOP:
        return r <= OUT_RX
    return (r / OUT_RX) ** 2 + ((y - DRUM_TOP) / OUT_RY) ** 2 <= 1.0


# ----------------------------------------------------------------------------------------------- parts
def plinth(s, rnd):
    tiers = [(0, 4, 31.4), (5, 8, 28.4), (9, FLOOR_Y, 26.4)]
    for (y1, y2, r) in tiers:
        s.cylinder(0, 0, y1, y2, r, SANCTUM)
    # gate podium (south) and reward annex podium (north)
    s.fill(-10, 0, 22, 10, FLOOR_Y, 35, SANCTUM)
    s.fill(-11, 0, -41, 11, FLOOR_Y, -22, SANCTUM)
    # stepped tier edges: cornice of upside-down stairs under each tier lip, and a band of chiseled stone
    for (y1, y2, r) in tiers:
        for x in range(-33, 34):
            for z in range(-43, 37):
                if s.name_at(x, y2, z) != SANCTUM:
                    continue
                # exposed ring = no plinth block on one horizontal side
                if any(s.name_at(x + dx, y2, z + dz) is None for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                    s.set(x, y2, z, 'raft:chiseled_tidestone' if (x * 7 + z * 3) % 9 == 0 else
                          pick(rnd, [('raft:tidestone_bricks', 5), ('raft:cracked_tidestone_bricks', 1)]))


def skin(s, rnd):
    """Clad exposed core blocks with decorative stone. The dome shell and every core block within 2 blocks of an
    interior space stay sanctum, so a closed unbreakable layer always remains (checked by enclosure_check)."""
    inside = set(s.dry) | {p for p, b in s.blocks.items() if b[0] == 'minecraft:water' and p[1] <= FLOOR_Y
                           and rad(p[0], p[2]) <= POOL_R + 0.5}
    near = set()
    for (x, y, z) in inside:
        for dx in (-2, -1, 0, 1, 2):
            for dy in (-2, -1, 0, 1, 2):
                for dz in (-2, -1, 0, 1, 2):
                    near.add((x + dx, y + dy, z + dz))
    tiers = ((0, 4, 31.4), (5, 8, 28.4), (9, FLOOR_Y, 26.4))
    for (x, y, z), b in list(s.blocks.items()):
        if b[0] != SANCTUM or y == 0 or (x, y, z) in near or (y >= FLOOR_Y and outer(x, y, z)):
            continue
        side = [(dx, dz) for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)) if (x + dx, y, z + dz) not in s.blocks]
        top = (x, y + 1, z) not in s.blocks
        if not side and not top:
            continue
        if side:
            tier = next((t for t in tiers if t[0] <= y <= t[1]), None)
            a = ang(x, z)
            if tier and y == tier[1]:
                st = 'raft:chiseled_tidestone'                       # cornice course
            elif tier and y == tier[0]:
                st = 'raft:tidestone_bricks'                          # plinth course
            elif tier and int(a) % 12 < 2 and abs(rad(x, z) - tier[2]) < 1.5:
                st = 'raft:tidestone_pillar[axis=y]'                  # pilasters
            elif tier and y == tier[0] + 2 and int(a) % 24 in (6, 7):
                st = 'raft:glowing_tidestone'                          # rune lights between pilasters
            else:
                st = pick(rnd, SKIN)
        else:
            st = pick(rnd, [('raft:tidestone_bricks', 6), ('raft:tidestone', 2), ('raft:cracked_tidestone_bricks', 1),
                            ('raft:mossy_tidestone_bricks', 1)])
        s.set(x, y, z, st)


def dome(s, rnd):
    ribs = [i * 30 for i in range(12)]
    for x in range(-26, 27):
        for z in range(-26, 27):
            r = rad(x, z)
            if r > OUT_RX + 1:
                continue
            a = ang(x, z)
            for y in range(FLOOR_Y, 36):
                if not outer(x, y, z) or inner(x, y, z):
                    continue
                if y == FLOOR_Y:
                    if r > IN_RX - 0.5:
                        s.set(x, y, z, SANCTUM)
                    continue
                # drum: solid sanctum with tall glass slits between the ribs
                rib = min(abs((a - rb + 180) % 360 - 180) for rb in ribs) * math.pi / 180 * max(r, 1)
                if y <= DRUM_TOP:
                    slit = rib > 2.2 and 14 <= y <= 17
                    s.set(x, y, z, SGLASS if slit else SANCTUM)
                    continue
                t = (y - DRUM_TOP) / OUT_RY
                band = y in (DRUM_TOP + 1, DRUM_TOP + 7)
                oculus = r < 4.5
                if rib <= 1.3 or band or oculus and r > 2.2:
                    s.set(x, y, z, SANCTUM)
                else:
                    s.set(x, y, z, SGLASS)


def interior(s, rnd):
    """Air dome, floor, pool."""
    for x in range(-23, 24):
        for z in range(-23, 24):
            for y in range(FLOOR_Y + 1, 33):
                if inner(x, y, z):
                    s.air(x, y, z, x, y, z)
    # floor y=12 inside r <= 22
    for x in range(-22, 23):
        for z in range(-22, 23):
            r = rad(x, z)
            if r > IN_RX + 0.3:
                continue
            a = ang(x, z)
            spoke = min(abs((a - sp + 180) % 360 - 180) for sp in range(0, 360, 45)) * math.pi / 180 * r < 0.6
            if r <= POOL_R + 0.3:
                continue
            if r <= POOL_R + 1.3:
                st = 'raft:chiseled_tidestone'
            elif r <= POOL_R + 2.3:
                st = 'raft:glowing_tidestone' if int(round(a)) % 20 < 4 else 'raft:tidestone_pillar[axis=y]'
            elif spoke and r < 19:
                st = 'minecraft:raw_gold_block' if int(r) % 4 == 0 else 'raft:chiseled_tidestone'
            elif 15.5 < r <= 16.5:
                st = 'raft:chiseled_tidestone'
            elif r > 20.6:
                st = 'raft:tidestone'
            else:
                st = pick(rnd, FLOOR)
            s.set(x, FLOOR_Y, z, st)
    # pool: water column y 2..12, lining with rune bands, sanctum bottom
    for x in range(-POOL_R - 1, POOL_R + 2):
        for z in range(-POOL_R - 1, POOL_R + 2):
            r = rad(x, z)
            if r <= POOL_R + 0.3:
                s.set(x, POOL_BOTTOM, z, 'raft:glowing_tidestone' if (x, z) == (0, 0) or r < 1.5 else
                      ('raft:chiseled_tidestone' if r < 3.3 else 'raft:tidestone_bricks'))
                for y in range(POOL_BOTTOM + 1, FLOOR_Y + 1):
                    s.set(x, y, z, 'minecraft:water')
                    s.dry.discard((x, y, z))
            elif r <= POOL_R + 1.3:
                for y in range(POOL_BOTTOM + 1, FLOOR_Y):
                    band = y in (4, 8)
                    s.set(x, y, z, 'raft:glowing_tidestone' if band and (x + z) % 2 == 0 else
                          ('raft:chiseled_tidestone' if band else 'raft:tidestone_bricks'))
    # eight sea lanterns set flush into the floor at the pool's rim: they light the water surface where the
    # Leviathan rises (the dimension has no ambient light) without blocking its lunges across the ring
    for k in range(8):
        a = math.radians(45 * k)
        s.set(round(math.cos(a) * 8), FLOOR_Y, round(math.sin(a) * 8), 'minecraft:sea_lantern')
    # the sanctum stone directly under the pool lining stays (core); light sea pickles at the pool bottom
    for (x, z) in ((3, 2), (-3, -2), (2, -4), (-4, 3), (5, -1), (-1, 5)):
        s.set(x, POOL_BOTTOM + 1, z, 'minecraft:sea_pickle[pickles=4]')
    for (x, z) in ((4, 4), (-5, 2), (1, -6), (-2, -5), (6, 1)):
        s.kelp(x, POOL_BOTTOM + 1, z, 5 + (x * z) % 3, glow=True)


def pillar(s, x0, z0, top):
    """Cross-section 3x3 Tidewarden column from the floor to the dome."""
    y0 = FLOOR_Y + 1
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            corner = dx != 0 and dz != 0
            s.set(x0 + dx, y0, z0 + dz, 'raft:chiseled_tidestone')
            for y in range(y0 + 1, top + 1):
                if not inner(x0 + dx, y, z0 + dz) and not s.free(x0 + dx, y, z0 + dz):
                    continue
                if corner:
                    if y in (y0 + 1, top - 1, top):
                        s.set(x0 + dx, y, z0 + dz, 'raft:tidestone_bricks')
                    continue
                if y in (y0 + 3, y0 + 8):
                    s.set(x0 + dx, y, z0 + dz, 'raft:glowing_tidestone')
                elif y >= top - 1:
                    s.set(x0 + dx, y, z0 + dz, 'raft:chiseled_tidestone')
                else:
                    s.set(x0 + dx, y, z0 + dz, 'raft:tidestone_pillar[axis=y]')
    # stair plinth around the base
    for (dx, dz, f) in ((0, -2, 'south'), (0, 2, 'north'), (-2, 0, 'east'), (2, 0, 'west')):
        if s.free(x0 + dx, y0, z0 + dz):
            s.set(x0 + dx, y0, z0 + dz, f'raft:tidestone_brick_stairs[facing={f},half=bottom]')
            s.dry.add((x0 + dx, y0, z0 + dz))


def pillars(s):
    for k in range(12):
        a = math.radians(15 + 30 * k)
        x0, z0 = round(math.cos(a) * 17.5), round(math.sin(a) * 17.5)
        if 15 + 30 * k in (255, 285):
            statue(s, x0, FLOOR_Y + 1, z0, 'south', mirror=x0 > 0)
            for dx in (-1, 0, 1):
                for dz in (-1, 0, 1):
                    s.dry.add((x0 + dx, FLOOR_Y + 1, z0 + dz))
            continue
        # top: highest y still inside the dome at the column's outer edge
        top = FLOOR_Y + 1
        while inner(x0, top + 1, z0) and top < 30:
            top += 1
        pillar(s, x0, z0, top)
        # hanging sea lantern chain from the dome above the inner ring (between pillars)
        b = math.radians(30 * k)
        hx, hz = round(math.cos(b) * 12), round(math.sin(b) * 12)
        ytop = FLOOR_Y + 1
        while inner(hx, ytop + 1, hz):
            ytop += 1
        length = 4 + (k % 3)
        for y in range(ytop, ytop - length, -1):
            s.set(hx, y, hz, 'minecraft:iron_chain[axis=y]')
        s.set(hx, ytop - length, hz, 'minecraft:sea_lantern')


def drum_wall(s):
    """Inside face of the drum: a ring of stone benches at the foot of the wall and glowing rune plaques on every
    rib (interior positions only, the shell behind stays whole)."""
    for x in range(-22, 23):
        for z in range(-22, 23):
            r = rad(x, z)
            if not (20.6 < r <= IN_RX) or abs(x) <= 3:
                continue
            y = FLOOR_Y + 1
            if s.name_at(x, y, z) != 'minecraft:air':
                continue
            a = ang(x, z)
            out = max((('east', math.cos(math.radians(a))), ('west', -math.cos(math.radians(a))),
                       ('south', math.sin(math.radians(a))), ('north', -math.sin(math.radians(a)))),
                      key=lambda t: t[1])[0]
            s.set(x, y, z, f'raft:tidestone_brick_stairs[facing={out},half=bottom]')
    for k in range(12):
        a = math.radians(30 * k)
        if k in (3, 9):            # above the entrance and the reward corridor
            continue
        for rr in (21.3, 21.0):
            x, z = round(math.cos(a) * rr), round(math.sin(a) * rr)
            if s.name_at(x, FLOOR_Y + 3, z) == 'minecraft:air':
                for y in (FLOOR_Y + 3, FLOOR_Y + 4):
                    s.set(x, y, z, 'raft:glowing_tidestone' if y == FLOOR_Y + 3 else 'raft:chiseled_tidestone')
                break


def altar_ring(s):
    ax, ay, az = ALTAR
    # dais inlay around the altar (floor stays flush; the altar stands on the floor, contract 9.2)
    for dx in range(-2, 3):
        for dz in range(-1, 2):
            s.set(ax + dx, FLOOR_Y, az + dz, 'minecraft:raw_gold_block' if (dx, dz) == (0, 0) else
                  'raft:chiseled_tidestone')
    s.set(ax, ay, az, 'raft:leviathan_altar[active=false,facing=north]')
    # four sigil pedestals in a crescent behind the altar, each on a themed inlay with a light behind it
    themes = {(-7, 8): 'raft:coral_stone_bricks', (-4, 11): 'minecraft:smooth_quartz',
              (4, 11): 'minecraft:waxed_oxidized_cut_copper', (7, 8): 'minecraft:shroomlight'}
    for (px, pz), inlay in themes.items():
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                s.set(px + dx, FLOOR_Y, pz + dz, inlay if dx == 0 or dz == 0 else 'raft:chiseled_tidestone')
        s.set(px, FLOOR_Y + 1, pz, 'raft:sigil_pedestal')
        # small light column one step outward
        ox = px + (1 if px > 0 else -1)
        oz = pz + 1
        s.set(ox, FLOOR_Y + 1, oz, 'raft:chiseled_tidestone')
        s.set(ox, FLOOR_Y + 2, oz, 'raft:glowing_tidestone')
        s.set(ox, FLOOR_Y + 3, oz, 'raft:tidestone_brick_slab[type=bottom]')


def statue(s, x0, y0, z0, facing, mirror=False):
    """Tidewarden guardian statue (8 tall) looking towards `facing` (north/south); mirror puts the trident left."""
    f = 1 if facing == 'south' else -1
    m = -1 if mirror else 1

    def P(dx, dy, dz, st):
        if mirror and 'facing=east' in st:
            st = st.replace('facing=east', 'facing=west')
        elif mirror and 'facing=west' in st:
            st = st.replace('facing=west', 'facing=east')
        s.set(x0 + dx * m, y0 + dy, z0 + dz * f, st)
    back, front = 'north' if f == 1 else 'south', facing
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            P(dx, 0, dz, 'raft:chiseled_tidestone')
    for dy in (1, 2):
        for dx in (-1, 0, 1):
            P(dx, dy, 0, 'raft:tidestone_bricks')
            P(dx, dy, -1, 'raft:tidestone_bricks')
        P(-1, dy, 1, f'raft:tidestone_brick_stairs[facing={back},half=bottom]')
        P(1, dy, 1, f'raft:tidestone_brick_stairs[facing={back},half=bottom]')
        P(0, dy, 1, 'raft:tidestone_bricks' if dy == 1 else 'raft:chiseled_tidestone')
    P(0, 3, 0, 'raft:tidestone_bricks'); P(0, 3, -1, 'raft:tidestone_bricks')
    P(-1, 3, 0, 'raft:tidestone_bricks'); P(1, 3, 0, 'raft:tidestone_bricks')
    P(-1, 3, 1, 'raft:tidestone_brick_wall'); P(1, 3, 1, 'raft:tidestone_brick_wall')  # forearms
    P(0, 4, 0, 'raft:tidestone_bricks'); P(0, 4, -1, 'raft:tidestone_brick_slab[type=bottom]')
    P(-1, 4, 0, f'raft:tidestone_brick_stairs[facing=east,half=bottom]')
    P(1, 4, 0, f'raft:tidestone_brick_stairs[facing=west,half=bottom]')
    P(0, 5, 0, 'raft:chiseled_tidestone')                   # head
    P(0, 5, 1, 'raft:glowing_tidestone')                    # glowing face
    P(0, 6, 0, f'raft:tidestone_brick_stairs[facing={front},half=bottom]')  # hood
    # trident in the right hand
    for dy in range(1, 8):
        P(2, dy, 1, 'minecraft:prismarine_wall')
    P(2, 8, 1, 'minecraft:lightning_rod[facing=up,powered=false]')
    P(1, 7, 1, 'minecraft:lightning_rod[facing=up,powered=false]')
    P(3, 7, 1, 'minecraft:lightning_rod[facing=up,powered=false]')


def entrance(s, rnd):
    """Gatehouse (flooded) with the 5x5 tide seal, dry passage into the dome, grand stair and causeway."""
    # unbreakable frame around the seal, merged with the drum
    s.fill(-4, FLOOR_Y, 20, 4, FLOOR_Y + 7, SEAL_Z, SANCTUM)
    # dry passage x -2..2, y 13..17, z 20..23 (opens into the dome)
    s.air(-2, FLOOR_Y + 1, 19, 2, FLOOR_Y + 5, SEAL_Z - 1)
    for x in range(-2, 3):
        s.set(x, FLOOR_Y, 20, 'raft:chiseled_tidestone')
        for z in range(21, SEAL_Z):
            s.set(x, FLOOR_Y, z, 'raft:tidestone_bricks' if x else 'raft:glowing_tidestone')
    # the seal
    for x in range(-2, 3):
        for y in range(FLOOR_Y + 1, FLOOR_Y + 6):
            s.set(x, y, SEAL_Z, SEAL)
    # gatehouse body z 25..34, x -8..8, up to y 26 (decorative, outside the seal)
    gx1, gx2, gz1, gz2 = -8, 8, SEAL_Z + 1, 34
    for x in range(gx1, gx2 + 1):
        for z in range(gz1, gz2 + 1):
            for y in range(FLOOR_Y + 1, 27):
                tower = abs(x) >= 4
                if not tower and y > 22:
                    continue
                if tower and y > 26 - (1 if abs(x) == 8 else 0) - (1 if z in (gz1, gz2) else 0):
                    continue
                s.set(x, y, z, pick(rnd, SKIN))
    # flooded vestibule x -3..3, y 13..19, z 25..34 (open at the front)
    s.water(-3, FLOOR_Y + 1, gz1, 3, FLOOR_Y + 7, gz2)
    for z in range(gz1, gz2 + 1):
        for x in range(-3, 4):
            s.set(x, FLOOR_Y, z, 'raft:chiseled_tidestone' if x == 0 and z % 2 == 0 else
                  pick(rnd, [('raft:tidestone_bricks', 6), ('raft:cracked_tidestone_bricks', 2),
                             ('raft:mossy_tidestone_bricks', 2)]))
    # ribbed vault and columns inside the vestibule
    for z in range(gz1 + 1, gz2, 3):
        for side in (-3, 3):
            for y in range(FLOOR_Y + 1, FLOOR_Y + 7):
                s.set(side, y, z, 'raft:tidestone_pillar[axis=y]' if y != FLOOR_Y + 4 else 'raft:glowing_tidestone')
            s.set(side, FLOOR_Y + 7, z, 'raft:chiseled_tidestone')
        for x in range(-2, 3):
            s.set(x, FLOOR_Y + 7, z, 'raft:tidestone_brick_slab[type=top]')
    # rune frieze around the seal on the gatehouse side
    for y in range(FLOOR_Y + 1, FLOOR_Y + 8):
        for x in (-3, 3):
            s.set(x, y, gz1, 'raft:chiseled_tidestone' if y % 2 else 'raft:glowing_tidestone')
    for x in range(-3, 4):
        s.set(x, FLOOR_Y + 6, gz1, 'raft:chiseled_tidestone' if x % 2 else 'raft:glowing_tidestone')
        s.set(x, FLOOR_Y + 7, gz1, 'raft:chiseled_tidestone')
    # front arch: pointed opening, upper courses chiseled, lintel with glowing runes
    for x in range(-4, 5):
        s.set(x, FLOOR_Y + 8, gz2, 'raft:chiseled_tidestone' if x % 2 else 'raft:glowing_tidestone')
    for y in range(FLOOR_Y + 1, FLOOR_Y + 8):
        for x in (-4, 4):
            s.set(x, y, gz2, 'raft:tidestone_pillar[axis=y]')
    # tower crowns (silhouette from afar)
    for sx in (-6, 6):
        for dx in (-2, -1, 0, 1, 2):
            for dz in (-2, -1, 0, 1, 2):
                x, z = sx + dx, 30 + dz
                if abs(dx) == 2 or abs(dz) == 2:
                    if (dx + dz) % 2 == 0:
                        s.set(x, 27, z, 'raft:tidestone_brick_wall')
                else:
                    s.set(x, 26, z, 'raft:chiseled_tidestone')
        s.set(sx, 27, 30, 'raft:glowing_tidestone')
        s.set(sx, 28, 30, 'raft:glowing_tidestone')
        s.set(sx, 29, 30, 'minecraft:sea_lantern')
        s.set(sx, 30, 30, 'raft:tidestone_brick_slab[type=bottom]')
        for y in (20, 23):
            for z in range(gz1 + 1, gz2):
                s.set(sx + (2 if sx > 0 else -2), y, z, 'raft:glowing_tidestone' if z % 3 == 0 else
                      'raft:chiseled_tidestone')
    # statues flanking the gate on the podium
    statue(s, -6, FLOOR_Y + 1, 37, 'south')
    statue(s, 6, FLOOR_Y + 1, 37, 'south', mirror=True)
    # podium front platform z 35..39 at y 12
    s.fill(-10, 0, 35, 10, FLOOR_Y, 39, SANCTUM)
    # grand stair: from y=12 at z=40 down to y=1, 9 wide, with balustrades
    for k in range(FLOOR_Y + 1):
        z = 40 + k
        top = FLOOR_Y - k
        for x in range(-6, 7):
            for y in range(0, top):
                s.set(x, y, z, pick(rnd, SKIN))
            if abs(x) <= 4:
                s.set(x, top, z, 'raft:tidestone_brick_stairs[facing=north,half=bottom]' if top > 0 else
                      'raft:tidestone_bricks')
            else:
                s.set(x, top, z, pick(rnd, SKIN))
                s.set(x, top + 1, z, 'raft:tidestone_brick_wall' if abs(x) == 6 else None)
    # causeway with obelisks to z=60
    for z in range(53, 61):
        for x in range(-4, 5):
            s.set(x, 0, z, 'raft:chiseled_tidestone' if x == 0 else pick(rnd, SKIN))
    for z in (44, 50, 56, 60):
        for sx in (-6, 6) if z < 53 else (-5, 5):
            base = max(0, FLOOR_Y - (z - 40)) + 1 if z < 53 else 1
            for y in range(base, base + 5):
                s.set(sx, y, z, 'raft:tidestone_pillar[axis=y]' if y < base + 4 else 'raft:chiseled_tidestone')
            s.set(sx, base + 5, z, 'raft:glowing_tidestone')
            s.set(sx, base + 6, z, 'raft:tidestone_brick_slab[type=bottom]')
    # bubble vents beside the stair foot: divers can refill their tanks before the fight
    for (x, z) in ((-8, 53), (8, 53)):
        s.set(x, 0, z, 'raft:bubble_vent')
    # open water in front of the gate (never buried by terrain)
    s.water(-5, 1, 53, 5, 6, 60, only_empty=True)


def reward_room(s, rnd):
    """North annex: corridor from the dome, vaulted treasure chamber (sealed, dry)."""
    # shell of the annex (sanctum), y 12..22, x -9..9, z -40..-21
    for x in range(-9, 10):
        for z in range(-40, -20):
            for y in range(FLOOR_Y, 23):
                if rad(x, z) <= IN_RX and inner(x, y, z):
                    continue
                if y >= 21 and (abs(x) >= 8 or z <= -39):
                    continue
                s.set(x, y, z, SANCTUM)
    # corridor x -2..2, y 13..17, z -25..-19
    s.air(-2, FLOOR_Y + 1, -26, 2, FLOOR_Y + 5, -19)
    for z in range(-26, -18):
        for x in range(-2, 3):
            s.set(x, FLOOR_Y, z, 'raft:glowing_tidestone' if x == 0 and z % 2 == 0 else 'raft:tidestone_bricks')
    for z in (-24, -21):
        for x in (-2, 2):
            for y in range(FLOOR_Y + 1, FLOOR_Y + 5):
                s.set(x, y, z, 'raft:tidestone_pillar[axis=y]')
            s.set(x, FLOOR_Y + 5, z, 'raft:chiseled_tidestone')
    # chamber x -6..6, z -37..-27, y 13..20 with a stepped vault
    s.air(-6, FLOOR_Y + 1, -37, 6, FLOOR_Y + 6, -27)
    s.air(-5, FLOOR_Y + 7, -37, 5, FLOOR_Y + 7, -27)
    s.air(-3, FLOOR_Y + 8, -37, 3, FLOOR_Y + 8, -27)
    for z in range(-37, -26):
        for x in range(-6, 7):
            s.set(x, FLOOR_Y, z, 'raft:chiseled_tidestone' if (x + z) % 4 == 0 else pick(rnd, FLOOR))
        s.set(-6, FLOOR_Y + 7, z, 'raft:tidestone_brick_stairs[facing=east,half=top]')
        s.set(6, FLOOR_Y + 7, z, 'raft:tidestone_brick_stairs[facing=west,half=top]')
        s.set(-4, FLOOR_Y + 8, z, 'raft:tidestone_brick_stairs[facing=east,half=top]')
        s.set(4, FLOOR_Y + 8, z, 'raft:tidestone_brick_stairs[facing=west,half=top]')
        s.set(-5, FLOOR_Y + 8, z, 'raft:tidestone_bricks')
        s.set(5, FLOOR_Y + 8, z, 'raft:tidestone_bricks')
        if z % 3 == 0:
            s.set(0, FLOOR_Y + 9, z, 'raft:glowing_tidestone')
    for (x, y, z) in [(x, y, z) for x in range(-6, 7) for y in range(FLOOR_Y + 7, FLOOR_Y + 9) for z in range(-37, -26)]:
        if s.name_at(x, y, z) == 'minecraft:air':
            s.dry.add((x, y, z))
    # wall columns with glowing bands
    for z in (-36, -32, -28):
        for x in (-6, 6):
            for y in range(FLOOR_Y + 1, FLOOR_Y + 7):
                s.set(x, y, z, 'raft:glowing_tidestone' if y == FLOOR_Y + 4 else 'raft:tidestone_pillar[axis=y]')
    # mural on the back wall z=-38: the Leviathan coiled around the Heart
    mural = ['..wwwww..',
             '.w.....w.',
             'w..ggg..w',
             'w.g.H.g.w',
             'w..ggg..w',
             '.w.....ww',
             '..wwwww.e']
    for i, row in enumerate(mural):
        for j, c in enumerate(row):
            x, y = -4 + j, FLOOR_Y + 7 - i
            st = {'w': 'minecraft:warped_wart_block', 'g': 'minecraft:raw_gold_block', 'H': 'minecraft:sea_lantern',
                  'e': 'raft:glowing_tidestone', '.': 'minecraft:dark_prismarine'}[c]
            s.set(x, y, -38, st)
    # dais with the reward chest
    for x in range(-3, 4):
        for z in range(-37, -33):
            s.set(x, FLOOR_Y + 1, z, 'raft:tidestone_brick_slab[type=bottom]' if z == -34 else 'raft:chiseled_tidestone')
    for x in (-3, 3):
        s.set(x, FLOOR_Y + 2, -37, 'raft:glowing_tidestone')
        s.set(x, FLOOR_Y + 3, -37, 'raft:tidestone_brick_slab[type=bottom]')
    s.chest(0, FLOOR_Y + 2, -36, 'raft:chests/sanctum_reward', facing='south')
    for (x, z, n) in ((-1, -36, 4), (1, -36, 6), (-2, -35, 2), (2, -35, 3), (-1, -35, 1)):
        s.set(x, FLOOR_Y + 2, z, f'raft:gold_coin_pile[layers={n}]')
    # treasure along the walls: Tidewarden offerings (the main reward is the chest on the dais)
    for (x, z, f) in ((-5, -30, 'east'), (5, -30, 'west'), (-5, -34, 'east'), (5, -34, 'west')):
        s.chest(x, FLOOR_Y + 1, z, 'raft:chests/coral_temple_offering', facing=f, block='raft:treasure_chest')
        s.set(x, FLOOR_Y + 1, z + 1, f'raft:gold_coin_pile[layers={2 + (z % 3)}]')
    # the empty socket of the Heart of the Tide
    s.set(0, FLOOR_Y + 3, -37, 'raft:chiseled_tidestone')
    s.set(0, FLOOR_Y + 4, -37, 'minecraft:conduit')
    s.set(-1, FLOOR_Y + 3, -37, 'raft:glowing_tidestone')
    s.set(1, FLOOR_Y + 3, -37, 'raft:glowing_tidestone')
    # entrance arch on the dome side
    for x in range(-3, 4):
        s.set(x, FLOOR_Y + 6, -19, 'raft:chiseled_tidestone' if x % 2 else 'raft:glowing_tidestone')
    for y in range(FLOOR_Y + 1, FLOOR_Y + 6):
        for x in (-3, 3):
            s.set(x, y, -19, 'raft:tidestone_pillar[axis=y]')
    # annex roof: small dome of sanctum glass with ribs
    for x in range(-7, 8):
        for z in range(-38, -24):
            for y in range(23, 27):
                d = math.sqrt((x / 7.5) ** 2 + ((z + 31) / 7.5) ** 2 + ((y - 22) / 4.6) ** 2)
                if d <= 1.0:
                    s.set(x, y, z, SANCTUM if (x == 0 or z == -31 or y == 23) else SGLASS)


def braziers(s):
    for k in range(8):
        a = math.radians(22.5 + 45 * k)
        x, z = round(math.cos(a) * 20), round(math.sin(a) * 20)
        if s.name_at(x, FLOOR_Y + 1, z) == 'minecraft:air':
            s.set(x, FLOOR_Y + 1, z, 'raft:brazier[lit=true]')


def pylons(s, rnd):
    """Four tall corner pylons on the middle tier with glowing crowns and binding chains."""
    for (sx, sz) in ((-19, -19), (19, -19), (-19, 19), (19, 19)):
        for x in range(sx - 2, sx + 3):
            for z in range(sz - 2, sz + 3):
                for y in range(5, 29):
                    edge = abs(x - sx) == 2 or abs(z - sz) == 2
                    corner = abs(x - sx) == 2 and abs(z - sz) == 2
                    if corner and y > 24:
                        continue
                    if inner(x, y, z) or (outer(x, y, z) and y >= FLOOR_Y):
                        continue
                    if not edge:
                        s.set(x, y, z, SANCTUM if y < 24 else 'raft:tidestone_bricks')
                    else:
                        s.set(x, y, z, 'raft:tidestone_pillar[axis=y]' if corner else pick(rnd, SKIN))
        for y in (12, 18, 24):
            for (dx, dz) in ((2, 0), (-2, 0), (0, 2), (0, -2)):
                if not outer(sx + dx, y, sz + dz):
                    s.set(sx + dx, y, sz + dz, 'raft:glowing_tidestone')
        s.set(sx, 29, sz, 'raft:chiseled_tidestone')
        s.set(sx, 30, sz, 'raft:glowing_tidestone')
        s.set(sx, 31, sz, 'minecraft:sea_lantern')
        s.set(sx, 32, sz, 'minecraft:lightning_rod[facing=up,powered=false]')
        for (dx, dz) in ((1, 1), (-1, 1), (1, -1), (-1, -1)):
            s.set(sx + dx, 29, sz + dz, 'raft:tidestone_brick_slab[type=bottom]')
        # chains hanging from the crown towards the dome (outside faces)
        ox = sx - (3 if sx > 0 else -3)
        oz = sz - (3 if sz > 0 else -3)
        for y in range(18, 28):
            if s.name_at(ox, y, sz) is None:
                s.set(ox, y, sz, 'minecraft:iron_chain[axis=y]')
            if s.name_at(sx, y, oz) is None:
                s.set(sx, y, oz, 'minecraft:iron_chain[axis=y]')


def overgrowth(s, rnd):
    """Trench life on the outside: barnacles, tube worms, glowing anemones (never inside the shell)."""
    (x0, y0, z0), (x1, y1, z1) = s.bounds()
    s.grow_barnacles(((x0, 1, z0), (x1, 9, z1)), 0.07, seed=7, faces=('north', 'south', 'east', 'west'),
                     skip=lambda x, y, z: rad(x, z) <= POOL_R + 1.5)
    cands = []
    for (x, y, z), b in list(s.blocks.items()):
        if y > 14 or not b[0].startswith('raft:') or not is_full(b[0]):
            continue
        if not s.wet_free(x, y + 1, z) or rad(x, z) < 24.5 and y >= FLOOR_Y:
            continue
        cands.append((x, y + 1, z))
    rnd.shuffle(cands)
    for (x, y, z) in cands[:90]:
        r = rnd.random()
        if r < 0.45:
            s.set(x, y, z, 'raft:tube_worms')
        elif r < 0.75:
            s.set(x, y, z, 'raft:sea_anemone_glow')
        elif r < 0.9:
            s.set(x, y, z, 'raft:sea_anemone_purple')
        else:
            s.set(x, y, z, 'minecraft:sea_pickle[pickles=3]')


def build(seed=31415962):
    rnd = random.Random(seed)
    s = Deep(SID, seed)
    plinth(s, rnd)
    dome(s, rnd)
    interior(s, rnd)
    pillars(s)
    drum_wall(s)
    altar_ring(s)
    braziers(s)
    entrance(s, rnd)
    reward_room(s, rnd)
    pylons(s, rnd)
    skin(s, rnd)
    overgrowth(s, rnd)
    s.finalize()
    return s


def verify(s):
    """Contract and safety checks; returns a dict printed by gen.py."""
    rep = {}
    ax, ay, az = ALTAR
    alt = s.blocks.get(ALTAR)
    rep['altar'] = (ALTAR, alt[0] if alt else None, alt[1] if alt else None)
    centre = (ax, ay, az - 9)
    rep['pool_centre'] = centre
    rep['boss_spawn'] = (ax, ay - 4, az - 9)
    rep['boss_spawn_block'] = s.get(ax, ay - 4, az - 9)
    rep['pool_depth'] = sum(1 for y in range(0, FLOOR_Y + 1) if s.name_at(0, y, 0) == 'minecraft:water')
    rep['pool_radius_ok'] = all(s.name_at(x, FLOOR_Y, z) == 'minecraft:water'
                                for x in range(-7, 8) for z in range(-7, 8) if rad(x, z) <= 7)
    rep['altar_floor_below'] = s.name_at(ax, ay - 1, az)
    seal = [p for p, b in s.blocks.items() if b[0] == 'raft:tide_seal']
    rep['seal_blocks'] = len(seal)
    inside = set(s.dry) | {p for p, b in s.blocks.items() if b[0] == 'minecraft:water' and rad(p[0], p[2]) <= 8
                           and p[1] <= FLOOR_Y}
    rep['dry_blocks'] = len(s.dry)
    rep['breach_sealed'] = len(enclosure_check(s, inside, UNBREAKABLE))
    rep['breach_seal_open'] = len(enclosure_check(s, inside, UNBREAKABLE - {'raft:tide_seal'}))
    rep['leaks'] = leak_check(s)[:10]
    return rep


def write_json_files():
    structure_json(SID, f'raft:{SID}/start', 'surface_structures', 'beard_thin', -1, 'OCEAN_FLOOR_WG', size=1,
                   max_distance=80, spawn_overrides=NO_SPAWNS)
    structure_set(SID, 60, 24, 31415962)
    template_pool(f'raft:{SID}/start', [(f'raft:{SID}/sanctum', 1, 'minecraft:empty')])
    biome_tag(SID, ['abyssal_trench', 'raft_ocean_biome'])


def main():
    s = build()
    s.save(MODULE, f'{SID}/sanctum')
    write_json_files()
    return s


def preview(s):
    from .common import pv
    return pv(s, 'sanctum', cuts=(13, 20), slices_y=[1, 6, 12, 13, 16, 19, 24, 30])
