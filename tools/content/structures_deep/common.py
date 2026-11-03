"""Shared helpers of the structures_deep module.

- Deep: a bclib Structure with a 'dry' set (positions inside watertight air rooms). finalize() then
  * sets waterlogged=true on every waterloggable block outside the dry set (the structures use
    liquid_settings ignore_waterlogging, so the template decides), false inside,
  * computes connections of panes / bars / railings / fences / walls and the shape of stairs (jigsaw pieces are
    placed with a 'known shape', the game does not fix them).
- leak_check(): fluid flood fill (sideways and down, like water) from everything wet; it must not reach a dry block.
- enclosure_check(): player flood fill through every block that is not in a given unbreakable set.
- JSON writers for structure / structure_set / template_pool / processor_list / biome tag (26.3 formats).
- previews into $SP/work/structures_deep/.
"""
import json
import math
import os
import random
import sys
from collections import deque

TOOLS = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
if TOOLS not in sys.path:
    sys.path.insert(0, TOOLS)

from bclib import preview  # noqa: E402
from bclib.assets import write_json  # noqa: E402
from bclib.nbt import Structure, parse_state, state_str  # noqa: E402

MODULE = 'structures_deep'
WORK = os.environ.get('BC_WORK', '/tmp/claude-0/-home-user/4ca9508b-4f8c-5a8e-91dc-6059d6cc887c/scratchpad/work/'
                                 'structures_deep')

H4 = ((0, -1, 'north'), (1, 0, 'east'), (0, 1, 'south'), (-1, 0, 'west'))
DIRS = {'north': (0, 0, -1), 'south': (0, 0, 1), 'east': (1, 0, 0), 'west': (-1, 0, 0), 'up': (0, 1, 0),
        'down': (0, -1, 0)}
OPP = {'north': 'south', 'south': 'north', 'east': 'west', 'west': 'east', 'up': 'down', 'down': 'up'}
CW = {'north': 'east', 'east': 'south', 'south': 'west', 'west': 'north'}
CCW = {v: k for k, v in CW.items()}

# ------------------------------------------------------------------------------------------- block knowledge
_WL_SUFFIX = ('_stairs', '_slab', '_wall', '_fence', '_pane', '_bars', '_chain', '_trapdoor', '_sign', '_leaves',
              '_grate', '_shelf', '_coral', '_coral_fan', '_coral_wall_fan', '_rail', '_candle', '_lightning_rod',
              '_lantern', '_copper_golem_statue')
_WL_EXACT = {
    'chest', 'trapped_chest', 'ender_chest', 'sea_pickle', 'ladder', 'conduit', 'scaffolding', 'glow_lichen',
    'sculk_vein', 'resin_clump', 'hanging_roots', 'mangrove_roots', 'pointed_dripstone', 'amethyst_cluster',
    'small_amethyst_bud', 'medium_amethyst_bud', 'large_amethyst_bud', 'small_dripleaf', 'big_dripleaf',
    'big_dripleaf_stem', 'light', 'decorated_pot', 'heavy_core', 'campfire', 'soul_campfire', 'mangrove_propagule',
    'dried_ghast', 'barrier', 'sculk_sensor', 'calibrated_sculk_sensor', 'sculk_shrieker', 'iron_bars', 'glass_pane',
    'lantern', 'soul_lantern', 'rail', 'candle', 'lightning_rod'}
_WL_NOT = {'jack_o_lantern', 'sea_lantern'}
_FULL_EXCEPT = {'jack_o_lantern', 'sea_lantern', 'carved_pumpkin'}
# Beach Craft blocks with a `waterlogged` property (spec sections 4.3, 5, 6, 9.2)
RAFT_WL = {
    'aqua_sea_glass_pane', 'green_sea_glass_pane', 'blue_sea_glass_pane', 'cracked_glass_pane', 'metal_hatch',
    'catwalk', 'steel_railing', 'steel_beam', 'rusty_pipe', 'rusty_pipe_valve', 'control_panel', 'sea_anemone_red',
    'sea_anemone_purple', 'sea_anemone_glow', 'giant_clam', 'starfish_orange', 'starfish_purple', 'starfish_red',
    'sea_urchin', 'barnacles', 'tube_worms', 'seashells', 'sea_glass_scatter', 'string_lights_warm',
    'string_lights_colorful', 'bunting_red_white', 'bunting_rainbow', 'ship_lantern', 'jelly_lamp', 'brazier',
    'buoy_red', 'buoy_green', 'neon_tube_cyan', 'neon_tube_pink', 'driftwood_lamp', 'seashell_lamp', 'ship_cannon',
    'ship_anchor', 'treasure_chest', 'gold_coin_pile', 'jolly_roger', 'rum_bottles', 'rope_railing', 'cargo_net',
    'powder_keg', 'ship_bell', 'spyglass_stand', 'figurehead', 'life_ring', 'driftwood_table', 'palm_table',
    'driftwood_chair', 'palm_chair', 'wind_chime', 'research_terminal', 'expedition_radio', 'sigil_pedestal'} | {
    f'paper_lantern_{c}' for c in ('white', 'red', 'orange', 'yellow', 'lime', 'cyan', 'blue', 'purple', 'pink')}
