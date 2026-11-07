"""helix_station – Meridian Expedition research base on the sea floor (spec 3.2 chapter 4, 11.1).

Template coordinates (x east, z south, y up), hub centre at x=0, z=0:
  y -3..0  concrete foundation pad (y=0 = floor of all modules), U-duct of the moon pool
  hub (dome r=7)      moon pool, dive lockers, terminal helix_1
  airlock (south)     two raft:bulkhead_door (z=7 and z=12), dry chamber between them
  storage (south-west, off the airlock)  raft:chests/helix_storage, terminal lab_misc_2
  crew (west, tube habitat)  bunks, mess, galley, radio, terminal helix_2
  lab (east, glass dome)     sigil analysis rig, aquariums, terminals helix_3 + helix_4, raft:chests/story/helix_lab
  generator (north)   energy core, consoles, server racks, terminal helix_5
  vault (behind the generator)  raft:chests/story/helix_vault, terminal helix_6
  flooded wing (north-west, broken observation dome, water)  debris, moray eels, raft:chests/helix_storage,
                       terminal lab_misc_1
Dry rooms are enclosed and filled with air; liquid_settings ignore_waterlogging, so the template decides every
waterlogged flag (common.Deep.finalize) and leak_check proves the dry rooms watertight.
"""
import math
import random

from .common import (Deep, MODULE, NO_SPILL, leak_check, pick, processor_list, spawns, structure_json,
                     structure_set, template_pool, biome_tag, pv)

SID = 'helix_station'
RIB = 'minecraft:white_concrete'      # painted steel ribs (Expedition white)
RIB_RED = 'minecraft:red_concrete'    # every other rib in Expedition red (white/red stripes, spec 2)


def rib_colour(i):
    return RIB_RED if i % 2 else RIB
HULL = [('raft:riveted_hull', 78), ('raft:rusty_hull', 22)]
HULL_RUSTY = [('raft:riveted_hull', 45), ('raft:rusty_hull', 55)]
PAD = [('raft:cracked_concrete', 50), ('minecraft:light_gray_concrete', 25), ('raft:mossy_concrete', 15),
       ('raft:rebar_concrete', 10)]
FLOOR = [('raft:rusted_plate', 80), ('minecraft:polished_andesite', 20)]
BULB = 'minecraft:waxed_copper_bulb[lit=true,powered=false]'
BULB_DIM = 'minecraft:waxed_weathered_copper_bulb[lit=true,powered=false]'
DOOR = 'raft:bulkhead_door'


def rad(x, z, cx=0, cz=0):
    return math.hypot(x - cx, z - cz)


class Station(Deep):
    def interior_air(self, pts):
        for p in pts:
            self.set(*p, 'minecraft:air')
            self.dry.add(p)


# ----------------------------------------------------------------------------------------------- shells
def dome_shell(s, cx, cz, R, wall_top, cap_ry, glass, rnd, ribs=8, porthole_y=2):
    """Round module: vertical wall y=1..wall_top, elliptic glass cap with hull ribs; returns interior positions."""
    inner = []
    Ri = R - 1.15
    for x in range(int(cx - R) - 1, int(cx + R) + 2):
        for z in range(int(cz - R) - 1, int(cz + R) + 2):
            r = rad(x, z, cx, cz)
            if r > R + 0.35:
                continue
            a = math.degrees(math.atan2(z - cz, x - cx)) % 360
            rib = min(abs((a - k * 360 / ribs + 180) % 360 - 180) for k in range(ribs)) * math.pi / 180 * max(r, 1)
            s.pset(x, 0, z, FLOOR if r <= Ri + 0.2 else HULL, rnd)
            for y in range(1, wall_top + int(cap_ry) + 2):
                if y <= wall_top:
                    out_ok, in_ok = r <= R + 0.35, r <= Ri + 0.3
                else:
                    dy = y - wall_top
                    out_ok = (r / (R + 0.35)) ** 2 + (dy / (cap_ry + 0.35)) ** 2 <= 1.0
                    in_ok = (r / (Ri + 0.3)) ** 2 + (dy / (cap_ry - 0.85)) ** 2 <= 1.0
                if in_ok:
                    inner.append((x, y, z))
                elif out_ok:
                    if y <= wall_top:
                        if y == porthole_y and rib > 1.6 and int(a) % 30 < 14:
                            s.set(x, y, z, 'raft:porthole')
                        elif y == wall_top:
                            s.set(x, y, z, 'raft:riveted_hull')
                        else:
                            s.pset(x, y, z, HULL, rnd)
                    else:
                        top = r < 1.8
                        s.set(x, y, z, 'raft:riveted_hull' if rib < 1.0 or top else pick(rnd, glass))
    return inner


