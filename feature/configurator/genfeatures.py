#!/usr/bin/env python3

import os
import re
import json
import argparse
import sys
from collections import defaultdict


# ----------------------------
# Config
# ----------------------------

IGNORE_DIRS = {
    ".git",
    ".idea",
    ".vscode",
    "node_modules",
    "external",
    "bazel-bin",
    "bazel-out",
    "bazel-testlogs",
}

BUILD_FILES = {"BUILD", "BUILD.bazel"}


# ----------------------------
# MAVEN LABEL INDEX
# ----------------------------

# Trifft  "group:artifact:version"  und optional ":classifier"
COORD_RE = re.compile(
    r'"((?:[a-zA-Z0-9_\-\.]+):'    # group
    r'(?:[a-zA-Z0-9_\-\.]+):'      # artifact
    r'(?:[a-zA-Z0-9_\-\.\+]+)'     # version
    r'(?::[a-zA-Z0-9_\-]+)?)"'     # optionaler classifier
)


def coord_to_bazel_label(coord: str) -> str:
    """
    Wandelt eine Maven-Koordinate in ein Bazel-@maven-Label um.

    Beispiele:
      "org.junit.jupiter:junit-jupiter-api:5.10.0"
        → "@maven//:org_junit_jupiter_junit_jupiter_api"
      "com.badlogicgames.gdx:gdx-platform:1.12.1:natives-desktop"
        → "@maven//:com_badlogicgames_gdx_gdx_platform_natives_desktop"

    Regeln:
      - group + artifact (+ optionaler classifier) werden mit '_' verbunden
      - Punkte und Bindestriche → '_'
      - Version wird weggeworfen
    """
    parts = coord.split(":")
    group    = parts[0]
    artifact = parts[1]
    # version ist parts[2], wird ignoriert
    classifier = parts[3] if len(parts) > 3 else None

    combined = f"{group}_{artifact}"
    if classifier:
        combined += f"_{classifier}"

    combined = combined.replace(".", "_").replace("-", "_")
    return f"@maven//:{combined}"


def build_maven_label_index(workspace_root: str) -> dict:
    """
    Durchsucht alle relevanten Dateien im Workspace nach Maven-Koordinaten
    und baut ein Reverse-Mapping:
      "@maven//:org_junit_jupiter_junit_jupiter_api"  →  "org.junit.jupiter:junit-jupiter-api:5.10.0"

    Durchsuchte Dateien:
      - MODULE.bazel, WORKSPACE, WORKSPACE.bazel (Root)
      - alle *.bzl / *.bazel Dateien im gesamten Workspace
    """
    index = {}

    candidates = [
        os.path.join(workspace_root, "MODULE.bazel"),
        os.path.join(workspace_root, "WORKSPACE"),
        os.path.join(workspace_root, "WORKSPACE.bazel"),
    ]

    for base, dirs, files in os.walk(workspace_root):
        dirs[:] = [d for d in dirs if d not in IGNORE_DIRS]
        for fname in files:
            if fname.endswith(".bzl") or fname.endswith(".bazel"):
                candidates.append(os.path.join(base, fname))

    seen_coords = set()

    for path in candidates:
        if not os.path.isfile(path):
            continue
        try:
            content = open(path, encoding="utf-8").read()
        except OSError:
            continue

        for m in COORD_RE.finditer(content):
            coord = m.group(1)
            if coord in seen_coords:
                continue
            seen_coords.add(coord)

            label = coord_to_bazel_label(coord)

            # Erstes Vorkommen gewinnt (typischerweise korrekt)
            if label not in index:
                index[label] = coord

    return index


def resolve_external_deps(raw_labels: list, maven_index: dict) -> list:
    """
    Ersetzt @maven-Labels durch Maven-Koordinaten im Format group:artifact:version.
    Labels, die nicht aufgelöst werden können, bleiben erhalten und erzeugen eine Warnung.
    Nicht-Maven-Einträge (z.B. @maven//:... die schon Koordinaten sind) werden durchgereicht.
    """
    resolved = []
    for label in raw_labels:
        if label.startswith("@maven"):
            coord = maven_index.get(label)
            if coord:
                resolved.append(coord)
            else:
                print(f"[WARN] Bazel-Label nicht auflösbar: {label}", file=sys.stderr)
                resolved.append(label)   # als Fallback behalten
        else:
            resolved.append(label)
    return resolved


