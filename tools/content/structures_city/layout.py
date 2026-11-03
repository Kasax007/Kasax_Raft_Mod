"""Offline check of the deterministic part of the Port Solace jigsaw (plaza -> avenues/hall -> lots -> tops).

    cd tools && python3 -m content.structures_city.layout

Places the plaza unrotated at the origin and follows the jigsaws with fixed single-element pools, as the game does:
the child is rotated so that its jigsaw faces the parent's jigsaw and touches it. Prints every piece's box (plaza
template coordinates) and any overlap between boxes (the game would reject the later piece and use the fallback pool).
"""
import os
import sys

TOOLS = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
if TOOLS not in sys.path:
    sys.path.insert(0, TOOLS)

from content.structures_city import gen  # noqa: E402

R = {0: lambda x, z: (x, z), 1: lambda x, z: (-z, x), 2: lambda x, z: (-x, -z), 3: lambda x, z: (z, -x)}
VEC = {'north': (0, -1), 'south': (0, 1), 'east': (1, 0), 'west': (-1, 0)}
FIXED = {'avenues_power': 'avenue_power', 'avenues_harbour': 'avenue_harbour', 'avenues_market': 'avenue_market',
         'city_hall': 'city_hall_base', 'city_hall_top': 'city_hall_top', 'lots_deco': 'deco_tower_base',
         'lots_glass': 'glass_tower_base', 'tops/deco_tower': 'deco_tower_top', 'tops/glass_tower': 'glass_tower_top'}


def jigsaws(s):
    (mx, my, mz), _ = s.bounds()
    out = []
    for (x, y, z), (b, p, n) in s.blocks.items():
        if b == 'minecraft:jigsaw':
            o = p['orientation'].split('_')[0]
            out.append(((x - mx, y - my, z - mz), o, n['name'], n['target'], n['pool'],
                        int(n.get('selection_priority', 0)), int(n.get('placement_priority', 0))))
    return out


def piece_box(size, rot, off):
    xs, ys, zs = size
    cs = [R[rot](x, z) for x in (0, xs - 1) for z in (0, zs - 1)]
    return (off[0] + min(c[0] for c in cs), off[1], off[2] + min(c[1] for c in cs),
            off[0] + max(c[0] for c in cs), off[1] + ys - 1, off[2] + max(c[1] for c in cs))


def overlap(a, b):
    return all(a[i] <= b[i + 3] and b[i] <= a[i + 3] for i in range(3))


def layout(t=None):
    t = t or gen.build_all()
    J = {k: jigsaws(v) for k, v in t.items()}
    pieces = [('plaza', 0, (0, 0, 0))]
    queue = [('plaza', 0, (0, 0, 0))]
    while queue:
        name, rot, off = queue.pop(0)
        for (pos, o, jn, target, pool, _s, _p) in J[name]:
            pool = pool.split('/', 1)[-1] if pool.startswith('raft:') else pool
            child = FIXED.get(pool)
            if child is None:
                continue
            q = R[rot](pos[0], pos[2])
            wp = (off[0] + q[0], off[1] + pos[1], off[2] + q[1])
            if o in ('up', 'down'):
                f = (0, 1 if o == 'up' else -1, 0)
            else:
                d = R[rot](*VEC[o])
                f = (d[0], 0, d[1])
            for (cpos, co, cn, ct, cp, _s, _p) in J[child]:
                if cn != target:
                    continue
                for crot in range(4):
                    if co in ('up', 'down'):
                        if (co == 'down') != (f[1] == 1) or crot != rot:
                            continue
                    else:
                        cd = R[crot](*VEC[co])
                        if (cd[0], cd[1]) != (-f[0], -f[2]):
                            continue
                    cq = R[crot](cpos[0], cpos[2])
                    coff = (wp[0] + f[0] - cq[0], wp[1] + f[1] - cpos[1], wp[2] + f[2] - cq[1])
                    pieces.append((child, crot, coff))
                    queue.append((child, crot, coff))
                    break
                break
    boxes = [(n, r, o, piece_box(t[n].size(), r, o)) for n, r, o in pieces]
    return boxes


if __name__ == '__main__':
    boxes = layout()
    for n, r, o, b in boxes:
        print(f'{n:18s} rot {r} box x {b[0]}..{b[3]}  y {b[1]}..{b[4]}  z {b[2]}..{b[5]}')
    for i in range(len(boxes)):
        for j in range(i + 1, len(boxes)):
            if overlap(boxes[i][3], boxes[j][3]):
                print('OVERLAP', boxes[i][0], boxes[j][0])


