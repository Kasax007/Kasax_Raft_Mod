"""16x16 item sprites of the diving gear (vanilla style: outlined, lit from the top-left, ~70 % of the canvas)."""
from bclib import tex

from content.diving import art
from content.diving.art import BRASS, COPPER, STEEL, DARK_STEEL, RED, WHITE, TITANIUM, TEAL, GLASS, NEOPRENE, \
    YELLOW, FIN, RUBBER, TEAK


def outline(img, colour=None):
    return tex.outline(img, colour)


def diving_helmet():
    img = tex.new()
    # breastplate (corselet) under the dome
    for y in range(11, 15):
        half = 5 + (y - 11) // 2
        for x in range(8 - half - 1, 8 + half + 1):
            dx = (x - 7.5) / (half + 1)
            tex.px(img, x, y, art.pick(BRASS, 0.55 - 0.35 * dx * dx - 0.08 * (y - 11) - 0.15 * dx))
    for x in (3, 6, 9, 12):
        tex.px(img, x, 13, BRASS[5])
    # dome
    art.sphere(img, 8.0, 7.2, 5.6, 5.9, BRASS, mask=lambda x, y: y <= 11)
    # front porthole: rim + glass + bolts
    art.ring(img, 8.0, 7.5, 3.4, 2.4, BRASS, bias=-0.12)
    art.disc_glass(img, 8.0, 7.5, 2.4)
    for bx, by in ((8, 3), (4, 7), (11, 7), (8, 11)):
        tex.px(img, bx, by, BRASS[5])
    # small top valve
    tex.px(img, 7, 1, BRASS[2]); tex.px(img, 8, 1, BRASS[3])
    tex.px(img, 6, 0, RED[3]); tex.px(img, 7, 0, RED[4]); tex.px(img, 8, 0, RED[3]); tex.px(img, 9, 0, RED[2])
    return outline(img)


