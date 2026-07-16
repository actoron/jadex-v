#!/usr/bin/env bash
set -euo pipefail

STAGING="release/staging"
BUNDLE="release/bundle.zip"
TOKEN_FILE="release/jadex-token.txt"

PUBLISHING_TYPE="USER_MANAGED"

if [[ "${1:-}" == "--automatic" ]]; then
    PUBLISHING_TYPE="AUTOMATIC"
fi


# ---------------------------------------------------------------------------
# Create and push git release tag
# ---------------------------------------------------------------------------
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

    # Check remote tag
    if git ls-remote --exit-code --tags origin "$version" >/dev/null 2>&1; then
        echo "Remote tag $version already exists."
        return 1
    fi

    echo "Configuring git identity"

    git config user.name "jadex-release-bot"
    git config user.email "release-bot@activecomponents.org"

    echo "Creating git tag $version"

    git tag -a "$version" -m "Release $version"

    echo "Pushing tag $version"

    if [[ -n "${GIT_PUSH_TOKEN:-}" ]]; then
        echo "Using GIT_PUSH_TOKEN for authentication"

        origin_url="$(git remote get-url origin)"

        case "$origin_url" in
            https://*)
                auth_url="${origin_url/https:\/\//https:\/\/oauth2:${GIT_PUSH_TOKEN}@}"
                git push "$auth_url" "refs/tags/$version"
                ;;
            *)
                echo "Origin is not HTTPS ($origin_url). Falling back to normal git push."
                git push origin "refs/tags/$version"
                ;;
        esac
    else
        git push origin "refs/tags/$version"
    fi

    echo "Release tag $version published."
}


# ---------------------------------------------------------------------------
# Check staging
# ---------------------------------------------------------------------------
if [[ ! -d "$STAGING" ]]; then
    echo "ERROR: Staging directory '$STAGING' not found."
    exit 1
fi


# ---------------------------------------------------------------------------
# Check version before upload
# ---------------------------------------------------------------------------
if [[ -z "${JADEX_VERSION:-}" ]]; then
    echo "ERROR: JADEX_VERSION not set."
    exit 1
fi

echo "Checking if version $JADEX_VERSION was already released..."

if git ls-remote --exit-code --tags origin "$JADEX_VERSION" >/dev/null 2>&1; then
    echo "ERROR: Git tag $JADEX_VERSION already exists."
    exit 1
fi

echo "Version $JADEX_VERSION is available."


# ---------------------------------------------------------------------------
# Load credentials
# ---------------------------------------------------------------------------
if [[ -z "${CENTRAL_USER:-}" || -z "${CENTRAL_PASSWORD:-}" ]]; then

    # Fallback auf env.sh Variablennamen
    if [[ -n "${centralUser:-}" && -n "${centralPassword:-}" ]]; then

        CENTRAL_USER="$centralUser"
        CENTRAL_PASSWORD="$centralPassword"

        export CENTRAL_USER CENTRAL_PASSWORD

        echo "Using credentials from centralUser/centralPassword"

    else

        if [[ ! -f "$TOKEN_FILE" ]]; then
            echo "ERROR: No credentials found."
            exit 1
        fi

        TOKEN=$(<"$TOKEN_FILE")

        if [[ "$TOKEN" != *:* ]]; then
            echo "ERROR: Invalid token format."
            exit 1
        fi

        CENTRAL_USER="${TOKEN%%:*}"
        CENTRAL_PASSWORD="${TOKEN#*:}"

        export CENTRAL_USER CENTRAL_PASSWORD

        echo "Using credentials from $TOKEN_FILE"

    fi
fi


# ---------------------------------------------------------------------------
# Create bundle
# ---------------------------------------------------------------------------
echo "Creating deployment bundle..."

rm -f "$BUNDLE"

(
    cd "$STAGING"
    zip -qr "../$(basename "$BUNDLE")" .
)

echo "Bundle created: $BUNDLE"


# ---------------------------------------------------------------------------
# Upload to Central
# ---------------------------------------------------------------------------
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


# ---------------------------------------------------------------------------
# Wait for publication
# ---------------------------------------------------------------------------
echo "Waiting for publication..."


while true
do
    sleep 10

    STATUS=$(curl -fsS \
        --request POST \
        --header "Authorization: Bearer $AUTH" \
        "https://central.sonatype.com/api/v1/publisher/status?id=${DEPLOYMENT_ID}")


    STATE=$(echo "$STATUS" \
        | grep -o '"deploymentState":"[^"]*"' \
        | cut -d'"' -f4)


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