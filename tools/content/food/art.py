"""Pixel art of the food module: crops (4 stages), wild plants, food sprites, cooking pot and fish trap."""
import math

from PIL import Image

from bclib import tex
from content.decor import art as DA
from content.decor.art import pal

M = 'food'

LEAF = pal('#1d4220', '#285a2a', '#357236', '#468c41', '#5ea653', '#7cc06a')
STEM = pal('#3b4a1d', '#4d6224', '#627c2d', '#7a9739')
PURPLE = pal('#3d1830', '#57223f', '#743352', '#93486a', '#b06a88')     # sweet potato skin
ORANGE_FLESH = pal('#9a4a10', '#c46418', '#e2862a', '#f4a94a', '#fccb7c')
RED = pal('#5c1010', '#851a14', '#b0281c', '#d6402a', '#ef6a46', '#fb9c78')
YELLOW = pal('#7a5a10', '#a8801c', '#d4aa2c', '#ecc94a', '#f8e27e', '#fff3b8')
BANANA_LEAF = pal('#1f4a24', '#2a632d', '#3a7e38', '#4f9a45', '#6cb558')
RICE_GREEN = pal('#3c5a22', '#52742c', '#6a8f38', '#86ab48')
GOLD = pal('#7a5c22', '#a07e30', '#c4a044', '#dcbd62', '#efd88c')
SEA_GREEN = pal('#164a2e', '#1f6a3c', '#2c8a4c', '#44aa60', '#6cc87c', '#a2e4a6')
RICE_WHITE = pal('#a8a294', '#c8c2b2', '#e0dccd', '#f0ede2', '#fbf9f2')
BOWL = pal('#3e2614', '#55361d', '#6e4827', '#8a5d33')
CREAM = pal('#b8a07a', '#d4bf98', '#e8d8b8', '#f6ecd6', '#fffaf0')
CURRY = pal('#7a4210', '#a05a16', '#c47820', '#e09a32', '#f2bc5a')
CHOWDER = pal('#8c8270', '#aea38e', '#cbc1aa', '#e2dac6', '#f2ecdc')
SOUP = pal('#8a3c10', '#b45418', '#d87226', '#f0943c', '#fbb868')
IRON = DA.IRON
WICKER = DA.WICKER


def img(w=16, h=16):
    return Image.new('RGBA', (w, h), (0, 0, 0, 0))


def P(im, x, y, c):
    if 0 <= x < im.width and 0 <= y < im.height:
        im.putpixel((int(x), int(y)), tex.rgb(c))


def save(im, rel):
    return tex.save(im, M, rel)


def blob(im, cx, cy, rx, ry, ramp, light=(-1, -1)):
    """Shaded ellipse: brighter towards the light (top left)."""
    n = len(ramp)
    for y in range(int(cy - ry - 1), int(cy + ry + 2)):
        for x in range(int(cx - rx - 1), int(cx + rx + 2)):
            dx, dy = (x + 0.5 - cx) / rx, (y + 0.5 - cy) / ry
            d = dx * dx + dy * dy
            if d <= 1.0:
                lit = -(dx * light[0] + dy * light[1]) * 0.5 + 0.5      # 0..1
                k = int(round((n - 2) * (0.25 + 0.75 * lit) * (1.0 - 0.35 * d)))
                if d > 0.75:
                    k = max(0, k - 1)
                P(im, x, y, ramp[max(0, min(n - 1, k))])


def outline(im):
    return tex.outline(im)


# ------------------------------------------------------------------------------------------------- crops
def _stems(im, xs, top, ramp=STEM):
    for x in xs:
        for y in range(top, 16):
            P(im, x, y, ramp[2] if (y + x) % 3 else ramp[1])


