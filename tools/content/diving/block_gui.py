"""Air compressor: block textures + model elements, its GUI texture, and the oxygen HUD sprites."""
import math
import os

from bclib import paths, tex

from content.diving import art
from content.diving.art import BRASS, COPPER, STEEL, DARK_STEEL, RED, WHITE, TEAL, GLASS, YELLOW, RUBBER, GAUGE_FACE

CLEAR = (0, 0, 0, 0)


# ----------------------------------------------------------------------------------------------- block textures
def tank_side():
    """Side of the red receiver tank (a cylinder lying along x): light band on top, rivet seams, white stripe."""
    img = tex.new()
    art.cyl_h(img, 0, 15, 0, 15, RED, bias=0.05)
    for x in range(16):
        tex.px(img, x, 5, WHITE[2])
        tex.px(img, x, 6, WHITE[1])
    for x in range(1, 16, 3):
        tex.px(img, x, 4, RED[4]); tex.px(img, x, 13, RED[1])
    # brass name plate
    tex.rect(img, 9, 8, 13, 10, BRASS[3])
    tex.rect(img, 10, 9, 12, 9, BRASS[1])
    tex.px(img, 9, 8, BRASS[5])
    return img


def tank_end():
    """Domed end cap: concentric rings and a ring of rivets."""
    img = tex.new()
    art.face_shade(img, 0, 0, 16, 16, RED, curve=0.9)
    art.ring(img, 8, 7.5, 5.2, 4.2, RED, bias=0.15)
    for a in range(0, 360, 45):
        x = int(round(8 + 4.7 * math.cos(math.radians(a)) - 0.5))
        y = int(round(7.5 + 4.7 * math.sin(math.radians(a)) - 0.5))
        tex.px(img, x, y, RED[4])
    tex.rect(img, 7, 7, 8, 8, DARK_STEEL[2])
    return img


def tank_top():
    img = tex.new()
    for y in range(16):
        for x in range(16):
            tex.px(img, x, y, art.pick(RED, 0.82 - 0.04 * abs(y - 7)))
    for y in range(16):
        tex.px(img, 5, y, WHITE[3]); tex.px(img, 6, y, WHITE[2])
    for y in range(1, 16, 3):
        tex.px(img, 1, y, RED[4]); tex.px(img, 14, y, RED[2])
    return img


