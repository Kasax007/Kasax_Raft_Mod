"""Structure templates (.nbt) from Minecraft 26.3 to 1.21.1, in place.

26.3 writes the palette as {id, properties}; 1.21.1 reads {Name, Properties}. The data version goes down to
1.21.1's (3955), so the game does not try to upgrade the file. Every tag keeps its NBT type (own reader/writer).
Block entity and entity data stay as they are (both versions use the same item stack format with id/count and
components); convert() reports block entity ids and entity ids so they can be checked against 1.21.1.
"""
import gzip
import io
import struct

DATA_VERSION_1211 = 3955


class Tag:
    __slots__ = ('t', 'v')

    def __init__(self, t, v):
        self.t = t
        self.v = v

    def __repr__(self):
        return 'Tag(%d, %r)' % (self.t, self.v)


def _r(f, t):
    if t == 1:
        return struct.unpack('>b', f.read(1))[0]
    if t == 2:
        return struct.unpack('>h', f.read(2))[0]
    if t == 3:
        return struct.unpack('>i', f.read(4))[0]
    if t == 4:
        return struct.unpack('>q', f.read(8))[0]
    if t == 5:
        return struct.unpack('>f', f.read(4))[0]
    if t == 6:
        return struct.unpack('>d', f.read(8))[0]
    if t == 7:
        n = struct.unpack('>i', f.read(4))[0]
        return f.read(n)
    if t == 8:
        n = struct.unpack('>H', f.read(2))[0]
        return f.read(n).decode('utf-8')
    if t == 9:
        et, n = struct.unpack('>bi', f.read(5))
        return (et, [Tag(et, _r(f, et)) for _ in range(n)])
    if t == 10:
        d = {}
        while True:
            ct = f.read(1)[0]
            if ct == 0:
                return d
            k = _r(f, 8)
            d[k] = Tag(ct, _r(f, ct))
    if t == 11:
        n = struct.unpack('>i', f.read(4))[0]
        return list(struct.unpack('>%di' % n, f.read(4 * n)))
    if t == 12:
        n = struct.unpack('>i', f.read(4))[0]
        return list(struct.unpack('>%dq' % n, f.read(8 * n)))
    raise ValueError(t)


def _w(f, tag):
    t, v = tag.t, tag.v
    if t == 1:
        f.write(struct.pack('>b', v))
    elif t == 2:
        f.write(struct.pack('>h', v))
    elif t == 3:
        f.write(struct.pack('>i', v))
    elif t == 4:
        f.write(struct.pack('>q', v))
    elif t == 5:
        f.write(struct.pack('>f', v))
    elif t == 6:
        f.write(struct.pack('>d', v))
    elif t == 7:
        f.write(struct.pack('>i', len(v)))
        f.write(v)
    elif t == 8:
        b = v.encode('utf-8')
        f.write(struct.pack('>H', len(b)))
        f.write(b)
    elif t == 9:
        et, items = v
        f.write(struct.pack('>bi', et if items else (et or 0), len(items)))
        for it in items:
            _w(f, it)
    elif t == 10:
        for k, c in v.items():
            f.write(bytes([c.t]))
            _w(f, Tag(8, k))
            _w(f, c)
        f.write(b'\x00')
    elif t == 11:
        f.write(struct.pack('>i', len(v)))
        f.write(struct.pack('>%di' % len(v), *v))
    elif t == 12:
        f.write(struct.pack('>i', len(v)))
        f.write(struct.pack('>%dq' % len(v), *v))
    else:
        raise ValueError(t)


def read(path):
    raw = open(path, 'rb').read()
    gz = raw[:2] == b'\x1f\x8b'
    if gz:
        raw = gzip.decompress(raw)
    f = io.BytesIO(raw)
    t = f.read(1)[0]
    name = _r(f, 8)
    return Tag(t, _r(f, t)), name, gz


def write(path, root, name='', gz=True):
    f = io.BytesIO()
    f.write(bytes([root.t]))
    _w(f, Tag(8, name))
    _w(f, root)
    data = f.getvalue()
    if gz:
        data = gzip.compress(data, mtime=0)
    with open(path, 'wb') as out:
        out.write(data)


def convert(path, report=None):
    """Converts one template in place; returns True when it changed. report: dict collecting ids to check."""
    root, name, gz = read(path)
    d = root.v
    changed = False
    for e in d.get('palette', Tag(9, (10, []))).v[1]:
        st = e.v
        if 'id' in st and 'Name' not in st:
            st['Name'] = st.pop('id')
            changed = True
        if 'properties' in st:
            st['Properties'] = st.pop('properties')
            changed = True
        if report is not None:
            report.setdefault('blocks', set()).add(st['Name'].v)
    if 'palettes' in d:
        for pal in d['palettes'].v[1]:
            for e in pal.v[1]:
                st = e.v
                if 'id' in st and 'Name' not in st:
                    st['Name'] = st.pop('id')
                    changed = True
                if 'properties' in st:
                    st['Properties'] = st.pop('properties')
                    changed = True
    dv = d.get('DataVersion')
    if dv is None or dv.v != DATA_VERSION_1211:
        d['DataVersion'] = Tag(3, DATA_VERSION_1211)
        changed = True
    if report is not None:
        for b in d.get('blocks', Tag(9, (10, []))).v[1]:
            n = b.v.get('nbt')
            if n is not None and 'id' in n.v:
                report.setdefault('block_entities', set()).add(n.v['id'].v)
        for e in d.get('entities', Tag(9, (10, []))).v[1]:
            n = e.v.get('nbt')
            if n is not None and 'id' in n.v:
                report.setdefault('entities', set()).add(n.v['id'].v)
    if changed:
        write(path, root, name, gz)
    return changed
