"""bctest scripts for structures_side (see tools/bctest/README.md).

    cd tools && python3 -m content.structures_side.ingame          -> $WORK/test_*.txt
    tools/bctest/run.sh $WORK/test_tanks.txt $WORK/shots_tanks

test_tanks.txt   – templates in barrier tanks high in the sky (independent of the terrain): underwater builds on a
                   sand floor in water, floating builds in a shallow pool (water top = world sea level layout),
                   land builds on a sand floor in air; screenshots from outside and inside.
test_natural.txt – /locate of every structure and /place structure (full worldgen pipeline) at the located spots.
(The tank helpers follow the structures_deep test scripts.)
"""
import math
import os

from .lib import WORK
from bclib.nbt import load_structure
from bclib.paths import out as _out

DIM = 'execute in raft:raftdim run '
HEAD = ['world bctest_side', 'gui 2', 'rd 7',
        'cmd effect give @p minecraft:night_vision infinite 0 true',
        'cmd effect give @p minecraft:water_breathing infinite 0 true',
        'cmd effect give @p minecraft:conduit_power infinite 0 true',
        'cmd gamerule spawn_mobs false', 'cmd time set noon', 'cmd gamemode spectator @p']


def tp(x, y, z, yaw=0, pitch=0):
    return f'cmd {DIM}tp @p {x} {y} {z} {yaw} {pitch}'


