"""Synthesised whale sounds (mono, 44.1 kHz, Ogg Vorbis): three songs of gliding moans and whoops in an underwater
echo, and the blowhole spout (a breathy burst with spray). Needs numpy and ffmpeg (libvorbis)."""
import os
import subprocess
import tempfile
import wave

import numpy as np

from bclib.paths import out

SR = 44100
M = 'marine'


def _lowpass(x, fc):
    a = np.exp(-2 * np.pi * fc / SR)
    y = np.empty_like(x)
    acc = 0.0
    for i in range(len(x)):
        acc = (1 - a) * x[i] + a * acc
        y[i] = acc
    return y


def _env(n, attack, release):
    e = np.ones(n)
    a, r = int(attack * SR), int(release * SR)
    if a > 0:
        t = np.linspace(0, 1, a)
        e[:a] = t * t * (3 - 2 * t)
    if r > 0:
        t = np.linspace(1, 0, r)
        e[-r:] *= t * t * (3 - 2 * t)
    return e


def _moan(dur, f0, f1, shape=1.0, vib=0.02, growl=0.0, rng=None):
    """One gliding call: frequency from f0 to f1 (shape > 1 = late glide), vibrato, soft harmonics."""
    n = int(dur * SR)
    t = np.arange(n) / SR
    u = (t / dur) ** shape
    f = f0 + (f1 - f0) * (u * u * (3 - 2 * u))
    f = f * (1 + vib * np.sin(2 * np.pi * 5.3 * t + rng.uniform(0, 6)))
    phase = 2 * np.pi * np.cumsum(f) / SR
    sig = np.sin(phase) + 0.42 * np.sin(2 * phase + 0.3) + 0.18 * np.sin(3 * phase + 1.1) + 0.07 * np.sin(4 * phase)
    if growl:
        sig *= 1 + growl * np.sin(2 * np.pi * 28 * t)       # rough, pulsed groan
    sig *= _env(n, min(0.3, dur * 0.3), min(0.6, dur * 0.45))
    sig *= 0.85 + 0.15 * np.sin(2 * np.pi * 0.9 * t)
    return sig


def _place(buf, sig, start):
    i = int(start * SR)
    j = min(len(buf), i + len(sig))
    buf[i:j] += sig[:j - i]


def _underwater(x):
    x = _lowpass(x, 2600.0)
    y = x.copy()
    for delay, gain in ((0.113, 0.34), (0.171, 0.27), (0.237, 0.22), (0.313, 0.17), (0.419, 0.12)):
        d = int(delay * SR)
        y[d:] += gain * x[:-d]
    y = _lowpass(y, 3200.0)
    return y


def _normalise(x, peak=0.7):
    m = np.max(np.abs(x)) or 1.0
    return x / m * peak


def _write_ogg(sig, rel):
    """rel: 'sounds/marine/whale_song1' -> assets/raft/sounds/marine/whale_song1.ogg"""
    path = out(M, 'assets', 'raft', *(rel + '.ogg').split('/'))
    pcm = (np.clip(sig, -1, 1) * 32767).astype('<i2')
    with tempfile.TemporaryDirectory() as tmp:
        wav = os.path.join(tmp, 'x.wav')
        with wave.open(wav, 'wb') as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(SR)
            w.writeframes(pcm.tobytes())
        subprocess.run(['ffmpeg', '-v', 'error', '-y', '-i', wav, '-ac', '1', '-ar', str(SR), '-c:a', 'libvorbis',
                        '-q:a', '4', '-map_metadata', '-1', '-fflags', '+bitexact', path], check=True)
    return path


SONGS = [
    # (start, duration, f0, f1, shape, vibrato, growl)
    [(0.0, 1.7, 150, 330, 1.0, 0.02, 0), (1.9, 1.3, 430, 250, 0.8, 0.025, 0), (3.5, 2.3, 118, 92, 1.0, 0.01, 0.35)],
    [(0.0, 0.55, 220, 470, 1.0, 0.01, 0), (0.7, 0.55, 230, 500, 1.0, 0.01, 0), (1.4, 0.6, 240, 520, 1.0, 0.01, 0),
     (2.3, 2.8, 380, 140, 1.3, 0.03, 0)],
    [(0.0, 2.2, 95, 135, 1.0, 0.01, 0.4), (2.4, 1.6, 520, 610, 0.7, 0.035, 0), (4.2, 1.4, 600, 300, 1.0, 0.03, 0)],
]


def whale_songs():
    files = []
    for k, units in enumerate(SONGS):
        rng = np.random.default_rng(100 + k)
        length = max(s + d for s, d, *_ in units) + 1.2
        buf = np.zeros(int(length * SR))
        for (start, dur, f0, f1, shape, vib, growl) in units:
            _place(buf, _moan(dur, f0, f1, shape, vib, growl, rng) * 0.6, start)
        buf = _normalise(_underwater(buf), 0.72)
        _write_ogg(buf, 'sounds/marine/whale_song%d' % (k + 1))
        files.append('marine/whale_song%d' % (k + 1))
    return files


def whale_spouts():
    files = []
    for k in range(2):
        rng = np.random.default_rng(200 + k)
        dur = 1.9 + 0.2 * k
        n = int(dur * SR)
        t = np.arange(n) / SR
        noise = rng.standard_normal(n)
        hiss = _lowpass(noise, 3800.0) - _lowpass(noise, 700.0)            # breathy band
        rumble = _lowpass(rng.standard_normal(n), 180.0) * 6.0               # the deep exhale
        env = np.minimum(1.0, t / 0.05) * np.exp(-np.maximum(0, t - 0.12) * 2.6)
        sig = (hiss * 1.0 + rumble * 0.8) * env
        # spray: little droplets landing on the water after the blow
        for _ in range(26):
            st = rng.uniform(0.45, dur - 0.15)
            f = rng.uniform(900, 2400)
            m = int(0.03 * SR)
            tt = np.arange(m) / SR
            drop = np.sin(2 * np.pi * f * tt * (1 + 2.5 * tt)) * np.exp(-tt * 140) * rng.uniform(0.05, 0.16)
            _place(sig, drop, st)
        sig = _normalise(_lowpass(sig, 6000.0), 0.75)
        _write_ogg(sig, 'sounds/marine/whale_spout%d' % (k + 1))
        files.append('marine/whale_spout%d' % (k + 1))
    return files


def generate():
    return whale_songs(), whale_spouts()
