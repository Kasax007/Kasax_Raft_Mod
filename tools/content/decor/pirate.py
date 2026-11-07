"""Pirate and ship decor: cannon, anchor, treasure, flags, bottles, railing, net, keg, crate, bell, chart, spyglass,
figurehead and life ring."""
import math
import random

from bclib import assets as A
from bclib import extrude, tex

from content.decor import art
from content.decor.art import P, fill
from content.decor.lights import small_item
from content.decor.mdl import Model, M, rid, facing_variants, shape


def _sides(t):
    return {f: t for f in ('north', 'south', 'west', 'east')}


# ================================================================================================= cannon
def ship_cannon():
    # dark cast iron with a soft sheen
    ci = art.pal('#17181c', '#202227', '#2a2d33', '#363a41', '#454a52', '#5c626b')
    im = tex.noise_fill(art.img(), ci[1:4], 61, 4, 2, 0.8)
    for (x, y) in ((3, 2), (4, 2), (11, 9), (12, 9), (6, 13)):
        P(im, x, y, ci[4])
    art.save(im, 'block/ship_cannon_iron')
    bore = art.img()
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5))
            bore.putpixel((x, y), tex.rgb('#0e0e10' if d < 5 else '#26282d'))
    art.save(bore, 'block/ship_cannon_bore')
    # wheel: plank disc with iron rim
    wh = art.img()
    t = art.TEAK
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            c = t[4] if (x // 3) % 2 else t[3]
            if d > 6.5:
                c = ci[3]
            elif d < 2:
                c = ci[4]
            wh.putpixel((x, y), c)
    art.save(wh, 'block/ship_cannon_wheel')
    m = Model({'wood': 'block/decor_teak', 'iron': 'block/ship_cannon_iron', 'bore': 'block/ship_cannon_bore',
               'wheel': 'block/ship_cannon_wheel', 'fittings': 'block/decor_iron'}, particle='block/ship_cannon_iron')
    # carriage
    m.box((3, 2, 4), (5, 8, 11), 'wood')
    m.box((11, 2, 4), (13, 8, 11), 'wood')
    m.box((3, 2, 11), (5, 6, 15), 'wood')
    m.box((11, 2, 11), (13, 6, 15), 'wood')
    m.box((5, 2, 4), (11, 4, 15), 'wood')
    m.box((1.5, 1.5, 5), (14.5, 3, 6.5), 'fittings', collide=False)   # front axle, like the rear one (its bottom is not
                                                                     # in the plane of the carriage bottom)
    m.box((1.5, 1.5, 12), (14.5, 3, 13.5), 'fittings', collide=False)
    whf = {'west': ('wheel', [0, 0, 16, 16]), 'east': ('wheel', [0, 0, 16, 16])}
    for x0 in (1, 13):
        m.box((x0, 0, 3.25), (x0 + 2, 5.5, 8.75), 'wood', faces=whf)
        m.box((x0, 0, 10), (x0 + 2, 4.5, 14.5), 'wood', faces=whf)
    # barrel
    m.box((5.5, 6, 1), (10.5, 11, 13), 'iron')
    m.box((5, 5.5, 0), (11, 11.5, 2), 'iron', faces={'north': ('bore', [2, 2, 14, 14])})
    m.box((5, 5.5, 6), (11, 11.5, 7), 'iron')
    m.box((5, 5.5, 11), (11, 11.5, 12.5), 'iron')
    m.box((6, 6.5, 13), (10, 10.5, 15), 'iron')
    m.box((7, 7.5, 15), (9, 9.5, 16), 'iron')
    m.box((4.5, 7.5, 7.5), (11.5, 9.5, 9.5), 'fittings', collide=False)
    m.box((7.5, 11, 12.5), (8.5, 11.75, 13.5), 'fittings', collide=False)
    m.write('ship_cannon')
    shape('ship_cannon', [(1, 0, 3, 15, 8, 15), (5, 5.5, 0, 11, 11.5, 15), (7, 7.5, 15, 9, 9.5, 16)])
    A.blockstate(M, 'ship_cannon', facing_variants('block/ship_cannon'))
    A.block_item(M, 'ship_cannon')


def cannonball_and_coin():
    ci = art.pal('#1c1d22', '#2a2c32', '#3a3d45', '#50545e', '#6f7480', '#9aa0aa')
    im = art.img()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d <= 6.2:
                l = math.hypot(x - 5.5, y - 5.5)
                k = 4 if l < 1.6 else 3 if l < 3.6 else 2 if l < 6 else 1
                im.putpixel((x, y), ci[k])
    P(im, 5, 5, ci[5])
    art.save(art.sprite_finish(im, '#0f1013'), 'item/cannonball')
    A.flat_item(M, 'cannonball')
    g = art.GOLD
    im = art.img()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d <= 5.6:
                k = 3
                if d > 4.4:
                    k = 2 if (x + y) > 15 else 4
                elif d < 3.2:
                    k = 3 if (x + y) % 5 else 4
                im.putpixel((x, y), g[k])
    # embossed anchor
    for (x, y) in ((7, 5), (8, 5), (7, 6), (8, 6), (7, 7), (8, 7), (7, 8), (8, 8), (6, 9), (9, 9), (7, 10), (8, 10), (6, 6), (9, 6)):
        P(im, x, y, g[2])
    P(im, 5, 5, g[5])
    art.save(art.sprite_finish(im, '#5a3a0c'), 'item/gold_coin')
    A.flat_item(M, 'gold_coin')


# ================================================================================================= anchor
def ship_anchor():
    ai = art.pal('#2a2624', '#3a3431', '#4c4440', '#615751', '#786c63', '#8f8277')
    im = tex.noise_fill(art.img(), ai[1:5], 71, 3, 2, 1.0)
    tex.overlay_spots(im, art.pal('#6e3a1e', '#8a4a24', '#a65f2e'), 0.18, 72, 3)
    art.save(im, 'block/ship_anchor')
    m = Model({'iron': 'block/ship_anchor', 'rope': 'block/decor_rope', 'ring': 'block/decor_iron'})
    z0, z1 = 7, 9
    # ring
    m.box((6.5, 15, z0 + 0.25), (9.5, 16, z1 - 0.25), 'ring', collide=False)
    m.box((6, 13, z0 + 0.25), (7, 15.5, z1 - 0.25), 'ring', collide=False)
    m.box((9, 13, z0 + 0.25), (10, 15.5, z1 - 0.25), 'ring', collide=False)
    # stock and shank
    m.box((3, 11.5, 6.5), (13, 13, 9.5), 'iron')
    m.box((2.5, 11.25, 6.25), (3.5, 13.25, 9.75), 'iron', collide=False)
    m.box((12.5, 11.25, 6.25), (13.5, 13.25, 9.75), 'iron', collide=False)
    m.box((7, 2, z0), (9, 11.5, z1), 'iron')
    m.box((7.25, 13, z0 + 0.5), (8.75, 14, z1 - 0.5), 'iron', collide=False)
    # crown and stepped arms with flukes
    m.box((6, 0.5, z0 - 0.5), (10, 3, z1 + 0.5), 'iron')
    for sgn in (-1, 1):
        def bx(a0, y0, a1, y1, dz=0.0):
            xs = sorted((8 + sgn * a0, 8 + sgn * a1))
            m.box((xs[0], y0, z0 + dz), (xs[1], y1, z1 - dz), 'iron')
        bx(2, 1, 4, 2.5)
        bx(4, 1.5, 5.5, 3.5)
        bx(5, 2.5, 6.5, 5)
        bx(5.5, 4.5, 7, 7)
        # fluke (wider, thinner)
        bx(4.5, 4, 7.5, 8, -0.75)
        bx(5.25, 8, 6.75, 9, -0.25)
    # rope wrapped around the shank
    m.box((6.6, 8, 6.6), (9.4, 9.5, 9.4), 'rope', collide=False)
    m.box((6.6, 5, 6.6), (9.4, 6, 9.4), 'rope', collide=False)
    m.write('ship_anchor')
    shape('ship_anchor', [(2, 0, 6, 14, 13.5, 10), (6, 13, 7, 10, 16, 9)])
    A.blockstate(M, 'ship_anchor', facing_variants('block/ship_anchor'))
    A.block_item(M, 'ship_anchor')


# ================================================================================================= treasure
def _chest_textures():
    t = art.TEAK
    band = art.pal('#2a2a2e', '#3d3d43', '#55555c', '#707078')
    g = art.GOLD

    def planks_v():
        im = art.img()
        for y in range(16):
            for x in range(16):
                k = 3 if (x // 4) % 2 else 4
                if x % 4 == 3:
                    k = 2
                im.putpixel((x, y), t[k])
        for (x, y) in ((1, 4), (9, 11), (13, 2)):
            P(im, x, y, t[2])
        return im

    side = planks_v()
    for y in range(16):
        for x in (2, 13):
            P(side, x, y, band[2])
            P(side, x + 1 if x == 2 else x - 1, y, band[1])
    for x in range(16):
        P(side, x, 7, band[1])
        P(side, x, 1, band[2])
        P(side, x, 15, band[1])
    for (x, y) in ((2, 3), (13, 3), (2, 11), (13, 11)):
        P(side, x, y, band[3])
    art.save(side, 'block/treasure_chest_side')
    front = side.copy()
    # gold lock plate across the lid edge
    fill(front, g[2], (6, 5, 9, 10))
    fill(front, g[3], (6, 5, 8, 9))
    P(front, 7, 8, '#1b1308')
    P(front, 7, 9, '#2b1d0c')
    P(front, 6, 5, g[4])
    art.save(front, 'block/treasure_chest_front')
    top = planks_v().transpose(art.Image.Transpose.ROTATE_90)
    for y in range(16):
        for x in (2, 13):
            P(top, x, y, band[2])
    for x in range(16):
        P(top, x, 0, band[1])
        P(top, x, 15, band[1])
    for (x, y) in ((2, 3), (13, 3), (2, 12), (13, 12)):
        P(top, x, y, band[3])
    art.save(top, 'block/treasure_chest_top')
    # inside: dark wood with a heap of gold
    ins = art.img()
    for y in range(16):
        for x in range(16):
            ins.putpixel((x, y), t[1] if (x + y) % 7 else t[0])
    rnd = random.Random(5)
    for y in range(3, 14):
        for x in range(3, 14):
            if math.hypot(x - 8, y - 8.5) < 5.2 + rnd.random() * 0.8:
                k = rnd.choice([2, 3, 3, 4])
                ins.putpixel((x, y), g[k])
    for (x, y) in ((6, 6), (9, 8), (7, 10), (10, 5)):
        P(ins, x, y, g[5])
    P(ins, 4, 10, '#c8303a')
    P(ins, 11, 11, '#3aa0e8')
    art.save(ins, 'block/treasure_chest_inside')
    lid_in = art.img()
    for y in range(16):
        for x in range(16):
            lid_in.putpixel((x, y), t[2] if (x // 4) % 2 else t[1])
    art.save(lid_in, 'block/treasure_chest_lid_inside')


def treasure_chest():
    _chest_textures()
    tx = {'side': 'block/treasure_chest_side', 'front': 'block/treasure_chest_front', 'top': 'block/treasure_chest_top',
          'inside': 'block/treasure_chest_inside', 'lid': 'block/treasure_chest_lid_inside', 'gold': 'block/decor_brass'}
    # closed
    m = Model(tx, particle='block/treasure_chest_side')
    m.box((1, 0, 1), (15, 9, 15), 'side', faces={'north': 'front', 'up': 'top', 'down': 'top'})
    m.box((1, 9, 1), (15, 14, 15), 'side', faces={'north': 'front', 'up': 'top', 'down': 'lid'})
    m.box((2, 14, 2), (14, 15, 14), 'top', faces=_sides('side'))
    m.box((7, 7, 0), (9, 11, 1), 'gold')
    m.write('treasure_chest')
    # open: lid stands up behind the hinge, the treasure shows
    m = Model(tx, particle='block/treasure_chest_side')
    m.box((1, 0, 1), (15, 9, 15), 'side', faces={'north': 'front', 'up': 'inside', 'down': 'top'})
    m.box((1, 9, 15), (15, 23, 20), 'side', faces={
        'north': ('lid', [1, 1, 15, 15]), 'south': ('top', [1, 1, 15, 15], 180),
        'up': ('front', [1, 2, 15, 7], 180), 'down': ('side', [1, 2, 15, 7]),
        'west': ('side', [2, 1, 7, 15], 90), 'east': ('side', [2, 1, 7, 15], 270)}, collide=False)
    m.box((7, 21, 14), (9, 23, 15), 'gold', collide=False)
    m.write('treasure_chest_open')
    shape('treasure_chest', [(1, 0, 1, 15, 15, 15)])
    shape('treasure_chest_open', [(1, 0, 1, 15, 9, 15), (1, 9, 14, 15, 16, 16)])
    v = {}
    for o in ('false', 'true'):
        v.update(facing_variants('block/treasure_chest' + ('_open' if o == 'true' else ''), props=f'open={o}'))
    A.blockstate(M, 'treasure_chest', v)
    A.block_item(M, 'treasure_chest')


def gold_coin_pile():
    g = art.GOLD
    top = art.img()
    rnd = random.Random(9)
    for y in range(16):
        for x in range(16):
            top.putpixel((x, y), g[1] if (x + y) % 3 else g[0])
    # staggered rows of overlapping coins (4 px discs): bright rim top-left, shadow bottom-right
    coin = [(1, 0), (2, 0), (0, 1), (1, 1), (2, 1), (3, 1), (0, 2), (1, 2), (2, 2), (3, 2), (1, 3), (2, 3)]
    spots = []
    for row in range(4):
        for col in range(4):
            spots.append(((col * 4 + (2 if row % 2 else 0) + rnd.randrange(0, 2)) % 16, (row * 4 + rnd.randrange(0, 2)) % 16))
    spots += [(rnd.randrange(16), rnd.randrange(16)) for _ in range(4)]
    rnd.shuffle(spots)
    for (cx, cy) in spots:
        for (dx, dy) in coin:
            k = 3
            if dx + dy <= 1:
                k = 5
            elif dy == 0 or dx == 0:
                k = 4
            elif dx + dy >= 5 or dy == 3:
                k = 2
            top.putpixel(((cx + dx) % 16, (cy + dy) % 16), g[k])
    art.save(top, 'block/gold_coin_pile')
    side = art.img()
    for y in range(16):
        for x in range(16):
            k = 3 if y % 2 == 0 else 2
            if (x + (y // 2) * 5) % 7 == 0:
                k = 1
            if (x * 3 + y) % 11 == 0 and y % 2 == 0:
                k = 4
            side.putpixel((x, y), g[k])
    art.save(side, 'block/gold_coin_pile_side')
    variants = {}
    for n in range(1, 9):
        h = 2 * n
        m = Model({'top': 'block/gold_coin_pile', 'side': 'block/gold_coin_pile_side'}, particle='block/gold_coin_pile')
        if n == 8:
            m.box((0, 0, 0), (16, 16, 16), 'side', faces={'up': 'top', 'down': 'top'}, cull=True)
        else:
            m.box((0, 0, 0), (16, h, 16), 'side', faces={'up': 'top', 'down': 'top'}, cull=True)
            m.box((3, h, 4), (12, h + 1, 13), 'side', faces={'up': 'top'}, collide=False)
            m.box((5.5, h + 1, 6), (10, h + 1.5, 10.5), 'side', faces={'up': 'top'}, collide=False)
        m.write(f'gold_coin_pile_{n}')
        shape(f'gold_coin_pile_{n}', [(0, 0, 0, 16, h, 16)])
        variants[f'layers={n}'] = {'model': rid(f'block/gold_coin_pile_{n}')}
    A.blockstate(M, 'gold_coin_pile', variants)
    A.block_item(M, 'gold_coin_pile', 'block/gold_coin_pile_2')


# ================================================================================================= flags and paper
def jolly_roger():
    im = art.img()
    k = art.BLACK_CLOTH
    for y in range(16):
        for x in range(16):
            t = 2 + (1 if (x + y // 3) % 6 == 0 else 0) - (1 if x % 7 == 3 else 0)
            im.putpixel((x, y), k[t])
    b = art.pal('#9a968a', '#c4c0b2', '#e2ded0', '#f6f3ea')
    # skull
    skull = ['..####..', '.######.', '##.##.##', '########', '.######.', '..#.#.#.']
    for j, row in enumerate(skull):
        for i, ch in enumerate(row):
            if ch == '#':
                P(im, 4 + i, 2 + j, b[3] if j < 2 or i < 3 else b[2])
    P(im, 6, 4, k[0])
    P(im, 9, 4, k[0])
    P(im, 7, 7, b[1])
    # crossbones
    for i in range(8):
        P(im, 4 + i, 9 + i // 2, b[2])
        P(im, 11 - i, 9 + i // 2, b[3] if i < 4 else b[2])
    for (x, y) in ((3, 8), (4, 8), (12, 8), (11, 8), (3, 13), (4, 13), (11, 13), (12, 13)):
        P(im, x, y, b[2])
    # frayed bottom
    for x in range(16):
        if x % 5 == 2 or x % 7 == 0:
            P(im, x, 15, (0, 0, 0, 0))
    art.save(im, 'block/jolly_roger')
    m = Model({'flag': 'block/jolly_roger', 'wood': 'block/decor_teak', 'iron': 'block/decor_iron'})
    m.box((0.5, 14, 14), (15.5, 15, 15), 'wood')
    m.box((0, 13.75, 13.75), (1, 15.25, 15.25), 'wood', collide=False)
    m.box((15, 13.75, 13.75), (16, 15.25, 15.25), 'wood', collide=False)
    for x in (3, 12):
        m.box((x, 14, 15), (x + 1, 15, 16), 'iron', collide=False)
    m.box((1, 1, 14.4), (15, 14, 14.6), 'flag', faces={'north': ('flag', [1, 2, 15, 15]), 'south': ('flag', [15, 2, 1, 15])},
          only=('north', 'south'), collide=False)
    m.write('jolly_roger')
    shape('jolly_roger', [(0, 1, 13.5, 16, 15.5, 16)])
    A.blockstate(M, 'jolly_roger', facing_variants('block/jolly_roger'))
    it = im.crop((0, 0, 16, 16)).copy()
    sp = art.img()
    sp.alpha_composite(it.resize((14, 13)), (1, 2))
    for x in range(1, 15):
        P(sp, x, 1, art.TEAK[4])
    art.save(art.sprite_finish(sp), 'item/jolly_roger')
    A.flat_item(M, 'jolly_roger')


def wanted_poster():
    p = art.PARCHMENT
    im = art.img()
    rnd = random.Random(3)
    for y in range(16):
        for x in range(16):
            t = 3 + (1 if rnd.random() < 0.15 else 0) - (1 if (x in (2, 13) or y in (1, 14)) else 0)
            im.putpixel((x, y), p[t])
    ink = art.pal('#2a1a10', '#3e2a1a', '#5a3e26')
    # "WANTED" headline (bold strokes)
    for x in range(3, 13):
        P(im, x, 2, ink[1] if x % 2 else ink[0])
        P(im, x, 3, ink[0] if x not in (5, 8, 11) else p[3])
    # portrait frame + face of Saltjaw (hat, eye patch, beard)
    fill(im, p[2], (4, 5, 11, 11))
    fill(im, ink[1], (4, 5, 11, 6))           # tricorn hat
    P(im, 3, 6, ink[1])
    P(im, 12, 6, ink[1])
    P(im, 7, 5, p[5])                           # skull badge on the hat
    fill(im, '#c99a70', (5, 7, 10, 10))        # face
    P(im, 6, 8, ink[0])                         # eye patch
    P(im, 7, 8, ink[0])
    P(im, 5, 7, ink[0])
    P(im, 9, 8, '#203040')
    fill(im, '#6a3a20', (5, 10, 10, 11))       # red-brown beard
    P(im, 6, 11, '#7e4a2a')
    P(im, 8, 10, '#c8b090')                     # grin
    # reward line
    for x in range(4, 12):
        P(im, x, 13, ink[2] if x % 3 else ink[0])
    P(im, 3, 13, '#7a5a10')
    P(im, 12, 13, '#7a5a10')
    # pins
    P(im, 2, 1, '#c8303a')
    P(im, 13, 1, '#c8303a')
    # torn corner
    P(im, 13, 14, (0, 0, 0, 0))
    P(im, 12, 14, p[1])
    P(im, 13, 13, p[1])
    art.save(im, 'block/wanted_poster')
    m = Model({'paper': 'block/wanted_poster'})
    # the paper is extruded row by row, so its thin edges follow the torn corner instead of sampling a strip
    m.elements += extrude.vertical('raft:block/wanted_poster', '#paper', region=(2, 1, 14, 15), z=(15.5, 15.95))
    m.write('wanted_poster')
    shape('wanted_poster', [(2, 1, 15, 14, 15, 16)])
    A.blockstate(M, 'wanted_poster', facing_variants('block/wanted_poster'))
    sp = art.img()
    sp.alpha_composite(im.crop((2, 1, 14, 15)), (2, 1))
    art.save(art.sprite_finish(sp), 'item/wanted_poster')
    A.flat_item(M, 'wanted_poster')


# ================================================================================================= bottles
def rum_bottles():
    im = art.img()
    glass = [tex.ramp('#2f6a2a', 4, 0.5), tex.ramp('#8a4a14', 4, 0.5), tex.ramp('#3a2a1e', 4, 0.45)]
    label = art.PARCHMENT
    for c, r in enumerate(glass):
        x0 = c * 5
        for y in range(16):
            for x in range(5):
                k = 2 if x in (1, 2) else 1
                if x == 1:
                    k = 3
                P(im, x0 + x, y, r[k][:3] + (235,))
        # label rows 8..11
        for y in range(8, 12):
            for x in range(5):
                P(im, x0 + x, y, label[3] if y in (9, 10) else label[2])
        P(im, x0 + 2, 9, '#8a1e1a')
        P(im, x0 + 3, 10, '#2a1a10')
    # cork at 15..16 columns
    for y in range(16):
        P(im, 15, y, art.WICKER[3] if y % 3 else art.WICKER[2])
    art.save(im, 'block/rum_bottles')
    spots = [[(8, 8, 0)], [(5.5, 7.5, 0), (10.5, 9, 1)], [(5, 6, 0), (10.5, 6.5, 1), (8, 11, 2)]]
    for n, pos in enumerate(spots, 1):
        m = Model({'b': 'block/rum_bottles'})
        boxes = []
        for (cx, cz, c) in pos:
            u = c * 5
            body = {f: ('b', [u, 6, u + 3, 13]) for f in ('north', 'south', 'west', 'east')}
            body['up'] = ('b', [u + 1, 0, u + 4, 3])
            body['down'] = ('b', [u + 1, 0, u + 4, 3])
            m.box((cx - 1.5, 0, cz - 1.5), (cx + 1.5, 6, cz + 1.5), 'b', faces=body)
            sh = {f: ('b', [u, 4, u + 2, 5]) for f in ('north', 'south', 'west', 'east')}
            sh['up'] = ('b', [u, 4, u + 2, 6])
            m.box((cx - 1, 6, cz - 1), (cx + 1, 7, cz + 1), 'b', faces=sh, skip=('down',), collide=False)
            nk = {f: ('b', [u + 1, 0, u + 2, 3]) for f in ('north', 'south', 'west', 'east')}
            m.box((cx - 0.5, 7, cz - 0.5), (cx + 0.5, 9.5, cz + 0.5), 'b', faces=nk, skip=('down', 'up'), collide=False)
            ck = {f: ('b', [15, 0, 16, 2]) for f in ('north', 'south', 'west', 'east', 'up')}
            m.box((cx - 0.6, 9.5, cz - 0.6), (cx + 0.6, 10.5, cz + 0.6), 'b', faces=ck, skip=('down',), collide=False)
            boxes.append((cx - 1.5, 0, cz - 1.5, cx + 1.5, 10.5, cz + 1.5))
        m.write(f'rum_bottles_{n}')
        shape(f'rum_bottles_{n}', boxes)
    v = {}
    for n in (1, 2, 3):
        v.update(facing_variants(f'block/rum_bottles_{n}', props=f'bottles={n}'))
    # facing must come first in the key; rebuild keys as "bottles=n,facing=x" (alphabetical order)
    v2 = {}
    for k, e in v.items():
        parts = dict(p.split('=') for p in k.split(','))
        v2[f"bottles={parts['bottles']},facing={parts['facing']}"] = e
    A.blockstate(M, 'rum_bottles', v2)
    sp = art.img()
    r = glass[0]
    for y in range(6, 16):
        for x in range(6, 10):
            P(sp, x, y, r[2] if x == 7 else r[1])
    for y in range(3, 6):
        P(sp, 7, y, r[2])
        P(sp, 8, y, r[1])
    P(sp, 7, 2, art.WICKER[3])
    P(sp, 8, 2, art.WICKER[2])
    for y in (10, 11, 12):
        for x in range(6, 10):
            P(sp, x, y, label[3])
    P(sp, 7, 11, '#8a1e1a')
    P(sp, 6, 7, r[3])
    art.save(art.sprite_finish(sp), 'item/rum_bottles')
    A.flat_item(M, 'rum_bottles')


# ================================================================================================= ropes
def rope_railing():
    tx = {'post': 'raft:block/stripped_driftwood_log', 'top': 'raft:block/stripped_driftwood_log_top',
          'rope': 'block/decor_rope'}
    m = Model(tx, particle='raft:block/stripped_driftwood_log')
    m.box((6, 0, 6), (10, 16, 10), 'post', faces={'up': 'top', 'down': 'top'})
    m.box((5.75, 12, 5.75), (10.25, 13.5, 10.25), 'rope', skip=('up', 'down'))
    m.box((5.75, 5.5, 5.75), (10.25, 7, 10.25), 'rope', skip=('up', 'down'))
    m.write('rope_railing_post')
    m = Model(tx, particle='raft:block/stripped_driftwood_log')
    m.box((7.4, 12.2, 0), (8.6, 13.3, 6), 'rope')
    m.box((7.4, 5.7, 0), (8.6, 6.8, 6), 'rope')
    m.write('rope_railing_side')
    m = Model(tx, particle='raft:block/stripped_driftwood_log')
    m.parent = 'minecraft:block/block'
    for z in (2, 10):
        m.box((6, 0, z), (10, 16, z + 4), 'post', faces={'up': 'top', 'down': 'top'})
    m.box((7.4, 12.2, 0), (8.6, 13.3, 16), 'rope')
    m.box((7.4, 5.7, 0), (8.6, 6.8, 16), 'rope')
    m.display = {'gui': {'rotation': [30, 135, 0], 'translation': [0, 0, 0], 'scale': [0.625, 0.625, 0.625]},
                 'fixed': {'rotation': [0, 90, 0], 'translation': [0, 0, 0], 'scale': [0.5, 0.5, 0.5]}}
    m.write('rope_railing_inventory')
    mp = [{'apply': {'model': rid('block/rope_railing_post')}}]
    for d, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
        a = {'model': rid('block/rope_railing_side')}
        if y:
            a['y'] = y
        mp.append({'when': {d: 'true'}, 'apply': a})
    A.blockstate(M, 'rope_railing', multipart=mp)
    A.item_def(M, 'rope_railing', 'block/rope_railing_inventory')


def cargo_net():
    im = art.img()
    r = art.ROPE
    for y in range(16):
        for x in range(16):
            on_v = x % 4 == 1
            on_h = y % 4 == 1
            if on_v or on_h:
                k = 3
                if on_v and on_h:
                    k = 4
                elif on_v:
                    k = 3 if y % 2 else 2
                else:
                    k = 3 if x % 2 else 2
                im.putpixel((x, y), r[k])
    for y in range(1, 16, 4):
        for x in range(1, 16, 4):
            P(im, x + 1, y + 1, r[1])  # knot shadow
    art.save(im, 'block/cargo_net')
    m = Model({'net': 'block/cargo_net', 'rope': 'block/decor_rope'})
    m.plane_z(0, 0, 16, 16, 15.2, 'net', uv=[0, 0, 16, 16])
    m.box((0, 14.5, 14.5), (16, 16, 16), 'rope', cull=True)
    m.write('cargo_net')
    shape('cargo_net', [(0, 0, 14, 16, 16, 16)])
    A.blockstate(M, 'cargo_net', facing_variants('block/cargo_net'))
    sp = art.img()
    for y in range(1, 15):
        for x in range(1, 15):
            if x % 3 == 1 or y % 3 == 1:
                P(sp, x, y, r[3] if (x + y) % 2 else r[4])
    for x in range(0, 16):
        P(sp, x, 1, r[4])
    art.save(art.sprite_finish(sp), 'item/cargo_net')
    A.flat_item(M, 'cargo_net')


# ================================================================================================= barrels and crates
def powder_keg():
    t = art.TEAK
    hoop = art.pal('#26262a', '#36363c', '#4a4a52', '#62626a')
    side = art.img()
    for y in range(16):
        for x in range(16):
            k = 4 if (x // 3) % 2 else 3
            if x % 3 == 2:
                k = 2
            side.putpixel((x, y), t[k])
    for y in (2, 3, 12, 13):
        for x in range(16):
            P(side, x, y, hoop[2] if y in (2, 12) else hoop[1])
    # stencilled skull + XXX
    for (x, y) in ((6, 6), (7, 6), (8, 6), (9, 6), (5, 7), (7, 7), (8, 7), (10, 7), (6, 8), (7, 8), (8, 8), (9, 8), (6, 9), (8, 9)):
        P(side, x, y, '#e8dcc0')
    for x in (5, 7, 9):
        P(side, x, 10, '#c8302a')
        P(side, x + 1, 11, '#c8302a')
    art.save(side, 'block/powder_keg_side')
    top = art.img()
    for y in range(16):
        for x in range(16):
            top.putpixel((x, y), t[4] if (x // 3) % 2 else t[3])
            if math.hypot(x - 7.5, y - 7.5) > 6.6:
                top.putpixel((x, y), hoop[2])
    fill(top, t[1], (7, 7, 8, 8))
    P(top, 4, 11, '#3a3a3e')
    art.save(top, 'block/powder_keg_top')
    m = Model({'side': 'block/powder_keg_side', 'top': 'block/powder_keg_top', 'fuse': 'block/decor_string'},
              particle='block/powder_keg_side')
    m.box((3, 0, 3), (13, 14, 13), 'side', faces={'up': 'top', 'down': 'top'}, cull=True)
    m.box((2, 2, 2), (14, 12, 14), 'side', faces={'up': ('top', [0, 0, 16, 16]), 'down': ('top', [0, 0, 16, 16])},
          uv={f: [0, 2, 16, 12] for f in ('north', 'south', 'west', 'east')})
    m.box((7.5, 14, 7.5), (8.5, 16, 8.5), 'fuse', collide=False)
    m.box((8.5, 15.5, 7.5), (10.5, 16.5, 8.5), 'fuse', collide=False)
    m.write('powder_keg')
    shape('powder_keg', [(2, 0, 2, 14, 14, 14)])
    A.blockstate(M, 'powder_keg', {'': {'model': rid('block/powder_keg')}})
    A.block_item(M, 'powder_keg')


def wooden_crate():
    w = art.pal('#5a3f22', '#7a5631', '#94693d', '#ad7f4c', '#c4965e', '#d9b07a')
    dark = art.pal('#3a2716', '#4a321c', '#5e4026')

    def frame(img_):
        for i in range(16):
            for d in (0, 1):
                P(img_, i, d, dark[2] if d == 0 else dark[1])
                P(img_, i, 15 - d, dark[1] if d == 0 else dark[2])
                P(img_, d, i, dark[2] if d == 0 else dark[1])
                P(img_, 15 - d, i, dark[1] if d == 0 else dark[2])
        for (x, y) in ((1, 1), (14, 1), (1, 14), (14, 14)):
            P(img_, x, y, '#8a8a90')
        return img_

    side = art.img()
    for y in range(16):
        for x in range(16):
            k = 3 if (y // 4) % 2 else 4
            if y % 4 == 3:
                k = 1
            side.putpixel((x, y), w[k])
    for i in range(2, 14):            # diagonal brace
        P(side, i, 15 - i, dark[2])
        P(side, i + 1, 15 - i, dark[1])
    frame(side)
    art.save(side, 'block/wooden_crate_side')
    front = side.copy()
    # stencilled anchor mark
    for (x, y) in ((7, 4), (8, 4), (7, 5), (8, 5), (6, 6), (7, 6), (8, 6), (9, 6), (7, 7), (8, 7), (7, 8), (8, 8),
                   (7, 9), (8, 9), (5, 10), (10, 10), (5, 11), (6, 11), (7, 11), (8, 11), (9, 11), (10, 11)):
        P(front, x, y, '#2c3a52')
    art.save(front, 'block/wooden_crate_front')
    top = art.img()
    for y in range(16):
        for x in range(16):
            k = 3 if (x // 4) % 2 else 4
            if x % 4 == 3:
                k = 1
            top.putpixel((x, y), w[k])
    frame(top)
    art.save(top, 'block/wooden_crate_top')
    ins = art.img()
    straw = art.pal('#8a6a2a', '#b08c3c', '#d2b05a', '#e8cc7a')
    rnd = random.Random(8)
    for y in range(16):
        for x in range(16):
            ins.putpixel((x, y), straw[rnd.choice([0, 1, 1, 2])])
    for _ in range(20):
        x, y = rnd.randrange(15), rnd.randrange(16)
        P(ins, x, y, straw[3])
        P(ins, x + 1, y, straw[2])
    art.save(ins, 'block/wooden_crate_inside')
    tx = {'side': 'block/wooden_crate_side', 'front': 'block/wooden_crate_front', 'top': 'block/wooden_crate_top',
          'inside': 'block/wooden_crate_inside'}
    m = Model(tx, ao=True, particle='block/wooden_crate_side')
    m.box((0, 0, 0), (16, 16, 16), 'side', faces={'north': 'front', 'up': 'top', 'down': 'top'}, cull=True)
    m.write('wooden_crate')
    m = Model(tx, ao=True, particle='block/wooden_crate_side')
    m.box((0, 0, 0), (16, 16, 16), 'side', faces={'north': 'front', 'down': 'top'}, skip=('up',), cull=True)
    m.box((1, 13, 1), (15, 13, 15), 'inside', only=('up',), collide=False)
    for (a, b) in (((1, 13, 1), (15, 16, 1.01)), ((1, 13, 14.99), (15, 16, 15)), ((1, 13, 1), (1.01, 16, 15)),
                   ((14.99, 13, 1), (15, 16, 15))):
        m.box(a, b, 'side', uv={f: [1, 0, 15, 3] for f in ('north', 'south', 'west', 'east')}, skip=('up', 'down'),
              collide=False)
    # rim of the walls
    for (a, b) in (((0, 15.99, 0), (16, 16, 1)), ((0, 15.99, 15), (16, 16, 16)), ((0, 15.99, 1), (1, 16, 15)),
                   ((15, 15.99, 1), (16, 16, 15))):
        m.box(a, b, 'top', only=('up',), collide=False)
    # lid pushed aside, resting askew on the rim
    m.box((2, 16, 5), (18, 17.5, 21), 'top', rot=('y', 22.5, (10, 16, 13)), collide=False)
    m.write('wooden_crate_open')
    shape('wooden_crate', [(0, 0, 0, 16, 16, 16)])
    shape('wooden_crate_open', [(0, 0, 0, 16, 16, 16)])
    v = {}
    for o in ('false', 'true'):
        v.update(facing_variants('block/wooden_crate' + ('_open' if o == 'true' else ''), props=f'open={o}'))
    A.blockstate(M, 'wooden_crate', v)
    A.block_item(M, 'wooden_crate')


# ================================================================================================= ship
def ship_bell():
    m = Model({'bell': 'block/decor_brass', 'iron': 'block/decor_iron', 'rope': 'block/decor_rope_vertical'},
              particle='block/decor_brass')
    m.box((6.5, 9, 15), (9.5, 16, 16), 'iron')
    m.box((7.25, 14, 7), (8.75, 15.5, 15), 'iron')
    m.box((7.5, 12.5, 13), (8.5, 14, 15), 'iron', collide=False)
    m.box((7.5, 11, 14), (8.5, 12.5, 15), 'iron', collide=False)
    m.box((6.5, 13, 6.5), (9.5, 14, 9.5), 'iron')
    m.box((6, 11, 6), (10, 13, 10), 'bell')
    m.box((5.5, 7, 5.5), (10.5, 11, 10.5), 'bell')
    m.box((4.5, 5, 4.5), (11.5, 7, 11.5), 'bell')
    m.box((7.5, 3.5, 7.5), (8.5, 5, 8.5), 'iron', collide=False)
    m.box((7.6, 0.5, 7.6), (8.4, 3.5, 8.4), 'rope', collide=False)
    m.box((7.25, 0, 7.25), (8.75, 1, 8.75), 'rope', collide=False)
    m.write('ship_bell')
    shape('ship_bell', [(4.5, 3.5, 4.5, 11.5, 14, 11.5), (7, 14, 6.5, 9, 15.5, 16), (6.5, 9, 15, 9.5, 16, 16)])
    A.blockstate(M, 'ship_bell', facing_variants('block/ship_bell'))
    small_item('ship_bell', 'block/ship_bell', 1.0, 0.5)


def chart_table():
    p = art.PARCHMENT
    im = art.img()
    sea = tex.ramp('#4a86b8', 4, 0.3)
    nz = tex.value_noise(16, 16, 77, 5, 2)
    for y in range(16):
        for x in range(16):
            v = nz[y][x]
            if v > 0.62:
                c = '#5f9a4a' if v > 0.72 else '#d8c48a'
            else:
                c = sea[2] if (x + y) % 9 else sea[1]
            im.putpixel((x, y), tex.mix(c, p[4], 0.35))
    for i in range(16):
        P(im, i, 0, p[1])
        P(im, i, 15, p[1])
        P(im, 0, i, p[1])
        P(im, 15, i, p[1])
    for k, (x, y) in enumerate(((2, 12), (3, 11), (5, 11), (6, 10), (8, 9), (9, 7), (11, 6))):
        P(im, x, y, '#a8282a')
    for (x, y) in ((11, 5), (13, 7), (12, 6), (11, 7), (13, 5)):   # X marks the spot
        P(im, x, y, '#7a1012')
    for (x, y) in ((3, 3), (3, 2), (3, 4), (2, 3), (4, 3)):         # compass rose
        P(im, x, y, '#2a2a3a')
    P(im, 3, 1, '#c8302a')
    art.save(im, 'block/chart_table_chart')
    m = Model({'wood': 'block/decor_teak', 'chart': 'block/chart_table_chart', 'brass': 'block/decor_brass',
               'legs': 'block/decor_teak_vertical', 'iron': 'block/decor_iron'}, particle='block/decor_teak')
    m.box((0, 13, 0), (16, 16, 16), 'wood', cull=True)
    for x, z in ((1, 1), (12.5, 1), (1, 12.5), (12.5, 12.5)):
        m.box((x, 0, z), (x + 2.5, 13, z + 2.5), 'legs')
    m.box((3.5, 3, 1.75), (12.5, 4, 2.75), 'wood', collide=False)
    m.box((3.5, 3, 13.25), (12.5, 4, 14.25), 'wood', collide=False)
    m.box((1.5, 16, 2), (14.5, 16.05, 14), 'chart', faces={'up': ('chart', [0, 0, 16, 16], 90)}, only=('up',), collide=False)
    m.box((11.5, 16, 2.5), (13.5, 17.75, 4.5), 'iron', collide=False)          # ink bottle
    m.box((12, 17.75, 3), (13, 18.25, 4), 'brass', collide=False)
    m.box((4, 16, 9), (5.5, 16.6, 13.5), 'brass', rot=('y', 22.5, (4.75, 16, 11)), collide=False)  # dividers
    m.box((5.5, 16, 9.5), (6, 16.4, 13.5), 'brass', rot=('y', -22.5, (5.75, 16, 11)), collide=False)
    m.write('chart_table')
    shape('chart_table', [(0, 13, 0, 16, 16, 16), (1, 0, 1, 3.5, 13, 3.5), (12.5, 0, 1, 15, 13, 3.5),
                          (1, 0, 12.5, 3.5, 13, 15), (12.5, 0, 12.5, 15, 13, 15)])
    A.blockstate(M, 'chart_table', facing_variants('block/chart_table'))
    A.block_item(M, 'chart_table')


def spyglass_stand():
    m = Model({'brass': 'block/decor_brass', 'wood': 'block/decor_teak_vertical', 'lens': 'block/decor_glass',
               'leather': 'block/decor_lantern_cap'}, particle='block/decor_brass')
    # tripod legs hinge at the hub (8, 10.5, 8)
    hub = (8, 10.5, 8)
    m.box((7.4, 0.5, 7.4), (8.6, 10.5, 8.6), 'wood', rot={'x': 24, 'y': 0, 'z': 0, 'origin': hub}, collide=False)
    m.box((7.4, 0.5, 7.4), (8.6, 10.5, 8.6), 'wood', rot={'x': -12, 'y': 0, 'z': 21, 'origin': hub}, collide=False)
    m.box((7.4, 0.5, 7.4), (8.6, 10.5, 8.6), 'wood', rot={'x': -12, 'y': 0, 'z': -21, 'origin': hub}, collide=False)
    m.box((7, 10, 7), (9, 11.5, 9), 'brass')
    # spyglass, tilted up to the front
    tilt = {'x': 14, 'y': 0, 'z': 0, 'origin': (8, 12.25, 8)}
    m.box((7.25, 11.5, 4), (8.75, 13, 13), 'brass', rot=tilt, collide=False)
    m.box((7.4, 11.65, 9), (8.6, 12.85, 11.5), 'leather', rot=tilt, collide=False)
    m.box((6.75, 11, 2), (9.25, 13.5, 4.5), 'brass', faces={'north': ('lens', [0, 0, 3, 3])}, rot=tilt, collide=False)
    m.box((7.5, 11.75, 13), (8.5, 12.75, 15), 'brass', rot=tilt, collide=False)
    m.write('spyglass_stand')
    shape('spyglass_stand', [(3, 0, 3, 13, 10, 13), (6.5, 10, 2, 9.5, 15, 15)])
    A.blockstate(M, 'spyglass_stand', facing_variants('block/spyglass_stand'))
    small_item('spyglass_stand', 'block/spyglass_stand', 0.9, 0.0)


def figurehead():
    skin = tex.ramp('#e8c39a', 5, 0.4)
    hair = tex.ramp('#d8a03a', 5, 0.5)
    scale = tex.ramp('#2f9a8f', 5, 0.5)
    im = art.img()
    for y in range(16):
        for x in range(16):
            im.putpixel((x, y), skin[2] if (x + 2 * y) % 9 else skin[1])
    art.save(im, 'block/figurehead_skin')
    face = im.copy()
    fill(face, skin[2], (0, 0, 15, 15))
    for (x, y) in ((5, 6), (10, 6)):
        P(face, x, y, '#2a4a5a')
    P(face, 5, 5, skin[0])
    P(face, 10, 5, skin[0])
    for x in (7, 8):
        P(face, x, 8, skin[1])
    for x in (6, 7, 8, 9):
        P(face, x, 11, '#b8605a')
    art.save(face, 'block/figurehead_face')
    hm = art.img()
    for y in range(16):
        for x in range(16):
            k = 2 + (1 if (x + y // 2) % 4 == 0 else 0) - (1 if x % 4 == 2 else 0)
            hm.putpixel((x, y), hair[k])
    art.save(hm, 'block/figurehead_hair')
    sc = art.img()
    for y in range(16):
        for x in range(16):
            k = 2
            if (y % 3 == 0) and ((x + (y // 3) * 2) % 4 == 0):
                k = 4
            elif (y % 3 == 1) and ((x + (y // 3) * 2) % 4 in (3, 1)):
                k = 1
            sc.putpixel((x, y), scale[k])
    art.save(sc, 'block/figurehead_scales')
    m = Model({'board': 'block/decor_teak_vertical', 'skin': 'block/figurehead_skin', 'face': 'block/figurehead_face',
               'hair': 'block/figurehead_hair', 'tail': 'block/figurehead_scales', 'gold': 'block/decor_brass'},
              particle='block/decor_teak')
    m.box((5, 0, 14.5), (11, 15, 16), 'board')                     # ends under the gold cap (no coplanar faces)
    m.box((4.5, 15, 14), (11.5, 16, 16), 'gold', collide=False)
    # tail curling down with a fin
    m.box((6.5, 2, 11), (9.5, 7, 14.5), 'tail')
    m.box((7, 0.5, 12), (9, 2, 14.5), 'tail')
    m.box((4.5, 0, 12.5), (11.5, 1.5, 14.5), 'tail', collide=False)
    # torso leans forward out of the bow
    lean = {'x': -18, 'y': 0, 'z': 0, 'origin': (8, 7, 14)}
    m.box((6, 7, 10.5), (10, 11.5, 14), 'skin', rot=lean, collide=False)
    m.box((5.5, 8.5, 9.75), (10.5, 10, 11.25), 'skin', rot=lean, collide=False)   # crossed arms
    m.box((6.5, 11.5, 9.5), (9.5, 15, 12.5), 'skin', faces={'north': ('face', [4, 3, 12, 13])}, rot=lean, collide=False)
    m.box((6, 12.5, 11.5), (10, 15.5, 14.5), 'hair', rot=lean, collide=False)
    m.box((6.25, 8, 12.5), (9.75, 12.5, 14.5), 'hair', rot=lean, collide=False)
    m.box((6.25, 14.75, 9.25), (9.75, 15.75, 12.5), 'hair', rot=lean, collide=False)
    m.write('figurehead')
    shape('figurehead', [(4.5, 0, 9, 11.5, 16, 16)])
    A.blockstate(M, 'figurehead', facing_variants('block/figurehead'))
    small_item('figurehead', 'block/figurehead', 0.95, 0.0)


def life_ring():
    im = art.img()
    red = tex.ramp('#d03a2e', 4, 0.35)
    wh = art.WHITE
    for y in range(16):
        for x in range(16):
            seg = (x // 4 + y // 4) % 2
            im.putpixel((x, y), red[2] if seg else wh[3])
            if (x + y) % 8 == 0:
                im.putpixel((x, y), red[1] if seg else wh[2])
    art.save(im, 'block/life_ring')
    redt = art.img()
    fill(redt, red[2])
    for y in range(16):
        P(redt, 0, y, red[3])
        P(redt, 15, y, red[1])
    art.save(redt, 'block/life_ring_red')
    wht = art.img()
    fill(wht, wh[3])
    for y in range(16):
        P(wht, 0, y, wh[4])
        P(wht, 15, y, wh[2])
    art.save(wht, 'block/life_ring_white')
    m = Model({'red': 'block/life_ring_red', 'white': 'block/life_ring_white', 'rope': 'block/decor_rope',
               'iron': 'block/decor_iron'}, particle='block/life_ring_red')
    z = (13, 16)
    m.box((5.5, 11.5, z[0]), (10.5, 14.5, z[1]), 'red')
    m.box((5.5, 1.5, z[0]), (10.5, 4.5, z[1]), 'red')
    m.box((1.5, 5.5, z[0]), (4.5, 10.5, z[1]), 'red')
    m.box((11.5, 5.5, z[0]), (14.5, 10.5, z[1]), 'red')
    for (cx, cy) in ((4, 12), (12, 12), (4, 4), (12, 4)):
        m.box((cx - 2.1, cy - 1.5, z[0] + 0.1), (cx + 2.1, cy + 1.5, z[1] - 0.1), 'white',
              rot=('z', 45 if (cx - 8) * (cy - 8) < 0 else -45, (cx, cy, 14.5)), collide=False)
    # rope loops between the stripes
    for (a, b) in (((5, 14.2, 12.8), (11, 14.8, 13.2)), ((5, 1.2, 12.8), (11, 1.8, 13.2)),
                   ((1.2, 5, 12.8), (1.8, 11, 13.2)), ((14.2, 5, 12.8), (14.8, 11, 13.2))):
        m.box(a, b, 'rope', collide=False)
    m.box((7.5, 14.5, 15), (8.5, 15.5, 16), 'iron', collide=False)
    m.write('life_ring')
    shape('life_ring', [(1.5, 1.5, 13, 14.5, 14.5, 16)])
    A.blockstate(M, 'life_ring', facing_variants('block/life_ring'))
    sp = art.img()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if 3.2 < d < 7.2:
                ang = math.atan2(y - 7.5, x - 7.5)
                seg = int((ang + math.pi) / (math.pi / 4)) % 2
                c = red[2] if seg else wh[3]
                if d > 6.2:
                    c = red[1] if seg else wh[2]
                if d < 4.2:
                    c = red[1] if seg else wh[1]
                sp.putpixel((x, y), tex.rgb(c))
    art.save(art.sprite_finish(sp), 'item/life_ring')
    A.flat_item(M, 'life_ring')


def build():
    ship_cannon()
    cannonball_and_coin()
    ship_anchor()
    treasure_chest()
    gold_coin_pile()
    jolly_roger()
    wanted_poster()
    rum_bottles()
    rope_railing()
    cargo_net()
    powder_keg()
    wooden_crate()
    ship_bell()
    chart_table()
    spyglass_stand()
    figurehead()
    life_ring()
