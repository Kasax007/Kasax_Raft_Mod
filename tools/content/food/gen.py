"""Generator of the "food" module (Beach Craft 3.3): island crops, wild plants, dishes and drinks, the cooking pot
and the fish trap, plus the world features that plant wild crops on the islands.

Run:  cd tools && python3 -m content.food.gen
Writes into src/main/modules/food/ and src/main/java/net/kasax/raft/content/food/block/FoodShapeData.java.
"""
import os
import shutil
import sys

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..')))

from bclib import assets as A  # noqa: E402
from bclib.paths import module_dir, ROOT  # noqa: E402

from content.cozy import cm  # noqa: E402
from content.cozy.cm import Model, rid, facing_variants  # noqa: E402
from content.cozy.recipes import Book  # noqa: E402
from content.food import art  # noqa: E402
from content.cozy.almanac import PAGES  # noqa: E402

M = 'food'
BLOCKS = {}      # id -> name
ITEMS = {}       # id -> name
LANG = {
    'itemgroup.raft.kitchen': 'Beach Craft: Island Kitchen',
    'container.raft.fish_trap': 'Fish Trap',
    'message.raft.pot.empty': 'Cooking Pot: right-click it with up to 4 ingredients',
    'message.raft.pot.recipe': 'Cooking %s - keep it on the fire',
    'message.raft.pot.no_heat': '%s is ready to cook: the pot needs a fire, campfire, brazier, magma or lava below it',
    'message.raft.pot.needs': 'For %s add %s',
    'message.raft.pot.could': 'This can become %s - add more',
    'message.raft.pot.unknown': 'No dish with %s: sneak-use with an empty hand to take them back',
    'message.raft.pot.full': 'The pot is full',
    'message.raft.pot.ready': '%s is ready: use a bowl to serve it (%s left)',
    'message.raft.pot.not_ingredient': '%s does not go into the pot (the Almanac lists all dishes)',
    'message.raft.pot.no_fit': '%s does not go with what is in the pot - sneak-use with an empty hand to start over',
    'message.raft.pot.taken': 'You took the ingredients back',
    'message.raft.pot.seafood': 'a fish, crab claw or eel fillet',
    'message.raft.pot.and': ' and ',
    'subtitles.raft.cooking_pot.done': 'Cooking Pot: dish ready',
    'tooltip.raft.cooking_pot': 'Put up to 4 ingredients in, over a fire',
    'tooltip.raft.cooking_pot.1': 'Serve the dish with bowls',
    'tooltip.raft.cooking_pot.2': 'Sneak-use with an empty hand: ingredients back',
    # JEI (optional; net.kasax.raft.compat.jei)
    'jei.raft.cooking_pot': 'Cooking Pot',
    'jei.raft.cooking_pot.servings': '%s bowls',
    'jei.raft.cooking_pot.time': '%s s',
    'jei.raft.cooking_pot.heat': 'below the pot (lit)',
    'jei.raft.fish_trap': 'Fish Trap',
    'jei.raft.fish_trap.bait': 'Optional: twice as fast, one per catch',
    'jei.raft.fish_trap.chance': '%s%% of catches',
    'jei.raft.fish_trap.count': '%s to %s at a time',
    'jei.raft.fish_trap.time': 'In open water: a catch every 1 to 2 minutes, twice as fast with bait',
    'jei.raft.island_crops': 'Island Crops',
    'jei.raft.crops.soil': 'Plant it here',
    'jei.raft.crops.farm': 'Farmland or crop plot',
    'jei.raft.crops.sea_grapes': 'Under water, pick it ripe',
    'jei.raft.crops.wild': 'Grows wild: break it',
    'jei.raft.info.cooking_pot': 'Set the pot on a campfire, fire, brazier, simple grill, magma block or lava. Right-click it '
                                 'with up to 4 ingredients: it only takes what fits a dish, and the action bar tells you '
                                 'what is still missing. When the ingredients make a dish it cooks in 10 seconds: the '
                                 'liquid turns the colour of the dish, and a chime rings when it is done. Serve it with '
                                 'bowls. Changed your mind? Sneak-use the pot with an empty hand to get every ingredient back.',
    'jei.raft.info.fish_trap': 'Place it in water with open water around it. Every 1 to 2 minutes it catches something. '
                               'Fish Bait or a Golden Lure inside makes it twice as fast (one per catch). Hoppers can empty it.',
    'jei.raft.info.crop_plot': 'Farmland that never dries out and cannot be trampled. Crops on it grow about one and a half '
                               'times as fast; the island crops grow on it too.',
    'tooltip.raft.fish_trap': 'Place it in water: catches fish over time',
    'tooltip.raft.fish_trap.1': 'Bait inside makes it twice as fast',
    'tooltip.raft.sea_grapes': 'Plant on sand or gravel under water',
    'tooltip.raft.plant_farmland': 'Plant on farmland or a crop plot',
    'tooltip.raft.wild_plant': 'Break it for food and seeds',
    'tooltip.raft.chili_pepper': 'Hot! Fire Resistance for a moment',
}

