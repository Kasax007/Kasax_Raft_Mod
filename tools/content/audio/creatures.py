"""Creatures of the sea and shore: sharks, whale, manta, jellyfish, anglerfish, moray, crabs and seagulls.

Every function returns a list of variants (mono float arrays); gen.py masters and exports them.
"""
import numpy as np

from content.audio import dsp, lib
from content.audio.dsp import SR


def _outdoor(x, rng, mix=0.08):
    return dsp.reverb(x, rt60=0.5, mix=mix, predelay=0.01, damp=6000, rng=rng, early=0.6)


# ------------------------------------------------------------------------------------------------ shark
def shark_ambient():
    """Sharks make no voice: a slow sweep of the tail through the water and the low pressure of a big body."""
    out = []
    for k in range(3):
        rng = dsp.rng_of(3000 + k)
        dur = 1.3 + 0.2 * k
        o = dsp.zeros(dur)
        for i in range(2):
            sw = lib.water_rush(0.55, rng, rise=0.25, hold=0.05, lo=120, hi=900)
            dsp.place(o, sw, 0.1 + i * (0.5 + 0.1 * k), 0.8 - 0.25 * i)
        press = dsp.lowpass(dsp.brown(dur, rng), 70) * dsp.env([(0, 0), (0.4, 1), (dur, 0)], dur)
        dsp.place(o, press, 0.0, 0.6)
        out.append(dsp.underwater(o, 1100, rng))
    return out


def shark_hurt():
    out = []
    for k in range(3):
        rng = dsp.rng_of(3100 + k)
        dur = 0.7
        o = lib.thrash(dur, rng, 1.2)
        knock = dsp.sine(np.interp(dsp.times(0.2), [0, 0.2], [120, 60]), 0.2) * dsp.decay_env(0.2, 0.05, 0.002)
        o = dsp.pad(o, dur + 0.3)
        dsp.place(o, knock, 0.0, 0.8)
        out.append(o)
    return out


def shark_death():
    rng = dsp.rng_of(3200)
    dur = 1.8
    o = lib.thrash(1.2, rng, 1.4)
    o = dsp.pad(o, dur) * dsp.env([(0, 1), (0.9, 1), (dur, 0.3)], dur)
    dsp.place(o, dsp.underwater(lib.bubble_burst(1.4, rng, rate=60, lo=150, hi=900, shape=np.linspace(1, 0, dsp.n_of(1.4))), 1500, rng), 0.4, 0.7)
    return [o]


def shark_bite():
    out = []
    for k in range(3):
        rng = dsp.rng_of(3300 + k)
        dur = 0.6
        o = dsp.zeros(dur)
        dsp.place(o, lib.jaw_snap(rng, 1.3 + 0.1 * k, 0.5), 0.0, 1.0)
        dsp.place(o, lib.crunch(0.18, rng, 220, 0.9), 0.008, 0.45)
        dsp.place(o, lib.water_rush(0.35, rng, rise=0.04, hold=0.05, lo=300, hi=1800), 0.0, 0.5)
        out.append(dsp.lowpass(o, 9000))
    return out


def shark_gnaw():
    """The shark bites the raft: teeth in planks (crunch, splinters) and a jerk of water."""
    out = []
    for k in range(3):
        rng = dsp.rng_of(3400 + k)
        dur = 0.75
        o = dsp.zeros(dur)
        dsp.place(o, lib.wood_crack(rng, 0.6, 1.0 + 0.15 * k), 0.0, 0.8)
        dsp.place(o, lib.jaw_snap(rng, 1.4, 0.4), 0.0, 0.7)
        # teeth grinding through the wood for a moment after the bite
        dsp.place(o, lib.crunch(0.35, rng, 320, 0.8), 0.05, 0.7)
        dsp.place(o, lib.splash(0.35, rng, 0.5), 0.05, 0.45)
        out.append(o)
    return out


