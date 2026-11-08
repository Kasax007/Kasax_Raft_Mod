"""Data of the story module (26.3 formats): loot tables (section 9.3), structure/item/entity/damage tags,
advancements (9.6), recipes and recipe unlocks, block loot, mining tags, and the lang of all story texts."""
import os

from bclib import assets as A
from bclib.paths import VANILLA

from content.story import texts as T

M = 'story'
NS = 'raft'
rid = A.rid

# ------------------------------------------------------------------------------------------------- known item ids
# Items that exist (vanilla + Beach Craft 2.0) or are promised by the spec (other 3.0 modules). Every item a loot
# table names is checked against this list so a typo does not break a whole table at load time.
RAFT_2 = """ancient_energy ancient_broken_ring ancient_crystal driftwood_log driftwood_planks palm_log palm_planks
palm_leaves palm_sapling driftwood_sapling titanium_ingot titanium_nugget raw_titanium plastic circuit_board coconut
pina_colada coconut_milk net rope scrap_metal shark_tooth shark_meat cooked_shark_meat fresh_water receiver plastic_hook
paddle coconut_lantern rope_ladder rope_coil sail_canvas raft_flag some_game_music_music_disc lofi_beat_music_disc
crystal_castles_music_disc metal_detector titanium_block""".split()
RAFT_3 = """pineapple pineapple_crown pearl seashell sea_glass_shard urchin_spine glow_kelp crab_claw cooked_crab_claw
ember_shell cutlass gold_coin cannonball jelly_blob glow_jelly angler_lure eel_fillet cooked_eel_fillet diving_helmet
oxygen_tank wetsuit flippers gold_coin_pile rum_bottles ship_lantern jelly_lamp seashell_lamp hibiscus
seafarers_almanac""".split()
# 3.4 (barrel loot): items of the food, fishing and farming modules
RAFT_34 = """fish_bait golden_lure chili_seeds rice sweet_potato banana banana_shoot sea_grapes""".split()
RAFT_STORY = """expedition_journal message_in_a_bottle supply_barrel coral_sigil pearl_sigil storm_sigil ember_sigil
tidewarden_key""".split()
_known = None


def iid(name):
    """Item id: 'raft:x' as given, story items by plain name, everything else plain = vanilla."""
    if ':' in name:
        return name
    return ('raft:' if name in RAFT_STORY else 'minecraft:') + name


def known(item):
    global _known
    if _known is None:
        _known = {'minecraft:' + f[:-5] for f in os.listdir(os.path.join(VANILLA, 'assets', 'minecraft', 'items'))}
        _known |= {rid(x) for x in RAFT_2 + RAFT_3 + RAFT_34 + RAFT_STORY}
    return iid(item) in _known


# ------------------------------------------------------------------------------------------------- loot helpers
def count(lo, hi=None):
    c = lo if hi is None or hi == lo else {'type': 'minecraft:uniform', 'min': lo, 'max': hi}
    return {'type': 'minecraft:set_count', 'count': c}


def item(name, lo=1, hi=None, weight=None, mods=None, chance=None):
    """A loot entry. Items of other 3.0 modules are written as a tag entry over a one-item tag with an optional
    entry ({"id": ..., "required": false}): in 26.3 one unknown item id in a loot table stops the whole world from
    loading, an empty optional tag just drops nothing. So every story chest always loads, with or without them."""
    if not known(name):
        raise SystemExit(f'story loot: unknown item {name}')
    full = iid(name)
    own = {rid(x) for x in RAFT_2 + RAFT_STORY}
    cross = full.startswith(NS + ':') and full not in own
    e = {'type': 'minecraft:tag' if cross else 'minecraft:item'}
    if chance is not None:
        e['condition'] = {'type': 'minecraft:random_chance', 'chance': chance}
    if cross:
        e['expand'] = False
        e['items'] = '#' + NS + ':loot/' + full.split(':', 1)[1]
        A.tag(M, 'item', 'loot/' + full.split(':', 1)[1], [{'id': full, 'required': False}])
    m = list(mods or [])
    if hi is not None or lo != 1:
        m.insert(0, count(lo, hi))
    if m:
        e['modifier'] = m[0] if len(m) == 1 else m
    if not cross:
        e['name'] = full
    if weight:
        e['weight'] = weight
    return e


def empty(weight):
    return {'type': 'minecraft:empty', 'weight': weight}


def nested(table, weight=None, chance=None):
    e = {'type': 'minecraft:loot_table'}
    if chance is not None:
        e['condition'] = {'type': 'minecraft:random_chance', 'chance': chance}
    e['value'] = rid(table)
    if weight:
        e['weight'] = weight
    return e


def book_mods(book_id, title, author, pages):
    return [
        {'type': 'minecraft:set_book_cover', 'author': author, 'generation': 0, 'title': title},
        {'type': 'minecraft:set_written_book_pages', 'mode': 'replace_all',
         'pages': [{'translate': f'book.{NS}.{book_id}.{i + 1}'} for i in range(len(pages))]},
        {'type': 'minecraft:set_custom_data', 'tag': '{raft_log:"%s"}' % book_id},
    ]


def book(book_id, weight=None):
    title, author, pages = T.BOOKS.get(book_id) or T.LETTERS[book_id]
    e = {'type': 'minecraft:item', 'modifier': book_mods(book_id, title, author, pages),
         'name': 'minecraft:written_book'}
    if weight:
        e['weight'] = weight
    return e


MAP_TAG = {'lighthouse': 'on_lighthouse_maps', 'saltmere': 'on_saltmere_maps', 'helix': 'on_helix_maps',
           'port_solace': 'on_port_solace_maps', 'ashen_forge': 'on_ashen_forge_maps',
           'coral_temple': 'on_coral_temple_maps', 'abyssal_sanctum': 'on_abyssal_sanctum_maps',
           'pirate': 'on_pirate_maps'}
MAP_STRUCTURE = {'lighthouse': 'old_lighthouse', 'saltmere': 'saltmere_village', 'helix': 'helix_station',
                 'port_solace': 'port_solace', 'ashen_forge': 'ashen_forge', 'coral_temple': 'coral_temple',
                 'abyssal_sanctum': 'abyssal_sanctum', 'pirate': 'pirate_cove'}


def chart(place, chance=None):
    """Exploration map to a story place (discarded when no such place is found, like vanilla maps)."""
    e = {'type': 'minecraft:item'}
    if chance is not None:
        e['condition'] = {'type': 'minecraft:random_chance', 'chance': chance}
    e['modifier'] = [
        {'type': 'minecraft:set_name', 'name': {'translate': f'filled_map.{NS}.{place}'}, 'target': 'item_name'},
        {'type': 'minecraft:exploration_map', 'decoration': 'minecraft:red_x' if place == 'pirate' else
         'minecraft:target_x', 'destination': f'#{NS}:{MAP_TAG[place]}', 'search_radius': 120,
         'skip_existing_chunks': False, 'zoom': 2},
        {'type': 'minecraft:filtered', 'item_filter': {'predicates': {'minecraft:map_id': {}}},
         'on_fail': {'type': 'minecraft:discard'}},
    ]
    e['name'] = 'minecraft:filled_map'
    return e


