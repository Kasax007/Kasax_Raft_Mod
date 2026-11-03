"""Synthesised radio static for the Expedition Radio (mono, 44.1 kHz, Ogg Vorbis): band-limited hiss with crackles,
a slow tuning whistle and a faint warble of distant voices. Needs numpy and ffmpeg (libvorbis)."""
import os
import subprocess
import tempfile
import wave

import numpy as np

from bclib.paths import out

SR = 44100
M = 'story'


def _bandpass(x, lo, hi):
    f = np.fft.rfft(x)
    freqs = np.fft.rfftfreq(len(x), 1 / SR)
    f[(freqs < lo) | (freqs > hi)] = 0
    return np.fft.irfft(f, len(x))


def _static(seed, dur):
    rng = np.random.default_rng(seed)
    n = int(dur * SR)
    t = np.arange(n) / SR
    hiss = _bandpass(rng.standard_normal(n), 400, 5200)
    hiss /= np.max(np.abs(hiss)) + 1e-9
    # slow fading of the hiss like a drifting signal
    fade = 0.55 + 0.25 * np.sin(2 * np.pi * (0.7 + rng.random()) * t + rng.random() * 6)
    sig = hiss * fade * 0.5
    # crackles: short clicks with random polarity
    for _ in range(int(dur * 28)):
        p = rng.integers(0, n - 400)
        ln = rng.integers(30, 260)
        sig[p:p + ln] += rng.choice([-1, 1]) * rng.uniform(0.3, 0.9) * np.exp(-np.arange(ln) / (ln / 4))
    # tuning whistle gliding through
    f0, f1 = rng.uniform(900, 1400), rng.uniform(500, 2200)
    freq = f0 + (f1 - f0) * (t / dur) ** 2
    phase = 2 * np.pi * np.cumsum(freq) / SR
    sig += 0.07 * np.sin(phase) * np.sin(np.pi * t / dur)
    # distant voice-like warble (formant band, amplitude modulated at syllable rate)
    voice = _bandpass(rng.standard_normal(n), 300, 1100)
    voice /= np.max(np.abs(voice)) + 1e-9
    syll = np.clip(np.sin(2 * np.pi * rng.uniform(3, 5) * t), 0, 1) ** 2
    sig += 0.18 * voice * syll * (0.5 + 0.5 * np.sin(np.pi * t / dur))
    env = np.minimum(1, t / 0.04) * np.minimum(1, (dur - t) / 0.25)
    sig *= env
    sig /= np.max(np.abs(sig)) + 1e-9
    return (sig * 0.8 * 32767).astype(np.int16)


def _write_ogg(samples, rel):
    path = out(M, 'assets', 'raft', 'sounds', *(rel + '.ogg').split('/'))
    with tempfile.TemporaryDirectory() as tmp:
        wav = os.path.join(tmp, 'x.wav')
        with wave.open(wav, 'wb') as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(SR)
            w.writeframes(samples.tobytes())
        subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', wav, '-c:a', 'libvorbis', '-q:a', '4', path],
                       check=True)
    return path


def generate():
    from bclib import assets as A
    for i in range(3):
        _write_ogg(_static(100 + i, 1.6 + 0.3 * i), f'story/radio_static{i + 1}')
    A.sounds(M, {'radio_static': {'sounds': [f'raft:story/radio_static{i + 1}' for i in range(3)],
                                  'subtitle': 'subtitles.raft.radio_static'}})
