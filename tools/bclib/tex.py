"""Pixel-art helpers for Beach Craft 3 textures (16x16 blocks/items, vanilla style).

Art direction (see docs/design/beachcraft3.md, section "Art"):
- Light comes from the top-left. Each material uses one hue-shifted ramp of 4-6 shades (shadows cooler/bluer,
  highlights warmer/yellower), no pure black, no single-pixel salt noise.
- Blocks must tile seamlessly (check with tile_preview). Items get a 1-px dark outline (outline()).
- Compare with vanilla neighbours: sheet([... 'minecraft:block/oak_planks' ...]) renders them side by side.
"""
import colorsys
import json
import math
import os
import random

from PIL import Image, ImageDraw

from .paths import out, find_texture, NS


# ----------------------------------------------------------------------------------------------- colour
def rgb(c):
    if isinstance(c, str):
        c = c.lstrip('#')
        return tuple(int(c[i:i + 2], 16) for i in (0, 2, 4)) + ((int(c[6:8], 16),) if len(c) == 8 else (255,))
    return tuple(c) if len(c) == 4 else tuple(c) + (255,)


def ramp(base, n=5, spread=0.55, hue_shift=0.04, sat_shift=0.08):
    """n shades dark->light around base. Shadows drift towards blue, highlights towards yellow."""
    r, g, b, a = rgb(base)
    h, l, s = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
    res = []
    for i in range(n):
        t = (i / (n - 1)) - 0.5 if n > 1 else 0
        lh = min(0.97, max(0.04, l + t * spread))
        hh = (h - t * hue_shift) % 1.0 if s > 0.05 else h
        ss = min(1, max(0, s + (-t if t > 0 else t * 0.5) * sat_shift))
        rr, gg, bb = colorsys.hls_to_rgb(hh, lh, ss)
        res.append((round(rr * 255), round(gg * 255), round(bb * 255), a))
    return res


def mix(c1, c2, t):
    c1, c2 = rgb(c1), rgb(c2)
    return tuple(round(c1[i] + (c2[i] - c1[i]) * t) for i in range(4))


def darker(c, f=0.75):
    c = rgb(c)
    return (round(c[0] * f), round(c[1] * f), round(c[2] * f), c[3])


# Material palettes (base colours). Use ramp(PAL['x']).
PAL = {
    'driftwood': '#9c8b74', 'palm': '#c49a5c', 'weathered': '#8a8378', 'teak': '#8a5a34', 'bamboo': '#c9b45d',
    'rope': '#b89a64', 'canvas': '#e8dfc8', 'thatch': '#c8a45a',
    'sand': '#e3d3a0', 'black_sand': '#3c3a40', 'ash': '#6d6862', 'pumice': '#b9b2a4', 'basalt': '#4a4a52',
    'coral_stone': '#d9a48f', 'tidestone': '#4f8e8f', 'tidestone_glow': '#7ff0e0', 'limestone': '#d8d0b8',
    'concrete': '#9a9a96', 'asphalt': '#3d3f44', 'rust': '#9c4f2a', 'steel': '#8c96a0', 'brass': '#c9a046',
    'gold': '#f2c94c', 'copper': '#c26a42', 'navy': '#2e3f6e', 'pirate_red': '#a3282a', 'sea_glass': '#7fd6c6',
    'water': '#2f8fd8', 'deep': '#123a63', 'kelp': '#4f7a2a', 'glow': '#9af7ff', 'coral_pink': '#f07aa0',
    'coral_orange': '#f29a4a', 'anemone_purple': '#9a5ad8', 'leaf': '#4f9a3a', 'hibiscus': '#e8344a',
}


# ----------------------------------------------------------------------------------------------- images
def new(w=16, h=16, fill=(0, 0, 0, 0)):
    return Image.new('RGBA', (w, h), rgb(fill) if fill != (0, 0, 0, 0) else fill)


def px(img, x, y, c):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), rgb(c))


def rect(img, x0, y0, x1, y1, c, fill=True):
    d = ImageDraw.Draw(img)
    if fill:
        d.rectangle((x0, y0, x1, y1), fill=rgb(c))
    else:
        d.rectangle((x0, y0, x1, y1), outline=rgb(c))


def line(img, x0, y0, x1, y1, c):
    ImageDraw.Draw(img).line((x0, y0, x1, y1), fill=rgb(c))


def ellipse(img, x0, y0, x1, y1, c, fill=True):
    d = ImageDraw.Draw(img)
    if fill:
        d.ellipse((x0, y0, x1, y1), fill=rgb(c))
    else:
        d.ellipse((x0, y0, x1, y1), outline=rgb(c))


