"""Sound-design building blocks shared by the Beach Craft sound effects (made from dsp primitives only)."""
import numpy as np

from content.audio import dsp
from content.audio.dsp import SR


# ------------------------------------------------------------------------------------------------ water
def splash(size, rng, dur=None, bright=1.0):
    """A body hitting the water (size 0.2 = pebble .. 1 = a player .. 3 = a sea serpent).
    Impact thump, the burst of the crown, a falling spray of droplets and a few bubbles."""
    dur = dur or (0.5 + 0.9 * size)
    t = dsp.times(dur)
    out = dsp.zeros(dur)
    # impact: a short low thump (pitch falls quickly)
    td = min(dur, 0.25 + 0.15 * size)
    f = (140 / (0.6 + 0.4 * size)) * np.exp(-dsp.times(td) * 12) + 35
    thump = dsp.sine(f, td) * dsp.decay_env(td, 0.04 + 0.04 * size, 0.001)
    dsp.place(out, thump, 0.0, 0.9)
    # crown: broadband noise burst, darker for bigger bodies, with a slower tail
    nb = dsp.white(dur, rng)
    e = dsp.env([(0, 0), (0.004, 1), (0.05 + 0.05 * size, 0.55), (0.25 + 0.4 * size, 0.18), (dur, 0)], dur, 1.0)
    cut = np.interp(t, [0, 0.08, dur], [9000 * bright, (3500 - 600 * min(size, 2.5)) * bright, 1500])
    crown = dsp.svf(nb * e, cut, 0.6, 'lp')
    crown = dsp.highpass(crown, 120 + 160 / (0.5 + size))
    dsp.place(out, crown, 0.0, 0.8)
    # gurgle: low resonant blobs right after the impact
    gl = min(dur, 0.6 + 0.4 * size)
    gurgle = dsp.bubbles(gl, rng, rate=14 + 10 * size, f_lo=90 / (0.5 + 0.5 * size), f_hi=500 / (0.5 + 0.3 * size),
                         size=(0.05, 0.14), amp=(0.3, 1.0), rise=(0.3, 1.2),
                         env=np.exp(-dsp.times(gl) * (5 / (0.6 + 0.4 * size))))
    dsp.place(out, dsp.lowpass(gurgle, 1200), 0.02, 0.45)
    # spray: droplets falling back (tiny high bubbles and ticks), spread over the tail
    sl = max(0.2, dur - 0.08)
    density = np.interp(dsp.times(sl), [0, 0.1, sl * 0.5, sl], [0.0, 1.0, 0.5, 0.0])
    drops = dsp.bubbles(sl, rng, rate=60 + 80 * size, f_lo=900, f_hi=4200, size=(0.008, 0.03), amp=(0.05, 0.4),
                        rise=(1.0, 4.0), env=density)
    dsp.place(out, drops, 0.08, 0.35 * bright)
    return out


def bubble_burst(dur, rng, rate=40, lo=200, hi=1600, shape=None):
    """A cloud of bubbles rising (under water), envelope shape optional."""
    shape = shape if shape is not None else np.exp(-dsp.times(dur) * 2.5)
    b = dsp.bubbles(dur, rng, rate=rate, f_lo=lo, f_hi=hi, size=(0.02, 0.08), amp=(0.2, 1.0), rise=(0.5, 2.5), env=shape)
    return dsp.lowpass(b, 3500)


def water_rush(dur, rng, rise=0.3, hold=0.5, fall=None, lo=200, hi=2500, swirl=0.0):
    """Moving water mass (surge, wake, whirlpool): band noise with a slow envelope; swirl > 0 adds rotation."""
    fall = fall if fall is not None else max(0.05, dur - rise - hold)
    t = dsp.times(dur)
    e = dsp.env([(0, 0), (rise, 1), (rise + hold, 0.8), (dur, 0)], dur, 1.0)
    n = dsp.pink(dur, rng)
    center = np.sqrt(lo * hi) * (1 + 0.35 * dsp.smooth_random(dur, 3, rng, -1, 1))
    if swirl:
        center *= 1 + 0.4 * np.sin(2 * np.pi * swirl * t)
        e = e * (0.75 + 0.25 * np.sin(2 * np.pi * swirl * t + 1.0))
    y = dsp.svf(n, center, 0.9, 'bp') * 2.0 + dsp.lowpass(n, lo * 1.5) * 0.5
    return y * e


