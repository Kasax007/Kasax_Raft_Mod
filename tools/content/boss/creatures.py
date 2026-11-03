"""Entity textures of the boss module, painted face by face onto the boxes read from the Java models
(javamodel.py, shared approach with the marine module): the Leviathan (256x256 + emissive layer), the tideling
(64x64 + emissive layer) and the brine bolt (32x32).

Palette (spec section 2, deep sea): navy/teal back scales, pale sea-green belly scutes, dark teal fin membranes with
lighter rays, bioluminescent cyan (and a little violet) spots, bone-white teeth, deep red mouth, ivory horns and the
broken Tidewarden shackle in gold with glowing cyan runes. Light from the top-left: upper faces lighter, lower faces and
the right side darker.
"""
import math

from PIL import Image

from bclib import tex

from . import art, javamodel

M = 'boss'
LEVIATHAN_JAVA = 'content/boss/client/model/LeviathanModel.java'
TIDELING_JAVA = 'content/boss/client/model/TidelingModel.java'
BOLT_JAVA = 'content/boss/client/model/BrineBoltModel.java'

# ------------------------------------------------------------------------------------------------- palette
BACK = tex.ramp('#21566a', 6, 0.42, 0.05, 0.10)        # navy-teal scales, dark -> light
BELLY = tex.ramp('#93c2ad', 5, 0.34, 0.03, 0.06)       # pale sea-green scutes
FIN = tex.ramp('#1d5f6d', 5, 0.34, 0.05, 0.08)         # membrane
RAY = tex.ramp('#3c9a9c', 4, 0.30, 0.03, 0.05)         # fin rays
BONE = tex.ramp('#ddd5bb', 5, 0.30, 0.04, 0.05)        # teeth, horns
HORN_BASE = tex.ramp('#4d4a52', 4, 0.30, 0.03, 0.04)
GUM = tex.ramp('#6e2840', 5, 0.30, 0.03, 0.06)         # mouth, tongue
GOLD = tex.ramp('#d9a640', 6, 0.50, 0.06, 0.10)        # shackle
IRON = tex.ramp('#6f6a5e', 4, 0.30, 0.03, 0.04)
GLOW = (110, 246, 234, 255)
GLOW_CORE = (214, 255, 250, 255)
GLOW_DIM = (60, 190, 196, 255)
VIOLET = (178, 128, 255, 255)
EYE_SOCKET = (14, 30, 40, 255)

NAMES = {
    (0, 54): 'skull', (162, 142): 'snout', (122, 100): 'crest', (100, 175): 'brow', (124, 201): 'eye',
    (178, 175): 'upper_teeth', (226, 201): 'fang', (56, 175): 'horn', (56, 189): 'horn_tip', (88, 100): 'frill', (52, 201): 'barbel',
    (116, 54): 'jaw', (0, 142): 'lower_teeth', (96, 201): 'chin', (46, 0): 'seg_a', (102, 142): 'fin_a',
    (0, 201): 'side_fin', (0, 100): 'seg_b', (0, 175): 'fin_b', (40, 175): 'collar', (146, 142): 'collar',
    (146, 201): 'collar', (200, 201): 'collar', (138, 201): 'chain', (178, 100): 'stock', (146, 175): 'tail_fin',
    (0, 0): 'fluke', (56, 201): 'side_fluke',
}


def _h(*k):
    n = 2166136261
    for v in k:
        n = ((n ^ (int(v) & 0xffffffff)) * 16777619) & 0xffffffff
    n ^= n >> 15
    n = (n * 2246822519) & 0xffffffff
    n ^= n >> 13
    return (n & 0xffff) / 65535.0


def _pick(r, k):
    return r[max(0, min(len(r) - 1, int(round(k))))]


class P:
    """Texel with box-local pixel coordinates X, Y, Z (Y down from the top, Z back from the front)."""
    __slots__ = ('name', 'face', 'fx', 'fy', 'fz', 'X', 'Y', 'Z', 'w', 'h', 'd', 'i', 'j', 'fw', 'fh', 'u', 'v')


