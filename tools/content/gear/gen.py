"""Generator of the "gear" module (Beach Craft 3.3): weapons (shark tooth spear, boomerang, machete), the shell and
explorer armour sets, the wreck compass, the captain's spyglass, fishing bait and (3.4) the Driftwood Fishing Rod with
its tackle box.

Run:  cd tools && python3 -m content.gear.gen
Armour layers and some sprites are vanilla shapes re-shaded with Beach Craft palettes (each colour mapped by its
brightness rank onto a 4-6 tone ramp), so they fit the vanilla look and the humanoid UV layout exactly.
"""
import colorsys
import json
import math
import os
import shutil
import sys

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..')))

from PIL import Image  # noqa: E402

from bclib import assets as A  # noqa: E402
from bclib import tex  # noqa: E402
from bclib.paths import module_dir, VANILLA  # noqa: E402

from content.cozy.recipes import Book  # noqa: E402
from content.decor.art import pal  # noqa: E402
from content.cozy.almanac import PAGES  # noqa: E402

M = 'gear'
rid = A.rid
ITEMS = {}
LANG = {
    'itemgroup.raft.gear': 'Beach Craft: Gear',
    'entity.raft.boomerang': 'Boomerang',
    'message.raft.wreck_compass.found': 'Nearest wreck: %s blocks away',
    'message.raft.wreck_compass.none': 'No wreck within reach of this compass',
    'message.raft.wreck_compass.dimension': 'The compass only finds wrecks in the Endless Sea and the overworld',
    'message.raft.spyglass.distance': '%s blocks',
    'message.raft.spyglass.entity': '%s - %s blocks',
    'tooltip.raft.shark_tooth_spear': 'Charge and jab like any spear',
    'tooltip.raft.boomerang': 'Throw it: it comes back to you',
    'tooltip.raft.boomerang.1': 'Brings back items it flies through',
    'tooltip.raft.machete': 'Cuts leaves, plants, bamboo and vines fast',
    'tooltip.raft.shell_set': 'Full set: Resistance in water',
    'tooltip.raft.explorer_set': 'Full set: +10% speed and Luck',
    'tooltip.raft.wreck_compass': 'Use: points to the nearest shipwreck',
    'tooltip.raft.captains_spyglass': 'Zooms twice as far and measures distance',
    'tooltip.raft.fish_bait': 'Keep it with your fishing rod: bites come faster',
    'tooltip.raft.fish_bait.1': 'Also makes fish traps twice as fast',
    'tooltip.raft.golden_lure': 'Keep it with your fishing rod: better catches',
    'tooltip.raft.fish_bait.rod': 'Or load it into a Driftwood Fishing Rod',
    'tooltip.raft.driftwood_fishing_rod': 'Sneak-use to load bait and lures, or right-click them onto it',
    'tooltip.raft.driftwood_fishing_rod.1': 'Each cast uses one of each that is loaded',
    'tooltip.raft.driftwood_fishing_rod.empty': 'Nothing loaded',
    'tooltip.raft.driftwood_fishing_rod.bait': 'Fish Bait: %s (faster bites)',
    'tooltip.raft.driftwood_fishing_rod.lure': 'Golden Lures: %s (better treasure)',
    'container.raft.tackle_box': 'Tackle Box',
    'jei.raft.info.fish_bait': 'Fish Bait makes fish bite 5 seconds sooner, like Lure I. With a plain fishing rod keep it in '
                               'your off hand or bag: each cast uses one. Or load a stack into a Driftwood Fishing Rod. In '
                               'a Fish Trap it makes the trap twice as fast.',
    'jei.raft.info.golden_lure': 'The Golden Lure brings better treasure, like Luck of the Sea II, and bites 5 seconds '
                                 'sooner. With a plain rod it is used from your off hand or bag, one per cast; a Driftwood '
                                 'Fishing Rod holds a stack of them in its own slot.',
    'jei.raft.info.driftwood_fishing_rod': 'A fishing rod with its own tackle box: one slot for Fish Bait, one for Golden '
                                           'Lures. Sneak-use the rod to open it, or right-click bait and lures onto the '
                                           'rod in your inventory (right-click it with an empty hand to take them out). '
                                           'Every cast uses one of each that is loaded, on top of the rod\'s own '
                                           'enchantments; it never takes bait from your bag. 128 uses, repaired with '
                                           'driftwood planks.',
    'jei.raft.info.fishing_rod': 'Beach Craft: keep Fish Bait or a Golden Lure in your off hand or bag and every cast uses '
                                 'one (faster bites; the lure also brings better treasure). The Driftwood Fishing Rod '
                                 'carries its own bait instead.',
    'container.raft.tackle_box.bait': 'faster bites',
    'container.raft.tackle_box.lure': 'better treasure',
    'container.raft.tackle_box.bait_hint': 'Fish Bait: bites come 5 seconds sooner',
    'container.raft.tackle_box.lure_hint': 'Golden Lure: treasure like Luck of the Sea II',
    'subtitles.raft.boomerang.throw': 'Boomerang whooshes',
}

