"""Lights of the decor module: garlands, lanterns, torches, braziers, buoys, lens, neon and lamps."""
import math
import random

from bclib import assets as A
from bclib import tex

from content.decor import art
from content.decor.art import P, fill
from content.decor.mdl import Model, M, rid, facing_variants, shape

SMALL_GUI = {'gui': {'rotation': [30, 225, 0], 'translation': [0, 1.5, 0], 'scale': [0.95, 0.95, 0.95]},
             'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.5, 0.5, 0.5]},
             'fixed': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [0.9, 0.9, 0.9]},
             'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.5, 0.5, 0.5]},
             'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 3, 0], 'scale': [0.5, 0.5, 0.5]},
             'firstperson_lefthand': {'rotation': [0, 225, 0], 'translation': [0, 3, 0], 'scale': [0.5, 0.5, 0.5]}}


def small_item(name, model, scale=0.95, lift=1.5):
    """Item definition using its own item model that inherits the block model with a bigger GUI view."""
    d = {k: dict(v) for k, v in SMALL_GUI.items()}
    d['gui'] = {'rotation': [30, 225, 0], 'translation': [0, lift, 0], 'scale': [scale, scale, scale]}
    A.item_model(M, name, parent=rid(model), display=d)
    A.item_def(M, name)


# ================================================================================================= shared textures
def shared():
    art.save(art.metal(art.IRON, 11), 'block/decor_iron')
    art.save(art.metal(art.BRASS[1:], 12, scratches=3), 'block/decor_brass')
    art.save(art.rope(art.ROPE), 'block/decor_rope')
    art.save(art.rope(art.ROPE, False), 'block/decor_rope_vertical')
    art.save(art.wood(art.TEAK, 13), 'block/decor_teak')
    art.save(art.wood(art.TEAK, 14, vertical=True), 'block/decor_teak_vertical')
    art.save(art.wood(art.DRIFT, 15), 'block/decor_driftwood')
    art.save(art.wood(art.PALM, 16), 'block/decor_palm')
    art.save(art.cloth(art.CANVAS, 17), 'block/decor_canvas')
    # dark bamboo / lacquer for lantern caps
    cap = art.wood(art.pal('#2a1a10', '#3a2416', '#4c301c', '#5e3d24', '#704b2d'), 18)
    art.save(cap, 'block/decor_lantern_cap')
    # thin string (2 px column used by models), dark twine
    s = art.img()
    tw = art.pal('#3b2c1e', '#54402b', '#6c5539')
    for y in range(16):
        for x in range(16):
            s.putpixel((x, y), tw[(x + y) % 3])
    art.save(s, 'block/decor_string')
    # translucent lantern glass with frame highlights
    g = art.img()
    for y in range(16):
        for x in range(16):
            a = 70 + (40 if (x + y) % 11 == 0 else 0)
            g.putpixel((x, y), (214, 236, 232, a))
    for k in range(4):
        P(g, 2 + k, 3 + k, (255, 255, 250, 150))
    art.save(g, 'block/decor_glass')


# ================================================================================================= garlands
def _catenary(x, top=2, sag=3):
    return top + round(sag * math.sin(math.pi * (x % 16) / 16.0))


