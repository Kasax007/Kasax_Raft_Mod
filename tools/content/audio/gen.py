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

from content.audio import ambience, blocks, boss, creatures, dsp, items, story  # noqa: E402

M = 'audio'


class Spec:
    """One sound file family: sfx/<name>1..n.ogg, made by fn, levelled to lufs. A loop is neither trimmed nor faded."""

    def __init__(self, name, fn, lufs=-18.0, ceiling=-1.0, numbered=True, quality=4, fout=0.03, loop=False, comp=False,
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
    Spec('shark/gnaw', creatures.shark_gnaw, -18.0, comp=True),
    Spec('shark/raft_break', creatures.raft_break, -16.0, comp=True),
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
    Spec('seagull/call', creatures.seagull_ambient, -20.0),
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
    Spec('ambient/sea_loop', ambience.sea_loop, -31.0, numbered=False, quality=3, loop=True),
    Spec('ambient/gulls_far', ambience.gulls_far, -30.0),
    Spec('ambient/deep', ambience.deep_mood, -27.0),
]


# ------------------------------------------------------------------------------------------------ sound events
# New events of the audio module: event -> (sfx names, subtitle or None, extra fields of every sound entry)
NEW_EVENTS = OrderedDict([
    ('entity.shark.ambient', (['shark/ambient'], 'Shark prowls', {})),
    ('entity.shark.hurt', (['shark/hurt'], 'Shark hurts', {})),
    ('entity.shark.death', (['shark/death'], 'Shark dies', {})),
    ('entity.shark.bite', (['shark/bite'], 'Shark bites', {})),
    ('entity.shark.gnaw', (['shark/gnaw'], 'Shark gnaws at the raft', {})),
    ('entity.shark.raft_break', (['shark/raft_break'], 'Raft plank breaks', {})),
    ('entity.whale.hurt', (['whale/hurt'], 'Whale groans', {'attenuation_distance': 32})),
    ('entity.whale.death', (['whale/death'], 'Whale dies', {'attenuation_distance': 48})),
    ('entity.manta_ray.hurt', (['manta_ray/hurt'], 'Manta ray hurts', {})),
    ('entity.manta_ray.death', (['manta_ray/death'], 'Manta ray dies', {})),
    ('entity.jellyfish.sting', (['jellyfish/sting'], 'Jellyfish stings', {})),
    ('entity.jellyfish.hurt', (['jellyfish/hurt'], 'Jellyfish squelches', {})),
    ('entity.jellyfish.death', (['jellyfish/death'], 'Jellyfish dies', {})),
    ('entity.anglerfish.ambient', (['anglerfish/lure'], 'Anglerfish lure hums', {'attenuation_distance': 12})),
    ('entity.anglerfish.bite', (['anglerfish/bite'], 'Anglerfish bites', {})),
    ('entity.anglerfish.hurt', (['anglerfish/hurt'], 'Anglerfish hurts', {})),
    ('entity.anglerfish.death', (['anglerfish/death'], 'Anglerfish dies', {})),
    ('entity.moray_eel.bite', (['moray_eel/bite'], 'Moray eel lunges', {})),
    ('block.water_purifier.boil', (['water_purifier/boil'], 'Water boils', {'attenuation_distance': 10})),
    ('block.grill.sizzle', (['grill/sizzle'], 'Food sizzles', {'attenuation_distance': 10})),
    ('block.wind_turbine.spin', (['wind_turbine/loop'], None, {'attenuation_distance': 16})),
    ('block.ship_cannon.fire', (['ship_cannon/fire'], 'Cannon fires', {'attenuation_distance': 64})),
    ('block.research_terminal.use', (['research_terminal/use'], 'Terminal beeps', {})),
    ('block.control_panel.beep', (['control_panel/beep'], 'Panel beeps', {})),
    ('block.hammock.creak', (['hammock/creak'], 'Hammock creaks', {})),
    ('block.tidewarden_altar.insert', (['tidewarden_altar/insert'], 'Sigil resonates', {'attenuation_distance': 24})),
    ('block.tidewarden_altar.forge', (['tidewarden_altar/forge'], 'Tidewarden Key is forged', {'attenuation_distance': 32})),
    ('block.tide_seal.open', (['tide_seal/open'], 'Tide Seal opens', {'attenuation_distance': 48})),
    ('block.tide_seal.locked', (['tide_seal/locked'], 'Tide Seal hums', {})),
    ('block.leviathan_altar.summon', (['leviathan_altar/summon'], 'Something stirs in the abyss', {'attenuation_distance': 64})),
    ('item.plastic_hook.reel', (['plastic_hook/reel'], 'Hook reels in', {})),
    ('ambient.raft.sea', (['ambient/sea_loop'], None, {})),
    ('ambient.raft.gulls', (['ambient/gulls_far'], 'Seagulls call in the distance', {})),
    ('ambient.raft.deep', (['ambient/deep'], 'Eerie noise from the deep', {})),
])