def raft_break():
    """A raft plank is bitten through: a loud splintering crack and the pieces hitting the water."""
    out = []
    for k in range(2):
        rng = dsp.rng_of(3500 + k)
        dur = 1.2
        o = dsp.zeros(dur)
        dsp.place(o, lib.wood_crack(rng, 0.9, 0.8), 0.0, 1.0)
        dsp.place(o, lib.wood_crack(rng, 0.6, 1.2), 0.07, 0.6)
        dsp.place(o, lib.splash(0.6, rng, 0.8), 0.18, 0.6)
        out.append(_outdoor(o, rng, 0.1))
    return out


# ------------------------------------------------------------------------------------------------ whale
def whale_hurt():
    out = []
    for k in range(2):
        rng = dsp.rng_of(3600 + k)
        dur = 1.1 + 0.2 * k
        y = lib.moan(dur, 150 + 20 * k, 105, rng, shape=0.6, vib=0.03, growl=0.5, harm=(1, 0.6, 0.35, 0.2, 0.1))
        y = y * dsp.env([(0, 0), (0.06, 1), (dur, 0)], dur)
        y = y + 0.4 * lib.thrash(dur, rng, 0.6, under=False)[:len(y)]
        out.append(dsp.underwater(y, 1800, rng))
    return out


def whale_death():
    rng = dsp.rng_of(3700)
    dur = 4.0
    y = lib.moan(3.4, 170, 55, rng, shape=1.2, vib=0.02, growl=0.35, harm=(1, 0.55, 0.3, 0.15, 0.08))
    o = dsp.zeros(dur)
    dsp.place(o, y, 0.0)
    dsp.place(o, lib.bubble_burst(2.6, rng, rate=50, lo=100, hi=700, shape=np.linspace(0.2, 1, dsp.n_of(2.6))), 1.2, 0.5)
    return [dsp.reverb(dsp.underwater(o, 1600, rng), 2.2, 0.3, rng=rng, damp=2500)]


# ------------------------------------------------------------------------------------------------ manta ray
def manta_hurt():
    out = []
    for k in range(2):
        rng = dsp.rng_of(3800 + k)
        dur = 0.6
        o = dsp.zeros(dur)
        flap = lib.water_rush(0.4, rng, rise=0.05, hold=0.05, lo=200, hi=1400)
        dsp.place(o, flap, 0.0, 0.9)
        f = dsp.env([(0, 300), (0.05, 360), (0.3, 240)], 0.3)
        squeak = lib.growl_voice(f, 0.3, rng, size=0.8, rough=0.4, breath=0.2, vowel=[(700, 4, 0), (1400, 5, -6)])
        dsp.place(o, squeak * dsp.env([(0, 0), (0.02, 1), (0.3, 0)], 0.3), 0.02, 0.35)
        out.append(dsp.underwater(o, 1700, rng))
    return out


def manta_death():
    rng = dsp.rng_of(3900)
    dur = 1.4
    o = dsp.zeros(dur)
    for i in range(3):
        dsp.place(o, lib.water_rush(0.45, rng, rise=0.1, hold=0.05, lo=180, hi=1200), i * 0.28, 0.9 - 0.25 * i)
    dsp.place(o, lib.bubble_burst(1.0, rng, rate=45, lo=200, hi=1000, shape=np.linspace(1, 0, dsp.n_of(1.0))), 0.3, 0.6)
    return [dsp.underwater(o, 1600, rng)]


# ------------------------------------------------------------------------------------------------ jellyfish
def _squish(rng, dur=0.35, f=420.0):
    """A wet, gelatinous squelch: low noise through a wobbling resonance."""
    t = dsp.times(dur)
    fc = f * (1 + 0.6 * np.sin(2 * np.pi * (9 + 4 * rng.uniform()) * t)) * np.interp(t, [0, dur], [1.2, 0.7])
    y = dsp.svf(dsp.pink(dur, rng), fc, 4.0, 'bp')
    return y * dsp.env([(0, 0), (0.02, 1), (dur * 0.5, 0.6), (dur, 0)], dur)


