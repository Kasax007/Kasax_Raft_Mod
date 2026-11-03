"""JSON of the boss module: loot tables (26.3 format), recipe, tags, sounds.json part and lang."""
from collections import OrderedDict

from bclib import assets

M = 'boss'


# ------------------------------------------------------------------------------------------------- loot
def _uniform(lo, hi):
    return {'type': 'minecraft:uniform', 'max': hi, 'min': lo}


def _looting(hi=1.0):
    return {'type': 'minecraft:enchanted_count_increase', 'count': _uniform(0.0, hi), 'enchantment': 'minecraft:looting'}


def _item(name, mods=None):
    e = OrderedDict(type='minecraft:item')
    if mods:
        e['modifier'] = mods if len(mods) > 1 else mods[0]
    e['name'] = assets.rid(name)
    return e


def _pool(entries, condition=None, rolls=1):
    p = OrderedDict()
    if condition:
        p['condition'] = condition
    p['entries'] = entries
    p['rolls'] = rolls
    return p


def loot():
    assets.loot_table(M, 'entities/leviathan', OrderedDict(type='minecraft:entity', pools=[
        _pool([_item('heart_of_the_tide')]),
        _pool([_item('leviathan_scale', [{'type': 'minecraft:set_count', 'count': _uniform(8, 12)}, _looting(2.0)])]),
        _pool([_item('leviathan_trophy')]),
    ], random_sequence='raft:entities/leviathan'))
    smelt = {'type': 'minecraft:furnace_smelt', 'condition': {'type': 'minecraft:entity_properties', 'entity': 'this',
                                                              'predicate': {'minecraft:flags': {'is_on_fire': True}}}}
    assets.loot_table(M, 'entities/tideling', OrderedDict(type='minecraft:entity', pools=[
        _pool([_item('minecraft:cod', [{'type': 'minecraft:set_count', 'count': _uniform(0, 1)}, _looting(), smelt])]),
        _pool([_item('minecraft:prismarine_crystals', [{'type': 'minecraft:set_count', 'count': _uniform(0, 1)}, _looting()])],
              condition={'type': 'minecraft:killed_by_player'}),
    ], random_sequence='raft:entities/tideling'))
    for block in ('heart_of_the_tide', 'leviathan_trophy'):
        assets.loot_table(M, 'blocks/' + block, OrderedDict(type='minecraft:block', pools=[
            _pool([_item(block)], condition={'type': 'minecraft:survives_explosion'})], random_sequence='raft:blocks/' + block))


# ------------------------------------------------------------------------------------------------- recipe and tags
def recipes():
    assets.write_json(M, 'data/raft/recipe/tidebreaker.json', OrderedDict(
        type='minecraft:crafting_shaped', category='equipment',
        key=OrderedDict([('S', 'raft:leviathan_scale'), ('I', 'raft:titanium_ingot'), ('T', 'minecraft:trident')]),
        pattern=['SIS', 'STS'],
        result=OrderedDict(count=1, id='raft:tidebreaker')))


def tags():
    assets.tag(M, 'item', 'raft:tidebreaker_repair_materials', ['leviathan_scale'])
    for t in ('minecraft:enchantable/weapon', 'minecraft:enchantable/sharp_weapon', 'minecraft:enchantable/melee_weapon',
              'minecraft:enchantable/fire_aspect', 'minecraft:enchantable/durability', 'minecraft:enchantable/sweeping',
              'minecraft:swords'):
        assets.tag(M, 'item', t, ['tidebreaker'])
    assets.tag(M, 'block', 'minecraft:mineable/pickaxe', ['heart_of_the_tide', 'leviathan_trophy'])
    # creatures of the sea: Tidebreaker deals +50 % damage to them
    assets.tag(M, 'entity_type', 'raft:tidebreaker_prey', [
        '#minecraft:aquatic', 'minecraft:drowned', 'raft:shark', 'raft:leviathan', 'raft:tideling',
        {'id': '#raft:sea_creatures', 'required': False}, {'id': 'raft:drowned_corsair', 'required': False}])
    for t in ('minecraft:aquatic', 'minecraft:can_breathe_under_water'):
        assets.tag(M, 'entity_type', t, ['leviathan', 'tideling'])
    assets.tag(M, 'entity_type', 'minecraft:fall_damage_immune', ['leviathan'])


# ------------------------------------------------------------------------------------------------- sounds
def _s(name, pitch=1.0, volume=1.0, dist=None):
    d = OrderedDict(name='minecraft:' + name)
    if pitch != 1.0:
        d['pitch'] = pitch
    if volume != 1.0:
        d['volume'] = volume
    if dist:
        d['attenuation_distance'] = dist
    return d


