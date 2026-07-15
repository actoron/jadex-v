#!/usr/bin/env bash
set -euo pipefail

STAGING="release/staging"

GNUPGHOME=$(mktemp -d)
KEY_FILE=""
PASSPHRASE_FILE=""

cleanup() {
    rm -rf "$GNUPGHOME"

    [[ -n "$KEY_FILE" && -f "$KEY_FILE" ]] && rm -f "$KEY_FILE"
    [[ -n "$PASSPHRASE_FILE" && -f "$PASSPHRASE_FILE" ]] && rm -f "$PASSPHRASE_FILE"
}

trap cleanup EXIT

# ---------------------------------------------------------------------------
# Allow loopback pinentry
# ---------------------------------------------------------------------------

echo "allow-loopback-pinentry" > "$GNUPGHOME/gpg-agent.conf"

# ---------------------------------------------------------------------------
# Determine key source
# ---------------------------------------------------------------------------

if [[ -n "${ORG_GRADLE_PROJECT_sigKey:-}" ]]; then
    KEY_FILE=$(mktemp)

    printf '%b' \
        "${ORG_GRADLE_PROJECT_sigKey#GPG_SIGNING_KEY=}" \
        > "$KEY_FILE"

    echo "Using signing key from ORG_GRADLE_PROJECT_sigKey"

elif [[ -n "${sigKey:-}" ]]; then
    KEY_FILE=$(mktemp)

    printf '%b' \
        "${sigKey#GPG_SIGNING_KEY=}" \
        > "$KEY_FILE"

    echo "Using signing key from sigKey"

else
    KEY_FILE="release/jadex-key.asc"
    echo "Using signing key from $KEY_FILE"
fi

# ---------------------------------------------------------------------------
# Determine passphrase source
# ---------------------------------------------------------------------------

if [[ -n "${ORG_GRADLE_PROJECT_signingPassword:-}" ]]; then
    PASSPHRASE_FILE=$(mktemp)
    printf '%s' "$ORG_GRADLE_PROJECT_signingPassword" > "$PASSPHRASE_FILE"
    echo "Using signing password from ORG_GRADLE_PROJECT_signingPassword"

elif [[ -n "${signingPassword:-}" ]]; then
    PASSPHRASE_FILE=$(mktemp)
    printf '%s' "$signingPassword" > "$PASSPHRASE_FILE"
    echo "Using signing password from signingPassword"

else
    PASSPHRASE_FILE="release/jadex-keypass.txt"
    echo "Using signing password from $PASSPHRASE_FILE"
fi

# ---------------------------------------------------------------------------
# Import key
# ---------------------------------------------------------------------------

gpg \
    --homedir "$GNUPGHOME" \
    --batch \
    --yes \
    --pinentry-mode loopback \
    --passphrase-file "$PASSPHRASE_FILE" \
    --import \
    "$KEY_FILE"

# Reload agent so configuration becomes active
gpgconf --homedir "$GNUPGHOME" --kill gpg-agent

# ---------------------------------------------------------------------------
# Sign all Maven artifacts
# ---------------------------------------------------------------------------

find "$STAGING" \
    -type f \
    \( -name "*.jar" -o -name "*.pom" \) |
while read -r file
do
    echo "Signing $file"

    gpg \
        --homedir "$GNUPGHOME" \
        --batch \
        --yes \
        --pinentry-mode loopback \
        --passphrase-file "$PASSPHRASE_FILE" \
        --armor \
        --detach-sign \
        "$file"
done

# ---------------------------------------------------------------------------
# Generate checksums
# ---------------------------------------------------------------------------

find "$STAGING" \
    -type f \
    \( -name "*.jar" -o -name "*.pom" -o -name "*.asc" \) |
while read -r file
do
    md5sum  "$file" | awk '{print $1}' > "$file.md5"
    sha1sum "$file" | awk '{print $1}' > "$file.sha1"
done

echo "Done."