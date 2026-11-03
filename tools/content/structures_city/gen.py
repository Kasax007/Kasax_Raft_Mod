"""Generate everything of the structures_city module (Port Solace, the drowned city).

    cd tools && python3 -m content.structures_city.gen [--previews]

Writes the templates to src/main/modules/structures_city/data/raft/structure/port_solace/*.nbt and the worldgen JSON
(structure, structure set, template pools, processor list, biome tag). Checks: block ids (vanilla 26.3 or spec),
template size <= 48 per side, the City Hall vault is watertight (leak check in city_hall.base()).
"""
import os
import shutil
import sys

TOOLS = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
if TOOLS not in sys.path:
    sys.path.insert(0, TOOLS)

from content.structures_city import blocks, city_hall, citylib, plaza, streets, towers, worldgen  # noqa: E402
from content.structures_city.common import MODULE, WORK, unknown_ids  # noqa: E402
from bclib.paths import MODULES  # noqa: E402

# ids the spec names only in section 14 (extra family blocks) or that the spec-token parser misses
KNOWN_EXTRA = {'raft:cracked_concrete_slab', 'raft:cracked_concrete_stairs', 'raft:cracked_concrete_wall'}


def build_all():
    out = {'plaza': plaza.build()}
    out.update(streets.build_all())
    out.update(city_hall.build_all())
    out.update(towers.build_all())
    out.update(blocks.build_all())
    return out


def main(previews=False):
    root = os.path.join(MODULES, MODULE, 'data', 'raft')
    for sub in ('structure', 'worldgen'):
        p = os.path.join(root, sub)
        if os.path.isdir(p):
            shutil.rmtree(p)
    tags = os.path.join(root, 'tags')
    if os.path.isdir(tags):
        shutil.rmtree(tags)
    out = build_all()
    problems = []
    for name, s in out.items():
        size = s.size()
        bad = [b for b in unknown_ids(s) if b not in KNOWN_EXTRA]
        if bad:
            problems.append(f'{name}: unknown ids {bad}')
        if max(size) > 48:
            problems.append(f'{name}: size {size} > 48')
        (mx, my, mz), _ = s.bounds()
        for (x, y, z), (bn, props, nbt) in s.blocks.items():
            if bn == 'minecraft:jigsaw':
                o = props['orientation']
                want = 0 if o.startswith('down') else (size[1] - 1 if o.startswith('up') else 2)
                if y - my != want:
                    problems.append(f'{name}: jigsaw {o} at y {y - my}, expected {want}')
        s.save(MODULE, f'port_solace/{name}')
        print(f'{name:18s} {size[0]:3d} x {size[1]:3d} x {size[2]:3d}  blocks {len(s.blocks)}')
        if previews:
            citylib.save_previews(s, name, ne=True)
    for f in worldgen.write_all():
        pass
    print('worldgen JSON written')
    if problems:
        print('PROBLEMS:')
        for p in problems:
            print('  ' + p)
    return out


if __name__ == '__main__':
    main(previews='--previews' in sys.argv)
