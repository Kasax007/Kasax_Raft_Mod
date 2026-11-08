"""Item sprites and the two reward blocks of the boss module.

Items (16x16, 1-px dark outline, light from the top-left): leviathan_scale, tidebreaker (handheld spear), and the
spawn eggs of the leviathan and the tideling (26.x water-mob egg style: an egg sitting in a little puddle).
Blocks: heart_of_the_tide (a glowing crystal heart held by golden claws on a tidestone base, animated texture,
emissive crystal) and leviathan_trophy (the Leviathan's head on a plinth, `facing`). Block models are built from
elements whose faces are packed into small atlases (64x64 / 32x32) at vanilla density (1 texel per 1/16 block).
"""
import math
from collections import OrderedDict

from PIL import Image

from bclib import assets, tex

from . import creatures as C

M = 'boss'


def _h(*k):
    return C._h(*k)


def put(img, pixels, colour):
    for (x, y) in pixels:
        tex.px(img, x, y, colour)


# ------------------------------------------------------------------------------------------------- spawn eggs
WATER = [tex.rgb(c) for c in ('#2f457a', '#355a8b', '#3e76a3', '#4b98c4', '#63c9ff')]
PUDDLE = {
    12: {1: 0, 2: 1, 13: 0, 14: 0},
    13: {1: 0, 2: 1, 3: 2, 4: 3, 11: 2, 12: 1, 13: 1, 14: 0},
    14: {2: 0, 3: 1, 4: 2, 5: 4, 6: 4, 7: 3, 8: 4, 9: 3, 10: 2, 11: 1, 12: 1, 13: 0},
    15: {x: 0 for x in range(3, 13)},
}


