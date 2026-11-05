"""Story moments: the Tidewarden altar (sigils, the key), the Tide Seal and the summoning at the Leviathan altar.

Every function returns a list of variants (mono float arrays); gen.py masters and exports them.
"""
import numpy as np

from content.audio import dsp, lib
from content.audio.dsp import SR

# D dorian-ish colours of the Tidewardens: D, F, A, C, E, G
D3, F3, A3, C4, D4, E4, F4, G4, A4, C5, D5, E5, A5 = 146.8, 174.6, 220.0, 261.6, 293.7, 329.6, 349.2, 392.0, 440.0, \
    523.3, 587.3, 659.3, 880.0


def _hall(x, rng, rt=2.6, mix=0.32):
    return dsp.reverb(x, rt60=rt, mix=mix, predelay=0.035, damp=6500, rng=rng, early=0.7)


def altar_insert():
    """A sigil set into the altar: a crystal chime with a soft swell of water-light (pitch rises in the game)."""
    out = []
    for k, root in enumerate([D5, A4]):
        rng = dsp.rng_of(8000 + k)
        dur = 2.4
        o = dsp.zeros(dur)
        for i, r in enumerate([1.0, 1.5, 2.0]):
            dsp.place(o, lib.bell(root * r, 2.0, rng, 0.6, 1.3), 0.03 * i, 0.5 / (1 + i * 0.4))
        swell = lib.pad_chord([root / 2, root * 0.75], 1.4, rng, attack=0.15, release=1.1, bright=0.35)
        dsp.place(o, swell, 0.0, 0.5)
        dsp.place(o, lib.shimmer(1.2, rng, 3000, 9000, 18) * np.linspace(1, 0, dsp.n_of(1.2)), 0.0, 0.3)
        out.append(_hall(o, rng, 2.2, 0.3))
    return out


def altar_forge():
    """All four sigils: the Tidewarden Key is forged. A rising tide of voices, a radiant strike, a long glow."""
    rng = dsp.rng_of(8100)
    dur = 6.0
    o = dsp.zeros(dur)
    rise = lib.choir([D3, A3, D4, F4], 2.2, rng, 'o', attack=1.9, release=0.3)
    dsp.place(o, rise, 0.0, 0.55)
    surge = lib.water_rush(2.2, rng, rise=2.0, hold=0.1, lo=200, hi=2400, swirl=2.0)
    dsp.place(o, surge, 0.0, 0.4)
    # the strike
    dsp.place(o, lib.boom(rng, 0.8, 2.0) * 0.6, 2.1)
    chord = lib.choir([D3, A3, D4, E4, A4], 3.6, rng, 'a', attack=0.05, release=2.6)
    dsp.place(o, chord, 2.1, 0.6)
    for i, f in enumerate([D5, A5, E5 * 2, D5 * 2]):
        dsp.place(o, lib.bell(f, 3.4, rng, 0.8, 1.6), 2.1 + 0.07 * i, 0.3)
    dsp.place(o, lib.shimmer(3.0, rng, 2500, 10000, 40) * np.linspace(1, 0, dsp.n_of(3.0)), 2.12, 0.4)
    return [_hall(o, rng, 3.2, 0.35)]


def seal_open():
    """The Tide Seal opens: ancient stone grinding apart, a deep chord of the Tidewardens and rushing water."""
    rng = dsp.rng_of(8200)
    dur = 6.0
    o = dsp.zeros(dur)
    t = dsp.times(4.0)
    grind = dsp.lowpass(dsp.crackle(4.0, 700, rng, 0.003, 0.2, 1.0), 900)
    grind = dsp.formants(grind, [(140, 2, 0), (310, 3, -3), (650, 3, -8)]) + 0.4 * grind
    grind *= dsp.env([(0, 0), (0.4, 1), (3.2, 0.8), (4.0, 0)], 4.0) * (0.7 + 0.3 * dsp.smooth_random(4.0, 5, rng, 0, 1))
    dsp.place(o, grind, 0.0, 0.8)
    rumble = dsp.lowpass(dsp.brown(4.5, rng), 80) * dsp.env([(0, 0), (0.5, 1), (4.5, 0)], 4.5)
    dsp.place(o, rumble, 0.0, 0.8)
    chord = lib.choir([D3, A3, D4, F4, A4], 4.2, rng, 'o', attack=1.4, release=1.8)
    dsp.place(o, chord, 0.6, 0.45)
    dsp.place(o, lib.pad_chord([D3 / 2, D3, A3], 4.5, rng, attack=1.2, release=2.0, bright=0.3), 0.4, 0.5)
    dsp.place(o, lib.water_rush(2.5, rng, rise=1.0, hold=0.6, lo=200, hi=2500), 2.0, 0.5)
    for i, f in enumerate([A4, D5, E5]):
        dsp.place(o, lib.bell(f, 2.5, rng, 0.5, 1.4), 2.6 + 0.12 * i, 0.2)
    return [_hall(o, rng, 3.0, 0.3)]


def seal_locked():
    """Touching the closed seal: a dull knock on stone and a low, sealed hum."""
    out = []
    for k in range(2):
        rng = dsp.rng_of(8300 + k)
        dur = 1.4
        o = dsp.zeros(dur)
        knock = dsp.modal([95 + 10 * k, 230, 410, 690], [0.12, 0.06, 0.04, 0.02], [1.0, 0.5, 0.3, 0.15], 0.4, rng)
        dsp.place(o, knock, 0.0, 1.0)
        hum = lib.pad_chord([D3 / 2, A3 / 2], 1.3, rng, attack=0.1, release=1.0, bright=0.15)
        dsp.place(o, hum, 0.02, 0.5)
        out.append(_hall(o, rng, 1.8, 0.25))
    return out


def summon():
    """The key turns in the Leviathan altar: a deep horn from the abyss, the pool heaving, a distant roar."""
    rng = dsp.rng_of(8400)
    dur = 6.5
    o = dsp.zeros(dur)
    # two horn blasts, a semitone apart (dread), like a conch blown in the deep
    for i, (st, f, d) in enumerate([(0.0, 55.0, 2.4), (2.0, 58.27, 2.8)]):
        tt = dsp.times(d)
        vib = 1 + 0.004 * np.sin(2 * np.pi * 4.2 * tt)
        horn = dsp.saw(f * vib, d, 40) + 0.5 * dsp.saw(f * 1.5 * vib * 1.003, d, 30)
        cut = np.interp(tt, [0, 0.5, d], [150, 900, 300])
        horn = dsp.svf(horn, cut, 1.2, 'lp') * dsp.env([(0, 0), (0.35, 1), (d - 0.5, 0.85), (d, 0)], d, 1.0)
        dsp.place(o, horn, st, 0.7)
    rumble = dsp.lowpass(dsp.brown(dur, rng), 70) * dsp.env([(0, 0), (2.0, 0.6), (4.5, 1), (dur, 0)], dur)
    dsp.place(o, rumble, 0.0, 0.9)
    dsp.place(o, lib.water_rush(3.0, rng, rise=1.6, hold=0.8, lo=120, hi=1500, swirl=1.2), 2.6, 0.6)
    dsp.place(o, lib.bubble_burst(2.5, rng, rate=40, lo=90, hi=500, shape=np.linspace(0.3, 1, dsp.n_of(2.5))), 3.0, 0.5)
    far = lib.roar(2.2, rng, f_lo=40, f_hi=80, size=3.0, wet=0.4, rough=0.8, attack=0.5)
    far = dsp.lowpass(far, 700)
    dsp.place(o, far, 4.1, 0.45)
    return [_hall(o, rng, 3.5, 0.32)]
