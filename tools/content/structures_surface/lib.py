"""Building helpers for the surface story structures (old_lighthouse, saltmere_village, ashen_forge).

B is a bclib Structure with palette support, architecture helpers (roofs, windows, doors, beds, stairs, lamps)
and a finalize() pass that computes the connection / shape properties the game would normally compute itself
(jigsaw pieces are placed with knownShape=true, so fences, panes, walls and stairs must be right in the template).
"""
import math
import os
import random
import sys

TOOLS = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
if TOOLS not in sys.path:
    sys.path.insert(0, TOOLS)

from bclib.nbt import Structure, parse_state, state_str  # noqa: E402

MODULE = 'structures_surface'
WORK = '/tmp/claude-0/-home-user/4ca9508b-4f8c-5a8e-91dc-6059d6cc887c/scratchpad/work/structures_surface'

DIRS = {'north': (0, -1), 'south': (0, 1), 'east': (1, 0), 'west': (-1, 0)}
OPP = {'north': 'south', 'south': 'north', 'east': 'west', 'west': 'east'}
CW = {'north': 'east', 'east': 'south', 'south': 'west', 'west': 'north'}
CCW = {v: k for k, v in CW.items()}


def rnd(*key):
    """Deterministic RNG for a key (seed, x, y, z ...)."""
    return random.Random(':'.join(str(k) for k in key))


def noise2(seed, x, z, scale=4.0):
    """Smooth value noise in [0,1) (bilinear on a hashed lattice)."""
    gx, gz = x / scale, z / scale
    x0, z0 = math.floor(gx), math.floor(gz)
    fx, fz = gx - x0, gz - z0
    fx, fz = fx * fx * (3 - 2 * fx), fz * fz * (3 - 2 * fz)

    def v(a, b):
        return rnd(seed, a, b).random()
    top = v(x0, z0) * (1 - fx) + v(x0 + 1, z0) * fx
    bot = v(x0, z0 + 1) * (1 - fx) + v(x0 + 1, z0 + 1) * fx
    return top * (1 - fz) + bot * fz


class Pal:
    """Weighted block palette; pick(x,y,z) is deterministic per position."""

    def __init__(self, *items, seed='pal'):
        self.items = []
        for it in items:
            if isinstance(it, str):
                self.items.append((it, 1))
            else:
                self.items.append((it[0], it[1]))
        self.total = sum(w for _, w in self.items)
        self.seed = seed

    def pick(self, x, y, z):
        r = rnd(self.seed, x, y, z).random() * self.total
        for st, w in self.items:
            r -= w
            if r < 0:
                return st
        return self.items[-1][0]


def stairs(block, facing, half='bottom', shape='straight'):
    return f'{block}[facing={facing},half={half},shape={shape},waterlogged=false]'


def slab(block, typ='bottom'):
    return f'{block}[type={typ},waterlogged=false]'


def log(block, axis='y'):
    return f'{block}[axis={axis}]'


def trapdoor(block, facing, half='bottom', open_=False):
    return f'{block}[facing={facing},half={half},open={"true" if open_ else "false"},powered=false,waterlogged=false]'


# ------------------------------------------------------------------------------------------------ shape rules
_NOT_FULL = ('_stairs', '_slab', '_fence', '_fence_gate', '_pane', '_door', '_trapdoor', '_carpet', 'torch',
             'lantern', '_button', 'pressure_plate', '_sign', 'banner', 'iron_bars', 'chain', 'sapling', 'short_grass',
             'tall_grass', 'fern', 'vine', 'ladder', 'rail', 'lever', '_rod', 'candle', '_bed', 'chest', 'anvil',
             'cake', 'potted_', 'flower_pot', '_head', '_skull', 'campfire', 'cobweb', 'kelp', 'seagrass', '_fan',
             'sea_pickle', 'lily', 'leaves', 'water', 'lava', 'air', 'jigsaw', 'structure_void', 'scaffolding', 'bell',
             'lectern', 'grindstone', 'stonecutter', 'enchanting', 'brewing', 'hopper', 'cauldron', 'composter',
             'dandelion', 'poppy', 'orchid', 'allium', 'bluet', 'tulip', 'daisy', 'cornflower', 'lily_of', 'rose',
             'peony', 'lilac', 'sunflower', 'dead_bush', 'bamboo', 'sugar_cane', 'cactus', 'snow', 'moss_carpet',
             'glow_lichen', 'decorated_pot', 'end_rod', 'heavy_core', 'daylight', 'carrots', 'potatoes', 'beetroots',
             'wheat', 'farmland', 'dirt_path', 'big_dripleaf', 'small_dripleaf', 'hanging_roots', 'spore', 'azalea',
             'mangrove_roots', 'pointed_dripstone', 'amethyst_cluster', 'bud', 'conduit', 'sweet_berry', 'cocoa',
             'wall_torch', 'redstone_wire', 'repeater', 'comparator', 'tripwire', 'pink_petals', 'leaf_litter',
             'firefly', 'cactus_flower', 'bush', 'dry_', 'wildflowers', 'mushroom', 'fungus', 'roots', 'sprouts',
             'nether_wart', 'frogspawn', 'sniffer', 'turtle_egg', 'pitcher', 'torchflower', 'item_frame', 'button',
             'shelf', 'copper_golem', 'lightning_rod', 'glass_pane', 'brazier')
