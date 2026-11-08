"""Brings loot tables written in the pre-26.3 shape into the 26.3 format.

26.3 reads `condition` (one condition; several become minecraft:all_of with `terms`) and `modifier` (one or a list)
with `type` keys; the old `conditions` / `functions` lists with `condition` / `function` keys are silently ignored,
which drops set_count, copy_components, fortune and the ripe/half checks. block_state_property became match_block
(`blocks`, `state`).
"""
import glob
import json
import os


def _cond(c):
    c = dict(c)
    if 'condition' in c and isinstance(c['condition'], str):
        c['type'] = c.pop('condition')
    if c.get('type') == 'minecraft:block_state_property':
        c['type'] = 'minecraft:match_block'
        if 'block' in c:
            c['blocks'] = c.pop('block')
        if 'properties' in c:
            c['state'] = c.pop('properties')
    if 'terms' in c:
        c['terms'] = [_cond(t) for t in c['terms']]
    return c


def _conds(lst):
    lst = [_cond(c) for c in lst]
    return lst[0] if len(lst) == 1 else {'type': 'minecraft:all_of', 'terms': lst}


def _fn(f):
    f = dict(f)
    if 'function' in f:
        f['type'] = f.pop('function')
    if 'conditions' in f:
        f['condition'] = _conds(f.pop('conditions'))
    elif isinstance(f.get('condition'), dict):
        f['condition'] = _cond(f['condition'])
    return f


def _walk(o):
    if isinstance(o, list):
        return [_walk(x) for x in o]
    if not isinstance(o, dict):
        return o
    o = {k: _walk(v) for k, v in o.items()}
    if 'functions' in o:
        fs = [_fn(f) for f in o.pop('functions')]
        o['modifier'] = fs[0] if len(fs) == 1 else fs
    if 'conditions' in o:
        o['condition'] = _conds(o.pop('conditions'))
    elif isinstance(o.get('condition'), dict):
        o['condition'] = _cond(o['condition'])
    if 'modifier' in o:
        m = o['modifier']
        o['modifier'] = [_fn(x) for x in m] if isinstance(m, list) else _fn(m)
    return o


def modernize(module_root):
    n = 0
    for p in glob.glob(os.path.join(module_root, 'data', '*', 'loot_table', '**', '*.json'), recursive=True):
        d = json.load(open(p))
        new = _walk(d)
        if new != d:
            with open(p, 'w', encoding='utf-8') as f:
                json.dump(new, f, indent=2)
                f.write('\n')
            n += 1
    return n