def thrash(dur, rng, intensity=1.0, under=True):
    """An animal thrashing in the water: irregular swishes, slaps and bubbles."""
    out = dsp.zeros(dur)
    k = int(3 + 4 * dur * intensity)
    for i in range(k):
        st = rng.uniform(0, dur * 0.8)
        d = rng.uniform(0.12, 0.3)
        sw = water_rush(d, rng, rise=d * 0.3, hold=d * 0.2, lo=250, hi=1800 if under else 4000)
        dsp.place(out, sw, st, rng.uniform(0.4, 1.0))
        if rng.uniform() < 0.5:
            dsp.place(out, splash(0.25 * intensity, rng, 0.35), st + d * 0.4, rng.uniform(0.2, 0.5))
    dsp.place(out, bubble_burst(dur, rng, rate=30 * intensity), 0.0, 0.5)
    return dsp.underwater(out, 1800, rng) if under else out


# ------------------------------------------------------------------------------------------------ voices
def growl_voice(f0, dur, rng, size=1.0, rough=0.5, breath=0.25, vowel=None, tilt=0.4):
    """A creature voice: a rough glottal source through a vocal tract (vowel formants scaled by body size)."""
    src = dsp.voice(f0, dur, rng, jitter=0.02 + 0.04 * rough, shimmer=0.15 + 0.3 * rough, subharmonic=0.3 * rough,
                    breath=breath, tilt=tilt)
    vowel = vowel or [(700, 4, 0), (1200, 5, -4), (2500, 6, -10)]
    tract = [(f / size, q, g) for f, q, g in vowel]
    y = dsp.formants(src, tract) + 0.15 * src
    # rough amplitude flutter (growl) at 20..35 Hz
    t = dsp.times(dur)
    fl = 1 + rough * 0.5 * np.sin(2 * np.pi * (24 + 6 * dsp.smooth_random(dur, 2, rng, -1, 1)) * t)
    return y * fl


def roar(dur, rng, f_lo=45, f_hi=95, size=2.5, wet=0.5, rough=0.8, attack=0.25, shape=None, body=1.0):
    """A huge beast roaring: rising then falling pitch, layered growl, breath noise, sub-bass, a wet throat.
    shape: optional (time, f0) points for the pitch contour."""
    t = dsp.times(dur)
    if shape is None:
        shape = [(0, f_lo), (attack, f_hi), (dur * 0.6, f_hi * 0.92), (dur, f_lo * 0.8)]
    f0 = dsp.env(shape, dur)
    f0 = f0 * (1 + 0.03 * dsp.smooth_random(dur, 6, rng, -1, 1))
    amp = dsp.env([(0, 0), (attack * 0.6, 0.7), (attack, 1.0), (dur * 0.7, 0.85), (dur, 0)], dur, 1.0)
    # 1) the growling vocal folds, two slightly detuned throats
    v1 = growl_voice(f0, dur, rng, size=size, rough=rough, breath=0.3, vowel=[(650, 3, 0), (1100, 4, -3), (2400, 5, -9), (3300, 6, -14)])
    v2 = growl_voice(f0 * 0.5, dur, rng, size=size * 1.2, rough=rough, breath=0.1, vowel=[(500, 3, 0), (900, 4, -4), (2000, 5, -12)])
    # 2) roaring breath: noise through the same tract, pulsed by the voice
    br = dsp.formants(dsp.pink(dur, rng), [(600 / size * 1.4, 2, 0), (1200 / size * 1.4, 2.5, -3), (2600 / size, 3, -8)])
    br *= 0.6 + 0.4 * np.abs(np.sin(np.pi * np.cumsum(f0) / SR))
    # 3) sub rumble one octave under the voice
    sub = dsp.sine(f0 * 0.5, dur) * 0.6 + dsp.lowpass(dsp.brown(dur, rng), 60) * 0.3
    y = 1.0 * v1 + 0.6 * v2 + 0.5 * br * body
    y = dsp.saturate(y / (np.max(np.abs(y)) + 1e-9) * 1.6, 1.8)
    y = y + 0.5 * sub
    # 4) a wet throat: low gurgling bubbles inside the roar
    if wet:
        gur = dsp.bubbles(dur, rng, rate=40 * wet, f_lo=80, f_hi=420, size=(0.04, 0.12), amp=(0.3, 1.0), rise=(0.2, 1.0))
        y = y + wet * 0.35 * dsp.lowpass(gur, 900) * amp
    # 5) rasp: the strained upper edge of a roar (2..5 kHz), pulsed by the vocal folds
    pulse = (0.5 + 0.5 * np.abs(np.sin(np.pi * np.cumsum(f0) / SR))) ** 2
    rasp = dsp.bandpass(dsp.white(dur, rng), 1700, 5200, 2) * pulse
    y = y + rasp * 0.22 * rough * (np.sqrt(np.mean(y ** 2)) / (np.sqrt(np.mean(rasp ** 2)) + 1e-9))
    y = y * amp
    y = dsp.low_shelf(y, 120, 4.0)
    y = dsp.peak_eq(y, 2800, -2.0, 0.8)
    return dsp.lowpass(y, 7500, 2)