_RAFT_FULL = {
    'raft:weathered_planks', 'raft:teak_planks', 'raft:aqua_sea_glass', 'raft:green_sea_glass', 'raft:blue_sea_glass',
    'raft:coral_stone', 'raft:polished_coral_stone', 'raft:coral_stone_bricks', 'raft:mossy_coral_stone_bricks',
    'raft:tidestone', 'raft:tidestone_bricks', 'raft:cracked_tidestone_bricks', 'raft:mossy_tidestone_bricks',
    'raft:chiseled_tidestone', 'raft:tidestone_pillar', 'raft:glowing_tidestone', 'raft:cracked_concrete',
    'raft:mossy_concrete', 'raft:rebar_concrete', 'raft:asphalt', 'raft:asphalt_line', 'raft:cracked_glass',
    'raft:riveted_hull', 'raft:rusty_hull', 'raft:corrugated_metal', 'raft:rusted_plate', 'raft:steel_beam',
    'raft:porthole', 'raft:shipping_container_red', 'raft:shipping_container_blue', 'raft:shipping_container_green',
    'raft:palm_thatch', 'raft:driftwood_log', 'raft:driftwood_wood', 'raft:stripped_driftwood_log',
    'raft:stripped_driftwood_wood', 'raft:driftwood_planks', 'raft:palm_log', 'raft:palm_wood', 'raft:stripped_palm_log',
    'raft:stripped_palm_wood', 'raft:palm_planks', 'raft:scrap_metal_block', 'raft:plastic_block', 'raft:sail_canvas',
    'raft:sail_canvas_red_striped', 'raft:titanium_block', 'raft:pumice', 'raft:black_sand', 'raft:volcanic_ash',
    'raft:shell_sand', 'raft:sanctum_tidestone', 'raft:bubble_vent', 'raft:powder_keg', 'raft:wooden_crate',
    'raft:raft_foundation', 'raft:reinforced_foundation',
}
_FENCE_EXCEPT = ('leaves', 'barrier', 'pumpkin', 'melon', 'shulker')


def is_full(name):
    if name is None:
        return False
    if name.startswith('raft:'):
        return name in _RAFT_FULL
    p = name.split(':', 1)[1]
    if p.endswith('_wall') or p.endswith('glass_pane') or p == 'glass_pane':
        return False
    return not any(k in p for k in _NOT_FULL)


def _fence_kind(name):
    if not name:
        return None
    if name == 'raft:rope_railing':
        return 'rope'
    if name.endswith('_fence'):
        return 'nether' if 'nether_brick' in name else 'wood'
    return None


def _pane_like(name):
    return bool(name) and (name.endswith('_pane') or name == 'minecraft:iron_bars' or name == 'raft:steel_railing')


def _wall_like(name):
    return bool(name) and name.endswith('_wall') and not name.endswith('_hanging_wall')


def _gate_connects(props, d):
    f = props.get('facing', 'north')
    # gate in facing north/south spans east-west: connects east/west
    return (f in ('north', 'south')) == (d in ('east', 'west'))


