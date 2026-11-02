"""Painting entity textures on Minecraft box UV nets (shore module).

A box made with texOffs(u, v).addBox(x, y, z, w, h, d) uses this net on the texture:

    top    (u+d,     v,   w, d)   bottom (u+d+w, v, w, d)
    right  (u,       v+d, d, h)   front  (u+d, v+d, w, h)   left (u+d+w, v+d, d, h)   back (u+2d+w, v+d, w, h)

'right' is the creature's right side (-x); its image right edge touches the front face. 'left' is +x; its image
left edge touches the front. On the top face the bottom image row is the front edge. Light comes from the top-left
(art direction), so top faces are lightest, sides get darker towards the bottom and bottoms are darkest.
"""
import math
import random

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


def new(w=64, h=64):
    return Image.new('RGBA', (w, h), (0, 0, 0, 0))


def put(img, x, y, c):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), tex.rgb(c))


def get(img, x, y):
    return img.getpixel((x, y))


def fill_rect(img, rect, fn):
    """fn(x, y, fx, fy, w, h) -> colour or None for each pixel of rect (fx, fy = position inside the rect)."""
    rx, ry, rw, rh = rect
    for fy in range(rh):
        for fx in range(rw):
            c = fn(rx + fx, ry + fy, fx, fy, rw, rh)
            if c is not None:
                put(img, rx + fx, ry + fy, c)


def noise(seed, w=64, h=64, cell=3):
    return tex.value_noise(w, h, seed, cell, 2)


def shade(ramp, value):
    """value 0..1 -> colour of the ramp (dark -> light)."""
    k = len(ramp)
    return ramp[max(0, min(k - 1, int(round(value * (k - 1)))))]


def material_box(img, box, ramp, seed=1, top=0.78, side=0.55, bottom=0.25, grad=0.22, mottling=0.16, faces=None):
    """Fill the faces of a box with a ramp: lit top, sides darkening towards the bottom, dark bottom, gentle mottling
    (value noise, never single-pixel salt)."""
    n = noise(seed, img.width, img.height, cell=3)
    base = {'top': top, 'bottom': bottom, 'front': side + 0.04, 'right': side, 'left': side - 0.04, 'back': side - 0.08}
    for name, rect in box.faces().items():
        if faces is not None and name not in faces:
            continue
        b = base[name]

        def fn(x, y, fx, fy, w, h, b=b, name=name):
            v = b
            if name in ('front', 'right', 'left', 'back') and h > 1:
                v += grad * (0.5 - fy / (h - 1))
            v += (n[y][x] - 0.5) * mottling * 2
            return shade(ramp, v)
        fill_rect(img, rect, fn)


def outline_face(img, rect, colour, sides=('l', 'r', 't', 'b')):
    rx, ry, rw, rh = rect
    for fx in range(rw):
        if 't' in sides:
            put(img, rx + fx, ry, colour)
        if 'b' in sides:
            put(img, rx + fx, ry + rh - 1, colour)
    for fy in range(rh):
        if 'l' in sides:
            put(img, rx, ry + fy, colour)
        if 'r' in sides:
            put(img, rx + rw - 1, ry + fy, colour)


def darken_region(img, rect, f=0.82):
    rx, ry, rw, rh = rect
    for y in range(ry, ry + rh):
        for x in range(rx, rx + rw):
            c = img.getpixel((x, y))
            if c[3]:
                img.putpixel((x, y), tex.darker(c, f))


def tint_region(img, rect, colour, amount):
    rx, ry, rw, rh = rect
    for y in range(ry, ry + rh):
        for x in range(rx, rx + rw):
            c = img.getpixel((x, y))
            if c[3]:
                img.putpixel((x, y), tex.mix(c, colour, amount))


def upscale_preview(img, out_png, scale=8, bg=(48, 52, 60, 255)):
    big = img.resize((img.width * scale, img.height * scale), Image.NEAREST)
    canvas = Image.new('RGBA', big.size, bg)
    canvas.alpha_composite(big)
    canvas.save(out_png)
    return out_png


def rng(seed):
    return random.Random(seed)


def dist(a, b):
    return math.hypot(a[0] - b[0], a[1] - b[1])