def string_lights():
    # bulbs texture: columns of colours, animated twinkle
    cols = [('warm', '#ffd27a'), ('red', '#ff5a4a'), ('green', '#6ef06a'), ('blue', '#5ab4ff'),
            ('yellow', '#ffe85a'), ('pink', '#ff7ad8'), ('cyan', '#6af3ff'), ('orange', '#ff9a3a')]
    frames = []
    for f in range(4):
        im = art.img()
        for i, (_, c) in enumerate(cols):
            r = tex.ramp(c, 4, 0.45)
            on = f != (i * 3) % 4 or i == 0
            bright = r[3] if on else r[2]
            x0 = i * 2
            for y in range(16):
                P(im, x0, y, bright)
                P(im, x0 + 1, y, r[2] if on else r[1])
            P(im, x0, 0, tex.mix(bright, '#ffffff', 0.5))
        # socket (dark) in rows 14..15 of every column
        for x in range(16):
            P(im, x, 14, '#2d2a26')
            P(im, x, 15, '#3d3833')
        frames.append(im)
    art.save_anim(frames, 'block/string_lights_bulbs', frametime=10, interpolate=True)
    # wire plane texture (transparent, catenary)
    wire = art.img()
    for x in range(16):
        y = _catenary(x)
        P(wire, x, y, '#2b2a28')
        if x % 4 == 2:
            P(wire, x, y + 1, '#3a3834')
    # tiny nails at the ends
    P(wire, 0, 1, '#6b6760')
    P(wire, 15, 1, '#6b6760')
    art.save(wire, 'block/string_lights_wire')

    for name, palette in (('string_lights_warm', [0, 0, 0, 0]), ('string_lights_colorful', [1, 4, 2, 3])):
        m = Model({'wire': 'block/string_lights_wire', 'bulb': 'block/string_lights_bulbs'}, particle='block/string_lights_wire')
        m.plane_z(0, 8, 16, 16, 15.6, 'wire', uv=[0, 0, 16, 8], double=True)
        for i, bx in enumerate((2, 6, 10, 14)):
            wy = 16 - _catenary(bx) - 1   # model y of the wire at this x (texture row -> model y)
            ci = palette[i]
            u0 = ci * 2
            bulb = {'north': ('bulb', [u0, 1, u0 + 2, 4]), 'south': ('bulb', [u0, 1, u0 + 2, 4]),
                    'west': ('bulb', [u0, 1, u0 + 2, 4]), 'east': ('bulb', [u0, 1, u0 + 2, 4]),
                    'down': ('bulb', [u0, 0, u0 + 2, 1]), 'up': ('bulb', [u0, 14, u0 + 2, 16])}
            m.box((bx - 0.75, wy - 2.5, 14.6), (bx + 0.75, wy - 0.5, 15.9), 'bulb', faces=bulb, light=15, collide=False)
            sock = {f: ('bulb', [u0, 14, u0 + 2, 16]) for f in ('north', 'south', 'west', 'east', 'up')}
            m.box((bx - 0.5, wy - 0.5, 14.85), (bx + 0.5, wy + 0.5, 15.85), 'bulb', faces=sock, skip=('down',), collide=False)
        m.write(name)
        shape(name, [(0, 7, 14, 16, 16, 16)])
        A.blockstate(M, name, facing_variants(f'block/{name}'))
        # item sprite: wire + glowing bulbs
        it = art.img()
        for x in range(1, 15):
            P(it, x, _catenary(x, 4, 4), '#3a3632')
        for i, bx in enumerate((3, 7, 11)):
            c = cols[palette[i]][1]
            r = tex.ramp(c, 4, 0.5)
            y = _catenary(bx, 4, 4) + 1
            P(it, bx, y, '#3d3833')
            for dy in range(1, 4):
                P(it, bx, y + dy, r[3] if dy < 3 else r[2])
                P(it, bx + 1, y + dy, r[2] if dy < 3 else r[1])
            P(it, bx, y + 1, tex.mix(r[3], '#ffffff', 0.5))
        art.save(art.sprite_finish(it), f'item/{name}')
        A.flat_item(M, name)


def bunting():
    for name, colours in (('bunting_red_white', ['#c8352b', '#efece2', '#c8352b', '#efece2']),
                          ('bunting_rainbow', ['#d8443a', '#f2c23a', '#4fae4a', '#3d72d8'])):
        im = art.img()
        for x in range(16):
            P(im, x, _catenary(x, 1, 2), '#5d4a33')
        for i, c in enumerate(colours):
            r = tex.ramp(c, 5, 0.35)
            x0 = i * 4
            top = max(_catenary(x, 1, 2) for x in range(x0, x0 + 4)) + 1
            for row in range(0, 7):
                w = 4 - (row * 4) // 7          # triangle narrowing
                for k in range(w):
                    xx = x0 + (4 - w) // 2 + k
                    if row == 0 and xx == x0 + 3:
                        continue
                    t = 3 if k == 0 else (1 if k == w - 1 and w > 1 else 2)
                    P(im, xx, top + row, r[t])
            P(im, x0 + 1, top + 6, r[1])
        art.save(im, f'block/{name}')
        m = Model({'flag': f'block/{name}'})
        m.plane_z(0, 6, 16, 16, 15.5, 'flag', uv=[0, 0, 16, 10], double=True)
        m.write(name)
        shape(name, [(0, 6, 14.5, 16, 16, 16)])
        A.blockstate(M, name, facing_variants(f'block/{name}'))
        # item sprite: three pennants on a string
        it = art.img()
        for x in range(1, 15):
            P(it, x, 3 + round(2 * math.sin(math.pi * x / 15)), '#5d4a33')
        for i, x0 in enumerate((1, 6, 11)):
            r = tex.ramp(colours[i % 4], 5, 0.35)
            for row in range(6):
                w = 4 - (row * 4) // 6
                for k in range(w):
                    P(it, x0 + (4 - w) // 2 + k, 5 + round(2 * math.sin(math.pi * (x0 + 1) / 15)) + row,
                      r[3] if k == 0 else r[2])
        art.save(art.sprite_finish(it), f'item/{name}')
        A.flat_item(M, name)


# ================================================================================================= lanterns
COLOURS = ('white', 'red', 'orange', 'yellow', 'lime', 'cyan', 'blue', 'purple', 'pink')


