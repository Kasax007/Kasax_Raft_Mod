"""Images of the CurseForge description (docs/curseforge/img): header, section strips, block palette, raft still.
  python3 docs/curseforge/make_images.py STILLS_DIR FONT.ttf
STILLS_DIR holds landscape screenshots of the raft (raft.png, optional raft_deco.png) from the film run."""
import os, sys
from PIL import Image, ImageDraw, ImageFont, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
TEX = os.path.join(HERE, '..', '..', 'src', 'main', 'resources', 'assets', 'raft', 'textures')
OUT = os.path.join(HERE, 'img')
STILLS, FONT = sys.argv[1], sys.argv[2]
NAVY, SEA, FOAM, WHITE, SAND = (10, 42, 78), (24, 132, 186), (120, 214, 236), (255, 255, 255), (238, 214, 160)
W = 1200
os.makedirs(OUT, exist_ok=True)


def font(size):
    return ImageFont.truetype(FONT, size)


def tex(path, size):
    im = Image.open(os.path.join(TEX, path + '.png')).convert('RGBA')
    im = im.crop((0, 0, im.width, im.width))            # animated / multi-frame textures: first frame
    return im.resize((size, size), Image.NEAREST)


def outlined(d, xy, text, f, fill=WHITE, stroke=6, stroke_fill=NAVY, anchor='la'):
    d.text((xy[0], xy[1] + stroke // 2 + 3), text, font=f, fill=stroke_fill, stroke_width=stroke, stroke_fill=stroke_fill, anchor=anchor)
    d.text(xy, text, font=f, fill=fill, stroke_width=stroke, stroke_fill=stroke_fill, anchor=anchor)


def rounded(im, r):
    m = Image.new('L', im.size, 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, im.width - 1, im.height - 1], r, fill=255)
    out = im.convert('RGBA')
    out.putalpha(m)
    return out


def waves(im, y0, color, amp=6, period=60, step=16):
    d = ImageDraw.Draw(im)
    import math
    for row, y in enumerate(range(y0, im.height + step, step)):
        pts = [(x, y + amp * math.sin((x + row * 23) / period * 6.283)) for x in range(-10, im.width + 10, 6)]
        d.line(pts, fill=color, width=3)


def cube(top, left, right, a=64):
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


def header():
    still = Image.open(os.path.join(STILLS, 'raft.png')).convert('RGB')
    h = 420
    s = W / still.width
    still = still.resize((W, int(still.height * s)), Image.LANCZOS)
    y = max(0, (still.height - h) // 2 + 40)
    im = still.crop((0, y, W, y + h)).convert('RGBA')
    shade = Image.new('RGBA', (W, h))
    d = ImageDraw.Draw(shade)
    for x in range(W):
        t = max(0.0, 1 - x / 760)
        d.line([(x, 0), (x, h)], fill=NAVY + (int(215 * t ** 1.2),))
    im = Image.alpha_composite(im, shade)
    d = ImageDraw.Draw(im)
    outlined(d, (60, 80), 'BEACH CRAFT', font(124), stroke=9)
    outlined(d, (66, 248), 'Survive the Endless Sea on a Raft', font(46), fill=SAND, stroke=5)
    tag = 'Minecraft 26.3  -  Fabric'
    f = font(30)
    tw = d.textlength(tag, font=f)
    d.rounded_rectangle([66, 338, 66 + tw + 40, 384], 22, fill=SEA + (255,), outline=WHITE + (255,), width=3)
    d.text((86, 344), tag, font=f, fill=WHITE)
    rounded(im, 26).save(os.path.join(OUT, 'header.png'))


def strip(name, title, icon):
    h = 104
    im = Image.new('RGBA', (W, h))
    d = ImageDraw.Draw(im)
    for x in range(W):
        t = x / W
        d.line([(x, 0), (x, h)], fill=(int(NAVY[0] + (SEA[0] - NAVY[0]) * t), int(NAVY[1] + (SEA[1] - NAVY[1]) * t),
                                       int(NAVY[2] + (SEA[2] - NAVY[2]) * t), 255))
    waves(im, 64, FOAM + (70,), amp=5, period=90, step=18)
    ic = icon if isinstance(icon, Image.Image) else tex(icon, 72)
    ic = ic.resize((72, 72), Image.NEAREST) if ic.size != (72, 72) else ic
    glow = Image.new('RGBA', (96, 96), (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse([0, 0, 95, 95], fill=WHITE + (60,))
    im.alpha_composite(glow, (26, 4))
    im.alpha_composite(ic, (38, 16))
    d = ImageDraw.Draw(im)
    outlined(d, (140, 52), title, font(56), stroke=5, anchor='lm')
    rounded(im, 22).save(os.path.join(OUT, f'strip_{name}.png'))


def framed(src, dst, w=W):
    im = Image.open(src).convert('RGB')
    s = (w - 24) / im.width
    im = im.resize((w - 24, int(im.height * s)), Image.LANCZOS)
    fr = Image.new('RGBA', (w, im.height + 24), (0, 0, 0, 0))
    ImageDraw.Draw(fr).rounded_rectangle([0, 0, fr.width - 1, fr.height - 1], 28, fill=WHITE + (255,))
    fr.alpha_composite(rounded(im, 18), (12, 12))
    fr.save(dst)


PALETTE = [  # (label, kind, textures)  kind: cube (top, left, right) or flat (one texture)
    ('Palm Thatch', 'cube', ('block/palm_thatch',) * 3),
    ('Raft Foundation', 'cube', ('block/raft_foundation',) * 3),
    ('Reinforced Foundation', 'cube', ('block/reinforced_foundation',) * 3),
    ('Sail Canvas', 'cube', ('block/sail_canvas',) * 3),
    ('Striped Sail', 'cube', ('block/sail_canvas_red_striped',) * 3),
    ('Scrap Metal Block', 'cube', ('block/scrap_metal_block',) * 3),
    ('Plastic Block', 'cube', ('block/plastic_block',) * 3),
    ('Titanium Grates', 'cube', ('block/titanium_grates',) * 3),
    ('Crop Plot', 'cube', ('block/crop_plot_top', 'block/crop_plot_side', 'block/crop_plot_side')),
    ('Water Purifier', 'cube', ('block/water_purifier_top', 'block/water_purifier_front', 'block/water_purifier_side')),
    ('Simple Grill', 'cube', ('block/simple_grill_top_on', 'block/simple_grill_side', 'block/simple_grill_side')),
    ('Solar Panel', 'cube', ('block/makeshift_solar_panel_top', 'block/makeshift_solar_panel_side', 'block/makeshift_solar_panel_side')),
    ('Ship Wheel', 'flat', 'block/ship_wheel'),
    ('Shark Jaw Trophy', 'flat', 'block/shark_jaw_trophy'),
    ('Raft Flag', 'flat', 'block/raft_flag'),
    ('Rope Ladder', 'flat', 'block/rope_ladder'),
    ('Rope Coil', 'flat', 'block/rope_coil'),
    ('Scarecrow', 'flat', 'item/scarecrow'),
    ('Fresh Water', 'flat', 'item/fresh_water'),
    ('Coconut Milk', 'flat', 'item/coconut_milk'),
    ('Pina Colada', 'flat', 'item/pina_colada'),
    ('Plastic Hook', 'flat', 'item/plastic_hook'),
    ('Receiver', 'flat', 'item/receiver'),
    ('Paddle', 'flat', 'item/paddle'),
]


def palette():
    cols, cw, ch = 6, W // 6, 214
    rows = (len(PALETTE) + cols - 1) // cols
    im = Image.new('RGBA', (W, rows * ch + 30))
    d = ImageDraw.Draw(im)
    for y in range(im.height):
        t = y / im.height
        d.line([(0, y), (W, y)], fill=(int(250 - 14 * t), int(240 - 12 * t), int(214 - 10 * t), 255))
    f = font(24)
    for k, (label, kind, t) in enumerate(PALETTE):
        cx, cy = (k % cols) * cw + cw // 2, (k // cols) * ch + 20
        d.ellipse([cx - 70, cy + 118, cx + 70, cy + 140], fill=(200, 180, 140, 255))
        if kind == 'cube':
            ic = cube(tex(t[0], 64), tex(t[1], 64), tex(t[2], 64), a=64)
        else:
            ic = tex(t, 112)
        im.alpha_composite(ic, (cx - ic.width // 2, cy + 128 - ic.height))
        if d.textlength(label, font=f) > cw - 14 and ' ' in label:      # long names on two lines
            a, b = label.rsplit(' ', 1)
            d.text((cx, cy + 162), a, font=f, fill=NAVY, anchor='mm')
            d.text((cx, cy + 188), b, font=f, fill=NAVY, anchor='mm')
        else:
            d.text((cx, cy + 172), label, font=f, fill=NAVY, anchor='mm')
    rounded(im, 26).save(os.path.join(OUT, 'blocks.png'))


if __name__ == '__main__':
    header()
    for name, title, icon in [('sea', 'THE ENDLESS SEA', 'block/palm_leaves'), ('portal', 'GETTING THERE', 'item/energy_staff'),
                              ('survive', 'SURVIVE', 'item/fresh_water'), ('shark', 'BRUCE THE SHARK', 'block/shark_jaw_trophy'),
                              ('explore', 'EXPLORE', 'item/receiver'), ('power', 'POWER', 'block/makeshift_solar_panel_top'),
                              ('build', 'BUILD YOUR RAFT', 'block/raft_flag')]:
        strip(name, title, icon)
    framed(os.path.join(STILLS, 'raft.png'), os.path.join(OUT, 'raft.png'))
    if os.path.exists(os.path.join(STILLS, 'raft_deco.png')):
        framed(os.path.join(STILLS, 'raft_deco.png'), os.path.join(OUT, 'raft_deco.png'))
    palette()
    print('images ->', OUT)
