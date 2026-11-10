"""Generates every resource of the diving module into src/main/modules/diving/ (spec 8.2).

Run: cd tools && python3 -m content.diving.gen   (or python3 tools/content/diving/gen.py)
Contact sheets for review go to $BC_PREVIEW (default: the module's work folder in the scratchpad).
"""
import os
import shutil
import sys

if __package__ in (None, ''):
    sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..')))
    __package__ = 'content.diving'

from bclib import assets, paths, tex  # noqa: E402

from content.diving import block_gui, gear, items  # noqa: E402

M = 'diving'
NS = 'raft'
PREVIEW = os.environ.get('BC_PREVIEW', '/tmp/claude-0/-home-user/4ca9508b-4f8c-5a8e-91dc-6059d6cc887c/scratchpad/work/diving')


def opt(i):
    return {'id': assets.rid(i), 'required': False}


def needs(*item_ids):
    """Fabric load condition: only load if these items exist (ids of other modules)."""
    return [{'condition': 'fabric:registry_contains', 'registry': 'minecraft:item', 'values': [assets.rid(i) for i in item_ids]}]


# ----------------------------------------------------------------------------------------------- assets
def textures():
    sprites = items.generate()
    for name, img in sprites.items():
        tex.save(img, M, 'item/' + name)
    worn = gear.generate()
    for rel, img in worn.items():
        tex.save(img, M, rel)
    blocks, gauge_on = block_gui.block_textures()
    for rel, img in blocks.items():
        tex.save(img, M, rel)
    tex.save_animated(gauge_on, M, 'block/air_compressor_gauge_on', frametime=3, interpolate=False)
    tex.save(block_gui.gui_texture(), M, 'gui/container/air_compressor')
    for rel, img in block_gui.hud_sprites().items():
        tex.save(img, M, rel)
    return sprites, worn, blocks


def item_models():
    for name in ('oxygen_tank', 'titanium_oxygen_tank', 'wetsuit', 'flippers', 'harpoon'):
        assets.flat_item(M, name)
    # diving helmet: shows the lamp when one is fitted (component raft:diving_lamp)
    assets.item_model(M, 'diving_helmet', textures={'layer0': 'item/diving_helmet'})
    assets.item_model(M, 'diving_helmet_lamp', textures={'layer0': 'item/diving_helmet_lamp'})
    assets.write_json(M, f'assets/{NS}/items/diving_helmet.json', {'model': {
        'type': 'minecraft:condition', 'property': 'minecraft:has_component', 'component': 'raft:diving_lamp',
        'on_true': {'type': 'minecraft:model', 'model': 'raft:item/diving_helmet_lamp'},
        'on_false': {'type': 'minecraft:model', 'model': 'raft:item/diving_helmet'}}})
    assets.handheld_item(M, 'sea_scooter')
    # harpoon gun: the harpoon tip shows while the gun is loaded, not during the 1 s reload (cooldown)
    # held forward like a crossbow (same transforms as the vanilla crossbow model)
    gun_display = {
        'thirdperson_righthand': {'rotation': [-90, 0, -60], 'translation': [2, 0.1, -3], 'scale': [0.9, 0.9, 0.9]},
        'thirdperson_lefthand': {'rotation': [-90, 0, 30], 'translation': [2, 0.1, -3], 'scale': [0.9, 0.9, 0.9]},
        'firstperson_righthand': {'rotation': [-90, 0, -55], 'translation': [1.13, 3.2, 1.13], 'scale': [0.68, 0.68, 0.68]},
        'firstperson_lefthand': {'rotation': [-90, 0, 35], 'translation': [1.13, 3.2, 1.13], 'scale': [0.68, 0.68, 0.68]},
    }
    assets.item_model(M, 'harpoon_gun', textures={'layer0': 'item/harpoon_gun'}, display=gun_display)
    assets.item_model(M, 'harpoon_gun_empty', textures={'layer0': 'item/harpoon_gun_empty'}, display=gun_display)
    assets.write_json(M, f'assets/{NS}/items/harpoon_gun.json', {'model': {
        'type': 'minecraft:range_dispatch', 'property': 'minecraft:cooldown',
        'entries': [{'threshold': 0.01, 'model': {'type': 'minecraft:model', 'model': 'raft:item/harpoon_gun_empty'}}],
        'fallback': {'type': 'minecraft:model', 'model': 'raft:item/harpoon_gun'}}})


def equipment():
    for name, layer in (('diving_helmet', 'humanoid'), ('oxygen_tank', 'humanoid'), ('titanium_oxygen_tank', 'humanoid'),
                        ('wetsuit', 'humanoid_leggings'), ('flippers', 'humanoid')):
        assets.write_json(M, f'assets/{NS}/equipment/{name}.json', {'layers': {layer: [{'texture': f'raft:{name}'}]}})


