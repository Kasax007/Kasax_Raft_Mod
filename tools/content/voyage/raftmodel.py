"""The Expedition Raft: box geometry, 128x128 texture, Java geometry class and a software preview.

The geometry is defined once here. gen.py writes
- src/main/java/net/kasax/raft/content/voyage/client/ExpeditionRaftGeometry.java (LayerDefinitions),
- textures/entity/expedition_raft.png (+ _glow.png for the lantern flames),
so texture regions and boxes can never drift apart.

Model space (like every entity model): 1 unit = 1 px = 1/16 block, y points down, the bow is -z, +x is port (left).
y = 0 is the deck surface (= top of the 3.0 x 0.6 hitbox); the renderer translates by 0.6 blocks.
"""
import math
import os

from PIL import Image, ImageDraw

from bclib import tex
from bclib.paths import ROOT

TW, TH = 128, 128
JAVA_OUT = os.path.join(ROOT, 'src', 'main', 'java', 'net', 'kasax', 'raft', 'content', 'voyage', 'client',
                        'ExpeditionRaftGeometry.java')


# ----------------------------------------------------------------------------------------------- geometry
class Box:
    def __init__(self, region, x, y, z, w, h, d):
        self.region, self.x, self.y, self.z, self.w, self.h, self.d = region, x, y, z, w, h, d


class Part:
    def __init__(self, name, pivot=(0, 0, 0), rot=(0, 0, 0)):
        self.name, self.pivot, self.rot = name, tuple(pivot), tuple(rot)
        self.boxes, self.children = [], []

    def box(self, region, x, y, z, w=None, h=None, d=None):
        r = REGIONS[region]
        w = r['w'] if w is None else w
        h = r['h'] if h is None else h
        d = r['d'] if d is None else d
        if (h, d) != (r['h'], r['d']) or w > r['w'] or (w != r['w'] and not r.get('stretch')):
            raise ValueError('box %s %sx%sx%s does not fit region %s' % (region, w, h, d, r))
        self.boxes.append(Box(region, x, y, z, w, h, d))
        return self

    def child(self, name, pivot=(0, 0, 0), rot=(0, 0, 0)):
        p = Part(name, pivot, rot)
        self.children.append(p)
        return p


# region name -> size (w, h, d) and material. 'stretch' regions may be used by shorter boxes (uniform material).
REGIONS = {}


def region(name, w, h, d, mat, **kw):
    REGIONS[name] = dict(w=w, h=h, d=d, mat=mat, **kw)


region('board_a', 48, 2, 6, 'deck', seed=3)
region('log', 48, 6, 6, 'log')
region('mast', 3, 30, 3, 'mast')
region('collar', 5, 3, 5, 'rope')
region('yard', 40, 2, 2, 'spar')
region('sail', 32, 24, 1, 'canvas')
region('flag', 7, 4, 1, 'flag')
region('rope', 32, 1, 1, 'rope', stretch=True)
region('chest', 12, 8, 10, 'chest')
region('lid', 12, 3, 10, 'chest_lid')
region('latch', 1, 3, 2, 'brass')
region('barrel', 8, 12, 8, 'barrel')
region('stool', 8, 3, 6, 'spar')
region('coil', 6, 2, 6, 'coil')
region('crate', 10, 10, 10, 'crate')
region('crate_small', 8, 8, 8, 'crate')
region('rudder_post', 2, 18, 2, 'spar')
region('rudder_blade', 1, 10, 7, 'spar')
region('tiller', 2, 2, 12, 'spar')
region('clamp', 4, 5, 3, 'iron')
region('cowl', 7, 9, 8, 'cowl')
region('shaft', 2, 15, 2, 'steel')
region('gearbox', 3, 3, 5, 'cowl_low')
region('handle', 2, 2, 6, 'steel')
region('hub', 2, 2, 2, 'brass')
region('blade_v', 1, 8, 1, 'brass')
region('blade_h', 8, 1, 1, 'brass')
region('davit', 9, 2, 2, 'spar')
region('davit_post', 2, 16, 2, 'spar')
region('drum', 6, 5, 5, 'drum')
region('plate', 1, 8, 6, 'spar')
region('crank', 1, 1, 3, 'iron')
region('anchor_rope', 1, 1, 1, 'rope')
region('ring', 3, 3, 1, 'iron')
region('shank', 2, 12, 2, 'iron')
region('stock', 2, 2, 8, 'iron')
region('arms', 12, 2, 2, 'iron')
region('fluke', 2, 4, 2, 'iron')
region('post', 2, 24, 2, 'spar')
region('arm', 5, 1, 1, 'iron', stretch=True)
region('lantern', 4, 5, 4, 'lantern')
region('cap', 3, 1, 3, 'iron')
region('oar_shaft', 22, 1, 1, 'spar')
region('oar_blade', 7, 4, 1, 'oar')