def moan(dur, f0, f1, rng, shape=1.0, vib=0.02, growl=0.0, harm=(1, 0.45, 0.2, 0.08)):
    """A gliding whale-like call (glides from f0 to f1)."""
    t = dsp.times(dur)
    u = (t / dur) ** shape
    f = f0 + (f1 - f0) * (u * u * (3 - 2 * u))
    f = f * (1 + vib * np.sin(2 * np.pi * 5.1 * t + rng.uniform(0, 6)))
    y = dsp.additive(f, dur, list(harm), rng=rng)
    if growl:
        y *= 1 + growl * np.sin(2 * np.pi * 27 * t)
    return y * dsp.env([(0, 0), (min(0.3, dur * 0.3), 1), (dur * 0.6, 0.85), (dur, 0)], dur, 1.0)


def bird_note(dur, f_start, f_peak, f_end, rng, peak_at=0.3, nasal=1.0, rough=0.15, harm=None):
    """One gull-like note: a bright, nasal, slightly rough harmonic tone with a rise-fall pitch contour."""
    f = dsp.env([(0, f_start), (dur * peak_at, f_peak), (dur, f_end)], dur, 1.0)
    f = f * (1 + 0.012 * dsp.smooth_random(dur, 30, rng, -1, 1))
    harm = harm or [1.0, 0.8, 0.9, 0.6, 0.45, 0.3, 0.2, 0.12, 0.08, 0.05]
    y = dsp.additive(f, dur, harm, rng=rng)
    # roughness: fast irregular amplitude modulation (the harsh quality of gull calls)
    t = dsp.times(dur)
    y *= 1 + rough * dsp.lowpass(dsp.white(dur, rng), 180) * 3
    # nasal formants
    y = dsp.formants(y, [(1700, 3, 0), (3100, 4, -2 * nasal), (4300, 5, -6)]) * 0.8 + 0.3 * y
    e = dsp.env([(0, 0), (min(0.02, dur * 0.15), 1), (dur * 0.55, 0.85), (dur, 0)], dur, 1.0)
    return y * e


