#!/bin/bash
set -e

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODULE_FILE="$ROOT_DIR/MODULE.bazel"

echo ""
echo "Collecting MAVEN_DEPS from all deps.bzl files..."

PLAIN_ARTIFACTS=""

while IFS= read -r -d '' deps_file; do
    module_path=$(dirname "$deps_file" | sed "s|$ROOT_DIR/||")

    result=$(python3 - "$deps_file" <<'EOF'
import sys, re

with open(sys.argv[1]) as f:
    content = f.read()

matches = re.findall(
    r'([A-Z0-9_]+)\s*(?:\+?=)\s*\[(.*?)\]',
    content,
    re.DOTALL
)

plain = []

for _, block in matches:
    items = re.findall(
        r'["\']([^"\']+:[^"\']+:[^"\']+(?::[^"\']+)?)["\']',
        block
    )

    for item in items:
        # IMPORTANT:
        # classifier NICHT trennen -> rules_jvm_external kann das selbst
        plain.append(item)

for item in plain:
    print(item)
EOF
)

    if [ -n "$result" ]; then
        echo "  Found in $module_path/deps.bzl:"

        while IFS= read -r line; do
            if [ -n "$line" ]; then
                echo "    - $line"
                PLAIN_ARTIFACTS="$PLAIN_ARTIFACTS\"$line\",\n"
            fi
        done <<< "$result"
    fi
done < <(find "$ROOT_DIR" -name "deps.bzl" -print0)

# dedupe + sort
PLAIN_ARTIFACTS=$(echo -e "$PLAIN_ARTIFACTS" | sort -u)

echo ""
echo "Generating MODULE.bazel..."

cat > "$MODULE_FILE" << MODULEEOF

module(
    name = "jadex-v",
    version = "1.0",
)

bazel_dep(name = "rules_java", version = "9.1.0")
bazel_dep(name = "rules_jvm_external", version = "6.7")
bazel_dep(name = "platforms", version = "1.0.0")
bazel_dep(name = "rules_shell", version = "0.6.1")
bazel_dep(name = "rules_oci", version = "2.3.0")
bazel_dep(name = "rules_pkg", version = "1.0.1")
bazel_dep(name = "rules_python", version = "1.7.0")

java_toolchains = use_extension(
    "@rules_java//java:extensions.bzl",
    "toolchains",
)
use_repo(java_toolchains, "local_jdk")

register_toolchains("@rules_java//toolchains:all")

maven = use_extension(
    "@rules_jvm_external//:extensions.bzl",
    "maven",
)

maven.install(
    name = "maven",
    artifacts = [
$(echo -e "$PLAIN_ARTIFACTS")
        # shared test deps
        "org.junit.platform:junit-platform-suite-api:1.10.0",
        "org.junit.platform:junit-platform-console:1.10.0",
        "org.junit.jupiter:junit-jupiter-engine:5.10.0",
        "org.junit.platform:junit-platform-suite-engine:1.10.0",
    ],
    repositories = ["https://repo1.maven.org/maven2"],
)

use_repo(maven, "maven")

oci = use_extension("@rules_oci//oci:extensions.bzl", "oci")

oci.pull(
    name = "distroless_java21",
    digest = "sha256:f34fd3e4e2d7a246d764d0614f5e6ffb3a735930723fac4cfc25a72798950262",
    image = "gcr.io/distroless/java21-debian12",
    platforms = ["linux/amd64", "linux/arm64"],
)

use_repo(
    oci,
    "distroless_java21",
    "distroless_java21_linux_amd64",
    "distroless_java21_linux_arm64",
)

MODULEEOF

echo ""
echo "DONE: MODULE.bazel generated successfully"