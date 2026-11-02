"""BisectHosting partner banner of the CurseForge description (docs/curseforge/img/bisect.png).
  python3 docs/curseforge/make_bisect_banner.py PARTNER_BANNER.png FONT.ttf
PARTNER_BANNER.png is the Quest Icons partner banner (https://i.imgur.com/fRoy5Eh.png, 3208x647): the BisectHosting
logo is cut out of its white left part. FONT.ttf is the title font of make_images.py (Luckiest Guy).
Same width/height ratio as that banner; colours, outline style and textures as in make_images.py."""
import math
import os
import sys
from PIL import Image, ImageDraw, ImageFilter, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
TEX = os.path.join(HERE, '..', '..', 'src', 'main', 'resources', 'assets', 'raft', 'textures')
OUT = os.path.join(HERE, 'img', 'bisect.png')
PARTNER, FONT = sys.argv[1], sys.argv[2]

NAVY, SEA, FOAM, WHITE, SAND = (10, 42, 78), (24, 132, 186), (120, 214, 236), (255, 255, 255), (238, 214, 160)
WET_SAND, SHALLOW = (214, 186, 128), (46, 170, 200)
W, H = 1600, 323                       # 3208 x 647 scaled to 1600 wide
LOGO_BOX = (120, 228, 1072, 558)       # white area around the logo in the partner banner (no purple shapes inside)
SS = 2                                 # supersampling of the drawn shapes


def font(size):
    return ImageFont.truetype(FONT, size)


def tex(path, size):
    im = Image.open(os.path.join(TEX, path + '.png')).convert('RGBA')
    im = im.crop((0, 0, im.width, im.width))            # animated textures: first frame
    return im.resize((size, size), Image.NEAREST)


def shore(y):
    """x of the water line at height y: a gently curved beach between the white part and the sea."""
    t = y / H
    return 640 + 34 * math.sin(6.283 * 0.85 * t + 0.9) + 10 * math.sin(6.283 * 2.2 * t)