# The lines of the rigging: (part name, start, end) in model space; mast top is (0, -60, -2).
MAST_Z = -2.0
DAVIT_Y = -16.0
RIGGING = [
    ('shroud_pl', (1.0, -58, MAST_Z - 1), (23.0, 0, -7)),
    ('shroud_pr', (-1.0, -58, MAST_Z - 1), (-23.0, 0, -7)),
    ('shroud_sl', (1.0, -58, MAST_Z + 1), (23.0, 0, 5)),
    ('shroud_sr', (-1.0, -58, MAST_Z + 1), (-23.0, 0, 5)),
    ('forestay', (0.0, -58, MAST_Z - 1.5), (0.0, -1, -23.5)),
]
# Lantern posts (corners) and the rope rails between them.
CORNERS = [(22.5, -22.5), (-22.5, -22.5), (22.5, 22.5), (-22.5, 22.5)]


def line_part(parent, name, a, b):
    """A 1x1 rope from a to b: part at a, boxes along local +x, rotated so +x points to b."""
    dx, dy, dz = b[0] - a[0], b[1] - a[1], b[2] - a[2]
    length = math.sqrt(dx * dx + dy * dy + dz * dz)
    ux, uy, uz = dx / length, dy / length, dz / length
    yrot = -math.asin(max(-1.0, min(1.0, uz)))
    zrot = math.atan2(uy, ux)
    p = parent.child(name, a, (0.0, yrot, zrot))
    seg = REGIONS['rope']['w']
    x = 0.0
    while x < length - 0.01:
        w = min(seg, length - x)
        p.box('rope', x, -0.5, -0.5, w=round(w, 3))
        x += w
    return p