def jellyfish_sting():
    """Nematocysts firing: a crackling electric prickle and a little squish."""
    out = []
    for k in range(3):
        rng = dsp.rng_of(4000 + k)
        dur = 0.5
        t = dsp.times(dur)
        zap = dsp.crackle(dur, 900, rng, 0.0004, 0.2, 1.0)
        buzz = (0.5 + 0.5 * np.sin(2 * np.pi * (60 + 30 * k) * t)) ** 3
        zap = dsp.bandpass(zap * buzz, 1800, 9000) * dsp.env([(0, 0), (0.01, 1), (0.15, 0.6), (dur, 0)], dur)
        hum = dsp.square(110 + 20 * k, dur, 15) * dsp.env([(0, 0), (0.01, 0.4), (0.2, 0.15), (dur, 0)], dur)
        y = zap + 0.25 * dsp.lowpass(hum, 1800)
        dsp.place(y, _squish(rng, 0.25, 500), 0.0, 0.6)
        out.append(y)
    return out


def jellyfish_hurt():
    out = []
    for k in range(2):
        rng = dsp.rng_of(4100 + k)
        y = _squish(rng, 0.4, 380 + 60 * k)
        dsp.place(y, lib.bubble_burst(0.4, rng, rate=40, lo=400, hi=1600), 0.05, 0.5)
        out.append(dsp.underwater(y, 2600, rng))
    return out


def jellyfish_death():
    rng = dsp.rng_of(4200)
    dur = 0.9
    o = dsp.zeros(dur)
    dsp.place(o, _squish(rng, 0.5, 350), 0.0, 1.0)
    dsp.place(o, lib.bubble_burst(0.8, rng, rate=60, lo=300, hi=1400, shape=np.linspace(1, 0, dsp.n_of(0.8))), 0.08, 0.6)
    return [dsp.underwater(o, 2400, rng)]


# ------------------------------------------------------------------------------------------------ anglerfish
def anglerfish_lure():
    """The glowing lure: an eerie, wavering hum from the dark (soft, so a trench full of anglers stays bearable)."""
    out = []
    for k in range(3):
        rng = dsp.rng_of(4300 + k)
        dur = 1.8 + 0.3 * k
        t = dsp.times(dur)
        base = [392, 349.2, 466.2][k]
        f = base * (1 + 0.012 * np.sin(2 * np.pi * 5.5 * t)) * np.interp(t, [0, dur], [1.0, 0.94])
        y = dsp.additive(f, dur, [1.0, 0.0, 0.3, 0.0, 0.1], rng=rng)
        y += 0.5 * dsp.sine(f * 1.5 * (1 + 0.004 * np.sin(2 * np.pi * 0.7 * t)), dur)
        y *= 0.75 + 0.25 * np.sin(2 * np.pi * 3.1 * t)
        y *= dsp.env([(0, 0), (0.5, 1), (dur - 0.6, 0.8), (dur, 0)], dur, 1.0)
        drone = dsp.lowpass(dsp.brown(dur, rng), 90) * 0.6 * dsp.env([(0, 0), (0.6, 1), (dur, 0)], dur)
        out.append(dsp.reverb(dsp.underwater(y + drone, 1500, rng), 2.0, 0.35, rng=rng, damp=2000))
    return out


def anglerfish_bite():
    out = []
    for k in range(2):
        rng = dsp.rng_of(4400 + k)
        dur = 0.7
        o = dsp.zeros(dur)
        dsp.place(o, lib.jaw_snap(rng, 1.6, 0.5), 0.0, 1.0)
        gulp = dsp.sine(np.interp(dsp.times(0.3), [0, 0.3], [180, 70]), 0.3) * dsp.env([(0, 0), (0.02, 1), (0.3, 0)], 0.3)
        dsp.place(o, gulp, 0.05, 0.6)
        dsp.place(o, lib.bubble_burst(0.4, rng, rate=60, lo=200, hi=900), 0.06, 0.5)
        out.append(dsp.underwater(o, 2400, rng, wobble=0.3))
    return out


