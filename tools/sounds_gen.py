"""
Звуки и музыка мода — синтез с нуля (numpy), без чужих записей. Каждый трек — бесшовная петля.
  music/boss_battle.ogg   — бой с боссом (гули, CCG, полугули, стражи глубин): ре минор, 140 ударов в минуту
  music/style_duel.ogg    — поединок с боссом стиля: тайко, пентатоника, флейта, 120 ударов
  music/ancient_ghoul.ogg — Древний гуль и Алый Монарх: хор, литавры, тритон, 96 ударов
  music/deep_ward.ogg     — Глубины 24-го района: гул, капли, далёкий металл, сердцебиение
  sfx/smoke.ogg, sfx/death_mark.ogg, sfx/execution.ogg, sfx/clown_laugh.ogg — эффекты стилей
Запуск: python3 sounds_gen.py <папка assets/tokyoghoulrpg/sounds>
Нужны numpy, scipy и ffmpeg (кодек libvorbis).
"""
import os
import subprocess
import sys
import tempfile

import numpy as np
from scipy.io import wavfile
from scipy.signal import butter, fftconvolve, lfilter, sosfilt

SR = 44100
RNG = np.random.default_rng(2477)


# ------------------------------------------------------------------ основа
def midi(n):
    return 440.0 * 2 ** ((n - 69) / 12)


def tt(dur):
    return np.arange(int(dur * SR)) / SR


def env(n, a=0.01, d=0.1, s=0.7, r=0.2):
    """ADSR на n сэмплов (r — затухание в конце внутри n)."""
    e = np.full(n, s, dtype=np.float64)
    ia, idd, ir = int(a * SR), int(d * SR), int(r * SR)
    ia = min(ia, n)
    e[:ia] = np.linspace(0, 1, ia, endpoint=False)
    j = min(n, ia + idd)
    e[ia:j] = np.linspace(1, s, j - ia, endpoint=False)
    if ir > 0:
        k = max(0, n - ir)
        e[k:] *= np.linspace(1, 0, n - k)
    return e


def saw(freq, dur, harmonics=14, detune=0.0):
    t = tt(dur)
    out = np.zeros_like(t)
    for h in range(1, harmonics + 1):
        if freq * h > SR / 2.2:
            break
        out += np.sin(2 * np.pi * freq * h * (1 + detune) * t + h) / h
    return out * 0.55


def square(freq, dur, harmonics=9):
    t = tt(dur)
    out = np.zeros_like(t)
    for h in range(1, 2 * harmonics, 2):
        if freq * h > SR / 2.2:
            break
        out += np.sin(2 * np.pi * freq * h * t) / h
    return out * 0.7


def sine(freq, dur, vib=0.0, vib_rate=5.0):
    t = tt(dur)
    phase = 2 * np.pi * freq * t + (vib * freq / vib_rate) * np.sin(2 * np.pi * vib_rate * t) * (t > 0.25)
    return np.sin(phase)


def noise(dur):
    return RNG.uniform(-1, 1, int(dur * SR))


def lp(x, cutoff, order=2):
    sos = butter(order, min(cutoff, SR / 2.1), "low", fs=SR, output="sos")
    return sosfilt(sos, x)


def hp(x, cutoff, order=2):
    sos = butter(order, cutoff, "high", fs=SR, output="sos")
    return sosfilt(sos, x)


def bp(x, lo, hi, order=2):
    sos = butter(order, [lo, min(hi, SR / 2.1)], "band", fs=SR, output="sos")
    return sosfilt(sos, x)


def reverb(x, seconds=2.0, wet=0.3, tone=5000):
    n = int(seconds * SR)
    ir = RNG.normal(0, 1, n) * np.exp(-np.linspace(0, 7, n))
    ir = lp(ir, tone)
    ir /= np.sqrt(np.sum(ir ** 2))
    tail = fftconvolve(x, ir)[: len(x)]
    return x * (1 - wet) + tail * wet