def _tank(img, x0, x1, body, band, cap, label=None):
    art.cyl_v(img, x0, x1, 4, 14, body, top_light=0.25)
    # rounded shoulders and bottom
    for x in (x0, x1):
        img.putpixel((x, 4), (0, 0, 0, 0))
        img.putpixel((x, 14), (0, 0, 0, 0))
    art.cyl_v(img, x0, x1, 6, 7, band)
    art.cyl_v(img, x0 + 1, x1 - 1, 2, 3, cap)
    if label:
        tex.px(img, (x0 + x1) // 2, 10, label)


def oxygen_tank():
    img = tex.new()
    _tank(img, 2, 6, RED, WHITE, BRASS)
    _tank(img, 9, 13, RED, WHITE, BRASS)
    # manifold and regulator
    art.cyl_h(img, 4, 11, 1, 2, BRASS)
    art.cyl_v(img, 7, 8, 0, 3, DARK_STEEL)
    # harness strap
    for x in range(2, 14):
        tex.px(img, x, 11, art.pick(art.WEBBING, 0.45 if x % 3 else 0.7))
    tex.px(img, 7, 11, BRASS[4]); tex.px(img, 8, 11, BRASS[3])
    return outline(img)


def titanium_oxygen_tank():
    img = tex.new()
    _tank(img, 2, 6, TITANIUM, TEAL, STEEL)
    _tank(img, 9, 13, TITANIUM, TEAL, STEEL)
    art.cyl_h(img, 4, 11, 1, 2, STEEL)
    art.cyl_v(img, 7, 8, 0, 3, DARK_STEEL)
    for x in range(2, 14):
        tex.px(img, x, 11, art.pick(art.tex.ramp('#2c3e5c', 4, spread=0.3), 0.45 if x % 3 else 0.7))
    tex.px(img, 7, 11, TITANIUM[5]); tex.px(img, 8, 11, TITANIUM[4])
    return outline(img)


def wetsuit():
    """Leggings-style sprite: neoprene with teal side stripes and yellow knee pads."""
    img = tex.new()
    shape = [
        "..############..",
        "..############..",
        "..######.#####..",
        "..#####...####..",
        "..#####...####..",
        "..#####...####..",
        "..#####...####..",
        "..#####...####..",
        "..#####...####..",
        "..#####...####..",
        "..#####...####..",
        "..#####...####..",
        "..#####...####..",
    ]
    for y, row in enumerate(shape):
        for x, ch in enumerate(row):
            if ch == '#':
                left_leg = x < 8
                lx = (x - 2) if left_leg else (x - 10)
                w = 5 if left_leg else 4
                v = 0.6 - 0.25 * (lx / w) - 0.012 * y
                tex.px(img, x, y + 1, art.pick(NEOPRENE, v))
    # waistband
    for x in range(2, 14):
        tex.px(img, x, 1, art.pick(NEOPRENE, 0.95 if x < 8 else 0.8))
    # teal outer stripes
    for y in range(2, 14):
        tex.px(img, 2, y, TEAL[3])
        tex.px(img, 13, y, TEAL[2])
    # yellow knee pads
    for y in (8, 9):
        for x in (4, 5, 6):
            tex.px(img, x, y, YELLOW[3] if y == 8 else YELLOW[2])
        for x in (10, 11):
            tex.px(img, x, y, YELLOW[3] if y == 8 else YELLOW[1])
    # zip pull
    tex.px(img, 7, 2, STEEL[4])
    return outline(img)


def flippers():
    """A pair of fins standing side by side: dark foot pockets with a yellow strap, blue blades with ribs and a notch."""
    img = tex.new()

    def fin(ox, oy, dark=0.0):
        rows = [(1, 4), (1, 4), (1, 4), (0, 5), (0, 5), (0, 5), (-1, 6), (-1, 6), (-1, 6), (-1, 6)]
        for i, (a, b) in enumerate(rows):
            for x in range(a, b + 1):
                t = (x - a) / max(1, b - a)
                rib = x in (1, 4) and i > 3
                v = 0.72 - 0.4 * t - 0.035 * i - dark - (0.18 if rib else 0.0)
                tex.px(img, ox + x, oy + 4 + i, art.pick(FIN, v))
        # notch at the tip
        tex.px(img, ox + 2, oy + 13, (0, 0, 0, 0)); tex.px(img, ox + 3, oy + 13, (0, 0, 0, 0))
        # foot pocket
        for y in range(0, 5):
            for x in range(1, 5):
                if y == 0 and x in (1, 4):
                    continue
                tex.px(img, ox + x, oy + y, art.pick(RUBBER, 0.75 - 0.18 * (x - 1) - 0.05 * y - dark))
        for x in range(1, 5):
            tex.px(img, ox + x, oy + 2, art.pick(YELLOW, 0.75 - 0.12 * x - dark))

    fin(8, 1, dark=0.12)
    fin(2, 0)
    return outline(img)


def harpoon():
    """Diagonal bolt, point up-right."""
    img = tex.new()
    # shaft from (3,12) to (11,4)
    for i in range(9):
        x, y = 3 + i, 12 - i
        tex.px(img, x, y, STEEL[3])
        tex.px(img, x + 1, y, STEEL[1])
    # head: blade + point
    for (x, y, c) in ((12, 3, STEEL[4]), (13, 2, STEEL[5]), (14, 1, STEEL[5]), (12, 2, STEEL[3]), (13, 3, STEEL[2]),
                      (14, 2, STEEL[3]), (13, 1, STEEL[4])):
        tex.px(img, x, y, c)
    # barbs
    tex.px(img, 10, 3, STEEL[4]); tex.px(img, 9, 3, STEEL[3])
    tex.px(img, 13, 6, STEEL[2]); tex.px(img, 13, 5, STEEL[3])
    # brass collar
    tex.px(img, 5, 10, BRASS[4]); tex.px(img, 6, 10, BRASS[2]); tex.px(img, 5, 11, BRASS[3])
    # fins red/white
    for (x, y, c) in ((1, 12, RED[3]), (2, 13, RED[2]), (2, 11, WHITE[2]), (3, 14, WHITE[1]), (1, 14, RED[1]),
                      (1, 13, WHITE[3]), (0, 13, RED[2]), (2, 14, RED[3]), (3, 13, RED[2])):
        tex.px(img, x, y, c)
    return outline(img)


def harpoon_gun(loaded=True):
    """Pneumatic speargun, muzzle up-right: steel barrel, brass air chamber, teak pistol grip, harpoon on top."""
    img = tex.new()
    # barrel (2 px) from (2,13) to (12,3)
    for i in range(11):
        x, y = 2 + i, 13 - i
        tex.px(img, x, y, STEEL[2])
        tex.px(img, x + 1, y, STEEL[1])
    # brass air chamber: thicker middle part
    for i in range(5):
        x, y = 4 + i, 12 - i
        tex.px(img, x, y - 1, BRASS[4])
        tex.px(img, x + 1, y - 1, BRASS[3])
        tex.px(img, x + 1, y, BRASS[2])
        tex.px(img, x + 2, y, BRASS[1])
    # muzzle band
    tex.px(img, 11, 3, BRASS[4]); tex.px(img, 12, 4, BRASS[2]); tex.px(img, 13, 3, BRASS[1])
    # teak pistol grip, perpendicular to the barrel near the back
    for (x, y, c) in ((4, 13, TEAK[3]), (5, 14, TEAK[3]), (6, 15, TEAK[2]), (5, 13, TEAK[2]), (6, 14, TEAK[1]),
                      (7, 15, TEAK[1]), (3, 14, TEAK[4])):
        tex.px(img, x, y, c)
    # trigger
    tex.px(img, 7, 12, DARK_STEEL[2]); tex.px(img, 8, 13, DARK_STEEL[1])
    # butt cap
    tex.px(img, 1, 14, DARK_STEEL[3]); tex.px(img, 2, 14, DARK_STEEL[2])
    if loaded:
        # harpoon shaft on top of the barrel and its point past the muzzle
        for i in range(10):
            tex.px(img, 3 + i, 11 - i, STEEL[5] if i % 3 else STEEL[4])
        tex.px(img, 13, 1, STEEL[5]); tex.px(img, 14, 0, STEEL[5]); tex.px(img, 14, 1, STEEL[3]); tex.px(img, 13, 0, STEEL[4])
        tex.px(img, 12, 1, STEEL[3])
    return outline(img)


def sea_scooter():
    """Yellow underwater scooter, side view, nose to the right: shrouded propeller, handles, headlight."""
    img = tex.new()
    art.cyl_h(img, 4, 13, 5, 11, YELLOW)
    # rounded nose
    for y in (5, 11):
        img.putpixel((13, y), (0, 0, 0, 0))
    tex.px(img, 14, 7, YELLOW[2]); tex.px(img, 14, 8, YELLOW[2]); tex.px(img, 14, 9, YELLOW[1])
    # headlight
    tex.px(img, 14, 7, '#e8f4f8'); tex.px(img, 13, 7, '#bfe3ef'); tex.px(img, 14, 8, '#9cc9d8')
    # dark stripe and grille
    for x in range(7, 12):
        tex.px(img, x, 9, art.pick(DARK_STEEL, 0.35 if x % 2 else 0.6))
    # handles on top
    for (x, y) in ((6, 3), (6, 4), (7, 3), (10, 3), (10, 4), (9, 3)):
        tex.px(img, x, y, RUBBER[2] if y == 3 else RUBBER[1])
    # shroud ring (back) with propeller
    art.cyl_v(img, 1, 3, 3, 13, DARK_STEEL)
    for y in range(5, 12):
        tex.px(img, 2, y, DARK_STEEL[0])
    tex.px(img, 2, 6, STEEL[4]); tex.px(img, 2, 10, STEEL[3]); tex.px(img, 2, 8, BRASS[3])
    # battery light
    tex.px(img, 11, 6, '#7af07a')
    return outline(img)


def generate():
    return {
        'diving_helmet': diving_helmet(),
        'oxygen_tank': oxygen_tank(),
        'titanium_oxygen_tank': titanium_oxygen_tank(),
        'wetsuit': wetsuit(),
        'flippers': flippers(),
        'harpoon': harpoon(),
        'harpoon_gun': harpoon_gun(True),
        'harpoon_gun_empty': harpoon_gun(False),
        'sea_scooter': sea_scooter(),
    }