def pool(entries, rolls=1, bonus=None):
    p = {'entries': entries, 'rolls': rolls if not isinstance(rolls, tuple) else
         {'type': 'minecraft:uniform', 'min': rolls[0], 'max': rolls[1]}}
    if bonus:
        p['bonus_rolls'] = bonus
    return p


def cross_module_items(obj):
    """raft: item ids in a JSON tree that other 3.0 modules register (not 2.0 items, not story items)."""
    own = {rid(x) for x in RAFT_2 + RAFT_STORY}
    found = []

    def walk(o):
        if isinstance(o, dict):
            if o.get('type') == 'minecraft:item' and isinstance(o.get('name'), str):
                n = o['name']
                if n.startswith(NS + ':') and n not in own and n not in found:
                    found.append(n)
            for v in o.values():
                walk(v)
        elif isinstance(o, list):
            for v in o:
                walk(v)
    walk(obj)
    return found


def load_condition(items):
    """Fabric load condition: the file only loads when these items exist. In 26.3 one loot table that names an
    unknown item stops the whole world from loading, so tables that use items of other modules carry this guard."""
    return [{'condition': 'fabric:registry_contains', 'registry': 'minecraft:item', 'values': items}]


def table(path, pools, kind='minecraft:chest'):
    d = {}
    cross = cross_module_items(pools)
    if cross:
        d['fabric:load_conditions'] = load_condition(cross)
    d.update({'type': kind, 'pools': pools, 'random_sequence': f'{NS}:{path}'})
    A.write_json(M, f'data/{NS}/loot_table/{path}.json', d)


def letter_pool(chance):
    """A random lore letter (raft:story/bottle_letters) with the given chance."""
    return pool([nested('story/bottle_letters', chance=chance)])


