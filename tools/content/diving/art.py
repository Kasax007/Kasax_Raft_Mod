"""Shared pixel-art helpers of the diving module: material ramps, shaded primitives and a box-UV painter."""
import math

from bclib import tex

# ----------------------------------------------------------------------------------------------- materials
BRASS = tex.ramp('#c9a046', 6, spread=0.62, hue_shift=0.05)
COPPER = tex.ramp('#c26a42', 5, spread=0.55)
STEEL = tex.ramp('#8c96a0', 6, spread=0.62)
DARK_STEEL = tex.ramp('#4d555e', 5, spread=0.45)
RED = tex.ramp('#b8302a', 5, spread=0.5, hue_shift=0.02)
WHITE = tex.ramp('#e6e1d6', 4, spread=0.25)
TITANIUM = tex.ramp('#a3b3c4', 6, spread=0.6, hue_shift=0.03)
TEAL = tex.ramp('#2aa79b', 5, spread=0.5)
GLASS = tex.ramp('#3f8f9a', 5, spread=0.55, hue_shift=0.06)
NEOPRENE = tex.ramp('#3a404c', 5, spread=0.34, hue_shift=0.02)
YELLOW = tex.ramp('#e3b53a', 5, spread=0.5)
FIN = tex.ramp('#2d8fb0', 5, spread=0.5)
RUBBER = tex.ramp('#2b2e33', 4, spread=0.25)
WEBBING = tex.ramp('#4a4f3a', 4, spread=0.3)
TEAK = tex.ramp('#8a5a34', 5, spread=0.5)
GAUGE_FACE = tex.ramp('#ece6d2', 4, spread=0.25)

LIGHT = (-0.55, -0.65, 0.52)  # top-left, towards the viewer


def _norm(v):
    length = math.sqrt(sum(c * c for c in v)) or 1.0
    return tuple(c / length for c in v)


L = _norm(LIGHT)


def pick(ramp, value):
    """value 0..1 -> ramp colour (quantised)."""
    i = int(round(max(0.0, min(1.0, value)) * (len(ramp) - 1)))
    return ramp[i]


def lambert(n, ambient=0.28, strength=0.85):
    d = -(n[0] * L[0] + n[1] * L[1]) + n[2] * L[2]
    return ambient + strength * max(0.0, d) * 0.9


def sphere(img, cx, cy, rx, ry, ramp, mask=None, bias=0.0):
    """Filled ellipse shaded like a dome lit from the top-left."""
    for y in range(img.height):
        for x in range(img.width):
            dx, dy = (x + 0.5 - cx) / rx, (y + 0.5 - cy) / ry
            r2 = dx * dx + dy * dy
            if r2 > 1.0 or (mask and not mask(x, y)):
                continue
            nz = math.sqrt(max(0.0, 1.0 - r2))
            tex.px(img, x, y, pick(ramp, lambert((dx, dy, nz)) + bias))


def cyl_v(img, x0, x1, y0, y1, ramp, bias=0.0, top_light=0.0):
    """Vertical cylinder (x0..x1 inclusive) shaded across its width."""
    w = x1 - x0 + 1
    for x in range(x0, x1 + 1):
        dx = ((x - x0 + 0.5) / w) * 2 - 1
        nz = math.sqrt(max(0.0, 1 - dx * dx))
        base = lambert((dx, 0.0, nz))
        for y in range(y0, y1 + 1):
            t = (y - y0) / max(1, y1 - y0)
            tex.px(img, x, y, pick(ramp, base + bias + top_light * (0.5 - t)))


def cyl_h(img, x0, x1, y0, y1, ramp, bias=0.0):
    """Horizontal cylinder (y0..y1 inclusive) shaded across its height."""
    h = y1 - y0 + 1
    for y in range(y0, y1 + 1):
        dy = ((y - y0 + 0.5) / h) * 2 - 1
        nz = math.sqrt(max(0.0, 1 - dy * dy))
        c = pick(ramp, lambert((0.0, dy, nz)) + bias)
        for x in range(x0, x1 + 1):
            tex.px(img, x, y, c)


def face_shade(img, x0, y0, w, h, ramp, bias=0.0, curve=0.35, vertical=0.25):
    """A flat face that bulges a little (edges darker, top-left lighter)."""
    for y in range(h):
        for x in range(w):
            dx = ((x + 0.5) / w) * 2 - 1
            dy = ((y + 0.5) / h) * 2 - 1
            v = 0.55 - curve * 0.5 * (dx * dx + dy * dy) - 0.18 * dx * curve - vertical * 0.5 * dy + bias
            tex.px(img, x0 + x, y0 + y, pick(ramp, v))


def ring(img, cx, cy, r_out, r_in, ramp, bias=0.0):
    """A shaded ring (porthole rim), lit from the top-left."""
    for y in range(img.height):
        for x in range(img.width):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            r = math.hypot(dx, dy)
            if r_in <= r <= r_out:
                n = _norm((dx / max(r, 0.01), dy / max(r, 0.01), 0.6))
                tex.px(img, x, y, pick(ramp, lambert(n) + bias))


def disc_glass(img, cx, cy, r, ramp=GLASS):
    """Round glass: dark rim, lighter centre and a two-pixel glint at the top-left."""
    for y in range(img.height):
        for x in range(img.width):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            d = math.hypot(dx, dy)
            if d <= r:
                v = 0.25 + 0.45 * (1 - d / r) - 0.12 * (dy / r)
                tex.px(img, x, y, pick(ramp, v))
    gx, gy = int(cx - r * 0.45), int(cy - r * 0.5)
    tex.px(img, gx, gy, '#d8f4f0')
    if r >= 2.5:
        tex.px(img, gx + 1, gy, ramp[-1])
        tex.px(img, gx, gy + 1, ramp[-1])


# ----------------------------------------------------------------------------------------------- box UV
def box_faces(u, v, w, h, d):
    """Texture rectangles (x, y, width, height) of a model box at texOffs(u, v) with size (w, h, d)."""
    return {
        'top': (u + d, v, w, d),
        'bottom': (u + d + w, v, w, d),
        'right': (u, v + d, d, h),
        'front': (u + d, v + d, w, h),
        'left': (u + d + w, v + d, d, h),
        'back': (u + d + w + d, v + d, w, h),
    }


def paint_box(img, u, v, w, h, d, painter):
    """painter(face, x, y, fw, fh) paints one face rectangle."""
    for face, (x, y, fw, fh) in box_faces(u, v, w, h, d).items():
        if fw > 0 and fh > 0:
            painter(face, x, y, fw, fh)


def fill(img, x, y, w, h, c):
    if w > 0 and h > 0:
        tex.rect(img, x, y, x + w - 1, y + h - 1, c)