SHELL = pal('#7d4f45', '#a46b5c', '#c98f7a', '#e2b39b', '#f2d2bd', '#fbeadd')
EXPLORER = pal('#4a3b20', '#66532e', '#83703f', '#a18e55', '#bdab72', '#d8c896')
TOOTH = pal('#8f8a78', '#b3ad98', '#d2ccb8', '#e8e4d4', '#f8f6ec')
TEAK = pal('#2c170d', '#412316', '#58311d', '#6f4026', '#87512f', '#a1663a')
DRIFT = pal('#4f473d', '#665d51', '#7e7466', '#978c7c', '#ada393', '#c4bbab')
BRASS = pal('#4a3311', '#74521e', '#a0772c', '#c99d43', '#e6c46c', '#f8e3a2')
TEAL = pal('#123c3c', '#1c5654', '#28716c', '#3a8f86', '#56ab9f')
STEEL = pal('#2a2d34', '#3b3f48', '#555b66', '#737a86', '#9aa1ab', '#c6ccd4')


CLIENT_JAR = os.environ.get('BC_CLIENT_JAR', '/root/.gradle/caches/fabric-loom/26.3/minecraft-client.jar')


def vanilla_png(path):
    """A vanilla texture from the extracted assets, or straight from the 26.3 client jar (GUI textures are not
    extracted)."""
    f = os.path.join(VANILLA, 'assets', 'minecraft', 'textures', path + '.png')
    if os.path.isfile(f):
        return Image.open(f).convert('RGBA')
    import io
    import zipfile
    with zipfile.ZipFile(CLIENT_JAR) as z:
        return Image.open(io.BytesIO(z.read(f'assets/minecraft/textures/{path}.png'))).convert('RGBA')


def vtex(path):
    return Image.open(os.path.join(VANILLA, 'assets', 'minecraft', 'textures', path + '.png')).convert('RGBA')


def lum(c):
    return c[0] * 0.3 + c[1] * 0.59 + c[2] * 0.11


def recolour(src, ramp):
    """Every opaque colour of src mapped by brightness rank onto ramp (keeps the vanilla shading)."""
    cols = sorted({c for c in src.getdata() if c[3] > 0}, key=lum)
    out = Image.new('RGBA', src.size, (0, 0, 0, 0))
    for y in range(src.height):
        for x in range(src.width):
            c = src.getpixel((x, y))
            if c[3] == 0:
                continue
            k = int(round(cols.index(c) * (len(ramp) - 1) / max(1, len(cols) - 1)))
            out.putpixel((x, y), tuple(ramp[k][:3]) + (255,))
    return out


def recolour_split(src, grey_ramp, warm_ramp, sat=0.18):
    """Re-shade greyish pixels (metal) with grey_ramp and coloured ones (wood) with warm_ramp, each by rank."""
    def is_grey(c):
        h, l, s = colorsys.rgb_to_hls(c[0] / 255, c[1] / 255, c[2] / 255)
        return s < sat or l > 0.85
    greys = sorted({c for c in src.getdata() if c[3] > 0 and is_grey(c)}, key=lum)
    warms = sorted({c for c in src.getdata() if c[3] > 0 and not is_grey(c)}, key=lum)
    out = Image.new('RGBA', src.size, (0, 0, 0, 0))
    for y in range(src.height):
        for x in range(src.width):
            c = src.getpixel((x, y))
            if c[3] == 0:
                continue
            group, ramp = (greys, grey_ramp) if is_grey(c) else (warms, warm_ramp)
            k = int(round(group.index(c) * (len(ramp) - 1) / max(1, len(group) - 1)))
            out.putpixel((x, y), tuple(ramp[k][:3]) + (255,))
    return out


