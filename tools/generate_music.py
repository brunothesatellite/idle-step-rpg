#!/usr/bin/env python3
"""Génère les musiques d'ambiance 8 bits de La Marche des Âmes.

Chaque biome a deux variantes de 3 minutes, en OGG Vorbis mono.
Les pistes sont originales, déterministes et publiées en CC0 avec le jeu.

Dépendances : numpy, scipy, soundfile (libsndfile avec OGG Vorbis).
Usage : python tools/generate_music.py
"""
from __future__ import annotations

import math
import os
import random
from dataclasses import dataclass
from pathlib import Path

import numpy as np
import soundfile as sf
from scipy import signal

SR = 22050
DURATION = 180.0
N = int(SR * DURATION)

ROOT = Path(__file__).resolve().parent.parent
OUTPUTS = [
    ROOT / "assets" / "music",
    ROOT / "android" / "app" / "src" / "main" / "assets" / "music",
]

MINOR = [0, 2, 3, 5, 7, 8, 10]
PHRYGIAN = [0, 1, 3, 5, 7, 8, 10]
DORIAN = [0, 2, 3, 5, 7, 9, 10]
MAJOR = [0, 2, 4, 5, 7, 9, 11]
LYDIAN = [0, 2, 4, 6, 7, 9, 11]


@dataclass(frozen=True)
class Biome:
    key: str
    root: int
    scale: list[int]
    bars: int
    pad_wave: str
    lead_wave: str
    bass_wave: str
    arp_wave: str
    progressions: list[list[int]]
    drum_style: str
    ambient_style: str
    lead_octave: int = 1
    pad_octave: int = 0
    bass_octave: int = -2
    arp_octave: int = 1


BIOMES = [
    Biome("crypts", 50, MINOR, 45, "triangle", "square", "triangle", "sine",
          [[0, 5, 3, 4], [0, 3, 5, 4]], "drip", "crypt", lead_octave=1),
    Biome("catacombs", 45, PHRYGIAN, 48, "triangle", "square", "triangle", "pulse",
          [[0, 1, 0, 4], [0, 4, 1, 0]], "bone", "catacomb", lead_octave=1),
    Biome("ash", 48, MINOR, 50, "saw", "square", "square", "pulse",
          [[0, 5, 3, 4], [0, 3, 4, 5]], "soft", "ember", lead_octave=1),
    Biome("forge", 52, MINOR, 54, "square", "square", "square", "pulse",
          [[0, 0, 5, 4], [0, 5, 3, 4]], "anvil", "forge", lead_octave=1),
    Biome("peaks", 57, LYDIAN, 45, "sine", "sine", "triangle", "sine",
          [[0, 4, 5, 2], [0, 5, 3, 2]], "soft", "wind", lead_octave=1),
    Biome("woods", 55, DORIAN, 50, "triangle", "triangle", "triangle", "sine",
          [[0, 4, 5, 3], [0, 3, 4, 0]], "soft", "forest", lead_octave=0),
    Biome("abyss", 41, PHRYGIAN, 42, "saw", "square", "saw", "pulse",
          [[0, 1, 4, 0], [0, 4, 1, 4]], "heart", "abyss", lead_octave=0),
    Biome("throne", 50, MINOR, 48, "saw", "square", "square", "pulse",
          [[0, 5, 3, 4], [0, 3, 5, 4]], "heart", "throne", lead_octave=0),
]


def note(midi: float) -> float:
    return 440.0 * (2.0 ** ((midi - 69.0) / 12.0))


def adsr(n: int, attack: float, decay: float, sustain: float, release: float) -> np.ndarray:
    env = np.full(n, sustain, dtype=np.float32)
    a = min(n, int(attack * SR))
    d = min(max(0, n - a), int(decay * SR))
    r = min(n, int(release * SR))
    if a > 0:
        env[:a] = np.linspace(0.0, 1.0, a, dtype=np.float32)
    if d > 0:
        env[a:a + d] = np.linspace(1.0, sustain, d, dtype=np.float32)
    if r > 0:
        env[-r:] *= np.linspace(1.0, 0.0, r, dtype=np.float32)
    return env


