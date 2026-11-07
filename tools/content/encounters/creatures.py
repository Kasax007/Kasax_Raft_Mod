"""Textures of the encounters module: the scavenger, the reef guardian and the corsair captain (3.2)."""
from bclib import tex

from content.shore import creatures as shore
from content.shore.skin import Box, new, put, fill_rect, material_box, shade, noise, rng, tint_region

MOD = 'encounters'

# humanoid net (HumanoidModel.createMesh, 64x64) plus the scavenger's backpack (ScavengerModel)
HEAD = Box(0, 0, 8, 8, 8)
HAT = Box(32, 0, 8, 8, 8)
BODY = Box(16, 16, 8, 12, 4)
R_ARM = Box(40, 16, 4, 12, 4)
L_ARM = Box(32, 48, 4, 12, 4)
R_LEG = Box(0, 16, 4, 12, 4)
L_LEG = Box(16, 48, 4, 12, 4)
PACK = Box(0, 32, 6, 7, 3)
ROLL = Box(0, 44, 8, 2, 2)


def _rows(img, box, fn, faces=('front', 'right', 'left', 'back')):
    for name in faces:
        rx, ry, rw, rh = box.face(name)
        for fy in range(rh):
            for fx in range(rw):
                c = fn(fy, fx, name, rw)
                if c is not None:
                    put(img, rx + fx, ry + fy, c)


