"""Generates every resource of the marine module into src/main/modules/marine/.

Run: cd tools && python3 -m content.marine.gen   (or python3 tools/content/marine/gen.py)
Entity textures are painted onto the boxes read from the Java models, so change the geometry in Java and re-run.
"""
import os
import sys

if __package__ in (None, ''):
    sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..')))
    __package__ = 'content.marine'

from bclib import assets  # noqa: E402

from content.marine import creatures, data, items, sounds  # noqa: E402

M = 'marine'


def main():
    creatures.shark_textures()
    creatures.whale_texture()
    creatures.manta_texture()
    creatures.jellyfish_textures()
    creatures.anglerfish_textures()
    creatures.moray_texture()
    items.generate()
    songs, spouts = sounds.generate()
    data.generate(songs, spouts)
    assets.flush(M)
    print('marine: done')


if __name__ == '__main__':
    main()
