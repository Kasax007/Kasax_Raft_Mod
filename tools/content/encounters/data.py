"""JSON data of the encounters module: item models, lang, loot tables, tags and the sounds.json fragment."""
from bclib import assets

from content.shore.data import _count, _looting, _entity_table, _player_chance, _event

MOD = 'encounters'

ENTITIES = {
    'scavenger': 'Scavenger',
    'reef_guardian': 'Reef Guardian',
    'corsair_captain': 'Corsair Captain',
}

# items of other modules: one-item tags with an optional entry (an unknown item id in a loot table stops the world
# from loading in 26.3), same convention as the shore and story modules; own tag names so no file is shared
FOREIGN = ('gold_coin', 'scrap_metal', 'rope', 'plastic', 'pearl', 'sea_glass_shard', 'harpoon', 'cutlass',
           'coconut', 'fresh_water')


def item_models():
    for e in ENTITIES:
        assets.spawn_egg(MOD, f'{e}_spawn_egg')


def lang():
    entries = {}
    for e, title in ENTITIES.items():
        entries[f'entity.raft.{e}'] = title
        entries[f'item.raft.{e}_spawn_egg'] = f'{title} Spawn Egg'
    entries.update({
        'entity.raft.scavenger_chief': 'Scavenger Chief',
        'entity.raft.reef_warden': 'Reef Warden',
        'encounter.raft.scavengers': 'Scavengers! They have spotted you...',
        'encounter.raft.deep': 'Something stirs in the depths...',
        'encounter.raft.corsairs': 'A horn sounds: the cursed crew is coming!',
        'encounter.raft.reef': 'The reef is watching you...',
        'encounter.raft.forge': 'The forge awakens...',
        'subtitles.raft.entity.scavenger.ambient': 'Scavenger mutters',
        'subtitles.raft.entity.scavenger.hurt': 'Scavenger hurts',
        'subtitles.raft.entity.scavenger.death': 'Scavenger dies',
        'subtitles.raft.entity.reef_guardian.ambient': 'Reef Guardian hums',
        'subtitles.raft.entity.reef_guardian.hurt': 'Reef Guardian hurts',
        'subtitles.raft.entity.reef_guardian.death': 'Reef Guardian dies',
        'subtitles.raft.entity.corsair_captain.rally': 'Captain blows his horn',
        'subtitles.raft.encounter.scavengers': 'Scavengers shout',
        'subtitles.raft.encounter.deep': 'Something stirs',
        'subtitles.raft.encounter.corsairs': 'Pirate horn',
        'subtitles.raft.encounter.reef': 'The reef hums',
        'subtitles.raft.encounter.forge': 'The forge roars',
    })
    assets.lang(MOD, entries)


def _pool(item, modifiers=None, condition=None):
    if item in FOREIGN:
        entry = {'type': 'minecraft:tag', 'expand': False, 'items': f'#raft:loot/encounters/{item}'}
    else:
        entry = {'type': 'minecraft:item', 'name': assets.rid(item)}
    if modifiers:
        entry['modifier'] = modifiers if len(modifiers) > 1 else modifiers[0]
    pool = {}
    if condition:
        pool['condition'] = condition
    pool['entries'] = [entry]
    pool['rolls'] = 1
    return pool


def _chance(c):
    return {'type': 'minecraft:random_chance', 'chance': c}


