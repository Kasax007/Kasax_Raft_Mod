"""Generator of the "cozy" module (Beach Craft 3.3): furniture, lights, plants, shells, dock and wall decor, music,
building blocks and the comfort tags.

Run:  cd tools && python3 -m content.cozy.gen
Writes textures, models, blockstates, item definitions, loot tables, recipes (+ recipe book unlocks), tags and lang
into src/main/modules/cozy/ and the shape boxes into src/main/java/net/kasax/raft/content/cozy/block/CozyShapeData.java.
Re-runnable: wipes its output folder first. Prints LINT lines for z-fighting faces or transparent texels on boxes.
"""
import json
import os
import shutil
import sys

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..')))

from bclib import assets as A  # noqa: E402
from bclib import tex  # noqa: E402
from bclib.paths import module_dir, ROOT, VANILLA  # noqa: E402

from content.cozy import cm, paint as P  # noqa: E402
from content.cozy.cm import Model, rid, facing_variants  # noqa: E402
from content.cozy import recipes as R  # noqa: E402
from content.cozy import sound  # noqa: E402
from content.cozy.almanac import PAGES  # noqa: E402
from content.decor import art  # noqa: E402

M = 'cozy'

PALM, TEAK, WEATHERED, DRIFT = 'raft:block/palm_planks', 'raft:block/teak_planks', 'raft:block/weathered_planks', \
    'raft:block/driftwood_planks'
PALM_LOG, DRIFT_LOG = 'raft:block/stripped_palm_log', 'raft:block/stripped_driftwood_log'
BAMBOO, BAMBOO_BLOCK = 'minecraft:block/bamboo_planks', 'minecraft:block/bamboo_block'
THATCH = 'raft:block/palm_thatch'

# id: (English name, tool or None, loot kind)
BLOCKS = {}
ITEM_NAMES = {}
LANG = {
    'itemgroup.raft.cozy': 'Beach Craft: Cozy',
    'container.raft.wicker_basket': 'Wicker Basket',
    'effect.raft.rested': 'Rested',
    'message.raft.comfort': 'Comfort %s: rested for %s min',
    'message.raft.comfort.low': 'Comfort %s: add lights, plants, rugs or a table nearby to rest better',
    'message.raft.plate.full': 'The plate is full',
    'message.raft.plate.not_food': 'Only food goes on a plate',
    'message.raft.plate.not_hungry': 'You are not hungry',
    'tooltip.raft.sit': 'Right-click to sit down',
    'tooltip.raft.comfort': 'Counts towards comfort',
    'tooltip.raft.toggle_light': 'Right-click to switch it on or off',
    'tooltip.raft.bench': 'Benches join when placed side by side',
    'tooltip.raft.plate': 'Put food on it; right-click to eat',
    'tooltip.raft.plate.1': 'Sneak-click with an empty hand to take it back',
    'tooltip.raft.wicker_basket': '9 slots; keeps its contents when picked up',
    'tooltip.raft.steel_drum': 'Right-click to play the next note of a tune',
    'tooltip.raft.conch_shell': 'Right-click to blow the horn',
    'tooltip.raft.bamboo_blinds': 'Right-click to roll them up or down',
    'tooltip.raft.parasol': 'Gives shade: counts towards comfort',
    'tooltip.raft.shelf': 'Shows up to three items',
    'tooltip.raft.rested': 'Sit down near cozy things to rest',
}


def blk(bid, name, tool=None, loot='self'):
    BLOCKS[bid] = (name, tool, loot)


def tx(img, name):
    """Save a block texture of this module and return its id."""
    P.save(img, f'block/{name}')
    return f'raft:block/{name}'


def stateless(bid, model):
    A.blockstate(M, bid, {'': {'model': rid(model)}})


# ================================================================================================= furniture
def benches():
    """Benches (palm, teak): a slatted seat and back; the end parts carry legs and armrests."""
    for wood, planks in (('palm', PALM), ('teak', TEAK)):
        bid = f'{wood}_bench'
        blk(bid, f'{wood.capitalize()} Bench', 'axe')
        variants = {}
        for part in ('single', 'left', 'center', 'right'):
            m = Model(M, {'wood': planks, 'leg': planks}, ao=True, particle=planks)
            m.box((0, 7, 2), (16, 9, 13), 'wood')                     # seat
            m.box((0, 10, 13), (16, 15, 14.5), 'wood', collide=False)  # back rest
            west = part in ('single', 'left')
            east = part in ('single', 'right')
            for x0, on in ((1, west), (13, east)):
                if not on:
                    continue
                m.box((x0, 0, 3), (x0 + 2, 7, 5), 'leg')
                m.box((x0, 0, 11), (x0 + 2, 7, 13), 'leg')
                m.box((x0, 9, 12.75), (x0 + 2, 16, 14.75), 'leg', collide=False)   # back post
                m.box((x0, 11, 3), (x0 + 2, 12.5, 12.75), 'leg', collide=False)   # arm
                m.box((x0, 9, 3), (x0 + 2, 11, 4.5), 'leg', collide=False)     # arm post
            if part == 'center':
                m.box((7, 0, 6), (9, 7, 9), 'leg')
            m.write(f'{bid}_{part}')
            shp = [(0, 0, 2, 16, 9, 13), (0, 9, 13, 16, 15, 14.5)]
            cm.shape(M, f'{bid}_{part}', shp)
            variants.update(facing_variants(f'block/{bid}_{part}', f'part={part}'))
        A.blockstate(M, bid, variants)
        cm.block_item(M, bid, f'block/{bid}_single', scale=0.6)


def bar_stool():
    blk('bar_stool', 'Bamboo Bar Stool', 'axe')
    cush = tx(P.cushion_cloth('#c8463c'), 'bar_stool_cushion')
    m = Model(M, {'pole': BAMBOO_BLOCK, 'seat': cush, 'wood': BAMBOO}, ao=True, particle=BAMBOO)
    m.box((4, 12, 4), (12, 13.5, 12), 'wood')
    m.box((4.5, 13.5, 4.5), (11.5, 15, 11.5), 'seat')
    m.box((7, 1, 7), (9, 12, 9), 'pole')
    m.box((5, 0, 5), (11, 1, 11), 'wood')
    for (a, b) in (((5, 5, 5), (11, 6, 6)), ((5, 5, 10), (11, 6, 11)), ((5, 5, 6), (6, 6, 10)), ((10, 5, 6), (11, 6, 10))):
        m.box(a, b, 'wood', collide=False)
    m.write('bar_stool')
    cm.shape(M, 'bar_stool', [(4, 0, 4, 12, 15, 12)])
    stateless('bar_stool', 'block/bar_stool')
    cm.block_item(M, 'bar_stool', 'block/bar_stool', scale=0.7)