CROPS = {
    # crop block: (English name, seed item, produce item, stage painter, model kind)
    'sweet_potatoes': ('Sweet Potatoes', 'sweet_potato', 'sweet_potato', art.sweet_potato_stage, 'crop'),
    'chili_crop': ('Chili Peppers', 'chili_seeds', 'chili_pepper', art.chili_stage, 'crop'),
    'banana_crop': ('Banana Plant', 'banana_shoot', 'banana', art.banana_stage, 'cross'),
    'rice_crop': ('Rice', 'rice', 'rice', art.rice_stage, 'crop'),
}
WILD = {
    'wild_sweet_potato': ('Wild Sweet Potato', 'sweet_potatoes', 'sweet_potato', None),
    'wild_chili': ('Wild Chili', 'chili_crop', 'chili_pepper', 'chili_seeds'),
    'wild_banana': ('Wild Banana', 'banana_crop', 'banana', 'banana_shoot'),
    'wild_rice': ('Wild Rice', 'rice_crop', 'rice', None),
}
FOOD_ITEMS = {
    'sweet_potato': ('Sweet Potato', art.sweet_potato),
    'baked_sweet_potato': ('Baked Sweet Potato', art.baked_sweet_potato),
    'chili_seeds': ('Chili Seeds', art.chili_seeds),
    'chili_pepper': ('Chili Pepper', art.chili_pepper),
    'banana_shoot': ('Banana Shoot', art.banana_shoot),
    'banana': ('Banana', art.banana),
    'rice': ('Rice', art.rice),
    'cooked_rice': ('Cooked Rice', art.cooked_rice),
    'sea_grapes': ('Sea Grapes', art.sea_grapes),
    'sushi_roll': ('Sushi Roll', art.sushi_roll),
    'chili_crab': ('Chili Crab', art.chili_crab),
    'fruit_salad': ('Fruit Salad', art.fruit_salad),
    'tropical_smoothie': ('Tropical Smoothie', art.tropical_smoothie),
    'banana_bread': ('Banana Bread', art.banana_bread),
    'island_curry': ('Island Curry', art.island_curry),
    'seafood_chowder': ('Seafood Chowder', art.seafood_chowder),
    'sweet_potato_soup': ('Sweet Potato Soup', art.sweet_potato_soup),
    'coconut_ice_cream': ('Coconut Ice Cream', art.coconut_ice_cream),
}