RAFT_FULL = {
    'tidestone', 'tidestone_bricks', 'cracked_tidestone_bricks', 'mossy_tidestone_bricks', 'chiseled_tidestone',
    'tidestone_pillar', 'glowing_tidestone', 'coral_stone', 'polished_coral_stone', 'coral_stone_bricks',
    'mossy_coral_stone_bricks', 'aqua_sea_glass', 'green_sea_glass', 'blue_sea_glass', 'cracked_concrete',
    'mossy_concrete', 'rebar_concrete', 'cracked_glass', 'riveted_hull', 'rusty_hull', 'corrugated_metal',
    'rusted_plate', 'porthole', 'shipping_container_red', 'shipping_container_blue',
    'shipping_container_green', 'sanctum_tidestone', 'sanctum_glass', 'tide_seal', 'bubble_vent', 'pumice',
    'shell_sand', 'black_sand', 'volcanic_ash', 'aquarium', 'wooden_crate', 'lighthouse_lens',
    'weathered_planks', 'teak_planks', 'asphalt', 'asphalt_line'}
_NONFULL_SUFFIX = ('_stairs', '_slab', '_wall', '_fence', '_fence_gate', '_pane', '_bars', '_chain', '_door',
                   '_trapdoor', '_sign', '_button', '_pressure_plate', '_carpet', '_bed', '_torch', '_lantern',
                   '_candle', '_coral', '_fan', '_rail', '_banner', '_head', '_skull', '_sapling', '_pot', '_plate',
                   '_rod', '_cluster', '_bud', '_shelf', '_bulb_off')
_NONFULL_EXACT = {
    'air', 'cave_air', 'void_air', 'water', 'lava', 'kelp', 'kelp_plant', 'seagrass', 'tall_seagrass', 'sea_pickle',
    'chest', 'trapped_chest', 'ender_chest', 'lectern', 'cauldron', 'water_cauldron', 'brewing_stand', 'ladder',
    'lever', 'flower_pot', 'cobweb', 'scaffolding', 'anvil', 'chipped_anvil', 'damaged_anvil', 'bell', 'campfire',
    'soul_campfire', 'conduit', 'end_rod', 'lightning_rod', 'decorated_pot', 'glow_lichen', 'iron_bars',
    'glass_pane', 'lantern', 'soul_lantern', 'torch', 'wall_torch', 'snow', 'grindstone', 'stonecutter',
    'enchanting_table', 'composter', 'hopper', 'heavy_core', 'structure_void', 'jigsaw'}

WASHED = ('_torch', '_carpet', '_button', '_rail', '_candle', '_sapling', 'torch', 'rail', 'candle', 'flower_pot',
          'fire', 'cobweb')


def nm(name):
    return name.split(':', 1)[1]


def is_waterloggable(name):
    ns, p = name.split(':', 1)
    if p in _WL_NOT:
        return False
    if ns == 'raft':
        return p in RAFT_WL or p.endswith(('_stairs', '_slab', '_wall', '_fence', '_trapdoor', '_pane'))
    return p in _WL_EXACT or p.endswith(_WL_SUFFIX)


def is_full(name):
    ns, p = name.split(':', 1)
    if ns == 'raft':
        return p in RAFT_FULL
    if p in _FULL_EXCEPT:
        return True
    if p in _NONFULL_EXACT or p.endswith(_NONFULL_SUFFIX):
        return False
    if p.startswith('potted_'):
        return False
    return True


def is_pane_like(name):
    p = nm(name)
    return p.endswith(('_pane', '_bars')) or p in ('glass_pane', 'iron_bars', 'steel_railing', 'rope_railing')


def is_fence(name):
    return nm(name).endswith('_fence')


def is_wall_block(name):
    return nm(name).endswith('_wall')


def is_stairs(name):
    return nm(name).endswith('_stairs')


