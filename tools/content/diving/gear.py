"""Textures worn on the body: the humanoid armor layers (equipment assets) and the 3D gear parts (DivingGearModels),
plus the harpoon entity texture."""
import math

from bclib import tex

from content.diving import art
from content.diving.art import BRASS, COPPER, STEEL, DARK_STEEL, RED, WHITE, TITANIUM, TEAL, GLASS, NEOPRENE, \
    YELLOW, FIN, RUBBER, WEBBING

CLEAR = (0, 0, 0, 0)


# ----------------------------------------------------------------------------------------------- humanoid layers
def _head_faces():
    """Faces of the 8x8x8 head box at (0, 0) of a 64x32 armor texture."""
    return art.box_faces(0, 0, 8, 8, 8)


def humanoid_helmet():
    """Brass dome: big glass porthole in front, small ones on the sides, rivet seams, a valve plate at the back."""
    img = tex.new(64, 32)
    f = _head_faces()
    for name in ('top', 'right', 'front', 'left', 'back'):
        x, y, w, h = f[name]
        art.face_shade(img, x, y, w, h, BRASS, bias=0.1 if name == 'top' else 0.0, curve=0.5)
    # rivet seam around the lower edge of the dome
    for name in ('right', 'front', 'left', 'back'):
        x, y, w, h = f[name]
        for i in range(w):
            tex.px(img, x + i, y + h - 1, BRASS[1] if i % 2 else BRASS[3])
    # top: crown seam + valve socket
    x, y, w, h = f['top']
    for i in range(w):
        tex.px(img, x + i, y + 3, BRASS[2]); tex.px(img, x + 3, y + i, BRASS[2])
    tex.rect(img, x + 3, y + 3, x + 4, y + 4, COPPER[1])
    # front porthole (the 3D rim frames it): dark glass with a glint
    x, y, w, h = f['front']
    for gy in range(1, 6):
        for gx in range(2, 6):
            if (gx, gy) in ((2, 1), (5, 1), (2, 5), (5, 5)):
                continue
            v = 0.55 - 0.08 * gy - 0.06 * (gx - 2)
            tex.px(img, x + gx, y + gy, art.pick(GLASS, v))
    tex.px(img, x + 3, y + 2, '#d8f4f0'); tex.px(img, x + 2, y + 2, GLASS[4])
    # side portholes (small, framed by the 3D side rims)
    for name in ('right', 'left'):
        x, y, w, h = f[name]
        for gy in (3, 4):
            for gx in (3, 4):
                tex.px(img, x + gx, y + gy, GLASS[1] if gy == 4 else GLASS[2])
        tex.px(img, x + 3, y + 3, GLASS[4])
    # back: a soldered seam down the dome, two small rivets and the brass boss the 3D air fitting screws into
    x, y, w, h = f['back']
    for yy in range(0, 3):
        tex.px(img, x + 3, y + yy, BRASS[1])
    tex.rect(img, x + 3, y + 3, x + 4, y + 5, BRASS[3])
    for bx, by in ((1, 2), (6, 2), (1, 5), (6, 5)):
        tex.px(img, x + bx, y + by, BRASS[3])
    return img


def _harness(img, webbing, buckle, plate=None):
    """Chest harness on the body box (16,16) 8x12x4: two shoulder straps, chest strap and a belt with buckles."""
    b = art.box_faces(16, 16, 8, 12, 4)
    x, y, w, h = b['front']
    for yy in range(h):
        for sx in (1, 6):
            tex.px(img, x + sx, y + yy, art.pick(webbing, 0.55 if sx == 1 else 0.4))
    for xx in range(w):
        tex.px(img, x + xx, y + 5, art.pick(webbing, 0.5))
        tex.px(img, x + xx, y + 10, art.pick(webbing, 0.45))
        tex.px(img, x + xx, y + 11, art.pick(webbing, 0.3))
    for (bx, by) in ((3, 5), (4, 5), (3, 10), (4, 10)):
        tex.px(img, x + bx, y + by, buckle[4] if bx == 3 else buckle[2])
    if plate:
        # small chest plate between the straps
        for yy in range(1, 5):
            for xx in range(2, 6):
                tex.px(img, x + xx, y + yy, art.pick(plate, 0.65 - 0.06 * yy - 0.05 * xx))
    # back: straps run down to the belt (the 3D cylinders cover the rest)
    x, y, w, h = b['back']
    for yy in range(h):
        for sx in (1, 6):
            tex.px(img, x + sx, y + yy, art.pick(webbing, 0.35))
    for xx in range(w):
        tex.px(img, x + xx, y + 10, art.pick(webbing, 0.35))
        tex.px(img, x + xx, y + 11, art.pick(webbing, 0.25))
    # sides: the belt
    for name in ('right', 'left'):
        x, y, w, h = b[name]
        for xx in range(w):
            tex.px(img, x + xx, y + 10, art.pick(webbing, 0.4))
            tex.px(img, x + xx, y + 11, art.pick(webbing, 0.28))
    # top: straps over the shoulders
    x, y, w, h = b['top']
    for yy in range(h):
        for sx in (1, 6):
            tex.px(img, x + sx, y + yy, art.pick(webbing, 0.6))