def oscillator(wave: str, phase: np.ndarray, rng: random.Random, duty: float = 0.5) -> np.ndarray:
    frac = np.mod(phase, 1.0)
    if wave == "square":
        return np.where(frac < duty, 1.0, -1.0).astype(np.float32)
    if wave == "pulse":
        return np.where(frac < 0.125, 1.0, -1.0).astype(np.float32)
    if wave == "triangle":
        return (2.0 * np.abs(2.0 * frac - 1.0) - 1.0).astype(np.float32)
    if wave == "saw":
        return (2.0 * frac - 1.0).astype(np.float32)
    if wave == "noise":
        return np.array([rng.uniform(-1.0, 1.0) for _ in range(len(phase))], dtype=np.float32)
    return np.sin(2.0 * np.pi * frac).astype(np.float32)


def add_note(
    buf: np.ndarray,
    start: float,
    duration: float,
    midi: float,
    wave: str,
    amp: float,
    rng: random.Random,
    attack: float = 0.01,
    decay: float = 0.08,
    sustain: float = 0.65,
    release: float = 0.18,
    duty: float = 0.5,
    vibrato: float = 0.0,
) -> None:
    if duration <= 0.02:
        return
    start_i = int(start * SR)
    length = min(int(duration * SR), N - start_i)
    if start_i < 0 or length <= 0:
        return
    t = np.arange(length, dtype=np.float32) / SR
    freq = note(midi) * (1.0 + vibrato * np.sin(2.0 * np.pi * 5.2 * t))
    phase = np.cumsum(freq, dtype=np.float32) / SR
    env = adsr(length, attack, decay, sustain, release)
    buf[start_i:start_i + length] += oscillator(wave, phase, rng, duty) * env * amp


def add_kick(buf: np.ndarray, at: float, amp: float = 0.18) -> None:
    start = int(at * SR)
    length = min(int(0.18 * SR), N - start)
    if length <= 0:
        return
    t = np.arange(length, dtype=np.float32) / SR
    freq = 82.0 * np.exp(-t * 18.0) + 38.0
    phase = np.cumsum(freq, dtype=np.float32) / SR
    env = np.exp(-t * 20.0).astype(np.float32)
    buf[start:start + length] += np.sin(2.0 * np.pi * phase) * env * amp


def add_noise_hit(buf: np.ndarray, at: float, amp: float, rng: random.Random, high: bool = True) -> None:
    start = int(at * SR)
    length = min(int(0.09 * SR), N - start)
    if length <= 0:
        return
    noise = np.array([rng.uniform(-1.0, 1.0) for _ in range(length)], dtype=np.float32)
    if high:
        sos = signal.butter(2, 1800.0 / (SR / 2), btype="highpass", output="sos")
        noise = signal.sosfilt(sos, noise).astype(np.float32)
    env = np.exp(-np.arange(length, dtype=np.float32) / (0.018 * SR))
    buf[start:start + length] += noise * env * amp


def add_anvil(buf: np.ndarray, at: float, amp: float, rng: random.Random) -> None:
    start = int(at * SR)
    length = min(int(0.5 * SR), N - start)
    if length <= 0:
        return
    t = np.arange(length, dtype=np.float32) / SR
    env = np.exp(-t * 9.0).astype(np.float32)
    tone = (
        np.sin(2.0 * np.pi * 780.0 * t) * 0.55
        + np.sin(2.0 * np.pi * 1170.0 * t) * 0.30
        + np.sin(2.0 * np.pi * 1560.0 * t) * 0.15
    ).astype(np.float32)
    noise = np.array([rng.uniform(-1.0, 1.0) for _ in range(length)], dtype=np.float32)
    noise = signal.sosfilt(signal.butter(2, 2500.0 / (SR / 2), btype="highpass", output="sos"), noise).astype(np.float32)
    buf[start:start + length] += (tone * 0.7 + noise * 0.3) * env * amp


def add_drip(buf: np.ndarray, at: float, amp: float) -> None:
    start = int(at * SR)
    length = min(int(0.22 * SR), N - start)
    if length <= 0:
        return
    t = np.arange(length, dtype=np.float32) / SR
    freq = 1400.0 * np.exp(-t * 11.0) + 520.0
    phase = np.cumsum(freq, dtype=np.float32) / SR
    env = np.exp(-t * 16.0).astype(np.float32)
    buf[start:start + length] += np.sin(2.0 * np.pi * phase) * env * amp


