"""Entity textures of the marine creatures, painted onto the boxes of the Java models (see javamodel.py)."""
import math

from bclib import tex

from . import javamodel
from .art import noise3, fbm3, cells3, pick, alpha, lerp

M = 'marine'
SHARK_JAVA = 'client/render/SharkModel.java'
MODEL_DIR = 'content/marine/client/model/'


# ================================================================================================= sharks
def _shark_painter(pal, boxes, hammer=False, scars=True):
    grey, white, dark = pal['top'], pal['belly'], pal['fin_dark']
    gum, tooth, eye = tex.ramp('#b0485a', 4, 0.35), tex.ramp('#f1ece0', 3, 0.15), tex.rgb('#14161d')
    seed = 11 if hammer else 3
    fb = {n: javamodel.bounds(boxes, [n]) for n in ('dorsal_fin', 'tail_fin_upper', 'tail_fin_lower', 'left_fin', 'right_fin')}

    def boundary(t):
        # wavy countershading line (absolute y, grows downwards); a bit lower towards the tail
        return 18.6 + (t.az * 0.03) + (noise3(t.ax * 0.3, 0, t.az, 3.0, seed) - 0.5) * 1.6

    def skin(t, base=3.0):
        n = fbm3(t.ax, t.ay, t.az, 3.5, seed)
        k = base + (n - 0.5) * 1.8
        if t.ay < 15.2:
            k -= 0.7                      # darker ridge along the back
        return pick(grey, k)

    def belly(t):
        n = noise3(t.ax, t.ay, t.az, 3.0, seed + 5)
        return pick(white, 2 + (n - 0.5) * 1.6)

    def body(t):
        yb = boundary(t)
        if t.face == 'top':
            return skin(t, 2.6)
        if t.face == 'bottom':
            return belly(t)
        if t.ay > yb + 0.6:
            return belly(t)
        if t.ay > yb - 0.4:
            return pick(grey, 4.6)       # soft light seam between the two colours
        return skin(t)

    def fin(t, edge_dark=True):
        # fins: grey with a dusky tip and a pale underside
        if t.face == 'bottom':
            return pick(white, 1.5) if t.fx < 0.85 else pick(grey, 2)
        b = fb[t.part]
        if t.part in ('left_fin', 'right_fin'):
            tipness = (abs(t.ax) - 5.0) / max(1.0, b[3] - b[0])
        elif t.part == 'tail_fin_lower':
            tipness = (t.ay - b[1]) / max(1.0, b[4] - b[1])
        else:
            tipness = (b[4] - t.ay) / max(1.0, b[4] - b[1])
        k = 3.2 - tipness * 1.6 + (noise3(t.ax, t.ay, t.az, 2.5, seed + 2) - 0.5) * 1.2
        c = pick(grey, k)
        if edge_dark and tipness > 0.85:
            c = pick(dark, 1)
        return c

    def teeth(t, upper):
        if t.face in ('top', 'bottom'):
            return pick(gum, 1)
        # zigzag row of triangular teeth over pink gums
        h = t.fh
        col = t.i % 2
        row = t.j if upper else h - 1 - t.j
        return pick(tooth, 2 - row) if row == 0 or col == 0 else pick(gum, 2)

    def paint(t):
        p = t.part
        if p == 'head' and t.box == 2:
            return teeth(t, True)
        if p == 'jaw' and t.box == 1:
            return teeth(t, False)
        if p == 'jaw':
            if t.face == 'top':
                return pick(gum, 1 + (t.i + t.j) % 2 * 0.6)
            if t.face in ('west', 'east', 'north') and t.fy < 0.5:
                return pick(grey, 3.4) if t.face != 'north' else pick(white, 1)
            return belly(t)
        if p in ('dorsal_fin', 'tail_fin_upper', 'tail_fin_lower', 'left_fin', 'right_fin'):
            return fin(t)
        if p == 'body' and t.box > 0:          # pelvic fins
            return pick(grey, 2.5) if t.face == 'top' else belly(t)
        if p == 'tail1' and t.box > 0:         # second dorsal / anal fin
            return pick(grey, 2) if t.box == 1 else pick(white, 1)
        c = body(t)
        # ---- head details
        if p == 'head' and t.box == 0 and t.face in ('west', 'east'):
            # eye: front third of the head, just above the colour seam
            ez = 0.38 if not hammer else -1
            if abs(t.fz - ez) < 0.08 and 1 <= t.j <= 2:
                return eye if t.j == 2 else tex.mix(eye, '#5a6b80', 0.35)
            # gill slits behind the eye
            if not hammer or True:
                gz = (t.fz - 0.62) / 0.35
                if 0 <= gz <= 1 and t.i % 2 == 0 and 2 <= t.j <= t.fh - 2:
                    return tex.darker(c, 0.72)
        if p == 'head' and t.box == 1:
            if hammer:
                # cephalofoil: eyes at both ends
                if t.face in ('west', 'east'):
                    if t.j == 1 and 1 <= t.i <= 2:
                        return eye
                    return skin(t, 2.8)
                if t.face == 'north':
                    return skin(t, 3.2) if t.j < 2 else belly(t)
                if t.face == 'top':
                    return skin(t, 2.4)
                return belly(t)
            if t.face == 'north':
                # snout tip with two nostrils
                if t.j == 2 and t.i in (1, t.fw - 2):
                    return tex.darker(skin(t), 0.6)
                return skin(t, 3.4) if t.j < 3 else belly(t)
        if scars and p == 'body' and t.box == 0 and t.face == 'west':
            # Bruce's old scars: three pale scratches on the left flank
            for k in range(3):
                if abs((t.i - 4 - k * 2) - (t.j - 2) * 0.8) < 0.5 and 2 <= t.j <= 5:
                    return pick(grey, 5)
        return c

    return paint


