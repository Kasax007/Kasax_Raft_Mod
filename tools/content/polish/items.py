"""2.0 item sprites: coconut, bottles, shark meat, plastic, rope, scrap, trash cube, net, item catcher, titanium
items and armour (icons + worn layers)."""
import math
import random

from PIL import Image

from .common import *  # noqa: F401,F403


# ================================================================================================ helpers
def shade_mask(mask, p, base, grad=2.0, rim=True):
    """Shade a set of pixels: lighter towards the top-left, lit rim on top/left edges, dark rim bottom/right."""
    img = Image.new('RGBA', (16, 16))
    xs = [x for x, y in mask]; ys = [y for x, y in mask]
    cx, cy = sum(xs) / len(xs), sum(ys) / len(ys)
    r = max(1.0, max(math.hypot(x - cx, y - cy) for x, y in mask))
    for (x, y) in mask:
        g = ((x - cx) + (y - cy)) / (2 * r)
        t = base - round(g * grad * 2)
        if rim:
            if (x - 1, y) not in mask or (x, y - 1) not in mask:
                t += 1
            if (x + 1, y) not in mask or (x, y + 1) not in mask:
                t -= 1
        img.putpixel((x, y), clamp(t, p))
    return img


def mask_rows(rows_, ch='#'):
    return {(x, y) for y, r in enumerate(rows_) for x, c in enumerate(r) if c in ch}


def finish(img, dark):
    """1-px outline in the darkest tone of the material (never pure black). Only transparent pixels connected to
    the canvas border get it, so holes inside a sprite (net meshes) stay see-through."""
    w, h = img.size
    outside = set()
    stack = [(x, y) for x in range(w) for y in (0, h - 1)] + [(x, y) for y in range(h) for x in (0, w - 1)]
    while stack:
        x, y = stack.pop()
        if (x, y) in outside or not (0 <= x < w and 0 <= y < h) or img.getpixel((x, y))[3]:
            continue
        outside.add((x, y))
        stack += [(x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)]
    out = img.copy()
    for (x, y) in outside:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            xx, yy = x + dx, y + dy
            if 0 <= xx < w and 0 <= yy < h and img.getpixel((xx, yy))[3]:
                out.putpixel((x, y), rgba(dark))
                break
    return out


# ================================================================================================ coconut
def coconut():
    p = pal('#2a170d', '#3f2414', '#5a341c', '#764727', '#925d34', '#ad7845', '#c6955c')
    img = new()
    cx, cy, R = 7.5, 8.0, 5.6
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx - 0.5, y + 0.5 - cy - 0.5
            if dx * dx + dy * dy <= R * R:
                nz = math.sqrt(max(0, 1 - (dx * dx + dy * dy) / (R * R)))
                l = (-dx * 0.55 - dy * 0.65) / R + nz * 0.6
                t = 2 + round(l * 3)
                if (x + 2 * y) % 5 == 0 and t > 1:      # husk fibres
                    t -= 1
                put(img, x, y, clamp(t, p))
    # three eyes at the top-right pole
    for (x, y) in ((9, 4), (11, 5), (10, 7)):
        put(img, x, y, p[0])
        put(img, x - 1, y - 1, p[4])
    put(img, 5, 5, p[6]); put(img, 4, 6, p[6]); put(img, 5, 6, p[5])   # highlight
    return finish(img, p[0])


# ================================================================================================ bottles
BOTTLE = [
    '................',
    '................',
    '.......kk.......',
    '......kKKk......',
    '.......gG.......',
    '.......gG.......',
    '......gwwG......',
    '.....gwwwwG.....',
    '....gwwwwwwG....',
    '....gwwwwwwG....',
    '....gwwwwwwG....',
    '....gwwwwwwG....',
    '.....GwwwwG.....',
    '......GGGG......',
    '................',
    '................',
]