# ------------------------------------------------------------------------------------------------ impacts and mechanics
def jaw_snap(rng, size=1.0, dur=0.5):
    """Teeth clacking shut: a hard click, a short bony knock and a low thud."""
    out = dsp.zeros(dur)
    click = dsp.highpass(dsp.white(0.012, rng), 1500) * dsp.decay_env(0.012, 0.002, 0.0002)
    dsp.place(out, click, 0.0, 0.9)
    knock = dsp.modal([380 / size, 820 / size, 1450 / size, 2300 / size], [0.035, 0.025, 0.018, 0.01],
                      [1.0, 0.6, 0.4, 0.25], 0.15, rng)
    dsp.place(out, knock, 0.002, 0.7)
    td = 0.25
    thud = dsp.sine(np.interp(dsp.times(td), [0, td], [110 / size, 50 / size]), td) * dsp.decay_env(td, 0.05, 0.002)
    dsp.place(out, thud, 0.0, 0.8)
    return out


def crunch(dur, rng, density=120, bright=1.0):
    """Crunching material (wood, shell, bone): bursts of crackles through a resonant body."""
    c = dsp.crackle(dur, density, rng, decay=0.0015, lo=0.1, hi=1.0)
    body = dsp.formants(c, [(500, 3, 0), (1300 * bright, 4, -2), (2900 * bright, 5, -5)])
    e = dsp.env([(0, 0), (0.01, 1), (dur * 0.5, 0.6), (dur, 0)], dur)
    return (body + 0.3 * dsp.highpass(c, 3000)) * e


def wood_crack(rng, dur=0.6, size=1.0):
    """Planks splintering: a sharp crack, fibre tearing and the wooden body ringing."""
    out = dsp.zeros(dur)
    crack = dsp.highpass(dsp.white(0.03, rng), 800) * dsp.decay_env(0.03, 0.006, 0.0003)
    dsp.place(out, crack, 0.0, 1.0)
    modes = rng.uniform(180, 2400, 9) / size
    ring = dsp.modal(modes, rng.uniform(0.02, 0.08, 9), rng.uniform(0.2, 1.0, 9), 0.4, rng)
    dsp.place(out, ring, 0.0, 0.6)
    dsp.place(out, crunch(dur * 0.8, rng, density=260, bright=0.8), 0.01, 0.6)
    return out


def metal_click(rng, f=2600.0, dur=0.08, ring=0.02):
    """A small metal click (latch, ratchet tooth, switch)."""
    modes = [f, f * 1.58, f * 2.31, f * 3.1]
    y = dsp.modal(modes, [ring, ring * 0.7, ring * 0.5, ring * 0.3], [1.0, 0.6, 0.4, 0.2], dur, rng, 0.0002)
    n = dsp.highpass(dsp.white(0.006, rng), 3000) * dsp.decay_env(0.006, 0.0012, 0.0001)
    return dsp.place(y, n, 0.0, 0.8)


def ratchet(count, rate, rng, f=2200.0, jitter=0.1):
    """A row of clicks (crank, reel, winch)."""
    dur = count / rate + 0.1
    out = dsp.zeros(dur)
    for i in range(count):
        st = i / rate * (1 + jitter * rng.uniform(-1, 1))
        dsp.place(out, metal_click(rng, f * rng.uniform(0.9, 1.1), 0.05, 0.012), max(0, st), rng.uniform(0.6, 1.0))
    return out


def air_hiss(dur, rng, lo=1500, hi=9000, attack=0.01, release=0.2, wobble=0.1):
    t = dsp.times(dur)
    y = dsp.bandpass(dsp.white(dur, rng), lo, hi, 2)
    y *= 1 + wobble * dsp.smooth_random(dur, 8, rng, -1, 1)
    return y * dsp.env([(0, 0), (attack, 1), (max(attack, dur - release), 0.8), (dur, 0)], dur)


