"""Loot tables, loot conditions and item modifiers: 26.3 JSON -> 1.21.1 JSON."""

# 26.3 named predicates used inline (1.21.1 has no predicate registry entries for them)
PREDICATES = {
    'minecraft:tool/can_shear': {'type': 'minecraft:match_tool', 'predicate': {'items': 'minecraft:shears'}},
    'minecraft:tool/can_silk_touch': {'type': 'minecraft:match_tool', 'predicate': {'predicates': {
        'minecraft:enchantments': [{'enchantments': 'minecraft:silk_touch', 'levels': {'min': 1}}]}}},
}


def as_list(v):
    if v is None:
        return []
    return v if isinstance(v, list) else [v]


def condition(c):
    if isinstance(c, str):
        if c not in PREDICATES:
            raise ValueError('unknown predicate reference ' + c)
        c = PREDICATES[c]
    if 'condition' in c and 'type' not in c:   # already 1.21.1
        return c
    c = dict(c)
    t = c.pop('type')
    out = {'condition': t}
    if t == 'minecraft:match_block':
        out['condition'] = 'minecraft:block_state_property'
        out['block'] = c.pop('blocks')
        if 'state' in c:
            out['properties'] = c.pop('state')
    elif t in ('minecraft:all_of', 'minecraft:any_of'):
        out['terms'] = [condition(x) for x in c.pop('terms')]
    elif t == 'minecraft:inverted':
        out['term'] = condition(c.pop('term'))
    if t == 'minecraft:entity_properties' and 'predicate' in c:
        c['predicate'] = entity_predicate(c['predicate'])
    if t == 'minecraft:damage_source_properties' and 'predicate' in c:
        c['predicate'] = damage_predicate(c['predicate'])
    out.update(c)
    return out


def conditions(v):
    return [condition(x) for x in as_list(v)]


def function(f):
    if 'function' in f and 'type' not in f:
        return f
    f = dict(f)
    t = f.pop('type')
    out = {'function': t}
    if 'condition' in f:
        out['conditions'] = conditions(f.pop('condition'))
    if t == 'minecraft:filtered':
        if 'on_fail' in f:
            out['on_fail'] = function(f.pop('on_fail'))
        if 'on_pass' in f:
            out['on_pass'] = function(f.pop('on_pass'))
    if t == 'minecraft:modify_contents' and 'modifier' in f:
        out['modifier'] = functions(f.pop('modifier'))
    out.update(f)
    return out


def functions(v):
    if isinstance(v, dict) and v.get('type') == 'minecraft:sequence':
        v = v['functions']
    return [function(x) for x in as_list(v)]


def entry(e):
    e = dict(e)
    out = {'type': e.pop('type')}
    if 'condition' in e:
        out['conditions'] = conditions(e.pop('condition'))
    if 'modifier' in e:
        out['functions'] = functions(e.pop('modifier'))
    if 'children' in e:
        out['children'] = [entry(x) for x in e.pop('children')]
    if out['type'] == 'minecraft:tag' and 'items' in e:
        name = e.pop('items')
        e['name'] = name[1:] if name.startswith('#') else name
    if out['type'] == 'minecraft:loot_table' and isinstance(e.get('value'), dict):
        e['value'] = table(e['value'])
    out.update(e)
    return out


def pool(p):
    p = dict(p)
    out = {}
    if 'condition' in p:
        out['conditions'] = conditions(p.pop('condition'))
    if 'modifier' in p:
        out['functions'] = functions(p.pop('modifier'))
    out['entries'] = [entry(x) for x in p.pop('entries', [])]
    out.update(p)
    return out


def table(t):
    t = dict(t)
    out = {}
    for k, v in t.items():
        if k == 'pools':
            out['pools'] = [pool(x) for x in v]
        elif k == 'modifier':
            out['functions'] = functions(v)
        else:
            out[k] = v
    return out


# ---------------------------------------------------------------------------------------------------- predicates
TYPE_SPECIFIC = {'cube_mob': 'minecraft:slime'}


def entity_predicate(p):
    """26.3 entity predicate (component style keys) -> 1.21.1 entity predicate."""
    if not isinstance(p, dict):
        return p
    out = {}
    for k, v in p.items():
        if k == 'minecraft:entity_type':
            out['type'] = v
        elif k.startswith('minecraft:type_specific/'):
            kind = k.split('/', 1)[1]
            d = {'type': TYPE_SPECIFIC.get(kind, 'minecraft:' + kind)}
            d.update(v if isinstance(v, dict) else {})
            out['type_specific'] = type_specific(d)
        elif k.startswith('minecraft:'):
            name = k[len('minecraft:'):]
            if name in ('vehicle', 'passenger', 'targeted_entity'):
                v = entity_predicate(v)
            out[name] = v
        elif k in ('vehicle', 'passenger', 'targeted_entity'):
            out[k] = entity_predicate(v)
        else:
            out[k] = v
    return out


def type_specific(d):
    if d.get('type') == 'minecraft:player' and 'looking_at' in d:
        d['looking_at'] = entity_predicate(d['looking_at'])
    return d


def damage_predicate(p):
    if not isinstance(p, dict):
        return p
    out = dict(p)
    for k in ('source_entity', 'direct_entity'):
        if k in out:
            out[k] = entity_predicate(out[k])
    if 'tags' in out:
        out['tags'] = [dict(t, id=t['id'][1:] if t.get('id', '').startswith('#') else t.get('id')) for t in out['tags']]
    return out


def predicate_values(k, v):
    return v