# ----------------------------
# FEATURE PARSING
# ----------------------------

FEATURE_BLOCK_RE = re.compile(r"feature\s*\(", re.MULTILINE)
ATTR_RE = re.compile(r'(\w+)\s*=\s*"([^"]*)"')


def _extract_call_block(content: str, match_end: int) -> str:
    """Extrahiert den Inhalt eines Funktionsaufrufs (zwischen den äußersten Klammern)."""
    body = content[match_end:]
    depth = 1
    extracted = []

    for ch in body:
        if ch == "(":
            depth += 1
        elif ch == ")":
            depth -= 1
            if depth == 0:
                break
        extracted.append(ch)

    return "".join(extracted)


def parse_feature(build_file_path: str):
    try:
        with open(build_file_path, "r", encoding="utf-8") as f:
            content = f.read()
    except FileNotFoundError:
        return None

    match = FEATURE_BLOCK_RE.search(content)
    if not match:
        return None

    block = _extract_call_block(content, match.end())
    attrs = dict(ATTR_RE.findall(block))

    if not attrs:
        return None

    return {
        "name":        attrs.get("name"),
        "category":    attrs.get("category"),
        "description": attrs.get("description", ""),
        "type":        attrs.get("type"),
    }


# ----------------------------
# FEATURE GROUP PARSING
# ----------------------------

FEATURE_GROUP_BLOCK_RE = re.compile(r"feature_group\s*\(", re.MULTILINE)


def parse_feature_group(build_file_path: str) -> dict | None:
    """
    Sucht nach einem feature_group(...)-Aufruf in der BUILD-Datei und gibt
    dessen Attribute zurück:
      {
        "name":        str | None,
        "description": str,
        "type":        "or" | "xor" | "and"   (default: "or")
      }
    Gibt None zurück wenn kein feature_group(...) gefunden.
    """
    try:
        with open(build_file_path, "r", encoding="utf-8") as f:
            content = f.read()
    except FileNotFoundError:
        return None

    match = FEATURE_GROUP_BLOCK_RE.search(content)
    if not match:
        return None

    block = _extract_call_block(content, match.end())
    attrs = dict(ATTR_RE.findall(block))

    if not attrs:
        return None

    return {
        "name":        attrs.get("name"),
        "description": attrs.get("description", ""),
        "type":        attrs.get("type", "or"),
    }


# ----------------------------
# DEP PARSING
# ----------------------------

TARGET_RE    = re.compile(r"(?:java_library|java_test)\s*\(", re.MULTILINE)
DEP_LIST_RE  = re.compile(r"deps\s*=\s*\[([^\]]*)\]", re.DOTALL)
DEP_ENTRY_RE = re.compile(r'"([^"]+)"')


def _extract_block(content: str, match_end: int) -> str:
    return _extract_call_block(content, match_end)


def _deps_from_block(block: str):
    m = DEP_LIST_RE.search(block)
    if not m:
        return [], []

    entries = DEP_ENTRY_RE.findall(m.group(1))
    internal, external = [], []

    for e in entries:
        if e.startswith("//"):
            internal.append(e.lstrip("/").split(":")[0])
        elif e.startswith("@maven"):
            external.append(e)

    return internal, external


def parse_deps(build_file_path: str, module_name: str):
    try:
        with open(build_file_path, "r", encoding="utf-8") as f:
            content = f.read()
    except FileNotFoundError:
        return [], []

    def find_named(name: str):
        return re.search(
            r"(?:java_library|java_test)\s*\([^)]*name\s*=\s*\"" + re.escape(name) + r"\"",
            content,
            re.DOTALL,
        )

    m = find_named(module_name) or find_named("publish") or TARGET_RE.search(content)

    if not m:
        return [], []

    return _deps_from_block(_extract_block(content, m.end()))


# ----------------------------
# EXTERNAL DEPS (aus deps.bzl)
# ----------------------------

LOAD_DEPS_RE  = re.compile(r'load\s*\(\s*"([^"]*deps\.bzl)"[^)]*DEPS')
MAVEN_ENTRY_RE = re.compile(r'["\']([^"\']+)["\']')


