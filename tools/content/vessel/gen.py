"""Generates every resource of the vessel module (built rafts) into src/main/modules/vessel/.

Run: cd tools && python3 -m content.vessel.gen   (or python3 tools/content/vessel/gen.py)
Previews go to $BC_PREVIEW (default: the module's work folder in the scratchpad), not into the mod.

Blocks (models face north = the block's `facing`; blockstates turn them):
  helm        ship's wheel on a pedestal, stool aft of it (the helmsman sits there facing the bow)
  sail        mast + canvas; multipart adds the yard (top=true) and the boom (bottom=true)
  oar         rowlock at the outboard edge, the loom slanting down over the side into the water
  raft_motor  outboard motor: clamp on the deck edge, cowling, tiller; shaft and propeller hang over the side
"""
import os
import sys
from collections import OrderedDict

if __package__ in (None, ''):
    sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..')))
    __package__ = 'content.vessel'

from bclib import assets as A, tex  # noqa: E402
from bclib.paths import NS  # noqa: E402

M = 'vessel'
PREVIEW = os.environ.get('BC_PREVIEW', '/tmp/claude-0/-home-user/4ca9508b-4f8c-5a8e-91dc-6059d6cc887c/scratchpad/work/vessel')


def rid(x):
    return A.rid(x)


# ------------------------------------------------------------------------------------------------- textures
def motor_texture():
    """16x16 sheet for the raft motor: v0-8 red cowling with a white stripe, v8-12 steel, v12-16 dark steel/brass."""
    img = tex.new()
    red = tex.ramp('#c9473b', 5, spread=0.42)
    steel = tex.ramp(tex.PAL['steel'], 5, spread=0.5)
    dark = tex.ramp('#4a5059', 4, spread=0.35)
    brass = tex.ramp(tex.PAL['brass'], 4, spread=0.4)
    white = tex.ramp('#efeae0', 3, spread=0.18)
    for y in range(16):
        for x in range(16):
            if y < 8:
                if y in (4, 5):
                    c = white[2] if y == 4 else white[1]
                elif y == 0:
                    c = red[4]
                elif y < 4:
                    c = red[3] if (x + y) % 7 else red[4]
                elif y == 7:
                    c = red[0]
                else:
                    c = red[1] if x % 5 else red[2]
                if x in (0, 15) and y not in (4, 5):
                    c = tex.darker(c, 0.85)
            elif y < 12:
                c = steel[3] if y == 8 else steel[2] if y < 11 else steel[1]
                if x % 4 == 3:
                    c = steel[1] if y < 11 else steel[0]
            else:
                c = dark[2] if y == 12 else dark[1] if y < 15 else dark[0]
                if 12 <= x <= 15 and y in (13, 14):
                    c = brass[2] if y == 13 else brass[1]
            tex.px(img, x, y, c)
    # vent slits on the cowling
    for x in (3, 5, 7):
        tex.px(img, x, 2, red[1])
        tex.px(img, x, 3, red[0])
    return img


ITEM_PAL = {
    '1': '#4f3a22', '2': '#6f5230', '3': '#8f6c3e', '4': '#b08950', '5': '#cfa96b',
    'w': '#f2ede1', 'v': '#d8cfbb', 'u': '#b3a88f',
    'y': '#dcc083', 'Y': '#a98a50',
    'g': '#ecc35a', 'G': '#b08530',
    'h': '#e2574a', 'H': '#b23a30', 'k': '#7c2620',
    'i': '#b4bcc6', 'I': '#828a95', 'j': '#59606b', 'J': '#3b4048',
    'a': '#c9a46a', 'b': '#a7804b', 'c': '#7e5c35',
}

