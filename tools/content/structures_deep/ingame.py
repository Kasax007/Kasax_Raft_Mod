"""bctest scripts for the structures_deep module (see tools/bctest/README.md).

    cd tools && python3 -m content.structures_deep.ingame      ->  $WORK/test_templates.txt, $WORK/test_natural.txt
    tools/bctest/run.sh $WORK/test_templates.txt $WORK/shots_t

test_templates.txt – every template in a barrier-walled water tank high in the sky (independent of the terrain the
  world module is rebuilding): /place template into air, fill the tank with water (replace air), /place template a
  second time (so dry rooms are not waterlogged by /place template's APPLY_WATERLOGGING – worldgen uses
  ignore_waterlogging), then contract checks (/say CHECK_OK ...), screenshots from outside / inside, water counts in
  the dry rooms (fill structure_void replace water: "No blocks were filled" = dry) and /locate of the natural
  structures and biomes.
test_natural.txt – /place structure (full worldgen pipeline: projection to the ocean floor, random rotation,
  processors, liquid_settings) at chunk-aligned spots; the rotation is unknown, so every check is written for all four
  rotations and anchored on a known top block (execute positioned over ocean_floor if block ...); only the matching
  rotation reports FOUND / CHECK_OK.
"""
import os

from .common import MODULE, WORK

DIM = 'execute in raft:raftdim run '
HEAD = ['world bctest_deep', 'gui 2', 'rd 8',
        'cmd effect give @p minecraft:night_vision infinite 0 true',
        'cmd effect give @p minecraft:conduit_power infinite 0 true',
        'cmd effect give @p minecraft:water_breathing infinite 0 true',
        'cmd gamerule spawn_mobs false']
ROTS = {'none': lambda x, z: (x, z), 'cw': lambda x, z: (-z, x), '180': lambda x, z: (-x, -z),
        'ccw': lambda x, z: (z, -x)}
ROT_FACING = {'none': {}, 'cw': {'north': 'east', 'east': 'south', 'south': 'west', 'west': 'north'},
              '180': {'north': 'south', 'south': 'north', 'east': 'west', 'west': 'east'},
              'ccw': {'north': 'west', 'west': 'south', 'south': 'east', 'east': 'north'}}
ROT_YAW = {'none': 0, 'cw': 90, '180': 180, 'ccw': 270}


def tp(x, y, z, yaw=0, pitch=0):
    return f'cmd {DIM}tp @p {x} {y} {z} {yaw} {pitch}'


def check(block_test, x, y, z, label):
    return f'cmd {DIM}execute if block {x} {y} {z} {block_test} run say CHECK_OK {label}'


def count_water(a, b, label):
    """Replaces water in the box by structure_void; the chat answer counts it ('No blocks were filled' = dry)."""
    return [f'log COUNT_WATER {label}',
            f'cmd {DIM}fill {a[0]} {a[1]} {a[2]} {b[0]} {b[1]} {b[2]} minecraft:structure_void replace minecraft:water']


