"""Titanium set, machines (furnaces, purifier, quarry, chunk destroyer, battery, solar panel, grill), plastic and
scrap blocks of 2.0."""
import math
import random

from content.building import tex_wood_stone as TW
from .common import *  # noqa: F401,F403

CLEAR = (0, 0, 0, 0)


# ================================================================================================ titanium
ORE = [  # titanium flecks: 7/6 lit facets, 5-3 body, 2 shadow; s = darkened stone around the vein
    '................',
    '.....s..........',
    '..ss76s.........',
    '.s7654s.....s...',
    '.s5s43s....s7s..',
    '..s.ss....s654s.',
    '..........s5532s',
    '...........s3s..',
    '....s.......s...',
    '...s76s.........',
    '..s6s54s....ss..',
    '..s5433s...s76s.',
    '...s22s...s6543s',
    '....s......ss2s.',
    '..............s.',
    '................',
]
ORE_DEEP = [
    '................',
    '..........ss....',
    '...s.....s76s...',
    '..s76s...s5s4s..',
    '.s6543s...s32s..',
    '..s32s.....s....',
    '...ss...........',
    '.........s......',
    '........s76s....',
    '..s....s6544s...',
    '.s7s....s432s...',
    '.s54s....ss.....',
    '..s3ss......s...',
    '...s.......s76s.',
    '...........s43s.',
    '............ss..',
]


def ore(base_rel, pattern, palette=TITAN):
    img = vanilla(base_rel)
    for y, r in enumerate(pattern):
        for x, ch in enumerate(r):
            if ch == 's':
                img.putpixel((x, y), darker(img.getpixel((x, y)), 0.72))
            elif ch.isdigit():
                img.putpixel((x, y), palette[int(ch)])
    return img


def darker(c, f):
    return (round(c[0] * f), round(c[1] * f), round(c[2] * f + 4), 255)


def titanium_ore():
    return ore('block/stone', ORE)


def deepslate_titanium_ore():
    return ore('block/deepslate', ORE_DEEP, TITAN)


def titanium_block():
    """One bevelled titanium plate per block (like vanilla iron_block reads as one block): lit top-left lip,
    shadowed bottom-right lip, two soft diagonal sheen bands, corner rivets."""
    p = TITAN
    img = new()
    for y in range(16):
        for x in range(16):
            s = x + y
            t = 5
            if 7 <= s <= 9 or 19 <= s <= 20:
                t = 6                                  # sheen
            if s >= 25:
                t = 4
            put(img, x, y, p[t])
    for i in range(16):
        put(img, i, 0, p[7] if i < 12 else p[6]); put(img, 0, i, p[7] if i < 12 else p[6])
        put(img, i, 15, p[3]); put(img, 15, i, p[3])
        if 0 < i < 15:
            put(img, i, 1, p[6]); put(img, 1, i, p[6]); put(img, i, 14, p[4]); put(img, 14, i, p[4])
    for (x, y) in ((3, 3), (12, 3), (3, 12), (12, 12)):
        put(img, x, y, p[7]); put(img, x + 1, y + 1, p[3]); put(img, x + 1, y, p[5]); put(img, x, y + 1, p[5])
    return img


def raw_titanium_block():
    """Like vanilla raw_iron_block: lumpy chunks (height noise, lit from the top-left), soft crevices."""
    p = TITAN
    h = tex.value_noise(16, 16, 77, cell=4, octaves=2)
    img = new()
    for y in range(16):
        for x in range(16):
            v = emboss(h, x, y, k=1.6)
            t = 2 + int((v - 0.2) * 5.5)
            if h[y][x] < 0.3:
                t = min(t, 3)                           # crevice
            put(img, x, y, p[max(2, min(7, t))])
    return img