ITEM_ART = {
    'sail': [
        '.......54.......',
        '..3444444443....',
        '...wwwww4vvu....',
        '...wwwww4vvu....',
        '...wwwww4vvu....',
        '...wwwww4vvu....',
        '...vwwww4vvu....',
        '...vwwww4vuu....',
        '...vvwww4vuu....',
        '...uvvvv4uuu....',
        '..2333333332....',
        '.......42.......',
        '.......42.......',
        '.......42.......',
        '......4422......',
        '................',
    ],
    'oar': [
        '................',
        '............55..',
        '...........5444.',
        '..........54443.',
        '..........44433.',
        '.........4.433..',
        '........4..33...',
        '.......4........',
        '......4.........',
        '.....4..........',
        '..JjjjjJ........',
        '...Ij4jI........',
        '...I4.I.........',
        '..32...I........',
        '.32....I........',
        '................',
    ],
    'raft_motor': [
        '................',
        '.....hhhhh......',
        '....hhhhhhH.....',
        '...hhhhhhhhH.34.',
        '...hwwwwwwwH345.',
        '...HHHHHHHHk2...',
        '.bb.HHHHHHk.....',
        '.bab.kjiIjk.....',
        '.bab..jiIj......',
        '..c....iI.......',
        '.......iI.......',
        '.....jjiIjjj....',
        '.......iI.......',
        '......JiIJ......',
        '....JJJgGJJJ....',
        '.......JJ.......',
    ],
}


def helm_sprite():
    """A ship's wheel (ring, eight spokes with handles, brass hub) on a short pedestal."""
    wood = tex.ramp('#a87d47', 5, spread=0.6)
    brass = tex.ramp(tex.PAL['brass'], 4, spread=0.45)
    post = tex.ramp('#76583a', 4, spread=0.5)
    img = tex.new()
    cx, cy = 7, 6
    ring = ['...###...', '.##...##.', '.#.....#.', '#.......#', '#.......#', '#.......#', '.#.....#.', '.##...##.',
            '...###...']
    for j, row in enumerate(ring):
        for i, ch in enumerate(row):
            if ch == '#':
                tex.px(img, cx - 4 + i, cy - 4 + j, wood[3] if i + j < 8 else wood[1])
    for dx, dy in ((0, -1), (1, -1), (1, 0), (1, 1), (0, 1), (-1, 1), (-1, 0), (-1, -1)):
        straight = dx == 0 or dy == 0
        for k in range(2, (6 if straight else 5) + 1):
            x, y = cx + dx * k, cy + dy * k
            c = wood[2] if (k <= 3 if straight else k <= 2) else None
            if (straight and k >= 5) or (not straight and k >= 4):
                c = wood[4] if dx + dy < 0 else wood[2]
            if c is not None and img.getpixel((x, y))[3] == 0:
                tex.px(img, x, y, c)
    for x, y, c in ((7, 6, brass[3]), (6, 6, brass[2]), (8, 6, brass[1]), (7, 5, brass[3]), (7, 7, brass[1])):
        tex.px(img, x, y, c)
    for y in range(13, 15):
        tex.px(img, 7, y, post[2])
        tex.px(img, 8, y, post[1])
    for x in range(5, 11):
        tex.px(img, x, 15, post[2] if x < 8 else post[1])
    return tex.outline(img)


def item_sprite(name):
    if name == 'helm':
        return helm_sprite()
    rows = ITEM_ART[name]
    img = tex.new()
    for y, row in enumerate(rows):
        assert len(row) == 16, (name, y, row)
        for x, ch in enumerate(row):
            if ch != '.':
                tex.px(img, x, y, ITEM_PAL[ch])
    return tex.outline(img)


# ------------------------------------------------------------------------------------------------- model helpers
def auto_uv(face, frm, to):
    x1, y1, z1 = frm
    x2, y2, z2 = to
    if face in ('north', 'south'):
        return [x1, 16 - y2, x2, 16 - y1]
    if face in ('east', 'west'):
        return [z1, 16 - y2, z2, 16 - y1]
    return [x1, z1, x2, z2]


def clamp_uv(uv):
    return [min(16.0, max(0.0, float(v))) for v in uv]


def el(frm, to, texture, uv=None, faces=None, rot=None, cull=None):
    """A model element. uv: None (auto from the position, clamped), a list (all faces) or a dict per face."""
    faces = faces or ('north', 'east', 'south', 'west', 'up', 'down')
    f = OrderedDict()
    for face in faces:
        if isinstance(uv, dict):
            u = uv.get(face) or uv.get('*') or clamp_uv(auto_uv(face, frm, to))
        elif uv is not None:
            u = uv
        else:
            u = clamp_uv(auto_uv(face, frm, to))
        d = OrderedDict(uv=u, texture='#' + texture)
        if cull and face in cull:
            d['cullface'] = face
        f[face] = d
    e = OrderedDict([('from', list(frm)), ('to', list(to))])
    if rot:
        e['rotation'] = rot
    e['faces'] = f
    return e


