"""Advancements: 26.3 JSON -> 1.21.1 JSON (context aware predicates as lists of loot conditions)."""
from . import loot

CONTEXT_KEYS = {'player', 'entity', 'parent', 'partner', 'child', 'lightning', 'bystander', 'projectile', 'shooter',
                'villager', 'zombie', 'source', 'victims', 'killing_blow_entity', 'from', 'target', 'location', 'cause'}
DAMAGE_KEYS = {'killing_blow', 'damage'}


def context(v):
    if isinstance(v, dict) and 'type' in v and isinstance(v['type'], str) and ':' in v['type']:
        return [loot.condition(v)]
    if isinstance(v, str):
        return [loot.condition(v)]
    if isinstance(v, list):
        # entries already in the 1.21.1 form ({"condition": ...}) stay as they are
        return [x if isinstance(x, dict) and 'condition' in x else context(x)[0] if isinstance(x, (dict, str)) else x for x in v]
    return v


def criterion(c):
    c = dict(c)
    cond = c.get('conditions')
    if isinstance(cond, dict):
        cond = dict(cond)
        for k, v in list(cond.items()):
            if k in CONTEXT_KEYS:
                if k == 'victims' and isinstance(v, list):
                    cond[k] = [context(x) for x in v]
                elif k == 'source' and c.get('trigger') not in ('minecraft:channeled_lightning',):
                    cond[k] = context(v)
                else:
                    cond[k] = context(v)
            elif k in DAMAGE_KEYS and isinstance(v, dict):
                d = dict(v)
                if 'type' in d and isinstance(d['type'], dict):
                    d['type'] = loot.damage_predicate(d['type'])
                if 'source_entity' in d:
                    d['source_entity'] = loot.entity_predicate(d['source_entity'])
                cond[k] = d
        trig = c.get('trigger')
        if trig == 'minecraft:recipe_unlocked' and 'recipes' in cond:
            cond['recipe'] = cond.pop('recipes')
        if trig in ('minecraft:enter_block', 'minecraft:bee_nest_destroyed') and 'blocks' in cond:
            cond['block'] = cond.pop('blocks')
        c['conditions'] = cond
    return c


def advancement(a):
    a = dict(a)
    disp = a.get('display')
    if isinstance(disp, dict) and isinstance(disp.get('background'), str) and '/textures/' not in disp['background'] \
            and not disp['background'].endswith('.png'):
        ns, path = disp['background'].split(':', 1) if ':' in disp['background'] else ('minecraft', disp['background'])
        a['display'] = dict(disp, background='%s:textures/%s.png' % (ns, path))
    if 'criteria' in a:
        a['criteria'] = {k: criterion(v) for k, v in a['criteria'].items()}
    return a
