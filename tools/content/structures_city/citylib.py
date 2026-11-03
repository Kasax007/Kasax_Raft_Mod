"""Port Solace building kit: jigsaw helpers, palettes, street furniture, vehicles, tower shells, interiors, decay.

Conventions (every template of the city):
- y 0..1 foundation, y GY=2 ground layer (asphalt / paving base), y FY=3 sidewalk slabs and building floor slabs.
- The structure starts at OCEAN_FLOOR_WG - 2 (the game puts template y 1 there), so GY sits in the top block of
  the sea floor.
- Buildings face north in their template (street jigsaw at the front face z=0, y=GY); the jigsaw rotates them.
- Horizontal jigsaws sit in the ground layer GY; vertical ones in the roof / bottom slab of stacked tower pieces.
- Most pieces use liquid_settings apply_waterlogging (template flags false, the sea decides; water in the template
  becomes air above the sea surface by the NO_SPILL processor). The City Hall base carries explicit flags
  (ignore_waterlogging) because it holds the dry vault.
"""
import math
import os
import random

from .common import (CCW, CW, DIRS, MODULE, OPP, WORK, Deep, is_full, is_waterloggable, pick, preview, proxy)

NS = 'raft:port_solace'
GY = 2
FY = 3
H4 = (('north', 0, -1), ('east', 1, 0), ('south', 0, 1), ('west', -1, 0))

# ------------------------------------------------------------------------------------------------ palettes
ASPHALT = [('raft:asphalt', 86), ('minecraft:gravel', 5), ('minecraft:sand', 4), ('raft:cracked_concrete', 3),
           ('minecraft:coarse_dirt', 2)]
PAVE_SLAB = [('minecraft:smooth_stone_slab[type=bottom]', 55), ('raft:cracked_concrete_slab[type=bottom]', 20),
             ('minecraft:andesite_slab[type=bottom]', 12), ('minecraft:polished_andesite_slab[type=bottom]', 8),
             ('minecraft:stone_slab[type=bottom]', 5)]
PAVE_BASE = [('minecraft:stone', 60), ('minecraft:andesite', 25), ('minecraft:gravel', 15)]
FOUND = [('minecraft:stone', 50), ('minecraft:cobblestone', 20), ('minecraft:andesite', 15), ('minecraft:gravel', 15)]
CONCRETE = [('minecraft:light_gray_concrete', 46), ('raft:cracked_concrete', 26), ('raft:mossy_concrete', 10),
            ('minecraft:smooth_stone', 8), ('raft:rebar_concrete', 6), ('minecraft:andesite', 4)]
CONCRETE_DARK = [('minecraft:gray_concrete', 50), ('raft:cracked_concrete', 20), ('minecraft:polished_andesite', 15),
                 ('minecraft:tuff', 10), ('raft:rebar_concrete', 5)]
FLOOR = [('minecraft:smooth_stone', 40), ('minecraft:light_gray_concrete', 25), ('raft:cracked_concrete', 20),
         ('minecraft:polished_andesite', 10), ('raft:rebar_concrete', 5)]
RUBBLE = [('raft:cracked_concrete', 30), ('minecraft:gravel', 20), ('raft:rebar_concrete', 12),
          ('minecraft:cobblestone', 10), ('raft:cracked_concrete_slab[type=bottom]', 14),
          ('minecraft:andesite', 6), ('minecraft:light_gray_concrete', 8)]
CAR_COLOURS = ['red', 'blue', 'light_blue', 'yellow', 'lime', 'white', 'orange', 'cyan', 'green']


def wl(state, wet=None):
    return state


def stair(block, facing, half='bottom'):
    return f'{block}[facing={facing},half={half}]'


def slab(block, typ='bottom'):
    return f'{block}[type={typ}]'


def mix(*pairs):
    return list(pairs)


