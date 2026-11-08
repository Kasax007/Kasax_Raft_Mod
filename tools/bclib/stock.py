"""Loot in a share of the empty barrels and crates of the structures (3.4).

The structures have many barrels and crates as decoration, and all of them were empty. Now a share of them gets a
small themed loot table (tables chests/barrel/* of the story module: supplies, food, seeds, fishing, scrap, ship
stores, city goods; the harder places get salvage with now and then a better find). Not all: the share and a cap per
template keep the value below the real chests of the place, and the choice is fixed (a hash of the template and the
position), so a regeneration gives the same barrels.

Structure.save() runs stock() for every template of the structure modules, so the generators keep this. Templates
already written can be patched without regenerating them: cd tools && python3 -m bclib.stock [--check]
"""
import glob
import os
import sys
import zlib

NS = 'raft'
STRUCTURE_MODULES = ('structures_side', 'structures_deep', 'structures_city', 'structures_surface')
CONTAINERS = {'minecraft:barrel': 'minecraft:barrel', 'raft:wooden_crate': 'raft:wooden_crate'}

# structure (first part of the template name) -> (share of the empty containers, cap per template, tables + weights)
B = 'chests/barrel/'
THEMES = {
    'abandoned_raft': (0.4, 3, [('supplies', 4), ('food', 3), ('fishing', 2), ('seeds', 1)]),
    'beach_shack': (0.5, 1, [('food', 3), ('seeds', 1)]),
    'buoy_field': (0.5, 1, [('supplies', 2), ('fishing', 2)]),
    'castaway_camp': (0.5, 2, [('food', 3), ('seeds', 2), ('supplies', 2)]),
    'iceberg_outpost': (0.5, 3, [('food', 3), ('supplies', 3), ('fishing', 1)]),
    'pirate_cove': (0.45, 4, [('ship_stores', 4), ('food', 2), ('supplies', 1)]),
    'rusted_rig': (0.5, 4, [('salvage', 3), ('scrap', 3), ('food', 1)]),
    'sunken_plane': (0.3, 4, [('supplies', 2), ('food', 2), ('scrap', 2)]),
    'sunken_ruins': (0.0, 0, []),
    'wreck_cargo_ship': (0.5, 3, [('scrap', 3), ('supplies', 3), ('food', 1)]),
    'wreck_fishing_boat': (0.35, 2, [('fishing', 4), ('food', 2), ('supplies', 1)]),
    'wreck_galleon': (0.2, 8, [('ship_stores', 5), ('food', 2), ('supplies', 2)]),
    'helix_station': (0.25, 8, [('salvage', 3), ('scrap', 2), ('food', 2), ('supplies', 1)]),
    'coral_temple': (0.0, 0, []),
    'abyssal_sanctum': (0.0, 0, []),
    'old_lighthouse': (0.3, 4, [('supplies', 3), ('food', 2), ('fishing', 2)]),
    'saltmere_village': (0.35, 3, [('fishing', 3), ('food', 3), ('seeds', 2), ('supplies', 1)]),
    'ashen_forge': (0.0, 0, []),
    'port_solace': (0.15, 4, [('city', 4), ('food', 2), ('salvage', 2), ('supplies', 1)]),
}


def _h(*parts):
    return zlib.crc32('|'.join(str(p) for p in parts).encode())


def choose(name, empties):
    """name: template name ('port_solace/dept_store'); empties: positions of the empty containers.
    Returns {pos: loot table id} for the ones that get loot."""
    theme = THEMES.get(name.split('/')[0])
    if not theme or not empties or theme[0] <= 0:
        return {}
    share, cap, tables = theme
    order = sorted(empties, key=lambda p: _h(name, *p))
    n = len(order)
    k = min(cap, round(share * n))
    if k == 0 and _h(name, 'single') % 1000 < share * 1000:
        k = 1                                   # one container: it gets loot with the share as chance
    total = sum(w for _, w in tables)
    out = {}
    for p in order[:k]:
        r = _h(name, 'table', *p) % total
        for t, w in tables:
            r -= w
            if r < 0:
                out[p] = f'{NS}:{B}{t}'
                break
    return out


def _empty(nbt):
    return not nbt or ('LootTable' not in nbt and not nbt.get('Items'))


def stock(s, name):
    """bclib Structure s (before save): fill a share of its empty barrels and crates."""
    (ox, oy, oz), _ = s.bounds()
    # positions as saved in the file (the box shifted to 0), so patch_file() picks the same containers
    empties = {(x - ox, y - oy, z - oz): (x, y, z) for (x, y, z), (bn, props, nbt) in s.blocks.items()
               if bn in CONTAINERS and _empty(nbt)}
    picked = choose(name, list(empties))
    for q, table in picked.items():
        p = empties[q]
        bn, props, nbt = s.blocks[p]
        d = dict(nbt or {})
        d['id'] = CONTAINERS[bn]
        d['LootTable'] = table
        s.blocks[p] = (bn, props, d)
    return len(picked), len(empties)


def patch_file(path, name, check=False):
    """Same as stock() on a template file already written (typed read and write, nothing else changes)."""
    from bclib import nbt as N
    root = N.read_typed(path)
    pal = [e.get('id', e.get('Name')) for e in root['palette']]
    by_pos = {}
    for b in root['blocks']:
        bn = pal[b['state'].v]
        if bn in CONTAINERS and _empty(b.get('nbt')):
            by_pos[tuple(x.v for x in b['pos'])] = b
    picked = choose(name, list(by_pos))
    for p, table in picked.items():
        b = by_pos[p]
        d = dict(b.get('nbt') or {})
        d['id'] = CONTAINERS[pal[b['state'].v]]
        d['LootTable'] = table
        b['nbt'] = d
    if picked and not check:
        N.write(path, root)
    return len(picked), len(by_pos)


def main(argv):
    from bclib.paths import MODULES
    check = '--check' in argv
    total = 0
    for m in STRUCTURE_MODULES:
        base = os.path.join(MODULES, m, 'data', NS, 'structure')
        for path in sorted(glob.glob(os.path.join(base, '**', '*.nbt'), recursive=True)):
            name = os.path.relpath(path, base)[:-4].replace(os.sep, '/')
            k, n = patch_file(path, name, check)
            total += k
            if n:
                print(f'{name:40s} {k:3d} of {n:3d} empty barrels/crates get loot')
    print(f'{total} containers get loot' + (' (check only, nothing written)' if check else ''))


if __name__ == '__main__':
    sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))
    main(sys.argv[1:])
