"""Pixel art of the cozy module: 16x16, light from the top left, 4-6 tone ramps, alpha 0 or 255 only."""
import math
import random

from PIL import Image

from bclib import tex
from content.decor import art
from content.decor.art import P, img, pal

M = 'cozy'

# palettes (dark -> light)
TERRACOTTA = pal('#5e2a1a', '#7d3a22', '#9c4d2c', '#b8653a', '#cf8250', '#e2a274')
CERAMIC = pal('#8b9096', '#aeb3b6', '#c9cdcd', '#dee0dc', '#eeefea', '#fbfbf6')
GLAZE_BLUE = pal('#1f3f6e', '#2b5590', '#3d70b0', '#5b8fcc', '#86b2e0')
SHELL_TILE = pal('#9d7b66', '#bb9a83', '#d3b8a0', '#e6d0b9', '#f3e4d1', '#fbf3e8')
PUMICE = pal('#59544d', '#6f6a62', '#878178', '#a09a90', '#b8b2a8', '#cdc8bf')
MONSTERA = pal('#173a1f', '#215029', '#2d6834', '#3c8341', '#559e52', '#77b86a')
CANDLE = pal('#b89b72', '#d4bb92', '#e8d6b3', '#f5ead2', '#fdf8ec')
FLAME = pal('#c8541c', '#ec8a24', '#fbc23e', '#fff0a0')
STEEL = art.IRON
DRUM = pal('#2a4a6e', '#356090', '#4479ad', '#5f95c7', '#86b4dc', '#b6d6ee')
CORAL_PINK = pal('#7a2238', '#a13049', '#c8475d', '#e2687a', '#f293a0')
FISH = pal('#1b4f6a', '#24698a', '#2f86a8', '#4aa3c2', '#7cc3d9', '#b9e3ee')
SAND = pal('#a88a5a', '#c4a672', '#d8bf8c', '#e7d3a6', '#f2e4c2')
NAVY = pal('#16223b', '#1f2f52', '#2a3e6b', '#3a5286')


def save(im, name):
    return tex.save(im, M, name)


def solid(c):
    im = img()
    for y in range(16):
        for x in range(16):
            im.putpixel((x, y), tex.rgb(c))
    return im