def titanium_grates():
    """Titanium grate: bevelled plate with four cross-shaped openings (the 2.0 motif), shaded rims."""
    p = TITAN
    img = new()
    for y in range(16):
        for x in range(16):
            put(img, x, y, p[5] if (x + y) < 14 else p[4])
    for i in range(16):
        put(img, i, 0, p[6]); put(img, 0, i, p[6]); put(img, i, 15, p[2]); put(img, 15, i, p[2])
    holes = set()
    for cx in (4, 11):
        for cy in (4, 11):
            holes |= {(cx + d, cy) for d in (-2, -1, 0, 1, 2)} | {(cx, cy + d) for d in (-2, -1, 1, 2)}
    for (x, y) in holes:
        put(img, x, y, CLEAR)
    for (x, y) in holes:            # rim: shadow on the top/left lip, light on the bottom/right lip
        for (dx, dy, c) in ((1, 0, p[7]), (0, 1, p[7]), (-1, 0, p[3]), (0, -1, p[3])):
            q = (x + dx, y + dy)
            if q not in holes:
                put(img, q[0], q[1], c)
    for (x, y) in ((7, 7), (8, 8)):
        put(img, x, y, p[6] if x == 7 else p[3])
    return img


def titanium_cable(old):
    return remap(old, TITAN[1:7])


def _plate(p, lo=3, hi=4, seed=1, rivets=((2, 2), (13, 2), (2, 13), (13, 13)), frame=True):
    """Machine casing: plate with soft tone areas, 1-px bevel, rivets with a lit top-left."""
    img = new()
    n = noise(seed, cell=8, octaves=1)
    soft = lerp(p[lo], p[hi], 0.5)
    for y in range(16):
        for x in range(16):
            put(img, x, y, soft if n[y][x] > 0.58 else p[lo])
    if frame:
        for i in range(16):
            put(img, i, 0, p[hi + 1]); put(img, 0, i, p[hi + 1]); put(img, i, 15, p[lo - 2]); put(img, 15, i, p[lo - 2])
        for i in range(1, 15):
            put(img, i, 1, p[hi]); put(img, 1, i, p[hi]); put(img, i, 14, p[lo - 1]); put(img, 14, i, p[lo - 1])
    for (x, y) in rivets:
        put(img, x, y, p[hi + 2] if hi + 2 < len(p) else p[-1]); put(img, x + 1, y + 1, p[lo - 2])
        put(img, x + 1, y, p[lo - 1]); put(img, x, y + 1, p[lo - 1])
    return img


def _slots(img, p, x0, y0, n, w=1, h=4, gap=2):
    """Vent slots: dark slot, lit lower edge."""
    for i in range(n):
        x = x0 + i * (w + gap)
        fill(img, x, y0, x + w - 1, y0 + h - 1, DARK[1])
        for xx in range(x, x + w):
            put(img, xx, y0 + h, p[-2])
        put(img, x + w, y0, p[1])


def titanium_furnace_side():
    p = TITAN
    img = _plate(p, 3, 4, seed=3)
    _slots(img, p, 3, 5, 4, w=1, h=5, gap=2)
    return img


def titanium_furnace_top():
    p = TITAN
    img = _plate(p, 3, 4, seed=4)
    for y in range(4, 12):                              # round exhaust grille
        for x in range(4, 12):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 3.6:
                put(img, x, y, DARK[1] if (x + y) % 2 else p[2])
            elif d < 4.4:
                put(img, x, y, p[2] if x + y < 15 else p[6])
    return img


def _furnace_front(fire):
    p = TITAN
    img = _plate(p, 3, 4, seed=5, rivets=((2, 2), (13, 2)))
    # firebox: arched opening rows 3-8
    box = ['..xxxxxxxx..', '.xxxxxxxxxx.', '.xxxxxxxxxx.', '.xxxxxxxxxx.', '.xxxxxxxxxx.']
    for yy, r in enumerate(box):
        for xx, ch in enumerate(r):
            if ch == 'x':
                put(img, 2 + xx, 4 + yy, DARK[0] if not fire else fire(xx, yy, 'box'))
    for xx in range(1, 11):
        put(img, 2 + xx, 9, p[6])                       # lit sill
    for xx in range(3, 9):
        put(img, 2 + xx, 3, p[1])                       # shadow under the arch top
    # ash slot rows 11-12
    fill(img, 4, 11, 11, 12, DARK[1])
    if fire:
        for x in range(4, 12):
            put(img, x, 12, fire(x, 0, 'slot'))
    for x in range(4, 12):
        put(img, x, 13, p[6])
    return img


