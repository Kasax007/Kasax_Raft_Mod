"""Noise parameters: 26.3 (base_octave, base_amplitude, octave_count, amplitude_modifiers) -> 1.21.1 (firstOctave,
amplitudes). The newer game scales a noise by base_amplitude; for the amplitudes of 1.21.1 that keep the same
strength, the vanilla 'parity' base amplitude (which reproduces 1.21.1's noise exactly) is computed and the
amplitudes are scaled by base_amplitude / parity."""
import math


def _modifiers(n):
    count = n.get('octave_count', 1)
    mods = list(n.get('amplitude_modifiers', []))
    if not mods:
        mods = [1.0] * count
    return count, mods


def _octaves(base_amp, count, mods):
    amp = base_amp * math.pow(0.5, -(count - 1)) / (math.pow(0.5, -count) - 1.0)
    out = []
    for i in range(count):
        m = mods[i] if i < len(mods) else 1.0
        if m != 0.0:
            out.append(amp * m)
        amp *= 0.5
    return out


def _new_factor(octaves):
    target = sum(abs(a) for a in octaves)
    dev = math.sqrt(sum((0.2702247831245211 * abs(a)) ** 2 for a in octaves))
    if dev == 0:
        return 0.0
    return target / 3.0 / (dev * math.sqrt(2.0))


def _parity_factor(base_amp, count, mods):
    idx = [i for i in range(count) if (mods[i] if i < len(mods) else 1.0) != 0.0]
    span = max(idx) - min(idx)
    return base_amp * 0.5 / 3.0 / (0.1 * (1.0 + 1.0 / (span + 1)))


def parity_base_amplitude(count, mods):
    octaves = _octaves(1.0, count, mods)
    new = _new_factor(octaves)
    if new == 0.0:
        return 1.0
    return _parity_factor(1.0, count, mods) / new


def noise(n):
    if 'firstOctave' in n:
        return n
    count, mods = _modifiers(n)
    base = n.get('base_amplitude', 1.0)
    k = base / parity_base_amplitude(count, mods)
    return {'firstOctave': n['base_octave'], 'amplitudes': [round(m * k, 9) for m in mods]}