def shark_textures(work=None):
    pal = {'top': tex.ramp('#6d8193', 6, 0.5, 0.03), 'belly': tex.ramp('#e7ebee', 4, 0.16),
           'fin_dark': tex.ramp('#3e4a58', 3, 0.3)}
    tw, th, boxes = javamodel.parse(SHARK_JAVA)
    img = javamodel.paint(tw, th, boxes, _shark_painter(pal, boxes))
    tex.save(img, M, 'entity/shark')
    hpal = {'top': tex.ramp('#8a8475', 6, 0.48, 0.03), 'belly': tex.ramp('#ebe9e2', 4, 0.16),
            'fin_dark': tex.ramp('#4d4a40', 3, 0.3)}
    tw, th, boxes = javamodel.parse(SHARK_JAVA, 'createHammerheadLayer')
    img2 = javamodel.paint(tw, th, boxes, _shark_painter(hpal, boxes, hammer=True, scars=False))
    tex.save(img2, M, 'entity/hammerhead_shark')
    return img, img2


# ================================================================================================= whale
def whale_texture():
    tw, th, boxes = javamodel.parse(MODEL_DIR + 'WhaleModel.java')
    dark = tex.ramp('#3e4757', 6, 0.42, 0.03)
    white = tex.ramp('#e6ebee', 4, 0.2)
    barn = tex.ramp('#c9c7bd', 3, 0.3)
    mouth = tex.ramp('#7a3a44', 3, 0.3)

    def skin(t, base=2.6):
        n = fbm3(t.ax, t.ay, t.az, 6.0, 21)
        return pick(dark, base + (n - 0.5) * 1.4)

    def white_skin(t):
        n = noise3(t.ax, t.ay, t.az, 3.0, 22)
        return pick(white, 2 + (n - 0.5) * 1.4)

    def mottled_boundary(t, y0):
        # white belly patch with a ragged, blotchy border
        n = fbm3(t.ax, t.ay, t.az, 3.0, 23)
        return t.ay + (n - 0.5) * 5.0 > y0

    def barnacles(t, c, amount=0.22):
        d, rid = cells3(t.ax, t.ay, t.az, 2.2, 24)
        if d < amount and rid < 0.55:
            return pick(barn, 1 + rid * 3)
        return c

    def pleats(t, c):
        # ventral grooves: lengthwise stripes on the throat
        return tex.darker(c, 0.86) if int(math.floor(t.ax + 20)) % 2 == 0 else c

    def paint(t):
        p = t.part
        if p == 'jaw':
            if t.face == 'top':
                return pick(mouth, 1 + (t.i % 3 == 0))
            if t.face == 'bottom':
                return pleats(t, white_skin(t))
            if t.face == 'north':
                c = skin(t, 2.2) if t.fy < 0.35 else white_skin(t)
                return barnacles(t, c, 0.3)
            # sides: dark lip line on top, pleated white throat below
            if t.fy < 0.3:
                return barnacles(t, skin(t, 2.0), 0.25)
            return pleats(t, white_skin(t)) if t.fy > 0.5 else white_skin(t)
        if p == 'snout':
            c = skin(t, 2.3)
            if t.face == 'top':
                d, rid = cells3(t.ax, 0, t.az, 2.6, 25)
                return pick(dark, 4 if rid > 0.5 else 3.6) if d < 0.28 else c
            if t.face in ('west', 'east'):
                if t.j >= t.fh - 1:
                    return pick(dark, 0.5)        # mouth line
                return barnacles(t, c, 0.24) if t.fy > 0.4 else c
            if t.face == 'north':
                return barnacles(t, c, 0.34)
            if t.face == 'bottom':
                return pick(dark, 1)
            return c
        if p == 'head':
            if t.box > 0:                        # tubercles on the head
                return pick(dark, 1) if t.face != 'top' else pick(dark, 3.5)
            c = skin(t, 2.4)
            if t.face == 'top':
                # knobbly head: small bumps in a few rows, blowholes at the back
                if 0.78 < t.fz < 0.92 and t.i in (t.fw // 2 - 2, t.fw // 2 + 1):
                    return pick(dark, 0)
                d, rid = cells3(t.ax, 0, t.az, 2.6, 25)
                if d < 0.28:
                    return pick(dark, 4 if rid > 0.5 else 3.6)
                return c
            if t.face in ('west', 'east'):
                # eye just above the mouth corner, at the back of the head
                if 0.76 < t.fz < 0.92 and t.fh - 4 <= t.j <= t.fh - 3:
                    first = (t.fz < 0.84) == (t.face == 'west')
                    return pick(white, 3) if (t.j == t.fh - 4 and first) else tex.rgb('#11141b')
                if 0.7 < t.fz < 0.98 and t.j == t.fh - 5:
                    return pick(dark, 4)          # pale ring above the eye
                if t.j >= t.fh - 1:
                    return pick(dark, 0.5)        # mouth line
                if t.fz < 0.3 and t.fy > 0.45:
                    return barnacles(t, c, 0.22)
                return c
            if t.face == 'north':
                return barnacles(t, c, 0.32)
            if t.face == 'bottom':
                return pick(dark, 1)
            return c
        if p in ('left_flipper', 'right_flipper'):
            # long white flippers, dark mottled on top near the body, knobbed leading edge
            base = (abs(t.ax) - 9.0) / 20.0
            if t.face == 'bottom':
                return white_skin(t) if base > 0.12 else pick(dark, 3)
            if t.face == 'north':
                return pick(white, 2) if t.i % 3 else pick(barn, 0)
            if t.face == 'top':
                n = fbm3(t.ax, t.ay, t.az, 3.0, 26)
                return skin(t, 2.6) if n + (0.6 - base) * 0.9 > 0.62 else white_skin(t)
            return white_skin(t) if base > 0.3 else skin(t, 3)
        if p in ('left_fluke', 'right_fluke'):
            if t.face == 'bottom':
                # underside: white with the dark pattern every humpback has
                n = fbm3(t.ax, t.ay, t.az, 2.5, 27)
                return white_skin(t) if n < 0.55 else skin(t, 2.0)
            if t.face == 'south':
                return pick(dark, 1.5) if t.i % 4 else pick(dark, 4)   # serrated trailing edge
            return skin(t, 2.2)
        # body and tail stock: dark above, mottled white belly patch below
        if t.face == 'top':
            if p == 'body' and t.box == 1:
                return pick(dark, 2)
            return skin(t, 2.2)
        if t.face == 'bottom':
            c = white_skin(t)
            if p == 'body' and t.fz < 0.55:
                return pleats(t, c)
            return c if p in ('body', 'tail1') else skin(t, 2.0)
        yb = {'body': 16.5, 'tail1': 15.5, 'tail2': 14.5, 'tail3': 99}.get(p, 99)
        if mottled_boundary(t, yb):
            return white_skin(t)
        return skin(t)

    img = javamodel.paint(tw, th, boxes, paint)
    tex.save(img, M, 'entity/whale')
    return img


# ================================================================================================= manta ray
def manta_texture():
    tw, th, boxes = javamodel.parse(MODEL_DIR + 'MantaRayModel.java')
    black = tex.ramp('#2c3038', 5, 0.3, 0.02)
    white = tex.ramp('#e8eceb', 4, 0.18)
    grey = tex.ramp('#8a9298', 4, 0.3)

    def top(t):
        n = fbm3(t.ax, t.ay, t.az, 3.5, 31)
        return pick(black, 2 + (n - 0.5) * 1.8)

    def shoulder(t):
        # the white shoulder patches of a reef manta: a crescent on each wing near the head
        ax, az = abs(t.ax), t.az
        if ax < 5.5 or ax > 15:
            return None
        cz = -5.5 + (ax - 5.5) * 0.32
        n = noise3(t.ax, 0, t.az, 2.0, 32)
        if cz - 1.8 < az < cz + 1.6 + (n - 0.5) * 1.6 and ax < 14 - (az - cz) * 0.8:
            return pick(white, 1.5 + n) if az > cz - 1.0 else pick(grey, 2)
        return None

    def under(t):
        ax = abs(t.ax)
        c = pick(white, 2 + (noise3(t.ax, t.ay, t.az, 2.0, 33) - 0.5))
        # dusky wing margins and a few dark belly spots
        if t.part.endswith('wing_tip') and (t.fz > 0.75 or (t.fx > 0.7 if t.part.startswith('left') else t.fx < 0.3)):
            return pick(grey, 1)
        if t.part == 'body':
            d, rid = cells3(t.ax, 0, t.az, 1.8, 34)
            if d < 0.28 and rid < 0.35:
                return pick(black, 3)
            # gill slits: five short lines on each side
            if 2.0 < ax < 4.0 and -4.5 < t.az < 0.5 and int(t.az + 10) % 1 == 0 and t.i % 1 == 0:
                if int(math.floor(t.az + 10)) % 2 == 0 and 2.5 < ax < 3.5:
                    return pick(grey, 0)
        return c

    def paint(t):
        p = t.part
        if p == 'tail':
            return pick(black, 1.5 + (t.j + t.i) % 2 * 0.5)
        if p.endswith('horn'):
            inner = (t.face == 'west' and p.startswith('left')) or (t.face == 'east' and p.startswith('right'))
            return pick(grey, 2) if inner or t.face == 'bottom' else top(t)
        if t.face == 'bottom':
            return under(t)
        if t.face == 'top':
            if p != 'body' or abs(t.ax) > 3:
                s = shoulder(t)
                if s is not None:
                    return s
            return top(t)
        if p == 'body' and t.face == 'north':
            # wide mouth on the front edge
            if t.j == 1:
                return pick(black, 0)
            return pick(white, 1) if t.j == 2 else top(t)
        if p == 'body' and t.face in ('west', 'east') and t.box == 0:
            if t.fz < 0.18 and t.j == 0:
                return tex.rgb('#0e1014')        # eye on the side of the head
        if t.fy > 0.6:
            return pick(white, 1)
        return top(t)

    img = javamodel.paint(tw, th, boxes, paint)
    tex.save(img, M, 'entity/manta_ray')
    return img


# ================================================================================================= jellyfish
JELLY = {
    # bell colour, bell alpha, accent (gonads / stripes), tentacle colour, frill colour
    'moon': ('#dbe8f4', 120, '#c38fd9', '#e6eef8', '#e8c9ef'),
    'crimson': ('#d0502f', 175, '#7c1d17', '#8e2a22', '#f0bc9a'),
    'glow': ('#58d8e6', 140, '#c9fbff', '#7cf2f2', '#b48cf0'),
}


def jellyfish_textures():
    tw, th, boxes = javamodel.parse(MODEL_DIR + 'JellyfishModel.java')
    out = {}
    for variant, (bell_c, bell_a, accent, tent_c, frill_c) in JELLY.items():
        bell = tex.ramp(bell_c, 5, 0.32)
        acc = tex.ramp(accent, 3, 0.25)
        tent = tex.ramp(tent_c, 3, 0.3)
        frill = tex.ramp(frill_c, 3, 0.3)
        glow = variant == 'glow'

        def radial(t):
            return math.atan2(t.az, t.ax)

        def paint(t, bell=bell, acc=acc, tent=tent, frill=frill, variant=variant, glow=glow, bell_a=bell_a):
            p = t.part
            r = math.hypot(t.ax, t.az)
            ang = radial(t)
            if p == 'bell' and t.box == 2:                 # stomach in the middle
                return alpha(pick(acc, 1), 190)
            if p in ('bell', 'rim'):
                n = noise3(t.ax, t.ay, t.az, 2.0, 41)
                a = bell_a + (n - 0.5) * 30
                c = pick(bell, 2.5 + (n - 0.5) * 1.5)
                if t.face == 'top':
                    if variant == 'moon' and p == 'bell':
                        # four horseshoe gonads around the centre
                        for k in range(4):
                            ga = k * math.pi / 2 + math.pi / 4
                            gx, gz = math.cos(ga) * 1.8, math.sin(ga) * 1.8
                            d = math.hypot(t.ax - gx, t.az - gz)
                            if 0.6 < d < 1.5:
                                return alpha(pick(acc, 1 + (d < 1.0)), 225)
                    if variant == 'crimson':
                        # sea nettle: dark radial stripes
                        if int((ang + math.pi) / (2 * math.pi) * 16) % 2 == 0 and r > 1.0:
                            return alpha(pick(acc, 1), 210)
                    if variant == 'glow':
                        # glowing spots at the ends of the radial canals
                        if abs(math.sin(ang * 4)) < 0.25 and r > 2.5:
                            return alpha(pick(acc, 2), 235)
                    # faint radial canals
                    if abs(math.sin(ang * 4)) < 0.12 and r > 1.2:
                        return alpha(pick(bell, 1), a + 30)
                    return alpha(c, a)
                if t.face == 'bottom':
                    return alpha(pick(bell, 3), a * 0.65)
                # side faces: the bell margin is denser, the rim edge has a fringe of tiny tentacles
                if p == 'rim':
                    if t.j == t.fh - 1:
                        return alpha(pick(tent, 2 if t.i % 2 else 1), 230)
                    return alpha(pick(bell, 3.5), min(255, a + 60))
                if variant == 'crimson' and int((ang + math.pi) / (2 * math.pi) * 16) % 2 == 0:
                    return alpha(pick(acc, 1), 200)
                return alpha(c, a + 20)
            if p == 'oral_arms':
                # frilly curtains: wavy, lacy edges with holes
                ww = max(t.fw, 1)
                edge = abs((t.i + 0.5) / ww - 0.5) * 2
                wave = 0.75 + 0.2 * math.sin(t.j * 1.3)
                if edge > wave:
                    return None
                if (t.i * 3 + t.j * 5) % 7 == 0:
                    return None
                return alpha(pick(frill, 1 + (t.j % 3 == 0)), 190 if not glow else 210)
            if p.startswith('tentacle'):
                k = 1.5 + (0.8 if (t.j + (2 if p.endswith('end') else 0)) % 3 == 0 else 0)
                return alpha(pick(tent, k), 170 if not glow else 215)
            return None

        img = javamodel.paint(tw, th, boxes, paint)
        tex.save(img, M, 'entity/jellyfish/' + variant)
        out[variant] = img
    return out


# ================================================================================================= anglerfish
def anglerfish_textures():
    tw, th, boxes = javamodel.parse(MODEL_DIR + 'AnglerfishModel.java')
    skin = tex.ramp('#3d3330', 5, 0.3, 0.03)
    belly = tex.ramp('#5b4f4a', 4, 0.3)
    mouth = tex.ramp('#5a1d24', 3, 0.3)
    fang = tex.ramp('#e6e0cc', 3, 0.2)
    lure = tex.ramp('#8ef2d2', 4, 0.35)

    def base(t):
        n = fbm3(t.ax, t.ay, t.az, 2.5, 51)
        d, rid = cells3(t.ax, t.ay, t.az, 1.6, 52)
        k = 2 + (n - 0.5) * 1.8
        if d < 0.25 and rid < 0.5:
            k += 1.4                          # little warts
        if t.ay > 20 and t.face != 'top':
            return pick(belly, k - 0.5)
        return pick(skin, k)

    def paint(t):
        p = t.part
        if p == 'body':
            if t.box == 1:                    # dorsal spines
                return pick(skin, 1)
            if t.box >= 2:                    # upper fangs
                return pick(fang, 2 - t.j)
            if t.face == 'north':
                if t.j >= t.fh - 2:
                    return pick(mouth, 0.5 + (t.i % 2) * 0.5)   # the dark gape behind the teeth
                if t.j == t.fh - 3:
                    return pick(skin, 3)                       # upper lip
            if t.face in ('west', 'east') and t.fz < 0.22 and t.fy < 0.4:
                if t.j == 2 and abs(t.fz - 0.12) < 0.08:
                    return tex.rgb('#a9c4cc')                 # small milky eye
                if t.j in (1, 3) and abs(t.fz - 0.12) < 0.15:
                    return pick(skin, 0)
            if t.face == 'bottom' and t.fz < 0.25:
                return pick(mouth, 1)
            return base(t)
        if p == 'jaw':
            if t.box > 0:
                return pick(fang, 2 - t.j * 0.7)
            if t.face == 'top':
                return pick(mouth, 1 + ((t.i + t.j) % 3 == 0))
            if t.face == 'north' and t.j == 0:
                return pick(skin, 3)
            return base(t)
        if p in ('lure', 'lure_tip'):
            return pick(skin, 2.5 + (t.j % 2) * 0.6)
        if p == 'bulb':
            return pick(lure, 2 + (t.i + t.j) % 2 * 0.6)
        if p in ('tail_fin', 'left_fin', 'right_fin'):
            # translucent-looking fins with darker rays and a ragged edge
            if p == 'tail_fin' and t.face in ('west', 'east') and t.fz > 0.75 and t.j % 3 == 1:
                return None
            return pick(skin, 1.6 if t.j % 2 == 0 else 2.4)
        return base(t)

    img = javamodel.paint(tw, th, boxes, paint)
    tex.save(img, M, 'entity/anglerfish')

    # emissive layer: only the lure bulb and a few photophores along the flanks
    glow = tex.ramp('#9ffff0', 3, 0.3)

    def paint_glow(t):
        if t.part == 'bulb':
            core = 0.3 < t.fx < 0.7 and 0.3 < t.fy < 0.7
            return alpha(glow[2] if core or (t.i + t.j) % 2 else glow[1], 255)
        if t.part == 'body' and t.box == 0 and t.face in ('west', 'east'):
            if t.j == t.fh - 3 and t.i % 3 == 1 and 0.3 < t.fz < 0.9:
                return alpha(glow[1], 220)
        return None

    gimg = javamodel.paint(tw, th, boxes, paint_glow)
    tex.save(gimg, M, 'entity/anglerfish_glow')
    return img, gimg


# ================================================================================================= moray eel
def moray_texture():
    tw, th, boxes = javamodel.parse(MODEL_DIR + 'MorayEelModel.java')
    base_r = tex.ramp('#c2ad62', 5, 0.32, 0.04)
    spot = tex.ramp('#4b3a25', 3, 0.25)
    belly = tex.ramp('#ddd3a4', 3, 0.2)
    mouth = tex.ramp('#d99a9a', 3, 0.3)

    def pattern(t, small=False):
        n = fbm3(t.ax, t.ay, t.az, 2.0, 61)
        d, rid = cells3(t.ax, t.ay, t.az, 1.25 if small else 1.6, 62)
        if d < (0.3 if small else 0.36):
            return pick(spot, 1 + rid)
        if t.face == 'bottom' or (t.face in ('west', 'east') and t.fy > 0.8):
            return pick(belly, 1.5 + (n - 0.5))
        return pick(base_r, 2.4 + (n - 0.5) * 1.8)

    def paint(t):
        p = t.part
        if p == 'jaw':
            if t.face == 'top':
                return pick(mouth, 1)
            if t.face in ('west', 'east', 'north') and t.j == 0 and t.i % 2 == 0:
                return pick(belly, 2)              # tiny teeth along the edge
            return pattern(t, True)
        if p == 'head':
            if t.box == 1:                         # snout
                if t.face == 'north' and t.j == 0 and t.i in (0, t.fw - 1):
                    return pick(spot, 0)           # tube nostrils
                if t.face == 'bottom':
                    return pick(mouth, 1)
                return pattern(t, True)
            if t.face in ('west', 'east') and t.fz < 0.35 and t.j <= 1:
                # yellow eye with a dark pupil
                if abs(t.fz - 0.2) < 0.11:
                    return tex.rgb('#15130f') if t.j == 1 else tex.rgb('#e8d24a')
            if t.face == 'bottom':
                return pick(mouth, 1) if t.fz < 0.9 else pattern(t, True)
            return pattern(t, True)
        if t.w == 0 or t.d == 0 or t.h == 0:
            return None
        # fin seams (zero-width planes): spotted with a dark edge
        return pattern(t)

    def paint_planes(t):
        if t.w == 0:
            if t.j == 0:
                return pick(base_r, 3.6)          # pale fin edge
            return pattern(t)
        return paint(t)

    img = javamodel.paint(tw, th, boxes, paint_planes)
    tex.save(img, M, 'entity/moray_eel')
    return img
