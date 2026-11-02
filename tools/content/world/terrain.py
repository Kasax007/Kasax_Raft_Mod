"""Terrain of the raft dimension ("The Endless Sea"): noises, density functions, noise settings and the multi-noise
biome parameters. See docs/design/beachcraft3.md, section 4.1/4.2.

How it works
- Six cheap 2D noises drive everything (all `raft:raftdim/<name>`):
  continents (islands + archipelago regions), erosion (flat drowned shelves / flat mangrove isles),
  ridges (long abyssal trenches where |ridges| is small), temperature (cold frost-floe seas, wreck graveyards),
  vegetation (volcanic / palm / jungle islands), relief (rolling sea floor and island hills) + one 3D detail noise.
- `continents` = continents noise minus a penalty in cold regions (no islands in the frost floes). The router uses
  exactly these functions for the biomes, so island biomes sit on land, shallows ring the islands, trench biomes sit
  in the trenches and so on.
- `height` is a 2D spline over continents with nested splines (erosion, |ridges|, vegetation) that gives the floor /
  land height per zone; `surface` = height + relief amplitude * relief noise.
- final density = squeeze(interpolated((surface - y) / 8 + detail)) + beardifier. With the small detail amplitude
  there are no floating overhangs; structures with terrain adaptation still work (beardifier).
"""
from collections import OrderedDict

NS = 'raft'
P = 'raftdim/'


def rid(name):
    return f'{NS}:{P}{name}'


# --------------------------------------------------------------------------------------------- noises
def _octaves(base_octave, mods):
    """Noise params with base_amplitude chosen so that the standard deviation is 1/3 (value range about -1..1)."""
    n = len(mods)
    w = [2 ** (n - 1) / (2 ** n - 1) * 0.5 ** i for i in range(n)]
    s = sum(wi * mi for wi, mi in zip(w, mods))
    return OrderedDict(base_amplitude=round(1.0 / s, 6), base_octave=base_octave, octave_count=n,
                       amplitude_modifiers=[float(m) for m in mods])


NOISES = {
    # wavelengths 2048 .. 32 blocks: archipelago regions, islands (256-512), coast detail
    'continents': _octaves(-11, [0.35, 0.3, 1.6, 8.0, 7.2, 6.4, 5.12]),
    # flat shelves / flat mangrove isles, 1024-256
    'erosion': _octaves(-10, [1.0, 0.55, 0.25]),
    # trenches follow the zero line of this noise, 1024-256 (smooth, long winding canyons)
    'ridges': _octaves(-10, [1.0, 0.35, 0.12]),
    # climate, very large (cold seas, wreck graveyards)
    'temperature': _octaves(-11, [1.0, 0.6, 0.3]),
    # island flavour (volcanic / palm / jungle), 2048-512
    'vegetation': _octaves(-11, [1.0, 0.6, 0.3]),
    # rolling sea floor and island hills, 128-32
    'relief': _octaves(-7, [1.0, 0.6, 0.3]),
    # 3D surface detail, 32-16 blocks
    'detail': _octaves(-5, [1.0, 0.5]),
}

# --------------------------------------------------------------------------------------------- thresholds
# Continents thresholds were chosen with sim.py so that the zone shares match the spec (section 4.1):
# islands ~15 %, shallows ~12 %, mid sea ~15 %, the rest deep (open sea, shelf, trench).
C_MID = 0.05      # deep -> mid sea (kelp)
C_SH = 0.205      # mid -> shallows (reef edge)
C_BEACH = 0.34    # shallows -> land (waterline)
C_TB = C_MID - 0.07   # trench biome only below this (trenches fade out towards the mid sea)
T_COLD = -0.37    # colder: frost floes
T_WRECK = 0.40    # warmer deep sea: wreck graveyard
V_VOLC = -0.42    # drier: volcanic isles
V_JUNGLE = 0.15   # wetter: jungle isles
E_SHELF = 0.32    # erosion above: drowned shelf (deep) / mangrove isle (land)
W_TRENCH = 0.075  # |ridges| below: abyssal trench (deep only)
W_GLOW = 0.38     # ridges above: glowing lagoon (shallows only)


