#!/usr/bin/env python3
"""Étape 1 de la vidéo promo : transcription et storyboard.

- Transcrit ``video_promo/audio.mp3`` en français avec faster-whisper.
- Écrit ``video_promo/timeline.json`` (segments + mots + timestamps).
- Associe chaque segment à l'une des 8 scènes et écrit
  ``video_promo/storyboard.json`` avec des durées exactes.

Exemples :
    .venv\\Scripts\\python 01_transcribe.py
    .venv\\Scripts\\python 01_transcribe.py --model small --device cpu
    .venv\\Scripts\\python 01_transcribe.py --from-timeline
"""

from __future__ import annotations

import argparse
import json
import math
import os
import re
import sys
import time
import unicodedata
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parent
AUDIO = ROOT / "audio.mp3"
TIMELINE = ROOT / "timeline.json"
STORYBOARD = ROOT / "storyboard.json"
LORE = ROOT.parent / "playstore" / "03-textes" / "lore.md"

PROMPT = (
    "Transcription en français d'un récit heroic fantasy pour un jeu vidéo. "
    "Kaelen, pèlerin, marche dans les Cryptes de Vael, les Catacombes, "
    "les Cavernes de cendre, la Forge noyée, les Cimes du Serment, "
    "le Bois des âmes, l'Abîme organique et le Trône des âmes. "
    "Dix pas pour un mètre, cent mètres pour un palier. "
    "La braise, le camp, la ferveur, les coffres, Garmin et Santé Connect."
)

_DLL_DIR_HANDLES: list = []

SCENES = [
    {
        "index": 1,
        "id": "01_intro_marche",
        "clip": "01_intro_marche.mp4",
        "title": "La marche de Kaelen",
        "description": "La marche de Kaelen dans les Cryptes de Vael (écran principal).",
        "direction": "Écran principal, héros en marche dans les Cryptes de Vael.",
    },
    {
        "index": 2,
        "id": "02_equipement",
        "clip": "02_equipement.mp4",
        "title": "Équipement et paper doll",
        "description": "L'écran d'inventaire et le paper doll du personnage, l'atelier d'amélioration.",
        "direction": "Onglet Inventaire, paper doll, puis onglet Améliorations.",
    },
    {
        "index": 3,
        "id": "03_combat_gardien",
        "clip": "03_combat_gardien.mp4",
        "title": "Combat et gardien",
        "description": "Un combat automatique contre un monstre ou un gardien de palier.",
        "direction": "Combat en cours, puis gardien de palier si possible.",
    },
    {
        "index": 4,
        "id": "04_biomes_codex",
        "clip": "04_biomes_codex.mp4",
        "title": "Biomes et Codex",
        "description": "Le Codex montrant les biomes et le bestiaire (ou défilement des paliers).",
        "direction": "Codex, onglet Biomes puis Bestiaire ; ou défilement des paliers.",
    },
    {
        "index": 5,
        "id": "05_camp_braise",
        "clip": "05_camp_braise.mp4",
        "title": "Campement et braise",
        "description": "L'écran du Campement avec la braise au centre et les soins.",
        "direction": "Campement, braise au centre, soins et veillée.",
    },
    {
        "index": 6,
        "id": "06_coffre_pas",
        "clip": "06_coffre_pas.mp4",
        "title": "Coffre et pas",
        "description": "L'ouverture d'un coffre ou le détail de la synchronisation des pas.",
        "direction": "Coffre en cours d'ouverture, ou écran de synchronisation des pas.",
    },
    {
        "index": 7,
        "id": "07_defaite_detour",
        "clip": "07_defaite_detour.mp4",
        "title": "Défaite, ferveur et forge",
        "description": "La gestion de la ferveur, des consommables ou de l'atelier pour reforger.",
        "direction": "Ferveur, consommables, ou atelier de reforger.",
    },
    {
        "index": 8,
        "id": "08_outro",
        "clip": "08_outro.mp4",
        "title": "La marche infinie",
        "description": "Retour sur la marche infinie de Kaelen.",
        "direction": "Retour à l'écran principal, marche dans le donjon.",
    },
]

