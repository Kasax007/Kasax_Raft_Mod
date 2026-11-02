"""16x16 item sprites of the shore module (crab claws, ember shell, cutlass, spawn eggs).

Shapes are rasterised from simple geometry into masks and then shaded per pixel: one hue-shifted ramp per
material, light from the top-left, darker rim on the lower-right, a 1-px dark outline (vanilla item style)."""
import math

from PIL import Image

from bclib import tex

MOD = 'shore'


# ---------------------------------------------------------------------------------------------- raster helpers
def blank():
    return [[0] * 16 for _ in range(16)]


def disc(mask, cx, cy, r, val=1):
    for y in range(16):
        for x in range(16):
            if (x - cx) ** 2 + (y - cy) ** 2 <= r * r:
                mask[y][x] = val


def ellipse(mask, cx, cy, rx, ry, angle=0.0, val=1):
    ca, sa = math.cos(angle), math.sin(angle)
    for y in range(16):
        for x in range(16):
            dx, dy = x - cx, y - cy
            u = dx * ca + dy * sa
            v = -dx * sa + dy * ca
            if (u / rx) ** 2 + (v / ry) ** 2 <= 1.0:
                mask[y][x] = val


def stroke(mask, pts, r, val=1, steps=40):
    """Thick polyline through pts (list of (x, y)) with radius r (can taper: r=(r0, r1))."""
    r0, r1 = (r, r) if not isinstance(r, tuple) else r
    total = len(pts) - 1
    for i in range(total):
        (x0, y0), (x1, y1) = pts[i], pts[i + 1]
        for s in range(steps + 1):
            t = s / steps
            g = (i + t) / total
            disc(mask, x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, r0 + (r1 - r0) * g, val)


def shade_mask(img, mask, ramp, val=1, light=(-0.7, -0.7), base=0.55, grad=0.32, rim=True):
    """Shade the pixels of mask==val: gradient along the light direction, highlight on lit edges, dark on the far rim."""
    pts = [(x, y) for y in range(16) for x in range(16) if mask[y][x] == val]
    if not pts:
        return
    xs = [p[0] for p in pts]
    ys = [p[1] for p in pts]
    cx, cy = (min(xs) + max(xs)) / 2, (min(ys) + max(ys)) / 2
    ext = max(1.0, max(max(xs) - min(xs), max(ys) - min(ys)) / 2)
    k = len(ramp)
    for (x, y) in pts:
        d = ((x - cx) * light[0] + (y - cy) * light[1]) / ext
        v = base + d * grad
        if rim:
            lit_edge = any(0 <= x + dx < 16 and 0 <= y + dy < 16 and mask[y + dy][x + dx] != val
                           for dx, dy in ((-1, 0), (0, -1)))
            dark_edge = any(not (0 <= x + dx < 16 and 0 <= y + dy < 16) or mask[y + dy][x + dx] != val
                            for dx, dy in ((1, 0), (0, 1)))
            if lit_edge:
                v += 0.18
            if dark_edge:
                v -= 0.2
        img.putpixel((x, y), ramp[max(0, min(k - 1, int(round(v * (k - 1)))))])


def finish(img, outline_colour):
    tex.outline(img, outline_colour)
    return img


def px(img, x, y, c):
    tex.px(img, x, y, c)


# ---------------------------------------------------------------------------------------------- claws
CLAW_MAP = [
    "................",
    "..........222...",
    "........222222..",
    ".......2222.222.",
    "......2222...22.",
    ".....1122.....2.",
    "....11111.....3.",
    "...1111111...33.",
    "..111111111333..",
    "..11111111133...",
    ".111111111......",
    ".11111111.......",
    ".1111111........",
    "..1111..........",
    ".11.............",
    "................",
]


def _claw_mask():
    m, upper, lower = blank(), blank(), blank()
    for y, row in enumerate(CLAW_MAP):
        for x, ch in enumerate(row):
            if ch == '1':
                m[y][x] = 1
            elif ch == '2':
                upper[y][x] = 1
            elif ch == '3':
                lower[y][x] = 1
    return m, upper, lower


def crab_claw(cooked=False):
    img = tex.new()
    palm, upper, lower = _claw_mask()
    shell = tex.ramp('#e8642c' if cooked else '#b8382a', 6, spread=0.55, sat_shift=0.0)
    tip = tex.ramp('#9a2a1a' if cooked else '#3c2522', 4, spread=0.4)
    shade_mask(img, palm, shell, base=0.55)
    for m in (upper, lower):
        shade_mask(img, m, shell, base=0.6)
    # dark tips of the fingers
    for (x, y) in ((13, 3), (14, 4), (14, 5), (14, 6), (13, 7)):
        if img.getpixel((x, y))[3]:
            px(img, x, y, tip[1] if (x + y) % 2 else tip[2])
    # cream teeth along the inner edges
    cream = tex.rgb('#f1dfbf')
    for (x, y) in ((10, 3), (12, 3), (11, 8), (12, 8)):
        if img.getpixel((x, y))[3]:
            px(img, x, y, cream)
    # knobbles on the palm
    for (x, y) in ((5, 7), (7, 7), (4, 9), (6, 9)):
        px(img, x, y, shell[5])
    if cooked:
        # cracked shell showing white meat
        meat = tex.ramp('#f6eee2', 3, spread=0.2)
        for (x, y) in ((2, 11), (3, 11), (2, 12), (3, 12), (4, 12), (3, 13), (4, 13), (2, 10)):
            px(img, x, y, meat[1] if (x + y) % 2 else meat[2])
        px(img, 5, 12, shell[1]); px(img, 4, 11, shell[1]); px(img, 3, 10, shell[1])
        for (x, y) in ((8, 8), (9, 8)):
            px(img, x, y, '#fbe7c4')
    return finish(img, '#3a1410' if not cooked else '#4a1a0e')


