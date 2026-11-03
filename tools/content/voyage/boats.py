"""Palm boats and driftwood rafts: the vanilla oak boat / bamboo raft textures and item sprites, recoloured to the
palm and driftwood planks of the mod (layout stays exactly vanilla, so BoatModel/RaftModel fit)."""
from PIL import Image

from bclib import tex

M = 'voyage'


def _lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def plank_ramp(texid, n, contrast=1.0):
    """n colours from dark to light: luminance quantiles of a plank texture (optionally with more contrast)."""
    im = tex.load(texid).convert('RGBA')
    px = sorted([p for p in im.get_flattened_data() if p[3] > 0], key=_lum)
    out = []
    for i in range(n):
        chunk = px[int(len(px) * i / n): max(int(len(px) * (i + 1) / n), int(len(px) * i / n) + 1)]
        out.append(tuple(round(sum(c[k] for c in chunk) / len(chunk)) for k in range(3)) + (255,))
    if contrast != 1.0:
        mean = [sum(c[k] for c in out) / n for k in range(3)]
        out = [tuple(max(0, min(255, round(mean[k] + (c[k] - mean[k]) * contrast))) for k in range(3)) + (255,) for c in out]
    return out


def remap(img, sources, ramp):
    """Replace each source colour (sorted by luminance) with the ramp colour of the same rank."""
    srcs = sorted(set(tuple(s[:3]) for s in sources), key=_lum)
    lut = {}
    for i, s in enumerate(srcs):
        k = round(i * (len(ramp) - 1) / max(1, len(srcs) - 1))
        lut[s] = ramp[k]
    out = img.convert('RGBA').copy()
    pix = out.load()
    for y in range(out.height):
        for x in range(out.width):
            c = pix[x, y]
            if c[3] > 0 and c[:3] in lut:
                pix[x, y] = lut[c[:3]][:3] + (c[3],)
    return out


def _hex(s):
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4))


OAK_BOAT = [_hex(c) for c in '67502c 7e6237 967441 9f844d af8f55 b8945f c29d62'.split()]
BAMBOO_WOOD = [_hex(c) for c in '907e3a 998741 b7ab49 bba84c c2b54e c8b14d d3bb50 d9c25e e3cc6a efd97e'.split()]
BAMBOO_GREEN = [_hex(c) for c in '454e22 4e5827 5c662b 657030 796b36'.split()]
OAK_ITEM = [_hex(c) for c in '39241b 473a24 59482d 6f5a38 836c3d 9f844d bc9862 c5a678'.split()]
BAMBOO_ITEM_WOOD = [_hex(c) for c in '342a19 473a24 59482d 604f32 756534 92803b bba84c c2ab48 d3bb50 d9c25e e3cc6a eed670 efd97e'.split()]
BAMBOO_ITEM_GREEN = [_hex(c) for c in '3e471d 5c662b'.split()]
ROPE = [tex.rgb(c) for c in ('#6b5530', '#8f7340', '#a98a50', '#c9a96a', '#d8bb7e')]


def item_ramp(base_ramp, n_dark):
    """Plank ramp extended by n_dark darker outline shades at the bottom."""
    d0 = base_ramp[0]
    dark = [tex.darker(d0, 0.45 + 0.4 * i / max(1, n_dark)) for i in range(n_dark)]
    return dark + base_ramp


def generate():
    palm = plank_ramp('raft:block/palm_planks', 7, contrast=1.35)
    drift = plank_ramp('raft:block/driftwood_planks', 10, contrast=1.45)
    out = {}
    # entity textures
    out['boat/palm'] = remap(tex.load('minecraft:entity/boat/oak'), OAK_BOAT, palm)
    out['chest_boat/palm'] = remap(tex.load('minecraft:entity/chest_boat/oak'), OAK_BOAT, palm)
    for name, src in (('boat/driftwood', 'minecraft:entity/boat/bamboo'), ('chest_boat/driftwood', 'minecraft:entity/chest_boat/bamboo')):
        im = remap(tex.load(src), BAMBOO_WOOD, drift)
        out[name] = remap(im, BAMBOO_GREEN, ROPE)
    for name, im in out.items():
        tex.save(im, M, 'entity/%s' % name)
    # item sprites
    palm_item = item_ramp(plank_ramp('raft:block/palm_planks', 6, contrast=1.4), 2)
    drift_item = item_ramp(plank_ramp('raft:block/driftwood_planks', 9, contrast=1.6), 4)
    items = {
        'palm_boat': remap(tex.load('minecraft:item/oak_boat'), OAK_ITEM, palm_item),
        'palm_chest_boat': remap(tex.load('minecraft:item/oak_chest_boat'), OAK_ITEM, palm_item),
    }
    for name, src in (('driftwood_raft', 'minecraft:item/bamboo_raft'), ('driftwood_chest_raft', 'minecraft:item/bamboo_chest_raft')):
        im = remap(tex.load(src), BAMBOO_ITEM_WOOD, drift_item)
        items[name] = remap(im, BAMBOO_ITEM_GREEN, [ROPE[1], ROPE[3]])
    for name, im in items.items():
        tex.save(im, M, 'item/%s' % name)
    return out, items
