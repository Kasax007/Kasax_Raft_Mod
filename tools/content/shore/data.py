"""JSON data of the shore module: item models, lang, loot tables, recipes, tags and the sounds.json fragment."""
from bclib import assets

MOD = 'shore'

ENTITIES = {
    'crab': 'Crab',
    'ember_crab': 'Ember Crab',
    'seagull': 'Seagull',
    'penguin': 'Penguin',
    'sea_otter': 'Sea Otter',
    'drowned_corsair': 'Drowned Corsair',
}


def item_models():
    for name in ('crab_claw', 'cooked_crab_claw', 'ember_shell'):
        assets.flat_item(MOD, name)
    assets.handheld_item(MOD, 'cutlass')
    for e in ENTITIES:
        assets.spawn_egg(MOD, f'{e}_spawn_egg')


def lang():
    entries = {}
    for e, title in ENTITIES.items():
        entries[f'entity.raft.{e}'] = title
        entries[f'item.raft.{e}_spawn_egg'] = f'{title} Spawn Egg'
    entries.update({
        'item.raft.crab_claw': 'Crab Claw',
        'item.raft.cooked_crab_claw': 'Cooked Crab Claw',
        'item.raft.ember_shell': 'Ember Shell',
        'item.raft.cutlass': 'Cutlass',
        'tooltip.raft.ember_shell': 'Still warm from the volcano',
        'tooltip.raft.cutlass': 'A quick pirate blade',
        'subtitles.raft.entity.crab.ambient': 'Crab clicks',
        'subtitles.raft.entity.crab.hurt': 'Crab hurts',
        'subtitles.raft.entity.crab.death': 'Crab dies',
        'subtitles.raft.entity.crab.pinch': 'Crab pinches',
        'subtitles.raft.entity.crab.burrow': 'Crab digs in',
        'subtitles.raft.entity.ember_crab.ambient': 'Ember Crab hisses',
        'subtitles.raft.entity.ember_crab.hurt': 'Ember Crab hurts',
        'subtitles.raft.entity.ember_crab.death': 'Ember Crab dies',
        'subtitles.raft.entity.seagull.ambient': 'Seagull cries',
        'subtitles.raft.entity.seagull.hurt': 'Seagull hurts',
        'subtitles.raft.entity.seagull.death': 'Seagull dies',
        'subtitles.raft.entity.seagull.flap': 'Wings flap',
        'subtitles.raft.entity.seagull.peck': 'Seagull pecks',
        'subtitles.raft.entity.seagull.steal': 'Seagull snatches food',
        'subtitles.raft.entity.penguin.ambient': 'Penguin brays',
        'subtitles.raft.entity.penguin.hurt': 'Penguin hurts',
        'subtitles.raft.entity.penguin.death': 'Penguin dies',
        'subtitles.raft.entity.penguin.slide': 'Penguin slides',
        'subtitles.raft.entity.sea_otter.ambient': 'Sea Otter squeaks',
        'subtitles.raft.entity.sea_otter.hurt': 'Sea Otter hurts',
        'subtitles.raft.entity.sea_otter.death': 'Sea Otter dies',
        'subtitles.raft.entity.sea_otter.crack': 'Sea Otter cracks a shell',
        'subtitles.raft.entity.sea_otter.gift': 'Sea Otter brings a gift',
        'subtitles.raft.entity.drowned_corsair.ambient': 'Corsair growls',
    })
    assets.lang(MOD, entries)


# ---------------------------------------------------------------------------------------------- loot
def _count(lo, hi):
    return {'type': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'max': float(hi), 'min': float(lo)}}


def _looting(hi=1.0):
    return {'type': 'minecraft:enchanted_count_increase',
            'count': {'type': 'minecraft:uniform', 'max': hi, 'min': 0.0},
            'enchantment': 'minecraft:looting'}


SMELT_IF_BURNING = {
    'type': 'minecraft:furnace_smelt',
    'condition': {
        'type': 'minecraft:any_of',
        'terms': [
            {'type': 'minecraft:entity_properties', 'entity': 'this',
             'predicate': {'minecraft:flags': {'is_on_fire': True}}},
            {'type': 'minecraft:entity_properties', 'entity': 'direct_attacker',
             'predicate': {'minecraft:equipment': {'mainhand': {'predicates': {
                 'minecraft:enchantments': [{'enchantments': '#minecraft:smelts_loot'}]}}}}},
        ]},
}


