"""Textures, block models, blockstates and item definitions of the story module.

Palette: Tidewarden relics = teal tidestone, gold inlays, glowing cyan runes (same ramp as the building module's
tidestone family); Meridian Expedition = cream painted steel, brass, red/white stripes, green-teal CRT glow.
"""
import math
import random

from PIL import Image, ImageDraw

from bclib import assets as A
from bclib import tex

M = 'story'
NS = 'raft'
rid = A.rid

TIDE = ['#1b3a3c', '#22494b', '#2f5f60', '#3e7575', '#4f8b88', '#64a19a', '#7fb9ae']
GOLD = ['#7a5418', '#a87a24', '#c2932e', '#e7bd45', '#fbe08a']
GLOW = ['#1b7f8f', '#2fb5b8', '#7ff0e0', '#c8fff6']
LEATHER = ['#3a2014', '#4f2c1a', '#6b3d22', '#8a5530', '#a86a3c']
PAPER = ['#a8946a', '#cdbf98', '#e2d5b0', '#efe4c4', '#f8f0da']
STEEL_CREAM = ['#6e6a5c', '#8f8a76', '#b4ad94', '#d4ccb0', '#ebe4c8']
WOOD = ['#3e2614', '#55361d', '#704a28', '#8c6034', '#a87a44']
IRON = ['#2c2e33', '#40434a', '#585c64', '#767b84', '#9aa0a8']
RED = ['#5e1418', '#8a1f22', '#b5302e', '#d64a40']
SIGIL = {
    'coral': {'disc': ['#173c42', '#1f5258', '#2a6a6e', '#3c8484'], 'em': ['#7a1e3a', '#c23a5e', '#f07aa0', '#ffc2d4']},
    'pearl': {'disc': ['#141e3a', '#1d2b52', '#283b6a', '#384f86'], 'em': ['#8a8fa8', '#c6cbe0', '#eef0fa', '#ffffff']},
    'storm': {'disc': ['#1c1440', '#2a1e5c', '#3a2c7a', '#4e3e98'], 'em': ['#a07a10', '#e8b830', '#ffe46a', '#fffbd0']},
    'ember': {'disc': ['#2a1410', '#3e1c14', '#55261a', '#6e3222'], 'em': ['#a8300c', '#e8601a', '#ffa030', '#ffe08a']},
}


def C(c):
    return tex.rgb(c)


def put(img, x, y, c):
    tex.px(img, x, y, c)


def save(img, rel):
    return tex.save(img, M, rel)


def anim(frames, rel, frametime=3, interpolate=False):
    return tex.save_animated(frames, M, rel, frametime, interpolate)


def outlined(img, colour=None):
    return tex.outline(img, colour)


# ================================================================================================= items
def journal():
    img = tex.new()
    L = LEATHER
    tex.rect(img, 2, 1, 12, 13, L[2])
    for y in range(1, 14):                      # spine
        put(img, 2, y, L[1]); put(img, 3, y, L[1] if y % 3 else L[3])
    for x in range(4, 13):                      # lit top / shaded bottom edge
        put(img, x, 1, L[3]); put(img, x, 13, L[1])
    for y in range(2, 13):
        put(img, 4, y, L[3] if y < 7 else L[2]); put(img, 12, y, L[1])
    for y in range(2, 15):                      # page block (thickness) right and below
        put(img, 13, y, PAPER[2] if y % 2 else PAPER[1])
    for x in range(3, 14):
        put(img, x, 14, PAPER[1] if x % 2 else PAPER[2])
    put(img, 13, 14, PAPER[0])
    for (x, y, c) in ((11, 1, 4), (12, 1, 3), (12, 2, 2), (11, 13, 2), (12, 13, 1), (12, 12, 2)):   # brass corners
        put(img, x, y, GOLD[c])
    # compass rose emblem
    cx, cy = 8, 7
    for (dx, dy) in ((-2, -1), (-2, 0), (-2, 1), (2, -1), (2, 0), (2, 1), (-1, -2), (0, -2), (1, -2), (-1, 2), (0, 2),
                     (1, 2)):
        put(img, cx + dx, cy + dy, GOLD[2])
    put(img, cx, cy - 3, GOLD[4]); put(img, cx, cy + 3, GOLD[1]); put(img, cx - 3, cy, GOLD[3]); put(img, cx + 3, cy, GOLD[1])
    put(img, cx, cy, GLOW[2]); put(img, cx, cy - 1, GOLD[4]); put(img, cx, cy + 1, GOLD[1])
    put(img, cx - 1, cy, GOLD[3]); put(img, cx + 1, cy, GOLD[1])
    put(img, cx - 2, cy - 1, GOLD[3]); put(img, cx - 1, cy - 2, GOLD[3])
    # teal ribbon bookmark hanging out at the bottom
    put(img, 6, 14, '#3f9c9a'); put(img, 6, 15, '#2a6e6e'); put(img, 7, 15, '#2a6e6e')
    return outlined(img, '#24140c')


def bottle():
    img = tex.new()
    ax, ay = 3.0, 12.6
    d = (1 / math.sqrt(2), -1 / math.sqrt(2))
    n = (d[1] * -1, d[0])   # perpendicular, pointing up-left
    glass = ['#2f6f68', '#4c9c90', '#7fd6c6', '#c8f4ea']
    for y in range(16):
        for x in range(16):
            px, py = x + 0.5 - ax, y + 0.5 - ay
            u = px * d[0] + py * d[1]
            v = px * n[0] + py * n[1]
            if u < -0.6:
                continue
            if u <= 9.0:
                r = 2.75 if u > 0.6 else 2.75 - (0.6 - u) * 0.9
            elif u <= 10.6:
                r = 2.75 - (u - 9.0) * 0.95
            elif u <= 12.4:
                r = 1.15
            elif u <= 14.2:
                r = 1.35
            else:
                continue
            if abs(v) > r:
                continue
            if u > 12.4:                                  # cork
                c = WOOD[3] if v > 0.2 else WOOD[2]
                if u > 13.6:
                    c = WOOD[4] if v > 0 else WOOD[3]
                put(img, x, y, c)
                continue
            edge = abs(v) > r - 0.95
            scroll = 1.0 < u < 8.6 and abs(v) < 1.35 and not edge
            if scroll:
                c = PAPER[3] if v > 0.3 else (PAPER[2] if v > -0.5 else PAPER[1])
                if 4.4 < u < 5.4:
                    c = RED[2] if v > 0 else RED[1]
                put(img, x, y, c)
            elif edge:
                c = glass[3] if (v > 0 and 1.5 < u < 8) else (glass[1] if v < 0 else glass[2])
                put(img, x, y, c)
            else:
                g = C(glass[2])
                put(img, x, y, (g[0], g[1], g[2], 110))
    return outlined(img, '#1d4a44')


