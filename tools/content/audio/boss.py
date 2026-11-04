"""The Leviathan (a colossal sea serpent), its tidelings and the Tidebreaker: roars, groans, splashes and magic.

Every function returns a list of variants (mono float arrays); gen.py masters and exports them.
"""
import numpy as np

from content.audio import dsp, lib
from content.audio.dsp import SR


def _big_space(x, rng, rt=1.8, mix=0.22):
    # the sanctum dome: a large stony room, the tail stays short enough for a game sound
    return dsp.reverb(x, rt60=rt, mix=mix, predelay=0.03, damp=3500, rng=rng, early=0.8)


def roar():
    out = []
    for k, (dur, lo, hi, att) in enumerate([(3.0, 42, 92, 0.35), (2.6, 48, 104, 0.25), (3.4, 38, 84, 0.45)]):
        rng = dsp.rng_of(1000 + k)
        y = lib.roar(dur, rng, f_lo=lo, f_hi=hi, size=2.6, wet=0.6, rough=0.85, attack=att)
        # a throat full of sea water: a burst of large bubbles as the roar starts
        dsp.place(y, lib.bubble_burst(0.9, rng, rate=60, lo=90, hi=600) * 0.5, 0.05)
        out.append(_big_space(y, rng))
    return out


def ambient():
    """Deep idle rumbles: a slow growl through closed jaws, sometimes a whale-like moan."""
    out = []
    for k in range(3):
        rng = dsp.rng_of(1100 + k)
        dur = 2.6 + 0.4 * k
        if k == 2:
            y = lib.moan(dur, 70, 52, rng, shape=1.3, vib=0.015, growl=0.45, harm=(1, 0.7, 0.45, 0.25, 0.12))
            y = dsp.lowpass(y, 1400)
        else:
            f = dsp.env([(0, 34 + 4 * k), (dur * 0.4, 44 + 3 * k), (dur, 31)], dur)
            y = lib.growl_voice(f, dur, rng, size=3.5, rough=0.9, breath=0.35,
                                vowel=[(300, 3, 0), (520, 4, -4), (900, 5, -10)], tilt=0.6)
            y = y * dsp.env([(0, 0), (0.6, 1), (dur - 0.8, 0.8), (dur, 0)], dur, 1.0)
            y = dsp.lowpass(y, 1600)
        y = y + 0.35 * dsp.lowpass(lib.bubble_burst(dur, rng, rate=18, lo=70, hi=300,
                                                     shape=np.full(dsp.n_of(dur), 0.7)), 700)
        out.append(dsp.underwater(y, 1600, rng))
    return out


def hurt():
    out = []
    for k in range(3):
        rng = dsp.rng_of(1200 + k)
        dur = 1.1 + 0.15 * k
        hi = 120 + 15 * k
        y = lib.roar(dur, rng, size=2.2, wet=0.4, rough=0.95, attack=0.08,
                     shape=[(0, hi * 0.7), (0.08, hi), (0.35, hi * 0.95), (dur, hi * 0.45)])
        dsp.place(y, lib.thrash(0.7, rng, 0.8), 0.0, 0.35)
        out.append(_big_space(y, rng, 1.2, 0.18))
    return out


def death():
    rng = dsp.rng_of(1300)
    dur = 5.5
    # a long falling roar that breaks into gurgles and a last, deep moan
    y = lib.roar(3.2, rng, size=2.6, wet=0.8, rough=1.0, attack=0.4,
                 shape=[(0, 60), (0.4, 105), (1.4, 92), (3.2, 34)])
    out = dsp.zeros(dur)
    dsp.place(out, y, 0.0)
    dsp.place(out, lib.moan(2.8, 62, 30, rng, shape=0.8, vib=0.01, growl=0.6, harm=(1, 0.6, 0.35, 0.2)) * 0.6, 2.4)
    dsp.place(out, lib.bubble_burst(3.0, rng, rate=70, lo=60, hi=500, shape=np.linspace(1, 0, dsp.n_of(3.0))) * 0.6, 2.3)
    return [_big_space(out, rng, 2.4, 0.3)]


