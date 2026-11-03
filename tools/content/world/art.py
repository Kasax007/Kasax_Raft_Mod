"""Pixel art of the natural blocks and items of the raft dimension (module world, docs/design/beachcraft3.md 4.3).

Vanilla style: 16x16, one hue-shifted ramp per material, light from the top-left, no pure black. Ground blocks reuse
the pixel structure (luminance) of the matching vanilla block and put our own ramp on it, so they sit naturally next
to vanilla sand/gravel/basalt. Plants and items are hand-placed pixel shapes.
"""
import math
import random

from PIL import Image

from bclib import tex

T = (0, 0, 0, 0)


def img(w=16, h=16):
    return Image.new('RGBA', (w, h), T)


def P(im, x, y, c):
    if 0 <= x < im.width and 0 <= y < im.height and c is not None:
        im.putpixel((x, y), tex.rgb(c))


def ramp(base, n=5, spread=0.55, hue_shift=0.04, sat_shift=0.08):
    return tex.ramp(base, n, spread, hue_shift, sat_shift)


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def recolor(src, shades, contrast=1.0, bias=0.0):
    """Map the luminance of src (by rank, so the whole ramp is used) onto shades (dark -> light)."""
    w, h = src.size
    vals = sorted(lum(src.getpixel((x, y))) for y in range(h) for x in range(w))
    out = img(w, h)
    k = len(shades)
    for y in range(h):
        for x in range(w):
            c = src.getpixel((x, y))
            if c[3] == 0:
                continue
            # rank of this pixel's luminance
            lo, hi = 0, len(vals)
            v = lum(c)
            while lo < hi:
                m = (lo + hi) // 2
                if vals[m] < v:
                    lo = m + 1
                else:
                    hi = m
            t = lo / max(1, len(vals) - 1)
            t = 0.5 + (t - 0.5) * contrast + bias
            out.putpixel((x, y), shades[max(0, min(k - 1, int(t * k)))])
    return out


def frame_strip(src):
    """Split a vertical animation strip into frames."""
    n = src.height // src.width
    return [src.crop((0, i * src.width, src.width, (i + 1) * src.width)) for i in range(n)]


def paint(im, rows, pal, ox=0, oy=0):
    """rows: strings, each char a key of pal ('.' or ' ' = skip)."""
    for j, row in enumerate(rows):
        for i, ch in enumerate(row):
            if ch in ' .':
                continue
            P(im, ox + i, oy + j, pal[ch])
    return im


# =============================================================================================== ground blocks
def shell_sand():
    base = recolor(tex.load('minecraft:block/sand'), ramp('#e6d4a4', 5, 0.24, 0.03, 0.05), contrast=0.9)
    cream = ramp('#f2e6d2', 4, 0.3)
    pink = ramp('#e8a69a', 4, 0.32)
    shadow = tex.mix('#c8b27e', '#8d7a55', 0.4)
    # small shell fragments: scallops (3x2), spirals (2x2), chips (1px + shadow); kept away from the edges a bit
    rnd = random.Random(7)
    spots = [(2, 3), (10, 2), (6, 8), (13, 9), (2, 12), (9, 13), (12, 5)]
    for i, (x, y) in enumerate(spots):
        kind = i % 3
        r = pink if i in (1, 4) else cream
        if kind == 0:   # scallop: fan with ribs
            paint(base, ['abc', '.d.'], {'a': r[2], 'b': r[3], 'c': r[2], 'd': r[1]}, x, y)
            P(base, x + 1, y + 2, shadow)
            P(base, x + 3, y + 1, shadow)
        elif kind == 1:  # spiral
            paint(base, ['ab', 'ca'], {'a': r[2], 'b': r[3], 'c': r[1]}, x, y)
            P(base, x + 2, y + 1, shadow)
            P(base, x + 1, y + 2, shadow)
        else:            # chip
            P(base, x, y, r[3])
            P(base, x + 1, y, r[2])
            P(base, x + 1, y + 1, shadow)
        rnd.random()
    return base


def black_sand():
    sh = ramp('#3d3a44', 5, 0.2, 0.05, 0.05)
    base = recolor(tex.load('minecraft:block/sand'), sh, contrast=0.8)
    glint = ['#8a8296', '#6f6a80']
    for i, (x, y) in enumerate([(3, 2), (12, 4), (7, 9), (1, 13), (14, 12), (9, 15)]):
        P(base, x, y, glint[i % 2])
    return base


