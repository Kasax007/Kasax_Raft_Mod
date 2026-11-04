"""The eight people of the sea, painted with skins.py (see docs/design/npcs.md for who they are)."""
from bclib import tex

from .skins import (Skin, SIDES, ramp, skin_body, face, hair, hair_volume, beard, shirt, neck_skin, trousers, shoes,
                    sandals, bare_feet, hands, outer, clear_box)


# ---------------------------------------------------------------------------------------------- Keeper Tobias Fenn
def keeper_fenn():
    s = Skin(slim=False, seed=101)
    tone = ramp('#d79f7b', 6, 0.42)
    skin_body(s, tone)
    face(s, tone, eye='#4f6d88', brow='#ece8df', age=2)
    hair(s, '#dcd7cf', style='bald', side_len=5, back_len=5)
    beard(s, '#ebe7df', rows=(5, 8), full=True, moustache=True)
    # cream knit sweater (seen at the neck), navy pea coat over it
    shirt(s, '#d8ccb2', sleeve=12)
    sweater = ramp('#d8ccb2', 5, 0.35)
    navy = ramp('#2a3756', 6, 0.42)
    brass = ramp('#d6a83c', 4, 0.4)

    def coat(name, fx, fy, w, h):
        if name == 'front':
            if fy < 3 and 2 < fx < 5:
                return 'clear'                        # the sweater's roll neck shows
            if fy < 4 and fx in (2, 5):
                return navy[4]                        # lapels
            if fx in (1, 6) and fy in (4, 7, 10):
                return brass[3]                       # double row of buttons
            if fy == 11:
                return navy[1]
        if name == 'back' and fy in (9, 10) and fx in (3, 4):
            return navy[1]                            # back vent
        return None
    outer(s, s.JACKET, navy, (0, 12), coat, base=0.5)
    for box in (s.RSLEEVE, s.LSLEEVE):
        outer(s, box, navy, (0, 11), lambda n, fx, fy, w, h: navy[3] if fy == 10 else None, base=0.5)
    # roll neck of the sweater on the body front
    rx, ry, rw, rh = s.BODY.face('front')
    for x in (3, 4):
        for y in range(3):
            s.put(rx + x, ry + y, sweater[3 - (y == 2)])
    hands(s, tone)
    trousers(s, '#4a4c55', length=12)
    shoes(s, '#2b2628', height=3, toe='#3d3739')
    # coat tails over the thighs
    for box in (s.RPANTS, s.LPANTS):
        outer(s, box, navy, (0, 3), lambda n, fx, fy, w, h: 'clear' if n == 'front' and fy > 0 else None, base=0.45)
    # navy knit watch cap with a turned-up rib
    cap = ramp('#253252', 5, 0.4)

    def knit(name, fx, fy, w, h):
        if name == 'bottom':
            return 'clear'
        if name in SIDES:
            if fy > 2:
                return 'clear'
            if fy == 2:
                return cap[3] if fx % 2 else cap[2]
            return cap[2] if (fx + fy) % 2 else cap[1]
        return None
    outer(s, s.HAT, cap, (0, 8), knit, base=0.5)
    s.pixel(s.HAT, 'front', 6, 2, '#c9c2b2')          # little anchor badge
    return s


