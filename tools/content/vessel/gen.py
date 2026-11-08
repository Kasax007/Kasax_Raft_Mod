"""Generates every resource of the vessel module (built rafts) into src/main/modules/vessel/.

Run: cd tools && python3 -m content.vessel.gen   (or python3 tools/content/vessel/gen.py)
Previews go to $BC_PREVIEW (default: the module's work folder in the scratchpad), not into the mod.

Blocks (models face north = the block's `facing`; blockstates turn them):
  helm        ship's wheel on a pedestal, stool aft of it (the helmsman sits there facing the bow)
  sail        canvas panel; multipart adds hems, the yard, the reach to a mast, the furled roll
  mast        pole for the sails (cap and lashing on top)
  oar         rowlock at the outboard edge, the loom slanting down over the side into the water
  raft_motor  outboard motor: clamp on the deck edge, cowling, tiller; shaft and propeller hang over the side
"""
import os
import sys
from collections import OrderedDict

if __package__ in (None, ''):
    sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..')))
    __package__ = 'content.vessel'

from bclib import assets as A, extrude, tex  # noqa: E402
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
                # plain bands (no seams): the shaft and clamp squeeze this strip onto narrow faces
                c = steel[3] if y == 8 else steel[2] if y < 11 else steel[1]
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
        '................',
        '.24444444444443.',
        '.13333333333332.',
        '..wwwvwwwvwwvu..',
        '..wwwvwwwvwwvu..',
        '..wwwvwwwvwwvu..',
        '..wwwvwwwvwvuu..',
        '..vwwvwwwvwvuu..',
        '..vwwvwwvvwvuu..',
        '..vvwvwwvvvvuu..',
        '..vvvvvvvuvuuu..',
        '..uvvvvvuuuuuu..',
        '..yyyyyyyyyyYY..',
        '................',
        '................',
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
    # the wheel is the ship_wheel sprite extruded pixel row by pixel row (its sides follow the ring and spokes),
    # 16 px across from y 4 to 20, seen unmirrored by the helmsman on the stool (south)
    wheel = extrude.vertical('raft:block/ship_wheel', '#wheel', at=(0, 20), z=(7.5, 8.5), front='south')
    return [
        el((5, 0, 3), (11, 2, 8), 'wood'),
        el((6.5, 2, 4), (9.5, 12, 7), 'post'),
        el((7, 11, 6), (9, 13, 7.5), 'post'),
        *wheel,
        el((4, 6, 10), (12, 7.5, 15.5), 'wood'),
        el((4.5, 0, 10.5), (6, 6, 12), 'post'),
        el((10, 0, 10.5), (11.5, 6, 12), 'post'),
        el((4.5, 0, 13.5), (6, 6, 15), 'post'),
        el((10, 0, 13.5), (11.5, 6, 15), 'post'),
    ]


# Sails are panels of canvas in the middle of their block, facing = the bow; side by side and on top of each other they
# make one sheet (no seams: the canvas texture tiles). The edges of the sheet get a hem: the yard along the top, a
# rope along the bottom and the free sides, and next to a mast the canvas reaches over to the mast pole.
# Furled (raised), a sail's canvas is rolled up under the yard; the rows below only keep their brail lines.
CANVAS_Z = (7.75, 8.25)


def sail_cloth_texture():
    """Canvas that tiles without seams at the block edges: cloth panels 8 px wide with a sewn seam, a faint weave."""
    c = tex.ramp('#e6dcc2', 4, spread=0.22)
    img = tex.new()
    for y in range(16):
        for x in range(16):
            k = 2
            if x % 8 == 0:
                k = 1                     # the seam between two cloths
            elif x % 8 == 1:
                k = 3                     # its lit edge
            elif (x + y) % 4 == 0 and y % 2 == 0:
                k = 1 if x % 8 > 4 else 2   # a few weave dots, regular so they tile
            tex.px(img, x, y, c[k])
    return img


def sail_canvas():
    z0, z1 = CANVAS_Z
    return [el((0, 0, z0), (16, 16, z1), 'canvas', uv={'north': [16, 0, 0, 16], 'south': [0, 0, 16, 16]},
               faces=('north', 'south'))]


