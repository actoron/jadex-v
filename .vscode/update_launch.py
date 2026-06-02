import json
import subprocess
import os
import sys

# Works whether script is in project root or .vscode/
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
if os.path.basename(SCRIPT_DIR) == ".vscode":
    WORKSPACE = os.path.dirname(SCRIPT_DIR)
else:
    WORKSPACE = SCRIPT_DIR
LAUNCH_PATH = os.path.join(WORKSPACE, ".vscode", "launch.json")
CONFIG_NAME = "Attach to Bazel"

# Find all src/main/java directories, exclude bazel-* symlinks
result = subprocess.run(
    ["find", WORKSPACE, "-type", "d", "-name", "java", "-path", "*/src/main/java"],
    capture_output=True, text=True
)

paths = [
    "${workspaceFolder}" + p.strip()[len(WORKSPACE):]
    for p in result.stdout.strip().split("\n")
    if p.strip() and "/bazel-" not in p
]

if not paths:
    print("No src/main/java directories found!")
    sys.exit(1)

# Read existing launch.json
with open(LAUNCH_PATH) as f:
    launch = json.load(f)

# Update matching config
updated = False
for config in launch["configurations"]:
    if config.get("name") == CONFIG_NAME:
        config["sourcePaths"] = paths
        updated = True

if not updated:
    print(f"Config '{CONFIG_NAME}' not found in launch.json!")
    sys.exit(1)

with open(LAUNCH_PATH, "w") as f:
    json.dump(launch, f, indent=2)

print(f"✓ Updated {len(paths)} source paths in launch.json:")
for p in paths:
    print(f"  {p}")