def tube_x(s, x1, x2, zc, rnd, porthole_every=3, ribs_every=4):
    """Straight tunnel along x: interior z zc-1..zc+1, y 1..3; returns interior positions."""
    inner = []
    for x in range(x1, x2 + 1):
        rib = (x - x1) % ribs_every == 0
        rc = rib_colour((x - x1) // ribs_every)
        for dz in range(-2, 3):
            s.pset(x, 0, zc + dz, FLOOR if abs(dz) < 2 else HULL, rnd)
            if abs(dz) < 2:
                s.set(x, 4, zc + dz, rc if rib else pick(rnd, HULL))
        for y in (1, 2, 3):
            for dz in (-2, 2):
                if rib:
                    s.set(x, y, zc + dz, rc)
                elif y == 2 and (x - x1) % porthole_every == 1:
                    s.set(x, y, zc + dz, 'raft:porthole')
                else:
                    s.pset(x, y, zc + dz, HULL, rnd)
            for dz in (-1, 0, 1):
                inner.append((x, y, zc + dz))
        # brass chamfers on the roof edge (outside)
        for dz, f in ((-2, 'south'), (2, 'north')):
            if s.name_at(x, 4, zc + dz) is None:
                s.set(x, 4, zc + dz, f'minecraft:waxed_cut_copper_stairs[facing={f},half=bottom]')
    return inner


def tube_z(s, z1, z2, xc, rnd, porthole_every=3, ribs_every=4):
    inner = []
    for z in range(z1, z2 + 1):
        rib = (z - z1) % ribs_every == 0
        rc = rib_colour((z - z1) // ribs_every)
        for dx in range(-2, 3):
            s.pset(xc + dx, 0, z, FLOOR if abs(dx) < 2 else HULL, rnd)
            if abs(dx) < 2:
                s.set(xc + dx, 4, z, rc if rib else pick(rnd, HULL))
        for y in (1, 2, 3):
            for dx in (-2, 2):
                if rib:
                    s.set(xc + dx, y, z, rc)
                elif y == 2 and (z - z1) % porthole_every == 1:
                    s.set(xc + dx, y, z, 'raft:porthole')
                else:
                    s.pset(xc + dx, y, z, HULL, rnd)
            for dx in (-1, 0, 1):
                inner.append((xc + dx, y, z))
        for dx, f in ((-2, 'east'), (2, 'west')):
            if s.name_at(xc + dx, 4, z) is None:
                s.set(xc + dx, 4, z, f'minecraft:waxed_cut_copper_stairs[facing={f},half=bottom]')
    return inner


def box_shell(s, x1, z1, x2, z2, top, rnd, palette=None, roof=True):
    """Rectangular module: walls x1..x2 / z1..z2, y 0..top; returns interior positions."""
    palette = palette or HULL
    inner = []
    for x in range(x1, x2 + 1):
        for z in range(z1, z2 + 1):
            edge = x in (x1, x2) or z in (z1, z2)
            s.pset(x, 0, z, HULL if edge else FLOOR, rnd)
            for y in range(1, top):
                if edge:
                    corner = x in (x1, x2) and z in (z1, z2)
                    if corner:
                        s.set(x, y, z, RIB)
                    elif y == 2 and (x + z) % 3 == 0:
                        s.set(x, y, z, 'raft:porthole')
                    elif y == top - 1:
                        s.set(x, y, z, 'raft:riveted_hull')
                    else:
                        s.pset(x, y, z, palette, rnd)
                else:
                    inner.append((x, y, z))
            if roof:
                s.set(x, top, z, 'raft:riveted_hull' if edge else ('raft:rusted_plate' if rnd.random() < 0.06
                                                                    else 'raft:corrugated_metal'))
    return inner


def crew_cylinder(s, x1, x2, rnd):
    """Tube habitat along x, axis at y=3, z=0, outer radius 5; returns interior positions."""
    inner = []
    for x in range(x1, x2 + 1):
        cap = x in (x1, x2)
        rib = (x - x1) % 4 == 0
        for z in range(-6, 7):
            for y in range(0, 10):
                d = math.hypot(y - 3, z)
                if d > 5.35:
                    continue
                if y == 0:
                    s.pset(x, 0, z, FLOOR if d < 4.2 and not cap else HULL, rnd)
                    continue
                if d <= 4.2 and not cap:
                    inner.append((x, y, z))
                    continue
                if rib and not cap:
                    s.set(x, y, z, rib_colour((x - x1) // 4))
                elif not cap and y == 3 and abs(z) >= 4 and (x - x1) % 4 == 2:
                    s.set(x, y, z, 'raft:porthole')
                elif cap and d < 2.2 and y >= 2:
                    s.set(x, y, z, 'raft:porthole')
                else:
                    s.pset(x, y, z, HULL, rnd)
    return inner


# ----------------------------------------------------------------------------------------------- build
def build(seed=31415960, variant=0):
    rnd = random.Random(seed)
    s = Station(f'{SID}/station_{variant}', seed)
    pad(s, rnd)
    interiors = []
    interiors += dome_shell(s, 0, 0, 7.0, 3, 5.0, [('raft:aqua_sea_glass', 6), ('raft:blue_sea_glass', 1)], rnd)
    interiors += dome_shell(s, 19, 0, 6.5, 3, 4.5, [('raft:aqua_sea_glass', 4), ('raft:green_sea_glass', 2),
                                                    ('raft:blue_sea_glass', 1)], rnd, ribs=6)
    flooded = dome_shell(s, -18, -19, 5.5, 3, 4.0, [('raft:aqua_sea_glass', 3), ('raft:cracked_glass', 2)], rnd,
                         ribs=6)
    interiors += crew_cylinder(s, -27, -14, rnd)
    interiors += tube_x(s, 6, 13, 0, rnd)          # hub -> lab
    interiors += tube_x(s, -14, -6, 0, rnd)        # crew -> hub
    interiors += tube_z(s, -13, -6, 0, rnd)        # generator -> hub
    flooded += tube_x(s, -12, -8, -19, rnd, ribs_every=5)   # generator -> flooded dome
    interiors += box_shell(s, -7, -25, 7, -13, 6, rnd)     # generator
    interiors += box_shell(s, -4, -33, 4, -25, 5, rnd, palette=[('raft:riveted_hull', 1)])  # vault
    interiors += box_shell(s, -9, 7, -2, 13, 4, rnd)       # storage
    airlock = box_shell(s, -2, 7, 2, 12, 4, rnd)           # airlock chamber
    interiors += airlock
    # vault: double walls (inner lining of iron)
    for x in range(-3, 4):
        for z in range(-32, -25):
            for y in range(1, 5):
                if x in (-3, 3) or z in (-32, -26):
                    s.set(x, y, z, 'minecraft:iron_block' if (x + y + z) % 4 else 'raft:rusted_plate')
    interiors = [p for p in interiors if not (-3 <= p[0] <= 3 and -32 <= p[2] <= -26 and
                                              (p[0] in (-3, 3) or p[2] in (-32, -26)))]
    s.interior_air(interiors)
    for p in flooded:
        s.set(*p, 'minecraft:water')
    # openings between modules (dry)
    s.air(-1, 1, 5, 1, 3, 6)                        # hub side of the airlock door
    s.air(-1, 1, -13, 1, 3, -12)                     # generator <- tunnel
    s.air(5, 1, -1, 6, 3, 1)                         # hub -> lab tunnel
    s.air(13, 1, -1, 14, 3, 1)                       # tunnel -> lab
    s.air(-15, 1, -1, -13, 3, 1)                     # crew <- tunnel
    s.air(-6, 1, -1, -5, 3, 1)                       # tunnel -> hub
    s.air(-1, 1, -6, 1, 3, -5)                       # tunnel -> hub (north)
    s.water(-13, 1, -20, -12, 3, -18)                # flooded tunnel -> dome
    airlock_doors(s, rnd)
    moon_pool(s, rnd)
    hub(s, rnd)
    crew(s, rnd)
    lab(s, rnd)
    generator(s, rnd)
    vault(s, rnd)
    storage(s, rnd)
    flooded_wing(s, rnd)
    exterior(s, rnd)
    s.finalize()
    return s


def pad(s, rnd):
    """Foundation y -3..0 under the whole station, rounded, with a walkway to the airlock."""
    shapes = [('c', 0, 0, 9.5), ('c', 19, 0, 8.5), ('c', -18, -19, 7.5), ('r', -29, -7, -12, 7),
              ('r', -9, -27, 9, -11), ('r', -6, -35, 6, -23), ('r', -11, 5, 4, 18), ('r', -13, -21, -6, -17),
              ('r', 4, -2, 14, 7), ('r', -14, -3, -4, 3)]

    def inside(x, z):
        for sh in shapes:
            if sh[0] == 'c' and rad(x, z, sh[1], sh[2]) <= sh[3]:
                return True
            if sh[0] == 'r' and sh[1] <= x <= sh[3] and sh[2] <= z <= sh[4]:
                return True
        return False
    for x in range(-31, 30):
        for z in range(-37, 20):
            if not inside(x, z):
                continue
            edge = not all(inside(x + dx, z + dz) for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            for y in (-3, -2, -1):
                s.pset(x, y, z, PAD, rnd)
            s.set(x, 0, z, pick(rnd, PAD) if not edge else 'minecraft:smooth_stone')
    # walkway to the airlock with lights
    for z in range(13, 19):
        for x in (-1, 0, 1):
            s.set(x, 0, z, 'raft:rusted_plate')
        if z % 2 == 1:
            s.set(-3, 0, z, 'minecraft:sea_lantern')
            s.set(3, 0, z, 'minecraft:sea_lantern')


def airlock_doors(s, rnd):
    # inner door in the hub wall (z=7), outer door at z=12; frames of steel
    for z in (7, 12):
        for x in (-1, 1):
            for y in (1, 2, 3):
                s.set(x, y, z, 'minecraft:iron_block')
        s.set(0, 3, z, 'minecraft:iron_block')
        s.door(0, 1, z, DOOR, 'south' if z == 12 else 'north')
        s.dry.add((0, 1, z)); s.dry.add((0, 2, z))
    # airlock chamber details
    s.set(-1, 2, 9, 'raft:control_panel[facing=east]')
    s.set(1, 2, 10, 'raft:rusty_pipe_valve[axis=y]')
    s.set(1, 1, 10, 'raft:rusty_pipe[axis=y]')
    s.set(1, 3, 10, 'raft:rusty_pipe[axis=y]')
    s.set(-1, 1, 11, 'minecraft:barrel[facing=up,open=false]')
    s.set(1, 2, 8, 'raft:life_ring[facing=west]')
    s.set(0, 3, 9, BULB)
    for z in range(8, 12):
        s.set(0, 0, z, 'minecraft:yellow_terracotta' if z % 2 else 'minecraft:black_terracotta')
    # side door into the storage room (west wall of the airlock)
    s.air(-3, 1, 10, -2, 2, 10)
    s.door(-2, 1, 10, DOOR, 'west')


def moon_pool(s, rnd):
    """Moon pool in the hub floor (x 2..4, z 2..4) -> U-duct under the wall -> dive pit outside (x 9..12, z 3..6)."""
    for x in range(2, 5):
        for z in range(2, 5):
            for y in (-2, -1, 0):
                s.set(x, y, z, 'minecraft:water')
            s.set(x, -3, z, 'raft:rusted_plate')
    for x in range(5, 10):
        for z in (3, 4):
            s.set(x, -2, z, 'minecraft:water')
            s.set(x, -1, z, 'minecraft:water')
            s.set(x, -3, z, 'raft:rusted_plate')
    for x in range(9, 13):
        for z in range(3, 7):
            for y in (-2, -1, 0):
                s.set(x, y, z, 'minecraft:water')
            s.set(x, -3, z, 'minecraft:sand')
    # pit rim and marker lights
    for (x, z) in ((8, 7), (13, 7)):
        s.set(x, 0, z, 'minecraft:sea_lantern')
        s.set(x, 1, z, 'raft:steel_beam[axis=y]')
        s.set(x, 2, z, 'raft:buoy_red' if (x + z) % 2 else 'raft:buoy_green')
    # railing around the pool inside the hub (open towards the lockers)
    for x in range(1, 6):
        s.set(x, 1, 1, 'raft:steel_railing')
    for z in range(2, 6):
        s.set(1, 1, z, 'raft:steel_railing')
    s.set(1, 1, 3, 'minecraft:air')        # gap to climb out
    for x in range(1, 6):
        for z in range(1, 6):
            if s.name_at(x, 0, z) not in ('minecraft:water',) and rad(x, z) <= 6.2:
                s.set(x, 0, z, 'minecraft:yellow_terracotta' if (x + z) % 2 else 'minecraft:black_terracotta')
    s.set(3, 1, 5, 'minecraft:air')


def terminal(s, x, y, z, facing, log):
    s.block_entity(x, y, z, f'raft:research_terminal[facing={facing}]', 'raft:research_terminal', {'LogId': log})


def hub(s, rnd):
    # central steel column with a hanging lamp ring
    for y in range(1, 8):
        s.set(0, y, 0, 'raft:steel_beam[axis=y]')
    for (dx, dz) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        s.set(dx, 6, dz, BULB)
    # the chart table stands next to the column (under it, the things on the table stuck into the beam)
    s.set(0, 1, -1, 'raft:chart_table[facing=north]')
    # dive lockers on the west wall
    for z in (-3, -2, -1, 1, 2, 3):
        x = -int(math.sqrt(max(0.0, 6.2 ** 2 - z * z)))
        x = max(x, -6)
        s.set(x, 1, z, 'minecraft:barrel[facing=east,open=false]')
        s.set(x, 2, z, 'minecraft:iron_trapdoor[facing=east,half=bottom,open=true,powered=false]')
        s.set(x, 3, z, 'minecraft:barrel[facing=east,open=false]' if z % 2 else BULB_DIM)
    s.set(-4, 1, -4, 'raft:driftwood_table')
    s.set(-3, 1, -4, 'raft:driftwood_chair[facing=west]')
    s.set(-4, 2, -4, 'minecraft:flower_pot')
    terminal(s, -3, 1, 5, 'north', 'helix_1')
    s.set(-4, 1, 4, 'raft:driftwood_chair[facing=east]')
    s.set(5, 1, -3, 'raft:wooden_crate[facing=west,open=false]')
    s.set(5, 2, -3, 'raft:ship_lantern[hanging=false]')
    s.set(4, 1, -4, 'minecraft:cauldron')
    s.set(-2, 1, -5, 'raft:expedition_radio[facing=south]')
    s.set(-3, 1, -5, 'minecraft:barrel[facing=up,open=false]')
    s.set(2, 6, 2, 'raft:jelly_lamp[hanging=true]')
    s.set(-2, 6, -2, 'raft:jelly_lamp[hanging=true]')
    for (x, z) in ((-2, 2), (2, -2), (-1, -3), (3, 0)):
        s.set(x, 0, z, 'minecraft:polished_andesite')
    s.set(-1, 1, 3, 'raft:woven_rug')
    s.set(-2, 1, 3, 'raft:woven_rug')


def crew(s, rnd):
    # bunks along the north wall
    for x0 in (-25, -21, -17):
        s.bed(x0, 1, -3, 'blue', 'west')
        s.set(x0 - 1, 2, -3, 'minecraft:spruce_slab[type=top]')
        s.set(x0, 2, -3, 'minecraft:spruce_slab[type=top]')
        s.bed(x0, 3, -3, 'white', 'west')
        s.set(x0 + 1, 1, -3, 'minecraft:barrel[facing=south,open=false]')
        s.set(x0 + 1, 2, -3, 'minecraft:barrel[facing=south,open=false]')
        s.set(x0 + 1, 3, -3, 'raft:ship_lantern[hanging=false]')
    # mess table in the south half
    for x in (-22, -21, -20, -19):
        s.set(x, 1, 2, 'raft:driftwood_table')
        s.set(x, 1, 1, 'raft:driftwood_chair[facing=south]')
        s.set(x, 1, 3, 'raft:driftwood_chair[facing=north]')
    s.set(-21, 2, 2, 'minecraft:flower_pot')
    s.set(-19, 2, 2, 'raft:rum_bottles[bottles=2,facing=north]')
    # galley at the west end
    s.set(-26, 1, -2, 'minecraft:smoker[facing=east,lit=false]')
    s.set(-26, 1, -1, 'minecraft:furnace[facing=east,lit=false]')
    s.set(-26, 1, 0, 'minecraft:water_cauldron[level=2]')
    s.set(-26, 1, 1, 'minecraft:barrel[facing=east,open=false]')
    s.set(-26, 1, 2, 'minecraft:crafting_table')
    s.set(-26, 2, 2, 'minecraft:barrel[facing=up,open=false]')
    s.set(-26, 3, -1, 'raft:driftwood_shelf[facing=east]')
    s.set(-26, 3, 1, 'raft:driftwood_shelf[facing=east]')
    s.set(-25, 1, 3, 'raft:wooden_crate[facing=north,open=false]')
    # radio corner and Voss's log near the tunnel
    s.set(-16, 1, 3, 'raft:driftwood_table')
    s.set(-16, 2, 3, 'raft:expedition_radio[facing=north]')
    s.set(-17, 1, 3, 'raft:cushion_blue')
    terminal(s, -15, 1, 3, 'north', 'helix_2')
    s.set(-24, 1, 1, 'raft:woven_rug'); s.set(-23, 1, 1, 'raft:woven_rug')
    # ceiling: lanterns hang from the crown of the tube (y=8 is the shell), bulbs set into the shell
    for x in (-25, -21, -17):
        s.set(x, 7, 0, 'raft:ship_lantern[hanging=true]')
    s.set(-23, 8, 0, BULB)
    s.set(-19, 8, 0, BULB_DIM)


def lab(s, rnd):
    cx = 19
    # sigil analysis rig in the centre: pedestal under glass, probes pointing at it
    s.set(cx, 0, 0, 'minecraft:polished_andesite')
    s.set(cx, 1, 0, 'raft:sigil_pedestal')
    for (dx, dz) in ((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (1, -1), (-1, 1), (-1, -1)):
        s.set(cx + dx, 1, dz, 'raft:aqua_sea_glass_pane' if dx == 0 or dz == 0 else 'raft:steel_beam[axis=y]')
        s.set(cx + dx, 2, dz, 'raft:aqua_sea_glass_pane' if dx == 0 or dz == 0 else 'minecraft:lightning_rod[facing=up,powered=false]')
    s.set(cx, 3, 0, 'minecraft:lightning_rod[facing=down,powered=false]')
    s.set(cx, 4, 0, 'raft:rusty_pipe[axis=y]')
    s.set(cx, 5, 0, 'raft:rusty_pipe[axis=y]')
    s.set(cx, 6, 0, BULB)
    # aquarium wall (east) and a real specimen tank
    for z in (-2, -1, 0, 1, 2):
        for y in (1, 2):
            s.set(cx + 4, y, z, 'raft:aquarium')
        s.set(cx + 4, 3, z, 'raft:neon_tube_cyan[axis=z]')
    for (tx, tz) in ((cx + 1, -3),):
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                for y in (1, 2, 3):
                    if dx == 0 and dz == 0 and y < 3:
                        s.set(tx + dx, y, tz + dz, 'minecraft:water')
                        s.dry.discard((tx + dx, y, tz + dz))
                    elif y < 3:
                        s.set(tx + dx, y, tz + dz, 'minecraft:glass')
                    else:
                        s.set(tx + dx, y, tz + dz, 'minecraft:smooth_stone_slab[type=bottom]')
        s.set(tx, 0, tz, 'minecraft:sea_lantern')
        s.set(tx, 1, tz, 'minecraft:sea_pickle[pickles=2]')
        s.force_wet.add((tx, 1, tz))
        s.set(tx, 3, tz, 'minecraft:glass')
        s.entity(tx + 0.5, 2.1, tz + 0.5, {'id': 'minecraft:pufferfish', 'PersistenceRequired': True,
                                           'FromBucket': True})
    # lab benches along the south-west, terminals helix_3 / helix_4 and Voss's desk with the lab chest
    for (x, z) in ((cx - 4, 1), (cx - 4, 2), (cx - 3, 3), (cx - 2, 4)):
        s.set(x, 1, z, 'minecraft:barrel[facing=up,open=false]')
    s.set(cx - 4, 2, 1, 'minecraft:brewing_stand')
    s.set(cx - 4, 2, 2, 'minecraft:flower_pot')
    s.set(cx - 3, 2, 3, 'minecraft:smooth_stone_slab[type=bottom]')
    terminal(s, cx - 4, 1, -1, 'east', 'helix_3')
    terminal(s, cx - 1, 1, -4, 'south', 'helix_4')
    s.set(cx - 2, 1, -3, 'raft:driftwood_chair[facing=north]')
    s.chest(cx, 1, -5, 'raft:chests/story/helix_lab', facing='south')
    s.set(cx - 1, 1, -5, 'minecraft:lectern[facing=south,has_book=false,powered=false]')
    s.set(cx + 1, 1, -5, 'minecraft:bookshelf')
    s.set(cx + 1, 2, -5, 'raft:jelly_lamp[hanging=false]')
    s.set(cx + 2, 1, 4, 'raft:chart_table[facing=north]')
    s.set(cx + 1, 1, 4, 'minecraft:cartography_table')
    s.set(cx + 3, 1, 3, 'raft:spyglass_stand[facing=west]')
    # specimen shelf: dead coral samples
    for (x, z, c) in ((cx - 3, -3, 'minecraft:dead_brain_coral_block'), (cx - 3, -2, 'minecraft:dead_tube_coral_block')):
        s.set(x, 1, z, c)
    for y, x in ((7, cx + 2), (7, cx - 2)):
        s.set(x, y - 1, 0, 'raft:jelly_lamp[hanging=true]')


def generator(s, rnd):
    zc = -19
    # energy core: Tidewarden relic stone inside a cage, pipes up to the roof
    s.set(0, 0, zc, 'raft:rusted_plate')
    for y in (1, 2, 3):
        s.set(0, y, zc, 'raft:glowing_tidestone' if y > 1 else 'raft:chiseled_tidestone')
    for (dx, dz) in ((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (1, -1), (-1, 1), (-1, -1)):
        for y in (1, 2, 3):
            s.set(dx, y, zc + dz, 'minecraft:iron_bars' if (dx == 0 or dz == 0) else 'raft:steel_beam[axis=y]')
        s.set(dx, 0, zc + dz, 'minecraft:yellow_terracotta' if (dx + dz) % 2 else 'minecraft:black_terracotta')
    for y in (4, 5):
        s.set(0, y, zc, 'raft:rusty_pipe[axis=y]')
    for x in range(-6, 7):
        if x != 0:
            s.set(x, 5, zc, 'raft:rusty_pipe[axis=x]' if x % 4 else 'raft:rusty_pipe_valve[axis=x]')
    for z in range(-24, -13):
        s.set(5, 5, z, 'raft:rusty_pipe[axis=z]' if z % 5 else 'raft:rusty_pipe_valve[axis=z]')
    # consoles (west) and server racks (east)
    for z in range(-23, -15):
        if z == -19:
            continue
        s.set(-6, 1, z, 'raft:control_panel[facing=east]')
        if z % 2 == 0:
            s.set(-5, 1, z, 'raft:driftwood_chair[facing=west]')
        s.set(6, 1, z, 'raft:server_rack[facing=west]')
        s.set(6, 2, z, 'raft:server_rack[facing=west]')
    s.set(-6, 2, -22, 'raft:neon_tube_cyan[axis=z]'); s.set(-6, 2, -21, 'raft:neon_tube_cyan[axis=z]')
    terminal(s, 2, 1, -24, 'south', 'helix_5')
    s.set(-3, 1, -14, 'minecraft:barrel[facing=up,open=false]')
    s.set(-4, 1, -14, 'raft:wooden_crate[facing=north,open=false]')
    s.set(-4, 2, -14, 'raft:wooden_crate[facing=north,open=false]')
    s.set(3, 1, -14, 'minecraft:anvil[facing=east]')
    s.set(4, 1, -14, 'minecraft:grindstone[face=floor,facing=north]')
    for (x, z) in ((-3, -22), (3, -22), (-3, -16), (3, -16)):
        s.set(x, 5, z, BULB if (x + z) % 2 else BULB_DIM)
    # warning floor stripe in front of the vault door
    for x in range(-2, 3):
        s.set(x, 0, -24, 'minecraft:yellow_terracotta' if x % 2 else 'minecraft:black_terracotta')
    # flooded wing door (west wall, closed – the room behind is under water)
    s.door(-7, 1, -19, DOOR, 'west')
    s.dry.add((-7, 1, -19)); s.dry.add((-7, 2, -19))
    s.set(-7, 3, -19, 'minecraft:iron_block')
    for z in (-18, -20):
        for y in (1, 2, 3):
            s.set(-7, y, z, 'minecraft:iron_block')


def vault(s, rnd):
    # heavy door: bulkhead in an iron frame, behind it a short passage through the double wall
    for x in (-1, 1):
        for y in (1, 2, 3):
            s.set(x, y, -25, 'minecraft:iron_block')
    s.set(0, 3, -25, 'minecraft:iron_block')
    s.set(0, 4, -25, 'minecraft:iron_block')
    s.door(0, 1, -25, DOOR, 'north')
    s.dry.add((0, 1, -25)); s.dry.add((0, 2, -25))
    s.air(0, 1, -26, 0, 2, -26)
    # vault room interior x -2..2, z -31..-27
    for x in range(-2, 3):
        for z in range(-31, -26):
            s.set(x, 0, z, 'minecraft:polished_deepslate' if (x + z) % 2 else 'raft:rusted_plate')
    s.set(0, 1, -31, 'minecraft:polished_deepslate')
    s.chest(0, 2, -31, 'raft:chests/story/helix_vault', facing='south')
    for x in (-1, 1):
        s.set(x, 1, -31, 'minecraft:iron_bars')
        s.set(x, 2, -31, 'minecraft:iron_bars')
    s.set(-2, 1, -31, 'raft:server_rack[facing=south]')
    s.set(-2, 2, -31, 'raft:server_rack[facing=south]')
    terminal(s, 2, 1, -29, 'west', 'helix_6')
    s.set(-2, 1, -28, 'raft:sigil_pedestal')
    s.set(0, 4, -29, BULB)
    s.set(2, 1, -31, 'minecraft:barrel[facing=up,open=false]')


def storage(s, rnd):
    # x -8..-3, z 8..12
    s.chest(-8, 1, 8, 'raft:chests/helix_storage', facing='east', block='minecraft:barrel')
    for (x, z) in ((-8, 9), (-8, 10), (-7, 8)):
        s.set(x, 1, z, 'minecraft:barrel[facing=up,open=false]')
    s.set(-8, 2, 8, 'minecraft:barrel[facing=up,open=false]')
    for (x, z, h) in ((-8, 12, 2), (-7, 12, 1), (-4, 12, 2), (-3, 12, 1)):
        for y in range(1, h + 1):
            s.set(x, y, z, 'raft:wooden_crate[facing=north,open=false]')
    terminal(s, -3, 1, 8, 'south', 'lab_misc_2')
    s.set(-6, 3, 10, BULB_DIM)
    s.set(-5, 2, 8, 'raft:driftwood_shelf[facing=south]')
    s.set(-4, 2, 8, 'raft:driftwood_shelf[facing=south]')


def flooded_wing(s, rnd):
    cx, cz = -18, -19
    # broken cap: knock holes into the glass (open to the sea) and crack panes
    for (x, y, z), b in list(s.blocks.items()):
        if rad(x, z, cx, cz) <= 6 and y >= 3 and b[0] in ('raft:aqua_sea_glass', 'raft:cracked_glass'):
            r = rnd.random()
            if (x - cx) > 0 and (z - cz) < 1 and r < 0.6:      # the north-east side caved in
                del s.blocks[(x, y, z)]
            elif r < 0.15:
                del s.blocks[(x, y, z)]
    # debris: fallen ribs, scattered crates and pipes
    for x in range(cx - 3, cx + 2):
        s.set(x, 1, cz + 2, 'raft:steel_beam[axis=x]')
    s.set(cx + 2, 1, cz - 1, 'raft:steel_beam[axis=z]'); s.set(cx + 2, 1, cz - 2, 'raft:steel_beam[axis=z]')
    s.set(cx - 2, 1, cz - 3, 'raft:wooden_crate[facing=east,open=false]')
    s.set(cx - 3, 1, cz - 2, 'minecraft:barrel[facing=up,open=false]')
    s.set(cx - 3, 2, cz - 2, 'raft:rusty_pipe[axis=x]')
    s.chest(cx - 4, 1, cz, 'raft:chests/helix_storage', facing='east')
    s.block_entity(cx + 1, 1, cz + 4, 'raft:research_terminal[facing=north]', 'raft:research_terminal',
                   {'LogId': 'lab_misc_1'})
    # overgrowth inside
    for (x, z, h) in ((cx - 1, cz - 1, 6), (cx + 1, cz + 1, 5), (cx - 3, cz + 3, 4), (cx, cz - 4, 6)):
        s.kelp(x, 1, z, h)
    for (x, z) in ((cx - 2, cz), (cx, cz + 1), (cx + 3, cz), (cx - 1, cz - 3), (cx + 2, cz + 3), (cx - 4, cz - 2)):
        if s.on_top(x, 1, z):
            s.seagrass(x, 1, z, tall=(x + z) % 2 == 0)
    for (x, z, c) in ((cx + 3, cz + 2, 'raft:sea_anemone_red'), (cx - 2, cz + 4, 'raft:sea_anemone_purple'),
                      (cx + 1, cz - 2, 'raft:giant_clam[facing=south,open=true,pearl=false]'),
                      (cx - 4, cz + 2, 'minecraft:sea_pickle[pickles=3]'),
                      (cx + 4, cz - 1, 'raft:sea_urchin')):
        if s.on_top(x, 1, z):
            s.set(x, 1, z, c)
    s.grow_barnacles(((cx - 6, 1, cz - 6), (cx + 6, 7, cz + 6)), 0.16, seed=11)
    # moray eels hiding in the wreckage
    for (x, z) in ((cx - 2, cz + 1), (cx + 2, cz - 3)):
        s.entity(x + 0.5, 1.2, z + 0.5, {'id': 'raft:moray_eel', 'PersistenceRequired': True})
    # flooded tunnel: half the portholes broken, rust
    for x in range(-12, -7):
        for dz in (-2, 2):
            if s.name_at(x, 2, -19 + dz) == 'raft:porthole' and rnd.random() < 0.5:
                s.set(x, 2, -19 + dz, 'raft:cracked_glass')


def exterior(s, rnd):
    (x0, y0, z0), (x1, y1, z1) = s.bounds()
    # rust on the lower hull, heavier on the flooded wing
    for (x, y, z), b in list(s.blocks.items()):
        if b[0] == 'raft:riveted_hull' and y >= 1 and (x, y, z) not in s.dry:
            exposed = any((x + dx, y, z + dz) not in s.blocks for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            p = 0.45 if rad(x, z, -18, -19) < 8 or (-13 <= x <= -7 and -21 <= z <= -17) else (0.22 if y <= 2 else 0.1)
            if exposed and rnd.random() < p:
                s.set(x, y, z, 'raft:rusty_hull')
    # antenna mast on the generator roof
    for y in range(7, 13):
        s.set(4, y, -16, 'raft:steel_beam[axis=y]')
    s.set(4, 13, -16, 'minecraft:lightning_rod[facing=up,powered=false]')
    for (dx, y) in ((-1, 11), (1, 11)):          # (not right above the buoy lamp, which reaches into that block)
        s.set(4 + dx, y, -16, 'minecraft:lightning_rod[facing=up,powered=false]')
    s.set(5, 9, -16, 'raft:buoy_red')
    # vent stacks and a roof hatch on the generator
    for (x, z) in ((-4, -22), (-4, -16)):
        s.set(x, 7, z, 'raft:rusty_pipe[axis=y]')
        s.set(x, 8, z, 'raft:rusty_pipe_valve[axis=y]' if z == -22 else 'raft:rusty_pipe[axis=y]')
        s.set(x, 9, z, 'minecraft:lightning_rod[facing=up,powered=false]')
    s.set(0, 7, -20, 'raft:metal_hatch[facing=north,half=bottom,open=false,powered=false]')
    s.set(1, 7, -20, 'raft:metal_hatch[facing=north,half=bottom,open=false,powered=false]')
    # roof railing on the generator
    for x in range(-7, 8):
        for z in (-25, -13):
            s.set(x, 7, z, 'raft:steel_railing')
    for z in range(-24, -13):
        for x in (-7, 7):
            s.set(x, 7, z, 'raft:steel_railing')
    # external pipes along the tunnels and a cable bundle to the lab
    for x in range(7, 13):
        s.set(x, 1, 3, 'raft:rusty_pipe[axis=x]') if x not in (9, 10, 11, 12) else None
    for x in range(-13, -6):
        s.set(x, 1, -3, 'raft:rusty_pipe[axis=x]')
    for z in range(-12, -6):
        s.set(3, 1, z, 'raft:rusty_pipe[axis=z]')
    # floodlights set into the pad around every module
    for (cx, cz, r) in ((0, 0, 8.6), (19, 0, 8.0), (-18, -19, 7.0)):
        for a in (45, 135, 225, 315):
            x = round(cx + math.cos(math.radians(a)) * r)
            z = round(cz + math.sin(math.radians(a)) * r)
            if s.name_at(x, 0, z) not in (None, 'minecraft:water') and s.name_at(x, 1, z) is None:
                s.set(x, 0, z, 'minecraft:sea_lantern')
    for (x, z) in ((-8, -26), (8, -26), (-8, -12), (8, -12), (-28, -6), (-28, 6), (-13, 6), (-10, 14)):
        if s.name_at(x, 0, z) not in (None, 'minecraft:water') and s.name_at(x, 1, z) is None:
            s.set(x, 0, z, 'minecraft:sea_lantern')
    # hub dome beacon
    s.set(0, 9, 0, 'minecraft:lightning_rod[facing=up,powered=false]')
    # sea floor life on the pad: kelp, seagrass, anemones, clams, starfish on the hulls
    cands = [(x, 1, z) for x in range(x0, x1 + 1) for z in range(z0, z1 + 1) if s.on_top(x, 1, z)]
    rnd.shuffle(cands)
    for i, (x, y, z) in enumerate(cands[:160]):
        r = rnd.random()
        if r < 0.30:
            s.seagrass(x, y, z, tall=r < 0.12)
        elif r < 0.42:
            s.kelp(x, y, z, 3 + int(rnd.random() * 6))
        elif r < 0.52:
            s.set(x, y, z, pick(rnd, [('raft:sea_anemone_red', 2), ('raft:sea_anemone_purple', 2),
                                      ('raft:sea_anemone_glow', 1)]))
        elif r < 0.58:
            s.set(x, y, z, f'raft:giant_clam[facing={rnd.choice(["north", "south", "east", "west"])},open=false,'
                           f'pearl=false]')
        elif r < 0.66:
            s.set(x, y, z, f'minecraft:sea_pickle[pickles={1 + int(rnd.random() * 4)}]')
        elif r < 0.72:
            s.set(x, y, z, f'raft:seashells[shells={1 + int(rnd.random() * 3)}]')
    # starfish on walls
    n = 0
    for (x, y, z), b in list(s.blocks.items()):
        if n > 24 or y not in (1, 2) or b[0] not in ('raft:riveted_hull', 'raft:rusty_hull'):
            continue
        for dx, dz, f in ((1, 0, 'east'), (-1, 0, 'west'), (0, 1, 'south'), (0, -1, 'north')):
            if s.wet_free(x + dx, y, z + dz) and rnd.random() < 0.04:
                s.set(x + dx, y, z + dz, f'raft:starfish_{rnd.choice(["orange", "purple", "red"])}'
                                         f'[face=wall,facing={f}]')
                n += 1
                break
    s.grow_barnacles(((x0, 1, z0), (x1, 4, z1)), 0.08, seed=5, support=lambda n: 'hull' in n or 'concrete' in n)


def verify(s):
    rep = {}
    terms = sorted((b[2]['LogId'], p) for p, b in s.blocks.items() if b[2] and b[2].get('id') == 'raft:research_terminal')
    rep['terminals'] = terms
    rep['chests'] = sorted((b[2]['LootTable'], p) for p, b in s.blocks.items() if b[2] and 'LootTable' in b[2])
    rep['doors'] = sorted(p for p, b in s.blocks.items() if b[0] == DOOR and b[1].get('half') == 'lower')
    rep['dry_blocks'] = len(s.dry)
    rep['leaks'] = leak_check(s)[:10]
    rep['entities'] = [e[3]['id'] for e in s.entities]
    return rep


def write_json_files():
    structure_json(SID, f'raft:{SID}/start', 'surface_structures', 'beard_thin', -2, 'OCEAN_FLOOR_WG', size=1,
                   max_distance=80, spawn_overrides={
                       'monster': spawns([('raft:moray_eel', 3, 1, 1), ('minecraft:drowned', 1, 1, 2)]),
                       'water_ambient': spawns([('raft:moray_eel', 2, 1, 1), ('minecraft:cod', 6, 2, 4),
                                                ('minecraft:tropical_fish', 4, 2, 4)]),
                   })
    structure_set(SID, 28, 10, 31415960)
    template_pool(f'raft:{SID}/start', [(f'raft:{SID}/station_0', 1, f'raft:{SID}')])
    processor_list(f'raft:{SID}', [NO_SPILL])
    biome_tag(SID, ['coral_shallows', 'glowing_lagoon', 'kelp_forest'])


def main():
    s = build()
    s.save(MODULE, f'{SID}/station_0')
    write_json_files()
    return s


def preview(s):
    return pv(s, 'helix', cuts=(1, 3), slices_y=[-2, 0, 1, 2, 3, 5, 7])