# --------------------------------------------------------------------------------------------- DF helpers
def noise(name, xz=1.0, y=0.0):
    return OrderedDict(type='minecraft:noise', noise=rid(name), xz_scale=xz, y_scale=y)


def op(t, a, b):
    return OrderedDict(type='minecraft:' + t, left=a, right=b)


def add(a, b):
    return op('add', a, b)


def mul(a, b):
    return op('mul', a, b)


def cache(x):
    return OrderedDict(type='minecraft:cache', input=x)


def unary(t, x):
    return OrderedDict(type='minecraft:' + t, input=x)


def interpolated(x, xz, y):
    return OrderedDict(type='minecraft:interpolated', cell_size_xz=xz, cell_size_y=y, input=x)


def _monotone_ders(locs, vals):
    """Fritsch-Carlson style derivatives for numeric points (no overshoot); nested points get 0."""
    n = len(locs)
    ders = [0.0] * n
    for i in range(n):
        if not all(isinstance(vals[j], (int, float)) for j in (max(0, i - 1), i, min(n - 1, i + 1))):
            continue
        if i == 0 or i == n - 1:
            continue
        s0 = (vals[i] - vals[i - 1]) / (locs[i] - locs[i - 1])
        s1 = (vals[i + 1] - vals[i]) / (locs[i + 1] - locs[i])
        if s0 * s1 <= 0:
            continue
        ders[i] = 2 * s0 * s1 / (s0 + s1)
    return ders


def spline(coord, points, ders=None):
    """CubicSpline object: points = [(location, value)], value = number or another spline() object."""
    locs = [p[0] for p in points]
    vals = [p[1] for p in points]
    if ders is None:
        ders = _monotone_ders(locs, vals)
    return OrderedDict(coordinate=coord, points=[
        OrderedDict(derivative=round(float(d), 4), location=round(float(l), 4),
                    value=(round(float(v), 4) if isinstance(v, (int, float)) else v))
        for l, v, d in zip(locs, vals, ders)])


def spline_df(coord, points, ders=None):
    return OrderedDict(type='minecraft:spline', spline=spline(coord, points, ders))


# --------------------------------------------------------------------------------------------- functions
CONT, EROS, RIDG, TEMP, VEG = rid('continents'), rid('erosion'), rid('ridges'), rid('temperature'), rid('vegetation')
ABS_RIDGES = unary('abs', RIDG)


def lerp(alpha, first, second):
    return OrderedDict(type='minecraft:lerp', alpha=alpha, first=first, second=second)


# Island types: blend weights (0..1) from vegetation / erosion; the biome boundaries are in the middle of the ramps.
A_VOLC = spline_df(VEG, [(V_VOLC - 0.04, 1.0), (V_VOLC + 0.03, 0.0)], ders=[0.0, 0.0])
A_MANG = spline_df(EROS, [(E_SHELF - 0.05, 0.0), (E_SHELF + 0.02, 1.0)], ders=[0.0, 0.0])
A_JUNG = spline_df(VEG, [(V_JUNGLE - 0.06, 0.0), (V_JUNGLE + 0.06, 1.0)], ders=[0.0, 0.0])
A_LAND = spline_df(rid('continents'), [(C_BEACH - 0.03, 0.0), (C_BEACH, 1.0)], ders=[0.0, 0.0])


