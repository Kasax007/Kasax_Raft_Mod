"""Turns a bctest script written for Minecraft 26.3 (tools/content/*/tests/*.txt) into one for the 1.21.1 harness:
the command syntax of 1.21.1 (game rule and attribute names, entity equipment, plain-text custom names). The script
language itself is the same.

Run from tools/:  python3 -m port1211.tests <script.txt> <out.txt>
"""
import re
import sys

# game rules the newer game renamed (snake case there, camel case in 1.21.1)
GAMERULES = {
    'spawn_monsters': 'doMobSpawning', 'random_tick_speed': 'randomTickSpeed', 'advance_time': 'doDaylightCycle',
    'advance_weather': 'doWeatherCycle', 'spawn_mobs': 'doMobSpawning', 'keep_inventory': 'keepInventory',
    'mob_griefing': 'mobGriefing', 'fall_damage': 'fallDamage', 'fire_damage': 'fireDamage',
    'drowning_damage': 'drowningDamage', 'immediate_respawn': 'doImmediateRespawn', 'show_death_messages': 'showDeathMessages',
    'do_daylight_cycle': 'doDaylightCycle', 'do_weather_cycle': 'doWeatherCycle', 'do_mob_spawning': 'doMobSpawning',
    'send_command_feedback': 'sendCommandFeedback', 'command_block_output': 'commandBlockOutput',
    'spawn_phantoms': 'doInsomnia', 'max_entity_cramming': 'maxEntityCramming',
}
# attributes: 1.21.1 names most of them with a generic. / player. prefix
ATTRIBUTES = {
    'movement_speed': 'generic.movement_speed', 'max_health': 'generic.max_health', 'attack_damage': 'generic.attack_damage',
    'attack_speed': 'generic.attack_speed', 'armor': 'generic.armor', 'armor_toughness': 'generic.armor_toughness',
    'knockback_resistance': 'generic.knockback_resistance', 'luck': 'generic.luck', 'scale': 'generic.scale',
    'step_height': 'generic.step_height', 'gravity': 'generic.gravity', 'safe_fall_distance': 'generic.safe_fall_distance',
    'water_movement_efficiency': 'generic.water_movement_efficiency', 'oxygen_bonus': 'generic.oxygen_bonus',
    'block_interaction_range': 'player.block_interaction_range', 'entity_interaction_range': 'player.entity_interaction_range',
    'block_break_speed': 'player.block_break_speed', 'submerged_mining_speed': 'player.submerged_mining_speed',
}
SLOTS_HAND = ('mainhand', 'offhand')
SLOTS_ARMOR = ('feet', 'legs', 'chest', 'head')


def _split_top(s):
    """Splits 'a:{...},b:{...}' at top-level commas."""
    out, depth, cur = [], 0, ''
    for ch in s:
        if ch in '{[':
            depth += 1
        elif ch in '}]':
            depth -= 1
        if ch == ',' and depth == 0:
            out.append(cur)
            cur = ''
        else:
            cur += ch
    if cur:
        out.append(cur)
    return out


def _equipment(m):
    body = m.group(1)
    slots = {}
    for part in _split_top(body):
        k, v = part.split(':', 1)
        slots[k.strip()] = v.strip()
    out = []
    if any(s in slots for s in SLOTS_HAND):
        out.append('HandItems:[%s]' % ','.join(slots.get(s, '{}') for s in SLOTS_HAND))
    if any(s in slots for s in SLOTS_ARMOR):
        out.append('ArmorItems:[%s]' % ','.join(slots.get(s, '{}') for s in SLOTS_ARMOR))
    return ','.join(out)


def _balanced(s, start):
    """End index (exclusive) of the {...} that starts at s[start]."""
    depth = 0
    for i in range(start, len(s)):
        if s[i] == '{':
            depth += 1
        elif s[i] == '}':
            depth -= 1
            if depth == 0:
                return i + 1
    return len(s)


def line(s):
    if s.lstrip().startswith('#'):
        return s
    s = re.sub(r'\bgamerule (\w+)', lambda m: 'gamerule ' + GAMERULES.get(m.group(1), m.group(1)), s)
    s = re.sub(r'\battribute (\S+) (minecraft:)?([a-z_]+)\b',
               lambda m: 'attribute %s minecraft:%s' % (m.group(1), ATTRIBUTES.get(m.group(3), m.group(3))), s)
    # equipment:{mainhand:{...},head:{...}} -> HandItems / ArmorItems
    while 'equipment:{' in s:
        i = s.index('equipment:{')
        j = _balanced(s, i + len('equipment:'))
        inner = s[i + len('equipment:{'):j - 1]
        s = s[:i] + _equipment(re.match(r'(.*)', inner, re.S)) + s[j:]
    # custom names as text components (1.21.1 reads JSON there): CustomName:'Target' -> CustomName:'"Target"'
    s = re.sub(r"CustomName:'([^'\"{\[][^']*)'", lambda m: "CustomName:'\"%s\"'" % m.group(1), s)
    return s


def convert(text):
    return '\n'.join(line(x) for x in text.split('\n'))


if __name__ == '__main__':
    with open(sys.argv[1], encoding='utf-8') as f:
        out = convert(f.read())
    with open(sys.argv[2], 'w', encoding='utf-8') as f:
        f.write(out)
