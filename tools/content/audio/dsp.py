"""A small DSP toolkit for the synthesised sounds of Beach Craft (numpy + scipy, ffmpeg with libvorbis for the export).

Signals are float64 numpy arrays at SR (mono: shape (n,), stereo: shape (n, 2)). Every generator takes a numpy
Generator so a sound is the same on every run. Nothing here reads recordings: all material is made from oscillators,
noise and filters.
"""
import os
import subprocess
import tempfile
import wave

import numpy as np
from scipy import signal

SR = 44100


# ------------------------------------------------------------------------------------------------ basics
def n_of(dur):
    return max(1, int(round(dur * SR)))


def times(dur):
    return np.arange(n_of(dur)) / SR


def zeros(dur):
    return np.zeros(n_of(dur))


def rng_of(seed):
    return np.random.default_rng(seed)


def place(buf, sig, start, gain=1.0):
    """Adds sig into buf at start seconds (mono or stereo). A signal that runs past the end of buf is faded out over
    its last 20 ms inside buf instead of being cut (no click)."""
    i = int(round(start * SR))
    if i >= len(buf):
        return buf
    j = min(len(buf), i + len(sig))
    part = sig[:j - i]
    if j - i < len(sig):
        part = fade(part, 0.0, min(0.02, (j - i) / SR * 0.5))
    if buf.ndim == 2 and part.ndim == 1:
        buf[i:j] += gain * part[:, None]
    else:
        buf[i:j] += gain * part
    return buf


def pad(x, dur):
    """x extended with silence to dur seconds (or cut)."""
    n = n_of(dur)
    if len(x) >= n:
        return x[:n].copy()
    shape = (n - len(x),) + x.shape[1:]
    return np.concatenate([x, np.zeros(shape)])


def mix(dur, *parts):
    """parts: (signal, start, gain) tuples -> one buffer of dur seconds."""
    stereo = any(p[0].ndim == 2 for p in parts)
    buf = np.zeros((n_of(dur), 2)) if stereo else np.zeros(n_of(dur))
    for p in parts:
        sig, start = p[0], p[1]
        gain = p[2] if len(p) > 2 else 1.0
        if stereo and sig.ndim == 1:
            sig = np.stack([sig, sig], axis=1)
        place(buf, sig, start, gain)
    return buf


def trim(x, threshold_db=-60.0, tail=0.05):
    """Cuts silence at the end (keeps tail seconds after the last sample above threshold)."""
    m = np.abs(x) if x.ndim == 1 else np.max(np.abs(x), axis=1)
    thr = 10 ** (threshold_db / 20) * (np.max(m) + 1e-12)
    idx = np.nonzero(m > thr)[0]
    if len(idx) == 0:
        return x
    end = min(len(x), idx[-1] + n_of(tail))
    return fade(x[:end], 0.0, min(tail, (end / SR) * 0.5))


# ------------------------------------------------------------------------------------------------ noise
def white(dur, rng):
    return rng.standard_normal(n_of(dur))


def _shaped(dur, rng, power):
    n = n_of(dur)
    spec = np.fft.rfft(rng.standard_normal(n))
    f = np.fft.rfftfreq(n, 1 / SR)
    f[0] = f[1] if len(f) > 1 else 1.0
    spec /= f ** (power / 2)
    y = np.fft.irfft(spec, n)
    return y / (np.std(y) + 1e-12)


def pink(dur, rng):
    return _shaped(dur, rng, 1.0)


def brown(dur, rng):
    return _shaped(dur, rng, 2.0)


def smooth_random(dur, rate, rng, lo=0.0, hi=1.0):
    """A slowly wandering control signal (cubic interpolation of random points, rate points per second)."""
    n = n_of(dur)
    k = max(4, int(dur * rate) + 4)
    pts = rng.uniform(lo, hi, k)
    xs = np.linspace(0, dur, k)
    from scipy.interpolate import CubicSpline
    y = CubicSpline(xs, pts)(np.arange(n) / SR)
    return np.clip(y, min(lo, hi), max(lo, hi))


