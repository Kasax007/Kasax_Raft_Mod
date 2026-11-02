"""Run all structures_deep generators:  cd tools && python3 -m content.structures_deep.gen [names...] [--preview]"""
import os
import sys
import time

if __package__ in (None, ''):
    sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..')))
    __package__ = 'content.structures_deep'

from importlib import import_module  # noqa: E402

from .common import magenta_ids, previews, unknown_ids  # noqa: E402

PARTS = ['helix_station', 'coral_temple', 'abyssal_sanctum']
# ids of other modules that may not have textures yet (preview shows them magenta)
SPEC_IDS_PENDING = ()


def main(argv):
    names = [a for a in argv if not a.startswith('--')] or PARTS
    for n in names:
        t = time.time()
        mod = import_module(f'.{n}', __package__)
        res = mod.main()
        structs = res if isinstance(res, list) else [res]
        for s in structs:
            print(f'{s.name}: size {s.size()} blocks {len(s.blocks)} entities {len(s.entities)}'
                  f' ({time.time() - t:.1f}s)')
            unk = unknown_ids(s, SPEC_IDS_PENDING)
            print('  ids not in vanilla/spec:', ', '.join(unk) or 'none')
            mag = magenta_ids(s)
            if mag:
                print('  spec ids without texture yet:', ', '.join(mag))
            if hasattr(mod, 'verify'):
                for k, v in mod.verify(s).items():
                    print(f'  {k}: {v}')
            if '--preview' in argv:
                for p in getattr(mod, 'preview', lambda st: previews(st, st.name.replace('/', '_')))(s):
                    print('  preview', p)


if __name__ == '__main__':
    main(sys.argv[1:])