def build():
    root = Part('root')

    # --- hull: deck boards across the raft on five logs running bow to stern
    hull = root.child('hull')
    for k in range(8):
        z0 = -24 + 6 * k
        hull.box('board_a', (-25 if k % 2 == 0 else -23), 0, z0)
    logs = hull.child('logs', (0, 0, 0), (0, math.pi / 2, 0))
    for c in (-19.5, -9.75, 0.0, 9.75, 19.5):
        logs.box('log', -24, 2, c - 3)
    # helm stool, water barrel, rope coil and the built-in sea chest
    hull.box('stool', -4, -3, 13)
    hull.box('barrel', 13, -12, 14)
    hull.box('coil', -21, -2, 8)
    chest = hull.child('chest', (13, 0, -15), (0, 0.08, 0))
    chest.box('chest', -6, -8, -5)
    chest.box('lid', -6, -11, -5)
    chest.box('latch', -7, -9, -1)

    # --- rudder with tiller (turns with steering)
    rudder = root.child('rudder', (0, -1, 25))
    rudder.box('rudder_post', -1, -4, -1)
    rudder.box('rudder_blade', -0.5, 4, 1)
    tiller = rudder.child('tiller', (0, -3, 0), (0.1, 0.42, 0))
    tiller.box('tiller', -1, -1, -12)

    # --- sail upgrade: mast, yard, boom, sail, rigging, pennant
    rig = root.child('rig', (0, 0, MAST_Z))
    rig.box('mast', -1.5, -60, -1.5)
    rig.box('mast', -1.5, -30, -1.5)
    rig.box('collar', -2.5, -3, -2.5)
    rig.box('yard', -20, -58, -3.5)
    rig.box('yard', -20, -31, -3.5)
    sail = rig.child('sail', (0, -56, -2.5))
    sail.box('sail', -16, 1, -0.5)
    flag = rig.child('flag', (0, -60, 0))
    flag.box('flag', 1.5, -4, -0.5)
    rigging = root.child('rigging')
    for name, a, b in RIGGING:
        line_part(rigging, name, a, b)

    # --- engine upgrade: outboard motor on the starboard stern, spinning propeller
    engine = root.child('engine', (-16, 0, 24))
    engine.box('clamp', -2, -3, -2)
    engine.box('cowl', -3.5, -12, 1)
    engine.box('shaft', -1, -3, 4)
    engine.box('gearbox', -1.5, 10, 3)
    engine.box('handle', -1, -9, -5)
    prop = engine.child('propeller', (0, 11.5, 8.5))
    prop.box('hub', -1, -1, 0)
    prop.box('blade_v', -0.5, -4, 0.5)
    prop.box('blade_h', -4, -0.5, 0.5)

    # --- anchor upgrade: winch, davit, rope and anchor (the anchor drops when it is set)
    gear = root.child('anchor_gear')
    gear.box('plate', 15, -8, 6.5)
    gear.box('plate', 22, -8, 6.5)
    gear.box('drum', 16, -7, 7)
    gear.box('crank', 23, -6, 7.5)
    gear.box('davit_post', 22, DAVIT_Y, 9)
    gear.box('davit', 22, DAVIT_Y, 9)
    line_part(gear, 'winch_rope', (19.0, -7.2, 9.5), (29.0, DAVIT_Y - 0.2, 10.0))
    rope = root.child('anchor_rope', (29, DAVIT_Y + 2, 10))
    rope.box('anchor_rope', -0.5, 0, -0.5)
    anchor = root.child('anchor', (29, DAVIT_Y + 2, 10), (0, math.pi / 2, 0))
    anchor.box('ring', -1.5, 0, -0.5)
    anchor.box('shank', -1, 2, -1)
    anchor.box('stock', -1, 3, -4)
    anchor.box('arms', -6, 12, -1)
    anchor.box('fluke', -7, 9, -1)
    anchor.box('fluke', 5, 9, -1)

    # --- crate upgrade: two lashed cargo crates on the starboard bow
    crates = root.child('crates', (-14, 0, -15))
    crates.box('crate', -5, -10, -5)
    top = crates.child('crate_top', (0.5, -10, 0), (0, 0.35, 0))
    top.box('crate_small', -4, -8, -4)

    # --- lantern upgrade: four corner posts with hanging lanterns and rope rails
    lanterns = root.child('lanterns')
    for i, (cx, cz) in enumerate(CORNERS):
        lanterns.box('post', cx - 1, -24, cz - 1)
        sx = 1 if cx > 0 else -1
        lanterns.box('arm', cx - 0.5 if sx > 0 else cx - 4.5, -24, cz - 0.5, w=5)
        lamp = lanterns.child('lantern_%d' % i, (cx + 4 * sx, -23, cz))
        lamp.box('cap', -1.5, 1, -1.5)
        lamp.box('lantern', -2, 2, -2)
    line_part(lanterns, 'rail_port', (22.5, -15, -21.5), (22.5, -15, 21.5))
    line_part(lanterns, 'rail_starboard', (-22.5, -15, -21.5), (-22.5, -15, 21.5))
    line_part(lanterns, 'rail_bow', (21.5, -15, -22.5), (-21.5, -15, -22.5))

    # --- oars (paddling without sail/engine power); local +x points outwards
    for name, x, sgn in (('left_oar', 24.5, 1), ('right_oar', -24.5, -1)):
        oar = root.child(name, (x, -4, 3), (0, 0 if sgn > 0 else math.pi, 0))
        oar.box('oar_shaft', -6, -0.5, -0.5)
        oar.box('oar_blade', 10, -2, -0.5)
    return root


