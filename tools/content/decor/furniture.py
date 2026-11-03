"""Furniture and home decor: hammock, deck chair, umbrellas, cushions, rug, tables, chairs, shelf, wind chime,
aquarium and hanging planter."""
import math
import random

from bclib import assets as A
from bclib import tex

from content.decor import art
from content.decor.art import P, fill
from content.decor.lights import small_item
from content.decor.mdl import Model, M, rid, facing_variants, shape

DRIFT_PLANKS = 'raft:block/driftwood_planks'
PALM_PLANKS = 'raft:block/palm_planks'
DRIFT_LOG = 'raft:block/stripped_driftwood_log'
PALM_LOG = 'raft:block/stripped_palm_log'


# ================================================================================================= hammock
def hammock():
    fab = art.img()
    a = tex.ramp('#e8dcc0', 4, 0.3)
    b = tex.ramp('#c8463a', 4, 0.35)
    c = tex.ramp('#3a8aa8', 4, 0.35)
    for y in range(16):
        for x in range(16):
            band = [a, a, b, a, c, a, a, b][(x // 2) % 8]
            k = 2 if (x + y) % 3 else 1
            fab.putpixel((x, y), band[k])
    for x in range(16):
        P(fab, x, 0, a[0])
    art.save(fab, 'block/hammock_fabric')
    for part in ('foot', 'head'):
        m = Model({'fabric': 'block/hammock_fabric', 'wood': 'block/decor_palm', 'rope': 'block/decor_rope'},
                  particle='block/hammock_fabric')
        far = part == 'foot'        # foot: the far end (with the frame) is south (z=16); head: north (z=0)
        def Z(z0, z1):
            return (z0, z1) if far else (16 - z1, 16 - z0)
        za, zb = Z(13.5, 15.5)
        m.box((0.5, 0, za), (2.5, 14, zb), 'wood')
        m.box((13.5, 0, za), (15.5, 14, zb), 'wood')
        m.box((0.5, 12, za), (15.5, 14, zb), 'wood')
        za, zb = Z(12, 13)
        m.box((2, 8.5, za), (14, 9.5, zb), 'wood', collide=False)
        for x in (2.5, 13):
            m.box((x, 9.5, za + 0.25), (x + 0.5, 12, zb - 0.25), 'rope', collide=False)
        m.box((7.75, 9.5, za + 0.25), (8.25, 12, zb - 0.25), 'rope', collide=False)
        if far:
            m.box((2, 5, 0), (14, 5.6, 12.5), 'fabric', rot=('x', -15, (8, 5.3, 0)), collide=False,
                  uv={'up': [2, 0, 14, 12.5], 'down': [2, 0, 14, 12.5]})
        else:
            m.box((2, 5, 3.5), (14, 5.6, 16), 'fabric', rot=('x', 15, (8, 5.3, 16)), collide=False,
                  uv={'up': [2, 3.5, 14, 16], 'down': [2, 3.5, 14, 16]})
        # pillow on the head end
        if not far:
            m.box((4, 7.5, 3.5), (12, 9.5, 6.5), 'fabric', rot=('x', 15, (8, 5.3, 16)), collide=False,
                  uv={'up': [0, 0, 8, 3]})
        m.write(f'hammock_{part}')
        zf = (0, 13) if far else (3, 16)
        za, zb = Z(13.5, 15.5)
        shape(f'hammock_{part}', [(1, 4, zf[0], 15, 9, zf[1]), (0.5, 0, za, 15.5, 14, zb)])
    v = {}
    for f, r in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
        for part in ('foot', 'head'):
            e = {'model': rid(f'block/hammock_{part}')}
            if r:
                e['y'] = r
            v[f'facing={f},part={part}'] = e
    A.blockstate(M, 'hammock', v)
    sp = art.img()
    w = art.PALM
    for y in range(4, 15):
        P(sp, 1, y, w[3])
        P(sp, 14, y, w[2])
    for x in range(1, 15):
        P(sp, x, 4, w[4])
    for x in range(2, 14):
        y = 7 + round(3 * math.sin(math.pi * (x - 2) / 11))
        for d in range(3):
            P(sp, x, y + d - 1, [a, b, a, c][(x // 2) % 4][2 - (d == 2)])
    for x in (2, 13):
        P(sp, x, 5, art.ROPE[3])
        P(sp, x, 6, art.ROPE[3])
    art.save(art.sprite_finish(sp), 'item/hammock')
    A.flat_item(M, 'hammock')


# ================================================================================================= deck chair
def deck_chair():
    art.save(art.stripes('#3a72c8', '#efece2', 2, vertical=True), 'block/deck_chair_canvas')
    m = Model({'wood': 'block/decor_palm', 'canvas': 'block/deck_chair_canvas'}, particle='block/deck_chair_canvas')
    for x0 in (1, 13.5):
        m.box((x0, 0, 1), (x0 + 1.5, 6, 2.5), 'wood')                         # front legs
        m.box((x0, 4, 1), (x0 + 1.5, 5.5, 10.5), 'wood', collide=False)        # seat rails
        m.box((x0, 4, 9.5), (x0 + 1.5, 5.5, 20), 'wood', rot=('x', -50, (x0, 4.75, 9.5)), collide=False)
        m.box((x0, 0, 12.5), (x0 + 1.5, 7, 14), 'wood', collide=False)        # rear legs
        ax = x0 - 0.5 if x0 < 8 else x0
        m.box((ax, 7.5, 2), (ax + 2, 8.5, 10), 'wood', collide=False)         # arm rests
        m.box((x0, 5.5, 2), (x0 + 1.5, 7.5, 3), 'wood', collide=False)
    m.box((2.5, 3.5, 1.5), (13.5, 4.25, 10), 'canvas', uv={'up': [2.5, 0, 13.5, 8.5], 'down': [2.5, 0, 13.5, 8.5]}, collide=False)
    m.box((2.5, 4, 9.5), (13.5, 4.75, 19.5), 'canvas', rot=('x', -50, (8, 4.4, 9.5)), collide=False,
          uv={'up': [2.5, 6, 13.5, 16], 'down': [2.5, 6, 13.5, 16]})
    m.box((2.5, 3.6, 1.2), (13.5, 4.6, 2.2), 'wood', collide=False)
    m.write('deck_chair')
    shape('deck_chair', [(1, 0, 1, 15, 8.5, 10), (1, 0, 9.5, 15, 12.5, 16)])
    A.blockstate(M, 'deck_chair', facing_variants('block/deck_chair'))
    A.block_item(M, 'deck_chair')


# ================================================================================================= umbrellas
def beach_umbrellas():
    for col, c in (('red', '#d83a30'), ('blue', '#3a6fd0'), ('yellow', '#f0c83a')):
        name = f'beach_umbrella_{col}'
        r1 = tex.ramp(c, 5, 0.35)
        r2 = art.WHITE
        can = art.img(32, 32)
        for y in range(32):
            for x in range(32):
                ang = math.atan2(y - 15.5, x - 15.5)
                seg = int((ang + math.pi) / (math.pi / 4)) % 2
                d = max(abs(x - 15.5), abs(y - 15.5)) / 16
                rr = r1 if seg else r2
                k = 3 if d < 0.5 else 2 if d < 0.85 else 1
                if (x + y) % 9 == 0:
                    k = max(0, k - 1)
                can.putpixel((x, y), rr[k + (1 if rr is r2 else 0)])
        art.save(can, f'block/{name}')
        val = art.img(32, 32)
        for x in range(32):
            seg = (x // 4) % 2
            rr = r1 if seg else r2
            depth = 3 if x % 4 in (1, 2) else 2
            for y in range(depth):
                val.putpixel((x, y), rr[2 + (1 if rr is r2 else 0)] if y < depth - 1 else rr[1])
        art.save(val, f'block/{name}_valance')
        tx = {'canopy': f'block/{name}', 'valance': f'block/{name}_valance', 'pole': 'block/decor_palm',
              'iron': 'block/decor_iron'}
        m = Model(tx, particle=f'block/{name}')
        m.box((7.25, 0, 7.25), (8.75, 16, 8.75), 'pole')
        m.box((5, 0, 5), (11, 1.5, 11), 'iron')
        m.write(f'{name}_lower')
        m = Model(tx, particle=f'block/{name}')
        m.box((7.25, 0, 7.25), (8.75, 9, 8.75), 'pole')
        full = [0, 0, 16, 16]
        edge = {f: ('canopy', [0, 0, 16, 0.5]) for f in ('north', 'south', 'west', 'east')}
        m.box((-8, 9, -8), (24, 10, 24), 'canopy', faces=dict(edge, up=('canopy', full), down=('canopy', full)), collide=False)
        m.box((-4, 10, -4), (20, 11.5, 20), 'canopy', faces=dict(edge, up=('canopy', [2, 2, 14, 14])), skip=('down',), collide=False)
        m.box((1, 11.5, 1), (15, 13, 15), 'canopy', faces=dict(edge, up=('canopy', [4.5, 4.5, 11.5, 11.5])), skip=('down',), collide=False)
        m.box((5, 13, 5), (11, 14, 11), 'canopy', faces=dict(edge, up=('canopy', [6.5, 6.5, 9.5, 9.5])), skip=('down',), collide=False)
        m.box((7.5, 14, 7.5), (8.5, 15.5, 8.5), 'pole', collide=False)
        vu = [0, 0, 16, 1.5]
        m.plane_z(-8, 7.5, 24, 9, -8, 'valance', uv=vu)
        m.plane_z(-8, 7.5, 24, 9, 24, 'valance', uv=vu)
        m.plane_x(-8, 7.5, 24, 9, -8, 'valance', uv=vu)
        m.plane_x(-8, 7.5, 24, 9, 24, 'valance', uv=vu)
        m.write(f'{name}_upper')
        shape(f'{name}_lower', [(5, 0, 5, 11, 1.5, 11), (7.25, 0, 7.25, 8.75, 16, 8.75)])
        shape(f'{name}_upper', [(7.25, 0, 7.25, 8.75, 9, 8.75), (-8, 7.5, -8, 24, 14, 24)])
        shape(f'{name}_upper_collision', [(7.25, 0, 7.25, 8.75, 9, 8.75)])
        A.blockstate(M, name, {'half=lower': {'model': rid(f'block/{name}_lower')},
                               'half=upper': {'model': rid(f'block/{name}_upper')}})
        sp = art.img()
        for x in range(1, 15):
            h = 5 - abs(x - 7.5) * 0.55
            for y in range(int(6 - h), 7):
                seg = (x // 2) % 2
                rr = r1 if seg else r2
                sp.putpixel((x, y), tex.rgb(rr[3 if y < 4 else 2]))
        for x in range(1, 15):
            P(sp, x, 7, r1[1] if (x // 2) % 2 else r2[2])
        for y in range(8, 16):
            P(sp, 7, y, art.PALM[3])
            P(sp, 8, y, art.PALM[2])
        art.save(art.sprite_finish(sp), f'item/{name}')
        A.flat_item(M, name)


# ================================================================================================= soft furnishing
CUSHIONS = ('white', 'red', 'blue', 'yellow', 'green')


def cushions():
    for col in CUSHIONS:
        name = f'cushion_{col}'
        r = art.dye_ramp(col, 6, 0.5)
        top = art.img()
        for y in range(16):
            for x in range(16):
                d = min(x, y, 15 - x, 15 - y)
                k = 3 if d > 1 else 2
                if d > 3 and ((x - 7.5) ** 2 + (y - 7.5) ** 2) < 30:
                    k = 4
                top.putpixel((x, y), r[k])
        for (x, y) in ((4, 4), (11, 4), (4, 11), (11, 11), (7, 7)):
            P(top, x, y, r[1])
            P(top, x + 1, y + 1, r[2])
        art.save(top, f'block/{name}_top')
        side = art.img()
        for y in range(16):
            for x in range(16):
                k = 3
                if y % 16 in (12,):
                    k = 1
                if y % 16 in (11,):
                    k = 4
                side.putpixel((x, y), r[k])
        art.save(side, f'block/{name}_side')
        m = Model({'top': f'block/{name}_top', 'side': f'block/{name}_side', 'tassel': 'block/decor_brass'},
                  particle=f'block/{name}_top')
        m.box((1, 0, 1), (15, 3, 15), 'side', faces={'down': 'top'}, skip=('up',),
              uv={f: [1, 9, 15, 12] for f in ('north', 'south', 'west', 'east')})
        m.box((1.5, 3, 1.5), (14.5, 4, 14.5), 'side', faces={'up': ('top', [1.5, 1.5, 14.5, 14.5])},
              uv={f: [1.5, 8, 14.5, 9] for f in ('north', 'south', 'west', 'east')})
        m.box((1, 2.9, 1), (15, 3, 15), 'top', only=('up',), collide=False)
        m.box((7.5, 4, 7.5), (8.5, 4.3, 8.5), 'side', faces={'up': ('top', [7, 7, 8, 8])}, collide=False)
        for (x, z) in ((0.5, 0.5), (14.5, 0.5), (0.5, 14.5), (14.5, 14.5)):
            m.box((x, 0, z), (x + 1, 1.5, z + 1), 'tassel', collide=False)
        m.write(name)
        shape(name, [(1, 0, 1, 15, 4, 15)])
        A.blockstate(M, name, {'': {'model': rid(f'block/{name}')}})
        small_item(name, f'block/{name}', 0.8, 2.5)


def woven_rug():
    im = art.img()
    nat = tex.ramp('#c8a868', 5, 0.35)
    red = tex.ramp('#b8443a', 4, 0.3)
    teal = tex.ramp('#2f8a86', 4, 0.3)
    for y in range(16):
        for x in range(16):
            band = None
            if y in (2, 3, 12, 13):
                band = red
            elif y in (7, 8):
                band = teal
            k = 2 if (x + (y % 2)) % 2 else 3
            if x % 4 == 0:
                k -= 1
            im.putpixel((x, y), (band or nat)[min(k, len(band or nat) - 1)])
    for x in range(0, 16, 4):          # diamond pattern on the teal band
        P(im, x + 2, 7, nat[4])
        P(im, x + 1, 8, nat[3])
        P(im, x + 3, 8, nat[3])
    art.save(im, 'block/woven_rug')
    A.block_model(M, 'woven_rug', 'minecraft:block/carpet', {'wool': 'block/woven_rug'})
    A.blockstate(M, 'woven_rug', {'': {'model': rid('block/woven_rug')}})
    A.block_item(M, 'woven_rug')


# ================================================================================================= tables and chairs
def tables_and_chairs():
    for wood, planks, log, seat in (('driftwood', DRIFT_PLANKS, DRIFT_LOG, DRIFT_PLANKS),
                                    ('palm', PALM_PLANKS, PALM_LOG, 'block/palm_chair_weave')):
        if wood == 'palm':
            wk = art.img()
            w = art.WICKER
            for y in range(16):
                for x in range(16):
                    k = 3 if ((x // 2) + (y // 2)) % 2 else 2
                    if (x + y) % 4 == 0:
                        k -= 1
                    wk.putpixel((x, y), w[k])
            art.save(wk, 'block/palm_chair_weave')
        tx = {'top': planks, 'leg': log, 'seat': seat}
        m = Model(tx, ao=True, particle=planks)
        m.box((0, 14, 0), (16, 16, 16), 'top', cull=True)
        m.box((1, 12, 1), (15, 14, 2), 'top', collide=False)
        m.box((1, 12, 14), (15, 14, 15), 'top', collide=False)
        m.box((1, 12, 2), (2, 14, 14), 'top', collide=False)
        m.box((14, 12, 2), (15, 14, 14), 'top', collide=False)
        for x, z in ((1.5, 1.5), (12.5, 1.5), (1.5, 12.5), (12.5, 12.5)):
            m.box((x, 0, z), (x + 2, 12, z + 2), 'leg')
        m.write(f'{wood}_table')
        shape(f'{wood}_table', [(0, 14, 0, 16, 16, 16), (1, 12, 1, 15, 14, 15), (1.5, 0, 1.5, 3.5, 12, 3.5),
                                (12.5, 0, 1.5, 14.5, 12, 3.5), (1.5, 0, 12.5, 3.5, 12, 14.5), (12.5, 0, 12.5, 14.5, 12, 14.5)])
        A.blockstate(M, f'{wood}_table', {'': {'model': rid(f'block/{wood}_table')}})
        A.block_item(M, f'{wood}_table')

        m = Model(tx, ao=True, particle=planks)
        m.box((2, 8, 2), (14, 10, 14), 'top', faces={'up': 'seat'})
        for x in (2.5, 12):
            m.box((x, 0, 2.5), (x + 1.5, 8, 4), 'leg')
            m.box((x, 0, 12), (x + 1.5, 20, 13.5), 'leg')
        m.box((4, 3, 2.75), (12, 4, 3.75), 'leg', collide=False)
        m.box((2.75, 3, 4), (3.75, 4, 12), 'leg', collide=False)
        m.box((12.25, 3, 4), (13.25, 4, 12), 'leg', collide=False)
        m.box((4, 12, 12.25), (12, 13.5, 13.25), 'top', collide=False)
        m.box((4, 16.5, 12.25), (12, 19, 13.25), 'top', collide=False)
        m.box((7.25, 13.5, 12.4), (8.75, 16.5, 13.1), 'top', collide=False)
        m.write(f'{wood}_chair')
        shape(f'{wood}_chair', [(2, 0, 2, 14, 10, 14), (2.5, 10, 12, 13.5, 20, 13.5)])
        A.blockstate(M, f'{wood}_chair', facing_variants(f'block/{wood}_chair'))
        A.block_item(M, f'{wood}_chair')


def driftwood_shelf():
    jar = art.img()
    sg = art.SEA_GLASS
    for y in range(16):
        for x in range(16):
            k = 3 if x % 5 else 4
            jar.putpixel((x, y), sg[k][:3] + (200,))
    art.save(jar, 'block/driftwood_shelf_jar')
    m = Model({'plank': DRIFT_PLANKS, 'wood': 'block/decor_driftwood', 'jar': 'block/driftwood_shelf_jar',
               'cork': 'block/tiki_torch_head', 'shell': 'block/seashell_lamp_shell', 'bottle': 'block/rum_bottles',
               'glow': 'block/jelly_lamp_jelly'}, particle=DRIFT_PLANKS)
    m.box((0, 7, 9), (16, 9, 16), 'plank')
    for x in (2, 12.5):
        m.box((x, 3, 14), (x + 1.5, 7, 16), 'wood')
        m.box((x, 5.5, 12), (x + 1.5, 7, 14), 'wood', collide=False)
    # jars and things on the shelf
    m.box((2.2, 9.5, 11.2), (4.8, 11.5, 13.8), 'glow', light=8, collide=False)
    m.box((1.5, 9, 10.5), (5.5, 13, 14.5), 'jar')
    m.box((2, 13, 11), (5, 14, 14), 'cork')
    m.box((6.5, 9, 11), (9, 12, 13.5), 'jar', collide=False)
    m.box((7, 12, 11.5), (8.5, 12.75, 13), 'cork', collide=False)
    m.box((7, 9.25, 11.5), (8.5, 10.5, 13), 'shell', collide=False)
    u = 5
    m.box((10.5, 9, 11), (12.5, 13, 13), 'bottle', faces={f: ('bottle', [u, 6, u + 2, 12]) for f in ('north', 'south', 'west', 'east')} | {'up': ('bottle', [u + 1, 0, u + 3, 2])})
    m.box((11, 13, 11.5), (12, 15, 12.5), 'bottle', faces={f: ('bottle', [u + 1, 0, u + 2, 2]) for f in ('north', 'south', 'west', 'east')}, skip=('down',), collide=False)
    m.box((13, 9, 10), (15, 10, 12.5), 'shell', collide=False)
    m.write('driftwood_shelf')
    shape('driftwood_shelf', [(0, 7, 9, 16, 9, 16), (1.5, 9, 10.5, 12.5, 14, 14.5), (2, 3, 14, 14, 7, 16)])
    A.blockstate(M, 'driftwood_shelf', facing_variants('block/driftwood_shelf'))
    A.block_item(M, 'driftwood_shelf')


def wind_chime():
    m = Model({'wood': 'block/decor_driftwood', 'string': 'block/decor_string', 'shell': 'block/seashell_lamp_shell',
               'glass': 'block/driftwood_shelf_jar', 'bamboo': 'block/tiki_torch_pole'}, particle='block/decor_driftwood')
    m.box((7.75, 14.5, 7.75), (8.25, 16, 8.25), 'string', collide=False)
    m.box((4.5, 13.5, 4.5), (11.5, 14.5, 11.5), 'wood')
    hang = [((5.5, 5.5), 8, 'bamboo'), ((10.5, 5.5), 7, 'shell'), ((5.5, 10.5), 6.5, 'shell'),
            ((10.5, 10.5), 8.5, 'bamboo'), ((8, 4.8), 7.5, 'glass')]
    for (x, z), yb, t in hang:
        m.box((x - 0.15, yb + 2.5, z - 0.15), (x + 0.15, 13.5, z + 0.15), 'string', collide=False)
        if t == 'bamboo':
            m.box((x - 0.6, yb - 1, z - 0.6), (x + 0.6, yb + 2.5, z + 0.6), 'bamboo', collide=False)
        elif t == 'shell':
            m.box((x - 1, yb, z - 0.4), (x + 1, yb + 2.5, z + 0.4), 'shell', collide=False)
        else:
            m.box((x - 0.8, yb, z - 0.3), (x + 0.8, yb + 2.5, z + 0.3), 'glass', collide=False)
    m.box((7.85, 5, 7.85), (8.15, 13.5, 8.15), 'string', collide=False)
    m.box((6.5, 6, 6.5), (9.5, 6.75, 9.5), 'wood', collide=False)
    m.box((7, 2, 7.6), (9, 5, 8.4), 'shell', collide=False)
    m.write('wind_chime')
    shape('wind_chime', [(4.5, 2, 4.5, 11.5, 16, 11.5)])
    A.blockstate(M, 'wind_chime', {'': {'model': rid('block/wind_chime')}})
    small_item('wind_chime', 'block/wind_chime', 1.0, 0.0)


def aquarium():
    # water + glass (translucent, blue tint, bright edge highlights)
    wg = art.img()
    for y in range(16):
        for x in range(16):
            a = 96 + (y * 3)
            c = tex.mix('#7ad0f0', '#2a78c0', y / 15)
            wg.putpixel((x, y), c[:3] + (min(170, a),))
    for i in range(3, 9):
        P(wg, i - 2, i, (230, 250, 255, 190))
    art.save(wg, 'block/aquarium_glass')
    # fish swimming (animated), transparent background
    frames = []
    fishes = [(2, 4, '#ff8a2a', '#fff2e0', 1), (9, 9, '#3ab8ff', '#ffe23a', -1), (5, 12, '#ffd23a', '#2a2a3a', 1)]
    for f in range(16):
        im = art.img()
        for (x0, y0, body, stripe, d) in fishes:
            x = (x0 + f * d) % 16
            pts = [(0, 0, body), (1, 0, stripe), (2, 0, body), (-1, -1, body), (-1, 1, body), (1, -1, body),
                   (1, 1, body), (3, 0, '#1a1a1a')] if d > 0 else \
                  [(0, 0, body), (-1, 0, stripe), (-2, 0, body), (1, -1, body), (1, 1, body), (-1, -1, body),
                   (-1, 1, body), (-3, 0, '#1a1a1a')]
            for (dx, dy, c) in pts:
                P(im, (x + dx) % 16, y0 + dy, c)
        frames.append(im)
    art.save_anim(frames, 'block/aquarium_fish', frametime=3)
    m = Model({'glass': 'block/aquarium_glass', 'frame': 'block/decor_teak', 'sand': 'minecraft:block/sand',
               'fish': 'block/aquarium_fish', 'grass': 'minecraft:block/seagrass', 'coral': 'minecraft:block/tube_coral_fan',
               'brass': 'block/decor_brass'}, particle='block/aquarium_glass')
    m.box((0, 0, 0), (16, 2, 16), 'frame', cull=True)
    for (a, b) in (((0, 15, 0), (16, 16, 1)), ((0, 15, 15), (16, 16, 16)), ((0, 15, 1), (1, 16, 15)), ((15, 15, 1), (16, 16, 15))):
        m.box(a, b, 'brass', cull=True, collide=False)
    for (x, z) in ((0, 0), (15, 0), (0, 15), (15, 15)):
        m.box((x, 2, z), (x + 1, 15, z + 1), 'brass', cull=True, collide=False)
    m.box((1, 2, 1), (15, 3.5, 15), 'sand', collide=False)
    m.cross(4, 11.5, 2.5, 3.5, 13, 'grass')
    m.cross(11.5, 4.5, 2, 3.5, 7.5, 'coral')
    m.plane_z(1.5, 5, 14.5, 13, 8, 'fish', uv=[1.5, 2, 14.5, 10])
    m.plane_x(1.5, 6, 14.5, 12, 6.5, 'fish', uv=[0, 6, 13, 12])
    m.box((0.5, 2, 0.5), (15.5, 15, 15.5), 'glass', light=6, faces={'down': None}, collide=False)
    m.write('aquarium')
    shape('aquarium', [(0, 0, 0, 16, 16, 16)])
    A.blockstate(M, 'aquarium', {'': {'model': rid('block/aquarium')}})
    A.block_item(M, 'aquarium')


def hanging_planter():
    fl = art.img()
    leaf = art.LEAF
    rnd = random.Random(12)
    for y in range(6, 16):
        for x in range(16):
            if rnd.random() < 0.55 and abs(x - 7.5) < 7.5 - (15 - y) * 0.2:
                fl.putpixel((x, y), tex.rgb(leaf[rnd.choice([1, 2, 3])]))
    hib = tex.ramp('#e8344a', 4, 0.4)
    pink = tex.ramp('#f08ac0', 4, 0.4)
    for (cx, cy, r) in ((4, 5, hib), (11, 4, pink), (8, 8, hib), (13, 9, hib), (2, 10, pink)):
        for (dx, dy) in ((0, 0), (1, 0), (0, 1), (-1, 0), (0, -1)):
            P(fl, cx + dx, cy + dy, r[3] if (dx, dy) != (0, 0) else '#f8e060')
        P(fl, cx + 1, cy + 1, r[1])
    art.save(fl, 'block/hanging_planter_flowers')
    vine = art.img()
    for x in (2, 6, 11, 14):
        ln = 6 + (x * 7) % 9
        for y in range(ln):
            P(vine, x + (1 if y % 5 == 3 else 0), y, leaf[2 if y % 3 else 3])
            if y % 3 == 1:
                P(vine, x - 1, y, leaf[1])
    art.save(vine, 'block/hanging_planter_vines')
    ropes = art.img()
    for y in range(16):
        t = y / 15
        for xa in (round(1 + 6.5 * (1 - t)), round(14 - 6.5 * (1 - t))):
            P(ropes, xa, 15 - y, art.ROPE[3])
    art.save(ropes, 'block/hanging_planter_ropes')
    m = Model({'basket': 'block/tiki_torch_head', 'soil': 'minecraft:block/rooted_dirt',
               'flowers': 'block/hanging_planter_flowers', 'vines': 'block/hanging_planter_vines',
               'ropes': 'block/hanging_planter_ropes', 'iron': 'block/decor_iron', 'rope': 'block/decor_rope'},
              particle='block/tiki_torch_head')
    m.box((5, 3, 5), (11, 4, 11), 'basket')
    m.box((4.5, 4, 4.5), (11.5, 7.5, 11.5), 'basket', skip=('up',))
    for (a, b) in (((4, 7.5, 4), (12, 8.5, 5)), ((4, 7.5, 11), (12, 8.5, 12)), ((4, 7.5, 5), (5, 8.5, 11)), ((11, 7.5, 5), (12, 8.5, 11))):
        m.box(a, b, 'rope', collide=False)
    m.box((5, 7, 5), (11, 7, 11), 'soil', only=('up',), collide=False)
    m.cross(8, 8, 4.5, 7, 13.5, 'flowers', uv=[0, 2, 16, 16])
    m.plane_z(3.5, -1, 12.5, 6, 4.4, 'vines', uv=[3, 0, 12, 7])
    m.plane_x(3.5, 0, 12.5, 6, 11.6, 'vines', uv=[2, 0, 11, 6])
    m.plane_z(4.5, 8, 11.5, 15.5, 8, 'ropes', uv=[1, 0, 15, 15])
    m.plane_x(4.5, 8, 11.5, 15.5, 8, 'ropes', uv=[1, 0, 15, 15])
    m.box((7.5, 15, 7.5), (8.5, 16, 8.5), 'iron', collide=False)
    m.write('hanging_planter')
    shape('hanging_planter', [(4, 0, 4, 12, 16, 12)])
    A.blockstate(M, 'hanging_planter', {'': {'model': rid('block/hanging_planter')}})
    small_item('hanging_planter', 'block/hanging_planter', 0.95, 0.0)


def build():
    hammock()
    deck_chair()
    beach_umbrellas()
    cushions()
    woven_rug()
    tables_and_chairs()
    driftwood_shelf()
    wind_chime()
    aquarium()
    hanging_planter()