def barrel():
    img = tex.new()
    widths = {2: 9, 3: 10, 4: 11, 5: 11, 6: 12, 7: 12, 8: 12, 9: 12, 10: 11, 11: 11, 12: 10, 13: 9}
    for y, w in widths.items():
        x0 = 8 - w // 2 - (w % 2)
        for i in range(w):
            x = x0 + i
            stave = (i * 4) // w
            shade = [3, 2, 3, 1][stave] if i not in (0, w - 1) else 1
            if i == 1:
                shade = 4 if y < 8 else 3
            c = WOOD[shade]
            if (i * 4) % w == 0 and 0 < i < w - 1:
                c = WOOD[0]                              # gap between staves
            put(img, x, y, c)
    for y in (4, 11):                                   # iron hoops
        w = widths[y]
        x0 = 8 - w // 2 - (w % 2)
        for i in range(w):
            put(img, x0 + i, y, IRON[3] if i < w // 2 else IRON[2])
            put(img, x0 + i, y + 1, IRON[1])
    for x in range(5, 12):                              # lid
        put(img, x, 1, WOOD[4] if x < 9 else WOOD[3])
    for x in range(4, 13):
        put(img, x, 2, WOOD[3])
    # rope loop and knot around the middle
    for x in range(3, 14):
        put(img, x, 8, '#c9a86a' if x % 2 else '#9a7a44')
    put(img, 12, 9, '#c9a86a'); put(img, 13, 10, '#9a7a44'); put(img, 12, 10, '#c9a86a')
    # a plastic tag nailed on, like in the debris of the old mod
    put(img, 6, 6, '#3f7fd0'); put(img, 7, 6, '#5a9ae8'); put(img, 6, 7, '#2f5fa8'); put(img, 7, 7, '#3f7fd0')
    put(img, 6, 14, WOOD[1]); put(img, 9, 14, WOOD[1])
    return outlined(img, '#2a180c')


def _disc(img, cx, cy, r, shades):
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            dist = math.hypot(dx, dy)
            if dist <= r:
                light = -(dx + dy) / (2 * r)                # top-left is lit
                k = int((light + 0.5) * len(shades))
                put(img, x, y, shades[max(0, min(len(shades) - 1, k))])


def sigil_frames(kind, frames=8):
    pal = SIGIL[kind]
    out = []
    for f in range(frames):
        img = tex.new()
        cx = cy = 8.0
        for y in range(16):                              # gold ring
            for x in range(16):
                dx, dy = x + 0.5 - cx, y + 0.5 - cy
                dist = math.hypot(dx, dy)
                if 4.7 < dist <= 6.6:
                    lit = -(dx + dy) / (2 * 6.6)
                    k = 2 + (1 if lit > 0.12 else 0) + (1 if lit > 0.35 else 0) - (1 if lit < -0.3 else 0)
                    put(img, x, y, GOLD[k])
        _disc(img, cx, cy, 4.7, pal['disc'])
        # rune notches on the ring (N, E, S, W) glow in turn
        notches = [(7, 1), (14, 7), (8, 14), (1, 8)]
        for i, (x, y) in enumerate(notches):
            put(img, x, y, GLOW[3] if (f // 2) % 4 == i else GLOW[1])
        pulse = [0, 1, 2, 3, 3, 2, 1, 0][f % 8]
        em = pal['em']
        hi = em[min(3, 1 + pulse // 2 + 1)]
        mid = em[min(3, 1 + pulse // 2)]
        lo = em[0 if pulse < 2 else 1]
        if kind == 'coral':
            pts = [(8, 11, lo), (8, 10, mid), (8, 9, mid), (7, 8, mid), (6, 7, hi), (6, 6, hi), (9, 8, mid),
                   (10, 7, mid), (10, 6, hi), (7, 9, lo), (9, 10, lo), (5, 6, hi), (11, 6, mid), (8, 7, mid),
                   (8, 6, hi), (7, 11, lo), (9, 11, lo)]
        elif kind == 'pearl':
            pts = []
            for y in range(5, 11):
                for x in range(5, 11):
                    dd = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
                    if dd <= 2.6:
                        lit = -(x + 0.5 - 8 + y + 0.5 - 8) / 5.2
                        pts.append((x, y, hi if lit > 0.15 else (mid if lit > -0.2 else lo)))
            pts += [(6, 6, em[3]), (7, 6, em[3])]
        elif kind == 'storm':
            pts = [(9, 4, hi), (8, 5, hi), (8, 6, hi), (7, 7, mid), (6, 8, mid), (7, 8, hi), (8, 8, hi),
                   (9, 8, mid), (8, 9, mid), (8, 10, mid), (7, 11, lo), (7, 12, lo), (9, 5, mid), (7, 6, lo)]
        else:  # ember flame
            pts = [(8, 4, hi), (8, 5, hi), (7, 6, hi), (8, 6, em[3]), (9, 6, mid), (7, 7, mid), (8, 7, em[3]),
                   (9, 7, hi), (6, 8, lo), (7, 8, hi), (8, 8, em[3]), (9, 8, mid), (10, 8, lo), (6, 9, lo),
                   (7, 9, mid), (8, 9, hi), (9, 9, mid), (10, 9, lo), (7, 10, lo), (8, 10, mid), (9, 10, lo),
                   (10, 7, lo)]
        for (x, y, c) in pts:
            put(img, x, y, c)
        # travelling sparkle on the ring
        ang = f / frames * 2 * math.pi - math.pi * 0.75
        sx, sy = int(round(cx - 0.5 + 5.6 * math.cos(ang))), int(round(cy - 0.5 + 5.6 * math.sin(ang)))
        put(img, sx, sy, '#fffbe8')
        out.append(outlined(img, '#3a2a10'))
    return out


def key_frames(frames=8):
    out = []
    gems = [('#f07aa0', '#ffc2d4'), ('#eef0fa', '#ffffff'), ('#4e7ae8', '#a8c4ff'), ('#ff8a2a', '#ffe08a')]
    for f in range(frames):
        img = tex.new()
        cx, cy = 5.0, 5.0
        for y in range(11):
            for x in range(11):
                dx, dy = x + 0.5 - cx, y + 0.5 - cy
                dist = math.hypot(dx, dy)
                if 2.0 < dist <= 4.4:
                    lit = -(dx + dy) / 8.8
                    put(img, x, y, GOLD[3] if lit > 0.15 else (GOLD[2] if lit > -0.25 else GOLD[1]))
                elif dist <= 2.0:
                    put(img, x, y, TIDE[4] if dx + dy < 0 else TIDE[3])
        put(img, 4, 4, GLOW[2 if f % 4 < 2 else 3])
        # gems on the ring: N, E, S, W
        for i, (x, y) in enumerate([(4, 1), (8, 4), (5, 8), (1, 5)]):
            put(img, x, y, gems[i][1] if (f // 2) % 4 == i else gems[i][0])
        # shaft (2 px, diagonal)
        for t in range(7):
            put(img, 8 + t, 8 + t, GOLD[3]); put(img, 7 + t, 8 + t, GOLD[2]); put(img, 8 + t, 7 + t, GOLD[4] if t < 3 else GOLD[3])
        # bit: two teeth
        for (x, y, c) in ((11, 14, 1), (10, 14, 2), (10, 13, 2), (13, 12, 1), (14, 11, 2), (13, 11, 2),
                          (12, 15, 1), (15, 12, 1)):
            put(img, x, y, GOLD[c])
        put(img, 9, 9, GOLD[4]); put(img, 10, 10, GOLD[4])
        out.append(outlined(img, '#3a2a10'))
    return out


# ================================================================================================= blocks
def seal_frames(frames=12):
    out = []
    for f in range(frames):
        img = tex.new()
        ph = f / frames * 2 * math.pi
        for y in range(16):
            for x in range(16):
                w = math.sin((x + y) / 16 * 2 * math.pi * 2 + ph) * 0.5 + math.sin((x - y) / 16 * 2 * math.pi + ph * 2) * 0.3
                k = 1 if w < -0.25 else (2 if w < 0.25 else 3)
                c = C(['#0f3a44', '#1f6e78', '#2fa0a4', '#6fe0d8'][k])
                put(img, x, y, (c[0], c[1], c[2], 150 + k * 18))
        # diamond lattice of gold lines (tiles seamlessly across blocks)
        for i in range(16):
            for (x, y) in ((i, i), (15 - i, i)):
                put(img, x, y, GOLD[3] if (i + f) % 6 else GOLD[4])
        for (x, y) in ((0, 0), (15, 0), (0, 15), (15, 15), (7, 7), (8, 8), (7, 8), (8, 7)):
            put(img, x, y, GOLD[4])
        # small runes in the four diamond cells, glowing in turn
        runes = [((7, 2), [(0, 0), (1, 0), (0, 1), (1, 2), (0, 2)]), ((12, 7), [(0, 0), (0, 1), (1, 1), (0, 2)]),
                 ((7, 12), [(0, 0), (1, 0), (1, 1), (0, 2), (1, 2)]), ((2, 7), [(1, 0), (0, 1), (1, 1), (1, 2)])]
        for i, ((bx, by), cells) in enumerate(runes):
            col = GLOW[3] if (f // 3) % 4 == i else GLOW[2]
            for (dx, dy) in cells:
                put(img, bx + dx, by + dy, col)
        out.append(img)
    return out


def sanctum_tidestone():
    base = tex.load('raft:block/tidestone_bricks')
    if base is None:
        base = tex.bricks([C(c) for c in TIDE[1:6]], C(TIDE[0]), seed=7)
    img = base.copy()
    for y in range(16):
        for x in range(16):
            r, g, b, a = img.getpixel((x, y))
            img.putpixel((x, y), (int(r * 0.74), int(g * 0.8), int(b * 0.86), a))
    # faint rune hairlines in the mortar, a hint of the seal's power
    for (x, y) in ((3, 3), (11, 7), (5, 11), (13, 15)):
        put(img, x, y, '#2f8f94')
    return img


def sanctum_glass():
    img = tex.new()
    for y in range(16):
        for x in range(16):
            c = C('#123c44')
            put(img, x, y, (c[0], c[1], c[2], 95))
    for i in range(16):
        for (x, y) in ((i, 0), (0, i)):
            put(img, x, y, TIDE[3])
        for (x, y) in ((i, 15), (15, i)):
            put(img, x, y, TIDE[1])
    for (x, y) in ((0, 0), (15, 0), (0, 15), (15, 15)):
        put(img, x, y, GOLD[2])
    for (x0, y0, ln) in ((3, 5, 3), (4, 4, 1), (10, 10, 3), (11, 9, 1)):   # streak highlights like vanilla glass
        for k in range(ln):
            c = C('#7fd6d0')
            put(img, x0 + k, y0 - k, (c[0], c[1], c[2], 170))
    return img


def stone_panel(seed, top_trim=(2, 4), body=(5, 12), plinth=(13, 15), rune=None, front=False):
    """Side texture for a carved altar whose model uses auto-UV: rows top_trim = slab, body = carved panel,
    plinth = foot. Tidestone with gold inlay lines and an optional glowing rune."""
    img = tex.noise_fill(tex.new(), [C(c) for c in TIDE[2:6]], seed, cell=4, octaves=2, contrast=0.7)
    for y in range(top_trim[0], top_trim[1] + 1):
        for x in range(16):
            put(img, x, y, TIDE[5] if y == top_trim[0] else TIDE[4])
    for x in range(16):
        put(img, x, top_trim[1], GOLD[3] if x % 4 else GOLD[2])
        put(img, x, top_trim[1] + 1, TIDE[1])
    for y in range(plinth[0], plinth[1] + 1):
        for x in range(16):
            put(img, x, y, TIDE[3] if y == plinth[0] else TIDE[2])
    for x in range(16):
        put(img, x, plinth[0] - 1, TIDE[1])
        put(img, x, plinth[0], GOLD[2] if x % 3 == 1 else TIDE[4])
    # inset panel frame
    for y in range(body[0] + 1, body[1]):
        put(img, 4, y, TIDE[1]); put(img, 11, y, TIDE[5])
    for x in range(4, 12):
        put(img, x, body[0] + 1, TIDE[1]); put(img, x, body[1] - 1, TIDE[5])
    if rune:
        for (x, y) in rune:
            put(img, x, y, GLOW[2])
    if front:
        for (x, y) in ((7, 7), (8, 7), (6, 8), (9, 8), (7, 9), (8, 9), (7, 10), (8, 10)):
            put(img, x, y, GOLD[3])
        put(img, 7, 8, GLOW[3]); put(img, 8, 8, GLOW[3])
    return img


def altar_top():
    img = tex.noise_fill(tex.new(), [C(c) for c in TIDE[3:6]], 41, cell=4, octaves=2, contrast=0.6)
    tex.border(img, C(TIDE[5]), C(TIDE[2]))
    for i in range(1, 15):
        for (x, y) in ((i, 1), (1, i), (i, 14), (14, i)):
            put(img, x, y, GOLD[2])
    # four sockets
    for (sx, sy) in ((3, 3), (10, 3), (10, 10), (3, 10)):
        tex.rect(img, sx, sy, sx + 2, sy + 2, TIDE[0])
        put(img, sx, sy, TIDE[1])
    # centre circle glyph
    for (x, y) in ((7, 5), (8, 5), (6, 6), (9, 6), (5, 7), (10, 7), (5, 8), (10, 8), (6, 9), (9, 9), (7, 10), (8, 10)):
        put(img, x, y, GOLD[3])
    for (x, y) in ((7, 7), (8, 8), (7, 8), (8, 7)):
        put(img, x, y, GLOW[2])
    return img


def gems():
    """Four 4-px wide gem strips (coral, pearl, storm, ember) for the altar's sigil gems: saturated body, darker
    right/bottom edge, one bright glint per face."""
    img = tex.new()
    cols = [('#7a1e3a', '#c23a5e', '#f07aa0', '#ffd0de'), ('#7a8098', '#b4bad4', '#e4e8f8', '#ffffff'),
            ('#1e2c7a', '#3456c8', '#5a84f0', '#c8d8ff'), ('#8a2a0c', '#d8501a', '#ff9030', '#fff0b0')]
    for i, c in enumerate(cols):
        x0 = i * 4
        for y in range(16):
            for x in range(4):
                k = 2
                if x == 3 or y % 4 == 3:
                    k = 1
                put(img, x0 + x, y, c[k])
        for gy in (0, 4, 8, 12):
            put(img, x0, gy, c[3])
    return img

def lev_stone(seed):
    return tex.noise_fill(tex.new(), [C(c) for c in ('#0f2428', '#16313a', '#1d3f48', '#264e58')], seed, cell=4,
                          octaves=2, contrast=0.7)


def lev_side(active=False):
    img = lev_stone(51)
    for x in range(16):                       # cap and plinth trims
        put(img, x, 2, '#3a6a72'); put(img, x, 3, '#2f5a62'); put(img, x, 4, GOLD[1] if x % 2 else GOLD[2])
        put(img, x, 13, '#2f5a62'); put(img, x, 14, '#1d3f48')
    # scale relief on the body
    for y in range(5, 12):
        for x in range(2, 14):
            if (x + (y % 2) * 2) % 4 == 0:
                put(img, x, y, '#2f5a62' if not active else '#2fb5b8')
                put(img, x + 1, y, '#264e58')
    return img


def lev_front(active=False):
    """The carved face of the Leviathan altar: a serpent coiled in a ring (body rows 5..12 of the model)."""
    img = lev_side(active)
    tex.rect(img, 3, 5, 12, 12, '#16313a')                 # recessed panel
    for x in range(3, 13):
        put(img, x, 5, '#0f2428'); put(img, x, 12, '#2f5a62')
    for y in range(5, 13):
        put(img, 3, y, '#0f2428'); put(img, 12, y, '#2f5a62')
    ring = GLOW[2] if active else '#4a8a90'
    ring_dark = GLOW[1] if active else '#2f5a62'
    # coiled body: a ring of scales, thicker on the lit (top-left) side
    for (x, y) in ((6, 6), (7, 6), (8, 6), (9, 6), (5, 7), (10, 7), (4, 8), (11, 8), (4, 9), (11, 9), (5, 10), (10, 10),
                   (6, 11), (7, 11), (8, 11)):
        put(img, x, y, ring)
    for (x, y) in ((6, 7), (5, 8), (5, 9), (6, 10), (7, 10)):
        put(img, x, y, ring_dark)
    # head in the middle of the coil, tail tip curling out at the bottom right
    for (x, y) in ((8, 8), (9, 8), (7, 9), (8, 9), (9, 9), (10, 9)):
        put(img, x, y, '#5a9aa0' if not active else '#9af7e8')
    put(img, 9, 11, ring_dark); put(img, 10, 11, ring)
    put(img, 9, 8, GLOW[3] if active else GLOW[0])         # eye
    put(img, 10, 10, '#d8e0d0'); put(img, 7, 9, '#d8e0d0')  # fangs
    return img


def lev_top_frames(active):
    frames = []
    n = 8 if active else 1
    for f in range(n):
        img = lev_stone(57)
        tex.border(img, C('#3a6a72'), C('#0f2428'))
        for a in range(24):                   # rune circle
            t = a / 24 * 2 * math.pi
            x, y = int(round(7.5 + 5.2 * math.cos(t))), int(round(7.5 + 5.2 * math.sin(t)))
            on = active and (a + f * 3) % 24 < 8
            put(img, x, y, GLOW[3] if on else (GLOW[1] if active else GOLD[1]))
        tex.rect(img, 7, 5, 8, 10, '#081418')  # key slot
        put(img, 6, 6, '#081418'); put(img, 9, 6, '#081418')
        if active:
            put(img, 7, 7, GLOW[2]); put(img, 8, 8, GLOW[2])
        frames.append(img)
    return frames


def terminal_case():
    img = tex.noise_fill(tex.new(), [C(c) for c in STEEL_CREAM[2:5]], 61, cell=8, octaves=2, contrast=0.5)
    tex.border(img, C(STEEL_CREAM[4]), C(STEEL_CREAM[1]))
    for (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)):
        put(img, x, y, STEEL_CREAM[4]); put(img, x + 1, y + 1, STEEL_CREAM[0])
    for y in (5, 7, 9):                        # vents
        for x in range(4, 12):
            put(img, x, y, STEEL_CREAM[0] if x % 2 == 0 else STEEL_CREAM[1])
    return img


def terminal_front():
    """Front of the cabinet; the model shows rows 1..13, the glowing screen element covers x 3..12, rows 3..11."""
    img = tex.noise_fill(tex.new(), [C(c) for c in STEEL_CREAM[2:5]], 62, cell=8, octaves=2, contrast=0.5)
    tex.border(img, C(STEEL_CREAM[4]), C(STEEL_CREAM[1]))
    for x in range(1, 15):                                   # red/white Expedition stripe on top
        put(img, x, 1, RED[2] if (x // 2) % 2 == 0 else '#efe8d8')
    tex.rect(img, 2, 2, 13, 12, STEEL_CREAM[1], fill=False)   # bezel
    tex.rect(img, 3, 3, 12, 11, '#1a2220')
    for x in range(3, 13):
        put(img, x, 12, STEEL_CREAM[4])
    for (x, y) in ((2, 13), (12, 13)):                       # knobs
        put(img, x, y, IRON[1]); put(img, x + 1, y, IRON[3])
    put(img, 7, 13, '#58e070'); put(img, 8, 13, '#2f8a40')  # power lamp
    return img


def terminal_screen_frames(n=8):
    rnd = random.Random(5)
    lines = [[rnd.random() < 0.75 for _ in range(16)] for _ in range(32)]
    lengths = [rnd.randrange(4, 13) for _ in range(32)]
    out = []
    for f in range(n):
        img = tex.new(fill='#0c2622')
        for y in range(16):
            if y % 2 == 0:
                for x in range(16):
                    put(img, x, y, '#0f2e29')
        for row in range(5):
            idx = (row + f) % 32
            y = 2 + row * 3
            for x in range(2, 2 + lengths[idx]):
                if lines[idx][x]:
                    put(img, x, y, '#7ff0b8' if row < 4 else '#c8ffe0')
        if f % 2 == 0:                                       # blinking cursor
            put(img, 2 + lengths[(4 + f) % 32] + 1, 14, '#c8ffe0')
        for x in range(16):                                  # glass reflection
            pass
        put(img, 1, 1, '#2f5a50'); put(img, 2, 1, '#2f5a50'); put(img, 1, 2, '#2f5a50')
        out.append(img)
    return out


def terminal_keys():
    img = tex.new(fill=STEEL_CREAM[2])
    for y in range(16):
        for x in range(16):
            if y % 3 != 2 and x % 2 == 0:
                put(img, x, y, '#3a3630' if (x + y) % 5 else '#8a2a24')
            elif y % 3 != 2:
                put(img, x, y, '#2a2620')
    return img


def radio_front():
    img = tex.new(fill=WOOD[2])
    tex.border(img, C(WOOD[4]), C(WOOD[0]))
    # fabric speaker grille (left) and dial window (right); the model shows rows 7..15
    for y in range(8, 15):
        for x in range(2, 8):
            put(img, x, y, '#c8b48a' if (x + y) % 2 else '#a8946a')
    tex.rect(img, 1, 7, 8, 15, WOOD[1], fill=False)
    tex.rect(img, 9, 8, 14, 11, '#e8d8a8')
    for x in range(10, 14):
        put(img, x, 9, '#5a4a30' if x % 2 else '#e8d8a8')
    put(img, 12, 10, RED[2]); put(img, 12, 8, RED[1])
    tex.rect(img, 9, 8, 14, 11, GOLD[1], fill=False)
    for (x, y) in ((10, 13), (13, 13)):
        put(img, x, y, IRON[0]); put(img, x + 1, y, IRON[2]); put(img, x, y + 1, IRON[1]); put(img, x + 1, y + 1, IRON[3])
    return img


def radio_side():
    img = tex.planks([C(c) for c in WOOD], seed=71, boards=4)
    for (x, y) in ((0, 7), (15, 7), (0, 15), (15, 15)):
        put(img, x, y, GOLD[2])
    return img


def radio_top():
    img = tex.planks([C(c) for c in WOOD], seed=72, boards=4, vertical=True)
    tex.border(img, C(WOOD[4]), C(WOOD[0]))
    return img


def antenna():
    img = tex.new(fill=IRON[3])
    for y in range(16):
        put(img, 0, y, IRON[4]); put(img, 1, y, IRON[2])
    put(img, 0, 0, '#d8dce0')
    return img


def pedestal_side():
    img = tex.noise_fill(tex.new(), [C(c) for c in TIDE[2:6]], 81, cell=4, octaves=2, contrast=0.6)
    for x in range(16):
        put(img, x, 1, TIDE[5]); put(img, x, 2, TIDE[4]); put(img, x, 3, GOLD[2] if x % 4 == 1 else TIDE[3])
        put(img, x, 12, TIDE[5]); put(img, x, 13, TIDE[3]); put(img, x, 15, TIDE[1])
    for y in range(4, 12):                    # fluting on the column
        for x in (5, 7, 9, 11):
            put(img, x, y, TIDE[2]); put(img, x - 1, y, TIDE[5] if y < 8 else TIDE[4])
    return img


def pedestal_top():
    img = tex.noise_fill(tex.new(), [C(c) for c in TIDE[3:6]], 82, cell=4, octaves=2, contrast=0.5)
    tex.border(img, C(TIDE[5]), C(TIDE[2]))
    for a in range(20):
        t = a / 20 * 2 * math.pi
        put(img, int(round(7.5 + 4.2 * math.cos(t))), int(round(7.5 + 4.2 * math.sin(t))), GOLD[2 if a % 5 else 3])
    put(img, 7, 7, GLOW[1]); put(img, 8, 8, GLOW[1])
    return img


# ================================================================================================= gui
def gui_journal():
    W, H = 320, 200
    img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle((0, 0, W - 1, H - 1), 8, fill=C(LEATHER[2]))
    d.rounded_rectangle((0, 0, W - 1, H - 1), 8, outline=C(LEATHER[0]))
    d.rounded_rectangle((3, 3, W - 4, H - 4), 6, outline=C(LEATHER[3]))
    for x in range(8, W - 8, 4):              # stitching
        img.putpixel((x, 5), C(PAPER[1])); img.putpixel((x, H - 6), C(PAPER[1]))
    for y in range(8, H - 8, 4):
        img.putpixel((5, y), C(PAPER[1])); img.putpixel((W - 6, y), C(PAPER[1]))
    for (x0, y0, x1, y1) in ((10, 8, 157, 190), (163, 8, 310, 190)):      # stacked page edges
        for k in (3, 2, 1):
            d.rectangle((x0 - k + (0 if x0 < 160 else k), y0 + k, x1 + (0 if x0 < 160 else k) - (k if x0 < 160 else 0), y1 + k),
                        fill=C(PAPER[0 if k == 3 else 1]))
    n = tex.value_noise(W, H, 11, cell=16, octaves=3, tile=False)
    for (x0, y0, x1, y1) in ((12, 8, 158, 188), (162, 8, 308, 188)):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                v = n[y][x]
                gut = abs(x - 160) / 40.0
                edge = min(x - x0, x1 - x, y - y0, y1 - y)
                c = PAPER[3] if v > 0.42 else PAPER[2]
                if v > 0.62:
                    c = PAPER[4]
                if gut < 0.12 or edge < 1:
                    c = PAPER[1]
                elif gut < 0.3:
                    c = PAPER[2]
                img.putpixel((x, y), C(c))
    # faint compass rose watermark on the right page and a coffee ring on the left page
    cx, cy = 286, 166
    for a in range(16):
        t = a / 16 * 2 * math.pi
        r = 14 if a % 4 == 0 else 7
        d.line((cx, cy, cx + r * math.cos(t), cy + r * math.sin(t)), fill=C(PAPER[1]))
    d.ellipse((cx - 9, cy - 9, cx + 9, cy + 9), outline=C(PAPER[1]))
    d.ellipse((24, 160, 50, 186), outline=C('#d8c49a'))
    d.ellipse((25, 161, 49, 185), outline=C('#e0cfa6'))
    # brass corners and teal ribbon
    for (x, y) in ((0, 0), (W - 14, 0), (0, H - 14), (W - 14, H - 14)):
        d.polygon([(x, y), (x + 13, y), (x, y + 13)] if (x == 0 and y == 0) else
                  [(x + 13, y), (x + 13, y + 13), (x, y)] if y == 0 else
                  [(x, y), (x, y + 13), (x + 13, y + 13)] if x == 0 else
                  [(x + 13, y), (x + 13, y + 13), (x, y + 13)], fill=C(GOLD[2]))
    d.rectangle((158, 186, 162, 199), fill=C('#3f9c9a'))
    d.line((158, 186, 158, 199), fill=C('#2a6e6e'))
    return img


def gui_terminal():
    W, H = 300, 200
    img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle((0, 0, W - 1, H - 1), 10, fill=C(STEEL_CREAM[3]))
    d.rounded_rectangle((0, 0, W - 1, H - 1), 10, outline=C(STEEL_CREAM[0]))
    d.rounded_rectangle((2, 2, W - 3, H - 3), 9, outline=C(STEEL_CREAM[4]))
    n = tex.value_noise(W, H, 13, cell=32, octaves=2, tile=False)
    for y in range(4, H - 4):
        for x in range(4, W - 4):
            r, g, b, a = img.getpixel((x, y))
            if a and n[y][x] > 0.66:
                img.putpixel((x, y), C(STEEL_CREAM[2]))
    # screen
    d.rounded_rectangle((14, 14, W - 15, 164), 14, fill=C('#2a2620'))
    d.rounded_rectangle((18, 18, W - 19, 160), 12, fill=C('#0c2622'))
    for y in range(20, 159, 2):
        d.line((22, y, W - 23, y), fill=C('#0f2e29'))
    d.line((30, 24, 80, 24), fill=C('#24504a'))
    d.line((26, 28, 26, 60), fill=C('#24504a'))
    # rivets, stripe, knobs, lamp
    for (x, y) in ((8, 8), (W - 9, 8), (8, H - 9), (W - 9, H - 9)):
        d.ellipse((x - 2, y - 2, x + 2, y + 2), fill=C(STEEL_CREAM[4]), outline=C(STEEL_CREAM[0]))
    for x in range(20, W - 20, 12):
        d.rectangle((x, 172, x + 5, 176), fill=C(RED[2]))
        d.rectangle((x + 6, 172, x + 11, 176), fill=C('#efe8d8'))
    for x in (40, W - 40):
        d.ellipse((x - 8, 178, x + 8, 194), fill=C(IRON[1]), outline=C(IRON[0]))
        d.line((x, 180, x, 186), fill=C(IRON[4]))
    d.ellipse((W // 2 - 4, 182, W // 2 + 4, 190), fill=C('#58e070'), outline=C('#2f8a40'))
    return img


def adv_backgrounds():
    img = tex.new()
    for y in range(16):
        for x in range(16):
            w = math.sin((x * 0.8 + y * 0.35)) + math.sin((y * 0.9 - x * 0.3) + 1.3)
            put(img, x, y, ['#0c2a3a', '#103446', '#163f52', '#1d4c60'][max(0, min(3, int((w + 2) / 4 * 4)))])
    for (x, y) in ((3, 4), (4, 4), (5, 5), (11, 10), (12, 10), (13, 11), (8, 14), (9, 14)):
        put(img, x, y, '#2a6a7a')
    save(img, 'gui/advancements/backgrounds/story')
    sand = tex.noise_fill(tex.new(), [C(c) for c in ('#c9b47c', '#d8c48c', '#e3d3a0', '#ece0b4')], 91, cell=4, octaves=2,
                          contrast=0.8)
    for (x, y, c) in ((4, 5, '#f0e8d8'), (5, 5, '#d8a48f'), (11, 12, '#f0e8d8'), (12, 11, '#e8c8b0')):
        put(sand, x, y, c)
    save(sand, 'gui/advancements/backgrounds/adventure')


# ================================================================================================= models
def face(t, uv=None, cull=None, rot=None):
    f = {}
    if uv is not None:
        f['uv'] = list(uv)
    f['texture'] = '#' + t
    if cull:
        f['cullface'] = cull
    if rot:
        f['rotation'] = rot
    return f


def box(frm, to, tex_of, light=None, cull_outer=True, uvs=None, skip=()):
    x0, y0, z0 = frm
    x1, y1, z1 = to
    auto = {'down': [x0, 16 - z1, x1, 16 - z0], 'up': [x0, z0, x1, z1],
            'north': [16 - x1, 16 - y1, 16 - x0, 16 - y0], 'south': [x0, 16 - y1, x1, 16 - y0],
            'west': [z0, 16 - y1, z1, 16 - y0], 'east': [16 - z1, 16 - y1, 16 - z0, 16 - y0]}
    edge = {'down': y0 == 0, 'up': y1 == 16, 'north': z0 == 0, 'south': z1 == 16, 'west': x0 == 0, 'east': x1 == 16}
    faces = {}
    for dname in ('down', 'up', 'north', 'south', 'west', 'east'):
        if dname in skip:
            continue
        t = tex_of[dname] if isinstance(tex_of, dict) else tex_of
        if t is None:
            continue
        faces[dname] = face(t, (uvs or {}).get(dname, auto[dname]), dname if (cull_outer and edge[dname]) else None)
    e = {'from': list(frm), 'to': list(to)}
    if light:
        e['light_emission'] = light
    e['faces'] = faces
    return e


def model(name, textures, elements, ao=None, parent='minecraft:block/block'):
    d = {'parent': parent}
    if ao is not None:
        d['ambientocclusion'] = ao
    d['textures'] = {k: (v if v.startswith('#') else rid(v)) for k, v in textures.items()}
    d['elements'] = elements
    return A.write_json(M, f'assets/{NS}/models/block/{name}.json', d)


def side_tex(front, side, top, bottom):
    return {'north': front, 'south': side, 'east': side, 'west': side, 'up': top, 'down': bottom}


def models():
    # --- tide seal (translucent animated cube) + simple cubes
    A.block_model(M, 'tide_seal', 'minecraft:block/cube_all', {'all': 'block/tide_seal'})
    A.blockstate(M, 'tide_seal', {'open=false': {'model': rid('block/tide_seal')},
                                  'open=true': {'model': rid('block/tide_seal_open')}})
    model('tide_seal_open', {'particle': 'block/tide_seal'}, [])
    A.block_item(M, 'tide_seal')
    A.simple_block(M, 'sanctum_tidestone')
    A.simple_block(M, 'sanctum_glass')

    # --- tidewarden altar: plinth, body, top slab; gems per sigil
    t = {'side': 'block/tidewarden_altar_side', 'front': 'block/tidewarden_altar_front',
         'top': 'block/tidewarden_altar_top', 'bottom': 'raft:block/sanctum_tidestone',
         'particle': 'block/tidewarden_altar_side'}
    st = side_tex('front', 'side', 'top', 'bottom')
    model('tidewarden_altar', t, [
        box((0, 0, 0), (16, 3, 16), st),
        box((2, 3, 2), (14, 11, 14), dict(st, up=None, down=None)),
        box((0, 11, 0), (16, 14, 16), dict(st, down='bottom')),
    ])
    sockets = {'coral': (3, 3), 'pearl': (10, 3), 'storm': (10, 10), 'ember': (3, 10)}
    for i, (name, (sx, sz)) in enumerate(sockets.items()):
        u0 = i * 4
        model(f'tidewarden_altar_{name}', {'gem': 'block/tidewarden_altar_gems', 'particle': 'block/tidewarden_altar_gems'}, [
            box((sx + 0.5, 13, sz + 0.5), (sx + 2.5, 15.5, sz + 2.5), 'gem', light=15, cull_outer=False,
                uvs={k: [u0, 4, u0 + 2, 6.5] for k in ('north', 'south', 'east', 'west')} | {
                    'up': [u0, 0, u0 + 2, 2], 'down': [u0, 0, u0 + 2, 2]})], ao=False)
    mp = []
    for facing, rot in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
        a = {'model': rid('block/tidewarden_altar')}
        if rot:
            a['y'] = rot
        mp.append({'when': {'facing': facing}, 'apply': a})
        for name in sockets:
            g = {'model': rid(f'block/tidewarden_altar_{name}')}
            if rot:
                g['y'] = rot
            mp.append({'when': {'facing': facing, name: 'true'}, 'apply': g})
    A.blockstate(M, 'tidewarden_altar', multipart=mp)
    A.block_item(M, 'tidewarden_altar')

    # --- leviathan altar
    for act in ('', '_active'):
        t = {'side': f'block/leviathan_altar_side{act}', 'front': f'block/leviathan_altar_front{act}',
             'top': f'block/leviathan_altar_top{act}', 'bottom': 'raft:block/sanctum_tidestone',
             'particle': 'block/leviathan_altar_side'}
        st = {'north': 'side', 'south': 'front', 'east': 'side', 'west': 'side', 'up': 'top', 'down': 'bottom'}
        model(f'leviathan_altar{act}', t, [
            box((1, 0, 1), (15, 2, 15), st),
            box((2, 2, 2), (14, 12, 14), dict(st, up=None, down=None)),
            box((1, 12, 1), (15, 14, 15), dict(st, down='bottom'), light=6 if act else None),
        ])
    v = {}
    for facing, rot in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
        for act, suffix in (('false', ''), ('true', '_active')):
            e = {'model': rid(f'block/leviathan_altar{suffix}')}
            if rot:
                e['y'] = rot
            v[f'active={act},facing={facing}'] = e
    A.blockstate(M, 'leviathan_altar', v)
    A.block_item(M, 'leviathan_altar')

    # --- research terminal: cabinet, CRT screen (glowing), keyboard ledge
    t = {'case': 'block/research_terminal_case', 'front': 'block/research_terminal_front',
         'screen': 'block/research_terminal_screen', 'keys': 'block/research_terminal_keys',
         'particle': 'block/research_terminal_case'}
    model('research_terminal', t, [
        box((1, 3, 3), (15, 15, 15), {'north': 'front', 'south': 'case', 'east': 'case', 'west': 'case', 'up': 'case',
                                      'down': 'case'},
            uvs={'north': [1, 1, 15, 13]}),
        box((3, 5, 2), (13, 13, 3), {'north': 'screen', 'east': 'case', 'west': 'case', 'up': 'case', 'down': 'case'},
            light=12, uvs={'north': [3, 3, 13, 11], 'east': [0, 0, 1, 8], 'west': [0, 0, 1, 8], 'up': [0, 0, 10, 1],
                           'down': [0, 0, 10, 1]}, skip=('south',)),
        box((1, 0, 1), (15, 3, 15), {'north': 'case', 'south': 'case', 'east': 'case', 'west': 'case', 'up': 'keys',
                                     'down': 'case'}),
    ])
    A.blockstate(M, 'research_terminal', A.horizontal_variants('block/research_terminal'))
    A.block_item(M, 'research_terminal')

    # --- expedition radio: cabinet + antenna
    t = {'front': 'block/expedition_radio_front', 'side': 'block/expedition_radio_side',
         'top': 'block/expedition_radio_top', 'antenna': 'block/expedition_radio_antenna',
         'particle': 'block/expedition_radio_side'}
    model('expedition_radio', t, [
        box((1, 0, 4), (15, 9, 12), {'north': 'front', 'south': 'side', 'east': 'side', 'west': 'side', 'up': 'top',
                                     'down': 'side'}, uvs={'north': [1, 7, 15, 16], 'south': [1, 7, 15, 16]}),
        box((12.5, 9, 9.5), (13.5, 16, 10.5), 'antenna', cull_outer=False,
            uvs={k: [0, 0, 1, 7] for k in ('north', 'south', 'east', 'west')} | {'up': [0, 0, 1, 1], 'down': [0, 0, 1, 1]}),
        box((4, 9, 7), (12, 10, 9), 'side', cull_outer=False),
    ])
    A.blockstate(M, 'expedition_radio', A.horizontal_variants('block/expedition_radio'))
    A.block_item(M, 'expedition_radio')

    # --- sigil pedestal
    t = {'side': 'block/sigil_pedestal_side', 'top': 'block/sigil_pedestal_top', 'particle': 'block/sigil_pedestal_side'}
    model('sigil_pedestal', t, [
        box((2, 0, 2), (14, 3, 14), {'north': 'side', 'south': 'side', 'east': 'side', 'west': 'side', 'up': 'top',
                                     'down': 'top'}),
        box((4, 3, 4), (12, 12, 12), 'side'),
        box((2, 12, 2), (14, 15, 14), {'north': 'side', 'south': 'side', 'east': 'side', 'west': 'side', 'up': 'top',
                                       'down': 'top'}),
    ])
    A.blockstate(M, 'sigil_pedestal', {'': {'model': rid('block/sigil_pedestal')}})
    A.block_item(M, 'sigil_pedestal')

    # --- flat items
    for name in ('expedition_journal', 'message_in_a_bottle', 'supply_barrel', 'coral_sigil', 'pearl_sigil',
                 'storm_sigil', 'ember_sigil', 'tidewarden_key'):
        A.flat_item(M, name)


def textures():
    save(journal(), 'item/expedition_journal')
    save(bottle(), 'item/message_in_a_bottle')
    save(barrel(), 'item/supply_barrel')
    for kind in SIGIL:
        anim(sigil_frames(kind), f'item/{kind}_sigil', frametime=3)
    anim(key_frames(), 'item/tidewarden_key', frametime=3)
    anim(seal_frames(), 'block/tide_seal', frametime=3, interpolate=True)
    save(sanctum_tidestone(), 'block/sanctum_tidestone')
    save(sanctum_glass(), 'block/sanctum_glass')
    save(stone_panel(31, rune=[(6, 7), (7, 7), (8, 7), (9, 7), (7, 9), (8, 9)]), 'block/tidewarden_altar_side')
    save(stone_panel(32, front=True), 'block/tidewarden_altar_front')
    save(altar_top(), 'block/tidewarden_altar_top')
    save(gems(), 'block/tidewarden_altar_gems')
    save(lev_side(False), 'block/leviathan_altar_side')
    save(lev_side(True), 'block/leviathan_altar_side_active')
    save(lev_front(False), 'block/leviathan_altar_front')
    save(lev_front(True), 'block/leviathan_altar_front_active')
    save(lev_top_frames(False)[0], 'block/leviathan_altar_top')
    anim(lev_top_frames(True), 'block/leviathan_altar_top_active', frametime=2)
    save(terminal_case(), 'block/research_terminal_case')
    save(terminal_front(), 'block/research_terminal_front')
    anim(terminal_screen_frames(), 'block/research_terminal_screen', frametime=6)
    save(terminal_keys(), 'block/research_terminal_keys')
    save(radio_front(), 'block/expedition_radio_front')
    save(radio_side(), 'block/expedition_radio_side')
    save(radio_top(), 'block/expedition_radio_top')
    save(antenna(), 'block/expedition_radio_antenna')
    save(pedestal_side(), 'block/sigil_pedestal_side')
    save(pedestal_top(), 'block/sigil_pedestal_top')
    save(gui_journal(), 'gui/journal')
    save(gui_terminal(), 'gui/terminal')
    adv_backgrounds()


def generate():
    textures()
    models()