# ------------------------------------------------------------------------------------------------- loot tables
def loot_tables():
    # ---- story chests (9.3)
    table('chests/story/castaway', [
        pool([book('marlow_note')]), pool([item('expedition_journal')]), pool([chart('lighthouse')]),
        pool([item('raft:seafarers_almanac')]), pool([item('bread', 3, 5)]), pool([item('raft:fresh_water', 2, 3)]), pool([item('raft:rope', 3, 6)]),
        pool([item('raft:paddle')]), pool([item('raft:plastic_hook')]),
        pool([item('raft:coconut', 1, 3, weight=3), item('cooked_cod', 1, 3, weight=3),
              item('raft:palm_sapling', 1, 2, weight=1), item('string', 2, 4, weight=2)], rolls=(1, 2))])
    table('chests/story/lighthouse', [
        pool([book('fenn_logbook')]), pool([chart('saltmere')]),
        pool([item('lantern', 1, 2, weight=4), item('candle', 2, 4, weight=4), item('torch', 4, 8, weight=5),
              item('raft:coconut_lantern', 1, 2, weight=3), item('glowstone_dust', 2, 5, weight=2),
              item('coal', 2, 5, weight=3)], rolls=(2, 3)),
        pool([item('raft:rope', 2, 5)]),
        pool([item('spyglass', weight=2), item('compass', weight=2), item('cooked_cod', 2, 4, weight=5),
              item('raft:fresh_water', 1, 2, weight=4), empty(4)], rolls=2)])
    table('chests/story/saltmere_elder', [
        pool([book('kalea_letters')]), pool([item('raft:diving_helmet')]), pool([chart('helix')]),
        pool([item('raft:seashell', 2, 4, weight=4), item('dried_kelp', 3, 6, weight=4),
              item('raft:pearl', 1, 2, weight=2), item('cooked_salmon', 1, 3, weight=3)], rolls=(1, 2))])
    table('chests/story/saltmere_shrine', [
        pool([item('raft:coral_sigil')]), pool([item('raft:seashell', 2, 5)]), pool([item('raft:pearl', 1, 3)]),
        pool([item('prismarine_crystals', 1, 3, weight=3), item('candle', 1, 3, weight=2), empty(3)])])
    table('chests/story/helix_lab', [
        pool([book('voss_journal')]), pool([item('raft:oxygen_tank')]), pool([chart('port_solace')]),
        pool([item('raft:circuit_board', 1, 2, weight=4), item('paper', 2, 6, weight=4),
              item('glass_bottle', 1, 3, weight=3), item('glow_ink_sac', 1, 3, weight=3)], rolls=(1, 2))])
    table('chests/story/helix_vault', [
        pool([item('raft:pearl_sigil')]), pool([item('raft:circuit_board', 2, 4)]),
        pool([item('raft:ancient_energy', weight=2), item('raft:titanium_ingot', 1, 3, weight=3),
              item('redstone', 3, 8, weight=3)])])
    table('chests/story/port_solace_hall', [
        pool([book('crane_broadcast')]), pool([chart('ashen_forge')]),
        pool([item('paper', 2, 6, weight=4), item('gold_nugget', 3, 9, weight=4), item('clock', weight=2),
              item('emerald', 1, 3, weight=3), item('book', 1, 2, weight=2)], rolls=(1, 3))])
    table('chests/story/port_solace_vault', [
        pool([item('raft:storm_sigil')]), pool([item('gold_ingot', 2, 5)]), pool([item('raft:titanium_ingot', 2, 4)]),
        pool([item('raft:gold_coin', 3, 8, weight=3), item('emerald', 2, 4, weight=2), empty(2)])])
    table('chests/story/ashen_forge', [
        pool([book('forge_tablet')]), pool([chart('coral_temple')]),
        pool([item('obsidian', 1, 3, weight=3), item('magma_cream', 1, 3, weight=3), item('iron_ingot', 2, 5, weight=4),
              item('raft:ember_shell', 1, 2, weight=3), item('coal', 3, 8, weight=4)], rolls=(1, 3))])
    table('chests/story/ashen_vault', [
        pool([item('raft:ember_sigil')]),
        pool([item('gold_ingot', 1, 4, weight=3), item('magma_cream', 1, 2, weight=2), empty(2)])])
    table('chests/story/coral_temple', [
        pool([book('tidewarden_prophecy')]), pool([chart('abyssal_sanctum')]),
        pool([item('prismarine_shard', 4, 10)]), pool([item('prismarine_crystals', 2, 6)]),
        pool([item('glow_ink_sac', 2, 5)]),
        pool([item('raft:pearl', 1, 2, weight=2), item('nautilus_shell', weight=1), empty(3)])])
    table('chests/story/pirate_captain', [
        pool([book('saltjaw_log')]), pool([item('raft:gold_coin', 4, 12)]), pool([item('raft:cutlass')]),
        pool([item('gold_ingot', 2, 6, weight=5), item('emerald', 2, 6, weight=4), item('diamond', 1, 2, weight=2),
              item('raft:ancient_broken_ring', weight=1), item('spyglass', weight=2)], rolls=(2, 3))])

    # ---- other chests
    table('chests/castaway_camp', [
        pool([item('bread', 1, 3, weight=4), item('raft:fresh_water', 1, 2, weight=4), item('raft:coconut', 1, 3, weight=5),
              item('raft:rope', 1, 4, weight=4), item('raft:plastic', 2, 5, weight=4), item('stick', 2, 6, weight=3),
              item('string', 1, 4, weight=3), item('raft:palm_planks', 2, 6, weight=3), item('cooked_cod', 1, 2, weight=3),
              item('raft:plastic_hook', weight=1)], rolls=(3, 6)),
        letter_pool(0.3)])
    table('chests/abandoned_raft', [
        pool([item('raft:driftwood_planks', 2, 6, weight=5), item('raft:plastic', 2, 6, weight=5),
              item('raft:scrap_metal', 1, 4, weight=4), item('raft:rope', 1, 4, weight=4),
              item('raft:fresh_water', 1, 2, weight=3), item('raft:cooked_shark_meat', 1, 2, weight=2),
              item('raft:shark_tooth', 1, 2, weight=2), item('raft:net', weight=1), item('raft:paddle', weight=1),
              item('raft:plastic_hook', weight=1)], rolls=(3, 6)),
        letter_pool(0.35)])
    table('chests/beach_shack', [
        pool([item('raft:pina_colada', 1, 2, weight=4), item('raft:coconut', 1, 4, weight=5),
              item('raft:coconut_milk', 1, 2, weight=3), item('raft:pineapple', 1, 3, weight=4),
              item('melon_slice', 2, 5, weight=3), item('sugar', 1, 4, weight=3), item('glass_bottle', 1, 3, weight=3),
              item('raft:hibiscus', 1, 3, weight=2), item('raft:seashell', 1, 4, weight=3),
              item('raft:lofi_beat_music_disc', weight=1)], rolls=(3, 6)),
        letter_pool(0.2)])
    table('chests/saltmere_house', [
        pool([item('cod', 1, 4, weight=5), item('salmon', 1, 3, weight=4), item('dried_kelp', 2, 6, weight=4),
              item('string', 2, 5, weight=3), item('fishing_rod', weight=1), item('raft:seashell', 1, 3, weight=3),
              item('raft:pearl', weight=1), item('raft:rope', 1, 3, weight=3), item('raft:net', weight=1),
              item('candle', 1, 2, weight=2), item('raft:fresh_water', 1, 2, weight=3)], rolls=(3, 6)),
        letter_pool(0.15)])
    table('chests/saltmere_market', [
        pool([item('emerald', 1, 3, weight=3), item('cod', 2, 5, weight=4), item('salmon', 1, 4, weight=4),
              item('raft:pineapple', 1, 3, weight=4), item('raft:coconut', 1, 3, weight=4),
              item('raft:seashell', 2, 5, weight=4), item('raft:sea_glass_shard', 1, 4, weight=3),
              item('raft:pearl', 1, 2, weight=2), item('kelp', 2, 8, weight=3), item('raft:crab_claw', 1, 3, weight=3)],
             rolls=(4, 7)),
        letter_pool(0.1)])
    table('chests/helix_storage', [
        pool([item('raft:circuit_board', 1, 3, weight=4), item('raft:scrap_metal', 2, 6, weight=5),
              item('raft:plastic', 2, 6, weight=4), item('iron_ingot', 1, 4, weight=4), item('glass', 2, 6, weight=3),
              item('copper_ingot', 2, 6, weight=4), item('redstone', 2, 6, weight=3),
              item('raft:titanium_nugget', 2, 6, weight=3), item('glow_ink_sac', 1, 3, weight=2),
              item('raft:oxygen_tank', weight=1)], rolls=(3, 6)),
        letter_pool(0.1)])
    table('chests/port_solace_office', [
        pool([item('paper', 2, 8, weight=5), item('book', 1, 3, weight=4), item('writable_book', weight=2),
              item('gold_nugget', 2, 7, weight=4), item('clock', weight=1), item('compass', weight=2),
              item('raft:circuit_board', 1, 2, weight=3), item('emerald', 1, 2, weight=2),
              item('ink_sac', 1, 3, weight=3)], rolls=(2, 5)),
        letter_pool(0.2)])
    table('chests/port_solace_apartment', [
        pool([item('bread', 1, 3, weight=4), item('cookie', 2, 6, weight=3), item('candle', 1, 3, weight=3),
              item('glass_bottle', 1, 3, weight=3), item('string', 1, 4, weight=3), item('clock', weight=1),
              item('emerald', 1, 2, weight=2), item('white_wool', 1, 3, weight=3), item('book', 1, 2, weight=2),
              item('raft:some_game_music_music_disc', weight=1)], rolls=(2, 5)),
        letter_pool(0.2)])
    table('chests/port_solace_shop', [
        pool([item('emerald', 1, 4, weight=4), item('gold_nugget', 3, 9, weight=4), item('apple', 2, 5, weight=4),
              item('bread', 2, 4, weight=4), item('cookie', 3, 8, weight=3), item('raft:coconut', 1, 3, weight=3),
              item('raft:pina_colada', 1, 2, weight=3), item('glass_bottle', 2, 4, weight=3),
              item('raft:crystal_castles_music_disc', weight=1)], rolls=(3, 6))])
    table('chests/ashen_storage', [
        pool([item('coal', 3, 10, weight=5), item('iron_ingot', 2, 6, weight=4), item('obsidian', 1, 3, weight=3),
              item('raft:titanium_ingot', 1, 3, weight=3), item('magma_cream', 1, 3, weight=3),
              item('raft:ember_shell', 1, 2, weight=3), item('blaze_powder', 1, 3, weight=2),
              item('raft:raw_titanium', 1, 4, weight=3)], rolls=(3, 6))])
    table('chests/coral_temple_offering', [
        pool([item('prismarine_shard', 3, 8, weight=5), item('prismarine_crystals', 2, 6, weight=4),
              item('raft:pearl', 1, 3, weight=4), item('gold_ingot', 1, 4, weight=4), item('emerald', 1, 3, weight=3),
              item('nautilus_shell', weight=2), item('raft:ancient_energy', weight=2),
              item('raft:ancient_broken_ring', weight=1), item('heart_of_the_sea', weight=1)], rolls=(3, 5))])
    table('chests/sanctum_reward', [
        pool([item('raft:ancient_crystal', 1, 2)]),
        pool([item('diamond', 2, 5)]),
        pool([item('gold_ingot', 4, 10, weight=4), item('emerald', 4, 10, weight=4), item('raft:titanium_ingot', 3, 8, weight=4),
              item('nautilus_shell', 1, 3, weight=3), item('prismarine_crystals', 4, 10, weight=3)], rolls=(3, 5)),
        pool([item('heart_of_the_sea', weight=3), item('trident', weight=1),
              item('book', weight=4, mods=[{'type': 'minecraft:enchant_randomly', 'options': '#minecraft:on_random_loot'}])],
             rolls=(1, 2))])
    table('chests/wreck_galleon', [
        pool([item('raft:gold_coin', 2, 9, weight=6), item('gold_ingot', 1, 3, weight=3), item('emerald', 1, 3, weight=3),
              item('raft:cannonball', 2, 6, weight=4), item('gunpowder', 2, 6, weight=4), item('raft:rope', 2, 5, weight=4),
              item('raft:rum_bottles', 1, 2, weight=2), item('raft:cutlass', weight=1), item('spyglass', weight=1),
              item('bone', 1, 4, weight=3)], rolls=(3, 6)),
        pool([chart('pirate', chance=0.35)]),
        letter_pool(0.2)])
    table('chests/wreck_cargo', [
        pool([item('raft:scrap_metal', 2, 8, weight=5), item('raft:plastic', 2, 8, weight=5), item('iron_ingot', 1, 5, weight=4),
              item('raft:titanium_nugget', 2, 6, weight=3), item('raft:circuit_board', 1, 2, weight=2),
              item('coal', 2, 8, weight=4), item('leather', 1, 4, weight=3), item('paper', 2, 6, weight=3),
              item('raft:rope', 1, 4, weight=3), item('copper_ingot', 2, 6, weight=3)], rolls=(3, 7))])
    table('chests/wreck_fishing', [
        pool([item('cod', 2, 6, weight=5), item('salmon', 1, 4, weight=4), item('fishing_rod', weight=2),
              item('string', 2, 6, weight=4), item('raft:net', weight=1), item('raft:rope', 1, 4, weight=4),
              item('kelp', 2, 6, weight=3), item('raft:plastic_hook', weight=1), item('tropical_fish', 1, 2, weight=2)],
             rolls=(2, 5)),
        letter_pool(0.15)])
    table('chests/pirate_treasure', [
        pool([item('raft:gold_coin', 8, 24)]),
        pool([item('gold_ingot', 3, 8, weight=5), item('emerald', 3, 8, weight=4), item('diamond', 1, 3, weight=3),
              item('gold_block', weight=1), item('raft:gold_coin_pile', 1, 3, weight=2),
              item('golden_apple', weight=2), item('enchanted_golden_apple', weight=1)], rolls=(3, 5)),
        pool([item('raft:cutlass', weight=1), item('raft:ancient_broken_ring', weight=1), empty(3)])])
    table('chests/rusted_rig', [
        pool([item('raft:scrap_metal', 3, 9, weight=5), item('iron_ingot', 2, 6, weight=4),
              item('raft:titanium_ingot', 1, 3, weight=3), item('raft:circuit_board', 1, 3, weight=3),
              item('redstone', 2, 6, weight=3), item('coal', 3, 8, weight=4), item('raft:raw_titanium', 1, 4, weight=3),
              item('raft:rope', 2, 5, weight=3), item('raft:receiver', weight=1)], rolls=(3, 6)),
        letter_pool(0.2)])
    table('chests/sunken_plane', [
        pool([item('golden_carrot', 1, 3, weight=3), item('raft:pineapple', 1, 3, weight=3),
              item('bread', 2, 4, weight=4), item('compass', weight=2), item('paper', 2, 6, weight=3),
              item('iron_ingot', 1, 4, weight=4), item('leather', 1, 4, weight=3), item('raft:circuit_board', 1, 2, weight=3),
              item('raft:plastic', 2, 6, weight=4), item('raft:metal_detector', weight=1)], rolls=(3, 6)),
        letter_pool(0.2)])
    table('chests/sunken_ruins', [
        pool([item('prismarine_shard', 2, 6, weight=5), item('gold_nugget', 2, 8, weight=4),
              item('raft:seashell', 1, 4, weight=4), item('raft:pearl', weight=2), item('raft:sea_glass_shard', 1, 4, weight=3),
              item('raft:ancient_energy', weight=1), item('raft:ancient_broken_ring', weight=1),
              item('nautilus_shell', weight=1)], rolls=(2, 4))])
    table('chests/iceberg_outpost', [
        pool([item('coal', 2, 6, weight=5), item('cooked_cod', 2, 4, weight=4), item('raft:fresh_water', 1, 3, weight=4),
              item('snowball', 4, 12, weight=3), item('leather', 1, 4, weight=3), item('candle', 1, 3, weight=3),
              item('writable_book', weight=1), item('compass', weight=2), item('iron_ingot', 1, 3, weight=3),
              item('blue_ice', 1, 3, weight=1)], rolls=(3, 6)),
        letter_pool(0.2)])
    table('chests/lighthouse_storage', [
        pool([item('torch', 4, 12, weight=5), item('lantern', 1, 2, weight=3), item('candle', 2, 5, weight=4),
              item('coal', 3, 9, weight=5), item('raft:rope', 2, 6, weight=4), item('cod', 1, 4, weight=3),
              item('string', 2, 5, weight=3), item('raft:coconut_lantern', weight=2), item('iron_nugget', 3, 9, weight=3)],
             rolls=(3, 6)),
        letter_pool(0.15)])

    barrel_tables()

    # ---- gameplay
    table('gameplay/supply_barrel', [
        pool([item('raft:driftwood_planks', 2, 5, weight=6), item('raft:palm_planks', 2, 5, weight=5),
              item('raft:plastic', 2, 5, weight=6), item('raft:scrap_metal', 1, 3, weight=5), item('raft:rope', 1, 3, weight=5),
              item('raft:coconut', 1, 3, weight=4), item('cooked_cod', 1, 3, weight=4), item('raft:fresh_water', 1, 2, weight=4),
              item('string', 2, 4, weight=3), item('raft:titanium_nugget', 1, 4, weight=2), item('potato', 1, 3, weight=3),
              item('carrot', 1, 3, weight=3), item('wheat_seeds', 2, 5, weight=3), item('beetroot_seeds', 2, 5, weight=2),
              item('raft:palm_sapling', weight=2), item('leather', 1, 2, weight=2), item('iron_nugget', 2, 6, weight=3)],
             rolls=(3, 5)),
        pool([item('raft:circuit_board', weight=3), item('raft:plastic_hook', weight=2), item('raft:net', weight=1),
              item('raft:paddle', weight=1), empty(10)]),
        letter_pool(0.08)])
    letters = [book(k) for k in T.LETTERS]
    table('story/bottle_letters', [pool(letters)])


