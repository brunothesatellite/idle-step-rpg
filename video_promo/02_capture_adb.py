#!/usr/bin/env python3
"""Étape 2 : capture ADB des 8 scènes du storyboard.

Le script :
- vérifie l'appareil ADB et lance l'application debug ;
- sauvegarde les données du package debug ;
- injecte un état adapté à chaque scène (palier, pas, coffre, buffs…) ;
- navigue dans l'application avec UI Automator ;
- enregistre chaque clip avec screenrecord (+3 s de marge) ;
- rapatrie le clip dans video_promo/clips/ et supprime la copie du téléphone ;
- restaure les données du package debug à la fin.

Exemples :
    .venv\\Scripts\\python 02_capture_adb.py
    .venv\\Scripts\\python 02_capture_adb.py --auto
    .venv\\Scripts\\python 02_capture_adb.py 03_combat_gardien.mp4
"""

from __future__ import annotations

import argparse
import copy
import json
import math
import os
import re
import shutil
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from datetime import date, datetime
from pathlib import Path

ROOT = Path(__file__).resolve().parent
STORYBOARD = ROOT / "storyboard.json"
CLIPS = ROOT / "clips"
BACKUP_DIR = ROOT / ".capture_backup"
UI_XML = ROOT / ".capture_ui.xml"
PACKAGE = "com.brunothesatellite.idlesteprpg.debug"
ACTIVITY = "com.brunothesatellite.idlesteprpg.MainActivity"

BACKUP_FILES = [
    "files/game_state.json",
    "files/game_state.bak.json",
    "shared_prefs/app_settings.xml",
    "shared_prefs/step_sync.xml",
]


def find_adb() -> str:
    candidates = [
        os.environ.get("ADB"),
        shutil.which("adb"),
        str(Path(os.environ.get("LOCALAPPDATA", "")) / "Android/Sdk/platform-tools/adb.exe"),
        str(Path(os.environ.get("ANDROID_HOME", "")) / "platform-tools/adb.exe"),
    ]
    for candidate in candidates:
        if candidate and Path(candidate).exists():
            return candidate
    raise SystemExit("adb introuvable. Installe les platform-tools ou définis ADB.")


ADB = find_adb()
SERIAL: str | None = None


def run(args: list[str], check: bool = False, binary: bool = False):
    result = subprocess.run(
        args,
        capture_output=not binary,
        text=not binary,
        encoding=None if binary else "utf-8",
        errors=None if binary else "replace",
    )
    if check and result.returncode != 0:
        raise SystemExit(f"Commande échouée : {' '.join(args)}\n{result.stderr}")
    return result


def adb(*args: str, check: bool = False):
    assert SERIAL
    return run([ADB, "-s", SERIAL, *args], check=check)


def shell(command: str, check: bool = False):
    return adb("shell", command, check=check)


def shell_out(command: str) -> str:
    return shell(command).stdout.strip()


def connect() -> str:
    global SERIAL
    requested = os.environ.get("ANDROID_SERIAL", "").strip()
    devices: list[str] = []
    try:
        output = run([ADB, "devices"]).stdout
    except Exception as exc:
        raise SystemExit(f"Impossible de lancer adb : {exc}") from exc
    for line in output.splitlines():
        parts = line.split()
        if len(parts) >= 2 and parts[1] == "device":
            devices.append(parts[0])
    if requested:
        if requested not in devices:
            raise SystemExit(
                f"Appareil ANDROID_SERIAL={requested} introuvable. "
                f"Appareils connectés : {devices or 'aucun'}"
            )
        SERIAL = requested
    elif devices:
        SERIAL = devices[0]
        if len(devices) > 1:
            print(f"Plusieurs appareils détectés : {devices}. Utilisation de {SERIAL}.")
    else:
        raise SystemExit(
            "Aucun appareil ADB connecté.\n"
            "Branche le téléphone, déverrouille-le et accepte l'autorisation USB, "
            "puis relance le script."
        )
    print(f"Appareil ADB : {SERIAL}")
    return SERIAL


def force_stop() -> None:
    shell(f"am force-stop {PACKAGE}")


