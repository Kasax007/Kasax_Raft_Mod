"""Reads the box models of the marine creatures straight from the Java sources (createBodyLayer methods) and paints
their textures face by face, so geometry (Java) and textures (Python) can never drift apart.

A painter is a function paint(t) -> RGBA or None, called for every texel of every box face. t is a Texel with:
  part, box (index in the part), face ('top','bottom','north','south','west','east'),
  fx, fy, fz   position inside the box, 0..1 (y = 0 is the top of the box, z = 0 its front),
  ax, ay, az   approximate absolute model position (pivots summed, rotations ignored; y grows downwards),
  i, j, fw, fh texel inside the face and the face size,
  w, h, d      box size.
"""
import os
import re

from PIL import Image

from bclib.paths import ROOT

JAVA = os.path.join(ROOT, 'src', 'main', 'java', 'net', 'kasax', 'raft')

_NUM = r'(-?[0-9.]+)F?'


class Box:
    def __init__(self, part, idx, u, v, x, y, z, w, h, d, mirror):
        self.part, self.idx = part, idx
        self.u, self.v = u, v
        self.x, self.y, self.z, self.w, self.h, self.d = x, y, z, w, h, d
        self.mirror = mirror
        self.abs = (0.0, 0.0, 0.0)


class Part:
    def __init__(self, name, parent, pivot, rot):
        self.name, self.parent, self.pivot, self.rot = name, parent, pivot, rot


class Texel:
    __slots__ = ('part', 'box', 'face', 'fx', 'fy', 'fz', 'ax', 'ay', 'az', 'i', 'j', 'fw', 'fh', 'w', 'h', 'd')


def _matching_paren(s, start):
    depth = 0
    for k in range(start, len(s)):
        if s[k] == '(':
            depth += 1
        elif s[k] == ')':
            depth -= 1
            if depth == 0:
                return k
    raise ValueError('unbalanced')


def parse(java_rel, method='createBodyLayer', with_parts=False):
    """Returns (tex_w, tex_h, [Box]) of one createXxxLayer method of a Java model file
    (plus {name: Part} with pivots and rotations if with_parts)."""
    src = open(os.path.join(JAVA, java_rel), encoding='utf-8').read()
    m = re.search(r'LayerDefinition\s+' + method + r'\s*\(\)\s*\{', src)
    if not m:
        raise ValueError('no %s in %s' % (method, java_rel))
    body_end = src.index('return LayerDefinition.create', m.end())
    body = src[m.end():src.index(';', body_end) + 1]
    tw, th = map(int, re.search(r'LayerDefinition\.create\(mesh,\s*(\d+),\s*(\d+)\)', body).groups())
    pivots = {'root': (0.0, 0.0, 0.0)}
    var_part = {'root': 'root'}
    boxes = []
    parts = {}
    for cm in re.finditer(r'(?:PartDefinition\s+(\w+)\s*=\s*)?(\w+)\.addOrReplaceChild\(\s*"(\w+)"', body):
        var, parent_var, name = cm.group(1), cm.group(2), cm.group(3)
        call_start = body.index('(', cm.start(2) + len(parent_var))
        call = body[call_start:_matching_paren(body, call_start) + 1]
        pm = re.search(r'PartPose\.(offset|offsetAndRotation)\(' + r',\s*'.join([_NUM] * 3), call)
        piv = tuple(float(g) for g in pm.groups()[1:4]) if pm else (0.0, 0.0, 0.0)
        rm = re.search(r'PartPose\.offsetAndRotation\(' + r',\s*'.join([_NUM] * 6), call)
        rot = tuple(float(g) for g in rm.groups()[3:6]) if rm else (0.0, 0.0, 0.0)
        parent = var_part.get(parent_var, 'root')
        parts[name] = Part(name, parent, piv, rot)
        pp = pivots[parent]
        apiv = (pp[0] + piv[0], pp[1] + piv[1], pp[2] + piv[2])
        pivots[name] = apiv
        if var:
            var_part[var] = name
        idx = 0
        for bm in re.finditer(r'\.texOffs\((\d+),\s*(\d+)\)(\.mirror\(\))?\.addBox\(' + r',\s*'.join([_NUM] * 6), call):
            u, v = int(bm.group(1)), int(bm.group(2))
            mir = bool(bm.group(3))
            x, y, z, w, h, d = (float(g) for g in bm.groups()[3:9])
            b = Box(name, idx, u, v, x, y, z, w, h, d, mir)
            b.abs = apiv
            boxes.append(b)
            idx += 1
    if with_parts:
        return tw, th, boxes, parts
    return tw, th, boxes


def _faces(b):
    """(face, u0, v0, fw, fh, fn(i, j) -> local (x, y, z)) for the six faces of a box."""
    u, v, w, h, d = b.u, b.v, int(round(b.w)), int(round(b.h)), int(round(b.d))
    x0, x1 = b.x, b.x + b.w
    if b.mirror:
        x0, x1 = x1, x0
    sx = 1 if x1 >= x0 else -1
    y0, z0, z1 = b.y, b.z, b.z + b.d
    return [
        ('top', u + d, v, w, d, lambda i, j: (x0 + sx * (i + .5), y0, z1 - (j + .5))),
        ('bottom', u + d + w, v, w, d, lambda i, j: (x0 + sx * (i + .5), y0 + b.h, z1 - (j + .5))),
        ('west', u, v + d, d, h, lambda i, j: (x0, y0 + j + .5, z1 - (i + .5))),
        ('north', u + d, v + d, w, h, lambda i, j: (x0 + sx * (i + .5), y0 + j + .5, z0)),
        ('east', u + d + w, v + d, d, h, lambda i, j: (x1, y0 + j + .5, z0 + i + .5)),
        ('south', u + 2 * d + w, v + d, w, h, lambda i, j: (x1 - sx * (i + .5), y0 + j + .5, z1)),
    ]


def paint(tw, th, boxes, painter, parts=None):
    """Paint all boxes (or only those of the given parts) into a new tw x th image."""
    img = Image.new('RGBA', (tw, th), (0, 0, 0, 0))
    t = Texel()
    for b in boxes:
        if parts and b.part not in parts:
            continue
        for face, u0, v0, fw, fh, fn in _faces(b):
            for j in range(fh):
                for i in range(fw):
                    x, y, z = fn(i, j)
                    t.part, t.box, t.face = b.part, b.idx, face
                    t.fx = (min(x, b.x + b.w) - b.x) / b.w if b.w else 0.5
                    t.fy = (y - b.y) / b.h if b.h else 0.5
                    t.fz = (z - b.z) / b.d if b.d else 0.5
                    t.ax, t.ay, t.az = b.abs[0] + x, b.abs[1] + y, b.abs[2] + z
                    t.i, t.j, t.fw, t.fh = i, j, fw, fh
                    t.w, t.h, t.d = b.w, b.h, b.d
                    c = painter(t)
                    if c is not None and 0 <= u0 + i < tw and 0 <= v0 + j < th:
                        img.putpixel((u0 + i, v0 + j), tuple(c))
    return img


def bounds(boxes, parts=None):
    """Absolute model-space bounds (minx, miny, minz, maxx, maxy, maxz) of the given parts."""
    xs, ys, zs = [], [], []
    for b in boxes:
        if parts and b.part not in parts:
            continue
        ax, ay, az = b.abs
        xs += [ax + b.x, ax + b.x + b.w]
        ys += [ay + b.y, ay + b.y + b.h]
        zs += [az + b.z, az + b.z + b.d]
    return min(xs), min(ys), min(zs), max(xs), max(ys), max(zs)
