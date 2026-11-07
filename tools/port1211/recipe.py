"""Recipes: 26.3 JSON -> 1.21.1 JSON (ingredients as objects, transmute recipes through Beach Craft's own type)."""


def ingredient(v):
    if isinstance(v, str):
        return {'tag': v[1:]} if v.startswith('#') else {'item': v}
    if isinstance(v, list):
        return [ingredient(x) for x in v]
    return v


def recipe(r):
    r = dict(r)
    t = r.get('type')
    if 'key' in r:
        r['key'] = {k: ingredient(v) for k, v in r['key'].items()}
    if 'ingredients' in r:
        r['ingredients'] = [ingredient(x) for x in r['ingredients']]
    for k in ('ingredient', 'base', 'addition', 'template', 'input', 'material'):
        if k in r and k != 'input' or (k == 'input' and t == 'minecraft:crafting_transmute'):
            if k in r:
                r[k] = ingredient(r[k])
    if t == 'minecraft:crafting_transmute':
        r['type'] = 'raft:crafting_transmute'
    return r
