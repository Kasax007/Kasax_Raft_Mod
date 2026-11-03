"""Small pixel-art toolkit for the building module: palettes, tileable helpers, bevels and glyphs.

Everything works on 16x16 RGBA images (PIL) and palettes given as lists of colours dark -> light.
"""
import math
import random

from PIL import Image

from bclib import tex


def pal(*hexes):
    return [tex.rgb(h) for h in hexes]


# ------------------------------------------------------------------------------------------------ palettes (dark -> light)
WEATHERED = pal('#4c4740', '#625c53', '#787166', '#8f887c', '#a69f92', '#bdb7aa', '#d2cdc2')
TEAK = pal('#2a160d', '#3f2215', '#56301c', '#6d3f24', '#854f2c', '#9e6237', '#b77846')
CORAL = pal('#7f5248', '#9f6b5e', '#b98271', '#cc9682', '#dcab95', '#e8c0a9', '#f2d5c1')
FOSSIL = pal('#a77a6a', '#efdcc6', '#fbf0e0')
TIDE = pal('#173638', '#22494b', '#2f5f60', '#3e7575', '#4f8b88', '#64a19a', '#7fb8ad')
GOLD = pal('#5c3f12', '#8f6620', '#c2932e', '#e7bd45', '#fbe08a')
GLOW = pal('#1b7f8f', '#2fb4c4', '#57dbe6', '#9af4f6', '#e0ffff')
CONCRETE = pal('#4d4f51', '#616365', '#76787a', '#8a8b8b', '#9d9e9c', '#b0b0ac', '#c3c2bc')
ALGAE = pal('#2f4528', '#405a30', '#55703a', '#6a8444', '#829a52')
ASPHALT = pal('#1f2125', '#272a2f', '#2f3238', '#383b42', '#42464d', '#4f535a')
PAINT_WHITE = pal('#7b8285', '#9fa6a8', '#c0c5c4', '#dadcd7', '#ecece6')
RUST = pal('#3f1d12', '#5c2a17', '#7a3b1e', '#974d26', '#b06232', '#c67c45', '#d89b62')
NAVY = pal('#141b2b', '#1c263b', '#25324c', '#30405f', '#3d5073', '#4f6489', '#6a7fa2')
STEEL = pal('#33383e', '#454b52', '#596068', '#6e767e', '#858d94', '#9ea5ab', '#babfc3')
GALV = pal('#4f575b', '#667074', '#7e888c', '#97a0a3', '#afb7b9', '#c6cccd', '#dce0df')
BRASS = pal('#4f3712', '#7a5720', '#a77c2e', '#cfa246', '#ebc86e', '#fbe6a8')
RED = pal('#4a1512', '#6a1e19', '#8c2a22', '#ad3a2c', '#c9523b', '#dd7356')
CONT_RED = pal('#3d1411', '#561d17', '#72281e', '#8f3527', '#a84432', '#bf5a43')
CONT_BLUE = pal('#12223a', '#1a3150', '#234168', '#2e5281', '#3d6698', '#567fae')
CONT_GREEN = pal('#132a1a', '#1c3b24', '#264d2f', '#33613b', '#43764a', '#5b8d5d')
CONSOLE = pal('#3b4442', '#525d5a', '#6a7773', '#83918c', '#9eaca6', '#bac6bf')
DARK = pal('#16191d', '#1e2227', '#272c32', '#31373e', '#3c434b', '#4a525b')
GLASS_BLUE = pal('#5f8fa8', '#86b7cc', '#b5dcea', '#e6f7fb')


def lerp(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(4))


def with_alpha(c, a):
    return (c[0], c[1], c[2], a)


def new(c=None):
    return Image.new('RGBA', (16, 16), c if c else (0, 0, 0, 0))


def get(img, x, y):
    return img.getpixel((x % 16, y % 16))


def put(img, x, y, c):
    img.putpixel((x % 16, y % 16), c)


def put_in(img, x, y, c):
    """Put without wrapping (ignored outside)."""
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), c)


def fill(img, x0, y0, x1, y1, c):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            put_in(img, x, y, c)


def noise(seed, cell=4, octaves=2):
    return tex.value_noise(16, 16, seed, cell, octaves)


