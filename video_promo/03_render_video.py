#!/usr/bin/env python3
"""Étape 3 : montage final de la vidéo promo verticale.

- Assemble les 8 clips ADB selon storyboard.json.
- Cadre 1080x1920 (9:16), 30 fps, H.264 crf 18, pixel art en neighbor.
- Sous-titres ASS générés depuis timeline.json et le texte exact du README.
- Musique d'ambiance bouclée et fondue, voix normalisée -14 LUFS.
- Écran de fin sur les 3 dernières secondes.

Usage :
    .venv\\Scripts\\python 03_render_video.py
    .venv\\Scripts\\python 03_render_video.py --no-render   # génère ASS + endcard seulement
"""

from __future__ import annotations

import argparse
import json
import math
import os
import re
import shutil
import subprocess
import sys
import unicodedata
from difflib import SequenceMatcher
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent
REPO = ROOT.parent
STORYBOARD = ROOT / "storyboard.json"
TIMELINE = ROOT / "timeline.json"
README = REPO / "README.md"
CLIPS = ROOT / "clips"
OUTPUT = ROOT / "la_marche_des_ames_vertical.mp4"
ASS_FILE = ROOT / "subtitles.ass"
ENDCARD = ROOT / "endcard.png"
FONT = REPO / "android/app/src/main/res/font/press_start_2p.ttf"
FONT_DIR = FONT.parent
MUSIC = REPO / "assets/music/crypts_1.ogg"
AUDIO = ROOT / "audio.mp3"

WIDTH = 1080
HEIGHT = 1920
FPS = 30
CRF = 18
MAIN_WIDTH = 864  # 1080 * 1920 / 2400 : la capture remplit toute la hauteur
SIDE = (WIDTH - MAIN_WIDTH) // 2
MAX_SUB_CHARS = 48
SUB_Y = 1420
END_SECONDS = 3.0

# Métriques mesurées sur le rendu libass (Press Start 2P, Fontsize=40) :
# 1 caractère = 29 px d'avance, hauteur d'encre 26 px, interligne 40 px,
# bloc de texte centré 7,5 px au-dessus du point d'ancrage \an5.
SUB_ADVANCE = 29.0
SUB_INK_H = 26.0
SUB_LINE_H = 40.0
SUB_BOX_CENTER_OFFSET = 7.5
SUB_BOX_PAD_X = 22
SUB_BOX_PAD_Y = 14
SUB_BOX_ALPHA = "66"  # ~60 % d'opacité, bien moins dense que l'écran de fin


def find_tool(name: str) -> str:
    candidates = [
        os.environ.get(name.upper()),
        shutil.which(name),
        rf"C:\Users\bruno\AndroidTools\ffmpeg\{name}.exe",
        str(ROOT / ".venv/Lib/site-packages/imageio_ffmpeg/binaries" / f"{name}.exe"),
        str(REPO / "tools/ffmpeg/bin" / f"{name}.exe"),
    ]
    for candidate in candidates:
        if candidate and Path(candidate).exists():
            return candidate
    raise SystemExit(
        f"{name} introuvable. Installe FFmpeg complet (avec libass) ou définis "
        f"{name.upper()}=chemin."
    )


def strip_accents(value: str) -> str:
    value = value.lower().replace("œ", "oe").replace("æ", "ae")
    value = unicodedata.normalize("NFD", value)
    return "".join(c for c in value if unicodedata.category(c) != "Mn")


def normalize_word(value: str) -> str:
    value = strip_accents(value)
    return re.sub(r"[^a-z0-9]+", "", value)


def read_reference_text() -> str:
    raw = README.read_text(encoding="utf-8")
    match = re.search(r"## Histoire\s*(.*?)(?=\n---)", raw, re.S)
    if not match:
        raise SystemExit("Section ## Histoire introuvable dans README.md")
    text = match.group(1)
    text = text.replace("*", " ").replace("—", " ").replace("–", " ")
    text = text.replace("«", "").replace("»", "")
    text = re.sub(r"\s+", " ", text).strip()
    return text


def ass_time(seconds: float) -> str:
    seconds = max(0.0, seconds)
    centis = int(round(seconds * 100))
    hours, rem = divmod(centis, 360000)
    minutes, rem = divmod(rem, 6000)
    secs, cs = divmod(rem, 100)
    return f"{hours}:{minutes:02d}:{secs:02d}.{cs:02d}"


