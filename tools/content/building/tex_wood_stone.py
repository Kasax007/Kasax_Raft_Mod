"""Textures: wood (weathered, teak), sea glass, coral stone, tidestone."""
import math
import random

from .art import *  # noqa: F401,F403


# ================================================================================================= wood
def _planks(p, seed, tones, joints, nails=(), cracks=(), caulk=None, seam_drop=2, grain=(3, 9)):
    """4 boards of 4 rows: lit top edge, two rows of long grain, darker seam row; butt joints only where
    joints[b] is not None; nails; cracks. p: palette (7 shades). tones: base shade per board."""
    rnd = random.Random(seed)
    img = new()
    top = len(p) - 1
    for b in range(4):
        y0, t = b * 4, tones[b]
        for y in range(y0, y0 + 4):
            for x in range(16):
                put(img, x, y, p[t])
        for x in range(16):  # lit top edge, broken now and then
            if rnd.random() < 0.7:
                put(img, x, y0, p[min(top, t + 1)])
        for y in (y0 + 1, y0 + 2):  # long grain streaks
            x = rnd.randrange(16)
            done = 0
            while done < 14:
                ln = rnd.randrange(*grain)
                d = -1 if rnd.random() < 0.55 else 1
                for k in range(ln):
                    put(img, x + k, y, p[max(0, min(top, t + d))])
                gap = rnd.randrange(2, 5)
                x += ln + gap
                done += ln + gap
        for x in range(16):  # seam under the board
            put(img, x, y0 + 3, caulk if caulk else p[max(0, t - seam_drop - (1 if rnd.random() < 0.2 else 0))])
        j = joints[b]
        if j is not None:
            for y in range(y0, y0 + 3):
                put(img, j, y, caulk if caulk else p[max(0, t - 2)])
                put(img, j + 1, y, p[min(top, t + 1)])
    for (x, y) in nails:  # nail head
        put(img, x, y, p[1])
        put(img, x, y - 1, p[min(top, 5)])
    for path in cracks:
        for (x, y) in path:
            put(img, x, y, p[2])
    return img


def weathered_planks():
    img = _planks(WEATHERED, 33, tones=[4, 3, 4, 3], joints=[None, 9, None, 3], seam_drop=2,
                  nails=[(7, 6), (12, 6), (1, 14), (6, 14)],
                  cracks=[[(1, 2), (2, 2), (3, 2), (4, 2)], [(10, 10), (11, 10), (12, 10)]])
    for (x, y) in [(11, 1), (12, 1), (13, 1), (4, 9), (5, 9), (6, 9)]:  # sun-bleached highlights
        put(img, x, y, WEATHERED[6])
    return img


def teak_planks():
    caulk = tex.rgb('#24150d')
    img = _planks(TEAK, 19, tones=[3, 2, 3, 2], joints=[11, None, 4, None], caulk=caulk)
    for (x, y) in [(2, 0), (3, 0), (4, 0), (8, 4), (9, 4), (13, 8), (14, 8), (6, 12), (7, 12)]:
        put(img, x, y, TEAK[5])
    return img


def teak_door_bottom():
    """Lower half: frame and two raised panels with a brass knob and iron hinge straps."""
    p = TEAK
    img = new(p[3])
    shade_fill(img, p, 3, 4, 401, cell=4, contrast=0.8)
    # vertical grain on the stiles
    for x in (0, 1, 14, 15):
        for y in range(16):
            put(img, x, y, p[4] if (y + x) % 5 else p[3])
    bevel(img, 0, 0, 15, 15, p[5], p[1])
    # two raised panels
    for (x0, x1) in ((3, 7), (8, 12)):
        fill(img, x0, 1, x1, 12, p[4])
        for y in range(2, 12):
            if (y * 3 + x0) % 7 == 0:
                put(img, x0 + 2, y, p[3])
        bevel(img, x0, 1, x1, 12, p[5], p[2])
        bevel(img, x0 + 1, 2, x1 - 1, 11, p[4], p[3])
    fill(img, 1, 13, 14, 14, p[2])  # kick rail
    for x in range(1, 15):
        put(img, x, 13, p[3])
    # iron hinge straps on the hinge side (left)
    iron = STEEL
    for y in (3, 10):
        fill(img, 0, y, 4, y, iron[3])
        put(img, 0, y, iron[4])
        put(img, 3, y, iron[1])
    # brass knob on the right
    put(img, 13, 1, BRASS[4])
    put(img, 13, 2, BRASS[3])
    put(img, 12, 2, BRASS[2])
    put(img, 13, 3, BRASS[1])
    return img