def sun_lounger():
    blk('sun_lounger', 'Sun Lounger', 'axe')
    cush = tx(P.stripes_cloth('#f3efe4', '#3d82c8'), 'sun_lounger_cushion')
    m = Model(M, {'frame': PALM, 'cush': cush}, ao=True, particle=PALM)
    m.box((1, 3, 0), (15, 5, 10), 'frame')
    m.box((2, 5, 0.5), (14, 6.5, 10), 'cush')
    for x in (1.5, 12.5):
        m.box((x, 0, 0.5), (x + 2, 3, 2.5), 'frame')
        m.box((x, 0, 11.5), (x + 2, 3, 13.5), 'frame')
    m.box((1, 3, 10), (15, 5, 16), 'frame')
    # the raised back rest, tilted backwards
    m.box((2, 5, 9.5), (14, 6.5, 16.5), 'cush', rot={'origin': [8, 5, 10], 'axis': 'x', 'angle': -22.5}, collide=False)
    m.box((1, 4, 10), (15, 5.5, 16.5), 'frame', rot={'origin': [8, 5, 10], 'axis': 'x', 'angle': -22.5}, collide=False)
    m.write('sun_lounger')
    cm.shape(M, 'sun_lounger', [(1, 0, 0, 15, 6.5, 16), (1, 6.5, 12, 15, 10, 16)])
    A.blockstate(M, 'sun_lounger', facing_variants('block/sun_lounger'))
    cm.block_item(M, 'sun_lounger', 'block/sun_lounger', scale=0.6)


def rattan_armchair():
    blk('rattan_armchair', 'Rattan Armchair', 'axe')
    wk = tx(P.wicker(), 'rattan')
    cush = tx(P.cushion_cloth('#e9dcc0'), 'rattan_cushion')
    m = Model(M, {'wicker': wk, 'cush': cush}, ao=True, particle=wk)
    m.box((3, 0, 2.5), (13, 6, 12.5), 'wicker')
    m.box((3, 6, 2.5), (13, 8, 12.5), 'cush')
    m.box((1, 0, 12.5), (15, 17, 14.5), 'wicker')
    m.box((1, 0, 2), (3, 11, 12.5), 'wicker')
    m.box((13, 0, 2), (15, 11, 12.5), 'wicker')
    m.box((3, 8, 11.5), (13, 14, 12.5), 'cush', collide=False)
    m.write('rattan_armchair')
    cm.shape(M, 'rattan_armchair', [(1, 0, 2, 15, 8, 14.5), (1, 8, 12.5, 15, 17, 14.5), (1, 8, 2, 3, 11, 12.5),
                                    (13, 8, 2, 15, 11, 12.5)])
    A.blockstate(M, 'rattan_armchair', facing_variants('block/rattan_armchair'))
    cm.block_item(M, 'rattan_armchair', 'block/rattan_armchair', scale=0.6)


def tables():
    # teak: a round-cornered top on a central pedestal
    blk('teak_table', 'Teak Table', 'axe')
    m = Model(M, {'top': TEAK}, ao=True, particle=TEAK)
    m.box((0, 14, 0), (16, 16, 16), 'top', cull=True)
    m.box((6, 2, 6), (10, 14, 10), 'top')
    m.box((2, 0, 6.5), (14, 2, 9.5), 'top')
    m.box((6.5, 0, 2), (9.5, 2, 6.5), 'top')
    m.box((6.5, 0, 9.5), (9.5, 2, 14), 'top')
    m.write('teak_table')
    cm.shape(M, 'teak_table', [(0, 14, 0, 16, 16, 16), (6, 2, 6, 10, 14, 10), (2, 0, 2, 14, 2, 14)])
    stateless('teak_table', 'block/teak_table')
    cm.block_item(M, 'teak_table', 'block/teak_table', scale=0.6)
    # bamboo: slatted top, bamboo legs with braces
    blk('bamboo_table', 'Bamboo Table', 'axe')
    m = Model(M, {'top': BAMBOO, 'leg': BAMBOO_BLOCK}, ao=True, particle=BAMBOO)
    m.box((0, 14, 0), (16, 16, 16), 'top', cull=True)
    for x, z in ((1, 1), (13, 1), (1, 13), (13, 13)):
        m.box((x, 0, z), (x + 2, 14, z + 2), 'leg')
    m.box((3, 5, 1.5), (13, 6, 2.5), 'leg', collide=False)
    m.box((3, 5, 13.5), (13, 6, 14.5), 'leg', collide=False)
    m.write('bamboo_table')
    cm.shape(M, 'bamboo_table', [(0, 14, 0, 16, 16, 16), (1, 0, 1, 3, 14, 3), (13, 0, 1, 15, 14, 3), (1, 0, 13, 3, 14, 15),
                                 (13, 0, 13, 15, 14, 15)])
    stateless('bamboo_table', 'block/bamboo_table')
    cm.block_item(M, 'bamboo_table', 'block/bamboo_table', scale=0.6)


def serving_plate():
    blk('serving_plate', 'Serving Plate', 'pickaxe', 'self')
    top = tx(P.plate_top(), 'serving_plate_top')
    side = tx(P.ceramic(), 'serving_plate_side')
    m = Model(M, {'top': top, 'side': side}, ao=False, particle=side)
    m.box((4, 0, 4), (12, 0.5, 12), 'side')
    m.box((1.5, 0.5, 1.5), (14.5, 1.5, 14.5), 'side', faces={'up': ('top', [0.5, 0.5, 15.5, 15.5])})
    m.write('serving_plate')
    cm.shape(M, 'serving_plate', [(1.5, 0, 1.5, 14.5, 1.5, 14.5)])
    stateless('serving_plate', 'block/serving_plate')
    cm.block_item(M, 'serving_plate', 'block/serving_plate', scale=0.9, lift=1.5)