# ------------------------------------------------------------------------------------------------ builder
def pick(rnd, palette):
    """palette: list of (state, weight) or a single state string."""
    if isinstance(palette, str):
        return palette
    tot = sum(w for _, w in palette)
    r = rnd.random() * tot
    for s, w in palette:
        r -= w
        if r <= 0:
            return s
    return palette[-1][0]


class Deep(Structure):
    def __init__(self, name='', seed=0):
        super().__init__(name)
        self.dry = set()
        self.force_wet = set()
        self.rnd = random.Random(seed)

    # -- volumes
    def air(self, x1, y1, z1, x2, y2, z2):
        """Dry air room volume (marked dry)."""
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    self.set(x, y, z, 'minecraft:air')
                    self.dry.add((x, y, z))

    def mark_dry(self, x, y, z):
        self.dry.add((x, y, z))

    def water(self, x1, y1, z1, x2, y2, z2, only_empty=False):
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    if only_empty and (x, y, z) in self.blocks and self.blocks[(x, y, z)][0] != 'minecraft:air':
                        continue
                    self.set(x, y, z, 'minecraft:water')
                    self.dry.discard((x, y, z))

    def pfill(self, x1, y1, z1, x2, y2, z2, palette, rnd=None, only_empty=False, hollow=False):
        """fill with a weighted palette."""
        rnd = rnd or self.rnd
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    if hollow and min(x1, x2) < x < max(x1, x2) and min(y1, y2) < y < max(y1, y2) \
                            and min(z1, z2) < z < max(z1, z2):
                        continue
                    if only_empty and (x, y, z) in self.blocks and self.blocks[(x, y, z)][0] not in (
                            'minecraft:air', 'minecraft:water'):
                        continue
                    self.set(x, y, z, pick(rnd, palette))

    def pset(self, x, y, z, palette, rnd=None):
        self.set(x, y, z, pick(rnd or self.rnd, palette))

    def is_solid(self, x, y, z):
        b = self.blocks.get((x, y, z))
        return b is not None and is_full(b[0])

    def free(self, x, y, z):
        """Air or water (inside the template) or not set."""
        b = self.blocks.get((x, y, z))
        return b is None or b[0] in ('minecraft:air', 'minecraft:water')

    def wet_free(self, x, y, z):
        b = self.blocks.get((x, y, z))
        return (b is None or b[0] == 'minecraft:water') and (x, y, z) not in self.dry

    # -- door helpers
    def door(self, x, y, z, block, facing, hinge='left', open_=False):
        for half, dy in (('lower', 0), ('upper', 1)):
            self.set(x, y + dy, z, f'{block}[facing={facing},half={half},hinge={hinge},open={str(open_).lower()},'
                                   f'powered=false]')

    def bed(self, x, y, z, color, facing):
        """facing = direction from foot to head; (x,y,z) is the foot."""
        dx, _, dz = DIRS[facing]
        self.set(x, y, z, f'minecraft:{color}_bed[facing={facing},occupied=false,part=foot]')
        self.set(x + dx, y, z + dz, f'minecraft:{color}_bed[facing={facing},occupied=false,part=head]')

    # -- organic growth (outside, wet)
    def grow_barnacles(self, box, chance, seed, support=None, faces=('north', 'south', 'east', 'west', 'down', 'up'),
                       skip=None):
        """Barnacle layers on wet free positions next to full blocks (support(name) -> bool filters,
        skip(x, y, z) -> True leaves a position alone)."""
        rnd = random.Random(seed)
        (x1, y1, z1), (x2, y2, z2) = box
        for x in range(x1, x2 + 1):
            for y in range(y1, y2 + 1):
                for z in range(z1, z2 + 1):
                    if not self.wet_free(x, y, z) or rnd.random() > chance or (skip and skip(x, y, z)):
                        continue
                    props = {}
                    for f in faces:
                        dx, dy, dz = DIRS[f]
                        nb = self.blocks.get((x + dx, y + dy, z + dz))
                        if nb and is_full(nb[0]) and (support is None or support(nb[0])) and rnd.random() < 0.8:
                            props[f] = 'true'
                    if props:
                        for f in DIRS:
                            props.setdefault(f, 'false')
                        props['waterlogged'] = 'true'
                        self.set(x, y, z, ('raft:barnacles', props))

    def on_top(self, x, y, z):
        """True when (x,y,z) is wet & free and the block below is full."""
        return self.wet_free(x, y, z) and self.is_solid(x, y - 1, z)

    def kelp(self, x, y, z, h, glow=False):
        """Kelp column from y up to y+h-1 (only through wet free space)."""
        plant, top = ('raft:glow_kelp_plant', 'raft:glow_kelp') if glow else ('minecraft:kelp_plant', 'minecraft:kelp')
        n = 0
        for i in range(h):
            if not self.wet_free(x, y + i, z):
                break
            n += 1
        if n == 0:
            return
        for i in range(n - 1):
            self.set(x, y + i, z, plant)
        self.set(x, y + n - 1, z, f'{top}[age={min(25, 15 + n)}]')

    def seagrass(self, x, y, z, tall=False):
        if tall and self.wet_free(x, y + 1, z):
            self.set(x, y, z, 'minecraft:tall_seagrass[half=lower]')
            self.set(x, y + 1, z, 'minecraft:tall_seagrass[half=upper]')
        else:
            self.set(x, y, z, 'minecraft:seagrass')

    # -- final pass
    def finalize(self, wet_max_y=None):
        """Waterlogging (outside dry set), connections, stair shapes. Call once before saving."""
        for pos, (name, props, nbt) in list(self.blocks.items()):
            if is_waterloggable(name):
                x, y, z = pos
                near_dry = any((x + dx, y + dy, z + dz) in self.dry and self.blocks.get(
                    (x + dx, y + dy, z + dz), ('',))[0] == 'minecraft:air' for dx, dy, dz in (
                    (1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)))
                wet = pos not in self.dry and not near_dry and (wet_max_y is None or pos[1] <= wet_max_y)
                if pos in self.force_wet:
                    wet = True
                props = dict(props)
                props['waterlogged'] = 'true' if wet else 'false'
                self.blocks[pos] = (name, props, nbt)
        for pos, (name, props, nbt) in list(self.blocks.items()):
            x, y, z = pos
            if is_pane_like(name) or is_fence(name):
                p = dict(props)
                for dx, dz, f in H4:
                    p[f] = 'true' if self._connects(name, x + dx, y, z + dz, f) else 'false'
                self.blocks[pos] = (name, p, nbt)
            elif is_wall_block(name):
                p = dict(props)
                conn = {}
                for dx, dz, f in H4:
                    c = self._connects(name, x + dx, y, z + dz, f)
                    if c:
                        above = self.blocks.get((x, y + 1, z))
                        above_side = self.blocks.get((x + dx, y + 1, z + dz))
                        tall = above is not None and (is_full(above[0]) or (is_wall_block(above[0]) and above_side
                                                                            and not self.free(x + dx, y + 1, z + dz)))
                        conn[f] = 'tall' if tall else 'low'
                    else:
                        conn[f] = 'none'
                p.update(conn)
                straight = (conn['north'] != 'none' and conn['south'] != 'none' and conn['east'] == 'none'
                            and conn['west'] == 'none') or (conn['east'] != 'none' and conn['west'] != 'none'
                                                            and conn['north'] == 'none' and conn['south'] == 'none')
                above = self.blocks.get((x, y + 1, z))
                up = not straight or (above is not None and (is_wall_block(above[0]) and above[1].get('up') == 'true'
                                                              or nm(above[0]).endswith(('lantern', 'torch'))))
                p['up'] = 'true' if up else 'false'
                self.blocks[pos] = (name, p, nbt)
        for pos, (name, props, nbt) in list(self.blocks.items()):
            if is_stairs(name):
                p = dict(props)
                p.setdefault('half', 'bottom')
                p['shape'] = self._stair_shape(pos, p)
                self.blocks[pos] = (name, p, nbt)

    def _connects(self, name, x, y, z, side):
        b = self.blocks.get((x, y, z))
        if b is None:
            return False
        n2 = b[0]
        if is_pane_like(name):
            if nm(name) in ('steel_railing', 'rope_railing'):
                return nm(n2) == nm(name) or is_full(n2) and nm(n2) not in ('aquarium',) and False
            return is_pane_like(n2) or is_wall_block(n2) or is_full(n2)
        if is_fence(name):
            if is_fence(n2):
                nether = 'nether' in name
                return nether == ('nether' in n2)
            if nm(n2).endswith('_fence_gate'):
                f = b[1].get('facing', 'north')
                return f in ('east', 'west') if side in ('north', 'south') else f in ('north', 'south')
            return is_full(n2)
        if is_wall_block(name):
            if nm(n2).endswith('_fence_gate'):
                f = b[1].get('facing', 'north')
                return f in ('east', 'west') if side in ('north', 'south') else f in ('north', 'south')
            return is_wall_block(n2) or is_pane_like(n2) or is_full(n2)
        return False

    def _stair_shape(self, pos, p):
        x, y, z = pos
        facing, half = p.get('facing', 'north'), p.get('half', 'bottom')

        def st(d):
            dx, _, dz = DIRS[d]
            b = self.blocks.get((x + dx, y, z + dz))
            if b and is_stairs(b[0]):
                return b[1].get('facing', 'north'), b[1].get('half', 'bottom')
            return None

        def can_take(d):
            n = st(d)
            return n is None or n[0] != facing or n[1] != half

        axis = lambda d: 'x' if d in ('east', 'west') else 'z'  # noqa: E731
        behind = st(facing)
        if behind and behind[1] == half and axis(behind[0]) != axis(facing) and can_take(OPP[behind[0]]):
            return 'outer_left' if behind[0] == CCW[facing] else 'outer_right'
        front = st(OPP[facing])
        if front and front[1] == half and axis(front[0]) != axis(facing) and can_take(front[0]):
            return 'inner_left' if front[0] == CCW[facing] else 'inner_right'
        return 'straight'


