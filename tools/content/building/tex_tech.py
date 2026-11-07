"""Textures: Expedition tech (control panel, server rack, satellite dish, wind turbine)."""
import math
import random

from PIL import Image

from .art import *  # noqa: F401,F403

CREAM = pal('#8d8873', '#b0aa8f', '#cfc8aa', '#e6dfc2', '#f4efd9')
CRT = pal('#06140c', '#0b2416', '#123a22', '#1d5a33')
CRT_GLOW = pal('#2f9e57', '#4fd17a', '#9af5b4', '#e2ffe9')
LED = {'g': pal('#1e5c2a', '#46e06a', '#c9ffd2'), 'r': pal('#5c1e1e', '#ff4a3d', '#ffd0c8'),
       'a': pal('#5c461e', '#ffb43d', '#ffe9bf'), 'b': pal('#1e3a5c', '#4aa8ff', '#d0ecff')}


# ================================================================================================= control panel
def control_panel_side():
    p = CONSOLE
    img = new()
    fill(img, 0, 0, 15, 15, p[3])
    bevel(img, 0, 0, 15, 15, p[4], p[0])
    for y in (11, 13):  # vent slots
        for x in range(3, 13):
            put(img, x, y, DARK[1])
        put(img, 2, y, p[1])
        put(img, 13, y, p[4])
    for (x, y) in [(2, 2), (13, 2), (2, 8), (13, 8)]:  # screws
        put(img, x, y, p[5])
        put(img, x + 1, y + 1, p[0])
    return img


def control_panel_front():
    """Cabinet front (rows 8..15 are used): two doors with chrome pulls and a vented kick plate."""
    p = CONSOLE
    img = new()
    fill(img, 0, 0, 15, 15, p[3])
    for half in (0, 8):
        bevel(img, 0, half, 15, half + 7, p[4], p[0])
        for (x0, x1) in ((1, 7), (8, 14)):
            bevel(img, x0, half + 1, x1, half + 5, p[4], p[1])
        put(img, 6, half + 3, GALV[5])
        put(img, 6, half + 4, GALV[2])
        put(img, 9, half + 3, GALV[5])
        put(img, 9, half + 4, GALV[2])
        for x in range(1, 15):
            put(img, x, half + 6, DARK[2] if x % 2 else p[1])
    return img


def control_panel_top():
    """The sloped desk (rows 3..12 are used): dark slate with toggle switches, two dials, knobs and keys."""
    p = DARK
    img = new()
    fill(img, 0, 0, 15, 15, p[2])
    bevel(img, 0, 0, 15, 15, p[4], p[0])
    for x in range(16):
        put(img, x, 3, CREAM[2])
        put(img, x, 12, p[0])
    # two round gauges with cream faces and needles
    for cx, needle in ((4, [(4, 6), (5, 5)]), (11, [(11, 6), (10, 5)])):
        ring(img, cx + 0.5, 6.5, 0, 2.6, lambda x, y, a, d: CREAM[3] if d < 1.9 else BRASS[3] if y < 6 else BRASS[1])
        for (x, y) in needle:
            put(img, x, y, RED[3])
        put(img, cx, 7, DARK[1])
    # row of toggle switches
    for x in range(2, 14, 2):
        put(img, x, 10, GALV[5])
        put(img, x, 11, GALV[2])
    # bakelite knobs
    for x in (7, 8):
        put(img, x, 5, DARK[0])
        put(img, x, 6, DARK[1])
    put(img, 7, 4, DARK[4])
    # key row (cream keys)
    for x in range(2, 14):
        if x % 3 != 1:
            put(img, x, 9, CREAM[3] if x % 3 == 0 else CREAM[2])
    return img


CP_LEDS = [(2, 4, 'g'), (13, 4, 'r'), (8, 8, 'a'), (3, 8, 'g'), (12, 8, 'b'), (14, 10, 'r')]