# ------------------------------------------------------------------------------------------------ the builder
class City(Deep):
    """Deep (dry set, waterlogging/connection finalize) plus city helpers."""

    def __init__(self, name='', seed=0, mode='apply'):
        super().__init__(name, seed)
        self.mode = mode          # 'apply' (sea decides waterlogging) or 'explicit' (template decides)
        self.interior = set()     # positions inside buildings (water in the template)

    # -- jigsaws
    def jig(self, x, y, z, facing, name, target, pool, final, sel=0, prio=0):
        if facing in ('up', 'down'):
            orient, joint = f'{facing}_north', 'aligned'
        else:
            orient, joint = f'{facing}_up', 'rollable'
        full = lambda n: n if (':' in n) else f'{NS}/{n}'  # noqa: E731
        self.jigsaw(x, y, z, orient, full(name), full(target), pool if pool == 'minecraft:empty' else full(pool),
                    final_state=final, joint=joint, selection_priority=sel, placement_priority=prio)

    # -- simple helpers
    def column(self, x, z, y1, y2, palette):
        for y in range(y1, y2 + 1):
            self.pset(x, y, z, palette)

    def wet(self, x1, y1, z1, x2, y2, z2):
        """Interior water (marks interior)."""
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    self.set(x, y, z, 'minecraft:water')
                    self.dry.discard((x, y, z))
                    self.interior.add((x, y, z))

    def clear(self, x, y, z):
        """Hole: water where the building/interior is, nothing (sea) outside."""
        if (x, y, z) in self.dry:
            self.set(x, y, z, 'minecraft:air')
        elif (x, y, z) in self.interior:
            self.set(x, y, z, 'minecraft:water')
        else:
            self.set(x, y, z, 'minecraft:water')

    def is_name(self, x, y, z, *subs):
        b = self.blocks.get((x, y, z))
        return b is not None and any(sb in b[0] for sb in subs)

    def foundation(self, x1, z1, x2, z2, top=GY, palette=FOUND):
        self.pfill(x1, 0, z1, x2, top, z2, palette)

    # -- finalize
    def fix_fallers(self):
        """Gravel/sand that would fall (over water, air, plants or an unset cell inside the template) becomes a
        look-alike block that does not fall, so no falling-block entities appear when a neighbour updates."""
        (mx, my, mz), _ = self.bounds()
        free = ('minecraft:air', 'minecraft:water', 'minecraft:seagrass', 'minecraft:tall_seagrass', 'minecraft:kelp',
                'minecraft:kelp_plant', 'minecraft:glow_lichen', 'minecraft:short_grass', 'raft:glow_kelp',
                'raft:glow_kelp_plant')
        repl = {'minecraft:gravel': 'minecraft:tuff', 'minecraft:sand': 'minecraft:smooth_sandstone',
                'minecraft:red_sand': 'minecraft:smooth_red_sandstone', 'minecraft:suspicious_gravel': 'minecraft:tuff',
                'minecraft:suspicious_sand': 'minecraft:smooth_sandstone'}
        n = 0
        for _ in range(3):
            for (x, y, z), (name, props, nbt) in list(self.blocks.items()):
                if name not in repl and not name.endswith('_concrete_powder'):
                    continue
                if y == my:
                    continue
                below = self.blocks.get((x, y - 1, z))
                if below is None or below[0] in free:
                    self.set(x, y, z, repl.get(name, name.replace('_concrete_powder', '_concrete')))
                    n += 1
        # kelp needs a full block or kelp below: cut floating kelp columns
        for (x, y, z), (name, props, nbt) in sorted(self.blocks.items(), key=lambda kv: kv[0][1]):
            if name in ('minecraft:kelp', 'minecraft:kelp_plant') and y > my:
                below = self.blocks.get((x, y - 1, z))
                if below is None or not (is_full(below[0]) or below[0] == 'minecraft:kelp_plant'):
                    self.set(x, y, z, 'minecraft:water')
                    n += 1
        return n

    def done(self):
        self.fix_fallers()
        self.finalize()
        if self.mode == 'apply':
            for pos, (name, props, nbt) in list(self.blocks.items()):
                if props.get('waterlogged') == 'true':
                    p = dict(props)
                    p['waterlogged'] = 'false'
                    self.blocks[pos] = (name, p, nbt)
        return self


