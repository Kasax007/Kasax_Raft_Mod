"""Approximate evaluator for 26.3 density functions (numpy), used to preview the raft dimension terrain.

It re-implements the density function types the world module uses (noise, spline, add/mul/min/max, clamp, abs,
square, cache, interpolated (ignored), gradient, constants, references) with an ImprovedNoise-style Perlin noise
and the NormalNoise octave/normalisation scheme of 26.3. Seeds differ from the game, statistics and shapes match.
Run `python3 -m content.world.sim` (from tools/) to write preview maps into $SP/work/world/.
"""
import math
import zlib
import os
import sys

import numpy as np

GRAD = np.array([[1, 1, 0], [-1, 1, 0], [1, -1, 0], [-1, -1, 0], [1, 0, 1], [-1, 0, 1], [1, 0, -1], [-1, 0, -1],
                 [0, 1, 1], [0, -1, 1], [0, 1, -1], [0, -1, -1], [1, 1, 0], [0, -1, 1], [-1, 1, 0], [0, -1, -1]],
                dtype=np.float64)


class Perlin:
    def __init__(self, rng):
        self.xo, self.yo, self.zo = rng.random() * 256, rng.random() * 256, rng.random() * 256
        p = np.arange(256)
        rng.shuffle(p)
        self.p = np.concatenate([p, p]).astype(np.int64)

    def __call__(self, x, y, z):
        x = x + self.xo
        y = y + self.yo
        z = z + self.zo
        xi, yi, zi = np.floor(x), np.floor(y), np.floor(z)
        xf, yf, zf = x - xi, y - yi, z - zi
        X, Y, Z = xi.astype(np.int64) & 255, yi.astype(np.int64) & 255, zi.astype(np.int64) & 255
        p = self.p

        def g(h, dx, dy, dz):
            gr = GRAD[h & 15]
            return gr[..., 0] * dx + gr[..., 1] * dy + gr[..., 2] * dz

        a = p[X] + Y
        aa = p[a] + Z
        ab = p[a + 1] + Z
        b = p[X + 1] + Y
        ba = p[b] + Z
        bb = p[b + 1] + Z
        f = lambda t: t * t * t * (t * (t * 6 - 15) + 10)
        u, v, w = f(xf), f(yf), f(zf)
        lerp = lambda t, a_, b_: a_ + t * (b_ - a_)
        x1 = lerp(u, g(p[aa], xf, yf, zf), g(p[ba], xf - 1, yf, zf))
        x2 = lerp(u, g(p[ab], xf, yf - 1, zf), g(p[bb], xf - 1, yf - 1, zf))
        y1 = lerp(v, x1, x2)
        x3 = lerp(u, g(p[aa + 1], xf, yf, zf - 1), g(p[ba + 1], xf - 1, yf, zf - 1))
        x4 = lerp(u, g(p[ab + 1], xf, yf - 1, zf - 1), g(p[bb + 1], xf - 1, yf - 1, zf - 1))
        y2 = lerp(v, x3, x4)
        return lerp(w, y1, y2)


class NormalNoiseSim:
    """26.3 NormalNoise: octaves 2^(base+i), amplitude halving, two Perlin layers per octave, normalised so the
    standard deviation is target_amplitude/3."""

    def __init__(self, params, seed):
        rng = np.random.default_rng(seed)
        base = params['base_octave']
        n = params.get('octave_count', 1)
        amp0 = params.get('base_amplitude', 1.0)
        mods = params.get('amplitude_modifiers') or [1.0] * n
        norm = params.get('normalize', True)
        amp = amp0 * (0.5 ** -(n - 1) / (0.5 ** -n - 1.0) if norm else 1.0)
        freq = 2.0 ** base
        self.oct = []
        for i in range(n):
            if mods[i] != 0:
                self.oct.append((freq, amp * mods[i], Perlin(rng), Perlin(rng)))
            freq *= 2
            amp *= 0.5
        target = sum(abs(o[1]) for o in self.oct)
        dev = math.sqrt(sum((0.2702247831245211 * abs(o[1])) ** 2 for o in self.oct)) * math.sqrt(2)
        self.factor = (target / 3.0) / dev if dev else 0

    def __call__(self, x, y, z):
        r = 0
        for freq, amp, p1, p2 in self.oct:
            r = r + (p1(x * freq, y * freq, z * freq) + p2(x * freq * 1.0181268882175227, y * freq * 1.0181268882175227,
                                                           z * freq * 1.0181268882175227)) * amp * self.factor
        return r


