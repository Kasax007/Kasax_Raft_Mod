"""Generator of the "building" module (docs/design/beachcraft3.md, section 5).

Run:  cd tools && python3 -m content.building.gen
Writes textures, models, blockstates, item definitions, loot tables, recipes, tags and lang into
src/main/modules/building/. Re-runnable: it wipes its own output folder first.
"""
import os
import shutil
import sys

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..')))

from bclib import assets as A  # noqa: E402
from bclib import extrude, tex  # noqa: E402
from bclib.paths import module_dir  # noqa: E402

from content.building import tex_metal as TM  # noqa: E402
from content.building import tex_tech as TT  # noqa: E402
from content.building import tex_wood_stone as TW  # noqa: E402

M = 'building'
NS = 'raft'
rid = A.rid


# ================================================================================================= small writers
def face(texture, uv=None, cull=None, rot=None, tint=None):
    f = {}
    if uv is not None:
        f['uv'] = list(uv)
    f['texture'] = '#' + texture
    if cull:
        f['cullface'] = cull
    if rot:
        f['rotation'] = rot
    return f


def el(frm, to, faces, rotation=None, light=None, shade=None):
    e = {'from': list(frm), 'to': list(to)}
    if rotation:
        e['rotation'] = rotation
    if light:
        e['light_emission'] = light
    if shade is not None:
        e['shade'] = shade
    e['faces'] = faces
    return e


def box_faces(texture, frm, to, cull_outer=True, skip=(), uvs=None):
    """Faces of a box with UVs taken from the box position (like vanilla auto-UV), optional per-face overrides."""
    x0, y0, z0 = frm
    x1, y1, z1 = to
    auto = {
        'down': [x0, 16 - z1, x1, 16 - z0], 'up': [x0, z0, x1, z1],
        'north': [16 - x1, 16 - y1, 16 - x0, 16 - y0], 'south': [x0, 16 - y1, x1, 16 - y0],
        'west': [z0, 16 - y1, z1, 16 - y0], 'east': [16 - z1, 16 - y1, 16 - z0, 16 - y0],
    }
    edge = {'down': y0 == 0, 'up': y1 == 16, 'north': z0 == 0, 'south': z1 == 16, 'west': x0 == 0, 'east': x1 == 16}
    out = {}
    for d in ('down', 'up', 'north', 'south', 'west', 'east'):
        if d in skip:
            continue
        t = texture[d] if isinstance(texture, dict) else texture
        if t is None:
            continue
        uv = (uvs or {}).get(d, auto[d])
        out[d] = face(t, uv, d if (cull_outer and edge[d]) else None)
    return out


def model(name, textures, elements, parent='minecraft:block/block', ao=None, display=None):
    d = {'parent': parent}
    if ao is not None:
        d['ambientocclusion'] = ao
    d['textures'] = {k: (v if v.startswith('#') else rid(v)) for k, v in textures.items()}
    if display:
        d['display'] = display
    d['elements'] = elements
    return A.write_json(M, f'assets/{NS}/models/block/{name}.json', d)


def axis_states(name, model_name=None):
    m = rid('block/' + (model_name or name))
    A.blockstate(M, name, {'axis=y': {'model': m}, 'axis=z': {'model': m, 'x': 90},
                           'axis=x': {'model': m, 'x': 90, 'y': 90}})


def facing_states(name, model_name=None):
    A.blockstate(M, name, A.horizontal_variants('block/' + (model_name or name)))


# ------------------------------------------------------------------------------------------------- loot (26.3 format)
def _write_loot(block, pool):
    A.write_json(M, f'data/{NS}/loot_table/blocks/{block}.json', {
        'type': 'minecraft:block', 'pools': [pool], 'random_sequence': f'{NS}:blocks/{block}'})


def loot_self(block):
    _write_loot(block, {'condition': {'type': 'minecraft:survives_explosion'},
                        'entries': [{'type': 'minecraft:item', 'name': rid(block)}], 'rolls': 1})


def loot_machine(block):
    """A machine drops itself with the energy it held (3.5: the raft:stored_energy component, StoredEnergy.java)."""
    _write_loot(block, {'condition': {'type': 'minecraft:survives_explosion'},
                        'entries': [{'type': 'minecraft:item', 'name': rid(block), 'modifier': {
                            'type': 'minecraft:copy_components', 'include': ['raft:stored_energy'],
                            'source': 'block_entity'}}], 'rolls': 1})


def loot_silk(block):
    _write_loot(block, {'condition': 'minecraft:tool/can_silk_touch',
                        'entries': [{'type': 'minecraft:item', 'name': rid(block)}], 'rolls': 1})


def loot_slab(block):
    _write_loot(block, {'entries': [{'type': 'minecraft:item', 'modifier': [
        {'type': 'minecraft:set_count', 'condition': {'type': 'minecraft:match_block', 'blocks': rid(block),
                                                      'state': {'type': 'double'}}, 'count': 2},
        {'type': 'minecraft:explosion_decay'}], 'name': rid(block)}], 'rolls': 1})


def loot_door(block):
    _write_loot(block, {'condition': {'type': 'minecraft:survives_explosion'}, 'entries': [{
        'type': 'minecraft:item', 'condition': {'type': 'minecraft:match_block', 'blocks': rid(block),
                                                'state': {'half': 'lower'}}, 'name': rid(block)}], 'rolls': 1})


# ------------------------------------------------------------------------------------------------- recipes
UNLOCKS = []  # (recipe name, category folder, ingredient ids) -> recipe book unlock advancements


def _ids(values):
    out = []
    for v in values:
        for x in (v if isinstance(v, list) else [v]):
            out.append(x if x.startswith('#') else rid(x))
    return out


def _unlock(name, category, ingredients):
    UNLOCKS.append((name, {'building': 'building_blocks', 'decorations': 'decorations', 'redstone': 'redstone',
                           'misc': 'misc', 'blocks': 'building_blocks'}.get(category, 'misc'), _ids(ingredients)))


def shaped(name, pattern, key, result, count=1, category='building', group=None):
    p = A.recipe_shaped(M, name, pattern, key, result, count, category)
    if group:
        import json
        d = json.load(open(p))
        d['group'] = group
        A.write_json(M, f'data/{NS}/recipe/{name}.json', d)
    _unlock(name, category, list(key.values()))


def shapeless(name, ingredients, result, count=1, category='building'):
    A.recipe_shapeless(M, name, ingredients, result, count, category)
    _unlock(name, category, ingredients)


def cut(base, result, count=1):
    n = f'{result}_from_{base}_stonecutting'
    A.recipe_stonecutting(M, n, base, result, count)
    _unlock(n, 'building', [base])


def smelt(name, ingredient, result):
    A.recipe_cooking(M, name, rid(ingredient), result, 0.1, 200, 'smelting', 'blocks')
    _unlock(name, 'building', [ingredient])


