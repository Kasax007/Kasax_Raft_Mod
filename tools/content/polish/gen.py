"""Polish pass over the 2.0 textures (src/main/resources/assets/raft/textures) for the Beach Craft 3 art direction.

Run:  cd tools && python3 -m content.polish.gen            (writes the textures)
      cd tools && python3 -m content.polish.gen --preview <out_dir> [filter]
                                                            (old | new contact sheets + 3x3 tiling, writes nothing)
File names, sizes and UV layouts stay as they were (models depend on them); only the paint changes. Cut-out shapes
of model textures (rope coil, rope ladder) are read from git HEAD so the script is re-runnable.
"""
import os
import subprocess
import sys

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..')))

from PIL import Image  # noqa: E402

from bclib import tex  # noqa: E402
from bclib.paths import ROOT  # noqa: E402
from content.polish import common as C  # noqa: E402
from content.polish import wood as W  # noqa: E402
from content.polish import metal as M  # noqa: E402
from content.polish import items as I  # noqa: E402

REL_GIT = 'src/main/resources/assets/raft/textures/'


def head(rel):
    """The 2.0 texture as committed (git HEAD), so repainting from an old mask does not feed on its own output."""
    import io
    try:
        data = subprocess.run(['git', '-C', ROOT, 'show', 'HEAD:' + REL_GIT + rel + '.png'], capture_output=True,
                              check=True).stdout
        return Image.open(io.BytesIO(data)).convert('RGBA')
    except Exception:
        return Image.open(os.path.join(C.TEX, rel + '.png')).convert('RGBA')


def purifier_gui_frame():
    """The 2.0 purifier GUI had a square, flat frame; give it the vanilla rounded bevel of the other two GUIs
    (only the outer 4 px of the 176x166 panel change; slots, flame and arrow areas stay as they are)."""
    img = head('gui/water_purifier_gui').copy()
    ref = head('gui/item_collector_gui')
    for y in range(166):
        for x in range(176):
            if x < 4 or y < 4 or x > 171 or y > 161:
                img.putpixel((x, y), ref.getpixel((x, y)))
    return img


def jobs():
    """rel -> zero-arg painter (returns an image or a list of animation frames)."""
    j = {
        # ---- driftwood
        'block/driftwood_planks': W.driftwood_planks,
        'block/driftwood_log': W.driftwood_log,
        'block/driftwood_log_top': W.driftwood_log_top,
        'block/stripped_driftwood_log': W.stripped_driftwood_log,
        'block/stripped_driftwood_log_top': W.stripped_driftwood_log_top,
        'block/driftwood_leaves': W.driftwood_leaves,
        'block/driftwood_door_bottom': W.driftwood_door_bottom,
        'block/driftwood_door_top': W.driftwood_door_top,
        'block/driftwood_trapdoor': W.driftwood_trapdoor,
        'item/driftwood_door': lambda: W.door_item('driftwood'),
        # ---- palm
        'block/palm_planks': W.palm_planks,
        'block/palm_log': W.palm_log,
        'block/palm_log_top': W.palm_log_top,
        'block/stripped_palm_log': W.stripped_palm_log,
        'block/stripped_palm_log_top': W.stripped_palm_log_top,
        'block/palm_leaves': W.palm_leaves,
        'block/palm_coconut_leaves': W.palm_coconut_leaves,
        'block/palm_thatch': W.palm_thatch,
        'block/palm_door_bottom': W.palm_door_bottom,
        'block/palm_door_top': W.palm_door_top,
        'block/palm_trapdoor': W.palm_trapdoor,
        'item/palm_door': lambda: W.door_item('palm'),
        # ---- raft, rope, canvas, farming
        'block/raft_foundation': W.raft_foundation,
        'block/reinforced_foundation': W.reinforced_foundation,
        'block/rope_coil': lambda: W.rope_coil(head('block/rope_coil')),
        'block/rope_ladder': lambda: W.rope_ladder(head('block/rope_ladder')),
        'block/sail_canvas': lambda: W.canvas(),
        'block/sail_canvas_red_striped': lambda: W.canvas(stripes=[(3, 5), (10, 12)]),
        'block/raft_flag_pole': W.raft_flag_pole,
        'block/crop_plot_side': W.crop_plot_side,
        'block/crop_plot_top': W.crop_plot_top,
        'block/item_catcher': W.item_catcher_net,
        'block/coconut_lantern': W.coconut_lantern,
        'block/scarecrow': lambda: W.scarecrow(head('block/scarecrow')),
    }
    j['gui/water_purifier_gui'] = purifier_gui_frame
    j.update(M.jobs(head))
    j.update(I.jobs(head))
    return j


def preview(out_dir, flt=None):
    os.makedirs(out_dir, exist_ok=True)
    pairs = []
    for rel, fn in jobs().items():
        if flt and not any(f in rel for f in flt.split(',')):
            continue
        new = fn()
        if isinstance(new, list):
            new = new[0]
        pairs.append((rel, head(rel), new))
    cells = []
    for rel, old, new in pairs:
        if old.height > old.width:
            old = old.crop((0, 0, old.width, old.width))
        cells += [old, new]
    # old/new side by side, 8 pairs per row
    scale = 9
    cw = 16 * scale
    cols = 4
    per_page = 12
    from PIL import ImageDraw
    p = None
    for page in range(0, len(pairs), per_page):
        chunk = pairs[page:page + per_page]
        rows_ = (len(chunk) + cols - 1) // cols
        sh = Image.new('RGBA', (cols * (2 * cw + 14), rows_ * (cw + 14)), (48, 52, 60, 255))
        d = ImageDraw.Draw(sh)
        for i, (rel, old, new) in enumerate(chunk):
            x, y = (i % cols) * (2 * cw + 14), (i // cols) * (cw + 14)
            for k, im in enumerate((old, new)):
                if im.height > im.width:
                    im = im.crop((0, 0, im.width, im.width))
                big = im.resize((cw, cw * im.height // im.width), Image.NEAREST)
                sh.alpha_composite(big, (x + k * (cw + 2), y))
            d.text((x + 2, y + cw + 1), rel.split('/')[-1][:30], fill=(230, 230, 230, 255))
        p = os.path.join(out_dir, 'compare%d.png' % (page // per_page))
        sh.save(p)
    # tiling of the full-cube blocks
    tiles = [n for (rel, o, n) in pairs if rel.startswith('block/') and n.size == (16, 16) and n.getpixel((0, 0))[3]]
    if tiles:
        t = Image.new('RGBA', (len(tiles[:12]) * 52, 48), (48, 52, 60, 255))
        for i, im in enumerate(tiles[:12]):
            for a in range(3):
                for b in range(3):
                    t.alpha_composite(im, (i * 52 + a * 16, b * 16))
        t.resize((t.width * 3, t.height * 3), Image.NEAREST).save(os.path.join(out_dir, 'tiles.png'))
    return p


def main():
    if len(sys.argv) > 1 and sys.argv[1] == '--preview':
        print(preview(sys.argv[2], sys.argv[3] if len(sys.argv) > 3 else None))
        return
    for rel, fn in jobs().items():
        img = fn()
        C.save(img, rel, frametime=getattr(fn, 'frametime', None))
    print('wrote %d textures' % len(C.WRITTEN))


if __name__ == '__main__':
    main()