def control_panel_lights(frames=8):
    """Emissive, animated: indicator lamps on the desk blinking in different rhythms."""
    out = []
    rhythm = {0: [1, 1, 1, 0, 1, 1, 1, 0], 1: [1, 0, 1, 0, 1, 0, 1, 0], 2: [1, 1, 0, 0, 1, 1, 0, 0],
              3: [0, 1, 1, 1, 1, 1, 0, 1], 4: [1, 1, 1, 1, 0, 0, 1, 1], 5: [0, 0, 1, 0, 0, 1, 0, 1]}
    for f in range(frames):
        img = new()
        for i, (x, y, c) in enumerate(CP_LEDS):
            on = rhythm[i][f]
            put(img, x, y, LED[c][2] if on else LED[c][0])
            if on:
                put(img, x, y + 1, LED[c][1])
        out.append(img)
    return out


def control_panel_screen():
    """Screen housing face (rows 1..7 used): cream bezel with two dark CRT screens and a brass label."""
    p = CONSOLE
    img = new()
    fill(img, 0, 0, 15, 15, p[3])
    bevel(img, 0, 0, 15, 8, p[4], p[0])
    for (x0, x1) in ((1, 7), (9, 14)):
        fill(img, x0, 2, x1, 6, CRT[1])
        bevel(img, x0 - 1, 1, x1 + 1, 7, p[1], p[4])
    put(img, 8, 3, BRASS[3])
    put(img, 8, 4, BRASS[2])
    for y in range(9, 16):
        for x in range(16):
            put(img, x, y, p[2])
    return img


def control_panel_screen_glow(frames=12):
    """Emissive, animated: a radar sweep with a blip (left) and a running sine wave (right)."""
    out = []
    for f in range(frames):
        img = new()
        # radar: screen pixels x 1..7, y 2..6, centre (4, 4)
        a = f / frames * 2 * math.pi
        for y in range(2, 7):
            for x in range(1, 8):
                ang = math.atan2(y - 4, x - 4)
                diff = (a - ang) % (2 * math.pi)
                d = math.hypot(x - 4, y - 4)
                if d < 0.5:
                    put(img, x, y, CRT_GLOW[2])
                elif diff < 0.6 and d <= 3.2:
                    put(img, x, y, CRT_GLOW[2])
                elif diff < 1.4 and d <= 3.2:
                    put(img, x, y, CRT_GLOW[0])
                elif abs(d - 3.0) < 0.5:
                    put(img, x, y, CRT[3])
                else:
                    put(img, x, y, CRT[2] if (x + y) % 2 else CRT[1])
        blip = (6, 3)
        if (f % frames) in (3, 4, 5):
            put(img, *blip, CRT_GLOW[3] if f == 3 else CRT_GLOW[1])
        # oscilloscope: x 9..14, y 2..6
        for x in range(9, 15):
            for y in range(2, 7):
                put(img, x, y, CRT[2] if y == 4 else CRT[1])
            yy = 4 + round(math.sin((x + f) / 6 * 2 * math.pi) * 1.6)
            put(img, x, yy, CRT_GLOW[2])
        out.append(img)
    return out


# ================================================================================================= server rack
def server_rack_front():
    p = DARK
    img = new(p[1])
    for y in range(16):  # rails with holes
        for x in (1, 14):
            put(img, x, y, p[4] if y % 2 else p[2])
        put(img, 0, y, p[3])
        put(img, 15, y, p[0])
    units = [(1, 3, 'server'), (4, 5, 'switch'), (6, 8, 'server'), (9, 11, 'tape'), (12, 14, 'vent')]
    for (y0, y1, kind) in units:
        fill(img, 2, y0, 13, y1, p[3])
        for x in range(2, 14):
            put(img, x, y0, p[4])
            put(img, x, y1, p[2])
        if kind == 'server':
            for x in range(3, 9, 2):  # drive bays
                put(img, x, y0 + 1, GALV[3])
            put(img, 12, y0 + 1, p[1])
        elif kind == 'switch':
            for x in range(3, 12):
                put(img, x, y0 + 1 if y1 > y0 + 1 else y0, p[0] if x % 2 else GALV[2])
        elif kind == 'tape':
            for cx in (5, 9):  # two tape reels behind glass
                put(img, cx, y0 + 1, GALV[4])
                put(img, cx + 1, y0 + 1, GALV[2])
        elif kind == 'vent':
            for x in range(3, 13):
                put(img, x, y0 + 1, p[0] if x % 2 else p[2])
    put(img, 0, 0, p[4])
    return img


