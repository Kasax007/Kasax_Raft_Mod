"""Renders sound groups to a folder with a spectrogram contact sheet, for checking them by eye and ear.

Run: cd tools && python3 -m content.audio.preview <group> [<out dir>]   (groups: see gen.GROUPS)
"""
import os
import sys
import time

if __package__ in (None, ''):
    sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..')))
    __package__ = 'content.audio'

from content.audio import dsp, gen  # noqa: E402


def main():
    group = sys.argv[1]
    out = sys.argv[2] if len(sys.argv) > 2 else os.path.join(os.environ.get('TMPDIR', '/tmp'), 'bc_audio_preview')
    os.makedirs(out, exist_ok=True)
    items = []
    for spec in gen.GROUPS[group]:
        t0 = time.time()
        variants = spec.render()
        for i, x in enumerate(variants, start=1):
            name = '%s%d' % (spec.name, i) if len(variants) > 1 or spec.numbered else spec.name
            dsp.write_wav(x, os.path.join(out, name.replace('/', '_') + '.wav'))
            items.append((name, x))
        print('%-28s %d variant(s) %.1fs  limiter dB %s  clicks %s' % (spec.name, len(variants), time.time() - t0, spec.reduction,
              [dsp.clicks(v)[:4] for v in variants]), flush=True)
    print(dsp.contact_sheet(items, os.path.join(out, group + '.png')))


if __name__ == '__main__':
    main()