# ---------------------------------------------------------------------------------------------- Nalu
def nalu():
    s = Skin(slim=False, seed=202)
    tone = ramp('#a3673f', 6, 0.42)
    skin_body(s, tone)
    face(s, tone, eye='#2e2018', brow='#1b1513', age=0)
    s.pixel(s.HEAD, 'front', 2, 6, tone[3]); s.pixel(s.HEAD, 'front', 5, 6, tone[3])   # smile corners
    hair(s, '#1d191b', style='short', fringe=1, side_len=3, back_len=4)
    hair_volume(s, '#24201f', rows=1, back=2, curly=True)
    # dark teal tank top with a cream wave band, shell necklace
    teal = ramp('#1d7f86', 6, 0.42)

    def top(name, fx, fy, r):
        if fy == 6:
            return '#e9e0c6' if (fx + (name == 'back')) % 3 else r[3]
        return None
    shirt(s, '#1d7f86', sleeve=0, pattern=top, body_rows=8)
    rx, ry, rw, rh = s.BODY.face('front')
    for (x, y) in ((1, 0), (2, 1), (3, 1), (4, 1), (5, 1), (6, 0)):
        s.put(rx + x, ry + y, '#f1ead8' if x % 2 else '#d9c9a4')
    s.put(rx + 4, ry + 2, '#e8a1a8')                  # a pink shell in the middle
    # shoulders bare
    for name in ('front', 'back'):
        bx, by, bw, bh = s.BODY.face(name)
        s.put(bx, by, tone[3]); s.put(bx + 7, by, tone[3])
    # woven arm band
    for arm in (s.RARM, s.LARM):
        s.around(arm, 3, lambda n, fx, w: '#b98d4f' if fx % 2 else '#8a6436')
    # red wrap (lava-lava) with white pattern, knee length
    red = ramp('#b3373a', 6, 0.42)

    def wrap(name, fx, fy, r):
        if (fx + fy) % 4 == 0 and fy % 2 == 0:
            return '#efe6d4'
        if fy == 0 and name != 'top':
            return '#7a2226'
        return None
    trousers(s, '#b3373a', length=7, pattern=wrap)
    for leg in (s.RLEG, s.LLEG):
        s.around(leg, 6, lambda n, fx, w: red[1])
    sandals(s, '#6e4a2c', tone)
    return s