def model(name, textures, elements, parent='minecraft:block/block', extra=None):
    d = OrderedDict(parent=parent)
    d['textures'] = OrderedDict((k, rid(v)) for k, v in textures.items())
    d['elements'] = elements
    if extra:
        d.update(extra)
    A.write_json(M, f'assets/{NS}/models/block/{name}.json', d)


# ------------------------------------------------------------------------------------------------- models
def helm_elements():
    wheel_uv = {'north': [16, 0, 0, 16], 'south': [0, 0, 16, 16], 'east': [7, 0, 8, 16], 'west': [8, 0, 9, 16],
                'up': [0, 7, 16, 8], 'down': [0, 8, 16, 9]}
    return [
        el((5, 0, 3), (11, 2, 8), 'wood'),
        el((6.5, 2, 4), (9.5, 12, 7), 'post'),
        el((7, 11, 6), (9, 13, 7.5), 'post'),
        el((0, 4, 7.5), (16, 20, 8.5), 'wheel', uv=wheel_uv),
        el((4, 6, 10), (12, 7.5, 15.5), 'wood'),
        el((4.5, 0, 10.5), (6, 6, 12), 'post'),
        el((10, 0, 10.5), (11.5, 6, 12), 'post'),
        el((4.5, 0, 13.5), (6, 6, 15), 'post'),
        el((10, 0, 13.5), (11.5, 6, 15), 'post'),
    ]


def sail_base():
    canvas_uv = {'north': [15.5, 0, 0.5, 16], 'south': [0.5, 0, 15.5, 16], 'east': [0, 0, 0.5, 16],
                 'west': [15.5, 0, 16, 16], 'up': [0.5, 0, 15.5, 0.5], 'down': [0.5, 15.5, 15.5, 16]}
    return [
        el((7, 0, 7), (9, 16, 9), 'mast'),
        el((0.5, 0, 9.25), (15.5, 16, 9.75), 'canvas', uv=canvas_uv),
    ]


def spar(y0, y1):
    uv = {'north': [0, 6, 16, 7.5], 'south': [0, 8, 16, 9.5], 'east': [6, 0, 7.5, 1.5], 'west': [8, 0, 9.5, 1.5],
          'up': [0, 2, 16, 3.5], 'down': [0, 11, 16, 12.5]}
    return el((-3, y0, 8.25), (19, y1, 9.75), 'mast', uv=uv)


def sail_yard():
    return [spar(14.5, 16), el((7, 16, 7), (9, 19, 9), 'mast', uv={'*': [7, 0, 9, 3]}),
            el((7.5, 19, 7.5), (8.5, 20, 8.5), 'rope', uv={'*': [6, 6, 7, 7]})]


def sail_boom():
    return [spar(0.5, 2)]


def oar_elements():
    rot = OrderedDict(origin=[8, 7.5, 3], axis='x', angle=-22.5)
    loom_uv = {'north': [7, 7, 8, 8], 'south': [8, 7, 9, 8], 'east': [0, 7, 16, 8], 'west': [0, 8, 16, 9],
               'up': [7, 0, 8, 16], 'down': [8, 0, 9, 16]}
    blade_uv = {'north': [6, 6, 10, 7.5], 'south': [6, 8, 10, 9.5], 'east': [0, 4, 7, 5.5], 'west': [9, 4, 16, 5.5],
                'up': [6, 0, 10, 7], 'down': [6, 9, 10, 16]}
    grip_uv = {'*': [7, 12, 8.5, 15]}
    return [
        el((5.5, 0, 0.5), (10.5, 2, 5.5), 'lock'),
        el((7, 2, 2), (9, 7, 4), 'lock'),
        el((7.5, 7, -14), (8.5, 8, 16), 'loom', uv=loom_uv, rot=rot),
        el((6, 6.75, -16), (10, 8.25, -9), 'blade', uv=blade_uv, rot=rot),
        el((7.25, 6.75, 12.5), (8.75, 8.25, 16), 'grip', uv=grip_uv, rot=rot),
    ]


