"""Minimal NBT reader/writer and a Structure builder that saves vanilla structure templates
(data/raft/structure/<name>.nbt, DataVersion of Minecraft 26.3).

Typed values: Python int -> Int, float -> Double, str -> String, bool -> Byte, dict -> Compound,
list -> List (element type from first element). Wrap with Byte/Short/Long/Float/Int for other types.
"""
import gzip
import io
import random
import re
import struct

from .paths import out, NS

DATA_VERSION = 5023  # Minecraft 26.3 world_version


class _Num:
    def __init__(self, v):
        self.v = v

    def __repr__(self):
        return f'{type(self).__name__}({self.v})'

    def __eq__(self, o):
        return type(o) is type(self) and o.v == self.v

    def __hash__(self):
        return hash((type(self).__name__, self.v))


class Byte(_Num): pass
class Short(_Num): pass
class Int(_Num): pass
class Long(_Num): pass
class Float(_Num): pass
class Double(_Num): pass


class TypedList(list):
    """List with an explicit element tag id (e.g. TypedList(6, [...]) for doubles)."""
    def __init__(self, tag, items=()):
        super().__init__(items)
        self.tag = tag


def _tag_of(v):
    if isinstance(v, bool) or isinstance(v, Byte): return 1
    if isinstance(v, Short): return 2
    if isinstance(v, Int) or isinstance(v, int): return 3
    if isinstance(v, Long): return 4
    if isinstance(v, Float): return 5
    if isinstance(v, Double) or isinstance(v, float): return 6
    if isinstance(v, (bytes, bytearray)): return 7
    if isinstance(v, str): return 8
    if isinstance(v, list): return 9
    if isinstance(v, dict): return 10
    raise TypeError(f'no NBT type for {v!r}')


def _w_payload(f, tag, v):
    raw = v.v if isinstance(v, _Num) else v
    if tag == 1: f.write(struct.pack('>b', int(raw)))
    elif tag == 2: f.write(struct.pack('>h', raw))
    elif tag == 3: f.write(struct.pack('>i', raw))
    elif tag == 4: f.write(struct.pack('>q', raw))
    elif tag == 5: f.write(struct.pack('>f', raw))
    elif tag == 6: f.write(struct.pack('>d', raw))
    elif tag == 7: f.write(struct.pack('>i', len(raw))); f.write(bytes(raw))
    elif tag == 8:
        b = raw.encode('utf-8'); f.write(struct.pack('>H', len(b))); f.write(b)
    elif tag == 9:
        et = raw.tag if isinstance(raw, TypedList) else (_tag_of(raw[0]) if raw else 0)
        f.write(struct.pack('>bi', et, len(raw)))
        for e in raw:
            _w_payload(f, et, e)
    elif tag == 10:
        for k, e in raw.items():
            t = _tag_of(e)
            f.write(struct.pack('>b', t)); _w_payload(f, 8, k); _w_payload(f, t, e)
        f.write(b'\x00')
    elif tag == 11:
        f.write(struct.pack('>i', len(raw))); [f.write(struct.pack('>i', x)) for x in raw]
    elif tag == 12:
        f.write(struct.pack('>i', len(raw))); [f.write(struct.pack('>q', x)) for x in raw]


def write(path, root, gz=True):
    buf = io.BytesIO()
    buf.write(b'\x0a'); _w_payload(buf, 8, ''); _w_payload(buf, 10, root)
    data = buf.getvalue()
    with open(path, 'wb') as f:
        f.write(gzip.compress(data, mtime=0) if gz else data)  # mtime 0: identical builds give identical files


def _r(f, tag):
    if tag == 1: return struct.unpack('>b', f.read(1))[0]
    if tag == 2: return struct.unpack('>h', f.read(2))[0]
    if tag == 3: return struct.unpack('>i', f.read(4))[0]
    if tag == 4: return struct.unpack('>q', f.read(8))[0]
    if tag == 5: return struct.unpack('>f', f.read(4))[0]
    if tag == 6: return struct.unpack('>d', f.read(8))[0]
    if tag == 7: n = struct.unpack('>i', f.read(4))[0]; return f.read(n)
    if tag == 8: n = struct.unpack('>H', f.read(2))[0]; return f.read(n).decode('utf-8')
    if tag == 9:
        et, n = struct.unpack('>bi', f.read(5)); return [_r(f, et) for _ in range(n)]
    if tag == 10:
        d = {}
        while True:
            t = f.read(1)[0]
            if t == 0: return d
            k = _r(f, 8); d[k] = _r(f, t)
    if tag == 11: n = struct.unpack('>i', f.read(4))[0]; return list(struct.unpack('>%di' % n, f.read(4 * n)))
    if tag == 12: n = struct.unpack('>i', f.read(4))[0]; return list(struct.unpack('>%dq' % n, f.read(8 * n)))
    raise ValueError(tag)