# ------------------------------------------------------------------------------------------------ street furniture
def lamp_post(s, x, z, arm='north', y=FY + 1, h=5, broken=False, kind='lantern'):
    """Street lamp standing on the sidewalk at (x, y, z); the arm reaches over the road towards `arm`."""
    s.set(x, y - 1, z, 'minecraft:polished_andesite') if not s.blocks.get((x, y - 1, z)) else None
    s.set(x, y, z, 'minecraft:polished_deepslate_wall')
    top = y + h
    for yy in range(y + 1, top + 1):
        s.set(x, yy, z, 'minecraft:deepslate_tile_wall' if yy < top else 'minecraft:polished_deepslate_wall')
    if broken:
        return
    dx, _, dz = DIRS[arm]
    s.set(x, top + 1, z, slab('minecraft:deepslate_tile_slab', 'bottom'))
    s.set(x + dx, top + 1, z + dz, slab('minecraft:deepslate_tile_slab', 'bottom'))
    if kind == 'lantern':
        s.set(x + dx, top, z + dz, 'minecraft:lantern[hanging=true]')
    else:
        s.set(x + dx, top, z + dz, 'minecraft:sea_lantern')


def fallen_lamp(s, x, y, z, along='x', length=5, light=True):
    """A street lamp lying on the ground (chain = thin pole, lantern at the end)."""
    dx, dz = (1, 0) if along == 'x' else (0, 1)
    s.set(x, y, z, 'minecraft:polished_deepslate_wall')
    for i in range(1, length):
        s.set(x + dx * i, y, z + dz * i, f'minecraft:iron_chain[axis={along}]')
    if light:
        s.set(x + dx * length, y, z + dz * length, 'minecraft:lantern[hanging=false]')


def bench(s, x, y, z, facing, length=3, along='x'):
    """Park bench: stairs seat facing `facing` (the backrest side is opposite)."""
    dx, dz = (1, 0) if along == 'x' else (0, 1)
    for i in range(length):
        s.set(x + dx * i, y, z + dz * i, stair('minecraft:spruce_stairs', OPP[facing]))


def hydrant(s, x, y, z):
    s.set(x, y, z, 'minecraft:red_nether_brick_wall')
    s.set(x, y + 1, z, 'minecraft:red_nether_brick_slab[type=bottom]')


def trash_can(s, x, y, z):
    s.set(x, y, z, 'minecraft:composter') if False else s.set(x, y, z, 'minecraft:mud_brick_wall')


def tree_pit(s, x, y, z, rnd, dead=True):
    """Square planter with a dead tree or a kelp 'tree'. y = sidewalk slab layer."""
    s.set(x, y - 1, z, 'minecraft:mud')
    s.set(x, y, z, 'minecraft:rooted_dirt') if False else s.set(x, y, z, 'minecraft:coarse_dirt')
    for f, dx, dz in H4:
        s.set(x + dx, y, z + dz, slab('minecraft:polished_andesite_slab', 'bottom'))
    if dead and rnd.random() < 0.6:
        h = rnd.randint(3, 5)
        for yy in range(y + 1, y + 1 + h):
            s.set(x, yy, z, 'minecraft:stripped_oak_log[axis=y]')
        for f, dx, dz in H4:
            if rnd.random() < 0.6:
                s.set(x + dx, y + h, z + dz, 'minecraft:oak_fence')
                if rnd.random() < 0.5:
                    s.set(x + dx, y + h + 1, z + dz, 'minecraft:oak_fence')
    else:
        s.kelp(x, y + 1, z, rnd.randint(6, 14))


def traffic_light(s, x, y, z, face='north'):
    for yy in range(y, y + 4):
        s.set(x, yy, z, 'minecraft:iron_bars')
    s.set(x, y + 4, z, 'minecraft:polished_blackstone')
    s.set(x, y + 5, z, 'minecraft:polished_blackstone')
    dx, _, dz = DIRS[face]
    s.set(x + dx, y + 4, z + dz, 'minecraft:lime_stained_glass_pane')
    s.set(x + dx, y + 5, z + dz, 'minecraft:red_stained_glass_pane')
    s.set(x, y + 6, z, slab('minecraft:polished_blackstone_slab', 'bottom'))


def road_sign(s, x, y, z, facing, colour='red'):
    """Sign on a pole: a coloured plate (stop / one way)."""
    for yy in range(y, y + 3):
        s.set(x, yy, z, 'minecraft:iron_bars')
    plate = {'red': 'minecraft:red_terracotta', 'blue': 'minecraft:blue_terracotta',
             'yellow': 'minecraft:yellow_terracotta', 'white': 'minecraft:white_terracotta'}[colour]
    s.set(x, y + 3, z, plate)