def split_two_lines(text: str) -> str:
    if len(text) <= 24:
        return text
    middle = len(text) // 2
    candidates = [i for i, ch in enumerate(text) if ch == " "]
    if not candidates:
        return text
    cut = min(candidates, key=lambda i: abs(i - middle))
    return text[:cut].rstrip() + r"\N" + text[cut + 1 :].lstrip()


def build_subtitles(timeline: dict, reference: str) -> list[dict]:
    hyp: list[tuple[str, float, float]] = []
    for segment in timeline.get("segments") or []:
        for word in segment.get("words") or []:
            token = (word.get("word") or "").strip()
            if not token:
                continue
            hyp.append((token, float(word["start"]), float(word["end"])))
    if not hyp:
        raise SystemExit("timeline.json ne contient aucun mot daté.")

    ref_tokens = [token for token in reference.split() if token]
    ref_norm = [normalize_word(token) for token in ref_tokens]
    hyp_norm = [normalize_word(token) for token, _, _ in hyp]

    matcher = SequenceMatcher(None, hyp_norm, ref_norm, autojunk=False)
    ref_times: list[tuple[float, float] | None] = [None] * len(ref_tokens)
    matched = 0
    for block in matcher.get_matching_blocks():
        for k in range(block.size):
            ref_idx = block.b + k
            hyp_idx = block.a + k
            _, start, end = hyp[hyp_idx]
            ref_times[ref_idx] = (start, end)
            matched += 1

    known = [(i, t) for i, t in enumerate(ref_times) if t is not None]
    if not known:
        raise SystemExit("Alignement impossible entre la timeline et le README.")
    first_start = known[0][1][0]
    for i in range(0, known[0][0]):
        ref_times[i] = (0.0, max(0.01, first_start))
    for (i0, t0), (i1, t1) in zip(known, known[1:]):
        span = i1 - i0
        for k in range(i0 + 1, i1):
            frac = (k - i0) / span
            start = t0[0] + (t1[0] - t0[0]) * frac
            end = t0[1] + (t1[1] - t0[1]) * frac
            ref_times[k] = (start, end)
    last_end = known[-1][1][1]
    duration = float(timeline["duration"])
    for i in range(known[-1][0] + 1, len(ref_tokens)):
        ref_times[i] = (min(duration, last_end), min(duration, last_end + 0.01))

    # Phrases de référence, puis cartons de 2 lignes maximum.
    sentences: list[list[tuple[str, float, float]]] = []
    current: list[tuple[str, float, float]] = []
    for token, times in zip(ref_tokens, ref_times):
        current.append((token, times[0], times[1]))
        if token.endswith((".", "!", "?", "…")):
            sentences.append(current)
            current = []
    if current:
        sentences.append(current)

    cards: list[list[tuple[str, float, float]]] = []
    for sentence in sentences:
        chunk: list[tuple[str, float, float]] = []
        for item in sentence:
            candidate = " ".join([entry[0] for entry in chunk] + [item[0]])
            if chunk and len(candidate) > MAX_SUB_CHARS:
                cards.append(chunk)
                chunk = []
            chunk.append(item)
        if chunk:
            cards.append(chunk)

    result: list[dict] = []
    for index, card in enumerate(cards):
        text = " ".join(entry[0] for entry in card).strip()
        if not text:
            continue
        start = card[0][1]
        end = card[-1][2] + 0.06
        if index + 1 < len(cards):
            next_start = cards[index + 1][0][1]
            end = min(end, next_start - 0.02)
        end = min(end, duration)
        if end <= start:
            end = min(duration, start + 0.45)
        if end <= start:
            continue
        result.append(
            {
                "start": start,
                "end": end,
                "text": split_two_lines(text),
            }
        )

    # Pas de chevauchement.
    for index in range(len(result) - 1):
        if result[index]["end"] > result[index + 1]["start"]:
            result[index]["end"] = max(
                result[index]["start"] + 0.2,
                result[index + 1]["start"] - 0.02,
            )
    print(
        f"Sous-titres : {len(result)} cartons, {matched}/{len(ref_tokens)} mots "
        f"alignés sur le README."
    )
    return result


