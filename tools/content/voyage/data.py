"""Item definitions, recipes, tags and lang of the voyage module."""
from bclib import assets

M = 'voyage'

BOATS = ['palm_boat', 'palm_chest_boat', 'driftwood_raft', 'driftwood_chest_raft']
UPGRADES = ['raft_sail', 'raft_engine', 'raft_anchor', 'raft_crate', 'raft_lantern']


def generate():
    for name in ['expedition_raft'] + UPGRADES + BOATS:
        assets.flat_item(M, name)

    # boats like vanilla
    assets.write_json(M, 'data/raft/recipe/palm_boat.json', {
        'type': 'minecraft:crafting_shaped', 'category': 'misc', 'group': 'boat',
        'key': {'#': 'raft:palm_planks'}, 'pattern': ['# #', '###'], 'result': {'count': 1, 'id': 'raft:palm_boat'}})
    assets.write_json(M, 'data/raft/recipe/driftwood_raft.json', {
        'type': 'minecraft:crafting_shaped', 'category': 'misc', 'group': 'boat',
        'key': {'#': 'raft:driftwood_planks'}, 'pattern': ['# #', '###'], 'result': {'count': 1, 'id': 'raft:driftwood_raft'}})
    for boat, chest in (('palm_boat', 'palm_chest_boat'), ('driftwood_raft', 'driftwood_chest_raft')):
        assets.write_json(M, 'data/raft/recipe/%s.json' % chest, {
            'type': 'minecraft:crafting_shapeless', 'category': 'misc', 'group': 'chest_boat',
            'ingredients': ['minecraft:chest', 'raft:' + boat], 'result': {'count': 1, 'id': 'raft:' + chest}})

    # the Expedition Raft and its upgrades
    assets.recipe_shaped(M, 'expedition_raft', ['RPR', 'FFF', 'FFF'],
                         {'R': 'raft:rope', 'P': 'raft:paddle', 'F': 'raft:raft_foundation'}, 'expedition_raft')
    assets.recipe_shaped(M, 'raft_sail', ['RCR', 'CLC', ' L '],
                         {'R': 'raft:rope', 'C': 'raft:sail_canvas', 'L': 'raft:palm_log'}, 'raft_sail')
    assets.recipe_shaped(M, 'raft_engine', ['SCS', 'TFT', ' T '],
                         {'S': 'raft:scrap_metal', 'C': 'raft:circuit_board', 'T': 'raft:titanium_ingot',
                          'F': 'minecraft:furnace'}, 'raft_engine')
    assets.recipe_shaped(M, 'raft_anchor', [' R ', ' I ', 'III'],
                         {'R': 'raft:rope', 'I': 'minecraft:iron_ingot'}, 'raft_anchor')
    assets.recipe_shaped(M, 'raft_crate', ['PRP', 'PCP', 'PPP'],
                         {'P': '#minecraft:planks', 'R': 'raft:rope', 'C': 'minecraft:chest'}, 'raft_crate')
    assets.recipe_shaped(M, 'raft_lantern', ['LRL', 'S S', 'S S'],
                         {'L': 'minecraft:lantern', 'R': 'raft:rope', 'S': 'minecraft:stick'}, 'raft_lantern')

    # tags: our boats count as boats (advancements, fuel, villager trades of other mods)
    assets.tag(M, 'item', 'minecraft:boats', ['raft:palm_boat', 'raft:driftwood_raft'])
    assets.tag(M, 'item', 'minecraft:chest_boats', ['raft:palm_chest_boat', 'raft:driftwood_chest_raft'])
    assets.tag(M, 'entity_type', 'minecraft:boat', ['raft:palm_boat', 'raft:driftwood_raft'])

    assets.lang(M, {
        'itemgroup.raft.voyage': 'Beach Craft: Voyage',
        'entity.raft.palm_boat': 'Palm Boat',
        'entity.raft.palm_chest_boat': 'Palm Boat with Chest',
        'entity.raft.driftwood_raft': 'Driftwood Raft',
        'entity.raft.driftwood_chest_raft': 'Driftwood Raft with Chest',
        'entity.raft.expedition_raft': 'Expedition Raft',
        'item.raft.palm_boat': 'Palm Boat',
        'item.raft.palm_chest_boat': 'Palm Boat with Chest',
        'item.raft.driftwood_raft': 'Driftwood Raft',
        'item.raft.driftwood_chest_raft': 'Driftwood Raft with Chest',
        'item.raft.expedition_raft': 'Expedition Raft',
        'item.raft.raft_sail': 'Raft Sail',
        'item.raft.raft_engine': 'Raft Engine',
        'item.raft.raft_anchor': 'Raft Anchor',
        'item.raft.raft_crate': 'Raft Cargo Crate',
        'item.raft.raft_lantern': 'Raft Lanterns',
        'upgrade.raft.raft_sail': 'Sail',
        'upgrade.raft.raft_engine': 'Engine',
        'upgrade.raft.raft_anchor': 'Anchor',
        'upgrade.raft.raft_crate': 'Cargo Crate',
        'upgrade.raft.raft_lantern': 'Lanterns',
        'tooltip.raft.expedition_raft': 'Four seats and storage. Sneak + use opens the storage.',
        'tooltip.raft.expedition_raft.upgrades': 'Upgrades:',
        'tooltip.raft.expedition_raft.no_upgrades': 'No upgrades yet',
        'tooltip.raft.raft_upgrade': 'Use it on an Expedition Raft',
        'tooltip.raft.raft_sail': 'Mast and sail: 50% faster sailing ahead',
        'tooltip.raft.raft_engine': 'Outboard motor: more than twice as fast, burns furnace fuel',
        'tooltip.raft.raft_anchor': 'Drop it to keep the raft in place',
        'tooltip.raft.raft_crate': '27 more storage slots',
        'tooltip.raft.raft_lantern': 'Lantern posts and rope rails',
        'message.raft.expedition_raft.installed': '%s installed',
        'message.raft.expedition_raft.has_upgrade': 'This raft already has: %s',
        'message.raft.expedition_raft.anchor_down': 'Anchor dropped',
        'message.raft.expedition_raft.anchor_up': 'Anchor raised',
        'message.raft.expedition_raft.hit': 'Taking the raft apart... (%s more hits)',
        'message.raft.expedition_raft.hit_last': 'Taking the raft apart... (one more hit)',
        'message.raft.expedition_raft.no_room': 'Not enough room for the raft here',
        'container.raft.expedition_raft.anchor_down_short': 'Drop',
        'container.raft.expedition_raft.anchor_up_short': 'Raise',
        'container.raft.expedition_raft.anchor_down': 'Drop the anchor: the raft stays where it is',
        'container.raft.expedition_raft.anchor_up': 'Raise the anchor',
        'container.raft.expedition_raft.engine_fuel': 'Engine fuel: coal, wood or any furnace fuel',
        'container.raft.expedition_raft.engine_running': 'Engine fuel (burning)',
        'container.raft.expedition_raft.no_engine': 'Install a Raft Engine to use fuel',
        'container.raft.expedition_raft.no_anchor': 'Install a Raft Anchor to moor the raft',
    })
