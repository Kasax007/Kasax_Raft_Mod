"""Ambience of the raft dimension: the open sea around the raft, gulls far away and the moods of the deep.

Every function returns a list of variants; the sea loop is stereo and loops seamlessly.
"""
import numpy as np

from content.audio import creatures, dsp, lib
from content.audio.dsp import SR


def _seamless(y, loop):
    """y is loop + overlap long: crossfade the overlap into the start (equal power), return loop seconds."""
    n = dsp.n_of(loop)
    m = len(y) - n
    w = np.linspace(0, 1, m)
    a, b = np.sqrt(w), np.sqrt(1 - w)
    if y.ndim == 2:
        a, b = a[:, None], b[:, None]
    head = y[:m] * a + y[n:n + m] * b
    return np.concatenate([head, y[m:n]])


def _sea_channel(dur, rng, swell_period):
    t = dsp.times(dur)
    # the body of the sea: dark rolling noise, breathing with the swell
    swell = 0.6 + 0.4 * np.sin(2 * np.pi * t / swell_period + rng.uniform(0, 6)) + 0.15 * dsp.smooth_random(dur, 0.3, rng, -1, 1)
    body = dsp.lowpass(dsp.brown(dur, rng), 420) * swell
    wash = dsp.svf(dsp.pink(dur, rng), 500 + 450 * swell, 0.7, 'lp') * 0.5 * swell
    y = body + wash
    # waves lapping at the raft logs: little slaps and gurgles
    k = int(dur * 0.9)
    for _ in range(k):
        st = rng.uniform(0, dur - 1.2)
        d = rng.uniform(0.35, 0.9)
        lap = lib.water_rush(d, rng, rise=d * 0.35, hold=d * 0.1, lo=300, hi=2600) * rng.uniform(0.15, 0.4)
        dsp.place(y, lap, st)
        if rng.uniform() < 0.5:
            g = dsp.bubbles(0.5, rng, rate=25, f_lo=250, f_hi=1100, size=(0.03, 0.08), amp=(0.1, 0.4), rise=(0.5, 1.5))
            dsp.place(y, dsp.lowpass(g, 2000), st + d * 0.4, 0.25)
    # foam crests now and then
    for _ in range(int(dur / 5)):
        st = rng.uniform(0, dur - 2.0)
        foam = dsp.bandpass(dsp.crackle(1.6, 500, rng, 0.0006, 0.1, 0.8), 1500, 7000)
        foam *= dsp.env([(0, 0), (0.4, 1), (1.6, 0)], 1.6)
        dsp.place(y, foam, st, 0.12)
    # wind across the open water
    wind = dsp.bandpass(dsp.pink(dur, rng), 180, 900) * (0.25 + 0.2 * dsp.smooth_random(dur, 0.25, rng, 0, 1))
    return y + 0.35 * wind


def sea_loop():
    """24 s of open sea, stereo, seamless."""
    loop, over = 24.0, 3.0
    dur = loop + over
    left = _sea_channel(dur, dsp.rng_of(9000), 7.3)
    right = _sea_channel(dur, dsp.rng_of(9001), 8.1)
    common = dsp.lowpass(dsp.brown(dur, dsp.rng_of(9002)), 200) * 0.6
    y = np.stack([left + common, right + common], axis=1)
    y = dsp.high_shelf(y, 4000, -3.0)
    return [_seamless(y, loop)]


def gulls_far():
    """Seagulls far away over the water (an ambient addition)."""
    out = []
    calls = creatures.seagull_ambient()
    for k, c in enumerate(calls[:3]):
        rng = dsp.rng_of(9100 + k)
        y = dsp.lowpass(c, 3200, 2)
        y = dsp.reverb(y, 1.4, 0.45, predelay=0.05, damp=2500, rng=rng, early=0.2)
        out.append(y)
    return out


def _creak(dur, rng, f0, f1):
    """Metal under strain: stick-slip pulses through a hull resonance, the pitch sliding."""
    t = dsp.times(dur)
    rate = np.interp(t, [0, dur], [f0, f1]) * (1 + 0.08 * dsp.smooth_random(dur, 4, rng, -1, 1))
    ph = np.cumsum(rate) / SR
    pulses = np.zeros(len(t))
    idx = np.nonzero(np.diff(np.floor(ph)) > 0)[0]
    pulses[idx] = rng.uniform(0.5, 1.0, len(idx))
    body = dsp.formants(pulses, [(180, 12, 0), (420, 14, -3), (760, 16, -6), (1300, 18, -10)])
    return body * dsp.env([(0, 0), (dur * 0.2, 1), (dur * 0.7, 0.9), (dur, 0)], dur, 1.0)


def deep_mood():
    """Sounds of the deep that come when it is dark: a far whale, a wreck's hull groaning, a rumble, a settling knock."""
    out = []
    rng = dsp.rng_of(9200)
    y = lib.moan(3.2, 95, 70, rng, shape=1.1, vib=0.02, growl=0.2, harm=(1, 0.5, 0.25, 0.1))
    out.append(dsp.reverb(dsp.underwater(y, 900, rng), 4.0, 0.6, predelay=0.08, damp=1200, rng=rng))
    rng = dsp.rng_of(9201)
    y = _creak(3.0, rng, 38, 22)
    out.append(dsp.reverb(dsp.underwater(y, 1200, rng), 3.5, 0.5, predelay=0.05, damp=1500, rng=rng))
    rng = dsp.rng_of(9202)
    dur = 4.0
    y = dsp.lowpass(dsp.brown(dur, rng), 60) * dsp.env([(0, 0), (1.5, 1), (dur, 0)], dur)
    y += 0.3 * dsp.lowpass(lib.bubble_burst(dur, rng, rate=12, lo=70, hi=260, shape=np.full(dsp.n_of(dur), 0.8)), 400)
    out.append(dsp.reverb(y, 3.0, 0.4, rng=rng, damp=800))
    rng = dsp.rng_of(9203)
    o = dsp.zeros(2.5)
    for i, st in enumerate([0.0, 0.55, 0.8]):
        kn = dsp.modal([70 + 15 * i, 165, 290], [0.25, 0.12, 0.06], [1.0, 0.4, 0.2], 1.2, rng)
        dsp.place(o, kn, st, 1.0 - 0.3 * i)
    out.append(dsp.reverb(dsp.underwater(o, 700, rng), 4.0, 0.55, predelay=0.1, damp=900, rng=rng))
    return out