def teak_door_top():
    """Upper half: frame with a small four-pane window (cutout) and an iron strap."""
    p = TEAK
    img = new(p[3])
    shade_fill(img, p, 3, 4, 402, cell=4, contrast=0.8)
    for x in (0, 1, 14, 15):
        for y in range(16):
            put(img, x, y, p[4] if (y + x) % 5 else p[3])
    bevel(img, 0, 0, 15, 15, p[5], p[1])
    # window 10x7 with mullions
    fill(img, 3, 3, 12, 9, (0, 0, 0, 0))
    bevel(img, 2, 2, 13, 10, p[2], p[5])
    for y in range(3, 10):
        put(img, 7, y, p[3])
        put(img, 8, y, p[2])
    for x in range(3, 13):
        put(img, x, 6, p[3])
    # lower panel
    fill(img, 3, 12, 12, 15, p[4])
    bevel(img, 3, 12, 12, 15, p[5], p[2])
    for y in (13,):
        fill(img, 0, y, 4, y, STEEL[3])
        put(img, 0, y, STEEL[4])
        put(img, 3, y, STEEL[1])
    return img


def teak_trapdoor():
    """A ship's hatch grating: teak lattice with square openings and a solid frame."""
    p = TEAK
    img = new()
    fill(img, 0, 0, 15, 15, p[3])
    shade_fill(img, p, 3, 4, 403, cell=4, contrast=0.6)
    bevel(img, 0, 0, 15, 15, p[5], p[1])
    bevel(img, 1, 1, 14, 14, p[4], p[2])
    for gy in range(3):
        for gx in range(3):
            x0, y0 = 3 + gx * 4, 3 + gy * 4
            fill(img, x0, y0, x0 + 1, y0 + 1, (0, 0, 0, 0))
            put(img, x0 - 1, y0 - 1, p[2])
            put(img, x0 + 2, y0 + 2, p[5])
    # lattice crossings
    for gy in range(4):
        for gx in range(4):
            put(img, 2 + gx * 4, 2 + gy * 4, p[5])
    return img


def teak_door_item(bottom, top):
    """Item sprite: the door shrunk to 8x16 with a darker outline."""
    from PIL import Image
    door = Image.new('RGBA', (16, 32))
    door.alpha_composite(top, (0, 0))
    door.alpha_composite(bottom, (0, 16))
    small = door.resize((8, 16), Image.NEAREST)
    img = new()
    img.alpha_composite(small.crop((0, 0, 8, 15)), (4, 0))
    return img


# ================================================================================================= sea glass
SEA_GLASS = {
    'aqua': pal('#3f9e91', '#62bfb1', '#86d8c9', '#b3eee2', '#e2fbf5'),
    'green': pal('#3f8650', '#5ea56c', '#80c28a', '#acdcae', '#e0f6df'),
    'blue': pal('#3d6f9e', '#5b8fbe', '#7fb0d8', '#acd2ee', '#e0f1fb'),
}


def sea_glass(colour):
    """Frosted, translucent sea glass: soft cloudy body, light rim and two glints (vanilla glass language)."""
    p = SEA_GLASS[colour]
    img = new()
    n = noise({'aqua': 61, 'green': 62, 'blue': 63}[colour], cell=4, octaves=2)
    for y in range(16):
        for x in range(16):
            v = n[y][x]
            i = 1 if v < 0.42 else 2 if v < 0.62 else 3
            a = 150 + int(v * 50)
            img.putpixel((x, y), with_alpha(p[i], a))
    for i in range(16):  # rim: light top/left, deeper bottom/right
        img.putpixel((i, 0), with_alpha(p[3], 225))
        img.putpixel((0, i), with_alpha(p[3], 225))
        img.putpixel((i, 15), with_alpha(p[1], 225))
        img.putpixel((15, i), with_alpha(p[1], 225))
    img.putpixel((0, 15), with_alpha(p[2], 225))
    img.putpixel((15, 0), with_alpha(p[2], 225))
    for (x, y) in [(2, 2), (3, 2), (2, 3), (4, 5), (5, 4)]:  # glint
        img.putpixel((x, y), with_alpha(p[4], 235))
    for (x, y) in [(12, 12), (13, 11)]:
        img.putpixel((x, y), with_alpha(p[4], 220))
    return img


