"""Biomes, noise settings (with surface rules), dimension types: 26.3 JSON -> 1.21.1 JSON."""
import copy
import json
import os

from . import density, noise as noise_mod

V26_MATERIAL = None  # set by convert.py: dict id -> 26.3 vanilla material rule / condition JSON


def color(v):
    if isinstance(v, str) and v.startswith('#'):
        return int(v[1:], 16) & 0xFFFFFF
    if isinstance(v, int):
        return v & 0xFFFFFF
    return v


def _music(m):
    return {'sound': m['sound'], 'min_delay': m.get('min_delay', 12000), 'max_delay': m.get('max_delay', 24000),
            'replace_current_music': m.get('replace_current_music', False)}


def spawner(e):
    count = e.get('count', 1)
    if isinstance(count, dict):
        lo, hi = count.get('min_inclusive', 1), count.get('max_inclusive', 1)
    else:
        lo = hi = count
    return {'type': e['type'], 'weight': e.get('weight', 1), 'minCount': lo, 'maxCount': hi}


DIM_MOOD = {'sound': 'raft:ambient.raft.deep', 'tick_delay': 6000, 'block_search_extent': 8, 'offset': 2.0}


def biome(b, extras):
    """extras: dict to collect facts for other files (underwater music tag, water fog factors)."""
    attrs = b.get('attributes', {})
    eff = dict(b.get('effects', {}))
    out_eff = {}
    for k in ('water_color', 'foliage_color', 'grass_color'):
        if k in eff:
            out_eff[k] = color(eff[k])
    if 'grass_color_modifier' in eff:
        out_eff['grass_color_modifier'] = eff['grass_color_modifier']
    out_eff['fog_color'] = color(attrs.get('minecraft:visual/fog_color', '#c0d8ff'))
    out_eff['sky_color'] = color(attrs.get('minecraft:visual/sky_color', '#78a7ff'))
    out_eff['water_fog_color'] = color(attrs.get('minecraft:visual/water_fog_color', '#050533'))
    out_eff.setdefault('water_color', 0x3F76E4)
    music = attrs.get('minecraft:audio/background_music')
    if isinstance(music, dict) and 'default' in music:
        out_eff['music'] = _music(music['default'])
        if 'underwater' in music:
            extras['underwater_music'] = True
    out_eff['mood_sound'] = dict(DIM_MOOD)
    parts = attrs.get('minecraft:visual/ambient_particles')
    if isinstance(parts, dict) and parts.get('argument'):
        p = parts['argument'][0]
        out_eff['particle'] = {'options': p['particle'], 'probability': p['probability']}
    fog = attrs.get('minecraft:visual/water_fog_end_distance')
    if isinstance(fog, dict) and fog.get('modifier') == 'multiply':
        extras['water_fog_factor'] = fog['argument']
    spawns = attrs.get('minecraft:gameplay/natural_mob_spawns', {}).get('argument', {})
    spawners = {cat: [spawner(e) for e in lst] for cat, lst in spawns.get('spawns_by_category', {}).items()}
    costs = {}
    for ent, c in spawns.get('spawn_costs', {}).items():
        costs[ent] = {'energy_budget': c.get('energy_budget', c.get('budget')), 'charge': c.get('charge')}
    carvers = b.get('carvers', [])
    if isinstance(carvers, list):
        carvers = {'air': carvers} if carvers else {}
    out = {
        'has_precipitation': b.get('has_precipitation', True),
        'temperature': b.get('temperature', 0.5),
        'downfall': b.get('downfall', 0.5),
        'effects': out_eff,
        'spawners': spawners,
        'spawn_costs': costs,
        'carvers': carvers,
        'features': b.get('features', []),
    }
    if 'temperature_modifier' in b:
        out['temperature_modifier'] = b['temperature_modifier']
    gen = attrs.get('minecraft:gameplay/creature_world_gen_spawn_probability')
    if gen is not None:
        # 1.21.1 keeps the chance of creatures placed with a new chunk in the biome itself
        out['creature_spawn_probability'] = gen['argument'] if isinstance(gen, dict) else gen
    return out


# ------------------------------------------------------------------------------------------- surface rules
def _resolve(ref, kind, mod_rules):
    if ref in mod_rules:
        return mod_rules[ref]
    data = V26_MATERIAL.get((kind, ref))
    if data is None:
        raise ValueError('unknown %s %s' % (kind, ref))
    return data


def condition(c, mod_rules):
    if isinstance(c, str):
        c = _resolve(c, 'material_condition', mod_rules)
    c = dict(c)
    t = c['type']
    if t == 'minecraft:not':
        c['invert'] = condition(c['invert'], mod_rules)
    if t == 'minecraft:noise_threshold':
        c.pop('is_3d', None)
    return c