def shelves():
    """Display shelves: the vanilla shelf (three item spots, swap with the hotbar) in palm, teak and weathered wood."""
    src = tex.load('minecraft:block/oak_shelf').convert('RGBA')
    for wood, ramp in (('palm', art.PALM), ('teak', art.TEAK), ('weathered', tex.ramp('#8a8378', 6, 0.5))):
        bid = f'{wood}_shelf'
        blk(bid, f'{wood.capitalize()} Shelf', 'axe')
        P.save(P.recolour(src, None, ramp), f'block/{bid}')
        A.from_vanilla(M, bid, 'oak_shelf', {'all': f'raft:block/{bid}', 'particle': f'raft:block/{bid}'})


def wicker_basket():
    blk('wicker_basket', 'Wicker Basket', 'axe', 'basket')
    wk = 'raft:block/rattan'
    lid = tx(P.wicker(), 'wicker_basket_lid')
    for open_ in (False, True):
        m = Model(M, {'w': wk, 'lid': lid, 'cloth': 'raft:block/rattan_cushion'}, ao=True, particle=wk)
        m.box((2, 0, 3), (14, 9, 13), 'w', faces={'up': None})
        m.box((2.5, 1, 3.5), (13.5, 8.5, 12.5), 'cloth', only=('up',), collide=False, solid=False)
        if open_:
            m.box((2, 9, 12), (14, 19, 13), 'lid', rot={'origin': [8, 9, 13], 'axis': 'x', 'angle': -22.5}, collide=False)
        else:
            m.box((1.5, 9, 2.5), (14.5, 10.5, 13.5), 'lid')
            m.box((7, 10.5, 7), (9, 11.5, 9), 'w', collide=False)
        # carrying handles on the short sides
        m.box((1, 6, 6.5), (2, 8, 9.5), 'w', collide=False)
        m.box((14, 6, 6.5), (15, 8, 9.5), 'w', collide=False)
        name = 'wicker_basket_open' if open_ else 'wicker_basket'
        m.write(name)
        cm.shape(M, name, [(2, 0, 3, 14, 9, 13)] if open_ else [(1.5, 0, 2.5, 14.5, 10.5, 13.5)])
    v = {}
    for o in ('false', 'true'):
        v.update(facing_variants('block/wicker_basket_open' if o == 'true' else 'block/wicker_basket', f'open={o}'))
    A.blockstate(M, 'wicker_basket', v)
    cm.block_item(M, 'wicker_basket', 'block/wicker_basket', scale=0.75)


def rugs():
    for colour, (c1, c2) in (('red', ('#d4473c', '#f2ece0')), ('blue', ('#3c78c8', '#f2ece0')),
                             ('yellow', ('#efc23a', '#e8803a'))):
        bid = f'beach_towel_{colour}'
        blk(bid, f'{colour.capitalize()} Beach Towel')
        t = tx(P.towel(c1, c2), bid)
        A.block_model(M, bid, 'minecraft:block/carpet', {'wool': t})
        stateless(bid, f'block/{bid}')
        A.block_item(M, bid)
    blk('rope_rug', 'Rope Rug')
    t = tx(P.rope_rug(), 'rope_rug')
    m = Model(M, {'r': t}, ao=True, particle=t, opaque=())
    m.box((0, 0, 0), (16, 1, 16), 'r', only=('up', 'down'), collide=False, solid=False)
    # cut-out rim faces so the round mat looks thick
    m.write('rope_rug')
    cm.shape(M, 'rope_rug', [(0, 0, 0, 16, 1, 16)])
    stateless('rope_rug', 'block/rope_rug')
    A.flat_item(M, 'rope_rug', 'block/rope_rug')


# ================================================================================================= lights
def hurricane_lantern():
    blk('hurricane_lantern', 'Hurricane Lantern', 'pickaxe')
    br = tx(P.brass(), 'hurricane_lantern_brass')
    for lit in (False, True):
        g = tx(P.lantern_glass(lit), f'hurricane_lantern_glass{"_lit" if lit else ""}')
        for hanging in (False, True):
            dy = 3 if hanging else 0
            m = Model(M, {'b': br, 'g': g}, ao=False, particle=br)
            m.box((4.5, 0 + dy, 4.5), (11.5, 1.5 + dy, 11.5), 'b')
            m.box((5.5, 1.5 + dy, 5.5), (10.5, 9.5 + dy, 10.5), 'g', light=(13 if lit else None))
            for x, z in ((5, 5), (10, 5), (5, 10), (10, 10)):
                m.box((x, 1.5 + dy, z), (x + 1, 9.5 + dy, z + 1), 'b', collide=False)
            m.box((5, 9.5 + dy, 5), (11, 11 + dy, 11), 'b')
            m.box((7, 11 + dy, 7), (9, 12 + dy, 9), 'b', collide=False)
            if hanging:
                m.box((7.5, 15, 7.5), (8.5, 16, 8.5), 'b', collide=False)
                m.box((7.5, 12 + dy, 7.5), (8.5, 15, 8.5), 'b', collide=False)
            else:
                m.box((6.5, 12, 7.5), (9.5, 14, 8.5), 'b', faces={'north': None, 'south': None}, collide=False)
            name = 'hurricane_lantern' + ('_hanging' if hanging else '') + ('_lit' if lit else '')
            m.write(name)
            if not lit:
                cm.shape(M, 'hurricane_lantern' + ('_hanging' if hanging else ''),
                         [(4.5, dy, 4.5, 11.5, 12 + dy, 11.5)] + ([(7.5, 12 + dy, 7.5, 8.5, 16, 8.5)] if hanging else []))
    v = {}
    for h in ('false', 'true'):
        for lit in ('false', 'true'):
            v[f'hanging={h},lit={lit}'] = {'model': rid('block/hurricane_lantern' + ('_hanging' if h == 'true' else '')
                                                        + ('_lit' if lit == 'true' else ''))}
    A.blockstate(M, 'hurricane_lantern', v)
    cm.block_item(M, 'hurricane_lantern', 'block/hurricane_lantern_lit', scale=0.85, lift=1)