# ------------------------------------------------------------------------------------------------ Monte Carlo
def _pools():
    import json
    from bclib.paths import MODULES
    root = os.path.join(MODULES, 'structures_city', 'data', 'raft', 'worldgen', 'template_pool', 'port_solace')
    pools = {}
    for dp, _, fs in os.walk(root):
        for f in fs:
            d = json.load(open(os.path.join(dp, f)))
            key = os.path.relpath(os.path.join(dp, f), root)[:-5]
            els = [(e['element']['location'].split('/')[-1], e['weight']) for e in d['elements']]
            fb = d['fallback'].split('port_solace/')[-1] if 'port_solace/' in d['fallback'] else None
            pools[key] = (els, fb)
    return pools


def simulate(seed, t=None, J=None, pools=None, max_depth=6, max_dist=112):
    """A simplified copy of the game's jigsaw assembly (bounding-box collisions, weighted shuffles, fallbacks)."""
    import random
    rnd = random.Random(seed)
    t = t or gen.build_all()
    J = J or {k: jigsaws(v) for k, v in t.items()}
    pools = pools or _pools()
    start_rot = rnd.randrange(4)
    b0 = piece_box(t['plaza'].size(), start_rot, (0, 0, 0))
    cx, cz = (b0[0] + b0[3]) // 2, (b0[2] + b0[5]) // 2
    lim = (cx - max_dist, -200, cz - max_dist, cx + max_dist, 400, cz + max_dist)
    placed = [('plaza', start_rot, (0, 0, 0), b0, 0)]
    queue = [(0, 0, placed[0])]
    seq = [0]

    def fits(b):
        if not all(lim[i] <= b[i] and b[i + 3] <= lim[i + 3] for i in range(3)):
            return False
        return not any(overlap(b, p[3]) for p in placed)

    while queue:
        queue.sort(key=lambda q: (-q[0], q[1]))
        name, rot, off, box, depth = queue.pop(0)[2]
        js = list(J[name])
        rnd.shuffle(js)
        js.sort(key=lambda j: -j[5])
        for (pos, o, jn, target, pool, sel, prio) in js:
            if pool == 'minecraft:empty':
                continue
            pk = pool.split('port_solace/', 1)[-1]
            els, fb = pools[pk]
            cand = []
            if depth != max_depth:
                lst = [e for e, w in els for _ in range(w)]
                rnd.shuffle(lst)
                cand += lst
            if fb:
                lst = [e for e, w in pools[fb][0] for _ in range(w)]
                rnd.shuffle(lst)
                cand += lst
            q = R[rot](pos[0], pos[2])
            wp = (off[0] + q[0], off[1] + pos[1], off[2] + q[1])
            if o in ('up', 'down'):
                f = (0, 1 if o == 'up' else -1, 0)
            else:
                d = R[rot](*VEC[o])
                f = (d[0], 0, d[1])
            done = False
            for child in cand:
                rots = [0, 1, 2, 3]
                rnd.shuffle(rots)
                for crot in rots:
                    cjs = list(J[child])
                    rnd.shuffle(cjs)
                    for (cpos, co, cn, ct, cp, _s, _p) in cjs:
                        if cn != target:
                            continue
                        if co in ('up', 'down'):
                            if (co == 'down') != (f[1] == 1) or crot != rot:
                                continue
                        else:
                            cd = R[crot](*VEC[co])
                            if (cd[0], cd[1]) != (-f[0], -f[2]) or f[1] != 0:
                                continue
                        cq = R[crot](cpos[0], cpos[2])
                        coff = (wp[0] + f[0] - cq[0], wp[1] + f[1] - cpos[1], wp[2] + f[2] - cq[1])
                        cb = piece_box(t[child].size(), crot, coff)
                        if fits(cb):
                            p = (child, crot, coff, cb, depth + 1)
                            placed.append(p)
                            seq[0] += 1
                            queue.append((prio, seq[0], p))
                            done = True
                            break
                    if done:
                        break
                if done:
                    break
    return placed


def stats(n=40):
    from collections import Counter
    t = gen.build_all()
    J = {k: jigsaws(v) for k, v in t.items()}
    pools = _pools()
    tot = Counter()
    tall = []
    ext = []
    for seed in range(n):
        ps = simulate(seed, t, J, pools)
        c = Counter(p[0] for p in ps)
        tot.update(c)
        tall.append(sum(1 for p in ps if p[3][4] - p[3][1] + 1 >= 25 and not p[0].endswith('_top')))
        xs = [p[3][0] for p in ps] + [p[3][3] for p in ps]
        zs = [p[3][2] for p in ps] + [p[3][5] for p in ps]
        ext.append((max(xs) - min(xs) + 1, max(zs) - min(zs) + 1))
    print(f'{n} cities; pieces per city:')
    for k, v in sorted(tot.items(), key=lambda kv: -kv[1]):
        print(f'  {k:18s} {v / n:5.2f}')
    print('buildings >= 25 high per city: min %d avg %.1f max %d' % (min(tall), sum(tall) / n, max(tall)))
    print('extent (x, z) avg %.0f x %.0f' % (sum(e[0] for e in ext) / n, sum(e[1] for e in ext) / n))


if __name__ == '__main__' and '--stats' in sys.argv:
    stats()
