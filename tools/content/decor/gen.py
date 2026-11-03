"""Generator of the "decor" module (docs/design/beachcraft3.md, section 6).

Run:  cd tools && python3 -m content.decor.gen
Writes textures, models, blockstates, item definitions, loot tables, recipes (+ recipe book unlocks), tags and lang
into src/main/modules/decor/, and the shape boxes of the models into
src/main/java/net/kasax/raft/content/decor/block/DecorShapeData.java. Re-runnable: wipes its output folder first.
"""
import glob
import json
import os
import re
import shutil
import sys

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..')))

from bclib import assets as A  # noqa: E402
from bclib.paths import module_dir, ROOT, RESOURCES  # noqa: E402

from content.decor import mdl  # noqa: E402
from content.decor import lights, pirate, furniture  # noqa: E402
from content.decor.lights import COLOURS  # noqa: E402
from content.decor.furniture import CUSHIONS  # noqa: E402

M = 'decor'
NS = 'raft'
rid = A.rid

# ================================================================================================= block table
# id: (English name, tool or None, loot kind)
BLOCKS = {
    'string_lights_warm': ('Warm String Lights', None, 'self'),
    'string_lights_colorful': ('Colorful String Lights', None, 'self'),
    'bunting_red_white': ('Red and White Bunting', None, 'self'),
    'bunting_rainbow': ('Rainbow Bunting', None, 'self'),
    **{f'paper_lantern_{c}': (f'{c.capitalize()} Paper Lantern', None, 'self') for c in COLOURS},
    'ship_lantern': ('Ship Lantern', 'pickaxe', 'self'),
    'tiki_torch': ('Tiki Torch', 'axe', 'lower'),
    'jelly_lamp': ('Jelly Lamp', None, 'self'),
    'brazier': ('Brazier', 'pickaxe', 'self'),
    'buoy_red': ('Red Buoy', 'pickaxe', 'self'),
    'buoy_green': ('Green Buoy', 'pickaxe', 'self'),
    'lighthouse_lens': ('Lighthouse Lens', 'pickaxe', 'self'),
    'neon_tube_cyan': ('Cyan Neon Tube', None, 'self'),
    'neon_tube_pink': ('Pink Neon Tube', None, 'self'),
    'driftwood_lamp': ('Driftwood Lamp', 'axe', 'self'),
    'seashell_lamp': ('Seashell Lamp', 'pickaxe', 'self'),
    'ship_cannon': ('Ship Cannon', 'pickaxe', 'self'),
    'ship_anchor': ('Ship Anchor', 'pickaxe', 'self'),
    'treasure_chest': ('Treasure Chest', 'axe', 'container'),
    'gold_coin_pile': ('Gold Coin Pile', 'pickaxe', 'layers'),
    'jolly_roger': ('Jolly Roger', None, 'self'),
    'wanted_poster': ('Wanted Poster', None, 'self'),
    'rum_bottles': ('Rum Bottles', None, 'bottles'),
    'rope_railing': ('Rope Railing', 'axe', 'self'),
    'cargo_net': ('Cargo Net', 'axe', 'self'),
    'powder_keg': ('Powder Keg', 'axe', 'self'),
    'wooden_crate': ('Wooden Crate', 'axe', 'container'),
    'ship_bell': ('Ship Bell', 'pickaxe', 'self'),
    'chart_table': ('Chart Table', 'axe', 'self'),
    'spyglass_stand': ('Spyglass Stand', 'axe', 'self'),
    'figurehead': ('Mermaid Figurehead', 'axe', 'self'),
    'life_ring': ('Life Ring', None, 'self'),
    'hammock': ('Hammock', 'axe', 'head'),
    'deck_chair': ('Deck Chair', 'axe', 'self'),
    'beach_umbrella_red': ('Red Beach Umbrella', 'axe', 'lower'),
    'beach_umbrella_blue': ('Blue Beach Umbrella', 'axe', 'lower'),
    'beach_umbrella_yellow': ('Yellow Beach Umbrella', 'axe', 'lower'),
    **{f'cushion_{c}': (f'{c.capitalize()} Cushion', None, 'self') for c in CUSHIONS},
    'woven_rug': ('Woven Rug', None, 'self'),
    'driftwood_table': ('Driftwood Table', 'axe', 'self'),
    'palm_table': ('Palm Table', 'axe', 'self'),
    'driftwood_chair': ('Driftwood Chair', 'axe', 'self'),
    'palm_chair': ('Palm Chair', 'axe', 'self'),
    'driftwood_shelf': ('Driftwood Shelf', 'axe', 'self'),
    'wind_chime': ('Wind Chime', 'axe', 'self'),
    'aquarium': ('Aquarium', 'pickaxe', 'self'),
    'hanging_planter': ('Hanging Planter', 'axe', 'self'),
}
ITEMS = {'cannonball': 'Cannonball', 'gold_coin': 'Gold Coin'}