def shade_fill(img, palette, lo, hi, seed, cell=4, octaves=2, box=(0, 0, 15, 15), contrast=1.0, mask=None):
    """Fill box with palette[lo..hi] following tileable noise."""
    n = noise(seed, cell, octaves)
    k = hi - lo + 1
    x0, y0, x1, y1 = box
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if mask and not mask(x, y):
                continue
            v = 0.5 + (n[y % 16][x % 16] - 0.5) * contrast * 1.6
            i = lo + max(0, min(k - 1, int(v * k)))
            put_in(img, x, y, palette[i])
    return img


def bevel(img, x0, y0, x1, y1, light, dark, corner=None):
    """1-px bevel: top/left light, bottom/right dark."""
    for x in range(x0, x1 + 1):
        put_in(img, x, y0, light)
        put_in(img, x, y1, dark)
    for y in range(y0, y1 + 1):
        put_in(img, x0, y, light)
        put_in(img, x1, y, dark)
    if corner:
        put_in(img, x1, y0, corner)
        put_in(img, x0, y1, corner)


def blotches(img, colours, amount, seed, cell=3, mask=None, octaves=2):
    """Noise-shaped patches (moss, rust, algae). colours dark->light; the patch centre gets the lighter ones."""
    n = noise(seed, cell, octaves)
    flat = sorted(v for row in n for v in row)
    thr = flat[int(len(flat) * (1 - amount))] if amount > 0 else 2
    for y in range(16):
        for x in range(16):
            if n[y][x] >= thr and (mask is None or mask(x, y)) and img.getpixel((x, y))[3] > 0:
                k = min(len(colours) - 1, int((n[y][x] - thr) / max(1e-6, 1 - thr) * len(colours)))
                img.putpixel((x, y), colours[k])
    return img


def blotch_mask(amount, seed, cell=3, octaves=2):
    n = noise(seed, cell, octaves)
    flat = sorted(v for row in n for v in row)
    thr = flat[int(len(flat) * (1 - amount))]
    return lambda x, y: n[y % 16][x % 16] >= thr


def crack(img, path, dark, light=None):
    """Draw a crack along a list of (x, y) points with a lit lower edge."""
    for (x, y) in path:
        put(img, x, y, dark)
    if light:
        s = set((x % 16, y % 16) for x, y in path)
        for (x, y) in path:
            if ((x % 16), (y + 1) % 16) not in s:
                put(img, x, y + 1, light)


def walk(x, y, steps, seed, dx_choices=(-1, 0, 1), dy_choices=(1,)):
    rnd = random.Random(seed)
    pts = [(x, y)]
    for _ in range(steps):
        x += rnd.choice(dx_choices)
        y += rnd.choice(dy_choices)
        pts.append((x, y))
    return pts


def circle_mask(cx, cy, r):
    return lambda x, y: (x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2 <= r * r


def ring(img, cx, cy, r0, r1, colour_fn):
    """Paint pixels whose centre distance is in [r0, r1); colour_fn(x, y, angle, dist) -> colour or None."""
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if r0 <= d < r1:
                c = colour_fn(x, y, math.atan2(y + 0.5 - cy, x + 0.5 - cx), d)
                if c:
                    img.putpixel((x, y), c)


def lit(angle, palette, base, spread=1):
    """Shade by direction: light from top-left (angle of the pixel around a centre)."""
    # direction towards the light: (-1, -1) -> angle -135 deg
    v = math.cos(angle - math.radians(-135))  # 1 facing light, -1 away
    i = base + round(v * spread)
    return palette[max(0, min(len(palette) - 1, i))]


def from_rows(rows, palette, alpha_char='.'):
    """Build an image from 16 strings; digits/letters index the palette, alpha_char is transparent."""
    img = new()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch == alpha_char or ch == ' ':
                continue
            img.putpixel((x, y), palette[int(ch, 36)])
    return img


def overlay_rows(img, rows, mapping):
    """Paint chars of rows that are keys in mapping (char -> colour) over img."""
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in mapping:
                img.putpixel((x, y), mapping[ch])
    return img


def rotate90(img):
    return img.transpose(Image.Transpose.ROTATE_90)
