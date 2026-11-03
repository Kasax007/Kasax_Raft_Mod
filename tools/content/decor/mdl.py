"""Small block-model builder for the decor module.

A Model collects cuboid elements (pixel coordinates, the model's front faces north) and the boxes of its
VoxelShape. UVs default to the vanilla auto-UV (taken from the element position) and are wrapped into 0..16, so
"material" textures (wood, iron, cloth) can be used on any part. Faces may be overridden with explicit UVs.
"""
import math
from collections import OrderedDict

from bclib import assets as A

M = 'decor'
NS = 'raft'
rid = A.rid

FACES = ('down', 'up', 'north', 'south', 'west', 'east')

# name -> list of shape boxes (x0, y0, z0, x1, y1, z1) in pixels; written to DecorShapeData.java by gen.py
SHAPES = OrderedDict()


def _wrap(a, b):
    """Move an interval into 0..16 (keeping its length), stretch it if it is longer than the texture."""
    lo, hi = min(a, b), max(a, b)
    span = hi - lo
    if span > 16:
        n = (0.0, 16.0)
    elif lo >= 0 and hi <= 16:
        n = (lo, hi)
    else:
        l2 = lo % 16
        if l2 + span > 16:
            l2 = 16 - span
        n = (l2, l2 + span)
    return n if a <= b else (n[1], n[0])


def auto_uv(face, frm, to):
    x0, y0, z0 = frm
    x1, y1, z1 = to
    uv = {
        'down': [x0, 16 - z1, x1, 16 - z0], 'up': [x0, z0, x1, z1],
        'north': [16 - x1, 16 - y1, 16 - x0, 16 - y0], 'south': [x0, 16 - y1, x1, 16 - y0],
        'west': [z0, 16 - y1, z1, 16 - y0], 'east': [16 - z1, 16 - y1, 16 - z0, 16 - y0],
    }[face]
    u0, u1 = _wrap(uv[0], uv[2])
    v0, v1 = _wrap(uv[1], uv[3])
    return [round(u0, 3), round(v0, 3), round(u1, 3), round(v1, 3)]


class Model:
    def __init__(self, textures, ao=False, parent='minecraft:block/block', particle=None):
        self.textures = OrderedDict((k, rid(v) if not v.startswith('#') else v) for k, v in textures.items())
        if 'particle' not in self.textures:
            self.textures['particle'] = particle and rid(particle) or '#' + next(iter(textures))
        self.ao = ao
        self.parent = parent
        self.elements = []
        self.shape = []
        self.display = None

    def box(self, frm, to, tex, faces=None, skip=(), only=None, rot=None, light=None, shade=None, cull=False,
            collide=True, uv=None, tint=None, flip=()):
        """tex: texture key for all faces. faces: {face: key | (key, uv) | (key, uv, rotation)} overrides.
        skip/only: which faces to write. rot: (axis, angle, origin) or {'x':..,'y':..,'z':..,'origin':..}.
        uv: {face: [u0,v0,u1,v1]} UV overrides that keep `tex`. flip: faces whose auto-UV is mirrored."""
        frm = [float(v) for v in frm]
        to = [float(v) for v in to]
        fs = OrderedDict()
        names = only or FACES
        for f in names:
            if f in skip:
                continue
            spec = (faces or {}).get(f, tex)
            if spec is None:
                continue
            frot = None
            if isinstance(spec, tuple):
                key, fuv = spec[0], spec[1]
                if len(spec) > 2:
                    frot = spec[2]
            else:
                key, fuv = spec, (uv or {}).get(f)
            if fuv is None:
                fuv = auto_uv(f, frm, to)
                if f in flip:
                    fuv = [fuv[2], fuv[1], fuv[0], fuv[3]]
            d = OrderedDict(uv=[round(v, 3) for v in fuv], texture='#' + key)
            if frot:
                d['rotation'] = frot
            if cull:
                edge = {'down': frm[1] == 0, 'up': to[1] == 16, 'north': frm[2] == 0, 'south': to[2] == 16,
                        'west': frm[0] == 0, 'east': to[0] == 16}
                if edge[f]:
                    d['cullface'] = f
            if tint is not None:
                d['tintindex'] = tint
            fs[f] = d
        e = OrderedDict()
        e['from'] = [round(v, 4) for v in frm]
        e['to'] = [round(v, 4) for v in to]
        if rot:
            if isinstance(rot, dict):
                r = OrderedDict(rot)
                r['origin'] = [float(v) for v in r['origin']]
                e['rotation'] = r
            else:
                axis, angle, origin = rot[:3]
                r = OrderedDict(origin=[float(v) for v in origin], axis=axis, angle=angle)
                if len(rot) > 3 and rot[3]:
                    r['rescale'] = True
                e['rotation'] = r
        if light:
            e['light_emission'] = light
        if shade:
            e['shade_direction_override'] = shade
        e['faces'] = fs
        self.elements.append(e)
        if collide and not rot and all(to[i] - frm[i] > 0.01 for i in range(3)):
            self.shape.append(tuple(frm + to))
        return e

    def plane_z(self, x0, y0, x1, y1, z, tex, uv=None, light=None, double=True, rot=None, shade='up'):
        """A flat picture in the XY plane at depth z (visible from north and south)."""
        if uv is None:
            faces = {'north': tex, 'south': tex}
        else:
            faces = {'north': (tex, list(uv)), 'south': (tex, [uv[2], uv[1], uv[0], uv[3]])}
        return self.box((x0, y0, z), (x1, y1, z), tex, faces=faces, only=('north', 'south') if double else ('north',),
                        light=light, rot=rot, collide=False, shade=shade)

    def plane_x(self, z0, y0, z1, y1, x, tex, uv=None, light=None, rot=None, shade='up'):
        if uv is None:
            faces = {'west': tex, 'east': tex}
        else:
            faces = {'west': (tex, list(uv)), 'east': (tex, [uv[2], uv[1], uv[0], uv[3]])}
        return self.box((x, y0, z0), (x, y1, z1), tex, faces=faces, only=('west', 'east'), light=light, rot=rot,
                        collide=False, shade=shade)

    def cross(self, cx, cz, half, y0, y1, tex, uv=None, light=None):
        """Two crossed planes (like flowers / fire) centred at cx, cz."""
        u = uv or [0, 0, 16, 16]
        for ang in (45, -45):
            self.box((cx - half, y0, cz), (cx + half, y1, cz), tex,
                     faces={'north': (tex, u), 'south': (tex, u)}, only=('north', 'south'),
                     rot=('y', ang, (cx, y0, cz), True), light=light, collide=False, shade='up')

    def write(self, name, shape_name=None, shape=None):
        d = OrderedDict(parent=self.parent)
        if not self.ao:
            d['ambientocclusion'] = False
        d['textures'] = self.textures
        if self.display:
            d['display'] = self.display
        d['elements'] = self.elements
        A.write_json(M, f'assets/{NS}/models/block/{name}.json', d)
        if shape_name is not None:
            SHAPES[shape_name] = [tuple(round(v, 3) for v in b) for b in (shape if shape is not None else self.shape)]
        return rid(f'block/{name}')