def barrel_tables():
    """3.4: small themed tables for a share of the barrels and crates of the structures (bclib/stock.py picks which).
    One to three stacks of common things (some rolls stay empty), well below the real chests of a place; only the
    salvage of the harder places (rig, Helix Station, Port Solace) has a small chance of a better find."""
    coins = lambda c: pool([item('raft:gold_coin', 1, 2, weight=c), empty(100 - c)])
    table('chests/barrel/supplies', [
        pool([item('raft:rope', 1, 3, weight=5), item('raft:plastic', 2, 5, weight=5), item('raft:scrap_metal', 1, 3, weight=4),
              item('raft:driftwood_planks', 2, 6, weight=4), item('raft:palm_planks', 2, 6, weight=3),
              item('string', 1, 4, weight=3), item('stick', 2, 6, weight=2), item('raft:fresh_water', 1, 2, weight=3),
              item('raft:fish_bait', 1, 3, weight=2), item('torch', 2, 4, weight=2), item('iron_nugget', 1, 4, weight=2),
              empty(3)], rolls=(1, 3)),
        coins(15)])
    table('chests/barrel/food', [
        pool([item('cooked_cod', 1, 3, weight=4), item('dried_kelp', 2, 6, weight=4), item('bread', 1, 2, weight=3),
              item('raft:coconut', 1, 3, weight=4), item('raft:pineapple', 1, 2, weight=3), item('raft:banana', 1, 3, weight=3),
              item('raft:rice', 1, 3, weight=2), item('raft:sweet_potato', 1, 3, weight=3), item('raft:fresh_water', 1, 2, weight=3),
              item('raft:sea_grapes', 1, 3, weight=2), empty(2)], rolls=(1, 3))])
    table('chests/barrel/seeds', [
        pool([item('raft:pineapple_crown', 1, 2, weight=3), item('raft:chili_seeds', 1, 3, weight=3), item('raft:rice', 2, 4, weight=3),
              item('raft:sweet_potato', 1, 3, weight=3), item('raft:banana_shoot', weight=2), item('raft:palm_sapling', 1, 2, weight=2),
              item('wheat_seeds', 2, 5, weight=2), item('melon_seeds', 1, 3, weight=1), empty(1)], rolls=(1, 2))])
    table('chests/barrel/fishing', [
        pool([item('raft:fish_bait', 2, 5, weight=5), item('cod', 1, 3, weight=4), item('salmon', 1, 2, weight=3),
              item('string', 2, 4, weight=3), item('kelp', 2, 5, weight=2), item('raft:rope', 1, 3, weight=3),
              item('raft:plastic_hook', weight=1), item('raft:net', weight=1), empty(2)], rolls=(1, 3))])
    table('chests/barrel/scrap', [
        pool([item('raft:scrap_metal', 2, 5, weight=5), item('raft:plastic', 2, 5, weight=4), item('iron_nugget', 2, 6, weight=4),
              item('copper_ingot', 1, 3, weight=3), item('raft:titanium_nugget', 1, 3, weight=2), item('raft:rope', 1, 2, weight=2),
              item('coal', 2, 5, weight=3), item('redstone', 1, 3, weight=1), empty(2)], rolls=(1, 3)),
        coins(10)])
    table('chests/barrel/ship_stores', [
        pool([item('raft:gold_coin', 1, 4, weight=4), item('raft:rope', 1, 3, weight=4), item('gunpowder', 1, 3, weight=3),
              item('raft:cannonball', 1, 3, weight=3), item('bone', 1, 3, weight=2), item('string', 1, 3, weight=2),
              item('dried_kelp', 2, 5, weight=2), item('raft:sail_canvas', 1, 2, weight=1), item('raft:rum_bottles', weight=1),
              empty(3)], rolls=(1, 3))])
    table('chests/barrel/city', [
        pool([item('paper', 2, 6, weight=4), item('book', weight=2), item('glass_bottle', 1, 3, weight=3),
              item('gold_nugget', 2, 5, weight=3), item('raft:gold_coin', 1, 3, weight=3), item('candle', 1, 2, weight=2),
              item('raft:plastic', 1, 4, weight=3), item('bread', 1, 2, weight=2), item('emerald', weight=1), empty(3)],
             rolls=(1, 3))])
    table('chests/barrel/salvage', [
        pool([item('raft:scrap_metal', 2, 5, weight=4), item('raft:titanium_nugget', 2, 5, weight=4),
              item('raft:circuit_board', weight=2), item('iron_ingot', 1, 3, weight=3), item('copper_ingot', 1, 4, weight=3),
              item('raft:plastic', 2, 4, weight=3), item('redstone', 1, 4, weight=2), item('glow_ink_sac', 1, 2, weight=1),
              empty(3)], rolls=(1, 3)),
        # a better find now and then (about one barrel in eight)
        pool([item('raft:titanium_ingot', 1, 2, weight=4), item('raft:oxygen_tank', weight=1), item('raft:golden_lure', weight=1),
              empty(42)])])


