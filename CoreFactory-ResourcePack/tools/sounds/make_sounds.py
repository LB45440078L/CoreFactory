#!/usr/bin/env python3
"""Builds the CoreFactory machine sounds and writes them as OGG Vorbis into art/sounds.

Two kinds of sound go into the pack:

- Procedural sounds are synthesised here from sine tones, decaying bursts and filtered noise. Each builder below says how
  its sound is made. They are low-passed and have soft attacks and releases, so they are free of clicks.
- Sourced sounds are public-domain or CC0 recordings listed in sources.json. Each entry gives the URL, the SHA-256 of the
  file, the author and licence, and the edit: a start time, a length, an ffmpeg filter chain, and for loops a crossfade
  length. The script downloads each file into a cache folder, checks its hash, and cuts and filters it with ffmpeg. Loops
  are joined by crossfading the tail into the head, so they repeat without a seam.

Each sound is written as a 16-bit mono WAV in a temporary folder, normalised to its level, and encoded with ffmpeg
(libvorbis, quality 3). The ffmpeg binary comes from the imageio-ffmpeg package when it is installed, otherwise from
--ffmpeg or the PATH. This is a build-time tool only and is not needed to run the plugin or the resource pack builder.

Usage: python make_sounds.py [--ffmpeg PATH] [--out DIR] [--cache DIR] [--only KEY ...]
"""

import argparse
import array
import hashlib
import json
import math
import random
import re
import shutil
import struct
import subprocess
import tempfile
import urllib.parse
import urllib.request
import wave
import zipfile
from pathlib import Path

RATE = 22050
TAU = 2.0 * math.pi
HERE = Path(__file__).resolve().parent
DEFAULT_OUT = HERE.parents[1] / "art" / "sounds"
SOURCES = HERE / "sources.json"
DEFAULT_CACHE = Path(tempfile.gettempdir()) / "corefactory-sound-sources"


def samples(seconds):
    return int(round(seconds * RATE))


def tone(n, freq):
    step = TAU * freq / RATE
    return [math.sin(step * i) for i in range(n)]


def sweep(n, f0, f1, seconds):
    """A tone that glides from f0 to f1 over the given seconds, on a log scale."""
    out = []
    phase = 0.0
    ratio = f1 / f0
    for i in range(n):
        t = min(i / RATE / seconds, 1.0)
        phase += TAU * f0 * ratio ** t / RATE
        out.append(math.sin(phase))
    return out


def noise(n, seed):
    rng = random.Random(seed)
    return [rng.uniform(-1.0, 1.0) for _ in range(n)]


def lowpass(x, cutoff, passes=2):
    """A cascade of one-pole low-pass filters, for a soft roll-off above the cutoff."""
    a = 1.0 - math.exp(-TAU * cutoff / RATE)
    y = list(x)
    for _ in range(passes):
        state = 0.0
        out = []
        for v in y:
            state += a * (v - state)
            out.append(state)
        y = out
    return y


def loop_lowpass(x, cutoff, passes=2):
    """The low-pass of a signal that repeats every len(x) samples. The filter runs over two copies and keeps the second,
    after its start-up transient has died away, so the result repeats seamlessly as well."""
    return lowpass(x + x, cutoff, passes)[len(x):]


def rms_norm(x):
    rms = math.sqrt(sum(v * v for v in x) / len(x)) or 1.0
    return [v / rms for v in x]


def mix(out, part, gain=1.0, at=0.0):
    """Adds part into out, starting at the given time in seconds. Samples past the end of out are dropped."""
    start = samples(at)
    for j, v in enumerate(part):
        k = start + j
        if k < len(out):
            out[k] += gain * v
    return out


def decay(x, tau, attack=0.002, release=0.02):
    """An exponential decay with time constant tau, a raised-cosine attack and a raised-cosine release, so that an event
    that is cut short never clicks."""
    n = len(x)
    a = max(1, samples(attack))
    r = max(1, samples(release))
    out = []
    for i, v in enumerate(x):
        g = math.exp(-i / (RATE * tau))
        if i < a:
            g *= 0.5 - 0.5 * math.cos(math.pi * i / a)
        if i >= n - r:
            g *= 0.5 - 0.5 * math.cos(math.pi * (n - i) / r)
        out.append(v * g)
    return out