def air_compressor_block():
    els = block_gui.model_elements()
    texs = {'particle': 'raft:block/air_compressor_tank', 'base': 'raft:block/air_compressor_base',
            'base_side': 'raft:block/air_compressor_base_side', 'tank': 'raft:block/air_compressor_tank',
            'end': 'raft:block/air_compressor_tank_end', 'top': 'raft:block/air_compressor_tank_top',
            'pump': 'raft:block/air_compressor_pump', 'pump_top': 'raft:block/air_compressor_pump_top',
            'brass': 'raft:block/air_compressor_brass', 'wheel': 'raft:block/air_compressor_wheel',
            'gauge': 'raft:block/air_compressor_gauge'}
    assets.write_json(M, f'assets/{NS}/models/block/air_compressor.json',
                      {'parent': 'minecraft:block/block', 'textures': texs, 'elements': els})
    on = dict(texs, gauge='raft:block/air_compressor_gauge_on')
    assets.write_json(M, f'assets/{NS}/models/block/air_compressor_on.json',
                      {'parent': 'minecraft:block/block', 'textures': on, 'elements': els})
    variants = {}
    for facing, rot in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
        for running in ('false', 'true'):
            v = {'model': 'raft:block/air_compressor' + ('_on' if running == 'true' else '')}
            if rot:
                v['y'] = rot
            variants[f'facing={facing},running={running}'] = v
    assets.blockstate(M, 'air_compressor', variants)
    assets.block_item(M, 'air_compressor')
    # drops itself with the energy it held (3.4.2: the raft:stored_energy component, StoredEnergy.java)
    assets.loot_block(M, 'air_compressor', [{'type': 'minecraft:item', 'name': 'raft:air_compressor', 'functions': [
        {'function': 'minecraft:copy_components', 'source': 'block_entity', 'include': ['raft:stored_energy']}]}],
                      conditions=[{'condition': 'minecraft:survives_explosion'}])
    assets.tag(M, 'block', 'minecraft:mineable/pickaxe', ['air_compressor'])


# ----------------------------------------------------------------------------------------------- data
def recipes():
    S, L = assets.recipe_shaped, assets.recipe_shapeless
    S(M, 'diving_helmet', ['GCG', 'CPC', 'CCC'],
      {'G': 'minecraft:gold_ingot', 'C': 'minecraft:copper_ingot', 'P': 'minecraft:glass_pane'}, 'diving_helmet', category='equipment')
    S(M, 'oxygen_tank', ['ICI', 'IBI', 'ILI'],
      {'I': 'minecraft:iron_ingot', 'C': 'minecraft:copper_ingot', 'B': 'minecraft:bucket', 'L': 'minecraft:leather'},
      'oxygen_tank', category='equipment')
    S(M, 'titanium_oxygen_tank', ['TCT', 'TOT', 'T T'],
      {'T': 'raft:titanium_ingot', 'C': 'minecraft:copper_ingot', 'O': 'raft:oxygen_tank'}, 'titanium_oxygen_tank',
      category='equipment')
    S(M, 'wetsuit', ['LKL', 'K K', 'L L'], {'L': 'minecraft:leather', 'K': 'minecraft:dried_kelp'}, 'wetsuit',
      category='equipment')
    S(M, 'flippers', ['L L', 'P P'], {'L': 'minecraft:leather', 'P': 'raft:plastic'}, 'flippers', category='equipment')
    S(M, 'air_compressor', ['IPI', 'CBC', 'IRI'],
      {'I': 'minecraft:iron_ingot', 'P': 'minecraft:piston', 'C': 'minecraft:copper_ingot', 'B': 'minecraft:bucket',
       'R': 'raft:circuit_board'}, 'air_compressor')
    S(M, 'harpoon_gun', ['III', 'PCW', '  W'],
      {'I': 'minecraft:iron_ingot', 'P': 'minecraft:piston', 'C': 'minecraft:copper_ingot', 'W': '#minecraft:planks'},
      'harpoon_gun', category='equipment')
    S(M, 'harpoon', ['I', 'S', 'F'], {'I': 'minecraft:iron_ingot', 'S': 'minecraft:stick', 'F': 'minecraft:string'},
      'harpoon', count=4, category='equipment')
    p = L(M, 'harpoon_from_urchin_spine', ['raft:urchin_spine', 'minecraft:stick', 'minecraft:string'], 'harpoon', count=2,
          category='equipment')
    _add_condition(p, needs('urchin_spine'))
    # helmet lamp: helmet + angler lure (marine) -> the same helmet (damage, enchantments kept) with raft:diving_lamp
    assets.write_json(M, f'data/{NS}/recipe/diving_helmet_lamp.json', {
        'fabric:load_conditions': needs('angler_lure'),
        'type': 'minecraft:crafting_transmute', 'category': 'equipment',
        'input': 'raft:diving_helmet', 'material': 'raft:angler_lure',
        'result': {'id': 'raft:diving_helmet', 'components': {'raft:diving_lamp': {}}}})
    S(M, 'sea_scooter', ['PIP', 'RCG', 'PIP'],
      {'P': 'raft:plastic', 'I': 'minecraft:iron_ingot', 'R': 'minecraft:redstone', 'C': 'raft:circuit_board',
       'G': 'minecraft:glass_pane'}, 'sea_scooter', category='equipment')