# ------------------------------------------------------------------------------------------------- block loot
def block_loot():
    def write(block, pools):
        A.write_json(M, f'data/{NS}/loot_table/blocks/{block}.json',
                     {'type': 'minecraft:block', 'pools': pools, 'random_sequence': f'{NS}:blocks/{block}'})

    for b in ('research_terminal', 'expedition_radio', 'sigil_pedestal', 'tide_seal', 'sanctum_tidestone',
              'sanctum_glass', 'leviathan_altar'):
        write(b, [{'condition': {'type': 'minecraft:survives_explosion'},
                   'entries': [{'type': 'minecraft:item', 'name': rid(b)}], 'rolls': 1}])
    # the altar gives back the sigils that rest in it
    pools = [{'condition': {'type': 'minecraft:survives_explosion'},
              'entries': [{'type': 'minecraft:item', 'name': rid('tidewarden_altar')}], 'rolls': 1}]
    for s in ('coral', 'pearl', 'storm', 'ember'):
        pools.append({'condition': {'type': 'minecraft:match_block', 'blocks': rid('tidewarden_altar'),
                                    'state': {s: 'true'}},
                      'entries': [{'type': 'minecraft:item', 'name': rid(f'{s}_sigil')}], 'rolls': 1})
    write('tidewarden_altar', pools)


# ------------------------------------------------------------------------------------------------- tags
def opt(x):
    return {'id': rid(x), 'required': False}