def sail_hem(side):
    """Hem of a free edge: 'up' = the yard (a spar), 'down' and the sides = a rope sewn into the edge."""
    if side == 'up':
        return [el((0, 15, 7), (16, 16.5, 9), 'mast', uv={'north': [0, 6, 16, 7.5], 'south': [0, 8, 16, 9.5],
                                                          'up': [0, 2, 16, 4], 'down': [0, 11, 16, 13],
                                                          'east': [6, 0, 8, 1.5], 'west': [8, 0, 10, 1.5]})]
    if side == 'down':
        return [el((0, 0, 7.5), (16, 1, 8.5), 'rope', uv={'*': [0, 7, 16, 8]})]
    x0 = 0 if side == 'left' else 15          # left = west for facing north (seen from the stern, looking to the bow)
    return [el((x0, 0, 7.5), (x0 + 1, 16, 8.5), 'rope', uv={'*': [7, 0, 8, 16]})]


def sail_to_mast(side):
    """Canvas reaching over into the next block up to the mast pole there (pole at 7..9 of that block)."""
    z0, z1 = CANVAS_Z
    if side == 'left':
        return [el((-7, 0, z0), (0, 16, z1), 'canvas', uv={'north': [16, 0, 9, 16], 'south': [9, 0, 16, 16]},
                   faces=('north', 'south'))]
    return [el((16, 0, z0), (23, 16, z1), 'canvas', uv={'north': [7, 0, 0, 16], 'south': [0, 0, 7, 16]},
               faces=('north', 'south'))]


def sail_furled():
    """The canvas rolled up under the yard (top row of a furled sail)."""
    return [el((0, 12.5, 7.25), (16, 15, 8.75), 'canvas', uv={'north': [0, 3, 16, 5.5], 'south': [0, 6, 16, 8.5],
                                                              'up': [0, 9, 16, 10.5], 'down': [0, 11, 16, 12.5],
                                                              'east': [3, 0, 4.5, 2.5], 'west': [11, 0, 12.5, 2.5]})]


def sail_brails():
    """Rows below the top of a furled sail: only the brail lines that hold the rolled canvas."""
    return [el((3.75, 0, 7.75), (4.25, 16, 8.25), 'rope', uv={'*': [7, 0, 8, 16]}),
            el((11.75, 0, 7.75), (12.25, 16, 8.25), 'rope', uv={'*': [7, 0, 8, 16]})]


def mast(top):
    """A mast: a pole in the middle of the block; the top of a mast gets a cap and a lashing."""
    pole_uv = {'north': [7, 0, 9, 16], 'south': [7, 0, 9, 16], 'east': [7, 0, 9, 16], 'west': [7, 0, 9, 16],
               'up': [7, 7, 9, 9], 'down': [7, 7, 9, 9]}
    if not top:
        return [el((7, 0, 7), (9, 16, 9), 'mast', uv=pole_uv, faces=('north', 'east', 'south', 'west'))]
    return [el((7, 0, 7), (9, 14, 9), 'mast', uv=pole_uv, faces=('north', 'east', 'south', 'west', 'down')),
            el((6.5, 14, 6.5), (9.5, 16, 9.5), 'end', uv={'up': [6.5, 6.5, 9.5, 9.5], '*': [6.5, 0, 9.5, 2]}),
            el((6.75, 11, 6.75), (9.25, 12, 9.25), 'rope', uv={'*': [6, 6, 8.5, 7]}, faces=('north', 'east', 'south', 'west'))]