def per_zone(deep_trench, deep_trench_wall, deep_rim, deep_floor, shelf, mid_edge, mid, reef_low, reef, shallow_top,
             beach, isl):
    """One 2D function of the zones. The sea part is a spline over continents with nested splines (erosion: shelf,
    |ridges|: trench); the land part blends four island profiles (palm, jungle, mangrove, volcanic), each a spline
    over continents given as [(offset above C_BEACH, value)], with the type weights above."""
    trench = spline(ABS_RIDGES, [(0.035, deep_trench), (0.062, deep_trench_wall), (0.085, deep_rim),
                                 (0.12, deep_floor)])
    deep_core = spline(EROS, [(E_SHELF - 0.05, trench), (E_SHELF, shelf)])
    deep_edge = spline(EROS, [(E_SHELF - 0.05, deep_floor), (E_SHELF, shelf)])
    sea = spline_df(CONT, [(C_MID - 0.13, deep_core), (C_MID - 0.035, deep_edge), (C_MID, mid_edge),
                           (C_SH - 0.03, mid), (C_SH - 0.008, reef_low), (C_SH + 0.004, reef),
                           (C_BEACH - 0.03, shallow_top), (C_BEACH, beach)])

    def prof(kind):
        return spline_df(CONT, [(C_BEACH - 0.03, shallow_top), (C_BEACH, beach)] +
                         [(C_BEACH + o, v) for o, v in isl[kind]])

    land = lerp(A_VOLC, lerp(A_MANG, lerp(A_JUNG, prof('palm'), prof('jungle')), prof('mangrove')), prof('volcanic'))
    return lerp(A_LAND, sea, land)


CRATER_Y = 164   # flat crater floor of the big volcanoes (top solid block y 163, lava lake there)

HEIGHT = per_zone(
    deep_trench=7, deep_trench_wall=24, deep_rim=42, deep_floor=47, shelf=47, mid_edge=57, mid=69, reef_low=76,
    reef=84, shallow_top=95, beach=100.4,
    isl={'palm':     [(0.03, 102.4), (0.08, 104.5), (0.15, 108), (0.23, 113), (0.30, 117), (0.38, 121), (0.5, 125)],
         'jungle':   [(0.03, 102.2), (0.08, 105.5), (0.15, 112), (0.23, 121), (0.30, 128), (0.38, 134), (0.5, 140)],
         'mangrove': [(0.02, 100.9), (0.08, 101.3), (0.15, 101.7), (0.23, 102.1), (0.30, 102.5), (0.5, 103.5)],
         'volcanic': [(0.03, 102.6), (0.10, 107), (0.20, 116), (0.28, 131), (0.34, 156), (0.385, 181),
                      (0.41, 173), (0.435, CRATER_Y)]})

RELIEF_AMP = per_zone(
    deep_trench=3, deep_trench_wall=6, deep_rim=10, deep_floor=13, shelf=1.2, mid_edge=11, mid=11, reef_low=7, reef=6,
    shallow_top=3, beach=0.0,
    isl={'palm':     [(0.03, 0.3), (0.08, 1.5), (0.15, 3.5), (0.23, 5.5), (0.30, 6.5), (0.5, 7)],
         'jungle':   [(0.03, 0.3), (0.08, 2.0), (0.15, 5.0), (0.23, 8.5), (0.30, 10), (0.5, 11)],
         'mangrove': [(0.02, 0.6), (0.08, 2.2), (0.15, 2.6), (0.5, 2.6)],
         'volcanic': [(0.03, 0.3), (0.10, 2.0), (0.20, 4.0), (0.28, 4.0), (0.34, 2.0), (0.385, 0.5), (0.42, 0.0)]})

DETAIL_AMP = per_zone(
    deep_trench=0.25, deep_trench_wall=0.55, deep_rim=0.3, deep_floor=0.12, shelf=0.03, mid_edge=0.14, mid=0.16,
    reef_low=0.25, reef=0.2, shallow_top=0.08, beach=0.0,
    isl={'palm':     [(0.03, 0.0), (0.08, 0.05), (0.15, 0.12), (0.23, 0.16), (0.5, 0.18)],
         'jungle':   [(0.03, 0.0), (0.08, 0.08), (0.15, 0.18), (0.23, 0.24), (0.5, 0.26)],
         'mangrove': [(0.02, 0.0), (0.08, 0.02), (0.5, 0.03)],
         'volcanic': [(0.03, 0.0), (0.10, 0.08), (0.20, 0.2), (0.30, 0.25), (0.36, 0.2), (0.39, 0.05),
                      (0.42, 0.0)]})

