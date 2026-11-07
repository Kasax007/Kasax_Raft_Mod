"""Item textures of the encounters module: the spawn eggs (16x16, same egg shape as the shore creatures)."""
from bclib import tex

from content.shore.items import egg_mask, shade_mask, finish, px

MOD = 'encounters'


def spawn_egg(kind):
    img = tex.new()
    m = egg_mask()
    if kind == 'scavenger':
        shade_mask(img, m, tex.ramp('#b3a77c', 6, spread=0.5), base=0.55)
        # rust bandana with goggles on top, leather vest stripes below
        for y in range(1, 5):
            for x in range(2, 14):
                if m[y][x]:
                    px(img, x, y, '#b4532e' if (x + y) % 4 else '#8e3f22')
        px(img, 5, 3, '#7fb3c4'); px(img, 10, 3, '#7fb3c4'); px(img, 6, 3, '#c99a45'); px(img, 9, 3, '#c99a45')
        for y in range(8, 15):
            for x in (4, 5, 10, 11):
                if m[y][x]:
                    px(img, x, y, '#5c4231')
        px(img, 7, 10, '#c99a45'); px(img, 8, 12, '#c99a45')
        return finish(img, '#2a2016')
    if kind == 'reef_guardian':
        shade_mask(img, m, tex.ramp('#3f8a7a', 6, spread=0.5), base=0.5)
        for (x, y) in ((4, 4), (11, 5), (5, 11), (10, 12), (7, 2), (12, 9), (3, 8)):
            px(img, x, y, '#e08a80'); px(img, x + 1, y, '#c9655e')
        px(img, 7, 7, '#f5c64a'); px(img, 8, 7, '#e2a836'); px(img, 7, 8, '#3a2a10'); px(img, 8, 8, '#f5c64a')
        return finish(img, '#163028')
    if kind == 'corsair_captain':
        shade_mask(img, m, tex.ramp('#24325e', 6, spread=0.5), base=0.5)
        hat = tex.ramp('#2c2427', 4, spread=0.3)
        for y in range(1, 5):
            for x in range(2, 14):
                if y == 4 or (y == 3 and 3 <= x <= 12) or (y <= 2 and 4 <= x <= 11 and m[y][x]):
                    px(img, x, y, hat[1 + (y < 3)])
        for x in range(2, 14):
            px(img, x, 4, '#dcaa3e' if x % 2 else '#b88a2a')
        px(img, 11, 1, '#c8352e'); px(img, 12, 2, '#c8352e'); px(img, 12, 1, '#e0574a')
        for y in range(6, 15):
            px(img, 7, y, '#dcaa3e') if m[y][7] else None
        px(img, 5, 6, '#a8f4ea'); px(img, 10, 6, '#a8f4ea')
        return finish(img, '#141a30')
    raise ValueError(kind)


SPAWN_EGGS = ('scavenger', 'reef_guardian', 'corsair_captain')


def build():
    out = {}
    for k in SPAWN_EGGS:
        out[f'{k}_spawn_egg'] = spawn_egg(k)
        tex.save(out[f'{k}_spawn_egg'], MOD, f'item/{k}_spawn_egg')
    return out
