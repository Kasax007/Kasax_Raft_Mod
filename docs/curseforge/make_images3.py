"""Images of the CurseForge page for Beach Craft 3.0 (docs/curseforge/img): header, section strips, screenshot
collages and item palettes. Same look as make_images.py (Luckiest Guy, navy/sea colours, rounded strips).
  python3 docs/curseforge/make_images3.py SHOTS_DIR FONT.ttf
SHOTS_DIR holds the in-game screenshots (paths below are relative to it)."""
import math
import os
import sys

from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, '..', '..')
OUT = os.path.join(HERE, 'img')
SHOTS, FONT = sys.argv[1], sys.argv[2]
NAVY, SEA, FOAM, WHITE, SAND = (10, 42, 78), (24, 132, 186), (120, 214, 236), (255, 255, 255), (238, 214, 160)
W = 1200
MAX_BYTES = 900 * 1024


def font(size):
    return ImageFont.truetype(FONT, size)


def tex(path, size=None):
    """Texture by 'module:kind/name' (module '' = 2.0 resources), first frame of animated textures."""
    mod, rel = path.split(':', 1)
    base = os.path.join(ROOT, 'src', 'main', 'modules', mod) if mod else os.path.join(ROOT, 'src', 'main', 'resources')
    im = Image.open(os.path.join(base, 'assets', 'raft', 'textures', rel + '.png')).convert('RGBA')
    im = im.crop((0, 0, im.width, im.width))
    return im.resize((size, size), Image.NEAREST) if size else im


def shot(rel):
    return Image.open(os.path.join(SHOTS, rel)).convert('RGB')


def outlined(d, xy, text, f, fill=WHITE, stroke=6, stroke_fill=NAVY, anchor='la'):
    d.text((xy[0], xy[1] + stroke // 2 + 3), text, font=f, fill=stroke_fill, stroke_width=stroke,
           stroke_fill=stroke_fill, anchor=anchor)
    d.text(xy, text, font=f, fill=fill, stroke_width=stroke, stroke_fill=stroke_fill, anchor=anchor)


def rounded(im, r):
    m = Image.new('L', im.size, 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, im.width - 1, im.height - 1], r, fill=255)
    out = im.convert('RGBA')
    out.putalpha(m)
    return out


def waves(im, y0, color, amp=6, period=60, step=16, width=3):
    d = ImageDraw.Draw(im)
    for row, y in enumerate(range(y0, im.height + step, step)):
        pts = [(x, y + amp * math.sin((x + row * 23) / period * 6.283)) for x in range(-10, im.width + 10, 6)]
        d.line(pts, fill=color, width=width)


def gradient(w, h, c0, c1, horizontal=True):
    im = Image.new('RGBA', (w, h))
    d = ImageDraw.Draw(im)
    n = w if horizontal else h
    for i in range(n):
        t = i / max(1, n - 1)
        c = tuple(int(c0[k] + (c1[k] - c0[k]) * t) for k in range(3)) + (255,)
        d.line([(i, 0), (i, h)] if horizontal else [(0, i), (w, i)], fill=c)
    return im


def save(im, name):
    """PNG (keeps the rounded corners); falls back to 256 colours when the file would be too big."""
    path = os.path.join(OUT, name)
    if name.endswith('.jpg'):
        q = 88
        while True:
            im.convert('RGB').save(path, quality=q, optimize=True, progressive=True)
            if os.path.getsize(path) <= MAX_BYTES or q <= 70:
                break
            q -= 3
    else:
        im.save(path, optimize=True)
        if os.path.getsize(path) > MAX_BYTES:
            alpha = im.split()[3] if im.mode == 'RGBA' else None
            p = im.convert('RGB').quantize(256, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.FLOYDSTEINBERG)
            if alpha is not None:
                p = p.convert('RGBA')
                p.putalpha(alpha)
                p = p.quantize(256, method=Image.Quantize.FASTOCTREE, dither=Image.Dither.NONE)
            p.save(path, optimize=True)
    print(f'{name:28s} {im.size[0]}x{im.size[1]}  {os.path.getsize(path) // 1024} KB')