def bus_stop(s, x, y, z, along='x', facing='south', length=4):
    """Shelter: glass back wall, roof slab, bench. (x,z) = first back-wall cell; facing = open side."""
    dx, dz = (1, 0) if along == 'x' else (0, 1)
    fx, _, fz = DIRS[facing]
    for i in range(length):
        bx, bz = x + dx * i, z + dz * i
        for yy in range(y, y + 3):
            edge = i in (0, length - 1)
            s.set(bx, yy, bz, 'minecraft:iron_bars' if edge else 'minecraft:light_blue_stained_glass_pane')
        s.set(bx, y + 3, bz, slab('minecraft:smooth_stone_slab', 'bottom'))
        s.set(bx + fx, y + 3, bz + fz, slab('minecraft:smooth_stone_slab', 'bottom'))
        if 0 < i < length - 1:
            s.set(bx + fx, y, bz + fz, stair('minecraft:spruce_stairs', OPP[facing]))
    s.set(x + fx, y, z + fz, 'minecraft:iron_bars')
    s.set(x + fx, y + 1, z + fz, 'minecraft:iron_bars')
    s.set(x + fx, y + 2, z + fz, 'raft:neon_tube_cyan[axis=y]')
    e = length - 1
    s.set(x + dx * e + fx, y, z + dz * e + fz, 'minecraft:iron_bars')
    s.set(x + dx * e + fx, y + 1, z + dz * e + fz, 'minecraft:iron_bars')
    s.set(x + dx * e + fx, y + 2, z + dz * e + fz, 'minecraft:iron_bars')


def phone_booth(s, x, y, z, door='south'):
    for yy in range(y, y + 3):
        for f, dx, dz in H4:
            pass
    for yy in range(y, y + 2):
        s.set(x, yy, z, 'minecraft:red_stained_glass')
    s.set(x, y + 2, z, 'minecraft:red_concrete')
    s.set(x, y + 3, z, slab('minecraft:red_nether_brick_slab', 'bottom'))


def newspaper_box(s, x, y, z, colour='blue'):
    s.set(x, y, z, f'minecraft:{colour}_terracotta' if colour != 'yellow' else 'minecraft:yellow_terracotta')
    s.set(x, y + 1, z, slab('minecraft:polished_blackstone_slab', 'bottom'))


def manhole(s, x, y, z):
    s.set(x, y, z, 'minecraft:iron_trapdoor[facing=north,half=top,open=false]')


# ------------------------------------------------------------------------------------------------ vehicles
def car(s, x, y, z, along='z', colour=None, rnd=None, wreck=0.3, flipped=False):
    """3 wide, 5 long car on the road surface y (first block above asphalt). (x,z) = min corner."""
    rnd = rnd or s.rnd
    c = colour or rnd.choice(CAR_COLOURS)
    body = f'minecraft:{c}_concrete'
    body_slab = {'red': 'minecraft:red_nether_brick_slab', 'blue': 'minecraft:lapis_block',
                 }.get(c)
    slab_b = {'red': 'minecraft:red_nether_brick_slab', 'white': 'minecraft:smooth_quartz_slab',
              'yellow': 'minecraft:smooth_sandstone_slab', 'orange': 'minecraft:smooth_red_sandstone_slab',
              'blue': 'minecraft:warped_slab', 'light_blue': 'minecraft:prismarine_slab',
              'cyan': 'minecraft:prismarine_brick_slab', 'lime': 'minecraft:bamboo_mosaic_slab',
              'green': 'minecraft:mossy_cobblestone_slab'}[c]
    rust = [(body, 70), ('raft:rusted_plate', 15), ('minecraft:brown_terracotta', 8), ('minecraft:exposed_copper', 7)]

    def P(i, j, k, st):
        # i across (0..2), j along (0..4), k up
        if along == 'z':
            px, pz = x + i, z + j
        else:
            px, pz = x + j, z + i
        py = y + (2 - k if flipped else k)
        if flipped and 'type=bottom' in st:
            st = st.replace('type=bottom', 'type=top')
        s.set(px, py, pz, st)

    wheel = 'minecraft:black_concrete'
    for j in range(5):
        for i in range(3):
            if j in (0, 4):
                P(i, j, 0, pick(rnd, rust) if i == 1 else slab(slab_b))
            elif j in (1, 3) and i in (0, 2):
                P(i, j, 0, wheel)
            else:
                P(i, j, 0, pick(rnd, rust))
    win = 'minecraft:black_stained_glass_pane' if rnd.random() < 0.5 else 'minecraft:gray_stained_glass_pane'
    for i in range(3):
        P(i, 0, 1, slab(slab_b))
        P(i, 1, 1, win if rnd.random() > wreck else 'minecraft:water')
        P(i, 2, 1, win if i != 1 else 'minecraft:water')
        P(i, 3, 1, win if rnd.random() > wreck else 'minecraft:water')
        P(i, 4, 1, slab(slab_b))
        for j in (1, 2, 3):
            P(i, j, 2, slab(slab_b) if rnd.random() > wreck * 0.5 else 'minecraft:water')
    if not flipped:
        P(1, 2, 1, 'minecraft:water')