def anchor_winch(dropped):
    """Anchor winch (facing = outboard, north): a frame with a rope drum on the deck; raised, the anchor hangs at the
    side over the edge; dropped, only its rope runs down over the edge into the water."""
    els = [
        el((3, 0, 4), (13, 2, 12), 'wood'),
        el((3, 2, 6.5), (4.5, 10, 9.5), 'wood'),
        el((11.5, 2, 6.5), (13, 10, 9.5), 'wood'),
        el((4.5, 4.5, 6), (11.5, 9.5, 10), 'rope', uv={'*': [4.5, 4.5, 11.5, 9.5]}),
        el((2, 6.5, 7.5), (3, 7.5, 8.5), 'iron', uv={'*': [7, 7, 8, 8]}),
        el((0, 6, 7.5), (2, 10, 8.5), 'iron', uv={'*': [6, 6, 8, 10]}),
    ]
    if dropped:
        # the rope leaves the drum and runs over the edge down into the water
        els += [el((7.5, 4, 0), (8.5, 5, 6), 'rope', uv={'*': [0, 7, 6, 8]}),
                el((7.5, -16, -1), (8.5, 5, 0), 'rope', uv={'*': [7, 0, 8, 16]})]
    else:
        # the anchor hangs at the outboard side: ring, shank, stock and the two arms with their flukes
        els += [el((7.5, 4, 0), (8.5, 5, 6), 'rope', uv={'*': [0, 7, 6, 8]}),
                el((7.25, 1, -2), (8.75, 4, -0.5), 'iron', uv={'*': [7, 1, 8.5, 4]}),
                el((7.25, -9, -2), (8.75, 1, -0.5), 'iron', uv={'*': [7, 3, 8.5, 13]}),
                el((4.5, -1, -2), (11.5, 0, -0.5), 'iron', uv={'*': [4.5, 7, 11.5, 8]}),
                el((4, -10, -2), (12, -8.5, -0.5), 'iron', uv={'*': [4, 9, 12, 10.5]}),
                el((3, -9, -2), (4.5, -6, -0.5), 'iron', uv={'*': [3, 6, 4.5, 9]}),
                el((11.5, -9, -2), (13, -6, -0.5), 'iron', uv={'*': [11.5, 6, 13, 9]})]
    return els


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
        el((7.5, 7, -14), (8.5, 8, 12.5), 'loom', uv=loom_uv, rot=rot),        # ends inside the grip
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
    sail_tex = {'particle': 'block/sail_cloth', 'mast': 'block/palm_log', 'canvas': 'block/sail_cloth',
                'rope': 'block/rope_coil'}
    model('sail', sail_tex, sail_canvas())
    for side in ('up', 'down', 'left', 'right'):
        model('sail_hem_' + side, sail_tex, sail_hem(side))
    for side in ('left', 'right'):
        model('sail_mast_' + side, sail_tex, sail_to_mast(side))
    model('sail_furled', sail_tex, sail_furled())
    model('sail_brails', sail_tex, sail_brails())
    mast_tex = {'particle': 'block/palm_log', 'mast': 'block/palm_log', 'end': 'block/palm_log_top', 'rope': 'block/rope_coil'}
    model('mast', mast_tex, mast(False))
    model('mast_top', mast_tex, mast(True))
    # the helm in two parts for a sailing raft: the wheel turns while the helmsman steers (VesselRenderer)
    helm_tex = {'particle': 'block/palm_planks', 'wheel': 'block/ship_wheel', 'wood': 'block/palm_planks',
                'post': 'block/stripped_palm_log'}
    full = helm_elements()
    model('helm_base', helm_tex, [e for e in full if '#wheel' not in str(e)])
    model('helm_wheel', helm_tex, [e for e in full if '#wheel' in str(e)])
    anchor_tex = {'particle': 'block/decor_iron', 'wood': 'block/palm_planks', 'rope': 'block/rope_coil',
                  'iron': 'block/decor_iron'}
    model('anchor_winch', anchor_tex, anchor_winch(False))
    model('anchor_winch_dropped', anchor_tex, anchor_winch(True))
    model('oar', {'particle': 'block/palm_planks', 'lock': 'block/driftwood_log', 'loom': 'block/stripped_palm_log',
                  'blade': 'block/palm_planks', 'grip': 'block/rope_coil'}, oar_elements())
    model('raft_motor', {'particle': 'block/raft_motor', 'motor': 'block/raft_motor'}, motor_elements())


