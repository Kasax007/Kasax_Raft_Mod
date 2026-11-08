"""Pixel art of the food module: crops (4 stages), wild plants, food sprites, cooking pot and fish trap."""
import math

from PIL import Image

from bclib import tex
from content.decor import art as DA
from content.decor.art import pal

M = 'food'

LEAF = pal('#1d4220', '#285a2a', '#357236', '#468c41', '#5ea653', '#7cc06a')
STEM = pal('#3b4a1d', '#4d6224', '#627c2d', '#7a9739')
PURPLE = pal('#3d1830', '#57223f', '#743352', '#93486a', '#b06a88')     # sweet potato skin
ORANGE_FLESH = pal('#9a4a10', '#c46418', '#e2862a', '#f4a94a', '#fccb7c')
RED = pal('#5c1010', '#851a14', '#b0281c', '#d6402a', '#ef6a46', '#fb9c78')
YELLOW = pal('#7a5a10', '#a8801c', '#d4aa2c', '#ecc94a', '#f8e27e', '#fff3b8')
BANANA_LEAF = pal('#1f4a24', '#2a632d', '#3a7e38', '#4f9a45', '#6cb558')
RICE_GREEN = pal('#3c5a22', '#52742c', '#6a8f38', '#86ab48')
GOLD = pal('#7a5c22', '#a07e30', '#c4a044', '#dcbd62', '#efd88c')
SEA_GREEN = pal('#164a2e', '#1f6a3c', '#2c8a4c', '#44aa60', '#6cc87c', '#a2e4a6')
RICE_WHITE = pal('#a8a294', '#c8c2b2', '#e0dccd', '#f0ede2', '#fbf9f2')
BOWL = pal('#3e2614', '#55361d', '#6e4827', '#8a5d33')
CREAM = pal('#b8a07a', '#d4bf98', '#e8d8b8', '#f6ecd6', '#fffaf0')
CURRY = pal('#7a4210', '#a05a16', '#c47820', '#e09a32', '#f2bc5a')
CHOWDER = pal('#8c8270', '#aea38e', '#cbc1aa', '#e2dac6', '#f2ecdc')
SOUP = pal('#8a3c10', '#b45418', '#d87226', '#f0943c', '#fbb868')
IRON = DA.IRON
WICKER = DA.WICKER


def img(w=16, h=16):
    return Image.new('RGBA', (w, h), (0, 0, 0, 0))


def P(im, x, y, c):
    if 0 <= x < im.width and 0 <= y < im.height:
        im.putpixel((int(x), int(y)), tex.rgb(c))


def save(im, rel):
    return tex.save(im, M, rel)


def blob(im, cx, cy, rx, ry, ramp, light=(-1, -1)):
    """Shaded ellipse: brighter towards the light (top left)."""
    n = len(ramp)
    for y in range(int(cy - ry - 1), int(cy + ry + 2)):
        for x in range(int(cx - rx - 1), int(cx + rx + 2)):
            dx, dy = (x + 0.5 - cx) / rx, (y + 0.5 - cy) / ry
            d = dx * dx + dy * dy
            if d <= 1.0:
                lit = -(dx * light[0] + dy * light[1]) * 0.5 + 0.5      # 0..1
                k = int(round((n - 2) * (0.25 + 0.75 * lit) * (1.0 - 0.35 * d)))
                if d > 0.75:
                    k = max(0, k - 1)
                P(im, x, y, ramp[max(0, min(n - 1, k))])


def outline(im):
    return tex.outline(im)


# ------------------------------------------------------------------------------------------------- crops
def _stems(im, xs, top, ramp=STEM):
    for x in xs:
        for y in range(top, 16):
            P(im, x, y, ramp[2] if (y + x) % 3 else ramp[1])