def flat(shades, k=None):
    return solid(shades[len(shades) // 2 if k is None else k])


# ------------------------------------------------------------------------------------------------- building
def shell_tiles():
    """Square tiles of crushed shell with pale grout, each tile lit from the top left."""
    s = SHELL_TILE
    im = img()
    for y in range(16):
        for x in range(16):
            tx, ty = x % 8, y % 8
            if tx == 7 or ty == 7:
                c = s[1]
            elif tx == 0 or ty == 0:
                c = s[5]
            elif tx == 6 or ty == 6:
                c = s[2]
            else:
                c = s[3] if (x // 8 + y // 8) % 2 == 0 else s[4]
            im.putpixel((x, y), c)
    # a few shell fragments, the same in every tile so the pattern stays calm
    for bx, by in ((0, 0), (8, 0), (0, 8), (8, 8)):
        for dx, dy, k in ((3, 3, 5), (4, 3, 2), (2, 4, 2)):
            P(im, bx + dx, by + dy, s[k])
    return im


def pumice_bricks():
    return tex.bricks(PUMICE[1:], PUMICE[0], seed=4, rows=4, cols=2, bevel=True)


def lattice():
    """Driftwood lattice for windows: diagonal slats on a transparent ground (pane texture)."""
    d = art.DRIFT
    im = img()
    for y in range(16):
        for x in range(16):
            a = (x + y) % 8
            b = (x - y) % 8
            if a in (0, 1) or b in (0, 1):
                k = 4 if a == 0 or b == 0 else 2
                im.putpixel((x, y), d[k])
    return im


def lattice_edge():
    return flat(art.DRIFT, 3)


# ------------------------------------------------------------------------------------------------- cloth & rugs
def towel(c1, c2):
    """Beach towel: wide stripes along the block, a pale fringe at the top and bottom edge."""
    r1 = tex.ramp(c1, 4, 0.3)
    r2 = tex.ramp(c2, 4, 0.3)
    im = img()
    for y in range(16):
        for x in range(16):
            stripe = (x // 4) % 2
            r = r1 if stripe == 0 else r2
            k = 2 if (x + y) % 5 else 1
            if x % 4 == 0:
                k = 3
            im.putpixel((x, y), r[k])
    return im


def rope_rug():
    """Round coiled rope mat seen from above: rings of twisted rope (each ring lit on its upper-left side), a dark gap
    between the rings; the corners are transparent and show the floor (cutout)."""
    r = art.ROPE
    im = img()
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - 8, y + 0.5 - 8
            d = math.hypot(dx, dy)
            if d > 8.0:
                continue
            ring = d / 1.75                     # one ring every 1.75 px
            f = ring - int(ring)
            ang = math.atan2(dy, dx)
            twist = (int(ang * 8 / math.pi + ring * 3)) % 2   # the twist of the rope strands
            if f < 0.22:
                k = 1                            # the groove between two rings
            else:
                k = 3 + twist
                if dx + dy < -2 and f > 0.5:
                    k += 1                       # light from the top left
                elif dx + dy > 3 and f < 0.55:
                    k -= 1
            if d > 7.3:
                k = min(k, 2)                    # the outer edge of the mat
            im.putpixel((x, y), r[max(0, min(5, k))])
    return im


def cushion_cloth(c):
    return art.cloth(tex.ramp(c, 5, 0.35), seed=7)


def stripes_cloth(c1, c2):
    return art.stripes(c1, c2, width=2, vertical=True, shade=False)


# ------------------------------------------------------------------------------------------------- wicker, ceramic
def wicker():
    w = art.WICKER
    im = img()
    for y in range(16):
        for x in range(16):
            k = 3 if ((x // 2) + (y // 2)) % 2 else 2
            if (x + y) % 4 == 0:
                k -= 1
            if y % 4 == 0:
                k = 4 if x % 2 == 0 else 3
            im.putpixel((x, y), w[k])
    return im


def plate_top():
    """Glazed plate seen from above: white glaze, a blue rim band and a soft highlight."""
    c, b = CERAMIC, GLAZE_BLUE
    im = img()
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5))
            if d > 6.5:
                k = b[2] if (x + y) % 2 else b[3]
                im.putpixel((x, y), k)
            elif d > 5.5:
                im.putpixel((x, y), c[2])
            else:
                im.putpixel((x, y), c[4] if (x < 6 and y < 6) else c[3])
    return im


def ceramic():
    return flat(CERAMIC, 3)


def terracotta():
    t = TERRACOTTA
    im = flat(t, 3)
    for x in range(16):
        P(im, x, 0, t[5])
        P(im, x, 1, t[4])
        P(im, x, 15, t[1])
    for y in range(4, 14, 4):
        for x in range(16):
            if (x + y) % 3:
                P(im, x, y, t[2])
    return im


def soil():
    return tex.noise_fill(img(), pal('#3b2617', '#4a3020', '#5b3c28', '#6b4931'), 11, cell=2, octaves=1, contrast=0.8)


# ------------------------------------------------------------------------------------------------- plants
def monstera():
    """Big split leaves of a monstera, cross texture (transparent background)."""
    g = MONSTERA
    im = img()
    for cx, cy, r in ((5, 5, 4.6), (11, 7, 4.3), (7, 11, 4.0)):
        for y in range(16):
            for x in range(16):
                dx, dy = x - cx, y - cy
                d = math.hypot(dx, dy * 1.2)
                if d <= r:
                    ang = math.atan2(dy, dx)
                    # the typical splits in the leaf
                    if d > r * 0.45 and int((ang + math.pi) * 5) % 3 == 0:
                        continue
                    k = 4 if dx < 0 and dy < 0 else 3 if d < r * 0.6 else 2
                    im.putpixel((x, y), g[k])
        P(im, cx, cy, g[5])
    for y in range(10, 16):
        P(im, 7 + (1 if y > 12 else 0), y, g[1])
        P(im, 8, y, g[2])
    return im


def flowers():
    """Small tropical flowers on stems (cross texture)."""
    im = img()
    g = MONSTERA
    rnd = random.Random(5)
    heads = [(3, 4, '#e04b5a'), (8, 2, '#f2c94c'), (12, 5, '#f07ab0'), (6, 8, '#ffffff'), (11, 9, '#e8813a')]
    for x, y, c in heads:
        for k in range(y + 2, 16):
            P(im, x, k, g[2] if k % 3 else g[3])
        pet = tex.ramp(c, 4, 0.3)
        for dx, dy, kk in ((0, -1, 3), (-1, 0, 2), (1, 0, 2), (0, 1, 1)):
            P(im, x + dx, y + dy, pet[kk])
        P(im, x, y, tex.rgb('#f8e27a'))
        lx = x + rnd.choice((-1, 1))
        P(im, lx, y + 4, g[4])
    return im


def coral_branches():
    c = CORAL_PINK
    im = img()

    def branch(x, y, dx, n):
        for i in range(n):
            P(im, x, y, c[3 if i % 2 else 2])
            P(im, x + 1, y, c[1])
            y -= 1
            if i % 3 == 2:
                x += dx
        P(im, x, y, c[4])
    branch(7, 15, 0, 13)
    branch(7, 10, -1, 7)
    branch(8, 8, 1, 6)
    branch(4, 9, 0, 4)
    branch(11, 6, 0, 3)
    return im


def palm_fronds():
    """Thatch of palm leaves for the parasol canopy (top view, opaque)."""
    g = pal('#5a5a24', '#787630', '#96933e', '#b2ac52', '#cbc46c', '#ded88e')
    im = img()
    for y in range(16):
        for x in range(16):
            k = 2 + ((x * 3 + y) // 3) % 3
            if (x - y) % 4 == 0:
                k = 1
            im.putpixel((x, y), g[min(5, k)])
    return im


def palm_fringe():
    """Hanging leaf tips under the canopy edge (plane, cutout)."""
    g = pal('#5a5a24', '#787630', '#96933e', '#b2ac52', '#cbc46c')
    im = img()
    for x in range(16):
        n = 3 + (x * 7) % 4
        for y in range(n):
            im.putpixel((x, y), g[3 - min(2, y // 2)])
    return im


# ------------------------------------------------------------------------------------------------- lights
def glass_pane_tex(c='#cfe7e3'):
    r = tex.ramp(c, 4, 0.25)
    im = img()
    for y in range(16):
        for x in range(16):
            im.putpixel((x, y), r[2])
    for k in range(3, 9):
        P(im, k - 1, 14 - k, r[3])
    return im


def lantern_glass(lit):
    """Glass of the hurricane lantern with the flame inside (lit) or a dark wick (unlit)."""
    g = pal('#6d7d78', '#8ea19b', '#b2c4bd', '#d6e4dd') if not lit else pal('#c98b3a', '#e7ad55', '#f6d27e', '#fff0bf')
    im = img()
    for y in range(16):
        for x in range(16):
            im.putpixel((x, y), g[1])
    for y in range(16):
        for x in range(16):
            d = math.hypot((x - 7.5) * 1.6, y - 9)
            if lit and d < 4.0:
                im.putpixel((x, y), g[3] if d < 2 else g[2])
    for y in range(2, 14):
        P(im, 3, y, g[3])
    if not lit:
        for y in range(9, 13):
            P(im, 7, y, tex.rgb('#3a3027'))
            P(im, 8, y, tex.rgb('#4a3d30'))
    return im


def flame():
    f = FLAME
    im = img()
    for y in range(16):
        for x in range(16):
            dx = abs(x - 7.5)
            top = 4 + dx * 2.2
            if y >= top and y < 15:
                k = 3 if dx < 1.2 and y > 8 else 2 if dx < 2.2 else 1
                if y > 12:
                    k = max(0, k - 1)
                im.putpixel((x, y), f[k])
    return im


def wax(c='#f5ead2'):
    return flat(tex.ramp(c, 4, 0.2), 2)


def sea_glass_shade(lit):
    g = art.SEA_GLASS if lit else pal('#2a5552', '#376b67', '#47837d', '#5a9a92', '#77b2a9', '#9ccac0')
    im = img()
    for y in range(16):
        for x in range(16):
            k = 3 if ((x // 3) + (y // 4)) % 2 else 2
            if lit and 5 <= y <= 11:
                k += 1
            if x in (0, 15):
                k -= 1
            im.putpixel((x, y), g[max(0, min(5, k))])
    return im


# ------------------------------------------------------------------------------------------------- wall decor
def surfboard(c, stripe='#f6f3ea'):
    """Front of a surfboard: a coloured deck, a pale stringer stripe and a darker rail."""
    r = tex.ramp(c, 5, 0.4)
    s = tex.ramp(stripe, 3, 0.15)
    im = img()
    for y in range(16):
        for x in range(16):
            k = 2
            if x <= 1:
                k = 3
            if x >= 14:
                k = 1
            im.putpixel((x, y), r[k])
        P(im, 7, y, s[1])
        P(im, 8, y, s[2])
    return im


def fish_side():
    """A blue game fish seen from the side (texture for the trophy's fish boxes)."""
    f = FISH
    im = img()
    for y in range(16):
        for x in range(16):
            k = 4 if y < 5 else 3 if y < 9 else 2
            if y > 12:
                k = 1
            im.putpixel((x, y), f[k])
    for x in range(0, 16, 3):
        P(im, x, 6, f[5])
    P(im, 2, 5, tex.rgb('#1c1c22'))
    P(im, 2, 4, tex.rgb('#f2f2f2'))
    return im


def net():
    """Knotted fishing net (cutout) with two round glass floats."""
    r = art.ROPE
    im = img()
    for y in range(16):
        for x in range(16):
            if (x + y) % 5 == 0 or (x - y) % 5 == 0:
                im.putpixel((x, y), r[3] if (x + y) % 2 else r[2])
    for cx, cy, col in ((4, 5, '#4fb3a6'), (11, 10, '#6f9ed8')):
        g = tex.ramp(col, 4, 0.3)
        for y in range(cy - 2, cy + 3):
            for x in range(cx - 2, cx + 3):
                if math.hypot(x - cx, y - cy) <= 2.3:
                    k = 3 if (x < cx and y < cy) else 2 if math.hypot(x - cx, y - cy) < 1.5 else 1
                    im.putpixel((x, y), g[k])
    return im


def shell_garland():
    """String with shells and a starfish (cutout plane)."""
    r = art.ROPE
    s = art.SHELL
    im = img()
    for x in range(16):
        y = int(3 + 3 * math.sin(math.pi * x / 15))
        P(im, x, y, r[3])
    for cx in (2, 6, 10, 14):
        cy = int(3 + 3 * math.sin(math.pi * cx / 15)) + 3
        for y in range(cy - 2, cy + 2):
            for x in range(cx - 2, cx + 2):
                if abs(x - cx + 0.5) + abs(y - cy + 0.5) <= 2.2:
                    im.putpixel((x, y), s[3] if (x + y) % 2 else s[4])
        P(im, cx, cy - 3, r[2])
        P(im, cx - 1, cy + 1, s[1])
    return im


def bamboo_slats(rolled=False):
    b = art.BAMBOO
    im = img()
    for y in range(16):
        for x in range(16):
            k = 3 if y % 2 == 0 else 2
            if x in (3, 12):
                k = 1
            im.putpixel((x, y), b[k])
    return im


def bamboo_roll_end():
    b = art.BAMBOO
    im = img()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            k = 4 if int(d) % 3 == 0 else 2
            im.putpixel((x, y), b[k])
    return im


# ------------------------------------------------------------------------------------------------- music & sea things
def drum_top():
    """Steel pan seen from above: dimpled note areas in a chrome-blue bowl."""
    d = DRUM
    im = img()
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - 7.5, y - 7.5)
            k = 3 if r < 7 else 2
            if r >= 7.3:
                k = 1
            im.putpixel((x, y), d[k])
    notes = [(7.5, 7.5, 2.0)] + [(7.5 + 4.6 * math.cos(a), 7.5 + 4.6 * math.sin(a), 1.4)
                                  for a in [i * math.pi / 3 for i in range(6)]]
    for cx, cy, rr in notes:
        for y in range(16):
            for x in range(16):
                dd = math.hypot(x - cx, y - cy)
                if dd <= rr:
                    im.putpixel((x, y), d[5] if (x < cx and y < cy) else d[4])
                elif dd <= rr + 0.7:
                    im.putpixel((x, y), d[2])
    return im


def drum_side():
    d = DRUM
    im = img()
    for y in range(16):
        for x in range(16):
            k = 3
            if y in (0, 15):
                k = 1
            elif y in (1, 14):
                k = 4
            elif x % 8 == 1:
                k = 5
            im.putpixel((x, y), d[k])
    return im


def conch():
    """Conch shell texture: creamy whorls with a pink lip."""
    s = art.SHELL
    im = img()
    for y in range(16):
        for x in range(16):
            k = 3 + (1 if (x + y // 2) % 4 == 0 else 0) - (1 if y % 5 == 4 else 0)
            im.putpixel((x, y), s[k])
    return im


def conch_lip():
    return flat(CORAL_PINK, 3)


def polished_brass():
    """Polished brass for small fittings: smooth, a bright streak on the left, darker on the right and bottom."""
    b = art.BRASS
    im = img()
    for y in range(16):
        for x in range(16):
            k = 3
            if x in (2, 3, 4):
                k = 4
            if x == 3:
                k = 5
            if x >= 12:
                k = 2
            if y == 0:
                k = min(5, k + 1)
            if y == 15:
                k = max(1, k - 1)
            im.putpixel((x, y), b[k])
    return im


def lantern_iron():
    """Blackened iron of the lantern cap and wire handle: smooth, a soft highlight on the left."""
    i = art.IRON
    im = img()
    for y in range(16):
        for x in range(16):
            k = 3 if x < 5 else 2 if x < 12 else 1
            if x == 2:
                k = 4
            if y == 0:
                k = min(5, k + 1)
            im.putpixel((x, y), i[k])
    return im


def coconut_husk():
    """Brown fibrous coconut husk: vertical fibres, lit from the left."""
    h = pal('#3a2414', '#4e321c', '#634126', '#785231', '#8e643d', '#a87a4c')
    rnd = random.Random(12)
    im = img()
    for x in range(16):
        base = 3 if x < 6 else 2 if x < 12 else 1
        for y in range(16):
            k = base + (1 if (x * 7 + y // 3) % 5 == 0 else 0) - (1 if (x * 3 + y) % 7 == 0 else 0)
            im.putpixel((x, y), h[max(0, min(5, k))])
    for _ in range(6):
        x, y = rnd.randrange(16), rnd.randrange(16)
        for k in range(3):
            P(im, x, (y + k) % 16, h[5] if x < 8 else h[4])
    return im


def coconut_meat():
    """The white coconut flesh along the cut rim of the shell."""
    c = pal('#cfc6b0', '#e2dbc8', '#f0ebde', '#faf7ef')
    im = img()
    for y in range(16):
        for x in range(16):
            im.putpixel((x, y), c[2] if (x + y) % 5 else c[3])
    return im


def sea_glass_vase():
    """Frosted sea glass of the coral vase: smooth, a bright streak on the left, a few tiny bubbles."""
    g = art.SEA_GLASS
    im = img()
    for y in range(16):
        for x in range(16):
            k = 3 if x < 11 else 2
            if x in (2, 3):
                k = 4
            if y >= 14:
                k -= 1
            im.putpixel((x, y), g[k])
    for x, y in ((6, 4), (9, 9), (5, 11), (12, 6)):
        P(im, x, y, g[5])
    return im


def sea_glass_vase_top():
    """Top of the vase lip: a glass ring round the dark opening."""
    g = art.SEA_GLASS
    im = img()
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5))
            im.putpixel((x, y), g[4] if d > 5.5 else g[3] if d > 4.5 else g[0] if d > 3.5 else tex.rgb('#1c3a37'))
    return im


def iron_dark():
    return art.metal(art.IRON, seed=3)


def brass():
    return art.metal(art.BRASS, seed=5)


# ------------------------------------------------------------------------------------------------- item sprites
def outline(im):
    return tex.outline(im)


def sprite(draw):
    im = img()
    draw(im)
    return tex.outline(im)


def recolour(src_img, src_ramp_order, dest):
    """Map every colour of an image to the destination ramp by brightness rank (keeps the shading)."""
    cols = sorted({c for c in src_img.getdata() if c[3] == 255}, key=lambda c: c[0] * 0.3 + c[1] * 0.59 + c[2] * 0.11)
    n = len(dest)
    out = Image.new('RGBA', src_img.size, (0, 0, 0, 0))
    for y in range(src_img.height):
        for x in range(src_img.width):
            c = src_img.getpixel((x, y))
            if c[3] == 0:
                continue
            i = cols.index(c)
            k = int(round(i * (n - 1) / max(1, len(cols) - 1)))
            out.putpixel((x, y), dest[k] + (255,) if len(dest[k]) == 3 else dest[k])
    return out
