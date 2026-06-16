# The script needs the following environment variables to be set, either directly or with the ORG_GRADLE_PROJECT_ prefix:
#
# JADEX_VERSION_PREFIX: Versionspräfix, Standard: 5.0-beta
# sigKey: GPG-Private-Key
# signingPassword:  Passwort für den GPG-Key
# repocentral: Central Repository-URL
# dl_host: SSH-Deploy-Ziel, Format: ssh://user:url-encoded-password[;fingerprint=xx-xx-...]@host:port

#!/usr/bin/env bash

set -euo pipefail

PROJECT_ROOT="${PROJECT_ROOT:-$(pwd)}"

# ---------------------------------------------------------------------------
# get_var <KEY>
# ---------------------------------------------------------------------------
get_var() {
    local key="$1"
    local val="${!key:-}"

    if [ -z "$val" ]; then
        local prefixed="ORG_GRADLE_PROJECT_${key}"
        val="${!prefixed:-}"
    fi

    printf '%s' "$val"
}

dl_host="$(get_var dl_host)"
sigKey="$(get_var sigKey)"
signingPassword="$(get_var signingPassword)"
repocentral="$(get_var repocentral)"

JADEX_VERSION_PREFIX="$(get_var JADEX_VERSION_PREFIX)"
JADEX_VERSION_PREFIX="5.0-beta-test" # for testing only, remove later

if [ -z "$JADEX_VERSION_PREFIX" ]; then
    JADEX_VERSION_PREFIX="5.0-beta"
fi

check_clean_worktree() {
    local changes

    changes="$(git -C "$PROJECT_ROOT" status --porcelain --untracked-files=no)"

    if [ -n "$changes" ]; then
        echo "Working directory contains changes. Please commit or stash changes before publishing." >&2
        return 1
    fi
}

get_latest_version() {
    local prefix="$1"

    git -C "$PROJECT_ROOT" tag -l "${prefix}*" |
    while read -r tag; do

        local rest="${tag#$prefix}"

        # Strip suffix after first '-'
        rest="${rest%%-*}"

        if [[ "$rest" =~ ^[0-9]+$ ]]; then
            echo "$rest"
        fi

    done |
    sort -n |
    tail -1
}

is_head() {
    local tag="$1"

    git -C "$PROJECT_ROOT" tag --points-at HEAD |
        grep -Fxq "$tag"
}

fetch_next_build_name_from_git_tag() {
    local prefix="$1"

    if [ -z "${prefix:-}" ]; then
        return 0
    fi

    check_clean_worktree

    local vnum
    vnum="$(get_latest_version "$prefix")"

    if [ -n "$vnum" ]; then

        local version="${prefix}${vnum}"

        echo "Latest version is ${version}" >&2

        if ! is_head "$version"; then
            vnum=$((vnum + 1))
            version="${prefix}${vnum}"
        fi

        printf '%s' "$version"

    else

        echo "No version found with prefix ${prefix}" >&2

        if [[ "$prefix" == *"." ]]; then
            printf '%s0' "$prefix"
        else
            printf '%s1' "$prefix"
        fi
    fi
}

# ---------------------------------------------------------------------------
# JADEX_VERSION
# ---------------------------------------------------------------------------

JADEX_VERSION_PREFIX="$(get_var JADEX_VERSION_PREFIX)"

JADEX_VERSION="$(fetch_next_build_name_from_git_tag "$JADEX_VERSION_PREFIX")"

if [ -n "${JADEX_VERSION:-}" ]; then
    export JADEX_VERSION
    echo "Configured version: ${JADEX_VERSION}" >&2
else
    echo "No version configured or detected" >&2
fi

# ---------------------------------------------------------------------------
# Signing key / password
# ---------------------------------------------------------------------------

sigKey="$(get_var sigKey)"

if [ -n "$sigKey" ]; then
    export signingKey="${sigKey//\\n/$'\n'}"
else
    echo "no signing key found" >&2
fi

signingPassword="$(get_var signingPassword)"

if [ -n "$signingPassword" ]; then
    export signingPassword
else
    echo "no signing key pass found" >&2
fi

# ---------------------------------------------------------------------------
# Central repository URL parsing
# ---------------------------------------------------------------------------

