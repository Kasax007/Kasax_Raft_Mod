"""Shared pixel-art helpers of the marine module: 3D noise (consistent across box faces), cell spots, ramps."""
import math

from bclib import tex


def _h(ix, iy, iz, seed):
    n = (ix * 374761393 + iy * 668265263 + iz * 1274126177 + seed * 144665) & 0xffffffff
    n = ((n ^ (n >> 13)) * 1274126177) & 0xffffffff
    return ((n ^ (n >> 16)) & 0xffff) / 65535.0


def _s(t):
    return t * t * (3 - 2 * t)


def noise3(x, y, z, scale=4.0, seed=0):
    """Smooth value noise in [0,1] with lattice spacing `scale` (model units)."""
    x, y, z = x / scale, y / scale, z / scale
    ix, iy, iz = math.floor(x), math.floor(y), math.floor(z)
    fx, fy, fz = _s(x - ix), _s(y - iy), _s(z - iz)
    r = 0.0
    for dx in (0, 1):
        for dy in (0, 1):
            for dz in (0, 1):
                w = (fx if dx else 1 - fx) * (fy if dy else 1 - fy) * (fz if dz else 1 - fz)
                r += w * _h(ix + dx, iy + dy, iz + dz, seed)
    return r


def fbm3(x, y, z, scale=4.0, seed=0, octaves=2):
    total, amp, norm = 0.0, 1.0, 0.0
    for o in range(octaves):
        total += noise3(x, y, z, scale, seed + o * 31) * amp
        norm += amp
        amp *= 0.5
        scale /= 2.0
    return total / norm


def cells3(x, y, z, scale=3.0, seed=0, jitter=0.8):
    """Distance (in cell units) to the nearest jittered feature point and that point's random id (0..1)."""
    x, y, z = x / scale, y / scale, z / scale
    ix, iy, iz = math.floor(x), math.floor(y), math.floor(z)
    best, bid = 9.0, 0.0
    for dx in (-1, 0, 1):
        for dy in (-1, 0, 1):
            for dz in (-1, 0, 1):
                cx, cy, cz = ix + dx, iy + dy, iz + dz
                px = cx + 0.5 + (_h(cx, cy, cz, seed) - 0.5) * jitter
                py = cy + 0.5 + (_h(cx, cy, cz, seed + 7) - 0.5) * jitter
                pz = cz + 0.5 + (_h(cx, cy, cz, seed + 13) - 0.5) * jitter
                dd = math.sqrt((x - px) ** 2 + (y - py) ** 2 + (z - pz) ** 2)
                if dd < best:
                    best, bid = dd, _h(cx, cy, cz, seed + 99)
    return best, bid


def pick(ramp, k):
    return ramp[max(0, min(len(ramp) - 1, int(round(k))))]


def alpha(c, a):
    c = tex.rgb(c)
    return (c[0], c[1], c[2], int(a))


def lerp(c1, c2, t):
    return tex.mix(c1, c2, max(0.0, min(1.0, t)))
