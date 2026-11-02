"""JSON of the marine module: item models, loot tables (26.3 format), recipes, tags, lang and the sounds.json part."""
from collections import OrderedDict

from bclib import assets

M = 'marine'

CREATURES = ['hammerhead_shark', 'whale', 'manta_ray', 'jellyfish', 'anglerfish', 'moray_eel']
ITEMS = ['jelly_blob', 'glow_jelly', 'angler_lure', 'eel_fillet', 'cooked_eel_fillet']


# ------------------------------------------------------------------------------------------------- loot helpers
def _uniform(lo, hi):
    return {'type': 'minecraft:uniform', 'max': hi, 'min': lo}


def _looting(hi=1.0):
    return {'type': 'minecraft:enchanted_count_increase', 'count': _uniform(0.0, hi), 'enchantment': 'minecraft:looting'}


SMELT_IF_BURNING = {
    'type': 'minecraft:furnace_smelt',
    'condition': {'type': 'minecraft:any_of', 'terms': [
        {'type': 'minecraft:entity_properties', 'entity': 'this', 'predicate': {'minecraft:flags': {'is_on_fire': True}}},
        {'type': 'minecraft:entity_properties', 'entity': 'direct_attacker', 'predicate': {'minecraft:equipment': {
            'mainhand': {'predicates': {'minecraft:enchantments': [{'enchantments': '#minecraft:smelts_loot'}]}}}}}]}}

KILLED_BY_PLAYER = {'type': 'minecraft:killed_by_player'}


def _entry(item, lo=None, hi=None, smelt=False, looting=True):
    mods = []
    if lo is not None:
        mods.append({'type': 'minecraft:set_count', 'count': _uniform(lo, hi)})
    if looting:
        mods.append(_looting())
    if smelt:
        mods.append(SMELT_IF_BURNING)
    e = OrderedDict(type='minecraft:item')
    if mods:
        e['modifier'] = mods if len(mods) > 1 else mods[0]
    e['name'] = assets.rid(item)
    return e


def _pool(entry, condition=None):
    p = OrderedDict()
    if condition:
        p['condition'] = condition
    p['entries'] = [entry]
    p['rolls'] = 1
    return p


def _table(name, pools):
    assets.loot_table(M, 'entities/' + name, OrderedDict(
        type='minecraft:entity', pools=pools, random_sequence='raft:entities/' + name))


def loot():
    _table('hammerhead_shark', [_pool(_entry('shark_tooth', 1, 3)), _pool(_entry('shark_meat', 1, 2, smelt=True))])
    _table('whale', [])                                       # a friend: no drops
    _table('manta_ray', [_pool(_entry('minecraft:leather', 0, 1))])
    _table('jellyfish', [_pool(_entry('jelly_blob', 1, 2))])
    _table('jellyfish_glow', [_pool(_entry('glow_jelly', 1, 2))])
    _table('anglerfish', [_pool(_entry('angler_lure', looting=False)),
                          _pool(_entry('minecraft:cod', 0, 1, smelt=True))])
    _table('moray_eel', [_pool(_entry('eel_fillet', 1, 2, smelt=True))])


# ------------------------------------------------------------------------------------------------- items
def item_models():
    for name in ITEMS + [c + '_spawn_egg' for c in CREATURES]:
        assets.flat_item(M, name)


# ------------------------------------------------------------------------------------------------- recipes
def recipes():
    for kind, time in (('smelting', 200), ('smoking', 100), ('campfire_cooking', 600)):
        assets.recipe_cooking(M, 'cooked_eel_fillet_from_' + kind, 'eel_fillet', 'cooked_eel_fillet', xp=0.35, time=time,
                              kind=kind, category='food')
    assets.recipe_shapeless(M, 'slime_ball_from_jelly_blob', ['jelly_blob', 'jelly_blob'], 'minecraft:slime_ball')
    assets.recipe_shapeless(M, 'glow_ink_sac_from_glow_jelly', ['glow_jelly'], 'minecraft:glow_ink_sac')
    assets.recipe_shaped(M, 'sea_lantern_from_angler_lure', [' S ', 'SLS', ' S '],
                         {'S': 'minecraft:prismarine_shard', 'L': 'angler_lure'}, 'minecraft:sea_lantern', category='building')


