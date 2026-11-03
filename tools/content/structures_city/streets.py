"""Street pieces of Port Solace: avenues and streets (with building lots), crossroads, T-junctions, dead ends.

Every street is 13 wide (x 0..12): sidewalk 0..2, road 3..9 (lane line at x 6), sidewalk 10..12. Street jigsaws sit
in the middle of the road ends (x 6, y GY), lot jigsaws on the outer sidewalk edges.
"""
import random

from .citylib import (ASPHALT, FOUND, FY, GY, H4, NS, PAVE_BASE, PAVE_SLAB, RUBBLE, City, bench, bus, bus_stop,
                      car, fallen_lamp, hydrant, lamp_post, manhole, newspaper_box, phone_booth, road_sign,
                      rubble_pile, scatter_growth, slab, stair, traffic_light, tree_pit)

W = 13
CLEAR = 9   # water above the street up to GY + CLEAR


def strip(s, z0, z1, rnd, x0=0, x1=W - 1, line=True):
    """Road cross-section between z0..z1 (inclusive)."""
    for z in range(z0, z1 + 1):
        for x in range(x0, x1 + 1):
            s.pfill(x, 0, z, x, 1, z, FOUND, rnd)
            if 3 <= x <= 9:
                if line and x == 6 and (z // 2) % 2 == 0:
                    s.set(x, GY, z, 'raft:asphalt_line[axis=z]')
                else:
                    s.pset(x, GY, z, ASPHALT, rnd)
                for y in range(GY + 1, GY + CLEAR + 1):
                    s.set(x, y, z, 'minecraft:water')
            else:
                s.pset(x, GY, z, PAVE_BASE, rnd)
                curb = x in (2, 10)
                s.set(x, FY, z, slab('minecraft:polished_andesite_slab') if curb else pick_slab(rnd))
                for y in range(FY + 1, GY + CLEAR + 1):
                    s.set(x, y, z, 'minecraft:water')


def pick_slab(rnd):
    from .common import pick
    return pick(rnd, PAVE_SLAB)


def potholes(s, z0, z1, rnd, n=3):
    for _ in range(n):
        x = rnd.randint(4, 8)
        z = rnd.randint(z0 + 1, z1 - 1)
        for dx, dz in ((0, 0), (1, 0), (0, 1), (1, 1)):
            if rnd.random() < 0.8 and s.is_name(x + dx, GY, z + dz, 'asphalt'):
                s.set(x + dx, GY, z + dz, 'minecraft:water')
                s.set(x + dx, GY - 1, z + dz, 'minecraft:gravel' if rnd.random() < 0.6 else 'minecraft:sand')


def street_ends(s, length, pool):
    s.jig(6, GY, 0, 'north', 'street', 'street', pool, 'raft:asphalt', sel=1)
    s.jig(6, GY, length - 1, 'south', 'street', 'street', pool, 'raft:asphalt', sel=1)


def lots(s, z, west_pool, east_pool):
    if west_pool:
        s.jig(0, GY, z, 'west', 'lot', 'front', west_pool, 'minecraft:stone', sel=2, prio=1)
    if east_pool:
        s.jig(W - 1, GY, z, 'east', 'lot', 'front', east_pool, 'minecraft:stone', sel=2, prio=1)


def sidewalk_props(s, length, rnd, lamps=10, extras=True):
    """Lamps (alternating sides), trees, hydrants, benches, newspaper boxes."""
    side = 0
    for z in range(4, length - 3, lamps):
        x = 1 if side == 0 else 11
        arm = 'east' if side == 0 else 'west'
        broken = rnd.random() < 0.2
        lamp_post(s, x, z, arm=arm, y=FY + 1 if False else FY, h=5, broken=broken,
                  kind='lantern' if rnd.random() < 0.7 else 'sea')
        side ^= 1
    if not extras:
        return
    for z in range(2, length - 2):
        for x in (1, 11):
            if not s.free(x, FY + 1, z) or not s.free(x, FY + 1, z - 1) or not s.free(x, FY + 1, z + 1):
                continue
            if s.is_name(x, FY, z, 'wall') or s.is_name(x, FY, z - 1, 'wall') or s.is_name(x, FY, z + 1, 'wall'):
                continue
            r = rnd.random()
            if r < 0.05:
                tree_pit(s, x, FY, z, rnd, dead=rnd.random() < 0.5)
            elif r < 0.08:
                hydrant(s, x - 1 if x == 1 else x + 1, FY + 1, z)
            elif r < 0.10:
                newspaper_box(s, x, FY + 1, z, rnd.choice(['blue', 'yellow', 'red']))


def vehicles(s, length, rnd, n_cars=2, bus_at=None):
    taken = set()
    if bus_at is not None:
        lane_x = 3 if rnd.random() < 0.5 else 7
        bus(s, lane_x, GY + 1, bus_at, along='z', rnd=rnd, length=min(11, length - bus_at - 1))
        taken |= set(range(bus_at - 1, bus_at + 12))
    for _ in range(n_cars):
        for _try in range(10):
            z = rnd.randint(1, length - 6)
            if any(zz in taken for zz in range(z - 1, z + 6)):
                continue
            lane_x = rnd.choice([3, 7, 4, 6])
            car(s, lane_x, GY + 1, z, along='z', rnd=rnd, wreck=0.35, flipped=rnd.random() < 0.15)
            taken |= set(range(z, z + 5))
            break


def finish_street(s, length, rnd, growth=True):
    if growth:
        scatter_growth(s, ((0, GY + 1, 0), (W - 1, GY + 4, length - 1)), rnd, kelp=0.008, grass=0.10,
                       pickles=0.012, kelp_h=(5, 16))
        s.grow_barnacles(((0, GY + 1, 0), (W - 1, GY + 7, length - 1)), 0.25, rnd.randint(0, 9999),
                         support=lambda n: 'concrete' in n or 'terracotta' in n or 'plate' in n or 'deepslate' in n)


# ------------------------------------------------------------------------------------------------ pieces
def straight(name, length, seed, west_pool, east_pool, end_pool, bus_at=None, n_cars=2, stop=False, fallen=False,
             lot_z=None):
    rnd = random.Random(seed)
    s = City(name, seed)
    strip(s, 0, length - 1, rnd)
    potholes(s, 0, length - 1, rnd, n=3)
    sidewalk_props(s, length, rnd)
    if stop:
        bus_stop(s, 12, FY + 1, length // 2 + 4, along='z', facing='west', length=4)
    vehicles(s, length, rnd, n_cars=n_cars, bus_at=bus_at)
    for z in range(5, length - 5, 9):
        if rnd.random() < 0.5:
            manhole(s, 5, GY, z)
    if fallen:
        fallen_lamp(s, 4, GY + 1, length - 8, along='x', length=5)
    road_sign(s, 1, FY + 1, 1, 'north', rnd.choice(['red', 'blue', 'white']))
    finish_street(s, length, rnd)
    street_ends(s, length, end_pool)
    lots(s, lot_z if lot_z is not None else length // 2, west_pool, east_pool)
    return s.done()


def crossing(name, seed, exits=('north', 'east', 'south', 'west'), pool='streets', closed_lot=None):
    """13x13 junction; sides without exit get a sidewalk and (optionally) a lot."""
    rnd = random.Random(seed)
    s = City(name, seed)
    for x in range(W):
        for z in range(W):
            s.pfill(x, 0, z, x, 1, z, FOUND, rnd)
            corner = (x < 3 or x > 9) and (z < 3 or z > 9)
            closed = (('west' not in exits and x < 3) or ('east' not in exits and x > 9)
                      or ('north' not in exits and z < 3) or ('south' not in exits and z > 9))
            if corner or closed:
                s.pset(x, GY, z, PAVE_BASE, rnd)
                s.set(x, FY, z, pick_slab(rnd))
                for y in range(FY + 1, GY + CLEAR + 1):
                    s.set(x, y, z, 'minecraft:water')
            else:
                zebra = False
                if z < 3 or z > 9:
                    zebra = x % 2 == 1
                    st = 'raft:asphalt_line[axis=z]' if zebra else None
                elif x < 3 or x > 9:
                    zebra = z % 2 == 1
                    st = 'raft:asphalt_line[axis=x]' if zebra else None
                if zebra and rnd.random() < 0.85:
                    s.set(x, GY, z, st)
                else:
                    s.pset(x, GY, z, ASPHALT, rnd)
                for y in range(GY + 1, GY + CLEAR + 1):
                    s.set(x, y, z, 'minecraft:water')
    # curb slabs on the corner edges
    manhole(s, 6, GY, 6)
    corners = [(1, 1, 'south'), (11, 1, 'west'), (11, 11, 'north'), (1, 11, 'east')]
    for i, (x, z, face) in enumerate(corners):
        if rnd.random() < 0.75:
            traffic_light(s, x, FY + 1, z, face)
        elif rnd.random() < 0.5:
            lamp_post(s, x, z, arm=face, y=FY, h=5, broken=True)
    if 'west' not in exits:
        bench(s, 1, FY + 1, 5, 'east', length=3, along='z')
    if rnd.random() < 0.6:
        car(s, 4, GY + 1, 3, along='x' if rnd.random() < 0.5 else 'z', rnd=rnd, wreck=0.4)
    rubble_pile(s, rnd.randint(4, 8), GY + 1, rnd.randint(4, 8), 1, rnd)
    scatter_growth(s, ((0, GY + 1, 0), (W - 1, GY + 4, W - 1)), rnd, kelp=0.03, grass=0.1, pickles=0.01)
    s.grow_barnacles(((0, GY + 1, 0), (W - 1, GY + 7, W - 1)), 0.25, seed, support=lambda n: 'blackstone' in n)
    for f in exits:
        x, z = {'north': (6, 0), 'south': (6, W - 1), 'east': (W - 1, 6), 'west': (0, 6)}[f]
        s.jig(x, GY, z, f, 'street', 'street', pool, 'raft:asphalt', sel=1)
    if closed_lot:
        missing = [f for f in ('north', 'east', 'south', 'west') if f not in exits][0]
        x, z = {'north': (6, 0), 'south': (6, W - 1), 'east': (W - 1, 6), 'west': (0, 6)}[missing]
        s.jig(x, GY, z, missing, 'lot', 'front', closed_lot, 'minecraft:stone', sel=2, prio=1)
    return s.done()


def dead_end(name, seed, kind='barricade'):
    """9 long end piece: the street stops at a barricade or a sinkhole."""
    rnd = random.Random(seed)
    s = City(name, seed)
    L = 10
    strip(s, 0, L - 1, rnd, line=False)
    # sidewalk wraps around the end
    for x in range(W):
        for z in range(L - 3, L):
            s.pset(x, GY, z, PAVE_BASE, rnd)
            s.set(x, FY, z, pick_slab(rnd))
    if kind == 'barricade':
        for x in range(3, 10):
            s.set(x, GY + 1, 5, 'minecraft:oak_fence')
            s.set(x, GY + 2, 5, 'minecraft:red_terracotta' if x % 2 else 'minecraft:white_terracotta')
        for x in (2, 10):
            s.set(x, FY + 1, 5, 'minecraft:andesite_wall')
        for x in range(3, 10, 2):
            s.set(x, GY + 1, 7, 'minecraft:smooth_stone')
            s.set(x, GY + 2, 7, slab('minecraft:smooth_stone_slab'))
        car(s, 4, GY + 1, 1, along='x', rnd=rnd, wreck=0.5)
        road_sign(s, 1, FY + 1, 4, 'north', 'red')
        rubble_pile(s, 9, FY + 1, 8, 2, rnd)
    else:
        # sinkhole: the road broke into a crater, kelp grows out of it
        cx, cz = 6, 5
        for x in range(1, 12):
            for z in range(1, L):
                d = ((x - cx) ** 2 + (z - cz) ** 2) ** 0.5
                if d < 4.2 + rnd.random():
                    depth = 2 if d < 2.5 else 1
                    for y in range(GY - depth + 1, GY + 1):
                        s.set(x, y, z, 'minecraft:water')
                    s.set(x, GY - depth, z, 'minecraft:gravel' if rnd.random() < 0.5 else 'minecraft:mud')
                    if rnd.random() < 0.35:
                        s.kelp(x, GY - depth + 1, z, rnd.randint(6, 18), glow=rnd.random() < 0.3)
                elif d < 5.5 and rnd.random() < 0.5:
                    s.set(x, GY, z, slab('raft:cracked_concrete_slab', 'top'))
        for x in (2, 10):
            s.set(x, FY + 1, 1, 'minecraft:andesite_wall')
        lamp_post(s, 11, L - 2, arm='west', y=FY, h=4, broken=True)
        fallen_lamp(s, 2, FY + 1, L - 2, along='x', length=4, light=False)
    scatter_growth(s, ((0, GY - 3, 0), (W - 1, GY + 4, L - 1)), rnd, kelp=0.03, grass=0.12, pickles=0.02)
    s.jig(6, GY, 0, 'north', 'street', 'street', 'minecraft:empty', 'raft:asphalt', sel=1)
    return s.done()


def build_all():
    out = {}
    out['avenue_power'] = straight('port_solace/avenue_power', 31, 1101, 'lots_deco', 'lots_mid', 'junctions',
                                   bus_at=None, n_cars=3, stop=True)
    out['avenue_harbour'] = straight('port_solace/avenue_harbour', 31, 1102, 'lots_glass', 'lots_mid', 'junctions',
                                     bus_at=12, n_cars=1, fallen=True)
    out['avenue_market'] = straight('port_solace/avenue_market', 31, 1103, 'buildings', 'buildings', 'junctions',
                                    n_cars=3, stop=True)
    out['street_a'] = straight('port_solace/street_a', 25, 1201, 'buildings', 'buildings', 'ends', n_cars=2)
    out['street_b'] = straight('port_solace/street_b', 25, 1202, 'buildings', 'small_lots', 'ends', n_cars=1,
                               fallen=True)
    out['crossroad'] = crossing('port_solace/crossroad', 1301)
    out['tee'] = crossing('port_solace/tee', 1302, exits=('north', 'east', 'south'), closed_lot='buildings')
    out['end_barricade'] = dead_end('port_solace/end_barricade', 1401, 'barricade')
    out['end_sinkhole'] = dead_end('port_solace/end_sinkhole', 1402, 'sinkhole')
    return out