def add_heart(buf: np.ndarray, at: float, amp: float) -> None:
    add_kick(buf, at, amp)
    add_kick(buf, at + 0.18, amp * 0.55)


def add_ambient(buf: np.ndarray, style: str, rng: random.Random) -> None:
    n = N
    t = np.arange(n, dtype=np.float32) / SR
    noise = np.array([rng.uniform(-1.0, 1.0) for _ in range(n)], dtype=np.float32)
    if style in ("wind", "throne"):
        sos = signal.butter(2, [180.0 / (SR / 2), 1600.0 / (SR / 2)], btype="bandpass", output="sos")
    elif style in ("crypt", "catacomb", "abyss"):
        sos = signal.butter(2, 600.0 / (SR / 2), btype="lowpass", output="sos")
    else:
        sos = signal.butter(2, [300.0 / (SR / 2), 2400.0 / (SR / 2)], btype="bandpass", output="sos")
    noise = signal.sosfilt(sos, noise).astype(np.float32)
    lfo = 0.5 + 0.5 * np.sin(2.0 * np.pi * t / (DURATION / 4.0) + rng.random() * math.tau)
    gate = np.sin(np.pi * t / DURATION) ** 2
    buf += noise * lfo * gate * 0.028


def make_track(biome: Biome, variant: int, seed: int) -> np.ndarray:
    rng = random.Random(seed)
    bar = DURATION / biome.bars
    beat = bar / 4.0
    buf = np.zeros(N, dtype=np.float32)
    progression = biome.progressions[variant % len(biome.progressions)]
    scale = biome.scale

    def degree(deg: int, octave: int = 0) -> int:
        return biome.root + 12 * octave + scale[deg % len(scale)] + 12 * (deg // len(scale))

    # Motif de lead, propre à chaque variante.
    motif: list[tuple[int, float]] = []
    cursor = 0.0
    for _ in range(8):
        deg = rng.choice([0, 1, 2, 4, 5, 6])
        dur = rng.choice([beat * 1.0, beat * 1.5, beat * 2.0, beat * 3.0])
        motif.append((deg, dur))
        cursor += dur
    motif_len = cursor

    # Structure : intro, basse, arpèges, lead + percussions, outro.
    intro_bars = max(4, biome.bars // 8)
    bass_start = intro_bars
    arp_start = intro_bars + max(4, biome.bars // 8)
    lead_start = arp_start + max(8, biome.bars // 4)
    outro_start = max(lead_start + 4, biome.bars - 3)

    # Nappe : accords tenus, avec attaque et release lents.
    for b in range(biome.bars):
        at = b * bar
        chord_root = progression[b % len(progression)]
        chord_degrees = [chord_root, chord_root + 2, chord_root + 4]
        for i, deg in enumerate(chord_degrees):
            midi = degree(deg, biome.pad_octave) + (12 if i == 2 and rng.random() < 0.3 else 0)
            add_note(
                buf, at, bar * 0.98, midi, biome.pad_wave,
                0.065 if i == 0 else 0.045, rng,
                attack=0.35, decay=0.4, sustain=0.55, release=0.9,
                duty=0.25 if biome.pad_wave == "square" else 0.5,
                vibrato=0.0015,
            )

    # Basse.
    for b in range(bass_start, outro_start):
        at = b * bar
        chord_root = progression[b % len(progression)]
        root_midi = degree(chord_root, biome.bass_octave)
        if b % 2 == 0:
            add_note(buf, at, beat * 1.7, root_midi, biome.bass_wave, 0.12, rng,
                     attack=0.005, decay=0.05, sustain=0.7, release=0.12, duty=0.5)
        add_note(buf, at + beat * 2.0, beat * 1.5, root_midi, biome.bass_wave, 0.09, rng,
                 attack=0.005, decay=0.05, sustain=0.65, release=0.12, duty=0.5)

    # Arpèges.
    for b in range(arp_start, outro_start):
        at = b * bar
        chord_root = progression[b % len(progression)]
        for step in range(8):
            if step % 2 == 0 or rng.random() < 0.55:
                deg = chord_root + [0, 2, 4, 2][step % 4]
                add_note(
                    buf, at + step * beat * 0.5, beat * 0.42,
                    degree(deg, biome.arp_octave), biome.arp_wave, 0.035, rng,
                    attack=0.004, decay=0.03, sustain=0.35, release=0.08,
                    duty=0.25,
                )

    # Lead : motif répété, transposé par l'accord.
    b = lead_start
    motif_index = 0
    while b < outro_start:
        at = b * bar
        chord_root = progression[b % len(progression)]
        local = 0.0
        while local < bar * 0.92:
            deg, dur = motif[motif_index % len(motif)]
            midi = degree(chord_root + deg, biome.lead_octave)
            add_note(
                buf, at + local, min(dur, bar - local - 0.05), midi,
                biome.lead_wave, 0.075, rng,
                attack=0.012, decay=0.08, sustain=0.58, release=0.22,
                duty=0.25 if biome.lead_wave == "square" else 0.5,
                vibrato=0.004,
            )
            local += dur
            motif_index += 1
        b += 1

    # Percussions.
    for b in range(lead_start, outro_start):
        at = b * bar
        if biome.drum_style == "soft":
            if b % 2 == 0:
                add_kick(buf, at, 0.10)
            if b % 4 == 2:
                add_noise_hit(buf, at + beat * 3.5, 0.025, rng)
        elif biome.drum_style == "anvil":
            add_anvil(buf, at, 0.075 if b % 2 == 0 else 0.04, rng)
            if b % 4 == 0:
                add_kick(buf, at, 0.10)
        elif biome.drum_style == "drip":
            if b % 2 == 1:
                add_drip(buf, at + beat * (1 + rng.random() * 2), 0.055)
        elif biome.drum_style == "bone":
            for step in (1, 2.5, 3.25):
                if rng.random() < 0.55:
                    add_noise_hit(buf, at + step * beat, 0.018, rng)
        elif biome.drum_style == "heart":
            if b % 3 == 0:
                add_heart(buf, at, 0.12)

    add_ambient(buf, biome.ambient_style, rng)

    # Oiseaux / bulles / braises ponctuels.
    if biome.ambient_style == "forest":
        for _ in range(10):
            add_drip(buf, rng.uniform(0.0, DURATION - 0.5), 0.025)
    elif biome.ambient_style == "abyss":
        for _ in range(14):
            add_drip(buf, rng.uniform(0.0, DURATION - 0.4), 0.018)
    elif biome.ambient_style == "ember":
        for _ in range(18):
            add_noise_hit(buf, rng.uniform(0.0, DURATION - 0.2), 0.012, rng, high=False)

    # Écho circulaire : la queue reboucle proprement.
    delay = int(0.38 * SR)
    buf += np.roll(buf, delay) * 0.16
    buf += np.roll(buf, delay * 2) * 0.06

    # Nettoyage et fondu de boucle très court.
    buf = np.nan_to_num(buf)
    sos = signal.butter(2, [35.0 / (SR / 2), 7800.0 / (SR / 2)], btype="bandpass", output="sos")
    buf = signal.sosfilt(sos, buf).astype(np.float32)
    buf = np.tanh(buf * 1.3).astype(np.float32)
    edge = int(0.045 * SR)
    buf[:edge] *= np.linspace(0.0, 1.0, edge, dtype=np.float32)
    buf[-edge:] *= np.linspace(1.0, 0.0, edge, dtype=np.float32)
    peak = float(np.max(np.abs(buf)))
    if peak > 0:
        buf *= 0.86 / peak
    return buf


def main() -> None:
    for directory in OUTPUTS:
        directory.mkdir(parents=True, exist_ok=True)
    for biome in BIOMES:
        for variant in (1, 2):
            seed = 1000 + BIOMES.index(biome) * 100 + variant
            track = make_track(biome, variant - 1, seed)
            name = f"{biome.key}_{variant}.ogg"
            for directory in OUTPUTS:
                with sf.SoundFile(
                    directory / name,
                    mode="w",
                    samplerate=SR,
                    channels=1,
                    format="OGG",
                    subtype="VORBIS",
                ) as handle:
                    for chunk in np.array_split(track, 180):
                        handle.write(chunk)
            print(f"ok {name} ({os.path.getsize(OUTPUTS[0] / name) // 1024} Ko)")


if __name__ == "__main__":
    main()