def bus(s, x, y, z, along='z', rnd=None, length=11, tilt=False):
    """City bus, 3 wide, `length` long, 4 high. (x,z) = min corner; y = road surface + 1."""
    rnd = rnd or s.rnd
    body = [('minecraft:cyan_terracotta', 60), ('raft:rusted_plate', 14), ('minecraft:light_blue_terracotta', 10),
            ('minecraft:brown_terracotta', 8), ('minecraft:oxidized_copper', 8)]
    white = [('minecraft:white_terracotta', 70), ('minecraft:white_concrete', 20), ('raft:rusted_plate', 10)]

    def P(i, j, k, st):
        if along == 'z':
            s.set(x + i, y + k, z + j, st)
        else:
            s.set(x + j, y + k, z + i, st)

    for j in range(length):
        for i in range(3):
            wheel = j in (1, 2, length - 3) and i in (0, 2)
            P(i, j, 0, 'minecraft:black_concrete' if wheel else pick(rnd, body))
            side = i in (0, 2)
            end = j in (0, length - 1)
            if side or end:
                door = (i == 2 and j in (2, 3))
                P(i, j, 1, 'minecraft:water' if door else pick(rnd, body))
                glass = 'minecraft:light_blue_stained_glass_pane' if rnd.random() > 0.35 else 'minecraft:water'
                P(i, j, 2, 'minecraft:water' if door else (glass if not (end and i == 1 and j == length - 1)
                                                              else pick(rnd, white)))
                P(i, j, 3, pick(rnd, white))
            else:
                P(i, j, 1, 'minecraft:water')
                P(i, j, 2, 'minecraft:water')
                P(i, j, 3, pick(rnd, white) if rnd.random() > 0.12 else 'minecraft:water')
        if 1 < j < length - 1 and j % 2 == 0:
            P(1, j, 1, stair('minecraft:dark_oak_stairs', 'south' if along == 'z' else 'east'))
    P(1, 0, 2, 'minecraft:light_blue_stained_glass_pane')
    P(0, 0, 3, 'raft:neon_tube_cyan[axis=x]' if along == 'z' else 'raft:neon_tube_cyan[axis=z]')


# ------------------------------------------------------------------------------------------------ growth
def scatter_growth(s, box, rnd, kelp=0.02, grass=0.08, pickles=0.01, kelp_h=(4, 14), lichen=0.0, glow_kelp=0.15):
    """On wet free cells with a solid block below: kelp columns, seagrass, sea pickles; glow lichen on walls."""
    (x1, y1, z1), (x2, y2, z2) = box
    for x in range(x1, x2 + 1):
        for z in range(z1, z2 + 1):
            for y in range(y1, y2 + 1):
                if not s.on_top(x, y, z):
                    continue
                r = rnd.random()
                if r < kelp:
                    s.kelp(x, y, z, rnd.randint(*kelp_h), glow=rnd.random() < glow_kelp)
                elif r < kelp + grass:
                    s.seagrass(x, y, z, tall=rnd.random() < 0.35)
                elif r < kelp + grass + pickles:
                    s.set(x, y, z, f'minecraft:sea_pickle[pickles={rnd.randint(1, 4)}]')
    if lichen:
        for x in range(x1, x2 + 1):
            for z in range(z1, z2 + 1):
                for y in range(y1, y2 + 1):
                    if not s.wet_free(x, y, z) or rnd.random() > lichen:
                        continue
                    props = {}
                    for f, d in DIRS.items():
                        nb = s.blocks.get((x + d[0], y + d[1], z + d[2]))
                        if nb and is_full(nb[0]) and rnd.random() < 0.7:
                            props[f] = 'true'
                    if props:
                        for f in DIRS:
                            props.setdefault(f, 'false')
                        s.set(x, y, z, ('minecraft:glow_lichen', props))