class B(Structure):
    """Structure with palette support and shape finalisation."""

    def __init__(self, name='', seed='s'):
        super().__init__(name)
        self.seed = seed

    # -- palette-aware setters
    def put(self, x, y, z, st):
        if st is None:
            return
        if isinstance(st, Pal):
            st = st.pick(x, y, z)
        elif callable(st):
            st = st(x, y, z)
            if st is None:
                return
        self.set(x, y, z, st)

    def box(self, x1, y1, z1, x2, y2, z2, st, hollow=False, only_empty=False, keep_existing=False):
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    if hollow and min(x1, x2) < x < max(x1, x2) and min(y1, y2) < y < max(y1, y2) \
                            and min(z1, z2) < z < max(z1, z2):
                        continue
                    cur = self.name_at(x, y, z)
                    if keep_existing and cur is not None:
                        continue
                    if only_empty and cur not in (None, 'minecraft:air', 'minecraft:water'):
                        continue
                    self.put(x, y, z, st)

    def wallbox(self, x1, y1, z1, x2, y2, z2, st):
        for y in range(min(y1, y2), max(y1, y2) + 1):
            for x in range(min(x1, x2), max(x1, x2) + 1):
                self.put(x, y, z1, st); self.put(x, y, z2, st)
            for z in range(min(z1, z2), max(z1, z2) + 1):
                self.put(x1, y, z, st); self.put(x2, y, z, st)

    def air(self, x1, y1, z1, x2, y2, z2):
        self.box(x1, y1, z1, x2, y2, z2, 'minecraft:air')

    def air_if_empty(self, x1, y1, z1, x2, y2, z2):
        self.box(x1, y1, z1, x2, y2, z2, 'minecraft:air', keep_existing=True)

    def is_air(self, x, y, z):
        n = self.name_at(x, y, z)
        return n is None or n in ('minecraft:air', 'minecraft:cave_air')

    # -- architecture
    def door(self, x, y, z, block, facing, hinge='left', open_=False):
        o = 'true' if open_ else 'false'
        self.set(x, y, z, f'{block}[facing={facing},half=lower,hinge={hinge},open={o},powered=false]')
        self.set(x, y + 1, z, f'{block}[facing={facing},half=upper,hinge={hinge},open={o},powered=false]')

    def bed(self, x, y, z, color, facing):
        """Foot at (x,y,z), head one block towards facing."""
        dx, dz = DIRS[facing]
        self.set(x, y, z, f'minecraft:{color}_bed[facing={facing},occupied=false,part=foot]')
        self.set(x + dx, y, z + dz, f'minecraft:{color}_bed[facing={facing},occupied=false,part=head]')

    def tall(self, x, y, z, block, extra=''):
        e = (',' + extra) if extra else ''
        self.set(x, y, z, f'{block}[half=lower{e}]')
        self.set(x, y + 1, z, f'{block}[half=upper{e}]')

    def hammock(self, x, y, z, facing):
        dx, dz = DIRS[facing]
        self.set(x, y, z, f'raft:hammock[facing={facing},part=foot]')
        self.set(x + dx, y, z + dz, f'raft:hammock[facing={facing},part=head]')

    def lamp_post(self, x, y, z, height=3, post='minecraft:spruce_fence', lamp='minecraft:lantern[hanging=false,waterlogged=false]',
                  base=None):
        if base:
            self.set(x, y, z, base)
            y += 1
        for i in range(height):
            self.set(x, y + i, z, post)
        self.set(x, y + height, z, lamp)

    def hang(self, x, y, z, lamp='minecraft:lantern', chain=0):
        """Lantern hanging below (x,y,z) which must be a ceiling; chain links first."""
        for i in range(chain):
            self.set(x, y - 1 - i, z, 'minecraft:iron_chain[axis=y,waterlogged=false]')
        self.set(x, y - 1 - chain, z, f'{lamp}[hanging=true,waterlogged=false]')

    def gable_roof(self, x1, z1, x2, z2, y0, axis, st_block, fill=None, overhang=1, top=None, steep=False,
                   gable=None, under=None):
        """Gable roof over the wall rectangle x1..x2, z1..z2, ridge along `axis` ('x' or 'z').
        st_block: stairs id; fill: state/palette under the stairs when steep; top: ridge state (odd widths);
        gable: palette for the triangular end walls; under: upside-down stairs id for the eave fascia.
        Returns the y above the ridge."""
        if axis == 'x':
            a1, a2, l1, l2 = z1 - overhang, z2 + overhang, x1 - overhang, x2 + overhang
            lo_face, hi_face, ends = 'south', 'north', (x1, x2)
        else:
            a1, a2, l1, l2 = x1 - overhang, x2 + overhang, z1 - overhang, z2 + overhang
            lo_face, hi_face, ends = 'east', 'west', (z1, z2)
        rise = 2 if steep else 1

        def P(l, yy, c):
            return (l, yy, c) if axis == 'x' else (c, yy, l)
        y, i = y0, 0
        while True:
            p, q = a1 + i, a2 - i
            if p > q:
                break
            if p == q:
                for l in range(l1, l2 + 1):
                    self.set(*P(l, y, p), top or slab(st_block.replace('_stairs', '_slab')))
                for l in ends:
                    if gable is not None:
                        for yy in range(y - rise + 1, y):
                            self.put(*P(l, yy, p), gable)
                y += 1
                break
            for r in range(rise):
                yy = y + r
                for l in range(l1, l2 + 1):
                    for c, face in ((p, lo_face), (q, hi_face)):
                        if r == rise - 1:
                            self.set(*P(l, yy, c), stairs(st_block, face))
                        elif fill is not None:
                            self.put(*P(l, yy, c), fill)
                if gable is not None:
                    for c in range(p + 1, q):
                        for l in ends:
                            self.put(*P(l, yy, c), gable)
            if under is not None and i == 0:
                for l in range(l1, l2 + 1):
                    for c, face in ((p, lo_face), (q, hi_face)):
                        if self.is_air(*P(l, y - 1, c)):
                            self.set(*P(l, y - 1, c), stairs(under, OPP[face], 'top'))
            y += rise
            i += 1
        return y

    def hip_roof(self, x1, z1, x2, z2, y0, st_block, fill=None, overhang=1, top=None, steep=False, under=None):
        """Pyramid / hip roof: each layer shrinks on all four sides (corner shapes come from finalize)."""
        a, b, c, d = x1 - overhang, z1 - overhang, x2 + overhang, z2 + overhang
        y = y0
        rise = 2 if steep else 1
        first = True
        while a <= c and b <= d:
            if a == c or b == d:
                for x in range(a, c + 1):
                    for z in range(b, d + 1):
                        self.set(x, y, z, top or slab(st_block.replace('_stairs', '_slab')))
                y += 1
                break
            for r in range(rise):
                yy = y + r
                for x in range(a, c + 1):
                    for z in range(b, d + 1):
                        if not (x in (a, c) or z in (b, d)):
                            continue
                        if r < rise - 1:
                            if fill is not None:
                                self.put(x, yy, z, fill)
                            continue
                        f = 'south' if z == b else 'north' if z == d else 'east' if x == a else 'west'
                        self.set(x, yy, z, stairs(st_block, f))
            if under is not None and first:
                for x in range(a, c + 1):
                    for z in range(b, d + 1):
                        if (x in (a, c) or z in (b, d)) and self.is_air(x, y - 1, z):
                            f = 'north' if z == b else 'south' if z == d else 'west' if x == a else 'east'
                            self.set(x, y - 1, z, stairs(under, f, 'top'))
            first = False
            y += rise
            a += 1; b += 1; c -= 1; d -= 1
        return y

    # -- decay / nature
    def vines(self, chance, seed, pred, ys=None, max_len=4):
        """Hang vines on air cells next to blocks whose name matches pred."""
        r = rnd(self.seed, 'vines', seed)
        cells = list(self.blocks.items())
        for (x, y, z), (name, props, nbt) in cells:
            if not pred(name) or (ys and not (ys[0] <= y <= ys[1])):
                continue
            for f, (dx, dz) in DIRS.items():
                nx, nz = x + dx, z + dz
                if self.name_at(nx, y, nz) is not None and not self.is_air(nx, y, nz):
                    continue
                if self.name_at(nx, y, nz) is None:
                    continue  # only into explicit air (inside the build), never outside into the unknown
                if r.random() < chance:
                    L = r.randint(1, max_len)
                    for k in range(L):
                        if not self.is_air(nx, y - k, nz) or self.name_at(nx, y - k, nz) is None:
                            break
                        self.set(nx, y - k, nz, f'minecraft:vine[{OPP[f]}=true]')

    def finalize(self):
        """Compute fence/pane/wall connections and stair shapes from neighbours (like the game's shape updates)."""
        get = self.blocks.get
        updates = {}
        for (x, y, z), (name, props, nbt) in self.blocks.items():
            fk = _fence_kind(name)
            if name.endswith('_fence_gate'):
                continue
            if fk or _pane_like(name) or _wall_like(name):
                p = dict(props)
                conn = {}
                for d, (dx, dz) in DIRS.items():
                    nb = get((x + dx, y, z + dz))
                    nn = nb[0] if nb else None
                    c = False
                    if nn:
                        if fk:
                            ok_kind = _fence_kind(nn)
                            c = (ok_kind is not None and (ok_kind == fk or 'rope' in (ok_kind, fk))) \
                                or (nn.endswith('_fence_gate') and _gate_connects(nb[1], d)) \
                                or (is_full(nn) and not any(e in nn for e in _FENCE_EXCEPT))
                        elif _pane_like(name):
                            c = _pane_like(nn) or _wall_like(nn) or (is_full(nn) and 'leaves' not in nn) \
                                or (nn.endswith('_fence_gate') and _gate_connects(nb[1], d))
                        else:
                            c = _wall_like(nn) or _pane_like(nn) or (is_full(nn) and 'leaves' not in nn) \
                                or (nn.endswith('_fence_gate') and _gate_connects(nb[1], d))
                    conn[d] = c
                if _wall_like(name):
                    above = get((x, y + 1, z))
                    an = above[0] if above else None
                    for d in DIRS:
                        if conn[d]:
                            p[d] = 'tall' if (an and (is_full(an) or _wall_like(an))) else 'low'
                        else:
                            p[d] = 'none'
                    straight = (conn['north'] and conn['south'] and not conn['east'] and not conn['west']) or \
                               (conn['east'] and conn['west'] and not conn['north'] and not conn['south'])
                    p['up'] = 'false' if (straight and not (an and ('lantern' in an or 'torch' in an or
                                                                    _wall_like(an) is False and an.endswith('_fence'))))\
                        else 'true'
                    if an and ('lantern' in an or 'torch' in an or 'brazier' in an or 'candle' in an):
                        p['up'] = 'true'
                else:
                    for d in DIRS:
                        p[d] = 'true' if conn[d] else 'false'
                p.setdefault('waterlogged', 'false')
                updates[(x, y, z)] = (name, p, nbt)
            elif name.endswith('_stairs'):
                p = dict(props)
                f = p.get('facing', 'north'); half = p.get('half', 'bottom')
                shape = 'straight'

                def st_at(d, dist=1):
                    dx, dz = DIRS[d]
                    b = get((x + dx * dist, y, z + dz * dist))
                    if b and b[0].endswith('_stairs') and b[1].get('half', 'bottom') == half:
                        return b[1].get('facing', 'north')
                    return None

                def can_take(d):
                    dx, dz = DIRS[d]
                    b = get((x + dx, y, z + dz))
                    return not (b and b[0].endswith('_stairs') and b[1].get('facing') == f and b[1].get('half', 'bottom') == half)
                back = st_at(f)
                if back and (back in ('east', 'west')) != (f in ('east', 'west')) and can_take(OPP[back]):
                    shape = 'outer_left' if back == CCW[f] else 'outer_right'
                else:
                    front = st_at(OPP[f])
                    if front and (front in ('east', 'west')) != (f in ('east', 'west')) and can_take(front):
                        shape = 'inner_left' if front == CCW[f] else 'inner_right'
                p['shape'] = shape
                p.setdefault('waterlogged', 'false')
                updates[(x, y, z)] = (name, p, nbt)
        self.blocks.update(updates)
        return self


