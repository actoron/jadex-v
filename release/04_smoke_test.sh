#!/usr/bin/env bash
set -euxo pipefail

STAGING="${1:-release/staging}"
SMOKE_DIR="release/smoke-test"

extract_xml() {
    local file="$1"
    local tag="$2"

    xmllint --xpath \
        "string(/*[local-name()='project']/*[local-name()='$tag'])" \
        "$file"
}

#
# Version aus dem Staging ziehen (gleiche Logik wie create_staging.sh)
#
first_pom=$(find "$STAGING" -name "*.pom" -print -quit)
if [[ -z "$first_pom" ]]; then
    echo "ERROR: no *.pom found under $STAGING (erst create_staging.sh laufen lassen?)"
    exit 1
fi

JADEX_VERSION=$(extract_xml "$first_pom" "version")

STAGING_REPO_URL="file://$(cd "$STAGING" && pwd)"

echo "Smoke-Test gegen:"
echo "  Version:      $JADEX_VERSION"
echo "  Staging-Repo: $STAGING_REPO_URL"

WORK_DIR=$(mktemp -d)
trap 'rm -rf "$WORK_DIR"' EXIT

cp -r "$SMOKE_DIR/src" "$WORK_DIR/"
sed \
    -e "s|@JADEX_VERSION@|$JADEX_VERSION|g" \
    -e "s|@STAGING_REPO_URL@|$STAGING_REPO_URL|g" \
    "$SMOKE_DIR/pom.xml.template" > "$WORK_DIR/pom.xml"

cd "$WORK_DIR"

echo
echo "=== mvn compile ==="
mvn -B -Dmaven.repo.local="$WORK_DIR/.m2" compile

echo
echo "=== mvn exec:java ==="
mvn -B -Dmaven.repo.local="$WORK_DIR/.m2" exec:java

echo
echo "Smoke-Test erfolgreich."
