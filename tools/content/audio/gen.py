"""Generates the sounds and music of Beach Craft into src/main/modules/audio/ (module "audio").

Run: cd tools && python3 -m content.audio.gen [group ...]     (needs numpy, scipy and ffmpeg with libvorbis)

* Sound effects are synthesised (oscillators, noise, filters; no recordings) by the group modules (boss, creatures,
  blocks, items, story, ambience) and written as mono Ogg Vorbis to assets/raft/sounds/sfx/<name><n>.ogg.
* Music is made by music.py from Kevin MacLeod pieces (incompetech.com, CC BY 4.0): downloaded, cut, levelled and
  written to assets/raft/sounds/music/<name>.ogg (see MUSIC_CREDITS.txt / docs/curseforge/description.html).
* The sound events of new sounds go to the audio module's sounds.json and lang; events that already existed in other
  modules (boss, shore, diving) keep their definition there and point at the files made here.
"""
import json
import os
import sys
from collections import OrderedDict

if __package__ in (None, ''):
    sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..')))
    __package__ = 'content.audio'

from bclib.paths import MODULES, out  # noqa: E402
import numpy as np  # noqa: E402

from content.audio import ambience, blocks, boss, creatures, dsp, events, items, story  # noqa: E402
from content.audio.events import NEW_EVENTS, RETARGET, _entries, sfx_files, sound_dir  # noqa: E402,F401

M = 'audio'


class Spec:
    """One sound file family: sfx/<name>1..n.ogg, made by fn, levelled to lufs. A loop is neither trimmed nor faded."""

    def __init__(self, name, fn, lufs=-18.0, ceiling=-1.5, numbered=True, quality=4, fout=0.03, loop=False, comp=False,
                 max_gr=5.0):
        self.name, self.fn, self.lufs, self.ceiling, self.numbered, self.quality, self.fout, self.loop = \
            name, fn, lufs, ceiling, numbered, quality, fout, loop
        self.comp, self.max_gr = comp, max_gr

    def render(self):
        """Masters every variant: impacts (comp) are compressed first; if the limiter would still have to take off
        more than max_gr dB of the peaks, the variant is made quieter instead (transients stay intact)."""
        res = []
        self.reduction = []
        for x in self.fn():
            if self.comp:
                x = x / (np.max(np.abs(x)) + 1e-12)
                x = dsp.compress(x, -16.0, 3.0, 0.002, 0.15)
            kw = dict(fin=0.0, fout=0.0) if self.loop else dict(fout=self.fout)
            if not self.loop:
                x = dsp.trim(x, -66.0, 0.04)
            y = dsp.master(x, self.lufs, self.ceiling, **kw)
            gr = dsp.LAST_GAIN_REDUCTION[0]
            if gr > self.max_gr:
                y = dsp.master(x, self.lufs - (gr - self.max_gr), self.ceiling, **kw)
                gr = dsp.LAST_GAIN_REDUCTION[0]
            res.append(y)
            self.reduction.append(round(gr, 1))
        return res

    def files(self, count):
        return ['sfx/%s%d' % (self.name, i) for i in range(1, count + 1)] if self.numbered or count > 1 \
            else ['sfx/' + self.name]


