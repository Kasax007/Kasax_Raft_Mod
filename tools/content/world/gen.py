"""Generator of the Beach Craft 3 module "world" (terrain, biomes, natural blocks, features).

Run from the repository root:  python3 tools/content/world/gen.py
or from tools/:                 python3 -m content.world.gen

WORLD_TEST=1 leaves out spawns of creatures and features that use blocks of other modules that are not registered
yet (for in-game tests while the other modules are still being built). The normal run writes everything.
"""
import os
import re
import shutil
import sys

TOOLS = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
if TOOLS not in sys.path:
    sys.path.insert(0, TOOLS)

from bclib import assets  # noqa: E402
from bclib.paths import MODULES, RESOURCES, GENERATED, ROOT  # noqa: E402
from content.world import terrain, surface, biomes  # noqa: E402

MODULE = 'world'
JAVA = os.path.join(ROOT, 'src', 'main', 'java')


def _java_text():
    out = []
    for dp, _, fs in os.walk(JAVA):
        for f in fs:
            if f.endswith('.java'):
                out.append(open(os.path.join(dp, f), encoding='utf-8', errors='replace').read())
    return '\n'.join(out)


def _exists_data(rel):
    for root in [RESOURCES, GENERATED] + [os.path.join(MODULES, m) for m in os.listdir(MODULES)]:
        if os.path.isfile(os.path.join(root, rel)):
            return True
    return False


def availability():
    """(feature_ok, entity_ok) predicates for WORLD_TEST mode."""
    java = _java_text()

    def registered(name):
        return re.search(r'"' + re.escape(name) + r'"', java) is not None

    def entity_ok(eid):
        ns, name = eid.split(':')
        return ns == 'minecraft' or name == 'shark' or re.search(r'entity\(\s*"' + re.escape(name) + '"', java)

    foreign_blocks = {'raft:concrete_rubble': ['cracked_concrete', 'rebar_concrete', 'mossy_concrete']}

    def feature_ok(fid):
        ns, name = fid.split(':')
        if ns == 'minecraft':
            return True
        if not _exists_data(f'data/raft/worldgen/placed_feature/{name}.json'):
            return False
        return all(registered(b) for b in foreign_blocks.get(fid, []))
    return feature_ok, entity_ok


def clean():
    d = os.path.join(MODULES, MODULE)
    for sub in ('assets', 'data'):
        p = os.path.join(d, sub)
        if os.path.isdir(p):
            shutil.rmtree(p)
        os.makedirs(p, exist_ok=True)


def main():
    test = os.environ.get('WORLD_TEST') == '1'
    clean()
    try:
        from content.world import blocks
    except ImportError:
        blocks = None
    try:
        from content.world import features
    except ImportError:
        features = None
    terrain.write(MODULE)
    if test:
        java = _java_text()
        stand_in = {'shell_sand': 'minecraft:sand', 'black_sand': 'minecraft:blackstone',
                    'volcanic_ash': 'minecraft:gray_concrete_powder', 'pumice': 'minecraft:calcite'}
        for k, v in stand_in.items():
            if '"' + k + '"' not in java:
                surface.B[k] = v
    surface.write(MODULE)
    if blocks is not None:
        blocks.write(MODULE)
    if features is not None:
        features.write(MODULE)
    feature_ok, entity_ok = availability() if test else (None, None)
    biomes.write(MODULE, feature_ok, entity_ok)
    assets.flush(MODULE)
    print('world: generated' + (' (test mode)' if test else ''))


if __name__ == '__main__':
    main()
