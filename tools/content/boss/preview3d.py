"""Tiny software renderer for the box models (rest pose, orthographic) to look at textures on the real shapes."""
import math

from PIL import Image, ImageDraw

from . import javamodel


def _rot(v, r):
    x, y, z = v
    rx, ry, rz = r
    # ModelPart: rotationZYX(z, y, x) -> v' = Rz * Ry * Rx * v
    c, s = math.cos(rx), math.sin(rx)
    y, z = y * c - z * s, y * s + z * c
    c, s = math.cos(ry), math.sin(ry)
    x, z = x * c + z * s, -x * s + z * c
    c, s = math.cos(rz), math.sin(rz)
    x, y = x * c - y * s, x * s + y * c
    return x, y, z


def _to_model(parts, name, p):
    while name != 'root':
        part = parts[name]
        p = _rot(p, part.rot)
        p = (p[0] + part.pivot[0], p[1] + part.pivot[1], p[2] + part.pivot[2])
        name = part.parent
    return p


def render(java_rel, texture, out_png, views=((210, 25), (150, 25), (270, 0), (0, 80)), size=360, method='createBodyLayer',
           bg=(40, 70, 105, 255), extra_scale=1.0):
    tw, th, boxes, parts = javamodel.parse(java_rel, method, with_parts=True)
    tx = texture if isinstance(texture, Image.Image) else Image.open(texture).convert('RGBA')
    sheet = Image.new('RGBA', (size * len(views), size), bg)
    for vi, (yaw, pitch) in enumerate(views):
        quads = []
        cy, sy = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
        cp, sp = math.cos(math.radians(pitch)), math.sin(math.radians(pitch))

        def cam(p):
            # model space: y down, front -z. World: flip y. Then yaw around y, pitch around x.
            x, y, z = p[0], -p[1], -p[2]
            x, z = x * cy - z * sy, x * sy + z * cy
            y, z = y * cp - z * sp, y * sp + z * cp
            return x, y, z

        light = (-0.4, 0.8, 0.45)
        for b in boxes:
            for face, u0, v0, fw, fh, fn in javamodel._faces(b):
                for j in range(fh):
                    for i in range(fw):
                        c = tx.getpixel((u0 + i, v0 + j)) if 0 <= u0 + i < tw and 0 <= v0 + j < th else (0, 0, 0, 0)
                        if c[3] < 20:
                            continue
                        # corners of the texel on the face
                        corners = []
                        for di, dj in ((0, 0), (1, 0), (1, 1), (0, 1)):
                            p = fn(i - 0.5 + di, j - 0.5 + dj)
                            corners.append(cam(_to_model(parts, b.part, p)))
                        a, bb, cc = corners[0], corners[1], corners[2]
                        ux, uy, uz = bb[0] - a[0], bb[1] - a[1], bb[2] - a[2]
                        vx, vy, vz = cc[0] - a[0], cc[1] - a[1], cc[2] - a[2]
                        n = (uy * vz - uz * vy, uz * vx - ux * vz, ux * vy - uy * vx)
                        ln = math.sqrt(n[0] ** 2 + n[1] ** 2 + n[2] ** 2) or 1
                        n = (n[0] / ln, n[1] / ln, n[2] / ln)
                        shade = 0.55 + 0.45 * abs(n[0] * light[0] + n[1] * light[1] + n[2] * light[2])
                        depth = sum(q[2] for q in corners) / 4
                        col = tuple(int(c[k] * shade) for k in range(3)) + (c[3],)
                        quads.append((depth, corners, col))
        quads.sort(key=lambda q: q[0])
        _, miny, _, _, maxy, _ = javamodel.bounds(boxes)
        img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
        d = ImageDraw.Draw(img, 'RGBA')
        sc = size / 64.0 * extra_scale
        for depth, corners, col in quads:
            pts = [(size / 2 + q[0] * sc, size * 0.62 - (q[1] + 14) * sc) for q in corners]
            d.polygon(pts, fill=col)
        sheet.alpha_composite(img, (vi * size, 0))
    sheet.save(out_png)
    return out_png