# ------------------------------------------------------------------------------------------------ checks
def _fluid_passable(b):
    if b is None:
        return True
    name, props = b[0], b[1]
    if name in ('minecraft:air', 'minecraft:water', 'minecraft:cave_air', 'minecraft:structure_void'):
        return True
    if is_waterloggable(name):
        return True
    p = nm(name)
    return p.endswith(WASHED) or p in WASHED


def _is_wet_source(b, pos, dry):
    if pos in dry:
        return False
    if b is None:
        return True
    name, props = b[0], b[1]
    return name == 'minecraft:water' or props.get('waterlogged') == 'true' or nm(name) in (
        'kelp', 'kelp_plant', 'seagrass', 'tall_seagrass', 'bubble_column')


def leak_check(s, margin=1):
    """Water simulation on the template: water spreads sideways and down through fluid-passable positions, starting
    from every wet position (template water, waterlogged blocks, the sea outside = unset positions). Returns a list
    of dry positions that water would reach (empty = watertight)."""
    (x0, y0, z0), (x1, y1, z1) = s.bounds()
    x0 -= margin; y0 -= margin; z0 -= margin; x1 += margin; y1 += margin; z1 += margin
    seen = set()
    q = deque()
    for (pos, b) in list(s.blocks.items()):
        if _is_wet_source(b, pos, s.dry):
            q.append(pos); seen.add(pos)
    # the sea around and inside the template: every unset position keeps the world's block (sea water)
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            for y in range(y0, y1 + 1):
                if (x, y, z) not in s.blocks and (x, y, z) not in seen:
                    q.append((x, y, z)); seen.add((x, y, z))
    for x in range(x0, x1 + 1):
        for y in range(y0, y1 + 1):
            for z in (z0, z1):
                if (x, y, z) not in s.blocks and (x, y, z) not in seen:
                    q.append((x, y, z)); seen.add((x, y, z))
    for z in range(z0, z1 + 1):
        for y in range(y0, y1 + 1):
            for x in (x0, x1):
                if (x, y, z) not in s.blocks and (x, y, z) not in seen:
                    q.append((x, y, z)); seen.add((x, y, z))
    leaks = []
    while q:
        x, y, z = q.popleft()
        for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 0, 1), (0, 0, -1), (0, -1, 0)):
            n = (x + dx, y + dy, z + dz)
            if n in seen or not (x0 <= n[0] <= x1 and y0 <= n[1] <= y1 and z0 <= n[2] <= z1):
                continue
            b = s.blocks.get(n)
            if not _fluid_passable(b):
                continue
            seen.add(n)
            if n in s.dry:
                leaks.append(n)
                continue
            q.append(n)
    return leaks