class Track:
    """Петля длиной length секунд плюс хвост; хвост потом складывается в начало (бесшовный повтор)."""

    def __init__(self, length, tail=4.0, stereo=True):
        self.length = length
        self.n = int(length * SR)
        self.buf = np.zeros((self.n + int(tail * SR), 2))

    def add(self, sound, at, gain=1.0, pan=0.0):
        i = int(at * SR)
        if i >= len(self.buf):
            return
        s = sound[: len(self.buf) - i] * gain
        left, right = np.cos((pan + 1) * np.pi / 4), np.sin((pan + 1) * np.pi / 4)
        self.buf[i:i + len(s), 0] += s * left * 1.41
        self.buf[i:i + len(s), 1] += s * right * 1.41

    def bus(self, other, gain=1.0):
        self.buf += other.buf * gain

    def fx(self, fn):
        for c in range(2):
            self.buf[:, c] = fn(self.buf[:, c])

    def loop(self):
        tail = self.buf[self.n:]
        out = self.buf[: self.n].copy()
        out[: len(tail)] += tail
        return out


def master(x, peak=0.89):
    x = np.tanh(x / (np.max(np.abs(x)) + 1e-9) * 1.4)
    return x / np.max(np.abs(x)) * peak


def save_ogg(x, path, quality=4):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    data = (np.clip(x, -1, 1) * 32767).astype(np.int16)
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
        wavfile.write(tmp.name, SR, data)
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", tmp.name, "-c:a", "libvorbis",
                        "-q:a", str(quality), path], check=True)
    os.unlink(tmp.name)


# ------------------------------------------------------------------ ударные
def kick(dur=0.45):
    t = tt(dur)
    f = 45 + 95 * np.exp(-t * 28)
    body = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 7)
    click = hp(noise(dur), 2000) * np.exp(-t * 180) * 0.4
    return body + click


def snare(dur=0.3):
    t = tt(dur)
    tone = np.sin(2 * np.pi * 185 * t) * np.exp(-t * 22) * 0.6
    rattle = bp(noise(dur), 1500, 9000) * np.exp(-t * 16)
    return tone + rattle * 0.9


def hat(dur=0.06, open_=False):
    d = 0.3 if open_ else dur
    t = tt(d)
    return hp(noise(d), 7000) * np.exp(-t * (9 if open_ else 70)) * 0.5


def taiko(dur=1.2, pitch=72):
    t = tt(dur)
    f = pitch * (1 + 0.5 * np.exp(-t * 30))
    body = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 3.2)
    skin = lp(noise(dur), 900) * np.exp(-t * 25) * 0.8
    return body + skin


def timpani(dur=2.2, note=36):
    t = tt(dur)
    f0 = midi(note)
    out = np.zeros_like(t)
    for k, (ratio, amp) in enumerate(((1.0, 1.0), (1.5, 0.5), (1.98, 0.35), (2.44, 0.2))):
        out += np.sin(2 * np.pi * f0 * ratio * t + k) * amp * np.exp(-t * (1.8 + k))
    hit = lp(noise(dur), 700) * np.exp(-t * 30)
    return out + hit * 0.6


def clank(dur=1.5):
    """Далёкий удар по железу: неровные обертоны."""
    t = tt(dur)
    out = np.zeros_like(t)
    for ratio, amp, dec in ((1.0, 1.0, 3), (2.76, 0.6, 5), (5.4, 0.4, 8), (8.93, 0.25, 12)):
        out += np.sin(2 * np.pi * 310 * ratio * t) * amp * np.exp(-t * dec)
    return out * np.exp(-t * 0.5)


# ------------------------------------------------------------------ инструменты
def pluck(freq, dur):
    """Щипок: пила с быстро закрывающимся фильтром."""
    x = saw(freq, dur, harmonics=18)
    t = tt(dur)
    bright = lp(x, 2600) * np.exp(-t * 6)
    return bright * env(len(t), 0.003, 0.05, 1.0, 0.04)