def guard_foreign_items():
    """Recipes and unlock advancements that use items of other modules (e.g. raft:sea_glass_shard of `world`) only
    load when that item exists, so a missing item never breaks registry loading (fabric:load_conditions)."""
    import glob
    import json
    import re
    from bclib.paths import RESOURCES
    known = set()
    base_lang = json.load(open(os.path.join(RESOURCES, 'assets', NS, 'lang', 'en_us.json')))
    for k in list(base_lang) + list(LANG):
        m = re.match(r'(?:block|item)\.raft\.([a-z0-9_]+)$', k)
        if m:
            known.add(m.group(1))
    root = module_dir(M)
    files = glob.glob(os.path.join(root, 'data', NS, 'recipe', '*.json')) + \
        glob.glob(os.path.join(root, 'data', NS, 'advancement', 'recipes', '*', '*.json'))
    for f in files:
        d = json.load(open(f))
        txt = json.dumps({k: v for k, v in d.items() if k not in ('result', 'rewards')})
        foreign = sorted({x for x in re.findall(r'"raft:([a-z0-9_]+)"', txt)
                          if x not in known and not f.endswith(f'/{x}.json')} - {
            r.split(':')[1] for r in re.findall(r'"recipes": "(raft:[a-z0-9_]+)"', txt)})
        if foreign:
            d = {'fabric:load_conditions': [{'condition': 'fabric:registry_contains', 'registry': 'minecraft:item',
                                             'values': [rid(x) for x in foreign]}], **d}
            with open(f, 'w', encoding='utf-8') as fh:
                json.dump(d, fh, indent=2)
                fh.write('\n')


def write_unlocks():
    for name, folder, ids in UNLOCKS:
        items = [i for i in ids if not i.startswith('#')]
        tags = [i for i in ids if i.startswith('#')]
        crit = {}
        if items:
            crit['has_ingredient'] = {'conditions': {'items': [{'items': sorted(set(items))}]},
                                      'trigger': 'minecraft:inventory_changed'}
        for k, t in enumerate(sorted(set(tags))):
            crit[f'has_tag_{k}'] = {'conditions': {'items': [{'items': t}]}, 'trigger': 'minecraft:inventory_changed'}
        crit['has_the_recipe'] = {'conditions': {'recipes': rid(name)}, 'trigger': 'minecraft:recipe_unlocked'}
        A.write_json(M, f'data/{NS}/advancement/recipes/{folder}/{name}.json', {
            'parent': 'minecraft:recipes/root', 'criteria': crit, 'requirements': [list(crit.keys())],
            'rewards': {'recipes': [rid(name)]}})


TAGS = {}


def tag(registry, tag_id, *values):
    A.tag(M, registry, tag_id, list(values))


def both(tag_id, *values):
    """Block and item tag with the same name (stairs, slabs, planks ...)."""
    tag('block', tag_id, *values)
    tag('item', tag_id, *values)


LANG = {}


def name(block, text):
    LANG[f'block.{NS}.{block}'] = text


# ================================================================================================= textures
def textures():
    S = lambda img, rel: tex.save(img, M, rel)  # noqa: E731
    # wood
    S(TW.weathered_planks(), 'block/weathered_planks')
    S(TW.teak_planks(), 'block/teak_planks')
    bottom, top = TW.teak_door_bottom(), TW.teak_door_top()
    S(bottom, 'block/teak_door_bottom')
    S(top, 'block/teak_door_top')
    S(tex.outline(TW.teak_door_item(bottom, top)), 'item/teak_door')
    S(TW.teak_trapdoor(), 'block/teak_trapdoor')
    bottom, top = TW.weathered_door_bottom(), TW.weathered_door_top()
    S(bottom, 'block/weathered_door_bottom')
    S(top, 'block/weathered_door_top')
    S(tex.outline(TW.teak_door_item(bottom, top)), 'item/weathered_door')
    S(TW.weathered_trapdoor(), 'block/weathered_trapdoor')
    # sea glass
    for c in ('aqua', 'green', 'blue'):
        S(TW.sea_glass(c), f'block/{c}_sea_glass')
        S(TW.sea_glass_pane_top(c), f'block/{c}_sea_glass_pane_top')
    # coral stone
    S(TW.coral_stone(), 'block/coral_stone')
    S(TW.polished_coral_stone(), 'block/polished_coral_stone')
    S(TW.coral_stone_bricks(), 'block/coral_stone_bricks')
    S(TW.mossy_coral_stone_bricks(), 'block/mossy_coral_stone_bricks')
    # tidestone
    S(TW.tidestone(), 'block/tidestone')
    S(TW.tidestone_bricks(), 'block/tidestone_bricks')
    S(TW.cracked_tidestone_bricks(), 'block/cracked_tidestone_bricks')
    S(TW.mossy_tidestone_bricks(), 'block/mossy_tidestone_bricks')
    S(TW.chiseled_tidestone(), 'block/chiseled_tidestone')
    S(TW.tidestone_pillar(), 'block/tidestone_pillar')
    S(TW.tidestone_pillar_top(), 'block/tidestone_pillar_top')
    S(TW.glowing_tidestone(), 'block/glowing_tidestone')
    tex.save_animated(TW.glowing_tidestone_runes(), M, 'block/glowing_tidestone_runes', frametime=5, interpolate=True)
    # drowned city
    S(TM.cracked_concrete(), 'block/cracked_concrete')
    S(TM.mossy_concrete(), 'block/mossy_concrete')
    S(TM.rebar_concrete(), 'block/rebar_concrete')
    S(TM.rebar_rod(), 'block/rebar_rod')
    S(TM.asphalt(), 'block/asphalt')
    S(TM.asphalt_line(), 'block/asphalt_line')
    S(TM.cracked_glass(), 'block/cracked_glass')
    S(TM.cracked_glass_pane_top(), 'block/cracked_glass_pane_top')
    # steel
    S(TM.riveted_hull(), 'block/riveted_hull')
    S(TM.rusty_hull(), 'block/rusty_hull')
    S(TM.corrugated_metal(), 'block/corrugated_metal')
    S(TM.rusted_plate(), 'block/rusted_plate')
    S(TM.steel_beam(), 'block/steel_beam')
    S(TM.steel_beam_inner(), 'block/steel_beam_inner')
    S(TM.steel_beam_end(), 'block/steel_beam_end')
    S(TM.catwalk(), 'block/catwalk')
    S(TM.catwalk_side(), 'block/catwalk_side')
    S(TM.steel_railing(), 'block/steel_railing')
    S(TM.porthole(), 'block/porthole')
    bottom, top = TM.bulkhead_door_bottom(), TM.bulkhead_door_top()
    S(bottom, 'block/bulkhead_door_bottom')
    S(top, 'block/bulkhead_door_top')
    S(tex.outline(TM.bulkhead_door_item(bottom, top)), 'item/bulkhead_door')
    S(TM.metal_hatch(), 'block/metal_hatch')
    S(TM.rusty_pipe(), 'block/rusty_pipe')
    S(TM.rusty_pipe_end(), 'block/rusty_pipe_end')
    S(TM.valve_wheel(), 'block/rusty_pipe_valve_wheel')
    for c in ('red', 'blue', 'green'):
        S(TM.container_side(c), f'block/shipping_container_{c}_side')
        S(TM.container_end(c), f'block/shipping_container_{c}_end')
        S(TM.container_top(c), f'block/shipping_container_{c}_top')
    # tech
    S(TT.control_panel_side(), 'block/control_panel_side')
    S(TT.control_panel_front(), 'block/control_panel_front')
    S(TT.control_panel_top(), 'block/control_panel_top')
    tex.save_animated(TT.control_panel_lights(), M, 'block/control_panel_lights', frametime=5)
    S(TT.control_panel_screen(), 'block/control_panel_screen')
    tex.save_animated(TT.control_panel_screen_glow(), M, 'block/control_panel_screen_glow', frametime=3)
    S(TT.server_rack_front(), 'block/server_rack_front')
    tex.save_animated(TT.server_rack_leds(), M, 'block/server_rack_leds', frametime=4)
    S(TT.server_rack_side(), 'block/server_rack_side')
    S(TT.server_rack_top(), 'block/server_rack_top')
    S(TT.satellite_dish(), 'block/satellite_dish')
    S(TT.satellite_dish_back(), 'block/satellite_dish_back')
    S(TT.satellite_dish_mount(), 'block/satellite_dish_mount')
    S(TT.wind_turbine_body(), 'block/wind_turbine_body')
    S(TT.wind_turbine_front(), 'block/wind_turbine_front')
    S(TT.wind_turbine_pole(), 'block/wind_turbine_pole')
    S(TT.wind_turbine_blade(), 'block/wind_turbine_blade')
    S(TT.wind_turbine_rotor(), 'entity/wind_turbine_rotor')