def enclosure_check(s, inside, unbreakable, margin=2):
    """Player flood fill from outside the template through every position whose block is not in `unbreakable`
    (a set of block names). Returns the positions of `inside` that are reachable (empty = sealed)."""
    (x0, y0, z0), (x1, y1, z1) = s.bounds()
    x0 -= margin; y0 -= margin; z0 -= margin; x1 += margin; y1 += margin; z1 += margin
    start = (x0, y0, z0)
    seen = {start}
    q = deque([start])
    hits = []
    inside = set(inside)
    while q:
        x, y, z = q.popleft()
        for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            n = (x + dx, y + dy, z + dz)
            if n in seen or not (x0 <= n[0] <= x1 and y0 <= n[1] <= y1 and z0 <= n[2] <= z1):
                continue
            b = s.blocks.get(n)
            if b is not None and b[0] in unbreakable:
                continue
            seen.add(n)
            if n in inside:
                hits.append(n)
            q.append(n)
    return hits


_SPEC_TOKENS = None


def _spec_tokens():
    """Every backticked id of the design spec, with the '+ _stairs' / '<color>' / '*_x' shorthands expanded."""
    global _SPEC_TOKENS
    if _SPEC_TOKENS is None:
        import re
        txt = open(os.path.join(TOOLS, '..', 'docs', 'design', 'beachcraft3.md'), encoding='utf-8').read()
        toks = set(re.findall(r'`([a-z0-9_<>*]+)`', txt))
        out = set()
        for line in txt.splitlines():
            ids = re.findall(r'`([a-z0-9_<>*]+)`', line)
            base = None
            for t in ids:
                if t.startswith('_') and base:
                    out.add(base + t)
                    if base.endswith('_bricks'):
                        out.add(base[:-1] + t)
                else:
                    base = t
                out.add(t)
            if 'stairs/slab/wall each for' in line:
                for t in ids:
                    for suf in ('_stairs', '_slab', '_wall'):
                        out.add(t[:-1] + suf if t.endswith('s') else t + suf)
        exp = set()
        colors = ['white', 'red', 'orange', 'yellow', 'lime', 'cyan', 'blue', 'purple', 'pink']
        for t in out | toks:
            if '<color>' in t:
                exp |= {t.replace('<color>', c) for c in colors}
            elif t.startswith('*_'):
                exp |= {c + t[1:] for c in ('aqua', 'green', 'blue')}
            else:
                exp.add(t)
        _SPEC_TOKENS = exp
    return _SPEC_TOKENS