RACK_LEDS = [(11, 2, 'g'), (12, 2, 'a'), (12, 5, 'g'), (10, 5, 'g'), (11, 7, 'b'), (12, 7, 'g'), (3, 10, 'a'),
             (12, 10, 'g'), (11, 13, 'r')]


def server_rack_leds(frames=8):
    out = []
    rnd = random.Random(311)
    pattern = [[rnd.random() < 0.6 for _ in range(frames)] for _ in RACK_LEDS]
    pattern[8] = [True, False, False, False, True, False, False, False]
    for f in range(frames):
        img = new()
        for i, (x, y, c) in enumerate(RACK_LEDS):
            put(img, x, y, LED[c][2] if pattern[i][f] else LED[c][0])
        out.append(img)
    return out


def server_rack_side():
    p = DARK
    img = new()
    shade_fill(img, p, 2, 3, 312, cell=8, contrast=0.6)
    bevel(img, 0, 0, 15, 15, p[4], p[0])
    for y in range(3, 13, 2):
        for x in range(3, 13, 2):
            put(img, x, y, p[0])
            put(img, x + 1, y + 1, p[4])
    return img


def server_rack_top():
    p = DARK
    img = new()
    shade_fill(img, p, 2, 3, 313, cell=8, contrast=0.6)
    bevel(img, 0, 0, 15, 15, p[4], p[0])
    ring(img, 8, 8, 0, 5.5, lambda x, y, a, d: p[0] if int(d) % 2 == 0 else p[3])
    fill(img, 7, 7, 8, 8, GALV[3])
    return img


# ================================================================================================= satellite dish
DISH = pal('#7d8486', '#a3a9a9', '#c3c7c4', '#dcdedb', '#f0f0ec')


# The dish is a pixel circle 14 px across (rows 1..14). The model builds it from one band per run of equal rows
# (DISH_BANDS), so the front and back pictures fill each band exactly and the rim faces need no cut-out pixels.
DISH_ROWS = {1: (5, 10), 2: (3, 12), 3: (2, 13), 4: (2, 13), 11: (2, 13), 12: (2, 13), 13: (3, 12), 14: (5, 10)}
DISH_ROWS.update({r: (1, 14) for r in range(5, 11)})
# (first row, last row, first column, last column)
DISH_BANDS = [(1, 1, 5, 10), (2, 2, 3, 12), (3, 4, 2, 13), (5, 10, 1, 14), (11, 12, 2, 13), (13, 13, 3, 12),
              (14, 14, 5, 10)]
# Plain pixels outside the circle that the rim faces of the dish sample: top, sides, bottom.
DISH_EDGE_UV = {'up': [0, 0, 1, 1], 'side': [15, 0, 16, 1], 'down': [0, 15, 1, 16]}


def _in_dish(x, y):
    r = DISH_ROWS.get(y)
    return r is not None and r[0] <= x <= r[1]


def _dish_rim(x, y):
    return _in_dish(x, y) and not all(_in_dish(x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))


def _dish_edges(img):
    put(img, 0, 0, DISH[3])
    put(img, 15, 0, DISH[2])
    put(img, 0, 15, DISH[1])


def satellite_dish():
    """Dish face: a raised rim lit from the top-left, the bowl's far (lower-right) wall in the light, the near wall
    in shade, a receiver ring and the dark feed point in the middle."""
    img = new()
    for y in range(16):
        for x in range(16):
            if not _in_dish(x, y):
                continue
            a = math.atan2(y + 0.5 - 8, x + 0.5 - 8)
            if _dish_rim(x, y):
                c = lit(a, DISH, 3, 1)
            else:
                c = DISH[3] if x + y >= 16 else DISH[2]
            put(img, x, y, c)
    ring(img, 8, 8, 2.2, 2.9, lambda x, y, a, d: DISH[1])
    fill(img, 7, 7, 8, 8, DARK[3])
    _dish_edges(img)
    return img


