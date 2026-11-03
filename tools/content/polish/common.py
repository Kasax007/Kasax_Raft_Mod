"""Shared palettes and helpers for the 2.0 texture polish (src/main/resources/assets/raft/textures).

The 2.0 textures keep their file names, sizes and UV layouts; only the paint changes. Palettes are shared with the
3.0 modules (building/decor) so old and new blocks read as one set.
"""
import math
import os
import random

from PIL import Image

from bclib import tex
from bclib.paths import RESOURCES, VANILLA
from content.building import art as BA
from content.decor import art as DA

pal = BA.pal
lerp = BA.lerp
new = BA.new
put = BA.put          # wraps (tileable)
put_in = BA.put_in    # clips
fill = BA.fill
noise = BA.noise
bevel = BA.bevel
from_rows = BA.from_rows

TEX = os.path.join(RESOURCES, 'assets', 'raft', 'textures')

# ------------------------------------------------------------------------------------------------ palettes (dark -> light)
DRIFT = [tex.rgb('#3a352f')] + DA.DRIFT                       # 7: driftwood (decor_driftwood)
DRIFT_BARK = pal('#2c2a2c', '#3d3937', '#504a44', '#665e54', '#7d7466', '#958b7b', '#ada392')
PALM = [tex.rgb('#4d3319')] + DA.PALM                         # 7: palm wood (decor_palm)
PALM_BARK = pal('#2a211b', '#3c2f24', '#4f3f2e', '#64513a', '#7a6548', '#927b58', '#ab946c')
STRIPPED_PALM = pal('#7a5a33', '#957243', '#b08b55', '#c6a26a', '#d8b983', '#e7cf9e', '#f2e2bb')
STRIPPED_DRIFT = pal('#6a6152', '#82786a', '#9a9081', '#b1a898', '#c6beaf', '#d8d1c4', '#e8e3d8')
ROPE = DA.ROPE
CANVAS = DA.CANVAS
TEAK = BA.TEAK
RED = DA.RED
WHITE = DA.WHITE
IRON = DA.IRON
BRASS = DA.BRASS
STEEL = BA.STEEL
GALV = BA.GALV
RUST = BA.RUST
DARK = BA.DARK
NAVY = BA.NAVY
GLASS_BLUE = BA.GLASS_BLUE
PALM_LEAF = pal('#143a30', '#1c5134', '#286a37', '#3a833c', '#559c42', '#78b44b', '#a1c95e')
RUSSET = pal('#3b1c16', '#57281b', '#743921', '#904c2a', '#a96336', '#be7c46', '#d29a5e')
THATCH = pal('#4e3a1c', '#6d5226', '#8d6c33', '#ac8742', '#c6a257', '#dcbd72', '#ecd596')
SOIL = pal('#20160f', '#2e2016', '#3d2b1d', '#4d3725', '#5e452e', '#715538')
# Titanium: cool silver with the 2.0 light-blue identity, shadows towards violet, highlights towards white-cyan.
TITAN = pal('#222a3d', '#2c3f5c', '#3b5878', '#4f7596', '#6894b4', '#8bb4cf', '#b5d6e8', '#e0f3fa')
COPPER = pal('#3d1d12', '#5c2b19', '#7e3d22', '#9f522c', '#bb6a38', '#d1874c', '#e2a66a', '#efc792')
CLAY = pal('#3f1a14', '#5a251b', '#783322', '#94432b', '#ab5536', '#bf6c48', '#d08a66')
MORTAR = pal('#6f6a62', '#8a857c', '#a39e94', '#b9b5ab')
PAINT_BLUE = pal('#2c4258', '#3d5a75', '#527594', '#6c92b0', '#8cb0ca', '#b0cee0', '#d4e8f2')
PAINT_WHITE = pal('#5f6468', '#7d8386', '#9da2a3', '#bcc0be', '#d6d8d3', '#ebece6', '#f7f7f1')
PLASTIC = pal('#7e6a3f', '#9f8a52', '#bba767', '#d2c27f', '#e4d89b', '#f1eab9', '#faf6dc')
WATER = pal('#123f6e', '#1a5590', '#2370b0', '#3a8fcc', '#62afe0', '#9bd2f0', '#d2eefa')
EMBER = pal('#4a140c', '#7c200e', '#b23a12', '#e0601a', '#f68f2a', '#ffc04a', '#fff08a')
SHADOW = (24, 22, 30, 255)