parse_url_with_credentials() {
    local url="$1"

    local re='^([a-zA-Z][a-zA-Z0-9+.-]*)://(([^@/]*)@)?([^/:?#]+)(:([0-9]+))?(.*)$'

    if [[ ! "$url" =~ $re ]]; then
        echo "Could not parse repo URL: $url" >&2
        return 1
    fi

    local scheme="${BASH_REMATCH[1]}"
    local userinfo="${BASH_REMATCH[3]}"
    local host="${BASH_REMATCH[4]}"
    local port="${BASH_REMATCH[6]}"
    local rest="${BASH_REMATCH[7]}"

    REPO_USER=""
    REPO_PASSWORD=""
    REPO_FINGERPRINT=""

    if [ -n "$userinfo" ]; then

        local IFS=':;'
        local -a parts

        read -ra parts <<< "$userinfo"

        REPO_USER="${parts[0]:-}"
        REPO_PASSWORD="${parts[1]:-}"

        local i

        for ((i = 2; i < ${#parts[@]}; i++)); do
            case "${parts[$i]}" in
                fingerprint=*)
                    REPO_FINGERPRINT="${parts[$i]#fingerprint=}"
                    REPO_FINGERPRINT="${REPO_FINGERPRINT//-/:}"
                    ;;
            esac
        done
    fi

    REPO_URL="${scheme}://${host}${port:+:${port}}${rest}"
}

repocentral="$(get_var repocentral)"

if [ -n "$repocentral" ]; then
    if parse_url_with_credentials "$repocentral"; then

        [ -n "$REPO_USER" ] && export centralUser="$REPO_USER"
        [ -n "$REPO_PASSWORD" ] && export centralPassword="$REPO_PASSWORD"

        export REPO_CENTRAL_URL="$REPO_URL"
    fi
fi

# ---------------------------------------------------------------------------
# SSH deploy URL parsing (dl_host)
#
# Format: ssh://user:url-encoded-password[;fingerprint=xx-xx-...]@host:port
#
# Das Passwort ist URL-encoded (z.B. "/" als "%2F") und muss vor der
# Verwendung mit sshpass/ssh/scp decodiert werden. Der optionale
# ";fingerprint=..."-Teil gehört NICHT zum Passwort.
# ---------------------------------------------------------------------------

url_decode() {
    local data="${1//+/ }"
    printf '%b' "${data//%/\\x}"
}

parse_ssh_url_with_credentials() {
    local input="$1"

    SSH_USER=""
    SSH_PASSWORD=""
    SSH_HOST=""
    SSH_PORT=""
    SSH_FINGERPRINT=""

    if [ -z "$input" ]; then
        echo "parse_ssh_url_with_credentials: empty URL given" >&2
        return 1
    fi

    local url="${input#ssh://}"
    if [ "$url" = "$input" ]; then
        echo "parse_ssh_url_with_credentials: URL must start with ssh:// (got: $input)" >&2
        return 1
    fi

    if [[ "$url" != *"@"* ]]; then
        echo "parse_ssh_url_with_credentials: missing '@' in URL" >&2
        return 1
    fi

    local userinfo="${url%%@*}"
    local hostport="${url#*@}"

    SSH_USER="${userinfo%%:*}"
    local rest="${userinfo#*:}"

    local password_raw="$rest"
    if [[ "$rest" == *";"* ]]; then
        password_raw="${rest%%;*}"
        local fp_part="${rest#*;}"
        if [[ "$fp_part" == fingerprint=* ]]; then
            local fp="${fp_part#fingerprint=}"
            SSH_FINGERPRINT="${fp//-/:}"
        fi
    fi
    SSH_PASSWORD="$(url_decode "$password_raw")"

    SSH_HOST="${hostport%%:*}"
    SSH_PORT="${hostport##*:}"

    if [ -z "$SSH_USER" ] || [ -z "$SSH_HOST" ] || [ -z "$SSH_PORT" ]; then
        echo "parse_ssh_url_with_credentials: failed to fully parse URL" >&2
        return 1
    fi

    export SSH_USER SSH_PASSWORD SSH_HOST SSH_PORT SSH_FINGERPRINT
    return 0
}

dl_host="$(get_var dl_host)"

if [ -n "$dl_host" ]; then
    parse_ssh_url_with_credentials "$dl_host" || true
else
    echo "no dl_host found" >&2
fi

echo "version:${JADEX_VERSION:-}|" >&2

echo >&2
echo "========================================" >&2
echo "Configured build environment:" >&2
echo "========================================" >&2

for var in \
    JADEX_VERSION_PREFIX \
    JADEX_VERSION \
    signingKey \
    signingPassword \
    centralUser \
    centralPassword \
    REPO_CENTRAL_URL \
    SSH_USER \
    SSH_HOST \
    SSH_PORT \
    SSH_FINGERPRINT
do
    printf '%s=%s\n' "$var" "${!var:-<not set>}"
done >&2

echo "========================================" >&2