GROUPS = OrderedDict()
GROUPS['boss'] = [
    Spec('leviathan/roar', boss.roar, -13.0),
    Spec('leviathan/ambient', boss.ambient, -18.0),
    Spec('leviathan/hurt', boss.hurt, -15.0),
    Spec('leviathan/death', boss.death, -14.0),
    Spec('leviathan/charge', boss.charge, -17.0),
    Spec('leviathan/spit', boss.spit, -16.0),
    Spec('leviathan/emerge', boss.emerge, -14.0, comp=True),
    Spec('leviathan/dive', boss.dive, -14.0, comp=True),
    Spec('leviathan/slam', boss.slam, -13.0, comp=True),
    Spec('leviathan/flop', boss.flop, -16.0, comp=True),
    Spec('leviathan/whirlpool', boss.whirlpool, -18.0),
    Spec('leviathan/deflect', boss.deflect, -17.0, comp=True),
    Spec('leviathan/bite', boss.bite, -17.0, comp=True),
    Spec('leviathan/burst', boss.burst, -14.0),
    Spec('tideling/ambient', boss.tideling_ambient, -22.0),
    Spec('tideling/hurt', boss.tideling_hurt, -20.0),
    Spec('tideling/death', boss.tideling_death, -20.0),
    Spec('brine_bolt/hit', boss.brine_hit, -18.0, comp=True),
]
GROUPS['creatures'] = [
    Spec('shark/ambient', creatures.shark_ambient, -24.0),
    Spec('shark/hurt', creatures.shark_hurt, -18.0),
    Spec('shark/death', creatures.shark_death, -18.0),
    Spec('shark/bite', creatures.shark_bite, -19.0),
    Spec('shark/gnaw', creatures.shark_gnaw, -18.0, comp=True, max_gr=9.0),
    Spec('shark/raft_break', creatures.raft_break, -16.0, comp=True, max_gr=9.0),
    Spec('whale/hurt', creatures.whale_hurt, -17.0),
    Spec('whale/death', creatures.whale_death, -16.0),
    Spec('manta_ray/hurt', creatures.manta_hurt, -20.0),
    Spec('manta_ray/death', creatures.manta_death, -19.0),
    Spec('jellyfish/sting', creatures.jellyfish_sting, -20.0),
    Spec('jellyfish/hurt', creatures.jellyfish_hurt, -22.0),
    Spec('jellyfish/death', creatures.jellyfish_death, -21.0),
    Spec('anglerfish/lure', creatures.anglerfish_lure, -27.0),
    Spec('anglerfish/bite', creatures.anglerfish_bite, -18.0),
    Spec('anglerfish/hurt', creatures.anglerfish_hurt, -19.0),
    Spec('anglerfish/death', creatures.anglerfish_death, -19.0),
    Spec('moray_eel/bite', creatures.moray_bite, -18.0),
    Spec('crab/ambient', creatures.crab_ambient, -25.0),
    Spec('crab/pinch', creatures.crab_pinch, -20.0),
    Spec('ember_crab/ambient', creatures.ember_crab_ambient, -26.0),
    Spec('seagull/call', creatures.seagull_ambient, -23.0),
    Spec('seagull/hurt', creatures.seagull_hurt, -19.0),
    Spec('seagull/death', creatures.seagull_death, -19.0),
    Spec('seagull/steal', creatures.seagull_steal, -18.0),
]
GROUPS['blocks'] = [
    Spec('water_purifier/boil', blocks.purifier_boil, -27.0),
    Spec('grill/sizzle', blocks.grill_sizzle, -28.0),
    Spec('air_compressor/crank', blocks.compressor_crank, -21.0),
    Spec('air_compressor/fill', blocks.compressor_fill, -26.0),
    Spec('wind_turbine/loop', blocks.turbine_loop, -27.0, numbered=False, loop=True),
    Spec('ship_cannon/fire', blocks.cannon_fire, -12.0, comp=True),
    Spec('research_terminal/use', blocks.terminal_use, -23.0),
    Spec('control_panel/beep', blocks.panel_beep, -25.0),
    Spec('hammock/creak', blocks.hammock_creak, -27.0),
]
GROUPS['items'] = [
    Spec('harpoon_gun/fire', items.harpoon_fire, -17.0, comp=True),
    Spec('harpoon_gun/reload', items.harpoon_reload, -21.0),
    Spec('sea_scooter/run', items.scooter_run, -22.0),
    Spec('plastic_hook/reel', items.hook_reel, -24.0),
    Spec('oxygen_tank/low', items.tank_low, -24.0),
    Spec('oxygen_tank/refill', items.tank_refill, -23.0),
]
GROUPS['story'] = [
    Spec('tidewarden_altar/insert', story.altar_insert, -19.0),
    Spec('tidewarden_altar/forge', story.altar_forge, -16.0),
    Spec('tide_seal/open', story.seal_open, -16.0),
    Spec('tide_seal/locked', story.seal_locked, -21.0),
    Spec('leviathan_altar/summon', story.summon, -15.0),
]
GROUPS['ambience'] = [
    Spec('ambient/sea_loop', ambience.sea_loop, -35.0, numbered=False, quality=3, loop=True),
    Spec('ambient/gulls_far', ambience.gulls_far, -30.0),
    Spec('ambient/deep', ambience.deep_mood, -27.0),
]


def export(groups):
    for g in groups:
        for spec in GROUPS[g]:
            variants = spec.render()
            files = spec.files(len(variants))
            # drop old files of this name (a sound may have fewer variants now)
            for old in sfx_files(spec.name):
                os.remove(os.path.join(sound_dir(), old + '.ogg'))
            for x, f in zip(variants, files):
                dsp.write_ogg(x, os.path.join(sound_dir(), f + '.ogg'), spec.quality)
            print('%-30s %d file(s), limiter %s' % (spec.name, len(files), spec.reduction), flush=True)