def copy_rotated(src, q):
    """Rotate a B clockwise q quarter turns (keeps the class)."""
    r = src.rotated(q)
    b = B(src.name, src.seed)
    b.blocks = r.blocks
    b.entities = r.entities
    # fix multiface/vine side props that rotated() copied (it keeps the old keys too)
    return b


# ------------------------------------------------------------------------------------------------ preview / save
def look(s, tag, cuts=(), slices_ys=None, ne=True, scale=5):
    """Write iso previews (SW, NE, cut views) and floor slices into WORK; print unknown block ids."""
    from bclib import preview
    os.makedirs(WORK, exist_ok=True)
    outs = [preview.iso(s, f'{WORK}/{tag}_sw.png', scale=scale)]
    if ne:
        outs.append(preview.iso(s, f'{WORK}/{tag}_ne.png', scale=scale, from_north_east=True))
    for c in cuts:
        outs.append(preview.iso(s, f'{WORK}/{tag}_cut{c}.png', scale=scale, cut_y=c))
    if slices_ys:
        outs.append(preview.slices(s, f'{WORK}/{tag}_slices.png', ys=slices_ys, scale=6))
    unk = preview.unknown_blocks(s)
    print(f'[{tag}] size={s.size()} blocks={len(s.blocks)} unknown={unk}')
    return outs