def _paper_texture(colour):
    r = art.dye_ramp(colour, 6, 0.55)
    im = art.img()
    # body side 0..8 x 0..9: warm glow in the middle, ribs at rows 0, 3, 6, 8
    for y in range(9):
        for x in range(8):
            d = abs(x - 3.5) / 4.0 + abs(y - 4) / 7.0
            t = 5 if d < 0.35 else 4 if d < 0.7 else 3
            if y in (0, 8):
                t = 2
            elif y in (3, 6):
                t = max(2, t - 1)
            P(im, x, y, r[t])
        P(im, 0, 1, r[5])
    # vertical seam
    for y in range(1, 8):
        if y not in (3, 6):
            P(im, 7, y, r[3])
    # top/bottom face 8..16 x 0..8: paper disc with darker rim
    for y in range(8):
        for x in range(8):
            d = math.hypot(x - 3.5, y - 3.5)
            P(im, 8 + x, y, r[4] if d < 2.2 else r[3] if d < 3.2 else r[2])
    # tassel 0..2 x 10..16
    for y in range(10, 16):
        P(im, 0, y, r[2])
        P(im, 1, y, r[1])
    return im


def paper_lanterns():
    for c in COLOURS:
        name = f'paper_lantern_{c}'
        art.save(_paper_texture(c), f'block/{name}')
        for hanging in (False, True):
            dy = 3 if hanging else 0
            m = Model({'paper': f'block/{name}', 'cap': 'block/decor_lantern_cap', 'string': 'block/decor_string'})
            ring_lo = {f: ('paper', [0, 0, 6, 1]) for f in ('north', 'south', 'west', 'east')}
            ring_hi = {f: ('paper', [0, 8, 6, 9]) for f in ('north', 'south', 'west', 'east')}
            disc = [9, 1, 15, 7]
            m.box((6, 0 + dy, 6), (10, 1 + dy, 10), 'cap')
            m.box((5, 1 + dy, 5), (11, 2 + dy, 11), 'paper', faces=dict(ring_lo, down=('paper', disc)), skip=('up',), light=12)
            m.box((4, 2 + dy, 4), (12, 9 + dy, 12), 'paper', faces={f: ('paper', [0, 1, 8, 8]) for f in ('north', 'south', 'west', 'east')},
                  skip=('up', 'down'), light=12)
            m.box((4, 2 + dy, 4), (12, 2 + dy, 12), 'paper', faces={'down': ('paper', [8, 0, 16, 8])}, only=('down',), light=12, collide=False)
            m.box((4, 9 + dy, 4), (12, 9 + dy, 12), 'paper', faces={'up': ('paper', [8, 0, 16, 8])}, only=('up',), light=12, collide=False)
            m.box((5, 9 + dy, 5), (11, 10 + dy, 11), 'paper', faces=dict(ring_hi, up=('paper', disc)), skip=('down',), light=12)
            m.box((6, 10 + dy, 6), (10, 11 + dy, 10), 'cap')
            if hanging:
                m.box((7.5, 14, 7.5), (8.5, 16, 8.5), 'string', skip=('up', 'down'), collide=False)
                m.box((7, 13, 7), (9, 14, 9), 'cap', collide=False)
                m.plane_z(7.5, 0.5, 8.5, 3, 8, 'paper', uv=[0, 10, 1, 13])
            else:
                m.box((7, 11, 7), (9, 11.5, 9), 'cap')
                m.plane_x(7.5, 11.5, 8.5, 13, 8, 'string', uv=[0, 0, 1, 1.5])
            suffix = '_hanging' if hanging else ''
            m.write(name + suffix)
        shape(name, [(5, 0, 5, 11, 2, 11), (4, 2, 4, 12, 9, 12), (5, 9, 5, 11, 11.5, 11)])
        shape(name + '_hanging', [(5, 3, 5, 11, 5, 11), (4, 5, 4, 12, 12, 12), (5, 12, 5, 11, 14, 11), (7, 14, 7, 9, 16, 9)])
        A.blockstate(M, name, {'hanging=false': {'model': rid(f'block/{name}')},
                               'hanging=true': {'model': rid(f'block/{name}_hanging')}})
        small_item(name, f'block/{name}', 1.0, 2.0)


