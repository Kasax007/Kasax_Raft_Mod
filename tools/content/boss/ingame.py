"""Writes bctest scripts for the boss module: a test arena built with fill commands (the sanctum contract: floor ring,
pool radius 7, 11 deep, boss spawn 4 below the altar position) and a scripted fight.

    python3 tools/content/boss/ingame.py $WORK      -> $WORK/boss_fight.txt, $WORK/boss_items.txt
    tools/bctest/run.sh $WORK/boss_fight.txt $WORK/shots_fight
"""
import math
import os
import sys

DIM = 'execute in raft:raftdim run '
X, Z, F = 200, 200, 160          # pool centre (x, z) and arena floor block layer


def arena():
    L = [f'cmd {DIM}fill {X - 23} {F - 12} {Z - 23} {X + 23} {F} {Z + 23} minecraft:polished_deepslate',
         f'cmd {DIM}fill {X - 23} {F + 1} {Z - 23} {X + 23} {F + 12} {Z + 23} minecraft:air',
         f'cmd {DIM}fill {X - 23} {F + 1} {Z - 23} {X + 23} {F + 4} {Z + 23} minecraft:light_blue_stained_glass hollow',
         f'cmd {DIM}fill {X - 22} {F + 1} {Z - 22} {X + 22} {F + 4} {Z + 22} minecraft:air']
    for dz in range(-7, 8):
        hx = int(math.floor(math.sqrt(max(0.0, 7.3 ** 2 - dz * dz))))
        L.append(f'cmd {DIM}fill {X - hx} {F - 10} {Z + dz} {X + hx} {F} {Z + dz} minecraft:water')
    for dx, dz in ((10, 4), (-10, 4), (4, -10), (-4, -10), (15, 15), (-15, -15)):
        L.append(f'cmd {DIM}setblock {X + dx} {F + 1} {Z + dz} minecraft:sea_lantern')
    return L


HEAD = ['world bctest_boss', 'gui 2', 'rd 8', 'cmd gamerule spawn_mobs false', 'cmd gamerule advance_time false',
        'cmd time set noon']


def fight():
    L = list(HEAD)
    L += [f'cmd {DIM}tp @p {X} {F + 30} {Z + 12} 180 60', 'settle 20']
    L += arena()
    L += ['wait 40', 'cmd gamemode survival @p',
          'cmd effect give @p minecraft:resistance infinite 4 true',
          'cmd effect give @p minecraft:regeneration infinite 4 true',
          'cmd effect give @p minecraft:saturation infinite 4 true',
          'cmd effect give @p minecraft:night_vision infinite 0 true',
          f'cmd {DIM}tp @p {X + 0.5} {F + 1} {Z + 13.5} 180 22', 'wait 20',
          f'cmd {DIM}summon raft:leviathan {X + 0.5} {F - 3} {Z + 0.5}',
          'log SUMMONED']
    for i in range(6):
        L += ['wait 12', f'shot intro_{i}']
    for i in range(24):
        L += ['wait 34', f'cmd {DIM}tp @p {X + 0.5 + (6 if i % 8 >= 4 else -6) * (i % 2)} {F + 1} {Z + 13.5} 180 22',
              f'shot p1_{i:02d}']
    L += ['log PHASE2', f'cmd {DIM}damage @e[type=raft:leviathan,limit=1] 310 minecraft:generic_kill by @p',
          f'cmd {DIM}data get entity @e[type=raft:leviathan,limit=1] Health']
    for i in range(22):
        L += ['wait 34', f'cmd {DIM}tp @p {X + 0.5} {F + 1} {Z + 12.5} 180 22', f'shot p2_{i:02d}']
    L += ['log PHASE3', f'cmd {DIM}damage @e[type=raft:leviathan,limit=1] 150 minecraft:generic_kill by @p',
          f'cmd {DIM}data get entity @e[type=raft:leviathan,limit=1] Health']
    for i in range(10):
        L += ['wait 30', f'cmd {DIM}tp @p {X + 0.5} {F + 1} {Z + 12.5} 180 22', f'shot p3_{i:02d}']
    L += ['log KILL', f'cmd {DIM}tp @p {X + 0.5} {F + 1} {Z + 12.5} 180 22',
          f'cmd {DIM}damage @e[type=raft:leviathan,limit=1] 200 minecraft:generic_kill by @p']
    for i in range(8):
        L += ['wait 12', f'shot death_{i}']
    L += ['wait 40', 'shot drops',
          f'cmd {DIM}execute if entity @e[type=item,distance=..60,x={X},y={F},z={Z}] run say DROPS_PRESENT',
          f'cmd {DIM}execute as @e[type=item,x={X},y={F},z={Z},distance=..60] run say ITEM',
          f'cmd {DIM}execute if entity @e[type=raft:leviathan] run say STILL_ALIVE',
          f'cmd {DIM}execute if entity @e[type=raft:tideling] run say TIDELINGS_LEFT', 'log done']
    return L


