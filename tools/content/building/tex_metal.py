"""Textures: drowned-city concrete, asphalt, cracked glass, ship steel, pipes, containers."""
import math
import random

from .art import *  # noqa: F401,F403


# ================================================================================================= concrete
def _concrete_base(seed):
    p = CONCRETE
    img = new()
    shade_fill(img, p, 3, 4, seed, cell=8, octaves=2, contrast=0.9)
    rnd = random.Random(seed)
    # aggregate: small 2-px clusters, lighter and darker
    for _ in range(9):
        x, y = rnd.randrange(16), rnd.randrange(16)
        put(img, x, y, p[5])
        put(img, x + 1, y, p[4])
    for _ in range(7):
        x, y = rnd.randrange(16), rnd.randrange(16)
        put(img, x, y, p[2])
        put(img, x, y + 1, p[3])
    return img


def cracked_concrete():
    """Weathered cast concrete: a branching diagonal crack, a hairline, faint water stains and a spalled pit."""
    p = CONCRETE
    img = _concrete_base(101)
    crack(img, [(1, 4), (2, 5), (3, 5), (4, 6), (5, 7), (6, 7), (7, 8), (8, 9), (8, 10), (9, 11)], p[1], p[5])
    crack(img, [(5, 7), (5, 8), (4, 9), (4, 10)], p[1], p[5])
    crack(img, [(8, 9), (9, 9), (10, 8)], p[1])
    crack(img, [(12, 1), (13, 2), (13, 3), (14, 4)], p[2])
    # water stains running down from the cracks (blended, not painted)
    for (x, y0, y1) in [(4, 11, 14), (9, 12, 15), (14, 5, 7)]:
        for y in range(y0, y1 + 1):
            put(img, x, y, lerp(get(img, x, y), p[2], 0.45 if y < y1 else 0.25))
    # a spalled pit
    for (x, y, c) in [(11, 13, 1), (12, 13, 2), (11, 14, 2), (12, 14, 3), (10, 13, 5)]:
        put(img, x, y, p[c])
    return img


def mossy_concrete():
    """Cracked concrete with algae growing out of the cracks and up from the water line (bottom)."""
    img = cracked_concrete()
    n = noise(103, 4, 2)
    crackpx = {(x, y) for y in range(16) for x in range(16) if get(img, x, y) == CONCRETE[1]}
    score = {}
    for y in range(16):
        for x in range(16):
            s = n[y][x] + max(0, y - 9) * 0.05
            if any(((x + dx) % 16, (y + dy) % 16) in crackpx for dx in (-1, 0, 1) for dy in (-1, 0, 1)):
                s += 0.22
            score[(x, y)] = s
    thr = sorted(score.values())[int(256 * 0.70)]
    moss = {k for k, v in score.items() if v >= thr}
    for (x, y) in moss:
        if (x, y) in crackpx:
            put(img, x, y, ALGAE[0])
        elif (x, (y - 1) % 16) not in moss:
            put(img, x, y, ALGAE[3])
        else:
            put(img, x, y, ALGAE[2] if (x * 3 + y) % 4 else ALGAE[1])
    return img


REBAR = pal('#3a1a10', '#5e2c18', '#87431f', '#a85e2e', '#c47d43')