def rgba(c, a=255):
    c = tex.rgb(c)
    return (c[0], c[1], c[2], a)


def clamp(i, p):
    return p[max(0, min(len(p) - 1, i))]


def vanilla(rel):
    """Vanilla texture, e.g. 'block/stone'."""
    return Image.open(os.path.join(VANILLA, 'assets', 'minecraft', 'textures', rel + '.png')).convert('RGBA')


def original_size(rel):
    p = os.path.join(TEX, rel + '.png')
    return Image.open(p).size if os.path.isfile(p) else None


WRITTEN = []


def save(img, rel, frames=None, frametime=None, interpolate=False):
    """Write textures/<rel>.png. img: one image, or a list of frames (vertical strip + .mcmeta).
    The size of a single frame must match the file it replaces (models depend on it)."""
    p = os.path.join(TEX, rel + '.png')
    if isinstance(img, list):
        w, h = img[0].size
        old = original_size(rel)
        if old is not None and old[0] != w:
            raise ValueError('%s: frame width %d != %d' % (rel, w, old[0]))
        strip = Image.new('RGBA', (w, h * len(img)))
        for i, f in enumerate(img):
            strip.alpha_composite(f, (0, i * h))
        strip.save(p)
        meta = {'animation': {'frametime': frametime or 4}}
        if interpolate:
            meta['animation']['interpolate'] = True
        import json
        with open(p + '.mcmeta', 'w') as f:
            json.dump(meta, f, indent=2)
            f.write('\n')
    else:
        old = original_size(rel)
        if old is not None and old != img.size and not os.path.isfile(p + '.mcmeta'):
            raise ValueError('%s: size %s != %s' % (rel, img.size, old))
        img.save(p)
    WRITTEN.append(rel)
    return p


def binary_alpha(img, thr=128):
    """26.x picks the render layer from the texture: half-transparent pixels make a block translucent. Snap alpha."""
    out = img.copy()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = out.getpixel((x, y))
            out.putpixel((x, y), (r, g, b, 255 if a >= thr else 0))
    return out


def opaque(img):
    out = img.copy()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = out.getpixel((x, y))
            out.putpixel((x, y), (r, g, b, 255))
    return out


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def remap(img, palette, lo=None, hi=None, keep_alpha=True, gamma=1.0):
    """Recolour by luminance onto a ramp (keeps the shading of a vanilla template, swaps the material)."""
    vals = [lum(img.getpixel((x, y))) for y in range(img.height) for x in range(img.width)
            if img.getpixel((x, y))[3] > 0]
    if not vals:
        return img.copy()
    lo = min(vals) if lo is None else lo
    hi = max(vals) if hi is None else hi
    out = img.copy()
    n = len(palette)
    for y in range(img.height):
        for x in range(img.width):
            c = img.getpixel((x, y))
            if c[3] == 0:
                continue
            t = (lum(c) - lo) / max(1e-6, hi - lo)
            t = max(0.0, min(1.0, t)) ** gamma
            k = palette[min(n - 1, int(t * n))]
            out.putpixel((x, y), (k[0], k[1], k[2], c[3] if keep_alpha else 255))
    return out


def emboss(h, x, y, k=1.4):
    """Height field shading with light from the top-left (h: 16x16 rows, tileable)."""
    v = h[y % 16][x % 16]
    return v + k * (v - h[(y - 1) % 16][(x - 1) % 16])


def outline(img, colour=None):
    return tex.outline(img, colour)


def drop_outline(img, palette_dark):
    """Item outline in the darkest shade of the material (no pure black)."""
    return tex.outline(img, palette_dark)


def rows(img, rows_, mapping):
    """Paint chars of 16 row strings (mapping char -> colour; missing chars skipped)."""
    for y, r in enumerate(rows_):
        for x, ch in enumerate(r):
            if ch in mapping and mapping[ch] is not None:
                img.putpixel((x, y), rgba(mapping[ch]))
    return img


def sprite(rows_, mapping, size=16):
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    return rows(img, rows_, mapping)
