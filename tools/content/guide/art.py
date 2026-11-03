"""Pixel art of the guide module: the Seafarer's Almanac item and its GUI (an oiled leather logbook).

gui/almanac.png (512x256):
  (0,0)    320x200  the open book: sea-green oilskin cover with stitching and brass corners, two ruled pages
  (320,0)  18x10    next arrow, (338,0) hovered
  (320,10) 18x10    previous arrow, (338,10) hovered
  (360,0)  12x26    ribbon bookmark (contents), (372,0) hovered
  (384,0)  18x18    icon slot
  (320,30) 128x5    divider under titles
  (320,40) 128x118  illustration plate (creature portraits)
  (450,0)  26x26    red wax seal (spoiler warning)
  (480,0)  31x31    faded compass rose (empty pages)
Coordinates are mirrored in AlmanacScreen.java.
"""
import math
import random

from PIL import Image

from bclib import tex

# ramps, dark -> light
LEATHER = tex.ramp('#335b55', 6, spread=0.42, hue_shift=0.03)       # oiled sea-green leather
PAPER = tex.ramp('#eadfc2', 5, spread=0.16, hue_shift=0.02)          # warm logbook paper
BRASS = tex.ramp('#c39a45', 5, spread=0.5)
RIBBON = tex.ramp('#a8322c', 5, spread=0.5)
WAX = tex.ramp('#9a2a24', 5, spread=0.5)
INK = (59, 42, 28, 255)
TEAL_INK = (31, 92, 88, 255)
THREAD = (205, 186, 132, 255)
SALT = (196, 214, 200, 255)

W, H = 320, 200
LEFT = (10, 7, 157, 189)        # page boxes (inclusive)
RIGHT = (162, 7, 309, 189)
TEXT_X = (22, 170)              # text columns (128 wide)
ROW0, ROWH, ROWS = 16, 10, 16   # text grid: rows at y = 16 + 10k


def _blend(c, over, a):
    return tuple(round(c[i] * (1 - a) + over[i] * a) for i in range(3)) + (255,)


def _put_blend(img, x, y, over, a):
    if 0 <= x < img.width and 0 <= y < img.height:
        c = img.getpixel((x, y))
        if c[3] > 0:
            img.putpixel((x, y), _blend(c, over, a))


def cover(img):
    rnd = random.Random(31)
    n = tex.value_noise(W, H, 7, cell=10, octaves=3, tile=False)
    for y in range(H):
        for x in range(W):
            v = n[y][x]
            # worn, lighter leather towards the edges (weathered), darker in the middle
            edge = min(x, y, W - 1 - x, H - 1 - y)
            wear = max(0.0, 1.0 - edge / 9.0) * 0.18
            k = 1 + int(min(0.999, max(0.0, (v - 0.5) * 1.3 + 0.5 + wear)) * 3)   # shades 1..3
            img.putpixel((x, y), LEATHER[k])
    # salt crust along the bottom edge: soft pale blotches
    m = tex.value_noise(W, 24, 12, cell=6, octaves=2, tile=False)
    for y in range(H - 14, H - 2):
        for x in range(2, W - 2):
            t = m[y - (H - 24)][x]
            if t > 0.62:
                _put_blend(img, x, y, SALT, 0.18 + (t - 0.62) * 0.5)
    # bevel: dark outline, highlight top/left, shadow bottom/right
    for x in range(W):
        tex.px(img, x, 0, LEATHER[0]); tex.px(img, x, H - 1, LEATHER[0])
        tex.px(img, x, 1, LEATHER[4]); tex.px(img, x, H - 2, LEATHER[0])
    for y in range(H):
        tex.px(img, 0, y, LEATHER[0]); tex.px(img, W - 1, y, LEATHER[0])
        tex.px(img, 1, y, LEATHER[4]); tex.px(img, W - 2, y, LEATHER[0])
    for (x, y) in ((0, 0), (1, 0), (0, 1), (W - 1, 0), (W - 2, 0), (W - 1, 1), (0, H - 1), (1, H - 1), (0, H - 2),
                   (W - 1, H - 1), (W - 2, H - 1), (W - 1, H - 2)):
        img.putpixel((x, y), (0, 0, 0, 0))
    # stitching 4 px inside the edge: 3 on, 2 off, with a pressed groove under every stitch
    def stitch(points):
        for i, (x, y, dx, dy) in enumerate(points):
            if i % 5 < 3:
                tex.px(img, x, y, THREAD)
                tex.px(img, x + dx, y + dy, LEATHER[0])
    s = 4
    stitch([(x, s, 0, 1) for x in range(s, W - s)])
    stitch([(x, H - 1 - s, 0, 1) for x in range(s, W - s)])
    stitch([(s, y, 1, 0) for y in range(s + 1, H - s - 1)])
    stitch([(W - 1 - s, y, 1, 0) for y in range(s + 1, H - s - 1)])