def sea_glass_pane_top(colour):
    p = SEA_GLASS[colour]
    img = new()
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), with_alpha(p[3] if (x + y * 3) % 7 else p[2], 225))
    return img


# ================================================================================================= coral stone
def _fossils(img, spots, light, mid, dark):
    """Branching coral twigs and tiny shells pressed into the stone."""
    for kind, x, y in spots:
        if kind == 'twig':     # a little branching coral, 4x4
            for (dx, dy) in [(1, 3), (1, 2), (1, 1), (0, 0), (2, 0), (2, 1)]:
                put(img, x + dx, y + dy, light)
            put(img, x + 2, y + 2, dark)
            put(img, x, y + 1, dark)
        elif kind == 'shell':  # spiral shell, 3x3
            for (dx, dy) in [(0, 0), (1, 0), (2, 0), (2, 1), (2, 2), (1, 2), (0, 1)]:
                put(img, x + dx, y + dy, light)
            put(img, x + 1, y + 1, mid)
            put(img, x + 1, y + 3, dark)
            put(img, x + 3, y + 1, dark)
        elif kind == 'pores':  # pair of pores
            put(img, x, y, dark)
            put(img, x + 2, y + 1, dark)
            put(img, x + 1, y + 1, light)


def coral_stone():
    p = CORAL
    img = new()
    shade_fill(img, p, 2, 5, 71, cell=4, octaves=3, contrast=1.0)
    # soft layered streaks (limestone bedding)
    for x in range(16):
        y = 5 + round(math.sin(x / 16 * 2 * math.pi) * 1.2)
        if img.getpixel((x, y)) != p[2]:
            put(img, x, y, p[5])
        y2 = 12 + round(math.sin((x + 5) / 16 * 2 * math.pi) * 1.2)
        put(img, x, y2, p[2])
    _fossils(img, [('twig', 1, 0), ('shell', 9, 7), ('pores', 12, 1), ('pores', 3, 9), ('twig', 11, 12)],
             FOSSIL[1], FOSSIL[0], p[1])
    return img


def polished_coral_stone():
    p = CORAL
    img = new()
    shade_fill(img, p, 3, 4, 72, cell=8, octaves=2, contrast=0.9)
    bevel(img, 0, 0, 15, 15, p[5], p[2], p[4])
    bevel(img, 1, 1, 14, 14, p[4], p[3])
    # faint fossil ghosts in the polish
    for (x, y) in [(4, 4), (5, 5), (5, 4), (10, 9), (11, 9), (11, 10), (6, 11), (7, 12)]:
        put(img, x, y, p[5])
    put(img, 5, 6, p[2])
    put(img, 12, 10, p[2])
    return img


def _small_bricks(p, mortar, seed, fossil=True):
    """4 rows of 8-wide bricks in running bond (like mud bricks), bevelled."""
    rnd = random.Random(seed)
    img = new(mortar)
    for r in range(4):
        y0 = r * 4
        off = 4 if r % 2 else 0
        for c in range(3):
            x0 = c * 8 - off
            t = rnd.choice([3, 3, 4, 4, 5])
            for y in range(y0, y0 + 3):
                for x in range(x0, x0 + 7):
                    if 0 <= x + 16 and True:
                        v = t
                        if y == y0:
                            v = min(6, t + 1)
                        elif y == y0 + 2:
                            v = max(0, t - 1)
                        if x == x0 + 6 and y != y0:
                            v = max(0, t - 1)
                        put(img, x, y, p[v])
            # a speck per brick
            if fossil and rnd.random() < 0.7:
                sx = x0 + rnd.randrange(1, 5)
                put(img, sx, y0 + 1, FOSSIL[1])
                put(img, sx + 1, y0 + 1, p[min(6, t + 1)])
    return img


