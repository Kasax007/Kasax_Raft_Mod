"""bctest script for Port Solace (see tools/bctest/README.md).

    cd tools && python3 -m content.structures_city.ingame   ->  $WORK/test_city.txt
    tools/bctest/run.sh $WORK/test_city.txt $WORK/shots

/place structure raft:port_solace runs the full worldgen pipeline (jigsaw, projection to the ocean floor, random
rotation, processors, liquid settings). The rotation of the plaza and the floor height are unknown, so the script
probes the City Hall vault's sigil pedestal for all four rotations and a range of heights and summons a marker on
the one it finds (tag r0..r3). Every camera position is then a teleport relative to that marker.
"""
import os
import sys

TOOLS = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
if TOOLS not in sys.path:
    sys.path.insert(0, TOOLS)

from content.structures_city import city_hall, plaza  # noqa: E402
from content.structures_city.common import WORK  # noqa: E402

DIM = 'execute in raft:raftdim run '
OX, OZ = 64, 64
R = {0: lambda x, z: (x, z), 1: lambda x, z: (-z, x), 2: lambda x, z: (-x, -z), 3: lambda x, z: (z, -x)}
VEC = {'north': (0, -1), 'south': (0, 1), 'east': (1, 0), 'west': (-1, 0)}


def yaw_of(dx, dz):
    import math
    return round(math.degrees(math.atan2(-dx, dz)), 1)


def norm(s, pos):
    (mx, my, mz), _ = s.bounds()
    return (pos[0] - mx, pos[1] - my, pos[2] - mz)


def find(s, pred):
    for pos, (n, p, nbt) in s.blocks.items():
        if pred(n, p, nbt):
            return pos
    raise KeyError


