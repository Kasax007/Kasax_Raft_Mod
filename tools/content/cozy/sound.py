"""Synthesised sounds of the cozy module (steel drum note, conch horn, rested chime), written as mono OGG.

Pure additive synthesis with numpy (no samples from anywhere else), encoded with ffmpeg/libvorbis.
"""
import os
import subprocess
import tempfile
import wave

import numpy as np

from bclib.paths import out

RATE = 44100


def _env(n, attack, tau):
    t = np.arange(n) / RATE
    a = np.clip(t / max(attack, 1e-4), 0, 1)
    return a * np.exp(-t / tau)


def steel_drum():
    """F#4 steel pan note: strong fundamental and octave, a short metallic ping on the attack."""
    n = int(RATE * 1.3)
    t = np.arange(n) / RATE
    f = 369.99
    s = (1.0 * np.sin(2 * np.pi * f * t) * _env(n, 0.003, 0.55)
         + 0.7 * np.sin(2 * np.pi * 2 * f * t + 0.3) * _env(n, 0.003, 0.32)
         + 0.25 * np.sin(2 * np.pi * 3 * f * t) * _env(n, 0.002, 0.18)
         + 0.12 * np.sin(2 * np.pi * 4.2 * f * t) * _env(n, 0.001, 0.05)
         + 0.05 * np.sin(2 * np.pi * 5.4 * f * t) * _env(n, 0.001, 0.03))
    return s


def conch_horn():
    """A low, breathy conch blast: harmonics on 196 Hz with a slow swell, a little vibrato and breath noise."""
    n = int(RATE * 2.2)
    t = np.arange(n) / RATE
    f = 196.0 * (1 + 0.006 * np.sin(2 * np.pi * 5.2 * t)) * (1 - 0.03 * np.exp(-t / 0.15))
    phase = 2 * np.pi * np.cumsum(f) / RATE
    s = sum((1.0 / k ** 1.3) * np.sin(k * phase) for k in range(1, 9))
    env = np.clip(t / 0.25, 0, 1) * np.clip((2.2 - t) / 0.6, 0, 1)
    rng = np.random.default_rng(3)
    noise = np.convolve(rng.standard_normal(n), np.ones(30) / 30, mode='same') * 0.08
    return (s + noise) * env


def rested_chime():
    """Two soft bell tones (C6, G6) - the 'ahh, cozy' moment."""
    n = int(RATE * 1.4)
    t = np.arange(n) / RATE
    out_ = np.zeros(n)
    for start, f in ((0.0, 1046.5), (0.18, 1567.98)):
        k = int(start * RATE)
        tt = t[: n - k]
        tone = (np.sin(2 * np.pi * f * tt) + 0.3 * np.sin(2 * np.pi * 2.76 * f * tt) * np.exp(-tt / 0.08))
        out_[k:] += tone * _env(n - k, 0.004, 0.45) * 0.5
    return out_


def _write(module, rel, signal):
    signal = signal / max(1e-6, np.max(np.abs(signal))) * 0.85
    pcm = (signal * 32767).astype(np.int16)
    dest = out(module, 'assets', 'raft', 'sounds', *(rel + '.ogg').split('/'))
    with tempfile.NamedTemporaryFile(suffix='.wav', delete=False) as tmp:
        wav = tmp.name
    with wave.open(wav, 'wb') as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(pcm.tobytes())
    subprocess.run(['ffmpeg', '-v', 'error', '-y', '-i', wav, '-c:a', 'libvorbis', '-q:a', '4', dest], check=True)
    os.remove(wav)
    return 'raft:' + rel


def build(module):
    return {
        'block.steel_drum.note': {'sounds': [_write(module, 'cozy/steel_drum', steel_drum())],
                                  'subtitle': 'subtitles.raft.block.steel_drum.note'},
        'block.conch_shell.blow': {'sounds': [{'name': _write(module, 'cozy/conch_horn', conch_horn()),
                                               'attenuation_distance': 64}],
                                   'subtitle': 'subtitles.raft.block.conch_shell.blow'},
        'cozy.rested': {'sounds': [_write(module, 'cozy/rested', rested_chime())], 'subtitle': 'subtitles.raft.cozy.rested'},
    }
