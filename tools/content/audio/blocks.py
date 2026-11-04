"""Blocks and machines: water purifier, grill, air compressor, wind turbine, ship cannon, terminals and panels.

Every function returns a list of variants (mono float arrays, the wind turbine a seamless loop); gen.py masters and
exports them.
"""
import numpy as np

from content.audio import dsp, lib
from content.audio.dsp import SR


def _room(x, rng, mix=0.08, rt=0.4):
    return dsp.reverb(x, rt60=rt, mix=mix, predelay=0.006, damp=6000, rng=rng, early=0.8)


def purifier_boil():
    """Water boiling in the purifier pot: big rolling bubbles, a simmer and a breath of steam."""
    out = []
    for k in range(3):
        rng = dsp.rng_of(6000 + k)
        dur = 2.4
        t = dsp.times(dur)
        dens = 0.55 + 0.45 * dsp.smooth_random(dur, 2.5, rng, 0, 1)
        big = dsp.bubbles(dur, rng, rate=26, f_lo=180, f_hi=650, size=(0.05, 0.12), amp=(0.4, 1.0), rise=(0.5, 1.5), env=dens)
        small = dsp.bubbles(dur, rng, rate=90, f_lo=700, f_hi=2600, size=(0.01, 0.035), amp=(0.1, 0.5), rise=(1, 3), env=dens)
        simmer = dsp.lowpass(dsp.brown(dur, rng), 220) * 0.35
        steam = dsp.bandpass(dsp.white(dur, rng), 3000, 9000) * (0.05 + 0.05 * dsp.smooth_random(dur, 1.5, rng, 0, 1))
        y = big + 0.6 * small + simmer + steam
        y = dsp.formants(y, [(500, 1.2, 0), (1300, 1.5, -2)]) * 0.7 + 0.5 * y   # the metal pot
        y = dsp.fade(y, 0.25, 0.4)
        out.append(_room(y, rng, 0.1, 0.35))
    return out


def grill_sizzle():
    """Food on a hot grill: fat sizzling, spits and pops."""
    out = []
    for k in range(3):
        rng = dsp.rng_of(6100 + k)
        dur = 2.2
        t = dsp.times(dur)
        hiss = dsp.bandpass(dsp.white(dur, rng), 2500, 11000) * (0.5 + 0.5 * dsp.smooth_random(dur, 6, rng, 0.2, 1))
        frying = dsp.crackle(dur, 1400, rng, 0.0003, 0.05, 0.6)
        frying = dsp.highpass(frying, 1800)
        pops = dsp.bandpass(dsp.crackle(dur, 9, rng, 0.002, 0.5, 1.0), 600, 6000)
        y = 0.3 * hiss + 0.8 * frying + 0.9 * pops
        out.append(dsp.fade(y, 0.2, 0.35))
    return out


def compressor_crank():
    """One turn of the hand crank: ratchet teeth and the piston pushing air into the tank."""
    out = []
    for k in range(3):
        rng = dsp.rng_of(6200 + k)
        dur = 0.75
        o = dsp.zeros(dur)
        dsp.place(o, lib.ratchet(5, 13 + 2 * k, rng, f=1900 + 150 * k), 0.0, 0.7)
        chuff = lib.air_hiss(0.35, rng, 500, 4000, 0.05, 0.2) * dsp.env([(0, 0), (0.1, 1), (0.35, 0)], 0.35)
        dsp.place(o, chuff, 0.25, 0.8)
        clunk = dsp.modal([210, 470, 980], [0.05, 0.03, 0.02], [1.0, 0.5, 0.3], 0.15, rng)
        dsp.place(o, clunk, 0.24, 0.6)
        out.append(_room(o, rng))
    return out


def compressor_fill():
    """Air hissing into the tank, the pitch of the hiss rising with the pressure."""
    out = []
    for k in range(2):
        rng = dsp.rng_of(6300 + k)
        dur = 1.6
        t = dsp.times(dur)
        fc = np.interp(t, [0, dur], [1800, 3600 + 400 * k])
        y = dsp.svf(dsp.white(dur, rng), fc, 1.6, 'bp')
        y *= dsp.env([(0, 0), (0.06, 1), (dur - 0.3, 0.9), (dur, 0)], dur)
        y *= 1 + 0.15 * dsp.smooth_random(dur, 10, rng, -1, 1)
        dsp.place(y, lib.metal_click(rng, 2300, 0.06, 0.01), 0.0, 0.5)
        out.append(_room(y, rng))
    return out