SOUNDS = OrderedDict([
    ('entity.leviathan.ambient', ([_s('mob/guardian/elder_idle%d' % i, 0.5, 1.0, 48) for i in (1, 2, 3)]
                                  + [_s('mob/enderdragon/growl%d' % i, 0.55, 0.6, 48) for i in (1, 2)], 'Leviathan rumbles')),
    ('entity.leviathan.hurt', ([_s('mob/guardian/elder_hit%d' % i, 0.6, 1.0, 40) for i in (1, 2, 3)], 'Leviathan hurts')),
    ('entity.leviathan.death', ([_s('mob/guardian/elder_death', 0.5, 1.0, 64)], 'Leviathan dies')),
    ('entity.leviathan.roar', ([_s('mob/warden/roar_%d' % i, 0.75, 1.0, 64) for i in (1, 2, 3)], 'Leviathan roars')),
    ('entity.leviathan.charge', ([_s('mob/warden/sonic_charge%d' % i, 1.0, 0.8, 40) for i in (1, 2, 3)], 'Leviathan gathers brine')),
    ('entity.leviathan.spit', ([_s('mob/llama/spit%d' % i, 0.5, 1.0, 40) for i in (1, 2)]
                               + [_s('mob/glow_squid/squirt%d' % i, 0.6, 1.0, 40) for i in (1, 2, 3)], 'Leviathan spits')),
    ('entity.leviathan.emerge', ([_s('liquid/heavy_splash', 0.6, 1.0, 48)], 'Leviathan breaks the surface')),
    ('entity.leviathan.dive', ([_s('liquid/heavy_splash', 0.75, 1.0, 48)], 'Leviathan dives')),
    ('entity.leviathan.slam', ([_s('item/mace/smash_ground_heavy', 0.7, 1.0, 48)], 'Leviathan slams down')),
    ('entity.leviathan.flop', ([_s('mob/guardian/flop%d' % i, 0.45, 1.0, 32) for i in (1, 2, 3)], 'Leviathan thrashes')),
    ('entity.leviathan.whirlpool', ([_s('block/bubble_column/whirlpool_inside', 0.6, 1.0, 48)]
                                    + [_s('block/bubble_column/whirlpool_ambient%d' % i, 0.5, 1.0, 48) for i in (1, 2, 3)],
                                    'Whirlpool churns')),
    ('entity.leviathan.deflect', ([_s('item/shield/block%d' % i, 0.55, 1.0, 24) for i in (1, 2, 3)], 'Scales deflect')),
    ('entity.leviathan.bite', ([_s('mob/zoglin/attack%d' % i, 0.5, 1.0, 24) for i in (1, 2)], 'Leviathan bites')),
    ('entity.leviathan.burst', ([_s('block/beacon/deactivate', 0.6, 1.0, 64), _s('mob/enderdragon/end', 1.4, 0.7, 64)],
                                'Light bursts from the deep')),
    ('entity.tideling.ambient', ([_s('mob/drowned/water/idle%d' % i, 1.6, 0.6) for i in (1, 2, 3)], 'Tideling gurgles')),
    ('entity.tideling.hurt', ([_s('entity/fish/hurt%d' % i, 0.8) for i in (1, 2, 3)], 'Tideling hurts')),
    ('entity.tideling.death', ([_s('mob/pufferfish/death%d' % i, 0.9) for i in (1, 2)], 'Tideling dies')),
    ('entity.tideling.flop', ([_s('entity/fish/flop%d' % i, 0.9) for i in (1, 2, 3)], 'Tideling flops')),
    ('entity.tideling.bite', ([_s('mob/hoglin/attack%d' % i, 1.7, 0.7) for i in (1, 2)], 'Tideling bites')),
    ('entity.brine_bolt.hit', ([_s('liquid/splash', 1.3), _s('liquid/splash2', 1.2)], 'Brine bolt bursts')),
    ('block.heart_of_the_tide.ambient', ([_s('block/conduit/ambient', 1.2, 0.8)], 'Heart of the Tide hums')),
    ('block.heart_of_the_tide.activate', ([_s('block/conduit/activate', 1.1)], 'Heart of the Tide awakens')),
    ('item.tidebreaker.dash', ([_s('item/trident/riptide3', 0.9)], 'Tidebreaker surges')),
])


def sounds():
    entries = OrderedDict()
    lang = {}
    for event, (files, subtitle) in SOUNDS.items():
        key = 'subtitles.raft.' + event
        entries[event] = OrderedDict(sounds=files, subtitle=key)
        lang[key] = subtitle
    assets.sounds(M, entries)
    assets.lang(M, lang)


# ------------------------------------------------------------------------------------------------- lang
LANG = {
    'entity.raft.leviathan': 'The Leviathan',
    'entity.raft.tideling': 'Tideling',
    'entity.raft.brine_bolt': 'Brine Bolt',
    'item.raft.leviathan_scale': 'Leviathan Scale',
    'item.raft.tidebreaker': 'Tidebreaker',
    'item.raft.leviathan_spawn_egg': 'Leviathan Spawn Egg',
    'item.raft.tideling_spawn_egg': 'Tideling Spawn Egg',
    'block.raft.heart_of_the_tide': 'Heart of the Tide',
    'block.raft.leviathan_trophy': 'Leviathan Trophy',
    'tooltip.raft.tidebreaker.1': '+50% damage to creatures of the sea',
    'tooltip.raft.tidebreaker.2': 'Hold use in water or rain to surge ahead',
    'tooltip.raft.heart_of_the_tide.1': 'Grants Conduit Power and Dolphin\'s Grace',
    'tooltip.raft.heart_of_the_tide.2': 'to swimmers within 32 blocks',
    'tooltip.raft.leviathan_trophy': 'The sea is calm again',
}


def generate():
    loot()
    recipes()
    tags()
    sounds()
    assets.lang(M, LANG)
