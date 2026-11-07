"""Block and item models from Minecraft 26.3 to 1.21.1.

* texture entries may be objects in 26.3 ({"sprite": ..., "force_translucent": ...}): 1.21.1 takes the sprite id.
* element rotation: 1.21.1 has one axis and the angles -45, -22.5, 0, 22.5, 45. A 26.3 rotation about several
  axes keeps its largest one; an angle is snapped to the nearest allowed one, and whole quarter turns beyond 45 are
  turned into the element's geometry (exact for the box, faces follow their new directions).
* "shade_direction_override" and "light_emission" are not known to 1.21.1 and are dropped.
* item_overrides(): 26.3 item model definitions (condition / range_dispatch / select) as 1.21.1 overrides; the
  predicates are registered in Java (ItemProperties): raft:lamp (has the diving lamp), raft:cooldown, raft:gui.
"""
import copy
import math

ALLOWED = (-45.0, -22.5, 0.0, 22.5, 45.0)
DIRS = {'north': (0, 0, -1), 'south': (0, 0, 1), 'east': (1, 0, 0), 'west': (-1, 0, 0), 'up': (0, 1, 0), 'down': (0, -1, 0)}


def _snap(a):
    return min(ALLOWED, key=lambda x: abs(x - a))


def _rot90(v, axis, k, origin):
    """Turn point v by k quarter turns about axis through origin (right hand rule, like the model rotation)."""
    x, y, z = (v[0] - origin[0], v[1] - origin[1], v[2] - origin[2])
    for _ in range(k % 4):
        if axis == 'x':
            y, z = -z, y
        elif axis == 'y':
            x, z = z, -x
        else:
            x, y = -y, x
    return [x + origin[0], y + origin[1], z + origin[2]]


def _dir_after(d, axis, k):
    v = _rot90(DIRS[d], axis, k, (0, 0, 0))
    for name, w in DIRS.items():
        if tuple(round(c) for c in v) == w:
            return name
    return d


def element(e):
    e = copy.deepcopy(e)
    e.pop('light_emission', None)
    for f in e.get('faces', {}).values():
        f.pop('shade_direction_override', None)
    r = e.get('rotation')
    if not r:
        return e
    origin = r.get('origin', [8, 8, 8])
    if 'axis' in r:
        axis, angle = r['axis'], float(r.get('angle', 0))
    else:
        cands = [(abs(float(r.get(a, 0))), a, float(r.get(a, 0))) for a in ('x', 'y', 'z')]
        _, axis, angle = max(cands)
    k = int(round(angle / 90.0))
    rest = angle - 90.0 * k
    if k:
        corners = [_rot90(p, axis, k, origin) for p in (e['from'], e['to'])]
        e['from'] = [min(corners[0][i], corners[1][i]) for i in range(3)]
        e['to'] = [max(corners[0][i], corners[1][i]) for i in range(3)]
        e['faces'] = {_dir_after(d, axis, k): f for d, f in e['faces'].items()}
    snapped = _snap(rest)
    nr = {'origin': origin, 'axis': axis, 'angle': snapped}
    if r.get('rescale'):
        nr['rescale'] = True
    if snapped == 0.0 and not k:
        e.pop('rotation')
    else:
        e['rotation'] = nr
    return e


def model(j):
    j = copy.deepcopy(j)
    tex = j.get('textures')
    if isinstance(tex, dict):
        for k, v in list(tex.items()):
            if isinstance(v, dict):
                tex[k] = v.get('sprite')
    if 'elements' in j:
        j['elements'] = [element(e) for e in j['elements']]
    return j


def _model_ref(m):
    if m.get('type') == 'minecraft:model':
        return m['model']
    raise ValueError('nested item model %s' % m.get('type'))


def item_overrides(m):
    """(base model id, overrides list) for a 26.3 item model definition that is not a plain model."""
    t = m.get('type')
    if t == 'minecraft:condition':
        prop = m.get('property')
        if prop == 'minecraft:has_component' and m.get('component') == 'raft:diving_lamp':
            return _model_ref(m['on_false']), [{'predicate': {'raft:lamp': 1}, 'model': _model_ref(m['on_true'])}]
        raise ValueError('condition %s' % prop)
    if t == 'minecraft:range_dispatch':
        prop = m.get('property')
        name = {'minecraft:cooldown': 'raft:cooldown'}.get(prop)
        if name is None:
            raise ValueError('range_dispatch %s' % prop)
        over = [{'predicate': {name: e['threshold']}, 'model': _model_ref(e['model'])} for e in m.get('entries', [])]
        return _model_ref(m['fallback']), over
    if t == 'minecraft:select':
        prop = m.get('property')
        if prop == 'minecraft:display_context':
            # 1.21.1 picks the model per display context in Java (ItemRendererMixin): the fallback (in hand) is the
            # base, the GUI model is listed under the raft:gui predicate (1 while drawn in a GUI)
            gui = None
            for c in m.get('cases', []):
                when = c['when'] if isinstance(c['when'], list) else [c['when']]
                if 'gui' in when:
                    gui = _model_ref(c['model'])
            over = [{'predicate': {'raft:gui': 1}, 'model': gui}] if gui else []
            return _model_ref(m['fallback']), over
        raise ValueError('select %s' % prop)
    raise ValueError('item model type %s' % t)