# ================================================================================================= families
def simple(block, label, texture=None, loot=loot_self):
    A.simple_block(M, block, texture)
    loot(block)
    name(block, label)


def stairs(block, label, texture):
    A.stairs(M, block, texture)
    loot_self(block)
    name(block, label)
    both('minecraft:stairs', block)


def slab(block, label, texture, full_model):
    A.slab(M, block, texture, full_model)
    loot_slab(block)
    name(block, label)
    both('minecraft:slabs', block)


def wall(block, label, texture):
    A.wall(M, block, texture)
    loot_self(block)
    name(block, label)
    both('minecraft:walls', block)


def wood_family(wood, label, extra_door=False):
    planks = f'{wood}_planks'
    t = f'block/{planks}'
    simple(planks, f'{label} Planks')
    stairs(f'{wood}_stairs', f'{label} Stairs', t)
    slab(f'{wood}_slab', f'{label} Slab', t, t)
    A.fence(M, f'{wood}_fence', t)
    loot_self(f'{wood}_fence')
    name(f'{wood}_fence', f'{label} Fence')
    A.fence_gate(M, f'{wood}_fence_gate', t)
    loot_self(f'{wood}_fence_gate')
    name(f'{wood}_fence_gate', f'{label} Fence Gate')
    both('minecraft:planks', planks)
    both('minecraft:wooden_stairs', f'{wood}_stairs')
    both('minecraft:wooden_slabs', f'{wood}_slab')
    both('minecraft:wooden_fences', f'{wood}_fence')
    both('minecraft:fence_gates', f'{wood}_fence_gate')
    tag('block', 'minecraft:mineable/axe', planks, f'{wood}_stairs', f'{wood}_slab', f'{wood}_fence',
        f'{wood}_fence_gate')
    # recipes
    P = planks
    shaped(f'{wood}_stairs', ['P  ', 'PP ', 'PPP'], {'P': P}, f'{wood}_stairs', 4, group='wooden_stairs')
    shaped(f'{wood}_slab', ['PPP'], {'P': P}, f'{wood}_slab', 6, group='wooden_slab')
    shaped(f'{wood}_fence', ['P#P', 'P#P'], {'P': P, '#': 'minecraft:stick'}, f'{wood}_fence', 3,
           category='misc', group='wooden_fence')
    shaped(f'{wood}_fence_gate', ['#P#', '#P#'], {'P': P, '#': 'minecraft:stick'}, f'{wood}_fence_gate', 1,
           category='redstone', group='wooden_fence_gate')


def stone_set(base, label, stairs_=None, slab_=None, wall_=None, cut_from=()):
    """Stairs/slab/wall of a base block with crafting and stonecutting recipes (also from cut_from blocks)."""
    t = f'block/{base}'
    if stairs_:
        stairs(stairs_, f'{label} Stairs', t)
        shaped(stairs_, ['B  ', 'BB ', 'BBB'], {'B': base}, stairs_, 4)
    if slab_:
        slab(slab_, f'{label} Slab', t, t)
        shaped(slab_, ['BBB'], {'B': base}, slab_, 6)
    if wall_:
        wall(wall_, f'{label} Wall', t)
        shaped(wall_, ['BBB', 'BBB'], {'B': base}, wall_, 6, category='misc')
    for src in (base,) + tuple(cut_from):
        if stairs_:
            cut(src, stairs_)
        if slab_:
            cut(src, slab_, 2)
        if wall_:
            cut(src, wall_)


# ================================================================================================= blocks
def wood():
    wood_family('weathered', 'Weathered')
    shaped('weathered_planks', [' P ', 'PBP', ' P '],
           {'P': ['raft:driftwood_planks', 'raft:palm_planks'], 'B': 'minecraft:bone_meal'}, 'weathered_planks', 4,
           group='planks')
    wood_family('teak', 'Teak')
    shapeless('teak_planks', ['minecraft:dark_oak_planks', 'raft:palm_planks'], 'teak_planks', 2)
    wood_doors('teak', 'Teak', 'Teak Hatch Grating')
    wood_doors('weathered', 'Weathered', 'Weathered Hatch')


def wood_doors(wood, label, hatch_label):
    door, hatch = f'{wood}_door', f'{wood}_trapdoor'
    A.door(M, door, f'block/{door}_bottom', f'block/{door}_top')
    loot_door(door)
    name(door, f'{label} Door')
    A.trapdoor(M, hatch, f'block/{hatch}')
    loot_self(hatch)
    name(hatch, hatch_label)
    both('minecraft:doors', door)
    both('minecraft:wooden_doors', door)
    both('minecraft:trapdoors', hatch)
    both('minecraft:wooden_trapdoors', hatch)
    tag('block', 'minecraft:mineable/axe', door, hatch)
    shaped(door, ['PP', 'PP', 'PP'], {'P': f'{wood}_planks'}, door, 3, category='redstone', group='wooden_door')
    shaped(hatch, ['PPP', 'PPP'], {'P': f'{wood}_planks'}, hatch, 2, category='redstone', group='wooden_trapdoor')


