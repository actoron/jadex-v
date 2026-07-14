#!/usr/bin/env bash
set -euo pipefail

STAGING="release/staging"
BUNDLE="release/bundle.zip"
TOKEN_FILE="release/jadex-token.txt"

if [[ ! -d "$STAGING" ]]; then
    echo "ERROR: Staging directory '$STAGING' not found."
    exit 1
fi

# Load credentials from token file if not already provided
if [[ -z "${CENTRAL_USER:-}" || -z "${CENTRAL_PASSWORD:-}" ]]; then
    if [[ ! -f "$TOKEN_FILE" ]]; then
        echo "ERROR: CENTRAL_USER/CENTRAL_PASSWORD not set and token file '$TOKEN_FILE' not found."
        exit 1
    fi

    TOKEN=$(cat "$TOKEN_FILE")

    if [[ "$TOKEN" != *:* ]]; then
        echo "ERROR: Invalid token format in '$TOKEN_FILE'. Expected user:password."
        exit 1
    fi

    CENTRAL_USER="${TOKEN%%:*}"
    CENTRAL_PASSWORD="${TOKEN#*:}"

    export CENTRAL_USER CENTRAL_PASSWORD

    echo "Using credentials from $TOKEN_FILE"
fi

echo "Creating deployment bundle..."

rm -f "$BUNDLE"

(
    cd "$STAGING"
    zip -qr "../$(basename "$BUNDLE")" .
)

echo "Bundle created: $BUNDLE"

AUTH=$(printf "%s:%s" "$CENTRAL_USER" "$CENTRAL_PASSWORD" | base64 -w0)

echo "Uploading bundle..."

DEPLOYMENT_ID=$(
curl -fsS \
    --request POST \
    --header "Authorization: Bearer $AUTH" \
    --form "bundle=@${BUNDLE}" \
    "https://central.sonatype.com/api/v1/publisher/upload"
)

echo "Deployment ID: $DEPLOYMENT_ID"

echo "Waiting for publication..."

while true
do
    sleep 10

    STATUS=$(curl -fsS \
        --request POST \
        --header "Authorization: Bearer $AUTH" \
        "https://central.sonatype.com/api/v1/publisher/status?id=${DEPLOYMENT_ID}")

    STATE=$(echo "$STATUS" | grep -o '"deploymentState":"[^"]*"' | cut -d'"' -f4)

    echo "$STATE"

    case "$STATE" in
        PUBLISHED)
            echo "Release published."
            exit 0
            ;;
        FAILED)
            echo "$STATUS"
            exit 1
            ;;
    esac
done