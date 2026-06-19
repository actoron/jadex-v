#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="${PROJECT_ROOT:-$(pwd)}"

# ---------------------------------------------------------------------------
# get_var <KEY>
# ---------------------------------------------------------------------------
get_var() {
    local key="$1"
    local val=""

    val="${!key:-}"

    if [ -z "$val" ]; then
        local prefixed="ORG_GRADLE_PROJECT_${key}"
        val="${!prefixed:-}"
    fi

    printf '%s' "$val"
}

# ---------------------------------------------------------------------------
# safe git wrapper
# ---------------------------------------------------------------------------
git_safe() {
    if command -v git >/dev/null 2>&1; then
        git -C "$PROJECT_ROOT" "$@" 2>/dev/null || true
    else
        return 0
    fi
}

dl_host="$(get_var dl_host)"
sigKey="$(get_var sigKey)"
signingPassword="$(get_var signingPassword)"
repocentral="$(get_var repocentral)"
repos="$(get_var repos)"
repoId="$(get_var repoId)"

repos="${repos:-central}"
export repos

[ -n "${repoId:-}" ] && export repoId
[ -n "${repocentral:-}" ] && export repocentral

# ---------------------------------------------------------------------------
# Version handling
# ---------------------------------------------------------------------------
JADEX_VERSION_PREFIX="$(get_var JADEX_VERSION_PREFIX)"
JADEX_VERSION_PREFIX="${JADEX_VERSION_PREFIX:-5.0-beta}"

# TEST OVERRIDE (falls wirklich gewollt)
# JADEX_VERSION_PREFIX="5.0-beta-test"

check_clean_worktree() {
    local changes
    changes="$(git_safe status --porcelain --untracked-files=no)"

    if [ -n "${changes:-}" ]; then
        echo "Working directory contains changes. Please commit or stash before publishing." >&2
        return 1
    fi
}

get_latest_version() {
    local prefix="$1"

    git_safe tag -l "${prefix}*" |
    while read -r tag; do
        local rest="${tag#$prefix}"
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
    git_safe tag --points-at HEAD | grep -Fxq "$tag" || true
}

fetch_next_build_name_from_git_tag() {
    local prefix="$1"

    [ -z "${prefix:-}" ] && return 0

    check_clean_worktree || true

    local vnum=""
    vnum="$(get_latest_version "$prefix" || true)"

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
# Compute version
# ---------------------------------------------------------------------------
JADEX_VERSION="$(fetch_next_build_name_from_git_tag "$JADEX_VERSION_PREFIX" || true)"

if [ -n "${JADEX_VERSION:-}" ]; then
    export JADEX_VERSION
    echo "Configured version: ${JADEX_VERSION}" >&2
else
    echo "No version configured or detected" >&2
fi

# ---------------------------------------------------------------------------
# Signing
# ---------------------------------------------------------------------------
sigKey="$(get_var sigKey)"

if [ -n "${sigKey:-}" ]; then
    export signingKey="${sigKey//\\n/$'\n'}"
else
    echo "no signing key found" >&2
fi

if [ -n "${signingPassword:-}" ]; then
    export signingPassword
else
    echo "no signing key pass found" >&2
fi

# ---------------------------------------------------------------------------
# Gradle JVM options
# ---------------------------------------------------------------------------
JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-}"

[ -n "${JADEX_VERSION:-}" ] && JAVA_TOOL_OPTIONS+=" -DJADEX_VERSION=$JADEX_VERSION"
[ -n "${repoId:-}" ] && JAVA_TOOL_OPTIONS+=" -DrepoId=$repoId"
[ -n "${signingKey:-}" ] && JAVA_TOOL_OPTIONS+=" -DsigningKey=$signingKey"
[ -n "${signingPassword:-}" ] && JAVA_TOOL_OPTIONS+=" -DsigningPassword=$signingPassword"

export JAVA_TOOL_OPTIONS

# ---------------------------------------------------------------------------
# Central repo parsing
# ---------------------------------------------------------------------------
parse_url_with_credentials() {
    local url="$1"

    REPO_USER=""
    REPO_PASSWORD=""
    REPO_FINGERPRINT=""

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

    if [ -n "${userinfo:-}" ]; then
        IFS=':;' read -r -a parts <<< "$userinfo"

        REPO_USER="${parts[0]:-}"
        REPO_PASSWORD="${parts[1]:-}"

        for part in "${parts[@]:2}"; do
            case "$part" in
                fingerprint=*)
                    REPO_FINGERPRINT="${part#fingerprint=}"
                    REPO_FINGERPRINT="${REPO_FINGERPRINT//-/:}"
                    ;;
            esac
        done
    fi

    REPO_URL="${scheme}://${host}${port:+:${port}}${rest}"
}

if [ -n "${repocentral:-}" ]; then
    if parse_url_with_credentials "$repocentral"; then
        [ -n "${REPO_USER:-}" ] && export centralUser="$REPO_USER"
        [ -n "${REPO_PASSWORD:-}" ] && export centralPassword="$REPO_PASSWORD"
        export REPO_CENTRAL_URL="$REPO_URL"
    fi
fi

# ---------------------------------------------------------------------------
# SSH parsing
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
        echo "empty dl_host" >&2
        return 1
    fi

    local url="${input#ssh://}"
    [[ "$url" == "$input" ]] && return 1

    local userinfo="${url%%@*}"
    local hostport="${url#*@}"

    SSH_USER="${userinfo%%:*}"
    local rest="${userinfo#*:}"

    local password_raw="$rest"

    if [[ "$rest" == *";"* ]]; then
        password_raw="${rest%%;*}"
        local fp_part="${rest#*;}"

        if [[ "$fp_part" == fingerprint=* ]]; then
            SSH_FINGERPRINT="${fp_part#fingerprint=}"
            SSH_FINGERPRINT="${SSH_FINGERPRINT//-/:}"
        fi
    fi

    SSH_PASSWORD="$(url_decode "$password_raw")"

    SSH_HOST="${hostport%%:*}"
    SSH_PORT="${hostport##*:}"

    export SSH_USER SSH_PASSWORD SSH_HOST SSH_PORT SSH_FINGERPRINT
}

if [ -n "${dl_host:-}" ]; then
    parse_ssh_url_with_credentials "$dl_host" || true
fi

# ---------------------------------------------------------------------------
# Output
# ---------------------------------------------------------------------------
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
    SSH_FINGERPRINT \
    repos \
    repoId \
    repocentral
do
    printf '%s=%s\n' "$var" "${!var:-<not set>}"
done >&2

echo "========================================" >&2