def parse_external_deps_from_bzl(build_file_path: str) -> list:
    try:
        with open(build_file_path, "r", encoding="utf-8") as f:
            content = f.read()
    except FileNotFoundError:
        return []

    m = LOAD_DEPS_RE.search(content)
    if not m:
        return []

    deps_bzl_rel = m.group(1)
    if deps_bzl_rel.startswith(":"):
        deps_bzl_rel = deps_bzl_rel[1:]

    deps_bzl_path = os.path.normpath(
        os.path.join(os.path.dirname(build_file_path), deps_bzl_rel)
    )

    if not os.path.exists(deps_bzl_path):
        return []

    deps   = []
    inside = False

    try:
        with open(deps_bzl_path, "r", encoding="utf-8") as f:
            for line in f:
                s = line.strip()

                if s.startswith("MAVEN_DEPS"):
                    inside = True
                    continue

                if not inside:
                    continue

                if s.startswith("]"):
                    break

                s = s.split("#")[0].strip()
                if not s:
                    continue

                m2 = MAVEN_ENTRY_RE.search(s)
                if m2:
                    deps.append(m2.group(1))

    except FileNotFoundError:
        return []

    return sorted(set(deps))


# ----------------------------
# HEADER COMMENT
# ----------------------------

def extract_file_header_comment(build_file: str) -> str:
    try:
        with open(build_file, "r", encoding="utf-8") as f:
            lines = f.read().splitlines()
    except FileNotFoundError:
        return ""

    comments = []

    for line in lines:
        s = line.strip()
        if s.startswith("#"):
            comments.append(s.lstrip("#").strip())
        elif s == "":
            continue
        else:
            break

    return " ".join(comments).strip()


# ----------------------------
# HELPERS
# ----------------------------

def normalize(p: str) -> str:
    return p.replace("\\", "/")


def should_skip_dir(name: str) -> bool:
    return name in IGNORE_DIRS


# ----------------------------
# COLLECTOR
# ----------------------------

def collect_features(root: str, maven_index: dict) -> dict:
    features = {}

    for base, dirs, files in os.walk(root):
        dirs[:] = [d for d in dirs if not should_skip_dir(d)]

        build_file = next((f for f in files if f in BUILD_FILES), None)
        if not build_file:
            continue

        rel_dir = normalize(os.path.relpath(base, root))
        if rel_dir == ".":
            continue

        parts = rel_dir.split("/")
        if len(parts) < 2:
            continue

        parent_category = parts[-2]
        name            = parts[-1]
        build_path      = os.path.join(base, build_file)

        meta    = parse_feature(build_path)
        comment = extract_file_header_comment(build_path)

        internal_deps, raw_external = parse_deps(build_path, name)

        # Externe Deps aus deps.bzl hinzufügen (können ebenfalls Labels sein)
        raw_external += parse_external_deps_from_bzl(build_path)

        # @maven-Labels → Maven-Koordinaten (group:artifact:version)
        external_deps = resolve_external_deps(list(set(raw_external)), maven_index)
        external_deps = sorted(set(external_deps))

        module = {
            "name":         name,
            "category":     parent_category,
            "description":  comment,
            "path":         rel_dir,
            "type":         "feature",
            "deps":         internal_deps,
            "external_deps": external_deps,
        }

        if meta:
            module["name"]     = meta.get("name")     or module["name"]
            module["category"] = meta.get("category") or module["category"]
            module["type"]     = meta.get("type")      or "feature"
            if meta.get("description"):
                module["description"] = meta["description"]

        features[rel_dir] = module

    return features


# ----------------------------
# GROUP METADATA COLLECTION
# ----------------------------

def collect_group_metadata(root: str, category_names: set) -> dict:
    """
    Sucht in jedem Verzeichnis, das als Kategorie (Gruppen-Verzeichnis) erkannt wurde,
    nach einer BUILD/BUILD.bazel-Datei mit einem feature_group(...)-Aufruf.

    Gibt ein Dict zurück:
      category_name → { "name": str, "description": str, "type": str }

    Für Kategorien ohne explizite feature_group-Deklaration wird ein implizites
    Default-Objekt erzeugt.
    """
    group_meta = {}

    for base, dirs, files in os.walk(root):
        dirs[:] = [d for d in dirs if not should_skip_dir(d)]

        rel_dir = normalize(os.path.relpath(base, root))
        if rel_dir == ".":
            continue

        # Nur direkte Kategorie-Verzeichnisse prüfen (Tiefe 1)
        parts = rel_dir.split("/")
        if len(parts) != 1:
            continue

        category = parts[0]
        if category not in category_names:
            continue

        build_file = next((f for f in files if f in BUILD_FILES), None)
        if not build_file:
            continue

        build_path = os.path.join(base, build_file)
        fg = parse_feature_group(build_path)

        if fg:
            group_meta[category] = {
                "name":        fg["name"] or category,
                "description": fg["description"],
                "type":        fg["type"],
            }

    return group_meta