def humanoid_tank(titanium=False):
    img = tex.new(64, 32)
    if titanium:
        _harness(img, tex.ramp('#2c3e5c', 4, spread=0.35), TITANIUM, plate=TITANIUM)
        # titanium shoulder pads on the arms (arm box at (40,16) 4x12x4)
        a = art.box_faces(40, 16, 4, 12, 4)
        x, y, w, h = a['top']
        art.face_shade(img, x, y, w, h, TITANIUM, bias=0.1)
        for name in ('right', 'front', 'left', 'back'):
            x, y, w, h = a[name]
            art.face_shade(img, x, y, w, 3, TITANIUM)
            for xx in range(w):
                tex.px(img, x + xx, y + 3, TITANIUM[1])
    else:
        _harness(img, WEBBING, BRASS)
    return img


def humanoid_wetsuit():
    """Leggings layer: full neoprene suit on body and legs, teal side stripes, yellow knee pads, back zip."""
    img = tex.new(64, 32)
    b = art.box_faces(16, 16, 8, 12, 4)
    l = art.box_faces(0, 16, 4, 12, 4)
    for faces in (b, l):
        for name, (x, y, w, h) in faces.items():
            if name == 'bottom' and faces is b:
                continue
            for yy in range(h):
                for xx in range(w):
                    v = 0.55 - 0.15 * (xx / max(1, w - 1)) - (0.1 if name in ('back', 'left') else 0.0)
                    tex.px(img, x + xx, y + yy, art.pick(NEOPRENE, v))
    # body: teal panels on the sides, chest stripe, back zip
    x, y, w, h = b['front']
    for xx in range(w):
        tex.px(img, x + xx, y + 2, TEAL[2])
    tex.px(img, x + 3, y + 0, NEOPRENE[4]); tex.px(img, x + 4, y + 0, NEOPRENE[4])
    for name in ('right', 'left'):
        x, y, w, h = b[name]
        for yy in range(h):
            tex.px(img, x + 1, y + yy, TEAL[3]); tex.px(img, x + 2, y + yy, TEAL[2])
    x, y, w, h = b['back']
    for yy in range(h):
        tex.px(img, x + 3, y + yy, STEEL[2] if yy % 2 else STEEL[3])
    tex.px(img, x + 3, y, STEEL[5])
    # legs: outer teal stripe, yellow knee pad
    x, y, w, h = l['right']
    for yy in range(h):
        tex.px(img, x + 1, y + yy, TEAL[3]); tex.px(img, x + 2, y + yy, TEAL[2])
    x, y, w, h = l['front']
    for yy in (5, 6, 7):
        for xx in range(w):
            tex.px(img, x + xx, y + yy, art.pick(YELLOW, 0.75 - 0.15 * (yy - 5) - 0.08 * xx))
    return img


def humanoid_flippers():
    """Boots layer: rubber foot pockets with a yellow heel strap; the blades are 3D (flippers_gear)."""
    img = tex.new(64, 32)
    l = art.box_faces(0, 16, 4, 12, 4)
    for name in ('right', 'front', 'left', 'back'):
        x, y, w, h = l[name]
        for yy in range(7, 12):
            for xx in range(w):
                tex.px(img, x + xx, y + yy, art.pick(RUBBER, 0.6 - 0.1 * xx / max(1, w - 1) - 0.05 * (yy - 7)))
        for xx in range(w):
            tex.px(img, x + xx, y + 8, art.pick(YELLOW, 0.6))
    x, y, w, h = l['front']
    for yy in range(9, 12):
        for xx in range(w):
            tex.px(img, x + xx, y + yy, art.pick(FIN, 0.55 - 0.1 * xx / 3))
    x, y, w, h = l['bottom']
    art.fill(img, x, y, w, h, FIN[1])
    return img


# ----------------------------------------------------------------------------------------------- 3D gear parts
def _shaded_box(img, u, v, w, h, d, ramp, bias=0.0):
    def painter(face, x, y, fw, fh):
        b = bias + {'top': 0.15, 'bottom': -0.25, 'left': -0.1, 'back': -0.1}.get(face, 0.0)
        art.face_shade(img, x, y, fw, fh, ramp, bias=b, curve=0.3)
    art.paint_box(img, u, v, w, h, d, painter)


