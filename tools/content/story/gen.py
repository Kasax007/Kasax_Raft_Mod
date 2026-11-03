"""Generator of the "story" module (docs/design/beachcraft3.md, sections 3 and 9).

Run:  cd tools && python3 -m content.story.gen
Writes textures, models, blockstates, item definitions, sounds, loot tables, advancements, tags, recipes and the
lang (all story texts) into src/main/modules/story/. Re-runnable: it wipes its own output folder first.
"""
import os
import shutil
import sys

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..')))

from bclib import assets as A  # noqa: E402
from bclib.paths import module_dir  # noqa: E402

from content.story import art, data, sounds  # noqa: E402

M = 'story'


def main():
    shutil.rmtree(module_dir(M), ignore_errors=True)
    os.makedirs(module_dir(M))
    art.generate()
    data.generate()
    sounds.generate()
    A.flush(M)
    n = sum(len(f) for _, _, f in os.walk(module_dir(M)))
    print(f'story: {n} files written')


if __name__ == '__main__':
    main()