def envelope(n, attack, release):
    a = max(1, samples(attack))
    r = max(1, samples(release))
    out = []
    for i in range(n):
        g = 1.0
        if i < a:
            g = 0.5 - 0.5 * math.cos(math.pi * i / a)
        if i >= n - r:
            g *= 0.5 - 0.5 * math.cos(math.pi * (n - i) / r)
        out.append(g)
    return out


def normalise(x, peak_db):
    peak = max(abs(v) for v in x)
    scale = (10 ** (peak_db / 20.0)) / peak
    return [v * scale for v in x]


def belt_hum(seconds=1.0):
    n = samples(seconds)
    body = rms_norm(loop_lowpass(noise(n, 2), 450))
    gate = [0.75 + 0.25 * math.sin(TAU * 5 * i / RATE) for i in range(n)]
    mix(body, [t * g for t, g in zip(tone(n, 110), gate)], 0.3)
    for k in range(4):
        mix(body, decay(tone(samples(0.1), 140), 0.012), 0.35, at=k * 0.25)
    return loop_lowpass(body, 900)


def drill_run(seconds=3.0):
    n = samples(seconds)
    body = [0.0] * n
    pulse = [0.7 + 0.3 * math.sin(TAU * 4 * i / RATE) for i in range(n)]
    mix(body, [t * p for t, p in zip(tone(n, 90), pulse)], 0.8)
    mix(body, [t * p for t, p in zip(tone(n, 180), pulse)], 0.25)
    mix(body, [v * p for v, p in zip(rms_norm(loop_lowpass(noise(n, 3), 800)), pulse)], 0.45)
    return loop_lowpass(body, 1000)


def lab_run(seconds=2.0):
    n = samples(seconds)
    body = [0.0] * n
    drone = [0.6 + 0.4 * math.sin(TAU * 1 * i / RATE) for i in range(n)]
    mix(body, [t * d for t, d in zip(tone(n, 330), drone)], 0.2)
    mix(body, [t * d for t, d in zip(tone(n, 495), drone)], 0.08)
    for k in range(4):
        mix(body, decay(sweep(samples(0.12), 300, 520, 0.12), 0.04), 0.5, at=k * 0.5)
    mix(body, rms_norm(loop_lowpass(noise(n, 5), 900)), 0.04)
    return loop_lowpass(body, 1800)


def mill_grind(seconds=1.5):
    n = samples(seconds)
    gate = [(0.5 + 0.5 * math.sin(TAU * 6 * i / RATE)) ** 2 for i in range(n)]
    grain = rms_norm(loop_lowpass(noise(n, 4), 1100))
    body = [g * k for g, k in zip(grain, gate)]
    mix(body, [t * k for t, k in zip(tone(n, 60), gate)], 0.5)
    return loop_lowpass(body, 1200)


def turret_rotate(seconds=0.6):
    """A servo: a sawtooth-like buzz gliding up from 900 to 1500 Hz, with a gear tick at 38 Hz, cut short with a soft
    attack and release. It is a tonal whine, not a hiss."""
    n = samples(seconds)
    phase = 0.0
    buzz = []
    for i in range(n):
        f = 900.0 * (1500.0 / 900.0) ** (i / n)
        phase += TAU * f / RATE
        buzz.append(sum(math.sin(k * phase) / k for k in range(1, 6)))
    ticks = [0.55 + 0.45 * max(0.0, math.sin(TAU * 38 * i / RATE)) ** 4 for i in range(n)]
    body = lowpass([v * t for v, t in zip(buzz, ticks)], 3500, 2)
    return [v * e for v, e in zip(body, envelope(n, 0.03, 0.08))]


def picker_clunk(seconds=0.35):
    out = [0.0] * samples(seconds)
    mix(out, decay(sweep(samples(0.12), 210, 150, 0.12), 0.05), 0.8)
    mix(out, decay(rms_norm(lowpass(noise(samples(0.05), 12), 900)), 0.012), 0.3)
    return out


def warble(n, freq, depth, cycles):
    """A tone whose pitch wobbles by depth Hz, cycles times over the loop, so the wobble repeats seamlessly."""
    out = []
    phase = 0.0
    for i in range(n):
        phase += TAU * (freq + depth * math.sin(TAU * cycles * i / n)) / RATE
        out.append(math.sin(phase))
    return out