# ================================================================================================= crops
def crops():
    for cid, (name, seed, produce, painter, kind) in CROPS.items():
        BLOCKS[cid] = name
        v = {}
        for s in range(4):
            t = f'raft:block/{cid}_stage{s}'
            art.save(painter(s), f'block/{cid}_stage{s}')
            if kind == 'crop':
                A.block_model(M, f'{cid}_stage{s}', 'minecraft:block/crop', {'crop': t})
            else:
                A.block_model(M, f'{cid}_stage{s}', 'minecraft:block/cross', {'cross': t})
            v[f'age={s}'] = {'model': rid(f'block/{cid}_stage{s}')}
        A.blockstate(M, cid, v)
        ripe = {'condition': 'minecraft:block_state_property', 'block': rid(cid), 'properties': {'age': '3'}}
        fortune = {'function': 'minecraft:apply_bonus', 'enchantment': 'minecraft:fortune',
                   'formula': 'minecraft:binomial_with_bonus_count', 'parameters': {'extra': 2, 'probability': 0.5714286}}
        if seed == produce:
            pools = [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': rid(seed)}]},
                     {'rolls': 1, 'conditions': [ripe], 'entries': [{'type': 'minecraft:item', 'name': rid(produce),
                                                                     'functions': [fortune]}]}]
        else:
            pools = [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': rid(seed)}]},
                     {'rolls': 1, 'conditions': [ripe], 'entries': [{'type': 'minecraft:item', 'name': rid(produce),
                                                                     'functions': [{'function': 'minecraft:set_count',
                                                                                    'count': {'type': 'minecraft:uniform',
                                                                                              'min': 1, 'max': 3}},
                                                                                   fortune]}]}]
        A.write_json(M, f'data/raft/loot_table/blocks/{cid}.json', {
            'type': 'minecraft:block', 'functions': [{'function': 'minecraft:explosion_decay'}], 'pools': pools,
            'random_sequence': f'raft:blocks/{cid}'})
    # sea grapes: an underwater crop (cross model, no farmland)
    BLOCKS['sea_grapes_crop'] = 'Sea Grapes'
    v = {}
    for s in range(4):
        art.save(art.sea_grapes_stage(s), f'block/sea_grapes_stage{s}')
        A.block_model(M, f'sea_grapes_stage{s}', 'minecraft:block/cross', {'cross': f'raft:block/sea_grapes_stage{s}'})
        v[f'age={s}'] = {'model': rid(f'block/sea_grapes_stage{s}')}
    A.blockstate(M, 'sea_grapes_crop', v)
    ripe = {'condition': 'minecraft:block_state_property', 'block': rid('sea_grapes_crop'), 'properties': {'age': '3'}}
    A.write_json(M, 'data/raft/loot_table/blocks/sea_grapes_crop.json', {
        'type': 'minecraft:block', 'pools': [
            {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'raft:sea_grapes'}]},
            {'rolls': 1, 'conditions': [ripe], 'entries': [{'type': 'minecraft:item', 'name': 'raft:sea_grapes',
                                                            'functions': [{'function': 'minecraft:set_count',
                                                                           'count': {'type': 'minecraft:uniform',
                                                                                     'min': 1, 'max': 2}}]}]}],
        'random_sequence': 'raft:blocks/sea_grapes_crop'})


def wild():
    for wid, (name, crop, produce, seed) in WILD.items():
        BLOCKS[wid] = name
        tex_id = f'raft:block/{crop}_stage3'
        A.block_model(M, wid, 'minecraft:block/cross', {'cross': tex_id})
        A.blockstate(M, wid, {'': {'model': rid(f'block/{wid}')}})
        A.flat_item(M, wid, f'block/{crop}_stage3')
        entries = [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': rid(produce), 'functions': [
            {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 1, 'max': 2}}]}]}]
        if seed:
            entries.append({'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': rid(seed)}]})
        A.write_json(M, f'data/raft/loot_table/blocks/{wid}.json', {
            'type': 'minecraft:block', 'functions': [{'function': 'minecraft:explosion_decay'}], 'pools': entries,
            'random_sequence': f'raft:blocks/{wid}'})