# ----------------------------------------------------------------------------------------------- packing
def pack():
    """Skyline packer: assigns texOffs (u, v) to every region. Returns {name: (u, v)}."""
    items = []
    for name, r in REGIONS.items():
        nw = int(math.ceil(2 * (r['w'] + r['d'])))
        nh = int(math.ceil(r['d'] + r['h']))
        items.append((name, nw, nh))
    items.sort(key=lambda t: (-t[2], -t[1]))
    sky = [0] * TW
    pos = {}
    for name, nw, nh in items:
        best = None
        for u in range(0, TW - nw + 1):
            v = max(sky[u:u + nw])
            key = (v + nh, sum(v - s for s in sky[u:u + nw]))
            if v + nh <= TH and (best is None or key < best[0]):
                best = (key, u, v)
        if best is None:
            raise ValueError('texture full at region %s (%dx%d)' % (name, nw, nh))
        _, u, v = best
        best = (u, v)
        for k in range(u, u + nw):
            sky[k] = v + nh
        pos[name] = best
    return pos


# ----------------------------------------------------------------------------------------------- painting
def _h(*a):
    x = 2166136261
    for v in a:
        x = ((x ^ (int(v) & 0xffffffff)) * 16777619) & 0xffffffff
    x ^= x >> 13
    x = (x * 1274126177) & 0xffffffff
    return (x & 0xffff) / 65536.0


R_DECK = tex.ramp('#8a7657', 6, spread=0.5)
R_LOG = tex.ramp('#5f4f3b', 5, spread=0.42)
R_END = tex.ramp('#a88d62', 4, spread=0.35)
R_SPAR = tex.ramp('#9a7746', 6, spread=0.5)
R_MAST = tex.ramp('#8c6a3e', 6, spread=0.5)
R_CHEST = tex.ramp('#6e4a2a', 5, spread=0.45)
R_CRATE = tex.ramp('#b48d58', 5, spread=0.45)
R_BARREL = tex.ramp('#8e6438', 5, spread=0.45)
R_ROPE = tex.ramp('#b0925a', 5, spread=0.5)
R_IRON = tex.ramp('#565c66', 5, spread=0.45)
R_STEEL = tex.ramp('#8a9098', 5, spread=0.45)
R_BRASS = tex.ramp('#c79a3a', 5, spread=0.5)
R_CANVAS = tex.ramp('#e2dccb', 4, spread=0.18)
R_RED = tex.ramp('#b3382f', 4, spread=0.3)
R_COWL = tex.ramp('#b8352c', 5, spread=0.42)
R_WHITE = tex.ramp('#e3dfd4', 4, spread=0.2)
GLASS = [tex.rgb('#ffe9a8'), tex.rgb('#ffd36b'), tex.rgb('#f5a93a')]


def _faces(u, v, w, h, d):
    """MC cube net: (face, u0, v0, fw, fh). top = visual top (y-), side faces with j going down."""
    return [('top', u + d, v, w, d), ('bottom', u + d + w, v, w, d), ('west', u, v + d, d, h),
            ('north', u + d, v + d, w, h), ('east', u + d + w, v + d, d, h), ('south', u + 2 * d + w, v + d, w, h)]


def _axes(face):
    """The box axes along the face's (i, j)."""
    return {'top': ('x', 'z'), 'bottom': ('x', 'z'), 'west': ('z', 'y'), 'east': ('z', 'y'),
            'north': ('x', 'y'), 'south': ('x', 'y')}[face]


def _pick(ramp, t):
    t = max(0.0, min(0.999, t))
    return ramp[int(t * len(ramp))]


