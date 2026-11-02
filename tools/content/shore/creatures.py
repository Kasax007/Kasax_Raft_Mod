"""Entity textures of the shore creatures. Box layouts mirror the Java models in
net.kasax.raft.content.shore.client.model (texOffs and box sizes must stay in sync)."""
from bclib import tex

from .skin import Box, new, put, fill_rect, material_box, shade, noise, rng, outline_face, tint_region, darken_region

MOD = 'shore'


# ============================================================================================== crab
CRAB_PALETTES = {
    'red': dict(shell='#c4462e', tip='#3a2422', belly='#ead2a6', spot='#7c2418', mouth='#7a3a2c', legs='#d0573a'),
    'blue': dict(shell='#3f76ad', tip='#d9552c', belly='#e9e3cf', spot='#24476e', mouth='#3d4a63', legs='#5b8fc0'),
    'sand': dict(shell='#d6b47c', tip='#f5eedc', belly='#f3e8c8', spot='#a8834e', mouth='#9a7a50', legs='#e2c592'),
    'ember': dict(shell='#3c383b', tip='#ff7c1e', belly='#2a2729', spot='#262325', mouth='#5a2a1c', legs='#4a4448'),
}

CARAPACE = Box(0, 0, 10, 4, 8)
DOME = Box(0, 12, 8, 1, 6)
EYE_STALK = Box(36, 0, 1, 3, 1)
EYEBALL = Box(40, 0, 2, 2, 2)
ARM = Box(0, 20, 2, 2, 4)
R_PALM = Box(0, 26, 3, 3, 4)
R_FINGER = Box(14, 26, 2, 1, 3)
R_PINCER = Box(14, 30, 2, 1, 3)
L_PALM = Box(24, 20, 2, 2, 3)
L_FINGER = Box(24, 25, 1, 1, 2)
L_PINCER = Box(30, 25, 1, 1, 2)
LEG_UP = Box(40, 12, 3, 1, 1)
LEG_LOW = Box(40, 14, 6, 1, 1)
SPIKES = [Box(48, 0, 1, 2, 1), Box(52, 0, 1, 3, 1), Box(48, 4, 1, 1, 1)]


def _tipped(img, box, S, T, seed):
    """Claw finger: shell colour at the base fading into the tip colour at the front."""
    material_box(img, box, S, seed)
    n = noise(seed + 5)
    f = box.faces()
    # top: bottom row = front
    def top(x, y, fx, fy, w, h):
        t = fy / max(1, h - 1)
        return tex.mix(shade(S, 0.75 + (n[y][x] - .5) * .2), T[3], min(1.0, t * 1.3))
    fill_rect(img, f['top'], top)
    def bottom(x, y, fx, fy, w, h):
        t = 1 - fy / max(1, h - 1)
        return tex.mix(shade(S, 0.35), T[1], min(1.0, t * 1.3))
    fill_rect(img, f['bottom'], bottom)
    def right(x, y, fx, fy, w, h):  # image right = front
        t = fx / max(1, w - 1)
        return tex.mix(shade(S, 0.55 - fy * 0.15), T[2], min(1.0, t * 1.4))
    fill_rect(img, f['right'], right)
    def left(x, y, fx, fy, w, h):  # image left = front
        t = 1 - fx / max(1, w - 1)
        return tex.mix(shade(S, 0.5 - fy * 0.15), T[2], min(1.0, t * 1.4))
    fill_rect(img, f['left'], left)
    fill_rect(img, f['front'], lambda x, y, fx, fy, w, h: T[2])
    fill_rect(img, f['back'], lambda x, y, fx, fy, w, h: shade(S, 0.4))