# ================================================================================================= kitchen
def cooking_pot():
    """The pot itself; the liquid, the ingredients and the dish are drawn in the world by CookingPotRenderer.java
    (cooking_pot_liquid is a grey surface it tints from water to the colour of the dish). Every block state shows the
    empty pot; the item shows the pot full of stew."""
    BLOCKS['cooking_pot'] = 'Cooking Pot'
    side = 'raft:block/cooking_pot_side'
    art.save(art.pot_side(), 'block/cooking_pot_side')
    art.save(art.pot_inside(), 'block/cooking_pot_inside')
    art.save(art.surface(art.CURRY, bubbles=False), 'block/cooking_pot_stew')
    art.save(art.liquid(), 'block/cooking_pot_liquid')
    inside = 'raft:block/cooking_pot_inside'
    for name, surf in (('empty', None), ('item', 'raft:block/cooking_pot_stew')):
        tx = {'s': side, 'i': inside, 'h': 'minecraft:block/iron_block'}
        if surf:
            tx['w'] = surf
        m = Model(M, tx, ao=True, particle=side)
        # floor and four walls, 1 px thick, so the inside is visible
        m.box((2, 0, 2), (14, 1, 14), 's', faces={'up': 'i'})
        m.box((2, 1, 2), (14, 9, 3), 's', faces={'south': 'i'})
        m.box((2, 1, 13), (14, 9, 14), 's', faces={'north': 'i'})
        m.box((2, 1, 3), (3, 9, 13), 's', faces={'east': 'i'})
        m.box((13, 1, 3), (14, 9, 13), 's', faces={'west': 'i'})
        m.box((0.5, 6, 7), (2, 7, 9), 's', collide=False)       # handles
        m.box((14, 6, 7), (15.5, 7, 9), 's', collide=False)
        if surf:
            m.box((3, 1, 3), (13, 7, 13), 'w', only=('up',), collide=False, solid=False)
        m.write(f'cooking_pot_{name}')
    cm.shape(M, 'cooking_pot', [(2, 0, 2, 14, 9, 14)])
    v = {}
    for state in ('empty', 'cooking', 'ready'):
        v.update(facing_variants('block/cooking_pot_empty', f'contents={state}'))
    A.blockstate(M, 'cooking_pot', v)
    cm.block_item(M, 'cooking_pot', 'block/cooking_pot_item', scale=0.7)


def fish_trap():
    BLOCKS['fish_trap'] = 'Fish Trap'
    art.save(art.trap_cage(), 'block/fish_trap_cage')
    art.save(art.trap_end(), 'block/fish_trap_end')
    cage, end = 'raft:block/fish_trap_cage', 'raft:block/fish_trap_end'
    m = Model(M, {'c': cage, 'e': end, 'w': 'raft:block/rattan'}, ao=True, particle=cage)
    m.box((1, 0, 3), (15, 1, 13), 'w')                                   # wicker floor
    m.plane_z(1, 1, 15, 10, 3, 'c', uv=[1, 6, 15, 15])                   # long sides (double-sided)
    m.plane_z(1, 1, 15, 10, 13, 'c', uv=[1, 6, 15, 15])
    m.plane_x(3, 1, 13, 10, 1, 'e', uv=[3, 3, 13, 12])                   # funnel ends
    m.plane_x(3, 1, 13, 10, 15, 'e', uv=[3, 3, 13, 12])
    m.box((1, 10, 3.5), (15, 10, 12.5), 'c', only=('up', 'down'), collide=False, solid=False)
    m.box((0.5, 10, 2.5), (15.5, 11, 3.5), 'w', collide=False)          # top rails
    m.box((0.5, 10, 12.5), (15.5, 11, 13.5), 'w', collide=False)
    m.write('fish_trap')
    cm.shape(M, 'fish_trap', [(1, 0, 3, 15, 11, 13)])
    A.blockstate(M, 'fish_trap', facing_variants('block/fish_trap'))
    cm.block_item(M, 'fish_trap', 'block/fish_trap', scale=0.65)


# ================================================================================================= items
def items():
    for iid, (name, painter) in FOOD_ITEMS.items():
        ITEMS[iid] = name
        art.save(painter(), f'item/{iid}')
        A.flat_item(M, iid)