def blockstates():
    helm = OrderedDict()
    for part, mdl in (('full', 'helm'), ('base', 'helm_base'), ('wheel', 'helm_wheel')):
        for facing, rot in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
            v = OrderedDict(model=rid('block/' + mdl))
            if rot:
                v['y'] = rot
            helm[f'facing={facing},part={part}'] = v
    A.blockstate(M, 'helm', helm)
    A.blockstate(M, 'oar', A.horizontal_variants('block/oar'))
    A.blockstate(M, 'raft_motor', A.horizontal_variants('block/raft_motor'))
    anchor = A.horizontal_variants('block/anchor_winch', extra='dropped=false')
    anchor.update(A.horizontal_variants('block/anchor_winch_dropped', extra='dropped=true'))
    A.blockstate(M, 'anchor_winch', anchor)
    parts = []
    conds = [('sail', {'furled': 'false'}), ('sail_hem_up', {'up': 'false'}),
             ('sail_hem_down', {'down': 'false', 'furled': 'false'}),
             ('sail_hem_left', {'left': 'none', 'furled': 'false'}), ('sail_hem_right', {'right': 'none', 'furled': 'false'}),
             ('sail_mast_left', {'left': 'mast', 'furled': 'false'}), ('sail_mast_right', {'right': 'mast', 'furled': 'false'}),
             ('sail_furled', {'up': 'false', 'furled': 'true'}), ('sail_brails', {'up': 'true', 'furled': 'true'})]
    for part, cond in conds:
        for facing, rot in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
            apply = OrderedDict(model=rid('block/' + part))
            if rot:
                apply['y'] = rot
            when = OrderedDict(facing=facing)
            when.update(cond)
            parts.append(OrderedDict(when=when, apply=apply))
    A.blockstate(M, 'sail', multipart=parts)
    A.blockstate(M, 'mast', {'up=false': {'model': rid('block/mast_top')}, 'up=true': {'model': rid('block/mast')}})