def _add_condition(path, conditions):
    import json
    with open(path, encoding='utf-8') as f:
        d = json.load(f)
    d = {'fabric:load_conditions': conditions, **d}
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(d, f, indent=2)
        f.write('\n')


def tags():
    T = assets.tag
    T(M, 'item', 'repairs_diving_helmet', ['minecraft:copper_ingot'])
    T(M, 'item', 'repairs_oxygen_tank', ['minecraft:iron_ingot'])
    T(M, 'item', 'repairs_titanium_oxygen_tank', ['raft:titanium_ingot'])
    T(M, 'item', 'repairs_wetsuit', ['minecraft:leather'])
    T(M, 'item', 'repairs_flippers', ['raft:plastic'])
    T(M, 'item', 'minecraft:head_armor', ['diving_helmet'])
    T(M, 'item', 'minecraft:chest_armor', ['oxygen_tank', 'titanium_oxygen_tank'])
    T(M, 'item', 'minecraft:leg_armor', ['wetsuit'])
    T(M, 'item', 'minecraft:foot_armor', ['flippers'])
    T(M, 'item', 'minecraft:enchantable/durability', ['harpoon_gun'])
    T(M, 'item', 'minecraft:enchantable/vanishing', ['harpoon_gun', 'sea_scooter'])
    T(M, 'entity_type', 'minecraft:arrows', ['harpoon'])
    T(M, 'entity_type', 'minecraft:impact_projectiles', ['harpoon'])


def sounds():
    def ev(sounds, subtitle, **kw):
        return {'sounds': [dict({'name': s}, **kw) if isinstance(s, str) else s for s in sounds],
                'subtitle': 'subtitles.raft.' + subtitle}

    # the harpoon gun, compressor, scooter and tank sounds are synthesised by the audio module (tools/content/audio)
    from content.audio import events
    assets.sounds(M, events.apply(M, {
        'item.armor.equip_diving_gear': ev(['minecraft:mob/turtle/armor', 'minecraft:item/armor/equip_iron1'],
                                           'item.armor.equip_diving_gear'),
        'item.harpoon_gun.fire': ev(['minecraft:item/crossbow/shoot1', 'minecraft:item/crossbow/shoot2',
                                     'minecraft:item/crossbow/shoot3'], 'item.harpoon_gun.fire', pitch=0.72),
        'item.harpoon_gun.reload': ev(['minecraft:item/crossbow/loading_end'], 'item.harpoon_gun.reload', pitch=1.25,
                                      volume=0.8),
        'entity.harpoon.hit': ev(['minecraft:item/trident/pierce1', 'minecraft:item/trident/pierce2',
                                  'minecraft:item/trident/pierce3'], 'entity.harpoon.hit', pitch=1.1),
        'entity.harpoon.hit_ground': ev(['minecraft:item/trident/ground_impact1', 'minecraft:item/trident/ground_impact2',
                                         'minecraft:item/trident/ground_impact3', 'minecraft:item/trident/ground_impact4'],
                                        'entity.harpoon.hit_ground', pitch=1.15),
        'block.air_compressor.crank': ev(['minecraft:block/grindstone/grindstone1', 'minecraft:block/grindstone/grindstone2',
                                          'minecraft:block/grindstone/grindstone3'], 'block.air_compressor.crank',
                                         pitch=1.5, volume=0.7),
        'block.air_compressor.fill': ev(['minecraft:random/fizz'], 'block.air_compressor.fill', pitch=1.7, volume=0.5),
        'item.sea_scooter.run': ev(['minecraft:block/beehive/work1', 'minecraft:block/beehive/work2',
                                    'minecraft:block/beehive/work3', 'minecraft:block/beehive/work4'],
                                   'item.sea_scooter.run', pitch=0.55),
        'item.oxygen_tank.refill': ev(['minecraft:block/bubble_column/upwards_ambient1',
                                       'minecraft:block/bubble_column/upwards_ambient2'], 'item.oxygen_tank.refill',
                                      pitch=1.4),
        'item.oxygen_tank.low': ev(['minecraft:note/bit'], 'item.oxygen_tank.low', volume=0.7),
    }))