def boom(rng, size=1.0, dur=None):
    """An explosion-like low boom (cannon, slam): crack, body, sub drop and a rolling tail."""
    dur = dur or 1.2 + size
    out = dsp.zeros(dur)
    crack = dsp.highpass(dsp.white(0.04, rng), 500) * dsp.decay_env(0.04, 0.008, 0.0002)
    dsp.place(out, crack, 0.0, 1.0)
    bl = 0.9 * size
    f = 75 * np.exp(-dsp.times(bl) * 3) / size ** 0.3 + 28
    sub = dsp.sine(f, bl) * dsp.decay_env(bl, 0.25 * size, 0.003)
    dsp.place(out, sub, 0.0, 1.0)
    body = dsp.lowpass(dsp.pink(dur, rng), 900) * dsp.decay_env(dur, 0.18 * size, 0.002)
    dsp.place(out, body, 0.0, 0.9)
    roll = dsp.lowpass(dsp.brown(dur, rng), 260) * dsp.env([(0, 0), (0.08, 1), (dur * 0.5, 0.4), (dur, 0)], dur)
    dsp.place(out, roll, 0.0, 0.6)
    return out


def bell(f, dur, rng, bright=1.0, decay=1.5):
    """A glassy bell / crystal (inharmonic partials of a bar)."""
    ratios = [1.0, 2.0, 2.76, 4.07, 5.4, 6.8]
    amps = [1.0, 0.5, 0.45 * bright, 0.3 * bright, 0.18 * bright, 0.1 * bright]
    decs = [decay, decay * 0.6, decay * 0.45, decay * 0.3, decay * 0.2, decay * 0.12]
    return dsp.modal([f * r * rng.uniform(0.998, 1.002) for r in ratios], decs, amps, dur, rng, 0.001)


def pad_chord(freqs, dur, rng, attack=0.8, release=1.0, bright=0.5, detune=0.004):
    """A soft sustained chord (detuned saws through a low-pass): magic, choirs, hums."""
    y = dsp.zeros(dur)
    for f in freqs:
        for d in (-detune, 0, detune):
            y += dsp.saw(f * (1 + d) * (1 + 0.002 * dsp.smooth_random(dur, 2, rng, -1, 1)), dur, 30)
    y = dsp.lowpass(y, 600 + 3000 * bright, 2)
    return y * dsp.env([(0, 0), (attack, 1), (max(attack, dur - release), 0.9), (dur, 0)], dur, 1.0) / (3 * len(freqs))


def choir(freqs, dur, rng, vowel='a', attack=0.6, release=1.0):
    """Ahh/ooh voices: glottal sources per note through vowel formants, slow vibrato."""
    vowels = {'a': [(800, 6, 0), (1150, 7, -4), (2900, 8, -15)], 'o': [(450, 6, 0), (800, 7, -6), (2830, 8, -18)],
              'u': [(325, 6, 0), (700, 7, -10), (2530, 8, -20)]}
    y = dsp.zeros(dur)
    t = dsp.times(dur)
    for f in freqs:
        for k in range(3):
            vib = 1 + 0.006 * np.sin(2 * np.pi * (4.8 + k * 0.4) * t + rng.uniform(0, 6))
            src = dsp.voice(f * vib * (1 + rng.uniform(-0.004, 0.004)), dur, rng, jitter=0.004, shimmer=0.03, breath=0.15, tilt=0.3)
            y += dsp.formants(src, vowels[vowel])
    y /= np.max(np.abs(y)) + 1e-9
    return y * dsp.env([(0, 0), (attack, 1), (max(attack, dur - release), 0.9), (dur, 0)], dur, 1.0)


def shimmer(dur, rng, lo=2000, hi=9000, rate=40):
    """Sparkling high tinkles (magic dust)."""
    out = dsp.zeros(dur)
    for _ in range(int(rate * dur)):
        f = lo * (hi / lo) ** rng.uniform()
        d = rng.uniform(0.15, 0.5)
        b = dsp.sine(f, d) * dsp.decay_env(d, d / 4, 0.002)
        dsp.place(out, b, rng.uniform(0, dur - 0.05), rng.uniform(0.1, 0.5))
    return out


def beep(f, dur, wave='square', bright=0.4, attack=0.003, release=0.01):
    y = {'square': dsp.square, 'sine': dsp.sine, 'triangle': dsp.triangle, 'saw': dsp.saw}[wave](f, dur)
    if wave != 'sine':
        y = dsp.lowpass(y, 1500 + 6000 * bright, 2)
    return dsp.fade(y, attack, release)