def _fire(frame):
    def f(x, y, part):
        if part == 'slot':
            return EMBER[3 + (x + frame) % 2]
        # flames rise from the bottom of the box (y 0 top .. 4 bottom)
        h = [2, 3, 4, 3, 2, 3, 4, 3, 2, 3, 4, 3][(x + frame * 2) % 12]
        if 4 - y < h - 1:
            return EMBER[[6, 5, 5, 4][(4 - y + frame) % 4]]
        if 4 - y < h:
            return EMBER[3]
        return EMBER[1] if y > 1 else DARK[0]
    return f


def titanium_furnace_front():
    return _furnace_front(None)


def titanium_furnace_front_on():
    return [_furnace_front(_fire(i)) for i in range(4)]


titanium_furnace_front_on.frametime = 3


# ================================================================================================ water purifier
def purifier_side():
    p = COPPER
    img = _plate(p, 3, 4, seed=7)
    for x in range(2, 14):                              # plate seam
        put(img, x, 7, p[1]); put(img, x, 8, p[6])
    return img


def purifier_bottom():
    return _plate(COPPER, 3, 4, seed=8, rivets=((7, 7),))


def purifier_top():
    p = COPPER
    img = _plate(p, 3, 4, seed=9)
    for y in range(3, 13):
        for x in range(3, 13):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 3.2:
                t = 3 if x + y < 13 else 4
                if (x, y) in ((6, 6), (7, 6)):
                    t = 6
                put(img, x, y, WATER[t])
            elif d < 4.2:
                put(img, x, y, BRASS[4] if x + y < 15 else BRASS[1])
            elif d < 4.9:
                put(img, x, y, p[1] if x + y < 15 else p[5])
    return img


def _purifier_front(frame=None):
    p = COPPER
    img = _plate(p, 3, 4, seed=10, rivets=((2, 2), (13, 2), (2, 13), (13, 13)))
    # glass tank x 3-12, y 2-10
    for y in range(2, 11):
        for x in range(3, 13):
            if y < 4:
                c = GLASS_BLUE[2]                           # air above the water
            else:
                c = WATER[4] if y == 4 else WATER[3] if y < 7 else WATER[2]
            put(img, x, y, c)
    for (x, y) in ((4, 3), (5, 3), (4, 5), (4, 6), (4, 7)):      # glass glint
        put(img, x, y, GLASS_BLUE[3])
    for x in range(3, 13):
        put(img, x, 1, p[1]); put(img, x, 11, p[6])
    for y in range(2, 11):
        put(img, 2, y, p[1]); put(img, 13, y, p[6])
    if frame is not None:                               # rising bubbles
        for (bx, by0) in ((7, 10), (10, 8), (6, 6)):
            y = 10 - ((10 - by0 + frame * 2) % 7)
            if 4 < y < 11:
                put(img, bx, y, WATER[6]); put(img, bx + 1, y + 1, WATER[5])
    # spout / drain below the tank
    fill(img, 6, 12, 9, 13, IRON[1])
    put(img, 6, 12, IRON[3]); put(img, 7, 12, IRON[3])
    if frame is not None:                               # little flame heating the still
        for x in range(6, 10):
            put(img, x, 13, EMBER[4 + (x + frame) % 3])
        put(img, 7, 12, EMBER[6] if frame % 2 else EMBER[5])
    return img


def purifier_front():
    return _purifier_front()


def purifier_front_on():
    return [_purifier_front(i) for i in range(4)]


purifier_front_on.frametime = 4


# ================================================================================================ quarry, chunk destroyer
def _machine_bottom(p):
    img = _plate(p, 3, 4, seed=11)
    lights = [RED[4], (60, 170, 80, 255)]
    for i, (x, y) in enumerate(((7, 3), (10, 4), (12, 7), (11, 10), (8, 12), (5, 11), (3, 8), (4, 5))):
        put(img, x, y, lights[i % 2])
        put(img, x + 1, y + 1, p[2])
    fill(img, 5, 7, 10, 8, DARK[1])
    for x in range(5, 11):
        put(img, x, 9, p[6])
    return img