# Ordre de montage : suit la narration de l'audio.
# Le plan 06 (pas/coffre) intervient pendant la phrase sur les pas, avant les
# biomes ; le plan 07 (défaite) conclut après l'outro.
EDIT_ORDER = [
    "01_intro_marche",
    "02_equipement",
    "03_combat_gardien",
    "06_coffre_pas",
    "04_biomes_codex",
    "05_camp_braise",
    "08_outro",
    "07_defaite_detour",
]

# Bornes manuelles sur les segments issus de la transcription GPU (40 phrases).
# (début inclus, fin exclue). Elles couvrent toute la timeline.
MANUAL_RANGES = {
    "01_intro_marche": (0, 8),
    "02_equipement": (8, 13),
    "03_combat_gardien": (13, 20),
    "06_coffre_pas": (20, 23),
    "04_biomes_codex": (23, 32),
    "05_camp_braise": (32, 35),
    "08_outro": (35, 38),
    "07_defaite_detour": (38, 40),
}

# Mots-clés pondérés. Les accents sont ignorés au matching.
SCENE_KEYWORDS: dict[str, dict[str, float]] = {
    "01_intro_marche": {
        "ame": 2.0, "vael": 3.0, "porte": 2.0, "escalier": 2.0, "donjon": 2.0,
        "kaelen": 2.0, "pelerin": 2.0, "marche": 2.0, "metre": 2.0, "palier": 2.0,
        "priere": 1.5, "lent": 1.5, "pierre": 1.0, "rune": 1.5, "terre": 1.0,
    },
    "02_equipement": {
        "besace": 3.0, "lame": 3.0, "equipement": 3.0, "inventaire": 3.0,
        "forge": 2.0, "amelioration": 2.0, "objet": 2.0, "arme": 2.0,
        "armure": 2.0, "pot": 1.5, "fer": 1.5, "braise": 1.0, "heritee": 2.0,
    },
    "03_combat_gardien": {
        "gardien": 3.0, "combat": 3.0, "ennemi": 2.5, "monstre": 2.5,
        "scelle": 2.5, "bourreau": 2.5, "douleur": 1.5, "attaque": 2.0,
        "victoire": 1.5, "epee": 1.5, "affronte": 2.0,
    },
    "04_biomes_codex": {
        "cryptes": 3.0, "catacombes": 3.0, "cavernes": 3.0, "cendre": 2.0,
        "forge noyee": 3.0, "cimes": 3.0, "serment": 2.0, "bois": 2.5,
        "abime": 3.0, "organique": 2.5, "trone": 3.0, "biome": 3.0,
        "codex": 3.0, "bestiaire": 2.5, "salle": 1.5, "racines": 1.5,
    },
    "05_camp_braise": {
        "camp": 3.0, "braise": 3.0, "feu": 2.0, "repos": 2.0, "soin": 2.0,
        "rechauffent": 2.5, "pot": 1.5, "veille": 2.0, "centre": 1.5,
    },
    "06_coffre_pas": {
        "coffre": 3.0, "synchronisation": 3.0, "garmin": 3.0,
        "sante connect": 3.0, "montre": 2.5, "distance": 2.0, "serrure": 2.5,
        "cle": 1.5, "pas": 1.0, "quotidien": 1.5, "compteur": 2.0,
    },
    "07_defaite_detour": {
        "defaite": 3.0, "detour": 3.0, "recul": 2.5, "arriere": 2.0,
        "or": 2.0, "ferveur": 3.0, "consommable": 3.0, "reforger": 3.0,
        "atelier": 2.5, "vingt": 2.0, "recommence": 2.0,
    },
    "08_outro": {
        "fin": 3.0, "prochaine": 3.0, "respirer": 3.0, "continue": 2.5,
        "infinie": 3.0, "kaelen": 1.0, "toujours": 1.5, "marche": 1.0,
    },
}