def unknown_ids(s, expected_missing=()):
    """Block ids that are neither vanilla 26.3 (a blockstate file exists) nor named in the spec."""
    from bclib.paths import VANILLA
    bad = []
    for n in sorted({b[0] for b in s.blocks.values()}):
        ns, p = n.split(':', 1)
        if ns == 'minecraft':
            if not os.path.isfile(os.path.join(VANILLA, 'assets', 'minecraft', 'blockstates', p + '.json')) \
                    and p not in ('air', 'water'):
                bad.append(n)
        elif p not in _spec_tokens() and n not in expected_missing:
            bad.append(n)
    return bad


def magenta_ids(s):
    """Ids preview colours magenta (texture not there yet – other modules still building them)."""
    return preview.unknown_blocks(s)


# ------------------------------------------------------------------------------------------------ JSON
def biome_tag(sid, biomes):
    return write_json(MODULE, f'data/raft/tags/worldgen/biome/has_structure/{sid}.json',
                      {'values': [{'id': b if ':' in b else 'raft:' + b, 'required': False} for b in biomes]})


def template_pool(pool_id, elements, fallback='minecraft:empty'):
    """elements: list of (location, weight, processors_id)."""
    data = {'elements': [{'element': {'element_type': 'minecraft:single_pool_element', 'location': loc,
                                      'processors': proc or 'minecraft:empty', 'projection': 'rigid'},
                          'weight': w} for (loc, w, proc) in elements],
            'fallback': fallback}
    ns, path = pool_id.split(':', 1)
    return write_json(MODULE, f'data/{ns}/worldgen/template_pool/{path}.json', data)


def processor_list(pid, processors):
    ns, path = pid.split(':', 1)
    return write_json(MODULE, f'data/{ns}/worldgen/processor_list/{path}.json', {'processors': processors})


def rule(input_block, output_state, probability=None, location=None, input_state_props=None):
    """One minecraft:rule entry."""
    if probability is None:
        ip = {'block': input_block, 'predicate_type': 'minecraft:block_match'}
    else:
        ip = {'block': input_block, 'predicate_type': 'minecraft:random_block_match', 'probability': probability}
    lp = location or {'predicate_type': 'minecraft:always_true'}
    name, props = parse_state(output_state)
    out = {'id': name, 'properties': props} if props else name  # 26.3 block state codec
    return {'input_predicate': ip, 'location_predicate': lp, 'output_state': out}


