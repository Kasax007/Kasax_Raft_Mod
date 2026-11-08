"""Model helper shared by the cozy, food and gear modules (Beach Craft 3.3).

Builds on the decor module's Model (cuboids in pixels, front faces north, auto-UV wrapped into 0..16) and adds:
  * the module the files go to (cozy / food / gear),
  * automatic removal of hidden faces (a face lying inside or on the surface of another solid box), so touching
    parts have no doubled, flickering faces,
  * a lint that reports faces sharing a plane with the same direction (z-fighting) and transparent texels on the
    faces of solid boxes (ragged edges),
  * one shape registry per module, written to <Name>ShapeData.java for the Java blocks.
"""
import os
from collections import OrderedDict

from PIL import Image

from bclib import assets as A
from bclib import paths
from content.decor import mdl as _mdl

NS = 'raft'
rid = A.rid
auto_uv = _mdl.auto_uv
FACES = _mdl.FACES

SHAPES = OrderedDict()   # (module, name) -> boxes
LINT = []                # warnings of the last run
_AXIS = {'down': (1, 0), 'up': (1, 1), 'north': (2, 0), 'south': (2, 1), 'west': (0, 0), 'east': (0, 1)}


class Model(_mdl.Model):
    """A block model of `module`. Same API as content.decor.mdl.Model (box, plane_z, plane_x, cross, write)."""

    def __init__(self, module, textures, ao=False, parent='minecraft:block/block', particle=None, opaque=None):
        super().__init__(textures, ao=ao, parent=parent, particle=particle)
        self.module = module
        # texture keys whose faces must be fully opaque (default: all keys of solid boxes are checked)
        self.opaque = opaque
        self._solid = []   # indices of elements that hide what is inside them

    def box(self, frm, to, tex, *args, solid=True, **kw):
        e = super().box(frm, to, tex, *args, **kw)
        is_box = all(float(to[i]) - float(frm[i]) > 0.01 for i in range(3))
        if solid and is_box and not kw.get('rot'):
            self._solid.append(len(self.elements) - 1)
        return e

    # ------------------------------------------------------------------------------------------- cleaning
    def _cull_hidden(self):
        boxes = [(i, self.elements[i]['from'], self.elements[i]['to']) for i in self._solid]
        for idx, e in enumerate(self.elements):
            if 'rotation' in e:
                continue
            f0, t0 = e['from'], e['to']
            for face in list(e['faces'].keys()):
                ax, side = _AXIS[face]
                plane = t0[ax] if side else f0[ax]
                others = [i for i in range(3) if i != ax]
                for j, bf, bt in boxes:
                    if j == idx:
                        continue
                    # the other box must extend beyond the face (outwards) and cover it completely
                    if side and not (bf[ax] <= plane < bt[ax]):
                        continue
                    if not side and not (bf[ax] < plane <= bt[ax]):
                        continue
                    if all(bf[o] <= f0[o] + 1e-6 and bt[o] >= t0[o] - 1e-6 for o in others):
                        del e['faces'][face]
                        break
        self.elements = [e for e in self.elements if e['faces']]

    def _lint(self, name):
        faces = []
        for idx, e in enumerate(self.elements):
            if 'rotation' in e:
                continue
            for face in e['faces']:
                ax, side = _AXIS[face]
                plane = e['to'][ax] if side else e['from'][ax]
                others = [i for i in range(3) if i != ax]
                rect = [(e['from'][o], e['to'][o]) for o in others]
                faces.append((face, round(plane, 4), rect, idx))
        for a in range(len(faces)):
            for b in range(a + 1, len(faces)):
                fa, pa, ra, ia = faces[a]
                fb, pb, rb, ib = faces[b]
                if fa != fb or pa != pb or ia == ib:
                    continue
                ov = all(min(ra[k][1], rb[k][1]) - max(ra[k][0], rb[k][0]) > 1e-6 for k in range(2))
                if ov:
                    LINT.append(f'{self.module}:{name}: z-fighting {fa} faces of elements {ia} and {ib} at {pa}')
        # transparent texels on solid boxes
        for idx in self._solid:
            if idx >= len(self.elements):
                continue
        for e in self.elements:
            if 'rotation' in e:
                continue
            size = [e['to'][i] - e['from'][i] for i in range(3)]
            if min(size) < 0.01:
                continue   # planes may be cut out
            for face, d in e['faces'].items():
                key = d['texture'][1:]
                if self.opaque is not None and key not in self.opaque:
                    continue
                tid = self.textures.get(key)
                if not tid or tid.startswith('#'):
                    continue
                img = _texture(tid)
                if img is None:
                    continue
                u0, v0, u1, v1 = d['uv']
                w, h = img.size
                sx, sy = w / 16.0, h / 16.0
                xa, xb = sorted((u0, u1))
                ya, yb = sorted((v0, v1))
                for y in range(int(ya * sy), max(int(ya * sy) + 1, int(yb * sy + 0.999))):
                    for x in range(int(xa * sx), max(int(xa * sx) + 1, int(xb * sx + 0.999))):
                        if img.getpixel((min(x, w - 1), min(y, h - 1)))[3] < 255:
                            LINT.append(f'{self.module}:{name}: transparent texel on {face} face ({tid})')
                            break
                    else:
                        continue
                    break

    def write(self, name, shape_name=None, shape=None):
        self._cull_hidden()
        self._lint(name)
        d = OrderedDict(parent=self.parent)
        if not self.ao:
            d['ambientocclusion'] = False
        d['textures'] = self.textures
        if self.display:
            d['display'] = self.display
        d['elements'] = self.elements
        A.write_json(self.module, f'assets/{NS}/models/block/{name}.json', d)
        if shape_name is not None:
            boxes = shape if shape is not None else self.shape
            SHAPES[(self.module, shape_name)] = [tuple(round(float(v), 3) for v in b) for b in boxes]
        return rid(f'block/{name}')