def _leaf(im, x, y, dx, length, ramp=LEAF):
    for k in range(length):
        P(im, x + dx * k, y - k // 2, ramp[3 if k < length - 1 else 4])
        P(im, x + dx * k, y - k // 2 + 1, ramp[2])


def sweet_potato_stage(s):
    im = img()
    h = [11, 8, 5, 4][s]
    _stems(im, (4, 8, 12) if s > 0 else (7, 9), h + 2)
    for (x, y) in ((4, h + 2), (8, h), (12, h + 2)) if s > 0 else ((7, h + 1), (9, h + 2)):
        _leaf(im, x, y + 1, -1, 3 + s)
        _leaf(im, x, y + 2, 1, 3 + s)
        P(im, x, y, LEAF[5])
    if s == 3:
        for x in (3, 9):
            blob(im, x + 1.5, 14.5, 2.2, 1.4, PURPLE)
    return im


def chili_stage(s):
    """A small pepper bush: leaves in pairs along two stems, peppers hanging below the leaves when grown."""
    im = img()
    top = [11, 8, 5, 3][s]
    for x0 in (6, 9):
        for y in range(top + 1 + (x0 == 9), 16):
            P(im, x0 + (1 if y < top + 4 and x0 == 9 else 0), y, STEM[2] if y % 3 else STEM[1])
    leaves = [(5, top + 2), (10, top + 1), (4, top + 5), (11, top + 4), (5, top + 8), (10, top + 8)]
    for k, (x, y) in enumerate(leaves):
        if y > 14 or k >= 2 + 2 * s:
            continue
        blob(im, x + 0.5, y + 0.5, 2.3, 1.2, LEAF[1:])
    if s >= 2:
        col = RED if s == 3 else pal('#2a5a1a', '#3a7424', '#4f9030', '#6aac40')
        for x, y in ((3, top + 4), (12, top + 3), (7, top + 7), (9, top + 9)):
            if y + 3 > 15:
                continue
            P(im, x, y - 1, STEM[3])
            for k in range(3):
                P(im, x, y + k, col[3 if k == 0 else 2 if k == 1 else 1])
            P(im, x + 1, y, col[1])
    return im


BANANA_TRUNK = pal('#2e5a24', '#3e7230', '#4f8a3a')


def _banana_leaf(im, x0, y0, d, n, a, b):
    """A broad banana leaf from (x0, y0) going sideways (d = -1/+1) for n px: it rises with slope a and droops by
    b * i^2; lit edge on top, shaded edge below."""
    for i in range(n):
        yc = y0 - a * i + b * i * i
        w = 1 if i == 0 or i >= n - 2 else 2
        top = int(round(yc - w / 2))
        for j in range(w + 1):
            P(im, x0 + d * i, top + j, BANANA_LEAF[4 if j == 0 else 3 if j < w else 2])


def banana_stage(s):
    """Banana plant: a green pseudo-stem with broad arching leaves; when grown, a hanging bunch of bananas (fingers
    pointing up, as on a real plant) with the purple flower bud below it."""
    im = img()
    top = [12, 8, 5, 4][s]
    for y in range(top, 16):
        P(im, 7, y, BANANA_TRUNK[1])
        P(im, 8, y, BANANA_TRUNK[2] if y % 3 else BANANA_TRUNK[1])
    leaves = {
        0: [(-1, 0, 4, 0.9, 0.12), (1, 0, 4, 0.9, 0.12)],
        1: [(-1, 0, 6, 1.0, 0.13), (1, 0, 6, 1.0, 0.13), (-1, 3, 4, 0.6, 0.1)],
        2: [(-1, 0, 7, 1.0, 0.12), (1, 0, 7, 1.0, 0.12), (-1, 3, 6, 0.6, 0.12), (1, 3, 6, 0.6, 0.12)],
        3: [(-1, 0, 8, 1.0, 0.12), (1, 0, 8, 1.0, 0.12), (-1, 3, 7, 0.6, 0.12), (1, 3, 6, 0.6, 0.12)],
    }[s]
    for d, dy, n, a, b in leaves:
        _banana_leaf(im, 7 if d < 0 else 8, top + dy, d, n, a, b)
    P(im, 8, top - 1, BANANA_LEAF[4])          # the next leaf unfurling
    if s >= 2:
        P(im, 8, top - 2, BANANA_LEAF[3])
    if s == 3:
        bunch = {'s': '#3e5a1e', 'a': '#fbef8a', 'b': '#ecd03e', 'c': '#c8a424', 'g': '#8a9a2a', 'p': '#7a2a4a',
                 'q': '#a8406a'}
        grid(im, ['s.....',
                  '.s....',
                  '.gsgg.',
                  '.abab.',
                  'abcbcg',
                  'bcgcab',
                  'c.sbcc',
                  '...cg.',
                  '...qp.',
                  '...p..'], bunch, 8, 6)
    return im


def rice_stage(s):
    im = img()
    top = [11, 8, 5, 3][s]
    for i, x in enumerate((2, 4, 6, 8, 10, 12, 14)):
        t = top + (i % 3)
        for y in range(t, 16):
            P(im, x + (1 if y < t + 3 and i % 2 else 0), y, RICE_GREEN[2 + (y + i) % 2])
        if s >= 2:
            col = GOLD if s == 3 else RICE_GREEN
            for k in range(3):
                P(im, x + 1 + (k % 2), t + k, col[3 - k % 2])
    return im


def sea_grapes_stage(s):
    """Sea grapes: wavy green stalks with round beads on short stems, more and taller with every stage."""
    im = img()
    stalks = [(4, 11), (8, 9), (12, 12), (6, 13)][:[1, 2, 3, 4][s]]
    grow = [5, 8, 11, 14][s]
    for i, (x, base_top) in enumerate(stalks):
        top = 16 - grow + (i % 2) * 2
        for y in range(top, 16):
            xx = x + (1 if (y // 3) % 2 else 0)
            P(im, xx, y, SEA_GREEN[1])
            if (y - top) % 3 == 1 and y < 15:
                side = 1 if (y // 3 + i) % 2 else -1
                bx = xx + side * 2
                P(im, xx + side, y, SEA_GREEN[2])
                P(im, bx, y, SEA_GREEN[4] if side < 0 else SEA_GREEN[3])
                P(im, bx, y - 1, SEA_GREEN[5] if s == 3 else SEA_GREEN[4])
        P(im, x, top - 1, SEA_GREEN[4])
    return im


# ------------------------------------------------------------------------------------------------- items
# Item sprites in the vanilla style: a clear silhouette with a dark outline, light from the top left, 3-5 tones per
# material and no noise. Dishes sit in the vanilla bowl (same shape and wood tones as minecraft:item/bowl).
def grid(im, rows, colours, x0=0, y0=0):
    """Paints a character map: every character a key of colours ('.' and ' ' stay as they are)."""
    for j, row in enumerate(rows):
        for i, ch in enumerate(row):
            if ch in '. ':
                continue
            P(im, x0 + i, y0 + j, colours[ch])
    return im


def outline_part(im, part, colour=None, f=0.42):
    """Outlines only the given pixels (a set of (x, y)), so the bowl's own outline is not doubled."""
    src = im.copy()
    for (x, y) in sorted(part):
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            xx, yy = x + dx, y + dy
            if 0 <= xx < 16 and 0 <= yy < 16 and src.getpixel((xx, yy))[3] == 0:
                c = colour or tex.darker(src.getpixel((x, y)), f)
                im.putpixel((xx, yy), tex.rgb(c))
    return im


def _painted(im, before):
    return {(x, y) for y in range(16) for x in range(16)
            if im.getpixel((x, y))[3] and im.getpixel((x, y)) != before.getpixel((x, y))}


# the vanilla bowl: R rim outline, K bottom outline, L light wood, M mid wood, S inner shadow, W inner wall, D dark
BOWL_WOOD = {'R': '#3f2b0b', 'K': '#1f1502', 'L': '#714f0f', 'M': '#533909', 'S': '#45320d', 'W': '#3a2706',
             'D': '#302109'}
BOWL_ROWS = [
    '.....RRRRRR.....',
    '...RRccccccRR...',
    '..RccccccccccK..',
    '..RWWccccccWWK..',
    '..RLMSSSSSSMWK..',
    '...DLLLLLLLMK...',
    '....KKMLLMKK....',
    '......KKKK......',
]
# shading of the filling inside the rim: 1 dark .. 4 light (light from the top left)
FILL_SHADE = {
    6: {5: 3, 6: 3, 7: 3, 8: 2, 9: 2, 10: 2},
    7: {3: 3, 4: 4, 5: 4, 6: 3, 7: 3, 8: 3, 9: 2, 10: 2, 11: 2, 12: 1},
    8: {5: 2, 6: 2, 7: 2, 8: 2, 9: 1, 10: 1},
}


def bowl(fill, toppings=(), heap=None):
    """A dish: the vanilla bowl filled with a soup/stew ramp (5 tones), toppings (x, y, colour) on the surface and an
    optional heap (a function drawing food piled above the rim; it gets its own outline)."""
    im = img()
    colours = dict(BOWL_WOOD)
    colours['c'] = fill[2]
    grid(im, BOWL_ROWS, colours, 0, 5)
    for y, row in FILL_SHADE.items():
        for x, k in row.items():
            P(im, x, y, fill[k])
    for x, y, c in toppings:
        P(im, x, y, c)
    if heap:
        before = im.copy()
        heap(im)
        outline_part(im, _painted(im, before) - {(x, y) for y in range(6, 9) for x in range(3, 13)})
    return im


DISH_CURRY = pal('#6e3a0c', '#9c5712', '#c87a1c', '#e49c2e', '#f4c058')
DISH_CHOWDER = pal('#8e8068', '#b8aa8e', '#d8ccb0', '#ece4cc', '#faf5e6')
DISH_SOUP = pal('#8a3e0e', '#bc5a18', '#e07c28', '#f49c42', '#fcc070')
CHILI_SAUCE = pal('#5e120e', '#8c2014', '#ba341e', '#dc502a', '#f07a46')
RICE_GRAIN = pal('#9c968a', '#c4beb0', '#dedacd', '#efece2', '#fbfaf4')
HERB = '#4f8a2a'
HERB_LIGHT = '#78b043'


def island_curry():
    def rice_heap(im):
        grid(im, ['..ab.',
                  '.abcc',
                  'abbcc',
                  'bbcc.'], {'a': RICE_GRAIN[4], 'b': RICE_GRAIN[3], 'c': RICE_GRAIN[2]}, 3, 3)
    return bowl(DISH_CURRY, ((9, 6, '#d23a22'), (10, 7, '#f0d090'), (11, 7, HERB), (8, 7, '#f0d090'), (8, 6, '#a8301c'),
                        (6, 8, '#d23a22'), (9, 8, HERB_LIGHT)), rice_heap)


def seafood_chowder():
    shrimp = {'a': '#ffc09a', 'b': '#f8844e', 'c': '#d2582e'}
    im = bowl(DISH_CHOWDER, ((4, 7, '#7d6e8e'), (5, 7, '#b8aac8'), (5, 6, '#f2e2a0'), (11, 6, HERB), (7, 8, HERB_LIGHT),
                             (6, 7, DISH_CHOWDER[4])))
    grid(im, ['ab.', 'b.c', '.cc'], shrimp, 8, 6)
    P(im, 9, 7, DISH_CHOWDER[3])
    return im


def sweet_potato_soup():
    cream = '#fbf3de'
    return bowl(DISH_SOUP, ((5, 7, cream), (6, 7, cream), (7, 6, cream), (8, 6, '#e8dcc0'), (9, 7, cream), (10, 6, HERB),
                       (11, 6, HERB_LIGHT), (10, 8, DISH_SOUP[0])))


def chili_crab():
    """A whole red crab sitting in chili sauce, claws up."""
    c = {'k': '#2a120c', 'a': '#ffa070', 'b': '#f06a40', 'c': '#c8402a', 'd': '#8e2418', 'w': '#fbe4cc'}

    def crab(im):
        grid(im, ['ab.......ab.',
                  'bc.......bc.',
                  'cbk.k.k.kcd.',
                  '.cbbabbbbcd.',
                  '.bbaabbbccd.',
                  '..cbbbbccd..'], c, 2, 1)
    return bowl(CHILI_SAUCE, ((4, 7, '#fbe4cc'), (12, 7, '#4f8a2a'), (3, 7, '#f2c84a'), (10, 8, '#fbe4cc'),
                              (6, 8, '#4f8a2a')), crab)


def fruit_salad():
    def heap(im):
        grid(im, ['....mmBb....',
                  '..yYmMBbgG..',
                  '.pPyYkKmMyY.',
                  'oOpPkKmMyYgG',
                  'oOyYgGpPkKmM'],
             {'m': '#f0545c', 'M': '#c42c3a', 'B': '#5a5ad0', 'b': '#3a3a9a', 'y': '#fbeaa0', 'Y': '#e6cc6e',
              'g': '#9ccc52', 'G': '#6a9c34', 'p': '#fbcf4a', 'P': '#e0a024', 'k': '#a0d060', 'K': '#5e9030',
              'o': '#f8a048', 'O': '#d87a24'}, 2, 3)
        P(im, 7, 5, '#1e2a12')
        P(im, 10, 7, '#1e2a12')
    return bowl(pal('#5a8a2a', '#7aa83a', '#9cc44e', '#bcdc6a', '#d8f08c'), (), heap)


def coconut_ice_cream():
    """Two scoops of coconut ice cream with toasted coconut flakes and a mint leaf."""
    cream = pal('#b4ab96', '#d6cfbe', '#ebe6da', '#f8f6f0', '#ffffff')

    def scoops(im):
        for cx, cy in ((10.2, 4.4), (5.8, 5.0)):
            for y in range(1, 9):
                for x in range(2, 14):
                    dx, dy = (x + 0.5 - cx) / 2.6, (y + 0.5 - cy) / 2.6
                    d = dx * dx + dy * dy
                    if d <= 1.0 and not (y >= 7 and 3 <= x <= 12):
                        k = 4 if dx + dy < -0.8 else 3 if dx + dy < 0.0 else 2 if dx + dy < 0.7 else 1
                        P(im, x, y, cream[k])
        for x, y in ((5, 4), (7, 6), (9, 3), (11, 5), (4, 6)):
            P(im, x, y, '#c89a64')
        grid(im, ['gG', 'G.'], {'g': '#7cc05a', 'G': '#4f8a34'}, 10, 1)
    return bowl(cream, (), scoops)


def sushi_roll():
    """Two maki pieces in a three-quarter view: rice ring around salmon and cucumber, wrapped in nori."""
    c = {'n': '#1a2e1c', 'N': '#2c4a2c', 'h': '#3e6440', 'w': '#fbfaf4', 'W': '#e2ded0', 'v': '#c4beb0',
         's': '#f48a5a', 'S': '#d8643a', 'g': '#8cc860'}
    im = img()
    grid(im, ['..nnnn..',
              '.nwwwWn.',
              'nwwssgWn',
              'nwWsSWvn',
              'hnwWWvnN',
              'hhnnnnNN',
              'hhhNNNNN',
              '.hhNNNN.'], c, 1, 2)
    grid(im, ['..nnnn..',
              '.nwwwWn.',
              'nwwsgWWn',
              'nwWSsWvn',
              'hnwWWvnN',
              'hhnnnnNN',
              'hhhNNNNN',
              '.hhNNNN.'], c, 7, 6)
    return outline(im)


def banana_bread():
    """A loaf with a domed, split crust and a cut end showing the crumb with banana pieces."""
    c = {'k': '#4a2810', 'd': '#6e3c18', 'm': '#8e5424', 'l': '#ac6e32', 'h': '#c88a48', 'y': '#e2b060',
         'q': '#f2d494', 'Q': '#e4c07a', 'b': '#8a5a28', 'e': '#d8a858'}
    im = img()
    grid(im, ['....hhhhhh......',
              '..hhyyyehhll....',
              '.hlhhhhlllmmq...',
              '.lllllmmmmdqQq..',
              '.mmmmmmmmddqbQq.',
              '.mmmmmmmmddQqqQ.',
              '.dmmmmmmmddqqbQ.',
              '.ddmmmmmmdkQQQq.',
              '..ddddddddkQQQ..',
              '...kkkkkkkk.....'], c, 0, 4)
    return outline(im)


def tropical_smoothie():
    """A glass of sunset smoothie (mango at the bottom, passion fruit on top) with a straw and a pineapple wedge."""
    c = {'g': '#d8eef0', 'G': '#a8c8cc', 'e': '#7a9ca4', 'r': '#f06a5a', 'o': '#f6923a', 'O': '#e07428',
         'y': '#f8c844', 'Y': '#e0a428', 'h': '#fde88a', 'p': '#f2d24a', 'P': '#d8a622', 'l': '#5e9a34',
         'L': '#86c04a', 's': '#f05a8a', 'S': '#c23a6a'}
    im = img()
    grid(im, ['..........s.....',
              '.L........S.....',
              'lLl......s......',
              '.pPp.....S......',
              'ppPPgGGGsGGGG...',
              '.pPPrrrrSrrre...',
              '..pGrhrrrrrOe...',
              '...GohoooooOe...',
              '...GohooooOOe...',
              '...GyhyyyyyYe...',
              '...GyhyyyyYYe...',
              '...GyyyyyyYYe...',
              '...GgggggGGee...',
              '....eeeeeeee....'], c, 1, 1)
    return outline(im)


def baked_sweet_potato():
    """A baked sweet potato split open: dark roasted skin, orange flesh and a pat of butter melting on top."""
    c = {'k': '#3a1626', 'd': '#55223a', 'm': '#74314c', 'l': '#924660', 'o': '#e8862c', 'O': '#c4641c',
         'h': '#f8b450', 'b': '#fff0a0', 'B': '#f0d060', 'w': '#fff8d8'}
    im = img()
    grid(im, ['......wb........',
              '...lllbBlll.....',
              '.lllhhooOOmmm...',
              'lllhhhoooOOOmmm.',
              'mlmmhoooooOmmmmd',
              '.mmmmmmmmmmmmmdd',
              '..dmmmmmmmmmddd.',
              '....ddddddddd...'], c, 0, 4)
    return outline(im)


def cooked_rice():
    """A heap of steamed rice on a banana leaf."""
    c = {'a': RICE_GRAIN[4], 'b': RICE_GRAIN[3], 'c': RICE_GRAIN[2], 'd': RICE_GRAIN[1], 'l': '#5ea648',
         'L': '#7cc05a', 'k': '#3a7a30', 'v': '#2c5e26'}
    im = img()
    grid(im, ['......abba......',
              '....aabbbbcc....',
              '...abababbcbc...',
              '..aabbbbbcbccd..',
              '.Labbabbbbccbdk.',
              'LLlcbbbbbcccddkv',
              '.LLllddddddkkkv.',
              '...LLlllllkkv...'], c, 0, 5)
    return outline(im)


def sweet_potato():
    c = {'a': '#b06a88', 'b': '#93486a', 'c': '#743352', 'd': '#57223f', 'e': '#3d1830', 'r': '#c88a6a'}
    im = img()
    grid(im, ['.....aab........',
              '...aaabbbbc.....',
              '..abbbbbbbccc...',
              '.rbbbcbbbbcccd..',
              '..ccccccccccddde',
              '...dddcdddddde..',
              '.....ddddeee....'], c, 0, 5)
    return outline(im)


def chili_pepper():
    c = {'g': '#86b040', 'G': '#4f7a24', 'a': '#fb8a6a', 'b': '#e84a30', 'c': '#b82a1c', 'd': '#7e1a14'}
    im = img()
    grid(im, ['..gG............',
              '.gGgG...........',
              '..Gbbc..........',
              '..abbbcc........',
              '...abbbccc......',
              '....abbbccc.....',
              '.....aabbccd....',
              '.......abbcd....',
              '........abcd....',
              '.........bcd....',
              '.........ccd....',
              '..........cd....',
              '.........cd.....'], c, 1, 1)
    return outline(im)


def chili_seeds():
    im = img()
    for x, y in ((5, 6), (9, 5), (7, 9), (11, 9), (4, 11), (8, 12)):
        P(im, x, y, '#f6e6a8')
        P(im, x + 1, y, '#dcc480')
        P(im, x, y + 1, '#c4a85e')
        P(im, x + 1, y + 1, '#a88a44')
    return outline(im)


def _bez(p0, p1, p2, t):
    return ((1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * p1[0] + t * t * p2[0],
            (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * p1[1] + t * t * p2[1])


BANANA_PEEL = pal('#8a6414', '#c08f1c', '#e2b52e', '#f4d24a', '#fde77e', '#fff6c0')


def banana(p0=(2.0, 9.5), p1=(10.5, 13.0), p2=(13.0, 2.5), rmax=2.3, rmin=0.55, peel=BANANA_PEEL, outline_col='#4e3510'):
    """A ripe banana lying as a crescent from the tip (bottom left) to the stalk (top right), the curve bulging
    to the bottom right; the inner side catches the light."""
    im = img()
    pts = [_bez(p0, p1, p2, i / 400) for i in range(401)]
    for y in range(16):
        for x in range(16):
            cx, cy = x + 0.5, y + 0.5
            d, i = min(((cx - px) ** 2 + (cy - py) ** 2, i) for i, (px, py) in enumerate(pts))
            t = i / 400
            r = rmin + (rmax - rmin) * math.sin(math.pi * min(1.0, t * 1.05)) ** 0.8
            if math.sqrt(d) > r:
                continue
            j = min(399, i)
            tx, ty = pts[j + 1][0] - pts[j][0], pts[j + 1][1] - pts[j][1]
            nx, ny = ty, -tx
            s = ((cx - pts[i][0]) * nx + (cy - pts[i][1]) * ny) / (math.hypot(nx, ny) or 1) / r
            k = 5 if s > 0.55 else 4 if s > 0.1 else 3 if s > -0.35 else 2 if s > -0.75 else 1
            col = peel[k]
            if t < 0.07:
                col = tex.rgb('#4a3416')
            elif t > 0.93:
                col = tex.rgb('#7d6a2a') if s > 0 else tex.rgb('#5e4c1e')
            elif t > 0.86:
                col = tex.rgb('#c9c24a') if s > 0 else tex.rgb('#a8a032')
            im.putpixel((x, y), col)
    return tex.outline(im, outline_col) if outline_col else im


def banana_shoot():
    im = img()
    for y in range(9, 15):
        P(im, 7, y, BANANA_LEAF[1])
        P(im, 8, y, BANANA_LEAF[2])
    for i in range(5):
        P(im, 7 - i, 9 - i // 2, BANANA_LEAF[3])
        P(im, 8 + i, 8 - i // 2, BANANA_LEAF[4])
        P(im, 7 - i, 10 - i // 2, BANANA_LEAF[2])
        P(im, 8 + i, 9 - i // 2, BANANA_LEAF[3])
    blob(im, 7.5, 13.5, 2.5, 1.8, pal('#4a3018', '#664424', '#825a30', '#9c7040'))
    return outline(im)


def rice():
    im = img()
    for i, (x0, y0) in enumerate(((5, 3), (8, 2), (11, 4))):
        for y in range(y0, 15):
            P(im, x0 + (y - y0) // 6, y, RICE_GREEN[2] if y > y0 + 5 else GOLD[2])
        for k in range(0, 6, 2):
            P(im, x0 - 1, y0 + k + 1, GOLD[4])
            P(im, x0 + 1, y0 + k, GOLD[3])
    return outline(im)


def sea_grapes():
    im = img()
    for y in range(3, 15):
        P(im, 7 + (y > 9), y, SEA_GREEN[1])
    for y in range(3, 13, 2):
        for dx in (-1, 1, 2):
            if dx == 2 and y % 4:
                continue
            blob(im, 7.5 + dx * 1.6 + (y > 9), y + 0.5, 1.2, 1.2, SEA_GREEN[1:])
    return outline(im)


# ------------------------------------------------------------------------------------------------- blocks
def pot_side():
    im = DA.metal(IRON, seed=9)
    for x in range(16):
        P(im, x, 0, IRON[5])
        P(im, x, 1, IRON[4])
        P(im, x, 15, IRON[1])
    return im


def pot_inside():
    return tex.noise_fill(img(), IRON[0:3], 4, cell=4, octaves=1, contrast=0.6)


def surface(ramp, bubbles=True):
    im = img()
    for y in range(16):
        for x in range(16):
            k = 2 + (1 if (x * 3 + y * 5) % 11 == 0 else 0)
            P(im, x, y, ramp[k])
    if bubbles:
        for x, y in ((4, 5), (10, 3), (7, 10), (12, 11)):
            P(im, x, y, ramp[4])
            P(im, x + 1, y, ramp[3])
    return im


WATER = pal('#1e4f8a', '#2a64a6', '#3a7cc0', '#5a9ad4', '#8cc0e8')


def trap_cage():
    """Wicker fish trap: open weave with gaps (cutout)."""
    im = img()
    for y in range(16):
        for x in range(16):
            if x % 4 == 0 or y % 5 == 0:
                k = 3 if (x + y) % 2 else 2
                if x % 4 == 0 and y % 5 == 0:
                    k = 4
                P(im, x, y, WICKER[k])
    return im


def trap_end():
    """Funnel end of the trap: rings of wicker around the opening."""
    im = img()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 2.5:
                continue
            if int(d) % 2 == 0 or (int(math.degrees(math.atan2(y - 7.5, x - 7.5))) // 30) % 2 == 0:
                P(im, x, y, WICKER[3 if int(d) % 2 == 0 else 2])
    return im