def package_installed() -> bool:
    output = shell_out(f"pm path {PACKAGE}")
    return "package:" in output


def backup_data() -> None:
    BACKUP_DIR.mkdir(parents=True, exist_ok=True)
    force_stop()
    saved = 0
    for relative in BACKUP_FILES:
        result = adb("exec-out", "run-as", PACKAGE, "cat", relative)
        if result.returncode == 0 and result.stdout:
            target = BACKUP_DIR / relative.replace("/", "__")
            target.write_bytes(result.stdout if isinstance(result.stdout, bytes)
                               else result.stdout.encode("utf-8"))
            saved += 1
    if not (BACKUP_DIR / "files__game_state.json").exists():
        raise SystemExit(
            "Impossible de sauvegarder files/game_state.json du package debug. "
            "Vérifie que le package debug est installé et debuggable."
        )
    print(f"Sauvegarde debug : {saved} fichier(s) dans {BACKUP_DIR}")


def restore_data() -> None:
    if not BACKUP_DIR.exists():
        return
    force_stop()
    shell(f"run-as {PACKAGE} mkdir -p files shared_prefs")
    for relative in BACKUP_FILES:
        local = BACKUP_DIR / relative.replace("/", "__")
        if not local.exists():
            continue
        remote = f"/data/local/tmp/capture_restore"
        adb("push", str(local), remote)
        shell(f"run-as {PACKAGE} cp {remote} {relative}")
        shell(f"rm -f {remote}")
    print("Données du package debug restaurées.")


def load_base_state() -> dict:
    path = BACKUP_DIR / "files__game_state.json"
    return json.loads(path.read_text(encoding="utf-8"))


def push_state(state: dict) -> None:
    text = json.dumps(state, ensure_ascii=False)
    json.loads(text)  # validation avant écriture
    temporary = ROOT / ".capture_state.json"
    temporary.write_text(text, encoding="utf-8")
    force_stop()
    adb("push", str(temporary), "/data/local/tmp/capture_state.json")
    shell(f"run-as {PACKAGE} mkdir -p files")
    shell(f"run-as {PACKAGE} cp /data/local/tmp/capture_state.json files/game_state.json")
    shell("rm -f /data/local/tmp/capture_state.json")
    temporary.unlink(missing_ok=True)


def now_ms() -> int:
    return int(time.time() * 1000)


def disable_step_sync() -> None:
    """Force la source Garmin (sans clé) pour éviter les pas Santé Connect.

    Pendant la capture, la synchro Santé Connect ajouterait les pas réels du
    jour et ferait avancer le héros hors du plan. On garde la sauvegarde
    d'origine et on la restaure à la fin.
    """
    local = BACKUP_DIR / "shared_prefs__app_settings.xml"
    if not local.exists():
        print("  ! app_settings.xml absent de la sauvegarde ; synchro non désactivée.")
        return
    text = local.read_text(encoding="utf-8")
    updated = re.sub(
        r'(<string name="step_source">)[^<]*(</string>)',
        r"\1garmin\2",
        text,
    )
    if updated == text:
        return
    temporary = ROOT / ".capture_app_settings.xml"
    temporary.write_text(updated, encoding="utf-8")
    force_stop()
    adb("push", str(temporary), "/data/local/tmp/capture_app_settings.xml")
    shell(
        f"run-as {PACKAGE} cp /data/local/tmp/capture_app_settings.xml "
        "shared_prefs/app_settings.xml"
    )
    shell("rm -f /data/local/tmp/capture_app_settings.xml")
    temporary.unlink(missing_ok=True)
    print("  Synchro des pas désactivée pour la capture (source Garmin sans clé).")