def crackle(dur, rate, rng, decay=0.002, lo=0.2, hi=1.0):
    """Random clicks (Poisson, rate per second) with short exponential tails: fire, sizzle, bubbles of foam."""
    n = n_of(dur)
    y = np.zeros(n)
    count = rng.poisson(rate * dur)
    pos = rng.integers(0, n, count)
    amp = rng.uniform(lo, hi, count) * rng.choice([-1, 1], count)
    np.add.at(y, pos, amp)
    k = np.exp(-np.arange(n_of(decay * 6)) / (decay * SR))
    return signal.fftconvolve(y, k)[:n]


# ------------------------------------------------------------------------------------------------ oscillators
def _freq_array(freq, n):
    f = np.asarray(freq, dtype=float)
    if f.ndim == 0:
        return np.full(n, float(f))
    if len(f) != n:
        f = np.interp(np.linspace(0, 1, n), np.linspace(0, 1, len(f)), f)
    return f


def phase(freq, dur, ph0=0.0):
    n = n_of(dur)
    f = _freq_array(freq, n)
    return ph0 + 2 * np.pi * np.cumsum(f) / SR, f


def sine(freq, dur, ph0=0.0):
    ph, _ = phase(freq, dur, ph0)
    return np.sin(ph)


def additive(freq, dur, amps, ph0=0.0, rng=None):
    """Harmonic oscillator: amps[k-1] is the level of harmonic k; harmonics above Nyquist fade out (no aliasing)."""
    ph, f = phase(freq, dur, ph0)
    y = np.zeros(len(ph))
    offsets = rng.uniform(0, 2 * np.pi, len(amps)) if rng is not None else np.zeros(len(amps))
    for k, a in enumerate(amps, start=1):
        if a == 0:
            continue
        g = np.clip((SR * 0.45 - k * f) / (SR * 0.05), 0, 1)
        if not np.any(g):
            break
        y += a * g * np.sin(k * ph + offsets[k - 1])
    return y


def saw(freq, dur, harmonics=60, ph0=0.0):
    return additive(freq, dur, [1.0 / k for k in range(1, harmonics + 1)], ph0) * (2 / np.pi)


def square(freq, dur, harmonics=40, ph0=0.0):
    return additive(freq, dur, [(1.0 / k if k % 2 else 0.0) for k in range(1, harmonics + 1)], ph0) * (4 / np.pi)


