"""Generates every resource of the guide module (the Seafarer's Almanac, spec 15) into src/main/modules/guide/.

Run: cd tools && python3 -m content.guide.gen   (or python3 tools/content/guide/gen.py)
Writes: item model + texture, the GUI texture, the recipe (+ recipe-book unlock), lang (all almanac texts).
Previews (GUI at 2x, item sheet) go to $BC_PREVIEW (default: the module's work folder in the scratchpad).
"""
import os
import sys

if __package__ in (None, ''):
    sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..')))
    __package__ = 'content.guide'

from PIL import Image  # noqa: E402

from bclib import assets, paths, tex  # noqa: E402

from content.guide import art, texts  # noqa: E402

M = 'guide'
ITEM = 'seafarers_almanac'
PREVIEW = os.environ.get('BC_PREVIEW', '/tmp/claude-0/-home-user/4ca9508b-4f8c-5a8e-91dc-6059d6cc887c/scratchpad/work/guide')


def textures():
    gui = art.gui()
    tex.save(gui, M, 'gui/almanac')
    sprite = art.item()
    tex.save(sprite, M, f'item/{ITEM}')
    return gui, sprite


def models():
    assets.flat_item(M, ITEM)
    assets.item_def(M, ITEM)


def recipes():
    # early game: a book and something from the sea (kelp, dried kelp or driftwood)
    assets.recipe_shapeless(M, ITEM, ['minecraft:book', ['minecraft:kelp', 'minecraft:dried_kelp',
                                                         'raft:driftwood_log', 'raft:driftwood_planks']], ITEM)
    assets.write_json(M, f'data/raft/advancement/recipes/misc/{ITEM}.json', {
        'parent': 'minecraft:recipes/root',
        'criteria': {
            'has_the_recipe': {'trigger': 'minecraft:recipe_unlocked', 'conditions': {'recipes': f'raft:{ITEM}'}},
            'has_book': {'trigger': 'minecraft:inventory_changed', 'conditions': {'items': [{'items': 'minecraft:book'}]}},
            'has_driftwood': {'trigger': 'minecraft:inventory_changed',
                              'conditions': {'items': [{'items': ['raft:driftwood_log', 'raft:driftwood_planks']}]}},
        },
        'requirements': [['has_the_recipe', 'has_book', 'has_driftwood']],
        'rewards': {'recipes': [f'raft:{ITEM}']},
    })


def lang():
    assets.lang(M, texts.lang())


def preview(gui, sprite):
    os.makedirs(PREVIEW, exist_ok=True)
    gui.resize((gui.width * 2, gui.height * 2), Image.NEAREST).save(os.path.join(PREVIEW, 'gui_2x.png'))
    tex.sheet([sprite, 'minecraft:item/book', 'minecraft:item/knowledge_book', 'raft:item/expedition_journal'],
              os.path.join(PREVIEW, 'item_sheet.png'), scale=8, cols=4)


def main():
    gui, sprite = textures()
    models()
    recipes()
    lang()
    assets.flush(M)
    preview(gui, sprite)
    print('guide: resources written to', paths.module_dir(M))


if __name__ == '__main__':
    main()