def _paint(tw, th, boxes, painter):
    base = Image.new('RGBA', (tw, th), (0, 0, 0, 0))
    glow = Image.new('RGBA', (tw, th), (0, 0, 0, 0))
    done = set()
    p = P()
    for b in boxes:
        key = (b.u, b.v)
        if key in done:
            continue
        done.add(key)
        name = NAMES.get(key) if painter is _leviathan_texel else painter.names.get(key)
        if name is None:
            raise ValueError('no name for box at %s' % (key,))
        for face, u0, v0, fw, fh, fn in javamodel._faces(b):
            for j in range(fh):
                for i in range(fw):
                    x, y, z = fn(i, j)
                    x0, x1 = (b.x, b.x + b.w)
                    p.name, p.face = name, face
                    p.fx = (x - x0) / b.w if b.w else 0.5
                    if b.mirror:
                        p.fx = 1.0 - p.fx
                    p.fy = (y - b.y) / b.h if b.h else 0.5
                    p.fz = (z - b.z) / b.d if b.d else 0.5
                    p.X, p.Y, p.Z = p.fx * b.w, p.fy * b.h, p.fz * b.d
                    p.w, p.h, p.d = b.w, b.h, b.d
                    p.i, p.j, p.fw, p.fh = i, j, fw, fh
                    p.u, p.v = u0 + i, v0 + j
                    c, g = painter(p)
                    if 0 <= p.u < tw and 0 <= p.v < th:
                        if c is not None:
                            base.putpixel((p.u, p.v), tuple(c))
                        if g is not None:
                            glow.putpixel((p.u, p.v), tuple(g))
    return base, glow


# ------------------------------------------------------------------------------------------------- patterns
def scales(a, b, cw=4.0, ch=3.0, seed=0):
    """Overlapping scale rows: (shade offset, scale id). a runs along the row, b across rows."""
    row = math.floor(b / ch)
    off = (row % 2) * cw * 0.5
    col = math.floor((a + off) / cw)
    la = (a + off) - col * cw
    lb = b - row * ch
    sid = _h(col, row, seed)
    s = 0.0
    if lb >= ch - 1.0:
        s -= 1.0                       # shadow under the scale's edge
    elif lb < 1.0 and la < cw * 0.55:
        s += 1.0                       # light catches the top-left of each scale
    if la >= cw - 1.0 and lb < ch - 1.0:
        s -= 0.5
    return s, sid


def _side_light(p):
    """Top-left light: top faces brighter, bottom darker, west (model +x side faces) a little brighter."""
    return {'top': 0.8, 'bottom': -1.2, 'north': 0.2, 'south': -0.4, 'west': 0.15, 'east': -0.1}[p.face]


def _hide(p, lo=0.0):
    return None


def body_scales(p, seed, belly_from=0.64, spots=True, scale_size=(4.0, 3.0), rows_along='z'):
    """Back scales fading to belly scutes on a body box (segments, tail stock, skull). Returns (base, glow)."""
    face = p.face
    light = _side_light(p)
    if face == 'bottom':
        # ventral scutes: bands across the width every 3 px along the body
        band = math.floor(p.Z / 3.0)
        lb = p.Z - band * 3.0
        k = 2.6 + (0.6 if lb < 1.0 else 0.0) - (1.0 if lb >= 2.0 else 0.0)
        edge = min(p.X, p.w - p.X)
        if edge < 2.0:
            k -= 1.0
        k += (_h(band, seed) - 0.5) * 0.6
        return _pick(BELLY, k - 0.6), None
    if face in ('top',):
        a, b = p.X, p.Z
    elif face in ('west', 'east'):
        a, b = p.Z, p.Y
    else:
        a, b = p.X, p.Y
    s, sid = scales(a, b, scale_size[0], scale_size[1], seed)
    # countershading on the sides
    fy = p.fy if face in ('west', 'east', 'north', 'south') else 0.0
    if face in ('west', 'east', 'north', 'south') and fy > belly_from:
        t = (fy - belly_from) / max(0.01, 1.0 - belly_from)
        if t > 0.35:
            band = math.floor(p.Z / 3.0) if face in ('west', 'east') else math.floor(p.X / 3.0)
            k = 2.2 + s * 0.6 - (0.5 if (p.Y % 3.0) >= 2.0 else 0.0) + (_h(band, seed + 3) - 0.5) * 0.4
            return _pick(BELLY, k - 0.4 - (1.0 - t) * 1.2), None
        k = 2.0 + s + light
        return art.lerp(_pick(BACK, k + 1.0), _pick(BELLY, 1.0), t * 1.6), None
    shade = 2.2 + s + light + (sid - 0.5) * 0.9 - fy * 1.0
    # dorsal ridge: darker scutes along the top centre
    if face == 'top' and abs(p.X - p.w / 2.0) < 2.0:
        shade -= 1.0 if (p.Z % 4.0) < 3.0 else 0.0
    c = _pick(BACK, shade)
    g = None
    if spots:
        # bioluminescent lateral line and back spots
        if face in ('west', 'east') and abs(p.fy - 0.44) < 0.06:
            ph = (p.Z + seed * 3) % 5.0
            if ph < 1.0:
                c, g = (70, 170, 170, 255), GLOW
        if face == 'top' and abs(abs(p.X - p.w / 2.0) - p.w * 0.28) < 0.6:
            ph = (p.Z + seed * 2 + 2) % 6.0
            if ph < 1.0:
                c, g = (70, 160, 168, 255), (VIOLET if _h(p.Z, seed, 7) > 0.7 else GLOW)
    return c, g