def wood(ramp, f, i, j, fw, fh, dims, seed, plank_seams=False):
    """Wood with grain along the box's longest axis, end grain on the faces across it."""
    long_axis = max(dims, key=lambda a: dims[a])
    ai, aj = _axes(f)
    if long_axis not in (ai, aj) and dims[long_axis] > 2.5:
        # end grain: rings around the face centre
        cx, cy = (fw - 1) / 2, (fh - 1) / 2
        r = math.sqrt((i - cx) ** 2 + (j - cy) ** 2)
        rmax = max(1.0, min(fw, fh) / 2)
        if r > rmax - 0.75:
            return ramp[1]
        ring = int(r * 1.3 + _h(seed, 7) * 2) % 2
        return R_END[1 + ring] if ramp is not R_IRON else ramp[2]
    along, across = (i, j) if long_axis == ai else (j, i)
    n_across = fh if long_axis == ai else fw
    # streaks across the grain, slow variation along it
    s = _h(seed, across, 1) * 0.55 + _h(seed, across, along // 5, 2) * 0.25 + _h(seed, across, along // 2, 3) * 0.12
    t = 0.18 + s * 0.62
    if across == 0 and f in ('top', 'north', 'west'):
        t += 0.22
    if across == n_across - 1 and n_across > 2:
        t -= 0.22
    if plank_seams:
        seam = (along + int(_h(seed, across // 6 if n_across > 4 else 0, 9) * 16)) % 16
        if seam == 0:
            return ramp[0]
        if seam in (1, 15) and across in (1, n_across - 2) and f == 'top':
            return R_IRON[1]  # nail heads next to the butt joint
    if _h(seed, along // 3, across // 2, 5) > 0.97:
        t -= 0.35  # knot
    return _pick(ramp, t)


def rope(f, i, j, fw, fh, dims, seed):
    k = (i + j * 2) % 4
    return [R_ROPE[1], R_ROPE[2], R_ROPE[3], R_ROPE[2]][k] if (i // 1) % 1 == 0 else R_ROPE[2]


def paint_texel(mat, f, i, j, fw, fh, dims, seed):
    if mat == 'deck':
        return wood(R_DECK, f, i, j, fw, fh, dims, seed, plank_seams=True)
    if mat in ('spar',):
        return wood(R_SPAR, f, i, j, fw, fh, dims, seed + 17)
    if mat == 'mast':
        if f in ('top', 'bottom'):
            return R_MAST[3] if 0 < i < fw - 1 and 0 < j < fh - 1 else R_MAST[1]
        col = [R_MAST[4], R_MAST[3], R_MAST[2]][min(2, i)]
        if j % 9 == 4 and _h(seed, j, i) > 0.4:
            col = R_MAST[1]
        if 2 <= j <= 4 or 30 <= j <= 31:
            col = [R_ROPE[3], R_ROPE[2]][(i + j) % 2]  # rope bands
        return col
    if mat == 'log':
        if f in ('west', 'east'):
            cx, cy = (fw - 1) / 2, (fh - 1) / 2
            r = max(abs(i - cx), abs(j - cy)) * 0.6 + math.sqrt((i - cx) ** 2 + (j - cy) ** 2) * 0.4
            if r >= 2.9:
                return R_LOG[1] if (i + j) % 3 else R_LOG[0]
            return [R_END[3], R_END[2], R_END[1], R_END[2]][int(r * 1.1) % 4]
        across = j if f in ('north', 'south') else (fh - 1 - j)
        n = fh
        groove = _h(seed, across, i // 7, 4) > 0.72
        t = 0.3 + _h(seed, across, 1) * 0.45 + _h(seed, across, i // 4, 6) * 0.2
        if groove:
            t -= 0.3
        if f == 'top' or (f in ('north', 'south') and across < 2):
            t += 0.12
        if (i + 7) % 24 in (0, 1) and f != 'bottom':
            return [R_ROPE[3], R_ROPE[1]][i % 2] if (across + i) % 2 == 0 else R_ROPE[2]  # lashings
        return _pick(R_LOG, t)
    if mat == 'rope':
        return rope(f, i, j, fw, fh, dims, seed)
    if mat == 'coil':
        if f == 'top':
            cx = (fw - 1) / 2
            r = math.sqrt((i - cx) ** 2 + (j - cx) ** 2)
            if r < 0.9:
                return R_ROPE[0]
            return [R_ROPE[3], R_ROPE[1]][int(r) % 2]
        return [R_ROPE[3], R_ROPE[2], R_ROPE[1]][(i + j) % 3]
    if mat == 'drum':
        if f in ('west', 'east'):
            return R_SPAR[1] if (i in (0, fw - 1) or j in (0, fh - 1)) else R_SPAR[3]
        return [R_ROPE[3], R_ROPE[2], R_ROPE[1]][(i + (j if f != 'top' else 0)) % 3]
    if mat in ('iron', 'steel', 'brass'):
        ramp = {'iron': R_IRON, 'steel': R_STEEL, 'brass': R_BRASS}[mat]
        t = 0.45
        if f in ('top',) or (i == 0 and f in ('north', 'west')):
            t = 0.8
        if j == fh - 1 and fh > 2 and f not in ('top', 'bottom'):
            t = 0.2
        if mat == 'steel' and f not in ('top', 'bottom') and i == fw // 2:
            t = 0.85
        if _h(seed, i, j, f == 'top') > 0.9 and mat == 'iron':
            t -= 0.2
        return _pick(ramp, t)
    if mat == 'chest' or mat == 'chest_lid':
        edge = i in (0, fw - 1) or (mat == 'chest' and j == fh - 1) or (mat == 'chest_lid' and j == 0 and f != 'top')
        if f in ('top', 'bottom'):
            edge = i in (0, fw - 1) or j in (0, fh - 1)
        if edge:
            return R_BRASS[2] if (i + j) % 5 else R_BRASS[3]
        if f in ('top', 'bottom'):
            return wood(R_CHEST, f, i, j, fw, fh, {'x': 12, 'y': 1, 'z': 10}, seed + 31)
        c = wood(R_CHEST, f, i, j, fw, fh, {'x': fw, 'y': 0.5, 'z': fw}, seed + 31)
        if mat == 'chest' and j % 4 == 0 and j > 0:
            c = R_CHEST[0]
        return c
    if mat == 'crate':
        border = i in (0, fw - 1) or j in (0, fh - 1)
        if border:
            return R_CRATE[1] if (i + j) % 2 else R_CRATE[0]
        diag = abs((i - 1) - (j - 1) * (fw - 3) / max(1, fh - 3)) < 1.0
        if diag and f not in ('top', 'bottom'):
            return R_CRATE[2]
        if f == 'top' and j == fh // 2:
            return R_ROPE[2]
        return wood(R_CRATE, f, i, j, fw, fh, {'x': fw, 'y': 0.5, 'z': 0.5} if f != 'top' else {'x': 9, 'y': 0, 'z': 1},
                    seed + 41)
    if mat == 'barrel':
        if f in ('top', 'bottom'):
            cx = (fw - 1) / 2
            r = max(abs(i - cx), abs(j - cx))
            if r > cx - 0.6:
                return R_BARREL[1]
            return R_BARREL[2] if i % 3 == 0 else R_BARREL[3]
        if j in (1, 2, fh - 3, fh - 2):
            return R_IRON[3] if j in (1, fh - 3) else R_IRON[1]
        t = 0.5 + (0.25 if i % 4 == 1 else 0) - (0.3 if i % 4 == 3 else 0) - (0.15 if j in (0, fh - 1) else 0)
        return _pick(R_BARREL, t + _h(seed, i, j // 3) * 0.15)
    if mat == 'canvas':
        if f not in ('north', 'south'):
            return R_CANVAS[1]
        ii = i if f == 'north' else fw - 1 - i
        if j == 0:
            return R_ROPE[2]
        if j == 1 and ii % 6 == 3:
            return R_IRON[1]  # grommets
        hem = ii in (0, fw - 1) or j == fh - 1
        red = 10 <= ii <= 13 or 18 <= ii <= 21
        base = R_RED if red else R_CANVAS
        t = 0.62 + _h(seed, ii, j // 2) * 0.12
        if j % 6 == 0:
            t -= 0.25  # horizontal seams
        if hem:
            t -= 0.35
        if 4 <= ii <= 8 and 14 <= j <= 18 and not red:
            t -= 0.12  # sewn patch
            if ii in (4, 8) or j in (14, 18):
                t -= 0.15
        return _pick(base, t)
    if mat == 'flag':
        if f in ('north', 'south'):
            return R_RED[2] if j in (0, fh - 1) or i > 4 else R_WHITE[2]
        return R_RED[1]
    if mat == 'cowl':
        if f == 'top':
            if 2 <= i <= 4 and 2 <= j <= 5:
                return R_IRON[1] if (i + j) % 2 else R_IRON[2]  # starter cap
            return R_COWL[3] if j > 0 else R_COWL[4]
        if j < 4:
            return R_COWL[3] if j > 0 else R_COWL[4]
        if j == 4:
            return R_WHITE[2]
        if j in (5, 6):
            if f in ('west', 'east') and i % 2 == 1 and 1 <= i <= fw - 2:
                return R_IRON[0]  # vents
            return R_WHITE[1]
        return R_COWL[1] if j < fh - 1 else R_COWL[0]
    if mat == 'cowl_low':
        return R_COWL[1] if j > 0 or f == 'top' else R_COWL[2]
    if mat == 'lantern':
        frame = i in (0, fw - 1) or j in (0, fh - 1)
        if f in ('top', 'bottom') or frame:
            return R_IRON[2] if frame and (i + j) % 2 else R_IRON[1]
        return GLASS[min(2, abs(j - fh // 2))]
    if mat == 'oar':
        if f in ('north', 'south'):
            edge = i == fw - 1 or j in (0, fh - 1)
            return R_SPAR[1] if edge else (R_SPAR[4] if i < 2 else R_SPAR[3])
        return R_SPAR[2]
    return (255, 0, 255, 255)


def texture(pos):
    img = Image.new('RGBA', (TW, TH), (0, 0, 0, 0))
    glow = Image.new('RGBA', (TW, TH), (0, 0, 0, 0))
    for name, r in REGIONS.items():
        u, v = pos[name]
        w, h, d = int(math.ceil(r['w'])), int(math.ceil(r['h'])), int(math.ceil(r['d']))
        dims = {'x': r['w'], 'y': r['h'], 'z': r['d']}
        seed = r.get('seed', sum(map(ord, name)))
        for f, u0, v0, fw, fh in _faces(u, v, w, h, d):
            for j in range(fh):
                for i in range(fw):
                    c = paint_texel(r['mat'], f, i, j, fw, fh, dims, seed)
                    img.putpixel((u0 + i, v0 + j), tuple(c))
                    if r['mat'] == 'lantern' and c in [tuple(g) for g in GLASS]:
                        glow.putpixel((u0 + i, v0 + j), tuple(c))
    return img, glow


# ----------------------------------------------------------------------------------------------- java
def _f(v):
    s = ('%.4f' % v).rstrip('0').rstrip('.')
    if s in ('-0', ''):
        s = '0'
    return s + 'F'


def java(root, pos):
    out = []
    counter = [0]

    def emit(part, parent_var):
        counter[0] += 1
        var = 'p%d' % counter[0]
        cubes = 'CubeListBuilder.create()'
        for b in part.boxes:
            u, v = pos[b.region]
            cubes += '.texOffs(%d, %d).addBox(%s, %s, %s, %s, %s, %s)' % (u, v, _f(b.x), _f(b.y), _f(b.z), _f(b.w), _f(b.h), _f(b.d))
        px, py, pz = part.pivot
        rx, ry, rz = part.rot
        if any(abs(a) > 1e-9 for a in part.rot):
            pose = 'PartPose.offsetAndRotation(%s, %s, %s, %s, %s, %s)' % tuple(_f(a) for a in (px, py, pz, rx, ry, rz))
        else:
            pose = 'PartPose.offset(%s, %s, %s)' % tuple(_f(a) for a in (px, py, pz))
        decl = ('PartDefinition %s = ' % var) if part.children else ''
        out.append('        %s%s.addOrReplaceChild("%s", %s, %s);' % (decl, parent_var, part.name, cubes, pose))
        for c in part.children:
            emit(c, var)

    for c in root.children:
        emit(c, 'root')
    body = '\n'.join(out)
    src = '''package net.kasax.raft.content.voyage.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Box geometry of the Expedition Raft. GENERATED by tools/content/voyage/raftmodel.py together with the texture
 * (textures/entity/expedition_raft.png) - change the geometry there and re-run the generator.
 */
@Environment(EnvType.CLIENT)
public final class ExpeditionRaftGeometry {
    public static final int TEXTURE_WIDTH = %d;
    public static final int TEXTURE_HEIGHT = %d;
    /** Model y of the anchor rope's top (the davit); the anchor hangs this far below the pivot of anchor_rope. */
    public static final float ANCHOR_PIVOT_Y = %s;

    private ExpeditionRaftGeometry() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
%s
        return LayerDefinition.create(mesh, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    /** Invisible depth-only box over the deck: hides the water surface inside the raft like the boat water patch. */
    public static LayerDefinition createWaterMask() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("water_mask", CubeListBuilder.create().texOffs(0, 0).addBox(-24.0F, 0.0F, -24.0F, 48.0F, 2.0F, 48.0F),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 0, 0);
    }
}
''' % (TW, TH, _f(DAVIT_Y + 2), body)
    with open(JAVA_OUT, 'w', encoding='utf-8') as f:
        f.write(src)
    return JAVA_OUT


# ----------------------------------------------------------------------------------------------- preview
def _rot(p, r):
    x, y, z = p
    rx, ry, rz = r
    c, s = math.cos(rx), math.sin(rx)
    y, z = y * c - z * s, y * s + z * c
    c, s = math.cos(ry), math.sin(ry)
    x, z = x * c + z * s, -x * s + z * c
    c, s = math.cos(rz), math.sin(rz)
    x, y = x * c - y * s, x * s + y * c
    return x, y, z


def preview(root, pos, img, out_png, views=((210, 28), (130, 22), (300, 12), (180, 75)), size=420):
    quads_all = []

    def walk(part, chain):
        chain = chain + [part]
        for b in part.boxes:
            u, v = pos[b.region]
            r = REGIONS[b.region]
            w, h, d = r['w'], r['h'], r['d']
            for f, u0, v0, fw, fh in _faces(u, v, int(math.ceil(w)), int(math.ceil(h)), int(math.ceil(d))):
                quads_all.append((chain, b, f, u0, v0, fw, fh))
        for c in part.children:
            walk(c, chain)

    for c in root.children:
        walk(c, [])

    def local(b, f, i, j):
        x0, y0, z0, x1, y1, z1 = b.x, b.y, b.z, b.x + b.w, b.y + b.h, b.z + b.d
        if f == 'top':
            return (x0 + i, y0, z1 - j)
        if f == 'bottom':
            return (x0 + i, y1, z1 - j)
        if f == 'west':
            return (x0, y0 + j, z1 - i)
        if f == 'north':
            return (x0 + i, y0 + j, z0)
        if f == 'east':
            return (x1, y0 + j, z0 + i)
        return (x1 - i, y0 + j, z1)

    def to_model(chain, p):
        for part in reversed(chain):
            p = _rot(p, part.rot)
            p = (p[0] + part.pivot[0], p[1] + part.pivot[1], p[2] + part.pivot[2])
        return p

    sheet = Image.new('RGBA', (size * len(views), size), (46, 92, 128, 255))
    for vi, (yaw, pitch) in enumerate(views):
        cy, sy = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
        cp, sp = math.cos(math.radians(pitch)), math.sin(math.radians(pitch))

        def cam(p):
            x, y, z = p[0], -p[1], -p[2]
            x, z = x * cy - z * sy, x * sy + z * cy
            y, z = y * cp - z * sp, y * sp + z * cp
            return x, y, z

        quads = []
        for chain, b, f, u0, v0, fw, fh in quads_all:
            # the face may be shorter than the region (stretch boxes)
            fwb = fw
            if f in ('top', 'bottom', 'north', 'south'):
                fwb = int(math.ceil(b.w))
            for j in range(fh):
                for i in range(fwb):
                    c = img.getpixel((u0 + i, v0 + j))
                    if c[3] < 20:
                        continue
                    cs = []
                    for di, dj in ((0, 0), (1, 0), (1, 1), (0, 1)):
                        ii, jj = min(i + di, b.w if f in ('top', 'bottom', 'north', 'south') else fw), j + dj
                        cs.append(cam(to_model(chain, local(b, f, ii, jj))))
                    shade = {'top': 1.0, 'bottom': 0.5, 'north': 0.8, 'south': 0.8, 'west': 0.62, 'east': 0.62}[f]
                    depth = sum(q[2] for q in cs) / 4
                    quads.append((depth, cs, tuple(int(c[k] * shade) for k in range(3)) + (255,)))
        quads.sort(key=lambda q: q[0])
        im = Image.new('RGBA', (size, size), (0, 0, 0, 0))
        dr = ImageDraw.Draw(im)
        sc = size / 90.0
        for _, cs, col in quads:
            dr.polygon([(size / 2 + q[0] * sc, size * 0.66 - q[1] * sc) for q in cs], fill=col)
        sheet.alpha_composite(im, (vi * size, 0))
    sheet.save(out_png)
    return out_png
