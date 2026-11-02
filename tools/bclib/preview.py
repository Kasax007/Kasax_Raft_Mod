"""Previews for structures: isometric render and floor-plan slices, coloured with the mean colour of each block's
texture (vanilla or Beach Craft). Use with the Read tool to look at your build before it goes into the game.

    from bclib import preview
    preview.iso(structure, '/path/out.png')                  # whole build, seen from south-west
    preview.iso(structure, '/path/cut.png', cut_y=12)         # only layers <= 12 (look inside)
    preview.slices(structure, '/path/plan.png', ys=[0, 1, 5]) # floor plans
"""
import os
import re
from functools import lru_cache

from PIL import Image, ImageDraw

from .paths import find_texture

_SUFFIXES = ['_stairs', '_slab', '_wall', '_fence_gate', '_fence', '_pane', '_door', '_trapdoor', '_button',
             '_pressure_plate', '_carpet', '_wall_sign', '_sign', '_wall_hanging_sign', '_hanging_sign']
_FALLBACK = {
    'water': (40, 110, 200, 120), 'air': None, 'structure_void': None, 'jigsaw': (200, 60, 200, 255),
    'glass': (200, 230, 240, 90), 'lava': (230, 110, 20, 255), 'kelp': (60, 120, 40, 255),
    'chest': (160, 110, 40, 255), 'torch': (250, 200, 80, 255), 'lantern': (240, 190, 90, 255),
    'spawner': (40, 50, 60, 255), 'bed': (180, 40, 40, 255), 'iron_bars': (150, 150, 150, 150),
    'chain': (90, 90, 100, 255), 'ladder': (150, 110, 60, 255), 'rope': (184, 154, 100, 255),
}
_TEX_MAP = [
    (r'^(.*)_planks?$', r'\1_planks'), (r'^(stripped_)?(.*)_(log|wood)$', r'\1\2_log'),
    (r'^(.*)_bricks?$', r'\1_bricks'),
]


@lru_cache(maxsize=None)
def block_colour(name):
    ns, path = name.split(':', 1)
    if path in _FALLBACK:
        return _FALLBACK[path]
    cands = [path]
    for suf in _SUFFIXES:
        if path.endswith(suf):
            base = path[:-len(suf)]
            cands += [base, base + '_planks', base + 's', base.replace('brick', 'bricks'), base + '_block']
    cands += [path + '_side', path + '_top', path + '_front', path + '_0', path + '_bottom', path + '_still']
    if 'glass' in path:
        cands.append(path.replace('_pane', ''))
    for c in cands:
        p = find_texture(f'{ns}:block/{c}')
        if p:
            im = Image.open(p).convert('RGBA')
            if im.height > im.width:
                im = im.crop((0, 0, im.width, im.width))
            px = [q for q in im.getdata() if q[3] > 30]
            if not px:
                continue
            r = sum(q[0] for q in px) // len(px); g = sum(q[1] for q in px) // len(px)
            b = sum(q[2] for q in px) // len(px)
            a = 110 if ('glass' in path or 'ice' == path) else 255
            return (r, g, b, a)
    for k, v in _FALLBACK.items():
        if k in path:
            return v
    return (255, 0, 255, 255)  # unknown: magenta, so it stands out


def _shade(c, f):
    return (min(255, int(c[0] * f)), min(255, int(c[1] * f)), min(255, int(c[2] * f)), c[3])


