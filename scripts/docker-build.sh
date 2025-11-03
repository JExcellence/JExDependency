#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GRADLEW="$ROOT_DIR/gradlew"
ARTIFACT_DEST="${ARTIFACT_DEST:-/artifacts}"
mkdir -p "$ARTIFACT_DEST"

echo "[docker-build] Artifact destination: $ARTIFACT_DEST"

require_cmd() {
    if ! command -v "$1" >/dev/null 2>&1; then
        echo "Required command '$1' is not available" >&2
        exit 1
    fi
}

for cmd in curl git; do
    require_cmd "$cmd"
done

configure_fine_grained_token() {
    local token="${GITHUB_FINE_GRAIN_TOKEN:-${GITHUB_TOKEN:-}}"
    if [[ -z "$token" ]]; then
        echo "[docker-build] GitHub fine-grained access token not provided. Proceeding without authenticated requests."
        return 0
    fi

    export GITHUB_TOKEN="$token"
    git config --global url."https://$GITHUB_TOKEN:@github.com/".insteadOf "https://github.com/"
    echo "[docker-build] Configured GitHub authentication with fine-grained access token."
}

configure_fine_grained_token

extract_version() {
    local file=$1
    local version
    version=$(sed -n 's/^version\s*=\s*"\(.*\)"/\1/p' "$file" | head -n1)
    if [[ -z "$version" ]]; then
        echo "Failed to determine version from $file" >&2
        exit 1
    fi
    printf '%s' "$version"
}

RCORE_VERSION=$(extract_version "$ROOT_DIR/RCore/build.gradle.kts")
RDQ_VERSION=$(extract_version "$ROOT_DIR/RDQ/build.gradle.kts")
JEXE_VERSION=$(extract_version "$ROOT_DIR/JExEconomy/build.gradle.kts")

REQUIRED_FILES=(
    "$ARTIFACT_DEST/RCore-Premium-${RCORE_VERSION}.jar"
    "$ARTIFACT_DEST/RDQ-Free-${RDQ_VERSION}.jar"
    "$ARTIFACT_DEST/RDQ-Premium-${RDQ_VERSION}.jar"
    "$ARTIFACT_DEST/JExEconomy-${JEXE_VERSION}.jar"
)

all_present=true
for file in "${REQUIRED_FILES[@]}"; do
    if [[ ! -f "$file" ]]; then
        all_present=false
        break
    fi
done

if [[ "$all_present" == true ]]; then
    echo "[docker-build] All artifacts already exist for requested versions. Skipping build."
    exit 0
fi

echo "[docker-build] Preparing local Maven dependencies..."
"$GRADLEW" --no-daemon :JExDependency:publishToMavenLocal
"$GRADLEW" --no-daemon :JExCommand:publishToMavenLocal
"$GRADLEW" --no-daemon :JExTranslate:publishToMavenLocal
"$GRADLEW" --no-daemon :RPlatform:publishToMavenLocal
"$GRADLEW" --no-daemon :JExEconomy:publishToMavenLocal
"$GRADLEW" --no-daemon :RCore:publishLocal
"$GRADLEW" --no-daemon :RDQ:publishLocal

echo "[docker-build] Building shaded artifacts..."
"$GRADLEW" --no-daemon \
    :RCore:rcore-premium:shadowJar \
    :RDQ:rdq-free:shadowJar \
    :RDQ:rdq-premium:shadowJar \
    :JExEconomy:shadowJar

copy_artifact() {
    local source=$1
    local target=$2
    if [[ ! -f "$source" ]]; then
        echo "[docker-build] Expected artifact not found: $source" >&2
        exit 1
    fi
    cp "$source" "$target"
    echo "[docker-build] Copied $(basename "$target")"
}

copy_artifact "$ROOT_DIR/RCore/rcore-premium/build/libs/RCore-premium-${RCORE_VERSION}.jar" \
    "$ARTIFACT_DEST/RCore-Premium-${RCORE_VERSION}.jar"
copy_artifact "$ROOT_DIR/RDQ/rdq-free/build/libs/RDQ-free-${RDQ_VERSION}.jar" \
    "$ARTIFACT_DEST/RDQ-Free-${RDQ_VERSION}.jar"
copy_artifact "$ROOT_DIR/RDQ/rdq-premium/build/libs/RDQ-premium-${RDQ_VERSION}.jar" \
    "$ARTIFACT_DEST/RDQ-Premium-${RDQ_VERSION}.jar"
copy_artifact "$ROOT_DIR/JExEconomy/build/libs/JExEconomy-${JEXE_VERSION}.jar" \
    "$ARTIFACT_DEST/JExEconomy-${JEXE_VERSION}.jar"

echo "[docker-build] Build complete. Artifacts available in $ARTIFACT_DEST"