def coral_stone_bricks():
    return _small_bricks(CORAL, CORAL[1], 73)


def mossy_coral_stone_bricks():
    img = _small_bricks(CORAL, CORAL[1], 73)
    m = blotch_mask(0.40, 77, cell=4)
    mortar_rows = {3, 7, 11, 15}
    for y in range(16):
        for x in range(16):
            if m(x, y):
                c = img.getpixel((x, y))
                if y in mortar_rows or c == CORAL[1]:
                    img.putpixel((x, y), ALGAE[1])
                else:
                    lum = sum(c[:3]) / 3
                    img.putpixel((x, y), ALGAE[3] if lum > 190 else ALGAE[2])
    # algae creeping in mortar lines
    for (x, y) in [(2, 3), (3, 3), (9, 7), (10, 7), (11, 7), (5, 11), (13, 15), (14, 15)]:
        put(img, x, y, ALGAE[2])
    return img


# ================================================================================================= tidestone
def tidestone():
    """Smooth ancient teal stone with faint flowing tide lines."""
    p = TIDE
    img = new()
    shade_fill(img, p, 3, 4, 81, cell=8, octaves=3, contrast=1.0)
    for x in range(16):
        y = 4 + round(math.sin(x / 16 * 2 * math.pi) * 1.4)
        put(img, x, y, p[5])
        if x % 4 == 1:
            put(img, x, y + 1, p[2])
        y = 11 + round(math.sin((x + 8) / 16 * 2 * math.pi) * 1.4)
        put(img, x, y, p[5] if x % 5 else p[4])
        if x % 4 == 3:
            put(img, x, y + 1, p[2])
    for (x, y) in [(2, 8), (12, 1), (9, 14)]:
        put(img, x, y, p[2])
    return img


def _big_bricks(p, seed, mortar=None):
    """Two rows of large ashlar blocks (top row one wide block, bottom row two halves), deep bevels."""
    mortar = mortar or p[1]
    img = new(mortar)
    shade_fill(img, p, 3, 4, seed, cell=4, octaves=2, contrast=0.8)
    blocks = [(0, 0, 15, 6), (-8, 8, 6, 14), (8, 8, 22, 14)]
    for (x0, y0, x1, y1) in blocks:
        for x in range(x0, x1 + 1):
            put(img, x, y0, p[5])
            put(img, x, y1, p[2])
        for y in range(y0, y1 + 1):
            put(img, x0, y, p[5] if y < y1 else p[3])
            put(img, x1, y, p[2])
    for x in range(16):  # mortar rows
        put(img, x, 7, mortar)
        put(img, x, 15, mortar)
    for y in range(8, 15):  # vertical joint of the lower row (x = 7 mortar)
        put(img, 7, y, mortar)
    # inner shading of the blocks
    for (x, y) in [(2, 2), (3, 2), (11, 4), (12, 4), (4, 11), (12, 10), (13, 10)]:
        put(img, x, y, p[5])
    return img


def tidestone_bricks():
    return _big_bricks(TIDE, 82)


def cracked_tidestone_bricks():
    img = _big_bricks(TIDE, 82)
    p = TIDE
    crack(img, [(5, 0), (5, 1), (6, 2), (6, 3), (5, 4), (5, 5), (6, 6)], p[0], None)
    crack(img, [(6, 3), (7, 3), (8, 4)], p[0])
    crack(img, [(12, 8), (11, 9), (11, 10), (12, 11), (12, 12), (13, 13), (13, 14)], p[0])
    crack(img, [(2, 9), (3, 10), (3, 11)], p[0])
    for (x, y) in [(6, 1), (7, 4), (12, 9), (13, 12), (4, 10)]:  # lit edges of the cracks
        put(img, x, y, p[5])
    # chipped corner
    for (x, y) in [(15, 6), (14, 6), (15, 5)]:
        put(img, x, y, p[1])
    return img


def mossy_tidestone_bricks():
    img = _big_bricks(TIDE, 82)
    m = blotch_mask(0.38, 85, cell=4)
    for y in range(16):
        for x in range(16):
            if m(x, y):
                c = img.getpixel((x, y))
                lum = sum(c[:3]) / 3
                img.putpixel((x, y), ALGAE[1] if lum < 80 else ALGAE[2] if lum < 115 else ALGAE[3])
    for (x, y) in [(3, 7), (4, 7), (5, 7), (7, 10), (7, 11), (12, 15), (13, 15)]:
        put(img, x, y, ALGAE[1])
    return img