def sea_glass():
    labels = {'aqua': 'Aqua', 'green': 'Green', 'blue': 'Blue'}
    for c, lab in labels.items():
        b = f'{c}_sea_glass'
        simple(b, f'{lab} Sea Glass')
        A.pane(M, f'{b}_pane', f'block/{b}', f'block/{b}_pane_top')
        loot_self(f'{b}_pane')
        name(f'{b}_pane', f'{lab} Sea Glass Pane')
        tag('block', 'minecraft:impermeable', b)
        both('c:glass_blocks', b)
        both('c:glass_panes', f'{b}_pane')
        shaped(f'{b}_pane', ['GGG', 'GGG'], {'G': b}, f'{b}_pane', 16, category='misc')
    S = 'raft:sea_glass_shard'
    shaped('aqua_sea_glass', ['SS', 'SS'], {'S': S}, 'aqua_sea_glass', 1)
    shapeless('green_sea_glass', [S, S, S, S, 'minecraft:green_dye'], 'green_sea_glass', 1)
    shapeless('blue_sea_glass', [S, S, S, S, 'minecraft:blue_dye'], 'blue_sea_glass', 1)


def coral_stone():
    simple('coral_stone', 'Coral Stone')
    simple('polished_coral_stone', 'Polished Coral Stone')
    simple('coral_stone_bricks', 'Coral Stone Bricks')
    simple('mossy_coral_stone_bricks', 'Mossy Coral Stone Bricks')
    stone_set('coral_stone_bricks', 'Coral Stone Brick', 'coral_stone_brick_stairs', 'coral_stone_brick_slab',
              'coral_stone_brick_wall', cut_from=('coral_stone', 'polished_coral_stone'))
    stone_set('coral_stone', 'Coral Stone', 'coral_stone_stairs', 'coral_stone_slab', 'coral_stone_wall')
    stone_set('polished_coral_stone', 'Polished Coral Stone', 'polished_coral_stone_stairs',
              'polished_coral_stone_slab', cut_from=('coral_stone',))
    stone_set('mossy_coral_stone_bricks', 'Mossy Coral Stone Brick', 'mossy_coral_stone_brick_stairs',
              'mossy_coral_stone_brick_slab', 'mossy_coral_stone_brick_wall')
    corals = ['minecraft:tube_coral_block', 'minecraft:brain_coral_block', 'minecraft:bubble_coral_block',
              'minecraft:fire_coral_block', 'minecraft:horn_coral_block', 'minecraft:dead_tube_coral_block',
              'minecraft:dead_brain_coral_block', 'minecraft:dead_bubble_coral_block',
              'minecraft:dead_fire_coral_block', 'minecraft:dead_horn_coral_block']
    shaped('coral_stone', ['SC', 'CS'], {'S': ['minecraft:stone', 'minecraft:cobblestone'], 'C': corals},
           'coral_stone', 4)
    shaped('polished_coral_stone', ['CC', 'CC'], {'C': 'coral_stone'}, 'polished_coral_stone', 4)
    shaped('coral_stone_bricks', ['CC', 'CC'], {'C': 'polished_coral_stone'}, 'coral_stone_bricks', 4)
    cut('coral_stone', 'polished_coral_stone')
    cut('coral_stone', 'coral_stone_bricks')
    cut('polished_coral_stone', 'coral_stone_bricks')
    shapeless('mossy_coral_stone_bricks', ['coral_stone_bricks', MOSS], 'mossy_coral_stone_bricks')
    tag('block', 'minecraft:mineable/pickaxe', 'coral_stone', 'polished_coral_stone', 'coral_stone_bricks',
        'mossy_coral_stone_bricks', 'coral_stone_brick_stairs', 'coral_stone_brick_slab', 'coral_stone_brick_wall',
        'coral_stone_stairs', 'coral_stone_slab', 'coral_stone_wall', 'polished_coral_stone_stairs',
        'polished_coral_stone_slab', 'mossy_coral_stone_brick_stairs', 'mossy_coral_stone_brick_slab',
        'mossy_coral_stone_brick_wall')


MOSS = ['minecraft:kelp', 'minecraft:vine', 'minecraft:moss_block']


def tidestone():
    simple('tidestone', 'Tidestone')
    simple('tidestone_bricks', 'Tidestone Bricks')
    simple('cracked_tidestone_bricks', 'Cracked Tidestone Bricks')
    simple('mossy_tidestone_bricks', 'Mossy Tidestone Bricks')
    simple('chiseled_tidestone', 'Chiseled Tidestone')
    A.column_block(M, 'tidestone_pillar', 'block/tidestone_pillar', 'block/tidestone_pillar_top')
    loot_self('tidestone_pillar')
    name('tidestone_pillar', 'Tidestone Pillar')
    # glowing: base cube + emissive rune overlay
    full = lambda t, light=None: el([0, 0, 0], [16, 16, 16], box_faces(t, [0, 0, 0], [16, 16, 16]), light=light)  # noqa
    model('glowing_tidestone', {'particle': 'block/glowing_tidestone', 'base': 'block/glowing_tidestone',
                                'runes': 'block/glowing_tidestone_runes'},
          [full('base'), full('runes', 15)], parent='minecraft:block/cube')
    A.blockstate(M, 'glowing_tidestone', {'': {'model': rid('block/glowing_tidestone')}})
    A.block_item(M, 'glowing_tidestone')
    loot_self('glowing_tidestone')
    name('glowing_tidestone', 'Glowing Tidestone')
    stone_set('tidestone_bricks', 'Tidestone Brick', 'tidestone_brick_stairs', 'tidestone_brick_slab',
              'tidestone_brick_wall', cut_from=('tidestone',))
    stone_set('tidestone', 'Tidestone', 'tidestone_stairs', 'tidestone_slab', 'tidestone_wall')
    stone_set('mossy_tidestone_bricks', 'Mossy Tidestone Brick', 'mossy_tidestone_brick_stairs',
              'mossy_tidestone_brick_slab', 'mossy_tidestone_brick_wall')
    shaped('tidestone', ['SP', 'PS'], {'S': ['minecraft:stone', 'minecraft:cobblestone'],
                                       'P': 'minecraft:prismarine_shard'}, 'tidestone', 2)
    shaped('tidestone_bricks', ['TT', 'TT'], {'T': 'tidestone'}, 'tidestone_bricks', 4)
    cut('tidestone', 'tidestone_bricks')
    smelt('cracked_tidestone_bricks', 'tidestone_bricks', 'cracked_tidestone_bricks')
    shapeless('mossy_tidestone_bricks', ['tidestone_bricks', MOSS], 'mossy_tidestone_bricks')
    shapeless('chiseled_tidestone', ['tidestone_bricks', 'minecraft:gold_nugget'], 'chiseled_tidestone')
    shaped('tidestone_pillar', ['T', 'T'], {'T': 'tidestone_bricks'}, 'tidestone_pillar', 2)
    cut('tidestone', 'tidestone_pillar')
    cut('tidestone_bricks', 'tidestone_pillar')
    shapeless('glowing_tidestone', ['tidestone_bricks', ['minecraft:glowstone_dust', 'minecraft:glow_ink_sac']],
              'glowing_tidestone')
    tag('block', 'minecraft:mineable/pickaxe', 'tidestone', 'tidestone_bricks', 'cracked_tidestone_bricks',
        'mossy_tidestone_bricks', 'chiseled_tidestone', 'tidestone_pillar', 'glowing_tidestone',
        'tidestone_brick_stairs', 'tidestone_brick_slab', 'tidestone_brick_wall', 'tidestone_stairs',
        'tidestone_slab', 'tidestone_wall', 'mossy_tidestone_brick_stairs', 'mossy_tidestone_brick_slab',
        'mossy_tidestone_brick_wall')