def write_events(music_events, music_lang):
    from bclib import assets
    sounds = OrderedDict()
    lang = OrderedDict()
    for event, (names, subtitle, extra) in NEW_EVENTS.items():
        entries = []
        for n in names:
            entries += _entries(n, extra, stream=n.startswith('ambient/sea'))
        e = OrderedDict(sounds=entries)
        if subtitle:
            e['subtitle'] = 'subtitles.raft.' + event
            lang['subtitles.raft.' + event] = subtitle
        sounds[event] = e
    for event, names in music_events.items():
        sounds[event] = OrderedDict(sounds=[OrderedDict(name=n, stream=True) for n in names])
    lang.update(music_lang)
    assets.write_json(M, 'assets/raft/sounds.json', sounds)
    assets.write_json(M, 'assets/raft/lang/en_us.json', OrderedDict(sorted(lang.items())))
    # existing events of other modules: only their sound lists change
    for module, retarget in RETARGET.items():
        path = os.path.join(MODULES, module, 'assets', 'raft', 'sounds.json')
        with open(path, encoding='utf-8') as f:
            data = json.load(f, object_pairs_hook=OrderedDict)
        missing = [e for e in retarget if e not in data]
        if missing:
            raise SystemExit('%s has no event %s' % (path, missing))
        assets.write_json(module, 'assets/raft/sounds.json', events.apply(module, data))


def verify():
    """Every raft:sfx / raft:music file named in any sounds.json exists, and every exported file is used."""
    used = set()
    for module in sorted(os.listdir(MODULES)):
        path = os.path.join(MODULES, module, 'assets', 'raft', 'sounds.json')
        if not os.path.isfile(path):
            continue
        with open(path, encoding='utf-8') as f:
            for event, d in json.load(f).items():
                for snd in d['sounds']:
                    n = snd if isinstance(snd, str) else snd['name']
                    if n.startswith('raft:sfx/') or n.startswith('raft:music/'):
                        used.add(n[5:])
    have = set()
    for root, _, files in os.walk(sound_dir()):
        for f in files:
            if f.endswith('.ogg'):
                have.add(os.path.relpath(os.path.join(root, f), sound_dir())[:-4].replace(os.sep, '/'))
    missing, unused = sorted(used - have), sorted(have - used)
    total = sum(os.path.getsize(os.path.join(r, f)) for r, _, fs in os.walk(sound_dir()) for f in fs)
    music = sum(os.path.getsize(os.path.join(sound_dir(), 'music', f)) for f in os.listdir(os.path.join(sound_dir(), 'music'))) \
        if os.path.isdir(os.path.join(sound_dir(), 'music')) else 0
    print('sounds: %d files, %.1f MB (music %.1f MB); missing %s; unused %s' % (
        len(have), total / 1e6, music / 1e6, missing or 'none', unused or 'none'))
    if missing or unused:
        raise SystemExit(1)


def write_description(credits):
    """Puts the music credits between the markers in docs/curseforge/description.html."""
    path = os.path.join(MODULES, '..', '..', '..', 'docs', 'curseforge', 'description.html')
    with open(path, encoding='utf-8') as f:
        html = f.read()
    a = '<!-- music credits: written by tools/content/audio/gen.py from tools/content/audio/music.py -->'
    b = '<!-- /music credits -->'
    i, j = html.index(a) + len(a), html.index(b)
    html = html[:i] + '\n' + credits + '\n' + html[j:]
    with open(path, 'w', encoding='utf-8') as f:
        f.write(html)


def main():
    from content.audio import music
    args = sys.argv[1:]
    groups = [a for a in args if a in GROUPS] or ([] if 'events' in args else list(GROUPS))
    export(groups)
    if 'music' in args or not args:
        music_events, music_lang = music.generate(sound_dir())
    else:
        # keep the music entries as they are
        music_events, music_lang = OrderedDict(), OrderedDict()
        for key, title, event, *_ in music.TRACKS:
            music_events.setdefault(event, []).append('raft:music/' + key)
            music_lang['raft.music.' + key] = 'Kevin MacLeod - ' + title
    write_events(music_events, music_lang)
    with open(out(M, 'CREDITS_MUSIC.txt'), 'w', encoding='utf-8') as f:
        f.write(music.credits_text())
    write_description(music.credits_html())
    verify()
    print('audio: done')


if __name__ == '__main__':
    main()