def items():
    L = list(HEAD)
    L += [f'cmd {DIM}tp @p {X} {F + 30} {Z} 0 30', 'settle 20']
    L += arena()
    L += ['wait 20', 'cmd clear @p', 'cmd gamemode creative @p',
          'cmd effect give @p minecraft:resistance infinite 4 true',
          'cmd effect give @p minecraft:regeneration infinite 4 true',
          'hud off',
          # model close-ups: a still Leviathan in the pool and one lying on the floor ring
          f'cmd {DIM}summon raft:leviathan {X + 0.5} {F - 0.5} {Z + 0.5} {{NoAI:1b,Rotation:[0f,-25f]}}',
          f'cmd {DIM}summon raft:leviathan {X - 13.5} {F + 1} {Z - 6.5} {{NoAI:1b,Rotation:[90f,0f]}}',
          f'cmd {DIM}tp @p {X + 0.5} {F + 3} {Z + 6} 180 15', 'wait 60', 'shot model_front',
          f'cmd {DIM}tp @p {X + 6.5} {F + 3} {Z + 2} 120 15', 'wait 20', 'shot model_side',
          f'cmd {DIM}tp @p {X - 4.5} {F + 2} {Z + 3} -130 5', 'wait 20', 'shot model_side2',
          f'cmd {DIM}tp @p {X + 0.5} {F + 12} {Z + 0.5} 0 89', 'wait 20', 'shot model_top',
          f'cmd {DIM}tp @p {X - 16.5} {F + 3} {Z - 1.5} -150 20', 'wait 20', 'shot land_front',
          f'cmd {DIM}tp @p {X - 9} {F + 7} {Z - 14} 30 35', 'wait 20', 'shot land_back',
          f'cmd {DIM}tp @p {X - 3} {F + 4} {Z - 5} 110 25', 'wait 20', 'shot land_side',
          f'cmd {DIM}tp @p {X + 8.5} {F + 2} {Z + 6} 135 10', 'wait 10', 'shot pool_view',
          f'cmd {DIM}kill @e[type=raft:leviathan]',
          'cmd give @p raft:tidebreaker', 'cmd give @p raft:leviathan_scale 12', 'cmd give @p raft:heart_of_the_tide',
          'cmd give @p raft:leviathan_trophy', 'cmd give @p raft:leviathan_spawn_egg', 'cmd give @p raft:tideling_spawn_egg',
          f'cmd {DIM}setblock {X + 12} {F + 1} {Z + 2} raft:leviathan_trophy[facing=south]',
          f'cmd {DIM}setblock {X + 14} {F + 1} {Z + 2} raft:leviathan_trophy[facing=east]',
          f'cmd {DIM}setblock {X + 12} {F + 1} {Z + 5} raft:heart_of_the_tide',
          f'cmd {DIM}setblock {X + 4} {F - 2} {Z} raft:heart_of_the_tide[waterlogged=true]',
          f'cmd {DIM}tp @p {X + 13} {F + 2.2} {Z + 7.5} 180 30', 'wait 30', 'shot blocks',
          f'cmd {DIM}tp @p {X + 12.5} {F + 1.6} {Z + 7} 160 35', 'wait 10', 'shot blocks_close',
          f'cmd {DIM}tp @p {X + 13.5} {F + 2} {Z + 4.5} 90 40', 'wait 10', 'shot trophy_side',
          'hud on', 'wait 10', 'shot hotbar',
          f'cmd {DIM}tp @p {X + 4} {F - 1} {Z + 3} 180 40', 'wait 60', 'shot heart_underwater',
          'cmd item replace entity @p weapon.mainhand with raft:tidebreaker', 'third on',
          f'cmd {DIM}tp @p {X + 12} {F + 1} {Z + 14} 0 0', 'wait 10', 'shot tidebreaker_third', 'third off',
          'wait 5', 'shot tidebreaker_first', 'hud off',
          f'cmd {DIM}summon raft:tideling {X + 13} {F + 1} {Z + 10}',
          f'cmd {DIM}summon raft:tideling {X + 1} {F - 3} {Z + 3} {{NoAI:1b}}',
          f'cmd {DIM}tp @p {X + 12.5} {F + 1.5} {Z + 13} 180 25', 'cmd gamemode survival @p', 'wait 30', 'shot tideling_land',
          'cmd gamemode creative @p', 'wait 200', 'shot tideling_flop',
          f'cmd {DIM}tp @p {X + 1.5} {F - 3} {Z + 5} 180 10', 'wait 30', 'shot tideling_water',
          f'cmd {DIM}summon raft:brine_bolt {X + 1.5} {F + 3} {Z + 2}', f'cmd {DIM}tp @p {X + 1.5} {F + 3} {Z + 5} 180 10',
          'wait 1', 'shot bolt', 'log done']
    return L


