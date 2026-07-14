#!/usr/bin/env bash
set -euo pipefail

STAGING="release/staging"
BUNDLE="release/bundle.zip"

if [[ ! -d "$STAGING" ]]; then
    echo "ERROR: Staging directory '$STAGING' not found."
    exit 1
fi

if [[ -z "${CENTRAL_USER:-}" ]]; then
    echo "ERROR: CENTRAL_USER not set."
    exit 1
fi

if [[ -z "${CENTRAL_PASSWORD:-}" ]]; then
    echo "ERROR: CENTRAL_PASSWORD not set."
    exit 1
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
    "https://central.sonatype.com/api/v1/publisher/upload" #?publishingType=AUTOMATIC"
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