def motor_elements():
    red_top = [2, 0, 14, 2]
    steel = [0, 8, 16, 11]
    dark = [0, 12, 12, 15]
    brass = [12, 13, 16, 15]
    return [
        el((5, 0, 0), (11, 3, 4.5), 'motor', uv=steel),
        el((6.5, 3, -2), (9.5, 7, 2.5), 'motor', uv=steel),
        el((4, 6, 2), (12, 14, 11), 'motor', uv={'north': [4, 0, 12, 8], 'south': [4, 0, 12, 8], 'east': [2, 0, 11, 8],
                                                   'west': [5, 0, 14, 8], 'up': red_top, 'down': [0, 7, 16, 8]}),
        el((4.5, 14, 2.5), (11.5, 15, 10.5), 'motor', uv=[0, 12, 7, 13]),
        el((7.25, 9, 11), (8.75, 10.5, 16), 'motor', uv=steel),
        el((7, 8.75, 15), (9, 10.75, 18.5), 'motor', uv=dark),
        el((7, -11, -2), (9, 3, 0), 'motor', uv=steel),
        el((6.5, -13, -5), (9.5, -10, 1), 'motor', uv=dark),
        el((5, -9, -5), (11, -8.5, 0), 'motor', uv=steel),
        el((7.25, -12.25, -6), (8.75, -10.75, -5), 'motor', uv=brass),
        el((4, -12, -5.75), (12, -11, -5.25), 'motor', uv=dark),
        el((7.5, -15.5, -5.75), (8.5, -7.5, -5.25), 'motor', uv=dark),
    ]


def models():
    model('helm', {'particle': 'block/palm_planks', 'wheel': 'block/ship_wheel', 'wood': 'block/palm_planks',
                   'post': 'block/stripped_palm_log'}, helm_elements())
    sail_tex = {'particle': 'block/sail_canvas', 'mast': 'block/palm_log', 'canvas': 'block/sail_canvas',
                'rope': 'block/rope_coil'}
    model('sail', sail_tex, sail_base())
    model('sail_yard', sail_tex, sail_yard())
    model('sail_boom', sail_tex, sail_boom())
    model('oar', {'particle': 'block/palm_planks', 'lock': 'block/driftwood_log', 'loom': 'block/stripped_palm_log',
                  'blade': 'block/palm_planks', 'grip': 'block/rope_coil'}, oar_elements())
    model('raft_motor', {'particle': 'block/raft_motor', 'motor': 'block/raft_motor'}, motor_elements())


def blockstates():
    A.blockstate(M, 'helm', A.horizontal_variants('block/helm'))
    A.blockstate(M, 'oar', A.horizontal_variants('block/oar'))
    A.blockstate(M, 'raft_motor', A.horizontal_variants('block/raft_motor'))
    parts = []
    for part, cond in (('sail', None), ('sail_yard', ('top', 'true')), ('sail_boom', ('bottom', 'true'))):
        for facing, rot in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
            apply = OrderedDict(model=rid('block/' + part))
            if rot:
                apply['y'] = rot
            when = OrderedDict(facing=facing)
            if cond:
                when[cond[0]] = cond[1]
            parts.append(OrderedDict(when=when, apply=apply))
    A.blockstate(M, 'sail', multipart=parts)


def items():
    sprites = {}
    for name in ['helm'] + list(ITEM_ART):
        img = item_sprite(name)
        tex.save(img, M, 'item/' + name)
        A.flat_item(M, name)
        sprites[name] = img
    return sprites


# ------------------------------------------------------------------------------------------------- data
def loot_self(block):
    A.write_json(M, f'data/{NS}/loot_table/blocks/{block}.json', OrderedDict([
        ('type', 'minecraft:block'),
        ('pools', [OrderedDict([('condition', {'type': 'minecraft:survives_explosion'}),
                                ('entries', [{'type': 'minecraft:item', 'name': rid(block)}]), ('rolls', 1)])]),
        ('random_sequence', f'{NS}:blocks/{block}')]))


