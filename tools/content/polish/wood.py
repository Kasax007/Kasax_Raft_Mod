"""Wood family of 2.0: driftwood and palm (planks, logs, stripped logs, leaves, doors, trapdoors), thatch, raft
foundations, rope blocks, sail canvas, flag pole, crop plot."""
import math
import random

from content.building import tex_wood_stone as TW
from .common import *  # noqa: F401,F403


# ================================================================================================ planks
def driftwood_planks():
    img = TW._planks(DRIFT, 71, tones=[4, 3, 4, 3], joints=[10, None, 3, 12], seam_drop=2, streaks=2,
                     nails=[(1, 1), (6, 9), (13, 13)],
                     cracks=[[(4, 6), (5, 6), (6, 6)], [(12, 1), (13, 1)]])
    for (x, y) in [(7, 0), (8, 0), (1, 8), (2, 8), (9, 12)]:  # sun-bleached
        put(img, x, y, DRIFT[6])
    return img


def palm_planks():
    caulk = lerp(PALM[0], PALM[1], 0.5)
    img = TW._planks(PALM, 52, tones=[4, 3, 4, 3], joints=[5, None, 12, None], seam_drop=2, streaks=2,
                     nails=[(10, 5), (2, 13)])
    for (x, y) in [(12, 0), (3, 4), (8, 12)]:
        put(img, x, y, PALM[6])
    return img


# ================================================================================================ logs
def bark(p, seed, furrows, knots=(), ridge_light=1, base=3, cracks_dark=0):
    """Vertical bark, tileable. Each column gets a tone and long vertical streaks (one shade up/down); furrows:
    list of (x, phase, breaks) – dark grooves wobbling by +-1 with a lit ridge on their right (light from the left),
    breaks = rows where the groove is shallow."""
    rnd = random.Random(seed)
    img = new()
    col = [base + rnd.choice([0, 0, 0, 1, -1]) for _ in range(16)]
    for x in range(16):
        for y in range(16):
            put(img, x, y, p[col[x]])
        y = rnd.randrange(16)
        for _ in range(2):
            ln = rnd.randrange(4, 9)
            d = rnd.choice([1, -1, -1])
            for k in range(ln):
                put(img, x, y + k, p[max(0, min(len(p) - 1, col[x] + d))])
            y += ln + rnd.randrange(2, 5)
    for (fx, phase, breaks) in furrows:
        for y in range(16):
            wob = round(math.sin((y + phase) / 16 * 2 * math.pi) * 0.8)
            x = fx + wob
            deep = y not in breaks
            put(img, x, y, p[cracks_dark] if deep else p[max(0, base - 2)])
            if deep:
                put(img, x + 1, y, p[min(len(p) - 1, base + ridge_light)])
                put(img, x - 1, y, p[max(0, base - 1)])
    for (kx, ky) in knots:
        put(img, kx, ky, p[0]); put(img, kx + 1, ky, p[1])
        put(img, kx, ky - 1, p[base + 1]); put(img, kx + 1, ky + 1, p[max(0, base - 1)])
    return img


def driftwood_log():
    img = bark(DRIFT_BARK, 11, furrows=[(1, 0, {5, 6}), (5, 5, {12}), (9, 9, {2, 3}), (13, 3, {9, 10})],
               knots=[(10, 13)], base=4, cracks_dark=1)
    # salt-bleached ridges
    for (x, y) in [(3, 2), (3, 3), (11, 7), (11, 8), (7, 14), (15, 11)]:
        put(img, x, y, DRIFT_BARK[6])
    return img