def _leaf(im, x, y, dx, length, ramp=LEAF):
    for k in range(length):
        P(im, x + dx * k, y - k // 2, ramp[3 if k < length - 1 else 4])
        P(im, x + dx * k, y - k // 2 + 1, ramp[2])


def sweet_potato_stage(s):
    im = img()
    h = [11, 8, 5, 4][s]
    _stems(im, (4, 8, 12) if s > 0 else (7, 9), h + 2)
    for (x, y) in ((4, h + 2), (8, h), (12, h + 2)) if s > 0 else ((7, h + 1), (9, h + 2)):
        _leaf(im, x, y + 1, -1, 3 + s)
        _leaf(im, x, y + 2, 1, 3 + s)
        P(im, x, y, LEAF[5])
    if s == 3:
        for x in (3, 9):
            blob(im, x + 1.5, 14.5, 2.2, 1.4, PURPLE)
    return im


def chili_stage(s):
    """A small pepper bush: leaves in pairs along two stems, peppers hanging below the leaves when grown."""
    im = img()
    top = [11, 8, 5, 3][s]
    for x0 in (6, 9):
        for y in range(top + 1 + (x0 == 9), 16):
            P(im, x0 + (1 if y < top + 4 and x0 == 9 else 0), y, STEM[2] if y % 3 else STEM[1])
    leaves = [(5, top + 2), (10, top + 1), (4, top + 5), (11, top + 4), (5, top + 8), (10, top + 8)]
    for k, (x, y) in enumerate(leaves):
        if y > 14 or k >= 2 + 2 * s:
            continue
        blob(im, x + 0.5, y + 0.5, 2.3, 1.2, LEAF[1:])
    if s >= 2:
        col = RED if s == 3 else pal('#2a5a1a', '#3a7424', '#4f9030', '#6aac40')
        for x, y in ((3, top + 4), (12, top + 3), (7, top + 7), (9, top + 9)):
            if y + 3 > 15:
                continue
            P(im, x, y - 1, STEM[3])
            for k in range(3):
                P(im, x, y + k, col[3 if k == 0 else 2 if k == 1 else 1])
            P(im, x + 1, y, col[1])
    return im


def banana_stage(s):
    im = img()
    h = [10, 6, 3, 1][s]
    for y in range(h + 3, 16):
        P(im, 7, y, BANANA_LEAF[1])
        P(im, 8, y, BANANA_LEAF[2])
    for k, (x, y, d) in enumerate(((7, h + 3, -1), (8, h + 2, 1), (7, h + 6, -1), (8, h + 7, 1))):
        if k > s + 1:
            continue
        for i in range(6 - k):
            yy = y + (i * i) // 6
            P(im, x + d * i, yy, BANANA_LEAF[3])
            P(im, x + d * i, yy + 1, BANANA_LEAF[4] if i % 2 else BANANA_LEAF[2])
    if s == 3:
        for y in range(9, 13):
            for x in range(9, 12):
                P(im, x, y, YELLOW[3] if (x + y) % 2 else YELLOW[2])
        P(im, 10, 8, STEM[2])
        P(im, 10, 13, '#5a3a1a')
    return im


def rice_stage(s):
    im = img()
    top = [11, 8, 5, 3][s]
    for i, x in enumerate((2, 4, 6, 8, 10, 12, 14)):
        t = top + (i % 3)
        for y in range(t, 16):
            P(im, x + (1 if y < t + 3 and i % 2 else 0), y, RICE_GREEN[2 + (y + i) % 2])
        if s >= 2:
            col = GOLD if s == 3 else RICE_GREEN
            for k in range(3):
                P(im, x + 1 + (k % 2), t + k, col[3 - k % 2])
    return im


def sea_grapes_stage(s):
    """Sea grapes: wavy green stalks with round beads on short stems, more and taller with every stage."""
    im = img()
    stalks = [(4, 11), (8, 9), (12, 12), (6, 13)][:[1, 2, 3, 4][s]]
    grow = [5, 8, 11, 14][s]
    for i, (x, base_top) in enumerate(stalks):
        top = 16 - grow + (i % 2) * 2
        for y in range(top, 16):
            xx = x + (1 if (y // 3) % 2 else 0)
            P(im, xx, y, SEA_GREEN[1])
            if (y - top) % 3 == 1 and y < 15:
                side = 1 if (y // 3 + i) % 2 else -1
                bx = xx + side * 2
                P(im, xx + side, y, SEA_GREEN[2])
                P(im, bx, y, SEA_GREEN[4] if side < 0 else SEA_GREEN[3])
                P(im, bx, y - 1, SEA_GREEN[5] if s == 3 else SEA_GREEN[4])
        P(im, x, top - 1, SEA_GREEN[4])
    return im


# ------------------------------------------------------------------------------------------------- items
def sweet_potato():
    im = img()
    blob(im, 7.5, 8.5, 6.2, 3.6, PURPLE)
    for x, y in ((5, 7), (9, 9), (11, 7)):
        P(im, x, y, PURPLE[0])
    P(im, 1, 10, STEM[2])
    P(im, 14, 7, PURPLE[1])
    return outline(im)


def baked_sweet_potato():
    im = img()
    blob(im, 7.5, 8.5, 6.2, 3.6, pal('#2e1420', '#45202f', '#5c2c3e', '#73384c', '#8a4c5e'))
    for y in range(7, 10):
        for x in range(4, 12):
            P(im, x, y, ORANGE_FLESH[3 if y == 7 else 2])
    for x in (5, 8, 10):
        P(im, x, 7, ORANGE_FLESH[4])
    P(im, 7, 6, '#fff0b0')
    return outline(im)


def chili_pepper():
    im = img()
    for i in range(10):
        x = 4 + i
        y = 5 + int(3 * math.sin(i / 3.2)) + i // 3
        for k in range(3 - (i > 7)):
            P(im, x, y + k, RED[4 if k == 0 else 3 if k == 1 else 2])
    P(im, 3, 5, STEM[3])
    P(im, 3, 4, STEM[2])
    P(im, 2, 4, STEM[1])
    return outline(im)


def chili_seeds():
    im = img()
    for x, y in ((5, 6), (9, 5), (7, 9), (11, 9), (4, 11), (8, 12)):
        P(im, x, y, '#f2e0a0')
        P(im, x + 1, y, '#d8c27a')
        P(im, x, y + 1, '#c4a85e')
    return outline(im)


def banana():
    im = img()
    for i in range(11):
        t = i / 10
        x = 3 + i
        y = int(10 - 6 * math.sin(t * math.pi * 0.9))
        for k in range(3):
            P(im, x, y + k - 1, YELLOW[4 if k == 0 else 3 if k == 1 else 2])
    P(im, 13, 3, '#5a4a20')
    P(im, 14, 3, '#3e321a')
    P(im, 3, 9, '#4a3a1a')
    return outline(im)


def banana_shoot():
    im = img()
    for y in range(9, 15):
        P(im, 7, y, BANANA_LEAF[1])
        P(im, 8, y, BANANA_LEAF[2])
    for i in range(5):
        P(im, 7 - i, 9 - i // 2, BANANA_LEAF[3])
        P(im, 8 + i, 8 - i // 2, BANANA_LEAF[4])
        P(im, 7 - i, 10 - i // 2, BANANA_LEAF[2])
        P(im, 8 + i, 9 - i // 2, BANANA_LEAF[3])
    blob(im, 7.5, 13.5, 2.5, 1.8, pal('#4a3018', '#664424', '#825a30', '#9c7040'))
    return outline(im)


def rice():
    im = img()
    for i, (x0, y0) in enumerate(((5, 3), (8, 2), (11, 4))):
        for y in range(y0, 15):
            P(im, x0 + (y - y0) // 6, y, RICE_GREEN[2] if y > y0 + 5 else GOLD[2])
        for k in range(0, 6, 2):
            P(im, x0 - 1, y0 + k + 1, GOLD[4])
            P(im, x0 + 1, y0 + k, GOLD[3])
    return outline(im)


def cooked_rice():
    im = img()
    blob(im, 7.5, 9.5, 6, 4.2, RICE_WHITE)
    for x, y in ((5, 8), (8, 7), (10, 9), (6, 11), (9, 11)):
        P(im, x, y, RICE_WHITE[4])
        P(im, x + 1, y, RICE_WHITE[1])
    return outline(im)


def sea_grapes():
    im = img()
    for y in range(3, 15):
        P(im, 7 + (y > 9), y, SEA_GREEN[1])
    for y in range(3, 13, 2):
        for dx in (-1, 1, 2):
            if dx == 2 and y % 4:
                continue
            blob(im, 7.5 + dx * 1.6 + (y > 9), y + 0.5, 1.2, 1.2, SEA_GREEN[1:])
    return outline(im)


def sushi_roll():
    im = img()
    blob(im, 7.5, 8.5, 5.5, 5.5, pal('#0f2418', '#163220', '#1e4029', '#284f33'))
    blob(im, 7.5, 8.5, 4.2, 4.2, RICE_WHITE)
    blob(im, 7.5, 8.5, 2.0, 2.0, pal('#a03a2a', '#c8503a', '#e8705a', '#f8987e'))
    return outline(im)


def _bowl(fill_ramp, bits=()):
    """A bowl (side view, from the top-left) filled with fill_ramp; bits: (x, y, colour) toppings."""
    im = img()
    for y in range(8, 14):
        w = [7, 7, 6, 5, 4, 3][y - 8]
        for x in range(8 - w, 8 + w):
            k = 3 if x < 6 else 2 if x < 11 else 1
            if y == 13:
                k = 0
            P(im, x, y, BOWL[k])
    for x in range(1, 15):
        P(im, x, 7, fill_ramp[2] if x % 3 else fill_ramp[3])
    for x in range(2, 14):
        P(im, x, 6, fill_ramp[3] if x % 4 else fill_ramp[4])
    for x in range(4, 12):
        P(im, x, 5, fill_ramp[3])
    for x, y, c in bits:
        P(im, x, y, c)
    return outline(im)


def chili_crab():
    return _bowl(pal('#7a1a10', '#a02a18', '#c43c22', '#e05a32', '#f48850'),
                 ((5, 4, RED[4]), (6, 4, RED[3]), (9, 5, '#f8c8a0'), (10, 4, '#fbe0c8'), (11, 5, RED[2])))


def fruit_salad():
    return _bowl(pal('#5a8a2a', '#7aa83a', '#9cc44e', '#bcdc6a', '#d8f08c'),
                 ((4, 5, YELLOW[4]), (6, 4, '#e8434a'), (8, 5, YELLOW[3]), (10, 4, '#f29a3a'), (11, 5, '#e8434a'),
                  (7, 6, '#7a3a8a')))


def island_curry():
    return _bowl(CURRY, ((5, 5, RICE_WHITE[4]), (6, 4, RICE_WHITE[3]), (9, 4, RED[3]), (10, 5, ORANGE_FLESH[3]),
                         (8, 5, '#5a9a3a')))


def seafood_chowder():
    return _bowl(CHOWDER, ((5, 4, '#e8846a'), (8, 5, ORANGE_FLESH[3]), (10, 4, '#e8846a'), (7, 5, SEA_GREEN[3])))


def sweet_potato_soup():
    return _bowl(SOUP, ((6, 5, CREAM[4]), (7, 5, CREAM[3]), (9, 4, '#5a9a3a')))


def coconut_ice_cream():
    im = _bowl(pal('#cfc8b8', '#e2dccd', '#f0ece2', '#faf8f2', '#ffffff'))
    blob(im, 7.5, 4.5, 3.5, 2.6, pal('#d8d2c4', '#ece8de', '#f8f6f0', '#ffffff'))
    P(im, 8, 1, '#8a5a3a')
    P(im, 9, 2, '#a87a50')
    return outline(im)


def tropical_smoothie():
    """A glass with an orange-yellow smoothie, a straw and a pineapple wedge."""
    im = img()
    glass = pal('#8aa8b0', '#b0c8cc', '#d4e4e4', '#eef6f4')
    for y in range(4, 15):
        w = 4 if y < 13 else 3
        for x in range(8 - w, 8 + w):
            P(im, x, y, YELLOW[3] if y > 5 else glass[3])
        P(im, 8 - w, y, glass[2])
        P(im, 7 + w, y, glass[1])
    for y in range(6, 14):
        P(im, 5, y, YELLOW[4])
    for x in range(4, 12):
        P(im, x, 6, pal('#e8803a')[0])
    for y in range(0, 7):
        P(im, 10 + (y < 2), y, '#e84a6a')
    P(im, 3, 4, YELLOW[4])
    P(im, 2, 5, YELLOW[3])
    P(im, 2, 3, LEAF[4])
    return outline(im)


def banana_bread():
    im = img()
    for y in range(6, 13):
        for x in range(2, 14):
            k = 3 if y == 6 else 2
            if x in (2, 13) or y == 12:
                k = 1
            P(im, x, y, pal('#5a3414', '#7a4a1e', '#9a622a', '#b87c3a', '#d49a52')[k])
    for x in range(3, 13):
        P(im, x, 5, pal('#7a4a1e', '#9a622a', '#b87c3a')[2 if x % 3 else 1])
    for x in (5, 9):
        P(im, x, 4, YELLOW[3])
        P(im, x + 1, 4, YELLOW[2])
    for x, y in ((4, 9), (8, 8), (11, 10)):
        P(im, x, y, '#4a2a10')
    return outline(im)


# ------------------------------------------------------------------------------------------------- blocks
def pot_side():
    im = DA.metal(IRON, seed=9)
    for x in range(16):
        P(im, x, 0, IRON[5])
        P(im, x, 1, IRON[4])
        P(im, x, 15, IRON[1])
    return im


def pot_inside():
    return tex.noise_fill(img(), IRON[0:3], 4, cell=4, octaves=1, contrast=0.6)


def surface(ramp, bubbles=True):
    im = img()
    for y in range(16):
        for x in range(16):
            k = 2 + (1 if (x * 3 + y * 5) % 11 == 0 else 0)
            P(im, x, y, ramp[k])
    if bubbles:
        for x, y in ((4, 5), (10, 3), (7, 10), (12, 11)):
            P(im, x, y, ramp[4])
            P(im, x + 1, y, ramp[3])
    return im


def liquid():
    """Grey liquid surface with soft ripples and a few bright specks; tinted in the world (water -> dish)."""
    g = pal('#a6a6a6', '#bdbdbd', '#d2d2d2', '#e4e4e4', '#f4f4f4', '#ffffff')
    im = img()
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - 7.5, y - 7.5)
            k = 3 if int(r * 0.9 + (x + y) * 0.15) % 3 else 2
            if (x * 7 + y * 3) % 13 == 0:
                k = 4
            P(im, x, y, g[k])
    for x, y in ((4, 5), (10, 3), (7, 10), (12, 11), (3, 12)):
        P(im, x, y, g[5])
        P(im, x + 1, y, g[4])
    return im


WATER = pal('#1e4f8a', '#2a64a6', '#3a7cc0', '#5a9ad4', '#8cc0e8')


def trap_cage():
    """Wicker fish trap: open weave with gaps (cutout)."""
    im = img()
    for y in range(16):
        for x in range(16):
            if x % 4 == 0 or y % 5 == 0:
                k = 3 if (x + y) % 2 else 2
                if x % 4 == 0 and y % 5 == 0:
                    k = 4
                P(im, x, y, WICKER[k])
    return im


def trap_end():
    """Funnel end of the trap: rings of wicker around the opening."""
    im = img()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 2.5:
                continue
            if int(d) % 2 == 0 or (int(math.degrees(math.atan2(y - 7.5, x - 7.5))) // 30) % 2 == 0:
                P(im, x, y, WICKER[3 if int(d) % 2 == 0 else 2])
    return im