def structure_json(sid, start_pool, step, terrain, start_height, heightmap='OCEAN_FLOOR_WG', size=1,
                   max_distance=80, spawn_overrides=None, liquid='ignore_waterlogging', start_jigsaw=None):
    d = {'type': 'minecraft:jigsaw', 'biomes': f'#raft:has_structure/{sid}', 'liquid_settings': liquid,
         'max_distance_from_center': max_distance}
    if heightmap:
        d['project_start_to_heightmap'] = heightmap
    d['size'] = size
    d['spawn_overrides'] = spawn_overrides or {}
    d['start_height'] = {'absolute': start_height}
    if start_jigsaw:
        d['start_jigsaw_name'] = start_jigsaw
    d['start_pool'] = start_pool
    d['step'] = step
    d['terrain_adaptation'] = terrain
    d['use_expansion_hack'] = False
    return write_json(MODULE, f'data/raft/worldgen/structure/{sid}.json', d)


def structure_set(sid, spacing, separation, salt, structures=None):
    return write_json(MODULE, f'data/raft/worldgen/structure_set/{sid}.json', {
        'placement': {'type': 'minecraft:random_spread', 'salt': salt, 'separation': separation,
                      'spacing': spacing},
        'structures': [{'structure': s, 'weight': 1} for s in (structures or [f'raft:{sid}'])]})


def spawns(entries, box='piece'):
    """entries: list of (entity_id, weight, min, max) -> spawn override for one category."""
    return {'bounding_box': box, 'spawns': [
        {'type': e, 'count': {'type': 'minecraft:uniform', 'max_inclusive': mx, 'min_inclusive': mn}, 'weight': w}
        for (e, w, mn, mx) in entries]}


NO_SPAWNS = {c: {'bounding_box': 'piece', 'spawns': []} for c in (
    'monster', 'creature', 'ambient', 'axolotls', 'underground_water_creature', 'water_creature', 'water_ambient',
    'misc')}

# processor: water in the template becomes air where the world is air (never spill water above the sea surface)
NO_SPILL = {'processor_type': 'minecraft:rule', 'rules': [
    rule('minecraft:water', 'minecraft:air', location={'predicate_type': 'minecraft:block_match',
                                                       'block': 'minecraft:air'})]}


# ------------------------------------------------------------------------------------------------ previews
def previews(s, name, cuts=(), slices_y=None):
    os.makedirs(WORK, exist_ok=True)
    outs = [preview.iso(s, f'{WORK}/{name}_sw.png'), preview.iso(s, f'{WORK}/{name}_ne.png', from_north_east=True)]
    for c in cuts:
        outs.append(preview.iso(s, f'{WORK}/{name}_cut{c}.png', cut_y=c))
    if slices_y is not None:
        outs.append(preview.slices(s, f'{WORK}/{name}_plan.png', ys=slices_y))
    return outs


def ellipsoid_d(x, y, z, rx, ry, rz):
    return math.sqrt((x / rx) ** 2 + (y / ry) ** 2 + (z / rz) ** 2)