def loot():
    # scavenger: what he looted (scrap, plastic, rope, coins), now and then food or water
    assets.loot_table(MOD, 'entities/scavenger', _entity_table('scavenger', [
        _pool('scrap_metal', [_count(0, 2), _looting()]),
        _pool('plastic', [_count(0, 2), _looting()]),
        _pool('rope', [_count(0, 1), _looting()]),
        _pool('gold_coin', [_count(0, 2), _looting()], condition={'type': 'minecraft:killed_by_player'}),
        _pool('coconut', condition=_chance(0.15)),
        _pool('fresh_water', condition=_chance(0.1)),
    ]))
    # reef guardian: prismarine like a guardian, pearls and sea glass of the reef
    assets.loot_table(MOD, 'entities/reef_guardian', _entity_table('reef_guardian', [
        _pool('minecraft:prismarine_shard', [_count(0, 2), _looting()]),
        _pool('minecraft:prismarine_crystals', [_count(0, 1), _looting()]),
        _pool('pearl', [_count(0, 1), _looting()], condition=_player_chance(0.35, 0.05)),
        _pool('sea_glass_shard', [_count(0, 2), _looting()]),
    ]))
    # captain: always his cutlass and a purse of gold, often a harpoon
    assets.loot_table(MOD, 'entities/corsair_captain', _entity_table('corsair_captain', [
        _pool('cutlass'),
        _pool('gold_coin', [_count(5, 9), _looting(2.0)]),
        _pool('minecraft:bone', [_count(1, 3), _looting()]),
        _pool('harpoon', condition=_player_chance(0.3, 0.05)),
    ]))


def tags():
    opt = lambda i: {'id': assets.rid(i), 'required': False}
    t = assets.tag
    for item in FOREIGN:
        t(MOD, 'item', f'raft:loot/encounters/{item}', [opt(item)])
    t(MOD, 'entity_type', 'minecraft:undead', ['raft:corsair_captain'])
    t(MOD, 'entity_type', 'minecraft:axolotl_always_hostiles', ['raft:corsair_captain'])
    t(MOD, 'entity_type', 'minecraft:aquatic', ['raft:reef_guardian'])
    t(MOD, 'entity_type', 'minecraft:sensitive_to_impaling', ['raft:reef_guardian'])
    t(MOD, 'entity_type', 'minecraft:can_breathe_under_water', ['raft:reef_guardian', 'raft:corsair_captain'])


def sounds():
    s = 'subtitles.raft.'
    rng = lambda base, n, start=1: [f'{base}{i}' for i in range(start, start + n)]
    data = {
        'entity.scavenger.ambient': _event(rng('mob/pillager/idle', 4), s + 'entity.scavenger.ambient', 0.9, 1.08),
        'entity.scavenger.hurt': _event(rng('mob/pillager/hurt', 3), s + 'entity.scavenger.hurt', 1.0, 1.08),
        'entity.scavenger.death': _event(rng('mob/pillager/death', 2), s + 'entity.scavenger.death', 1.0, 1.05),
        'entity.reef_guardian.ambient': _event(rng('mob/guardian/guardian_idle', 4), s + 'entity.reef_guardian.ambient', 0.9, 1.25),
        'entity.reef_guardian.hurt': _event(rng('mob/guardian/guardian_hit', 4), s + 'entity.reef_guardian.hurt', 1.0, 1.2),
        'entity.reef_guardian.death': _event(['mob/guardian/guardian_death'], s + 'entity.reef_guardian.death', 1.0, 1.2),
        'entity.corsair_captain.rally': _event(['event/raid/raidhorn_01', 'event/raid/raidhorn_02'], s + 'entity.corsair_captain.rally', 0.9, 0.85),
        'encounter.scavengers': _event(rng('mob/pillager/celebrate', 4), s + 'encounter.scavengers', 1.0, 1.1),
        'encounter.deep': _event(rng('mob/drowned/idle', 3), s + 'encounter.deep', 1.0, 0.6),
        'encounter.corsairs': _event(['event/raid/raidhorn_03', 'event/raid/raidhorn_04'], s + 'encounter.corsairs', 0.8, 1.0),
        'encounter.reef': _event(rng('mob/guardian/elder_idle', 4), s + 'encounter.reef', 0.9, 1.3),
        'encounter.forge': _event(rng('mob/blaze/breathe', 4), s + 'encounter.forge', 1.0, 0.75),
    }
    assets.sounds(MOD, data)


def build():
    item_models()
    lang()
    loot()
    tags()
    sounds()
    assets.flush(MOD)