# ---------------------------------------------------------------------------------------------- Old Tamo
def old_tamo():
    s = Skin(slim=False, seed=303)
    tone = ramp('#bd8156', 6, 0.42)
    skin_body(s, tone)
    face(s, tone, eye='#3c4a38', brow='#c9c2b6', age=2)
    hair(s, '#a8a39b', style='bald', side_len=4, back_len=4)
    beard(s, '#d6d1c8', rows=(5, 7), full=False, moustache=True, sides=False)
    # stubble on the chin
    rx, ry, rw, rh = s.HEAD.face('front')
    for x in range(1, 7):
        s.put(rx + x, ry + 7, tex.mix(tone[2], '#9a958d', 0.45))
    # red and cream checked flannel shirt, sleeves rolled to the elbow
    def check(name, fx, fy, r):
        a = (fx // 2) % 2 == 0
        b = (fy // 2) % 2 == 0
        if a and b:
            return r[1]
        if a or b:
            return r[3]
        return '#e3d6bf'
    shirt(s, '#b8322f', sleeve=6, pattern=check)
    for arm in (s.RARM, s.LARM):
        s.around(arm, 5, lambda n, fx, w: '#e3d6bf')      # rolled cuff
    # yellow oilskin bib overalls
    oil = ramp('#dca728', 6, 0.42)
    trousers(s, '#dca728', length=12)

    def bib(name, fx, fy, w, h):
        if name == 'front':
            if fy < 3 and fx not in (1, 6):
                return 'clear'
            if fy < 3:
                return oil[2]                             # straps
            if fy == 3 and fx in (1, 6):
                return '#8f9298'                          # buckles
        if name == 'back':
            if fy < 3 and fx not in (2, 5):
                return 'clear'
            if fy < 3:
                return oil[2]
        if name in ('right', 'left'):
            if fy < 7:
                return 'clear'
        if name == 'top':
            return 'clear' if fx not in (1, 6) else oil[3]
        return None
    outer(s, s.JACKET, oil, (0, 12), bib, base=0.55)
    hands(s, tone)
    shoes(s, '#2f4a35', height=4, toe='#26392b')
    # straw hat crown with a red band (the brim is the hat model)
    straw = ramp('#d8b76c', 5, 0.38)

    def crown(name, fx, fy, w, h):
        if name == 'bottom':
            return 'clear'
        if name in SIDES:
            if fy > 2:
                return 'clear'
            if fy == 2:
                return '#a8302c' if fx % 3 else '#7c2321'
            return straw[3] if (fx + fy) % 2 else straw[2]
        return straw[3] if (fx + fy) % 2 else straw[4]
    outer(s, s.HAT, straw, (0, 8), crown, base=0.55)
    return s


# ---------------------------------------------------------------------------------------------- Dr. Imara Voss
def dr_voss():
    s = Skin(slim=True, seed=404)
    tone = ramp('#7a4930', 6, 0.42)
    skin_body(s, tone)
    face(s, tone, eye='#2a1c15', brow='#17110f', age=1, lashes=False, glasses='#c19a4a')
    hair(s, '#1b1618', style='short', fringe=1, side_len=4, back_len=6)
    # (the bun on top is a 3D extra, see extras())
    # teal roll-neck, white lab coat (soot on the hem), dark trousers
    shirt(s, '#2b6b70', sleeve=12)
    white = ramp('#e8e6de', 6, 0.3)

    def lab(name, fx, fy, w, h):
        if name == 'front':
            if 2 < fx < 5:
                return 'clear'                            # open coat
            if fx in (2, 5) and fy < 5:
                return white[3]                           # lapels
            if fx == 1 and fy in (3, 4):
                return '#3c6db0' if fy == 3 else '#c8433a'   # pens in the breast pocket
            if fy in (8, 9) and fx in (0, 1, 6, 7):
                return white[2] if fy == 8 else white[1]  # pockets
        if fy >= 10 and (fx * 7 + fy * 3) % 5 == 0:
            return '#8d8a85'                              # soot from the forge
        return None
    outer(s, s.JACKET, white, (0, 12), lab, base=0.6, mottle=0.06)
    for box in (s.RSLEEVE, s.LSLEEVE):
        outer(s, box, white, (0, 11), lambda n, fx, fy, w, h: white[2] if fy == 10 else None, base=0.6, mottle=0.06)
    # Expedition patch on the left shoulder
    s.pixel(s.LSLEEVE, 'left', 1, 1, '#c8433a'); s.pixel(s.LSLEEVE, 'left', 2, 1, '#f2f0ea')
    s.pixel(s.LSLEEVE, 'left', 1, 2, '#f2f0ea'); s.pixel(s.LSLEEVE, 'left', 2, 2, '#c8433a')
    hands(s, tone)
    trousers(s, '#3a3d45', length=12)
    shoes(s, '#26211f', height=2)
    # coat tails over the thighs (open at the front)
    for box in (s.RPANTS, s.LPANTS):
        outer(s, box, white, (0, 4), lambda n, fx, fy, w, h: 'clear' if n == 'front' else (
            '#8d8a85' if fy == 3 and fx % 2 else None), base=0.55, mottle=0.06)
    return s


# ---------------------------------------------------------------------------------------------- Teo
def teo():
    s = Skin(slim=False, seed=505)
    tone = ramp('#c78f65', 6, 0.42)
    skin_body(s, tone)
    face(s, tone, eye='#35261c', brow='#1a1513', age=0)
    rx, ry, rw, rh = s.HEAD.face('front')
    for x in range(2, 6):
        s.put(rx + x, ry + 5, '#241c19')                 # neat moustache
    s.put(rx + 3, ry + 6, tone[1]); s.put(rx + 4, ry + 6, tone[1])
    hair(s, '#1b1719', style='short', fringe=1, side_len=3, back_len=4)
    hair_volume(s, '#211c1d', rows=1, back=2, sides=False)
    # orange aloha shirt with hibiscus flowers, open collar
    def aloha(name, fx, fy, r):
        k = (fx + 3 * (fy // 3)) % 5
        if fy % 3 == 1 and k == 0:
            return '#f6efe0'
        if fy % 3 == 1 and k == 1:
            return '#f3c94a'
        if fy % 3 == 2 and k == 0:
            return '#3f8a4a'
        return None
    shirt(s, '#e0782c', sleeve=4, pattern=aloha, body_rows=12)
    neck_skin(s, tone, 'v', depth=3)
    for arm in (s.RARM, s.LARM):
        s.around(arm, 3, lambda n, fx, w: '#b65a1c')
    trousers(s, '#e8e2d4', length=6, waist_rows=(9, 12))
    for leg in (s.RLEG, s.LLEG):
        s.around(leg, 5, lambda n, fx, w: '#d0c8b6')
    sandals(s, '#5a3b25', tone)
    # bar towel over the left shoulder
    towel = ramp('#f1efe8', 4, 0.25)

    def towel_fn(name, fx, fy, w, h):
        # over the left shoulder: front x 6-7 and back x 0-1 are the entity's left side
        x0 = 6 if name == 'front' else 0
        if name in ('front', 'back') and x0 <= fx <= x0 + 1 and fy < 4:
            return towel[2] if fy % 2 else towel[3]
        if name in ('front', 'back') and x0 <= fx <= x0 + 1 and fy == 4:
            return '#4a8ec2'                              # blue stripe at the end
        if name == 'top' and fx >= 6:
            return towel[3]
        if name == 'left' and fy < 2:
            return towel[2]
        return 'clear'
    outer(s, s.JACKET, towel, (0, 12), towel_fn)
    return s


# ---------------------------------------------------------------------------------------------- Kai
def kai():
    s = Skin(slim=False, seed=606)
    tone = ramp('#e2ad87', 6, 0.42)
    skin_body(s, tone)
    face(s, tone, eye='#4b7ea8', brow='#b88a44', age=0, blush='#e48a72')
    s.pixel(s.HEAD, 'front', 3, 5, '#e48a72'); s.pixel(s.HEAD, 'front', 4, 5, '#da7f68')   # sunburnt nose
    hair(s, '#d6b062', style='short', fringe=2, side_len=4, back_len=5)
    hair_volume(s, '#dcb76a', rows=2, back=4, curly=True)
    # red bandana around the forehead, knot at the back
    for name in SIDES:
        rx, ry, rw, rh = s.HAT.face(name)
        for fx in range(rw):
            s.put(rx + fx, ry + 2, '#c2352f' if (fx + (name == 'back')) % 3 else '#f0e6dc')
    bx, by, bw, bh = s.HAT.face('back')
    s.put(bx + 3, by + 3, '#a52b26'); s.put(bx + 4, by + 3, '#c2352f'); s.put(bx + 3, by + 4, '#a52b26')
    # torn light blue t-shirt
    shirt(s, '#7db4d4', sleeve=4)
    neck_skin(s, tone, 'crew')
    tears = [('front', 5, 4), ('front', 6, 4), ('front', 5, 5), ('back', 1, 7), ('front', 1, 9), ('front', 2, 10),
             ('right', 1, 8), ('left', 2, 10)]
    for (name, x, y) in tears:
        s.pixel(s.BODY, name, x, y, tone[3])
    for arm in (s.RARM, s.LARM):
        s.around(arm, 3, lambda n, fx, w: tone[3] if fx % 2 else '#6aa0c0')   # ragged sleeve edge
    # patched khaki shorts
    trousers(s, '#a59265', length=6, waist_rows=(9, 12))
    s.pixel(s.RLEG, 'front', 1, 2, '#6e8fb0'); s.pixel(s.RLEG, 'front', 2, 2, '#6e8fb0')
    s.pixel(s.RLEG, 'front', 1, 3, '#5d7f9f'); s.pixel(s.RLEG, 'front', 2, 3, '#6e8fb0')
    for leg in (s.RLEG, s.LLEG):
        s.around(leg, 5, lambda n, fx, w: '#8c7a52' if fx % 2 else tone[3])
    bare_feet(s, tone)
    return s


# ---------------------------------------------------------------------------------------------- Dr. Elin Lund
def dr_lund():
    s = Skin(slim=True, seed=707)
    tone = ramp('#eec3a3', 6, 0.38)
    skin_body(s, tone)
    face(s, tone, eye='#5f86a8', brow='#b9a582', age=1, blush='#e79a86')
    hair(s, '#d5c59a', style='short', fringe=1, side_len=3, back_len=7)
    # orange parka with a fur-trimmed hood around the face
    orange = ramp('#df5f2a', 6, 0.42)
    fur = ramp('#e2ddd3', 4, 0.25)
    shirt(s, '#3b4f7a', sleeve=12)

    def parka(name, fx, fy, w, h):
        if name == 'front':
            if fx in (3, 4) and fy < 11:
                return '#b9bcc2' if fx == 3 else orange[2]     # zip
            if fy in (7, 8) and fx in (0, 1, 6, 7):
                return orange[1]                              # pockets
            if fy == 11:
                return orange[1]
        if fy == 4 and name in ('right', 'left', 'back'):
            return '#d8dbe0'                                  # reflective band
        return None
    outer(s, s.JACKET, orange, (0, 12), parka, base=0.55)
    for box in (s.RSLEEVE, s.LSLEEVE):
        outer(s, box, orange, (0, 10), lambda n, fx, fy, w, h: '#d8dbe0' if fy == 4 else None, base=0.55)
    hands(s, '#3b5f91', rows=2)                              # mittens

    def hood(name, fx, fy, w, h):
        if name == 'bottom':
            return 'clear'
        if name == 'front':
            if fy == 0 or fx in (0, 7) or (fy == 1 and fx in (1, 6)):
                return fur[2] if (fx + fy) % 2 else fur[3]
            return 'clear'
        if name in ('right', 'left'):
            near_face = fx == (w - 1 if name == 'right' else 0)
            if near_face:
                return fur[2] if fy % 2 else fur[3]
            return None
        return None
    outer(s, s.HAT, orange, (0, 8), hood, base=0.5)
    trousers(s, '#2b3450', length=12)
    shoes(s, '#5b3b25', height=3)
    for leg in (s.RLEG, s.LLEG):
        s.around(leg, 9, lambda n, fx, w: fur[3] if fx % 2 else fur[2])    # fur boot cuffs
    return s


# ---------------------------------------------------------------------------------------------- Patience Okafor
def okafor():
    s = Skin(slim=True, seed=808)
    tone = ramp('#5d3822', 6, 0.42)
    skin_body(s, tone)
    face(s, tone, eye='#1d1410', brow='#110c0a', age=0, lashes=True)
    hair(s, '#141113', style='short', fringe=1, side_len=4, back_len=6)
    # blue coveralls with hi-vis bands, sleeves pushed up
    blue = ramp('#2e5d9b', 6, 0.42)

    def cover(name, fx, fy, r):
        if fy == 5:
            return '#e2e65a' if name != 'top' else None
        return None
    shirt(s, '#2e5d9b', sleeve=7, pattern=cover)
    for arm in (s.RARM, s.LARM):
        s.around(arm, 6, lambda n, fx, w: blue[1])
    trousers(s, '#2e5d9b', length=12, pattern=lambda n, fx, fy, r: '#e2e65a' if fy == 7 and n != 'top' else None)
    rx, ry, rw, rh = s.BODY.face('front')
    for x in (3, 4):
        for y in range(1, 9):
            s.put(rx + x, ry + y, blue[2] if x == 3 else blue[3])    # zip placket
    s.put(rx + 6, ry + 2, '#f2f0ea'); s.put(rx + 7, ry + 2, '#c8433a')  # name patch
    neck_skin(s, tone, 'crew')
    hands(s, '#d2ac55', rows=2)                              # work gloves
    shoes(s, '#3b2a1e', height=3, toe='#9ba1a9')
    # tool belt with pouches (outer layer)
    leather = ramp('#6c4a2b', 5, 0.38)

    def belt(name, fx, fy, w, h):
        if fy == 8:
            return leather[2] if name != 'front' or fx not in (3, 4) else '#b9bcc2'
        if name == 'front' and fy in (9, 10) and fx in (0, 1, 6, 7):
            return leather[1] if fy == 10 else leather[2]
        if name in ('right', 'left') and fy in (9, 10) and fx in (1, 2):
            return leather[1]
        return 'clear'
    outer(s, s.JACKET, leather, (0, 12), belt)
    s.pixel(s.JACKET, 'front', 1, 9, '#a0a6ae')               # a wrench in the pouch
    # orange hard hat
    hat = ramp('#ef8a24', 5, 0.38)

    def hard(name, fx, fy, w, h):
        if name == 'bottom':
            return 'clear'
        if name in SIDES:
            if fy > 2:
                return 'clear'
            if fy == 2:
                return hat[1]
        return None
    outer(s, s.HAT, hat, (0, 8), hard, base=0.6, mottle=0.05)
    s.pixel(s.HAT, 'front', 3, 1, '#f4f2ec'); s.pixel(s.HAT, 'front', 4, 1, '#f4f2ec')   # sticker
    return s


# ============================================================================================== 3D extras
# textures of the extra head models (NpcAccessories.java), 64x32, box UV nets like the skins
def _extra_box(img, box, ramp_, fn=None, base=0.55):
    from .skins import FACE_LIGHT
    for name, (rx, ry, rw, rh) in box.faces().items():
        for fy in range(rh):
            for fx in range(rw):
                c = fn(name, fx, fy, rw, rh) if fn else None
                if c is None:
                    v = base + FACE_LIGHT[name]
                    k = len(ramp_)
                    c = ramp_[max(0, min(k - 1, int(round(v * (k - 1)))))]
                img.putpixel((rx + fx, ry + fy), tex.rgb(c))


def extras():
    from PIL import Image
    from .skins import Box
    out = {}
    # Tamo: wide straw brim with a woven pattern, darker rim and underside
    img = Image.new('RGBA', (64, 32), (0, 0, 0, 0))
    straw = ramp('#d8b76c', 5, 0.38)

    def weave(name, fx, fy, w, h):
        if name == 'top':
            ring = min(fx, fy, w - 1 - fx, h - 1 - fy)
            if ring == 0:
                return straw[1]
            return straw[3] if (fx + fy) % 2 else straw[2 if ring > 1 else 1]
        if name == 'bottom':
            return straw[1] if (fx + fy) % 2 else straw[0]
        return straw[1]
    _extra_box(img, Box(0, 0, 14, 1, 14), straw, weave)
    out['old_tamo'] = img
    # Okafor: hard hat visor and ridge, the orange of the crown on the skin
    img = Image.new('RGBA', (64, 32), (0, 0, 0, 0))
    hat = ramp('#ef8a24', 5, 0.38)
    _extra_box(img, Box(0, 0, 9, 1, 2), hat, lambda n, fx, fy, w, h: hat[1] if n in ('bottom', 'front') else None, base=0.6)
    _extra_box(img, Box(0, 4, 1, 1, 8), hat, None, base=0.65)
    out['okafor'] = img
    # Voss: the hair bun with a pencil through it
    img = Image.new('RGBA', (64, 32), (0, 0, 0, 0))
    hr = ramp('#211b1d', 5, 0.4)
    _extra_box(img, Box(0, 0, 3, 2, 3), hr, lambda n, fx, fy, w, h: '#c19a4a' if n in ('right', 'left') and fy == 0 and fx == 1 else None, base=0.5)
    out['dr_voss'] = img
    # Nalu: a red hibiscus behind the left ear
    img = Image.new('RGBA', (64, 32), (0, 0, 0, 0))
    red = ramp('#d8343f', 4, 0.4)

    def flower(name, fx, fy, w, h):
        if name == 'left':
            return '#f6d24a' if (fx, fy) == (0, 1) else red[3 if fy == 0 else 2]
        return red[1]
    _extra_box(img, Box(0, 0, 1, 2, 2), red, flower)
    out['nalu'] = img
    return out


ALL = {
    'keeper_fenn': keeper_fenn,
    'nalu': nalu,
    'old_tamo': old_tamo,
    'dr_voss': dr_voss,
    'teo': teo,
    'kai': kai,
    'dr_lund': dr_lund,
    'okafor': okafor,
}
