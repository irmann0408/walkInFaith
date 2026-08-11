#!/usr/bin/env python3
"""Generates placeholder SFX/music WAV files for app/src/main/res/raw/.

Pure Python stdlib (wave + struct + math) — no ffmpeg/sox/numpy available
in this environment, and no way to source or license real recorded
music/instrument samples. These are clearly synthesized placeholder tones,
matching this app's "placeholder art now, swap in final assets later
without touching logic" precedent, extended to audio. Run once:

    python3 scripts/generate_placeholder_audio.py

Re-run any time to regenerate all files deterministically (no randomness).
"""
import math
import os
import struct
import wave

SAMPLE_RATE = 22050
OUT_DIR = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res", "raw")

# Equal-tempered note frequencies (Hz), A4 = 440.
NOTE_FREQS = {
    "C4": 261.63, "D4": 293.66, "E4": 329.63, "F4": 349.23, "G4": 392.00, "A4": 440.00, "B4": 493.88,
    "C5": 523.25, "D5": 587.33, "E5": 659.25, "F5": 698.46, "G5": 783.99, "A5": 880.00,
}


def _envelope(i: int, n: int, attack: int, release: int) -> float:
    """Linear attack/release envelope to avoid clicks at segment boundaries."""
    if i < attack:
        return i / attack
    if i > n - release:
        return max(0.0, (n - i) / release)
    return 1.0


def tone(freq: float, duration: float, amplitude: float = 0.5, harmonics: list[tuple[float, float]] | None = None) -> list[float]:
    """A single tone: fundamental plus optional (harmonic_multiple, relative_amplitude) pairs."""
    n = int(SAMPLE_RATE * duration)
    attack = max(1, int(SAMPLE_RATE * 0.01))
    release = max(1, int(SAMPLE_RATE * min(0.08, duration * 0.4)))
    samples = []
    partials = [(1.0, 1.0)] + (harmonics or [])
    norm = sum(a for _, a in partials)
    for i in range(n):
        t = i / SAMPLE_RATE
        value = sum(a * math.sin(2 * math.pi * freq * mult * t) for mult, a in partials) / norm
        samples.append(value * amplitude * _envelope(i, n, attack, release))
    return samples


def sweep(freq_start: float, freq_end: float, duration: float, amplitude: float = 0.4) -> list[float]:
    """A frequency sweep (chirp) — used for the soft transition/dodge "swish" effects."""
    n = int(SAMPLE_RATE * duration)
    attack = max(1, int(SAMPLE_RATE * 0.01))
    release = max(1, int(SAMPLE_RATE * duration * 0.3))
    samples = []
    phase = 0.0
    for i in range(n):
        t = i / n
        freq = freq_start + (freq_end - freq_start) * t
        phase += 2 * math.pi * freq / SAMPLE_RATE
        samples.append(math.sin(phase) * amplitude * _envelope(i, n, attack, release))
    return samples


def silence(duration: float) -> list[float]:
    return [0.0] * int(SAMPLE_RATE * duration)


def concat(*parts: list[float]) -> list[float]:
    out: list[float] = []
    for p in parts:
        out.extend(p)
    return out


def mix(*parts: list[float]) -> list[float]:
    n = max(len(p) for p in parts)
    out = [0.0] * n
    for p in parts:
        for i, v in enumerate(p):
            out[i] += v
    peak = max((abs(v) for v in out), default=1.0) or 1.0
    scale = min(1.0, 0.9 / peak)
    return [v * scale for v in out]


def loop_pattern(notes: list[str], note_duration: float, amplitude: float, repeats: int) -> list[float]:
    """A gentle arpeggiated pad loop for background music."""
    one_pass = concat(*[tone(NOTE_FREQS[n], note_duration, amplitude, harmonics=[(2.0, 0.25)]) for n in notes])
    return one_pass * repeats


def write_wav(name: str, samples: list[float]) -> None:
    os.makedirs(OUT_DIR, exist_ok=True)
    path = os.path.join(OUT_DIR, f"{name}.wav")
    with wave.open(path, "wb") as f:
        f.setnchannels(1)
        f.setsampwidth(2)
        f.setframerate(SAMPLE_RATE)
        frames = b"".join(struct.pack("<h", max(-32767, min(32767, int(v * 32767)))) for v in samples)
        f.writeframes(frames)
    print(f"wrote {path} ({len(samples) / SAMPLE_RATE:.2f}s)")


def main() -> None:
    # Short SFX
    write_wav("match_success", concat(tone(NOTE_FREQS["C5"], 0.1, 0.5), tone(NOTE_FREQS["E5"], 0.15, 0.5)))
    write_wav("scene_transition", sweep(400, 900, 0.18, 0.35))
    write_wav("reward_celebration", concat(
        tone(NOTE_FREQS["C4"], 0.15, 0.45, harmonics=[(2.0, 0.3)]),
        tone(NOTE_FREQS["E4"], 0.15, 0.45, harmonics=[(2.0, 0.3)]),
        tone(NOTE_FREQS["G4"], 0.15, 0.45, harmonics=[(2.0, 0.3)]),
        tone(NOTE_FREQS["C5"], 0.4, 0.5, harmonics=[(2.0, 0.3), (3.0, 0.15)]),
    ))
    write_wav("item_collected", tone(1200.0, 0.12, 0.4, harmonics=[(2.0, 0.2)]))
    write_wav("target_hit", concat(tone(NOTE_FREQS["G5"], 0.08, 0.45), tone(NOTE_FREQS["C5"], 0.12, 0.4)))
    write_wav("obstacle_dodged", sweep(700, 300, 0.15, 0.35))

    # Trumpet fanfare — the whole reason this milestone exists (Jericho's shout).
    # Additive brass-ish timbre: strong 2nd/3rd/4th harmonics, fast attack.
    brass_harmonics = [(2.0, 0.6), (3.0, 0.4), (4.0, 0.25)]
    write_wav("trumpet_fanfare", concat(
        tone(NOTE_FREQS["G4"], 0.18, 0.55, harmonics=brass_harmonics),
        tone(NOTE_FREQS["C5"], 0.18, 0.55, harmonics=brass_harmonics),
        tone(NOTE_FREQS["E5"], 0.18, 0.55, harmonics=brass_harmonics),
        tone(NOTE_FREQS["G5"], 0.9, 0.6, harmonics=brass_harmonics),
    ))

    # Looping background music beds — gentle, short, clearly placeholder.
    write_wav("world_map_music", loop_pattern(["C4", "E4", "G4", "E4"], 0.9, 0.18, repeats=4))
    write_wav("adventure_music", loop_pattern(["D4", "F4", "A4", "F4", "G4", "F4"], 0.7, 0.16, repeats=3))


if __name__ == "__main__":
    main()