def _rim(img, x, y, w, h, ramp, hole=True, bolts=True):
    """Square-ish porthole rim on a w x h face: brass frame, transparent rounded hole."""
    art.face_shade(img, x, y, w, h, ramp, curve=0.2)
    if hole:
        for yy in range(1, h - 1):
            for xx in range(1, w - 1):
                corner = (xx in (1, w - 2)) and (yy in (1, h - 2)) and w > 4
                if not corner:
                    img.putpixel((x + xx, y + yy), CLEAR)
    if bolts and w >= 6:
        for bx, by in ((w // 2, 0), (0, h // 2), (w - 1, h // 2), (w // 2, h - 1)):
            tex.px(img, x + bx, y + by, ramp[-1])


def helmet_gear():
    img = tex.new(64, 32)
    # front porthole rim 6x6x1 at (0,0)
    f = art.box_faces(0, 0, 6, 6, 1)
    for name, (x, y, w, h) in f.items():
        if name in ('front', 'back'):
            _rim(img, x, y, w, h, BRASS)
        else:
            art.fill(img, x, y, w, h, BRASS[2])
    # side porthole rims 1x4x4 at (16,0)
    f = art.box_faces(16, 0, 1, 4, 4)
    for name, (x, y, w, h) in f.items():
        if name in ('right', 'left'):
            _rim(img, x, y, w, h, BRASS, bolts=False)
        else:
            art.fill(img, x, y, w, h, BRASS[2])
    # top valve 2x2x2 at (28,0)
    _shaded_box(img, 28, 0, 2, 2, 2, BRASS)
    # red hand wheel 4x0x4 at (28,4): top/bottom faces
    f = art.box_faces(28, 4, 4, 0, 4)
    for name in ('top', 'bottom'):
        x, y, w, h = f[name]
        for yy in range(h):
            for xx in range(w):
                edge = xx in (0, w - 1) or yy in (0, h - 1)
                corner = xx in (0, w - 1) and yy in (0, h - 1)
                spoke = xx == yy or xx == w - 1 - yy
                if corner:
                    continue
                if edge:
                    tex.px(img, x + xx, y + yy, RED[3] if (xx + yy) % 2 else RED[2])
                elif spoke:
                    tex.px(img, x + xx, y + yy, RED[1])
    # inlet 3x3x2 at (44,0)
    _shaded_box(img, 44, 0, 3, 3, 2, COPPER)
    x, y, w, h = art.box_faces(44, 0, 3, 3, 2)['back']
    tex.px(img, x + 1, y + 1, COPPER[0])
    # collar 11x2x7 at (0,10): bolted brass plate
    f = art.box_faces(0, 10, 11, 2, 7)
    x, y, w, h = f['top']
    art.face_shade(img, x, y, w, h, BRASS, bias=0.08, curve=0.45)
    for xx in range(0, w, 2):
        tex.px(img, x + xx, y, BRASS[5]); tex.px(img, x + xx, y + h - 1, BRASS[4])
    for yy in range(1, h - 1, 2):
        tex.px(img, x, y + yy, BRASS[5]); tex.px(img, x + w - 1, y + yy, BRASS[4])
    for name in ('right', 'front', 'left', 'back'):
        x, y, w, h = f[name]
        for xx in range(w):
            tex.px(img, x + xx, y, BRASS[4] if xx % 3 else BRASS[5])
            tex.px(img, x + xx, y + 1, BRASS[2])
    x, y, w, h = f['bottom']
    art.fill(img, x, y, w, h, BRASS[0])
    # helmet lamp 2x2x3 at (0,20): steel housing, glowing lens on the front (drawn full bright)
    _shaded_box(img, 0, 20, 2, 2, 3, DARK_STEEL, bias=0.1)
    x, y, w, h = art.box_faces(0, 20, 2, 2, 3)['front']
    tex.px(img, x, y, '#fffbe6'); tex.px(img, x + 1, y, '#ffe9a0')
    tex.px(img, x, y + 1, '#ffd65c'); tex.px(img, x + 1, y + 1, '#f0b840')
    return img


def tank_gear(titanium=False):
    img = tex.new(64, 32)
    body = TITANIUM if titanium else RED
    band = TEAL if titanium else WHITE
    metal = STEEL if titanium else BRASS
    strap = tex.ramp('#2c3e5c', 4, spread=0.35) if titanium else WEBBING
    # cylinder 3x10x3 at (0,0)
    f = art.box_faces(0, 0, 3, 10, 3)
    for name, (x, y, w, h) in f.items():
        if name in ('top', 'bottom'):
            art.fill(img, x, y, w, h, body[2] if name == 'top' else body[0])
            continue
        shade = {'right': 0.1, 'front': 0.0, 'left': -0.1, 'back': -0.15}[name]
        for xx in range(w):
            col = [0.62, 0.78, 0.45][xx] + shade
            for yy in range(h):
                c = art.pick(body, col - 0.02 * yy)
                if yy in (1, 2):
                    c = art.pick(band, col + 0.05)
                if yy == h - 1:
                    c = art.pick(body, col - 0.35)
                tex.px(img, x + xx, y + yy, c)
        if titanium and name == 'back':
            tex.px(img, x + 1, y + 5, DARK_STEEL[1]); tex.px(img, x + 1, y + 6, DARK_STEEL[2])
    # caps 2x1x2 at (12,0), manifold 6x1x1 at (12,4), regulator 2x2x2 at (26,0)
    _shaded_box(img, 12, 0, 2, 1, 2, metal)
    _shaded_box(img, 12, 4, 6, 1, 1, metal)
    _shaded_box(img, 26, 0, 2, 2, 2, DARK_STEEL)
    x, y, w, h = art.box_faces(26, 0, 2, 2, 2)['top']
    tex.px(img, x, y, metal[4])
    # hose 1x3x1 at (34,0): black rubber with rings
    f = art.box_faces(34, 0, 1, 3, 1)
    for name, (x, y, w, h) in f.items():
        for yy in range(h):
            tex.px(img, x, y + yy, RUBBER[2] if yy % 2 else RUBBER[1])
    # strap with buckle 4x1x1 at (38,0)
    f = art.box_faces(38, 0, 4, 1, 1)
    for name, (x, y, w, h) in f.items():
        for xx in range(w):
            tex.px(img, x + xx, y, art.pick(strap, 0.5))
    x, y, w, h = f['back']
    tex.px(img, x + 1, y, metal[4]); tex.px(img, x + 2, y, metal[3])
    return img


def flipper_gear():
    """Blade 5x1x7 at (0,0): fin outline with ribs; the end away from the foot is wide and notched."""
    img = tex.new(32, 16)
    f = art.box_faces(0, 0, 5, 1, 7)
    for name in ('top', 'bottom'):
        x, y, w, h = f[name]
        for yy in range(h):
            # yy = 0 at the foot, h-1 at the tip
            half = 1.6 + 0.6 * yy
            for xx in range(w):
                if abs(xx - 2) > half:
                    continue
                if yy == h - 1 and xx == 2:
                    continue
                rib = xx in (1, 3) and yy >= 1
                v = 0.62 - 0.03 * yy - (0.2 if rib else 0.0) - (0.2 if name == 'bottom' else 0.0)
                tex.px(img, x + xx, y + yy, art.pick(FIN, v))
    for name in ('right', 'front', 'left', 'back'):
        x, y, w, h = f[name]
        for xx in range(w):
            tex.px(img, x + xx, y, FIN[1])
    return img


def harpoon_entity():
    img = tex.new(32, 32)

    def steel(u, v, w, h, d, bias=0.0, ramp=STEEL):
        _shaded_box(img, u, v, w, h, d, ramp, bias)

    steel(0, 0, 1, 17, 1, bias=-0.05)
    steel(4, 0, 2, 2, 2, ramp=BRASS)
    steel(12, 0, 1, 3, 2, bias=0.2)
    steel(18, 0, 1, 1, 1, bias=0.3)
    steel(4, 4, 1, 3, 1, bias=0.1)
    steel(8, 4, 1, 3, 1, bias=0.0)
    steel(12, 5, 2, 1, 2, ramp=BRASS)
    steel(20, 5, 1, 2, 1, ramp=BRASS)
    # fins: front/back faces of the zero-thickness planes, red with a white stripe, swept triangle
    for (x0, y0, w, h) in ((0, 18, 5, 4), (5, 18, 5, 4), (10, 23, 5, 4), (15, 23, 5, 4)):
        for yy in range(h):
            for xx in range(w):
                # keep a swept shape: near the shaft (centre) full height, outer columns shorter
                outer = abs(xx - (w - 1) / 2)
                if yy < outer - 0.5:
                    continue
                c = WHITE[2] if yy == 2 else (RED[3] if xx < w / 2 else RED[2])
                tex.px(img, x0 + xx, y0 + yy, c)
    return img


def generate():
    return {
        'entity/equipment/humanoid/diving_helmet': humanoid_helmet(),
        'entity/equipment/humanoid/oxygen_tank': humanoid_tank(False),
        'entity/equipment/humanoid/titanium_oxygen_tank': humanoid_tank(True),
        'entity/equipment/humanoid_leggings/wetsuit': humanoid_wetsuit(),
        'entity/equipment/humanoid/flippers': humanoid_flippers(),
        'entity/diving/diving_helmet_gear': helmet_gear(),
        'entity/diving/oxygen_tank_gear': tank_gear(False),
        'entity/diving/titanium_oxygen_tank_gear': tank_gear(True),
        'entity/diving/flippers_gear': flipper_gear(),
        'entity/diving/harpoon': harpoon_entity(),
    }