def outlined(d, xy, text, f, fill=WHITE, stroke=5, stroke_fill=NAVY, anchor='la'):
    d.text((xy[0], xy[1] + stroke // 2 + 3), text, font=f, fill=stroke_fill, stroke_width=stroke, stroke_fill=stroke_fill, anchor=anchor)
    d.text(xy, text, font=f, fill=fill, stroke_width=stroke, stroke_fill=stroke_fill, anchor=anchor)


def background():
    """White left part, sand, foam and a sea that gets deeper to the right (drawn at SS x size)."""
    w, h = W * SS, H * SS
    im = Image.new('RGBA', (w, h), WHITE + (255,))
    d = ImageDraw.Draw(im)
    # sea: shallow turquoise at the beach, deep navy at the right edge
    x0 = int(min(shore(y) for y in range(H)) * SS) - 40
    for x in range(x0, w):
        t = min(1.0, max(0.0, (x - x0) / (w - x0)))
        t = t ** 0.7
        c = tuple(int(SHALLOW[i] + (NAVY[i] - SHALLOW[i]) * t) for i in range(3))
        d.line([(x, 0), (x, h)], fill=c + (255,))
    # waves like on the section strips
    for row, yy in enumerate(range(14, H + 18, 18)):
        pts = [(x * SS, (yy + 5 * math.sin((x + row * 23) / 90 * 6.283)) * SS) for x in range(0, W + 6, 6)]
        d.line(pts, fill=FOAM + (46,), width=3 * SS)
    # land: white up to the sand, the sand band, wet sand and a foam line at the water
    ys = list(range(-2, H + 3))
    def band(offset):
        return [(int((shore(y) + offset) * SS), y * SS) for y in ys]
    land = [(0, -2 * SS)] + band(0) + [(0, (H + 2) * SS)]
    d.polygon(land, fill=WET_SAND + (255,))
    d.polygon([(0, -2 * SS)] + band(-14) + [(0, (H + 2) * SS)], fill=SAND + (255,))
    d.polygon([(0, -2 * SS)] + band(-46) + [(0, (H + 2) * SS)], fill=WHITE + (255,))
    d.line(band(2), fill=WHITE + (230,), width=5 * SS)
    d.line(band(12), fill=FOAM + (200,), width=3 * SS)
    return im.resize((W, H), Image.LANCZOS)


def logo():
    src = Image.open(PARTNER).convert('RGBA').crop(LOGO_BOX)
    s = W / 3208                                         # the logo keeps its size relative to the banner
    lg = src.resize((round(src.width * s), round(src.height * s)), Image.LANCZOS)
    return lg.filter(ImageFilter.UnsharpMask(radius=1.2, percent=60, threshold=2))


def icons(im):
    """A few Beach Craft items washed up along the beach, like the items on the partner banner."""
    items = [('item/coconut', 58, 0.06), ('item/pina_colada', 58, 0.29), ('item/fresh_water', 58, 0.52),
             ('item/receiver', 58, 0.75)]
    for k, (path, size, t) in enumerate(items):
        y = int(t * H)
        x = int(shore(y + size // 2)) - size // 2 + (10 if k % 2 else -10)
        ic = tex(path, size)
        shadow = Image.new('RGBA', ic.size, NAVY + (0,))
        shadow.putalpha(ic.split()[3].point(lambda a: int(a * 0.35)))
        im.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(3)), (x + 4, y + 5))
        im.alpha_composite(ic, (x, y))


def text(im):
    d = ImageDraw.Draw(im)
    left = int(max(shore(y) for y in range(H))) + 76      # right of the beach and the icons
    right = W - 64
    cx, avail = (left + right) // 2, right - left

    def fit(line, size):
        while d.textlength(line, font=font(size)) > avail and size > 20:
            size -= 2
        return font(size)

    f1 = fit('THE HOST W/ THE MOST', 74)
    outlined(d, (cx, 132), 'THE HOST W/ THE MOST', f1, fill=WHITE, stroke=6, anchor='ms')
    a, b = 'FOR 25% OFF USE CODE: ', 'KASAX007'
    f2 = fit(a + b + '   ', 52)
    wa, wb = d.textlength(a, font=f2), d.textlength(b, font=f2)
    pad = 18
    x = cx - (wa + wb + 2 * pad) / 2
    base = 232
    outlined(d, (x, base), a, f2, fill=SAND, stroke=5, anchor='ls')
    # the code sits on a tag like "Minecraft 26.3 - Fabric" on the header
    top, bot = base - f2.size * 0.86 - 10, base + 14
    d.rounded_rectangle([x + wa - 4, top, x + wa + wb + 2 * pad, bot], 22, fill=SAND + (255,), outline=WHITE + (255,), width=4)
    d.text((x + wa + pad - 2, base), b, font=f2, fill=NAVY, anchor='ls')


def rounded(im, r):
    m = Image.new('L', (im.width * SS, im.height * SS), 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, m.width - 1, m.height - 1], r * SS, fill=255)
    out = im.convert('RGBA')
    out.putalpha(m.resize(im.size, Image.LANCZOS))
    return out


def border(im, r):
    """A thin light frame, so the white part still reads as a card on CurseForge's white page."""
    ov = Image.new('RGBA', (im.width * SS, im.height * SS), (0, 0, 0, 0))
    ImageDraw.Draw(ov).rounded_rectangle([1, 1, ov.width - 2, ov.height - 2], r * SS, outline=(205, 214, 224, 255), width=2 * SS)
    im.alpha_composite(ov.resize(im.size, Image.LANCZOS))


if __name__ == '__main__':
    im = background()
    lg = logo()
    white_right = int(min(shore(y) for y in range(H))) - 46
    im.alpha_composite(lg, ((white_right - lg.width) // 2 + 6, (H - lg.height) // 2))
    icons(im)
    text(im)
    im = rounded(im, 26)
    border(im, 26)
    im.save(OUT)
    print('banner ->', OUT, im.size)