def layered(path):
    """A vanilla texture with its _overlay drawn on top (leather pieces are split in two layers)."""
    im = vtex(path)
    ov = os.path.join(VANILLA, 'assets', 'minecraft', 'textures', path + '_overlay.png')
    if os.path.isfile(ov):
        im.alpha_composite(Image.open(ov).convert('RGBA'))
    return im


def straw_hat():
    """Explorer's hat: a straw sun hat with a wide brim and a teal band."""
    im = img()
    straw = EXPLORER
    for y in range(5, 10):
        w = [3, 4, 4, 4, 4][y - 5]
        for x in range(8 - w, 8 + w):
            k = 4 if x < 7 else 3
            if (x + y) % 3 == 0:
                k -= 1
            P(im, x, y, straw[k])
    for x in range(4, 12):
        P(im, x, 8, TEAL[2] if x < 11 else TEAL[1])
    for x in range(1, 15):
        for y in (10, 11):
            k = 4 if y == 10 else 2
            if x in (1, 14):
                k -= 1
            P(im, x, y, straw[k])
    return tex.outline(im)


def save(im, rel):
    return tex.save(im, M, rel)


def P(im, x, y, c):
    if 0 <= x < im.width and 0 <= y < im.height:
        im.putpixel((x, y), tex.rgb(c))


def img(w=16, h=16):
    return Image.new('RGBA', (w, h), (0, 0, 0, 0))


def item(iid, name, sprite=None, handheld=False):
    ITEMS[iid] = name
    if sprite is not None:
        save(sprite, f'item/{iid}')
    if handheld:
        A.handheld_item(M, iid)
    else:
        A.flat_item(M, iid)


# ================================================================================================= weapons
def spear():
    ITEMS['shark_tooth_spear'] = 'Shark Tooth Spear'
    save(recolour_split(vtex('item/iron_spear'), TOOTH, TEAK), 'item/shark_tooth_spear')
    save(recolour_split(vtex('item/iron_spear_in_hand'), TOOTH, TEAK), 'item/shark_tooth_spear_in_hand')
    A.item_model(M, 'shark_tooth_spear', textures={'layer0': 'raft:item/shark_tooth_spear'})
    A.item_model(M, 'shark_tooth_spear_in_hand', parent='minecraft:item/spear_in_hand',
                 textures={'layer0': 'raft:item/shark_tooth_spear_in_hand'})
    A.write_json(M, 'assets/raft/items/shark_tooth_spear.json', {
        'model': {'type': 'minecraft:select', 'property': 'minecraft:display_context',
                  'cases': [{'when': ['gui', 'ground', 'fixed', 'on_shelf'],
                             'model': {'type': 'minecraft:model', 'model': 'raft:item/shark_tooth_spear'}}],
                  'fallback': {'type': 'minecraft:model', 'model': 'raft:item/shark_tooth_spear_in_hand'}},
        'swap_animation_scale': 1.95})


def boomerang_sprite():
    """A driftwood boomerang: two broad, slightly curved arms meeting in a rounded elbow, a teal grip band."""
    im = img()
    d = DRIFT
    for t in range(40):
        a = t / 39.0
        # elbow at (4, 4); one arm to the right, one down, both bowing outwards
        for (x0, y0, x1, y1, bow) in ((4.5, 4.5, 14.0, 6.5, 1.2), (4.5, 4.5, 6.5, 14.0, 1.2)):
            x = x0 + (x1 - x0) * a + (0 if x1 > y1 else -bow * math.sin(a * math.pi))
            y = y0 + (y1 - y0) * a + (-bow * math.sin(a * math.pi) if x1 > y1 else 0)
            width = 1.4 if a < 0.85 else 0.9
            for dy in range(-2, 3):
                for dx in range(-2, 3):
                    if dx * dx + dy * dy <= width * width:
                        px, py = int(x + dx), int(y + dy)
                        if 0 <= px < 16 and 0 <= py < 16:
                            k = 4 if dx + dy < -0.5 else 3 if dx + dy < 0.8 else 2
                            P(im, px, py, d[k])
    for x, y in ((5, 6), (6, 5), (6, 6), (5, 7), (7, 5)):
        P(im, x, y, TEAL[2])
    return tex.outline(im)