def base():
    """Steel skid: diamond tread plate on top, channel with bolt holes on the sides."""
    img = tex.new()
    for y in range(16):
        for x in range(16):
            tex.px(img, x, y, art.pick(DARK_STEEL, 0.55 - 0.15 * ((x + y) / 30)))
    for y in range(0, 16, 4):
        for x in range(0, 16, 4):
            ox = 2 if (y // 4) % 2 else 0
            tex.px(img, (x + ox) % 16, y + 1, DARK_STEEL[4])
            tex.px(img, (x + ox + 1) % 16, y + 2, DARK_STEEL[3])
    return img


def base_side():
    img = tex.new()
    for y in range(16):
        for x in range(16):
            v = 0.6 if y in (14,) else (0.35 if y == 15 else 0.5)
            tex.px(img, x, y, art.pick(DARK_STEEL, v - 0.06 * (y % 2)))
    for x in range(2, 16, 4):
        tex.px(img, x, 14, DARK_STEEL[0])
    return img


def pump():
    """Steel pump head with cooling fins."""
    img = tex.new()
    for y in range(16):
        fin = y % 2 == 0
        for x in range(16):
            v = (0.75 if fin else 0.35) - 0.2 * (x / 15)
            tex.px(img, x, y, art.pick(STEEL, v))
    return img


def pump_top():
    img = tex.new()
    art.face_shade(img, 0, 0, 16, 16, STEEL, bias=0.1, curve=0.4)
    for (x, y) in ((4, 4), (11, 4), (4, 11), (11, 11)):
        tex.px(img, x, y, STEEL[5])
    tex.rect(img, 6, 6, 9, 9, DARK_STEEL[2])
    return img


def brass():
    img = tex.new()
    art.face_shade(img, 0, 0, 16, 16, BRASS, curve=0.5)
    return img


def wheel():
    """Red hand wheel with four spokes and a brass hub (end of the crank)."""
    img = tex.new()
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - 8, y + 0.5 - 8
            r = math.hypot(dx, dy)
            if 5.0 <= r <= 7.2:
                tex.px(img, x, y, art.pick(RED, 0.6 - 0.04 * dy - 0.03 * dx))
            elif r < 5.0 and (abs(dx) < 0.8 or abs(dy) < 0.8):
                tex.px(img, x, y, RED[1])
            elif r < 5.0:
                tex.px(img, x, y, DARK_STEEL[1])
    tex.rect(img, 7, 7, 8, 8, BRASS[4])
    return img


def gauge(angle_deg, lamp=False):
    """16x16 texture with the 6x6 gauge at [5,5]-[11,11]: brass bezel, cream dial, red needle."""
    img = tex.new()
    art.face_shade(img, 0, 0, 16, 16, BRASS, curve=0.3)
    for y in range(5, 11):
        for x in range(5, 11):
            corner = x in (5, 10) and y in (5, 10)
            tex.px(img, x, y, BRASS[3] if corner else GAUGE_FACE[2])
    tex.px(img, 6, 6, GAUGE_FACE[3])
    # red zone
    tex.px(img, 9, 6, RED[3]); tex.px(img, 9, 7, RED[2])
    # needle from centre (7.5, 8)
    a = math.radians(angle_deg)
    for r in (0.0, 1.0, 1.9):
        x = int(round(7.5 + math.sin(a) * r - 0.5))
        y = int(round(8.0 - math.cos(a) * r - 0.5))
        tex.px(img, x, y, '#2a1e18' if r == 0 else RED[1])
    if lamp:
        tex.px(img, 10, 10, '#7af07a')
    return img


def block_textures():
    off = gauge(-60)
    on_frames = [gauge(a, lamp=(i % 2 == 0)) for i, a in enumerate((40, 55, 48, 62, 45, 58))]
    return {
        'block/air_compressor_tank': tank_side(),
        'block/air_compressor_tank_end': tank_end(),
        'block/air_compressor_tank_top': tank_top(),
        'block/air_compressor_base': base(),
        'block/air_compressor_base_side': base_side(),
        'block/air_compressor_pump': pump(),
        'block/air_compressor_pump_top': pump_top(),
        'block/air_compressor_brass': brass(),
        'block/air_compressor_wheel': wheel(),
        'block/air_compressor_gauge': off,
    }, on_frames


def _face(tex_key, uv, cull=None):
    d = {'uv': uv, 'texture': '#' + tex_key}
    if cull:
        d['cullface'] = cull
    return d


def _el(frm, to, faces):
    return {'from': frm, 'to': to, 'faces': faces}


def model_elements():
    """Block model elements (facing north = front with the gauge)."""
    e = []
    # skid
    e.append(_el([0, 0, 0], [16, 2, 16], {
        'down': _face('base', [0, 0, 16, 16], 'down'), 'up': _face('base', [0, 0, 16, 16]),
        'north': _face('base_side', [0, 14, 16, 16], 'north'), 'south': _face('base_side', [0, 14, 16, 16], 'south'),
        'west': _face('base_side', [0, 14, 16, 16], 'west'), 'east': _face('base_side', [0, 14, 16, 16], 'east')}))
    # receiver tank 14x9x10
    e.append(_el([1, 2, 3], [15, 11, 13], {
        'north': _face('tank', [1, 4, 15, 13]), 'south': _face('tank', [1, 4, 15, 13]),
        'west': _face('end', [3, 3, 13, 12]), 'east': _face('end', [3, 3, 13, 12]),
        'up': _face('top', [1, 3, 15, 13]), 'down': _face('base', [1, 3, 15, 13])}))
    # pump head with fins
    e.append(_el([3, 11, 5], [10, 15, 11], {
        'north': _face('pump', [3, 1, 10, 5]), 'south': _face('pump', [3, 1, 10, 5]),
        'west': _face('pump', [5, 1, 11, 5]), 'east': _face('pump', [5, 1, 11, 5]),
        'up': _face('pump_top', [4, 5, 11, 11])}))
    # outlet valve (brass) on top
    e.append(_el([11, 11, 6], [13, 14, 8], {
        'north': _face('brass', [0, 0, 2, 3]), 'south': _face('brass', [2, 0, 4, 3]),
        'west': _face('brass', [4, 0, 6, 3]), 'east': _face('brass', [6, 0, 8, 3]), 'up': _face('brass', [8, 0, 10, 2])}))
    e.append(_el([10, 13, 5], [14, 14, 9], {
        'north': _face('wheel', [6, 1, 10, 2]), 'south': _face('wheel', [6, 1, 10, 2]),
        'west': _face('wheel', [6, 1, 10, 2]), 'east': _face('wheel', [6, 1, 10, 2]),
        'up': _face('wheel', [6, 6, 10, 10]), 'down': _face('wheel', [6, 6, 10, 10])}))
    # pressure gauge on the front
    e.append(_el([5, 4, 2], [11, 10, 3], {
        'north': _face('gauge', [5, 5, 11, 11]), 'west': _face('brass', [0, 0, 1, 6]),
        'east': _face('brass', [1, 0, 2, 6]), 'up': _face('brass', [0, 0, 6, 1]), 'down': _face('brass', [0, 1, 6, 2])}))
    # hand wheel (crank) on the east end
    e.append(_el([15, 3, 5], [16, 10, 12], {
        'east': _face('wheel', [4.5, 4.5, 11.5, 11.5]), 'west': _face('wheel', [4.5, 4.5, 11.5, 11.5]),
        'north': _face('wheel', [8, 1, 9, 8]), 'south': _face('wheel', [8, 1, 9, 8]),
        'up': _face('wheel', [8, 1, 9, 8]), 'down': _face('wheel', [8, 1, 9, 8])}))
    # crank handle knob
    e.append(_el([16, 8, 10], [17, 9, 11], {
        'east': _face('brass', [0, 0, 1, 1]), 'north': _face('brass', [1, 0, 2, 1]),
        'south': _face('brass', [2, 0, 3, 1]), 'up': _face('brass', [3, 0, 4, 1]), 'down': _face('brass', [4, 0, 5, 1])}))
    return e


# ----------------------------------------------------------------------------------------------- GUI
BG = (198, 198, 198, 255)
DARK = (55, 55, 55, 255)
LIGHT_EDGE = (255, 255, 255, 255)
SLOT_IN = (139, 139, 139, 255)


def _inset(img, x, y, w, h):
    """Vanilla-style sunken frame (dark top-left, white bottom-right)."""
    tex.rect(img, x, y, x + w - 1, y + h - 1, SLOT_IN)
    tex.line(img, x, y, x + w - 2, y, DARK)
    tex.line(img, x, y, x, y + h - 2, DARK)
    tex.line(img, x + 1, y + h - 1, x + w - 1, y + h - 1, LIGHT_EDGE)
    tex.line(img, x + w - 1, y + 1, x + w - 1, y + h - 1, LIGHT_EDGE)


def _crank_frame(angle, hovered):
    img = tex.new(20, 20)
    for y in range(20):
        for x in range(20):
            dx, dy = x + 0.5 - 10, y + 0.5 - 10
            r = math.hypot(dx, dy)
            if 6.5 <= r <= 9.0:
                tex.px(img, x, y, art.pick(BRASS, 0.62 - 0.03 * dy - 0.02 * dx + (0.15 if hovered else 0.0)))
            elif r < 6.5:
                a = math.atan2(dy, dx) - math.radians(angle)
                spoke = min(abs(math.sin(a)), abs(math.cos(a))) * r < 0.9
                if spoke:
                    tex.px(img, x, y, BRASS[2] if not hovered else BRASS[3])
    # hub
    tex.rect(img, 9, 9, 10, 10, BRASS[5])
    # handle knob on the rim at the current angle
    a = math.radians(angle)
    hx, hy = 10 + 7.6 * math.cos(a), 10 + 7.6 * math.sin(a)
    tex.ellipse(img, int(hx) - 2, int(hy) - 2, int(hx) + 1, int(hy) + 1, RED[3] if not hovered else RED[4])
    tex.px(img, int(hx) - 1, int(hy) - 1, RED[4])
    return tex.outline(img, (60, 44, 20, 255))


def gui_texture():
    """176x166 container with the slot, fill bar, gauge, energy column and crank recess; sprites to the right."""
    # the mod's own container background (same frame and inventory grid as the water purifier)
    base = tex.load(os.path.join(paths.RESOURCES, 'assets', 'raft', 'textures', 'gui', 'water_purifier_gui.png'))
    img = tex.new(256, 256)
    img.alpha_composite(base.crop((0, 0, 176, 166)), (0, 0))
    # clear the furnace parts
    tex.rect(img, 7, 14, 168, 78, BG)
    # slot
    _inset(img, 43, 34, 18, 18)
    # small valve picture left of the slot
    for y in range(36, 50):
        tex.px(img, 30, y, BRASS[2]); tex.px(img, 31, y, BRASS[4]); tex.px(img, 32, y, BRASS[3])
    tex.rect(img, 27, 34, 35, 35, RED[3]); tex.rect(img, 28, 33, 34, 33, RED[4])
    tex.rect(img, 33, 42, 41, 43, BRASS[3]); tex.line(img, 33, 44, 41, 44, BRASS[1])
    # fill bar frame
    _inset(img, 65, 37, 56, 12)
    tex.rect(img, 66, 38, 119, 47, (60, 64, 70, 255))
    for x in range(66, 120, 9):
        tex.px(img, x + 8, 47, (90, 96, 104, 255))
    # pressure gauge (centre 93,24; radius 9)
    for y in range(12, 37):
        for x in range(81, 106):
            dx, dy = x + 0.5 - 93.5, y + 0.5 - 24.5
            r = math.hypot(dx, dy)
            if r <= 10.5:
                if r > 8.6:
                    tex.px(img, x, y, art.pick(BRASS, 0.6 - 0.05 * dy / 10 - 0.05 * dx / 10))
                else:
                    tex.px(img, x, y, GAUGE_FACE[2] if r > 1 else GAUGE_FACE[1])
    # ticks and red zone on the dial
    for i in range(10):
        a = math.radians(-135 + 270 * i / 9)
        x = int(round(93 + math.sin(a) * 7.4))
        y = int(round(24 - math.cos(a) * 7.4))
        tex.px(img, x, y, RED[2] if i >= 8 else (70, 60, 50, 255))
    # energy column frame
    _inset(img, 151, 16, 14, 54)
    tex.rect(img, 152, 17, 163, 68, (60, 64, 70, 255))
    for y in range(17, 69, 6):
        tex.px(img, 152, y, (90, 96, 104, 255)); tex.px(img, 163, y, (90, 96, 104, 255))
    # lightning icon under the column
    for (x, y) in ((158, 72), (157, 73), (156, 74), (157, 74), (158, 74), (157, 75), (156, 76)):
        tex.px(img, x, y, YELLOW[2])
    # crank recess and lamp socket
    _inset(img, 125, 30, 22, 22)
    tex.rect(img, 132, 21, 139, 28, (90, 90, 90, 255))
    tex.rect(img, 133, 22, 138, 27, (50, 70, 50, 255))
    # sprites: energy fill (176,0) 12x52
    for y in range(52):
        t = y / 51
        c = tex.mix('#f0e050', '#e06a20', t)
        for x in range(12):
            tex.px(img, 176 + x, y, tex.mix(c, '#ffffff', 0.25) if x in (2, 3) else (tex.darker(c, 0.8) if x >= 10 else c))
        if y % 6 == 0:
            for x in range(12):
                tex.px(img, 176 + x, y, tex.darker(c, 0.7))
    # air fill (176,52) and FE fill (176,62): 54x10
    for x in range(54):
        for y in range(10):
            air = tex.mix(TEAL[4], TEAL[1], y / 9)
            fe = tex.mix(YELLOW[4], YELLOW[1], y / 9)
            if y == 1:
                air = tex.mix(air, '#ffffff', 0.35)
                fe = tex.mix(fe, '#ffffff', 0.35)
            tex.px(img, 176 + x, 52 + y, air)
            tex.px(img, 176 + x, 62 + y, fe)
        if x % 9 == 4:
            tex.px(img, 176 + x, 55, '#e8fffa'); tex.px(img, 176 + x + 1 if x < 53 else 176 + x, 54, '#c8f4ee')
    # lamp on (176,72) 6x6
    tex.rect(img, 176, 72, 181, 77, '#3fbf4f')
    tex.rect(img, 177, 73, 178, 74, '#b8ffb8')
    # crank frames (176,80) normal, (176,100) hovered
    for i in range(4):
        img.alpha_composite(_crank_frame(-90 + 90 * i, False), (176 + 20 * i, 80))
        img.alpha_composite(_crank_frame(-90 + 90 * i, True), (176 + 20 * i, 100))
    return img


# ----------------------------------------------------------------------------------------------- HUD
def _capsule(fill, ramp, empty_ramp=None):
    """9x9 oxygen capsule: rounded vertical cylinder with a brass cap. fill: 0 empty, 1 half, 2 full."""
    img = tex.new(9, 9)
    body = [(x, y) for y in range(2, 9) for x in range(2, 7) if not ((x in (2, 6)) and y in (2, 8))]
    for (x, y) in body:
        filled = fill == 2 or (fill == 1 and y >= 5)
        if filled:
            v = [0.55, 0.85, 0.7, 0.5, 0.3][x - 2]
            tex.px(img, x, y, art.pick(ramp, v))
        else:
            tex.px(img, x, y, (40, 46, 54, 150))
    tex.rect(img, 3, 0, 5, 1, BRASS[3])
    tex.px(img, 3, 0, BRASS[5])
    return tex.outline(img, (24, 28, 34, 255))


def _helmet_icon():
    img = tex.new(9, 9)
    art.sphere(img, 4.5, 4.0, 3.8, 3.8, BRASS, mask=lambda x, y: y <= 6)
    for x in range(1, 8):
        tex.px(img, x, 7, BRASS[2])
    art.disc_glass(img, 4.5, 4.0, 1.6)
    return tex.outline(img, (30, 24, 14, 255))


def hud_sprites():
    low = tex.ramp('#e0632a', 5, spread=0.5)
    return {
        'gui/sprites/hud/diving/oxygen_full': _capsule(2, TEAL),
        'gui/sprites/hud/diving/oxygen_half': _capsule(1, TEAL),
        'gui/sprites/hud/diving/oxygen_empty': _capsule(0, TEAL),
        'gui/sprites/hud/diving/oxygen_low': _capsule(2, low),
        'gui/sprites/hud/diving/oxygen_low_half': _capsule(1, low),
        'gui/sprites/hud/diving/oxygen_icon': _helmet_icon(),
    }