def _machine_front(p, screen):
    img = _plate(p, 3, 4, seed=12)
    fill(img, 3, 2, 12, 7, DARK[1])                     # screen
    for x in range(3, 13):
        put(img, x, 8, p[6]); put(img, x, 1, p[1])
    for y in range(2, 8):
        put(img, 2, y, p[1]); put(img, 13, y, p[6])
    wave = [5, 4, 3, 4, 5, 6, 5, 3, 4, 4]
    for i, x in enumerate(range(3, 13)):
        put(img, x, wave[i], screen)
    put(img, 3, 2, DARK[3])
    # buttons
    for (x, c) in ((4, RED), (10, pal('#1e4d25', '#2d6e35', '#3f9147', '#5cb35f', '#8fd28c'))):
        fill(img, x, 11, x + 1, 12, c[3])
        put(img, x, 11, c[4]); put(img, x + 1, 12, c[1])
        put(img, x, 13, p[6]); put(img, x + 1, 13, p[6])
    return img


def _machine_side(p):
    img = _plate(p, 3, 4, seed=13)
    for x in range(2, 14):
        put(img, x, 5, p[1]); put(img, x, 6, p[5])
    _slots(img, p, 3, 8, 5, w=1, h=3, gap=1)
    for x in (4, 8, 12):                                # warning lamps
        put(img, x, 13, RED[4]); put(img, x - 1, 13, RED[2])
    return img


def _machine_top(p):
    img = _plate(p, 3, 4, seed=14)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 2.2:
                put(img, x, y, DARK[0])
            elif d < 3.3:
                put(img, x, y, STEEL[2] if x + y < 15 else STEEL[5])
            elif d < 4.4:
                put(img, x, y, STEEL[5] if x + y < 15 else STEEL[2])
            elif d < 5.2:
                put(img, x, y, p[2])
    return img


# ================================================================================================ makeshift power
def battery_side():
    p = GALV
    img = _plate(p, 3, 4, seed=15)
    fill(img, 6, 2, 9, 13, DARK[1])                    # charge tube
    for y in range(5, 13):
        put(img, 7, y, RED[4]); put(img, 8, y, RED[3])
        put(img, 6, y, RED[2]); put(img, 9, y, RED[1])
    for y in range(2, 14):
        put(img, 5, y, p[1]); put(img, 10, y, p[6])
    for (x, y) in ((7, 6), (7, 7)):
        put(img, x, y, RED[5])
    # a scrap patch with rust
    fill(img, 11, 9, 13, 12, RUST[3])
    put(img, 11, 9, RUST[5]); put(img, 13, 12, RUST[1]); put(img, 12, 10, RUST[4])
    return img


def battery_top():
    p = GALV
    img = _plate(p, 3, 4, seed=16)
    for (cx, c) in ((5, RED), (10, pal('#262a30', '#363b43', '#4a515b', '#5f6873', '#7a8490', '#9aa5b0'))):
        fill(img, cx - 1, 6, cx + 1, 9, c[3])
        put(img, cx - 1, 6, c[5]); put(img, cx, 6, c[4]); put(img, cx + 1, 9, c[1]); put(img, cx + 1, 8, c[2])
        put(img, cx - 1, 10, p[1]); put(img, cx, 10, p[1]); put(img, cx + 1, 10, p[1])
    put(img, 5, 7, RED[5]); put(img, 4, 7, RED[5]); put(img, 6, 7, RED[5])   # + sign hint
    for x in range(5, 11):
        put(img, x, 12, ROPE[3])                           # wire between the terminals
    return img


def battery_bottom():
    return _plate(GALV, 3, 4, seed=17)


