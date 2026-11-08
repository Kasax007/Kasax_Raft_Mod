"""Seed survey of the raft dimension (sim.py terrain): land / ocean share and structure starts per km².

cd tools && python3 -m content.world.survey [size] [seeds...]
For every structure set (random_spread placement, the game's own random: Java LCG with the set's salt) the start
chunk of each grid cell is worked out, and counted when the biome there (sim.py, multi-noise boxes) is one the
structure accepts. The site checks of StructureSiteMixin (land radius, flat ground, depth) and StructurePriority
(no overlaps) remove a few more in the game, so these are upper bounds; "closest pair" is the smallest distance
between two starts of the same set (blocks).
"""
import glob
import json
import math
import os
import sys

import numpy as np

from content.world import terrain
from content.world.preview_terrain import biome_of, evaluator, sample

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..', '..'))
MASK48 = (1 << 48) - 1
LAB = []
MAJ = {}
CLOSEST_MAJOR = []
# keep in step with StructureSpacing.java (order = who stays when two majors of different kinds come too close)
MAJOR_ORDER = ['ashen_forge', 'old_lighthouse', 'port_solace', 'saltmere_village', 'helix_station', 'abyssal_sanctum',
               'coral_temple', 'pirate_cove', 'rusted_rig', 'iceberg_outpost', 'wreck_galleon', 'wreck_cargo_ship',
               'sunken_plane']
MAJOR_DISTANCE = int(os.environ.get('SURVEY_MAJOR_DISTANCE', '384'))


def _i64(v):
    v &= (1 << 64) - 1
    return v - (1 << 64) if v >= 1 << 63 else v


class JRandom:
    def __init__(self, seed):
        self.s = (seed ^ 0x5DEECE66D) & MASK48

    def next(self, bits):
        self.s = (self.s * 0x5DEECE66D + 0xB) & MASK48
        r = self.s >> (48 - bits)
        return r - (1 << bits) if r >= 1 << (bits - 1) else r

    def next_int(self, bound):
        if bound & -bound == bound:
            return (bound * (self.next(31) & 0x7FFFFFFF)) >> 31
        while True:
            bits = self.next(31) & 0x7FFFFFFF
            val = bits % bound
            if bits - val + (bound - 1) < (1 << 31):
                return val


def start_chunk(seed, salt, spacing, separation, cx, cz, triangular=False):
    s = _i64(cx * 341873128712 + cz * 132897987541 + seed + salt)
    r = JRandom(s)
    n = spacing - separation
    if triangular:
        ox = (r.next_int(n) + r.next_int(n)) // 2
        oz = (r.next_int(n) + r.next_int(n)) // 2
    else:
        ox = r.next_int(n)
        oz = r.next_int(n)
    return cx * spacing + ox, cz * spacing + oz


def load(path):
    with open(path) as f:
        return json.load(f)


def find(kind, ident):
    ns, p = ident.split(':')
    for base in glob.glob(os.path.join(ROOT, 'src', 'main', 'modules', '*', 'data')) + [os.path.join(ROOT, 'src', 'main', 'resources', 'data')]:
        f = os.path.join(base, ns, kind, p + '.json')
        if os.path.exists(f):
            return f
    return None


def biomes_of(structure_id):
    s = load(find('worldgen/structure', structure_id))
    b = s.get('biomes')
    if isinstance(b, str) and b.startswith('#'):
        t = load(find('tags/worldgen/biome', b[1:]))
        return {v if isinstance(v, str) else v['id'] for v in t['values']}
    return set(b if isinstance(b, list) else [b])


def sets():
    out = []
    for f in sorted(glob.glob(os.path.join(ROOT, 'src', 'main', '**', 'worldgen', 'structure_set', '*.json'), recursive=True)):
        j = load(f)
        p = j['placement']
        if p['type'] != 'minecraft:random_spread':
            continue
        bs = set()
        for e in j['structures']:
            try:
                bs |= biomes_of(e['structure'])
            except Exception:
                pass
        out.append((os.path.basename(f)[:-5], p['spacing'], p['separation'], p['salt'], p.get('spread_type') == 'triangular', bs))
    return out