def ship_lantern():
    for hanging in (False, True):
        dy = 2 if hanging else 0
        m = Model({'brass': 'block/decor_brass', 'glass': 'block/decor_glass', 'fire': 'minecraft:block/fire_0',
                   'iron': 'block/decor_iron'}, particle='block/decor_brass')
        m.box((5, 0 + dy, 5), (11, 1 + dy, 11), 'brass')
        for x, z in ((5, 5), (10, 5), (5, 10), (10, 10)):
            m.box((x, 1 + dy, z), (x + 1, 8 + dy, z + 1), 'brass', skip=('up', 'down'), collide=False)
        m.cross(8, 8, 1.6, 1.5 + dy, 6 + dy, 'fire', light=15)
        m.box((6, 1 + dy, 6), (10, 1.6 + dy, 10), 'iron', collide=False)
        m.box((5.5, 1 + dy, 5.5), (10.5, 8 + dy, 10.5), 'glass', skip=('up', 'down'), collide=False)
        m.box((4.5, 8 + dy, 4.5), (11.5, 9 + dy, 11.5), 'brass')
        m.box((5.5, 9 + dy, 5.5), (10.5, 10 + dy, 10.5), 'brass')
        m.box((6.5, 10 + dy, 6.5), (9.5, 11 + dy, 9.5), 'iron')
        # ring handle
        m.box((7.5, 11 + dy, 6.5), (8.5, 13 + dy, 7.5), 'brass', collide=False)
        m.box((7.5, 11 + dy, 8.5), (8.5, 13 + dy, 9.5), 'brass', collide=False)
        m.box((7.5, 12.5 + dy, 7.5), (8.5, 13.5 + dy, 8.5), 'brass', collide=False)
        if hanging:
            m.box((7.5, 15.5, 7.5), (8.5, 16, 8.5), 'iron', collide=False)
            m.box((7.75, 15.5, 7.75), (8.25, 15.6, 8.25), 'iron', collide=False)
        m.write('ship_lantern' + ('_hanging' if hanging else ''))
    shape('ship_lantern', [(5, 0, 5, 11, 1, 11), (4.5, 1, 4.5, 11.5, 11, 11.5), (6.5, 11, 6.5, 9.5, 13, 9.5)])
    shape('ship_lantern_hanging', [(5, 2, 5, 11, 3, 11), (4.5, 3, 4.5, 11.5, 13, 11.5), (6.5, 13, 6.5, 9.5, 16, 9.5)])
    A.blockstate(M, 'ship_lantern', {'hanging=false': {'model': rid('block/ship_lantern')},
                                     'hanging=true': {'model': rid('block/ship_lantern_hanging')}})
    small_item('ship_lantern', 'block/ship_lantern', 1.0, 2.0)


def jelly_lamp():
    # animated jelly: soft pulsing cyan/violet blob texture
    frames = []
    for f in range(8):
        im = art.img()
        ph = math.sin(2 * math.pi * f / 8)
        for y in range(16):
            for x in range(16):
                d = math.hypot((x - 7.5) / 8, (y - 7.5) / 8)
                v = max(0.0, 1.0 - d) + 0.15 * ph * (1 - d)
                c = tex.mix('#5a3ad8', '#9af7ff', min(1, v * 1.3))
                c = tex.mix(c, '#ffffff', max(0, v - 0.75) * 1.6)
                im.putpixel((x, y), c)
        # floating specks
        for k in range(3):
            sx = (3 + k * 5 + f) % 16
            sy = (12 - f * 2 + k * 5) % 16
            P(im, sx, sy, '#e8ffff')
        frames.append(im)
    art.save_anim(frames, 'block/jelly_lamp_jelly', frametime=4, interpolate=True)
    for hanging in (False, True):
        dy = 3 if hanging else 0
        m = Model({'glass': 'block/decor_glass', 'jelly': 'block/jelly_lamp_jelly', 'brass': 'block/decor_brass',
                   'rope': 'block/decor_rope'}, particle='block/jelly_lamp_jelly')
        m.box((6, 0.5 + dy, 6), (10, 5.5 + dy, 10), 'jelly', light=15, collide=False)
        m.box((5, 0 + dy, 5), (11, 8 + dy, 11), 'glass')
        m.box((5.5, 8 + dy, 5.5), (10.5, 9.5 + dy, 10.5), 'brass')
        m.box((7, 9.5 + dy, 7), (9, 10.5 + dy, 9), 'brass')
        if hanging:
            m.box((7.5, 10.5 + dy, 7.5), (8.5, 16, 8.5), 'rope', skip=('up', 'down'), collide=False)
        m.write('jelly_lamp' + ('_hanging' if hanging else ''))
    shape('jelly_lamp', [(5, 0, 5, 11, 9.5, 11), (7, 9.5, 7, 9, 10.5, 9)])
    shape('jelly_lamp_hanging', [(5, 3, 5, 11, 12.5, 11), (7, 12.5, 7, 9, 16, 9)])
    A.blockstate(M, 'jelly_lamp', {'hanging=false': {'model': rid('block/jelly_lamp')},
                                   'hanging=true': {'model': rid('block/jelly_lamp_hanging')}})
    small_item('jelly_lamp', 'block/jelly_lamp', 1.0, 2.0)


