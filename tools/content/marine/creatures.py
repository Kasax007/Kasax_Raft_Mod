"""Entity textures of the marine creatures, painted onto the boxes of the Java models (see javamodel.py)."""
import math

from bclib import tex

from . import javamodel
from .art import noise3, fbm3, cells3, pick, alpha, lerp

M = 'marine'
SHARK_JAVA = 'client/render/SharkModel.java'
MODEL_DIR = 'content/marine/client/model/'


# ================================================================================================= sharks
def _shark_painter(pal, hammer=False, scars=True):
    grey, white, dark = pal['top'], pal['belly'], pal['fin_dark']
    gum, tooth, eye = tex.ramp('#b0485a', 4, 0.35), tex.ramp('#f1ece0', 3, 0.15), tex.rgb('#14161d')
    seed = 11 if hammer else 3

    def boundary(t):
        # wavy countershading line (absolute y, grows downwards); a bit lower towards the tail
        return 18.6 + (t.az * 0.03) + (noise3(t.ax * 0.3, 0, t.az, 3.0, seed) - 0.5) * 1.6

    def skin(t, base=3.0):
        n = fbm3(t.ax, t.ay, t.az, 3.5, seed)
        k = base + (n - 0.5) * 2.4
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
        tipness = max(t.fy if t.part in ('tail_fin_lower',) else 1 - t.fy, 0)
        if t.part in ('left_fin', 'right_fin'):
            tipness = t.fx if t.part == 'left_fin' else 1 - t.fx
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
            return fin(t, False) if t.face != 'bottom' else belly(t)
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
    img = javamodel.paint(tw, th, boxes, _shark_painter(pal))
    tex.save(img, M, 'entity/shark')
    hpal = {'top': tex.ramp('#8a8475', 6, 0.48, 0.03), 'belly': tex.ramp('#ebe9e2', 4, 0.16),
            'fin_dark': tex.ramp('#4d4a40', 3, 0.3)}
    tw, th, boxes = javamodel.parse(SHARK_JAVA, 'createHammerheadLayer')
    img2 = javamodel.paint(tw, th, boxes, _shark_painter(hpal, hammer=True, scars=False))
    tex.save(img2, M, 'entity/hammerhead_shark')
    return img, img2