def _entity_table(name, pools):
    return {'type': 'minecraft:entity', 'pools': pools, 'random_sequence': f'raft:entities/{name}'}


# items of other 3.0 modules: in 26.3 one unknown item id in a loot table stops the world from loading, so they are
# named through one-item tags with an optional entry (same convention as the story module, spec section 14)
FOREIGN = ('seashell', 'gold_coin')


def _pool(item, modifiers=None, condition=None):
    if item in FOREIGN:
        entry = {'type': 'minecraft:tag', 'expand': False, 'items': f'#raft:loot/{item}'}
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


def _player_chance(chance, per_level):
    return {'type': 'minecraft:all_of', 'terms': [
        {'type': 'minecraft:killed_by_player'},
        {'type': 'minecraft:random_chance_with_enchanted_bonus',
         'enchanted_chance': {'type': 'minecraft:linear', 'base': chance + per_level, 'per_level_above_first': per_level},
         'enchantment': 'minecraft:looting', 'unenchanted_chance': chance}]}


def loot():
    assets.loot_table(MOD, 'entities/crab', _entity_table('crab', [
        _pool('crab_claw', [_count(1, 2), SMELT_IF_BURNING, _looting()]),
        _pool('seashell', condition={'type': 'minecraft:random_chance', 'chance': 0.08}),
    ]))
    assets.loot_table(MOD, 'entities/ember_crab', _entity_table('ember_crab', [
        _pool('crab_claw', [_count(0, 1), SMELT_IF_BURNING, _looting()]),
        _pool('ember_shell', [_count(0, 1), _looting()]),
    ]))
    assets.loot_table(MOD, 'entities/seagull', _entity_table('seagull', [
        _pool('minecraft:feather', [_count(0, 2), _looting()]),
    ]))
    assets.loot_table(MOD, 'entities/penguin', _entity_table('penguin', [
        _pool('minecraft:feather', [_count(0, 2), _looting()]),
    ]))
    # the otter is a friend: it drops nothing
    assets.loot_table(MOD, 'entities/sea_otter', _entity_table('sea_otter', []))
    assets.loot_table(MOD, 'entities/drowned_corsair', _entity_table('drowned_corsair', [
        _pool('gold_coin', [_count(0, 3), _looting()]),
        _pool('minecraft:bone', [_count(0, 2), _looting()]),
        _pool('cutlass', condition=_player_chance(0.025, 0.01)),
    ]))


def recipes():
    assets.recipe_cooking(MOD, 'cooked_crab_claw', 'crab_claw', 'cooked_crab_claw', xp=0.35, time=200,
                          kind='smelting', category='food')
    assets.recipe_cooking(MOD, 'cooked_crab_claw_from_smoking', 'crab_claw', 'cooked_crab_claw', xp=0.35, time=100,
                          kind='smoking', category='food')
    assets.recipe_cooking(MOD, 'cooked_crab_claw_from_campfire_cooking', 'crab_claw', 'cooked_crab_claw', xp=0.35,
                          time=600, kind='campfire_cooking', category='food')
    path = assets.recipe_shaped(MOD, 'cutlass', ['  I', ' I ', 'T  '],
                                {'I': '#minecraft:iron_tool_materials', 'T': 'teak_planks'}, 'cutlass',
                                category='equipment')
    # teak planks come from the building module: only load the recipe when they exist
    _add_load_condition(path, ['raft:teak_planks'])


def _add_load_condition(path, items):
    import json
    with open(path) as f:
        d = json.load(f)
    d['fabric:load_conditions'] = [{'condition': 'fabric:registry_contains', 'registry': 'minecraft:item',
                                    'values': items}]
    with open(path, 'w') as f:
        json.dump(d, f, indent=2)
        f.write('\n')


