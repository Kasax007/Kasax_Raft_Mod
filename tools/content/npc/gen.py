"""Generator of all resources of the npc module (skins, spawn eggs, the conversation panel, lang, advancements).

Run: cd tools && python3 -m content.npc.gen [--preview]   (writes into src/main/modules/npc/)
The people are placed into the structure templates by content/npc/place.py (run separately)."""
import json
import os
import sys

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..')))

from PIL import Image  # noqa: E402

from bclib import assets, tex  # noqa: E402
from bclib.paths import out  # noqa: E402
from content.npc import characters, texts  # noqa: E402

MOD = 'npc'
PREVIEW = '/tmp/claude-0/-home-user/4ca9508b-4f8c-5a8e-91dc-6059d6cc887c/scratchpad/work/npcs/prev'

# spawn egg colours: (shell, spots, detail)
EGGS = {
    'keeper_fenn': ('#2a3756', '#ebe7df', '#d6a83c'),
    'nalu': ('#1d7f86', '#b3373a', '#f1ead8'),
    'old_tamo': ('#dca728', '#b8322f', '#2f4a35'),
    'dr_voss': ('#e8e6de', '#2b6b70', '#c19a4a'),
    'teo': ('#e0782c', '#f6efe0', '#3f8a4a'),
    'kai': ('#7db4d4', '#d6b062', '#c2352f'),
    'dr_lund': ('#df5f2a', '#e2ddd3', '#3b5f91'),
    'okafor': ('#2e5d9b', '#e2e65a', '#ef8a24'),
}