# Events that already existed in other modules (definition, subtitle and Java constant stay there); their sounds are
# replaced by the synthesised files: module -> event -> (sfx name, extra fields of every sound entry)
RETARGET = OrderedDict([
    ('boss', OrderedDict([
        ('entity.leviathan.ambient', ('leviathan/ambient', {'attenuation_distance': 48})),
        ('entity.leviathan.hurt', ('leviathan/hurt', {'attenuation_distance': 40})),
        ('entity.leviathan.death', ('leviathan/death', {'attenuation_distance': 64})),
        ('entity.leviathan.roar', ('leviathan/roar', {'attenuation_distance': 64})),
        ('entity.leviathan.charge', ('leviathan/charge', {'attenuation_distance': 40})),
        ('entity.leviathan.spit', ('leviathan/spit', {'attenuation_distance': 40})),
        ('entity.leviathan.emerge', ('leviathan/emerge', {'attenuation_distance': 48})),
        ('entity.leviathan.dive', ('leviathan/dive', {'attenuation_distance': 48})),
        ('entity.leviathan.slam', ('leviathan/slam', {'attenuation_distance': 48})),
        ('entity.leviathan.flop', ('leviathan/flop', {'attenuation_distance': 32})),
        ('entity.leviathan.whirlpool', ('leviathan/whirlpool', {'attenuation_distance': 48})),
        ('entity.leviathan.deflect', ('leviathan/deflect', {'attenuation_distance': 24})),
        ('entity.leviathan.bite', ('leviathan/bite', {'attenuation_distance': 24})),
        ('entity.leviathan.burst', ('leviathan/burst', {'attenuation_distance': 64})),
        ('entity.tideling.ambient', ('tideling/ambient', {})),
        ('entity.tideling.hurt', ('tideling/hurt', {})),
        ('entity.tideling.death', ('tideling/death', {})),
        ('entity.brine_bolt.hit', ('brine_bolt/hit', {})),
    ])),
    ('shore', OrderedDict([
        ('entity.seagull.ambient', ('seagull/call', {})),
        ('entity.seagull.hurt', ('seagull/hurt', {})),
        ('entity.seagull.death', ('seagull/death', {})),
        ('entity.seagull.steal', ('seagull/steal', {})),
        ('entity.crab.ambient', ('crab/ambient', {})),
        ('entity.crab.pinch', ('crab/pinch', {})),
        ('entity.ember_crab.ambient', ('ember_crab/ambient', {})),
    ])),
    ('diving', OrderedDict([
        ('item.harpoon_gun.fire', ('harpoon_gun/fire', {})),
        ('item.harpoon_gun.reload', ('harpoon_gun/reload', {})),
        ('block.air_compressor.crank', ('air_compressor/crank', {})),
        ('block.air_compressor.fill', ('air_compressor/fill', {})),
        ('item.sea_scooter.run', ('sea_scooter/run', {})),
        ('item.oxygen_tank.low', ('oxygen_tank/low', {})),
        ('item.oxygen_tank.refill', ('oxygen_tank/refill', {})),
    ])),
])


def sound_dir():
    return os.path.join(MODULES, M, 'assets', 'raft', 'sounds')


def sfx_files(name):
    """The exported files of an sfx name (sorted 1..n), as sound names without the namespace."""
    d = os.path.join(sound_dir(), 'sfx', os.path.dirname(name))
    base = os.path.basename(name)
    if not os.path.isdir(d):
        return []
    names = [f[:-4] for f in os.listdir(d) if f.endswith('.ogg') and (f[:-4] == base or (
        f[:-4].startswith(base) and f[len(base):-4].isdigit()))]
    names.sort(key=lambda f: int(f[len(base):]) if f[len(base):].isdigit() else 0)
    return ['sfx/%s/%s' % (os.path.dirname(name), f) for f in names]


def _entries(name, extra, stream=False):
    files = sfx_files(name)
    if not files:
        raise SystemExit('no files for ' + name)
    res = []
    for f in files:
        e = OrderedDict(name='raft:' + f)
        if stream:
            e['stream'] = True
        e.update(extra)
        res.append(e)
    return res


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
    for module, events in RETARGET.items():
        path = os.path.join(MODULES, module, 'assets', 'raft', 'sounds.json')
        with open(path, encoding='utf-8') as f:
            data = json.load(f, object_pairs_hook=OrderedDict)
        for event, (name, extra) in events.items():
            if event not in data:
                raise SystemExit('%s has no event %s' % (path, event))
            data[event]['sounds'] = _entries(name, extra)
        assets.write_json(module, 'assets/raft/sounds.json', data)


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


def main():
    from content.audio import music
    args = sys.argv[1:]
    groups = [a for a in args if a in GROUPS] or ([] if 'events' in args else list(GROUPS))
    export(groups)
    if 'music' in args or not args:
        music_events, music_lang = music.generate(sound_dir())
        with open(out(M, 'CREDITS_MUSIC.txt'), 'w', encoding='utf-8') as f:
            f.write(music.credits_text())
    else:
        # keep the music entries as they are
        music_events, music_lang = OrderedDict(), OrderedDict()
        for key, title, event, *_ in music.TRACKS:
            music_events.setdefault(event, []).append('raft:music/' + key)
            music_lang['raft.music.' + key] = 'Kevin MacLeod - ' + title
    write_events(music_events, music_lang)
    verify()
    print('audio: done')


if __name__ == '__main__':
    main()