def ember_shell():
    img = tex.new()
    m = blank()
    # scallop-like fan with a hinge at the bottom
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 12.5
            r = math.hypot(dx, dy)
            ang = math.atan2(dy, dx)
            if r <= 9.2 and -math.pi * 0.93 <= ang <= -math.pi * 0.07 and y >= 2:
                m[y][x] = 1
    stroke(m, [(5.5, 13.2), (9.5, 13.2)], 1.0)
    basalt = tex.ramp('#4a4448', 6, spread=0.45)
    shade_mask(img, m, basalt, base=0.5)
    # radial ridges
    for i in range(-3, 4):
        ang = -math.pi / 2 + i * 0.32
        for rr in range(3, 10):
            x = int(round(7.5 + math.cos(ang) * rr))
            y = int(round(12.5 + math.sin(ang) * rr))
            if 0 <= x < 16 and 0 <= y < 16 and m[y][x]:
                px(img, x, y, basalt[4] if rr % 2 else basalt[3])
                xx = int(round(7.5 + math.cos(ang + 0.16) * rr))
                if 0 <= xx < 16 and m[y][xx]:
                    px(img, xx, y, basalt[1])
    # glowing ember cracks between the ridges
    hot = tex.ramp('#ff8a1e', 4, spread=0.45)
    for (x, y) in ((6, 5), (6, 6), (7, 7), (9, 4), (10, 5), (10, 6), (4, 8), (5, 9), (11, 8), (12, 9), (8, 9)):
        if m[y][x]:
            px(img, x, y, hot[3] if (x + y) % 3 == 0 else hot[2])
    px(img, 7, 7, '#ffe08a'); px(img, 10, 5, '#ffd36a')
    return finish(img, '#1c1719')


CUTLASS_MAP = [
    "..............ES",
    "...........EESsd",
    ".........EESssd.",
    "........ESssd...",
    ".......ESsd.....",
    "......ESsd......",
    ".....ESsd.......",
    ".....Esd........",
    "....ESsd........",
    "..ggESd.........",
    ".gGGGgg.........",
    "g..WGg..........",
    "g.Ww.g..........",
    ".gwg.g..........",
    "pPgggg..........",
    "pp..............",
]


def cutlass():
    img = tex.new()
    steel = tex.ramp('#c3ccd6', 6, spread=0.55)
    gold = tex.ramp('#e0ae3a', 5, spread=0.5)
    wood = tex.ramp('#7a4a2a', 4, spread=0.45)
    colours = {'E': steel[5], 'S': steel[4], 's': steel[3], 'd': steel[1],
               'G': gold[4], 'g': gold[2], 'P': gold[3], 'p': gold[1], 'W': wood[2], 'w': wood[1]}
    for y, row in enumerate(CUTLASS_MAP):
        for x, ch in enumerate(row):
            if ch in colours:
                px(img, x, y, colours[ch])
    return finish(img, '#1d1a22')


# ---------------------------------------------------------------------------------------------- spawn eggs
def egg_mask():
    m = blank()
    for y in range(16):
        for x in range(16):
            dx = (x - 7.5) / 5.4
            dy = (y - 8.6) / 6.6
            # narrower towards the top
            dx *= 1.0 + max(0.0, -dy) * 0.28
            if dx * dx + dy * dy <= 1.0 and 1 <= y <= 15:
                m[y][x] = 1
    return m


