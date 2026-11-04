"""Skins of the people of the sea (npc module): 64x64 textures in the player skin layout, painted in code.

Each character is built from layers like a real outfit: skin, face, hair, shirt, trousers, shoes, then the outer
layers of the player model (hat, jacket, sleeves, trouser legs) for coats, collars, hair volume and caps. Shading
follows the art direction of the mod: light from the top-left, top faces lightest, sides darker towards the bottom,
backs and bottoms darkest, one hue-shifted ramp per material, soft value noise instead of salt noise.

Layout (u, v, w, h, d of the boxes; arms are 3 wide on slim skins):
  head 0,0 8x8x8   hat 32,0     body 16,16 8x12x4   jacket 16,32
  right arm 40,16  sleeve 40,32  left arm 32,48  sleeve 48,48
  right leg 0,16   pants 0,32    left leg 16,48  pants 0,48
"""
from PIL import Image

from bclib import tex


class Box:
    def __init__(self, u, v, w, h, d):
        self.u, self.v, self.w, self.h, self.d = u, v, w, h, d

    def faces(self):
        u, v, w, h, d = self.u, self.v, self.w, self.h, self.d
        return {
            'top': (u + d, v, w, d),
            'bottom': (u + d + w, v, w, d),
            'right': (u, v + d, d, h),
            'front': (u + d, v + d, w, h),
            'left': (u + d + w, v + d, d, h),
            'back': (u + 2 * d + w, v + d, w, h),
        }

    def face(self, name):
        return self.faces()[name]


SIDES = ('front', 'right', 'left', 'back')
# light per face (top-left light): value added to the base shade
FACE_LIGHT = {'top': 0.18, 'front': 0.04, 'right': 0.0, 'left': -0.08, 'back': -0.12, 'bottom': -0.3}