def turbine_loop():
    """Wind turbine rotor (seamless 4 s loop): three blades sweeping past the mast, wind and a quiet gearbox hum."""
    rng = dsp.rng_of(6400)
    loop = 4.0
    over = 0.5
    dur = loop + over
    t = dsp.times(dur)
    passes = 1.5    # blade passes per second at full speed (pitch and speed set by the game)
    ph = 2 * np.pi * passes * t
    swish = (0.5 + 0.5 * np.cos(ph)) ** 3
    wind = dsp.bandpass(dsp.pink(dur, rng), 250, 2200) * (0.15 + swish)
    wind = dsp.svf(wind, 600 + 700 * swish, 0.8, 'lp')
    gust = dsp.lowpass(dsp.pink(dur, rng), 400) * 0.25
    hum = (dsp.sine(118, dur) + 0.4 * dsp.sine(236, dur)) * 0.05
    y = wind + gust + hum
    # make the loop seamless: crossfade the overlap at the end into the start
    n, m = dsp.n_of(loop), dsp.n_of(over)
    w = np.linspace(0, 1, m)
    head = y[:m] * w + y[n:n + m] * (1 - w)
    res = np.concatenate([head, y[m:n]])
    return [res]


def cannon_fire():
    """A ship cannon: the crack of the charge, a heavy boom, the barrel ringing and an echo over the water."""
    out = []
    for k in range(2):
        rng = dsp.rng_of(6500 + k)
        dur = 2.6
        o = dsp.zeros(dur)
        dsp.place(o, lib.boom(rng, 1.25, dur), 0.0, 1.0)
        ring = dsp.modal([180 + 10 * k, 412, 655, 980], [0.35, 0.22, 0.15, 0.1], [0.5, 0.35, 0.25, 0.15], 0.9, rng)
        dsp.place(o, ring, 0.0, 0.35)
        whoosh = lib.air_hiss(0.4, rng, 600, 5000, 0.002, 0.35)
        dsp.place(o, whoosh, 0.0, 0.5)
        o = dsp.delay(o, 0.32 + 0.05 * k, feedback=0.35, mix=0.22, damp=0.5, repeats=3)
        out.append(dsp.reverb(o, 1.6, 0.18, rng=rng, damp=3000))
    return out


def terminal_use():
    """The expedition computer wakes: a relay click and a short melody of square beeps."""
    out = []
    scale = [523.3, 587.3, 659.3, 784.0, 880.0, 1046.5]
    for k in range(3):
        rng = dsp.rng_of(6600 + k)
        dur = 0.75
        o = dsp.zeros(dur)
        dsp.place(o, lib.metal_click(rng, 1800, 0.05, 0.008), 0.0, 0.8)
        st = 0.06
        for i in range(3 + k % 2):
            f = scale[rng.integers(len(scale))]
            d = rng.choice([0.06, 0.08, 0.12])
            dsp.place(o, lib.beep(f, d, 'square', 0.25), st, 0.45)
            st += d + 0.025
        out.append(_room(o, rng, 0.1))
    return out


def panel_beep():
    """A button on a control panel: a click and one or two soft beeps."""
    out = []
    for k in range(4):
        rng = dsp.rng_of(6700 + k)
        dur = 0.4
        o = dsp.zeros(dur)
        dsp.place(o, lib.metal_click(rng, 2400, 0.04, 0.006), 0.0, 0.7)
        f = [880, 1175, 988, 1319][k]
        dsp.place(o, lib.beep(f, 0.07, 'triangle'), 0.03, 0.5)
        if k % 2:
            dsp.place(o, lib.beep(f * 1.25, 0.07, 'triangle'), 0.12, 0.45)
        out.append(_room(o, rng, 0.08))
    return out


def hammock_creak():
    """Ropes and wood creaking as someone settles into the hammock."""
    out = []
    for k in range(2):
        rng = dsp.rng_of(6800 + k)
        dur = 0.9
        t = dsp.times(dur)
        f = np.interp(t, [0, dur], [180 + 30 * k, 140])
        stick = (dsp.crackle(dur, 260, rng, 0.0008, 0.3, 1.0))
        y = dsp.formants(stick, [(f.mean(), 8, 0), (f.mean() * 2.7, 10, -4), (1400, 6, -8)])
        y *= dsp.env([(0, 0), (0.1, 1), (0.6, 0.7), (dur, 0)], dur)
        out.append(_room(y, rng, 0.08))
    return out