def bottle(liquid, extra=None):
    cork = pal('#5a3a1e', '#8a5f34', '#b48850')
    glass_l, glass_d = rgba('#e6f2f2'), rgba('#9cb4bc')
    img = new()
    wm = mask_rows(BOTTLE, 'w')
    body = shade_mask(wm, liquid, 3, grad=1.2, rim=False)
    for y, r in enumerate(BOTTLE):
        for x, ch in enumerate(r):
            if ch == 'k':
                put(img, x, y, cork[2])
            elif ch == 'K':
                put(img, x, y, cork[1])
            elif ch == 'g':
                put(img, x, y, glass_l)
            elif ch == 'G':
                put(img, x, y, glass_d)
            elif ch == 'w':
                put(img, x, y, body.getpixel((x, y)))
    for y in range(6, 8):                               # air at the top of the liquid
        for x in range(16):
            if BOTTLE[y][x] == 'w':
                put(img, x, y, lerp(glass_l, liquid[4], 0.25))
    put(img, 5, 8, rgba('#ffffff')); put(img, 5, 9, glass_l)        # glint
    if extra:
        extra(img)
    return finish(img, pal('#33404a')[0])


def fresh_water():
    return bottle(WATER[1:7])


def coconut_milk():
    milk = pal('#9a9488', '#bdb7a8', '#d8d3c4', '#ebe7da', '#f6f3ea', '#fffdf6')
    def flake(img):   # a bit of coconut flesh floating
        put(img, 7, 9, rgba('#c9b28a'))
    return bottle(milk, flake)


# ================================================================================================ shark meat
STEAK = [
    '................',
    '................',
    '................',
    '.....######.....',
    '...#########....',
    '..###########...',
    '..############..',
    '.#############..',
    '.##############.',
    '..#############.',
    '..############..',
    '...##########...',
    '.....#######....',
    '.......###......',
    '................',
    '................',
]


def shark_meat(cooked=False):
    if cooked:
        p = pal('#3d1b0f', '#5e2b16', '#7f3d1e', '#9e5427', '#b86d33', '#cc8a47', '#dda766')
    else:
        p = pal('#7c4a4c', '#a0646a', '#c1828a', '#d79da2', '#e8b8b8', '#f4d2cc', '#fbe7e0')
    skin = pal('#3c4652', '#56636f', '#74828e')
    m = mask_rows(STEAK)
    img = shade_mask(m, p, 4, grad=1.6)
    # flake lines (fish muscle)
    for (x0, y0) in ((4, 4), (7, 3), (10, 4)):
        for k in range(6):
            x, y = x0 + (1 if k > 2 else 0), y0 + k
            if (x, y) in m:
                put(img, x, y, p[3] if not cooked else p[2])
    if cooked:
        for k in range(10):                              # grill marks
            for (x0, y0) in ((2, 6), (5, 3), (8, 2)):
                x, y = x0 + k, y0 + k // 2
                if (x, y) in m and k % 1 == 0 and (k + x0) % 3 != 0:
                    put(img, x, y, p[0])
    else:
        # grey shark skin along the lower-right rim
        for (x, y) in m:
            if ((x + 1, y) not in m or (x, y + 1) not in m) and x + y > 15:
                put(img, x, y, skin[1])
                if (x - 1, y - 1) in m:
                    put(img, x - 1, y - 1, skin[2])
    return finish(img, p[0])


# ================================================================================================ plastic, hook, rope, scrap
PLASTIC_BAG = [
    '................',
    '......##........',
    '.....####..##...',
    '....#########...',
    '...###########..',
    '...###########..',
    '..############..',
    '..#############.',
    '..#############.',
    '...############.',
    '...###########..',
    '....##########..',
    '....#########...',
    '.....######.....',
    '................',
    '................',
]


