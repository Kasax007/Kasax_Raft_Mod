"""Recipes of the cozy module, plus a small recipe book helper shared with the food and gear modules.

Every recipe gets a recipe-book unlock advancement (unlocked when the player holds one of its ingredients). Recipes
that use items of other modules get fabric:load_conditions, so a missing module never breaks loading.
"""
import glob
import json
import os
import re

from bclib import assets as A
from bclib.paths import module_dir, MODULES, RESOURCES

rid = A.rid
NS = 'raft'


def known_ids():
    """Every raft block/item id named in a lang file (all modules + the 2.0 resources)."""
    ids = set()
    files = glob.glob(os.path.join(MODULES, '*', 'assets', NS, 'lang', 'en_us.json'))
    files.append(os.path.join(RESOURCES, 'assets', NS, 'lang', 'en_us.json'))
    for f in files:
        try:
            for k in json.load(open(f)):
                mm = re.match(r'(?:block|item)\.raft\.([a-z0-9_]+)$', k)
                if mm:
                    ids.add(mm.group(1))
        except (OSError, ValueError):
            pass
    return ids


class Book:
    def __init__(self, module, own_ids):
        self.module = module
        self.own = set(own_ids)
        self.unlocks = []

    def _ids(self, values):
        res = []
        for v in values:
            for x in (v if isinstance(v, list) else [v]):
                res.append(x if x.startswith('#') else rid(x))
        return res

    def shaped(self, name, pattern, key, result, count=1, category='building', group=None):
        p = A.recipe_shaped(self.module, name, pattern, key, result, count, category)
        if group:
            d = json.load(open(p))
            d['group'] = group
            A.write_json(self.module, f'data/{NS}/recipe/{name}.json', d)
        self.unlocks.append((name, self._ids(list(key.values()))))

    def shapeless(self, name, ingredients, result, count=1, category='misc', group=None):
        p = A.recipe_shapeless(self.module, name, ingredients, result, count, category)
        if group:
            d = json.load(open(p))
            d['group'] = group
            A.write_json(self.module, f'data/{NS}/recipe/{name}.json', d)
        self.unlocks.append((name, self._ids(ingredients)))

    def cooking(self, name, ingredient, result, xp=0.35, time=200, kinds=('smelting', 'smoking', 'campfire_cooking')):
        """Food cooked in a furnace, smoker and on a campfire (smoker half, campfire triple the time)."""
        for kind in kinds:
            n = name if kind == 'smelting' else f'{name}_from_{kind}'
            t = {'smelting': time, 'smoking': time // 2, 'campfire_cooking': time * 3, 'blasting': time // 2}[kind]
            A.recipe_cooking(self.module, n, ingredient, result, xp, t, kind, 'food')
            self.unlocks.append((n, self._ids([ingredient])))

    def stonecutting(self, name, ingredient, result, count=1):
        A.recipe_stonecutting(self.module, name, ingredient, result, count)
        self.unlocks.append((name, self._ids([ingredient])))

    def finish(self, folder='decorations'):
        for name, ids in self.unlocks:
            items = sorted({i for i in ids if not i.startswith('#')})
            tags = sorted({i for i in ids if i.startswith('#')})
            crit = {}
            if items:
                crit['has_ingredient'] = {'conditions': {'items': [{'items': items}]},
                                          'trigger': 'minecraft:inventory_changed'}
            for k, t in enumerate(tags):
                crit[f'has_tag_{k}'] = {'conditions': {'items': [{'items': t}]}, 'trigger': 'minecraft:inventory_changed'}
            crit['has_the_recipe'] = {'conditions': {'recipes': rid(name)}, 'trigger': 'minecraft:recipe_unlocked'}
            A.write_json(self.module, f'data/{NS}/advancement/recipes/{folder}/{name}.json', {
                'parent': 'minecraft:recipes/root', 'criteria': crit, 'requirements': [list(crit.keys())],
                'rewards': {'recipes': [rid(name)]}})
        self.guard_foreign()

    def guard_foreign(self):
        known = self.own | known_ids()
        root = module_dir(self.module)
        files = glob.glob(os.path.join(root, 'data', NS, 'recipe', '*.json')) + \
            glob.glob(os.path.join(root, 'data', NS, 'advancement', 'recipes', '*', '*.json'))
        # the ids of this module itself are always there; ids of other modules get a load condition
        for f in files:
            d = json.load(open(f))
            txt = json.dumps({k: v for k, v in d.items() if k not in ('result', 'rewards')})
            used = set(re.findall(r'"raft:([a-z0-9_]+)"', txt)) - \
                {r.split(':')[1] for r in re.findall(r'"recipes": "(raft:[a-z0-9_]+)"', txt)}
            foreign = sorted(x for x in used if x not in self.own)
            missing = [x for x in foreign if x not in known]
            if missing:
                raise SystemExit(f'{self.module}: unknown ids {missing} in {os.path.basename(f)}')
            if foreign:
                d = {'fabric:load_conditions': [{'condition': 'fabric:registry_contains', 'registry': 'minecraft:item',
                                                 'values': [rid(x) for x in foreign]}], **d}
                with open(f, 'w', encoding='utf-8') as fh:
                    json.dump(d, fh, indent=2)
                    fh.write('\n')


def cozy_recipes(M, BLOCKS):
    b = Book(M, BLOCKS)
    S, ST, ROPE = 'minecraft:string', 'minecraft:stick', 'raft:rope'
    PP, TP, WP, DP = 'raft:palm_planks', 'raft:teak_planks', 'raft:weathered_planks', 'raft:driftwood_planks'
    BAM = 'minecraft:bamboo'
    for wood, planks, slab in (('palm', PP, 'raft:palm_slab'), ('teak', TP, 'raft:teak_slab')):
        b.shaped(f'{wood}_bench', ['  P', 'SSS', 'P P'], {'P': planks, 'S': slab}, f'{wood}_bench', 2, group='cozy_bench')
    b.shaped('bar_stool', ['W', 'B', 'B'], {'W': '#minecraft:wool', 'B': BAM}, 'bar_stool', 2)
    b.shaped('sun_lounger', ['  W', 'WWW', 'P P'], {'W': '#minecraft:wool', 'P': PP}, 'sun_lounger')
    b.shaped('rattan_armchair', ['B  ', 'BWB', 'BBB'], {'B': BAM, 'W': '#minecraft:wool'}, 'rattan_armchair')
    b.shaped('teak_table', ['PPP', ' S ', 'SSS'], {'P': TP, 'S': 'raft:teak_slab'}, 'teak_table')
    b.shaped('bamboo_table', ['PPP', 'B B', 'B B'], {'P': 'minecraft:bamboo_planks', 'B': BAM}, 'bamboo_table')
    b.shaped('serving_plate', ['C C', 'CCC'], {'C': 'minecraft:clay_ball'}, 'serving_plate', 2)
    for wood, log in (('palm', 'raft:stripped_palm_log'), ('teak', TP), ('weathered', WP)):
        b.shaped(f'{wood}_shelf', ['LLL', '   ', 'LLL'], {'L': log}, f'{wood}_shelf', 6, group='cozy_shelf')
    b.shaped('wicker_basket', ['B B', 'BCB', 'BBB'], {'B': BAM, 'C': 'minecraft:chest'}, 'wicker_basket')
    for col, (d1, d2) in (('red', ('red', 'white')), ('blue', ('blue', 'white')), ('yellow', ('yellow', 'orange'))):
        b.shaped(f'beach_towel_{col}', ['AB'], {'A': f'minecraft:{d1}_wool', 'B': f'minecraft:{d2}_wool'},
                 f'beach_towel_{col}', 4, group='beach_towel')
    b.shaped('rope_rug', ['RR', 'RR'], {'R': ROPE}, 'rope_rug', 2)
    b.shaped('hurricane_lantern', [' N ', 'GTG', 'NIN'], {'N': 'minecraft:copper_nugget', 'G': 'minecraft:glass_pane',
                                                          'T': 'minecraft:torch', 'I': 'minecraft:copper_ingot'},
             'hurricane_lantern')
    b.shapeless('shell_candle', ['raft:seashell', 'minecraft:candle'], 'shell_candle', 1)
    b.shaped('sea_glass_lamp', ['GGG', 'GTG', ' L '], {'G': 'raft:sea_glass_shard', 'T': 'minecraft:torch',
                                                       'L': 'raft:stripped_driftwood_log'}, 'sea_glass_lamp')
    b.shaped('driftwood_sconce', ['C', 'I', 'L'], {'C': 'minecraft:candle', 'I': 'minecraft:copper_nugget',
                                                   'L': 'raft:stripped_driftwood_log'}, 'driftwood_sconce', 2)
    b.shaped('potted_monstera', ['FF', 'PF'], {'F': 'raft:tropical_fern', 'P': 'minecraft:flower_pot'}, 'potted_monstera')
    b.shapeless('coconut_planter', ['raft:coconut', 'minecraft:dirt', '#minecraft:small_flowers'], 'coconut_planter')
    b.shaped('flower_box', ['FDF', 'PPP'], {'F': '#minecraft:small_flowers', 'D': 'minecraft:dirt', 'P': DP},
             'flower_box', 2)
    b.shaped('coral_vase', ['C', 'G'], {'C': ['minecraft:tube_coral', 'minecraft:brain_coral', 'minecraft:bubble_coral', 'minecraft:fire_coral', 'minecraft:horn_coral'], 'G': 'raft:sea_glass_shard'}, 'coral_vase')
    b.shapeless('conch_shell', ['raft:seashell', 'raft:seashell', 'raft:seashell', 'minecraft:pink_dye'], 'conch_shell')
    b.shaped('steel_drum', ['III', 'IBI', 'S S'], {'I': 'minecraft:iron_ingot', 'B': 'minecraft:blue_dye',
                                                   'S': 'raft:scrap_metal'}, 'steel_drum')
    b.shaped('shell_garland', ['SSS', 'HHH'], {'S': S, 'H': 'raft:seashell'}, 'shell_garland', 3)
    b.shaped('fishing_net', ['S S', ' S ', 'SGS'], {'S': S, 'G': 'raft:sea_glass_shard'}, 'fishing_net', 2)
    b.shaped('dock_bollard', ['III', ' I ', 'IRI'], {'I': 'minecraft:iron_ingot', 'R': ROPE}, 'dock_bollard')
    for col in ('red', 'blue', 'yellow'):
        b.shaped(f'surfboard_{col}', ['P', 'D', 'P'], {'P': 'raft:plastic', 'D': f'minecraft:{col}_dye'},
                 f'surfboard_{col}', 1, group='surfboard')
    b.shaped('fish_trophy', ['F', 'P'], {'F': '#minecraft:fishes', 'P': TP}, 'fish_trophy')
    b.shaped('bamboo_blinds', ['BBB', 'SBS', 'BBB'], {'B': BAM, 'S': S}, 'bamboo_blinds', 2)
    b.shaped('thatch_parasol', ['TTT', ' B ', ' B '], {'T': 'raft:palm_thatch', 'B': BAM}, 'thatch_parasol')
    b.shaped('shell_tiles', ['SS', 'SS'], {'S': 'raft:shell_sand'}, 'shell_tiles', 4)
    b.shaped('pumice_bricks', ['PP', 'PP'], {'P': 'raft:pumice'}, 'pumice_bricks', 4)
    for base, stem in (('shell_tiles', 'shell_tile'), ('pumice_bricks', 'pumice_brick')):
        b.shaped(f'{stem}_stairs', ['X  ', 'XX ', 'XXX'], {'X': base}, f'{stem}_stairs', 4)
        b.shaped(f'{stem}_slab', ['XXX'], {'X': base}, f'{stem}_slab', 6)
        b.shaped(f'{stem}_wall', ['XXX', 'XXX'], {'X': base}, f'{stem}_wall', 6)
        b.stonecutting(f'{stem}_stairs_from_stonecutting', base, f'{stem}_stairs')
        b.stonecutting(f'{stem}_slab_from_stonecutting', base, f'{stem}_slab', 2)
        b.stonecutting(f'{stem}_wall_from_stonecutting', base, f'{stem}_wall')
    b.shaped('driftwood_lattice', ['SPS', 'PSP'], {'S': ST, 'P': DP}, 'driftwood_lattice', 8)
    b.finish('decorations')