def spawn_egg(kind):
    img = tex.new()
    m = egg_mask()
    if kind == 'crab':
        shade_mask(img, m, tex.ramp('#c8472e', 6, spread=0.6), base=0.55)
        for (x, y) in ((4, 7), (11, 7), (6, 11), (9, 11), (7, 5), (8, 5)):
            px(img, x, y, '#7c2418')
        # eye stalks and claws poking out
        for x in (6, 9):
            px(img, x, 2, '#e07a5a'); px(img, x, 1, '#24212a')
        px(img, 6, 0, '#f4f2ea')
        for (x, y) in ((1, 9), (1, 10), (2, 8), (14, 9), (14, 10), (13, 8)):
            px(img, x, y, '#d8603e')
        px(img, 0, 9, '#3a2422'); px(img, 15, 9, '#3a2422')
        return finish(img, '#4a1410')
    if kind == 'ember_crab':
        shade_mask(img, m, tex.ramp('#45403f', 6, spread=0.45), base=0.5)
        for (x, y) in ((7, 4), (7, 5), (8, 6), (8, 7), (5, 8), (6, 9), (10, 9), (11, 10), (9, 12), (5, 12)):
            px(img, x, y, '#ff8a1e')
        px(img, 8, 6, '#ffd36a'); px(img, 6, 9, '#ffd36a')
        for x in (6, 9):
            px(img, x, 2, '#5c5558'); px(img, x, 1, '#ffb347')
        for (x, y) in ((1, 9), (2, 8), (14, 9), (13, 8)):
            px(img, x, y, '#5c5558')
        px(img, 1, 10, '#ff7a1a'); px(img, 14, 10, '#ff7a1a')
        return finish(img, '#151214')
    if kind == 'seagull':
        shade_mask(img, m, tex.ramp('#f2f1ec', 5, spread=0.3), base=0.6)
        grey = tex.ramp('#9aa4ae', 4, spread=0.4)
        for y in range(6, 15):
            for x in (2, 3, 12, 13):
                if m[y][x]:
                    px(img, x, y, grey[2 if x in (3, 12) else 1])
        for (x, y) in ((3, 13), (12, 13), (2, 12), (13, 12)):
            if m[y][x]:
                px(img, x, y, '#2c2c33')
        px(img, 6, 5, '#1e1c24'); px(img, 9, 5, '#1e1c24')
        for (x, y) in ((7, 6), (8, 6), (7, 7), (8, 7)):
            px(img, x, y, '#f0c030')
        px(img, 8, 7, '#d2382a')
        return finish(img, '#5a6068')
    if kind == 'penguin':
        shade_mask(img, m, tex.ramp('#2c2f3b', 5, spread=0.35), base=0.55)
        belly = blank()
        ellipse(belly, 7.5, 10.2, 3.3, 4.4)
        for y in range(16):
            for x in range(16):
                belly[y][x] = belly[y][x] and m[y][x]
        shade_mask(img, belly, tex.ramp('#f3f1ea', 4, spread=0.25), base=0.65, rim=False)
        for (x, y) in ((7, 6), (8, 6)):
            px(img, x, y, '#f39a2c')
        px(img, 7, 7, '#d27a1c')
        px(img, 5, 4, '#f2f2f2'); px(img, 10, 4, '#f2f2f2')
        px(img, 4, 6, '#f7c84a'); px(img, 11, 6, '#f7c84a')
        return finish(img, '#101116')
    if kind == 'sea_otter':
        shade_mask(img, m, tex.ramp('#6f4a30', 6, spread=0.5), base=0.55)
        face = blank()
        ellipse(face, 7.5, 6.6, 3.6, 3.0)
        shade_mask(img, face, tex.ramp('#cdb08a', 4, spread=0.35), base=0.6, rim=False)
        px(img, 6, 6, '#171214'); px(img, 9, 6, '#171214')
        px(img, 7, 8, '#2a1c18'); px(img, 8, 8, '#2a1c18')
        px(img, 5, 8, '#f6efe2'); px(img, 10, 8, '#f6efe2')
        px(img, 4, 2, '#45301f'); px(img, 11, 2, '#45301f')
        # paws holding a pink shell
        for (x, y) in ((6, 11), (9, 11)):
            px(img, x, y, '#45301f')
        px(img, 7, 11, '#f1c9b6'); px(img, 8, 11, '#e0a898')
        return finish(img, '#2a1a10')
    if kind == 'drowned_corsair':
        shade_mask(img, m, tex.ramp('#6d8f80', 6, spread=0.5), base=0.5)
        # tricorn hat on top, red coat collar below, eye patch and a glowing eye
        hat = tex.ramp('#2c2427', 4, spread=0.3)
        for y in range(1, 5):
            for x in range(2, 14):
                if y == 4 or (y == 3 and 3 <= x <= 12) or (y <= 2 and 4 <= x <= 11 and m[y][x]):
                    px(img, x, y, hat[1 + (y < 3)])
        for x in range(2, 14):
            px(img, x, 4, '#dcaa3e' if x % 2 else '#b88a2a')
        px(img, 5, 6, '#141317'); px(img, 6, 6, '#141317'); px(img, 5, 7, '#141317')
        px(img, 10, 6, '#a8f4ea')
        for x in range(3, 13):
            for y in (12, 13, 14):
                if m[y][x]:
                    px(img, x, y, '#7d2228' if (x + y) % 3 else '#5e1a1e')
        px(img, 7, 12, '#dcaa3e'); px(img, 8, 13, '#dcaa3e')
        return finish(img, '#1c2622')
    raise ValueError(kind)


SPAWN_EGGS = ('crab', 'ember_crab', 'seagull', 'penguin', 'sea_otter', 'drowned_corsair')


def build():
    out = {}
    out['crab_claw'] = crab_claw(False)
    out['cooked_crab_claw'] = crab_claw(True)
    out['ember_shell'] = ember_shell()
    out['cutlass'] = cutlass()
    for k in SPAWN_EGGS:
        out[f'{k}_spawn_egg'] = spawn_egg(k)
    for name, img in out.items():
        tex.save(img, MOD, f'item/{name}')
    return out