def volcanic_ash():
    sh = ramp('#77716b', 5, 0.2, 0.02, 0.03)
    base = recolor(tex.load('minecraft:block/sand'), sh, contrast=0.8)
    cinder = tex.mix('#4a4644', '#5a514a', 0.5)
    ember = '#8c5a44'
    for i, (x, y) in enumerate([(4, 3), (11, 2), (8, 10), (2, 9), (13, 13), (6, 14)]):
        P(base, x, y, cinder)
        if i % 3 == 0:
            P(base, x + 1, y, ember)
    return base


def pumice():
    sh = ramp('#c4bcab', 5, 0.26, 0.03, 0.05)
    base = recolor(tex.load('minecraft:block/calcite'), sh, contrast=0.7)
    hole = tex.mix(sh[0], '#6d6658', 0.5)
    rim = sh[4]
    rnd = random.Random(11)
    pts = []
    while len(pts) < 17:
        x, y = rnd.randrange(16), rnd.randrange(16)
        if all(abs(x - a) + abs(y - b) > 2 for a, b in pts):
            pts.append((x, y))
    for i, (x, y) in enumerate(pts):
        P(base, x, y, hole)
        if i % 3 == 0:   # bigger pore
            P(base, (x + 1) % 16, y, sh[1])
        P(base, (x + 1) % 16, (y + 1) % 16, rim)
    return base


def bubble_vent():
    """side, top frames (animated glow of the vent), bottom"""
    bas = ramp('#3d3f4a', 5, 0.3, 0.05, 0.04)
    side = recolor(tex.load('minecraft:block/basalt_side'), bas, contrast=1.0)
    # mineral crust streaks (pale, sulfur) near the top of the side
    for x in (2, 3, 7, 11, 12, 14):
        P(side, x, 0, '#b8b28e')
        if x % 2:
            P(side, x, 1, '#8c8a72')
    top_base = recolor(tex.load('minecraft:block/basalt_top'), bas, contrast=1.0)
    # pale mineral crust around the vent mouth (sulfur and salt deposits)
    crust = ['#c8c09a', '#a8a282', '#8c8a72']
    for (x, y, k) in [(5, 4, 1), (6, 3, 2), (9, 4, 0), (10, 5, 1), (4, 7, 0), (4, 9, 2), (11, 8, 1), (11, 10, 0),
                      (6, 11, 1), (9, 11, 0), (7, 12, 2), (3, 6, 2), (12, 6, 2), (8, 3, 1)]:
        P(top_base, x, y, crust[k])
    mouth = [(7, 6), (8, 6), (6, 7), (7, 7), (8, 7), (9, 7), (6, 8), (7, 8), (8, 8), (9, 8), (7, 9), (8, 9)]
    rim = [(7, 5), (8, 5), (6, 6), (9, 6), (5, 7), (10, 7), (5, 8), (10, 8), (6, 9), (9, 9), (7, 10), (8, 10)]
    cracks = [[(5, 6), (4, 5), (3, 5), (2, 4)], [(10, 9), (11, 9), (12, 10), (13, 11)], [(9, 5), (10, 4), (11, 3)],
              [(6, 10), (5, 11), (4, 12), (4, 13)]]
    frames = []
    glow = [('#b8461e', '#f08a34', '#ffd27a'), ('#c8521f', '#f89a40', '#ffe090'), ('#d8622a', '#ffb050', '#fff0b0'),
            ('#c8521f', '#f89a40', '#ffe090'), ('#b8461e', '#f08a34', '#ffd27a'), ('#a83e1c', '#e07c30', '#f8c470')]
    for i, (g0, g1, g2) in enumerate(glow):
        t = top_base.copy()
        for (x, y) in rim:
            P(t, x, y, g0)
        for (x, y) in mouth:
            P(t, x, y, bas[0])
        P(t, 7, 7, '#2a2228')
        P(t, 8, 8, '#2a2228')
        # glowing fissures, brightest near the mouth
        for c_ in cracks:
            for k, (x, y) in enumerate(c_):
                P(t, x, y, g2 if k == 0 else g1 if k < 2 else g0)
        P(t, 6 + (i % 2), 6, g2)
        frames.append(t)
    bottom = recolor(tex.load('minecraft:block/basalt_top'), bas, contrast=1.0)
    return side, frames, bottom