def shell_candle():
    blk('shell_candle', 'Shell Candle')
    sh = tx(art.wood(art.SHELL, seed=3), 'shell_candle_shell')
    wx = tx(P.wax(), 'shell_candle_wax')
    fl = tx(P.flame(), 'shell_candle_flame')
    for lit in (False, True):
        m = Model(M, {'s': sh, 'w': wx, 'f': fl}, ao=False, particle=sh)
        m.box((3, 0, 3), (13, 2, 13), 's')
        m.box((2, 1, 4), (3, 3, 12), 's', collide=False)
        m.box((13, 1, 4), (14, 3, 12), 's', collide=False)
        m.box((4, 2, 12.5), (12, 3.5, 13.5), 's', collide=False)
        m.box((6.5, 2, 6.5), (9.5, 7, 9.5), 'w')
        m.box((7.75, 7, 7.75), (8.25, 8, 8.25), 's', collide=False)
        if lit:
            m.cross(8, 8, 1.5, 7.5, 10.5, 'f', light=15)
        m.write('shell_candle' + ('_lit' if lit else ''))
    cm.shape(M, 'shell_candle', [(2, 0, 3, 14, 3.5, 13.5), (6.5, 3.5, 6.5, 9.5, 8, 9.5)])
    A.blockstate(M, 'shell_candle', {'lit=false': {'model': rid('block/shell_candle')},
                                     'lit=true': {'model': rid('block/shell_candle_lit')}})
    cm.block_item(M, 'shell_candle', 'block/shell_candle_lit', scale=1.0, lift=2)


def sea_glass_lamp():
    blk('sea_glass_lamp', 'Sea Glass Lamp', 'pickaxe')
    for lit in (False, True):
        shade = tx(P.sea_glass_shade(lit), 'sea_glass_lamp_shade' + ('_lit' if lit else ''))
        m = Model(M, {'base': DRIFT_LOG, 'shade': shade}, ao=False, particle=DRIFT_LOG)
        m.box((5, 0, 5), (11, 1.5, 11), 'base')
        m.box((7, 1.5, 7), (9, 7, 9), 'base')
        m.box((3.5, 7, 3.5), (12.5, 14, 12.5), 'shade', light=(14 if lit else None))
        m.box((5, 14, 5), (11, 15, 11), 'shade', light=(14 if lit else None))
        m.write('sea_glass_lamp' + ('_lit' if lit else ''))
    cm.shape(M, 'sea_glass_lamp', [(5, 0, 5, 11, 1.5, 11), (7, 1.5, 7, 9, 7, 9), (3.5, 7, 3.5, 12.5, 15, 12.5)])
    A.blockstate(M, 'sea_glass_lamp', {'lit=false': {'model': rid('block/sea_glass_lamp')},
                                       'lit=true': {'model': rid('block/sea_glass_lamp_lit')}})
    cm.block_item(M, 'sea_glass_lamp', 'block/sea_glass_lamp_lit', scale=0.8)


def driftwood_sconce():
    blk('driftwood_sconce', 'Driftwood Sconce', 'axe')
    fl = 'raft:block/shell_candle_flame'
    for lit in (False, True):
        m = Model(M, {'w': DRIFT_LOG, 'x': 'raft:block/shell_candle_wax', 'f': fl, 'i': 'raft:block/hurricane_lantern_brass'},
                  ao=False, particle=DRIFT_LOG)
        m.box((5, 2, 14), (11, 12, 16), 'w')
        m.box((7, 5, 9), (9, 6.5, 14), 'i', collide=False)
        m.box((6, 6.5, 8), (10, 7.5, 12), 'i')
        m.box((7, 7.5, 9), (9, 11.5, 11), 'x')
        if lit:
            m.cross(8, 10, 1.25, 11.5, 14, 'f', light=15)
        m.write('driftwood_sconce' + ('_lit' if lit else ''))
    cm.shape(M, 'driftwood_sconce', [(5, 2, 14, 11, 12, 16), (6, 5, 8, 10, 11.5, 14)])
    v = {}
    for lit in ('false', 'true'):
        v.update(facing_variants('block/driftwood_sconce' + ('_lit' if lit == 'true' else ''), f'lit={lit}'))
    A.blockstate(M, 'driftwood_sconce', v)
    sp = P.img()
    d = art.DRIFT
    for y in range(4, 15):
        for x in range(6, 10):
            sp.putpixel((x, y), d[3 if x < 8 else 2])
    for y in range(2, 6):
        for x in range(7, 9):
            sp.putpixel((x, y), P.CANDLE[2])
    for x, y, k in ((7, 0, 2), (8, 0, 1), (7, 1, 3), (8, 1, 2)):
        sp.putpixel((x, y), P.FLAME[k])
    P.save(P.outline(sp), 'item/driftwood_sconce')
    A.flat_item(M, 'driftwood_sconce')


