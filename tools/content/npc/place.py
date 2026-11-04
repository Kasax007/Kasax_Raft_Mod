"""Puts the people of the sea into the structure templates (template entities, docs/design/npcs.md).

The templates belong to the structure modules. Their generators do not reproduce the committed .nbt files exactly any
more (later fixes were made to the files), so this script does not re-run them: it patches the saved templates in
place and only touches their entity list. It is idempotent: NPC entities of earlier runs are replaced.

Each spot is relative to an anchor block the template is known to contain (a story chest, a campfire ...), so it
stays right if the template is rebuilt with the same layout. The script prints the blocks at the feet, the head and
under the feet of every spot as a check.

Run: cd tools && python3 -m content.npc.place [--check]
"""
import os
import sys

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..')))

import gzip  # noqa: E402
import io  # noqa: E402
import struct  # noqa: E402

from bclib.paths import MODULES  # noqa: E402


# ---------------------------------------------------------------------------------------------- exact NBT round trip
# Values are (tag, payload): compounds are dicts of name -> (tag, payload), lists are (element tag, [payloads]).
# Unlike bclib.nbt.read this keeps every tag type, so a patched template differs only in the entities.
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
        et, n = struct.unpack('>bi', f.read(5))
        return (et, [_r(f, et) for _ in range(n)])
    if tag == 10:
        d = {}
        while True:
            t = f.read(1)[0]
            if t == 0:
                return d
            k = _r(f, 8)
            d[k] = (t, _r(f, t))
    if tag == 11: n = struct.unpack('>i', f.read(4))[0]; return list(struct.unpack('>%di' % n, f.read(4 * n)))
    if tag == 12: n = struct.unpack('>i', f.read(4))[0]; return list(struct.unpack('>%dq' % n, f.read(8 * n)))
    raise ValueError(tag)


def _w(f, tag, v):
    if tag == 1: f.write(struct.pack('>b', v))
    elif tag == 2: f.write(struct.pack('>h', v))
    elif tag == 3: f.write(struct.pack('>i', v))
    elif tag == 4: f.write(struct.pack('>q', v))
    elif tag == 5: f.write(struct.pack('>f', v))
    elif tag == 6: f.write(struct.pack('>d', v))
    elif tag == 7: f.write(struct.pack('>i', len(v))); f.write(v)
    elif tag == 8: b = v.encode('utf-8'); f.write(struct.pack('>H', len(b))); f.write(b)
    elif tag == 9:
        et, items = v
        f.write(struct.pack('>bi', et if items else (et or 0), len(items)))
        for e in items:
            _w(f, et, e)
    elif tag == 10:
        for k, (t, e) in v.items():
            f.write(struct.pack('>b', t)); _w(f, 8, k); _w(f, t, e)
        f.write(b'\x00')
    elif tag == 11: f.write(struct.pack('>i', len(v))); f.write(struct.pack('>%di' % len(v), *v))
    elif tag == 12: f.write(struct.pack('>i', len(v))); f.write(struct.pack('>%dq' % len(v), *v))
    else:
        raise ValueError(tag)


def read_exact(path):
    raw = gzip.decompress(open(path, 'rb').read())
    f = io.BytesIO(raw)
    t = f.read(1)[0]
    name = _r(f, 8)
    return name, (t, _r(f, t)), raw


def write_exact(path, name, root):
    f = io.BytesIO()
    t, v = root
    f.write(struct.pack('>b', t)); _w(f, 8, name); _w(f, t, v)
    data = f.getvalue()
    with open(path, 'wb') as out:
        out.write(gzip.compress(data, mtime=0))
    return data


def plain(v):
    """(tag, payload) -> plain Python (for reading block states and block entity data)."""
    t, p = v
    if t == 10:
        return {k: plain(e) for k, e in p.items()}
    if t == 9:
        et, items = p
        return [plain((et, e)) for e in items]
    return p

NPC_IDS = ('raft:keeper_fenn', 'raft:nalu', 'raft:old_tamo', 'raft:dr_voss', 'raft:teo', 'raft:kai', 'raft:dr_lund',
           'raft:okafor')


def loot(table):
    return lambda state, block_nbt: block_nbt is not None and block_nbt.get('LootTable') == table


def block(block_id):
    return lambda state, block_nbt: state['id'] == block_id


def data(key, value):
    return lambda state, block_nbt: block_nbt is not None and block_nbt.get(key) == value