def recipe_unlock(name, items_needed):
    A.write_json(M, f'data/{NS}/advancement/recipes/transportation/{name}.json', OrderedDict([
        ('parent', 'minecraft:recipes/root'),
        ('criteria', OrderedDict([
            ('has_ingredient', {'conditions': {'items': [{'items': [rid(i) for i in items_needed]}]},
                                'trigger': 'minecraft:inventory_changed'}),
            ('has_the_recipe', {'conditions': {'recipes': rid(name)}, 'trigger': 'minecraft:recipe_unlocked'})])),
        ('requirements', [['has_ingredient', 'has_the_recipe']]),
        ('rewards', {'recipes': [rid(name)]})]))


def data():
    for b in ('helm', 'sail', 'oar', 'raft_motor'):
        loot_self(b)
    A.tag(M, 'block', 'minecraft:mineable/axe', ['helm', 'sail', 'oar'])
    A.tag(M, 'block', 'minecraft:mineable/pickaxe', ['raft_motor'])
    A.tag(M, 'block', 'raft:vessel_hulls', ['raft:raft_foundation', 'raft:reinforced_foundation'])
    A.tag(M, 'block', 'raft:not_seaworthy', [
        'minecraft:bedrock', 'minecraft:barrier', 'minecraft:command_block', 'minecraft:chain_command_block',
        'minecraft:repeating_command_block', 'minecraft:structure_block', 'minecraft:structure_void',
        'minecraft:jigsaw', 'minecraft:test_block', 'minecraft:test_instance_block', 'minecraft:end_portal',
        'minecraft:end_portal_frame', 'minecraft:end_gateway', 'minecraft:nether_portal', 'minecraft:moving_piston',
        'minecraft:reinforced_deepslate', {'id': 'raft:raft_portal', 'required': False}])

    A.recipe_shaped(M, 'helm', [' W ', ' S ', 'PPP'],
                    {'W': 'raft:ship_wheel', 'S': 'minecraft:stick', 'P': '#minecraft:planks'}, 'helm', category='misc')
    A.recipe_shaped(M, 'sail', [' R ', 'CSC', ' S '],
                    {'R': 'raft:rope', 'C': 'raft:sail_canvas', 'S': 'minecraft:stick'}, 'sail', category='misc')
    A.recipe_shapeless(M, 'oar', ['raft:paddle', 'raft:rope', 'minecraft:stick'], 'oar', category='misc')
    A.recipe_shaped(M, 'raft_motor', ['SCS', 'SFS', ' T '],
                    {'S': 'raft:scrap_metal', 'C': 'raft:circuit_board', 'F': 'minecraft:furnace',
                     'T': 'raft:titanium_ingot'}, 'raft_motor', category='misc')
    recipe_unlock('helm', ['raft:ship_wheel', 'raft:raft_foundation'])
    recipe_unlock('sail', ['raft:sail_canvas'])
    recipe_unlock('oar', ['raft:paddle'])
    recipe_unlock('raft_motor', ['raft:circuit_board', 'raft:titanium_ingot'])

    # advancement (adventure tab): set sail on a raft you built
    A.write_json(M, f'data/{NS}/advancement/adventure/shipwright.json', OrderedDict([
        ('parent', 'raft:adventure/set_sail'),
        ('criteria', {'sail': {'conditions': {'player': {
            'type': 'minecraft:entity_properties', 'entity': 'this',
            'predicate': {'minecraft:vehicle': {'minecraft:entity_type': 'raft:vessel'}}}},
            'trigger': 'minecraft:started_riding'}}),
        ('display', OrderedDict([
            ('description', {'translate': 'advancements.raft.adventure.shipwright.description'}),
            ('frame', 'goal'),
            ('icon', {'id': 'raft:helm'}),
            ('title', {'translate': 'advancements.raft.adventure.shipwright.title'})])),
        ('requirements', [['sail']]),
        ('rewards', {'experience': 30})]))