def tags():
    for place, tag_name in MAP_TAG.items():
        A.tag(M, 'worldgen/structure', tag_name, [opt(MAP_STRUCTURE[place])])
    A.tag(M, 'worldgen/structure', 'visit/rusted_rig', [opt('rusted_rig')])
    A.tag(M, 'worldgen/structure', 'visit/wreck_galleon', [opt('wreck_galleon')])
    # advancement helpers: optional entries, so the advancements load even while a module is missing
    A.tag(M, 'entity_type', 'adv/expedition_raft', [opt('expedition_raft')])
    A.tag(M, 'entity_type', 'adv/sharks', [opt('shark'), opt('hammerhead_shark')])
    A.tag(M, 'entity_type', 'adv/whale', [opt('whale')])
    A.tag(M, 'entity_type', 'adv/leviathan', [opt('leviathan')])
    A.tag(M, 'item', 'adv/diving_helmets', [opt('diving_helmet')])
    A.tag(M, 'item', 'adv/oxygen_tanks', [opt('oxygen_tank'), opt('titanium_oxygen_tank')])
    A.tag(M, 'item', 'adv/gold_coins', [opt('gold_coin')])
    A.tag(M, 'item', 'adv/cooked_crab', [opt('cooked_crab_claw')])
    A.tag(M, 'item', 'tide_sigils', ['coral_sigil', 'pearl_sigil', 'storm_sigil', 'ember_sigil'])
    # sigils and the key survive fire, lava, lightning and explosions
    A.tag(M, 'damage_type', 'relic_resistant', ['#minecraft:is_fire', '#minecraft:is_explosion',
                                                'minecraft:lightning_bolt', 'minecraft:cactus'])
    A.tag(M, 'block', 'minecraft:mineable/pickaxe', ['tidewarden_altar', 'research_terminal', 'sigil_pedestal'])
    A.tag(M, 'block', 'minecraft:mineable/axe', ['expedition_radio'])
    A.tag(M, 'block', 'minecraft:needs_iron_tool', ['tidewarden_altar'])
    # the sanctum shell must not be broken by anything (dragon/wither proof too)
    A.tag(M, 'block', 'minecraft:dragon_immune', ['tide_seal', 'sanctum_tidestone', 'sanctum_glass', 'leviathan_altar'])
    A.tag(M, 'block', 'minecraft:wither_immune', ['tide_seal', 'sanctum_tidestone', 'sanctum_glass', 'leviathan_altar'])


# ------------------------------------------------------------------------------------------------- advancements
def _display(icon, title_key, frame='task', background=None, toast=True, chat=True, hidden=False):
    d = {}
    if not chat:
        d['announce_to_chat'] = False
    if background:
        d['background'] = background
    d['description'] = {'translate': title_key + '.description'}
    if frame != 'task':
        d['frame'] = frame
    if hidden:
        d['hidden'] = True
    d['icon'] = {'id': rid(icon)}
    if not toast:
        d['show_toast'] = False
    d['title'] = {'translate': title_key + '.title'}
    return d


def _player(predicate):
    return {'type': 'minecraft:entity_properties', 'entity': 'this', 'predicate': predicate}


def _adv(path, parent, criteria, display, xp=None):
    d = {}
    if parent:
        d['parent'] = rid(parent)
    d['criteria'] = criteria
    d['display'] = display
    d['requirements'] = [list(criteria)]
    if xp:
        d['rewards'] = {'experience': xp}
    A.write_json(M, f'data/{NS}/advancement/{path}.json', d)


ADV_TEXT = {}


def adv(path, parent, criteria, icon, title, desc, frame='task', xp=None, **kw):
    key = f'advancements.{NS}.{path.replace("/", ".")}'
    ADV_TEXT[key + '.title'] = title
    ADV_TEXT[key + '.description'] = desc
    _adv(path, parent, criteria, _display(icon, key, frame, **kw), xp)


def visit(tag):
    return {'visit': {'conditions': {'player': _player({'minecraft:location': {'structures': '#' + rid(tag)}})},
                      'trigger': 'minecraft:location'}}


def obtain(*items):
    return {'obtain': {'conditions': {'items': [{'items': (rid(i) if not i.startswith('#') else i)} for i in items]},
                       'trigger': 'minecraft:inventory_changed'}}