def solar_top():
    """Solar cells (navy, lit facets) in a grey aluminium frame, 3x3 cells."""
    img = new()
    a = GALV
    for i in range(16):
        for j in range(16):
            put(img, i, j, a[4])
    cell = pal('#16213f', '#1d2c55', '#253a6c', '#2f4a86', '#4a68a8', '#8aa7d6')
    for cy in range(3):
        for cx in range(3):
            x0, y0 = 1 + cx * 5, 1 + cy * 5
            for y in range(y0, y0 + 4):
                for x in range(x0, x0 + 4):
                    t = 2 if (x - x0) + (y - y0) > 2 else 3
                    put(img, x, y, cell[t])
            put(img, x0, y0, cell[5]); put(img, x0 + 1, y0, cell[4]); put(img, x0, y0 + 1, cell[4])
            put(img, x0 + 3, y0 + 3, cell[1])
            for x in range(x0, x0 + 4):                 # silver bus bar
                put(img, x, y0 + 2, cell[4] if x == x0 else lerp(cell[3], a[4], 0.35))
    for i in range(16):
        put(img, i, 0, a[5]); put(img, 0, i, a[5]); put(img, i, 15, a[2]); put(img, 15, i, a[2])
    return img


def solar_side():
    a = GALV
    img = _plate(a, 3, 4, seed=18, rivets=())
    for x in range(16):
        put(img, x, 2, a[5]); put(img, x, 3, pal('#1d2c55')[0]); put(img, x, 4, pal('#2f4a86')[0]); put(img, x, 5, a[1])
    for x in range(2, 14, 4):                           # struts
        for y in range(6, 15):
            put(img, x, y, a[5]); put(img, x + 1, y, a[2])
    for (x, y) in ((3, 12), (11, 12)):
        put(img, x, y, RED[4])
    return img


def solar_bottom():
    a = GALV
    img = _plate(a, 3, 4, seed=19)
    for i in range(2, 14):                              # cross brace
        put(img, i, i, a[5]); put(img, i, i + 1 if i < 13 else i, a[1])
        put(img, 15 - i, i, a[5]); put(img, 15 - i, i + 1 if i < 13 else i, a[1])
    return img


# ================================================================================================ brick furnace
def _clay_bricks(seed=21):
    """Running-bond clay bricks (8x4 incl. mortar), lit top/left edge of each brick."""
    rnd = random.Random(seed)
    img = new()
    for r in range(4):
        y0 = r * 4
        off = 4 * (r % 2)
        for b in range(3):
            x0 = b * 8 - off
            t = rnd.choice([3, 4, 4, 5])
            for yy in range(3):
                for xx in range(7):
                    tt = t
                    if yy == 0 or xx == 0:
                        tt = t + 1
                    if yy == 2 or xx == 6:
                        tt = t - 1
                    if rnd.random() < 0.08:
                        tt -= 1
                    put(img, x0 + xx, y0 + yy, CLAY[max(0, min(6, tt))])
            for xx in range(8):
                put(img, x0 + xx, y0 + 3, MORTAR[1] if xx % 3 else MORTAR[0])
            for yy in range(3):
                put(img, x0 + 7, y0 + yy, MORTAR[2] if yy == 0 else MORTAR[1])
    return img


def brick_furnace_side():
    return _clay_bricks(21)


def brick_furnace_top():
    img = _clay_bricks(22)
    for y in range(5, 11):                              # soot round the flue
        for x in range(5, 11):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 1.8:
                put(img, x, y, DARK[0])
            elif d < 3.2:
                put(img, x, y, lerp(img.getpixel((x, y)), DARK[1], 0.55))
    return img


def _brick_front(fire=None):
    img = _clay_bricks(23)
    # upper oven: small arched mouth rows 3-6 with a soot-dark inside and an iron grate
    arch = ['....xxxx....', '..xxxxxxxx..', '..xxxxxxxx..', '..xxxxxxxx..']
    for yy, r in enumerate(arch):
        for xx, ch in enumerate(r):
            if ch == 'x':
                put(img, 2 + xx, 3 + yy, DARK[0] if yy < 1 else DARK[2])
    for x in range(4, 12):
        put(img, x, 5, IRON[3] if x % 2 else IRON[2])
    for x in range(4, 12):
        put(img, x, 7, CLAY[6])
    # lower firebox rows 10-13
    fire_box = ['..xxxxxxxx..', '.xxxxxxxxxx.', '.xxxxxxxxxx.', '.xxxxxxxxxx.']
    for yy, r in enumerate(fire_box):
        for xx, ch in enumerate(r):
            if ch == 'x':
                c = DARK[0]
                if fire:
                    c = fire(xx, yy + 1, 'box')
                put(img, 2 + xx, 10 + yy, c)
    for x in range(3, 13):
        put(img, x, 14, CLAY[6])
        put(img, x, 9, CLAY[1])
    return img