# ================================================================================================= fire
def tiki_torch():
    # bamboo pole with nodes
    pole = art.img()
    b = art.BAMBOO
    for y in range(16):
        for x in range(16):
            t = [2, 3, 4, 3][x % 4] if x % 8 < 4 else [3, 2, 2, 1][x % 4]
            pole.putpixel((x, y), b[t])
    for y in (4, 12):
        for x in range(16):
            P(pole, x, y, b[1])
            P(pole, x, y - 1, b[4])
    art.save(pole, 'block/tiki_torch_pole')
    # woven head
    head = art.img()
    w = art.WICKER
    for y in range(16):
        for x in range(16):
            k = ((x // 2) + (y // 2)) % 2
            t = 3 if k else 2
            if (x + y) % 4 == 0:
                t -= 1
            head.putpixel((x, y), w[t])
    art.save(head, 'block/tiki_torch_head')
    # burnt top (unlit) / embers (lit)
    top = tex.noise_fill(art.img(), art.pal('#1c1612', '#2a201a', '#3a2d24', '#4d3c2f'), 21, 3, 2)
    art.save(top, 'block/tiki_torch_top')
    lit = tex.noise_fill(art.img(), art.pal('#5a1a08', '#a8380c', '#e8701c', '#ffb43c', '#ffe08a'), 22, 3, 2, 1.3)
    art.save(lit, 'block/tiki_torch_top_lit')

    m = Model({'pole': 'block/tiki_torch_pole', 'rope': 'block/decor_rope'})
    m.box((6.5, 0, 6.5), (9.5, 16, 9.5), 'pole')
    m.box((6.25, 6, 6.25), (9.75, 7, 9.75), 'rope', collide=False)
    m.write('tiki_torch_lower')
    shape('tiki_torch_lower', [(6.5, 0, 6.5, 9.5, 16, 9.5)])
    for lit_ in (False, True):
        m = Model({'pole': 'block/tiki_torch_pole', 'head': 'block/tiki_torch_head', 'rope': 'block/decor_rope',
                   'top': 'block/tiki_torch_top_lit' if lit_ else 'block/tiki_torch_top', 'fire': 'minecraft:block/fire_0'},
                  particle='block/tiki_torch_head')
        m.box((6.5, 0, 6.5), (9.5, 7, 9.5), 'pole')
        m.box((5.5, 7, 5.5), (10.5, 8, 10.5), 'head')
        m.box((5, 8, 5), (11, 11.5, 11), 'head', faces={'up': 'top'}, light=6 if lit_ else None)
        m.box((4.5, 11, 4.5), (11.5, 12, 11.5), 'head', faces={'up': None}, collide=False)
        m.box((4.75, 9, 4.75), (11.25, 10, 11.25), 'rope', skip=('up', 'down'), collide=False)
        if lit_:
            m.cross(8, 8, 3.5, 11.5, 20, 'fire', light=15)
        m.write('tiki_torch_upper' + ('_lit' if lit_ else ''))
    shape('tiki_torch_upper', [(6.5, 0, 6.5, 9.5, 7, 9.5), (4.5, 7, 4.5, 11.5, 12, 11.5)])
    A.blockstate(M, 'tiki_torch', {
        'half=lower,lit=false': {'model': rid('block/tiki_torch_lower')},
        'half=lower,lit=true': {'model': rid('block/tiki_torch_lower')},
        'half=upper,lit=false': {'model': rid('block/tiki_torch_upper')},
        'half=upper,lit=true': {'model': rid('block/tiki_torch_upper_lit')}})
    # item sprite
    it = art.img()
    for y in range(5, 16):
        P(it, 7, y, b[3])
        P(it, 8, y, b[2])
    for y in (9, 13):
        P(it, 7, y, b[1])
        P(it, 8, y, b[1])
    for y in range(3, 6):
        for x in range(5, 11):
            P(it, x, y, w[3] if (x + y) % 2 else w[2])
    for x in range(5, 11):
        P(it, x, 6, w[1])
    fl = art.pal('#c23a0c', '#f0701c', '#ffb43c', '#ffe9a0')
    for (x, y, c) in ((7, 0, 2), (8, 1, 2), (6, 1, 1), (7, 1, 3), (7, 2, 3), (8, 2, 2), (6, 2, 1), (9, 2, 1)):
        P(it, x, y, fl[c])
    art.save(art.sprite_finish(it), 'item/tiki_torch')
    A.flat_item(M, 'tiki_torch')


def brazier():
    frames = []
    base = tex.noise_fill(art.img(), art.pal('#2a1a14', '#3a241a', '#4a3020'), 31, 3, 2)
    for f in range(6):
        im = base.copy()
        rnd = random.Random(40 + f)
        nz = tex.value_noise(16, 16, 50 + f, 4, 2)
        for y in range(16):
            for x in range(16):
                v = nz[y][x]
                if v > 0.55:
                    im.putpixel((x, y), tex.rgb(['#8a2a0a', '#d0501a', '#ff8a2a', '#ffc860'][min(3, int((v - 0.55) * 9))]))
        frames.append(im)
    art.save_anim(frames, 'block/brazier_coals_lit', frametime=6, interpolate=True)
    coal = tex.noise_fill(art.img(), art.pal('#18161a', '#232026', '#2f2b31', '#3b363c'), 33, 3, 2)
    art.save(coal, 'block/brazier_coals')
    for lit_ in (False, True):
        m = Model({'iron': 'block/decor_iron', 'coals': 'block/brazier_coals_lit' if lit_ else 'block/brazier_coals',
                   'fire': 'minecraft:block/fire_0'}, particle='block/decor_iron')
        # legs
        for x, z in ((3.5, 3.5), (11, 3.5), (3.5, 11), (11, 11)):
            m.box((x, 0, z), (x + 1.5, 7, z + 1.5), 'iron')
        m.box((4.5, 2, 7.25), (11.5, 3, 8.75), 'iron', collide=False)
        m.box((7.25, 2, 4.5), (8.75, 3, 11.5), 'iron', collide=False)
        # bowl
        m.box((4, 6, 4), (12, 7, 12), 'iron')
        m.box((3, 7, 3), (13, 11, 4), 'iron')
        m.box((3, 7, 12), (13, 11, 13), 'iron')
        m.box((3, 7, 4), (4, 11, 12), 'iron')
        m.box((12, 7, 4), (13, 11, 12), 'iron')
        m.box((2.5, 10.5, 2.5), (13.5, 11.5, 3.5), 'iron', collide=False)
        m.box((2.5, 10.5, 12.5), (13.5, 11.5, 13.5), 'iron', collide=False)
        m.box((2.5, 10.5, 3.5), (3.5, 11.5, 12.5), 'iron', collide=False)
        m.box((12.5, 10.5, 3.5), (13.5, 11.5, 12.5), 'iron', collide=False)
        m.box((4, 7, 4), (12, 9.5, 12), 'coals', light=10 if lit_ else None)
        if lit_:
            m.cross(8, 8, 4, 9, 18, 'fire', light=15)
        m.write('brazier' + ('_lit' if lit_ else ''))
    shape('brazier', [(3, 0, 3, 13, 6, 13), (2.5, 6, 2.5, 13.5, 11.5, 13.5)])
    A.blockstate(M, 'brazier', {'lit=false': {'model': rid('block/brazier')},
                                'lit=true': {'model': rid('block/brazier_lit')}})
    small_item('brazier', 'block/brazier_lit', 0.9, 1.0)


# ================================================================================================= sea
def buoys():
    for col, c, lamp in (('red', '#c8352b', '#ff4a3a'), ('green', '#3c9a48', '#4aff6a')):
        name = f'buoy_{col}'
        r = tex.ramp(c, 6, 0.5)
        im = art.img()
        for y in range(16):
            for x in range(16):
                t = 3 + (1 if x < 5 else 0) - (1 if x > 11 else 0)
                if y in (8, 9, 10):
                    wr = art.WHITE
                    im.putpixel((x, y), wr[t + 1] if y != 10 else wr[t - 1])
                else:
                    im.putpixel((x, y), r[t] if y > 1 else r[t + 1])
        # rust streaks / algae at the bottom (waterline)
        for x in range(0, 16, 3):
            P(im, x, 15, '#3f5a2a')
            P(im, x + 1, 14, '#4f6a32')
        art.save(im, f'block/{name}')
        lr = tex.ramp(lamp, 4, 0.5)
        frames = []
        for f in range(8):
            l = art.img()
            on = f < 2
            for y in range(16):
                for x in range(16):
                    d = math.hypot(x - 7.5, y - 7.5) / 8
                    if on:
                        l.putpixel((x, y), tex.mix(lr[3], '#ffffff', max(0, 0.6 - d)))
                    else:
                        l.putpixel((x, y), lr[0] if d > 0.5 else lr[1])
            frames.append(l)
        art.save_anim(frames, f'block/{name}_lamp', frametime=5)
        top = art.img()
        for y in range(16):
            for x in range(16):
                d = math.hypot(x - 5.5, y - 5.5)
                top.putpixel((x, y), tex.rgb(r[4] if d < 3 else r[3] if d < 9 else r[2]))
        for (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)):
            P(top, x, y, r[1])
        art.save(top, f'block/{name}_top')
        m = Model({'body': f'block/{name}', 'top': f'block/{name}_top', 'iron': 'block/decor_iron', 'lamp': f'block/{name}_lamp'},
                  particle=f'block/{name}')
        sides = ('north', 'south', 'west', 'east')
        m.box((4, 0, 4), (12, 1, 12), 'body', uv={f: [0, 14, 16, 15] for f in sides}, faces={'down': 'top'})
        m.box((3, 1, 3), (13, 6, 13), 'body', uv={f: [0, 6, 16, 11] for f in sides}, faces={'up': 'top', 'down': 'top'})
        m.box((4, 6, 4), (12, 7.5, 12), 'body', uv={f: [0, 2, 16, 3.5] for f in sides}, faces={'up': 'top'}, skip=('down',))
        m.box((5, 7.5, 5), (11, 8, 11), 'iron')
        m.box((5, 8, 5), (11, 9, 11), 'iron')
        for x, z in ((5, 5), (10, 5), (5, 10), (10, 10)):
            m.box((x, 9, z), (x + 1, 17, z + 1), 'iron', collide=False)
        m.box((6.5, 11, 6.5), (9.5, 15, 9.5), 'lamp', light=15, collide=False)
        m.box((6, 10, 6), (10, 11, 10), 'iron', collide=False)
        m.box((5, 17, 5), (11, 18, 11), 'iron', collide=False)
        m.box((7.5, 18, 7.5), (8.5, 20, 8.5), 'iron', collide=False)
        m.write(name)
        shape(name, [(4, 0, 4, 12, 1, 12), (3, 1, 3, 13, 6, 13), (4, 6, 4, 12, 8, 12), (5, 8, 5, 11, 18, 11)])
        A.blockstate(M, name, {'': {'model': rid(f'block/{name}')}})
        small_item(name, f'block/{name}', 0.8, 0.0)


def lighthouse_lens():
    # Fresnel lens: clear glass prism rings with brass-tinted ribs, translucent so the core shows
    im = art.img()
    for y in range(16):
        band = y % 4
        for x in range(16):
            if band == 0:
                c, a = (196, 168, 96), 215
            else:
                c = [(0, 0, 0), (232, 246, 240), (206, 232, 232), (184, 220, 226)][band]
                a = [0, 120, 92, 74][band]
            if x in (0, 15):
                c, a = (176, 150, 84), 225
            im.putpixel((x, y), c + (a,))
    for y in (1, 5, 9, 13):
        P(im, 3, y, (255, 255, 250, 225))
        P(im, 4, y, (255, 255, 245, 190))
        P(im, 11, y + 1, (255, 255, 245, 150))
    art.save(im, 'block/lighthouse_lens')
    core = art.img()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5) / 8
            core.putpixel((x, y), tex.mix('#ffffff', '#ffd860', min(1, d * 1.2)))
    art.save(core, 'block/lighthouse_lens_core')
    m = Model({'brass': 'block/decor_brass', 'lens': 'block/lighthouse_lens', 'core': 'block/lighthouse_lens_core'},
              particle='block/lighthouse_lens')
    m.box((1, 0, 1), (15, 2, 15), 'brass', cull=True)
    m.box((5.5, 5.5, 5.5), (10.5, 10.5, 10.5), 'core', light=15, collide=False)
    m.box((6.5, 2, 6.5), (9.5, 5.5, 9.5), 'brass', collide=False)
    m.box((2, 2, 2), (14, 14, 14), 'lens', light=11, uv={f: [2, 2, 14, 14] for f in ('north', 'south', 'west', 'east')})
    m.box((1, 14, 1), (15, 15.5, 15), 'brass')
    m.box((5, 15.5, 5), (11, 16, 11), 'brass', cull=True)
    m.write('lighthouse_lens')
    shape('lighthouse_lens', [(1, 0, 1, 15, 16, 15)])
    A.blockstate(M, 'lighthouse_lens', {'': {'model': rid('block/lighthouse_lens')}})
    A.block_item(M, 'lighthouse_lens')


