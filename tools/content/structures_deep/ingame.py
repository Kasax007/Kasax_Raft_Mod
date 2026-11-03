"""Writes bctest scripts for the structures_deep templates (water box + /place template at a known origin + contract
checks logged with /say), see tools/bctest/README.md.

    cd tools && python3 -m content.structures_deep.ingame       ->  $WORK/test_<name>.txt
    tools/bctest/run.sh $WORK/test_sanctum.txt $WORK/shots_sanctum
"""
import os

from .common import MODULE, WORK

# stand-ins for spec blocks that other modules have not registered yet (test copies only, deleted after the run)
MISSING_PROXY = {'raft:sanctum_tidestone': 'minecraft:dark_prismarine', 'raft:sanctum_glass': 'minecraft:cyan_stained_glass',
                 'raft:tide_seal': 'minecraft:light_blue_stained_glass', 'raft:leviathan_altar': 'minecraft:gold_block',
                 'raft:tidewarden_altar': 'minecraft:gold_block'}


def registered(block_id):
    """True when some module (or the base mod) ships a blockstate for this raft id."""
    from bclib.paths import MODULES, RESOURCES
    ns, p = block_id.split(':', 1)
    if ns != 'raft':
        return True
    cands = [os.path.join(MODULES, m, 'assets', 'raft', 'blockstates', p + '.json') for m in os.listdir(MODULES)]
    cands.append(os.path.join(RESOURCES, 'assets', 'raft', 'blockstates', p + '.json'))
    return any(os.path.isfile(c) for c in cands)


def test_copy(s, path):
    """Save a copy of s with unregistered story blocks replaced (data/raft/structure/<path>.nbt). Returns the id
    map actually used."""
    from bclib.nbt import Structure
    t = Structure(path)
    used = {}
    for pos, (name, props, nbt) in s.blocks.items():
        if name in MISSING_PROXY and not registered(name):
            used[name] = MISSING_PROXY[name]
            t.blocks[pos] = (MISSING_PROXY[name], {}, None)
        else:
            t.blocks[pos] = (name, props, nbt)
    if not used:
        return used
    t.entities = list(s.entities)
    t.save(MODULE, path)
    return used

DIM = 'execute in raft:raftdim run '


