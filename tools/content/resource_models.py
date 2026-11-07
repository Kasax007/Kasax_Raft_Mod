"""Block models of src/main/resources that are built from cut-out sprites (ship's wheel, shark jaw trophy).

Run:  cd tools && python3 -m content.resource_models
The sprites are extruded pixel row by pixel row (bclib.extrude), so their sides follow the outline.
"""
import json
import os
import sys

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from bclib import extrude  # noqa: E402
from bclib.paths import RESOURCES  # noqa: E402


def write(name, textures, elements):
    p = os.path.join(RESOURCES, 'assets', 'raft', 'models', 'block', name + '.json')
    with open(p, 'w', encoding='utf-8') as f:
        json.dump({'parent': 'minecraft:block/block', 'textures': textures, 'elements': elements}, f, indent=2)
        f.write('\n')


def box(frm, to, uv_by_face, var):
    return {'from': frm, 'to': to, 'faces': {d: {'uv': uv, 'texture': var} for d, uv in uv_by_face.items()}}


def main():
    hub = [7, 7, 9, 9]
    wheel = extrude.vertical('raft:block/ship_wheel', '#wheel', z=(13, 14))
    wheel.append(box([6, 6, 11], [10, 10, 13], {'north': [6, 6, 10, 10], 'up': hub, 'down': hub, 'east': hub,
                                                'west': hub}, '#wheel'))
    axle = box([7, 7, 14], [9, 9, 16], {'up': hub, 'down': hub, 'east': hub, 'west': hub, 'north': hub, 'south': hub},
               '#wheel')
    axle['faces']['south']['cullface'] = 'south'
    wheel.append(axle)
    write('ship_wheel', {'particle': 'raft:block/ship_wheel', 'wheel': 'raft:block/ship_wheel'}, wheel)

    jaw = extrude.vertical('raft:block/shark_jaw_trophy', '#trophy', z=(14, 16), cull={'south': 'south'})
    write('shark_jaw_trophy', {'particle': 'raft:block/shark_jaw_trophy', 'trophy': 'raft:block/shark_jaw_trophy'},
          jaw)


if __name__ == '__main__':
    main()