def page(img, box, spine_right):
    x0, y0, x1, y1 = box
    n = tex.value_noise(W, H, 5 if spine_right else 6, cell=12, octaves=2, tile=False)
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            v = n[y][x]
            c = PAPER[4]
            if v < 0.36:                          # soft mottling, no hard blotches
                c = _blend(PAPER[4], PAPER[3], 0.45)
            elif v > 0.72:
                c = _blend(PAPER[4], (255, 250, 236), 0.35)
            d_spine = (x1 - x) if spine_right else (x - x0)
            if d_spine < 10:                     # gutter shading towards the spine
                c = PAPER[2] if d_spine < 3 else PAPER[3] if d_spine < 7 else c
                if d_spine < 1:
                    c = PAPER[1]
            d_out = (x - x0) if spine_right else (x1 - x)
            if d_out < 2 or y - y0 < 1 or y1 - y < 2:
                c = PAPER[2]
            img.putpixel((x, y), c)
    # page stack below the page and on the outer side (two thin layers)
    for i, yy in enumerate((y1 + 1, y1 + 2)):
        for x in range(x0 + 1 + i, x1 + 1 - (0 if spine_right else 0)):
            img.putpixel((x, yy), PAPER[1] if i == 0 else PAPER[2])
    ox = (x0 - 1, x0 - 2) if spine_right else (x1 + 1, x1 + 2)
    for i, xx in enumerate(ox):
        for y in range(y0 + 1 + i, y1 + 2):
            img.putpixel((xx, y), PAPER[1] if i == 0 else PAPER[2])
    # ruled lines under every text row and a faint red margin line
    tx = TEXT_X[0] if spine_right else TEXT_X[1]
    for k in range(ROWS):
        y = ROW0 + ROWH * k + 9
        for x in range(tx - 3, tx + 128 + 2):
            _put_blend(img, x, y, (96, 132, 150), 0.16)
    for y in range(y0 + 3, y1 - 2):
        _put_blend(img, tx - 5, y, (176, 70, 60), 0.14)


def stain(img, cx, cy, rx, ry, seed):
    """A dried water ring: darker rim, a hint of tint inside."""
    rnd = random.Random(seed)
    for a in range(0, 360, 1):
        r = 1.0 + 0.04 * math.sin(math.radians(a * 3 + seed)) + 0.02 * math.sin(math.radians(a * 7))
        x = round(cx + math.cos(math.radians(a)) * rx * r)
        y = round(cy + math.sin(math.radians(a)) * ry * r)
        _put_blend(img, x, y, (160, 130, 80), 0.06)
    for y in range(cy - ry, cy + ry + 1):
        for x in range(cx - rx, cx + rx + 1):
            if ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 < 0.85 and rnd.random() < 0.9:
                _put_blend(img, x, y, (170, 140, 90), 0.04)


def brass_corner(img, cx, cy, fx, fy):
    """Brass corner cap; (cx, cy) the outer corner, fx/fy = +1/-1 direction into the book."""
    L = 15
    for i in range(L):
        for j in range(L - i):
            x, y = cx + fx * i, cy + fy * j
            edge = (i + j == L - 1) or i == 0 or j == 0
            shade = BRASS[3] if (i + j) < 6 else BRASS[2]
            if i + j < 3:
                shade = BRASS[4]
            tex.px(img, x, y, BRASS[0] if edge and (i + j == L - 1) else shade)
    # outer rim highlight and a rivet
    for i in range(L - 1):
        tex.px(img, cx + fx * i, cy, BRASS[1] if fy < 0 else BRASS[4])
        tex.px(img, cx, cy + fy * i, BRASS[1] if fx < 0 else BRASS[4])
    rx, ry = cx + fx * 4, cy + fy * 4
    tex.px(img, rx, ry, BRASS[4]); tex.px(img, rx + fx, ry, BRASS[2])
    tex.px(img, rx, ry + fy, BRASS[2]); tex.px(img, rx + fx, ry + fy, BRASS[0])
    for (x, y) in ((cx, cy),):
        tex.px(img, x, y, (0, 0, 0, 0))


def book():
    img = tex.new(W, H)
    cover(img)
    # spine groove between the pages
    for y in range(4, H - 4):
        for x in range(157, 163):
            img.putpixel((x, y), LEATHER[0] if 159 <= x <= 160 else LEATHER[1])
    page(img, LEFT, True)
    page(img, RIGHT, False)
    stain(img, 132, 168, 14, 10, 3)
    stain(img, 290, 150, 16, 12, 9)
    brass_corner(img, 0, 0, 1, 1)
    brass_corner(img, W - 1, 0, -1, 1)
    brass_corner(img, 0, H - 1, 1, -1)
    brass_corner(img, W - 1, H - 1, -1, -1)
    return img