def items():
    sprites = {}
    for name in ['helm'] + list(ITEM_ART):
        img = item_sprite(name)
        tex.save(img, M, 'item/' + name)
        A.flat_item(M, name)
        sprites[name] = img
    A.block_item(M, 'mast', 'block/mast_top')
    A.block_item(M, 'anchor_winch')
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
    for b in ('helm', 'sail', 'mast', 'oar', 'raft_motor', 'anchor_winch'):
        loot_self(b)
    A.tag(M, 'block', 'minecraft:mineable/axe', ['helm', 'sail', 'mast', 'oar'])
    A.tag(M, 'block', 'minecraft:mineable/pickaxe', ['raft_motor', 'anchor_winch'])
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
    A.recipe_shaped(M, 'anchor_winch', [' R ', 'PIP', ' I '],
                    {'R': 'raft:rope', 'P': '#minecraft:planks', 'I': 'minecraft:iron_ingot'}, 'anchor_winch', category='misc')
    recipe_unlock('anchor_winch', ['raft:rope', 'minecraft:iron_ingot'])
    A.recipe_shaped(M, 'mast', ['L', 'L', 'L'], {'L': '#minecraft:logs'}, 'mast', count=4, category='misc')
    recipe_unlock('mast', ['raft:sail_canvas'])
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
Floors, walls, a roof, chests, furnaces, crops, redstone: everything that stands on the foundations sails along, and it all keeps working at sea. Keep it inside the edge of the foundations.
# The Helm
Set a Helm on the raft so that it faces the bow, and use it to set sail. W and S sail, A and D steer: the wheel turns in your hands.
Sneak to leave the helm: the raft slows down and waits where it is. Use the helm again to steer on.
To moor, use the wheel while you steer, or sneak and use the helm. The raft turns to the nearest compass direction and becomes blocks again.
# At sea
Open chests and furnaces, craft, farm, flip levers, build and break: the raft's blocks work while it sails. Set Raft Foundations against its side to grow it at sea. Beds and signs wait until you moor.
# Power
* Oar: rowing power. Set it on the deck edge, pointing out.
* Sail: wind power. Set Masts and Sails next to them; sails side by side and on top of each other make one big sheet, and every block of sail counts. Use a sail to furl it (no power) or set it again.
* Raft Motor: the most power. Set it on the stern edge and click it to put furnace fuel in (or take it out), even while you sail.
Heavy rafts need more power: they are slower and turn more slowly.
# Anchor
Set an Anchor Winch on the deck edge, anchor over the side. Use it to drop the anchor: the raft stops and holds its position, even in a current. Use it again to raise it and sail on.
# Good to know
* Up to 1024 blocks and 32 blocks across to set sail; at sea it can grow a little.
* A raft needs water under it. Land, ice and piers stop it.
* Friends, animals and items on deck sail along.
* No room to moor? Sail a little away from the shore."""

LANG = {
    'block.raft.helm': 'Helm',
    'block.raft.sail': 'Sail',
    'block.raft.mast': 'Mast',
    'block.raft.anchor_winch': 'Anchor Winch',
    'block.raft.oar': 'Oar',
    'block.raft.raft_motor': 'Raft Motor',
    'entity.raft.vessel': 'Raft',
    'tooltip.raft.helm': 'Set it on your raft facing the bow and use it to set sail.',
    'tooltip.raft.helm.1': 'Everything on the raft foundations sails along and keeps working.',
    'tooltip.raft.sail': 'Wind power for a raft you built. Sails side by side and on top of each other make one sheet; use it to furl or set it.',
    'tooltip.raft.mast': 'Set sails next to it: their canvas reaches over to the mast.',
    'tooltip.raft.anchor_winch': 'Set it on the deck edge of a raft you built, anchor over the side.',
    'tooltip.raft.anchor_winch.1': 'Use it to drop or raise the anchor: with the anchor down the raft holds its position.',
    'tooltip.raft.oar': 'Rowing power for a raft you built. Set it on the deck edge.',
    'tooltip.raft.raft_motor': 'Strong power for a raft you built. Set it on the stern edge.',
    'tooltip.raft.raft_motor.1': 'Runs on furnace fuel: click it to put fuel in or take it out.',
    'message.raft.vessel.no_hull': 'The helm must stand on a raft of Raft Foundations.',
    'message.raft.vessel.too_big': 'This raft is too big to sail (more than %s blocks).',
    'message.raft.vessel.too_wide': 'This raft is too wide to sail (more than %s blocks across).',
    'message.raft.vessel.not_seaworthy': "%s can't go to sea.",
    'message.raft.vessel.not_floating': 'The raft must float on water to set sail.',
    'message.raft.vessel.not_loaded': 'Part of this raft is too far away to set sail.',
    'message.raft.vessel.controls': 'W/S: sail · A/D: steer · Use the wheel: moor (blocks again) · Sneak: leave the helm',
    'message.raft.vessel.setting_sail': 'Setting sail...',
    'message.raft.vessel.shipyard_full': 'Too many rafts are at sea in this world.',
    'message.raft.vessel.left_helm': 'You left the helm: the raft waits here. Sneak and use the helm to moor it.',
    'message.raft.vessel.anchoring': 'Mooring...',
    'message.raft.vessel.anchored': 'Moored. The raft is blocks again.',
    'message.raft.vessel.no_room': 'No room to moor here. Sail a little away from the shore.',
    'message.raft.vessel.helm_taken': 'Someone is already at the helm.',
    'message.raft.vessel.anchor_first': 'Moor the raft to use this.',
    'message.raft.vessel.no_sleep': "You can't sleep on a sailing raft.",
    'message.raft.vessel.too_far_out': 'Build over the raft: add Raft Foundations to grow it (up to 8 blocks out at sea).',
    'message.raft.vessel.no_open_water': 'Open water would run off a sailing raft.',
    'message.raft.vessel.motor_fuel': 'Motor fuel: %s',
    'message.raft.vessel.motor_full': "The motor's tank is full.",
    'message.raft.vessel.motor_empty': 'The motor has no fuel. Click it and put in coal, charcoal, logs or planks.',
    'container.raft.raft_motor': 'Raft Motor',
    'container.raft.raft_motor.fuel': 'Running time',
    'container.raft.raft_motor.time': '%s',
    'container.raft.raft_motor.empty': 'No fuel',
    'message.raft.sail.furled': 'Sail furled',
    'message.raft.sail.set': 'Sail set',
    'message.raft.anchor.dropped': 'Anchor down: the raft holds its position',
    'message.raft.anchor.raised': 'Anchor up',
    'message.raft.anchor.holding': 'The anchor is down. Raise it at the anchor winch to sail.',
    'advancements.raft.adventure.shipwright.title': 'Shipwright',
    'advancements.raft.adventure.shipwright.description': 'Set sail on a raft you built yourself',
    'almanac.raft.sailing.shipwright.title': 'Your Own Raft',
    'almanac.raft.sailing.shipwright.text': ALMANAC,
}


def main():
    tex.save(motor_texture(), M, 'block/raft_motor')
    tex.save(sail_cloth_texture(), M, 'block/sail_cloth')
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