def plastic():
    """Crumpled sheet of plastic: pale cream, creases lit from the top-left, one blue label scrap."""
    p = PLASTIC
    m = mask_rows(PLASTIC_BAG)
    img = shade_mask(m, p, 4, grad=1.4)
    creases = [((4, 5), (8, 9)), ((9, 3), (12, 7)), ((6, 11), (11, 8))]
    for (a, b) in creases:
        n = max(abs(b[0] - a[0]), abs(b[1] - a[1]))
        for k in range(n + 1):
            x = round(a[0] + (b[0] - a[0]) * k / n); y = round(a[1] + (b[1] - a[1]) * k / n)
            if (x, y) in m:
                put(img, x, y, p[2])
                if (x + 1, y - 1) in m:
                    put(img, x + 1, y - 1, p[6])
    lab = pal('#2f5f94', '#4c86c0')
    for (x, y) in ((10, 10), (11, 10), (10, 11)):
        put(img, x, y, lab[1] if y == 10 else lab[0])
    return finish(img, p[0])


def plastic_hook():
    """Hand-made grappling hook: cream plastic shaft and curved tines, rope tied at the end."""
    p = PLASTIC
    img = new()
    shaft = [(4 + k, 11 - k) for k in range(7)]          # diagonal shaft bottom-left -> top-right
    for (x, y) in shaft:
        put(img, x, y, p[5]); put(img, x + 1, y + 1, p[3])
    hook = [(10, 4), (11, 3), (12, 3), (13, 4), (13, 5), (12, 6), (9, 3), (8, 2), (8, 1)]
    for (x, y) in hook:
        put(img, x, y, p[4])
    for (x, y) in ((11, 3), (12, 3), (8, 1)):
        put(img, x, y, p[6])
    put(img, 12, 6, p[2]); put(img, 13, 5, p[3])
    # rope tail
    for (x, y) in ((3, 12), (2, 13), (3, 14), (2, 15)):
        put(img, x, y, ROPE[4] if y % 2 else ROPE[2])
    put(img, 4, 11, ROPE[5]); put(img, 5, 11, ROPE[3])
    return finish(img, pal('#5a4a2a')[0])


def rope():
    """Coiled rope: twisted ring, lit top-left, loose end to the bottom-right."""
    p = ROPE
    img = new()
    cx, cy = 7.0, 7.0
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            d = math.hypot(dx, dy)
            if 3.0 <= d <= 5.8:
                a = math.atan2(dy, dx)
                twist = int((a + math.pi) / (2 * math.pi) * 14 + d * 0.9) % 2
                light = -(dx + dy) / (2 * 5.8)
                t = 3 + round(light * 2.2) + (1 if twist else 0)
                if 4.2 < d < 4.6:
                    t -= 1                              # groove between the two turns
                put(img, x, y, clamp(t, p))
    for (x, y, t) in ((11, 11, 3), (12, 12, 4), (12, 13, 2), (13, 13, 3), (14, 14, 2), (13, 14, 1)):
        put(img, x, y, p[t])
    return finish(img, p[0])


SCRAP = [
    '................',
    '..aaaa..........',
    '.aaaaaaa........',
    '.aaaaaaaaa......',
    '..aaaaaaaaab....',
    '..aaaaaaabbbb...',
    '...aaaaabbbbbbb.',
    '....aaabbbbbbbb.',
    '.....bbbbbbbbbb.',
    '....bbbbbbbbbb..',
    '....bbbbbbbbb...',
    '.....bbbbbbbb...',
    '......bbbbbb....',
    '.......bbb......',
    '................',
    '................',
]


def scrap_metal():
    """Two bent plates: galvanised steel with rivet holes over a rusty sheet."""
    img = new()
    a = mask_rows(SCRAP, 'a'); b = mask_rows(SCRAP, 'b')
    ib = shade_mask(b, RUST[1:7], 3, grad=1.6)
    ia = shade_mask(a, GALV[1:7], 3, grad=1.6)
    img.alpha_composite(ib); img.alpha_composite(ia)
    for (x, y) in ((3, 2), (6, 4), (4, 5)):
        put(img, x, y, GALV[1]); put(img, x + 1, y + 1, GALV[5])
    for (x, y) in ((8, 9), (11, 8), (10, 11)):
        put(img, x, y, RUST[1])
    for k in range(5):                                  # bend line
        put(img, 4 + k, 2 + k, GALV[5])
    return finish(img, pal('#2b2523')[0])


