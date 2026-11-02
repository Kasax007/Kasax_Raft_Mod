"""Item sprites of the marine module: creature drops, eel food and spawn eggs in the 26.x egg style
(a creature-coloured egg sitting in a little puddle of water)."""
import math

from bclib import tex

M = 'marine'

# the water puddle every vanilla water-mob egg sits in (outline, deep, mid, light, foam)
W = [tex.rgb(c) for c in ('#2f457a', '#355a8b', '#3e76a3', '#4b98c4', '#63c9ff')]
PUDDLE = {
    12: {1: 0, 2: 1, 13: 0, 14: 0},
    13: {1: 0, 2: 1, 3: 2, 4: 3, 11: 2, 12: 1, 13: 1, 14: 0},
    14: {2: 0, 3: 1, 4: 2, 5: 4, 6: 4, 7: 3, 8: 4, 9: 3, 10: 2, 11: 1, 12: 1, 13: 0},
    15: {x: 0 for x in range(3, 13)},
}


def _egg_mask(top=1, bottom=13, rx=5.6):
    """Pixels of an egg (pointier at the top) between the rows top..bottom."""
    cy = (top + bottom) / 2.0 + 0.8
    ry = (bottom - top) / 2.0 + 0.6
    mask = set()
    for y in range(top, bottom + 1):
        t = (y + 0.5 - cy) / ry
        if abs(t) > 1:
            continue
        w = rx * math.sqrt(max(0.0, 1 - t * t)) * (1 + 0.12 * t)
        for x in range(16):
            if abs(x + 0.5 - 8.0) <= w + 0.15:
                mask.add((x, y))
    return mask