def rubble_pile(s, x, y, z, r, rnd, palette=RUBBLE, height=2):
    """Heap of debris on the ground (y = first free layer)."""
    for dx in range(-r, r + 1):
        for dz in range(-r, r + 1):
            d = math.hypot(dx, dz) / max(r, 1)
            if d > 1.05:
                continue
            hh = max(0, round((1 - d) * height + rnd.random() * 0.8 - 0.3))
            yy = y
            while not s.free(x + dx, yy - 1, z + dz) is False and yy > y - 3:
                yy -= 1
            for k in range(hh):
                if s.free(x + dx, y + k, z + dz):
                    s.set(x + dx, y + k, z + dz, pick(rnd, palette))
            if hh == 0 and rnd.random() < 0.4 and s.free(x + dx, y, z + dz):
                s.set(x + dx, y, z + dz, slab('raft:cracked_concrete_slab'))


def bite(s, cx, cy, cz, rx, ry, rz, rnd, keep_floor_y=None, jag=0.35):
    """Collapse: remove a noisy ellipsoid of blocks (interior -> water, exterior -> sea)."""
    for x in range(int(cx - rx - 1), int(cx + rx + 2)):
        for y in range(int(cy - ry - 1), int(cy + ry + 2)):
            for z in range(int(cz - rz - 1), int(cz + rz + 2)):
                d = math.sqrt(((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 + ((z - cz) / rz) ** 2)
                if d > 1 + rnd.random() * jag:
                    continue
                b = s.blocks.get((x, y, z))
                if b is None or b[0] in ('minecraft:water', 'minecraft:air') or 'jigsaw' in b[0]:
                    continue
                if keep_floor_y is not None and y <= keep_floor_y:
                    continue
                if 'chest' in b[0] or 'terminal' in b[0] or 'barrel' in b[0] or b[2] is not None:
                    continue
                if (x, y, z) in s.interior or (x, y, z) in s.dry:
                    s.set(x, y, z, 'minecraft:water')
                    s.dry.discard((x, y, z))
                else:
                    s.set(x, y, z, 'minecraft:water')


def save_previews(s, name, cuts=(), ne=True, slices_y=None):
    os.makedirs(WORK, exist_ok=True)
    s = proxy(s)
    outs = [preview.iso(s, f'{WORK}/{name}_sw.png', scale=4)]
    if ne:
        outs.append(preview.iso(s, f'{WORK}/{name}_ne.png', scale=4, from_north_east=True))
    for c in cuts:
        outs.append(preview.iso(s, f'{WORK}/{name}_cut{c}.png', scale=4, cut_y=c))
    if slices_y:
        outs.append(preview.slices(s, f'{WORK}/{name}_plan.png', ys=slices_y, scale=5))
    return outs


def contact_sheet(paths, out, width=1800):
    from PIL import Image
    ims = [Image.open(p).convert('RGB') for p in paths if os.path.isfile(p)]
    if not ims:
        return None
    cols = min(len(ims), 3)
    cw = width // cols
    rows, row, cur_w = [], [], 0
    scaled = []
    for im in ims:
        f = cw / im.width
        scaled.append(im.resize((cw, max(1, int(im.height * f)))))
    hs = []
    for i in range(0, len(scaled), cols):
        hs.append(max(im.height for im in scaled[i:i + cols]))
    sheet = Image.new('RGB', (cw * cols, sum(hs)), (20, 20, 24))
    y = 0
    for r, i in enumerate(range(0, len(scaled), cols)):
        for c, im in enumerate(scaled[i:i + cols]):
            sheet.paste(im, (c * cw, y))
        y += hs[r]
    sheet.save(out)
    return out