class Skin:
    def __init__(self, slim=False, seed=1):
        self.img = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
        self.slim = slim
        aw = 3 if slim else 4
        self.HEAD = Box(0, 0, 8, 8, 8)
        self.HAT = Box(32, 0, 8, 8, 8)
        self.BODY = Box(16, 16, 8, 12, 4)
        self.JACKET = Box(16, 32, 8, 12, 4)
        self.RARM = Box(40, 16, aw, 12, 4)
        self.RSLEEVE = Box(40, 32, aw, 12, 4)
        self.LARM = Box(32, 48, aw, 12, 4)
        self.LSLEEVE = Box(48, 48, aw, 12, 4)
        self.RLEG = Box(0, 16, 4, 12, 4)
        self.RPANTS = Box(0, 32, 4, 12, 4)
        self.LLEG = Box(16, 48, 4, 12, 4)
        self.LPANTS = Box(0, 48, 4, 12, 4)
        self.seed = seed
        self.noise = tex.value_noise(64, 64, seed, 3, 2)

    # ------------------------------------------------------------------------------------------ pixels
    def put(self, x, y, c):
        if 0 <= x < 64 and 0 <= y < 64 and c is not None:
            self.img.putpixel((x, y), tex.rgb(c))

    def clear(self, x, y):
        self.img.putpixel((x, y), (0, 0, 0, 0))

    def get(self, x, y):
        return self.img.getpixel((x, y))

    def shade(self, ramp, v):
        k = len(ramp)
        return ramp[max(0, min(k - 1, int(round(v * (k - 1)))))]

    def paint(self, box, ramp, rows=None, faces=None, base=0.55, grad=0.16, mottle=0.10, fn=None):
        """Fill faces of a box with a ramp. rows=(r0, r1): only those rows of the side faces (and top/bottom only
        when the range touches them). fn(face, fx, fy, w, h) -> colour/None overrides single pixels."""
        for name, (rx, ry, rw, rh) in box.faces().items():
            if faces is not None and name not in faces:
                continue
            side = name in SIDES
            if rows is not None:
                if name == 'top' and rows[0] > 0:
                    continue
                if name == 'bottom' and rows[1] < box.h:
                    continue
            for fy in range(rh):
                if side and rows is not None and not (rows[0] <= fy < rows[1]):
                    continue
                for fx in range(rw):
                    c = fn(name, fx, fy, rw, rh) if fn else None
                    if c is None:
                        v = base + FACE_LIGHT[name]
                        if side and rh > 1:
                            v += grad * (0.5 - fy / (rh - 1))
                        v += (self.noise[ry + fy][rx + fx] - 0.5) * mottle * 2
                        c = self.shade(ramp, v)
                    if c == 'clear':
                        self.clear(rx + fx, ry + fy)
                    else:
                        self.put(rx + fx, ry + fy, c)

    def pixel(self, box, face, fx, fy, c):
        rx, ry, rw, rh = box.face(face)
        if 0 <= fx < rw and 0 <= fy < rh:
            self.put(rx + fx, ry + fy, c)

    def row(self, box, face, fy, colours, start=0):
        for i, c in enumerate(colours):
            self.pixel(box, face, start + i, fy, c)

    def around(self, box, fy, fn):
        """fn(face, fx, w) -> colour for one row of all four side faces (a band around a limb or the body)."""
        for name in SIDES:
            rx, ry, rw, rh = box.face(name)
            for fx in range(rw):
                c = fn(name, fx, rw)
                if c is not None:
                    self.put(rx + fx, ry + fy, c)

    def save(self, module, name):
        return tex.save(self.img, module, f'entity/npc/{name}')

    # ------------------------------------------------------------------------------------------ preview
    def preview(self, scale=8):
        """Front and back view (16x32 each, outer layers on top), side by side, for a quick look."""
        def view(back=False):
            v = Image.new('RGBA', (16, 32), (0, 0, 0, 0))

            def blit(box, x, y, face=None):
                f = face or ('back' if back else 'front')
                rx, ry, rw, rh = box.face(f)
                part = self.img.crop((rx, ry, rx + rw, ry + rh))
                if back and f == 'back':
                    pass
                v.alpha_composite(part, (x, y))
            aw = self.RARM.w
            # front: the entity's right side is on the viewer's left
            for base, over in ((self.HEAD, self.HAT),):
                blit(base, 4, 0); blit(over, 4, 0)
            blit(self.BODY, 4, 8); blit(self.JACKET, 4, 8)
            if not back:
                blit(self.RARM, 4 - aw, 8); blit(self.RSLEEVE, 4 - aw, 8)
                blit(self.LARM, 12, 8); blit(self.LSLEEVE, 12, 8)
                blit(self.RLEG, 4, 20); blit(self.RPANTS, 4, 20)
                blit(self.LLEG, 8, 20); blit(self.LPANTS, 8, 20)
            else:
                blit(self.LARM, 4 - aw, 8); blit(self.LSLEEVE, 4 - aw, 8)
                blit(self.RARM, 12, 8); blit(self.RSLEEVE, 12, 8)
                blit(self.LLEG, 4, 20); blit(self.LPANTS, 4, 20)
                blit(self.RLEG, 8, 20); blit(self.RPANTS, 8, 20)
            return v
        a, b = view(False), view(True)
        sheet = Image.new('RGBA', (40, 34), (70, 110, 140, 255))
        sheet.alpha_composite(a, (2, 1))
        sheet.alpha_composite(b, (22, 1))
        return sheet.resize((40 * scale, 34 * scale), Image.NEAREST)


# ============================================================================================== building blocks
def ramp(c, n=6, spread=0.5, hue_shift=0.04):
    return tex.ramp(c, n, spread=spread, hue_shift=hue_shift)


def skin_body(s, tone):
    """Bare skin everywhere (clothes are painted over it)."""
    for box in (s.HEAD, s.BODY, s.RARM, s.LARM, s.RLEG, s.LLEG):
        s.paint(box, tone, base=0.55, grad=0.12, mottle=0.06)