# ------------------------------------------------------------------------------------------------- tags
def tags():
    assets.tag(M, 'item', 'minecraft:fishes', ['eel_fillet', 'cooked_eel_fillet'])
    assets.tag(M, 'item', 'c:foods/raw_fish', ['eel_fillet'])
    assets.tag(M, 'item', 'c:foods/cooked_fish', ['cooked_eel_fillet'])
    assets.tag(M, 'item', 'c:foods', ['eel_fillet', 'cooked_eel_fillet'])
    sea = ['shark'] + CREATURES
    assets.tag(M, 'entity_type', 'minecraft:aquatic', sea)
    assets.tag(M, 'entity_type', 'minecraft:can_breathe_under_water', sea)
    assets.tag(M, 'entity_type', 'raft:sea_creatures', sea)


# ------------------------------------------------------------------------------------------------- lang
LANG = {
    'itemgroup.raft.creatures': 'Beach Craft: Creatures',
    'entity.raft.hammerhead_shark': 'Hammerhead Shark',
    'entity.raft.whale': 'Whale',
    'entity.raft.manta_ray': 'Manta Ray',
    'entity.raft.jellyfish': 'Jellyfish',
    'entity.raft.anglerfish': 'Anglerfish',
    'entity.raft.moray_eel': 'Moray Eel',
    'item.raft.hammerhead_shark_spawn_egg': 'Hammerhead Shark Spawn Egg',
    'item.raft.whale_spawn_egg': 'Whale Spawn Egg',
    'item.raft.manta_ray_spawn_egg': 'Manta Ray Spawn Egg',
    'item.raft.jellyfish_spawn_egg': 'Jellyfish Spawn Egg',
    'item.raft.anglerfish_spawn_egg': 'Anglerfish Spawn Egg',
    'item.raft.moray_eel_spawn_egg': 'Moray Eel Spawn Egg',
    'item.raft.jelly_blob': 'Jelly Blob',
    'item.raft.glow_jelly': 'Glow Jelly',
    'item.raft.angler_lure': 'Angler Lure',
    'item.raft.eel_fillet': 'Raw Eel Fillet',
    'item.raft.cooked_eel_fillet': 'Cooked Eel Fillet',
    'tooltip.raft.jelly_blob': 'Wobbly and a little sticky.',
    'tooltip.raft.glow_jelly': 'It still glows softly.',
    'tooltip.raft.angler_lure': 'A lure that never stops shining.',
    'gamerule.raft.shark_bites': 'Sharks bite rafts',
    'gamerule.raft.shark_bites.description': 'Sharks circle rafts that players stand on and bite through the edge foundations. Reinforced foundations are safe.',
    'subtitles.raft.entity.whale.song': 'Whale sings',
    'subtitles.raft.entity.whale.spout': 'Whale spouts',
}


def lang():
    assets.lang(M, LANG)


# ------------------------------------------------------------------------------------------------- sounds
def sounds(song_files, spout_files):
    assets.sounds(M, OrderedDict([
        ('entity.whale.song', {'sounds': [{'name': 'raft:' + f, 'stream': True} for f in song_files],
                               'subtitle': 'subtitles.raft.entity.whale.song'}),
        ('entity.whale.spout', {'sounds': ['raft:' + f for f in spout_files],
                                'subtitle': 'subtitles.raft.entity.whale.spout'}),
    ]))


def generate(song_files, spout_files):
    loot()
    item_models()
    recipes()
    tags()
    lang()
    sounds(song_files, spout_files)