_TEX_CACHE = {}


def _texture(tid):
    if tid in _TEX_CACHE:
        return _TEX_CACHE[tid]
    p = paths.find_texture(tid)
    img = Image.open(p).convert('RGBA') if p else None
    if img is not None and img.height > img.width:
        img = img.crop((0, 0, img.width, img.width))   # animated strip: first frame
    _TEX_CACHE[tid] = img
    return img


def forget_textures():
    _TEX_CACHE.clear()


def shape(module, name, boxes):
    SHAPES[(module, name)] = [tuple(float(v) for v in b) for b in boxes]


def facing_variants(model, props=None, base_rot=0):
    v = OrderedDict()
    for f, r in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
        key = 'facing=' + f + (',' + props if props else '')
        e = OrderedDict(model=rid(model))
        rr = (r + base_rot) % 360
        if rr:
            e['y'] = rr
        v[key] = e
    return v


GUI_SMALL = {
    'gui': {'rotation': [30, 225, 0], 'translation': [0, 0, 0], 'scale': [0.625, 0.625, 0.625]},
    'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.25, 0.25, 0.25]},
    'fixed': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [0.5, 0.5, 0.5]},
    'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.375, 0.375, 0.375]},
    'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 0, 0], 'scale': [0.4, 0.4, 0.4]},
    'firstperson_lefthand': {'rotation': [0, 225, 0], 'translation': [0, 0, 0], 'scale': [0.4, 0.4, 0.4]},
}


def block_item(module, name, model, scale=0.625, lift=0.0, rot=(30, 225, 0)):
    """Item that shows a block model, with its own GUI scale (small decorations are drawn bigger)."""
    d = {k: dict(v) for k, v in GUI_SMALL.items()}
    d['gui'] = {'rotation': list(rot), 'translation': [0, lift, 0], 'scale': [scale, scale, scale]}
    A.item_model(module, name, parent=rid(model), display=d)
    A.item_def(module, name)


def write_java_shapes(module, package, cls, path):
    lines = [f'package {package};', '',
             'import java.util.Map;', '',
             '/**', f' * Shape boxes of the {module} models in pixels, front facing north (generated by',
             f' * tools/content/{module}/gen.py from the same boxes as the block models - do not edit by hand).', ' */',
             f'public final class {cls} {{',
             f'    private {cls}() {{', '    }', '']
    chunks = [(n, b) for (m, n), b in SHAPES.items() if m == module]
    per = 40
    for i in range(0, len(chunks), per):
        lines.append(f'    private static void fill{i // per}(Map<String, double[][]> map) {{')
        for name, boxes in chunks[i:i + per]:
            arr = ', '.join('{' + ', '.join(_num(v) for v in b) + '}' for b in boxes)
            lines.append(f'        map.put("{name}", new double[][]{{{arr}}});')
        lines.append('    }')
        lines.append('')
    lines.append('    /** Adds every shape of this module to the map. */')
    lines.append('    public static void fill(Map<String, double[][]> map) {')
    for i in range(0, len(chunks), per):
        lines.append(f'        fill{i // per}(map);')
    lines.append('    }')
    lines.append('}')
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        f.write('\n'.join(lines) + '\n')


def _num(v):
    s = ('%.3f' % v).rstrip('0').rstrip('.')
    return s if '.' in s else s + '.0'


def report_lint(module):
    mine = [w for w in LINT if w.startswith(module + ':')]
    for w in mine:
        print('LINT', w)
    return len(mine)