# ================================================================================================= recipes, loot, tags
def recipes():
    b = Book(M, set(BLOCKS) | set(ITEMS))
    b.cooking('baked_sweet_potato', 'sweet_potato', 'baked_sweet_potato', 0.35)
    b.cooking('cooked_rice', 'rice', 'cooked_rice', 0.2)
    b.shapeless('chili_seeds', ['chili_pepper'], 'chili_seeds', 2)
    b.shapeless('sushi_roll', ['cooked_rice', ['minecraft:cod', 'minecraft:salmon', 'minecraft:tropical_fish'],
                               'minecraft:dried_kelp'], 'sushi_roll', 3, 'misc')
    b.shapeless('chili_crab', ['raft:cooked_crab_claw', 'chili_pepper', 'minecraft:bowl'], 'chili_crab', 1, 'misc')
    b.shapeless('fruit_salad', ['banana', 'raft:pineapple', 'minecraft:melon_slice', 'minecraft:bowl'], 'fruit_salad', 1, 'misc')
    b.shapeless('tropical_smoothie', ['banana', 'raft:pineapple', 'minecraft:sugar', 'raft:fresh_water'],
                'tropical_smoothie', 1, 'misc')
    b.shapeless('banana_bread', ['banana', 'banana', 'minecraft:wheat', 'minecraft:wheat', 'minecraft:wheat',
                                 'minecraft:sugar'], 'banana_bread', 1, 'misc')
    b.shapeless('coconut_ice_cream', ['raft:coconut', 'minecraft:snowball', 'minecraft:sugar', 'minecraft:bowl'],
                'coconut_ice_cream', 1, 'misc')
    b.shaped('cooking_pot', [' S ', 'I I', 'III'], {'S': 'minecraft:stick', 'I': 'minecraft:iron_ingot'}, 'cooking_pot',
             1, 'misc')
    b.shaped('fish_trap', ['SBS', 'B B', 'SBS'], {'S': 'minecraft:string', 'B': 'minecraft:bamboo'}, 'fish_trap', 1, 'misc')
    b.finish('food')


FISH_TRAP_CATCHES = [
    # (item, weight, min, max): the loot table raft:gameplay/fish_trap and FishTrapCatches.java (JEI page)
    ('minecraft:cod', 45, 1, 2), ('minecraft:salmon', 25, 1, 1), ('minecraft:tropical_fish', 10, 1, 1),
    ('minecraft:pufferfish', 4, 1, 1), ('raft:crab_claw', 6, 1, 1), ('raft:seashell', 6, 1, 1),
    ('minecraft:kelp', 4, 1, 3), ('raft:sea_grapes', 4, 1, 2), ('minecraft:nautilus_shell', 1, 1, 1),
]


def fish_trap_loot():
    def e(item, w, lo=1, hi=1):
        d = {'type': 'minecraft:item', 'name': item, 'weight': w}
        if hi > 1:
            d['functions'] = [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}]
        return d
    A.loot_table(M, 'gameplay/fish_trap', {'type': 'minecraft:chest', 'pools': [{'rolls': 1, 'entries': [
        e(*c) for c in FISH_TRAP_CATCHES]}], 'random_sequence': 'raft:gameplay/fish_trap'})
    rows = ',\n'.join(f'            new Catch("{i}", {w}, {lo}, {hi})' for i, w, lo, hi in FISH_TRAP_CATCHES)
    java = f"""package net.kasax.raft.content.food.block;

import java.util.List;

/**
 * What a fish trap catches, for the JEI page (generated by tools/content/food/gen.py from the same list as the loot
 * table raft:gameplay/fish_trap, which decides the real catch).
 */
public final class FishTrapCatches {{
    public record Catch(String id, int weight, int min, int max) {{
    }}

    public static final List<Catch> CATCHES = List.of(
{rows});
    public static final int TOTAL_WEIGHT = {sum(c[1] for c in FISH_TRAP_CATCHES)};

    private FishTrapCatches() {{
    }}
}}
"""
    with open(os.path.join(ROOT, 'src', 'main', 'java', 'net', 'kasax', 'raft', 'content', 'food', 'block',
                           'FishTrapCatches.java'), 'w') as f:
        f.write(java)