def drowned_city():
    simple('cracked_concrete', 'Cracked Concrete')
    simple('mossy_concrete', 'Mossy Concrete')
    # rebar concrete: a cube with two bent rusty rods sticking out of the top
    rod = lambda f, t: el(f, t, {d: face('rod', None) for d in ('north', 'south', 'west', 'east', 'up')})  # noqa
    model('rebar_concrete', {'particle': 'block/rebar_concrete', 'all': 'block/rebar_concrete',
                             'rod': 'block/rebar_rod'},
          [el([0, 0, 0], [16, 16, 16], box_faces('all', [0, 0, 0], [16, 16, 16])),
           rod([11, 16, 3], [12, 20, 4]), rod([11, 19, 3], [14, 20, 4]),
           rod([12.5, 16, 10], [13.5, 18.5, 11]), rod([3, 16, 12], [4, 17.5, 13])])
    A.blockstate(M, 'rebar_concrete', {'': {'model': rid('block/rebar_concrete')}})
    A.block_item(M, 'rebar_concrete')
    loot_self('rebar_concrete')
    name('rebar_concrete', 'Rebar Concrete')
    concrete = 'minecraft:gray_concrete'
    shaped('cracked_concrete', ['CG', 'GC'], {'C': '#minecraft:concrete', 'G': 'minecraft:gravel'},
           'cracked_concrete', 4)
    shapeless('mossy_concrete', ['cracked_concrete', MOSS], 'mossy_concrete')
    shapeless('rebar_concrete', ['cracked_concrete', 'minecraft:iron_nugget', 'minecraft:iron_nugget'],
              'rebar_concrete')
    del concrete
    stone_set('cracked_concrete', 'Cracked Concrete', 'cracked_concrete_stairs', 'cracked_concrete_slab',
              'cracked_concrete_wall')
    tag('block', 'minecraft:mineable/pickaxe', 'cracked_concrete', 'mossy_concrete', 'rebar_concrete',
        'cracked_concrete_stairs', 'cracked_concrete_slab', 'cracked_concrete_wall')

    simple('asphalt', 'Asphalt')
    A.block_model(M, 'asphalt_line', 'minecraft:block/cube_bottom_top',
                  {'top': 'block/asphalt_line', 'bottom': 'block/asphalt', 'side': 'block/asphalt'})
    A.blockstate(M, 'asphalt_line', {'axis=z': {'model': rid('block/asphalt_line')},
                                     'axis=x': {'model': rid('block/asphalt_line'), 'y': 90}})
    A.block_item(M, 'asphalt_line')
    loot_self('asphalt_line')
    name('asphalt_line', 'Asphalt Lane Line')
    slab('asphalt_slab', 'Asphalt Slab', 'block/asphalt', 'block/asphalt')
    shaped('asphalt', ['GSG', 'SCS', 'GSG'], {'G': 'minecraft:gravel', 'S': '#minecraft:sand',
                                              'C': ['minecraft:coal', 'minecraft:charcoal']}, 'asphalt', 8)
    shaped('asphalt_line', ['AAA', 'ADA', 'AAA'], {'A': 'asphalt', 'D': 'minecraft:white_dye'}, 'asphalt_line', 8)
    shaped('asphalt_slab', ['AAA'], {'A': 'asphalt'}, 'asphalt_slab', 6)
    cut('asphalt', 'asphalt_slab', 2)
    cut('asphalt', 'asphalt_line')
    stairs('asphalt_stairs', 'Asphalt Stairs', 'block/asphalt')
    shaped('asphalt_stairs', ['A  ', 'AA ', 'AAA'], {'A': 'asphalt'}, 'asphalt_stairs', 4)
    cut('asphalt', 'asphalt_stairs')
    tag('block', 'minecraft:mineable/pickaxe', 'asphalt', 'asphalt_line', 'asphalt_slab', 'asphalt_stairs')

    simple('cracked_glass', 'Cracked Glass', loot=loot_silk)
    A.pane(M, 'cracked_glass_pane', 'block/cracked_glass', 'block/cracked_glass_pane_top')
    loot_silk('cracked_glass_pane')
    name('cracked_glass_pane', 'Cracked Glass Pane')
    tag('block', 'minecraft:impermeable', 'cracked_glass')
    both('c:glass_blocks', 'cracked_glass')
    both('c:glass_panes', 'cracked_glass_pane')
    shapeless('cracked_glass', ['minecraft:glass', 'minecraft:gravel'], 'cracked_glass')
    shaped('cracked_glass_pane', ['GGG', 'GGG'], {'G': 'cracked_glass'}, 'cracked_glass_pane', 16,
           category='misc')


METAL = []  # blocks that need a pickaxe and at least a stone tool