def membrane(p, along, across, length, width, seed, glow_edge=True, rays=5.0, root_at=0.0):
    """Fin membrane: rays spreading from the root (along = 0 at root), darker towards the root, glowing rim dots."""
    t = along / max(1.0, length)
    ray = (across + along * 0.25) % rays
    k = 1.2 + t * 1.4
    c = _pick(FIN, k)
    if ray < 1.0:
        c = _pick(RAY, 1.0 + t * 1.6)
    g = None
    if glow_edge and along >= length - 1.0:
        if (across + seed) % 3.0 < 1.0:
            c, g = (90, 200, 200, 255), GLOW
        else:
            c = _pick(RAY, 3)
    return c, g


# ------------------------------------------------------------------------------------------------- leviathan
def _skull(p):
    if p.face == 'bottom':
        # throat: scutes
        return body_scales(p, 11)
    c, g = body_scales(p, 11, belly_from=0.7, spots=False, scale_size=(3.0, 2.0))
    if p.face in ('west', 'east'):
        # gill slits at the back of the head with glowing edges
        for gx in (19.0, 22.0, 25.0):
            if abs(p.Z - gx) < 0.6 and 5.0 <= p.Y <= 15.0:
                return (16, 36, 48, 255), None
            if abs(p.Z - gx - 1.0) < 0.6 and 6.0 <= p.Y <= 14.0:
                return (60, 150, 156, 255), GLOW_DIM
        # dark mask around the eye (eye box sits at z 3..9, y 4..8 of the skull)
        if 2.0 <= p.Z <= 10.0 and 2.5 <= p.Y <= 9.5:
            return art.lerp(c, EYE_SOCKET, 0.55), None
        # a row of glowing dots behind the eye
        if abs(p.Y - 5.5) < 0.6 and 11.0 <= p.Z <= 17.0 and int(p.Z) % 2 == 1:
            return (70, 170, 170, 255), GLOW
    if p.face == 'top':
        # paired spots on the skull
        for sx, sz in ((9.0, 6.0), (21.0, 6.0), (11.0, 14.0), (19.0, 14.0)):
            if abs(p.X - sx) < 1.0 and abs(p.Z - sz) < 1.0:
                return (80, 180, 178, 255), GLOW
    if p.face == 'north':
        # face behind the snout: dark
        return art.lerp(c, BACK[0], 0.4), None
    return c, g


def _snout(p):
    if p.face == 'bottom':
        # palate: ridged red
        k = 2.0 + (0.7 if int(p.Z) % 3 == 0 else 0.0) - min(p.X, p.w - p.X) * 0.0
        return _pick(GUM, k), None
    if p.face == 'north':
        # snout tip: lips at the bottom, scales above
        if p.Y >= p.h - 2.0:
            return _pick(GUM, 1), None
        s, _ = scales(p.X, p.Y, 3.0, 2.0, 21)
        return _pick(BACK, 2.6 + s - p.fy), None
    c, g = body_scales(p, 21, belly_from=0.75, spots=False, scale_size=(3.0, 2.0))
    if p.face in ('west', 'east') and p.Y >= p.h - 1.0:
        return _pick(GUM, 0), None          # lip line
    if p.face == 'top':
        # nostrils near the tip
        for sx in (6.0, 15.0):
            if abs(p.X - sx) < 1.5 and 2.0 <= p.Z < 4.0:
                return (12, 26, 34, 255), None
            if abs(p.X - sx) < 1.5 and 4.0 <= p.Z < 5.0:
                return _pick(BACK, 4), None
        # brighter muzzle ridge
        if abs(p.X - p.w / 2.0) < 1.0 and p.Z > 5.0:
            return _pick(BACK, 3.5 if int(p.Z) % 3 else 2.5), None
    return c, g