def band(n, seed, low, high):
    """Noise between two cutoffs, normalised. Both cutoffs run over the loop, so the band repeats seamlessly."""
    raw = noise(n, seed)
    return rms_norm([a - b for a, b in zip(loop_lowpass(raw, high), loop_lowpass(raw, low))])


def bubbles(seconds, seed, count, low, high, tau, length):
    """Short upward blips at random times, the bubbles of a vessel. Each blip ends inside the sound."""
    rng = random.Random(seed)
    out = [0.0] * samples(seconds)
    for _ in range(count):
        f = rng.uniform(low, high)
        at = rng.uniform(0.0, seconds - length)
        mix(out, decay(sweep(samples(length), f, f * 1.6, length), tau, attack=0.003, release=0.01), 1.0, at=at)
    return out


def wind_generator(seconds=2.0):
    n = samples(seconds)
    swell = [0.7 + 0.3 * math.sin(TAU * i / n) for i in range(n)]
    blades = [0.85 + 0.15 * math.sin(TAU * 6 * i / n) for i in range(n)]
    body = [v * s * b for v, s, b in zip(band(n, 20, 150, 700), swell, blades)]
    mix(body, [t * s for t, s in zip(tone(n, 55), swell)], 0.12)
    return loop_lowpass(body, 900)


def solar_panel(seconds=2.0):
    n = samples(seconds)
    body = [0.0] * n
    mix(body, tone(n, 100), 0.5)
    mix(body, tone(n, 200), 0.12)
    for k in range(4):
        mix(body, decay(tone(samples(0.03), 700), 0.008), 0.35, at=k * 0.5)
    return loop_lowpass(body, 800)


def charcoal_generator(seconds=2.0):
    n = samples(seconds)
    flicker = [0.75 + 0.25 * math.sin(TAU * 3 * i / n) for i in range(n)]
    body = [v * f for v, f in zip(band(n, 21, 220, 1100), flicker)]
    rng = random.Random(22)
    for _ in range(14):
        click = rms_norm(lowpass(noise(samples(0.03), rng.randrange(1 << 30)), 1200, 1))
        mix(body, decay(click, 0.004), 0.3, at=rng.uniform(0.0, seconds - 0.05))
    mix(body, [t * f for t, f in zip(tone(n, 70), flicker)], 0.15)
    return loop_lowpass(body, 1400)


def petroleum_generator(seconds=2.0):
    n = samples(seconds)
    thumps = [0.0] * n
    for k in range(2):
        mix(thumps, decay(sweep(samples(0.4), 110, 50, 0.4), 0.07), 1.0, at=k * 1.0)
    rock = [0.8 + 0.2 * math.sin(TAU * i / n) for i in range(n)]
    body = [v * r for v, r in zip(thumps, rock)]
    mix(body, rms_norm(loop_lowpass(noise(n, 23), 300)), 0.06)
    return loop_lowpass(body, 600)


def nuclear_generator(seconds=2.0):
    n = samples(seconds)
    swell = [0.85 + 0.15 * math.sin(TAU * i / n) for i in range(n)]
    body = [0.0] * n
    mix(body, [t * s for t, s in zip(tone(n, 40), swell)], 1.0)
    mix(body, tone(n, 80), 0.4)
    mix(body, tone(n, 120), 0.15)
    for k in range(2):
        mix(body, decay(tone(samples(0.25), 60), 0.07), 0.25, at=k * 1.0)
    mix(body, rms_norm(loop_lowpass(noise(n, 25), 120)), 0.05)
    return loop_lowpass(body, 400)


def ore_drill(seconds=3.0):
    n = samples(seconds)
    rotor = [0.8 + 0.2 * math.sin(TAU * 9 * i / n) for i in range(n)]
    body = [g * r for g, r in zip(band(n, 26, 300, 1200), rotor)]
    mix(body, [t * r for t, r in zip(tone(n, 150), rotor)], 0.35)
    mix(body, [t * r for t, r in zip(tone(n, 300), rotor)], 0.12)
    return loop_lowpass(body, 1100)


def petroleum_drill(seconds=3.0):
    n = samples(seconds)
    stroke = [0.7 + 0.3 * abs(math.sin(math.pi * 3 * i / n)) for i in range(n)]
    body = [0.0] * n
    mix(body, [t * s for t, s in zip(tone(n, 50), stroke)], 1.0)
    mix(body, [t * s for t, s in zip(tone(n, 100), stroke)], 0.35)
    mix(body, [v * s for v, s in zip(rms_norm(loop_lowpass(noise(n, 27), 160)), stroke)], 0.15)
    return loop_lowpass(body, 350)