def ship_steel():
    SC, I, N = 'raft:scrap_metal', 'minecraft:iron_ingot', 'minecraft:iron_nugget'
    simple('riveted_hull', 'Riveted Hull')
    simple('rusty_hull', 'Rusty Hull')
    shaped('riveted_hull', [' S ', 'SIS', ' S '], {'S': SC, 'I': I}, 'riveted_hull', 4)
    shaped('rusty_hull', ['S S', ' I ', 'S S'], {'S': SC, 'I': I}, 'rusty_hull', 4)
    simple('corrugated_metal', 'Corrugated Metal')
    shaped('corrugated_metal', ['NNN', 'SSS'], {'N': N, 'S': SC}, 'corrugated_metal', 4)
    stone_set('corrugated_metal', 'Corrugated Metal', 'corrugated_metal_stairs', 'corrugated_metal_slab')
    simple('rusted_plate', 'Rusted Plate')
    shaped('rusted_plate', ['SS', 'SS'], {'S': SC}, 'rusted_plate', 4)
    METAL.extend(['riveted_hull', 'rusty_hull', 'corrugated_metal', 'corrugated_metal_stairs',
                  'corrugated_metal_slab', 'rusted_plate'])

    # steel beam (I-beam), upright model; flanges north/south, web between
    fl = lambda z0, z1, outer: el([1, 0, z0], [15, 16, z1], box_faces(  # noqa
        {'north': 'outer' if outer == 'north' else 'inner', 'south': 'outer' if outer == 'south' else 'inner',
         'west': 'outer', 'east': 'outer', 'up': 'end', 'down': 'end'}, [1, 0, z0], [15, 16, z1]))
    model('steel_beam', {'particle': 'block/steel_beam', 'outer': 'block/steel_beam', 'inner': 'block/steel_beam_inner',
                         'end': 'block/steel_beam_end'},
          [fl(0, 3, 'north'), fl(13, 16, 'south'),
           el([6, 0, 3], [10, 16, 13], box_faces({'west': 'inner', 'east': 'inner', 'up': 'end', 'down': 'end',
                                                   'north': None, 'south': None}, [6, 0, 3], [10, 16, 13]))])
    axis_states('steel_beam')
    A.block_item(M, 'steel_beam')
    loot_self('steel_beam')
    name('steel_beam', 'Steel Beam')
    shaped('steel_beam', ['III', ' I ', 'III'], {'I': I}, 'steel_beam', 8)

    # catwalk: grating slab (bottom/top/double)
    tx = {'bottom': 'block/catwalk', 'top': 'block/catwalk', 'side': 'block/catwalk_side'}
    A.block_model(M, 'catwalk', 'minecraft:block/slab', tx)
    A.block_model(M, 'catwalk_top', 'minecraft:block/slab_top', tx)
    A.block_model(M, 'catwalk_double', 'minecraft:block/cube_bottom_top', tx)
    A.blockstate(M, 'catwalk', {'type=bottom': {'model': rid('block/catwalk')},
                                'type=top': {'model': rid('block/catwalk_top')},
                                'type=double': {'model': rid('block/catwalk_double')}})
    A.block_item(M, 'catwalk')
    loot_slab('catwalk')
    name('catwalk', 'Catwalk')
    both('minecraft:slabs', 'catwalk')
    shaped('catwalk', ['SBS'], {'S': SC, 'B': 'minecraft:iron_bars'}, 'catwalk', 6)

    # steel railing: post + top and middle rail per connected side
    post = el([7, 0, 7], [9, 16, 9], {
        'north': face('r', [7, 0, 9, 16]), 'south': face('r', [7, 0, 9, 16]),
        'west': face('r', [7, 0, 9, 16]), 'east': face('r', [7, 0, 9, 16]),
        'up': face('r', [7, 0, 9, 2], 'up'), 'down': face('r', [7, 14, 9, 16], 'down')})
    model('steel_railing_post', {'particle': 'block/steel_railing', 'r': 'block/steel_railing'}, [post],
          parent='minecraft:block/block', ao=False)
    side = [
        el([7, 13, 0], [9, 16, 7], {
            'west': face('r', [0, 0, 7, 3]), 'east': face('r', [9, 0, 16, 3]),
            'up': face('r', [0, 0, 7, 2], rot=90), 'down': face('r', [0, 1, 7, 3], rot=90),
            'north': face('r', [7, 0, 9, 3], 'north')}),
        el([7, 7, 0], [9, 9, 7], {
            'west': face('r', [0, 7, 7, 9]), 'east': face('r', [9, 7, 16, 9]),
            'up': face('r', [0, 7, 7, 9], rot=90), 'down': face('r', [0, 7, 7, 9], rot=90),
            'north': face('r', [7, 7, 9, 9], 'north')}),
    ]
    model('steel_railing_side', {'particle': 'block/steel_railing', 'r': 'block/steel_railing'}, side,
          parent='minecraft:block/block', ao=False)
    mp = [{'apply': {'model': rid('block/steel_railing_post')}}]
    for d, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
        a = {'model': rid('block/steel_railing_side')}
        if y:
            a['y'] = y
        mp.append({'when': {d: 'true'}, 'apply': a})
    A.blockstate(M, 'steel_railing', multipart=mp)
    A.item_model(M, 'steel_railing', textures={'layer0': 'block/steel_railing'})
    A.item_def(M, 'steel_railing')
    loot_self('steel_railing')
    name('steel_railing', 'Steel Railing')
    shaped('steel_railing', ['NNN', 'NNN'], {'N': N}, 'steel_railing', 6, category='misc')

    simple('porthole', 'Porthole')
    shaped('porthole', [' I ', 'IGI', ' I '], {'I': I, 'G': 'minecraft:glass'}, 'porthole', 2)

    A.door(M, 'bulkhead_door', 'block/bulkhead_door_bottom', 'block/bulkhead_door_top')
    loot_door('bulkhead_door')
    name('bulkhead_door', 'Bulkhead Door')
    both('minecraft:doors', 'bulkhead_door')
    shaped('bulkhead_door', ['II', 'IP', 'II'], {'I': I, 'P': 'porthole'}, 'bulkhead_door', 2, category='redstone')
    A.trapdoor(M, 'metal_hatch', 'block/metal_hatch')
    loot_self('metal_hatch')
    name('metal_hatch', 'Metal Hatch')
    both('minecraft:trapdoors', 'metal_hatch')
    shaped('metal_hatch', ['III', 'ISI'], {'I': I, 'S': SC}, 'metal_hatch', 2, category='redstone')
    METAL.extend(['steel_beam', 'catwalk', 'steel_railing', 'porthole', 'bulkhead_door', 'metal_hatch'])

    # rusty pipe (upright): pipe body between two flanged collars
    pipe_tex = {'particle': 'block/rusty_pipe', 'pipe': 'block/rusty_pipe', 'end': 'block/rusty_pipe_end'}
    collar_uv = {d: [3, 0, 13, 2] for d in ('north', 'south', 'west', 'east')}
    collar_uv.update({'up': [3, 3, 13, 13], 'down': [3, 3, 13, 13]})

    def collar(y0, y1):
        return el([3, y0, 3], [13, y1, 13], box_faces({'north': 'pipe', 'south': 'pipe', 'west': 'pipe',
                                                       'east': 'pipe', 'up': 'end', 'down': 'end'},
                                                      [3, y0, 3], [13, y1, 13], uvs=collar_uv))
    body_uv = {d: [4, 2, 12, 14] for d in ('north', 'south', 'west', 'east')}
    pipe_elements = [el([4, 2, 4], [12, 14, 12], box_faces('pipe', [4, 2, 4], [12, 14, 12], skip=('up', 'down'),
                                                           uvs=body_uv)), collar(0, 2), collar(14, 16)]
    model('rusty_pipe', pipe_tex, pipe_elements)
    axis_states('rusty_pipe')
    A.block_item(M, 'rusty_pipe')
    loot_self('rusty_pipe')
    name('rusty_pipe', 'Rusty Pipe')
    shaped('rusty_pipe', ['S', 'S', 'S'], {'S': SC}, 'rusty_pipe', 6)

    valve_tex = dict(pipe_tex, wheel='block/rusty_pipe_valve_wheel', stem='block/steel_beam_end')
    valve = pipe_elements + [
        el([3.5, 5, 3.5], [12.5, 11, 12.5], box_faces({'north': 'pipe', 'south': 'pipe', 'west': 'pipe',
                                                        'east': 'pipe', 'up': 'end', 'down': 'end'},
                                                       [3.5, 5, 3.5], [12.5, 11, 12.5],
                                                       uvs={d: [3.5, 5, 12.5, 11] for d in
                                                            ('north', 'south', 'west', 'east')})),
        el([7, 7, 12.5], [9, 9, 14.5], box_faces('stem', [7, 7, 12.5], [9, 9, 14.5], skip=('north',))),
    ]
    # the hand wheel: its sprite extruded row by row, so the rim follows the round wheel (no square frame)
    valve += extrude.vertical('raft:block/rusty_pipe_valve_wheel', '#wheel', region=(4, 4, 12, 12), at=(4, 12),
                              z=(14.5, 15.5), front='south')
    model('rusty_pipe_valve', valve_tex, valve)
    axis_states('rusty_pipe_valve')
    A.block_item(M, 'rusty_pipe_valve')
    loot_self('rusty_pipe_valve')
    name('rusty_pipe_valve', 'Rusty Pipe Valve')
    shapeless('rusty_pipe_valve', ['rusty_pipe', I, 'minecraft:red_dye'], 'rusty_pipe_valve')
    METAL.extend(['rusty_pipe', 'rusty_pipe_valve'])

    for c, lab in (('red', 'Red'), ('blue', 'Blue'), ('green', 'Green')):
        b = f'shipping_container_{c}'
        t = lambda s: f'block/{b}_{s}'  # noqa: E731
        A.block_model(M, b, 'minecraft:block/cube', {
            'particle': t('side'), 'north': t('end'), 'south': t('end'), 'east': t('side'), 'west': t('side'),
            'up': t('top'), 'down': t('top')})
        A.blockstate(M, b, {'axis=z': {'model': rid(f'block/{b}')}, 'axis=x': {'model': rid(f'block/{b}'), 'y': 90}})
        A.block_item(M, b)
        loot_self(b)
        name(b, f'{lab} Shipping Container')
        shaped(b, ['SIS', 'IDI', 'SIS'], {'S': SC, 'I': I, 'D': f'minecraft:{c}_dye'}, b, 8)
        METAL.append(b)


