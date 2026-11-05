"""A small software preview of the skins on the player model (three-quarter view, outer layers included), so the
skins can be judged without starting the game. Run: cd tools && python3 -m content.npc.preview3d <out.png>"""
import math
import os
import sys

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..')))

from PIL import Image, ImageDraw  # noqa: E402

from content.npc import characters  # noqa: E402


def boxes(slim):
    aw = 3 if slim else 4
    # (u, v, w, h, d, x0, y0, z0, inflate); model space in pixels, y up, the person looks towards -z
    return [
        (16, 48, 4, 12, 4, 0, 0, -2, 0), (0, 48, 4, 12, 4, 0, 0, -2, 0.25),          # left leg + pants
        (0, 16, 4, 12, 4, -4, 0, -2, 0), (0, 32, 4, 12, 4, -4, 0, -2, 0.25),         # right leg + pants
        (16, 16, 8, 12, 4, -4, 12, -2, 0), (16, 32, 8, 12, 4, -4, 12, -2, 0.25),     # body + jacket
        (40, 16, aw, 12, 4, -4 - aw, 12, -2, 0), (40, 32, aw, 12, 4, -4 - aw, 12, -2, 0.25),  # right arm
        (32, 48, aw, 12, 4, 4, 12, -2, 0), (48, 48, aw, 12, 4, 4, 12, -2, 0.25),     # left arm
        (0, 0, 8, 8, 8, -4, 24, -4, 0), (32, 0, 8, 8, 8, -4, 24, -4, 0.5),           # head + hat
    ]


def faces(box):
    u, v, w, h, d, x0, y0, z0, g = box
    x0, y0, z0 = x0 - g, y0 - g, z0 - g
    x1, y1, z1 = x0 + w + 2 * g, y0 + h + 2 * g, z0 + d + 2 * g
    # each face: texture rect, then the 3D corners for texture (0,0), (w,0), (0,h) -> origin, step-x, step-y
    return [
        ((u + d, v + d, w, h), (x0, y1, z0), (x1, y1, z0), (x0, y0, z0), (0, 0, -1)),          # front
        ((u, v + d, d, h), (x0, y1, z1), (x0, y1, z0), (x0, y0, z1), (-1, 0, 0)),              # right (-x)
        ((u + d + w, v + d, d, h), (x1, y1, z0), (x1, y1, z1), (x1, y0, z0), (1, 0, 0)),       # left (+x)
        ((u + 2 * d + w, v + d, w, h), (x1, y1, z1), (x0, y1, z1), (x1, y0, z1), (0, 0, 1)),   # back
        ((u + d, v, w, d), (x0, y1, z1), (x1, y1, z1), (x0, y1, z0), (0, 1, 0)),               # top
    ]


# the 3D extras of NpcAccessories.java: (u, v, w, h, d, x, y, z) with x, y, z the cube corner in head model space
# (y down from the neck); converted to the preview space (y up, the neck at 24)
EXTRAS = {
    'old_tamo': [(0, 0, 14, 1, 14, -7.0, -5.6, -7.0)],
    'okafor': [(0, 0, 9, 1, 2, -4.5, -6.2, -6.6), (0, 4, 1, 1, 8, -0.5, -9.1, -4.0)],
    'dr_voss': [(0, 0, 3, 2, 3, -1.5, -10.0, 0.5)],
    'nalu': [(0, 0, 1, 2, 2, 4.1, -6.5, -1.5)],
}


def extra_boxes(pid):
    out = []
    for (u, v, w, h, d, x, y, z) in EXTRAS.get(pid, []):
        out.append((u, v, w, h, d, x, 24 - (y + h), z, 0))
    return out


def render(img, slim, yaw=-35.0, pitch=-12.0, scale=7, size=(260, 320), extra=None, extra_img=None):
    out = Image.new('RGBA', size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(out)
    cy, sy = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
    cp, sp = math.cos(math.radians(pitch)), math.sin(math.radians(pitch))

    def rot(p):
        x, y, z = p
        x, z = x * cy - z * sy, x * sy + z * cy
        y, z = y * cp - z * sp, y * sp + z * cp
        return x, y, z

    def proj(p):
        x, y, z = rot(p)
        return size[0] / 2 + x * scale, size[1] - 20 - y * scale, z

    quads = []
    parts = [(b, img) for b in boxes(slim)] + [(b, extra_img) for b in (extra or [])]
    for box, tex_img in parts:
        for (rect, o, ax, ay, n) in faces(box):
            nx, ny, nz = rot(n)
            if nz >= 0:            # facing away from the camera (it looks along +z)
                continue
            rx, ry, rw, rh = rect
            for j in range(rh):
                for i in range(rw):
                    c = tex_img.getpixel((rx + i, ry + j))
                    if c[3] == 0:
                        continue
                    pts = []
                    for (a, b) in ((i, j), (i + 1, j), (i + 1, j + 1), (i, j + 1)):
                        p = tuple(o[k] + (ax[k] - o[k]) * a / rw + (ay[k] - o[k]) * b / rh for k in range(3))
                        pts.append(proj(p))
                    depth = sum(p[2] for p in pts) / 4
                    quads.append((depth, [(p[0], p[1]) for p in pts], c))
    for depth, pts, c in sorted(quads, key=lambda q: -q[0]):
        draw.polygon(pts, fill=c)
    return out


def sheet(path):
    tiles = []
    extras = characters.extras()
    for name, fn in characters.ALL.items():
        s = fn()
        kw = {'extra': extra_boxes(name), 'extra_img': extras.get(name)}
        tiles.append(render(s.img, s.slim, **kw))
        tiles.append(render(s.img, s.slim, yaw=150.0, **kw))
    w, h = tiles[0].size
    big = Image.new('RGBA', (w * 8, h * 2), (96, 150, 186, 255))
    for i, t in enumerate(tiles):
        col, row = (i // 2) % 8, (i % 2)
        big.alpha_composite(t, (col * w, row * h))
    big.save(path)
    return path


if __name__ == '__main__':
    print(sheet(sys.argv[1] if len(sys.argv) > 1 else 'npc_3d.png'))