# ================================================================================================= plants & sea things
def plants():
    pot = tx(P.terracotta(), 'terracotta_pot')
    soil = tx(P.soil(), 'pot_soil')
    leaves = tx(P.monstera(), 'monstera_leaves')
    blk('potted_monstera', 'Potted Monstera')
    m = Model(M, {'p': pot, 's': soil, 'l': leaves}, ao=True, particle=pot)
    m.box((4, 0, 4), (12, 6, 12), 'p', faces={'up': None})
    m.box((4.5, 0, 4.5), (11.5, 5.5, 11.5), 's', only=('up',), collide=False, solid=False)
    m.box((3.5, 5, 3.5), (12.5, 7, 12.5), 'p', faces={'up': ('p', [3.5, 3.5, 12.5, 12.5])})
    m.cross(8, 8, 7.5, 6, 16, 'l')
    m.write('potted_monstera')
    cm.shape(M, 'potted_monstera', [(3.5, 0, 3.5, 12.5, 7, 12.5), (2, 7, 2, 14, 16, 14)])
    stateless('potted_monstera', 'block/potted_monstera')
    cm.block_item(M, 'potted_monstera', 'block/potted_monstera', scale=0.8)

    fl = tx(P.flowers(), 'tropical_flowers')
    blk('coconut_planter', 'Coconut Planter')
    shell = 'raft:block/coconut_planter_shell'
    P.save(art.wood(art.pal('#3b2614', '#4f331c', '#654426', '#7a5531', '#8f673d'), seed=8), 'block/coconut_planter_shell')
    m = Model(M, {'c': shell, 's': soil, 'f': fl}, ao=True, particle=shell)
    m.box((4, 0, 4), (12, 4, 12), 'c', faces={'up': None})
    m.box((4.5, 0, 4.5), (11.5, 3.5, 11.5), 's', only=('up',), collide=False, solid=False)
    m.cross(8, 8, 5, 3.5, 13.5, 'f')
    m.write('coconut_planter')
    cm.shape(M, 'coconut_planter', [(4, 0, 4, 12, 4, 12), (4, 4, 4, 12, 12, 12)])
    stateless('coconut_planter', 'block/coconut_planter')
    cm.block_item(M, 'coconut_planter', 'block/coconut_planter', scale=0.9, lift=1)

    blk('flower_box', 'Driftwood Flower Box', 'axe')
    m = Model(M, {'w': DRIFT, 's': soil, 'f': fl}, ao=True, particle=DRIFT)
    m.box((0, 0, 3), (16, 6, 13), 'w', faces={'up': None})
    m.box((0.5, 0, 3.5), (15.5, 5, 12.5), 's', only=('up',), collide=False, solid=False)
    m.box((0, 6, 3), (16, 7, 4), 'w', collide=False)
    m.box((0, 6, 12), (16, 7, 13), 'w', collide=False)
    m.cross(4, 8, 4, 5, 13, 'f')
    m.cross(12, 8, 4, 5, 13, 'f')
    m.write('flower_box')
    cm.shape(M, 'flower_box', [(0, 0, 3, 16, 7, 13)])
    A.blockstate(M, 'flower_box', facing_variants('block/flower_box'))
    cm.block_item(M, 'flower_box', 'block/flower_box', scale=0.6)

    blk('coral_vase', 'Coral Vase')
    vase = tx(art.metal(art.SEA_GLASS, seed=4, scratches=1), 'coral_vase_glass')
    cor = tx(P.coral_branches(), 'coral_vase_coral')
    m = Model(M, {'v': vase, 'c': cor}, ao=False, particle=vase)
    m.box((5, 0, 5), (11, 6, 11), 'v', faces={'up': None})
    m.box((6, 6, 6), (10, 8, 10), 'v', faces={'up': None})
    m.box((6.5, 0, 6.5), (9.5, 7.5, 9.5), 'v', only=('up',), collide=False, solid=False,
          faces={'up': ('v', [0, 0, 3, 3])})
    m.cross(8, 8, 4.5, 6, 16, 'c')
    m.write('coral_vase')
    cm.shape(M, 'coral_vase', [(5, 0, 5, 11, 8, 11), (5, 8, 5, 11, 16, 11)])
    stateless('coral_vase', 'block/coral_vase')
    cm.block_item(M, 'coral_vase', 'block/coral_vase', scale=0.8)


def shells_and_music():
    # conch shell: a spiral cone lying on the floor, the pink lip to the front
    blk('conch_shell', 'Conch Shell')
    c = tx(P.conch(), 'conch_shell')
    lip = tx(P.conch_lip(), 'conch_shell_lip')
    m = Model(M, {'c': c, 'l': lip}, ao=True, particle=c)
    m.box((5, 0, 4), (11, 5, 11), 'c')
    m.box((6, 0.5, 11), (10, 4, 13), 'c')
    m.box((7, 1, 13), (9, 3, 14.5), 'c')
    m.box((5.5, 0.5, 2.5), (10.5, 4.5, 4), 'l')
    m.box((6, 5, 5), (10, 6.5, 10), 'c')
    m.box((7, 6.5, 6), (9, 7.5, 9), 'c', collide=False)
    m.write('conch_shell')
    cm.shape(M, 'conch_shell', [(5, 0, 2.5, 11, 7.5, 14.5)])
    A.blockstate(M, 'conch_shell', facing_variants('block/conch_shell'))
    cm.block_item(M, 'conch_shell', 'block/conch_shell', scale=1.0, lift=2)

    blk('steel_drum', 'Steel Drum', 'pickaxe')
    top = tx(P.drum_top(), 'steel_drum_top')
    side = tx(P.drum_side(), 'steel_drum_side')
    m = Model(M, {'t': top, 's': side, 'l': 'raft:block/hurricane_lantern_brass'}, ao=True, particle=side)
    m.box((1, 9, 1), (15, 14, 15), 's', faces={'up': 't'})
    for x, z in ((2, 2), (13, 2), (2, 13), (13, 13)):
        m.box((x, 0, z), (x + 1, 9, z + 1), 'l')
    m.box((3, 4, 2), (13, 5, 3), 'l', collide=False)
    m.box((3, 4, 13), (13, 5, 14), 'l', collide=False)
    m.write('steel_drum')
    cm.shape(M, 'steel_drum', [(1, 9, 1, 15, 14, 15), (2, 0, 2, 14, 9, 14)])
    stateless('steel_drum', 'block/steel_drum')
    cm.block_item(M, 'steel_drum', 'block/steel_drum', scale=0.65)

    blk('shell_garland', 'Shell Garland')
    g = tx(P.shell_garland(), 'shell_garland')
    m = Model(M, {'g': g}, ao=False, particle=g)
    m.plane_z(0, 4, 16, 16, 15.5, 'g')
    m.write('shell_garland')
    cm.shape(M, 'shell_garland', [(0, 4, 15, 16, 16, 16)])
    A.blockstate(M, 'shell_garland', facing_variants('block/shell_garland'))
    A.flat_item(M, 'shell_garland', 'block/shell_garland')

    blk('fishing_net', 'Fishing Net', 'axe')
    n = tx(P.net(), 'fishing_net')
    m = Model(M, {'n': n}, ao=False, particle=n)
    m.plane_z(0, 0, 16, 16, 15, 'n')
    m.write('fishing_net')
    cm.shape(M, 'fishing_net', [(0, 0, 14.5, 16, 16, 16)])
    A.blockstate(M, 'fishing_net', facing_variants('block/fishing_net'))
    A.flat_item(M, 'fishing_net', 'block/fishing_net')

    blk('dock_bollard', 'Dock Bollard', 'pickaxe')
    ir = tx(P.iron_dark(), 'dock_bollard')
    m = Model(M, {'i': ir, 'r': tx(art.rope(art.ROPE), 'rope_band')}, ao=True, particle=ir)
    m.box((4, 0, 4), (12, 1, 12), 'i')
    m.box((5, 1, 5), (11, 10, 11), 'i')
    m.box((4, 10, 4), (12, 12, 12), 'i')
    m.box((4.5, 5, 4.5), (11.5, 7, 11.5), 'r', collide=False, solid=False,
          only=('north', 'south', 'east', 'west'))
    m.write('dock_bollard')
    cm.shape(M, 'dock_bollard', [(4, 0, 4, 12, 1, 12), (5, 1, 5, 11, 10, 11), (4, 10, 4, 12, 12, 12)])
    stateless('dock_bollard', 'block/dock_bollard')
    cm.block_item(M, 'dock_bollard', 'block/dock_bollard', scale=0.8)