def face(s, tone, eye='#3d5a80', brow='#3a2a20', white='#f3efe6', mouth=None, age=0, glasses=None, blush=None,
         lashes=False, eye_row=4):
    """The front of the head. age 0 young .. 2 old (wrinkles, heavy brows)."""
    H = s.HEAD
    fx = lambda x, y, c: s.pixel(H, 'front', x, y, c)
    sk = tone
    # cheek shading and nose
    for y in range(8):
        fx(0, y, sk[2]); fx(7, y, sk[2])
    fx(3, eye_row + 1, sk[2]); fx(4, eye_row + 1, sk[1])       # nose shadow
    fx(3, eye_row, sk[4]); fx(4, eye_row, sk[3])                # nose bridge highlight
    # eyes: white outside, iris inside
    e = tex.rgb(eye)
    fx(1, eye_row, white); fx(2, eye_row, e)
    fx(5, eye_row, e); fx(6, eye_row, white)
    if lashes:
        fx(1, eye_row - 1, tex.darker(brow, 0.8)); fx(6, eye_row - 1, tex.darker(brow, 0.8))
    # brows
    b = tex.rgb(brow)
    bw = (1, 2) if age < 2 else (1, 2, 3)
    for x in bw:
        fx(x, eye_row - 1 - (1 if lashes else 0), b)
        fx(7 - x, eye_row - 1 - (1 if lashes else 0), b)
    # mouth
    m = tex.rgb(mouth) if mouth else tex.darker(sk[1], 0.82)
    fx(3, eye_row + 2, m); fx(4, eye_row + 2, m)
    if age == 0:
        fx(2, eye_row + 2, sk[2]); fx(5, eye_row + 2, sk[2])
    if age >= 1:
        fx(0, eye_row, sk[1]); fx(7, eye_row, sk[1])           # crow's feet
    if age >= 2:
        fx(2, eye_row + 1, sk[2]); fx(5, eye_row + 1, sk[2])   # bags under the eyes
        fx(2, eye_row + 2, sk[1]); fx(5, eye_row + 2, sk[1])   # laugh lines
    if blush:
        fx(1, eye_row + 1, blush); fx(6, eye_row + 1, blush)
    if glasses:
        g = tex.rgb(glasses)
        for x in (0, 3, 4, 7):
            fx(x, eye_row, g)
        for x in (1, 2, 5, 6):
            fx(x, eye_row - 1, g)
            fx(x, eye_row + 1, g)
        fx(1, eye_row, tex.mix(white, '#bfe4f2', 0.5)); fx(6, eye_row, tex.mix(white, '#bfe4f2', 0.5))
        # arms of the glasses on the sides
        for side in ('right', 'left'):
            for x in range(1, 6):
                s.pixel(H, side, x if side == 'left' else 7 - x, eye_row, g)


def hair(s, colour, style='short', fringe=1, back_len=3, side_len=2):
    """Hair on the head box: top, the back down to back_len rows, the sides down to side_len, a fringe on the
    front (fringe rows). Styles: 'short', 'long' (to the shoulders), 'bald' (sides and back only), 'bun'."""
    H = s.HEAD
    r = ramp(colour, 5, 0.45)
    if style != 'bald':
        s.paint(H, r, faces=('top',), base=0.6, mottle=0.18)
    for name in ('right', 'left', 'back'):
        rx, ry, rw, rh = H.face(name)
        depth = back_len if name == 'back' else side_len
        if style == 'long':
            depth = 7 if name == 'back' else 6
        for fy in range(depth):
            for fx in range(rw):
                if name != 'back' and fy >= 2 and style != 'long':
                    # sideburn only at the back half of the side (towards the back of the head)
                    near_back = (fx < 3) if name == 'right' else (fx > rw - 4)
                    if not near_back:
                        continue
                v = 0.5 - fy * 0.05 + (s.noise[ry + fy][rx + fx] - 0.5) * 0.35
                if style == 'bald' and fy < 2:
                    continue
                s.put(rx + fx, ry + fy, s.shade(r, v))
    if style != 'bald':
        rx, ry, rw, rh = H.face('front')
        for fy in range(fringe):
            for fx in range(rw):
                s.put(rx + fx, ry + fy, s.shade(r, 0.62 - fy * 0.12 + (s.noise[ry + fy][rx + fx] - 0.5) * 0.3))
        if style == 'long':
            for fy in range(fringe, 7):
                s.put(rx, ry + fy, s.shade(r, 0.45))
                s.put(rx + 7, ry + fy, s.shade(r, 0.4))
    return r