def _teeth(p, down=True):
    """Teeth rows: on the outer sides a row of pointed teeth (white), the far edge gums."""
    if p.face in ('west', 'east', 'north', 'south'):
        a = p.Z if p.face in ('west', 'east') else p.X
        tooth = a % 3.0
        depth = p.Y if down else p.h - 1.0 - p.Y   # 0 = gum side
        if depth < 0.5:
            return _pick(GUM, 1), None
        if tooth < 2.0:
            return _pick(BONE, 3.5 - (tooth > 1.0) - depth * 0.4), None
        return _pick(GUM, 0), None
    if p.face == ('bottom' if down else 'top'):
        edge = min(p.X, p.w - 1.0 - p.X, p.Z, p.d - 1.0 - p.Z)
        if edge < 1.0:
            return _pick(BONE, 3 if int(p.Z + p.X) % 3 else 2), None
        return _pick(GUM, 2 + (int(p.Z) % 4 == 0)), None
    return _pick(GUM, 1), None


def _jaw(p):
    if p.face == 'top':
        # inside of the mouth: tongue in the middle, gums at the edges
        mid = abs(p.X - p.w / 2.0)
        if mid < 5.0 and p.Z > 6.0:
            return _pick(GUM, 3.2 - mid * 0.25 + (0.5 if mid < 0.6 else 0.0) - (p.Z < 8.0) * 0.5), None
        return _pick(GUM, 1.5), None
    if p.face == 'bottom':
        return body_scales(p, 31)
    if p.face == 'north':
        if p.Y < 1.0:
            return _pick(GUM, 1), None
        s, _ = scales(p.X, p.Y, 3.0, 2.0, 31)
        return _pick(BELLY, 2.0 + s - p.fy), None
    if p.face == 'south':
        return _pick(BACK, 1), None
    # sides: lighter jaw scales, lip at the top
    if p.Y < 1.0:
        return _pick(GUM, 0), None
    s, sid = scales(p.Z, p.Y, 3.0, 2.0, 31)
    t = p.fy
    c = art.lerp(_pick(BACK, 2.6 + s), _pick(BELLY, 2.0 + s * 0.5), t * 1.2)
    if abs(p.Y - 3.0) < 0.6 and int(p.Z) % 4 == 1 and p.Z < 30:
        return (70, 170, 170, 255), GLOW
    return c, None


def _eye(p):
    if p.face == 'east' or p.face == 'west':
        # 6 x 4 eye: glowing slit pupil in a cyan iris
        cz, cy = 2.5, 1.5
        dz, dy = abs(p.Z - 0.5 - cz), abs(p.Y - 0.5 - cy)
        if dz <= 2.6 and dy <= 1.6:
            if dz < 0.6:
                return (20, 60, 70, 255), (40, 120, 130, 255)
            if dz <= 1.6 and dy <= 1.0:
                return (150, 250, 245, 255), GLOW_CORE
            return (80, 220, 225, 255), GLOW
        return EYE_SOCKET, None
    return EYE_SOCKET, None


def _brow(p):
    s, _ = scales(p.Z if p.face in ('west', 'east') else p.X, p.Y if p.face != 'top' else p.Z, 3.0, 2.0, 41)
    if p.face == 'top':
        # knobbly ridge with a highlight on its crest
        k = 3.6 + s * 0.8 - (0.0 if abs(p.X - p.w / 2.0) < 1.5 else 0.8)
        return _pick(BACK, k), None
    return _pick(BACK, 2.0 + s + _side_light(p)), None


def _horn(p, tip=False):
    # dark horn with growth ridges at the root, turning to pale ivory towards the tip
    t = (0.55 + p.fz * 0.45) if tip else p.fz * 0.55
    ring = (p.Z % 3.0) < 1.0 and t < 0.85
    light = _side_light(p) * 0.8
    if t < 0.35:
        return _pick(HORN_BASE, 1.2 + t * 4.0 + light - (0.7 if ring else 0.0)), None
    k = (t - 0.35) * 5.5 + light - (0.8 if ring else 0.0)
    if p.face == 'south' and tip:
        k = 4.0
    return _pick(BONE, k), None


