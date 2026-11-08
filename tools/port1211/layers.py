"""Block render layers for 1.21.1. The newer game picks a block's layer from its textures (pixels fully clear: cutout,
half clear: translucent); 1.21.1 needs them registered (BlockRenderLayerMap). This works the same out from the
textures of each block's models and writes assets/raft/port1211/render_layers.json, read by RaftClient."""
import json
import os

from PIL import Image


def _load(p):
    with open(p, encoding='utf-8') as f:
        return json.load(f)


def build(roots, out_path):
    blockstates, models, textures = {}, {}, {}
    for r in roots:
        a = os.path.join(r, 'assets')
        if not os.path.isdir(a):
            continue
        for ns in os.listdir(a):
            for kind, store, ext in (('blockstates', blockstates, '.json'), ('models', models, '.json'), ('textures', textures, '.png')):
                d = os.path.join(a, ns, kind)
                for dp, dn, fn in os.walk(d):
                    for f in fn:
                        if f.endswith(ext):
                            rel = os.path.relpath(os.path.join(dp, f), d)[:-len(ext)].replace(os.sep, '/')
                            store[ns + ':' + rel] = os.path.join(dp, f)

    def norm(i):
        return i if ':' in i else 'minecraft:' + i

    alpha_cache = {}

    def tex_kind(t):
        t = norm(t)
        if t not in textures:
            return 'solid'
        if t not in alpha_cache:
            im = Image.open(textures[t]).convert('RGBA')
            a = im.getchannel('A').getdata()
            part = sum(1 for v in a if 0 < v < 250)
            clear = sum(1 for v in a if v == 0)
            alpha_cache[t] = 'translucent' if part > 0 else 'cutout' if clear > 0 else 'solid'
        return alpha_cache[t]

    def model_textures(m, seen=None):
        seen = seen or set()
        m = norm(m)
        if m in seen or m not in models:
            return {}, False
        seen.add(m)
        j = _load(models[m])
        tex, has_el = {}, 'elements' in j
        if 'parent' in j:
            tex, pe = model_textures(j['parent'], seen)
            has_el = has_el or pe
        for k, v in j.get('textures', {}).items():
            tex[k] = v
        return tex, has_el

    def resolve(tex, v, depth=0):
        while isinstance(v, str) and v.startswith('#') and depth < 10:
            v = tex.get(v[1:], v)
            depth += 1
        return v if isinstance(v, str) and not v.startswith('#') else None

    def models_of(bs):
        out = []
        def add(m):
            for x in (m if isinstance(m, list) else [m]):
                if isinstance(x, dict) and 'model' in x:
                    out.append(x['model'])
        for v in bs.get('variants', {}).values():
            add(v)
        for part in bs.get('multipart', []):
            add(part.get('apply', {}))
        return out

    rank = {'solid': 0, 'cutout': 1, 'translucent': 2}
    result = {'cutout': [], 'translucent': []}
    for bid, p in sorted(blockstates.items()):
        if not bid.startswith('raft:'):
            continue
        kind = 'solid'
        for m in models_of(_load(p)):
            tex, _ = model_textures(m)
            for k, v in tex.items():
                if k == 'particle':
                    continue
                t = resolve(tex, v)
                if t:
                    k2 = tex_kind(t)
                    if rank[k2] > rank[kind]:
                        kind = k2
        if kind != 'solid':
            result[kind].append(bid)
    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    with open(out_path, 'w', encoding='utf-8') as f:
        json.dump(result, f, indent=2)
        f.write('\n')
    return {k: len(v) for k, v in result.items()}