LANG = {
    'itemgroup.raft.decor': 'Beach Craft: Decoration',
    'entity.raft.seat': 'Seat',
    'entity.raft.cannonball': 'Cannonball',
    'container.raft.treasure_chest': 'Treasure Chest',
    'container.raft.wooden_crate': 'Wooden Crate',
    'message.raft.ship_cannon.empty': 'Load the cannon with a cannonball',
    'message.raft.hammock.rest': 'You settle into the hammock',
    'message.raft.seat.occupied': 'Someone is already sitting there',
    'tooltip.raft.sit': 'Right-click to sit down',
    'tooltip.raft.ship_cannon': 'Use a cannonball on it to fire',
    'tooltip.raft.hammock': 'Rest by day, sleep by night',
    'tooltip.raft.hammock.1': 'Sets your respawn point',
    'tooltip.raft.brazier': 'Light it with flint and steel, douse it with a shovel or water',
    'tooltip.raft.tiki_torch': 'Light it with flint and steel, douse it with a shovel',
    'tooltip.raft.buoy': 'Floats on water: use it on the water surface',
    'tooltip.raft.gold_coin': 'Nine coins make a coin pile',
    'tooltip.raft.wind_chime': 'Chimes softly in the breeze',
    'tooltip.raft.ship_bell': 'Right-click to ring',
}


# ================================================================================================= data helpers
def _write_loot(block, pools):
    A.write_json(M, f'data/{NS}/loot_table/blocks/{block}.json', {
        'type': 'minecraft:block', 'pools': pools, 'random_sequence': f'{NS}:blocks/{block}'})


def loot(block, kind):
    surv = {'type': 'minecraft:survives_explosion'}
    item = {'type': 'minecraft:item', 'name': rid(block)}
    if kind == 'self':
        _write_loot(block, [{'condition': surv, 'entries': [item], 'rolls': 1}])
    elif kind == 'container':
        e = dict(item, modifier={'type': 'minecraft:copy_components', 'include': ['minecraft:custom_name'],
                                 'source': 'block_entity'})
        _write_loot(block, [{'condition': surv, 'entries': [e], 'rolls': 1}])
    elif kind in ('lower', 'head'):
        prop = {'lower': ('half', 'lower'), 'head': ('part', 'head')}[kind]
        e = dict(item, condition={'type': 'minecraft:match_block', 'blocks': rid(block), 'state': {prop[0]: prop[1]}})
        _write_loot(block, [{'condition': surv, 'entries': [e], 'rolls': 1}])
    elif kind in ('layers', 'bottles'):
        n = 8 if kind == 'layers' else 3
        mods = [{'type': 'minecraft:set_count', 'condition': {'type': 'minecraft:match_block', 'blocks': rid(block),
                                                              'state': {kind: str(k)}}, 'count': k} for k in range(2, n + 1)]
        mods.append({'type': 'minecraft:explosion_decay'})
        _write_loot(block, [{'entries': [dict(item, modifier=mods)], 'rolls': 1}])
    else:
        raise ValueError(kind)


UNLOCKS = []


def _ids(values):
    res = []
    for v in values:
        for x in (v if isinstance(v, list) else [v]):
            res.append(x if x.startswith('#') else rid(x))
    return res


def shaped(name, pattern, key, result, count=1, category='building', group=None):
    p = A.recipe_shaped(M, name, pattern, key, result, count, category)
    d = json.load(open(p))
    if group:
        d['group'] = group
    A.write_json(M, f'data/{NS}/recipe/{name}.json', d)
    UNLOCKS.append((name, _ids(list(key.values()))))


def shapeless(name, ingredients, result, count=1, category='misc', group=None):
    p = A.recipe_shapeless(M, name, ingredients, result, count, category)
    if group:
        d = json.load(open(p))
        d['group'] = group
        A.write_json(M, f'data/{NS}/recipe/{name}.json', d)
    UNLOCKS.append((name, _ids(ingredients)))