# Cold seas have no islands: subtract up to 0.6 from the continents value where temperature < T_COLD.
COLD_PENALTY = spline_df(TEMP, [(T_COLD - 0.06, -0.6), (T_COLD + 0.06, 0.0)], ders=[0.0, 0.0])

FUNCTIONS = OrderedDict()
FUNCTIONS['continents_base'] = cache(noise('continents'))
FUNCTIONS['continents'] = cache(add(rid('continents_base'), COLD_PENALTY))
FUNCTIONS['erosion'] = cache(noise('erosion'))
FUNCTIONS['ridges'] = cache(noise('ridges'))
FUNCTIONS['temperature'] = cache(noise('temperature'))
FUNCTIONS['vegetation'] = cache(noise('vegetation'))
FUNCTIONS['height'] = cache(HEIGHT)
FUNCTIONS['relief_amplitude'] = cache(RELIEF_AMP)
FUNCTIONS['detail_amplitude'] = cache(DETAIL_AMP)
# top of the terrain (2D): floor of the sea / height of the land
FUNCTIONS['surface'] = cache(add(rid('height'), mul(rid('relief_amplitude'), noise('relief'))))
# density: positive below the surface, 1/8 per block, plus a gentle 3D detail
FUNCTIONS['base_density'] = add(mul(add(rid('surface'), mul('minecraft:y', -1.0)), 0.125),
                                mul(rid('detail_amplitude'), noise('detail', 1.0, 1.0)))
FUNCTIONS['final_density'] = add(unary('squeeze', interpolated(rid('base_density'), 4, 8)),
                                 OrderedDict(type='minecraft:beardifier'))
# a safe lower bound of the surface for the material (surface) rules
FUNCTIONS['chunk_surface_level'] = interpolated(add(rid('surface'), -16.0), 16, 1)


def noise_settings():
    return OrderedDict([
        ('debug_functions', [
            {'function': rid('final_density'), 'label': 'N'},
            {'function': rid('continents'), 'label': 'C'},
            {'function': rid('erosion'), 'label': 'E'},
            {'function': rid('ridges'), 'label': 'W'},
            {'function': rid('temperature'), 'label': 'T'},
            {'function': rid('vegetation'), 'label': 'V'},
            {'function': rid('surface'), 'label': 'S'},
        ]),
        ('default_block', 'minecraft:stone'),
        ('default_fluid', 'minecraft:water'),
        ('disable_mob_generation', False),
        ('legacy_random_source', False),
        ('material_rule', 'raft:raftdim'),
        ('noise', {'height': 384, 'min_y': -64}),
        ('noise_router', OrderedDict([
            ('chunk_surface_level', rid('chunk_surface_level')),
            ('continents', rid('continents')),
            ('depth', 0.0),
            ('erosion', rid('erosion')),
            ('final_density', rid('final_density')),
            ('ridges', rid('ridges')),
            ('temperature', rid('temperature')),
            ('vegetation', rid('vegetation')),
        ])),
        ('sea_level', 100),
        # players arrive through the portal; this only matters for /spawnpoint-less respawns in the dimension
        ('spawn_target', [OrderedDict([(rid('continents'), [C_BEACH + 0.05, 2.0]),
                                       (rid('erosion'), [-2.0, 2.0]), (rid('ridges'), [-2.0, 2.0]),
                                       (rid('temperature'), [-2.0, 2.0]), (rid('vegetation'), [-2.0, 2.0])])]),
    ])


# --------------------------------------------------------------------------------------------- biome parameters
FULL = (-2.0, 2.0)


