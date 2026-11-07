"""Configured features, placed features, block state providers, block predicates: 26.3 JSON -> 1.21.1 JSON.

The newer game stores a configured feature flat ({"type": t, ...settings}); 1.21.1 nests the settings under
"config". Feature types that 1.21.1 lacks are Beach Craft's own (raft:overlay, raft:block_blob, raft:fallen_tree)."""

MOD_TYPES_26_ONLY = {
    'minecraft:overlay': 'raft:overlay',
    'minecraft:block_blob': 'raft:block_blob',
    'minecraft:fallen_tree': 'raft:fallen_tree',
}
CORAL = {'minecraft:coral_tree', 'minecraft:coral_claw', 'minecraft:coral_mushroom'}


def state(s):
    """block state: 26.3 "ns:id" or {"id", "properties"} -> 1.21.1 {"Name", "Properties"}."""
    if isinstance(s, str):
        if '[' in s:
            name, props = s[:-1].split('[', 1)
            p = dict(kv.split('=', 1) for kv in props.split(',') if kv)
            return {'Name': name, 'Properties': p}
        return {'Name': s}
    if isinstance(s, dict) and 'id' in s and 'Name' not in s:
        out = {'Name': s['id']}
        if s.get('properties'):
            out['Properties'] = {k: str(v).lower() if isinstance(v, bool) else str(v) for k, v in s['properties'].items()}
        return out
    return s


def provider(p):
    if isinstance(p, str) or (isinstance(p, dict) and ('id' in p or 'Name' in p) and 'type' not in p):
        return {'type': 'minecraft:simple_state_provider', 'state': state(p)}
    p = dict(p)
    t = p.get('type')
    if t in ('minecraft:weighted', 'minecraft:weighted_state_provider'):
        return {'type': 'minecraft:weighted_state_provider',
                'entries': [{'data': state(e['data']), 'weight': e.get('weight', 1)} for e in p['entries']]}
    if t in ('minecraft:randomized_int', 'minecraft:randomized_int_state_provider'):
        return {'type': 'minecraft:randomized_int_state_provider', 'property': p['property'], 'source': provider(p['source']),
                'values': p['values']}
    if t in ('minecraft:rule_based', 'minecraft:rule_based_state_provider'):
        return rule_based(p)
    if t in ('minecraft:noise_threshold', 'minecraft:noise_threshold_provider'):
        p['type'] = 'minecraft:noise_threshold_provider'
        for k in ('default_state',):
            if k in p:
                p[k] = state(p[k])
        for k in ('low_states', 'high_states'):
            if k in p:
                p[k] = [state(x) for x in p[k]]
        return p
    if t in ('minecraft:noise', 'minecraft:noise_provider', 'minecraft:dual_noise', 'minecraft:dual_noise_provider'):
        p['type'] = t if t.endswith('_provider') else t + '_provider'
        if 'states' in p:
            p['states'] = [state(x) for x in p['states']]
        return p
    if t in ('minecraft:rotated_block', 'minecraft:rotated_block_provider'):
        return {'type': 'minecraft:rotated_block_provider', 'state': state(p['state'])}
    if t == 'raft:randomized_block_state_provider':
        return {'type': t, 'primary': provider(p['primary']), 'secondary': provider(p['secondary']),
                'secondary_chance': p['secondary_chance']}
    if t == 'minecraft:simple_state_provider':
        return {'type': t, 'state': state(p['state'])}
    return p


def rule_based(p):
    """1.21.1 writes this provider without a type (it is not dispatched)."""
    return {'fallback': provider(p['fallback']) if p.get('fallback') is not None else None,
            'rules': [{'if_true': block_predicate(r['if_true']), 'then': provider(r['then'])} for r in p.get('rules', [])]}


def block_predicate(b):
    if not isinstance(b, dict):
        return b
    b = dict(b)
    t = b.get('type')
    if t in ('minecraft:all_of', 'minecraft:any_of'):
        b['predicates'] = [block_predicate(x) for x in b['predicates']]
    elif t == 'minecraft:not':
        b['predicate'] = block_predicate(b['predicate'])
    elif t == 'minecraft:matching_block_tag' and isinstance(b.get('tag'), str) and b['tag'].startswith('#'):
        b['tag'] = b['tag'][1:]
    return b


def placed(p):
    """placed feature (or inline placed feature)."""
    if isinstance(p, str):
        return p
    p = dict(p)
    out = {'feature': configured(p['feature']), 'placement': [modifier(m) for m in p.get('placement', [])]}
    return out


