#!/usr/bin/env bash
set -euo pipefail

STAGING="release/staging"
BUNDLE="release/bundle.zip"
TOKEN_FILE="release/jadex-token.txt"

PUBLISHING_TYPE="USER_MANAGED"

if [[ "${1:-}" == "--automatic" ]]; then
    PUBLISHING_TYPE="AUTOMATIC" 
fi

tag_release() {
    local version="${JADEX_VERSION:-}"

    if [[ -z "$version" ]]; then
        echo "No JADEX_VERSION set, skipping git tag."
        return
    fi

    if ! git rev-parse --git-dir >/dev/null 2>&1; then
        echo "Not inside a git repository."
        return 1
    fi

    if git rev-parse -q --verify "refs/tags/$version" >/dev/null; then
        echo "Tag $version already exists."
        return
    fi

    echo "Configuring git identity"

    git config user.name "jadex-release-bot"
    git config user.email "release-bot@activecomponents.org"

    echo "Creating git tag $version"

    git tag -a "$version" -m "Release $version"

    echo "Pushing tag $version"

    git push origin "$version"

    echo "Release tag $version published."
}

if [[ ! -d "$STAGING" ]]; then
    echo "ERROR: Staging directory '$STAGING' not found."
    exit 1
fi

# Load credentials from environment or token file
if [[ -z "${CENTRAL_USER:-}" || -z "${CENTRAL_PASSWORD:-}" ]]; then

    # Fallback auf die in env.sh verwendeten Variablennamen
    if [[ -n "${centralUser:-}" && -n "${centralPassword:-}" ]]; then
        CENTRAL_USER="$centralUser"
        CENTRAL_PASSWORD="$centralPassword"

        export CENTRAL_USER CENTRAL_PASSWORD

        echo "Using credentials from centralUser/centralPassword"

    else
        if [[ ! -f "$TOKEN_FILE" ]]; then
            echo "ERROR: No credentials found (CENTRAL_USER/CENTRAL_PASSWORD, centralUser/centralPassword or $TOKEN_FILE)."
            exit 1
        fi

        TOKEN=$(<"$TOKEN_FILE")

        if [[ "$TOKEN" != *:* ]]; then
            echo "ERROR: Invalid token format in '$TOKEN_FILE'. Expected user:password."
            exit 1
        fi

        CENTRAL_USER="${TOKEN%%:*}"
        CENTRAL_PASSWORD="${TOKEN#*:}"

        export CENTRAL_USER CENTRAL_PASSWORD

        echo "Using credentials from $TOKEN_FILE"
    fi
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

UPLOAD_URL="https://central.sonatype.com/api/v1/publisher/upload"

if [[ "$PUBLISHING_TYPE" == "AUTOMATIC" ]]; then
    UPLOAD_URL="${UPLOAD_URL}?publishingType=AUTOMATIC"
    echo "Publishing mode: automatic"
else
    echo "Publishing mode: manual"
fi

DEPLOYMENT_ID=$(
curl -fsS \
    --request POST \
    --header "Authorization: Bearer $AUTH" \
    --form "bundle=@${BUNDLE}" \
    "$UPLOAD_URL"
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
        VALIDATED)
            echo "Waiting for manual publish in Central..."
            ;;
        PUBLISHING)
            echo "Publishing..."
            ;;
        PUBLISHED)
            echo "Release published."
            tag_release
            exit 0
            ;;
        FAILED)
            echo "$STATUS"
            exit 1
            ;;
        *)
            echo "Current state: $STATE"
            ;;
    esac
done