def iso(structure, out_png, scale=6, cut_y=None, from_north_east=False, hide=('minecraft:water',),
        background=(28, 32, 40, 255)):
    """Isometric render. hide: block names not drawn (water by default so you see underwater builds)."""
    blocks = {}
    for (x, y, z), (name, props, nbt) in structure.blocks.items():
        if name in hide or name in ('minecraft:air', 'minecraft:structure_void', 'minecraft:cave_air'):
            continue
        if cut_y is not None and y > cut_y:
            continue
        c = block_colour(name)
        if c is None:
            continue
        if from_north_east:
            x, z = -x, -z
        blocks[(x, y, z)] = c
    if not blocks:
        return None
    s = scale
    def proj(x, y, z):
        return ((x - z) * s, (x + z) * s / 2 - y * s)
    pts = [proj(x, y, z) for (x, y, z) in blocks]
    minx = min(p[0] for p in pts) - 2 * s; maxx = max(p[0] for p in pts) + 2 * s
    miny = min(p[1] for p in pts) - 2 * s; maxy = max(p[1] for p in pts) + 2 * s
    W, H = int(maxx - minx), int(maxy - miny)
    if W * H > 36_000_000:
        return iso(structure, out_png, max(1, scale // 2), cut_y, from_north_east, hide, background)
    img = Image.new('RGBA', (W, H), background)
    layer = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    order = sorted(blocks, key=lambda p: (p[0] + p[2], p[1], p[0]))
    for (x, y, z) in order:
        if (x + 1, y, z) in blocks and (x, y + 1, z) in blocks and (x, y, z + 1) in blocks:
            continue
        c = blocks[(x, y, z)]
        X, Y = proj(x, y, z)
        X -= minx; Y -= miny
        top = [(X, Y - s), (X + s, Y - s / 2), (X, Y), (X - s, Y - s / 2)]
        left = [(X - s, Y - s / 2), (X, Y), (X, Y + s), (X - s, Y + s / 2)]
        right = [(X, Y), (X + s, Y - s / 2), (X + s, Y + s / 2), (X, Y + s)]
        # projection: +x to the right-down, +z to the left-down; visible faces: top, +z (left), +x (right)
        if (x, y + 1, z) not in blocks:
            d.polygon(top, fill=_shade(c, 1.12))
        if (x, y, z + 1) not in blocks:
            d.polygon(left, fill=_shade(c, 0.82))
        if (x + 1, y, z) not in blocks:
            d.polygon(right, fill=_shade(c, 0.64))
    img.alpha_composite(layer)
    img.save(out_png)
    return out_png


def slices(structure, out_png, ys=None, scale=8, cols=4):
    """Top-down floor plans of the given layers (default: every 4th layer). Water drawn blue, air dark."""
    (x0, y0, z0), (x1, y1, z1) = structure.bounds()
    ys = ys if ys is not None else list(range(y0, y1 + 1, max(1, (y1 - y0 + 1) // 8)))
    w, h = (x1 - x0 + 1) * scale, (z1 - z0 + 1) * scale
    rows = (len(ys) + cols - 1) // cols
    sheet = Image.new('RGBA', (cols * (w + 6), rows * (h + 18)), (20, 22, 28, 255))
    d = ImageDraw.Draw(sheet)
    for i, y in enumerate(ys):
        ox, oy = (i % cols) * (w + 6), (i // cols) * (h + 18)
        d.text((ox + 2, oy + 2), f'y={y}', fill=(230, 230, 230, 255))
        for (x, yy, z), (name, props, nbt) in structure.blocks.items():
            if yy != y:
                continue
            c = block_colour(name) or (45, 45, 50, 255)
            if name == 'minecraft:air':
                c = (60, 60, 66, 255)
            px, pz = ox + (x - x0) * scale, oy + 16 + (z - z0) * scale
            d.rectangle((px, pz, px + scale - 1, pz + scale - 1), fill=c[:3] + (255,))
            if 'facing' in props and scale >= 6:
                f = props['facing']
                cx, cz = px + scale // 2, pz + scale // 2
                dx, dz = {'north': (0, -1), 'south': (0, 1), 'east': (1, 0), 'west': (-1, 0)}.get(f, (0, 0))
                d.line((cx, cz, cx + dx * scale // 2, cz + dz * scale // 2), fill=(255, 255, 255, 255))
    sheet.save(out_png)
    return out_png


def unknown_blocks(structure):
    """Block names without any texture (would render magenta): check your ids against the spec."""
    return sorted({n for (n, p, b) in structure.blocks.values() if block_colour(n) == (255, 0, 255, 255)})
