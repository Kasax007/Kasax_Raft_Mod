"""Worldgen JSON (26.3 formats, compared with the vanilla files) for the surface story structures."""
from bclib.assets import write_json

from .lib import MODULE


def structure(sid, start_pool, step, adaptation, start_height, heightmap, size, max_dist=80,
              spawn_overrides=None, start_jigsaw=None):
    d = {
        'type': 'minecraft:jigsaw',
        'biomes': f'#raft:has_structure/{sid}',
        'max_distance_from_center': max_dist,
    }
    if heightmap:
        d['project_start_to_heightmap'] = heightmap
    d.update({
        'size': size,
        'spawn_overrides': spawn_overrides or {},
        'start_height': {'absolute': start_height},
        'start_pool': start_pool,
        'step': step,
        'terrain_adaptation': adaptation,
        'use_expansion_hack': False,
    })
    if start_jigsaw:
        d['start_jigsaw_name'] = start_jigsaw
    return write_json(MODULE, f'data/raft/worldgen/structure/{sid}.json', d)


def structure_set(sid, spacing, separation, salt):
    return write_json(MODULE, f'data/raft/worldgen/structure_set/{sid}.json', {
        'placement': {'type': 'minecraft:random_spread', 'salt': salt, 'separation': separation, 'spacing': spacing},
        'structures': [{'structure': f'raft:{sid}', 'weight': 1}],
    })


def pool(path, elements, fallback='minecraft:empty'):
    """elements: list of (location, weight, processors_or_None, projection)."""
    els = []
    for loc, w, proc, proj in elements:
        els.append({'element': {
            'element_type': 'minecraft:single_pool_element',
            'location': loc,
            'processors': proc if proc else {'processors': []},
            'projection': proj,
        }, 'weight': w})
    return write_json(MODULE, f'data/raft/worldgen/template_pool/{path}.json', {'elements': els, 'fallback': fallback})


def biome_tag(sid, biomes):
    return write_json(MODULE, f'data/raft/tags/worldgen/biome/has_structure/{sid}.json',
                      {'values': [{'id': f'raft:{b}', 'required': False} for b in biomes]})


def rule(block, prob, out):
    return {
        'input_predicate': {'block': block, 'predicate_type': 'minecraft:random_block_match', 'probability': prob},
        'location_predicate': {'predicate_type': 'minecraft:always_true'},
        'output_state': out,
    }


def processor_list(name, rules):
    return write_json(MODULE, f'data/raft/worldgen/processor_list/{name}.json',
                      {'processors': [{'processor_type': 'minecraft:rule', 'rules': rules}]})