# ----------------------------
# INVENTORY
# ----------------------------

def build_inventory(features: dict, group_meta: dict) -> dict:
    """
    Gruppiert Features nach Kategorie und erzeugt das neue Ausgabeformat:

      {
        "category_key": {
          "name":        "...",
          "description": "...",
          "type":        "or" | "xor" | "and",
          "features":    [ ... ]
        },
        ...
      }

    Explizite Gruppen-Metadaten (aus feature_group()) haben Vorrang.
    Implizite Gruppen erhalten sinnvolle Defaults.
    """
    grouped: dict[str, list] = defaultdict(list)

    for f in features.values():
        grouped[f["category"]].append(f)

    inventory = {}

    for category in sorted(grouped.keys()):
        feature_list = sorted(grouped[category], key=lambda x: x["name"])

        # Explizite Metadaten bevorzugen, sonst implizite Defaults
        meta = group_meta.get(category, {})

        inventory[category] = {
            "name":        meta.get("name", category),
            "description": meta.get("description", ""),
            "type":        meta.get("type", "or"),
            "features":    feature_list,
        }

    return inventory


# ----------------------------
# MAIN
# ----------------------------

def main():
    parser = argparse.ArgumentParser(
        description="Scannt einen Bazel-Workspace und erzeugt ein Feature-Inventar als JSON."
    )
    parser.add_argument(
        "--root",
        default=os.environ.get("BUILD_WORKSPACE_DIRECTORY", os.getcwd()),
        help="Wurzelverzeichnis des Bazel-Workspace (Standard: $BUILD_WORKSPACE_DIRECTORY oder cwd)",
    )
    parser.add_argument(
        "--out",
        default="feature/configurator/features.json",
        help="Ausgabepfad für features.json (relativ zu --root oder absolut)",
    )
    parser.add_argument(
        "--dump-label-index",
        action="store_true",
        help="Gibt den vollständigen Bazel-Label → Maven-Koordinaten-Index auf stderr aus (Debug)",
    )

    args = parser.parse_args()
    root = os.path.abspath(args.root)

    # 1. Maven-Label-Index einmalig aufbauen
    print("[genfeatures] Baue Maven-Label-Index ...", file=sys.stderr)
    maven_index = build_maven_label_index(root)
    print(f"[genfeatures] {len(maven_index)} Maven-Koordinaten indiziert.", file=sys.stderr)

    if args.dump_label_index:
        print("\n--- Maven-Label-Index ---", file=sys.stderr)
        for label, coord in sorted(maven_index.items()):
            print(f"  {label}  →  {coord}", file=sys.stderr)
        print("--- Ende Index ---\n", file=sys.stderr)

    # 2. Features sammeln (Labels werden inline aufgelöst)
    features = collect_features(root, maven_index)

    # 3. Gruppen-Metadaten sammeln (feature_group()-Aufrufe in Kategorie-Verzeichnissen)
    category_names = {f["category"] for f in features.values()}
    group_meta = collect_group_metadata(root, category_names)
    print(
        f"[genfeatures] {len(group_meta)} explizite Gruppen-Metadaten gefunden "
        f"({len(category_names) - len(group_meta)} implizit).",
        file=sys.stderr,
    )

    # 4. Inventar mit neuem Format aufbauen
    inventory = build_inventory(features, group_meta)

    json_data = json.dumps(inventory, indent=2)

    # 5. Ausgabe schreiben
    out_path = args.out
    if not os.path.isabs(out_path):
        out_path = os.path.join(root, out_path)

    out_path = os.path.abspath(out_path)
    os.makedirs(os.path.dirname(out_path), exist_ok=True)

    with open(out_path, "w", encoding="utf-8") as f:
        f.write(json_data)

    print(json_data)
    print(f"\n[genfeatures] Geschrieben: {out_path}", file=sys.stderr)


if __name__ == "__main__":
    main()