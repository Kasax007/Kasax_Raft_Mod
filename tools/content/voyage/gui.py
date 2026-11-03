"""GUI of the Expedition Raft storage (vanilla container style, 256x256).

Layout (matches ExpeditionRaftScreen / ExpeditionRaftMenu):
- v 0..124: top part, 176 wide main panel with 6 rows of slots (the screen draws rows*18+17 of it) and the engine tab
  at x 172..209, y 0..70 (fuel slot at 186,36, flame at 187,19, anchor lever button at 179,55).
- v 126..221: bottom part (player inventory), 176 wide.
- sprites: lit flame 212,0 (14x14), closed fuel hatch 230,0 (22x36), disabled anchor plate 212,40 (26x14).
"""
from PIL import Image

from bclib import tex

M = 'voyage'
BG = (198, 198, 198, 255)
WHITE = (255, 255, 255, 255)
DARK = (85, 85, 85, 255)
BLACK = (0, 0, 0, 255)
SLOT = (139, 139, 139, 255)
SLOT_DARK = (55, 55, 55, 255)


def _rect(img, x0, y0, x1, y1, c):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if 0 <= x < img.width and 0 <= y < img.height:
                img.putpixel((x, y), c)


def slot(img, x, y):
    """18x18 slot background with its top-left corner at x, y."""
    _rect(img, x, y, x + 17, y + 17, SLOT)
    _rect(img, x, y, x + 16, y, SLOT_DARK)
    _rect(img, x, y, x, y + 16, SLOT_DARK)
    _rect(img, x + 1, y + 17, x + 17, y + 17, WHITE)
    _rect(img, x + 17, y + 1, x + 17, y + 17, WHITE)


def panel(img, x0, y0, x1, y1, top=True, bottom=True):
    """Vanilla window panel: black outline, white/grey bevel, light grey face (open ends for joined parts)."""
    _rect(img, x0, y0, x1, y1, BG)
    _rect(img, x0, y0, x0, y1, BLACK)
    _rect(img, x1, y0, x1, y1, BLACK)
    _rect(img, x0 + 1, y0, x0 + 2, y1, WHITE)
    _rect(img, x1 - 2, y0, x1 - 1, y1, DARK)
    if top:
        _rect(img, x0 + 2, y0, x1 - 2, y0, BLACK)
        _rect(img, x0 + 1, y0 + 1, x1 - 3, y0 + 2, WHITE)
        img.putpixel((x0, y0), (0, 0, 0, 0))
        img.putpixel((x1, y0), (0, 0, 0, 0))
        img.putpixel((x0 + 1, y0), BLACK)
        img.putpixel((x1 - 1, y0), BLACK)
        img.putpixel((x1 - 2, y0 + 1), BG)
    if bottom:
        _rect(img, x0 + 2, y1, x1 - 2, y1, BLACK)
        _rect(img, x0 + 3, y1 - 2, x1 - 1, y1 - 1, DARK)
        img.putpixel((x0, y1), (0, 0, 0, 0))
        img.putpixel((x1, y1), (0, 0, 0, 0))
        img.putpixel((x0 + 1, y1), BLACK)
        img.putpixel((x1 - 1, y1), BLACK)
        img.putpixel((x0 + 2, y1 - 1), BG)


def flame(img, ox, oy, lit):
    """14x14 furnace-like flame; lit = orange/yellow, else the grey empty outline."""
    shape = [
        '......##......',
        '.....####.....',
        '.....####.....',
        '....######....',
        '....######.#..',
        '.#..#######...',
        '..#.########..',
        '..##########..',
        '.############.',
        '.#####oo#####.',
        '.####oyyo####.',
        '.####oyyyo###.',
        '..###oyyo###..',
        '...########...',
    ]
    for y, row in enumerate(shape):
        for x, ch in enumerate(row):
            if ch == '.':
                continue
            if lit:
                c = {'#': (232, 104, 32, 255), 'o': (255, 170, 40, 255), 'y': (255, 236, 130, 255)}[ch]
                if ch == '#' and y < 5:
                    c = (210, 70, 30, 255)
            else:
                c = (139, 139, 139, 255) if ch == '#' else (150, 150, 150, 255)
            img.putpixel((ox + x, oy + y), c)


def anchor_icon(img, ox, oy, c):
    pts = ['..##..', '.#..#.', '..##..', '######', '..##..', '..##..', '#.##.#', '.####.']
    for y, row in enumerate(pts):
        for x, ch in enumerate(row):
            if ch == '#':
                img.putpixel((ox + x, oy + y), c)


def generate():
    img = Image.new('RGBA', (256, 256), (0, 0, 0, 0))
    # engine tab (drawn first, the main panel's right edge is opened where they join)
    panel(img, 172, 0, 209, 70)
    # engine bay inset
    _rect(img, 181, 15, 206, 53, (170, 170, 170, 255))
    _rect(img, 181, 15, 206, 15, DARK)
    _rect(img, 181, 15, 181, 53, DARK)
    _rect(img, 182, 53, 206, 53, WHITE)
    _rect(img, 206, 16, 206, 53, WHITE)
    flame(img, 187, 19, False)
    slot(img, 185, 35)
    # tiny propeller icon above the engine bay
    for x, y in ((191, 5), (192, 6), (193, 7), (194, 8), (195, 9), (195, 5), (194, 6), (192, 8), (191, 9)):
        img.putpixel((x, y), (110, 110, 110, 255))
    # main panel, top part: 6 rows of slots
    panel(img, 0, 0, 175, 124, top=True, bottom=False)
    for row in range(6):
        for col in range(9):
            slot(img, 7 + col * 18, 17 + row * 18)
    # join: open the main panel's right border along the tab
    _rect(img, 172, 3, 176, 66, BG)
    # bottom part (player inventory)
    panel(img, 0, 126, 175, 221, top=False, bottom=True)
    for row in range(3):
        for col in range(9):
            slot(img, 7 + col * 18, 126 + 13 + row * 18)
    for col in range(9):
        slot(img, 7 + col * 18, 126 + 71)
    # sprites
    flame(img, 212, 0, True)
    # closed fuel hatch (no engine installed)
    hatch = (150, 128, 96, 255)
    _rect(img, 230, 0, 251, 35, hatch)
    for y in range(0, 36, 6):
        _rect(img, 231, y, 250, y, (118, 98, 70, 255))
    _rect(img, 230, 0, 230, 35, (96, 78, 54, 255))
    _rect(img, 251, 0, 251, 35, (96, 78, 54, 255))
    for (x, y) in ((233, 3), (248, 3), (233, 32), (248, 32)):
        img.putpixel((x, y), (70, 70, 74, 255))
    # disabled anchor plate
    _rect(img, 212, 40, 237, 53, (150, 150, 150, 255))
    _rect(img, 212, 40, 237, 40, (110, 110, 110, 255))
    _rect(img, 212, 53, 237, 53, (200, 200, 200, 255))
    anchor_icon(img, 222, 43, (120, 120, 120, 255))
    tex.save(img, M, 'gui/expedition_raft')
    return img