def scene_state(base: dict, clip: str) -> dict:
    state = copy.deepcopy(base)
    now = now_ms()
    state["day"] = date.today().isoformat()
    state["lastActiveAt"] = now
    state["offlineReport"] = None
    state["special"] = None
    state["fight"] = None
    state["mode"] = "walk"
    state["chest"] = None
    state["chestQueue"] = []
    state["buffs"] = []
    state["distance"] = 0.0
    state["stepBank"] = 0.0
    state["nextFight"] = 10
    state["hp"] = 999999.0
    state["ferveur"] = 80

    if clip == "01_intro_marche.mp4":
        state["floor"] = 1
        state["mode"] = "walk"
        state["stepBank"] = 900.0
        state["nextFight"] = 9999
    elif clip == "02_equipement.mp4":
        state["floor"] = 17
        state["mode"] = "camp"
        state["stepBank"] = 10000.0
    elif clip == "03_combat_gardien.mp4":
        state["floor"] = 10
        state["mode"] = "boss"
        state["distance"] = 99.0
        state["stepBank"] = 0.0
        state["nextFight"] = 9999
        state["ferveur"] = 100
        state["fight"] = {
            "enemyName": "Roi des Cryptes",
            "enemyIcon": "mon/lich.png",
            "enemyLevel": 25,
            "enemyHp": 50000.0,
            "enemyMaxHp": 50000.0,
            "enemyAtk": 20.0,
            "enemyIntervalMs": 1250,
            "gold": 500,
            "xp": 800,
            "isBoss": True,
            "elite": False,
            "farm": False,
            "heroTimerMs": 380.0,
            "enemyTimerMs": 900.0,
            "elapsedMs": 0.0,
            "specialMs": 8000.0,
            "usedRation": False,
            "modifier": "RÉGÉNÉRANT",
            "lastCrit": False,
            "ability": "drain",
            "abilityLabel": "DRAIN D'ÂME",
            "abilityFlashMs": 0.0,
            "shieldMs": 0.0,
        }
    elif clip == "04_biomes_codex.mp4":
        state["floor"] = 71
        state["mode"] = "camp"
        state["stepBank"] = 10000.0
        state["ferveur"] = 100
    elif clip == "05_camp_braise.mp4":
        state["floor"] = 21
        state["mode"] = "camp"
        state["stepBank"] = 10000.0
        state["hp"] = 5000.0
        state["ferveur"] = 80
    elif clip == "06_coffre_pas.mp4":
        state["floor"] = 17
        state["mode"] = "walk"
        state["stepBank"] = 10000.0
        state["nextFight"] = 9999
        state["chest"] = {
            "type": "fer",
            "quota": 400,
            "progress": 0.0,
            "goldMin": 130,
            "goldMax": 220,
            "rarity": "rare",
            "expiresAt": now + 24 * 60 * 60 * 1000,
            "expiryNotified": False,
        }
    elif clip == "07_defaite_detour.mp4":
        state["floor"] = 31
        state["mode"] = "fight"
        state["stepBank"] = 0.0
        state["distance"] = 50.0
        state["hp"] = 1.0
        state["ferveur"] = 35
        state["nextFight"] = 9999
        state["buffs"] = [
            {"type": "atk", "magnitude": 0.4, "expiresAt": now + 120000, "label": "RAGE"},
            {"type": "def", "magnitude": 0.35, "expiresAt": now + 120000, "label": "ÉCORCE"},
            {"type": "vulnerable", "magnitude": 0.15, "expiresAt": now + 120000, "label": "FRAGILE"},
        ]
        state["fight"] = {
            "enemyName": "Roi des Cryptes",
            "enemyIcon": "mon/lich.png",
            "enemyLevel": 25,
            "enemyHp": 50000.0,
            "enemyMaxHp": 50000.0,
            "enemyAtk": 99999.0,
            "enemyIntervalMs": 4000,
            "gold": 0,
            "xp": 0,
            "isBoss": True,
            "elite": False,
            "farm": False,
            "heroTimerMs": 99999.0,
            "enemyTimerMs": 4000.0,
            "elapsedMs": 0.0,
            "specialMs": 8000.0,
            "usedRation": False,
            "modifier": "BERSERK",
            "lastCrit": False,
            "ability": "drain",
            "abilityLabel": "DRAIN D'ÂME",
            "abilityFlashMs": 0.0,
            "shieldMs": 0.0,
        }
    elif clip == "08_outro.mp4":
        state["floor"] = 51
        state["mode"] = "walk"
        state["stepBank"] = 800.0
        state["nextFight"] = 9999
        state["ferveur"] = 100
    return state