def lang():
    assets.lang(M, {
        'item.raft.diving_helmet': 'Diving Helmet',
        'item.raft.oxygen_tank': 'Oxygen Tank',
        'item.raft.titanium_oxygen_tank': 'Titanium Oxygen Tank',
        'item.raft.wetsuit': 'Wetsuit',
        'item.raft.flippers': 'Flippers',
        'item.raft.harpoon_gun': 'Harpoon Gun',
        'item.raft.harpoon': 'Harpoon',
        'item.raft.sea_scooter': 'Sea Scooter',
        'block.raft.air_compressor': 'Air Compressor',
        'entity.raft.harpoon': 'Harpoon',
        'tooltip.raft.diving_helmet': 'See farther under water. With an Oxygen Tank you breathe from the tank.',
        'tooltip.raft.diving_helmet.lamp': 'Lamp fitted: you see in the dark while under water.',
        'tooltip.raft.diving_helmet.no_lamp': 'Craft it with an Angler Lure to fit a lamp.',
        'tooltip.raft.oxygen_tank': 'Refills at the surface, in bubble columns or in an Air Compressor.',
        'tooltip.raft.oxygen_tank.air': 'Air: %s / %s',
        'tooltip.raft.wetsuit': 'Mine three times faster under water. Jellyfish cannot sting you.',
        'tooltip.raft.flippers': 'Swim much faster, but walk slower on land.',
        'tooltip.raft.diving_gear.set': "Full diving set: Dolphin's Grace while swimming",
        'tooltip.raft.harpoon_gun': 'Shoots harpoons: 8 damage under water, 4 in the air.',
        'tooltip.raft.harpoon_gun.1': 'Needs Harpoons in your inventory. Pick them up again after the hunt.',
        'tooltip.raft.sea_scooter': 'Hold use under water and it pulls you where you look.',
        'tooltip.raft.sea_scooter.energy': 'Battery: %s / %s FE',
        'tooltip.raft.air_compressor': 'Fills Oxygen Tanks and charges the Sea Scooter.',
        'tooltip.raft.air_compressor.1': 'Runs on FE from a neighbour, or sneak-use with an empty hand to crank it.',
        'container.raft.air_compressor': 'Air Compressor',
        'container.raft.air_compressor.crank': 'Turn the crank (+5 seconds of air)',
        'container.raft.air_compressor.air': '%s / %s',
        'container.raft.air_compressor.fe': '%s / %s FE',
        'container.raft.air_compressor.empty': 'Put in a tank',
        'container.raft.air_compressor.energy': 'Energy: %s / %s FE',
        'subtitles.raft.item.armor.equip_diving_gear': 'Diving gear fastens',
        'subtitles.raft.item.harpoon_gun.fire': 'Harpoon gun fires',
        'subtitles.raft.item.harpoon_gun.reload': 'Harpoon gun reloads',
        'subtitles.raft.entity.harpoon.hit': 'Harpoon hits',
        'subtitles.raft.entity.harpoon.hit_ground': 'Harpoon sticks',
        'subtitles.raft.block.air_compressor.crank': 'Compressor crank turns',
        'subtitles.raft.block.air_compressor.fill': 'Compressor hisses',
        'subtitles.raft.item.sea_scooter.run': 'Sea Scooter hums',
        'subtitles.raft.item.oxygen_tank.refill': 'Oxygen tank refilled',
        'subtitles.raft.item.oxygen_tank.low': 'Oxygen low',
    })


def preview(sprites, worn, blocks):
    os.makedirs(PREVIEW, exist_ok=True)
    tex.sheet(list(sprites.values()) + ['minecraft:item/turtle_helmet', 'minecraft:item/iron_chestplate',
                                        'minecraft:item/crossbow_standby', 'minecraft:item/trident'],
              os.path.join(PREVIEW, 'sheet_items.png'), scale=8, cols=7)
    tex.sheet(list(worn.values()), os.path.join(PREVIEW, 'sheet_worn.png'), scale=6, cols=4)
    tex.sheet(list(blocks.values()) + list(block_gui.hud_sprites().values()), os.path.join(PREVIEW, 'sheet_blocks.png'),
              scale=8, cols=8)
    g = block_gui.gui_texture()
    g.resize((512, 512), 0).save(os.path.join(PREVIEW, 'gui.png'))


def main():
    out = paths.module_dir(M)
    # start clean so removed files do not linger (lang/tags are rewritten by flush)
    for sub in ('assets', 'data'):
        shutil.rmtree(os.path.join(out, sub), ignore_errors=True)
    sprites, worn, blocks = textures()
    item_models()
    equipment()
    air_compressor_block()
    recipes()
    tags()
    sounds()
    lang()
    assets.flush(M)
    preview(sprites, worn, blocks)
    print('diving: resources written to', out)


if __name__ == '__main__':
    main()
