"""Items: harpoon gun, sea scooter, plastic hook and the oxygen tank's regulator.

Every function returns a list of variants (mono float arrays); gen.py masters and exports them.
"""
import numpy as np

from content.audio import dsp, lib
from content.audio.dsp import SR


def harpoon_fire():
    """Compressed air kicks the harpoon out: trigger clack, a sharp pneumatic burst, the shaft whooshing away."""
    out = []
    for k in range(3):
        rng = dsp.rng_of(7000 + k)
        dur = 0.8
        o = dsp.zeros(dur)
        dsp.place(o, lib.metal_click(rng, 1500 + 200 * k, 0.08, 0.02), 0.0, 0.8)
        burst = dsp.highpass(dsp.white(0.25, rng), 700) * dsp.decay_env(0.25, 0.035, 0.0005)
        dsp.place(o, burst, 0.008, 1.0)
        thump = dsp.sine(np.interp(dsp.times(0.18), [0, 0.18], [150, 70]), 0.18) * dsp.decay_env(0.18, 0.04, 0.001)
        dsp.place(o, thump, 0.008, 0.9)
        t = dsp.times(0.5)
        whoosh = dsp.svf(dsp.white(0.5, rng), np.interp(t, [0, 0.5], [3200, 900]), 1.2, 'bp')
        whoosh *= dsp.env([(0, 0), (0.03, 1), (0.5, 0)], 0.5, 1.0)
        dsp.place(o, whoosh, 0.02, 0.5)
        twang = dsp.modal([520 + 30 * k, 1310, 2150], [0.12, 0.06, 0.04], [1.0, 0.4, 0.2], 0.3, rng)
        dsp.place(o, twang, 0.01, 0.25)
        out.append(o)
    return out


def harpoon_reload():
    """A new harpoon slides in, the latch snaps and the air chamber is pumped up."""
    out = []
    for k in range(2):
        rng = dsp.rng_of(7100 + k)
        dur = 0.7
        o = dsp.zeros(dur)
        slide = dsp.bandpass(dsp.white(0.18, rng), 1500, 6000) * dsp.env([(0, 0), (0.05, 1), (0.18, 0)], 0.18)
        dsp.place(o, slide, 0.0, 0.4)
        dsp.place(o, lib.metal_click(rng, 1700, 0.08, 0.02), 0.17, 1.0)
        dsp.place(o, lib.metal_click(rng, 2600, 0.06, 0.012), 0.24, 0.7)
        dsp.place(o, lib.ratchet(3, 22, rng, 2100), 0.32, 0.5)
        hiss = lib.air_hiss(0.25, rng, 1500, 7000, 0.02, 0.15)
        dsp.place(o, hiss, 0.44, 0.35)
        out.append(o)
    return out


def scooter_run():
    """Sea scooter: an electric motor whine and a propeller churning water (played every 0.6 s, overlapping)."""
    out = []
    for k in range(3):
        rng = dsp.rng_of(7200 + k)
        dur = 0.85
        t = dsp.times(dur)
        f = (215 + 8 * k) * (1 + 0.01 * dsp.smooth_random(dur, 6, rng, -1, 1))
        whine = dsp.additive(f, dur, [1.0, 0.5, 0.35, 0.2, 0.15, 0.1], rng=rng)
        whine += 0.25 * dsp.sine(f * 4.02, dur)
        blades = 0.6 + 0.4 * np.sin(2 * np.pi * 31 * t + rng.uniform(0, 6))
        churn = dsp.bandpass(dsp.pink(dur, rng), 200, 1800) * blades
        bub = dsp.bubbles(dur, rng, rate=60, f_lo=300, f_hi=1500, size=(0.015, 0.05), amp=(0.2, 0.7))
        y = 0.35 * whine + churn + 0.5 * bub
        y *= dsp.env([(0, 0), (0.15, 1), (dur - 0.2, 1), (dur, 0)], dur, 1.0)
        out.append(dsp.underwater(y, 2400, rng, wobble=0.9))
    return out


def hook_reel():
    """Reeling in with the plastic hook: line whirring off the spool and things dragged through the water."""
    out = []
    for k in range(3):
        rng = dsp.rng_of(7300 + k)
        dur = 0.55
        t = dsp.times(dur)
        clicks = lib.ratchet(9, 18 + 2 * k, rng, 3000, 0.2)
        line = dsp.bandpass(dsp.white(dur, rng), 3500, 9000) * 0.15 * dsp.env([(0, 0), (0.05, 1), (dur, 0)], dur)
        drag = lib.water_rush(dur, rng, rise=0.15, hold=0.2, lo=300, hi=2500)
        o = dsp.zeros(dur + 0.1)
        dsp.place(o, clicks, 0.0, 0.5)
        dsp.place(o, line, 0.0, 1.0)
        dsp.place(o, drag, 0.02, 0.6)
        out.append(o)
    return out


def tank_low():
    """The regulator warning: two short electronic beeps."""
    rng = dsp.rng_of(7400)
    o = dsp.zeros(0.45)
    dsp.place(o, lib.beep(1568, 0.09, 'square', 0.3), 0.0, 0.6)
    dsp.place(o, lib.beep(1175, 0.12, 'square', 0.3), 0.15, 0.6)
    return [o]


def tank_refill():
    """Air rushing into the tank, then the valve closes."""
    rng = dsp.rng_of(7500)
    dur = 0.9
    t = dsp.times(dur)
    y = dsp.svf(dsp.white(dur, rng), np.interp(t, [0, dur], [1200, 2800]), 1.4, 'bp')
    y *= dsp.env([(0, 0), (0.04, 1), (0.6, 0.8), (0.75, 0)], dur)
    dsp.place(y, lib.metal_click(rng, 1900, 0.08, 0.015), 0.74, 0.8)
    dsp.place(y, lib.bubble_burst(0.6, rng, rate=40, lo=400, hi=1500), 0.0, 0.3)
    return [y]