ALMANAC = """\
Build a raft of your own and sail it like a ship.
# Foundations
Look at open water and place a Raft Foundation: it settles into the sea, its top level with the waves. Set more against its sides to grow the raft block by block.
# Build on it
Floors, walls, a roof, chests, stations, decorations: everything that stands on the foundations sails along. Keep it inside the edge of the foundations.
# The Helm
Set a Helm on the raft so that it faces the bow, and use it to set sail. W and S sail, A and D steer.
Sneak or use the wheel again to leave the helm: the raft slows down, turns to the nearest compass direction and drops anchor. Then it is blocks again, and chests keep what is in them.
# Power
* Oar: rowing power. Set it on the deck edge, pointing out.
* Sail: wind power. Stack sails for a taller mast; each sail counts.
* Raft Motor: the most power. Set it on the stern edge and use furnace fuel on it, even while you sail.
Heavy rafts need more power: they are slower and turn more slowly.
# Good to know
* Up to 1024 blocks and 32 blocks across.
* A raft needs water under it. Land, ice and piers stop it.
* Friends, animals and items on deck sail along.
* Blocks of a sailing raft can't be broken or used: drop anchor first.
* No room to drop anchor? Sail a little away from the shore."""

LANG = {
    'block.raft.helm': 'Helm',
    'block.raft.sail': 'Sail',
    'block.raft.oar': 'Oar',
    'block.raft.raft_motor': 'Raft Motor',
    'entity.raft.vessel': 'Raft',
    'tooltip.raft.helm': 'Set it on your raft facing the bow and use it to set sail.',
    'tooltip.raft.helm.1': 'Everything on the raft foundations sails along.',
    'tooltip.raft.sail': 'Wind power for a raft you built. Stack sails for a taller mast.',
    'tooltip.raft.oar': 'Rowing power for a raft you built. Set it on the deck edge.',
    'tooltip.raft.raft_motor': 'Strong power for a raft you built. Set it on the stern edge.',
    'tooltip.raft.raft_motor.1': 'Runs on furnace fuel: use fuel on it.',
    'message.raft.vessel.no_hull': 'The helm must stand on a raft of Raft Foundations.',
    'message.raft.vessel.too_big': 'This raft is too big to sail (more than %s blocks).',
    'message.raft.vessel.too_wide': 'This raft is too wide to sail (more than %s blocks across).',
    'message.raft.vessel.not_seaworthy': "%s can't go to sea.",
    'message.raft.vessel.not_floating': 'The raft must float on water to set sail.',
    'message.raft.vessel.not_loaded': 'Part of this raft is too far away to set sail.',
    'message.raft.vessel.controls': 'W/S: sail · A/D: steer · Sneak: leave the helm and drop anchor',
    'message.raft.vessel.anchoring': 'Dropping anchor...',
    'message.raft.vessel.anchored': 'Anchor dropped. The raft is blocks again.',
    'message.raft.vessel.no_room': 'No room to drop anchor here. Sail a little away from the shore.',
    'message.raft.vessel.helm_taken': 'Someone is already at the helm.',
    'message.raft.vessel.anchor_first': 'Drop anchor to use this.',
    'message.raft.vessel.motor_fuel': 'Motor fuel: %s',
    'message.raft.vessel.motor_full': "The motor's tank is full.",
    'message.raft.vessel.motor_empty': 'The motor has no fuel. Use coal, charcoal, logs or planks on it.',
    'advancements.raft.adventure.shipwright.title': 'Shipwright',
    'advancements.raft.adventure.shipwright.description': 'Set sail on a raft you built yourself',
    'almanac.raft.sailing.shipwright.title': 'Your Own Raft',
    'almanac.raft.sailing.shipwright.text': ALMANAC,
}


def main():
    tex.save(motor_texture(), M, 'block/raft_motor')
    models()
    blockstates()
    sprites = items()
    data()
    A.lang(M, LANG)
    A.flush(M)
    if PREVIEW and os.path.isdir(os.path.dirname(PREVIEW)):
        os.makedirs(PREVIEW, exist_ok=True)
        tex.sheet(list(sprites.values()) + ['raft:block/raft_motor', 'raft:item/raft_engine', 'raft:item/paddle',
                                            'raft:item/raft_sail', 'raft:block/ship_wheel'],
                  os.path.join(PREVIEW, 'vessel_items.png'), scale=6, cols=5)


if __name__ == '__main__':
    main()