def launch_app() -> None:
    force_stop()
    shell("input keyevent KEYCODE_WAKEUP")
    shell("wm dismiss-keyguard")
    shell(f"monkey -p {PACKAGE} -c android.intent.category.LAUNCHER 1")
    time.sleep(2.5)
    # Un rapport d'absence ne devrait pas apparaître (lastActiveAt est à now),
    # mais on le ferme s'il est présent.
    if find_text("BON RETOUR"):
        tap_text("REPRENDRE LA MARCHE")
        time.sleep(1.0)
    for _ in range(10):
        if find_text("PALIER") or find_text("Stats"):
            break
        time.sleep(0.5)


def dump_ui() -> ET.Element | None:
    for attempt in range(3):
        result = shell("uiautomator dump /sdcard/capture_ui.xml")
        if result.returncode == 0:
            pull = adb("pull", "/sdcard/capture_ui.xml", str(UI_XML))
            if pull.returncode == 0 and UI_XML.exists():
                try:
                    return ET.parse(UI_XML).getroot()
                except ET.ParseError:
                    pass
        time.sleep(0.8 + attempt * 0.5)
    return None


def parse_bounds(bounds: str) -> tuple[int, int, int, int] | None:
    match = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", bounds or "")
    if not match:
        return None
    return tuple(int(value) for value in match.groups())  # type: ignore[return-value]


def find_node(text: str, exact: bool = False, root: ET.Element | None = None):
    root = root or dump_ui()
    if root is None:
        return None
    target = text.lower()
    for node in root.iter("node"):
        values = [node.get("text") or "", node.get("content-desc") or ""]
        for value in values:
            if not value:
                continue
            matched = value.lower() == target if exact else target in value.lower()
            if matched:
                bounds = parse_bounds(node.get("bounds") or "")
                if bounds:
                    return bounds
    return None


def find_text(text: str, exact: bool = False) -> bool:
    return find_node(text, exact=exact) is not None


def tap_bounds(bounds: tuple[int, int, int, int]) -> None:
    x1, y1, x2, y2 = bounds
    shell(f"input tap {(x1 + x2) // 2} {(y1 + y2) // 2}")


def tap_text(
    text: str,
    exact: bool = False,
    scroll: bool = False,
    max_scrolls: int = 5,
    pause: float = 0.7,
) -> bool:
    for attempt in range(max_scrolls + 1):
        bounds = find_node(text, exact=exact)
        if bounds:
            tap_bounds(bounds)
            time.sleep(pause)
            return True
        if not scroll or attempt >= max_scrolls:
            break
        swipe_up()
        time.sleep(0.8)
    print(f"  ! Élément introuvable : {text}")
    return False


def center(text: str, exact: bool = False) -> tuple[int, int] | None:
    bounds = find_node(text, exact=exact)
    if not bounds:
        print(f"  ! Coordonnées introuvables : {text}")
        return None
    x1, y1, x2, y2 = bounds
    return (x1 + x2) // 2, (y1 + y2) // 2


def tap_point(point: tuple[int, int] | None) -> None:
    if point:
        shell(f"input tap {point[0]} {point[1]}")


def press_back() -> None:
    shell("input keyevent 4")


def find_any_text(names: tuple[str, ...]):
    root = dump_ui()
    if root is None:
        return None
    for node in root.iter("node"):
        value = (node.get("text") or "").lower()
        if not value:
            continue
        if any(name.lower() in value for name in names):
            bounds = parse_bounds(node.get("bounds") or "")
            if bounds:
                return bounds
    return None


def swipe_up(duration: int = 450) -> None:
    shell(f"input swipe 540 1900 540 760 {duration}")


def swipe_down(duration: int = 450) -> None:
    shell(f"input swipe 540 1600 540 2300 {duration}")


def scroll_to_top(times: int = 6) -> None:
    for _ in range(times):
        swipe_down(duration=300)
        time.sleep(0.35)


def screen_size() -> tuple[int, int]:
    output = shell_out("wm size")
    match = re.search(r"(\d+)x(\d+)", output)
    if match:
        return int(match.group(1)), int(match.group(2))
    return 1080, 2400


