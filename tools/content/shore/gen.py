"""Generator of all resources of the shore module (textures, item models, lang, loot, recipes, tags, sounds).

Run: cd tools && python3 -m content.shore.gen   (writes into src/main/modules/shore/)"""
import os
import sys

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..')))

from content.shore import creatures, data, items  # noqa: E402


def main():
    creatures.build()
    items.build()
    data.build()
    print('shore: resources written')


if __name__ == '__main__':
    main()
