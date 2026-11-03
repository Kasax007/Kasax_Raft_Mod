"""Generates every resource of the voyage module into src/main/modules/voyage/ and the Expedition Raft geometry
(src/main/java/net/kasax/raft/content/voyage/client/ExpeditionRaftGeometry.java).

Run: cd tools && python3 -m content.voyage.gen   (or python3 tools/content/voyage/gen.py)
Previews go to $BC_PREVIEW (default: the module's work folder in the scratchpad), not into the mod.
"""
import os
import sys

if __package__ in (None, ''):
    sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..')))
    __package__ = 'content.voyage'

from bclib import assets, tex  # noqa: E402

from content.voyage import boats, data, gui, items, raftmodel  # noqa: E402

M = 'voyage'
PREVIEW = os.environ.get('BC_PREVIEW', '/tmp/claude-0/-home-user/4ca9508b-4f8c-5a8e-91dc-6059d6cc887c/scratchpad/work/voyage')


def main():
    root = raftmodel.build()
    pos = raftmodel.pack()
    img, glow = raftmodel.texture(pos)
    tex.save(img, M, 'entity/expedition_raft')
    tex.save(glow, M, 'entity/expedition_raft_glow')
    raftmodel.java(root, pos)
    _, boat_items = boats.generate()
    sprites = items.generate()
    gui.generate()
    data.generate()
    assets.flush(M)
    if PREVIEW and os.path.isdir(os.path.dirname(PREVIEW)):
        os.makedirs(PREVIEW, exist_ok=True)
        raftmodel.preview(root, pos, img, os.path.join(PREVIEW, 'expedition_raft_preview.png'))
        tex.sheet(list(sprites.values()) + list(boat_items.values()), os.path.join(PREVIEW, 'voyage_items.png'), scale=6, cols=5)


if __name__ == '__main__':
    main()