def open_tab(name: str) -> bool:
    expected = {
        "Stats": "PUISSANCE",
        "Inventaire": "ÉQUIPEMENT",
        "Améliorations": "Forger la marche",
    }.get(name)
    fallback = {
        "Stats": (200, 1352),
        "Inventaire": (540, 1352),
        "Améliorations": (878, 1352),
    }.get(name)
    for _ in range(3):
        bounds = find_node(name, exact=True)
        if bounds:
            tap_bounds(bounds)
        elif fallback:
            shell(f"input tap {fallback[0]} {fallback[1]}")
        time.sleep(1.0)
        if expected and find_text(expected):
            return True
    print(f"  ! Onglet non confirmé : {name}")
    return False


def launch_fast() -> None:
    """Lancement rapide pour les scènes de marche.

    On évite les dumps UI Automator de launch_app : le héros commencerait à
    marcher pendant la préparation et viderait sa réserve avant l'enregistrement.
    """
    force_stop()
    shell("input keyevent KEYCODE_WAKEUP")
    shell("wm dismiss-keyguard")
    shell(f"monkey -p {PACKAGE} -c android.intent.category.LAUNCHER 1")
    time.sleep(1.8)
    # Deux essais : l'onglet Stats peut ne pas être dessiné au premier tap.
    shell("input tap 200 1352")
    time.sleep(0.35)
    shell("input tap 200 1352")
    time.sleep(0.35)


def prepare_01() -> dict:
    launch_fast()
    return {}


def during_01(limit: int, coords: dict) -> None:
    time.sleep(max(0.0, limit - 1.0))


def prepare_02() -> dict:
    launch_app()
    open_tab("Inventaire")
    time.sleep(1.0)
    return {"upgrades": center("Améliorations", exact=True)}


def during_02(limit: int, coords: dict) -> None:
    time.sleep(2.0)
    swipe_up()
    time.sleep(1.2)
    item = find_any_text(
        ("Masse d'armes", "Masque de gnoll", "Lanterne des âmes", "Gourdin", "Hache")
    )
    if item:
        tap_bounds(item)
        time.sleep(1.0)
        opened = find_text("ÉQUIPER") or find_text("VENDRE")
        if opened:
            press_back()
            time.sleep(0.5)
    else:
        time.sleep(0.5)
    tap_point(coords.get("upgrades"))
    time.sleep(4.5)
    swipe_up()
    time.sleep(max(0.0, limit - 11.0))


def prepare_03() -> dict:
    launch_app()
    open_tab("Stats")
    time.sleep(1.0)
    return {}


def during_03(limit: int, coords: dict) -> None:
    time.sleep(max(0.0, limit - 1.0))


def open_codex() -> bool:
    """Ouvre le Codex avec un scroll fin pour ne pas sauter la carte."""
    open_tab("Stats")
    scroll_to_top(10)
    for _ in range(24):
        bounds = find_node("OUVRIR LE CODEX")
        if bounds:
            tap_bounds(bounds)
            time.sleep(1.5)
            return True
        shell("input swipe 540 1800 540 1200 400")
        time.sleep(0.5)
    print("  ! Codex introuvable")
    return False


def prepare_04() -> dict:
    launch_app()
    if not open_codex():
        return {}
    tap_text("BESTIAIRE", exact=True)
    time.sleep(1.0)
    return {
        "bestiary": center("BESTIAIRE", exact=True),
        "sets": center("ENSEMBLES", exact=True),
        "biomes": center("BIOMES", exact=True),
    }


def during_04(limit: int, coords: dict) -> None:
    started = time.time()
    # Bestiaire
    time.sleep(2.0)
    swipe_up()
    time.sleep(3.0)
    swipe_up()
    time.sleep(2.0)
    # Ensembles / collections
    tap_point(coords.get("sets"))
    time.sleep(1.0)
    swipe_up()
    time.sleep(4.0)
    swipe_up()
    time.sleep(3.0)
    # Biomes
    tap_point(coords.get("biomes"))
    time.sleep(1.0)
    swipe_up()
    time.sleep(5.0)
    swipe_up()
    time.sleep(5.0)
    swipe_up()
    time.sleep(max(0.0, limit - (time.time() - started) - 0.5))