def satellite_dish_back():
    """Back of the dish: convex shell lit from the top-left, two stiffening ribs, steel hub plate."""
    img = new()
    for y in range(16):
        for x in range(16):
            if not _in_dish(x, y):
                continue
            a = math.atan2(y + 0.5 - 8, x + 0.5 - 8)
            if _dish_rim(x, y):
                c = DISH[1]
            elif x == y or x == 15 - y:
                c = DISH[1]
            else:
                c = DISH[3] if x + y < 15 else DISH[2]
            put(img, x, y, c)
    fill(img, 6, 6, 9, 9, STEEL[3])
    bevel(img, 6, 6, 9, 9, STEEL[5], STEEL[1])
    _dish_edges(img)
    return img


def satellite_dish_mount():
    """Galvanised base plate: flat steel with a bevel and four bolts."""
    p = STEEL
    img = new()
    fill(img, 0, 0, 15, 15, p[4])
    fill(img, 2, 2, 13, 13, p[3])
    bevel(img, 0, 0, 15, 15, p[5], p[1])
    for (x, y) in [(3, 3), (12, 3), (3, 12), (12, 12)]:
        put(img, x, y, p[6])
        put(img, x + 1, y + 1, p[1])
    return img


# ================================================================================================= wind turbine
def wind_turbine_body():
    """White painted nacelle with a red Expedition stripe and panel seams."""
    p = PAINT_WHITE
    img = new()
    shade_fill(img, p, 3, 4, 331, cell=8, contrast=0.5)
    for x in range(16):
        put(img, x, 0, p[4])
        put(img, x, 15, p[1])
        put(img, x, 7, RED[4])
        put(img, x, 8, RED[3])
        put(img, x, 9, RED[2])
    for y in range(16):
        if y not in (7, 8, 9):
            put(img, 10, y, p[2])
    for (x, y) in [(2, 3), (13, 12), (5, 13)]:
        put(img, x, y, p[2])
    return img


def wind_turbine_front():
    p = PAINT_WHITE
    img = wind_turbine_body()
    ring(img, 8, 8, 0, 3.5, lambda x, y, a, d: DARK[2] if d > 2.3 else DARK[0])
    return img


def wind_turbine_pole():
    img = new()
    col = [5, 4, 4, 3, 3, 2, 2, 1]
    for y in range(16):
        for x in range(16):
            put(img, x, y, GALV[col[(x // 2) % 8]])
    for x in range(16):
        put(img, x, 0, GALV[5])
    return img


def wind_turbine_blade():
    """Block-atlas blade for the item model: white with a red tip (rows 12..15) and a grey root."""
    p = PAINT_WHITE
    img = new()
    for y in range(16):
        for x in range(16):
            c = p[4] if x < 8 else p[3]
            if y >= 12:
                c = RED[4] if x < 8 else RED[3]
            elif y < 2:
                c = GALV[3]
            put(img, x, y, c)
    return img


def wind_turbine_rotor():
    """Entity texture 32x32 for the rotor model (box UV layout, see WindTurbineRotorModel)."""
    img = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
    W = PAINT_WHITE

    def box(u, v, w, h, d, colour_at):
        # standard box unwrap: top/bottom row then the four sides
        regions = [(u + d, v, w, d), (u + d + w, v, w, d), (u, v + d, d, h), (u + d, v + d, w, h),
                   (u + d + w, v + d, d, h), (u + 2 * d + w, v + d, w, h)]
        for (x0, y0, ww, hh) in regions:
            for yy in range(hh):
                for xx in range(ww):
                    img.putpixel((x0 + xx, y0 + yy), colour_at(xx, yy, ww, hh))

    def blade_root(xx, yy, ww, hh):
        return GALV[4] if yy < 2 else (W[4] if xx < ww / 2 else W[3])

    def blade_tip(xx, yy, ww, hh):
        if yy >= hh - 4:
            return RED[4] if xx < ww / 2 else RED[3]
        if yy == hh - 5:
            return W[2]
        return W[4] if xx < ww / 2 else W[3]

    box(0, 0, 4, 8, 1, blade_root)
    box(10, 0, 3, 13, 1, blade_tip)
    box(0, 23, 5, 5, 4, lambda xx, yy, ww, hh: W[3] if (xx + yy) % 5 else W[2])
    box(18, 23, 3, 3, 1, lambda xx, yy, ww, hh: RED[3])
    return img