def _box(biome, t=FULL, v=FULL, c=FULL, e=FULL, w=FULL):
    return (biome, OrderedDict(temperature=t, humidity=v, continentalness=c, erosion=e, weirdness=w))


def biome_boxes():
    """Disjoint parameter boxes covering the whole climate space (depth is always 0)."""
    B = []
    isl = (C_BEACH, 2.0)
    B.append(_box('raft:volcanic_isle', c=isl, v=(-2.0, V_VOLC)))
    B.append(_box('raft:mangrove_isle', c=isl, v=(V_VOLC, 2.0), e=(E_SHELF, 2.0)))
    B.append(_box('raft:jungle_isle', c=isl, v=(V_JUNGLE, 2.0), e=(-2.0, E_SHELF)))
    B.append(_box('raft:palm_isle', c=isl, v=(V_VOLC, V_JUNGLE), e=(-2.0, E_SHELF)))
    sh = (C_SH, C_BEACH)
    B.append(_box('raft:glowing_lagoon', c=sh, w=(W_GLOW, 2.0)))
    B.append(_box('raft:coral_shallows', c=sh, w=(-2.0, W_GLOW)))
    mid = (C_MID, C_SH)
    B.append(_box('raft:frost_floes', c=mid, t=(-2.0, T_COLD)))
    B.append(_box('raft:kelp_forest', c=mid, t=(T_COLD, 2.0)))
    # deep: trench first, then the rest split into pieces without trench
    B.append(_box('raft:abyssal_trench', c=(-2.0, C_TB), e=(-2.0, E_SHELF), w=(-W_TRENCH, W_TRENCH)))
    pieces = [  # (c, e, w)
        ((C_TB, C_MID), FULL, FULL),
        ((-2.0, C_TB), (E_SHELF, 2.0), FULL),
        ((-2.0, C_TB), (-2.0, E_SHELF), (-2.0, -W_TRENCH)),
        ((-2.0, C_TB), (-2.0, E_SHELF), (W_TRENCH, 2.0)),
    ]
    for c, e, w in pieces:
        B.append(_box('raft:frost_floes', c=c, e=e, w=w, t=(-2.0, T_COLD)))
        if e[1] > E_SHELF:   # piece reaches the shelf range
            B.append(_box('raft:drowned_shelf', c=c, w=w, t=(T_COLD, 2.0), e=(max(E_SHELF, e[0]), e[1])))
        if e[0] < E_SHELF:
            el = (e[0], min(E_SHELF, e[1]))
            B.append(_box('raft:wreck_graveyard', c=c, w=w, e=el, t=(T_WRECK, 2.0)))
            B.append(_box('raft:raft_ocean_biome', c=c, w=w, e=el, t=(T_COLD, T_WRECK)))
    return B


def dimension():
    biomes = []
    for biome, p in biome_boxes():
        params = OrderedDict()
        for k in ('temperature', 'humidity', 'continentalness', 'erosion', 'weirdness'):
            lo, hi = p[k]
            params[k] = [round(lo, 4), round(hi, 4)]
        params['depth'] = 0.0
        params['offset'] = 0.0
        biomes.append(OrderedDict(biome=biome, parameters=params))
    return OrderedDict(type='raft:raftdim_type', generator=OrderedDict(
        type='minecraft:noise', settings='raft:raftdim',
        biome_source=OrderedDict(type='minecraft:multi_noise', biomes=biomes)))


def write(module):
    from bclib import assets
    for name, params in NOISES.items():
        assets.write_json(module, f'data/{NS}/worldgen/noise/{P}{name}.json', params)
    for name, df in FUNCTIONS.items():
        assets.write_json(module, f'data/{NS}/worldgen/density_function/{P}{name}.json', df)
    assets.write_json(module, f'data/{NS}/worldgen/noise_settings/raftdim.json', noise_settings())
    assets.write_json(module, f'data/{NS}/dimension/raftdim.json', dimension())
