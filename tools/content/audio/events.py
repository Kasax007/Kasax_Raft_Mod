"""Sound event tables of the audio module (no numpy needed: the boss, shore and diving generators import this too).

NEW_EVENTS are defined in the audio module's sounds.json. RETARGET lists events that other modules define (with their
subtitles and Java constants); apply() swaps their vanilla placeholder sounds for the synthesised files, so the
generators of those modules and content.audio.gen write the same sound lists.
"""
import os
from collections import OrderedDict

from bclib.paths import MODULES

M = 'audio'


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
    ('entity.manta_ray.ambient', (['manta_ray/glide'], 'Manta ray glides', {})),
    ('entity.manta_ray.swim', (['manta_ray/swim'], None, {})),
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
    ('block.raft_motor.run', (['raft_motor/run'], 'Motor runs', {'attenuation_distance': 32})),
    ('block.raft_motor.start', (['raft_motor/start'], 'Motor starts', {'attenuation_distance': 24})),
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




def apply(module, sounds):
    """sounds: a module's sounds.json content (event -> definition); the retargeted events get the synthesised files."""
    for event, (name, extra) in RETARGET.get(module, {}).items():
        if event in sounds:
            entries = _entries(name, extra)
            d = sounds[event]
            if isinstance(d, dict):
                d['sounds'] = entries
            else:
                sounds[event] = (entries,) + tuple(d[1:])
    return sounds