def pad(freqs, dur, cutoff=1800, detune=0.004):
    n = int(dur * SR)
    out = np.zeros(n)
    for f in freqs:
        for d in (-detune, 0.0, detune):
            out += saw(f, dur, harmonics=10, detune=d)[:n]
    out = lp(out, cutoff)
    return out * env(n, 0.6, 0.4, 0.85, 0.8) / (len(freqs) * 3)


def bass(freq, dur):
    n = int(dur * SR)
    x = saw(freq, dur, harmonics=10)[:n] + sine(freq / 2, dur)[:n] * 0.6
    return lp(x, 600) * env(n, 0.005, 0.1, 0.8, 0.05)


def flute(freq, dur):
    n = int(dur * SR)
    x = sine(freq, dur, vib=0.012, vib_rate=5.5)[:n] + 0.18 * sine(freq * 2, dur)[:n]
    breath = bp(noise(dur), freq * 0.8, freq * 3)[:n] * 0.12
    return (x + breath) * env(n, 0.08, 0.2, 0.75, 0.25)


def choir(freqs, dur, vowel="a"):
    """Хор: пилы, пропущенные через форманты гласной."""
    formants = {"a": ((800, 1150), (1150, 1500), (2800, 3100)), "o": ((400, 600), (750, 950), (2400, 2700))}[vowel]
    n = int(dur * SR)
    src = np.zeros(n)
    for f in freqs:
        for d in (-0.006, -0.002, 0.003, 0.007):
            src += saw(f, dur, harmonics=24, detune=d)[:n]
    out = sum(bp(src, lo, hi) * g for (lo, hi), g in zip(formants, (1.0, 0.6, 0.25)))
    return out * env(n, 0.9, 0.5, 0.85, 1.0) / (len(freqs) * 4)


