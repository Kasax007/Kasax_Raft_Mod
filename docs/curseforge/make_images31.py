"""Images of the CurseForge page for Beach Craft 3.1 (docs/curseforge/img): the header, the video card, the "at a
glance" numbers, the two flow diagrams (the way into the Endless Sea, your own raft) and the section collages, cut
from the shader footage of the documentary "Tides of the Deep". Same look as make_images3.py, whose helpers
(outlined, rounded, waves, gradient, tex, cube/tcube, fit, caption, npc_face, save) are used here; the section strips
and the two block palettes of make_images3.py are used on the page as they are.

  python3 docs/curseforge/make_images31.py FOOTAGE_DIR FONT.ttf [what ...]

FOOTAGE_DIR: Archive/footage/bc3v2 (one MP4 per shot). FONT: Luckiest Guy; Inter Display Bold from the same folder
sets the small print. Frames are cut with ffmpeg into $BC31_FRAMES (default: a temporary folder) when first needed.
Vanilla textures (furnace, map, fishing rod ...) come from the Minecraft client jar ($MC_JAR, default: Loom cache).
what: header video glance flow_start flow_raft collages counts, or collage file names such as world_31.jpg
(default: all images). The numbers of the "at a glance" image are counted from the repository (see counts())."""
import io
import json
import math
import os
import re
import subprocess
import sys
import tempfile
import zipfile

from PIL import Image, ImageDraw, ImageEnhance, ImageFilter, ImageFont, ImageOps

import make_images3 as m3          # reads sys.argv[1:3] (SHOTS, FONT) on import, like this script

from make_images3 import NAVY, SEA, FOAM, WHITE, SAND, W, outlined, rounded, waves, gradient, tex, save

FOOT = sys.argv[1]
FONT = sys.argv[2]
SRC = os.path.join(m3.ROOT, 'src', 'main')
MODS = os.path.join(SRC, 'modules')
CACHE = os.environ.get('BC31_FRAMES') or os.path.join(tempfile.gettempdir(), 'bc31_frames')
MC_JAR = os.environ.get('MC_JAR') or os.path.expanduser('~/.gradle/caches/fabric-loom/26.3/minecraft-client.jar')
_inter = os.path.join(os.path.dirname(FONT), 'InterDisplay-Bold.otf')
SMALL = _inter if os.path.exists(_inter) else FONT
INK = (24, 44, 70)                 # small print on sand
DEEP = (6, 26, 52)
PANEL = (250, 240, 214)


def font(size):
    return ImageFont.truetype(FONT, size)


def small(size):
    return ImageFont.truetype(SMALL, size)


# ----------------------------------------------------------------------------------------------- sources
def frame(spec):
    """'clip@t': the picture at t seconds; 'clip#k': picture k (from 1) of a stills clip, one picture per second."""
    os.makedirs(CACHE, exist_ok=True)
    out = os.path.join(CACHE, spec.replace('#', '_') + '.jpg')
    if not os.path.exists(out):
        if '@' in spec:
            clip, t = spec.split('@')
            cmd = ['-ss', t, '-i', os.path.join(FOOT, clip + '.mp4'), '-frames:v', '1']
        else:
            clip, k = spec.split('#')
            cmd = ['-i', os.path.join(FOOT, clip + '.mp4'), '-vf', 'fps=1,select=eq(n\\,%d)' % (int(k) - 1), '-frames:v', '1']
        subprocess.run(['ffmpeg', '-v', 'error', '-y'] + cmd + ['-q:v', '2', out], check=True)
    return Image.open(out).convert('RGB')