def anglerfish_hurt():
    out = []
    for k in range(2):
        rng = dsp.rng_of(4500 + k)
        dur = 0.5
        f = dsp.env([(0, 170 + 20 * k), (0.06, 210), (dur, 120)], dur)
        y = lib.growl_voice(f, dur, rng, size=1.1, rough=0.9, breath=0.3, vowel=[(500, 4, 0), (1100, 5, -5)], tilt=0.4)
        y *= dsp.env([(0, 0), (0.01, 1), (dur, 0)], dur)
        dsp.place(y, lib.bubble_burst(0.4, rng, rate=40, lo=250, hi=1100), 0.0, 0.5)
        out.append(dsp.underwater(y, 2000, rng))
    return out


def anglerfish_death():
    rng = dsp.rng_of(4600)
    dur = 1.2
    f = dsp.env([(0, 200), (0.1, 230), (dur, 70)], dur)
    y = lib.growl_voice(f, dur, rng, size=1.1, rough=1.0, breath=0.35, vowel=[(500, 4, 0), (1100, 5, -5)], tilt=0.4)
    y *= dsp.env([(0, 0), (0.02, 1), (dur, 0)], dur)
    y += 0.7 * lib.bubble_burst(dur, rng, rate=50, lo=150, hi=900, shape=np.linspace(1, 0.2, dsp.n_of(dur)))
    return [dsp.underwater(y, 1800, rng)]


# ------------------------------------------------------------------------------------------------ moray eel
def moray_bite():
    """The eel shoots out of its hole: a hiss of water and a quick, sharp snap."""
    out = []
    for k in range(3):
        rng = dsp.rng_of(4700 + k)
        dur = 0.5
        o = dsp.zeros(dur)
        dsp.place(o, lib.water_rush(0.25, rng, rise=0.12, hold=0.02, lo=800, hi=4000), 0.0, 0.6)
        dsp.place(o, lib.jaw_snap(rng, 0.8, 0.3), 0.14, 1.0)
        out.append(dsp.underwater(o, 3500, rng, wobble=0.2))
    return out


# ------------------------------------------------------------------------------------------------ crabs
def _chitin(rng, f=3200.0, dur=0.05):
    return lib.metal_click(rng, f, dur, 0.004)


def crab_ambient():
    """Claws clicking and a few bubbles of froth at the mouth."""
    out = []
    for k in range(4):
        rng = dsp.rng_of(4800 + k)
        dur = 0.7
        o = dsp.zeros(dur)
        n = 3 + k % 3
        st = 0.02
        for i in range(n):
            dsp.place(o, _chitin(rng, rng.uniform(2400, 4200)), st, rng.uniform(0.5, 1.0))
            st += rng.uniform(0.05, 0.14)
        froth = dsp.bubbles(dur, rng, rate=30, f_lo=1500, f_hi=5000, size=(0.005, 0.02), amp=(0.1, 0.5), rise=(1, 3))
        dsp.place(o, froth, 0.0, 0.35)
        out.append(_outdoor(o, rng, 0.05))
    return out


def crab_pinch():
    """Snip: the two halves of a claw snapping shut."""
    out = []
    for k in range(3):
        rng = dsp.rng_of(4900 + k)
        dur = 0.25
        o = dsp.zeros(dur)
        dsp.place(o, _chitin(rng, 2600 + 300 * k, 0.06), 0.0, 1.0)
        dsp.place(o, _chitin(rng, 1900 + 200 * k, 0.08), 0.022, 0.9)
        scrape = dsp.bandpass(dsp.white(0.05, rng), 2500, 7000) * dsp.decay_env(0.05, 0.012)
        dsp.place(o, scrape, 0.01, 0.3)
        out.append(o)
    return out