# ------------------------------------------------------------------------------------------------- Expedition tech
def tech():
    SC, I, C, R = 'raft:scrap_metal', 'minecraft:iron_ingot', 'raft:circuit_board', 'minecraft:redstone'
    # control panel: cabinet, sloped desk (+ emissive lamps), screen housing (+ emissive screens)
    slope_rot = {'origin': [8, 8, 2], 'axis': 'x', 'angle': -22.5}
    # the desk is 9.7 long so its far end stays in front of the screen housing (z 11), no coplanar side faces
    slope_from, slope_to = [0, 8, 2], [16, 9.5, 11.7]
    cp = [
        el([0, 0, 2], [16, 8, 16], {
            'north': face('front', [0, 8, 16, 16]), 'south': face('side', [0, 8, 16, 16], 'south'),
            'west': face('side', [2, 8, 16, 16], 'west'), 'east': face('side', [0, 8, 14, 16], 'east'),
            'down': face('side', [0, 2, 16, 16], 'down')}),
        el(slope_from, slope_to, {
            'up': face('top', [0, 2, 16, 11.7]), 'north': face('side', [0, 0, 16, 1.5]),
            'west': face('side', [2, 0, 11.7, 1.5]), 'east': face('side', [4.3, 0, 14, 1.5])}, rotation=slope_rot),
        el(slope_from, slope_to, {'up': face('lights', [0, 2, 16, 11.7])}, rotation=slope_rot, light=15),
    ]
    # fill the wedge under the sloped desk with 1 px steps (side texture continues from the cabinet), so the
    # sides of the panel are closed; each step ends just below the desk
    for (y0, z0) in ((8, 4.5), (9, 7), (10, 9.5)):
        cp.append(el([0, y0, z0], [16, y0 + 1, 11], box_faces('side', [0, y0, z0], [16, y0 + 1, 11],
                                                              skip=('down', 'south'))))
    cp += [
        el([0, 8, 11], [16, 15, 16], {
            'north': face('screen', [0, 1, 16, 8]), 'up': face('side', [0, 0, 16, 5]),
            'south': face('side', [0, 1, 16, 8], 'south'), 'west': face('side', [11, 1, 16, 8], 'west'),
            'east': face('side', [0, 1, 5, 8], 'east')}),
        el([0, 8, 11], [16, 15, 16], {'north': face('glow', [0, 1, 16, 8])}, light=15),
    ]
    model('control_panel', {'particle': 'block/control_panel_side', 'front': 'block/control_panel_front',
                            'side': 'block/control_panel_side', 'top': 'block/control_panel_top',
                            'lights': 'block/control_panel_lights', 'screen': 'block/control_panel_screen',
                            'glow': 'block/control_panel_screen_glow'}, cp)
    facing_states('control_panel')
    A.block_item(M, 'control_panel')
    loot_self('control_panel')
    name('control_panel', 'Control Panel')
    shaped('control_panel', [' G ', 'RCR', 'SSS'], {'G': 'minecraft:glass_pane', 'R': R, 'C': C, 'S': SC},
           'control_panel', 1, category='misc')

    # server rack: cabinet with an emissive LED overlay on the front
    rack = [
        el([1, 0, 1], [15, 16, 15], box_faces({'north': 'front', 'south': 'side', 'west': 'side', 'east': 'side',
                                                'up': 'top', 'down': 'top'}, [1, 0, 1], [15, 16, 15],
                                               uvs={'north': [1, 0, 15, 16], 'south': [1, 0, 15, 16],
                                                    'west': [1, 0, 15, 16], 'east': [1, 0, 15, 16],
                                                    'up': [1, 1, 15, 15], 'down': [1, 1, 15, 15]})),
        el([1, 0, 1], [15, 16, 15], {'north': face('leds', [1, 0, 15, 16])}, light=15),
    ]
    model('server_rack', {'particle': 'block/server_rack_side', 'front': 'block/server_rack_front',
                          'side': 'block/server_rack_side', 'top': 'block/server_rack_top',
                          'leds': 'block/server_rack_leds'}, rack)
    facing_states('server_rack')
    A.block_item(M, 'server_rack')
    loot_self('server_rack')
    name('server_rack', 'Server Rack')
    shaped('server_rack', ['SCS', 'SRS', 'SCS'], {'S': SC, 'C': C, 'R': R}, 'server_rack', 1,
           category='misc')

    # satellite dish: base, post, yoke, a dish tilted 45 deg up-north with a feed arm and horn
    dish_rot = {'origin': [8, 10, 8], 'axis': 'x', 'angle': 45}
    m = 'mount'
    dish = [
        el([3, 0, 3], [13, 2, 13], box_faces(m, [3, 0, 3], [13, 2, 13])),
        el([6.5, 2, 6.5], [9.5, 6, 9.5], box_faces(m, [6.5, 2, 6.5], [9.5, 6, 9.5], skip=('up', 'down'))),
        el([5, 6, 7], [11, 9, 9.5], box_faces(m, [5, 6, 7], [11, 9, 9.5])),
    ]
    # the round dish: one band per run of equal pixel rows (TT.DISH_BANDS); texture row r is at y = 17 - r
    for (r0, r1, c0, c1) in TT.DISH_BANDS:
        e = TT.DISH_EDGE_UV
        dish.append(el([c0, 17 - r1, 8], [c1 + 1, 18 - r0, 9], {
            'north': face('dish', [c0, r0, c1 + 1, r1 + 1]), 'south': face('back', [c0, r0, c1 + 1, r1 + 1]),
            'west': face('dish', e['side']), 'east': face('dish', e['side']),
            'up': face('dish', e['up']), 'down': face('dish', e['down'])}, rotation=dish_rot))
    dish += [
        el([7.5, 9.5, 2.5], [8.5, 10.5, 8], box_faces(m, [7.5, 9.5, 2.5], [8.5, 10.5, 8], cull_outer=False),
           rotation=dish_rot),
        el([7, 9, 1.5], [9, 11, 2.5], box_faces('horn', [7, 9, 1.5], [9, 11, 2.5], cull_outer=False),
           rotation=dish_rot),
    ]
    model('satellite_dish', {'particle': 'block/satellite_dish_mount', 'mount': 'block/satellite_dish_mount',
                             'dish': 'block/satellite_dish', 'back': 'block/satellite_dish_back',
                             'horn': 'block/control_panel_side'}, dish)
    facing_states('satellite_dish')
    A.block_item(M, 'satellite_dish')
    loot_self('satellite_dish')
    name('satellite_dish', 'Satellite Dish')
    shaped('satellite_dish', ['S S', ' C ', 'SIS'], {'S': SC, 'C': C, 'I': I}, 'satellite_dish', 1,
           category='misc')

    # wind turbine: pole + nacelle + tail vane (the rotor is drawn by the block entity renderer)
    W = {'particle': 'block/wind_turbine_body', 'body': 'block/wind_turbine_body',
         'front': 'block/wind_turbine_front', 'pole': 'block/wind_turbine_pole', 'blade': 'block/wind_turbine_blade'}
    turbine = [
        el([5, 0, 5], [11, 1, 11], box_faces('pole', [5, 0, 5], [11, 1, 11])),
        el([6.5, 1, 6.5], [9.5, 5, 9.5], box_faces('pole', [6.5, 1, 6.5], [9.5, 5, 9.5], skip=('up', 'down'))),
        el([5, 5, 2], [11, 11, 14], box_faces({'north': 'front', 'south': 'body', 'west': 'body', 'east': 'body',
                                                'up': 'body', 'down': 'body'}, [5, 5, 2], [11, 11, 14],
                                               uvs={'north': [5, 5, 11, 11], 'south': [5, 5, 11, 11],
                                                    'west': [2, 5, 14, 11], 'east': [2, 5, 14, 11],
                                                    'up': [5, 2, 11, 14], 'down': [5, 2, 11, 14]})),
        el([7.5, 6, 14], [8.5, 13, 16], box_faces('body', [7.5, 6, 14], [8.5, 13, 16])),
    ]
    model('wind_turbine', W, turbine)
    facing_states('wind_turbine')
    # item: the turbine with a static rotor (hub + three blades) in front
    # the hub sticks out in front of the block (z < 0): its side UVs are shifted into 0..16
    rotor = [el([5.5, 5.5, -2], [10.5, 10.5, 2], box_faces('body', [5.5, 5.5, -2], [10.5, 10.5, 2],
                                                            cull_outer=False,
                                                            uvs={'west': [0, 5.5, 4, 10.5], 'east': [12, 5.5, 16, 10.5],
                                                                 'up': [5.5, 0, 10.5, 4], 'down': [5.5, 12, 10.5, 16]}))]
    for k, ang in enumerate((0, 120, 240)):
        r = {'origin': [8, 8, -1], 'axis': 'z', 'angle': float(ang - 360 if ang > 180 else ang)}
        rotor.append(el([6.5, 10, -1.5], [9.5, 31, -0.5], {
            'north': face('blade', [6.5, 0, 9.5, 16]), 'south': face('blade', [9.5, 0, 6.5, 16]),
            'west': face('blade', [0, 0, 1, 16]), 'east': face('blade', [0, 0, 1, 16]),
            'up': face('blade', [0, 12, 3, 13])}, rotation=r))
    gui = {'gui': {'rotation': [30, 225, 0], 'translation': [0, 0, 0], 'scale': [0.42, 0.42, 0.42]},
           'fixed': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [0.35, 0.35, 0.35]},
           'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.2, 0.2, 0.2]},
           'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0],
                                     'scale': [0.3, 0.3, 0.3]},
           'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 0, 0], 'scale': [0.3, 0.3, 0.3]},
           'firstperson_lefthand': {'rotation': [0, 225, 0], 'translation': [0, 0, 0], 'scale': [0.3, 0.3, 0.3]}}
    model('wind_turbine_inventory', W, turbine + rotor, display=gui)
    A.item_def(M, 'wind_turbine', 'block/wind_turbine_inventory')
    loot_machine('wind_turbine')
    name('wind_turbine', 'Wind Turbine')
    shaped('wind_turbine', [' S ', 'SCS', ' I '], {'S': SC, 'C': C, 'I': I}, 'wind_turbine', 1, category='redstone')
    METAL.extend(['control_panel', 'server_rack', 'satellite_dish', 'wind_turbine'])