def water_box(x1, y1, z1, x2, y2, z2, floor='minecraft:sand'):
    """fill commands (each <= 32768 blocks) making a water volume with a floor below it."""
    cmds = [f'cmd {DIM}fill {x1} {y1 - 1} {z1} {x2} {y1 - 1} {z2} {floor}']
    area = (x2 - x1 + 1) * (z2 - z1 + 1)
    step = max(1, 32000 // area)
    y = y1
    while y <= y2:
        top = min(y2, y + step - 1)
        cmds.append(f'cmd {DIM}fill {x1} {y} {z1} {x2} {top} {z2} minecraft:water')
        y = top + 1
    # air above so the box has a surface (the sea surface of the test)
    return cmds


def tp(x, y, z, yaw=0, pitch=0):
    return f'cmd {DIM}tp @p {x} {y} {z} {yaw} {pitch}'


def check(block_test, x, y, z, label):
    return f'cmd {DIM}execute if block {x} {y} {z} {block_test} run say CHECK_OK {label}'


def check_not(block_test, x, y, z, label):
    return f'cmd {DIM}execute unless block {x} {y} {z} {block_test} run say CHECK_OK {label}'


def count_water(x1, y1, z1, x2, y2, z2, label):
    """Replaces water in the box by structure_void and reports the count (0 = 'No blocks were filled')."""
    return [f'log COUNT_WATER {label}',
            f'cmd {DIM}fill {x1} {y1} {z1} {x2} {y2} {z2} minecraft:structure_void replace minecraft:water']


HEAD = ['world bctest_deep', 'gui 2', 'rd 10',
        'cmd effect give @p minecraft:night_vision infinite 0 true',
        'cmd effect give @p minecraft:conduit_power infinite 0 true',
        'cmd effect give @p minecraft:water_breathing infinite 0 true',
        'cmd gamerule spawn_mobs false']


def sanctum_script():
    from . import abyssal_sanctum as A
    s = A.build()
    (bx, by, bz), _ = s.bounds()
    X, Y, Z = 3000, 40, 3000                 # world position of the template's min corner
    W = lambda x, y, z: (X + x - bx, Y + y - by, Z + z - bz)  # noqa: E731
    L = list(HEAD)
    cx, cy, cz = W(0, 20, 10)
    L += [tp(cx, 90, cz, 0, 90), 'settle 30']
    (x1, y1, z1), (x2, y2, z2) = W(-33, 0, -43), W(33, 40, 62)
    L += water_box(x1, y1, z1, x2, y2, z2)
    used = test_copy(s, 'abyssal_sanctum/zz_test_copy')
    tid = 'raft:abyssal_sanctum/zz_test_copy' if used else 'raft:abyssal_sanctum/sanctum'
    L += [f'log template {tid} proxies {used}']
    L += ['wait 20', f'cmd {DIM}place template {tid} {X} {Y} {Z}', 'wait 300']
    ax, ay, az = W(*A.ALTAR)
    altar = 'minecraft:gold_block' if 'raft:leviathan_altar' in used else 'raft:leviathan_altar[facing=north]'
    seal = 'minecraft:light_blue_stained_glass' if 'raft:tide_seal' in used else 'raft:tide_seal[open=false]'
    L += [f'log ALTAR at {ax} {ay} {az}; pool centre {ax} {ay} {az - 9}; boss spawn {ax} {ay - 4} {az - 9}',
          check(altar, ax, ay, az, 'altar_at_contract_position'),
          check('minecraft:water', ax, ay - 4, az - 9, 'boss_spawn_in_water'),
          check('minecraft:water', ax, ay - 1, az - 9, 'pool_surface_water'),
          check('minecraft:water', ax, ay - 11, az - 9, 'pool_depth_11'),
          check('minecraft:air', ax, ay, az - 1, 'floor_in_front_of_altar_air')]
    sx, sy, sz = W(0, 15, A.SEAL_Z)
    L += [check(seal, sx, sy, sz, 'seal_closed'),
          check('minecraft:air', sx, sy, sz - 1, 'air_behind_seal'),
          check('minecraft:water', sx, sy, sz + 1, 'water_in_front_of_seal')]
    # shots before the leak count (structure_void would show nothing, but keep pictures honest)
    ex, ey, ez = W(0, 22, 74)
    L += ['hud off', tp(ex, ey, ez, 180, 12), 'settle 20', 'wait 200', 'shot sanctum_outside_south']
    ex, ey, ez = W(-40, 30, -30)
    L += [tp(ex, ey, ez, -125, 18), 'settle 20', 'wait 40', 'shot sanctum_outside_nw']
    ex, ey, ez = W(0, 14, 30)
    L += [tp(ex, ey, ez, 180, 5), 'wait 40', 'shot sanctum_vestibule_seal']
    ex, ey, ez = W(0, 14, 21)
    L += [tp(ex, ey, ez, 180, 8), 'wait 40', 'shot sanctum_arena_from_gate']
    ex, ey, ez = W(14, 20, -12)
    L += [tp(ex, ey, ez, 135, 25), 'wait 40', 'shot sanctum_arena_pool']
    ex, ey, ez = W(0, 14, -21)
    L += [tp(ex, ey, ez, 180, 5), 'wait 40', 'shot sanctum_reward_room']
    ex, ey, ez = W(-30, 22, 50)
    L += [tp(ex, ey, ez, -135, 10), 'wait 40', 'shot sanctum_stair_causeway']
    # leak counts: dome interior box above the floor, reward room, passage
    a = W(-10, 13, -10); b = W(10, 27, 10)
    L += count_water(*a, *b, 'dome_interior')
    a = W(-6, 13, -37); b = W(6, 18, -27)
    L += count_water(*a, *b, 'reward_room')
    a = W(-2, 13, 19); b = W(2, 17, 23)
    L += count_water(*a, *b, 'dry_passage')
    a = W(-21, 13, -5); b = W(21, 17, 5)
    L += count_water(*a, *b, 'dome_ring_east_west')
    L += ['log done']
    return L


def helix_script():
    from . import helix_station as H
    s = H.build()
    (bx, by, bz), _ = s.bounds()
    X, Y, Z = 3200, 50, 3000
    W = lambda x, y, z: (X + x - bx, Y + y - by, Z + z - bz)  # noqa: E731
    L = list(HEAD)
    cx, cy, cz = W(0, 10, -8)
    L += [tp(cx, 90, cz, 0, 90), 'settle 30']
    (x1, y1, z1), (x2, y2, z2) = W(-32, -3, -38), W(30, 22, 22)
    L += water_box(x1, y1, z1, x2, y2, z2)
    L += ['wait 20', f'cmd {DIM}place template raft:helix_station/station_0 {X} {Y} {Z}', 'wait 300']
    for log, (x, y, z) in [(b[2]['LogId'], p) for p, b in s.blocks.items()
                           if b[2] and b[2].get('id') == 'raft:research_terminal']:
        L.append(check('raft:research_terminal', *W(x, y, z), f'terminal_{log}'))
    L += [check('raft:bulkhead_door', *W(0, 1, 12), 'outer_airlock_door'),
          check('raft:bulkhead_door', *W(0, 1, 7), 'inner_airlock_door'),
          check('minecraft:water', *W(3, 0, 3), 'moon_pool_water'),
          check('minecraft:air', *W(3, 1, 3), 'moon_pool_air_above')]
    L += ['hud off']
    for name, (x, y, z), yaw, pitch in [
            ('helix_outside_south', (-6, 9, 30), 200, 18), ('helix_outside_east', (40, 12, 8), 110, 20),
            ('helix_outside_nw', (-38, 12, -40), -40, 18), ('helix_airlock', (0, 2, 10), 180, 5),
            ('helix_hub', (-4, 3, 4), 135, 15), ('helix_lab', (14, 3, 0), -90, 10),
            ('helix_crew', (-15, 3, 0), 90, 12), ('helix_generator', (0, 3, -14), 180, 10),
            ('helix_vault', (0, 2, -27), 180, 15), ('helix_flooded', (-13, 4, -15), 120, 20)]:
        L += [tp(*W(x, y, z), yaw, pitch), 'wait 60' if name.endswith('south') else 'wait 30']
        if name.endswith('south'):
            L += ['settle 20', 'wait 200']
        L.append(f'shot {name}')
    for label, a, b in [('hub', (-4, 1, -4), (4, 5, 4)), ('lab', (15, 1, -3), (23, 4, 3)),
                        ('crew', (-25, 1, -2), (-16, 5, 2)), ('generator', (-5, 1, -23), (5, 5, -15)),
                        ('vault', (-2, 1, -31), (2, 4, -27)), ('storage', (-8, 1, 8), (-3, 3, 12)),
                        ('airlock', (-1, 1, 8), (1, 3, 11)), ('tunnels_x', (-13, 1, -1), (13, 3, 1))]:
        L += count_water(*W(*a), *W(*b), f'helix_{label}')
    L += ['log done']
    return L


def temple_script():
    from . import coral_temple as C
    s = C.build()
    (bx, by, bz), _ = s.bounds()
    X, Y, Z = 3400, 70, 3000
    W = lambda x, y, z: (X + x - bx, Y + y - by, Z + z - bz)  # noqa: E731
    L = list(HEAD)
    cx, cy, cz = W(0, 10, 0)
    L += [tp(cx, 100, cz, 0, 90), 'settle 30']
    (x1, y1, z1), (x2, y2, z2) = W(-24, -1, -27), W(24, 24, 27)   # top y 94 < 99: all under the sea
    L += water_box(x1, y1, z1, x2, y2, z2)
    used = test_copy(s, 'coral_temple/zz_test_copy')
    tid = 'raft:coral_temple/zz_test_copy' if used else 'raft:coral_temple/temple_0'
    altar = 'minecraft:gold_block' if used else 'raft:tidewarden_altar[facing=south]'
    L += [f'log template {tid} proxies {used}']
    L += ['wait 20', f'cmd {DIM}place template {tid} {X} {Y} {Z}', 'wait 200']
    L += [check(altar, *W(0, 4, -18), 'altar_position'),
          check('raft:sigil_pedestal', *W(-3, 4, -20), 'pedestal_1'),
          check('minecraft:chest', *W(0, 4, -21), 'story_chest')]
    L += ['hud off']
    for name, (x, y, z), yaw, pitch in [
            ('temple_outside_south', (6, 12, 44), 190, 15), ('temple_outside_ne', (34, 18, -30), 135, 25),
            ('temple_courtyard', (0, 6, 15), 180, 8), ('temple_sanctum', (0, 6, -8), 180, 10),
            ('temple_altar', (3, 6, -14), 150, 25), ('temple_above', (0, 24, 2), 180, 60)]:
        L += [tp(*W(x, y, z), yaw, pitch), 'wait 30']
        if name.endswith('south'):
            L += ['settle 20', 'wait 200']
        L.append(f'shot {name}')
    L += ['log done']
    return L


def cleanup():
    """Delete the temporary test copies again."""
    from bclib.paths import MODULES
    for sub in ('abyssal_sanctum', 'coral_temple', 'helix_station'):
        p = os.path.join(MODULES, MODULE, 'data', 'raft', 'structure', sub, 'zz_test_copy.nbt')
        if os.path.isfile(p):
            os.remove(p)
            print('removed', p)


def main(argv=()):
    if 'clean' in argv:
        cleanup()
        return
    os.makedirs(WORK, exist_ok=True)
    allc = list(HEAD)
    for name, fn in (('sanctum', sanctum_script), ('helix', helix_script), ('temple', temple_script)):
        L = fn()
        p = f'{WORK}/test_{name}.txt'
        with open(p, 'w') as f:
            f.write('\n'.join(L) + '\n')
        allc += [c for c in L[len(HEAD):] if c != 'hud off'] + ['hud on']
        print(p)
    # full worldgen pipeline: /place structure (projection to the ocean floor, random rotation, processors)
    for i, sid in enumerate(('helix_station', 'coral_temple', 'abyssal_sanctum')):
        x = 3700 + 250 * i
        allc += [tp(x, 120, 3000, 0, 90), 'settle 20', f'log PLACE_STRUCTURE {sid} at {x} 3000',
                 f'cmd {DIM}place structure raft:{sid} {x} 60 3000', 'wait 100',
                 tp(x + 8, 115, 3040, 180, 35), 'settle 15', 'wait 60', 'hud off', f'shot place_{sid}', 'hud on']
    with open(f'{WORK}/test_all.txt', 'w') as f:
        f.write('\n'.join(c if not c.startswith('shot') else 'hud off\n' + c for c in allc) + '\n')
    print(f'{WORK}/test_all.txt')


if __name__ == '__main__':
    import sys
    main(sys.argv[1:])
