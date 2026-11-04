"""Music of the raft dimension: pieces by Kevin MacLeod (incompetech.com), licensed CC BY 4.0.

Each piece is downloaded from incompetech.com (cached), cut to the part used in the game, faded, levelled (vanilla music
plays at about -23 LUFS) and written as Ogg Vorbis q3 to assets/raft/sounds/music/<key>.ogg. Wide pieces stay
stereo, the drones and quiet pieces become mono to keep the jar small (all music together stays well under 25 MB).
The credits (CREDITS_MUSIC.txt in the jar, docs/curseforge/description.html) and the "Now Playing" names come from
TRACKS as well, so they always match the files.
"""
import os
import subprocess
import urllib.parse

import numpy as np

from content.audio import dsp

LICENSE = 'Licensed under Creative Commons: By Attribution 4.0 License, http://creativecommons.org/licenses/by/4.0/'
SOURCE = 'https://incompetech.com/music/royalty-free/mp3-royaltyfree/'

# key: (title, event, start s, end s or None, channels, target LUFS, where it plays)
TRACKS = [
    ('morning', 'Morning', 'music.raft.sea_day', 0.0, None, 2, -23.0, 'the open sea by day'),
    ('clear_waters', 'Clear Waters', 'music.raft.sea_day', 0.0, None, 1, -23.0, 'the open sea by day'),
    ('with_the_sea', 'With the Sea', 'music.raft.sea_night', 0.0, None, 2, -23.0, 'the open sea at night'),
    ('decline', 'Decline', 'music.raft.sea_night', 0.0, None, 1, -23.0, 'the open sea at night'),
    ('shores_of_avalon', 'Shores of Avalon', 'music.raft.port_solace', 0.0, None, 1, -22.0, 'Port Solace, the drowned city'),
    ('port_horizon', 'Port Horizon', 'music.raft.saltmere', 24.0, 236.0, 1, -23.0, 'Saltmere, the stilt village'),
    ('shadowlands_horizon', 'Shadowlands 1 - Horizon', 'music.raft.abyss', 0.0, None, 1, -22.0, 'the Abyssal Trench'),
    ('night_vigil', 'Night Vigil', 'music.raft.sanctum', 0.0, 209.0, 1, -22.0, 'the Abyssal Sanctum'),
    ('black_vortex', 'Black Vortex', 'music.raft.leviathan', 0.0, None, 2, -19.0, 'the fight against the Leviathan'),
]


def cache_dir():
    d = os.environ.get('BC_MUSIC_CACHE') or os.path.join(os.environ.get('TMPDIR', '/tmp'), 'bc_music_cache')
    os.makedirs(d, exist_ok=True)
    return d


def fetch(title):
    """The MP3 of a piece (downloaded once into the cache)."""
    path = os.path.join(cache_dir(), title + '.mp3')
    if not os.path.isfile(path) or os.path.getsize(path) < 100000:
        url = SOURCE + urllib.parse.quote(title + '.mp3')
        subprocess.run(['curl', '-sS', '-f', '-L', '-o', path, url], check=True)
    return path


def render(key, title, start, end, channels, target):
    x = dsp.read_audio(fetch(title), 2)
    a = int(start * dsp.SR)
    b = len(x) if end is None else min(len(x), int(end * dsp.SR))
    x = x[a:b]
    # cut silence at the very end, fade in/out (a longer fade where the piece is cut in the middle)
    m = np.max(np.abs(x), axis=1)
    idx = np.nonzero(m > 10 ** (-60 / 20))[0]
    x = x[:idx[-1] + 1] if len(idx) else x
    x = dsp.fade(x, 0.5 if start > 0 else 0.01, 6.0 if end is not None else 1.0)
    if channels == 1:
        x = x.mean(axis=1)
    gain = target - dsp.lufs(x)
    y = x * 10 ** (gain / 20)
    # no more than 3 dB of peak limiting: a quiet, dynamic piece stays a bit quieter instead
    over = dsp.peak_db(y) + 1.5
    if over > 3.0:
        y = y * 10 ** (-(over - 3.0) / 20)
    y = dsp.limit(y, -1.5, release=0.2)
    return y


def generate(sound_dir):
    """Writes every track; returns {event: [sound names]} for sounds.json and the lang entries of the toast."""
    events, lang = {}, {}
    for key, title, event, start, end, channels, target, where in TRACKS:
        y = render(key, title, start, end, channels, target)
        path = os.path.join(sound_dir, 'music', key + '.ogg')
        dsp.write_ogg(y, path, quality=3)
        events.setdefault(event, []).append('raft:music/' + key)
        lang['raft.music.' + key] = 'Kevin MacLeod - ' + title
        print('music %-22s %5.1fs %s %.1f LUFS %d KB' % (key, len(y) / dsp.SR, 'stereo' if y.ndim == 2 else 'mono',
                                                        dsp.lufs(y), os.path.getsize(path) // 1024), flush=True)
    return events, lang


def credits_text():
    lines = ['Beach Craft - music credits', '',
             'The music of the raft dimension is by Kevin MacLeod (incompetech.com).', LICENSE, '']
    for key, title, event, start, end, channels, target, where in TRACKS:
        lines.append('"%s" Kevin MacLeod (incompetech.com)' % title)
        lines.append('  %s  (plays in %s; file assets/raft/sounds/music/%s.ogg%s)' % (
            LICENSE, where, key, ', shortened' if end is not None else ''))
    lines += ['', 'All sound effects of Beach Craft are synthesised for the mod (no recordings).', '']
    return '\n'.join(lines)