def write_unlocks():
    for name, ids in UNLOCKS:
        items = sorted({i for i in ids if not i.startswith('#')})
        tags = sorted({i for i in ids if i.startswith('#')})
        crit = {}
        if items:
            crit['has_ingredient'] = {'conditions': {'items': [{'items': items}]}, 'trigger': 'minecraft:inventory_changed'}
        for k, t in enumerate(tags):
            crit[f'has_tag_{k}'] = {'conditions': {'items': [{'items': t}]}, 'trigger': 'minecraft:inventory_changed'}
        crit['has_the_recipe'] = {'conditions': {'recipes': rid(name)}, 'trigger': 'minecraft:recipe_unlocked'}
        A.write_json(M, f'data/{NS}/advancement/recipes/decorations/{name}.json', {
            'parent': 'minecraft:recipes/root', 'criteria': crit, 'requirements': [list(crit.keys())],
            'rewards': {'recipes': [rid(name)]}})


def guard_foreign_items():
    """Recipes/unlocks that use items of other modules only load when those items exist (fabric:load_conditions)."""
    known = set(BLOCKS) | set(ITEMS)
    base = json.load(open(os.path.join(RESOURCES, 'assets', NS, 'lang', 'en_us.json')))
    for k in base:
        mm = re.match(r'(?:block|item)\.raft\.([a-z0-9_]+)$', k)
        if mm:
            known.add(mm.group(1))
    root = module_dir(M)
    files = glob.glob(os.path.join(root, 'data', NS, 'recipe', '*.json')) + \
        glob.glob(os.path.join(root, 'data', NS, 'advancement', 'recipes', '*', '*.json'))
    for f in files:
        d = json.load(open(f))
        txt = json.dumps({k: v for k, v in d.items() if k not in ('result', 'rewards')})
        foreign = sorted({x for x in re.findall(r'"raft:([a-z0-9_]+)"', txt) if x not in known} -
                         {r.split(':')[1] for r in re.findall(r'"recipes": "(raft:[a-z0-9_]+)"', txt)})
        if foreign:
            d = {'fabric:load_conditions': [{'condition': 'fabric:registry_contains', 'registry': 'minecraft:item',
                                             'values': [rid(x) for x in foreign]}], **d}
            with open(f, 'w', encoding='utf-8') as fh:
                json.dump(d, fh, indent=2)
                fh.write('\n')