def prepare_05() -> dict:
    launch_app()
    open_tab("Stats")
    tap_text("Feu de camp")
    time.sleep(2.0)
    return {
        "veillee": center("Veillée gratuite"),
        "reposer": center("Se reposer"),
        "ressourcer": center("Se ressourcer"),
    }


def during_05(limit: int, coords: dict) -> None:
    time.sleep(3.0)
    tap_point(coords.get("veillee"))
    time.sleep(3.0)
    tap_point(coords.get("reposer"))
    time.sleep(3.0)
    tap_point(coords.get("ressourcer"))
    time.sleep(max(0.0, limit - 12.0))


def prepare_06() -> dict:
    launch_fast()
    time.sleep(0.4)
    # Chip COFFRE en haut à droite (au-dessous du SAC).
    shell("input tap 925 375")
    time.sleep(1.0)
    # Bouton CONSACRER MES PAS dans la fiche du coffre.
    shell("input tap 539 1495")
    time.sleep(0.5)
    return {"forge": (539, 1495)}


def during_06(limit: int, coords: dict) -> None:
    time.sleep(max(0.0, limit - 0.5))


def prepare_07() -> dict:
    launch_fast()
    # Le chip SAC est fixe en haut à droite ; on évite un dump pendant le combat.
    return {"sac": (974, 306)}


def during_07(limit: int, coords: dict) -> None:
    # Le gardien doit tuer le héros (PV = 1), puis la retraite de 20 m s'affiche.
    time.sleep(4.5)
    tap_point(coords.get("sac"))
    time.sleep(1.0)
    swipe_up()
    time.sleep(1.5)
    swipe_up()
    time.sleep(max(0.0, limit - 8.0))


def prepare_08() -> dict:
    launch_fast()
    return {}


def during_08(limit: int, coords: dict) -> None:
    time.sleep(max(0.0, limit - 1.0))


PREPARE = {
    "01_intro_marche.mp4": prepare_01,
    "02_equipement.mp4": prepare_02,
    "03_combat_gardien.mp4": prepare_03,
    "04_biomes_codex.mp4": prepare_04,
    "05_camp_braise.mp4": prepare_05,
    "06_coffre_pas.mp4": prepare_06,
    "07_defaite_detour.mp4": prepare_07,
    "08_outro.mp4": prepare_08,
}

DURING = {
    "01_intro_marche.mp4": during_01,
    "02_equipement.mp4": during_02,
    "03_combat_gardien.mp4": during_03,
    "04_biomes_codex.mp4": during_04,
    "05_camp_braise.mp4": during_05,
    "06_coffre_pas.mp4": during_06,
    "07_defaite_detour.mp4": during_07,
    "08_outro.mp4": during_08,
}


