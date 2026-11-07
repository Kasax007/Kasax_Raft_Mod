"""Z-fighting check for entity / block entity models: reads the polygons written by the bctest command
`dumpmodels <file>` (every raft: model layer in its default pose) and lists faces of different cubes that lie in the
same plane, face the same way, overlap and show different texture pixels there (those flicker in the game).

usage: python3 tools/bctest/zfight_entities.py <outdir>/models.jsonl [min_overlap_px2]
"""
import json
import sys
from collections import defaultdict

import numpy as np


def clip(subject, clipper):
    """Sutherland-Hodgman: intersection of two convex polygons (2D lists of points)."""
    out = subject
    n = len(clipper)
    for i in range(n):
        a, b = clipper[i], clipper[(i + 1) % n]
        inp, out = out, []
        if not inp:
            break

        def inside(p):
            return (b[0] - a[0]) * (p[1] - a[1]) - (b[1] - a[1]) * (p[0] - a[0]) >= -1e-9

        def inter(p, q):
            x1, y1, x2, y2 = p[0], p[1], q[0], q[1]
            x3, y3, x4, y4 = a[0], a[1], b[0], b[1]
            den = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4)
            if abs(den) < 1e-12:
                return q
            t = ((x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)) / den
            return (x1 + t * (x2 - x1), y1 + t * (y2 - y1))
        s = inp[-1]
        for e in inp:
            if inside(e):
                if not inside(s):
                    out.append(inter(s, e))
                out.append(e)
            elif inside(s):
                out.append(inter(s, e))
            s = e
    return out


def area(poly):
    return abs(sum(poly[i][0] * poly[(i + 1) % len(poly)][1] - poly[(i + 1) % len(poly)][0] * poly[i][1]
                   for i in range(len(poly)))) / 2 if len(poly) >= 3 else 0.0


def ccw(poly):
    s = sum(poly[i][0] * poly[(i + 1) % len(poly)][1] - poly[(i + 1) % len(poly)][0] * poly[i][1] for i in range(len(poly)))
    return poly if s >= 0 else poly[::-1]


def uv_at(P, UV, q):
    """Affine UV of point q on the quad P (3D) from its first three vertices."""
    e1, e2 = P[1] - P[0], P[3] - P[0]
    M = np.array([e1, e2]).T
    st, *_ = np.linalg.lstsq(M, q - P[0], rcond=None)
    return UV[0] + st[0] * (UV[1] - UV[0]) + st[1] * (UV[3] - UV[0])


def main():
    path = sys.argv[1]
    min_ov = float(sys.argv[2]) if len(sys.argv) > 2 else 0.05
    layers = defaultdict(list)
    for line in open(path):
        j = json.loads(line)
        v = np.array(j['v'], dtype=float)
        P, UV = v[:, :3], v[:, 3:]
        n = np.cross(P[1] - P[0], P[2] - P[0])
        if np.linalg.norm(n) < 1e-9:
            continue
        n /= np.linalg.norm(n)
        layers[j['layer']].append((j['part'] + '#' + str(j['cube']), P, UV, n))
    found = 0
    for layer, polys in sorted(layers.items()):
        hits = []
        for i in range(len(polys)):
            ci, Pi, Ui, ni = polys[i]
            for k in range(i + 1, len(polys)):
                ck, Pk, Uk, nk = polys[k]
                if ci == ck or np.dot(ni, nk) < 0.9999 or abs(np.dot(Pk[0] - Pi[0], ni)) > 0.01:
                    continue
                ax = Pi[1] - Pi[0]
                ax /= np.linalg.norm(ax)
                ay = np.cross(ni, ax)
                A = ccw([(p @ ax, p @ ay) for p in Pi])
                B = ccw([(p @ ax, p @ ay) for p in Pk])
                inter = clip(A, B)
                ov = area(inter)
                if ov < min_ov:
                    continue
                c2 = np.mean(np.array(inter), axis=0)
                q = Pi[0] + ((c2[0] - Pi[0] @ ax) * ax) + ((c2[1] - Pi[0] @ ay) * ay)
                d_uv = np.abs(uv_at(Pi, Ui, q) - uv_at(Pk, Uk, q)).max()
                if d_uv > 1e-4:
                    hits.append((ov, ci, ck, ni))
        if hits:
            print(layer)
            for ov, ci, ck, n in sorted(hits, key=lambda h: -h[0]):
                print('   %-40s %-40s normal %s overlap %.2f px^2' % (ci, ck, np.round(n, 2), ov))
            found += len(hits)
    print('%d layers, %d coplanar overlaps with different pixels' % (len(layers), found))


if __name__ == '__main__':
    main()