def crab(variant, seed=11):
    pal = CRAB_PALETTES[variant]
    ember = variant == 'ember'
    img = new()
    S = tex.ramp(pal['shell'], 6, spread=0.5 if not ember else 0.4, sat_shift=0.0)
    B = tex.ramp(pal['belly'], 5, spread=0.35)
    T = tex.ramp(pal['tip'], 5, spread=0.5)
    L = tex.ramp(pal['legs'], 5, spread=0.55)
    spot = tex.rgb(pal['spot'])
    n = noise(seed)

    # ---------------------------------------------------------------- carapace
    material_box(img, CARAPACE, S, seed)
    f = CARAPACE.faces()

    def top(x, y, fx, fy, w, h):
        dx, dy = (fx - 4.5) / 5.5, (fy - 3.8) / 4.6
        dome = 1 - (dx * dx + dy * dy)
        light = -dx * 0.12 - dy * 0.10
        v = 0.4 + dome * 0.26 + light + (n[y][x] - 0.5) * 0.16
        if fx in (0, 9) or fy == 0:
            v -= 0.18
        return shade(S, v)
    fill_rect(img, f['top'], top)
    tx, ty = f['top'][0], f['top'][1]
    # serrated front rim and the spine row of little bumps on the sides
    for fx in range(10):
        put(img, tx + fx, ty + 7, S[1] if fx % 2 == 0 else S[3])
    for fy in (2, 4, 6):
        put(img, tx, ty + fy, S[3])
        put(img, tx + 9, ty + fy, S[2])
    # markings (symmetric)
    if variant == 'red':
        for (sx, sy) in ((2, 2), (3, 4), (2, 5)):
            put(img, tx + sx, ty + sy, spot)
            put(img, tx + 9 - sx, ty + sy, spot)
        put(img, tx + 4, ty + 3, S[5]); put(img, tx + 5, ty + 3, S[5])
    elif variant == 'blue':
        for (sx, sy) in ((2, 3), (4, 2), (3, 5)):
            put(img, tx + sx, ty + sy, S[5])
            put(img, tx + 9 - sx, ty + sy, S[5])
        for fx in range(2, 8):
            put(img, tx + fx, ty + 6, tex.mix(S[3], spot, 0.4))
    elif variant == 'sand':
        r = rng(seed)
        for _ in range(9):
            sx, sy = r.randrange(1, 5), r.randrange(1, 7)
            put(img, tx + sx, ty + sy, spot)
            put(img, tx + 9 - sx, ty + sy, spot)
    # sides: lit rim, shell, belly lip at the bottom
    for name in ('right', 'left', 'back'):
        rx, ry, rw, rh = f[name]
        for fx in range(rw):
            put(img, rx + fx, ry, S[4])
            put(img, rx + fx, ry + 1, S[3] if (fx + seed) % 3 else S[4])
            put(img, rx + fx, ry + 2, S[2])
            put(img, rx + fx, ry + 3, B[2] if name != 'back' else B[1])
    # front: rim, mouth parts in the middle, small eye sockets
    rx, ry, rw, rh = f['front']
    mouth = tex.ramp(pal['mouth'], 4, spread=0.4)
    for fx in range(rw):
        put(img, rx + fx, ry, S[4] if fx % 2 else S[3])
        for fy in range(1, 4):
            if 3 <= fx <= 6:
                put(img, rx + fx, ry + fy, mouth[1 + (fx + fy) % 2] if fy < 3 else mouth[0])
            else:
                put(img, rx + fx, ry + fy, S[2] if fy < 3 else B[1])
    put(img, rx + 2, ry + 1, S[1]); put(img, rx + 7, ry + 1, S[1])
    # underside: cream plates with the abdomen flap
    bx, by, bw, bh = f['bottom']
    for fy in range(bh):
        for fx in range(bw):
            v = 0.55 + (n[by + fy][bx + fx] - 0.5) * 0.15
            c = shade(B, v)
            if fy in (2, 5) or fx in (0, bw - 1):
                c = B[1]
            if abs(fx - 4.5) < (fy - 1) * 0.5 and fy >= 2:
                c = B[3] if fy % 2 else B[2]
            put(img, bx + fx, by + fy, c)

    # ---------------------------------------------------------------- dome
    material_box(img, DOME, S, seed + 1, top=0.85, side=0.7)
    dx0, dy0, dw, dh = DOME.face('top')
    for fy in range(dh):
        for fx in range(dw):
            ddx, ddy = (fx - 3.5) / 4.0, (fy - 2.5) / 3.0
            v = 0.5 + (1 - ddx * ddx - ddy * ddy) * 0.25 - ddx * 0.08 - ddy * 0.08 + (n[dy0 + fy][dx0 + fx] - .5) * .15
            put(img, dx0 + fx, dy0 + fy, shade(S, v))
    if variant == 'red':
        put(img, dx0 + 2, dy0 + 2, spot); put(img, dx0 + 5, dy0 + 2, spot)
    if variant == 'sand':
        put(img, dx0 + 1, dy0 + 3, spot); put(img, dx0 + 6, dy0 + 1, spot)

    # ---------------------------------------------------------------- eyes
    material_box(img, EYE_STALK, S, seed + 2, top=0.8, side=0.65)
    eye = tex.ramp('#24212a' if not ember else '#5a2a10', 4, spread=0.3)
    material_box(img, EYEBALL, eye, seed + 3, top=0.5, side=0.4, mottling=0.05)
    for name in ('front', 'right', 'left'):
        rx, ry, rw, rh = EYEBALL.face(name)
        put(img, rx, ry, '#f4f2ea' if not ember else '#ffd36a')
    if ember:
        for name, rect in EYEBALL.faces().items():
            fill_rect(img, rect, lambda x, y, fx, fy, w, h: '#ff9a2a' if (fx + fy) % 2 == 0 else '#e0601a')

    # ---------------------------------------------------------------- claws
    material_box(img, ARM, S, seed + 4, top=0.7, side=0.5)
    for name in ('right', 'left'):
        rx, ry, rw, rh = ARM.face(name)
        put(img, rx + rw // 2, ry, S[4])
    for palm, seedo in ((R_PALM, 5), (L_PALM, 6)):
        material_box(img, palm, S, seed + seedo, top=0.8, side=0.58, grad=0.3)
        # knobbly ridge along the top
        px, py, pw, ph = palm.face('top')
        for fy in range(ph):
            put(img, px + pw // 2, py + fy, S[5] if fy % 2 == 0 else S[4])
        rx, ry, rw, rh = palm.face('front')
        fill_rect(img, (rx, ry, rw, rh), lambda x, y, fx, fy, w, h: tex.mix(S[2], T[2], 0.35))
    for finger, seedo in ((R_FINGER, 7), (R_PINCER, 8), (L_FINGER, 9), (L_PINCER, 10)):
        _tipped(img, finger, S, T, seed + seedo)
    # teeth on the inner (lower) edge of the fixed fingers, upper edge of the pincers
    for finger in (R_FINGER, L_FINGER):
        bx, by, bw, bh = finger.face('bottom')
        for fx in range(bw):
            for fy in range(bh):
                if (fx + fy) % 2 == 0:
                    put(img, bx + fx, by + fy, B[3])
    for finger in (R_PINCER, L_PINCER):
        bx, by, bw, bh = finger.face('top')
        for fy in range(bh):
            if fy % 2 == 1:
                put(img, bx, by + fy, B[3])

    # ---------------------------------------------------------------- legs
    material_box(img, LEG_UP, L, seed + 12, top=0.75, side=0.55)
    material_box(img, LEG_LOW, L, seed + 13, top=0.8, side=0.6)
    fl = LEG_LOW.faces()
    for name in ('top', 'front', 'bottom'):
        rx, ry, rw, rh = fl[name]
        put(img, rx + rw - 1, ry, T[1])
        put(img, rx + rw - 2, ry, tex.mix(L[2], T[1], 0.5))
        put(img, rx, ry, L[1])
    rx, ry, rw, rh = fl['back']
    put(img, rx, ry, T[1]); put(img, rx + 1, ry, tex.mix(L[2], T[1], 0.5))
    put(img, *fl['left'][:2], T[1])
    for name in ('top', 'front', 'back'):
        rx, ry, rw, rh = LEG_UP.face(name)
        put(img, rx + (rw - 1 if name != 'back' else 0), ry, L[4])

    glow = None
    if ember:
        glow = new()
        _ember_cracks(img, glow, S, seed)
    return img, glow


def _ember_cracks(img, glow, S, seed):
    """Molten cracks across the basalt shell, claws and spikes; the same pixels glow in the emissive layer."""
    hot = tex.rgb('#ffd36a')
    warm = tex.rgb('#ff8a1e')
    deep = tex.rgb('#b8401a')
    pts = set()
    r = rng(seed + 99)
    tx, ty, tw, th = CARAPACE.face('top')
    # main crack network on the carapace (symmetric-ish)
    for start in ((2, 1), (7, 1), (4, 4)):
        x, y = start
        for _ in range(7):
            pts.add((tx + x, ty + y))
            pts.add((tx + 9 - x, ty + y))
            x += r.choice((-1, 0, 1))
            y += r.choice((0, 1, 1))
            x = max(1, min(8, x)); y = max(1, min(6, y))
    for name in ('right', 'left', 'back', 'front'):
        rx, ry, rw, rh = CARAPACE.face(name)
        x = r.randrange(1, rw - 1)
        for fy in range(3):
            pts.add((rx + x, ry + fy))
            x = max(0, min(rw - 1, x + r.choice((-1, 0, 1))))
    dx, dy, dw, dh = DOME.face('top')
    for fx in range(1, dw - 1):
        if fx % 3 != 0:
            pts.add((dx + fx, dy + 2 + (fx % 2)))
    for p in pts:
        put(img, p[0], p[1], deep)
        put(glow, p[0], p[1], warm)
    for p in list(pts)[::3]:
        put(glow, p[0], p[1], hot)
        put(img, p[0], p[1], warm)
    # glowing claw tips and spike tips, eyes
    for box in (R_FINGER, R_PINCER, L_FINGER, L_PINCER):
        rx, ry, rw, rh = box.face('front')
        for fx in range(rw):
            put(glow, rx + fx, ry, warm)
        tx2, ty2, tw2, th2 = box.face('top')
        for fx in range(tw2):
            put(glow, tx2 + fx, ty2 + th2 - 1, deep)
    for box in SPIKES:
        material_box(img, box, tex.ramp('#4a4448', 5, spread=0.4), seed + 3)
        tx3, ty3, tw3, th3 = box.face('top')
        put(img, tx3, ty3, warm)
        put(glow, tx3, ty3, hot)
        for name in ('front', 'right', 'left', 'back'):
            rx, ry, rw, rh = box.face(name)
            put(img, rx, ry, warm)
            put(glow, rx, ry, warm)
    for name, rect in EYEBALL.faces().items():
        fill_rect(glow, rect, lambda x, y, fx, fy, w, h: '#ffcf5a' if (fx + fy) % 2 == 0 else '#ff8a1e')


# ============================================================================================== seagull
GULL_BODY = Box(0, 0, 5, 4, 8)
GULL_HEAD = Box(26, 0, 3, 3, 4)
GULL_BEAK = Box(40, 0, 1, 1, 3)
GULL_LBEAK = Box(40, 4, 1, 1, 2)
GULL_TAIL = Box(0, 12, 4, 1, 3)
GULL_WING = Box(0, 16, 5, 1, 4)
GULL_TIP = Box(0, 21, 6, 1, 3)
GULL_LEG = Box(26, 7, 1, 3, 1)
GULL_FOOT = Box(30, 7, 2, 1, 2)


def seagull(seed=21):
    img = new()
    W = tex.ramp('#f4f3ee', 5, spread=0.22, hue_shift=0.02)
    G = tex.ramp('#9aa4ae', 5, spread=0.4)
    K = tex.ramp('#2c2c33', 4, spread=0.25)
    Y = tex.ramp('#f0c030', 4, spread=0.35)
    P = tex.ramp('#e39a88', 4, spread=0.35)
    n = noise(seed)

    material_box(img, GULL_BODY, W, seed, top=0.8, side=0.75, bottom=0.55, grad=0.25, mottling=0.08)
    f = GULL_BODY.faces()
    # grey mantle on the back, fading down the sides
    fill_rect(img, f['top'], lambda x, y, fx, fy, w, h: shade(G, 0.62 + (n[y][x] - .5) * .2 - fx * 0.03 + (0.1 if fy == h - 1 else 0)))
    for name in ('right', 'left'):
        rx, ry, rw, rh = f[name]
        for fx in range(rw):
            put(img, rx + fx, ry, G[3])
            if fx < rw - 2:
                put(img, rx + fx, ry + 1, G[2] if name == 'right' else G[2])
    rx, ry, rw, rh = f['back']
    for fx in range(rw):
        put(img, rx + fx, ry, G[2])
    # soft breast shading on the front
    rx, ry, rw, rh = f['front']
    for fx in range(rw):
        put(img, rx + fx, ry + rh - 1, W[2])

    material_box(img, GULL_HEAD, W, seed + 1, top=0.85, side=0.8, bottom=0.6, grad=0.2, mottling=0.06)
    hf = GULL_HEAD.faces()
    eye = tex.rgb('#1e1c24')
    ring = tex.rgb('#e8c04a')
    rx, ry, rw, rh = hf['right']
    put(img, rx + rw - 2, ry + 1, eye); put(img, rx + rw - 3, ry + 1, ring)
    put(img, rx + rw - 1, ry + 2, W[2])
    rx, ry, rw, rh = hf['left']
    put(img, rx + 1, ry + 1, eye); put(img, rx + 2, ry + 1, ring)
    put(img, rx, ry + 2, W[2])
    # faint grey streak on the nape
    rx, ry, rw, rh = hf['back']
    for fx in range(rw):
        put(img, rx + fx, ry + rh - 1, G[3])

    material_box(img, GULL_BEAK, Y, seed + 2, top=0.8, side=0.6, bottom=0.4)
    bf = GULL_BEAK.faces()
    for name in ('top', 'right', 'left'):
        rx, ry, rw, rh = bf[name]
        tipx = rx + (rw - 1 if name == 'right' else 0) if name != 'top' else rx
        if name == 'top':
            put(img, rx, ry + rh - 1, Y[1])  # hooked tip
        else:
            put(img, tipx, ry, Y[1])
    material_box(img, GULL_LBEAK, Y, seed + 3, top=0.7, side=0.55, bottom=0.4)
    lf = GULL_LBEAK.faces()
    red = tex.rgb('#d2382a')
    put(img, lf['right'][0] + lf['right'][2] - 1, lf['right'][1], red)
    put(img, lf['left'][0], lf['left'][1], red)
    put(img, lf['front'][0], lf['front'][1], red)

    material_box(img, GULL_TAIL, W, seed + 4, top=0.82, side=0.7, bottom=0.6, mottling=0.05)
    tf = GULL_TAIL.faces()
    rx, ry, rw, rh = tf['top']
    for fx in range(rw):
        put(img, rx + fx, ry + rh - 1, G[3])

    # wings: grey above with a white trailing edge, pale below; black tips with white mirrors
    material_box(img, GULL_WING, G, seed + 5, top=0.6, side=0.5, bottom=0.5, mottling=0.12)
    wf = GULL_WING.faces()
    rx, ry, rw, rh = wf['top']
    for fx in range(rw):
        put(img, rx + fx, ry, W[3])           # trailing edge (back row)
        put(img, rx + fx, ry + rh - 1, G[4])  # leading edge highlight
    fill_rect(img, wf['bottom'], lambda x, y, fx, fy, w, h: shade(W, 0.55 + (n[y][x] - .5) * .1))
    fill_rect(img, wf['back'], lambda x, y, fx, fy, w, h: W[3])
    fill_rect(img, wf['front'], lambda x, y, fx, fy, w, h: G[4])

    material_box(img, GULL_TIP, G, seed + 6, top=0.58, side=0.5, bottom=0.5, mottling=0.1)
    tf = GULL_TIP.faces()
    rx, ry, rw, rh = tf['top']
    for fy in range(rh):
        for fx in range(rw):
            if fx >= 3:
                c = K[1 + (fx + fy) % 2] if fx < rw - 1 else K[0]
                if (fx, fy) in ((4, 1), (5, 0)):
                    c = W[3]
                put(img, rx + fx, ry + fy, c)
        put(img, rx + 2, ry + fy, G[2])
    for fx in range(3):
        put(img, rx + fx, ry, W[3])
    rx, ry, rw, rh = tf['bottom']
    for fy in range(rh):
        for fx in range(rw):
            put(img, rx + fx, ry + fy, K[2] if fx >= 3 else W[2])
        put(img, rx + 4, ry + 1, W[3])
    for name in ('front', 'back'):
        rx, ry, rw, rh = tf[name]
        for fx in range(rw):
            black = fx >= 3 if name == 'front' else fx <= 2
            put(img, rx + fx, ry, K[1] if black else (G[4] if name == 'front' else W[3]))
    put(img, tf['left'][0], tf['left'][1], K[0])

    material_box(img, GULL_LEG, P, seed + 7, top=0.7, side=0.55)
    material_box(img, GULL_FOOT, P, seed + 8, top=0.75, side=0.55)
    rx, ry, rw, rh = GULL_FOOT.face('top')
    put(img, rx, ry, P[1]); put(img, rx + 1, ry + 1, P[3])
    return img


# ============================================================================================== penguin
PEN_BODY = Box(0, 0, 7, 9, 6)
PEN_HEAD = Box(0, 15, 6, 5, 5)
PEN_BEAK = Box(22, 15, 2, 1, 3)
PEN_FLIPPER = Box(26, 0, 1, 7, 3)
PEN_FOOT = Box(34, 0, 2, 2, 3)
PEN_TAIL = Box(34, 5, 3, 1, 2)


def penguin(seed=31):
    img = new()
    K = tex.ramp('#2a2d39', 5, spread=0.3, hue_shift=0.03)
    W = tex.ramp('#f3f1ea', 5, spread=0.22)
    O = tex.ramp('#f39a2c', 4, spread=0.35)
    Yl = tex.rgb('#f7c84a')
    F = tex.ramp('#e9875c', 4, spread=0.35)
    n = noise(seed)

    material_box(img, PEN_BODY, K, seed, top=0.75, side=0.55, bottom=0.3, grad=0.25, mottling=0.12)
    f = PEN_BODY.faces()
    # white belly on the front, wrapping a little around the sides; orange blush on the upper chest
    rx, ry, rw, rh = f['front']
    for fy in range(rh):
        for fx in range(rw):
            edge = fx == 0 or fx == rw - 1
            if fy == 0 or (edge and fy < rh - 2):
                c = K[2]
            else:
                c = shade(W, 0.78 - fy * 0.035 + (n[ry + fy][rx + fx] - .5) * .08)
            put(img, rx + fx, ry + fy, c)
    for name, white_cols in (('right', range(4, 6)), ('left', range(0, 2))):
        rx, ry, rw, rh = f[name]
        for fy in range(2, rh):
            for fx in white_cols:
                if not (fy < 3 and fx in (3, 2)):
                    put(img, rx + fx, ry + fy, shade(W, 0.62 - fy * 0.03))
    rx, ry, rw, rh = f['bottom']
    fill_rect(img, (rx, ry, rw, rh), lambda x, y, fx, fy, w, h: W[1] if fy < 3 else K[1])
    # feather scallops on the back
    rx, ry, rw, rh = f['back']
    for fy in range(1, rh, 3):
        for fx in range((fy // 3) % 2, rw, 2):
            put(img, rx + fx, ry + fy, K[3])

    material_box(img, PEN_HEAD, K, seed + 1, top=0.75, side=0.55, bottom=0.4, grad=0.2, mottling=0.1)
    hf = PEN_HEAD.faces()
    eye = tex.rgb('#141218')
    hl = tex.rgb('#f2f2f2')
    # gentoo look: white patches over the eyes joined by a white band over the crown, bright beak
    for name in ('right', 'left'):
        rx, ry, rw, rh = hf[name]
        front_x = rw - 1 if name == 'right' else 0
        step = -1 if name == 'right' else 1
        for (ox, oy) in ((0, 0), (1, 0), (2, 0), (0, 1), (2, 1), (3, 0), (1, 2)):
            put(img, rx + front_x + step * ox, ry + oy, shade(W, 0.75 - oy * 0.08))
        put(img, rx + front_x + step * 1, ry + 1, eye)
        put(img, rx + front_x, ry + 2, K[3])
    rx, ry, rw, rh = hf['top']
    for fx in range(rw):
        put(img, rx + fx, ry + rh - 2, shade(W, 0.8))
        if fx in (0, rw - 1):
            put(img, rx + fx, ry + rh - 1, shade(W, 0.75))
    rx, ry, rw, rh = hf['front']
    put(img, rx, ry, shade(W, 0.7)); put(img, rx + rw - 1, ry, shade(W, 0.7))
    put(img, rx, ry + 1, eye); put(img, rx + rw - 1, ry + 1, eye)
    put(img, rx + 1, ry + 1, K[3]); put(img, rx + rw - 2, ry + 1, K[3])
    for fx in range(rw):
        put(img, rx + fx, ry + rh - 1, K[1])
    rx, ry, rw, rh = hf['bottom']
    fill_rect(img, (rx, ry, rw, rh), lambda x, y, fx, fy, w, h: W[2] if fy < 2 else K[1])

    Bk = tex.ramp('#e8552a', 4, spread=0.4)
    material_box(img, PEN_BEAK, Bk, seed + 2, top=0.7, side=0.55, bottom=0.4, mottling=0.05)
    bf = PEN_BEAK.faces()
    rx, ry, rw, rh = bf['top']
    for fx in range(rw):
        put(img, rx + fx, ry + rh - 1, K[2])   # dark hooked tip
    put(img, *bf['front'][:2], K[2]); put(img, bf['front'][0] + 1, bf['front'][1], K[2])
    for name in ('right', 'left'):
        rx, ry, rw, rh = bf[name]
        put(img, rx + (rw - 1 if name == 'right' else 0), ry, K[2])

    material_box(img, PEN_FLIPPER, K, seed + 3, top=0.65, side=0.5, bottom=0.35, grad=0.3, mottling=0.1)
    ff = PEN_FLIPPER.faces()
    # inner side (towards the body) is white with a dark rim
    rx, ry, rw, rh = ff['right']
    for fy in range(rh):
        for fx in range(rw):
            inner = 0 < fx < rw - 1 and 0 < fy < rh - 1
            put(img, rx + fx, ry + fy, shade(W, 0.6 - fy * 0.03) if inner else K[2])
    rx, ry, rw, rh = ff['left']
    for fy in range(rh):
        put(img, rx + rw - 1, ry + fy, K[3])

    material_box(img, PEN_FOOT, F, seed + 4, top=0.7, side=0.5, bottom=0.35)
    rx, ry, rw, rh = PEN_FOOT.face('top')
    for fy in range(rh):
        put(img, rx + (fy % 2), ry + fy, F[1])
    material_box(img, PEN_TAIL, K, seed + 5, top=0.6, side=0.45)
    return img


def penguin_chick(seed=37):
    """Grey fluffy chick: dark cap, white face mask, pale fluff."""
    img = new()
    Gr = tex.ramp('#9fa3aa', 5, spread=0.32)
    K = tex.ramp('#2c2e36', 4, spread=0.25)
    W = tex.ramp('#eeece6', 4, spread=0.2)
    n = noise(seed, cell=2)
    material_box(img, PEN_BODY, Gr, seed, top=0.72, side=0.55, bottom=0.35, grad=0.25, mottling=0.3)
    f = PEN_BODY.faces()
    rx, ry, rw, rh = f['front']
    for fy in range(rh):
        for fx in range(rw):
            put(img, rx + fx, ry + fy, shade(Gr, 0.72 - fy * 0.02 + (n[ry + fy][rx + fx] - .5) * .35))
    # fluffy tufts sticking out on the outline of every face
    for name, rect in f.items():
        rx, ry, rw, rh = rect
        for fx in range(0, rw, 2):
            put(img, rx + fx, ry, Gr[4])
    material_box(img, PEN_HEAD, K, seed + 1, top=0.7, side=0.5, bottom=0.35, mottling=0.12)
    hf = PEN_HEAD.faces()
    # white mask around the eyes on the sides and the face
    for name in ('right', 'left', 'front'):
        rx, ry, rw, rh = hf[name]
        for fy in range(1, 5):
            for fx in range(rw):
                if name == 'front':
                    mask = fx in (0, rw - 1) and fy >= 1
                else:
                    front_x = rw - 1 if name == 'right' else 0
                    mask = abs(fx - front_x) <= 2 and fy >= 1
                if mask:
                    put(img, rx + fx, ry + fy, shade(W, 0.7 - fy * 0.05))
        if name != 'front':
            front_x = rw - 1 if name == 'right' else 0
            step = -1 if name == 'right' else 1
            put(img, rx + front_x + step, ry + 2, '#141218')
            put(img, rx + front_x + step * 2, ry + 2, K[1])
    rx, ry, rw, rh = hf['bottom']
    fill_rect(img, (rx, ry, rw, rh), lambda x, y, fx, fy, w, h: W[2])
    material_box(img, PEN_BEAK, K, seed + 2, top=0.55, side=0.4)
    material_box(img, PEN_FLIPPER, Gr, seed + 3, top=0.65, side=0.5, mottling=0.3)
    material_box(img, PEN_FOOT, tex.ramp('#7d7a80', 4, spread=0.3), seed + 4)
    material_box(img, PEN_TAIL, Gr, seed + 5)
    return img


# ============================================================================================== sea otter
OT_BODY = Box(0, 0, 6, 5, 10)
OT_HEAD = Box(0, 15, 5, 4, 4)
OT_SNOUT = Box(18, 15, 3, 2, 2)
OT_EAR = Box(28, 15, 1, 1, 1)
OT_ARM = Box(32, 0, 2, 3, 2)
OT_LEG = Box(40, 0, 2, 3, 3)
OT_TAIL = Box(32, 6, 3, 2, 7)
OT_STONE = Box(52, 0, 2, 2, 2)
OT_SHELL = Box(52, 4, 3, 1, 3)


def sea_otter(seed=41):
    img = new()
    Fu = tex.ramp('#6f4a30', 6, spread=0.5)
    Fa = tex.ramp('#c9ad88', 5, spread=0.4)
    Pd = tex.ramp('#45301f', 4, spread=0.3)
    n = noise(seed, cell=2)

    material_box(img, OT_BODY, Fu, seed, top=0.6, side=0.5, bottom=0.45, grad=0.2, mottling=0.22)
    f = OT_BODY.faces()
    # fur streaks along the back, slightly lighter belly
    rx, ry, rw, rh = f['top']
    for fy in range(rh):
        for fx in range(rw):
            v = 0.55 + (n[ry + fy][rx + fx] - .5) * .35 - abs(fx - 2.5) * 0.04
            if (fx + fy * 2) % 5 == 0:
                v += 0.12
            put(img, rx + fx, ry + fy, shade(Fu, v))
    rx, ry, rw, rh = f['bottom']
    fill_rect(img, (rx, ry, rw, rh), lambda x, y, fx, fy, w, h: shade(Fu, 0.68 + (n[y][x] - .5) * .25))
    # grizzled cream chest under the chin
    rx, ry, rw, rh = f['front']
    fill_rect(img, (rx, ry, rw, rh), lambda x, y, fx, fy, w, h: shade(Fa, 0.6 - fy * 0.08 + (n[y][x] - .5) * .2) if fy < 3 else shade(Fu, 0.6))

    material_box(img, OT_HEAD, Fa, seed + 1, top=0.62, side=0.55, bottom=0.6, grad=0.2, mottling=0.2)
    hf = OT_HEAD.faces()
    # darker brown crown and nape fading into the pale face
    rx, ry, rw, rh = hf['top']
    fill_rect(img, (rx, ry, rw, rh), lambda x, y, fx, fy, w, h: tex.mix(shade(Fu, 0.6), shade(Fa, 0.65), fy / (h - 1) * 0.8))
    rx, ry, rw, rh = hf['back']
    fill_rect(img, (rx, ry, rw, rh), lambda x, y, fx, fy, w, h: shade(Fu, 0.55 - fy * 0.05))
    eye = tex.rgb('#171214')
    for name in ('right', 'left'):
        rx, ry, rw, rh = hf[name]
        back_x = 0 if name == 'right' else rw - 1
        for fy in range(rh):
            put(img, rx + back_x, ry + fy, shade(Fu, 0.5))
            put(img, rx + (1 if name == 'right' else rw - 2), ry + fy, tex.mix(shade(Fu, 0.55), Fa[2], 0.5))
    rx, ry, rw, rh = hf['front']
    put(img, rx + 1, ry + 1, eye); put(img, rx + 3, ry + 1, eye)
    put(img, rx + 1, ry, Fa[1]); put(img, rx + 3, ry, Fa[1])
    put(img, rx + 1, ry + 2, Fa[4]); put(img, rx + 3, ry + 2, Fa[4])

    material_box(img, OT_SNOUT, Fa, seed + 2, top=0.75, side=0.65, bottom=0.55, mottling=0.1)
    sf = OT_SNOUT.faces()
    rx, ry, rw, rh = sf['front']
    put(img, rx + 1, ry, '#2a1c18')
    put(img, rx, ry, Fa[3]); put(img, rx + 2, ry, Fa[3])
    put(img, rx + 1, ry + 1, Fa[1])
    whisker = tex.rgb('#f6efe2')
    for name in ('right', 'left'):
        rx, ry, rw, rh = sf[name]
        put(img, rx + (rw - 1 if name == 'right' else 0), ry + 1, whisker)
        put(img, rx + (0 if name == 'right' else rw - 1), ry, whisker)
    rx, ry, rw, rh = sf['top']
    put(img, rx + 1, ry + rh - 1, '#3a2620')

    material_box(img, OT_EAR, Pd, seed + 3, top=0.6, side=0.5)
    material_box(img, OT_ARM, Pd, seed + 4, top=0.6, side=0.5, grad=0.3)
    material_box(img, OT_LEG, Pd, seed + 5, top=0.6, side=0.5, grad=0.3)
    rx, ry, rw, rh = OT_LEG.face('bottom')
    for fx in range(rw):
        for fy in range(rh):
            put(img, rx + fx, ry + fy, Pd[0] if (fx + fy) % 2 else Pd[1])
    material_box(img, OT_TAIL, Fu, seed + 6, top=0.55, side=0.45, bottom=0.4, mottling=0.2)
    # props
    material_box(img, OT_STONE, tex.ramp('#8c8b8f', 5, spread=0.45), seed + 7, top=0.75, side=0.5, mottling=0.25)
    Sh = tex.ramp('#f1c9b6', 5, spread=0.35)
    material_box(img, OT_SHELL, Sh, seed + 8, top=0.8, side=0.5)
    rx, ry, rw, rh = OT_SHELL.face('top')
    for fy in range(rh):
        for fx in range(rw):
            put(img, rx + fx, ry + fy, Sh[3] if fx % 2 == 0 else Sh[2])
    put(img, rx + 1, ry + rh - 1, Sh[1])
    return img


# ============================================================================================== drowned corsair
HEAD = Box(0, 0, 8, 8, 8)
HAT = Box(32, 0, 8, 8, 8)
BODY = Box(16, 16, 8, 12, 4)
R_ARM = Box(40, 16, 4, 12, 4)
L_ARM = Box(32, 48, 4, 12, 4)
R_LEG = Box(0, 16, 4, 12, 4)
L_LEG = Box(16, 48, 4, 12, 4)


def _rows(img, box, fn, faces=('front', 'right', 'left', 'back')):
    """fn(y, x, face, fx, fw) -> colour or None for the side faces of a limb, row by row."""
    for name in faces:
        rx, ry, rw, rh = box.face(name)
        for fy in range(rh):
            for fx in range(rw):
                c = fn(fy, fx, name, rw)
                if c is not None:
                    put(img, rx + fx, ry + fy, c)


def corsair(seed=51):
    img = new()
    Sk = tex.ramp('#6d8f80', 6, spread=0.45, hue_shift=0.03)
    Sh = tex.ramp('#d6ccb2', 5, spread=0.3)
    Rd = tex.ramp('#8e2c2c', 4, spread=0.35)
    Tr = tex.ramp('#3d3650', 5, spread=0.35)
    Bt = tex.ramp('#3c2a1d', 5, spread=0.4)
    Be = tex.ramp('#2a2424', 4, spread=0.3)
    Au = tex.ramp('#e2b13e', 4, spread=0.4)
    Kp = tex.ramp('#4f7a3a', 4, spread=0.35)
    glow = tex.rgb('#a8f4ea')
    n = noise(seed, cell=2)

    # ---------------------------------------------------------------- head
    material_box(img, HEAD, Sk, seed, top=0.55, side=0.5, bottom=0.35, grad=0.2, mottling=0.2)
    hf = HEAD.faces()
    # dark hair under the hat
    for name in ('top',):
        fill_rect(img, hf[name], lambda x, y, fx, fy, w, h: shade(Be, 0.5 + (n[y][x] - .5) * .4))
    for name in ('right', 'left', 'back'):
        rx, ry, rw, rh = hf[name]
        for fx in range(rw):
            for fy in range(3 if name != 'back' else 6):
                if fy < 2 or (fx + fy) % 3 != 0:
                    put(img, rx + fx, ry + fy, shade(Be, 0.45 + (n[ry + fy][rx + fx] - .5) * .4))
    rx, ry, rw, rh = hf['front']
    # brow and hairline
    for fx in range(rw):
        put(img, rx + fx, ry, Be[2])
        put(img, rx + fx, ry + 1, Be[1] if fx in (0, 1, 6, 7) else Sk[3])
    # eyes: left one glows pale cyan, right one under an eye patch
    put(img, rx + 5, ry + 3, glow); put(img, rx + 6, ry + 3, Sk[1])
    put(img, rx + 5, ry + 2, Sk[1])
    patch = tex.rgb('#141317')
    for (px, py) in ((1, 3), (2, 3), (1, 4), (2, 4)):
        put(img, rx + px, ry + py, patch)
    for fx in range(rw):  # strap across the forehead
        y = 2 if fx >= 3 else 3
        if fx not in (1, 2):
            put(img, rx + fx, ry + (2 if fx > 2 else 2), tex.mix(patch, Sk[2], 0.2) if fx < 6 else Sk[2])
    put(img, rx + 3, ry + 2, patch); put(img, rx + 4, ry + 2, Sk[3]); put(img, rx + 5, ry + 2, Sk[2])
    put(img, rx + 4, ry + 4, Sk[2])  # nose shadow
    # scraggly beard with kelp
    for fy in range(5, 8):
        for fx in range(1, 7):
            if fy == 5 and fx in (3, 4):
                put(img, rx + fx, ry + fy, '#3a1e1e')  # mouth
            elif (fx + fy) % 4 != 0 or fy > 5:
                put(img, rx + fx, ry + fy, shade(Be, 0.4 + (n[ry + fy][rx + fx] - .5) * .5))
    put(img, rx + 2, ry + 7, Kp[2]); put(img, rx + 5, ry + 6, Kp[1])
    # gold earring on the right side
    rx2, ry2, rw2, rh2 = hf['right']
    put(img, rx2 + 4, ry2 + 5, Au[3]); put(img, rx2 + 4, ry2 + 6, Au[2])
    # overlay: a few kelp strands hanging from the hair (rest transparent)
    hof = HAT.faces()
    for name in ('right', 'left', 'back'):
        rx, ry, rw, rh = hof[name]
        r = rng(seed + len(name))
        for _ in range(2):
            x = r.randrange(0, rw)
            for fy in range(r.randrange(3, 7)):
                put(img, rx + x, ry + 1 + fy, Kp[(fy + x) % 3])

    # ---------------------------------------------------------------- body: striped shirt, rib cage, belt
    material_box(img, BODY, Sh, seed + 1, top=0.6, side=0.55, bottom=0.4, grad=0.15, mottling=0.15)
    bf = BODY.faces()

    def shirt(fy, fx, face, fw):
        if fy >= 8:
            return None
        stripe = fy % 3 == 1
        return Rd[1 + (fx % 2 == 0)] if stripe else shade(Sh, 0.62 - fy * 0.02 + (n[16 + fy][fx + 20] - .5) * .2)
    _rows(img, BODY, shirt)
    # torn hole showing ribs on the front
    rx, ry, rw, rh = bf['front']
    for (px, py) in ((4, 3), (5, 3), (4, 4), (5, 4), (6, 4), (5, 5), (4, 5), (5, 6)):
        put(img, rx + px, ry + py, Sk[1] if (px + py) % 2 else Sk[3])
    put(img, rx + 3, ry + 4, Sh[0]); put(img, rx + 7, ry + 5, Sh[0])
    # collar open at the neck
    put(img, rx + 3, ry, Sk[2]); put(img, rx + 4, ry, Sk[2]); put(img, rx + 3, ry + 1, Sk[1])
    # belt and buckle
    def belt(fy, fx, face, fw):
        if fy in (8, 9):
            return Be[1 + (fy == 8)]
        if fy >= 10:
            return shade(Tr, 0.55 - (fy - 10) * 0.1)
        return None
    _rows(img, BODY, belt)
    put(img, rx + 3, ry + 8, Au[3]); put(img, rx + 4, ry + 8, Au[2]); put(img, rx + 3, ry + 9, Au[1]); put(img, rx + 4, ry + 9, Au[3])
    tint_region(img, bf['bottom'], Tr[1], 0.8)

    # ---------------------------------------------------------------- arms: torn sleeves, rotten forearms
    for arm, s in ((R_ARM, 2), (L_ARM, 3)):
        material_box(img, arm, Sk, seed + s, top=0.6, side=0.5, bottom=0.35, mottling=0.22)

        def sleeve(fy, fx, face, fw, s=s):
            if fy < 5:
                return Rd[1 + (fx % 2 == 0)] if fy % 3 == 1 else shade(Sh, 0.6 - fy * 0.03)
            if fy == 5:
                return Sh[1] if (fx + s) % 2 else None
            if fy >= 10:
                return Sk[1] if fy == 11 else Sk[2]
            return None
        _rows(img, arm, sleeve)
        tx, ty, tw, th = arm.face('top')
        fill_rect(img, (tx, ty, tw, th), lambda x, y, fx, fy, w, h: shade(Sh, 0.7))
        # a bit of exposed bone at the wrist
        rx, ry, rw, rh = arm.face('front')
        put(img, rx + 1, ry + 8, '#d8d2bf'); put(img, rx + 2, ry + 9, '#cfc8b2')

    # ---------------------------------------------------------------- legs: torn trousers, boots
    for leg, s in ((R_LEG, 4), (L_LEG, 5)):
        material_box(img, leg, Tr, seed + s, top=0.55, side=0.5, bottom=0.3, mottling=0.2)

        def boots(fy, fx, face, fw, s=s):
            if fy >= 7:
                return Bt[3] if fy == 7 else shade(Bt, 0.45 - (fy - 8) * 0.06 + (0.15 if fx == 1 else 0))
            if fy == 6 and (fx + s) % 2 == 0:
                return Sk[2]
            if fy == 5 and (fx + s) % 3 == 0:
                return Sk[2]
            return None
        _rows(img, leg, boots)
        rx, ry, rw, rh = leg.face('front')
        put(img, rx + 1, ry + 7, Au[2])  # boot buckle
        fill_rect(img, leg.face('bottom'), lambda x, y, fx, fy, w, h: Bt[0])
    return img


def corsair_coat(seed=61):
    """Outer layer: ragged long red coat with gold trim and brass buttons, open at the front."""
    img = new()
    C = tex.ramp('#7d2228', 6, spread=0.45)
    Au = tex.ramp('#d8a63c', 4, spread=0.4)
    n = noise(seed, cell=2)

    def coat_face(rect, open_front=False, hem_rows=None, kind='body'):
        rx, ry, rw, rh = rect
        r = rng(seed + rx * 7 + ry)
        for fy in range(rh):
            for fx in range(rw):
                if open_front and 2 < fx < rw - 3:
                    continue
                v = 0.5 - fy * 0.015 + (n[ry + fy][rx + fx] - .5) * .25
                c = shade(C, v)
                if open_front and fx in (2, rw - 3):
                    c = Au[2 if fy % 3 else 3]
                put(img, rx + fx, ry + fy, c)
        if hem_rows:
            # ragged torn hem
            for fx in range(rw):
                depth = r.randrange(0, hem_rows)
                for k in range(depth):
                    put(img, rx + fx, ry + rh - 1 - k, (0, 0, 0, 0))
                put(img, rx + fx, ry + rh - 1 - depth, Au[1] if (fx % 3 == 0 and kind == 'body') else C[1])

    # body (front open over the shirt)
    bf = BODY.faces()
    coat_face(bf['front'], open_front=True)
    for name in ('right', 'left', 'back'):
        coat_face(bf[name])
    coat_face(bf['top'])
    rx, ry, rw, rh = bf['front']
    for fy in (3, 6, 9):
        put(img, rx + 1, ry + fy, Au[3]); put(img, rx + rw - 2, ry + fy, Au[3])
    # collar
    for fx in range(rw):
        put(img, rx + fx, ry, Au[1] if fx in (0, rw - 1) else C[4] if 2 < fx < rw - 3 else C[3])
    # sleeves with gold cuffs, frayed below the elbow
    for arm in (R_ARM, L_ARM):
        af = arm.faces()
        for name in ('front', 'right', 'left', 'back'):
            rx, ry, rw, rh = af[name]
            for fy in range(8):
                for fx in range(rw):
                    put(img, rx + fx, ry + fy, shade(C, 0.52 - fy * 0.02 + (n[ry + fy][rx + fx] - .5) * .25))
            for fx in range(rw):
                put(img, rx + fx, ry + 7, Au[2] if fx % 2 else Au[1])
                if (fx + rx) % 3 == 0:
                    put(img, rx + fx, ry + 8, C[1])
        coat_face(af['top'])
    # coat tails over the upper legs (back and sides only, torn)
    for leg in (R_LEG, L_LEG):
        lf = leg.faces()
        for name in ('right', 'left', 'back'):
            rx, ry, rw, rh = lf[name]
            coat_face((rx, ry, rw, 7), hem_rows=3)
    return img


def corsair_hat(seed=71):
    img = new(64, 32)
    Fe = tex.ramp('#2c2427', 5, spread=0.32)
    Au = tex.ramp('#dcaa3e', 4, spread=0.4)
    crown = Box(0, 0, 7, 3, 7)
    back = Box(0, 10, 12, 4, 1)
    front = Box(0, 15, 14, 4, 1)
    feather = Box(30, 0, 1, 6, 3)
    material_box(img, crown, Fe, seed, top=0.62, side=0.5, bottom=0.25, mottling=0.15)
    cf = crown.faces()
    for name in ('front', 'right', 'left', 'back'):
        rx, ry, rw, rh = cf[name]
        for fx in range(rw):
            put(img, rx + fx, ry + rh - 1, Au[1] if fx % 2 else Au[2])  # gold hat band
    tx, ty, tw, th = cf['top']
    put(img, tx + 3, ty + 3, Fe[1]); put(img, tx + 2, ty + 3, Fe[2])  # dent in the crown
    for wall in (back, front):
        material_box(img, wall, Fe, seed + wall.w, top=0.6, side=0.5, bottom=0.3, mottling=0.15)
        wf = wall.faces()
        for name in ('front', 'back'):
            rx, ry, rw, rh = wf[name]
            for fx in range(rw):
                put(img, rx + fx, ry, Au[3] if fx % 3 else Au[2])   # gold edging on the upturned brim
                put(img, rx + fx, ry + rh - 1, Fe[1])
        fill_rect(img, wf['top'], lambda x, y, fx, fy, w, h: Au[3] if fx % 2 else Au[2])
    # a small skull badge in the middle of the front walls
    rx, ry, rw, rh = front.face('front')
    for (px, py) in ((12, 1), (13, 1), (12, 2), (13, 2)):
        put(img, rx + px, ry + py, '#e8e2d0')
    put(img, rx + 12, ry + 1, '#3a3236')
    # white feather with grey quill
    Wf = tex.ramp('#efeae0', 4, spread=0.25)
    material_box(img, feather, Wf, seed + 9, top=0.7, side=0.6, mottling=0.1)
    for name in ('right', 'left'):
        rx, ry, rw, rh = feather.face(name)
        for fy in range(rh):
            put(img, rx + 1, ry + fy, '#a39a8c')
            if fy % 2 == 0:
                put(img, rx + (0 if name == 'right' else 2), ry + fy, Wf[1])
    return img


def build(preview_dir=None):
    out = {}
    for v in ('red', 'blue', 'sand'):
        img, _ = crab(v)
        tex.save(img, MOD, f'entity/crab/crab_{v}')
        out[f'crab_{v}'] = img
    img, glow = crab('ember')
    tex.save(img, MOD, 'entity/crab/ember_crab')
    tex.save(glow, MOD, 'entity/crab/ember_crab_glow')
    out['ember_crab'] = img
    out['ember_crab_glow'] = glow
    out['seagull'] = seagull()
    tex.save(out['seagull'], MOD, 'entity/seagull')
    out['penguin'] = penguin()
    tex.save(out['penguin'], MOD, 'entity/penguin/penguin')
    out['penguin_chick'] = penguin_chick()
    tex.save(out['penguin_chick'], MOD, 'entity/penguin/penguin_chick')
    out['sea_otter'] = sea_otter()
    tex.save(out['sea_otter'], MOD, 'entity/sea_otter')
    out['drowned_corsair'] = corsair()
    tex.save(out['drowned_corsair'], MOD, 'entity/drowned_corsair/drowned_corsair')
    out['drowned_corsair_coat'] = corsair_coat()
    tex.save(out['drowned_corsair_coat'], MOD, 'entity/drowned_corsair/drowned_corsair_coat')
    out['drowned_corsair_hat'] = corsair_hat()
    tex.save(out['drowned_corsair_hat'], MOD, 'entity/drowned_corsair/drowned_corsair_hat')
    return out
