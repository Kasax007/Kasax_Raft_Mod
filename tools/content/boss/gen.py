"""Generates every resource of the boss module into src/main/modules/boss/.

Run: cd tools && python3 -m content.boss.gen   (or python3 tools/content/boss/gen.py)
Entity textures are painted onto the boxes read from the Java models, so change the geometry in Java and re-run.
"""
import os
import sys

if __package__ in (None, ''):
    sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..')))
    __package__ = 'content.boss'

from bclib import assets  # noqa: E402

from content.boss import creatures, data, items  # noqa: E402

M = 'boss'


def main():
    creatures.generate()
    items.generate()
    data.generate()
    assets.flush(M)
    print('boss: done')


if __name__ == '__main__':
    main()