def modifier(m):
    m = dict(m)
    t = m.get('type')
    if t == 'minecraft:offset':
        # 26.3's fixed offset; 1.21.1 only has random_offset: the mod registers raft:offset (OffsetPlacement)
        return {'type': 'raft:offset', 'x': m.get('x', 0), 'y': m.get('y', 0), 'z': m.get('z', 0)}
    if t == 'minecraft:block_predicate_filter':
        m['predicate'] = block_predicate(m['predicate'])
    if t == 'minecraft:environment_scan':
        m['target_condition'] = block_predicate(m['target_condition'])
        if 'allowed_search_condition' in m:
            m['allowed_search_condition'] = block_predicate(m['allowed_search_condition'])
    if t == 'minecraft:height_range' and 'height' in m:
        pass
    return m


def configured(f):
    if isinstance(f, str):
        return f
    if 'config' in f:
        return f
    f = dict(f)
    t = f.pop('type')
    t = MOD_TYPES_26_ONLY.get(t, t)
    if t in CORAL:
        return {'type': t, 'config': {}}
    c = f
    if t == 'minecraft:simple_block':
        c['to_place'] = provider(c['to_place'])
        c.setdefault('schedule_tick', False)
    elif t == 'minecraft:ore' or t == 'minecraft:scattered_ore':
        c['targets'] = [{'state': state(x['state']), 'target': x['target']} for x in c['targets']]
    elif t == 'minecraft:disk':
        c['state_provider'] = rule_based(c['state_provider']) if c['state_provider'].get('type', '').startswith('minecraft:rule_based') \
            else {'fallback': provider(c['state_provider']), 'rules': []}
        c['target'] = block_predicate(c['target'])
    elif t == 'minecraft:block_column':
        c['layers'] = [{'height': l['height'], 'provider': provider(l['provider'])} for l in c['layers']]
        c['allowed_placement'] = block_predicate(c['allowed_placement'])
        c.setdefault('prioritize_tip', False)
    elif t == 'minecraft:multiface_growth':
        c.setdefault('can_place_on_floor', False)
        c.setdefault('can_place_on_ceiling', False)
        c.setdefault('can_place_on_wall', False)
        c.setdefault('chance_of_spreading', 0.5)
        c.setdefault('search_range', 10)
    elif t in ('minecraft:simple_random_selector',):
        c['features'] = [placed(x) for x in c['features']]
    elif t in ('minecraft:random_selector',):
        c['features'] = [dict(x, feature=placed(x['feature'])) for x in c['features']]
        c['default'] = placed(c['default'])
    elif t == 'minecraft:random_boolean_selector':
        c['feature_true'] = placed(c['feature_true'])
        c['feature_false'] = placed(c['feature_false'])
    elif t == 'minecraft:tree':
        c = tree(c)
    elif t == 'raft:overlay':
        c['features'] = [placed(x) for x in c['features']]
    elif t == 'raft:block_blob':
        c['state'] = state(c['state'])
        if 'can_place_on' in c:
            c['can_place_on'] = block_predicate(c['can_place_on'])
    elif t == 'raft:fallen_tree':
        c['trunk_provider'] = provider(c['trunk_provider'])
    elif t == 'raft:rubble':
        c['state_provider'] = provider(c['state_provider'])
    return {'type': t, 'config': c}


def tree(c):
    out = {}
    out['trunk_provider'] = provider(c['trunk_provider'])
    out['foliage_provider'] = provider(c['foliage_provider'])
    below = c.get('below_trunk_provider')
    if below is not None:
        # 26.3: rule based (grass/podzol -> dirt, else fallback); 1.21.1 takes one dirt provider and replaces dirt only
        fb = below.get('fallback') if isinstance(below, dict) and below.get('type', '').startswith('minecraft:rule_based') else below
        out['dirt_provider'] = provider(fb)
        out['force_dirt'] = False
    else:
        out['dirt_provider'] = provider('minecraft:dirt')
        out['force_dirt'] = False
    out['trunk_placer'] = c['trunk_placer']
    out['foliage_placer'] = c['foliage_placer']
    out['minimum_size'] = c.get('minimum_size', {'type': 'minecraft:two_layers_feature_size', 'limit': 1, 'lower_size': 0, 'upper_size': 1})
    out['decorators'] = c.get('decorators', [])
    out['ignore_vines'] = c.get('ignore_vines', False)
    if 'root_placer' in c:
        out['root_placer'] = c['root_placer']
    return out