def world_features():
    """Wild crops on the islands and sea grapes in the warm shallows (added to the biomes by FoodModule.java)."""
    def offset(xz, y):
        return {'type': 'minecraft:offset',
                'x': {'type': 'minecraft:trapezoid', 'max': xz, 'min': -xz, 'plateau': 0},
                'y': {'type': 'minecraft:trapezoid', 'max': y, 'min': -y, 'plateau': 0},
                'z': {'type': 'minecraft:trapezoid', 'max': xz, 'min': -xz, 'plateau': 0}}

    ground = {
        'wild_sweet_potato': ['minecraft:grass_block', 'minecraft:sand', 'minecraft:coarse_dirt', 'raft:shell_sand'],
        'wild_chili': ['minecraft:grass_block', 'minecraft:coarse_dirt', 'raft:black_sand', 'raft:volcanic_ash'],
        'wild_banana': ['minecraft:grass_block', 'minecraft:podzol', 'minecraft:coarse_dirt'],
        'wild_rice': ['minecraft:grass_block', 'minecraft:mud', 'minecraft:muddy_mangrove_roots'],
    }
    rarity = {'wild_sweet_potato': 9, 'wild_chili': 12, 'wild_banana': 8, 'wild_rice': 8}
    for wid, blocks in ground.items():
        A.write_json(M, f'data/raft/worldgen/feature/{wid}.json', {
            'type': 'minecraft:simple_block', 'to_place': {'id': rid(wid)}})
        A.write_json(M, f'data/raft/worldgen/placed_feature/patch_{wid}.json', {'feature': rid(wid), 'placement': [
            {'type': 'minecraft:rarity_filter', 'chance': rarity[wid]}, {'type': 'minecraft:in_square'},
            {'type': 'minecraft:heightmap', 'heightmap': 'WORLD_SURFACE_WG'}, {'type': 'minecraft:biome'},
            {'type': 'minecraft:count', 'count': 6}, offset(3, 1),
            {'type': 'minecraft:block_predicate_filter', 'predicate': {'type': 'minecraft:all_of', 'predicates': [
                {'type': 'minecraft:matching_block_tag', 'tag': 'minecraft:air'},
                {'type': 'minecraft:matching_blocks', 'blocks': blocks, 'offset': [0, -1, 0]}]}},
            {'type': 'raft:clear_of_structures', 'distance': 0}]})
    A.write_json(M, 'data/raft/worldgen/feature/sea_grapes.json', {
        'type': 'minecraft:simple_block', 'to_place': {
            'type': 'minecraft:randomized_int', 'property': 'age',
            'source': {'id': 'raft:sea_grapes_crop', 'properties': {'age': '3'}},
            'values': {'type': 'minecraft:uniform', 'min_inclusive': 1, 'max_inclusive': 3}}})
    A.write_json(M, 'data/raft/worldgen/placed_feature/patch_sea_grapes.json', {'feature': 'raft:sea_grapes', 'placement': [
        {'type': 'minecraft:rarity_filter', 'chance': 4}, {'type': 'minecraft:in_square'},
        {'type': 'minecraft:count', 'count': 8},
        {'type': 'minecraft:offset', 'x': {'type': 'minecraft:trapezoid', 'max': 4, 'min': -4, 'plateau': 0},
         'y': {'type': 'minecraft:trapezoid', 'max': 0, 'min': 0, 'plateau': 0},
         'z': {'type': 'minecraft:trapezoid', 'max': 4, 'min': -4, 'plateau': 0}},
        {'type': 'minecraft:heightmap', 'heightmap': 'OCEAN_FLOOR'},
        {'type': 'minecraft:block_predicate_filter', 'predicate': {'type': 'minecraft:all_of', 'predicates': [
            {'type': 'minecraft:matching_blocks', 'blocks': ['minecraft:water']},
            {'type': 'minecraft:matching_blocks', 'blocks': ['minecraft:sand', 'minecraft:gravel', 'raft:shell_sand'],
             'offset': [0, -1, 0]}]}},
        {'type': 'minecraft:biome'}, {'type': 'raft:clear_of_structures', 'distance': 0}]})