# (module, template, npc, anchor, offset from the anchor block's corner (x, y, z), yaw in degrees: 0 = south)
SPOTS = [
    # the keeper in his cottage, on the rug between the table and the desk
    ('structures_surface', 'old_lighthouse/compound_a', 'keeper_fenn', loot('raft:chests/story/lighthouse'), (1.5, 0, 3.5), 180.0),
    ('structures_surface', 'old_lighthouse/compound_b', 'keeper_fenn', loot('raft:chests/story/lighthouse'), (1.5, 0, 3.5), 180.0),
    # Nalu on the veranda of Nana Kalea's house, in front of the door
    ('structures_surface', 'saltmere_village/elder', 'nalu', loot('raft:chests/story/saltmere_elder'), (-3.5, 0, -6.5), 180.0),
    # Old Tamo on the pier of the docks, by the moored boat
    ('structures_surface', 'saltmere_village/docks', 'old_tamo', loot('raft:chests/saltmere_house'), (4.5, 0, 4.5), -90.0),
    # Dr. Voss in the storeroom of the forge hall, with her notes
    ('structures_surface', 'ashen_forge/hall', 'dr_voss', block('minecraft:raw_gold_block'), (2.5, 0, 1.5), 90.0),
    # Teo behind the counter of the tiki bar
    ('structures_side', 'beach_shack/tiki_bar', 'teo', loot('raft:chests/beach_shack'), (-2.5, 0, -1.5), 180.0),
    # Kai at the camp fire of the tent camp
    ('structures_side', 'castaway_camp/tent', 'kai', loot('raft:chests/castaway_camp'), (-0.5, 0, 4.5), -90.0),
    # Dr. Lund in her hut on the ice floe
    ('structures_side', 'iceberg_outpost/outpost_0', 'dr_lund', loot('raft:chests/iceberg_outpost'), (-2.5, 0, 2.5), 180.0),
    # Okafor in the control room of the rig
    ('structures_side', 'rusted_rig/rig_0', 'okafor', data('LogId', 'rig_1'), (-1.5, 0, 2.5), 180.0),
]


def template_path(module, name):
    return os.path.join(MODULES, module, 'data', 'raft', 'structure', *(name + '.nbt').split('/'))


def find_anchor(blocks, palette, match):
    hits = []
    for b in blocks:
        state = palette[b['state']]
        if match(state, b.get('nbt')):
            hits.append(tuple(b['pos']))
    return hits


def block_at(palette, pos, index):
    b = index.get(tuple(pos))
    if b is None:
        return '(none)'
    st = palette[b['state']]
    props = st.get('properties') or {}
    return st['id'] + ('[' + ','.join(f'{k}={v}' for k, v in sorted(props.items())) + ']' if props else '')


def npc_entity(npc, x, y, z, bx, by, bz, yaw):
    return {
        'pos': (9, (6, [float(x), float(y), float(z)])),
        'blockPos': (9, (3, [bx, by, bz])),
        'nbt': (10, {
            'id': (8, f'raft:{npc}'),
            'PersistenceRequired': (1, 1),
            'Rotation': (9, (5, [float(yaw), 0.0])),
        }),
    }


def place(check_only=False):
    by_file = {}
    for spot in SPOTS:
        by_file.setdefault((spot[0], spot[1]), []).append(spot)
    for (module, name), spots in by_file.items():
        path = template_path(module, name)
        tag_name, root, raw = read_exact(path)
        if write_exact(os.devnull, tag_name, root) != raw:
            raise SystemExit(f'{name}: NBT round trip is not exact')
        top = root[1]
        blocks = plain(top['blocks'])
        palette = plain(top['palette'])
        index = {tuple(b['pos']): b for b in blocks}
        et, old = top.get('entities', (9, (10, [])))[1] if 'entities' in top else (10, [])
        ents = [e for e in old if e['nbt'][1].get('id', (8, ''))[1] not in NPC_IDS]
        for (_, _, npc, match, off, yaw) in spots:
            hits = find_anchor(blocks, palette, match)
            if len(hits) != 1:
                raise SystemExit(f'{name}: anchor for {npc} found {len(hits)} times: {hits}')
            ax, ay, az = hits[0]
            x, y, z = ax + off[0], ay + off[1], az + off[2]
            bx, by, bz = int(x // 1), int(y // 1), int(z // 1)
            print(f'{name:32s} {npc:12s} at ({x}, {y}, {z})')
            for label, p in (('feet', (bx, by, bz)), ('head', (bx, by + 1, bz)), ('below', (bx, by - 1, bz))):
                print(f'    {label:5s} {block_at(palette, p, index)}')
            ents.append(npc_entity(npc, x, y, z, bx, by, bz, yaw))
        if not check_only:
            top['entities'] = (9, (10, ents))
            write_exact(path, tag_name, root)
    if not check_only:
        print('npc: templates patched')


if __name__ == '__main__':
    place('--check' in sys.argv)