# ================================================================================================= recipes
def recipes():
    S, ST, ROPE = 'minecraft:string', 'minecraft:stick', 'raft:rope'
    DP, PP = 'raft:driftwood_planks', 'raft:palm_planks'
    shaped('string_lights_warm', ['SSS', 'GPG'], {'S': S, 'G': 'minecraft:glowstone_dust', 'P': 'raft:plastic'},
           'string_lights_warm', 6)
    shapeless('string_lights_colorful', ['raft:string_lights_warm', 'raft:string_lights_warm', 'minecraft:red_dye',
                                         'minecraft:lime_dye', 'minecraft:blue_dye'], 'string_lights_colorful', 2)
    shaped('bunting_red_white', ['SSS', 'RWR'], {'S': S, 'R': 'minecraft:red_wool', 'W': 'minecraft:white_wool'},
           'bunting_red_white', 3)
    shaped('bunting_rainbow', ['SSS', 'RYB'], {'S': S, 'R': 'minecraft:red_wool', 'Y': 'minecraft:yellow_wool',
                                               'B': 'minecraft:blue_wool'}, 'bunting_rainbow', 3)
    for c in COLOURS:
        shaped(f'paper_lantern_{c}', [' S ', 'PTP', ' D '], {'S': S, 'P': 'minecraft:paper', 'T': 'minecraft:torch',
                                                              'D': f'minecraft:{c}_dye'}, f'paper_lantern_{c}', 1,
               group='paper_lantern')
    shaped('ship_lantern', ['NCN', 'GTG', 'CCC'], {'N': 'minecraft:iron_nugget', 'C': 'minecraft:copper_ingot',
                                                   'G': 'minecraft:glass_pane', 'T': 'minecraft:torch'}, 'ship_lantern')
    shaped('tiki_torch', ['C', 'R', 'B'], {'C': '#minecraft:coals', 'R': ROPE, 'B': 'minecraft:bamboo'}, 'tiki_torch', 2)
    shaped('jelly_lamp', [' N ', 'GJG', 'GGG'], {'N': 'minecraft:iron_nugget', 'G': 'minecraft:glass',
                                                 'J': 'raft:glow_jelly'}, 'jelly_lamp')
    shaped('brazier', ['I I', 'ICI', ' I '], {'I': 'minecraft:iron_ingot', 'C': '#minecraft:coals'}, 'brazier')
    for col in ('red', 'green'):
        shaped(f'buoy_{col}', [' L ', 'PDP', 'PPP'], {'L': 'minecraft:glowstone_dust', 'P': 'raft:plastic',
                                                      'D': f'minecraft:{col}_dye'}, f'buoy_{col}', 2, group='buoy')
    shaped('lighthouse_lens', ['CGC', 'GSG', 'CGC'], {'C': 'minecraft:copper_ingot', 'G': 'minecraft:glass',
                                                      'S': 'minecraft:sea_lantern'}, 'lighthouse_lens')
    for col in ('cyan', 'pink'):
        shaped(f'neon_tube_{col}', [' P ', 'DLD', ' P '], {'P': 'minecraft:glass_pane', 'L': 'minecraft:glowstone_dust',
                                                           'D': f'minecraft:{col}_dye'}, f'neon_tube_{col}', 4, group='neon_tube')
    shaped('driftwood_lamp', ['PPP', 'PTP', ' L '], {'P': 'minecraft:paper', 'T': 'minecraft:torch',
                                                     'L': 'raft:stripped_driftwood_log'}, 'driftwood_lamp')
    shaped('seashell_lamp', ['HHH', 'HTH', ' D '], {'H': 'raft:seashell', 'T': 'minecraft:torch', 'D': DP}, 'seashell_lamp')
    shaped('ship_cannon', ['III', 'WSW'], {'I': 'minecraft:iron_ingot', 'S': 'raft:scrap_metal', 'W': DP}, 'ship_cannon')
    shapeless('cannonball', ['minecraft:iron_ingot', 'minecraft:gunpowder'], 'cannonball', 4)
    shaped('ship_anchor', [' R ', 'SIS', 'I I'], {'R': ROPE, 'S': 'raft:scrap_metal', 'I': 'minecraft:iron_ingot'}, 'ship_anchor')
    shaped('treasure_chest', [' G ', 'ICI', ' I '], {'G': 'minecraft:gold_ingot', 'I': 'minecraft:iron_ingot',
                                                     'C': 'minecraft:chest'}, 'treasure_chest')
    shapeless('gold_coin_pile', ['raft:gold_coin'] * 9, 'gold_coin_pile', 1)
    shapeless('gold_coin_from_pile', ['raft:gold_coin_pile'], 'gold_coin', 9)
    shaped('jolly_roger', ['RSR', 'BWB'], {'R': ROPE, 'S': ST, 'B': 'minecraft:black_wool', 'W': 'minecraft:bone'}, 'jolly_roger')
    shapeless('wanted_poster', ['minecraft:paper', 'minecraft:ink_sac'], 'wanted_poster', 1)
    shapeless('rum_bottles', ['minecraft:glass_bottle', 'minecraft:sugar_cane', 'minecraft:sugar'], 'rum_bottles', 1)
    shaped('rope_railing', ['SRS', 'SRS'], {'S': ST, 'R': ROPE}, 'rope_railing', 4)
    shaped('cargo_net', ['R R', ' R ', 'R R'], {'R': ROPE}, 'cargo_net', 3)
    shaped('powder_keg', ['PIP', 'GGG', 'PIP'], {'P': DP, 'I': 'minecraft:iron_nugget', 'G': 'minecraft:gunpowder'}, 'powder_keg')
    shaped('wooden_crate', ['SPS', 'P P', 'SPS'], {'S': ST, 'P': DP}, 'wooden_crate')
    shaped('ship_bell', [' I ', 'CCC', 'CNC'], {'I': 'minecraft:iron_ingot', 'C': 'minecraft:copper_ingot',
                                                'N': 'minecraft:iron_nugget'}, 'ship_bell')
    shaped('chart_table', ['MMM', 'PPP', 'S S'], {'M': 'minecraft:paper', 'P': DP, 'S': ST}, 'chart_table')
    shaped('spyglass_stand', [' G ', ' S ', 'S S'], {'G': 'minecraft:spyglass', 'S': ST}, 'spyglass_stand')
    shaped('figurehead', [' Y ', 'LLL', ' C '], {'Y': 'minecraft:yellow_dye', 'L': 'raft:stripped_driftwood_log',
                                                 'C': 'minecraft:cyan_dye'}, 'figurehead')
    shaped('life_ring', ['PRP', 'R R', 'PRP'], {'P': 'raft:plastic', 'R': 'minecraft:red_dye'}, 'life_ring', 2)
    shaped('hammock', ['WWW', 'RSR'], {'W': '#minecraft:wool', 'R': ROPE, 'S': ST}, 'hammock')
    shaped('deck_chair', ['S  ', 'SWW', 'S S'], {'S': ST, 'W': '#minecraft:wool'}, 'deck_chair')
    for col in ('red', 'blue', 'yellow'):
        shaped(f'beach_umbrella_{col}', ['WWW', ' S ', ' S '], {'W': f'minecraft:{col}_wool', 'S': ST},
               f'beach_umbrella_{col}', 1, group='beach_umbrella')
    for col in CUSHIONS:
        shaped(f'cushion_{col}', ['WFW'], {'W': f'minecraft:{col}_wool', 'F': 'minecraft:feather'}, f'cushion_{col}', 2,
               group='cushion')
    shaped('woven_rug', ['TT'], {'T': 'raft:palm_thatch'}, 'woven_rug', 3)
    for wood, planks in (('driftwood', DP), ('palm', PP)):
        shaped(f'{wood}_table', ['PPP', 'S S', 'S S'], {'P': planks, 'S': ST}, f'{wood}_table', 1, group='decor_table')
        shaped(f'{wood}_chair', ['S  ', 'PPP', 'S S'], {'P': planks, 'S': ST}, f'{wood}_chair', 2, group='decor_chair')
    shaped('driftwood_shelf', ['BGB', 'PPP'], {'B': 'minecraft:glass_bottle', 'G': 'raft:sea_glass_shard', 'P': DP}, 'driftwood_shelf')
    shaped('wind_chime', [' S ', 'SPS', 'H H'], {'S': S, 'P': DP, 'H': 'raft:seashell'}, 'wind_chime')
    shaped('aquarium', ['GGG', 'GFG', 'PSP'], {'G': 'minecraft:glass', 'F': 'minecraft:tropical_fish',
                                               'P': '#minecraft:planks', 'S': 'minecraft:sand'}, 'aquarium')
    shaped('hanging_planter', [' R ', 'FDF', 'BBB'], {'R': ROPE, 'F': '#minecraft:small_flowers', 'D': 'minecraft:dirt',
                                                      'B': 'minecraft:bamboo'}, 'hanging_planter')