# ============================================================================================== scavenger
def scavenger(seed=81):
    """A sunburnt sea scavenger: rust bandana with brass goggles, khaki shirt under a leather vest, patched trousers,
    boots, and a canvas backpack with a bedroll."""
    img = new()
    Sk = tex.ramp('#b98a62', 6, spread=0.42, hue_shift=0.03)
    Hr = tex.ramp('#4a3424', 4, spread=0.35)
    Sh = tex.ramp('#b3a77c', 5, spread=0.35)
    Vs = tex.ramp('#5c4231', 5, spread=0.4)
    Tr = tex.ramp('#4d5d6c', 5, spread=0.38)
    Pt = tex.ramp('#6d7d58', 4, spread=0.35)
    Bt = tex.ramp('#3b2c22', 5, spread=0.4)
    Bd = tex.ramp('#b4532e', 5, spread=0.4)
    Au = tex.ramp('#c99a45', 4, spread=0.4)
    Gl = tex.ramp('#7fb3c4', 3, spread=0.35)
    Cv = tex.ramp('#8a7f5c', 5, spread=0.38)
    n = noise(seed, cell=2)

    # ---------------------------------------------------------------- head: tanned face, stubble, short hair
    material_box(img, HEAD, Sk, seed, top=0.6, side=0.55, bottom=0.35, grad=0.18, mottling=0.12)
    hf = HEAD.faces()
    fill_rect(img, hf['top'], lambda x, y, fx, fy, w, h: shade(Hr, 0.55 + (n[y][x] - .5) * .4))
    for name in ('right', 'left', 'back'):
        rx, ry, rw, rh = hf[name]
        for fx in range(rw):
            for fy in range(3 if name != 'back' else 5):
                put(img, rx + fx, ry + fy, shade(Hr, 0.5 - fy * 0.06 + (n[ry + fy][rx + fx] - .5) * .3))
    rx, ry, rw, rh = hf['front']
    for fx in range(rw):
        put(img, rx + fx, ry, Hr[2])
    put(img, rx, ry + 1, Hr[1]); put(img, rx + 7, ry + 1, Hr[1])
    # eyes and brows
    for ex in (1, 5):
        put(img, rx + ex, ry + 3, '#efe9dc'); put(img, rx + ex + 1, ry + 3, '#2c3a46')
        put(img, rx + ex, ry + 2, Hr[1]); put(img, rx + ex + 1, ry + 2, Hr[1])
    put(img, rx + 3, ry + 4, Sk[2]); put(img, rx + 4, ry + 4, Sk[2])   # nose
    put(img, rx + 3, ry + 5, Sk[1]); put(img, rx + 4, ry + 5, Sk[1])   # under the nose
    # stubble and mouth
    for fy in (5, 6, 7):
        for fx in range(1, 7):
            if fy == 6 and fx in (3, 4):
                put(img, rx + fx, ry + fy, '#5a2f28')
            elif (fx * 3 + fy) % 4 and not (fy == 5 and fx in (3, 4)):
                put(img, rx + fx, ry + fy, tex.mix(Sk[2], Hr[1], 0.45))
    # ---------------------------------------------------------------- bandana and goggles (overlay)
    of = HAT.faces()
    fill_rect(img, of['top'], lambda x, y, fx, fy, w, h: shade(Bd, 0.62 + (n[y][x] - .5) * .3 - (0.12 if (fx + fy) % 5 == 0 else 0)))
    for name in ('front', 'right', 'left', 'back'):
        rx, ry, rw, rh = of[name]
        for fx in range(rw):
            for fy in range(3):
                put(img, rx + fx, ry + fy, shade(Bd, 0.58 - fy * 0.14 + (n[ry + fy][rx + fx] - .5) * .25))
            if (fx + len(name)) % 3 == 0:
                put(img, rx + fx, ry + 3, Bd[1])
    # brass goggles pushed up on the bandana
    rx, ry, rw, rh = of['front']
    for gx in (1, 5):
        put(img, rx + gx, ry + 1, Gl[2]); put(img, rx + gx + 1, ry + 1, Gl[1])
        put(img, rx + gx, ry + 2, Au[2]); put(img, rx + gx + 1, ry + 2, Au[1])
        put(img, rx + gx, ry, Au[3]); put(img, rx + gx + 1, ry, Au[2])
    put(img, rx + 3, ry + 1, Au[1]); put(img, rx + 4, ry + 1, Au[1])
    # knot and two tails at the back
    rx, ry, rw, rh = of['back']
    put(img, rx + 3, ry + 3, Bd[2]); put(img, rx + 4, ry + 3, Bd[3])
    for k in range(3):
        put(img, rx + 3, ry + 4 + k, Bd[2 - (k > 1)])
        put(img, rx + 5, ry + 4 + k, Bd[1])
    # ---------------------------------------------------------------- body: khaki shirt, leather vest, belt
    material_box(img, BODY, Sh, seed + 1, top=0.62, side=0.55, bottom=0.4, grad=0.15, mottling=0.14)
    bf = BODY.faces()

    def vest(fy, fx, face, fw):
        if fy >= 9:
            return None
        if face == 'front' and 2 < fx < fw - 3:
            return None   # open at the front: the shirt shows
        return shade(Vs, 0.55 - fy * 0.03 + (n[16 + fy][16 + fx] - .5) * .25)
    _rows(img, BODY, vest)
    rx, ry, rw, rh = bf['front']
    for fy in range(9):
        put(img, rx + 2, ry + fy, Vs[1]); put(img, rx + rw - 3, ry + fy, Vs[1])
    for fy in (2, 5):
        put(img, rx + 1, ry + fy, Au[2])
    # shirt collar and buttons
    put(img, rx + 3, ry, Sk[2]); put(img, rx + 4, ry, Sk[2])
    for fy in (2, 4, 6):
        put(img, rx + 4, ry + fy, Sh[1])
    # pack straps over the shoulders (front and top)
    for fy in range(9):
        put(img, rx + 1, ry + fy, Cv[1] if fy % 4 else Au[1]); put(img, rx + rw - 2, ry + fy, Cv[1] if fy % 4 else Au[1])
    def belt(fy, fx, face, fw):
        if fy in (9, 10):
            return Bt[2 + (fy == 9)]
        if fy == 11:
            return shade(Tr, 0.55)
        return None
    _rows(img, BODY, belt)
    put(img, rx + 3, ry + 9, Au[3]); put(img, rx + 4, ry + 9, Au[2]); put(img, rx + 3, ry + 10, Au[1]); put(img, rx + 4, ry + 10, Au[2])
    put(img, rx + 6, ry + 10, Cv[2]); put(img, rx + 6, ry + 9, Cv[3])   # a pouch
    tint_region(img, bf['top'], Vs[2], 0.5)
    # ---------------------------------------------------------------- arms: rolled sleeves, bare forearms, gloves
    for arm, s in ((R_ARM, 2), (L_ARM, 3)):
        material_box(img, arm, Sk, seed + s, top=0.6, side=0.55, bottom=0.35, mottling=0.12)

        def sleeve(fy, fx, face, fw, s=s):
            if fy < 4:
                return shade(Sh, 0.6 - fy * 0.05 + (n[16 + fy][40 + fx] - .5) * .2)
            if fy == 4:
                return Sh[1] if (fx + s) % 2 else Sh[2]
            if fy >= 10:
                return Vs[2] if fy == 10 else Vs[1]
            return None
        _rows(img, arm, sleeve)
        fill_rect(img, arm.face('top'), lambda x, y, fx, fy, w, h: shade(Sh, 0.72))
        fill_rect(img, arm.face('bottom'), lambda x, y, fx, fy, w, h: Vs[1])
    # ---------------------------------------------------------------- legs: patched trousers, boots
    for leg, s in ((R_LEG, 4), (L_LEG, 5)):
        material_box(img, leg, Tr, seed + s, top=0.55, side=0.52, bottom=0.3, mottling=0.18)

        def boots(fy, fx, face, fw, s=s):
            if fy >= 8:
                return Bt[3] if fy == 8 else shade(Bt, 0.5 - (fy - 9) * 0.08)
            return None
        _rows(img, leg, boots)
        rx, ry, rw, rh = leg.face('front')
        # a square patch on the knee
        for (px, py) in ((1, 4), (2, 4), (1, 5), (2, 5)):
            put(img, rx + px, ry + py, Pt[2 if (px + py + s) % 2 else 1])
        put(img, rx + 1, ry + 8, Au[1])
        fill_rect(img, leg.face('bottom'), lambda x, y, fx, fy, w, h: Bt[0])
    # ---------------------------------------------------------------- backpack and bedroll
    material_box(img, PACK, Cv, seed + 7, top=0.66, side=0.55, bottom=0.3, grad=0.2, mottling=0.14)
    rx, ry, rw, rh = PACK.face('back')   # the side facing away from the body
    for fx in range(rw):
        put(img, rx + fx, ry + 2, Cv[1])
    for fy in range(3, rh):
        put(img, rx + 1, ry + fy, Bt[2]); put(img, rx + rw - 2, ry + fy, Bt[2])
    put(img, rx + 1, ry + 4, Au[3]); put(img, rx + rw - 2, ry + 4, Au[3])
    rx, ry, rw, rh = PACK.face('front')
    for fx in range(rw):
        put(img, rx + fx, ry, Cv[1])
    Rl = tex.ramp('#3f6a6e', 4, spread=0.35)
    material_box(img, ROLL, Rl, seed + 8, top=0.7, side=0.5, bottom=0.3, grad=0.3, mottling=0.1)
    for name in ('front', 'back', 'top'):
        rx, ry, rw, rh = ROLL.face(name)
        for fy in range(rh):
            put(img, rx + 2, ry + fy, Bt[2]); put(img, rx + rw - 3, ry + fy, Bt[2])
    return img