# ================================================================================================= wall decor
def wall_decor():
    for colour, c in (('red', '#d4473c'), ('blue', '#3c86d0'), ('yellow', '#f0c43a')):
        bid = f'surfboard_{colour}'
        blk(bid, f'{colour.capitalize()} Surfboard', None, 'lower')
        t = tx(P.surfboard(c), bid)
        for half in ('lower', 'upper'):
            m = Model(M, {'t': t}, ao=True, particle=t)
            rot = {'origin': [8, 0 if half == 'lower' else -16, 15.5], 'axis': 'x', 'angle': -11.25}
            if half == 'lower':
                m.box((4, 1, 13), (12, 16, 14.5), 't', rot=rot, collide=False)
                m.box((5, 0, 13.25), (11, 1, 14.75), 't', rot=rot, collide=False)
            else:
                m.box((4, 0, 13), (12, 13, 14.5), 't', rot=rot, collide=False)
                m.box((5, 13, 13.25), (11, 15, 14.75), 't', rot=rot, collide=False)
                m.box((6, 15, 13.5), (10, 16, 14.5), 't', rot=rot, collide=False)
            m.write(f'{bid}_{half}')
        v = {}
        for half in ('lower', 'upper'):
            v.update(facing_variants(f'block/{bid}_{half}', f'half={half}'))
        A.blockstate(M, bid, v)
        sp = P.img()
        r = tex.ramp(c, 5, 0.4)
        for y in range(1, 15):
            w = 2 if y in (1, 14) else 3
            for x in range(8 - w, 8 + w):
                sp.putpixel((x, y), r[3 if x < 8 else 2])
            sp.putpixel((8, y), tex.rgb('#f6f3ea'))
        P.save(P.outline(sp), f'item/{bid}')
        A.flat_item(M, bid)
    cm.shape(M, 'surfboard_lower', [(4, 0, 11, 12, 16, 15)])
    cm.shape(M, 'surfboard_upper', [(4, 0, 9, 12, 15, 13)])

    blk('fish_trophy', 'Fish Trophy', 'axe')
    fish = tx(P.fish_side(), 'fish_trophy_fish')
    m = Model(M, {'p': TEAK, 'f': fish, 'b': 'raft:block/hurricane_lantern_brass'}, ao=True, particle=TEAK)
    m.box((2, 4, 15), (14, 12, 16), 'p')
    m.box((3, 6.5, 13), (12, 10, 15), 'f')
    m.box((12, 7, 13.25), (13, 9.5, 14.75), 'f', collide=False)
    m.box((13, 5.5, 13.5), (14.5, 11, 14.5), 'f', collide=False)
    m.box((6, 10, 13.5), (9, 11.5, 14.5), 'f', collide=False)
    m.box((7, 4.5, 14.5), (9, 5.5, 15), 'b', collide=False)
    m.write('fish_trophy')
    cm.shape(M, 'fish_trophy', [(2, 4, 13, 14, 12, 16)])
    A.blockstate(M, 'fish_trophy', facing_variants('block/fish_trophy'))
    sp = P.img()
    f = P.FISH
    for y in range(6, 11):
        for x in range(2, 12):
            if abs(y - 8) <= 2 - (1 if x < 4 or x > 9 else 0):
                sp.putpixel((x, y), f[4 if y < 8 else 2])
    for y in range(5, 12):
        if abs(y - 8) <= 3:
            sp.putpixel((13, y), f[3])
            sp.putpixel((12, y), f[2]) if abs(y - 8) <= 1 else None
    sp.putpixel((3, 7), tex.rgb('#1c1c22'))
    P.save(P.outline(sp), 'item/fish_trophy')
    A.flat_item(M, 'fish_trophy')

    blk('bamboo_blinds', 'Bamboo Blinds', 'axe')
    sl = tx(P.bamboo_slats(), 'bamboo_blinds')
    end = tx(P.bamboo_roll_end(), 'bamboo_blinds_roll')
    rope = tx(art.rope(art.ROPE, horizontal=False), 'rope')
    m = Model(M, {'s': sl, 'r': rope}, ao=False, particle=sl)
    m.box((0, 0, 15), (16, 16, 16), 's')
    m.box((3, 0, 14.5), (4, 16, 15), 'r', collide=False, solid=False)
    m.box((12, 0, 14.5), (13, 16, 15), 'r', collide=False, solid=False)
    m.write('bamboo_blinds')
    m = Model(M, {'s': sl, 'e': end, 'r': rope}, ao=False, particle=sl)
    m.box((0, 13, 13.5), (16, 16, 16), 's', faces={'east': 'e', 'west': 'e'})
    m.box((3, 11, 13), (4, 13, 14), 'r', collide=False, solid=False)
    m.box((12, 11, 13), (13, 13, 14), 'r', collide=False, solid=False)
    m.write('bamboo_blinds_open')
    cm.shape(M, 'bamboo_blinds', [(0, 0, 15, 16, 16, 16)])
    cm.shape(M, 'bamboo_blinds_open', [(0, 13, 13.5, 16, 16, 16)])
    v = {}
    for o in ('false', 'true'):
        v.update(facing_variants('block/bamboo_blinds' + ('_open' if o == 'true' else ''), f'open={o}'))
    A.blockstate(M, 'bamboo_blinds', v)
    A.flat_item(M, 'bamboo_blinds', 'block/bamboo_blinds')


