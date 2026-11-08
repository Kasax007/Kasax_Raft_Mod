"""Last pass over the worldgen JSON (configured and placed features, processor lists, structures): the 26.3 forms
the first conversion left alone, found when 1.21.1 loaded the data. Safe to run again on converted files."""
from . import feature

# blocks the newer game renamed
BLOCK_RENAMES = {'minecraft:iron_chain': 'minecraft:chain'}
# 1.21.1 members of #minecraft:corals (the coral plants and fans), for the newer game's random_block provider
CORALS = ['minecraft:%s_coral%s' % (c, s) for c in ('tube', 'brain', 'bubble', 'fire', 'horn') for s in ('', '_fan')]
WALL_CORALS = ['minecraft:%s_coral_wall_fan' % c for c in ('tube', 'brain', 'bubble', 'fire', 'horn')]
RANDOM_BLOCK_TAGS = {'#minecraft:corals': CORALS, '#minecraft:wall_corals': WALL_CORALS}
STATE_KEYS = ('block_state', 'output_state', 'state', 'default_state', 'to_place_state')


def _is_state(v):
    return isinstance(v, str) and ':' in v or isinstance(v, dict) and 'id' in v and 'type' not in v and 'Name' not in v


def walk(o, kind):
    if isinstance(o, list):
        return [walk(x, kind) for x in o]
    if isinstance(o, str):
        return BLOCK_RENAMES.get(o, o)
    if not isinstance(o, dict):
        return o
    o = {k: walk(v, kind) for k, v in o.items()}
    t = o.get('type')
    # int trapezoid (min/max/plateau): Beach Craft's own provider on 1.21.1 (the height provider has min_inclusive)
    if t == 'minecraft:trapezoid' and 'min' in o and 'max' in o and isinstance(o['min'], int):
        o['type'] = 'raft:trapezoid'
    if kind in ('placed_feature', 'configured_feature') and t == 'minecraft:random_chance' and 'chance' in o:
        o['type'] = 'raft:random_chance'
    if t in ('minecraft:rotated', 'minecraft:rotated_block_provider'):
        st = o['state']
        if isinstance(st, dict) and st.get('type') == 'minecraft:weighted_state_provider':
            # a random block turned to a random side: every block in all four horizontal facings
            entries = []
            for e in st['entries']:
                for f in ('north', 'south', 'east', 'west'):
                    d = dict(e['data'])
                    d['Properties'] = dict(d.get('Properties', {}), facing=f)
                    entries.append({'data': d, 'weight': e.get('weight', 1)})
            o = {'type': 'minecraft:weighted_state_provider', 'entries': entries}
        else:
            o = {'type': 'minecraft:rotated_block_provider', 'state': feature.state(st)}
    # a block chosen by rules (1.21.1 has no rule based block state provider): Beach Craft's raft:rule_block
    if t == 'minecraft:simple_block':
        cfg = o.get('config', {})
        tp = cfg.get('to_place')
        if isinstance(tp, dict) and 'fallback' in tp and 'rules' in tp:
            o = {'type': 'raft:rule_block', 'config': {'fallback': tp['fallback'], 'rules': tp['rules']}}
    if o.get('processor_type') == 'minecraft:block_ignore':
        o['blocks'] = [feature.state(b) for b in o.get('blocks', [])]
    if t == 'minecraft:random_block' and o.get('blocks') in RANDOM_BLOCK_TAGS:
        o = {'type': 'minecraft:weighted_state_provider',
             'entries': [{'data': {'Name': b}, 'weight': 1} for b in RANDOM_BLOCK_TAGS[o['blocks']]]}
    if t == 'minecraft:weighted_random_selector':
        cfg = o.get('config', o)
        entries = cfg.get('features', [])
        total = sum(e.get('weight', 1) for e in entries)
        rest = total
        feats = []
        for e in entries[:-1]:
            w = e.get('weight', 1)
            feats.append({'chance': round(w / rest, 4) if rest else 0.0, 'feature': e['data']})
            rest -= w
        o = {'type': 'minecraft:random_selector', 'config': {'features': feats, 'default': entries[-1]['data']}}
    if t == 'minecraft:would_survive' and _is_state(o.get('state')):
        o['state'] = feature.state(o['state'])
    for k in STATE_KEYS:
        if k in o and _is_state(o[k]):
            o[k] = feature.state(o[k])
    # the newer game's coral mushroom (a cuboid of coral): the 1.21.1 coral mushroom feature
    if isinstance(o.get('features'), list):
        fs = []
        for e in o['features']:
            pl = e.get('placement') if isinstance(e, dict) else None
            if isinstance(pl, list) and any(isinstance(p, dict) and p.get('type') == 'minecraft:cuboid' for p in pl):
                e = {'feature': {'type': 'minecraft:coral_mushroom', 'config': {}}, 'placement': []}
            fs.append(e)
        o['features'] = fs
    # structure spawn overrides: count provider -> minCount / maxCount
    if kind == 'structure' and 'type' in o and 'weight' in o and 'count' in o and 'minCount' not in o:
        c = o.pop('count')
        lo, hi = (c, c) if isinstance(c, int) else (c.get('min_inclusive', 1), c.get('max_inclusive', 1))
        o['minCount'], o['maxCount'] = lo, hi
    return o
