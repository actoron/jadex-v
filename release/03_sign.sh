#!/usr/bin/env bash
set -euo pipefail

STAGING="release/staging"

GNUPGHOME=$(mktemp -d)
trap 'rm -rf "$GNUPGHOME"' EXIT

# Loopback-Pinentry erlauben
echo "allow-loopback-pinentry" > "$GNUPGHOME/gpg-agent.conf"

# Schlüssel importieren
gpg \
    --homedir "$GNUPGHOME" \
    --batch \
    --yes \
    --pinentry-mode loopback \
    --passphrase-file release/jadex-keypass.txt \
    --import \
    release/jadex-key.asc

# Agent neu laden, damit die Konfiguration aktiv ist
gpgconf --homedir "$GNUPGHOME" --kill gpg-agent

# Alle Maven-Artefakte signieren
find "$STAGING" \
    -type f \
    \( -name "*.jar" -o -name "*.pom" \) \
    | while read -r file
do
    echo "Signing $file"

    gpg \
        --homedir "$GNUPGHOME" \
        --batch \
        --yes \
        --pinentry-mode loopback \
        --passphrase-file release/jadex-keypass.txt \
        --armor \
        --detach-sign \
        "$file"
done

find "$STAGING" \
    -type f \
    \( -name "*.jar" -o -name "*.pom" -o -name "*.asc" \) \
    | while read -r file
do
    md5sum "$file" | awk '{print $1}' > "$file.md5"
    sha1sum "$file" | awk '{print $1}' > "$file.sha1"
done

echo "Done."