EMBLEM = [  # the Tidewarden sigil, 12x12: a trident rising from a wave inside a gold ring.
    # G gold, g gold shade, H highlight, D carved groove, '.' stone
    '...DDDDDD...',
    '..DHGGGGgD..',
    '.DH.D.D.DgD.',
    'DH.DHDHDHDgD',
    'DG.DGDGDgDgD',
    'DG.DGGGGgDgD',
    'DG...DGD..gD',
    'DG.DD.Gg.DgD',
    'DGDHGD.DHGgD',
    '.DgD.GgGD.D.',
    '..DggggggD..',
    '...DDDDDD...',
]


def chiseled_tidestone():
    p = TIDE
    img = new()
    shade_fill(img, p, 3, 4, 86, cell=4, contrast=0.7)
    bevel(img, 0, 0, 15, 15, p[5], p[1], p[3])
    bevel(img, 1, 1, 14, 14, p[2], p[5])  # carved frame (inverted bevel)
    fill(img, 2, 2, 13, 13, p[3])
    shade_fill(img, p, 3, 4, 87, cell=8, contrast=0.6, box=(2, 2, 13, 13))
    # gold corner studs on the frame
    for (x, y) in [(1, 1), (14, 1), (1, 14), (14, 14)]:
        put(img, x, y, GOLD[3])
    m = {'G': GOLD[3], 'g': GOLD[2], 'H': GOLD[4], 'D': p[1]}
    for y, row in enumerate(EMBLEM):
        for x, ch in enumerate(row):
            if ch in m:
                put(img, 2 + x, 2 + y, m[ch])
    return img


def tidestone_pillar():
    """Fluted column shaft with a gold-dotted band at the top."""
    p = TIDE
    img = new()
    for x in range(16):
        col = [4, 5, 4, 3, 2][x % 5] if x not in (0, 15) else (5 if x == 0 else 1)
        for y in range(16):
            put(img, x, y, p[col])
    n = noise(88, 4)
    for y in range(16):
        for x in range(1, 15):
            if n[y][x] > 0.72:
                c = img.getpixel((x, y))
                idx = p.index(c) if c in p else 3
                put(img, x, y, p[min(6, idx + 1)])
    # band
    for x in range(16):
        put(img, x, 0, p[5])
        put(img, x, 1, p[2])
        put(img, x, 2, p[4])
        put(img, x, 3, p[1])
    for x in range(1, 16, 3):
        put(img, x, 2, GOLD[3])
    put(img, 0, 2, p[5])
    return img


def tidestone_pillar_top():
    p = TIDE
    img = new()
    shade_fill(img, p, 3, 4, 89, cell=4, contrast=0.6)
    bevel(img, 0, 0, 15, 15, p[5], p[1], p[3])
    ring(img, 8, 8, 5.0, 6.0, lambda x, y, a, d: lit(a, p, 3, 2))
    ring(img, 8, 8, 2.0, 3.0, lambda x, y, a, d: lit(a + math.pi, p, 3, 1))
    fill(img, 7, 7, 8, 8, GOLD[3])
    put(img, 7, 7, GOLD[4])
    put(img, 8, 8, GOLD[2])
    return img


RUNES = [  # carved Tidewarden glyphs in the faces of glowing tidestone ('R'): wave, eye and trident
    '................',
    '................',
    '..R.R.R...R.....',
    '..RRRRR..R.R....',
    '.........RRR....',
    '..R...R...R.....',
    '................',
    '................',
    '................',
    '..R.....R.R.R...',
    '..RR.....RRR....',
    '..R.R.....R.....',
    '..RR......R.....',
    '..R.......R.....',
    '................',
    '................',
]


def _rune_pixels():
    return [(x, y) for y, row in enumerate(RUNES) for x, ch in enumerate(row) if ch == 'R']


