"""One-off helper (the Java models keep the chosen offsets; horn and horn_tip were placed by hand in the old horn slot): packs the UV footprints of box models (shelf packing) to choose texOffs for the Java models.
Run: python3 tools/content/boss/uvpack.py   (prints name -> (u, v))."""

LEVIATHAN = [  # name, w, h, d
    ('skull', 30, 18, 28), ('jaw', 24, 7, 38), ('seg_a', 26, 22, 26), ('seg_b', 20, 18, 24),
    ('lower_teeth', 20, 2, 31), ('snout', 22, 12, 16), ('stock', 14, 12, 22), ('fluke', 1, 32, 22),
    ('upper_teeth', 18, 2, 14), ('crest', 2, 9, 26), ('fin_a', 2, 9, 20), ('fin_b', 2, 8, 18),
    ('frill', 1, 20, 16), ('collar_b', 22, 3, 5), ('side_fin', 14, 1, 12), ('brow', 7, 4, 16),
    ('horn', 4, 4, 10), ('horn_tip', 2, 2, 8), ('side_fluke', 10, 1, 10), ('tail_fin', 2, 6, 14), ('chin', 6, 3, 8),
    ('collar_r', 3, 24, 5), ('collar_l', 3, 21, 5), ('collar_t', 8, 3, 5), ('eye', 1, 4, 6),
    ('barbel', 1, 12, 1), ('chain', 2, 8, 2), ('fang', 2, 5, 2),
]


def pack(boxes, size=256, pad=0):
    items = sorted(boxes, key=lambda b: -(b[3] + b[2]))
    x = y = shelf = 0
    out = {}
    for name, w, h, d in items:
        W, H = 2 * (w + d), d + h
        if x + W > size:
            x, y, shelf = 0, y + shelf + pad, 0
        out[name] = (x, y, W, H)
        x += W + pad
        shelf = max(shelf, H)
    if y + shelf > size:
        raise SystemExit('does not fit: height %d' % (y + shelf))
    return out


if __name__ == '__main__':
    for k, v in pack(LEVIATHAN).items():
        print(k, v)