def strip_accents(value: str) -> str:
    return "".join(
        c for c in unicodedata.normalize("NFD", value)
        if unicodedata.category(c) != "Mn"
    )


def normalize(value: str) -> str:
    value = strip_accents(value.lower())
    value = re.sub(r"[^a-z0-9]+", " ", value)
    return re.sub(r"\s+", " ", value).strip()


def fmt_time(seconds: float) -> str:
    seconds = max(0.0, float(seconds))
    minutes = int(seconds // 60)
    return f"{minutes:02d}:{seconds % 60:06.3f}"


def load_reference_text() -> tuple[str, str]:
    if not LORE.exists():
        return "", ""
    raw = LORE.read_text(encoding="utf-8")
    long_text = raw.split("## Version courte")[0]
    long_text = re.sub(r"^#.*$", "", long_text, flags=re.MULTILINE)
    long_text = long_text.replace("*", " ").replace("—", " ").replace("–", " ")
    long_text = re.sub(r"\s+", " ", long_text).strip()
    return long_text, str(LORE.relative_to(ROOT.parent))


def load_audio_duration(path: Path) -> float:
    try:
        import av

        with av.open(str(path)) as container:
            return float(container.duration / av.time_base)
    except Exception:
        return 0.0


def add_nvidia_dll_dirs() -> None:
    """Ajoute les DLL CUDA installées par pip (cuBLAS, cuDNN, runtime).

    Le pilote NVIDIA ne fournit pas ces DLL ; CTranslate2 les cherche dans le
    PATH ou via os.add_dll_directory sous Windows.
    """
    if os.name != "nt":
        return
    base = Path(sys.prefix) / "Lib" / "site-packages" / "nvidia"
    if not base.exists():
        return
    for directory in sorted(base.glob("*/bin")):
        if not directory.is_dir():
            continue
        try:
            _DLL_DIR_HANDLES.append(os.add_dll_directory(str(directory)))
        except OSError:
            pass
        os.environ["PATH"] = str(directory) + os.pathsep + os.environ.get("PATH", "")


def transcribe(args) -> dict:
    add_nvidia_dll_dirs()
    try:
        from faster_whisper import WhisperModel
    except ImportError as exc:
        raise SystemExit(
            "faster-whisper n'est pas installé. Lance : "
            ".venv\\Scripts\\python -m pip install faster-whisper"
        ) from exc

    if not AUDIO.exists():
        raise SystemExit(f"Audio introuvable : {AUDIO}")

    device = args.device
    compute_type = args.compute_type

    def make_model(dev: str, compute: str):
        return WhisperModel(
            args.model,
            device=dev,
            compute_type=compute,
            cpu_threads=args.cpu_threads,
            num_workers=1,
        )

    def run(dev: str, compute: str):
        print(f"Chargement du modèle {args.model} sur {dev} ({compute})…")
        model = make_model(dev, compute)
        print(f"Transcription de {AUDIO.name} ({args.model}, {dev}/{compute})…")
        segments_iter, info = model.transcribe(
            str(AUDIO),
            language="fr",
            beam_size=args.beam_size,
            best_of=args.beam_size,
            temperature=0.0,
            word_timestamps=True,
            vad_filter=True,
            vad_parameters={"min_silence_duration_ms": 500, "speech_pad_ms": 200},
            initial_prompt=PROMPT,
            condition_on_previous_text=True,
            no_speech_threshold=0.6,
            log_prob_threshold=-1.0,
        )

        segments: list[dict] = []
        full_text_parts: list[str] = []
        for index, segment in enumerate(segments_iter):
            text = (segment.text or "").strip()
            words = []
            for word in segment.words or []:
                if word.start is None or word.end is None:
                    continue
                words.append(
                    {
                        "word": (word.word or "").strip(),
                        "start": round(float(word.start), 3),
                        "end": round(float(word.end), 3),
                        "probability": round(float(word.probability or 0.0), 4),
                    }
                )
            item = {
                "id": index,
                "start": round(float(segment.start), 3),
                "end": round(float(segment.end), 3),
                "text": text,
                "words": words,
            }
            segments.append(item)
            full_text_parts.append(text)
            print(f"[{fmt_time(item['start'])} -> {fmt_time(item['end'])}] {text}")
        return segments, full_text_parts, info

    started = time.time()
    try:
        if device == "auto":
            try:
                segments, full_text_parts, info = run("cuda", "float16")
                device, compute_type = "cuda", "float16"
            except Exception as exc:  # cublas/cuDNN absent ou GPU occupé
                print(f"CUDA indisponible ou échec ({exc}). Repli CPU int8.")
                segments, full_text_parts, info = run("cpu", "int8")
                device, compute_type = "cpu", "int8"
        else:
            segments, full_text_parts, info = run(device, compute_type)
    except Exception as exc:
        if device == "cuda":
            print(f"Échec CUDA ({exc}). Repli CPU int8.")
            segments, full_text_parts, info = run("cpu", "int8")
            device, compute_type = "cpu", "int8"
        else:
            raise

    duration = float(info.duration or 0.0)
    if duration <= 0.0:
        duration = load_audio_duration(AUDIO)
    if duration <= 0.0:
        duration = segments[-1]["end"] if segments else 0.0

    reference_text, reference_file = load_reference_text()
    timeline = {
        "version": 1,
        "source_audio": str(AUDIO.relative_to(ROOT.parent)).replace("\\", "/"),
        "duration": round(duration, 3),
        "duration_after_vad": round(float(info.duration_after_vad or duration), 3),
        "language": info.language or "fr",
        "language_probability": round(float(info.language_probability or 0.0), 4),
        "model": args.model,
        "device": device,
        "compute_type": compute_type,
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "transcription_seconds": round(time.time() - started, 2),
        "reference_file": reference_file,
        "reference_text": reference_text,
        "segments": segments,
        "full_text": " ".join(full_text_parts).strip(),
    }
    return resegment_timeline(timeline)


def words_to_text(words: list[dict]) -> str:
    raw = " ".join((word.get("word") or "").strip() for word in words)
    raw = re.sub(r"\s+", " ", raw)
    raw = re.sub(r"\s+([,.!?;:…»])", r"\1", raw)
    raw = re.sub(r"\s+(['’])", r"\1", raw)
    raw = re.sub(r"(['’])\s+", r"\1", raw)
    raw = re.sub(r"([«(])\s+", r"\1", raw)
    return raw.strip()


def split_segments_into_sentences(segments: list[dict]) -> list[dict]:
    """Découpe les longs segments Whisper aux ponctuations fortes.

    Les timestamps des mots sont conservés. Cela donne des unités exploitables
    pour le storyboard et les sous-titres, même quand Whisper fusionne plusieurs
    phrases dans un seul segment.
    """

    def make_chunk(words: list[dict], raw_id: int) -> dict:
        return {
            "id": 0,
            "start": round(float(words[0]["start"]), 3),
            "end": round(float(words[-1]["end"]), 3),
            "text": words_to_text(words),
            "words": words,
            "raw_segment_id": raw_id,
        }

    result: list[dict] = []
    for segment in segments:
        words = segment.get("words") or []
        if not words:
            result.append(
                {
                    "id": 0,
                    "start": segment.get("start", 0.0),
                    "end": segment.get("end", 0.0),
                    "text": segment.get("text", ""),
                    "words": [],
                    "raw_segment_id": segment.get("id"),
                }
            )
            continue

        chunk: list[dict] = []
        for index, word in enumerate(words):
            chunk.append(word)
            word_text = (word.get("word") or "").strip()
            next_start = words[index + 1]["start"] if index + 1 < len(words) else None
            ends_sentence = bool(re.search(r"[.!?…]$", word_text))
            long_pause = (
                next_start is not None
                and float(next_start) - float(word["end"]) >= 0.9
                and len(chunk) >= 4
            )
            if ends_sentence or long_pause:
                result.append(make_chunk(chunk, int(segment.get("id", 0))))
                chunk = []
        if chunk:
            result.append(make_chunk(chunk, int(segment.get("id", 0))))

    for index, segment in enumerate(result):
        segment["id"] = index
    return result


def resegment_timeline(timeline: dict) -> dict:
    raw = timeline.get("segments") or []
    if not raw:
        return timeline
    timeline = dict(timeline)
    timeline["segments"] = split_segments_into_sentences(raw)
    timeline["segmenter"] = "sentences"
    timeline["full_text"] = " ".join(
        segment.get("text", "") for segment in timeline["segments"]
    ).strip()
    return timeline


def repair_timeline_texts(timeline: dict) -> dict:
    for segment in timeline.get("segments") or []:
        if segment.get("words"):
            segment["text"] = words_to_text(segment["words"])
    timeline["full_text"] = " ".join(
        segment.get("text", "") for segment in timeline.get("segments") or []
    ).strip()
    return timeline


def segment_score(text: str, scene_id: str) -> float:
    normalized = normalize(text)
    total = 0.0
    for keyword, weight in SCENE_KEYWORDS[scene_id].items():
        key = normalize(keyword)
        if not key:
            continue
        if key.endswith("s"):
            variants = {key, key[:-1]}
        else:
            variants = {key, key + "s"}
        pattern = r"\b(" + "|".join(re.escape(v) for v in sorted(variants, key=len, reverse=True)) + r")\b"
        total += len(re.findall(pattern, normalized)) * weight
    return total


def build_storyboard(timeline: dict) -> dict:
    segments = timeline.get("segments") or []
    duration = float(timeline.get("duration") or 0.0)
    if not segments:
        raise SystemExit("timeline.json ne contient aucun segment.")

    scene_ids = [scene["id"] for scene in SCENES]
    count = len(segments)
    scene_count = len(scene_ids)
    if count < scene_count:
        raise SystemExit(
            f"Pas assez de segments ({count}) pour les {scene_count} scènes."
        )

    scores = [
        [segment_score(segment.get("text", ""), scene_id) for scene_id in scene_ids]
        for segment in segments
    ]

    # Programmation dynamique : 8 blocs contigus, au moins 1 segment par bloc.
    # dp[k][i] = meilleur score en ayant traité i segments avec k scènes.
    neg_inf = -1e18
    dp = [[neg_inf] * (count + 1) for _ in range(scene_count + 1)]
    previous = [[-1] * (count + 1) for _ in range(scene_count + 1)]
    dp[0][0] = 0.0
    for k in range(1, scene_count + 1):
        for i in range(k, count - (scene_count - k) + 1):
            running = 0.0
            for j in range(i - 1, k - 2, -1):
                running += scores[j][k - 1]
                if dp[k - 1][j] <= neg_inf:
                    continue
                # Léger bonus de régularité : évite les blocs d'un seul segment.
                length = i - j
                regular = -0.08 * abs(length - count / scene_count)
                value = dp[k - 1][j] + running + regular
                if value > dp[k][i]:
                    dp[k][i] = value
                    previous[k][i] = j

    if dp[scene_count][count] <= neg_inf:
        raise SystemExit("Impossible de répartir les segments sur les 8 scènes.")

    ranges: list[tuple[int, int]] = []
    i = count
    for k in range(scene_count, 0, -1):
        j = previous[k][i]
        if j < 0:
            raise SystemExit("Répartition interne invalide.")
        ranges.append((j, i))
        i = j
    ranges.reverse()

    scene_by_id = {scene["id"]: scene for scene in SCENES}
    manual_ok = (
        len(EDIT_ORDER) == len(SCENES)
        and all(scene_id in MANUAL_RANGES for scene_id in EDIT_ORDER)
        and all(0 <= start < end <= count for start, end in MANUAL_RANGES.values())
        and MANUAL_RANGES[EDIT_ORDER[0]][0] == 0
        and MANUAL_RANGES[EDIT_ORDER[-1]][1] == count
        and all(
            MANUAL_RANGES[EDIT_ORDER[i]][1] == MANUAL_RANGES[EDIT_ORDER[i + 1]][0]
            for i in range(len(EDIT_ORDER) - 1)
        )
    )
    if manual_ok:
        order = EDIT_ORDER
        ranges_by_id = {scene_id: MANUAL_RANGES[scene_id] for scene_id in order}
    else:
        print(
            "Attention : bornes manuelles invalides, repli sur la répartition "
            "automatique par mots-clés."
        )
        order = [scene["id"] for scene in SCENES]
        ranges_by_id = {
            scene["id"]: ranges[index] for index, scene in enumerate(SCENES)
        }

    storyboard_scenes = []
    for position, scene_id in enumerate(order):
        scene = scene_by_id[scene_id]
        start_idx, end_idx = ranges_by_id[scene_id]
        block = segments[start_idx:end_idx]
        start = 0.0 if position == 0 else float(block[0]["start"])
        end = duration if position == len(order) - 1 else float(segments[end_idx]["start"])
        if end <= start:
            end = max(start + 0.001, float(block[-1]["end"]))
        text = " ".join(part.get("text", "") for part in block).strip()
        word_count = sum(len(part.get("words") or []) for part in block)
        storyboard_scenes.append(
            {
                "index": scene["index"],
                "sequence": position + 1,
                "id": scene["id"],
                "clip": scene["clip"],
                "title": scene["title"],
                "description": scene["description"],
                "direction": scene["direction"],
                "start": round(start, 3),
                "end": round(end, 3),
                "duration": round(end - start, 3),
                "duration_frames_30fps": int(round((end - start) * 30)),
                "segment_ids": [part["id"] for part in block],
                "segment_start": block[0]["id"],
                "segment_end": block[-1]["id"],
                "text": text,
                "word_count": word_count,
            }
        )

    # Couverture exacte : le premier commence à 0, le dernier finit à la durée.
    if storyboard_scenes:
        storyboard_scenes[0]["start"] = 0.0
        storyboard_scenes[0]["end"] = round(storyboard_scenes[0]["end"], 3)
        storyboard_scenes[-1]["end"] = round(duration, 3)
        # Rend les bornes contiguës : chaque scène commence où la précédente finit.
        for index in range(1, len(storyboard_scenes)):
            storyboard_scenes[index]["start"] = storyboard_scenes[index - 1]["end"]
        for scene in storyboard_scenes:
            scene["start"] = round(scene["start"], 3)
            scene["end"] = round(scene["end"], 3)
            scene["duration"] = round(max(0.0, scene["end"] - scene["start"]), 3)
            scene["duration_frames_30fps"] = int(round(scene["duration"] * 30))

    total = round(sum(scene["duration"] for scene in storyboard_scenes), 3)
    gap = round(duration - total, 3)
    if storyboard_scenes and abs(gap) >= 0.001:
        last = storyboard_scenes[-1]
        last["duration"] = round(last["duration"] + gap, 3)
        last["duration_frames_30fps"] = int(round(last["duration"] * 30))
        total = round(sum(scene["duration"] for scene in storyboard_scenes), 3)
        gap = round(duration - total, 3)
    return {
        "version": 1,
        "source_audio": timeline.get("source_audio", "video_promo/audio.mp3"),
        "audio_duration": round(duration, 3),
        "fps": 30,
        "resolution": "1080x1920",
        "scene_count": len(storyboard_scenes),
        "edit_order": order,
        "using_manual_ranges": manual_ok,
        "capture_margin_seconds": 3,
        "total_duration": round(total, 3),
        "coverage_start": storyboard_scenes[0]["start"] if storyboard_scenes else 0.0,
        "coverage_end": storyboard_scenes[-1]["end"] if storyboard_scenes else 0.0,
        "coverage_gap": round(duration - total, 3),
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "notes": (
            "L'ordre du tableau scenes est l'ordre de montage, qui suit la narration : "
            "le plan 06 (pas/coffre) intervient pendant la phrase sur les pas, avant les biomes, "
            "et le plan 07 (défaite) conclut après l'outro. Les numéros 01-08 sont les "
            "identifiants de scène. Les durées sont celles du montage final ; l'étape 2 "
            "ajoute +3 s de marge à l'enregistrement ADB, puis le montage recoupe à la durée exacte."
        ),
        "scenes": storyboard_scenes,
    }


def main() -> int:
    # Console Windows : éviter un crash si le texte contient des caractères
    # non représentables en cp1252 (flèches, emoji, etc.).
    for stream in (sys.stdout, sys.stderr):
        try:
            stream.reconfigure(encoding="utf-8", errors="replace")
        except Exception:
            pass

    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--model", default="medium", help="Modèle Whisper (small, medium, large-v3…)")
    parser.add_argument("--device", choices=("auto", "cuda", "cpu"), default="auto")
    parser.add_argument("--compute-type", default="float16")
    parser.add_argument("--cpu-threads", type=int, default=12)
    parser.add_argument("--beam-size", type=int, default=5)
    parser.add_argument(
        "--from-timeline",
        action="store_true",
        help="Réutilise timeline.json et reconstruit seulement storyboard.json.",
    )
    args = parser.parse_args()

    ROOT.mkdir(parents=True, exist_ok=True)

    if args.from_timeline:
        if not TIMELINE.exists():
            raise SystemExit(f"Timeline introuvable : {TIMELINE}")
        timeline = json.loads(TIMELINE.read_text(encoding="utf-8"))
        if timeline.get("segmenter") != "sentences":
            timeline = resegment_timeline(timeline)
            TIMELINE.write_text(
                json.dumps(timeline, ensure_ascii=False, indent=2),
                encoding="utf-8",
            )
            print(f"Timeline resegmentée : {TIMELINE}")
        else:
            timeline = repair_timeline_texts(timeline)
            TIMELINE.write_text(
                json.dumps(timeline, ensure_ascii=False, indent=2),
                encoding="utf-8",
            )
            print(f"Timeline réutilisée : {TIMELINE}")
        for segment in timeline.get("segments", []):
            print(
                f"  {segment['id']:02d} "
                f"[{fmt_time(segment['start'])} -> {fmt_time(segment['end'])}] "
                f"{segment['text']}"
            )
    else:
        timeline = transcribe(args)
        TIMELINE.write_text(
            json.dumps(timeline, ensure_ascii=False, indent=2),
            encoding="utf-8",
        )
        print(f"\nTimeline écrite : {TIMELINE}")

    storyboard = build_storyboard(timeline)
    STORYBOARD.write_text(
        json.dumps(storyboard, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    print(f"Storyboard écrit : {STORYBOARD}")
    print(
        f"Durée audio : {storyboard['audio_duration']:.3f} s · "
        f"couverture : {storyboard['total_duration']:.3f} s · "
        f"écart : {storyboard['coverage_gap']:.3f} s"
    )
    for scene in storyboard["scenes"]:
        print(
            f"  {scene['clip']:<24} {scene['start']:8.3f} → {scene['end']:8.3f} "
            f"({scene['duration']:7.3f} s, {scene['segment_ids'][0]}–{scene['segment_ids'][-1]})"
        )
    return 0


if __name__ == "__main__":
    sys.exit(main())