# ----------------------------------------------------------------------------------------------- strips
def strip(name, title, icon):
    h = 104
    im = gradient(W, h, NAVY, SEA)
    waves(im, 64, FOAM + (70,), amp=5, period=90, step=18)
    ic = icon if isinstance(icon, Image.Image) else tex(icon, 72)
    ic = ic.resize((72, 72), Image.NEAREST) if ic.size != (72, 72) else ic
    glow = Image.new('RGBA', (96, 96), (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse([0, 0, 95, 95], fill=WHITE + (60,))
    im.alpha_composite(glow, (26, 4))
    im.alpha_composite(ic, (38, 16))
    d = ImageDraw.Draw(im)
    size = 56
    while d.textlength(title, font=font(size)) > W - 180 and size > 30:
        size -= 2
    outlined(d, (140, 52), title, font(size), stroke=5, anchor='lm')
    save(rounded(im, 22), f'strip_{name}.png')


# ----------------------------------------------------------------------------------------------- header
def header(src, box, name='header_3.png', size=(1600, 500)):
    """box = crop box in the source shot (any aspect close to size); title on the left over a navy shade."""
    w, h = size
    im = shot(src).crop(box).resize(size, Image.LANCZOS).convert('RGBA')
    shade = Image.new('RGBA', size)
    d = ImageDraw.Draw(shade)
    for x in range(w):
        t = max(0.0, 1 - x / (w * 0.6))
        d.line([(x, 0), (x, h)], fill=NAVY + (int(220 * t ** 1.3),))
    im = Image.alpha_composite(im, shade)
    d = ImageDraw.Draw(im)
    outlined(d, (72, 70), 'BEACH CRAFT 3.0', font(124), stroke=10)
    outlined(d, (80, 236), 'Tides of the Deep', font(74), fill=SAND, stroke=7)
    tag = 'Minecraft 26.3  -  Fabric'
    f = font(36)
    tw = d.textlength(tag, font=f)
    d.rounded_rectangle([80, 368, 80 + tw + 52, 422], 27, fill=SEA + (255,), outline=WHITE + (255,), width=4)
    d.text((106, 375), tag, font=f, fill=WHITE)
    save(rounded(im, 30), name)


# ----------------------------------------------------------------------------------------------- collages
PAD, GAP = 16, 12


def fit(src, w, h, focus=(0.5, 0.5), zoom=1.0, box=None):
    im = shot(src) if isinstance(src, str) else src
    if box:
        im = im.crop(box)
    sw, sh = im.size
    cw = sw / zoom
    ch = cw * h / w
    if ch > sh / zoom:
        ch = sh / zoom
        cw = ch * w / h
    cx = min(max(focus[0] * sw, cw / 2), sw - cw / 2)
    cy = min(max(focus[1] * sh, ch / 2), sh - ch / 2)
    return im.crop((int(cx - cw / 2), int(cy - ch / 2), int(cx + cw / 2), int(cy + ch / 2))).resize((w, h), Image.LANCZOS)


def caption(tile, text, size=30):
    d = ImageDraw.Draw(tile)
    while size > 16 and d.textlength(text, font=font(size)) > tile.width - 40:
        size -= 1
    f = font(size)
    shade = Image.new('RGBA', tile.size, (0, 0, 0, 0))
    sd = ImageDraw.Draw(shade)
    band = size + 34
    for i in range(band):
        a = int(150 * (i / band) ** 1.4)
        sd.line([(0, tile.height - band + i), (tile.width, tile.height - band + i)], fill=(5, 20, 40, a))
    tile.alpha_composite(shade)
    d = ImageDraw.Draw(tile)
    outlined(d, (16, tile.height - 14), text, f, stroke=4, anchor='ld')


def collage(name, rows, title=None, height_scale=1.0):
    """rows: list of (height, [tile, ...]); tile = dict(src, w (weight), focus, zoom, box, cap)."""
    inner = W - 2 * PAD
    top = PAD
    total_h = PAD + sum(int(h * height_scale) for h, _ in rows) + GAP * (len(rows) - 1) + PAD
    im = gradient(W, total_h, NAVY, (16, 78, 128), horizontal=False)
    waves(im, total_h - 40, FOAM + (40,), amp=4, period=80, step=14, width=2)
    y = top
    for h, tiles in rows:
        h = int(h * height_scale)
        weights = sum(t.get('w', 1) for t in tiles)
        avail = inner - GAP * (len(tiles) - 1)
        x = PAD
        for i, t in enumerate(tiles):
            tw = int(avail * t.get('w', 1) / weights) if i < len(tiles) - 1 else PAD + inner - x
            tile = fit(t['src'], tw, h, t.get('focus', (0.5, 0.5)), t.get('zoom', 1.0), t.get('box')).convert('RGBA')
            if t.get('cap'):
                caption(tile, t['cap'], t.get('capsize', 30 if tw > 360 else 24))
            frame = Image.new('RGBA', (tw, h), (0, 0, 0, 0))
            ImageDraw.Draw(frame).rounded_rectangle([0, 0, tw - 1, h - 1], 16, fill=WHITE + (255,))
            frame.alpha_composite(rounded(tile.crop((3, 3, tw - 3, h - 3)), 13), (3, 3))
            im.alpha_composite(frame, (x, y))
            x += tw + GAP
        y += h + GAP
    save(im.convert('RGB'), name)


# ----------------------------------------------------------------------------------------------- palettes
def cube(top, left, right, a=48):
    """Minecraft inventory-style block: three textures, 2a x 2a."""
    S = top.width
    out = Image.new('RGBA', (2 * a, 2 * a), (0, 0, 0, 0))
    faces = [
        (top, (S / (2 * a), -S / a, S / 2, S / (2 * a), S / a, -S / 2), [(a, 0), (2 * a, a / 2), (a, a), (0, a / 2)], 1.0),
        (left, (S / a, 0, 0, -S / (2 * a), S / a, -S / 2), [(0, a / 2), (a, a), (a, 2 * a), (0, 1.5 * a)], 0.8),
        (right, (S / a, 0, -S, S / (2 * a), S / a, -1.5 * S), [(a, a), (2 * a, a / 2), (2 * a, 1.5 * a), (a, 2 * a)], 0.62),
    ]
    for t, aff, poly, shade in faces:
        f = t.transform((2 * a, 2 * a), Image.AFFINE, aff, resample=Image.NEAREST)
        if shade < 1:
            r, g, b, al = f.split()
            r, g, b = (c.point(lambda v, s=shade: int(v * s)) for c in (r, g, b))
            f = Image.merge('RGBA', (r, g, b, al))
        m = Image.new('L', (2 * a, 2 * a), 0)
        ImageDraw.Draw(m).polygon(poly, fill=255)
        out.paste(f, (0, 0), Image.composite(f.split()[3], Image.new('L', m.size, 0), m))
    return out


def palette(name, entries, cols=8, cell=(W // 8, 176), icon=96, label_size=21):
    """entries: (label, Image icon). Sand panel like the 2.0 blocks.png."""
    cw, ch = cell
    rows = (len(entries) + cols - 1) // cols
    im = gradient(W, rows * ch + 24, (250, 240, 214), (236, 226, 202), horizontal=False)
    d = ImageDraw.Draw(im)
    f = font(label_size)
    x0 = (W - cols * cw) // 2
    for k, (label, ic) in enumerate(entries):
        cx, cy = x0 + (k % cols) * cw + cw // 2, (k // cols) * ch + 14
        d.ellipse([cx - 52, cy + icon - 4, cx + 52, cy + icon + 14], fill=(214, 194, 150, 255))
        ic = ic if ic.size[0] == icon else ic.resize((icon, icon * ic.height // ic.width), Image.NEAREST)
        im.alpha_composite(ic, (cx - ic.width // 2, cy + icon + 6 - ic.height))
        words, lines = label.split(), ['']
        for wd in words:
            t = (lines[-1] + ' ' + wd).strip()
            if d.textlength(t, font=f) <= cw - 10:
                lines[-1] = t
            else:
                lines.append(wd)
        for i, ln in enumerate(lines[:2]):
            d.text((cx, cy + icon + 34 + i * (label_size + 3)), ln, font=f, fill=NAVY, anchor='mm')
    save(rounded(im, 26), name)


# ----------------------------------------------------------------------------------------------- page images
STRIPS = [('start', 'GETTING STARTED', ':item/energy_staff'),
          ('world', 'A WORLD OF ISLANDS', 'world:block/hibiscus'),
          ('story', 'TIDES OF THE DEEP', 'story:item/expedition_journal'),
          ('places', 'PLACES TO EXPLORE', 'building:block/porthole'),
          ('creatures', 'CREATURES', 'shore:item/crab_claw'),
          ('sailing', 'SET SAIL', 'voyage:item/raft_sail'),
          ('diving', 'DIVING', 'diving:item/diving_helmet'),
          ('decor', 'BUILD AND DECORATE', 'decor:item/string_lights_colorful'),
          ('almanac', "THE SEAFARER'S ALMANAC", 'guide:item/seafarers_almanac'),
          ('deep', 'THE BOTTOM OF THE SEA', 'story:item/tidewarden_key'),
          # 3.1
          ('shipwright', 'BUILD YOUR OWN RAFT', 'vessel:item/helm'),
          ('sounds', 'SOUNDS AND MUSIC', ':item/some_game_music_music_disc')]


def npc_face(npc, size=72):
    """Head of a person of the sea (front of the skin's head plus the hat layer)."""
    skin = Image.open(os.path.join(ROOT, 'src', 'main', 'modules', 'npc', 'assets', 'raft', 'textures', 'entity', 'npc',
                                   npc + '.png')).convert('RGBA')
    head = skin.crop((8, 8, 16, 16))
    head.alpha_composite(skin.crop((40, 8, 48, 16)))
    return head.resize((size, size), Image.NEAREST)


def build_strips():
    for name, title, icon in STRIPS:
        strip(name, title, icon)
    strip('people', 'PEOPLE OF THE SEA', npc_face('keeper_fenn'))


def hotbar_icons(rel, names, empty=None, x0=376, y0=664, step=60, size=48, tol=9, sel=0):
    """Cut the item icons out of a hotbar screenshot. The slot background is keyed out by a flood fill from the
    border over colours that also occur in an empty slot (empty = (shot, slot index))."""
    im = shot(rel)
    ref_im = shot(empty[0]) if empty else im
    ei = empty[1] if empty else 8
    ref = ref_im.crop((x0 + step * ei, y0, x0 + step * ei + size, y0 + size)).getcolors(size * size)
    ref = [c for _, c in ref]

    def is_bg(c):
        return any(abs(c[0] - r[0]) + abs(c[1] - r[1]) + abs(c[2] - r[2]) <= tol for r in ref)

    out = {}
    for i, name in enumerate(names):
        if not name:
            continue
        box = im.crop((x0 + step * i, y0, x0 + step * i + size, y0 + size)).convert('RGBA')
        px = box.load()
        seen = set()
        stack = [(x, y) for x in range(size) for y in (0, size - 1)] + [(x, y) for y in range(size) for x in (0, size - 1)]
        while stack:
            x, y = stack.pop()
            if (x, y) in seen or not (0 <= x < size and 0 <= y < size):
                continue
            seen.add((x, y))
            if not is_bg(px[x, y]):
                continue
            px[x, y] = (0, 0, 0, 0)
            stack += [(x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)]
        if i == sel:      # the selection frame of the selected slot leaves light grey lines at the box edge
            for x in range(size):
                for y in range(size):
                    r, g, b, a = px[x, y]
                    if a and min(x, y, size - 1 - x, size - 1 - y) < 4 and max(r, g, b) - min(r, g, b) < 8 and r > 100:
                        px[x, y] = (0, 0, 0, 0)
        out[name] = clean(box)
    return out


def clean(icon, min_px=24):
    """Drop tiny opaque specks that are not part of the item."""
    px = icon.load()
    w, h = icon.size
    seen = set()
    for sx in range(w):
        for sy in range(h):
            if (sx, sy) in seen or px[sx, sy][3] == 0:
                continue
            comp, stack = [], [(sx, sy)]
            while stack:
                x, y = stack.pop()
                if (x, y) in seen or not (0 <= x < w and 0 <= y < h) or px[x, y][3] == 0:
                    continue
                seen.add((x, y))
                comp.append((x, y))
                stack += [(x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)]
            if len(comp) < min_px:
                for x, y in comp:
                    px[x, y] = (0, 0, 0, 0)
    return icon

BOOK = (158, 26, 1122, 642)          # the open almanac in a 1280x720 shot


def T(src, cap=None, w=1.0, focus=(0.5, 0.5), zoom=1.0, box=None):
    return dict(src=src, cap=cap, w=w, focus=focus, zoom=zoom, box=box)


COLLAGES = {
    'world.jpg': [
        (330, [T('worldlook/beauty/coral_shallows_reef_underwater.png', 'Coral Shallows', 1.25, (0.5, 0.55)),
               T('worldlook/beauty/palm_isle_beach.png', 'Palm Isle', 1.0, (0.42, 0.5))]),
        (236, [T('worldlook/beauty/volcanic_isle_aerial.png', 'Volcanic Isle', 1, (0.5, 0.5), 1.25),
               T('worldlook/beauty/frost_floes_surface.png', 'Frost Floes', 1, (0.3, 0.45), 1.3),
               T('worldlook/beauty/glowing_lagoon_underwater_2.png', 'Glowing Lagoon', 1, (0.5, 0.55))]),
        (236, [T('worldlook/beauty/jungle_isle_shore.png', 'Jungle Isle', 1, (0.5, 0.45)),
               T('worldlook/beauty/kelp_forest_underwater.png', 'Kelp Forest', 1, (0.5, 0.5)),
               T('worldlook/beauty/abyssal_trench_floor_glow.png', 'Abyssal Trench', 1, (0.45, 0.6), 1.15)]),
    ],
    'almanac.jpg': [
        (372, [T('guide/t1/a01_welcome.png', 'Welcome Aboard', 1, box=BOOK),
               T('guide/t2/c04_creatures_whale.png', 'A page for every creature', 1, box=BOOK)]),
        (372, [T('guide/t1/a25_sailing.png', 'Sailing', 1, box=BOOK),
               T('guide/t1/a22_biomes.png', "The Sea's Biomes", 1, box=BOOK)]),
    ],
    'story.jpg': [
        (330, [T('storypath/r2/lighthouse_overview.png', 'The Old Lighthouse', 1.3, (0.62, 0.5), 1.15),
               T('storypath/r2/j00_root.png', 'Expedition Journal', 1, box=(318, 140, 962, 572))]),
        (250, [T('story/t2/book_marlow.png', "Captain's note", 0.8, (0.5, 0.42), 1, (476, 6, 806, 300)),
               T('storypath/r2/saltmere_overview.png', 'Saltmere', 1.2, (0.45, 0.5), 1.1),
               T('storypath/r2/helix_lab.png', 'Research Terminals', 1, (0.5, 0.55), 1.1)]),
    ],
    'places.jpg': [
        (330, [T('structures_city/shots4/skyline.png', 'Port Solace', 1.2, (0.5, 0.45), 1.2),
               T('structures_side/shotsA/sunken_plane_airliner_0_0.png', 'Sunken Plane', 1, (0.5, 0.45), 1.3)]),
        (236, [T('structures_deep/shots_t2/helix_outside_nw.png', 'Helix Station', 1, (0.5, 0.5), 1.15),
               T('structures_deep/shots_t2/temple_gate.png', 'Coral Temple', 1, (0.5, 0.5), 1.05),
               T('structures_surface/t4/af_front.png', 'Ashen Forge', 1, (0.5, 0.45), 1.2)]),
        (236, [T('structures_side/shotsA/wreck_galleon_galleon_1_1.png', 'Galleon Wreck', 1, (0.5, 0.55), 1.15),
               T('worldlook/beauty/rusted_rig_over_the_sea.png', 'Rusted Rig', 1, (0.38, 0.55), 1.5),
               T('storypath/r8/arch_1.png', 'Pirate Cove', 1, (0.5, 0.5), 1.1)]),
    ],
    'sailing.jpg': [
        (380, [T('release/shots/run1/raft_close_0.png', 'The Expedition Raft', 1.45, (0.55, 0.56), 1.25),
               T('gameplay2/r3/raft_menu.png', 'Storage, fuel and anchor', 1, box=(328, 24, 952, 528))]),
        (250, [T('release/shots/run1/lh_hdr_4.png', 'Sail to the islands', 1, (0.62, 0.42), 1.8),
               T('gameplay2/r2/boats.png', 'Palm Boat and Driftwood Raft', 1, (0.62, 0.62), 1.15),
               T('release/shots/run1/raft_close_2.png', 'Sail, engine and lanterns', 1, (0.5, 0.62), 2.0)]),
    ],
    'diving.jpg': [
        (430, [T('release/shots/run1/diver_0.png', 'Helmet, tank, wetsuit, flippers and harpoon gun', 1, (0.49, 0.45), 1.45)]),
        (250, [T('gameplay/r1/gear_stand.png', 'The full set', 1, (0.48, 0.5), 1.7),
               T('gameplay2/r2/trench_lamp.png', 'Helmet Lamp in the trench', 1, (0.5, 0.5), 1.1),
               T('gameplay2/r2/compressor_menu.png', 'Air Compressor', 1, box=(380, 104, 900, 444))]),
    ],
    'build.jpg': [
        (300, [T('structures_surface/t5/sv_plaza_deck.png', 'Saltmere market', 1.2, (0.5, 0.5), 1.05),
               T('structures_deep/shots_t2/helix_crew.png', 'Helix crew quarters', 1, (0.5, 0.5), 1.05)]),
        (230, [T('structures_deep/shots_t2/helix_hub.png', 'Expedition tech', 1, (0.5, 0.45), 1.1),
               T('structures_side/shotsC/cove_treasure.png', 'Pirate treasure', 1, (0.62, 0.5), 1.3),
               T('structures_surface/t4/sv_elder_inside.png', "The elder's hut", 1, (0.5, 0.55), 1.1)]),
    ],
    'creatures.jpg': [
        (330, [T('release/shots/run2/shark_under_raft.png', 'Bruce and the Hammerhead', 1.15, (0.56, 0.6), 1.38),
               T('release/shots/run2/sea_reef_b.png', 'Manta Ray and Jellyfish', 1, (0.42, 0.72), 1.55)]),
        (236, [T('release/shots/run2/beach_close.png', 'Crabs and Seagulls', 1, (0.33, 0.34), 1.6),
               T('shore/t9/a1_corsair_cutlass.png', 'Corsair and Ember Crab', 1, (0.48, 0.5), 1.45),
               T('release/shots/run2/penguins_close_b.png', 'Penguins', 1, box=(560, 414, 1436, 958))]),
        (236, [T('release/shots/run1/whale_a.png', 'Whale', 1, (0.45, 0.62), 1.25),
               T('release/shots/run2/otters_close.png', 'Sea Otters', 1, (0.45, 0.45), 2.0),
               T('release/shots/run2/trench_angler_dark.png', 'Anglerfish', 1, (0.42, 0.5), 1.5)]),
    ],
    # 3.1: the people of the sea (shots of the npc module's tests) and a raft built from blocks (release smoke test)
    'people.jpg': [
        (330, [T('work/npcs/out4/fenn_greet.jpg', 'Keeper Fenn at the Old Lighthouse', 1.3, (0.5, 0.55), 1.1),
               T('work/npcs/out5/nalu_veranda.jpg', 'Nalu in Saltmere', 1.0, (0.45, 0.45), 1.15)]),
        (236, [T('work/npcs/out5/kai_sitting.jpg', 'Kai at his camp fire', 1, (0.5, 0.5), 1.15),
               T('work/npcs/out2/lund_view.jpg', 'Dr. Lund on the ice', 1, (0.5, 0.5), 1.1),
               T('work/npcs/out2/okafor_view.jpg', 'Okafor on Rig Seven', 1, (0.55, 0.5), 1.1)]),
        (236, [T('work/npcs/out5/teo_view.jpg', "Teo's tiki bar", 1, (0.5, 0.5), 1.1),
               T('work/npcs/out2/voss_view.jpg', 'Dr. Voss in the Ashen Forge', 1, (0.5, 0.5), 1.1),
               T('work/npcs/out4/fenn_trade.jpg', 'Trade for gold and pearls', 1, box=(380, 124, 900, 444))]),
    ],
    'shipwright.jpg': [
        (330, [T('rel/smoke/74_ship_sailing_back.jpg', 'Sail the raft you built', 1.35, (0.54, 0.47), 1.5),
               T('rel/smoke/72_ship_sailing_side.jpg', 'Everything on deck sails along', 1.0, (0.62, 0.52), 1.9)]),
        (236, [T('rel/smoke/31_lamp_lit.jpg', 'Redstone keeps working', 1, box=(180, 70, 1000, 532)),
               T('rel/smoke/34_chest_open_moving.jpg', 'Chests stay open at sea', 1, box=(300, 140, 980, 523)),
               T('rel/smoke/60_almanac_shipwright.jpg', 'A page in the Almanac', 1, box=BOOK)]),
    ],
    'boss.jpg': [
        (390, [T('release/shots/run2/lev_07.png', 'The Leviathan', 1.35, (0.5, 0.55), 1.12),
               T('release/shots/run2/lev_29.png', 'Tidelings', 1, (0.5, 0.45), 1.12)]),
        (250, [T('release/shots/run2/lev_17.png', 'It leaps across the ring', 1, (0.56, 0.68), 1.25),
               T('release/shots/run2/lev_31.png', 'Rising from the pool', 1, (0.5, 0.6), 1.6),
               T('boss/shots_items/blocks_close.png', 'Heart of the Tide and Trophy', 1, box=(640, 60, 1260, 468))]),
    ],
}


def build_collages(names=None):
    for name, rows in COLLAGES.items():
        if names and name not in names:
            continue
        collage(name, rows)


def tcube(top, side=None, front=None, a=48, overlay=None):
    """Cube icon from module textures ('module:block/name'); overlay = texture drawn over all faces."""
    t, sd = tex(top), tex(side or top)
    fr = tex(front) if front else sd
    if overlay:
        o = tex(overlay)
        t, sd, fr = (Image.alpha_composite(x, o) for x in (t, sd, fr))
    return cube(t, fr, sd, a)


def flat(path, size=96):
    return tex(path, size)


DECOR_HOTBARS = [
    ('decor/run4/hotbar_0.png', ['string_lights_warm', 'string_lights_colorful', 'bunting_red_white', 'bunting_rainbow',
                                 'paper_lantern_white', 'paper_lantern_red', 'paper_lantern_orange', 'paper_lantern_yellow',
                                 'paper_lantern_lime']),
    ('decor/run4/hotbar_1.png', ['paper_lantern_cyan', 'paper_lantern_blue', 'paper_lantern_purple', 'paper_lantern_pink',
                                 'ship_lantern', 'tiki_torch', 'jelly_lamp', 'brazier', 'buoy_red']),
    ('decor/run4/hotbar_2.png', ['buoy_green', 'lighthouse_lens', 'neon_tube_cyan', 'neon_tube_pink', 'driftwood_lamp',
                                 'seashell_lamp', 'ship_cannon', 'ship_anchor', 'treasure_chest']),
    ('decor/run4/hotbar_3.png', ['gold_coin_pile', 'jolly_roger', 'wanted_poster', 'rum_bottles', 'rope_railing',
                                 'cargo_net', 'powder_keg', 'wooden_crate', 'ship_bell']),
    ('decor/run4/hotbar_4.png', ['chart_table', 'spyglass_stand', 'figurehead', 'life_ring', 'hammock', 'deck_chair',
                                 'beach_umbrella_red', 'beach_umbrella_blue', 'beach_umbrella_yellow']),
    ('decor/run4/hotbar_5.png', ['cushion_white', 'cushion_red', 'cushion_blue', 'cushion_yellow', 'cushion_green',
                                 'woven_rug', 'driftwood_table', 'palm_table', 'driftwood_chair']),
    ('decor/run4/hotbar_6.png', ['palm_chair', 'driftwood_shelf', 'wind_chime', 'aquarium', 'hanging_planter', 'cannonball',
                                 'gold_coin', None, None]),
]


def decor_icons():
    icons = {}
    for rel, names in DECOR_HOTBARS:
        icons.update(hotbar_icons(rel, names, empty=('decor/run4/hotbar_6.png', 7)))
    return {k: v.resize((96, 96), Image.NEAREST) for k, v in icons.items()}


def build_palettes():
    B = 'building:block/'
    d = decor_icons()
    new = [
        ('Weathered Planks', tcube(B + 'weathered_planks')), ('Teak Planks', tcube(B + 'teak_planks')),
        ('Aqua Sea Glass', tcube(B + 'aqua_sea_glass')), ('Coral Stone Bricks', tcube(B + 'coral_stone_bricks')),
        ('Tidestone Bricks', tcube(B + 'tidestone_bricks')), ('Chiseled Tidestone', tcube(B + 'chiseled_tidestone')),
        ('Glowing Tidestone', tcube(B + 'glowing_tidestone', overlay=B + 'glowing_tidestone_runes')),
        ('Tidestone Pillar', tcube(B + 'tidestone_pillar_top', B + 'tidestone_pillar')),
        ('Cracked Concrete', tcube(B + 'cracked_concrete')), ('Asphalt Lane Line', tcube(B + 'asphalt_line', B + 'asphalt')),
        ('Riveted Hull', tcube(B + 'riveted_hull')), ('Corrugated Metal', tcube(B + 'corrugated_metal')),
        ('Porthole', tcube(B + 'porthole')),
        ('Shipping Container', tcube(B + 'shipping_container_red_top', B + 'shipping_container_red_side',
                                     B + 'shipping_container_red_end')),
        ('Server Rack', tcube(B + 'server_rack_top', B + 'server_rack_side', B + 'server_rack_front')),
        ('Rusty Hull', tcube(B + 'rusty_hull')),
        ('String Lights', d['string_lights_colorful']), ('Paper Lantern', d['paper_lantern_red']),
        ('Ship Lantern', d['ship_lantern']), ('Tiki Torch', d['tiki_torch']), ('Jelly Lamp', d['jelly_lamp']),
        ('Buoy', d['buoy_red']), ('Lighthouse Lens', d['lighthouse_lens']), ('Neon Tube', d['neon_tube_cyan']),
        ('Ship Cannon', d['ship_cannon']), ('Treasure Chest', d['treasure_chest']), ('Gold Coin Pile', d['gold_coin_pile']),
        ('Jolly Roger', d['jolly_roger']), ('Life Ring', d['life_ring']), ('Deck Chair', d['deck_chair']),
        ('Beach Umbrella', d['beach_umbrella_red']), ('Hammock', d['hammock']),
    ]
    palette('blocks_3.png', new)
    R = ':block/'
    classic = [
        ('Palm Thatch', tcube(R + 'palm_thatch')), ('Raft Foundation', tcube(R + 'raft_foundation')),
        ('Reinforced Foundation', tcube(R + 'reinforced_foundation')), ('Sail Canvas', tcube(R + 'sail_canvas')),
        ('Striped Sail', tcube(R + 'sail_canvas_red_striped')), ('Driftwood Planks', tcube(R + 'driftwood_planks')),
        ('Palm Planks', tcube(R + 'palm_planks')), ('Palm Log', tcube(R + 'palm_log_top', R + 'palm_log')),
        ('Crop Plot', tcube(R + 'crop_plot_top', R + 'crop_plot_side')),
        ('Water Purifier', tcube(R + 'water_purifier_top', R + 'water_purifier_side', R + 'water_purifier_front')),
        ('Simple Grill', tcube(R + 'simple_grill_top_on', R + 'simple_grill_side')),
        ('Solar Panel', tcube(R + 'makeshift_solar_panel_top', R + 'makeshift_solar_panel_side')),
        ('Scrap Metal Block', tcube(R + 'scrap_metal_block')), ('Titanium Grates', tcube(R + 'titanium_grates')),
        ('Titanium Ore', tcube(R + 'titanium_ore')), ('Rope Coil', flat(R + 'rope_coil')),
        ('Scarecrow', flat(':item/scarecrow')), ('Plastic Hook', flat(':item/plastic_hook')),
        ('Fresh Water', flat(':item/fresh_water')), ('Coconut', flat(':item/coconut')),
        ('Pina Colada', flat(':item/pina_colada')), ('Rope', flat(':item/rope')),
        ('Scrap Metal', flat(':item/scrap_metal')), ('Plastic', flat(':item/plastic')),
    ]
    palette('blocks_classic.png', classic)


if __name__ == '__main__':
    os.makedirs(OUT, exist_ok=True)
    what = sys.argv[3:] or ['strips', 'header', 'collages', 'palettes']
    if 'strips' in what:
        build_strips()
    if 'header' in what:
        header('release/shots/run1/lh_hdr_2.png', (128, 150, 1920, 710))
    if 'palettes' in what:
        build_palettes()
    if 'collages' in what:
        build_collages([w for w in what if w.endswith('.jpg')] or None)