def shape(name, boxes):
    SHAPES[name] = [tuple(float(v) for v in b) for b in boxes]


# ------------------------------------------------------------------------------------------------ blockstates
def facing_variants(model, extra=None, props=None):
    """{facing=..[,props]: model rotated} for a model whose front faces north."""
    v = OrderedDict()
    for f, r in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
        key = 'facing=' + f + (',' + props if props else '')
        e = OrderedDict(model=rid(model))
        if r:
            e['y'] = r
        v[key] = e
    return v


def write_java_shapes(path):
    lines = ['package net.kasax.raft.content.decor.block;', '',
             'import java.util.HashMap;', 'import java.util.Map;', '',
             '/**', ' * Shape boxes of the decor models in pixels, front facing north (generated by',
             ' * tools/content/decor/gen.py from the same boxes as the block models - do not edit by hand).', ' */',
             'public final class DecorShapeData {', '    private static final Map<String, double[][]> BOXES = new HashMap<>();',
             '', '    private DecorShapeData() {', '    }', '',
             '    /** The boxes of a shape, {x0, y0, z0, x1, y1, z1} each. */',
             '    public static double[][] get(String name) {',
             '        double[][] boxes = BOXES.get(name);',
             '        if (boxes == null) throw new IllegalArgumentException("Unknown decor shape " + name);',
             '        return boxes;', '    }', '']
    chunks = list(SHAPES.items())
    # split into several init methods to stay below the JVM method size limit
    per = 40
    for i in range(0, len(chunks), per):
        lines.append(f'    private static void init{i // per}() {{')
        for name, boxes in chunks[i:i + per]:
            arr = ', '.join('{' + ', '.join(_num(v) for v in b) + '}' for b in boxes)
            lines.append(f'        BOXES.put("{name}", new double[][]{{{arr}}});')
        lines.append('    }')
        lines.append('')
    lines.append('    static {')
    for i in range(0, len(chunks), per):
        lines.append(f'        init{i // per}();')
    lines.append('    }')
    lines.append('}')
    with open(path, 'w') as f:
        f.write('\n'.join(lines) + '\n')


def _num(v):
    s = ('%.3f' % v).rstrip('0').rstrip('.')
    return s if '.' in s else s + '.0'