def _fang(p):
    k = 3.2 + _side_light(p) - p.fy * 1.5
    if p.face == 'bottom':
        k = 4
    return _pick(BONE, k), None


def _barbel(p):
    if p.fy > 0.8:
        return (110, 230, 225, 255), GLOW
    return _pick(BACK, 1.5 + (p.Y % 3.0 < 1.0)), None


def _crest(p):
    if p.face in ('west', 'east'):
        along = p.h - p.Y           # from the skull upwards
        return membrane(p, along, p.Z, p.h, p.d, 5, rays=4.0)
    if p.face == 'top':
        return (_pick(RAY, 3), GLOW) if int(p.Z) % 4 == 0 else (_pick(RAY, 2), None)
    return _pick(FIN, 1), None


def _frill(p):
    # fan from the front edge (z = 0) backwards, rays radiating from the mid height
    if p.face in ('west', 'east'):
        dy = p.Y - p.h / 2.0
        along = math.sqrt(p.Z ** 2 + (dy * 0.6) ** 2)
        ang = math.atan2(dy, p.Z + 2.0)
        across = ang * 9.0
        c, g = membrane(p, along, across + 20, 15.0, p.h, 9, rays=3.0)
        if along > 15.5:
            return None, None
        return c, g
    return _pick(RAY, 2), None


def _chin(p):
    s, _ = scales(p.X, p.Z if p.face in ('top', 'bottom') else p.Y, 3.0, 2.0, 51)
    return _pick(BELLY, 1.8 + s * 0.6 + _side_light(p) * 0.5), None


def _segment(p, seed):
    c, g = body_scales(p, seed)
    if p.face in ('north', 'south'):
        # hidden mostly; keep them dark so gaps read as shadow
        c = art.lerp(c, BACK[0], 0.35)
    return c, g


def _dorsal(p, seed):
    if p.face in ('west', 'east'):
        along = p.h - p.Y
        # swept-back shape: the fin rises towards the back
        top = p.h * (0.45 + 0.55 * min(1.0, p.fz * 1.4))
        if along > top + 0.5:
            return None, None
        return membrane(p, along, p.Z, top, p.d, seed, rays=4.0)
    if p.face == 'top':
        return (_pick(RAY, 3), GLOW) if int(p.Z + seed) % 5 == 0 else (_pick(RAY, 2), None)
    return _pick(FIN, 1), None


def _side_fin(p, seed):
    if p.face in ('top', 'bottom'):
        along = p.X                  # from the body outwards
        c, g = membrane(p, along, p.Z, p.w, p.d, seed, rays=3.0)
        # rounded tip: cut the outer corners
        if along > p.w - 3.0 and (p.Z < 2.0 or p.Z > p.d - 2.0) or (along > p.w - 1.0 and (p.Z < 4.0 or p.Z > p.d - 3.0)):
            return None, None
        if p.face == 'bottom':
            c = art.lerp(c, FIN[0], 0.3)
        return c, g
    return _pick(RAY, 1), None


def _collar(p):
    # Tidewarden gold: engraved band, cyan runes, scuffed and broken where it snapped
    light = _side_light(p)
    k = 3.0 + light
    if p.face in ('north', 'south'):
        k += 0.3
        edge = (p.X % 6.0) if p.w > p.h else (p.Y % 6.0)
        if edge < 1.0:
            k -= 1.2
    else:
        a = p.Y if p.h > p.w else p.X
        if p.Z < 1.0 or p.Z >= p.d - 1.0:
            k += 0.8                 # raised rims
        else:
            # rune panel in the middle of the band
            rune = int(a) % 5
            if 1.0 <= p.Z < p.d - 1.0 and rune in (1, 3) and _h(int(a), p.face == 'top', 7) > 0.35:
                return (90, 210, 200, 255), GLOW
            k -= 0.6
    # broken, blackened ends
    if p.name == 'collar' and ((p.h > 20 and p.h < 22 and p.Y < 1.5) or (p.w == 8 and p.X < 1.5)):
        return _pick(GOLD, 0), None
    k += (_h(p.u, p.v, 3) - 0.5) * 0.6
    return _pick(GOLD, k), None


