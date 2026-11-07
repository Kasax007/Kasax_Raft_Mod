"""Generator of all resources of the encounters module (textures, item models, lang, loot, tags, sounds).

Run: cd tools && python3 -m content.encounters.gen   (writes into src/main/modules/encounters/)"""
import os
import sys

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..')))

from content.encounters import creatures, data, items  # noqa: E402


def main():
    creatures.build()
    items.build()
    data.build()
    print('encounters: resources written')


if __name__ == '__main__':
    main()
