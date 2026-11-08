"""Extruded sprites for block models: one element per horizontal run of opaque pixels, like vanilla extrudes item
sprites. Cut-out shapes (a ship's wheel, a jaw, a shell) then get side faces exactly along their outline, coloured
with the pixel at that edge (or with a side strip of the texture), instead of a box whose sides sample transparent or
unrelated parts of the texture.

    from bclib import extrude
    els = extrude.vertical('raft:block/ship_wheel', '#wheel', z=(13, 14))          # stands upright, front north
    els = extrude.flat('raft:block/seashells', '#shell', (0, 0, 4, 4), at=(6, 0, 8), height=1.5,
                       side_row=6)                                                # lies on the floor

Elements are plain model JSON dicts ({'from', 'to', 'faces'}); pass rotation= to rotate all of them together.
"""
from PIL import Image

from .paths import find_texture


def _opaque(ident):
    p = find_texture(ident)
    if p is None:
        raise FileNotFoundError(ident)
    im = Image.open(p).convert('RGBA')
    w = im.width
    px = im.load()
    return lambda u, v: 0 <= u < w and 0 <= v < w and px[u, v][3] >= 128


def _runs(op, region):
    u0, v0, u1, v1 = region
    for v in range(v0, v1):
        u = u0
        while u < u1:
            if op(u, v):
                s = u
                while u < u1 and op(u, v):
                    u += 1
                yield v, s, u - 1
            else:
                u += 1


def _covered(op, v, c0, c1, region):
    """True when the row v (inside the region) is opaque over all columns c0..c1."""
    u0, v0, u1, v1 = region
    return v0 <= v < v1 and all(op(u, v) for u in range(c0, c1 + 1))


def _el(frm, to, faces, rotation, light):
    e = {'from': [round(x, 4) for x in frm], 'to': [round(x, 4) for x in to]}
    if rotation:
        e['rotation'] = rotation
    if light:
        e['light_emission'] = light
    e['faces'] = faces
    return e


def vertical(texture_id, var, region=(0, 0, 16, 16), at=None, z=(7.5, 8.5), back=True, rotation=None, light=None,
             cull=None, front='north'):
    """Sprite standing upright. front='north': the north face shows the picture unmirrored (seen from the north),
    the south face mirrored; front='south' the other way round.
    region: texture pixels (u0, v0, u1, v1); at: model (x, y) of the region's top-left corner as seen from the front
    (default: the same place as the pixels, pixel v at y = 16 - v and, for front north, pixel u at x = 16 - u)."""
    op = _opaque(texture_id)
    u0, v0, u1, v1 = region
    north = front == 'north'
    if at is None:
        at = (16 - u0 if north else u0, 16 - v0)
    ax, ay = at                                       # x of the region's left edge as seen from the front
    z0, z1 = z
    out = []
    for v, c0, c1 in _runs(op, region):
        if north:
            x_lo, x_hi = ax - (c1 + 1 - u0), ax - (c0 - u0)
        else:
            x_lo, x_hi = ax + (c0 - u0), ax + (c1 + 1 - u0)
        y_hi = ay - (v - v0)
        y_lo = y_hi - 1
        fwd = [c0, v, c1 + 1, v + 1]
        rev = [c1 + 1, v, c0, v + 1]
        lo_px = [c1, v, c1 + 1, v + 1] if north else [c0, v, c0 + 1, v + 1]     # pixel at the low-x end
        hi_px = [c0, v, c0 + 1, v + 1] if north else [c1, v, c1 + 1, v + 1]
        f = {front: {'uv': fwd, 'texture': var}}
        if back:
            f['south' if north else 'north'] = {'uv': rev, 'texture': var}
        if not _covered(op, v - 1, c0, c1, region):
            f['up'] = {'uv': rev if north else fwd, 'texture': var}
        if not _covered(op, v + 1, c0, c1, region):
            f['down'] = {'uv': rev if north else fwd, 'texture': var}
        f['west'] = {'uv': lo_px, 'texture': var}
        f['east'] = {'uv': hi_px, 'texture': var}
        for d in list(f):
            if cull and d in cull:
                f[d]['cullface'] = cull[d]
        out.append(_el([x_lo, y_lo, z0], [x_hi, y_hi, z1], f, rotation, light))
    return out


def flat(texture_id, var, region, at, height=1.0, side_row=None, rotation=None, light=None, bottom=True):
    """Sprite lying flat (seen from above: pixel u along +x, v along +z), extruded `height` up from at=(x, y, z).
    Side faces show the pixel at that edge, or the pixel of the same column in texture row side_row."""
    op = _opaque(texture_id)
    u0, v0, u1, v1 = region
    ax, ay, az = at
    out = []
    for v, c0, c1 in _runs(op, region):
        x0 = ax + (c0 - u0)
        x1 = ax + (c1 + 1 - u0)
        zz0 = az + (v - v0)
        zz1 = zz0 + 1
        sr = side_row if side_row is not None else v
        f = {'up': {'uv': [c0, v, c1 + 1, v + 1], 'texture': var}}
        if bottom:
            f['down'] = {'uv': [c0, v, c1 + 1, v + 1], 'texture': var, 'cullface': 'down'} if ay == 0 else \
                {'uv': [c0, v, c1 + 1, v + 1], 'texture': var}
        if not _covered(op, v - 1, c0, c1, region):
            f['north'] = {'uv': [c1 + 1, sr, c0, sr + 1], 'texture': var}
        if not _covered(op, v + 1, c0, c1, region):
            f['south'] = {'uv': [c0, sr, c1 + 1, sr + 1], 'texture': var}
        f['west'] = {'uv': [c0, sr, c0 + 1, sr + 1], 'texture': var}
        f['east'] = {'uv': [c1, sr, c1 + 1, sr + 1], 'texture': var}
        out.append(_el([x0, ay, zz0], [x1, ay + height, zz1], f, rotation, light))
    return out