def script():
    pz = plaza.build()
    hb = city_hall.base()
    jp = norm(pz, find(pz, lambda n, p, nbt: n == 'minecraft:jigsaw' and nbt['name'].endswith('hall_anchor')))
    jh = norm(hb, find(hb, lambda n, p, nbt: n == 'minecraft:jigsaw' and nbt['name'].endswith('hall_front')))
    ped = norm(hb, find(hb, lambda n, p, nbt: n == 'raft:sigil_pedestal'))
    hmin, _ = hb.bounds()
    pmin, _ = pz.bounds()

    def frames(r):
        rh = (r + 2) % 4
        jpw = (OX + R[r](jp[0], jp[2])[0], OZ + R[r](jp[0], jp[2])[1])
        d = R[r](*VEC['north'])
        jhw = (jpw[0] + d[0], jpw[1] + d[1])
        rj = R[rh](jh[0], jh[2])
        ho = (jhw[0] - rj[0], jhw[1] - rj[1])

        def hall(x, y, z, raw=True):
            if raw:
                x, y, z = x - hmin[0], y - hmin[1], z - hmin[2]
            q = R[rh](x, z)
            return (ho[0] + q[0], y, ho[1] + q[1])

        def plz(x, y, z):
            q = R[r](x - pmin[0], z - pmin[2])
            return (OX + q[0], y - pmin[1], OZ + q[1])

        return rh, hall, plz

    lines = ['world portsolace', 'gui 2', 'rd 10',
             'cmd gamerule spawn_mobs false',
             'cmd effect give @p minecraft:night_vision infinite 0 true',
             'cmd effect give @p minecraft:water_breathing infinite 0 true',
             'cmd effect give @p minecraft:conduit_power infinite 0 true',
             f'cmd {DIM}tp @p {OX} 150 {OZ} 0 60', 'settle 60',
             'log LOCATE',
             f'cmd {DIM}locate structure raft:port_solace',
             f'cmd {DIM}locate biome raft:drowned_shelf',
             'log PLACE',
             f'cmd {DIM}place structure raft:port_solace {OX} 60 {OZ}',
             'wait 60', 'settle 60']
    # probe the pedestal
    for r in range(4):
        rh, hall, plz = frames(r)
        px, _, pz_ = hall(*ped, raw=False)
        for y in range(70, 118):
            lines.append(f'cmd execute in raft:raftdim if block {px} {y} {pz_} raft:sigil_pedestal run summon '
                         f'minecraft:marker {px} {y} {pz_} {{Tags:["ps","r{r}"]}}')
    lines.append('cmd execute in raft:raftdim as @e[type=marker,tag=ps] run say FOUND_PEDESTAL')
    lines.append('hud off')

    def view(name, pos_fn, look, pitch, absy=None, wait=40):
        """pos_fn(r) -> world (x, y, z) in the frame where the pedestal is (px, ped_y, pz)."""
        for r in range(4):
            rh, hall, plz = frames(r)
            pp = hall(*ped, raw=False)
            p = pos_fn(hall, plz)
            dx, dy, dz = p[0] - pp[0], p[1] - pp[1], p[2] - pp[2]
            lv = look(R[rh] if look.__name__ == 'h' else R[r])
            yaw = yaw_of(*lv)
            ys = f'{absy}' if absy is not None else f'~{dy}'
            lines.append(f'cmd execute in raft:raftdim as @e[type=marker,tag=r{r}] at @s run tp @p ~{dx} {ys} ~{dz} '
                         f'{yaw} {pitch}')
        lines.extend([f'settle 20', f'wait {wait}', f'shot {name}'])

    def h(rot):  # direction given in hall template coords
        return rot(*h.v)

    def make_h(v):
        def h(rot):
            return rot(*v)
        return h

    def make_p(v):
        def p(rot):
            return rot(*v)
        return p

    # plaza / city overview (plaza frame: hall lies to the north = -z)
    view('surface_close', lambda hall, plz: plz(23, 0, 95), make_p((0, -1)), -2, absy=104)
    view('surface_far', lambda hall, plz: plz(23, 0, 175), make_p((0, -1)), 4, absy=122)
    view('surface_side', lambda hall, plz: plz(150, 0, 23), make_p((-1, 0)), 2, absy=110)
    view('plaza_statue', lambda hall, plz: plz(23, 12, 40), make_p((0, -1)), 8)
    view('plaza_low', lambda hall, plz: plz(5, 6, 41), make_p((1, -1)), 0)
    view('avenue', lambda hall, plz: plz(23, 7, 52), make_p((0, 1)), 5)
    # city hall
    view('hall_front', lambda hall, plz: hall(17, 12, -16), make_h((0, 1)), -5)
    view('hall_lobby', lambda hall, plz: hall(17, 5, 5), make_h((0, 1)), 5)
    view('hall_mayor', lambda hall, plz: hall(23, 40, 16), make_h((-1, -1)), 15)
    view('hall_landing', lambda hall, plz: hall(10, 44, 15), make_h((1, 0)), 5)
    view('vault', lambda hall, plz: hall(19, 44, 15), make_h((1, 0)), 10)
    view('vault_back', lambda hall, plz: hall(24, 45, 18), make_h((-1, -1)), 20)
    # dryness of the vault: replaces water by structure_void around the pedestal ("No blocks were filled" = dry)
    lines.append('log COUNT_WATER vault')
    lines.append('cmd execute in raft:raftdim as @e[type=marker,tag=ps] at @s run fill ~-4 ~ ~-4 ~3 ~2 ~4 '
                 'minecraft:structure_void replace minecraft:water')
    lines.append('log COUNT_WATER airlock')
    for r in range(4):
        rh, hall, plz = frames(r)
        pp = hall(*ped, raw=False)
        a = hall(15, 44, 14)
        b = hall(15, 46, 16)
        lines.append(f'cmd execute in raft:raftdim as @e[type=marker,tag=r{r}] at @s run fill ~{a[0] - pp[0]} ~ '
                     f'~{a[2] - pp[2]} ~{b[0] - pp[0]} ~2 ~{b[2] - pp[2]} minecraft:structure_void replace '
                     f'minecraft:water')
    lines.append('wait 40')
    os.makedirs(WORK, exist_ok=True)
    p = os.path.join(WORK, 'test_city.txt')
    with open(p, 'w') as f:
        f.write('\n'.join(lines) + '\n')
    return p


if __name__ == '__main__':
    print(script())