def brick_furnace_front():
    return _brick_front()


def brick_furnace_front_on():
    return [_brick_front(_fire(i)) for i in range(4)]


brick_furnace_front_on.frametime = 3


# ================================================================================================ simple grill
def grill_side():
    """Rows 0-5: kettle body (side faces), rows 10-13 x 0-1: legs; the whole texture is also the bottom face."""
    p = IRON
    img = new()
    for y in range(16):
        for x in range(16):
            put(img, x, y, p[2] if (x + y) % 5 else p[1])
    for x in range(16):
        put(img, x, 0, p[5]); put(img, x, 1, p[4])
        for y in (2, 3, 4):
            put(img, x, y, p[3] if x % 5 else p[2])
        put(img, x, 5, p[1])
    for x in (2, 7, 12):
        put(img, x, 3, p[5]); put(img, x + 1, 3, p[0])     # bolts / air holes
    for y in range(6, 16):
        for x in range(16):
            if x in (0, 15) or y == 15:
                put(img, x, y, p[1])
    for y in range(10, 14):                               # leg
        put(img, 0, y, p[4]); put(img, 1, y, p[2])
    return img


EMBER_N = tex.value_noise(16, 16, 41, cell=4, octaves=2)
EMBER_N2 = tex.value_noise(16, 16, 42, cell=4, octaves=1)


def _grill_top(frame=None):
    p = IRON
    img = new()
    rnd = random.Random(31)
    for y in range(16):
        for x in range(16):
            if frame is None:
                c = DARK[1] if (x * 3 + y * 5) % 7 else pal('#57524e')[0]   # cold charcoal and ash
            else:
                v = EMBER_N[y][x] + 0.18 * math.sin((EMBER_N2[y][x] * 6.28) + frame * 2.1)
                c = EMBER[max(1, min(6, int(v * 7)))]
            put(img, x, y, c)
    for x in range(1, 15, 2):                              # grate bars
        for y in range(1, 15):
            put(img, x, y, p[4] if y < 8 else p[3])
        put(img, x, 1, p[5])
    for i in range(16):
        put(img, i, 0, p[5]); put(img, 0, i, p[5]); put(img, i, 15, p[1]); put(img, 15, i, p[1])
    return img


def grill_top():
    return _grill_top()


def grill_top_on():
    return [_grill_top(i) for i in range(3)]


grill_top_on.frametime = 6


# ================================================================================================ plastic and scrap blocks
def plastic_block():
    """Pressed plastic bale: crumpled cream plastic (embossed folds), a few coloured scraps, pressed edge."""
    p = PLASTIC
    h = tex.value_noise(16, 16, 5, cell=8, octaves=2)
    img = new()
    for y in range(16):
        for x in range(16):
            v = emboss(h, x, y, k=1.3)
            put(img, x, y, p[max(2, min(5, 1 + int(v * 5)))])
    scraps = [((2, 3), pal('#4f86b8', '#79a8d4')), ((11, 6), pal('#c0504a', '#dd7a6e')),
              ((6, 12), pal('#5a9a5a', '#86bf7e'))]
    for (x, y), c in scraps:
        put(img, x, y, c[1]); put(img, x + 1, y, c[0]); put(img, x, y + 1, c[0])
    for i in range(16):                                  # pressed edge of the bale
        put(img, i, 0, p[6]); put(img, 0, i, p[6]); put(img, i, 15, p[2]); put(img, 15, i, p[2])
    return img