def tags():
    opt = lambda i: {'id': assets.rid(i), 'required': False}
    t = assets.tag
    t(MOD, 'block', 'raft:crab_burrowable', ['#minecraft:sand', 'minecraft:gravel',
                                              opt('shell_sand'), opt('black_sand'), opt('volcanic_ash')])
    t(MOD, 'block', 'raft:crab_spawnable_on', ['#minecraft:sand', 'minecraft:gravel', 'minecraft:mud',
                                                'minecraft:grass_block', 'minecraft:coarse_dirt',
                                                'minecraft:muddy_mangrove_roots',
                                                opt('shell_sand'), opt('black_sand')])
    t(MOD, 'block', 'raft:seagull_spawnable_on', ['#minecraft:sand', '#minecraft:dirt', 'minecraft:grass_block',
                                                   'minecraft:stone', 'minecraft:gravel', 'minecraft:mud',
                                                   opt('shell_sand'), opt('black_sand'), opt('pumice'),
                                                   opt('volcanic_ash')])
    t(MOD, 'block', 'raft:seagull_perches', ['raft:raft_foundation', 'raft:reinforced_foundation', '#minecraft:fences',
                                              '#minecraft:logs', '#minecraft:walls', 'raft:palm_thatch',
                                              'raft:palm_thatch_slab', 'raft:rope_coil',
                                              opt('weathered_planks'), opt('weathered_planks_slab'),
                                              opt('teak_planks'), opt('rope_railing'), opt('steel_railing'),
                                              opt('buoy_red'), opt('buoy_green'), opt('catwalk')])
    t(MOD, 'block', 'raft:penguin_slides_on', ['#minecraft:ice', 'minecraft:snow_block', 'minecraft:snow',
                                                'minecraft:powder_snow'])
    t(MOD, 'block', 'raft:penguin_spawnable_on', ['#raft:penguin_slides_on', 'minecraft:stone', 'minecraft:gravel',
                                                   '#minecraft:sand'])
    t(MOD, 'item', 'raft:crab_food', ['minecraft:kelp', 'minecraft:dried_kelp'])
    t(MOD, 'item', 'raft:penguin_food', ['minecraft:cod'])
    t(MOD, 'item', 'raft:sea_otter_treats', [opt('seashell')])
    for item in FOREIGN:
        t(MOD, 'item', f'raft:loot/{item}', [opt(item)])
    t(MOD, 'item', 'minecraft:swords', ['raft:cutlass'])
    t(MOD, 'entity_type', 'minecraft:undead', ['raft:drowned_corsair'])
    t(MOD, 'entity_type', 'minecraft:axolotl_always_hostiles', ['raft:drowned_corsair'])
    t(MOD, 'entity_type', 'minecraft:arthropod', ['raft:crab', 'raft:ember_crab'])
    t(MOD, 'entity_type', 'minecraft:sensitive_to_bane_of_arthropods', ['raft:crab', 'raft:ember_crab'])
    t(MOD, 'entity_type', 'minecraft:can_breathe_under_water', ['raft:crab'])
    t(MOD, 'entity_type', 'minecraft:fall_damage_immune', ['raft:seagull'])
    t(MOD, 'entity_type', 'minecraft:freeze_immune_entity_types', ['raft:penguin'])
    t(MOD, 'worldgen/structure', 'raft:corsair_haunts', [opt('pirate_cove'), opt('wreck_galleon')])


def _snd(name, volume=None, pitch=None, weight=None):
    # vanilla recordings: the namespace must be given, a bare name would point into assets/raft/sounds
    name = name if ':' in name else 'minecraft:' + name
    if volume is None and pitch is None and weight is None:
        return name
    d = {'name': name}
    if volume is not None:
        d['volume'] = volume
    if pitch is not None:
        d['pitch'] = pitch
    if weight is not None:
        d['weight'] = weight
    return d


def _event(files, subtitle=None, volume=None, pitch=None):
    sounds = []
    for f in files:
        if isinstance(f, tuple):
            sounds.append(_snd(*f))
        else:
            sounds.append(_snd(f, volume, pitch))
    e = {'sounds': sounds}
    if subtitle:
        e['subtitle'] = subtitle
    return e