def hair_volume(s, colour, rows=2, back=4, curly=False, sides=True):
    """Hair on the hat layer (stands out half a pixel): top and the upper rows of the sides and back."""
    r = ramp(colour, 5, 0.45)
    s.paint(s.HAT, r, faces=('top',), base=0.62, mottle=0.25)
    for name in ('right', 'left', 'back', 'front'):
        if name in ('right', 'left') and not sides:
            continue
        rx, ry, rw, rh = s.HAT.face(name)
        depth = back if name == 'back' else (1 if name == 'front' else rows)
        for fy in range(depth):
            for fx in range(rw):
                if curly and fy == depth - 1 and (fx + fy) % 2 == 0:
                    continue
                if name == 'front' and curly and fx % 3 == 1:
                    continue
                v = 0.55 - fy * 0.07 + (s.noise[ry + fy][rx + fx] - 0.5) * 0.4
                s.put(rx + fx, ry + fy, s.shade(r, v))
    return r


def beard(s, colour, rows=(5, 8), full=True, moustache=True, sides=True):
    H = s.HEAD
    r = ramp(colour, 5, 0.4)
    rx, ry, rw, rh = H.face('front')
    y0, y1 = rows
    for fy in range(y0, y1):
        for fx in range(rw):
            if not full and fy > y0 and 2 < fx < 5:
                continue
            v = 0.55 - (fy - y0) * 0.08 + (s.noise[ry + fy][rx + fx] - 0.5) * 0.35
            s.put(rx + fx, ry + fy, s.shade(r, v))
    if moustache:
        for fx in (2, 3, 4, 5):
            s.put(rx + fx, ry + y0, s.shade(r, 0.62))
        s.put(rx + 3, ry + y0 + 1, s.shade(r, 0.2)); s.put(rx + 4, ry + y0 + 1, s.shade(r, 0.2))   # mouth in the beard
    if sides:
        for name in ('right', 'left'):
            fx0, fx1 = (5, 8) if name == 'right' else (0, 3)
            srx, sry, _, _ = H.face(name)
            for fy in range(y0 - 1, y1):
                for fx in range(fx0, fx1):
                    s.put(srx + fx, sry + fy, s.shade(r, 0.45 + (s.noise[sry + fy][srx + fx] - 0.5) * 0.3))
    # under the chin
    brx, bry, brw, brh = H.face('bottom')
    for fy in range(brh // 2):
        for fx in range(brw):
            s.put(brx + fx, bry + brh - 1 - fy, s.shade(r, 0.3))
    return r


def shirt(s, colour, sleeve=12, pattern=None, body_rows=12):
    """Shirt on body and arms. sleeve = rows of the arm covered (12 long, 4 short, 0 sleeveless). pattern(face,
    fx, fy, base_colour) -> colour or None."""
    r = ramp(colour, 6, 0.42)

    def fn(rws):
        def f(name, fx, fy, w, h):
            if name in SIDES and fy >= rws:
                return None
            if pattern:
                return pattern(name, fx, fy, r)
            return None
        return f
    s.paint(s.BODY, r, rows=(0, body_rows), fn=fn(body_rows), base=0.55, mottle=0.08)
    if sleeve:
        for arm in (s.RARM, s.LARM):
            s.paint(arm, r, rows=(0, sleeve), fn=fn(sleeve), base=0.55, mottle=0.08)
    return r


def neck_skin(s, tone, kind='v', depth=2):
    """Skin showing at the neck opening of a shirt (v, crew or wide)."""
    rx, ry, rw, rh = s.BODY.face('front')
    if kind == 'v':
        cells = [(3, 0), (4, 0), (3, 1), (4, 1)] + ([(3, 2), (4, 2)] if depth > 2 else [])
    elif kind == 'crew':
        cells = [(3, 0), (4, 0)]
    else:
        cells = [(x, 0) for x in range(2, 6)] + [(3, 1), (4, 1)]
    for (x, y) in cells:
        s.put(rx + x, ry + y, tone[3] if y == 0 else tone[2])


def trousers(s, colour, length=12, belt=None, waist_rows=(8, 12), cuffs=False, pattern=None):
    """Trousers: the lower body rows (waist) and the legs down to `length` rows."""
    r = ramp(colour, 6, 0.42)

    def f(name, fx, fy, w, h):
        if pattern:
            return pattern(name, fx, fy, r)
        return None
    s.paint(s.BODY, r, rows=waist_rows, fn=f, base=0.5, mottle=0.08)
    for leg in (s.RLEG, s.LLEG):
        s.paint(leg, r, rows=(0, length), fn=f, base=0.5, grad=0.1, mottle=0.08)
        if cuffs and length < 12:
            s.around(leg, length - 1, lambda n, fx, w: r[1])
    if belt:
        b = ramp(belt, 4, 0.35)
        s.around(s.BODY, waist_rows[0], lambda n, fx, w: b[2] if n != 'back' else b[1])
    return r


def shoes(s, colour, height=2, laces=None, toe=None):
    r = ramp(colour, 5, 0.4)
    for leg in (s.RLEG, s.LLEG):
        s.paint(leg, r, rows=(12 - height, 12), base=0.45, grad=0.25, mottle=0.06)
        if toe:
            s.pixel(leg, 'front', 1, 11, toe); s.pixel(leg, 'front', 2, 11, toe)
        if laces and height >= 2:
            s.pixel(leg, 'front', 1, 12 - height, laces); s.pixel(leg, 'front', 2, 12 - height, laces)
    return r


def sandals(s, strap, tone):
    st = ramp(strap, 4, 0.35)
    for leg in (s.RLEG, s.LLEG):
        s.around(leg, 11, lambda n, fx, w: st[1])
        s.pixel(leg, 'front', 1, 10, st[2]); s.pixel(leg, 'front', 2, 10, st[2])
        s.pixel(leg, 'front', 1, 9, tone[2]); s.pixel(leg, 'front', 2, 9, tone[3])
        rx, ry, rw, rh = leg.face('bottom')
        for y in range(rh):
            for x in range(rw):
                s.put(rx + x, ry + y, st[0])


def bare_feet(s, tone):
    for leg in (s.RLEG, s.LLEG):
        rx, ry, rw, rh = leg.face('bottom')
        for y in range(rh):
            for x in range(rw):
                s.put(rx + x, ry + y, tone[1])
        s.pixel(leg, 'front', 0, 11, tone[2]); s.pixel(leg, 'front', 3, 11, tone[2])


def hands(s, colour, rows=2):
    r = ramp(colour, 5, 0.4) if isinstance(colour, str) else colour
    for arm in (s.RARM, s.LARM):
        s.paint(arm, r, rows=(12 - rows, 12), base=0.55, grad=0.1, mottle=0.06)


def outer(s, box, colour, rows, fn=None, base=0.52, mottle=0.1):
    """Paint rows of an outer-layer box (jacket, sleeves, pants, hat)."""
    r = ramp(colour, 6, 0.42) if isinstance(colour, str) else colour
    s.paint(box, r, rows=rows, fn=fn, base=base, mottle=mottle)
    return r


def clear_box(s, box):
    for name, (rx, ry, rw, rh) in box.faces().items():
        for y in range(rh):
            for x in range(rw):
                s.clear(rx + x, ry + y)


def spots(s, box, faces, every, colour, light=None, offset=0):
    """A printed pattern: a flower/dot every `every` pixels in a diagonal grid."""
    for name in faces:
        rx, ry, rw, rh = box.face(name)
        for fy in range(rh):
            for fx in range(rw):
                if (fx + offset + (fy // every) * (every // 2)) % every == 0 and fy % every == (offset % every):
                    s.put(rx + fx, ry + fy, colour)
                    if light and fx + 1 < rw:
                        s.put(rx + fx + 1, ry + fy, light)