# ============================================================================================== reef guardian
def reef_guardian(seed=91):
    """Guardian net (GuardianModel, 64x64), grown over by a reef: sea-green scales, coral plates, coral spikes, an amber
    eye. The model shares some texture areas between parts (as vanilla does), so every area is painted as a whole."""
    img = new()
    Sc = tex.ramp('#3f8a7a', 6, spread=0.45, hue_shift=0.04)
    Pl = tex.ramp('#d77a72', 5, spread=0.42, hue_shift=0.05)
    Sp = tex.ramp('#f0a24a', 5, spread=0.45)
    Bn = tex.ramp('#e6dcc4', 4, spread=0.3)
    Ey = tex.ramp('#f5c64a', 4, spread=0.4)
    n = noise(seed, cell=2)
    head = Box(0, 0, 12, 12, 16)
    side = Box(0, 28, 2, 12, 12)
    plate = Box(16, 40, 12, 2, 12)
    tail0 = Box(40, 0, 4, 4, 8)
    tail1 = Box(0, 54, 3, 3, 7)
    tail2 = Box(41, 32, 2, 2, 6)
    fin = Box(25, 19, 1, 9, 9)

    def scales(box, s, base=0.55):
        material_box(img, box, Sc, seed + s, top=base + 0.12, side=base, bottom=base - 0.25, grad=0.2, mottling=0.16)
        for name, (rx, ry, rw, rh) in box.faces().items():
            for fy in range(rh):
                for fx in range(rw):
                    # staggered scale rows: a darker lower edge on every second pixel
                    if (fy % 3 == 2) and ((fx + fy // 3) % 2 == 0):
                        c = img.getpixel((rx + fx, ry + fy))
                        put(img, rx + fx, ry + fy, tex.darker(c, 0.86))
    for b, s in ((head, 1), (tail0, 2), (tail1, 3), (tail2, 4)):
        scales(b, s)
    # coral growths on the head: pink knobs with a light top pixel
    r = rng(seed)
    for name, (rx, ry, rw, rh) in head.faces().items():
        for _ in range(4 if name != 'bottom' else 1):
            x, y = rx + r.randrange(1, rw - 2), ry + r.randrange(1, rh - 2)
            put(img, x, y, Pl[3]); put(img, x + 1, y, Pl[2]); put(img, x, y + 1, Pl[1])
    # the face: a dark socket for the eye, coral rim
    rx, ry, rw, rh = head.face('front')
    for fy in range(3, 9):
        for fx in range(3, 9):
            d = abs(fx - 5.5) + abs(fy - 5.5)
            if d < 3.2:
                put(img, rx + fx, ry + fy, '#1d2a2c' if d < 2.4 else Pl[1])
    # armour plates (sides, top, bottom): coral pink with ridges
    for b, s in ((side, 5), (plate, 6)):
        material_box(img, b, Pl, seed + s, top=0.7, side=0.55, bottom=0.3, grad=0.2, mottling=0.12)
        for name, (rx, ry, rw, rh) in b.faces().items():
            for fy in range(rh):
                for fx in range(rw):
                    if (fx + fy) % 4 == 0 and rw > 2 and rh > 2:
                        put(img, rx + fx, ry + fy, Pl[1])
    # tail fin: translucent-looking pale sea green with rays
    material_box(img, fin, tex.ramp('#7cc7b2', 4, spread=0.35), seed + 7, top=0.7, side=0.6, mottling=0.08)
    for name in ('right', 'left'):
        rx, ry, rw, rh = fin.face(name)
        for k in range(rw):
            put(img, rx + k, ry + min(rh - 1, k // 2 + 1), Sc[2])
    # spikes: coral branches, orange to a pale tip (texture 0,0: shared with the eye, painted after)
    spike = Box(0, 0, 2, 9, 2)
    for name, (rx, ry, rw, rh) in spike.faces().items():
        for fy in range(rh):
            for fx in range(rw):
                v = 0.8 - fy / max(1, rh - 1) * 0.6 if name not in ('top', 'bottom') else 0.9
                put(img, rx + fx, ry + fy, shade(Sp, v - (0.1 if fx == 0 else 0)))
    # eye (8,0: 2x2x1): amber with a dark slit
    eye = Box(8, 0, 2, 2, 1)
    for name, (rx, ry, rw, rh) in eye.faces().items():
        fill_rect(img, (rx, ry, rw, rh), lambda x, y, fx, fy, w, h: Ey[2])
    rx, ry, rw, rh = eye.face('front')
    put(img, rx, ry, Ey[3]); put(img, rx + 1, ry, Ey[2]); put(img, rx, ry + 1, '#3a2a10'); put(img, rx + 1, ry + 1, Ey[1])
    # bone-white spine ridge along the tail top
    for b in (tail0, tail1):
        rx, ry, rw, rh = b.face('top')
        for fy in range(rh):
            put(img, rx + rw // 2, ry + fy, Bn[2 if fy % 2 else 1])
    return img


# ============================================================================================== corsair captain
def _swap(img, src, dst):
    m = {tuple(tex.rgb(a)[:3]): tex.rgb(b) for a, b in zip(src, dst)}
    out = img.copy()
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            c = px[x, y]
            if c[3] and c[:3] in m:
                k = m[c[:3]]
                px[x, y] = (k[0], k[1], k[2], c[3])
    return out


def captain():
    """Saltjaw's first mate: the corsair body with a gold-striped shirt instead of the red one."""
    img = shore.corsair(seed=57)
    return _swap(img, tex.ramp('#8e2c2c', 4, spread=0.35), tex.ramp('#c79a3a', 4, spread=0.35))


def captain_coat():
    """Long navy captain's coat with gold trim (the corsair coat cut, other cloth)."""
    img = shore.corsair_coat(seed=67)
    return _swap(img, tex.ramp('#7d2228', 6, spread=0.45), tex.ramp('#24325e', 6, spread=0.45))


def captain_hat():
    """The tricorn with a red plume."""
    img = shore.corsair_hat(seed=77)
    return _swap(img, tex.ramp('#efeae0', 4, spread=0.25), tex.ramp('#c8352e', 4, spread=0.35))


def build():
    out = {
        'scavenger': scavenger(),
        'reef_guardian': reef_guardian(),
        'corsair_captain': captain(),
        'corsair_captain_coat': captain_coat(),
        'corsair_captain_hat': captain_hat(),
    }
    tex.save(out['scavenger'], MOD, 'entity/scavenger/scavenger')
    tex.save(out['reef_guardian'], MOD, 'entity/reef_guardian/reef_guardian')
    tex.save(out['corsair_captain'], MOD, 'entity/corsair_captain/corsair_captain')
    tex.save(out['corsair_captain_coat'], MOD, 'entity/corsair_captain/corsair_captain_coat')
    tex.save(out['corsair_captain_hat'], MOD, 'entity/corsair_captain/corsair_captain_hat')
    return out