def subtitle_box(text: str) -> tuple[int, int, int, int]:
    """Rectangle du fond, ajusté à la largeur du texte et au nombre de lignes."""
    lines = text.split(r"\N")
    longest = max(len(line) for line in lines)
    ink_h = SUB_INK_H + SUB_LINE_H * (len(lines) - 1)
    center = SUB_Y - SUB_BOX_CENTER_OFFSET
    half_w = longest * SUB_ADVANCE / 2 + SUB_BOX_PAD_X
    half_h = ink_h / 2 + SUB_BOX_PAD_Y
    return (
        round(540 - half_w),
        round(center - half_h),
        round(540 + half_w),
        round(center + half_h),
    )


def write_ass(cards: list[dict]) -> None:
    if not FONT.exists():
        raise SystemExit(f"Police introuvable : {FONT}")
    header = f"""[Script Info]
Title: La Marche des Âmes - promo verticale
ScriptType: v4.00+
PlayResX: {WIDTH}
PlayResY: {HEIGHT}
WrapStyle: 2
ScaledBorderAndShadow: yes
YCbCr Matrix: TV.709

[V4+ Styles]
Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
Style: Marche,Press Start 2P,40,&H00FFFFFF,&H00FFFFFF,&H00000000,&H90000000,0,0,0,0,100,100,0,0,1,4,2,5,40,40,0,1

[Events]
Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
"""
    lines = []
    for card in cards:
        text = card["text"].replace("{", r"\{").replace("}", r"\}")
        x1, y1, x2, y2 = subtitle_box(card["text"])
        plate = (
            rf"{{\an7\pos(0,0)\p1\bord0\shad0\1c&H10080B&\1a&H{SUB_BOX_ALPHA}&}}"
            f"m {x1} {y1} l {x2} {y1} l {x2} {y2} l {x1} {y2}"
        )
        start = ass_time(card["start"])
        end = ass_time(card["end"])
        lines.append(f"Dialogue: 0,{start},{end},Marche,,0,0,0,,{plate}")
        lines.append(
            f"Dialogue: 1,{start},{end},Marche,,0,0,0,,"
            f"{{\\an5\\pos(540,{SUB_Y})}}{text}"
        )
    ASS_FILE.write_text(header + "\n".join(lines) + "\n", encoding="utf-8")
    print(f"ASS écrit : {ASS_FILE} (fond semi-transparent ajusté au texte)")