def separator(seconds=2.0):
    n = samples(seconds)
    body = [0.0] * n
    mix(body, warble(n, 240, 8, 2), 0.3)
    mix(body, warble(n, 480, 8, 2), 0.08)
    mix(body, rms_norm(loop_lowpass(noise(n, 28), 700)), 0.05)
    return loop_lowpass(body, 1200)


def roaster(seconds=2.0):
    n = samples(seconds)
    roar = [0.6 + 0.4 * math.sin(TAU * 2 * i / n) for i in range(n)]
    body = [v * r for v, r in zip(band(n, 29, 200, 900), roar)]
    mix(body, tone(n, 90), 0.12)
    return loop_lowpass(body, 1000)


def crude_oil_refinery(seconds=2.0):
    n = samples(seconds)
    body = [0.0] * n
    mix(body, bubbles(seconds, 32, 8, 140, 260, 0.05, 0.2), 0.6)
    mix(body, tone(n, 70), 0.12)
    mix(body, rms_norm(loop_lowpass(noise(n, 33), 350)), 0.08)
    return loop_lowpass(body, 700)


def converter(seconds=2.0):
    n = samples(seconds)
    gate = [0.85 + 0.15 * math.sin(TAU * 4 * i / n) for i in range(n)]
    body = [0.0] * n
    mix(body, [t * g for t, g in zip(tone(n, 150), gate)], 0.5)
    mix(body, [t * g for t, g in zip(tone(n, 300), gate)], 0.2)
    mix(body, [t * g for t, g in zip(tone(n, 450), gate)], 0.06)
    return loop_lowpass(body, 900)


def enrichment_cascade(seconds=2.0):
    n = samples(seconds)
    body = [0.0] * n
    mix(body, warble(n, 360, 14, 1), 0.3)
    mix(body, warble(n, 720, 14, 1), 0.1)
    mix(body, rms_norm(loop_lowpass(noise(n, 34), 900)), 0.04)
    return loop_lowpass(body, 1200)


def transmuter(seconds=2.0):
    n = samples(seconds)
    gate = [0.8 + 0.2 * math.sin(TAU * i / n) for i in range(n)]
    body = [0.0] * n
    mix(body, [t * g for t, g in zip(tone(n, 440), gate)], 0.22)
    mix(body, [t * g for t, g in zip(tone(n, 442), gate)], 0.22)
    mix(body, [t * g for t, g in zip(tone(n, 660), gate)], 0.08)
    return loop_lowpass(body, 1200)