def layered_fill(x1, y1, z1, x2, y2, z2, block, extra=''):
    area = (x2 - x1 + 1) * (z2 - z1 + 1)
    step = max(1, 32000 // area)
    o, y = [], y1
    while y <= y2:
        top = min(y2, y + step - 1)
        o.append(f'cmd {DIM}fill {x1} {y} {z1} {x2} {top} {z2} {block}{extra}')
        y = top + 1
    return o


def look(cam, target):
    dx, dy, dz = target[0] - cam[0], target[1] - (cam[1] + 1.6), target[2] - cam[2]
    yaw = math.degrees(math.atan2(-dx, dz))
    pitch = -math.degrees(math.atan2(dy, math.hypot(dx, dz)))
    return round(yaw), round(pitch)


def tmpl(path):
    return load_structure(_out('structures_side', 'data', 'raft', 'structure', *(path + '.nbt').split('/')))


SINK = {'wreck_galleon': -3, 'wreck_cargo_ship': -3, 'wreck_fishing_boat': -3, 'sunken_plane': -3,
        'sunken_ruins': -2, 'castaway_camp': -1, 'beach_shack': -1}
BOX = {}


def tank(items, X, Y, Z, water_top=None, floor='minecraft:sand', margin=8, gap=6):
    """items: template paths placed in a row along +x; Y is the top of the floor + 1 (= OCEAN_FLOOR_WG / WORLD_SURFACE_WG
    height), each template is sunk like its start_height does in worldgen. water_top: absolute y of the water surface
    (None = dry). Returns (cmds, placed) with placed[path] = (X0, Y0, Z0, size)."""
    placed, x = {}, X
    depth = 0
    for p in items:
        sx, sy, sz = tmpl(p).size()
        placed[p] = (x, Y + SINK.get(p.split('/')[0], 0), Z, (sx, sy, sz))
        x += sx + gap
        depth = max(depth, sz)
    x1, z1, x2, z2 = X - margin, Z - margin, x - gap + margin, Z + depth + margin
    top = (water_top if water_top is not None else Y) + 2
    for p in items:
        BOX[p] = (x1, z1, x2, z2, Y, water_top)
    L = [tp((x1 + x2) // 2, Y + 40, (z1 + z2) // 2, 0, 90), 'settle 20']
    L += layered_fill(x1 - 1, Y - 6, z1 - 1, x2 + 1, Y - 6, z2 + 1, 'minecraft:barrier')
    L += layered_fill(x1, Y - 5, z1, x2, Y - 1, z2, floor)
    if water_top is not None:
        for (a, b) in (((x1 - 1, z1 - 1), (x2 + 1, z1 - 1)), ((x1 - 1, z2 + 1), (x2 + 1, z2 + 1)),
                       ((x1 - 1, z1), (x1 - 1, z2)), ((x2 + 1, z1), (x2 + 1, z2))):
            L += layered_fill(a[0], Y - 5, a[1], b[0], top, b[1], 'minecraft:barrier')
        L += layered_fill(x1, Y, z1, x2, water_top, z2, 'minecraft:water')
    for p, (px, py, pz, size) in placed.items():
        L += [f'cmd {DIM}place template raft:{p} {px} {py} {pz}', 'wait 5']
    L += ['wait 60']
    return L, placed


def views(placed, p, n=2, inside=None, dist=0.65, up=3):
    """Screenshot views of one placed template (cameras kept inside its tank): south-west, north-east, south."""
    px, py, pz, (sx, sy, sz) = placed[p]
    x1, z1, x2, z2, Y, wt = BOX[p]
    c = (px + sx / 2, py + sy / 3, pz + sz / 2)
    r = max(sx, sz) * dist + 5
    name = p.replace('/', '_')
    L = []
    cams = [(-0.7, 0.7), (0.75, -0.65), (0.0, 1.0)][:n]
    for i, (fx, fz) in enumerate(cams):
        cam = [round(c[0] + fx * r), round(py + sy * 0.55 + up), round(c[2] + fz * r)]
        cam[0] = min(max(cam[0], x1 + 1), x2 - 1)
        cam[2] = min(max(cam[2], z1 + 1), z2 - 1)
        if wt is not None:
            cam[1] = min(cam[1], wt - 2) if py + sy < wt else cam[1]
        yaw, pitch = look(cam, c)
        L += [tp(*cam, yaw, pitch), 'wait 30', f'shot {name}_{i}']
    if inside:
        cam, tgt = inside
        cam = (px + cam[0], py + cam[1], pz + cam[2])
        tgt = (px + tgt[0], py + tgt[1], pz + tgt[2])
        yaw, pitch = look(cam, tgt)
        L += [tp(*cam, yaw, pitch), 'wait 30', f'shot {name}_in']
    return L


def script_tanks():
    """Run A: big underwater wrecks (own wide tank) and the land builds."""
    L = list(HEAD) + ['hud off']
    deep = ['wreck_galleon/galleon_0', 'wreck_galleon/galleon_1', 'wreck_cargo_ship/freighter_0',
            'sunken_plane/airliner_0', 'sunken_plane/seaplane_1']
    cmds, pl = tank(deep, 1000, 120, 1000, water_top=162, margin=16, gap=24)
    L += cmds
    for p in deep:
        L += views(pl, p, n=2)
    L += views(pl, 'wreck_galleon/galleon_0', n=0, inside=((14, 12, 8), (14, 10, 1)))
    land = ['castaway_camp/lean_to', 'castaway_camp/tent', 'castaway_camp/boat_shelter', 'beach_shack/tiki_bar',
            'beach_shack/surf_shack']
    cmds, pl5 = tank(land, 1000, 150, 1400, water_top=None, margin=12, gap=10)
    L += cmds
    for p in land:
        L += views(pl5, p, n=1, dist=0.5, up=9)
    L += views(pl5, 'beach_shack/tiki_bar', n=0, inside=((6, 4, 3), (6, 3, 8)))
    L += ['cmd time set 18000', 'wait 20']
    L += views(pl5, 'beach_shack/tiki_bar', n=1, dist=0.5, up=6)
    L += views(pl5, 'castaway_camp/tent', n=1, dist=0.5, up=6)
    return L


def script_big(with_rig=True):
    """Run B: floating builds, rig, cove, iceberg (sea level layout: rig template y 79 = water top, cove y 11)."""
    L = list(HEAD) + ['hud off']
    float_items = ['abandoned_raft/survivor', 'abandoned_raft/homestead', 'abandoned_raft/wrecked']
    cmds, pf = tank(float_items, 1000, 141, 1800, water_top=142, margin=10, gap=8)
    L += cmds
    for p in float_items:
        L += views(pf, p, n=1, dist=0.9)
    buoys = ['buoy_field/channel', 'buoy_field/beacon']
    cmds, pb = tank(buoys, 1100, 138, 1800, water_top=142, margin=10, gap=8)
    L += cmds
    for p in buoys:
        L += views(pb, p, n=1, dist=0.9)
    if with_rig:
        cmds, pl = tank(['rusted_rig/rig_0'], 2000, 60, 2000, water_top=60 + 79, margin=10)
        L += cmds
        L += views(pl, 'rusted_rig/rig_0', n=3)
        px, py, pz, (sx, sy, sz) = pl['rusted_rig/rig_0']
        for name, cam, tgt in (('rig_deck', (8, 92, 30), (24, 92, 22)), ('rig_quarters', (12, 91, 10), (16, 91, 6)),
                               ('rig_helipad', (34, 106, 14), (18, 100, 8))):
            c = (px + cam[0], py + cam[1], pz + cam[2])
            t = (px + tgt[0], py + tgt[1], pz + tgt[2])
            L += [tp(*c, *look(c, t)), 'wait 30', f'shot {name}']
    cmds, pl2 = tank(['pirate_cove/cove_0'], 2200, 160, 2000, water_top=160 + 11, margin=12)
    L += cmds
    L += views(pl2, 'pirate_cove/cove_0', n=3)
    px, py, pz, (sx, sy, sz) = pl2['pirate_cove/cove_0']
    # template x/z of the cove are shifted by its bounds (x0 = z0 = -27): centre of the cavern = (27, 16, 27)
    cx, cz = sx // 2, sz // 2
    for name, cam, tgt in (('cove_entrance', (cx, 14, sz + 6), (cx, 14, cz)),
                           ('cove_inside_dock', (cx - 2, 15, cz + 8), (cx, 13, cz - 8)),
                           ('cove_inside_back', (cx, 20, cz - 10), (cx, 12, cz + 8)),
                           ('cove_treasure', (cx + 13, 14, cz - 1), (cx + 19, 13, cz - 1)),
                           ('cove_captain', (cx - 15, 14, cz), (cx - 19, 13, cz - 2))):
        c = (px + cam[0], py + cam[1], pz + cam[2])
        t = (px + tgt[0], py + tgt[1], pz + tgt[2])
        L += [tp(*c, *look(c, t)), 'wait 30', f'shot {name}']
    cmds, pl3 = tank(['iceberg_outpost/outpost_0'], 2400, 160, 2000, water_top=160 + 10, margin=8)
    L += cmds
    L += views(pl3, 'iceberg_outpost/outpost_0', n=2)
    return L


IDS = ['wreck_galleon', 'wreck_cargo_ship', 'wreck_fishing_boat', 'pirate_cove', 'rusted_rig', 'sunken_plane',
       'sunken_ruins', 'castaway_camp', 'abandoned_raft', 'iceberg_outpost', 'beach_shack', 'buoy_field']


def script_land():
    """Run C: land builds close up, by day and by night."""
    L = list(HEAD) + ['hud off']
    land = ['castaway_camp/lean_to', 'castaway_camp/tent', 'castaway_camp/boat_shelter', 'beach_shack/tiki_bar',
            'beach_shack/surf_shack']
    cmds, pl5 = tank(land, 1000, 150, 1400, water_top=None, margin=12, gap=10)
    L += cmds
    for p in land:
        L += views(pl5, p, n=2, dist=0.5, up=9)
    L += ['cmd time set 18000', 'wait 20']
    for p in ('beach_shack/tiki_bar', 'castaway_camp/tent', 'beach_shack/surf_shack'):
        L += views(pl5, p, n=1, dist=0.5, up=6)
    return L


def script_locate():
    L = list(HEAD) + [tp(0, 200, 0, 0, 30), 'settle 10']
    for b in ('wreck_graveyard', 'raft_ocean_biome', 'kelp_forest', 'coral_shallows', 'palm_isle', 'jungle_isle',
              'frost_floes'):
        L += [f'cmd {DIM}locate biome raft:{b}', 'wait 10']
    for i in IDS:
        L += [f'log LOCATE {i}', f'cmd {DIM}locate structure raft:{i}', 'wait 10']
    return L


SURFACE = {'castaway_camp', 'beach_shack', 'pirate_cove', 'abandoned_raft', 'buoy_field', 'iceberg_outpost',
           'rusted_rig'}


def script_natural(found):
    """found: {structure id: (x, z)} from /locate. Teleports to each naturally generated structure and takes an
    overview shot from above plus, for the underwater ones, a shot near the sea floor (night vision)."""
    L = list(HEAD) + ['hud off', 'rd 6']
    for sid, (x, z) in found.items():
        L += [f'log NATURAL {sid} {x} {z}']
        if sid == 'rusted_rig':
            L += [tp(x + 40, 135, z + 40, 135, 20), 'settle 30', f'shot nat_{sid}_far', tp(x + 12, 112, z + 18, 160, 10),
                  'wait 40', f'shot nat_{sid}_deck']
        elif sid in SURFACE:
            L += [tp(x - 18, 118, z - 18, -45, 35), 'settle 30', f'shot nat_{sid}']
        else:
            L += [tp(x - 14, 110, z - 14, -45, 50), 'settle 30', f'shot nat_{sid}_top',
                  f'cmd {DIM}execute positioned {x} 0 {z} positioned over ocean_floor run tp @p ~-12 ~6 ~-12 -45 25',
                  'wait 60', f'shot nat_{sid}_floor']
    return L


FOUND = {'wreck_fishing_boat': (-96, -96), 'sunken_ruins': (-224, -96), 'wreck_galleon': (-400, -176),
         'sunken_plane': (-416, -320), 'abandoned_raft': (-320, -384), 'buoy_field': (-96, -384),
         'castaway_camp': (80, -384), 'beach_shack': (-720, -176), 'wreck_cargo_ship': (-624, -736),
         'rusted_rig': (-384, -944), 'pirate_cove': (-1472, 336)}


def script_c():
    """Run C: land builds, floating builds, cove and iceberg again (spectator cameras), then natural generation."""
    L = script_land()
    big = script_big(with_rig=False)
    L += big[len(HEAD) + 1:]
    nat = script_natural(FOUND)
    L += nat[len(HEAD) + 2:]
    return L


def main():
    os.makedirs(WORK, exist_ok=True)
    for name, fn in (('tanks', script_tanks), ('big', script_big), ('land', script_land), ('locate', script_locate),
                     ('c', script_c)):
        BOX.clear()
        p = os.path.join(WORK, f'test_{name}.txt')
        with open(p, 'w') as f:
            f.write('\n'.join(fn()) + '\n')
        print(p)


if __name__ == '__main__':
    main()