# ================================================================================================ trash cube
def cube_face(x, y):
    """Isometric cube 14 px wide: top diamond (1,4)-(7.5,1)-(14,4)-(7.5,7), left/right parallelogram faces."""
    if 1 <= x <= 7:
        top = 4 - (x - 1) / 2
        lo = 4 + (x - 1) / 2
        if top - 0.01 <= y <= lo and y >= 1:
            return 't' if y < lo else ('t' if x >= 6 else 'l')
        if lo < y <= lo + 7:
            return 'l'
    if 8 <= x <= 14:
        top = 1 + (x - 8) / 2
        hi = 7 - (x - 8) / 2
        if top - 0.01 <= y <= hi:
            return 't' if y < hi else ('t' if x <= 9 else 'r')
        if hi < y <= hi + 7:
            return 'r'
    return None


def trash_cube():
    """Compressed garbage bale (isometric cube, the 2.0 teal-grey): pressed mass with coloured scraps, lit top,
    mid left face, dark right face, a strap round it."""
    base = pal('#1c2b30', '#2a3f44', '#3d5759', '#557170', '#6f8c88', '#8faba3')
    scraps = [pal('#8f4a41', '#ad6556', '#c98370'), pal('#4c6582', '#6984a4', '#8aa4c0'),
              pal('#7e6442', '#9c7f56', '#bb9d70'), pal('#8f918b', '#b0b1a8', '#d0d0c6')]
    face_t = {'t': 4, 'l': 2, 'r': 1}  # base ramp below has 6 shades; right face uses the darkest pair
    n = tex.value_noise(16, 16, 21, cell=3, octaves=2)
    img = new()
    for y in range(16):
        for x in range(16):
            f = cube_face(x, y)
            if not f:
                continue
            t = face_t[f] + (1 if n[y][x] > 0.7 else 0) - (1 if n[y][x] < 0.25 and f == 't' else 0)
            put(img, x, y, clamp(t, base))
    fl = {'t': 2, 'l': 1, 'r': 0}
    for i, (x, y) in enumerate(((4, 3), (9, 2), (11, 4), (3, 8), (5, 11), (10, 9), (12, 11), (6, 6))):
        f = cube_face(x, y)
        if f:
            c = scraps[i % 4]
            put(img, x, y, c[fl[f]])
            if cube_face(x + 1, y) == f:
                put(img, x + 1, y, c[max(0, fl[f] - 1)])
    strap = pal('#3a2a1c', '#5a4228', '#7a5c38')     # rope strap
    for (x, y) in ((4, 4), (5, 4), (6, 4), (7, 4), (8, 4), (9, 4), (10, 4), (11, 4)):
        if cube_face(x, y) == 't':
            put(img, x, y, strap[2])
    for y in range(8, 15):
        if cube_face(4, y) == 'l':
            put(img, 4, y, strap[1])
        if cube_face(11, y) == 'r':
            put(img, 11, y, strap[0])
    for x in range(1, 15):                               # lit top edges of the side faces
        for y in range(16):
            if cube_face(x, y) in ('l', 'r') and cube_face(x, y - 1) == 't':
                put(img, x, y, lerp(img.getpixel((x, y)), (255, 255, 255, 255), 0.2))
                break
    return finish(img, pal('#121c20')[0])


# ================================================================================================ net, item catcher
def net():
    """Folded fishing net: diamond mesh of rope with darker knots, see-through holes, rope edge."""
    p = ROPE
    img = new()
    for y in range(2, 14):
        for x in range(2, 14):
            if (x + y) % 3 == 0 or (x - y) % 3 == 0:
                knot = (x + y) % 3 == 0 and (x - y) % 3 == 0
                t = 2 if knot else (4 if (x + y) < 16 else 3)
                put(img, x, y, p[t])
    for i in range(2, 14):                               # rope edge
        put(img, i, 2, p[5]); put(img, 2, i, p[5]); put(img, i, 13, p[2]); put(img, 13, i, p[2])
    for (x, y) in ((1, 1), (2, 1), (1, 2), (14, 14), (13, 14), (14, 13)):   # rope ends at the corners
        put(img, x, y, p[4] if x < 8 else p[1])
    return finish(img, p[0])