def ember_crab_ambient():
    """Glowing shell: a soft sizzle, little pops and clicks."""
    out = []
    for k in range(3):
        rng = dsp.rng_of(5000 + k)
        dur = 1.0
        t = dsp.times(dur)
        sizzle = dsp.bandpass(dsp.white(dur, rng), 2500, 9000) * (0.4 + 0.6 * dsp.smooth_random(dur, 12, rng, 0, 1))
        pops = dsp.highpass(dsp.crackle(dur, 30, rng, 0.0015, 0.3, 1.0), 800)
        y = 0.3 * sizzle + 0.6 * pops
        y *= dsp.env([(0, 0), (0.08, 1), (dur - 0.2, 0.8), (dur, 0)], dur)
        for i in range(2 + k):
            dsp.place(y, _chitin(rng, rng.uniform(2000, 3500)), rng.uniform(0.05, 0.8), 0.6)
        out.append(y)
    return out


# ------------------------------------------------------------------------------------------------ seagull
def _gull_call(rng, notes, gap=(0.06, 0.11), base=1.0, rough=0.18, decay=0.08):
    """notes: [(duration, start, peak, end)] in Hz (scaled by base); one call = a row of notes."""
    total = sum(n[0] for n in notes) + gap[1] * len(notes) + 0.2
    o = dsp.zeros(total)
    st = 0.0
    for i, (d, a, p, e) in enumerate(notes):
        note = lib.bird_note(d, a * base, p * base, e * base, rng, peak_at=0.25, rough=rough)
        dsp.place(o, note, st, max(0.4, 1.0 - decay * i))
        st += d + rng.uniform(*gap)
    return o


def seagull_ambient():
    out = []
    # a long call: "kee-ow" then a row of short "kyow"s
    rng = dsp.rng_of(5100)
    notes = [(0.42, 820, 1150, 640)] + [(0.17, 760, 980, 600)] * 4
    out.append(_gull_call(rng, notes, (0.05, 0.08)))
    # a row of laughing "ha"s
    rng = dsp.rng_of(5101)
    out.append(_gull_call(rng, [(0.12, 900, 1060, 820)] * 6, (0.04, 0.06), base=1.05, decay=0.06))
    # two plaintive mews
    rng = dsp.rng_of(5102)
    out.append(_gull_call(rng, [(0.36, 980, 1240, 760), (0.33, 940, 1180, 720)], (0.12, 0.18), rough=0.1))
    # one short call
    rng = dsp.rng_of(5103)
    out.append(_gull_call(rng, [(0.22, 850, 1100, 700), (0.16, 800, 960, 640)], (0.05, 0.07)))
    return [_outdoor(x, dsp.rng_of(5110 + i), 0.12) for i, x in enumerate(out)]


def seagull_hurt():
    out = []
    for k in range(2):
        rng = dsp.rng_of(5200 + k)
        y = _gull_call(rng, [(0.2, 1150, 1500, 980)], base=1.0 + 0.05 * k, rough=0.4)
        out.append(_outdoor(y, rng, 0.08))
    return out


def seagull_death():
    rng = dsp.rng_of(5300)
    y = _gull_call(rng, [(0.22, 1200, 1450, 1100), (0.45, 1050, 1150, 520)], (0.03, 0.05), rough=0.35)
    return [_outdoor(y, rng, 0.1)]


def seagull_steal():
    """An excited, triumphant laugh as it snatches your fish."""
    out = []
    for k in range(2):
        rng = dsp.rng_of(5400 + k)
        y = _gull_call(rng, [(0.1, 1000, 1220, 950)] * (5 + k), (0.025, 0.04), base=1.1, rough=0.25, decay=0.03)
        out.append(_outdoor(y, rng, 0.1))
    return out
