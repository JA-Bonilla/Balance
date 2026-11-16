"""Generates the bundled timer sounds (Balance/Sounds/*.wav). Run: python3 tools/make_sounds.py"""
import math
import struct
import sys
import wave
from pathlib import Path

RATE = 22050
OUT = Path(__file__).resolve().parent.parent / "Balance" / "Sounds"


def tone(freqs, seconds, decay, start=0.0):
    """Sum of sine partials (freq, amplitude) with exponential decay, placed at `start`."""
    return [(start, [(f, a) for f, a in freqs], seconds, decay)]


def render(events, length):
    samples = [0.0] * int(RATE * length)
    for start, partials, seconds, decay in events:
        s0 = int(start * RATE)
        for i in range(int(seconds * RATE)):
            if s0 + i >= len(samples):
                break
            t = i / RATE
            env = math.exp(-decay * t) * min(1.0, t / 0.005)  # 5 ms attack avoids clicks
            samples[s0 + i] += env * sum(a * math.sin(2 * math.pi * f * t) for f, a in partials)
    peak = max(abs(s) for s in samples) or 1.0
    return [s / peak * 0.8 for s in samples]


def write(name, samples):
    OUT.mkdir(parents=True, exist_ok=True)
    with wave.open(str(OUT / f"{name}.wav"), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(b"".join(struct.pack("<h", int(s * 32767)) for s in samples))


sounds = {
    # Rising C-E-G arpeggio.
    "chime": (sum((tone([(f, 1), (f * 2, 0.3)], 1.5, 3, i * 0.18) for i, f in enumerate([1047, 1319, 1568])), []), 2.0),
    # Struck bell with inharmonic partials, repeated twice.
    "bell": (sum((tone([(660, 1), (1320, 0.5), (1980, 0.3), (2772, 0.2)], 2.0, 2, s) for s in (0, 1.2)), []), 3.2),
    # Three quick beeps, twice.
    "pulse": (sum((tone([(1000, 1)], 0.12, 0, g * 0.8 + b * 0.18) for g in range(2) for b in range(3)), []), 1.6),
    # Soft two-note swell.
    "gentle": (tone([(440, 1), (554, 0.6)], 1.2, 1.5) + tone([(392, 1), (494, 0.6)], 1.6, 1.5, 0.9), 2.6),
}

for name, (events, length) in sounds.items():
    write(name, render(events, length))
    print(f"wrote {name}.wav", file=sys.stderr)