# ------------------------------------------------------------------ треки
def boss_battle():
    bpm = 140
    beat = 60 / bpm
    bar = beat * 4
    bars = 32
    tr = Track(bars * bar)
    # i - VI - VII - V в ре миноре (Dm Bb C A), по 2 такта
    prog = [(50, (62, 65, 69)), (46, (58, 62, 65)), (48, (60, 64, 67)), (45, (57, 61, 64))]
    drums, music = Track(bars * bar), Track(bars * bar)
    for b in range(bars):
        root, chord = prog[(b // 2) % 4]
        t0 = b * bar
        intense = b >= 8
        # ударные
        for i in range(8):
            at = t0 + i * beat / 2
            if i in (0, 3, 4) or (intense and i == 7 and b % 2):
                drums.add(kick(), at, 0.9)
            if i in (2, 6):
                drums.add(snare(), at, 0.55, 0.05)
            drums.add(hat(open_=(i == 7 and b % 4 == 3)), at, 0.22 if i % 2 else 0.3, 0.3)
        if b % 8 == 7:
            for k in range(4):
                drums.add(snare(), t0 + 3 * beat + k * beat / 4, 0.3 + 0.08 * k, -0.1)
        # бас: восьмые с октавным скачком
        for i in range(8):
            f = midi(root - 12 + (12 if i in (3, 6) else 0))
            music.add(bass(f, beat / 2 * 0.9), t0 + i * beat / 2, 0.55)
        # пад
        if b % 2 == 0:
            music.add(pad([midi(n) for n in chord], bar * 2 + 0.4, cutoff=1400 if intense else 900), t0, 0.35)
        # арпеджио (с 9-го такта)
        if intense:
            notes = list(chord) + [chord[0] + 12, chord[1] + 12, chord[2], chord[1]]
            for i in range(16):
                n = notes[i % len(notes)] + 12
                music.add(pluck(midi(n), beat / 4 * 0.95), t0 + i * beat / 4, 0.16, -0.35 if i % 2 else 0.35)
        # тема (такты 17-32): простая мелодия по ступеням аккорда
        if b >= 16:
            melody = [chord[2] + 12, chord[1] + 12, chord[0] + 12, chord[1] + 12]
            for i, n in enumerate(melody):
                music.add(square(midi(n), beat * 0.9) * env(int(beat * 0.9 * SR), 0.01, 0.1, 0.6, 0.1),
                          t0 + i * beat, 0.09, 0.0)
    drums.fx(lambda x: reverb(x, 1.0, 0.12))
    music.fx(lambda x: reverb(x, 2.2, 0.28))
    tr.bus(drums)
    tr.bus(music)
    return master(tr.loop())


def style_duel():
    bpm = 120
    beat = 60 / bpm
    bar = beat * 4
    bars = 32
    tr = Track(bars * bar)
    drums, music = Track(bars * bar), Track(bars * bar)
    # ми минорная пентатоника: E G A B D
    scale = [52, 55, 57, 59, 62, 64, 67, 69, 71, 74, 76]
    roots = [40, 40, 43, 38, 40, 45, 43, 47]
    phrase = [7, 6, 4, 6, 7, 9, 8, 7, 6, 4, 3, 4, 6, 4, 3, 1]
    for b in range(bars):
        t0 = b * bar
        root = roots[b % 8]
        # тайко: тяжёлые удары на 1 и 3, перекличка на 4
        drums.add(taiko(pitch=66), t0, 1.0, -0.1)
        drums.add(taiko(pitch=78), t0 + 2 * beat, 0.7, 0.15)
        if b % 2:
            drums.add(taiko(0.6, pitch=110), t0 + 3 * beat, 0.45, 0.3)
            drums.add(taiko(0.6, pitch=110), t0 + 3.5 * beat, 0.5, -0.3)
        for i in range(8):
            drums.add(hp(noise(0.03), 5000) * np.exp(-tt(0.03) * 90) * 0.4, t0 + i * beat / 2, 0.5, 0.4)
        # низкий бурдон и квинта
        if b % 2 == 0:
            music.add(pad([midi(root), midi(root + 7), midi(root + 12)], bar * 2 + 0.5, cutoff=700), t0, 0.4)
        # остинато щипками (как кото)
        if b >= 4:
            for i in range(8):
                n = scale[(phrase[(b * 8 + i) % 16] + (b // 8)) % len(scale)] - 12
                music.add(pluck(midi(n), beat / 2), t0 + i * beat / 2, 0.15, 0.4 if i % 2 else -0.2)
        # флейта: длинные ноты, вторая половина петли
        if b >= 12 and b % 2 == 0:
            line = [(9, 1.5), (8, 0.5), (7, 1.0), (6, 1.0), (7, 2.0), (4, 2.0)]
            at = t0
            shift = 0 if (b // 4) % 2 == 0 else -2
            for idx, ln in line:
                music.add(flute(midi(scale[idx + shift]), beat * ln * 0.98), at, 0.2, 0.0)
                at += beat * ln
    drums.fx(lambda x: reverb(x, 1.6, 0.2, tone=3000))
    music.fx(lambda x: reverb(x, 3.0, 0.35))
    tr.bus(drums)
    tr.bus(music)
    return master(tr.loop())


def ancient_ghoul():
    bpm = 96
    beat = 60 / bpm
    bar = beat * 4
    bars = 24
    tr = Track(bars * bar, tail=6.0)
    drums, music = Track(bars * bar, tail=6.0), Track(bars * bar, tail=6.0)
    # до минор: Cm - Gb (тритон!) - Ab - G
    prog = [(36, (60, 63, 67)), (42, (61, 66, 70)), (44, (60, 63, 68)), (43, (59, 62, 67))]
    for b in range(bars):
        t0 = b * bar
        root, chord = prog[(b // 2) % 4]
        # литавры и большой барабан
        drums.add(timpani(note=root - 12 if root > 40 else root), t0, 0.9)
        drums.add(kick(0.6), t0 + 1.5 * beat, 0.6)
        drums.add(timpani(1.4, note=root), t0 + 2 * beat, 0.6)
        if b % 4 == 3:
            for k in range(6):
                drums.add(timpani(0.5, note=root), t0 + 3 * beat + k * beat / 6, 0.25 + 0.06 * k)
        drums.add(snare(0.4) * 0.6, t0 + 3 * beat, 0.25)
        # хор
        if b % 2 == 0:
            music.add(choir([midi(n) for n in chord], bar * 2 + 1.0, "a" if (b // 4) % 2 else "o"), t0, 0.7)
            music.add(pad([midi(root), midi(root + 12)], bar * 2 + 1.0, cutoff=500), t0, 0.5)
        # низкие струнные: тревожные восьмые на последней трети
        if b >= 8:
            for i in range(8):
                f = midi(root + (1 if i % 4 == 3 else 0))
                music.add(lp(saw(f, beat / 2 * 0.9), 700) * env(int(beat / 2 * 0.9 * SR), 0.01, 0.05, 0.7, 0.05),
                          t0 + i * beat / 2, 0.3, -0.2)
        # колокол на первую долю каждые 4 такта
        if b % 4 == 0:
            music.add(clank(3.0) * 0.5, t0, 0.35, 0.3)
    drums.fx(lambda x: reverb(x, 2.5, 0.3, tone=2500))
    music.fx(lambda x: reverb(x, 4.0, 0.4))
    tr.bus(drums)
    tr.bus(music)
    return master(tr.loop())


def deep_ward():
    length = 64.0
    tr = Track(length, tail=8.0)
    # гул: два низких тона с медленным биением и фильтр, который «дышит»
    t = tt(length + 8.0)
    drone = (np.sin(2 * np.pi * 41.2 * t) + 0.7 * np.sin(2 * np.pi * 41.9 * t + 1)
             + 0.35 * np.sin(2 * np.pi * 61.7 * t) + 0.2 * saw(82.4, length + 8.0, harmonics=8)[: len(t)])
    wind = lp(noise(length + 8.0), 400) * (0.5 + 0.5 * np.sin(2 * np.pi * t / 16.0)) * 1.4
    tr.add(drone * 0.45 + wind * 0.25, 0.0, 1.0)
    # капли
    for _ in range(46):
        at = RNG.uniform(0, length)
        f = RNG.uniform(900, 2200)
        d = tt(0.25)
        drip = np.sin(2 * np.pi * (f + 900 * np.exp(-d * 40)) * d) * np.exp(-d * 30)
        tr.add(drip, at, RNG.uniform(0.08, 0.22), RNG.uniform(-0.9, 0.9))
    # далёкий металл
    for at in (5.0, 19.5, 33.0, 47.5, 58.0):
        tr.add(lp(clank(2.5), 1800), at + RNG.uniform(-1, 1), 0.22, RNG.uniform(-0.7, 0.7))
    # сердцебиение раз в 16 секунд (4 удара)
    for start in (10.0, 42.0):
        for k in range(4):
            beat_at = start + k * 0.95
            tr.add(lp(kick(0.4), 160), beat_at, 0.5)
            tr.add(lp(kick(0.35), 160), beat_at + 0.28, 0.32)
    tr.fx(lambda x: reverb(x, 5.0, 0.5, tone=1800))
    return master(tr.loop(), peak=0.7)


# ------------------------------------------------------------------ эффекты
def sfx_smoke():
    d = 1.1
    t = tt(d)
    x = bp(noise(d), 300, 2500) * np.exp(-t * 3.5) * env(len(t), 0.02, 0.1, 0.9, 0.3)
    nz = noise(d)
    w = np.exp(-t * 4)
    sweep = lp(nz, 2900) * w + lp(nz, 900) * (1 - w)          # фильтр закрывается: «пшшш» гаснет
    return master(np.stack([x + 0.5 * sweep * np.exp(-t * 4)] * 2, axis=1) * 0.8, peak=0.8)


def sfx_death_mark():
    d = 2.6
    t = tt(d)
    f0 = 196.0
    bell = sum(np.sin(2 * np.pi * f0 * r * t + i) * a * np.exp(-t * dec)
               for i, (r, a, dec) in enumerate(((1, 1, 1.2), (2.01, 0.6, 1.8), (2.99, 0.4, 2.5), (4.07, 0.3, 3.5),
                                                 (1.414, 0.45, 1.5))))
    whisper = bp(noise(d), 1800, 5000) * np.exp(-t * 2.5) * env(len(t), 0.4, 0.3, 0.6, 0.8) * 0.3
    x = reverb(bell + whisper, 2.0, 0.35)
    return master(np.stack([x, np.roll(x, 300)], axis=1), peak=0.8)


def sfx_execution():
    d = 0.9
    t = tt(d)
    swoosh = bp(noise(d), 1500, 7000) * np.exp(-((t - 0.12) / 0.08) ** 2)
    ring = sum(np.sin(2 * np.pi * 1200 * r * t) * a for r, a in ((1, 1), (2.7, 0.5), (4.1, 0.3))) * \
        np.exp(-np.clip(t - 0.18, 0, None) * 6) * (t > 0.18)
    thud = lp(kick(d), 300) * (t > 0.18)
    x = reverb(swoosh * 0.9 + ring * 0.35 + thud * 0.8, 1.2, 0.25)
    return master(np.stack([x, x], axis=1), peak=0.85)


def sfx_clown_laugh():
    """Короткий смешок «ха-ха-ха»: голосовой тон с формантами «а», каждый слог выше и короче."""
    out = np.zeros(int(1.4 * SR))
    at = 0.0
    for k, (f, dur) in enumerate(((210, 0.16), (235, 0.14), (260, 0.13), (300, 0.22))):
        n = int(dur * SR)
        tv = tt(dur)
        pitch = f * (1 + 0.15 * np.sin(np.pi * tv / dur)) * (1 + (k == 3) * 0.3 * tv / dur)
        src = np.zeros(n)
        phase = 2 * np.pi * np.cumsum(pitch) / SR
        for h in range(1, 30):
            src += np.sin(h * phase) / h
        breath = noise(dur) * 0.35
        voiced = sum(bp(src + breath, lo, hi) * g for (lo, hi), g in (((700, 1200), 1.0), ((1100, 1700), 0.7),
                                                                         ((2500, 3300), 0.3)))
        e = env(n, 0.02, 0.04, 0.8, dur * 0.5)
        i = int(at * SR)
        out[i:i + n] += voiced * e
        at += dur + 0.05
    x = reverb(out, 0.8, 0.2)
    return master(np.stack([x, x], axis=1), peak=0.8)


if __name__ == "__main__":
    folder = sys.argv[1]
    jobs = {
        "music/boss_battle": boss_battle, "music/style_duel": style_duel, "music/ancient_ghoul": ancient_ghoul,
        "music/deep_ward": deep_ward, "sfx/smoke": sfx_smoke, "sfx/death_mark": sfx_death_mark,
        "sfx/execution": sfx_execution, "sfx/clown_laugh": sfx_clown_laugh,
    }
    only = sys.argv[2].split(",") if len(sys.argv) > 2 else None
    for name, fn in jobs.items():
        if only and name not in only:
            continue
        x = fn()
        if name.startswith("sfx/"):
            x = x.mean(axis=1)                  # эффекты — моно, чтобы слышно было с той стороны, где они
        save_ogg(x, os.path.join(folder, name + ".ogg"), quality=4 if name.startswith("music/") else 5)
        print(name, f"{len(x) / SR:.1f} c")