def parasol():
    blk('thatch_parasol', 'Thatch Parasol', 'axe', 'lower')
    fr = tx(P.palm_fronds(), 'thatch_parasol')
    fringe = tx(P.palm_fringe(), 'thatch_parasol_fringe')
    m = Model(M, {'p': BAMBOO_BLOCK, 'f': fr}, ao=True, particle=fr)
    m.box((5, 0, 5), (11, 1.5, 11), 'p')
    m.box((7, 1.5, 7), (9, 16, 9), 'p')
    m.write('thatch_parasol_lower')
    m = Model(M, {'p': BAMBOO_BLOCK, 'f': fr, 'e': fringe}, ao=True, particle=fr)
    m.box((7, 0, 7), (9, 10, 9), 'p')
    m.box((-6, 8, -6), (22, 10, 22), 'f', collide=False)
    m.box((-2, 10, -2), (18, 12, 18), 'f', collide=False)
    m.box((3, 12, 3), (13, 14, 13), 'f', collide=False)
    m.box((6, 14, 6), (10, 15.5, 10), 'f', collide=False)
    # leaf tips hanging over the rim
    m.plane_z(-6, 5, 22, 8, -6, 'e', uv=[0, 0, 16, 3])
    m.plane_z(-6, 5, 22, 8, 22, 'e', uv=[0, 0, 16, 3])
    m.plane_x(-6, 5, 22, 8, -6, 'e', uv=[0, 0, 16, 3])
    m.plane_x(-6, 5, 22, 8, 22, 'e', uv=[0, 0, 16, 3])
    m.write('thatch_parasol_upper')
    cm.shape(M, 'thatch_parasol_lower', [(5, 0, 5, 11, 1.5, 11), (7, 1.5, 7, 9, 16, 9)])
    cm.shape(M, 'thatch_parasol_upper', [(7, 0, 7, 9, 15.5, 9), (0, 8, 0, 16, 12, 16)])
    A.blockstate(M, 'thatch_parasol', {'half=lower': {'model': rid('block/thatch_parasol_lower')},
                                       'half=upper': {'model': rid('block/thatch_parasol_upper')}})
    sp = P.img()
    g = P.pal('#5a5a24', '#787630', '#96933e', '#b2ac52', '#cbc46c')
    for y in range(2, 7):
        w = 2 + (y - 2) * 1.6
        for x in range(int(8 - w), int(8 + w)):
            sp.putpixel((x, y), g[3 if x < 8 else 2] if y < 6 else g[1])
    for y in range(7, 15):
        sp.putpixel((7, y), art.BAMBOO[3])
        sp.putpixel((8, y), art.BAMBOO[2])
    P.save(P.outline(sp), 'item/thatch_parasol')
    A.flat_item(M, 'thatch_parasol')


# ================================================================================================= building blocks
def building():
    st = tx(P.shell_tiles(), 'shell_tiles')
    pb = tx(P.pumice_bricks(), 'pumice_bricks')
    for base, t, label in (('shell_tiles', st, 'Shell Tile'), ('pumice_bricks', pb, 'Pumice Brick')):
        full = 'Shell Tiles' if base == 'shell_tiles' else 'Pumice Bricks'
        stem = 'shell_tile' if base == 'shell_tiles' else 'pumice_brick'
        blk(base, full, 'pickaxe')
        A.simple_block(M, base, t)
        blk(f'{stem}_stairs', f'{label} Stairs', 'pickaxe')
        A.stairs(M, f'{stem}_stairs', t)
        blk(f'{stem}_slab', f'{label} Slab', 'pickaxe', 'slab')
        A.slab(M, f'{stem}_slab', t, f'block/{base}')
        blk(f'{stem}_wall', f'{label} Wall', 'pickaxe')
        A.wall(M, f'{stem}_wall', t)
    lt = tx(P.lattice(), 'driftwood_lattice')
    le = tx(P.lattice_edge(), 'driftwood_lattice_edge')
    blk('driftwood_lattice', 'Driftwood Lattice', 'axe')
    A.pane(M, 'driftwood_lattice', lt, le)


# ================================================================================================= effect icon
def rested_icon():
    """18x18 icon of the Rested effect: a warm crescent moon with a small star."""
    from PIL import Image
    im = Image.new('RGBA', (18, 18), (0, 0, 0, 0))
    moon = tex.ramp('#f2c46d', 5, 0.5)
    for y in range(18):
        for x in range(18):
            d1 = ((x - 8.5) ** 2 + (y - 9.0) ** 2) ** 0.5
            d2 = ((x - 11.5) ** 2 + (y - 6.5) ** 2) ** 0.5
            if d1 <= 6.6 and d2 > 5.2:
                k = 4 if x + y < 14 else 3 if d1 < 5.5 else 2
                im.putpixel((x, y), moon[k])
    for x, y in ((14, 11), (13, 12), (15, 12), (14, 13), (14, 12)):
        im.putpixel((x, y), tex.rgb('#fff3bd'))
    P.save(tex.outline(im), 'mob_effect/rested')


# ================================================================================================= data
def loot(bid, kind):
    surv = {'condition': 'minecraft:survives_explosion'}
    item = {'type': 'minecraft:item', 'name': rid(bid)}
    if kind == 'self':
        A.loot_block(M, bid, [item], conditions=[surv])
    elif kind == 'slab':
        A.loot_slab(M, bid)
    elif kind == 'lower':
        A.loot_block(M, bid, [dict(item, conditions=[{'condition': 'minecraft:block_state_property', 'block': rid(bid),
                                                      'properties': {'half': 'lower'}}])], conditions=[surv])
    elif kind == 'basket':
        # keeps its contents and name, like a shulker box
        A.loot_block(M, bid, [dict(item, functions=[{'function': 'minecraft:copy_components', 'source': 'block_entity',
                                                     'include': ['minecraft:custom_name', 'minecraft:container']}])])
    else:
        raise ValueError(kind)