def rule(r, mod_rules):
    if isinstance(r, str):
        r = _resolve(r, 'material_rule', mod_rules)
    r = dict(r)
    t = r['type']
    if t == 'minecraft:sequence':
        seq = []
        for x in r['sequence']:
            y = rule(x, mod_rules)
            if y is not None:
                seq.append(y)
        return {'type': t, 'sequence': seq}
    if t == 'minecraft:condition':
        then = rule(r['then_run'], mod_rules)
        if then is None:
            return None
        return {'type': t, 'if_true': condition(r['if_true'], mod_rules), 'then_run': then}
    if t == 'minecraft:block':
        from .feature import state
        return {'type': t, 'result_state': state(r['result_state'])}
    if t == 'minecraft:ore_vein':
        return None   # 1.21.1: ore veins come from the noise router (vein_toggle/ridged/gap), see noise_settings
    return r


def noise_settings(n, mod_rules, router_extra):
    from .feature import state
    r = n['noise_router']
    rt = {}
    for k in ('temperature', 'vegetation', 'continents', 'erosion', 'depth', 'ridges', 'final_density'):
        rt[k] = density.df(r.get(k, 0.0))
    for k in ('barrier', 'fluid_level_floodedness', 'fluid_level_spread', 'lava', 'vein_toggle', 'vein_ridged', 'vein_gap'):
        rt[k] = 0.0
    # 26.3 gives the chunk's surface height directly; 1.21.1 finds it where this density first exceeds 0.390625
    csl = r.get('chunk_surface_level')
    if isinstance(csl, str):
        csl = router_extra['density'][csl]
    if isinstance(csl, dict) and csl.get('type') == 'minecraft:interpolated':
        csl = csl['input']
    surf = density.df(csl) if csl is not None else 64.0
    rt['initial_density_without_jaggedness'] = {
        'type': 'minecraft:add', 'argument1': 0.390625,
        'argument2': {'type': 'minecraft:mul', 'argument1': 0.125,
                      'argument2': {'type': 'minecraft:add', 'argument1': surf,
                                    'argument2': {'type': 'minecraft:mul', 'argument1': 'minecraft:y', 'argument2': -1.0}}}}
    has_veins = router_extra.get('has_ore_veins', False)
    if has_veins:
        rt.update(router_extra['vanilla_veins'])
    noise = dict(n['noise'])
    noise.setdefault('size_horizontal', 1)
    noise.setdefault('size_vertical', 2)
    surface = rule(n['material_rule'], mod_rules)
    names = {'continents': 'continentalness', 'erosion': 'erosion', 'ridges': 'weirdness', 'temperature': 'temperature',
             'vegetation': 'humidity'}
    spawn = []
    for point in n.get('spawn_target', []):
        p = {v: [-2.0, 2.0] for v in names.values()}
        for k, val in point.items():
            short = k.split('/')[-1]
            if short in names:
                p[names[short]] = val
        p['depth'] = 0.0
        p['offset'] = 0.0
        spawn.append(p)
    return {
        'sea_level': n['sea_level'],
        'disable_mob_generation': n.get('disable_mob_generation', False),
        'aquifers_enabled': bool(n.get('aquifers')),
        'ore_veins_enabled': has_veins,
        'legacy_random_source': n.get('legacy_random_source', False),
        'default_block': state(n['default_block']),
        'default_fluid': dict(state(n['default_fluid']), Properties={'level': '0'}) if 'water' in json.dumps(n['default_fluid']) else state(n['default_fluid']),
        'noise': noise,
        'noise_router': rt,
        'spawn_target': spawn,
        'surface_rule': surface,
    }


def dimension_type(d):
    attrs = d.get('attributes', {})
    out = {
        'ultrawarm': False,
        'natural': True,
        'coordinate_scale': d.get('coordinate_scale', 1.0),
        'has_skylight': d.get('has_skylight', True),
        'has_ceiling': d.get('has_ceiling', False),
        'ambient_light': d.get('ambient_light', 0.0),
        'monster_spawn_light_level': d.get('monster_spawn_light_level', 0),
        'monster_spawn_block_light_limit': d.get('monster_spawn_block_light_limit', 0),
        'piglin_safe': False,
        'bed_works': True,
        'respawn_anchor_works': bool(attrs.get('minecraft:gameplay/respawn_anchor_works', False)),
        'has_raids': True,
        'logical_height': d['logical_height'],
        'min_y': d['min_y'],
        'height': d['height'],
        'infiniburn': d['infiniburn'],
        'effects': 'minecraft:overworld',
    }
    if d.get('fixed_time') is not None:
        out['fixed_time'] = d['fixed_time']
    return out
