"""Entry point: builds all surface story structures (templates + worldgen JSON).

    cd tools && python3 -m content.structures_surface.gen [--no-preview]
"""
import os
import sys

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..')))

from content.structures_surface import worldgen as wg  # noqa: E402
from content.structures_surface import lib  # noqa: E402

SALTS = {'old_lighthouse': 31415921, 'saltmere_village': 31415922, 'ashen_forge': 31415923}


def lighthouse(preview):
    from content.structures_surface import old_lighthouse as lh
    res = lh.build_all(preview)
    wg.processor_list('old_lighthouse_weathering', [
        wg.rule('minecraft:cobblestone', 0.18, 'minecraft:mossy_cobblestone'),
        wg.rule('minecraft:stone_bricks', 0.10, 'minecraft:cracked_stone_bricks'),
        wg.rule('minecraft:stone_bricks', 0.08, 'minecraft:mossy_stone_bricks'),
        wg.rule('minecraft:white_concrete', 0.04, 'minecraft:calcite'),
    ])
    wg.pool('old_lighthouse/start', [
        ('raft:old_lighthouse/compound_a', 1, 'raft:old_lighthouse_weathering', 'rigid'),
        ('raft:old_lighthouse/compound_b', 1, 'raft:old_lighthouse_weathering', 'rigid'),
    ])
    wg.pool('old_lighthouse/jetty', [('raft:old_lighthouse/jetty', 1, None, 'terrain_matching')])
    wg.structure('old_lighthouse', 'raft:old_lighthouse/start', 'surface_structures', 'beard_thin', 0,
                 'WORLD_SURFACE_WG', size=1, max_dist=80)
    wg.structure_set('old_lighthouse', 22, 8, SALTS['old_lighthouse'])
    wg.biome_tag('old_lighthouse', ['palm_isle', 'jungle_isle'])
    return res


def saltmere(preview):
    from content.structures_surface import saltmere_village as sv
    return sv.build_all(preview)


def ashen(preview):
    from content.structures_surface import ashen_forge as af
    return af.build_all(preview)


def main():
    preview = '--no-preview' not in sys.argv
    only = [a for a in sys.argv[1:] if not a.startswith('--')]
    out = {}
    for name, fn in (('old_lighthouse', lighthouse), ('saltmere_village', saltmere), ('ashen_forge', ashen)):
        if only and name not in only:
            continue
        out.update(fn(preview) or {})
    for k, (p, size) in sorted(out.items()):
        print(f'{k:40s} {size}')


if __name__ == '__main__':
    main()