def stripped(p, seed, lines):
    """Stripped log side: smooth wood with long vertical grain lines (tileable)."""
    img = new()
    n = noise(seed, cell=8, octaves=1)
    for y in range(16):
        for x in range(16):
            put(img, x, y, p[4] if n[(y // 4) % 16][x] > 0.5 else p[3])
    rnd = random.Random(seed)
    for (x, y0, ln, d) in lines:
        for k in range(ln):
            c = img.getpixel((x % 16, (y0 + k) % 16))
            put(img, x, y0 + k, lerp(c, p[2] if d < 0 else p[5], 0.6))
    return img


def stripped_driftwood_log():
    return stripped(STRIPPED_DRIFT, 5, [(2, 0, 9, -1), (2, 11, 3, 1), (6, 4, 12, -1), (9, 0, 5, 1), (11, 8, 7, -1),
                                         (14, 2, 10, -1), (4, 13, 6, 1), (12, 0, 3, -1)])


def stripped_palm_log():
    return stripped(STRIPPED_PALM, 8, [(1, 3, 10, -1), (4, 0, 6, 1), (5, 9, 9, -1), (8, 2, 11, -1), (10, 12, 6, 1),
                                        (12, 5, 8, -1), (15, 0, 12, -1), (13, 14, 4, 1)])


def ring_top(p_wood, p_bark, seed, bark_ring=True, fibres=False):
    """Log end: squarish growth rings, 1-px bark rim, pith; light from the top-left. fibres=True gives the speckled
    end grain of a palm (monocot: fibre bundles instead of rings)."""
    rnd = random.Random(seed)
    img = new()
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            d = max(abs(dx), abs(dy)) * 0.65 + math.hypot(dx, dy) * 0.35
            if fibres:
                t = 4 if d < 5.2 else 3
                if d >= 6.3:
                    t = 2
            else:
                ring = int(d * 1.05)
                t = 4 if ring % 2 == 0 else 3
                if d < 1.3:
                    t = 2
            # light: top-left brighter
            if dx + dy < -9 and t < 5:
                t += 1
            if dx + dy > 10:
                t -= 1
            put(img, x, y, p_wood[max(0, t)])
    if fibres:
        pts = set()
        for y in range(1, 15):
            for x in range(1, 15):
                if (x * 7 + y * 11 + (x * y) % 5) % 9 == 0 and max(abs(x - 7.5), abs(y - 7.5)) < 6:
                    pts.add((x, y))
        for (x, y) in pts:
            put(img, x, y, p_wood[2])
            if (x + 1, y) not in pts:
                put(img, x + 1, y, p_wood[5])
    if bark_ring:
        for i in range(16):
            for (x, y) in ((i, 0), (0, i), (i, 15), (15, i)):
                t = 3 if (x + y + rnd.randrange(2)) % 3 else 2
                if x == 15 or y == 15:
                    t -= 1
                put(img, x, y, p_bark[t])
    else:
        for i in range(16):
            put(img, i, 0, p_wood[2]); put(img, 0, i, p_wood[2]); put(img, i, 15, p_wood[1]); put(img, 15, i, p_wood[1])
    return img


def driftwood_log_top():
    return ring_top(STRIPPED_DRIFT, DRIFT_BARK, 3)


def stripped_driftwood_log_top():
    return ring_top(STRIPPED_DRIFT, DRIFT_BARK, 3, bark_ring=False)


def palm_log_top():
    return ring_top(STRIPPED_PALM, PALM_BARK, 4, fibres=True)


def stripped_palm_log_top():
    return ring_top(STRIPPED_PALM, PALM_BARK, 4, bark_ring=False, fibres=True)


def palm_log():
    """Palm trunk: stacked rings of old leaf bases (4 rows each: lit lip, fibrous body, shadow under the lip of the
    ring above), staggered diamond leaf scars, fibres. No vertical seams, so it does not read as bricks."""
    p = PALM_BARK
    img = new()
    rnd = random.Random(9)
    for b in range(4):
        y0 = b * 4
        wave = [0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0]
        sh = (b * 5) % 16
        for x in range(16):
            w = wave[(x + sh) % 16]
            for yy in range(4):
                t = [5, 4, 3, 2][yy]
                if yy == 1 and (x + b) % 5 == 0:
                    t = 3
                if yy == 2 and (x * 3 + b) % 7 == 0:
                    t = 4                      # fibre highlight
                put(img, x, y0 + yy, p[t])
            put(img, x, y0 + 3 + w, p[1])       # shadow under the ring, wavy
            if w:
                put(img, x, y0 + 3, p[2])
        # diamond leaf scars, staggered per ring
        cx = (b * 8 + 3) % 16
        for (dx, dy, t) in ((0, 1, 1), (-1, 2, 2), (1, 2, 1), (0, 2, 0), (0, 3, 1)):
            put(img, cx + dx, y0 + dy, p[t])
        put(img, cx - 1, y0 + 1, p[5])
    for (x, y) in [(6, 2), (13, 6), (9, 14), (1, 10)]:
        put(img, x, y, p[6])
    return img


# ================================================================================================ leaves
def leaves_cluster(p, seed, holes=0.2, n_leaves=22, accents=()):
    """Vanilla-like leaves: tileable clusters of small lit leaves over a darker canopy, some see-through gaps."""
    rnd = random.Random(seed)
    img = new()
    n = noise(seed, cell=4, octaves=2)
    flat = sorted(v for r in n for v in r)
    thr = flat[int(len(flat) * holes)]
    for y in range(16):
        for x in range(16):
            if n[y][x] < thr:
                continue
            put(img, x, y, p[1] if n[y][x] < 0.5 else p[2])
    for i in range(n_leaves):
        x, y = rnd.randrange(16), rnd.randrange(16)
        if n[y][x] < thr:
            continue
        put(img, x, y, p[4]); put(img, x + 1, y, p[3]); put(img, x, y + 1, p[3]); put(img, x + 1, y + 1, p[2])
        if rnd.random() < 0.5:
            put(img, x - 1, y, p[5])
    for (x, y, c) in accents:
        put(img, x, y, c)
    return img


def driftwood_leaves():
    img = leaves_cluster(RUSSET, 23, holes=0.2, n_leaves=26)
    # a few olive leaves still holding on
    olive = pal('#4f5a2c', '#6a7438')
    for (x, y) in [(3, 2), (11, 9), (6, 13)]:
        if img.getpixel((x, y))[3]:
            put(img, x, y, olive[1]); put(img, x + 1, y + 1, olive[0])
    return binary_alpha(img)


def palm_fronds(seed, coconuts=()):
    """Palm leaves block: long drooping leaflets (2 px: lit upper edge, shaded underside) laid over a dark canopy
    with see-through gaps; tileable (strokes wrap). Optional coconuts: (x, y) of a 4x4 nut."""
    p = PALM_LEAF
    rnd = random.Random(seed)
    img = new()
    n = noise(seed, cell=4, octaves=2)
    flat = sorted(v for r in n for v in r)
    thr = flat[int(len(flat) * 0.22)]
    for y in range(16):
        for x in range(16):
            if n[y][x] >= thr:
                put(img, x, y, p[1] if n[y][x] < 0.55 else p[2])
    for i in range(16):
        x, y = rnd.randrange(16), rnd.randrange(16)
        dx = rnd.choice([1, -1])
        ln = rnd.randrange(4, 8)
        for k in range(ln):
            xx, yy = x + k * dx, y + (k + 1) // 2           # droops: one row down every two columns
            tip = k >= ln - 2
            put(img, xx, yy, p[5] if k < 2 else (p[4] if not tip else p[3]))
            put(img, xx, yy + 1, p[3] if not tip else p[2])
        put(img, x, y, p[6])                                  # sunlit base of the leaflet
    for (cx, cy) in coconuts:
        nut = ['.12.', '1342', '1322', '.21.']
        cp = pal('#2c1a10', '#4a2c18', '#6b4224', '#8c5a32', '#a87444')
        for yy, r in enumerate(nut):
            for xx, ch in enumerate(r):
                if ch != '.':
                    put(img, cx + xx, cy + yy, cp[int(ch)])
    return binary_alpha(img)


def palm_leaves():
    return palm_fronds(41)


def palm_coconut_leaves():
    return palm_fronds(41, coconuts=[(2, 9), (9, 3), (11, 11)])


# ================================================================================================ thatch
def palm_thatch():
    """Dried palm-frond thatch: 4 overlapping rows of bundles, vertical strands, shadow under each row."""
    p = THATCH
    img = new()
    rnd = random.Random(17)
    green = pal('#5e6a2c', '#76833a')
    for r in range(4):
        y0 = r * 4
        off = (r % 2) * 2
        for x in range(16):
            col = (x + off) % 4
            for yy in range(4):
                y = y0 + yy
                t = [4, 3, 3, 2][yy]
                if col == 0:
                    t -= 1
                if col == 1 and yy < 2:
                    t += 1
                if yy == 0 and col in (1, 2):
                    t = 5
                put(img, x, y, p[max(0, min(6, t))])
            put(img, x, y0 + 3, p[1] if col else p[0])   # shadow line under the bundle row
        # strand ends hang over the next row by 0-1 px (jagged edge)
        for x in range(16):
            if rnd.random() < 0.35:
                put(img, x, y0 + 4, p[3] if (x + off) % 4 else p[2])
                put(img, x, y0 + 5, p[1])
    for (x, y) in [(5, 1), (13, 9), (2, 13)]:
        put(img, x, y, green[1]); put(img, x, y + 1, green[0])
    return img


# ================================================================================================ doors, trapdoors
def _board_door(p, seed, vertical_boards=True):
    """Door half: 4 vertical boards with a lit left edge and seam, frame on top/bottom left/right."""
    img = new()
    rnd = random.Random(seed)
    for b in range(4):
        x0 = b * 4
        t = [4, 3, 4, 3][b]
        for x in range(x0, x0 + 4):
            for y in range(16):
                put(img, x, y, p[t])
        for y in range(16):
            put(img, x0, y, p[t + 1] if x0 else p[t])
            put(img, x0 + 3, y, p[max(0, t - 2)])
        # grain streaks
        for _ in range(2):
            x = x0 + 1 + rnd.randrange(2)
            y = rnd.randrange(16)
            for k in range(rnd.randrange(3, 7)):
                put_in(img, x, y + k, p[t - 1])
    return img


def driftwood_door_bottom():
    p = DRIFT
    img = _board_door(p, 31)
    # Z brace: two battens and a diagonal
    for y0 in (2, 11):
        fill(img, 1, y0, 14, y0 + 2, p[4])
        for x in range(1, 15):
            put(img, x, y0, p[5]); put(img, x, y0 + 2, p[1])
        for x in (2, 13):
            put(img, x, y0 + 1, p[1])          # nails
    for i in range(8):
        x, y = 12 - i, 4 + i
        if 4 < y < 11:
            put(img, x, y, p[4]); put(img, x + 1, y, p[5]); put(img, x - 1, y, p[1])
    for x in range(16):
        put(img, x, 15, p[1])
    for y in range(16):
        put(img, 0, y, p[5]); put(img, 15, y, p[1])
    return img


def driftwood_door_top():
    p = DRIFT
    img = _board_door(p, 32)
    # window frame with two panes (see-through)
    fill(img, 2, 2, 13, 9, p[2])
    for x in range(2, 14):
        put(img, x, 2, p[1]); put(img, x, 9, p[5])
    for y in range(2, 10):
        put(img, 2, y, p[1]); put(img, 13, y, p[5])
    for (x0, x1) in ((3, 7), (9, 12)):
        fill(img, x0, 3, x1, 8, (0, 0, 0, 0))
    fill(img, 8, 3, 8, 8, p[4])
    put(img, 8, 3, p[5])
    # batten
    fill(img, 1, 12, 14, 14, p[4])
    for x in range(1, 15):
        put(img, x, 12, p[5]); put(img, x, 14, p[1])
    put(img, 2, 13, p[1]); put(img, 13, 13, p[1])
    put(img, 13, 6, p[0])   # no handle on top: knob is on the bottom in vanilla? keep a latch
    for x in range(16):
        put(img, x, 0, p[5])
    for y in range(16):
        put(img, 0, y, p[5]); put(img, 15, y, p[1])
    # latch
    put(img, 12, 11, IRON[3]); put(img, 13, 11, IRON[1])
    return img


def driftwood_trapdoor():
    p = DRIFT
    img = new()
    for b in range(4):
        y0 = b * 4
        t = [4, 3, 4, 3][b]
        fill(img, 0, y0, 15, y0 + 3, p[t])
        for x in range(16):
            put(img, x, y0, p[t + 1]); put(img, x, y0 + 3, p[t - 2])
    rnd = random.Random(4)
    for b in range(4):
        y = b * 4 + 1 + rnd.randrange(2)
        x = rnd.randrange(16)
        for k in range(5):
            put(img, x + k, y, p[[4, 3, 4, 3][b] - 1])
    # frame
    for i in range(16):
        put(img, i, 0, p[5]); put(img, 0, i, p[5]); put(img, i, 15, p[1]); put(img, 15, i, p[1])
    for x in (1, 14):
        for y in range(1, 15):
            put(img, x, y, p[3])
    # two vent holes (see-through)
    for (x0, y0) in ((5, 6), (9, 6)):
        fill(img, x0, y0, x0 + 1, y0 + 3, (0, 0, 0, 0))
    # nails
    for (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)):
        put(img, x, y, p[0])
    return img


def palm_window(x, y):
    """Diamond window of the palm door (4 panes, 2-px cross mullion), centre (7.5, 5.5)."""
    return abs(x - 7.5) + abs(y - 5.5) <= 4.6 and x not in (7, 8) and y not in (5, 6)


def _palm_board(p, seed):
    img = new()
    rnd = random.Random(seed)
    for b in range(4):
        x0 = b * 4
        t = [4, 4, 3, 4][b]
        fill(img, x0, 0, x0 + 3, 15, p[t])
        for y in range(16):
            put(img, x0, y, p[t + 1]); put(img, x0 + 3, y, p[t - 2])
        for _ in range(2):
            x = x0 + 1 + rnd.randrange(2); y = rnd.randrange(16)
            for k in range(rnd.randrange(3, 7)):
                put_in(img, x, y + k, p[t - 1])
    return img


def palm_door_top():
    p = PALM
    img = _palm_board(p, 61)
    fill(img, 2, 1, 13, 10, p[2])       # carved recess
    for x in range(2, 14):
        put(img, x, 1, p[1]); put(img, x, 10, p[5])
    for y in range(1, 11):
        put(img, 2, y, p[1]); put(img, 13, y, p[5])
    for y in range(16):
        for x in range(16):
            if palm_window(x, y):
                put(img, x, y, (0, 0, 0, 0))
    for y in range(16):                 # lit lower/right sill of each pane, shadowed upper/left lip
        for x in range(16):
            if img.getpixel((x, y))[3] and not palm_window(x, y):
                if palm_window(x - 1, y) or palm_window(x, y - 1):
                    put(img, x, y, p[5])
                elif palm_window(x + 1, y) or palm_window(x, y + 1):
                    put(img, x, y, p[1])
    fill(img, 1, 12, 14, 14, p[3])
    for x in range(1, 15):
        put(img, x, 12, p[5]); put(img, x, 14, p[1])
    for y in range(16):
        put(img, 0, y, p[5]); put(img, 15, y, p[1])
    for x in range(16):
        put(img, x, 0, p[5])
    return img


def palm_door_bottom():
    p = PALM
    img = _palm_board(p, 62)
    for y0 in (1, 8):                      # two raised panels
        fill(img, 2, y0, 13, y0 + 5, p[4])
        for x in range(2, 14):
            put(img, x, y0, p[5]); put(img, x, y0 + 5, p[1])
        for y in range(y0, y0 + 6):
            put(img, 2, y, p[5]); put(img, 13, y, p[1])
        fill(img, 5, y0 + 2, 10, y0 + 3, p[3])   # diamond inlay
        put(img, 5, y0 + 2, p[2]); put(img, 10, y0 + 3, p[5])
    # handle
    put(img, 12, 1, BRASS[4]); put(img, 12, 2, BRASS[2]); put(img, 11, 1, BRASS[3])
    for x in range(16):
        put(img, x, 15, p[1])
    for y in range(16):
        put(img, 0, y, p[5]); put(img, 15, y, p[1])
    return img


def palm_trapdoor():
    p = PALM
    img = _palm_board(p, 63)
    for i in range(16):
        put(img, i, 0, p[5]); put(img, 0, i, p[5]); put(img, i, 15, p[1]); put(img, 15, i, p[1])
    for y in (1, 14):
        fill(img, 1, y, 14, y, p[3])
    # star-shaped cut-out (the 2.0 palm trapdoor's motif)
    star = ['...x..x...', '....xx....', '.xxxxxxxx.', '....xx....', '...x..x...']
    for yy, r in enumerate(star):
        for xx, ch in enumerate(r):
            if ch == 'x':
                put(img, 3 + xx, 5 + yy, (0, 0, 0, 0))
    # shadowed rim below/right of the cut
    for (x, y) in ((4, 6), (12, 8), (7, 10)):
        put(img, x, y, p[1])
    return img


DOOR_ITEM = {
    'driftwood': ['....0000000.....',
                  '....0544442.....',
                  '....04.4.42.....',
                  '....04.3.42.....',
                  '....0444442.....',
                  '....0555552.....',
                  '....0433332.....',
                  '....0433372.....',
                  '....0434332.....',
                  '....0443332.....',
                  '....0555552.....',
                  '....0433332.....',
                  '....0433332.....',
                  '....0555552.....',
                  '....0333332.....',
                  '....0111111.....'],
    'palm': ['....0000000.....',
             '....0555554.....',
             '....052..24.....',
             '....05....4.....',
             '....052222.4....',
             '....0533334.....',
             '....0555554.....',
             '....0544424.....',
             '....0533327.....',
             '....0522224.....',
             '....0555554.....',
             '....0544424.....',
             '....0533324.....',
             '....0522224.....',
             '....0444443.....',
             '....0111111.....'],
}


def door_item(kind):
    """Item icon like vanilla door items (8 px wide door with frame, window, battens)."""
    p = DRIFT if kind == 'driftwood' else PALM
    m = {str(i): p[i] for i in range(7)}
    m['0'] = p[0]
    m['7'] = IRON[4] if kind == 'driftwood' else BRASS[4]
    img = sprite(DOOR_ITEM[kind], m)
    img = binary_alpha(img)
    return img


# ================================================================================================ raft foundation
def _log_row(img, p, x0, w, seed):
    """One vertical log w px wide: cylinder shading (lit left, dark right), soft bark streaks, tileable in y."""
    rnd = random.Random(seed)
    shade = {4: [5, 4, 3, 2], 3: [5, 3, 2], 5: [5, 4, 4, 3, 2]}[w]
    for x in range(w):
        for y in range(16):
            put(img, x0 + x, y, p[shade[x]])
    for _ in range(3):
        x = x0 + rnd.randrange(1, w - 1)
        y = rnd.randrange(16)
        for k in range(rnd.randrange(3, 6)):
            put(img, x, y + k, p[shade[x - x0] - 1])


def _rope_band(img, y0, p=ROPE):
    """Horizontal rope lashing 2 rows tall: twisted strands (diagonal light/dark)."""
    for x in range(16):
        a = x % 3
        put(img, x, y0, p[[4, 3, 2][a]])
        put(img, x, y0 + 1, p[[3, 2, 3][a]])
        put(img, x, y0 + 2, lerp(p[1], DRIFT[1], 0.5))      # shadow under the rope


def raft_foundation():
    img = new()
    p = DRIFT
    for i, x0 in enumerate((0, 4, 8, 12)):
        _log_row(img, p, x0, 4, 100 + i)
    for y in range(16):                       # gaps between logs
        for x0 in (0, 4, 8, 12):
            put(img, x0 + 3, y, p[1])
            put(img, x0, y, p[4] if y % 5 else p[5])
    for y0 in (2, 11):
        _rope_band(img, y0)
    for (x, y) in ((6, 7), (14, 15)):          # knots in the logs
        put(img, x, y, p[1]); put(img, x + 1, y, p[2])
    return img


def reinforced_foundation():
    img = new()
    p = TEAK
    # horizontal teak planks
    img = TW._planks(TEAK, 77, tones=[4, 3, 4, 3], joints=[None, 6, None, 11], caulk=lerp(TEAK[0], TEAK[1], 0.5))
    # two steel straps with rivets
    for y0 in (1, 11):
        for x in range(16):
            put(img, x, y0, STEEL[5]); put(img, x, y0 + 1, STEEL[4]); put(img, x, y0 + 2, STEEL[2])
            put(img, x, y0 + 3, lerp(TEAK[0], (0, 0, 0, 255), 0.2))
        for x in (2, 7, 12):
            put(img, x, y0 + 1, STEEL[6]); put(img, x + 1, y0 + 1, STEEL[1])
    # rust streak under a rivet
    put(img, 8, 4, RUST[3]); put(img, 8, 5, RUST[2])
    return img


# ================================================================================================ rope, canvas, pole
def recolour_mask(old, painter):
    """Keep the alpha mask of an old cut-out texture and repaint each opaque pixel with painter(x, y, old_colour)."""
    img = new()
    for y in range(old.height):
        for x in range(old.width):
            c = old.getpixel((x, y))
            if c[3] >= 128:
                img.putpixel((x, y), rgba(painter(x, y, c)))
    return img


def rope_coil(old):
    """Coil seen from above: concentric turns with a twist pattern, centre hole dark."""
    def paint(x, y, c):
        dx, dy = x - 7.5, y - 7.5
        d = math.hypot(dx, dy)
        a = math.atan2(dy, dx)
        turn = int(d / 1.6)
        twist = (int(a / (2 * math.pi) * (10 + turn * 6)) + turn) % 2
        t = 4 if twist else 3
        if (d / 1.6) % 1 > 0.7:
            t = 1                                  # groove between turns
        if dx + dy < -4:
            t += 1
        if dx + dy > 5:
            t -= 1
        if d < 2.2:
            t = 0
        return ROPE[max(0, min(5, t))]
    return recolour_mask(old, paint)


def rope_ladder(old):
    """Keep the ladder shape: rope rails (twisted) and driftwood rungs."""
    def paint(x, y, c):
        bright = lum(c)
        # rungs are the horizontal runs: detect by row having many opaque pixels
        if ROW_COUNT[y] > 6 and 2 < x < 13:
            t = 4 if y == RUNG_TOP.get(y, y) else 3
            return PALM[(5 if x < 5 else 4) if RUNG_TOP.get(y) == y else 2]
        return ROPE[[4, 3, 2][(y + x) % 3] + (1 if x % 2 == 0 else 0)]
    ROW_COUNT = [sum(1 for x in range(16) if old.getpixel((x, y))[3] >= 128) for y in range(16)]
    RUNG_TOP = {y: y for y in range(16) if ROW_COUNT[y] > 6 and (y == 0 or ROW_COUNT[y - 1] <= 6)}
    return recolour_mask(old, paint)


def canvas(stripes=()):
    """Sail canvas: plain weave, stitched hem; stripes: list of (x0, x1) red stripes."""
    p = CANVAS
    img = new()
    n = noise(8, cell=8, octaves=1)
    for y in range(16):
        for x in range(16):
            t = 4 if n[y][x] > 0.45 else 3
            if n[y][x] > 0.72:
                t = 5
            c = p[t]
            if (x % 2 == 0 and y % 2 == 0) or (x % 4 == 1 and y % 3 == 1):
                c = lerp(c, p[t - 1], 0.45)          # weave
            put(img, x, y, c)
    for (x0, x1) in stripes:
        for y in range(16):
            for x in range(x0, x1 + 1):
                t = 3 if (x % 2 or y % 2) else 2
                if x == x0:
                    t += 1
                if x == x1:
                    t -= 1
                put(img, x, y, RED[t])
    for i in range(16):   # hem with stitches
        for (x, y) in ((i, 0), (0, i)):
            put(img, x, y, p[2] if i % 2 else p[1])
        for (x, y) in ((i, 15), (15, i)):
            put(img, x, y, p[1] if i % 2 else p[0])
    return img


def raft_flag_pole():
    """Pole texture (used on a thin post): vertical lathed driftwood with rope wraps."""
    img = new()
    p = DRIFT
    shade = [5, 5, 4, 4, 4, 3, 3, 3, 3, 3, 2, 2, 2, 2, 1, 1]
    for x in range(16):
        for y in range(16):
            put(img, x, y, p[shade[x]])
    for (x, y0, ln) in ((3, 2, 6), (9, 9, 5), (6, 12, 4), (12, 1, 5)):
        for k in range(ln):
            put(img, x, y0 + k, p[shade[x] - 1])
    for y0 in (5,):
        for x in range(16):
            put(img, x, y0, ROPE[[5, 4, 3][x % 3] - (1 if x > 9 else 0)])
            put(img, x, y0 + 1, ROPE[[3, 2, 4][x % 3] - (1 if x > 9 else 0)])
    return img


# ================================================================================================ crop plot
def crop_plot_side():
    """Planter box side: two driftwood boards with corner posts (dirt shows over the rim)."""
    img = TW._planks(DRIFT, 91, tones=[4, 3, 4, 3], joints=[None, None, None, None], seam_drop=2)
    for y in range(16):
        for x in (0, 15):
            put(img, x, y, DRIFT[2] if x else DRIFT[5])
        put(img, 1, y, DRIFT[4]); put(img, 14, y, DRIFT[2])
    for (x, y) in ((1, 1), (14, 1), (1, 9), (14, 9)):
        put(img, x, y, IRON[2])
    return img


def crop_plot_top():
    """Tilled soil in rows with a driftwood rim (sides of the box)."""
    p = SOIL
    img = new()
    n = noise(13, cell=4, octaves=2)
    for y in range(16):
        for x in range(16):
            row = y % 4
            t = [4, 3, 2, 1][row]
            if n[y][x] > 0.65 and row < 2:
                t += 1
            put(img, x, y, p[t])
    for (x, y) in ((3, 1), (10, 5), (6, 9), (13, 13), (1, 10)):   # pebbles / straw
        put(img, x, y, THATCH[4])
    for i in range(16):
        put(img, i, 0, DRIFT[5]); put(img, 0, i, DRIFT[5]); put(img, i, 15, DRIFT[2]); put(img, 15, i, DRIFT[2])
    for i in range(1, 15):
        put(img, i, 1, DRIFT[3]); put(img, 1, i, DRIFT[3]); put(img, i, 14, DRIFT[1]); put(img, 14, i, DRIFT[1])
    return img


def item_catcher_net():
    """32x32 sheet for the net strings of the item catcher (only thin slices are used): twine colour."""
    from PIL import Image
    img = Image.new('RGBA', (32, 32))
    for y in range(32):
        for x in range(32):
            t = 4 if (x + y) % 3 else 3
            img.putpixel((x, y), ROPE[t])
    return img


# ================================================================================================ model sheets
COCO = pal('#2a170d', '#3f2414', '#5a341c', '#764727', '#925d34', '#ad7845', '#c6955c')


def coconut_lantern():
    """32x16 sheet (UVs are 16 units wide, so 1 texel is half a unit across). Left half: husk of the carved coconut
    with its glowing face (eyes at x 3-4 / 11-12, rows 5-6; mouth x 6-9, row 10 – as in 2.0); row 0 x 16-23: the
    stalk cap; x 24-25 rows 2-4: the hanging cord."""
    from PIL import Image
    img = Image.new('RGBA', (32, 16))
    rnd = random.Random(5)
    for y in range(16):
        for x in range(16):
            t = 3 + (1 if (x // 2 + y) % 5 == 0 else 0) - (1 if (x // 2) % 4 == 3 else 0)
            if y <= 1:
                t += 1                             # lit upper rim
            if y >= 10:
                t -= 1
            if x <= 1:
                t += 1
            if x >= 14:
                t -= 1
            img.putpixel((x, y), clamp(t, COCO))
    glow = EMBER
    face = [(3, 5), (4, 5), (3, 6), (4, 6), (11, 5), (12, 5), (11, 6), (12, 6),
            (6, 10), (7, 10), (8, 10), (9, 10), (5, 9), (10, 9)]
    for (x, y) in face:
        img.putpixel((x, y), glow[6] if y in (6, 10) else glow[5])
    for (x, y) in ((3, 4), (4, 4), (11, 4), (12, 4), (6, 9), (7, 9), (8, 9), (9, 9)):
        img.putpixel((x, y), COCO[0])              # carved edge in shadow above each hole
    for y in range(16):
        for x in range(16, 32):
            img.putpixel((x, y), COCO[2] if (x + y) % 3 else COCO[1])
    for x in range(16, 24):                        # stalk cap
        img.putpixel((x, 0), PALM[3] if x < 20 else PALM[2])
    for y in range(2, 5):                          # cord
        img.putpixel((24, y), ROPE[4]); img.putpixel((25, y), ROPE[2])
    return img


def scarecrow(old):
    """32x32 sheet: TL burlap face (face pixels kept from 2.0), TR straw hat / back of the head, BL straw body,
    BR driftwood pole."""
    from PIL import Image
    img = Image.new('RGBA', (32, 32))
    burlap = pal('#5e4529', '#7a5c38', '#977649', '#b0905d', '#c6a874', '#d9c08f')
    for y in range(16):
        for x in range(16):
            t = 3 if (x + y) % 2 else 4
            if x % 4 == 0 or y % 4 == 0:
                t -= 1                             # coarse weave
            if x + y < 6:
                t += 1
            if x + y > 24:
                t -= 1
            img.putpixel((x, y), clamp(t, burlap))
    for y in range(16):
        for x in range(16):
            c = old.getpixel((x, y))
            if c[3] and lum(c) < 70:
                img.putpixel((x, y), COCO[0] if lum(c) < 45 else COCO[1])   # eyes and stitched mouth
    for y in range(16):
        for x in range(16):
            u = (x + y) % 4
            t = [4, 3, 3, 2][u] + (1 if y < 2 else 0)
            img.putpixel((16 + x, y), clamp(t, THATCH))
    for y in range(16):
        for x in range(16):
            t = 3 + (1 if x % 3 == 0 else 0) - (1 if x % 3 == 2 else 0) + (1 if y % 5 == 0 and x % 3 == 1 else 0)
            img.putpixel((x, 16 + y), clamp(t, THATCH))
    pole = bark(DRIFT_BARK, 61, furrows=[(4, 0, {3}), (11, 6, {12})], base=4, cracks_dark=1)
    img.alpha_composite(pole, (16, 16))
    return img