def _egg_mask(top=1, bottom=13, rx=5.6):
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
    img = tex.new()
    mask = _egg_mask(top, bottom, rx)
    cx, cy = 8.0, (top + bottom) / 2.0 + 0.5
    rim = tex.darker(ramp[0], 0.85)
    for (x, y) in mask:
        if any((x + dx, y + dy) not in mask for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            tex.px(img, x, y, rim)
            continue
        nx, ny = (x + 0.5 - cx) / 6.0, (y + 0.5 - cy) / 7.0
        k = 2.2 + (-nx * 0.6 - ny * 0.8) * 2.2
        tex.px(img, x, y, ramp[max(1, min(len(ramp) - 1, int(round(k))))])
    return img, mask, rim


def puddle(img):
    for y, row in PUDDLE.items():
        for x, k in row.items():
            tex.px(img, x, y, WATER[k])
    return img


def leviathan_egg():
    body = tex.ramp('#24586c', 5, 0.55, 0.05)
    belly = tex.ramp('#9cc8b2', 3, 0.2)
    img, mask, rim = egg(body, top=3, rx=5.6)
    for (x, y) in mask:
        if y >= 10 and img.getpixel((x, y)) != tex.rgb(rim):
            tex.px(img, x, y, belly[2] if y % 2 else belly[1])      # belly scutes
    # ivory horns sweeping back, glowing eye, fanged mouth line, glowing spots
    # horns grow out of the crown and sweep outwards
    put(img, [(5, 3), (4, 2), (10, 3), (11, 2)], C.BONE[2])
    put(img, [(3, 1), (12, 1)], C.BONE[4])
    put(img, [(2, 0), (13, 0)], C.BONE[3])
    put(img, [(4, 7), (5, 7)], (150, 250, 245, 255))
    put(img, [(4, 8)], (40, 120, 130, 255))
    put(img, [(5, 9), (6, 9), (7, 9), (8, 9)], C.GUM[0])
    put(img, [(6, 10), (8, 10)], C.BONE[4])
    put(img, [(9, 5), (11, 7), (10, 9), (7, 4)], C.GLOW)
    put(img, [(8, 2), (8, 3)], C.RAY[2])                            # crest
    return puddle(img)


def tideling_egg():
    body = tex.ramp('#2f7482', 5, 0.55, 0.05)
    belly = tex.ramp('#b3d3c0', 3, 0.2)
    img, mask, rim = egg(body, top=2, rx=5.0)
    for (x, y) in mask:
        if y >= 9 and img.getpixel((x, y)) != tex.rgb(rim):
            tex.px(img, x, y, belly[1] if (x + y) % 3 else belly[2])
    put(img, [(5, 6), (10, 6)], (200, 255, 248, 255))               # glowing eyes
    put(img, [(5, 7), (10, 7)], C.EYE_SOCKET)
    put(img, [(5, 10), (6, 10), (7, 10), (8, 10), (9, 10), (10, 10)], C.GUM[0])
    put(img, [(5, 9), (10, 9), (7, 11), (9, 11)], C.BONE[4])        # fangs of the underbite
    put(img, [(8, 1), (7, 2), (9, 2)], C.RAY[2])                     # dorsal spines
    put(img, [(3, 8), (12, 8)], C.GLOW)
    return puddle(img)


# ------------------------------------------------------------------------------------------------- scale
def leviathan_scale():
    """A single great scale: broad rounded top, tapering to a point, a keel down the middle, growth ridges and an
    iridescent sheen (cyan to violet) where the light hits; one bioluminescent fleck."""
    img = tex.new()
    back = C.BACK
    mask = set()
    for y in range(1, 15):
        t = (y - 0.5) / 14.0
        if t < 0.3:
            half = 6.2 * math.sqrt(max(0.0, 1.0 - ((0.3 - t) / 0.32) ** 2))
        else:
            half = 6.2 * (1.0 - ((t - 0.3) / 0.7) ** 1.6)
        for x in range(16):
            if abs(x + 0.5 - 8.0) <= half + 0.2:
                mask.add((x, y))
    for (x, y) in mask:
        nx, ny = (x + 0.5 - 8.0) / 6.0, (y + 0.5 - 6.0) / 8.0
        k = 2.7 - nx * 1.3 - ny * 1.5
        c = back[max(0, min(5, int(round(k))))]
        band = (x - y * 0.9) - 0.5
        if 1.0 <= band < 3.0 and y < 10:
            c = tex.mix(c, (110, 230, 225, 255) if band < 2.0 else (150, 120, 230, 255), 0.5)
        if y in (5, 9, 12) and abs(x + 0.5 - 8.0) > 0.6:
            c = tex.darker(c, 0.8)
        if abs(x + 0.5 - 8.0) < 0.6 and y > 2:
            c = back[5] if y < 9 else back[4]
        tex.px(img, x, y, c)
    for (x, y) in mask:
        if ((x - 1, y) not in mask or (x, y - 1) not in mask) and (x < 8 or y < 3):
            tex.px(img, x, y, back[5])
        elif ((x + 1, y) not in mask or (x, y + 1) not in mask) and x >= 8:
            tex.px(img, x, y, back[1])
    tex.px(img, 5, 4, (170, 255, 248, 255))
    tex.px(img, 10, 7, C.GLOW)
    return tex.outline(img, (8, 22, 32, 255))


# ------------------------------------------------------------------------------------------------- tidebreaker
def tidebreaker():
    img = tex.new()
    shaft = tex.ramp('#2b4e5a', 4, 0.4, 0.04)
    gold = C.GOLD
    blade = tex.ramp('#bfe3da', 5, 0.45, 0.04)
    # shaft from bottom-left to the head, two pixels thick (light upper row, dark lower row)
    for i in range(0, 9):
        x, y = 1 + i, 14 - i
        tex.px(img, x, y, shaft[2])
        tex.px(img, x + 1, y, shaft[1])
    for (x, y) in ((3, 12), (4, 12), (6, 10), (7, 10)):       # gold wraps
        tex.px(img, x, y, gold[4] if x % 2 else gold[3])
    put(img, [(1, 14), (0, 15), (1, 15)], gold[3])            # pommel
    put(img, [(2, 15)], gold[1])
    put(img, [(0, 14)], gold[5])
    # guard: two curved barbs (trident-like) made of fang bone
    put(img, [(8, 5), (7, 4), (7, 3)], C.BONE[3])
    put(img, [(10, 7), (11, 8), (12, 8)], C.BONE[2])
    put(img, [(9, 6)], gold[4])
    put(img, [(10, 6), (9, 7)], gold[2])
    tex.px(img, 9, 5, (150, 255, 245, 255))                  # the glowing heart-gem
    # long leaf blade of scale, central glowing fuller
    for i in range(0, 6):
        x, y = 10 + i, 5 - i
        if x > 15 or y < 0:
            break
        tex.px(img, x, y, blade[4] if i < 5 else blade[3])
        tex.px(img, x - 1, y, blade[3])
        tex.px(img, x, y + 1, blade[1])
        if 0 < i < 5:
            tex.px(img, x - 1, y - 1 if y - 1 >= 0 else y, blade[4])
            tex.px(img, x + 1 if x + 1 < 16 else x, y + 1, blade[0])
    for i in range(1, 5):
        tex.px(img, 10 + i, 5 - i, (120, 240, 230, 255))
    tex.px(img, 15, 0, blade[4])
    return tex.outline(img, (10, 24, 30, 255))


# ------------------------------------------------------------------------------------------------- block models
class Atlas:
    """Packs element faces (sizes in pixels) into a texture at 1 texel per pixel; UVs are in 0..16 model space."""

    def __init__(self, size=32):
        self.size = size
        self.x = self.y = self.row = 0
        self.rects = []

    def add(self, w, h):
        w, h = max(1, int(math.ceil(w - 0.01))), max(1, int(math.ceil(h - 0.01)))
        if self.x + w > self.size:
            self.x, self.y, self.row = 0, self.y + self.row, 0
        r = (self.x, self.y, w, h)
        if self.y + h > self.size:
            raise ValueError('atlas full')
        self.x += w
        self.row = max(self.row, h)
        self.rects.append(r)
        return r

    def uv(self, r):
        s = 16.0 / self.size
        x, y, w, h = r
        return [round(x * s, 4), round(y * s, 4), round((x + w) * s, 4), round((y + h) * s, 4)]


FACE_DIMS = {'north': (0, 1), 'south': (0, 1), 'east': (2, 1), 'west': (2, 1), 'up': (0, 2), 'down': (0, 2)}


def build_model(elements, atlas, texture_key, painter, img):
    """elements: list of dicts {name, from, to, faces (list), rotation?, emissive?}. Paints each face via
    painter(name, face, u, v, w, h) -> RGBA (u, v texel inside the face rect, top-left origin)."""
    out = []
    for el in elements:
        f, t = el['from'], el['to']
        size = [t[0] - f[0], t[1] - f[1], t[2] - f[2]]
        faces = OrderedDict()
        for face in el['faces']:
            a, b = FACE_DIMS[face]
            r = atlas.add(size[a], size[b])
            x0, y0, w, h = r
            for v in range(h):
                for u in range(w):
                    c = painter(el['name'], face, u, v, w, h)
                    if c is not None:
                        img.putpixel((x0 + u, y0 + v), tuple(c))
            faces[face] = OrderedDict(uv=atlas.uv(r), texture='#' + el.get('tex', texture_key))
        e = OrderedDict([('from', f), ('to', t)])
        if 'rotation' in el:
            e['rotation'] = el['rotation']
        if el.get('emissive'):
            e['light_emission'] = 15
        e['faces'] = faces
        out.append(e)
    return out


# ---------------------------------------------------------------------------------- leviathan trophy
# (the horns sit 0.5 inside the skull's sides: their sides must not lie in the skull's side planes)
TROPHY = [
    dict(name='plinth', **{'from': [1, 0, 1], 'to': [15, 3, 15]}, faces=['north', 'south', 'east', 'west', 'up', 'down']),
    dict(name='skull', **{'from': [3, 3, 5], 'to': [13, 11, 15]}, faces=['north', 'south', 'east', 'west', 'up']),
    dict(name='snout', **{'from': [4, 6, 1], 'to': [12, 10, 5]}, faces=['north', 'east', 'west', 'up', 'down']),
    dict(name='jaw', **{'from': [4.5, 3, 1.5], 'to': [11.5, 5, 5]}, faces=['north', 'east', 'west', 'up']),
    dict(name='teeth', **{'from': [5, 5, 1.5], 'to': [11, 6, 4.5]}, faces=['north', 'east', 'west']),
    dict(name='brow', **{'from': [2.5, 10, 4.5], 'to': [13.5, 12, 8]}, faces=['north', 'east', 'west', 'up']),
    dict(name='crest', **{'from': [7.5, 11, 7], 'to': [8.5, 15, 15]}, faces=['east', 'west', 'up', 'north', 'south']),
    dict(name='horn_l', **{'from': [3.5, 11, 9], 'to': [5.5, 13, 16]}, faces=['east', 'west', 'up', 'down', 'south'],
         rotation={'origin': [4.5, 12, 9], 'axis': 'x', 'angle': 22.5}),
    dict(name='horn_r', **{'from': [10.5, 11, 9], 'to': [12.5, 13, 16]}, faces=['east', 'west', 'up', 'down', 'south'],
         rotation={'origin': [11.5, 12, 9], 'axis': 'x', 'angle': 22.5}),
    dict(name='eye_l', **{'from': [2.9, 7, 5.5], 'to': [3, 9, 8.5]}, faces=['west'], emissive=True),
    dict(name='eye_r', **{'from': [13, 7, 5.5], 'to': [13.1, 9, 8.5]}, faces=['east'], emissive=True),
]


def _trophy_paint(name, face, u, v, w, h):
    light = {'up': 0.8, 'down': -1.0, 'north': 0.2, 'south': -0.4, 'west': 0.3, 'east': -0.2}[face]
    if name == 'plinth':
        # tidestone plinth with a gold band and a glowing rune in the middle of each side
        stone = tex.ramp('#3f7a7c', 5, 0.4, 0.04)
        if face == 'up':
            edge = min(u, v, w - 1 - u, h - 1 - v)
            if edge == 0:
                return C.GOLD[4]
            return stone[2 + ((u * 3 + v * 5) % 7 == 0)]
        if face == 'down':
            return stone[0]
        if v == 0:
            return C.GOLD[4] if face in ('north', 'west') else C.GOLD[3]
        if v == h - 1:
            return C.GOLD[1]
        if abs(u + 0.5 - w / 2.0) < 1.0:
            return (110, 240, 230, 255)
        return stone[max(0, min(4, int(round(2 + light + (((u + v) % 4) == 0) * -0.7))))]
    if name in ('skull', 'snout', 'brow'):
        a, b = u, v
        s, sid = C.scales(a, b, 3.0, 2.0, 5 + len(name))
        k = 2.2 + s + light + (sid - 0.5) * 0.6
        if name == 'snout' and face == 'up' and v < 1 and u in (2, 5):
            return (14, 28, 36, 255)            # nostrils
        if name == 'snout' and face == 'north' and v >= h - 1:
            return C.GUM[1]
        if name == 'snout' and face == 'down':
            return C.GUM[2]
        if name == 'skull' and face in ('east', 'west') and v >= h - 3:
            return C.BELLY[2 + (u % 2)]
        if name == 'skull' and face in ('east', 'west') and u >= w - 4 and u % 2 == 0 and 2 <= v <= 5:
            return C.GLOW_DIM                   # gill slits
        if name == 'brow' and face == 'up':
            k += 0.8
        return C.BACK[max(0, min(5, int(round(k))))]
    if name == 'jaw':
        if face == 'up':
            return C.GUM[3] if abs(u + 0.5 - w / 2.0) < 1.5 else C.GUM[1]
        if v == 0:
            return C.BONE[4] if u % 2 == 0 else C.GUM[0]
        return C.BELLY[max(0, min(4, int(round(2 + light))))]
    if name == 'teeth':
        return C.BONE[4] if u % 2 == 0 else C.BONE[2]
    if name == 'crest':
        if face == 'up':
            return C.GLOW if u % 2 == 0 else C.RAY[2]
        return C.RAY[2] if u % 3 == 0 else C.FIN[2 + (v < 1)]
    if name in ('horn_l', 'horn_r'):
        if face == 'south':
            return C.BONE[4]
        k = 1.0 + light * 0.6 + (v / max(1, h)) * -0.5
        if face in ('east', 'west') and u % 3 == 0:
            k -= 0.8
        return C.BONE[max(0, min(4, int(round(k))))] if face != 'down' else C.HORN_BASE[1]
    if name in ('eye_l', 'eye_r'):
        if 1 <= u <= 1 and v <= 1:
            return (30, 90, 100, 255)
        return (150, 250, 245, 255) if v == 0 else (90, 220, 225, 255)
    return (255, 0, 255, 255)


def leviathan_trophy():
    atlas = Atlas(64)
    img = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    els = build_model(TROPHY, atlas, 'all', _trophy_paint, img)
    tex.save(img, M, 'block/leviathan_trophy')
    assets.write_json(M, 'assets/raft/models/block/leviathan_trophy.json', OrderedDict(
        parent='minecraft:block/block',
        textures=OrderedDict(all='raft:block/leviathan_trophy', particle='raft:block/leviathan_trophy'),
        elements=els))
    assets.blockstate(M, 'leviathan_trophy', assets.horizontal_variants('block/leviathan_trophy'))
    assets.block_item(M, 'leviathan_trophy')
    return img


# ---------------------------------------------------------------------------------- heart of the tide
HEART = [
    dict(name='base', **{'from': [3, 0, 3], 'to': [13, 2, 13]}, faces=['north', 'south', 'east', 'west', 'up', 'down']),
    dict(name='ring', **{'from': [4.5, 2, 4.5], 'to': [11.5, 3, 11.5]}, faces=['north', 'south', 'east', 'west', 'up']),
    dict(name='claw', **{'from': [4, 2, 7.5], 'to': [5, 11, 8.5]}, faces=['north', 'south', 'east', 'west', 'up'],
         rotation={'origin': [4.5, 2, 8], 'axis': 'z', 'angle': -22.5}),
    dict(name='claw', **{'from': [11, 2, 7.5], 'to': [12, 11, 8.5]}, faces=['north', 'south', 'east', 'west', 'up'],
         rotation={'origin': [11.5, 2, 8], 'axis': 'z', 'angle': 22.5}),
    dict(name='claw', **{'from': [7.5, 2, 4], 'to': [8.5, 11, 5]}, faces=['north', 'south', 'east', 'west', 'up'],
         rotation={'origin': [8, 2, 4.5], 'axis': 'x', 'angle': 22.5}),
    dict(name='claw', **{'from': [7.5, 2, 11], 'to': [8.5, 11, 12]}, faces=['north', 'south', 'east', 'west', 'up'],
         rotation={'origin': [8, 2, 11.5], 'axis': 'x', 'angle': -22.5}),
    dict(name='crystal', tex='crystal', **{'from': [5, 4, 5], 'to': [11, 12, 11]},
         faces=['north', 'south', 'east', 'west', 'up', 'down'],
         rotation={'origin': [8, 8, 8], 'axis': 'y', 'angle': 45}, emissive=True),
    dict(name='crystal_top', tex='crystal', **{'from': [6.5, 12, 6.5], 'to': [9.5, 15, 9.5]},
         faces=['north', 'south', 'east', 'west', 'up'],
         rotation={'origin': [8, 8, 8], 'axis': 'y', 'angle': 45}, emissive=True),
    dict(name='crystal_bottom', tex='crystal', **{'from': [6.5, 2.5, 6.5], 'to': [9.5, 4, 9.5]},
         faces=['north', 'south', 'east', 'west'],
         rotation={'origin': [8, 8, 8], 'axis': 'y', 'angle': 45}, emissive=True),
]


def _heart_paint(name, face, u, v, w, h):
    light = {'up': 0.8, 'down': -1.0, 'north': 0.2, 'south': -0.4, 'west': 0.3, 'east': -0.2}[face]
    stone = tex.ramp('#2f6668', 5, 0.4, 0.04)
    if name == 'base':
        if face == 'up':
            edge = min(u, v, w - 1 - u, h - 1 - v)
            return C.GOLD[4] if edge == 0 else stone[2 + ((u + v) % 3 == 0)]
        if face == 'down':
            return stone[0]
        if v == 0:
            return C.GOLD[4]
        if (u % 3 == 1) and v == 1:
            return (110, 240, 230, 255)           # rune dots
        return stone[max(0, min(4, int(round(1.6 + light))))]
    if name == 'ring':
        return C.GOLD[max(0, min(5, int(round(3 + light))))]
    if name == 'claw':
        if face == 'up' or v < 2:
            return C.GOLD[5]
        return C.GOLD[max(0, min(5, int(round(2.6 + light - v * 0.1))))]
    return (255, 0, 255, 255)


def _crystal_frames(n=12):
    """16x16 animated crystal: facets with a sweeping shimmer and a pulsing core (cyan, a touch of violet)."""
    frames = []
    for f in range(n):
        img = tex.new()
        ph = f / n
        for y in range(16):
            for x in range(16):
                # facet pattern: diagonal bands, each a flat shade
                facet = (x + y * 2) // 5 + (x - y) // 6
                k = 0.25 + 0.3 * ((facet * 37) % 5) / 4.0
                d = math.hypot(x - 7.5, y - 7.5) / 10.6
                core = max(0.0, 1.0 - d) ** 2
                pulse = 0.5 + 0.5 * math.sin(ph * math.tau)
                k += core * (0.3 + 0.3 * pulse)
                # shimmer sweeping across the facets
                sweep = ((x + y) / 30.0 - ph) % 1.0
                if sweep < 0.07:
                    k += 0.3
                k = min(1.0, k)
                base = tex.mix((14, 78, 104, 255), (150, 250, 238, 255), k)
                if (facet % 4 == 3) and k < 0.7:
                    base = tex.mix(base, (120, 90, 210, 255), 0.35)
                # bright facet edges
                if (x + y * 2) % 5 == 0:
                    base = tex.mix(base, (190, 255, 250, 255), 0.35)
                img.putpixel((x, y), base)
        frames.append(img)
    return frames


def heart_of_the_tide():
    atlas = Atlas(32)
    img = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
    plain = [e for e in HEART if e.get('tex') != 'crystal']
    els = build_model(plain, atlas, 'frame', _heart_paint, img)
    # crystal faces use the animated 16x16 crystal texture, mapped 1:1 from its centre
    for el in HEART:
        if el.get('tex') != 'crystal':
            continue
        f, t = el['from'], el['to']
        size = [t[0] - f[0], t[1] - f[1], t[2] - f[2]]
        faces = OrderedDict()
        for face in el['faces']:
            a, b = FACE_DIMS[face]
            w, h = size[a], size[b]
            u0, v0 = 8 - w / 2.0, 8 - h / 2.0
            faces[face] = OrderedDict(uv=[u0, v0, u0 + w, v0 + h], texture='#crystal')
        e = OrderedDict([('from', f), ('to', t), ('rotation', el['rotation']), ('light_emission', 15), ('faces', faces)])
        els.append(e)
    tex.save(img, M, 'block/heart_of_the_tide_frame')
    tex.save_animated(_crystal_frames(), M, 'block/heart_of_the_tide', frametime=3, interpolate=True)
    assets.write_json(M, 'assets/raft/models/block/heart_of_the_tide.json', OrderedDict(
        parent='minecraft:block/block',
        textures=OrderedDict(frame='raft:block/heart_of_the_tide_frame', crystal='raft:block/heart_of_the_tide',
                             particle='raft:block/heart_of_the_tide'),
        elements=els))
    assets.blockstate(M, 'heart_of_the_tide', OrderedDict([('waterlogged=false', {'model': 'raft:block/heart_of_the_tide'}),
                                                           ('waterlogged=true', {'model': 'raft:block/heart_of_the_tide'})]))
    assets.block_item(M, 'heart_of_the_tide')
    return img


# ------------------------------------------------------------------------------------------------- all
SPRITES = OrderedDict([
    ('leviathan_scale', leviathan_scale), ('tidebreaker', tidebreaker),
    ('leviathan_spawn_egg', leviathan_egg), ('tideling_spawn_egg', tideling_egg),
])


def generate():
    out = {}
    for name, fn in SPRITES.items():
        img = fn()
        tex.save(img, M, 'item/' + name)
        if name == 'tidebreaker':
            assets.handheld_item(M, name)
        else:
            assets.flat_item(M, name)
        out[name] = img
    out['leviathan_trophy'] = leviathan_trophy()
    out['heart_of_the_tide'] = heart_of_the_tide()
    return out