def check_ids(s, allowed_unknown):
    """Unknown (no texture yet) ids must be spec ids of other modules."""
    from bclib import preview
    bad = [u for u in preview.unknown_blocks(s) if u not in allowed_unknown]
    if bad:
        print('  !! ids not in spec list:', bad)
    return bad


# Spec ids (sections 4-10) this module uses; they may not have textures yet while other modules build them.
SPEC_IDS = {
    # world 4.3
    'raft:shell_sand', 'raft:black_sand', 'raft:volcanic_ash', 'raft:pumice', 'raft:beach_grass', 'raft:sea_oats',
    'raft:hibiscus', 'raft:bird_of_paradise', 'raft:tropical_fern', 'raft:pineapple_bush', 'raft:pineapple_crop',
    'raft:potted_hibiscus', 'raft:potted_tropical_fern', 'raft:potted_palm_sapling', 'raft:glow_kelp',
    'raft:glow_kelp_plant', 'raft:sea_anemone_red', 'raft:sea_anemone_purple', 'raft:sea_anemone_glow',
    'raft:giant_clam', 'raft:starfish_orange', 'raft:starfish_purple', 'raft:starfish_red', 'raft:sea_urchin',
    'raft:barnacles', 'raft:tube_worms', 'raft:bubble_vent', 'raft:seashells', 'raft:sea_glass_scatter',
    # building 5
    'raft:weathered_planks', 'raft:weathered_plank_stairs', 'raft:weathered_plank_slab', 'raft:weathered_plank_fence',
    'raft:weathered_plank_fence_gate', 'raft:weathered_planks_stairs', 'raft:weathered_planks_slab',
    'raft:weathered_planks_fence', 'raft:weathered_planks_fence_gate',
    'raft:teak_planks', 'raft:teak_stairs', 'raft:teak_slab', 'raft:teak_fence', 'raft:teak_fence_gate',
    'raft:teak_planks_stairs', 'raft:teak_planks_slab', 'raft:teak_planks_fence', 'raft:teak_planks_fence_gate',
    'raft:teak_door', 'raft:teak_trapdoor', 'raft:aqua_sea_glass', 'raft:green_sea_glass', 'raft:blue_sea_glass',
    'raft:aqua_sea_glass_pane', 'raft:green_sea_glass_pane', 'raft:blue_sea_glass_pane', 'raft:coral_stone',
    'raft:polished_coral_stone', 'raft:coral_stone_bricks', 'raft:mossy_coral_stone_bricks',
    'raft:coral_stone_brick_stairs', 'raft:coral_stone_brick_slab', 'raft:coral_stone_brick_wall', 'raft:tidestone',
    'raft:tidestone_bricks', 'raft:cracked_tidestone_bricks', 'raft:mossy_tidestone_bricks', 'raft:chiseled_tidestone',
    'raft:tidestone_pillar', 'raft:glowing_tidestone', 'raft:tidestone_brick_stairs', 'raft:tidestone_brick_slab',
    'raft:tidestone_brick_wall', 'raft:cracked_concrete', 'raft:mossy_concrete', 'raft:rebar_concrete', 'raft:asphalt',
    'raft:asphalt_line', 'raft:asphalt_slab', 'raft:cracked_glass', 'raft:cracked_glass_pane', 'raft:riveted_hull',
    'raft:rusty_hull', 'raft:corrugated_metal', 'raft:corrugated_metal_stairs', 'raft:corrugated_metal_slab',
    'raft:rusted_plate', 'raft:steel_beam', 'raft:catwalk', 'raft:steel_railing', 'raft:porthole', 'raft:bulkhead_door',
    'raft:metal_hatch', 'raft:rusty_pipe', 'raft:rusty_pipe_valve', 'raft:shipping_container_red',
    'raft:shipping_container_blue', 'raft:shipping_container_green', 'raft:control_panel', 'raft:server_rack',
    'raft:satellite_dish', 'raft:wind_turbine',
    'raft:weathered_stairs', 'raft:weathered_slab', 'raft:weathered_fence', 'raft:weathered_fence_gate',
    # decor 6
    'raft:string_lights_warm', 'raft:string_lights_colorful', 'raft:bunting_red_white', 'raft:bunting_rainbow',
    'raft:ship_lantern', 'raft:tiki_torch', 'raft:jelly_lamp', 'raft:brazier', 'raft:buoy_red', 'raft:buoy_green',
    'raft:lighthouse_lens', 'raft:neon_tube_cyan', 'raft:neon_tube_pink', 'raft:driftwood_lamp', 'raft:seashell_lamp',
    'raft:ship_cannon', 'raft:ship_anchor', 'raft:treasure_chest', 'raft:gold_coin_pile', 'raft:jolly_roger',
    'raft:wanted_poster', 'raft:rum_bottles', 'raft:rope_railing', 'raft:cargo_net', 'raft:powder_keg',
    'raft:wooden_crate', 'raft:ship_bell', 'raft:chart_table', 'raft:spyglass_stand', 'raft:figurehead',
    'raft:life_ring', 'raft:hammock', 'raft:deck_chair', 'raft:beach_umbrella_red', 'raft:beach_umbrella_blue',
    'raft:beach_umbrella_yellow', 'raft:cushion_white', 'raft:cushion_red', 'raft:cushion_blue', 'raft:cushion_yellow',
    'raft:cushion_green', 'raft:woven_rug', 'raft:driftwood_table', 'raft:palm_table', 'raft:driftwood_chair',
    'raft:palm_chair', 'raft:driftwood_shelf', 'raft:wind_chime', 'raft:aquarium', 'raft:hanging_planter',
} | {f'raft:paper_lantern_{c}' for c in ('white', 'red', 'orange', 'yellow', 'lime', 'cyan', 'blue', 'purple', 'pink')} | {
    # story 9.2
    'raft:tide_seal', 'raft:sanctum_tidestone', 'raft:sanctum_glass', 'raft:tidewarden_altar', 'raft:leviathan_altar',
    'raft:research_terminal', 'raft:expedition_radio', 'raft:sigil_pedestal',
}


