"""Images of the CurseForge page for Beach Craft 3.3 (docs/curseforge/img): three section strips (Cozy living,
Island kitchen, Gear), the cozy collage from in-game screenshots and the two item palettes (crops and dishes, gear).
Same look as make_images3.py, whose helpers (strip, collage, palette, tex, save) are used here.

  python3 docs/curseforge/make_images33.py SHOTS_DIR FONT.ttf [what ...]

SHOTS_DIR: the bctest output of tools/content/cozy/tests/scene.txt (PNG screenshots). FONT: Luckiest Guy.
what: strips collage kitchen gear (default: all).
"""
import os
import sys

from PIL import Image

import make_images3 as m3          # reads sys.argv[1:3] (SHOTS, FONT) on import

from make_images3 import W, tex, strip, palette



def icon(path, size=96):
    """A flat 16x16 sprite scaled up without blur."""
    return tex(path, size)


def strips():
    strip('cozy', 'COZY LIVING', _item_icon('cozy', 'thatch_parasol'))
    strip('kitchen', 'THE ISLAND KITCHEN', 'food:item/island_curry')
    strip('gear', 'GEAR AND GADGETS', 'gear:item/boomerang')


def _item_icon(module, name):
    return tex(f'{module}:item/{name}', 72)


def collage():
    m3.collage('cozy_33.jpg', [
        (430, [dict(src='10_scene_day.jpg', w=1.6, cap='A cozy beach house'),
               dict(src='12_scene_dusk.jpg', w=1, cap='Lights for the evening')]),
        (330, [dict(src='13_resting.jpg', w=1, cap='Sit down and rest'),
               dict(src='14_kitchen.jpg', w=1, cap='Cook in the pot'),
               dict(src='15_plate.jpg', w=1, cap='Serve on a plate')]),
    ])


KITCHEN = [('Sweet Potato', 'food:item/sweet_potato'), ('Chili Pepper', 'food:item/chili_pepper'),
           ('Banana', 'food:item/banana'), ('Rice', 'food:item/rice'), ('Sea Grapes', 'food:item/sea_grapes'),
           ('Baked Sweet Potato', 'food:item/baked_sweet_potato'), ('Cooked Rice', 'food:item/cooked_rice'),
           ('Sushi Roll', 'food:item/sushi_roll'), ('Banana Bread', 'food:item/banana_bread'),
           ('Fruit Salad', 'food:item/fruit_salad'), ('Chili Crab', 'food:item/chili_crab'),
           ('Island Curry', 'food:item/island_curry'), ('Seafood Chowder', 'food:item/seafood_chowder'),
           ('Sweet Potato Soup', 'food:item/sweet_potato_soup'), ('Coconut Ice Cream', 'food:item/coconut_ice_cream'),
           ('Tropical Smoothie', 'food:item/tropical_smoothie')]

GEAR = [('Shark Tooth Spear', 'gear:item/shark_tooth_spear'), ('Boomerang', 'gear:item/boomerang'),
        ('Machete', 'gear:item/machete'), ('Shell Helmet', 'gear:item/shell_helmet'),
        ('Shell Chestplate', 'gear:item/shell_chestplate'), ('Shell Leggings', 'gear:item/shell_leggings'),
        ('Shell Boots', 'gear:item/shell_boots'), ("Explorer's Hat", 'gear:item/explorer_helmet'),
        ("Explorer's Jacket", 'gear:item/explorer_chestplate'), ("Explorer's Trousers", 'gear:item/explorer_leggings'),
        ("Explorer's Boots", 'gear:item/explorer_boots'), ('Wreck Compass', 'gear:item/wreck_compass_08'),
        ("Captain's Spyglass", 'gear:item/captains_spyglass'), ('Fish Bait', 'gear:item/fish_bait'),
        ('Golden Lure', 'gear:item/golden_lure'), ('Fish Trap', None)]


def kitchen():
    palette('kitchen_33.png', [(label, icon(p)) for label, p in KITCHEN])


def gear():
    entries = []
    for label, p in GEAR:
        if p is None:
            shot_path = os.path.join(m3.SHOTS, 'icon_fish_trap.png')
            ic = Image.open(shot_path).convert('RGBA') if os.path.exists(shot_path) else icon('food:block/fish_trap_end')
            entries.append((label, ic.resize((96, 96), Image.NEAREST)))
        else:
            entries.append((label, icon(p)))
    palette('gear_33.png', entries)


if __name__ == '__main__':
    what = sys.argv[3:] or ['strips', 'collage', 'kitchen', 'gear']
    for w in what:
        globals()[w]()
