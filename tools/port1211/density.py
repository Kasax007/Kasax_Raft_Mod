"""Density functions: 26.3 JSON -> 1.21.1 JSON."""

UNARY = {'minecraft:abs', 'minecraft:square', 'minecraft:cube', 'minecraft:half_negative', 'minecraft:quarter_negative',
         'minecraft:squeeze', 'minecraft:blend_density', 'minecraft:interpolated', 'minecraft:flat_cache',
         'minecraft:cache_2d', 'minecraft:cache_once', 'minecraft:cache_all_in_cell'}
BINARY = {'minecraft:add', 'minecraft:mul', 'minecraft:min', 'minecraft:max'}
Y_REF = 'minecraft:y'


def _is_2d(df):
    """True if the function cannot depend on y (for picking cache_2d over cache_once)."""
    if isinstance(df, (int, float)):
        return True
    if isinstance(df, str):
        return df not in (Y_REF,) and _REFS_2D.get(df, False)
    t = df.get('type')
    if t in ('minecraft:noise',):
        return df.get('y_scale', 1.0) == 0.0
    if t in ('minecraft:gradient', 'minecraft:y_clamped_gradient', 'minecraft:old_blended_noise', 'minecraft:beardifier',
             'minecraft:interpolated', 'minecraft:end_islands', 'minecraft:end_outer_islands'):
        return t == 'minecraft:gradient' and df.get('axis') != 'y'
    for k, v in df.items():
        if k == 'type':
            continue
        if isinstance(v, (dict, str)) and not _is_2d_value(k, v):
            return False
    return True


def _is_2d_value(k, v):
    if k == 'spline':
        return _spline_2d(v)
    if k in ('noise',) and isinstance(v, str):
        return True
    return _is_2d(v)


def _spline_2d(s):
    if isinstance(s, (int, float)):
        return True
    if not _is_2d(s['coordinate']):
        return False
    return all(_spline_2d(p['value']) for p in s['points'])


# filled by convert.py with the 2D-ness of the mod's own named density functions
_REFS_2D = {}


def spline(s):
    if isinstance(s, (int, float)):
        return s
    out = {'coordinate': df(s['coordinate']), 'points': []}
    for p in s['points']:
        q = dict(p)
        q['value'] = spline(p['value'])
        out['points'].append(q)
    return out


def neg(x):
    return {'type': 'minecraft:mul', 'argument1': x, 'argument2': -1.0}


def df(d):
    if isinstance(d, (int, float, str)):
        return d
    d = dict(d)
    t = d.get('type')
    if t in BINARY and 'left' in d:
        return {'type': t, 'argument1': df(d['left']), 'argument2': df(d['right'])}
    if t == 'minecraft:sub':
        return {'type': 'minecraft:add', 'argument1': df(d['left']), 'argument2': neg(df(d['right']))}
    if t == 'minecraft:negate':
        return neg(df(d['input']))
    if t == 'minecraft:lerp':
        first = df(d['first'])
        return {'type': 'minecraft:add', 'argument1': first,
                'argument2': {'type': 'minecraft:mul', 'argument1': df(d['alpha']),
                              'argument2': {'type': 'minecraft:add', 'argument1': df(d['second']), 'argument2': neg(first)}}}
    if t == 'minecraft:cache':
        inner = d['input']
        return {'type': 'minecraft:cache_2d' if _is_2d(inner) else 'minecraft:cache_once', 'argument': df(inner)}
    if t in UNARY and 'input' in d:
        return {'type': t, 'argument': df(d['input'])}
    if t == 'minecraft:gradient':
        if d.get('axis', 'y') != 'y':
            raise ValueError('gradient on axis ' + d['axis'])
        return {'type': 'minecraft:y_clamped_gradient', 'from_y': int(d['from_coordinate']), 'to_y': int(d['to_coordinate']),
                'from_value': d['from_value'], 'to_value': d['to_value']}
    if t == 'minecraft:noise' and ('shift_x' in d or 'shift_z' in d):
        return {'type': 'minecraft:shifted_noise', 'noise': d['noise'], 'xz_scale': d.get('xz_scale', 1.0),
                'y_scale': d.get('y_scale', 1.0), 'shift_x': df(d.get('shift_x', 0.0)), 'shift_y': 0.0,
                'shift_z': df(d.get('shift_z', 0.0))}
    if t in ('minecraft:shift_a', 'minecraft:shift_b', 'minecraft:shift') and 'noise' in d:
        return {'type': t, 'argument': d['noise']}
    if t == 'minecraft:spline':
        return {'type': t, 'spline': spline(d['spline'])}
    if t == 'minecraft:clamp':
        return {'type': t, 'input': df(d['input']), 'min': d['min'], 'max': d['max']}
    if t == 'minecraft:range_choice':
        return {'type': t, 'input': df(d['input']), 'min_inclusive': d['min_inclusive'], 'max_exclusive': d['max_exclusive'],
                'when_in_range': df(d['when_in_range']), 'when_out_of_range': df(d['when_out_of_range'])}
    if t in ('minecraft:interval_select', 'minecraft:find_top_surface', 'minecraft:slice', 'minecraft:div',
             'minecraft:distance_to_point'):
        raise ValueError('density function %s has no 1.21.1 form' % t)
    return d