def value_noise(w, h, seed, cell=4, octaves=2, tile=True):
    """Tileable value noise in [0,1], list of rows."""
    rnd = random.Random(seed)
    res = [[0.0] * w for _ in range(h)]
    amp, total = 1.0, 0.0
    c = cell
    for _ in range(octaves):
        gw, gh = max(1, w // c), max(1, h // c)
        grid = [[rnd.random() for _ in range(gw)] for _ in range(gh)]
        for y in range(h):
            for x in range(w):
                fx, fy = x / c, y / c
                x0, y0 = int(fx), int(fy)
                tx, ty = fx - x0, fy - y0
                tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
                g = lambda i, j: grid[j % gh][i % gw]
                v = (g(x0, y0) * (1 - tx) + g(x0 + 1, y0) * tx) * (1 - ty) + \
                    (g(x0, y0 + 1) * (1 - tx) + g(x0 + 1, y0 + 1) * tx) * ty
                res[y][x] += v * amp
        total += amp
        amp *= 0.5
        c = max(1, c // 2)
    return [[v / total for v in row] for row in res]


def noise_fill(img, shades, seed, cell=4, octaves=2, contrast=1.0, box=None):
    """Fill (part of) img with noise mapped onto a ramp (list of colours, dark->light)."""
    x0, y0, x1, y1 = box or (0, 0, img.width - 1, img.height - 1)
    n = value_noise(img.width, img.height, seed, cell, octaves)
    k = len(shades)
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            v = 0.5 + (n[y][x] - 0.5) * contrast
            img.putpixel((x, y), shades[max(0, min(k - 1, int(v * k)))])
    return img


def planks(shades, seed=1, boards=4, vertical=False, knots=1):
    """Wooden planks like vanilla: `boards` boards, dark seam below each board, a lit top edge, long horizontal
    grain streaks, staggered butt joints and the odd knot. shades: ramp of 5 (dark->light)."""
    rnd = random.Random(seed)
    img = new()
    bh = 16 // boards
    top = len(shades) - 1
    for b in range(boards):
        tone = rnd.choice([2, 2, 3]) if len(shades) > 4 else 1
        y0 = b * bh
        for y in range(y0, y0 + bh):
            for x in range(16):
                img.putpixel((x, y), shades[tone])
        # grain: long streaks one shade lighter/darker
        for y in range(y0 + 1, y0 + bh - 1):
            x = rnd.randrange(16)
            for _ in range(rnd.randrange(1, 3)):
                ln = rnd.randrange(3, 8)
                d = rnd.choice([-1, 1])
                for k in range(ln):
                    img.putpixel(((x + k) % 16, y), shades[max(0, min(top, tone + d))])
                x = (x + ln + rnd.randrange(2, 6)) % 16
        for x in range(16):
            img.putpixel((x, y0 + bh - 1), shades[max(0, tone - 2)])        # seam / shadow
            img.putpixel((x, y0), shades[min(top, tone + 1)])                # lit top edge
        joint = (rnd.randrange(4, 12) + b * 5) % 16                          # butt joint
        for y in range(y0, y0 + bh - 1):
            img.putpixel((joint, y), shades[max(0, tone - 1)])
        for _ in range(knots if rnd.random() < 0.6 else 0):
            kx, ky = rnd.randrange(1, 15), y0 + max(1, bh // 2)
            img.putpixel((kx, ky), shades[max(0, tone - 2)])
            img.putpixel(((kx + 1) % 16, ky), shades[max(0, tone - 1)])
    return img.transpose(Image.Transpose.ROTATE_90) if vertical else img


def bricks(shades, mortar, seed=1, rows=4, cols=2, bevel=True):
    """Running-bond bricks. shades: ramp; mortar colour."""
    rnd = random.Random(seed)
    img = new(fill=mortar)
    rh, cw = 16 // rows, 16 // cols
    for r in range(rows):
        off = (cw // 2) * (r % 2)
        for c in range(cols + 1):
            x0 = c * cw - off
            tone = rnd.choice([1, 2, 2, 3])
            for y in range(r * rh, r * rh + rh - 1):
                for x in range(x0, x0 + cw - 1):
                    xx = x % 16
                    v = tone + (1 if rnd.random() < 0.12 else 0) - (1 if rnd.random() < 0.10 else 0)
                    if bevel and y == r * rh:
                        v = min(len(shades) - 1, tone + 1)
                    if bevel and y == r * rh + rh - 2:
                        v = max(0, tone - 1)
                    img.putpixel((xx, y), shades[max(0, min(len(shades) - 1, v))])
    return img


def metal_plate(shades, seed=1, rivets=True, seams=True):
    rnd = random.Random(seed)
    img = noise_fill(new(), shades[1:4], seed, cell=8, octaves=2, contrast=0.8)
    if seams:
        for i in range(16):
            img.putpixel((i, 0), shades[-1]); img.putpixel((0, i), shades[-1])
            img.putpixel((i, 15), shades[0]); img.putpixel((15, i), shades[0])
    if rivets:
        for (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)):
            img.putpixel((x, y), shades[-1]); img.putpixel((x + 1, y + 1), shades[0])
    return img


def overlay_spots(img, colours, amount=0.15, seed=1, cell=3, mask=None):
    """Rust, moss, algae, ash: noise-shaped blotches of `colours` over amount of the area."""
    n = value_noise(img.width, img.height, seed, cell, 2)
    flat = sorted(v for row in n for v in row)
    thr = flat[int(len(flat) * (1 - amount))] if amount > 0 else 2
    for y in range(img.height):
        for x in range(img.width):
            if n[y][x] >= thr and img.getpixel((x, y))[3] > 0 and (mask is None or mask(x, y)):
                k = min(len(colours) - 1, int((n[y][x] - thr) / max(1e-6, 1 - thr) * len(colours)))
                img.putpixel((x, y), rgb(colours[k]))
    return img


def cracks(img, colour, n=2, seed=1, length=6):
    rnd = random.Random(seed)
    for _ in range(n):
        x, y = rnd.randrange(16), rnd.randrange(16)
        for _ in range(length):
            px(img, x, y, colour)
            x += rnd.choice([-1, 0, 1]); y += rnd.choice([0, 1, 1])
    return img


def outline(img, colour=None):
    """1-px outline around the opaque shape (for item sprites). colour defaults to a dark tone of the shape."""
    src = img.copy()
    w, h = img.size
    for y in range(h):
        for x in range(w):
            if src.getpixel((x, y))[3] == 0:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    xx, yy = x + dx, y + dy
                    if 0 <= xx < w and 0 <= yy < h and src.getpixel((xx, yy))[3] > 0:
                        c = colour or darker(src.getpixel((xx, yy)), 0.45)
                        img.putpixel((x, y), rgb(c))
                        break
    return img


def border(img, light, dark):
    for i in range(img.width):
        px(img, i, 0, light); px(img, i, img.height - 1, dark)
    for j in range(img.height):
        px(img, 0, j, light); px(img, img.width - 1, j, dark)
    return img


# ----------------------------------------------------------------------------------------------- io
def save(img, module, rel):
    """rel: 'block/name', 'item/name', 'entity/x/y', 'gui/...'. Writes assets/raft/textures/<rel>.png."""
    p = out(module, 'assets', NS, 'textures', *(rel + '.png').split('/'))
    img.save(p)
    return p


def save_animated(frames, module, rel, frametime=4, interpolate=False):
    """Vertical strip + .mcmeta animation file."""
    w, h = frames[0].size
    strip = Image.new('RGBA', (w, h * len(frames)))
    for i, f in enumerate(frames):
        strip.alpha_composite(f, (0, i * h))
    p = save(strip, module, rel)
    meta = {'animation': {'frametime': frametime}}
    if interpolate:
        meta['animation']['interpolate'] = True
    with open(p + '.mcmeta', 'w') as f:
        json.dump(meta, f, indent=2)
    return p


def load(ident):
    """Load a texture by id ('minecraft:block/stone', 'raft:item/coconut') or path."""
    p = ident if os.path.isfile(ident) else find_texture(ident)
    return Image.open(p).convert('RGBA') if p else None


def sheet(items, out_png, scale=6, cols=8, label=True):
    """Contact sheet of textures (ids, paths or Images) for visual review with the Read tool."""
    from PIL import ImageFont
    ims = []
    for it in items:
        im = it if isinstance(it, Image.Image) else load(it)
        name = '' if isinstance(it, Image.Image) else str(it).split('/')[-1].replace('.png', '')
        if im is None:
            continue
        if im.height > im.width:  # animated strip: first frame
            im = im.crop((0, 0, im.width, im.width))
        ims.append((im, name))
    if not ims:
        return None
    cell = max(max(i.width, i.height) for i, _ in ims) * scale
    lab = 14 if label else 0
    rows = (len(ims) + cols - 1) // cols
    sh = Image.new('RGBA', (cols * (cell + 4), rows * (cell + 4 + lab)), (48, 52, 60, 255))
    d = ImageDraw.Draw(sh)
    for i, (im, name) in enumerate(ims):
        big = im.resize((im.width * scale, im.height * scale), Image.NEAREST)
        x, y = (i % cols) * (cell + 4), (i // cols) * (cell + 4 + lab)
        sh.alpha_composite(big, (x, y))
        if label:
            d.text((x + 2, y + cell + 1), name[:22], fill=(230, 230, 230, 255))
    sh.save(out_png)
    return out_png


def tile_preview(item, out_png, n=3, scale=6):
    """n x n tiling of a block texture to check seams."""
    im = item if isinstance(item, Image.Image) else load(item)
    if im.height > im.width:
        im = im.crop((0, 0, im.width, im.width))
    t = Image.new('RGBA', (im.width * n, im.height * n))
    for i in range(n):
        for j in range(n):
            t.alpha_composite(im, (i * im.width, j * im.height))
    t.resize((t.width * scale, t.height * scale), Image.NEAREST).save(out_png)
    return out_png