# =============================================================================================== island plants
GRASS = ramp('#9fb46a', 5, 0.42, 0.05, 0.08)
LEAF = ramp('#4f9a3a', 5, 0.42, 0.06, 0.1)
DEEP_LEAF = ramp('#2f7a3a', 5, 0.4, 0.06, 0.1)


def blade(im, x, y0, h, lean, shades, tip=None):
    """A grass blade from the ground (y0 = bottom row) up h pixels, leaning lean pixels over its height."""
    for i in range(h):
        t = i / max(1, h - 1)
        xx = x + round(lean * t * t)
        c = shades[1] if i < h // 3 else shades[2] if i < 2 * h // 3 else shades[3]
        if tip and i == h - 1:
            c = tip
        P(im, xx, y0 - i, c)


def beach_grass():
    im = img()
    g = GRASS
    for x, h, lean in [(2, 9, -2), (4, 12, -1), (5, 7, 1), (7, 14, 0), (8, 10, 2), (10, 12, 1), (11, 8, 3),
                       (13, 10, 2), (6, 5, -1), (9, 6, 0)]:
        blade(im, x, 15, h, lean, g, tip=g[4] if h > 9 else None)
    return im


def sea_oats():
    lower, upper = img(), img()
    g = GRASS
    for x, h, lean in [(3, 12, -2), (5, 15, -1), (7, 16, 0), (9, 14, 1), (11, 13, 2), (6, 8, 1), (12, 7, 2)]:
        blade(lower, x, 15, h, lean, g)
    # stems going on into the upper half, with drooping golden seed heads
    seed = ramp('#d6b26a', 4, 0.36, 0.05, 0.06)
    stem = g[2]
    for x, h, lean, heads in [(5, 12, -2, 3), (7, 14, 0, 4), (9, 11, 2, 3), (11, 8, 3, 2)]:
        pts = []
        for i in range(h):
            t = i / max(1, h - 1)
            xx = x + round(lean * t * t)
            P(upper, xx, 15 - i, stem)
            pts.append((xx, 15 - i))
        # spikelets hanging from the upper part of the stem, alternating sides
        for k in range(heads):
            px_, py_ = pts[h - 2 - k * 2] if h - 2 - k * 2 >= 0 else pts[0]
            side = -1 if k % 2 == 0 else 1
            P(upper, px_ + side, py_, seed[2])
            P(upper, px_ + side, py_ + 1, seed[1])
            P(upper, px_ + side * 2, py_ + 1, seed[3] if k == 0 else seed[2])
        tx, ty = pts[-1]
        P(upper, tx, ty, seed[3])
    return lower, upper


def hibiscus():
    im = img()
    stem = LEAF
    for y in range(9, 16):
        P(im, 8 if y > 12 else 7, y, stem[1])
    # leaves
    paint(im, ['..ab', '.abc', 'abc.'], {'a': stem[1], 'b': stem[2], 'c': stem[3]}, 8, 11)
    paint(im, ['ba..', 'cba.', '.cba'], {'a': stem[1], 'b': stem[2], 'c': stem[3]}, 3, 12)
    red = ramp('#e3283c', 5, 0.42, 0.04, 0.08)
    rows = ['...bcb...',
            '..bccdb..',
            '.ab.dd.ba',
            'abcdeedcb',
            'bccdyydcc',
            'abcdeedcb',
            '.ab.dd.ba',
            '..bccdb..',
            '...aba...']
    paint(im, rows, {'a': red[0], 'b': red[1], 'c': red[2], 'd': red[3], 'e': '#7a1428', 'y': '#ffe070'}, 3, 1)
    P(im, 8, 3, '#ffe070')   # stamen sticking out
    P(im, 9, 2, '#fff4b0')
    return im