def triangle(freq, dur, harmonics=21, ph0=0.0):
    amps = [((-1) ** ((k - 1) // 2)) / (k * k) if k % 2 else 0.0 for k in range(1, harmonics + 1)]
    return additive(freq, dur, amps, ph0) * (8 / np.pi ** 2)


def fm(carrier, ratio, index, dur, ph0=0.0):
    """Two-operator FM: carrier frequency (array or scalar), modulator = carrier * ratio, index (array or scalar)."""
    n = n_of(dur)
    fc = _freq_array(carrier, n)
    idx = _freq_array(index, n)
    pm = np.sin(2 * np.pi * np.cumsum(fc * ratio) / SR)
    return np.sin(ph0 + 2 * np.pi * np.cumsum(fc) / SR + idx * pm)


def voice(f0, dur, rng, jitter=0.01, shimmer=0.08, open_q=0.6, subharmonic=0.0, breath=0.05, tilt=0.0):
    """Glottal-pulse source for animal and monster voices: one pulse per period of the (time-varying) pitch f0, with
    period jitter, amplitude shimmer, an optional subharmonic (every second pulse weaker: rough, croaky) and breath
    noise synchronous to the pulses. tilt > 0 makes it darker (one-pole low-pass of that strength)."""
    n = n_of(dur)
    f = _freq_array(f0, n)
    y = np.zeros(n)
    gate = np.zeros(n)
    t = 0
    odd = False
    while t < n - 2:
        period = SR / max(20.0, f[t]) * (1 + jitter * rng.standard_normal())
        period = max(8.0, period)
        L = int(period)
        if t + L >= n:
            L = n - t
        oq = open_q
        Lo = max(4, int(L * oq))
        u = np.arange(Lo) / Lo
        # Rosenberg pulse, used as its derivative: a sharp closing edge gives the buzz
        flow = np.where(u < 0.6, 0.5 * (1 - np.cos(np.pi * u / 0.6)), np.cos(np.pi * (u - 0.6) / 0.8))
        dflow = np.diff(np.concatenate([[0.0], flow]))
        a = 1 + shimmer * rng.standard_normal()
        if subharmonic and odd:
            a *= 1 - subharmonic
        odd = not odd
        y[t:t + Lo] += a * dflow[:max(0, min(Lo, n - t))] * period / 20
        gate[t:t + Lo] += np.sin(np.pi * u)[:max(0, min(Lo, n - t))]
        t += L
    y /= np.max(np.abs(y)) + 1e-12
    if breath:
        y += breath * highpass(white(dur, rng), 400) * (0.3 + gate) * 0.5
    if tilt:
        y = onepole_lp(y, 1.0 - tilt)
    return y


def chirp_bubble(f0, dur, rise=2.0, decay=None, phase0=0.0):
    """One bubble (Minnaert): a sine that rises in pitch as it nears the surface and dies away quickly."""
    t = times(dur)
    decay = decay if decay is not None else dur / 4
    f = f0 * (1 + rise * t / dur)
    ph = phase0 + 2 * np.pi * np.cumsum(f) / SR
    e = np.exp(-t / decay) * np.minimum(1, t / 0.0015)
    return np.sin(ph) * e


def bubbles(dur, rng, rate=20, f_lo=300, f_hi=1500, size=(0.02, 0.09), amp=(0.2, 1.0), rise=(0.5, 3.0), env=None):
    """A stream of bubbles (rate per second). env: optional density envelope (array over dur, 0..1)."""
    n = n_of(dur)
    y = np.zeros(n)
    count = rng.poisson(rate * dur)
    for _ in range(count):
        st = rng.uniform(0, dur)
        level = 1.0
        if env is not None:
            level = float(env[min(n - 1, int(st * SR))])
            if rng.uniform() > level:
                continue
        d = rng.uniform(*size)
        # small bubbles are high: pitch and length go together
        f = f_lo * (f_hi / f_lo) ** rng.uniform()
        d *= (f_lo / f) ** 0.5 * 1.4
        b = chirp_bubble(f, d, rng.uniform(*rise), d / 3.5, rng.uniform(0, 6.3)) * rng.uniform(*amp)
        # where the density envelope is low the few bubbles left are also quieter (no lone loud blip in a tail)
        place(y, b, st, max(0.0, level) ** 0.5)
    return y


def modal(freqs, decays, amps, dur, rng=None, strike=0.0005):
    """Struck object: a sum of damped sine modes (metal, wood, stone, glass); a short strike transient on top."""
    t = times(dur)
    y = np.zeros(len(t))
    for f, d, a in zip(freqs, decays, amps):
        ph = rng.uniform(0, 2 * np.pi) if rng is not None else 0.0
        y += a * np.sin(2 * np.pi * f * t + ph) * np.exp(-t / d)
    y *= np.minimum(1, t / strike)
    return fade(y, 0.0, min(0.03, dur * 0.2))


# ------------------------------------------------------------------------------------------------ envelopes
def env(points, dur, curve=1.0):
    """Piecewise envelope through (time, value) points; curve > 1 bends segments (slower start)."""
    n = n_of(dur)
    ts = np.array([p[0] for p in points], dtype=float)
    vs = np.array([p[1] for p in points], dtype=float)
    x = np.arange(n) / SR
    if curve == 1.0:
        return np.interp(x, ts, vs)
    idx = np.clip(np.searchsorted(ts, x, side='right') - 1, 0, len(ts) - 2)
    t0, t1 = ts[idx], ts[idx + 1]
    u = np.clip((x - t0) / np.maximum(1e-9, t1 - t0), 0, 1) ** curve
    return vs[idx] + (vs[idx + 1] - vs[idx]) * u


def adsr(dur, a=0.01, d=0.1, s=0.7, r=0.2):
    r = min(r, dur * 0.9)
    return env([(0, 0), (a, 1), (a + d, s), (max(a + d, dur - r), s), (dur, 0)], dur)


def decay_env(dur, tau, attack=0.002):
    t = times(dur)
    return np.minimum(1, t / max(attack, 1e-5)) * np.exp(-t / tau)


def fade(x, fin=0.005, fout=0.02):
    """Raised-cosine fade in/out (seconds)."""
    y = x.copy()
    a, b = n_of(fin) if fin > 0 else 0, n_of(fout) if fout > 0 else 0
    if a > 1:
        w = 0.5 - 0.5 * np.cos(np.linspace(0, np.pi, a))
        y[:a] *= w if y.ndim == 1 else w[:, None]
    if b > 1:
        w = 0.5 + 0.5 * np.cos(np.linspace(0, np.pi, b))
        y[-b:] *= w if y.ndim == 1 else w[:, None]
    return y


# ------------------------------------------------------------------------------------------------ filters
def _sos(x, sos):
    return signal.sosfilt(sos, x, axis=0)


def lowpass(x, fc, order=4):
    fc = min(fc, SR * 0.49)
    return _sos(x, signal.butter(order, fc, 'low', fs=SR, output='sos'))


def highpass(x, fc, order=4):
    return _sos(x, signal.butter(order, max(5.0, fc), 'high', fs=SR, output='sos'))


def bandpass(x, lo, hi, order=2):
    return _sos(x, signal.butter(order, [max(5.0, lo), min(hi, SR * 0.49)], 'band', fs=SR, output='sos'))


def _rbj(kind, f0, q, gain_db=0.0):
    A = 10 ** (gain_db / 40)
    w = 2 * np.pi * min(f0, SR * 0.49) / SR
    alpha = np.sin(w) / (2 * q)
    cw = np.cos(w)
    if kind == 'peak':
        b = [1 + alpha * A, -2 * cw, 1 - alpha * A]
        a = [1 + alpha / A, -2 * cw, 1 - alpha / A]
    elif kind == 'lowshelf':
        sa = 2 * np.sqrt(A) * alpha
        b = [A * ((A + 1) - (A - 1) * cw + sa), 2 * A * ((A - 1) - (A + 1) * cw), A * ((A + 1) - (A - 1) * cw - sa)]
        a = [(A + 1) + (A - 1) * cw + sa, -2 * ((A - 1) + (A + 1) * cw), (A + 1) + (A - 1) * cw - sa]
    elif kind == 'highshelf':
        sa = 2 * np.sqrt(A) * alpha
        b = [A * ((A + 1) + (A - 1) * cw + sa), -2 * A * ((A - 1) + (A + 1) * cw), A * ((A + 1) + (A - 1) * cw - sa)]
        a = [(A + 1) - (A - 1) * cw + sa, 2 * ((A - 1) - (A + 1) * cw), (A + 1) - (A - 1) * cw - sa]
    elif kind == 'bandpass':  # constant 0 dB peak gain
        b = [alpha, 0, -alpha]
        a = [1 + alpha, -2 * cw, 1 - alpha]
    else:
        raise ValueError(kind)
    return np.array(b) / a[0], np.array(a) / a[0]


def peak_eq(x, f0, gain_db, q=1.0):
    b, a = _rbj('peak', f0, q, gain_db)
    return signal.lfilter(b, a, x, axis=0)


def low_shelf(x, f0, gain_db, q=0.707):
    b, a = _rbj('lowshelf', f0, q, gain_db)
    return signal.lfilter(b, a, x, axis=0)


def high_shelf(x, f0, gain_db, q=0.707):
    b, a = _rbj('highshelf', f0, q, gain_db)
    return signal.lfilter(b, a, x, axis=0)


def resonator(x, f0, q):
    b, a = _rbj('bandpass', f0, q)
    return signal.lfilter(b, a, x, axis=0)


def formants(x, spec):
    """Parallel resonators: spec = [(frequency, q, gain_db), ...] (a vocal tract or a body)."""
    y = np.zeros_like(x)
    for f, q, g in spec:
        y += resonator(x, f, q) * 10 ** (g / 20)
    return y


def onepole_lp(x, a):
    """y[n] = (1 - a) x[n] + a y[n-1]; a in 0..1 (bigger = darker)."""
    return signal.lfilter([1 - a], [1, -a], x, axis=0)


def svf(x, fc, q=0.707, mode='lp'):
    """Zavalishin TPT state-variable filter with a time-varying cutoff (array or scalar) and q: 'lp', 'bp', 'hp'."""
    n = len(x)
    fc = np.clip(_freq_array(fc, n), 10.0, SR * 0.45)
    qa = _freq_array(q, n)
    g = np.tan(np.pi * fc / SR)
    k = 1.0 / qa
    a1 = 1.0 / (1.0 + g * (g + k))
    a2 = g * a1
    a3 = g * a2
    out = np.empty(n)
    ic1 = ic2 = 0.0
    xs = x.tolist()
    a1l, a2l, a3l, kl = a1.tolist(), a2.tolist(), a3.tolist(), k.tolist()
    if mode == 'lp':
        for i in range(n):
            v3 = xs[i] - ic2
            v1 = a1l[i] * ic1 + a2l[i] * v3
            v2 = ic2 + a2l[i] * ic1 + a3l[i] * v3
            ic1 = 2 * v1 - ic1
            ic2 = 2 * v2 - ic2
            out[i] = v2
    elif mode == 'bp':
        for i in range(n):
            v3 = xs[i] - ic2
            v1 = a1l[i] * ic1 + a2l[i] * v3
            v2 = ic2 + a2l[i] * ic1 + a3l[i] * v3
            ic1 = 2 * v1 - ic1
            ic2 = 2 * v2 - ic2
            out[i] = v1
    else:
        for i in range(n):
            v3 = xs[i] - ic2
            v1 = a1l[i] * ic1 + a2l[i] * v3
            v2 = ic2 + a2l[i] * ic1 + a3l[i] * v3
            ic1 = 2 * v1 - ic1
            ic2 = 2 * v2 - ic2
            out[i] = xs[i] - kl[i] * v1 - v2
    return out


def comb_bank(x, freqs, feedback=0.7, damp=0.3):
    """Feedback combs tuned to freqs (metallic or tubular body); output summed."""
    y = np.zeros(len(x))
    for f in freqs:
        d = max(2, int(round(SR / f)))
        b = [1.0]
        a = np.zeros(d + 1)
        a[0] = 1.0
        a[d] = -feedback
        y += signal.lfilter(b, a, onepole_lp(x, damp))
    return y / max(1, len(freqs))


# ------------------------------------------------------------------------------------------------ effects
def saturate(x, drive=2.0):
    return np.tanh(drive * x) / np.tanh(drive)


def reverb(x, rt60=1.2, mix=0.3, predelay=0.02, damp=5000.0, rng=None, early=0.5, width=0.0, tail=None):
    """Convolution reverb with a synthetic impulse response (decaying noise, darker with time, early taps).
    width > 0 returns stereo (decorrelated left/right tails). The output is longer by the tail."""
    rng = rng if rng is not None else rng_of(1)
    tail = tail if tail is not None else rt60 * 1.1
    n = n_of(tail)
    t = np.arange(n) / SR

    def one_ir(r):
        ir = r.standard_normal(n) * np.exp(-6.91 * t / rt60)
        # frequency-dependent decay: split into 3 bands that decay at different rates
        lo = lowpass(ir, 600)
        hi = highpass(ir, 600)
        hi2 = hi * np.exp(-6.91 * t / (rt60 * 0.55))
        ir = lo + lowpass(hi2, damp)
        er = np.zeros(n)
        for _ in range(12):
            p = int(r.uniform(0.004, 0.09) * SR)
            if p < n:
                er[p] += r.uniform(0.2, 1.0) * r.choice([-1, 1])
        ir = ir / (np.sqrt(np.sum(ir ** 2)) + 1e-12) + early * lowpass(er, damp) * 0.15
        pd = n_of(predelay)
        return np.concatenate([np.zeros(pd), ir])

    mono_in = x if x.ndim == 1 else x.mean(axis=1)
    dry_len = len(mono_in)
    if width > 0:
        irl, irr = one_ir(rng), one_ir(rng_of(int(rng.integers(1 << 30))))
        wl = signal.fftconvolve(mono_in, irl)
        wr = signal.fftconvolve(mono_in, irr)
        mid, side = (wl + wr) / 2, (wl - wr) / 2
        wet = np.stack([mid + width * side, mid - width * side], axis=1)
        dry = x if x.ndim == 2 else np.stack([x, x], axis=1)
    else:
        wet = signal.fftconvolve(mono_in, one_ir(rng))
        dry = mono_in
    total = len(wet)
    out_dry = np.zeros((total,) + dry.shape[1:])
    out_dry[:dry_len] = dry
    # keep the wet level comparable to the dry one
    s_dry = np.sqrt(np.mean(dry ** 2)) + 1e-12
    s_wet = np.sqrt(np.mean(wet[:max(dry_len, 1)] ** 2)) + 1e-12
    wet = wet * (s_dry / s_wet)
    return np.sqrt(1 - mix) * out_dry + np.sqrt(mix) * wet


def delay(x, time, feedback=0.3, mix=0.3, damp=0.3, repeats=6):
    d = n_of(time)
    y = np.concatenate([x, np.zeros((d * repeats,) + x.shape[1:])])
    out = y.copy() * (1 - mix)
    tap = y.copy()
    g = mix
    for i in range(1, repeats + 1):
        tap = onepole_lp(np.concatenate([np.zeros((d,) + x.shape[1:]), tap[:-d]]), damp)
        out += g * tap
        g *= feedback
    return out


def chorus(x, depth=0.004, rate=0.6, mix=0.5, base=0.012, rng=None):
    """Modulated delay (also gives the slow warble of water)."""
    n = len(x)
    t = np.arange(n) / SR
    ph = rng.uniform(0, 6.28) if rng is not None else 0.0
    dly = (base + depth * np.sin(2 * np.pi * rate * t + ph)) * SR
    idx = np.arange(n) - dly
    i0 = np.clip(np.floor(idx).astype(int), 0, n - 1)
    fr = idx - np.floor(idx)
    i1 = np.clip(i0 + 1, 0, n - 1)
    wet = x[i0] * (1 - fr) + x[i1] * fr
    wet[idx < 0] = 0
    return (1 - mix) * x + mix * wet


def underwater(x, cutoff=1400.0, rng=None, wobble=0.6):
    """Muffles a sound as if heard under water: low-pass, a slow pitch warble, dense short reflections."""
    y = lowpass(x, cutoff, order=2)
    y = chorus(y, depth=0.0015, rate=wobble, mix=0.35, base=0.006, rng=rng)
    r = reverb(y, rt60=0.6, mix=0.25, predelay=0.004, damp=cutoff * 1.4, rng=rng, early=1.0)
    return lowpass(r, cutoff * 1.6, order=2)


def compress(x, threshold_db=-18.0, ratio=4.0, attack=0.005, release=0.12, makeup_db=0.0):
    m = np.abs(x) if x.ndim == 1 else np.max(np.abs(x), axis=1)
    aa = np.exp(-1.0 / (attack * SR))
    ar = np.exp(-1.0 / (release * SR))
    envl = np.empty(len(m))
    e = 0.0
    for i, v in enumerate(m.tolist()):
        e = aa * e + (1 - aa) * v if v > e else ar * e + (1 - ar) * v
        envl[i] = e
    db = 20 * np.log10(envl + 1e-9)
    over = np.maximum(0, db - threshold_db)
    gain_db = -over * (1 - 1 / ratio) + makeup_db
    g = 10 ** (gain_db / 20)
    return x * (g if x.ndim == 1 else g[:, None])


def limit(x, ceiling_db=-1.0, release=0.05):
    """Look-ahead peak limiter: the result never exceeds the ceiling."""
    c = 10 ** (ceiling_db / 20)
    m = np.abs(x) if x.ndim == 1 else np.max(np.abs(x), axis=1)
    need = np.minimum(1.0, c / np.maximum(m, 1e-12))
    look = n_of(0.003)
    from scipy.ndimage import minimum_filter1d
    g = minimum_filter1d(need, size=2 * look + 1)
    # smooth release (attack is the look-ahead window)
    a = np.exp(-1.0 / (release * SR))
    g = np.minimum(g, signal.lfilter([1 - a], [1, -a], g - 1.0) + 1.0)
    g = np.minimum(g, need)
    y = x * (g if x.ndim == 1 else g[:, None])
    return np.clip(y, -c, c)


def resample(x, factor):
    """Speeds a sound up by factor (pitch and tempo, like a tape): factor 0.5 = an octave lower, twice as long."""
    n = len(x)
    m = max(2, int(round(n / factor)))
    src = np.linspace(0, n - 1, m)
    if x.ndim == 1:
        return np.interp(src, np.arange(n), x)
    return np.stack([np.interp(src, np.arange(n), x[:, c]) for c in range(x.shape[1])], axis=1)


def stereo(x, width=0.3, rng=None, delay_ms=12.0):
    """Mono -> stereo with a little width (decorrelated copy)."""
    if x.ndim == 2:
        return x
    d = n_of(delay_ms / 1000)
    other = np.concatenate([np.zeros(d), x[:-d]]) if d < len(x) else x
    other = peak_eq(other, 2500, -2.0, 0.7)
    l = x + width * (other - x)
    r = x - width * (other - x)
    return np.stack([l, r], axis=1)


def pan(x, p):
    """Mono -> stereo, p in -1 (left) .. 1 (right), equal power."""
    a = (p + 1) * np.pi / 4
    return np.stack([x * np.cos(a), x * np.sin(a)], axis=1)


# ------------------------------------------------------------------------------------------------ loudness
def _kweight48(x):
    b1 = [1.53512485958697, -2.69169618940638, 1.19839281085285]
    a1 = [1.0, -1.69065929318241, 0.73248077421585]
    b2 = [1.0, -2.0, 1.0]
    a2 = [1.0, -1.99004745483398, 0.99007225036621]
    return signal.lfilter(b2, a2, signal.lfilter(b1, a1, x, axis=0), axis=0)


def lufs(x):
    """Integrated loudness (ITU BS.1770, gated); short sounds are measured over their whole length."""
    y = signal.resample_poly(x, 160, 147, axis=0)
    if y.ndim == 1:
        y = y[:, None]
    y = _kweight48(y)
    blk, hop = int(0.4 * 48000), int(0.1 * 48000)
    if len(y) < blk:
        ms = np.array([np.sum(np.mean(y ** 2, axis=0))])
    else:
        ms = np.array([np.sum(np.mean(y[i:i + blk] ** 2, axis=0)) for i in range(0, len(y) - blk + 1, hop)])
    l = -0.691 + 10 * np.log10(ms + 1e-15)
    g = ms[l > -70]
    if len(g) == 0:
        return -99.0
    rel = -0.691 + 10 * np.log10(np.mean(g)) - 10
    g2 = ms[(l > -70) & (l > rel)]
    return float(-0.691 + 10 * np.log10(np.mean(g2) + 1e-15))


def peak_db(x):
    return float(20 * np.log10(np.max(np.abs(x)) + 1e-12))


LAST_GAIN_REDUCTION = [0.0]


def master(x, loudness=-18.0, ceiling=-1.0, fin=0.002, fout=0.03, dc=True):
    """Final stage of every sound: remove DC/subsonics, set the loudness, limit the peaks, fade the ends.
    The peak reduction the limiter needed (dB) is left in LAST_GAIN_REDUCTION[0]."""
    y = highpass(x, 22, order=2) if dc else x
    y = y * 10 ** ((loudness - lufs(y)) / 20)
    LAST_GAIN_REDUCTION[0] = max(0.0, peak_db(y) - ceiling)
    y = limit(y, ceiling)
    return fade(y, fin, fout)


# ------------------------------------------------------------------------------------------------ files
def write_wav(x, path):
    pcm = (np.clip(x, -1, 1) * 32767).astype('<i2')
    with wave.open(path, 'wb') as w:
        w.setnchannels(1 if x.ndim == 1 else x.shape[1])
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm.tobytes())


def write_ogg(x, path, quality=4):
    """Ogg Vorbis (mono or stereo, 44.1 kHz) without metadata, reproducible."""
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        wav = os.path.join(tmp, 'x.wav')
        write_wav(x, wav)
        subprocess.run(['ffmpeg', '-v', 'error', '-y', '-i', wav, '-c:a', 'libvorbis', '-q:a', str(quality),
                        '-map_metadata', '-1', '-fflags', '+bitexact', '-flags:a', '+bitexact', path], check=True)
    return path


def read_audio(path, channels=1):
    raw = subprocess.run(['ffmpeg', '-v', 'error', '-i', path, '-f', 'f32le', '-ac', str(channels), '-ar', str(SR), '-'],
                         capture_output=True, check=True).stdout
    x = np.frombuffer(raw, dtype='<f4').astype(np.float64)
    return x.reshape(-1, channels) if channels > 1 else x


# ------------------------------------------------------------------------------------------------ preview
def spectrogram_image(x, width=600, height=160, fmax=12000.0):
    """PIL image of a log-frequency spectrogram (dark = quiet) plus the waveform envelope on top."""
    from PIL import Image
    m = x if x.ndim == 1 else x.mean(axis=1)
    nfft = 2048
    hop = max(64, len(m) // width)
    f, t, S = signal.stft(m, fs=SR, nperseg=nfft, noverlap=nfft - hop, boundary=None, padded=True)
    db = 20 * np.log10(np.abs(S) + 1e-9)
    db = np.clip((db - (db.max() - 70)) / 70, 0, 1) ** 1.3
    # log frequency axis 40 Hz .. fmax
    rows = np.geomspace(40, fmax, height)[::-1]
    fi = np.clip(np.searchsorted(f, rows), 0, len(f) - 1)
    img = db[fi, :]
    cols = np.linspace(0, img.shape[1] - 1, width).astype(int)
    img = img[:, cols]
    r = (np.clip(img * 2.2 - 0.25, 0, 1) * 255).astype(np.uint8)
    g = (np.clip(img * 2.0 - 1.0, 0, 1) * 255).astype(np.uint8)
    b = (np.clip(img * 3.0, 0, 1) * np.clip(1.6 - img * 2.0, 0.25, 1) * 255).astype(np.uint8)
    spec = Image.fromarray(np.stack([r, g, b], axis=2), 'RGB')
    # waveform strip
    wave_h = 40
    strip = np.zeros((wave_h, width, 3), np.uint8) + 20
    chunks = np.array_split(np.abs(m), width)
    for i, c in enumerate(chunks):
        v = float(np.max(c)) if len(c) else 0.0
        h = int(min(1.0, v) * (wave_h // 2 - 1))
        strip[wave_h // 2 - h:wave_h // 2 + h + 1, i] = (120, 200, 120) if v < 0.99 else (255, 80, 80)
    out = Image.new('RGB', (width, height + wave_h))
    out.paste(Image.fromarray(strip, 'RGB'), (0, 0))
    out.paste(spec, (0, wave_h))
    return out


def contact_sheet(items, path, cols=3, width=420, height=130):
    """items: [(label, signal)] -> one PNG with a spectrogram per sound (for checking a batch by eye)."""
    from PIL import Image, ImageDraw
    rows = (len(items) + cols - 1) // cols
    cell_h = height + 40 + 16
    sheet = Image.new('RGB', (cols * width, rows * cell_h), (0, 0, 0))
    d = ImageDraw.Draw(sheet)
    for k, (label, x) in enumerate(items):
        im = spectrogram_image(x, width - 6, height)
        cx, cy = (k % cols) * width, (k // cols) * cell_h
        sheet.paste(im, (cx + 3, cy + 16))
        info = '%s  %.2fs  %.1f LUFS  pk %.1f' % (label, len(x) / SR, lufs(x), peak_db(x))
        d.text((cx + 4, cy + 2), info, fill=(255, 255, 255))
    os.makedirs(os.path.dirname(path) or '.', exist_ok=True)
    sheet.save(path)
    return path


def clicks(x, ratio=8.0, floor_db=-50.0):
    """Times (s) of isolated high-frequency spikes (cuts, discontinuities): energy above 6 kHz in 2 ms frames
    that jumps far above its 60 ms neighbourhood. Wanted transients (snaps, cracks) show up too."""
    from scipy.ndimage import median_filter
    m = x if x.ndim == 1 else x.mean(axis=1)
    hp = highpass(m, 6000, 4)
    fr = n_of(0.002)
    k = len(hp) // fr
    if k < 8:
        return []
    e = np.sqrt(np.mean(hp[:k * fr].reshape(k, fr) ** 2, axis=1))
    loc = median_filter(e, size=31) + 1e-9
    peak = np.max(np.abs(m)) + 1e-12
    hits = np.nonzero((e / loc > ratio) & (20 * np.log10(e / peak + 1e-12) > floor_db))[0]
    res = []
    for h in hits:
        tt_ = h * fr / SR
        if not res or tt_ - res[-1] > 0.03:
            res.append(round(tt_, 3))
    return res
