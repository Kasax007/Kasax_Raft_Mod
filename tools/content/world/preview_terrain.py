"""Preview of the raft dimension terrain with sim.py: height map, biome map, zone statistics, cross sections.

cd tools && python3 -m content.world.preview_terrain [size] [step] [seed] [x0] [z0]
Writes PNGs into $SP/work/world/ (see OUT).
"""
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

from content.world import terrain
from content.world.sim import Evaluator

OUT = os.environ.get('WORLD_OUT', '/tmp/claude-0/-home-user/4ca9508b-4f8c-5a8e-91dc-6059d6cc887c/scratchpad/work/world')

BIOME_COL = {
    'raft:raft_ocean_biome': (30, 70, 160), 'raft:kelp_forest': (40, 120, 110), 'raft:coral_shallows': (70, 210, 220),
    'raft:glowing_lagoon': (60, 230, 170), 'raft:drowned_shelf': (110, 120, 110), 'raft:abyssal_trench': (10, 15, 40),
    'raft:wreck_graveyard': (90, 100, 130), 'raft:frost_floes': (200, 230, 250), 'raft:palm_isle': (230, 210, 120),
    'raft:jungle_isle': (40, 150, 40), 'raft:mangrove_isle': (100, 110, 50), 'raft:volcanic_isle': (120, 40, 30),
}


def evaluator(seed):
    noises = {f'raft:raftdim/{k}': v for k, v in terrain.NOISES.items()}
    funcs = {f'raft:raftdim/{k}': v for k, v in terrain.FUNCTIONS.items()}
    funcs['minecraft:y'] = {'type': 'minecraft:gradient', 'axis': 'y', 'from_coordinate': -4064, 'from_value': -4064.0,
                            'to_coordinate': 4062, 'to_value': 4062.0}
    return Evaluator(noises, funcs, seed)


def biome_of(params):
    """params: dict name->array. Nearest box (squared distance), like the game's RTree search."""
    boxes = terrain.biome_boxes()
    shape = params['continentalness'].shape
    best = np.full(shape, np.inf)
    idx = np.zeros(shape, dtype=np.int32)
    for i, (b, p) in enumerate(boxes):
        d = np.zeros(shape)
        for k, (lo, hi) in p.items():
            v = params[k]
            d += np.where(v < lo, lo - v, np.where(v > hi, v - hi, 0)) ** 2
        m = d < best
        best = np.where(m, d, best)
        idx = np.where(m, i, idx)
    return idx, [b for b, _ in boxes]


def sample(ev, x, z):
    memo = {}
    y = np.zeros_like(x)
    r = lambda n: ev.ev(f'raft:raftdim/{n}', x, y, z, memo)
    return {'surface': r('surface'), 'continentalness': r('continents'), 'erosion': r('erosion'),
            'weirdness': r('ridges'), 'temperature': r('temperature'), 'humidity': r('vegetation')}


def height_colour(h):
    h = np.asarray(h)
    img = np.zeros(h.shape + (3,), dtype=np.float64)
    water = h < 100
    d = np.clip((100 - h) / 100.0, 0, 1)
    img[..., 0] = np.where(water, 20 + 60 * (1 - d), 0)
    img[..., 1] = np.where(water, 60 + 150 * (1 - d) ** 1.5, 0)
    img[..., 2] = np.where(water, 110 + 140 * (1 - d), 0)
    land = ~water
    t = np.clip((h - 100) / 80.0, 0, 1)
    beach = land & (h < 103.5)
    img[..., 0] = np.where(land, np.where(beach, 235, 70 + 150 * t), img[..., 0])
    img[..., 1] = np.where(land, np.where(beach, 220, 150 + 30 * t - 80 * t * t), img[..., 1])
    img[..., 2] = np.where(land, np.where(beach, 160, 60 + 100 * t), img[..., 2])
    return np.clip(img, 0, 255).astype(np.uint8)