def _chain(p):
    link = int(p.Y) % 4
    if link == 3:
        return _pick(IRON, 0), None
    return _pick(GOLD, 2.0 + _side_light(p) + (link == 0)), None


def _fluke(p):
    if p.face in ('west', 'east'):
        # big fan: rays radiate from the stock (front, mid height); outline lobed at the back
        dy = p.Y - p.h / 2.0
        r = math.sqrt((p.Z + 1.0) ** 2 + (dy * 0.7) ** 2)
        ang = math.atan2(dy, p.Z + 1.0)
        lobe = 20.0 + 2.5 * math.cos(ang * 6.0)
        # notch in the middle of the trailing edge
        if abs(dy) < 2.0 and p.Z > 17.0:
            return None, None
        if r > lobe or abs(dy) > 4.0 + p.Z * 0.9:
            return None, None
        across = ang * 12.0 + 30.0
        c, g = membrane(p, r, across, lobe, p.h, 13, rays=3.0)
        if r > lobe - 1.0:
            c, g = ((100, 220, 215, 255), GLOW) if (int(across) % 3 == 0) else (_pick(RAY, 3), None)
        elif r > lobe - 3.0 and int(across) % 6 == 2:
            c, g = (150, 110, 230, 255), VIOLET
        return c, g
    return _pick(RAY, 2), None


def _side_fluke(p):
    return _side_fin(p, 17)


def _leviathan_texel(p):
    n = p.name
    if n == 'skull':
        return _skull(p)
    if n == 'snout':
        return _snout(p)
    if n == 'upper_teeth':
        return _teeth(p, down=True)
    if n == 'lower_teeth':
        return _teeth(p, down=False)
    if n == 'jaw':
        return _jaw(p)
    if n == 'eye':
        return _eye(p)
    if n == 'brow':
        return _brow(p)
    if n == 'horn':
        return _horn(p)
    if n == 'horn_tip':
        return _horn(p, tip=True)
    if n == 'fang':
        return _fang(p)
    if n == 'barbel':
        return _barbel(p)
    if n == 'crest':
        return _crest(p)
    if n == 'frill':
        return _frill(p)
    if n == 'chin':
        return _chin(p)
    if n == 'seg_a':
        return _segment(p, 61)
    if n == 'seg_b':
        return _segment(p, 67)
    if n == 'stock':
        c, g = _segment(p, 71)
        return c, g
    if n == 'fin_a':
        return _dorsal(p, 2)
    if n == 'fin_b':
        return _dorsal(p, 4)
    if n == 'tail_fin':
        return _dorsal(p, 6)
    if n == 'side_fin':
        return _side_fin(p, 3)
    if n == 'collar':
        return _collar(p)
    if n == 'chain':
        return _chain(p)
    if n == 'fluke':
        return _fluke(p)
    if n == 'side_fluke':
        return _side_fluke(p)
    raise ValueError(n)


def leviathan_texture():
    tw, th, boxes = javamodel.parse(LEVIATHAN_JAVA)
    base, glow = _paint(tw, th, boxes, _leviathan_texel)
    tex.save(base, M, 'entity/leviathan/leviathan')
    tex.save(glow, M, 'entity/leviathan/leviathan_glow')
    return base, glow


# ------------------------------------------------------------------------------------------------- tideling
T_BACK = tex.ramp('#2a6a78', 5, 0.40, 0.05, 0.08)
T_BELLY = tex.ramp('#a9cdb8', 4, 0.30, 0.03, 0.05)