def charge():
    """Gathering brine before the spit: water sucked in, a rising churn and a crackling glow."""
    out = []
    for k in range(2):
        rng = dsp.rng_of(1400 + k)
        dur = 1.8
        t = dsp.times(dur)
        suck = lib.water_rush(dur, rng, rise=1.4, hold=0.2, lo=150, hi=1800, swirl=6 + 3 * k)
        rise = np.interp(t, [0, dur], [70, 210])
        churn = lib.growl_voice(rise, dur, rng, size=2.0, rough=0.7, breath=0.4,
                                vowel=[(500, 3, 0), (900, 4, -4)], tilt=0.5) * dsp.env([(0, 0), (dur * 0.9, 1), (dur, 0.2)], dur, 1.6)
        glow = lib.shimmer(dur, rng, 1500, 6000, 25) * np.interp(t, [0, dur], [0.1, 1.0])
        y = suck * 0.9 + churn * 0.5 + glow * 0.4
        out.append(dsp.reverb(y, 1.0, 0.15, rng=rng))
    return out


def spit():
    """A ball of brine launched from the jaws: a throaty heave, a wet burst and its whoosh."""
    out = []
    for k in range(3):
        rng = dsp.rng_of(1500 + k)
        dur = 0.9
        o = dsp.zeros(dur)
        heave = lib.growl_voice(np.interp(dsp.times(0.3), [0, 0.3], [90, 60]), 0.3, rng, size=2.0, rough=0.8, breath=0.6)
        dsp.place(o, heave * dsp.env([(0, 0), (0.05, 1), (0.3, 0)], 0.3), 0.0, 0.6)
        burst = dsp.bandpass(dsp.white(0.25, rng), 300, 3500) * dsp.decay_env(0.25, 0.05, 0.002)
        dsp.place(o, burst, 0.12, 0.9)
        dsp.place(o, lib.bubble_burst(0.4, rng, rate=90, lo=150, hi=900), 0.12, 0.6)
        whoosh = lib.water_rush(0.6, rng, rise=0.05, hold=0.1, lo=400, hi=2500)
        dsp.place(o, whoosh, 0.18, 0.6)
        out.append(o)
    return out


def emerge():
    """The serpent bursts out of the pool: rising rumble, an eruption of water, a curtain falling back."""
    out = []
    for k in range(2):
        rng = dsp.rng_of(1600 + k)
        dur = 3.2
        o = dsp.zeros(dur)
        rumble = dsp.lowpass(dsp.brown(1.0, rng), 120) * dsp.env([(0, 0), (0.9, 1), (1.0, 0.6)], 1.0, 1.5)
        dsp.place(o, rumble, 0.0, 0.8)
        dsp.place(o, lib.water_rush(1.2, rng, rise=0.6, hold=0.3, lo=120, hi=1200), 0.0, 0.7)
        dsp.place(o, lib.splash(2.6, rng, 2.4), 0.75, 1.0)
        # the water curtain falling back down
        fall = lib.splash(1.2, rng, 1.6, bright=1.2)
        dsp.place(o, fall, 1.4, 0.5)
        rain = dsp.highpass(dsp.crackle(1.6, 900, rng, 0.0008), 1500) * dsp.env([(0, 0), (0.1, 1), (1.6, 0)], 1.6)
        dsp.place(o, rain, 1.2, 0.4)
        out.append(_big_space(o, rng, 1.6, 0.18))
    return out


def dive():
    """It plunges back: a heavy splash, the gulp of displaced water and a trail of bubbles."""
    out = []
    for k in range(2):
        rng = dsp.rng_of(1700 + k)
        dur = 2.6
        o = dsp.zeros(dur)
        dsp.place(o, lib.splash(2.2, rng, 2.0), 0.0, 1.0)
        gulp = dsp.sine(np.interp(dsp.times(0.6), [0, 0.6], [95, 38]), 0.6) * dsp.env([(0, 0), (0.05, 1), (0.6, 0)], 0.6)
        dsp.place(o, gulp, 0.18, 0.8)
        dsp.place(o, dsp.underwater(lib.bubble_burst(1.8, rng, rate=60, lo=90, hi=700), 1500, rng), 0.4, 0.6)
        out.append(_big_space(o, rng, 1.4, 0.15))
    return out