def sounds():
    s = 'subtitles.raft.entity.'
    rng = lambda base, n, start=1: [f'{base}{i}' for i in range(start, start + n)]
    data = {
        'entity.crab.ambient': _event(rng('mob/silverfish/step', 4), s + 'crab.ambient', 0.5, 0.75),
        'entity.crab.hurt': _event(rng('mob/turtle/hurt', 5), s + 'crab.hurt', 0.8, 1.45),
        'entity.crab.death': _event(rng('mob/turtle/death', 3), s + 'crab.death', 0.8, 1.45),
        'entity.crab.step': _event(rng('mob/silverfish/step', 4), None, 0.35, 1.3),
        'entity.crab.pinch': _event(['mob/armadillo/scute_drop1', 'mob/armadillo/scute_drop2'], s + 'crab.pinch', 0.9, 1.55),
        'entity.crab.burrow': _event(rng('dig/sand', 4), s + 'crab.burrow', 0.7, 1.2),
        'entity.ember_crab.ambient': _event(['random/fizz'] + rng('mob/silverfish/step', 2), s + 'ember_crab.ambient', 0.4, 0.8),
        'entity.ember_crab.hurt': _event(rng('mob/turtle/hurt', 5), s + 'ember_crab.hurt', 0.9, 1.05),
        'entity.ember_crab.death': _event(rng('mob/turtle/death', 3), s + 'ember_crab.death', 0.9, 1.0),
        'entity.seagull.ambient': _event(rng('mob/parrot/idle', 6), s + 'seagull.ambient', 0.9, 0.78),
        'entity.seagull.hurt': _event(['mob/parrot/hurt1', 'mob/parrot/hurt2'], s + 'seagull.hurt', 0.9, 0.85),
        'entity.seagull.death': _event(rng('mob/parrot/death', 4), s + 'seagull.death', 0.9, 0.85),
        'entity.seagull.flap': _event(rng('mob/parrot/fly', 6), s + 'seagull.flap', 0.6, 0.8),
        'entity.seagull.peck': _event(rng('mob/parrot/eat', 3), s + 'seagull.peck', 0.8, 0.9),
        'entity.seagull.steal': _event(rng('mob/parrot/idle', 3), s + 'seagull.steal', 1.0, 1.05),
        'entity.penguin.ambient': _event(rng('mob/horse/donkey/idle', 3), s + 'penguin.ambient', 0.5, 1.75),
        'entity.penguin.hurt': _event(rng('mob/horse/donkey/hit', 3), s + 'penguin.hurt', 0.6, 1.8),
        'entity.penguin.death': _event(['mob/horse/donkey/death'], s + 'penguin.death', 0.6, 1.8),
        'entity.penguin.step': _event(rng('mob/frog/step', 4), None, 0.5, 1.35),
        'entity.penguin.slide': _event(rng('step/snow', 4), s + 'penguin.slide', 1.0, 0.7),
        'entity.sea_otter.ambient': _event(rng('mob/axolotl/idle_air', 5), s + 'sea_otter.ambient', 0.8, 1.25),
        'entity.sea_otter.hurt': _event(rng('mob/fox/hurt', 4), s + 'sea_otter.hurt', 0.8, 1.2),
        'entity.sea_otter.death': _event(['mob/fox/death1', 'mob/fox/death2'], s + 'sea_otter.death', 0.8, 1.2),
        'entity.sea_otter.crack': _event(rng('mob/turtle/egg/egg_crack', 5), s + 'sea_otter.crack', 0.8, 1.3),
        'entity.sea_otter.gift': _event(['random/pop'], s + 'sea_otter.gift', 0.6, 1.2),
        'entity.drowned_corsair.ambient': _event([('mob/drowned/idle1', 0.9, 0.8), ('mob/drowned/idle2', 0.9, 0.8),
                                                  ('mob/drowned/idle3', 0.9, 0.8), ('mob/zombie/say1', 0.7, 0.75),
                                                  ('mob/zombie/say2', 0.7, 0.75)], s + 'drowned_corsair.ambient'),
    }
    # seagull calls, crab clicks and the ember crab's sizzle are synthesised by the audio module (tools/content/audio)
    from content.audio import events
    assets.sounds(MOD, events.apply(MOD, data))


def build():
    item_models()
    lang()
    loot()
    recipes()
    tags()
    sounds()
    assets.flush(MOD)