def neon_tubes():
    for col, c in (('cyan', '#3ff0ff'), ('pink', '#ff4ad8')):
        name = f'neon_tube_{col}'
        r = tex.ramp(c, 5, 0.5)
        frames = []
        for f in range(12):
            im = art.img()
            dim = f == 7
            for y in range(16):
                for x in range(16):
                    k = x % 4
                    t = [2, 4, 4, 2][k]
                    if dim:
                        t -= 1
                    col_ = r[t] if k in (0, 3) else tex.mix(r[t], '#ffffff', 0.55 if not dim else 0.3)
                    im.putpixel((x, y), col_)
            frames.append(im)
        art.save_anim(frames, f'block/{name}', frametime=3)
        halo = art.img()
        for y in range(16):
            for x in range(16):
                halo.putpixel((x, y), tex.rgb(c)[:3] + (54,))
        art.save(halo, f'block/{name}_halo')
        m = Model({'tube': f'block/{name}', 'halo': f'block/{name}_halo', 'iron': 'block/decor_iron'},
                  particle=f'block/{name}')
        # tube and halo end inside the iron caps, so no face of them lies in the plane of a cap face (z-fighting)
        m.box((7, 1.5, 7), (9, 14.5, 9), 'tube', light=15, uv={f: [0, 1.5, 2, 14.5] for f in ('north', 'south', 'west', 'east')},
              skip=('up', 'down'))
        m.box((6.5, 1.5, 6.5), (9.5, 14.5, 9.5), 'halo', light=15, skip=('up', 'down'), collide=False)
        m.box((6.5, 0, 6.5), (9.5, 1.5, 9.5), 'iron', collide=False)
        m.box((6.5, 14.5, 6.5), (9.5, 16, 9.5), 'iron', collide=False)
        m.write(name)
        shape(name, [(6.5, 0, 6.5, 9.5, 16, 9.5)])
        h = rid(f'block/{name}')
        A.blockstate(M, name, {'axis=y': {'model': h}, 'axis=z': {'model': h, 'x': 90},
                               'axis=x': {'model': h, 'x': 90, 'y': 90}})
        small_item(name, f'block/{name}', 1.0, 0.0)