def _tideling_texel(p):
    n = p.name
    light = _side_light(p)
    if n in ('body', 'head', 'tail'):
        if p.face == 'bottom':
            return _pick(T_BELLY, 1.8 + (0.6 if int(p.Z) % 3 == 0 else 0.0)), None
        a, b = (p.Z, p.Y) if p.face in ('west', 'east') else (p.X, p.Z if p.face == 'top' else p.Y)
        s, sid = scales(a, b, 3.0, 2.0, 81 + len(n))
        fy = p.fy if p.face in ('west', 'east', 'north', 'south') else 0.0
        if fy > 0.62:
            return _pick(T_BELLY, 2.0 + s * 0.5 - (1.0 - fy)), None
        c = _pick(T_BACK, 2.3 + s * 0.55 + light * 0.8 + (sid - 0.5) * 0.5 - fy)
        if n == 'head':
            if p.face in ('west', 'east') and 1.0 <= p.Z <= 3.0 and 1.0 <= p.Y <= 2.0:
                return (190, 255, 245, 255), GLOW_CORE        # eye
            if p.face in ('west', 'east') and 0.0 <= p.Z <= 4.0 and 0.0 <= p.Y <= 3.0:
                return EYE_SOCKET, None
            if p.face == 'north' and p.Y >= p.h - 1.0:
                return _pick(GUM, 1), None
            if p.face == 'bottom':
                return _pick(GUM, 2), None
        if n == 'body' and p.face in ('west', 'east') and abs(p.Y - 3.5) < 0.6 and int(p.Z) % 3 == 1:
            return (90, 200, 200, 255), GLOW
        return c, None
    if n == 'jaw':
        if p.face == 'top':
            edge = min(p.X, p.w - 1.0 - p.X, p.Z)
            return (_pick(BONE, 3), None) if edge < 1.0 and int(p.X + p.Z) % 2 == 0 else (_pick(GUM, 2), None)
        if p.face == 'bottom':
            return _pick(T_BELLY, 1.5), None
        if p.Y < 1.0:
            return (_pick(BONE, 3), None) if int(p.X + p.Z) % 2 == 0 else (_pick(GUM, 0), None)
        return _pick(T_BELLY, 1.2 + light), None
    if n == 'fang':
        return _pick(BONE, 3.3 + light - p.fy), None
    if n == 'spine':
        if p.face in ('west', 'east'):
            spike = p.Z % 2.7
            top = p.h * (0.3 + 0.7 * (1.0 - spike / 2.7))
            if p.h - p.Y > top + 0.4:
                return None, None
            return (_pick(RAY, 2), GLOW_DIM) if p.Y < 1.0 else (_pick(FIN, 1.5 + (spike < 1.0)), None)
        return _pick(RAY, 2), None
    if n == 'pec':
        if p.face in ('top', 'bottom'):
            return membrane(p, p.X, p.Z, p.w, p.d, 3, rays=2.0)
        return _pick(RAY, 1), None
    if n == 'tailfin':
        if p.face in ('west', 'east'):
            dy = abs(p.Y - p.h / 2.0)
            if p.Z > 3.0 and dy < 1.5:
                return None, None              # forked
            if dy > 2.0 + p.Z * 0.75:
                return None, None
            return membrane(p, p.Z, p.Y, p.d, p.h, 5, rays=2.0)
        return _pick(RAY, 2), None
    raise ValueError(n)


_tideling_texel.names = {(0, 0): 'body', (18, 18): 'head', (0, 30): 'jaw', (44, 30): 'fang', (0, 18): 'spine',
                         (30, 30): 'pec', (46, 18): 'tail', (36, 0): 'tailfin'}


def tideling_texture():
    tw, th, boxes = javamodel.parse(TIDELING_JAVA)
    base, glow = _paint(tw, th, boxes, _tideling_texel)
    tex.save(base, M, 'entity/leviathan/tideling')
    tex.save(glow, M, 'entity/leviathan/tideling_glow')
    return base, glow


# ------------------------------------------------------------------------------------------------- brine bolt
def _bolt_texel(p):
    if p.name == 'core':
        k = 1.0 - (abs(p.fx - 0.5) + abs(p.fy - 0.5)) * 0.8
        return art.lerp((120, 255, 240, 255), (235, 255, 252, 255), k), None
    # shell: translucent brine with brighter rim and droplets
    edge = min(p.X, p.w - 1 - p.X, p.Y, p.h - 1 - p.Y) if p.face in ('north', 'south', 'west', 'east') else 1
    a = 150 if edge < 1 else 95
    c = art.lerp((40, 190, 190, 255), (110, 240, 225, 255), 0.5 + 0.5 * math.sin(p.i * 1.7 + p.j * 2.3))
    if _h(p.i, p.j, len(p.face)) > 0.86:
        c, a = (200, 255, 250, 255), 200
    return (c[0], c[1], c[2], a), None


_bolt_texel.names = {(0, 0): 'shell', (0, 12): 'core'}


def bolt_texture():
    tw, th, boxes = javamodel.parse(BOLT_JAVA)
    base, _ = _paint(tw, th, boxes, _bolt_texel)
    tex.save(base, M, 'entity/leviathan/brine_bolt')
    return base


def generate():
    return leviathan_texture(), tideling_texture(), bolt_texture()