def egg(ramp, top=1, bottom=13, rx=5.6):
    """Shaded egg: light from the top-left, dark rim. ramp: 5 shades dark->light. Returns (img, mask)."""
    img = tex.new()
    mask = _egg_mask(top, bottom, rx)
    cx, cy = 8.0, (top + bottom) / 2.0 + 0.5
    for (x, y) in mask:
        edge = any((x + dx, y + dy) not in mask for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        if edge:
            tex.px(img, x, y, tex.darker(ramp[0], 0.85))
            continue
        nx, ny = (x + 0.5 - cx) / 6.0, (y + 0.5 - cy) / 7.0
        light = -nx * 0.6 - ny * 0.8
        k = 2.2 + light * 2.2
        tex.px(img, x, y, ramp[max(1, min(4, int(round(k))))])
    return img, mask


def puddle(img):
    for y, row in PUDDLE.items():
        for x, k in row.items():
            tex.px(img, x, y, W[k])
    return img


def put(img, pixels, colour):
    for (x, y) in pixels:
        tex.px(img, x, y, colour)


# ------------------------------------------------------------------------------------------------- spawn eggs
def egg_hammerhead():
    body = tex.ramp('#8c8270', 5, 0.6, 0.05)
    belly = tex.ramp('#ece9df', 3, 0.15)
    img, mask = egg(body, top=2, rx=5.0)
    out = tex.darker(body[0], 0.8)
    for (x, y) in mask:
        if y >= 10 and img.getpixel((x, y)) != tex.darker(body[0], 0.85):
            tex.px(img, x, y, belly[1 if x > 8 else 2])
    # the hammer: flat lobes reaching out on both sides, an eye at each tip
    for x in list(range(0, 4)) + list(range(12, 16)):
        tex.px(img, x, 4, out)
        tex.px(img, x, 5, body[3] if x in (2, 3, 12, 13) else body[2])
        tex.px(img, x, 6, body[1])
        tex.px(img, x, 7, out)
    put(img, [(0, 5), (15, 5), (0, 6), (15, 6)], out)
    put(img, [(1, 5), (14, 5)], belly[2])
    put(img, [(1, 6), (14, 6)], '#121318')
    for x in range(4, 12):
        tex.px(img, x, 7, body[1])                  # the hammer's edge across the face
    put(img, [(6, 9), (7, 9), (8, 9), (9, 9)], out)  # mouth
    # dorsal fin on top
    put(img, [(8, 0), (8, 1), (9, 1)], body[2])
    put(img, [(7, 0), (7, 1), (10, 1), (9, 0)], out)
    return puddle(img)


def egg_whale():
    body = tex.ramp('#45506a', 5, 0.5, 0.04)
    belly = tex.ramp('#e3e8ec', 3, 0.2)
    img, mask = egg(body, top=4, rx=6.0)
    for (x, y) in mask:
        if y >= 10 and img.getpixel((x, y)) != tex.darker(body[0], 0.85):
            tex.px(img, x, y, belly[2] if x % 2 else belly[1])     # throat grooves
    put(img, [(4, 9)], '#10131a')                                  # eye
    put(img, [(5, 9), (6, 9)], body[0])                            # mouth line
    put(img, [(9, 5), (10, 6), (6, 6)], body[3])                   # knobs
    # the spout above the blowhole
    put(img, [(8, 3), (8, 2), (7, 1), (9, 1), (6, 0), (10, 0)], '#cfefff')
    put(img, [(8, 1), (7, 0), (9, 0)], '#ffffff')
    return puddle(img)


def egg_manta():
    body = tex.ramp('#30353f', 5, 0.42, 0.03)
    white = tex.ramp('#e8ecec', 3, 0.2)
    img, mask = egg(body, top=2, rx=5.0)
    out = tex.darker(body[0], 0.85)
    # wings sweeping out of the sides
    for y, xs in ((6, (1, 2, 13, 14)), (7, (0, 1, 2, 13, 14, 15)), (8, (0, 1, 2, 13, 14, 15)), (9, (1, 2, 13, 14))):
        for x in xs:
            tex.px(img, x, y, body[2] if y < 8 else body[1])
    for (x, y) in ((0, 6), (15, 6), (0, 9), (15, 9), (1, 5), (14, 5), (1, 10), (2, 10), (13, 10), (14, 10)):
        tex.px(img, x, y, out)
    # white shoulder chevrons and pale belly
    put(img, [(5, 5), (6, 6), (10, 5), (9, 6)], white[2])
    for (x, y) in mask:
        if y >= 11 and img.getpixel((x, y)) != out:
            tex.px(img, x, y, white[1])
    # cephalic fins on top
    put(img, [(6, 1), (9, 1)], body[2])
    put(img, [(6, 0), (9, 0)], out)
    return puddle(img)


def egg_jellyfish():
    img = tex.new()
    bell = tex.ramp('#dccfee', 5, 0.4)
    pink = tex.ramp('#c78bdc', 3, 0.3)
    mask = _egg_mask(2, 8, 6.0)
    out = (122, 98, 150, 255)
    for (x, y) in mask:
        edge = any((x + dx, y + dy) not in mask for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        if edge and y < 8:
            tex.px(img, x, y, out)
        else:
            k = 3.4 - (x - 4) * 0.15 - (y - 3) * 0.25
            tex.px(img, x, y, (*bell[max(1, min(4, int(round(k))))][:3], 225))
    for x in range(2, 14):
        tex.px(img, x, 8, out if x in (2, 13) else bell[2])
    # four-leaf gonads on top
    put(img, [(6, 4), (7, 4), (8, 5), (9, 5), (7, 6), (6, 6)], pink[1])
    put(img, [(8, 4), (9, 4), (6, 5), (7, 5), (8, 6), (9, 6)], pink[2])
    # tentacles trailing down into the water
    for x, length in ((3, 4), (5, 5), (7, 4), (8, 5), (10, 4), (12, 3)):
        for y in range(9, 9 + length):
            tex.px(img, x, y, (*bell[3 if y % 2 else 2][:3], 230))
    put(img, [(6, 9), (6, 10), (9, 9), (9, 10), (9, 11)], (*pink[2][:3], 230))
    return puddle(img)


def egg_anglerfish():
    body = tex.ramp('#4a3d38', 5, 0.42, 0.03)
    img, mask = egg(body, top=4, rx=6.0)
    out = tex.darker(body[0], 0.85)
    # wide gaping mouth with fangs
    for x in range(4, 13):
        tex.px(img, x, 10, '#4a141b')
    put(img, [(4, 9), (6, 9), (9, 9), (11, 9)], '#efe8d2')
    put(img, [(5, 11), (8, 11), (10, 11), (12, 11)], '#efe8d2')
    put(img, [(9, 6)], '#bcd6dc')                     # milky eye
    put(img, [(10, 6)], body[0])
    # the lure: a stalk arching forward with a glowing bulb
    put(img, [(8, 3), (7, 2), (6, 1), (5, 1)], body[1])
    put(img, [(3, 1), (4, 1), (3, 2), (4, 2)], '#a8ffe6')
    put(img, [(3, 0), (4, 0), (2, 1), (2, 2), (5, 2), (3, 3), (4, 3)], (90, 200, 170, 255))
    put(img, [(3, 1)], '#ffffff')
    return puddle(img)


def egg_moray():
    body = tex.ramp('#c4ae62', 5, 0.5, 0.04)
    spot = tex.rgb('#4c3b25')
    img, mask = egg(body, top=2, rx=5.0)
    for (x, y) in ((5, 10), (9, 9), (7, 12), (10, 11), (4, 8), (11, 7), (6, 11), (8, 10), (11, 10)):
        if (x, y) in mask:
            tex.px(img, x, y, spot)
    # a moray face in profile: long gaping jaws with needle teeth and a yellow eye
    for x in range(3, 12):
        tex.px(img, x, 5, '#5e2424')
        tex.px(img, x, 6, '#8f413e')
    put(img, [(3, 4), (4, 4), (5, 4), (6, 4), (7, 4), (8, 4), (9, 4), (10, 4)], body[3])
    put(img, [(4, 5), (6, 5), (8, 5), (10, 5)], '#f2ecd6')
    put(img, [(5, 6), (7, 6), (9, 6)], '#f2ecd6')
    put(img, [(3, 7), (4, 7), (5, 7), (6, 7), (7, 7), (8, 7), (9, 7), (10, 7)], body[2])
    put(img, [(9, 3)], '#f0d84a')
    put(img, [(10, 3)], '#15130f')
    return puddle(img)


# ------------------------------------------------------------------------------------------------- drops and food
def _blob(base, alpha_v, highlight, seed_shape=0):
    r = tex.ramp(base, 5, 0.5)
    img = tex.new()
    pts = set()
    for y in range(16):
        for x in range(16):
            dx, dy = (x + 0.5 - 8) / 5.6, (y + 0.5 - 9.2) / 4.6
            wob = 0.08 * math.sin(x * 1.3 + seed_shape) + 0.06 * math.cos(y * 1.7)
            if dx * dx + dy * dy < 1 + wob:
                pts.add((x, y))
    # a drip on the lower right
    pts |= {(10, 13), (10, 14), (11, 13)}
    for (x, y) in pts:
        k = 2.6 - (x - 8) * 0.18 - (y - 9) * 0.3
        c = r[max(0, min(4, int(round(k))))]
        tex.px(img, x, y, (c[0], c[1], c[2], alpha_v))
    put(img, [(5, 6), (6, 6), (5, 7)], highlight)
    put(img, [(7, 6)], (*tex.rgb(highlight)[:3], 200))
    return tex.outline(img, tex.darker(r[0], 0.7))


def jelly_blob():
    return _blob('#c9b8e6', 215, '#ffffff', 0.3)


def glow_jelly():
    img = _blob('#4fd6e2', 230, '#f2ffff', 1.7)
    # glowing core and two little sparkles
    put(img, [(8, 9), (9, 9), (8, 10)], '#c8ffff')
    put(img, [(13, 3), (12, 3), (14, 3), (13, 2), (13, 4)], (180, 255, 255, 220))
    put(img, [(13, 3)], '#ffffff')
    put(img, [(2, 12), (2, 11), (2, 13), (1, 12), (3, 12)], (150, 245, 250, 180))
    return img


def angler_lure():
    img = tex.new()
    stalk = tex.ramp('#5a4a42', 4, 0.35)
    # curved stalk from the lower left up to the bulb
    for (x, y), k in zip([(3, 14), (3, 13), (4, 12), (4, 11), (5, 10), (5, 9), (6, 8), (7, 7)], [0, 1, 1, 2, 2, 2, 3, 3]):
        tex.px(img, x, y, stalk[k])
        tex.px(img, x + 1, y, stalk[max(0, k - 1)])
    # halo and bulb
    glow = tex.ramp('#8ef2d2', 5, 0.5)
    for y in range(1, 11):
        for x in range(5, 15):
            d = math.hypot(x + 0.5 - 10.5, y + 0.5 - 5.0)
            if d < 2.6:
                k = 4 - d * 0.9 - (x - 9) * 0.15
                tex.px(img, x, y, glow[max(1, min(4, int(round(k))))])
            elif d < 4.3 and img.getpixel((x, y))[3] == 0:
                tex.px(img, x, y, (*glow[3][:3], int(110 - (d - 2.6) * 50)))
    put(img, [(9, 4), (10, 4), (9, 3)], '#ffffff')
    return tex.outline(img, (40, 34, 30, 255)) if False else img


def _fillet(flesh, skin, cooked=False):
    img = tex.new()
    fr = tex.ramp(flesh, 5, 0.45)
    sr = tex.ramp(skin, 3, 0.35)
    pts = []
    for y in range(16):
        for x in range(16):
            # a long diagonal fillet from the lower left to the upper right
            u = (x - y) / 2.0
            v = (x + y - 15) / 2.0
            u += 0.35 * math.sin(v * 0.45)                          # a slight curve
            half = 2.4 * math.sqrt(max(0.0, 1 - (v / 5.9) ** 2)) + 0.3
            if abs(u) < half and abs(v) < 5.9:
                pts.append((x, y, u, v))
    for x, y, u, v in pts:
        if u < -1.2 and abs(v) < 5.6:
            c = sr[1 if (x + y) % 3 else 0]                           # skin along one edge
        else:
            k = 2.5 + u * 0.5 - v * 0.08
            if not cooked and (x + 2 * y) % 5 == 0:
                k += 1.2                                              # pale flake lines
            c = fr[max(0, min(4, int(round(k))))]
            if cooked and (x + y) % 5 == 0 and abs(v) < 5.5:
                c = tex.darker(fr[1], 0.7)                            # grill marks
        tex.px(img, x, y, c)
    return tex.outline(img, tex.darker(fr[0], 0.55))


def eel_fillet():
    return _fillet('#e9938a', '#6f6a3c')


def cooked_eel_fillet():
    return _fillet('#c98a48', '#4d3f26', cooked=True)


EGGS = {
    'hammerhead_shark_spawn_egg': egg_hammerhead, 'whale_spawn_egg': egg_whale, 'manta_ray_spawn_egg': egg_manta,
    'jellyfish_spawn_egg': egg_jellyfish, 'anglerfish_spawn_egg': egg_anglerfish, 'moray_eel_spawn_egg': egg_moray,
}
ITEMS = {
    'jelly_blob': jelly_blob, 'glow_jelly': glow_jelly, 'angler_lure': angler_lure,
    'eel_fillet': eel_fillet, 'cooked_eel_fillet': cooked_eel_fillet,
}


def generate():
    out = {}
    for name, fn in list(EGGS.items()) + list(ITEMS.items()):
        img = fn()
        tex.save(img, M, 'item/' + name)
        out[name] = img
    return out