def slam():
    """The body crashes onto the arena floor: a stone-shaking boom, a crack and a wet slap."""
    out = []
    for k in range(2):
        rng = dsp.rng_of(1800 + k)
        dur = 2.2
        o = dsp.zeros(dur)
        dsp.place(o, lib.boom(rng, 1.3, dur), 0.0, 1.0)
        stone = dsp.modal(rng.uniform(150, 1400, 8), rng.uniform(0.03, 0.12, 8), rng.uniform(0.3, 1.0, 8), 0.5, rng)
        dsp.place(o, stone, 0.0, 0.6)
        dsp.place(o, lib.crunch(0.5, rng, 300, 0.7), 0.02, 0.4)
        dsp.place(o, lib.splash(1.0, rng, 1.0), 0.03, 0.6)
        out.append(_big_space(o, rng, 1.8, 0.2))
    return out


def flop():
    """Stranded on the floor: a heavy wet body slapping the stone."""
    out = []
    for k in range(3):
        rng = dsp.rng_of(1900 + k)
        dur = 0.9
        o = dsp.zeros(dur)
        slap = dsp.bandpass(dsp.white(0.08, rng), 250, 4000) * dsp.decay_env(0.08, 0.015, 0.0005)
        dsp.place(o, slap, 0.0, 1.0)
        thud = dsp.sine(np.interp(dsp.times(0.35), [0, 0.35], [90, 45]), 0.35) * dsp.decay_env(0.35, 0.08, 0.002)
        dsp.place(o, thud, 0.0, 1.0)
        dsp.place(o, lib.splash(0.5, rng, 0.6), 0.01, 0.5)
        out.append(_big_space(o, rng, 1.0, 0.12))
    return out


def whirlpool():
    """Water spinning around the arena: a turning roar of water with gurgles."""
    out = []
    for k in range(2):
        rng = dsp.rng_of(2000 + k)
        dur = 3.0
        y = lib.water_rush(dur, rng, rise=0.6, hold=1.6, lo=120, hi=1600, swirl=1.6 + 0.4 * k)
        y += 0.5 * dsp.lowpass(lib.bubble_burst(dur, rng, rate=35, lo=80, hi=500,
                                                shape=np.full(dsp.n_of(dur), 0.8)), 900)
        low = dsp.lowpass(dsp.brown(dur, rng), 90) * dsp.env([(0, 0), (0.6, 1), (2.2, 1), (3.0, 0)], dur)
        out.append(_big_space(y + 0.5 * low, rng, 1.4, 0.15))
    return out


def deflect():
    """A blow glances off the armoured scales: a hard, glassy-metallic clang."""
    out = []
    for k in range(3):
        rng = dsp.rng_of(2100 + k)
        dur = 0.8
        base = 420 + 60 * k
        modes = [base * r for r in (1.0, 1.47, 2.09, 2.56, 3.39, 4.12)]
        y = dsp.modal(modes, [0.25, 0.18, 0.12, 0.09, 0.06, 0.04], [1.0, 0.7, 0.5, 0.45, 0.3, 0.2], dur, rng, 0.0003)
        tick = dsp.highpass(dsp.white(0.02, rng), 2000) * dsp.decay_env(0.02, 0.003, 0.0002)
        dsp.place(y, tick, 0.0, 0.8)
        out.append(_big_space(y, rng, 1.0, 0.15))
    return out


def bite():
    """The jaws slam shut: a giant snap with a crunch and a deep thud."""
    out = []
    for k in range(2):
        rng = dsp.rng_of(2200 + k)
        dur = 0.9
        o = dsp.zeros(dur)
        dsp.place(o, lib.jaw_snap(rng, 2.2, 0.6), 0.0, 1.0)
        dsp.place(o, lib.crunch(0.25, rng, 250, 0.7), 0.01, 0.5)
        growl = lib.growl_voice(np.interp(dsp.times(0.5), [0, 0.5], [70, 50]), 0.5, rng, size=2.5, rough=0.9, breath=0.4)
        dsp.place(o, growl * dsp.env([(0, 0), (0.03, 1), (0.5, 0)], 0.5), 0.0, 0.4)
        out.append(_big_space(o, rng, 1.0, 0.12))
    return out