def layered_fill(x1, y1, z1, x2, y2, z2, block, extra=''):
    area = (x2 - x1 + 1) * (z2 - z1 + 1)
    step = max(1, 32000 // area)
    out, y = [], y1
    while y <= y2:
        top = min(y2, y + step - 1)
        out.append(f'cmd {DIM}fill {x1} {y} {z1} {x2} {top} {z2} {block}{extra}')
        y = top + 1
    return out


def sky_tank(s, tid, X, Y, Z, margin=14, top=8):
    """Barrier tank with a sand floor around the template placed at X,Y,Z (its min corner); returns (cmds, W)."""
    (bx, by, bz), (ex, ey, ez) = s.bounds()
    W = lambda x, y, z: (X + x - bx, Y + y - by, Z + z - bz)  # noqa: E731
    x1, y1, z1 = X - margin, Y, Z - margin
    x2, y2, z2 = X + ex - bx + margin, Y + ey - by + top, Z + ez - bz + margin
    L = [tp((x1 + x2) // 2, y2 + 25, (z1 + z2) // 2, 0, 90), 'settle 25']
    L += layered_fill(x1 - 1, y1 - 2, z1 - 1, x2 + 1, y1 - 2, z2 + 1, 'minecraft:barrier')
    L += layered_fill(x1, y1 - 1, z1, x2, y1 - 1, z2, 'minecraft:sand')
    for (a, b) in (((x1 - 1, z1 - 1), (x2 + 1, z1 - 1)), ((x1 - 1, z2 + 1), (x2 + 1, z2 + 1)),
                   ((x1 - 1, z1), (x1 - 1, z2)), ((x2 + 1, z1), (x2 + 1, z2))):
        L += layered_fill(a[0], y1 - 1, a[1], b[0], y2, b[1], 'minecraft:barrier')
    L += [f'cmd {DIM}place template {tid} {X} {Y} {Z}', 'wait 20']
    L += layered_fill(x1, y1, z1, x2, y2, z2, 'minecraft:water', ' replace minecraft:air')
    L += [f'cmd {DIM}place template {tid} {X} {Y} {Z}', 'wait 200']
    return L, W


def look(cam, target):
    """Minecraft yaw/pitch from cam looking at target (yaw 0 = south, 90 = west; pitch > 0 looks down)."""
    import math
    dx, dy, dz = target[0] - cam[0], target[1] - (cam[1] + 1.6), target[2] - cam[2]
    yaw = math.degrees(math.atan2(-dx, dz))
    pitch = -math.degrees(math.atan2(dy, math.hypot(dx, dz)))
    return round(yaw), round(pitch)


def free_cam(s, cam):
    """Nudge a camera position (eye at +1.6) up/sideways until both the feet and the eye cells are free."""
    def ok(p):
        for dy in (0, 1, 2):
            b = s.blocks.get((p[0], p[1] + dy, p[2]))
            if b and b[0] not in ('minecraft:air', 'minecraft:water') and not b[0].endswith(
                    ('seagrass', 'kelp', 'kelp_plant', '_coral', '_coral_fan', 'barnacles')):
                return False
        return True
    x, y, z = cam
    for d in [(0, 0, 0), (0, 1, 0), (1, 0, 0), (-1, 0, 0), (0, 0, 1), (0, 0, -1), (0, 2, 0), (1, 1, 0), (-1, 1, 0),
              (0, 1, 1), (0, 1, -1), (2, 0, 0), (-2, 0, 0), (0, 0, 2), (0, 0, -2)]:
        p = (x + d[0], y + d[1], z + d[2])
        if ok(p):
            return p
    return cam


def shots(W, views, s=None):
    """views: (name, cam, yaw, pitch) or (name, cam, target) in template coordinates."""
    L = []
    for v in views:
        name, cam = v[0], v[1]
        if s is not None:
            cam = free_cam(s, cam)
        yaw, pitch = look(cam, v[2]) if len(v) == 3 else (v[2], v[3])
        L += [tp(*W(*cam), yaw, pitch), 'wait 40', f'shot {name}']
    return L


# ------------------------------------------------------------------------------------------- templates
def sanctum_part():
    from . import abyssal_sanctum as A
    s = A.build()
    L, W = sky_tank(s, 'raft:abyssal_sanctum/sanctum', 3000, 170, 3000)
    ax, ay, az = W(*A.ALTAR)
    L += [f'log SANCTUM ALTAR at {ax} {ay} {az}; pool centre {ax} {ay} {az - 9}; boss spawn {ax} {ay - 4} {az - 9}',
          check('raft:leviathan_altar[facing=north]', ax, ay, az, 'sanctum_altar_at_contract_position'),
          check('minecraft:water', ax, ay - 4, az - 9, 'sanctum_boss_spawn_in_water'),
          check('minecraft:water', ax, ay - 1, az - 9, 'sanctum_pool_surface_water'),
          check('minecraft:water', ax, ay - 11, az - 9, 'sanctum_pool_depth_11'),
          check('minecraft:air', ax, ay, az - 1, 'sanctum_floor_in_front_of_altar_air'),
          check('minecraft:air', ax, ay + 3, az - 9, 'sanctum_air_above_pool')]
    sx, sy, sz = W(0, 15, A.SEAL_Z)
    L += [check('raft:tide_seal[open=false]', sx, sy, sz, 'sanctum_seal_closed'),
          check('minecraft:air', sx, sy, sz - 1, 'sanctum_air_behind_seal'),
          check('minecraft:water', sx, sy, sz + 1, 'sanctum_water_in_front_of_seal'),
          'hud off']
    L += shots(W, [('sanctum_outside_south', (0, 26, 72), 180, 14),
                   ('sanctum_outside_nw', (-40, 28, -50), (0, 14, 0)),
                   ('sanctum_outside_east', (44, 16, 6), 90, 4),
                   ('sanctum_vestibule_seal', (0, 14, 30), 180, 5),
                   ('sanctum_arena_from_gate', (0, 15, 21), 180, 10),
                   ('sanctum_arena_pool', (12, 19, -12), (0, 11, 0)),
                   ('sanctum_altar', (-6, 14, 15), (0, 13.5, 9)),
                   ('sanctum_ring_pillars', (-17, 14, 4), (12, 16, -4)),
                   ('sanctum_reward_room', (0, 14, -24), 180, 10),
                   ('sanctum_stair_causeway', (-18, 20, 54), (0, 14, 36))], s)
    for label, a, b in [('sanctum_dome_ring', (-14, 13, -14), (14, 23, 14)),
                        ('sanctum_reward_room', (-6, 13, -37), (6, 18, -27)),
                        ('sanctum_dry_passage', (-2, 13, 19), (2, 17, 23))]:
        L += count_water(W(*a), W(*b), label)
    L += ['hud on']
    return L


def helix_part():
    from . import helix_station as H
    s = H.build()
    L, W = sky_tank(s, 'raft:helix_station/station_0', 3200, 190, 3000)
    for log, (x, y, z) in sorted(((b[2]['LogId'], p) for p, b in s.blocks.items()
                                  if b[2] and b[2].get('id') == 'raft:research_terminal')):
        L.append(check('raft:research_terminal', *W(x, y, z), f'helix_terminal_{log}'))
    L += [check('raft:bulkhead_door', *W(0, 1, 12), 'helix_outer_airlock_door'),
          check('raft:bulkhead_door', *W(0, 1, 7), 'helix_inner_airlock_door'),
          check('minecraft:water', *W(3, 0, 3), 'helix_moon_pool_water'),
          check('minecraft:air', *W(3, 1, 3), 'helix_moon_pool_air_above'),
          check('minecraft:chest', *W(19, 1, -5), 'helix_lab_chest'),
          check('minecraft:chest', *W(0, 2, -31), 'helix_vault_chest'), 'hud off']
    L += shots(W, [('helix_outside_south', (-6, 9, 30), 200, 18), ('helix_outside_east', (40, 10, 8), 110, 16),
                   ('helix_outside_nw', (-38, 12, -44), -40, 16), ('helix_airlock', (0, 1, 11), (0, 2, 7)),
                   ('helix_hub', (-3, 1, -3), (3, 1, 3)), ('helix_lab', (14, 1, 0), (21, 1.5, -2)),
                   ('helix_crew', (-15, 1, 0), (-26, 2, 0)), ('helix_generator', (0, 1, -14), (0, 2, -24)),
                   ('helix_vault', (0, 1, -27), (0, 2, -31)), ('helix_flooded', (-14, 2, -16), (-20, 1, -21))], s)
    for label, a, b in [('hub', (-4, 1, -4), (4, 5, 4)), ('lab', (15, 1, -3), (23, 4, 3)),
                        ('crew', (-25, 1, -2), (-16, 5, 2)), ('generator', (-5, 1, -23), (5, 5, -15)),
                        ('vault', (-2, 1, -31), (2, 4, -27)), ('storage', (-8, 1, 8), (-3, 3, 12)),
                        ('airlock', (-1, 1, 8), (1, 3, 11)), ('tunnels_x', (-13, 1, -1), (13, 3, 1))]:
        L += count_water(W(*a), W(*b), f'helix_{label}')
    L += ['hud on']
    return L


def temple_part():
    from . import coral_temple as C
    s = C.build()
    L, W = sky_tank(s, 'raft:coral_temple/temple_0', 3400, 190, 3000, top=8)
    L += [check('raft:tidewarden_altar[facing=south]', *W(0, 4, -18), 'temple_altar_position'),
          check('raft:sigil_pedestal', *W(-3, 4, -20), 'temple_pedestal_1'),
          check('minecraft:chest', *W(0, 4, -21), 'temple_story_chest'), 'hud off']
    L += shots(W, [('temple_outside_south', (6, 12, 36), 190, 15),
                   ('temple_outside_ne', (30, 18, -34), (0, 6, -4)),
                   ('temple_outside_west', (-32, 8, 4), (0, 6, 0)),
                   ('temple_gate', (0, 5, 30), 180, 2), ('temple_courtyard', (0, 6, 15), 180, 8),
                   ('temple_sanctum', (0, 6, -8), 180, 10), ('temple_altar', (3, 6, -13), (0, 4.5, -18)),
                   ('temple_above', (0, 27, 4), 180, 55)], s)
    L += ['hud on']
    return L


LOCATE = ['log LOCATE', f'cmd {DIM}locate structure raft:helix_station', f'cmd {DIM}locate structure raft:coral_temple',
          f'cmd {DIM}locate structure raft:abyssal_sanctum', f'cmd {DIM}locate biome raft:abyssal_trench',
          f'cmd {DIM}locate biome raft:coral_shallows']


# ------------------------------------------------------------------------------------------- natural pipeline
def anchored(s, X0, Z0, anchor, rot, tail):
    """execute ... anchored at template column `anchor` (build coords) for rotation rot of a piece whose saved
    origin is at chunk corner X0, Z0; tail continues the execute chain at the anchor's top block +1."""
    (bx, by, bz), _ = s.bounds()
    ax, az = anchor
    top = max(y for y in range(by, 64) if (ax, y, az) in s.blocks and
              s.blocks[(ax, y, az)][0] not in ('minecraft:air', 'minecraft:water'))
    tb = s.blocks[(ax, top, az)][0]
    rx, rz = ROTS[rot](ax - bx, az - bz)
    return (f'cmd {DIM}execute positioned {X0 + rx} 0 {Z0 + rz} positioned over ocean_floor if block ~ ~-1 ~ {tb} '
            f'{tail}'), top


def _rotated_world(s, X0, Z0, rot, x, z):
    """World x, z of build column (x, z) for a piece whose saved origin is at X0, Z0 with rotation rot."""
    (bx, by, bz), _ = s.bounds()
    rx, rz = ROTS[rot](x - bx, z - bz)
    return X0 + rx, Z0 + rz


def natural_script(sites=None, place=True):
    """sites: {sid: (X0, Z0)} chunk-aligned spots; place=False visits structures that generated naturally there
    (X0, Z0 = the start chunk's corner, as /locate prints it). Rotation and height are unknown, so a probe block with a
    known facing is searched in all four rotations over a height range (one execute per candidate); only the real
    one answers FOUND / CHECK_OK and moves the camera."""
    from . import abyssal_sanctum as A, coral_temple as C, helix_station as H
    sites = sites or {'abyssal_sanctum': (4000, 3008), 'helix_station': (4400, 3008), 'coral_temple': (4800, 3008)}
    L = list(HEAD) + LOCATE
    D = f'cmd {DIM}execute '
    for sid in ('abyssal_sanctum', 'helix_station', 'coral_temple'):
        if sid not in sites:
            continue
        X0, Z0 = sites[sid]
        L += [tp(X0 + 8, 160, Z0 + 8, 0, 90), 'settle 25', f'log VISIT {sid} at {X0} {Z0}']
        if place:
            L += [f'cmd {DIM}place structure raft:{sid} {X0} 64 {Z0}', 'wait 200']
        for rot in ROTS:
            F = lambda f: ROT_FACING[rot].get(f, f)  # noqa: E731
            W2 = lambda x, z: _rotated_world(MODS[sid], X0, Z0, rot, x, z)  # noqa: E731
            yaw = ROT_YAW[rot]
            if sid == 'abyssal_sanctum':
                ax, az = W2(*A.ALTAR[0::2])
                cx, cz = W2(0, 0)
                rx, rz = W2(0, -15)
                probe = f'raft:leviathan_altar[facing={F("north")}]'
                for y in range(0, 91):
                    # any facing: finds the altar even while LeviathanAltarBlock does not rotate with the structure
                    L += [D + f'if block {ax} {y} {az} raft:leviathan_altar if block {cx} {y - 4} {cz} '
                              f'minecraft:water if block {cx} {y + 3} {cz} minecraft:air run say FOUND sanctum altar '
                              f'rotation {rot} at {ax} {y} {az} (pool + air ok; facing checked below)']
                    L += [D + f'if block {ax} {y} {az} {probe} if block {cx} {y - 4} {cz} minecraft:water '
                              f'if block {cx} {y + 3} {cz} minecraft:air run say CHECK_OK natural_sanctum_contract '
                              f'rotation {rot} altar {ax} {y} {az} pool_centre {cx} {cz} boss_spawn {cx} {y - 4} {cz}',
                          D + f'if block {ax} {y} {az} raft:leviathan_altar run tp @p {rx} {y + 2} {rz} {yaw} 22',
                          D + f'if block {ax} {y} {az} raft:leviathan_altar run fill {cx - 14} {y + 1} {cz - 14} {cx + 14} '
                              f'{y + 9} {cz + 14} minecraft:structure_void replace minecraft:water']
            elif sid == 'helix_station':
                vx, vz = W2(0, -31)
                hx, hz = W2(0, 0)
                cam = W2(-3, -3)
                probe = f'minecraft:chest[facing={F("south")}]'
                for y in range(30, 111):
                    L += [D + f'if block {vx} {y} {vz} {probe} run say FOUND helix vault chest rotation {rot} at '
                              f'{vx} {y} {vz}',
                          D + f'if block {vx} {y} {vz} {probe} run tp @p {cam[0]} {y - 1} {cam[1]} {yaw + 135} 15',
                          D + f'if block {vx} {y} {vz} {probe} run fill {hx - 4} {y - 1} {hz - 4} {hx + 4} {y + 3} '
                              f'{hz + 4} minecraft:structure_void replace minecraft:water']
            else:
                ax, az = W2(0, -18)
                cam = W2(0, 10)
                probe = f'raft:tidewarden_altar[facing={F("south")}]'
                for y in range(40, 111):
                    L += [D + f'if block {ax} {y} {az} {probe} run say FOUND temple altar rotation {rot} at '
                              f'{ax} {y} {az}',
                          D + f'if block {ax} {y} {az} {probe} run tp @p {cam[0]} {y + 2} {cam[1]} {yaw + 180} 10']
        L += ['log COUNT_WATER (the structure_void fills above are the dry-room counts)', 'wait 60', 'hud off',
              f'shot natural_{sid}', 'hud on']
        L += [f'cmd {DIM}execute positioned {X0 + 8} 0 {Z0 + 8} positioned over ocean_floor run tp @p ~-30 ~18 ~-30 '
              f'-45 30', 'wait 60', 'hud off', f'shot natural_{sid}_overview', 'hud on']
    L += ['log done']
    return L


MODS = {}


def _mods():
    from . import abyssal_sanctum as A, coral_temple as C, helix_station as H
    if not MODS:
        MODS.update({'abyssal_sanctum': A.build(), 'helix_station': H.build(), 'coral_temple': C.build()})


def main(argv=()):
    os.makedirs(WORK, exist_ok=True)
    nat = [a for a in argv if a.startswith('natural=')]
    if nat:          # natural=helix_station:x:z,coral_temple:x:z,...  -> visit naturally generated structures
        sites = {}
        for part in nat[0][8:].split(','):
            sid, x, z = part.split(':')
            sites[sid] = (int(x) // 16 * 16, int(z) // 16 * 16)
        _mods()
        with open(f'{WORK}/test_visit.txt', 'w') as f:
            f.write('\n'.join(natural_script(sites, place=False)) + '\n')
        print(f'{WORK}/test_visit.txt')
        return
    L = list(HEAD) + sanctum_part() + temple_part() + helix_part() + LOCATE + ['log done']
    with open(f'{WORK}/test_templates.txt', 'w') as f:
        f.write('\n'.join(L) + '\n')
    _mods()
    with open(f'{WORK}/test_natural.txt', 'w') as f:
        f.write('\n'.join(natural_script()) + '\n')
    print(f'{WORK}/test_templates.txt', len(L))
    print(f'{WORK}/test_natural.txt')


if __name__ == '__main__':
    import sys
    main(sys.argv[1:])