# ================================================================================================= main
def main():
    out = module_dir(M)
    if os.path.isdir(out):
        shutil.rmtree(out)
    os.makedirs(out)
    lights.build()
    pirate.build()
    furniture.build()

    for b, (label, tool, kind) in BLOCKS.items():
        loot(b, kind)
        if tool:
            A.tag(M, 'block', f'minecraft:mineable/{tool}', [b])
        LANG[f'block.raft.{b}'] = label
    for i, label in ITEMS.items():
        LANG[f'item.raft.{i}'] = label
    A.tag(M, 'block', 'minecraft:climbable', ['cargo_net'])
    A.tag(M, 'block', 'minecraft:fences', ['rope_railing'])
    A.tag(M, 'block', 'minecraft:wooden_fences', ['rope_railing'])
    A.tag(M, 'item', 'minecraft:fences', ['rope_railing'])
    A.tag(M, 'item', 'minecraft:wooden_fences', ['rope_railing'])
    A.tag(M, 'block', 'minecraft:dampens_vibrations', ['woven_rug'] + [f'cushion_{c}' for c in CUSHIONS])
    A.tag(M, 'block', 'minecraft:combination_step_sound_blocks', ['woven_rug'])
    A.tag(M, 'block', 'minecraft:beds', ['hammock'])
    recipes()
    write_unlocks()
    guard_foreign_items()
    A.lang(M, LANG)
    A.flush(M)
    mdl.write_java_shapes(os.path.join(ROOT, 'src', 'main', 'java', 'net', 'kasax', 'raft', 'content', 'decor', 'block',
                                       'DecorShapeData.java'))
    n = sum(len(fs) for _, _, fs in os.walk(out))
    print(f'decor: {len(BLOCKS)} blocks, {len(ITEMS)} items, {len(mdl.SHAPES)} shapes, {n} files')


if __name__ == '__main__':
    main()