def lamps():
    # driftwood lamp: glowing canvas shade
    shade_tex = art.img()
    cr = art.pal('#c8a46a', '#dcbc82', '#ecd39c', '#f8e8bc', '#fff6dc')
    for y in range(16):
        for x in range(16):
            t = 3 if 3 < x < 12 else 2
            if y % 5 == 0:
                t -= 1
            if x % 4 == 0:
                t = max(0, t - 1)
            shade_tex.putpixel((x, y), cr[t])
    art.save(shade_tex, 'block/driftwood_lamp_shade')
    m = Model({'wood': 'raft:block/stripped_driftwood_log', 'drift': 'block/decor_driftwood',
               'shade': 'block/driftwood_lamp_shade', 'core': 'block/lighthouse_lens_core'}, particle='block/decor_driftwood')
    m.box((4.5, 0, 4.5), (11.5, 1.5, 11.5), 'drift')
    m.box((7, 1.5, 7), (9, 6, 9), 'wood')
    m.box((7.5, 5.5, 6.5), (9.5, 10, 8.5), 'wood')
    m.box((6, 2, 8.5), (7, 3, 9.5), 'drift', collide=False)
    m.box((6.5, 9.5, 6.5), (9.5, 10, 9.5), 'core', light=15, collide=False)
    m.box((4, 9, 4), (12, 15, 12), 'shade', light=13, faces={'down': None})
    m.box((4.01, 9.01, 4.01), (11.99, 9.01, 11.99), 'core', only=('down', 'up'), light=15, collide=False)
    m.box((5, 15, 5), (11, 16, 11), 'shade', uv={'up': [5, 5, 11, 11]}, light=10)
    m.write('driftwood_lamp')
    shape('driftwood_lamp', [(4.5, 0, 4.5, 11.5, 1.5, 11.5), (7, 1.5, 6.5, 9.5, 9, 9.5), (4, 9, 4, 12, 16, 12)])
    A.blockstate(M, 'driftwood_lamp', {'': {'model': rid('block/driftwood_lamp')}})
    small_item('driftwood_lamp', 'block/driftwood_lamp', 0.9, 0.5)

    # seashell lamp: two crossed glowing scallop shells on a driftwood stem
    sh = art.img()
    s_ = art.SHELL
    for y in range(16):
        for x in range(16):
            t = 3 + (1 if x % 3 == 1 else 0) - (1 if x % 3 == 0 else 0)
            sh.putpixel((x, y), s_[max(0, min(5, t))])
    art.save(sh, 'block/seashell_lamp_shell')
    fan = art.img()
    hx, hy = 7.5, 15.5
    for y in range(16):
        for x in range(16):
            dx, dy_ = x - hx, hy - y
            d = math.hypot(dx, dy_)
            ang = math.degrees(math.atan2(dy_, dx))           # 0 = right, 90 = up
            if not (12 <= ang <= 168):
                continue
            rib = int((ang - 12) / 13)                          # 12 ribs
            edge = 9.6 + 0.9 * math.cos(math.radians((ang - 12) % 13 / 13 * 360))
            if d > edge:
                continue
            k = 4 if rib % 2 == 0 else 3
            if d > edge - 1.2:
                k -= 1
            if d < 3:
                k = 2
            if x < 6 and y < 9:
                k = min(5, k + 1)
            fan.putpixel((x, y), s_[k])
    # hinge ears
    for x in range(5, 11):
        P(fan, x, 15, s_[2])
        P(fan, x, 14, s_[3])
    art.save(fan, 'block/seashell_lamp_fan')
    m = Model({'shell': 'block/seashell_lamp_shell', 'fan': 'block/seashell_lamp_fan', 'drift': 'block/decor_driftwood',
               'core': 'block/lighthouse_lens_core', 'sand': 'minecraft:block/sandstone_top'}, particle='block/seashell_lamp_shell')
    m.box((4, 0, 4), (12, 1.5, 12), 'sand')
    m.box((9, 1.5, 4.5), (11, 3, 7), 'shell', light=6, collide=False)
    m.box((5, 1.5, 9.5), (6.5, 2.5, 11), 'shell', light=6, collide=False)
    m.box((7, 1.5, 7), (9, 6, 9), 'drift')
    m.box((7, 6, 7), (9, 8, 9), 'core', light=15, collide=False)
    fu = [0.5, 5, 15.5, 16]
    for rot in (45, -45):
        m.box((0.5, 5, 7.6), (15.5, 16, 8.4), 'fan', faces={'north': ('fan', fu), 'south': ('fan', [fu[2], fu[1], fu[0], fu[3]])},
              only=('north', 'south'), rot=('y', rot, (8, 8, 8)), light=12, collide=False)
    m.write('seashell_lamp')
    shape('seashell_lamp', [(4, 0, 4, 12, 1.5, 12), (7, 1.5, 7, 9, 6, 9), (3, 5, 3, 13, 16, 13)])
    A.blockstate(M, 'seashell_lamp', {'': {'model': rid('block/seashell_lamp')}})
    small_item('seashell_lamp', 'block/seashell_lamp', 0.9, 0.5)


def build():
    shared()
    string_lights()
    bunting()
    paper_lanterns()
    ship_lantern()
    jelly_lamp()
    tiki_torch()
    brazier()
    buoys()
    lighthouse_lens()
    neon_tubes()
    lamps()