def tags():
    for c in list(CROPS) + ['sea_grapes_crop']:
        A.tag(M, 'block', 'minecraft:crops', [c] if c != 'sea_grapes_crop' else [])
    A.tag(M, 'block', 'minecraft:bee_attractive', ['chili_crop', 'banana_crop'])
    # crops keep their farmland from drying out into dirt (vanilla lists its crops here)
    A.tag(M, 'block', 'minecraft:maintains_farmland', list(CROPS))
    for w in WILD:
        A.tag(M, 'block', 'minecraft:replaceable_by_trees', [w])
        A.tag(M, 'block', 'minecraft:sword_efficient', [w])
        A.tag(M, 'block', 'minecraft:mineable/hoe', [w])
    A.tag(M, 'block', 'minecraft:mineable/pickaxe', ['cooking_pot'])
    A.tag(M, 'block', 'minecraft:mineable/axe', ['fish_trap'])
    A.tag(M, 'block', 'raft:pot_heat_sources', ['minecraft:fire', 'minecraft:soul_fire', 'minecraft:magma_block',
                                                'minecraft:lava', '#minecraft:campfires',
                                                {'id': 'raft:brazier', 'required': False},
                                                {'id': 'raft:simple_grill', 'required': False}])
    A.tag(M, 'item', 'minecraft:villager_plantable_seeds', ['chili_seeds', 'sweet_potato', 'rice'])
    A.tag(M, 'item', 'raft:pot_seafood', ['minecraft:cod', 'minecraft:salmon', 'minecraft:tropical_fish',
                                          {'id': 'raft:crab_claw', 'required': False},
                                          {'id': 'raft:eel_fillet', 'required': False}])
    A.tag(M, 'item', 'raft:island_fruit', ['banana', {'id': 'raft:pineapple', 'required': False},
                                           {'id': 'raft:coconut', 'required': False}])
    A.tag(M, 'item', 'raft:island_seeds', ['chili_seeds', 'banana_shoot', 'sweet_potato', 'rice', 'sea_grapes'])
    A.tag(M, 'item', 'minecraft:chicken_food', ['chili_seeds', 'rice'])
    A.tag(M, 'item', 'minecraft:parrot_food', ['chili_seeds', 'rice'])


def main():
    out = module_dir(M)
    if os.path.isdir(out):
        shutil.rmtree(out)
    os.makedirs(out)
    crops()
    wild()
    cooking_pot()
    fish_trap()
    items()
    for b, name in BLOCKS.items():
        LANG[f'block.raft.{b}'] = name
        if b in ('cooking_pot', 'fish_trap'):
            A.loot_self(M, b)
    for i, name in ITEMS.items():
        LANG[f'item.raft.{i}'] = name
    recipes()
    fish_trap_loot()
    world_features()
    tags()
    A.sounds(M, {'cooking_pot.done': {'sounds': [{'name': 'minecraft:block.note_block.bell', 'type': 'event',
                                                    'pitch': 1.26, 'volume': 0.8}],
                                       'subtitle': 'subtitles.raft.cooking_pot.done'}})
    LANG.update(PAGES['food'])
    A.lang(M, LANG)
    A.flush(M)
    cm.write_java_shapes(M, 'net.kasax.raft.content.food.block', 'FoodShapeData',
                         os.path.join(ROOT, 'src', 'main', 'java', 'net', 'kasax', 'raft', 'content', 'food', 'block',
                                      'FoodShapeData.java'))
    from content.cozy import loot26
    loot26.modernize(module_dir(M))
    lint = cm.report_lint(M)
    n = sum(len(fs) for _, _, fs in os.walk(out))
    print(f'food: {len(BLOCKS)} blocks, {len(ITEMS)} items, {n} files, {lint} lint warnings')


if __name__ == '__main__':
    main()