def read(path):
    raw = open(path, 'rb').read()
    if raw[:2] == b'\x1f\x8b':
        raw = gzip.decompress(raw)
    f = io.BytesIO(raw)
    t = f.read(1)[0]; _r(f, 8)
    return _r(f, t)


# ----------------------------------------------------------------------------------------------- structures
_STATE = re.compile(r'^([a-z0-9_.\-]+:)?([a-z0-9_/.\-]+)(\[(.*)\])?$')


def parse_state(s):
    """'raft:tiki_torch[half=upper]' -> ('raft:tiki_torch', {'half': 'upper'}). No namespace = minecraft."""
    m = _STATE.match(s.strip())
    if not m:
        raise ValueError(f'bad block state {s!r}')
    ns = (m.group(1) or 'minecraft:')
    props = {}
    if m.group(4):
        for kv in m.group(4).split(','):
            k, v = kv.split('=')
            props[k.strip()] = v.strip()
    return ns + m.group(2), props


def state_str(name, props):
    return name + ('[' + ','.join(f'{k}={v}' for k, v in sorted(props.items())) + ']' if props else '')


FACINGS = ('north', 'east', 'south', 'west')


def rotate_facing(f, quarter_turns):
    if f not in FACINGS:
        return f
    return FACINGS[(FACINGS.index(f) + quarter_turns) % 4]