def glowing_tidestone():
    """Base: tidestone bricks with carved rune grooves and a faint teal halo around them."""
    img = _big_bricks(TIDE, 90)
    p = TIDE
    runes = set(_rune_pixels())
    for (x, y) in runes:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            q = (x + dx, y + dy)
            if q not in runes and 0 <= q[0] < 16 and 0 <= q[1] < 16:
                put(img, q[0], q[1], p[5])
    for (x, y) in runes:
        put(img, x, y, GLOW[0])
    return img


def glowing_tidestone_runes(frames=8):
    """Emissive overlay (animated, interpolated): the runes pulse between deep and bright cyan."""
    out = []
    runes = _rune_pixels()
    for f in range(frames):
        t = 0.5 + 0.5 * math.sin(f / frames * 2 * math.pi)
        img = new()
        for (x, y) in runes:
            # upper strokes slightly brighter, like light running through the glyph
            base = GLOW[1] if y % 4 else GLOW[2]
            c = lerp(base, GLOW[4] if (x + y) % 3 == 0 else GLOW[3], t)
            put(img, x, y, c)
        out.append(img)
    return out


def weathered_door_bottom():
    """Lower half of a dock-shack door: vertical boards, a Z brace and a rope pull."""
    p = WEATHERED
    img = new()
    for x in range(16):
        board = x // 4
        t = [4, 3, 4, 3][board]
        for y in range(16):
            put(img, x, y, p[t])
        if x % 4 == 3:
            for y in range(16):
                put(img, x, y, p[1])
        if x % 4 == 0:
            for y in range(16):
                if (y * 7 + x) % 5:
                    put(img, x, y, p[t + 1])
    rnd = random.Random(501)
    for _ in range(10):  # grain
        x, y = rnd.randrange(16), rnd.randrange(16)
        if x % 4 not in (0, 3):
            for k in range(rnd.randrange(2, 5)):
                put_in(img, x, y + k, p[2])
    for x in range(16):  # rails
        for y in (2, 3, 12, 13):
            put(img, x, y, p[5] if y in (2, 12) else p[1])
    for k in range(9):  # diagonal brace
        x, y = 1 + k * 13 // 8, 11 - k
        put(img, x, y, p[6])
        put(img, x + 1, y, p[5])
        put(img, x, y + 1, p[1])
        put(img, x + 1, y + 1, p[2])
    for (x, y) in [(2, 2), (13, 2), (2, 12), (13, 12)]:
        put(img, x, y, p[0])
    put(img, 13, 6, TW_ROPE[2])
    put(img, 13, 7, TW_ROPE[1])
    put(img, 12, 8, TW_ROPE[1])
    return img


TW_ROPE = pal('#6e5634', '#9c7f4e', '#c7a86f')


def weathered_door_top():
    p = WEATHERED
    img = weathered_door_bottom()
    for x in range(16):  # replace the brace area with a small open window
        for y in range(4, 12):
            pass
    fill(img, 4, 5, 11, 9, (0, 0, 0, 0))
    bevel(img, 3, 4, 12, 10, p[2], p[5])
    for y in range(5, 10):
        put(img, 7, y, p[3])
    for x in range(16):
        for y in (12, 13):
            put(img, x, y, p[5] if y == 12 else p[2])
        for y in (14, 15):
            put(img, x, y, img.getpixel((x, 0)))
    return img


def weathered_trapdoor():
    """Dock hatch: planks with two battens and a rope handle."""
    p = WEATHERED
    img = new()
    for y in range(16):
        b = y // 4
        t = [4, 3, 4, 3][b]
        for x in range(16):
            put(img, x, y, p[t] if y % 4 != 3 else p[t - 2])
        if y % 4 == 0:
            for x in range(16):
                if (x * 5 + y) % 7:
                    put(img, x, y, p[t + 1])
    for y in range(16):  # battens
        for x in (2, 3, 12, 13):
            put(img, x, y, p[5] if x in (2, 12) else p[2])
    bevel(img, 0, 0, 15, 15, p[5], p[1])
    for (x, y) in [(7, 7), (8, 7), (6, 8), (9, 8)]:
        put(img, x, y, TW_ROPE[2] if y == 7 else TW_ROPE[1])
    for (x, y) in [(2, 1), (13, 1), (2, 14), (13, 14)]:
        put(img, x, y, p[0])
    return img