def machete_sprite():
    im = img()
    for i in range(10):                         # blade, broad towards the tip
        x, y = 5 + i, 10 - i
        w = 2 if i < 4 else 3
        for k in range(w):
            P(im, x + k // 2, y + k, STEEL[5 - min(3, k + (i < 3))] if k == 0 else STEEL[3 - (k > 1)])
        P(im, x + 1, y - 1, STEEL[5]) if i > 6 else None
    for i in range(4):                          # teak grip
        P(im, 2 + i, 13 - i, TEAK[3 + (i % 2)])
        P(im, 3 + i, 13 - i, TEAK[2])
    P(im, 5, 11, BRASS[3])
    P(im, 6, 11, BRASS[4])
    P(im, 6, 10, BRASS[2])
    return tex.outline(im)


def weapons():
    spear()
    item('boomerang', 'Driftwood Boomerang', boomerang_sprite(), handheld=True)
    item('machete', 'Machete', machete_sprite(), handheld=True)


# ================================================================================================= armour
def armour():
    for mat, base, ramp, names in (
            ('shell', 'iron', SHELL, ('Shell Helmet', 'Shell Chestplate', 'Shell Leggings', 'Shell Boots')),
            ('explorer', 'leather', EXPLORER, ("Explorer's Hat", "Explorer's Jacket", "Explorer's Trousers",
                                               "Explorer's Boots"))):
        save(recolour(layered(f'entity/equipment/humanoid/{base}'), ramp), f'entity/equipment/humanoid/{mat}')
        save(recolour(layered(f'entity/equipment/humanoid_leggings/{base}'), ramp), f'entity/equipment/humanoid_leggings/{mat}')
        A.write_json(M, f'assets/raft/equipment/{mat}.json', {'layers': {
            'humanoid': [{'texture': f'raft:{mat}'}], 'humanoid_leggings': [{'texture': f'raft:{mat}'}]}})
        for piece, name in zip(('helmet', 'chestplate', 'leggings', 'boots'), names):
            iid = f'{mat}_{piece}'
            spr = recolour(layered(f'item/{base}_{piece}'), ramp)
            if mat == 'shell':
                # a few scallop ridges on the shell pieces
                for y in range(spr.height):
                    for x in range(spr.width):
                        c = spr.getpixel((x, y))
                        if c[3] and x % 3 == 1 and y % 2 == 0 and lum(c) > lum(ramp[2]):
                            spr.putpixel((x, y), tuple(ramp[1][:3]) + (255,))
            elif piece == 'helmet':
                spr = straw_hat()
            item(iid, name, spr)
    A.tag(M, 'item', 'raft:repairs_shell_armor', ['raft:seashell'])
    A.tag(M, 'item', 'raft:repairs_explorer_armor', ['minecraft:leather', 'raft:sail_canvas'])
    A.tag(M, 'item', 'raft:repairs_shark_tooth', ['raft:shark_tooth'])
    A.tag(M, 'item', 'raft:repairs_machete', ['minecraft:iron_ingot'])
    pieces = {'helmet': 'head', 'chestplate': 'chest', 'leggings': 'leg', 'boots': 'foot'}
    for mat in ('shell', 'explorer'):
        for piece, slot in pieces.items():
            iid = f'{mat}_{piece}'
            A.tag(M, 'item', f'minecraft:{slot}_armor', [iid])
            A.tag(M, 'item', 'minecraft:trimmable_armor', [iid])
    A.tag(M, 'item', 'minecraft:spears', ['shark_tooth_spear'])
    A.tag(M, 'item', 'raft:repairs_boomerang', ['raft:driftwood_planks'])
    A.tag(M, 'block', 'raft:mineable/machete', ['#minecraft:leaves', '#minecraft:replaceable_by_trees', 'minecraft:bamboo',
                                                'minecraft:vine', 'minecraft:cobweb', 'minecraft:sugar_cane',
                                                'minecraft:melon', 'minecraft:pumpkin', 'minecraft:kelp',
                                                'minecraft:kelp_plant', 'minecraft:seagrass', 'minecraft:tall_seagrass',
                                                '#minecraft:wool_carpets', '#minecraft:crops'])
    A.tag(M, 'item', 'minecraft:swords', ['machete'])
    A.tag(M, 'item', 'minecraft:enchantable/durability', ['boomerang', 'machete', 'shark_tooth_spear'])


# ================================================================================================= tools
def compass():
    """32 needle frames: the vanilla compass re-shaded in brass with a teal face, pointing like a lodestone compass."""
    ITEMS['wreck_compass'] = 'Wreck Compass'
    entries = []
    for i in range(32):
        src = vtex(f'item/compass_{i:02d}')
        out = img()
        for y in range(16):
            for x in range(16):
                c = src.getpixel((x, y))
                if c[3] == 0:
                    continue
                h, l, s = colorsys.rgb_to_hls(c[0] / 255, c[1] / 255, c[2] / 255)
                if c[0] > 150 and c[1] < 90:                    # red needle tip stays red
                    out.putpixel((x, y), c)
                elif s < 0.2 and l < 0.55:                       # dark rim -> brass
                    out.putpixel((x, y), tuple(BRASS[min(5, int(l * 9))][:3]) + (255,))
                elif s < 0.2:                                    # light face -> teal
                    out.putpixel((x, y), tuple(TEAL[min(4, int((l - 0.5) * 9) + 1)][:3]) + (255,))
                else:
                    out.putpixel((x, y), tuple(BRASS[min(5, int(l * 7) + 1)][:3]) + (255,))
        save(out, f'item/wreck_compass_{i:02d}')
        A.item_model(M, f'wreck_compass_{i:02d}', textures={'layer0': f'raft:item/wreck_compass_{i:02d}'})
    first = json.load(open(os.path.join(VANILLA, 'assets', 'minecraft', 'items', 'compass.json')))

    def swap(o):
        if isinstance(o, dict):
            return {k: (v.replace('minecraft:item/compass_', 'raft:item/wreck_compass_') if k == 'model' and isinstance(v, str)
                        else swap(v)) for k, v in o.items()}
        if isinstance(o, list):
            return [swap(x) for x in o]
        return o
    A.write_json(M, 'assets/raft/items/wreck_compass.json', swap(first))


def spyglass():
    ITEMS['captains_spyglass'] = "Captain's Spyglass"
    save(recolour_split(vtex('item/spyglass'), STEEL, BRASS, sat=0.3), 'item/captains_spyglass')
    save(recolour_split(vtex('item/spyglass_model'), STEEL, BRASS, sat=0.3), 'item/captains_spyglass_model')
    A.item_model(M, 'captains_spyglass', textures={'layer0': 'raft:item/captains_spyglass'})
    A.item_model(M, 'captains_spyglass_in_hand', parent='minecraft:item/spyglass_in_hand',
                 textures={'spyglass': 'raft:item/captains_spyglass_model', 'particle': 'raft:item/captains_spyglass_model'})
    A.write_json(M, 'assets/raft/items/captains_spyglass.json', {
        'model': {'type': 'minecraft:select', 'property': 'minecraft:display_context',
                  'cases': [{'when': ['gui', 'ground', 'fixed', 'on_shelf'],
                             'model': {'type': 'minecraft:model', 'model': 'raft:item/captains_spyglass'}}],
                  'fallback': {'type': 'minecraft:model', 'model': 'raft:item/captains_spyglass_in_hand'}}})


def bait_sprites():
    im = img()                                     # a small pouch of chum with a hook
    sack = pal('#4a3018', '#664424', '#825a30', '#9c7040', '#b48a56')
    for y in range(6, 14):
        w = 4 if y > 7 else 3
        for x in range(8 - w, 8 + w):
            P(im, x, y, sack[3 if x < 7 else 2] if y < 12 else sack[1])
    for x in range(6, 10):
        P(im, x, 5, sack[4])
    for x, y in ((6, 8), (9, 10), (7, 11)):
        P(im, x, y, '#b8504a')
    P(im, 8, 4, STEEL[4])
    P(im, 9, 3, STEEL[3])
    P(im, 10, 4, STEEL[3])
    bait = tex.outline(im)
    im = img()                                     # a golden spoon lure with a red bead
    g = pal('#6b4410', '#9c6a17', '#cc9524', '#efbe3c', '#fbdc74', '#fff3bd')
    for y in range(4, 13):
        w = 2 if 5 < y < 11 else 1
        for x in range(8 - w, 8 + w):
            P(im, x, y, g[4] if x < 8 else g[2])
    P(im, 7, 6, g[5])
    P(im, 8, 3, '#c83a2a')
    P(im, 8, 2, STEEL[3])
    for x, y in ((9, 13), (10, 14), (9, 14)):
        P(im, x, y, STEEL[4])
    return bait, tex.outline(im)


def rod_sprite(name):
    """The vanilla rod with a pale driftwood pole, a brass reel and a teal grip."""
    src = vtex(f'item/{name}')
    out = src.copy()
    wood = sorted({c for c in src.getdata() if c[3] and colorsys.rgb_to_hls(*[v / 255 for v in c[:3]])[2] > 0.3}, key=lum)
    for y in range(16):
        for x in range(16):
            c = src.getpixel((x, y))
            if c[3] == 0:
                continue
            if c in wood:
                k = 1 + int(round(wood.index(c) * 3 / max(1, len(wood) - 1)))
                out.putpixel((x, y), tuple(DRIFT[k][:3]) + (255,))
            elif 5 <= x <= 8 and 11 <= y <= 13:                       # the reel
                out.putpixel((x, y), tuple(BRASS[1 + min(4, int(lum(c) / 50))][:3]) + (255,))
    for x, y, k in ((2, 13, 2), (3, 13, 3), (2, 14, 1), (3, 14, 2)):      # grip
        if out.getpixel((x, y))[3]:
            out.putpixel((x, y), tuple(TEAL[k][:3]) + (255,))
    return out


def tackle_box_gui():
    """176x157 window: the chest window's top and player inventory with a plain middle holding the bait and lure
    slots (TackleMenu.java: slots at x 12, y 20 and 40; their labels are drawn beside them), each with a faint
    picture of what goes in."""
    chest = vanilla_png('gui/container/generic_54')
    gui = Image.new('RGBA', (256, 256), (0, 0, 0, 0))
    gui.paste(chest.crop((0, 0, 176, 17)), (0, 0))
    line = chest.crop((0, 10, 176, 11))
    for y in range(17, 61):
        gui.paste(line, (0, y))
    gui.paste(chest.crop((0, 126, 176, 222)), (0, 61))
    frame = chest.crop((7, 17, 25, 35))
    bait, lure = bait_sprites()
    for y, spr in ((19, bait), (39, lure)):
        gui.paste(frame, (11, y))
        ghost = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
        for yy in range(16):
            for xx in range(16):
                c = spr.getpixel((xx, yy))
                if c[3]:
                    g = int(lum(c) * 0.35 + 139 * 0.65)
                    ghost.putpixel((xx, yy), (g, g, g, 150))
        gui.alpha_composite(ghost, (12, y + 1))
    tex.save(gui, M, 'gui/container/tackle_box')


def fishing_rod():
    ITEMS['driftwood_fishing_rod'] = 'Driftwood Fishing Rod'
    save(rod_sprite('fishing_rod'), 'item/driftwood_fishing_rod')
    save(rod_sprite('fishing_rod_cast'), 'item/driftwood_fishing_rod_cast')
    for n in ('driftwood_fishing_rod', 'driftwood_fishing_rod_cast'):
        A.item_model(M, n, parent='minecraft:item/handheld_rod', textures={'layer0': f'raft:item/{n}'})
    A.write_json(M, 'assets/raft/items/driftwood_fishing_rod.json', {'model': {
        'type': 'minecraft:condition', 'property': 'minecraft:fishing_rod/cast',
        'on_false': {'type': 'minecraft:model', 'model': 'raft:item/driftwood_fishing_rod'},
        'on_true': {'type': 'minecraft:model', 'model': 'raft:item/driftwood_fishing_rod_cast'}}})
    A.tag(M, 'item', 'minecraft:enchantable/fishing', ['driftwood_fishing_rod'])
    A.tag(M, 'item', 'minecraft:enchantable/durability', ['driftwood_fishing_rod'])
    A.tag(M, 'item', 'raft:repairs_driftwood_fishing_rod', ['raft:driftwood_planks'])
    tackle_box_gui()


def tools():
    compass()
    spyglass()
    bait, lure = bait_sprites()
    item('fish_bait', 'Fish Bait', bait)
    item('golden_lure', 'Golden Lure', lure)
    fishing_rod()
    A.tag(M, 'item', 'raft:bait', ['fish_bait', 'golden_lure'])
    A.tag(M, 'worldgen/structure', 'raft:wrecks', [{'id': f'raft:{s}', 'required': False} for s in (
        'wreck_galleon', 'wreck_cargo_ship', 'wreck_fishing_boat', 'sunken_plane')] + ['minecraft:shipwreck',
                                                                                         'minecraft:shipwreck_beached'])


# ================================================================================================= recipes
def recipes():
    b = Book(M, set(ITEMS))
    b.shaped('shark_tooth_spear', ['  T', ' S ', 'S  '], {'T': 'raft:shark_tooth', 'S': 'minecraft:stick'},
             'shark_tooth_spear', 1, 'equipment')
    b.shaped('boomerang', ['PP', ' P'], {'P': 'raft:driftwood_planks'}, 'boomerang', 1, 'equipment')
    b.shaped('machete', ['  I', ' I ', 'T  '], {'I': 'minecraft:iron_ingot', 'T': 'raft:teak_planks'}, 'machete', 1,
             'equipment')
    for mat, key in (('shell', 'raft:seashell'), ('explorer', 'raft:sail_canvas')):
        b.shaped(f'{mat}_helmet', ['XXX', 'X X'], {'X': key}, f'{mat}_helmet', 1, 'equipment')
        b.shaped(f'{mat}_chestplate', ['X X', 'XXX', 'XXX'], {'X': key}, f'{mat}_chestplate', 1, 'equipment')
        b.shaped(f'{mat}_leggings', ['XXX', 'X X', 'X X'], {'X': key}, f'{mat}_leggings', 1, 'equipment')
        b.shaped(f'{mat}_boots', ['X X', 'X X'], {'X': key}, f'{mat}_boots', 1, 'equipment')
    b.shaped('wreck_compass', [' G ', 'GCG', ' G '], {'G': 'minecraft:gold_ingot', 'C': 'minecraft:compass'},
             'wreck_compass', 1, 'equipment')
    b.shaped('captains_spyglass', ['G', 'S', 'P'], {'G': 'minecraft:gold_ingot', 'S': 'minecraft:spyglass',
                                                    'P': 'raft:pearl'}, 'captains_spyglass', 1, 'equipment')
    b.shapeless('fish_bait', [['minecraft:rotten_flesh', 'minecraft:spider_eye', 'raft:crab_claw', 'minecraft:cod'],
                              'minecraft:kelp'], 'fish_bait', 4)
    b.shapeless('golden_lure', ['fish_bait', 'minecraft:gold_nugget', 'minecraft:gold_nugget', 'minecraft:gold_nugget'],
                'golden_lure', 1)
    b.shaped('driftwood_fishing_rod', ['  D', ' DS', 'D S'], {'D': 'raft:driftwood_planks', 'S': 'minecraft:string'},
             'driftwood_fishing_rod', 1, 'equipment')
    b.finish('tools')


def main():
    out = module_dir(M)
    if os.path.isdir(out):
        shutil.rmtree(out)
    os.makedirs(out)
    weapons()
    armour()
    tools()
    for i, name in ITEMS.items():
        LANG[f'item.raft.{i}'] = name
    recipes()
    A.sounds(M, {'boomerang.throw': {'sounds': [{'name': 'minecraft:item/trident/throw1', 'pitch': 1.3},
                                                {'name': 'minecraft:item/trident/throw2', 'pitch': 1.3}],
                                     'subtitle': 'subtitles.raft.boomerang.throw'}})
    LANG.update(PAGES['gear'])
    A.lang(M, LANG)
    A.flush(M)
    n = sum(len(fs) for _, _, fs in os.walk(out))
    print(f'gear: {len(ITEMS)} items, {n} files')


if __name__ == '__main__':
    main()
