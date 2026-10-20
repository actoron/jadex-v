#!/usr/bin/env python3
import sys
from pathlib import Path

root = Path(sys.argv[1] if len(sys.argv) > 1 else ".")

files = list(root.rglob("*.java"))
total = 0

for file in files:
    try:
        lines = len(file.read_text(encoding="utf-8", errors="replace").splitlines())
        total += lines
    except OSError as e:
        print(f"Fehler bei {file}: {e}", file=sys.stderr)

print(f"Java files: {len(files)}")
print(f"LOC:        {total}")