def main():
    size = int(sys.argv[1]) if len(sys.argv) > 1 else 8192
    seeds = [int(s) for s in sys.argv[2:]] or [1, 2, 3]
    km2 = (size / 1000.0) ** 2
    rows = {}
    land = []
    for seed in seeds:
        ev = evaluator(seed)
        step = 32
        xs = np.arange(-size // 2, size // 2, step, dtype=np.float64)
        x, z = np.meshgrid(xs, xs)
        h = sample(ev, x, z)['surface']
        land.append((h >= 100).mean() * 100)
        for name, spacing, sep, salt, tri, biomes in sets():
            cells = range(math.floor(-size / 2 / 16 / spacing), math.ceil(size / 2 / 16 / spacing))
            pts = []
            for cx in cells:
                for cz in cells:
                    ch = start_chunk(seed, salt, spacing, sep, cx, cz, tri)
                    bx, bz = ch[0] * 16 + 8, ch[1] * 16 + 8
                    if -size / 2 <= bx < size / 2 and -size / 2 <= bz < size / 2:
                        pts.append((bx, bz))
            if not pts:
                rows.setdefault(name, []).append((0, None))
                continue
            px = np.array([p[0] for p in pts], dtype=np.float64)
            pz = np.array([p[1] for p in pts], dtype=np.float64)
            smp = sample(ev, px, pz)
            idx, names = biome_of(smp)
            ok = np.array([names[i] in biomes for i in idx]) if biomes else np.ones(len(pts), bool)
            if name == 'helix_station':
                # the lab: floor height under the start (sim surface), and the site check of 3.2 (floor 45..76)
                fl = smp['surface'][ok]
                if os.environ.get('SURVEY_LAB_CHECK'):
                    keep = (fl >= 45) & (fl <= 76)
                    ok[np.where(ok)[0][~keep]] = False
                    fl = fl[keep]
                if len(fl):
                    LAB.append(fl)
            good = [p for p, o in zip(pts, ok) if o]
            if name in MAJOR_ORDER:
                MAJ.setdefault(seed, {})[name] = good
                continue
            closest = None
            for i in range(len(good)):
                for k in range(i + 1, len(good)):
                    d = math.dist(good[i], good[k])
                    closest = d if closest is None else min(closest, d)
            rows.setdefault(name, []).append((len(good), closest))
        # majors: greedy in MAJOR_ORDER, no start within MAJOR_DISTANCE of an earlier kind's start
        kept = []
        for name in MAJOR_ORDER:
            mine = [p for p in MAJ.get(seed, {}).get(name, [])
                    if all(math.dist(p, q) >= MAJOR_DISTANCE for q, _ in kept)]
            closest = None
            for i in range(len(mine)):
                for k in range(i + 1, len(mine)):
                    d = math.dist(mine[i], mine[k])
                    closest = d if closest is None else min(closest, d)
            kept += [(p, name) for p in mine]
            rows.setdefault(name, []).append((len(mine), closest))
        for p, name in kept:
            d = min((math.dist(p, q) for q, _ in kept if q != p), default=None)
            if d is not None:
                CLOSEST_MAJOR.append(d)
    print('land share: ' + ', '.join(f'seed {s}: {l:.1f}%' for s, l in zip(seeds, land)) + f'  (mean {np.mean(land):.1f}%)')
    total = 0.0
    print(f'{"structure set":22s} {"per km2":>8s} {"closest pair (blocks)":>22s}')
    for name, vals in sorted(rows.items()):
        per = np.mean([v[0] for v in vals]) / km2
        total += per
        cl = [v[1] for v in vals if v[1] is not None]
        print(f'{name:22s} {per:8.3f} {min(cl) if cl else float("nan"):22.0f}')
    print(f'{"all":22s} {total:8.3f}')
    major = sum(np.mean([v[0] for v in rows[n]]) / km2 for n in MAJOR_ORDER if n in rows)
    print(f'{"majors":22s} {major:8.3f}   closest two majors of any kind: {min(CLOSEST_MAJOR):.0f} blocks, '
          f'median nearest major {np.median(CLOSEST_MAJOR):.0f} (distance rule {MAJOR_DISTANCE})')
    if LAB:
        fl = np.concatenate(LAB)
        print(f'helix station floor (sea level 100): min {fl.min():.0f}, median {np.median(fl):.0f}, max {fl.max():.0f}; '
              f'top of the lab (floor + 14) at least {100 - (fl.max() + 14):.0f} blocks under the surface')
    # island types (share of all land)
    pass


if __name__ == '__main__':
    main()