_VANILLA_BS = '/tmp/claude-0/-home-user/4ca9508b-4f8c-5a8e-91dc-6059d6cc887c/scratchpad/vanilla/assets/minecraft/blockstates'
_RAFT_20 = {
    'raft:driftwood_portal_frame', 'raft:driftwood_log', 'raft:driftwood_wood', 'raft:stripped_driftwood_log',
    'raft:stripped_driftwood_wood', 'raft:driftwood_planks', 'raft:driftwood_leaves', 'raft:driftwood_sapling',
    'raft:item_catcher', 'raft:palm_log', 'raft:palm_wood', 'raft:stripped_palm_log', 'raft:stripped_palm_wood',
    'raft:palm_planks', 'raft:palm_leaves', 'raft:palm_coconut_leaves', 'raft:palm_sapling', 'raft:driftwood_stairs',
    'raft:driftwood_slab', 'raft:driftwood_button', 'raft:driftwood_pressure_plate', 'raft:driftwood_fence',
    'raft:driftwood_fence_gate', 'raft:driftwood_door', 'raft:driftwood_trapdoor', 'raft:palm_stairs', 'raft:palm_slab',
    'raft:palm_button', 'raft:palm_pressure_plate', 'raft:palm_fence', 'raft:palm_fence_gate', 'raft:palm_door',
    'raft:palm_trapdoor', 'raft:titanium_block', 'raft:titanium_grates', 'raft:makeshift_solar_panel',
    'raft:makeshift_battery', 'raft:brick_furnace', 'raft:titanium_furnace', 'raft:copper_cable', 'raft:titanium_cable',
    'raft:palm_thatch', 'raft:palm_thatch_stairs', 'raft:palm_thatch_slab', 'raft:raft_foundation',
    'raft:reinforced_foundation', 'raft:scrap_metal_block', 'raft:plastic_block', 'raft:rope_ladder', 'raft:rope_coil',
    'raft:sail_canvas', 'raft:sail_canvas_red_striped', 'raft:ship_wheel', 'raft:shark_jaw_trophy', 'raft:raft_flag',
    'raft:coconut_lantern', 'raft:crop_plot', 'raft:simple_grill', 'raft:water_purifier', 'raft:scarecrow',
}