def burst():
    """The light of the deep set free when the Leviathan dies: a rising swell, a radiant blast, a long shimmer."""
    rng = dsp.rng_of(2300)
    dur = 5.0
    o = dsp.zeros(dur)
    swell = lib.pad_chord([110, 165, 220, 277.2, 330], 1.2, rng, attack=1.1, release=0.1, bright=0.6)
    dsp.place(o, swell, 0.0, 0.8)
    dsp.place(o, lib.boom(rng, 1.4, 3.0), 1.1, 0.8)
    chord = lib.pad_chord([220, 277.2, 330, 440, 554.4, 660], 3.6, rng, attack=0.02, release=2.8, bright=0.9)
    dsp.place(o, chord, 1.1, 0.7)
    for i, f in enumerate([880, 1108.7, 1318.5, 1760]):
        dsp.place(o, lib.bell(f, 3.0, rng, 0.8, 1.2), 1.1 + 0.05 * i, 0.25)
    dsp.place(o, lib.shimmer(3.5, rng, 2500, 10000, 45) * np.linspace(1, 0, dsp.n_of(3.5)), 1.15, 0.5)
    return [dsp.reverb(o, 3.0, 0.35, predelay=0.04, damp=7000, rng=rng)]


# ------------------------------------------------------------------------------------------------ tidelings
def tideling_ambient():
    out = []
    for k in range(3):
        rng = dsp.rng_of(2400 + k)
        dur = 0.7 + 0.1 * k
        f = dsp.env([(0, 300 + 40 * k), (dur * 0.4, 420 + 30 * k), (dur, 260)], dur)
        y = lib.growl_voice(f, dur, rng, size=0.6, rough=0.6, breath=0.3, vowel=[(900, 4, 0), (1800, 5, -5)], tilt=0.2)
        y = y * dsp.env([(0, 0), (0.05, 1), (dur, 0)], dur)
        y += 0.6 * lib.bubble_burst(dur, rng, rate=40, lo=500, hi=2200)
        out.append(dsp.underwater(y, 2600, rng))
    return out


def tideling_hurt():
    out = []
    for k in range(2):
        rng = dsp.rng_of(2500 + k)
        dur = 0.45
        f = dsp.env([(0, 520 + 50 * k), (0.06, 640), (dur, 380)], dur)
        y = lib.growl_voice(f, dur, rng, size=0.55, rough=0.9, breath=0.3, vowel=[(1000, 4, 0), (2000, 5, -4)], tilt=0.1)
        y *= dsp.env([(0, 0), (0.01, 1), (dur, 0)], dur)
        dsp.place(y, lib.splash(0.25, rng, 0.4), 0.0, 0.4)
        out.append(y)
    return out


def tideling_death():
    rng = dsp.rng_of(2600)
    dur = 1.0
    f = dsp.env([(0, 600), (0.1, 680), (dur, 220)], dur)
    y = lib.growl_voice(f, dur, rng, size=0.55, rough=1.0, breath=0.4, vowel=[(1000, 4, 0), (2000, 5, -4)], tilt=0.15)
    y *= dsp.env([(0, 0), (0.02, 1), (dur, 0)], dur)
    y += 0.7 * lib.bubble_burst(dur, rng, rate=70, lo=300, hi=1800, shape=np.linspace(1, 0, dsp.n_of(dur)))
    return [dsp.underwater(y, 3000, rng)]


def brine_hit():
    out = []
    for k in range(2):
        rng = dsp.rng_of(2700 + k)
        o = lib.splash(0.6, rng, 0.9, bright=1.1)
        hiss = lib.air_hiss(0.6, rng, 2500, 9000, 0.005, 0.5) * 0.25
        out.append(o + dsp.pad(hiss, len(o) / SR))
    return out