# preview proxies: spec blocks that have no texture yet are drawn with a vanilla look-alike
PROXY = {
    'raft:tidestone': 'minecraft:prismarine', 'raft:tidestone_bricks': 'minecraft:prismarine_bricks',
    'raft:cracked_tidestone_bricks': 'minecraft:dark_prismarine', 'raft:mossy_tidestone_bricks': 'minecraft:oxidized_copper',
    'raft:chiseled_tidestone': 'minecraft:chiseled_tuff_bricks', 'raft:tidestone_pillar': 'minecraft:warped_planks',
    'raft:glowing_tidestone': 'minecraft:sea_lantern', 'raft:sanctum_tidestone': 'minecraft:dark_prismarine',
    'raft:sanctum_glass': 'minecraft:cyan_stained_glass', 'raft:tide_seal': 'minecraft:light_blue_stained_glass',
    'raft:tidestone_brick_stairs': 'minecraft:prismarine_brick_stairs',
    'raft:tidestone_brick_slab': 'minecraft:prismarine_brick_slab', 'raft:tidestone_brick_wall': 'minecraft:prismarine_wall',
    'raft:coral_stone_bricks': 'minecraft:pink_terracotta', 'raft:leviathan_altar': 'minecraft:gold_block',
    'raft:tidewarden_altar': 'minecraft:gold_block', 'raft:sigil_pedestal': 'minecraft:quartz_pillar',
    'raft:barnacles': 'minecraft:tuff', 'raft:tube_worms': 'minecraft:red_terracotta',
    'raft:sea_anemone_glow': 'minecraft:light_blue_concrete', 'raft:sea_anemone_purple': 'minecraft:purple_concrete',
    'raft:sea_anemone_red': 'minecraft:red_concrete', 'raft:brazier': 'minecraft:campfire',
    'raft:gold_coin_pile': 'minecraft:gold_block', 'raft:bubble_vent': 'minecraft:magma_block',
    'raft:riveted_hull': 'minecraft:blue_terracotta', 'raft:rusty_hull': 'minecraft:brown_terracotta',
    'raft:rusted_plate': 'minecraft:orange_terracotta', 'raft:steel_beam': 'minecraft:gray_concrete',
    'raft:porthole': 'minecraft:light_gray_stained_glass', 'raft:aqua_sea_glass': 'minecraft:cyan_stained_glass',
    'raft:blue_sea_glass': 'minecraft:light_blue_stained_glass', 'raft:green_sea_glass': 'minecraft:lime_stained_glass',
    'raft:catwalk': 'minecraft:iron_bars', 'raft:steel_railing': 'minecraft:iron_bars',
    'raft:bulkhead_door': 'minecraft:iron_door', 'raft:metal_hatch': 'minecraft:iron_trapdoor',
    'raft:rusty_pipe': 'minecraft:copper_block', 'raft:rusty_pipe_valve': 'minecraft:red_concrete',
    'raft:control_panel': 'minecraft:observer', 'raft:server_rack': 'minecraft:black_concrete',
    'raft:research_terminal': 'minecraft:green_concrete', 'raft:aquarium': 'minecraft:light_blue_stained_glass',
    'raft:cracked_glass': 'minecraft:glass', 'raft:cracked_concrete': 'minecraft:gray_concrete',
    'raft:corrugated_metal': 'minecraft:iron_block', 'raft:sea_anemone': 'minecraft:red_concrete',
    'raft:giant_clam': 'minecraft:purpur_block', 'raft:coral_stone': 'minecraft:pink_terracotta',
    'raft:polished_coral_stone': 'minecraft:pink_terracotta', 'raft:mossy_coral_stone_bricks': 'minecraft:moss_block',
    'raft:jelly_lamp': 'minecraft:sea_lantern', 'raft:ship_lantern': 'minecraft:lantern',
    'raft:neon_tube_cyan': 'minecraft:light_blue_concrete', 'raft:wooden_crate': 'minecraft:barrel',
    'raft:glow_kelp': 'minecraft:kelp', 'raft:glow_kelp_plant': 'minecraft:kelp_plant',
    'raft:starfish_orange': 'minecraft:orange_concrete', 'raft:starfish_red': 'minecraft:red_concrete',
    'raft:starfish_purple': 'minecraft:purple_concrete', 'raft:seashells': 'minecraft:bone_block',
    'raft:sea_urchin': 'minecraft:black_concrete', 'raft:expedition_radio': 'minecraft:jukebox',
    'raft:life_ring': 'minecraft:red_wool', 'raft:hammock': 'minecraft:white_wool', 'raft:chart_table':
    'minecraft:cartography_table', 'raft:driftwood_table': 'minecraft:oak_planks', 'raft:driftwood_chair':
    'minecraft:oak_stairs', 'raft:driftwood_shelf': 'minecraft:bookshelf', 'raft:treasure_chest': 'minecraft:chest',
}


def proxy(s):
    """Copy of s with spec ids replaced by vanilla look-alikes (only for previews)."""
    from bclib.nbt import Structure as _S
    t = _S(s.name)
    for pos, (name, props, nbt) in s.blocks.items():
        if preview.block_colour(name) != (255, 0, 255, 255):
            t.blocks[pos] = (name, props, nbt)
            continue
        p = PROXY.get(name)
        if p is None:
            base = name
            for suf in ('_stairs', '_slab', '_wall', '_pane'):
                if name.endswith(suf):
                    base = name[:-len(suf)]
            p = PROXY.get(base, name)
        t.blocks[pos] = (p, props, nbt)
    return t


def pv(s, name, cuts=(), slices_y=None, ne=True):
    """Proxy previews (sw, ne, cuts, plan)."""
    t = proxy(s)
    os.makedirs(WORK, exist_ok=True)
    outs = [preview.iso(t, f'{WORK}/{name}_sw.png')]
    if ne:
        outs.append(preview.iso(t, f'{WORK}/{name}_ne.png', from_north_east=True))
    for c in cuts:
        outs.append(preview.iso(t, f'{WORK}/{name}_cut{c}.png', cut_y=c))
    if slices_y is not None:
        outs.append(preview.slices(t, f'{WORK}/{name}_plan.png', ys=slices_y))
    return outs