def arrow(hover, back):
    im = tex.new(18, 10)
    body = (46, 120, 112, 255) if hover else TEAL_INK
    light = (120, 200, 186, 255) if hover else (70, 140, 130, 255)
    # shaft x 1..10 rows 4..5, head x 9..16 rows 0..9 (a triangle)
    for x in range(1, 11):
        for y in (4, 5):
            im.putpixel((x, y), body)
    for x in range(9, 17):
        half = 16 - x
        for y in range(4 - half // 2 - (1 if half > 5 else 0), 6 + half // 2 + (1 if half > 5 else 0)):
            if 0 <= y < 10:
                im.putpixel((x, y), body)
    for x in range(1, 10):
        im.putpixel((x, 4), light)
    tex.outline(im, (28, 44, 40, 255) if not hover else (20, 60, 56, 255))
    if back:
        im = im.transpose(Image.FLIP_LEFT_RIGHT)
    return im


def ribbon(hover):
    im = tex.new(12, 26)
    r = RIBBON if not hover else [tex.mix(c, (255, 255, 255, 255), 0.18) for c in RIBBON]
    for y in range(26):
        for x in range(1, 11):
            notch = y >= 21 and abs(x - 5.5) < (y - 20)
            if notch:
                continue
            c = r[3] if x < 4 else r[2] if x < 8 else r[1]
            if x == 1:
                c = r[4]
            if y % 6 == 2 and 3 < x < 9:                    # woven ridges
                c = r[2]
            im.putpixel((x, y), c)
    tex.outline(im, tex.darker(RIBBON[0], 0.6))
    return im


def divider():
    im = tex.new(128, 5)
    for x in range(4, 124):
        d = min(x - 4, 123 - x)
        a = min(1.0, d / 24.0)
        if a > 0.08:
            im.putpixel((x, 2), (59, 42, 28, round(200 * a)))
    for dx, dy in ((0, 0), (-1, 1), (1, 1), (0, 2), (-1, 3), (1, 3), (0, 4), (-2, 2), (2, 2)):
        im.putpixel((63 + dx, dy), (31, 92, 88, 255))
    im.putpixel((63, 2), (190, 150, 70, 255))
    return im


def slot():
    im = tex.new(18, 18)
    for y in range(18):
        for x in range(18):
            c = PAPER[2]
            if x == 0 or y == 0:
                c = PAPER[1]
            elif x == 17 or y == 17:
                c = PAPER[4]
            im.putpixel((x, y), c)
    return im


def plate():
    """Illustration plate for a creature: a watercolour sea under a pale sky, a sandy floor, an ink frame."""
    w, h = 128, 118
    im = tex.new(w, h)
    sky_top, sky_low = (238, 234, 216), (222, 232, 226)
    sea_top, sea_low = (196, 222, 222), (150, 192, 200)
    hy = 46
    n = tex.value_noise(w, h, 21, cell=8, octaves=2, tile=False)
    for y in range(h):
        for x in range(w):
            if y < hy:
                t = y / hy
                c = tex.mix(sky_top + (255,), sky_low + (255,), t)
            else:
                t = (y - hy) / (h - hy)
                c = tex.mix(sea_top + (255,), sea_low + (255,), t)
                if n[y][x] > 0.66:                      # soft watercolour patches
                    c = _blend(c, (238, 246, 240), 0.18)
            im.putpixel((x, y), c)
    for x in range(1, w - 1):                           # little waves on the horizon
        y = hy + round(math.sin(x / 4.0) * 0.9)
        _put_blend(im, x, y, (90, 140, 150), 0.45)
        _put_blend(im, x, y - 1, (255, 255, 255), 0.3)
    m = tex.value_noise(w, 16, 22, cell=6, octaves=2, tile=False)
    for x in range(1, w - 1):                           # sandy floor
        top = h - 12 + round(math.sin(x / 9.0) * 1.5 + m[0][x] * 2)
        for y in range(top, h - 1):
            c = (214, 196, 150) if y > top else (230, 214, 170)
            if (x * 7 + y * 13) % 29 == 0:
                c = (190, 168, 120)
            im.putpixel((x, y), c + (255,))
    for x in range(w):
        im.putpixel((x, 0), INK); im.putpixel((x, h - 1), INK)
        _put_blend(im, x, 1, (255, 255, 255), 0.4); _put_blend(im, x, h - 2, (60, 80, 80), 0.25)
    for y in range(h):
        im.putpixel((0, y), INK); im.putpixel((w - 1, y), INK)
    for (cx, cy, fx, fy) in ((2, 2, 1, 1), (w - 3, 2, -1, 1), (2, h - 3, 1, -1), (w - 3, h - 3, -1, -1)):
        for i in range(5):
            im.putpixel((cx + fx * i, cy), TEAL_INK)
            im.putpixel((cx, cy + fy * i), TEAL_INK)
    return im


def seal():
    im = tex.new(26, 26)
    rnd = random.Random(2)
    for y in range(26):
        for x in range(26):
            d = math.hypot(x - 12.5, y - 12.5) + 0.9 * math.sin(math.atan2(y - 12.5, x - 12.5) * 7)
            if d < 12:
                c = WAX[2]
                if d > 10:
                    c = WAX[1]
                if (x - 12.5) + (y - 12.5) < -9 and d < 10:
                    c = WAX[3]
                im.putpixel((x, y), c)
    # inner ring and a little trident
    for a in range(0, 360, 4):
        x = round(12.5 + math.cos(math.radians(a)) * 7.5)
        y = round(12.5 + math.sin(math.radians(a)) * 7.5)
        im.putpixel((x, y), WAX[1])
    for y in range(8, 19):
        im.putpixel((12, y), WAX[4]); im.putpixel((13, y), WAX[3])
    for x in (9, 16):
        for y in range(8, 12):
            im.putpixel((x, y), WAX[4])
    for x in range(9, 17):
        im.putpixel((x, 12), WAX[4])
    for x in (9, 12, 16):
        im.putpixel((x, 7), WAX[4])
    tex.outline(im, tex.darker(WAX[0], 0.7))
    return im


def compass():
    im = tex.new(31, 31)
    c = (59, 42, 28)
    def p(x, y, a):
        if 0 <= x < 31 and 0 <= y < 31:
            im.putpixel((x, y), c + (a,))
    for a in range(0, 360, 2):
        p(round(15 + math.cos(math.radians(a)) * 11), round(15 + math.sin(math.radians(a)) * 11), 60)
    for i in range(1, 15):
        w = max(0, 3 - i // 4)
        for d in range(-w, w + 1):
            p(15 + d, 15 - i, 70); p(15 + d, 15 + i, 50); p(15 - i, 15 + d, 50); p(15 + i, 15 + d, 50)
    for i in range(1, 8):
        p(15 + i, 15 - i, 40); p(15 - i, 15 - i, 40); p(15 + i, 15 + i, 40); p(15 - i, 15 + i, 40)
    p(15, 15, 90)
    return im


def gui():
    sheet = tex.new(512, 256)
    sheet.alpha_composite(book(), (0, 0))
    sheet.alpha_composite(arrow(False, False), (320, 0))
    sheet.alpha_composite(arrow(True, False), (338, 0))
    sheet.alpha_composite(arrow(False, True), (320, 10))
    sheet.alpha_composite(arrow(True, True), (338, 10))
    sheet.alpha_composite(ribbon(False), (360, 0))
    sheet.alpha_composite(ribbon(True), (372, 0))
    sheet.alpha_composite(slot(), (384, 0))
    sheet.alpha_composite(divider(), (320, 30))
    sheet.alpha_composite(plate(), (320, 40))
    sheet.alpha_composite(seal(), (450, 0))
    sheet.alpha_composite(compass(), (480, 0))
    return sheet


# ----------------------------------------------------------------------------------------------- item
def item():
    """The almanac item: the vanilla book silhouette re-bound in sea-green oilskin, with cream pages, embossed brass
    lines on the cover (where the vanilla book has its title lines) and a red ribbon."""
    src = tex.load('minecraft:item/book')
    im = tex.new(16, 16)
    cover = {(49, 33, 4): LEATHER[0], (82, 46, 16): LEATHER[2], (101, 75, 23): LEATHER[3], (84, 62, 19): LEATHER[2],
             (68, 37, 10): None, (22, 16, 5): tex.darker(LEATHER[0], 0.55)}
    for y in range(16):
        for x in range(16):
            p = src.getpixel((x, y))
            if p[3] == 0:
                continue
            k = p[:3]
            if p[0] == p[1] == p[2]:                          # pages
                v = p[0]
                c = PAPER[4] if v > 200 else PAPER[3] if v > 170 else PAPER[1] if v > 120 else tex.darker(PAPER[0], 0.7)
            elif k == (68, 37, 10):                           # title lines / spine shadow
                c = BRASS[3] if y < 9 and 3 < x < 14 else LEATHER[1]
            else:
                c = cover.get(k, LEATHER[2])
            im.putpixel((x, y), c)
    # worn highlight along the top edge of the cover
    for (x, y) in ((8, 2), (6, 3), (4, 4), (2, 5)):
        if im.getpixel((x, y))[3]:
            im.putpixel((x, y), LEATHER[4])
    # red ribbon hanging out of the pages at the bottom
    for (x, y, k) in ((9, 13, 3), (9, 14, 2), (10, 14, 1)):
        im.putpixel((x, y), RIBBON[k])
    return im