def main():
    size = int(sys.argv[1]) if len(sys.argv) > 1 else 4096
    step = int(sys.argv[2]) if len(sys.argv) > 2 else 8
    seed = int(sys.argv[3]) if len(sys.argv) > 3 else 1
    x0 = int(sys.argv[4]) if len(sys.argv) > 4 else -size // 2
    z0 = int(sys.argv[5]) if len(sys.argv) > 5 else -size // 2
    os.makedirs(OUT, exist_ok=True)
    ev = evaluator(seed)
    xs = np.arange(x0, x0 + size, step, dtype=np.float64)
    zs = np.arange(z0, z0 + size, step, dtype=np.float64)
    x, z = np.meshgrid(xs, zs)
    s = sample(ev, x, z)
    h = s['surface']
    idx, names = biome_of(s)
    # statistics
    tot = h.size
    zones = [('trench <=15', h <= 15), ('deep 15-60', (h > 15) & (h <= 60)), ('mid 60-80', (h > 60) & (h <= 80)),
             ('shallow 80-99', (h > 80) & (h < 100)), ('land', h >= 100), ('high >140', h > 140)]
    print('zones:', ', '.join(f'{n} {m.sum() / tot * 100:.1f}%' for n, m in zones))
    print('biomes:')
    for i, n in enumerate(names):
        pass
    uniq = {}
    for i, n in enumerate(names):
        uniq.setdefault(n, []).append(i)
    for n, ids in sorted(uniq.items()):
        m = np.isin(idx, ids)
        if m.any():
            print(f'  {n:24s} {m.sum() / tot * 100:5.1f}%  height {np.percentile(h[m], 5):6.1f}..{np.percentile(h[m], 50):6.1f}..{np.percentile(h[m], 95):6.1f}')
        else:
            print(f'  {n:24s}   0.0%')
    for k in ('continentalness', 'erosion', 'weirdness', 'temperature', 'humidity'):
        v = s[k]
        print(f'  {k}: std {v.std():.3f} q15 {np.percentile(v, 15):.3f} q42 {np.percentile(v, 42):.3f} '
              f'q73 {np.percentile(v, 73):.3f} q85 {np.percentile(v, 85):.3f}')
    # land on non-island biome / island biome on water
    isl = np.isin(idx, [i for i, n in enumerate(names) if n.endswith('_isle')])
    print(f'land outside island biomes: {((h >= 100.5) & ~isl).sum() / max(1, (h >= 100.5).sum()) * 100:.2f}% of land;'
          f' island biome under water: {((h < 99) & isl).sum() / max(1, isl.sum()) * 100:.2f}%')
    hc = height_colour(h).astype(np.float64)
    gy, gx = np.gradient(h, step)
    shade = np.clip(1.0 + (-gx - gy) * 0.35, 0.55, 1.45)[..., None]
    img = Image.fromarray(np.clip(hc * shade, 0, 255).astype(np.uint8))
    img.save(os.path.join(OUT, f'map_height_s{seed}.png'))
    col = np.array([BIOME_COL[n] for n in names], dtype=np.uint8)
    bimg = col[idx]
    shade = height_colour(h).astype(np.float64)
    blend = (bimg * 0.65 + shade * 0.35).astype(np.uint8)
    bi = Image.fromarray(blend)
    d = ImageDraw.Draw(bi)
    yy = 4
    for n, c in BIOME_COL.items():
        d.rectangle((4, yy, 14, yy + 10), fill=c, outline=(0, 0, 0))
        d.text((18, yy - 1), n.split(':')[1], fill=(255, 255, 255))
        yy += 13
    bi.save(os.path.join(OUT, f'map_biome_s{seed}.png'))
    # cross section through the middle row
    row = h.shape[0] // 2
    W, H = h.shape[1], 260
    cs = Image.new('RGB', (W, H), (200, 230, 255))
    dc = ImageDraw.Draw(cs)
    for i in range(W):
        top = 255 - int(h[row, i] * 1.0) + 64 // 2
        dc.line((i, H - 1, i, max(0, H - 1 - int(h[row, i] + 64) * H // 320)), fill=(110, 100, 90))
    sea = H - 1 - (100 + 64) * H // 320
    dc.line((0, sea, W, sea), fill=(30, 90, 200))
    cs.save(os.path.join(OUT, f'section_s{seed}.png'))
    print('written', OUT)


if __name__ == '__main__':
    main()
