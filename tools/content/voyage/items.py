"""16x16 item sprites of the voyage module (pixel art drawn from character grids, outlined like vanilla items)."""
from PIL import Image

from bclib import tex

M = 'voyage'

PAL = {
    # wood (mast, spars, posts)
    '1': '#4f3a22', '2': '#6f5230', '3': '#8f6c3e', '4': '#b08950', '5': '#cfa96b',
    # canvas and red stripe
    'w': '#f2ede1', 'v': '#d8cfbb', 'u': '#b3a88f', 'r': '#c9473b', 'R': '#8f2f29',
    # rope
    'y': '#dcc083', 'Y': '#a98a50', 'Z': '#7a6238',
    # iron / steel
    'i': '#b4bcc6', 'I': '#828a95', 'j': '#59606b', 'J': '#3b4048',
    # brass
    'g': '#ecc35a', 'G': '#b08530',
    # lantern glass
    'l': '#fff4c0', 'L': '#ffd468', 'm': '#ef9f35',
    # deck (driftwood planks) and logs
    'a': '#b9a27a', 'b': '#937d5a', 'c': '#6e5c41', 'n': '#6e5a40', 'N': '#4d3f2d', 'e': '#c7ab7b',
    # crate (light pine)
    'p': '#d4ae74', 'q': '#b58c55', 's': '#6e5230',
    # engine cowl
    'h': '#e2574a', 'H': '#b23a30', 'k': '#7c2620',
}

ART = {
    'expedition_raft': [
        '................',
        '.......52.......',
        '..444444444444..',
        '..wwwwwrRwwwwv..',
        '..wwwwwrRwwwwv..',
        '..wwwwwrRwwwvv..',
        '..vwwwwrRwwwvv..',
        '..vvwwwrRwwvvu..',
        '...uvvvrRvvuu...',
        '...3333532333...',
        '.......52..gqG..',
        '.aaaaaaaaaaaaaa.',
        '.bbbcbbbbbcbbbb.',
        '.nnYnnnnnnnnYnn.',
        '..NZNNNNNNNNZN..',
        '................',
    ],
    'raft_sail': [
        '................',
        '.............54.',
        '............542.',
        '..........ww42..',
        '.........wwR2...',
        '........wwwr2...',
        '.......wwwwr2...',
        '......wwwwwv2...',
        '.....vwwwwv42...',
        '....uvvwwvv42...',
        '.....uuvvuu2....',
        '......yYy.42....',
        '.........42.....',
        '........42......',
        '.......42.......',
        '................',
    ],
    'raft_engine': [
        '................',
        '.....hhhhh......',
        '....hhhhhhH.....',
        '...hhhhhhhhH....',
        '...hwwwwwwwH....',
        '...HHHHHHHHk....',
        '.jjjHHHHHHk.....',
        '.J..kjiIjk......',
        '.....jiIj.......',
        '......iI........',
        '......iI........',
        '....jjiIjjj.....',
        '......iI...gG...',
        '....jIiIIIjgg...',
        '.....jjjjj.gG...',
        '................',
    ],
    'raft_anchor': [
        '................',
        '.......jj.......',
        '......j..j......',
        '......j..jyY....',
        '.......ijyy.Y...',
        '....iiiiIIIj.Y..',
        '.......iI....Y..',
        '.......iI...Y...',
        '.......iI.......',
        '.i.....iI.....j.',
        '.ii....iI....jj.',
        '..ii...iI...jj..',
        '...iiIIiIIjjj...',
        '.....IIIjjj.....',
        '................',
        '................',
    ],
    'raft_crate': [
        '................',
        '................',
        '...sssssssssss..',
        '..sppppyZppppqs.',
        '.sppppyZppppqqs.',
        '.ssssssYZssssss.',
        '.sqpppsYZspppqs.',
        '.sppqqsYZsqqpps.',
        '.sYYYYYYZYYYYYs.',
        '.sZZZZZYZZZZZZs.',
        '.spqqpsYZspqqps.',
        '.spppssYZssppqs.',
        '.sqqpssYZsspqps.',
        '.ssssssssssssss.',
        '................',
        '................',
    ],
    'raft_lantern': [
        '................',
        '.......444444...',
        '.......2....J...',
        '.......4...jjj..',
        '.......4..jLlLj.',
        '.......4..jlllj.',
        '.......4..jLlLj.',
        '.......4..jmLmj.',
        '.......4...jjj..',
        '.......4........',
        '.......4........',
        '.......4........',
        '.......4........',
        '.....y343y......',
        '.....2222.......',
        '................',
    ],
}


def draw(name):
    rows = ART[name]
    img = tex.new()
    for y, row in enumerate(rows):
        assert len(row) == 16, (name, y, row)
        for x, ch in enumerate(row):
            if ch != '.':
                tex.px(img, x, y, PAL[ch])
    return tex.outline(img)


def generate():
    out = {}
    for name in ART:
        img = draw(name)
        tex.save(img, M, 'item/' + name)
        out[name] = img
    return out
