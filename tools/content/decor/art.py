"""Pixel-art helpers and palettes of the decor module (16x16, light from the top-left, ramps dark -> light)."""
import math
import random

from PIL import Image

from bclib import tex

M = 'decor'


def pal(*hexes):
    return [tex.rgb(h) for h in hexes]


IRON = pal('#1e2026', '#2a2d34', '#373b43', '#474c55', '#5b616b', '#757b85')
BRASS = pal('#4a3311', '#74521e', '#a0772c', '#c99d43', '#e6c46c', '#f8e3a2')
GOLD = pal('#6b4410', '#9c6a17', '#cc9524', '#efbe3c', '#fbdc74', '#fff3bd')
TEAK = pal('#2c170d', '#412316', '#58311d', '#6f4026', '#87512f', '#a1663a', '#b97c49')
DRIFT = pal('#4f473d', '#665d51', '#7e7466', '#978c7c', '#ada393', '#c4bbab')
PALM = pal('#6b4a26', '#8a6233', '#a87c42', '#c49a5c', '#d8b57a', '#e9cf9c')
ROPE = pal('#5a4529', '#79603c', '#987d52', '#b49764', '#cfb47f', '#e3cfa1')
CANVAS = pal('#8a8069', '#a89f86', '#c4bba1', '#dad2b9', '#ebe5d1', '#f7f3e6')
RED = pal('#4a1411', '#6c1e18', '#8f2a21', '#b0392b', '#cc5038', '#e06e4f')
WHITE = pal('#8c8f94', '#aeb1b4', '#cacbcb', '#dedfdc', '#efefea', '#fbfbf6')
BLACK_CLOTH = pal('#141418', '#1c1c22', '#25252c', '#2f2f37', '#3b3b44')
BAMBOO = pal('#55492a', '#766a38', '#988a48', '#b9a95c', '#d3c57a', '#e8dca0')
PARCHMENT = pal('#8a6a45', '#a8885c', '#c4a676', '#d9bf8f', '#e9d5a9', '#f4e7c6')
SEA_GLASS = pal('#2f6d68', '#43918a', '#5fb3a9', '#83cfc2', '#b0e6da', '#dcf7ef')
SHELL = pal('#9a6a5a', '#bd8a77', '#d8a893', '#ebc4b0', '#f6dccd', '#fdf0e6')
LEAF = pal('#24461f', '#30602a', '#3f7a33', '#55933f', '#70ad4f')
WICKER = pal('#5b4022', '#7a5730', '#9a7140', '#b98c54', '#d2a96e', '#e6c690')

DYE = {
    'white': '#f2efe6', 'red': '#d8443a', 'orange': '#ef8a2e', 'yellow': '#f2cf3a', 'lime': '#8fd13f',
    'cyan': '#3fc2c8', 'blue': '#3d6fd8', 'purple': '#9a52cf', 'pink': '#ef86b3', 'green': '#4f9a44',
}


def dye_ramp(name, n=6, spread=0.62):
    return tex.ramp(DYE[name], n=n, spread=spread, hue_shift=0.05)


def img(w=16, h=16):
    return Image.new('RGBA', (w, h), (0, 0, 0, 0))


def fill(im, c, box=None):
    x0, y0, x1, y1 = box or (0, 0, im.width - 1, im.height - 1)
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            im.putpixel((x, y), tex.rgb(c))
    return im


def P(im, x, y, c):
    if 0 <= x < im.width and 0 <= y < im.height:
        im.putpixel((x, y), tex.rgb(c))


def get(im, x, y):
    return im.getpixel((x % im.width, y % im.height))