class Structure:
    """Block template builder. Coordinates may be negative while building; save() shifts the bounding box to 0.

    Blocks you do not set are 'structure void': the world keeps whatever is there. To clear a room (air on land,
    water under the sea) set those blocks explicitly (fill(..., 'air') or fill(..., 'water')).
    """

    def __init__(self, name=''):
        self.name = name
        self.blocks = {}      # (x,y,z) -> (state_name, props_dict, nbt or None)
        self.entities = []    # (x,y,z, nbt)

    # -- basic
    def set(self, x, y, z, state, nbt=None):
        if state is None:
            self.blocks.pop((x, y, z), None)
            return
        name, props = parse_state(state) if isinstance(state, str) else state
        self.blocks[(x, y, z)] = (name, dict(props), nbt)

    def get(self, x, y, z):
        b = self.blocks.get((x, y, z))
        return state_str(b[0], b[1]) if b else None

    def name_at(self, x, y, z):
        b = self.blocks.get((x, y, z))
        return b[0] if b else None

    def fill(self, x1, y1, z1, x2, y2, z2, state, hollow=False, only_empty=False, nbt=None):
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    if hollow and min(x1, x2) < x < max(x1, x2) and min(y1, y2) < y < max(y1, y2) \
                            and min(z1, z2) < z < max(z1, z2):
                        continue
                    if only_empty and (x, y, z) in self.blocks and self.blocks[(x, y, z)][0] not in (
                            'minecraft:air', 'minecraft:water'):
                        continue
                    self.set(x, y, z, state, nbt)

    def walls(self, x1, y1, z1, x2, y2, z2, state):
        """Four walls (no floor/ceiling)."""
        for y in range(min(y1, y2), max(y1, y2) + 1):
            for x in range(min(x1, x2), max(x1, x2) + 1):
                self.set(x, y, z1, state); self.set(x, y, z2, state)
            for z in range(min(z1, z2), max(z1, z2) + 1):
                self.set(x1, y, z, state); self.set(x2, y, z, state)

    def cylinder(self, cx, cz, y1, y2, r, state, hollow=False, thickness=1):
        for y in range(y1, y2 + 1):
            for x in range(cx - int(r) - 1, cx + int(r) + 2):
                for z in range(cz - int(r) - 1, cz + int(r) + 2):
                    d = ((x - cx) ** 2 + (z - cz) ** 2) ** 0.5
                    if d <= r + 0.3 and (not hollow or d > r - thickness + 0.3):
                        self.set(x, y, z, state)

    def sphere(self, cx, cy, cz, r, state, hollow=False, thickness=1, upper_only=False):
        for x in range(cx - int(r) - 1, cx + int(r) + 2):
            for y in range(cy - int(r) - 1, cy + int(r) + 2):
                if upper_only and y < cy:
                    continue
                for z in range(cz - int(r) - 1, cz + int(r) + 2):
                    d = ((x - cx) ** 2 + (y - cy) ** 2 + (z - cz) ** 2) ** 0.5
                    if d <= r + 0.3 and (not hollow or d > r - thickness + 0.3):
                        self.set(x, y, z, state)

    def line(self, p1, p2, state):
        (x1, y1, z1), (x2, y2, z2) = p1, p2
        n = max(abs(x2 - x1), abs(y2 - y1), abs(z2 - z1), 1)
        for i in range(n + 1):
            t = i / n
            self.set(round(x1 + (x2 - x1) * t), round(y1 + (y2 - y1) * t), round(z1 + (z2 - z1) * t), state)

    def replace(self, pred, state, chance=1.0, seed=0):
        """Replace blocks whose state name matches pred (callable(name) or substring) with state (or callable(name))."""
        rnd = random.Random(seed)
        test = pred if callable(pred) else (lambda n: pred in n)
        for pos, (name, props, nbt) in list(self.blocks.items()):
            if test(name) and rnd.random() < chance:
                self.set(*pos, state(name) if callable(state) else state)

    def decay(self, chance, seed=0, keep=('chest', 'jigsaw', 'raft:'), fill_with=None, y_bias=0.0):
        """Ruin effect: remove (or replace with fill_with, e.g. 'water') random blocks; higher blocks decay more
        when y_bias > 0. Blocks whose name contains one of `keep` survive."""
        rnd = random.Random(seed)
        ys = [p[1] for p in self.blocks] or [0]
        lo, hi = min(ys), max(ys)
        for pos, (name, props, nbt) in list(self.blocks.items()):
            if name in ('minecraft:air', 'minecraft:water') or any(k in name for k in keep):
                continue
            c = chance * (1 + y_bias * ((pos[1] - lo) / max(1, hi - lo)))
            if rnd.random() < c:
                if fill_with:
                    self.set(*pos, fill_with)
                else:
                    del self.blocks[pos]

    # -- block entities / entities
    def chest(self, x, y, z, loot_table, facing='north', block='minecraft:chest', waterlogged=None, seed=None):
        """Container with a loot table (chest, barrel, trapped_chest, raft:treasure_chest, raft:wooden_crate...)."""
        props = {'facing': facing}
        if waterlogged is not None:
            props['waterlogged'] = 'true' if waterlogged else 'false'
        if block in ('minecraft:chest', 'minecraft:trapped_chest'):
            props.setdefault('type', 'single')
        be_id = block if block.startswith('minecraft:') else block
        nbt = {'id': be_id, 'LootTable': loot_table if ':' in loot_table else NS + ':' + loot_table}
        if seed is not None:
            nbt['LootTableSeed'] = Long(seed)
        self.set(x, y, z, state_str(block, props), nbt)

    def block_entity(self, x, y, z, state, be_id, data):
        """Any block with block entity data, e.g. raft:research_terminal with {'LogId': 'helix_1'}."""
        nbt = {'id': be_id}
        nbt.update(data)
        self.set(x, y, z, state, nbt)

    def spawner(self, x, y, z, entity_id, delay=20):
        self.set(x, y, z, 'minecraft:spawner', {
            'id': 'minecraft:mob_spawner', 'Delay': Short(delay),
            'SpawnData': {'entity': {'id': entity_id}}})

    def entity(self, x, y, z, nbt):
        """Entity placed with the structure, e.g. {'id': 'raft:drowned_corsair', 'PersistenceRequired': True}.
        x,y,z are doubles (use +0.5 to centre)."""
        self.entities.append((x, y, z, nbt))

    def jigsaw(self, x, y, z, orientation, name, target, pool, final_state='minecraft:air', joint='rollable',
               selection_priority=0, placement_priority=0):
        """Jigsaw connector. orientation e.g. 'north_up' (horizontal, facing north) or 'up_north' (pointing up).
        name/target are ids like 'raft:village_street'; pool is a template pool id or 'minecraft:empty'."""
        self.set(x, y, z, f'minecraft:jigsaw[orientation={orientation}]', {
            'id': 'minecraft:jigsaw', 'name': name, 'target': target, 'pool': pool,
            'final_state': final_state, 'joint': joint,
            'selection_priority': selection_priority, 'placement_priority': placement_priority})

    # -- transforms
    def rotated(self, quarter_turns):
        """Copy rotated clockwise (seen from above) around the origin; rotates facing properties too."""
        q = quarter_turns % 4
        s = Structure(self.name)
        for (x, y, z), (name, props, nbt) in self.blocks.items():
            nx, nz = x, z
            for _ in range(q):
                nx, nz = -nz, nx
            p = dict(props)
            if 'facing' in p:
                p['facing'] = rotate_facing(p['facing'], q)
            if 'axis' in p and q % 2 == 1 and p['axis'] in ('x', 'z'):
                p['axis'] = 'z' if p['axis'] == 'x' else 'x'
            for d in FACINGS:
                if d in p and q:
                    pass
            if q % 2 == 1 or q == 2:
                sides = {d: props[d] for d in FACINGS if d in props}
                for d, v in sides.items():
                    p[rotate_facing(d, q)] = v
            s.blocks[(nx, y, nz)] = (name, p, nbt)
        for (x, y, z, nbt) in self.entities:
            nx, nz = x, z
            for _ in range(q):
                nx, nz = -nz, nx
            s.entities.append((nx, y, nz, nbt))
        return s

    def paste(self, other, dx, dy, dz, overwrite=True):
        for (x, y, z), b in other.blocks.items():
            if overwrite or (x + dx, y + dy, z + dz) not in self.blocks:
                self.blocks[(x + dx, y + dy, z + dz)] = b
        for (x, y, z, nbt) in other.entities:
            self.entities.append((x + dx, y + dy, z + dz, nbt))

    # -- output
    def bounds(self):
        xs = [p[0] for p in self.blocks]; ys = [p[1] for p in self.blocks]; zs = [p[2] for p in self.blocks]
        return (min(xs), min(ys), min(zs)), (max(xs), max(ys), max(zs))

    def size(self):
        (a, b, c), (d, e, f) = self.bounds()
        return d - a + 1, e - b + 1, f - c + 1

    def to_nbt(self):
        (ox, oy, oz), _ = self.bounds()
        palette, index, blocks = [], {}, []
        for (x, y, z), (name, props, nbt) in sorted(self.blocks.items(), key=lambda kv: (kv[0][1], kv[0][2], kv[0][0])):
            key = state_str(name, props)
            if key not in index:
                index[key] = len(palette)
                e = {'id': name}  # 26.3 palette format: id + properties (older versions: Name/Properties)
                if props:
                    e['properties'] = dict(sorted(props.items()))
                palette.append(e)
            b = {'pos': [x - ox, y - oy, z - oz], 'state': index[key]}
            if nbt:
                b['nbt'] = nbt
            blocks.append(b)
        ents = []
        for (x, y, z, nbt) in self.entities:
            ents.append({'pos': TypedList(6, [Double(x - ox), Double(y - oy), Double(z - oz)]),
                         'blockPos': [int(x - ox), int(y - oy), int(z - oz)], 'nbt': nbt})
        sx, sy, sz = self.size()
        return {'DataVersion': DATA_VERSION, 'size': [sx, sy, sz], 'palette': palette, 'blocks': blocks,
                'entities': TypedList(10, ents)}

    def save(self, module, name=None):
        """Write data/raft/structure/<name>.nbt into the module folder. name may contain '/'."""
        name = name or self.name
        p = out(module, 'data', NS, 'structure', *(name + '.nbt').split('/'))
        write(p, self.to_nbt())
        return p


def load_structure(path):
    """Read a .nbt template (vanilla or ours) into a Structure (for inspection / reuse)."""
    d = read(path)
    s = Structure()
    pal = d['palette'] if 'palette' in d else d['palettes'][0]
    for b in d['blocks']:
        e = pal[b['state']]
        s.blocks[tuple(b['pos'])] = (e.get('id', e.get('Name')), dict(e.get('properties', e.get('Properties', {}))),
                                     b.get('nbt'))
    for e in d.get('entities', []):
        s.entities.append((*e['pos'], e['nbt']))
    return s