def make_endcard() -> None:
    if not FONT.exists():
        raise SystemExit(f"Police introuvable : {FONT}")
    image = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    panel = (80, 690, 1000, 1230)
    draw.rounded_rectangle(panel, radius=24, fill=(11, 8, 16, 225), outline=(232, 194, 122, 235), width=3)
    draw.line((150, 1110, 930, 1110), fill=(232, 194, 122, 90), width=2)

    title_font = ImageFont.truetype(str(FONT), 46)
    mention_font = ImageFont.truetype(str(FONT), 17)
    url_font = ImageFont.truetype(str(FONT), 15)
    small_font = ImageFont.truetype(str(FONT), 12)

    def draw_centered(text: str, font, y: int, fill, stroke: int = 3) -> None:
        draw.text(
            (WIDTH // 2 + 3, y + 3),
            text,
            font=font,
            fill=(0, 0, 0, 220),
            anchor="mm",
            stroke_width=stroke,
            stroke_fill=(0, 0, 0, 255),
        )
        draw.text(
            (WIDTH // 2, y),
            text,
            font=font,
            fill=fill,
            anchor="mm",
            stroke_width=stroke,
            stroke_fill=(0, 0, 0, 255),
        )

    draw_centered("LA MARCHE", title_font, 790, (246, 221, 166, 255), 4)
    draw_centered("DES ÂMES", title_font, 870, (246, 221, 166, 255), 4)
    draw_centered("RPG de marche Android", mention_font, 980, (240, 226, 196, 255), 2)
    draw_centered("· Garmin & Santé Connect ·", mention_font, 1020, (232, 194, 122, 255), 2)
    draw_centered("github.com/brunothesatellite/idle-step-rpg", url_font, 1090, (246, 221, 166, 255), 2)
    draw_centered("MERCI D'AVOIR MARCHÉ", small_font, 1160, (149, 135, 168, 255), 1)
    image.save(ENDCARD)
    print(f"Écran de fin écrit : {ENDCARD}")


def build_filtergraph(scenes: list[dict], duration: float, end_start: float) -> str:
    parts: list[str] = []
    concat_labels: list[str] = []
    for index, scene in enumerate(scenes):
        duration_scene = float(scene["duration"])
        label = f"v{index}"
        concat_labels.append(f"[{label}]")
        parts.append(
            f"[{index}:v]fps={FPS},scale={MAIN_WIDTH}:{HEIGHT}:flags=neighbor,"
            f"setsar=1,trim=start=0:duration={duration_scene:.6f},"
            f"setpts=PTS-STARTPTS,"
            f"pad={WIDTH}:{HEIGHT}:{SIDE}:0:color=0x0B0810[{label}]"
        )
    parts.append(f"{''.join(concat_labels)}concat=n={len(scenes)}:v=1:a=0[base]")
    parts.append(
        "[base]"
        "drawbox=x=106:y=0:w=2:h=1920:color=0xE8C27A@0.18:t=fill,"
        "drawbox=x=972:y=0:w=2:h=1920:color=0xE8C27A@0.18:t=fill[base2]"
    )
    parts.append(
        "[base2]ass=filename=video_promo/subtitles.ass:"
        "fontsdir=android/app/src/main/res/font[sub]"
    )
    parts.append(
        f"[10:v]format=rgba,fade=t=in:st=0:d=0.35:alpha=1,"
        f"setpts=PTS+{end_start:.6f}/TB[ec]"
    )
    parts.append("[sub][ec]overlay=0:0:eof_action=pass:shortest=0[vout]")
    parts.append(
        "[8:a]aresample=48000[main]"
    )
    parts.append(
        f"[9:a]atrim=start=0:end={duration:.6f},asetpts=PTS-STARTPTS,"
        f"volume=-22dB,afade=t=in:st=0:d=2,"
        f"afade=t=out:st={max(0.0, duration - 2.0):.6f}:d=2,aresample=48000[mus]"
    )
    parts.append(
        "[main][mus]amix=inputs=2:duration=first:dropout_transition=0:normalize=0[aout]"
    )
    return ";".join(parts)


def measure_loudness(ffmpeg: str, path: Path) -> tuple[float, float, float] | None:
    """Mesure I (LUFS), TP (dBTP), LRA (LU) via loudnorm en mode analyse."""
    analysis = subprocess.run(
        [
            ffmpeg, "-hide_banner", "-i", str(path.relative_to(REPO)),
            "-map", "0:a:0",
            "-af", "loudnorm=I=-14:TP=-1.5:LRA=11:print_format=json",
            "-f", "null", "-",
        ],
        cwd=str(REPO),
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    text = (analysis.stdout or "") + "\n" + (analysis.stderr or "")
    matches = re.findall(r"\{[\s\S]*?\}", text)
    if not matches:
        return None
    data = json.loads(matches[-1])
    try:
        return float(data["input_i"]), float(data["input_tp"]), float(data["input_lra"])
    except (KeyError, TypeError, ValueError):
        return None


def normalize_audio(path: Path, duration: float) -> None:
    """Amène la piste finale à -14 LUFS sans réencoder la vidéo.

    loudnorm seul sous-cible sur ce mix voix + musique (pic déjà à -1,3 dBTP),
    donc on applique un gain mesuré + un limiteur, puis on itère jusqu'à la cible.
    """
    ffmpeg = find_tool("ffmpeg")
    raw = path.with_name(path.stem + ".mix.mp4")
    path.replace(raw)

    measured = measure_loudness(ffmpeg, raw)
    if measured is None:
        raw.replace(path)
        print("  ! Mesure audio impossible, piste laissée telle quelle.")
        return
    start_i, start_tp, start_lra = measured
    print(f"  Mesure audio : I={start_i:.2f} LUFS, TP={start_tp:.2f} dBTP, LRA={start_lra:.2f} LU")

    target = -14.0
    gain = target - start_i
    accepted: Path | None = None
    for attempt in range(1, 5):
        candidate = path.with_name(f"{path.stem}.norm{attempt}.mp4")
        af = (
            f"volume={gain:.2f}dB,"
            "alimiter=limit=0.84:level=false:attack=1:release=20"
        )
        result = subprocess.run(
            [
                ffmpeg, "-y", "-hide_banner",
                "-i", str(raw.relative_to(REPO)),
                "-map", "0:v:0", "-map", "0:a:0",
                "-af", af,
                "-c:v", "copy", "-c:a", "aac", "-b:a", "192k",
                "-t", f"{duration:.6f}",
                "-movflags", "+faststart",
                str(candidate.relative_to(REPO)),
            ],
            cwd=str(REPO),
            capture_output=True,
            text=True,
            encoding="utf-8",
            errors="replace",
        )
        if result.returncode != 0:
            print(result.stderr[-3000:])
            raise SystemExit("Normalisation audio échouée.")
        got = measure_loudness(ffmpeg, candidate)
        if got is None:
            raise SystemExit("Mesure audio impossible après normalisation.")
        current_i, current_tp, current_lra = got
        print(
            f"  Passe {attempt} : gain={gain:+.2f} dB → I={current_i:.2f} LUFS, "
            f"TP={current_tp:.2f} dBTP, LRA={current_lra:.2f} LU"
        )
        if abs(current_i - target) <= 0.15:
            accepted = candidate
            break
        gain += target - current_i
        accepted = candidate

    if accepted is None:
        raw.replace(path)
        return
    for leftover in path.parent.glob(f"{path.stem}.norm*.mp4"):
        if leftover != accepted:
            leftover.unlink(missing_ok=True)
    accepted.replace(path)
    raw.unlink(missing_ok=True)
    print(f"  Audio normalisé à {target:.0f} LUFS (vidéo copiée, sans réencodage).")


def render(scenes: list[dict], duration: float) -> None:
    ffmpeg = find_tool("ffmpeg")
    ffprobe = find_tool("ffprobe")
    end_start = max(0.0, duration - END_SECONDS)
    graph = build_filtergraph(scenes, duration, end_start)

    inputs: list[str] = []
    for scene in scenes:
        clip = CLIPS / scene["clip"]
        if not clip.exists():
            raise SystemExit(f"Clip manquant : {clip}")
        inputs += ["-stream_loop", "-1", "-i", str(clip.relative_to(REPO))]
    inputs += ["-i", str(AUDIO.relative_to(REPO))]
    inputs += ["-stream_loop", "-1", "-i", str(MUSIC.relative_to(REPO))]
    inputs += [
        "-loop", "1", "-framerate", str(FPS), "-t", str(END_SECONDS),
        "-i", str(ENDCARD.relative_to(REPO)),
    ]

    command = [
        ffmpeg, "-y", "-hide_banner",
        *inputs,
        "-filter_complex", graph,
        "-map", "[vout]", "-map", "[aout]",
        "-t", f"{duration:.6f}",
        "-r", str(FPS),
        "-c:v", "libx264", "-preset", "medium", "-crf", str(CRF),
        "-pix_fmt", "yuv420p",
        "-c:a", "aac", "-b:a", "192k",
        "-movflags", "+faststart",
        str(OUTPUT.relative_to(REPO)),
    ]
    print(f"Rendu FFmpeg : {OUTPUT}")
    print(f"Durée : {duration:.3f} s · écran de fin à {end_start:.3f} s")
    result = subprocess.run(
        command,
        cwd=str(REPO),
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    if result.returncode != 0:
        print(result.stdout[-4000:])
        print(result.stderr[-8000:])
        raise SystemExit(f"FFmpeg a échoué ({result.returncode}).")
    print(result.stderr[-1200:])

    probe = subprocess.run(
        [
            ffprobe, "-v", "error",
            "-show_entries", "format=duration,size:stream=codec_name,width,height,r_frame_rate,pix_fmt",
            "-of", "json", str(OUTPUT.relative_to(REPO)),
        ],
        cwd=str(REPO),
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    print(probe.stdout)
    if OUTPUT.exists():
        print(f"Fichier final : {OUTPUT} ({OUTPUT.stat().st_size / 1_000_000:.1f} Mo)")


def main() -> int:
    for stream in (sys.stdout, sys.stderr):
        try:
            stream.reconfigure(encoding="utf-8", errors="replace")
        except Exception:
            pass

    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--no-render", action="store_true", help="Génère seulement ASS et endcard.")
    args = parser.parse_args()

    if not STORYBOARD.exists() or not TIMELINE.exists():
        raise SystemExit("storyboard.json ou timeline.json manquant.")
    storyboard = json.loads(STORYBOARD.read_text(encoding="utf-8"))
    timeline = json.loads(TIMELINE.read_text(encoding="utf-8"))
    reference = read_reference_text()
    cards = build_subtitles(timeline, reference)
    write_ass(cards)
    make_endcard()
    if args.no_render:
        return 0
    scenes = sorted(storyboard["scenes"], key=lambda scene: scene["sequence"])
    duration = float(storyboard["audio_duration"])
    render(scenes, duration)
    normalize_audio(OUTPUT, duration)
    return 0


if __name__ == "__main__":
    sys.exit(main())