def bird_of_paradise():
    lower, upper = img(), img()
    lf = DEEP_LEAF
    # paddle leaves on long stalks
    for x, top, lean in [(4, 2, -2), (7, 0, 0), (11, 3, 2)]:
        for y in range(top + 5, 16):
            P(lower, x + (lean if y < 9 else 0) // 2, y, lf[1])
        for y in range(top, top + 6):
            w = 1 if y in (top, top + 5) else 2
            cx = x + lean // 2
            for dx in range(-w, w + 1):
                P(lower, cx + dx, y, lf[3] if dx < 0 else lf[2])
            P(lower, cx, y, lf[4] if y > top else lf[3])
    # upper: one stem with the bird head flower
    for y in range(8, 16):
        P(upper, 7, y, lf[1] if y > 11 else lf[2])
    beak = ramp('#4c6a4a', 4, 0.4)
    orange = ramp('#f08a24', 5, 0.42, 0.05, 0.06)
    blue = ramp('#3a5ad8', 4, 0.42)
    # sheath ("beak") horizontal from the stem towards the left, slightly up
    paint(upper, ['abbbccd', '.aabbcc'], {'a': beak[0], 'b': beak[1], 'c': beak[2], 'd': '#8a3a5a'}, 2, 8)
    # orange crest feathers fanning up and back
    for (x, y, c) in [(7, 7, 3), (8, 6, 3), (8, 5, 4), (9, 4, 4), (6, 6, 2), (6, 5, 3), (6, 4, 4), (7, 3, 4),
                      (5, 5, 2), (4, 4, 3), (4, 3, 4), (9, 6, 2), (10, 5, 3), (11, 4, 4), (7, 5, 3), (5, 3, 3)]:
        P(upper, x, y, orange[c])
    # blue tongue
    for (x, y, c) in [(3, 7, 2), (4, 6, 3), (2, 7, 1), (3, 6, 3)]:
        P(upper, x, y, blue[c])
    return lower, upper


def tropical_fern():
    """Tropical fern: the frond structure of the vanilla fern and large fern (both drawn grey for biome tinting)
    layered into a fuller clump and painted with a fixed lush green ramp (no tint, so it stays bright everywhere)."""
    lf = ramp('#3f9a3a', 5, 0.46, 0.07, 0.1)
    fern = recolor(tex.load('minecraft:block/fern'), lf, contrast=1.1)
    big = recolor(tex.load('minecraft:block/large_fern_top'), lf[:4], contrast=1.0)
    im = img()
    # the broad large-fern fronds behind, pushed down so they grow from the ground
    im.alpha_composite(big.crop((0, 0, 16, 13)), (0, 3))
    im.alpha_composite(fern, (0, 0))
    return im


PINE_LEAF = ramp('#5f9a5a', 5, 0.42, 0.08, 0.08)
PINE_GOLD = ramp('#e3a82a', 5, 0.46, 0.05, 0.06)
PINE_GREEN = ramp('#8fa63a', 5, 0.42, 0.05, 0.06)


def spiky_rosette(im, cx, base_y, size, shades):
    """Pineapple-like rosette of stiff pointed leaves (2 px wide at the base, sharp lit tips)."""
    leaves = [(-1.0, 0.6), (1.0, 0.55), (-0.65, 0.85), (0.7, 0.8), (-0.3, 1.0), (0.3, 0.95), (0.0, 0.75)]
    for k, (dirx, hgt) in enumerate(leaves):
        length = max(2, round(size * hgt))
        for i in range(length):
            t = i / max(1, length - 1)
            x = cx + dirx * size * 0.55 * t
            y = base_y - length * (1.0 - 0.3 * abs(dirx)) * t
            c = shades[1] if t < 0.25 else shades[2] if t < 0.6 else shades[3]
            P(im, round(x), round(y), c)
            if t < 0.55:
                off = -1 if dirx > 0.2 else 1
                P(im, round(x) + off, round(y), shades[1] if t < 0.3 else shades[2])
        tx = cx + dirx * size * 0.55
        ty = base_y - length * (1.0 - 0.3 * abs(dirx))
        P(im, round(tx), round(ty), shades[4])


def fruit(im, cx, top, w, h, shades, crown=True):
    """Pineapple fruit: oval with the diamond pattern, small crown on top."""
    for y in range(h):
        t = y / max(1, h - 1)
        half = max(1, round(w / 2 * math.sin(math.pi * (0.15 + 0.8 * t))))
        for x in range(-half, half):
            px_, py_ = cx + x, top + y
            dia = (px_ + py_) % 3 == 0 or (px_ - py_) % 3 == 0
            c = shades[1] if dia else shades[3] if x < 0 else shades[2]
            if x == -half:
                c = shades[1]
            P(im, px_, py_, c)
        if y == 1:
            P(im, cx - 1, top + y, shades[4])
    if crown:
        lf = PINE_LEAF
        for (x, y, c) in [(0, -1, 2), (-1, -1, 1), (1, -2, 3), (-1, -3, 3), (0, -3, 2), (2, -3, 3), (-2, -2, 2)]:
            P(im, cx + x, top + y, lf[c])


def pineapple_bush(stage):
    im = img()
    size = [5, 7, 9, 9][stage]
    spiky_rosette(im, 8, 15, size, PINE_LEAF)
    if stage == 2:
        fruit(im, 8, 6, 4, 5, PINE_GREEN)
    if stage == 3:
        fruit(im, 8, 4, 6, 7, PINE_GOLD)
    return im


def pineapple_crop(stage):
    """Crop texture (drawn on the 4 planes of the crop model)."""
    im = img()
    size = [3, 4, 5, 6, 7, 8, 9, 9][stage]
    lf = PINE_LEAF
    for cx in (4, 11):
        spiky_rosette(im, cx, 15, size, lf)
    if stage == 5:
        fruit(im, 8, 10, 4, 4, PINE_GREEN, crown=False)
    if stage == 6:
        fruit(im, 8, 8, 5, 6, PINE_GREEN)
    if stage == 7:
        fruit(im, 8, 6, 6, 8, PINE_GOLD)
    return im


# =============================================================================================== sea life
def glow_kelp(plant):
    """Animated frames: the vanilla kelp shape in teal with glowing bulbs that pulse."""
    src = tex.load('minecraft:block/kelp_plant' if plant else 'minecraft:block/kelp')
    frames = frame_strip(src)
    sh = ramp('#2f8a78', 5, 0.42, 0.06, 0.08)
    out = []
    pulse = [0, 1, 2, 3, 2, 1]
    bulbs = [(5, 3), (10, 8), (6, 12)] if plant else [(9, 4), (6, 10)]
    for i in range(len(pulse)):
        f = recolor(frames[i % len(frames)], sh, contrast=1.0)
        lvl = pulse[i]
        cols = [('#4fd0c8', '#b8fff4'), ('#5ee0d4', '#d0fff8'), ('#74f0e0', '#ecfffc'), ('#8cfff0', '#ffffff')][lvl]
        for (x, y) in bulbs:
            # bulb sits on the frond: find the nearest opaque pixel in this row
            row = [xx for xx in range(16) if f.getpixel((xx, y))[3] > 0]
            if not row:
                continue
            bx = min(row, key=lambda xx: abs(xx - x))
            P(f, bx, y, cols[0])
            P(f, bx, y - 1, cols[1])
            P(f, bx + (1 if bx < 8 else -1), y, cols[0])
        out.append(f)
    return out


def anemone(base, tip, glow=False):
    """Stalk with a crown of tentacles curling up and out."""
    im = img()
    sh = ramp(base, 5, 0.42, 0.04, 0.08)
    # column
    for y in range(11, 16):
        for x in range(6, 10):
            P(im, x, y, sh[2] if x < 8 else sh[1])
    P(im, 6, 15, sh[0])
    P(im, 9, 15, sh[0])
    # oral disc
    for x in range(5, 11):
        P(im, x, 10, sh[3])
    # tentacles
    tips = ramp(tip, 3, 0.4)
    for x0, dx, h in [(5, -4, 6), (6, -2, 8), (7, -1, 9), (8, 1, 9), (9, 2, 8), (10, 4, 6), (7, 0, 6), (9, 3, 5),
                      (6, -3, 4)]:
        for i in range(h):
            t = i / max(1, h - 1)
            x = x0 + round(dx * math.sin(t * math.pi / 2))
            y = 10 - i
            c = sh[3] if t < 0.5 else sh[4]
            if i >= h - 2:
                c = tips[1] if i == h - 2 else tips[2]
            P(im, x, y, c)
    return im


def anemone_glow_frames():
    frames = []
    for k, (b, t) in enumerate([('#2a8c9c', '#7ff4ff'), ('#2a8c9c', '#a8fbff'), ('#2a8c9c', '#d8ffff'),
                                ('#2a8c9c', '#a8fbff')]):
        frames.append(anemone(b, t))
    return frames


def tube_worms():
    im = img()
    tube = ramp('#e8e2d4', 5, 0.36, 0.04, 0.05)
    plume = ramp('#d8283a', 4, 0.42)
    for x, h in [(2, 7), (4, 11), (6, 6), (8, 13), (10, 9), (12, 12), (14, 6)]:
        for i in range(h):
            y = 15 - i
            P(im, x, y, tube[3] if i % 3 else tube[2])
            P(im, x + 1, y, tube[1])
        y = 15 - h
        # feathery red plume
        P(im, x, y, plume[2])
        P(im, x + 1, y, plume[1])
        P(im, x - 1, y - 1, plume[3])
        P(im, x, y - 1, plume[2])
        P(im, x + 1, y - 1, plume[2])
        P(im, x + 2, y - 1, plume[1])
        P(im, x, y - 2, plume[3])
    return im


def giant_clam():
    """shell (outer, ribbed), lip (side of the valves with a wavy rim), mantle (inner, vivid), pearl"""
    shell = ramp('#c9c4b4', 5, 0.38, 0.03, 0.05)
    s = img()
    for y in range(16):
        for x in range(16):
            rib = (x // 3) % 2
            c = shell[3] if rib else shell[2]
            if x % 3 == 0:
                c = shell[1]
            if (y % 4 == 0) and rib:
                c = shell[4]
            P(s, x, y, c)
    lip = img()
    for y in range(16):
        for x in range(16):
            rib = (x // 3) % 2
            c = shell[2] if rib else shell[1]
            if y % 8 in (0,):
                c = shell[4] if rib else shell[3]
            # zigzag rim of the valve: the top 2 rows of every 8-row band
            if y % 8 == 1:
                c = shell[0] if x % 3 == 1 else shell[3]
            P(lip, x, y, c)
    mantle = ramp('#2a7ad8', 5, 0.46, 0.06, 0.1)
    m = img()
    rnd = random.Random(5)
    for y in range(16):
        for x in range(16):
            d = abs(x - 7.5) + abs(y - 7.5) * 0.6
            c = mantle[3] if d < 4 else mantle[2] if d < 8 else mantle[1]
            P(m, x, y, c)
    for _ in range(14):
        x, y = rnd.randrange(16), rnd.randrange(16)
        P(m, x, y, '#7fe8ff' if rnd.random() < 0.5 else '#4cd8a0')
    # siphon slit in the middle
    for x in range(5, 11):
        P(m, x, 8, mantle[0])
    pearl = img()
    pr = ramp('#f2eee6', 4, 0.3)
    for y in range(16):
        for x in range(16):
            P(pearl, x, y, pr[2])
    for (x, y) in [(1, 1), (2, 1), (1, 2)]:
        P(pearl, x, y, '#ffffff')
    for y in range(16):
        P(pearl, 15, y, pr[0])
        P(pearl, y, 15, pr[0])
    return s, lip, m, pearl


STAR = ['......aa......',
        '.....abba.....',
        '.....abca.....',
        '....abccba....',
        'aaaaabccbaaaaa',
        'abbbbcccccbbba',
        '.abccccdcccba.',
        '..abccccccba..',
        '...abcccccba..',
        '...abccbccba..',
        '..abcba.abcba.',
        '..abba...abba.',
        '.aba......aba.',
        '.aa........aa.']


def starfish(colour):
    sh = ramp(colour, 5, 0.42, 0.05, 0.08)
    im = img()
    paint(im, STAR, {'a': sh[0], 'b': sh[1], 'c': sh[2], 'd': sh[3]}, 1, 1)
    # tubercles: light dots along the arms
    for (x, y) in [(7, 3), (7, 5), (3, 6), (5, 6), (10, 6), (12, 6), (6, 9), (9, 9), (5, 11), (10, 11)]:
        if im.getpixel((x, y))[3]:
            P(im, x, y, sh[4])
    return im


def sea_urchin():
    """core (body), spines (radiating, on the crossed planes)"""
    body = ramp('#64347e', 5, 0.4, 0.05, 0.06)
    core = img()
    for y in range(16):
        for x in range(16):
            c = body[2] if (x + y * 3) % 5 else body[3]
            if (x * 7 + y * 3) % 11 == 0:
                c = body[4]
            if (x + y) % 4 == 0:
                c = body[1]
            P(core, x, y, c)
    spines = img()
    sp = ramp('#8a5aaa', 4, 0.46)
    cx, cy = 7.5, 15.0
    for k in range(13):
        a = math.pi * (0.06 + 0.88 * k / 12)
        length = 5.5 + (k % 3)
        for i in range(2, int(length) + 1):
            x = round(cx + math.cos(a) * i)
            y = round(cy - math.sin(a) * i)
            P(spines, x, y, sp[1] if i < length - 2 else sp[3] if i == int(length) else sp[2])
    return core, spines


def barnacles():
    im = img()
    sh = ramp('#cfc8b8', 5, 0.4, 0.03, 0.04)
    hole = '#4a4640'
    rnd = random.Random(3)
    spots = [(2, 2), (6, 1), (11, 3), (3, 7), (8, 6), (13, 8), (1, 12), (6, 11), (10, 12), (14, 14), (9, 15),
             (12, 1)]
    for i, (x, y) in enumerate(spots):
        big = i % 3 != 2
        if big:
            paint(im, ['.bc.', 'bdhc', 'abcb', '.aa.'], {'a': sh[0], 'b': sh[2], 'c': sh[3], 'd': sh[4], 'h': hole},
                  x - 1, y - 1)
        else:
            paint(im, ['bc', 'ah'], {'a': sh[1], 'b': sh[3], 'c': sh[2], 'h': hole}, x, y)
        rnd.random()
    return im


# shells atlas: (name, rows, palette) placed at known uv positions; models pick the regions
def seashell_atlas():
    im = img()
    cream = ramp('#f0dcc4', 4, 0.34)
    pink = ramp('#ec9c8c', 4, 0.36)
    conch = ramp('#e8a060', 4, 0.4)
    cow = ramp('#9a6a44', 4, 0.42)
    # scallop 4x4 at (0,0)
    paint(im, ['abba', 'bccb', 'bdcb', '.aa.'], {'a': pink[1], 'b': pink[2], 'c': pink[3], 'd': '#fff0e8'}, 0, 0)
    # scallop cream 4x4 at (4,0)
    paint(im, ['abba', 'bccb', 'bdcb', '.aa.'], {'a': cream[0], 'b': cream[1], 'c': cream[2], 'd': cream[3]}, 4, 0)
    # conch 4x5 at (8,0)
    paint(im, ['.ab.', 'abcb', 'bcdb', 'abca', '.aa.'], {'a': conch[0], 'b': conch[1], 'c': conch[2], 'd': '#fff4e0'}, 8, 0)
    # cowrie 3x4 at (12,0)
    paint(im, ['aba', 'bcb', 'bdb', 'aba'], {'a': cow[0], 'b': cow[1], 'c': cow[3], 'd': '#f4e8d0'}, 12, 0)
    # sides (darker) row at y 6..7
    for x in range(16):
        P(im, x, 6, cream[1])
        P(im, x, 7, cream[0])
    for x in range(0, 4):
        P(im, x, 6, pink[1])
        P(im, x, 7, pink[0])
    for x in range(8, 12):
        P(im, x, 6, conch[1])
        P(im, x, 7, conch[0])
    for x in range(12, 16):
        P(im, x, 6, cow[1])
        P(im, x, 7, cow[0])
    return im


def sea_glass_atlas():
    """3 frosted pebbles (aqua, green, blue), translucent."""
    im = img()
    cols = [ramp('#7fd6c6', 4, 0.36), ramp('#8fd68a', 4, 0.36), ramp('#6aa8e0', 4, 0.36)]
    for i, sh in enumerate(cols):
        ox = i * 5
        a = 190
        rows = ['abba', 'bccb', 'bcdb', 'abba']
        pal = {k: (c[0], c[1], c[2], a) for k, c in zip('abcd', sh)}
        pal['d'] = (sh[3][0], sh[3][1], sh[3][2], 230)
        paint(im, rows, pal, ox, 0)
        # side strip
        for x in range(ox, ox + 4):
            P(im, x, 5, (sh[1][0], sh[1][1], sh[1][2], a))
            P(im, x, 6, (sh[0][0], sh[0][1], sh[0][2], a))
    return im


# =============================================================================================== items
def item_finish(im):
    tex.outline(im)
    return im


def pineapple_item():
    im = img()
    fruit(im, 8, 6, 8, 9, PINE_GOLD, crown=False)
    lf = PINE_LEAF
    for (x, y, c) in [(8, 5, 2), (7, 5, 1), (8, 4, 3), (9, 4, 2), (7, 3, 3), (6, 2, 3), (9, 3, 3), (10, 2, 4),
                      (8, 2, 2), (8, 1, 3), (5, 3, 2), (11, 3, 3)]:
        P(im, x, y, lf[c])
    return item_finish(im)


def pineapple_crown_item():
    im = img()
    lf = PINE_LEAF
    spiky_rosette(im, 8, 13, 10, lf)
    # cut base of the fruit
    paint(im, ['abba', 'bccb'], {'a': PINE_GOLD[1], 'b': PINE_GOLD[2], 'c': PINE_GOLD[3]}, 6, 13)
    return item_finish(im)


def pearl_item():
    im = img()
    pr = ramp('#efe8e2', 5, 0.34, 0.03, 0.04)
    for y in range(4, 12):
        for x in range(4, 12):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 4.0:
                t = math.hypot(x - 6.0, y - 6.0)
                c = pr[4] if t < 1.2 else pr[3] if t < 2.6 else pr[2] if t < 4.0 else pr[1]
                P(im, x, y, c)
    # iridescent sheen
    P(im, 9, 9, '#d8c8f0')
    P(im, 10, 8, '#c8e0f0')
    P(im, 8, 10, '#f0d0d8')
    P(im, 6, 6, '#ffffff')
    return item_finish(im)


def seashell_item():
    im = img()
    sh = ramp('#f0b8a0', 5, 0.42, 0.04, 0.06)
    # scallop: fan with ribs and a hinge
    for y in range(3, 12):
        t = (y - 3) / 8
        half = round(1 + 5.5 * math.sin(math.pi * 0.5 * min(1, t * 1.2)))
        for x in range(8 - half, 8 + half):
            rib = (x - (8 - half)) % 2
            c = sh[3] if rib else sh[2]
            if y == 3:
                c = sh[4]
            P(im, x, y, c)
    for x in range(5, 11):
        P(im, x, 12, sh[1])
    paint(im, ['abba', '.ab.'], {'a': sh[1], 'b': sh[2]}, 6, 13)
    return item_finish(im)


def sea_glass_item():
    im = img()
    sh = ramp('#7fd6c6', 5, 0.4, 0.05, 0.06)
    rows = ['....aab...',
            '...abcca..',
            '..abccdca.',
            '.abccddcb.',
            'abcccdccba',
            'abccccccb.',
            '.abcccbb..',
            '..abbba...',
            '...aa.....']
    pal = {'a': sh[1], 'b': sh[2], 'c': sh[3], 'd': sh[4]}
    paint(im, rows, pal, 3, 4)
    for (x, y) in [(7, 5), (6, 6)]:
        P(im, x, y, '#f0fffc')
    # frosted: slightly translucent body
    for y in range(16):
        for x in range(16):
            c = im.getpixel((x, y))
            if c[3]:
                im.putpixel((x, y), (c[0], c[1], c[2], 225))
    return item_finish(im)


def urchin_spine_item():
    im = img()
    sh = ramp('#6a3c80', 4, 0.42)
    for i in range(12):
        x, y = 2 + i, 13 - i
        P(im, x, y, sh[2] if i < 9 else sh[3])
        if i < 6:
            P(im, x + 1, y, sh[1])
    P(im, 14, 1, '#d8c0e8')
    return item_finish(im)


def glow_kelp_item():
    im = img()
    sh = ramp('#2f8a78', 5, 0.42, 0.06, 0.08)
    pts = []
    for i in range(13):
        x = 8 + round(2 * math.sin(i * 0.7))
        y = 14 - i
        pts.append((x, y))
        P(im, x, y, sh[2])
        P(im, x + 1, y, sh[1])
    for k, (x, y) in enumerate(pts):
        if k % 3 == 1:
            side = -1 if k % 2 else 2
            P(im, x + side, y, sh[3])
            P(im, x + side, y - 1, sh[4])
    for (x, y) in [pts[3], pts[7], pts[11]]:
        P(im, x - 1, y, '#74f0e0')
        P(im, x - 1, y - 1, '#ecfffc')
    return item_finish(im)


def pineapple_bush_item():
    return pineapple_bush(3)