def advancements():
    enter = {'enter': {'conditions': {'to': 'raft:raftdim'}, 'trigger': 'minecraft:changed_dimension'}}
    adv('story/root', None, enter, 'message_in_a_bottle', 'Tides of the Deep',
        'Arrive on the Endless Sea through a driftwood portal',
        background='raft:gui/advancements/backgrounds/story', toast=False, chat=False)
    adv('story/castaway', 'story/root', obtain('expedition_journal'), 'expedition_journal', 'The Castaway',
        'Find the Expedition Journal of Captain Marlow')
    adv('story/lighthouse', 'story/castaway', visit('on_lighthouse_maps'), 'minecraft:lantern',
        'Keeper of the Light', 'Reach the Old Lighthouse on the rocky isle', xp=20)
    adv('story/saltmere', 'story/lighthouse', visit('on_saltmere_maps'), 'raft:palm_thatch', 'Saltmere',
        'Visit the stilt village of Saltmere, where the reef glows', xp=20)
    adv('story/coral_sigil', 'story/saltmere', obtain('coral_sigil'), 'coral_sigil', 'The Coral Sigil',
        'Take the Coral Sigil from the shrine of Saltmere', frame='goal', xp=50)
    adv('story/helix', 'story/saltmere', visit('on_helix_maps'), 'research_terminal', 'Deep Science',
        'Dive to Helix Research Station', xp=30)
    adv('story/pearl_sigil', 'story/helix', obtain('pearl_sigil'), 'pearl_sigil', 'The Pearl Sigil',
        'Recover the Pearl Sigil from the lab vault of Helix Station', frame='goal', xp=50)
    adv('story/port_solace', 'story/helix', visit('on_port_solace_maps'), 'expedition_radio', 'The Drowned City',
        'Explore the ruins of Port Solace', xp=30)
    adv('story/storm_sigil', 'story/port_solace', obtain('storm_sigil'), 'storm_sigil', 'The Storm Sigil',
        'Claim the Storm Sigil from the vault of City Hall', frame='goal', xp=50)
    adv('story/ashen_forge', 'story/port_solace', visit('on_ashen_forge_maps'), 'minecraft:magma_block',
        'Through Fire', 'Enter the Ashen Forge on the volcanic isle', xp=30)
    adv('story/ember_sigil', 'story/ashen_forge', obtain('ember_sigil'), 'ember_sigil', 'The Ember Sigil',
        'Carry the Ember Sigil out of the forge', frame='goal', xp=50)
    adv('story/coral_temple', 'story/ashen_forge', visit('on_coral_temple_maps'), 'tidewarden_altar',
        'The Coral Temple', 'Find the Tidewarden temple on the reef floor', xp=30)
    adv('story/tidewarden_key', 'story/coral_temple', obtain('tidewarden_key'), 'tidewarden_key',
        'The Tidewarden Key', 'Forge the key from all four Tide Sigils', frame='goal', xp=100)
    adv('story/abyssal_sanctum', 'story/tidewarden_key', visit('on_abyssal_sanctum_maps'), 'tide_seal',
        'The Abyssal Sanctum', 'Reach the sanctum deep in the trench', xp=50)
    adv('story/leviathan', 'story/abyssal_sanctum',
        {'kill': {'conditions': {'entity': _player({'minecraft:entity_type': '#raft:adv/leviathan'})},
                  'trigger': 'minecraft:player_killed_entity'}},
        'minecraft:heart_of_the_sea', 'Calm Seas', 'Defeat the Leviathan and calm the Endless Sea',
        frame='challenge', xp=250)

    # adventure tab
    adv('adventure/root', None, enter, 'raft:paddle', 'Endless Sea Adventures', 'Life on the open water',
        background='raft:gui/advancements/backgrounds/adventure', toast=False, chat=False)
    adv('adventure/set_sail', 'adventure/root',
        {'ride': {'conditions': {'player': _player({'minecraft:vehicle': {
            'minecraft:entity_type': '#raft:adv/expedition_raft'}})}, 'trigger': 'minecraft:started_riding'}},
        'raft:sail_canvas', 'Set Sail', 'Take the helm of an Expedition Raft')
    adv('adventure/deep_breath', 'adventure/root',
        {'wear': {'conditions': {'player': _player({'minecraft:equipment': {
            'head': {'items': '#raft:adv/diving_helmets'}, 'chest': {'items': '#raft:adv/oxygen_tanks'}}})},
                  'trigger': 'minecraft:tick'}},
        'minecraft:turtle_helmet', 'Deep Breath', 'Wear a diving helmet together with an oxygen tank')
    adv('adventure/shark_bait', 'adventure/root',
        {'bitten': {'conditions': {'damage': {'source_entity': {'minecraft:entity_type': '#raft:adv/sharks'}}},
                    'trigger': 'minecraft:entity_hurt_player'}},
        'raft:shark_tooth', 'Shark Bait', 'Get bitten by a shark')
    adv('adventure/whale_watcher', 'adventure/root',
        {'spot': {'conditions': {'item': {'items': 'minecraft:spyglass'}, 'player': _player({
            'minecraft:type_specific/player': {'looking_at': {'minecraft:entity_type': '#raft:adv/whale'}}})},
                  'trigger': 'minecraft:using_item'}},
        'minecraft:spyglass', 'Whale Watcher', 'Watch a whale through a spyglass')
    adv('adventure/pirate_gold', 'adventure/root', obtain('#raft:adv/gold_coins'), 'minecraft:gold_nugget',
        'Pieces of Eight', 'Find a pirate gold coin')
    adv('adventure/crab_dinner', 'adventure/root',
        {'eat': {'conditions': {'item': {'items': '#raft:adv/cooked_crab'}}, 'trigger': 'minecraft:consume_item'}},
        'minecraft:cooked_cod', 'Crab Dinner', 'Eat a cooked crab claw')
    adv('adventure/rusted_giant', 'adventure/root', visit('visit/rusted_rig'), 'raft:scrap_metal', 'Rusted Giant',
        'Climb aboard the abandoned rig')
    adv('adventure/ghost_ship', 'adventure/pirate_gold', visit('visit/wreck_galleon'), 'minecraft:skeleton_skull',
        'Ghost Ship', 'Explore the wreck of a pirate galleon', frame='goal', xp=30)
    adv('adventure/saltjaws_gold', 'adventure/ghost_ship', visit('on_pirate_maps'), 'minecraft:gold_ingot',
        "Saltjaw's Gold", "Find the hidden cove of Saltjaw Brannigan", frame='goal', xp=50)


# ------------------------------------------------------------------------------------------------- recipes
RECIPES = []  # (name, ingredient ids)


def _guard(path, ingredients):
    """Prepends a Fabric load condition to a written JSON file when it uses items of other 3.0 modules."""
    import json
    own = {rid(x) for x in RAFT_2 + RAFT_STORY}
    cross = [rid(i) for i in ingredients if not i.startswith('#') and rid(i).startswith(NS + ':') and rid(i) not in own]
    if not cross:
        return
    with open(path, encoding='utf-8') as f:
        d = json.load(f)
    A.write_json(M, path.split(f'/modules/{M}/', 1)[1], {'fabric:load_conditions': load_condition(cross), **d})


def shaped(name, pattern, key, result, n=1, category='misc'):
    _guard(A.recipe_shaped(M, name, pattern, key, result, n, category), list(key.values()))
    RECIPES.append((name, [v for v in key.values()]))


def shapeless(name, ingredients, result, n=1, category='misc'):
    A.recipe_shapeless(M, name, ingredients, result, n, category)
    RECIPES.append((name, list(ingredients)))


def recipes():
    shapeless('expedition_journal', ['minecraft:book', 'minecraft:compass', 'minecraft:paper'], 'expedition_journal')
    shaped('research_terminal', ['SGS', 'SCS', 'SRS'], {'S': 'raft:scrap_metal', 'G': 'minecraft:glass_pane',
                                                       'C': 'raft:circuit_board', 'R': 'minecraft:redstone'},
           'research_terminal', category='redstone')
    shaped('expedition_radio', [' A ', 'SNS', 'SCS'], {'A': 'minecraft:copper_ingot', 'S': 'raft:scrap_metal',
                                                      'N': 'minecraft:note_block', 'C': 'raft:circuit_board'},
           'expedition_radio', category='redstone')
    shaped('sigil_pedestal', ['BBB', ' C ', 'BBB'], {'B': 'raft:tidestone_bricks', 'C': 'raft:chiseled_tidestone'},
           'sigil_pedestal', n=2, category='building')
    shaped('tidewarden_altar', ['GAG', 'BCB', 'BBB'], {'G': 'raft:glowing_tidestone', 'A': 'raft:ancient_crystal',
                                                      'B': 'raft:tidestone_bricks', 'C': 'raft:chiseled_tidestone'},
           'tidewarden_altar', category='building')
    for name, ings in RECIPES:
        items = [rid(i) for i in ings if not i.startswith('#')]
        items = list(dict.fromkeys(items))
        crit = {'has_ingredient': {'conditions': {'items': [{'items': items}]}, 'trigger': 'minecraft:inventory_changed'},
                'has_the_recipe': {'conditions': {'recipes': rid(name)}, 'trigger': 'minecraft:recipe_unlocked'}}
        _guard(A.write_json(M, f'data/{NS}/advancement/recipes/misc/{name}.json', {
            'parent': 'minecraft:recipes/root', 'criteria': crit, 'requirements': [list(crit.keys())],
            'rewards': {'recipes': [rid(name)]}}), items)