def record(clip: str, duration: float, during, coords: dict) -> None:
    CLIPS.mkdir(parents=True, exist_ok=True)
    limit = int(math.ceil(duration + 3.0))
    remote = f"/sdcard/{clip}"
    local = CLIPS / clip
    local.unlink(missing_ok=True)
    shell(f"rm -f {remote}")
    print(f"  Enregistrement {limit} s → {remote}")
    formatted = f"{int(duration // 60):02d}:{duration % 60:06.3f}"
    print(f"  Durée storyboard : {duration:.3f} s ({formatted}) + 3 s de marge")
    process = subprocess.Popen(
        [
            ADB, "-s", SERIAL, "shell", "screenrecord",
            "--bit-rate", "8000000", "--time-limit", str(limit), remote,
        ],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    try:
        during(limit, coords)
    finally:
        try:
            process.wait(timeout=limit + 20)
        except subprocess.TimeoutExpired:
            process.terminate()
            process.wait(timeout=10)
    pull = adb("pull", remote, str(local))
    if pull.returncode != 0:
        print(f"  ! Échec du rapatriement : {pull.stderr.strip()}")
    shell(f"rm -f {remote}")
    if local.exists():
        size = local.stat().st_size
        print(f"  Clip enregistré : {local} ({size / 1_000_000:.1f} Mo)")
    else:
        print(f"  ! Clip absent : {local}")


def main() -> int:
    for stream in (sys.stdout, sys.stderr):
        try:
            stream.reconfigure(encoding="utf-8", errors="replace")
        except Exception:
            pass

    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("clip", nargs="?", help="Nom du clip à refaire (ex: 03_combat_gardien.mp4)")
    parser.add_argument("--auto", action="store_true", help="Ne pas attendre Entrée avant chaque clip.")
    parser.add_argument("--keep-state", action="store_true", help="Ne pas restaurer les données debug.")
    args = parser.parse_args()

    connect()
    if not package_installed():
        raise SystemExit(
            f"Le package {PACKAGE} n'est pas installé. Installe l'APK debug d'abord."
        )
    if not STORYBOARD.exists():
        raise SystemExit(f"Storyboard introuvable : {STORYBOARD}")

    storyboard = json.loads(STORYBOARD.read_text(encoding="utf-8"))
    scenes = storyboard.get("scenes") or []
    if args.clip:
        wanted = args.clip if args.clip.endswith(".mp4") else f"{args.clip}.mp4"
        scenes = [scene for scene in scenes if scene.get("clip") == wanted]
        if not scenes:
            available = ", ".join(scene.get("clip", "?") for scene in storyboard.get("scenes") or [])
            raise SystemExit(f"Clip inconnu : {args.clip}\nClips disponibles : {available}")

    print(f"Storyboard : {STORYBOARD}")
    print(f"Clips à enregistrer : {len(scenes)}")
    for scene in scenes:
        print(
            f"  {scene.get('clip')} · {scene.get('duration'):.3f} s · "
            f"{scene.get('description')}"
        )

    backup_data()
    disable_step_sync()
    original_accel = shell_out("settings get system accelerometer_rotation").strip()
    original_rotation = shell_out("settings get system user_rotation").strip()
    shell("settings put system accelerometer_rotation 0")
    shell("settings put system user_rotation 0")
    shell("settings put global stay_on_while_plugged_in 7")
    shell("settings put system screen_off_timeout 2147483647")
    shell("svc power stayon true")
    time.sleep(1.0)

    base = load_base_state()
    summary = []
    try:
        for index, scene in enumerate(scenes, start=1):
            clip = scene["clip"]
            duration = float(scene["duration"])
            print()
            print("=" * 72)
            print(f"[{index}/{len(scenes)}] {clip}")
            print(f"Scène : {scene.get('description')}")
            print(f"Durée requise : {duration:.3f} s (+3 s de marge)")
            print(f"Storyboard : séquence {scene.get('sequence')} · scène {scene.get('index')}")

            state = scene_state(base, clip)
            push_state(state)
            print("  État injecté.")
            prepare = PREPARE.get(clip)
            during = DURING.get(clip)
            if prepare is None or during is None:
                raise SystemExit(f"Pas d'action définie pour {clip}.")
            coords = prepare() or {}
            print(f"  Écran préparé. Coordonnées utiles : {coords}")
            if not args.auto:
                input("  Appuie sur Entrée pour lancer l'enregistrement… ")
            record(clip, duration, during, coords)
            summary.append(
                {
                    "clip": clip,
                    "duration": duration,
                    "limit": int(math.ceil(duration + 3.0)),
                    "file": str((CLIPS / clip).relative_to(ROOT)),
                    "bytes": (CLIPS / clip).stat().st_size if (CLIPS / clip).exists() else 0,
                }
            )
    finally:
        # Le téléphone doit rester éveillé tant qu'il est branché.
        shell("settings put global stay_on_while_plugged_in 7")
        shell("svc power stayon true")
        shell(f"settings put system accelerometer_rotation {original_accel or '1'}")
        shell(f"settings put system user_rotation {original_rotation or '0'}")
        if not args.keep_state:
            restore_data()

    log = CLIPS / "capture_log.json"
    if summary:
        log.write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
        print(f"\nJournal des captures : {log}")
    print(f"Captures terminées. Dossier : {CLIPS}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