def scrap_metal_block():
    """Crushed scrap bale: patchwork of plates (steel, rust, galvanised, painted) with lit top-left edges,
    dark bottom-right seams and rivets. Patches are tileable Voronoi cells."""
    rnd = random.Random(12)
    seeds = [(rnd.uniform(0, 16), rnd.uniform(0, 16)) for _ in range(8)]
    mats = [STEEL[1:6], RUST[1:6], GALV[1:6], pal('#5a2620', '#7a3328', '#9a4535', '#b45a47', '#c97561'),
            STEEL[0:5], GALV[0:5], pal('#2a3d55', '#365070', '#46658a', '#5b7ca3', '#7898bb'), STEEL[2:7]]
    def cell(x, y):
        best, bi = 1e9, 0
        for i, (sx, sy) in enumerate(seeds):
            for ox in (-16, 0, 16):
                for oy in (-16, 0, 16):
                    d = max(abs(x - sx - ox), abs(y - sy - oy) * 1.3)   # boxy cells read as plates
                    if d < best:
                        best, bi = d, i
        return bi
    owner = [[cell(x + 0.5, y + 0.5) for x in range(16)] for y in range(16)]
    n = noise(3, cell=4, octaves=2)
    img = new()
    for y in range(16):
        for x in range(16):
            m = mats[owner[y][x]]
            t = 2 + (1 if n[y][x] > 0.6 else 0) - (1 if n[y][x] < 0.3 else 0)
            if owner[(y - 1) % 16][x] != owner[y][x] or owner[y][(x - 1) % 16] != owner[y][x]:
                t = 4                                   # lit edge
            if owner[(y + 1) % 16][x] != owner[y][x] or owner[y][(x + 1) % 16] != owner[y][x]:
                t = 0                                   # seam shadow
            put(img, x, y, m[t])
    for (x, y) in ((3, 4), (12, 2), (8, 10), (14, 13), (2, 12)):
        m = mats[owner[y][x]]
        put(img, x, y, m[4]); put(img, x + 1, y + 1, m[0])
    return img


def jobs(head):
    blue, white = PAINT_BLUE, PAINT_WHITE
    return {
        'block/titanium_ore': titanium_ore,
        'block/deepslate_titanium_ore': deepslate_titanium_ore,
        'block/titanium_block': titanium_block,
        'block/raw_titanium_block': raw_titanium_block,
        'block/titanium_grates': titanium_grates,
        'block/cables/titanium_cable': lambda: titanium_cable(head('block/cables/titanium_cable')),
        'block/titanium_furnace_side': titanium_furnace_side,
        'block/titanium_furnace_top': titanium_furnace_top,
        'block/titanium_furnace_front': titanium_furnace_front,
        'block/titanium_furnace_front_on': titanium_furnace_front_on,
        'block/water_purifier_side': purifier_side,
        'block/water_purifier_bottom': purifier_bottom,
        'block/water_purifier_top': purifier_top,
        'block/water_purifier_front': purifier_front,
        'block/water_purifier_front_on': purifier_front_on,
        'block/chunk_destroyer_bottom': lambda: _machine_bottom(blue),
        'block/chunk_destroyer_front': lambda: _machine_front(blue, (90, 230, 240, 255)),
        'block/chunk_destroyer_side': lambda: _machine_side(blue),
        'block/chunk_destroyer_top': lambda: _machine_top(blue),
        'block/quarry_bottom': lambda: _machine_bottom(white),
        'block/quarry_front': lambda: _machine_front(white, (250, 180, 70, 255)),
        'block/quarry_side': lambda: _machine_side(white),
        'block/quarry_top': lambda: _machine_top(white),
        'block/makeshift_battery_side': battery_side,
        'block/makeshift_battery_top': battery_top,
        'block/makeshift_battery_bottom': battery_bottom,
        'block/makeshift_solar_panel_top': solar_top,
        'block/makeshift_solar_panel_side': solar_side,
        'block/makeshift_solar_panel_bottom': solar_bottom,
        'block/brick_furnace_side': brick_furnace_side,
        'block/brick_furnace_top': brick_furnace_top,
        'block/brick_furnace_front': brick_furnace_front,
        'block/brick_furnace_front_on': brick_furnace_front_on,
        'block/simple_grill_side': grill_side,
        'block/simple_grill_top': grill_top,
        'block/simple_grill_top_on': grill_top_on,
        'block/plastic_block': plastic_block,
        'block/scrap_metal_block': scrap_metal_block,
    }