# ------------------------------------------------------------------------------------------------- lang
def lang():
    L = {'itemgroup.raft.story': 'Beach Craft: Tides of the Deep'}
    names = {
        'item': {'expedition_journal': 'Expedition Journal', 'message_in_a_bottle': 'Message in a Bottle',
                 'supply_barrel': 'Supply Barrel', 'coral_sigil': 'Coral Sigil', 'pearl_sigil': 'Pearl Sigil',
                 'storm_sigil': 'Storm Sigil', 'ember_sigil': 'Ember Sigil', 'tidewarden_key': 'Tidewarden Key'},
        'block': {'tide_seal': 'Tide Seal', 'sanctum_tidestone': 'Sanctum Tidestone', 'sanctum_glass': 'Sanctum Glass',
                  'tidewarden_altar': 'Tidewarden Altar', 'leviathan_altar': 'Leviathan Altar',
                  'research_terminal': 'Research Terminal', 'expedition_radio': 'Expedition Radio',
                  'sigil_pedestal': 'Sigil Pedestal'},
    }
    for kind, d in names.items():
        for k, v in d.items():
            L[f'{kind}.{NS}.{k}'] = v
    tips = {
        'expedition_journal.0': 'Use: read your current goal',
        'expedition_journal.1': 'Sneak + use: draw a map to the next place',
        'expedition_journal.ready': 'A new map can be drawn',
        'expedition_journal.wait': 'New map in %s',
        'message_in_a_bottle.0': 'Use: pull the cork',
        'supply_barrel.0': 'Use: break it open',
        'sigil.0': 'A Tide Sigil of the Tidewardens',
        'sigil.1': 'Four of them forge a key at a Tidewarden Altar',
        'tidewarden_key.0': 'Opens the Tide Seal',
        'tidewarden_key.1': 'Wakes the Leviathan at its altar',
        'tide_seal.0': 'Opens only for the Tidewarden Key',
        'sanctum.0': 'Unbreakable',
        'tidewarden_altar.0': 'Place the four Tide Sigils in it',
        'leviathan_altar.0': 'Use with the Tidewarden Key to wake the Leviathan',
        'research_terminal.0': 'Shows an Expedition log',
        'expedition_radio.0': 'Use: listen to the static',
    }
    for k, v in tips.items():
        L[f'tooltip.{NS}.{k}'] = v
    lore = {'coral_sigil': 'Coral, for the living reef', 'pearl_sigil': 'Pearl, for patience',
            'storm_sigil': "Storm, for the wind's anger", 'ember_sigil': 'Ember, for the deep fire',
            'tidewarden_key': 'Four tides gathered in one hand'}
    for k, v in lore.items():
        L[f'item.{NS}.{k}.lore'] = v
    msgs = {
        'journal.not_sea': "The journal's charts only work on the Endless Sea.",
        'journal.no_place': "The journal finds no trace of that place nearby. Sail on and try again.",
        'journal.wait': 'The ink is still wet. You can draw a new map in %s.',
        'journal.done': 'Your journey is complete. The sea needs no more charts.',
        'journal.drawn': 'You sketch a chart in the journal and tear out the page.',
        'altar.inserted': 'The %s settles into the altar with a soft glow.',
        'altar.has': 'That sigil already rests in the altar.',
        'altar.status': 'Sigils in the altar: %s of 4. Missing: %s',
        'altar.empty': 'Four hollows wait for the Tide Sigils: Coral, Pearl, Storm and Ember.',
        'altar.awake': 'The altar blazes with four lights!',
        'altar.forged': 'The Tidewarden Key rises from the light.',
        'seal.locked': 'The Tide Seal does not move. It waits for the Tidewarden Key.',
        'seal.open': 'The Tide Seal parts like water.',
        'leviathan_altar.need_key': 'The altar waits for the Tidewarden Key.',
        'leviathan_altar.alive': 'The Leviathan is already awake!',
        'leviathan_altar.need_crystal': 'The altar hungers for an Ancient Crystal before it calls again.',
        'leviathan_altar.not_ready': 'The pool is not ready: the altar cannot call the Leviathan here.',
        'leviathan_altar.silent': 'The altar stays silent.',
        'leviathan_altar.summon': 'The Leviathan rises from the deep!',
        'radio.prefix': 'Radio',
        'bottle.empty': 'The bottle is empty. Only the smell of the sea.',
    }
    for k, v in msgs.items():
        L[f'message.{NS}.{k}'] = v
    for k, v in T.MAP_NAMES.items():
        L[f'filled_map.{NS}.{k}'] = v
    for bid, (title, author, pages) in list(T.BOOKS.items()) + list(T.LETTERS.items()):
        for i, p in enumerate(pages):
            assert len(p) <= 230, (bid, i, len(p))
            L[f'book.{NS}.{bid}.{i + 1}'] = p
    for lid, (title, text) in T.TERMINALS.items():
        L[f'terminal.{NS}.{lid}.title'] = title
        L[f'terminal.{NS}.{lid}.text'] = text
    L[f'terminal.{NS}.unknown.title'] = 'NO SIGNAL'
    L[f'terminal.{NS}.unknown.text'] = 'The screen flickers. Whatever was stored here was lost to the sea.'
    for i, line in enumerate(T.RADIO):
        L[f'radio.{NS}.{i + 1}'] = line
    for i, (adv_id, title, objective, hint) in enumerate(T.CHAPTERS):
        L[f'journal.{NS}.{adv_id}.title'] = title
        L[f'journal.{NS}.{adv_id}.goal'] = objective
        L[f'journal.{NS}.{adv_id}.hint'] = hint
    L[f'journal.{NS}.done.title'], L[f'journal.{NS}.done.goal'], L[f'journal.{NS}.done.hint'] = T.DONE
    ui = {
        'journal.raft.heading': 'Expedition Journal',
        'journal.raft.chapter': 'Chapter %s',
        'journal.raft.goal': 'Goal',
        'journal.raft.hint': 'Hint',
        'journal.raft.progress': 'The Voyage',
        'journal.raft.unknown': '? ? ?',
        'journal.raft.sigils': 'Tide Sigils',
        'journal.raft.map_ready': 'Sneak + use: draw a chart.',
        'journal.raft.map_wait': 'Next chart in %s.',
        'journal.raft.map_done': 'No more charts needed.',
        'journal.raft.map_not_sea': 'Charts only work at sea.',
        'terminal.raft.header': 'MERIDIAN EXPEDITION - TERMINAL',
        'terminal.raft.page': 'PAGE %s/%s',
        'terminal.raft.keys': '[<] [>] PAGE   [ESC] LOG OFF',
        'subtitles.raft.radio_static': 'Radio crackles',
    }
    L.update(ui)
    L.update(ADV_TEXT)
    A.lang(M, L)


def generate():
    loot_tables()
    block_loot()
    tags()
    advancements()
    recipes()
    lang()