# ================================================================================================= lang, tags, main
def lang():
    LANG.update({
        'itemgroup.raft.building': 'Beach Craft: Building',
        'tooltip.raft.wind_turbine.1': 'Makes 20 FE per tick in the wind.',
        'tooltip.raft.wind_turbine.2': 'Needs free air in a 3x3 area in front and a spot above y 90.',
        'tooltip.raft.wind_turbine.3': 'Gives its energy to cables, batteries and machines next to it.',
        'message.raft.wind_turbine.energy': 'Wind Turbine: %s / %s FE - %s',
        'message.raft.wind_turbine.running': 'spinning, +20 FE per tick',
        'message.raft.wind_turbine.too_low': 'too little wind down here, build it above y 90',
        'message.raft.wind_turbine.blocked': 'blocked, the rotor needs free air in a 3x3 area in front',
        'tooltip.raft.rusty_pipe_valve.1': 'Turn the wheel to let off some steam.',
    })
    A.lang(M, LANG)


def main():
    root = module_dir(M)
    for sub in ('assets', 'data'):
        shutil.rmtree(os.path.join(root, sub), ignore_errors=True)
    textures()
    wood()
    sea_glass()
    coral_stone()
    tidestone()
    drowned_city()
    ship_steel()
    tech()
    tag('block', 'minecraft:mineable/pickaxe', *METAL)
    tag('block', 'minecraft:needs_stone_tool', *METAL)
    write_unlocks()
    lang()
    A.flush(M)
    guard_foreign_items()
    n = sum(len(f) for _, _, f in os.walk(root))
    print(f'building: wrote {n} files into {root}')


if __name__ == '__main__':
    main()
