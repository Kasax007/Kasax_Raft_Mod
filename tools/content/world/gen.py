"""Generator of the Beach Craft 3 module "world" (terrain, biomes, natural blocks, features).

Run from the repository root:  python3 tools/content/world/gen.py
or from tools/:                 python3 -m content.world.gen

Everything written here only names vanilla ids, 2.0 Beach Craft ids and ids of this module, so the data always
loads. Creatures of other modules spawn through content/world/WorldSpawns.java, blocks of other modules appear only
as optional rubble blocks (both looked up by id at runtime).
"""
import os
import shutil
import sys

TOOLS = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
if TOOLS not in sys.path:
    sys.path.insert(0, TOOLS)

from bclib import assets  # noqa: E402
from bclib.paths import MODULES  # noqa: E402
from content.world import terrain, surface, biomes, blocks, features  # noqa: E402

MODULE = 'world'


def clean():
    d = os.path.join(MODULES, MODULE)
    for sub in ('assets', 'data'):
        p = os.path.join(d, sub)
        if os.path.isdir(p):
            shutil.rmtree(p)
        os.makedirs(p, exist_ok=True)


def main():
    clean()
    terrain.write(MODULE)
    surface.write(MODULE)
    blocks.write(MODULE)
    features.write(MODULE)
    biomes.write(MODULE)
    assets.flush(MODULE)
    print('world: generated')


if __name__ == '__main__':
    main()
