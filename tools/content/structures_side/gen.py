"""Run all structures_side generators:  cd tools && python3 -m content.structures_side.gen [names...] [--preview]
Each part module has main(preview=False) -> list of (template path, Structure) and writes its worldgen JSON."""
import os
import sys
import time

if __package__ in (None, ''):
    sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..')))
    __package__ = 'content.structures_side'

from importlib import import_module  # noqa: E402

from .lib import report  # noqa: E402

PARTS = ['wreck_galleon', 'wreck_cargo_ship', 'wreck_fishing_boat', 'pirate_cove', 'rusted_rig', 'sunken_plane',
         'sunken_ruins', 'castaway_camp', 'abandoned_raft', 'iceberg_outpost', 'beach_shack', 'buoy_field']


def main(argv):
    names = [a for a in argv if not a.startswith('--')] or PARTS
    bad_all = []
    for n in names:
        t = time.time()
        mod = import_module(f'.{n}', __package__)
        res = mod.main(preview='--preview' in argv)
        print(f'{n}: {len(res)} template(s) ({time.time() - t:.1f}s)')
        for path, s in res:
            bad, mag = report(s, path)
            bad_all += bad
    if bad_all:
        print('UNKNOWN IDS:', sorted(set(bad_all)))


if __name__ == '__main__':
    main(sys.argv[1:])