# ------------------------------------------------------------------------------------------------ materials
def wood(shades, seed=1, vertical=False, rings=False):
    """Fine wood grain without board seams (for carved parts, legs, frames)."""
    rnd = random.Random(seed)
    im = img()
    n = len(shades)
    base = n // 2
    for y in range(16):
        for x in range(16):
            im.putpixel((x, y), shades[base])
    for row in range(16):
        x = rnd.randrange(16)
        for _ in range(2):
            ln = rnd.randrange(4, 10)
            d = rnd.choice([-1, 1, 1])
            for k in range(ln):
                im.putpixel(((x + k) % 16, row), shades[max(0, min(n - 1, base + d))])
            x = (x + ln + rnd.randrange(2, 5)) % 16
    for _ in range(2):
        kx, ky = rnd.randrange(16), rnd.randrange(16)
        P(im, kx, ky, shades[max(0, base - 2)])
        P(im, (kx + 1) % 16, ky, shades[max(0, base - 1)])
    return im.transpose(Image.Transpose.ROTATE_90) if vertical else im


def metal(shades, seed=1, scratches=2, mottled=0.9):
    """Cast/forged metal: soft mottling, a few short highlights, no hard seams (tiles)."""
    rnd = random.Random(seed)
    im = tex.noise_fill(img(), shades[1:5], seed, cell=4, octaves=2, contrast=mottled)
    for _ in range(scratches):
        x, y = rnd.randrange(16), rnd.randrange(16)
        for k in range(rnd.randrange(2, 4)):
            P(im, (x + k) % 16, y, shades[-1])
    return im


def cloth(shades, seed=1, weave=True):
    """Woven fabric: a calm base with a fine checker weave (1 shade apart)."""
    rnd = random.Random(seed)
    im = img()
    b = len(shades) // 2
    nz = tex.value_noise(16, 16, seed, 8, 1)
    for y in range(16):
        for x in range(16):
            t = b + (1 if nz[y][x] > 0.62 else 0) - (1 if nz[y][x] < 0.3 else 0)
            if weave and (x + y) % 2 == 0 and (x // 2 + y) % 3 == 0:
                t -= 1
            im.putpixel((x, y), shades[max(0, min(len(shades) - 1, t))])
    return im


def rope(shades, horizontal=True):
    """Twisted rope strands, diagonal pattern, tiles along the rope."""
    im = img()
    n = len(shades)
    for y in range(16):
        for x in range(16):
            k = (x + y) % 4
            t = [n - 2, n - 3, n - 4, n - 3][k]
            im.putpixel((x, y), shades[max(0, t)])
            if k == 0:
                im.putpixel((x, y), shades[n - 1] if y % 2 == 0 else shades[n - 2])
    return im if horizontal else im.transpose(Image.Transpose.ROTATE_90)


def stripes(c1, c2, width=2, vertical=True, shade=True):
    """Two-colour stripes (deck chairs, awnings), each colour a 3-shade ramp for a soft fold."""
    r1 = tex.ramp(c1, 4, 0.25)
    r2 = tex.ramp(c2, 4, 0.25)
    im = img()
    for y in range(16):
        for x in range(16):
            i = (x if vertical else y) // width % 2
            r = r1 if i == 0 else r2
            t = 2
            if shade and (y if vertical else x) % 8 == 7:
                t = 1
            im.putpixel((x, y), r[t])
    return im


def bevel(im, light, dark, box=None):
    x0, y0, x1, y1 = box or (0, 0, im.width - 1, im.height - 1)
    for x in range(x0, x1 + 1):
        P(im, x, y0, light)
        P(im, x, y1, dark)
    for y in range(y0, y1 + 1):
        P(im, x0, y, light)
        P(im, x1, y, dark)
    return im


def glow_ramp(c, n=5):
    return tex.ramp(c, n, 0.5, hue_shift=0.06)


def frames_pulse(base, steps, fn):
    """Build animation frames by calling fn(img_copy, step)."""
    out = []
    for i in range(steps):
        f = base.copy()
        fn(f, i)
        out.append(f)
    return out


def save(im, name):
    return tex.save(im, M, name)


def save_anim(frames, name, frametime=4, interpolate=False):
    return tex.save_animated(frames, M, name, frametime, interpolate)


def sprite_finish(im, outline_col=None):
    return tex.outline(im, outline_col)