def item_catcher_item():
    """Driftwood frame with a net stretched inside (top view)."""
    img = new()
    d = DRIFT
    for i in range(1, 15):
        for j in (1, 2):
            put(img, i, j, d[5] if j == 1 else d[4]); put(img, j, i, d[5] if j == 1 else d[4])
            put(img, i, 15 - j, d[2] if j == 1 else d[3]); put(img, 15 - j, i, d[2] if j == 1 else d[3])
    for (x, y) in ((1, 1), (13, 1), (1, 13), (13, 13)):     # lashed corners
        put(img, x, y, ROPE[4]); put(img, x + 1, y + 1, ROPE[2])
    for y in range(3, 13):
        for x in range(3, 13):
            if (x + y) % 3 == 0 or (x - y) % 3 == 0:
                put(img, x, y, ROPE[4] if (x + y) < 16 else ROPE[3])
    return finish(img, d[0])


# ================================================================================================ titanium
def titanium_like(vanilla_rel, old, palette=TITAN, gamma=0.9):
    """Titanium version of a vanilla iron texture: luminance of vanilla iron (where its shape matches) remapped
    onto the titanium ramp; pixels only in the 2.0 shape take the old texture's luminance."""
    v = vanilla(vanilla_rel)
    if v.size != old.size:
        v = v.resize(old.size, Image.NEAREST)
    def norm(img):
        vals = [lum(img.getpixel((x, y))) for y in range(img.height) for x in range(img.width)
                if img.getpixel((x, y))[3] > 0]
        lo, hi = min(vals), max(vals)
        return lambda c: (lum(c) - lo) / max(1e-6, hi - lo)
    nv, no = norm(v), norm(old)
    out = Image.new('RGBA', old.size)
    n = len(palette)
    for y in range(old.height):
        for x in range(old.width):
            o = old.getpixel((x, y))
            if o[3] == 0:
                continue
            c = v.getpixel((x, y))
            t = nv(c) if c[3] > 0 else no(o)
            t = max(0.0, min(1.0, t)) ** gamma
            k = palette[min(n - 1, int(t * n))]
            out.putpixel((x, y), (k[0], k[1], k[2], 255 if o[3] >= 128 else o[3]))
    return out


def jobs(head):
    j = {
        'item/coconut': coconut,
        'item/fresh_water': fresh_water,
        'item/coconut_milk': coconut_milk,
        'item/shark_meat': lambda: shark_meat(False),
        'item/cooked_shark_meat': lambda: shark_meat(True),
        'item/plastic': plastic,
        'item/plastic_hook': plastic_hook,
        'item/rope': rope,
        'item/scrap_metal': scrap_metal,
        'item/trash_cube': trash_cube,
        'item/net': net,
        'item/item_catcher': item_catcher_item,
    }
    for name, van in (('raw_titanium', 'item/raw_iron'), ('titanium_ingot', 'item/iron_ingot'),
                      ('titanium_nugget', 'item/iron_nugget'), ('titanium_helmet', 'item/iron_helmet'),
                      ('titanium_chestplate', 'item/iron_chestplate'), ('titanium_leggings', 'item/iron_leggings'),
                      ('titanium_boots', 'item/iron_boots')):
        j['item/' + name] = (lambda n=name, v=van: titanium_like(v, head('item/' + n)))
    # worn layers: the light end of the ramp (vanilla iron armour is light grey; the model shading darkens it)
    j['entity/equipment/humanoid/titanium'] = lambda: titanium_like('entity/equipment/humanoid/iron',
                                                                    head('entity/equipment/humanoid/titanium'),
                                                                    palette=TITAN[3:])
    j['entity/equipment/humanoid_leggings/titanium'] = lambda: titanium_like(
        'entity/equipment/humanoid_leggings/iron', head('entity/equipment/humanoid_leggings/titanium'),
        palette=TITAN[3:])
    j['item/cables/titanium'] = lambda: remap(head('item/cables/titanium'), TITAN[1:])
    return j