def decross(im):
    """Removes the crosshair in the middle of a first-person picture (thin lines: a median filter of the spot)."""
    cx, cy = im.width // 2, im.height // 2
    r = max(14, im.width // 120)
    box = (cx - r, cy - r, cx + r, cy + r)
    im.paste(im.crop(box).filter(ImageFilter.MedianFilter(9)), box[:2])
    return im


def shot(spec, enh=None, cross=False):
    """A frame, optionally without crosshair and with (brightness, contrast, colour) applied."""
    im = frame(spec)
    if cross:
        im = decross(im)
    if enh:
        b, c, s = enh
        im = ImageEnhance.Color(ImageEnhance.Contrast(ImageEnhance.Brightness(im).enhance(b)).enhance(c)).enhance(s)
    return im


_jar = None


def van(rel, size=None):
    """Vanilla texture 'block/x' or 'item/x' from the client jar (first frame of animated textures)."""
    global _jar
    _jar = _jar or zipfile.ZipFile(MC_JAR)
    im = Image.open(io.BytesIO(_jar.read('assets/minecraft/textures/%s.png' % rel))).convert('RGBA')
    im = im.crop((0, 0, im.width, im.width))
    return im.resize((size, size), Image.NEAREST) if size else im


def vcube(top, side=None, front=None, a=48):
    t, s = van(top), van(side or top)
    return m3.cube(t, van(front) if front else s, s, a)


def icon(name, size=96):
    """The item and block pictures of the page, by short name."""
    I = {
        'ring': lambda: tex(':item/ancient_broken_ring'), 'energy': lambda: tex(':item/ancient_energy'),
        'crystal': lambda: tex(':item/ancient_crystal'), 'staff': lambda: tex(':item/energy_staff'),
        'stick': lambda: van('item/stick'), 'log': lambda: m3.tcube(':block/driftwood_log_top', ':block/driftwood_log'),
        'frame': lambda: m3.tcube(':block/driftwood_portal_frame'),
        'blast': lambda: vcube('block/blast_furnace_top', 'block/blast_furnace_side', 'block/blast_furnace_front_on'),
        'furnace': lambda: vcube('block/furnace_top', 'block/furnace_side', 'block/furnace_front_on'),
        'chest': lambda: m3.tcube('decor:block/treasure_chest_top', 'decor:block/treasure_chest_side',
                                  'decor:block/treasure_chest_front'),
        'map': lambda: van('item/buried_treasure_map'), 'rod': lambda: van('item/fishing_rod'),
        'ruins': lambda: vcube('block/mossy_stone_bricks'),
        'helm': lambda: tex('vessel:item/helm'), 'sail': lambda: tex('vessel:item/sail'),
        'oar': lambda: tex('vessel:item/oar'), 'motor': lambda: tex('vessel:item/raft_motor'),
        'foundation': lambda: m3.tcube(':block/raft_foundation'),
        'hibiscus': lambda: tex('world:block/hibiscus'), 'plumeria': lambda: tex('world:block/plumeria'),
        'porthole': lambda: m3.tcube('building:block/porthole'), 'claw': lambda: tex('shore:item/crab_claw'),
        'scale': lambda: tex('boss:item/leviathan_scale'),
        'tidestone': lambda: m3.tcube('building:block/glowing_tidestone', overlay='building:block/glowing_tidestone_runes'),
        'disc': lambda: tex(':item/some_game_music_music_disc'), 'clock': lambda: van('item/clock_00'),
        'journal': lambda: tex('story:item/expedition_journal'), 'helmet': lambda: tex('diving:item/diving_helmet'),
        'orchid': lambda: tex('world:block/tropical_orchid'),
        'cutlass': lambda: tex('shore:item/cutlass'), 'parrot': lambda: van('item/parrot_spawn_egg'),
        'parasol': lambda: tex('cozy:item/thatch_parasol'), 'curry': lambda: tex('food:item/island_curry'),
        'boomerang': lambda: tex('gear:item/boomerang'),
    }
    im = I[name]()
    return im.resize((size, size * im.height // im.width), Image.NEAREST)


def face(npc, size):
    return m3.npc_face(npc, size)


# ----------------------------------------------------------------------------------------------- the numbers
def counts():
    """Every number of the "at a glance" image, counted from the repository."""
    dim = json.load(open(os.path.join(MODS, 'world', 'data', 'raft', 'dimension', 'raftdim.json')))
    biomes = {b['biome'] for b in dim['generator']['biome_source']['biomes']}

    def structures(module):
        d = os.path.join(MODS, module, 'data', 'raft', 'worldgen', 'structure')
        return [f for f in os.listdir(d) if f.endswith('.json')]

    story = sum(len(structures(m)) for m in ('structures_surface', 'structures_deep', 'structures_city'))
    side = len(structures('structures_side'))
    npc = open(os.path.join(SRC, 'java', 'net', 'kasax', 'raft', 'content', 'npc', 'NpcCharacter.java')).read()
    people = len(re.findall(r'^ {4}[A-Z_]+\("', npc, re.M))

    def entities(lang):
        d = json.load(open(lang, encoding='utf-8'))
        return [k for k in d if k.startswith('entity.raft.') and k.count('.') == 2]

    lang = lambda m: os.path.join(MODS, m, 'assets', 'raft', 'lang', 'en_us.json')
    bruce = 'entity.raft.shark' in entities(os.path.join(SRC, 'resources', 'assets', 'raft', 'lang', 'en_us.json'))
    opt = lambda m: os.path.isdir(os.path.join(MODS, m))
    enemies = [e for e in entities(lang('encounters')) if not e.endswith('_spawn_egg')] if opt('encounters') else []
    creatures = len(entities(lang('shore'))) + len(entities(lang('marine'))) + len(enemies) + int(bruce)
    blocks = lambda m: len(os.listdir(os.path.join(MODS, m, 'assets', 'raft', 'blockstates')))
    lev = open(os.path.join(SRC, 'java', 'net', 'kasax', 'raft', 'content', 'boss', 'entity', 'LeviathanEntity.java')).read()
    health = int(float(re.search(r'MAX_HEALTH,\s*([\d.]+)', lev).group(1)))
    return dict(biomes=len(biomes), story=story, side=side, places=story + side, people=people,
                new_creatures=creatures - int(bruce), creatures=creatures, building=blocks('building'),
                decor=blocks('decor'), boss_health=health, enemies=len(enemies),
                cozy=blocks('cozy') if opt('cozy') else 0, kitchen=blocks('food') if opt('food') else 0)


# ----------------------------------------------------------------------------------------------- drawing helpers
def text_w(d, s, f):
    return d.textlength(s, font=f)


def wrap(d, s, f, width):
    lines = ['']
    for word in s.split():
        t = (lines[-1] + ' ' + word).strip()
        if text_w(d, t, f) <= width or not lines[-1]:
            lines[-1] = t
        else:
            lines.append(word)
    return lines


def para(d, xy, s, f, width, fill=INK, gap=6, anchor='la'):
    """Wrapped text; anchor 'ma' centres every line on x. Returns the y below the last line."""
    x, y = xy
    asc, desc = f.getmetrics()
    for ln in wrap(d, s, f, width):
        d.text((x, y), ln, font=f, fill=fill, anchor=anchor)
        y += asc + desc + gap
    return y


def shadowed(im, layer, xy, blur=10, offset=(0, 6), alpha=110):
    """Pastes an RGBA layer with a soft drop shadow."""
    sh = Image.new('RGBA', (layer.width + 4 * blur, layer.height + 4 * blur), (0, 0, 0, 0))
    a = layer.split()[3].point(lambda v: v * alpha // 255)
    sh.paste((0, 0, 0, 255), (2 * blur, 2 * blur), a)
    sh = sh.filter(ImageFilter.GaussianBlur(blur))
    im.alpha_composite(sh, (xy[0] - 2 * blur + offset[0], xy[1] - 2 * blur + offset[1]))
    im.alpha_composite(layer, xy)


def card(w, h, r=22, fill=PANEL, outline=WHITE, width=4):
    c = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    g = gradient(w, h, fill, tuple(max(0, v - 16) for v in fill), horizontal=False)
    m = Image.new('L', (w, h), 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, w - 1, h - 1], r, fill=255)
    c.paste(g, (0, 0), m)
    if outline:
        ImageDraw.Draw(c).rounded_rectangle([0, 0, w - 1, h - 1], r, outline=outline + (255,), width=width)
    return c


def background(h):
    im = gradient(W, h, NAVY, (16, 78, 128), horizontal=False)
    waves(im, h - 46, FOAM + (40,), amp=4, period=80, step=14, width=2)
    return im


def arrow(d, pts, color=SAND, width=14, head=26, outline=DEEP):
    """An outlined arrow along the points (head at the last one)."""
    (xa, ya), (xb, yb) = pts[-2], pts[-1]
    ang = math.atan2(yb - ya, xb - xa)
    bx, by = xb - head * math.cos(ang), yb - head * math.sin(ang)
    nx, ny = -math.sin(ang), math.cos(ang)
    line = list(pts[:-1]) + [(bx + 2 * math.cos(ang), by + 2 * math.sin(ang))]
    tri = [(xb, yb), (bx + nx * head * 0.9, by + ny * head * 0.9), (bx - nx * head * 0.9, by - ny * head * 0.9)]
    mx, my = sum(p[0] for p in tri) / 3, sum(p[1] for p in tri) / 3
    big = [(mx + (px - mx) * 1.32, my + (py - my) * 1.32) for px, py in tri]
    for col, extra, t in ((outline, 7, big), (color, 0, tri)):
        r = (width + extra) / 2
        d.ellipse([line[0][0] - r, line[0][1] - r, line[0][0] + r, line[0][1] + r], fill=col)
        d.line(line, fill=col, width=width + extra, joint='curve')
        d.polygon(t, fill=col)


def badge(im, center, n, r=26, fill=SEA):
    d = ImageDraw.Draw(im)
    x, y = center
    d.ellipse([x - r - 4, y - r - 4, x + r + 4, y + r + 4], fill=WHITE)
    d.ellipse([x - r, y - r, x + r, y + r], fill=fill)
    outlined(d, (x, y + 2), str(n), font(int(r * 1.3)), stroke=3, anchor='mm')


def slot(im, x, y, s=64, item=None, count=None):
    """A slot of the crafting grid (vanilla look) with an item."""
    d = ImageDraw.Draw(im)
    d.rectangle([x, y, x + s - 1, y + s - 1], fill=(139, 139, 139))
    d.line([(x, y), (x + s - 1, y)], fill=(55, 55, 55), width=4)
    d.line([(x, y), (x, y + s - 1)], fill=(55, 55, 55), width=4)
    d.line([(x, y + s - 2), (x + s - 1, y + s - 2)], fill=WHITE, width=4)
    d.line([(x + s - 2, y), (x + s - 2, y + s - 1)], fill=WHITE, width=4)
    if item is not None:
        ic = item.resize((s - 16, (s - 16) * item.height // item.width), Image.NEAREST)
        im.alpha_composite(ic, (x + (s - ic.width) // 2, y + (s - ic.height) // 2))
    if count:
        outlined(d, (x + s - 6, y + s - 4), str(count), font(int(s * 0.42)), stroke=3, stroke_fill=(40, 40, 40),
                 anchor='rd')


def grid(im, x, y, items, s=64, gap=6):
    """3x3 crafting grid; items[row][col] = icon or None."""
    for r in range(3):
        for c in range(3):
            slot(im, x + c * (s + gap), y + r * (s + gap), s, items[r][c])
    return 3 * s + 2 * gap


def keycap(im, x, y, label, w=None, h=50):
    d = ImageDraw.Draw(im)
    f = small(24)
    w = w or max(h, int(text_w(d, label, f)) + 28)
    d.rounded_rectangle([x, y + 5, x + w, y + h + 5], 9, fill=(120, 108, 84))
    d.rounded_rectangle([x, y, x + w, y + h], 9, fill=WHITE, outline=(120, 108, 84), width=2)
    d.text((x + w / 2, y + h / 2), label, font=f, fill=INK, anchor='mm')
    return w


def anchor_glyph(size, color=WHITE):
    """A simple ship's anchor (drawn, it is a symbol here, not an item)."""
    s = 4 * size
    g = Image.new('RGBA', (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(g)
    lw = s // 11
    cx = s // 2
    d.ellipse([cx - s * 0.11, s * 0.04, cx + s * 0.11, s * 0.26], outline=color, width=lw)
    d.line([(cx, s * 0.25), (cx, s * 0.88)], fill=color, width=lw)
    d.line([(cx - s * 0.22, s * 0.36), (cx + s * 0.22, s * 0.36)], fill=color, width=lw)
    d.arc([s * 0.12, s * 0.34, s * 0.88, s * 0.9], 25, 155, fill=color, width=lw)
    for side in (-1, 1):
        px, py = cx + side * s * 0.36, s * 0.58
        d.polygon([(px - side * s * 0.02, py - s * 0.14), (px + side * s * 0.12, py + s * 0.04), (px - side * s * 0.09, py + s * 0.05)],
                  fill=color)
    return g.resize((size, size), Image.LANCZOS)


def play_glyph(size, ring=WHITE, tri=SEA):
    s = 4 * size
    g = Image.new('RGBA', (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(g)
    d.ellipse([0, 0, s - 1, s - 1], fill=ring + (240,))
    d.polygon([(s * 0.38, s * 0.27), (s * 0.38, s * 0.73), (s * 0.76, s * 0.5)], fill=tri)
    return g.resize((size, size), Image.LANCZOS)


# ----------------------------------------------------------------------------------------------- header
def header31(name='header_31.png', size=(1600, 500), title='BEACH CRAFT 3.1'):
    """The jungle isle from the air, scaled so the whole island fits; its foggy left edge is extended under the title."""
    w, h = size
    src = shot('b_jungle_air@1.6', enh=(1.02, 1.08, 1.12))
    sc, y0, ramp = 0.7, 112, 180
    pic = src.resize((int(src.width * sc), int(src.height * sc)), Image.LANCZOS).crop((0, y0, int(src.width * sc), y0 + h))
    x0 = w - pic.width
    fog = pic.crop((0, 0, ramp, h)).resize((1, h), Image.BOX).resize((x0 + ramp, h), Image.NEAREST)
    im = Image.new('RGB', size)
    im.paste(fog.filter(ImageFilter.GaussianBlur(10)), (0, 0))
    mask = Image.new('L', pic.size, 255)
    md = ImageDraw.Draw(mask)
    for x in range(ramp):
        md.line([(x, 0), (x, h)], fill=int(255 * x / ramp))
    im.paste(pic, (x0, 0), mask)
    im = im.convert('RGBA')
    shade = Image.new('RGBA', size)
    d = ImageDraw.Draw(shade)
    for x in range(w):
        t = max(0.0, 1 - x / (w * 0.62))
        d.line([(x, 0), (x, h)], fill=NAVY + (int(230 * t ** 1.25),))
    im = ImageOps.posterize(Image.alpha_composite(im, shade).convert('RGB'), 6).convert('RGBA')   # keeps the PNG small
    d = ImageDraw.Draw(im)
    outlined(d, (72, 70), title, font(124), stroke=10)
    outlined(d, (80, 236), 'Tides of the Deep', font(74), fill=SAND, stroke=7)
    tag = 'Minecraft 26.3 · Fabric'
    f = font(36)
    tw = d.textlength(tag, font=f)
    d.rounded_rectangle([80, 368, 80 + tw + 52, 422], 27, fill=SEA + (255,), outline=WHITE + (255,), width=4)
    d.text((106, 375), tag, font=f, fill=WHITE)
    save(rounded(im, 30), name)


# ----------------------------------------------------------------------------------------------- video card
def video_card(name='video.jpg', minutes=21, length='21:27'):
    """Fallback picture under the YouTube player: Saltmere from the air, a big play button."""
    w, h = 1200, 675
    im = shot('n_nalu_place@3.6', enh=(1.0, 1.06, 1.1)).resize((w, h), Image.LANCZOS).convert('RGBA')
    shade = Image.new('RGBA', (w, h))
    d = ImageDraw.Draw(shade)
    for y in range(h):
        t = abs(y - h * 0.5) / (h * 0.5)
        d.line([(0, y), (w, y)], fill=DEEP + (int(70 + 120 * t ** 1.6),))
    im = Image.alpha_composite(im, shade)
    d = ImageDraw.Draw(im)
    outlined(d, (w // 2, 88), 'TIDES OF THE DEEP', font(78), stroke=8, anchor='mm')
    outlined(d, (w // 2, 158), 'The Beach Craft 3.1 documentary, with shaders', font(34), fill=SAND, stroke=5,
             anchor='mm')
    p = play_glyph(190)
    shadowed(im, p, ((w - 190) // 2, (h - 190) // 2 + 10), blur=14, offset=(0, 8), alpha=150)
    d = ImageDraw.Draw(im)
    outlined(d, (w // 2, h - 92), 'WATCH THE %d-MINUTE TOUR' % minutes, font(60), fill=WHITE, stroke=8, anchor='mm')
    f = small(30)
    tw = d.textlength(length, font=f)
    d.rounded_rectangle([w - 40 - tw - 28, h - 70, w - 40, h - 28], 8, fill=(0, 0, 0, 200))
    d.text((w - 40 - 14, h - 49), length, font=f, fill=WHITE, anchor='rm')
    save(im.convert('RGB'), name)


# ----------------------------------------------------------------------------------------------- at a glance
NEW_31 = [('helm', 'Build your\nown raft'), ('face', 'Meet the people\nof the sea'), ('disc', 'New sounds\nand music'),
          ('clock', 'Day and night\nat sea'), ('orchid', 'Six new\nisland flowers')]   # changelog-3.1.0+26.3.md
NEW_33 = [('sail', 'Sail, mast\nand anchor'), ('cutlass', 'Guarded places,\nnew enemies'),
          ('parrot', 'Islands full\nof animals'), ('parasol', 'Cozy living\nand comfort'),
          ('curry', 'Crops and an\nisland kitchen'), ('boomerang', 'Gear and\ngadgets')]   # changelogs 3.2.0 and 3.3.0


def glance(name='glance.png', pill='NEW IN 3.1', new=None, all_blocks=False):
    """all_blocks: the tile counts the cozy and kitchen blocks of 3.3 too."""
    new = new or NEW_31
    c = counts()
    print('counts', c)
    tiles = [
        ('hibiscus', c['biomes'], 'BIOMES', 'islands, reefs and trenches'),
        ('porthole', c['places'], 'PLACES', '%d story places, %d side places' % (c['story'], c['side'])),
        ('face', c['people'], 'PEOPLE', 'to talk and trade with'),
        ('claw', c['creatures'], 'CREATURES', 'Bruce the shark and %d more' % c['new_creatures']),
        ('tidestone', c['building'] + c['decor'], 'BLOCKS', '%d building blocks, %d decorations' % (c['building'], c['decor']))
        if not all_blocks else
        ('tidestone', c['building'] + c['decor'] + c['cozy'] + c['kitchen'], 'BLOCKS',
         '%d building blocks, %d for decor and kitchen' % (c['building'], c['decor'] + c['cozy'] + c['kitchen'])),
        ('scale', 1, 'BOSS', 'the Leviathan, %d health' % c['boss_health']),
    ]
    cols, tw, th, gx, gy = 3, 368, 216, 16, 16
    top, band_h = 100, 176
    by = top + 2 * th + gy + 40
    h = by + band_h + 40
    im = background(h)
    d = ImageDraw.Draw(im)
    outlined(d, (W // 2, 56), 'BEACH CRAFT AT A GLANCE', font(56), stroke=7, anchor='mm')
    x0 = (W - cols * tw - (cols - 1) * gx) // 2
    for k, (ic, num, label, sub) in enumerate(tiles):
        x, y = x0 + (k % cols) * (tw + gx), top + (k // cols) * (th + gy)
        shadowed(im, card(tw, th), (x, y), blur=8, offset=(0, 5), alpha=90)
        d = ImageDraw.Draw(im)
        cx, cy = x + 82, y + 88
        d.ellipse([cx - 62, cy - 62, cx + 62, cy + 62], fill=(232, 214, 172))
        pic = face('keeper_fenn', 84) if ic == 'face' else icon(ic, 92)
        im.alpha_composite(pic, (cx - pic.width // 2, cy - pic.height // 2))
        d = ImageDraw.Draw(im)
        outlined(d, (x + 166, y + 104), str(num), font(100), fill=WHITE, stroke=7, stroke_fill=SEA, anchor='ls')
        d.text((x + 168, y + 146), label, font=font(34), fill=NAVY, anchor='ls')
        f = small(21)
        while d.textlength(sub, font=f) > tw - 28:
            f = small(f.size - 1)
        d.text((x + tw // 2, y + th - 30), sub, font=f, fill=INK, anchor='ms')
    # new in 3.1
    band = Image.new('RGBA', (W - 32, band_h), (0, 0, 0, 0))
    ImageDraw.Draw(band).rounded_rectangle([0, 0, band.width - 1, band_h - 1], 24, fill=SEA + (255,),
                                           outline=WHITE + (255,), width=4)
    im.alpha_composite(band, (16, by))
    d = ImageDraw.Draw(im)
    f = font(32)
    pw = d.textlength(pill, font=f) + 44
    d.rounded_rectangle([W // 2 - pw / 2, by - 24, W // 2 + pw / 2, by + 24], 24, fill=SAND, outline=WHITE, width=4)
    d.text((W // 2, by + 2), pill, font=f, fill=NAVY, anchor='mm')
    n = len(new)
    cw = (W - 64) / n
    for k, (ic, label) in enumerate(new):
        cx = 32 + cw * k + cw / 2
        pic = face('nalu', 56) if ic == 'face' else icon(ic, 64)
        im.alpha_composite(pic, (int(cx - pic.width / 2), by + 40 + (64 - pic.height) // 2))
        d = ImageDraw.Draw(im)
        for i, ln in enumerate(label.split('\n')):
            d.text((cx, by + 114 + 28 * i), ln, font=small(23), fill=WHITE, anchor='ma')
    save(rounded(im, 26), name)


# ----------------------------------------------------------------------------------------------- flow: the way in
def photo_card(pic, w, h, photo_h, n, title, text, focus=(0.5, 0.5), zoom=1.0, box=None):
    """A card with a picture on top, a step number, a title and a line or two of text."""
    c = card(w, h)
    ph = m3.fit(pic, w - 16, photo_h, focus, zoom, box).convert('RGBA')
    c.alpha_composite(rounded(ph, 16), (8, 8))
    d = ImageDraw.Draw(c)
    y = card_title(c, (20, photo_h + 22), n, title)
    para(d, (20, y), text, small(21), w - 40)
    return c


def card_title(c, xy, n, title, size=30):
    """Step badge and title on a card; returns the y below."""
    x, y = xy
    badge(c, (x + 20, y + 20), n, r=20)
    d = ImageDraw.Draw(c)
    f = font(size)
    while d.textlength(title, font=f) > c.width - x - 70 and f.size > 18:
        f = font(f.size - 1)
    d.text((x + 50, y + 22), title, font=f, fill=NAVY, anchor='lm')
    return y + 54


def icon_card(w, h, n, title):
    c = card(w, h)
    card_title(c, (20, 16), n, title)
    return c


def flow_start(name='flow_start.png'):
    """Driftwood, the ring and its finds, smelting, the two recipes and the lit portal; recipes as in
    src/main/generated/data/raft/recipe, finds as in util/ModLootTableModifiers.java."""
    M, ch = 24, 336
    rows = (M, M + ch + 84)
    h = rows[1] + ch + 34
    im = background(h)
    stump = shot('w_beach@2.5', enh=(1.0, 1.08, 1.1))
    portal = shot('w_light@3.2', enh=(1.0, 1.06, 1.1))
    # row 1: driftwood, the ring, smelting
    a = photo_card(stump, 330, ch, 196, 1, 'DRIFTWOOD', 'Dark stumps on overworld beaches give the logs.',
                   focus=(0.5, 0.47), zoom=2.1)
    b = icon_card(400, ch, 2, 'ANCIENT BROKEN RING')
    ring = icon('ring', 104)
    b.alpha_composite(ring, ((b.width - ring.width) // 2, 70))
    bd = ImageDraw.Draw(b)
    bd.text((b.width // 2, 192), 'found in', font=small(20), fill=INK, anchor='mm')
    srcs = [('chest', 'Shipwreck\ntreasure'), ('map', 'Buried\ntreasure'), ('ruins', 'Ocean\nruins'), ('rod', 'Fishing')]
    cw = (b.width - 24) / len(srcs)
    for k, (ic, lab) in enumerate(srcs):
        cx = 12 + cw * k + cw / 2
        pic = icon(ic, 56)
        b.alpha_composite(pic, (int(cx - pic.width / 2), 212))
        for i, ln in enumerate(lab.split('\n')):
            bd.text((cx, 276 + 22 * i), ln, font=small(18), fill=INK, anchor='ma')
    c = icon_card(330, ch, 3, 'SMELT IT')
    cd = ImageDraw.Draw(c)
    bl, en = icon('blast', 104), icon('energy', 96)
    c.alpha_composite(bl, (26, 92))
    arrow(cd, [(146, 146), (200, 146)], color=SEA, width=10, head=20, outline=WHITE)
    c.alpha_composite(en, (212, 98))
    para(cd, (20, 222), 'In a furnace or a blast furnace the ring turns into Ancient Energy.', small(21), c.width - 40)
    # row 2: frames, staff, light
    log, energy = icon('log', 64), icon('energy', 64)
    d_ = icon_card(400, ch, 4, 'PORTAL FRAMES')
    gs = 60
    gw = grid(d_, 22, 74, [[None, log, None], [log, energy, log], [None, log, None]], s=gs, gap=4)
    text_y = 74 + gw + 14                # the notes of cards 4 and 5 start on the same line
    dd = ImageDraw.Draw(d_)
    arrow(dd, [(22 + gw + 12, 74 + gw // 2), (22 + gw + 58, 74 + gw // 2)], color=SEA, width=10, head=20, outline=WHITE)
    slot(d_, 22 + gw + 72, 74 + gw // 2 - 40, 80, icon('frame', 64), count=4)
    para(dd, (20, text_y), 'The smallest portal needs 10 frames.', small(21), d_.width - 40)
    e = icon_card(330, ch, 5, 'ENERGY STAFF')
    gw2 = grid(e, 20, 78, [[None, energy, None], [None, icon('crystal', 64), None], [None, icon('stick', 64), None]],
               s=56, gap=4)
    ed = ImageDraw.Draw(e)
    arrow(ed, [(20 + gw2 + 10, 78 + gw2 // 2), (20 + gw2 + 44, 78 + gw2 // 2)], color=SEA, width=10, head=20, outline=WHITE)
    slot(e, 20 + gw2 + 54, 78 + gw2 // 2 - 36, 72, icon('staff', 64))
    para(ed, (20, text_y), 'Ancient Crystals hide in shipwreck treasure too.', small(21), e.width - 40)
    f_ = photo_card(portal, 330, ch, 196, 6, 'LIGHT IT', 'Build the frame like a Nether portal and light it.',
                    box=(330, 0, 1590, 860))
    placed = []
    for y, cards in zip(rows, ((a, b, c), (d_, e, f_))):
        gap = (W - 2 * M - sum(k.width for k in cards)) / (len(cards) - 1)
        x = M
        for k in cards:
            shadowed(im, k, (int(x), y), blur=9, offset=(0, 6), alpha=110)
            placed.append((int(x), y, k.width, k.height))
            x += k.width + gap
    d = ImageDraw.Draw(im)
    for i in (0, 1, 3, 4):
        x0, y0, w0, h0 = placed[i]
        x1 = placed[i + 1][0]
        arrow(d, [(x0 + w0 + 10, y0 + h0 // 2), (x1 - 10, y0 + h0 // 2)])
    x0, y0, w0, h0 = placed[2]
    x1, y1, w1, _ = placed[3]
    ym = y0 + h0 + 30
    arrow(d, [(x0 + w0 // 2, y0 + h0 + 8), (x0 + w0 // 2, ym), (x1 + w1 // 2, ym), (x1 + w1 // 2, y1 - 6)])
    save(rounded(im, 26), name)


# ----------------------------------------------------------------------------------------------- flow: your own raft
def flow_raft(name='flow_raft.png'):
    """Foundations, helm and keys, power, anchor (vessel module: VesselEntity power 1/3/6, changelog 3.1.0)."""
    M, gapx, ch = 24, 44, 404
    n = 4
    cw = (W - 2 * M - (n - 1) * gapx) // n
    h = ch + 2 * M + 10
    im = background(h)
    cards = []
    # 1 foundations
    c1 = icon_card(cw, ch, 1, 'FOUNDATIONS')
    d1 = ImageDraw.Draw(c1)
    water = (60, 150, 200)
    d1.rounded_rectangle([18, 150, cw - 18, 206], 16, fill=water)
    for k in range(6):
        y = 160 + 10 * (k % 2)
        d1.arc([22 + 36 * k, y, 58 + 36 * k, y + 16], 200, 340, fill=FOAM, width=3)
    fo = icon('foundation', 64)
    for i, j in sorted(((i, j) for i in range(3) for j in range(2)), key=lambda p: p[0] + p[1]):
        c1.alpha_composite(fo, (cw // 2 - 32 + (i - j) * 32 - 16, 82 + (i + j) * 16))
    para(d1, (20, 224), 'Place them right on open water. Add more and your raft grows, block by block.', small(21), cw - 36)
    cards.append(c1)
    # 2 helm
    c2 = icon_card(cw, ch, 2, 'HELM')
    d2 = ImageDraw.Draw(c2)
    hm = icon('helm', 92)
    c2.alpha_composite(hm, ((cw - hm.width) // 2, 66))
    para(d2, (cw // 2, 168), 'Use it to set sail.', small(21), cw - 36, anchor='ma')
    keys = [('W', 'S', 'sail'), ('A', 'D', 'steer')]
    y = 210
    for a, b, lab in keys:
        x = 28
        x += keycap(c2, x, y, a, 46, 46) + 6
        x += keycap(c2, x, y, b, 46, 46) + 12
        d2.text((x, y + 24), lab, font=small(22), fill=INK, anchor='lm')
        y += 62
    x = 28
    x += keycap(c2, x, y, 'Shift', None, 46) + 12
    d2.text((x, y + 24), 'leave', font=small(22), fill=INK, anchor='lm')
    cards.append(c2)
    # 3 power
    c3 = icon_card(cw, ch, 3, 'POWER')
    d3 = ImageDraw.Draw(c3)
    parts = [('oar', 'Oar', '+1'), ('sail', 'Sail', '+3'), ('motor', 'Motor', '+6')]
    pw = (cw - 24) / 3
    for k, (ic, lab, val) in enumerate(parts):
        cx = 12 + pw * k + pw / 2
        pic = icon(ic, 72)
        c3.alpha_composite(pic, (int(cx - pic.width / 2), 74))
        d3.text((cx, 164), lab, font=small(20), fill=INK, anchor='mm')
        outlined(d3, (cx, 202), val, font(40), fill=WHITE, stroke=5, stroke_fill=SEA, anchor='mm')
    para(d3, (20, 240), 'Stack sails for a taller mast. The motor runs on furnace fuel. Heavy rafts are slower.',
         small(21), cw - 36)
    cards.append(c3)
    # 4 anchor
    c4 = icon_card(cw, ch, 4, 'DROP ANCHOR')
    d4 = ImageDraw.Draw(c4)
    r = 56
    cx, cy = cw // 2, 128
    d4.ellipse([cx - r - 5, cy - r - 5, cx + r + 5, cy + r + 5], fill=WHITE)
    d4.ellipse([cx - r, cy - r, cx + r, cy + r], fill=SEA)
    an = anchor_glyph(84)
    c4.alpha_composite(an, (cx - 42, cy - 44))
    para(d4, (20, 214), 'Use the wheel again: the raft turns back into blocks, just as it was.', small(21), cw - 36)
    cards.append(c4)
    x = M
    xs = []
    for c in cards:
        shadowed(im, c, (x, M), blur=9, offset=(0, 6), alpha=110)
        xs.append(x)
        x += cw + gapx
    d = ImageDraw.Draw(im)
    for k in range(n - 1):
        x0 = xs[k] + cw
        arrow(d, [(x0 + 4, M + ch // 2), (x0 + gapx - 4, M + ch // 2)], width=12, head=22)
    save(rounded(im, 26), name)


# ----------------------------------------------------------------------------------------------- collages
def S(spec, cap=None, w=1.0, focus=(0.5, 0.5), zoom=1.0, box=None, enh=None, cross=False, sub=None):
    """A collage tile cut from a frame of the footage (see make_images3.collage)."""
    return dict(src=shot(spec, enh, cross), cap=cap, w=w, focus=focus, zoom=zoom, box=box, sub=sub)


def caption2(tile, name, sub):
    """Name in the display font, the place under it in the small print."""
    d = ImageDraw.Draw(tile)
    f, fs = font(28), small(19)
    while d.textlength(name, font=f) > tile.width - 28 and f.size > 16:
        f = font(f.size - 1)
    band = 86
    shade = Image.new('RGBA', tile.size, (0, 0, 0, 0))
    sd = ImageDraw.Draw(shade)
    for i in range(band):
        sd.line([(0, tile.height - band + i), (tile.width, tile.height - band + i)],
                fill=(5, 20, 40, int(190 * (i / band) ** 1.2)))
    tile.alpha_composite(shade)
    d = ImageDraw.Draw(tile)
    outlined(d, (14, tile.height - 40), name, f, stroke=4, anchor='ls')
    d.text((15, tile.height - 14), sub, font=fs, fill=SAND, anchor='ls', stroke_width=2, stroke_fill=(5, 20, 40))


def collage31(name, rows):
    """make_images3.collage with an optional second caption line (t['sub'])."""
    PAD, GAP = m3.PAD, m3.GAP
    inner = W - 2 * PAD
    total_h = PAD + sum(h for h, _ in rows) + GAP * (len(rows) - 1) + PAD
    im = gradient(W, total_h, NAVY, (16, 78, 128), horizontal=False)
    waves(im, total_h - 40, FOAM + (40,), amp=4, period=80, step=14, width=2)
    y = PAD
    for h, tiles in rows:
        weights = sum(t.get('w', 1) for t in tiles)
        avail = inner - GAP * (len(tiles) - 1)
        x = PAD
        for i, t in enumerate(tiles):
            tw = int(avail * t.get('w', 1) / weights) if i < len(tiles) - 1 else PAD + inner - x
            tile = m3.fit(t['src'], tw, h, t.get('focus', (0.5, 0.5)), t.get('zoom', 1.0), t.get('box')).convert('RGBA')
            if t.get('sub'):
                caption2(tile, t['cap'], t['sub'])
            elif t.get('cap'):
                m3.caption(tile, t['cap'], 30 if tw > 360 else 24)
            frame_ = Image.new('RGBA', (tw, h), (0, 0, 0, 0))
            ImageDraw.Draw(frame_).rounded_rectangle([0, 0, tw - 1, h - 1], 16, fill=WHITE + (255,))
            frame_.alpha_composite(rounded(tile.crop((3, 3, tw - 3, h - 3)), 13), (3, 3))
            im.alpha_composite(frame_, (x, y))
            x += tw + GAP
        y += h + GAP
    save(im.convert('RGB'), name)


def collages31():
    """Section collages; frames and crops picked from contact sheets of footage/bc3v2."""
    return {
        'world_31.jpg': lambda: [
            (330, [S('b_coral@3.0', 'Coral Shallows', 1.25, enh=(1.0, 1.04, 1.06)),
                   S('n_teo_place@2.4', 'Palm Isle', 1.0, (0.5, 0.45), 1.1, enh=(1.0, 1.04, 1.06))]),
            (236, [S('b_kelp@1.2', 'Kelp Forest', zoom=1.1),
                   S('b_floes@3.0', 'Frost Floes', focus=(0.33, 0.55), zoom=1.25),
                   S('n_voss_place@0.9', 'Volcanic Isle', focus=(0.35, 0.62), zoom=1.3)]),
            (236, [S('b_mangrove@3.5', 'Mangrove Isle', zoom=1.1),
                   S('b_lagoon@3.0', 'Glowing Lagoon', zoom=1.15, enh=(1.18, 1.06, 1.1)),
                   S('stills_g2#2', 'Island flowers', zoom=1.1),
                   S('b_daynight@3.0', 'Day and night', focus=(0.3, 0.42), zoom=1.2)]),
        ],
        'places_31.jpg': lambda: [
            (330, [S('p_solace_street@2.0', 'Port Solace, the drowned city', 1.2, (0.5, 0.5), 1.05, enh=(1.3, 1.1, 1.12)),
                   S('p_saltmere_air@3.0', 'Saltmere', 1.0, (0.5, 0.55), 1.2)]),
            (236, [S('p_lighthouse_air@1.4', 'Old Lighthouse', focus=(0.44, 0.45), zoom=1.35),
                   S('p_helix_in@2.5', 'Helix Station'),
                   S('p_temple_air@4.0', 'Coral Temple'),
                   S('p_forge_bridge@2.5', 'Ashen Forge')]),
            (236, [S('p_rig_air@3.0', 'Rusted Rig', focus=(0.55, 0.52), zoom=1.7),
                   S('p_raft@1.0', 'Abandoned Raft', focus=(0.55, 0.42), zoom=1.6),
                   S('p_iceberg_air@3.0', 'Iceberg Outpost', focus=(0.42, 0.42), zoom=1.45),
                   S('stills_b#1', "Saltjaw's treasure")]),
        ],
        'people_31.jpg': lambda: [
            (272, [S('n_nalu_greet@0.5', 'Talk: hints, rumours and gifts', 2.6, box=(350, 590, 1570, 1060)),
                   S('n_teo_trade@1.2', 'Trade for gold and pearls', 1.63, box=(520, 270, 1400, 810))]),
            (330, [S('n_fenn_gift@0.5', 'Keeper Fenn', box=(591, 10, 1329, 870), cross=True, sub='Old Lighthouse'),
                   S('n_nalu_gift@0.5', 'Nalu', box=(631, 70, 1249, 790), cross=True, sub='Saltmere'),
                   S('n_tamo_work@0.9', 'Old Tamo', box=(658, 170, 1241, 850), sub='Saltmere docks'),
                   S('n_voss_gift@0.5', 'Dr. Voss', box=(617, 50, 1303, 850), cross=True, sub='Ashen Forge')]),
            (330, [S('n_teo_work@3.6', 'Teo', box=(624, 110, 1276, 870), sub='Tiki bars'),
                   S('n_kai_work@0.9', 'Kai', box=(854, 140, 1506, 900), sub='Castaway camps'),
                   S('n_lund_gift@0.5', 'Dr. Lund', box=(605, 60, 1326, 900), cross=True, sub='Iceberg outposts'),
                   S('n_okafor_gift@0.5', 'Okafor', box=(589, 0, 1361, 900), cross=True, sub='Rusted rigs')]),
        ],
        'creatures_31.jpg': lambda: [
            (330, [S('c_jelly@1.0', 'Jellyfish', 1.15, (0.6, 0.42), 1.1),
                   S('c_crab@2.5', 'Crabs', 1.0, (0.62, 0.52), 1.3)]),
            (236, [S('c_shark@1.2', 'Bruce the shark', focus=(0.44, 0.52), zoom=2.5),
                   S('c_otter@1.2', 'Sea Otters', focus=(0.34, 0.55), zoom=1.5),
                   S('c_gull_peck@2.5', 'Seagulls', focus=(0.42, 0.55), zoom=1.2),
                   S('c_ember@2.5', 'Ember Crabs', focus=(0.48, 0.58), zoom=1.6)]),
            (236, [S('c_whale@1.2', 'Whale', focus=(0.44, 0.54), zoom=2.3),
                   S('c_manta@2.5', 'Manta Ray', focus=(0.44, 0.62), zoom=1.9),
                   S('c_angler@1.7', 'Anglerfish', focus=(0.5, 0.48), zoom=2.4, enh=(1.35, 1.08, 1.0)),
                   S('c_moray@2.2', 'Moray Eel', focus=(0.52, 0.47), zoom=2.0),
                   S('c_penguin@2.5', 'Penguin', focus=(0.48, 0.57), zoom=2.8)]),
        ],
        'raft_31.jpg': lambda: [
            (330, [S('r_sail_start@2.8', 'Sail the raft you built', 1.3, (0.42, 0.45), 1.35),
                   S('r_build_sail@3.2', 'Build anything on it', 1.0, (0.45, 0.6), 1.2)]),
            (236, [S('r_sea_wheat@4.0', 'Crops keep growing'),
                   S('r_sea_lever@1.5', 'Redstone works', zoom=1.15),
                   S('r_anchor@4.0', 'Drop anchor', focus=(0.4, 0.58), zoom=1.45),
                   S('f_raft_upgrades@4.0', 'Or the Expedition Raft', focus=(0.42, 0.45), zoom=1.6)]),
        ],
        'diving_31.jpg': lambda: [
            (400, [S('f_dive@2.5', 'Helmet, tank, wetsuit and flippers', focus=(0.55, 0.5), zoom=1.15)]),
            (250, [S('f_gear@1.2', "The full set: Dolphin's Grace", box=(660, 255, 1600, 872)),
                   S('stills_p1#1', 'Air Compressor', box=(570, 340, 1170, 740)),
                   S('f_lamp@4.0', 'Helmet Lamp in the trench', enh=(1.2, 1.05, 1.0))]),
        ],
        'story_31.jpg': lambda: [
            (340, [S('stills_g3#2', 'Expedition Journal', 1.0, box=(595, 280, 1323, 800)),
                   S('s_lev_strike4@4.0', 'The Leviathan', 1.4, (0.5, 0.45))]),
            (236, [S('s_helix_terminal@1.2', 'Research Terminals', box=(470, 210, 1450, 870)),
                   S('s_altar@4.0', 'Forge the Tidewarden Key', focus=(0.45, 0.45), zoom=1.4),
                   S('s_dome@2.5', 'The Abyssal Sanctum', zoom=1.1),
                   S('s_lev_strike7@4.0', 'Strike when it is stranded', focus=(0.35, 0.6), zoom=1.3)]),
        ],
    }


def build_collages31(names=None):
    for name, rows in collages31().items():
        if names and name not in names:
            continue
        collage31(name, rows())


# ----------------------------------------------------------------------------------------------- main
if __name__ == '__main__':
    os.makedirs(m3.OUT, exist_ok=True)
    what = sys.argv[3:] or ['header', 'video', 'glance', 'flow_start', 'flow_raft', 'collages']
    if 'counts' in what:
        print(counts())
    if 'header' in what:
        header31()
    if 'header33' in what:
        header31('header_33.png', title='BEACH CRAFT 3.3')
    if 'video' in what:
        video_card()
    if 'glance' in what:
        glance()
    if 'glance33' in what:
        glance('glance_33.png', 'NEW IN 3.2 AND 3.3', NEW_33, all_blocks=True)
    if 'flow_start' in what:
        flow_start()
    if 'flow_raft' in what:
        flow_raft()
    names = [w for w in what if w.endswith('.jpg')]
    if 'collages' in what or names:
        build_collages31(names or None)