class Evaluator:
    def __init__(self, noises, functions, seed=1234):
        """noises: id -> params dict; functions: id -> density function json (ids with namespace)."""
        self.noises = noises
        self.functions = functions
        self.seed = seed
        self._noise_cache = {}

    def noise(self, nid):
        if nid not in self._noise_cache:
            if nid not in self.noises:
                raise KeyError('unknown noise ' + nid)
            self._noise_cache[nid] = NormalNoiseSim(self.noises[nid], zlib.crc32(f'{nid}|{self.seed}'.encode()))
        return self._noise_cache[nid]

    def ev(self, df, x, y, z, memo):
        if isinstance(df, (int, float)):
            return np.full(np.broadcast(x, y, z).shape, float(df))
        if isinstance(df, str):
            if df in memo:
                return memo[df]
            if df not in self.functions:
                raise KeyError('unknown density function ' + df)
            r = self.ev(self.functions[df], x, y, z, memo)
            memo[df] = r
            return r
        t = df['type'].replace('minecraft:', '')
        e = lambda k: self.ev(df[k], x, y, z, memo)
        if t in ('cache', 'interpolated', 'blend_density'):
            return e('input')
        if t == 'noise':
            n = self.noise(df['noise'])
            return n(x * df['xz_scale'], y * df['y_scale'], z * df['xz_scale'])
        if t == 'add':
            return e('left') + e('right')
        if t == 'sub':
            return e('left') - e('right')
        if t == 'mul':
            return e('left') * e('right')
        if t == 'div':
            return e('left') / e('right')
        if t == 'min':
            return np.minimum(e('left'), e('right'))
        if t == 'max':
            return np.maximum(e('left'), e('right'))
        if t == 'abs':
            return np.abs(e('input'))
        if t == 'square':
            v = e('input'); return v * v
        if t == 'cube':
            v = e('input'); return v * v * v
        if t == 'negate':
            return -e('input')
        if t == 'half_negative':
            v = e('input'); return np.where(v > 0, v, v * 0.5)
        if t == 'quarter_negative':
            v = e('input'); return np.where(v > 0, v, v * 0.25)
        if t == 'squeeze':
            v = np.clip(e('input'), -1, 1); return v / 2 - v * v * v / 24
        if t == 'clamp':
            return np.clip(e('input'), df['min'], df['max'])
        if t == 'gradient':
            ax = {'x': x, 'y': y, 'z': z}[df['axis']]
            a0, a1, v0, v1 = df['from_coordinate'], df['to_coordinate'], df['from_value'], df['to_value']
            tt = np.clip((ax - a0) / (a1 - a0), 0, 1)
            return np.broadcast_to(v0 + (v1 - v0) * tt, np.broadcast(x, y, z).shape)
        if t == 'spline':
            return self.spline(df['spline'], x, y, z, memo)
        if t == 'range_choice':
            v = e('input')
            m = (v >= df['min_inclusive']) & (v < df['max_exclusive'])
            return np.where(m, e('when_in_range'), e('when_out_of_range'))
        if t == 'lerp':
            a = e('alpha'); return e('first') + a * (e('second') - e('first'))
        raise ValueError('unsupported type ' + t)

    def spline(self, sp, x, y, z, memo):
        if isinstance(sp, (int, float)):
            return np.full(np.broadcast(x, y, z).shape, float(sp))
        c = self.ev(sp['coordinate'], x, y, z, memo)
        pts = sp['points']
        locs = np.array([p['location'] for p in pts])
        vals = [self.spline(p['value'], x, y, z, memo) for p in pts]
        ders = [p['derivative'] for p in pts]
        res = np.zeros(c.shape)
        # below first / above last: linear extension
        lo = c < locs[0]
        res = np.where(lo, vals[0] + ders[0] * (c - locs[0]), res)
        hi = c >= locs[-1]
        res = np.where(hi, vals[-1] + ders[-1] * (c - locs[-1]), res)
        for i in range(len(pts) - 1):
            m = (c >= locs[i]) & (c < locs[i + 1])
            if not m.any():
                continue
            x1, x2 = locs[i], locs[i + 1]
            tt = (c - x1) / (x2 - x1)
            y1, y2 = vals[i], vals[i + 1]
            p1 = ders[i] * (x2 - x1) - (y2 - y1)
            p2 = -ders[i + 1] * (x2 - x1) + (y2 - y1)
            v = y1 + tt * (y2 - y1) + tt * (1 - tt) * (p1 + tt * (p2 - p1))
            res = np.where(m, v, res)
        return res