def validate(s, tag=''):
    """Every block id must be vanilla 26.3 (blockstate file exists), a 2.0 raft block or a spec id."""
    bad = set()
    for (name, props, nbt) in s.blocks.values():
        if name.startswith('minecraft:'):
            if not os.path.isfile(os.path.join(_VANILLA_BS, name.split(':')[1] + '.json')):
                bad.add(name)
        elif name not in _RAFT_20 and name not in SPEC_IDS:
            bad.add(name)
    if bad:
        print(f'  !! [{tag}] invalid ids: {sorted(bad)}')
    return sorted(bad)


# Preview colours for spec blocks whose textures other modules have not painted yet (preview only).
_PREVIEW_COLOURS = {
    'weathered_planks': (150, 140, 125), 'weathered_stairs': (150, 140, 125), 'weathered_slab': (150, 140, 125),
    'weathered_fence': (150, 140, 125), 'weathered_fence_gate': (150, 140, 125), 'teak_planks': (95, 60, 40),
    'teak_stairs': (95, 60, 40), 'teak_slab': (95, 60, 40), 'tidestone': (60, 150, 140),
    'tidestone_bricks': (55, 140, 130), 'cracked_tidestone_bricks': (50, 125, 118),
    'mossy_tidestone_bricks': (60, 135, 100), 'chiseled_tidestone': (70, 160, 150), 'tidestone_pillar': (65, 155, 145),
    'glowing_tidestone': (110, 235, 225), 'tidestone_brick_stairs': (55, 140, 130), 'tidestone_brick_slab': (55, 140, 130),
    'tidestone_brick_wall': (55, 140, 130), 'coral_stone': (215, 170, 150), 'coral_stone_bricks': (205, 160, 140),
    'mossy_coral_stone_bricks': (170, 160, 120), 'polished_coral_stone': (220, 180, 160),
    'coral_stone_brick_wall': (205, 160, 140), 'coral_stone_brick_stairs': (205, 160, 140),
    'coral_stone_brick_slab': (205, 160, 140), 'barnacles': (200, 195, 180), 'bunting_rainbow': (230, 120, 60),
    'bunting_red_white': (220, 80, 80), 'string_lights_warm': (250, 220, 120), 'string_lights_colorful': (240, 140, 200),
    'palm_table': (190, 150, 90), 'palm_chair': (190, 150, 90), 'seashell_lamp': (240, 220, 200),
    'jelly_lamp': (120, 200, 255), 'brazier': (80, 70, 60), 'wooden_crate': (160, 120, 70), 'seashells': (240, 225, 200),
    'buoy_red': (220, 40, 40), 'buoy_green': (40, 180, 60), 'ship_anchor': (70, 70, 75),
    'lighthouse_lens': (255, 250, 200), 'driftwood_shelf': (150, 120, 90), 'driftwood_table': (150, 120, 90),
    'driftwood_chair': (150, 120, 90), 'chart_table': (170, 140, 100), 'expedition_radio': (90, 90, 80),
    'life_ring': (230, 90, 80), 'ship_bell': (200, 160, 60), 'hibiscus': (220, 40, 60), 'bird_of_paradise': (240, 140, 40),
    'pineapple_bush': (90, 140, 50), 'pineapple_crop': (90, 140, 50), 'corrugated_metal': (150, 155, 160),
    'corrugated_metal_slab': (150, 155, 160), 'corrugated_metal_stairs': (150, 155, 160), 'aquarium': (100, 170, 220),
    'hanging_planter': (100, 150, 60), 'driftwood_lamp': (220, 200, 150), 'woven_rug': (190, 160, 110),
    'figurehead': (180, 140, 90), 'wind_chime': (230, 220, 200), 'giant_clam': (180, 170, 200),
    'black_sand': (45, 43, 48), 'volcanic_ash': (125, 122, 118), 'pumice': (175, 170, 160), 'tiki_torch': (170, 140, 80),
    'deck_chair': (230, 230, 200), 'rope_railing': (184, 154, 100), 'cargo_net': (184, 154, 100),
    'hammock': (220, 200, 150), 'spyglass_stand': (180, 150, 90), 'rum_bottles': (100, 140, 60),
    'sigil_pedestal': (60, 150, 140), 'potted_hibiscus': (200, 90, 70), 'potted_tropical_fern': (90, 140, 60),
    'potted_palm_sapling': (110, 150, 60), 'ship_lantern': (230, 190, 90), 'treasure_chest': (150, 100, 40),
    'gold_coin_pile': (240, 200, 60), 'powder_keg': (110, 80, 50), 'research_terminal': (80, 90, 100),
    'beach_grass': (150, 180, 90), 'sea_oats': (200, 190, 120), 'tropical_fern': (70, 140, 60),
    'ember_crab': (200, 80, 30), 'rusted_plate': (140, 90, 60), 'steel_beam': (100, 100, 110),
}
for _k, _c in _PREVIEW_COLOURS.items():
    _c4 = _c + (255,)
    try:
        from bclib import preview as _pv
        if _pv.find_texture(f'raft:block/{_k}') is None:
            _pv._FALLBACK.setdefault(_k, _c4)
    except Exception:
        pass
for _col, _rgb in (('white', (235, 235, 230)), ('red', (210, 60, 50)), ('orange', (240, 140, 50)),
                   ('yellow', (245, 210, 70)), ('lime', (130, 210, 70)), ('cyan', (70, 200, 210)),
                   ('blue', (60, 90, 210)), ('purple', (150, 70, 200)), ('pink', (240, 140, 180))):
    try:
        from bclib import preview as _pv
        _pv._FALLBACK.setdefault(f'paper_lantern_{_col}', _rgb + (255,))
        _pv._FALLBACK.setdefault(f'cushion_{_col}', _rgb + (255,))
    except Exception:
        pass