def spectrometer(seconds=2.0):
    out = [0.0] * samples(seconds)
    for k, freq in enumerate((660, 660, 880, 660)):
        mix(out, decay(tone(samples(0.4), freq), 0.12), 0.5, at=k * 0.5)
        mix(out, decay(tone(samples(0.4), freq * 3 // 2), 0.06), 0.1, at=k * 0.5)
    return out


def particle_accelerator(seconds=3.0):
    n = samples(seconds)
    body = [0.0] * n
    period = 1.0
    for k in range(int(seconds / period)):
        at = k * period
        mix(body, [v * e for v, e in zip(sweep(samples(period), 110, 220, period),
                                         envelope(samples(period), 0.15, 0.25))], 0.4, at=at)
        mix(body, [v * e for v, e in zip(sweep(samples(period), 220, 440, period),
                                         envelope(samples(period), 0.15, 0.25))], 0.1, at=at)
    return body


def antimatter_condenser(seconds=2.0):
    n = samples(seconds)
    pulse = [(0.5 + 0.5 * math.sin(TAU * 4 * i / n)) ** 2 for i in range(n)]
    body = [0.0] * n
    mix(body, [t * p for t, p in zip(tone(n, 180), pulse)], 0.5)
    mix(body, [t * p for t, p in zip(tone(n, 360), pulse)], 0.15)
    mix(body, tone(n, 90), 0.25)
    return loop_lowpass(body, 900)


def research_console(seconds=2.0):
    out = [0.0] * samples(seconds)
    for at, freq in ((0.0, 660), (0.2, 880), (1.0, 660), (1.2, 990)):
        mix(out, decay(tone(samples(0.12), freq), 0.05), 0.5, at=at)
    return out


def cartridge_press(seconds=1.2):
    out = [0.0] * samples(seconds)
    mix(out, decay(sweep(samples(0.5), 200, 80, 0.5), 0.12), 0.8)
    mix(out, decay(rms_norm(lowpass(noise(samples(0.08), 33), 600, 1)), 0.03), 0.3, at=0.02)
    mix(out, decay(sweep(samples(0.5), 90, 200, 0.5), 0.2), 0.12, at=0.6)
    return out


def bullet_swaging_machine(seconds=0.8):
    out = [0.0] * samples(seconds)
    for freq, gain in ((380, 0.5), (610, 0.3), (905, 0.15)):
        mix(out, decay(tone(samples(0.6), freq), 0.12), gain)
    mix(out, decay(rms_norm(lowpass(noise(samples(0.03), 34), 1000, 1)), 0.008), 0.3)
    return out


def ammunition_assembly(seconds=0.8):
    out = [0.0] * samples(seconds)
    for k in range(4):
        at = k * 0.16
        mix(out, decay(rms_norm(lowpass(noise(samples(0.02), 35 + k), 900)), 0.004), 0.5, at=at)
        mix(out, decay(tone(samples(0.03), 700 - 40 * k), 0.006), 0.15, at=at)
    return out


def metal_lathe(seconds=2.0):
    n = samples(seconds)
    body = [0.0] * n
    mix(body, warble(n, 260, 4, 3), 0.3)
    mix(body, tone(n, 520), 0.1)
    mix(body, rms_norm(loop_lowpass(noise(n, 36), 1000)), 0.05)
    return loop_lowpass(body, 1300)


def component_assembly(seconds=2.0):
    n = samples(seconds)
    teeth = [(0.5 + 0.5 * math.sin(TAU * 16 * i / n)) ** 2 for i in range(n)]
    body = [v * t for v, t in zip(band(n, 37, 400, 1000), teeth)]
    mix(body, [t * g for t, g in zip(tone(n, 200), teeth)], 0.3)
    return loop_lowpass(body, 1100)


def heavy_weapons_assembly(seconds=2.0):
    n = samples(seconds)
    pump = [0.75 + 0.25 * math.sin(TAU * i / n) for i in range(n)]
    body = [0.0] * n
    mix(body, [t * p for t, p in zip(tone(n, 50), pump)], 0.9)
    mix(body, tone(n, 100), 0.3)
    mix(body, tone(n, 150), 0.1)
    mix(body, [v * p for v, p in zip(rms_norm(loop_lowpass(noise(n, 38), 260)), pump)], 0.2)
    return loop_lowpass(body, 600)


def pylon_hum(seconds=2.0):
    n = samples(seconds)
    swell = [0.9 + 0.1 * math.sin(TAU * 2 * i / n) for i in range(n)]
    body = [0.0] * n
    for k in range(1, 5):
        mix(body, tone(n, 300 * k), 1.0 / k)
    body = [v * s for v, s in zip(body, swell)]
    return loop_lowpass(body, 1300)


def accumulator_charge(seconds=1.2):
    n = samples(seconds)
    env = envelope(n, 0.15, 0.25)
    out = [0.0] * n
    mix(out, [v * e for v, e in zip(sweep(n, 330, 660, seconds), env)], 0.8)
    mix(out, [v * e for v, e in zip(sweep(n, 495, 990, seconds), env)], 0.2)
    return out


def water_pump(seconds=2.0):
    """A centrifugal pump: a turbine rumble (band-limited noise with a slow swell over the loop), a motor hum at 110 Hz with
    a faint octave, and a soft swish at the vane passing rate, 8 per second (16 cycles over the 2 s loop). Every part
    divides evenly into the loop, so it repeats seamlessly."""
    n = samples(seconds)
    swell = [0.8 + 0.2 * math.sin(TAU * i / n) for i in range(n)]
    body = [v * s for v, s in zip(band(n, 40, 50, 350), swell)]
    mix(body, tone(n, 110), 0.35)
    mix(body, tone(n, 220), 0.06)
    vanes = [0.25 + 0.75 * (0.5 - 0.5 * math.cos(TAU * 16 * i / n)) ** 3 for i in range(n)]
    mix(body, [v * g for v, g in zip(band(n, 41, 300, 1400), vanes)], 0.3)
    return loop_lowpass(body, 1000)


# Procedural sounds: key, builder, peak level in dBFS. The hums sit a little lower because they play for long stretches.
# The sourced sounds (press, turret, laser, generator and hiss keys, and the bubbling purifier) are in sources.json.
SOUNDS = [
    ("belt_hum", belt_hum, -15.0),
    ("drill_run", drill_run, -14.0),
    ("lab_run", lab_run, -15.0),
    ("mill_grind", mill_grind, -13.0),
    ("turret_rotate", turret_rotate, -13.0),
    ("picker_clunk", picker_clunk, -12.0),
    ("wind_generator", wind_generator, -14.0),
    ("solar_panel", solar_panel, -16.0),
    ("charcoal_generator", charcoal_generator, -14.0),
    ("petroleum_generator", petroleum_generator, -14.0),
    ("nuclear_generator", nuclear_generator, -15.0),
    ("ore_drill", ore_drill, -14.0),
    ("petroleum_drill", petroleum_drill, -14.0),
    ("separator", separator, -15.0),
    ("roaster", roaster, -14.0),
    ("converter", converter, -15.0),
    ("enrichment_cascade", enrichment_cascade, -15.0),
    ("transmuter", transmuter, -15.0),
    ("spectrometer", spectrometer, -15.0),
    ("particle_accelerator", particle_accelerator, -15.0),
    ("antimatter_condenser", antimatter_condenser, -15.0),
    ("research_console", research_console, -15.0),
    ("crude_oil_refinery", crude_oil_refinery, -14.0),
    ("cartridge_press", cartridge_press, -13.0),
    ("bullet_swaging_machine", bullet_swaging_machine, -13.0),
    ("ammunition_assembly", ammunition_assembly, -14.0),
    ("metal_lathe", metal_lathe, -15.0),
    ("component_assembly", component_assembly, -15.0),
    ("heavy_weapons_assembly", heavy_weapons_assembly, -15.0),
    ("pylon_hum", pylon_hum, -16.0),
    ("accumulator_charge", accumulator_charge, -15.0),
    ("water_pump", water_pump, -14.0),
]


def crossfade_loop(x, length, fade):
    """Turns len(x) == length + fade samples into a loop of length samples. The tail is faded in over the head, so the
    last sample of the loop meets the first one as the next sample of the recording, and the join is seamless."""
    n = samples(length)
    f = samples(fade)
    if len(x) < n + f:
        raise SystemExit(f"loop source too short: {len(x)} samples, need {n + f}")
    out = list(x[:n])
    for i in range(f):
        w = 0.5 - 0.5 * math.cos(math.pi * i / f)
        out[i] = x[i] * w + x[n + i] * (1.0 - w)
    return out


def fetch_source(entry, cache):
    """The local file for a sourced sound, downloaded into the cache once and checked against its SHA-256. A zip member is
    extracted from the cached archive."""
    url = entry["url"]
    name = urllib.parse.unquote(url.rsplit("/", 1)[1])
    cache.mkdir(parents=True, exist_ok=True)
    path = cache / (entry["sha256"][:16] + "_" + name)
    if not path.exists():
        request = urllib.request.Request(url, headers={"User-Agent": "CoreFactory-sound-build/1.0"})
        with urllib.request.urlopen(request, timeout=120) as response:
            data = response.read()
        if hashlib.sha256(data).hexdigest() != entry["sha256"]:
            raise SystemExit(f"SHA-256 mismatch for {url}")
        path.write_bytes(data)
    member = entry.get("member")
    if member is None:
        return path
    extracted = cache / (entry["sha256"][:16] + "_" + Path(member).name)
    if not extracted.exists():
        with zipfile.ZipFile(path) as archive:
            extracted.write_bytes(archive.read(member))
    return extracted


def decode_source(ffmpeg, path, start, duration, filters):
    """The samples of a source from start for duration seconds, mono at RATE, after the ffmpeg filter chain."""
    cmd = [ffmpeg, "-v", "error", "-ss", str(start), "-t", str(duration), "-i", str(path)]
    if filters:
        cmd += ["-af", filters]
    cmd += ["-ac", "1", "-ar", str(RATE), "-f", "f32le", "-"]
    data = subprocess.run(cmd, capture_output=True, check=True).stdout
    return list(array.array("f", data))


def sourced(entry, ffmpeg, cache):
    """A sourced sound, cut, filtered and looped as its entry in sources.json says."""
    path = fetch_source(entry, cache)
    loop_fade = entry.get("loop_fade", 0.0)
    duration = entry["length"] + loop_fade if entry.get("loop") else entry["length"]
    x = decode_source(ffmpeg, path, entry["start"], duration, entry.get("filter", ""))
    if entry.get("loop"):
        return crossfade_loop(x, entry["length"], loop_fade)
    if len(x) < samples(entry["length"]) * 0.98:
        raise SystemExit(f"{path.name}: only {len(x)} samples from {entry['start']}s, wanted {samples(entry['length'])}")
    return [v * g for v, g in zip(x, envelope(len(x), entry.get("fade_in", 0.002), entry.get("fade_out", 0.015)))]


def load_sources():
    with open(SOURCES, encoding="utf-8") as handle:
        return json.load(handle)


def write_wav(path, x):
    frames = b"".join(struct.pack("<h", max(-32768, min(32767, int(round(v * 32767))))) for v in x)
    with wave.open(str(path), "wb") as wav:
        wav.setnchannels(1)
        wav.setsampwidth(2)
        wav.setframerate(RATE)
        wav.writeframes(frames)


def ffmpeg_path(explicit):
    if explicit:
        return explicit
    try:
        import imageio_ffmpeg
        return imageio_ffmpeg.get_ffmpeg_exe()
    except ImportError:
        pass
    found = shutil.which("ffmpeg")
    if found is None:
        raise SystemExit("No ffmpeg found. Run: python -m pip install imageio-ffmpeg, or pass --ffmpeg PATH")
    return found


def encode(ffmpeg, wav, ogg):
    subprocess.run([ffmpeg, "-y", "-hide_banner", "-loglevel", "error", "-i", str(wav),
                    "-c:a", "libvorbis", "-q:a", "3", str(ogg)], check=True)


def decoded_peak_db(ffmpeg, ogg):
    """The peak level of the encoded file, as ffmpeg decodes it."""
    result = subprocess.run([ffmpeg, "-hide_banner", "-i", str(ogg), "-af", "volumedetect", "-f", "null", "-"],
                            capture_output=True, text=True, check=True)
    match = re.search(r"max_volume: (-?[\d.]+) dB", result.stderr)
    return float(match.group(1)) if match else float("nan")


def main():
    parser = argparse.ArgumentParser(description="Generate the CoreFactory machine sounds as OGG Vorbis.")
    parser.add_argument("--ffmpeg", help="path to an ffmpeg binary (default: imageio-ffmpeg, then the PATH)")
    parser.add_argument("--out", type=Path, default=DEFAULT_OUT, help="output folder for the .ogg files")
    parser.add_argument("--cache", type=Path, default=DEFAULT_CACHE, help="folder for the downloaded sources")
    parser.add_argument("--only", nargs="*", help="build only these keys")
    args = parser.parse_args()

    ffmpeg = ffmpeg_path(args.ffmpeg)
    args.out.mkdir(parents=True, exist_ok=True)
    sources = load_sources()
    jobs = [(key, "procedural", lambda b=builder: b(), level) for key, builder, level in SOUNDS]
    jobs += [(key, "sourced", lambda e=entry: sourced(e, ffmpeg, args.cache), entry["level"])
             for key, entry in sources.items()]
    if args.only:
        jobs = [job for job in jobs if job[0] in args.only]
    print(f"ffmpeg: {ffmpeg}")
    print(f"{'sound':<24}{'kind':<12}{'seconds':>8}{'wav peak':>10}{'ogg peak':>10}{'ogg bytes':>11}")
    with tempfile.TemporaryDirectory() as tmp:
        for key, kind, make, level in jobs:
            x = normalise(make(), level)
            wav = Path(tmp) / f"{key}.wav"
            ogg = args.out / f"{key}.ogg"
            write_wav(wav, x)
            encode(ffmpeg, wav, ogg)
            wav_peak = 20 * math.log10(max(abs(v) for v in x))
            print(f"{key:<24}{kind:<12}{len(x) / RATE:>8.2f}{wav_peak:>10.1f}{decoded_peak_db(ffmpeg, ogg):>10.1f}"
                  f"{ogg.stat().st_size:>11}")


if __name__ == "__main__":
    main()