def final():
    """Phase 2 straight away (roar, tidelings, whirlpool), then the Tidebreaker dash in the pool."""
    L = list(HEAD)
    L += [f'cmd {DIM}tp @p {X} {F + 30} {Z + 12} 180 60', 'settle 20']
    L += arena()
    L += ['wait 40', 'cmd gamemode survival @p',
          'cmd effect give @p minecraft:resistance infinite 4 true',
          'cmd effect give @p minecraft:regeneration infinite 4 true',
          'cmd effect give @p minecraft:saturation infinite 4 true',
          f'cmd {DIM}tp @p {X + 0.5} {F + 1} {Z + 10.5} 180 25', 'wait 20',
          f'cmd {DIM}summon raft:leviathan {X + 0.5} {F - 3} {Z + 0.5}', 'wait 30',
          f'cmd {DIM}damage @e[type=raft:leviathan,limit=1] 320 minecraft:generic_kill by @p']
    for i in range(36):
        L += ['wait 22', f'shot f_{i:02d}']
        if i % 6 == 5:
            L += [f'cmd {DIM}tp @p {X + 0.5} {F + 1} {Z + 10.5} 180 25']
    L += [f'cmd {DIM}kill @e[type=raft:leviathan]', f'cmd {DIM}kill @e[type=raft:tideling]', 'wait 100',
          'cmd item replace entity @p weapon.mainhand with raft:tidebreaker',
          f'cmd {DIM}tp @p {X + 0.5} {F - 4} {Z + 5.5} 180 -10', 'wait 20', 'shot dash_before',
          'hold use 16', 'wait 3', 'shot dash_1', 'wait 6', 'shot dash_2', 'log done']
    return L


if __name__ == '__main__':
    out = sys.argv[1] if len(sys.argv) > 1 else '.'
    os.makedirs(out, exist_ok=True)
    for name, fn in (('boss_fight', fight), ('boss_items', items), ('boss_final', final)):
        with open(os.path.join(out, name + '.txt'), 'w') as f:
            f.write('\n'.join(fn()) + '\n')
    print('written to', out)