def rebar_concrete():
    """Concrete whose upper-right corner has broken off: a rough, shadowed core of aggregate with two ribbed, rusty
    rebars running through it, and rust stains bleeding down the intact face."""
    p = CONCRETE
    img = _concrete_base(105)
    rnd = random.Random(106)
    edge = {y: 7 + (y * 5) // 8 + (1 if y in (3, 6) else 0) for y in range(0, 11)}  # broken edge x per row
    for y, x0 in edge.items():
        for x in range(x0, 16):
            r = rnd.random()
            put(img, x, y, p[0] if r < 0.12 else p[1] if r < 0.55 else p[2])
        put(img, x0, y, p[5])  # lit fracture rim
        if x0 - 1 >= 0:
            put(img, x0 - 1, y, p[4])
    for x in range(edge[10], 16):  # shadow under the break
        put(img, x, 11, lerp(get(img, x, 11), p[1], 0.5))
    for y in (2, 7):  # two ribbed rebars, rust with a dark underside
        for x in range(edge[y] - 1, 16):
            put(img, x, y, REBAR[3] if x % 2 else REBAR[4])
            put(img, x, y + 1, REBAR[1] if x % 2 else REBAR[2])
    for (x, y0, y1) in [(9, 9, 14), (12, 12, 15), (13, 12, 13)]:  # rust stains
        for y in range(y0, y1 + 1):
            put(img, x, y, lerp(get(img, x, y), REBAR[1], 0.38 if y < y1 else 0.2))
    return img


def rebar_rod():
    """Texture for the rods sticking out of rebar concrete (only the used area matters)."""
    img = new()
    for y in range(16):
        for x in range(16):
            put(img, x, y, REBAR[[2, 3, 4, 2][x % 4]] if y % 3 else REBAR[1])
    return img


# ================================================================================================= asphalt
def asphalt():
    p = ASPHALT
    img = new()
    shade_fill(img, p, 1, 3, 111, cell=4, octaves=3, contrast=1.0)
    rnd = random.Random(112)
    for _ in range(10):  # pale aggregate grains (2 px)
        x, y = rnd.randrange(16), rnd.randrange(16)
        put(img, x, y, p[5])
        put(img, x + rnd.choice([1, 0]), y + rnd.choice([0, 1]), p[4])
    for _ in range(6):  # dark tar pits
        x, y = rnd.randrange(16), rnd.randrange(16)
        put(img, x, y, p[0])
    # a fine repaired crack
    crack(img, [(2, 15), (2, 0), (3, 1), (3, 2), (4, 3)], p[0])
    return img


LINE = pal('#9e9b8f', '#c6c3b6', '#e3e0d3')


def asphalt_line():
    """Top of the lane-line asphalt for axis=z: a worn white dash running north-south (rows 1..10)."""
    img = asphalt()
    rnd = random.Random(113)
    for y in range(1, 11):
        for x in (7, 8):
            if rnd.random() < 0.12:
                continue  # worn
            put(img, x, y, LINE[2] if x == 7 else LINE[1])
    put(img, 7, 1, LINE[1])
    put(img, 8, 10, LINE[0])
    return img


# ================================================================================================= glass
def cracked_glass():
    """Vanilla-glass style (clear inside, cutout) with a shattered corner and spider cracks."""
    frame = pal('#8fa9b0', '#bcd6db', '#e7f5f7')
    img = new()
    for i in range(16):
        put(img, i, 0, frame[2])
        put(img, 0, i, frame[2])
        put(img, i, 15, frame[1])
        put(img, 15, i, frame[1])
    # missing frame pieces where the pane broke
    for (x, y) in [(15, 4), (15, 5), (15, 6), (11, 15), (12, 15)]:
        img.putpixel((x, y), (0, 0, 0, 0))
    # impact point and cracks
    cx, cy = 10, 6
    rays = [[(10, 6), (11, 5), (12, 4), (13, 3), (14, 2), (15, 1)],
            [(10, 6), (9, 5), (8, 4), (7, 4), (6, 3), (5, 2), (4, 2), (3, 1)],
            [(10, 6), (10, 7), (9, 8), (9, 9), (8, 10), (8, 11), (7, 12), (6, 13), (6, 14)],
            [(10, 6), (11, 7), (12, 8), (13, 9), (14, 9)],
            [(10, 6), (9, 7), (7, 7), (5, 8), (3, 8), (1, 9)]]
    for r in rays:
        for (x, y) in r:
            put(img, x, y, frame[1])
    for (x, y) in [(11, 6), (10, 5), (9, 6)]:  # crushed centre
        put(img, x, y, frame[2])
    for (x, y) in [(12, 5), (11, 4), (8, 8), (12, 7)]:  # ring crack
        put(img, x, y, frame[0])
    for (x, y) in [(2, 2), (3, 3), (2, 4)]:  # glint
        put(img, x, y, frame[2])
    return img


def cracked_glass_pane_top():
    img = new()
    c = pal('#bcd6db', '#e7f5f7')
    for y in range(16):
        for x in range(16):
            put(img, x, y, c[1] if (y % 5) else c[0])
    return img


# ================================================================================================= hull plates
def _plates(p, seed, rivet_light, rivet_dark):
    """Two horizontal hull plates (seam at y=7/8) with rivet rows along the seams."""
    img = new()
    shade_fill(img, p, 3, 4, seed, cell=8, octaves=2, contrast=0.8)
    for x in range(16):
        put(img, x, 0, p[5])
        put(img, x, 7, p[1])
        put(img, x, 8, p[5])
        put(img, x, 15, p[1])
    for x in range(1, 16, 4):  # one rivet row along each seam
        for y in (2, 10):
            put(img, x, y, rivet_light)
            put(img, x + 1, y + 1, rivet_dark)
    # vertical butt seam of the lower plate
    for y in range(8, 15):
        put(img, 9, y, p[2])
        put(img, 10, y, p[4])
    return img


def riveted_hull():
    p = NAVY
    img = _plates(p, 121, p[6], p[1])
    # paint wear and a little rust bleeding from rivets
    for (x, y) in [(4, 4), (5, 4), (12, 11), (13, 11), (14, 3)]:
        put(img, x, y, p[5])
    for (x, y0, y1) in [(7, 3, 5), (13, 11, 13)]:
        for y in range(y0, y1 + 1):
            put(img, x, y, RUST[3] if y == y0 else RUST[2])
    return img


def rusty_hull():
    p = NAVY
    img = _plates(p, 122, p[5], p[0])
    rust_colours = [RUST[1], RUST[2], RUST[3], RUST[4], RUST[5]]
    blotches(img, rust_colours, 0.45, 124, cell=4, octaves=2)
    for (x, y0, y1) in [(3, 6, 12), (11, 0, 6), (14, 8, 15)]:  # streaks
        for y in range(y0, y1 + 1):
            put(img, x, y, RUST[2] if y % 3 else RUST[3])
    for (x, y) in [(6, 3), (7, 3), (6, 4)]:  # flaking pit
        put(img, x, y, RUST[0])
    return img


def corrugated_metal():
    """Galvanised corrugated sheet: vertical waves (period 4), a lap seam and a few rust spots."""
    p = GALV
    img = new()
    wave = [5, 4, 3, 2]
    for y in range(16):
        for x in range(16):
            put(img, x, y, p[wave[x % 4]])
    n = noise(131, 4)
    for y in range(16):
        for x in range(16):
            if n[y][x] > 0.7:
                c = img.getpixel((x, y))
                put(img, x, y, p[min(6, p.index(c) + 1)] if c in p else c)
    for x in range(16):  # overlap seam with screws
        put(img, x, 12, p[1])
        put(img, x, 13, p[6] if x % 4 == 0 else p[4] if x % 4 == 1 else p[3])
    for x in range(1, 16, 4):
        put(img, x, 14, p[1])
    blotches(img, [RUST[2], RUST[3], RUST[4]], 0.08, 133, cell=2,
             mask=lambda x, y: y > 8)
    return img


def rusted_plate():
    """Diamond-plate steel floor: raised lozenges in staggered rows, rust eating in from patches; the lozenges stay
    visible through the rust (they are drawn after it, tinted)."""
    p = STEEL
    img = new()
    shade_fill(img, p, 2, 3, 141, cell=8, contrast=0.6)
    rust = blotch_mask(0.34, 147, cell=4, octaves=2)
    deep = blotch_mask(0.12, 147, cell=4, octaves=2)
    for y in range(16):
        for x in range(16):
            if deep(x, y):
                put(img, x, y, RUST[2] if (x + y) % 4 else RUST[1])
            elif rust(x, y):
                put(img, x, y, RUST[3] if (x * 5 + y * 3) % 7 else RUST[4])
    for gy in range(4):
        for gx in range(4):
            ox = gx * 4 + (2 if gy % 2 else 0)
            oy = gy * 4 + 1
            rusty = rust(ox + 1, oy + 1)
            hi, mid, lo = (RUST[5], RUST[4], RUST[1]) if rusty else (p[5], p[4], p[1])
            put(img, ox, oy + 1, hi)  # raised lozenge, lit top-left
            put(img, ox + 1, oy, hi)
            put(img, ox + 1, oy + 1, mid)
            put(img, ox + 2, oy + 1, lo)
            put(img, ox + 1, oy + 2, lo)
    return img


# ================================================================================================= beams, grating, railing
BEAM = pal('#2b3238', '#3a434b', '#4b555e', '#5d6872', '#717d87', '#8a959e')


def steel_beam():
    """Outer face of a flange (along the beam): painted steel with a bolt line."""
    p = BEAM
    img = new()
    shade_fill(img, p, 3, 4, 151, cell=8, contrast=0.6)
    for y in range(16):
        put(img, 0, y, p[5])
        put(img, 1, y, p[4])
        put(img, 14, y, p[2])
        put(img, 15, y, p[1])
    for y in (2, 10):  # bolt pairs
        for x in (4, 11):
            put(img, x, y, p[5])
            put(img, x + 1, y, p[4])
            put(img, x, y + 1, p[2])
            put(img, x + 1, y + 1, p[1])
    for (x, y0, y1) in [(8, 5, 8), (6, 12, 14)]:  # rust drips from the bolts
        for y in range(y0, y1 + 1):
            put(img, x, y, RUST[2] if y == y0 else lerp(RUST[2], p[3], 0.4))
    return img


def steel_beam_inner():
    """Web and the inner faces of the flanges: in shadow, with weathering streaks."""
    p = BEAM
    img = new()
    shade_fill(img, p, 1, 3, 152, cell=4, contrast=0.6)
    for x in (3, 9, 12):
        for y in range(16):
            if (y + x) % 6:
                put(img, x, y, p[1])
    for (x, y) in [(5, 4), (5, 5), (10, 11)]:
        put(img, x, y, RUST[2])
    return img


def steel_beam_end():
    p = BEAM
    img = new(p[4])
    shade_fill(img, p, 4, 5, 153, cell=4, contrast=0.6)
    bevel(img, 0, 0, 15, 15, p[5], p[2])
    return img


def catwalk():
    """Bar grating: frame, bearing bars every 3 px and two cross rods; the gaps are open (cutout)."""
    p = GALV
    img = new()
    for y in range(16):
        for x in range(16):
            frame = x in (0, 15) or y in (0, 15)
            bar = x in (3, 6, 9, 12)
            rod = y in (5, 10)
            if frame:
                c = p[5] if (x == 0 or y == 0) else p[2]
            elif rod:
                c = p[4] if not bar else p[5]
            elif bar:
                c = p[3] if y % 5 != 4 else p[2]
            else:
                continue
            img.putpixel((x, y), c)
    blotches(img, [RUST[2], RUST[3]], 0.10, 155, cell=2)
    return img


RAIL = pal('#5a5f60', '#7d8384', '#a2a8a7', '#c7cbc8', '#e4e6e1')


def steel_railing():
    """Railing picture (also the item sprite): top rail rows 0..2, mid rail rows 7..8, post columns 7..8."""
    p = RAIL
    img = new()
    for x in range(16):
        put(img, x, 0, p[4])
        put(img, x, 1, p[3])
        put(img, x, 2, p[1])
        put(img, x, 7, p[3])
        put(img, x, 8, p[1])
    for y in range(16):
        put(img, 7, y, p[3] if y not in (2, 8) else p[2])
        put(img, 8, y, p[1] if y not in (0, 7) else p[2])
    put(img, 7, 15, p[2])
    put(img, 8, 15, p[0])
    # a little rust where the rails meet the post
    for (x, y) in [(9, 2), (6, 8), (9, 8)]:
        put(img, x, y, RUST[3])
    return img


# ================================================================================================= porthole, door, hatch
def porthole():
    """Steel plate with a brass-rimmed round window (translucent glass)."""
    p = NAVY
    img = new()
    shade_fill(img, p, 3, 4, 161, cell=8, contrast=0.6)
    bevel(img, 0, 0, 15, 15, p[5], p[1])
    for (x, y) in [(2, 2), (13, 2), (2, 13), (13, 13)]:
        put(img, x, y, p[6])
        put(img, x + 1, y + 1, p[1])
    ring(img, 8, 8, 4.6, 6.6, lambda x, y, a, d: lit(a, BRASS, 3, 1) if d < 5.8 else lit(a, BRASS, 2, 1))
    for k in range(8):  # rim bolts
        a = k * math.pi / 4
        x, y = int(8 + math.cos(a) * 5.6), int(8 + math.sin(a) * 5.6)
        put(img, x, y, BRASS[5] if k in (5, 6, 4) else BRASS[1])
    g = GLASS_BLUE
    ring(img, 8, 8, 0, 4.6, lambda x, y, a, d: with_alpha(g[2] if d < 3.2 else g[1], 140))
    for (x, y) in [(6, 5), (5, 6), (7, 5), (5, 7)]:
        put(img, x, y, with_alpha(g[3], 210))
    put(img, 10, 10, with_alpha(g[3], 180))
    return img


def _door_frame(img, p):
    for y in range(16):
        put(img, 0, y, p[4])
        put(img, 15, y, p[1])


def bulkhead_door_top():
    p = PAINT_WHITE
    img = new()
    shade_fill(img, p, 2, 3, 171, cell=8, contrast=0.6)
    _door_frame(img, p)
    for x in range(16):
        put(img, x, 0, p[4])
    for y in range(1, 16, 3):  # rivet columns
        put(img, 1, y, p[4])
        put(img, 2, y + 1, p[0])
        put(img, 13, y, p[4])
        put(img, 14, y + 1, p[0])
    ring(img, 8, 8, 3.9, 5.6, lambda x, y, a, d: lit(a, BRASS, 3, 1))
    g = GLASS_BLUE
    ring(img, 8, 8, 0, 3.9, lambda x, y, a, d: with_alpha(g[2] if d < 2.5 else g[1], 140))
    put(img, 6, 6, with_alpha(g[3], 210))
    put(img, 7, 5, with_alpha(g[3], 210))
    for x in range(1, 15):  # weathered lower edge with grime
        if x % 3:
            put(img, x, 15, p[1])
    return img


def bulkhead_door_bottom():
    p = PAINT_WHITE
    img = new()
    shade_fill(img, p, 2, 3, 172, cell=8, contrast=0.6)
    _door_frame(img, p)
    for y in range(0, 16, 3):
        put(img, 1, y, p[4])
        put(img, 2, y + 1, p[0])
        put(img, 13, y, p[4])
        put(img, 14, y + 1, p[0])
    # horizontal stiffener and lever handle
    for x in range(3, 13):
        put(img, x, 5, p[4])
        put(img, x, 6, p[0])
    for y in range(1, 5):
        put(img, 11, y, DARK[3])
        put(img, 12, y, DARK[1])
    put(img, 10, 1, DARK[4])
    put(img, 11, 0, DARK[4])
    # red-white hazard band at the bottom
    for x in range(1, 15):
        for y in (12, 13, 14):
            put(img, x, y, RED[3] if (x + y) % 4 < 2 else p[3])
    for x in range(16):
        put(img, x, 15, p[0])
    for (x, y) in [(4, 9), (5, 9), (9, 10)]:
        put(img, x, y, RUST[3])
    return img


def bulkhead_door_item(bottom, top):
    from PIL import Image
    door = Image.new('RGBA', (16, 32))
    door.alpha_composite(top, (0, 0))
    door.alpha_composite(bottom, (0, 16))
    small = door.resize((8, 16), Image.NEAREST)
    img = new()
    img.alpha_composite(small.crop((0, 0, 8, 15)), (4, 0))
    return img


def metal_hatch():
    """Steel hatch: raised rim, diamond plate and a red hand wheel in the middle."""
    p = STEEL
    img = new()
    shade_fill(img, p, 3, 4, 181, cell=8, contrast=0.6)
    bevel(img, 0, 0, 15, 15, p[6], p[1])
    bevel(img, 1, 1, 14, 14, p[2], p[5])
    for gy in range(3):
        for gx in range(3):
            ox, oy = 3 + gx * 4, 3 + gy * 4
            if (gx, gy) == (1, 1):
                continue
            put(img, ox, oy + 1, p[5])
            put(img, ox + 1, oy, p[5])
            put(img, ox + 1, oy + 1, p[1])
    ring(img, 8, 8, 2.6, 4.2, lambda x, y, a, d: lit(a, RED, 3, 1))
    for (x, y) in [(7, 7), (8, 8), (7, 8), (8, 7)]:
        put(img, x, y, DARK[3])
    put(img, 7, 7, STEEL[5])
    for (x, y) in [(8, 5), (8, 10), (5, 8), (10, 8)]:
        put(img, x, y, RED[2])
    return img


# ================================================================================================= pipes
def rusty_pipe():
    """Pipe body: rows 0..1 are the bolted flange band (used by the collars), the rest is the pipe skin.
    Columns 4..11 are the visible face of the pipe; shading across it makes it look round."""
    img = new()
    across = [3, 4, 5, 5, 4, 4, 3, 3, 2, 2, 1, 1, 2, 3, 3, 3]
    for y in range(16):
        for x in range(16):
            put(img, x, y, RUST[across[x]])
    blotches(img, [RUST[1], RUST[2]], 0.18, 191, cell=2)
    for (x, y0, y1) in [(6, 3, 9), (9, 6, 14)]:  # drip streaks
        for y in range(y0, y1 + 1):
            put(img, x, y, RUST[2])
    # flange band
    for x in range(16):
        put(img, x, 0, RUST[5])
        put(img, x, 1, RUST[2])
    for x in range(1, 16, 3):
        put(img, x, 0, RUST[6])
    return img


def rusty_pipe_end():
    img = new()
    ring(img, 8, 8, 0, 7.2, lambda x, y, a, d: lit(a, RUST, 3, 1))
    ring(img, 8, 8, 0, 4.6, lambda x, y, a, d: lit(a, RUST, 4, 1))
    ring(img, 8, 8, 0, 2.9, lambda x, y, a, d: RUST[0] if d > 1.5 else tex.rgb('#24120b'))
    for k in range(4):
        a = k * math.pi / 2 + math.pi / 4
        put(img, int(8 + math.cos(a) * 5.8), int(8 + math.sin(a) * 5.8), RUST[6])
    fill_outside = [(x, y) for y in range(16) for x in range(16) if img.getpixel((x, y))[3] == 0]
    for (x, y) in fill_outside:
        put(img, x, y, RUST[2])
    return img


def valve_wheel():
    """Red hand wheel with spokes (cutout); 8x8 in the middle of the texture."""
    img = new()
    ring(img, 8, 8, 2.7, 4.0, lambda x, y, a, d: lit(a, RED, 3, 1))
    for (x, y) in [(7, 5), (8, 5), (7, 10), (8, 10), (5, 7), (5, 8), (10, 7), (10, 8)]:
        put(img, x, y, RED[3])
    for (x, y) in [(7, 6), (8, 9), (6, 7), (9, 8)]:
        put(img, x, y, RED[2])
    for (x, y) in [(7, 7), (8, 7), (7, 8), (8, 8)]:
        put(img, x, y, STEEL[4] if (x, y) == (7, 7) else STEEL[2])
    return img


# ================================================================================================= containers
CONTAINERS = {'red': CONT_RED, 'blue': CONT_BLUE, 'green': CONT_GREEN}


def container_side(colour):
    p = CONTAINERS[colour]
    img = new()
    rib = [4, 3, 3, 1]  # lit rib face, flat, flat, shadowed groove
    for y in range(16):
        for x in range(16):
            put(img, x, y, p[rib[x % 4]])
    for x in range(16):  # top and bottom rails
        put(img, x, 0, p[5])
        put(img, x, 1, p[2])
        put(img, x, 14, p[2])
        put(img, x, 15, p[0])
    blotches(img, [RUST[2], RUST[3], RUST[4]], 0.07, {'red': 201, 'blue': 202, 'green': 203}[colour], cell=2,
             mask=lambda x, y: y >= 11 or y <= 2)
    for (x, y) in [(5, 6), (6, 6), (13, 9)]:  # scuffs
        put(img, x, y, p[5])
    return img


def container_end(colour):
    """Door end: two leaves, four locking bars with cams and handles."""
    p = CONTAINERS[colour]
    img = new()
    rib = [3, 3, 2, 1]
    for y in range(16):
        for x in range(16):
            put(img, x, y, p[rib[y % 4]] if 1 <= x <= 14 else p[2])
    bevel(img, 0, 0, 15, 15, p[5], p[0])
    for y in range(1, 15):
        put(img, 7, y, p[0])
        put(img, 8, y, p[4])
    bar = GALV
    for x in (3, 5, 10, 12):
        for y in range(1, 15):
            put(img, x, y, bar[4] if y % 6 else bar[2])
        put(img, x + 1, 1, bar[1])
        put(img, x + 1, 14, bar[1])
    for (x, y) in [(4, 8), (6, 8), (11, 8), (13, 8)]:  # handles
        put(img, x, y, bar[5])
        put(img, x, y + 1, bar[2])
    blotches(img, [RUST[2], RUST[3]], 0.06, {'red': 204, 'blue': 205, 'green': 206}[colour], cell=2)
    return img


def container_top(colour):
    p = CONTAINERS[colour]
    img = new()
    for y in range(16):
        for x in range(16):
            put(img, x, y, p[[3, 2, 2, 1][y % 4]])
    blotches(img, [p[1], p[0]], 0.25, {'red': 207, 'blue': 208, 'green': 209}[colour], cell=4)
    blotches(img, [RUST[2], RUST[3]], 0.08, {'red': 210, 'blue': 211, 'green': 212}[colour], cell=2)
    for x in range(16):
        put(img, x, 0, p[4])
    return img


def catwalk_side():
    """Side of the grating: each 8-row half shows a lit top bar, the bearing-bar ends and a dark bottom bar,
    so bottom slabs (rows 8..15), top slabs (rows 0..7) and double blocks all read right."""
    p = GALV
    img = new()
    for half in (0, 8):
        for x in range(16):
            put(img, x, half, p[5])
            put(img, x, half + 1, p[3])
            put(img, x, half + 6, p[3])
            put(img, x, half + 7, p[1])
        for x in range(0, 16, 3):
            for y in range(half + 2, half + 6):
                put(img, x, y, p[4] if y < half + 4 else p[2])
    blotches(img, [RUST[2], RUST[3]], 0.08, 156, cell=2)
    return img