# Comfort categories: blocks in these tags count once each when a player rests nearby (CozyComfort.java).
COMFORT = {
    'seating': ['#raft:cozy_seats', 'raft:deck_chair', 'raft:driftwood_chair', 'raft:palm_chair', 'raft:cushion_white',
                'raft:cushion_red', 'raft:cushion_blue', 'raft:cushion_yellow', 'raft:cushion_green', 'raft:hammock'],
    'table': ['raft:teak_table', 'raft:bamboo_table', 'raft:driftwood_table', 'raft:palm_table', 'raft:chart_table',
              'raft:serving_plate'],
    'light': ['raft:hurricane_lantern', 'raft:shell_candle', 'raft:sea_glass_lamp', 'raft:driftwood_sconce',
              '#raft:decor_lights', 'minecraft:lantern', 'minecraft:soul_lantern', '#minecraft:candles',
              'minecraft:sea_lantern', 'raft:coconut_lantern'],
    'fire': ['minecraft:campfire', 'minecraft:soul_campfire', 'raft:brazier', 'raft:tiki_torch', 'raft:simple_grill',
             'raft:cooking_pot'],
    'rug': ['raft:beach_towel_red', 'raft:beach_towel_blue', 'raft:beach_towel_yellow', 'raft:rope_rug',
            'raft:woven_rug', '#minecraft:wool_carpets'],
    'plant': ['raft:potted_monstera', 'raft:coconut_planter', 'raft:flower_box', 'raft:coral_vase',
              'raft:hanging_planter', '#minecraft:flower_pots'],
    'shade': ['raft:thatch_parasol', 'raft:beach_umbrella_red', 'raft:beach_umbrella_blue', 'raft:beach_umbrella_yellow'],
    'music': ['raft:steel_drum', 'raft:conch_shell', 'raft:wind_chime', 'raft:ship_bell', 'minecraft:jukebox',
              'minecraft:note_block'],
    'storage': ['raft:palm_shelf', 'raft:teak_shelf', 'raft:weathered_shelf', 'raft:wicker_basket', 'raft:wooden_crate',
                'raft:treasure_chest', 'raft:driftwood_shelf', 'minecraft:chest', 'minecraft:barrel'],
    'wall_decor': ['raft:shell_garland', 'raft:fishing_net', 'raft:fish_trophy', 'raft:surfboard_red',
                   'raft:surfboard_blue', 'raft:surfboard_yellow', 'raft:life_ring', 'raft:figurehead',
                   'raft:jolly_roger', 'raft:bunting_red_white', 'raft:bunting_rainbow', 'raft:string_lights_warm',
                   'raft:string_lights_colorful', 'raft:shark_jaw_trophy', 'minecraft:decorated_pot'],
    'water_view': ['raft:aquarium', 'raft:bamboo_blinds', 'raft:driftwood_lattice', '#raft:sea_glass_panes'],
}


def tags():
    for b, (_, tool, _) in BLOCKS.items():
        if tool:
            A.tag(M, 'block', f'minecraft:mineable/{tool}', [b])
    seats = ['palm_bench', 'teak_bench', 'bar_stool', 'sun_lounger', 'rattan_armchair']
    A.tag(M, 'block', 'raft:cozy_seats', seats)
    A.tag(M, 'block', 'raft:decor_lights', [{'id': f'raft:{x}', 'required': False} for x in (
        'paper_lantern_white', 'paper_lantern_red', 'paper_lantern_orange', 'paper_lantern_yellow', 'paper_lantern_lime',
        'paper_lantern_cyan', 'paper_lantern_blue', 'paper_lantern_purple', 'paper_lantern_pink', 'ship_lantern',
        'jelly_lamp', 'driftwood_lamp', 'seashell_lamp', 'neon_tube_cyan', 'neon_tube_pink', 'lighthouse_lens')])
    A.tag(M, 'block', 'raft:sea_glass_panes', [{'id': f'raft:{x}', 'required': False} for x in (
        'aqua_sea_glass_pane', 'green_sea_glass_pane', 'blue_sea_glass_pane')])
    own = set(BLOCKS)
    for cat, values in COMFORT.items():
        vals = []
        for v in values:
            if v.startswith('#') or not v.startswith('raft:') or v[5:] in own:
                vals.append(v)
            else:
                vals.append({'id': v, 'required': False})   # blocks of other modules: optional
        A.tag(M, 'block', f'raft:comfort/{cat}', vals)
    for b in ('beach_towel_red', 'beach_towel_blue', 'beach_towel_yellow', 'rope_rug'):
        A.tag(M, 'block', 'minecraft:dampens_vibrations', [b])
        A.tag(M, 'block', 'minecraft:combination_step_sound_blocks', [b])
    A.tag(M, 'block', 'minecraft:stairs', ['shell_tile_stairs', 'pumice_brick_stairs'])
    A.tag(M, 'item', 'minecraft:stairs', ['shell_tile_stairs', 'pumice_brick_stairs'])
    A.tag(M, 'block', 'minecraft:slabs', ['shell_tile_slab', 'pumice_brick_slab'])
    A.tag(M, 'item', 'minecraft:slabs', ['shell_tile_slab', 'pumice_brick_slab'])
    A.tag(M, 'block', 'minecraft:walls', ['shell_tile_wall', 'pumice_brick_wall'])
    A.tag(M, 'item', 'minecraft:walls', ['shell_tile_wall', 'pumice_brick_wall'])
    A.tag(M, 'item', 'raft:cozy_furniture', seats + ['teak_table', 'bamboo_table'])


def main():
    out = module_dir(M)
    if os.path.isdir(out):
        shutil.rmtree(out)
    os.makedirs(out)
    benches()
    bar_stool()
    sun_lounger()
    rattan_armchair()
    tables()
    serving_plate()
    shelves()
    wicker_basket()
    rugs()
    hurricane_lantern()
    shell_candle()
    sea_glass_lamp()
    driftwood_sconce()
    plants()
    shells_and_music()
    wall_decor()
    parasol()
    building()

    for b, (label, tool, kind) in BLOCKS.items():
        loot(b, kind)
        LANG[f'block.raft.{b}'] = label
    tags()
    rested_icon()
    LANG.update(PAGES['cozy'])
    A.sounds(M, sound.build(M))
    LANG.update({'subtitles.raft.block.steel_drum.note': 'Steel drum plays', 'subtitles.raft.block.conch_shell.blow': 'Conch horn blows', 'subtitles.raft.cozy.rested': 'You feel rested'})
    R.cozy_recipes(M, BLOCKS)
    A.lang(M, LANG)
    A.flush(M)
    cm.write_java_shapes(M, 'net.kasax.raft.content.cozy.block', 'CozyShapeData',
                         os.path.join(ROOT, 'src', 'main', 'java', 'net', 'kasax', 'raft', 'content', 'cozy', 'block',
                                      'CozyShapeData.java'))
    from content.cozy import loot26
    loot26.modernize(module_dir(M))
    lint = cm.report_lint(M)
    n = sum(len(fs) for _, _, fs in os.walk(out))
    print(f'cozy: {len(BLOCKS)} blocks, {n} files, {lint} lint warnings')


if __name__ == '__main__':
    main()