# ---------------------------------------------------------------------------------------------- spawn eggs
def egg_mask():
    m = [[0] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            dx = (x - 7.5) / 5.4
            dy = (y - 8.6) / 6.6
            dx *= 1.0 + max(0.0, -dy) * 0.28
            if dx * dx + dy * dy <= 1.0 and 1 <= y <= 15:
                m[y][x] = 1
    return m


def spawn_egg(pid):
    shell, spot, detail = EGGS[pid]
    img = tex.new()
    m = egg_mask()
    r = tex.ramp(shell, 6, spread=0.5)
    sp = tex.ramp(spot, 4, spread=0.35)
    for y in range(16):
        for x in range(16):
            if not m[y][x]:
                continue
            # light from the top-left: brighter towards (5, 5)
            d = ((x - 5.0) ** 2 + (y - 5.0) ** 2) ** 0.5
            v = 0.85 - d * 0.07
            tex.px(img, x, y, r[max(0, min(5, int(round(v * 5))))])
    # a band (shirt / coat) across the lower half and spots above
    for x in range(16):
        for y in (10, 11):
            if m[y][x]:
                tex.px(img, x, y, sp[2] if y == 10 else sp[1])
    for (x, y) in ((6, 4), (9, 6), (5, 8), (10, 13), (6, 13)):
        if m[y][x]:
            tex.px(img, x, y, sp[2])
    tex.px(img, 8, 11, detail)
    tex.px(img, 5, 3, '#ffffff')                     # gloss
    return tex.outline(img, tex.darker(r[0], 0.7))


# ---------------------------------------------------------------------------------------------- conversation panel
def panel():
    """64x64 nine-slice (border 8): driftwood frame with brass corner studs around parchment. The edges and the
    centre are tiled by the game, so every pattern repeats every 16 pixels (48 = 3 x 16)."""
    W = 64
    img = Image.new('RGBA', (W, W), (0, 0, 0, 0))
    wood = tex.ramp('#8c6a48', 6, spread=0.5)
    paper = tex.ramp('#ecdfbf', 5, spread=0.14)
    brass = tex.ramp('#d6a83c', 4, spread=0.45)
    grain = [0.0, 0.05, 0.08, 0.04, -0.02, -0.06, -0.03, 0.02, 0.06, 0.03, -0.04, -0.08, -0.05, 0.0, 0.04, 0.02]
    for y in range(W):
        for x in range(W):
            edge = min(x, y, W - 1 - x, W - 1 - y)
            horizontal = (y < 8 or y > W - 9) and not (x < edge or x > W - 1 - edge)
            along = x if (y <= x <= W - 1 - y or W - 1 - y <= x <= y) else y
            if edge == 0:
                c = '#2a1d14'
            elif edge < 6:
                lit = (y <= x <= W - 1 - y and y < 32) or (x <= y <= W - 1 - x and x < 32)
                v = 0.58 if lit else 0.36
                v += grain[(along + edge * 5) % 16]
                if edge == 1:
                    v += 0.14
                elif edge == 5:
                    v -= 0.14
                if along % 16 == 7 and edge in (2, 3):
                    v -= 0.12                                 # a knot every 16 pixels
                c = wood[max(0, min(5, int(round(v * 5))))]
            elif edge == 6:
                c = '#4a3524'
            elif edge == 7:
                c = paper[1]
            else:
                v = 0.66
                if (x + 2 * y) % 16 == 0 or (3 * x + y) % 16 == 5:
                    v -= 0.08                                 # faint fibres in the paper
                c = paper[max(0, min(4, int(round(v * 4))))]
            tex.px(img, x, y, c)
    for (cx, cy) in ((2, 2), (W - 4, 2), (2, W - 4), (W - 4, W - 4)):
        tex.px(img, cx, cy, brass[3]); tex.px(img, cx + 1, cy, brass[2])
        tex.px(img, cx, cy + 1, brass[2]); tex.px(img, cx + 1, cy + 1, brass[1])
    p = out(MOD, 'assets', 'raft', 'textures', 'gui', 'sprites', 'npc', 'dialog.png')
    img.save(p)
    with open(p + '.mcmeta', 'w') as f:
        json.dump({'gui': {'scaling': {'type': 'nine_slice', 'width': W, 'height': W, 'border': 8}}}, f, indent=2)
        f.write('\n')
    return img


# ---------------------------------------------------------------------------------------------- data
def advancements():
    def adv(name, icon, criteria, frame=None):
        data = {
            'parent': 'raft:adventure/root',
            'criteria': {c: {'trigger': 'minecraft:impossible'} for c in criteria},
            'display': {
                'icon': {'id': icon},
                'title': {'translate': f'advancements.raft.adventure.{name}.title'},
                'description': {'translate': f'advancements.raft.adventure.{name}.description'},
            },
            'requirements': [[c] for c in criteria],
        }
        if frame:
            data['display']['frame'] = frame
        assets.write_json(MOD, f'data/raft/advancement/adventure/{name}.json', data)
    adv('new_friend', 'minecraft:spyglass', ['talk'])
    adv('every_soul', 'minecraft:cake', list(texts.PEOPLE), frame='challenge')


def main():
    preview = '--preview' in sys.argv
    bad = texts.check()
    if bad:
        raise SystemExit(f'missing texts: {bad}')
    sheets = []
    for pid, fn in characters.ALL.items():
        s = fn()
        s.save(MOD, pid)
        sheets.append(s.preview(6))
        tex.save(spawn_egg(pid), MOD, f'item/{pid}_spawn_egg')
        assets.spawn_egg(MOD, f'{pid}_spawn_egg')
    for pid, img in characters.extras().items():
        tex.save(img, MOD, f'entity/npc/{pid}_extra')
    panel()
    advancements()
    assets.lang(MOD, texts.lang())
    assets.flush(MOD)
    if preview:
        os.makedirs(PREVIEW, exist_ok=True)
        w, h = sheets[0].size
        big = Image.new('RGBA', (w * 4, h * 2), (40, 40, 40, 255))
        for i, p in enumerate(sheets):
            big.alpha_composite(p, ((i % 4) * w, (i // 4) * h))
        big.save(os.path.join(PREVIEW, 'skins.png'))
        eggs = Image.new('RGBA', (16 * 8 + 14, 18), (60, 60, 60, 255))
        for i, pid in enumerate(characters.ALL):
            eggs.alpha_composite(spawn_egg(pid), (1 + i * 18, 1))
        eggs.resize((eggs.width * 6, eggs.height * 6), Image.NEAREST).save(os.path.join(PREVIEW, 'eggs.png'))
        panel().resize((64 * 6, 64 * 6), Image.NEAREST).save(os.path.join(PREVIEW, 'panel.png'))
    print('npc: resources written')


if __name__ == '__main__':
    main()
